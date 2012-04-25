package com.mageventory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
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

	// pseudo constants
	private String ENABLE;

	// views
	private View atrListWrapperV;
	private ViewGroup atrListV;
	private EditText productCategoryView;
	private EditText attrSetView;
	private Spinner statusSpinner;

	// dialogs
	private ProgressDialog progressDialog;
	private Dialog attrSetListDialog;

	// adapters
	private ArrayAdapter<String> statusAdapter;

	// state
	private int createProductRequestId;
	private int loadCategoriesRequestId;
	private int loadAttributeSetsRequestId;
	private int loadAttributeListRequestId;
	private boolean isRunning;

	// data (?) XXX
	private int attrSetId;
	private Category productCategory;
	private Map<String, Object> rootCategory;
	private List<Map<String, Object>> attrSets;

	private final List<EditText> atrEditFields = new LinkedList<EditText>();
	private final List<Spinner> atrSpinnerFields = new LinkedList<Spinner>();

	// listeners
	private OnCategorySelectListener onCategorySelectedL = new OnCategorySelectListener() {
		@Override
		public boolean onCategorySelect(Category category) {
			setProductCategory(category);
			dismissCategoryListDialog();
			return true;
		}
	};

	// ---
	private LayoutInflater inflater;

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
	
	private OnLongClickListener scanSKUOnClickL = new OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			Intent scanInt = new Intent("com.google.zxing.client.android.SCAN");
			scanInt.putExtra("SCAN_MODE", "QR_CODE_MODE");
			startActivityForResult(scanInt,SCAN_QR_CODE);
			return true;
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
		attrSetView = (EditText) findViewById(R.id.attr_set);
		statusSpinner = (Spinner) findViewById(R.id.status);
		atrListWrapperV = findViewById(R.id.attr_list_wrapper);
		atrListV = (ViewGroup) findViewById(R.id.attr_list);

		// init other fields
		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

		// constants
		ENABLE = getString(R.string.enable);

		// set adapters
		statusAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[] { ENABLE,
		        getString(R.string.disable) });
		statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		statusSpinner.setAdapter(statusAdapter);

		// set listeners
		findViewById(R.id.createbutton).setOnClickListener(createBtnOnClickL);
		productCategoryView.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				Log.d("tmp", "productCategoryView.onFocusChange, hasFocus=" + hasFocus);
				if (hasFocus) {
					showCategoryListDialog();
				}
			}
		});

		// that is convenient for the user, to reopen the catalog list dialog with a simple click
		productCategoryView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("tmp", "productCategoryView.onClick, focused=" + v.isFocused());
				if (v.isFocused()) {
					showCategoryListDialog();
				}
			}
		});

		attrSetView.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				Log.d("tmp", "attrSetView.onFocusChange, hasFocus=" + hasFocus);
				if (hasFocus) {
					showAttrSetListDialog();
				}
			}
		});
		attrSetView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("tmp", "attrSetView.onClick, focused=" + v.isFocused());
				if (v.isFocused()) {
					showAttrSetListDialog();
				}
			}
		});

		EditText skuInput = (EditText) findViewById(R.id.product_sku_input);		
		skuInput.setOnLongClickListener(scanSKUOnClickL);
		
		loadCategories();
		
		
		// Get the Extra "PASSING SKU -- SHOW IT"
		if(getIntent().hasExtra(PASSING_SKU))
		{
			boolean isSKU = getIntent().getBooleanExtra(PASSING_SKU, false);		
			if(isSKU)
			{
				skuInput.setText(getIntent().getStringExtra(MAGEKEY_PRODUCT_SKU));
			}		
		}
		
		// Set the Action for Quick Sell Button
		Button quickSell = (Button) findViewById(R.id.createAndSellButton);
		quickSell.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Collect Product Information and Pass it to Express Sell
				Intent newIntent = new Intent(getApplicationContext(),ExpressSellActivity.class);
				newIntent.putExtra(MAGEKEY_PRODUCT_NAME, ((EditText)findViewById(R.id.product_name_input)).getText().toString());
				newIntent.putExtra(MAGEKEY_PRODUCT_PRICE, ((EditText)findViewById(R.id.product_price_input)).getText().toString());
				newIntent.putExtra(MAGEKEY_PRODUCT_SKU, ((EditText)findViewById(R.id.product_sku_input)).getText().toString());
				newIntent.putExtra(MAGEKEY_PRODUCT_QUANTITY, ((EditText)findViewById(R.id.quantity_input)).getText().toString());
				newIntent.putExtra(MAGEKEY_PRODUCT_DESCRIPTION, ((EditText)findViewById(R.id.description_input)).getText().toString());
				newIntent.putExtra(MAGEKEY_PRODUCT_WEIGHT, ((EditText)findViewById(R.id.weight_input)).getText().toString());
				final Category cat = getProductCategory();
				newIntent.putExtra(MAGEKEY_PRODUCT_CATEGORIES,  new Object[] { String.valueOf(cat.getId()) });
				
							
				startActivity(newIntent);
				
			}
		});
	}

	public boolean verifyForm() {
		final String name = ((EditText) findViewById(R.id.product_name_input)).getText().toString();
		final String price = ((EditText) findViewById(R.id.product_price_input)).getText().toString();
		final String description = ((EditText) findViewById(R.id.description_input)).getText().toString();
		final String weight = ((EditText) findViewById(R.id.weight_input)).getText().toString();
		final String category = productCategoryView.getText().toString();
		final String atrSet = attrSetView.getText().toString();

		final String[] values = { name, price, description, weight, category, atrSet };
		for (final String value : values) {
			if (TextUtils.isEmpty(value)) {
				return false;
			}
		}

		for (final EditText editField : atrEditFields) {
			if (TextUtils.isEmpty("" + editField.getText()) && editField.getTag(R.id.tkey_atr_required) == Boolean.TRUE) {
				return false;
			}
		}

		for (final Spinner spinnerField : atrSpinnerFields) {
			if (spinnerField.getSelectedItem() == null && spinnerField.getTag(R.id.tkey_atr_required) == Boolean.TRUE) {
				return false;
			}
		}

		return true;
	}

	private class LoadProductAttributeList extends AsyncTask<Object, Integer, Integer> {

		private final int LOAD = 1;
		private final int RESTORE = 2;
		private final int FAIL = 3;

		private List<Map<String, Object>> atrListData;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showProgressDialog("Loading product attribute list..."); // y TODO: extract string
		}

		@Override
		protected Integer doInBackground(Object... arg0) {
			try {
				final int setId = (Integer) arg0[0];
				if (setId == INVALID_ATTRIBUTE_SET_ID) {
					return FAIL;
				}
				final String[] params = new String[] { String.valueOf(setId) };
				final ResourceServiceHelper helper = ResourceServiceHelper.getInstance();
				if (helper.isResourceAvailable(getApplicationContext(), RES_PRODUCT_ATTRIBUTE_LIST, params) == false) {
					// load
					loadAttributeListRequestId = helper.loadResource(getApplicationContext(),
					        RES_PRODUCT_ATTRIBUTE_LIST, params);
					return LOAD;
				} else {
					// restore
					atrListData = helper.restoreResource(getApplicationContext(), RES_PRODUCT_ATTRIBUTE_LIST, params);
					return RESTORE;
				}
			} catch (Throwable e) {
				return FAIL;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			if (result == FAIL) {
				// TODO y: bad error handling....
				Toast.makeText(getApplicationContext(), "Problem occured while loading product attribute list...",
				        Toast.LENGTH_LONG).show();
			}
			if (result == FAIL || result == RESTORE) {
				dismissProgressDialog();
			}
			if (result == RESTORE) {
				buildAtrList(atrListData);
			}
		}

	}

	// TODO y: move this code out and create a generic and abstract async task class for loading operations
	private class LoadCategoriesAndAttrSets extends AsyncTask<Object, Integer, Integer> {

		private final int LOAD = 1;
		private final int PREPARE = 2;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showProgressDialog(getString(R.string.loading_categories_and_attr_sets));
		}

		@Override
		protected Integer doInBackground(Object... params) {
			boolean force = false;
			if (params != null && params.length >= 1 && params[0] instanceof Boolean) {
				force = (Boolean) params[0];
			}
			ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
			if (force
			        || resHelper.isResourceAvailable(getApplicationContext(), RES_CATALOG_CATEGORY_TREE) == false
			        || resHelper.isResourceAvailable(getApplicationContext(), RES_CATALOG_PRODUCT_ATTRIBUTE_SET_LIST) == false) {

				// load
				loadCategoriesRequestId = resHelper.loadResource(getApplicationContext(), RES_CATALOG_CATEGORY_TREE);
				loadAttributeSetsRequestId = resHelper.loadResource(getApplicationContext(),
				        RES_CATALOG_PRODUCT_ATTRIBUTE_SET_LIST);
				return LOAD;
			} else {

				// restore
				rootCategory = resHelper.restoreResource(ProductCreateActivity.this, RES_CATALOG_CATEGORY_TREE);
				attrSets = resHelper.restoreResource(getApplicationContext(), RES_CATALOG_PRODUCT_ATTRIBUTE_SET_LIST);
				return PREPARE;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == LOAD) {

			} else if (result == PREPARE) {
				dismissProgressDialog();

				attrSetView.setText("");
				removeAttributeListV();
				
				if (rootCategory != null && attrSets != null) {
				} else {
					// TODO y: handle
					Toast.makeText(ProductCreateActivity.this, "Error...", Toast.LENGTH_LONG).show();
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
			final String sku = ((EditText) findViewById(R.id.product_sku_input)).getText().toString();
			
			String quantity = ((EditText) findViewById(R.id.quantity_input)).getText().toString();

			int status = 1;					// Must be Always 1 - to be able to sell it
			String inventoryControl = "";
			String isInStock = "1";			// Any Product is Always in Stock
			
			if (TextUtils.isEmpty(quantity)) {
				// Inventory Control Enabled and Item is not shown @ site
				inventoryControl = "0";
				quantity = "-1000000"; // Set Quantity to -1000000
			} else if ("0".equals(quantity)) {
				// Item is not Visible but Inventory Control Enabled
				inventoryControl = "1";				
			} else if (TextUtils.isDigitsOnly(quantity) && Integer.parseInt(quantity) >= 1) {
				// Item is Visible And Inventory Control Enable
				inventoryControl = "1";
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
				bundle.putString(MAGEKEY_PRODUCT_SKU, sku);
				bundle.putSerializable(MAGEKEY_PRODUCT_CATEGORIES, new Object[] { String.valueOf(categoryId) });

				bundle.putString(MAGEKEY_PRODUCT_QUANTITY, quantity);
				bundle.putString(MAGEKEY_PRODUCT_MANAGE_INVENTORY, inventoryControl);
				bundle.putString(MAGEKEY_PRODUCT_IS_IN_STOCK, isInStock);

				// bundle attributes
				final HashMap<String, Object> atrs = new HashMap<String, Object>();
				for (EditText editField : atrEditFields) {
					final String code = editField.getTag(R.id.tkey_atr_code).toString();
					if (TextUtils.isEmpty(code)) {
						continue;
					}
					final String type = "" + editField.getTag(R.id.tkey_atr_type);
					if ("multiselect".equalsIgnoreCase(type)) { // TODO y: define as constant
						@SuppressWarnings("unchecked")
						final Set<String> selectedSet = (Set<String>) editField.getTag(R.id.tkey_atr_selected);
						final String[] selected;
						if (selectedSet != null) {
							selected = new String[selectedSet.size()];
							int i = 0;
							for (String e : selectedSet) {
								selected[i++] = e;
							}
						} else {
							selected = new String[0];
						}
						atrs.put(code, selected);
					} else {
						atrs.put(code, editField.getText().toString());
					}
				}
				for (Spinner spinnerField : atrSpinnerFields) {
					final String code = spinnerField.getTag(R.id.tkey_atr_code).toString();
					if (TextUtils.isEmpty(code)) {
						continue;
					}
					@SuppressWarnings("unchecked")
					final HashMap<String, String> options = (HashMap<String, String>) spinnerField
					        .getTag(R.id.tkey_atr_options);
					if (options == null || options.isEmpty()) {
						continue;
					}
					final Object selected = spinnerField.getSelectedItem();
					if (selected == null) {
						continue;
					}
					final String selAsStr = selected.toString();
					if (options.containsKey(selAsStr) == false) {
						continue;
					}
					atrs.put(code, options.get(selAsStr));
				}

				bundle.putInt(EKEY_PRODUCT_ATTRIBUTE_SET_ID, attrSetId);
				bundle.putSerializable(EKEY_PRODUCT_ATTRIBUTE_VALUES, atrs);

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
		dismissCategoryListDialog();
		
		if (rootCategory == null || rootCategory.isEmpty()) {
			Toast.makeText(this, "No category data... Try refreshing.", Toast.LENGTH_LONG).show();
		}
		
		// XXX y: HUGE OVERHEAD... transforming category data in the main thread
		categoryListDialog = DialogUtil.createCategoriesDialog(ProductCreateActivity.this, rootCategory,
				onCategorySelectedL, productCategory);
		if (categoryListDialog != null) {
			categoryListDialog.show();
		} else {
			// y: handle
		}
	}

	private void dismissCategoryListDialog() {
		if (categoryListDialog == null) {
			return;
		}
		categoryListDialog.dismiss();
		categoryListDialog = null;
	}

	private void showAttrSetListDialog() {
		if (isRunning == false) {
			return;
		}
		dismissAttrSetListDialog();
		if (attrSets == null || attrSets.isEmpty()) {
			Toast.makeText(this, "No attribute data... Try refreshing.", Toast.LENGTH_LONG).show();
			return;
		}
		attrSetListDialog = DialogUtil.createListDialog(this, "Attribute sets", attrSets,
		        android.R.layout.simple_list_item_1, new String[] { MAGEKEY_ATTRIBUTE_SET_NAME },
		        new int[] { android.R.id.text1 }, new OnItemClickListener() {
			        @Override
			        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				        final Object item = arg0.getAdapter().getItem(arg2);
				        @SuppressWarnings("unchecked")
				        final Map<String, Object> itemData = (Map<String, Object>) item;
				        try {
					        attrSetId = Integer.parseInt(itemData.get(MAGEKEY_ATTRIBUTE_SET_ID).toString());
				        } catch (Throwable e) {
					        attrSetId = INVALID_ATTRIBUTE_SET_ID;
				        }

				        final String atrSetName = "" + itemData.get(MAGEKEY_ATTRIBUTE_SET_NAME);
				        dismissAttrSetListDialog();
				        showProgressDialog("Loading attributes for set \"" + atrSetName + "\"...");
				        
				        attrSetView.setText(atrSetName);
				        loadProductAtrList(attrSetId);
			        }
		        });
		attrSetListDialog.show();
	}

	private void loadProductAtrList(int atrSetId) {
		loadProductAtrList(atrSetId, false);
	}

	private void loadProductAtrList(int atrSetId, boolean forceRefresh) {
		new LoadProductAttributeList().execute(atrSetId);
	}

	private void dismissAttrSetListDialog() {
		if (isRunning == false) {
			return;
		}
		if (attrSetListDialog == null) {
			return;
		}
		attrSetListDialog.dismiss();
		attrSetListDialog = null;
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

	private boolean isCatOpComplete = false;
	private boolean isAtrOpComplete = false;

	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		if (op.getException() != null) {
			Toast.makeText(getApplicationContext(), "Action Failed\n" + op.getException().getMessage(),
			        Toast.LENGTH_SHORT).show();
			dismissProgressDialog();
			return;
		}

		if (op.getOperationRequestId() == loadCategoriesRequestId) {
			isCatOpComplete = true;
		} else if (op.getOperationRequestId() == loadAttributeSetsRequestId) {
			isAtrOpComplete = true;
		} else if (op.getOperationRequestId() == loadAttributeListRequestId) {
			// dismissAttrSetListDialog();
			loadProductAtrList(attrSetId);
		} else if (op.getOperationRequestId() == createProductRequestId) {
			dismissProgressDialog();
			final String ekeyProductId = getString(R.string.ekey_product_id);
			final int productId = op.getExtras().getInt(ekeyProductId, INVALID_PRODUCT_ID);
			final Intent intent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
			intent.putExtra(ekeyProductId, productId);
			startActivity(intent);
		}

		if (isCatOpComplete && isAtrOpComplete) {
			loadCategories();
			isCatOpComplete = isAtrOpComplete = false;
		}
	}

	// y TODO: rename method to loadCatsAndAttrSets or smth
	private void loadCategories() {
		loadCategories(false);
	}

	private void loadCategories(boolean force) {
		new LoadCategoriesAndAttrSets().execute(force);
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

	
	
	/**
	 * Get the Scanned Code 
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		
		if(requestCode == SCAN_QR_CODE)
		{
			if (resultCode == RESULT_OK) {
	            String contents = intent.getStringExtra("SCAN_RESULT");
	            String [] urlData = contents.split("/");
	            if(urlData.length > 0) {
	            	EditText skuInput = (EditText) findViewById(R.id.product_sku_input);
		            skuInput.setText(urlData[urlData.length - 1]);
		            skuInput.requestFocus();	
	            	
	            } else {
	            	Toast.makeText(getApplicationContext(), "Not Valid", Toast.LENGTH_SHORT).show();
	            	return;
	            }
	        } else if (resultCode == RESULT_CANCELED) {
	            // Do Nothing
	        }
		}
	}
		
	private void buildAtrList(List<Map<String, Object>> atrList) {
		removeAttributeListV();

		if (atrList == null || atrList.isEmpty()) {
			return;
		}

		showAttributeList();
		for (Map<String, Object> atr : atrList) {
			View edit = newAtrEditView(atr);
			atrListV.addView(edit);
		}
	}

	private void clearAttributeList() {
		atrEditFields.clear();
		atrSpinnerFields.clear();
	}

	private void removeAttributeListV() {
		clearAttributeList();
		atrListWrapperV.setVisibility(View.GONE);
		atrListV.removeAllViews();
	}

	private void showAttributeList() {
		atrListWrapperV.setVisibility(View.VISIBLE);
	}

	@SuppressWarnings("unchecked")
	private View newAtrEditView(Map<String, Object> atrData) {
		final String code = atrData.get(MAGEKEY_ATTRIBUTE_CODE).toString();
		final String name = atrData.get(MAGEKEY_ATTRIBUTE_INAME).toString();

		if (TextUtils.isEmpty(name)) {
			// y: ?
			throw new RuntimeException("bad data...");
		}

		final String type = "" + atrData.get(MAGEKEY_ATTRIBUTE_TYPE);
		Map<String, String> options = null;
		List<String> labels = null;

		if ("boolean".equalsIgnoreCase(type) || "select".equalsIgnoreCase(type) || "multiselect".equalsIgnoreCase(type)
		        || atrData.containsKey(MAGEKEY_ATTRIBUTE_IOPTIONS)) {
			final List<Object> tmp = (List<Object>) atrData.get(MAGEKEY_ATTRIBUTE_IOPTIONS);
			if (tmp != null) {
				options = new HashMap<String, String>(tmp.size());
				labels = new ArrayList<String>(tmp.size());
				for (final Object obj : tmp) {
					if (obj == null) {
						continue;
					} else if (obj instanceof Map) {
						final Map<String, Object> asMap = (Map<String, Object>) obj;
						final Object label = asMap.get("label");
						final Object value = asMap.get("value");
						if (label != null && value != null) {
							final String labelAsStr = label.toString();
							final String valueAsStr = value.toString();
							if (labelAsStr.length() > 0 && valueAsStr.length() > 0) {
								options.put(labelAsStr, valueAsStr);
								labels.add(labelAsStr);
							}
						}
					}
				}
			}

			// handle boolean and select fields
			if (options != null && options.isEmpty() == false && "multiselect".equalsIgnoreCase(type) == false) {
				final View v = inflater.inflate(R.layout.product_attribute_spinner, null);
				final Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
				final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				        android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, labels);
				spinner.setAdapter(adapter);
				spinner.setTag(R.id.tkey_atr_code, code);
				spinner.setTag(R.id.tkey_atr_type, type);
				spinner.setTag(R.id.tkey_atr_options, options);
				boolean isRequired;
				if (atrData.containsKey(MAGEKEY_ATTRIBUTE_REQUIRED)
				        && "1".equals(atrData.get(MAGEKEY_ATTRIBUTE_REQUIRED).toString())) {
					spinner.setTag(R.id.tkey_atr_required, Boolean.TRUE);
					isRequired = true;
				} else {
					spinner.setTag(R.id.tkey_atr_required, Boolean.FALSE);
					isRequired = false;
				}

				final TextView label = (TextView) v.findViewById(R.id.label);
				label.setText(name + (isRequired ? " (required)" : ""));
				atrSpinnerFields.add(spinner);
				return v;
			}
		}
		
		// TODO y: a lot of repetitions... move the common logic out

		// handle text fields, multiselect (special case text field), date (another special case), null, etc...

		final View v = inflater.inflate(R.layout.product_attribute_edit, null);
		EditText edit = (EditText) v.findViewById(R.id.edit);
		
		if ("price".equalsIgnoreCase(type)) {
			edit.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
		} else if ("multiselect".equalsIgnoreCase(type)) {
			if (options != null && options.isEmpty() == false) {
				final Map<String, String> finOptions = options;
				final List<String> finLabels = labels;
				edit.setOnFocusChangeListener(new OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if (hasFocus) {
							showMultiselectDialog((EditText) v, finOptions, finLabels);
						}
					}
				});
				edit.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (v.isFocused()) {
							showMultiselectDialog((EditText) v, finOptions, finLabels);
						}
					}
				});
			}
		} else if ("date".equalsIgnoreCase(type)) {
			edit.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						showDatepickerDialog((EditText) v);
					}
				}
			});
			edit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (v.isFocused()) {
						showDatepickerDialog((EditText) v);
					}
				}
			});
		}

		boolean isRequired;
		if (atrData.containsKey(MAGEKEY_ATTRIBUTE_REQUIRED)
		        && "1".equals(atrData.get(MAGEKEY_ATTRIBUTE_REQUIRED).toString())) {
			edit.setTag(R.id.tkey_atr_required, Boolean.TRUE);
			isRequired = true;
		} else {
			edit.setTag(R.id.tkey_atr_required, Boolean.FALSE);
			isRequired = false;
		}
		edit.setHint(name);
		edit.setTag(R.id.tkey_atr_code, code);
		edit.setTag(R.id.tkey_atr_type, type);

		atrEditFields.add(edit);

		TextView label = (TextView) v.findViewById(R.id.label);
		label.setText(name + (isRequired ? " (required)" : ""));
		return v;
	}
	
	private void showDatepickerDialog(final EditText v) {
		final OnDateSetListener onDateSetL = new OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				monthOfYear += 1; // because it's from 0 to 11 for compatibility reasons
				final String date = "" + monthOfYear + "/" + dayOfMonth + "/" + year;
				v.setText(date);
			}
		};
		
		final Calendar c = Calendar.getInstance();

		// parse date if such is present
		try {
			final SimpleDateFormat f = new SimpleDateFormat("M/d/y");
			final Date d = f.parse(v.getText().toString());
			c.setTime(d);
		} catch (Throwable ignored) {
		}
		
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);
		
		final Dialog d = new DatePickerDialog(this, onDateSetL, year, month, day);
		d.show();
	}

	private void showMultiselectDialog(final EditText v, final Map<String, String> options, final List<String> labels) {
		final CharSequence[] items = new CharSequence[labels.size()];
		for (int i = 0; i < labels.size(); i++) {
			items[i] = labels.get(i);
		}
		final boolean[] checkedItems = new boolean[labels.size()];
		final Dialog dialog = new AlertDialog.Builder(this).setTitle("Options").setCancelable(false)
		        .setMultiChoiceItems(items, checkedItems, new OnMultiChoiceClickListener() {
			        @Override
			        @SuppressWarnings("unchecked")
			        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				        Object obj;
				        
				        final Set<String> selectedValues;
				        if ((obj = v.getTag(R.id.tkey_atr_selected)) == null) {
					        selectedValues = new HashSet<String>();
					        v.setTag(R.id.tkey_atr_selected, selectedValues);
				        } else {
					        selectedValues = (Set<String>) obj;
				        }
				        
				        final Set<String> selectedLabels;
				        if ((obj = v.getTag(R.id.tkey_atr_selected_labels)) == null) {
				        	selectedLabels = new HashSet<String>();
				        	v.setTag(R.id.tkey_atr_selected_labels, selectedLabels);
				        } else {
				        	selectedLabels = (Set<String>) obj;
				        }

				        final String label = items[which].toString();
				        final String val = options.get(label);
				        
				        if (isChecked) {
					        selectedValues.add(val);
					        selectedLabels.add(label);
				        } else {
					        selectedValues.remove(val);
					        selectedLabels.remove(label);
				        }
			        }
		        }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			        @Override
			        public void onClick(DialogInterface dialog, int which) {
				        @SuppressWarnings("unchecked")
				        final Set<String> selectedLabels = (Set<String>) v.getTag(R.id.tkey_atr_selected_labels);
				        if (selectedLabels != null) {
					        String s = Arrays.toString(selectedLabels.toArray());
					        v.setText(s);
				        } else {
					        v.setText("");
				        }
			        }
		        }).create();
		dialog.show();
	}

}
/*
 * $newProductData = array( 'name' => 'name of product', // websites - Array of website ids to which you want to assign
 * a new product 'websites' => array(1), // array(1,2,3,...) 'short_description' => 'short description', 'description'
 * => 'description', 'status' => 1, 'weight' => 0, 'tax_class_id' => 1, 'categories' => array(3), //3 is the category id
 * 'price' => 12.05 );
 */