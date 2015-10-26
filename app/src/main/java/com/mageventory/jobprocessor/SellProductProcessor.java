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

public class SellProductProcessor implements IProcessor, MageventoryConstants {

    @Override
    public void process(Context context, Job job) {
        Map<String, Object> requestData = JobCacheManager.cloneMap((Map<String, Object>) job
                .getExtras());

        /*
         * Don't need this key here. It is just there to pass info about product
         * creation mode selected by the user.
         */
        requestData.remove(EKEY_QUICKSELLMODE);

        requestData.put(MAGEKEY_PRODUCT_TRANSACTION_ID, job.getJobID());

        MagentoClient client;
        try {
            client = new MagentoClient(job.getSettingsSnapshot());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage());
        }

        final Map<String, Object> productMap = client.orderCreate(requestData);

        final Product product;
        if (productMap != null) {
            product = new Product(productMap);
        } else {
            throw new RuntimeException(client.getLastErrorMessage());
        }

        // cache
        if (product != null) {
            JobCacheManager.storeProductDetailsWithMergeSynchronous(product, job.getJobID()
                    .getUrl());

            Boolean quickSellMode = ((Boolean) job
                    .getExtraInfo(MageventoryConstants.EKEY_QUICKSELLMODE));

            /*
             * If QUICKSELLMODE key is present in the job extra info this means
             * that the user wants to create a product and sell it at the same
             * time (we only set this key in case user creates a product and
             * don't set it when user just sells a product). We want to make
             * sure that the list of products gets refreshed after product
             * creation next time user sees it so we remove all lists from the
             * cache here.
             */
            if (quickSellMode != null)
            {
                JobCacheManager.removeAllProductLists(job.getJobID().getUrl());
            }
        }
    }
}
