package com.mageventory.jobprocessor;

import static com.mageventory.res.ResourceStateDao.buildParameterizedUri;

import java.io.IOException;
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
import com.mageventory.model.Product;
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
		boolean is_main = ((Boolean)job.getExtraInfo(MAGEKEY_PRODUCT_IMAGE_IS_MAIN)).booleanValue();
		
		/* Very temporary and ugly way of finding out the uri */
		String[] params = new String[2];
		
		params[0] = GET_PRODUCT_BY_ID; // ZERO --> Use Product ID , ONE --> Use Product SKU 
		params[1] = "" + job.getJobID().getProductID();
		
		final String resourceUri = ResourceStateDao.buildParameterizedUri(RES_PRODUCT_DETAILS, params);
		final ResourceStateDao state = new ResourceStateDao(context);
		final ResourceCache cache = ResourceCache.getInstance();
		
		state.addResource(resourceUri);
		state.setState(resourceUri, STATE_BUILDING);
		
		final MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();

		state.setTransacting(resourceUri, true);
	    Map<String, Object> productMap = client.uploadImage(imageData, "" + job.getJobID().getProductID(),is_main,mCallback);
	    
	    final Product product;
		if (productMap != null) {
			product = new Product(productMap, true);
		}
		else
		{
			state.setState(resourceUri, STATE_NONE);
	        throw new RuntimeException(client.getLastErrorMessage());
	    }
		
		state.setTransacting(resourceUri, false);
		
		// cache
		if (product != null) {
			try {
				cache.store(context, resourceUri, product);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			state.setState(resourceUri, STATE_AVAILABLE);
		}
	}
}
