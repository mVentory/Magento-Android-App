package com.mageventory.tasks;

import com.mageventory.MyApplication;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCacheManager.ProductDetailsExistResult;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.resprocessor.ProductDetailsProcessor.ProductDetailsLoadException;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.LoadingControl;

/**
 * The asynchronous task to check SKU and load product details
 */
public class AbstractLoadProductTask extends AbstractSimpleLoadTask {
    /**
     * The tag used for logging
     */
    static final String TAG = AbstractLoadProductTask.class.getSimpleName();
    /**
     * Flag indicating whether the product doesn't exist on the server
     */
    boolean mNotExists = false;

    /**
     * The SKU or barcode of the product to load. Will be updated in the
     * doInBackground method with the actual SKU value if the product will be
     * found
     */
    private String mSku;
    /**
     * The original value of the mSku before it gets updated in the
     * doInBackground method
     */
    private String mOriginalSku;

    /**
     * The loaded product details
     */
    private Product mProduct;

    /**
     * @param sku SKU or barcode of the product to load
     * @param settings the application settings snapshot
     * @param loadingControl the loading control related to the task
     */
    public AbstractLoadProductTask(String sku, SettingsSnapshot settings, LoadingControl loadingControl) {
        super(settings, loadingControl);
        CommonUtils.debug(TAG, "AbstractLoadProductTask.constructor");
        mSku = sku;
        mOriginalSku = sku;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            CommonUtils.debug(TAG, "AbstractLoadProductTask.doInBackground executing");
            boolean loadResult = true;
            ProductDetailsExistResult existResult;
            if (!isCancelled()) {
                // check product already present in local cache
                existResult = JobCacheManager.productDetailsExist(mSku, settingsSnapshot.getUrl(),
                        true);
            } else {
                CommonUtils.debug(TAG, "AbstractLoadProductTask.doInBackground: cancelled");
                return false;
            }
            if (!existResult.isExisting()) {
                // product doesn't exist in local cache, load from server
                loadResult = loadGeneral();
            } else {
                // update value to actual one
                mSku = existResult.getSku();
            }
            if (isCancelled()) {
                return false;
            }
            if (loadResult) {
                // if details was loaded from the server to the cache
                mProduct = JobCacheManager.restoreProductDetails(mSku,
                        settingsSnapshot.getUrl());
                if (mProduct != null && !isCancelled()) {
                    // perform extra loading if necessary
                    extraLoadAfterProductIsLoaded();
                }
            }
            CommonUtils.debug(TAG, "AbstractLoadProductTask.doInBackground completed");
            return !isCancelled() && mProduct != null;
        } catch (Exception ex) {
            CommonUtils.error(TAG, ex);
        }
        return false;
    }

    /**
     * Extra loading operations called from the doInBackgrond method. May be
     * overridden to perform extra data initialization in the background
     */
    public void extraLoadAfterProductIsLoaded() {
    }

    @Override
    protected int requestLoadResource() {
        return requestLoadProduct(mSku, settingsSnapshot, resHelper);
    }

    @Override
    public void onLoadOperationCompleted(LoadOperation op) {
        super.onLoadOperationCompleted(op);
        if (op.getOperationRequestId() == getRequestId()
                && op.getResourceType() == RES_PRODUCT_DETAILS) {
            // check whether any exception occurred during loading
            ProductDetailsLoadException exception = (ProductDetailsLoadException) op.getException();
            if (exception != null
                    && exception.getFaultCode() == ProductDetailsLoadException.ERROR_CODE_PRODUCT_DOESNT_EXIST) {
                // product doesn't exist on the server, set the mNotExists
                // flag
                mNotExists = true;
            } else {
                // update SKU variable with the real product SKU in case
                // barcode was scanned
                mSku = op.getExtras().getString(MAGEKEY_PRODUCT_SKU);
            }
        }
    }

    /**
     * Is the server returned product doesn't exist errror
     * 
     * @return
     */
    public boolean isNotExists() {
        return mNotExists;
    }

    /**
     * Get the original SKU passed to the constructor of the task
     * 
     * @return
     */
    public String getOriginalSku() {
        return mOriginalSku;
    }

    /**
     * Get the SKU of the product
     * 
     * @return
     */
    public String getSku() {
        return mSku;
    }

    /**
     * Get the loaded produt details
     * 
     * @return
     */
    public Product getProduct() {
        return mProduct;
    }

    /**
     * Request load product details from the server via the resource service
     * helper
     * 
     * @param sku the SKU or Barcode of the product the details should be loaded
     *            for
     * @param settingsSnapshot the settings snapshot
     * @param resHelper the resource service helper
     * @return the load resource operation request code
     */
    public static int requestLoadProduct(String sku, SettingsSnapshot settingsSnapshot,
            ResourceServiceHelper resHelper) {
        final String[] params = new String[2];
        params[0] = GET_PRODUCT_BY_SKU;
        params[1] = sku;
        return resHelper.loadResource(MyApplication.getContext(), RES_PRODUCT_DETAILS, params,
                settingsSnapshot);
    }
}

