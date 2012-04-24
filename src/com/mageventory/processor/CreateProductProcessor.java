package com.mageventory.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.client.MagentoClient2;
import com.mageventory.res.ResourceCache;
import com.mageventory.res.ResourceStateDao;

public class CreateProductProcessor extends AbsProductProcessor {

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

    private static final String TAG = "CreateProductProcessor";
    // @formatter:on

    private static String generateSku(final Map<String, Object> data, boolean alt) {
        String name = data.get(MAGEKEY_PRODUCT_NAME).toString();
        if (name == null) {
            Log.v(TAG, "product name is null");
            name = "";
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
     *  Extract Update Information 
     * 	Quantity and IS_IN_MARKET
     */
    private Map<String, Object> extractUpdate(Bundle bundle) throws IncompleteDataException {
        final String[] stringKeys = {
                MAGEKEY_PRODUCT_QUANTITY,
                MAGEKEY_PRODUCT_MANAGE_INVENTORY,
                MAGEKEY_PRODUCT_IS_IN_STOCK
        };
        // @formatter:on
        final Map<String, Object> productData = new HashMap<String, Object>();
        for (final String stringKey : stringKeys) {
            productData.put(stringKey, extractString(bundle, stringKey, true));
        }       
        return productData;
    }

    @Override
    public Bundle process(Context context, String[] params, Bundle extras, String parameterizedResourceUri,
            ResourceStateDao state, ResourceCache cache) {
        final Map<String, Object> productData = extractData(extras, true);
        
        // extract attribute data
        final int attrSet = extras.getInt(EKEY_PRODUCT_ATTRIBUTE_SET_ID, INVALID_ATTRIBUTE_SET_ID);
        @SuppressWarnings("unchecked")
        final Map<String, String> atrs = (Map<String, String>) extras.getSerializable(EKEY_PRODUCT_ATTRIBUTE_VALUES);
        
        if (atrs != null && atrs.isEmpty() == false) {
        	productData.putAll(atrs);
        }
        
        if (attrSet == INVALID_ATTRIBUTE_SET_ID) {
        	Log.w(TAG, "INVALID ATTRIBUTE SET ID");
        	return null;
        }

        // y: huss, i belive this solution is better
        productData.putAll(extractUpdate(extras));
        
        final MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();
        
        // Check if SKU is empty then generate SKU else use existing one
        String sku = extras.getString(MAGEKEY_PRODUCT_SKU);
        if(TextUtils.isEmpty(sku))
        {
        	// Empty Generate SKU
        	sku = generateSku(productData, false);
        }
		
        int pid = client.catalogProductCreate("simple", attrSet, sku, productData);
        if (pid == -1) {
            // issue #49 ( http://code.google.com/p/mageventory/issues/detail?id=49 )
            // says we should regenerate SKU and retry if it fails the first time
        	sku= generateSku(productData, true);
            pid = client.catalogProductCreate("simple", attrSet, sku, productData);
        }
        if (pid == -1) {
            throw new RuntimeException(client.getLastErrorMessage());
        }
        
        ResourceExpirationRegistry.getInstance().productCreated(context);
        
        final Bundle result = new Bundle();
        result.putInt(context.getString(R.string.ekey_product_id), pid);
        return result;
    }

}
