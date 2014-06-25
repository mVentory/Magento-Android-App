
package com.mageventory.tasks;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.mageventory.MyApplication;
import com.mageventory.activity.ConfigServerActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.concurent.SerialExecutor;

/**
 * Load or reload attribute set information asynchronously without any callbacks
 * 
 * @author Eugene Popovich
 */
public class LoadAttributeSetTaskAsync extends AbstractSimpleLoadTask {

    static final String TAG = LoadAttributeSetTaskAsync.class.getSimpleName();

    public static final SerialExecutor sExecutor = new SerialExecutor(
            Executors.newSingleThreadExecutor());
    private boolean mForceReload;

    /**
     * Map to preserve loaders count for each settings snapshot. Keys are
     * settings snapshot hashCode()
     */
    private static ConcurrentHashMap<Integer, AtomicInteger> sLoaders = new ConcurrentHashMap<Integer, AtomicInteger>();

    /**
     * @param settingsSnapshot the settings snapshot
     * @param forceReload whether the data should be force reloaded if exists
     */
    public LoadAttributeSetTaskAsync(SettingsSnapshot settingsSnapshot, boolean forceReload) {
        super(settingsSnapshot, null);
        mForceReload = forceReload;
        CommonUtils.debug(TAG, "LoadAttributeSetTaskAsync.constructor");
    }

    @Override
    public void startLoading() {
        super.startLoading();
        // increment loaders count so it may be checked in the loadAttributes
        // method. Loaders should be incremented only for cases when forceReload
        // is true because without force reloading operations doesn't take to
        // much time
        if (mForceReload) {
            getLoaders().incrementAndGet();
        }
    }

    @Override
    public void stopLoading() {
        super.stopLoading();
        // decrement loaders count such as loading operation is finished and
        // user may request the attribute set data reloading again
        if (mForceReload) {
            getLoaders().decrementAndGet();
        }
    }

    @Override
    protected void onSuccessPostExecute() {
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            CommonUtils
                    .debug(TAG,
                            "LoadAttributeSetTaskAsync.doInBackground executing with params: forceReload %1$b",
                            mForceReload);
            boolean loadResult = true;
            synchronized (LoadAttributeSets.sCatalogProductAttributesLock) {
                if (mForceReload || !JobCacheManager.attributeSetsExist(settingsSnapshot.getUrl())) {
                    loadResult = loadGeneral();
                }
            }
            CommonUtils.debug(TAG, "LoadAttributeSetTaskAsync.doInBackground completed");
            return !isCancelled() && loadResult;
        } catch (Exception ex) {
            CommonUtils.error(ConfigServerActivity.TAG, ex);
        }
        return false;
    }

    @Override
    protected int requestLoadResource() {
        return resHelper.loadResource(MyApplication.getContext(), RES_CATALOG_PRODUCT_ATTRIBUTES,
                settingsSnapshot);
    }

    /**
     * Get the proper loaders for current task instance. Depends on
     * settingsSnaphot value passed to the constructor
     * 
     * @return
     */
    public AtomicInteger getLoaders() {
        return getLoadersForSettings(settingsSnapshot);
    }

    /**
     * Get the corresponding loaders counter for the settings snapshot.
     * SettingsSnaphot.hashCode() method is used to determine corresponding
     * loaders
     * 
     * @param settings
     * @return
     */
    public static AtomicInteger getLoadersForSettings(SettingsSnapshot settings) {
        int key = settings.hashCode();
        sLoaders.putIfAbsent(key, new AtomicInteger(0));
        return sLoaders.get(key);
    }

    /**
     * Load attributes set asynchronously without any callbacks
     * 
     * @param forceReload whether the data should be force reloaded if exists
     */
    public static void loadAttributes(boolean forceReload) {
        loadAttributes(new SettingsSnapshot(MyApplication.getContext()), forceReload);
    }

    /**
     * Load attributes set asynchronously without any callbacks
     * 
     * @param settingsSnapshot the settings snapshot
     * @param forceReload whether the data should be force reloaded if exists
     */
    public static void loadAttributes(SettingsSnapshot settingsSnapshot, boolean forceReload) {
        // the check to avoid sequential reloading of the attributes list
        // because of it is very long running operation (around 30 seconds)
        if (getLoadersForSettings(settingsSnapshot).get() > 0) {
            CommonUtils
                    .debug(TAG,
                            "LoadAttributeSetTaskAsync.loadAttributes: skipping because of already running task for the same settings");
            return;
        }
        new LoadAttributeSetTaskAsync(settingsSnapshot, forceReload).executeOnExecutor(sExecutor);
    }
}
