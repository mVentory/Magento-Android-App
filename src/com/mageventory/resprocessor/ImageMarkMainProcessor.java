package com.mageventory.resprocessor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.ResourceProcessorManager.IProcessor;

public class ImageMarkMainProcessor implements IProcessor, MageventoryConstants {

	@Override
	public Bundle process(Context context, String[] params, Bundle extras) {
		final MagentoClient client = ((MyApplication) context.getApplicationContext()).getClient2();
		if (client == null) {
			return null;
		}
		
		HashMap<String, Object> image_data = new HashMap<String, Object>();
		image_data.put("types", new Object[] { "image", "small_image", "thumbnail" });

		Boolean updateSuccessful = client.catalogProductAttributeMediaUpdate(params[0], params[1], image_data);
		
		if (updateSuccessful == null || updateSuccessful == false) {
			throw new RuntimeException(client.getLastErrorMessage());
		}
		
		return null;
	}

}
