package com.mageventory.resprocessor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient2;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.ResourceProcessorManager.IProcessor;

public class CatalogProductListProcessor implements IProcessor, MageventoryConstants {

	@Override
	public Bundle process(Context context, String[] params, Bundle extras) {
		boolean success = false;

		// get resource parameters
		String nameFilter = null;
		int categoryId = INVALID_CATEGORY_ID;
		if (params != null) {
			if (params.length >= 1 && params[0] instanceof String) {
				nameFilter = (String) params[0];
			}
			if (params.length >= 2 && TextUtils.isDigitsOnly(params[1])) {
				categoryId = Integer.parseInt(params[1]);
			}
		}

		// retrieve data
		final MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();
		if (client == null) {
			return null;
		}
		final List<Map<String, Object>> productList;

		productList = client.catalogProductList(nameFilter, categoryId);

		// store data
		if (productList != null) {
			JobCacheManager.storeProductList(productList, params);
		} else {
			throw new RuntimeException(client.getLastErrorMessage());
		}

		return null;
	}

}
