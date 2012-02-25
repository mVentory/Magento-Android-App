package com.zetaprints.magventory;

import java.util.ArrayList;
import java.util.HashMap;

import com.zetaprints.magventory.client.MagentoClient;
import com.zetaprints.magventory.model.Category;
import com.zetaprints.magventory.model.Product;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class CreateProductActivity extends Activity {
	MagentoClient magentoClient;
	ArrayList<Category> categories;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_product);
		Button create = (Button) findViewById(R.id.createbutton);
		Spinner spin = (Spinner) findViewById(R.id.status);

		String[] status_options = { "Enable", "Disable" };
		// spin.setOnItemSelectedListener((OnItemSelectedListener) this);
		ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, status_options);
		aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spin.setAdapter(aa);
		create.setOnClickListener(buttonlistener);
		
		categories=getCategorylist();
		
		Spinner catSpin = (Spinner) findViewById(R.id.categoriesSpin);
		ArrayAdapter<Category> categories_adapter = new ArrayAdapter<Category>(this, android.R.layout.simple_spinner_item, categories);
		categories_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		catSpin.setAdapter(categories_adapter);
		
	}

	private OnClickListener buttonlistener = new OnClickListener() {
		public void onClick(View v) {
			if (v.getId() == R.id.createbutton) {

				String name = ((EditText) findViewById(R.id.product_name_input)).getText().toString();
				String price = ((EditText) findViewById(R.id.product_price_input)).getText().toString();
				String sku = ((EditText) findViewById(R.id.product_sku_input)).getText().toString();
				String description = ((EditText) findViewById(R.id.description_input)).getText().toString();
				String weight = ((EditText) findViewById(R.id.weight_input)).getText().toString();
				long categorie_id=((Spinner) findViewById(R.id.status)).getSelectedItemId();
				long status = ((Spinner) findViewById(R.id.status)).getSelectedItemId();
				magentoClient = new MagentoClient(getApplicationContext());
				Object[] map = (Object[]) magentoClient.execute("product_attribute_set.list");
				Log.d("APP", "type " + map[0].getClass().getName());
				Log.d("APP", "size " + map.length);
				Log.d("APP", "string " + map[0].toString());
				String set_id = (String) ((HashMap) map[0]).get("set_id");
				HashMap<String, Object> product_data = new HashMap<String, Object>();
				product_data.put("name", name);
				product_data.put("price", price);
				product_data.put("website", "1");
				product_data.put("description", description);
				product_data.put("short_description", description);
				product_data.put("status", status);
				product_data.put("weight", weight);
				product_data.put("categories", categorie_id);

				magentoClient.execute("catalog_product.create", new Object[] { "simple", 4, sku, product_data });

			}
		}
	};

	private static ArrayList<Category> getListFromTree(Object[] maps) {
		ArrayList<Category> catList = new ArrayList<Category>();
		if (maps == null) {
			return null;
		}
		for (Object o : maps) {
			HashMap map = (HashMap) o;
			Log.i("APP_INFO", map.toString());
			catList.add(new Category(map.get("name").toString(), map.get("category_id").toString()));
			// catList.addAll(getListFromTree((HashMap[]) map.get("children")));
		}
		return catList;
	}

	private ArrayList<Category> getCategorylist() {
		ArrayList<Category> catList = null;
		magentoClient = new MagentoClient(getApplicationContext());
		try {

			Object categories;
			categories = magentoClient.execute("catalog_category.tree");
			// for (Object o : products) {
			Log.i("APP_INFO", categories.getClass().getName());
			HashMap map = (HashMap) categories;
			// items.add(new Product(map));
			Log.i("APP_INFO", "keys: " + map.keySet().toString());
			Log.i("APP_INFO", "keys: " + map.toString());
			Log.i("APP_INFO", "keys: " + map.get("children").getClass().getName());
			Object[] child = (Object[]) map.get("children");
			for (Object o : child) {
				Log.i("APP_INFO", "keys: " + o.getClass().getName());
			}
			catList =getListFromTree(child);
		} catch (Exception e) {
			e.printStackTrace();
			Log.i("APP_INFO", "dead");
		}
		return catList;
	}

}

/*
 * $newProductData = array( 'name' => 'name of product', // websites - Array of
 * website ids to which you want to assign a new product 'websites' => array(1),
 * // array(1,2,3,...) 'short_description' => 'short description', 'description'
 * => 'description', 'status' => 1, 'weight' => 0, 'tax_class_id' => 1,
 * 'categories' => array(3), //3 is the category id 'price' => 12.05 );
 */