/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/

package com.mageventory.job;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.model.Product;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils.BroadcastReceiverRegisterHandler;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;

/* This job queue is designed with assumption that there is at most one job being selected and processed at any given time.
 * In order to select a job you must call selectJob(). After you're finished with the job you must call handleProcessedJob().
 * You can't call selectJob() twice without calling handleProcessedJob(). You can however add any number of jobs from any number
 * threads at any time without doing any synchronisation in those threads. */
public class JobQueue {

    public static String JOBS_SUMMARY_CHANGED_EVENT_ACTION = MyApplication.getContext()
            .getPackageName() + ".JOBS_SUMMARY_CHANGED_EVENT_ACTION";
    public static String JOBS_SUMMARY = MyApplication.getContext().getPackageName()
            + ".JOBS_SUMMARY";

    public static Object sQueueSynchronizationObject = new Object();

    /*
     * Specifies how many times a job can fail before it is moved to the failed
     * table.
     */
    public static final int sFailureCounterLimit = 5;

    /*
     * DB helper creates tables we use if they are not already created and helps
     * interface with the underlaying database.
     */
    private JobQueueDBHelper mDbHelper;

    /* Reference to the underlying database. */
    private SQLiteDatabase mDB;

    private static String TAG = "JOB_QUEUE";

    /*
     * This always points to a job that is currently being processed. If no job
     * is being processed - this is null.
     */
    private static Job mCurrentJob;

    /* Add a job to the queue. */
    public boolean add(Job job) {
        synchronized (sQueueSynchronizationObject) {
            CommonUtils.debug(TAG, true, "Adding a job to the queue" + " timestamp="
                    + job.getJobID().getTimeStamp()
                    + " jobtype="
                    + job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID()
                    + " SKU="
                    + job.getJobID().getSKU());
            if (JobCacheManager.store(job) == true) {
                dbOpen();
                ContentValues cv = new ContentValues();
                boolean res;
                cv.put(JobQueueDBHelper.JOB_TIMESTAMP, job.getJobID().getTimeStamp());
                cv.put(JobQueueDBHelper.JOB_PRODUCT_ID, job.getJobID().getProductID());
                cv.put(JobQueueDBHelper.JOB_TYPE, job.getJobID().getJobType());
                cv.put(JobQueueDBHelper.JOB_SKU, job.getJobID().getSKU());
                cv.put(JobQueueDBHelper.JOB_ATTEMPTS, 0);
                cv.put(JobQueueDBHelper.JOB_SERVER_URL, job.getJobID().getUrl());
                res = insert(cv, true);

                if (res != true) {
                    CommonUtils.warn(TAG, "Unable to add a job to the database" + " timestamp="
                            + job.getJobID().getTimeStamp()
                            + " jobtype=" + job.getJobID().getJobType() + " prodID="
                            + job.getJobID().getProductID()
                            + " SKU=" + job.getJobID().getSKU());
                    JobCacheManager.removeFromCache(job.getJobID());
                } else {
                    changeSummary(job.getJobID(), true, true);
                }

                dbClose();
                return res;
            } else {
                CommonUtils.warn(TAG, "Unable to store job in cache" + " timestamp="
                        + job.getJobID().getTimeStamp() + " jobtype="
                        + job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID()
                        + " SKU="
                        + job.getJobID().getSKU());
            }

            return false;
        }
    }

    /*
     * Return a job that is currently being executed. Return null if no job is
     * being executed. This needs to be called when sQueueSynchronizationObject
     * is held in order to be sure that the current job doesn't finish after
     * this function returns but before any necessary processing is done by the
     * caller.
     */
    public static Job getCurrentJob()
    {
        return mCurrentJob;
    }

    /* Do we have any jobs that need executing? */
    public boolean isPendingTableEmpty()
    {
        dbOpen();
        boolean out = isEmpty(true);
        dbClose();

        return out;
    }

    /*
     * Select next job from the queue to be executed. In case the job cannot be
     * deserialized from the cache we delete it from the queue. This function is
     * part of the job lifecycle and is always called before the job starts
     * being executed.
     */
    public Job selectJob(boolean dontReturnImageUploadJobs) {
        synchronized (sQueueSynchronizationObject) {
            CommonUtils.debug(TAG, "Selecting next job");

            dbOpen();

            while (true) {
                Cursor c;

                /*
                 * RES_ADD_PRODUCT_TO_CART have priority over all other jobs
                 * types.
                 */
                c = query(new String[] {
                        JobQueueDBHelper.JOB_TIMESTAMP, JobQueueDBHelper.JOB_PRODUCT_ID,
                        JobQueueDBHelper.JOB_TYPE, JobQueueDBHelper.JOB_SKU,
                        JobQueueDBHelper.JOB_SERVER_URL
                },
                        JobQueueDBHelper.JOB_TYPE + "="
                                + MageventoryConstants.RES_ADD_PRODUCT_TO_CART,
                        null, JobQueueDBHelper.JOB_ATTEMPTS + " ASC, "
                                + JobQueueDBHelper.JOB_TIMESTAMP + " ASC", "0, 1", true);

                if (c.getCount() == 0)
                {
                    if (dontReturnImageUploadJobs)
                    {
                        c = query(new String[] {
                                JobQueueDBHelper.JOB_TIMESTAMP, JobQueueDBHelper.JOB_PRODUCT_ID,
                                JobQueueDBHelper.JOB_TYPE, JobQueueDBHelper.JOB_SKU,
                                JobQueueDBHelper.JOB_SERVER_URL
                        },
                                "(" + JobQueueDBHelper.JOB_PRODUCT_ID + "!=-1 OR "
                                        + JobQueueDBHelper.JOB_TYPE + "="
                                        + MageventoryConstants.RES_CATALOG_PRODUCT_CREATE +
                                        ") AND " + JobQueueDBHelper.JOB_TYPE + " != "
                                        + MageventoryConstants.RES_UPLOAD_IMAGE,
                                null, JobQueueDBHelper.JOB_ATTEMPTS
                                        + " ASC, " + JobQueueDBHelper.JOB_TIMESTAMP + " ASC",
                                "0, 1", true);
                    }
                    else
                    {
                        c = query(new String[] {
                                JobQueueDBHelper.JOB_TIMESTAMP, JobQueueDBHelper.JOB_PRODUCT_ID,
                                JobQueueDBHelper.JOB_TYPE, JobQueueDBHelper.JOB_SKU,
                                JobQueueDBHelper.JOB_SERVER_URL
                        }, JobQueueDBHelper.JOB_PRODUCT_ID
                                + "!=-1 OR " + JobQueueDBHelper.JOB_TYPE + "="
                                + MageventoryConstants.RES_CATALOG_PRODUCT_CREATE, null,
                                JobQueueDBHelper.JOB_ATTEMPTS
                                        + " ASC, " + JobQueueDBHelper.JOB_TIMESTAMP + " ASC",
                                "0, 1", true);
                    }
                }

                if (c.moveToFirst() == true) {
                    JobID jobID = new JobID(c.getLong(c
                            .getColumnIndex(JobQueueDBHelper.JOB_TIMESTAMP)), c.getInt(c
                            .getColumnIndex(JobQueueDBHelper.JOB_PRODUCT_ID)), c.getInt(c
                            .getColumnIndex(JobQueueDBHelper.JOB_TYPE)), c.getString(c
                            .getColumnIndex(JobQueueDBHelper.JOB_SKU)), c.getString(c
                            .getColumnIndex(JobQueueDBHelper.JOB_SERVER_URL)));
                    c.close();

                    CommonUtils.debug(TAG, true,
                            "Selected a job" + " timestamp=" + jobID.getTimeStamp() + " jobtype="
                                    + jobID.getJobType()
                                    + " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());

                    Job out = JobCacheManager.restore(jobID);

                    if (out == null) {
                        CommonUtils.warn(TAG,
                                "Unable to restore job from cache, will delete it and try the next one"
                                        + " timestamp=" + jobID.getTimeStamp() + " jobtype="
                                        + jobID.getJobType() + " prodID="
                                        + jobID.getProductID() + " SKU=" + jobID.getSKU());

                        dbClose();
                        deleteJobFromQueue(jobID, true, true, false);
                        dbOpen();
                        continue;
                    }
                    dbClose();

                    /* Job will be executed now, reset all state fields. */
                    out.setException(null);
                    out.setFinished(false);
                    out.setPending(true);

                    JobCacheManager.store(out);

                    CommonUtils.debug(TAG, true,
                            "Job selected" + " timestamp=" + jobID.getTimeStamp() + " jobtype="
                            + jobID.getJobType()
                            + " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());

                    out.getJobID().setProductID(jobID.getProductID());

                    /* This is the current job now. */
                    mCurrentJob = out;
                    return out;
                }

                CommonUtils.debug(TAG, "Didn't find any jobs in the queue, returning null");

                c.close();
                dbClose();
                return null;
            }
        }
    }

    /* Update product id of all jobs with a given SKU and URL */
    private boolean updateProductID(int prodID, String SKU, String URL) {
        synchronized (sQueueSynchronizationObject) {
            CommonUtils.debug(TAG, true,
                    "Updating product id in the database for a given SKU and URL," + " prodID="
                    + prodID + " SKU=" + SKU + " URL=" + URL);

            boolean res = false;

            dbOpen();
            ContentValues cv = new ContentValues();
            cv.put(JobQueueDBHelper.JOB_PRODUCT_ID, prodID);

            res = update(cv, JobQueueDBHelper.JOB_SKU + "=? AND " + JobQueueDBHelper.JOB_SERVER_URL
                    + "=?", new String[] {
                    SKU, URL
            }, true);

            if (res == false) {
                CommonUtils.warn(TAG, "Updating product id unsuccessful," + " prodID=" + prodID
                        + " SKU="
                        + SKU);
            } else {
                CommonUtils.debug(TAG, true, "Updating product id successful," + " prodID="
                        + prodID + " SKU=" + SKU);
            }

            dbClose();

            return res;
        }
    }

    public boolean isNewProductJobInThePendingTable(String SKU, String URL)
    {
        boolean out;

        dbOpen();

        Cursor c = query(new String[] {
                JobQueueDBHelper.JOB_TIMESTAMP, JobQueueDBHelper.JOB_PRODUCT_ID,
                JobQueueDBHelper.JOB_TYPE, JobQueueDBHelper.JOB_SKU,
                JobQueueDBHelper.JOB_SERVER_URL
        }, JobQueueDBHelper.JOB_TYPE + "="
                + MageventoryConstants.RES_CATALOG_PRODUCT_CREATE + " AND "
                + JobQueueDBHelper.JOB_SKU + "=" + "'" + SKU + "'" + " AND "
                + JobQueueDBHelper.JOB_SERVER_URL + "=" + "'" + URL + "'"
                , null, null, "0, 1", true);

        out = c.getCount() > 0 ? true : false;

        c.close();

        dbClose();

        return out;
    }

    /* Helper function to update sku for a specific table. */
    private void updateSKUInTable(String URL, String from, String to, boolean pendingTable)
    {
        Cursor c = query(new String[] {
                JobQueueDBHelper.JOB_TIMESTAMP, JobQueueDBHelper.JOB_PRODUCT_ID,
                JobQueueDBHelper.JOB_TYPE, JobQueueDBHelper.JOB_SKU,
                JobQueueDBHelper.JOB_SERVER_URL
        }, JobQueueDBHelper.JOB_SKU + "=" + "'"
                + from + "' AND " + JobQueueDBHelper.JOB_SERVER_URL + "=" + "'" + URL + "'", null,
                null, null, pendingTable);

        /* Update skus in job files */
        for (; c.moveToNext();)
        {
            JobID jobID = new JobID(c.getLong(c.getColumnIndex(JobQueueDBHelper.JOB_TIMESTAMP)),
                    c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_PRODUCT_ID)), c.getInt(c
                            .getColumnIndex(JobQueueDBHelper.JOB_TYPE)), c.getString(c
                            .getColumnIndex(JobQueueDBHelper.JOB_SKU)), c.getString(c
                            .getColumnIndex(JobQueueDBHelper.JOB_SERVER_URL)));

            Job job = JobCacheManager.restore(jobID);

            /* Did we manage to deserialize the job from the cache? */
            if (job != null)
            {
                /* Yes, the job was deserialized with success. */

                /* Modify the SKU in the job file */
                job.getJobID().setSKU(to);

                if (job.getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_SELL)
                {
                    job.putExtraInfo(MageventoryConstants.MAGEKEY_PRODUCT_SKU, to);
                }

                /*
                 * In case of image job we need to update the path to the image
                 * file.
                 */
                if (job.getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE)
                {
                    String oldPath = (String) job
                            .getExtraInfo(MageventoryConstants.MAGEKEY_PRODUCT_IMAGE_CONTENT);
                    if (!ImageUtils.isUrl(oldPath)) {
                        // update only local file paths
                        /*
                         * Need to find the slash the directly precedes the file
                         * name.
                         */
                        int fileNameSlashIndex = oldPath.lastIndexOf('/');
                        if (fileNameSlashIndex == oldPath.length() - 1) {
                            fileNameSlashIndex = oldPath.substring(0, oldPath.length() - 1)
                                    .lastIndexOf('/');
                        }

                        String fileName = oldPath.substring(fileNameSlashIndex + 1);
                        String newImageUploadDir = JobCacheManager.getImageUploadDirectory(to,
                                jobID.getUrl()).getAbsolutePath();

                        String newPath;
                        if (newImageUploadDir.endsWith("/")) {
                            newPath = newImageUploadDir + fileName;
                        } else {
                            newPath = newImageUploadDir + '/' + fileName;
                        }

                        job.putExtraInfo(MageventoryConstants.MAGEKEY_PRODUCT_IMAGE_CONTENT,
                                newPath);
                    }

                }

                /*
                 * Store the job in the old location (regardless of the new
                 * sku). It will be copied later.
                 */
                JobCacheManager.store(job, from);
            }

            /* Update skus in the queue table */
            ContentValues cv = new ContentValues();
            cv.put(JobQueueDBHelper.JOB_SKU, to);

            update(cv, JobQueueDBHelper.JOB_SKU + "=?" + " AND " + JobQueueDBHelper.JOB_SERVER_URL
                    + "=?", new String[] {
                    from, URL
            }, pendingTable);
        }

        c.close();
    }

    /*
     * Perform everything that is needed to be performed after an sku was
     * modified by a product update job.
     */
    private void updateSKU(String url, String SKUfrom, String SKUto)
    {
        synchronized (sQueueSynchronizationObject) {
            synchronized (JobCacheManager.sSynchronizationObject) {
                dbOpen();

                updateSKUInTable(url, SKUfrom, SKUto, true);
                updateSKUInTable(url, SKUfrom, SKUto, false);

                dbClose();

                JobCacheManager.moveSKUdir(url, SKUfrom, SKUto);
            }
        }
    }

    /*
     * Called by the serivce when it wants to tell the queue it is done with the
     * job. The queue handles this by checking whether the job succeeded or
     * failed. If the job succeeded then if it was a product creation job then
     * all dependent jobs are assigned product id. The job is deleted from the
     * queue in this case. If on the other hand the job fails then it's failure
     * counter is increased. This function is part of job lifecycle and is
     * always called at the end of processing. The job object passed to this
     * function may be out of date after the function finishes because the
     * corresponding job file may be modified. This function always returns a
     * fresh job object that can be used to check the status of the job for
     * example.
     */
    public Job handleProcessedJob(Job job) {
        CommonUtils.debug(TAG, true, "Handling a processed job" + " timestamp="
                + job.getJobID().getTimeStamp()
                + " jobtype="
                + job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID()
                + " SKU="
                + job.getJobID().getSKU());

        Job out = job;

        /* If the job finished with success. */
        if (job.getFinished() == true) {
            CommonUtils.debug(TAG, true, "Handling a processed job (job finished)" + " timestamp="
                    + job.getJobID().getTimeStamp()
                    + " jobtype=" + job.getJobID().getJobType() + " prodID="
                    + job.getJobID().getProductID() + " SKU="
                    + job.getJobID().getSKU());

            deleteJobFromQueue(job.getJobID(), true, false, false);

            /* If it was a product creation job. */
            if (job.getJobID().getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_CREATE) {
                CommonUtils
                        .debug(TAG, true,
                        "Handling a processed job (this is a product job, will update the jobid in database)"
                                + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype="
                                + job.getJobID().getJobType()
                                + " prodID=" + job.getJobID().getProductID() + " SKU="
                                + job.getJobID().getSKU());

                Product product = JobCacheManager.restoreProductDetails(job.getJobID().getSKU(),
                        job.getJobID().getUrl());

                if (product != null) {
                    /* Update product id of dependent jobs. */
                    updateProductID(Integer.parseInt(product.getId()), job.getJobID().getSKU(), job
                            .getJobID().getUrl());
                } else {
                    CommonUtils.debug(TAG, true,
                            "Handling a processed job (new product job), unable to restore product details from cache "
                                    + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype="
                                    + job.getJobID().getJobType() + " prodID="
                                    + job.getJobID().getProductID()
                                    + " SKU=" + job.getJobID().getSKU());
                }
            }
            else
            /* If it was product update job. */
            if (job.getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE)
            {
                /*
                 * If the product update job changed sku we need to update that
                 * sku in the queue and in the cache as well (for all jobs
                 * associated with that sku).
                 */
                if (!TextUtils.equals(
                        (String) job.getExtraInfo(MageventoryConstants.MAGEKEY_PRODUCT_SKU),
                        job.getSKU()))
                {
                    updateSKU(job.getUrl(), job.getSKU(),
                            (String) job.getExtraInfo(MageventoryConstants.MAGEKEY_PRODUCT_SKU));
                }
            }
            /*
             * If the job is not finished and and exception was thrown while
             * processing it.
             */
        } else if (job.getException() != null) {
            CommonUtils.warn(TAG, "Handling a processed job (job failed)" + " timestamp="
                    + job.getJobID().getTimeStamp()
                    + " jobtype=" + job.getJobID().getJobType() + " prodID="
                    + job.getJobID().getProductID() + " SKU="
                    + job.getJobID().getSKU());

            /*
             * Store the job in the cache to keep the job file up to date. It is
             * important to store it before calling increaseFailureCounter()
             * which is restoring it, modifying it, and storing it back again.
             */
            JobCacheManager.store(job);

            increaseFailureCounter(job.getJobID());

            out = JobCacheManager.restore(job.getJobID());
        }

        /* Set the current job to null. */
        synchronized (sQueueSynchronizationObject) {
            mCurrentJob = null;
        }

        if (job.getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE)
        {
            JobCacheManager.remergeProductDetailsWithEditJob((String) job
                    .getExtraInfo(MageventoryConstants.MAGEKEY_PRODUCT_SKU), job.getJobID()
                    .getUrl());
        }

        return out;
    }

    /*
     * Increase a failure counter for a given job from the pending table. If
     * failure counter limit gets reached the jobs will be moved to the failed
     * table.
     */
    private boolean increaseFailureCounter(JobID jobID) {
        synchronized (sQueueSynchronizationObject) {
            CommonUtils.debug(TAG, true,
                    "Increasing failure counter" + " timestamp=" + jobID.getTimeStamp()
                            + " jobtype="
                            + jobID.getJobType() + " prodID=" + jobID.getProductID() +
                            " SKU=" + jobID.getSKU());

            dbOpen();

            boolean res = false;

            int currentFailureCounter = 0;

            Cursor c = query(new String[] {
                JobQueueDBHelper.JOB_ATTEMPTS
            }, JobQueueDBHelper.JOB_TIMESTAMP + "=?",
                    new String[] {
                        "" + jobID.getTimeStamp()
                    }, null, null, true);
            if (c.moveToFirst() == true) {
                currentFailureCounter = c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_ATTEMPTS));
                ContentValues cv = new ContentValues();
                cv.put(JobQueueDBHelper.JOB_ATTEMPTS, currentFailureCounter + 1);

                CommonUtils.debug(TAG, true,
                        "Increasing failure counter, old=" + currentFailureCounter + " new="
                                + (currentFailureCounter + 1) + " timestamp="
                                + jobID.getTimeStamp() + " jobtype="
                                + jobID.getJobType() + " prodID=" + jobID.getProductID() + " SKU="
                                + jobID.getSKU());

                res = update(cv, JobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[] {
                    "" + jobID.getTimeStamp()
                },
                        true);

                if (res == false) {
                    CommonUtils.warn(
                            TAG,
                            "Unable to increase failure counter, old=" + currentFailureCounter
                                    + " new="
                                    + (currentFailureCounter + 1) + " timestamp="
                                    + jobID.getTimeStamp() + " jobtype="
                                    + jobID.getJobType() + " prodID=" + jobID.getProductID()
                                    + " SKU="
                                    + jobID.getSKU());
                } else {
                    CommonUtils.debug(
                            TAG,
                            true,
                            "Increasing failure counter successful" + " timestamp="
                                    + jobID.getTimeStamp()
                                    + " jobtype=" + jobID.getJobType() + " prodID="
                                    + jobID.getProductID() + " SKU="
                                    + jobID.getSKU());
                }
            } else {
                CommonUtils.warn(
                        TAG,
                        "Increasing failure counter problem (cannot find job in the queue)"
                                + " timestamp="
                                + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
                                + " prodID="
                                + jobID.getProductID() + " SKU=" + jobID.getSKU());
            }

            c.close();
            dbClose();

            if (currentFailureCounter > sFailureCounterLimit) {
                CommonUtils.debug(
                        TAG,
                        true,
                        "Failure counter reached the limit, deleting job from queue"
                                + " timestamp="
                                + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
                                + " prodID="
                                + jobID.getProductID() + " SKU=" + jobID.getSKU());

                deleteJobFromQueue(jobID, true, true, true);
            }

            return res;
        }
    }

    /* Set failure counter to 0 for a given job from the pending table. */
    public boolean resetFailureCounter(JobID jobID) {
        synchronized (sQueueSynchronizationObject) {
            CommonUtils.debug(TAG, true,
                    "Reseting failure counter" + " timestamp=" + jobID.getTimeStamp() + " jobtype="
                            + jobID.getJobType() + " prodID=" + jobID.getProductID() +
                            " SKU=" + jobID.getSKU());

            dbOpen();

            boolean res = false;

            ContentValues cv = new ContentValues();
            cv.put(JobQueueDBHelper.JOB_ATTEMPTS, 0);

            res = update(cv, JobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[] {
                "" + jobID.getTimeStamp()
            },
                    true);

            if (res == false) {
                CommonUtils.warn(
                        TAG,
                        "Unable to reset failure counter, timestamp=" + jobID.getTimeStamp()
                                + " jobtype="
                                + jobID.getJobType() + " prodID=" + jobID.getProductID() + " SKU="
                                + jobID.getSKU());
            } else {
                CommonUtils.debug(TAG, true,
                        "Resetting of the failure counter performed with success," + " timestamp="
                                + jobID.getTimeStamp()
                                + " jobtype=" + jobID.getJobType() + " prodID="
                                + jobID.getProductID() + " SKU="
                                + jobID.getSKU());
            }

            dbClose();

            return res;
        }
    }

    /*
     * Move a job from failed table to pending table and reset the failure
     * counter.
     */
    public boolean retryJob(JobID jobID) {
        synchronized (sQueueSynchronizationObject) {

            /*
             * This function needs to be synchronised in terms of sdcard cache
             * as well as it is doing things like reading a job from the cache,
             * modifying it and then saving it. This is why we don't want
             * anybody else to touch the cache when we're doing that.
             */
            synchronized (JobCacheManager.sSynchronizationObject) {

                CommonUtils.debug(
                        TAG,
                        true,
                        "Trying to retry a job " + " timestamp=" + jobID.getTimeStamp()
                                + " jobtype=" + jobID.getJobType()
                                + " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());

                dbOpen();

                /* Delete the job from the "failed" queue. */
                if (delete(JobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[] {
                    "" + jobID.getTimeStamp()
                }, false) > 0) {
                    /* The job deleted from the "failed" queue successfully. */

                    changeSummary(jobID, false, false);

                    /*
                     * Try to deserialize the job file from the cache before
                     * moving the job to the "pending" table.
                     */
                    Job job = JobCacheManager.restore(jobID);

                    /* Did we manage to deserialize the job? */
                    if (job != null)
                    {
                        /* Yes, the job was deserialized with success. */

                        /* Reset the state of the job. */
                        job.setPending(true);
                        job.setFinished(false);
                        job.setException(null);

                        JobCacheManager.store(job);

                        ContentValues cv = new ContentValues();
                        boolean res;
                        cv.put(JobQueueDBHelper.JOB_TIMESTAMP, jobID.getTimeStamp());
                        cv.put(JobQueueDBHelper.JOB_PRODUCT_ID, jobID.getProductID());
                        cv.put(JobQueueDBHelper.JOB_TYPE, jobID.getJobType());
                        cv.put(JobQueueDBHelper.JOB_SKU, jobID.getSKU());
                        cv.put(JobQueueDBHelper.JOB_ATTEMPTS, 0);
                        cv.put(JobQueueDBHelper.JOB_SERVER_URL, jobID.getUrl());
                        res = insert(cv, true);

                        if (res != true) {
                            CommonUtils.warn(
                                    TAG,
                                    "Unable to add a job to the pending table" + " timestamp="
                                            + jobID.getTimeStamp()
                                            + " jobtype=" + jobID.getJobType() + " prodID="
                                            + jobID.getProductID() + " SKU="
                                            + jobID.getSKU());
                            JobCacheManager.removeFromCache(jobID);
                            return false;
                        } else {
                            changeSummary(jobID, true, true);
                        }
                    }
                    else
                    {
                        /*
                         * We could not deserialize the job. In this case just
                         * get rid of it.
                         */
                        JobCacheManager.removeFromCache(jobID);
                    }
                } else {
                    /* We didn't manage to delete this job from the queue. */
                    CommonUtils.warn(TAG,
                            "Unable to find job in the failed queue to delete it" + " timestamp="
                                    + jobID.getTimeStamp()
                                    + " jobtype=" + jobID.getJobType() + " prodID="
                                    + jobID.getProductID() + " SKU="
                                    + jobID.getSKU());
                }

                dbClose();

                return false;
            }
        }
    }

    /*
     * Wipe all data from both tables. Use only if there are no jobs currently
     * being executed.
     */
    public void wipeTables()
    {
        synchronized (sQueueSynchronizationObject) {
            dbOpen();
            delete(null, null, false);
            delete(null, null, true);
            dbClose();
        }
    }

    /*
     * Delete a job from the queue. There is a number of parameters here. The
     * job can be deleted from the pending table or from the failed table. The
     * function can delete dependent jobs in case of product creation job or
     * not. The job can be moved to failed table after deletion or not.
     */
    public boolean deleteJobFromQueue(JobID jobID, boolean fromPendingTable,
            boolean deleteDependendIfNewProduct,
            boolean moveToFailedTable) {
        synchronized (sQueueSynchronizationObject) {

            /*
             * This function needs to be synchronised in terms of sdcard cache
             * as well as it is doing things like reading a job from the cache,
             * modifying it and then saving it. This is why we don't want
             * anybody else to touch the cache when we're doing that.
             */
            synchronized (JobCacheManager.sSynchronizationObject) {
                CommonUtils.debug(TAG, true,
                        "Trying to delete a job from queue" + " timestamp=" + jobID.getTimeStamp()
                                + " jobtype="
                                + jobID.getJobType() + " prodID=" + jobID.getProductID() + " SKU="
                                + jobID.getSKU()
                                + "fromPendingTable=" + fromPendingTable + " moveToFailedTable="
                                + moveToFailedTable);
                dbOpen();
                boolean global_res = true;
                boolean del_res;

                /* Delete the specified job from the queue */
                del_res = (delete(JobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[] {
                    "" + jobID.getTimeStamp()
                },
                        fromPendingTable) > 0);

                /* Did we succeed deleting the specified job from the queue? */
                if (del_res) {
                    /* We succeeded deleting the specified job from the queue. */

                    changeSummary(jobID, fromPendingTable, false);

                    /* Do we want to move the deleted job to the failed table? */
                    if (!moveToFailedTable) {
                        /*
                         * No, we don't want to move the deleted job to the
                         * failed table.
                         */

                        /* Just remove the job from the cache in that case. */
                        JobCacheManager.removeFromCache(jobID);

                        CommonUtils.debug(
                                TAG,
                                true,
                                "Job deleted successfully from queue" + " timestamp="
                                        + jobID.getTimeStamp() + " jobtype="
                                        + jobID.getJobType() + " prodID=" + jobID.getProductID()
                                        + " SKU=" + jobID.getSKU()
                                        + "fromPendingTable=" + fromPendingTable
                                        + " moveToFailedTable="
                                        + moveToFailedTable);
                    } else {
                        /*
                         * Yes, we want to move the deleted job to the failed
                         * table.
                         */

                        /*
                         * The job is not in pending state anymore, update the
                         * cache with that info.
                         */
                        Job job = JobCacheManager.restore(jobID);

                        /* Did we manage to deserialize the job from the cache? */
                        if (job != null)
                        {
                            /* Yes, the job was deserialized with success. */

                            /*
                             * Update the "pending" field in the job class and
                             * store it back in the cache.
                             */
                            job.setPending(false);
                            JobCacheManager.store(job);

                            /*
                             * Insert the deleted job in the failed table and
                             * don't delete the cached job file.
                             */
                            ContentValues cv = new ContentValues();
                            boolean res;
                            cv.put(JobQueueDBHelper.JOB_TIMESTAMP, jobID.getTimeStamp());
                            cv.put(JobQueueDBHelper.JOB_PRODUCT_ID, jobID.getProductID());
                            cv.put(JobQueueDBHelper.JOB_TYPE, jobID.getJobType());
                            cv.put(JobQueueDBHelper.JOB_SKU, jobID.getSKU());
                            cv.put(JobQueueDBHelper.JOB_ATTEMPTS, 0);
                            cv.put(JobQueueDBHelper.JOB_SERVER_URL, jobID.getUrl());
                            res = insert(cv, false);
                            if (res != true) {
                                CommonUtils.warn(TAG,
                                        "Unable to add a job to the failed table" + " timestamp="
                                                + jobID.getTimeStamp()
                                                + " jobtype=" + jobID.getJobType() + " prodID="
                                                + jobID.getProductID() + " SKU="
                                                + jobID.getSKU() + "fromPendingTable="
                                                + fromPendingTable + " moveToFailedTable="
                                                + moveToFailedTable);
                                global_res = false;
                                JobCacheManager.removeFromCache(jobID);
                            } else {
                                changeSummary(jobID, false, true);
                            }
                        }
                        else
                        {
                            /*
                             * We didn't manage to deserialize job from the
                             * cache.
                             */
                            global_res = false;
                            JobCacheManager.removeFromCache(jobID);
                        }
                    }

                    /*
                     * Did we delete product creation job and at the same time
                     * we want to delete all dependent jobs?
                     */
                    if (jobID.getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_CREATE
                            && deleteDependendIfNewProduct == true) {
                        /*
                         * Yes, we just deleted product creation job and we want
                         * to delete all dependent jobs as well.
                         */

                        Cursor c = query(new String[] {
                                JobQueueDBHelper.JOB_TIMESTAMP, JobQueueDBHelper.JOB_PRODUCT_ID,
                                JobQueueDBHelper.JOB_TYPE, JobQueueDBHelper.JOB_SKU,
                                JobQueueDBHelper.JOB_SERVER_URL
                        }, JobQueueDBHelper.JOB_SKU + "=" + "'"
                                + jobID.getSKU() + "'" + " AND " + JobQueueDBHelper.JOB_SERVER_URL
                                + "=" + "'" + jobID.getUrl() + "'",
                                null, null, null, fromPendingTable);

                        /*
                         * Iterate over all dependent jobs (having the same SKU
                         * as the product creation job) and delete them.
                         */
                        for (; c.moveToNext();) {
                            JobID jobIDdependent = new JobID(c.getLong(c
                                    .getColumnIndex(JobQueueDBHelper.JOB_TIMESTAMP)),
                                    c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_PRODUCT_ID)),
                                    c.getInt(c
                                            .getColumnIndex(JobQueueDBHelper.JOB_TYPE)),
                                    c.getString(c
                                            .getColumnIndex(JobQueueDBHelper.JOB_SKU)),
                                    c.getString(c
                                            .getColumnIndex(JobQueueDBHelper.JOB_SERVER_URL)));

                            boolean del_res_dependent;

                            /* Delete the dependent job. */
                            del_res_dependent = (delete(JobQueueDBHelper.JOB_TIMESTAMP + "=?",
                                    new String[] {
                                        ""
                                                + jobIDdependent.getTimeStamp()
                                    }, fromPendingTable) > 0);

                            /* Did we succeed deleting the dependent job? */
                            if (del_res_dependent) {
                                /*
                                 * Yes, we did succeed deleting the dependent
                                 * job.
                                 */
                                changeSummary(jobIDdependent, fromPendingTable, false);

                                /*
                                 * Do we want to move the dependent job to the
                                 * failed table?
                                 */
                                if (!moveToFailedTable) {
                                    /*
                                     * No, we don't want to move the dependent
                                     * job to the failed table, just remove it
                                     * from the cache in that case.
                                     */
                                    JobCacheManager.removeFromCache(jobIDdependent);
                                } else {
                                    /*
                                     * Yes, we want to move the dependent job to
                                     * the failed table, don't remove it from
                                     * the cache.
                                     */

                                    /*
                                     * This job is not in pending state anymore,
                                     * update the cache with that info.
                                     */
                                    Job job = JobCacheManager.restore(jobIDdependent);

                                    /*
                                     * Did we manage to deserialize the job from
                                     * the cache?
                                     */
                                    if (job != null)
                                    {
                                        /*
                                         * Yes, the job was deserialized with
                                         * success.
                                         */

                                        /*
                                         * Update the "pending" field in the job
                                         * class and store it back in the cache.
                                         */
                                        job.setPending(false);
                                        JobCacheManager.store(job);

                                        ContentValues cv = new ContentValues();
                                        boolean res;
                                        cv.put(JobQueueDBHelper.JOB_TIMESTAMP,
                                                jobIDdependent.getTimeStamp());
                                        cv.put(JobQueueDBHelper.JOB_PRODUCT_ID,
                                                jobIDdependent.getProductID());
                                        cv.put(JobQueueDBHelper.JOB_TYPE,
                                                jobIDdependent.getJobType());
                                        cv.put(JobQueueDBHelper.JOB_SKU, jobIDdependent.getSKU());
                                        cv.put(JobQueueDBHelper.JOB_ATTEMPTS, 0);
                                        cv.put(JobQueueDBHelper.JOB_SERVER_URL,
                                                jobIDdependent.getUrl());
                                        res = insert(cv, false);
                                        if (res != true) {
                                            CommonUtils.warn(
                                                    TAG,
                                                    "Unable to add a job to the failed table"
                                                            + " timestamp="
                                                            + jobIDdependent.getTimeStamp()
                                                            + " jobtype="
                                                            + jobIDdependent.getJobType()
                                                            + " prodID="
                                                            + jobIDdependent.getProductID()
                                                            + " SKU=" + jobIDdependent.getSKU()
                                                            + "fromPendingTable="
                                                            + fromPendingTable
                                                            + " moveToFailedTable="
                                                            + moveToFailedTable);
                                            JobCacheManager.removeFromCache(jobIDdependent);
                                            global_res = false;
                                        } else {
                                            changeSummary(jobIDdependent, false, true);
                                        }
                                    }
                                    else
                                    {
                                        /*
                                         * We didn't manage to deserialize job
                                         * from the cache.
                                         */
                                        JobCacheManager.removeFromCache(jobIDdependent);
                                        global_res = false;
                                    }
                                }
                            } else {
                                /* We didn't succeed deleting the dependent job. */
                                global_res = false;
                            }
                        }
                    }
                } else {
                    /*
                     * We didn't succeed deleting the specified job from the
                     * queue
                     */
                    global_res = false;
                }

                dbClose();

                return global_res;
            }
        }
    }

    public JobQueue(Context context) {
        mDbHelper = new JobQueueDBHelper(context);

        refreshJobsSummary();
    }

    /* ============================================================== */
    /*
     * JobDetail related stuff. The code here can be used to get a more detailed
     * (the job summary) information about queue state. Most of the time each
     * JobDetail represents a single job but in case of image upload jobs we
     * bundle them in one job detail for each SKU.
     */
    /* ============================================================== */
    public static class JobDetail {
        public String productName;
        /*
         * Used only in case of sell jobs. Specifies how many items the user has
         * choosen to sell.
         */
        public double soldItemsCount;
        public String SKU;
        public int jobType;
        /*
         * TODO: We probably don't need this variable. We can get images count
         * by checking the number of elements in the jobIdList.
         */
        public int imagesCount;
        public long timestamp;

        /* Used only for shipment creation and multiple prod. sell job. */
        public String orderIncrementID;

        /* Used only for "add to cart" job. */
        public String transactionID;

        /*
         * JobIDs of all jobs associated with this JobDetail. This can be used
         * to remove all of them from the queue.
         */
        public List<JobID> jobIDList = new ArrayList<JobID>();
    }

    /*
     * Remove all jobs associated with a JobDetail from the queue (either from
     * pending or failed table)
     */
    public void deleteJobEntries(JobDetail jobDetail, boolean fromPendingTable) {
        synchronized (sQueueSynchronizationObject) {
            for (int i = 0; i < jobDetail.jobIDList.size(); i++) {
                deleteJobFromQueue(jobDetail.jobIDList.get(i), fromPendingTable, true, false);
            }
        }
    }

    /*
     * Move all jobs associated with a JobDetail from the failed table to the
     * pending table.
     */
    public void retryJobDetail(JobDetail jobDetail) {
        synchronized (sQueueSynchronizationObject) {
            for (int i = 0; i < jobDetail.jobIDList.size(); i++) {
                retryJob(jobDetail.jobIDList.get(i));
            }
        }
    }

    /*
     * Get a list of JobDetails for a given table (it's almost like a list of
     * jobs but we had to create a new class for this because there was a
     * requirement that image upload jobs have to be bundled as one)
     */
    public List<JobDetail> getJobDetailList(boolean pendingTable) {
        synchronized (sQueueSynchronizationObject) {
            List<JobDetail> list = new ArrayList<JobDetail>();
            dbOpen();

            Map<String, Object> imageSKUMap = new HashMap<String, Object>();

            Cursor c = query(new String[] {
                    JobQueueDBHelper.JOB_SKU, JobQueueDBHelper.JOB_TYPE,
                    JobQueueDBHelper.JOB_TIMESTAMP, JobQueueDBHelper.JOB_PRODUCT_ID,
                    JobQueueDBHelper.JOB_SERVER_URL
            }, null, null,
                    JobQueueDBHelper.JOB_ATTEMPTS + " ASC, " + JobQueueDBHelper.JOB_TIMESTAMP
                            + " ASC", null,
                    pendingTable);

            for (; c.moveToNext() != false;) {
                String SKU = c.getString(c.getColumnIndex(JobQueueDBHelper.JOB_SKU));
                String serverUrl = c.getString(c.getColumnIndex(JobQueueDBHelper.JOB_SERVER_URL));
                int type = c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_TYPE));
                int pid = c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_PRODUCT_ID));
                long timestamp = c.getLong(c.getColumnIndex(JobQueueDBHelper.JOB_TIMESTAMP));

                if (type != MageventoryConstants.RES_UPLOAD_IMAGE) {
                    JobDetail detail = new JobDetail();
                    detail.SKU = SKU;
                    detail.jobType = type;
                    detail.timestamp = timestamp;
                    detail.jobIDList.add(new JobID(timestamp, pid, type, SKU, serverUrl));

                    Job job = JobCacheManager.restoreProductCreationJob(SKU, serverUrl);

                    if (job != null) {
                        detail.productName = (String) job
                                .getExtraInfo(MageventoryConstants.MAGEKEY_PRODUCT_NAME);
                    } else {
                        detail.productName = "Unable to deserialize job file.";

                        if (type == MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE)
                        {
                            /*
                             * If we are here it means that product creation
                             * file is not in the cache but we can try to get
                             * the product name from the edit job file.
                             */

                            Job editJob = JobCacheManager.restoreEditJob(SKU, serverUrl);

                            if (editJob != null)
                            {
                                detail.productName = (String) editJob
                                        .getExtraInfo(MageventoryConstants.MAGEKEY_PRODUCT_NAME);
                            }
                        }
                        else if (type == MageventoryConstants.RES_CATALOG_PRODUCT_SUBMIT_TO_TM)
                        {
                            /* Try to get product name from product details. */
                            Product p = JobCacheManager.restoreProductDetails(SKU, serverUrl);

                            if (p != null)
                            {
                                detail.productName = (String) p.getName();
                            }
                        }
                    }

                    /* attach qty information to the sell job detail object */
                    if (type == MageventoryConstants.RES_CATALOG_PRODUCT_SELL)
                    {
                        Job sellJob = JobCacheManager.restore(new JobID(timestamp, pid, type, SKU,
                                serverUrl));

                        if (sellJob != null)
                        {
                            detail.soldItemsCount = Double.parseDouble((String) sellJob
                                    .getExtraInfo(MageventoryConstants.MAGEKEY_PRODUCT_QUANTITY));
                        }
                        else
                        {
                            detail.soldItemsCount = -1;
                        }
                    }

                    if (type == MageventoryConstants.RES_ORDER_SHIPMENT_CREATE)
                    {
                        Job shipmentJob = JobCacheManager.restore(new JobID(timestamp, pid, type,
                                SKU, serverUrl));

                        if (shipmentJob != null)
                        {
                            detail.orderIncrementID = (String) shipmentJob
                                    .getExtraInfo(MageventoryConstants.EKEY_SHIPMENT_ORDER_INCREMENT_ID);
                        }
                        else
                        {
                            detail.orderIncrementID = "Unknown";
                        }
                    }

                    if (type == MageventoryConstants.RES_ADD_PRODUCT_TO_CART)
                    {
                        Job addProductToCartJob = JobCacheManager.restore(new JobID(timestamp, pid,
                                type, SKU, serverUrl));

                        if (addProductToCartJob != null)
                        {
                            detail.transactionID = (String) addProductToCartJob
                                    .getExtraInfo(MageventoryConstants.MAGEKEY_PRODUCT_TRANSACTION_ID);
                        }
                        else
                        {
                            detail.transactionID = "Unknown";
                        }
                    }

                    if (type == MageventoryConstants.RES_SELL_MULTIPLE_PRODUCTS)
                    {
                        // we're using job timestamp as temporary
                        // orderIncrementID in case of this type of job
                        detail.orderIncrementID = "" + timestamp;
                    }

                    list.add(detail);
                } else {
                    if (imageSKUMap.containsKey(SKU)) {
                        JobDetail detail = (JobDetail) imageSKUMap.get(SKU);
                        detail.imagesCount++;
                        detail.jobIDList.add(new JobID(timestamp, pid, type, SKU, serverUrl));
                    } else {
                        JobDetail detail = new JobDetail();
                        detail.SKU = SKU;
                        detail.jobType = type;
                        detail.imagesCount = 1;
                        detail.productName = "tmp name";
                        detail.timestamp = timestamp;
                        detail.jobIDList.add(new JobID(timestamp, pid, type, SKU, serverUrl));

                        List<Job> jobs = JobCacheManager.restoreImageUploadJobs(SKU, serverUrl);

                        if (jobs != null && jobs.size() > 0) {
                            detail.productName = (String) jobs.get(0).getExtraInfo(
                                    MageventoryConstants.MAGEKEY_PRODUCT_NAME);
                        } else {
                            detail.productName = "Unable to deserialize job file.";
                        }

                        list.add(detail);

                        imageSKUMap.put(SKU, detail);
                    }
                }
            }

            c.close();
            dbClose();

            return list;
        }
    }

    /* ============================================================== */
    /*
     * Jobs summary. The stuff here can be used to retrieve and get notified
     * asynchronously about the summary of what is the status of the job queue
     * (how many jobs of each type are either failed or pending). Currently we
     * show this info in the main activity. It gets refreshed automatically
     * thanks to the callbacks mechanisms this code here provides.
     */
    /* ============================================================== */


    /* An object which hold all the data about the jobs summary. */
    private static JobsSummary mJobsSummary;

    /* Stores number of jobs for each job type. */
    public static class JobsCount implements Parcelable {
        public int newProd;
        public int photo;
        public int edit;
        public int sell;
        public int other;

        public JobsCount() {
        }

        public int getTotal() {
            return newProd + photo + edit + sell + other;
        }

        /*****************************
         * PARCELABLE IMPLEMENTATION *
         *****************************/
        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(newProd);
            out.writeInt(photo);
            out.writeInt(edit);
            out.writeInt(sell);
            out.writeInt(other);
        }

        public static final Parcelable.Creator<JobsCount> CREATOR = new Parcelable.Creator<JobsCount>() {
            @Override
            public JobsCount createFromParcel(Parcel in) {
                return new JobsCount(in);
            }

            @Override
            public JobsCount[] newArray(int size) {
                return new JobsCount[size];
            }
        };

        private JobsCount(Parcel in) {
            newProd = in.readInt();
            photo = in.readInt();
            edit = in.readInt();
            sell = in.readInt();
            other = in.readInt();
        }
    }

    /*
     * Stores number of jobs for each job type and for each database table
     * (failed, pending)
     */
    public static class JobsSummary implements Parcelable {
        public JobsCount failed;
        public JobsCount pending;

        public JobsSummary() {
        }

        /*****************************
         * PARCELABLE IMPLEMENTATION *
         *****************************/
        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeParcelable(failed, flags);
            out.writeParcelable(pending, flags);
        }

        public static final Parcelable.Creator<JobsSummary> CREATOR = new Parcelable.Creator<JobsSummary>() {
            @Override
            public JobsSummary createFromParcel(Parcel in) {
                return new JobsSummary(in);
            }

            @Override
            public JobsSummary[] newArray(int size) {
                return new JobsSummary[size];
            }
        };

        private JobsSummary(Parcel in) {
            failed = in.readParcelable(JobsSummary.class.getClassLoader());
            pending = in.readParcelable(JobsSummary.class.getClassLoader());
        }
    }

    /* Interface for the listener listening on summary changed event. */
    public static interface JobSummaryChangedListener {
        void OnJobSummaryChanged(JobsSummary jobsSummary);
    }

    /*
     * Initializing job summary before anything else from this class gets
     * called.
     */
    static {
        mJobsSummary = new JobsSummary();
        mJobsSummary.pending = new JobsCount();
        mJobsSummary.failed = new JobsCount();
    }

    /* Count the jobs of a given type in a given table. */
    private int getJobCount(int jobType, boolean pendingJob) {
        synchronized (sQueueSynchronizationObject) {
            int count = 0;
            dbOpen();
            Cursor c = query(new String[] {
                JobQueueDBHelper.JOB_TIMESTAMP
            }, JobQueueDBHelper.JOB_TYPE + "=?",
                    new String[] {
                        "" + jobType
                    }, null, null, pendingJob);

            count = c.getCount();

            c.close();

            dbClose();

            return count;
        }
    }

    /*
     * Update the summary. Every time something happens in the queue that
     * changes the jobs summary this function will be called.
     */
    private void changeSummary(JobID jobId, boolean pendingJobChanged, boolean added) {
        int change = (added ? 1 : -1);

        switch (jobId.getJobType()) {
            case MageventoryConstants.RES_CATALOG_PRODUCT_CREATE:
                if (pendingJobChanged) {
                    mJobsSummary.pending.newProd += change;
                } else {
                    mJobsSummary.failed.newProd += change;
                }
                break;
            case MageventoryConstants.RES_UPLOAD_IMAGE:
                if (pendingJobChanged) {
                    mJobsSummary.pending.photo += change;
                } else {
                    mJobsSummary.failed.photo += change;
                }
                break;
            case MageventoryConstants.RES_CATALOG_PRODUCT_SELL:
            case MageventoryConstants.RES_SELL_MULTIPLE_PRODUCTS:
                if (pendingJobChanged) {
                    mJobsSummary.pending.sell += change;
                } else {
                    mJobsSummary.failed.sell += change;
                }
                break;
            case MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE:
                if (pendingJobChanged) {
                    mJobsSummary.pending.edit += change;
                } else {
                    mJobsSummary.failed.edit += change;
                }
                break;
            default:
                if (pendingJobChanged) {
                    mJobsSummary.pending.other += change;
                } else {
                    mJobsSummary.failed.other += change;
                }
                break;
        }
        /* Notify the listener about the change. */
        sendJobSummaryChangedBroadcast(mJobsSummary);
    }


    /**
     * Get and register the broadcast receiver for the job summary changed event
     * 
     * @param TAG
     * @param handler
     * @return
     */
    public static BroadcastReceiver getAndRegisterJobSummaryChangedBroadcastReceiver(
            final String TAG, final JobSummaryChangedListener handler) {
        BroadcastReceiver br = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    CommonUtils.debug(TAG, "Received on job summary changed broadcast message.");
                    JobsSummary summary = intent.getParcelableExtra(JOBS_SUMMARY);
                    handler.OnJobSummaryChanged(summary);
                } catch (Exception ex) {
                    GuiUtils.noAlertError(TAG, ex);
                }
            }
        };
        LocalBroadcastManager.getInstance(MyApplication.getContext()).registerReceiver(br,
                new IntentFilter(JOBS_SUMMARY_CHANGED_EVENT_ACTION));

        handler.OnJobSummaryChanged(mJobsSummary);
        return br;
    }

    /**
     * Register the broadcast receiver for the job summary changed event
     * 
     * @param TAG
     * @param handler
     * @param broadcastReceiverRegisterHandler
     * @return
     */
    public static void registerJobSummaryChangedBroadcastReceiver(
            final String TAG, final JobSummaryChangedListener handler,
            final BroadcastReceiverRegisterHandler broadcastReceiverRegisterHandler) {
        broadcastReceiverRegisterHandler
                .addRegisteredLocalReceiver(getAndRegisterJobSummaryChangedBroadcastReceiver(TAG,
                        handler));
    }

    /**
     * Send job summary changed broadcast
     */
    public static void sendJobSummaryChangedBroadcast(JobsSummary jobsSummary) {
        Intent intent = new Intent(JOBS_SUMMARY_CHANGED_EVENT_ACTION);
        intent.putExtra(JOBS_SUMMARY, jobsSummary);
        LocalBroadcastManager.getInstance(MyApplication.getContext()).sendBroadcast(intent);
    }

    /*
     * In cache the job summary is totally out of sync with the queue (like for
     * example when the appliation starts) this function needs to be called.
     * Each next change to the queue will just use changeSummary() function.
     */
    private void refreshJobsSummary() {
        mJobsSummary.pending.newProd = getJobCount(MageventoryConstants.RES_CATALOG_PRODUCT_CREATE,
                true);
        mJobsSummary.pending.photo = getJobCount(MageventoryConstants.RES_UPLOAD_IMAGE, true);
        mJobsSummary.pending.sell = getJobCount(MageventoryConstants.RES_CATALOG_PRODUCT_SELL, true)
                +
                getJobCount(MageventoryConstants.RES_SELL_MULTIPLE_PRODUCTS, true);
        mJobsSummary.pending.edit = getJobCount(MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE,
                true);
        mJobsSummary.pending.other = getJobCount(
                MageventoryConstants.RES_CATALOG_PRODUCT_SUBMIT_TO_TM, true) +
                getJobCount(MageventoryConstants.RES_ORDER_SHIPMENT_CREATE, true) +
                getJobCount(MageventoryConstants.RES_ADD_PRODUCT_TO_CART, true);

        mJobsSummary.failed.newProd = getJobCount(MageventoryConstants.RES_CATALOG_PRODUCT_CREATE,
                false);
        mJobsSummary.failed.photo = getJobCount(MageventoryConstants.RES_UPLOAD_IMAGE, false);
        mJobsSummary.failed.sell = getJobCount(MageventoryConstants.RES_CATALOG_PRODUCT_SELL, false)
                +
                getJobCount(MageventoryConstants.RES_SELL_MULTIPLE_PRODUCTS, false);
        mJobsSummary.failed.edit = getJobCount(MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE,
                false);
        mJobsSummary.failed.other = getJobCount(
                MageventoryConstants.RES_CATALOG_PRODUCT_SUBMIT_TO_TM, false) +
                getJobCount(MageventoryConstants.RES_ORDER_SHIPMENT_CREATE, false) +
                getJobCount(MageventoryConstants.RES_ADD_PRODUCT_TO_CART, false);

        /* Notify the listener about the change. */
        sendJobSummaryChangedBroadcast(mJobsSummary);
    }

    /* ===================================================== */
    /* Operations related to dumping queue database tables */
    /* ===================================================== */

    /*
     * Dump both failed and pending tables to files. (csv format). Returns true
     * on success.
     */
    /* Pass null as "dir" to use the default directory. */
    public boolean dumpQueueDatabase(File dir)
    {
        synchronized (sQueueSynchronizationObject) {

            boolean out = true;

            File pendingTableDumpFile = JobCacheManager.getQueuePendingTableDumpFile(dir);
            File failedTableDumpFile = JobCacheManager.getQueueFailedTableDumpFile(dir);

            long jobTimestamp;
            int jobProductID;
            int jobType;
            String jobSKU;
            String jobServerURL;

            FileWriter fileWriter = null;

            try {

                for (int i = 0; i < 2; i++)
                {
                    dbOpen();
                    Cursor c = null;

                    switch (i)
                    {
                        case 0:
                            c = query(new String[] {
                                    JobQueueDBHelper.JOB_TIMESTAMP,
                                    JobQueueDBHelper.JOB_PRODUCT_ID,
                                    JobQueueDBHelper.JOB_TYPE, JobQueueDBHelper.JOB_SKU,
                                    JobQueueDBHelper.JOB_SERVER_URL
                            },
                                    null, null, JobQueueDBHelper.JOB_TIMESTAMP + " ASC", null, true);
                            fileWriter = new FileWriter(pendingTableDumpFile, false);
                            break;
                        case 1:
                            c = query(new String[] {
                                    JobQueueDBHelper.JOB_TIMESTAMP,
                                    JobQueueDBHelper.JOB_PRODUCT_ID,
                                    JobQueueDBHelper.JOB_TYPE, JobQueueDBHelper.JOB_SKU,
                                    JobQueueDBHelper.JOB_SERVER_URL
                            },
                                    null, null, JobQueueDBHelper.JOB_TIMESTAMP + " ASC", null,
                                    false);
                            fileWriter = new FileWriter(failedTableDumpFile, false);
                            break;
                    }

                    fileWriter.write("TIMESTAMP, PRODUCT_ID, TYPE, SKU, URL\n");

                    while (c.moveToNext())
                    {
                        jobTimestamp = c.getLong(c.getColumnIndex(JobQueueDBHelper.JOB_TIMESTAMP));
                        jobProductID = c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_PRODUCT_ID));
                        jobType = c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_TYPE));
                        jobSKU = c.getString(c.getColumnIndex(JobQueueDBHelper.JOB_SKU));
                        jobServerURL = c.getString(c
                                .getColumnIndex(JobQueueDBHelper.JOB_SERVER_URL));

                        fileWriter.write(jobTimestamp + ", " + jobProductID + ", " + jobType + ", "
                                + jobSKU + ", " + jobServerURL + "\n");
                    }

                    fileWriter.close();

                    c.close();
                    dbClose();
                }

            } catch (IOException e) {
                CommonUtils.error(TAG, e);
                out = false;
            }

            return out;
        }
    }

    /* ============================================================== */
    /* DB accessors */
    /* ============================================================== */
    private int delete(String selection, String[] selectionArgs, boolean pendingTable) {
        String table = pendingTable ? JobQueueDBHelper.TABLE_PENDING_NAME
                : JobQueueDBHelper.TABLE_FAILED_NAME;
        return mDB.delete(table, selection, selectionArgs);
    }

    private boolean insert(ContentValues values, boolean pendingTable) {
        String table = pendingTable ? JobQueueDBHelper.TABLE_PENDING_NAME
                : JobQueueDBHelper.TABLE_FAILED_NAME;
        final long id = mDB.insert(table, null, values);
        if (id == -1) {
            return false;
        }
        return true;
    }

    private Cursor query(String[] columns, String selection, String[] selectionArgs,
            String sortOrder, String limit,
            boolean pendingTable) {
        String table = pendingTable ? JobQueueDBHelper.TABLE_PENDING_NAME
                : JobQueueDBHelper.TABLE_FAILED_NAME;
        return mDB.query(table, columns, selection, selectionArgs, null, null, sortOrder, limit);
    }

    private Cursor query(String[] columns, String selection, String[] selectionArgs,
            String groupBy, String having,
            String sortOrder, String limit, boolean pendingTable) {
        String table = pendingTable ? JobQueueDBHelper.TABLE_PENDING_NAME
                : JobQueueDBHelper.TABLE_FAILED_NAME;
        return mDB.query(table, columns, selection, selectionArgs, groupBy, having, sortOrder,
                limit);
    }

    private boolean update(ContentValues values, String selection, String[] selectionArgs,
            boolean pendingTable) {
        String table = pendingTable ? JobQueueDBHelper.TABLE_PENDING_NAME
                : JobQueueDBHelper.TABLE_FAILED_NAME;
        int count = mDB.update(table, values, selection, selectionArgs);

        if (count < 1) {
            return false;
        }
        return true;
    }

    private boolean isEmpty(boolean pendingTable)
    {
        String table = pendingTable ? JobQueueDBHelper.TABLE_PENDING_NAME
                : JobQueueDBHelper.TABLE_FAILED_NAME;

        Cursor cur = mDB.rawQuery("SELECT COUNT(*) FROM " + table, null);
        if (cur != null) {
            cur.moveToFirst(); // Always one row returned.
            if (cur.getInt(0) == 0) { // Zero count means empty table.
                return true;
            }
            else
            {
                return false;
            }
        }

        return false;
    }

    private void dbOpen() {
        mDB = mDbHelper.getWritableDatabase();
    }

    private void dbClose() {
        mDB.close();
    }
}
