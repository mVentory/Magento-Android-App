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

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.mageventory.MageventoryConstants;
import com.mageventory.job.Job;

public class JobProcessorManager implements MageventoryConstants {
    public static interface IProcessor {
        public void process(Context context, Job job);
    }

    private static Map<Integer, IProcessor> sResourceProcessors = new HashMap<Integer, IProcessor>();

    public static void bindResourceProcessor(final int resourceType, final IProcessor processor) {
        if (sResourceProcessors.containsKey(resourceType)) {
            throw new IllegalArgumentException(
                    "there is already a processor added for this resource type: "
                            + resourceType);
        }
        sResourceProcessors.put(resourceType, processor);
    }

    /*
     * This function is not pretty but it is for a special case when we need to
     * get updated about some additional data from the image processor while the
     * image is being uploaded.
     */
    public UploadImageProcessor getImageProcessorInstance() {
        UploadImageProcessor processor = (UploadImageProcessor) sResourceProcessors
                .get(MageventoryConstants.RES_UPLOAD_IMAGE);

        if (processor == null) {
            throw new IllegalArgumentException("no processor for resource type: "
                    + MageventoryConstants.RES_UPLOAD_IMAGE);
        }

        return processor;
    }

    public void process(Context context, Job job) {
        IProcessor processor;

        /*
         * In case of product creation job we have two modes: quick sell mode
         * and normal creation mode. Depending on which mode is choosen from the
         * UI we are using different processor classes to process the request.
         */
        if (job.getJobType() == RES_CATALOG_PRODUCT_CREATE)
        {
            boolean quickSellMode = ((Boolean) job.getExtraInfo(EKEY_QUICKSELLMODE)).booleanValue();

            if (quickSellMode == true)
            {
                processor = sResourceProcessors.get(RES_CATALOG_PRODUCT_SELL);
            }
            else
            {
                processor = sResourceProcessors.get(RES_CATALOG_PRODUCT_CREATE);
            }
        }
        else
        {
            processor = sResourceProcessors.get(job.getJobType());
        }

        if (processor == null) {
            throw new IllegalArgumentException("no processor for resource type: "
                    + job.getJobType());
        }
        processor.process(context, job);
    }
}
