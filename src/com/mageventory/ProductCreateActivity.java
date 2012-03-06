package com.mageventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.client.MagentoClient;
import com.mageventory.model.Category;

public class ProductCreateActivity extends BaseActivity {
	MagentoClient magentoClient;
	ArrayList<Category> categories;
	ProgressDialog pDialog;
	ArrayAdapter<Category> categories_adapter;
	ArrayAdapter aa;
	MyApplication app;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_product);
		this.setTitle("Mventory: Create Product");
		app = (MyApplication) getApplication();
		this.setTitle("Mventory: Create Product");
		String[] status_options = { "Enable", "Disable" };

		aa = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_spinner_item, status_options);
		aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		categories = app.getCategories();

		categories_adapter = new ArrayAdapter<Category>(getApplicationContext(), android.R.layout.simple_spinner_item,
				categories);
		categories_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		Button create = (Button) findViewById(R.id.createbutton);
		Button refresh = (Button) findViewById(R.id.refreshbutton);
		Spinner spin = (Spinner) findViewById(R.id.status);
		Spinner catSpin = (Spinner) findViewById(R.id.categoriesSpin);

		spin.setAdapter(aa);
		catSpin.setAdapter(categories_adapter);
		create.setOnClickListener(buttonlistener);
		refresh.setOnClickListener(buttonlistener);
		/* if no categories auto refresh*/
		if(categories.size()==0){
			CategoryRetrieve cr = new CategoryRetrieve();
			cr.execute(new Integer[] { 1 });
		}

	}

	private OnClickListener buttonlistener = new OnClickListener() {
		public void onClick(View v) {
			if (v.getId() == R.id.createbutton) {
				if (!verify_form()) {
					Toast.makeText(getApplicationContext(), "All Fields Required.", Toast.LENGTH_SHORT).show();
				} else {
					ProductCreate pr = new ProductCreate();
					pr.execute(new Integer[] { 1 });
				}

			}
			if (v.getId() == R.id.refreshbutton) {
				CategoryRetrieve cr = new CategoryRetrieve();
				cr.execute(new Integer[] { 1 });

			}
		}
	};

	public boolean verify_form() {

		String name = ((EditText) findViewById(R.id.product_name_input)).getText().toString();
		String price = ((EditText) findViewById(R.id.product_price_input)).getText().toString();
		String description = ((EditText) findViewById(R.id.description_input)).getText().toString();
		String weight = ((EditText) findViewById(R.id.weight_input)).getText().toString();
		Spinner catspin = (Spinner) findViewById(R.id.categoriesSpin);

		if (name.equalsIgnoreCase("") || description.equalsIgnoreCase("") || price.equalsIgnoreCase("")
				|| weight.equalsIgnoreCase("") || catspin.getSelectedItem() == null) {
			return false;
		}

		return true;

	}

	private static ArrayList<Category> getCategorylist(HashMap map) {

		Object[] children = (Object[]) map.get("children");
		if (children.length == 0)
			return new ArrayList<Category>();
		ArrayList<Category> catList = new ArrayList<Category>();
		try {

			for (Object m : children) {
				HashMap cHashmap = (HashMap) m;
				catList.add(new Category(cHashmap.get("name").toString(), cHashmap.get("category_id").toString()));
				catList.addAll(getCategorylist(cHashmap));
			}

		} catch (Exception e) {
			e.printStackTrace();
			Log.i("APP_INFO", "dead");
		}
		Log.i("APP_INFO", "" + catList.size());
		return catList;
	}

	private class CategoryRetrieve extends AsyncTask<Integer, Integer, ArrayList<Category>> {
		@Override
		protected void onPreExecute() {
			pDialog = new ProgressDialog(ProductCreateActivity.this);
			pDialog.setMessage("Loading Categories");
			pDialog.setIndeterminate(true);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		@Override
		protected ArrayList<Category> doInBackground(Integer... ints) {
			try {
				magentoClient = app.getClient();
				Object categories;
				categories = magentoClient.execute("catalog_category.tree");
				HashMap map = (HashMap) categories;
				return getCategorylist(map);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

		}

		@Override
		protected void onPostExecute(ArrayList<Category> result) {
			pDialog.dismiss();
			if (result != null) {
				app.setCategories(result);
				categories.clear();
				categories.addAll(result);
				categories_adapter.notifyDataSetChanged();
			} else {
				Toast.makeText(getApplicationContext(), "Request Error", Toast.LENGTH_SHORT).show();
			}

			// end execute
		}

	}

	private class ProductCreate extends AsyncTask<Integer, Integer, String> {
		@Override
		protected void onPreExecute() {
			pDialog = new ProgressDialog(ProductCreateActivity.this);
			pDialog.setMessage("Uploading Product");
			pDialog.setIndeterminate(true);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		@Override
		protected String doInBackground(Integer... ints) {
			String name = ((EditText) findViewById(R.id.product_name_input)).getText().toString();
			// String sku = ((EditText)
			// findViewById(R.id.product_sku_input)).getText().toString();
			Random random = new Random();
			String sku = "" + random.nextInt(999999);
			String description = ((EditText) findViewById(R.id.description_input)).getText().toString();
			String weight = ((EditText) findViewById(R.id.weight_input)).getText().toString();
			String price = ((EditText) findViewById(R.id.product_price_input)).getText().toString();
			Category cat=(Category)((Spinner) findViewById(R.id.categoriesSpin)).getSelectedItem();
			int categorie_id=cat.getId();
			long status_id = ((Spinner) findViewById(R.id.status)).getSelectedItemId();
			String status = (String) aa.getItem((int) status_id);
			Log.d("status", status + "");
			if (status.equalsIgnoreCase("Enable")) {
				status_id = 1;
			} else {
				status_id = 0;
			}
			Log.d("status s", status_id + "");

			try {
				magentoClient = app.getClient();
				Object[] map = (Object[]) magentoClient.execute("product_attribute_set.list");
				String set_id = (String) ((HashMap) map[0]).get("set_id");

				HashMap<String, Object> product_data = new HashMap<String, Object>();

				product_data.put("name", name);
				product_data.put("price", price);
				product_data.put("website", "1");
				product_data.put("description", description);
				product_data.put("short_description", description);
				product_data.put("status", "" + status_id);
				product_data.put("weight", weight);
				//Log.d("APP",categorie_id+"");
				product_data.put("categories", new Object[] { categorie_id+""});
				//product_data.put("categories", new Object[] { "5"});
				Object response = magentoClient.execute("catalog_product.create", new Object[] { "simple", 4, sku,
						product_data });

				return (String) response;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(String result) {
			pDialog.dismiss();
			try {
				Integer.parseInt(result);
				Intent myIntent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
				myIntent.putExtra("id", result);
				startActivity(myIntent);
			} catch (Exception e) {
				Toast.makeText(getApplicationContext(), "Action Failed", Toast.LENGTH_SHORT).show();
			}
			// end execute
		}

	}

}
/*
 * $newProductData = array( 'name' => 'name of product', // websites - Array of
 * website ids to which you want to assign a new product 'websites' => array(1),
 * // array(1,2,3,...) 'short_description' => 'short description', 'description'
 * => 'description', 'status' => 1, 'weight' => 0, 'tax_class_id' => 1,
 * 'categories' => array(3), //3 is the category id 'price' => 12.05 );
 */