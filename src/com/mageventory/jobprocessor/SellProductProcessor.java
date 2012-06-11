package com.mageventory.jobprocessor;

import java.util.Map;

import android.content.Context;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient2;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.jobprocessor.JobProcessorManager.IProcessor;
import com.mageventory.model.Product;

public class SellProductProcessor implements IProcessor, MageventoryConstants {

	@Override
	public void process(Context context, Job job) {
		MagentoClient2 client = ((MyApplication) context
				.getApplicationContext()).getClient2();
		
		final Map<String, Object> productMap = client.orderCreate(job.getExtras());

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
}
