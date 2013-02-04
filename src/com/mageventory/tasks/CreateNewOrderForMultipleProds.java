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
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.Log;

public class CreateNewOrderForMultipleProds extends AsyncTask<Void, Void, Integer> implements MageventoryConstants {

	private OrderListActivity mHostActivity;
	private JobControlInterface mJobControlInterface;

	private SettingsSnapshot mSettingsSnapshot;
	private Object[] mProductsToSell;
	private String mSKU;
	private int mProductID;

	public CreateNewOrderForMultipleProds(OrderListActivity hostActivity, Object[] productsToSell, String sku, int productID) {
		mHostActivity = hostActivity;
		mJobControlInterface = new JobControlInterface(mHostActivity);
		mProductsToSell = productsToSell;
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
		extras.put(EKEY_PRODUCTS_TO_SELL_ARRAY, mProductsToSell);
	
		job.setExtras(extras);

		mJobControlInterface.addJobSimple(job);
		
		return 0;
	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
/*
		Intent myIntent = new Intent(mHostActivity, OrderDetailsActivity.class);
		myIntent.putExtra(mHostActivity.getString(R.string.ekey_order_increment_id), (String)((Map<String, Object>)((Object [])mLoadOrderListDataTask.getData().get("orders"))[position]).get("increment_id"));
		myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		mHostActivity.startActivity(myIntent);
	*/
		mHostActivity.dismissProgressDialog();
		mHostActivity.finish();
	}
}
