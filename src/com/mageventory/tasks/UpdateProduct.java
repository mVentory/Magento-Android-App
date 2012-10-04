package com.mageventory.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.ProductCreateActivity;
import com.mageventory.activity.ProductDetailsActivity;
import com.mageventory.activity.ProductEditActivity;
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
import com.mageventory.settings.SettingsSnapshot;

public class UpdateProduct extends AsyncTask<Void, Void, Integer> implements MageventoryConstants {

	private ProductEditActivity mHostActivity;
	private JobControlInterface mJobControlInterface;

	private static int FAILURE = 0;
	private static int UPDATE_PENDING = 1;
	private static int SUCCESS = 2;
	private SettingsSnapshot mSettingsSnapshot;

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

	/*
	 * Check if one list of categories contains the second list of categories.
	 * You can't pass nulls to that function.
	 */
	private boolean categoryListsContain(Object[] superset, Object[] subset) {
		for (int i = 0; i < subset.length; i++) {
			String subsetCat = (String) subset[i];

			boolean match = false;
			for (int j = 0; j < superset.length; j++) {
				String supersetCat = (String) superset[j];

				if (supersetCat.equals(subsetCat)) {
					match = true;
					break;
				}
			}

			if (match == false) {
				return false;
			}
		}

		return true;
	}

	private boolean categoryListsEqual(Object[] originalCategories, Object[] updatedCategories) {
		if ((originalCategories == null || originalCategories.length==0) && (updatedCategories == null || updatedCategories.length==0))
			return true;

		if ((originalCategories == null && updatedCategories != null)
				|| (updatedCategories == null && originalCategories != null)) {
			return false;
		}

		if (categoryListsContain(originalCategories, updatedCategories)
				&& categoryListsContain(updatedCategories, originalCategories)) {
			return true;
		}

		return false;
	}

	/* Checks what changes were made by the user and returns a list of those changed attribute keys.
	 * The first argument is response from the server representing product details that user is modifying
	 * in product edit activity. The second argument is the request to the server to update a product.*/
	private List<String> createListOfUpdatedAttributes(Map<String, Object> originalProduct,
			Map<String, Object> updatedProduct) {
		List<String> out = new ArrayList<String>();

		final String[] stringKeysNotSetAssociated = { MAGEKEY_PRODUCT_NAME, MAGEKEY_PRODUCT_PRICE, MAGEKEY_PRODUCT_SKU,
				MAGEKEY_PRODUCT_QUANTITY, MAGEKEY_PRODUCT_DESCRIPTION, MAGEKEY_PRODUCT_SHORT_DESCRIPTION,
				MAGEKEY_PRODUCT_STATUS, MAGEKEY_PRODUCT_WEIGHT, MAGEKEY_PRODUCT_MANAGE_INVENTORY };

		/* Check everything except custom attributes and categories. */
		for (String attribute : stringKeysNotSetAssociated) {
			String originalAttribValue = "";
			String updatedAttribValue = (String) updatedProduct.get(attribute);

			/* In case of "manage inventory" flag the server can return an integer or a string and we don't know which one.
			 * We need to make sure the app will work in both cases. */
			if (attribute == MAGEKEY_PRODUCT_MANAGE_INVENTORY)
			{
				Object manageInventory = originalProduct.get(attribute);
				
				if (manageInventory instanceof String)
				{
					originalAttribValue = (String) manageInventory;
				}
				else
				if (manageInventory instanceof Integer)
				{
					originalAttribValue = ((Integer) manageInventory).toString();
				}
			}
			else
			{
				originalAttribValue = (String) originalProduct.get(attribute);
			}
			
			/*
			 * In case of numerical attributes the server is formatting them and
			 * sends them back in a slightly different format. This is taken
			 * care of here (we're formatting them the same way before
			 * comparison).
			 */
			if (attribute.equals(MAGEKEY_PRODUCT_PRICE) || attribute.equals(MAGEKEY_PRODUCT_QUANTITY)
					|| attribute.equals(MAGEKEY_PRODUCT_WEIGHT)) {
				originalAttribValue = "" + Double.parseDouble(originalAttribValue);
				updatedAttribValue = "" + Double.parseDouble(updatedAttribValue);
			}

			if (!TextUtils.equals(originalAttribValue, updatedAttribValue)) {
				out.add(attribute);
			}
		}

		/* Check categories. We send a different key to the server than the one we get from the server. */
		if (!categoryListsEqual((Object[]) originalProduct.get(MAGEKEY_PRODUCT_CATEGORY_IDS),
				(Object[]) updatedProduct.get(MAGEKEY_PRODUCT_CATEGORIES))) {
			out.add(MAGEKEY_PRODUCT_CATEGORIES);
		}

		/* Check custom attributes */
		for (String key : updatedProduct.keySet()) {
			if (key.endsWith("_")) {
				String originalAttribValue = (String) originalProduct.get(key);
				String updatedAttribValue = (String) updatedProduct.get(key);

				/*
				 * If we send empty custom attribute to the server sometimes it is not
				 * sending it back which is what we are taking care of here.
				 */
				if (TextUtils.equals(originalAttribValue, "")) {
					originalAttribValue = null;
				}
				
				if (TextUtils.equals(updatedAttribValue, "")) {
					updatedAttribValue = null;
				}

				if (!TextUtils.equals(originalAttribValue, updatedAttribValue)) {
					out.add(key);
				}
			}
		}

		return out;
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
				MAGEKEY_PRODUCT_IS_IN_STOCK, MAGEKEY_PRODUCT_USE_CONFIG_MANAGE_STOCK };
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
	
	@Override
	protected Integer doInBackground(Void... arg0) {

		if (mHostActivity == null || isCancelled()) {
			return FAILURE;
		}

		if (mHostActivity.getProduct() == null) {
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

		/* 1 - status enabled, 2 - status disabled */
		bundle.putString(MAGEKEY_PRODUCT_STATUS, mHostActivity.statusV.isChecked() ? "1" : "2");

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

		// generated
		String quantity = mHostActivity.quantityV.getText().toString();
		String inventoryControl;
		String isInStock = "1"; // Any Product is Always in Stock
		
		if (!TextUtils.isEmpty(quantity) && TextUtils.isDigitsOnly(quantity) && Integer.parseInt(quantity) >= 0)
		{
			inventoryControl = "1";
		}
		else
		{
			inventoryControl = "0";
			quantity = "0";
		}

		bundle.putString(MAGEKEY_PRODUCT_QUANTITY, quantity);
		bundle.putString(MAGEKEY_PRODUCT_MANAGE_INVENTORY, inventoryControl);
		bundle.putString(MAGEKEY_PRODUCT_IS_IN_STOCK, isInStock);
		bundle.putString(MAGEKEY_PRODUCT_USE_CONFIG_MANAGE_STOCK, "0");

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
		
		productRequestData.put(MAGEKEY_PRODUCT_SKU, bundle.getString(MAGEKEY_PRODUCT_SKU));
		
		productRequestData.put("tax_class_id", 0);

		productRequestData.putAll(extractUpdate(bundle));

		final HashMap<String, Object> atrs2 = (HashMap<String, Object>) bundle.get(EKEY_PRODUCT_ATTRIBUTE_VALUES);
		if (atrs2 != null && atrs2.isEmpty() == false) {
			productRequestData.putAll(atrs2);
		}
		
		/* Product details file in the cache can be merged with product edit job in which case it will have original copy attached.
		 * We want to compare with that original copy in that case. */
		Product productToCompareAgainst;
		
		if (mHostActivity.getProduct().getUnmergedProduct() != null)
		{
			productToCompareAgainst = mHostActivity.getProduct().getUnmergedProduct();
		}
		else
		{
			productToCompareAgainst = mHostActivity.getProduct();
		}

		List<String> updatedAttributesList = createListOfUpdatedAttributes(productToCompareAgainst.getData(),
				productRequestData);
		
		/* Save updated attributes list in the job product request data (it will be removed before sending this to
		 * the server. We just put it here so that it is saved in the cache. We need this in the cache to do
		 * two-way merge of the product edit job file with the product details file)*/
		productRequestData.put(EKEY_UPDATED_KEYS_LIST, updatedAttributesList);

		JobID jobID = new JobID(INVALID_PRODUCT_ID, RES_CATALOG_PRODUCT_UPDATE, mHostActivity.productSKU, null);
		Job job = new Job(jobID, mSettingsSnapshot);
		job.setExtras(productRequestData);

		boolean res = mJobControlInterface.addEditJob(job);
		
		if (res == true)
		{
			/* Store additional values in the input cache. */
			mHostActivity.updateInputCacheWithCurrentValues();
		}
		else
		{
			return UPDATE_PENDING;
		}

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

			mHostActivity.dismissProgressDialog();
			mHostActivity.finish();
		} 
		else if (result == UPDATE_PENDING) {
			Toast.makeText(mHostActivity, "An update is being processed at the moment. Please wait a couple of seconds and try again...", Toast.LENGTH_LONG).show();
			mHostActivity.dismissProgressDialog();

		}
		else if (result == FAILURE) {
			Toast.makeText(mHostActivity, "Update failed...", Toast.LENGTH_LONG).show();
			
			mHostActivity.dismissProgressDialog();
			mHostActivity.finish();
		}

		
	}
}