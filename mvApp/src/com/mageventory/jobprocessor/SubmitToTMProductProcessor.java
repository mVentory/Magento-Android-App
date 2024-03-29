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
import java.util.Random;

import android.content.Context;
import android.util.Base64;
import com.mageventory.util.Log;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.jobprocessor.JobProcessorManager.IProcessor;

public class SubmitToTMProductProcessor implements IProcessor, MageventoryConstants {

    @Override
    public void process(Context context, Job job) {
        Map<String, Object> tmData = job.getExtras();

        MagentoClient client;
        try {
            client = new MagentoClient(job.getSettingsSnapshot());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage());
        }

        Map<String, Object> productMap = client.catalogProductSubmitToTM(job.getJobID()
                .getProductID(), tmData);

        int pid = -1;

        Product product = null;

        if (productMap != null) {

            if (productMap.get(MAGEKEY_PRODUCT_TM_ERROR) != null)
            {
                throw new RuntimeException((String) productMap.get(MAGEKEY_PRODUCT_TM_ERROR));
            }
            product = new Product(productMap);
            pid = Integer.parseInt(product.getId());
        }

        if (pid == -1) {
            throw new RuntimeException(client.getLastErrorMessage());
        } else {
            JobCacheManager.storeProductDetailsWithMergeSynchronous(product, job.getJobID()
                    .getUrl());
        }
    }
}
