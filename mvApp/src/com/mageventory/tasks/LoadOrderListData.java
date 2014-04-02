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

package com.mageventory.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.OrderListActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.OrderList;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.resprocessor.OrdersListByStatusProcessor;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.SettingsSnapshot;

public class LoadOrderListData extends BaseTask<OrderListActivity, Map<String, Object>> implements
        MageventoryConstants, OperationObserver {

    public static final String CART_ITEMS_KEY = "cart_items_key";
    public static final String STATUSES = "statuses";

    private ResourceServiceHelper mResHelper = ResourceServiceHelper.getInstance();
    private SettingsSnapshot mSettingsSnapshot;
    private int mReqId = INVALID_REQUEST_ID;
    private int mNLatches = 0;
    private boolean mSuccess = false;
    private CountDownLatch mDoneSignal;
    private Map<String, Object> myData;
    private String mStatus;
    private boolean mRefresh;
    private boolean mNeedListOfStatuses;

    public LoadOrderListData(OrderListActivity hostActivity, String status, boolean refresh,
            boolean needListOfStatuses) {
        super(hostActivity);
        mRefresh = refresh;
        mStatus = status;
        mNeedListOfStatuses = needListOfStatuses;
    }

    public String getStatusParam()
    {
        return mStatus;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mSettingsSnapshot = new SettingsSnapshot(getHost());

        getHost().onOrderListLoadStart();
    }

    private Integer loadShippingCart()
    {
        mNLatches = 0;
        mSuccess = false;

        if (mRefresh || JobCacheManager.cartItemsExist(mSettingsSnapshot.getUrl()) == false) {
            mResHelper.registerLoadOperationObserver(this);
            mReqId = mResHelper.loadResource(getHost(), RES_CART_ITEMS, mSettingsSnapshot);
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

            if (myData == null)
            {
                myData = new HashMap<String, Object>();
            }
            myData.put(CART_ITEMS_KEY, JobCacheManager.restoreCartItems(mSettingsSnapshot.getUrl()));

            setData(myData);
            getHost().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (myData.get(CART_ITEMS_KEY) != null) {
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

    private Integer loadOrderList(boolean doNotifyUI)
    {
        mNLatches = 0;
        mSuccess = false;

        String status = mStatus;

        if (status.equals(OrdersListByStatusProcessor.SHOPPING_CART_STATUS_CODE))
        {
            status = OrdersListByStatusProcessor.LATEST_STATUS_CODE;
        }

        if (status.equals(OrdersListByStatusProcessor.QUEUED_STATUS_CODE))
        {
            mRefresh = false;
        }

        if (mRefresh || JobCacheManager.orderListExist(new String[] {
            status
        }, mSettingsSnapshot.getUrl()) == false) {

            if (status.equals(OrdersListByStatusProcessor.QUEUED_STATUS_CODE))
            {
                JobCacheManager.saveEmptyOrderList(new String[] {
                    status
                }, mSettingsSnapshot.getUrl());
                mSuccess = true;
            }
            else
            {
                mResHelper.registerLoadOperationObserver(this);
                mReqId = mResHelper.loadResource(getHost(), RES_ORDERS_LIST_BY_STATUS,
                        new String[] {
                            status
                        }, mSettingsSnapshot);
                mNLatches += 1;
            }
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
            OrderList orderList = JobCacheManager.restoreOrderList(new String[] {
                    status
            }, mSettingsSnapshot.getUrl());
            myData = orderList.getData();
            myData.put(STATUSES, orderList.getOrderStatusList());
            setData(myData);

            if (doNotifyUI)
            {
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
        }
        else
        {
            if (doNotifyUI)
            {
                getHost().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getHost().onOrderListLoadFailure();
                    }
                });
            }
        }

        return 0;
    }

    @Override
    protected Integer doInBackground(Object... args) {

        if (mStatus.equals(OrdersListByStatusProcessor.SHOPPING_CART_STATUS_CODE))
        {
            if (mNeedListOfStatuses == true)
            {
                loadOrderList(false);

                if (myData == null)
                {
                    getHost().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getHost().onOrderListLoadFailure();
                        }
                    });
                    return 0;
                }
            }

            return loadShippingCart();
        }
        else
        {
            return loadOrderList(true);
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
    }

    @Override
    public void onLoadOperationCompleted(LoadOperation op) {
        if (op.getOperationRequestId() == mReqId) {
            if (op.getException() == null) {
                mSuccess = true;
            }
        } else {
            return;
        }

        mDoneSignal.countDown();
    }
}
