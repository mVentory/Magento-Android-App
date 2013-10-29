
package com.mageventory.activity;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import android.app.Activity;
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
import android.os.Bundle;
import android.os.FileObserver;
import android.os.SystemClock;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.activity.MainActivity.ThumbnailsAdapter.ItemViewHolder;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.bitmapfun.util.DiskLruCache;
import com.mageventory.bitmapfun.util.ImageCache;
import com.mageventory.bitmapfun.util.ImageCacheUtils;
import com.mageventory.bitmapfun.util.ImageFileSystemFetcher;
import com.mageventory.bitmapfun.util.ImageResizer;
import com.mageventory.bitmapfun.util.ImageWorker;
import com.mageventory.bitmapfun.util.ImageWorker.ImageWorkerAdapter;
import com.mageventory.components.LinkTextView;
import com.mageventory.job.ExternalImagesJob;
import com.mageventory.job.ExternalImagesJobQueue;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCacheManager.GalleryTimestampRange;
import com.mageventory.job.JobCacheManager.ProductDetailsExistResult;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobQueue;
import com.mageventory.job.JobQueue.JobDetail;
import com.mageventory.job.JobQueue.JobsSummary;
import com.mageventory.job.JobService;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.resprocessor.ProductDetailsProcessor.ProductDetailsLoadException;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.ErrorReportCreation;
import com.mageventory.tasks.ExecuteProfile;
import com.mageventory.tasks.LoadProfilesList;
import com.mageventory.tasks.LoadStatistics;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.FileUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.ImagesLoader;
import com.mageventory.util.ImagesLoader.CachedImage;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.Log;
import com.mageventory.util.Log.OnErrorReportingFileStateChangedListener;
import com.mageventory.util.ScanUtils;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.SimpleViewLoadingControl;
import com.mageventory.util.SingleFrequencySoundGenerator;
import com.mageventory.util.ZXingCodeScanner;
import com.mageventory.widget.HorizontalListView;
import com.mageventory.widget.HorizontalListView.OnDownListener;

public class MainActivity extends BaseFragmentActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int SCAN_QR_CODE = 1;

    protected MyApplication app;
    private Settings settings;
    ProgressDialog pDialog;
    private boolean isActivityAlive;
    private Button errorReportingButton;
    private Button profilesButton;
    private Button retryFailedButton;
    private Button mUploadButton;

    private boolean errorReportingLastLogOnly;

    private LayoutInflater mInflater;

    private JobQueue.JobSummaryChangedListener mJobSummaryListener;
    private ExternalImagesJobQueue.ExternalImagesCountChangedListener mExternalImagesListener;

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
    private View mThumbsLoadIndicator;
    private LoadingControl mThumbsLoadingControl;

    private ThumbnailsAdapter thumbnailsAdapter;

    int orientation;
    
    private LoadingControl mDecodeStatusLoadingControl;
    private LoadingControl mMatchingByTimeStatusLoadingControl;
    private LoadingControl mScanResultProcessingLoadingControl;
    private LoadingControl mIgnoringLoadingControl;
    private LoadingControl mDeletingLoadingControl;
    private LoadingControl mPrepareUploadingLoadingControl;
    DecodeImageTask mDecodeImageTask;
    ProcessScanResultsTask mProcessScanResultTask;
    MatchingByTimeTask mMatchingByTimeTask;
    SyncTimeTask mSyncimeTask;
    IgnoringTask mIgnoringTask;
    DeleteTask mDeleteTask;
    UploadTask mUploadTask;
    private SingleFrequencySoundGenerator mCurrentBeep;

    private ThumbnailsAdapter.CurrentDataInfo mLastCurrentData;

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

        mUploadButton = (Button) findViewById(R.id.uploadButton);
        mUploadButton.setEnabled(false);
        mUploadButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                uploadButtonClicked();
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

        mMainContent = findViewById(R.id.scroll);
        mErrorReportingProgress = (LinearLayout) findViewById(R.id.errorReportingProgress);

        mStatisticsLoadingProgressLayout = (LinearLayout) findViewById(R.id.statisticsLoadingProgress);
        mStatisticsLayout = (LinearLayout) findViewById(R.id.statisticsLayout);
        mStatisticsLoadingFailedLayout = (LinearLayout) findViewById(R.id.statisticsLoadingFailed);
        mThumbsLoadIndicator = findViewById(R.id.imagesLoadingStatusLine);
        mThumbsLoadingControl = new SimpleViewLoadingControl(mThumbsLoadIndicator);

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
        mDecodeStatusLoadingControl = new SimpleViewLoadingControl(
                findViewById(R.id.decodeStatusLine));
        mMatchingByTimeStatusLoadingControl = new SimpleViewLoadingControl(
                findViewById(R.id.matchingStatusLine));
        mScanResultProcessingLoadingControl = new SimpleViewLoadingControl(
                findViewById(R.id.processingScanResultStatusLine));
        mIgnoringLoadingControl = new SimpleViewLoadingControl(findViewById(R.id.ignoreStatusLine));
        mDeletingLoadingControl = new SimpleViewLoadingControl(findViewById(R.id.deleteStatusLine));
        mPrepareUploadingLoadingControl = new SimpleViewLoadingControl(
                findViewById(R.id.prepareUploadingStatusLine));
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
            // need to stop observation here, because onDestroy is called after
            // the recreated activity onCreate() and stopObservation there may
            // stop observer for newly created activity
            stopObservation();
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
            newImageObserver = new ImagesObserver(imagesDirPath, this);
            newImageObserver.startWatching();
        } catch (Exception ex)
        {
            GuiUtils.noAlertError(TAG, ex);
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
            newImageObserver = null;
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
        final ScrollView scroll = (ScrollView) findViewById(R.id.scroll);
        thumbnailsList = (HorizontalListView) findViewById(R.id.thumbs);
        // such as thumbnailsList is has horizontal scroll it is not working
        // good when included in vertical scroll container. We should request
        // disallow intercept of touch events for the parent scroll to have
        // better user experience http://stackoverflow.com/a/11554823/527759
        thumbnailsList.setOnDownListener(new OnDownListener() {
            @Override
            public void onDown(MotionEvent e) {
                CommonUtils.debug(TAG, "thumbnailsList: onDown");
                scroll.requestDisallowInterceptTouchEvent(true);
            }
        });
        initImageWorker();
        reloadThumbs();
        restartObservation();
    }

    /**
     * Reload thumbs on the main page
     */
    private void reloadThumbs() {
        CommonUtils.debug(TAG, "reloadThumbs: started");
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.container_root) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main_thumb, menu);
            super.onCreateContextMenu(menu, v, menuInfo);
        } else {
            super.onCreateContextMenu(menu, v, menuInfo);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int menuItemIndex = item.getItemId();
        switch (menuItemIndex) {
            case R.id.menu_decode:
                if (!checkModifierTasksActive()) {
                    return false;
                }
                mDecodeImageTask = new DecodeImageTask(thumbnailsAdapter.currentData);
                mDecodeImageTask.execute();
                return true;
            case R.id.menu_scan:
                if (!checkModifierTasksActive()) {
                    return false;
                }
                mLastCurrentData = thumbnailsAdapter.currentData;
                startActivityForResult(ScanUtils.getScanActivityIntent(), SCAN_QR_CODE);
                return true;
            case R.id.menu_ignore:
                if (!checkModifierTasksActive()) {
                    return false;
                }
                String fileName = thumbnailsAdapter.currentData.imageData.file.getName();
                if (!ImagesLoader.hasSKUInFileName(fileName)
                        && !ImagesLoader.isDecodedCode(fileName)) {
                    GuiUtils.alert(R.string.main_ignoring_already_done);
                    return false;
                }
                mIgnoringTask = new IgnoringTask(thumbnailsAdapter.currentData, false);
                mIgnoringTask.execute();
                return true;
            case R.id.menu_ignore_all_from_now:
                if (!checkModifierTasksActive()) {
                    return false;
                }
                mIgnoringTask = new IgnoringTask(thumbnailsAdapter.currentData, true);
                mIgnoringTask.execute();
                return true;
            case R.id.menu_view_and_edit:
                Intent intent = new Intent(this, ExternalImagesEditActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_delete: {
                if (!checkModifierTasksActive()) {
                    return false;
                }
                mLastCurrentData = thumbnailsAdapter.currentData;
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setMessage(R.string.main_delete_confirmation);

                alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mDeleteTask = new DeleteTask(mLastCurrentData);
                        mDeleteTask.execute();
                    }
                });

                alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
                return true;
            }
            case R.id.menu_match:
                if (!checkModifierTasksActive())
                {
                    return false;
                }
                if (!settings.isCameraTimeDifferenceAssigned()) {
                    GuiUtils.alert(R.string.main_camera_sync_required);
                    return false;
                }
                mMatchingByTimeTask = new MatchingByTimeTask(thumbnailsAdapter.currentData, 0);
                mMatchingByTimeTask.execute();
                return true;
            case R.id.menu_match_with_shift: {
                if (!checkModifierTasksActive()) {
                    return false;
                }
                if (!settings.isCameraTimeDifferenceAssigned()) {
                    GuiUtils.alert(R.string.main_camera_sync_required);
                    return false;
                }
                mLastCurrentData = thumbnailsAdapter.currentData;
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle(R.string.main_matching_by_time_enter_offset);

                // Set an EditText view to get user input
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                alert.setView(input);

                alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        Double number = CommonUtils.parseNumber(value);
                        if (number == null) {
                            GuiUtils.alert(R.string.main_matching_by_time_offset_invalid);
                        } else {
                            mMatchingByTimeTask = new MatchingByTimeTask(mLastCurrentData,
                                    number == null ? 0 : number.intValue());
                            mMatchingByTimeTask.execute();
                        }
                    }
                });

                alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
                return true;
            }
            case R.id.menu_sync:
                if(!checkModifierTasksActive())
                {
                    return false;
                }
                ImageData imageData = thumbnailsAdapter.currentData.imageData;
                mSyncimeTask = new SyncTimeTask(imageData);
                mSyncimeTask.execute();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public boolean checkModifierTasksActive() {
        if (mMatchingByTimeTask != null) {
            GuiUtils.alert(R.string.main_matching_by_time_please_wait);
            return false;
        }
        if (mDecodeImageTask != null || mSyncimeTask != null) {
            GuiUtils.alert(R.string.main_decoding_please_wait);
            return false;
        }
        if (mProcessScanResultTask != null) {
            GuiUtils.alert(R.string.main_decoding_please_wait);
            return false;
        }
        if (mIgnoringTask != null) {
            GuiUtils.alert(R.string.main_ignoring_please_wait);
            return false;
        }
        if (mDeleteTask != null) {
            GuiUtils.alert(R.string.main_deleting_please_wait);
            return false;
        }
        if (mUploadTask != null) {
            GuiUtils.alert(R.string.main_uploading_please_wait);
            return false;
        }
        return true;
    }

    private void playSuccessfulBeep() {
        if (mCurrentBeep != null) {
            mCurrentBeep.stopSound();
        }

        mCurrentBeep = new SingleFrequencySoundGenerator(1500, 200);
        mCurrentBeep.playSound();
    }

    private void uploadButtonClicked() {
        if (!checkModifierTasksActive()) {
            return;
        }
        final List<ImageDataGroup> snapshot = ((ThumbsImageWorkerAdapter) mImageWorker.getAdapter()).data;
        Map<String, Integer> skuPhotoCounters = new HashMap<String, Integer>();
        int photosCount = 0;
        int productsInRangeCount = 0;
        int uploadMinRangeStart = getResources().getInteger(R.integer.main_upload_min_number);
        int uploadMaxRangeStart = getResources().getInteger(R.integer.main_upload_max_number);
        for (int i = 0, size = snapshot.size(); i < size; i++) {
            ImageDataGroup idg = snapshot.get(i);
            if (TextUtils.isEmpty(idg.sku)) {
                continue;
            }
            int count = idg.data.size();
            for (ImageData id : idg.data) {
                if (ImagesLoader.isDecodedCode(id.file.getName())) {
                    count--;
                }
            }
            int value = skuPhotoCounters.containsKey(idg.sku) ? skuPhotoCounters.get(idg.sku) : 0;
            value += count;
            skuPhotoCounters.put(idg.sku, value);
            photosCount += count;
        }
        for (Entry<String, Integer> entry : skuPhotoCounters.entrySet()) {
            int count = entry.getValue();
            if (count > uploadMinRangeStart) {
                if (uploadMaxRangeStart == uploadMinRangeStart) {
                    productsInRangeCount++;
                } else if (count < uploadMaxRangeStart) {
                    productsInRangeCount++;
                }
            }
        }
        if (photosCount == 0) {
            GuiUtils.alert(R.string.main_upload_no_items);
        } else {
            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

            alert.setTitle(R.string.main_upload_title);

            View uploadQuestionView = getLayoutInflater()
                    .inflate(R.layout.main_upload_dialog, null);
            TextView uploadQuestion = (TextView) uploadQuestionView
                    .findViewById(R.id.uploadQuestion);
            TextView imagesCountStat = (TextView) uploadQuestionView
                    .findViewById(R.id.imagesCountStat);
            uploadQuestion.setText(getString(R.string.main_upload_question, photosCount,
                    skuPhotoCounters.size()));
            if (productsInRangeCount == 0) {
                imagesCountStat.setVisibility(View.GONE);
            } else {
                if (uploadMaxRangeStart == uploadMinRangeStart) {
                    imagesCountStat.setText(getString(
                            R.string.main_upload_products_count_stat_no_range,
                            productsInRangeCount, uploadMinRangeStart));
                } else {
                    imagesCountStat.setText(getString(
                            R.string.main_upload_products_count_stat_range, productsInRangeCount,
                            uploadMinRangeStart, uploadMaxRangeStart));
                }
            }
            alert.setView(uploadQuestionView);

            alert.setPositiveButton(R.string.main_upload, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    mUploadTask = new UploadTask(snapshot);
                    mUploadTask.execute();
                }
            });

            alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCAN_QR_CODE) {
            if (resultCode == RESULT_OK) {
                String contents = ScanUtils.getSanitizedScanResult(data);
                if (contents != null) {
                    String[] urlData = contents.split("/");
                    String sku = urlData[urlData.length - 1];
                    mProcessScanResultTask = new ProcessScanResultsTask(mLastCurrentData, sku);
                    mProcessScanResultTask.execute();
                }
            }
        }

    }
    
    abstract class DataModifierTask extends SimpleAsyncTask {
    
        boolean mObservationDelayed = false;
        
        public DataModifierTask(LoadingControl loadingControl) {
            super(loadingControl);
        }

        void incModifiersIfNecessary() {
            if (!mObservationDelayed && newImageObserver != null) {
                newImageObserver.incModifiers();
                mObservationDelayed = true;
            }
        }

        abstract void nullifyTask();

        void onFinish() {
            nullifyTask();
            if (mObservationDelayed && newImageObserver != null) {
                newImageObserver.decModifiers();
            }
            if (mObservationDelayed) {
                reloadThumbs();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            onFinish();
        }

        @Override
        protected void onSuccessPostExecute() {
            onFinish();
        }

        @Override
        protected void onFailedPostExecute() {
            super.onFailedPostExecute();
            onFinish();
        }
    }

    class UploadTask extends DataModifierTask {
        List<ImageDataGroup> mData;

        public UploadTask(List<ImageDataGroup> data) {
            super(mPrepareUploadingLoadingControl);
            this.mData = data;
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled()) {
                    return false;
                }
                File destinationDir = new File(JobCacheManager.getProdImagesQueuedDirName());

                if (!destinationDir.exists()) {
                    if (destinationDir.mkdirs() == false) {
                        return false;
                    }
                }

                for (int i = 0, size = mData.size(); i < size; i++) {
                    ImageDataGroup idg = mData.get(i);
                    if (TextUtils.isEmpty(idg.sku)) {
                        continue;
                    }
                    processImageDataGroup(idg, destinationDir, mJobControlInterface);
                }

                DiskLruCache.clearCaches(MyApplication.getContext(),
                        ImageCache.LOCAL_THUMBS_CACHE_DIR);
                ImageCacheUtils.sendDiskCacheClearedBroadcast();
                return !isCancelled();
            } catch (Exception e) {
                GuiUtils.noAlertError(TAG, e);
            }
            return false;
        }

        boolean processImageDataGroup(ImageDataGroup idg, File destinationDir,
                JobControlInterface jobControl) {
            boolean addJob = false;
            for (int i = 0, size = idg.data.size(); i < size; i++) {
                if (isCancelled()) {
                    return false;
                }
                ImageData id = idg.data.get(i);
                File destinationFile = new File(destinationDir, id.file.getName());
                incModifiersIfNecessary();
                boolean renameSuccessful = id.file.renameTo(destinationFile);

                if (renameSuccessful) {
                    addJob = true;
                } else {
                    CommonUtils
                            .debug(TAG,
                                    "UploadTask.processImageDataGroup: Unable to rename file from %1$s to %2$s",
                                    id.file.getAbsolutePath(), destinationFile.getAbsolutePath());
                    Log.d(TAG, "Unable to rename file from: " + id.file.getAbsolutePath() + " to "
                            + destinationFile.getAbsolutePath());
                }
            }
            if (addJob) {
                ExternalImagesJob ejob = new ExternalImagesJob(System.currentTimeMillis(), idg.sku,
                        null, settings.getProfileID(), 0);

                jobControl.addExternalImagesJob(ejob);
            }
            return true;
        }
        @Override
        void nullifyTask() {
            mUploadTask = null;
        }

        @Override
        protected void onSuccessPostExecute() {
            super.onSuccessPostExecute();
            GuiUtils.alert(R.string.main_uploading_success);
        }
    }
    
    class DeleteTask extends DataModifierTask {
        ThumbnailsAdapter.CurrentDataInfo mCurrentDataInfo;

        public DeleteTask(ThumbnailsAdapter.CurrentDataInfo currentDataInfo) {
            super(mDeletingLoadingControl);
            this.mCurrentDataInfo = currentDataInfo;
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled()) {
                    return false;
                }
                ImageData imageData = mCurrentDataInfo.imageData;
                incModifiersIfNecessary();
                CommonUtils.debug(TAG, "DeleteTask.doInBackground: delete image file %1$s",
                        imageData.file.getAbsolutePath());
                imageData.file.delete();
                return !isCancelled();
            } catch (Exception e) {
                GuiUtils.noAlertError(TAG, e);
            }
            return false;
        }

        @Override
        void nullifyTask() {
            mDeleteTask = null;
        }

        @Override
        protected void onSuccessPostExecute() {
            super.onSuccessPostExecute();
            GuiUtils.alert(R.string.main_deleting_success);
        }
    }
    
    class IgnoringTask extends DataModifierTask {
        ThumbnailsAdapter.CurrentDataInfo mCurrentDataInfo;
        boolean mIgnoreAllStartingFrom;

        public IgnoringTask(ThumbnailsAdapter.CurrentDataInfo currentDataInfo,
                boolean ignoreAllStartingFrom) {
            super(mIgnoringLoadingControl);
            this.mCurrentDataInfo = currentDataInfo;
            this.mIgnoreAllStartingFrom = ignoreAllStartingFrom;
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled()) {
                    return false;
                }
                ImageData imageData = mCurrentDataInfo.imageData;
                if(mIgnoreAllStartingFrom)
                {
                    for (int i = mCurrentDataInfo.groupPosition, size = mCurrentDataInfo.dataSnapshot
                            .size(); i < size; i++) {
                        ImageDataGroup idg = mCurrentDataInfo.dataSnapshot.get(i);
                        int startPos = i == mCurrentDataInfo.groupPosition ? mCurrentDataInfo.inGroupPosition
                                : 0;

                        if (!processImageDataGroup(idg, startPos)) {
                            return false;
                        }
                    }
                } else
                {
                    incModifiersIfNecessary();
                    CommonUtils.debug(TAG, "IgnoringTask.doInBackground: undo image file %1$s",
                            imageData.file.getAbsolutePath());
                    ImagesLoader.undoImage(imageData.file);
                }
                return !isCancelled();
            } catch (Exception e) {
                GuiUtils.noAlertError(TAG, e);
            }
            return false;
        }

        private boolean processImageDataGroup(ImageDataGroup idg, int startPos) {
            for (int i = startPos, size2 = idg.data.size(); i < size2; i++) {
                ImageData id = idg.data.get(i);
                CommonUtils.debug(TAG,
                                "IgnoringTask.processImageDataGroup: undo image file %1$s",
                        id.file.getAbsolutePath());
                incModifiersIfNecessary();
                ImagesLoader.undoImage(id.file);
                if (isCancelled()) {
                    return false;
                }
            }
            return true;
        }
        @Override
        void nullifyTask() {
            mIgnoringTask = null;
        }

        @Override
        protected void onSuccessPostExecute() {
            super.onSuccessPostExecute();
            GuiUtils.alert(R.string.main_ignoring_success);
        }
    }

    class ProcessScanResultsTask extends DataModifierTask {
        ThumbnailsAdapter.CurrentDataInfo mCurrentDataInfo;
        String mSku;

        public ProcessScanResultsTask(ThumbnailsAdapter.CurrentDataInfo currentDataInfo, String sku) {
            super(mScanResultProcessingLoadingControl);
            this.mCurrentDataInfo = currentDataInfo;
            this.mSku = sku;
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled()) {
                    return false;
                }
                ImageDataGroup idg = mCurrentDataInfo.dataSnapshot
                        .get(mCurrentDataInfo.groupPosition);
                String startingSku = idg.sku;
                if (TextUtils.equals(startingSku, mSku)) {
                    return true;
                }
                int startPos;
                startPos = TextUtils.isEmpty(startingSku) ? mCurrentDataInfo.groupPosition : 0;
                for (int i = startPos, size = mCurrentDataInfo.dataSnapshot
                        .size(); i < size; i++) {
                    idg = mCurrentDataInfo.dataSnapshot.get(i);
                    if (TextUtils.isEmpty(startingSku)) {
                        startPos = mCurrentDataInfo.inGroupPosition;
                        if (!TextUtils.equals(startingSku, idg.sku)) {
                            break;
                        }
                    } else {
                        startPos = 0;
                        if (!TextUtils.equals(startingSku, idg.sku)) {
                            continue;
                        }
                    }

                    if (!processImageDataGroup(idg, startPos)) {
                        return false;
                    }
                }
                return !isCancelled();
            } catch (Exception e) {
                GuiUtils.noAlertError(TAG, e);
            }
            return false;
        }

        private boolean processImageDataGroup(ImageDataGroup idg, int startPos) {
            for (int i = startPos, size2 = idg.data.size(); i < size2; i++) {
                ImageData id = idg.data.get(i);
                CommonUtils.debug(TAG,
                                "ProcessScanResultsTask.processImageDataGroup: queuing file %1$s for sku %2$s",
                        id.file.getAbsolutePath(), mSku);
                incModifiersIfNecessary();
                ImagesLoader.queueImage(id.file, mSku, true, false);
                if (isCancelled()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        void nullifyTask() {
            mProcessScanResultTask = null;
        }

        @Override
        protected void onSuccessPostExecute() {
            super.onSuccessPostExecute();
            GuiUtils.alert(R.string.main_scan_success);
        }
    }
    
    class DecodeImageTask extends DataModifierTask {
        int mScreenLargerDimension;
        ThumbnailsAdapter.CurrentDataInfo mCurrentDataInfo;
        String mCode;

        public DecodeImageTask(ThumbnailsAdapter.CurrentDataInfo currentDataInfo) {
            super(mMatchingByTimeStatusLoadingControl);
            this.mCurrentDataInfo = currentDataInfo;
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mScreenLargerDimension = metrics.widthPixels;
            if (mScreenLargerDimension < metrics.heightPixels) {
                mScreenLargerDimension = metrics.heightPixels;
            }
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled()) {
                    return false;
                }
                Bitmap bitmap = ImageUtils.decodeSampledBitmapFromFile(
                        mCurrentDataInfo.imageData.file.getAbsolutePath(), mScreenLargerDimension,
                        mScreenLargerDimension);
                ZXingCodeScanner multiDetector = new ZXingCodeScanner();
                mCode = multiDetector.decode(bitmap);
                if (mCode != null) {

                    String[] urlData = mCode.split("/");
                    String sku = urlData[urlData.length - 1];
                    String startingSku = mCurrentDataInfo.dataSnapshot
                            .get(mCurrentDataInfo.groupPosition).sku;
                    for (int i = mCurrentDataInfo.groupPosition, size = mCurrentDataInfo.dataSnapshot
                            .size(); i < size; i++) {
                        ImageDataGroup idg = mCurrentDataInfo.dataSnapshot.get(i);
                        if (!TextUtils.equals(startingSku, idg.sku)) {
                            break;
                        }
                        int startPos = mCurrentDataInfo.inGroupPosition;
                        if (!processImageDataGroup(sku, idg, startPos, i)) {
                            return false;
                        }
                    }
                }
                return !isCancelled();
            } catch (Exception e) {
                GuiUtils.noAlertError(TAG, e);
            }
            return false;
        }

        private boolean processImageDataGroup(String sku, ImageDataGroup idg, int startPos,
                int groupPosition) {
            for (int i = startPos, size2 = idg.data.size(); i < size2; i++) {
                ImageData id = idg.data.get(i);
                    CommonUtils
                            .debug(TAG,
                        "ScanTask.processImageDataGroup: queuing file %1$s for sku %2$s",
                        id.file.getAbsolutePath(), sku);
                incModifiersIfNecessary();
                ImagesLoader.queueImage(id.file, sku, true,
                        groupPosition == mCurrentDataInfo.groupPosition && i == startPos);
                if (isCancelled()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        void nullifyTask() {
            mDecodeImageTask = null;
        }

        @Override
        protected void onSuccessPostExecute() {
            super.onSuccessPostExecute();
            if (mCode != null) {
                playSuccessfulBeep();
                GuiUtils.alert(R.string.main_decoding_Image_success);
            } else {
                GuiUtils.alert(R.string.main_decoding_Image_failed);
            }
        }
    }

    class MatchingByTimeTask extends DataModifierTask {
        
        ThumbnailsAdapter.CurrentDataInfo mCurrentDataInfo;
        int mManualShift;

        public MatchingByTimeTask(ThumbnailsAdapter.CurrentDataInfo currentDataInfo, int manualShift) {
            super(mMatchingByTimeStatusLoadingControl);
            this.mCurrentDataInfo = currentDataInfo;
            this.mManualShift = manualShift;
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled()) {
                    return false;
                }
                for (int i = mCurrentDataInfo.groupPosition, size = mCurrentDataInfo.dataSnapshot
                        .size(); i < size; i++) {
                    ImageDataGroup idg = mCurrentDataInfo.dataSnapshot.get(i);
                    if (!TextUtils.isEmpty(idg.sku) && i != mCurrentDataInfo.groupPosition) {
                        break;
                    }
                    int startPos = i == mCurrentDataInfo.groupPosition ? mCurrentDataInfo.inGroupPosition
                            : 0;
                    for (int j = startPos, size2 = idg.data.size(); j < size2; j++) {
                        ImageData id = idg.data.get(j);
                        long exifTime = ImageUtils.getExifDateTime(id.file.getAbsolutePath());
                        GalleryTimestampRange gtr = JobCacheManager
                                .getSkuProfileIDForExifTimeStamp(MyApplication.getContext(),
                                        exifTime + mManualShift * 1000);
                        if (gtr != null) {
                            CommonUtils
                                    .debug(TAG,
                                            "MatchingByTimeTask.doInBackground: queuing file %1$s for sku %2$s",
                                            id.file.getAbsolutePath(), gtr.escapedSKU);
                            incModifiersIfNecessary();
                            ImagesLoader.queueImage(id.file, gtr.escapedSKU, true, false);
                        }
                        if (!TextUtils.isEmpty(idg.sku)) {
                            break;
                        }
                        if (isCancelled()) {
                            return false;
                        }
                    }
                }
                return !isCancelled();
            } catch (Exception e) {
                GuiUtils.noAlertError(TAG, e);
            }
            return false;
        }

        @Override
        void nullifyTask() {
            mMatchingByTimeTask = null;
        }


        @Override
        protected void onSuccessPostExecute() {
            super.onSuccessPostExecute();
            GuiUtils.alert(R.string.main_matching_by_time_success);
        }

    }
    
    class SyncTimeTask extends DataModifierTask {
        ImageData mImageData;
        int mScreenLargerDimension;
        String mCode;
        long mExifDateTime = -1;

        public SyncTimeTask(ImageData imageData) {
            super(mDecodeStatusLoadingControl);
            this.mImageData = imageData;

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mScreenLargerDimension = metrics.widthPixels;
            if (mScreenLargerDimension < metrics.heightPixels) {
                mScreenLargerDimension = metrics.heightPixels;
            }
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled()) {
                    return false;
                }
                Bitmap bitmap = ImageUtils.decodeSampledBitmapFromFile(
                        mImageData.file.getAbsolutePath(), mScreenLargerDimension,
                        mScreenLargerDimension);
                ZXingCodeScanner multiDetector = new ZXingCodeScanner();
                mCode = multiDetector.decode(bitmap);
                mExifDateTime = ImageUtils.getExifDateTime(mImageData.file.getAbsolutePath());
                if (mCode != null && mCode.startsWith(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX)
                        && mExifDateTime != -1) {
                    incModifiersIfNecessary();
                    mImageData.file.delete();
                }
                return !isCancelled();
            } catch (Exception e) {
                GuiUtils.noAlertError(TAG, e);
            }
            return false;
        }

        @Override
        void nullifyTask() {
            mSyncimeTask = null;
        }

        @Override
        protected void onSuccessPostExecute() {
            super.onSuccessPostExecute();
            if (mCode == null) {
                GuiUtils.alert(R.string.main_decoding_failed);
            } else {
                if (mCode.startsWith(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX)) {
                    if (mExifDateTime != -1) {
                        Date phoneDate = CommonUtils.parseDateTime(mCode
                                .substring(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX.length()));

                        int timeDifference = (int) ((phoneDate.getTime() - mExifDateTime) / 1000);

                        settings.setCameraTimeDifference(timeDifference);
                        GuiUtils.alert(R.string.main_decoding_camera_success, timeDifference);
                    } else {
                        GuiUtils.alert(R.string.main_decoding_failed_exif_date);
                    }
                } else {
                    GuiUtils.alert(R.string.main_decoding_invalid_information);
                }
            }
        }
    }

    class LoadThumbsTask extends SimpleAsyncTask
    {
        ThumbsImageWorkerAdapter adapter;

        public LoadThumbsTask() {
            super(mThumbsLoadingControl);
        }

        @Override
        protected void onFailedPostExecute() {
            super.onFailedPostExecute();
            if (isActivityAlive)
            {
            }
        }

        @Override
        protected void onSuccessPostExecute() {
            if (!isCancelled()) {
                try
                {
                    mUploadButton.setEnabled(adapter.getSize() > 0);
                    final boolean scrollToEnd = thumbnailsAdapter == null
                            || thumbnailsList.getRightViewIndex() == thumbnailsAdapter.getCount();
                    mImageWorker.setAdapter(adapter);
                    if (thumbnailsAdapter == null) {
                        thumbnailsAdapter = new ThumbnailsAdapter(MainActivity.this, mImageWorker);
                        thumbnailsList.setAdapter(thumbnailsAdapter);
                    } else {
                        thumbnailsAdapter.notifyDataSetChanged();
                    }
                    if (scrollToEnd) {
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
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled()) {
                    return false;
                }
                String imagesDirPath = settings.getGalleryPhotosDirectory();
                List<ImageDataGroup> data = new ArrayList<ImageDataGroup>();
                if (!TextUtils.isEmpty(imagesDirPath)) {
                    File f = new File(imagesDirPath);

                    File[] files = f.listFiles(new FileFilter() {

                        @Override
                        public boolean accept(File pathname) {
                            return (pathname.getName().toLowerCase().contains(".jpg") && !pathname
                                    .isDirectory());
                        }
                    });
                    if (files != null && files.length > 0) {
                        Arrays.sort(files, ExternalImagesEditActivity.filesComparator);
                        ImageDataGroup lastDataGroup = null;
                        for (File file : files) {
                            if (isCancelled()) {
                                return false;
                            }
                            ImageData id = ImageData.getImageDataForFile(file, true);
                            String sku = getSku(id);
                            ImageDataGroup childData;
                            if (lastDataGroup != null && TextUtils.equals(lastDataGroup.sku, sku)) {
                                childData = lastDataGroup;
                            } else {
                                childData = new ImageDataGroup();
                                if (TextUtils.isEmpty(sku)) {
                                    childData.cached = true;
                                }
                                data.add(childData);
                                childData.sku = sku;
                            }
                            childData.data.add(id);
                            lastDataGroup = childData;
                        }
                    }
                }
                adapter = new ThumbsImageWorkerAdapter(data);
                return !isCancelled();
            } catch (Exception e) {
                GuiUtils.noAlertError(TAG, e);
            }
            return false;
        }

        public String getSku(ImageData value) {
            return ImagesLoader.getSkuFromFileName(value.file.getName());
        }
    }

    public static class ImagesObserver extends FileObserver {

        public static AtomicLong sLastUpdatedTime = new AtomicLong(0);

        private final String mPath;
        private MainActivity mActivity;
        private AtomicInteger mModifiers = new AtomicInteger(0);

        public ImagesObserver(String path, MainActivity activity) {
            super(path,
                    FileObserver.DELETE |
                            FileObserver.DELETE_SELF |
                            FileObserver.MOVE_SELF |
                            FileObserver.MOVED_FROM |
                            FileObserver.MOVED_TO |
                            FileObserver.CLOSE_WRITE);
            mPath = path;
            mActivity = activity;
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
                    String type = FileUtils.getMimeType(file);
                    if (type != null && type.toLowerCase().startsWith("image/"))
                    {
                        reloadThumbsUI();
                    } else if (ImagesLoader.isSpecialRenamedFile(file))
                    {
                        reloadThumbsUI();
                    }
                }
            } catch (Exception ex)
            {
                GuiUtils.noAlertError(TAG, ex);
            }
        }

        void reloadThumbsUI() {
            sLastUpdatedTime.set(SystemClock.elapsedRealtime());
            if (mModifiers.get() == 0) {
                CommonUtils.debug(TAG, "reloadThumbsUI: modifiers are inactive. Request reloading");
                GuiUtils.post(new Runnable() {

                    @Override
                    public void run() {
                        mActivity.reloadThumbs();
                    }
                });
            } else {
                CommonUtils.debug(TAG, "reloadThumbsUI: modifiers are active. Skipping");
            }
        }

        public void incModifiers() {
            mModifiers.incrementAndGet();
        }

        public void decModifiers() {
            mModifiers.decrementAndGet();
        }
    }

    public static class ImageDataGroup {
        List<ImageData> data = new ArrayList<ImageData>();
        String sku;
        String name;
        boolean cached = false;
        AtomicBoolean loadRequested = new AtomicBoolean(false);
        AtomicBoolean loadFailed = new AtomicBoolean(false);
        AtomicBoolean doesntExist = new AtomicBoolean(false);
    }

    public static class ImageData
    {
        File file;
        int width;
        int height;
        int viewWidth;
        int viewHeight;

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

        public static ImageData getImageDataForFile(File file, boolean supportCropRect)
                throws IOException {
            Rect cropRect = supportCropRect ? ImagesLoader.getBitmapRect(file) : null;
            int width, height;
            if (cropRect == null) {
                BitmapFactory.Options options = ImageUtils.calculateImageSize(file
                        .getAbsolutePath());
                width = options.outWidth;
                height = options.outHeight;
            } else {
                width = cropRect.width();
                height = cropRect.height();
            }
            int orientation = ImageUtils.getOrientationInDegreesForFileName(file.getAbsolutePath());
            if (orientation == 90 || orientation == 270) {
                int tmp = width;
                width = height;
                height = tmp;
            }
            return new ImageData(file, width, height);
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }
    }

    public class ThumbsImageWorkerAdapter extends
            ImageWorkerAdapter
    {
        public List<ImageDataGroup> data;

        ThumbsImageWorkerAdapter(List<ImageDataGroup> data) throws IOException
        {
            this.data = data;
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

    /**
     * Extension of HorizontalListView which moves group description to be
     * visible on scroll for the first child. Had to use custom component
     * instead of layout listeners because they are available only since api 11
     */
    public static class HorizontalListViewExt extends HorizontalListView {

        int thumbGroupBorder;
        Stack<View> mUnusedViews = new Stack<View>();

        public HorizontalListViewExt(Context context, AttributeSet attrs) {
            super(context, attrs);
            thumbGroupBorder = context.getResources().getDimensionPixelSize(
                    R.dimen.home_thumbnail_group_border);
        }

        @Override
        protected synchronized void onLayout(boolean changed, int left, int top, int right,
                int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            if (getChildCount() > 0) {
                View child = getChildAt(0);
                CommonUtils.debug(TAG, "childOffset: %1$d", child.getLeft());
                ThumbnailsAdapter.GroupViewHolder gvh = (ThumbnailsAdapter.GroupViewHolder) child
                        .getTag();
                gvh.groupDescription.layout(Math.max(thumbGroupBorder, -child.getLeft()),
                        gvh.groupDescription.getTop(), gvh.groupDescription.getMeasuredWidth(),
                        gvh.groupDescription.getMeasuredHeight());
                layoutImages(gvh, child);
                for (int i = 1; i < getChildCount(); i++) {
                    gvh = (ThumbnailsAdapter.GroupViewHolder) getChildAt(i).getTag();
                    gvh.groupDescription.layout(thumbGroupBorder, gvh.groupDescription.getTop(),
                            gvh.groupDescription.getMeasuredWidth(),
                            gvh.groupDescription.getMeasuredHeight());
                    layoutImages(gvh, getChildAt(i));
                }
            }
        }

        void layoutImages(ThumbnailsAdapter.GroupViewHolder gvh, View child) {
            int usedChildCount = addOrReuseChilds(gvh, gvh.data,
                    child.getLeft() + thumbGroupBorder,
                    getWidth());
            removeUnusedViews(gvh.images, usedChildCount);
        }

        private int addOrReuseChilds(ThumbnailsAdapter.GroupViewHolder holder, ImageDataGroup idg,
                int parentOffset, int width) {
            List<ImageDataGroup> snapshot = holder.dataSnapshot;
            int offset = 0;
            View view;
            int childCount = holder.images.getChildCount();
            int usedChild = 0;
            int usedChildCount = 0;
            for (int i = 0, size = idg.data.size(); i < size; i++) {
                ImageData value = idg.data.get(i);
                int rightPos = value.viewWidth + offset;
                if (rightPos + parentOffset < 0) {
                    offset = rightPos;
                    continue;
                }
                boolean add = false;
                if (usedChild < childCount) {
                    view = holder.images.getChildAt(usedChild++);
                } else {
                    if (!mUnusedViews.isEmpty()) {
                        CommonUtils.debug(TAG, "Reusing view from the stack");
                        view = mUnusedViews.pop();
                    } else {
                        view = null;
                    }
                    add = true;
                }
                usedChildCount++;
                View singleImageView = getSingleImageView(new ThumbnailsAdapter.CurrentDataInfo(
                        value, holder.position, i, snapshot), view, holder);
                singleImageView.measure(
                        MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.UNSPECIFIED),
                        MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.UNSPECIFIED));
                if (add) {
                    holder.images.addView(singleImageView);
                }
                singleImageView.layout(offset, 0, offset + value.viewWidth, value.viewHeight);
                if (rightPos + parentOffset >= width) {
                    break;
                }
                offset = rightPos;
            }
            return usedChildCount;
        }

        public final View getSingleImageView(ThumbnailsAdapter.CurrentDataInfo cdi,
                View convertView, ThumbnailsAdapter.GroupViewHolder gvh) {
            ImageWorker mImageWorker = ((ThumbnailsAdapter) getAdapter()).mImageWorker;
            LayoutInflater mInflater = ((ThumbnailsAdapter) getAdapter()).mInflater;
            OnClickListener mImageItemOnClickListener = ((ThumbnailsAdapter) getAdapter()).mImageItemOnClickListener;
            ItemViewHolder holder;
            if (convertView == null) { // if it's not recycled, instantiate and
                                       // initialize
                convertView = mInflater.inflate(R.layout.main_item_thumb_image, null);
                holder = new ItemViewHolder();
                holder.containerRoot = convertView.findViewById(R.id.container_root);
                holder.selectedOverlay = convertView.findViewById(R.id.selection_overlay);
                holder.imageView = (ImageView) convertView.findViewById(R.id.image);
                holder.containerRoot.setOnClickListener(mImageItemOnClickListener);
                convertView.setTag(holder);
            } else { // Otherwise re-use the converted view
                holder = (ItemViewHolder) convertView.getTag();
            }
            holder.containerRoot.setTag(cdi);
            ImageData data = cdi.imageData;
            if (holder.data == data) {
                CommonUtils.debug(TAG,
                        "getSingleImageView: requested view for same data. Return cached.");
                return convertView;
            }
            holder.data = data;
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) holder.containerRoot
                    .getLayoutParams();
            if (layoutParams.width != data.viewWidth || layoutParams.height != data.viewHeight) {
                layoutParams = new FrameLayout.LayoutParams(data.viewWidth, data.viewHeight);
                holder.containerRoot.setLayoutParams(layoutParams);
            }

            CommonUtils.debug(TAG, "getSingleImageView: height: %1$d %2$d width: %3$d %4$d",
                    data.viewHeight, holder.containerRoot.getLayoutParams().height, data.viewWidth,
                    holder.containerRoot.getLayoutParams().width);
            holder.selectedOverlay
                    .setVisibility(ImagesLoader.hasSKUInFileName(data.file.getName()) ? View.VISIBLE
                            : View.INVISIBLE);
            mImageWorker.loadImage(data, holder.imageView);
            return convertView;
        }

        protected void removeUnusedViews(ViewGroup view, int usedChilds) {
            for (int i = view.getChildCount() - 1; i >= usedChilds; i--) {
                View subView = view.getChildAt(i);
                ItemViewHolder viewHolder = (ItemViewHolder) subView.getTag();
                ImageView imageView = viewHolder.imageView;
                ImageWorker.cancelPotentialWork(null, imageView);
                mUnusedViews.add(subView);
                view.removeViewAt(i);
            }
        }
    }
    public static class ThumbnailsAdapter extends BaseAdapter {

        protected int mItemBorder;
        private ImageResizer mImageWorker;
        LayoutInflater mInflater;
        private SettingsSnapshot mSettingsSnapshot;
        int mThumbGroupBorder;
        int mItemHeight;
        WeakReference<Activity> mContext;
        public CurrentDataInfo currentData;

        private OnClickListener mTextViewOnClickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                String value = (String) v.getTag();
                if (TextUtils.isEmpty(value)) {
                    return;
                }
                final Intent intent;
                intent = new Intent(mContext.get(), ScanActivity.class);
                intent.putExtra(mContext.get().getString(R.string.ekey_product_sku), value);

                mContext.get().startActivity(intent);
            }
        };
        public OnClickListener mImageItemOnClickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                currentData = (CurrentDataInfo) v.getTag();
                mContext.get().registerForContextMenu(v);
                v.showContextMenu();
                mContext.get().unregisterForContextMenu(v);
            }
        };

        public ThumbnailsAdapter(Activity context, ImageResizer imageWorker)
        {
            super();
            this.mContext = new WeakReference<Activity>(context);
            mThumbGroupBorder = context.getResources().getDimensionPixelSize(
                    R.dimen.home_thumbnail_group_border);
            mItemHeight = context.getResources().getDimensionPixelSize(
                    R.dimen.home_thumbnail_with_border_size);
            mSettingsSnapshot = new SettingsSnapshot(context);
            this.mImageWorker = imageWorker;
            this.mInflater = LayoutInflater.from(context);
            mItemBorder = context.getResources().getDimensionPixelSize(
                    R.dimen.home_thumbnail_border);
        }

        @Override
        public int getCount()
        {
            return mImageWorker.getAdapter().getSize();
        }

        @Override
        public ImageDataGroup getItem(int position)
        {
            return (ImageDataGroup) mImageWorker.getAdapter().getItem(position);
        }

        @Override
        public long getItemId(int position)
        {
            return getItem(position).data.get(0).file.getAbsolutePath().hashCode();
        }

        @Override
        public boolean hasStableIds()
        {
            return super.hasStableIds();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GroupViewHolder holder;
            if (convertView == null) { // if it's not recycled, instantiate and
                                       // initialize
                convertView = mInflater.inflate(R.layout.main_item_thumb_images_group, null);
                holder = new GroupViewHolder();
                holder.containerRoot = convertView.findViewById(R.id.containerRoot);
                holder.images = (LinearLayout) convertView.findViewById(R.id.images);
                holder.groupDescription = convertView.findViewById(R.id.groupDescription);
                holder.sku = (TextView) convertView.findViewById(R.id.sku);
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.groupDescription.setOnClickListener(mTextViewOnClickListener);
                holder.loadingControl = new SimpleViewLoadingControl(
                        convertView.findViewById(R.id.loadingIndicator));
                convertView.setTag(holder);
            } else { // Otherwise re-use the converted view
                holder = (GroupViewHolder) convertView.getTag();
            }
            ImageDataGroup idg = getItem(position);
            if (holder.data == idg) {
                CommonUtils.debug(TAG, "getView: requested view for same data. Return cached.");
                return convertView;
            }
            holder.data = idg;
            holder.dataSnapshot = ((ThumbsImageWorkerAdapter) mImageWorker.getAdapter()).data;
            holder.position = position;
            if (holder.loaderTask != null) {
                holder.loaderTask.cancel(true);
            }
            if (!idg.cached && !idg.loadFailed.get()) {
                holder.loaderTask = new CacheLoaderTask(idg, holder, holder.loadingControl);
                holder.loaderTask.execute();
            }
            setProductInformation(holder, idg);
            int expectedWidth = 0;
            int expectedHeight = 0;
            for (int i = 0; i < idg.data.size(); i++) {
                ImageData data = idg.data.get(i);
                int width = mImageWorker.getImageWidth();
                int height = mImageWorker.getImageHeight();
                if (data.width != 0 && data.height != 0) {
                    float ratio = (float) data.width / data.height;
                    width = (int) (ratio * height);
                }
                width += 2 * mItemBorder;
                height += 2 * mItemBorder;
                data.viewWidth = width;
                data.viewHeight = height;
                expectedWidth += width;
                expectedHeight = height;
            }
            {
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.images
                        .getLayoutParams();
                if (layoutParams.width != expectedWidth || layoutParams.height != expectedHeight) {
                    layoutParams = new LinearLayout.LayoutParams(expectedWidth, expectedHeight);
                    holder.images.setLayoutParams(layoutParams);
                }
            }
            int width = expectedWidth + 2 * mThumbGroupBorder;
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) holder.containerRoot
                    .getLayoutParams();
            if (layoutParams.width != width || layoutParams.height != mItemHeight) {
                layoutParams = new FrameLayout.LayoutParams(width, mItemHeight);
                holder.containerRoot.setLayoutParams(layoutParams);
            }
            return convertView;
        }

        private void setProductInformation(GroupViewHolder holder, ImageDataGroup idg) {
            String skuText;
            String nameText;
            if (TextUtils.isEmpty(idg.sku)) {
                skuText = null;
            } else {
                skuText = CommonUtils.getStringResource(R.string.main_sku_text, idg.sku);
            }
            holder.name.setTextColor(holder.sku.getCurrentTextColor());
            if (idg.cached) {
                if (skuText == null) {
                    nameText = CommonUtils.getStringResource(R.string.main_unassigned);
                    holder.name.setTextColor(Color.RED);
                } else {
                    nameText = CommonUtils.getStringResource(R.string.main_name_text, idg.name);
                }
            } else {
                if (idg.doesntExist.get()) {
                    nameText = CommonUtils.getStringResource(R.string.main_load_doesnt_exist);
                    holder.name.setTextColor(Color.RED);
                } else if (idg.loadFailed.get()) {
                    nameText = CommonUtils.getStringResource(R.string.main_load_failed);
                    holder.name.setTextColor(Color.RED);
                } else {
                    nameText = null;
                }
            }
            holder.sku.setText(skuText);
            holder.name.setText(nameText);

            holder.groupDescription.setTag(idg.sku);
        }

        public static class GroupViewHolder {
            public LinearLayout images;
            public ImageDataGroup data;
            public List<ImageDataGroup> dataSnapshot;
            public int position;
            View containerRoot;
            View groupDescription;
            TextView sku;
            TextView name;
            CacheLoaderTask loaderTask;
            LoadingControl loadingControl;
        }

        public static class ItemViewHolder {
            View selectedOverlay;
            View containerRoot;
            ImageView imageView;
            public ImageData data;
        }

        public static class CurrentDataInfo {
            ImageData imageData;
            int groupPosition;
            int inGroupPosition;
            List<ImageDataGroup> dataSnapshot;

            public CurrentDataInfo(ImageData imageData, int groupPosition, int inGroupPosition,
                    List<ImageDataGroup> dataSnapshot) {
                super();
                this.imageData = imageData;
                this.groupPosition = groupPosition;
                this.inGroupPosition = inGroupPosition;
                this.dataSnapshot = dataSnapshot;
            }
        }

        /**
         * The actual AsyncTaskEx that will asynchronously process the image.
         */
        private class CacheLoaderTask extends SimpleAsyncTask implements OperationObserver {
            private final WeakReference<GroupViewHolder> groupViewHolderReference;
            private ImageDataGroup idg;

            private CountDownLatch doneSignal;
            private boolean success;
            private boolean doesntExist;
            private int requestId = MageventoryConstants.INVALID_REQUEST_ID;

            private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();

            public CacheLoaderTask(ImageDataGroup idg, GroupViewHolder groupViewHolder,
                    LoadingControl loadingControl) {
                super(loadingControl);
                groupViewHolderReference = new WeakReference<GroupViewHolder>(groupViewHolder);
                this.idg = idg;
            }

            /**
             * Background processing.
             */
            @Override
            protected Boolean doInBackground(Void... ps) {

                try {
                    ProductDetailsExistResult existResult;
                    if (!isCancelled()) {
                        existResult = JobCacheManager.productDetailsExist(idg.sku,
                                mSettingsSnapshot.getUrl(), true);
                    } else {
                        return false;
                    }
                    if (existResult.isExisting()) {
                        updateImageDataGroupFromProduct(existResult.getSku());
                    } else if (!idg.loadRequested.getAndSet(true)) {
                        doneSignal = new CountDownLatch(1);
                        resHelper.registerLoadOperationObserver(this);
                        try {
                            final String[] params = new String[2];
                            params[0] = MageventoryConstants.GET_PRODUCT_BY_SKU_OR_BARCODE;
                            params[1] = idg.sku;

                            Bundle b = new Bundle();
                            b.putBoolean(
                                    MageventoryConstants.EKEY_DONT_REPORT_PRODUCT_NOT_EXIST_EXCEPTION,
                                    true);
                            requestId = resHelper.loadResource(MyApplication.getContext(),
                                    MageventoryConstants.RES_PRODUCT_DETAILS, params, b,
                                    mSettingsSnapshot);
                            while (true) {
                                if (isCancelled()) {
                                    idg.loadRequested.set(false);
                                    return false;
                                }
                                try {
                                    if (doneSignal.await(1, TimeUnit.SECONDS)) {
                                        break;
                                    }
                                } catch (InterruptedException e) {
                                    idg.loadRequested.set(false);
                                    return false;
                                }
                            }
                        } finally {
                            resHelper.unregisterLoadOperationObserver(this);
                        }
                        if (success) {
                            updateImageDataGroupFromProduct(idg.sku);
                        } else {
                            idg.loadFailed.set(true);
                            idg.doesntExist.set(doesntExist);
                        }
                    }

                    return !isCancelled();
                } catch (Exception ex) {
                    GuiUtils.noAlertError(TAG, ex);
                }
                return false;
            }

            private void updateImageDataGroupFromProduct(String sku) {
                Product p = JobCacheManager.restoreProductDetails(sku, mSettingsSnapshot.getUrl());
                synchronized (idg) {
                    idg.sku = p.getSku();
                    idg.name = p.getName();
                    idg.cached = true;
                }
            }

            @Override
            protected void onSuccessPostExecute() {
                if (isCancelled()) {
                    return;
                }
                GroupViewHolder gvh = groupViewHolderReference.get();
                if (gvh != null && gvh.loaderTask == this) {
                    setProductInformation(gvh, idg);
                }
            }

            @Override
            public void onLoadOperationCompleted(LoadOperation op) {
                if (op.getOperationRequestId() == requestId) {
                    success = op.getException() == null;
                    ProductDetailsLoadException exception = (ProductDetailsLoadException) op
                            .getException();
                    if (exception != null
                            && exception.getFaultCode() == ProductDetailsLoadException.ERROR_CODE_PRODUCT_DOESNT_EXIST) {
                        doesntExist = true;
                    }
                    doneSignal.countDown();
                }
            }
        }
    }
}
