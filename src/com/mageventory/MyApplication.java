package com.mageventory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import android.app.Application;
import android.content.Context;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.FileObserver;

import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.resprocessor.CatalogCategoryTreeProcessor;
import com.mageventory.resprocessor.CatalogProductListProcessor;
import com.mageventory.resprocessor.ImageDeleteProcessor;
import com.mageventory.resprocessor.ImageMarkMainProcessor;
import com.mageventory.resprocessor.ProductAttributeAddOptionProcessor;
import com.mageventory.resprocessor.ProductAttributeFullInfoProcessor;
import com.mageventory.resprocessor.ProductDeleteProcessor;
import com.mageventory.resprocessor.ProductDetailsProcessor;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.Log;
import com.mageventory.components.ImagePreviewLayout;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.jobprocessor.CreateProductProcessor;
import com.mageventory.jobprocessor.JobProcessorManager;
import com.mageventory.jobprocessor.SellProductProcessor;
import com.mageventory.jobprocessor.UpdateProductProcessor;
import com.mageventory.jobprocessor.UploadImageProcessor;

public class MyApplication extends Application implements MageventoryConstants {
	public static final String APP_DIR_NAME = "mventory";
	private Settings mSettings;
	private FileObserver photosDirectoryFileObserver;
	private Object fileObserverMutex = new Object();

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
	
	
	private class UploadImageTask extends AsyncTask<String, Void, Boolean> {
		
		private Job mUploadImageJob;
		private String mSKU;
		private SettingsSnapshot mSettingsSnapshot;
		private String mImagePath;
		private JobControlInterface mJobControlInterface;

		public UploadImageTask(Context c, String sku, String url, String imagePath)
		{
			mSKU = sku;
			
			Settings s = new Settings(c, url);
			
			mSettingsSnapshot = new SettingsSnapshot(c);
			mSettingsSnapshot.setUser(s.getUser());
			mSettingsSnapshot.setPassword(s.getPass());
			mSettingsSnapshot.setUrl(url);
			
			mImagePath = imagePath;
			mJobControlInterface = new JobControlInterface(c);
		}
		
		@Override
		protected Boolean doInBackground(String... args) {
			JobID jobID = new JobID(INVALID_PRODUCT_ID, RES_UPLOAD_IMAGE, mSKU, null);
			
			Job uploadImageJob = new Job(jobID, mSettingsSnapshot);

			File file = new File(mImagePath);

			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_NAME,
					file.getName().substring(0, file.getName().lastIndexOf(".jpg")));

			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_CONTENT, mImagePath);
			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_MIME, "image/jpeg");
			//uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_NAME, instance.getName());

			mJobControlInterface.addJob(uploadImageJob);

			mUploadImageJob = uploadImageJob;

			return true;
		}
	}
	
	private void uploadImage(String path)
	{
		String sku, url;
		File currentFile = new File(path);
		
		if (!currentFile.exists())
		{
			return;
		}
		
		try {
			ExifInterface exif = new ExifInterface(path);
			
			String dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME);
			String escapedSkuUrl = JobCacheManager.getSkuUrlForExifTimeStamp(dateTime);
			
			if (escapedSkuUrl != null)
			{
				String escapedSKU = escapedSkuUrl.split(" ")[0];
				String escapedUrl = escapedSkuUrl.split(" ")[1];
				
				sku = URLDecoder.decode(escapedSKU, "UTF-8");
				url = URLDecoder.decode(escapedUrl, "UTF-8");
			}
			else
			{
				File badPicsDir = JobCacheManager.getBadPicsDir();
				File moveHere = new File(badPicsDir, currentFile.getName());
				
				currentFile.renameTo(moveHere);
				return;
			}
		
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		/* Move the image and upload it. */
		long currentTime = System.currentTimeMillis();
		File imagesDir = JobCacheManager.getImageUploadDirectory(sku, url);
		String extension = path.substring(path.lastIndexOf("."));
		String imageName = String.valueOf(currentTime) + extension;
			
		final File newFile = new File(imagesDir, imageName);
			
		if (currentFile.renameTo(newFile) == false)
		{
			return;
		}
		
		UploadImageTask u = new UploadImageTask(MyApplication.this, sku, url, newFile.getAbsolutePath());
		u.execute();
	}

	private void uploadAllImages(String galleryPath)
	{
		final File galleryDir = new File(galleryPath);
		
		if (!galleryDir.exists())
		{
			return;
		}
		
		final File [] imageFiles = galleryDir.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String filename) {
				
				if (filename.endsWith(".jpg"))
				{
					return true;
				}
				
				return false;
			}
		});
		
		
		for (File file : imageFiles)
		{
			uploadImage(file.getAbsolutePath());
		}
	}
	
	private String currentGalleryPath = null;
	public void registerFileObserver(final String galleryPath)
	{
	synchronized(fileObserverMutex)
	{
		if (photosDirectoryFileObserver == null || currentGalleryPath != galleryPath)
		{
			if (galleryPath != null && new File(galleryPath).exists())
			{
				uploadAllImages(galleryPath);
				
				photosDirectoryFileObserver = new FileObserver(galleryPath) {
					@Override
					public void onEvent(int event, String path) {
					synchronized(fileObserverMutex)
					{
						if (event == FileObserver.CLOSE_WRITE)
						{
							String imagePath = (new File(galleryPath, path)).getAbsolutePath();
							uploadImage(imagePath);
						}
						else
						if (event == FileObserver.DELETE_SELF || event == FileObserver.MOVE_SELF)
						{
							photosDirectoryFileObserver = null;
							currentGalleryPath = null;
						}
						}
					}
				};
				currentGalleryPath = galleryPath;
				photosDirectoryFileObserver.startWatching();
			}
			else
			{
				if (photosDirectoryFileObserver != null)
					photosDirectoryFileObserver.stopWatching();
				currentGalleryPath = null;
			}
		}

	}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mSettings = new Settings(this);
		configure();

		Thread.setDefaultUncaughtExceptionHandler(new ApplicationExceptionHandler());
		
		String galleryPath = mSettings.getGalleryPhotosDirectory();

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
		
		JobProcessorManager.bindResourceProcessor(RES_CATALOG_PRODUCT_UPDATE, new UpdateProductProcessor());
		JobProcessorManager.bindResourceProcessor(RES_CATALOG_PRODUCT_CREATE, new CreateProductProcessor());
		JobProcessorManager.bindResourceProcessor(RES_UPLOAD_IMAGE, new UploadImageProcessor());
		JobProcessorManager.bindResourceProcessor(RES_CATALOG_PRODUCT_SELL, new SellProductProcessor());
	}
}