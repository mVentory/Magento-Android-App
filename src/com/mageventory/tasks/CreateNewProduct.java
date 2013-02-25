package com.mageventory.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.QuickContact;
import android.text.TextUtils;
import android.widget.Toast;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.ProductCreateActivity;
import com.mageventory.activity.ProductDetailsActivity;
import com.mageventory.activity.base.BaseActivityCommon;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.jobprocessor.CreateProductProcessor;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.Product;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.Log;

public class CreateNewProduct extends AsyncTask<Void, Void, Integer> implements MageventoryConstants {

	private ProductCreateActivity mHostActivity;
	private JobControlInterface mJobControlInterface;

	private static int FAILURE = 0;
	private static int E_BAD_FIELDS = 1;
	private static int E_SKU_ALREADY_EXISTS = 3;
	private static int SUCCESS = 4;

	private String mNewSKU;
	private boolean mQuickSellMode;
	private SettingsSnapshot mSettingsSnapshot;

	public CreateNewProduct(ProductCreateActivity hostActivity, boolean quickSellMode) {
		mHostActivity = hostActivity;
		mJobControlInterface = new JobControlInterface(mHostActivity);
		mQuickSellMode = quickSellMode;
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
		final String[] stringKeys = { MAGEKEY_PRODUCT_NAME, MAGEKEY_PRODUCT_PRICE, MAGEKEY_PRODUCT_WEBSITE,
				MAGEKEY_PRODUCT_DESCRIPTION, MAGEKEY_PRODUCT_SHORT_DESCRIPTION, MAGEKEY_PRODUCT_STATUS,
				MAGEKEY_PRODUCT_WEIGHT, };
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

	private String extractString(final Bundle bundle, final String key, final boolean exceptionOnFail)
			throws IncompleteDataException {
		final String s = bundle.getString(key);
		if (s == null && exceptionOnFail) {
			throw new IncompleteDataException("bad data for key '" + key + "'");
		}
		return s == null ? "" : s;
	}

	private Map<String, Object> extractUpdate(Bundle bundle) throws IncompleteDataException {
		final String[] stringKeys = { MAGEKEY_PRODUCT_QUANTITY, MAGEKEY_PRODUCT_MANAGE_INVENTORY,
				MAGEKEY_PRODUCT_IS_IN_STOCK, MAGEKEY_PRODUCT_USE_CONFIG_MANAGE_STOCK,
				MAGEKEY_PRODUCT_IS_QTY_DECIMAL };
		// @formatter:on
		final Map<String, Object> productData = new HashMap<String, Object>();
		for (final String stringKey : stringKeys) {
			productData.put(stringKey, extractString(bundle, stringKey, true));
		}
		return productData;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		mSettingsSnapshot = new SettingsSnapshot(mHostActivity);
	}
	
	private String generateSku() {
		/* Since we can't get microsecond time in java we just use milliseconds time and add microsecond part from System.nanoTime() which
		 * doesn't return a normal timestamp but a number of nanoseconds from some arbitrary point in time which we don't know. This
		 * should be enough to make every SKU we'll ever generate different. */
		return "M" + System.currentTimeMillis() + (System.nanoTime()/1000)%1000;
	}
	
	@Override
	protected Integer doInBackground(Void... params) {
		if (mHostActivity == null || isCancelled()) {
			return FAILURE;
		}

		if (mHostActivity.verifyForm(mQuickSellMode) == false) {
			return E_BAD_FIELDS;
		}

		/*
		 * The request is missing a structure related to attributes when
		 * compared to the response. We're building this structure (list of
		 * maps) here to simulate the response.
		 */
		List<Map<String, Object>> selectedAttributesResponse = new ArrayList<Map<String, Object>>();

		final Bundle data = new Bundle();
		final Map<String, String> extracted = mHostActivity.extractCommonData();

		// user fields
		for (Map.Entry<String, String> e : extracted.entrySet()) {
			data.putString(e.getKey(), e.getValue());
		}

		if (mHostActivity.category != null && mHostActivity.category.getId() != INVALID_CATEGORY_ID) {
			data.putSerializable(MAGEKEY_PRODUCT_CATEGORIES,
					new Object[] { String.valueOf(mHostActivity.category.getId()) });
		}

		// default values
		data.putString(MAGEKEY_PRODUCT_WEBSITE, TODO_HARDCODED_PRODUCT_WEBSITE);

		data.putString(MAGEKEY_PRODUCT_SKU, mHostActivity.skuV.getText().toString());

		// generated
		String quantity = mHostActivity.quantityV.getText().toString();
		
		/* 1 - status enabled, 2 - status disabled */
		int status = mHostActivity.statusV.isChecked() ? 1 : 2;
		String inventoryControl;
		String isQtyDecimal;
		String isInStock;

		if (!TextUtils.isEmpty(quantity) && Double.parseDouble(quantity) >= 0)
		{
			inventoryControl = "1";
			
			/* See https://code.google.com/p/mageventory/issues/detail?id=148 */
			if (quantity.contains("."))
			{
				isQtyDecimal = "1";
			}
			else
			{
				isQtyDecimal = "0";
			}
			
			if (Double.parseDouble(quantity) > 0)
			{
				isInStock = "1";
			}
			else
			{
				isInStock = "0";
			}
		}
		else
		{
			isQtyDecimal = "0";
			inventoryControl = "0";
			quantity = "0";
			isInStock = "0";
		}
		
		data.putString(MAGEKEY_PRODUCT_QUANTITY, quantity);
		data.putString(MAGEKEY_PRODUCT_STATUS, "" + status);
		data.putString(MAGEKEY_PRODUCT_MANAGE_INVENTORY, inventoryControl);
		data.putString(MAGEKEY_PRODUCT_IS_IN_STOCK, isInStock);
		data.putString(MAGEKEY_PRODUCT_USE_CONFIG_MANAGE_STOCK, "0");
		data.putString(MAGEKEY_PRODUCT_IS_QTY_DECIMAL, isQtyDecimal);

		// attributes
		// bundle attributes
		final HashMap<String, Object> atrs = new HashMap<String, Object>();

		if (mHostActivity.customAttributesList.getList() != null) {
			for (CustomAttribute elem : mHostActivity.customAttributesList.getList()) {
				atrs.put(elem.getCode(), elem.getSelectedValue());

				Map<String, Object> selectedAttributesResponseMap = new HashMap<String, Object>();
				selectedAttributesResponseMap.put(MAGEKEY_ATTRIBUTE_CODE_PRODUCT_DETAILS_REQ, elem.getCode());

				Map<String, Object> frontEndLabel = new HashMap<String, Object>();
				frontEndLabel.put("label", elem.getMainLabel());
				frontEndLabel.put("store_id", "0");

				selectedAttributesResponseMap.put("frontend_label", new Object[] { frontEndLabel });
				selectedAttributesResponseMap.put("frontend_input", elem.getType());
				selectedAttributesResponseMap.put(MAGEKEY_ATTRIBUTE_OPTIONS, elem.getOptionsAsArrayOfMaps());

				selectedAttributesResponse.add(selectedAttributesResponseMap);
			}
		}

		atrs.put("product_barcode_", mHostActivity.barcodeInput.getText().toString());

		/* Response simulation related code */
		Map<String, Object> selectedAttributesResponseMap = new HashMap<String, Object>();
		selectedAttributesResponseMap.put(MAGEKEY_ATTRIBUTE_CODE_PRODUCT_DETAILS_REQ, "product_barcode_");

		Map<String, Object> frontEndLabel = new HashMap<String, Object>();
		frontEndLabel.put("label", "Barcode");
		frontEndLabel.put("store_id", "0");

		selectedAttributesResponseMap.put("frontend_label", new Object[] { frontEndLabel });
		selectedAttributesResponseMap.put("frontend_input", "");
		selectedAttributesResponseMap.put(MAGEKEY_ATTRIBUTE_OPTIONS, new Object[0]);

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

		productRequestData.putAll(extractUpdate(data));

		mNewSKU = data.getString(MAGEKEY_PRODUCT_SKU);

		if (TextUtils.isEmpty(mNewSKU)) {
			// Empty Generate SKU
			mNewSKU = generateSku();
		}

		productRequestData.put(MAGEKEY_PRODUCT_SKU, mNewSKU);
		productRequestData.put(EKEY_PRODUCT_ATTRIBUTE_SET_ID, new Integer(attrSet));

		/* Simulate a response from the server so that we can store it in cache. */
		Map<String, Object> productResponseData = new HashMap<String, Object>(productRequestData);

		/*
		 * Filling the things that were missing in the request to simulate a
		 * response.
		 */

		if (mHostActivity.category != null && mHostActivity.category.getId() != INVALID_CATEGORY_ID) {
			productResponseData.put(MAGEKEY_PRODUCT_CATEGORY_IDS,
					new Object[] { String.valueOf(mHostActivity.category.getId()) });
		}

		productResponseData.put(MAGEKEY_PRODUCT_ATTRIBUTE_SET_ID, new Integer(attrSet));
		productResponseData.put(MAGEKEY_PRODUCT_IMAGES, new Object[0]);
		productResponseData.put(MAGEKEY_PRODUCT_ID, INVALID_PRODUCT_ID);
		productResponseData.put("set_attributes", selectedAttributesResponse.toArray());

		Product p = new Product(productResponseData);

		if (JobCacheManager.productDetailsExist(p.getSku(), mSettingsSnapshot.getUrl())) {
			
			/* It doesn't matter if the product already exists if we are in quicksell mode. Quick selling is
			 * still a valid thing to do in that case. */
			if (mQuickSellMode)
			{
				JobCacheManager.removeProductDetails(p.getSku(), mSettingsSnapshot.getUrl());
			}
			else
			{
				return E_SKU_ALREADY_EXISTS;
			}
		}

		JobID jobID = new JobID(INVALID_PRODUCT_ID, RES_CATALOG_PRODUCT_CREATE, mNewSKU, null);
		Job job = new Job(jobID, mSettingsSnapshot);
		job.setExtras(productRequestData);
		
		/* Inform lower layer about which product creation mode was selected by the user
		 * (quick sell mode or normal mode)*/
		job.putExtraInfo(EKEY_QUICKSELLMODE, new Boolean(mQuickSellMode));

		mJobControlInterface.addJob(job);

		JobCacheManager.storeProductDetailsWithMerge(p, mSettingsSnapshot.getUrl());

		/* Store additional values in the input cache. */
		mHostActivity.updateInputCacheWithCurrentValues();
		
		/* Save the state of product create activity in permanent storage for the
		 * user to be able to restore it next time when creating a product. */
		SharedPreferences preferences;
		preferences = mHostActivity.getSharedPreferences(ProductCreateActivity.PRODUCT_CREATE_SHARED_PREFERENCES, Context.MODE_PRIVATE);
		
		SharedPreferences.Editor editor = preferences.edit();

		editor.putString(ProductCreateActivity.PRODUCT_CREATE_DESCRIPTION, mHostActivity.descriptionV.getText().toString());
		editor.putString(ProductCreateActivity.PRODUCT_CREATE_WEIGHT, mHostActivity.weightV.getText().toString());
		editor.putInt(ProductCreateActivity.PRODUCT_CREATE_ATTRIBUTE_SET, mHostActivity.atrSetId);

		if (mHostActivity.category != null) {
			editor.putInt(ProductCreateActivity.PRODUCT_CREATE_CATEGORY, mHostActivity.category.getId());
		} else {
			editor.putInt(ProductCreateActivity.PRODUCT_CREATE_CATEGORY, INVALID_CATEGORY_ID);
		}

		editor.commit();

		if (mHostActivity.customAttributesList != null)
			mHostActivity.customAttributesList.saveInCache();
		
		return SUCCESS;
	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);

		if (mHostActivity == null) {
			return;
		}
		if (result == SUCCESS) {
			
			/* NewNewReloadCycle starts here. */
			BaseActivityCommon.mNewNewReloadCycle = true;
			
			// successful creation, launch product details activity
			final String ekeyProductSKU = mHostActivity.getString(R.string.ekey_product_sku);
			final String ekeyNewProduct = mHostActivity.getString(R.string.ekey_new_product);
			final Intent intent = new Intent(mHostActivity, ProductDetailsActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra(ekeyProductSKU, mNewSKU);
			intent.putExtra(ekeyNewProduct, true);
			
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
