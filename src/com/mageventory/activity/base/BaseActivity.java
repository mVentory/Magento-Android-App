package com.mageventory.activity.base;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.mageventory.util.DefaultOptionsMenuHelper;

/* This is one of the base classes for all activities in this application. Please note that all activities should
 * extend either BaseActivity or BaseListActivity. */
public class BaseActivity extends Activity {

	private BaseActivityCommon mBaseActivityCommon;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mBaseActivityCommon = new BaseActivityCommon(this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return DefaultOptionsMenuHelper.onCreateOptionsMenu(this, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return DefaultOptionsMenuHelper.onOptionsItemSelected(this, item);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mBaseActivityCommon.onResume();
	}
	
	public Class<? extends Activity> getPreviousActivityClass()
	{
		return BaseActivityCommon.sPreviousActivityClass;
	}
}
