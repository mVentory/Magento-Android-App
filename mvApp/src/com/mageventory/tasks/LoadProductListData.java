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

import android.os.AsyncTask;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.ProductListActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.settings.SettingsSnapshot;

public class LoadProductListData extends AsyncTask<Object, Integer, Boolean> implements
        MageventoryConstants {

    private boolean forceReload;
    private SettingsSnapshot mSettingsSnapshot;
    ProductListActivity mHost;

    public LoadProductListData(boolean forceReload, ProductListActivity host) {
        super();
        this.forceReload = forceReload;
        mHost = host;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        mSettingsSnapshot = new SettingsSnapshot(mHost);
    }

    /**
     * Expected arguments:
     * <ul>
     * <li>Activity host</li>
     * <li>int resourceType</li>
     * <li>String[] resourceParams</li>
     * </ul>
     */
    @Override
    protected Boolean doInBackground(Object... args) {
        setThreadName();
        try {
            if (args == null || args.length < 1) {
                throw new IllegalArgumentException();
            }

            final int resType = (Integer) args[0];
            final String[] params = args.length >= 2 ? (String[]) args[1] : null;

            if ((!forceReload && JobCacheManager.productListExist(params,
                            mSettingsSnapshot.getUrl()))) {
                // there is cached data available, retrieve and display it
                mHost.restoreAndDisplayProductList(resType, params);
            } else {
                // load new data
                final int reqId = ResourceServiceHelper.getInstance().loadResource(mHost, resType,
                        params, mSettingsSnapshot);
                mHost.operationRequestId.set(reqId);
            }
            return Boolean.TRUE;
        } catch (Throwable e) {
            return Boolean.FALSE;
        }
    }

    private void setThreadName() {
        final String threadName = Thread.currentThread().getName();
        Thread.currentThread().setName("LoadDataTask[" + threadName + "]");
    }

}
