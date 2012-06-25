package com.mageventory.resprocessor;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient;
import com.mageventory.res.ResourceProcessorManager.IProcessor;

public class ImageDeleteProcessor implements IProcessor, MageventoryConstants {

	@Override
	public Bundle process(Context context, String[] params, Bundle extras) {
		final MagentoClient client = ((MyApplication) context.getApplicationContext()).getClient2();
		if (client == null) {
			return null;
		}
		
		Boolean deleteSuccessful = client.catalogProductAttributeMediaRemove(params[0], params[1]);
		
		if (deleteSuccessful == null || deleteSuccessful == false) {
			throw new RuntimeException(client.getLastErrorMessage());
		}
		
		return null;
	}

}
