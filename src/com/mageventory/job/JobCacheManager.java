package com.mageventory.job;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.os.Environment;

import com.mageventory.MageventoryConstants;

public class JobCacheManager {
	
    private static final String APP_DIR_NAME = "mageventory";
	
	private static String getCachedResourceSubdirName(int resourceType)
	{
		switch (resourceType)
		{
		case MageventoryConstants.RES_CATALOG_PRODUCT_LIST:
			return "CATALOG_PRODUCT_LIST";
		case MageventoryConstants.RES_PRODUCT_DETAILS:
			return "PRODUCT_DETAILS";
		case MageventoryConstants.RES_CATALOG_CATEGORY_TREE:
			return "CATALOG_CATEGORY_TREE";
		case MageventoryConstants.RES_CATALOG_PRODUCT_CREATE:
			return "CATALOG_PRODUCT_CREATE";
		case MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE:
			return "CATALOG_PRODUCT_UPDATE";
		case MageventoryConstants.RES_CART_ORDER_CREATE:
			return "CART_ORDER_CREATE";
		case MageventoryConstants.RES_FIND_PRODUCT:
			return "FIND_PRODUCT";
		case MageventoryConstants.RES_CATALOG_PRODUCT_ATTRIBUTE_SET_LIST:
			return "CATALOG_PRODUCT_ATTRIBUTE_SET_LIST";
		case MageventoryConstants.RES_PRODUCT_ATTRIBUTE_LIST:
			return "PRODUCT_ATTRIBUTE_LIST";
		case MageventoryConstants.RES_CATEGORY_ATTRIBUTE_LIST:
			return "CATEGORY_ATTRIBUTE_LIST";
		case MageventoryConstants.RES_UPLOAD_IMAGE:
			return "UPLOAD_IMAGE";
		case MageventoryConstants.RES_PRODUCT_DELETE:
			return "PRODUCT_DELETE";
		default:
			return "OTHER";
		}
	}
	
	private static File getDirectoryAssociatedWithJob(JobID jobID, boolean createDirectories)
	{
		File dir = new File(Environment.getExternalStorageDirectory(), APP_DIR_NAME);
		dir = new File(dir, getCachedResourceSubdirName(jobID.getResourceType()));
		
		if (jobID.getResourceType() == MageventoryConstants.RES_UPLOAD_IMAGE)
		{
			dir = new File(dir, (String) jobID.getExtraInfo(MageventoryConstants.MAGEKEY_PRODUCT_ID));
		}
		
//		dir = JobCleanupManager.appendCleanupSpecificSubdir(dir);
		
		if (createDirectories == true)
		{
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					return null;
				}
			}
		}
		
		return dir;
	}
	
	private static File getFileAssociatedWithJob(JobID jobID, boolean createDirectories)
	{
		File fileToSave = new File(getDirectoryAssociatedWithJob(jobID, createDirectories), jobID.toString());
		return fileToSave;
	}
	
	private static File getImageUploadDirectory(int productID)
	{
		File dir = new File(Environment.getExternalStorageDirectory(), APP_DIR_NAME);
		dir = new File(dir, getCachedResourceSubdirName(MageventoryConstants.RES_UPLOAD_IMAGE));
		dir = new File(dir, "" + productID);
//		dir = JobCleanupManager.appendCleanupSpecificSubdir(dir);
		return dir;
	}
	
	public static String getFilePathAssociatedWithJob(JobID jobID)
	{
		return getFileAssociatedWithJob(jobID, false).getAbsolutePath();
	}
	
	public static boolean store(Job job)
	{
		File fileToSave = getFileAssociatedWithJob(job.getJobID(), true);
		
		if (fileToSave != null && job.serialize(fileToSave) == true)
			return true;
		else
			return false;
	}
	
	public static Job restore(JobID jobID)
	{
		File fileToRead = getFileAssociatedWithJob(jobID, false);
		
		if (fileToRead == null)
			return null;
		else
			return Job.deserialize(fileToRead);
	}
	
	public static List<Job> restoreImageUploadJobs(int productID)
	{
		File uploadDir = getImageUploadDirectory(productID);
		List<Job> out = new ArrayList<Job>();
		
		File[] jobFileList = uploadDir.listFiles();
		
		if (jobFileList != null)
		{
			for (int i=0; i<jobFileList.length; i++)
			{
				Job job = Job.deserialize(jobFileList[i]);
				if (job != null)
					out.add(job);
			}
		}
		
		return out;
	}
	
	public static void removeFromCache(JobID jobID)
	{
		File fileToRemove = getFileAssociatedWithJob(jobID, false);
		
		if (fileToRemove !=null)
		{
			fileToRemove.delete();
		}
	}

}
