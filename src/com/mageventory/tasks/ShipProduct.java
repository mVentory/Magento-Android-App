package com.mageventory.tasks;

import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.os.AsyncTask;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.OrderDetailsActivity;
import com.mageventory.activity.OrderShippingActivity;

import com.mageventory.job.Job;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.settings.SettingsSnapshot;

public class ShipProduct extends AsyncTask<Void, Void, Integer> implements MageventoryConstants {
	         
	private OrderShippingActivity mHostActivity;
	private JobControlInterface mJobControlInterface;

	private SettingsSnapshot mSettingsSnapshot;

	public ShipProduct(OrderShippingActivity hostActivity) {
		mHostActivity = hostActivity;
		mJobControlInterface = new JobControlInterface(mHostActivity);
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		mSettingsSnapshot = new SettingsSnapshot(mHostActivity);
	}
	
	@Override
	protected Integer doInBackground(Void... arg0) {

		if (isCancelled()) {
			return 0;
		}

		final Map<String, Object> jobExtras = new HashMap<String, Object>();
		
		jobExtras.put(EKEY_SHIPMENT_ORDER_INCREMENT_ID, mHostActivity.getOrderIncrementID());
		jobExtras.put(EKEY_SHIPMENT_TITLE, "");
		jobExtras.put(EKEY_SHIPMENT_CARRIER_CODE, mHostActivity.getCarrierIDField());
		jobExtras.put(EKEY_SHIPMENT_TRACKING_NUMBER, mHostActivity.getTrackingNumberField());
		jobExtras.put(EKEY_SHIPMENT_WITH_TRACKING_PARAMS, mHostActivity.getShipmentWithTrackingParams());
		
		JobID jobID = new JobID(mHostActivity.getProductID(), RES_ORDER_SHIPMENT_CREATE, mHostActivity.getProductSKU(), null);
		Job job = new Job(jobID, mSettingsSnapshot);
		job.setExtras(jobExtras);

		mJobControlInterface.addOrderShipmentCreateJob(job);
		
		return 0;
	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		
		final String ekeyOrderIncrementID = mHostActivity.getString(R.string.ekey_order_increment_id);

		Intent myIntent = new Intent(mHostActivity, OrderDetailsActivity.class);
		myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		myIntent.putExtra(ekeyOrderIncrementID, mHostActivity.getOrderIncrementID());
		mHostActivity.startActivity(myIntent);
			
		mHostActivity.dismissProgressDialog();
		mHostActivity.finish();
	}
}