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

import java.net.MalformedURLException;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MageventoryRuntimeException;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;

/**
 * The get resource processor for the cart clear API call
 */
public class CartClearProcessor implements IProcessor, MageventoryConstants {
    /**
     * Tag used for logging
     */
    static final String TAG = CartClearProcessor.class.getSimpleName();

    @Override
    public Bundle process(Context context, String[] params, Bundle extras) {

        SettingsSnapshot ss = (SettingsSnapshot) extras.get(EKEY_SETTINGS_SNAPSHOT);

        MagentoClient client;
        try {
            client = new MagentoClient(ss);
        } catch (MalformedURLException e) {
            CommonUtils.error(TAG, e);
            throw new MageventoryRuntimeException(e.getMessage());
        }

        final Object[] cartItems = client.cartClear();

        if (cartItems != null) {
            // if the clear cart call was successful
            JobCacheManager.storeCartItems(cartItems, ss.getUrl());
        } else {
            // if the clear cart call failed throw an exception
            throw new MageventoryRuntimeException(client.getLastErrorMessage());
        }

        return null;
    }

}
