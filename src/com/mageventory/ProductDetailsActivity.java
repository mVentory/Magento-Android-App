package com.mageventory;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.mageventory.client.MagentoClient;
import com.mageventory.model.Category;
import com.mageventory.model.Product;

public class ProductDetailsActivity extends BaseActivity {

	MagentoClient magentoClient;
	ArrayList<Category> categories;
	ProgressDialog pDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.product_details);
		TextView title = (TextView) findViewById(R.id.textTitle);
		title.setOnClickListener(homelistener);
		Bundle extras = getIntent().getExtras();
		String id = extras.getString("id");
		Log.d("APP", "create " + id);
		ProductInfoRetrieve pir = new ProductInfoRetrieve();
		pir.execute(new String[] { id });

	}

	private class ProductInfoRetrieve extends AsyncTask<String, Integer, Product> {
		@Override
		protected void onPreExecute() {
			pDialog = new ProgressDialog(ProductDetailsActivity.this);
			pDialog.setMessage("Loading Product");
			pDialog.setIndeterminate(true);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		@Override
		protected Product doInBackground(String... st) {
			Log.d("APP", "back" + st[0]);
			magentoClient = new MagentoClient(getApplicationContext());
			Log.d("APP", st[0]);
			Object o = magentoClient.execute("catalog_product.info", new Object[] { st[0] });
			Log.d("APP", o.getClass().getName());
			Log.d("APP", o.toString());
			HashMap map = (HashMap) o;
			Log.d("APP", map.toString());
			Product product = new Product(map, true);
			return product;
		}

		@Override
		protected void onPostExecute(Product result) {
			((EditText) findViewById(R.id.product_name_input)).setText(result.getName());
			((EditText) findViewById(R.id.product_price_input)).setText(result.getPrice().toString());
			((EditText) findViewById(R.id.product_description_input)).setText(result.getDescription());
			((EditText) findViewById(R.id.product_weight_input)).setText(result.getWeight().toString());
			if (result.getDescription().equals("1")) {
				((EditText) findViewById(R.id.product_status)).setText("Enabled");
			} else {
				((EditText) findViewById(R.id.product_status)).setText("Disabled");
			}
			((EditText) findViewById(R.id.product_categories)).setText(result.getMaincategory());

			pDialog.dismiss();
			// end execute
		}
	}

}
