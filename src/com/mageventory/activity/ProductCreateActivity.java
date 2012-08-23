package com.mageventory.activity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;

import com.mageventory.R;
import com.mageventory.tasks.BookInfoLoader;
import com.mageventory.tasks.CreateNewProduct;
import com.mageventory.tasks.CreateOptionTask;
import com.mageventory.util.Util;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.R.id;
import com.mageventory.R.layout;
import com.mageventory.R.string;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Category;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.DefaultOptionsMenuHelper;
import android.widget.AutoCompleteTextView;

public class ProductCreateActivity extends AbsProductActivity implements OperationObserver {

	public static final String PRODUCT_CREATE_ATTRIBUTE_SET = "attribute_set";
	public static final String PRODUCT_CREATE_DESCRIPTION = "description";
	public static final String PRODUCT_CREATE_WEIGHT = "weight";
	public static final String PRODUCT_CREATE_CATEGORY = "category";
	public static final String PRODUCT_CREATE_SHARED_PREFERENCES = "ProductCreateSharedPreferences";

	private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
	private SharedPreferences preferences;

	@SuppressWarnings("unused")
	private static final String TAG = "ProductCreateActivity";
	private static final String[] MANDATORY_USER_FIELDS = { MAGEKEY_PRODUCT_QUANTITY };

	// views
	public EditText skuV;
	public EditText priceV;
	public EditText quantityV;
	public EditText weightV;
	// private CheckBox statusV;
	public EditText barcodeInput;
	private TextView attrFormatterStringV;

	// dialogs
	private ProgressDialog progressDialog;
	private boolean firstTimeAttributeSetResponse = true;
	
	private String productSKUPassed;
	private boolean skuExistsOnServerUncertaintyPassed;
	private boolean isActivityAlive;
	private ProductInfoLoader backgroundProductInfoLoader;
	private int loadRequestID;
	private boolean mLoadLastAttributeSetAndCategory;

	/* Show dialog that informs the user that we are uncertain whether the product with a scanned SKU is present on the 
	 * server or not (This will be only used in case when we get to "product create" activity from "scan" acivity) */
	public void showSKUExistsOnServerUncertaintyDialog()
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Info");
		alert.setMessage("The application could not establish whether the product with the " +
			"scanned SKU exists on the server's database or not. This may have been caused either by connection problem " +
			"or by server problem.");
	
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				/* Do nothing. */
			}
		});

		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.product_create);

		nameV = (AutoCompleteTextView) findViewById(R.id.name);

		super.onCreate(savedInstanceState);

		if (getPreviousActivityClass() != null && getPreviousActivityClass().equals(ProductCreateActivity.class))
		{
			mLoadLastAttributeSetAndCategory = true;
		}
		else
		{
			mLoadLastAttributeSetAndCategory = false;
		}
		
		skuV = (EditText) findViewById(R.id.sku);
		priceV = (EditText) findViewById(R.id.price);
		quantityV = (EditText) findViewById(R.id.quantity);
		descriptionV = (AutoCompleteTextView) findViewById(R.id.description);
		weightV = (EditText) findViewById(R.id.weight);
		// statusV = (CheckBox) findViewById(R.id.status);
		attrFormatterStringV = (TextView) findViewById(R.id.attr_formatter_string);

		preferences = getSharedPreferences(PRODUCT_CREATE_SHARED_PREFERENCES, Context.MODE_PRIVATE);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			productSKUPassed = extras.getString(getString(R.string.ekey_product_sku));
			skuExistsOnServerUncertaintyPassed = extras.getBoolean(getString(R.string.ekey_sku_exists_on_server_uncertainty));
			
			if (!TextUtils.isEmpty(productSKUPassed))
			{
				skuV.setText(productSKUPassed);
			}
			
			/* Not sure whether this product is on the server. Show info about this problem. */
			if (skuExistsOnServerUncertaintyPassed == true)
			{
				showSKUExistsOnServerUncertaintyDialog();
			}
		}
		
		// listeners
		findViewById(R.id.create_btn).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				/* It is not possible for the user to create a product if some custom attribute options
				 * are being created. */
				if (newAttributeOptionPendingCount == 0) {
					if (verifyForm(false) == false) {
						Toast.makeText(getApplicationContext(), "Please fill out all required fields...",
								Toast.LENGTH_SHORT).show();
					} else {
						createNewProduct(false);
					}
				} else {
					Toast.makeText(getApplicationContext(), "Wait for options creation...", Toast.LENGTH_SHORT).show();
				}
			}
		});

		attributeSetV.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				attributeSetLongTap = true;

				String description = preferences.getString(PRODUCT_CREATE_DESCRIPTION, "");
				String weight = preferences.getString(PRODUCT_CREATE_WEIGHT, "");

				descriptionV.setText(description);
				weightV.setText(weight);

				loadLastAttributeSetAndCategory(true);
				
				return true;
			}
		});

		// ---
		// sell functionality

		skuV.setOnLongClickListener(scanSKUOnClickL);

		// Get the Extra "PASSING SKU -- SHOW IT"
		if (getIntent().hasExtra(PASSING_SKU)) {
			boolean isSKU = getIntent().getBooleanExtra(PASSING_SKU, false);
			if (isSKU) {
				skuV.setText(getIntent().getStringExtra(MAGEKEY_PRODUCT_SKU));
			}
		}

		// Set the Action for Quick Sell Button
		final Button quickSell = (Button) findViewById(R.id.createAndSellButton);
		quickSell.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Create Order and Save Product
				createOrder();
			}
		});

		barcodeInput = (EditText) findViewById(R.id.barcode_input);
		barcodeInput.setOnLongClickListener(scanBarcodeOnClickL);
		barcodeInput.setOnTouchListener(null);
		isActivityAlive = true;
	}

	private void loadLastAttributeSetAndCategory(boolean loadLastUsedCustomAttribs)
	{
		int lastAttributeSet = preferences.getInt(PRODUCT_CREATE_ATTRIBUTE_SET, INVALID_CATEGORY_ID);
		int lastCategory = preferences.getInt(PRODUCT_CREATE_CATEGORY, INVALID_CATEGORY_ID);
		
		selectAttributeSet(lastAttributeSet, false, loadLastUsedCustomAttribs, false);

		if (lastCategory != INVALID_CATEGORY_ID) {
			Map<String, Object> cats = getCategories();

			if (cats != null && !cats.isEmpty()) {
				List<Category> list = Util.getCategorylist(cats, null);

				for (Category cat : list) {
					if (cat.getId() == lastCategory) {
						category = cat;
						categoryV.setText(cat.getFullName());
						break;
					}
				}
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		isActivityAlive = false;
	}
	
	private OnLongClickListener scanSKUOnClickL = new OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			Intent scanInt = new Intent("com.google.zxing.client.android.SCAN");
			scanInt.putExtra("SCAN_MODE", "QR_CODE_MODE");
			startActivityForResult(scanInt, SCAN_QR_CODE);
			return true;
		}
	};

	private OnLongClickListener scanBarcodeOnClickL = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			Intent scanInt = new Intent("com.google.zxing.client.android.SCAN");
			startActivityForResult(scanInt, SCAN_BARCODE);
			return true;
		}
	};

	public boolean verifyForm(boolean quickSellMode) {
		// check user fields
		if (checkForFields(extractCommonData(), MANDATORY_USER_FIELDS) == false) {
			return false;
		}

		/* We don't need attribute set in quick sell mode. */
		if (quickSellMode == false)
		{
			//check attribute set
			if (atrSetId == INVALID_ATTRIBUTE_SET_ID) {
				return false;
			}
		}

		if (customAttributesList.getList() != null) {
			for (CustomAttribute elem : customAttributesList.getList()) {
				if (elem.getIsRequired() == true && TextUtils.isEmpty(elem.getSelectedValue())) {
					return false;
				}
			}
		}

		return true;
	}

	private void createNewProduct(boolean quickSellMode) {
		showProgressDialog("Creating product...");

		CreateNewProduct createTask = new CreateNewProduct(this, quickSellMode);
		createTask.execute();
	}

	public Map<String, String> extractCommonData() {
		final Map<String, String> data = new HashMap<String, String>();

		String name = getProductName(this, nameV);
		String price = priceV.getText().toString();
		String description = descriptionV.getText().toString();
		String weight = weightV.getText().toString();
		final String quantity = quantityV.getText().toString();

		if (TextUtils.isEmpty(price)) {
			price = "0";
		}

		if (TextUtils.isEmpty(description)) {
			description = "n/a";
		}

		if (TextUtils.isEmpty(weight)) {
			weight = "0";
		}

		data.put(MAGEKEY_PRODUCT_NAME, name);
		data.put(MAGEKEY_PRODUCT_PRICE, price);
		data.put(MAGEKEY_PRODUCT_DESCRIPTION, description);
		data.put(MAGEKEY_PRODUCT_SHORT_DESCRIPTION, description);
		data.put(MAGEKEY_PRODUCT_WEIGHT, weight);
		data.put(MAGEKEY_PRODUCT_QUANTITY, quantity);

		return data;
	}

	private static boolean checkForFields(final Map<String, ?> fields, final String[] fieldKeys) {
		for (final String fieldKey : fieldKeys) {
			final Object obj = fields.get(fieldKey);
			if (obj == null) {
				return false;
			}
			if (obj instanceof String) {
				if (TextUtils.isEmpty((String) obj)) {
					return false;
				}
			}
		}
		return true;
	}

	public void showProgressDialog(final String message) {
		if (isActive == false) {
			return;
		}
		if (progressDialog != null) {
			return;
		}
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(message);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.show();
	}

	public void dismissProgressDialog() {
		if (progressDialog == null) {
			return;
		}
		progressDialog.dismiss();
		progressDialog = null;
	}

	// sell functionality

	/**
	 * Create Order
	 */
	private void createOrder() {
		// Check that all necessary information exists
		if (validateProductInfo()) {
			if (newAttributeOptionPendingCount == 0) {
				createNewProduct(true);
			} else {
				Toast.makeText(getApplicationContext(), "Wait for options creation...", Toast.LENGTH_SHORT).show();
			}
		}
	}

	// Validate Necessary Product Information
	// to create an order [Name, Price, Quantity]
	private boolean validateProductInfo() {
		String message = "You Must Enter:";
		boolean result = true;

		if (TextUtils.isEmpty(priceV.getText())) {
			result = false;
			message += " Price,";
		}

		if (TextUtils.isEmpty(quantityV.getText())) {
			result = false;
			message += " Quantity,";
		}

		// Check if name is empty
		if (TextUtils.isEmpty(getProductName(this, nameV))) {
			result = false;
			message += " Name";
		}

		if (!result) {
			AlertDialog.Builder builder = new Builder(ProductCreateActivity.this);

			if (message.endsWith(","))
				message = message.substring(0, message.length() - 1);

			builder.setMessage(message);
			builder.setTitle("Missing Information");
			builder.setPositiveButton("OK", null);

			builder.create().show();
		}

		// All Required Data Exists
		return result;
	}

	@Override
	public void onAttributeSetLoadSuccess() {
		super.onAttributeSetLoadSuccess();
		
		if (firstTimeAttributeSetResponse == true) {
		
			if (mLoadLastAttributeSetAndCategory == true)
			{
				loadLastAttributeSetAndCategory(false);
				mLoadLastAttributeSetAndCategory = false;
			}
			else
			{
				// y: hard-coding 4 as required:
				// http://code.google.com/p/mageventory/issues/detail?id=18#c29
				selectAttributeSet(TODO_HARDCODED_DEFAULT_ATTRIBUTE_SET, false, false, true);
			}
			
			firstTimeAttributeSetResponse = false;
		}
		
	}

	@Override
	public void onAttributeListLoadSuccess() {
		super.onAttributeListLoadSuccess();

		String formatterString = customAttributesList.getUserReadableFormattingString();

		if (formatterString != null) {
			attrFormatterStringV.setVisibility(View.VISIBLE);
			attrFormatterStringV.setText(formatterString);
		} else {
			attrFormatterStringV.setVisibility(View.GONE);
		}
	}

	public void showInvalidLabelDialog(final String settingsDomainName, final String skuDomainName) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Warning");
		alert.setMessage("Wrong label. Expected domain name: '" + settingsDomainName + "' found: '" + skuDomainName +"'" );

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ScanActivity.rememberDomainNamePair(settingsDomainName, skuDomainName);
			}
		});
		
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				skuV.setText("");
			}
		});

		AlertDialog srDialog = alert.create();
		
		srDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				skuV.setText("");
			}
		});
		
		srDialog.show();
	}

	/**
	 * Get the Scanned Code
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode == SCAN_QR_CODE) {
			if (resultCode == RESULT_OK) {
				String contents = intent.getStringExtra("SCAN_RESULT");
				
				String[] urlData = contents.split("/");
				
				if (urlData.length > 0) {
					skuV.setText(urlData[urlData.length - 1]);
					skuV.requestFocus();
					
					if (backgroundProductInfoLoader != null)
					{
						backgroundProductInfoLoader.cancel(false);
					}
					
					backgroundProductInfoLoader = new ProductInfoLoader(urlData[urlData.length - 1]);
					backgroundProductInfoLoader.execute();
					
				}
				priceV.requestFocus();
				
				/* Check if the label is valid in relation to the url set in the settings and show appropriate
				information if it's not. */
				if (!ScanActivity.isLabelValid(this, contents))
				{
					Settings settings = new Settings(this);
					String settingsUrl = settings.getUrl();
					
					if (!ScanActivity.domainPairRemembered(ScanActivity.getDomainNameFromUrl(settingsUrl), ScanActivity.getDomainNameFromUrl(contents)))
					{
						showInvalidLabelDialog(ScanActivity.getDomainNameFromUrl(settingsUrl), ScanActivity.getDomainNameFromUrl(contents));
					}
				}
				
			} else if (resultCode == RESULT_CANCELED) {
				// Do Nothing
			}
		}

		if (requestCode == SCAN_BARCODE) {
			if (resultCode == RESULT_OK) {
				String contents = intent.getStringExtra("SCAN_RESULT");

				// Set Barcode in Product Barcode TextBox
				((EditText) findViewById(R.id.barcode_input)).setText(contents);

				// Check if Attribute Set is Book
				EditText attrSet = (EditText) findViewById(R.id.attr_set);
				if (TextUtils.equals(attrSet.getText().toString(), "Book")) {
					Settings settings = new Settings(getApplicationContext());
					String apiKey = settings.getAPIkey();
					if (TextUtils.equals(apiKey, "")) {
						Toast.makeText(getApplicationContext(),
								"Book Search is Disabled - set Google API KEY to enable it", Toast.LENGTH_SHORT).show();
					} else {
						new BookInfoLoader(this, customAttributesList).execute(contents, apiKey);
					}
				}
				weightV.requestFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
				imm.showSoftInput(weightV, 0);
			} else if (resultCode == RESULT_CANCELED) {
				// Do Nothing
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		resHelper.registerLoadOperationObserver(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		resHelper.unregisterLoadOperationObserver(this);
	}

	public void showKnownSkuDialog(final String sku) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Question");
		alert.setMessage("Known SKU. Show product details?");

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
				launchProductDetails(sku);
			}
		});
		
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				skuV.setText("");
			}
		});

		AlertDialog srDialog = alert.create();
		srDialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				skuV.setText("");
			}
		});
		srDialog.show();
	}
	
	private void launchProductDetails(String sku)
	{
		final String ekeyProductSKU = getString(R.string.ekey_product_sku);
		final Intent intent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(ekeyProductSKU, sku);

		startActivity(intent);
	}
	
	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		if (isActivityAlive) {
			if (op.getOperationRequestId() == loadRequestID)
			{
				if (op.getException() == null) {
					showKnownSkuDialog(op.getResourceParams()[1]);	
				}
			}
		}
	}
	
	private class ProductInfoLoader extends AsyncTask<String, Void, Boolean> {

		private String sku;
		
		private SettingsSnapshot mSettingsSnapshot;
		
		public ProductInfoLoader(String sku)
		{
			this.sku = sku;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mSettingsSnapshot = new SettingsSnapshot(ProductCreateActivity.this);
		}
		
		@Override
		protected Boolean doInBackground(String... args) {
			final String[] params = new String[2];
			params[0] = GET_PRODUCT_BY_SKU; // ZERO --> Use Product ID , ONE -->
											// Use Product SKU
			params[1] = this.sku;

			if (JobCacheManager.productDetailsExist(params[1], mSettingsSnapshot.getUrl())) {
				return Boolean.TRUE;
			} else {
				loadRequestID = resHelper.loadResource(ProductCreateActivity.this, RES_PRODUCT_DETAILS, params, mSettingsSnapshot);
				return Boolean.FALSE;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result.booleanValue() == true) {
				if (isActivityAlive) {
					showKnownSkuDialog(this.sku);
				}
			}
		}

	}

}