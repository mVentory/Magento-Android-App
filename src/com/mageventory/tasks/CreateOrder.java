package com.mageventory.tasks;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.ProductCreateActivity;
import com.mageventory.ProductDetailsActivity;
import com.mageventory.R;
import com.mageventory.job.JobControlInterface;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.util.Log;

/**
 * Create Order Invoice
 * 
 * @author hussein
 * 
 */
public class CreateOrder extends AsyncTask<Integer, Integer, String> implements
		MageventoryConstants {

	Product product;

	private ProductCreateActivity mHostActivity;

	public CreateOrder(ProductCreateActivity hostActivity) {
		mHostActivity = hostActivity;
	}

	@Override
	protected String doInBackground(Integer... ints) {

		// 2- Set Product Information
		final String sku = mHostActivity.skuV.getText().toString();
		String soldPrice = mHostActivity.priceV.getText().toString();
		final String qty = mHostActivity.quantityV.getText().toString();
		String name = mHostActivity.getProductName(mHostActivity,
				mHostActivity.nameV);

		if (TextUtils.isEmpty(soldPrice)) {
			soldPrice = "0";
		}

		try {
			final Bundle bundle = new Bundle();
			/* PRODUCT INFORMAITON */
			bundle.putString(MAGEKEY_PRODUCT_SKU, sku);
			bundle.putString(MAGEKEY_PRODUCT_QUANTITY, qty);
			bundle.putString(MAGEKEY_PRODUCT_PRICE, soldPrice);
			bundle.putString(MAGEKEY_PRODUCT_NAME, name);

			if (ResourceServiceHelper.getInstance().isResourceAvailable(
					mHostActivity, RES_CART_ORDER_CREATE, null))
				product = ResourceServiceHelper.getInstance().restoreResource(
						mHostActivity, RES_CART_ORDER_CREATE, null);
			else
				mHostActivity.orderCreateId = ResourceServiceHelper
						.getInstance().loadResource(mHostActivity,
								RES_CART_ORDER_CREATE, null, bundle);

			return null;
		} catch (Exception e) {
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
			mHostActivity.dismissProgressDialog();
			// set as old
			ResourceServiceHelper.getInstance().markResourceAsOld(
					mHostActivity, RES_CART_ORDER_CREATE, null);

			// Product Exists --> Show Product Details
			final String ekeyProductSKU = mHostActivity
					.getString(R.string.ekey_product_sku);
			final String productSKU = product.getSku();
			final Intent intent = new Intent(mHostActivity,
					ProductDetailsActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra(ekeyProductSKU, productSKU);
			mHostActivity.startActivity(intent);
		}
	}
}