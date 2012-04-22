package com.mageventory.processor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient2;
import com.mageventory.res.ResourceCache;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.res.ResourceStateDao;

public class ProductAttributeSetListProcessor implements IProcessor, MageventoryConstants {

	@Override
    public Bundle process(Context context, String[] params, Bundle extras, String parameterizedResourceUri,
            ResourceStateDao state, ResourceCache cache) {
		state.addResource(parameterizedResourceUri);
		state.setState(parameterizedResourceUri, STATE_BUILDING);
		
		final MyApplication application = (MyApplication) context.getApplicationContext();
		final MagentoClient2 client = application.getClient2();
		
		state.setTransacting(parameterizedResourceUri, true);
		final List<Map<String,Object>> attrSets = client.catalogProductAttributeSetList();
		state.setTransacting(parameterizedResourceUri, false);
		
		if (attrSets == null) {
			return null;
		}
		
		try {
	        cache.store(context, parameterizedResourceUri, attrSets);
	        state.setState(parameterizedResourceUri, STATE_AVAILABLE);
	        ResourceExpirationRegistry.getInstance().attributeSetListChanged(context);
        } catch (IOException e) {
        	// NOP
        }
		
	    return null;
    }

}
