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
import java.util.HashMap;


public class UpdateProductProcessor implements IProcessor, MageventoryConstants {

	@Override
	public void process(Context context, Job job) {

		Map<String, Object> requestData = (Map<String, Object>)(((HashMap<String, Object>)(job.getExtras())).clone());

		/* Don't need this key here. It is just here in case we need to merge product edit job file with product details file.
		 * We don't need to send that to the server. */
		requestData.remove(EKEY_UPDATED_KEYS_LIST);
		
		boolean success; 
		
		final MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();
		success = client.catalogProductUpdate(job.getJobID().getProductID(), job.getExtras());
		
		/* For now the edit call doesn't return product details so we just remove the original version of product details
		 * from the cache as the edit job was successful and we don't need the original version anymore.
		 * */
		if (success)
		{
			synchronized(JobCacheManager.sSynchronizationObject)
			{
				Product product = JobCacheManager.restoreProductDetails(job.getSKU());
				
				product.setUnmergedProduct(null);
				
				if (product != null)
				{
					JobCacheManager.storeProductDetails(product);	
				}
			}
		}
		else
		{
			throw new RuntimeException("unsuccessful update");
		}
	}
}
