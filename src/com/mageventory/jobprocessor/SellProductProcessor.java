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
		Map<String, Object> requestData = job.getExtras();

		/* Don't need this key here. It is just there to pass info about product creation mode selected by the user. */
		requestData.remove(EKEY_QUICKSELLMODE);
		
		MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();

		final Map<String, Object> productMap = client.orderCreate(job.getExtras());

		final Product product;
		if (productMap != null) {
			product = new Product(productMap);
		} else {
			throw new RuntimeException(client.getLastErrorMessage());
		}

		// cache
		if (product != null) {
			JobCacheManager.storeProductDetails(product);

			Boolean quickSellMode = ((Boolean)job.getExtraInfo(MageventoryConstants.EKEY_QUICKSELLMODE));
			
			/* If QUICKSELLMODE key is present in the job extra info this means that the user wants to create a product and
			 * sell it at the same time (we only set this key in case user creates a product and don't set it when user just
			 * sells a product). We want to make sure that the list of products gets refreshed after product creation next
			 * time user sees it so we remove all lists from the cache here. */
			if (quickSellMode != null)
			{
				JobCacheManager.removeAllProductLists();
			}
		}
	}
}
