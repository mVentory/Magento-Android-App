package com.mageventory.tasks;

import java.util.Map;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.mageventory.MageventoryConstants;
import com.mageventory.ProductCreateActivity;
import com.mageventory.ProductDetailsActivity;
import com.mageventory.R;
import com.mageventory.job.JobControlInterface;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.resprocessor.CreateCartOrderProcessor;
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
	String sku;

	private ProductCreateActivity mHostActivity;

	public CreateOrder(ProductCreateActivity hostActivity) {
		mHostActivity = hostActivity;
	}

	@Override
	protected String doInBackground(Integer... ints) {

		sku = mHostActivity.skuV.getText().toString();
		String soldPrice = mHostActivity.priceV.getText().toString();
		String qty = mHostActivity.quantityV.getText().toString();
		String name = mHostActivity.getProductName(mHostActivity,
				mHostActivity.nameV);

		// 2- Set Product Information
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
				
			Map<String, Object> productData = CreateCartOrderProcessor.extractProductDetails(bundle);
			
			if (TextUtils.isEmpty(sku)) {
				sku = CreateCartOrderProcessor.generateSku(productData, false);
				bundle.putString(MAGEKEY_PRODUCT_SKU, sku);
				
				mHostActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mHostActivity.skuV.setText(sku);
					}
				});
			}
			
			
			final String[] params = new String[2];
			params[0] = GET_PRODUCT_BY_SKU; // ZERO --> Use Product ID , ONE -->
										// 	Use Product SKU
			params[1] = sku;
		
			if (ResourceServiceHelper.getInstance().isResourceAvailable(
				mHostActivity, RES_PRODUCT_DETAILS, params))
			{
				product = ResourceServiceHelper.getInstance().restoreResource(
					mHostActivity, RES_PRODUCT_DETAILS, params);
			}
			else
			{
				mHostActivity.orderCreateId = ResourceServiceHelper
						.getInstance().loadResource(mHostActivity,
								RES_CART_ORDER_CREATE, null, bundle);
			}
		} catch (Exception e) {
		}
		
		return null;
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