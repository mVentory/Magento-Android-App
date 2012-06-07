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
import com.mageventory.res.ResourceCache;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.res.ResourceStateDao;

public class ProductAttributeAddOptionProcessor implements IProcessor,
		MageventoryConstants {

	@Override
	public Bundle process(Context context, String[] params, Bundle extras,
			String parameterizedResourceUri, ResourceStateDao state,
			ResourceCache cache) {

		final MyApplication application = (MyApplication) context
				.getApplicationContext();
		final MagentoClient2 client = application.getClient2();

		final Map<String, Object> attrib = client.productAttributeAddOption(
				params[0], params[1]);

		if (attrib != null) {
			
			Object[] options = (Object[]) attrib
					.get(MAGEKEY_ATTRIBUTE_OPTIONS);
			List<Object> optionsList = new ArrayList<Object>();

			for (Object option : options) {
				optionsList.add(option);
			}

			Collections.sort(optionsList, new Comparator<Object>() {

				@Override
				public int compare(Object lhs, Object rhs) {
					String left = (String) (((Map<String, Object>) lhs)
							.get(MAGEKEY_ATTRIBUTE_OPTIONS_LABEL));
					String right = (String) (((Map<String, Object>) rhs)
							.get(MAGEKEY_ATTRIBUTE_OPTIONS_LABEL));

					if (left.equals("Other")
							&& !right.equals("Other"))
						return 1;

					if (right.equals("Other")
							&& !left.equals("Other"))
						return -1;

					return left.compareTo(right);
				}
			});
			
			JobCacheManager.updateSingleAttributeInTheCache(attrib, params[2]);
			return new Bundle();
		} else {
			return null;
		}
	}
}
