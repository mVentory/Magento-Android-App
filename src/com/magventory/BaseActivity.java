package com.magventory;

import com.magventory.R;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class BaseActivity extends Activity {
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {  
	   MenuInflater inflater = getMenuInflater();
	   inflater.inflate(R.menu.menu, menu);
	   return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	
		/*if (item.getItemId() == android.R.id.home) {
			Intent myIntent = new Intent(getApplicationContext(),MainActivity.class);
			startActivityForResult(myIntent, 0);

		}*/
		if (item.getItemId() == R.id.menu_create) {
			Log.d("APP", "menu_create");
			 Intent myIntent = new Intent(getApplicationContext(), CreateProductActivity.class);
			 startActivityForResult(myIntent, 0);
		}
		if (item.getItemId() == R.id.menu_refresh) {
			Log.d("APP", "menu_create");
		}
		if (item.getItemId() == R.id.menu_settings) {
		}
		if (item.getItemId() == R.id.menu_quit) {
			finish();
		}
		return true;
		
		}
	

}
