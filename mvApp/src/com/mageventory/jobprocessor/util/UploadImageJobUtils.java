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

package com.mageventory.jobprocessor.util;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import android.net.Uri;

import com.mageventory.MageventoryConstants;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.FileUtils;
import com.mageventory.util.run.CallableWithParameterAndResult;

/**
 * Utilities for the upload image jobs
 * 
 * @author Eugene Popovich
 */
public class UploadImageJobUtils implements MageventoryConstants {
    /**
     * Tag used for logging
     */
    public static final String TAG = UploadImageJobUtils.class.getSimpleName();
    /**
     * Copy the source image file to the target location and add required file
     * describing extras to the uploadImageJob
     * 
     * @param uploadImageJob the job to associate copied file with
     * @param source the source file
     * @param target the target location for the copied/moved file
     * @param moveFile whether the source file should be moved or just copied
     * @throws IOException
     */
    public static void copyImageFileAndInitJob(Job uploadImageJob, File source, File target,
            boolean moveFile) throws IOException {
        if (moveFile) {
            source.renameTo(target);
        } else {
            FileUtils.copy(source, target);
        }
    
        // fix for the file without .jpg extension in its name
        int p = target.getName().toLowerCase().lastIndexOf(".jpg");
        String name = target.getName();
        uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_NAME,
                p == -1 ? name : name.substring(0, p));
    
        uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_CONTENT, target.getAbsolutePath());
        String mimeType = FileUtils.getMimeType(target);
        if (mimeType == null) {
            // if mime type was not determined properly from extension
            mimeType = "image/jpeg";
        }
        uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_MIME, mimeType);
    }

    /**
     * Create the upload image job for the data
     * 
     * @param productSku the product SKU the image should be uploaded to
     * @param filePath the image path: either local file system path or web URL
     * @param moveOriginal whether the file should be moved to product folder
     *            and removed from its original location. Works only for local
     *            filePath
     * @param getTargetFileNameCallable the executable to obtain local target
     *            file name
     * @param settingsSnapshot the settings snapshot
     * @return instance of the image upload Job. The job should be then added to
     *         execution via {@link JobControlInterface#addJob(Job)} method
     * @throws IOException
     */
    public static Job createImageUploadJob(String productSku, String filePath,
            boolean moveOriginal,
            CallableWithParameterAndResult<File, String> getTargetFileNameCallable,
            SettingsSnapshot settingsSnapshot) throws IOException {
        JobID jobID = new JobID(INVALID_PRODUCT_ID, RES_UPLOAD_IMAGE, "" + productSku, null);
        Job uploadImageJob = new Job(jobID, settingsSnapshot);

        Uri contentUri = Uri.parse(filePath);
        if ("file".equals(contentUri.getScheme())) {
            // if file content URI is passed as the file path
            filePath = contentUri.getPath();
            contentUri = Uri.parse(filePath);
        }
        if (contentUri.isAbsolute()) {
            CommonUtils.debug(TAG,
                    "createImageUploadJob: Passed file path %1$s is the content URI", filePath);
            // if specified path is URL
            uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_CONTENT, filePath);
        } else {
            CommonUtils.debug(TAG,
                    "createImageUploadJob: Passed file path %1$s is absolute file system path",
                    filePath);
        	// if specified path is local file
            File source = new File(filePath);
            File imagesDir = JobCacheManager.getImageUploadDirectory(productSku,
                    settingsSnapshot.getUrl());
            File target = new File(imagesDir, getTargetFileNameCallable.call(source));
            UploadImageJobUtils.copyImageFileAndInitJob(uploadImageJob, source, target,
                    moveOriginal);
        }
        return uploadImageJob;
    }

    /**
     * Get the UUID generated file name with the same extension as passed in the parameter
     * 
     * @param extension the extension for the generated file name, can be null
     * @return generated unique generated file name
     */
    public static String getGeneratedUploadImageFileName(String extension) {
        return UUID.randomUUID() + (extension == null ? "" : ("." + extension));
    }
}
