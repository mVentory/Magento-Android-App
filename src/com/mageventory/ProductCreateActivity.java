package com.mageventory;

import java.util.Map;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.model.Category;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.util.DefaultOptionsMenuHelper;
import com.mageventory.util.DialogUtil;
import com.mageventory.util.DialogUtil.OnCategorySelectListener;

public class ProductCreateActivity extends BaseActivity implements MageventoryConstants, OperationObserver {

	private static final String TAG = "ProductCreateActivity";

	private ProgressDialog progressDialog;
	ArrayAdapter aa;

	private TextView productCategoryView;
	private int createProductRequestId;
	private int loadCategoriesRequestId;
	private Category productCategory;

	private OnCategorySelectListener onCategorySelectedL = new OnCategorySelectListener() {
		@Override
		public boolean onCategorySelect(Category category) {
			setProductCategory(category);
			dismissCategoryListDialog();
			return true;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_product);
		setTitle("Mventory: Create Product");

		productCategoryView = (TextView) findViewById(R.id.category);

		// app = (MyApplication) getApplication();

		aa = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_spinner_item, new String[] { "Enable",
		        "Disable" });
		aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		((Spinner) findViewById(R.id.status)).setAdapter(aa);

		findViewById(R.id.createbutton).setOnClickListener(buttonlistener);

		findViewById(R.id.select_category).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showCategoryListDialog();
			}
		});
		loadCategories();
	}

	private OnClickListener buttonlistener = new OnClickListener() {
		public void onClick(View v) {
			if (v.getId() == R.id.createbutton) {
				if (!verify_form()) {
					Toast.makeText(getApplicationContext(), "All Fields Required.", Toast.LENGTH_SHORT).show();
				} else {
					createProduct();
				}
			}
		}
	};

	public boolean verify_form() {

		String name = ((EditText) findViewById(R.id.product_name_input)).getText().toString();
		String price = ((EditText) findViewById(R.id.product_price_input)).getText().toString();
		String description = ((EditText) findViewById(R.id.description_input)).getText().toString();
		String weight = ((EditText) findViewById(R.id.weight_input)).getText().toString();
		String category = productCategoryView.getText().toString();

		if (name.equalsIgnoreCase("") || description.equalsIgnoreCase("") || price.equalsIgnoreCase("")
		        || weight.equalsIgnoreCase("") || TextUtils.isEmpty(category) || getProductCategory() == null) {
			return false;
		}

		return true;
	}
	
	private Map<String, Object> rootCategory;

	// TODO y: move this code out and create a generic and abstract async task class for loading operations
	private class LoadCategories extends AsyncTask<Object, Integer, Integer> {

		private final int LOAD = 1;
		private final int PREPARE = 2;


		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showProgressDialog(getString(R.string.loading_categories));
		}

		@Override
		protected Integer doInBackground(Object... params) {
			boolean force = false;
			if (params != null && params.length >= 1 && params[0] instanceof Boolean) {
				force = (Boolean) params[0];
			}
			ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
			if (force || resHelper.isResourceAvailable(ProductCreateActivity.this, RES_CATALOG_CATEGORY_TREE) == false) {
				// load
				loadCategoriesRequestId = resHelper.loadResource(ProductCreateActivity.this, RES_CATALOG_CATEGORY_TREE);
				return LOAD;
			} else {
				// restore
				rootCategory = resHelper.restoreResource(ProductCreateActivity.this, RES_CATALOG_CATEGORY_TREE);
				return PREPARE;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == null) {
				return;
			}
			if (result == LOAD) {

			} else if (result == PREPARE) {
				if (rootCategory != null) {
//					categoryListDialog = DialogUtil.createCategoriesDialog(ProductCreateActivity.this, rootCategory,
//					        onCategoryLongClickL);
					dismissProgressDialog();
				} else {
					// TODO y: handle
				}
			}
		}

	}

	private class CreateProduct extends AsyncTask<Integer, Integer, String> {

		@Override
		protected String doInBackground(Integer... ints) {
			String name = ((EditText) findViewById(R.id.product_name_input)).getText().toString();
			String description = ((EditText) findViewById(R.id.description_input)).getText().toString();
			String weight = ((EditText) findViewById(R.id.weight_input)).getText().toString();
			String price = ((EditText) findViewById(R.id.product_price_input)).getText().toString();
			Category cat = getProductCategory();
			int categorie_id = cat.getId();
			long status_id = ((Spinner) findViewById(R.id.status)).getSelectedItemId();
			String status = (String) aa.getItem((int) status_id);
			if ("Enable".equalsIgnoreCase(status)) {
				status_id = 1;
			} else {
				status_id = 0;
			}
			Log.d("status s", status_id + "");

			try {
				// FIXME y: apply attribute set, issue #18
				// Object[] map = (Object[]) magentoClient.execute("product_attribute_set.list");
				// String set_id = (String) ((HashMap) map[0]).get("set_id");

				final Bundle bundle = new Bundle();
				bundle.putString(MAGEKEY_PRODUCT_NAME, name);
				bundle.putString(MAGEKEY_PRODUCT_PRICE, price);
				bundle.putString(MAGEKEY_PRODUCT_WEBSITE, "1");
				bundle.putString(MAGEKEY_PRODUCT_DESCRIPTION, description);
				bundle.putString(MAGEKEY_PRODUCT_SHORT_DESCRIPTION, description);
				bundle.putString(MAGEKEY_PRODUCT_STATUS, "" + status_id);
				bundle.putString(MAGEKEY_PRODUCT_WEIGHT, weight);
				bundle.putSerializable(MAGEKEY_PRODUCT_CATEGORIES, new Object[] { String.valueOf(categorie_id) });
				createProductRequestId = ResourceServiceHelper.getInstance().loadResource(ProductCreateActivity.this,
				        RES_CATALOG_PRODUCT_CREATE, null, bundle);
				return null;
			} catch (Exception e) {
				Log.w(TAG, "" + e);
				return null;
			}
		}
	}

	// dialogs

	private Dialog categoryListDialog;

	private void showCategoryListDialog() {
		// prevent click spamming
		if (categoryListDialog != null) {
			return;
		}
		// TODO y: HUGE performance hit... the category data is being processed in the main thread...
		if (categoryListDialog == null) {
			categoryListDialog = DialogUtil.createCategoriesDialog(ProductCreateActivity.this, rootCategory,
					onCategorySelectedL, productCategory);
		}
		if (categoryListDialog != null) {
			categoryListDialog.show();
		} else {
			Toast.makeText(this, "No category data... Try refreshing.", Toast.LENGTH_LONG).show();
		}
	}

	private void dismissCategoryListDialog() {
		if (categoryListDialog == null) {
			return;
		}
		categoryListDialog.dismiss();
		categoryListDialog = null;
	}

	private void showProgressDialog(final String message) {
		if (progressDialog != null) {
			return;
		}
		progressDialog = new ProgressDialog(ProductCreateActivity.this);
		progressDialog.setMessage(message);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.show();
	}
	
	private void dismissProgressDialog() {
		if (progressDialog == null) {
			return;
		}
		progressDialog.dismiss();
		progressDialog = null;
	}

	private void createProduct() {
		showProgressDialog("Creating product");
		new CreateProduct().execute();
	}

	@Override
	protected void onResume() {
		super.onResume();
		ResourceServiceHelper.getInstance().registerLoadOperationObserver(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		ResourceServiceHelper.getInstance().unregisterLoadOperationObserver(this);
	}

	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		if (op.getException() != null) {
			Toast.makeText(getApplicationContext(), "Action Failed\n" + op.getException().getMessage(),
			        Toast.LENGTH_SHORT).show();
			dismissProgressDialog();
			return;
		}

		if (op.getOperationRequestId() == loadCategoriesRequestId) {
			loadCategories();
		} else if (op.getOperationRequestId() == createProductRequestId) {
			dismissProgressDialog();
			final String ekeyProductId = getString(R.string.ekey_product_id);
			final int productId = op.getExtras().getInt(ekeyProductId, INVALID_PRODUCT_ID);
			final Intent intent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
			intent.putExtra(ekeyProductId, productId);
			startActivity(intent);
		}
	}
	
	private void loadCategories() {
		loadCategories(false);
	}
	
	private void loadCategories(boolean force) {
		new LoadCategories().execute(force);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			loadCategories(true);
			return true;
		}
		return DefaultOptionsMenuHelper.onOptionsItemSelected(this, item);
	}
	
	private void setProductCategory(Category cat) {
		if (cat == null) {
			productCategory = null;
			productCategoryView.setText("");
			return;
		}
		productCategory = cat;
		productCategoryView.setText(cat.getName());
	}
	
	public Category getProductCategory() {
		return productCategory;
	}

}
/*
 * $newProductData = array( 'name' => 'name of product', // websites - Array of website ids to which you want to assign
 * a new product 'websites' => array(1), // array(1,2,3,...) 'short_description' => 'short description', 'description'
 * => 'description', 'status' => 1, 'weight' => 0, 'tax_class_id' => 1, 'categories' => array(3), //3 is the category id
 * 'price' => 12.05 );
 */