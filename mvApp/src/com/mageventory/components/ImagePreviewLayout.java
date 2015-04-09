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

package com.mageventory.components;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.ProductDetailsActivity;
import com.mageventory.bitmapfun.util.ImageFetcher;
import com.mageventory.bitmapfun.util.ImageResizer;
import com.mageventory.bitmapfun.util.ImageWorker;
import com.mageventory.interfaces.IOnClickManageHandler;
import com.mageventory.job.Job;
import com.mageventory.job.JobID;
import com.mageventory.job.JobService;
import com.mageventory.job.JobService.NetworkStateInformation;
import com.mageventory.job.ParcelableJobDetails;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.util.ProductResourceUtils;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.AbstractSimpleLoadTask;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.SimpleViewLoadingControl;
import com.mageventory.widget.AspectRatioImageView;
import com.mventory.R;

/**
 * LinearLayout containing three elements: one <code>ImageView</code>, one
 * delete <code>Button</code> and one <code>CheckBox</code> and an indeterminate
 * progress bar for the loading, the deletion or the update of the image on
 * server
 * 
 * @author Bogdan Petran
 */
public class ImagePreviewLayout extends FrameLayout implements MageventoryConstants {

    static final String TAG = ImagePreviewLayout.class.getSimpleName();

    /**
     * The width part of the URL for the resized image
     */
    private static final String IMAGE_WIDTH_PATH_PART = "/width/";
    /**
     * The prefix part of the URL for the resized image
     */
    private static final String MVENTORY_TM_IMAGE_GET_FILE = "/mventory/image/get/file/";

    // private int uploadPhotoID = 0;
    // private int uploadImageRequestId = INVALID_REQUEST_ID;
    ResourceServiceHelper resHelper;

    /**
     * This task updates the image position on server
     */
    private class MarkImageMainTask extends AbstractSimpleLoadTask {
    	/**
         * Related activity
         */
        final ProductDetailsActivity mActivityInstance;
        /**
         * The product there the image should be marked main
         */
        private Product mProduct;

        /**
         * @param instance related activity
         */
        public MarkImageMainTask(ProductDetailsActivity instance) {
            super(new SettingsSnapshot(instance), loadingControl);
            mProduct = instance.instance;
            mActivityInstance = instance;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (mActivityInstance == null)
                    return false;

                boolean success = loadGeneral();

                if (isCancelled()) {
                    return false;
                }
                if (success) {
                    // if image was marked main reload product details with its
                    // siblings
                    success = ProductResourceUtils.reloadSiblings(true, mProduct,
                            settingsSnapshot.getUrl());
                }

                return success && !isCancelled();
            } catch (Exception ex) {
                CommonUtils.error(TAG, ex);
            }
            return false;
        }

        @Override
        protected int requestLoadResource() {
            return resHelper.loadResource(mActivityInstance, RES_MARK_IMAGE_MAIN,
                    new String[] {
                    mProduct.getId(), getImageName()
            }, settingsSnapshot);
        }

        @Override
        protected void onFailedPostExecute() {
            GuiUtils.alert(R.string.error_image_mark_main);
            setMainImageCheck(false);
        };

        @Override
        protected void onSuccessPostExecute() {

            if (!mActivityInstance.isActivityAlive()) {
            	// if activity was closed
                return;
            }
            for (int i = 0; i < mActivityInstance.list.getChildCount(); i++) {
                View child = mActivityInstance.list.getChildAt(i);
                if (child instanceof ImagePreviewLayout) {
                    ((ImagePreviewLayout) child).setMainImageCheck(false);
                }
            }
            for (ImagePreviewLayoutData data : mActivityInstance.productDetailsAdapter
                    .getImagesData()) {
                data.mainImage = false;
            }
            mData.mainImage = true;
            setMainImageCheck(true);
            // call refresh callback if exists
            if (mData.refreshCallback != null) {
                mData.refreshCallback.run();
            }

        }
    }

    private class EventListener implements OnClickListener, OnCheckedChangeListener {

        WeakReference<ImagePreviewLayout> viewReference;
        final ImagePreviewLayout viewInstance;

        public EventListener(ImagePreviewLayout instance) {
            viewReference = new WeakReference<ImagePreviewLayout>(instance);
            viewInstance = viewReference.get();
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.imageViewHolder) {
                // if this will be necessary, it will start the edit image view
                // when clicking the image
                getOnClickManageHandler().onClickForEdit(mData);
            } else if (v.getId() == R.id.deleteBtn) {
                // notify to delete current layout and image from server
                getOnClickManageHandler().onDelete(viewInstance);
            } else if (v.getId() == R.id.deleteJobBtn) {
                // notify to delete current layout and image from server
                getOnClickManageHandler().onDelete(viewInstance);
            }
        }

        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {

            System.out.println("is checked: " + isChecked + " ask for aproval: "
                    + askForMainImageApproval);
            // don't let this button to be clicked when it's checked but let it
            // when it's unchecked
            buttonView.setFocusable(!isChecked);
            buttonView.setClickable(!isChecked);

            if (isChecked && askForMainImageApproval && (!setAsMainImageOverride)) {
                askForMainImageApproval = true;

                Builder dialogBuilder = new Builder(getContext());
                dialogBuilder.setMessage("Make it the main product image?");
                dialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        buttonView.setChecked(false);
                    }
                });

                dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // notify to set the selected image as
                        // main/first image
                        getOnClickManageHandler().onClickForMainImage(viewInstance);
                    }
                });

                dialogBuilder.show();
            }
            setAsMainImageOverride = false;
        }
    }

    private EventListener eventListener;

    // private String IMAGES_URL =
    // "http://mventory.simple-helix.net/media/catalog/product";

    private AspectRatioImageView imgView;
    private Button deleteBtn;
    private Button deleteJobBtn;
    private ProgressBar loadingProgressBar;
    private ProgressBar uploadingProgressBar;
    private View uploadingView;
    private ImageView uploadingThumb;
    private TextView uploadFailedText;
    private TextView uploadWaitingForConnectionText;
    private CheckBox mainImageCheckBox;

    private int errorCounter = 0; // counter used when an upload error occures
                                  // (if this is > 0, this layout and the
                                  // image coresponding to this layout, will
                                  // be deleted)
    private boolean askForMainImageApproval = true;
    private boolean setAsMainImageOverride = false; // if this is true the image
                                                    // won't be set as main in
    // // error occures and we need to try again the
    // // image upload

    private LinearLayout elementsLayout;

    private TextView imageSizeTxtView;

    ImagePreviewLayoutData mData;
    
    private DownloadImageFromServerTask mDownloadImageFromServerTask;
    private LoadStillDownloadingFromServerTask mLoadStillDownloadingFromServerTask;

    ImageResizer mImageWorker;
    ImageResizer mThumbImageWorker;
    /**
     * The image worker to display thumbs for the URLs
     */
    ImageResizer mUrlThumbImageWorker;
    LoadingControl loadingControl;

    // private String imageLocalPath;

    public ImagePreviewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setData(ImagePreviewLayoutData data, ImageResizer imageWorker,
            ImageResizer thumbImageWorker, ImageResizer urlThumbImageWorker) {
        mData = data;
        loadingControl = new CustomLoadingControl();
        mImageWorker = imageWorker;
        mThumbImageWorker = thumbImageWorker;
        mUrlThumbImageWorker = urlThumbImageWorker;
        askForMainImageApproval = true;
        setAsMainImageOverride = false;
        imgView.setAspectRatio(null);
        // TODO perhaps we need to cancel download task in some cases also
        // if (mDownloadImageFromServerTask != null) {
        // mDownloadImageFromServerTask.cancel(true);
        // }
        if (mLoadStillDownloadingFromServerTask != null) {
            mLoadStillDownloadingFromServerTask.cancel(true);
        }
       
        Job job = getUploadJob();
        setUploading(job != null);

        // this is necessary only in the case when setUrl is called from outside
        // and the imgView is null then
        if (getUrl() != null && !isNoDownload()) {
            mImageWorker.loadImage(null, imgView, loadingControl);
            setImageFromUrl();
        }
        setAspectRatioIfAvailable();
        if (!TextUtils.isEmpty(getImageLocalPath()) && isNoDownload()) {
            synchronized (ImageCachingManager.sSynchronisationObject) {
                mImageWorker.loadImage(null, imgView, loadingControl);
                if (ImageCachingManager.isDownloadPending(getSku(), getImageLocalPath()) == false
                        && new File(getImageLocalPath()).exists() == false) {
                    // we have no choice, we have to redownload, file is missing
                    setImageFromUrl();
                } else {
                    mLoadStillDownloadingFromServerTask = new LoadStillDownloadingFromServerTask(
                            mData);
                    mLoadStillDownloadingFromServerTask
                            .executeOnExecutor(ImageWorker.THREAD_POOL_EXECUTOR);
                }
            }
        }
        if (job == null) {
            updateJobStatusesFields(false, false, 0, null);
        } else {
            updateJobStatusesFields(job.getFinished(), job.getPending(),
                    job.getProgressPercentage(), job.getJobID());

            Map<String, Object> imageData = job.getExtras();

            if (imageData != null) {
                String path = imageData.get(MAGEKEY_PRODUCT_IMAGE_CONTENT).toString();
                // choose image worker depend on path type
                ImageResizer worker = ImageUtils.isUrl(path) ? mUrlThumbImageWorker
                        : mThumbImageWorker;
                worker.loadImage(path, uploadingThumb);
            } else {
                mThumbImageWorker.loadImage(null, uploadingThumb);
            }
        }
        setMainImageCheck(mData.mainImage);
    }

    public void setAspectRatioIfAvailable() {
        if (getOriginalImageWidth() != null && getOriginalImageHeight() != null) {
            imgView.setAspectRatio(getOriginalImageWidth().floatValue()
                    / getOriginalImageHeight().floatValue());
        }
    }

    public ImagePreviewLayoutData getData() {
        return mData;
    }

    public ImageView getImageView() {
        return imgView;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // only after the inflate is finished we can get references to the
        // ImageView and the delete Button
        imgView = (AspectRatioImageView) findViewById(R.id.imageViewHolder);
        deleteBtn = (Button) findViewById(R.id.deleteBtn);
        deleteJobBtn = (Button) findViewById(R.id.deleteJobBtn);
        loadingProgressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);
        uploadingProgressBar = (ProgressBar) findViewById(R.id.uploadingProgressBar);
        uploadingView = findViewById(R.id.uploadingView);
        uploadingThumb = (ImageView) findViewById(R.id.uploadingThumb);
        uploadFailedText = (TextView) findViewById(R.id.uploadFailedText);
        uploadWaitingForConnectionText = (TextView) findViewById(R.id.uploadWaitingForConnection);
        mainImageCheckBox = (CheckBox) findViewById(R.id.mainImageCheckBox);
        elementsLayout = (LinearLayout) findViewById(R.id.elementsLinearLayout);
        imageSizeTxtView = (TextView) findViewById(R.id.imageSizeTxtView);

        init();
    }

    /**
     * Constructs the containing objects
     */
    private void init() {

        eventListener = new EventListener(this);

        imgView.setOnClickListener(eventListener);
        deleteBtn.setOnClickListener(eventListener);
        deleteJobBtn.setOnClickListener(eventListener);
        mainImageCheckBox.setOnCheckedChangeListener(eventListener);

        resHelper = ResourceServiceHelper.getInstance();
    }

    /**
     * @return the url as <code>String</code>
     */
    public String getUrl() {
        return mData == null ? null : mData.url;
    }

    public boolean isNoDownload() {
        return mData == null ? false : mData.noDownload;
    }

    public Runnable getRefreshCallback() {
        return mData == null ? null : mData.refreshCallback;
    }

    private void setImageFromUrl() {
        mDownloadImageFromServerTask = new DownloadImageFromServerTask(mData);
        mDownloadImageFromServerTask.executeOnExecutor(ImageWorker.THREAD_POOL_EXECUTOR);
    }

    private Integer getOriginalImageWidth() {
        return mData == null ? null : mData.originalImageWidth;
    }

    private Integer getOriginalImageHeight() {
        return mData == null ? null : mData.originalImageHeight;
    }

    /**
     * Set the image size in the image size TextView
     */
    public void updateImageTextSize() {
        Object tag;
        int width, height;

        tag = getOriginalImageWidth();
        if (tag != null) {
            width = (Integer) tag;
        } else {
            width = 0;
        }
        tag = getOriginalImageHeight();
        if (tag != null) {
            height = (Integer) tag;
        } else {
            height = 0;
        }

        imageSizeTxtView.setText(width + " x " + height + "px");
    }

    /**
     * @return the image name provided by server
     */
    public String getImageName() {
        return mData == null ? null : mData.imageName;
    }

    public String getSku() {
        return mData == null ? null : mData.SKU;
    }

    public Job getUploadJob()
    {
        return mData == null ? null : mData.uploadJob;
    }

    private boolean isCurrentJob(JobID jobId)
    {
        return mData == null ? false : mData.isCurrentJob(jobId);
    }

    public boolean onJobStateChange(final ParcelableJobDetails job) {
        
        if (!isCurrentJob(job.getJobId()))
            return false;

        updateJobStatusesFields(job.isFinished(), job.isPending(), job.getProgressPercentage(),
                job.getJobId());
        return true;
    }

    public void updateJobStatusesFields(boolean finished, boolean pending,
            final int progressPercentage, final JobID jobId) {
        if (jobId == null || !isCurrentJob(jobId)) {
            ImagePreviewLayout.this.post(new Runnable() {

                @Override
                public void run() {
                    uploadFailedText.setVisibility(View.GONE);
                }
            });
        } else {
            ImagePreviewLayout.this.post(new Runnable() {

                @Override
                public void run() {
                    if (!isCurrentJob(jobId)) {
                        return;
                    }
                    NetworkStateInformation nsi = JobService.getNetworkStateInformation();
                    if (nsi.networkStateOK && nsi.avoidImageUploadJobs) {
                        uploadWaitingForConnectionText.setVisibility(View.VISIBLE);
                    } else {
                        uploadWaitingForConnectionText.setVisibility(View.GONE);
                    }
                    uploadingProgressBar.setIndeterminate(progressPercentage <= 0);
                    uploadingProgressBar.setProgress(progressPercentage);
                }
            });

            if (!pending) {
                ImagePreviewLayout.this.post(new Runnable() {

                    @Override
                    public void run() {
                        uploadFailedText.setVisibility(View.VISIBLE);
                    }
                });
            } else {
                if (uploadFailedText.getVisibility() == View.VISIBLE) {
                    ImagePreviewLayout.this.post(new Runnable() {

                        @Override
                        public void run() {
                            uploadFailedText.setVisibility(View.GONE);
                        }
                    });
                }
            }
        }
    }
    
    public void markAsMain(String productId, ProductDetailsActivity host) {
        new MarkImageMainTask(host).execute();
    }

    public void setLoading(boolean isLoading) {
        if (isLoading) {
            // show only the progress bar when loading
            setVisibilityToChilds(GONE);
            loadingProgressBar.setVisibility(VISIBLE);
            uploadingView.setVisibility(GONE);
            uploadWaitingForConnectionText.setVisibility(View.GONE);
            return;
        }

        // remove the progress bar and show the image view and the delete button
        setVisibilityToChilds(VISIBLE);
        loadingProgressBar.setVisibility(GONE);
        uploadingView.setVisibility(GONE);
        uploadWaitingForConnectionText.setVisibility(View.GONE);
    }

    public void setUploading(boolean isUploading) {
        if (isUploading) {
            // show only the progress bar when loading
            setVisibilityToChilds(GONE);
            imgView.setVisibility(GONE);
            uploadingProgressBar.setIndeterminate(true);
            uploadingView.setVisibility(VISIBLE);
            loadingProgressBar.setVisibility(GONE);
            return;
        }

        // remove the progress bar and show the image view and the delete button
        setVisibilityToChilds(VISIBLE);
        imgView.setVisibility(VISIBLE);
        loadingProgressBar.setVisibility(GONE);
        uploadingView.setVisibility(GONE);
        uploadWaitingForConnectionText.setVisibility(View.GONE);
    }

    /**
     * modifies the visibility to the image view and the delete button inside
     */
    private void setVisibilityToChilds(int visibility) {
        elementsLayout.setVisibility(visibility);
    }

    /**
     * Changes the check state for the checkbox contained by this view. Used for
     * the first image so that when it is loaded from server, make it main.
     * 
     * @param checked
     */
    private void setMainImageCheck(boolean checked) {
        if (checked) {
            setAsMainImageOverride = true;
        }
        mainImageCheckBox.setChecked(checked);
    }

    /**
     * @return the imageLocalPath
     */
    public String getImageLocalPath() {
        return mData == null ? null : mData.imageLocalPath;
    }

    IOnClickManageHandler getOnClickManageHandler() {
        return mData == null ? null : mData.onClickManageHandler;
    }

    /**
     * Get the URL for the image of the required size
     * 
     * @param url the original image URL
     * @param settings the current settings
     * @param size the required image size
     * @return
     */
    public static String getUrlForResizedImage(String url, SettingsSnapshot settings, int size) {
        String resizedImageURL = settings.getUrl() + MVENTORY_TM_IMAGE_GET_FILE
                + url.substring(url.lastIndexOf("/") + 1);
        resizedImageURL = resizedImageURL + IMAGE_WIDTH_PATH_PART + size;
        return resizedImageURL;
    }
    /**
     * This task updates the image position on server
     */
    private class DownloadImageFromServerTask extends SimpleAsyncTask {

        ImagePreviewLayoutData mData;
        private SettingsSnapshot mSettingsSnapshot;

        DownloadImageFromServerTask(ImagePreviewLayoutData data) {
            super(loadingControl);
            mSettingsSnapshot = new SettingsSnapshot(getContext());
            mData = data;
            ImageCachingManager.addDownload(mData.SKU, mData.imageLocalPath);
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            /*
             * We use different url for loading resized images. This url is
             * generated here. See:
             * http://code.google.com/p/mageventory/issues/detail?id=131
             */

            String resizedImageURL = getUrlForResizedImage(mData.url, mSettingsSnapshot,
                    mImageWorker.getImageWidth());

            try {
                File file = new File(mData.imageLocalPath);
                if(ImageFetcher.downloadBitmap(resizedImageURL, file, null))
                {
                        /*
                         * Make sure to remove the image from the list before we
                         * create a file on the sdcard.
                         */
                        ImageCachingManager.removeDownload(mData.SKU, mData.imageLocalPath);
                        BitmapFactory.Options opts = ImageUtils
                                .calculateImageSize(mData.imageLocalPath);
                        synchronized (mData) {
                            // set these as view properties
                            mData.originalImageWidth = opts.outWidth;
                            mData.originalImageHeight = opts.outHeight;
                            mData.noDownload = true;
                        }
                        return !isCancelled();
                }
            } catch (Throwable e) {
                CommonUtils.error(TAG, e);
            }

            /* We can potentially call this twice but it's not a problem. */
            ImageCachingManager.removeDownload(mData.SKU, mData.imageLocalPath);
            return false;
        }

        @Override
        protected void onSuccessPostExecute() {
            if (!isCancelled()) {
                mImageWorker.loadImage(mData.imageLocalPath, imgView, getLoadingControl());
          //  Removing as it reports incorrect values - size of displayed image (cropped to screen size),
          //  not the original image
          //      updateImageTextSize();
            }
        }
    }

    private class LoadStillDownloadingFromServerTask extends SimpleAsyncTask {

        ImagePreviewLayoutData mData;

        LoadStillDownloadingFromServerTask(ImagePreviewLayoutData data) {
            super(loadingControl);
            mData = data;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                File fileToProbe = new File(getImageLocalPath());

                // TODO
                // this is very bad logic to have sleep timer here such
                // as it uses a whole second thread until first thread is
                // downloading image.
                // Should be reworked to broadcast event system
                while (ImageCachingManager.isDownloadPending(mData.SKU, mData.imageLocalPath)) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // CommonUtils.error(TAG, e);
                    }
                }

                if (fileToProbe.exists()) {
                    BitmapFactory.Options opts = ImageUtils
                            .calculateImageSize(mData.imageLocalPath);
                    synchronized (mData) {
                        // set these as view properties
                        mData.originalImageWidth = opts.outWidth;
                        mData.originalImageHeight = opts.outHeight;
                    }
                    return true;
                }
            } catch (Throwable t) {
                CommonUtils.error(TAG, t);
            }
            return false;
        }

        @Override
        protected void onSuccessPostExecute() {
            if (!isCancelled()) {
                if (imgView.getAspectRatio() == null) {
                    // if aspect ratio was not specified before
                    setAspectRatioIfAvailable();
                }
                mImageWorker.loadImage(mData.imageLocalPath, imgView, getLoadingControl());
          //  Removing as it reports incorrect values - size of displayed image (cropped to screen size),
          //  not the original image
          //      updateImageTextSize();
            }
        }
    }

    public static class ImagePreviewLayoutData {
        public String url; // this will be IMAGES_URL + "imageName"
        public String imageLocalPath;
        public String imageName;
        public int productID;
        public String SKU;
        public Job uploadJob;
        public Integer originalImageWidth;
        public Integer originalImageHeight;
        public boolean noDownload;
        public boolean mainImage;
        public Runnable refreshCallback;
        /**
         * handler for parent layout notification (when a delete or edit click
         * occures inside this layout)
         */
        public IOnClickManageHandler onClickManageHandler;


        public void setImageLocalPath(String imageLocalPath) {
            String[] name = imageName.split("/");

            this.imageLocalPath = imageLocalPath + "/" + name[name.length - 1];
        }

        public boolean isCurrentJob(JobID jobId) {
            if (uploadJob == null) {
                return false;
            }
            return uploadJob.getJobID().getTimeStamp() == jobId.getTimeStamp();
        }
    }

    class CustomLoadingControl extends SimpleViewLoadingControl {

        public CustomLoadingControl() {
            super(null);
        }

        @Override
        public void setViewVisibile(boolean visible) {
            if (loadingControl == this) {
                setLoading(visible);
            } else {
                CommonUtils.debug(TAG, "setViewVisible: skipped %1$b", visible);
            }
        }
    }
}
