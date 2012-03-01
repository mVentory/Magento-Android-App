package com.mageventory;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.mageventory.adapters.ProductListAdapter;
import com.mageventory.client.MagentoClient;
import com.mageventory.model.Product;
import com.mageventory.settings.Settings;

public class ProductListActivity extends BaseActivity {
	private Settings settings;

	ArrayList<Product> items;
	ProductListAdapter m_adapter;
	MagentoClient magentoClient;
	ProgressDialog pDialog;

	// need a public empty constructor for framework to instantiate
	public ProductListActivity() {
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.prod_list);
		TextView title = (TextView) findViewById(R.id.textTitle);
		title.setOnClickListener(homelistener);
		items = new ArrayList<Product>();
		settings = new Settings(getApplicationContext());
		m_adapter = new ProductListAdapter(getApplicationContext(), R.layout.item, items);
		if (!(settings.hasSettings())) {
			return;
		}
		ListView lv = (ListView) findViewById(R.id.product_listview);
		Log.d("resume", "" + items.size());
		lv.setAdapter(m_adapter);
		registerForContextMenu(lv);

	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.menu_refresh:
	        DataRetrieve dr=new DataRetrieve();
	        dr.execute(new Integer[]{1});
	        return true;
	    }
	    return super.onOptionsItemSelected(item);
	}

	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.product_listview) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			menu.setHeaderTitle(items.get(info.position).getName());
			// String[] menuItems = getResources().getStringArray(R.array.menu);
			// for (int i = 0; i<menuItems.length; i++) {
			menu.add(Menu.NONE, 0, 0, "Details");
			// menu.add(Menu.NONE,1,1,"Edit");

			// }
		}
	}
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	  AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	  int menuItemIndex = item.getItemId();
	  if(menuItemIndex==0){
	  int index=info.position;
	  //Toast.makeText(this, "boorar el "+info.position, Toast.LENGTH_LONG).show();
	  Intent myIntent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
	  myIntent.putExtra("id",items.get(index).getId());
      startActivity(myIntent);  
	  }
	  return true;
	}
	public void setProductList(ArrayList<Product> prods) {
		items.clear();
		items.addAll(prods);
		m_adapter.notifyDataSetChanged();
	}
	
	private class DataRetrieve extends AsyncTask<Integer, Integer, Object[]> {
		@Override
		protected void onPreExecute() {
			pDialog = new ProgressDialog(ProductListActivity.this);
			pDialog.setMessage("Loading Products");
			pDialog.setIndeterminate(true);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		@Override
		protected Object[] doInBackground(Integer... ints) {
			magentoClient = new MagentoClient(getApplicationContext());
			Object[] products = null;
			//products = (Object[]) mc[0].execute("catalog_product.list");
			products = (Object[]) magentoClient.execute("catalog_product.list");
			return products;
		}

		@Override
		protected void onPostExecute(Object[] result) {
			items.clear();
			for (Object o : result) {
				HashMap map = (HashMap) o;
				Log.d("product", map.toString());
				items.add(new Product(map,false));
			}

			m_adapter.notifyDataSetChanged();
			pDialog.dismiss();
			// end execute
		}

	}
	
	
}
