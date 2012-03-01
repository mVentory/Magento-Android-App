package com.mageventory;

import com.mageventory.R;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

public class BaseActivity extends Activity {

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.menu_products) {
			Intent myIntent = new Intent(getApplicationContext(), ProductListActivity.class);
			startActivityForResult(myIntent, 0);

		}
		if (item.getItemId() == R.id.menu_new) {
			Log.d("APP", "menu_create");
			Intent myIntent = new Intent(getApplicationContext(), ProductCreateActivity.class);
			startActivityForResult(myIntent, 0);
		}
		if (item.getItemId() == R.id.menu_refresh) {
			Intent myIntent = new Intent(getApplicationContext(), this.getClass());
			finish();
			startActivityForResult(myIntent, 0);

		}
		if (item.getItemId() == R.id.menu_settings) {
			Intent myIntent = new Intent(getApplicationContext(), ConfigServerActivity.class);
			startActivityForResult(myIntent, 0);
		}
		if (item.getItemId() == R.id.menu_quit) {
			finish();
		}
		return true;

	}

	public OnClickListener homelistener = new OnClickListener() {
		public void onClick(View v) {
			Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
			finish();
			startActivity(myIntent);

		}
	};

}
