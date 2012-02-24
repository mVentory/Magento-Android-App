package com.zetaprints.magventory;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CreateProductActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_product);
		//Button create = (Button) findViewById(R.id.createbutton);
		/*Spinner spin = (Spinner) findViewById(R.id.status);

		String[] status_options = { "Enable", "Disable" };
		// spin.setOnItemSelectedListener((OnItemSelectedListener) this);
		ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, status_options);
		aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spin.setAdapter(aa);*/
		//create.setOnClickListener(buttonlistener);
	}

	private OnClickListener buttonlistener = new OnClickListener() {
		public void onClick(View v) {
			if (v.getId() == R.id.createbutton) {
/*
				String name = ((EditText) findViewById(R.id.product_name_input)).getText().toString();
				String price = ((EditText) findViewById(R.id.product_price_input)).getText().toString();
				String sku = ((EditText) findViewById(R.id.product_sku_input)).getText().toString();
				String description = ((EditText) findViewById(R.id.description_input)).getText().toString();
				//String weight = ((EditText) findViewById(R.id.weight_input)).getText().toString();
				//long status= ((Spinner)findViewById(R.id.status)).getSelectedItemId();
				MagentoClient magentoClient = new MagentoClient(getApplicationContext());
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
				//product_data.put("status", status);
				//product_data.put("weight", weight);
				product_data.put("categories", 0);

				magentoClient.execute("catalog_product.create", new Object[] { "simple", 4, sku, product_data });
				*/
			}
		}
	};

}

/*
 * $newProductData = array( 'name' => 'name of product', // websites - Array of
 * website ids to which you want to assign a new product 'websites' => array(1),
 * // array(1,2,3,...) 'short_description' => 'short description', 'description'
 * => 'description', 'status' => 1, 'weight' => 0, 'tax_class_id' => 1,
 * 'categories' => array(3), //3 is the category id 'price' => 12.05 );
 */