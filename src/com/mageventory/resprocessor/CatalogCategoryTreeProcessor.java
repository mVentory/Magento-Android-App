package com.mageventory.resprocessor;

import java.io.IOException;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient2;
import com.mageventory.res.ResourceCache;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.res.ResourceStateDao;

public class CatalogCategoryTreeProcessor implements IProcessor,
		MageventoryConstants {

	@Override
	public Bundle process(Context context, String[] params, Bundle extras,
			String resourceUri, ResourceStateDao state, ResourceCache cache) {
		state.addResource(resourceUri);
		state.setState(resourceUri, STATE_BUILDING);
		final MagentoClient2 client = ((MyApplication) context
				.getApplicationContext()).getClient2();
		state.setTransacting(resourceUri, true);
		final Map<String, Object> tree = client.catalogCategoryTree();
		state.setTransacting(resourceUri, false);
		if (tree != null) {
			try {
				cache.store(context, resourceUri, tree);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			state.setState(resourceUri, STATE_AVAILABLE);
		} else {
			throw new RuntimeException(client.getLastErrorMessage());
		}
		return null;
	}

}
