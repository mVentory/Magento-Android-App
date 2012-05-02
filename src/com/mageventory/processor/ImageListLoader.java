package com.mageventory.processor;

import java.util.HashMap;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.client.MagentoClient2;
import com.mageventory.res.ResourceCache;
import com.mageventory.res.ResourceStateDao;
import com.mageventory.res.ResourceProcessorManager.IProcessor;


/**
 * Processor Class Responsible of Loading Images List From Site
 * @author hussein
 *
 */
public class ImageListLoader implements IProcessor, MageventoryConstants {

	
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

    

    private static final String TAG = "ImageListLoader";

    private String extractString(final Bundle bundle, final String key) throws IncompleteDataException {
        final String s = bundle.getString(key);
        if (s == null) {
            throw new IncompleteDataException("bad data for key '" + key + "'");
        }
        return s;
    }

    
    /**
     *  Process 
     *  extras has all information of order
     */
    @Override
    public Bundle process(Context context, String[] params, Bundle extras, String parameterizedResourceUri,
            ResourceStateDao state, ResourceCache cache) {
        
		state.addResource(parameterizedResourceUri);
		state.setState(parameterizedResourceUri, STATE_BUILDING);

		MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();
		
		String productID = extractString(extras, MAGEKEY_PRODUCT_ID);
		
		// retrieve product
		state.setTransacting(parameterizedResourceUri, true);
		Object [] imagesList = (Object []) client.getImagesList(productID);
		
		@SuppressWarnings("unchecked")
		HashMap<String, Object>[] imgMap = (HashMap<String, Object>[]) imagesList;
		
		state.setTransacting(parameterizedResourceUri, false);
		state.setState(parameterizedResourceUri, STATE_AVAILABLE);
		
		final Bundle result = new Bundle();
        result.putSerializable(IMAGES_LIST,imgMap);
        return result;                      
    }
}
