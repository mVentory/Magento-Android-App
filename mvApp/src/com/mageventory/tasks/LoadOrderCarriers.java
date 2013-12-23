
package com.mageventory.tasks;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.OrderShippingActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.CarriersList;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.SettingsSnapshot;

public class LoadOrderCarriers extends BaseTask<OrderShippingActivity, CarriersList> implements
        MageventoryConstants {

    private SettingsSnapshot mSettingsSnapshot;

    public LoadOrderCarriers(OrderShippingActivity hostActivity) {
        super(hostActivity);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        getHost().onOrderCarriersLoadStart();
        mSettingsSnapshot = new SettingsSnapshot(getHost());
    }

    @Override
    protected Integer doInBackground(Object... args) {

        OrderShippingActivity host = getHost();
        if (isCancelled()) {
            return 0;
        }

        final CarriersList data = JobCacheManager.restoreOrderCarriers(mSettingsSnapshot.getUrl());
        setData(data);

        final OrderShippingActivity finalHost = host;

        host.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finalHost.onOrderCarriersLoadSuccess();
            }
        });

        return 0;
    }

}
