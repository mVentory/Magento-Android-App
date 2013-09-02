
package com.mageventory.resprocessor;

import java.net.MalformedURLException;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.settings.SettingsSnapshot;

public class ImageDeleteProcessor implements IProcessor, MageventoryConstants {

    @Override
    public Bundle process(Context context, String[] params, Bundle extras) {
        SettingsSnapshot ss = (SettingsSnapshot) extras.get(EKEY_SETTINGS_SNAPSHOT);

        MagentoClient client;
        try {
            client = new MagentoClient(ss);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage());
        }

        if (client == null) {
            return null;
        }

        Boolean deleteSuccessful = client.catalogProductAttributeMediaRemove(params[0], params[1]);

        if (deleteSuccessful == null || deleteSuccessful == false) {
            throw new RuntimeException(client.getLastErrorMessage());
        }

        return null;
    }

}
