/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/

package com.mageventory.resprocessor;

import java.io.IOException;
import java.net.MalformedURLException;
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
import com.mageventory.settings.SettingsSnapshot;

public class CatalogProductListProcessor implements IProcessor, MageventoryConstants {

    @Override
    public Bundle process(Context context, String[] params, Bundle extras) {
        // get resource parameters
        String nameFilter = null;
        int categoryId = INVALID_CATEGORY_ID;
        if (params != null) {
            if (params.length >= 1 && params[0] instanceof String) {
                nameFilter = (String) params[0];
            }
            if (params.length >= 2 && TextUtils.isDigitsOnly(params[1])) {
                categoryId = Integer.parseInt(params[1]);
            }
        }

        // retrieve data
        SettingsSnapshot ss = (SettingsSnapshot) extras.get(EKEY_SETTINGS_SNAPSHOT);

        MagentoClient client;
        try {
            client = new MagentoClient(ss);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage());
        }

        final List<Map<String, Object>> productList;

        productList = client.catalogProductList(nameFilter, categoryId);

        // store data
        if (productList != null) {
            JobCacheManager.storeProductList(productList, params, ss.getUrl());
        } else {
            throw new RuntimeException(client.getLastErrorMessage());
        }

        return null;
    }

}
