package com.mageventory.job;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.os.Environment;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;

public class JobCacheManager {
	
	private static String getCachedResourceSubdirName(int resourceType)
	{
		switch (resourceType)
		{
		case MageventoryConstants.RES_UPLOAD_IMAGE:
			return "UPLOAD_IMAGE";

		default:
			return null;
		}
	}
	
	private static File getDirectoryAssociatedWithJob(JobID jobID, boolean createDirectories)
	{
		File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		dir = new File(dir, jobID.getSKU());
		String subdir = getCachedResourceSubdirName(jobID.getJobType());
		
		if (subdir != null)
		{
			dir = new File(dir, subdir);	
		}
		
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
		File fileToSave = new File(getDirectoryAssociatedWithJob(jobID, createDirectories), "" + jobID.getTimeStamp());
		return fileToSave;
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
	
	public static void removeFromCache(JobID jobID)
	{
		File fileToRemove = getFileAssociatedWithJob(jobID, false);
		
		if (fileToRemove !=null)
		{
			fileToRemove.delete();
		}
	}
	
	public static File getImageUploadDirectory(String SKU)
	{
		return getDirectoryAssociatedWithJob(new JobID(-1, MageventoryConstants.RES_UPLOAD_IMAGE, SKU), true);
	}
	
	public static File getImageDownloadDirectory(String SKU)
	{
		File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		dir = new File(dir, SKU);
		dir = new File(dir, "DOWNLOAD_IMAGE");	
		
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				return null;
			}
		}
		
		return dir;
	}
	
	public static List<Job> restoreImageUploadJobs(String SKU)
	{
		File uploadDir = getImageUploadDirectory(SKU);
		List<Job> out = new ArrayList<Job>();
		
		if (uploadDir == null)
			return out;
		
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
}
