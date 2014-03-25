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

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SoundEffectConstants;
import android.view.ViewGroup;

import com.mageventory.R;
import com.mageventory.activity.base.BaseFragmentActivity.BroadcastManager;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils.BroadcastReceiverRegisterHandler;
import com.mageventory.util.TrackerUtils;

/* This is one of the base classes for all activities in this application. Please note that all activities should
 * extend either BaseActivity or BaseListActivity. */
public class BaseListActivity extends ListActivity implements BroadcastReceiverRegisterHandler {

    static final String TAG = BaseListActivity.class.getSimpleName();
    static final String CATEGORY = "Activity Lifecycle";

    private BaseActivityCommon<BaseListActivity> mBaseActivityCommon;
    private BroadcastManager mBroadcastManager = new BroadcastManager();

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
        super.setContentView(R.layout.activities_root_with_list);

        mBaseActivityCommon = new BaseActivityCommon<BaseListActivity>(this);
        mBaseActivityCommon.onCreate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        trackLifecycleEvent("onDestroy");
        mBaseActivityCommon.onDestroy();
        mBroadcastManager.onDestroy();
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        trackLifecycleEvent("onPause");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        trackLifecycleEvent("onActivityResult");
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup root = (ViewGroup) findViewById(R.id.content_frame);
        root.removeAllViews();
        mBaseActivityCommon.setContentView(layoutResID);
        onContentChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mBaseActivityCommon.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mBaseActivityCommon.onOptionsItemSelected(item);
    }

    public void hideKeyboard()
    {
        mBaseActivityCommon.hideKeyboard();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        hideKeyboard();
        return super.onPrepareOptionsMenu(menu);
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

    @Override
    public void addRegisteredLocalReceiver(BroadcastReceiver receiver) {
        mBroadcastManager.addRegisteredLocalReceiver(receiver);
    }
}
