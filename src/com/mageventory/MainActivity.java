package com.mageventory;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.client.MagentoClient;
import com.mageventory.settings.Settings;

public class MainActivity extends BaseActivity {
	private Settings settings;
	public static final String PREFS_NAME = "pref.dat";
	MagentoClient magentoClient;
	ProgressDialog pDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		settings = new Settings(getApplicationContext());
		if (settings.hasSettings()) {
			((TextView) findViewById(R.id.config_state)).setText(settings.getUrl().substring(0, 20) + "...");
		} else {
			Toast.makeText(getApplicationContext(), "Make Config", 1000);
		}
		// openOptionsMenu();

	}

}
