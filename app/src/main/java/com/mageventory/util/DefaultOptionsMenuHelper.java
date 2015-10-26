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
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mventory.R;
import com.mageventory.activity.ConfigServerActivity;
import com.mageventory.activity.MainActivity;
import com.mageventory.activity.OrderListActivity;
import com.mageventory.activity.ProductCreateActivity;
import com.mageventory.activity.ProductListActivity;
import com.mageventory.activity.ScanActivity;

public class DefaultOptionsMenuHelper implements MageventoryConstants {

    public static boolean onCreateOptionsMenu(final Activity activity, final Menu menu) {
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

    public static boolean onOptionsItemSelected(final Activity activity, final MenuItem item) {

        if (item.getItemId() == R.id.menu_products) {
            onMenuProductsPressed(activity);
        }
        if (item.getItemId() == R.id.menu_new) {
            onMenuNewPressed(activity);
        }
        if (item.getItemId() == R.id.menu_refresh) {
            Intent myIntent = new Intent(activity.getApplicationContext(), activity.getClass());
            activity.finish();
            activity.startActivity(myIntent);
        }
        if (item.getItemId() == R.id.menu_orderlist) {
            Intent myIntent = new Intent(activity.getApplicationContext(), OrderListActivity.class);
            myIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(myIntent);
        }
        if (item.getItemId() == R.id.menu_scan) {
            onMenuScanPressed(activity);
        }
        if (item.getItemId() == R.id.menu_menu) {
            toggleMenuVisibility(activity);
        }
        if (item.getItemId() == R.id.menu_help) {
            onMenuHelpPressed(activity);
        }
        if (item.getItemId() == R.id.menu_settings) {
            onMenuSettingsPressed(activity);
        }
        if (item.getItemId() == android.R.id.home || item.getItemId() == R.id.menu_home) {
            onMenuHomePressed(activity);
        }
        return true;
    }

    /**
     * Action which should be performed when menu settings is pressed
     * 
     * @param activity
     */
    public static void onMenuSettingsPressed(final Activity activity) {
        Intent newInt = new Intent(activity.getApplicationContext(), ConfigServerActivity.class);
        newInt.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(newInt);
    }

    public static void onMenuHomePressed(final Activity activity) {
        if (activity.getClass() != MainActivity.class) {
            Intent myIntent = new Intent(activity.getApplicationContext(), MainActivity.class);
            myIntent.putExtra(activity.getString(R.string.ekey_dont_show_menu), true);
            activity.startActivity(myIntent);
        }
    }

    public static void onMenuHelpPressed(final Activity activity) {
        final DrawerLayout drawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            GuiUtils.hideKeyboard(drawerLayout);
            drawerLayout.closeDrawer(Gravity.END);
            if (drawerLayout.isDrawerOpen(Gravity.START)) {
                drawerLayout.closeDrawer(Gravity.START);
            } else {
                GuiUtils.post(new Runnable(){
                    @Override
                    public void run() {
                        drawerLayout.openDrawer(Gravity.START);
                    }
                });
            }
        }
    }

    public static void onMenuProductsPressed(final Activity activity) {
        Intent myIntent = new Intent(activity.getApplicationContext(),
                ProductListActivity.class);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(myIntent);
    }

    public static void onMenuScanPressed(final Activity activity) {
        // Start Scan Activity
        // A temp activity starts Scan and check site DB
        Intent myIntent = new Intent(activity.getApplicationContext(), ScanActivity.class);
        activity.startActivity(myIntent);
    }

    public static void onMenuNewPressed(final Activity activity) {
        Intent myIntent = new Intent(activity.getApplicationContext(),
                ProductCreateActivity.class);
        activity.startActivity(myIntent);
    }

    /**
     * Adjust visibility of the sliding menu
     */
    public static void toggleMenuVisibility(final Activity activity) {
        DrawerLayout drawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            GuiUtils.hideKeyboard(drawerLayout);
            drawerLayout.closeDrawer(Gravity.START);
            if (drawerLayout.isDrawerOpen(Gravity.END)) {
                drawerLayout.closeDrawer(Gravity.END);
            } else {
                drawerLayout.openDrawer(Gravity.END);
            }
        }
    }
}
