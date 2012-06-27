package com.mageventory.resprocessor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.settings.SettingsSnapshot;

public class CatalogCategoryTreeProcessor implements IProcessor, MageventoryConstants {

	@Override
	public Bundle process(Context context, String[] params, Bundle extras) {
		SettingsSnapshot ss = (SettingsSnapshot)extras.get(EKEY_SETTINGS_SNAPSHOT);
		
		MagentoClient client;
		try {
			client = new MagentoClient(ss);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e.getMessage());
		}
		
		final Map<String, Object> tree = client.catalogCategoryTree();
		if (tree != null) {
			JobCacheManager.storeCategories(tree, ss.getUrl());
		} else {
			throw new RuntimeException(client.getLastErrorMessage());
		}
		return null;
	}

}
