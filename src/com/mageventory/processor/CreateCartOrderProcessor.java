package com.mageventory.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.client.MagentoClient2;
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

    /**
     * Extract Product Details
     * @param bundle
     * @return
     * @throws IncompleteDataException
     */
    private Map<String, Object> extractProductDetails(Bundle bundle) throws IncompleteDataException {
    	// Implements Order Invoice Information
    	final String[] stringKeys = {
    			MAGEKEY_PRODUCT_ID,
                MAGEKEY_PRODUCT_QUANTITY,
                MAGEKEY_PRODUCT_PRICE
        };
        
        final Map<String, Object> productData = new HashMap<String, Object>();
        for (final String stringKey : stringKeys) {
            productData.put(stringKey, extractString(bundle, stringKey));
        }
        return productData;
    }
    
    /**
     * extractCustomerInformation
     * @param bundle
     * @return
     * @throws IncompleteDataException
     */
    private Map<String, Object> extractCustomerInformation(Bundle bundle) throws IncompleteDataException {
    	// Implements Order Invoice Information
    	final String[] stringKeys = {
    			MAGEKEY_CUSTOMER_INFO_FIRST_NAME,
    			MAGEKEY_CUSTOMER_INFO_LAST_NAME,
    			MAGEKEY_CUSTOMER_INFO_EMAIL,
    			MAGEKEY_CUSTOMER_INFO_STORE_ID,
    			MAGEKEY_CUSTOMER_INFO_WEBSITE_ID,
    			MAGEKEY_CUSTOMER_INFO_MODE
        };
        
        final Map<String, Object> productData = new HashMap<String, Object>();
        for (final String stringKey : stringKeys) {
        	productData.put(stringKey, extractString(bundle, stringKey));
        }
        return productData;
    }

    /**
     * extractCustomerAddress
     * @param bundle
     * @return
     * @throws IncompleteDataException
     */
    private Map<String, Object> extractCustomerAddress(Bundle bundle) throws IncompleteDataException {
    	// Implements Order Invoice Information
    	final String[] stringKeys = {
    			MAGEKEY_CUSTOMER_ADDRESS_MODE,
    			MAGEKEY_CUSTOMER_INFO_FIRST_NAME,
    			MAGEKEY_CUSTOMER_INFO_LAST_NAME,
    			MAGEKEY_CUSTOMER_ADDRESS_COMPANY,
    			MAGEKEY_CUSTOMER_ADDRESS_STREET,
    			MAGEKEY_CUSTOMER_ADDRESS_CITY,
    			MAGEKEY_CUSTOMER_ADDRESS_REGION,
    			MAGEKEY_CUSTOMER_ADDRESS_POSTCODE,
    			MAGEKEY_CUSTOMER_ADDRESS_COUNTRY_ID,
    			MAGEKEY_CUSTOMER_ADDRESS_TELEPHONE,
    			MAGEKEY_CUSTOMER_ADDRESS_FAX,
    			MAGEKEY_CUSTOMER_ADDRESS_IS_DEFAULT_SHIPPING,
    			MAGEKEY_CUSTOMER_ADDRESS_IS_DEFAULT_BILLING
        };
        
        final Map<String, Object> productData = new HashMap<String, Object>();
        for (final String stringKey : stringKeys) {
            String value = extractString(bundle, stringKey);
        	if(stringKey.compareTo(MAGEKEY_CUSTOMER_ADDRESS_MODE) == 0)
            {
            	value = "billing";
            	productData.put("mode", value);
            }
        	else
        	{
        		productData.put(stringKey, value);
        	}
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
        
    	final Map<String, Object> productData = extractProductDetails(extras);
        final String newQty = extractString(extras,NEW_QUANTITY);
        final boolean updateQty = extras.getBoolean(UPDATE_PRODUCT_QUANTITY);
        
        final MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();

        int invoiceID = client.orderCreate(productData, newQty,updateQty); //orderCreate(customerInfo,null,productData);        
        if (invoiceID == -1) 
        {
            throw new RuntimeException(client.getLastErrorMessage());
        }
        
        final Bundle result = new Bundle();      
        result.putInt(context.getString(R.string.ekey_order_id), invoiceID);
        return result;
    }
}
