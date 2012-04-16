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
	
	// pseudo constants
	private String ENABLE;

	// views
	private EditText productCategoryView;
	private Spinner statusSpinner;
	
	// dialogs
	private ProgressDialog progressDialog;
	
	// adapters
	private ArrayAdapter<String> statusAdapter;
	
	// state
	private int createProductRequestId;
	private int loadCategoriesRequestId;
	private boolean isRunning;
	
	// data (?)
	private Category productCategory;
	private Map<String, Object> rootCategory;

	// listeners
	private OnCategorySelectListener onCategorySelectedL = new OnCategorySelectListener() {
		@Override
		public boolean onCategorySelect(Category category) {
			setProductCategory(category);
			dismissCategoryListDialog();
			return true;
		}
	};
	
	private OnClickListener createBtnOnClickL = new OnClickListener() {
		public void onClick(View v) {
			if (v.getId() == R.id.createbutton) {
				if (!verifyForm()) {
					Toast.makeText(getApplicationContext(), "All Fields Required.", Toast.LENGTH_SHORT).show();
				} else {
					createProduct();
				}
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// y: dialogs depend on this variable (and some of them are being created before the onResume method executes)
		isRunning = true;
		
		setContentView(R.layout.create_product);
		setTitle("Mventory: Create Product");

		// find views
		productCategoryView = (EditText) findViewById(R.id.category);
		statusSpinner = (Spinner) findViewById(R.id.status);
		
		// constants
		ENABLE = getString(R.string.enable);

		// set adapters
		statusAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[] {
				ENABLE, getString(R.string.disable) });
		statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		statusSpinner.setAdapter(statusAdapter);

		// set listeners
		findViewById(R.id.createbutton).setOnClickListener(createBtnOnClickL);
		productCategoryView.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showCategoryListDialog();
				}
			}
		});
		
		// that is convenient for the user, to reopen the catalog list dialog with a simple click
		productCategoryView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.isFocused()) {
					showCategoryListDialog();
				}
			}
		});
//		findViewById(R.id.select_category).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				showCategoryListDialog();
//			}
//		});
		loadCategories();
	}

	public boolean verifyForm() {
		final String name = ((EditText) findViewById(R.id.product_name_input)).getText().toString();
		final String price = ((EditText) findViewById(R.id.product_price_input)).getText().toString();
		final String description = ((EditText) findViewById(R.id.description_input)).getText().toString();
		final String weight = ((EditText) findViewById(R.id.weight_input)).getText().toString();
		final String category = productCategoryView.getText().toString();

		final String[] values = { name, price, description, weight, category };
		for (final String value : values) {
			if (TextUtils.isEmpty(value)) {
				return false;
			}
		}
		return true;
	}
	
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
			// TODO y: use the find method in onCreate only and assign references for all of the used views
			final String name = ((EditText) findViewById(R.id.product_name_input)).getText().toString();
			final String description = ((EditText) findViewById(R.id.description_input)).getText().toString();
			final String weight = ((EditText) findViewById(R.id.weight_input)).getText().toString();
			final String price = ((EditText) findViewById(R.id.product_price_input)).getText().toString();
			final Category cat = getProductCategory();
			final int categoryId = cat.getId();
			
			// y: commenting these out since Huss got a new to determine the status
			// final String statusVal = statusSpinner.getSelectedItem().toString();
			// int status = ENABLE.equalsIgnoreCase(statusVal) ? 1 : 0;
			
			final String quantity = ((EditText) findViewById(R.id.quantity_input)).getText().toString();

			int status = 0;
			String inventoryControl = "";
			
			if (TextUtils.isEmpty(quantity)) {
				// Inventory Control Enable and Item is not shown @ site
				inventoryControl = "0";
				status = 1;
			} else if ("0".equals(quantity)) {
				// Item is not Visible but Inventory Control Enable
				inventoryControl = "1";
				status = 2;
			} else if (TextUtils.isDigitsOnly(quantity) && Integer.parseInt(quantity) > 1) {
				// Item is Visible And Inventory Control Enable
				inventoryControl = "1";
				status = 1;
			}
			
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
				bundle.putString(MAGEKEY_PRODUCT_STATUS, "" + status);
				bundle.putString(MAGEKEY_PRODUCT_WEIGHT, weight);
				bundle.putSerializable(MAGEKEY_PRODUCT_CATEGORIES, new Object[] { String.valueOf(categoryId) });
				
				bundle.putString(MAGEKEY_PRODUCT_QUANTITY, quantity);				
				bundle.putString(MAGEKEY_PRODUCT_MANAGE_INVENTORY, inventoryControl);
				
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
		if (isRunning == false) {
			return;
		}
		if (categoryListDialog != null) {
			return;
		} else {
			// XXX y: HUGE OVERHEAD... transforming category data in the main thread
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
		if (isRunning == false) {
			return;
		}
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
		isRunning = true;
		ResourceServiceHelper.getInstance().registerLoadOperationObserver(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		isRunning = false;
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