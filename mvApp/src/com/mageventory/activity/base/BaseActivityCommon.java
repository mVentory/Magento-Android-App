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

package com.mageventory.activity.base;

import java.lang.reflect.Field;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.DataSetObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mageventory.activity.HelpActivity;
import com.mageventory.settings.Settings;
import com.mageventory.tasks.ErrorReportCreation;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.DefaultOptionsMenuHelper;
import com.mageventory.util.EventBusUtils.BroadcastReceiverRegisterHandler;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.Log;
import com.mageventory.util.Log.OnErrorReportingFileStateChangedListener;
import com.mageventory.util.security.Security;
import com.mventory.R;

/* This class helps us overcome the lack of multiple inheritance in java.
 * We want to have two classes from which all activities extend (either from one or from the other). Those are:
 * BaseActivity and BaseListActivity.
 *  
 * We want to BaseActivity to extend Activity and we want BaseListActivity to extend ListActivity. At the same time
 * we want both of these base classes to have some common methods that we implement. We can't inherit from any more classes
 * so we created a separate class which is BaseActivityCommon. */
public class BaseActivityCommon<T extends Activity & BroadcastReceiverRegisterHandler> {

    static final String TAG = BaseActivityCommon.class.getSimpleName();

    /*
     * This is needed for the following issue:
     * http://code.google.com/p/mageventory/issues/detail?id=199 It keeps track
     * of whether product creation activity is supposed to load last used
     * category and attribute set automatically or not.
     */
    public static boolean sNewNewReloadCycle = false;

    private T mActivity;
    DrawerLayout mDrawerLayout;
    ViewTreeObserver.OnGlobalLayoutListener mRightDrawerLayoutListener;
    ListView mRightDrawerList;
    private Button mErrorReportingButton;
    private int mButtonDefaultTextColor;
    private boolean mErrorReportingLastLogOnly;
    /**
     * Keeps initial requested orientation of the activity
     */
    private int mInitialOrientation;
    
    /**
     * Flag indicating that the invalid license dialog was already shown
     */
    boolean mLicenseDialogActive = false;

    /**
     * Flag indicating whether activity was launched with the
     * {@link Intent#FLAG_ACTIVITY_REORDER_TO_FRONT} flag
     */
    boolean mIsRestoredToTop = false;

    public BaseActivityCommon(T activity)
    {
        mActivity = activity;
        mDrawerLayout = (DrawerLayout) mActivity.findViewById(R.id.drawer_layout);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return DefaultOptionsMenuHelper.onCreateOptionsMenu(mActivity, menu);
    }

    public void onCreate() {
        ActionBar actionBar = mActivity.getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            try {
                String versionName = mActivity.getPackageManager().getPackageInfo(
                        mActivity.getPackageName(), 0).versionName;
                ((TextView) mActivity.findViewById(R.id.versionName)).setText("v. " + versionName);
            } catch (NameNotFoundException e) {
                CommonUtils.error(TAG, e);
            }
        }
        final TextView host_url = (TextView) mActivity.findViewById(R.id.config_state);
        if (host_url != null) {
            Settings settings = new Settings(mActivity.getApplicationContext());
            host_url.setVisibility(settings.hasSettings() ? View.VISIBLE : View.GONE);
            if (settings.hasSettings()) {
                host_url.setTag(settings.getUrl());
                host_url.setText(settings.getProfileName());
                host_url.setOnLongClickListener(new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(host_url.getTag().toString()));
                        mActivity.startActivity(i);
                        closeDrawers();
                        return true;
                    }
                });
            }
        }
        initHelp();
        initMenu();

        mActivity.setVolumeControlStream(AudioManager.STREAM_NOTIFICATION);
        mInitialOrientation = mActivity.getRequestedOrientation();
    }

    public void onDestroy() {
        GuiUtils.removeGlobalOnLayoutListener(mRightDrawerList, mRightDrawerLayoutListener);
    }

    /**
     * The method which should be called by the using activity in its onResume()
     * method
     */
    public void onResume() {
        resetRequestedOrientationIfNecessary();
    }

    /**
     * The method which should be called by the using activity in its
     * onNewIntent() method
     */
    public void onNewIntent(Intent intent) {
        if ((intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) > 0) {
            // if activity was launched with the FLAG_ACTIVITY_REORDER_TO_FRONT
            // flag
            mIsRestoredToTop = true;
        }
    }
    
    /**
     * The method which should be called by the using activity in its finish()
     * method
     */
    public void finish() {
    	//overcome the issue https://code.google.com/p/android/issues/detail?id=63570#c15
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT
                && !mActivity.isTaskRoot() && mIsRestoredToTop) {
            // 4.4.2 platform issues for FLAG_ACTIVITY_REORDER_TO_FRONT,
            // reordered activity back press will go to home unexpectly,
            // Workaround: move reordered activity current task to front when
            // it's finished.
            ActivityManager tasksManager = (ActivityManager) mActivity
                    .getSystemService(Context.ACTIVITY_SERVICE);
            tasksManager.moveTaskToFront(mActivity.getTaskId(),
                    ActivityManager.MOVE_TASK_NO_USER_ACTION);
        }
    }
    /**
     * Reset the activity requested orientation to the initial value if it was
     * updated. This is used to prevent orientation locking when returning from
     * the scan activity (orientation gets locked when scan is called)
     */
    public void resetRequestedOrientationIfNecessary() {
        if (mActivity.getRequestedOrientation() != mInitialOrientation) {
            // if requested orientation was changed
            mActivity.setRequestedOrientation(mInitialOrientation);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() != R.id.menu_new 
                && item.getItemId() != R.id.menu_refresh
                && item.getItemId() != R.id.menu_scan
                && item.getItemId() != R.id.menu_menu
                && item.getItemId() != R.id.menu_help
                )
        {
            /*
             * If the user didn't select one of the above options from the menu
             * then we are breaking the NewNewReloadCycle
             */
            BaseActivityCommon.sNewNewReloadCycle = false;
        }

        return DefaultOptionsMenuHelper.onOptionsItemSelected(mActivity, item);
    }

    public void hideKeyboard() {
        View currentFocus = mActivity.getCurrentFocus();

        if (currentFocus != null)
        {
            InputMethodManager inputManager = (InputMethodManager) mActivity
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    /**
     * Init the left drawer which contains help, currently contains fake values
     */
    void initHelp() {
        mErrorReportingButton = (Button) mActivity.findViewById(R.id.reportErrorsBtn);
        mButtonDefaultTextColor = mErrorReportingButton.getCurrentTextColor();

        mErrorReportingButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showErrorReportingQuestion();
            }
        });
        Log.OnErrorReportingFileStateChangedListener errorReportingFileStateChangedListener = new OnErrorReportingFileStateChangedListener() {

            @Override
            public void onErrorReportingFileStateChanged(boolean fileExists) {
                if (fileExists) {
                    GuiUtils.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            mErrorReportingLastLogOnly = false;
                            mErrorReportingButton.setEnabled(true);
                            mErrorReportingButton.setText(R.string.report_errors);
                        }
                    });
                } else {
                    GuiUtils.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            mErrorReportingLastLogOnly = true;
                            mErrorReportingButton.setEnabled(true);
                            mErrorReportingButton.setTextColor(mButtonDefaultTextColor);
                            mErrorReportingButton.setText(R.string.report_status);
                        }
                    });
                }
            }
        };
        Log.registerOnErrorReportingFileStateChangedBroadcastReceiver(TAG,
                errorReportingFileStateChangedListener, mActivity);
        ListView mDrawerList = (ListView) mActivity.findViewById(R.id.left_drawer);

        if (mDrawerList != null) {
            // Set the adapter for the list view
            mDrawerList.setAdapter(new ArrayAdapter<String>(mActivity, R.layout.help_list,
                    android.R.id.text1, mActivity.getResources().getStringArray(
                            R.array.help_items_text)) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View result = super.getView(position, convertView, parent);
                    result.findViewById(R.id.bottom_vertical).setVisibility(
                            position == getCount() - 1 ? View.INVISIBLE : View.VISIBLE);
                    return result;
                }
            });
            // Set the list's click listener
            mDrawerList.setOnItemClickListener(new OnItemClickListener() {
                String[] mUrls = mActivity.getResources().getStringArray(R.array.help_items_urls);
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        HelpActivity.launchHelp(mUrls[position], mActivity);
                        closeDrawers();
                    } catch (Exception ex) {
                        CommonUtils.error(TAG, ex);
                    }
                }

            });
        }
    }

    public void showErrorReportingQuestion() {

        AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);

        alert.setTitle(R.string.report_errors_dialog_title);
        alert.setMessage(R.string.report_errors_dialog_message);

        alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ErrorReportCreation errorReportCreationTask = new ErrorReportCreation(mActivity,
                        mErrorReportingLastLogOnly);
                errorReportCreationTask.execute();
            }
        });

        alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }
    
    /**
     * init the sliding navigation menu in the right navigation drawer
     */
    void initMenu() {
        mRightDrawerList = (ListView) mActivity.findViewById(R.id.right_drawer);

        if (mRightDrawerList != null) {
            Menu menu = GuiUtils.newMenuInstance(mActivity);
            new MenuInflater(mActivity).inflate(R.menu.navigation_menu, menu);
            final MenuAdapter adapter = new MenuAdapter(menu, LayoutInflater.from(mActivity));
            mRightDrawerList.setAdapter(adapter);
            mRightDrawerList.setOnItemClickListener(new OnItemClickListener() {


                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    MenuItem mi = adapter.getItem(position);
                    OnMenuItemClickListener listener = getOnMenuItemClickListener(mi);
                    if (listener != null) {
                        // if custom OnMenuItemClickListener is present in the
                        // menu item
                        listener.onMenuItemClick(mi);
                    } else {
                        mActivity.onOptionsItemSelected(mi);
                    }
                    closeDrawers();
                }

                /**
                 * Get the menu item onMenuItemClickListener via the Reflection
                 * API. No way to access it via public API yet
                 * 
                 * @param menuItem
                 * @return
                 */
                OnMenuItemClickListener getOnMenuItemClickListener(MenuItem menuItem) {
                    try {
                        Field f = menuItem.getClass().getDeclaredField("mClickListener");
                        f.setAccessible(true);
                        OnMenuItemClickListener result = (OnMenuItemClickListener) f.get(menuItem);
                        return result;
                    } catch (Exception ex) {
                        CommonUtils.error(TAG, ex);
                    }
                    return null;
                }
            });
            final View showMoreView = mActivity.findViewById(R.id.show_more_view);
            final FrameLayout showMoreControl = (FrameLayout) showMoreView
                    .findViewById(R.id.show_more_control);
            showMoreControl.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    GuiUtils.post(new Runnable() {

                        @Override
                        public void run() {
                            hideShowMoreOption(showMoreView);
                            mRightDrawerList.smoothScrollToPosition(mRightDrawerList.getCount() - 1);
                        }
                    });
                }
            });

            adapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    checkShowMoreVisible(mRightDrawerList, showMoreView);
                }

            });

            mRightDrawerList.setOnScrollListener(new OnScrollListener() {

                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {

                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                        int totalItemCount) {
                    if (showMoreView.getVisibility() != View.GONE && firstVisibleItem != 0) {
                        hideShowMoreOption(showMoreView);
                    }
                }

            });
            
            mRightDrawerLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                int lastHeight = 0;
                @Override
                public void onGlobalLayout() {
                    if (lastHeight != mRightDrawerList.getHeight()) {
                        checkShowMoreVisible(mRightDrawerList, showMoreView);
                        lastHeight = mRightDrawerList.getHeight();
                    }
                }
            };

            mRightDrawerList.getViewTreeObserver().addOnGlobalLayoutListener(mRightDrawerLayoutListener);

            mDrawerLayout.setDrawerListener(new DrawerListener() {

                @Override
                public void onDrawerStateChanged(int arg0) {

                }

                @Override
                public void onDrawerSlide(View arg0, float arg1) {

                }

                @Override
                public void onDrawerOpened(View view) {
                    if (mDrawerLayout.isDrawerOpen(Gravity.END)) {
                        checkShowMoreVisible(mRightDrawerList, showMoreView);
                    }
                    // request the focus to correctly process pressed back key
                    mDrawerLayout.requestFocus();
                }

                @Override
                public void onDrawerClosed(View arg0) {

                }
            });
        }
    }

    void hideShowMoreOption(final View showMoreView) {
        Animation animation = AnimationUtils.loadAnimation(mActivity, android.R.anim.fade_out);
        long animationDuration = 500;
        animation.setDuration(animationDuration);
        showMoreView.startAnimation(animation);
        showMoreView.setVisibility(View.GONE);
    }

    void showShowMoreOption(final View showMoreView) {
        Animation animation = AnimationUtils.loadAnimation(mActivity, android.R.anim.fade_in);
        long animationDuration = 500;
        animation.setDuration(animationDuration);
        showMoreView.startAnimation(animation);
        showMoreView.setVisibility(View.VISIBLE);
    }

    public void checkShowMoreVisible(final ListView drawerList, final View showMoreView) {
        GuiUtils.post(new Runnable() {

            @Override
            public void run() {
                int last = drawerList.getLastVisiblePosition();
                boolean itemsFit = last == drawerList.getCount() - 1
                        && drawerList.getChildAt(drawerList.getChildCount() - 1).getBottom() <= drawerList
                                .getHeight();
                if (itemsFit) {
                    if (showMoreView.getVisibility() == View.VISIBLE) {
                        hideShowMoreOption(showMoreView);
                    }
                } else {
                    if (showMoreView.getVisibility() == View.GONE) {
                        showShowMoreOption(showMoreView);
                    }
                }
            }
        });
    }

    /**
     * Close all navigation drawers
     */
    public void closeDrawers() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawers();
        }
    }

    /**
     * Lock/unlock drawers
     * 
     * @param locked
     */
    public void setDrawersLocked(boolean locked) {
        if (mDrawerLayout != null) {
            mDrawerLayout.setDrawerLockMode(locked ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                    : DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    /**
     * set the content view to the drawer layout content frame
     * 
     * @param id
     */
    void setContentView(int id) {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        ViewGroup root = (ViewGroup) mActivity.findViewById(R.id.content_frame);
        inflater.inflate(id, root, true);
    }

    /**
     * Adjust visibility of the sliding menu
     */
    public void toggleMenuVisibility() {
        DefaultOptionsMenuHelper.toggleMenuVisibility(mActivity);
    }

    /**
     * Verify the license information. If license information is invalid the
     * alert dialog will be shown with exit/configure options
     */
    public void verifyLicense() {
        if (mLicenseDialogActive) {
            // if the dialog was already shown before
            return;
        }
        Settings settings = new Settings(mActivity);
        if (settings.hasSettings()) {
            // if there are configured profiles
            String message = null;
            if (!Security.verifyLicense(settings.getLicense(), settings.getSignature(), true)) {
                // if license signature is invalid
                message = CommonUtils.getStringResource(R.string.invalid_license_information);
            }
            if (message == null
                    && !Security.checkStoreValid(settings.getLicense(), settings.getUrl(), true)) {
                // if domain is not allowed in the license
                message = CommonUtils.getStringResource(R.string.store_is_not_licensed);
            }
            if (message != null) {
                // if invalid message is initialized

                // set the flag to do not show dialog twice
                mLicenseDialogActive = true;
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                builder.setTitle(R.string.warning);
                builder.setMessage(message);
                builder.setPositiveButton(R.string.open_configuration,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                DefaultOptionsMenuHelper.onMenuSettingsPressed(mActivity);
                                mActivity.finish();
                            }
                        });
                builder.setCancelable(false);
                builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mActivity.finish();
                    }
                });
                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mActivity.finish();
                    }
                });
                builder.show();
            }
        }
    }

    /**
     * Menu adapter which is used for the navigation sliding menu
     */
    public static class MenuAdapter extends BaseAdapter {
        Menu mMenu;
        LayoutInflater mInflater;

        /**
         * The key for the menu item view type inten extra parameter
         */
        public static final String VIEW_TYPE = "VIEW_TYPE";
        public static final int VIEW_TYPE_SMALL = 0;
        public static final int VIEW_TYPE_NORMAL = 1;

        public MenuAdapter(Menu menu, LayoutInflater inflater) {
            super();
            this.mMenu = menu;
            this.mInflater = inflater;
        }

        @Override
        public int getCount() {
            return mMenu.size();
        }

        @Override
        public MenuItem getItem(int position) {
            return mMenu.getItem(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                int viewType = getItemViewType(position);
                if (viewType == VIEW_TYPE_NORMAL) {
                    convertView = mInflater.inflate(R.layout.slider_navigation_item_right, parent,
                            false);
                } else {
                    convertView = mInflater.inflate(R.layout.slider_navigation_item_right_small,
                            parent, false);
                }
                holder = new ViewHolder();
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                holder.text = (TextView) convertView.findViewById(android.R.id.text1);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            MenuItem mi = getItem(position);
            if (mi.getIcon() != null) {
                holder.icon.setImageDrawable(mi.getIcon());
            }
            holder.icon.setVisibility(mi.getIcon() == null ? View.GONE : View.VISIBLE);
            holder.text.setText(mi.getTitle());
            return convertView;
        }

        class ViewHolder {
            ImageView icon;
            TextView text;
        }

        public Menu getMenu() {
            return mMenu;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            MenuItem mi = getItem(position);
            Intent intent = mi.getIntent();
            Integer result = null;
            if(intent != null && intent.hasExtra(VIEW_TYPE)){
                result = intent.getIntExtra(VIEW_TYPE, -1);
            } else {
                result = mi.getIcon() == null ? VIEW_TYPE_SMALL : VIEW_TYPE_NORMAL;
            }
            return result;
        }
    }
}
