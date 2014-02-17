
package com.mageventory.activity.base;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.DataSetObserver;
import android.net.Uri;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mageventory.R;
import com.mageventory.util.DefaultOptionsMenuHelper;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.Log;

/* This class helps us overcome the lack of multiple inheritance in java.
 * We want to have two classes from which all activities extend (either from one or from the other). Those are:
 * BaseActivity and BaseListActivity.
 *  
 * We want to BaseActivity to extend Activity and we want BaseListActivity to extend ListActivity. At the same time
 * we want both of these base classes to have some common methods that we implement. We can't inherit from any more classes
 * so we created a separate class which is BaseActivityCommon. */
public class BaseActivityCommon {

    /*
     * This is needed for the following issue:
     * http://code.google.com/p/mageventory/issues/detail?id=199 It keeps track
     * of whether product creation activity is supposed to load last used
     * category and attribute set automatically or not.
     */
    public static boolean mNewNewReloadCycle = false;

    private Activity mActivity;
    DrawerLayout mDrawerLayout;

    public BaseActivityCommon(Activity activity)
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
            String versionName;
            try {
                versionName = mActivity.getPackageManager().getPackageInfo(
                        mActivity.getPackageName(), 0).versionName;
                versionName = versionName.substring(versionName.lastIndexOf("r"));

                actionBar.setSubtitle(versionName);
            } catch (NameNotFoundException e) {
                Log.logCaughtException(e);
            }
        }
        initHelp();
        initMenu();
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() != R.id.menu_new &&
                item.getItemId() != R.id.menu_refresh &&
                item.getItemId() != R.id.menu_scan)
        {
            /*
             * If the user didn't select one of the above options from the menu
             * then we are breaking the NewNewReloadCycle
             */
            BaseActivityCommon.mNewNewReloadCycle = false;
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
        ListView mDrawerList = (ListView) mActivity.findViewById(R.id.left_drawer);

        if (mDrawerList != null) {
            // Set the adapter for the list view
            mDrawerList.setAdapter(new ArrayAdapter<String>(mActivity,
                    android.R.layout.simple_list_item_1, new String[] {
                            "link1", "link2", "link3", "link4", "link5", "link6", "link7", "link8",
                    }));
            // Set the list's click listener
            mDrawerList.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("http://www.mventory.com"));
                    mActivity.startActivity(i);
                }

            });
        }
    }

    /**
     * init the sliding navigation menu in the right navigation drawer
     */
    void initMenu() {
        final ListView mDrawerList = (ListView) mActivity.findViewById(R.id.right_drawer);

        if (mDrawerList != null) {
            Menu menu = GuiUtils.newMenuInstance(mActivity);
            new MenuInflater(mActivity).inflate(R.menu.navigation_menu, menu);
            final MenuAdapter adapter = new MenuAdapter(menu, LayoutInflater.from(mActivity));
            mDrawerList.setAdapter(adapter);
            mDrawerList.setOnItemClickListener(new OnItemClickListener() {


                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    MenuItem mi = adapter.getItem(position);
                    mActivity.onOptionsItemSelected(mi);
                    closeDrawers();
                }
            });
            final View showMoreView = mActivity.findViewById(R.id.show_more_view);
            final FrameLayout showMoreControl = (FrameLayout) showMoreView
                    .findViewById(R.id.show_more_control);
            final TextView showMoreText = (TextView) showMoreControl
                    .findViewById(android.R.id.text1);
            showMoreText.setText(R.string.menu_show_more);
            showMoreControl.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    GuiUtils.post(new Runnable() {

                        @Override
                        public void run() {
                            mDrawerList.smoothScrollToPosition(mDrawerList.getCount() - 1);
                        }
                    });
                }
            });
            adapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    GuiUtils.post(new Runnable() {

                        @Override
                        public void run() {
                            int last = mDrawerList.getLastVisiblePosition();
                            boolean itemsFit = last == mDrawerList.getCount() - 1
                                    && mDrawerList.getChildAt(last).getBottom() <= mDrawerList
                                            .getHeight();
                            showMoreView.setVisibility(itemsFit ? View.GONE : View.VISIBLE);
                        }
                    });
                }
            });

            mDrawerList.setOnScrollListener(new OnScrollListener() {

                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {

                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                        int totalItemCount) {
                    if (showMoreView.getVisibility() != View.GONE) {
                        Animation animation = AnimationUtils.loadAnimation(mActivity,
                                android.R.anim.fade_out);
                        long animationDuration = 500;
                        animation.setDuration(animationDuration);
                        showMoreView.startAnimation(animation);
                        showMoreView.setVisibility(View.GONE);
                    }
                }
            });
        }
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
     * Menu adapter which is used for the navigation sliding menu
     */
    public static class MenuAdapter extends BaseAdapter {
        Menu mMenu;
        LayoutInflater mInflater;

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
            if (holder.icon != null) {
                holder.icon.setImageDrawable(mi.getIcon());
            }
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
            return mi.getIcon() == null ? VIEW_TYPE_SMALL : VIEW_TYPE_NORMAL;
        }
    }
}
