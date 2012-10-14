package com.mageventory.activity;

import java.io.IOException;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.job.JobQueue;
import com.mageventory.job.JobService;
import com.mageventory.job.JobQueue.JobsSummary;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.ErrorReportCreation;
import com.mageventory.tasks.LoadStatistics;
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
	
	private LoadStatistics mLoadStatisticsTask;
	private LinearLayout mStatisticsLoadingProgressLayout;
	private LinearLayout mStatisticsLayout;
	private LinearLayout mStatisticsLoadingFailedLayout;
	private boolean mForceRefreshStatistics = false;
	
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
	
		mStatisticsLoadingProgressLayout = (LinearLayout) findViewById(R.id.statisticsLoadingProgress); 
		mStatisticsLayout = (LinearLayout) findViewById(R.id.statisticsLayout);
		mStatisticsLoadingFailedLayout = (LinearLayout) findViewById(R.id.statisticsLoadingFailed);
		
		final TextView newJobStatsText = (TextView) findViewById(R.id.newJobStats);
		final TextView photoJobStatsText = (TextView) findViewById(R.id.photoJobStats);
		final TextView editJobStatsText = (TextView) findViewById(R.id.editJobStats);
		final TextView saleJobStatsText = (TextView) findViewById(R.id.saleJobStats);
		final TextView otherJobStatsText = (TextView) findViewById(R.id.otherJobStats);
		
		mJobSummaryListener = new JobQueue.JobSummaryChangedListener() {

			@Override
			public void OnJobSummaryChanged(final JobsSummary jobsSummary) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (isActivityAlive) {
							
							if (jobsSummary.pending.newProd + jobsSummary.failed.newProd > 0)
								newJobStatsText.setText("" + jobsSummary.pending.newProd + "/" + jobsSummary.failed.newProd);
							else
								newJobStatsText.setText("0");
							
							if (jobsSummary.pending.photo + jobsSummary.failed.photo > 0)
								photoJobStatsText.setText("" + jobsSummary.pending.photo + "/" + jobsSummary.failed.photo);
							else
								photoJobStatsText.setText("0");
								
							if (jobsSummary.pending.edit + jobsSummary.failed.edit > 0)
								editJobStatsText.setText("" + jobsSummary.pending.edit + "/" + jobsSummary.failed.edit);
							else
								editJobStatsText.setText("0");
							
							if (jobsSummary.pending.sell + jobsSummary.failed.sell > 0)
								saleJobStatsText.setText("" + jobsSummary.pending.sell + "/" + jobsSummary.failed.sell);
							else
								saleJobStatsText.setText("0");
							
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
		
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mForceRefreshStatistics = extras.getBoolean(getString(R.string.ekey_reload_statistics));
		}
	}
	
	public void statisticsLoadStart()
	{
		if (isActivityAlive)
		{
			mStatisticsLoadingProgressLayout.setVisibility(View.VISIBLE);
			mStatisticsLayout.setVisibility(View.GONE);
			mStatisticsLoadingFailedLayout.setVisibility(View.GONE);
		}
	}
	
	private String parseNumericalValue(Object number)
	{
		if (number instanceof Integer)
		{
			return "" + ((Integer)number).intValue();
		}
		else
		if (number instanceof Double)
		{
			if ( ((Double)number).doubleValue() == Math.round(((Double)number).doubleValue()) )
			{
				return "" + Math.round(((Double)number).doubleValue());	
			}
			else
			{
				return String.format( "%.2f", ((Double)number).doubleValue()   );	
			}
		}
		else
		{
			throw new RuntimeException("Unable to parse server response about statistics.");
		}
	}
	
	public void statisticsLoadSuccess()
	{
		if (isActivityAlive)
		{
			TextView salesToday = (TextView) findViewById(R.id.salesToday);
			TextView salesWeek = (TextView) findViewById(R.id.salesWeek);
			TextView salesMonth = (TextView) findViewById(R.id.salesMonth);
			TextView salesTotal = (TextView) findViewById(R.id.salesTotal);
			
			TextView stockQty = (TextView) findViewById(R.id.stockQty);
			TextView stockValue = (TextView) findViewById(R.id.stockValue);
			
			TextView dayLoaded = (TextView) findViewById(R.id.dayLoaded);
			TextView weekLoaded = (TextView) findViewById(R.id.weekLoaded);
			TextView monthLoaded = (TextView) findViewById(R.id.monthLoaded);

			Map<String, Object> statisticsData = mLoadStatisticsTask.mStatisticsData;
			
			salesToday.setText("$"+parseNumericalValue(statisticsData.get("day_sales")));
			salesWeek.setText("$"+parseNumericalValue(statisticsData.get("week_sales")));
			salesMonth.setText("$"+parseNumericalValue(statisticsData.get("month_sales")));
			salesTotal.setText("$"+parseNumericalValue(statisticsData.get("total_sales")));
			
			stockQty.setText(parseNumericalValue(statisticsData.get("total_stock_qty")));
			stockValue.setText("$"+parseNumericalValue(statisticsData.get("total_stock_value")));
			
			dayLoaded.setText(parseNumericalValue(statisticsData.get("day_loaded")));
			weekLoaded.setText(parseNumericalValue(statisticsData.get("week_loaded")));
			monthLoaded.setText(parseNumericalValue(statisticsData.get("month_loaded")));
			
			mStatisticsLoadingProgressLayout.setVisibility(View.GONE);
			mStatisticsLayout.setVisibility(View.VISIBLE);
			mStatisticsLoadingFailedLayout.setVisibility(View.GONE);
		}
	}
	
	public void statisticsLoadFailure()
	{
		if (isActivityAlive)
		{
			mStatisticsLoadingProgressLayout.setVisibility(View.GONE);
			mStatisticsLayout.setVisibility(View.GONE);
			mStatisticsLoadingFailedLayout.setVisibility(View.VISIBLE);
		}
	}

	public void showErrorReportingQuestion() {
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
		alert.setTitle("Report errors?");
		alert.setMessage("Report errors now? Complete and send the email when prompted.");
			
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
		
	@Override
	protected void onResume() {
		super.onResume();
		JobQueue.setOnJobSummaryChangedListener(mJobSummaryListener);
		JobService.registerOnJobServiceStateChangedListener(mJobServiceStateListener);
		Log.registerOnErrorReportingFileStateChangedListener(mErrorReportingFileStateChangedListener);
		
		if (mLoadStatisticsTask == null)
		{
			mLoadStatisticsTask = new LoadStatistics(this, mForceRefreshStatistics);
			mLoadStatisticsTask.execute();
			
			mForceRefreshStatistics = false;
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
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			
			Intent myIntent = new Intent(getApplicationContext(), getClass());
			myIntent.putExtra(getString(R.string.ekey_reload_statistics), true);
			finish();
			startActivity(myIntent);
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
