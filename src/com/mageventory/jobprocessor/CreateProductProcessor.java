package com.mageventory.jobprocessor;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.util.Base64;
import com.mageventory.util.Log;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.jobprocessor.JobProcessorManager.IProcessor;

public class CreateProductProcessor implements IProcessor, MageventoryConstants {

	private static final String TAG = "CreateProductProcessor";

	@Override
	public void process(Context context, Job job) {
		Map<String, Object> requestData = (Map<String, Object>)(((HashMap<String, Object>)(job.getExtras())).clone());

		// extract attribute data
		final int attrSet = ((Integer) requestData.get(EKEY_PRODUCT_ATTRIBUTE_SET_ID)).intValue();

		boolean duplicationMode = (Boolean)requestData.get(EKEY_DUPLICATIONMODE);
		String productSKUToDuplicate = (String)requestData.get(EKEY_PRODUCT_SKU_TO_DUPLICATE);
		String photoCopyMode = (String)requestData.get(EKEY_DUPLICATION_PHOTO_COPY_MODE);
		int decreaseOriginalQuantity = (Integer)requestData.get(EKEY_DECREASE_ORIGINAL_QTY);
				
		/*
		 * Don't need this in extras as we are passing it in a separate argument
		 * later.
		 */
		requestData.remove(EKEY_PRODUCT_ATTRIBUTE_SET_ID);
		
		/* Don't need this key here. It is just there to pass info about product creation mode selected by the user. */
		requestData.remove(EKEY_QUICKSELLMODE);
		requestData.remove(EKEY_DUPLICATIONMODE);
		requestData.remove(EKEY_PRODUCT_SKU_TO_DUPLICATE);
		requestData.remove(EKEY_DUPLICATION_PHOTO_COPY_MODE);
		requestData.remove(EKEY_DECREASE_ORIGINAL_QTY);

		if (attrSet == INVALID_ATTRIBUTE_SET_ID) {
			Log.w(TAG, "INVALID ATTRIBUTE SET ID");
			return;
		}

		MagentoClient client;
		try {
			client = new MagentoClient(job.getSettingsSnapshot());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e.getMessage());
		}

		String sku = (String) requestData.get(MAGEKEY_PRODUCT_SKU);
		Map<String, Object> productMap = null;
		
		if (duplicationMode)
		{
			productMap = client.catalogProductCreate(null, 0, sku, requestData, true, productSKUToDuplicate, photoCopyMode, decreaseOriginalQuantity);
		}
		else
		{
			productMap = client.catalogProductCreate("simple", attrSet, sku, requestData, false, null, null, 0);	
		}

		int pid = -1;

		Product product = null;

		if (productMap != null) {
			product = new Product(productMap);
			pid = Integer.parseInt(product.getId());
		}

		if (pid == -1) {
			throw new RuntimeException(client.getLastErrorMessage());
		} else {
			JobCacheManager.storeProductDetailsWithMerge(product, job.getJobID().getUrl());
			JobCacheManager.removeAllProductLists(job.getJobID().getUrl());
		}
	}

}
