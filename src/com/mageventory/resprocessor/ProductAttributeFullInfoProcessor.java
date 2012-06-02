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

public class ProductAttributeFullInfoProcessor implements IProcessor, MageventoryConstants {

	@Override
	public Bundle process(Context context, String[] params, Bundle extras, String parameterizedResourceUri,
			ResourceStateDao state, ResourceCache cache) {

		final MyApplication application = (MyApplication) context.getApplicationContext();
		final MagentoClient2 client = application.getClient2();

		final Object [] atrs = client.productAttributeFullInfo();
    	List<Map<String, Object>> atrsList = new ArrayList<Map<String, Object>>();

		
		if (atrs != null)
		{
			for(Object elem : atrs)
			{
				Map<String, Object> attrSetMap = (Map<String, Object>)elem;
				atrsList.add(attrSetMap);
				
				Object [] customAttrs = (Object[])attrSetMap.get("attributes");
				
				final List<Map<String, Object>> customAttrsList = new ArrayList<Map<String,Object>>(customAttrs.length);
				for (final Object obj : customAttrs) {
					customAttrsList.add((Map<String, Object>) obj);
				}
				
				attrSetMap.put("attributes", customAttrsList);
				
				for (Iterator<Map<String, Object>> iter = customAttrsList.iterator(); iter.hasNext();) {
					final Map<String, Object> atrData = iter.next();
					final String atrCode = atrData.get(MAGEKEY_ATTRIBUTE_CODE_ATTRIBUTE_LIST_REQUEST).toString();
					if (TextUtils.isEmpty(atrCode)) {
						continue;
					}
					if (atrCode.endsWith("_") == false) {
						iter.remove();
						continue;
					}
				}
			}
			
			JobCacheManager.storeAttributes(atrsList);
		}
		
		return null;
	}
}
