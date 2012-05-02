package com.mageventory.components;

import java.io.File;
import java.io.FilenameFilter;

import com.mageventory.res.ImagesState;
import com.mageventory.res.ImagesStateContentProvider;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.provider.ContactsContract.CommonDataKinds.Im;



public class ImageCachingManager {

	public enum cachingState
	{
		cachingState_ALL_CACHED,				// All Data Are Cached
		cachingState_STILL_DOWNLOADING,			// Some Records are still downloading
		cachingState_NOT_CACHED					// Not Cached at all
	}

		
	private String productID;			// Product ID
	private String productPath;			// Product Path
	private File imagesDirectory;
	private String [] imagesList;
	private Context appContext; 
	private Cursor imagesRecords;
	ImagesStateContentProvider imagesStateProvider;
	
	/**
	 * @param productID
	 */
	public ImageCachingManager(String productID,Context appContext,ImagesStateContentProvider imagesStateProvider) {
		super();
		this.productID = productID;
		productPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MageventoryImages/" + this.productID;
		imagesDirectory = new File(productPath);
		this.appContext = appContext;
		this.imagesStateProvider = imagesStateProvider;
	}
	
	/**
	 * remove cached Data
	 */
	public void removeCachedData()
	{
		// remove Directory
		imagesDirectory.delete();
		
		// Delete all records from database 
		String selection = ImagesState.PRODUCT_ID + "=" + productID;
		imagesStateProvider.delete(selection, null);
		
	}
	
	
	/**
	 * Check if Images are Cached
	 * @return
	 */
	public cachingState getCachingState()
	{		
		// Check if directory exists and has images
		if(imagesDirectory.exists() && imagesDirectory.isDirectory())
		{
			imagesList = imagesDirectory.list(new JpgExtFilter());
			
			// Check The database - check if there is images are still downloading 
			String selection = ImagesState.PRODUCT_ID + "=" + productID;
			imagesRecords = imagesStateProvider.query(null,selection, null, null);

			// If there is no rows returned -- Fully Cached
			if(imagesRecords.getCount() == 0)
			{
				if(imagesList.length == 0)
					return cachingState.cachingState_NOT_CACHED;
			
				// Then All Images Are Cached
				return cachingState.cachingState_ALL_CACHED;
			}
			else
			{
				while(imagesRecords.moveToNext())
				{
					int imageState = imagesRecords.getInt(imagesRecords.getColumnIndex(ImagesState.STATE));
					if(imageState == ImagesState.STATE_DOWNLOAD)
					{
						// Image is Still Downloading
						imagesRecords.close();
						return cachingState.cachingState_STILL_DOWNLOADING;
					}
				}
				
				imagesRecords.close();
				
				// All Cached Delete From database
				imagesStateProvider.delete(selection, null);
				
				// if all finished and there is no downloading state, then all images are cached
				// load them from SD Card
				return cachingState.cachingState_ALL_CACHED;				
			}
			
			
		}
		else
		{
			// There is no directory -- not cached
			return cachingState.cachingState_NOT_CACHED;
		}
	}
	
	/**
	 * get Images List
	 * @return
	 */
	public String[] getImagesList()
	{
		return imagesDirectory.list();
	}
	
	public ImageMetaData[] getImagesData()
	{
		String selection = ImagesState.PRODUCT_ID + "=" + productID;
		imagesRecords = imagesStateProvider.query(null,selection, null, null);
			
		ImageMetaData[] metaData = new ImageMetaData[imagesRecords.getCount()];
		
		int index = 0;
		while (imagesRecords.moveToNext()) 
		{
			metaData[index] = new ImageMetaData();
			metaData[index].setName(imagesRecords.getString(imagesRecords.getColumnIndex(ImagesState.IMAGE_NAME)));
			metaData[index].setUrl(imagesRecords.getString(imagesRecords.getColumnIndex(ImagesState.IMAGE_URL)));
			metaData[index].setState(imagesRecords.getInt(imagesRecords.getColumnIndex(ImagesState.STATE)));
			index++;         			        
		}		 
		
		imagesRecords.close();
		
		
		return metaData;
	}
	
	
	/**
	 * Filter files to get images only
	 * @author hussein
	 *
	 */
	private class JpgExtFilter implements FilenameFilter
	{			
		@Override
		public boolean accept(File dir, String filename) {
			// TODO Auto-generated method stub
			return filename.endsWith(".jpg");
		}		
	}
	
	
	/**
	 * Image MetaData Used only for still downloading images
	 * @author hussein
	 *
	 */
	public class ImageMetaData
	{
		String name;
		String url;
		int state;
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
		/**
		 * @return the url
		 */
		public String getUrl() {
			return url;
		}
		/**
		 * @param url the url to set
		 */
		public void setUrl(String url) {
			this.url = url;
		}
		/**
		 * @return the state
		 */
		public int getState() {
			return state;
		}
		/**
		 * @param state the state to set
		 */
		public void setState(int state) {
			this.state = state;
		}
	}
}
