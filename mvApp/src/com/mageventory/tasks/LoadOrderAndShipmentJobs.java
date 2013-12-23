
package com.mageventory.tasks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.OrderShippingActivity;
import com.mageventory.activity.OrderShippingActivity.OrderDataAndShipmentJobs;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.SettingsSnapshot;

public class LoadOrderAndShipmentJobs extends
        BaseTask<OrderShippingActivity, OrderDataAndShipmentJobs> implements OperationObserver,
        MageventoryConstants {

    private boolean mForceRefresh;
    private String mOrderIncrementID;
    private String mSKU;

    private CountDownLatch mDoneSignal;
    private int mRequestID = INVALID_REQUEST_ID;
    private ResourceServiceHelper mResHelper = ResourceServiceHelper.getInstance();
    private boolean mSuccess = false;
    private SettingsSnapshot mSettingsSnapshot;

    public LoadOrderAndShipmentJobs(String orderIncrementID, String SKU, boolean forceRefresh,
            OrderShippingActivity hostActivity)
    {
        super(hostActivity);
        mOrderIncrementID = orderIncrementID;
        mSKU = SKU;
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

        if (mForceRefresh || JobCacheManager.orderDetailsExist(new String[] {
            mOrderIncrementID
        }, mSettingsSnapshot.getUrl()) == false) {

            mDoneSignal = new CountDownLatch(1);
            mResHelper.registerLoadOperationObserver(this);
            mRequestID = mResHelper.loadResource(host, RES_ORDER_DETAILS, new String[] {
                mOrderIncrementID
            }, mSettingsSnapshot);

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

            final OrderDataAndShipmentJobs data = new OrderDataAndShipmentJobs();

            synchronized (JobCacheManager.sSynchronizationObject)
            {
                Map<String, Object> orderDetails = JobCacheManager.restoreOrderDetails(
                        new String[] {
                            mOrderIncrementID
                        }, mSettingsSnapshot.getUrl());

                if (orderDetails != null)
                {
                    List<Job> shipmentJobs = JobCacheManager.restoreShipmentJobs(mSKU,
                            mSettingsSnapshot.getUrl());

                    data.mOrderData = orderDetails;
                    data.mShipmentJobs = shipmentJobs;

                    setData(data);
                }
            }

            finalHost.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (data.mOrderData != null) {
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
