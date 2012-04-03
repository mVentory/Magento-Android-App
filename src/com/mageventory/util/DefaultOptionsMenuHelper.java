package com.mageventory.util;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.mageventory.CategoryListActivity;
import com.mageventory.ConfigServerActivity;
import com.mageventory.ProductCreateActivity;
import com.mageventory.ProductListActivity2;
import com.mageventory.R;

public class DefaultOptionsMenuHelper {
    
    public static boolean onCreateOptionsMenu(final Activity activity, final Menu menu) {
        MenuInflater inflater = activity.getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    public static boolean onOptionsItemSelected(final Activity activity, final MenuItem item) {

        if (item.getItemId() == R.id.menu_products) {
            Intent myIntent = new Intent(activity.getApplicationContext(), ProductListActivity2.class);
            activity.startActivityForResult(myIntent, 0);

        }
        if (item.getItemId() == R.id.menu_new) {
            Log.d("APP", "menu_create");
            Intent myIntent = new Intent(activity.getApplicationContext(), ProductCreateActivity.class);
            activity.startActivityForResult(myIntent, 0);
        }
        if (item.getItemId() == R.id.menu_refresh) {
            Intent myIntent = new Intent(activity.getApplicationContext(), activity.getClass());
            activity.finish();
            activity.startActivityForResult(myIntent, 0);

        }
        if (item.getItemId() == R.id.menu_settings) {
            Intent myIntent = new Intent(activity.getApplicationContext(), ConfigServerActivity.class);
            activity.startActivityForResult(myIntent, 0);
        }
        if (item.getItemId() == R.id.menu_Categories) {
            Intent myIntent = new Intent(activity.getApplicationContext(), CategoryListActivity.class);
            activity.startActivity(myIntent);
            // activity.startActivityForResult(myIntent, 0);
        }
        if (item.getItemId() == R.id.menu_quit) {
             Intent i = new Intent();
                i.setAction(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_HOME);
                activity.startActivity(i);
        }
        return true;
    }

}
