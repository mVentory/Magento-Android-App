package com.mageventory.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.text.Html;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.components.LinkTextView;
import com.mageventory.job.ExternalImagesJob;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.job.JobQueue;
import com.mageventory.job.JobService;
import com.mageventory.job.JobQueue.JobDetail;
import com.mageventory.job.JobQueue.JobsSummary;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.ErrorReportCreation;
import com.mageventory.tasks.ExecuteProfile;
import com.mageventory.tasks.LoadProfilesList;
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
	private Button profilesButton;
	private Button retryFailedButton;
	
	private boolean errorReportingLastLogOnly;
	
	private LayoutInflater mInflater;

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
	
	private LoadProfilesList mLoadProfilesTask;
	private ExecuteProfile mExecuteProfileTask;
	
	private ProgressDialog mProgressDialog;
	
	private JobControlInterface mJobControlInterface;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		
		mJobControlInterface = new JobControlInterface(this);
		
		isActivityAlive = true;
		
		JobService.wakeUp(this);

		app = (MyApplication) getApplication();
		settings = new Settings(getApplicationContext());

		String versionName;
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			versionName = versionName.substring(versionName.lastIndexOf("r"));

			this.setTitle("mVentory: Home " + versionName);
		} catch (NameNotFoundException e) {
			this.setTitle("mVentory: Home");
			Log.logCaughtException(e);
		}

		if (settings.hasSettings()) {
			final LinkTextView host_url = (LinkTextView) findViewById(R.id.config_state);
			
			host_url.setURL(settings.getUrl());

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
		
		Button imagesEditingButton = (Button) findViewById(R.id.imagesEditingButton);
		imagesEditingButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent newInt = new Intent(getApplicationContext(), ExternalImagesEditActivity.class);
				startActivityForResult(newInt, 0);
			}
		});
		
		retryFailedButton = (Button) findViewById(R.id.retryFailedButton);
		
		retryFailedButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				List<JobDetail> jobDetails = mJobControlInterface.getJobDetailList(false);

				for (JobDetail detail : jobDetails)
				{
					mJobControlInterface.retryJobDetail(detail);
				}
			}
		});
		
		errorReportingButton = (Button) findViewById(R.id.errorReportingButton);
		
		errorReportingButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showErrorReportingQuestion();
			}
		});
		
		profilesButton = (Button) findViewById(R.id.profilesButton);
		
		profilesButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				mLoadProfilesTask = new LoadProfilesList(MainActivity.this, false);
				mLoadProfilesTask.execute();
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
							
							if (jobsSummary.pending.newProd > 0 && jobsSummary.failed.newProd == 0)
								newJobStatsText.setText("" + jobsSummary.pending.newProd + "/" + jobsSummary.failed.newProd);
							else if (jobsSummary.failed.newProd > 0)
								newJobStatsText.setText(Html.fromHtml("" + jobsSummary.pending.newProd + "/<font color=\"#ff0000\">" + jobsSummary.failed.newProd + "</font>"));
							else
								newJobStatsText.setText("0");
							
							if (jobsSummary.pending.photo > 0 && jobsSummary.failed.photo == 0)
								photoJobStatsText.setText("" + jobsSummary.pending.photo + "/" + jobsSummary.failed.photo);
							else if (jobsSummary.failed.photo > 0)
								photoJobStatsText.setText(Html.fromHtml("" + jobsSummary.pending.photo + "/<font color=\"#ff0000\">" + jobsSummary.failed.photo + "</font>"));
							else
								photoJobStatsText.setText("0");
								
							if (jobsSummary.pending.edit > 0 && jobsSummary.failed.edit == 0)
								editJobStatsText.setText("" + jobsSummary.pending.edit + "/" + jobsSummary.failed.edit);
							else if (jobsSummary.failed.edit > 0)
								editJobStatsText.setText(Html.fromHtml("" + jobsSummary.pending.edit + "/<font color=\"#ff0000\">" + jobsSummary.failed.edit + "</font>"));
							else
								editJobStatsText.setText("0");
							
							if (jobsSummary.pending.sell > 0 && jobsSummary.failed.sell == 0)
								saleJobStatsText.setText("" + jobsSummary.pending.sell + "/" + jobsSummary.failed.sell);
							else if (jobsSummary.failed.sell > 0)
								saleJobStatsText.setText(Html.fromHtml("" + jobsSummary.pending.sell + "/<font color=\"#ff0000\">" + jobsSummary.failed.sell + "</font>"));
							else
								saleJobStatsText.setText("0");
							
							if (jobsSummary.pending.other > 0 && jobsSummary.failed.other == 0)
								otherJobStatsText.setText("" + jobsSummary.pending.other + "/" + jobsSummary.failed.other);
							else if (jobsSummary.failed.other > 0)
								otherJobStatsText.setText(Html.fromHtml("" + jobsSummary.pending.other + "/<font color=\"#ff0000\">" + jobsSummary.failed.other + "</font>"));
							else
								otherJobStatsText.setText("0");
							
							if (jobsSummary.failed.newProd>0 ||
								jobsSummary.failed.photo>0 || 
								jobsSummary.failed.edit>0 ||
								jobsSummary.failed.sell>0 ||
								jobsSummary.failed.other>0)
							{
								retryFailedButton.setEnabled(true);
								retryFailedButton.setTextColor(Color.RED);
							}
							else
							{
								retryFailedButton.setEnabled(false);
								retryFailedButton.setTextColor(Color.BLACK);
							}
						}
					}
				});
			}
		};
		
		final TextView service_status = (TextView) findViewById(R.id.service_status);
		
		mJobServiceStateListener = new JobService.OnJobServiceStateChangedListener() {
			
			@Override
			public void onJobServiceStateChanged(boolean running) {
	
				if (settings.getServiceCheckBox() == true)
				{
					if (running)
					{
						service_status.setText("RUNNING");
					}
					else
					{
						service_status.setText("STOPPED");
					}
				}
				else
				{
					service_status.setText(Html.fromHtml("<font color=\"#ff0000\">DISABLED</font>"));
				}
			}
		};
		
		mErrorReportingFileStateChangedListener = new OnErrorReportingFileStateChangedListener() {
			
			@Override
			public void onErrorReportingFileStateChanged(boolean fileExists) {
				if (fileExists)
				{
					MainActivity.this.runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							errorReportingLastLogOnly = false;
							errorReportingButton.setEnabled(true);
							errorReportingButton.setTextColor(Color.RED);
							errorReportingButton.setText("Report errors");
						}
					});
				}
				else
				{
					MainActivity.this.runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							errorReportingLastLogOnly = true;
							errorReportingButton.setEnabled(true);
							errorReportingButton.setTextColor(Color.BLACK);
							errorReportingButton.setText("Report status");
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
	
	public void dismissProgressDialog() {
		if (mProgressDialog == null) {
			return;
		}
		mProgressDialog.dismiss();
		mProgressDialog = null;
	}
	
	public void showProgressDialog(final String message) {
		if (isActivityAlive == false) {
			return;
		}
		if (mProgressDialog != null) {
			return;
		}
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setMessage(message);
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setCancelable(true);

		mProgressDialog.setButton(ProgressDialog.BUTTON1, "Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mProgressDialog.cancel();
			}
		});
		
		mProgressDialog.show();
	}
	
	public void profileExecutionStart()
	{
		showProgressDialog("Preparing the report...");
		
		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				mProgressDialog = null;	
				mExecuteProfileTask.cancel(false);
			}
		});
	}
	
	public void profileExecutionSuccess()
	{
		if (mProgressDialog == null || mProgressDialog.isShowing() == false)
		{
			dismissProgressDialog();
			return;
		}
		else
		{
			dismissProgressDialog();	
		}
		
		if (isActivityAlive)
		{
			showProfileExecutionSuccessDialog(mExecuteProfileTask.mProfileExecutionMessage);
		}
	}
	
	public void profileExecutionFailure()
	{
		if (mProgressDialog == null || mProgressDialog.isShowing() == false)
		{
			dismissProgressDialog();
			return;
		}
		else
		{
			dismissProgressDialog();
			
			if (isActivityAlive)
			{
				showProfileExecutionError();
			}
		}
	}
	
	public void profilesLoadStart()
	{
		showProgressDialog("Loading reports list...");
		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				mProgressDialog = null;	
				mLoadProfilesTask.cancel(false);
			}
		});
	}
	
	public void profilesLoadSuccess()
	{
		if (mProgressDialog == null || mProgressDialog.isShowing() == false)
		{
			dismissProgressDialog();
			return;
		}
		else
		{
			dismissProgressDialog();	
		}
		
		if (isActivityAlive == false) {
			return;
		}
		
		AlertDialog.Builder menuBuilder = new AlertDialog.Builder(MainActivity.this);
		
		ListView profilesList = (ListView)mInflater.inflate(R.layout.profiles_list, null);
		
		ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		
		for(Object profileObject : mLoadProfilesTask.mProfilesData)
		{
			list.add((Map<String, Object>)profileObject);
		}
		
		ListAdapter adapter = new SimpleAdapter(MainActivity.this, list, R.layout.profile_item, new String [] {"name"},
				new int [] { android.R.id.text1 });
		
		profilesList.setAdapter(adapter);
		
		menuBuilder.setView(profilesList);

		final AlertDialog menuDlg = menuBuilder.create();
		
		profilesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				menuDlg.dismiss();
				
				String profileID = (String)((Map<String, Object>)mLoadProfilesTask.mProfilesData[position]).get("profile_id");
				
				mExecuteProfileTask = new ExecuteProfile(MainActivity.this, profileID);
				mExecuteProfileTask.execute();
			}
		});
		
		menuDlg.show();
	}
	
	public void profilesLoadFailure()
	{
		if (mProgressDialog == null || mProgressDialog.isShowing() == false)
		{
			dismissProgressDialog();
			return;
		}
		else
		{
			dismissProgressDialog();
			
			if (isActivityAlive)
			{
				showProfilesListLoadError();
			}
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
	
	/* Add a comma every three characters starting from the right. */
	private String addCommaSeparatorToNumericalValue(String value)
	{
		StringBuilder out = new StringBuilder();
		int firstCommaPosition = value.length()%3;
		
		for(int i=0; i<value.length(); i++)
		{
			if (i>0 && (i-firstCommaPosition)%3==0)
			{
				out.append(',');
			}
			
			out.append(value.charAt(i));
		}

		return out.toString();
	}
	
	private String parseNumericalValue(Object number)
	{
		String out;
		
		if (number instanceof Integer)
		{
			out = "" + ((Integer)number).intValue();
		}
		else
		if (number instanceof Double)
		{
			out = "" + Math.round(((Double)number).doubleValue());	
		}
		else
		{
			throw new RuntimeException("Unable to parse server response about statistics.");
		}
		
		return addCommaSeparatorToNumericalValue(out);
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
			
			try
			{
				salesToday.setText("$"+parseNumericalValue(statisticsData.get("day_sales")));
				salesWeek.setText("$"+parseNumericalValue(statisticsData.get("week_sales")));
				salesMonth.setText("$"+parseNumericalValue(statisticsData.get("month_sales")));
				salesTotal.setText("$"+parseNumericalValue(statisticsData.get("total_sales")));
			
				stockQty.setText(parseNumericalValue(statisticsData.get("total_stock_qty")));
				stockValue.setText("$"+parseNumericalValue(statisticsData.get("total_stock_value")));
			
				dayLoaded.setText(parseNumericalValue(statisticsData.get("day_loaded")));
				weekLoaded.setText(parseNumericalValue(statisticsData.get("week_loaded")));
				monthLoaded.setText(parseNumericalValue(statisticsData.get("month_loaded")));
			}
			catch (RuntimeException e)
			{
				statisticsLoadFailure();
				return;
			}
			
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
				ErrorReportCreation errorReportCreationTask = new ErrorReportCreation(MainActivity.this, errorReportingLastLogOnly);
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
	
	public void showProfilesListLoadError() {
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
		alert.setTitle("Connection problem");
		alert.setMessage("Unable to load reports list. Check internet connection and try again.");
			
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
			
		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	public void showProfileExecutionError() {
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
		alert.setTitle("Connection problem");
		alert.setMessage("Unable to execute a profile. Check internet connection and try again.");
			
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
			
		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	public void showProfileExecutionSuccessDialog(String message) {
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
		alert.setTitle("Profile execution result");
		alert.setMessage(message);
			
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
		boolean dontShowOptionsMenu = false;
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			dontShowOptionsMenu = extras.getBoolean(getString(R.string.ekey_dont_show_menu), false);
		}
		if (dontShowOptionsMenu == false)
		{
			openOptionsMenu();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		isActivityAlive = false;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			
			JobCacheManager.removeProfilesList(settings.getUrl());
			
			Intent myIntent = new Intent(getApplicationContext(), getClass());
			myIntent.putExtra(getString(R.string.ekey_reload_statistics), true);
			myIntent.putExtra(getString(R.string.ekey_dont_show_menu), true);				
			finish();
			startActivity(myIntent);
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
