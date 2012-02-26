package com.zetaprints.magventory;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.zetaprints.magventory.adapters.ProductListAdapter;
import com.zetaprints.magventory.client.MagentoClient;
import com.zetaprints.magventory.model.Product;
import com.zetaprints.magventory.settings.Settings;

public class MainActivity extends FragmentActivity {
	private Settings settings;
	public static final String PREFS_NAME = "pref.dat";
	ArrayList<Product> items;
	ProductListAdapter m_adapter;
	MagentoClient magentoClient;
	ProgressDialog pDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		ListView lv = (ListView) findViewById(R.id.ListView1);
		items = new ArrayList<Product>();
		settings = new Settings(getApplicationContext());
		
		if (!(settings.hasSettings())) {
			return;
		}

		m_adapter = new ProductListAdapter(this, R.layout.item, items);
		lv.setAdapter(m_adapter);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_create) {
			Intent myIntent = new Intent(getApplicationContext(), CreateProductActivity.class);
			startActivityForResult(myIntent, 0);
		}
		if (item.getItemId() == R.id.menu_refresh) {
			magentoClient = new MagentoClient(getApplicationContext());
			DataRetrieve dr = new DataRetrieve();
			dr.execute(new MagentoClient[]{magentoClient});
		}
		if (item.getItemId() == R.id.menu_settings) {
			Intent myIntent = new Intent(getApplicationContext(), ConfigServerActivity.class);
			startActivityForResult(myIntent, 1);
		}
		if (item.getItemId() == R.id.menu_quit) {
			finish();
		}

		return true;
	}

	private class DataRetrieve extends AsyncTask<MagentoClient, Integer, Object[]> {
		@Override
		protected void onPreExecute() {
			 pDialog=new ProgressDialog(MainActivity.this);
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
			items.clear();
			for (Object o : result) {
				HashMap map = (HashMap) o;
				items.add(new Product(map));

			}
			m_adapter.notifyDataSetChanged();
			pDialog.dismiss();
			// end execute
		}

	}
}
