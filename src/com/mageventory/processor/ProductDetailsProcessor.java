package com.mageventory.processor;

import java.io.IOException;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient2;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceCache;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.res.ResourceStateDao;

public class ProductDetailsProcessor implements IProcessor, MageventoryConstants {

	@Override
	public Bundle process(Context context, String[] params, Bundle extras, String resourceUri, ResourceStateDao state,
			ResourceCache cache) {
		state.addResource(resourceUri);
		state.setState(resourceUri, STATE_BUILDING);

		MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();
		int productId = Integer.parseInt(params[0]);

		// retrieve product
		state.setTransacting(resourceUri, true);
		final Map<String, Object> productMap = client.catalogProductInfo(productId);

		final Product product;
		if (productMap != null) {
			product = new Product(productMap, true);
		} else {
			state.setState(resourceUri, STATE_NONE);
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

		return null;
	}

}
