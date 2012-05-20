package com.mageventory.resprocessor;

import java.io.IOException;
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

public class ProductAttributeListProcessor implements IProcessor, MageventoryConstants {

	@Override
	public Bundle process(Context context, String[] params, Bundle extras, String parameterizedResourceUri,
			ResourceStateDao state, ResourceCache cache) {
		state.addResource(parameterizedResourceUri);
		state.setState(parameterizedResourceUri, STATE_BUILDING);

		final MyApplication application = (MyApplication) context.getApplicationContext();
		final MagentoClient2 client = application.getClient2();

		int setId;
		try {
			setId = Integer.parseInt(params[0]);
		} catch (Throwable e) {
			setId = INVALID_ATTRIBUTE_SET_ID;
		}

		if (setId == INVALID_ATTRIBUTE_SET_ID) {
			state.setState(parameterizedResourceUri, STATE_NONE);
			return null;
		}

		state.setTransacting(parameterizedResourceUri, true);
		final List<Map<String, Object>> atrs = client.productAttributeList(setId);

		if (atrs != null) {
    		// apply filters, generate name using the code property, retrieve options if field type is select or multiselect
    		for (Iterator<Map<String, Object>> iter = atrs.iterator(); iter.hasNext();) {
    			final Map<String, Object> atrData = iter.next();
    			final String atrCode = atrData.get(MAGEKEY_ATTRIBUTE_CODE).toString();
    			if (TextUtils.isEmpty(atrCode)) {
    				continue;
    			}
    			if (atrCode.endsWith("_") == false) {
    				iter.remove();
    				continue;
    			}
    			// y: since the Magento XMLRPC API is so unfriendly I found myself in need to generate the name
    			// using the CODE field...
    			atrData.put(MAGEKEY_ATTRIBUTE_INAME, genAtrNameFromCode(atrCode));
				final String type = "" + atrData.get(MAGEKEY_ATTRIBUTE_TYPE);
				if ("select".equalsIgnoreCase(type) || "multiselect".equalsIgnoreCase(type)
				        || "boolean".equalsIgnoreCase(type)) {
    				try {
    					final int atrId = Integer.parseInt(atrData.get(MAGEKEY_ATTRIBUTE_ID).toString());
    					List<Map<String, Object>> options = client.productAttributeOptions(atrId);
    					atrData.put(MAGEKEY_ATTRIBUTE_IOPTIONS, options);
    				} catch (Throwable e) {
    					// NOP
    				}
    			}
    		}
		}

		state.setTransacting(parameterizedResourceUri, false);

		try {
			cache.store(context, parameterizedResourceUri, atrs);
			state.setState(parameterizedResourceUri, STATE_AVAILABLE);
		} catch (IOException e) {
			// NOP
		}

		return null;
	}

	private static String genAtrNameFromCode(final String code) {
		if (TextUtils.isEmpty(code)) {
			return "";
		}
		final StringBuilder atrName = new StringBuilder();
		for (String word : TextUtils.split(code, "_")) {
			if (TextUtils.isEmpty(word)) {
				continue;
			}
			word = capitalize(word);
			if (atrName.length() > 0) {
				atrName.append(' ');
			}
			atrName.append(word);
		}
		return atrName.toString();
	}

	private static String capitalize(final String s) {
		if (TextUtils.isEmpty(s)) {
			return "";
		}
		return String.format("%c%s", Character.toUpperCase(s.charAt(0)), s.substring(1));
	}

}
