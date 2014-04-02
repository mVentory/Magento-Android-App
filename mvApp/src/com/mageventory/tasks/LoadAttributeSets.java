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

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.AbsProductActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.SettingsSnapshot;

public class LoadAttributeSets extends BaseTask<AbsProductActivity, List<Map<String, Object>>>
        implements
        MageventoryConstants, OperationObserver {

    public static Object sCatalogProductAttributesLock = new Object();

    private CountDownLatch doneSignal;
    private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
    private boolean forceRefresh = false;

    private int state = TSTATE_NEW;
    private boolean atrSuccess;
    private int atrRequestId = INVALID_REQUEST_ID;
    private SettingsSnapshot mSettingsSnapshot;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        state = TSTATE_RUNNING;

        getHost().onAttributeSetLoadStart();

        mSettingsSnapshot = new SettingsSnapshot(getHost());
    }

    @Override
    protected Integer doInBackground(Object... args) {
        synchronized (sCatalogProductAttributesLock)
        {
            if (args == null || args.length != 1) {
                throw new IllegalArgumentException();
            }
            if (args[0] instanceof Boolean == false) {
                throw new IllegalArgumentException();
            }

            if (getHost().inputCache == null)
            {
                final Map<String, List<String>> inputCache = JobCacheManager
                        .loadInputCache(mSettingsSnapshot.getUrl());

                getHost().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getHost().onInputCacheLoaded(inputCache);
                    }
                });
            }

            forceRefresh = (Boolean) args[0];

            AbsProductActivity host = getHost();

            if (isCancelled()) {
                return 0;
            }

            if (forceRefresh
                    || JobCacheManager.attributeSetsExist(mSettingsSnapshot.getUrl()) == false) {
                // remote load
                doneSignal = new CountDownLatch(1);
                resHelper.registerLoadOperationObserver(this);

                atrRequestId = resHelper.loadResource(host, RES_CATALOG_PRODUCT_ATTRIBUTES,
                        mSettingsSnapshot);

                while (true) {
                    if (isCancelled()) {
                        return 0;
                    }
                    try {
                        if (doneSignal.await(1, TimeUnit.SECONDS)) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        return 0;
                    }
                }

                resHelper.unregisterLoadOperationObserver(this);
            } else {
                atrSuccess = true;
            }

            if (isCancelled()) {
                return 0;
            }

            final List<Map<String, Object>> atrs;
            if (atrSuccess) {
                atrs = JobCacheManager.restoreAttributeSets(mSettingsSnapshot.getUrl());
            } else {
                atrs = null;
            }
            setData(atrs);

            if (isCancelled()) {
                return 0;
            }

            final AbsProductActivity finalHost = host;
            host.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (atrs != null) {
                        finalHost.onAttributeSetLoadSuccess();
                    } else {
                        finalHost.onAttributeSetLoadFailure();
                    }
                }
            });

            return 0;
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        state = TSTATE_TERMINATED;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        state = TSTATE_CANCELED;
    }

    @Override
    public void onLoadOperationCompleted(final LoadOperation op) {
        if (atrRequestId == op.getOperationRequestId()) {
            atrSuccess = op.getException() == null;
            doneSignal.countDown();
        }
    }

    public int getState() {
        return state;
    }

}
