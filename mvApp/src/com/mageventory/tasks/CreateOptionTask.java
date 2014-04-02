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
import android.os.AsyncTask;
import android.view.View;

import com.mageventory.MageventoryConstants;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.model.CustomAttributesList.OnNewOptionTaskEventListener;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceConstants;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.settings.SettingsSnapshot;

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
    private CustomAttributesList attribList;
    private String newOptionName;
    private String setID;
    private OnNewOptionTaskEventListener newOptionListener;
    private List<Map<String, Object>> customAttributesList;
    private SettingsSnapshot mSettingsSnapshot;

    public CreateOptionTask(Activity host, CustomAttribute attribute,
            CustomAttributesList attribList,
            String newOptionName, String setID, OnNewOptionTaskEventListener listener) {
        this.host = host;
        this.attribute = attribute;
        this.newOptionName = newOptionName;
        this.setID = setID;
        this.newOptionListener = listener;
        this.attribList = attribList;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (newOptionListener != null) {
            newOptionListener.OnAttributeCreationStarted();

            attribute.getNewOptionSpinningWheel().setVisibility(View.VISIBLE);
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
            attribList.updateCustomAttributeOptions(attribute, customAttributesList, newOptionName);

            if (newOptionListener != null) {
                newOptionListener.OnAttributeCreationFinished(attribute.getMainLabel(),
                        newOptionName, true);
                attribute.getNewOptionSpinningWheel().setVisibility(View.GONE);
            }

        } else {
            attribute.removeOption(host, newOptionName);

            if (newOptionListener != null) {
                newOptionListener.OnAttributeCreationFinished(attribute.getMainLabel(),
                        newOptionName, false);
                attribute.getNewOptionSpinningWheel().setVisibility(View.GONE);
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

}
