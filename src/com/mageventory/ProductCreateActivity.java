package com.mageventory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;

import com.mageventory.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.model.Book;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.Settings;
import com.mageventory.util.DefaultOptionsMenuHelper;

public class ProductCreateActivity extends AbsProductActivity implements OperationObserver {

    // tasks
    private static class CreateProductTask extends BaseTask<ProductCreateActivity, Void> implements OperationObserver {

        private static ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();

        private static int FAILURE = 0;
        private static int E_BAD_FIELDS = 1;
        private static int E_BAD_CATEGORY = 2;
        private static int SUCCESS = 3;

        private CountDownLatch doneSignal;
        private int state = TSTATE_NEW;
        private int createProductRequestId;
        private boolean success;
        private Bundle extras;

        public int getState() {
            return state;
        }

        public CreateProductTask(ProductCreateActivity hostActivity) {
            super(hostActivity);
            state = TSTATE_RUNNING;
        }

        @Override
        protected Integer doInBackground(Object... params) {
            final ProductCreateActivity host = getHost();
            if (host == null || isCancelled()) {
                return 0;
            }

            if (host.verifyForm() == false) {
                return E_BAD_FIELDS;
            }

            final Bundle data = new Bundle();
            final Map<String, String> extracted = host.extractCommonData();

            // user fields
            for (Map.Entry<String, String> e : extracted.entrySet()) {
                data.putString(e.getKey(), e.getValue());
            }
            data.putString(MAGEKEY_PRODUCT_SHORT_DESCRIPTION, extracted.get(MAGEKEY_PRODUCT_DESCRIPTION));

            if (host.category == null || host.category.getId() == INVALID_CATEGORY_ID) {
                return E_BAD_CATEGORY;
            }
            data.putSerializable(MAGEKEY_PRODUCT_CATEGORIES, new Object[] { String.valueOf(host.category.getId()) });

            // default values
            data.putString(MAGEKEY_PRODUCT_WEBSITE, "1");

            // can be empty
            data.putString(MAGEKEY_PRODUCT_SKU, host.skuV.getText().toString());

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
            for (EditText editField : host.atrEditFields) {
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
            for (Spinner spinnerField : host.atrSpinnerFields) {
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

            data.putInt(EKEY_PRODUCT_ATTRIBUTE_SET_ID, host.atrSetId);
            data.putSerializable(EKEY_PRODUCT_ATTRIBUTE_VALUES, atrs);

            // proceed with operation
            doneSignal = new CountDownLatch(1);
            resHelper.registerLoadOperationObserver(this);
            createProductRequestId = resHelper.loadResource(host, RES_CATALOG_PRODUCT_CREATE, null, data);
            while (true) {
                if (isCancelled()) {
                    return FAILURE;
                }
                try {
                    if (doneSignal.await(2, TimeUnit.SECONDS)) {
                        break;
                    }
                } catch (InterruptedException e) {
                    return FAILURE;
                }
            }
            resHelper.unregisterLoadOperationObserver(this);
            return success ? SUCCESS : FAILURE;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            state = TSTATE_CANCELED;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            final ProductCreateActivity host = getHost();
            if (host == null) {
                return;
            }
            if (result == SUCCESS) {
                // successful creation
                final Intent data = new Intent();
                data.putExtras(extras);
                host.setResult(RESULT_SUCCESS, data);
                host.finish();            	 
            } else if (result == FAILURE) {
                Toast.makeText(host, "Creation failed...", Toast.LENGTH_LONG).show();
            } else if (result == E_BAD_FIELDS) {
                Toast.makeText(host, "Please fill out all fields...", Toast.LENGTH_LONG).show();
            } else if (result == E_BAD_CATEGORY) {
                Toast.makeText(host, "Please select a category...", Toast.LENGTH_LONG).show();
            }
            host.dismissProgressDialog();

            state = TSTATE_TERMINATED;
        }

        @Override
        public void onLoadOperationCompleted(LoadOperation op) {
            final ProductCreateActivity host = getHost();
            if (host == null || isCancelled()) {
                return;
            }
            if (op.getOperationRequestId() == createProductRequestId) {
                success = op.getException() == null;
                extras = op.getExtras();
                doneSignal.countDown();
            }
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

    private final List<EditText> atrEditFields = new LinkedList<EditText>();
    private final List<Spinner> atrSpinnerFields = new LinkedList<Spinner>();

    // state
    private int orderCreateId;
    // XXX y: this task should be passed around as savedInstanceState, for now I turn on the orientation flag
    private CreateProductTask createTask;

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
                    createProduct();
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
        
        EditText barcodeInput = (EditText) findViewById(R.id.barcode_input);
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

    private void createProduct() {
        if (createTask == null || createTask.getState() == TSTATE_CANCELED) {
            //
        } else {
            createTask.setHost(null);
            createTask.cancel(true);
            createTask = null;
        }
        showProgressDialog("Creating product");
        createTask = new CreateProductTask(this);
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
                final String ekeyProductId = getString(R.string.ekey_product_id);
                final int productId = Integer.valueOf(product.getId());
                final Intent intent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
                intent.putExtra(ekeyProductId, productId);
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
    	if (DefaultOptionsMenuHelper.onActivityResult(this, requestCode, resultCode, intent) == false) 
    	{
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
        
    }
    
    

	/**
	 * Getting Book Details
	 * @author hussein
	 *
	 */
	private class BookInfoLoader extends AsyncTask<Object, Void, Boolean> {

		private Book bookInfo;
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
				
				String line = "";
				bookInfo = new Book();
				bookInfo.setTitle("NONE");
				
				while ( (line = reader.readLine()) != null) {
					
					/// Get Book ID
					if(line.contains("\"id\""))
					{
						bookInfo.setId(line.replace("id", "").replace(",", "").replace("\"", "").replace(":", "").trim());
					}
					
					/// Get Book Self Link
					if(line.contains("\"selfLink\""))
					{
						bookInfo.setSelfLink(line.replace("selfLink", "").replace(",", "").replace("\"", "").replace(":", "").replace("https", "https:").trim());
					}
					
					/// Read Book Title
					if(line.contains("\"title\""))
					{						
						bookInfo.setTitle(line.replace("title", "").replace(",","").replace(":", "").replace("\"", "").trim());
					}
										
					/// Read Book Authors
					if(line.contains("\"authors\""))
					{
						String authors = "";
						line = reader.readLine();
						while(!(line.contains("]")))
						{
							authors += line.replace("\"", "").trim();
							line = reader.readLine();
						}
						
						bookInfo.setAuthors(authors.trim());
					}
					
					/// Read Publisher Name
					if(line.contains("\"publisher\""))
					{						
						bookInfo.setPublisher(line.replace("publisher", "").replace(",","").replace(":", "").replace("\"", "").trim());
					}
					
					
					/// Read Book Published Date
					if(line.contains("\"publishedDate\""))
					{
						bookInfo.setPublishDate(line.replace("publishedDate", "").replace(":","").replace(",","").replace("\"","").trim());
					}
					
					/// Read Book Description
					if(line.contains("\"description\""))
					{
						bookInfo.setDescription(line.replace("description", "").replace(":","").replace(","," ").replace("\"","").trim());
					}

					/// Read Book ISBN_10 URL
					if(line.contains("\"ISBN_10\""))
					{
						line = reader.readLine();
						bookInfo.setiSBN_10(line.replace("identifier", "").replace(":","").replace(",","").replace("\"","").trim());
					}
					
					/// Read Book ISBN_13 URL
					if(line.contains("\"ISBN_13\""))
					{
						line = reader.readLine();
						bookInfo.setiSBN_13(line.replace("identifier", "").replace(":","").replace(",","").replace("\"","").trim());
					}

					/// Read Page Count
					if(line.contains("\"pageCount\""))
					{
						bookInfo.setPageCount(line.replace("pageCount", "").replace(":","").replace(",","").replace("\"","").trim());
					}
					
					/// Read Average Rating
					if(line.contains("\"averageRating\""))
					{
						bookInfo.setAverageRate(line.replace("averageRating", "").replace(":","").replace(",","").replace("\"","").trim());
					}
					
					/// Read Rating Count
					if(line.contains("\"ratingsCount\""))
					{
						bookInfo.setRateCount(line.replace("ratingsCount", "").replace(":","").replace(",","").replace("\"","").trim());
					}

					/// Read Book Small Thumbnail URL
					if(line.contains("\"smallThumbnail\""))
					{
						bookInfo.setSmallThumbnail(line.replace("smallThumbnail", "").replace(":","").replace(",","").replace("\"","").trim().replace("http", "http:"));
					}
										
					/// Read Book Thumbnail URL
					if(line.contains("\"thumbnail\""))
					{
						bookInfo.setThumbnail(line.replace("thumbnail", "").replace(":","").replace(",","").replace("\"","").trim().replace("http", "http:"));
						image = BitmapFactory.decodeStream((new URL(bookInfo.getSmallThumbnail())).openStream());						
					}
					
					/// Read Book Language
					if(line.contains("\"language\""))
					{
						bookInfo.setLanguage(line.replace("language", "").replace(":","").replace(",","").replace("\"","").trim().replace("http", "http:"));
					}
										
					/// Read Book previewLink URL
					if(line.contains("\"previewLink\""))
					{
						bookInfo.setPreviewLink(line.replace("previewLink", "").replace(",","").replace("\"","").trim().replace(":","").replace("http", "http:"));
					}
					
					/// Read Book infoLink URL
					if(line.contains("\"infoLink\""))
					{
						bookInfo.setInfoLink(line.replace("infoLink", "").replace(",","").replace("\"","").trim().replace(":","").replace("http", "http:"));
					}
					
					/// Read Book Viewability
					if(line.contains("\"viewability\""))
					{
						bookInfo.setViewability(line.replace("viewability", "").replace(":","").replace(",","").replace("\"","").trim().replace("http", "http:"));
					}
					
					/// Read Book Embeddable
					if(line.contains("\"embeddable\""))
					{
						bookInfo.setEmbeddable(line.replace("embeddable", "").replace(":","").replace(",","").replace("\"","").trim().replace("http", "http:"));
					}
					
					/// Read Book WebReaderLink
					if(line.contains("\"webReaderLink\""))
					{
						bookInfo.setWebReadedLink(line.replace("webReaderLink", "").replace(":","").replace(",","").replace("\"","").trim().replace("http", "http:"));
					}
					
					/// Read Book WebReaderLink
					if(line.contains("\"textSnippet\""))
					{
						bookInfo.setTextSnippet(line.replace("textSnippet", "").replace(":","").replace(",","").replace("\"","").trim().replace("http", "http:"));
					}
				}
				
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
			
			if(bookInfo != null)
			{								
				if(TextUtils.equals(bookInfo.getTitle(),"NONE"))
				{
					Toast.makeText(getApplicationContext(), "No Book Found", Toast.LENGTH_SHORT).show();
					return;
				}
				
				// Show Book Details
				nameV.setText(bookInfo.getTitle());
				descriptionV.setText(bookInfo.getDescription());
				
				for(int i=0;i<atrListV.getChildCount();i++)
				{					
					ViewGroup v = (ViewGroup) atrListV.getChildAt(i);
				
					// View ID
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Id"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getId());
					}
					
					// View Self Link
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Self"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getSelfLink());
					}
					
					// View Title
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Title"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getTitle());
					}
					
					// View Authors
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Author"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getAuthors());
					}
					
					// View Publisher
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Publisher"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getPublisher());
					}
					
					// View Published Date
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("date"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getPublishDate());
					}
					
					// View Description
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Description"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getDescription());
					}
					
					// View ISBN 10
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("10"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getiSBN_10());
					}
					
					// View ISBN 13
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("13"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getiSBN_13());
					}
					
					// View Page Count
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Pagecount"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getPageCount());
					}
					
					// View Average Rating
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Average"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getAverageRate());
					}
					
					// View Ratings count
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Ratingcount"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getAverageRate());
					}
					
					// View small Thumbnail
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Small"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getSmallThumbnail());										
					}
					
					// View Thumbnail
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Thumbnail"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getThumbnail());				
						((ImageView)v.getChildAt(2)).setImageBitmap(image);										
					}
					
					// View Language
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Language"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getLanguage());						
					}
					
					// View Preview Link
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Preview"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getPreviewLink());						
					}
					
					// View Info Link
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Info"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getInfoLink());						
					}
					
					// View viewabilility
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Viewability"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getViewability());						
					}
					
					// View Embeddable
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Embedd"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getEmbeddable());						
					}
					
					// View WebReader Link
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Web"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getWebReadedLink());
						
					}
					
					// View Text Snippet
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("Snippet"))
					{
						((EditText)v.getChildAt(1)).setText(bookInfo.getTextSnippet());						
					}	
						
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("link"))
						Linkify.addLinks(((EditText)v.getChildAt(1)), Linkify.ALL);
					if(((TextView)v.findViewById(R.id.label)).getText().toString().contains("humbnail"))
						Linkify.addLinks(((EditText)v.getChildAt(1)), Linkify.ALL);
				}				
			}
		}
			
			
		}

}
