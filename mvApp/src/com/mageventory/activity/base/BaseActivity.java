
package com.mageventory.activity.base;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SoundEffectConstants;

import com.mageventory.R;
import com.mageventory.activity.base.BaseFragmentActivity.BroadcastManager;
import com.mageventory.util.EventBusUtils.BroadcastReceiverRegisterHandler;

/* This is one of the base classes for all activities in this application. Please note that all activities should
 * extend either BaseActivity or BaseListActivity. */
public class BaseActivity extends Activity implements BroadcastReceiverRegisterHandler {

    private BaseActivityCommon<BaseActivity> mBaseActivityCommon;
    private BroadcastManager mBroadcastManager = new BroadcastManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activities_root);

        mBaseActivityCommon = new BaseActivityCommon<BaseActivity>(this);
        mBaseActivityCommon.onCreate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBaseActivityCommon.onDestroy();
        mBroadcastManager.onDestroy();
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        hideKeyboard();
        return super.onPrepareOptionsMenu(menu);
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

    @Override
    public void addRegisteredLocalReceiver(BroadcastReceiver receiver) {
        mBroadcastManager.addRegisteredLocalReceiver(receiver);
    }
}
