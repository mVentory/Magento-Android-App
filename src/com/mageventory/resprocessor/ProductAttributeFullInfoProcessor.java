package com.mageventory.resprocessor;

import java.io.IOException;
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
import com.mageventory.res.ResourceCache;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.res.ResourceStateDao;

public class ProductAttributeFullInfoProcessor implements IProcessor, MageventoryConstants {

	@Override
	public Bundle process(Context context, String[] params, Bundle extras, String parameterizedResourceUri,
			ResourceStateDao state, ResourceCache cache) {

		final MyApplication application = (MyApplication) context.getApplicationContext();
		final MagentoClient2 client = application.getClient2();

		//final List<Map<String, Object>> atrs = client.productAttributeFullInfo();
		final Object atrs = client.productAttributeFullInfo();

		return null;
	}
}
