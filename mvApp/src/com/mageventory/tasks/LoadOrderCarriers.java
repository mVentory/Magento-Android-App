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
