/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/

package com.mageventory.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.os.AsyncTask;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.OrderDetailsActivity;
import com.mageventory.activity.OrderShippingActivity;

import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.model.CarriersList;
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

        String carrierName = mHostActivity.getTitleField();
        CarriersList data = JobCacheManager.restoreOrderCarriers(mSettingsSnapshot.getUrl());

        if (data == null)
        {
            data = new CarriersList();
            data.mCarriersList = new ArrayList<String>();
        }

        data.mLastUsedCarrier = carrierName;

        if (!data.mCarriersList.contains(carrierName))
        {
            data.mCarriersList.add(0, carrierName);
        }

        JobCacheManager.storeOrderCarriers(data, mSettingsSnapshot.getUrl());

        jobExtras.put(EKEY_SHIPMENT_ORDER_INCREMENT_ID, mHostActivity.getOrderIncrementID());
        jobExtras.put(EKEY_SHIPMENT_TITLE, carrierName);
        jobExtras.put(EKEY_SHIPMENT_CARRIER_CODE, mHostActivity.getCarrierIDField());
        jobExtras.put(EKEY_SHIPMENT_TRACKING_NUMBER, mHostActivity.getTrackingNumberField());
        jobExtras.put(EKEY_SHIPMENT_WITH_TRACKING_PARAMS,
                mHostActivity.getShipmentWithTrackingParams());

        JobID jobID = new JobID(mHostActivity.getProductID(), RES_ORDER_SHIPMENT_CREATE,
                mHostActivity.getProductSKU(), null);
        Job job = new Job(jobID, mSettingsSnapshot);
        job.setExtras(jobExtras);

        mJobControlInterface.addJobSimple(job);

        return 0;
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        final String ekeyOrderIncrementID = mHostActivity
                .getString(R.string.ekey_order_increment_id);

        Intent myIntent = new Intent(mHostActivity, OrderDetailsActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        myIntent.putExtra(ekeyOrderIncrementID, mHostActivity.getOrderIncrementID());
        mHostActivity.startActivity(myIntent);

        mHostActivity.dismissProgressDialog();
        mHostActivity.finish();
    }
}
