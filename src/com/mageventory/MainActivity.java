package com.mageventory;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobQueue;
import com.mageventory.job.JobQueueDBHelper;
import com.mageventory.job.JobService;
import com.mageventory.job.JobQueue.JobsSummary;
import com.mageventory.settings.Settings;
import com.mageventory.util.Log;

public class MainActivity extends BaseActivity {
	protected MyApplication app;
	private Settings settings;
	ProgressDialog pDialog;
	private boolean isActivityAlive;

	private JobQueue.JobSummaryChangedListener jobSummaryListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		isActivityAlive = true;

		//JobCacheManager.saveRangeStart("skuHash4", "urlHash4");
		//JobCacheManager.saveRangeEnd();
		
		//Log.d("haha", "" + JobCacheManager.getSkuUrlHashForTimeStamp(799999));
		
		JobService.wakeUp(this);

		app = (MyApplication) getApplication();
		settings = new Settings(getApplicationContext());

		String versionName;
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			versionName = versionName.substring(versionName.lastIndexOf("r"));

			this.setTitle("Mventory: Home " + versionName);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			this.setTitle("Mventory: Home");
			Log.logCaughtException(e);
		}

		if (settings.hasSettings()) {
			TextView host_url = (TextView) findViewById(R.id.config_state);
			host_url.setText(settings.getUrl());
			Linkify.addLinks(host_url, Linkify.WEB_URLS);

		} else {
			Toast.makeText(getApplicationContext(), "Make Config", 1000);
		}

		Button settingsButton = (Button) findViewById(R.id.settingsButton);
		settingsButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent newInt = new Intent(getApplicationContext(), ConfigServerActivity.class);
				startActivityForResult(newInt, 0);
			}
		});

		Button queueButton = (Button) findViewById(R.id.queueButton);
		queueButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, QueueActivity.class);
				MainActivity.this.startActivity(intent);
			}
		});

		final TextView new_prod_pending = (TextView) findViewById(R.id.new_prod_pending);
		final TextView photos_pending = (TextView) findViewById(R.id.photos_pending);
		final TextView sell_pending = (TextView) findViewById(R.id.sell_pending);
		final TextView edit_pending = (TextView) findViewById(R.id.edit_pending);

		final TextView new_prod_failed = (TextView) findViewById(R.id.new_prod_failed);
		final TextView photos_failed = (TextView) findViewById(R.id.photos_failed);
		final TextView sell_failed = (TextView) findViewById(R.id.sell_failed);
		final TextView edit_failed = (TextView) findViewById(R.id.edit_failed);

		jobSummaryListener = new JobQueue.JobSummaryChangedListener() {

			@Override
			public void OnJobSummaryChanged(final JobsSummary jobsSummary) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (isActivityAlive) {
							new_prod_pending.setText("" + jobsSummary.pending.newProd);
							photos_pending.setText("" + jobsSummary.pending.photo);
							sell_pending.setText("" + jobsSummary.pending.sell);
							edit_pending.setText("" + jobsSummary.pending.edit);

							new_prod_failed.setText("" + jobsSummary.failed.newProd);
							photos_failed.setText("" + jobsSummary.failed.photo);
							sell_failed.setText("" + jobsSummary.failed.sell);
							edit_failed.setText("" + jobsSummary.failed.edit);
						}
					}
				});
			}
		};
		
		boolean externalPhotosCheckboxChecked = settings.getExternalPhotosCheckBox();
		((CheckBox) findViewById(R.id.external_photos_checkbox)).setChecked(externalPhotosCheckboxChecked);
		
		((CheckBox) findViewById(R.id.external_photos_checkbox)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				settings.setExternalPhotosCheckBox(isChecked);
				
				/* Check for new images in the external camera directory only if the checkbox got checked. */
				if (isChecked)
				{
					((MyApplication)MainActivity.this.getApplication()).uploadAllImages(settings.getGalleryPhotosDirectory());
				}
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		JobQueue.setOnJobSummaryChangedListener(jobSummaryListener);
	}

	@Override
	protected void onPause() {
		super.onPause();
		JobQueue.setOnJobSummaryChangedListener(null);
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		/* must be here, on onCreate app crashes */
		openOptionsMenu();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		isActivityAlive = false;
	}
}
