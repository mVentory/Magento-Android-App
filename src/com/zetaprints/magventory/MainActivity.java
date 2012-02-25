package com.zetaprints.magventory;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		ListView lv = (ListView) findViewById(R.id.ListView1);
		Button createbutton = (Button) findViewById(R.id.create);
		Button refreshbutton = (Button) findViewById(R.id.refresh);

		createbutton.setOnClickListener(buttonlistener);
		refreshbutton.setOnClickListener(buttonlistener);
		items = new ArrayList<Product>();
		settings=new Settings(getApplicationContext());
		if(!(settings.hasSettings())){ return;}
		//magentoClient = new MagentoClient(getApplicationContext());
		/*magentoClient = new MagentoClient(
                "http://magento.chilerocks.org/index.php/api/xmlrpc/",
                "api-user", "123123");*/

		/*Object[] products = null;
		products = (Object[]) magentoClient.execute("catalog_product.list");
		for (Object o : products) {
			Log.i("APP_INFO", o.getClass().getName());
			HashMap map = (HashMap) o;
			items.add(new Product(map));
			Log.i("APP_INFO", "keys: " + map.keySet().toString());
			Log.i("APP_INFO", "keys: " + map.get("product_id"));
			// Object[] info=(Object[])magentoClient.execute(true);
			// Log.i("APP_INFO_I","info viene"+ map.get("product_id"));
			// Log.i("APP_INFO_I","info "+ info.getClass().getName());
		}
		 */
		m_adapter = new ProductListAdapter(this, R.layout.item, items);
		lv.setAdapter(m_adapter);

	}

	private OnClickListener buttonlistener = new OnClickListener() {
		public void onClick(View v) {
			if (v.getId() == R.id.create) {

				Intent myIntent = new Intent(getApplicationContext(),CreateProductActivity.class);
				startActivityForResult(myIntent, 0);

			}
			if (v.getId() == R.id.refresh) {
				items.clear();
				magentoClient=new MagentoClient(getApplicationContext());
				Object[] products = null;
				products = (Object[]) magentoClient.execute("catalog_product.list");
				for (Object o : products) {
					Log.i("APP_INFO", o.getClass().getName());
					HashMap map = (HashMap) o;
					items.add(new Product(map));
					Log.i("APP_INFO", "keys: " + map.keySet().toString());
					Log.i("APP_INFO", "keys: " + map.get("product_id"));
					// Object info = (Object) magentoClient.execute(true,0);
					// Log.i("APP_INFO_I", "info viene" +
					// map.get("product_id"));
					// Log.i("APP_INFO_I", "info " +
					// ((HashMap)info).keySet().toString());
				}
				m_adapter.notifyDataSetChanged();

			}

		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	         //Toast.makeText(this, "You pressed the text!", Toast.LENGTH_LONG).show();
	         Intent myIntent = new Intent(getApplicationContext(), ConfigServerActivity.class);
	         startActivityForResult(myIntent, 1);
	        					
	   
	    
	    return true;
	}
	
}
