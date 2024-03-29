/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/

package com.mageventory.activity;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.SystemClock;
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
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mventory.R;
import com.mageventory.activity.MainActivity.HorizontalListViewExt.AutoScrollType;
import com.mageventory.activity.MainActivity.HorizontalListViewExt.On2FingersDownListener;
import com.mageventory.activity.MainActivity.ThumbnailsAdapter.ItemViewHolder;
import com.mageventory.activity.MainActivity.ThumbnailsAdapter.OnLoadedSkuUpdatedListener;
import com.mageventory.activity.ScanActivity.CheckSkuResult;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.bitmapfun.util.DiskLruCache;
import com.mageventory.bitmapfun.util.ImageCache;
import com.mageventory.bitmapfun.util.ImageCacheUtils;
import com.mageventory.bitmapfun.util.ImageCacheUtils.AbstractClearDiskCachesTask;
import com.mageventory.bitmapfun.util.ImageFileSystemFetcher;
import com.mageventory.bitmapfun.util.ImageResizer;
import com.mageventory.bitmapfun.util.ImageWorker;
import com.mageventory.bitmapfun.util.ImageWorker.ImageWorkerAdapter;
import com.mageventory.fragment.ManageDialogFragment;
import com.mageventory.job.ExternalImagesJob;
import com.mageventory.job.ExternalImagesJobQueue;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCacheManager.GalleryTimestampRange;
import com.mageventory.job.JobCacheManager.ProductDetailsExistResult;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobQueue;
import com.mageventory.job.JobQueue.JobsSummary;
import com.mageventory.job.JobService;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.resprocessor.ProductDetailsProcessor.ProductDetailsLoadException;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.ExecuteProfile;
import com.mageventory.tasks.LoadAttributeSetTaskAsync;
import com.mageventory.tasks.LoadProfilesList;
import com.mageventory.tasks.LoadStatistics;
import com.mageventory.util.CommonAnimationUtils;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.DefaultOptionsMenuHelper;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.util.EventBusUtils.GeneralBroadcastEventHandler;
import com.mageventory.util.FileUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.ImagesLoader;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.Log;
import com.mageventory.util.ScanUtils;
import com.mageventory.util.ScanUtils.ScanState;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.SimpleViewLoadingControl;
import com.mageventory.util.SingleFrequencySoundGenerator;
import com.mageventory.util.TrackerUtils;
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
    static final String REFRESH_PRESSED = "MainActivity.REFRESH_PRESSED";
    static final int RECENT_PRODUCTS_LIST_CAPACITY = 3;

    private static SerialExecutor sAutoDecodeExecutor = new SerialExecutor(
            Executors.newSingleThreadExecutor());

    static Comparator<CurrentDataInfo> sCurrentDataInfoComparator = new Comparator<CurrentDataInfo>() {
        @Override
        public int compare(CurrentDataInfo lhs, CurrentDataInfo rhs) {
            return ExternalImagesEditActivity.filesComparator.compare(lhs.imageData.getFile(),
                    rhs.imageData.getFile());
        }
    };

    /**
     * An enum describing possible MainActivity states
     */
    enum State {
        /**
         * The state for the statistics screen
         */
        STATS,
        /**
         * The state for the photos screen
         */
        PHOTOS,
        /**
         * The state for the no photos loaded screen
         */
        NO_PHOTOS
    }

    /**
     * The current activity state
     */
    State mCurrentState;

    protected MyApplication app;
    private Settings settings;
    ProgressDialog pDialog;
    private boolean isActivityAlive;
    private Button profilesButton;
    private Button mUploadButton;

    private LayoutInflater mInflater;

    public View mMainContent;
    public LinearLayout mErrorReportingProgress;

    private LoadStatistics mLoadStatisticsTask;
    private LinearLayout mStatisticsLoadingProgressLayout;
    private LinearLayout mStatisticsLayout;
    private LinearLayout mStatisticsLoadingFailedLayout;
    private boolean mForceRefreshStatistics = false;

    private boolean mRefreshOnResume = false;
    /**
     * Flag to schedule attribute set reloading in the onResume method. We
     * schedule reloading there to minimize data reloading time in the
     * MainActivity itself. However in most cases product details in the images
     * strip will not be loaded until attribute set is reloaded because of
     * delayed call
     */
    private boolean mScheduleAttributeSetsReloading = false;

    private LoadProfilesList mLoadProfilesTask;
    private ExecuteProfile mExecuteProfileTask;

    private ProgressDialog mProgressDialog;

    private JobControlInterface mJobControlInterface;

    private JobsSummary mJobsSummary;
    private int mExternalImagesPendingPhotoCount;

    private TextView mPendingJobStatsText;
    private TextView mFailedJobStatsText;
    private View mJobsStatsContainer;
    private View mPendingIndicator;
    private View mFailedIndicator;

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
    RecentProductsAdapter mRecentProductsAdapter;
    LoadingControl mRecentProductsListLoadingControl;
    /**
     * Reference to the registered media mounted broadcast receiver so it can be
     * used in the onDestroy method for the unregisterReceiver call
     */
    private BroadcastReceiver mMediaMountedReceiver;
    /**
     * Reference to the last view used to display context menu. Need to remember
     * it to handle "More" menu items for the images strip context menus
     */
    private View mLastViewForContextMenu;

    /**
     * The view the the {@link State#STATS} state
     */
    View mStatsView;
    /**
     * The view the the {@link State#PHOTOS} state
     */
    View mPhotosView;
    /**
     * The view the the {@link State#NO_PHOTOS} state
     */
    View mNoPhotosView;
    /**
     * The view which indicates selected {@link State#STATS} state
     */
    View mStatsStateIndicator;
    /**
     * The view which indicates selected {@link State#PHOTOS} or
     * {@link State#NO_PHOTOS} state
     */
    View mPhotosStateIndicator;
    /**
     * The view which activates {@link State#STATS} state
     */
    private View mStatsStateButton;
    /**
     * The view which activates {@link State#PHOTOS} or {@link State#NO_PHOTOS}
     * state
     */
    private View mPhotosStateButton;

    private CurrentDataInfo mLastCurrentData;
    private String mLastRequestedMatchByTimeFileNameWithSyncRecommendation;

    private void updateJobsStats()
    {
        int pending = mJobsSummary == null ? 0 : mJobsSummary.pending.getTotal()
                + mExternalImagesPendingPhotoCount;
        int failed = mJobsSummary == null ? 0 : mJobsSummary.failed.getTotal();

        mPendingJobStatsText.setText(CommonUtils.formatNumber(pending));
        mFailedJobStatsText.setText(CommonUtils.formatNumber(failed));
        mFailedJobStatsText.setTextColor(failed > 0 ? getResources().getColor(R.color.red)
                : mPendingJobStatsText.getCurrentTextColor());
        mPendingIndicator.setVisibility(pending > 0 ? View.VISIBLE : View.GONE);
        mFailedIndicator.setVisibility(failed > 0 && pending == 0 ? View.VISIBLE : View.GONE);
        mJobsStatsContainer.setVisibility(pending + failed > 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new Settings(getApplicationContext());
        if (startWelcomeActivityIfNecessary()) {
            return;
        }
        setContentView(R.layout.main);

        mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        mJobControlInterface = new JobControlInterface(this);

        isActivityAlive = true;

        JobService.wakeUp(this);

        app = (MyApplication) getApplication();
        settings.registerGalleryPhotosDirectoryChangedListener(new Runnable() {

            @Override
            public void run() {
                reloadThumbs();
                restartObservation();
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



        profilesButton = (Button) findViewById(R.id.profilesButton);

        profilesButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                mLoadProfilesTask = new LoadProfilesList(MainActivity.this, false);
                mLoadProfilesTask.execute();
            }
        });

        OnClickListener generalListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.productsBtn) {
                    DefaultOptionsMenuHelper.onMenuProductsPressed(MainActivity.this);
                } else if (v.getId() == R.id.newBtn) {
                    DefaultOptionsMenuHelper.onMenuNewPressed(MainActivity.this);
                } else if (v.getId() == R.id.helpBtn) {
                    DefaultOptionsMenuHelper.onMenuHelpPressed(MainActivity.this);
                } else if (v.getId() == R.id.manageBtn) {
                    ManageDialogFragment dialogFragment = new ManageDialogFragment();
                    dialogFragment.show(getSupportFragmentManager(),
                            ManageDialogFragment.class.getSimpleName());
                } else if (v.getId() == R.id.statsButton) {
                    setState(State.STATS);
                } else if (v.getId() == R.id.photosButton) {
                    if (thumbnailsAdapter == null || thumbnailsAdapter.isEmpty()) {
                        setState(State.NO_PHOTOS);
                    } else {
                        setState(State.PHOTOS);
                    }
                }
            }
        };

        findViewById(R.id.productsBtn).setOnClickListener(generalListener);
        findViewById(R.id.newBtn).setOnClickListener(generalListener);
        findViewById(R.id.helpBtn).setOnClickListener(generalListener);
        findViewById(R.id.manageBtn).setOnClickListener(generalListener);
        mStatsStateButton = findViewById(R.id.statsButton);
        mPhotosStateButton = findViewById(R.id.photosButton);
        mStatsStateButton.setOnClickListener(generalListener);
        mPhotosStateButton.setOnClickListener(generalListener);

        mMainContent = findViewById(R.id.scroll);
        mErrorReportingProgress = (LinearLayout) findViewById(R.id.errorReportingProgress);

        mStatisticsLoadingProgressLayout = (LinearLayout) findViewById(R.id.statisticsLoadingProgress);
        mStatisticsLayout = (LinearLayout) findViewById(R.id.statisticsLayout);
        mStatisticsLoadingFailedLayout = (LinearLayout) findViewById(R.id.statisticsLoadingFailed);
        mThumbsLoadIndicator = findViewById(R.id.imagesLoadingStatusLine);
        mThumbsLoadingControl = new SimpleViewLoadingControl(mThumbsLoadIndicator);

        mPendingJobStatsText = (TextView) findViewById(R.id.pendingJobStats);
        mFailedJobStatsText = (TextView) findViewById(R.id.failedJobStats);
        mJobsStatsContainer = findViewById(R.id.jobsStatsContainer);
        mPendingIndicator = findViewById(R.id.pendingIndicator);
        mFailedIndicator = findViewById(R.id.failedIndicator);

        
        // Initialize states related views
        mStatsView = findViewById(R.id.statsView);
        mPhotosView = findViewById(R.id.photosView);
        mNoPhotosView = findViewById(R.id.noPhotosView);
        mStatsStateIndicator = findViewById(R.id.statsIndicator);
        mPhotosStateIndicator = findViewById(R.id.photosIndicator);

        ExternalImagesJobQueue.registerExternalImagesCountChangedBroadcastReceiver(TAG,
                new ExternalImagesJobQueue.ExternalImagesCountChangedListener() {

                    @Override
                    public void onExternalImagesCountChanged(final int newCount) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isActivityAlive) {
                                    mExternalImagesPendingPhotoCount = newCount;
                                    updateJobsStats();
                                }
                            }
                        });
                    }
                }, this);

        JobQueue.registerJobSummaryChangedBroadcastReceiver(TAG,
                new JobQueue.JobSummaryChangedListener() {

            @Override
            public void OnJobSummaryChanged(final JobsSummary jobsSummary) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (isActivityAlive) {
                            mJobsSummary = jobsSummary;
                            updateJobsStats();
                        }
                    }
                });
            }
                }, this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mForceRefreshStatistics = extras.getBoolean(getString(R.string.ekey_reload_statistics));
        }

        boolean refreshPressed = extras != null ? extras.getBoolean(REFRESH_PRESSED, false)
                : false;
        mScheduleAttributeSetsReloading = refreshPressed;
        initRecentProducts();
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
        initMediaMountedReceiver();
        setState(State.STATS);
    }

    /**
     * Init the media mounted receiver field and register it to listen for the
     * {@link Intent.#ACTION_MEDIA_MOUNTED} broadcast events
     */
    private void initMediaMountedReceiver() {
        mMediaMountedReceiver = new MediaMountedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addDataScheme("file");
        registerReceiver(mMediaMountedReceiver, filter);
    }

    public boolean startWelcomeActivityIfNecessary() {
        boolean result = false;
        if (!settings.hasSettings()) {
            Intent i = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(i);
            finish();
            result = true;
        }
        return result;
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
        if (startWelcomeActivityIfNecessary()) {
            return;
        }

        if (mLoadStatisticsTask == null)
        {
            mLoadStatisticsTask = new LoadStatistics(this, mForceRefreshStatistics);
            mLoadStatisticsTask.execute();

            mForceRefreshStatistics = false;
        }
        updateClearCacheStatus();

        if (mRefreshOnResume) {
            mRefreshOnResume = false;
            refresh(false);
        }
        new CheckEyeFiStateTask().execute();
        
        // request attribute set reloading if scheduled. Scheduling is performed
        // when activity is recreated after the refresh operation
        if (mScheduleAttributeSetsReloading) {
            mScheduleAttributeSetsReloading = false;
            LoadAttributeSetTaskAsync.loadAttributes(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        JobService.deregisterOnJobServiceStateChangedListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityAlive = false;
        settings.unregisterListOfStoresPreferenceChangeListeners();
        stopObservation();
        if (diskCacheClearedReceiver != null) {
            unregisterReceiver(diskCacheClearedReceiver);
        }
        // unregister media mounted receiver if necessary
        if (mMediaMountedReceiver != null) {
            unregisterReceiver(mMediaMountedReceiver);
        }
        if (loadThumbsTask != null) {
            loadThumbsTask.cancel(true);
            loadThumbsTask = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            refresh();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            DefaultOptionsMenuHelper.onMenuHelpPressed(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Restart the activity and reload data
     */
    private void refresh() {
        refresh(true);
    }

    /**
     * Restart the activity
     * 
     * @param forceReload whether to clear cached and reload some data
     */
    private void refresh(boolean forceReload) {
        if (forceReload) {
            JobCacheManager.removeProfilesList(settings.getUrl());
            JobCacheManager.removeAllProductLists(settings.getUrl());
            JobCacheManager.removeOrderList(settings.getUrl());
        }

        Intent myIntent = new Intent(getApplicationContext(), getClass());
        myIntent.putExtra(getString(R.string.ekey_reload_statistics), true);
        myIntent.putExtra(getString(R.string.ekey_dont_show_menu), true);
        if (forceReload) {
            myIntent.putExtra(REFRESH_PRESSED, true);
        }
        // need to stop observation here, because onDestroy is called after
        // the recreated activity onCreate() and stopObservation there may
        // stop observer for newly created activity
        stopObservation();
        finish();
        startActivity(myIntent);
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
        thumbnailsList
                .setScrollIndicator((HorizontalScrollView) findViewById(R.id.thumbsScrollIndicator));
        // such as thumbnailsList is has horizontal scroll it is not working
        // good when included in vertical scroll container. We should request
        // disallow intercept of touch events for the parent scroll to have
        // better user experience http://stackoverflow.com/a/11554823/527759
        thumbnailsList.setOnDownListener(new OnDownListener() {
            @Override
            public void onDown(MotionEvent e) {
                CommonUtils.debug(TAG, "thumbnailsList: onDown");
                scroll.requestDisallowInterceptTouchEvent(true);
                // lock the drawers when images strip is touched to avoid their
                // slide in instead of images strip scroll
                setDrawersLocked(true);
            }
        });
        thumbnailsList.setOnUpListener(new OnUpListener() {
            @Override
            public void onUp(MotionEvent e) {
                CommonUtils.debug(TAG, "thumbnailsList: onUp");
                scroll.requestDisallowInterceptTouchEvent(false);
                // unlock drawers, such as scroll or other touch event is
                // finished
                setDrawersLocked(false);
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
        // initailize autoscroll buttons
        View thumbsScrollLeft = findViewById(R.id.thumbsScrollLeft);
        View thumbsScrollRight = findViewById(R.id.thumbsScrollRight);
        thumbsScrollLeft.setOnTouchListener(
                new AutoScrollTouchListener(AutoScrollType.LEFT, thumbnailsList));
        thumbsScrollRight.setOnTouchListener(
                new AutoScrollTouchListener(AutoScrollType.RIGHT, thumbnailsList));
        thumbnailsList.setScrollAvailableViews(thumbsScrollLeft, thumbsScrollRight);

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
        // remember last view used for context menu
        mLastViewForContextMenu = v;
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
                ScanUtils.startScanActivityForResult(MainActivity.this, SCAN_QR_CODE,
                        R.string.scan_barcode_or_qr_label);
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
            case R.id.menu_delete_all:
            case R.id.menu_delete_all_left:
            case R.id.menu_delete_all_right: {
                if (!checkModifierTasksActive()) {
                    return false;
                }
                final DeleteAllOption deleteOption;
                Integer message = null;
                // initialize variables depend on selected menu item
                switch(menuItemIndex)
                {
                    case R.id.menu_delete_all:
                        deleteOption = DeleteAllOption.ALL;
                        message = R.string.main_delete_all_confirmation;
                        break;
                    case R.id.menu_delete_all_left:
                        deleteOption = DeleteAllOption.LEFT;
                        message = R.string.main_delete_all_left_confirmation;
                        break;
                    case R.id.menu_delete_all_right:
                        deleteOption = DeleteAllOption.RIGHT;
                        message = R.string.main_delete_all_right_confirmation;
                        break;
                    default:
                        deleteOption = null;
                }
                mLastCurrentData = thumbnailsAdapter.currentData;
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setMessage(message);

                alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (!checkModifierTasksActive()) {
                            return;
                        }
                        mDeleteAllTask = new DeleteAllTask(mLastCurrentData.imageData.file
                                .getAbsolutePath(), deleteOption);
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
                        thumbnailsAdapter.currentData.imageData.getFile().getAbsolutePath(), false);
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
            case R.id.menu_more: {
                GuiUtils.post(new Runnable(){
                    @Override
                    public void run() {
                        // switch indicator which is used to determine what
                        // context menu should be displayed
                        thumbnailsAdapter.longClicked = !thumbnailsAdapter.longClicked;
                        registerForContextMenu(mLastViewForContextMenu);
                        mLastViewForContextMenu.showContextMenu();
                        unregisterForContextMenu(mLastViewForContextMenu);
                    }
                });
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
        mCurrentBeep = SingleFrequencySoundGenerator.playSuccessfulBeep(settings, mCurrentBeep);
    }

    private void playFailureBeep() {
        mCurrentBeep = SingleFrequencySoundGenerator.playFailureBeep(settings, mCurrentBeep);
    }

    public static SingleFrequencySoundGenerator checkConditionAndSetCameraTimeDifference(
            String code, long exifDateTime, Settings settings, SingleFrequencySoundGenerator beep,
            boolean silent, boolean withSound, Runnable runOnSuccess) {
        SingleFrequencySoundGenerator result = beep;
        if (exifDateTime != -1) {
            try {
                Date phoneDate = CommonUtils.parseDateTime(code
                        .substring(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX.length()));
                if (withSound) {
                    result = SingleFrequencySoundGenerator.playSuccessfulBeep(settings, result);
                }

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
            GuiUtils.showMessageDialog(R.string.main_upload_no_items_title,
                    R.string.main_upload_no_items_text, MainActivity.this);
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
                CheckSkuResult checkSkuResult = ScanActivity.checkSku(data);
                if (checkSkuResult != null) {
                    mProcessScanResultTask = new ProcessScanResultsTask(mLastCurrentData.imageData
                            .getFile().getAbsolutePath(), checkSkuResult.code);
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
                CheckSkuResult checkSkuResult = ScanActivity.checkSku(extra.getStringExtra(EventBusUtils.CODE));
                if (checkSkuResult != null) {
                    if (!checkModifierTasksActive()) {
                        return;
                    }
                    mDecodeImageTask = new DecodeImageTask(mLastCurrentData.imageData.getFile()
                            .getAbsolutePath(), checkSkuResult.code, false);
                    mDecodeImageTask.execute();
                }
                break;
            case SETTINGS_CHANGED:
                if (isActivityResumed()) {
                    refresh(false);
                } else {
                    mRefreshOnResume = true;
                }
                break;
            case PRODUCT_DETAILS_LOADED_IN_ACTIVITY: {
                String sku = extra.getStringExtra(EventBusUtils.SKU);
                new LoadRecentProductsTask(sku, false).execute();
                break;
            }
            case PRODUCT_DOESNT_EXISTS_AND_CACHE_REMOVED: {
                CommonUtils
                        .debug(TAG,
                                "onGeneralBroadcastEvent: received product doesn't exists and cache remvoed event");
                String sku = extra.getStringExtra(EventBusUtils.SKU);
                boolean reloadRecentProducts;
                if (mRecentProductsAdapter != null) {
                    reloadRecentProducts = mRecentProductsAdapter.removeProductForSkuIfExists(sku);
                } else {
                    reloadRecentProducts = true;
                }
                if (reloadRecentProducts) {
                    new LoadRecentProductsTask(null, false).execute();
                }
                break;
            }
            case LOAD_OPERATION_COMPLETED: {
                CommonUtils.debug(TAG,
                        "onGeneralBroadcastEvent: received load operation completed event");
                LoadOperation op = extra.getParcelableExtra(EventBusUtils.LOAD_OPERATION);
                if (op.getResourceType() == MageventoryConstants.RES_PRODUCT_DETAILS
                        && op.isSuccess()) {
                    // if product details successfully loaded
                    String sku = op.getExtras().getString(MageventoryConstants.MAGEKEY_PRODUCT_SKU);
                    CommonUtils
                            .debug(TAG,
                                    "onGeneralBroadcastEvent: received load operation completed event for sku %1$s",
                                    sku);
                    if (mRecentProductsAdapter.getProductPosition(sku) != -1) {
                        // if recent products list contains loaded product with
                        // the sku
                        new LoadRecentProductsTask(sku, true).execute();
                    }
                }
                break;
            }
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
                if (mCurrentState == State.PHOTOS && thumbnailsAdapter.isEmpty()) {
                    // if last file is removed and current state is PHOTOS state
                    setState(State.NO_PHOTOS);
                }
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
                    if (mCurrentState == State.NO_PHOTOS && !thumbnailsAdapter.isEmpty()) {
                        // if first file is added and current state is NO_PHOTOS
                        setState(State.PHOTOS);
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

    void initRecentProducts() {
        mRecentProductsAdapter = new RecentProductsAdapter(MainActivity.this);
        mRecentProductsListLoadingControl = new SimpleViewLoadingControl(
                findViewById(R.id.recentProductsListLoading));
        new LoadRecentProductsTask(null, false).execute();
    }

    /**
     * Set the current activity state. Possible states can be found in
     * {@link State} enumeration. Setting state will adjust visibility of
     * different views with various animation
     * 
     * @param state the state to select
     */
    void setState(final State state) {
        if (mCurrentState == state) {
            return;
        }
        final Runnable showNewStateWidgetsRunnable = new Runnable() {
            /**
             * Flag to prevent from running same actions twice
             */
            boolean mStarted = false;

            @Override
            public void run() {
                if (mStarted) {
                    // if was already run before
                    return;
                }
                // set the flag that action was already run
                mStarted = true;
                List<View> containers = new LinkedList<View>();
                Runnable runOnAnimationEnd = null;
                switch (state) {
                    case STATS:
                        containers.add(mStatsView);
                        containers.add(profilesButton);
                        if (mPhotosStateButton.getVisibility() == View.GONE) {
                            // if photos state button is not visible
                            containers.add(mPhotosStateButton);
                        }
                        containers.add(mStatsStateIndicator);
                        break;
                    case PHOTOS:
                        containers.add(mPhotosView);
                        containers.add(mUploadButton);
                        if (mStatsStateButton.getVisibility() == View.GONE) {
                            // if stats state button is not visible
                            containers.add(mStatsStateButton);
                        }
                        if (mPhotosStateIndicator.getVisibility() != View.VISIBLE) {
                            containers.add(mPhotosStateIndicator);
                        }
                        runOnAnimationEnd = new Runnable() {

                            @Override
                            public void run() {
                                thumbnailsList.updateScrollIndicator();
                            }
                        };
                        break;
                    case NO_PHOTOS:
                        containers.add(mNoPhotosView);
                        if (mStatsStateButton.getVisibility() == View.GONE) {
                            // if stats state button is not visible
                            containers.add(mStatsStateButton);
                        }
                        if (mPhotosStateButton.getVisibility() == View.GONE) {
                            // if photos state button is not visible
                            containers.add(mPhotosStateButton);
                        }
                        if (mPhotosStateIndicator.getVisibility() != View.VISIBLE) {
                            containers.add(mPhotosStateIndicator);
                        }
                        break;
                    default:
                        break;
                }
                CommonAnimationUtils.fadeIn(runOnAnimationEnd, containers);
            }
        };
        // if state was specified before we need to hide previous state
        // widgets
        if (mCurrentState != null) {
            // views which should be hidden when animation ends
            final List<View> containersToGone = new LinkedList<View>();
            List<View> containers = new LinkedList<View>();
            switch (mCurrentState) {
                case STATS:
                    containers.add(mStatsView);
                    containers.add(profilesButton);
                    containersToGone.add(profilesButton);
                    containers.add(mStatsStateIndicator);
                    break;
                case PHOTOS:
                    containers.add(mPhotosView);
                    containers.add(mUploadButton);
                    containersToGone.add(mUploadButton);
                    if (state != State.NO_PHOTOS) {
                        containers.add(mPhotosStateIndicator);
                    }
                    break;
                case NO_PHOTOS:
                    containers.add(mNoPhotosView);
                    if (state != State.PHOTOS) {
                        containers.add(mPhotosStateIndicator);
                    }
                    break;
                default:
                    break;
            }
            switch (state) {
                case STATS:
                    containers.add(mStatsStateButton);
                    containersToGone.add(mStatsStateButton);
                    break;
                case PHOTOS:
                    containers.add(mPhotosStateButton);
                    containersToGone.add(mPhotosStateButton);
                    break;
                default:
                    break;
            }
            CommonAnimationUtils.fadeOut(new Runnable() {

                @Override
                public void run() {
                    if (mCurrentState != state) {
                        // if state was changed again during animation run
                        return;
                    }
                    // run scheduled operation to show new state
                    // widgets when the hiding widget animation ends
                    showNewStateWidgetsRunnable.run();
                    for (View view : containersToGone) {
                        view.setVisibility(View.GONE);
                    }
                }
            }, containers);
        } else {
            // reset visibility of containers to fix possible invalid
            // appearance after the activity state restore
            List<View> containersToGone = new LinkedList<View>();
            List<View> containers = new LinkedList<View>();
            containersToGone.add(profilesButton);
            containersToGone.add(mUploadButton);
            containersToGone.add(mStatsStateButton);
            containersToGone.add(mPhotosStateButton);
            containers.add(mPhotosStateIndicator);
            containers.add(mStatsStateIndicator);
            containers.add(mStatsView);
            containers.add(mPhotosView);
            containers.add(mNoPhotosView);
            for (View view : containersToGone) {
                view.setVisibility(View.GONE);
            }
            for (View view : containers) {
                view.setVisibility(View.INVISIBLE);
            }
        }

        // run widget for the new state showing operation explicitly if
        // previous state is null
        if (mCurrentState == null) {
            showNewStateWidgetsRunnable.run();
        }
        mCurrentState = state;
    }

    class LoadRecentProductsTask extends SimpleAsyncTask {
        /**
         * Loaded product details
         */
        List<Product> mData;
        /**
         * The sku of the product which should be appended to the recent
         * products list
         */
        String mSku;
        /**
         * Whether the loaded data should not be added on top to the current
         * recent products list but just updated product details if there are
         * matches
         */
        boolean mUpdateDataOnly;
        /**
         * Settings snapshot
         */
        private SettingsSnapshot mSettingsSnapshot;

        /**
         * @param sku the SKU to load details for. If null the recent product
         *            details list will be refreshed, otherwise the product
         *            details for the SKU will be appended or refreshed depend
         *            on updateDataOnly parameter to the list
         * @param updateDataOnly whether the loaded data should not be added on
         *            top to the current recent products list but just updated
         *            product details if there are matches
         */
        public LoadRecentProductsTask(String sku, boolean updateDataOnly) {
            super(mRecentProductsListLoadingControl);
            mSku = sku;
            mSettingsSnapshot = new SettingsSnapshot(MainActivity.this);
            mUpdateDataOnly = updateDataOnly;
        }

        @Override
        protected void onSuccessPostExecute() {
            if (!isActivityAlive()) {
                return;
            }
            List<Product> recentProducts = mRecentProductsAdapter.recentProducts;
            // remove duplicates
            for (int i = 0, size = mData.size(); i < size; i++) {
                Product p = mData.get(i);
                if (mUpdateDataOnly) {
                    // if data in the list should be updated only without
                    // changing of products order
                    int ind = RecentProductsAdapter.getProductPosition(recentProducts, p.getSku());
                    if (ind != -1) {
                        // if data for the product sku is present in the list
                        recentProducts.set(ind, p);
                    }
                } else {
                    RecentProductsAdapter.removeProductForSkuIfExists(recentProducts, p.getSku());
                }
            }
            if (!mUpdateDataOnly) {
                // if data was updated before and should ot be appended to the
                // list
                recentProducts.addAll(0, mData);
            }
            // remove all products above max allowed number
            for (int i = recentProducts.size() - 1; i >= RECENT_PRODUCTS_LIST_CAPACITY; i--) {
                recentProducts.remove(i);
            }
            mRecentProductsAdapter.notifyDataSetChanged();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                mData = new ArrayList<Product>();
                if (mSku != null) {
                    ProductDetailsExistResult existResult = JobCacheManager
                            .productDetailsExist(mSku, mSettingsSnapshot.getUrl(), true);
                    if (existResult.isExisting()) {
                        Product p = JobCacheManager.restoreProductDetails(existResult.getSku(),
                                mSettingsSnapshot.getUrl());
                        if (p != null) {
                            mData.add(p);
                        }
                    } else {
                        CommonUtils
                        .debug(TAG,
                                "LoadRecentProductsTask.doInBackground: recent product with SKU %1$s doesn't have cached details",
                                mSku);
            }
                } else {
                    ArrayList<GalleryTimestampRange> galleryTimestampsRangesArray = JobCacheManager
                            .getGalleryTimestampRangesArray();

                    if (galleryTimestampsRangesArray != null) {
                        Set<String> addedProducts = new HashSet<String>();
                        for (int i = galleryTimestampsRangesArray.size() - 1; i >= 0; i--) {
                            if (isCancelled()) {
                                return false;
                            }
                            GalleryTimestampRange gts = galleryTimestampsRangesArray.get(i);
                            String sku = gts.sku;
                            ProductDetailsExistResult existResult = JobCacheManager
                                    .productDetailsExist(sku, mSettingsSnapshot.getUrl(), true);
                            if (existResult.isExisting()) {
                                if (addedProducts.contains(existResult.getSku())) {
                                    CommonUtils
                                            .debug(TAG,
                                                    "LoadRecentProductsTask.doInBackground: recent product with SKU %1$s already added. Skipping.",
                                                    existResult.getSku());
                                } else {
                                    Product p = JobCacheManager.restoreProductDetails(
                                            existResult.getSku(),
                                            mSettingsSnapshot.getUrl());
                                    if (p != null) {
                                        mData.add(p);
                                        addedProducts.add(existResult.getSku());
                                    }
                                    if (mData.size() == RECENT_PRODUCTS_LIST_CAPACITY) {
                                        break;
                                    }
                                }
                            } else {
                                CommonUtils
                                        .debug(TAG,
                                                "LoadRecentProductsTask.doInBackground: recent product with SKU %1$s doesn't have cached details",
                                                sku);
                            }
                        }
                    }
                }
                return !isCancelled();
            } catch (Exception ex) {
                CommonUtils.error(TAG, ex);
            }
            return false;
        }

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
        long mExifDateTime = -1;
        String mCode = null;

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
                DetectDecodeResult ddr = multiDetector.decode(id.file.getAbsolutePath());
                ScanState scanState = null;
                ImageData lastDecodedData = null;
                if (ddr.isDecoded()) {
                    scanState = ScanState.SCANNED_DECODED;
                    CheckSkuResult checkSkuResult = ScanActivity.checkSku(ddr.getCode());
                    mCode = checkSkuResult.code;
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
                        if (TextUtils.isEmpty(mCode)) {
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
                                    List<ImageData> ids = ds.imageDataList
                                            .get(cdi.groupPosition - 1);
                                    for (ImageData id2 : ids) {
                                        if (id2.getScanState() == ScanState.SCANNED_DECODED) {
                                            ImagesLoader.queueImage(newFile, previousGroup.sku,
                                                    true, false);
                                        }
                                    }
                                }
                            }
                        } else {
                            cameraSyncCode = false;
                                cameraSyncCode = mCode.startsWith(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX);
                            if (cameraSyncCode) {
                                mExifDateTime = ImageUtils.getExifDateTime(newFile
                                        .getAbsolutePath());

                                if (mExifDateTime != -1) {
                                    newFile.delete();
                                }
                            } else {
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
                                        discardLater = !TextUtils.equals(idg.sku, mCode);
                                    }
                                }
                                newFile = ImagesLoader.queueImage(newFile, mCode, true,
                                        discardLater);
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
                                            startPos, multiDetector, mCode)) {
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
            if (mCode != null) {
                if (mCode.startsWith(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX)) {
                    mCurrentBeep = checkConditionAndSetCameraTimeDifference(mCode, mExifDateTime,
                            settings, mCurrentBeep, false, true, new Runnable() {
                                public void run() {
                                    if(mLastRequestedMatchByTimeFileNameWithSyncRecommendation != null)
                                    {
                                        mMatchingByTimeCheckConditionTask = new MatchingByTimeCheckConditionTask(
                                                mLastRequestedMatchByTimeFileNameWithSyncRecommendation,
                                                false);
                                        mMatchingByTimeCheckConditionTask.execute();
                                    }
                                }
                            });
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
                ExternalImagesJobQueue.updateExternalImagesCount();
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
    
    /**
     * Enumeration describing possible cases for the DeleteAllTask
     */
    enum DeleteAllOption {
        LEFT, RIGHT, ALL
    }

    /**
     * The task which deletes set of images from the Images strip. It may be
     * either all images or images to the left or to the right of the selected
     * image
     */
    class DeleteAllTask extends DataModifierTask {
        /**
         * The file name for the item where the context menu was displayed
         */
        String mFileName;
        /**
         * The delete option. Either ALL or LEFT or RIGHT
         */
        DeleteAllOption mDeleteAllOption;
        /**
         * The list of files to delete. List is filled in the doInBackground
         * method
         */
        List<File> mFilesToRemove = new ArrayList<File>();

        public DeleteAllTask(String fileName, DeleteAllOption deleteAllOption) {
            super(mDeletingLoadingControl);
            mFileName = fileName;
            mDeleteAllOption = deleteAllOption;
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            try {
                if (isCancelled()) {
                    return false;
                }
                // get the copy of the images strip data. We need to work with a
                // copy such as data in the image strip may be changed at any
                // moment
                DataSnapshot ds = getDataSnapshot();
                // if Delete all option was specified
                if (mDeleteAllOption == DeleteAllOption.ALL) {
                    processDataSnapshot(ds, null, 0, ds.dataSnapshot.size());
                } else {
                    // check whether the file the context menu was displayed for
                    // still exists
                    File file = new File(mFileName);
                    if (!file.exists()) {
                        CommonUtils.debug(TAG,
                                "DeleteAllTask.doInBackground: file %1$s is not found, returning",
                                mFileName);
                        return false;
                    }
                    CurrentDataInfo cdi = ds.getCurrentDataInfoForFileName(mFileName);
                    if (cdi != null) {
                        // if Delete all right option was specified
                        if (mDeleteAllOption == DeleteAllOption.RIGHT) {
                            processDataSnapshot(ds, cdi, cdi.groupPosition, ds.dataSnapshot.size());
                        } else {
                            // if Delete all left option was specified
                            processDataSnapshot(ds, cdi, 0, cdi.groupPosition + 1);
                        }
                    }
                }
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

        /**
         * Process data snapshot. Iterate through the images and populate
         * mFilesToRemove field
         * 
         * @param ds the DataSnapshot which contains all the necessary data
         * @param cdi the current data info representing information about the
         *            item where the context menu was displayed
         * @param startPos the starting position to iterate from (including)
         * @param endPos the ending position to iterate to (excluding)
         * @return
         */
        private boolean processDataSnapshot(DataSnapshot ds, CurrentDataInfo cdi, int startPos,
                int endPos) {
            for (int i = startPos; i < endPos; i++) {
                List<ImageData> imagesList = ds.imageDataList.get(i);
                int startPos2 = 0;
                int endPos2 = imagesList.size();
                // special case when the iteration index equals to the item
                // where the context menu was displayed group position
                if (cdi != null && i == cdi.groupPosition) {
                    // adjust startPos2 and endPos2 depend on delete all option
                    if (mDeleteAllOption == DeleteAllOption.RIGHT) {
                        startPos2 = cdi.inGroupPosition + 1;
                    } else {
                        endPos2 = cdi.inGroupPosition;
                    }
                }

                if (!processImageDataGroup(imagesList, startPos2, endPos2)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Process single image data group
         * 
         * @param data a list of ImageData information
         * @param startPos the starting position to iterate from (including)
         * @param endPos the ending position to iterate to (excluding)
         * @return
         */
        private boolean processImageDataGroup(List<ImageData> data, int startPos, int endPos) {
            for (int i = startPos; i < endPos; i++) {
                ImageData id = data.get(i);
                CommonUtils.debug(TAG,
                        "DeleteAllTask.processImageDataGroup: adding image file %1$s to list",
                        id.file.getAbsolutePath());
                mFilesToRemove.add(id.file);
                if (isCancelled()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        void nullifyTask() {
            mDeleteAllTask = null;
        }

        @Override
        protected void onSuccessPostExecute() {
            super.onSuccessPostExecute();
            GuiUtils.alert(R.string.main_deleting_success);
            // thumbnailsList should be scrolled to 0 position to fix invalid
            // scroll position after too many data removed
            if (mDeleteAllOption != DeleteAllOption.RIGHT && isActivityAlive()) {
                GuiUtils.post(new Runnable() {

                    @Override
                    public void run() {
                        thumbnailsList.scrollTo(0);
                    }
                });
            }
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
                        ZXingCodeScanner multiDetector = new ZXingCodeScanner(true);
                        DetectDecodeResult ddr = multiDetector.decode(file.getAbsolutePath());
                        mCode = ddr.getCode();
                    }
                    if (mCode != null) {

                        if (mCode.startsWith(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX)) {
                            mExifDateTime = ImageUtils.getExifDateTime(file.getAbsolutePath());

                            if (mExifDateTime != -1) {
                                incModifiersIfNecessary();
                                file.delete();
                            }
                        } else {
                            CheckSkuResult checkSkuResult = ScanActivity.checkSku(mCode);
                            String sku = checkSkuResult.code;
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
                    DetectDecodeResult ddr = scanner.decode(id.file.getAbsolutePath());
                    if (ddr.isDecoded()) {
                        scanState = ScanState.SCANNED_DECODED;
                        CheckSkuResult checkSkuResult = ScanActivity.checkSku(ddr.getCode());
                        sku = checkSkuResult.code;
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
                                mExifDateTime, settings, mCurrentBeep, mSilent, true, null);
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

    class ReassignSkuForGroupTask extends DataModifierTask {
        int mScreenLargerDimension;
        String mFileName;
        String mCode;

        public ReassignSkuForGroupTask(String fileName, String code) {
            super(null);
            this.mFileName = fileName;
            this.mCode = code;
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
                                    "ReassignSkuForGroupTask.doInBackground: file %1$s is not found, returning",
                                    mFileName);
                    return false;
                }

                String sku = mCode;
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
                        if (!processImageDataGroup(sku, imagesList, startPos, i)) {
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

        private boolean processImageDataGroup(String sku, List<ImageData> data, int startPos,
                int groupPosition) {
            for (int i = startPos, size2 = data.size(); i < size2; i++) {
                ImageData id = data.get(i);
                CommonUtils
                        .debug(TAG,
                                "ReassignSkuForGroupTask.processImageDataGroup: queuing file %1$s for sku %2$s",
                                id.file.getAbsolutePath(), sku);
                incModifiersIfNecessary();
                ImagesLoader.queueImage(id.file, sku, true,
                        ImagesLoader.isDecodedCode(id.file.getName()));
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

    class MatchingByTimeCheckConditionTask extends DataModifierTask {
        String mFileName;
        boolean mShowSyncRecommendation = false;
        static final long sHour = 60 * 60 * 1000;
        boolean mSilent;

        public MatchingByTimeCheckConditionTask(String fileName, boolean silent) {
            super(mMatchingByTimeStatusLoadingControl);
            this.mFileName = fileName;
            mSilent = silent;
            if (!silent) {
                mLastRequestedMatchByTimeFileNameWithSyncRecommendation = null;
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
                if (!mSilent && isActivityAlive) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

                    alert.setMessage(R.string.main_matching_by_time_sync_recommendation);

                    alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Intent intent = new Intent(MainActivity.this,
                                    CameraTimeSyncActivity.class);
                            startActivity(intent);
                            mLastRequestedMatchByTimeFileNameWithSyncRecommendation = mFileName;
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
            mLastRequestedMatchByTimeFileNameWithSyncRecommendation = null;
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
                                    id.file.getAbsolutePath(), gtr.sku);
                    incModifiersIfNecessary();
                    ImagesLoader.queueImage(id.file, gtr.sku, true, false);
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
                        thumbnailsAdapter = new ThumbnailsAdapter(MainActivity.this, mImageWorker,
                                new OnLoadedSkuUpdatedListener() {

                                    @Override
                                    public void onLoadedSkuUpdated(String fileName, String sku) {
                                        new ReassignSkuForGroupTask(fileName, sku).execute();
                                    }
                                });
                        thumbnailsList.setAdapter(thumbnailsAdapter);
                    } else {
                        thumbnailsAdapter.notifyDataSetChanged();
                    }
                    if (mCurrentState == State.NO_PHOTOS || mCurrentState == State.PHOTOS) {
                        // if one of the photos state is activated
                        if (thumbnailsAdapter.isEmpty()) {
                            // if there are no loaded images
                            setState(State.NO_PHOTOS);
                        } else {
                        	// if there are loaded images
                            setState(State.PHOTOS);
                        }
                    }
                    if (mCurrentState != State.PHOTOS && !thumbnailsAdapter.isEmpty()) {
                        // activate photos state by default if there are loaded
                        // photos
                        setState(State.PHOTOS);
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
                                        DetectDecodeResult ddr = scanner.decode(
                                                id.file.getAbsolutePath());
                                        justScanned = true;
                                        if (ddr.isDecoded()) {
                                            scanState = ScanState.SCANNED_DECODED;
                                            CheckSkuResult checkSkuResult = ScanActivity
                                                    .checkSku(ddr.getCode());
                                            sku = checkSkuResult.code;
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
        protected Bitmap processBitmap(Object data, ProcessingState state)
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
     * Touch listener for the auto scroll horizontal listview buttons
     */
    class AutoScrollTouchListener implements OnTouchListener {
        final String TAG = AutoScrollTouchListener.class.getSimpleName();

        /**
         * Type of the autoscroll
         */
        AutoScrollType mAutoScrollType;
        /**
         * The related horizontal list view
         */
        HorizontalListViewExt mHorizontalList;

        /**
         * @param autoScrollType Type of the autoscroll
         * @param horizontalList The related horizontal list view
         */
        AutoScrollTouchListener(AutoScrollType autoScrollType, HorizontalListViewExt horizontalList) {
            mAutoScrollType = autoScrollType;
            mHorizontalList = horizontalList;
        }

        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            boolean handled = false;
            switch (ev.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    CommonUtils.verbose(TAG, "onTouch: ACTION_DOWN");
                    // run auto scroll
                    mHorizontalList.autoScroll(mAutoScrollType);
                    // lock left/right drawers to do not conflict with the
                    // button press
                    setDrawersLocked(true);
                    handled = true;
                    break;
                case MotionEvent.ACTION_UP:
                    CommonUtils.verbose(TAG, "onTouch: ACTION_UP");
                    // cancel auto scroll
                    mHorizontalList.autoScroll(AutoScrollType.NONE);
                    // unlock previously locked drawers
                    setDrawersLocked(false);
                    handled = true;
                    break;
            }
            return handled;
        }
    }
    /**
     * Extension of HorizontalListView which moves group description to be
     * visible on scroll for the first child. Had to use custom component
     * instead of layout listeners because they are available only since api 11
     */
    public static class HorizontalListViewExt extends HorizontalListView {

        static final String TAG = HorizontalListViewExt.class.getSimpleName();

        /**
         * The type for the automatic list scroll
         */
        enum AutoScrollType {
        	/**
        	 * No automatic scrolling
        	 */
            NONE, 
            /**
             * Automatic left scroll
             */
            LEFT, 
            /**
             * Automatic right scroll
             */
            RIGHT
        }

        /**
         * The current automatic scroll type
         */
        AutoScrollType mAutoScrollType = AutoScrollType.NONE;
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

        Runnable mAutoScrollRunnable = new Runnable() {

            @Override
            public void run() {
                if (mScroller.isFinished() && mAutoScrollType != AutoScrollType.NONE) {
                	// scroll step size. Currently listview width divided by 2
                    int shift = getWidth() / 2;
                    int offset = getStartX()
                            + (mAutoScrollType == AutoScrollType.LEFT ? -shift : shift);
                    // be sure new offset is more than zero
                    offset = Math.max(0, offset);
                    // be sure that new offset is not larger than max possible
                    offset = Math.min(offset, getMaxX());
                    if (offset != getStartX()) {
                        // if offset is not the same as current one
                        scrollTo(offset);
                    }
                }
            }
        };
        
        /**
         * The related scroll view indicator.
         */
        HorizontalScrollView mHorizontalScrollView;
        /**
         * The view within mHorizontalScrollView. The width of the view controls
         * scroll thumb size.
         */
        View mStretchingView;
        
        /**
         * The view which indicates scroll left is available
         */
        View mLeftScrollAvailableView;
        /**
         * The view which indicates scroll right is available
         */
        View mRightScrollAvailableView;

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
            if (mScroller.isFinished() && mAutoScrollType != AutoScrollType.NONE) {
                // if scroll is done and auto scroll type is specified
                post(mAutoScrollRunnable);
            }
            // update related scroll indicator
            updateScrollIndicator();
            // update related auto scroll controls
            updateScrollAvailableViewsVisibility();
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
            CommonUtils.verbose(TAG, "dispatchTouchEvent: MotionEvent %1$s", ev.toString());
            switch (ev.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    int pointerCount = ev.getPointerCount();
                    CommonUtils.verbose(TAG, 
                            "dispatchTouchEvent: ACTION_POINTER_DOWN pointerCount: %1$d",
                            pointerCount);
                    // pointer down event. Check whether the 2 fingers are down
                    // and set the flag if they are. Also fire on2FingersDown
                    // event
                    if (pointerCount == 2) {
                        m2FingersDetected = true;
                        if (mOn2FingersDownListener != null) {
                            mOn2FingersDownListener.on2FingersDown(ev);
                        }
                    }
                    handled = true;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    // finger up event
                    CommonUtils.verbose(TAG, "dispatchTouchEvent: ACTION_POINTER_UP");
                    handled = true;
                    break;
                case MotionEvent.ACTION_DOWN:
                    // one finger down event. Reset the 2 fingers detected flag
                    CommonUtils.verbose(TAG, "dispatchTouchEvent: ACTION_DOWN");
                    handled = true;
                    m2FingersDetected = false;
                    // cancel automatic scroll
                    autoScroll(AutoScrollType.NONE);
                    break;
                case MotionEvent.ACTION_UP:
                    // one finger up event. Fire onUp event if 2 fingers flag is
                    // specified. This is necessary because if 2 fingers flag is
                    // true then super.dispatchTouchEvent call are suppressed
                    CommonUtils.verbose(TAG, 
                            "dispatchTouchEvent: ACTION_UP m2FingersDetected: %1$b",
                            m2FingersDetected);
                    if (m2FingersDetected) {
                        onUp(ev);
                    }
                    break;
            }
            CommonUtils.verbose(TAG, "dispatchTouchEvent: m2FingersDetected: %1$b",
                    m2FingersDetected);
            // suppress super.dispatchTouchEvent call in case 2 fingers touch is
            // detected to avoid false onLongClick event for the child items
            if (!m2FingersDetected) {
                handled |= super.dispatchTouchEvent(ev);
            }
            return handled;
        }

        public void setOn2FingersDownListener(On2FingersDownListener listener) {
            this.mOn2FingersDownListener = listener;
        }

        /**
         * Set the list automatic scroll type.
         * 
         * @param autoScrollType the automatic scroll type
         */
        public void autoScroll(AutoScrollType autoScrollType) {
            mAutoScrollType = autoScrollType;
            if (mAutoScrollType == AutoScrollType.NONE) {
                // if NONE automatic scroll type is specified
            	//
            	// cancel all scheduled auto scroll actions
                removeCallbacks(mAutoScrollRunnable);
            } else {
            	// start automatic scrolling
                mAutoScrollRunnable.run();
            }
        }

        /**
         * Set the related scroll indicator view. This view will be linked with
         * this horizontal listview to indicate scroll size and position
         * 
         * @param horizontalScrollView the HorizontalScrollView widget to link
         *            with
         */
        public void setScrollIndicator(HorizontalScrollView horizontalScrollView) {
            mHorizontalScrollView = horizontalScrollView;
            // hack to cancel all touch events handling by the
            // horizontalScrollView
            mHorizontalScrollView.setOnTouchListener(new OnTouchListener() {
                
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            mStretchingView = mHorizontalScrollView.getChildAt(0);
            updateScrollIndicator();
        }
        
        /**
         * Update the related scroll indicator if exists with the new width and
         * offset
         */
        public void updateScrollIndicator() {
            if (mHorizontalScrollView != null && mStretchingView != null) {
                // if there is linked scroll indicator
            	//
                // update child view width to be he same as max offset of the
                // list
                LayoutParams layoutParams = mStretchingView.getLayoutParams();
                layoutParams.width = getMaxX() == Integer.MAX_VALUE ? getStartX() : getMaxX()
                        + getWidth();
                mStretchingView.setLayoutParams(layoutParams);
                // update scroller position
                mHorizontalScrollView.setScrollX(getStartX());
            }
        }
        
        /**
         * Set the related views which indicates scrolling left/right is
         * available or not
         * 
         * @param leftScrollView the view which indicates scroll left is
         *            available. Can be null.
         * @param rightScrollView the view which indicates scroll right is
         *            available. Can be null.
         */
        public void setScrollAvailableViews(View leftScrollView, View rightScrollView) {
            mLeftScrollAvailableView = leftScrollView;
            mRightScrollAvailableView = rightScrollView;
        }

        /**
         * Update the visibility of scroll left/right indicators if they are
         * exists depend on whether the scrolling left/right is possible
         */
        public void updateScrollAvailableViewsVisibility() {
            if (mLeftScrollAvailableView != null) {
                mLeftScrollAvailableView.setVisibility(getStartX() > 0 ? View.VISIBLE : View.GONE);
            }
            if (mRightScrollAvailableView != null) {
                mRightScrollAvailableView.setVisibility(getStartX() < getMaxX() ? View.VISIBLE : View.GONE);
            }
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
        /**
         * The text color of the error messages
         */
        int mErrorTextColor;
        WeakReference<Activity> mContext;
        public CurrentDataInfo currentData;
        public boolean longClicked = false;
        private static SerialExecutor sCacheLoaderExecutor = new SerialExecutor(
                Executors.newSingleThreadExecutor());
        private OnLoadedSkuUpdatedListener mOnLoadedSkuUpdatedListener;

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

        public ThumbnailsAdapter(Activity context, ImageResizer imageWorker,
                OnLoadedSkuUpdatedListener onLoadedSkuUpdatedListener)
        {
            super();
            this.mContext = new WeakReference<Activity>(context);
            mThumbGroupBorder = context.getResources().getDimensionPixelSize(
                    R.dimen.home_thumbnail_group_border);
            mItemHeight = context.getResources().getDimensionPixelSize(
                    R.dimen.home_thumbnail_with_border_size);
            mErrorTextColor = context.getResources().getColor(R.color.main_images_strip_error_text);
            mSettingsSnapshot = new SettingsSnapshot(context);
            this.mImageWorker = imageWorker;
            this.mInflater = LayoutInflater.from(context);
            mItemBorder = context.getResources().getDimensionPixelSize(
                    R.dimen.home_thumbnail_border);
            mOnLoadedSkuUpdatedListener = onLoadedSkuUpdatedListener;
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
                    holder.name.setTextColor(mErrorTextColor);
                } else {
                    nameText = CommonUtils.getStringResource(R.string.main_name_text, idg.name);
                }
            } else {
                if (idg.doesntExist.get()) {
                    nameText = CommonUtils.getStringResource(R.string.main_load_doesnt_exist);
                    holder.name.setTextColor(mErrorTextColor);
                } else if (idg.loadFailed.get()) {
                    nameText = CommonUtils.getStringResource(R.string.main_load_failed);
                    holder.name.setTextColor(mErrorTextColor);
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
            private String mRetrievedSku;
            private boolean doesntExist;
            private boolean dontPerformServerOperation;
            private int requestId = MageventoryConstants.INVALID_REQUEST_ID;
            boolean mSkuChanged;

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
                                                "CacheLoaderTask.doInBackground: success loading for sku: %1$s; retrieved sku: %2$s",
                                                idg.sku, mRetrievedSku);
                                updateImageDataGroupFromProduct(mRetrievedSku);
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
                    mSkuChanged = !idg.sku.equals(p.getSku());
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
                    if (mSkuChanged && mOnLoadedSkuUpdatedListener != null) {
                        synchronized (idg.data) {
                            if (idg.data.size() > 0) {
                                mOnLoadedSkuUpdatedListener.onLoadedSkuUpdated(idg.data.get(0)
                                        .getFile().getAbsolutePath(), idg.sku);
                            }
                        }
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
                    if (success) {
                        Bundle extras = op.getExtras();
                        if (extras != null
                                && extras.getString(MageventoryConstants.MAGEKEY_PRODUCT_SKU) != null) {
                            mRetrievedSku = extras
                                    .getString(MageventoryConstants.MAGEKEY_PRODUCT_SKU);
                        } else {
                            CommonUtils.error(TAG, CommonUtils.format(
                                    "API response didn't return SKU information for the sku: %1$s",
                                    idg.sku));
                        }
                    }
                    doneSignal.countDown();
                }
            }
        }

        static interface OnLoadedSkuUpdatedListener {
            void onLoadedSkuUpdated(String fileName, String sku);
        }
    }

    static class RecentProductsAdapter extends BaseAdapter implements OnClickListener {
        List<Product> recentProducts = new ArrayList<Product>();
        LinearLayout recentProductsList;
        Activity mActivity;

        public RecentProductsAdapter(Activity activity) {
            mActivity = activity;
            recentProductsList = (LinearLayout) mActivity.findViewById(R.id.recentProductsList);
        }

        @Override
        public int getCount() {
            return recentProducts.size();
        }

        @Override
        public Product getItem(int position) {
            return recentProducts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh;
            if (convertView == null) {
                convertView = mActivity.getLayoutInflater().inflate(
                        R.layout.main_item_recent_product,
                        parent, false);
                convertView.setOnClickListener(this);
                vh = new ViewHolder();
                vh.text = (TextView) convertView.findViewById(android.R.id.text1);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            vh.text.setText(getItem(position).getName());
            vh.position = position;
            return convertView;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();

            GuiUtils.post(new Runnable() {

                @Override
                public void run() {
                    int childsCount = recentProductsList.getChildCount();
                    for (int i = 0, size = getCount(); i < size; i++) {
                        View convertView = null;
                        if (childsCount > i) {
                            convertView = recentProductsList.getChildAt(i);
                        }
                        View v = getView(i, convertView, recentProductsList);
                        if (convertView == null) {
                            recentProductsList.addView(v);
                        }
                    }
                    for (int i = getCount(), size = recentProductsList.getChildCount(); i < size; i++) {
                        recentProductsList.removeViewAt(i);
                    }
                }
            });
        }

        class ViewHolder {
            TextView text;
            int position;
        }

        @Override
        public void onClick(View v) {
            ViewHolder vh = (ViewHolder) v.getTag();
            Product p = getItem(vh.position);
            ScanActivity.startForSku(p.getSku(), mActivity);
        }

        public boolean removeProductForSkuIfExists(String sku) {
            boolean result;
            result = removeProductForSkuIfExists(recentProducts, sku);
            if (result) {
                notifyDataSetChanged();
            }
            return result;
        }

        public static boolean removeProductForSkuIfExists(List<Product> recentProducts, String sku) {
            boolean result = false;
            for (int j = recentProducts.size() - 1; j >= 0; j--) {
                Product p2 = recentProducts.get(j);
                if (sku.equals(p2.getSku())) {
                    recentProducts.remove(j);
                    result = true;
                }
            }
            return result;
        }

        /**
         * Get the index of the product element in the adapter with the given
         * SKU
         * 
         * @param sku the SKU of the product
         * @return index of the product element with the given SKU if found, -1
         *         in case product with the given SKU is absent
         */
        public int getProductPosition(String sku) {
            return getProductPosition(recentProducts, sku);
        }

        /**
         * Get the index of the product element with the given SKU in the
         * recentProducts list
         * 
         * @param recentProducts the list of the products where the search
         *            should be performed
         * @param sku the SKU of the product
         * @return index of the product element with the given SKU if found, -1
         *         in case product with the given SKU is absent
         */
        public static int getProductPosition(List<Product> recentProducts, String sku) {
            int result = -1;
            // iterate through recentProducts and find the index of the product
            // with the given SKU
            for (int j = recentProducts.size() - 1; j >= 0; j--) {
                Product p = recentProducts.get(j);
                if (sku.equals(p.getSku())) {
                    // given SKU matches with the product SKU, remember the
                    // index and interrupt the loop
                    result = j;
                	break;
                }
            }
            return result;
        }
    }

    public class CheckEyeFiStateTask extends SimpleAsyncTask {

        static final String EYE_FI_PACKAGE = "fi.eye.android";
        /**
         * The name of the Eye-Fi service default intent filter action
         */
        static final String EYE_FI_SERVICE = "fi.eye.android.services.EyeFiMediaService";

        public CheckEyeFiStateTask() {
            super(null);
        }

        @Override
        protected void onSuccessPostExecute() {

        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                long start = System.currentTimeMillis();
                boolean eyeFiInstalled = isPackageInstalled(EYE_FI_PACKAGE);
                boolean eyeFiRunning = false;
                if (eyeFiInstalled) {
                    ActivityManager activityManager = (ActivityManager) MyApplication.getContext()
                            .getSystemService(Activity.ACTIVITY_SERVICE);
                    for (RunningAppProcessInfo processInfo : activityManager
                            .getRunningAppProcesses()) {
                        if (processInfo.processName.equals(EYE_FI_PACKAGE)) {
                            eyeFiRunning = true;
                            break;
                        }
                    }
                }
                // if Eye-Fi is installed and not running then try to run the
                // Eye-Fi service
                if (eyeFiInstalled && !eyeFiRunning) {
                    Log.d(TAG, "EyeFi installed but not running. Trying to start the service");
                    Intent intent = new Intent(EYE_FI_SERVICE);
                    startService(intent);
                }
                long runningTime = System.currentTimeMillis() - start;
                String message = CommonUtils
                        .format("EyeFi state: installed %1$b, running %2$b, check state running time %3$d ms",
                                eyeFiInstalled, eyeFiRunning, runningTime);
                Log.d(TAG, message);
                TrackerUtils.trackBackgroundEvent("eyeFiState", CommonUtils.format(
                        "installed %1$b, running %2$b", eyeFiInstalled, eyeFiRunning));
                TrackerUtils.trackDataLoadTiming(runningTime, "checkEyeFiState", TAG);
                return !isCancelled();
            } catch (Exception e) {
                CommonUtils.error(TAG, e);
            }
            return false;
        }

        private boolean isPackageInstalled(String packagename) {
            PackageManager pm = MyApplication.getContext().getPackageManager();
            try {
                pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
                return true;
            } catch (NameNotFoundException e) {
                return false;
            }
        }

    }

    /**
     * BroadcastReceiver for the {@link Intent.#ACTION_MEDIA_MOUNTED} event. The
     * receiver reloads thumbnails list and restarts file observation when the
     * sd card is mounted
     * 
     * @author Eugene Popovich
     */
    class MediaMountedReceiver extends BroadcastReceiver {

        final String TAG = MediaMountedReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            CommonUtils.debug(TAG, "onReceive: Received media mounted event %1$s",
                    intent == null ? ""
                    : intent.toString());
            Uri uri = intent.getData();
            if (uri != null) {
                CommonUtils.debug(TAG, "onReceive: Received media mounted event for uri path %1$s",
                        uri.getPath());
                // log whether the photos path contains mounted media path but
                // reload list in any case.
                //
                // We need to collect some log information. For a now we can't
                // be sure that some sdcard alias is not used for the photos
                // directory instead of absolute path. So app reloads list in
                // case any media is mounted but tracks information for future
                // use
                String photosPath = settings.getGalleryPhotosDirectory();
                String message = CommonUtils
                        .format("MediaMountedReceiver.onReceive: the photosPath %1$s, mounted media path %2$s, photos path is on that media %3$b",
                                photosPath, uri.getPath(), photosPath.contains(uri.getPath()));
                Log.d(TAG, message);
                TrackerUtils.trackBackgroundEvent("mediaMountedCheck", message);
                reloadThumbs();
                restartObservation();
            }
        }

    }
}
