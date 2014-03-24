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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.widget.Toast;

import com.mageventory.activity.ProductDetailsActivity;
import com.mageventory.job.JobCacheManager;

public class LoadImagePreviewFromServer extends AsyncTask<Void, Void, Boolean> {

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
        imagePreviewProgressDialog.setMessage("Loading image preview...");
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
        final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");

        boolean success = true;

        if (new File(mLocalPath).exists()) {
            /* The file is cached, no need to redownload. */
            return success;
        }

        final HttpGet request = new HttpGet(mUrl);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        InputStream in = null;

        // be nice to memory management
        opts.inInputShareable = true;
        opts.inPurgeable = true;

        try {
            HttpResponse response;
            HttpEntity entity;

            response = client.execute(request);
            entity = response.getEntity();
            if (entity != null) {
                in = entity.getContent();
                if (in != null) {
                    in = new BufferedInputStream(in);

                    opts.inJustDecodeBounds = false;

                    final Bitmap bitmap = BitmapFactory.decodeStream(in, null, opts);

                    // Save Image in SD Card
                    FileOutputStream imgWriter = new FileOutputStream(mLocalPath);
                    bitmap.compress(CompressFormat.JPEG, 100, imgWriter);
                    imgWriter.flush();
                    imgWriter.close();
                }

                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        } catch (Throwable e) {
            success = false;
        }

        // close client
        client.close();

        return success;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        dismissPreviewDownloadProgressDialog();
        if (result == true) {
            mHost.startPhotoEditActivity(mLocalPath, false);
        } else {
            Toast.makeText(mHost, "Unable to load the preview.", Toast.LENGTH_SHORT).show();
        }
    }
}
