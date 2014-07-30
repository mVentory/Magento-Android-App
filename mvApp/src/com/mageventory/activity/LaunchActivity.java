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
package com.mageventory.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.TextView;

import com.mageventory.R;
import com.mageventory.job.JobCacheManager;
import com.mageventory.settings.Settings;
import com.mageventory.tasks.LoadAttributeSetTaskAsync;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.SimpleViewLoadingControl;

/**
 * The intermediate activity which handles app restore when app is launched from
 * the android launcher using MAIN, LAUNCHER intent filter
 * 
 * @author Eugene Popovich
 */
public class LaunchActivity extends Activity {

    static final String TAG = LaunchActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean finishActivity = true;
        if (isTaskRoot()) {
            Settings settings = new Settings(getApplicationContext());
            // check whether the cache should be cleared condition
            if (settings.getCacheVersion() == JobCacheManager.CACHE_VERSION) {
                startFirstActivity(LaunchActivity.this);
            } else {
                // clear of old cache is required
                finishActivity = false;
                setContentView(R.layout.inc_overtlay_progress);
                clearCache(settings);
            }
        }

        if (finishActivity) {
            finish();
        }
    }

    /**
     * Clear the cache because of new application version is not compatible
     * with the old cached data
     * 
     * @param settings
     */
    void clearCache(Settings settings) {
        TextView messageView = (TextView) findViewById(R.id.progressMesage);
        messageView.setText(R.string.clearing_old_cache);
        SimpleViewLoadingControl loadingControl = new SimpleViewLoadingControl(
                findViewById(R.id.progressStatus));
        new DeleteCacheTask(settings, loadingControl).execute();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // this prevents StartupActivity recreation on Configuration changes
        // (device orientation changes or hardware keyboard open/close).
        // just do nothing on these changes:
        super.onConfigurationChanged(null);
    }

    /**
     * Start the first activity depend on configuration state. In case there are
     * configured profiles the MainActivity will be launched. Otherwise
     * WelcomeActivity will be launched.
     * 
     * @param context
     */
    public static void startFirstActivity(Context context) {
        Settings settings = new Settings(context.getApplicationContext());
        startFirstActivity(context, settings);
    }

    /**
     * Start the first activity depend on configuration state. In case there are
     * configured profiles the MainActivity will be launched. Otherwise
     * WelcomeActivity will be launched.
     * 
     * @param context
     * @param settings
     */
    public static void startFirstActivity(Context context, Settings settings) {
        Intent launchingIntent;
        if (settings.hasSettings()) {
            launchingIntent = new Intent(context, MainActivity.class);
        } else {
            launchingIntent = new Intent(context, WelcomeActivity.class);
        }
        context.startActivity(launchingIntent);
    }

    /**
     * Async task to clear the cached information (some serialized downloaded
     * data)
     */
    class DeleteCacheTask extends SimpleAsyncTask {
        Settings mSettings;

        public DeleteCacheTask(Settings settings, LoadingControl loadingControl) {
            super(loadingControl);
            mSettings = settings;
        }

        @Override
        protected void onSuccessPostExecute() {
            // set the cache version to the latest one so the new activity
            // launches will not clear cache again
            mSettings.setCacheVersion(JobCacheManager.CACHE_VERSION);
            if (mSettings.hasSettings()) {
                // request preloading of attributes in background because this
                // is very slow operation
                LoadAttributeSetTaskAsync.loadAttributes(false);
            }
            startFirstActivity(LaunchActivity.this);
            finish();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                JobCacheManager.deleteAllCaches();
                return !isCancelled();
            } catch (Exception ex) {
                GuiUtils.error(TAG, R.string.errorGeneral, ex);
            }
            return false;
        }

    }
}
