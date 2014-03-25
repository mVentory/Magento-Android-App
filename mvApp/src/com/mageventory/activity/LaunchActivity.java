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
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import com.mageventory.settings.Settings;

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
            Settings settings = new Settings(getApplicationContext());
            if (settings.hasSettings()) {
                Intent i = new Intent(LaunchActivity.this, MainActivity.class);
                startActivity(i);
            } else {
                Intent i = new Intent(LaunchActivity.this, WelcomeActivity.class);
                startActivity(i);
            }
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
