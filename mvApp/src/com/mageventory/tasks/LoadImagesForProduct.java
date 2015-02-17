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

package com.mageventory.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;

import com.mageventory.bitmapfun.util.ImageFetcher;
import com.mageventory.bitmapfun.util.ImageWorker.ProcessingState;
import com.mageventory.model.Product;
import com.mageventory.model.Product.imageInfo;
import com.mageventory.settings.Settings;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.run.CallableWithParameterAndResult;
import com.mventory.R;

/**
 * The asynchronous task to load all the images related to the product
 * 
 * @author Eugene Popovich
 */
public class LoadImagesForProduct extends SimpleAsyncTask implements ProcessingState {
    static final String TAG = LoadImagesForProduct.class.getSimpleName();

    /**
     * The activity related to the task
     */
    private Activity mHost;
    /**
     * The progress showing dialog
     */
    private ProgressDialog mProgressDialog;
    /**
     * The product which images should be loaded
     */
    private Product mProduct;
    /**
     * The directory where the downloaded images should be placed
     */
    private File mImagesDir;
    /**
     * The callable to retrieve image URL from the {@link imageInfo}
     */
    private CallableWithParameterAndResult<imageInfo, String> mGetImageUrlCallable;
    /**
     * The list of downloaded files
     */
    private List<File> mDownloadedImages = new ArrayList<File>();

    /**
     * The flag indicating whether the main image should be loaded only
     */
    private boolean mLoadMainImageOnly;

    /**
     * @param product The product to load images for
     * @param loadMainImageOnly The flag indicating whether the main image
     *            should be loaded only
     * @param imagesDir The directory where the downloaded images should be
     *            placed
     * @param getImageUrlCallable The callable to retrieve image URL from the
     *            {@link imageInfo}
     * @param settings the settings
     * @param host The activity related to the task
     */
    public LoadImagesForProduct(Product product, boolean loadMainImageOnly, File imagesDir,
            CallableWithParameterAndResult<imageInfo, String> getImageUrlCallable,
            Settings settings, Activity host) {
        super(null);
        mProduct = product;
        mLoadMainImageOnly = loadMainImageOnly;
        mImagesDir = imagesDir;
        mGetImageUrlCallable = getImageUrlCallable;
        mHost = host;
    }

    @Override
    public void stopLoading() {
        super.stopLoading();
        if (mProgressDialog == null) {
            return;
        }
        try {
            if (mProgressDialog != null && mProgressDialog.getWindow() != null
                    && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        } catch (Exception ex) {
            CommonUtils.error(TAG, ex);
        }
        mProgressDialog = null;
    }

    @Override
    public void startLoading() {
        super.startLoading();
        if (mProgressDialog != null) {
            return;
        }
        mProgressDialog = new ProgressDialog(mHost);
        mProgressDialog.setMessage(CommonUtils.getStringResource(R.string.loading_images));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.show();
        mProgressDialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                LoadImagesForProduct.this.cancel(true);
            }
        });
    }

    /**
     * Update the loading progress message showing to user
     * 
     * @param current number of image currently downloading
     * @param total total number of images to download
     */
    void updateProgress(final int current, final int total) {
        GuiUtils.post(new Runnable() {

            @Override
            public void run() {
                try {
                    mProgressDialog.setMessage(CommonUtils.getStringResource(
                            R.string.loading_images_with_counter, current, total));
                } catch (Exception ex) {
                    CommonUtils.error(TAG, ex);
                }
            }
        });
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            boolean success = true;
            for (int i = 0, size = mProduct.getImages().size(); i < size; i++) {
                if (isProcessingCancelled()) {
                    return false;
                }
                if (!mLoadMainImageOnly) {
                    updateProgress(i + 1, size);
                }

                imageInfo ii = mProduct.getImages().get(i);
                if (mLoadMainImageOnly && !ii.getMain()) {
                    continue;
                }
                String name = ii.getImgName();
                File file = new File(mImagesDir, name.substring(name.lastIndexOf("/") + 1));
                mDownloadedImages.add(file);

                if (file.exists()) {
                    /* The file is cached, no need to redownload. */
                    continue;
                }

                success &= ImageFetcher.downloadBitmap(mGetImageUrlCallable.call(ii), file, this);
                if (!success) {
                    break;
                }
            }
            return success;
        } catch (Throwable e) {
            CommonUtils.error(TAG, e);
        }

        return false;
    }

    @Override
    protected void onFailedPostExecute() {
        super.onFailedPostExecute();
        GuiUtils.alert(R.string.unable_to_load_all_images);
    }

    @Override
    public boolean isProcessingCancelled() {
        // If task is cancelled no point to continue image downloading
        return isCancelled();
    }

    @Override
    protected void onSuccessPostExecute() {

    }

    /**
     * Get the downloaded image files
     * 
     * @return
     */
    public List<File> getDownloadedImages() {
        return mDownloadedImages;
    }

}
