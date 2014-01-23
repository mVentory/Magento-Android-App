
package com.mageventory.activity;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
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
import com.mageventory.activity.MainActivity.HorizontalListViewExt.On2FingersDownListener;
import com.mageventory.activity.MainActivity.ThumbnailsAdapter.ItemViewHolder;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.bitmapfun.util.DiskLruCache;
import com.mageventory.bitmapfun.util.ImageCache;
import com.mageventory.bitmapfun.util.ImageCacheUtils;
import com.mageventory.bitmapfun.util.ImageCacheUtils.AbstractClearDiskCachesTask;
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
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.util.EventBusUtils.GeneralBroadcastEventHandler;
import com.mageventory.util.FileUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.ImagesLoader;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.Log;
import com.mageventory.util.Log.OnErrorReportingFileStateChangedListener;
import com.mageventory.util.ScanUtils;
import com.mageventory.util.ScanUtils.ScanState;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.SimpleViewLoadingControl;
import com.mageventory.util.SingleFrequencySoundGenerator;
import com.mageventory.util.ZXingCodeScanner;
import com.mageventory.util.ZXingCodeScanner.DetectDecodeResult;
import com.mageventory.util.concurent.SerialExecutor;
import com.mageventory.widget.HorizontalListView;
import com.mageventory.widget.HorizontalListView.OnDownListener;
import com.mageventory.widget.HorizontalListView.OnUpListener;

public class MainActivity extends BaseFragmentActivity implements GeneralBroadcastEventHandler {
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int SCAN_QR_CODE = 1;

    static final String THUMBS_CACHE_PATH = ImageCache.LOCAL_THUMBS_CACHE_DIR;
    static final String RERFRESH_PRESSED = "MainActivity.REFRESH_PRESSED";

    private static SerialExecutor sAutoDecodeExecutor = new SerialExecutor(
            Executors.newSingleThreadExecutor());

    static Comparator<CurrentDataInfo> sCurrentDataInfoComparator = new Comparator<CurrentDataInfo>() {
        @Override
        public int compare(CurrentDataInfo lhs, CurrentDataInfo rhs) {
            return ExternalImagesEditActivity.filesComparator.compare(lhs.imageData.getFile(),
                    rhs.imageData.getFile());
        }
    };
    protected MyApplication app;
    private Settings settings;
    ProgressDialog pDialog;
    private boolean isActivityAlive;
    private Button errorReportingButton;
    private int mButtonDefaultTextColor;
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
    private HorizontalListViewExt thumbnailsList;
    private View mThumbsLoadIndicator;
    private LoadingControl mThumbsLoadingControl;

    private ThumbnailsAdapter thumbnailsAdapter;

    int orientation;
    
    private TextView mDecodeStatusLineText;
    private TextView mDecodeAutoStatusLineText;
    private LoadingControl mDecodeStatusLoadingControl;
    private SimpleViewLoadingControl mDecodeAutoStatusLoadingControl;
    private LoadingControl mMatchingByTimeStatusLoadingControl;
    private LoadingControl mScanResultProcessingLoadingControl;
    private LoadingControl mIgnoringLoadingControl;
    private LoadingControl mDeletingLoadingControl;
    private LoadingControl mPrepareUploadingLoadingControl;
    View mClearCacheStatusLine;
    DecodeImageTask mDecodeImageTask;
    ProcessScanResultsTask mProcessScanResultTask;
    MatchingByTimeTask mMatchingByTimeTask;
    MatchingByTimeCheckConditionTask mMatchingByTimeCheckConditionTask;
    IgnoringTask mIgnoringTask;
    DeleteAllTask mDeleteAllTask;
    UploadTask mUploadTask;
    private SingleFrequencySoundGenerator mCurrentBeep;

    private CurrentDataInfo mLastCurrentData;

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

            this.setTitle("mVentory: Home");
            getActionBar().setSubtitle(versionName);
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
        mButtonDefaultTextColor = errorReportingButton.getCurrentTextColor();

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
                                retryFailedButton.setTextColor(mButtonDefaultTextColor);
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
                            errorReportingButton.setTextColor(mButtonDefaultTextColor);
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

        boolean refreshPressed = extras != null ? extras.getBoolean(RERFRESH_PRESSED, false)
                : false;
        initThumbs(refreshPressed);
        diskCacheClearedReceiver = ImageCacheUtils
                .getAndRegisterOnDiskCacheClearedBroadcastReceiver(TAG, this);
        mDecodeStatusLineText = (TextView) findViewById(R.id.decodeStatusLineText);
        mDecodeStatusLoadingControl = new SimpleViewLoadingControl(
                findViewById(R.id.decodeStatusLine));
        mDecodeAutoStatusLineText = (TextView) findViewById(R.id.decodeAutoStatusLineText);
        mDecodeAutoStatusLoadingControl = new SimpleViewLoadingControl(
                findViewById(R.id.decodeAutoStatusLine));
        mMatchingByTimeStatusLoadingControl = new SimpleViewLoadingControl(
                findViewById(R.id.matchingStatusLine));
        mScanResultProcessingLoadingControl = new SimpleViewLoadingControl(
                findViewById(R.id.processingScanResultStatusLine));
        mIgnoringLoadingControl = new SimpleViewLoadingControl(findViewById(R.id.ignoreStatusLine));
        mDeletingLoadingControl = new SimpleViewLoadingControl(findViewById(R.id.deleteStatusLine));
        mPrepareUploadingLoadingControl = new SimpleViewLoadingControl(
                findViewById(R.id.prepareUploadingStatusLine));
        mClearCacheStatusLine = findViewById(R.id.clearCacheStatusLine);
        EventBusUtils.registerOnGeneralEventBroadcastReceiver(TAG, this, this);
        initHelp();
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
        updateClearCacheStatus();
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
            myIntent.putExtra(RERFRESH_PRESSED, true);
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

    void initThumbs(boolean refreshPressed)
    {
        final ScrollView scroll = (ScrollView) findViewById(R.id.scroll);
        thumbnailsList = (HorizontalListViewExt) findViewById(R.id.thumbs);
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
        thumbnailsList.setOnUpListener(new OnUpListener() {
            @Override
            public void onUp(MotionEvent e) {
                CommonUtils.debug(TAG, "thumbnailsList: onUp");
                scroll.requestDisallowInterceptTouchEvent(false);
            }
        });
        thumbnailsList.setOn2FingersDownListener(new On2FingersDownListener() {

            @Override
            public void on2FingersDown(MotionEvent ev) {
                View[] views = new View[2];
                PointF[] rawPoints = new PointF[2];
                for (int i = 0; i < rawPoints.length; i++) {
                    rawPoints[i] = getRawCoordinates(ev, i, thumbnailsList);
                }
                for (int i = 0, size = thumbnailsList.getChildCount(); i < size; i++) {
                    if (views[0] != null && views[1] != null) {
                        break;
                    }
                    View child = thumbnailsList.getChildAt(i);
                    ThumbnailsAdapter.GroupViewHolder gvh = (ThumbnailsAdapter.GroupViewHolder) child
                            .getTag();
                    processGroupViewHolder(gvh, views, rawPoints);
                }
                if (views[0] != null && views[1] != null) {
                    CurrentDataInfo data1 = (CurrentDataInfo) ((ItemViewHolder) views[0]
                            .getTag()).containerRoot.getTag();
                    CurrentDataInfo data2 = (CurrentDataInfo) ((ItemViewHolder) views[1]
                            .getTag()).containerRoot.getTag();

                    ImageDataGroup idg1 = data1.dataSnapshot.get(data1.groupPosition);
                    ImageDataGroup idg2 = data2.dataSnapshot.get(data2.groupPosition);
                    ImageDataGroup source = null;
                    CurrentDataInfo target = null;
                    if (TextUtils.isEmpty(idg1.sku)) {
                        target = data1;
                    } else {
                        source = idg1;
                    }
                    if (TextUtils.isEmpty(idg2.sku)) {
                        target = data2;
                    } else {
                        source = idg2;
                    }
                    if (source != null && target != null) {
                        if (!checkModifierTasksActive()) {
                            return;
                        }
                        mLastCurrentData = thumbnailsAdapter.currentData;
                        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

                        alert.setMessage(R.string.main_same_as_selected_question);

                        final File file = target.imageData.getFile();
                        final String sku = source.sku;
                        alert.setPositiveButton(R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        File newFile = ImagesLoader.queueImage(file, sku, true,
                                                false);
                                        if (newFile.exists()) {
                                        } else {
                                            GuiUtils.alert(R.string.errorCantRenameFile);
                                        }
                                    }
                                });

                        alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Canceled.
                            }
                        });

                        alert.show();
                    }
                }
            }

            void processGroupViewHolder(ThumbnailsAdapter.GroupViewHolder gvh, View[] views,
                    PointF[] rawPoints) {
                for (int i = 0, size = gvh.images.getChildCount(); i < size; i++) {
                    View child = gvh.images.getChildAt(i);
                    for (int pointerIndex = 0; pointerIndex < rawPoints.length; pointerIndex++) {
                        PointF p = rawPoints[pointerIndex];
                        if (isPointWithinView(p, child)) {
                            CommonUtils
                                    .debug(TAG,
                                            "On2FingersDownListener.processGroupViewHolder: found view at index %1$d for pointer %2$d",
                                            i, pointerIndex);
                            views[pointerIndex] = child;
                        }
                    }
                    if (views[0] != null && views[1] != null) {
                        break;
                    }
                }
            }

            PointF getRawCoordinates(MotionEvent event, int pointerIndex, View v) {
                float rawX, rawY;
                final int location[] = {
                        0, 0
                };
                v.getLocationOnScreen(location);
                rawX = (int) event.getX(pointerIndex) + location[0];
                rawY = (int) event.getY(pointerIndex) + location[1];
                return new PointF(rawX, rawY);

            }

            public boolean isPointWithinView(PointF p, View child) {
                Rect viewRect = new Rect();
                int[] childPosition = new int[2];
                child.getLocationOnScreen(childPosition);
                int left = childPosition[0];
                int right = left + child.getWidth();
                int top = childPosition[1];
                int bottom = top + child.getHeight();
                viewRect.set(left, top, right, bottom);
                return viewRect.contains((int) p.x, (int) p.y);
            }
        });
        initImageWorker();
        reloadThumbs(refreshPressed, false);
        restartObservation();
    }

    /**
     * Reload thumbs on the main page
     */
    private void reloadThumbs() {
        reloadThumbs(false, false);
    }

    /**
     * Reload thumbs on the main page
     * 
     * @param refreshPressed
     * @param autoDetect
     */
    private void reloadThumbs(boolean refreshPressed, boolean autoDetect) {
        CommonUtils.debug(TAG, "reloadThumbs: started");
        if (!isActivityAlive)
        {
            return;
        }
        if (loadThumbsTask != null) {
            loadThumbsTask.cancel(true);
        }
        if (isClearingCache()) {
            GuiUtils.alert(R.string.main_wait_cache_clear);
            return;
        }
        if (refreshPressed) {
            new ClearThumbCachesTask().executeOnExecutor(Executors.newSingleThreadExecutor());
            updateClearCacheStatus();
        } else {
            loadThumbsTask = new LoadThumbsTask(autoDetect);
            loadThumbsTask.execute();
        }
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

        mImageWorker.setImageCache(ImageCache
                .findOrCreateCache(this, THUMBS_CACHE_PATH, 1500, true, false));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.container_root) {
            MenuInflater inflater = getMenuInflater();
            if (thumbnailsAdapter.longClicked) {
                inflater.inflate(R.menu.main_thumb_long, menu);
            } else {
                inflater.inflate(R.menu.main_thumb, menu);
                mLastCurrentData = thumbnailsAdapter.currentData;
                boolean sameAsPreviousVisible = false;
                if (mLastCurrentData.groupPosition != 0 && mLastCurrentData.inGroupPosition == 0) {
                    ImageDataGroup idg = mLastCurrentData.dataSnapshot
                            .get(mLastCurrentData.groupPosition);
                    ImageDataGroup idgSource = mLastCurrentData.dataSnapshot
                            .get(mLastCurrentData.groupPosition - 1);
                    if (TextUtils.isEmpty(idg.sku) && !TextUtils.isEmpty(idgSource.sku)) {
                        sameAsPreviousVisible = true;
                    }
                }
                menu.findItem(R.id.menu_same_as_previous).setVisible(sameAsPreviousVisible);
            }
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
                mDecodeImageTask = new DecodeImageTask(thumbnailsAdapter.currentData.imageData
                        .getFile().getAbsolutePath(), null, false);
                mDecodeImageTask.execute();
                return true;
            case R.id.menu_decode_all:
                if (!checkModifierTasksActive()) {
                    return false;
                }
                mDecodeImageTask = new DecodeImageTask(thumbnailsAdapter.currentData.imageData
                        .getFile().getAbsolutePath(), null, true);
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
                mIgnoringTask = new IgnoringTask(thumbnailsAdapter.currentData.imageData.getFile()
                        .getAbsolutePath(), false);
                mIgnoringTask.execute();
                return true;
            case R.id.menu_ignore_all_from_now:
                if (!checkModifierTasksActive()) {
                    return false;
                }
                mIgnoringTask = new IgnoringTask(thumbnailsAdapter.currentData.imageData.getFile()
                        .getAbsolutePath(), true);
                mIgnoringTask.execute();
                return true;
            case R.id.menu_view_and_edit: {
                mLastCurrentData = thumbnailsAdapter.currentData;
                Intent intent = new Intent(this, PhotoViewActivity.class);
                intent.putExtra(PhotoViewActivity.EXTRA_SOURCE,
                        PhotoViewActivity.Source.MAIN.toString());
                intent.putExtra(PhotoViewActivity.EXTRA_PATH,
                        thumbnailsAdapter.currentData.imageData.file
                        .getAbsolutePath());
                startActivity(intent);
            }
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
                        boolean success = false;
                        CommonUtils.debug(TAG, "deleting image file %1$s",
                                mLastCurrentData.imageData.file.getAbsolutePath());
                        success = mLastCurrentData.imageData.getFile().delete()
                                || !mLastCurrentData.imageData.getFile().exists();

                        if (success) {
                        } else {
                            GuiUtils.alert(R.string.errorCantRemoveFile);
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
            case R.id.menu_delete_all: {
                if (!checkModifierTasksActive()) {
                    return false;
                }
                mLastCurrentData = thumbnailsAdapter.currentData;
                final List<File> filesToRemove = new ArrayList<File>();
                List<ImageDataGroup> data = ((ThumbsImageWorkerAdapter) mImageWorker.getAdapter()).data;
                synchronized (data) {
                    for (ImageDataGroup idg : data) {
                        for (ImageData id : idg.data) {
                            filesToRemove.add(id.getFile());
                        }
                    }
                }
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setMessage(R.string.main_delete_all_confirmation);

                alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (!checkModifierTasksActive()) {
                            return;
                        }
                        mDeleteAllTask = new DeleteAllTask(filesToRemove);
                        mDeleteAllTask.execute();
                    }
                });

                alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
                return true;
            }
            case R.id.menu_same_as_previous: {
                if (!checkModifierTasksActive()) {
                    return false;
                }
                CommonUtils.debug(TAG, "same as previous called for file %1$s",
                        mLastCurrentData.imageData.file.getAbsolutePath());

                ImageDataGroup idgSource = mLastCurrentData.dataSnapshot
                        .get(mLastCurrentData.groupPosition - 1);
                File newFile = ImagesLoader.queueImage(mLastCurrentData.imageData.getFile(),
                        idgSource.sku, true, false);
                if (newFile.exists()) {
                } else {
                    GuiUtils.alert(R.string.errorCantRenameFile);
                }
                return true;
            }
            case R.id.menu_match:
                if (!checkModifierTasksActive())
                {
                    return false;
                }
                mMatchingByTimeCheckConditionTask = new MatchingByTimeCheckConditionTask(
                        thumbnailsAdapter.currentData.imageData.getFile().getAbsolutePath());
                mMatchingByTimeCheckConditionTask.execute();
                return true;
            case R.id.menu_match_with_shift: {
                if (!checkModifierTasksActive()) {
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
                            mMatchingByTimeTask = new MatchingByTimeTask(mLastCurrentData.imageData
                                    .getFile().getAbsolutePath(), number == null ? 0 : number
                                    .intValue());
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
            case R.id.menu_display_sync: {
                Intent intent = new Intent(this, CameraTimeSyncActivity.class);
                startActivity(intent);
            }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public boolean checkModifierTasksActive() {
        if (mMatchingByTimeTask != null || mMatchingByTimeCheckConditionTask != null) {
            GuiUtils.alert(R.string.main_matching_by_time_please_wait);
            return false;
        }
        if (mDecodeImageTask != null) {
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
        if (mDeleteAllTask != null) {
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
        mCurrentBeep = playSuccessfulBeep(mCurrentBeep);
    }

    private void playFailureBeep() {
        mCurrentBeep = playFailureBeep(mCurrentBeep);
    }

    public static SingleFrequencySoundGenerator playSuccessfulBeep(
            SingleFrequencySoundGenerator beep) {
        if (beep != null) {
            beep.stopSound();
        }

        beep = new SingleFrequencySoundGenerator(1500, 200);
        beep.playSound();
        return beep;
    }

    public static SingleFrequencySoundGenerator playFailureBeep(SingleFrequencySoundGenerator beep) {
        if (beep != null) {
            beep.stopSound();
        }

        beep = new SingleFrequencySoundGenerator(700, 200, true);
        beep.playSound();
        return beep;
    }

    public static SingleFrequencySoundGenerator checkConditionAndSetCameraTimeDifference(
            String code, long exifDateTime, Settings settings, SingleFrequencySoundGenerator beep,
            boolean silent, Runnable runOnSuccess) {
        SingleFrequencySoundGenerator result = null;
        if (exifDateTime != -1) {
            try {
                Date phoneDate = CommonUtils.parseDateTime(code
                        .substring(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX.length()));
                result = playSuccessfulBeep(beep);

                int timeDifference = (int) ((phoneDate.getTime() - exifDateTime) / 1000);

                settings.setCameraTimeDifference(timeDifference, phoneDate);
                if (!silent) {
                    GuiUtils.alert(R.string.main_decoding_camera_success, timeDifference);
                }
                if (runOnSuccess != null) {
                    runOnSuccess.run();
                }
            } catch (Exception ex) {
                if (!silent) {
                    GuiUtils.error(TAG, R.string.main_decoding_failed_exif_date, ex);
                }
            }
        } else {
            if (!silent) {
                GuiUtils.alert(R.string.main_decoding_failed_exif_date);
            }
        }
        return result;
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
        final List<File> filesToUpload = new ArrayList<File>();
        synchronized (snapshot) {
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
                    filesToUpload.add(id.getFile());
                }
                int value = skuPhotoCounters.containsKey(idg.sku) ? skuPhotoCounters.get(idg.sku)
                        : 0;
                value += count;
                skuPhotoCounters.put(idg.sku, value);
                if (count > uploadMinRangeStart) {
                    if (uploadMaxRangeStart == uploadMinRangeStart) {
                        productsInRangeCount++;
                    } else if (count < uploadMaxRangeStart) {
                        productsInRangeCount++;
                    }
                }
                photosCount += count;
            }
        }
        if (photosCount == 0) {
            GuiUtils.alert(R.string.main_upload_no_items);
        } else {
            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

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
                    mUploadTask = new UploadTask(filesToUpload);
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
                    mProcessScanResultTask = new ProcessScanResultsTask(mLastCurrentData.imageData
                            .getFile().getAbsolutePath(), sku);
                    mProcessScanResultTask.execute();
                }
            }
        }

    }

    boolean isClearingCache() {
        return ClearThumbCachesTask.isActive();
    }

    void updateClearCacheStatus() {
        if (isActivityResumed()) {
            mClearCacheStatusLine.setVisibility(isClearingCache() ? View.VISIBLE : View.GONE);
        }
    }

    public boolean isCurrentCachePath(String path) {
        return THUMBS_CACHE_PATH.equals(path);
    }

    @Override
    public void onGeneralBroadcastEvent(EventType eventType, Intent extra) {
        switch (eventType) {
            case MAIN_THUMB_CACHE_CLEARED:
                updateClearCacheStatus();
                String path = extra.getStringExtra(EventBusUtils.PATH);
                if (path != null && mImageWorker != null && isCurrentCachePath(path)) {
                    mImageWorker.getImageCache().clearCaches(true);
                }
                if (!isClearingCache()) {
                    reloadThumbs();
                }
                break;
            case MAIN_THUMB_CACHE_CLEAR_FAILED:
                updateClearCacheStatus();
                break;
            case DECODE_RESULT:
                String contents = extra.getStringExtra(EventBusUtils.CODE);
                if (contents != null) {
                    if (!checkModifierTasksActive()) {
                        return;
                    }
                    String[] urlData = contents.split("/");
                    String sku = urlData[urlData.length - 1];
                    mDecodeImageTask = new DecodeImageTask(mLastCurrentData.imageData.getFile()
                            .getAbsolutePath(), sku, false);
                    mDecodeImageTask.execute();
                }
                break;
            default:
                break;
        }
    }
    
    private void removeDataGroup(CurrentDataInfo targetDataInfo) {
        if (targetDataInfo.groupPosition != 0
                && targetDataInfo.groupPosition != targetDataInfo.dataSnapshot.size() - 1) {
            ImageDataGroup previous = targetDataInfo.dataSnapshot
                    .get(targetDataInfo.groupPosition - 1);
            ImageDataGroup next = targetDataInfo.dataSnapshot.get(targetDataInfo.groupPosition + 1);
            if (TextUtils.equals(previous.sku, next.sku)) {
                previous.data.addAll(next.data);
                previous.modified = true;
                targetDataInfo.dataSnapshot.remove(targetDataInfo.groupPosition + 1);
            }
        }
        targetDataInfo.dataSnapshot.remove(targetDataInfo.groupPosition);
    }

    /**
     * Called by ImagesObserver to modify datamodel accordingly to file system
     * changes
     * 
     * @param fileName
     */
    private void fileRemoved(String fileName) {
        CommonUtils.debug(TAG, "fileRemoved: called for file %1$s", fileName);
        if (thumbnailsAdapter == null) {
            CommonUtils.debug(TAG, "fileRemoved: thumbnailsAdapter is null, returning");
            return;
        }
        List<ImageDataGroup> data = ((ThumbsImageWorkerAdapter) mImageWorker.getAdapter()).data;
        synchronized (data) {
            List<CurrentDataInfo> dataInfo = buildDataInfo(data);
            ImageData imageDataToRemove = new ImageData(new File(fileName), 0, 0);
            CurrentDataInfo currentDataToRemove = new CurrentDataInfo(imageDataToRemove, 0, 0, data);
            int ix = Collections.binarySearch(dataInfo, currentDataToRemove,
                    sCurrentDataInfoComparator);
            if (ix >= 0) {
                currentDataToRemove = dataInfo.get(ix);
                ImageDataGroup idg = data.get(currentDataToRemove.groupPosition);
                idg.data.remove(currentDataToRemove.inGroupPosition);
                if (idg.data.size() == 0) {
                    removeDataGroup(currentDataToRemove);
                } else {
                    idg.modified = true;
                }
                thumbnailsAdapter.notifyDataSetChanged();
                mUploadButton.setEnabled(data.size() > 0);
            }
        }
    }

    /**
     * Called by ImagesObserver to modify datamodel accordingly to file system
     * changes
     * 
     * @param fileName
     */
    private void fileAdded(String fileName) {
        try {
            CommonUtils.debug(TAG, "fileAdded: called for file %1$s", fileName);
            File file = new File(fileName);
            if (!fileName.toLowerCase().contains(".jpg") || file.isDirectory() || !file.exists()) {
                CommonUtils.debug(TAG, "fileAdded: file %1$s is not accepted, returning", fileName);
                return;
            }
            List<ImageDataGroup> data = ((ThumbsImageWorkerAdapter) mImageWorker.getAdapter()).data;
            synchronized (data) {
                List<CurrentDataInfo> dataInfo = buildDataInfo(data);
                ImageData imageDataToAdd = ImageData.getImageDataForFile(file, true);
                CurrentDataInfo currentDataToAdd = new CurrentDataInfo(imageDataToAdd, 0, 0, data);
                int ix = Collections.binarySearch(dataInfo, currentDataToAdd,
                        sCurrentDataInfoComparator);
                if (ix > 0) {
                    CurrentDataInfo cdi = dataInfo.get(ix);
                    if (cdi.imageData.getFile().getAbsolutePath().equals(fileName)) {
                        CommonUtils
                                .debug(TAG,
                                        "fileAdded: file %1$s already present in list, returning",
                                        fileName);
                        return;
                    }
                    ix = -ix - 2;
                }
                if (ix < 0) {
                    String sku = ImagesLoader.getSkuFromFileName(file.getName());
                    boolean scheduleScan = false;
                    if (sku == null) {
                        ScanState scanState = ScanUtils.getScanStateForFileName(imageDataToAdd.file
                                .getName());
                        imageDataToAdd.setScanState(scanState);
                        if (scanState == ScanState.NOT_SCANNED) {
                            // schedule scan
                            scheduleScan = true;
                        }
                    } else {
                        boolean isDecodedCode = ImagesLoader.isDecodedCode(imageDataToAdd.getFile()
                                .getName());
                        if (isDecodedCode) {
                            imageDataToAdd.setScanState(ScanState.SCANNED_DECODED);
                        }
                    }
                    CurrentDataInfo previousItem = ix < -1 ? dataInfo.get(-ix - 2) : null;
                    CurrentDataInfo nextItem = ix == (-dataInfo.size() - 1) ? null : dataInfo
                            .get(-ix - 1);
                    ImageDataGroup prevDataGroup = previousItem == null ? null : data
                            .get(previousItem.groupPosition);
                    ImageDataGroup nextDataGroup = nextItem == null ? null : data
                            .get(nextItem.groupPosition);
                    if (prevDataGroup != null && TextUtils.equals(sku, prevDataGroup.sku)) {
                        prevDataGroup.data.add(previousItem.inGroupPosition + 1, imageDataToAdd);
                        prevDataGroup.modified = true;
                        currentDataToAdd.groupPosition = previousItem.groupPosition;
                        currentDataToAdd.inGroupPosition = previousItem.inGroupPosition + 1;
                    } else if (nextDataGroup != null && TextUtils.equals(sku, nextDataGroup.sku)) {
                        nextDataGroup.data.add(nextItem.inGroupPosition, imageDataToAdd);
                        nextDataGroup.modified = true;
                        currentDataToAdd.groupPosition = nextItem.groupPosition;
                        currentDataToAdd.inGroupPosition = nextItem.inGroupPosition;
                    } else {
                        ImageDataGroup idgToAdd = new ImageDataGroup();
                        if (TextUtils.isEmpty(sku)) {
                            idgToAdd.cached = true;
                        }
                        idgToAdd.sku = sku;
                        ix = 0;
                        if (previousItem != null) {
                            ix = previousItem.groupPosition + 1;
                        } else if (nextItem != null) {
                            ix = nextItem.groupPosition;
                        }
                        idgToAdd.data.add(imageDataToAdd);
                        data.add(ix, idgToAdd);
                        currentDataToAdd.groupPosition = ix;
                        currentDataToAdd.inGroupPosition = 0;
                        if (prevDataGroup == nextDataGroup && prevDataGroup != null) {
                            ImageDataGroup idg2 = new ImageDataGroup();
                            prevDataGroup.copyInfoShort(idg2);
                            idg2.modified = true;
                            for (int i = prevDataGroup.data.size() - 1; i > previousItem.inGroupPosition; i--) {
                                idg2.data.add(0, prevDataGroup.data.remove(i));
                            }
                            data.add(ix + 1, idg2);
                        }
                    }
                    final int scrollTo;
                    if (thumbnailsAdapter == null
                            || thumbnailsList.getStartX() == thumbnailsList.getMaxX()) {
                        scrollTo = Integer.MAX_VALUE;
                    } else {
                        scrollTo = -1;
                    }
                    thumbnailsAdapter.notifyDataSetChanged();
                    if (scrollTo > 0) {
                        GuiUtils.post(new Runnable() {

                            @Override
                            public void run() {
                                thumbnailsList.scrollTo(scrollTo);
                            }
                        });
                    }
                    mUploadButton.setEnabled(data.size() > 0);
                    if (scheduleScan) {
                        AutoDecodeImageTask task = new AutoDecodeImageTask(fileName);
                        task.executeOnExecutor(sAutoDecodeExecutor);
                    }
                }
            }
        } catch (Exception ex) {
            GuiUtils.noAlertError(TAG, ex);
        }
    }

    private List<CurrentDataInfo> buildDataInfo(List<ImageDataGroup> data) {
        List<CurrentDataInfo> dataInfo = new ArrayList<CurrentDataInfo>();
        for (int groupPosition = 0, groupsCount = data.size(); groupPosition < groupsCount; groupPosition++) {
            ImageDataGroup idg = data.get(groupPosition);
            for (int inGroupPosition = 0, inGroupCount = idg.data.size(); inGroupPosition < inGroupCount; inGroupPosition++) {
                ImageData id = idg.data.get(inGroupPosition);
                dataInfo.add(new CurrentDataInfo(id, groupPosition, inGroupPosition, data));
            }
        }
        return dataInfo;
    }

    private DataSnapshot getDataSnapshot() {
        List<ImageDataGroup> data = ((ThumbsImageWorkerAdapter) mImageWorker.getAdapter()).data;
        List<List<ImageData>> imageDataList = new ArrayList<List<ImageData>>();
        List<ImageDataGroup> dataSnapshot;
        synchronized (data) {
            dataSnapshot = new ArrayList<MainActivity.ImageDataGroup>(data);
            for (ImageDataGroup idg : dataSnapshot) {
                imageDataList.add(new ArrayList<ImageData>(idg.data));
            }
        }
        return new DataSnapshot(imageDataList, dataSnapshot);
    }

    void initHelp() {
        ListView mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                new String[] {
                        "link1", "link2", "link3", "link4", "link5", "link6", "link7", "link8",
                }));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("http://www.mventory.com"));
                startActivity(i);
            }

        });
    }
    static class DataSnapshot {
        List<List<ImageData>> imageDataList;
        List<ImageDataGroup> dataSnapshot;

        public DataSnapshot(List<List<ImageData>> imageDataList, List<ImageDataGroup> dataSnapshot) {
            super();
            this.imageDataList = imageDataList;
            this.dataSnapshot = dataSnapshot;
        }

        CurrentDataInfo getCurrentDataInfoForFileName(String fileName) {
            return getCurrentDataInfoForFileName(fileName, this);
        }

        static CurrentDataInfo getCurrentDataInfoForFileName(String fileName, DataSnapshot snapshot) {
            CurrentDataInfo cdi = null;
            for (int groupPosition = 0, groupsCount = snapshot.dataSnapshot.size(); groupPosition < groupsCount; groupPosition++) {
                List<ImageData> ids = snapshot.imageDataList.get(groupPosition);
                for (int inGroupPosition = 0, inGroupCount = ids.size(); inGroupPosition < inGroupCount; inGroupPosition++) {
                    ImageData id = ids.get(inGroupPosition);
                    if (id.file.getAbsolutePath().equals(fileName)) {
                        cdi = new CurrentDataInfo(id, groupPosition, inGroupPosition,
                                snapshot.dataSnapshot);
                        break;
                    }
                }
                if (cdi != null) {
                    break;
                }
            }
            return cdi;
        }
    }

    class AutoDecodeImageTask extends DataModifierTask {
        int mScreenLargerDimension;
        boolean breakExecution = false;
        String mFileName;

        public AutoDecodeImageTask(String fileName) {
            super(mDecodeAutoStatusLoadingControl);
            this.mFileName = fileName;
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mScreenLargerDimension = metrics.widthPixels;
            if (mScreenLargerDimension < metrics.heightPixels) {
                mScreenLargerDimension = metrics.heightPixels;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDecodeAutoStatusLineText.setText(CommonUtils.getStringResource(
                    R.string.main_autodecoding_status,
                    mDecodeAutoStatusLoadingControl.getLoadersCount()));
        }

        @Override
        void onFinish() {
            super.onFinish();
            mDecodeAutoStatusLineText.setText(CommonUtils.getStringResource(
                    R.string.main_autodecoding_status,
                    mDecodeAutoStatusLoadingControl.getLoadersCount()));
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled() || !isActivityAlive) {
                    return false;
                }
                ZXingCodeScanner multiDetector = new ZXingCodeScanner();
                boolean cameraSyncCode = false;
                File file = new File(mFileName);
                if (!file.exists()) {
                    CommonUtils
                            .debug(TAG,
                                    "AutoDecodeImageTask.doInBackground: file %1$s is not found, returning",
                                    mFileName);
                    return false;
                }
                ImageData id = ImageData.getImageDataForFile(file, false);
                DetectDecodeResult ddr = multiDetector.detectDecodeMultiStep(
                        id.file.getAbsolutePath(), mScreenLargerDimension);
                ScanState scanState = null;
                String sku = null;
                ImageData lastDecodedData = null;
                if (ddr.isDecoded()) {
                    scanState = ScanState.SCANNED_DECODED;
                    String[] urlData = ddr.getCode().split("/");
                    sku = URLEncoder.encode(urlData[urlData.length - 1], "UTF-8");
                    lastDecodedData = id;
                } else {
                    {
                        scanState = ScanState.SCANNED_NOT_DETECTED;
                    }
                }
                String filePath = id.getFile().getAbsolutePath();
                File newFile = new File(ScanUtils.setScanStateForFileName(filePath, scanState));
                id.setScanState(scanState);
                DataSnapshot ds = getDataSnapshot();
                CurrentDataInfo cdi = ds.getCurrentDataInfoForFileName(filePath);
                if (cdi != null) {
                    if (id.getFile().renameTo(newFile)) {
                        if (TextUtils.isEmpty(sku)) {
                            if (cdi.groupPosition > 0) {
                                boolean getSkuFromPreviousGroup = true;
                                if (cdi.inGroupPosition != 0) {
                                    List<ImageData> ids = ds.imageDataList.get(cdi.groupPosition);
                                    for (int i = cdi.inGroupPosition - 1; i >= 0; i--) {
                                        if (ids.get(i).getScanState() != ScanState.NOT_SCANNED) {
                                            getSkuFromPreviousGroup = false;
                                            break;
                                        }
                                    }
                                }
                                if (getSkuFromPreviousGroup) {
                                    ImageDataGroup previousGroup = cdi.dataSnapshot
                                            .get(cdi.groupPosition - 1);
                                    for (ImageData id2 : previousGroup.data) {
                                        if (id2.getScanState() == ScanState.SCANNED_DECODED) {
                                            ImagesLoader.queueImage(newFile, previousGroup.sku,
                                                    true, false);
                                        }
                                    }
                                }
                            }
                        } else {

                            cameraSyncCode = sku != null
                                    && sku.startsWith(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX);
                            if (sku != null && !cameraSyncCode) {
                                boolean discardLater = true;
                                if (cdi.groupPosition > 0) {
                                    discardLater = false;
                                    ImageDataGroup idg = cdi.dataSnapshot.get(cdi.groupPosition);
                                    List<ImageData> imagesData = ds.imageDataList
                                            .get(cdi.groupPosition);
                                    for (int i = cdi.inGroupPosition - 1; i >= 0; i--) {
                                        if (imagesData.get(i).getScanState() != ScanState.NOT_SCANNED) {
                                            discardLater = true;
                                            break;
                                        }
                                    }
                                    if (!discardLater) {
                                        idg = cdi.dataSnapshot.get(cdi.groupPosition - 1);
                                        discardLater = !TextUtils.equals(idg.sku, sku);
                                    }
                                }
                                newFile = ImagesLoader.queueImage(newFile, sku, true, discardLater);
                            }
                            if (cameraSyncCode) {
                                lastDecodedData = null;
                            }
                            if (lastDecodedData != null) {
                                for (int i = cdi.groupPosition, size = cdi.dataSnapshot.size(); i < size; i++) {
                                    ImageDataGroup idg = cdi.dataSnapshot.get(i);
                                    int startPos = i == cdi.groupPosition ? cdi.inGroupPosition + 1
                                            : 0;
                                    if (!processImageDataGroup2(idg, ds.imageDataList.get(i),
                                            startPos, multiDetector, sku)) {
                                        return false;
                                    }
                                    if (breakExecution) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                return !isCancelled();
            } catch (Exception e) {
                GuiUtils.noAlertError(TAG, e);
            }
            return false;
        }

        private boolean processImageDataGroup2(ImageDataGroup idg, List<ImageData> data,
                int startPos, ZXingCodeScanner scanner, String newSku) throws IOException {
            for (int i = startPos, size2 = data.size(); i < size2; i++) {
                ImageData id = data.get(i);
                String sku = idg.sku;
                ScanState scanState = id.getScanState();
                if (scanState == null) {
                    scanState = ScanUtils.getScanStateForFileName(id.file.getName());
                    id.setScanState(scanState);
                }
                if (scanState == ScanState.SCANNED_DECODED
                        || scanState == ScanState.SCANNED_DETECTED_NOT_DECODED
                        || TextUtils.equals(newSku, sku)) {
                    breakExecution = true;
                    break;
                }
                if (scanState == ScanState.NOT_SCANNED) {
                    continue;
                }
                if (TextUtils.isEmpty(newSku)) {
                    ImagesLoader.undoImage(id.getFile());
                } else {
                    ImagesLoader.queueImage(id.getFile(), newSku, true, false);
                }
                if (isCancelled()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        void nullifyTask() {
        }

        @Override
        protected void onFailedPostExecute() {
            super.onFailedPostExecute();
        }

        @Override
        protected void onSuccessPostExecute() {
            super.onSuccessPostExecute();
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
                // reloadThumbs();
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
        List<File> mFilesToUpload;

        public UploadTask(List<File> filesToUpload) {
            super(mPrepareUploadingLoadingControl);
            this.mFilesToUpload = filesToUpload;
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
                ArrayList<String> productCodeList = new ArrayList<String>();

                for (int i = 0, size = mFilesToUpload.size(); i < size; i++) {
                    if (isCancelled()) {
                        return false;
                    }
                    File f = mFilesToUpload.get(i);
                    String sku = ImagesLoader.getSkuFromFileName(f.getName());
                    if (TextUtils.isEmpty(sku)) {
                        continue;
                    }
                    if (processFile(f, sku, destinationDir)) {
                        if (!productCodeList.contains(sku)) {
                            productCodeList.add(sku);
                        }
                    }
                }
                for (int i = 0; i < productCodeList.size(); i++) {
                    ExternalImagesJob ejob = new ExternalImagesJob(System.currentTimeMillis(),
                            productCodeList.get(i), null, settings.getProfileID(), 0);

                    mJobControlInterface.addExternalImagesJob(ejob);
                }
                DiskLruCache.clearCaches(MyApplication.getContext(), THUMBS_CACHE_PATH);
                ImageCacheUtils.sendDiskCacheClearedBroadcast();
                return !isCancelled();
            } catch (Exception e) {
                GuiUtils.noAlertError(TAG, e);
            }
            return false;
        }

        boolean processFile(File file, String sku, File destinationDir) {
            boolean result = false;
            if (!file.exists()) {
                CommonUtils.debug(TAG, "UploadTask.processFile: file %1$s doesn't exist",
                        file.getAbsolutePath());
                Log.d(TAG,
                        CommonUtils.format("UploadTask.processFile: file %1$s doesn't exist",
                                file.getAbsolutePath()));
                return result;
            }
            String fileName = file.getName();
            fileName = ScanUtils.setScanStateForFileName(fileName, ScanState.NOT_SCANNED);
            File destinationFile = new File(destinationDir, fileName);
            incModifiersIfNecessary();
            boolean renameSuccessful = file.renameTo(destinationFile);

            if (renameSuccessful) {
                result = true;
            } else {
                CommonUtils.debug(TAG,
                        "UploadTask.processFile: Unable to rename file from %1$s to %2$s",
                        file.getAbsolutePath(), destinationFile.getAbsolutePath());
                Log.d(TAG, "Unable to rename file from: " + file.getAbsolutePath() + " to "
                        + destinationFile.getAbsolutePath());
            }

            return result;
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
    
    class DeleteAllTask extends DataModifierTask {
        List<File> mFilesToRemove;

        public DeleteAllTask(List<File> filesToRemove) {
            super(mDeletingLoadingControl);
            this.mFilesToRemove = filesToRemove;
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                for (int i = 0, size = mFilesToRemove.size(); i < size; i++) {
                    if (isCancelled()) {
                        return false;
                    }
                    File file = mFilesToRemove.get(i);
                    CommonUtils.debug(TAG, "DeleteAllTask.doInBackground: delete image file %1$s",
                            file.getAbsolutePath());
                    file.delete();
                }
                return !isCancelled();
            } catch (Exception e) {
                GuiUtils.error(TAG, R.string.errorCantRemoveFiles, e);
            }
            return false;
        }

        @Override
        void nullifyTask() {
            mDeleteAllTask = null;
        }

        @Override
        protected void onSuccessPostExecute() {
            super.onSuccessPostExecute();
            GuiUtils.alert(R.string.main_deleting_success);
        }
    }
    
    class IgnoringTask extends DataModifierTask {
        boolean mIgnoreAllStartingFrom;
        String mFileName;

        public IgnoringTask(String fileName, boolean ignoreAllStartingFrom) {
            super(mIgnoringLoadingControl);
            this.mFileName = fileName;
            this.mIgnoreAllStartingFrom = ignoreAllStartingFrom;
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled()) {
                    return false;
                }
                File file = new File(mFileName);
                if (!file.exists()) {
                    CommonUtils.debug(TAG,
                            "IgnoringTask.doInBackground: file %1$s is not found, returning",
                            mFileName);
                    return false;
                }

                if (mIgnoreAllStartingFrom) {
                    DataSnapshot ds = getDataSnapshot();
                    CurrentDataInfo cdi = ds.getCurrentDataInfoForFileName(mFileName);
                    if (cdi != null) {
                        for (int i = cdi.groupPosition, size = cdi.dataSnapshot.size(); i < size; i++) {
                            List<ImageData> imagesList = ds.imageDataList.get(i);
                            int startPos = i == cdi.groupPosition ? cdi.inGroupPosition : 0;

                            if (!processImageDataGroup(imagesList, startPos)) {
                                return false;
                            }
                        }
                    }
                } else {
                    incModifiersIfNecessary();
                    CommonUtils.debug(TAG, "IgnoringTask.doInBackground: undo image file %1$s",
                            file.getAbsolutePath());
                    ImagesLoader.undoImage(file);
                }
                return !isCancelled();
            } catch (Exception e) {
                GuiUtils.noAlertError(TAG, e);
            }
            return false;
        }

        private boolean processImageDataGroup(List<ImageData> data, int startPos) {
            for (int i = startPos, size2 = data.size(); i < size2; i++) {
                ImageData id = data.get(i);
                CommonUtils.debug(TAG, "IgnoringTask.processImageDataGroup: undo image file %1$s",
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
        String mSku;
        String mFileName;

        public ProcessScanResultsTask(String fileName, String sku) {
            super(mScanResultProcessingLoadingControl);
            this.mFileName = fileName;
            this.mSku = sku;
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled()) {
                    return false;
                }
                File file = new File(mFileName);
                if (!file.exists()) {
                    CommonUtils
                            .debug(TAG,
                                    "ProcessScanResultsTask.doInBackground: file %1$s is not found, returning",
                                    mFileName);
                    return false;
                }
                DataSnapshot ds = getDataSnapshot();
                CurrentDataInfo cdi = ds.getCurrentDataInfoForFileName(mFileName);
                if (cdi != null) {
                    ImageDataGroup idg = cdi.dataSnapshot.get(cdi.groupPosition);
                    String startingSku = idg.sku;
                    if (TextUtils.equals(startingSku, mSku)) {
                        return true;
                    }
                    int startPos;
                    startPos = cdi.groupPosition;
                    for (int i = startPos, size = cdi.dataSnapshot.size(); i < size; i++) {
                        idg = cdi.dataSnapshot.get(i);
                        startPos = i == cdi.groupPosition ? cdi.inGroupPosition : 0;
                        if (!TextUtils.equals(startingSku, idg.sku) && !TextUtils.isEmpty(idg.sku)) {
                            break;
                        }
                        List<ImageData> imagesList = ds.imageDataList.get(i);
                        if (!processImageDataGroup(imagesList, startPos)) {
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

        private boolean processImageDataGroup(List<ImageData> data, int startPos) {
            for (int i = startPos, size2 = data.size(); i < size2; i++) {
                ImageData id = data.get(i);
                CommonUtils
                        .debug(TAG,
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
        String mFileName;
        String mCode;
        boolean mSilent;
        long mExifDateTime = -1;
        boolean mDecodeAll;
        String mLastDecodedSku = null;
        int mSkusToDecodeCount = 0;
        int mSkusDecodingCount = 0;

        public DecodeImageTask(String fileName, String code, boolean decodeAll) {
            super(mDecodeStatusLoadingControl);
            mDecodeStatusLineText.setText(R.string.main_decoding_status);
            this.mFileName = fileName;
            this.mCode = code;
            mSilent = mCode != null;
            this.mDecodeAll = decodeAll;
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
                File file = new File(mFileName);
                if (!file.exists()) {
                    CommonUtils.debug(TAG,
                            "DecodeImageTask.doInBackground: file %1$s is not found, returning",
                            mFileName);
                    return false;
                }
                if (mDecodeAll) {
                    DataSnapshot ds = getDataSnapshot();
                    CurrentDataInfo cdi = ds.getCurrentDataInfoForFileName(mFileName);
                    if (cdi != null) {
                        if (!decodeAll(true, cdi, ds)) {
                            return false;
                        }
                        if (!decodeAll(false, cdi, ds)) {
                            return false;
                        }
                    }
                } else {
                    if (mCode == null) {
                        Bitmap bitmap = ImageUtils.decodeSampledBitmapFromFile(
                                file.getAbsolutePath(), mScreenLargerDimension,
                                mScreenLargerDimension);
                        ZXingCodeScanner multiDetector = new ZXingCodeScanner();
                        mCode = multiDetector.decode(bitmap);
                    }
                    if (mCode != null) {

                        if (mCode.startsWith(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX)) {
                            mExifDateTime = ImageUtils.getExifDateTime(file.getAbsolutePath());

                            if (mExifDateTime != -1) {
                                incModifiersIfNecessary();
                                file.delete();
                            }
                        } else {
                            String[] urlData = mCode.split("/");
                            String sku = urlData[urlData.length - 1];
                            DataSnapshot ds = getDataSnapshot();
                            CurrentDataInfo cdi = ds.getCurrentDataInfoForFileName(mFileName);
                            if (cdi != null) {
                                String startingSku = cdi.dataSnapshot.get(cdi.groupPosition).sku;
                                for (int i = cdi.groupPosition, size = cdi.dataSnapshot.size(); i < size; i++) {
                                    ImageDataGroup idg = cdi.dataSnapshot.get(i);
                                    if (!TextUtils.equals(startingSku, idg.sku)) {
                                        break;
                                    }
                                    int startPos = i == cdi.groupPosition ? cdi.inGroupPosition : 0;
                                    List<ImageData> imagesList = ds.imageDataList.get(i);
                                    if (!processImageDataGroup(sku, imagesList, startPos, i, cdi)) {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
                return !isCancelled();
            } catch (Exception e) {
                GuiUtils.noAlertError(TAG, e);
            }
            return false;
        }

        private boolean decodeAll(boolean countOnly, CurrentDataInfo cdi, DataSnapshot ds)
                throws IOException {
            String startingSku = cdi.dataSnapshot.get(cdi.groupPosition).sku;
            ZXingCodeScanner multiDetector = new ZXingCodeScanner();
            for (int i = cdi.groupPosition, size = cdi.dataSnapshot.size(); i < size; i++) {
                ImageDataGroup idg = cdi.dataSnapshot.get(i);
                if (!TextUtils.equals(startingSku, idg.sku) && !TextUtils.isEmpty(idg.sku)) {
                    break;
                }
                int startPos = i == cdi.groupPosition ? cdi.inGroupPosition : 0;
                List<ImageData> imagesList = ds.imageDataList.get(i);
                if (!processImageDataGroup2(idg, imagesList, startPos, multiDetector,
                        i == cdi.groupPosition, countOnly)) {
                    return false;
                }
            }
            return true;
        }

        private boolean processImageDataGroup(String sku, List<ImageData> data, int startPos,
                int groupPosition, CurrentDataInfo cdi) {
            for (int i = startPos, size2 = data.size(); i < size2; i++) {
                ImageData id = data.get(i);
                CommonUtils.debug(TAG,
                        "DecodeImageTask.processImageDataGroup: queuing file %1$s for sku %2$s",
                        id.file.getAbsolutePath(), sku);
                incModifiersIfNecessary();
                ImagesLoader.queueImage(id.file, sku, true, groupPosition == cdi.groupPosition
                        && i == startPos);
                if (isCancelled()) {
                    return false;
                }
            }
            return true;
        }

        private boolean processImageDataGroup2(ImageDataGroup idg, List<ImageData> data,
                int startPos, ZXingCodeScanner scanner, boolean firstGroup, boolean countOnly)
                throws IOException {
            boolean cameraSyncCode = false;
            for (int i = startPos, size2 = data.size(); i < size2; i++) {
                ImageData id = data.get(i);
                if (((firstGroup && i > startPos) || !firstGroup) && !TextUtils.isEmpty(idg.sku)) {
                    break;
                }
                if (!id.file.exists()) {
                    continue;
                }
                ScanState scanState = ScanUtils.getScanStateForFileName(id.file.getName());
                if (scanState == ScanState.NOT_SCANNED || scanState == ScanState.SCANNED_DECODED) {
                    if (countOnly) {
                        mSkusToDecodeCount++;
                        continue;
                    }
                    mSkusDecodingCount++;
                    GuiUtils.post(new Runnable() {

                        @Override
                        public void run() {
                            if (isActivityAlive) {
                                mDecodeStatusLineText.setText(CommonUtils.getStringResource(
                                        R.string.main_decoding_status2, mSkusDecodingCount,
                                        mSkusToDecodeCount));
                            }
                        }
                    });
                    String sku = null;
                    DetectDecodeResult ddr = scanner.detectDecodeMultiStep(
                            id.file.getAbsolutePath(), mScreenLargerDimension);
                    if (ddr.isDecoded()) {
                        scanState = ScanState.SCANNED_DECODED;
                        String[] urlData = ddr.getCode().split("/");
                        sku = URLEncoder.encode(urlData[urlData.length - 1], "UTF-8");
                        mLastDecodedSku = sku;
                    } else {
                        {
                            scanState = ScanState.SCANNED_NOT_DETECTED;
                        }
                    }
                    incModifiersIfNecessary();
                    String filePath = id.getFile().getAbsolutePath();
                    File newFile = new File(ScanUtils.setScanStateForFileName(filePath, scanState));
                    if (id.getFile().renameTo(newFile)) {
                        cameraSyncCode = sku != null
                                && sku.startsWith(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX);
                        if (sku != null && !cameraSyncCode) {
                            ImagesLoader.queueImage(newFile, sku, true, true);
                        }
                        if (cameraSyncCode) {
                            mLastDecodedSku = null;
                        }
                    }
                }
                if (scanState != ScanState.SCANNED_DECODED && mLastDecodedSku != null) {
                    CommonUtils
                            .debug(TAG,
                                    "DecodeImageTask.processImageDataGroup2: queuing file %1$s for sku %2$s",
                                    id.file.getAbsolutePath(), mLastDecodedSku);
                    incModifiersIfNecessary();
                    ImagesLoader.queueImage(id.file, mLastDecodedSku, true, false);
                }
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
        protected void onFailedPostExecute() {
            super.onFailedPostExecute();
            playFailureBeep();
        }

        @Override
        protected void onSuccessPostExecute() {
            super.onSuccessPostExecute();
            if (mDecodeAll) {
                GuiUtils.alert(R.string.main_decoding_Image_success);
            } else {
                if (mCode != null) {
                    if (mCode.startsWith(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX)) {
                        mCurrentBeep = checkConditionAndSetCameraTimeDifference(mCode,
                                mExifDateTime, settings, mCurrentBeep, mSilent, null);
                    } else {
                        if (!mSilent) {
                            playSuccessfulBeep();
                            GuiUtils.alert(R.string.main_decoding_Image_success);
                        }
                    }
                } else {
                    if (!mSilent) {
                        GuiUtils.alert(R.string.main_decoding_Image_failed);
                        playFailureBeep();
                    }
                }
            }
        }
    }

    class MatchingByTimeCheckConditionTask extends DataModifierTask {
        String mFileName;
        boolean mShowSyncRecommendation = false;
        static final long sHour = 60 * 60 * 1000;

        public MatchingByTimeCheckConditionTask(String fileName) {
            super(mMatchingByTimeStatusLoadingControl);
            this.mFileName = fileName;
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled()) {
                    return false;
                }
                File file = new File(mFileName);
                if (!file.exists()) {
                    CommonUtils
                            .debug(TAG,
                                    "MatchingByTimeCheckConditionTask.doInBackground: file %1$s is not found, returning",
                                    mFileName);
                    return false;
                }
                if (!settings.isCameraTimeDifferenceAssigned()) {
                    mShowSyncRecommendation = true;
                } else {
                    Date cameraLastSyncTime = settings.getCameraLastSyncTime();
                    long currentTime = System.currentTimeMillis();
                    long diff = Math.abs(currentTime - cameraLastSyncTime.getTime());
                    if (diff >= sHour * 5 * 24) {
                        CommonUtils
                                .debug(TAG,
                                        "MatchingByTimeCheckConditionTask.doInBackground: last camera sync was more than 5 days ago");
                        mShowSyncRecommendation = true;
                    } else if (diff >= sHour * 2 * 24) {
                        CommonUtils
                                .debug(TAG,
                                        "MatchingByTimeCheckConditionTask.doInBackground: last camera sync was more than 2 days ago");
                        if (!checkImagesWithinThresholdAvailable(4000)) {
                            return false;
                        }
                    } else if (diff >= sHour * 4) {
                        CommonUtils
                                .debug(TAG,
                                        "MatchingByTimeCheckConditionTask.doInBackground: last camera sync was more than 4 hours ago");
                        if (!checkImagesWithinThresholdAvailable(2000)) {
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

        boolean checkImagesWithinThresholdAvailable(long threshold) throws IOException,
                ParseException {
            synchronized (JobCacheManager.sSynchronizationObject) {
                DataSnapshot ds = getDataSnapshot();
                CurrentDataInfo cdi = ds.getCurrentDataInfoForFileName(mFileName);
                if (cdi != null) {
                    ArrayList<GalleryTimestampRange> galleryTimestampsRangesArray = JobCacheManager
                            .getGalleryTimestampRangesArray();

                    if (galleryTimestampsRangesArray != null) {
                        for (int i = cdi.groupPosition, size = cdi.dataSnapshot.size(); i < size; i++) {
                            ImageDataGroup idg = cdi.dataSnapshot.get(i);
                            if (!TextUtils.isEmpty(idg.sku) && i != cdi.groupPosition) {
                                break;
                            }
                            int startPos = i == cdi.groupPosition ? cdi.inGroupPosition : 0;
                            List<ImageData> imagesList = ds.imageDataList.get(i);
                            if (!processImageDataGroup(imagesList, startPos,
                                    galleryTimestampsRangesArray, threshold)) {
                                return false;
                            }
                            if (mShowSyncRecommendation) {
                                break;
                            }
                        }
                    }
                }
            }
            return true;
        }

        private boolean processImageDataGroup(List<ImageData> data, int startPos,
                ArrayList<GalleryTimestampRange> galleryTimestampsRangesArray, long threshold)
                throws IOException, ParseException {
            for (int i = data.size() - 1; i >= startPos; i--) {
                if (isCancelled()) {
                    return false;
                }
                ImageData id = data.get(i);
                if (!processImageData(id, galleryTimestampsRangesArray, threshold)) {
                    return false;
                }

                if (mShowSyncRecommendation) {
                    break;
                }
            }
            return true;
        }

        private boolean processImageData(ImageData id,
                ArrayList<GalleryTimestampRange> galleryTimestampsRangesArray, long threshold)
                throws IOException, ParseException {
            long exifTimestamp = id.getExifTime();
            if (exifTimestamp == 0) {
                exifTimestamp = ImageUtils.getExifDateTime(id.file.getAbsolutePath());
                id.setExifTime(exifTimestamp);
            }
            if (exifTimestamp == -1) {
                return true;
            }
            long adjustedTime = exifTimestamp + settings.getCameraTimeDifference() * 1000;
            long timestamp = JobCacheManager.getGalleryTimestamp(adjustedTime);
            long timestampWithThreshold = JobCacheManager.getGalleryTimestamp(adjustedTime
                    + threshold);

            for (int i = galleryTimestampsRangesArray.size() - 1; i >= 0; i--) {
                if (isCancelled()) {
                    return false;
                }
                GalleryTimestampRange gts = galleryTimestampsRangesArray.get(i);
                if (gts.rangeStart <= timestamp || gts.rangeStart <= timestampWithThreshold) {
                    long rangeTime = JobCacheManager.getTimeFromGalleryTimestamp(gts.rangeStart);
                    long diff = Math.abs(rangeTime - adjustedTime);
                    if (diff <= threshold) {
                        CommonUtils
                                .debug(TAG,
                                        "MatchingByTimeCheckConditionTask.processImageData: found image withing threshold %1$d ms. Image path %2$s. Image timestamp: %3$d. Gallery timestamp %4$d",
                                        threshold, id.file.getAbsolutePath(), timestamp,
                                        gts.rangeStart);
                        mShowSyncRecommendation = true;
                        break;
                    }
                    if (gts.rangeStart <= timestamp) {
                        break;
                    }
                }
            }
            return true;
        }

        @Override
        void nullifyTask() {
            mMatchingByTimeCheckConditionTask = null;
        }

        @Override
        protected void onSuccessPostExecute() {
            super.onSuccessPostExecute();

            if (mShowSyncRecommendation) {
                if (isActivityAlive) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

                    alert.setMessage(R.string.main_matching_by_time_sync_recommendation);

                    alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Intent intent = new Intent(MainActivity.this,
                                    CameraTimeSyncActivity.class);
                            startActivity(intent);
                        }
                    });

                    alert.setNegativeButton(R.string.main_matching_no_continue_matching,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    mMatchingByTimeTask = new MatchingByTimeTask(mFileName, 0);
                                    mMatchingByTimeTask.execute();
                                }
                            });
                    alert.show();
                }
            } else {
                mMatchingByTimeTask = new MatchingByTimeTask(mFileName, 0);
                mMatchingByTimeTask.execute();
            }
        }

        @Override
        protected void onFailedPostExecute() {
            super.onFailedPostExecute();
            GuiUtils.alert(R.string.main_matching_by_time_failed);
        }
    }

    class MatchingByTimeTask extends DataModifierTask {
        
        String mFileName;
        int mManualShift;

        public MatchingByTimeTask(String fileName, int manualShift) {
            super(mMatchingByTimeStatusLoadingControl);
            this.mFileName = fileName;
            this.mManualShift = manualShift;
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled()) {
                    return false;
                }
                File file = new File(mFileName);
                if (!file.exists()) {
                    CommonUtils.debug(TAG,
                            "MatchingByTimeTask.doInBackground: file %1$s is not found, returning",
                            mFileName);
                    return false;
                }
                DataSnapshot ds = getDataSnapshot();
                CurrentDataInfo cdi = ds.getCurrentDataInfoForFileName(mFileName);
                if (cdi != null) {
                    for (int i = cdi.groupPosition, size = cdi.dataSnapshot.size(); i < size; i++) {
                        ImageDataGroup idg = cdi.dataSnapshot.get(i);
                        if (!TextUtils.isEmpty(idg.sku) && i != cdi.groupPosition) {
                            break;
                        }
                        int startPos = i == cdi.groupPosition ? cdi.inGroupPosition : 0;
                        List<ImageData> imagesList = ds.imageDataList.get(i);
                        if (!processImageDataGroup(idg, imagesList, startPos)) {
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

        private boolean processImageDataGroup(ImageDataGroup idg, List<ImageData> data, int startPos) throws IOException,
                ParseException {
            for (int j = startPos, size2 = data.size(); j < size2; j++) {
                if (isCancelled()) {
                    return false;
                }
                ImageData id = data.get(j);
                long exifTime = ImageUtils.getExifDateTime(id.file.getAbsolutePath());
                GalleryTimestampRange gtr = JobCacheManager.getSkuProfileIDForExifTimeStamp(
                        MyApplication.getContext(), exifTime + mManualShift * 1000);
                if (gtr != null) {
                    CommonUtils
                            .debug(TAG,
                                    "MatchingByTimeTask.processImageDataGroup: queuing file %1$s for sku %2$s",
                                    id.file.getAbsolutePath(), gtr.escapedSKU);
                    incModifiersIfNecessary();
                    ImagesLoader.queueImage(id.file, gtr.escapedSKU, true, false);
                }
                if (!TextUtils.isEmpty(idg.sku)) {
                    break;
                }
            }
            return true;
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

    public static class ClearThumbCachesTask extends AbstractClearDiskCachesTask {
        static AtomicInteger activeCounter = new AtomicInteger(0);

        public static boolean isActive() {
            return activeCounter.get() > 0;
        }

        @Override
        protected AtomicInteger getActiveCounter() {
            return activeCounter;
        }

        public ClearThumbCachesTask() {
            super(EventType.MAIN_THUMB_CACHE_CLEARED, EventType.MAIN_THUMB_CACHE_CLEAR_FAILED,
                    THUMBS_CACHE_PATH);
        }

    }
    
    class LoadThumbsTask extends DataModifierTask
    {
        ThumbsImageWorkerAdapter adapter;
        int mScreenLargerDimension;
        boolean mAutoScan;
        boolean mFoundNotScanned = false;
        List<File> mFilesToScan = new ArrayList<File>();

        public LoadThumbsTask(boolean autoScan) {
            super(mThumbsLoadingControl);
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mScreenLargerDimension = metrics.widthPixels;
            if (mScreenLargerDimension < metrics.heightPixels) {
                mScreenLargerDimension = metrics.heightPixels;
            }
            this.mAutoScan = autoScan;
        }

        @Override
        protected void onFailedPostExecute() {
            super.onFailedPostExecute();
            if (isActivityAlive)
            {
            }
        }

        @Override
        void nullifyTask() {
            // do nothing
        }

        @Override
        protected void onSuccessPostExecute() {
            super.onSuccessPostExecute();
            if (!isCancelled()) {
                try
                {
                    mUploadButton.setEnabled(adapter.getSize() > 0);
                    final int scrollTo;
                    if (thumbnailsAdapter == null
                            || thumbnailsList.getStartX() == thumbnailsList.getMaxX()) {
                        scrollTo = Integer.MAX_VALUE;
                    } else {
                        scrollTo = thumbnailsList.getStartX();
                    }
                    mImageWorker.setAdapter(adapter);
                    if (thumbnailsAdapter == null) {
                        thumbnailsAdapter = new ThumbnailsAdapter(MainActivity.this, mImageWorker);
                        thumbnailsList.setAdapter(thumbnailsAdapter);
                    } else {
                        thumbnailsAdapter.notifyDataSetChanged();
                    }
                    GuiUtils.post(new Runnable() {

                        @Override
                        public void run() {
                            thumbnailsList.scrollTo(scrollTo);
                        }
                    });
                    for(File f: mFilesToScan)
                    {
                        AutoDecodeImageTask task = new AutoDecodeImageTask(f.getAbsolutePath());
                        task.executeOnExecutor(sAutoDecodeExecutor);
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
                        ZXingCodeScanner scanner = new ZXingCodeScanner();
                        ImageData lastDecodedData = null;
                        boolean lastDecodedDataFromScan = false;
                        for (File file : files) {
                            if (isCancelled()) {
                                return false;
                            }
                            ImageData id = ImageData.getImageDataForFile(file, true);
                            String sku = getSku(id);
                            boolean justScanned = false;
                            boolean cameraSyncCode = false;
                            if (sku == null) {
                                ScanState scanState = ScanUtils.getScanStateForFileName(id.file
                                        .getName());
                                if (scanState == ScanState.NOT_SCANNED) {
                                    if (mAutoScan)
                                    {
                                        DetectDecodeResult ddr = scanner.detectDecodeMultiStep(
                                                id.file.getAbsolutePath(), mScreenLargerDimension);
                                        justScanned = true;
                                        if (ddr.isDecoded()) {
                                            scanState = ScanState.SCANNED_DECODED;
                                            String[] urlData = ddr.getCode().split("/");
                                            sku = URLEncoder.encode(urlData[urlData.length - 1],
                                                    "UTF-8");
                                            lastDecodedData = id;
                                            lastDecodedDataFromScan = true;
                                        } else {
                                            {
                                                scanState = ScanState.SCANNED_NOT_DETECTED;
                                            }
                                        }
                                        incModifiersIfNecessary();
                                        String filePath = id.getFile().getAbsolutePath();
                                        File newFile = new File(ScanUtils.setScanStateForFileName(
                                                filePath, scanState));
                                        if (id.getFile().renameTo(newFile)) {
                                            id.setFile(newFile);
                                            cameraSyncCode = sku != null
                                                    && sku.startsWith(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX);
                                            if (sku != null && !cameraSyncCode) {
                                                id.setFile(ImagesLoader.queueImage(id.getFile(),
                                                        sku, true, true));
                                            }
                                            if (cameraSyncCode) {
                                                lastDecodedData = null;
                                            }
                                        }
                                    } else {
                                        mFoundNotScanned = true;
                                        mFilesToScan.add(id.getFile());
                                    }
                                } else {
                                    if (scanState != ScanState.SCANNED_NOT_DETECTED) {
                                        lastDecodedData = null;
                                    }
                                }
                                id.setScanState(scanState);
                            } else {
                                boolean isDecodedCode = ImagesLoader.isDecodedCode(id.getFile()
                                        .getName());
                                if (isDecodedCode) {
                                    id.setScanState(ScanState.SCANNED_DECODED);
                                    lastDecodedData = id;
                                    lastDecodedDataFromScan = false;
                                }
                            }
                            if (id.getScanState() != ScanState.SCANNED_DECODED
                                    && id.getScanState() != ScanState.SCANNED_DETECTED_NOT_DECODED
                                    && lastDataGroup != null
                                    && !TextUtils.equals(sku, lastDataGroup.sku)
                                    && lastDataGroup.data.indexOf(lastDecodedData) != -1) {
                                if ((justScanned && !cameraSyncCode) || lastDecodedDataFromScan) {
                                    sku = lastDataGroup.sku;
                                    incModifiersIfNecessary();
                                    id.setFile(TextUtils.isEmpty(sku) ? ImagesLoader
                                            .undoImage(file) : ImagesLoader.queueImage(
                                            id.getFile(), sku, true,
                                            false));
                                }
                            }
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
            super(path, FileObserver.DELETE | FileObserver.DELETE_SELF | FileObserver.MOVE_SELF
                    | FileObserver.MOVED_FROM | FileObserver.MOVED_TO | FileObserver.CLOSE_WRITE);
            mPath = path;
            mActivity = activity;
        }

        @Override
        public void onEvent(final int event, final String fileName) {
            try {
                CommonUtils.debug(TAG, "ImageObserver: event %1$d fileName %2$s", event, fileName);
                if (fileName != null && !fileName.equals(".probe")) {
                    final File file = new File(mPath + "/" + fileName);
                    CommonUtils.debug(TAG, "File modified [" + file.getAbsolutePath() + "]");
                    // fix for the issue #309
                    String type = FileUtils.getMimeType(file);
                    if ((type != null && type.toLowerCase().startsWith("image/"))
                            || ImagesLoader.isSpecialRenamedFile(file)) {
                        sLastUpdatedTime.set(SystemClock.elapsedRealtime());
                        GuiUtils.post(new Runnable() {

                            @Override
                            public void run() {
                                switch (event) {
                                    case FileObserver.DELETE:
                                    case FileObserver.MOVED_FROM:
                                        mActivity.fileRemoved(file.getAbsolutePath());
                                        break;
                                    case FileObserver.MOVED_TO:
                                    case FileObserver.CLOSE_WRITE:
                                        mActivity.fileAdded(file.getAbsolutePath());
                                        break;
                                }
                            }
                        });
                    }
                }
            } catch (Exception ex) {
                GuiUtils.noAlertError(TAG, ex);
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
        boolean modified = false;
        AtomicBoolean loadRequested = new AtomicBoolean(false);
        AtomicBoolean loadFailed = new AtomicBoolean(false);
        AtomicBoolean doesntExist = new AtomicBoolean(false);

        public void copyInfoShort(ImageDataGroup target) {
            target.sku = sku;
            target.name = name;
            target.cached = cached;
            target.modified = true;
        }
    }

    public static class ImageData
    {
        File file;
        int width;
        int height;
        int viewWidth;
        int viewHeight;
        int orientation;
        ScanState scanState;
        AtomicLong exifTime = new AtomicLong(0);

        public ImageData(File file, int width, int height) {
            this(file, width, height, 0, 0);
        }
        
        public ImageData(File file, int width, int height, long exifTime, int orientation) {
            super();
            this.file = file;
            this.width = width;
            this.height = height;
            this.exifTime.set(exifTime);
            this.orientation = orientation;
        }

        @Override
        public String toString() {
            return file.getName();
        }

        public static ImageData getImageDataForFile(File file, boolean supportCropRect)
                throws IOException {
            return getImageDataForFile(file, supportCropRect, false);
        }

        public static ImageData getImageDataForFile(File file, boolean supportCropRect,
                boolean getExifTime) throws IOException {
            Rect cropRect = supportCropRect ? ImagesLoader.getBitmapRect(file) : null;
            int width, height;
            if (cropRect == null) {
                long start = System.currentTimeMillis();
                BitmapFactory.Options options = ImageUtils.calculateImageSize(file
                        .getAbsolutePath());
                width = options.outWidth;
                height = options.outHeight;
                CommonUtils.debug(TAG,
                        "getImageDataForFile: image dimension calculation time %1$d ms",
                        System.currentTimeMillis() - start);
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
            long exifTime = 0;
            if (getExifTime) {
                exifTime = ImageUtils.getExifDateTime(file.getAbsolutePath());
            }
            return new ImageData(file, width, height, exifTime, orientation);
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

        public long getExifTime() {
            return exifTime.get();
        }

        public void setExifTime(long exifTime) {
            this.exifTime.set(exifTime);
        }

        public int getOrientation() {
            return orientation;
        }

        public ScanState getScanState() {
            return scanState;
        }

        public void setScanState(ScanState scanState) {
            this.scanState = scanState;
        }
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
            Rect cropRect = ImagesLoader.getBitmapRect(imageData.getFile());
            return ImageUtils.decodeSampledBitmapFromFile(imageData.getFile().getAbsolutePath(),
                    mImageWorker.getImageHeight(), mImageWorker.getImageHeight(),
                    imageData.getOrientation(),
                    cropRect);
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
        boolean m2FingersDetected = false;
        private On2FingersDownListener mOn2FingersDownListener;

        public OnLongClickListener mImageItemOnLongClickListener = new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                ThumbnailsAdapter adapter = ((ThumbnailsAdapter) getAdapter());
                adapter.currentData = (CurrentDataInfo) v.getTag();
                adapter.longClicked = true;
                if (!isMoving() && !m2FingersDetected) {
                    adapter.mContext.get().registerForContextMenu(v);
                    v.showContextMenu();
                    adapter.mContext.get().unregisterForContextMenu(v);
                    return true;
                } else {
                    return false;
                }
            }
        };

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
                View singleImageView = getSingleImageView(new CurrentDataInfo(
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

        public final View getSingleImageView(CurrentDataInfo cdi,
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
                holder.decodedIndicator = convertView.findViewById(R.id.selection_overlay);
                holder.detectedIndicator = convertView.findViewById(R.id.detectedIndicator);
                holder.imageView = (ImageView) convertView.findViewById(R.id.image);
                holder.containerRoot.setOnClickListener(mImageItemOnClickListener);
                holder.containerRoot.setOnLongClickListener(mImageItemOnLongClickListener);
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
            ScanState scanState = data.getScanState();
            holder.decodedIndicator.setVisibility(gvh.data.sku != null
                    && scanState == ScanState.SCANNED_DECODED ? View.VISIBLE : View.INVISIBLE);
            holder.detectedIndicator.setVisibility(gvh.data.sku == null
                    && scanState == ScanState.SCANNED_DETECTED_NOT_DECODED ? View.VISIBLE
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

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            boolean handled = false;
            switch (ev.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    int pointerCount = ev.getPointerCount();
                    if (pointerCount == 2) {
                        m2FingersDetected = true;
                        if (mOn2FingersDownListener != null) {
                            mOn2FingersDownListener.on2FingersDown(ev);
                        }
                    }
                    handled = true;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    handled = true;
                    break;
                case MotionEvent.ACTION_DOWN:
                    handled = true;
                    m2FingersDetected = false;
                    break;
                case MotionEvent.ACTION_UP:
                    if (m2FingersDetected) {
                        onUp(ev);
                    }
                    break;
            }
            if (!m2FingersDetected) {
                handled |= super.dispatchTouchEvent(ev);
            }
            return handled;
        }

        public void setOn2FingersDownListener(On2FingersDownListener listener) {
            this.mOn2FingersDownListener = listener;
        }

        public static interface On2FingersDownListener {
            void on2FingersDown(MotionEvent ev);
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
        public boolean longClicked = false;
        private static SerialExecutor sCacheLoaderExecutor = new SerialExecutor(
                Executors.newSingleThreadExecutor());

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
                longClicked = false;
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
            holder.position = position;
            if (holder.data == idg && !idg.modified) {
                CommonUtils.debug(TAG, "getView: requested view for same data. Return cached.");
                return convertView;
            }
            idg.modified = false;
            holder.data = idg;
            holder.dataSnapshot = ((ThumbsImageWorkerAdapter) mImageWorker.getAdapter()).data;
            if (holder.loaderTask != null) {
                holder.loaderTask.cancel(true);
            }
            if (holder.timerTask != null) {
                holder.timerTask.cancel();
            }
            if (!idg.cached && !idg.loadFailed.get()) {
                holder.loaderTask = new CacheLoaderTask(idg, holder, true, holder.loadingControl);
                holder.loaderTask.executeOnExecutor(sCacheLoaderExecutor);
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
            Timer timerTask;
            LoadingControl loadingControl;
        }

        public static class ItemViewHolder {
            View decodedIndicator;
            View detectedIndicator;
            View containerRoot;
            ImageView imageView;
            public ImageData data;
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
            private boolean dontPerformServerOperation;
            private int requestId = MageventoryConstants.INVALID_REQUEST_ID;

            private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();

            public CacheLoaderTask(ImageDataGroup idg, GroupViewHolder groupViewHolder,
                    boolean dontPerformServerOperation,
                    LoadingControl loadingControl) {
                super(loadingControl);
                groupViewHolderReference = new WeakReference<GroupViewHolder>(groupViewHolder);
                this.idg = idg;
                this.dontPerformServerOperation = dontPerformServerOperation;
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
                        CommonUtils.debug(TAG, "CacheLoaderTask.doInBackground: cancelled");
                        return false;
                    }
                    if (existResult.isExisting()) {
                        updateImageDataGroupFromProduct(existResult.getSku());
                    } else {
                        if (dontPerformServerOperation) {
                            // do nothing
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
                                        CommonUtils
                                                .debug(TAG,
                                                        "CacheLoaderTask.doInBackground: cancelled (interrupted)");
                                        return false;
                                    }
                                }
                            } finally {
                                resHelper.unregisterLoadOperationObserver(this);
                            }
                            if (success) {
                                CommonUtils
                                        .debug(TAG,
                                                "CacheLoaderTask.doInBackground: success loading for sku: %1$s",
                                                idg.sku);
                                updateImageDataGroupFromProduct(idg.sku);
                            } else {
                                CommonUtils
                                        .debug(TAG,
                                                "CacheLoaderTask.doInBackground: failed loading for sku: %1$s",
                                                String.valueOf(idg.sku));
                                idg.loadFailed.set(true);
                                idg.doesntExist.set(doesntExist);
                            }
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
                    if (dontPerformServerOperation && !idg.cached && !idg.loadRequested.get()) {
                        final CacheLoaderTask task = new CacheLoaderTask(idg, gvh, false,
                                gvh.loadingControl);
                        gvh.loaderTask = task;
                        gvh.timerTask = new Timer();
                        gvh.timerTask.schedule(new TimerTask() {

                            @Override
                            public void run() {
                                GuiUtils.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        GroupViewHolder gvh = groupViewHolderReference.get();
                                        if (gvh != null && gvh.loaderTask == task
                                                && !gvh.loaderTask.isCancelled()) {
                                            CommonUtils.debug(TAG,
                                                    "CacheLoaderTask: scheduling from timer");
                                            gvh.loaderTask.executeOnExecutor(sCacheLoaderExecutor);
                                        }
                                    }
                                });
                            }
                        }, 2000);
                    }
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
