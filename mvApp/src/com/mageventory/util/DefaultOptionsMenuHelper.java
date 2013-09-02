
package com.mageventory.util;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.CategoryListActivity;
import com.mageventory.activity.MainActivity;
import com.mageventory.activity.OrderListActivity;
import com.mageventory.activity.ProductCreateActivity;
import com.mageventory.activity.ProductListActivity;
import com.mageventory.activity.ScanActivity;

public class DefaultOptionsMenuHelper implements MageventoryConstants {

    public static boolean onCreateOptionsMenu(final Activity activity, final Menu menu) {
        MenuInflater inflater = activity.getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public static boolean onOptionsItemSelected(final Activity activity, final MenuItem item) {

        if (item.getItemId() == R.id.menu_products) {
            Intent myIntent = new Intent(activity.getApplicationContext(),
                    ProductListActivity.class);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(myIntent);
        }
        if (item.getItemId() == R.id.menu_new) {
            Intent myIntent = new Intent(activity.getApplicationContext(),
                    ProductCreateActivity.class);
            activity.startActivity(myIntent);
        }
        if (item.getItemId() == R.id.menu_refresh) {
            Intent myIntent = new Intent(activity.getApplicationContext(), activity.getClass());
            activity.finish();
            activity.startActivity(myIntent);
        }
        if (item.getItemId() == R.id.menu_orderlist) {
            Intent myIntent = new Intent(activity.getApplicationContext(), OrderListActivity.class);
            activity.startActivity(myIntent);
        }
        if (item.getItemId() == R.id.menu_scan) {
            // Start Scan Activity
            // A temp activity starts Scan and check site DB
            Intent myIntent = new Intent(activity.getApplicationContext(), ScanActivity.class);
            activity.startActivity(myIntent);
        }
        if (item.getItemId() == R.id.menu_home) {
            if (activity.getClass() != MainActivity.class) {
                Intent myIntent = new Intent(activity.getApplicationContext(), MainActivity.class);
                myIntent.putExtra(activity.getString(R.string.ekey_dont_show_menu), true);
                activity.startActivity(myIntent);
            }
        }
        return true;
    }
}
