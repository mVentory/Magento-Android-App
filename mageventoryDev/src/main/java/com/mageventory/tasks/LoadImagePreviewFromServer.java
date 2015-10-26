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

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;

import com.mventory.R;
import com.mageventory.activity.ProductDetailsActivity;
import com.mageventory.bitmapfun.util.ImageFetcher;
import com.mageventory.bitmapfun.util.ImageWorker.ProcessingState;
import com.mageventory.job.JobCacheManager;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;

public class LoadImagePreviewFromServer extends AsyncTask<Void, Void, Boolean> implements
        ProcessingState {

    static final String TAG = LoadImagePreviewFromServer.class.getSimpleName();

    private String mLocalPath;
    private String mUrl;
    private ProductDetailsActivity mHost;
    private ProgressDialog imagePreviewProgressDialog;

    public LoadImagePreviewFromServer(ProductDetailsActivity host, String localPath, String url) {

        String SKU = host.instance.getSku();

        String fullPreviewDir = JobCacheManager.getImageFullPreviewDirectory(SKU,
                host.mSettings.getUrl(), true).getAbsolutePath();
        mLocalPath = fullPreviewDir + localPath.substring(localPath.lastIndexOf("/"));

        mUrl = url;
        mHost = host;
    }

    public void dismissPreviewDownloadProgressDialog() {
        if (imagePreviewProgressDialog == null) {
            return;
        }
        imagePreviewProgressDialog.dismiss();
        imagePreviewProgressDialog = null;
    }

    public void showPreviewDownloadProgressDialog() {
        if (imagePreviewProgressDialog != null) {
            return;
        }
        imagePreviewProgressDialog = new ProgressDialog(mHost);
        imagePreviewProgressDialog.setMessage(CommonUtils
                .getStringResource(R.string.loading_image_preview));
        imagePreviewProgressDialog.setIndeterminate(true);
        imagePreviewProgressDialog.setCancelable(true);
        imagePreviewProgressDialog.show();
        imagePreviewProgressDialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                LoadImagePreviewFromServer.this.cancel(false);
            }
        });
    }

    @Override
    protected void onPreExecute() {
        showPreviewDownloadProgressDialog();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            boolean success = true;
            File file = new File(mLocalPath);
            if (file.exists()) {
                /* The file is cached, no need to redownload. */
                return success;
            }

            success = ImageFetcher.downloadBitmap(mUrl, file, this);
            return success;
        } catch (Throwable e) {
            CommonUtils.error(TAG, e);
        }

        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        dismissPreviewDownloadProgressDialog();
        if (result == true) {
            mHost.startPhotoViewActivity(mLocalPath);
        } else {
            GuiUtils.alert(R.string.unable_to_load_image_preview);
        }
    }

    @Override
    public boolean isProcessingCancelled() {
        // If task is cancelled no point to continue image downloading
        return isCancelled();
    }
}
