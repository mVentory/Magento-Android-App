package com.mageventory;

import java.security.KeyStore.LoadStoreParameter;

import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ExpressSellActivity extends BaseActivity implements MageventoryConstants,OperationObserver{

	ProgressDialog progressDialog;
	Product P = new Product();
	int orderCreateID;
	ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance(); 
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);						
		setContentView(R.layout.express_sell);
		
		// read Description and SKU and Price 
		P.setName(getIntent().getStringExtra(MAGEKEY_PRODUCT_NAME));
		P.setDescription(getIntent().getStringExtra(MAGEKEY_PRODUCT_DESCRIPTION));
		P.setPrice(Double.valueOf((getIntent().getStringExtra(MAGEKEY_PRODUCT_PRICE))));
		P.setQuantity((getIntent().getStringExtra(MAGEKEY_PRODUCT_QUANTITY)));
		P.setSku((getIntent().getStringExtra(MAGEKEY_PRODUCT_SKU)));
		P.setWeight(Double.valueOf((getIntent().getStringExtra(MAGEKEY_PRODUCT_WEIGHT))));
		P.addCategory((getIntent().getStringExtra(MAGEKEY_PRODUCT_CATEGORIES)));
		
		
		((EditText) findViewById(R.id.product_sku_input_express)).setText(P.getSku());
		((EditText) findViewById(R.id.description_input_express)).setText(P.getDescription());
		((EditText) findViewById(R.id.product_price_input_express)).setText(P.getPrice());
		
		Button createButton = (Button) findViewById(R.id.express_sell_sold_button);
		createButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Create new Product and Order
				
			}
		});		
	}
	
	
	
	/**
	 * 	Create Order
	 */
	private void createOrder() {
		showProgressDialog("Submitting Order");
		new CreateOrder().execute();
	}
	
	private void dismissProgressDialog() {
		if (progressDialog == null) {
			return;
		}
		progressDialog.dismiss();
		progressDialog = null;
	}
	
	private void showProgressDialog(final String message) {
		if (progressDialog != null) {
			return;
		}
		progressDialog = new ProgressDialog(ExpressSellActivity.this);
		progressDialog.setMessage(message);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.show();
	}
	
	
	
	
	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		resHelper.unregisterLoadOperationObserver(this);
	}



	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		resHelper.registerLoadOperationObserver(this);
	}






	/**
	 * Create Order Invoice
	 * @author hussein
	 *
	 */
	private class CreateOrder extends AsyncTask<Integer, Integer, String> {

		int productID = 0;
		
		@Override
		protected String doInBackground(Integer... ints) {
			
			// 2- Set Product Information
			final String sku = P.getSku();
			String soldPrice = ((EditText)findViewById(R.id.product_price_input_express)).getText().toString();
			final String qty = ((EditText)findViewById(R.id.quantity_input_express)).getText().toString();
												
			try {
				final Bundle bundle = new Bundle();
				/* PRODUCT INFORMAITON */
				bundle.putString(MAGEKEY_PRODUCT_SKU,sku);
				bundle.putString(MAGEKEY_PRODUCT_QUANTITY, qty);
				bundle.putString(MAGEKEY_PRODUCT_PRICE, soldPrice);
				
				if(resHelper.isResourceAvailable(ExpressSellActivity.this, RES_CART_ORDER_CREATE, null))
					productID = resHelper.restoreResource(ExpressSellActivity.this, RES_CART_ORDER_CREATE, null);
				else
					orderCreateID = resHelper.loadResource(ExpressSellActivity.this,RES_CART_ORDER_CREATE, null, bundle);
				
				return null;
			} catch (Exception e) {
				Log.w("ExpressSellActivity","" + e);
				return null;
			}			
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(String result) {
			if(productID != 0)
			{
				// Show Product Details 
				final String ekeyProductId = getString(R.string.ekey_product_id);
				Intent newInt = new Intent(getApplicationContext(), ProductDetailsActivity.class);
				 newInt.putExtra(EKEY_PRODUCT_ID, productID);
                 startActivity(newInt);							
			}			
		}		
		
		
		
	}






	
	
	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		// TODO Auto-generated method stub
		if(op.getOperationRequestId() == orderCreateID)
		{
			
		}
	}	

	
	
}
