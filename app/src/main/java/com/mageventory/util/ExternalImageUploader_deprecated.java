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

package com.mageventory.util;

import java.io.File;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCacheManager.ProductDetailsExistResult;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;

public class ExternalImageUploader_deprecated implements MageventoryConstants {
	
    static final String TAG = ExternalImageUploader_deprecated.class.getSimpleName();

    private static final String TAG_EXTERNAL_IMAGE_UPLOADER = "GALLERY_EXTERNAL_IMAGE_UPLOADER";

    private Context mContext;
    private LinkedList<String> mImagesToUploadQueue;
    private Object mQueueSynchronisationObject = new Object();
    private Settings mSettings;

    private class UploadImageTask extends AsyncTask<String, Void, Boolean> implements
            OperationObserver {

        private String mSKU;
        private String mURL;
        private String mUser;
        private String mPassword;

        private SettingsSnapshot mSettingsSnapshot;
        private String mImagePath, mOriginalImagePath;
        private JobControlInterface mJobControlInterface;
        private ResourceServiceHelper mResHelper = ResourceServiceHelper.getInstance();
        private int mLoadReqId = INVALID_REQUEST_ID;
        private CountDownLatch mDoneSignal;
        private boolean mProductLoadSuccess;
        private Settings mSettings;
        private boolean mSKUTimestampModeSelected;
        private boolean mForceSKUTimestampMode;
        private String mProductDetailsSKU;

        /*
         * If we are not in "sku timestamp mode" (we are taking sku from the
         * file name) and the sku doesn't exist in the cache nor on the server
         * or we cannot check if it exists on the server then we want to retry
         * the image upload in "sku timestamp mode"
         */
        private boolean retryFlag;

        public UploadImageTask(String imagePath, boolean forceSKUTimestampMode)
        {
            CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true, "UploadImageTask(" + imagePath
                    + ","
                    + forceSKUTimestampMode + ")");
            mForceSKUTimestampMode = forceSKUTimestampMode;

            mSettings = new Settings(mContext);

            mOriginalImagePath = imagePath;
            mImagePath = imagePath;
            mJobControlInterface = new JobControlInterface(mContext);
        }

        /* Return true on success */
        private boolean getSKUAndOtherData()
        {
            long profileID = -1;
            File currentFile = new File(mImagePath);
            String fileName = currentFile.getName();

            if (!currentFile.exists())
            {
                if (fileName.contains("__"))
                {
                    mSKU = fileName.substring(0, fileName.indexOf("__"));
                    String fileNameWithoutSKU = fileName.substring(fileName.indexOf("__") + 2);

                    currentFile = new File(currentFile.getParentFile(), fileNameWithoutSKU);
                    fileName = fileNameWithoutSKU;

                    mImagePath = currentFile.getAbsolutePath();
                }
            }

            if (!currentFile.exists())
            {
                CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
                        "getSKUAndOtherData(); The image does not exist: " + mImagePath);
                return false;
            }

            if (mSKU == null && !mForceSKUTimestampMode && fileName.contains("__"))
            {
                mSKU = fileName.substring(0, fileName.indexOf("__"));
            }
            else if (mSKU == null)
            {
                return false;
            }

            mSKUTimestampModeSelected = false;
            mURL = mSettings.getUrl();
            mUser = mSettings.getUser();
            mPassword = mSettings.getPass();

            /* TODO: We are not using timestamps file for now. */
            /*
             * else { mSKUTimestampModeSelected = true; try { ExifInterface exif
             * = new ExifInterface(mImagePath); String dateTime =
             * exif.getAttribute(ExifInterface.TAG_DATETIME);
             * CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
             * "getSKUAndOtherData(); Retrieved exif timestamp from the file: "
             * + dateTime); String escapedSkuProfileID =
             * JobCacheManager.getSkuProfileIDForExifTimeStamp(mContext,
             * dateTime); CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
             * "getSKUAndOtherData(); Retrieved escaped SKU and profile ID from the timestamps file: "
             * + escapedSkuProfileID); if (escapedSkuProfileID != null) { String
             * escapedSKU = escapedSkuProfileID.split(" ")[0]; String
             * profileIDString = escapedSkuProfileID.split(" ")[1]; mSKU=
             * URLDecoder.decode(escapedSKU, "UTF-8"); profileID =
             * Long.parseLong(profileIDString);
             * CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
             * "getSKUAndOtherData(); Decoded sku and profile ID: " + mSKU +
             * ", " + profileID ); Settings s; try { s = new Settings(mContext,
             * profileID); } catch (ProfileIDNotFoundException e) {
             * e.printStackTrace();
             * CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
             * "getSKUAndOtherData(); Profile is missing. Moving the image to BAD_PICS."
             * ); // Profile is missing. Move the file to the "bad pics" dir.
             * boolean success = moveImageToBadPics(currentFile); if (success) {
             * CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
             * "getSKUAndOtherData(); Image moved to BAD_PICS with success."); }
             * else { CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
             * "getSKUAndOtherData(); Moving image to BAD_PICS FAILED."); }
             * return false; } mURL = s.getUrl(); mUser = s.getUser(); mPassword
             * = s.getPass(); CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER,
             * true, "getSKUAndOtherData(); Retrieving url from the profile: " +
             * mURL ); } else { CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER,
             * true,
             * "getSKUAndOtherData(); Retrieved escaped SKU and profile ID are null. Moving the image to BAD_PICS."
             * ); boolean success = moveImageToBadPics(currentFile); if
             * (success) { CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
             * "getSKUAndOtherData(); Image moved to BAD_PICS with success."); }
             * else { CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
             * "getSKUAndOtherData(); Moving image to BAD_PICS FAILED."); }
             * return false; } } catch (IOException e) { e.printStackTrace();
             * return false; } }
             */

            mSettingsSnapshot = new SettingsSnapshot(mContext);
            mSettingsSnapshot.setUser(mUser);
            mSettingsSnapshot.setPassword(mPassword);
            mSettingsSnapshot.setUrl(mURL);

            return true;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... args) {
            boolean res;

            CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
                    "UploadImageTask; doInBackground();");

            res = getSKUAndOtherData();

            if (res == false)
            {
                CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
                        "UploadImageTask; doInBackground(); An attempt of figuring out the SKU failed for the"
                                +
                                "following file: " + mImagePath);
                return false;
            }

            /*
             * Build a path to an image in the product folder where it needs to
             * be placed in order to be uploaded.
             */
            File currentFile = new File(mImagePath);
            File imagesDir = JobCacheManager.getImageUploadDirectory(mSKU, mURL);

            String newFileName = currentFile.getName();

            if (!newFileName.contains("__"))
            {
                newFileName = mSKU + "__" + newFileName;
            }

            File newImageFile = new File(imagesDir, newFileName);

            if (newImageFile.exists())
            {
                newImageFile = new File(imagesDir, getModifedFileName(currentFile));
            }

            JobID jobID = new JobID(INVALID_PRODUCT_ID, RES_UPLOAD_IMAGE, mSKU, null);

            Job uploadImageJob = new Job(jobID, mSettingsSnapshot);

            uploadImageJob.putExtraInfo(
                    MAGEKEY_PRODUCT_IMAGE_NAME,
                    newImageFile.getName().substring(0,
                            newImageFile.getName().toLowerCase().lastIndexOf(".jpg")));

            uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_CONTENT,
                    newImageFile.getAbsolutePath());
            uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_MIME, "image/jpeg");

            boolean doAddJob = true;

            ProductDetailsExistResult existResult = JobCacheManager.productDetailsExist(
                    uploadImageJob.getSKU(),
                    uploadImageJob.getUrl(), true);

            if (existResult.isExisting()) {
                uploadImageJob.getJobID().setSKU(existResult.getSku());
            } else
            {
                // download product details
                final String[] params = new String[2];
                params[0] = GET_PRODUCT_BY_SKU_OR_BARCODE; // ZERO --> Use
                                                           // Product ID , ONE
                                                           // -->
                // Use Product SKU
                params[1] = uploadImageJob.getSKU();

                mResHelper.registerLoadOperationObserver(this);
                mLoadReqId = mResHelper.loadResource(mContext, RES_PRODUCT_DETAILS, params,
                        mSettingsSnapshot);

                mDoneSignal = new CountDownLatch(1);
                while (true) {
                    if (isCancelled()) {
                        return true;
                    }
                    try {
                        if (mDoneSignal.await(1, TimeUnit.SECONDS)) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        return true;
                    }
                }

                mResHelper.unregisterLoadOperationObserver(this);

                if (mProductLoadSuccess == false)
                {
                    CommonUtils.error(TAG, new Exception("Unable to download product details."));
                    doAddJob = false;
                }
                else
                {
                    if (mProductDetailsSKU != null)
                    {
                        uploadImageJob.getJobID().setSKU(mProductDetailsSKU);
                        mProductDetailsSKU = null;
                    }
                }
            }

            if (doAddJob == true)
            {
                retryFlag = false;

                if (currentFile.renameTo(newImageFile) == false)
                {
                    CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
                            "UploadImageTask; Failed to move the file to the right directory before uploading. The dir path: "
                                    + imagesDir.getAbsolutePath());
                    return true;
                }
                else
                {
                    CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
                            "UploadImageTask; Moved file, from: " + currentFile.getAbsolutePath()
                                    + ", to:" + newImageFile.getAbsolutePath());
                }

                mJobControlInterface.addJob(uploadImageJob);
            }
            else
            {
                /*
                 * TODO: Retrying is temporarily turned off since we don't use
                 * gallery timestamp file for now.
                 */
                if (false && mSKUTimestampModeSelected == false)
                {
                    /*
                     * If we are here it means product details are not in the
                     * cache nor on the server (OR product details are not in
                     * the cache and we don't know whether they are on the
                     * server). In this case we retry the upload using the
                     * gallery file.
                     */
                    retryFlag = true;
                }
                else
                {
                    retryFlag = false;

                    boolean success = moveImageToGalleryDir(currentFile);

                    if (success)
                    {
                        CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
                                "UploadImageTask; Image moved to BAD_PICS with success.");
                    }
                    else
                    {
                        CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
                                "UploadImageTask; Moving image to BAD_PICS FAILED.");
                    }
                }
            }

            return true;
        }

        @Override
        public void onLoadOperationCompleted(LoadOperation op) {
            if (op.getOperationRequestId() == mLoadReqId) {

                if (op.getException() == null) {

                    Bundle extras = op.getExtras();
                    if (extras != null && extras.getString(MAGEKEY_PRODUCT_SKU) != null)
                    {
                        mProductDetailsSKU = extras.getString(MAGEKEY_PRODUCT_SKU);
                    }

                    mProductLoadSuccess = true;
                } else {
                    mProductLoadSuccess = false;
                }
                mDoneSignal.countDown();
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (retryFlag)
            {
                CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
                        "UploadImageTask; Retrying with forced sku timestamp mode, for image: "
                                + mImagePath);
                new UploadImageTask(mImagePath, true).execute();
            }
            else
            {
                synchronized (mQueueSynchronisationObject)
                {
                    mImagesToUploadQueue.remove(mOriginalImagePath);
                    processNextImageFromQueue();
                }
            }
        }
    }

    public void clearImageQueue()
    {
        synchronized (mQueueSynchronisationObject)
        {
            CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true, "Clearing image queue.");
            mImagesToUploadQueue.clear();
        }
    }

    private void processNextImageFromQueue()
    {
        if (mImagesToUploadQueue.size() > 0)
        {
            CommonUtils.debug(
                    TAG_EXTERNAL_IMAGE_UPLOADER,
                    true,
                    "processNextImageFromQueue(), processing next image: "
                            + mImagesToUploadQueue.getFirst());
            CommonUtils
                    .debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
                            "processNextImageFromQueue(), image queue size: "
                    + mImagesToUploadQueue.size());

            new UploadImageTask(mImagesToUploadQueue.getFirst(), false).execute();
        }
    }

    public ExternalImageUploader_deprecated(Context c)
    {
        mSettings = new Settings(c);
        mContext = c;
        mImagesToUploadQueue = new LinkedList<String>();
    }

    /*
     * Appends a timestamp at the end of the file name of the file passed and
     * returns the new modified file name as a String.
     */
    private static String getModifedFileName(File imageFile)
    {
        long currentTime = System.currentTimeMillis();

        int lastDotIndex = imageFile.getName().lastIndexOf(".");

        String newFileName;

        if (lastDotIndex == -1)
        {
            newFileName = imageFile.getName() + "_" + currentTime;
        }
        else
        {
            newFileName = imageFile.getName().substring(0, lastDotIndex);
            newFileName = newFileName + "_" + currentTime
                    + imageFile.getName().substring(lastDotIndex);
        }

        return newFileName;
    }

    public static boolean moveImageToBadPics(File imageFile)
    {
        File badPicsDir = JobCacheManager.getBadPicsDir();

        File moveHere = new File(badPicsDir, imageFile.getName());

        /*
         * Append a timestamp to the end of the file name only if it already
         * exists to avoid conflict.
         */
        if (moveHere.exists())
        {
            moveHere = new File(badPicsDir, getModifedFileName(imageFile));
        }

        boolean success = imageFile.renameTo(moveHere);

        return success;
    }

    public boolean moveImageToGalleryDir(File imageFile)
    {
        String imagesDirPath = mSettings.getGalleryPhotosDirectory();
        File renamedFile = new File(imagesDirPath, imageFile.getName());
        boolean success = imageFile.renameTo(renamedFile);

        return success;
    }

    public void scheduleImageUpload(String path)
    {
        synchronized (mQueueSynchronisationObject)
        {
            CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true, "> scheduleImageUpload()");

            if (!mImagesToUploadQueue.contains(path))
            {
                CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
                        "scheduleImageUpload(), adding image path to the queue: " + path);
                mImagesToUploadQueue.addLast(path);

                /*
                 * If the only thing in the queue is the path we just added then
                 * it means we need to wake up the queue because there is
                 * nothing being processed at the moment.
                 */
                if (mImagesToUploadQueue.size() == 1)
                {
                    CommonUtils
                            .debug(TAG_EXTERNAL_IMAGE_UPLOADER, true,
                            "scheduleImageUpload(), the image we added is the only one in the queue. Starting the queue.");
                    processNextImageFromQueue();
                }
            }
            CommonUtils.debug(TAG_EXTERNAL_IMAGE_UPLOADER, true, "< scheduleImageUpload()");
        }
    }
}
