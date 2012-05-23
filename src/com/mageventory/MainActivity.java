package com.mageventory;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.job.Job;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.job.JobQueue;
import com.mageventory.job.JobQueueDBHelper;
import com.mageventory.job.JobService;
import com.mageventory.res.ResourceStateActivity;
import com.mageventory.settings.Settings;
import com.mageventory.util.DefaultOptionsMenuHelper;
import com.mageventory.util.Log;

public class MainActivity extends BaseActivity {
	protected MyApplication app;
	private Settings settings;
	public static final String PREFS_NAME = "pref.dat";
	ProgressDialog pDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
			
		/* TODO: Don't forget to delete this!!!! This is just for testing purposes!!! */
   		//this.deleteDatabase(JobQueueDBHelper.DB_NAME);
   		
   		JobService.wakeUp(this);

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
			Log.logCaughtException(e);
		}
		
		if (settings.hasSettings()) {
			TextView host_url = (TextView) findViewById(R.id.config_state);
			host_url.setText(settings.getUrl());
			Linkify.addLinks(host_url,Linkify.WEB_URLS);
			
		} else {
			Toast.makeText(getApplicationContext(), "Make Config", 1000);
		}
		
		Button settingsButton = (Button) findViewById(R.id.settingsButton);
		settingsButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent newInt = new Intent(getApplicationContext(),ConfigServerActivity.class);
				startActivityForResult(newInt,0);				
			}
		});
		
		
		Button quitButton = (Button) findViewById(R.id.quitButton);
		quitButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				moveTaskToBack(true);
			}
		});
	}
	@Override
	public void onAttachedToWindow() {
	    super.onAttachedToWindow();
	    /* must be here, on onCreate app crashes*/
	    openOptionsMenu();
	}
	
	public void onResourceStateButtonClick(View v) {
		Intent i = new Intent(this, ResourceStateActivity.class);
		startActivity(i);
	}
}
