package com.mageventory.job;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.Time;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.Base64Coder_magento;
import com.mageventory.model.CarriersList;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.model.Product;
import com.mageventory.settings.Settings;
import com.mageventory.util.Log;

/* Contains methods for performing operations on the cache. */
public class JobCacheManager {

	public static Object sSynchronizationObject = new Object();
	
	private static final String PRODUCT_LIST_DIR_NAME = "product_lists";
	private static final String ORDER_DETAILS_DIR_NAME = "order_details";
	private static final String PROFILE_EXECUTION_DIR_NAME = "profile_execution";
	private static final String ATTRIBUTE_LIST_DIR_NAME = "attribute_list";
	private static final String ORDER_LIST_DIR_NAME = "order_list";
	private static final String QUEUE_DATABASE_DUMP_DIR_NAME = "database_dump";
	private static final String DOWNLOAD_IMAGE_PREVIEW_DIR_NAME = "DOWNLOAD_IMAGE_PREVIEW";
	private static final String DOWNLOAD_IMAGE_DIR = "DOWNLOAD_IMAGE";

	public static final String LOG_DIR_NAME = "log";
	private static final String ERROR_REPORTING_DIR_NAME = "error_reporting";
	private static final String ERROR_REPORTING_FILE_NAME = "error_reporting_timestamps";
	
	private static final String GALLERY_BAD_PICS_DIR_NAME = "bad_pics";
	private static final String GALLERY_TIMESTAMPS_DIR_NAME = "GALLERY_TIMESTAMPS";
	private static final String GALLERY_TIMESTAMPS_FILE_NAME = "gallery_timestamps.txt";
	
	private static final String PRODUCT_DETAILS_FILE_NAME = "prod_dets.obj";
	private static final String ATTRIBUTE_SETS_FILE_NAME = "attribute_sets.obj";
	private static final String CATEGORIES_LIST_FILE_NAME = "categories_list.obj";
	private static final String ORDER_CARRIERS_FILE_NAME = "order_carriers.obj";
	private static final String STATISTICS_FILE_NAME = "statistics.obj";
	private static final String PROFILES_FILE_NAME = "profiles.obj";
	private static final String INPUT_CACHE_FILE_NAME = "input_cache.obj";
	private static final String LAST_USED_ATTRIBUTES_FILE_NAME = "last_used_attributes_list.obj";
	public static final String QUEUE_PENDING_TABLE_DUMP_FILE_NAME = "pending_table_dump.csv";
	public static final String QUEUE_FAILED_TABLE_DUMP_FILE_NAME = "failed_table_dump.csv";
	
	public static final String GALLERY_TAG = "GALLERY_EXTERNAL_CAM_JCM";

	public static File getLogDir()
	{
		File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		dir = new File(dir, LOG_DIR_NAME);
		
		if (!dir.exists())
		{
			dir.mkdir();
		}
		
		return dir;
	}
	
	public static File getErrorReportingDir()
	{
		File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		dir = new File(dir, ERROR_REPORTING_DIR_NAME);
		
		if (!dir.exists())
		{
			dir.mkdir();
		}
		
		return dir;
	}
	
	public static File getErrorReportingFile()
	{
		File file = new File(getErrorReportingDir(), ERROR_REPORTING_FILE_NAME);
		
		return file;
	}
	
	/* External camera gallery cache functions */
	public static File getBadPicsDir()
	{
		File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		dir = new File(dir, GALLERY_BAD_PICS_DIR_NAME);
		
		if (!dir.exists())
		{
			Log.d(GALLERY_TAG, "BAD_PICS dir does not exist, creating.");
			dir.mkdir();
		}
		
		return dir;
	}
	
	/* Get human readable timestamp of the EXIF format. */
	private static long getGalleryTimestampFromExif(String exifDateTime, long offsetSeconds)
	{
		if (TextUtils.isEmpty(exifDateTime))
		{
			return 0;
		}
		
		String [] dateTimeArray = exifDateTime.split(" ");
		String [] dateArray = dateTimeArray[0].split(":");
		String [] timeArray = dateTimeArray[1].split(":");
		
		Time time = new Time();
		time.year = Integer.parseInt(dateArray[0]);
		time.month = Integer.parseInt(dateArray[1]) - 1;
		time.monthDay = Integer.parseInt(dateArray[2]);
		
		time.hour = Integer.parseInt(timeArray[0]);
		time.minute = Integer.parseInt(timeArray[1]);
		time.second = Integer.parseInt(timeArray[2]);
		
		time.set(time.toMillis(true) + offsetSeconds * 1000);
		
		return getGalleryTimestampFromTime(time, 0);
	}
	
	/* Get human readable timestamp of the current time. */
	private static long getGalleryTimestampNow()
	{
		long millis = System.currentTimeMillis();
		Time time = new Time();
		time.set(millis);
		
		return getGalleryTimestampFromTime(time, millis);
	}
	
	/* Get human readable timestamp of given time. Milliseconds is a separate variable because Time class does not
	 * allow storing milliseconds. */
	private static long getGalleryTimestampFromTime(Time time, long millis)
	{
		int year = time.year;
		int month = time.month + 1;
		int day = time.monthDay;
		int hour = time.hour;
		int minute = time.minute;
		int second = time.second;
		int hundreth = (int)( (millis/10)%100 );
		
		String yearString = "" + year;
		String monthString = "" + month;
		String dayString = "" + day;
		String hourString = "" + hour;
		String minuteString = "" + minute;
		String secondString = "" + second;
		String hundrethString = "" + hundreth;
		
		if (monthString.length()<2)
			monthString = "0" + monthString;
		if (dayString.length()<2)
			dayString = "0" + dayString;
		if (hourString.length()<2)
			hourString = "0" + hourString;
		if (minuteString.length()<2)
			minuteString = "0" + minuteString;
		if (secondString.length()<2)
			secondString = "0" + secondString;
		if (hundrethString.length()<2)
			hundrethString = "0" + hundrethString;

		String timestamp = yearString + monthString + dayString + hourString + minuteString + secondString + hundrethString;
		
		Log.d(GALLERY_TAG, "getGalleryTimestampNow(); returning: " + timestamp);
		
		return Long.parseLong(timestamp);
	}
	
	
	/* Return a file where timestamp ranges are stored. */
	private static File getGalleryTimestampsFile()
	{
		File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		dir = new File(dir, GALLERY_TIMESTAMPS_DIR_NAME);
		
		if (!dir.exists())
		{
			Log.d(GALLERY_TAG, "Timestamps file does not exist, creating");
			dir.mkdir();
		}
		
		return new File(dir, GALLERY_TIMESTAMPS_FILE_NAME);
	}
	
	public static class GalleryTimestampRange
	{
		public long rangeStart;
		public long profileID;
		public String escapedSKU;
	};
	
	/* If this is not null it means the gallery file was read successfully and is backed up in the memory. */
	public static ArrayList<GalleryTimestampRange> sGalleryTimestampRangesArray;
	
	private static void reloadGalleryTimestampRangesArray()
	{
		Log.d(GALLERY_TAG, "reloadGalleryTimestampRangesArray(); Entered the function.");

		sGalleryTimestampRangesArray = new ArrayList<GalleryTimestampRange>();
		
		File galleryFile = getGalleryTimestampsFile();
		
		if (!galleryFile.exists())
		{
			try {
				galleryFile.createNewFile();
			} catch (IOException e) {
				Log.d(GALLERY_TAG, "Unable to create gallery file.");
				Log.logCaughtException(e);
			}	
		}
		
		if (galleryFile.exists())
		{
			Log.d(GALLERY_TAG, "galleryFile exists. Proceeding.");
			
			try {
				FileReader fileReader = new FileReader(galleryFile);
				LineNumberReader lineNumberReader = new LineNumberReader(fileReader);
				String line, lastLine = null;
				
				while((line = lineNumberReader.readLine())!=null)
				{
					if (line.length() > 0)
					{
						Log.d(GALLERY_TAG, "Parsing line: " + line);
						
						String [] splittedLine = line.split(" ");
						
						if (splittedLine.length != 3)
							continue;
						
						GalleryTimestampRange newRange = new GalleryTimestampRange();
						newRange.escapedSKU = splittedLine[0];
						
						try
						{
							newRange.profileID = Long.parseLong(splittedLine[1]);
							newRange.rangeStart = Long.parseLong(splittedLine[2]);
						}
						catch (NumberFormatException nfe)
						{
							Log.logCaughtException(nfe);
							continue;
						}

						sGalleryTimestampRangesArray.add(newRange);

					}
				}

				fileReader.close();
			} catch (FileNotFoundException e) {
				Log.logCaughtException(e);
			} catch (IOException e) {
				Log.logCaughtException(e);
			}

		}
		else
		{
			sGalleryTimestampRangesArray = null;
			Log.d(GALLERY_TAG, "galleryFile does not exist and we couldn't create it.");
		}
	}
	
	public static ArrayList<GalleryTimestampRange> getGalleryTimestampRangesArray()
	{
		if (sGalleryTimestampRangesArray == null)
		{
			reloadGalleryTimestampRangesArray();
		}
		
		if (sGalleryTimestampRangesArray == null)
		{
			Log.d(GALLERY_TAG, "saveRangeStart(); Unable to load gallery timestamp ranges array.");
		}

		return sGalleryTimestampRangesArray;
	}
	
	/* Save the beginning of a timestamp range in the timestamps file. Return true on success. */
	public static boolean saveRangeStart(String sku, long profileID)
	{
	synchronized (sSynchronizationObject) {
		Log.d(GALLERY_TAG, "saveRangeStart(); Entered the function.");

		if (sGalleryTimestampRangesArray == null)
		{
			reloadGalleryTimestampRangesArray();
		}
		
		if (sGalleryTimestampRangesArray == null)
		{
			Log.d(GALLERY_TAG, "saveRangeStart(); Unable to load gallery timestamp ranges array.");
			return false;
		}
		
		String escapedSKU;
		try {
			escapedSKU = URLEncoder.encode(sku, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			Log.d(GALLERY_TAG, "saveRangeStart(); Cannot encode sku.");
			return false;
		}
		
		if (
			!(
				sGalleryTimestampRangesArray.size() > 0 &&
				sGalleryTimestampRangesArray.get(sGalleryTimestampRangesArray.size() - 1).escapedSKU.equals(escapedSKU) &&
				sGalleryTimestampRangesArray.get(sGalleryTimestampRangesArray.size() - 1).profileID == profileID
			)
		)
		{
			long timestamp = getGalleryTimestampNow();
			File galleryFile = getGalleryTimestampsFile();

			try {
				FileWriter fileWriter = null;
				fileWriter = new FileWriter(galleryFile, true);

				fileWriter.write(escapedSKU + " " + profileID + " " + timestamp + "\n");
				fileWriter.close();
			
				GalleryTimestampRange newRange = new GalleryTimestampRange();
				newRange.escapedSKU = escapedSKU;
				newRange.profileID = profileID;
				newRange.rangeStart = timestamp;
			
				sGalleryTimestampRangesArray.add(newRange);
			} catch (IOException e) {
				Log.d(GALLERY_TAG, "saveRangeStart(); Writing to file failed.");
				Log.logCaughtException(e);
				return false;
			}
		}
		
		return true;
	}
	}
	
	/* Get SKU and profile ID separated with a space. */
	public static String getSkuProfileIDForExifTimeStamp(Context c, String exifTimestamp)
	{
	synchronized (sSynchronizationObject) {
		Log.d(GALLERY_TAG, "getSkuProfileIDForExifTimeStamp(); Entered the function.");
		Settings settings = new Settings(c);
		
		if (sGalleryTimestampRangesArray == null)
		{
			reloadGalleryTimestampRangesArray();
		}
		
		if (sGalleryTimestampRangesArray == null)
		{
			Log.d(GALLERY_TAG, "getSkuProfileIDForExifTimeStamp(); Unable to load gallery timestamp ranges array.");
			return null;
		}
		
		long timestamp = getGalleryTimestampFromExif(exifTimestamp, settings.getCameraTimeDifference());
		
		for (int i = sGalleryTimestampRangesArray.size()-1; i >=0; i--)
		{
			if (sGalleryTimestampRangesArray.get(i).rangeStart <= timestamp)
			{
				Log.d(GALLERY_TAG, "getSkuProfileIDForExifTimeStamp(); Found match. Returning: " +
						sGalleryTimestampRangesArray.get(i).escapedSKU + " " + sGalleryTimestampRangesArray.get(i).profileID);
				
				return sGalleryTimestampRangesArray.get(i).escapedSKU + " " + sGalleryTimestampRangesArray.get(i).profileID;
			}
		}
		
		Log.d(GALLERY_TAG, "getSkuProfileIDForExifTimeStamp(); No match found. Returning null.");
		
		return null;
	}
	}
	
	/* Returns true on success. */
	private static boolean serialize(Object o, File file) {
		FileOutputStream fos;
		ObjectOutputStream oos;
		try {
			fos = new FileOutputStream(file);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(o);
			oos.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	/* Returns something else than null on success */
	private static Object deserialize(File file) {
		Object out;
		FileInputStream fis;
		ObjectInputStream ois;

		try {
			fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);
			out = ois.readObject();
			ois.close();
		} catch (Exception e) {
			return null;
		}
		return out;
	}

	/* Return a unique hash for a given SKU. */
	public static String encodeSKU(String SKU) {
		return Base64Coder_magento.encodeString(SKU).replace("+", "_").replace("/", "-").replace("=", "");
	}
	
	public static String encodeURL(String url) {
		return Base64Coder_magento.encodeString(url).replace("+", "_").replace("/", "-").replace("=", "");
	}
	
	/* Get a directory name for a given job type. */
	private static String getCachedJobSubdirName(int resourceType) {
		switch (resourceType) {
		case MageventoryConstants.RES_UPLOAD_IMAGE:
			return "UPLOAD_IMAGE";
		case MageventoryConstants.RES_CATALOG_PRODUCT_SELL:
			return "SELL";
		case MageventoryConstants.RES_ORDER_SHIPMENT_CREATE:
			return "SHIPMENT";

		default:
			return null;
		}
	}

	/* Get a filename for a given job type (job is extracted from jobID). */
	private static String getCachedResourceFileName(JobID jobID) {
		switch (jobID.getJobType()) {
		case MageventoryConstants.RES_UPLOAD_IMAGE:
		case MageventoryConstants.RES_CATALOG_PRODUCT_SELL:
		case MageventoryConstants.RES_ORDER_SHIPMENT_CREATE:
			return jobID.getTimeStamp() + ".obj";

		case MageventoryConstants.RES_CATALOG_PRODUCT_CREATE:
			return "new_prod.obj";
		case MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE:
			return "edit_prod.obj";
		case MageventoryConstants.RES_CATALOG_PRODUCT_SUBMIT_TO_TM:
			return "submit_to_tm.obj";

		default:
			return null;
		}
	}

	/* Return a directory where a given job resides. */
	private static File getDirectoryAssociatedWithJob(JobID jobID, boolean createDirectories) {
		File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		
		dir = new File(dir, encodeURL(jobID.getUrl()));
		dir = new File(dir, encodeSKU(jobID.getSKU()));

		String subdir = getCachedJobSubdirName(jobID.getJobType());

		if (subdir != null) {
			dir = new File(dir, subdir);
		}

		if (createDirectories == true) {
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					return null;
				}
			}
		}

		return dir;
	}

	/*
	 * Return a file associated with a given job. It can be used for example to
	 * serialize a job in the right place.
	 */
	private static File getFileAssociatedWithJob(JobID jobID, boolean createDirectories) {
		File fileToSave = new File(getDirectoryAssociatedWithJob(jobID, createDirectories),
				getCachedResourceFileName(jobID));
		return fileToSave;
	}

	/* Return a file path associated with a given job. */
	public static String getFilePathAssociatedWithJob(JobID jobID) {
		synchronized (sSynchronizationObject) {
			return getFileAssociatedWithJob(jobID, false).getAbsolutePath();
		}
	}
	
	public static void moveSKUdir(String url, String SKUfrom, String SKUto)
	{
		synchronized (sSynchronizationObject) {
			
			File dirFrom = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
			dirFrom = new File(dirFrom, encodeURL(url));
			dirFrom = new File(dirFrom, encodeSKU(SKUfrom));
			
			File dirTo = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
			dirTo = new File(dirTo, encodeURL(url));
			dirTo = new File(dirTo, encodeSKU(SKUto));

			if (dirTo.exists())
			{
				/* If the directory associated with the new sku already exists, this means that
				 * product details from product update call was already saved there and it is
				 * the newest product details info we have. Let's save it and continute. */
				File newProdDetFile = getProductDetailsFile(SKUto, url, false);
				File oldProdDetFile = getProductDetailsFile(SKUfrom, url, false);
				if (newProdDetFile.exists())
				{
					if (oldProdDetFile.exists())
					{
						oldProdDetFile.delete();
					}
					
					newProdDetFile.renameTo(oldProdDetFile);
				}
				
				deleteRecursive(dirTo);
			}
			
			dirFrom.renameTo(dirTo);
		}	
	}
	
	/* Save job in the cache.
	 * 
	 * There is a problem with storing a job in the cache. If multiple pieces of code were trying to do that without
	 * any coordination the state of such job could cause problems difficult to debug. This is why all code in the
	 * application should be following rules:
	 * 
	 * 1. The service and the queue have the absolute priority of restoring and storing jobs from/in the cache. No
	 * other code should interfere with them. When a job starts it is deserialized by the queue and passed to the service.
	 * The service is then allowed to store the job any number of times it wants (for example it's doing that every
	 * time upload progress changes). When the job is finished the job is either deleted or modified and stored
	 * back in the cache. When these things are happening no other code should store the job that is being processed.
	 * There is a way of checking whether a job is being processed: The JobQueue provides getCurrentJob() function
	 * which returns the job that the queue deserialized and passed to the service for processing. In order to store a
	 * job in the queue the interested code should first lock the sQueueSynchronizationObject which prevents the
	 * current job changes and then check the current job. If the current job is equal to the job which the calling code
	 * wants to store then it should either do it later or not at all.
	 * 
	 * 2. All pieces of code that want to restore, then modify, then store a job in the cache should lock the
	 * JobCacheManager.sSynchronizationObject before doing that to prevent a conflict with other pieces of code.
	 * */
	public static boolean store(Job job) {
		synchronized (sSynchronizationObject) {
			File fileToSave = getFileAssociatedWithJob(job.getJobID(), true);

			if (fileToSave != null && serialize(job, fileToSave) == true)
				return true;
			else
				return false;
		}
	}
	
	/* Store a job in a directory that doesn't necessarily correspond to the sku found in the job id of the job object passed. */
	public static boolean store(Job job, String SKU) {
		synchronized (sSynchronizationObject) {
			/* Create fake job id just to get the file associated with job. */
			JobID jobID = new JobID(job.getJobID().getTimeStamp(), job.getJobID().getProductID(), job.getJobType(), SKU, job.getUrl());
			
			File fileToSave = getFileAssociatedWithJob(jobID, true);
			
			if (fileToSave != null && serialize(job, fileToSave) == true)
				return true;
			else
				return false;
		}
	}

	/* Load job from the cache. */
	public static Job restore(JobID jobID) {
		synchronized (sSynchronizationObject) {
			File fileToRead = getFileAssociatedWithJob(jobID, false);

			if (fileToRead == null)
				return null;
			else
				return (Job) deserialize(fileToRead);
		}
	}

	/* Remove job from cache. */
	public static void removeFromCache(JobID jobID) {
		synchronized (sSynchronizationObject) {
			File fileToRemove = getFileAssociatedWithJob(jobID, false);

			if (fileToRemove != null) {
				fileToRemove.delete();
			}
		}
	}

	public static File getImageUploadDirectory(String SKU, String url) {
		synchronized (sSynchronizationObject) {
			return getDirectoryAssociatedWithJob(new JobID(-1, MageventoryConstants.RES_UPLOAD_IMAGE, SKU, url), true);
		}
	}

	public static File getSellDirectory(String SKU, String url) {
		synchronized (sSynchronizationObject) {
			return getDirectoryAssociatedWithJob(new JobID(-1, MageventoryConstants.RES_CATALOG_PRODUCT_SELL, SKU, url),
					true);
		}
	}
	
	public static File getShipmentDirectory(String SKU, String url) {
		synchronized (sSynchronizationObject) {
			return getDirectoryAssociatedWithJob(new JobID(-1, MageventoryConstants.RES_ORDER_SHIPMENT_CREATE, SKU, url),
					true);
		}
	}

	/* Load all upload jobs for a given SKU. */
	public static List<Job> restoreImageUploadJobs(String SKU, String url) {
		synchronized (sSynchronizationObject) {
			File uploadDir = getImageUploadDirectory(SKU, url);
			List<Job> out = new ArrayList<Job>();

			if (uploadDir == null)
				return out;

			File[] jobFileList = uploadDir.listFiles();

			if (jobFileList != null) {
				for (int i = 0; i < jobFileList.length; i++) {
					Job job = (Job) deserialize(jobFileList[i]);
					if (job != null)
						out.add(job);
				}
			}

			return out;
		}
	}

	/* Load all sell jobs for a given SKU. */
	public static List<Job> restoreSellJobs(String SKU, String url) {
		synchronized (sSynchronizationObject) {
			File sellDir = getSellDirectory(SKU, url);
			List<Job> out = new ArrayList<Job>();

			if (sellDir == null)
				return out;

			File[] jobFileList = sellDir.listFiles();

			if (jobFileList != null) {
				for (int i = 0; i < jobFileList.length; i++) {
					Job job = (Job) deserialize(jobFileList[i]);
					if (job != null)
						out.add(job);
				}
			}

			return out;
		}
	}
	
	/* Load all shipment jobs for a given SKU. */
	public static List<Job> restoreShipmentJobs(String SKU, String url) {
		synchronized (sSynchronizationObject) {
			File shipmentDir = getShipmentDirectory(SKU, url);
			List<Job> out = new ArrayList<Job>();

			if (shipmentDir == null)
				return out;

			File[] jobFileList = shipmentDir.listFiles();

			if (jobFileList != null) {
				for (int i = 0; i < jobFileList.length; i++) {
					Job job = (Job) deserialize(jobFileList[i]);
					if (job != null)
						out.add(job);
				}
			}

			return out;
		}
	}
	
	/* Load edit job for a given SKU. */
	public static Job restoreEditJob(String SKU, String url) {
		synchronized (sSynchronizationObject) {
			File file = getFileAssociatedWithJob(new JobID(-1, MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE, SKU, url),
					false);
			Job job = null;

			if (file.exists()) {
				job = (Job) deserialize(file);
			}

			return job;
		}
	}

	/* Load product creation job for a given SKU. */
	public static Job restoreProductCreationJob(String SKU, String url) {
		synchronized (sSynchronizationObject) {
			File file = getFileAssociatedWithJob(new JobID(-1, MageventoryConstants.RES_CATALOG_PRODUCT_CREATE, SKU, url),
					false);
			Job job = null;

			if (file.exists()) {
				job = (Job) deserialize(file);
			}

			return job;
		}
	}
	
	/* Load "submit to TM" job for a given SKU. */
	public static Job restoreSubmitToTMJob(String SKU, String url) {
		synchronized (sSynchronizationObject) {
			File file = getFileAssociatedWithJob(new JobID(-1, MageventoryConstants.RES_CATALOG_PRODUCT_SUBMIT_TO_TM, SKU, url),
					false);
			Job job = null;

			if (file.exists()) {
				job = (Job) deserialize(file);
			}

			return job;
		}
	}

	/* ======================================================================== */
	/* Queue database dump download */
	/* ======================================================================== */
	
	public static File getQueueDatabaseDumpDirectory(boolean createIfNotExists) {
		synchronized (sSynchronizationObject) {
			File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
			dir = new File(dir, QUEUE_DATABASE_DUMP_DIR_NAME);

			if (createIfNotExists && !dir.exists()) {
				dir.mkdirs();
			}

			return dir;
		}
	}
	
	/* Pass null as "dir" parameter to use the default directory */
	public static File getQueuePendingTableDumpFile(File dir) {
		synchronized (sSynchronizationObject) {
			File file;
			
			if (dir == null)
			{
				file = new File(getQueueDatabaseDumpDirectory(true), QUEUE_PENDING_TABLE_DUMP_FILE_NAME);	
			}
			else
			{
				file = new File(dir, QUEUE_PENDING_TABLE_DUMP_FILE_NAME);
			}
			
			return file;
		}
	}
	
	/* Pass null as "dir" parameter to use the default directory */
	public static File getQueueFailedTableDumpFile(File dir) {
		synchronized (sSynchronizationObject) {
			File file;
			
			if (dir == null)
			{
				file = new File(getQueueDatabaseDumpDirectory(true), QUEUE_FAILED_TABLE_DUMP_FILE_NAME);
			}
			else
			{
				file = new File(dir, QUEUE_FAILED_TABLE_DUMP_FILE_NAME);
			}
			
			return file;
		}
	}
	
	/* ======================================================================== */
	/* Image download */
	/* ======================================================================== */

	public static File getImageFullPreviewDirectory(String SKU, String url, boolean createIfNotExists) {
		synchronized (sSynchronizationObject) {
			File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
			dir = new File(dir, encodeURL(url));
			dir = new File(dir, encodeSKU(SKU));
			dir = new File(dir, DOWNLOAD_IMAGE_PREVIEW_DIR_NAME);

			if (createIfNotExists && !dir.exists()) {
				dir.mkdirs();
			}

			return dir;
		}
	}

	public static void clearImageFullPreviewDirectory(String SKU, String url) {
		synchronized (sSynchronizationObject) {
			File dir = getImageFullPreviewDirectory(SKU, url, false);

			if (dir.exists()) {
				for (File child : dir.listFiles()) {
					child.delete();
				}

				dir.delete();
			}
		}
	}

	public static File getImageDownloadDirectory(String SKU, String url, boolean createIfNotExists) {
		synchronized (sSynchronizationObject) {
			File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
			dir = new File(dir, encodeURL(url));
			dir = new File(dir, encodeSKU(SKU));
			dir = new File(dir, DOWNLOAD_IMAGE_DIR);

			if (createIfNotExists && !dir.exists()) {
				dir.mkdirs();
			}

			return dir;
		}
	}

	public static void clearImageDownloadDirectory(String SKU, String url) {
		synchronized (sSynchronizationObject) {
			File dir = getImageDownloadDirectory(SKU, url, false);

			if (dir.exists()) {
				for (File child : dir.listFiles()) {
					child.delete();
				}

				dir.delete();
			}
		}
	}

	/* ======================================================================== */
	/* Product details data */
	/* ======================================================================== */

	private static File getProductDetailsFile(String SKU, String url, boolean createDirectories) {
		File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		file = new File(file, encodeURL(url));
		file = new File(file, encodeSKU(SKU));

		if (createDirectories == true) {
			if (!file.exists()) {
				file.mkdirs();
			}
		}

		return new File(file, PRODUCT_DETAILS_FILE_NAME);
	}

	/* Store product detail in the cache but merge it with currently pending product edit job if any. */
	public static void storeProductDetailsWithMerge(Product product, String url) {
		synchronized (JobQueue.sQueueSynchronizationObject) {
		synchronized (sSynchronizationObject) {
			if (product == null || product.getSku() == null) {
				return;
			}
			
			/* A reference pointing to a product which is going to be serialized at the end. */
			Product mergedProduct = product;
			
			Job existingEditJob = JobCacheManager.restoreEditJob(product.getSku(), url);
			
			/* Check if an edit job exists in the pending table. */
			if (existingEditJob != null && existingEditJob.getPending() == true)
			{
				/* Product edit job exists we will do either one way or two way merge. */
				
				Product prodBeforeChanges = product.getCopy();
				
				Job currentJob = JobQueue.getCurrentJob();

				/* If this is true after next checks it means we will do the merge both ways (from product details
				 * to edit job and the other way around as well). If this will be false it means we just merge product
				 * edit job to product details. */
				boolean twoWayMerge = true;
				
				/* If the currently pending job is an edit job and if it has the same SKU that the product passed to this
				 * 	function we don't merge the product to the product edit job but we still do it the other way around. */
				if (currentJob!=null && currentJob.getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE &&
						currentJob.getSKU().equals(product.getSku()))
				{
					twoWayMerge = false;
				}
				
				List<String> updatedKeys = (List<String>) existingEditJob.getExtraInfo(MageventoryConstants.EKEY_UPDATED_KEYS_LIST);
				
				for (String key : existingEditJob.getExtras().keySet())
				{
					/* This is a special key that is not sent to the server. We shouldn't merge the value of this key. */
					if (key.equals(MageventoryConstants.EKEY_UPDATED_KEYS_LIST))
						continue;
					
					if (updatedKeys.contains(key))
					{
						/* In case of keys that were updated by the user we copy the values from the edit job file to product details file. */
						
						/* We send a different key to the server than the one we get from the server in case of categories. */
						if (key.equals(MageventoryConstants.MAGEKEY_PRODUCT_CATEGORIES))
						{
							mergedProduct.getData().put(MageventoryConstants.MAGEKEY_PRODUCT_CATEGORY_IDS, existingEditJob.getExtraInfo(key));
						}
						else
						{
							mergedProduct.getData().put(key, existingEditJob.getExtraInfo(key));
						}
					}
					else
					{
						/* In case of keys that were not updated by the user we merge the values from the product details file to
						 * edit job file but only in case the product edit request is not being processed while we're doing that. */
						
						if (twoWayMerge)
						{
							/* We send a different key to the server than the one we get from the server in case of categories. */
							if (key.equals(MageventoryConstants.MAGEKEY_PRODUCT_CATEGORIES))
							{
								existingEditJob.putExtraInfo(key, mergedProduct.getData().get(MageventoryConstants.MAGEKEY_PRODUCT_CATEGORY_IDS));	
							}
							else
							{
								existingEditJob.putExtraInfo(key, mergedProduct.getData().get(key));	
							}
						}
					}
				}

				/* Store the merged edit job if we were in two way merge mode. */
				if (twoWayMerge)
				{
					store(existingEditJob);	
				}
				
				/* Reinitialize all fields in Product class with the new data from the map. */
				mergedProduct = new Product(mergedProduct.getData());
				
				/* Remember the copy of original product to easily roll back later on. */
				mergedProduct.setUnmergedProduct(prodBeforeChanges);
			}
			else
			{
				/* Edit job doesn't exist in the pending table. We don't do any merge. */
			}
			
			serialize(mergedProduct, getProductDetailsFile(product.getSku(), url, true));
		}
		}
	}
	
	public static void storeProductDetails(Product product, String url) {
		synchronized (sSynchronizationObject) {
			if (product == null || product.getSku() == null) {
				return;
			}
			
			serialize(product, getProductDetailsFile(product.getSku(), url, true));
		}
	}
	
	/* Product details data can be merged with product edit job in two cases:
	 *  - when product details is being saved to the cache
	 *  - when edit job starts or finishes
	 *  
	 *  This function is handling the second case.
	 */
	public static void remergeProductDetailsWithEditJob(String sku, String url)
	{
	synchronized (sSynchronizationObject) {

		Product product = restoreProductDetails(sku, url);
		
		if (product != null)
		{
			/* We want to merge with the unmerged version of the product details.
			 * If getUnmergedProduct() returns null it means no merge took place on this
			 * instance of product details. */
			if (product.getUnmergedProduct() != null)
			{
				JobCacheManager.storeProductDetailsWithMerge(product.getUnmergedProduct(), url);
			}
			else
			{
				JobCacheManager.storeProductDetailsWithMerge(product, url);
			}
		}
	}
	}

	public static Product restoreProductDetails(String SKU, String url) {
		synchronized (sSynchronizationObject) {
			return (Product) deserialize(getProductDetailsFile(SKU, url, false));
		}
	}

	public static void removeProductDetails(String SKU, String url) {
		synchronized (sSynchronizationObject) {
			File f = getProductDetailsFile(SKU, url, false);

			if (f.exists()) {
				f.delete();
			}
		}
	}

	public static boolean productDetailsExist(String SKU, String url) {
		return getProductDetailsFile(SKU, url, false).exists();
	}

	/* ======================================================================== */
	/* Order list data */
	/* ======================================================================== */

	private static File getOrderListDir(boolean createIfNotExists, String url) {
		File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		dir = new File(dir, encodeURL(url));
		dir = new File(dir, ORDER_LIST_DIR_NAME);
		if (createIfNotExists && !dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}
	
	private static File getOrderListFile(boolean createDirectories, String[] params, String url) {
		File file = getOrderListDir(createDirectories, url);

		StringBuilder fileName = new StringBuilder();

		fileName.append("order_list_");

		if (params.length >= 1 && params[0] != null) {
			fileName.append(params[0]);
		}

		fileName.append(".obj");

		return new File(file, fileName.toString());
	}

	public static void storeOrderList(Map<String, Object> orderList, String[] params, String url) {
		synchronized (sSynchronizationObject) {
			if (orderList == null) {
				return;
			}
			serialize(orderList, getOrderListFile(true, params, url));
		}
	}

	public static Map<String, Object> restoreOrderList(String[] params, String url) {
		synchronized (sSynchronizationObject) {
			return (Map<String, Object>) deserialize(getOrderListFile(false, params, url));
		}
	}

	public static void removeOrderList(String url) {
		synchronized (sSynchronizationObject) {
			File dir = getOrderListDir(false, url);

			if (dir.exists()) {
				for (File child : dir.listFiles()) {
					child.delete();
				}

				dir.delete();
			}
		}
	}

	public static boolean orderListExist(String[] params, String url) {
		return getOrderListFile(false, params, url).exists();
	}
	
	/* ======================================================================== */
	/* Order carriers data */
	/* ======================================================================== */

	private static File getOrderCarriersFile(boolean createDirectories, String url) {
		File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		file = new File(file, encodeURL(url));

		if (createDirectories == true) {
			if (!file.exists()) {
				file.mkdirs();
			}
		}
		
		return new File(file, ORDER_CARRIERS_FILE_NAME);
	}

	public static void storeOrderCarriers(CarriersList carriers, String url) {
		synchronized (sSynchronizationObject) {
			if (carriers == null) {
				return;
			}
			serialize(carriers, getOrderCarriersFile(true, url));
		}
	}

	public static CarriersList restoreOrderCarriers(String url) {
		synchronized (sSynchronizationObject) {
			return (CarriersList) deserialize(getOrderCarriersFile(false, url));
		}
	}

	public static void removeOrderCarriers(String url) {
		synchronized (sSynchronizationObject) {
			File f = getOrderCarriersFile(false, url);

			if (f.exists()) {
				f.delete();
			}
		}
	}

	public static boolean orderCarriersExist(String url) {
		return getOrderCarriersFile(false, url).exists();
	}
	
	/* ======================================================================== */
	/* Order details data */
	/* ======================================================================== */
	private static File getOrderDetailsDir(boolean createIfNotExists, String url) {
		File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		dir = new File(dir, encodeURL(url));
		dir = new File(dir, ORDER_DETAILS_DIR_NAME);
		if (createIfNotExists && !dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	private static File getOrderDetailsFile(boolean createDirectories, String[] params, String url) {
		File file = getOrderDetailsDir(createDirectories, url);

		StringBuilder fileName = new StringBuilder();

		fileName.append("order_details_");

		if (params.length >= 1 && params[0] != null) {
			fileName.append(params[0]);
		}

		fileName.append(".obj");

		return new File(file, fileName.toString());
	}

	/* Params are in a form: {orderIncrementId}. */
	public static void storeOrderDetails(Map<String, Object> orderDetails, String[] params, String url) {
		synchronized (sSynchronizationObject) {
			if (orderDetails == null) {
				return;
			}
			serialize(orderDetails, getOrderDetailsFile(true, params, url));
		}
	}

	public static Map<String, Object> restoreOrderDetails(String[] params, String url) {
		synchronized (sSynchronizationObject) {
			return (Map<String, Object>) deserialize(getOrderDetailsFile(false, params, url));
		}
	}

	public static void removeOrderDetails(String url) {
		synchronized (sSynchronizationObject) {
			File dir = getOrderDetailsDir(false, url);

			if (dir.exists()) {
				for (File child : dir.listFiles()) {
					child.delete();
				}

				dir.delete();
			}
		}
	}

	public static boolean orderDetailsExist(String[] params, String url) {
		return getOrderDetailsFile(false, params, url).exists();
	}
	
	/* ======================================================================== */
	/* Product list data */
	/* ======================================================================== */

	private static File getProductListDir(boolean createIfNotExists, String url) {
		File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		dir = new File(dir, encodeURL(url));
		dir = new File(dir, PRODUCT_LIST_DIR_NAME);

		if (createIfNotExists && !dir.exists()) {
			dir.mkdirs();
		}

		return dir;
	}

	private static File getProductListFile(boolean createDirectories, String[] params, String url) {
		File file = getProductListDir(createDirectories, url);

		StringBuilder fileName = new StringBuilder();

		fileName.append("product_list_");

		if (params.length >= 1 && params[0] != null) {
			fileName.append( Base64Coder_magento.encodeString(params[0]).replace("+", "_").replace("/", "-").replace("=", "") );
		}

		fileName.append("_");

		if (params.length >= 2 && params[1] != null
				&& (Integer.parseInt(params[1]) != MageventoryConstants.INVALID_CATEGORY_ID)) {
			fileName.append(params[1]);
		}

		fileName.append(".obj");

		return new File(file, fileName.toString());
	}

	/* Params are in a form: {nameFilter, categoryId}. */
	public static void storeProductList(List<Map<String, Object>> productList, String[] params, String url) {
		synchronized (sSynchronizationObject) {
			if (productList == null) {
				return;
			}
			serialize(productList, getProductListFile(true, params, url));
		}
	}

	public static List<Map<String, Object>> restoreProductList(String[] params, String url) {
		synchronized (sSynchronizationObject) {
			return (List<Map<String, Object>>) deserialize(getProductListFile(false, params, url));
		}
	}

	public static void removeAllProductLists(String url) {
		synchronized (sSynchronizationObject) {
			File dir = getProductListDir(false, url);

			if (dir.exists()) {
				for (File child : dir.listFiles()) {
					child.delete();
				}

				dir.delete();
			}
		}
	}

	public static boolean productListExist(String[] params, String url) {
		return getProductListFile(false, params, url).exists();
	}

	/* ======================================================================== */
	/* Categories data */
	/* ======================================================================== */

	private static File getCategoriesFile(boolean createDirectories, String url) {
		File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		file = new File(file, encodeURL(url));

		if (createDirectories == true) {
			if (!file.exists()) {
				file.mkdirs();
			}
		}
		
		return new File(file, CATEGORIES_LIST_FILE_NAME);
	}

	public static void storeCategories(Map<String, Object> attributes, String url) {
		synchronized (sSynchronizationObject) {
			if (attributes == null) {
				return;
			}
			serialize(attributes, getCategoriesFile(true, url));
		}
	}

	public static Map<String, Object> restoreCategories(String url) {
		synchronized (sSynchronizationObject) {
			return (Map<String, Object>) deserialize(getCategoriesFile(false, url));
		}
	}

	public static void removeCategories(String url) {
		synchronized (sSynchronizationObject) {
			File f = getCategoriesFile(false, url);

			if (f.exists()) {
				f.delete();
			}
		}
	}

	public static boolean categoriesExist(String url) {
		return getCategoriesFile(false, url).exists();
	}

	/* ======================================================================== */
	/* Statistics data */
	/* ======================================================================== */

	private static File getStatisticsFile(boolean createDirectories, String url) {
		File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		file = new File(file, encodeURL(url));

		if (createDirectories == true) {
			if (!file.exists()) {
				file.mkdirs();
			}
		}
		
		return new File(file, STATISTICS_FILE_NAME);
	}

	public static void storeStatistics(Map<String, Object> statistics, String url) {
		synchronized (sSynchronizationObject) {
			if (statistics == null) {
				return;
			}
			serialize(statistics, getStatisticsFile(true, url));
		}
	}

	public static Map<String, Object> restoreStatistics(String url) {
		synchronized (sSynchronizationObject) {
			return (Map<String, Object>) deserialize(getStatisticsFile(false, url));
		}
	}

	public static void removeStatistics(String url) {
		synchronized (sSynchronizationObject) {
			File f = getStatisticsFile(false, url);

			if (f.exists()) {
				f.delete();
			}
		}
	}

	public static boolean statisticsExist(String url) {
		return getStatisticsFile(false, url).exists();
	}
	
	/* ======================================================================== */
	/* Profiles list data */
	/* ======================================================================== */

	private static File getProfilesListFile(boolean createDirectories, String url) {
		File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		file = new File(file, encodeURL(url));

		if (createDirectories == true) {
			if (!file.exists()) {
				file.mkdirs();
			}
		}
		
		return new File(file, PROFILES_FILE_NAME);
	}

	public static void storeProfilesList(Object[] profilesList, String url) {
		synchronized (sSynchronizationObject) {
			if (profilesList == null) {
				return;
			}
			serialize(profilesList, getProfilesListFile(true, url));
		}
	}

	public static Object[] restoreProfilesList(String url) {
		synchronized (sSynchronizationObject) {
			return (Object[]) deserialize(getProfilesListFile(false, url));
		}
	}

	public static void removeProfilesList(String url) {
		synchronized (sSynchronizationObject) {
			File f = getProfilesListFile(false, url);

			if (f.exists()) {
				f.delete();
			}
		}
	}

	public static boolean profilesListExist(String url) {
		return getProfilesListFile(false, url).exists();
	}
	
	/* ======================================================================== */
	/* Profile execution */
	/* ======================================================================== */
	private static File getProfileExecutionDir(boolean createIfNotExists, String url) {
		File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		dir = new File(dir, encodeURL(url));
		dir = new File(dir, PROFILE_EXECUTION_DIR_NAME);
		if (createIfNotExists && !dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	private static File getProfileExecutionFile(boolean createDirectories, String[] params, String url) {
		File file = getProfileExecutionDir(createDirectories, url);

		StringBuilder fileName = new StringBuilder();

		fileName.append("profile_execution_");

		if (params.length >= 1 && params[0] != null) {
			fileName.append(params[0]);
		}

		fileName.append(".obj");

		return new File(file, fileName.toString());
	}

	/* Params are in a form: {profileID}. */
	public static void storeProfileExecution(String profileExecutionMessage, String[] params, String url) {
		synchronized (sSynchronizationObject) {
			if (profileExecutionMessage == null) {
				return;
			}
			serialize(profileExecutionMessage, getProfileExecutionFile(true, params, url));
		}
	}

	public static String restoreProfileExecution(String[] params, String url) {
		synchronized (sSynchronizationObject) {
			return (String) deserialize(getProfileExecutionFile(false, params, url));
		}
	}

	public static void removeProfileExecution(String url) {
		synchronized (sSynchronizationObject) {
			File dir = getProfileExecutionDir(false, url);

			if (dir.exists()) {
				for (File child : dir.listFiles()) {
					child.delete();
				}

				dir.delete();
			}
		}
	}

	public static boolean profileExecutionExists(String[] params, String url) {
		return getProfileExecutionFile(false, params, url).exists();
	}
	
	/* ======================================================================== */
	/* Custom attribute set data */
	/* ======================================================================== */

	private static File getAttributeSetsFile(boolean createDirectories, String url) {
		File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		file = new File(file, encodeURL(url));

		if (createDirectories == true) {
			if (!file.exists()) {
				file.mkdirs();
			}
		}

		return new File(file, ATTRIBUTE_SETS_FILE_NAME);
	}

	public static void storeAttributeSets(List<Map<String, Object>> attributeSets, String url) {
		synchronized (sSynchronizationObject) {
			if (attributeSets == null) {
				return;
			}
			serialize(attributeSets, getAttributeSetsFile(true, url));
		}
	}

	/*
	 * Helper function providing means of updating an attribute map inside of a
	 * list of attribute maps.
	 */
	private static void updateAttributeInTheAttributeList(List<Map<String, Object>> attribList,
			Map<String, Object> attribute) {
		if (attribList != null) {
			int i = 0;
			for (Map<String, Object> elem : attribList) {
				String codeFromCache = (String) elem
						.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_CODE_ATTRIBUTE_LIST_REQUEST);
				String codeToUpdate = (String) attribute
						.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_CODE_ATTRIBUTE_LIST_REQUEST);

				if (TextUtils.equals(codeFromCache, codeToUpdate)) {
					attribList.set(i, attribute);
					break;
				}
				i++;
			}
		}
	}

	/*
	 * When adding an option to a custom attribute through an application we are
	 * getting back the whole attribute which we might want to save in the cache
	 * without downloading the entire list. This function provides the code that
	 * updates just one attribute in the cache.
	 */
	public static void updateSingleAttributeInTheCache(Map<String, Object> attribute, String setID, String url) {
		synchronized (sSynchronizationObject) {
			
			List<Map<String, Object>> attrsList = restoreAttributeList(setID, url);

			if (attrsList != null) {
				updateAttributeInTheAttributeList(attrsList, attribute);

				storeAttributeList(attrsList, setID, url);
			}
		}
	}

	public static List<Map<String, Object>> restoreAttributeSets(String url) {
		synchronized (sSynchronizationObject) {
			return (List<Map<String, Object>>) deserialize(getAttributeSetsFile(false, url));
		}
	}

	public static void removeAttributeSets(String url) {
		synchronized (sSynchronizationObject) {
			File f = getAttributeSetsFile(false, url);

			if (f.exists()) {
				f.delete();
			}
		}
	}

	public static boolean attributeSetsExist(String url) {
		return getAttributeSetsFile(false, url).exists();
	}

	/* ======================================================================== */
	/* Custom attribute list data */
	/* ======================================================================== */
	private static File getAttributeListDir(boolean createIfNotExists, String url) {
		File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		dir = new File(dir, encodeURL(url));
		dir = new File(dir, ATTRIBUTE_LIST_DIR_NAME);
		if (createIfNotExists && !dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	private static File getAttributeListFile(boolean createDirectories, String attributeSetID, String url) {
		File file = getAttributeListDir(createDirectories, url);

		StringBuilder fileName = new StringBuilder();

		fileName.append("attribute_list_");
		fileName.append(attributeSetID);
		fileName.append(".obj");

		return new File(file, fileName.toString());
	}

	public static void storeAttributeList(List<Map<String, Object>> attributeList, String attributeSetID, String url) {
		synchronized (sSynchronizationObject) {
			if (attributeList == null) {
				return;
			}
			serialize(attributeList, getAttributeListFile(true, attributeSetID, url));
		}
	}

	public static List<Map<String, Object>> restoreAttributeList(String attributeSetID, String url) {
		synchronized (sSynchronizationObject) {
			return (List<Map<String, Object>>) deserialize(getAttributeListFile(false, attributeSetID, url));
		}
	}

	public static void removeAttributeList(String url) {
		synchronized (sSynchronizationObject) {
			File dir = getAttributeListDir(false, url);

			if (dir.exists()) {
				for (File child : dir.listFiles()) {
					child.delete();
				}

				dir.delete();
			}
		}
	}

	public static boolean attributeListExist(String attributeSetID, String url) {
		return getAttributeListFile(false, attributeSetID, url).exists();
	}
	
	/* ======================================================================== */
	/* Last used custom attributes data */
	/* ======================================================================== */
	private static File getLastUsedCustomAttribsFile(boolean createDirectories, String url) {
		File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		file = new File(file, encodeURL(url));

		if (createDirectories == true) {
			if (!file.exists()) {
				file.mkdirs();
			}
		}
		
		return new File(file, LAST_USED_ATTRIBUTES_FILE_NAME);
	}

	public static void storeLastUsedCustomAttribs(CustomAttributesList list, String url) {
		synchronized (sSynchronizationObject) {
			if (list == null) {
				return;
			}
			serialize(list, getLastUsedCustomAttribsFile(true, url));
		}
	}

	public static CustomAttributesList restoreLastUsedCustomAttribs(String url) {
		synchronized (sSynchronizationObject) {
			return (CustomAttributesList) deserialize(getLastUsedCustomAttribsFile(false, url));
		}
	}

	/* ======================================================================== */
	/* Input cache */
	/* ======================================================================== */
	
	private static File getInputCacheFile(boolean createDirectories, String url) {
		File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		file = new File(file, encodeURL(url));
		
		if (createDirectories == true) {
			if (!file.exists()) {
				file.mkdirs();
			}
		}
		
		return new File(file, INPUT_CACHE_FILE_NAME);
	}

	public static void storeInputCache(Map<String, List<String>> inputCache, String url) {
		synchronized (sSynchronizationObject) {
			if (inputCache == null) {
				return;
			}
			serialize(inputCache, getInputCacheFile(true, url));
		}
	}

	public static Map<String, List<String>> loadInputCache(String url) {
		synchronized (sSynchronizationObject) {
			return (Map<String, List<String>>) deserialize(getInputCacheFile(false, url));
		}
	}
	
	/* ======================================================================== */
	/* Deleting cache */
	/* ======================================================================== */
	private static void deleteCacheFiles(File dirOrFile)
	{
		if (dirOrFile.getName().equals(PRODUCT_LIST_DIR_NAME) ||
			dirOrFile.getName().equals(DOWNLOAD_IMAGE_PREVIEW_DIR_NAME) ||
			dirOrFile.getName().equals(DOWNLOAD_IMAGE_DIR) ||
			dirOrFile.getName().equals(ORDER_DETAILS_DIR_NAME) ||
			dirOrFile.getName().equals(ORDER_LIST_DIR_NAME) ||
			dirOrFile.getName().equals(ATTRIBUTE_LIST_DIR_NAME) ||
			dirOrFile.getName().equals(PROFILE_EXECUTION_DIR_NAME)
			)
		{
			deleteRecursive(dirOrFile);
		}
		else
		if (dirOrFile.getName().equals(PRODUCT_DETAILS_FILE_NAME) ||
			dirOrFile.getName().equals(ATTRIBUTE_SETS_FILE_NAME) ||
			dirOrFile.getName().equals(CATEGORIES_LIST_FILE_NAME) ||
			dirOrFile.getName().equals(INPUT_CACHE_FILE_NAME) ||
			dirOrFile.getName().equals(LAST_USED_ATTRIBUTES_FILE_NAME) ||
			dirOrFile.getName().equals(STATISTICS_FILE_NAME) ||
			dirOrFile.getName().equals(ORDER_CARRIERS_FILE_NAME) ||
			dirOrFile.getName().equals(PROFILES_FILE_NAME)
			)
		{
			dirOrFile.delete();
		}
		else
		if (dirOrFile.isDirectory())
		{
			for (File child : dirOrFile.listFiles())
				deleteCacheFiles(child);	
		}
	}
	
	public static void deleteCache(String url)
	{
		synchronized (sSynchronizationObject) {
			String encodedUrl = encodeSKU(url);
			
			File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
			file = new File(file, encodedUrl);

			if (file.exists())
				deleteCacheFiles(file);
		}
	}
	
	public static void deleteAllCaches()
	{
		synchronized (sSynchronizationObject) {
			File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);

			if (file.exists())
				deleteCacheFiles(file);
		}
	}
	
	/* If this returns false it means there is a job being executed so we cannot wipe the data. Otherwise
	 * it's a success. */
	public static boolean wipeData(Context context)
	{
		synchronized(JobQueue.sQueueSynchronizationObject)
		{
			if (JobQueue.getCurrentJob() == null)
			{
				new JobQueue(context).wipeTables();
				
				synchronized (sSynchronizationObject)
				{
					File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
					
					if (file.exists())
						deleteRecursive(file);
				}
			}
			else
			{
				return false;
			}
		}
		
		return true;
	}
	
	/* Delete all files recursively from a given directory. */
	public static void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				deleteRecursive(child);

		fileOrDirectory.delete();
	}
}
