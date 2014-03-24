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

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.os.AsyncTask;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.MainActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceConstants;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.settings.SettingsSnapshot;

public class LoadStatistics extends AsyncTask<Void, Void, Boolean> implements ResourceConstants,
        OperationObserver,
        MageventoryConstants {

    private CountDownLatch mDoneSingal;
    private ResourceServiceHelper mResHelper = ResourceServiceHelper.getInstance();
    private int mRequestID = INVALID_REQUEST_ID;
    private MainActivity mHost;
    private boolean mSuccess;
    public Map<String, Object> mStatisticsData;
    private SettingsSnapshot mSettingsSnapshot;
    private boolean mForceReload;

    public LoadStatistics(MainActivity host, boolean forceReload) {
        mHost = host;
        mForceReload = forceReload;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mSettingsSnapshot = new SettingsSnapshot(mHost);
        mHost.statisticsLoadStart();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        if (mForceReload || !JobCacheManager.statisticsExist(mSettingsSnapshot.getUrl()))
        {
            mDoneSingal = new CountDownLatch(1);
            mResHelper.registerLoadOperationObserver(this);

            mRequestID = mResHelper.loadResource(mHost, RES_CATALOG_PRODUCT_STATISTICS,
                    mSettingsSnapshot);
            while (true) {
                if (isCancelled()) {
                    return true;
                }
                try {
                    if (mDoneSingal.await(1, TimeUnit.SECONDS)) {
                        break;
                    }
                } catch (InterruptedException e) {
                    return true;
                }
            }

            mResHelper.unregisterLoadOperationObserver(this);
        }
        else
        {
            mSuccess = true;
        }

        if (isCancelled()) {
            return true;
        }

        if (mSuccess) {
            mStatisticsData = JobCacheManager.restoreStatistics(mSettingsSnapshot.getUrl());

            if (mStatisticsData == null || mStatisticsData.isEmpty()) {
                mSuccess = false;
            }
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if (mSuccess) {
            mHost.statisticsLoadSuccess();
        } else {
            mHost.statisticsLoadFailure();
        }
    }

    @Override
    public void onLoadOperationCompleted(LoadOperation op) {
        if (op.getOperationRequestId() == mRequestID) {
            mSuccess = op.getException() == null;
            mDoneSingal.countDown();
        }
    }

}
