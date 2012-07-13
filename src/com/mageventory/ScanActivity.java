package com.mageventory;

import java.util.Scanner;

import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.resprocessor.ProductDetailsProcessor.ProductDetailsLoadException;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;

import android.R.integer;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

public class ScanActivity extends BaseActivity implements MageventoryConstants, OperationObserver {

	ProgressDialog progressDialog;
	private int loadRequestID;
	private String sku;
	private String labelUrl;
	private boolean skuFound;
	private boolean scanDone;
	private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
	private boolean isActivityAlive;

	public static String getDomainNameFromUrl(String url)
	{
		if (url == null)
			return null;
		
		int index;
		String domain;
		
		domain = url;
		
		index = domain.indexOf("://");
		
		if (index != -1)
		{
			domain = domain.substring(index+"://".length(), domain.length());
		}
		
		index = domain.indexOf("/");
		
		if (index != -1)
		{
			domain = domain.substring(0, index);
		}
	
		return domain;
	}
	
	/* Validate the label against the current url in the settings. If they don't match return false. */
	public static boolean isLabelValid(Context c, String label)
	{
		Settings settings = new Settings(c);
		String settingsUrl = settings.getUrl();
		
		String settingsDomainName = getDomainNameFromUrl(settingsUrl);
		String skuDomainName = getDomainNameFromUrl(label);
		
		if (TextUtils.equals(settingsDomainName, skuDomainName))
		{
			return true;	
		}
		else
		{
			return false;
		}
	}
	
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
	
	private void launchProductDetails()
	{
		final String ekeyProductSKU = getString(R.string.ekey_product_sku);
		final Intent intent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(ekeyProductSKU, sku);

		startActivity(intent);
	}
	
	private void launchProductCreate(boolean skuExistsOnServerUncertainty)
	{
		final String ekeyProductSKU = getString(R.string.ekey_product_sku);
		final String ekeySkuExistsOnServerUncertainty = getString(R.string.ekey_sku_exists_on_server_uncertainty);
		final Intent intent = new Intent(getApplicationContext(), ProductCreateActivity.class);
		
		intent.putExtra(ekeyProductSKU, sku);
		intent.putExtra(ekeySkuExistsOnServerUncertainty, skuExistsOnServerUncertainty);
		
		startActivity(intent);
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
							
							if (((ProductDetailsLoadException)op.getException()).getFaultCode() ==
									ProductDetailsLoadException.ERROR_CODE_PRODUCT_DOESNT_EXIST)
							{
								/* Show new product activity withOUT information saying that we are not sure if
								 * the product is on the server or not (we know it is not) */
								launchProductCreate(false);
							}
							else
							{
								/* Show new product activity WITH information saying that we are not sure if
								 * the product is on the server or not (we really don't know, we just received some strange exception) */
								launchProductCreate(true);
							}
							
						} else {
							launchProductDetails();
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
		super.onResume();
		resHelper.registerLoadOperationObserver(this);
		if (scanDone) {
			getInfo();
		}
	}

	public void showInvalidLabelDialog(String settingsDomainName, String skuDomainName) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Error");
		alert.setMessage("Wrong label. Expected domain name: '" + settingsDomainName + "' found: '" + skuDomainName +"'" );

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});

		AlertDialog srDialog = alert.create();
		srDialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				finish();
			}
		});
		srDialog.show();
	}
	
	private void getInfo() {
		if (skuFound) {
			
			if (isLabelValid(this, labelUrl))
			{
				showProgressDialog("Checking........");
				new ProductInfoLoader().execute(sku);
			}
			else
			{
				Settings settings = new Settings(this);
				String settingsUrl = settings.getUrl();

				showInvalidLabelDialog(getDomainNameFromUrl(settingsUrl), getDomainNameFromUrl(labelUrl));
			}
			
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
				labelUrl = contents;
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

		private SettingsSnapshot mSettingsSnapshot;
		private String sku;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mSettingsSnapshot = new SettingsSnapshot(ScanActivity.this);
		}
		
		@Override
		protected Boolean doInBackground(Object... args) {
			final String[] params = new String[2];
			params[0] = GET_PRODUCT_BY_SKU; // ZERO --> Use Product ID , ONE -->
											// Use Product SKU
			params[1] = sku;

			if (JobCacheManager.productDetailsExist(params[1], mSettingsSnapshot.getUrl())) {
				return Boolean.TRUE;
			} else {
				loadRequestID = resHelper.loadResource(ScanActivity.this, RES_PRODUCT_DETAILS, params, mSettingsSnapshot);
				return Boolean.FALSE;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result.booleanValue() == true) {
				if (isActivityAlive) {
					dismissProgressDialog();
					finish();
					launchProductDetails();
				}
			}
		}

	}

}
