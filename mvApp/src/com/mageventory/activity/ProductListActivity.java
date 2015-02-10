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

package com.mageventory.activity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.mageventory.MageventoryConstants;
import com.mventory.R;
import com.mageventory.activity.base.BaseListActivity;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.tasks.LoadProductListData;
import com.mageventory.tasks.RestoreAndDisplayProductListData;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.util.EventBusUtils.GeneralBroadcastEventHandler;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.Log;

public class ProductListActivity extends BaseListActivity implements MageventoryConstants,
        OperationObserver, GeneralBroadcastEventHandler {

    private static class EmptyListAdapter extends BaseAdapter {

        private static final Map<String, String> data = new HashMap<String, String>();
        static {
            for (final String key : REQUIRED_PRODUCT_KEYS) {
                data.put(key, "");
            }
        }

        private final boolean displayPlaceholder;
        private final LayoutInflater inflater;

        public EmptyListAdapter(final Context context, final boolean displayPlaceholder) {
            this.displayPlaceholder = displayPlaceholder;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return displayPlaceholder ? 1 : 0;
        }

        @Override
        public Object getItem(int position) {
            if (position == 0) {
                return data;
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView != null) {
                return convertView;
            }
            return inflater.inflate(R.layout.product_list_placeholder, null);
        }

    };

    private static final int[] KEY_TO_VIEW_MAP = {
        android.R.id.text1
    };
    public static final int LOAD_FAILURE_DIALOG = 1;
    public static final String[] REQUIRED_PRODUCT_KEYS = {
        "name"
    };
    private static final String TAG = "ProductListActivity2";

    private String EKEY_ERROR_MESSAGE;

    private boolean isDataDisplayed = false;
    private String nameFilter = "";
    private EditText nameFilterEdit;
    public AtomicInteger operationRequestId = new AtomicInteger(INVALID_REQUEST_ID);
    private RestoreAndDisplayProductListData restoreAndDisplayTask;
    private int selectedItemPos = ListView.INVALID_POSITION;
    /**
     * Whether the data should be reloaded when activity is resumed
     */
    private boolean mRefreshOnResume = false;

    public void displayData(final List<Map<String, Object>> data) {
        final ProductListActivity host = this;
        final Runnable display = new Runnable() {
            public void run() {
                // if call is successful but there are no products to list
                final ListAdapter adapter;
                if (data.size() == 0) {
                    adapter = new EmptyListAdapter(host, true);
                } else {
                    adapter = new SimpleAdapter(host, data, android.R.layout.simple_list_item_1,
                            REQUIRED_PRODUCT_KEYS,
                            KEY_TO_VIEW_MAP);
                    host.isDataDisplayed = true;
                }
                setListAdapter(adapter);
                if (selectedItemPos != ListView.INVALID_POSITION) {
                    getListView().setSelectionFromTop(selectedItemPos, 0);
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            display.run();
        } else {
            runOnUiThread(display);
        }
    }

    private void emptyList() {
        emptyList(false);
    }

    private void emptyList(final boolean displayPlaceholder) {
        final Runnable empty = new Runnable() {
            @Override
            public void run() {
                setListAdapter(new EmptyListAdapter(ProductListActivity.this, displayPlaceholder));
                isDataDisplayed = false;
            }
        };
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            empty.run();
        } else {
            runOnUiThread(empty);
        }
    }

    public String getNameFilter() {
        return nameFilter;
    }

    private void hideSoftKeyboard() {
        nameFilterEdit.clearFocus();
        InputMethodManager m = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        m.hideSoftInputFromWindow(nameFilterEdit.getWindowToken(), 0);
    }

    private void loadProductList() {
        loadProductList(false);
    }

    private void loadProductList(final boolean forceReload) {
        loadProductList(getNameFilter(), forceReload);
    }

    private void loadProductList(final String nameFilter,
            final boolean forceReload) {
        Log.v(TAG, "loadProductList(" + nameFilter + ", " + forceReload + ");");

        restoreAndDisplayTask = null;
        hideSoftKeyboard();
        emptyList();
        String[] params;
        params = new String[] {
            nameFilter
        };
        new LoadProductListData(forceReload, this).execute(RES_CATALOG_PRODUCT_LIST, params);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.product_list);

        // initialize
        if (icicle != null) {
            setNameFilter(icicle.getString(getString(R.string.ekey_name_filter)));
            operationRequestId.set(icicle.getInt(getString(R.string.ekey_operation_request_id)));
        }

        // constants
        EKEY_ERROR_MESSAGE = getString(R.string.ekey_error_message);

        // add header
        final LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View header = inflater.inflate(R.layout.product_list_header, null);
        getListView().addHeaderView(header);

        // set on list item long click listener
        getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                launchDetails(arg0, arg2, true);
                return true;
            }
        });

        // initialize filtering
        nameFilterEdit = (EditText) header.findViewById(R.id.filter_query);
        nameFilterEdit.setText(getNameFilter());

        nameFilterEdit.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    String nameFilter = "" + nameFilterEdit.getText();
                    setNameFilter(nameFilter);
                    loadProductList();

                    return true;
                }

                return false;
            }
        });

        // try to restore data loading task after orientation switch
        restoreAndDisplayTask = (RestoreAndDisplayProductListData) getLastNonConfigurationInstance();

        EventBusUtils.registerOnGeneralEventBroadcastReceiver(TAG, this, this);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle bundle) {
        if (id == LOAD_FAILURE_DIALOG) {
            // build dialog
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final AlertDialog dialog = builder.create();

            // set title
            dialog.setTitle(getString(R.string.data_load_failure));

            // set message
            final StringBuilder message = new StringBuilder(64);
            message.append(getString(R.string.check_your_internet_retry));
            if (bundle != null && bundle.containsKey(EKEY_ERROR_MESSAGE)) {
                message.append("\n\n");
                message.append(getString(R.string.error));
                message.append(": ");
                message.append(bundle.get(EKEY_ERROR_MESSAGE));
            }
            dialog.setMessage(message.toString());

            // set buttons
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.try_again),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loadProductList();
                        }
                    });
            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ProductListActivity.this.finish();
                        }
                    });

            return dialog;
        }
        return super.onCreateDialog(id);
    }

    private void launchDetails(AdapterView<? extends Adapter> list, int position, final boolean edit) {
        // TODO y: use action
        // get product id and put it as intent extra
        String SKU;
        try {
            @SuppressWarnings("unchecked")
            final Map<String, Object> data = (Map<String, Object>) list.getAdapter().getItem(
                    position);
            SKU = data.get(MAGEKEY_PRODUCT_SKU).toString();
        } catch (Throwable e) {
            GuiUtils.alert(getString(R.string.invalid_product_id));
            return;
        }

        final Intent intent;
        if (edit) {
            intent = new Intent(this, ProductEditActivity.class);
        } else {
            intent = new Intent(this, ScanActivity.class);
            // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        intent.putExtra(getString(R.string.ekey_product_sku), SKU);

        startActivityForResult(intent, REQ_EDIT_PRODUCT);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        launchDetails(l, position, false);
    }

    @Override
    public void onLoadOperationCompleted(final LoadOperation op) {
        if (operationRequestId.get() == op.getOperationRequestId()) {
            restoreAndDisplayProductList(op.getResourceType(), op.getResourceParams());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            loadProductList(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ResourceServiceHelper.getInstance().unregisterLoadOperationObserver(this);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        selectedItemPos = state.getInt(getString(R.string.ekey_selected_item_pos),
                ListView.INVALID_POSITION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ResourceServiceHelper.getInstance().registerLoadOperationObserver(this);

        // if there is a scheduled refresh operation
        if (mRefreshOnResume) {
            mRefreshOnResume = false;
            loadProductList(true);
            return;
        }
        // do nothing more, if data is already displayed
        if (isDataDisplayed) {
            Log.d(TAG, "onResume(): Data is already displayed.");
            return;
        }

        // if there is currently ongoing restore task
        if (restoreAndDisplayTask != null) {
            if (restoreAndDisplayTask.isRunning()) {
                // wait
                Log.d(TAG, "restoreAndDisplayTask is currently running");
                return;
            } else {
                final List<Map<String, Object>> data = restoreAndDisplayTask.getData();
                if (data != null) {
                    Log.d(TAG,
                            "onResume(): dispaly data retrieved by calling restoreAndDisplayTask::getData()");
                    displayData(data);
                    return;
                }
            }
        }

        // is we get here, then there is no restore task currently executing no
        // data displayed,
        // thus we should make a data load request
        if (ResourceServiceHelper.getInstance().isPending(operationRequestId.get())) {
            // good, we just need to wait for notification
        } else {
            // check if the resource is present, if yes -- load it, if not --
            // start new operation
            loadProductList();
        }
    }

    // following 2 methods enable the default options menu

    @Override
    public Object onRetainNonConfigurationInstance() {
        return restoreAndDisplayTask;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(getString(R.string.ekey_name_filter), getNameFilter());
        outState.putInt(getString(R.string.ekey_operation_request_id), operationRequestId.get());
        outState.putInt(getString(R.string.ekey_selected_item_pos), getListView()
                .getFirstVisiblePosition());
    }

    public synchronized void restoreAndDisplayProductList(int resType, String[] params) {
        if (restoreAndDisplayTask != null && restoreAndDisplayTask.isRunning()) {
            return;
        }
        restoreAndDisplayTask = new RestoreAndDisplayProductListData(this);
        restoreAndDisplayTask.execute((Object) params);
    }

    private void setNameFilter(String s) {
        nameFilter = s;
    }

    @Override
    public void onGeneralBroadcastEvent(EventType eventType, Intent extra) {
        switch (eventType) {
            case PRODUCT_DELETED: {
                CommonUtils.debug(TAG, "onGeneralBroadcastEvent: received product deleted event");
                // if activity is resumed refresh immediately. Otherwise
                // schedule refresh operation
                if (isActivityResumed()) {
                    loadProductList(true);
                } else {
                    mRefreshOnResume = true;
                }
            }
                break;
            default:
                break;
        }
    }
}
