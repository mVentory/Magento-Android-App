package com.mageventory.tasks;

import java.util.HashMap;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.mageventory.MageventoryConstants;
import com.mageventory.ProductDetailsActivity;
import com.mageventory.ProductEditActivity;
import com.mageventory.R;
import com.mageventory.model.CustomAttribute;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;

public class UpdateProduct extends BaseTask<ProductEditActivity, Object> implements MageventoryConstants,
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

			bundle.putString(MAGEKEY_PRODUCT_NAME, host.getProductName(host, host.nameV));

			if (TextUtils.isEmpty(host.priceV.getText().toString())) {
				bundle.putString(MAGEKEY_PRODUCT_PRICE, "0");
			} else {
				bundle.putString(MAGEKEY_PRODUCT_PRICE, host.priceV.getText().toString());
			}

			bundle.putString(MAGEKEY_PRODUCT_WEBSITE, TODO_HARDCODED_PRODUCT_WEBSITE); // y
																						// TODO:
			// hard-coded
			// website...

			if (TextUtils.isEmpty(host.descriptionV.getText().toString())) {
				bundle.putString(MAGEKEY_PRODUCT_DESCRIPTION, "n/a");
				bundle.putString(MAGEKEY_PRODUCT_SHORT_DESCRIPTION, "n/a");
			} else {
				bundle.putString(MAGEKEY_PRODUCT_DESCRIPTION, host.descriptionV.getText().toString());
				bundle.putString(MAGEKEY_PRODUCT_SHORT_DESCRIPTION, host.descriptionV.getText().toString());
			}

			bundle.putString(MAGEKEY_PRODUCT_STATUS, host.statusV.isChecked() ? "1" : "0");

			if (TextUtils.isEmpty(host.weightV.getText().toString())) {
				bundle.putString(MAGEKEY_PRODUCT_WEIGHT, "0");
			} else {
				bundle.putString(MAGEKEY_PRODUCT_WEIGHT, host.weightV.getText().toString());
			}

			bundle.putString(MAGEKEY_PRODUCT_SKU, host.skuV.getText().toString());

			if (host.category != null && host.category.getId() != INVALID_CATEGORY_ID) {
				bundle.putSerializable(MAGEKEY_PRODUCT_CATEGORIES,
						new Object[] { String.valueOf(host.category.getId()) });
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

			if (getHost().customAttributesList.getList() != null) {
				for (CustomAttribute elem : getHost().customAttributesList.getList()) {
					atrs.put(elem.getCode(), elem.getSelectedValue());
				}
			}

			atrs.put("product_barcode_", getHost().barcodeInput.getText().toString());

			// bundle.putInt(EKEY_PRODUCT_ATTRIBUTE_SET_ID, host.atrSetId);
			bundle.putSerializable(EKEY_PRODUCT_ATTRIBUTE_VALUES, atrs);

			ResourceServiceHelper.getInstance().registerLoadOperationObserver(this);

			updateProductRequestId = ResourceServiceHelper.getInstance().loadResource(host, RES_CATALOG_PRODUCT_UPDATE,
					new String[] { String.valueOf(host.productId) }, bundle);
			return 1;
		} catch (Exception ex) {
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
				host.updateInputCacheWithCurrentValues();
				Toast.makeText(host, "Product updated", Toast.LENGTH_LONG).show();
				host.setResult(RESULT_CHANGE);
			} else {
				Toast.makeText(host, "Error occurred while uploading: " + op.getException(), Toast.LENGTH_LONG).show();
			}

			ResourceServiceHelper.getInstance().unregisterLoadOperationObserver(this);

			// Load Product Details Screen
			Intent newIntent = new Intent(host.getApplicationContext(), ProductDetailsActivity.class);
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