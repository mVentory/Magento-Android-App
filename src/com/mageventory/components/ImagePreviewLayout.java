package com.mageventory.components;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
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
import android.widget.Toast;

import com.mageventory.CategoryListActivity;
import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.ProductDetailsActivity;
import com.mageventory.R;
import com.mageventory.interfaces.IOnClickManageHandler;
import com.mageventory.job.Job;
import com.mageventory.job.JobCallback;
import com.mageventory.job.JobControlInterface;
import com.mageventory.jobprocessor.UploadImageProcessor;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.Log;

/**
 * LinearLayout containing three elements: one <code>ImageView</code>, one
 * delete <code>Button</code> and one <code>CheckBox</code> and an indeterminate
 * progress bar for the loading, the deletion or the update of the image on
 * server
 * 
 * @author Bogdan Petran
 */
public class ImagePreviewLayout extends FrameLayout implements MageventoryConstants {

	// private int uploadPhotoID = 0;
	// private int uploadImageRequestId = INVALID_REQUEST_ID;
	ResourceServiceHelper resHelper;
	private SettingsSnapshot mSettingsSnapshot;

	/**
	 * This task updates the image position on server
	 */
	private class MarkImageMainTask extends AsyncTask<String, Void, Boolean> implements OperationObserver {

		final ProductDetailsActivity activityInstance;
		private int requestId = INVALID_REQUEST_ID;
		private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
		private CountDownLatch doneSignal;
		private boolean success;
		private SettingsSnapshot mSettingsSnapshot;
		
		@Override
		protected void onPreExecute() {
			setLoading(true);
			mSettingsSnapshot = new SettingsSnapshot(activityInstance);
		}

		public MarkImageMainTask(ProductDetailsActivity instance) {
			activityInstance = instance;
		}
		
		@Override
		protected Boolean doInBackground(String... params) {
			if (activityInstance == null)
				return null;
			
			String productId = params[0];
			
			doneSignal = new CountDownLatch(1);
			resHelper.registerLoadOperationObserver(this);
			requestId = resHelper.loadResource(activityInstance, RES_MARK_IMAGE_MAIN,
					new String[] { productId, imageName }, mSettingsSnapshot);
			while (true) {
				if (isCancelled()) {
					return null;
				}
				try {
					if (doneSignal.await(1, TimeUnit.SECONDS)) {
						break;
					}
				} catch (InterruptedException e) {
					return null;
				}
			}
			resHelper.unregisterLoadOperationObserver(this);

			return success;
		}

		@Override
		protected void onPostExecute(Boolean result) {

			if (result == Boolean.FALSE) {
				Toast.makeText(activityInstance.getApplicationContext(), "Could mark image as main. Check the connection and try again.", Toast.LENGTH_LONG)
						.show();
				setMainImageCheck(false);
			}
			else
			{
				for (int i = 0; i < activityInstance.imagesLayout.getChildCount(); i++) {
					((ImagePreviewLayout) activityInstance.imagesLayout.getChildAt(i)).setMainImageCheck(false);
				}
				setMainImageCheck(true);
			}
			
			setLoading(false);
		}
		
		@Override
		public void onLoadOperationCompleted(LoadOperation op) {
			if (op.getOperationRequestId() == requestId) {
				success = op.getException() == null;
				doneSignal.countDown();
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
				onClickManageHandler.onClickForEdit(viewInstance);
			} else if (v.getId() == R.id.deleteBtn) {
				// notify to delete current layout and image from server
				onClickManageHandler.onDelete(viewInstance);
			}
		}

		@Override
		public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {

			System.out.println("is checked: " + isChecked + " ask for aproval: " + askForMainImageApproval);
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
						onClickManageHandler.onClickForMainImage(viewInstance);
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

	private String url; // this will be IMAGES_URL + "imageName"
	private String imageName;
	private int productID;
	private String SKU;
	private Job uploadJob;
	private JobCallback uploadJobCallback = null;
	private ImageView imgView;
	private Button deleteBtn;
	private ProgressBar loadingProgressBar;
	private ProgressBar uploadingProgressBar;
	private CheckBox mainImageCheckBox;
	private IOnClickManageHandler onClickManageHandler; // handler for parent
														// layout notification
														// (when a delete or
														// edit click occures
														// inside this layout)
	private int index; // the index of this layout within its parrent
	private int errorCounter = 0; // counter used when an upload error occures
									// (if this is > 0, this layout and the
									// image coresponding to this layout, will
									// be deleted)
	private boolean askForMainImageApproval = true;
	private boolean setAsMainImageOverride = false; // if this is true the image
													// won't be set as main in
													// case the checkbox gets
													// checked
	private String imagePath; // image path is saved for the case when some
								// error occures and we need to try again the
								// image upload
	private Runnable mRefreshCallback;

	private LinearLayout elementsLayout;

	private TextView imageSizeTxtView;
	private String imageLocalPath;

	public ImagePreviewLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ImageView getImageView() {
		return imgView;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		// only after the inflate is finished we can get references to the
		// ImageView and the delete Button
		imgView = (ImageView) findViewById(R.id.imageViewHolder);
		deleteBtn = (Button) findViewById(R.id.deleteBtn);
		loadingProgressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);
		uploadingProgressBar = (ProgressBar) findViewById(R.id.uploadingProgressBar);
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
		mainImageCheckBox.setOnCheckedChangeListener(eventListener);

		// this is necessary only in the case when setUrl is called from outside
		// and the imgView is null then
		if (url != null) {
			setImageFromUrl();
		}

		resHelper = ResourceServiceHelper.getInstance();
	}

	/**
	 * 
	 * @return the url as <code>String</code>
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * 
	 * @param url
	 *            of the image to be shown in <code>ImageView</code>
	 */
	public void setUrl(String url) {
		this.url = url;

		if (imgView != null) {
			setImageFromUrl();
		}
	}

	/**
	 * 
	 * @param listener
	 *            <code>OnClickManageHandler</code> used to handle click events
	 *            outside this layout
	 */
	public void setManageClickListener(IOnClickManageHandler listener) {
		onClickManageHandler = listener;
	}

	private void setImageFromUrl() {
		ImageCachingManager.addDownload(SKU, imageLocalPath);
		new DownloadImageFromServerTask().execute();
	}

	/**
	 * Set the image size in the image size TextView
	 */
	public void updateImageTextSize() {
		Drawable imgDrawable = imgView.getDrawable();
		if (imgDrawable == null) {
			return;
		}

		Object tag;
		int width, height;

		tag = imgView.getTag(R.id.tkey_original_img_width);
		if (tag != null) {
			width = (Integer) tag;
		} else {
			width = imgDrawable.getIntrinsicWidth();
		}
		tag = imgView.getTag(R.id.tkey_original_img_height);
		if (tag != null) {
			height = (Integer) tag;
		} else {
			height = imgDrawable.getIntrinsicHeight();
		}

		imageSizeTxtView.setText(width + " x " + height + "px");
	}

	/**
	 * 
	 * @return the image name provided by server
	 */
	public String getImageName() {
		return imageName;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public void setImageUrlNoDownload(String imageUrl) {
		this.url = imageUrl;
	}

	/**
	 * 
	 * @param imageUrl
	 *            which is the name of the image saved on server
	 */
	// y XXX: REWORK THAT
	public void setImageUrl(String imageUrl) {
		setUrl(imageUrl);
	}

	public void setProductID(int productID) {
		this.productID = productID;
	}

	public void setSKU(String SKU) {
		this.SKU = SKU;
	}

	public void setUploadJob(final Job job, final JobControlInterface jobControl, Runnable refreshCallback) {
		mRefreshCallback = refreshCallback;
		uploadJob = job;
		setUploading(true);
	}
	
	public Job getUploadJob()
	{
		return uploadJob;
	}

	public void registerCallbacks(final JobControlInterface jobControl) {
		if (uploadJob == null)
			return;

		final Context context = this.getContext();

		if (uploadJobCallback != null) {
			jobControl.deregisterJobCallback(uploadJob.getJobID(), uploadJobCallback);
		}

		uploadJobCallback = new JobCallback() {

			@Override
			public void onJobStateChange(final Job job) {

				ImagePreviewLayout.this.post(new Runnable() {

					@Override
					public void run() {
						uploadingProgressBar.setProgress(job.getProgressPercentage());
					}
				});

				if (job.getFinished() == true) {
					if (mRefreshCallback != null) {
						ImagePreviewLayout.this.post(mRefreshCallback);
					}

					jobControl.deregisterJobCallback(job.getJobID(), this);
				}
			}
		};

		if (!jobControl.registerJobCallback(uploadJob.getJobID(), uploadJobCallback)) {
			uploadJobCallback = null;
			ImagePreviewLayout.this.post(mRefreshCallback);
		}
	}

	public void deregisterCallbacks(final JobControlInterface jobControl) {
		if (uploadJob != null) {
			if (uploadJobCallback != null) {
				jobControl.deregisterJobCallback(uploadJob.getJobID(), uploadJobCallback);
				uploadJobCallback = null;
			}
		}
	}

	/**
	 * 
	 * @return the index of this view inside its parent
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * 
	 * @param index
	 *            of this view inside its parent
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	public void markAsMain(String productId, ProductDetailsActivity host) {
		new MarkImageMainTask(host).execute(productId);
	}

	public void setLoading(boolean isLoading) {
		if (isLoading) {
			// show only the progress bar when loading
			setVisibilityToChilds(GONE);
			loadingProgressBar.setVisibility(VISIBLE);
			uploadingProgressBar.setVisibility(GONE);
			return;
		}

		// remove the progress bar and show the image view and the delete button
		setVisibilityToChilds(VISIBLE);
		loadingProgressBar.setVisibility(GONE);
		uploadingProgressBar.setVisibility(GONE);
	}

	public void setUploading(boolean isUploading) {
		if (isUploading) {
			// show only the progress bar when loading
			setVisibilityToChilds(GONE);
			uploadingProgressBar.setVisibility(VISIBLE);
			loadingProgressBar.setVisibility(GONE);
			return;
		}

		// remove the progress bar and show the image view and the delete button
		setVisibilityToChilds(VISIBLE);
		loadingProgressBar.setVisibility(GONE);
		uploadingProgressBar.setVisibility(GONE);
	}

	/**
	 * modifies the visibility to the image view and the delete button inside
	 */
	private void setVisibilityToChilds(int visibility) {
		elementsLayout.setVisibility(visibility);
	}

	public String getImagePath() {
		return imagePath;
	}

	/**
	 * Sets the image path/url used to load or send image to server
	 * 
	 * @param imagePath
	 */
	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	/**
	 * Changes the check state for the checkbox contained by this view. Used for
	 * the first image so that when it is loaded from server, make it main.
	 * 
	 * @param checked
	 */
	public void setMainImageCheck(boolean checked) {
		if (checked) {
			setAsMainImageOverride = true;
		}
		mainImageCheckBox.setChecked(checked);
	}

	// y XXX: move this out, its right place isn't here...
	private static DisplayMetrics sDisplayMetrics;

	private static DisplayMetrics getDisplayMetrics(final Activity a) {
		if (sDisplayMetrics == null) {
			sDisplayMetrics = new DisplayMetrics();
			a.getWindowManager().getDefaultDisplay().getMetrics(sDisplayMetrics);
		}
		return sDisplayMetrics;
	}

	public void loadFromSDPendingDownload(String imageName, String folderPath) {
		this.setImageName(imageName);
		this.setImageLocalPath(folderPath);

		synchronized (ImageCachingManager.sSynchronisationObject) {
			if (ImageCachingManager.isDownloadPending(SKU, imageLocalPath) == false
					&& new File(getImageLocalPath()).exists() == false) {
				// we have no choice, we have to redownload, file is missing
				setUrl(url);
			} else {
				new loadStillDownloadingFromServerTask().execute();
			}
		}
	}

	/**
	 * @return the imageLocalPath
	 */
	public String getImageLocalPath() {
		return imageLocalPath;
	}

	/**
	 * @param imageLocalPath
	 *            the imageLocalPath to set
	 */
	public void setImageLocalPath(String imageLocalPath) {
		String[] name = imageName.split("/");

		this.imageLocalPath = imageLocalPath + "/" + name[name.length - 1];
	}

	/**
	 * This task updates the image position on server
	 */
	private class DownloadImageFromServerTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			// a view holder image is set until the upload is complete
			setLoading(true);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");

			/*
			 * We use different url for loading resized images. This url is
			 * generated here. See:
			 * http://code.google.com/p/mageventory/issues/detail?id=131
			 */
			final DisplayMetrics m = getDisplayMetrics((Activity) getContext());

			String resizedImageURL = url.substring(0, url.indexOf("/", url.indexOf("http://") + "http://".length()));
			resizedImageURL = resizedImageURL + "/mventory_tm/image/get/file/"
					+ url.substring(url.lastIndexOf("/") + 1);
			resizedImageURL = resizedImageURL + "/width/"
					+ (m.widthPixels > m.heightPixels ? m.widthPixels : m.heightPixels);

			final HttpGet request = new HttpGet(resizedImageURL);

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

						// set these as view properties
						imgView.setTag(R.id.tkey_original_img_width, opts.outWidth);
						imgView.setTag(R.id.tkey_original_img_height, opts.outHeight);

						if (bitmap != null) {
							imgView.setImageBitmap(bitmap);

							/*
							 * Make sure to remove the image from the list
							 * before we create a file on the sdcard.
							 */
							ImageCachingManager.removeDownload(SKU, imageLocalPath);
							// Save Image in SD Card
							FileOutputStream imgWriter = new FileOutputStream(imageLocalPath);
							bitmap.compress(CompressFormat.JPEG, 100, imgWriter);
							imgWriter.flush();
							imgWriter.close();

							updateImageTextSize();
						}

						try {
							in.close();
						} catch (IOException ignored) {
						}
					}
				}
			} catch (Throwable e) {
				// NOP
			}

			// close client
			client.close();

			/* We can potentially call this twice but it's not a problem. */
			ImageCachingManager.removeDownload(SKU, imageLocalPath);
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			setLoading(false);
		}
	}

	private class loadStillDownloadingFromServerTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			// a view holder image is set until the upload is complete
			setLoading(true);
		}

		@Override
		protected Boolean doInBackground(Void... params) {

			File fileToProbe = new File(imageLocalPath);

			while (ImageCachingManager.isDownloadPending(SKU, imageLocalPath)) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Log.logCaughtException(e);
				}
			}

			if (fileToProbe.exists()) {
				Bitmap image = null;
				try {
					BitmapFactory.Options options = new BitmapFactory.Options();
					image = BitmapFactory.decodeFile(imageLocalPath, options);
				} catch (OutOfMemoryError e) {
				}

				if (image != null)
					imgView.setImageBitmap(image);
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			setLoading(false);
		}
	}

}
