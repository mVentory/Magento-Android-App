package com.mageventory.resprocessor;

import java.net.MalformedURLException;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.xmlrpc.XMLRPCException;
import com.mageventory.xmlrpc.XMLRPCFault;

public class StatisticsProcessor implements IProcessor, MageventoryConstants {
	
	@Override
	public Bundle process(Context context, String[] params, Bundle extras) {
		SettingsSnapshot ss = (SettingsSnapshot)extras.get(EKEY_SETTINGS_SNAPSHOT);
		
		MagentoClient client;
		try {
			client = new MagentoClient(ss);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e.getMessage());
		}

		final Map<String, Object> response = client.catalogProductStatistics();
		
		if (response != null)
		{
			JobCacheManager.storeStatistics(response, ss.getUrl());
		}
		else
		{
			throw new RuntimeException(client.getLastErrorMessage());
		}
		
		return null;
	}

}
