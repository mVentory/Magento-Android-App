package com.mageventory;

import android.app.ProgressDialog;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.util.Linkify;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.client.MagentoClient;
import com.mageventory.settings.Settings;

public class MainActivity extends BaseActivity {
	protected MyApplication app;
	private Settings settings;
	public static final String PREFS_NAME = "pref.dat";
	ProgressDialog pDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		app=(MyApplication) getApplication();
		settings=new Settings(getApplicationContext());
		this.setTitle("Mventory: Home");
		TextView versioname= (TextView) findViewById(R.id.version_name);
		String versionName;
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			versioname.setText("v"+versionName);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (settings.hasSettings()) {
			TextView host_url = (TextView) findViewById(R.id.config_state);
			host_url.setText(settings.getUrl());
			Linkify.addLinks(host_url,Linkify.WEB_URLS);
			
		} else {
			Toast.makeText(getApplicationContext(), "Make Config", 1000);
		}
	}
	@Override
	public void onAttachedToWindow() {
	    super.onAttachedToWindow();
	    /* must be here, on onCreate app crashes*/
	    openOptionsMenu();
	}

}
