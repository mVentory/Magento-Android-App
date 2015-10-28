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

package com.mageventory.util;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.ConfigServerActivity;
import com.mageventory.activity.MainActivity;
import com.mageventory.activity.OrderListActivity;
import com.mageventory.activity.ProductCreateActivity;
import com.mageventory.activity.ProductListActivity;
import com.mageventory.activity.ScanActivity;
import com.mageventory.util.run.CallableWithParameterAndResult;
import com.mventory.R;

public class DefaultOptionsMenuHelper implements MageventoryConstants {

    /**
     * Default menu actions
     */
    public static enum MenuAction{
        /**
         * Action which should be performed when menu new is pressed
         */
        NEW(true, new CallableWithParameterAndResult<Activity, Boolean>() {
            @Override public Boolean call(Activity activity) {
                Intent myIntent = new Intent(activity.getApplicationContext(),
                        ProductCreateActivity.class);
                activity.startActivity(myIntent);
                return true;
            }
        }, R.id.menu_new),

        /**
         * Action which should be performed when menu products is pressed
         */
        PRODUCTS(true, new CallableWithParameterAndResult<Activity, Boolean>() {
            @Override public Boolean call(Activity activity) {
                Intent myIntent = new Intent(activity.getApplicationContext(),
                        ProductListActivity.class);
                myIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(myIntent);
                return true;
            }
        }, R.id.menu_products),

        /**
         * Action which should be performed when menu refresh is pressed
         */
        REFRESH(true, new CallableWithParameterAndResult<Activity, Boolean>() {
            @Override public Boolean call(Activity activity) {
                Intent myIntent = new Intent(activity.getApplicationContext(), activity.getClass());
                activity.finish();
                activity.startActivity(myIntent);
                return true;
            }
        }, R.id.menu_refresh),

        /**
         * Action which should be performed when menu order list is pressed
         */
        ORDER_LIST(true, new CallableWithParameterAndResult<Activity, Boolean>() {
            @Override public Boolean call(Activity activity) {
                Intent myIntent = new Intent(activity.getApplicationContext(), OrderListActivity.class);
                myIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(myIntent);
                return true;
            }
        }, R.id.menu_orderlist),

        /**
         * Action which should be performed when menu scan is pressed
         */
        SCAN(true, new CallableWithParameterAndResult<Activity, Boolean>() {
            @Override public Boolean call(Activity activity) {
                // Start Scan Activity
                // A temp activity starts Scan and check site DB
                Intent myIntent = new Intent(activity.getApplicationContext(), ScanActivity.class);
                activity.startActivity(myIntent);
                return true;
            }
        }, R.id.menu_scan),

        /**
         * Action which should be performed when menu menu is pressed
         */
        MENU(false, new CallableWithParameterAndResult<Activity, Boolean>() {
            @Override public Boolean call(Activity activity) {
                DrawerLayout drawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
                if (drawerLayout != null) {
                    GuiUtils.hideKeyboard(drawerLayout);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                        drawerLayout.closeDrawer(GravityCompat.END);
                    } else {
                        drawerLayout.openDrawer(GravityCompat.END);
                    }
                }
                return true;
            }
        }, R.id.menu_menu),

        /**
         * Action which should be performed when menu help is pressed
         */
        HELP(false, new CallableWithParameterAndResult<Activity, Boolean>() {
            @Override public Boolean call(Activity activity) {
                final DrawerLayout drawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
                if (drawerLayout != null) {
                    GuiUtils.hideKeyboard(drawerLayout);
                    drawerLayout.closeDrawer(GravityCompat.END);
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    } else {
                        GuiUtils.post(new Runnable(){
                            @Override
                            public void run() {
                                drawerLayout.openDrawer(GravityCompat.START);
                            }
                        });
                    }
                }
                return true;
            }
        }, R.id.menu_help),

        /**
         * Action which should be performed when menu settings is pressed
         */
        SETTINGS(true, new CallableWithParameterAndResult<Activity, Boolean>() {
            @Override public Boolean call(Activity activity) {
                Intent newInt = new Intent(activity.getApplicationContext(), ConfigServerActivity.class);
                newInt.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(newInt);
                return true;
            }
        }, R.id.menu_settings),

        /**
         * Action which should be performed when menu home is pressed
         */
        HOME(true, new CallableWithParameterAndResult<Activity, Boolean>() {
            @Override public Boolean call(Activity activity) {
                if (activity.getClass() != MainActivity.class) {
                    Intent myIntent = new Intent(activity.getApplicationContext(), MainActivity.class);
                    myIntent.putExtra(activity.getString(R.string.ekey_dont_show_menu), true);
                    activity.startActivity(myIntent);
                }
                return true;
            }
        }, R.id.menu_home,android.R.id.home),

        ;
        /**
         * The menu item ids associated with the action
         */
        int[] mIds;
        /**
         * Whether the action navigates somewhere
         */
        boolean mNavigateAction;
        /**
         * The action to execute in the {@link #executeAction(Activity)} method
         */
        CallableWithParameterAndResult<Activity, Boolean> mAction;

        /**
         * @param navigateAction Whether the action navigates somewhere
         * @param action The action to execute in the {@link #executeAction(Activity)} method
         * @param ids The menu item ids associated with the action
         */
        MenuAction(boolean navigateAction, CallableWithParameterAndResult<Activity, Boolean>
                action, int ... ids) {
            mIds = ids;
            mAction = action;
            mNavigateAction = navigateAction;
        }

        /**
         * Whether the menu item matches to any id associated with the action
         * @param item
         * @return
         */
        public boolean matches(MenuItem item) {
            int itemId = item.getItemId();
            return matches(itemId);
        }

        /**
         * Whether the itemId matches to any id associated with the action
         * @param itemId
         * @return
         */
        public boolean matches(int itemId) {
            boolean result = false;
            for (int id : mIds) {
                result = itemId == id;
                if (result) {
                    break;
                }
            }
            return result;
        }

        /**
         * Whether the action navigate somewhere
         * @return
         */
        public boolean isNavigateAction(){
            return mNavigateAction;
        }

        /**
         * Execute the associated action with the specified params
         * @param activity
         * @return
         */
        public boolean executeAction(Activity activity){
            return mAction.call(activity);
        }

        /**
         * Find the action associated with the menu item
         * @param item
         * @return corresponding action if found, null otherwise
         */
        public static MenuAction findForItem(MenuItem item){
            return findForId(item.getItemId());
        }

        /**
         * Find the action associated with the specified item id
         * @param id
         * @return corresponding action if found, null otherwise
         */
        public static MenuAction findForId(int id){
            MenuAction result = null;
            for(MenuAction ma: values()){
                if(ma.matches(id)){
                    result = ma;
                    break;
                }
            }
            return result;
        }
    }

    public static <T extends Activity & MenuActionExecutor> boolean onCreateOptionsMenu(
            final T activity, final Menu menu) {
        MenuInflater inflater = activity.getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        // initialize action items with custom layouts
        // menu IDs with custom action layout.
        int[] customActionLayoutItemIds = new int[] {
                R.id.menu_home, R.id.menu_scan, R.id.menu_menu
        };
        for (int id : customActionLayoutItemIds) {
            final MenuItem mi = menu.findItem(id);

            View view = mi.getActionView();

            // set the icon
            ImageView iconView = (ImageView) view.findViewById(R.id.icon);
            iconView.setImageDrawable(mi.getIcon());

            // set the label
            TextView textView = (TextView) view.findViewById(R.id.text1);
            textView.setText(mi.getTitle());

            // set click handler
            view.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    onOptionsItemSelected(activity, mi);
                }
            });
        }
        return true;
    }

    /**
     * Search for the associated MenuAction associated with the specified menu item. Execute
     * action with the specified executor if found.
     * @param menuActionExecutor
     * @param item
     * @return
     */
    public static boolean onOptionsItemSelected(final MenuActionExecutor menuActionExecutor, final MenuItem item) {

        MenuAction action = MenuAction.findForItem(item);
        if(action != null){
            return menuActionExecutor.executeMenuAction(action);
        } else {
            return false;
        }
    }

    /**
     * @{link MenuAction} executors should implement this interface
     */
    public static interface MenuActionExecutor {
        boolean executeMenuAction(MenuAction menuAction);
    }
}
