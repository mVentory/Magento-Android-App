package com.mageventory.util;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.media.ExifInterface;
import android.os.AsyncTask;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.settings.Settings.ProfileIDNotFoundException;

public class ExternalImageUploader implements MageventoryConstants {
	
	private static final String TAG_EXTERNAL_IMAGE_UPLOADER = "GALLERY_EXTERNAL_IMAGE_UPLOADER";
	
	private Context mContext;
	private LinkedList<String> mImagesToUploadQueue;
	private Object mQueueSynchronisationObject = new Object();
	
	private class UploadImageTask extends AsyncTask<String, Void, Boolean> implements OperationObserver {
		
		private String mSKU;
		private String mURL;
		private String mUser;
		private String mPassword;
		
		private SettingsSnapshot mSettingsSnapshot;
		private String mImagePath;
		private JobControlInterface mJobControlInterface;
		private ResourceServiceHelper mResHelper = ResourceServiceHelper.getInstance();
		private int mLoadReqId = INVALID_REQUEST_ID;
		private CountDownLatch mDoneSignal;
		private boolean mProductLoadSuccess;
		private Settings mSettings;
		private boolean mSKUTimestampModeSelected;
		private boolean mForceSKUTimestampMode;
		
		/* If we are not in "sku timestamp mode" (we are taking sku from the file name) and the sku doesn't exist in the cache
		 * nor on the server or we cannot check if it exists on the server then we want to retry the image upload in "sku timestamp mode" */
		private boolean retryFlag;

		public UploadImageTask(String imagePath, boolean forceSKUTimestampMode)
		{
			Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "UploadImageTask(" + imagePath + "," + forceSKUTimestampMode + ")");
			mForceSKUTimestampMode = forceSKUTimestampMode;
			
			mSettings = new Settings(mContext);
			
			mImagePath = imagePath;
			mJobControlInterface = new JobControlInterface(mContext);
		}
		
		/* Return true on success */
		private boolean getSKUAndOtherData()
		{
			long profileID = -1;
			File currentFile = new File(mImagePath);
			String fileName = currentFile.getName();
			
			if (!currentFile.exists())
			{
				Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "getSKUAndOtherData(); The image does not exist: " + mImagePath);
				return false;
			}

			if (!mForceSKUTimestampMode && fileName.contains("__"))
			{
				mSKUTimestampModeSelected = false;
				
				mSKU = fileName.substring(0, fileName.indexOf("__"));
				mURL = mSettings.getUrl();
				mUser = mSettings.getUser();
				mPassword = mSettings.getPass();
			}
			else
			{
				mSKUTimestampModeSelected = true;
				
				try {
					ExifInterface exif = new ExifInterface(mImagePath);
				
					String dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME);
					Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "getSKUAndOtherData(); Retrieved exif timestamp from the file: " + dateTime);
				
					String escapedSkuProfileID = JobCacheManager.getSkuProfileIDForExifTimeStamp(mContext, dateTime);
					Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "getSKUAndOtherData(); Retrieved escaped SKU and profile ID from the timestamps file: " + escapedSkuProfileID);
				
					if (escapedSkuProfileID != null)
					{
						String escapedSKU = escapedSkuProfileID.split(" ")[0];
						String profileIDString = escapedSkuProfileID.split(" ")[1];
					
						mSKU= URLDecoder.decode(escapedSKU, "UTF-8");
						profileID = Long.parseLong(profileIDString);
					
						Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "getSKUAndOtherData(); Decoded sku and profile ID: " + mSKU + ", " + profileID );
					
						Settings s;
						try {
							s = new Settings(mContext, profileID);
						} catch (ProfileIDNotFoundException e) {
							e.printStackTrace();
						
							Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "getSKUAndOtherData(); Profile is missing. Moving the image to BAD_PICS.");
						
							/* Profile is missing. Move the file to the "bad pics" dir. */
							boolean success = moveImageToBadPics(currentFile);
						
							if (success)
							{
								Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "getSKUAndOtherData(); Image moved to BAD_PICS with success.");
							}
							else
							{
								Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "getSKUAndOtherData(); Moving image to BAD_PICS FAILED.");
							}
						
							return false;
						}
					
						mURL = s.getUrl();
						mUser = s.getUser();
						mPassword = s.getPass();
						
						Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "getSKUAndOtherData(); Retrieving url from the profile: " + mURL );
					}
					else
					{
						Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "getSKUAndOtherData(); Retrieved escaped SKU and profile ID are null. Moving the image to BAD_PICS.");
					
						boolean success = moveImageToBadPics(currentFile);
					
						if (success)
						{
							Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "getSKUAndOtherData(); Image moved to BAD_PICS with success.");
						}
						else
						{
							Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "getSKUAndOtherData(); Moving image to BAD_PICS FAILED.");
						}
						return false;
					}
			
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}
			
			mSettingsSnapshot = new SettingsSnapshot(mContext);
			mSettingsSnapshot.setUser(mUser);
			mSettingsSnapshot.setPassword(mPassword);
			mSettingsSnapshot.setUrl(mURL);
			
			return true;
		}

		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		
		@Override
		protected Boolean doInBackground(String... args) {
			boolean res;
			
			Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "UploadImageTask; doInBackground();");
			
			if (mSettings.getExternalPhotosCheckBox() == false)
			{
				Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "UploadImageTask; doInBackground(); external photos checkbox is unchecked. Will clear the queue and return,.");

				/* External photos checkbox is unchecked. Let's clear the images queue. It will be refilled next time the checkbox
				 * is checked back on. */
				clearImageQueue();
				return false;
			}
			
			res = getSKUAndOtherData();
			
			if (res == false)
			{
				Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "UploadImageTask; doInBackground(); An attempt of figuring out the SKU failed for the" +
						"following file: " + mImagePath);
				return false;
			}
				
			/* Build a path to an image in the product folder where it needs to be placed in order to be uploaded. */
			File currentFile = new File(mImagePath);
			File imagesDir = JobCacheManager.getImageUploadDirectory(mSKU, mURL);
			File newImageFile = new File(imagesDir, currentFile.getName());
			
			if (newImageFile.exists())
			{
				newImageFile = new File(imagesDir, getModifedFileName(currentFile));
			}
			
			JobID jobID = new JobID(INVALID_PRODUCT_ID, RES_UPLOAD_IMAGE, mSKU, null);
			
			Job uploadImageJob = new Job(jobID, mSettingsSnapshot);

			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_NAME,
					newImageFile.getName().substring(0, newImageFile.getName().toLowerCase().lastIndexOf(".jpg")));

			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_CONTENT, newImageFile.getAbsolutePath());
			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_MIME, "image/jpeg");
			
			boolean doAddJob = true;
			boolean prodDetExists = JobCacheManager.productDetailsExist(uploadImageJob.getSKU(), uploadImageJob.getUrl());
				
			if (prodDetExists == false)
			{
				//download product details
				final String[] params = new String[2];
				params[0] = GET_PRODUCT_BY_SKU; // ZERO --> Use Product ID , ONE -->
												// Use Product SKU
				params[1] = uploadImageJob.getSKU();
					
				mResHelper.registerLoadOperationObserver(this);
				mLoadReqId = mResHelper.loadResource(mContext, RES_PRODUCT_DETAILS, params, mSettingsSnapshot);

				mDoneSignal = new CountDownLatch(1);
				while (true) {
					if (isCancelled()) {
						return true;
					}
					try {
						if (mDoneSignal.await(1, TimeUnit.SECONDS)) {
							break;
						}
					} catch (InterruptedException e) {
						return true;
					}
				}
						
				mResHelper.unregisterLoadOperationObserver(this);
					
				if (mProductLoadSuccess == false)
				{
					doAddJob = false;
				}
			}
			
			if (doAddJob == true)
			{
				retryFlag = false;
				
				if (currentFile.renameTo(newImageFile) == false)
				{
					Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "UploadImageTask; Failed to move the file to the right directory before uploading. The dir path: " + imagesDir.getAbsolutePath());
					return true;
				}
				else
				{
					Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "UploadImageTask; Moved file, from: " + currentFile.getAbsolutePath() + ", to:" + newImageFile.getAbsolutePath());
				}
				
				mJobControlInterface.addJob(uploadImageJob);
			}
			else
			{
				if (mSKUTimestampModeSelected == false)
				{
					/* If we are here it means product details are not in the cache nor on the server
					 * (OR product details are not in the cache and we don't know whether they are on the server).
					 * In this case we retry the upload using the gallery file. */
					retryFlag = true;	
				}
				else
				{
					retryFlag = false;
					
					boolean success = moveImageToBadPics(currentFile);
				
					if (success)
					{
						Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "UploadImageTask; Image moved to BAD_PICS with success.");
					}
					else
					{
						Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "UploadImageTask; Moving image to BAD_PICS FAILED.");
					}
				}
			}

			return true;
		}

		@Override
		public void onLoadOperationCompleted(LoadOperation op) {
			if (op.getOperationRequestId() == mLoadReqId) {

				if (op.getException() == null) {
					mProductLoadSuccess = true;
				} else {
					mProductLoadSuccess = false;
				}
				mDoneSignal.countDown();
			}
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
			if (retryFlag)
			{
				Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "UploadImageTask; Retrying with forced sku timestamp mode, for image: " + mImagePath);
				new UploadImageTask(mImagePath, true).execute();
			}
			else
			{
				synchronized(mQueueSynchronisationObject)
				{
					mImagesToUploadQueue.remove(mImagePath);
					processNextImageFromQueue();
				}
			}
		}
	}
	
	public void clearImageQueue()
	{
		synchronized(mQueueSynchronisationObject)
		{
			Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "Clearing image queue.");
			mImagesToUploadQueue.clear();
		}
	}
	
	private void processNextImageFromQueue()
	{
		if (mImagesToUploadQueue.size() > 0)
		{
			Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "processNextImageFromQueue(), processing next image: " + mImagesToUploadQueue.getFirst());
			Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "processNextImageFromQueue(), image queue size: " + mImagesToUploadQueue.size());

			new UploadImageTask(mImagesToUploadQueue.getFirst(), false).execute();
		}
	}
	
	public ExternalImageUploader(Context c)
	{
		mContext = c;
		mImagesToUploadQueue = new LinkedList<String>();
	}
	
	/* Appends a timestamp at the end of the file name of the file passed and returns the new modified file name as
	 * a String. */
	private String getModifedFileName(File imageFile)
	{
		long currentTime = System.currentTimeMillis();

		int lastDotIndex = imageFile.getName().lastIndexOf(".");
		
		String newFileName;
		
		if (lastDotIndex == -1)
		{
			newFileName = imageFile.getName() + "_" + currentTime;
		}
		else
		{
			newFileName = imageFile.getName().substring(0, lastDotIndex);
			newFileName = newFileName + "_" + currentTime + imageFile.getName().substring(lastDotIndex);
		}
		
		return newFileName;
	}
	
	private boolean moveImageToBadPics(File imageFile)
	{
		File badPicsDir = JobCacheManager.getBadPicsDir();
		
		File moveHere = new File(badPicsDir, imageFile.getName());

		/* Append a timestamp to the end of the file name only if it already exists to avoid conflict. */
		if (moveHere.exists())
		{
			moveHere = new File(badPicsDir, getModifedFileName(imageFile));
		}
		
		boolean success = imageFile.renameTo(moveHere);
		
		return success;
	}
	
	public void scheduleImageUpload(String path)
	{
		synchronized(mQueueSynchronisationObject)
		{
			Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "> scheduleImageUpload()");

			if (!mImagesToUploadQueue.contains(path))
			{
				Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "scheduleImageUpload(), adding image path to the queue: " + path);
				mImagesToUploadQueue.addLast(path);
				
				/* If the only thing in the queue is the path we just added then it means we need to wake up the queue because
				 * there is nothing being processed at the moment. */
				if (mImagesToUploadQueue.size() == 1)
				{
					Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "scheduleImageUpload(), the image we added is the only one in the queue. Starting the queue.");
					processNextImageFromQueue();
				}
			}
			Log.d(TAG_EXTERNAL_IMAGE_UPLOADER, "< scheduleImageUpload()");
		}
	}
}
