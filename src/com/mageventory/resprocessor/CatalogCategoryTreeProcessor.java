package com.mageventory.resprocessor;

import java.io.IOException;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient2;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.ResourceProcessorManager.IProcessor;

public class CatalogCategoryTreeProcessor implements IProcessor,
		MageventoryConstants {

	@Override
	public Bundle process(Context context, String[] params, Bundle extras) {
		final MagentoClient2 client = ((MyApplication) context
				.getApplicationContext()).getClient2();
		final Map<String, Object> tree = client.catalogCategoryTree();
		if (tree != null) {
			JobCacheManager.storeCategories(tree);
		} else {
			throw new RuntimeException(client.getLastErrorMessage());
		}
		return null;
	}

}
