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
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

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
	MyApplication app;
	String category_id=null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.prod_list);

		app = (MyApplication) getApplication();
		this.setTitle("Mventory: Products");

		ListView lv = (ListView) findViewById(R.id.product_listview);

		items = new ArrayList<Product>();

		settings = new Settings(getApplicationContext());
		m_adapter = new ProductListAdapter(getApplicationContext(), R.layout.item, items);
		category_id="";
		if(getIntent().hasExtra("id") ||getIntent().hasExtra("name") ){
		Bundle extras = getIntent().getExtras();
		category_id = extras.getString("id");
		this.setTitle("Mventory: Products "+extras.getString("name"));
		}
		if (!(settings.hasSettings())) {
			return;
		}

		lv.setAdapter(m_adapter);
		lv.setOnItemClickListener(myOnItemClickListener);
		
		/*set empty view*/
		TextView empty= (TextView) findViewById(R.id.empty);
		empty.setText("No Products");
		lv.setEmptyView(empty);
		
		registerForContextMenu(lv);
		/*automatic refresh if set is dirty*/
		if(app.isDirty() || !category_id.equals("")){
			DataRetrieve dr = new DataRetrieve();
			dr.execute(new Integer[] { 1 });
		}
		else {
			items.clear();
			items.addAll(app.getProducts());
			m_adapter.notifyDataSetChanged();
		}
		


	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			DataRetrieve dr = new DataRetrieve();
			dr.execute(new Integer[] { 1 });
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.product_listview) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			menu.setHeaderTitle(items.get(info.position).getName());

			menu.add(Menu.NONE, 0, 0, "Details");
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int menuItemIndex = item.getItemId();
		if (menuItemIndex == 0) {
			int index = info.position;

			Intent myIntent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
			myIntent.putExtra("id", items.get(index).getId());
			startActivity(myIntent);
		}
		return true;
	}
	public OnItemClickListener myOnItemClickListener= new OnItemClickListener() {

		public void onItemClick(AdapterView<?> arg0, View v, int pos,
				long arg3) {
			Intent myIntent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
			myIntent.putExtra("id",items.get(pos).getId());
			startActivity(myIntent);
		}
	};

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
			magentoClient = app.getClient();
			Object[] products = null;

			
			try {
				if(category_id.equals("")){
				products = (Object[]) magentoClient.execute("catalog_product.list");
				return products;
				}
				else{
				HashMap filter= new HashMap();
				filter.put("category_ids",category_id);
				products = (Object[]) magentoClient.execute("catalog_category.assignedProducts",filter);
				ArrayList<Object> categoryProducts=new ArrayList<Object>();
				for(Object product :products){
					HashMap pinfo=(HashMap)product;
					String pid= (String) pinfo.get("product_id");
					Object o=magentoClient.execute("catalog_product.info",new Object[] { pid });
					categoryProducts.add(o);
				}
				
				return categoryProducts.toArray();
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(Object[] result) {
			if (result != null) {
				items.clear();
				for (Object o : result) {
					HashMap map = (HashMap) o;
					Log.d("product", map.toString());
					items.add(new Product(map, false));
				}
				app.setProducts(items);
				app.setDirty(false);
				m_adapter.notifyDataSetChanged();
			} else {
				Toast.makeText(ProductListActivity.this, "Request Error", Toast.LENGTH_SHORT).show();
			}
			pDialog.dismiss();

		}
	}
	}



