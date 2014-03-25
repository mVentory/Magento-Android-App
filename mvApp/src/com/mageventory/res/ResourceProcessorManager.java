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

package com.mageventory.res;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

public class ResourceProcessorManager implements ResourceConstants {

    public static interface IProcessor {
        public Bundle process(final Context context, final String[] params, final Bundle extras);
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

    /**
     * Process and store a resource.
     * 
     * @param context
     * @param resourceType
     * @param params
     * @return Bundle any information you want to pass back to the caller.
     */
    public Bundle process(final Context context, final int resourceType, final String[] params,
            final Bundle extras) {
        final IProcessor processor = sResourceProcessors.get(resourceType);
        if (processor == null) {
            throw new IllegalArgumentException("no processor for resource type: " + resourceType);
        }
        return processor.process(context, params, extras);
    }

}
