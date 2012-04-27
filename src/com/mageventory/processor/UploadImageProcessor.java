package com.mageventory.processor;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.client.MagentoClient2;
import com.mageventory.res.ResourceCache;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.res.ResourceStateDao;

public class UploadImageProcessor implements IProcessor, MageventoryConstants {

	
	 private static class IncompleteDataException extends RuntimeException {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unused")
        public IncompleteDataException() {
            super();
        }

        public IncompleteDataException(String detailMessage) {
            super(detailMessage);
        }

    }
	 
	private Map<String,Object> extractImageInfo(Bundle bundle) throws IncompleteDataException
	{
		// Implements Order Invoice Information
    	final String[] stringKeys = {
    			MAGEKEY_PRODUCT_IMAGE_NAME,
    			MAGEKEY_PRODUCT_IMAGE_CONTENT,
    			MAGEKEY_PRODUCT_IMAGE_MIME
        };
        
        final Map<String, Object> imageData = new HashMap<String, Object>();
        for (final String stringKey : stringKeys) {
        	imageData.put(stringKey, extractString(bundle, stringKey));
        }
        return imageData;		
	}
	
	private String extractString(final Bundle bundle, final String key) throws IncompleteDataException {
	        final String s = bundle.getString(key);
	        if (s == null) {
	            throw new IncompleteDataException("bad data for key '" + key + "'");
	        }
	        return s;
	    }
	
	private int extractInteger(final Bundle bundle, final String key) throws IncompleteDataException {
        final String s = bundle.getString(key);
        if (s == null) {
            throw new IncompleteDataException("bad data for key '" + key + "'");
        }
        return Integer.valueOf(s);
    }

	
	
	@Override
	public Bundle process(Context context, String[] params, Bundle extras,
			String parameterizedResourceUri, ResourceStateDao state,
			ResourceCache cache) {

			Map<String,Object> imageData = extractImageInfo(extras);
			String sku = extractString(extras, MAGEKEY_PRODUCT_SKU);
			int index = extractInteger(extras, MAGEKEY_PRODUCT_IMAGE_POSITION);
			final MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();
		    String fileName = client.uploadImage(imageData,sku,index); //orderCreate(customerInfo,null,productData);        
		    if (fileName == null) 
		    {
		        throw new RuntimeException(client.getLastErrorMessage());
		    }
		    
		    final Bundle result = new Bundle();      
		    result.putString(context.getString(R.string.ekey_image_file_name), fileName);
		    return result;

	}

}
