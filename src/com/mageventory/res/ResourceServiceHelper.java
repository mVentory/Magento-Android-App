package com.mageventory.res;

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
import android.text.TextUtils;

import com.mageventory.job.JobService;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.settings.SettingsSnapshot;

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

	public int loadResource(final Context context, final int resourceType, SettingsSnapshot settingsSnapshot) {
		return loadResource(context, resourceType, null, null, settingsSnapshot);
	}

	/* The 'params' parameter here is very important. It helps us decide whether two request are asking the server for
	 * the same thing or not. Two requests are the same requests if and only if their resourceTypes and params are the same.
	 * This is why the params that are important for differentiating a request should be passed through 'params' arrays.
	 * For all other parameters we use bundle (there is another function similar to this one that takes an additional bundle).
	 * 
	 * If the upper layers are trying to perform some request and there already is the same request currently being processed
	 * then no second request is performed.
	 * 
	 * Another important thing to note is that two requests are different if and only if they correspond to different cache files
	 * (in case they correspond to any cache file at all). */
	public int loadResource(final Context context, final int resourceType, final String[] params,
		SettingsSnapshot settingsSnapshot) {
		return loadResource(context, resourceType, params, null, settingsSnapshot);
	}

	/**
	 * 
	 * @param context
	 * @param params
	 * @return operation request id
	 */
	public int loadResource(final Context context, final int resourceType, final String[] params, final Bundle extras,
		SettingsSnapshot settingsSnapshot) {
		
		if (settingsSnapshot == null)
			throw new RuntimeException("programming error: settings snapshot is null");
		
		final String resourceUri = buildParameterizedUri(resourceType, params);
		final int requestId = getRequestIdForUri(resourceUri);
		synchronized (ResourceServiceHelper.class) {
			if (isPending(requestId)) {
				return requestId;
			}
			sPendingOperations.add(requestId);
		}

		final Intent serviceIntent;

		serviceIntent = new Intent(context, JobService.class);

		serviceIntent.putExtra(EKEY_MESSENGER, createMessenger());
		serviceIntent.putExtra(EKEY_OP_REQUEST_ID, requestId);
		serviceIntent.putExtra(EKEY_RESOURCE_TYPE, resourceType);
		serviceIntent.putExtra(EKEY_PARAMS, params);
		serviceIntent.putExtra(EKEY_SETTINGS_SNAPSHOT, settingsSnapshot);

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

	private static int requestCounter = 0;

	/**
	 * Build a parameterized URI in this form: baseUri/param1/param2/param3/...
	 * 
	 * @param baseUri
	 * @param params
	 * @return parameterized URI
	 */
	public String buildParameterizedUri(final int resourceType, final String[] params) {
		final String baseUri = String.format("urn:mageventory:resource%d!", resourceType);
		final StringBuilder uriBuilder = new StringBuilder(baseUri);
		if (params == null || params.length == 0) {
			return uriBuilder.toString();
		}
		for (int i = 0; i < params.length; i++) {
			final String param = params[i];
			if (TextUtils.isEmpty(param)) {
				continue;
			}

			uriBuilder.append('/');
			uriBuilder.append(param);
		}
		return uriBuilder.toString();
	}

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

	/* This is a singleton therefore the constructor is private. */
	private ResourceServiceHelper() {
	}

	// TODO y: make the processor instantiating lazy instead
	public void bindResourceProcessor(final int resourceType, final IProcessor processor) {
		ResourceProcessorManager.bindResourceProcessor(resourceType, processor);
	}
}