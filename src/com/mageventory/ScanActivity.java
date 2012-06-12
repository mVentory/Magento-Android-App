package com.mageventory;

import java.util.Scanner;

import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;

import android.R.integer;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

public class ScanActivity extends BaseActivity implements MageventoryConstants, OperationObserver {

	ProgressDialog progressDialog;
	private int loadRequestID;
	private String sku;
	private boolean skuFound;
	private boolean scanDone;
	private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
	private boolean isActivityAlive;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.scan_activity);
		// Start QR Code Scanner

		if (savedInstanceState == null) {
			scanDone = false;
			Intent scanInt = new Intent("com.google.zxing.client.android.SCAN");
			scanInt.putExtra("SCAN_MODE", "QR_CODE_MODE");
			startActivityForResult(scanInt, SCAN_QR_CODE);
		} else
			scanDone = true;

		isActivityAlive = true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		isActivityAlive = false;
	}

	@Override
	public void onLoadOperationCompleted(final LoadOperation op) {
		if (op.getOperationRequestId() == loadRequestID) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (isActivityAlive) {
						dismissProgressDialog();
						finish();

						if (op.getException() != null) {
							dismissProgressDialog();
							Toast.makeText(ScanActivity.this, "" + op.getException(), Toast.LENGTH_LONG).show();
						} else {
							final String ekeyProductSKU = getString(R.string.ekey_product_sku);
							final Intent intent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							intent.putExtra(ekeyProductSKU, sku);

							startActivity(intent);
						}
					}
				}
			});
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		outState.putBoolean(SCAN_DONE, true);
		super.onSaveInstanceState(outState);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		resHelper.unregisterLoadOperationObserver(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		resHelper.registerLoadOperationObserver(this);
		if (scanDone) {
			getInfo();
		}
	}

	private void getInfo() {
		if (skuFound) {
			showProgressDialog("Checking........");
			new ProductInfoLoader().execute(sku);
		} else {
			finish();
		}
	}

	private void showProgressDialog(final String message) {
		if (progressDialog != null) {
			return;
		}
		progressDialog = new ProgressDialog(ScanActivity.this);
		progressDialog.setMessage(message);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(true);
		progressDialog.show();
		progressDialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				ScanActivity.this.finish();
			}
		});
	}

	private void dismissProgressDialog() {
		if (progressDialog == null) {
			return;
		}
		progressDialog.dismiss();
		progressDialog = null;
	}

	/**
	 * Handling Scan Result
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == SCAN_QR_CODE) {
			scanDone = true;
			if (resultCode == RESULT_OK) {
				String contents = data.getStringExtra("SCAN_RESULT");
				String[] urlData = contents.split("/");
				if (urlData.length > 0) {
					sku = urlData[urlData.length - 1];
					skuFound = true;
				} else {
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
	 * 
	 * @author hussein
	 * 
	 */
	private class ProductInfoLoader extends AsyncTask<Object, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Object... args) {
			final String[] params = new String[2];
			params[0] = GET_PRODUCT_BY_SKU; // ZERO --> Use Product ID , ONE -->
											// Use Product SKU
			params[1] = String.valueOf(args[0]);
			sku = String.valueOf(args[0]);
			if (JobCacheManager.productDetailsExist(params[1])) {
				return Boolean.TRUE;
			} else {
				loadRequestID = resHelper.loadResource(ScanActivity.this, RES_PRODUCT_DETAILS, params);
				return Boolean.FALSE;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result.booleanValue() == true) {
				if (isActivityAlive) {
					final String ekeyProductSKU = getString(R.string.ekey_product_sku);
					final Intent intent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.putExtra(ekeyProductSKU, sku);

					dismissProgressDialog();
					startActivity(intent);
					finish();
				}
			}
		}

	}

}
