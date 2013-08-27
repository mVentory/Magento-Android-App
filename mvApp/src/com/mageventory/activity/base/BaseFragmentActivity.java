package com.mageventory.activity.base;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

/* This is one of the base classes for all activities in this application. Please note that all activities should
 * extend either BaseActivity, BaseFragmentActivity or BaseListActivity. */
public class BaseFragmentActivity extends FragmentActivity {

	private BaseActivityCommon mBaseActivityCommon;
    private boolean mActivityAlive;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mActivityAlive = true;
		mBaseActivityCommon = new BaseActivityCommon(this);
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
        mActivityAlive = false;
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
}
