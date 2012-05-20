package com.mageventory.resprocessor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient2;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceCache;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.res.ResourceStateDao;

public class ProductDetailsProcessor implements IProcessor, MageventoryConstants {

	@Override
	public Bundle process(Context context, String[] params, Bundle extras, String resourceUri, ResourceStateDao state,
			ResourceCache cache) {
		MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();
		
		String useIDorSKU = params[0];
		
		if(useIDorSKU.compareToIgnoreCase(GET_PRODUCT_BY_ID) == 0)
		{
			int productId = Integer.parseInt(params[1]);

			// retrieve product
			final Map<String, Object> productMap = client.catalogProductInfo(productId);
	
			final Product product;
			if (productMap != null) {
				product = new Product(productMap, true);
			} else {
				throw new RuntimeException(client.getLastErrorMessage());
			}
	
			// cache
			if (product != null) {
				JobCacheManager.storeProductDetails(product);
			}				
		}
		else if(useIDorSKU.compareToIgnoreCase(GET_PRODUCT_BY_SKU) == 0)
		{
			String productSKU = params[1];

			// retrieve product
			final Map<String, Object> productMap = client.catalogProductInfoBySKU(productSKU);
	
			final Product product;
			if (productMap != null) {
				product = new Product(productMap, true);
			} else {
				product = new Product();
			}
	
			// cache
			if (product != null) {
				JobCacheManager.storeProductDetails(product);
			}							
		}
				
		return null;
	}

}
