package com.mageventory.tasks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.mageventory.AbsProductActivity;
import com.mageventory.MageventoryConstants;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;

public class LoadAttributes extends BaseTask<AbsProductActivity, List<Map<String, Object>>> implements
		MageventoryConstants, OperationObserver {

	private CountDownLatch doneSignal;
	private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
	private boolean forceRefresh = false;

	private int state = TSTATE_NEW;
	private boolean atrSuccess;
	private int atrRequestId = INVALID_REQUEST_ID;

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		state = TSTATE_RUNNING;

		getHost().onAttributeSetLoadStart();
		getHost().onAttributeListLoadStart();
	}

	@Override
	protected Integer doInBackground(Object... args) {
		if (args == null || args.length != 1) {
			throw new IllegalArgumentException();
		}
		if (args[0] instanceof Boolean == false) {
			throw new IllegalArgumentException();
		}

		if (getHost().inputCache == null)
		{
			final Map<String, List<String>> inputCache = JobCacheManager.loadInputCache();
			
			getHost().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					getHost().onInputCacheLoaded(inputCache);
				}
			});
		}
		
		
		forceRefresh = (Boolean) args[0];

		AbsProductActivity host = getHost();
		if (host == null) {
			return 0;
		}

		if (isCancelled()) {
			return 0;
		}

		if (forceRefresh || JobCacheManager.attributesExist() == false) {
			// remote load
			doneSignal = new CountDownLatch(1);
			resHelper.registerLoadOperationObserver(this);

			atrRequestId = resHelper.loadResource(host, RES_CATALOG_PRODUCT_ATTRIBUTES);

			while (true) {
				if (isCancelled()) {
					return 0;
				}
				try {
					if (doneSignal.await(10, TimeUnit.SECONDS)) {
						break;
					}
				} catch (InterruptedException e) {
					return 0;
				}
			}

			resHelper.unregisterLoadOperationObserver(this);
		} else {
			atrSuccess = true;
		}

		if (isCancelled()) {
			return 0;
		}

		final List<Map<String, Object>> atrs;
		if (atrSuccess) {
			atrs = JobCacheManager.restoreAttributes();
		} else {
			atrs = null;
		}
		setData(atrs);

		if (isCancelled()) {
			return 0;
		}

		host = getHost();

		if (host != null) {
			final AbsProductActivity finalHost = host;
			host.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (atrs != null) {
						finalHost.onAttributeSetLoadSuccess();
						finalHost.onAttributeListLoadSuccess();
					} else {
						finalHost.onAttributeSetLoadFailure();
						finalHost.onAttributeListLoadFailure();
					}
				}
			});
		}

		return 0;
	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		state = TSTATE_TERMINATED;
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
		state = TSTATE_CANCELED;
	}

	@Override
	public void onLoadOperationCompleted(final LoadOperation op) {
		// final AbsProductActivity host = getHost();
		if (atrRequestId == op.getOperationRequestId()) {
			atrSuccess = op.getException() == null;
			doneSignal.countDown();
		}
	}

	public int getState() {
		return state;
	}

}
