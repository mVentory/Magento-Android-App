
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

    /**
     * The settings snapshot
     */
    protected SettingsSnapshot settingsSnapshot;
    /**
     * The resoruce service helper
     */
    protected ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();

    /**
     * Resource loader which should be used for the loading operation
     */
    protected SynchrnonousResourceLoader resourceLoader;

    /**
     * @param settingsSnapshot the settings snapshot
     * @param loadingControl the loading control which should be used to
     *            indicate resource loading process
     */
    public AbstractSimpleLoadTask(SettingsSnapshot settingsSnapshot, LoadingControl loadingControl) {
        super(loadingControl);
        this.settingsSnapshot = settingsSnapshot;
    }

    @Override
    protected void onSuccessPostExecute() {
    }

    /**
     * @return
     */
    protected boolean loadGeneral() {
        // remote load
        resourceLoader = new SimpleAsyncTaskLoader();
        return resourceLoader.loadGeneral();
    }

    protected abstract int requestLoadResource();

    /**
     * Get the load operation request id
     * 
     * @return load operation request id if resource load request was done or
     *         {@link ResourceConstants.#INVALID_REQUEST_ID} in other cases
     */
    public int getRequestId() {
        return resourceLoader == null ? INVALID_REQUEST_ID : resourceLoader.requestId;
    }

    /**
     * Is the resource loader load operation failed with any error
     * 
     * @return
     */
    public boolean isLoadError() {
        return resourceLoader == null ? false : resourceLoader.isLoadError();
    }

    /**
     * Set the resource loader load operation failed flag value manually
     * 
     * @param loadError
     */
    public void setLoadError(boolean loadError) {
        if (resourceLoader != null) {
            // if resource loader exists
            resourceLoader.setLoadError(loadError);
        }
    }

    @Override
    public void onLoadOperationCompleted(final LoadOperation op) {
    }

    /**
     * Extension of {@link SynchrnonousResourceLoader} for the
     * {@link AbstractSimpleLoadTask}
     */
    public class SimpleAsyncTaskLoader extends SynchrnonousResourceLoader {

        public SimpleAsyncTaskLoader() {
            super(AbstractSimpleLoadTask.this.resHelper);
        }

        @Override
        protected int requestLoadResource() {
            // translate the request to the task
            return AbstractSimpleLoadTask.this.requestLoadResource();
        }

        @Override
        protected boolean isCancelled() {
            // is the task cancelled
            return AbstractSimpleLoadTask.this.isCancelled();
        }

        @Override
        public void onLoadOperationCompleted(LoadOperation op) {
            super.onLoadOperationCompleted(op);
            // pass the event to the async task
            AbstractSimpleLoadTask.this.onLoadOperationCompleted(op);
        }

    }

    /**
     * Abstract synchronous resource loader
     */
    public static abstract class SynchrnonousResourceLoader implements OperationObserver {
        /**
         * The count down latch for the waiting while data is loaded from the
         * server
         */
        private CountDownLatch mDoneSignal;
        /**
         * The resource service helper
         */
        protected ResourceServiceHelper resHelper;
        /**
         * The load resource request id
         */
        protected int requestId = INVALID_REQUEST_ID;

        /**
         * Flag indicating load operation failed with some error
         */
        private boolean mLoadError = false;

        /**
         * @param resHelper resource service helper
         */
        public SynchrnonousResourceLoader(ResourceServiceHelper resHelper) {
            this.resHelper = resHelper;
        }

        /**
         * Load data and await for the result. It synchronize asynchronous load
         * operation via the {@link CountDownLatch}
         * 
         * @return true if data was successfully loaded, otherwise returns false
         */
        public boolean loadGeneral() {
            // remote load
            mDoneSignal = new CountDownLatch(1);
            resHelper.registerLoadOperationObserver(this);

            requestId = requestLoadResource();

            // await while mDoneSignal will not be interrupted or notified
            // remotely
            while (true) {
                if (isCancelled()) {
                    // if operation was cancelled remotely
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
            return !isCancelled() && !isLoadError();
        }

        @Override
        public void onLoadOperationCompleted(final LoadOperation op) {
            if (requestId == op.getOperationRequestId()) {
                // if notification received for the required task then notify
                // mDoneSignal
                mDoneSignal.countDown();

                if (op.getException() != null) {
                    // if there were any exception during loading operation
                    setLoadError(true);
                }
            }
        }

        /**
         * Request load resource and
         * 
         * @return load operation request id
         */
        protected abstract int requestLoadResource();

        /**
         * Whether the operation was cancelled
         * 
         * @return
         */
        protected abstract boolean isCancelled();

        /**
         * Is load operation failed with any error
         * 
         * @return
         */
        public boolean isLoadError() {
            return mLoadError;
        }

        /**
         * Set whether the load operation failed with any error flag
         * 
         * @param loadError
         */
        public void setLoadError(boolean loadError) {
            mLoadError = loadError;
        }
    }
}
