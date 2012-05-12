package com.mageventory.components;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.provider.ContactsContract.CommonDataKinds.Im;

public class ImageCachingManager {
	
	/* This map stores product ids as keys and list of file paths as values. */
	private static Map<String, List<String>> sNumberOfPendingDownloads = new HashMap<String, List<String>>();
	
	public static Object sSynchronisationObject = new Object();

	public static void addDownload(int product, String filePath)
	{
	synchronized(sSynchronisationObject)
	{
		List<String> list = sNumberOfPendingDownloads.get(""+product);
		
		if (list == null)
		{
			list = new ArrayList<String>();
			sNumberOfPendingDownloads.put(""+product, list);
		}
		
		list.add(filePath);
	}
	}
	
	public static void removeDownload(int product, String filePath)
	{
	synchronized(sSynchronisationObject)
	{
		List<String> list = sNumberOfPendingDownloads.get(""+product);
		
		if (list == null)
			return;
		
		list.remove(filePath);
		
		if (list.size() == 0)
		{
			sNumberOfPendingDownloads.remove(""+product);
		}
	}
	}
	
	public static int getPendingDownloadCount(int product)
	{
	synchronized(sSynchronisationObject)
	{
		List<String> downloadsList = sNumberOfPendingDownloads.get(""+product);
		
		if ( downloadsList != null )
		{
			return downloadsList.size();
		}
		else
		{
			return 0;
		}
	}
	}
	
	public static List<String> getPendingDownloads(int product)
	{
	synchronized(sSynchronisationObject)
	{
		return sNumberOfPendingDownloads.get(""+product);
	}
	}
	
	public static boolean isDownloadPending(int product, String path)
	{
	synchronized(sSynchronisationObject)
	{
		List<String> downloadsList = sNumberOfPendingDownloads.get(""+product);
		
		if ( downloadsList != null )
		{
			return downloadsList.contains(path);
		}
		else
		{
			return false;
		}		
	}
	}
	
	/*public static void deleteRecursive(File fileOrDirectory, boolean deleteTop) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	        	deleteRecursive(child, true);

	    if (deleteTop)
	    	fileOrDirectory.delete();
	}*/
	
    public static void removeCachedData(int productID)
    {
        File imagesDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MageventoryImages/" + productID);
        
        if (imagesDirectory.exists())
        {
        	for (File child : imagesDirectory.listFiles(new JpgExtFilter()))
        	{
        		child.delete();
        	}
        }
    }
    
    public static boolean cacheDirectoryExists(int productID)
    {
    	File imagesDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MageventoryImages/" + productID);
    	return imagesDirectory.exists();
    }
	
	/**
	 * get Images List
	 * @return
	 */
	public static String[] getImagesList(int productID)
	{
        File imagesDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MageventoryImages/" + productID);
		return imagesDirectory.list(new JpgExtFilter());
	}
	
	/**
	 * Filter files to get images only
	 * @author hussein
	 *
	 */
	private static class JpgExtFilter implements FilenameFilter
	{			
		@Override
		public boolean accept(File dir, String filename) {
			// TODO Auto-generated method stub
			return filename.endsWith(".jpg");
		}		
	}
}
