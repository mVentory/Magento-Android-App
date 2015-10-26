package com.mageventory.tasks;

import java.util.List;
import java.util.Map;

import com.mageventory.MyApplication;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.LoadingControl;

/**
 * The asynchronous task to load product list
 */
public class AbstractLoadProductListTask extends AbstractSimpleLoadTask {
    /**
     * The tag used for logging
     */
    static final String TAG = AbstractLoadProductListTask.class.getSimpleName();
    /**
     * The loaded product list data (raw API output data)
     */
    private List<Map<String, Object>> mData;

    /**
     * The string filter used for the product list API call
     */
    private String mNameFilter;
    /**
     * Whether the data should be forced to reload even if local cached copy is
     * present
     */
    private boolean mForceReload;

    /**
     * @param nameFilter The string filter used for the product list API call
     * @param forceReload Whether the data should be forced to reload even if
     *            local cached copy is present
     * @param settings the application settings snapshot
     * @param loadingControl the loading control related to the task
     */
    public AbstractLoadProductListTask(String nameFilter, boolean forceReload,
            SettingsSnapshot settings,
            LoadingControl loadingControl) {
        super(settings, loadingControl);
        CommonUtils.debug(TAG, "AbstractLoadProductListTask.constructor");
        mNameFilter = nameFilter;
        mForceReload = forceReload;
    }

    @Override
    protected Boolean doInBackground(Void... args) {
        try {
            CommonUtils.debug(TAG, "AbstractLoadProductListTask.doInBackground executing");
            boolean loadResult = true;
            final String[] params = new String[] {
                mNameFilter
            };

            if ((mForceReload || !JobCacheManager.productListExist(params,
                    settingsSnapshot.getUrl()))) {
                // force reload is requested or product list doesn't exist in
                // local cache, load from server
                loadResult = loadGeneral();
            }
            if (isCancelled()) {
                return false;
            }
            if (loadResult) {
                // if data was loaded from the server to the cache
                mData = JobCacheManager.restoreProductList(params,
                        settingsSnapshot.getUrl());
                if (mData != null && !isCancelled()) {
                    // perform extra loading if necessary
                    if (!extraLoadAfterProductListIsLoaded()) {
                        return false;
                    }
                }
            }
            CommonUtils.debug(TAG, "AbstractLoadProductListTask.doInBackground completed");
            return !isCancelled() && mData != null;
        } catch (Exception ex) {
            CommonUtils.error(TAG, ex);
        }
        return false;
    }

    /**
     * Extra loading operations called from the doInBackgrond method. May be
     * overridden to perform extra data initialization in the background
     * 
     * @return true in case of successfull load, false otherwise
     */
    public boolean extraLoadAfterProductListIsLoaded() {
        return true;
    }

    @Override
    protected int requestLoadResource() {
        return requestLoadProductList(mNameFilter, settingsSnapshot, resHelper);
    }

    /**
     * Get the loaded product list data
     * 
     * @return
     */
    public List<Map<String, Object>> getData() {
        return mData;
    }

    /**
     * Get the name filter associated with the task
     * 
     * @return
     */
    public String getNameFilter() {
        return mNameFilter;
    }

    /**
     * Get the forceReload flag value associated with the task
     * 
     * @return
     */
    public boolean isForceReload() {
        return mForceReload;
    }

    /**
     * Request load product list from the server via the resource service helper
     * 
     * @param nameFilter the string filter used for the API call
     * @param settingsSnapshot the settings snapshot
     * @param resHelper the resource service helper
     * @return the load resource operation request code
     */
    public static int requestLoadProductList(String nameFilter, SettingsSnapshot settingsSnapshot,
            ResourceServiceHelper resHelper) {
        final String[] params = new String[] {
            nameFilter
        };
        return resHelper.loadResource(MyApplication.getContext(), RES_CATALOG_PRODUCT_LIST, params,
                settingsSnapshot);
    }
}

