
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

public class LoadProfilesList extends AsyncTask<Void, Void, Boolean> implements ResourceConstants,
        OperationObserver,
        MageventoryConstants {

    private CountDownLatch mDoneSingal;
    private ResourceServiceHelper mResHelper = ResourceServiceHelper.getInstance();
    private int mRequestID = INVALID_REQUEST_ID;
    private MainActivity mHost;
    private boolean mSuccess;
    public Object[] mProfilesData;
    private SettingsSnapshot mSettingsSnapshot;
    private boolean mForceReload;

    public LoadProfilesList(MainActivity host, boolean forceReload) {
        mHost = host;
        mForceReload = forceReload;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mSettingsSnapshot = new SettingsSnapshot(mHost);
        mHost.profilesLoadStart();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        if (mForceReload || !JobCacheManager.profilesListExist(mSettingsSnapshot.getUrl()))
        {
            mDoneSingal = new CountDownLatch(1);
            mResHelper.registerLoadOperationObserver(this);

            mRequestID = mResHelper.loadResource(mHost, RES_GET_PROFILES_LIST, mSettingsSnapshot);
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
            mProfilesData = JobCacheManager.restoreProfilesList(mSettingsSnapshot.getUrl());

            if (mProfilesData == null) {
                mSuccess = false;
            }
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if (mSuccess) {
            mHost.profilesLoadSuccess();
        } else {
            mHost.profilesLoadFailure();
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
