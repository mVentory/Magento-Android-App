package com.mageventory.res;

import static com.mageventory.res.ResourceStateDao.buildParameterizedUri;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

public class ResourceProcessorManager implements ResourceConstants {

	public static interface IProcessor {
		public Bundle process(final Context context, final String[] params, final Bundle extras,
				final String parameterizedResourceUri, final ResourceStateDao state, final ResourceCache cache);
	}

	private static Map<Integer, IProcessor> sResourceProcessors = new HashMap<Integer, IProcessor>();

	public static void bindResourceProcessor(final int resourceType, final IProcessor processor) {
		if (sResourceProcessors.containsKey(resourceType)) {
			throw new IllegalArgumentException("there is already a processor added for this resource type: "
					+ resourceType);
		}
		sResourceProcessors.put(resourceType, processor);
	}

	private static ResourceStateDao sStateDao;
	@SuppressWarnings("unused")
	private static final String TAG = "DataProcessor";

	private static ResourceStateDao getStateDao(final Context context) {
		if (sStateDao == null) {
			sStateDao = new ResourceStateDao(context);
		}
		return sStateDao;
	}

	/**
	 * Process and store a resource.
	 * 
	 * @param context
	 * @param resourceType
	 * @param params
	 * @return Bundle any information you want to pass back to the caller.
	 */
	public Bundle process(final Context context, final int resourceType, final String[] params, final Bundle extras) {
		final IProcessor processor = sResourceProcessors.get(resourceType);
		if (processor == null) {
			throw new IllegalArgumentException("no processor for resource type: " + resourceType);
		}
		final String resourceUri = buildParameterizedUri(resourceType, params);
		final ResourceStateDao state = getStateDao(context);
		final ResourceCache store = ResourceCache.getInstance();
		return processor.process(context, params, extras, resourceUri, state, store);
	}

}
