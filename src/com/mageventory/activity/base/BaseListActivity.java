package com.mageventory.activity.base;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/* This is one of the base classes for all activities in this application. Please note that all activities should
* extend either BaseActivity or BaseListActivity. */
public class BaseListActivity extends ListActivity {

	private BaseActivityCommon mBaseActivityCommon;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
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
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mBaseActivityCommon.hideKeyboard();
		return super.onPrepareOptionsMenu(menu);
	}
}
