package com.mageventory.jobprocessor;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.jobprocessor.JobProcessorManager.IProcessor;
import com.mageventory.model.Product;

public class AddProductToCartProcessor implements IProcessor, MageventoryConstants {

	@Override
	public void process(Context context, Job job) {
		Map<String, Object> requestData = job.getExtras();
		
		MagentoClient client;
		try {
			client = new MagentoClient(job.getSettingsSnapshot());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e.getMessage());
		}
		
		Boolean res = client.addToCart(requestData);
		
		if (res == null) {
			JobCacheManager.removeCartItem((String)requestData.get(MAGEKEY_PRODUCT_TRANSACTION_ID), job.getSettingsSnapshot().getUrl());
			throw new RuntimeException(client.getLastErrorMessage());
		}
		else
		{
			JobCacheManager.addCartItem(requestData, job.getSettingsSnapshot().getUrl());
		}
	}
}
