package com.mageventory;

import java.util.Scanner;

import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;

import android.R.integer;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

public class ScanActivity extends BaseActivity implements MageventoryConstants,OperationObserver {

	ProgressDialog progressDialog;	
	private int loadRequestID;
	private String sku;
	private boolean skuFound;
	private boolean scanDone;
	private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.scan_activity);				
		// Start QR Code Scanner
		
		if(savedInstanceState == null)		
		{
			scanDone = false;
			Intent scanInt = new Intent("com.google.zxing.client.android.SCAN");
			scanInt.putExtra("SCAN_MODE", "QR_CODE_MODE");		
			startActivityForResult(scanInt,SCAN_QR_CODE);			
		}
		else
			scanDone = true;
	}
			
	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		if (op.getOperationRequestId() == loadRequestID)
		{
			dismissProgressDialog();
			getInfo();
		}
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		outState.putBoolean(SCAN_DONE, true);
		super.onSaveInstanceState(outState);
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
		if(scanDone)
		{
			getInfo();	
		}
	}
	
	private void getInfo()
	{
		if(skuFound)
		{
			showProgressDialog("Checking........");
			new ProductInfoLoader().execute(sku);
		}
		else
		{
			// No SKU Back to Home
			Intent newIntent = new Intent(getApplicationContext(),MainActivity.class);
			startActivity(newIntent);
		}
	}


	private void showProgressDialog(final String message) {
		if (progressDialog != null) {
			return;
		}
		progressDialog = new ProgressDialog(ScanActivity.this);
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
	
	/**
	 *	Handling Scan Result 
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == SCAN_QR_CODE)
		{
			scanDone = true;
			if (resultCode == RESULT_OK) {
				String contents = data.getStringExtra("SCAN_RESULT");
	            String [] urlData = contents.split("/");
	            if(urlData.length > 0)
	            {
	            	sku = urlData[urlData.length -1];
	            	skuFound = true;	            	
	            }
	            else
	            {
	            	Toast.makeText(getApplicationContext(), "Not Valid", Toast.LENGTH_SHORT).show();
	            	skuFound = false;
	            	return;
	            }
	            	           	            	            	           
	        } else if (resultCode == RESULT_CANCELED) {
	            // Do Nothing
	        }	
		}
		
	}

	
	/**
	 * Getting Product Details
	 * @author hussein
	 *
	 */
	private class ProductInfoLoader extends AsyncTask<Object, Void, Boolean> {

		private Product p;
		private String sku;
		
		@Override
		protected Boolean doInBackground(Object... args) {			
			final String[] params = new String[2];
			params[0] = GET_PRODUCT_BY_SKU; // ZERO --> Use Product ID , ONE --> Use Product SKU 
			params[1] = String.valueOf(args[0]) ;
			sku = String.valueOf(args[0]) ;
			if (resHelper.isResourceAvailable(ScanActivity.this, RES_PRODUCT_DETAILS, params)) {
				p = resHelper.restoreResource(ScanActivity.this, RES_PRODUCT_DETAILS, params);
				return Boolean.TRUE;
			} else {
				loadRequestID = resHelper.loadResource(ScanActivity.this, RES_PRODUCT_DETAILS, params);
				return Boolean.FALSE;
			}			
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if(p != null)
			{
				// One Scan Completed its Action
				// Mark it as history
				resHelper.markResourceAsOld(ScanActivity.this, RES_PRODUCT_DETAILS);
				
				// Check if Data Product Has a valid ID or not
				if((p.getId().compareToIgnoreCase(String.valueOf(INVALID_PRODUCT_ID))) == 0)
				{
					// Invalid Product ID --> Product Not Found
					// Start a new Activity --> New Product
					Intent newIntent = new Intent(getApplicationContext(),ProductCreateActivity.class);
					newIntent.putExtra(PASSING_SKU,true);
					newIntent.putExtra(MAGEKEY_PRODUCT_SKU, sku);
					startActivity(newIntent);
				}
				else
				{
					// Product Exists --> Show Product Details
					final String ekeyProductSKU = getString(R.string.ekey_product_sku);
					final String SKU = p.getSku();
					final Intent intent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
					intent.putExtra(ekeyProductSKU, SKU);
					
					startActivity(intent);										
				}
			}
		}
			
			
		}
		
	
}
