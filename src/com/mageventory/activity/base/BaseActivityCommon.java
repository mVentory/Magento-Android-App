package com.mageventory.activity.base;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;

import com.mageventory.util.DefaultOptionsMenuHelper;

/* This class helps us overcome the lack of multiple inheritance in java.
 * We want to have two classes from which all activities extend (either from one or from the other). Those are:
 * BaseActivity and BaseListActivity.
 *  
 * We want to BaseActivity to extend Activity and we want BaseListActivity to extend ListActivity. At the same time
 * we want both of these base classes to have some common methods that we implement. We can't inherit from any more classes
 * so we created a separate class which is BaseActivityCommon. */
public class BaseActivityCommon {
	
	private Activity mActivity;
	
	static private Class<? extends Activity> sCurrentActivityClass;
	static public Class<? extends Activity> sPreviousActivityClass;
	
	public BaseActivityCommon(Activity activity)
	{
		mActivity = activity;
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		return DefaultOptionsMenuHelper.onCreateOptionsMenu(mActivity, menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		return DefaultOptionsMenuHelper.onOptionsItemSelected(mActivity, item);
	}
	
	public void onResume() {
		sPreviousActivityClass = sCurrentActivityClass;
		sCurrentActivityClass = mActivity.getClass(); 
	}
}
