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
import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.jobprocessor.JobProcessorManager.IProcessor;
import com.mageventory.model.Product;

public class UpdateProductProcessor implements IProcessor, MageventoryConstants {

    @Override
    public void process(Context context, Job job) {

        Map<String, Object> requestData = JobCacheManager.cloneMap((Map<String, Object>) job
                .getExtras());

        /*
         * Don't need this key here. It is just here in case we need to merge
         * product edit job file with product details file. We don't need to
         * send that to the server.
         */
        requestData.remove(EKEY_UPDATED_KEYS_LIST);

        boolean success;

        MagentoClient client;
        try {
            client = new MagentoClient(job.getSettingsSnapshot());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage());
        }

        success = client.catalogProductUpdate(job.getJobID().getProductID(), requestData);

        /*
         * For now the edit call doesn't return product details so we just
         * remove the original version of product details from the cache as the
         * edit job was successful and we don't need the original version
         * anymore.
         */
        if (success)
        {
            synchronized (JobCacheManager.sSynchronizationObject)
            {
                Product product = JobCacheManager.restoreProductDetails(job.getSKU(), job
                        .getJobID().getUrl());

                if (product != null)
                {
                    product.setUnmergedProduct(null);
                    JobCacheManager.storeProductDetails(product, job.getJobID().getUrl());
                }
            }

            /* If sku changed then remove all product lists. */
            if (!TextUtils.equals(job.getSKU(), (String) job.getExtraInfo(MAGEKEY_PRODUCT_SKU)))
            {
                JobCacheManager.removeAllProductLists(job.getJobID().getUrl());
            }
        }
        else
        {
            throw new RuntimeException("unsuccessful update");
        }
    }
}
