package com.mageventory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.FileObserver;

import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.resprocessor.CartItemsProcessor;
import com.mageventory.resprocessor.CatalogCategoryTreeProcessor;
import com.mageventory.resprocessor.CatalogProductListProcessor;
import com.mageventory.resprocessor.ImageDeleteProcessor;
import com.mageventory.resprocessor.ImageMarkMainProcessor;
import com.mageventory.resprocessor.OrderDetailsProcessor;
import com.mageventory.resprocessor.OrdersListByStatusProcessor;
import com.mageventory.resprocessor.ProductAttributeAddOptionProcessor;
import com.mageventory.resprocessor.ProductAttributeFullInfoProcessor;
import com.mageventory.resprocessor.ProductDeleteProcessor;
import com.mageventory.resprocessor.ProductDetailsProcessor;
import com.mageventory.resprocessor.ProfileExecutionProcessor;
import com.mageventory.resprocessor.ProfilesListProcessor;
import com.mageventory.resprocessor.StatisticsProcessor;
import com.mageventory.settings.Settings;
import com.mageventory.settings.Settings.ProfileIDNotFoundException;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.ErrorReporterUtils;
import com.mageventory.util.ExternalImageUploader;
import com.mageventory.util.Log;
import com.mageventory.activity.ScanActivity;
import com.mageventory.components.ImagePreviewLayout;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.job.JobQueue;
import com.mageventory.jobprocessor.AddProductToCartProcessor;
import com.mageventory.jobprocessor.CreateProductProcessor;
import com.mageventory.jobprocessor.CreateShipmentProcessor;
import com.mageventory.jobprocessor.JobProcessorManager;
import com.mageventory.jobprocessor.SellProductProcessor;
import com.mageventory.jobprocessor.SubmitToTMProductProcessor;
import com.mageventory.jobprocessor.UpdateProductProcessor;
import com.mageventory.jobprocessor.UploadImageProcessor;
import com.mageventory.model.Product;

public class MyApplication extends Application implements MageventoryConstants {
	public static final String APP_DIR_NAME = "mventory";
	private FileObserver photosDirectoryFileObserver;
	private Object fileObserverMutex = new Object();
	private static final String TAG_GALLERY = "GALLERY_EXTERNAL_CAM_MYAPP";
	private BroadcastReceiver mSDCardStateChangeListener;
	private ExternalImageUploader mExternalImageUploader;
	
	/* We want to have just one UploadAllImagesTask active at any given moment as it doesn't make sense to have more than one
	 * (No need to do the same thing more than once). This reference is used to keep track of this task. */
	private UploadAllImagesTask mCurrentUploadAllImagesTask;

	public class ApplicationExceptionHandler implements UncaughtExceptionHandler {

		private UncaughtExceptionHandler defaultUEH;

		public ApplicationExceptionHandler() {
			this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
		}

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			Log.logUncaughtException(e);
			defaultUEH.uncaughtException(t, e);
		}
	}

	private class UploadAllImagesTask extends AsyncTask<Void, Void, Boolean> {
		
		private String mGalleryPath;
		private Settings mSettings;

		public UploadAllImagesTask(String galleryPath)
		{
			mGalleryPath = galleryPath;
			mSettings = new Settings(MyApplication.this);
		}
		
		@Override
		protected Boolean doInBackground(Void... args)
		{
			Log.d(TAG_GALLERY, "uploadAllImages(); Uploading all images from the path: " + mGalleryPath);
			
			final File galleryDir = new File(mGalleryPath);
			
			if (!galleryDir.exists())
			{
				Log.d(TAG_GALLERY, "uploadAllImages(); Gallery folder does not exist. Cannot upload any images.");
				return false;
			}
			
			final File [] imageFiles = galleryDir.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String filename) {
					
					if (filename.toLowerCase().endsWith(".jpg"))
					{
						return true;
					}
					
					return false;
				}
			});
			
			Arrays.sort(imageFiles, new Comparator<File>() {

				@Override
				public int compare(File lhs, File rhs) {
					return lhs.getName().compareTo(rhs.getName());
				}
			});
			
			for (File file : imageFiles)
			{
				if (mSettings.getExternalPhotosCheckBox() == false)
					return false;
				
				mExternalImageUploader.scheduleImageUpload(file.getAbsolutePath());
			}

			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			mCurrentUploadAllImagesTask = null;
		}
	}
	
	private Object uploadAllImagesSynchronisationObject = new Object();
	public void uploadAllImages(String galleryPath)
	{
		Settings settings = new Settings(this);
		
		synchronized(uploadAllImagesSynchronisationObject)
		{
			if (settings.getExternalPhotosCheckBox() == true && mCurrentUploadAllImagesTask == null)
			{
				mCurrentUploadAllImagesTask = new UploadAllImagesTask(galleryPath);
				mCurrentUploadAllImagesTask.execute();
			}
		}
	}
	
	private String currentGalleryPath = null;
	public void registerFileObserver(final String galleryPath)
	{
		final Settings settings = new Settings(this);
		
		Log.d(TAG_GALLERY, ">>>>>>> Trying to register file observer, path:" + galleryPath);
		
	synchronized(fileObserverMutex)
	{
		if (photosDirectoryFileObserver == null || currentGalleryPath != galleryPath)
		{
			Log.d(TAG_GALLERY, "Current file observer is null or the gallery path to be observed should change. Proceeding.");
			if (galleryPath != null && new File(galleryPath).exists())
			{
				Log.d(TAG_GALLERY, "Gallery path is not null and gallery folder exists. Proceeding.");

				uploadAllImages(galleryPath);
				
				photosDirectoryFileObserver = new FileObserver(galleryPath) {
					@Override
					public void onEvent(int event, String path) {
					synchronized(fileObserverMutex)
					{
						if (settings.getExternalPhotosCheckBox() == false)
						{
							return;
						}
						
						Log.d(TAG_GALLERY, "FileObserver onEvent(); event = " + event + ", path = " + path);
						
						if (event == FileObserver.CLOSE_WRITE || event == FileObserver.CLOSE_NOWRITE || event == FileObserver.MOVED_TO)
						{
							String imagePath = (new File(galleryPath, path)).getAbsolutePath();
							
							if (imagePath.toLowerCase().endsWith(".jpg"))
							{
								Log.d(TAG_GALLERY, "FileObserver onEvent(); uploadImage()");
								mExternalImageUploader.scheduleImageUpload(imagePath);
							}
						}
						else
						if (event == FileObserver.DELETE_SELF || event == FileObserver.MOVE_SELF)
						{
							Log.d(TAG_GALLERY, "FileObserver onEvent(); event = FileObserver.DELETE_SELF || FileObserver.MOVE_SELF");
							
							photosDirectoryFileObserver = null;
							currentGalleryPath = null;
						}
						}
					}
				};
				currentGalleryPath = galleryPath;
				
				Log.d(TAG_GALLERY, "Starting watching with the file observer.");
				photosDirectoryFileObserver.startWatching();
			}
			else
			{
				Log.d(TAG_GALLERY, "Gallery path is null or gallery folder doesn't exist. Deregistering the observer. ");
				
				if (photosDirectoryFileObserver != null)
					photosDirectoryFileObserver.stopWatching();
				currentGalleryPath = null;
			}
		}
		else
		{
			Log.d(TAG_GALLERY, "Current file observer is not null and the gallery path to be observed is the same. No need to reregister " +
					"observer.");
		}

	}
	}
	
	void registerSDCardStateChangeListener()
	{
		final Settings settings = new Settings(this);
		
	synchronized(fileObserverMutex)
	{

	    final String MEDIA_MOUNTED = "android.intent.action.MEDIA_MOUNTED";
	    
	    final String MEDIA_REMOVED = "android.intent.action.MEDIA_REMOVED";
	    final String MEDIA_UNMOUNTED = "android.intent.action.MEDIA_UNMOUNTED";
	    final String MEDIA_BAD_REMOVAL = "android.intent.action.MEDIA_BAD_REMOVAL";
        final String MEDIA_EJECT = "android.intent.action.MEDIA_EJECT";

	    mSDCardStateChangeListener = new BroadcastReceiver() {

	        @Override
	        public void onReceive(Context context, Intent intent) {
	            String action = intent.getAction();
	            
	            if(action.equalsIgnoreCase(MEDIA_MOUNTED)) {
	            	registerFileObserver(settings.getGalleryPhotosDirectory());
	            	Log.d(TAG_GALLERY, "sdcard mounted");
	            }
	            else
	            if( action.equalsIgnoreCase(MEDIA_REMOVED) ||
	    	      	action.equalsIgnoreCase(MEDIA_UNMOUNTED) ||
	    	        action.equalsIgnoreCase(MEDIA_BAD_REMOVAL) ||
	    	        action.equalsIgnoreCase(MEDIA_EJECT) )
	            {
	            	if (photosDirectoryFileObserver != null)
						photosDirectoryFileObserver.stopWatching();
					currentGalleryPath = null;
					Log.d(TAG_GALLERY, "sdcard unmounted");
					mExternalImageUploader.clearImageQueue();
	            }
	        }
	    };

	    IntentFilter filter = new IntentFilter();
	    filter.addAction(MEDIA_MOUNTED);
	    filter.addAction(MEDIA_REMOVED);
	    filter.addAction(MEDIA_UNMOUNTED);
	    filter.addAction(MEDIA_BAD_REMOVAL);
	    filter.addAction(MEDIA_EJECT);
	    
	    filter.addDataScheme("file");
	    registerReceiver(mSDCardStateChangeListener, filter);

	}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mExternalImageUploader = new ExternalImageUploader(this);
		
		registerSDCardStateChangeListener();
		
		Settings settings = new Settings(this); 
		
		configure();

		Thread.setDefaultUncaughtExceptionHandler(new ApplicationExceptionHandler());
		
		String galleryPath = settings.getGalleryPhotosDirectory();

		registerFileObserver(galleryPath);
	}

	private void configure() {
		final ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
		resHelper.bindResourceProcessor(RES_CATALOG_PRODUCT_LIST, new CatalogProductListProcessor());
		resHelper.bindResourceProcessor(RES_PRODUCT_DETAILS, new ProductDetailsProcessor());
		resHelper.bindResourceProcessor(RES_CATALOG_CATEGORY_TREE, new CatalogCategoryTreeProcessor());
		resHelper.bindResourceProcessor(RES_CATALOG_PRODUCT_ATTRIBUTES, new ProductAttributeFullInfoProcessor());
		resHelper.bindResourceProcessor(RES_PRODUCT_ATTRIBUTE_ADD_NEW_OPTION, new ProductAttributeAddOptionProcessor());
		resHelper.bindResourceProcessor(RES_PRODUCT_DELETE, new ProductDeleteProcessor());
		resHelper.bindResourceProcessor(RES_DELETE_IMAGE, new ImageDeleteProcessor());
		resHelper.bindResourceProcessor(RES_MARK_IMAGE_MAIN, new ImageMarkMainProcessor());
		resHelper.bindResourceProcessor(RES_ORDERS_LIST_BY_STATUS, new OrdersListByStatusProcessor());
		resHelper.bindResourceProcessor(RES_ORDER_DETAILS, new OrderDetailsProcessor());
		resHelper.bindResourceProcessor(RES_CATALOG_PRODUCT_STATISTICS, new StatisticsProcessor());
		resHelper.bindResourceProcessor(RES_GET_PROFILES_LIST, new ProfilesListProcessor());
		resHelper.bindResourceProcessor(RES_EXECUTE_PROFILE, new ProfileExecutionProcessor());
		resHelper.bindResourceProcessor(RES_CART_ITEMS, new CartItemsProcessor());
		
		JobProcessorManager.bindResourceProcessor(RES_ORDER_SHIPMENT_CREATE, new CreateShipmentProcessor());
		JobProcessorManager.bindResourceProcessor(RES_CATALOG_PRODUCT_UPDATE, new UpdateProductProcessor());
		JobProcessorManager.bindResourceProcessor(RES_CATALOG_PRODUCT_CREATE, new CreateProductProcessor());
		JobProcessorManager.bindResourceProcessor(RES_UPLOAD_IMAGE, new UploadImageProcessor());
		JobProcessorManager.bindResourceProcessor(RES_CATALOG_PRODUCT_SELL, new SellProductProcessor());
		JobProcessorManager.bindResourceProcessor(RES_CATALOG_PRODUCT_SUBMIT_TO_TM, new SubmitToTMProductProcessor());
		JobProcessorManager.bindResourceProcessor(RES_ADD_PRODUCT_TO_CART, new AddProductToCartProcessor());
	}
}