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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
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
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.resprocessor.CreateCartOrderProcessor;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.Settings;
import com.mageventory.util.DefaultOptionsMenuHelper;

public class ProductCreateActivity extends AbsProductActivity implements OperationObserver {
	
    private static class CreateNewProductTask extends AsyncTask<Void, Void, Integer> {

    	private ProductCreateActivity mHostActivity;
    	private JobControlInterface mJobControlInterface;
    	
        private static int FAILURE = 0;
        private static int E_BAD_FIELDS = 1;
        private static int E_BAD_CATEGORY = 2;
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
    		// TODO y: which fields are mandatory?
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
    		if (cat == null || cat instanceof Object[] == false) {
    			throw new IncompleteDataException("bad category");
    		}
    		productData.put(MAGEKEY_PRODUCT_CATEGORIES, cat);
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
            final Map<String, String> productMap = mHostActivity.extractCommonData();
            
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
            data.putString(MAGEKEY_PRODUCT_SHORT_DESCRIPTION, extracted.get(MAGEKEY_PRODUCT_DESCRIPTION));

            if (mHostActivity.category == null || mHostActivity.category.getId() == INVALID_CATEGORY_ID) {
                return E_BAD_CATEGORY;
            }
            
            data.putSerializable(MAGEKEY_PRODUCT_CATEGORIES, new Object[] { String.valueOf(mHostActivity.category.getId())});

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
            for (EditText editField : mHostActivity.atrEditFields) {
                final String code = editField.getTag(R.id.tkey_atr_code).toString();
                if (TextUtils.isEmpty(code)) {
                    continue;
                }
                
                /* Response simulation related code */
                Map<String, Object> selectedAttributesResponseMap = new HashMap<String, Object>();
                selectedAttributesResponseMap.put(MAGEKEY_ATTRIBUTE_CODE, code);
                
                for(int j=0;j<attributeList.size();j++)
				{
                	if (attributeList.get(j).containsValue(code))
                	{
                		
                		Map<String,Object> frontEndLabel = new HashMap<String,Object>();
                		frontEndLabel.put("label",attributeList.get(j).get(MAGEKEY_ATTRIBUTE_INAME));
                		
                		selectedAttributesResponseMap.put("frontend_label", new Object[] {frontEndLabel});
                		selectedAttributesResponseMap.put("frontend_input", attributeList.get(j).get(MAGEKEY_ATTRIBUTE_TYPE));
                		
                		Object op = attributeList.get(j).get(MAGEKEY_ATTRIBUTE_IOPTIONS);
                		
                		if (op != null)
                		{
                			selectedAttributesResponseMap.put("options", ((ArrayList<Map<String, Object>>)op).toArray());
                		}
                		else
                		{
                			selectedAttributesResponseMap.put("options", new Object[0]);
                		}
                		
                		break;
                	}
				}
                /* End of response simulation related code */
                
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
              		selectedAttributesResponseMap.put(code, selected);
                } else {
                    atrs.put(code, editField.getText().toString());
                    selectedAttributesResponseMap.put(code, editField.getText().toString());
                }
                
                selectedAttributesResponse.add(selectedAttributesResponseMap);
            }
            for (Spinner spinnerField : mHostActivity.atrSpinnerFields) {
                final String code = spinnerField.getTag(R.id.tkey_atr_code).toString();
                if (TextUtils.isEmpty(code)) {
                    continue;
                }
                
                /* Response simulation related code */
                Map<String, Object> selectedAttributesResponseMap = new HashMap<String, Object>();
                selectedAttributesResponseMap.put(MAGEKEY_ATTRIBUTE_CODE, code);
                
                for(int j=0;j<attributeList.size();j++)
				{
                	if (attributeList.get(j).containsValue(code))
                	{
                		Map<String,Object> frontEndLabel = new HashMap<String,Object>();
                		frontEndLabel.put("label",attributeList.get(j).get(MAGEKEY_ATTRIBUTE_INAME));
                		
                		selectedAttributesResponseMap.put("frontend_label", new Object[] {frontEndLabel});
                		selectedAttributesResponseMap.put("frontend_input", attributeList.get(j).get(MAGEKEY_ATTRIBUTE_TYPE));
                		selectedAttributesResponseMap.put("options", ((ArrayList<Map<String, Object>>)attributeList.get(j).get(MAGEKEY_ATTRIBUTE_IOPTIONS)).toArray());
                		break;
                	}
				}
                /* End of response simulation related code */
               
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
                selectedAttributesResponseMap.put(code, options.get(selAsStr));
                
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
            productResponseData.put(MAGEKEY_PRODUCT_CATEGORY_IDS, new Object[] { String.valueOf(mHostActivity.category.getId())});
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
                intent.putExtra(ekeyProductSKU, newSKU);
                mHostActivity.startActivity(intent);
                
            } else if (result == FAILURE) {
                Toast.makeText(mHostActivity, "Creation failed...", Toast.LENGTH_LONG).show();
            } else if (result == E_BAD_FIELDS) {
                Toast.makeText(mHostActivity, "Please fill out all fields...", Toast.LENGTH_LONG).show();
            } else if (result == E_BAD_CATEGORY) {
                Toast.makeText(mHostActivity, "Please select a category...", Toast.LENGTH_LONG).show();
            } else if (result == E_SKU_ALREADY_EXISTS) {
                Toast.makeText(mHostActivity, "Product with that SKU already exists...", Toast.LENGTH_LONG).show();
            }
            
            mHostActivity.dismissProgressDialog();
            mHostActivity.finish();
        }
    }
    
    
    @SuppressWarnings("unused")
    private static final String TAG = "ProductCreateActivity";
    private static final String[] MANDATORY_USER_FIELDS = { MAGEKEY_PRODUCT_NAME, MAGEKEY_PRODUCT_PRICE,
            MAGEKEY_PRODUCT_QUANTITY, MAGEKEY_PRODUCT_DESCRIPTION, };
    
    // views
    private EditText nameV;
    private EditText skuV;
    private EditText priceV;
    private EditText quantityV;
    private EditText descriptionV;
    private EditText weightV;
    // private CheckBox statusV;
    private EditText barcodeInput;

    private final List<EditText> atrEditFields = new LinkedList<EditText>();
    private final List<Spinner> atrSpinnerFields = new LinkedList<Spinner>();

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

        // listeners
        findViewById(R.id.create_btn).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (verifyForm() == false) {
                    Toast.makeText(getApplicationContext(), "Please fill out all required fields...",
                            Toast.LENGTH_SHORT).show();
                } else {
                    createNewProduct();
                }
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

        // check category
        if (category == null || category.getId() == INVALID_CATEGORY_ID) {
            return false;
        }

        // check attribute set
        if (atrSetId == INVALID_ATTRIBUTE_SET_ID) {
            return false;
        }

        // check attribute EditText fields
        for (final EditText editField : atrEditFields) {
            if (TextUtils.isEmpty("" + editField.getText()) && editField.getTag(R.id.tkey_atr_required) == Boolean.TRUE) {
                return false;
            }
        }

        // check attribute Spinner fields
        for (final Spinner spinnerField : atrSpinnerFields) {
            if (spinnerField.getSelectedItem() == null && spinnerField.getTag(R.id.tkey_atr_required) == Boolean.TRUE) {
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

        final String name = nameV.getText().toString();
        final String price = priceV.getText().toString();
        final String description = descriptionV.getText().toString();
        final String weight = weightV.getText().toString();
        final String quantity = quantityV.getText().toString();

        data.put(MAGEKEY_PRODUCT_NAME, name);
        data.put(MAGEKEY_PRODUCT_PRICE, price);
        data.put(MAGEKEY_PRODUCT_DESCRIPTION, description);
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

    // ---
    // progress dialog (TODO y: move this to a common utilities class)

    @Override
    protected View newAtrEditView(Map<String, Object> atrData) {
        final View v = super.newAtrEditView(atrData);
        if (v == null) {
            return null;
        }
        View edit;
        if ((edit = v.findViewById(R.id.edit)) != null && edit instanceof EditText) {
            atrEditFields.add((EditText) edit);
        } else if ((edit = v.findViewById(R.id.spinner)) != null && edit instanceof Spinner) {
            atrSpinnerFields.add((Spinner) edit);
        } else {
            if (BuildConfig.DEBUG) {
                throw new RuntimeException("Unrecognized view type... " + v);
            }
        }
        return v;
    }

    @Override
    protected void removeAttributeListV() {
        super.removeAttributeListV();

        atrEditFields.clear();
        atrSpinnerFields.clear();
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
    	// Check that all necessary information exists 
    	if(validateProductInfo())
    	{
    		showProgressDialog("Creating Product & Submitting Order");
    		new CreateOrder().execute();
    	}    	
    }
    
    // Validate Necessary Product Information 
    // to create an order [Name, Price, Quantity]
    private boolean validateProductInfo()
    {
    	String message = " Inofrmation Missing ";
    	boolean result = true; 
    	
    	// Check if name is empty
    	if(TextUtils.isEmpty(nameV.getText()))
    	{
    		result =  false;
    		message += " - Product Name ";
    	}
    	
    	if(TextUtils.isEmpty(priceV.getText()))
    	{
    		result = false;
    		message += " - Price ";
    	}
    	   
    	if(TextUtils.isEmpty(quantityV.getText()))
    	{
    		result = false;
    		message += " - Quantity";
    	}	
    	    	   
    	
    	if(!result)
    	{
    		AlertDialog.Builder builder = new Builder(ProductCreateActivity.this);
			
			builder.setMessage(message);
			builder.setTitle("Missing Information");
			builder.setPositiveButton("OK", null);
			
			builder.create().show(); 
    	}
    	
    	// All Required Data Exists
    	return result;
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
            final String soldPrice = priceV.getText().toString();
            final String qty = quantityV.getText().toString();
            final String name = nameV.getText().toString();

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

		private String bookInfo = "";
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
			
			if(TextUtils.isEmpty(bookInfo))
			{
				Toast.makeText(getApplicationContext(), "No Book Found", Toast.LENGTH_SHORT).show();
				return;
			}
			
			// Show Book Details
			
			showBookInfo();
		}
		
					
		// read all Book Information from Buffer reader
		// For the List of attributes get attribute code 
		// and try to find its information in the response
		// Special Cases "ISBN-10 , ISBN-13 and Authors"
		// STRING FORMAT
		// code::value;code::value;.............. 
		private void loadBookInfo(BufferedReader reader)
		{
			String line = "";			
			try
			{
				// Copy AtrList into a temp list
				ArrayList<ViewGroup> atrViews = new ArrayList<ViewGroup>();
				for(int i=0;i<atrListV.getChildCount();i++)
					atrViews.add((ViewGroup)atrListV.getChildAt(i));
				
				
				
				while ( (line = reader.readLine()) != null) 
				{	
					// Set Line in Lower Case [Helpful in comparison and so on]
					line = line.toLowerCase();
					
					for(int i=0;i<atrViews.size();i++)
					{
						ViewGroup v = (ViewGroup) atrViews.get(i);						
						EditText value = ((EditText)v.getChildAt(1));
						
						String code = value.getTag(R.id.tkey_atr_code).toString();		// Get Code 
						String codeString = "\"" + code.replace("bk_", "").trim();		// Get Parameter to be found in string
						int lastUnderScoreIndex = codeString.lastIndexOf("_");			
						codeString = codeString.substring(0, lastUnderScoreIndex).toLowerCase();	// remove last underscore 	
						
						
						// If Line contains the Code
						if(line.contains(codeString))
						{							
							// Handling Special Cases "ISBN_10,ISBN_13"
							if(codeString.contains("isbn"))
							{
								line = reader.readLine();	// Get ISBN 
								bookInfo += code + "::" + getInfo(line, "identifier") + ";";
								atrViews.remove(i);
								break; // Break Loop --> go to read next line
							}
							
							// Handling Special Case "Authors"
					 		if(TextUtils.equals(codeString, "\"authors"))
							{
								line = reader.readLine();
								String authors = "";
								while(!line.contains("]"))
								{
									authors += line.replace("\"", "");
									line = reader.readLine();
								}				
								bookInfo += code + "::" + authors.trim() + ";";
								atrViews.remove(i);
								break; // Break Loop --> go to read next line
							}
							
							// Any Other Parameter -- get details
							bookInfo += code + "::" + getInfo(line, codeString) + ";";
							atrViews.remove(i);
							break; // Break Loop --> go to read next line
						}
					}
				}
			}
			catch(IOException excpetion)
			{
				return;
			}						
		}


		// Show Book Information in attributes
		// Loop Over attributes  get the code 
		// find the code index in bookInfo string and get the value 
		private void showBookInfo()
		{
			for(int i=0;i<atrListV.getChildCount();i++)
			{
				ViewGroup v = (ViewGroup) atrListV.getChildAt(i);						
				EditText value = ((EditText)v.getChildAt(1));
				
				// Get Code
				String code = value.getTag(R.id.tkey_atr_code).toString(); 
				
				// Get Value from BookInfo String
				// 1- get code index in book info string
				int index = bookInfo.indexOf(code);
				if(index == -1)  /// Attribute doesn't exist "Escape it"
					continue;
				
				// 2- get next index of ";"
				int endOfValIndex = bookInfo.indexOf(";", index);
				
				String attrCodeValue = bookInfo.substring(index, endOfValIndex);
				String attrValue = attrCodeValue.replace(code,"").replace("::", "");
				value.setText(attrValue);
			
				// Special Cases [Description and Title]
				if(code.toLowerCase().contains("title"))
					nameV.setText(attrValue);
				if(code.toLowerCase().contains("description"))
					descriptionV.setText(attrValue);
				
				
				if(attrValue.contains("http:") || attrValue.contains("https:"))
					Linkify.addLinks(value, Linkify.ALL);				
			}
		}
	
		// Get Book Information from line
		private String getInfo(String line,String name)
		{
			if(line.contains("https"))
				return line.replace(name, "").replace(",", "").replace("\"", "").replace(":", "").replace("https","https:").trim();
			else
				return line.replace(name, "").replace(",", "").replace("\"", "").replace(":", "").replace("http","http:").trim();
		}
	}


	
}