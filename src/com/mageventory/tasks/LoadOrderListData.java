package com.mageventory.tasks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.mageventory.AbsProductActivity;
import com.mageventory.MageventoryConstants;
import com.mageventory.OrderListActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.SettingsSnapshot;

public class LoadOrderListData extends BaseTask<OrderListActivity, Object []> implements
		MageventoryConstants, OperationObserver {

	private ResourceServiceHelper mResHelper = ResourceServiceHelper.getInstance();
	private SettingsSnapshot mSettingsSnapshot;
	private int mOrderListReqId = INVALID_REQUEST_ID;
	private int mNLatches = 0;
	private boolean mSuccess = false;
	private CountDownLatch mDoneSignal;
	private Object [] myData;

	public LoadOrderListData(OrderListActivity hostActivity) {
		super(hostActivity);
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mSettingsSnapshot = new SettingsSnapshot(getHost());
		
		getHost().onOrderListLoadStart();
	}
	
	@Override
	protected Integer doInBackground(Object... args) {
			
		if (JobCacheManager.orderListExist(mSettingsSnapshot.getUrl()) == false) {
			mResHelper.registerLoadOperationObserver(this);
			mOrderListReqId = mResHelper.loadResource(getHost(), RES_ORDERS_LIST_BY_STATUS, mSettingsSnapshot);
			mNLatches += 1;
		} else {
			mSuccess = true;
		}

		if (mNLatches > 0) {
			mDoneSignal = new CountDownLatch(mNLatches);
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
		}
		mResHelper.unregisterLoadOperationObserver(this);	
		
		if (isCancelled()) {
			return 0;
		}
		
		if (mSuccess) {
			myData = JobCacheManager.restoreOrderList(mSettingsSnapshot.getUrl());
			setData(myData);
			getHost().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (myData != null) {
						getHost().onOrderListLoadSuccess();
					} else {
						getHost().onOrderListLoadFailure();
					}
				}
			});
		}
		else
		{
			getHost().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					getHost().onOrderListLoadFailure();
				}
			});
		}
		
		return 0;
	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
	}

	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		if (op.getOperationRequestId() == mOrderListReqId) {
			if (op.getException() == null) {
				mSuccess = true;
			}
		} else {
			return;
		}

		mDoneSignal.countDown();
	}
}
