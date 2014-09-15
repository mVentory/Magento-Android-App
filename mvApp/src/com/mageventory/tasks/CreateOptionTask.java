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

package com.mageventory.tasks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttribute.CustomAttributeOption;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.model.CustomAttributesList.OnAttributeValueChangedListener;
import com.mageventory.model.CustomAttributesList.OnNewOptionTaskEventListener;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceConstants;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.resprocessor.ProductAttributeAddOptionProcessor;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;

/*
 * An asynctask which can be used to create an attribute option on the
 * server in asynchronous way.
 */
public class CreateOptionTask extends AsyncTask<Void, Void, Boolean> implements ResourceConstants,
        OperationObserver,
        MageventoryConstants {

    private CountDownLatch doneSignal;
    private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
    private int requestId = INVALID_REQUEST_ID;
    private Activity host;
    private boolean success;
    private CustomAttribute attribute;
    /**
     * List of custom attributes. TODO perhaps it is unnecessary to have this
     * field.
     */
    private List<CustomAttribute> attribList;
    private String newOptionName;
    private String setID;
    private OnNewOptionTaskEventListener newOptionListener;
    private List<Map<String, Object>> customAttributesList;
    private SettingsSnapshot mSettingsSnapshot;
    /**
     * Reference to the {@link OnAttributeValueChangedListener} which should be
     * called when user manually changes attribute value
     */
    private transient OnAttributeValueChangedListener mOnAttributeValueChangedByUserInputListener;

    public CreateOptionTask(Activity host, CustomAttribute attribute,
            List<CustomAttribute> attribList, String newOptionName, String setID,
            OnNewOptionTaskEventListener listener,
            OnAttributeValueChangedListener onAttributeValueChangedByUserInputListener) {
        this.host = host;
        this.attribute = attribute;
        this.newOptionName = newOptionName;
        this.setID = setID;
        this.newOptionListener = listener;
        this.attribList = attribList;
        mOnAttributeValueChangedByUserInputListener = onAttributeValueChangedByUserInputListener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (newOptionListener != null) {
            newOptionListener.OnAttributeCreationStarted();

            attribute.getAttributeLoadingControl().startLoading();
        }
        mSettingsSnapshot = new SettingsSnapshot(host);
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        doneSignal = new CountDownLatch(1);
        resHelper.registerLoadOperationObserver(this);
        requestId = resHelper.loadResource(host, RES_PRODUCT_ATTRIBUTE_ADD_NEW_OPTION,
                new String[] {
                        attribute.getCode(), newOptionName, setID
                }, mSettingsSnapshot);
        while (true) {
            if (isCancelled()) {
                return true;
            }
            try {
                if (doneSignal.await(1, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                return true;
            }
        }
        resHelper.unregisterLoadOperationObserver(this);

        if (host == null || isCancelled()) {
            return true;
        }

        if (success) {
            customAttributesList = JobCacheManager.restoreAttributeList(setID,
                    mSettingsSnapshot.getUrl());

            if (customAttributesList == null) {
                success = false;
            }
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if (success) {
            updateCustomAttributeOptions(attribute, setID, customAttributesList,
                    newOptionName, attribList, mOnAttributeValueChangedByUserInputListener);

            if (newOptionListener != null) {
                newOptionListener.OnAttributeCreationFinished(attribute.getMainLabel(),
                        newOptionName, true);
                attribute.getAttributeLoadingControl().stopLoading();
            }

        } else {
            attribute.removeOption(host, newOptionName);

            if (newOptionListener != null) {
                newOptionListener.OnAttributeCreationFinished(attribute.getMainLabel(),
                        newOptionName, false);
                attribute.getAttributeLoadingControl().stopLoading();
            }
        }
    }

    @Override
    public void onLoadOperationCompleted(LoadOperation op) {
        if (op.getOperationRequestId() == requestId) {
            success = op.getException() == null;
            doneSignal.countDown();
        }
    }

    /*
     * Update a single custom attribute's options with new data from the cache.
     * Also make this option selected + update the for user to see the changes.
     */
    public static void updateCustomAttributeOptions(CustomAttribute attr,
            String attributeSetId,
            List<Map<String, Object>> customAttrsList, String newOptionToSet,
            List<CustomAttribute> listCopy,
            OnAttributeValueChangedListener onAttributeValueChangedByUserInputListener) {

        if (customAttrsList == null)
            return;
        String oldValue = attr.getSelectedValue();
        for (Map<String, Object> elem : customAttrsList) {
            if (TextUtils.equals((String) elem.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE),
                    attr.getCode())) {

                // TODO perhaps this is unnecessary here and we may simply copy
                // value from attr to updatedAttrib via
                // CustomAttributesList.restoreAttributeValue method
                CustomAttribute updatedAttrib = CustomAttributesList.createCustomAttribute(elem,
                        listCopy);
                copySerializableData(updatedAttrib, attr);

                int i = 0;
                for (CustomAttributeOption option : attr.getOptions()) {

                    if (ProductAttributeAddOptionProcessor.optionStringsEqual(option.getLabel(),
                            newOptionToSet)) {
                        attr.setOptionSelected(i, true, true);

                        break;
                    }
                    i++;
                }

                break;
            }
        }
        Intent intent = EventBusUtils
                .getGeneralEventIntent(EventType.CUSTOM_ATTRIBUTE_OPTIONS_UPDATED);
        intent.putExtra(EventBusUtils.ATTRIBUTE, (Parcelable) attr);
        intent.putExtra(EventBusUtils.ATTRIBUTE_SET, attributeSetId);
        EventBusUtils.sendGeneralEventBroadcast(intent);
        if (onAttributeValueChangedByUserInputListener != null) {
            onAttributeValueChangedByUserInputListener.attributeValueChanged(oldValue,
                    attr.getSelectedValue(), attr);
        }
    }

    /*
     * Copy all serializable data (which in this case is everything except View
     * classes from one CustomAttribute to another)
     */
    public static void copySerializableData(CustomAttribute from, CustomAttribute to) {
        to.setType(from.getType());
        to.setIsRequired(from.getIsRequired());
        to.setConfigurable(from.isConfigurable());
        to.setUseForSearch(from.isUseForSearch());
        to.setCopyFromSearch(from.isCopyFromSearch());
        to.setMainLabel(from.getMainLabel());
        to.setCode(from.getCode());
        to.setOptions(from.getOptions());
        to.setAttributeID(from.getAttributeID());
    }
}
