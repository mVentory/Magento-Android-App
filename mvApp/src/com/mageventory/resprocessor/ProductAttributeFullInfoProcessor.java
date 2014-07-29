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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.Log;
import com.mageventory.util.TrackerUtils;

public class ProductAttributeFullInfoProcessor implements IProcessor, MageventoryConstants {

    static final String TAG = ProductAttributeFullInfoProcessor.class.getSimpleName();

    private static boolean isNumber(String string) {
        try {
            Double.parseDouble(string);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    /*
     * Return positive value if left option should be put after the right option
     * and negative value otherwise.
     */
    public static int compareOptions(String left, String right) {
        /* Putting "Other" always at the end of the list. */
        if (left.equalsIgnoreCase("Other") && !right.equalsIgnoreCase("Other"))
            return 1;

        if (right.equalsIgnoreCase("Other") && !left.equalsIgnoreCase("Other"))
            return -1;

        if (isNumber(left) && isNumber(right)) {
            if (Double.parseDouble(left) > Double.parseDouble(right)) {
                return 1;
            } else {
                return -1;
            }
        }

        return left.compareToIgnoreCase(right);
    }

    public static void sortOptionsList(List<Object> optionsList) {
        Collections.sort(optionsList, new Comparator<Object>() {

            @Override
            public int compare(Object lhs, Object rhs) {
                String left = (String) (((Map<String, Object>) lhs)
                        .get(MAGEKEY_ATTRIBUTE_OPTIONS_LABEL));
                String right = (String) (((Map<String, Object>) rhs)
                        .get(MAGEKEY_ATTRIBUTE_OPTIONS_LABEL));

                return compareOptions(left, right);
            }
        });
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

        final Object[] atrs = client.productAttributeFullInfo();
        List<Map<String, Object>> atrSets = new ArrayList<Map<String, Object>>();

        if (atrs != null) {
            for (Object elem : atrs) {
                Map<String, Object> attrSetMap = (Map<String, Object>) elem;
                atrSets.add(attrSetMap);

                Object[] customAttrs = JobCacheManager
                        .getObjectArrayFromDeserializedItem(attrSetMap.get("attributes"));
                String setName = (String) attrSetMap.get(MAGEKEY_ATTRIBUTE_SET_NAME);
                String setID = (String) attrSetMap.get(MAGEKEY_ATTRIBUTE_SET_ID);

                final List<Map<String, Object>> customAttrsList = new ArrayList<Map<String, Object>>(
                        customAttrs.length);
                for (final Object obj : customAttrs) {
                    Map<String, Object> attributeMap = (Map<String, Object>) obj;

                    String label = (String) attributeMap.get(MAGEKEY_ATTRIBUTE_LABEL);

                    if (TextUtils.equals(setName, label)) {
                        /*
                         * Special attribute that is used for compound names
                         * formatting.
                         */
                        attributeMap.put(MAGEKEY_ATTRIBUTE_IS_FORMATTING_ATTRIBUTE, new Boolean(
                                true));
                        customAttrsList.add(attributeMap);
                        continue;
                    }
                    final String atrCode = attributeMap.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE)
                            .toString();
                    // if attribute code marked as special attribute then skip
                    // it
                    if (Product.SPECIAL_ATTRIBUTES.contains(atrCode)) {
                        continue;
                    }
                    String type = (String) attributeMap.get(MAGEKEY_ATTRIBUTE_TYPE);
                    // some not yet filtered attribute has null type. We should
                    // skip them
                    if (type == null) {
                        String message = CommonUtils.format("Null attribute type %1$s", atrCode);
                        Log.d(TAG, message);
                        TrackerUtils.trackErrorEvent("ProductAttributeFullInfoProcessor.process",
                                message);
                        continue;
                    }
                    CommonUtils.debug(TAG, "processing attribute %1$s", atrCode);
                    customAttrsList.add(attributeMap);

                    if (type.equals("multiselect") || type.equals("dropdown")
                            || type.equals("boolean")
                            || type.equals("select")) {
                        Object[] options = JobCacheManager
                                .getObjectArrayFromDeserializedItem(attributeMap
                                        .get(MAGEKEY_ATTRIBUTE_OPTIONS));
                        List<Object> optionsList = new ArrayList<Object>();

                        for (Object option : options) {
                            optionsList.add(option);
                        }

                        sortOptionsList(optionsList);

                        optionsList.toArray(options);
                    }
                }
                attrSetMap.remove("attributes");
                JobCacheManager.storeAttributeList(customAttrsList, setID, ss.getUrl());
            }

            Collections.sort(atrSets, new Comparator<Map<String, Object>>() {

                @Override
                public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {

                    String setNameLeft = ((String) lhs.get(MAGEKEY_ATTRIBUTE_SET_NAME))
                            .toLowerCase();
                    String setNameRight = ((String) rhs.get(MAGEKEY_ATTRIBUTE_SET_NAME))
                            .toLowerCase();

                    return setNameLeft.compareTo(setNameRight);
                }
            });

            JobCacheManager.storeAttributeSets(atrSets, ss.getUrl());
        }

        return null;
    }
}
