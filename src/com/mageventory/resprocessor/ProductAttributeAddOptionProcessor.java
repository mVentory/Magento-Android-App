package com.mageventory.resprocessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient2;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.ResourceProcessorManager.IProcessor;

public class ProductAttributeAddOptionProcessor implements IProcessor,
		MageventoryConstants {

	@Override
	public Bundle process(Context context, String[] params, Bundle extras) {

		final MyApplication application = (MyApplication) context
				.getApplicationContext();
		final MagentoClient2 client = application.getClient2();

		final Map<String, Object> attrib = client.productAttributeAddOption(
				params[0], params[1]);

		boolean newOptionPresentInTheResponse = false;
		
		if (attrib != null) {

			Object[] options = (Object[]) attrib.get(MAGEKEY_ATTRIBUTE_OPTIONS);
			List<Object> optionsList = new ArrayList<Object>();

			for (Object option : options) {
				optionsList.add(option);
				
				String optionLabel = (String) (((Map<String, Object>) option)
						.get(MAGEKEY_ATTRIBUTE_OPTIONS_LABEL));
				
				if (TextUtils.equals(optionLabel, params[1]))
				{
					newOptionPresentInTheResponse = true;
				}
			}

			ProductAttributeFullInfoProcessor.sortOptionsList(optionsList);
			
			optionsList.toArray(options);

			if (newOptionPresentInTheResponse == true)
			{
				JobCacheManager.updateSingleAttributeInTheCache(attrib, params[2]);
			}
			else
			{
				throw new RuntimeException("New option label missing from the server response.");
			}
			
		} else {
			throw new RuntimeException(client.getLastErrorMessage());
		}
		
		return null;
	}
}
