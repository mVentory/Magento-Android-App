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

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.client.MagentoClient;
import com.mageventory.interfaces.IOnClickManageHandler;
import com.mageventory.job.Job;
import com.mageventory.job.JobCallback;
import com.mageventory.job.JobControlInterface;
import com.mageventory.jobprocessor.UploadImageProcessor;
import com.mageventory.res.ResourceServiceHelper;

/**
 * LinearLayout containing three elements: one <code>ImageView</code>, one delete <code>Button</code> and one <code>CheckBox</code>
 * and an indeterminate progress bar for the loading, the deletion or the update of the image on server 
 *
 * @author Bogdan Petran
 */
public class ImagePreviewLayout extends FrameLayout implements MageventoryConstants {

	/* TODO: Temporary piece of information to be able to delete things from cache. This will be deleted in the future. */
	public String[] productDetailsCacheParams;	
	
    // private int uploadPhotoID = 0;
    // private int uploadImageRequestId = INVALID_REQUEST_ID;
    ResourceServiceHelper resHelper;
    
	/**
	 * This task updates the image position on server
	 */
	private class UpdateImageOnServerTask extends AsyncTask<String, Void, Boolean>{

		@Override
		protected void onPreExecute() {
			// a view holder image is set until the upload is complete
			setLoading(true);
		}

		@Override
		protected Boolean doInBackground(String... params) {

			String productId = params[0];

			MyApplication app = (MyApplication)((Activity) getContext()).getApplication();
			MagentoClient magentoClient = app.getClient();

			// build the request data
			HashMap<String, Object> image_data = new HashMap<String, Object>();

			image_data.put("position", index);
			
			if(index == 0){
				// make first image as main image on server
				image_data.put("types", new Object[]{"image", "small_image", "thumbnail"});
			}

			boolean responseFromServer;
			
			// make a synchronized request
			synchronized (lockMutex) {
				responseFromServer = (Boolean) magentoClient.execute("catalog_product_attribute_media.update", 
						new Object[] {productId, imageName, image_data});
			}

			return responseFromServer;
		}

		@Override
		protected void onPostExecute(Boolean result) {

			//TODO: result can be true or false but don't know what to do in either of this cases because an image can recieve a true result while another false
			
			setLoading(false);
		}
	}

	private class EventListener implements OnClickListener, OnCheckedChangeListener{

		WeakReference<ImagePreviewLayout> viewReference; 
		final ImagePreviewLayout viewInstance; 
		
		public EventListener(ImagePreviewLayout instance){ 
			viewReference = new WeakReference<ImagePreviewLayout>(instance); 
			viewInstance = viewReference.get(); 
		} 
		
		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.imageViewHolder) {
				// if this will be necessary, it will start the edit image view when clicking the image
				onClickManageHandler.onClickForEdit(viewInstance);
			} else if (v.getId() == R.id.deleteBtn) {
				// notify to delete current layout and image from server
				onClickManageHandler.onDelete(viewInstance);
			}
		}

		@Override
		public void onCheckedChanged(final CompoundButton buttonView,
				final boolean isChecked) {
			
			System.out.println("is checked: " + isChecked + " ask for aproval: " + askForMainImageApproval );
			// don't let this button to be clicked when it's checked but let it when it's unchecked
			buttonView.setFocusable(!isChecked);
			buttonView.setClickable(!isChecked);
			
			if(isChecked && askForMainImageApproval && (!setAsMainImageOverride)){
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
						
						// notify to set the selected image as main/first image
						onClickManageHandler.onClickForMainImage(viewInstance);
					}
				});
				
				dialogBuilder.show();
			}
			setAsMainImageOverride = false;
		}
	}
	
	private EventListener eventListener;

	// private String IMAGES_URL = "http://mventory.simple-helix.net/media/catalog/product";

	private String url;									// this will be IMAGES_URL + "imageName"
	private String imageName;
	private int productID;
	private Job uploadJob;
	private ImageView imgView;
	private Button deleteBtn;
	private ProgressBar loadingProgressBar;
	private ProgressBar uploadingProgressBar;	
	private CheckBox mainImageCheckBox;
	private IOnClickManageHandler onClickManageHandler;	// handler for parent layout notification (when a delete or edit click occures inside this layout)
	private int index;									// the index of this layout within its parrent
	private int errorCounter = 0;						// counter used when an upload error occures (if this is > 0, this layout and the image coresponding to this layout, will be deleted)
	private boolean askForMainImageApproval = true;
	private boolean setAsMainImageOverride = false;     // if this is true the image won't be set as main in case the checkox get checked
	private String imagePath; 							// image path is saved for the case when some error occures and we need to try again the image upload
	
	private LinearLayout elementsLayout;
	
	private TextView imageSizeTxtView;
	private String imageLocalPath;
	
	private static Object lockMutex = new Object();		// object used to synchronize requests

	public ImagePreviewLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public ImageView getImageView() {
	    return imgView;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		// only after the inflate is finished we can get references to the ImageView and the delete Button
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

		// this is necessary only in the case when setUrl is called from outside and the imgView is null then
		if(url != null){
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
	 * @param url of the image to be shown in <code>ImageView</code>
	 */
	public void setUrl(String url) {
		this.url = url;

		if(imgView != null){
			setImageFromUrl();
		}
	}
	
	/**
	 * 
	 * @param listener <code>OnClickManageHandler</code> used to handle click events outside this layout
	 */
	public void setManageClickListener(IOnClickManageHandler listener){
		onClickManageHandler = listener;
	}

	
	private void setImageFromUrl(){
		ImageCachingManager.addDownload(productID, imageLocalPath);
		new DownloadImageFromServerTask().execute();	
	}
	
	/**
	 * Set the image size in the image size TextView
	 */
	public void updateImageTextSize(){
		Drawable imgDrawable = imgView.getDrawable();
		if(imgDrawable == null){
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
	 * @param imageUrl which is the name of the image saved on server
	 */
	// y XXX: REWORK THAT
	public void setImageUrl(String imageUrl) {
		setUrl(imageUrl);
	}

	public void setProductID(int productID)
	{
		this.productID = productID;
	}
	
	public void setUploadJob(final Job job, final JobControlInterface jobControl)
	{
		uploadJob = job;
		setUploading(true);
	}
	
	public void registerCallbacks(final JobControlInterface jobControl)
	{
		if (uploadJob == null)
			return;
		
		final Context context = this.getContext();
		
		jobControl.registerJobCallback(uploadJob.getJobID(), new JobCallback() {
			
			@Override
			public void onJobStateChange(final Job job) {

				uploadingProgressBar.setProgress(job.getProgressPercentage());
				
				if (job.getFinished() == true)
				{
					ImagePreviewLayout.this.post(new Runnable() {
						@Override
						public void run()
						{
							setImageName((String)job.getServerResponse());
							setImageLocalPath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MageventoryImages/" + productID);
							
							synchronized(ImageCachingManager.sSynchronisationObject)
							{
								if (!ImageCachingManager.isDownloadPending(productID, imageLocalPath))
								{
									setLoading(true);
									setImageUrl(UploadImageProcessor.IMAGE_SERVER_PATH + (String)job.getServerResponse());	
								}
							}
						}
					});
					
					if (productDetailsCacheParams != null)
						ResourceServiceHelper.getInstance().deleteResource(context, RES_PRODUCT_DETAILS, productDetailsCacheParams);
					jobControl.removeFromCache(job.getJobID());
					jobControl.deregisterJobCallback(job.getJobID(), this);
				}
			}
		});
	}
	
	public void deregisterCallbacks(final JobControlInterface jobControl)
	{
		if (uploadJob != null)
		{
			jobControl.deregisterJobCallback(uploadJob.getJobID(), null);
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
	 * @param index of this view inside its parent
	 */
	public void setIndex(int index) {
		this.index = index;
	}
	
	/**
	 * You must call setImagePath before calling this method!
	 * 
	 * @param index is the index of this view inside its parent
	 * 
	 * @see ImagePreviewLayout#setImagePath(String)
	 */
	
	public int sendImageToServer(int index) {
		this.index = index;
		errorCounter = 0;
		return startNewUploadOperation();
	}
	
	public void updateImageIndex(String productId, int index){
		this.index = index;
		
		if (productDetailsCacheParams != null)
			ResourceServiceHelper.getInstance().deleteResource(this.getContext(), RES_PRODUCT_DETAILS, productDetailsCacheParams);
		
		new UpdateImageOnServerTask().execute(productId);
	}

	public void setLoading(boolean isLoading){
		if(isLoading){
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

	public void setUploading(boolean isUploading){
		if(isUploading){
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
	private void setVisibilityToChilds(int visibility){
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
	 * Changes the check state for the checkbox contained by this view. Used for the first image so that when it is loaded from server, make it main.
	 * 
	 * @param checked
	 */
	public void setMainImageCheck(boolean checked){
		setAsMainImageOverride = true;
		mainImageCheckBox.setChecked(checked);
	}
	
	// y FIXME: this view is just a mess, REWORK IT!
    // y: it's bad we do this in the UI thread, but it provides us with proper synchronization, which is what we need
    // right now
	private int startNewUploadOperation() {
        File imgFile = new File(getImagePath());
        final Bundle bundle = new Bundle();
        bundle.putString(MAGEKEY_PRODUCT_IMAGE_NAME, imgFile.getName());
        bundle.putString(MAGEKEY_PRODUCT_IMAGE_CONTENT, imagePath);
        bundle.putString(MAGEKEY_PRODUCT_IMAGE_MIME, "image/jpeg");
        bundle.putString(MAGEKEY_PRODUCT_SKU, imgFile.getParentFile().getName()); // y: ...FIXME
        bundle.putString(MAGEKEY_PRODUCT_IMAGE_POSITION, String.valueOf(index));
        return resHelper.loadResource(getContext(), RES_UPLOAD_IMAGE, new String[] {imagePath}, bundle);
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

	
	
	public void loadFromSD(String imageName, String folderPath)
	{
		this.setImageName(imageName);
		this.setImageLocalPath(folderPath);
		Bitmap image = null;
		try
		{
			BitmapFactory.Options options = new BitmapFactory.Options();
			image = BitmapFactory.decodeFile(this.getImageLocalPath(), options);
		}
		catch(OutOfMemoryError e)
		{
		}
		loadingProgressBar.setVisibility(View.GONE);
		imgView.setVisibility(View.VISIBLE);
		if(image != null)
			this.imgView.setImageBitmap(image);
	}
	
	public void loadFromSDPendingDownload(String imageName, String folderPath)
	{
		this.setImageName(imageName);
		this.setImageLocalPath(folderPath);
		
		new loadStillDownloadingFromServerTask().execute();
		
	}

	/**
	 * @return the imageLocalPath
	 */
	public String getImageLocalPath() {
		return imageLocalPath;
	}

	/**
	 * @param imageLocalPath the imageLocalPath to set
	 */
	public void setImageLocalPath(String imageLocalPath) {
		String [] name = imageName.split("/");
		
		this.imageLocalPath = imageLocalPath + "/" + name[name.length-1];
	}
	

	/**
	 * This task updates the image position on server
	 */
	private class DownloadImageFromServerTask extends AsyncTask<Void, Void, Boolean>{

		@Override
		protected void onPreExecute() {
			// a view holder image is set until the upload is complete
			setLoading(true);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
		    final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
	        final HttpGet request = new HttpGet(url);
	        
	        BitmapFactory.Options opts = new BitmapFactory.Options();
	        InputStream in = null;
	        int coef = 0;

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
	                    opts.inJustDecodeBounds = true;
	                    BitmapFactory.decodeStream(in, null, opts);
	                    
	                    // set these as view properties
	                    imgView.setTag(R.id.tkey_original_img_width, opts.outWidth);
	                    imgView.setTag(R.id.tkey_original_img_height, opts.outHeight);
	                    
	                    final DisplayMetrics m = getDisplayMetrics((Activity) getContext());
	                    coef = Integer.highestOneBit(opts.outWidth / m.widthPixels);
	                    
	                    try {
	                        in.close();
	                    } catch (IOException ignored) {
	                    }
	                }
	            }
	            
	            response = client.execute(request);
	            entity = response.getEntity();
	            if (entity != null) {
	                in = entity.getContent();
	                if (in != null) {
	                    in = new BufferedInputStream(in);

	                    opts.inJustDecodeBounds = false;
	                    if (coef > 1) {
	                        opts.inSampleSize = coef;
	                    }
	                    final Bitmap bitmap = BitmapFactory.decodeStream(in, null, opts);
	                    if (bitmap != null) {
	                        imgView.setImageBitmap(bitmap);
	                        
	                        /* Make sure to remove the image from the list before we create a file on the sdcard. */
	            			ImageCachingManager.removeDownload(productID, imageLocalPath);
	                        // Save Image in SD Card
	                        FileOutputStream imgWriter =  new FileOutputStream(imageLocalPath);
	                        bitmap.compress(CompressFormat.JPEG, 100,imgWriter);
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
			ImageCachingManager.removeDownload(productID, imageLocalPath);
	        return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			setLoading(false);
		}
	}


	private class loadStillDownloadingFromServerTask extends AsyncTask<Void, Void, Boolean>{

		@Override
		protected void onPreExecute() {
			// a view holder image is set until the upload is complete
			setLoading(true);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			
			File fileToProbe = new File(imageLocalPath);
			
			while(ImageCachingManager.isDownloadPending(productID, imageLocalPath))
			{
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
					
			if (fileToProbe.exists())
			{
				Bitmap image = null;
				try
				{
					BitmapFactory.Options options = new BitmapFactory.Options();
					image = BitmapFactory.decodeFile(imageLocalPath, options);
				}
				catch(OutOfMemoryError e)
				{
				}
				
				if(image != null)
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
