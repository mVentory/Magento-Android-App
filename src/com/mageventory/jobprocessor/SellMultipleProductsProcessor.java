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

public class SellMultipleProductsProcessor implements IProcessor, MageventoryConstants {

	@Override
	public void process(Context context, Job job) {
		Map<String, Object> requestData = job.getExtras();
		
		MagentoClient client;
		try {
			client = new MagentoClient(job.getSettingsSnapshot());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e.getMessage());
		}
		
		Map<String, Object> res = client.orderForMultipleProductsCreate((Object[])requestData.get(EKEY_PRODUCTS_TO_SELL_ARRAY));
		
		if (res == null) {
			throw new RuntimeException(client.getLastErrorMessage());
		}
		else
		{
			job.setResultData((String)res.get("increment_id"));
			JobCacheManager.storeOrderDetails(res, new String [] {(String)res.get("increment_id")}, job.getSettingsSnapshot().getUrl());
		}
	}
}
