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

package com.mageventory.jobprocessor;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.UUID;

import android.content.Context;

import com.mageventory.MageventoryConstants;
import com.mageventory.MageventoryRuntimeException;
import com.mageventory.bitmapfun.util.ImageFetcher;
import com.mageventory.client.ImageStreaming;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.jobprocessor.JobProcessorManager.IProcessor;
import com.mageventory.jobprocessor.util.UploadImageJobUtils;
import com.mageventory.model.Product;
import com.mageventory.res.util.ProductResourceUtils;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.FileUtils;
import com.mageventory.util.ImageUtils;

public class UploadImageProcessor implements IProcessor, MageventoryConstants {

    ImageStreaming.StreamUploadCallback mCallback;
    private static String TAG = "UploadImageProcessor";

    public void setCallback(ImageStreaming.StreamUploadCallback callback) {
        mCallback = callback;
    }

    @Override
    public void process(Context context, Job job) {
        Map<String, Object> imageData = job.getExtras();

        MagentoClient client;
        try {
            client = new MagentoClient(job.getSettingsSnapshot());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage());
        }

        String path = imageData.get(MAGEKEY_PRODUCT_IMAGE_CONTENT).toString();

        CommonUtils.debug(TAG, true, "Uploading image: " + path);

        try {
            if (ImageUtils.isUrl(path)) {
                // the specified path is URL so file need to be downloaded first
                CommonUtils.debug(TAG, true,
                        "process: content is URL, file will be downloaded first.");
                File source = ImageFetcher.downloadBitmap(context, path, null);
                if (source == null) {
                    // if file download failed
                    throw new MageventoryRuntimeException(CommonUtils.format(
                            "File download failed for URL %1$s", path));
                } else {
                    // if file download successful
                    File imagesDir = JobCacheManager.getImageUploadDirectory(job.getJobID()
                            .getSKU(), job.getJobID().getUrl());
                    String extension = FileUtils.getExtension(source.getName());
                    // return UUID genrated file name with the same extension as
                    // source
                    String name = UUID.randomUUID() + (extension == null ? "" : ("." + extension));
                    File target = new File(imagesDir, name);
                    UploadImageJobUtils.copyImageFileAndInitJob(job, source, target, false);
                    JobCacheManager.store(job);
                }
            }
        } catch (Exception ex) {
            throw new MageventoryRuntimeException("File download failed", ex);
        }
        Map<String, Object> productMap = client.uploadImage(imageData, ""
                + job.getJobID().getProductID(), mCallback);

        final Product product;
        if (productMap != null) {
            product = new Product(productMap);
        } else {
            throw new RuntimeException(client.getLastErrorMessage());
        }

        // cache
        if (product != null) {
            JobCacheManager.storeProductDetailsWithMergeSynchronous(product, job.getJobID()
                    .getUrl());
            ProductResourceUtils.reloadSiblings(false, product, job.getJobID().getUrl());
        }
    }
}
