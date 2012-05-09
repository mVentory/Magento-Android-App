package com.mageventory.jobprocessor;

import java.util.HashMap;
import java.util.Map;

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

	ImageStreaming.StreamUploadCallback mCallback;
	
	public void setCallback(ImageStreaming.StreamUploadCallback callback)
	{
		mCallback = callback;
	}
	
	@Override
	public void process(Context context, Job job) {
		Map<String,Object> imageData = job.getExtras();
		String sku = (String) job.getExtraInfo(MAGEKEY_PRODUCT_SKU);
		
		final MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();

	    String fileName = client.uploadImage(imageData,sku,false,mCallback);        
	    if (fileName == null) 
	    {
	        throw new RuntimeException(client.getLastErrorMessage());
	    }
	    job.setServerResponse(fileName);
	}

}
