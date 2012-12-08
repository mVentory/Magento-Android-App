package com.mageventory.tasks;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.AbsProductActivity;
import com.mageventory.activity.AbsProductActivity.CategoriesData;
import com.mageventory.activity.OrderShippingActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.SettingsSnapshot;

public class LoadOrderCarriers extends BaseTask<OrderShippingActivity, Map<String, Object>> implements MageventoryConstants,
		OperationObserver {

	private boolean mForceRefresh;
	private CountDownLatch mDoneSignal;
	private int mRequestId = INVALID_REQUEST_ID;
	private boolean mSuccess = false;
	private ResourceServiceHelper mResHelper = ResourceServiceHelper.getInstance();
	private SettingsSnapshot mSettingsSnapshot;
	private String mOrderIncrementId;

	public LoadOrderCarriers(String orderIncrementId, boolean forceRefresh, OrderShippingActivity hostActivity) {
		super(hostActivity);
		mOrderIncrementId = orderIncrementId;
		mForceRefresh = forceRefresh;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		getHost().onOrderCarriersLoadStart();
		mSettingsSnapshot = new SettingsSnapshot(getHost());
	}

	@Override
	protected Integer doInBackground(Object... args) {
		
		OrderShippingActivity host = getHost();
		if (isCancelled()) {
			return 0;
		}

		final OrderShippingActivity finalHost = host;
		
		if (mForceRefresh || JobCacheManager.orderCarriersExist(mSettingsSnapshot.getUrl()) == false) {
			mResHelper.registerLoadOperationObserver(this);
			mRequestId = mResHelper.loadResource(host, RES_GET_ORDER_CARRIERS, new String [] {mOrderIncrementId}, mSettingsSnapshot);
			mDoneSignal = new CountDownLatch(1);
			while (true) {
				if (isCancelled()) {
					return 0;
				}
				try {
					if (mDoneSignal.await(1, TimeUnit.SECONDS)) {
						break;
					}
				} catch (InterruptedException e) {
					return 0;
				}
			}
		} else {
			mSuccess = true;
		}

		mResHelper.unregisterLoadOperationObserver(this);

		if (isCancelled()) {
			return 0;
		}

		if (mSuccess) {
			final Map<String, Object> data = JobCacheManager.restoreOrderCarriers(mSettingsSnapshot.getUrl());
			setData(data);
			
			host.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (data != null) {
						finalHost.onOrderCarriersLoadSuccess();
					} else {
						finalHost.onOrderCarriersLoadFailure();
					}
				}
			});
		}
		else
		{
			host.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					finalHost.onOrderCarriersLoadFailure();
				}
			});
		}
		return 0;
	}

	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		if (op.getOperationRequestId() == mRequestId) {
			mSuccess = op.getException() == null;
			mDoneSignal.countDown();
		}
	}
}
