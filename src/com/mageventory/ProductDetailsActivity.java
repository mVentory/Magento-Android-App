package com.mageventory;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.ProgressDialog;

import android.graphics.LightingColorFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.client.MagentoClient;
import com.mageventory.model.Category;
import com.mageventory.model.Product;

public class ProductDetailsActivity extends BaseActivity {

	MagentoClient magentoClient;
	ArrayList<Category> categories;
	ProgressDialog pDialog;
	MyApplication app;
	String product_id;
	BaseActivity activity = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.product_details);

		app = (MyApplication) getApplication();
		activity = this;

		this.setTitle("Mventory: Product Details");
		((Button)findViewById(R.id.button2)).getBackground().setColorFilter(new LightingColorFilter(0x444444, 0x737575));
		((Button)findViewById(R.id.button3)).getBackground().setColorFilter(new LightingColorFilter(0x444444, 0x737575));
		/* product id */
		Bundle extras = getIntent().getExtras();
		product_id = extras.getString("id");

		/* Start loading Details */
		ProductInfoRetrieve pir = new ProductInfoRetrieve();
		pir.execute(new String[] { product_id });
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			ProductInfoRetrieve pir = new ProductInfoRetrieve();
			pir.execute(new String[] { product_id });
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private class ProductInfoRetrieve extends
			AsyncTask<String, Integer, Product> {
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

			try {
				magentoClient = app.getClient();
				Object o = magentoClient.execute("catalog_product.info",
						new Object[] { st[0] });
				HashMap map = (HashMap) o;
				Product product = new Product(map, true);
				if (!product.getMaincategory().equalsIgnoreCase("")) {
					Object c = magentoClient.execute("catalog_category.info",
							new Object[] { product.getMaincategory() });
					HashMap cat = (HashMap) c;
					product.setMaincategory_name((String) cat.get("name"));
				}
				return product;
			} catch (Exception e) {
				return null;
			}

		}

		@Override
		protected void onPostExecute(Product result) {
			if (result == null) {
				Toast.makeText(getApplicationContext(), "Request Error",
						Toast.LENGTH_SHORT).show();
				return;
			}

			((EditText) findViewById(R.id.product_name_input)).setText(result
					.getName());
			((EditText) findViewById(R.id.product_price_input)).setText(result
					.getPrice().toString());
			((EditText) findViewById(R.id.product_description_input))
					.setText(result.getDescription());
			((EditText) findViewById(R.id.product_weight_input)).setText(result
					.getWeight().toString());
			((EditText) findViewById(R.id.product_categories)).setText(result
					.getMaincategory_name());

			if (result.getStatus() == 1) {
				((EditText) findViewById(R.id.product_status))
						.setText("Enabled");
			} else {
				((EditText) findViewById(R.id.product_status))
						.setText("Disabled");
			}

			pDialog.dismiss();
			// end execute
		}
	}

}
