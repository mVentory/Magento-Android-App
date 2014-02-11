package com.mageventory.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

/**
 * The intermediate activity which handles app restore when app is launched from
 * the android launcher using MAIN, LAUNCHER intent filter
 * 
 * @author Eugene Popovich
 */
public class LaunchActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isTaskRoot()) {
            Intent i = new Intent(LaunchActivity.this, MainActivity.class);
            startActivity(i);
        }

        finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // this prevents StartupActivity recreation on Configuration changes
        // (device orientation changes or hardware keyboard open/close).
        // just do nothing on these changes:
        super.onConfigurationChanged(null);
    }
}
