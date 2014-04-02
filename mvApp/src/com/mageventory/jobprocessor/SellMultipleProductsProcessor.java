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
import java.util.Map;

import android.content.Context;

import com.mageventory.MageventoryConstants;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.jobprocessor.JobProcessorManager.IProcessor;
import com.mageventory.model.Product;
import com.mageventory.resprocessor.OrdersListByStatusProcessor;

public class SellMultipleProductsProcessor implements IProcessor, MageventoryConstants {

    @Override
    public void process(Context context, Job job) {
        Map<String, Object> requestData = job.getExtras();

        MagentoClient client;
        try {
            client = new MagentoClient(job.getSettingsSnapshot());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage());
        }

        Map<String, Object> res = client.orderForMultipleProductsCreate(JobCacheManager
                .getObjectArrayFromDeserializedItem(requestData.get(EKEY_PRODUCTS_TO_SELL_ARRAY)));

        if (res == null) {
            throw new RuntimeException(client.getLastErrorMessage());
        }
        else
        {
            Map<String, Object> orderDetails = (Map<String, Object>) res.get("order_details");
            Map<String, Object> qtys = null;

            if (res.get("qtys") != null && res.get("qtys") instanceof Map)
            {
                qtys = (Map<String, Object>) res.get("qtys");
            }

            job.setResultData((String) orderDetails.get("increment_id"));
            JobCacheManager.storeOrderDetails(orderDetails, new String[] {
                (String) orderDetails.get("increment_id")
            }, job.getSettingsSnapshot().getUrl());
            JobCacheManager.removeFromOrderList("" + job.getJobID().getTimeStamp(), new String[] {
                OrdersListByStatusProcessor.QUEUED_STATUS_CODE
            }, job.getSettingsSnapshot().getUrl());
            JobCacheManager.removeOrderDetails(new String[] {
                "" + job.getJobID().getTimeStamp()
            }, job.getSettingsSnapshot().getUrl());

            synchronized (JobCacheManager.sSynchronizationObject)
            {
                if (qtys != null)
                {
                    for (String sku : qtys.keySet())
                    {
                        Product product = JobCacheManager.restoreProductDetails(sku, job.getJobID()
                                .getUrl());

                        if (product != null)
                        {
                            product.setQuantity((String) qtys.get(sku));
                            JobCacheManager.storeProductDetailsWithMergeSynchronous(product, job
                                    .getJobID().getUrl());
                        }
                    }
                }
            }
        }
    }
}
