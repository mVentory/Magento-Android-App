
package com.mageventory.resprocessor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.settings.SettingsSnapshot;

public class ProfilesListProcessor implements IProcessor, MageventoryConstants {

    @Override
    public Bundle process(Context context, String[] params, Bundle extras) {

        SettingsSnapshot ss = (SettingsSnapshot) extras.get(EKEY_SETTINGS_SNAPSHOT);

        MagentoClient client;
        try {
            client = new MagentoClient(ss);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage());
        }

        Object[] profilesList = client.getProfilesList();

        Arrays.sort(profilesList, new Comparator<Object>() {

            @Override
            public int compare(Object lhs, Object rhs) {
                Map<String, Object> left = (Map<String, Object>) lhs;
                Map<String, Object> right = (Map<String, Object>) rhs;

                String leftStr = (String) left.get("name");
                String rightStr = (String) right.get("name");

                return leftStr.compareTo(rightStr);
            }
        });

        if (profilesList != null) {
            JobCacheManager.storeProfilesList(profilesList, ss.getUrl());
        } else {
            throw new RuntimeException(client.getLastErrorMessage());
        }

        return null;
    }
}
