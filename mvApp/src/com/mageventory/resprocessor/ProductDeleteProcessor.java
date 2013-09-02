
package com.mageventory.resprocessor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import com.mageventory.util.Log;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.settings.SettingsSnapshot;

public class ProductDeleteProcessor implements IProcessor, MageventoryConstants {

    private static class IncompleteDataException extends RuntimeException {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unused")
        public IncompleteDataException() {
            super();
        }

        public IncompleteDataException(String detailMessage) {
            super(detailMessage);
        }

    }

    private static final String TAG = "ProductDeleteProcessor";

    private String extractString(final Bundle bundle, final String key)
            throws IncompleteDataException {
        final String s = bundle.getString(key);
        if (s == null) {
            throw new IncompleteDataException("bad data for key '" + key + "'");
        }
        return s;
    }

    /**
     * Process extras has all information of order
     */
    @Override
    public Bundle process(Context context, String[] params, Bundle extras) {

        SettingsSnapshot ss = (SettingsSnapshot) extras.get(EKEY_SETTINGS_SNAPSHOT);

        MagentoClient client;
        try {
            client = new MagentoClient(ss);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage());
        }
        // retrieve product (params[0] is SKU)
        client.deleteProduct(params[0]);

        return null;
    }
}
