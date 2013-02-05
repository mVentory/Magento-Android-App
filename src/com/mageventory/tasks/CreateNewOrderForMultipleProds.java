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
import com.mageventory.activity.OrderDetailsActivity;
import com.mageventory.activity.OrderListActivity;
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
import com.mageventory.resprocessor.OrdersListByStatusProcessor;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.Log;

public class CreateNewOrderForMultipleProds extends AsyncTask<Void, Void, Integer> implements MageventoryConstants {

	public static final String MULTIPLE_PRODUCTS_ORDER_QUEUED_STATE = "Queued";
	
	private OrderListActivity mHostActivity;
	private JobControlInterface mJobControlInterface;

	private SettingsSnapshot mSettingsSnapshot;
	private Object[] mProductsToSellJobExtras;
	private Object[] mProductsToSellAllData;
	private String mSKU;
	private int mProductID;
	private String mOrderIncrementID;

	public CreateNewOrderForMultipleProds(OrderListActivity hostActivity, Object[] productsToSellJobExtras, Object[] productsToSellAllData, String sku, int productID) {
		mHostActivity = hostActivity;
		mJobControlInterface = new JobControlInterface(mHostActivity);
		mProductsToSellJobExtras = productsToSellJobExtras;
		mProductsToSellAllData = productsToSellAllData;
		mSKU = sku;
		mProductID = productID;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		mSettingsSnapshot = new SettingsSnapshot(mHostActivity);
	}
	
	@Override
	protected Integer doInBackground(Void... params) {
		
		JobID jobID = new JobID(mProductID, RES_SELL_MULTIPLE_PRODUCTS, mSKU, mSettingsSnapshot.getUrl());
		Job job = new Job(jobID, mSettingsSnapshot);
		Map<String, Object> extras = new HashMap<String, Object>();
		Map<String, Object> orderDetails = new HashMap<String, Object>();
		ArrayList<Object> orderDetailsItems = new ArrayList<Object>();
		Map<String, Object> paymentMap = new HashMap<String, Object>();
		Map<String, Object> shipmentsMap = new HashMap<String, Object>();
		Map<String, Object> orderListItem = new HashMap<String, Object>();
		String [] skusArray = new String[mProductsToSellAllData.length];
		
		double total = 0;
		
		int i=0;
		for(Object product : mProductsToSellAllData)
		{
			Map<String, Object> productMap = (Map<String, Object>)product;
			Map<String, Object> itemMap = new HashMap<String, Object>();
			
			itemMap.put("sku", productMap.get(MAGEKEY_PRODUCT_SKU));
			itemMap.put("name", productMap.get(MAGEKEY_PRODUCT_NAME2));
			itemMap.put("qty_ordered", productMap.get(MAGEKEY_PRODUCT_QUANTITY));
			itemMap.put("qty", productMap.get(MAGEKEY_PRODUCT_QUANTITY));
			itemMap.put("price_incl_tax", productMap.get(MAGEKEY_PRODUCT_PRICE));
			
			total += new Double((String)productMap.get(MAGEKEY_PRODUCT_QUANTITY)) * new Double((String)productMap.get(MAGEKEY_PRODUCT_PRICE));
			
			orderDetailsItems.add(itemMap);
			
			JobCacheManager.removeCartItem((String)productMap.get("transaction_id"), job.getSettingsSnapshot().getUrl());
			
			skusArray[i] = (String)productMap.get(MAGEKEY_PRODUCT_SKU);
					
			i++;
		}
		
		mOrderIncrementID = "" + job.getJobID().getTimeStamp();
		
		paymentMap.put("base_amount_ordered", "" + total);
		paymentMap.put("method", "dummy");
		paymentMap.put("amount_paid", "" + total);
		
		shipmentsMap.put("items", orderDetailsItems.toArray(new Object[0]));
		shipmentsMap.put("tracks", new Object[0]);
		
		orderDetails.put("increment_id", mOrderIncrementID);
		orderDetails.put("created_at", (String) android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss", new java.util.Date()));
		orderDetails.put("items", orderDetailsItems.toArray(new Object[0]));
		orderDetails.put("payment", paymentMap);
		orderDetails.put("shipments", new Object[]{shipmentsMap});
		orderDetails.put("status", MULTIPLE_PRODUCTS_ORDER_QUEUED_STATE);
		
		orderListItem.put("increment_id", mOrderIncrementID);
		orderListItem.put("created_at", (String) android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss", new java.util.Date()));
		orderListItem.put("items", orderDetailsItems.toArray(new Object[0]));
		
		JobCacheManager.storeOrderDetails(orderDetails, new String [] {mOrderIncrementID}, job.getSettingsSnapshot().getUrl());
		JobCacheManager.addToOrderList(orderListItem, new String[]{OrdersListByStatusProcessor.QUEUED_STATUS_CODE}, job.getSettingsSnapshot().getUrl());
		
		//submit a job
		extras.put(EKEY_PRODUCTS_TO_SELL_ARRAY, mProductsToSellJobExtras);
		extras.put(EKEY_PRODUCT_SKUS_TO_SELL_ARRAY, skusArray);
	
		job.setExtras(extras);

		JobCacheManager.storeMutlipleSellJobStubs(job, job.getSettingsSnapshot().getUrl());
		
		mJobControlInterface.addJobSimple(job);
		
		return 0;
	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);

		Intent myIntent = new Intent(mHostActivity, OrderDetailsActivity.class);
		myIntent.putExtra(mHostActivity.getString(R.string.ekey_order_increment_id), mOrderIncrementID);
		myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		mHostActivity.startActivity(myIntent);
	
		mHostActivity.dismissProgressDialog();
		mHostActivity.finish();
	}
}
