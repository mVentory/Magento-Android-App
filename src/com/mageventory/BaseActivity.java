package com.mageventory;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.mageventory.util.DefaultOptionsMenuHelper;

public class BaseActivity extends Activity {

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return DefaultOptionsMenuHelper.onCreateOptionsMenu(this, menu);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		// we need to find a workaround for this so if a low memory event occurs
		// when user is in camera mode and the OS is finishing the app, the last
		// seen activity is started and not the MainActivity (Bogdan Petran)
		// Intent myIntent = new Intent(getApplicationContext(),
		// MainActivity.class);
		// startActivityForResult(myIntent, 0);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return DefaultOptionsMenuHelper.onOptionsItemSelected(this, item);
	}
}
