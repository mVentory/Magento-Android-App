package com.magventory;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.magventory.adapters.ProductListAdapter;
import com.magventory.client.MagentoClient;
import com.magventory.model.Product;
import com.magventory.settings.Settings;

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
		magentoClient = new MagentoClient(getApplicationContext());
		items = new ArrayList<Product>();
		settings = new Settings(getApplicationContext());
		m_adapter = new ProductListAdapter(getApplicationContext(), R.layout.item, items);
		if (!(settings.hasSettings())) {
			return;
		}
		ListView lv = (ListView) findViewById(R.id.ListView1);
		Log.d("resume", "" + items.size());
		lv.setAdapter(m_adapter);

	}

	public void setProductList(ArrayList<Product> prods) {
		items.clear();
		items.addAll(prods);
		m_adapter.notifyDataSetChanged();
	}
}
