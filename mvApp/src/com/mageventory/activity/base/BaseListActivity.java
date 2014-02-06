
package com.mageventory.activity.base;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.mageventory.R;

/* This is one of the base classes for all activities in this application. Please note that all activities should
 * extend either BaseActivity or BaseListActivity. */
public class BaseListActivity extends ListActivity {

    private BaseActivityCommon mBaseActivityCommon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activities_root_with_list);

        mBaseActivityCommon = new BaseActivityCommon(this);
        mBaseActivityCommon.onCreate();
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup root = (ViewGroup) findViewById(R.id.content_frame);
        root.removeAllViews();
        mBaseActivityCommon.setContentView(layoutResID);
        onContentChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mBaseActivityCommon.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mBaseActivityCommon.onOptionsItemSelected(item);
    }

    public void hideKeyboard()
    {
        mBaseActivityCommon.hideKeyboard();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        hideKeyboard();
        return super.onPrepareOptionsMenu(menu);
    }
}
