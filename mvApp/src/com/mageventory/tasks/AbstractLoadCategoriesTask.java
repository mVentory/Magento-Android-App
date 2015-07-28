package com.mageventory.tasks;

import java.util.Map;

import com.mageventory.MyApplication;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.LoadingControl;

/**
 * The asynchronous task to load categories list
 */
public class AbstractLoadCategoriesTask extends AbstractSimpleLoadTask {
    /**
     * The tag used for logging
     */
    static final String TAG = AbstractLoadCategoriesTask.class.getSimpleName();
    /**
     * The loaded categories data (raw API output data)
     */
    private Map<String, Object> mData;

    /**
     * Whether the data should be forced to reload even if local cached copy is
     * present
     */
    private boolean mForceReload;

    /**
     * @param forceReload Whether the data should be forced to reload even if
     *            local cached copy is present
     * @param settings the application settings snapshot
     * @param loadingControl the loading control related to the task
     */
    public AbstractLoadCategoriesTask(boolean forceReload,
            SettingsSnapshot settings,
            LoadingControl loadingControl) {
        super(settings, loadingControl);
        CommonUtils.debug(TAG, TAG + ".constructor");
        mForceReload = forceReload;
    }

    @Override
    protected Boolean doInBackground(Void... args) {
        try {
            CommonUtils.debug(TAG, TAG + ".doInBackground executing");
            boolean loadResult = true;

            if ((mForceReload || !JobCacheManager.categoriesExist(settingsSnapshot.getUrl()))) {
                // force reload is requested or categories data doesn't exist in
                // local cache, load from server
                loadResult = loadGeneral();
            }
            if (isCancelled()) {
                return false;
            }
            if (loadResult) {
                // if data was loaded from the server to the cache
                mData = JobCacheManager.restoreCategories(settingsSnapshot.getUrl());
                if (mData != null && !isCancelled()) {
                    // perform extra loading if necessary
                    if (!extraLoadAfterCategoriesDataIsLoaded()) {
                        return false;
                    }
                }
            }
            CommonUtils.debug(TAG, TAG + ".doInBackground completed");
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
     * @return true in case of successful load, false otherwise
     */
    public boolean extraLoadAfterCategoriesDataIsLoaded() {
        return true;
    }

    @Override
    protected int requestLoadResource() {
        return requestLoadCategories(settingsSnapshot, resHelper);
    }

    /**
     * Get the loaded categories data
     * 
     * @return
     */
    public Map<String, Object> getData() {
        return mData;
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
     * Request load categories from the server via the resource service helper
     * 
     * @param settingsSnapshot the settings snapshot
     * @param resHelper the resource service helper
     * @return the load resource operation request code
     */
    public static int requestLoadCategories(SettingsSnapshot settingsSnapshot,
            ResourceServiceHelper resHelper) {
        return resHelper.loadResource(MyApplication.getContext(), RES_CATALOG_CATEGORY_TREE,
                settingsSnapshot);
    }
}

