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
				Map<String, Object> attrSetMap = (Map<String, Object>) elem;
				atrsList.add(attrSetMap);
				
				Object [] customAttrs = (Object[])attrSetMap.get("attributes");
				
				final List<Map<String, Object>> customAttrsList = new ArrayList<Map<String,Object>>(customAttrs.length);
				for (final Object obj : customAttrs) {
					Map<String, Object> attributeMap = (Map<String, Object>) obj; 
					
					final String atrCode = attributeMap.get(MAGEKEY_ATTRIBUTE_CODE_ATTRIBUTE_LIST_REQUEST).toString();

					if (atrCode.endsWith("_") == false) {
						continue;
					}
					
					customAttrsList.add(attributeMap);
					
					String type = (String) attributeMap.get(MAGEKEY_ATTRIBUTE_TYPE);
					
					if (type.equals("multiselect") ||
						type.equals("dropdown") ||
						type.equals("boolean") ||
						type.equals("select"))
					{
						Object [] options = (Object []) attributeMap.get(MAGEKEY_ATTRIBUTE_OPTIONS);
						List<Object> optionsList = new ArrayList<Object>();
						
						for(Object option : options)
						{
							optionsList.add(option);
						}
						
						Collections.sort(optionsList,
							new Comparator<Object>() {

								@Override
								public int compare(Object lhs, Object rhs) {
									String left = (String)(((Map<String, Object>) lhs).get(MAGEKEY_ATTRIBUTE_OPTIONS_LABEL));
									String right = (String)(((Map<String, Object>) rhs).get(MAGEKEY_ATTRIBUTE_OPTIONS_LABEL));
									
									if (left.equals("Other") && !right.equals("Other"))
										return 1;
									
									if (right.equals("Other") && !left.equals("Other"))
										return -1;
									
									return left.compareTo(right);
								}
							}
						);
						
						optionsList.toArray(options);
					}
				}
				attrSetMap.put("attributes", customAttrsList);	
			}
			
			JobCacheManager.storeAttributes(atrsList);
		}
		
		return null;
	}
}
