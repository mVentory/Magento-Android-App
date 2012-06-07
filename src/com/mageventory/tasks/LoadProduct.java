package com.mageventory.tasks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Activity;

import com.mageventory.MageventoryConstants;
import com.mageventory.ProductEditActivity;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;

public  class LoadProduct extends BaseTask<ProductEditActivity, Product> implements OperationObserver, MageventoryConstants {

    private CountDownLatch doneSignal;
    private boolean forceRefresh = false;
    private int requestId = INVALID_REQUEST_ID;
    private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
    private int state = TSTATE_NEW;
    private boolean success;

    @Override
    protected Integer doInBackground(Object... args) {
        final String[] params = new String[2];
        params[0] = GET_PRODUCT_BY_SKU; // ZERO --> Use Product ID , ONE --> Use Product SKU
        params[1] = args[0].toString();
        forceRefresh = (Boolean) args[1];

        ProductEditActivity host = getHost();
        if (host == null || isCancelled()) {
            return 0;
        }

        final ProductEditActivity finalHost = host;
        
        if (forceRefresh || resHelper.isResourceAvailable(host, RES_PRODUCT_DETAILS, params) == false) {
            // load
        	
            doneSignal = new CountDownLatch(1);
            resHelper.registerLoadOperationObserver(this);
            requestId = resHelper.loadResource(host, RES_PRODUCT_DETAILS, params);

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
            success = true;
        }

        host = getHost();
        if (host == null || isCancelled()) {
            return 0;
        }

        if (success) {
            final Product data = resHelper.restoreResource(host, RES_PRODUCT_DETAILS, params);
            setData(data);

            finalHost.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (data != null) {
                        finalHost.onProductLoadSuccess();
                    } else {
                        finalHost.onProductLoadFailure();
                    }
                }
            });
        } else {
            finalHost.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    finalHost.onProductLoadFailure();
                }
            });
        }
        
        host = getHost();
        if (host == null || isCancelled()) {
            return 0;
        }
        return 1;
    }

    public int getState() {
        return state;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        state = TSTATE_CANCELED;
    }

    @Override
    public void onLoadOperationCompleted(LoadOperation op) {
        if (op.getOperationRequestId() == requestId) {
            success = op.getException() == null;
            doneSignal.countDown();

            final Activity a = getHost();
            if (a != null) {
                resHelper.stopService(a, false);
            }
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        state = TSTATE_TERMINATED;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        state = TSTATE_RUNNING;
    }

}
