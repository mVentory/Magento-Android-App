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

public class ExecuteProfile extends AsyncTask<Void, Void, Boolean> implements ResourceConstants,
        OperationObserver,
        MageventoryConstants {

    private CountDownLatch mDoneSingal;
    private ResourceServiceHelper mResHelper = ResourceServiceHelper.getInstance();
    private int mRequestID = INVALID_REQUEST_ID;
    private MainActivity mHost;
    private boolean mSuccess;
    public String mProfileExecutionMessage;
    private SettingsSnapshot mSettingsSnapshot;
    private String mProfileID;

    public ExecuteProfile(MainActivity host, String profileID) {
        mHost = host;
        mProfileID = profileID;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mSettingsSnapshot = new SettingsSnapshot(mHost);
        mHost.profileExecutionStart();
    }

    @Override
    protected Boolean doInBackground(Void... p) {
        String[] params = new String[] {
            mProfileID
        };

        mDoneSingal = new CountDownLatch(1);
        mResHelper.registerLoadOperationObserver(this);

        mRequestID = mResHelper.loadResource(mHost, RES_EXECUTE_PROFILE, params, mSettingsSnapshot);
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

        if (isCancelled()) {
            return true;
        }

        if (mSuccess) {
            mProfileExecutionMessage = JobCacheManager.restoreProfileExecution(params,
                    mSettingsSnapshot.getUrl());

            if (mProfileExecutionMessage == null) {
                mSuccess = false;
            }
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if (mSuccess) {
            mHost.profileExecutionSuccess();
        } else {
            mHost.profileExecutionFailure();
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
