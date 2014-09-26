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

import com.mageventory.MageventoryConstants;
import com.mageventory.job.Job;
import com.mageventory.util.FileUtils;

/**
 * Utilities for the upload image jobs
 * 
 * @author Eugene Popovich
 */
public class UploadImageJobUtils implements MageventoryConstants {

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

}
