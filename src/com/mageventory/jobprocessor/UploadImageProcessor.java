package com.mageventory.jobprocessor;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.client.ImageStreaming;
import com.mageventory.client.MagentoClient2;
import com.mageventory.res.ResourceCache;
import com.mageventory.job.Job;
import com.mageventory.job.JobProcessorManager.IProcessor;
import com.mageventory.res.ResourceStateDao;

public class UploadImageProcessor implements IProcessor, MageventoryConstants {

	static public final String IMAGE_SERVER_MEDIA_PATH = "/media/catalog/product";
		
	ImageStreaming.StreamUploadCallback mCallback;
	
	public void setCallback(ImageStreaming.StreamUploadCallback callback)
	{
		mCallback = callback;
	}
	
	@Override
	public void process(Context context, Job job) {
		Map<String,Object> imageData = job.getExtras();
		String sku = (String) job.getExtraInfo(MAGEKEY_PRODUCT_SKU);
		boolean is_main = ((Boolean)job.getExtraInfo(MAGEKEY_PRODUCT_IMAGE_IS_MAIN)).booleanValue();
		
		final MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();

	    String fileName = client.uploadImage(imageData,sku,is_main,mCallback);        
	    if (fileName == null) 
	    {
	        throw new RuntimeException(client.getLastErrorMessage());
	    }
	    
	    /*ImagesStateContentProvider imageStatesProvider = new ImagesStateContentProvider(context);
	    
		ContentValues dbValues = new ContentValues();
		dbValues.put(ImagesState.PRODUCT_ID, new Integer((String) job.getExtraInfo(MAGEKEY_PRODUCT_ID)).intValue());
		dbValues.put(ImagesState.IMAGE_INDEX, -1);
		dbValues.put(ImagesState.STATE, ImagesState.STATE_NOT_CACHED);
		dbValues.put(ImagesState.IMAGE_NAME, (String) job.getExtraInfo(MAGEKEY_PRODUCT_IMAGE_NAME) + ".jpg");
		dbValues.put(ImagesState.IMAGE_URL,IMAGE_SERVER_PATH + fileName);
		dbValues.put(ImagesState.ERROR_MSG,"");
		dbValues.put(ImagesState.UPLOAD_PERCENTAGE,0);
		dbValues.put(ImagesState.IMAGE_PATH,"");
		
		imageStatesProvider.insert(dbValues);
	    */
	    job.setServerResponse(fileName);
	}

}
