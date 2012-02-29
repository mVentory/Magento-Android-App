package com.magventory;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.magventory.client.MagentoClient;
import com.magventory.model.Product;
import com.magventory.settings.Settings;
import com.magventory.R;

public class MainActivity extends Activity {
	private Settings settings;
	public static final String PREFS_NAME = "pref.dat";
	MagentoClient magentoClient;
	ProgressDialog pDialog;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		settings = new Settings(getApplicationContext());
		if (settings.hasSettings()) {
			magentoClient = new MagentoClient(getApplicationContext());
		} else {
			Toast.makeText(getApplicationContext(), "Make Config", 1000);
		}

	}

	private class DataRetrieve extends AsyncTask<MagentoClient, Integer, Object[]> {
		@Override
		protected void onPreExecute() {
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("Loading Data");
			pDialog.setIndeterminate(true);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		@Override
		protected Object[] doInBackground(MagentoClient... mc) {

			Object[] products = null;
			products = (Object[]) mc[0].execute("catalog_product.list");
			return products;
		}

		@Override
		protected void onPostExecute(Object[] result) {
			ArrayList<Product> items = new ArrayList<Product>();
			Log.d("con", "" + result.length);
			for (Object o : result) {
				Log.d("con", o.getClass().getName());
				HashMap map = (HashMap) o;
				Log.d("product", map.toString());
				items.add(new Product(map));
			}
			//prodFragment.setProductList(items);
			pDialog.dismiss();
			// end execute
		}

	}

}
