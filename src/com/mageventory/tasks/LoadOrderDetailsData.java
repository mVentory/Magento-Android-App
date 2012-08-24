package com.mageventory.tasks;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.AbsProductActivity;
import com.mageventory.activity.OrderDetailsActivity;
import com.mageventory.activity.OrderListActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.SettingsSnapshot;

public class LoadOrderDetailsData extends BaseTask<OrderDetailsActivity, Map<String, Object>> implements
		MageventoryConstants, OperationObserver {

	private ResourceServiceHelper mResHelper = ResourceServiceHelper.getInstance();
	private SettingsSnapshot mSettingsSnapshot;
	private int mOrderDetailsReqId = INVALID_REQUEST_ID;
	private int mNLatches = 0;
	private boolean mSuccess = false;
	private CountDownLatch mDoneSignal;
	private Map<String, Object> myData;
	private String mOrderIncrementId;
	private boolean mRefresh;

	public LoadOrderDetailsData(OrderDetailsActivity hostActivity, String orderIncrementId, boolean refresh) {
		super(hostActivity);
		mOrderIncrementId = orderIncrementId;
		mRefresh = refresh;		
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mSettingsSnapshot = new SettingsSnapshot(getHost());
		
		getHost().onOrderDetailsLoadStart();
	}
	
	@Override
	protected Integer doInBackground(Object... args) {
		if (mRefresh || JobCacheManager.orderDetailsExist(new String [] {mOrderIncrementId}, mSettingsSnapshot.getUrl()) == false) {
			mResHelper.registerLoadOperationObserver(this);
			mOrderDetailsReqId = mResHelper.loadResource(getHost(), RES_ORDER_DETAILS, new String [] {mOrderIncrementId}, mSettingsSnapshot);
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
			myData = JobCacheManager.restoreOrderDetails(new String [] {mOrderIncrementId}, mSettingsSnapshot.getUrl());
			setData(myData);
			getHost().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (myData != null) {
						getHost().onOrderDetailsSuccess();
					} else {
						getHost().onOrderDetailsFailure();
					}
				}
			});
		}
		else
		{
			getHost().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					getHost().onOrderDetailsFailure();
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
		if (op.getOperationRequestId() == mOrderDetailsReqId) {
			if (op.getException() == null) {
				mSuccess = true;
			}
		} else {
			return;
		}

		mDoneSignal.countDown();
	}
}
