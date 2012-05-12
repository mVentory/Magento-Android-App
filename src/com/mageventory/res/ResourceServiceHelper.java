package com.mageventory.res;

import static com.mageventory.res.ResourceStateDao.buildParameterizedUri;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;

import com.mageventory.res.ResourceProcessorManager.IProcessor;

public class ResourceServiceHelper implements ResourceConstants {

	public static interface OperationObserver {
		public void onLoadOperationCompleted(final LoadOperation op);
	}

	private static final Callback callback = new Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			final int opRequestId = msg.what;
			final LoadOperation op = (LoadOperation) msg.obj;
			
			final Set<OperationObserver> observersCopy;
			synchronized (ResourceServiceHelper.class) {
				sPendingOperations.remove(opRequestId);
				observersCopy = new HashSet<OperationObserver>(sObservers);
			}
			for (final OperationObserver obs : observersCopy) {
                obs.onLoadOperationCompleted(op);
            }
			return true;
		}

	};

	private static ResourceServiceHelper instance;
	private static final Set<OperationObserver> sObservers = new HashSet<OperationObserver>();
	private static final Set<Integer> sPendingOperations = new HashSet<Integer>();
	private static final Map<String, Integer> sUriToRequestId = new HashMap<String, Integer>();
	@SuppressWarnings("unused")
	private static final String TAG = "ResourceServiceHelper";

	private Messenger createMessenger() {
		final Handler handler;
		if (Looper.myLooper() == null) {
			handler = new Handler(Looper.getMainLooper(), callback);
		} else {
			handler = new Handler(callback);
		}
		return new Messenger(handler);
	}

	public static ResourceServiceHelper getInstance() {
		if (instance == null) {
			synchronized (ResourceServiceHelper.class) {
				if (instance == null) {
					instance = new ResourceServiceHelper();
				}
			}
		}
		return instance;
	}

	public boolean isPending(final int operationRequestId) {
		synchronized (ResourceServiceHelper.class) {
			return sPendingOperations.contains(operationRequestId);
		}
	}

	public int loadResource(final Context context, final int resourceType) {
		return loadResource(context, resourceType, null, null);
	}
	
	public int loadResource(final Context context, final int resourceType, final String[] params) {
		return loadResource(context, resourceType, params, null);
	}

	/**
	 * 
	 * @param context
	 * @param params
	 * @return operation request id
	 */
	public int loadResource(final Context context, final int resourceType, final String[] params, final Bundle extras) {
		final String resourceUri = buildParameterizedUri(resourceType, params);
		final int requestId = getRequestIdForUri(resourceUri);
		synchronized (ResourceServiceHelper.class) {
			if (isPending(requestId)) {
				return requestId;
			}
			sPendingOperations.add(requestId);
		}
		final Intent serviceIntent = new Intent(context, ResourceService.class);
		serviceIntent.putExtra(EKEY_MESSENGER, createMessenger());
		serviceIntent.putExtra(EKEY_OP_REQUEST_ID, requestId);
		serviceIntent.putExtra(EKEY_RESOURCE_TYPE, resourceType);
		serviceIntent.putExtra(EKEY_PARAMS, params);
		
		if (extras != null) {
			serviceIntent.putExtra(EKEY_REQUEST_EXTRAS, extras);
		}
		
		context.startService(serviceIntent);
		return requestId;
	}
	
	public void registerLoadOperationObserver(final OperationObserver observer) {
	    synchronized (ResourceServiceHelper.class) {
	        sObservers.add(observer);
	    }
	}

	public void unregisterLoadOperationObserver(final OperationObserver observer) {
	    synchronized (ResourceServiceHelper.class) {
	        sObservers.remove(observer);
	    }
	}

	private <T> T restoreResource(final Context context, final String resourceUri) {
		try {
			final T data = ResourceCache.getInstance().restore(context, resourceUri);
			if (data == null) {
				// clear row if data is unavailable
				final ResourceStateDao stateDao = new ResourceStateDao(context);
				stateDao.deleteResource(resourceUri);
			}
			return data;
		} catch (IOException e) {
			return null;
		}
	}

	public <T> T restoreResource(final Context context, final int resourceType) {
		return restoreResource(context, resourceType, null);
	}

	public <T> T restoreResource(final Context context, final int resourceType, final String[] params) {
		final String resourceUri = buildParameterizedUri(resourceType, params);
		return restoreResource(context, resourceUri);
	}
	
	public void deleteResource(final Context context, final int resourceType, final String[] params) {
		final String resourceUri = buildParameterizedUri(resourceType, params);
		final ResourceStateDao stateDao = new ResourceStateDao(context);
		stateDao.deleteResource(resourceUri);
	}

	private static int requestCounter = 0;

	private int getRequestIdForUri(final String resourceUri) {
		synchronized (sUriToRequestId) {
			Integer requestId = sUriToRequestId.get(resourceUri);
			if (requestId == null) {
				requestId = ++requestCounter;
				sUriToRequestId.put(resourceUri, requestId);
			}
			return requestId;
		}
	}

	public boolean isResourceAvailable(Context context, final int resourceType) {
		return isResourceAvailable(context, resourceType, null);
	}

	public boolean isResourceAvailable(Context context, final int resourceType, final String[] params) {
		return isResourceAvailable(context, buildParameterizedUri(resourceType, params));
	}

	boolean isResourceAvailable(Context context, final String resourceUri) {
		final ResourceStateDao stateDao = new ResourceStateDao(context);
		final ResourceRepr res = stateDao.getResource(resourceUri);
		if (res == null || res.isAvailable() == false || res.old
		        || ResourceCache.contains(context, resourceUri) == false) {
			stateDao.deleteResource(resourceUri);
			return false;
		}
		return true;
	}

	/**
	 * Stop the running resource service.
	 * 
	 * @param context
	 * @param force
	 *            If this is true, the service will be stopped even if there are
	 *            still pending operations, otherwise, if it's false, the
	 *            service will be shut down only if there are no more operations
	 *            for it to process.
	 * @return
	 */
	public boolean stopService(final Context context, final boolean force) {
		if (force || sPendingOperations.isEmpty()) {
			final Intent service = new Intent(context, ResourceService.class);
			return context.stopService(service);
		}
		return false;
	}

	// TODO y: make the processor instantiating lazy instead
	public void bindResourceProcessor(final int resourceType, final IProcessor processor) {
		ResourceProcessorManager.bindResourceProcessor(resourceType, processor);
	}

	public boolean markResourceAsOld(final Context context, final int resourceType) {
		return markResourceAsOld(context, resourceType, new String[] { "*" });
	}

	public boolean markResourceAsOld(final Context context, final int resourceType, final String[] params) {
		final String resourceUri = buildParameterizedUri(resourceType, params);
		return markResourceAsOld(context, resourceUri);
	}

	/**
	 * The character '*' is considered as wildcard that matches everything.
	 * 
	 * @param context
	 * @param resourceUri
	 * @return
	 */
	private boolean markResourceAsOld(final Context context, final String resourceUri) {
		final ResourceStateDao stateDao = new ResourceStateDao(context);
		return stateDao.setOld(resourceUri, true);
	}

	boolean isOld(Context context, String resourceUri) {
		final ResourceStateDao stateDao = new ResourceStateDao(context);
		return stateDao.isOld(resourceUri);
    }

}