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
import com.mageventory.ProductDetailsActivity;
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
            activity.startActivityForResult(myIntent, 0);
        }
        if (item.getItemId() == R.id.menu_new) {
            Intent myIntent = new Intent(activity.getApplicationContext(), ProductCreateActivity.class);
            activity.startActivityForResult(myIntent, REQ_NEW_PRODUCT);
        }
        if (item.getItemId() == R.id.menu_refresh) {
            Intent myIntent = new Intent(activity.getApplicationContext(), activity.getClass());
            activity.finish();
            activity.startActivityForResult(myIntent, 0);
        }        
        if (item.getItemId() == R.id.menu_Categories) {
            Intent myIntent = new Intent(activity.getApplicationContext(), CategoryListActivity.class);
            activity.startActivity(myIntent);
            // activity.startActivityForResult(myIntent, 0);
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
    
    public static boolean onActivityResult(final Activity a, final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQ_NEW_PRODUCT) {
            if (resultCode == RESULT_SUCCESS) {
                // new product created successfully, launch details
                if (data != null) {
                    final String ekeyProductId = a.getString(R.string.ekey_product_id);
                    final int productId = data.getIntExtra(ekeyProductId, INVALID_PRODUCT_ID);
                    if (productId != INVALID_PRODUCT_ID) {
                        final Intent intent = new Intent(a, ProductDetailsActivity.class);
                        intent.putExtra(ekeyProductId, productId);
                        a.startActivity(intent);
                    }
                }
            }
            return true;
        }
        return false;
    }

}
