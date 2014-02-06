
package com.mageventory.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobQueue.JobDetail;

public class QueueActivity extends BaseActivity {
    private JobControlInterface jobControlInterface;
    private boolean is_pending_group_open;
    ListView listView;

    public static final String DELETE_SELECTED = "Delete selected";
    public static final String RETRY_SELECTED = "Retry selected";
    public static final String DELETE_MARKED = "Delete marked";
    public static final String RETRY_MARKED = "Retry marked";
    public static final String MARK_ALL = "Mark all";
    public static final String UNMARK_ALL = "Unmark all";
    public static final String DUMP_TABLES = "Dump database";

    private String[] menuItemsPending = new String[] {
            DELETE_SELECTED, DELETE_MARKED, MARK_ALL, UNMARK_ALL, DUMP_TABLES
    };
    private String[] menuItemsFailed = new String[] {
            DELETE_SELECTED, RETRY_SELECTED, DELETE_MARKED, RETRY_MARKED,
            MARK_ALL, UNMARK_ALL, DUMP_TABLES
    };
    List<JobDetail> jobDetails;

    private SimpleAdapter getSimpleAdapter(boolean pendingTable, boolean useCurrenJobDetails) {

        if (useCurrenJobDetails == false)
        {
            jobDetails = jobControlInterface.getJobDetailList(pendingTable);
        }

        List<Map<String, String>> data = new ArrayList<Map<String, String>>();

        for (JobDetail detail : jobDetails) {
            Map<String, String> item = new HashMap<String, String>();

            switch (detail.jobType) {
                case MageventoryConstants.RES_CATALOG_PRODUCT_CREATE:
                    item.put("firstLine", "Product creation");
                    item.put("secondLine", "Pr. name: " + detail.productName);
                    break;
                case MageventoryConstants.RES_UPLOAD_IMAGE:
                    item.put("firstLine", "Image upload (count: " + detail.imagesCount + ")");
                    item.put("secondLine", "Pr. name: " + detail.productName);
                    break;
                case MageventoryConstants.RES_CATALOG_PRODUCT_SELL:
                    item.put("firstLine", "Sell (QTY: " + detail.soldItemsCount + ")");
                    item.put("secondLine", "SKU: " + detail.SKU);
                    break;
                case MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE:
                    item.put("firstLine", "Product edit");
                    item.put("secondLine", "Pr. name: " + detail.productName);
                    break;
                case MageventoryConstants.RES_CATALOG_PRODUCT_SUBMIT_TO_TM:
                    item.put("firstLine", "Submit to TM");
                    item.put("secondLine", "Pr. name: " + detail.productName);
                    break;
                case MageventoryConstants.RES_ORDER_SHIPMENT_CREATE:
                    item.put("firstLine", "Create shipment");
                    item.put("secondLine", "Order inc. id: #" + detail.orderIncrementID);
                    break;
                case MageventoryConstants.RES_ADD_PRODUCT_TO_CART:
                    item.put("firstLine", "Add to cart");
                    item.put("secondLine", "Transaction id: " + detail.transactionID);
                    break;
                case MageventoryConstants.RES_SELL_MULTIPLE_PRODUCTS:
                    item.put("firstLine", "Multiple prod. sell");
                    item.put("secondLine", "Order inc. id: #" + detail.orderIncrementID);
                    break;

                default:
                    break;
            }
            data.add(item);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.queue_item, new String[] {
                "firstLine",
                "secondLine"
        }, new int[] {
                R.id.firstLine, R.id.secondLine
        });

        return adapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.queue_activity);

        jobControlInterface = new JobControlInterface(this);

        final Button failedQueue = (Button) findViewById(R.id.buttonFailedQueue);
        final Button pendingQueue = (Button) findViewById(R.id.buttonPendingQueue);

        listView = (ListView) findViewById(R.id.queue_listview);

        registerForContextMenu(listView);

        failedQueue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QueueActivity.this.setTitle(R.string.activity_queue_failed_name);
                failedQueue.setEnabled(false);
                pendingQueue.setEnabled(true);
                listView.setAdapter(getSimpleAdapter(false, false));

                is_pending_group_open = false;
            }
        });

        pendingQueue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QueueActivity.this.setTitle(R.string.activity_queue_pending_name);

                failedQueue.setEnabled(true);
                pendingQueue.setEnabled(false);
                listView.setAdapter(getSimpleAdapter(true, false));

                is_pending_group_open = true;
            }
        });

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                /*
                 * Show order details activity in case of shipment jobs and
                 * product details activity in all other cases.
                 */
                if (jobDetails.get(position).orderIncrementID == null)
                {
                    Intent newIntent = new Intent(QueueActivity.this, ProductDetailsActivity.class);

                    newIntent.putExtra(getString(R.string.ekey_product_sku),
                            jobDetails.get(position).SKU);
                    newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    QueueActivity.this.startActivity(newIntent);
                }
                else
                {
                    Intent myIntent = new Intent(QueueActivity.this, OrderDetailsActivity.class);
                    myIntent.putExtra(getString(R.string.ekey_order_increment_id),
                            jobDetails.get(position).orderIncrementID);
                    myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    QueueActivity.this.startActivity(myIntent);
                }
            }

        });

        jobDetails = jobControlInterface.getJobDetailList(true);

        if (jobDetails.size() > 0)
        {
            is_pending_group_open = true;
        }
        else
        {
            jobDetails = jobControlInterface.getJobDetailList(false);
            is_pending_group_open = false;
        }

        if (is_pending_group_open) {
            QueueActivity.this.setTitle(R.string.activity_queue_pending_name);
            failedQueue.setEnabled(true);
            pendingQueue.setEnabled(false);

            listView.setAdapter(getSimpleAdapter(true, true));
        } else {
            QueueActivity.this.setTitle(R.string.activity_queue_failed_name);
            failedQueue.setEnabled(false);
            pendingQueue.setEnabled(true);

            listView.setAdapter(getSimpleAdapter(false, true));
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        // AdapterView.AdapterContextMenuInfo info =
        // (AdapterView.AdapterContextMenuInfo)menuInfo;
        menu.setHeaderTitle("Choose an action");
        // is_pending_group_open

        if (is_pending_group_open) {
            for (int i = 0; i < menuItemsPending.length; i++) {
                menu.add(Menu.NONE, i, i, menuItemsPending[i]);
            }
        } else {
            for (int i = 0; i < menuItemsFailed.length; i++) {
                menu.add(Menu.NONE, i, i, menuItemsFailed[i]);
            }
        }
    }

    private void refresh() {
        Intent myIntent = new Intent(getApplicationContext(), getClass());
        finish();
        startActivity(myIntent);
    }

    public void showDatabaseDumpSuccess()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Info");
        alert.setMessage("Database was dumped successfully.");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /* Do nothing. */
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }

    public void showDatabaseDumpFailure()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Error");
        alert.setMessage("Database could not be dumped.");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /* Do nothing. */
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        int menuItemIndex = item.getItemId();
        int positon = info.position;
        /*
         * String[] menuItems = getResources().getStringArray(R.array.menu);
         * String menuItemName = menuItems[menuItemIndex]; String listItemName =
         * Countries[info.position]; TextView text =
         * (TextView)findViewById(R.id.footer);
         * text.setText(String.format("Selected %s for item %s", menuItemName,
         * listItemName));
         */

        String action;

        if (is_pending_group_open) {
            action = menuItemsPending[menuItemIndex];
        } else {
            action = menuItemsFailed[menuItemIndex];
        }

        if (action.equals(MARK_ALL)) {
            for (int i = 0; i < listView.getChildCount(); i++) {
                View v = listView.getChildAt(i);
                CheckBox c = (CheckBox) v.findViewById(R.id.queue_item_checkbox);
                c.setChecked(true);
            }
        } else if (action.equals(UNMARK_ALL)) {
            for (int i = 0; i < listView.getChildCount(); i++) {
                View v = listView.getChildAt(i);
                CheckBox c = (CheckBox) v.findViewById(R.id.queue_item_checkbox);
                c.setChecked(false);
            }
        } else if (action.equals(DELETE_SELECTED)) {
            jobControlInterface.deleteJobEntries(jobDetails.get(positon), is_pending_group_open);
            refresh();
        } else if (action.equals(DELETE_MARKED)) {
            for (int i = 0; i < listView.getChildCount(); i++) {
                View v = listView.getChildAt(i);
                CheckBox c = (CheckBox) v.findViewById(R.id.queue_item_checkbox);
                if (c.isChecked()) {
                    jobControlInterface.deleteJobEntries(jobDetails.get(i), is_pending_group_open);
                }
            }
            refresh();
        } else if (action.equals(RETRY_SELECTED)) {
            jobControlInterface.retryJobDetail(jobDetails.get(positon));
            refresh();
        } else if (action.equals(RETRY_MARKED)) {
            for (int i = 0; i < listView.getChildCount(); i++) {
                View v = listView.getChildAt(i);
                CheckBox c = (CheckBox) v.findViewById(R.id.queue_item_checkbox);
                if (c.isChecked()) {
                    jobControlInterface.retryJobDetail(jobDetails.get(i));
                }
            }
            refresh();
        } else if (action.equals(DUMP_TABLES)) {
            if (jobControlInterface.dumpQueueDatabase(null) == true)
            {
                showDatabaseDumpSuccess();
            }
            else
            {
                showDatabaseDumpFailure();
            }
        }

        return true;
    }

}
