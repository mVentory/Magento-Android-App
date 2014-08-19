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

package com.mageventory.activity.base;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SoundEffectConstants;

import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils.BroadcastReceiverRegisterHandler;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.TrackerUtils;

/* This is one of the base classes for all activities in this application. Please note that all activities should
 * extend either BaseActivity, BaseFragmentActivity or BaseListActivity. */
public class BaseFragmentActivity extends FragmentActivity implements
        BroadcastReceiverRegisterHandler {

    static final String TAG = BaseFragmentActivity.class.getSimpleName();
    static final String CATEGORY = "Activity Lifecycle";

    private BaseActivityCommon<BaseFragmentActivity> mBaseActivityCommon;
    private boolean mActivityAlive;
    private BroadcastManager mBroadcastManager = new BroadcastManager();

    /**
     * Whether activity is resumed flag. Handled in onResume, onPause methods
     */
    private boolean mResumed = false;

    void trackLifecycleEvent(String event) {
        CommonUtils.debug(TAG, event + ": " + getClass().getSimpleName());
        TrackerUtils.trackEvent(CATEGORY, event, getClass().getSimpleName());
    }

    @Override
    protected void onStart() {
        super.onStart();
        trackLifecycleEvent("onStart");
        TrackerUtils.activityStart(this);
        TrackerUtils.trackView(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        trackLifecycleEvent("onStop");
        TrackerUtils.activityStop(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trackLifecycleEvent("onCreate");
        super.setContentView(R.layout.activities_root);
        mActivityAlive = true;
        mBaseActivityCommon = new BaseActivityCommon<BaseFragmentActivity>(this);
        mBaseActivityCommon.onCreate();
    }

    @Override
    public void setContentView(int layoutResID) {
        mBaseActivityCommon.setContentView(layoutResID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mBaseActivityCommon.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mBaseActivityCommon.onOptionsItemSelected(item);
    }

    public void postDelayedHideKeyboard() {
        GuiUtils.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideKeyboard();
            }
        }, 100);
    }

    public void hideKeyboard()
    {
        mBaseActivityCommon.hideKeyboard();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        trackLifecycleEvent("onDestroy");
        mBaseActivityCommon.onDestroy();
        mActivityAlive = false;
        mBroadcastManager.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        hideKeyboard();
        return super.onPrepareOptionsMenu(menu);
    }

    public boolean isActivityAlive()
    {
        return mActivityAlive;
    }

    @Override
    public void addRegisteredLocalReceiver(BroadcastReceiver receiver) {
        mBroadcastManager.addRegisteredLocalReceiver(receiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        trackLifecycleEvent("onSaveInstanceState");
    }

    @Override
    protected void onResume() {
        super.onResume();
        trackLifecycleEvent("onResume");
        mResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        trackLifecycleEvent("onPause");
        mResumed = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        trackLifecycleEvent("onActivityResult");
    }

    /**
     * Is the activity active and resumed
     * 
     * @return
     */
    public boolean isActivityResumed() {
        return mResumed;
    }

    protected void closeDrawers() {
        mBaseActivityCommon.closeDrawers();
    }

    /**
     * Lock/unlock drawers
     * 
     * @param locked
     */
    public void setDrawersLocked(boolean locked) {
        mBaseActivityCommon.setDrawersLocked(locked);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            getWindow().getDecorView().findViewById(android.R.id.content)
                    .playSoundEffect(SoundEffectConstants.CLICK);
            mBaseActivityCommon.toggleMenuVisibility();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public BaseActivityCommon<BaseFragmentActivity> getBaseActivityCommon() {
        return mBaseActivityCommon;
    }

    public static class BroadcastManager implements BroadcastReceiverRegisterHandler {
        private List<BroadcastReceiver> mReceivers = new ArrayList<BroadcastReceiver>();

        @Override
        public void addRegisteredLocalReceiver(BroadcastReceiver receiver) {
            mReceivers.add(receiver);
        }

        public void onDestroy() {
            for (BroadcastReceiver br : mReceivers) {
                LocalBroadcastManager.getInstance(MyApplication.getContext())
                        .unregisterReceiver(br);
            }
            mReceivers.clear();
        }
    }
}
