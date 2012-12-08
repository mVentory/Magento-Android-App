package com.mageventory.tasks;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Activity;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.CategoryListActivity;
import com.mageventory.activity.OrderShippingActivity;
import com.mageventory.activity.ProductEditActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.SettingsSnapshot;

public class LoadOrder extends BaseTask<OrderShippingActivity, Map<String, Object>> implements OperationObserver,
		MageventoryConstants {

	private boolean mForceRefresh;
	private String mOrderIncrementID;
	
	private CountDownLatch mDoneSignal;
	private int mRequestID = INVALID_REQUEST_ID;
	private ResourceServiceHelper mResHelper = ResourceServiceHelper.getInstance();
	private boolean mSuccess = false;
	private SettingsSnapshot mSettingsSnapshot;

	public LoadOrder(String orderIncrementID, boolean forceRefresh, OrderShippingActivity hostActivity)
	{
		super(hostActivity);
		mOrderIncrementID = orderIncrementID;
		mForceRefresh = forceRefresh;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mSettingsSnapshot = new SettingsSnapshot(getHost());
		getHost().onOrderLoadStart();
	}
	
	@Override
	protected Integer doInBackground(Object... args) {
		OrderShippingActivity host = getHost();
		if (isCancelled()) {
			return 0;
		}

		final OrderShippingActivity finalHost = host;

		if (mForceRefresh || JobCacheManager.orderDetailsExist(new String [] {mOrderIncrementID}, mSettingsSnapshot.getUrl()) == false) {

			mDoneSignal = new CountDownLatch(1);
			mResHelper.registerLoadOperationObserver(this);
			mRequestID = mResHelper.loadResource(host, RES_ORDER_DETAILS, new String [] {mOrderIncrementID}, mSettingsSnapshot);

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
			mResHelper.unregisterLoadOperationObserver(this);
		} else {
			mSuccess = true;
		}

		if (isCancelled()) {
			return 0;
		}

		if (mSuccess) {
			final Map<String, Object> data = JobCacheManager.restoreOrderDetails(new String [] {mOrderIncrementID}, mSettingsSnapshot.getUrl());
			setData(data);

			finalHost.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (data != null) {
						finalHost.onOrderLoadSuccess();
					} else {
						finalHost.onOrderLoadFailure();
					}
				}
			});
		} else {
			finalHost.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					finalHost.onOrderLoadFailure();
				}
			});
		}

		return 1;
	}

	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		if (op.getOperationRequestId() == mRequestID) {
			mSuccess = op.getException() == null;
			mDoneSignal.countDown();
		}
	}
}
