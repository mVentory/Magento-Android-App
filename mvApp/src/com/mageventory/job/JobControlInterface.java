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
import java.util.Iterator;
import java.util.List;

import android.content.Context;

import com.mageventory.MageventoryConstants;
import com.mageventory.job.JobQueue.JobDetail;
import com.mageventory.model.Product;

/* Additional abstraction layer between UI and any code used for manipulating jobs (job queue, job cache manager etc.)*/
public class JobControlInterface {

    private Context mContext;
    private JobQueue mJobQueue;
    private ExternalImagesJobQueue mExternalImagesJobQueue;

    public JobControlInterface(Context context) {
        mContext = context;
        mJobQueue = new JobQueue(context);
        mExternalImagesJobQueue = new ExternalImagesJobQueue(context);
    }

    /**
     * Register callbacks for the all jobs in the list
     * 
     * @param jobs
     * @param jobCallback
     * @return true in case all jobs was registered successfully. false in case
     *         at least one job was absent
     */
    public boolean registerJobCallbacksAndRemoveAbsentJobs(List<Job> jobs, JobCallback jobCallback) {
        boolean result = true;

        Iterator<Job> i = jobs.iterator();
        while (i.hasNext()) {
            Job job = i.next();
            if (!registerJobCallback(job.getJobID(), jobCallback)) {
                result &= false;
                i.remove();
            }
        }
        return result;
    }

    /**
     * Unregister all job callbacks from the list
     * 
     * @param jobs
     */
    public void unregisterJobCallbacks(List<Job> jobs) {
        for (Job job : jobs) {
            deregisterJobCallback(job.getJobID(), null);
        }
    }

    /*
     * Register a callback on a job to get informed about its status changes. If
     * the job exists in the cache when this function is called the callback
     * function is going to be called also right after the callback gets
     * registered.
     */
    public boolean registerJobCallback(JobID jobID, JobCallback jobCallback) {
        /*
         * This synchronisation is necessary here as we want to make sure we
         * receive the callback when the job gets complete. If we didn't
         * synchronise here the job might get finished after we check the cache
         * but before we add the callback in which case UI would think the job
         * is not complete and it would never receive a callback with
         * information that it completed.
         */
        synchronized (JobService.sCallbackListSynchronizationObject) {
            Job job = JobCacheManager.restore(jobID);

            if (job != null) {
                jobCallback.onJobStateChange(job);
                JobService.addCallback(jobID, jobCallback);
                return true;
            }

            return false;
        }
    }

    /* Can pass null as jobCallback here to deregister all callbacks for a job. */
    public void deregisterJobCallback(JobID jobID, JobCallback jobCallback) {
        JobService.removeCallback(jobID, jobCallback);
    }

    public boolean isNewProductJobInThePendingTable(String SKU, String URL)
    {
        return mJobQueue.isNewProductJobInThePendingTable(SKU, URL);
    }

    public void addExternalImagesJob(ExternalImagesJob job) {

        mExternalImagesJobQueue.add(job);

        // Notify the service there is a new job in the queue
        JobService.wakeUp(mContext);
    }

    /*
     * Add a job to the queue. If this is not a product creation job and it
     * doesn't contain a product id then we try to restore the product details
     * from the cache, retrieve the product id and fill the missing id in the
     * new job.
     */
    public void addJob(Job job) {
        /*
         * In case a job needs product id we check whether we have it in the
         * cache. If it's there we use it. This needs to be synchronised. We
         * have to make sure that once we establish we don't have product id in
         * the cache then it doesn't suddenly appear there before we manage to
         * add a job to the queue because that would mean the job could never
         * get the product id assigned.
         */
        if (job.getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE
                || job.getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_SELL
                || job.getJobType() == MageventoryConstants.RES_ADD_PRODUCT_TO_CART
                || job.getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_SUBMIT_TO_TM) {
            synchronized (JobQueue.sQueueSynchronizationObject) {
                Product product = JobCacheManager.restoreProductDetails(job.getJobID().getSKU(),
                        job.getJobID().getUrl());

                if (product != null) {
                    job.getJobID().setProductID(Integer.parseInt(product.getId()));
                }

                mJobQueue.add(job);
            }
        } else {
            mJobQueue.add(job);
        }

        // Notify the service there is a new job in the queue
        JobService.wakeUp(mContext);
    }

    /*
     * Adding an edit job to the queue is a special case because there can only
     * ever be just one edit job for a given SKU. That's why we have a separate
     * function for that here. It returns true if the job was created (or
     * updated) successfully and returns false if the service is currently
     * processing the job so it shouldn't be touched.
     */
    public boolean addEditJob(Job newEditJob) {
        /*
         * In case a job needs product id we check whether we have it in the
         * cache. If it's there we use it. This needs to be synchronised. We
         * have to make sure that once we establish we don't have product id in
         * the cache then it doesn't suddenly appear there before we manage to
         * add a job to the queue because that would mean the job could never
         * get the product id assigned. The second reason why we hold a lock
         * here is because we check if there are any edit jobs currently being
         * processed and we don't want a new edit job to start or stop being
         * processed when this function is being executed.
         */
        synchronized (JobQueue.sQueueSynchronizationObject) {
            Job currentJob = JobQueue.getCurrentJob();

            /*
             * If the currently pending job is and edit job and if it has the
             * same SKU that the job we have a problem and can do nothing. Just
             * return false and let the caller call us later.
             */
            if (currentJob != null
                    && currentJob.getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE &&
                    currentJob.getSKU().equals(newEditJob.getSKU()))
            {
                return false;
            }

            Product product = JobCacheManager.restoreProductDetails(newEditJob.getSKU(), newEditJob
                    .getJobID().getUrl());

            if (product != null) {
                newEditJob.getJobID().setProductID(Integer.parseInt(product.getId()));
            }

            Job existingJob = JobCacheManager.restoreEditJob(newEditJob.getSKU(), newEditJob
                    .getJobID().getUrl());

            /* Check if there already is an edit job in the cache. */
            if (existingJob != null)
            {
                /*
                 * There is an edit job already in the cache. Let's keep its
                 * timestamp.
                 */
                newEditJob.getJobID().setTimeStamp(existingJob.getJobID().getTimeStamp());

                /* Check in which table the existing job resides. */
                if (existingJob.getPending())
                {
                    /*
                     * The edit job is in the pending table. Let's reset it's
                     * failure counter.
                     */
                    mJobQueue.resetFailureCounter(existingJob.getJobID());
                }
                else
                {
                    /*
                     * The edit job is in the failed table. Let's move it back
                     * to the pending table. In this case the function for
                     * retrying is also reseting the failure counter so no need
                     * to do that separately.
                     */
                    mJobQueue.resetFailureCounter(existingJob.getJobID());
                }

                /* Save the new job in the cache. */
                JobCacheManager.store(newEditJob);
            }
            else
            {
                /*
                 * There is no existing edit job in the queue. We just simply
                 * need to add a new one in that case.
                 */
                mJobQueue.add(newEditJob);
            }

            JobCacheManager.remergeProductDetailsWithEditJob(newEditJob.getSKU(), newEditJob
                    .getJobID().getUrl());
        }

        // Notify the service there is a new job in the queue
        JobService.wakeUp(mContext);

        return true;
    }

    /* Just add a job to the queue without any additional processing. */
    public void addJobSimple(Job job) {
        mJobQueue.add(job);

        // Notify the service there is a new job in the queue
        JobService.wakeUp(mContext);
    }

    /* Get a list of all image upload jobs for a given SKU from the cache. */
    public List<Job> getAllImageUploadJobs(String SKU, String url) {
        return JobCacheManager.restoreImageUploadJobs(SKU, url);
    }

    /* Get a list of all sell jobs for a given SKU from the cache. */
    public List<Job> getAllSellJobs(String SKU, String url) {
        return JobCacheManager.restoreSellJobs(SKU, url);
    }

    /* ===================================================== */
    /* Operations related to JobDetail class. */
    /* ===================================================== */

    /*
     * Return information about jobs from a given table (pending/failed) in a
     * form of a list. The image upload jobs for a given product are getting
     * returned as one list entry. Each list entry then provides additional list
     * of image upload jobs that can be used to get more info about each
     * particular job.
     */
    public List<JobDetail> getJobDetailList(boolean pendingTable) {
        return mJobQueue.getJobDetailList(pendingTable);
    }

    /*
     * Delete a job detail. When deleting image upload detail containing
     * multiple image upload jobs all of them are deleted.
     */
    public void deleteJobEntries(JobDetail jobDetail, boolean fromPendingTable) {
        mJobQueue.deleteJobEntries(jobDetail, fromPendingTable);
    }

    /*
     * Move all of the jobs represented by a given JobDetail from the failed
     * table to the pending table and reset their failure counter to 0.
     */
    public void retryJobDetail(JobDetail jobDetail) {
        mJobQueue.retryJobDetail(jobDetail);

        // Notify the service there is a new job in the queue
        JobService.wakeUp(mContext);
    }

    /*
     * Move a job from failed to pending table so that it can be picked up by
     * the service and retried.
     */
    public void retryJob(JobID jobID)
    {
        mJobQueue.retryJob(jobID);

        // Notify the service there is a new job in the queue
        JobService.wakeUp(mContext);
    }

    /**
     * Delete a job from the queue.
     * 
     * @param jobID
     */
    public void cancelJob(JobID jobID) {
        mJobQueue.deleteJobFromQueue(jobID, true, true, false);
    }

    /* Delete a failed job from the queue. */
    public void deleteFailedJob(JobID jobID)
    {
        mJobQueue.deleteJobFromQueue(jobID, false, true, false);
    }

    /* ===================================================== */
    /* Operations related to dumping queue database tables */
    /* ===================================================== */

    /*
     * Dump both failed and pending tables to files. (csv format). Returns true
     * on success.
     */
    /* Pass null as "directoryPath" to use the default directory. */
    public boolean dumpQueueDatabase(File dir)
    {
        return mJobQueue.dumpQueueDatabase(dir);
    }
}
