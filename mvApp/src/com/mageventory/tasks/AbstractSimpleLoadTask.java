
package com.mageventory.tasks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.mageventory.MageventoryConstants;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.SimpleAsyncTask;

/**
 * Abstract resource loading task which awaits from the response from the
 * loading service
 */
public abstract class AbstractSimpleLoadTask extends SimpleAsyncTask implements OperationObserver,
        MageventoryConstants {
    static final String TAG = AbstractSimpleLoadTask.class.getSimpleName();

    protected SettingsSnapshot settingsSnapshot;
    private CountDownLatch mDoneSignal;
    private int mRequestId = INVALID_REQUEST_ID;
    protected ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();

    public AbstractSimpleLoadTask(SettingsSnapshot settingsSnapshot, LoadingControl loadingControl) {
        super(loadingControl);
        this.settingsSnapshot = settingsSnapshot;
    }

    @Override
    protected void onSuccessPostExecute() {
    }

    protected boolean loadGeneral() {
        // remote load
        mDoneSignal = new CountDownLatch(1);
        resHelper.registerLoadOperationObserver(this);

        mRequestId = requestLoadResource();

        while (true) {
            if (isCancelled()) {
                return false;
            }
            try {
                if (mDoneSignal.await(1, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                return false;
            }
        }

        resHelper.unregisterLoadOperationObserver(this);
        return !isCancelled();
    }

    protected abstract int requestLoadResource();

    @Override
    public void onLoadOperationCompleted(final LoadOperation op) {
        if (mRequestId == op.getOperationRequestId()) {
            mDoneSignal.countDown();
        }
    }
}
