package com.mageventory.processor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.client.MagentoClient2;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceCache;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.res.ResourceStateDao;


/**
 * Class Implements the Order Invoice
 * @author hussein
 *
 */

public class CreateCartOrderProcessor implements IProcessor, MageventoryConstants {

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

    private static final String TAG = "CreateCartOrderProcessor";
    
    
    // @formatter:off
    private static final char CHARS[] = {
        // 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        // 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    	'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
    	'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    	// '+', '/',
    };
    private static Random random;
  
    // @formatter:on

    private static String generateSku(final Map<String, Object> data, boolean alt) {
        
    	String name = "";
    	if(!data.containsKey(MAGEKEY_PRODUCT_NAME))
    	{
    		name = "";
    	}
    	else
    	{
    		name = data.get(MAGEKEY_PRODUCT_NAME).toString();    	
	        if (name == null) {
	            Log.v(TAG, "product name is null");
	            name = "";
	        }
    	}
    	
        final StringBuilder sku = new StringBuilder();
        while (name.length() < 3) {
            name += randomChar();
        }
        System.out.println("name=" + name);
        System.out.println("base64name=" + new String(Base64.encode(name.getBytes(), Base64.NO_PADDING | Base64.NO_WRAP)));
        name = name.substring(name.length() - 3);
        name = new String(Base64.encode(name.getBytes(), Base64.NO_PADDING | Base64.NO_WRAP));
        name = name.toLowerCase();
        sku.append(System.currentTimeMillis());

        if (alt) {
            // insert 3 random characters to make it unique
            for (int i = 0; i < 3; i++) {
                sku.insert(getRandom().nextInt(sku.length()), randomChar());
            }
        }

        sku.append(name);
        return sku.toString();
    }

    private static Random getRandom() {
        if (random == null) {
            random = new Random();
        }
        return random;
    }

    private static char randomChar() {
        return CHARS[getRandom().nextInt(CHARS.length)];
    }

    

    /**
     * Extract Product Details
     * @param bundle
     * @return
     * @throws IncompleteDataException
     */
    private Map<String, Object> extractProductDetails(Bundle bundle) throws IncompleteDataException {
    	// Implements Order Invoice Information
    	final String[] stringKeys = {
    			MAGEKEY_PRODUCT_SKU,
                MAGEKEY_PRODUCT_QUANTITY,
                MAGEKEY_PRODUCT_PRICE,
                MAGEKEY_PRODUCT_DESCRIPTION
        };
        
        final Map<String, Object> productData = new HashMap<String, Object>();
        for (final String stringKey : stringKeys) {
            productData.put(stringKey, extractString(bundle, stringKey));
        }
        return productData;
    }
           
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
		
		Map<String,Object> productData = extractProductDetails(extras);
		
		if(TextUtils.isEmpty(productData.get(MAGEKEY_PRODUCT_SKU).toString()))
		{
			String sku = generateSku(productData,false);
			productData.remove(MAGEKEY_PRODUCT_SKU);
			productData.put(MAGEKEY_PRODUCT_SKU, sku);
		}
		
		// retrieve product
		state.setTransacting(parameterizedResourceUri, true);
		final Map<String, Object> productMap = client.orderCreate(productData);

		final Product product;
		if (productMap != null) {
			product = new Product(productMap, true);
		} else {
			state.setState(parameterizedResourceUri, STATE_NONE);
			throw new RuntimeException(client.getLastErrorMessage());
		}

		// get category id
		int mainCategoryId;
		try {
			mainCategoryId = Integer.parseInt(product.getMaincategory());
		} catch (Throwable e) {
			mainCategoryId = INVALID_CATEGORY_ID;
		}

		// retrieve and set category name
		if (mainCategoryId != INVALID_CATEGORY_ID) {
			final Map<String, Object> category = client.catalogCategoryInfo(mainCategoryId);
			if (category != null && category.containsKey(MAGEKEY_CATEGORY_NAME)) {
				product.setMaincategory_name(category.get(MAGEKEY_CATEGORY_NAME).toString());
			}
		}
		state.setTransacting(parameterizedResourceUri, false);

		// cache
		if (product != null) {
			try {
				cache.store(context, parameterizedResourceUri, product);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			state.setState(parameterizedResourceUri, STATE_AVAILABLE);
		}
		
		return null;                      
    }
}
