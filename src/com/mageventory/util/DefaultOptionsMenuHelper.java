package com.mageventory.util;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.mageventory.CategoryListActivity;
import com.mageventory.MageventoryConstants;
import com.mageventory.MainActivity;
import com.mageventory.ProductCreateActivity;
import com.mageventory.ProductListActivity2;
import com.mageventory.R;
import com.mageventory.ScanActivity;

public class DefaultOptionsMenuHelper implements MageventoryConstants {
    
    public static boolean onCreateOptionsMenu(final Activity activity, final Menu menu) {
        MenuInflater inflater = activity.getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    public static boolean onOptionsItemSelected(final Activity activity, final MenuItem item) {

        if (item.getItemId() == R.id.menu_products) {
            Intent myIntent = new Intent(activity.getApplicationContext(), ProductListActivity2.class);
            activity.startActivity(myIntent);
        }
        if (item.getItemId() == R.id.menu_new) {
            Intent myIntent = new Intent(activity.getApplicationContext(), ProductCreateActivity.class);
            activity.startActivity(myIntent);
        }
        if (item.getItemId() == R.id.menu_refresh) {
            Intent myIntent = new Intent(activity.getApplicationContext(), activity.getClass());
            activity.finish();
            activity.startActivity(myIntent);
        }        
        if (item.getItemId() == R.id.menu_Categories) {
            Intent myIntent = new Intent(activity.getApplicationContext(), CategoryListActivity.class);
            activity.startActivity(myIntent);
        }
        if(item.getItemId() == R.id.menu_scan)
        {
        	// Start Scan Activity 
        	// A temp activity starts Scan and check site DB
        	Intent myIntent = new Intent(activity.getApplicationContext(),ScanActivity.class);
        	activity.startActivity(myIntent);        	
        }
        if(item.getItemId() == R.id.menu_home)
        {
        	 if(activity.getClass() != MainActivity.class)
        	 {
        		 Intent myIntent = new Intent(activity.getApplicationContext(),MainActivity.class);
             	 activity.startActivity(myIntent);
        	 }
        }
        return true;
    }
}
