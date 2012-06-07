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
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.res.ResourceStateDao;
import com.mageventory.res.ResourceCache;

public class CatalogProductListProcessor implements IProcessor,
		MageventoryConstants {

	@Override
	public Bundle process(Context context, String[] params, Bundle extras,
			String resourceUri, ResourceStateDao state, ResourceCache store) {
		boolean success = false;

		state.addResource(resourceUri);
		state.setState(resourceUri, STATE_BUILDING);

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
		final MagentoClient2 client = ((MyApplication) context
				.getApplicationContext()).getClient2();
		if (client == null) {
			return null;
		}
		state.setTransacting(resourceUri, true);
		final List<Map<String, Object>> productList;
		if (categoryId == INVALID_CATEGORY_ID) {
			productList = client.catalogProductList(nameFilter);
		} else {
			productList = client.catalogCategoryAssignedProducts(categoryId);
		}
		state.setTransacting(resourceUri, false);

		// store data
		if (productList != null) {
			try {
				store.store(context, resourceUri, productList);
				success = true;
			} catch (IOException ignored) {
			}
		}

		if (success) {
			state.setState(resourceUri, STATE_AVAILABLE);
			ResourceExpirationRegistry.getInstance()
					.productListChanged(context);
		} else {
			state.setState(resourceUri, STATE_NONE);
		}
		return null;
	}

}
