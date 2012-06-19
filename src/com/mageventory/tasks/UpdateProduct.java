package com.mageventory.tasks;

import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.mageventory.MageventoryConstants;
import com.mageventory.ProductCreateActivity;
import com.mageventory.ProductDetailsActivity;
import com.mageventory.ProductEditActivity;
import com.mageventory.R;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;

public class UpdateProduct extends AsyncTask<Void, Void, Integer> implements MageventoryConstants {
	
	private ProductEditActivity mHostActivity;
	private JobControlInterface mJobControlInterface;
	
	private static int FAILURE = 0;
	private static int SUCCESS = 1;

	public UpdateProduct(ProductEditActivity hostActivity) {
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
				MAGEKEY_PRODUCT_IS_IN_STOCK };
		// @formatter:on
		final Map<String, Object> productData = new HashMap<String, Object>();
		for (final String stringKey : stringKeys) {
			productData.put(stringKey, extractString(bundle, stringKey, true));
		}
		return productData;
	}
	
	@Override
	protected Integer doInBackground(Void... arg0) {

		if (mHostActivity == null || isCancelled()) {
			return FAILURE;
		}
		
			final Bundle bundle = new Bundle();

			bundle.putString(MAGEKEY_PRODUCT_NAME, mHostActivity.getProductName(mHostActivity, mHostActivity.nameV));

			if (TextUtils.isEmpty(mHostActivity.priceV.getText().toString())) {
				bundle.putString(MAGEKEY_PRODUCT_PRICE, "0");
			} else {
				bundle.putString(MAGEKEY_PRODUCT_PRICE, mHostActivity.priceV.getText().toString());
			}

			bundle.putString(MAGEKEY_PRODUCT_WEBSITE, TODO_HARDCODED_PRODUCT_WEBSITE); // y
																						// TODO:
			// hard-coded
			// website...

			if (TextUtils.isEmpty(mHostActivity.descriptionV.getText().toString())) {
				bundle.putString(MAGEKEY_PRODUCT_DESCRIPTION, "n/a");
				bundle.putString(MAGEKEY_PRODUCT_SHORT_DESCRIPTION, "n/a");
			} else {
				bundle.putString(MAGEKEY_PRODUCT_DESCRIPTION, mHostActivity.descriptionV.getText().toString());
				bundle.putString(MAGEKEY_PRODUCT_SHORT_DESCRIPTION, mHostActivity.descriptionV.getText().toString());
			}

			bundle.putString(MAGEKEY_PRODUCT_STATUS, mHostActivity.statusV.isChecked() ? "1" : "0");

			if (TextUtils.isEmpty(mHostActivity.weightV.getText().toString())) {
				bundle.putString(MAGEKEY_PRODUCT_WEIGHT, "0");
			} else {
				bundle.putString(MAGEKEY_PRODUCT_WEIGHT, mHostActivity.weightV.getText().toString());
			}

			bundle.putString(MAGEKEY_PRODUCT_SKU, mHostActivity.skuV.getText().toString());

			if (mHostActivity.category != null && mHostActivity.category.getId() != INVALID_CATEGORY_ID) {
				bundle.putSerializable(MAGEKEY_PRODUCT_CATEGORIES,
						new Object[] { String.valueOf(mHostActivity.category.getId()) });
			}

			bundle.putString(MAGEKEY_PRODUCT_QUANTITY, mHostActivity.quantityV.getText().toString());

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

			if (mHostActivity.customAttributesList.getList() != null) {
				for (CustomAttribute elem : mHostActivity.customAttributesList.getList()) {
					atrs.put(elem.getCode(), elem.getSelectedValue());
				}
			}

			atrs.put("product_barcode_", mHostActivity.barcodeInput.getText().toString());

			// bundle.putInt(EKEY_PRODUCT_ATTRIBUTE_SET_ID, host.atrSetId);
			bundle.putSerializable(EKEY_PRODUCT_ATTRIBUTE_VALUES, atrs);

			final Map<String, Object> productRequestData = extractData(bundle, false);
			productRequestData.put("tax_class_id", 0);
			
			productRequestData.putAll(extractUpdate(bundle));
			
			final HashMap<String, Object> atrs2 = (HashMap<String, Object>) bundle.get(EKEY_PRODUCT_ATTRIBUTE_VALUES);
			if (atrs2 != null && atrs2.isEmpty() == false) {
				productRequestData.putAll(atrs2);
			}
			
			JobID jobID = new JobID(INVALID_PRODUCT_ID, RES_CATALOG_PRODUCT_UPDATE, mHostActivity.productSKU);
			Job job = new Job(jobID);
			job.setExtras(productRequestData);
			
			mJobControlInterface.addJob(job);

			/* Store additional values in the input cache. */
			mHostActivity.updateInputCacheWithCurrentValues();
			
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
			intent.putExtra(ekeyProductSKU, mHostActivity.productSKU);
			mHostActivity.startActivity(intent);

		} else if (result == FAILURE) {
			Toast.makeText(mHostActivity, "Update failed...", Toast.LENGTH_LONG).show();
		}
		
		mHostActivity.dismissProgressDialog();
		mHostActivity.finish();
	}
}