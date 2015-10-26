
package com.mageventory.tasks;

import java.io.File;

import android.content.Intent;

import com.mageventory.job.Job;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.ParcelableJobDetails;
import com.mageventory.jobprocessor.util.UploadImageJobUtils;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.run.CallableWithParameterAndResult;

/**
 * The abstract implementation of the asynchronous task to add upload images job
 * for the specified product
 * 
 * @author Eugene Popovich
 */
public abstract class AbstractAddNewImagesTask extends SimpleAsyncTask {
    /**
     * Tag used for logging
     */
    private static final String TAG = AbstractAddNewImagesTask.class.getSimpleName();
    /**
     * The settings snapshot
     */
    private SettingsSnapshot mSettingsSnapshot;
    /**
     * The product SKU to add image upload jobs to
     */
    private String mProductSku;
    /**
     * The paths to the images which should be uploaded
     */
    private String[] mFilePaths;
    /**
     * Whether the original files should be moved or copied
     */
    boolean mMoveOriginal;
    /**
     * The job controller interface
     */
    protected JobControlInterface mJobControlInterface;

    /**
     * @param filePath The path to the image which should be uploaded
     * @param productSku The product SKU to add image upload jobs to
     * @param moveOriginal Whether the original files should be moved or copied
     * @param jobControlInterface The job controller interface
     * @param settings The settings snapshot
     * @param loadingControl the loading control for the progress indication
     */
    public AbstractAddNewImagesTask(String filePath, String productSku,
            boolean moveOriginal, JobControlInterface jobControlInterface,
            SettingsSnapshot settings, LoadingControl loadingControl) {
        this(new String[] {
            filePath
        }, productSku, moveOriginal, jobControlInterface, settings, loadingControl);
    }

    /**
     * @param filePaths The paths to the images which should be uploaded
     * @param productSku The product SKU to add image upload jobs to
     * @param moveOriginal Whether the original files should be moved or copied
     * @param jobControlInterface The job controller interface
     * @param settings The settings snapshot
     * @param loadingControl the loading control for the progress indication
     */
    public AbstractAddNewImagesTask(String[] filePaths, String productSku,
            boolean moveOriginal, JobControlInterface jobControlInterface,
            SettingsSnapshot settings, LoadingControl loadingControl) {
        super(loadingControl);
        mFilePaths = filePaths;
        this.mProductSku = productSku;
        this.mMoveOriginal = moveOriginal;
        this.mSettingsSnapshot = settings;
        this.mJobControlInterface = jobControlInterface;
    }

    /**
     * Get the file name for the target file. By default it equals to
     * source file name. This method may be overridden if some special
     * file name is required
     * 
     * @param source the source file
     * @return target file name
     */
    protected String getTargetFileName(File source) {
        return source.getName();
    }

    @Override
    protected Boolean doInBackground(Void... args) {
        try {
            // Add image upload job for each image file path
            for (String filePath : mFilePaths) {
                if (isCancelled()) {
                    return false;
                }
                Job uploadImageJob = UploadImageJobUtils.createImageUploadJob(mProductSku,
                        filePath, mMoveOriginal,
                        new CallableWithParameterAndResult<File, String>() {

                            @Override
                            public String call(File p) {
                                return getTargetFileName(p);
                            }
                        }, mSettingsSnapshot);

                mJobControlInterface.addJob(uploadImageJob);
                // send broadcast notification about image job added
                Intent intent = EventBusUtils.getGeneralEventIntent(EventType.JOB_ADDED);
                intent.putExtra(EventBusUtils.JOB, new ParcelableJobDetails(uploadImageJob));
                EventBusUtils.sendGeneralEventBroadcast(intent);
            }
            // do some extra background job if necessary
            doExtraWithJobInBackground();
            return !isCancelled();
        } catch (Exception ex) {
            CommonUtils.error(TAG, ex);
        }
        return false;

    }

    /**
     * The method which is called from the doInBackground method which childs
     * may override to execute some extra job in the background
     */
    protected void doExtraWithJobInBackground() {
    }

    /**
     * Get the image file path
     * 
     * @return
     */
    public String getFilePath() {
        return mFilePaths[0];
    }

    /**
     * Get the product SKU
     * 
     * @return
     */
    public String getProductSku() {
        return mProductSku;
    }
}