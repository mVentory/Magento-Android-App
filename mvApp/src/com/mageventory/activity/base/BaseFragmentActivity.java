
package com.mageventory.activity.base;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SoundEffectConstants;

import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.util.EventBusUtils.BroadcastReceiverRegisterHandler;

/* This is one of the base classes for all activities in this application. Please note that all activities should
 * extend either BaseActivity, BaseFragmentActivity or BaseListActivity. */
public class BaseFragmentActivity extends FragmentActivity implements
        BroadcastReceiverRegisterHandler {

    private BaseActivityCommon<BaseFragmentActivity> mBaseActivityCommon;
    private boolean mActivityAlive;
    private BroadcastManager mBroadcastManager = new BroadcastManager();
    private boolean mResumed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    public void hideKeyboard()
    {
        mBaseActivityCommon.hideKeyboard();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
    protected void onResume() {
        super.onResume();
        mResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
    }

    public boolean isActivityResumed() {
        return mResumed;
    }

    protected void closeDrawers() {
        mBaseActivityCommon.closeDrawers();
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
