package com.mageventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import com.mageventory.util.Log;
import com.mageventory.util.Util;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.mageventory.model.Category;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.Settings;
import com.mageventory.util.DefaultOptionsMenuHelper;

public class ProductEditActivity extends AbsProductActivity {

	@Override
    protected void onCategoryLoadSuccess() {
        super.onCategoryLoadSuccess();
        mapCategory();
    }
	
    private static class LoadProduct extends BaseTask<ProductEditActivity, Product> implements OperationObserver {

        private CountDownLatch doneSignal;
        private boolean forceRefresh = false;
        private int requestId = INVALID_REQUEST_ID;
        private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
        private int state = TSTATE_NEW;
        private boolean success;

        @Override
        protected Integer doInBackground(Object... args) {
            final String[] params = new String[2];
            params[0] = GET_PRODUCT_BY_SKU; // ZERO --> Use Product ID , ONE --> Use Product SKU
            params[1] = args[0].toString();
            forceRefresh = (Boolean) args[1];

            ProductEditActivity host = getHost();
            if (host == null || isCancelled()) {
                return 0;
            }

            final ProductEditActivity finalHost = host;
            finalHost.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    finalHost.onProductLoadStart();
                }
            });
            
            if (forceRefresh || resHelper.isResourceAvailable(host, RES_PRODUCT_DETAILS, params) == false) {
                // load
            	
                doneSignal = new CountDownLatch(1);
                resHelper.registerLoadOperationObserver(this);
                requestId = resHelper.loadResource(host, RES_PRODUCT_DETAILS, params);

                while (true) {
                    if (isCancelled()) {
                        return 0;
                    }
                    try {
                        if (doneSignal.await(10, TimeUnit.SECONDS)) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        return 0;
                    }
                }
                resHelper.unregisterLoadOperationObserver(this);
            } else {
                success = true;
            }

            host = getHost();
            if (host == null || isCancelled()) {
                return 0;
            }

            if (success) {
                final Product data = resHelper.restoreResource(host, RES_PRODUCT_DETAILS, params);
                setData(data);

                finalHost.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (data != null) {
                            finalHost.onProductLoadSuccess();
                        } else {
                            finalHost.onProductLoadFailure();
                        }
                    }
                });
            } else {
                finalHost.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finalHost.onProductLoadFailure();
                    }
                });
            }
            
            host = getHost();
            if (host == null || isCancelled()) {
                return 0;
            }
            if (forceRefresh) {
                host.loadAttributeSetsAndCategories(true);
            }
            return 1;
        }

        public int getState() {
            return state;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            state = TSTATE_CANCELED;
        }

        @Override
        public void onLoadOperationCompleted(LoadOperation op) {
            if (op.getOperationRequestId() == requestId) {
                success = op.getException() == null;
                doneSignal.countDown();

                final Activity a = getHost();
                if (a != null) {
                    resHelper.stopService(a, false);
                }
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            state = TSTATE_TERMINATED;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            state = TSTATE_RUNNING;
        }

    }

    private static class UpdateProduct extends BaseTask<ProductEditActivity, Object> implements MageventoryConstants,
            OperationObserver {

        private static final String TAG = "UpdateProduct";
        private int updateProductRequestId = INVALID_REQUEST_ID;
        private int state = TSTATE_NEW;

        @Override
        protected Integer doInBackground(Object... arg0) {
            ProductEditActivity host;
            host = getHost();
            if (host == null && isCancelled()) {
                return 0;
            }

            try {
                final Bundle bundle = new Bundle();
                
                if (TextUtils.isEmpty(host.nameV.getText().toString()))
                {
                	bundle.putString(MAGEKEY_PRODUCT_NAME, "n/a");	
                }
                else
                {
                	bundle.putString(MAGEKEY_PRODUCT_NAME, host.nameV.getText().toString());	
                }
                
                
                if (TextUtils.isEmpty(host.priceV.getText().toString()))
                {
                	bundle.putString(MAGEKEY_PRODUCT_PRICE, "0");	
                }
                else
                {
                	bundle.putString(MAGEKEY_PRODUCT_PRICE, host.priceV.getText().toString());
                }
                
                bundle.putString(MAGEKEY_PRODUCT_WEBSITE, "1"); // y TODO: hard-coded website...
                
                if (TextUtils.isEmpty(host.descriptionV.getText().toString()))
                {
                	bundle.putString(MAGEKEY_PRODUCT_DESCRIPTION, "n/a");
                    bundle.putString(MAGEKEY_PRODUCT_SHORT_DESCRIPTION, "n/a");	
                }
                else
                {
                	bundle.putString(MAGEKEY_PRODUCT_DESCRIPTION, host.descriptionV.getText().toString());
                    bundle.putString(MAGEKEY_PRODUCT_SHORT_DESCRIPTION, host.descriptionV.getText().toString());	
                }
                
                bundle.putString(MAGEKEY_PRODUCT_STATUS, host.statusV.isChecked() ? "1" : "0");
                
                if (TextUtils.isEmpty(host.weightV.getText().toString()))
                {
                	bundle.putString(MAGEKEY_PRODUCT_WEIGHT, "0");
                }
                else
                {
                	bundle.putString(MAGEKEY_PRODUCT_WEIGHT, host.weightV.getText().toString());	
                }
                
                bundle.putString(MAGEKEY_PRODUCT_SKU, host.skuV.getText().toString());
                
                
                if (host.category != null && host.category.getId() != INVALID_CATEGORY_ID) {
                	bundle.putSerializable(MAGEKEY_PRODUCT_CATEGORIES, new Object[] { String.valueOf(host.category.getId()) });
                }

                bundle.putString(MAGEKEY_PRODUCT_QUANTITY, host.quantityV.getText().toString());

                
                // generated
                String quantity = bundle.getString(MAGEKEY_PRODUCT_QUANTITY);
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

                bundle.putString(MAGEKEY_PRODUCT_MANAGE_INVENTORY, inventoryControl);
                bundle.putString(MAGEKEY_PRODUCT_IS_IN_STOCK, isInStock);
                
                // bundle attributes
                final HashMap<String, Object> atrs = new HashMap<String, Object>();
                for (final View v : host.atrCodeToView.values()) {
                    if (v instanceof EditText) {
                        final EditText editField = (EditText) v;
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
                    } else if (v instanceof Spinner) {
                        final Spinner spinnerField = (Spinner) v;

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
                }
                
                atrs.put("product_barcode_", getHost().barcodeInput.getText().toString());

                // bundle.putInt(EKEY_PRODUCT_ATTRIBUTE_SET_ID, host.atrSetId);
                bundle.putSerializable(EKEY_PRODUCT_ATTRIBUTE_VALUES, atrs);

                ResourceServiceHelper.getInstance().registerLoadOperationObserver(this);

                updateProductRequestId = ResourceServiceHelper.getInstance().loadResource(host,
                        RES_CATALOG_PRODUCT_UPDATE, new String[] { String.valueOf(host.productId) }, bundle);
                return 1;
            } catch (Exception ex) {
                Log.w(TAG, "" + ex);
                host.dismissProgressDialog();
                return 0;
            }
        }

        public int getState() {
            return state;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            state = TSTATE_CANCELED;
        }

        @Override
        public void onLoadOperationCompleted(LoadOperation op) {
            final ProductEditActivity host = getHost();
            if (host == null || isCancelled()) {
                return;
            }
            if (op.getOperationRequestId() == updateProductRequestId) {
                host.dismissProgressDialog();

                if (op.getException() == null) {
                    Toast.makeText(host, "Product updated", Toast.LENGTH_LONG).show();
                    host.setResult(RESULT_CHANGE);
                } else {
                    Toast.makeText(host, "Error occurred while uploading: " + op.getException(), Toast.LENGTH_LONG)
                            .show();
                }

                ResourceServiceHelper.getInstance().unregisterLoadOperationObserver(this);
                ResourceServiceHelper.getInstance().stopService(host, false);
                
                // Load Product Details Screen
                Intent newIntent = new Intent(host.getApplicationContext(),ProductDetailsActivity.class);
                newIntent.putExtra(host.getString(R.string.ekey_product_sku), host.productSKU);
                newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                
                host.startActivity(newIntent);
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            state = TSTATE_TERMINATED;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            state = TSTATE_RUNNING;

            final ProductEditActivity host = getHost();
            if (host != null) {
                host.showProgressDialog("Updating product...");
            }
        }

    }

    // views
    private EditText descriptionV;
    private EditText nameV;
    private EditText priceV;
    private EditText quantityV;
    private EditText skuV;
    private EditText weightV;
    private CheckBox statusV;
    private EditText barcodeInput;

    // state
    private LoadProduct loadProductTask;
    private UpdateProduct updateProductTask;
    private int productId;
    private String productSKU;
    private int categoryId;
    private ProgressDialog progressDialog;

    private void dismissProgressDialog() {
        if (progressDialog == null) {
            return;
        }
        progressDialog.dismiss();
        progressDialog = null;
    }

    @Override
    protected int getContentView() {
        return R.layout.product_edit;
    }

    private Product getProduct() {
        if (loadProductTask == null) {
            return null;
        }
        return loadProductTask.getData();
    }

    private void loadProduct(final String productId, final boolean forceRefresh) {
        if (loadProductTask != null) {
            if (loadProductTask.getState() == TSTATE_RUNNING) {
                return;
            } else if (forceRefresh == false && loadProductTask.getState() == TSTATE_TERMINATED
                    && loadProductTask.getData() != null) {
                return;
            }
        }
        if (loadProductTask != null) {
            loadProductTask.cancel(true);
        }
        loadProductTask = new LoadProduct();
        loadProductTask.setHost(this);
        loadProductTask.execute(productId, forceRefresh);
    }

    private void mapCategory()
    {
        final Runnable map = new Runnable() {
            public void run() {
                
            	final Map<String, Object> rootCategory = getCategories();
            	if (rootCategory != null && !rootCategory.isEmpty()) {
            		for (Category cat: Util.getCategorylist(rootCategory, null))
            		{
            			if ( cat.getId() == categoryId )
            			{
            				category = cat;
            				categoryV.setText(cat.getFullName());
            			}
            		}	
            	}
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            map.run();
        } else {
            runOnUiThread(map);
        }
    }
    
    private void mapData(final Product p) {
        if (p == null) {
            return;
        }
        final Runnable map = new Runnable() {
            public void run() {
                try {
                    categoryId = Integer.parseInt(p.getMaincategory());
                } catch (Throwable e) {
                    categoryId = INVALID_CATEGORY_ID;
                }
                categoryV.setText("");
                
            	final Map<String, Object> rootCategory = getCategories();
            	if (rootCategory != null && !rootCategory.isEmpty()) {
            		for (Category cat: Util.getCategorylist(rootCategory, null))
            		{
            			if ( cat.getId() == categoryId )
            			{
            				category = cat;
            				categoryV.setText(cat.getFullName());
            			}
            		}	
            	}
                
                descriptionV.setText(p.getDescription());
                nameV.setText(p.getName());
                priceV.setText(p.getPrice());
                weightV.setText(p.getWeight().toString());
                statusV.setChecked(p.getStatus() == 1 ? true : false);
                skuV.setText(p.getSku());
                quantityV.setText(p.getQuantity().toString());

                String total = "";
                if (p.getQuantity().compareToIgnoreCase("") != 0) {
                    total = String.valueOf(Double.valueOf(p.getPrice()) * Double.valueOf(p.getQuantity()));
                    String[] totalParts = total.split("\\.");
                    if (totalParts.length > 1) {
                        if ((!totalParts[1].contains("E")) && (Integer.valueOf(totalParts[1]) == 0))
                            total = totalParts[0];
                    }
                }

                final int atrSetId = p.getAttributeSetId();
                selectAttributeSet(atrSetId, false);
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            map.run();
        } else {
            runOnUiThread(map);
        }
    }

    @Override
    protected void onAttributeListLoadSuccess() {
        super.onAttributeListLoadSuccess();

        final Product product = getProduct();
        if (product == null) {
            return;
        }

        final List<Map<String, Object>> atrs = getAttributeList();
        if (atrs == null) {
            return;
        }

        for (Map<String, Object> atr : atrs) {
            final String code = (String) atr.get(MAGEKEY_ATTRIBUTE_CODE);
            if (TextUtils.isEmpty(code)) {
                continue;
            }
            if(TextUtils.equals("product_barcode_", code))
            {
            	if (product.getData().containsKey(code))
            	{
            		barcodeInput.setText(product.getData().get(code).toString());
            	}
            	else
            	{
            		barcodeInput.setText("");
            	}
            }
            else
            {
            	mapAtrDataToView(atr, product.getData().get(code));
            }
            
        }
    }

    
    private OnLongClickListener scanBarcodeOnClickL = new OnLongClickListener() {
		
		@Override
		public boolean onLongClick(View v) {
            Intent scanInt = new Intent("com.google.zxing.client.android.SCAN");
            startActivityForResult(scanInt, SCAN_BARCODE);
            return true;
		}
	};
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // map views
        descriptionV = (EditText) findViewById(R.id.description_input);
        nameV = (EditText) findViewById(R.id.product_name_input);
        priceV = (EditText) findViewById(R.id.product_price_input);
        quantityV = (EditText) findViewById(R.id.quantity_input);
        skuV = (EditText) findViewById(R.id.product_sku_input);
        weightV = (EditText) findViewById(R.id.weight_input);
        statusV = (CheckBox) findViewById(R.id.status);

        // extras
        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            throw new IllegalStateException();
        }
        productId = extras.getInt(getString(R.string.ekey_product_id), INVALID_PRODUCT_ID);
        productSKU = extras.getString(getString(R.string.ekey_product_sku));
        loadProduct(productSKU, false);

        // listeners

        // attributeSetV.setClickable(false); // attribute set cannot be changed
        attributeSetV.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Attribute set cannot be changed...", Toast.LENGTH_SHORT)
                        .show();
            }
        });

        findViewById(R.id.update_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateProduct(false);
            }
        });
    
        barcodeInput = (EditText)findViewById(R.id.barcode_input);
        barcodeInput.setOnLongClickListener(scanBarcodeOnClickL);
        
    }

    private void onProductLoadFailure() {
        dismissProgressDialog();
    }

    private void onProductLoadStart() {
        showProgressDialog("Loading product...");
    }

    private void onProductLoadSuccess() {
        dismissProgressDialog();
        mapData(getProduct());
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

    private void updateProduct(boolean force) {
        if (force == false && updateProductTask != null && updateProductTask.getState() == TSTATE_RUNNING) {
            return;
        }
        if (updateProductTask != null) {
            updateProductTask.cancel(true);
        }
        updateProductTask = new UpdateProduct();
        updateProductTask.setHost(this);
        updateProductTask.execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            loadProduct(productSKU, true);
            return true;
        }
        return DefaultOptionsMenuHelper.onOptionsItemSelected(this, item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return DefaultOptionsMenuHelper.onCreateOptionsMenu(this, menu);
    }
    
    /**
     * Handles the Scan Process Result --> Get Barcode result and set it in GUI  
     **/
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {		
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SCAN_BARCODE) {
            if (resultCode == RESULT_OK) {
            	String contents = data.getStringExtra("SCAN_RESULT");               
                // Set Barcode in Product Barcode TextBox
				barcodeInput.setText(contents);
				
               } else if (resultCode == RESULT_CANCELED) {
                // Do Nothing
            }
		}
	}        
}
