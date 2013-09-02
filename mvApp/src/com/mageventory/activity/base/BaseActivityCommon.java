
package com.mageventory.activity.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.mageventory.R;
import com.mageventory.activity.OrderListActivity;
import com.mageventory.activity.ProductCreateActivity;
import com.mageventory.activity.ScanActivity;
import com.mageventory.util.DefaultOptionsMenuHelper;

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

    public BaseActivityCommon(Activity activity)
    {
        mActivity = activity;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return DefaultOptionsMenuHelper.onCreateOptionsMenu(mActivity, menu);
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
}
