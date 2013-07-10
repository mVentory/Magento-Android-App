
package com.mageventory.activity;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.bitmapfun.util.ImageCache;
import com.mageventory.bitmapfun.util.ImageCacheUtils;
import com.mageventory.bitmapfun.util.ImageFileSystemFetcher;
import com.mageventory.bitmapfun.util.ImageResizer;
import com.mageventory.bitmapfun.util.ImageWorker.ImageWorkerAdapter;
import com.mageventory.components.LinkTextView;
import com.mageventory.job.ExternalImagesJobQueue;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobQueue;
import com.mageventory.job.JobQueue.JobDetail;
import com.mageventory.job.JobQueue.JobsSummary;
import com.mageventory.job.JobService;
import com.mageventory.settings.Settings;
import com.mageventory.tasks.ErrorReportCreation;
import com.mageventory.tasks.ExecuteProfile;
import com.mageventory.tasks.LoadProfilesList;
import com.mageventory.tasks.LoadStatistics;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.ImagesLoader;
import com.mageventory.util.ImagesLoader.CachedImage;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.Log;
import com.mageventory.util.Log.OnErrorReportingFileStateChangedListener;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.widget.HorizontalListView;

public class MainActivity extends BaseFragmentActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    protected MyApplication app;
    private Settings settings;
    ProgressDialog pDialog;
    private boolean isActivityAlive;
    private Button errorReportingButton;
    private Button profilesButton;
    private Button retryFailedButton;
    private Button imagesEditingButton;

    private boolean errorReportingLastLogOnly;

    private LayoutInflater mInflater;

    private JobQueue.JobSummaryChangedListener mJobSummaryListener;
    private ExternalImagesJobQueue.ExternalImagesCountChangedListener mExternalImagesListener;

    private JobService.OnJobServiceStateChangedListener mJobServiceStateListener;
    private Log.OnErrorReportingFileStateChangedListener mErrorReportingFileStateChangedListener;

	public View mMainContent;
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

    private int mJobSummaryPendingPhotoCount;
    private int mJobSummaryFailedPhotoCount;
    private int mExternalImagesPendingPhotoCount;

    private TextView mPhotoJobStatsText;

    private LoadThumbsTask loadThumbsTask;
    ImagesObserver newImageObserver;
    BroadcastReceiver diskCacheClearedReceiver;

    ImageResizer mImageWorker;
    private HorizontalListView thumbnailsList;

    private ThumbnailsAdapter thumbnailsAdapter;

    int orientation;

    private void updatePhotoSummary()
    {
        int pending = mJobSummaryPendingPhotoCount + mExternalImagesPendingPhotoCount;
        int failed = mJobSummaryFailedPhotoCount;

        if (pending > 0 && failed == 0)
            mPhotoJobStatsText.setText("" + pending + "/" + failed);
        else if (failed > 0)
            mPhotoJobStatsText.setText(Html.fromHtml("" + pending + "/<font color=\"#ff0000\">"
                    + failed + "</font>"));
        else
            mPhotoJobStatsText.setText("0");
    }

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
        settings.registerGalleryPhotosDirectoryChangedListener(new Runnable() {

            @Override
            public void run() {
                reloadThumbs();
                restartObservation();
            }
        });
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

        imagesEditingButton = (Button) findViewById(R.id.imagesEditingButton);
        imagesEditingButton.setEnabled(false);
        imagesEditingButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent newInt = new Intent(getApplicationContext(),
                        ExternalImagesEditActivity.class);
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

		mMainContent = findViewById(R.id.mainContent);
        mErrorReportingProgress = (LinearLayout) findViewById(R.id.errorReportingProgress);

        mStatisticsLoadingProgressLayout = (LinearLayout) findViewById(R.id.statisticsLoadingProgress);
        mStatisticsLayout = (LinearLayout) findViewById(R.id.statisticsLayout);
        mStatisticsLoadingFailedLayout = (LinearLayout) findViewById(R.id.statisticsLoadingFailed);

        final TextView newJobStatsText = (TextView) findViewById(R.id.newJobStats);
        mPhotoJobStatsText = (TextView) findViewById(R.id.photoJobStats);
        final TextView editJobStatsText = (TextView) findViewById(R.id.editJobStats);
        final TextView saleJobStatsText = (TextView) findViewById(R.id.saleJobStats);
        final TextView otherJobStatsText = (TextView) findViewById(R.id.otherJobStats);

        mExternalImagesListener = new ExternalImagesJobQueue.ExternalImagesCountChangedListener() {

            @Override
            public void onExternalImagesCountChanged(final int newCount) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isActivityAlive) {
                            mExternalImagesPendingPhotoCount = newCount;
                            updatePhotoSummary();
                        }
                    }
                });
            }
        };

        mJobSummaryListener = new JobQueue.JobSummaryChangedListener() {

            @Override
            public void OnJobSummaryChanged(final JobsSummary jobsSummary) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (isActivityAlive) {

                            mJobSummaryPendingPhotoCount = jobsSummary.pending.photo;
                            mJobSummaryFailedPhotoCount = jobsSummary.failed.photo;
                            updatePhotoSummary();

                            if (jobsSummary.pending.newProd > 0 && jobsSummary.failed.newProd == 0)
                                newJobStatsText.setText("" + jobsSummary.pending.newProd + "/"
                                        + jobsSummary.failed.newProd);
                            else if (jobsSummary.failed.newProd > 0)
                                newJobStatsText.setText(Html.fromHtml(""
                                        + jobsSummary.pending.newProd + "/<font color=\"#ff0000\">"
                                        + jobsSummary.failed.newProd + "</font>"));
                            else
                                newJobStatsText.setText("0");

                            if (jobsSummary.pending.edit > 0 && jobsSummary.failed.edit == 0)
                                editJobStatsText.setText("" + jobsSummary.pending.edit + "/"
                                        + jobsSummary.failed.edit);
                            else if (jobsSummary.failed.edit > 0)
                                editJobStatsText.setText(Html.fromHtml(""
                                        + jobsSummary.pending.edit + "/<font color=\"#ff0000\">"
                                        + jobsSummary.failed.edit + "</font>"));
                            else
                                editJobStatsText.setText("0");

                            if (jobsSummary.pending.sell > 0 && jobsSummary.failed.sell == 0)
                                saleJobStatsText.setText("" + jobsSummary.pending.sell + "/"
                                        + jobsSummary.failed.sell);
                            else if (jobsSummary.failed.sell > 0)
                                saleJobStatsText.setText(Html.fromHtml(""
                                        + jobsSummary.pending.sell + "/<font color=\"#ff0000\">"
                                        + jobsSummary.failed.sell + "</font>"));
                            else
                                saleJobStatsText.setText("0");

                            if (jobsSummary.pending.other > 0 && jobsSummary.failed.other == 0)
                                otherJobStatsText.setText("" + jobsSummary.pending.other + "/"
                                        + jobsSummary.failed.other);
                            else if (jobsSummary.failed.other > 0)
                                otherJobStatsText.setText(Html.fromHtml(""
                                        + jobsSummary.pending.other + "/<font color=\"#ff0000\">"
                                        + jobsSummary.failed.other + "</font>"));
                            else
                                otherJobStatsText.setText("0");

                            if (jobsSummary.failed.newProd > 0 ||
                                    jobsSummary.failed.photo > 0 ||
                                    jobsSummary.failed.edit > 0 ||
                                    jobsSummary.failed.sell > 0 ||
                                    jobsSummary.failed.other > 0)
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
                    service_status
                            .setText(Html.fromHtml("<font color=\"#ff0000\">DISABLED</font>"));
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

        initThumbs();
        diskCacheClearedReceiver = ImageCacheUtils
                .getAndRegisterOnDiskCacheClearedBroadcastReceiver(TAG, this);
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

        mProgressDialog.setButton(ProgressDialog.BUTTON1, "Cancel",
                new DialogInterface.OnClickListener() {

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

        ListView profilesList = (ListView) mInflater.inflate(R.layout.profiles_list, null);

        ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        for (Object profileObject : mLoadProfilesTask.mProfilesData)
        {
            list.add((Map<String, Object>) profileObject);
        }

        ListAdapter adapter = new SimpleAdapter(MainActivity.this, list, R.layout.profile_item,
                new String[] {
                        "name"
                },
                new int[] {
                        android.R.id.text1
                });

        profilesList.setAdapter(adapter);

        menuBuilder.setView(profilesList);

        final AlertDialog menuDlg = menuBuilder.create();

        profilesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                menuDlg.dismiss();

                String profileID = (String) ((Map<String, Object>) mLoadProfilesTask.mProfilesData[position])
                        .get("profile_id");

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
        int firstCommaPosition = value.length() % 3;

        for (int i = 0; i < value.length(); i++)
        {
            if (i > 0 && (i - firstCommaPosition) % 3 == 0)
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
            out = "" + ((Integer) number).intValue();
        }
        else if (number instanceof Double)
        {
            out = "" + Math.round(((Double) number).doubleValue());
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
                salesToday.setText("$" + parseNumericalValue(statisticsData.get("day_sales")));
                salesWeek.setText("$" + parseNumericalValue(statisticsData.get("week_sales")));
                salesMonth.setText("$" + parseNumericalValue(statisticsData.get("month_sales")));
                salesTotal.setText("$" + parseNumericalValue(statisticsData.get("total_sales")));

                stockQty.setText(parseNumericalValue(statisticsData.get("total_stock_qty")));
                stockValue.setText("$"
                        + parseNumericalValue(statisticsData.get("total_stock_value")));

                dayLoaded.setText(parseNumericalValue(statisticsData.get("day_loaded")));
                weekLoaded.setText(parseNumericalValue(statisticsData.get("week_loaded")));
                monthLoaded.setText(parseNumericalValue(statisticsData.get("month_loaded")));
            } catch (RuntimeException e)
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
                ErrorReportCreation errorReportCreationTask = new ErrorReportCreation(
                        MainActivity.this, errorReportingLastLogOnly);
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
        ExternalImagesJobQueue.setExternalImagesCountChangedListener(mExternalImagesListener);
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
        ExternalImagesJobQueue.setExternalImagesCountChangedListener(null);
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
        settings.unregisterListOfStoresPreferenceChangeListeners();
        stopObservation();
        unregisterReceiver(diskCacheClearedReceiver);
        if (loadThumbsTask != null) {
            loadThumbsTask.cancel(true);
            loadThumbsTask = null;
        }
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

    /**
     * Restart observation of images directory
     */
    private void restartObservation()
    {
        try
        {
            stopObservation();
            String imagesDirPath = settings.getGalleryPhotosDirectory();
            if (TextUtils.isEmpty(imagesDirPath))
            {
                return;
            }
            newImageObserver = new ImagesObserver(imagesDirPath);
            newImageObserver.startWatching();
        } catch (Exception ex)
        {
            GuiUtils.error(TAG, ex);
        }
    }

    /**
     * Stop observation of images directory
     */
    private void stopObservation()
    {
        if (newImageObserver != null)
        {
            newImageObserver.stopWatching();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // This is really dirty hack. HorizontalListView doesn't support
        // configuration changes such as orientation so we need to reinitialize
        // it. In future would be good to rework current activity to store its
        // state in bundle instead of using custom orientation change handling
        if (orientation != newConfig.orientation)
        {
            GuiUtils.postDelayed(new Runnable() {

                @Override
                public void run() {
                    resetThumbsList();
                }
            }, 1000);
        }
    }

    void initThumbs()
    {
        thumbnailsList = (HorizontalListView) findViewById(R.id.thumbs);
        initImageWorker();
        reloadThumbs();
        restartObservation();
    }

    /**
     * Reload thumbs on the main page
     */
    private void reloadThumbs() {
        if (!isActivityAlive)
        {
            return;
        }
        if (loadThumbsTask != null) {
            loadThumbsTask.cancel(true);
        }
        loadThumbsTask = new LoadThumbsTask();
        loadThumbsTask.execute();
    }

    private void resetThumbsList() {
        CommonUtils.debug(TAG, "recreateThumbsList");
        orientation = getResources().getConfiguration().orientation;
        if (thumbnailsAdapter != null)
        {
            thumbnailsList.setAdapter(thumbnailsAdapter);
            thumbnailsList.scrollTo(Integer.MAX_VALUE);
        }
    }

    protected void initImageWorker()
    {
        mImageWorker = new CustomImageFileSystemFetcher(this,
                null,
                getResources().getDimensionPixelSize(
                        R.dimen.home_thumbnail_size));
        mImageWorker.setLoadingImage(R.drawable.empty_photo);

        mImageWorker.setImageCache(ImageCache.findOrCreateCache(this,
                ImageCache.LOCAL_THUMBS_CACHE_DIR, 1500, true, false));
    }

    class LoadThumbsTask extends SimpleAsyncTask
    {
        ThumbsImageWorkerAdapter adapter;

        public LoadThumbsTask() {
            super(null);
        }

        @Override
        protected void onFailedPostExecute() {
            super.onFailedPostExecute();
            loadThumbsTask = null;
            if (isActivityAlive)
            {
            }
        }

        @Override
        protected void onSuccessPostExecute() {
            try
            {
                imagesEditingButton.setEnabled(adapter.getSize() > 0);
                final boolean scrollToEnd = thumbnailsAdapter == null
                        || thumbnailsList.getRightViewIndex() == thumbnailsAdapter.getCount();
                mImageWorker.setAdapter(adapter);
                if (thumbnailsAdapter == null)
                {
                    thumbnailsAdapter = new ThumbnailsAdapter(MainActivity.this, mImageWorker);
                    thumbnailsList.setAdapter(thumbnailsAdapter);
                } else
                {
                    thumbnailsAdapter.notifyDataSetChanged();
                }
                if (scrollToEnd)
                {
                    GuiUtils.post(new Runnable() {

                        @Override
                        public void run() {
                            thumbnailsList.scrollTo(Integer.MAX_VALUE);
                        }
                    });
                }
            } finally
            {
                loadThumbsTask = null;
            }
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled())
                {
                    return false;
                }
                adapter = new ThumbsImageWorkerAdapter();
                return !isCancelled();
            } catch (Exception e) {
                GuiUtils.error(TAG, e);
            }
            return false;
        }

    }

    public class ImagesObserver extends FileObserver {

        private final String mPath;

        public ImagesObserver(String path) {
            super(path,
                    FileObserver.DELETE |
                            FileObserver.DELETE_SELF |
                            FileObserver.MOVE_SELF |
                            FileObserver.MOVED_FROM |
                            FileObserver.MOVED_TO |
                            FileObserver.CLOSE_WRITE);
            mPath = path;
        }

        @Override
        public void onEvent(int event, String fileName) {
            try
            {
                CommonUtils.debug(TAG, "ImageObserver: event %1$d fileName %2$s", event, fileName);
                if (fileName != null && !fileName.equals(".probe")) {
                    File file = new File(mPath + "/" + fileName);
                    CommonUtils.debug(TAG, "File modified [" + file.getAbsolutePath() + "]");
                    // fix for the issue #309
                    String type = getMimeType(file);
                    if (type != null && type.toLowerCase().startsWith("image/"))
                    {
                        reloadThumbs();
                    } else if (ImagesLoader.isSpecialRenamedFile(file))
                    {
                        reloadThumbs();
                    }
                }
            } catch (Exception ex)
            {
                GuiUtils.error(TAG, ex);
            }
        }

        /**
         * Get the mime type for the file
         * 
         * @param file
         * @return
         */
        public String getMimeType(File file)
        {
            String type = null;
            String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).getPath());
            if (extension != null) {
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                type = mime.getMimeTypeFromExtension(extension.toLowerCase());
            }
            CommonUtils.debug(TAG, "File: %1$s; extension %2$s; MimeType: %3$s",
                    file.getAbsolutePath(), extension, type);
            return type;
        }
    }

    public static class ImageData
    {
        File file;
        int width;
        int height;

        public ImageData(File file, int width, int height) {
            super();
            this.file = file;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return file.getName();
        }
    }

    public class ThumbsImageWorkerAdapter extends
            ImageWorkerAdapter
    {
        public List<ImageData> data;

        ThumbsImageWorkerAdapter() throws IOException
        {
            String imagesDirPath = settings.getGalleryPhotosDirectory();
            data = new ArrayList<ImageData>();
            if (TextUtils.isEmpty(imagesDirPath))
            {
                return;
            }

            File f = new File(imagesDirPath);

            File[] files = f.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    return (pathname.getName().toLowerCase().contains(".jpg") && !pathname
                            .isDirectory());
                }
            });
            if (files == null || files.length == 0)
            {
                return;
            }

            Arrays.sort(files, ExternalImagesEditActivity.filesComparator);
            for (File file : files)
            {
                Rect cropRect = ImagesLoader.getBitmapRect(file);
                int width, height;
                if (cropRect == null)
                {
                    BitmapFactory.Options options = ImageUtils.calculateImageSize(file
                            .getAbsolutePath());
                    width = options.outWidth;
                    height = options.outHeight;
                } else
                {
                    width = cropRect.width();
                    height = cropRect.height();
                }
                int orientation = ImageUtils.getOrientationInDegreesForFileName(file
                        .getAbsolutePath());
                if (orientation == 90 || orientation == 270)
                {
                    int tmp = width;
                    width = height;
                    height = tmp;
                }
                data.add(new ImageData(file, width, height));
            }
        }

        @Override
        public Object getItem(int num) {
            return data.get(num);
        }

        @Override
        public int getSize() {
            return data.size();
        }
    }

    private class CustomImageFileSystemFetcher extends ImageFileSystemFetcher
    {
        public CustomImageFileSystemFetcher(Context context,
                LoadingControl loadingControl, int imageSize)
        {
            super(context, loadingControl, imageSize);
        }

        public CustomImageFileSystemFetcher(Context context,
                LoadingControl loadingControl, int imageWidth,
                int imageHeight)
        {
            super(context, loadingControl, imageWidth, imageHeight);
        }

        @Override
        protected Bitmap processBitmap(Object data)
        {
            ImageData imageData = (ImageData) data;
            CachedImage cachedImage = new CachedImage(imageData.file);
            ImagesLoader.loadBitmap(cachedImage, mImageWorker.getImageHeight());
            return cachedImage.mBitmap;
        }
    }

    private static class ThumbnailsAdapter extends BaseAdapter {

        protected final Context mContext;
        protected int mItemBorder;
        private ImageResizer mImageWorker;
        LayoutInflater inflater;

        public ThumbnailsAdapter(Context context, ImageResizer imageWorker)
        {
            super();
            mContext = context;
            this.mImageWorker = imageWorker;
            this.inflater = LayoutInflater.from(context);
            mItemBorder = context.getResources().getDimensionPixelSize(
                    R.dimen.home_thumbnail_border);
        }

        @Override
        public int getCount()
        {
            return mImageWorker.getAdapter().getSize();
        }

        @Override
        public Object getItem(int position)
        {
            return mImageWorker.getAdapter().getItem(position);
        }

        @Override
        public long getItemId(int position)
        {
            return ((ImageData) getItem(position)).file.getAbsolutePath().hashCode();
        }

        @Override
        public boolean hasStableIds()
        {
            return super.hasStableIds();
        }

        @Override
        public final View getView(int position, View convertView,
                ViewGroup container)
        {
            ViewHolder holder;
            if (convertView == null)
            { // if it's not recycled, instantiate and initialize
                convertView = inflater
                        .inflate(R.layout.main_item_thumb_image, null);
                holder = new ViewHolder();
                holder.containerRoot = convertView.findViewById(R.id.container_root);
                holder.selectedOverlay = convertView
                        .findViewById(R.id.selection_overlay);
                holder.imageView = (ImageView) convertView.findViewById(R.id.image);
                convertView.setTag(holder);
            }
            else
            { // Otherwise re-use the converted view
                holder = (ViewHolder) convertView.getTag();
            }
            int width = mImageWorker.getImageWidth();
            int height = mImageWorker.getImageHeight();
            ImageData data = (ImageData) getItem(position);
            if (data.width != 0 && data.height != 0)
            {
                float ratio = (float) data.width / data.height;
                width = (int) (ratio * height);
            }
            width += 2 * mItemBorder;
            height += 2 * mItemBorder;
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) holder.containerRoot
                    .getLayoutParams();
            if (layoutParams.width != width
                    || layoutParams.height != height)
            {
                layoutParams = new FrameLayout.LayoutParams(width,
                        height);
                holder.containerRoot.setLayoutParams(layoutParams);
            }
            CommonUtils.debug(TAG, "getView: %1$d height: %2$d %3$d width: %4$d %5$d",
                    position, height, holder.containerRoot.getLayoutParams().height,
                    width, holder.containerRoot.getLayoutParams().width);
            holder.selectedOverlay.setVisibility(ImagesLoader.hasSKUInFileName(data.file.getName())
                    ?
                    View.VISIBLE : View.INVISIBLE);
            mImageWorker.loadImage(position, holder.imageView);
            return convertView;
        }

        protected class ViewHolder
        {
            View selectedOverlay;
            View containerRoot;
            ImageView imageView;
        }
    }
}
