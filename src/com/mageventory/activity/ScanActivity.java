package com.mageventory.activity;

import java.util.HashSet;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.activity.base.BaseActivityCommon;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.resprocessor.ProductDetailsProcessor.ProductDetailsLoadException;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.SingleFrequencySoundGenerator;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

public class ScanActivity extends BaseActivity implements MageventoryConstants, OperationObserver {
	
	ProgressDialog progressDialog;
	private int loadRequestID;
	private boolean barcodeScanned;
	private String sku;
	private String labelUrl;
	private boolean skuFound;
	private boolean scanDone;
	private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
	private boolean isActivityAlive;
	private SingleFrequencySoundGenerator mDetailsLoadFailureSound = new SingleFrequencySoundGenerator(700, 1000);
	private Settings mSettings;
	
	public static class DomainNamePair	
	{
		private String mDomain1;
		private String mDomain2;
		
		public DomainNamePair(String domain1, String domain2)
		{
			mDomain1 = domain1;
			mDomain2 = domain2;
		}
		
		@Override
		public int hashCode() {
			return mDomain1.hashCode() * mDomain2.hashCode() + mDomain1.hashCode() + mDomain2.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			
			if ( TextUtils.equals( ((DomainNamePair)o).mDomain1, mDomain1 ) &&
				 TextUtils.equals( ((DomainNamePair)o).mDomain2, mDomain2 ) )
			{
				return true;
			}
			
			return false;
		}
	}
	
	/* In case user scans a label which contains domain name which doesn't match the domain name from the current profile we are
	 * showing a dialog with a warning and two buttons: "OK" and "Cancel". In case user presses "OK" for any pair of such domain names we
	 * don't want the warning dialog to be displayed anymore for this particular pair during the lifetime of the application's process.
	 * This is why we need to store those pairs in this hashset. */
	private static HashSet<DomainNamePair> sDomainNamePairsRemembered = new HashSet<DomainNamePair>();
	
	public static boolean domainPairRemembered(String settingsDomain, String labelDomain)
	{
		synchronized(sDomainNamePairsRemembered)
		{
			if (sDomainNamePairsRemembered.contains(new DomainNamePair(settingsDomain, labelDomain)))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	}
	
	public static void rememberDomainNamePair(String settingsDomain, String labelDomain)
	{
		synchronized(sDomainNamePairsRemembered)
		{
			DomainNamePair newDomainNamePair = new DomainNamePair(settingsDomain, labelDomain);
			
			if (!sDomainNamePairsRemembered.contains(newDomainNamePair))
			{
				sDomainNamePairsRemembered.add(newDomainNamePair);
			}
		}
	}
	
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
	
	/* Is the label in the following format: http://..../sku/[sku] */
	public static boolean isLabelInTheRightFormat(String label)
	{
		/* Does the label start with "http://" ? */
		if (!label.startsWith("http://"))
		{
			/* No, bad label. */
			return false;
		}
		
		/* Get rid of the "http://" from the label */
		label = label.substring("http://".length());
		
		int lastSlashIndex = label.lastIndexOf("/");
		
		/* Does the label still contain a slash? */
		if (lastSlashIndex == -1)
		{
			/* No, bad label. */
			return false;
		}
		
		label = label.substring(0, lastSlashIndex);
		
		/* Does the label end with "/sku" ? */
		if (!label.endsWith("/sku"))
		{
			/* No, bad label. */
			return false;
		}
		
		return true;
	}
	
	/* Check if the SKU is in the form of "P" + 16 digits or "M" + 16 digits. */
	public static boolean isSKUInTheRightFormat(String sku)
	{
		if (!(sku.length()==17))
			return false;
		
		if (!sku.startsWith("M") && !sku.startsWith("P"))
			return false;
		
		long timestamp;
		
		try
		{
			timestamp = Long.parseLong(sku.substring(1));
		}
		catch(NumberFormatException nfe)
		{
			return false;
		}
		
		if (timestamp < 0)
		{
			return false;
		}
		
		return true;
	}

	
	/* Validate the label against the current url in the settings. If they don't match return false. */
	public static boolean isLabelValid(Context c, String label)
	{
		/* Treat the label as valid if it's not in the right format. */
		if (!isLabelInTheRightFormat(label))
		{
			return true;
		}
		
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
		super.onCreate(savedInstanceState);

		mSettings = new Settings(this);
		
		setContentView(R.layout.scan_activity);
		// Start QR Code Scanner

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			sku = extras.getString(getString(R.string.ekey_product_sku));
		}
		
		if (sku != null)
		{
			scanDone = true;
			skuFound = true;
			labelUrl = mSettings.getUrl();
		}
		else
		{
			if (savedInstanceState == null) {
				scanDone = false;
				Intent scanInt = new Intent("com.google.zxing.client.android.SCAN");
				startActivityForResult(scanInt, SCAN_QR_CODE);
			} else
				scanDone = true;	
		}

		isActivityAlive = true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		isActivityAlive = false;
	}
	
	private void launchProductDetails(String prodSKU)
	{
		/* Launching product details from scan activity breaks NewNewReloadCycle */
		BaseActivityCommon.mNewNewReloadCycle = false;	
		
		final String ekeyProductSKU = getString(R.string.ekey_product_sku);
		final Intent intent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		intent.putExtra(getString(R.string.ekey_prod_det_launched_from_menu_scan), true);
		intent.putExtra(ekeyProductSKU, prodSKU);

		startActivity(intent);
	}
	
	private void launchProductCreate(boolean skuExistsOnServerUncertainty)
	{
		final String ekeyProductSKU = getString(R.string.ekey_product_sku);
		final String ekeySkuExistsOnServerUncertainty = getString(R.string.ekey_sku_exists_on_server_uncertainty);
		final String brScanned = getString(R.string.ekey_barcode_scanned);
		
		final Intent intent = new Intent(getApplicationContext(), ProductCreateActivity.class);
		
		intent.putExtra(ekeyProductSKU, sku);
		intent.putExtra(ekeySkuExistsOnServerUncertainty, skuExistsOnServerUncertainty);
		intent.putExtra(brScanned, barcodeScanned);
		
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
							
							Settings settings = new Settings(ScanActivity.this);
							
							if (settings.getSoundCheckBox() == true)
							{
								mDetailsLoadFailureSound.playSound();
							}
							
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
							launchProductDetails(op.getExtras().getString(MAGEKEY_PRODUCT_SKU));
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

	public void showInvalidLabelDialog(final String settingsDomainName, final String skuDomainName) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Warning");
		alert.setMessage("Wrong label. Expected domain name: '" + settingsDomainName + "' found: '" + skuDomainName +"'" );

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				rememberDomainNamePair(settingsDomainName, skuDomainName);
				showProgressDialog("Checking........");
				new ProductInfoLoader().execute(sku);
		}});
		
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ScanActivity.this.finish();
		}});

		AlertDialog srDialog = alert.create();
		srDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				ScanActivity.this.finish();
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

				if (!domainPairRemembered(getDomainNameFromUrl(settingsUrl), getDomainNameFromUrl(labelUrl)))
				{
					showInvalidLabelDialog(getDomainNameFromUrl(settingsUrl), getDomainNameFromUrl(labelUrl));	
				}
				else
				{
					showProgressDialog("Checking........");
					new ProductInfoLoader().execute(sku);	
				}
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
					
					if (ScanActivity.isLabelInTheRightFormat(contents))
					{
						sku = urlData[urlData.length - 1];
					}
					else
					{
						sku = contents;
						
						if (!ScanActivity.isSKUInTheRightFormat(sku))
							barcodeScanned = true;
					}
					
					if (JobCacheManager.saveRangeStart(sku, mSettings.getProfileID()) == false)
					{
						ProductDetailsActivity.showTimestampRecordingError(this);
					}				
					
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
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mSettingsSnapshot = new SettingsSnapshot(ScanActivity.this);
		}
		
		@Override
		protected Boolean doInBackground(Object... args) {
			final String[] params = new String[2];
			params[0] = GET_PRODUCT_BY_SKU_OR_BARCODE;
			params[1] = sku;

			if (JobCacheManager.productDetailsExist(params[1], mSettingsSnapshot.getUrl())) {
				return Boolean.TRUE;
			} else {
				
				Bundle b = new Bundle();
				b.putBoolean(EKEY_DONT_REPORT_PRODUCT_NOT_EXIST_EXCEPTION, true);
				
				loadRequestID = resHelper.loadResource(ScanActivity.this, RES_PRODUCT_DETAILS, params, b, mSettingsSnapshot);
				return Boolean.FALSE;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result.booleanValue() == true) {
				if (isActivityAlive) {
					dismissProgressDialog();
					finish();
					launchProductDetails(sku);
				}
			}
		}

	}

}
