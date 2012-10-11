package com.mageventory.activity;

import java.io.IOException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.job.JobQueue;
import com.mageventory.job.JobService;
import com.mageventory.job.JobQueue.JobsSummary;
import com.mageventory.settings.Settings;
import com.mageventory.tasks.ErrorReportCreation;
import com.mageventory.util.ErrorReporterUtils;
import com.mageventory.util.Log;
import com.mageventory.util.Log.OnErrorReportingFileStateChangedListener;

public class MainActivity extends BaseActivity {
	protected MyApplication app;
	private Settings settings;
	ProgressDialog pDialog;
	private boolean isActivityAlive;
	private Button errorReportingButton;

	private JobQueue.JobSummaryChangedListener mJobSummaryListener;
	private JobService.OnJobServiceStateChangedListener mJobServiceStateListener;
	private Log.OnErrorReportingFileStateChangedListener mErrorReportingFileStateChangedListener;
	
	public LinearLayout mMainContent;
	public LinearLayout mErrorReportingProgress;
	public boolean mShowDeleteErrorReportsDialogInOnResume = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		isActivityAlive = true;
		
		JobService.wakeUp(this);

		app = (MyApplication) getApplication();
		settings = new Settings(getApplicationContext());

		String versionName;
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			versionName = versionName.substring(versionName.lastIndexOf("r"));

			this.setTitle("Mventory: Home " + versionName);
		} catch (NameNotFoundException e) {
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
		
		errorReportingButton = (Button) findViewById(R.id.errorReportingButton);
		
		errorReportingButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showErrorReportingQuestion();
			}
		});
		
		mMainContent = (LinearLayout) findViewById(R.id.mainContent);
		mErrorReportingProgress = (LinearLayout) findViewById(R.id.errorReportingProgress);

		final TextView new_prod_pending = (TextView) findViewById(R.id.new_prod_pending);
		final TextView photos_pending = (TextView) findViewById(R.id.photos_pending);
		final TextView sell_pending = (TextView) findViewById(R.id.sell_pending);
		final TextView edit_pending = (TextView) findViewById(R.id.edit_pending);

		final TextView new_prod_failed = (TextView) findViewById(R.id.new_prod_failed);
		final TextView photos_failed = (TextView) findViewById(R.id.photos_failed);
		final TextView sell_failed = (TextView) findViewById(R.id.sell_failed);
		final TextView edit_failed = (TextView) findViewById(R.id.edit_failed);

		mJobSummaryListener = new JobQueue.JobSummaryChangedListener() {

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
		
		final TextView service_status = (TextView) findViewById(R.id.service_status);
		
		mJobServiceStateListener = new JobService.OnJobServiceStateChangedListener() {
			
			@Override
			public void onJobServiceStateChanged(boolean running) {
				if (running)
				{
					service_status.setText("RUNNING");
				}
				else
				{
					service_status.setText("STOPPED");
				}
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
		
		
		boolean serviceCheckboxChecked = settings.getServiceCheckBox();
		((CheckBox) findViewById(R.id.service_checkbox)).setChecked(serviceCheckboxChecked);
		
		((CheckBox) findViewById(R.id.service_checkbox)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				settings.setServiceCheckBox(isChecked);
				
				if (isChecked)
				{
					JobService.wakeUp(MainActivity.this);
				}
			}
		});
		
		mErrorReportingFileStateChangedListener = new OnErrorReportingFileStateChangedListener() {
			
			@Override
			public void onErrorReportingFileStateChanged(boolean fileExists) {
				if (fileExists)
				{
					MainActivity.this.runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							errorReportingButton.setEnabled(true);
							errorReportingButton.setTextColor(Color.RED);
						}
					});
				}
				else
				{
					MainActivity.this.runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							errorReportingButton.setEnabled(false);
							errorReportingButton.setTextColor(Color.BLACK);
						}
					});
				}
			}
		};
	}

	public void showErrorReportingQuestion() {
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
		alert.setTitle("Report errors?");
		alert.setMessage("Are you sure you want to report errors now? It make take up to one minute.");
			
		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ErrorReportCreation errorReportCreationTask = new ErrorReportCreation(MainActivity.this);
				errorReportCreationTask.execute();
			}
		});
		
		alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
			
		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	public void showErrorReportRemovalQuestion() {
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
		alert.setTitle("Delete error reports?");
		alert.setMessage("Do you want to delete all error reports now?");
			
		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ErrorReporterUtils.removeErrorReports();
				
				errorReportingButton.setEnabled(false);
				errorReportingButton.setTextColor(Color.BLACK);
			}
		});
		
		alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
			
		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		JobQueue.setOnJobSummaryChangedListener(mJobSummaryListener);
		JobService.registerOnJobServiceStateChangedListener(mJobServiceStateListener);
		Log.registerOnErrorReportingFileStateChangedListener(mErrorReportingFileStateChangedListener);
		
		if (mShowDeleteErrorReportsDialogInOnResume)
		{
			mShowDeleteErrorReportsDialogInOnResume = false;
			showErrorReportRemovalQuestion();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		JobQueue.setOnJobSummaryChangedListener(null);
		JobService.deregisterOnJobServiceStateChangedListener();
		Log.deregisterOnErrorReportingFileStateChangedListener();
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
