
package com.mageventory.resprocessor;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.settings.SettingsSnapshot;

public class ProductAttributeAddOptionProcessor implements IProcessor, MageventoryConstants {

    public static boolean optionStringsEqual(String option1, String option2)
    {
        String normalizedOption1 = option1.replaceAll("[^a-zA-Z0-9]+", "");
        String normalizedOption2 = option2.replaceAll("[^a-zA-Z0-9]+", "");

        if (normalizedOption1.equalsIgnoreCase(normalizedOption2))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public Bundle process(Context context, String[] params, Bundle extras) {

        SettingsSnapshot ss = (SettingsSnapshot) extras.get(EKEY_SETTINGS_SNAPSHOT);

        MagentoClient client;
        try {
            client = new MagentoClient(ss);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage());
        }

        final Map<String, Object> attrib = client.productAttributeAddOption(params[0], params[1]);

        boolean newOptionPresentInTheResponse = false;

        if (attrib != null) {

            Object[] options = JobCacheManager.getObjectArrayFromDeserializedItem(attrib
                    .get(MAGEKEY_ATTRIBUTE_OPTIONS));
            List<Object> optionsList = new ArrayList<Object>();

            for (Object option : options) {
                optionsList.add(option);

                String optionLabel = (String) (((Map<String, Object>) option)
                        .get(MAGEKEY_ATTRIBUTE_OPTIONS_LABEL));

                if (optionStringsEqual(optionLabel, params[1])) {
                    newOptionPresentInTheResponse = true;
                }
            }

            ProductAttributeFullInfoProcessor.sortOptionsList(optionsList);

            optionsList.toArray(options);

            if (newOptionPresentInTheResponse == true) {
                JobCacheManager.updateSingleAttributeInTheCache(attrib, params[2], ss.getUrl());
            } else {
                throw new RuntimeException("New option label missing from the server response.");
            }

        } else {
            throw new RuntimeException(client.getLastErrorMessage());
        }

        return null;
    }
}
