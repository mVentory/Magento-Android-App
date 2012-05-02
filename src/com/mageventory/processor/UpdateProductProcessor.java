package com.mageventory.processor;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient2;
import com.mageventory.res.ResourceCache;
import com.mageventory.res.ResourceStateDao;

public class UpdateProductProcessor extends AbsProductProcessor {

	@Override
    public Bundle process(Context context, String[] params, Bundle extras, String parameterizedResourceUri,
            ResourceStateDao state, ResourceCache cache) {
		final int productId;
		try {
			productId = Integer.parseInt(params[0]);
		} catch (Throwable e) {
			throw new RuntimeException("invalid product id");
		}
		
		final Map<String, Object> productData = extractData(extras, false);
		productData.put("tax_class_id",0);
		
		// productData.put(MAGEKEY_ATTRIBUTE_SET_ID, extras.getString(EKEY_PRODUCT_ATTRIBUTE_SET_ID)); // y?
		@SuppressWarnings("unchecked")
        final HashMap<String, Object> atrs = (HashMap<String, Object>) extras.get(EKEY_PRODUCT_ATTRIBUTE_VALUES);
		productData.putAll(atrs);
		
		final MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();
		if (client.catalogProductUpdate(productId, productData) == false) {
			throw new RuntimeException("unsuccessful update");
		}
		ResourceExpirationRegistry.getInstance().productUpdated(context, productId);
	    return null;
    }

}
