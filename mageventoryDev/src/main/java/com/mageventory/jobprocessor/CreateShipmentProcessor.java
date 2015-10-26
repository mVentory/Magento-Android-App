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

package com.mageventory.jobprocessor;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.jobprocessor.JobProcessorManager.IProcessor;
import com.mageventory.model.Product;

public class CreateShipmentProcessor implements IProcessor, MageventoryConstants {

    @Override
    public void process(Context context, Job job) {
        Map<String, Object> requestData = job.getExtras();

        MagentoClient client;
        try {
            client = new MagentoClient(job.getSettingsSnapshot());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage());
        }

        String orderIncrementId = (String) requestData.get(EKEY_SHIPMENT_ORDER_INCREMENT_ID);
        String title = (String) requestData.get(EKEY_SHIPMENT_TITLE);
        String carrierCode = (String) requestData.get(EKEY_SHIPMENT_CARRIER_CODE);
        String trackingNumber = (String) requestData.get(EKEY_SHIPMENT_TRACKING_NUMBER);
        Map<String, Object> params = (Map<String, Object>) requestData
                .get(EKEY_SHIPMENT_WITH_TRACKING_PARAMS);

        final Map<String, Object> orderDetails = client.shipmentCreate(orderIncrementId,
                carrierCode, title, trackingNumber, params);

        if (orderDetails != null) {
            JobCacheManager.storeOrderDetails(orderDetails, new String[] {
                orderIncrementId
            }, job.getJobID().getUrl());
        } else {
            throw new RuntimeException(client.getLastErrorMessage());
        }
    }
}
