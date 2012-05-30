package com.mageventory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.util.Linkify;

import com.mageventory.util.Log;
import com.mageventory.util.Util;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.jobprocessor.CreateProductProcessor;
import com.mageventory.model.Category;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.resprocessor.CreateCartOrderProcessor;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.Settings;
import com.mageventory.util.DefaultOptionsMenuHelper;

public class ProductCreateActivity extends AbsProductActivity implements OperationObserver {

    private static final String PRODUCT_CREATE_ATTRIBUTE_SET = "attribute_set";
    private static final String PRODUCT_CREATE_DESCRIPTION = "description";
    private static final String PRODUCT_CREATE_WEIGHT = "weight";
    private static final String PRODUCT_CREATE_CATEGORY = "category";
	private static final String PRODUCT_CREATE_SHARED_PREFERENCES = "ProductCreateSharedPreferences";

	private SharedPreferences preferences;
	
    private static class CreateNewProductTask extends AsyncTask<Void, Void, Integer> {

    	private ProductCreateActivity mHostActivity;
    	private JobControlInterface mJobControlInterface;
    	
        private static int FAILURE = 0;
        private static int E_BAD_FIELDS = 1;
        private static int E_SKU_ALREADY_EXISTS = 3;
        private static int SUCCESS = 4;
    	
        private String newSKU;
        
        public CreateNewProductTask(ProductCreateActivity hostActivity) {
        	mHostActivity = hostActivity;
        	mJobControlInterface = new JobControlInterface(mHostActivity);
        }
        
    	private static class IncompleteDataException extends RuntimeException {

    		private static final long serialVersionUID = 1L;

    		public IncompleteDataException() {
    			super();
    		}

    		public IncompleteDataException(String detailMessage) {
    			super(detailMessage);
    		}

    	}
        
    	private Map<String, Object> extractData(Bundle bundle, boolean exceptionOnFail) throws IncompleteDataException {
    		// @formatter:off
            final String[] stringKeys = {
                    MAGEKEY_PRODUCT_NAME,
                    MAGEKEY_PRODUCT_PRICE,
                    MAGEKEY_PRODUCT_WEBSITE,
                    MAGEKEY_PRODUCT_DESCRIPTION,
                    MAGEKEY_PRODUCT_SHORT_DESCRIPTION,
                    MAGEKEY_PRODUCT_STATUS,
                    MAGEKEY_PRODUCT_WEIGHT,
            };
            // @formatter:on
    		final Map<String, Object> productData = new HashMap<String, Object>();
    		for (final String stringKey : stringKeys) {
    			productData.put(stringKey, extractString(bundle, stringKey, exceptionOnFail));
    		}
    		final Object cat = bundle.get(MAGEKEY_PRODUCT_CATEGORIES);
    		if (cat != null && cat instanceof Object[] == true) {
    			productData.put(MAGEKEY_PRODUCT_CATEGORIES, cat);	
    		}
    		
    		return productData;
    	}
    	
    	private String extractString(final Bundle bundle, final String key, final boolean exceptionOnFail) throws IncompleteDataException {
    		final String s = bundle.getString(key);
    		if (s == null && exceptionOnFail) {
    			throw new IncompleteDataException("bad data for key '" + key + "'");
    		}
    		return s == null ? "" : s;
    	}
        
        private Map<String, Object> extractUpdate(Bundle bundle) throws IncompleteDataException {
            final String[] stringKeys = {
                    MAGEKEY_PRODUCT_QUANTITY,
                    MAGEKEY_PRODUCT_MANAGE_INVENTORY,
                    MAGEKEY_PRODUCT_IS_IN_STOCK
            };
            // @formatter:on
            final Map<String, Object> productData = new HashMap<String, Object>();
            for (final String stringKey : stringKeys) {
                productData.put(stringKey, extractString(bundle, stringKey, true));
            }       
            return productData;
        }

		@Override
        protected Integer doInBackground(Void... params) {
            if (mHostActivity == null || isCancelled()) {
                return FAILURE;
            }

            if (mHostActivity.verifyForm() == false) {
                return E_BAD_FIELDS;
            }
            
            /* Attribute list from the server for the attribute set we're using*/
            List<Map<String, Object>> attributeList = mHostActivity.getAttributeList();
            
            /* The request is missing a structure related to attributes when compared to the response. We're building
             * this structure (list of maps) here to simulate the response. */
            List<Map<String, Object>> selectedAttributesResponse = new ArrayList<Map<String, Object>>();
            
            final Bundle data = new Bundle();
            final Map<String, String> extracted = mHostActivity.extractCommonData();

            // user fields
            for (Map.Entry<String, String> e : extracted.entrySet()) {
                data.putString(e.getKey(), e.getValue());
            }

            if (mHostActivity.category != null && mHostActivity.category.getId() != INVALID_CATEGORY_ID) {
            	data.putSerializable(MAGEKEY_PRODUCT_CATEGORIES, new Object[] { String.valueOf(mHostActivity.category.getId())});
            }

            // default values
            data.putString(MAGEKEY_PRODUCT_WEBSITE, "1");
            
            data.putString(MAGEKEY_PRODUCT_SKU, mHostActivity.skuV.getText().toString());

            // generated
            String quantity = "" + extracted.get(MAGEKEY_PRODUCT_QUANTITY);
            int status = 1; // Must be Always 1 - to be able to sell it
            String inventoryControl = "";
            String isInStock = "1"; // Any Product is Always in Stock

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

            data.putString(MAGEKEY_PRODUCT_STATUS, "" + status);
            data.putString(MAGEKEY_PRODUCT_MANAGE_INVENTORY, inventoryControl);
            data.putString(MAGEKEY_PRODUCT_IS_IN_STOCK, isInStock);

            // attributes
            // bundle attributes
            final HashMap<String, Object> atrs = new HashMap<String, Object>();
            
            for (CustomAttribute elem : mHostActivity.customAttributesList.getList())
            {
            	atrs.put(elem.getCode(), elem.getSelectedValue());
            	
            	Map<String, Object> selectedAttributesResponseMap = new HashMap<String, Object>();
                selectedAttributesResponseMap.put(MAGEKEY_ATTRIBUTE_CODE, elem.getCode());
                
                Map<String,Object> frontEndLabel = new HashMap<String,Object>();
        		frontEndLabel.put("label",elem.getMainLabel());
        		
        		selectedAttributesResponseMap.put("frontend_label", new Object[] {frontEndLabel});
        		selectedAttributesResponseMap.put("frontend_input", elem.getType());
        		
        		if (elem.getOptionsAsArrayOfMaps() != null)
        		{
        			selectedAttributesResponseMap.put("options", elem.getOptionsAsArrayOfMaps());
        		}
        		else
        		{
        			selectedAttributesResponseMap.put("options", new Object[0]);
        		}
        		
                selectedAttributesResponse.add(selectedAttributesResponseMap);
            }

            atrs.put("product_barcode_", mHostActivity.barcodeInput.getText().toString());
            
            /* Response simulation related code */
            Map<String, Object> selectedAttributesResponseMap = new HashMap<String, Object>();
            selectedAttributesResponseMap.put(MAGEKEY_ATTRIBUTE_CODE, "product_barcode_");
  		
            Map<String,Object> frontEndLabel = new HashMap<String,Object>();
    		frontEndLabel.put("label","Barcode");
            
      		selectedAttributesResponseMap.put("frontend_label", new Object[] {frontEndLabel});
      		selectedAttributesResponseMap.put("frontend_input", "");
      		selectedAttributesResponseMap.put("options", new Object[0]);
            
            selectedAttributesResponse.add(selectedAttributesResponseMap);
            /* End of response simulation related code */
            
            data.putInt(EKEY_PRODUCT_ATTRIBUTE_SET_ID, mHostActivity.atrSetId);
            data.putSerializable(EKEY_PRODUCT_ATTRIBUTE_VALUES, atrs);
            
            /* Convert this data to a format understandable by the job service. */
            Map<String, Object> productRequestData = extractData(data, true);
            productRequestData.put("tax_class_id", "0");
            
            // extract attribute data
            final int attrSet = data.getInt(EKEY_PRODUCT_ATTRIBUTE_SET_ID, INVALID_ATTRIBUTE_SET_ID);
            @SuppressWarnings("unchecked")
            final Map<String, String> atrs2 = (Map<String, String>) data.getSerializable(EKEY_PRODUCT_ATTRIBUTE_VALUES);
            
            if (atrs2 != null && atrs2.isEmpty() == false) {
            	productRequestData.putAll(atrs2);
            }
            
            if (attrSet == INVALID_ATTRIBUTE_SET_ID) {
            	Log.w(TAG, "INVALID ATTRIBUTE SET ID");
            	return FAILURE;
            }

            productRequestData.putAll(extractUpdate(data));
            
            newSKU = data.getString(MAGEKEY_PRODUCT_SKU);
            
            if(TextUtils.isEmpty(newSKU))
            {
            	// Empty Generate SKU
            	newSKU = CreateProductProcessor.generateSku(productRequestData, false);
            }
            
            productRequestData.put(MAGEKEY_PRODUCT_SKU, newSKU);
            productRequestData.put(EKEY_PRODUCT_ATTRIBUTE_SET_ID, new Integer(attrSet));
            
            /* Simulate a response from the server so that we can store it in cache. */
            Map<String, Object> productResponseData = new HashMap<String, Object>(productRequestData);
            
            /* Filling the things that were missing in the request to simulate a response. */

            if (mHostActivity.category != null && mHostActivity.category.getId() != INVALID_CATEGORY_ID) {
            	productResponseData.put(MAGEKEY_PRODUCT_CATEGORY_IDS, new Object[] { String.valueOf(mHostActivity.category.getId())});	
            }
            
            productResponseData.put(MAGEKEY_PRODUCT_IMAGES, new Object[0]);
            productResponseData.put(MAGEKEY_PRODUCT_ID, INVALID_PRODUCT_ID);
            productResponseData.put("set_attributes", selectedAttributesResponse.toArray());
            
            Product p = new Product(productResponseData, true, true);
            
            if (JobCacheManager.productDetailsExists(p.getSku()))
            {
            	return E_SKU_ALREADY_EXISTS;
            }

            JobID jobID = new JobID(INVALID_PRODUCT_ID, RES_CATALOG_PRODUCT_CREATE, newSKU);
            Job job = new Job(jobID);
            job.setExtras(productRequestData);
            
            mJobControlInterface.addJob(job);
            
            JobCacheManager.storeProductDetails(p);
            
            return SUCCESS;
        }


        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            
            if (mHostActivity == null) {
                return;
            }
            if (result == SUCCESS) {
                // successful creation, launch product details activity
            	
                final String ekeyProductSKU = mHostActivity.getString(R.string.ekey_product_sku);
                final Intent intent = new Intent(mHostActivity, ProductDetailsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(ekeyProductSKU, newSKU);
                mHostActivity.startActivity(intent);
                
            } else if (result == FAILURE) {
                Toast.makeText(mHostActivity, "Creation failed...", Toast.LENGTH_LONG).show();
            } else if (result == E_BAD_FIELDS) {
                Toast.makeText(mHostActivity, "Please fill out all fields...", Toast.LENGTH_LONG).show();
            } else if (result == E_SKU_ALREADY_EXISTS) {
                Toast.makeText(mHostActivity, "Product with that SKU already exists...", Toast.LENGTH_LONG).show();
            }
            
            mHostActivity.dismissProgressDialog();
            mHostActivity.finish();
        }
    }
    
    
    @SuppressWarnings("unused")
    private static final String TAG = "ProductCreateActivity";
    private static final String[] MANDATORY_USER_FIELDS = { MAGEKEY_PRODUCT_QUANTITY };

    // views
    private EditText nameV;
    private EditText skuV;
    private EditText priceV;
    private EditText quantityV;
    private EditText descriptionV;
    private EditText weightV;
    // private CheckBox statusV;
    private EditText barcodeInput;

    // state
    private int orderCreateId;

    // dialogs
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // map views
        nameV = (EditText) findViewById(R.id.name);
        skuV = (EditText) findViewById(R.id.sku);
        priceV = (EditText) findViewById(R.id.price);
        quantityV = (EditText) findViewById(R.id.quantity);
        descriptionV = (EditText) findViewById(R.id.description);
        weightV = (EditText) findViewById(R.id.weight);
        // statusV = (CheckBox) findViewById(R.id.status);

		preferences = getSharedPreferences(PRODUCT_CREATE_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        
        // listeners
        findViewById(R.id.create_btn).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (verifyForm() == false) {
                    Toast.makeText(getApplicationContext(), "Please fill out all required fields...",
                            Toast.LENGTH_SHORT).show();
                } else {
                	SharedPreferences.Editor editor = preferences.edit();
                	
                	editor.putString(PRODUCT_CREATE_DESCRIPTION, descriptionV.getText().toString());
                	editor.putString(PRODUCT_CREATE_WEIGHT, weightV.getText().toString());
                	editor.putInt(PRODUCT_CREATE_ATTRIBUTE_SET, atrSetId);		
                			
                	if (category != null) {
                		editor.putInt(PRODUCT_CREATE_CATEGORY, category.getId());
                	}
                	else
                	{
                		editor.putInt(PRODUCT_CREATE_CATEGORY, INVALID_CATEGORY_ID);
                	}
    			    
    			    editor.commit();
                	
                    createNewProduct();
                }
            }
        });
        
        attributeSetV.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				attributeSetLongTap = true;

				int lastAttributeSet = preferences.getInt(PRODUCT_CREATE_ATTRIBUTE_SET, INVALID_CATEGORY_ID);
			    int lastCategory = preferences.getInt(PRODUCT_CREATE_CATEGORY, INVALID_CATEGORY_ID);
			    String description = preferences.getString(PRODUCT_CREATE_DESCRIPTION, "");
			    String weight = preferences.getString(PRODUCT_CREATE_WEIGHT, "");
			    
			    selectAttributeSet(lastAttributeSet, false);
			    
			    if (lastCategory != INVALID_CATEGORY_ID)
			    {
			    	Map<String, Object> cats = getCategories();
				
			    	if (cats != null && !cats.isEmpty())
			    	{
			    		List<Category> list = Util.getCategorylist(cats, null);
			    		
			    		for (Category cat: list)
			    		{	
			    			if ( cat.getId() == lastCategory )	
			    			{
			    				category = cat;
			    				categoryV.setText(cat.getFullName());
			    				break;
			    			}
			    		}
			    	}
			    }
			    
			    descriptionV.setText(description);
			    weightV.setText(weight);
			    
				return false;
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
        
        // set Qty view on edit action
        // When Qty TextBox is edited and both price and quantity has text
        // then enable the sell button
        quantityV.addTextChangedListener(checkMandatorySellFields());
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

    @Override
    protected int getContentView() {
        return R.layout.product_create;
    }

    public boolean verifyForm() {
        // check user fields
        if (checkForFields(extractCommonData(), MANDATORY_USER_FIELDS) == false) {
            return false;
        }

        // check attribute set
        if (atrSetId == INVALID_ATTRIBUTE_SET_ID) {
            return false;
        }

        for (CustomAttribute elem : customAttributesList.getList())
        {
        	if (elem.getIsRequired() == true && TextUtils.isEmpty(elem.getSelectedValue()))
        	{
        		return false;
        	}
        }
        
        return true;
    }
    
    private void createNewProduct()
    {
        showProgressDialog("Creating product");
        
        CreateNewProductTask createTask = new CreateNewProductTask(this);
        createTask.execute();
    }

    private Map<String, String> extractCommonData() {
        final Map<String, String> data = new HashMap<String, String>();

        String name = nameV.getText().toString();
        String price = priceV.getText().toString();
        String description = descriptionV.getText().toString();
        String weight = weightV.getText().toString();
        final String quantity = quantityV.getText().toString();

        if (TextUtils.isEmpty(name)) {
        	name = "n/a";
        }
        
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

    private void showProgressDialog(final String message) {
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

    private void dismissProgressDialog() {
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
        showProgressDialog("Creating Product & Submitting Order");
        new CreateOrder().execute();
    }

    /**
     * Create Order Invoice
     * 
     * @author hussein
     * 
     */
    private class CreateOrder extends AsyncTask<Integer, Integer, String> {

        Product product;

        @Override
        protected String doInBackground(Integer... ints) {

            // 2- Set Product Information
            final String sku = skuV.getText().toString();
            String soldPrice = priceV.getText().toString();
            final String qty = quantityV.getText().toString();
            String name = nameV.getText().toString();

            if (TextUtils.isEmpty(name)) {
            	name = "n/a";
            }
            
            if (TextUtils.isEmpty(soldPrice)) {
            	soldPrice = "0";
            }
            
            try {
                final Bundle bundle = new Bundle();
                /* PRODUCT INFORMAITON */
                bundle.putString(MAGEKEY_PRODUCT_SKU, sku);
                bundle.putString(MAGEKEY_PRODUCT_QUANTITY, qty);
                bundle.putString(MAGEKEY_PRODUCT_PRICE, soldPrice);
                bundle.putString(MAGEKEY_PRODUCT_NAME, name);

                if (ResourceServiceHelper.getInstance().isResourceAvailable(ProductCreateActivity.this,
                        RES_CART_ORDER_CREATE, null))
                    product = ResourceServiceHelper.getInstance().restoreResource(ProductCreateActivity.this,
                            RES_CART_ORDER_CREATE, null);
                else
                    orderCreateId = ResourceServiceHelper.getInstance().loadResource(ProductCreateActivity.this,
                            RES_CART_ORDER_CREATE, null, bundle);

                return null;
            } catch (Exception e) {
                Log.w(TAG, "" + e);
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (product != null) {
                dismissProgressDialog();
                // set as old
                ResourceServiceHelper.getInstance().markResourceAsOld(ProductCreateActivity.this,
                        RES_CART_ORDER_CREATE, null);

                // Product Exists --> Show Product Details
                final String ekeyProductSKU = getString(R.string.ekey_product_sku);
                final String productSKU = product.getSku();
                final Intent intent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(ekeyProductSKU, productSKU);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onLoadOperationCompleted(LoadOperation op) {
        if (op.getOperationRequestId() == orderCreateId) {
            new CreateOrder().execute();
            if (op.getException() != null) {
                Toast.makeText(getApplicationContext(), "Action Failed\n" + op.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
                dismissProgressDialog();
                return;
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return DefaultOptionsMenuHelper.onCreateOptionsMenu(this, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            loadAttributeSetsAndCategories(true);
            return true;
        }
        return DefaultOptionsMenuHelper.onOptionsItemSelected(this, item);
    }

    @Override
    protected void onAttributeSetLoadSuccess() {
        super.onAttributeSetLoadSuccess();
        selectDefaultAttributeSet();
    }

    private void selectDefaultAttributeSet() {
        // y: hard-coding 4 as required: http://code.google.com/p/mageventory/issues/detail?id=18#c29
        selectAttributeSet(4, false);
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
                } else {
                    Toast.makeText(getApplicationContext(), "Not Valid", Toast.LENGTH_SHORT).show();
                    return;
                }
                priceV.requestFocus();
            } else if (resultCode == RESULT_CANCELED) {
                // Do Nothing
            }
        }
	        
        if (requestCode == SCAN_BARCODE) {
            if (resultCode == RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");
               
                // Set Barcode in Product Barcode TextBox
				((EditText)findViewById(R.id.barcode_input)).setText(contents);
					
                // Check if Attribute Set is Book
                EditText attrSet = (EditText) findViewById(R.id.attr_set);
                if(TextUtils.equals(attrSet.getText().toString(), "Book"))
                {
                	Settings settings = new Settings(getApplicationContext());
                	String apiKey = settings.getAPIkey();
                	if(TextUtils.equals(apiKey,""))
                	{
                		Toast.makeText(getApplicationContext(), "Book Search is Disabled - set Google API KEY to enable it", Toast.LENGTH_SHORT).show();
                	}
                	else
                	{
                		new BookInfoLoader().execute(contents,apiKey);
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
    
	/**
	 * Getting Book Details
	 * @author hussein
	 *
	 */
	private class BookInfoLoader extends AsyncTask<Object, Void, Boolean> {

		private String title = "NONE";
		private String description;
		private String authors;
		private String publishDate;
		private String iSBN_10;
		private String iSBN_13;
		private String thumbnail;
		private String previewLink;
		private String infoLink;
		private String id;
		private String selfLink;
		private String publisher;
		private String pageCount;
		private String averageRate;
		private String rateCount;
		private String smallThumbnail;	
		private String viewability;
		private String embeddable;
		private String webReadedLink;
		private String textSnippet;
		private String language;
		private Bitmap image;
		
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			isActive = true;
			showProgressDialog("Loading Book Information ..........");
		}

		@Override
		protected Boolean doInBackground(Object... args) {			
			try 
			{				
				HttpClient client = new DefaultHttpClient();
				HttpGet getRequest = new HttpGet();
				getRequest.setURI(new  URI("https://www.googleapis.com/books/v1/volumes?q=isbn:" + args[0].toString() + "&key="+args[1].toString()));
				
				HttpResponse response = client.execute(getRequest);				
				BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				
				loadBookInfo(reader);
				
				reader.close();
				
				return true;
				
				
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				Log.logCaughtException(e);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.logCaughtException(e);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				Log.logCaughtException(e);
			}
			
			return false;
			
		}

		@Override
		protected void onPostExecute(Boolean result) {
			
			dismissProgressDialog();
			
			if(TextUtils.equals(title,"NONE"))
			{
				Toast.makeText(getApplicationContext(), "No Book Found", Toast.LENGTH_SHORT).show();
				return;
			}
			
			// Show Book Details
			nameV.setText(title);
			descriptionV.setText(description);
			showBookInfo();
		}
		
					
		// read all Book Information from Buffer reader 
		private void loadBookInfo(BufferedReader reader)
		{
			String line = "";			
			try
			{
				while ( (line = reader.readLine()) != null) 
				{				
					/// Get Book ID
					if(line.contains("\"id\""))
					{
						id = getInfo(line,"id");
					}
					
					/// Get Book Self Link
					if(line.contains("\"selfLink\""))
					{
						selfLink = getInfo(line,"selfLink");
					}
					
					/// Read Book Title
					if(line.contains("\"title\""))
					{						
						title = getInfo(line,"title");
					}
										
					/// Read Book Authors
					if(line.contains("\"authors\""))
					{
						line = reader.readLine();
						while(!(line.contains("]")))
						{
							authors += line.replace("\"", "").trim();
							line = reader.readLine();
						}						
						authors = authors.trim();
					}
					
					/// Read Publisher Name
					if(line.contains("\"publisher\""))
					{						
						publisher = getInfo(line,"publisher");
					}
					
					
					/// Read Book Published Date
					if(line.contains("\"publishedDate\""))
					{
						publishDate = getInfo(line,"publishedDate");
					}
					
					/// Read Book Description
					if(line.contains("\"description\""))
					{
						description = getInfo(line,"description");
					}
	
					/// Read Book ISBN_10 URL
					if(line.contains("\"ISBN_10\""))
					{
						line = reader.readLine();
						iSBN_10 = getInfo(line, "identifier");
					}
					
					/// Read Book ISBN_13 URL
					if(line.contains("\"ISBN_13\""))
					{
						line = reader.readLine();
						iSBN_13 = getInfo(line, "identifier");
					}
	
					/// Read Page Count
					if(line.contains("\"pageCount\""))
					{
						pageCount = getInfo(line, "pageCount");
					}
					
					/// Read Average Rating
					if(line.contains("\"averageRating\""))
					{
						averageRate = getInfo(line, "averageRating");
					}
					
					/// Read Rating Count
					if(line.contains("\"ratingsCount\""))
					{
						rateCount = getInfo(line, "ratingsCount");
					}
	
					/// Read Book Small Thumbnail URL
					if(line.contains("\"smallThumbnail\""))
					{
						smallThumbnail = getInfo(line, "smallThumbnail");
					}
										
					/// Read Book Thumbnail URL
					if(line.contains("\"thumbnail\""))
					{
						thumbnail = getInfo(line, "thumbnail");
						image = BitmapFactory.decodeStream((new URL(thumbnail)).openStream());						
					}
					
					/// Read Book Language
					if(line.contains("\"language\""))
					{
						language = getInfo(line, "language");
					}
										
					/// Read Book previewLink URL
					if(line.contains("\"previewLink\""))
					{
						previewLink = getInfo(line, "previewLink");
					}
					
					/// Read Book infoLink URL
					if(line.contains("\"infoLink\""))
					{
						infoLink = getInfo(line, "infoLink");
					}
					
					/// Read Book Viewability
					if(line.contains("\"viewability\""))
					{
						viewability = getInfo(line, "viewability");
					}
					
					/// Read Book Embeddable
					if(line.contains("\"embeddable\""))
					{
						embeddable = getInfo(line, "embeddable");
					}
					
					/// Read Book WebReaderLink
					if(line.contains("\"webReaderLink\""))
					{
						webReadedLink = getInfo(line, "webReaderLink");
					}
					
					/// Read Book WebReaderLink
					if(line.contains("\"textSnippet\""))
					{
						textSnippet = getInfo(line, "textSnippet");
					}
				}
			}
			catch(IOException excpetion)
			{
				return;
			}						
		}


		// Show Book Information in attributes
		private void showBookInfo()
		{
			for(int i=0;i<atrListV.getChildCount();i++)
			{					
				ViewGroup v = (ViewGroup) atrListV.getChildAt(i);
			
				String label = ((TextView)v.findViewById(R.id.label)).getText().toString();
				EditText value = ((EditText)v.getChildAt(1));
				
				// View ID
				if(label.contains("Id"))
				{
					value.setText(id);
				}
				
				// View Self Link
				if(label.contains("Self"))
				{
					value.setText(selfLink);
				}
				
				// View Title
				if(label.contains("Title"))
				{
					value.setText(title);
				}
				
				// View Authors
				if(label.contains("Author"))
				{
					value.setText(authors);
				}
				
				// View Publisher
				if(label.contains("Publisher"))
				{
					value.setText(publisher);
				}
				
				// View Published Date
				if(label.contains("date"))
				{
					value.setText(publishDate);
				}
				
				// View Description
				if(label.contains("Description"))
				{
					value.setText(description);
				}
				
				// View ISBN 10
				if(label.contains("10"))
				{
					value.setText(iSBN_10);
				}
				
				// View ISBN 13
				if(label.contains("13"))
				{
					value.setText(iSBN_13);
				}
				
				// View Page Count
				if(label.contains("Pagecount"))
				{
					value.setText(pageCount);
				}
				
				// View Average Rating
				if(label.contains("Average"))
				{
					value.setText(averageRate);
				}
				
				// View Ratings count
				if(label.contains("Ratingcount"))
				{
					value.setText(rateCount);
				}
				
				// View small Thumbnail
				if(label.contains("Small"))
				{
					value.setText(smallThumbnail);										
				}
				
				// View Thumbnail
				if(label.contains("Thumbnail"))
				{
					value.setText(thumbnail);				
					((ImageView)v.getChildAt(2)).setImageBitmap(image);										
				}
				
				// View Language
				if(label.contains("Language"))
				{
					value.setText(language);					
				}
				
				// View Preview Link
				if(label.contains("Preview"))
				{
					value.setText(previewLink);						
				}
				
				// View Info Link
				if(label.contains("Info"))
				{
					value.setText(infoLink);						
				}
				
				// View viewabilility
				if(label.contains("Viewability"))
				{
					value.setText(viewability);						
				}
				
				// View Embeddable
				if(label.contains("Embedd"))
				{
					value.setText(embeddable);						
				}
				
				// View WebReader Link
				if(label.contains("Web"))
				{
					value.setText(webReadedLink);
					
				}
				
				// View Text Snippet
				if(label.contains("Snippet"))
				{					
					value.setText(textSnippet);						
				}	
					
				if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("link"))
					Linkify.addLinks(((EditText)v.getChildAt(1)), Linkify.ALL);
				if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("humbnail"))
					Linkify.addLinks(((EditText)v.getChildAt(1)), Linkify.ALL);
			}
		}
	
		// Get Book Information from line
		private String getInfo(String line,String name)
		{
			return line.replace(name, "").replace(",", "").replace("\"", "").replace(":", "").replace("http","http:").trim();
		}
	}
	

	/**
	 * This Function Checks that "Quantity, Price and Name" are not empty
	 * If Not Empty then enable Sell Button
	 * Will be called in Name,Price and Quantity TextBox Edit Handlers
	 */
	TextWatcher checkMandatorySellFields()
	{
			TextWatcher textWatcher = new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
				if(!(TextUtils.isEmpty(quantityV.getText())))		//	Check If Quantity TextBox is Empty
				{				
					((Button)findViewById(R.id.createAndSellButton)).setEnabled(true);
					return;
				}
				
				((Button)findViewById(R.id.createAndSellButton)).setEnabled(false);
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
		};
		
		return textWatcher;
	}
	
}