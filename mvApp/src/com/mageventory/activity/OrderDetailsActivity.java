
package com.mageventory.activity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.components.LinkTextView;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCallback;
import com.mageventory.job.JobControlInterface;
import com.mageventory.settings.Settings;
import com.mageventory.tasks.LoadOrderDetailsData;

public class OrderDetailsActivity extends BaseActivity implements MageventoryConstants {

    /*
     * If a shipment map contains this key it means this shipment is not really
     * on the server yet. It was just injected by us.
     */
    private static final String INJECTED_SHIPMENT_FIELD = "injected_shipment_field";

    private ScrollView mOrderDetailsLayout;
    private LinearLayout mSpinningWheel;

    private LoadOrderDetailsData mLoadOrderDetailsDataTask;
    private LinkTextView mOrderNumText;
    private TextView mOrderDateText;
    private TextView mStatusText;
    private LinkTextView mCustomerNameText;
    private LinkTextView mCustomerEmailText;
    private LayoutInflater mInflater;
    private String mOrderIncrementId;
    private Settings mSettings;
    private LinearLayout mMoreDetailsLayout;
    private LinearLayout mRawDumpLayout;
    private Button mRawDumpButtonShow;
    private Button mRawDumpButtonHide;
    private Button mShipmentButton;
    private LinearLayout mLayoutShipmentPending;
    private LinearLayout mLayoutShipmentFailed;
    private TextView mTextShipmentPending;
    private TextView mTextShipmentFailed;

    private LinearLayout mLayoutCreationPending;
    private TextView mTextCreationPending;

    private List<Job> mShipmentJobs = null;
    public Job orderCreationJob;
    public JobCallback orderCreationJobCallback;

    private JobControlInterface mJobControlInterface;

    private boolean mIsResumed = false;

    private static final int INDENTATION_LEVEL_DP = 20;

    private String getProductSKU()
    {
        Map<String, Object> orderDetails = mLoadOrderDetailsDataTask.getData();
        Map<String, Object> product = (Map<String, Object>) ((JobCacheManager
                .getObjectArrayFromDeserializedItem(orderDetails.get("items")))[0]);

        return (String) product.get("sku");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.order_details_activity);

        this.setTitle("mVentory: Order details");

        Resources r = getResources();
        int shipmentButtonMarginsPix = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                5, r.getDisplayMetrics());

        mJobControlInterface = new JobControlInterface(this);

        mShipmentButton = new Button(this);
        LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        buttonLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        buttonLayoutParams.setMargins(0, shipmentButtonMarginsPix, 0, shipmentButtonMarginsPix);

        mShipmentButton.setLayoutParams(buttonLayoutParams);
        mShipmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(getApplicationContext(),
                        OrderShippingActivity.class);
                intent.putExtra(getString(R.string.ekey_order_increment_id), mOrderIncrementId);
                intent.putExtra(getString(R.string.ekey_product_sku), getProductSKU());
                startActivity(intent);
            }
        });

        mOrderDetailsLayout = (ScrollView) findViewById(R.id.order_details_layout);
        mSpinningWheel = (LinearLayout) findViewById(R.id.spinning_wheel);

        mOrderNumText = (LinkTextView) findViewById(R.id.order_num_text);
        mOrderDateText = (TextView) findViewById(R.id.order_date_text);

        mStatusText = (TextView) findViewById(R.id.status_text);
        mCustomerNameText = (LinkTextView) findViewById(R.id.customer_name_text);
        mCustomerEmailText = (LinkTextView) findViewById(R.id.customer_email_text);

        mRawDumpLayout = (LinearLayout) findViewById(R.id.raw_dump_layout);
        mMoreDetailsLayout = (LinearLayout) findViewById(R.id.more_details_layout);

        mLayoutShipmentPending = (LinearLayout) findViewById(R.id.layoutShipmentPending);
        mLayoutShipmentFailed = (LinearLayout) findViewById(R.id.layoutShipmentFailed);
        mTextShipmentPending = (TextView) findViewById(R.id.textShipmentPending);
        mTextShipmentFailed = (TextView) findViewById(R.id.textShipmentFailed);

        mLayoutCreationPending = (LinearLayout) findViewById(R.id.layoutCreationPending);
        mTextCreationPending = (TextView) findViewById(R.id.textCreationPending);

        mInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mOrderIncrementId = extras.getString(getString(R.string.ekey_order_increment_id));
        }

        mLoadOrderDetailsDataTask = new LoadOrderDetailsData(this, mOrderIncrementId, false);
        mLoadOrderDetailsDataTask.execute();

        mSettings = new Settings(this);

        mRawDumpButtonShow = (Button) findViewById(R.id.raw_dump_button_show);
        mRawDumpButtonHide = (Button) findViewById(R.id.raw_dump_button_hide);

        mRawDumpButtonShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mRawDumpLayout.getChildCount() == 0)
                {
                    rawDumpMapIntoLayout(mLoadOrderDetailsDataTask.getData(), 0);
                }

                mRawDumpLayout.setVisibility(View.VISIBLE);
                mRawDumpButtonShow.setEnabled(false);
                mRawDumpButtonHide.setEnabled(true);
            }
        });

        mRawDumpButtonHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRawDumpLayout.removeAllViews();
                mRawDumpLayout.setVisibility(View.GONE);
                mRawDumpButtonShow.setEnabled(true);
                mRawDumpButtonHide.setEnabled(false);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsResumed = false;
        if (mShipmentJobs != null)
        {
            unregisterShipmentJobCallbacks();
        }

        deregisterOrderCreationJobCallback();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsResumed = true;
        if (mShipmentJobs != null)
        {
            registerShipmentJobCallbacks();
        }

        registerOrderCreationCallback();
    }

    private void updateUIWithShipmentJobs(boolean reloadFromCache)
    {
        int pendingCount = 0;
        int failedCount = 0;

        for (Job job : mShipmentJobs)
        {
            if (job.getPending() == true)
            {
                pendingCount++;
            }
            else
            {
                failedCount++;
            }
        }

        if (pendingCount > 0)
        {
            mTextShipmentPending.setText("Shipping is pending (" + pendingCount + ")");
            mLayoutShipmentPending.setVisibility(View.VISIBLE);
        }
        else
        {
            mLayoutShipmentPending.setVisibility(View.GONE);
        }

        if (failedCount > 0)
        {
            mTextShipmentFailed.setText("Shipping failed (" + failedCount + ")");
            mLayoutShipmentFailed.setVisibility(View.VISIBLE);
        }
        else
        {
            mLayoutShipmentFailed.setVisibility(View.GONE);
        }

        /*
         * If the main spinning wheel is gone that means that the order details
         * is loaded so we can update it with the currently pending shipment
         * jobs.
         */
        if (mSpinningWheel.getVisibility() == View.GONE)
        {
            if (reloadFromCache)
            {
                mLoadOrderDetailsDataTask = new LoadOrderDetailsData(this, mOrderIncrementId, true);
                mLoadOrderDetailsDataTask.execute();

                /*
                 * If reloading from the cache then there is no point in doing
                 * the rest of the things in this function as they are going to
                 * be done again after the data gets reloaded from the cache.
                 */
                return;
            }

            /* Make the shipments list editable */
            Object[] shipments = JobCacheManager
                    .getObjectArrayFromDeserializedItem(mLoadOrderDetailsDataTask.getData().get(
                            "shipments"));

            ArrayList<Object> shipmentsList = new ArrayList<Object>();

            if (shipments != null)
            {
                for (Object shipmentObject : shipments)
                {
                    shipmentsList.add(shipmentObject);
                }
            }

            /* Remove all injected shipments */
            int iShipment = 0;
            while (iShipment < shipmentsList.size())
            {
                Map<String, Object> shipment = (Map<String, Object>) shipmentsList.get(iShipment);
                Object injectedField = shipment.get(INJECTED_SHIPMENT_FIELD);
                if (injectedField != null)
                {
                    shipmentsList.remove(iShipment);
                }
                else
                {
                    iShipment++;
                }
            }

            for (Job job : mShipmentJobs)
            {
                if (job.getPending() == true)
                {
                    Map<String, Object> jobExtras = job.getExtras();
                    Map<String, Object> newShipment = new HashMap<String, Object>();
                    newShipment.put(INJECTED_SHIPMENT_FIELD, "");

                    Map<String, Object> track = new HashMap<String, Object>();
                    track.put("created_at", "Creation pending...");
                    track.put("track_number", jobExtras.get(EKEY_SHIPMENT_TRACKING_NUMBER));
                    track.put("carrier_code", jobExtras.get(EKEY_SHIPMENT_CARRIER_CODE));
                    newShipment.put("tracks", new Object[] {
                        track
                    });

                    Map<String, Object> itemsMap = (Map<String, Object>) ((Map<String, Object>) jobExtras
                            .get(EKEY_SHIPMENT_WITH_TRACKING_PARAMS)).get(EKEY_SHIPMENT_ITEMS_QTY);
                    Object[] itemsArray = new Object[itemsMap.keySet().size()];
                    Object[] orderItems = JobCacheManager
                            .getObjectArrayFromDeserializedItem(mLoadOrderDetailsDataTask.getData()
                                    .get("items"));
                    int i = 0;
                    for (String itemID : itemsMap.keySet())
                    {
                        Map<String, Object> orderItemFound = null;

                        for (Object orderItemObject : orderItems)
                        {
                            Map<String, Object> orderItem = (Map<String, Object>) orderItemObject;
                            if (orderItem.get("item_id").equals(itemID))
                            {
                                orderItemFound = orderItem;
                                break;
                            }
                        }

                        Map<String, Object> item = new HashMap<String, Object>();
                        if (orderItemFound != null)
                        {
                            item.put("name", orderItemFound.get("name"));
                            item.put("sku", orderItemFound.get("sku"));
                        }
                        item.put("qty", itemsMap.get(itemID));

                        itemsArray[i] = item;

                        i++;
                    }
                    newShipment.put("items", itemsArray);

                    shipmentsList.add(newShipment);
                }
            }

            mLoadOrderDetailsDataTask.getData().put("shipments", shipmentsList.toArray());

            /* Check whether we need to show button for doing the shipment. */
            boolean showShipmentButton = false;

            Map<String, Double> qtyShipped = new HashMap<String, Double>();

            for (Object shipmentObject : shipmentsList)
            {
                Map<String, Object> shipment = (Map<String, Object>) shipmentObject;

                Object[] shipmentItems = JobCacheManager
                        .getObjectArrayFromDeserializedItem(shipment.get("items"));
                for (Object itemObject : shipmentItems)
                {
                    Map<String, Object> item = (Map<String, Object>) itemObject;

                    String shipmentItemID = (String) item.get("order_item_id");

                    if (qtyShipped.get(shipmentItemID) == null)
                    {
                        qtyShipped.put(shipmentItemID, 0.0);
                    }
                    qtyShipped.put(shipmentItemID, qtyShipped.get(shipmentItemID)
                            + new Double((String) item.get("qty")));
                }
            }

            for (Job shipmentJob : mShipmentJobs)
            {
                if (shipmentJob.getPending() == true)
                {
                    Map<String, Object> qtysMap = (Map<String, Object>) ((Map<String, Object>) shipmentJob
                            .getExtras().get(EKEY_SHIPMENT_WITH_TRACKING_PARAMS))
                            .get(EKEY_SHIPMENT_ITEMS_QTY);

                    for (String itemIDfromJob : qtysMap.keySet())
                    {
                        if (qtyShipped.get(itemIDfromJob) == null)
                        {
                            qtyShipped.put(itemIDfromJob, 0.0);
                        }
                        qtyShipped.put(itemIDfromJob, qtyShipped.get(itemIDfromJob)
                                + new Double((String) qtysMap.get(itemIDfromJob)));
                    }
                }
            }

            Object[] products = JobCacheManager
                    .getObjectArrayFromDeserializedItem(mLoadOrderDetailsDataTask.getData().get(
                            "items"));

            for (Object productObject : products)
            {
                Map<String, Object> product = (Map<String, Object>) productObject;
                String itemId = (String) product.get("item_id");

                double qtyOrderedD;
                double qtyShippedD = 0;

                qtyOrderedD = new Double((String) product.get("qty_ordered"));

                if (qtyShipped.get(itemId) != null)
                {
                    qtyShippedD = qtyShipped.get(itemId);
                }

                if (qtyOrderedD > qtyShippedD)
                {
                    showShipmentButton = true;
                }
            }

            fillTextViewsWithData(showShipmentButton);
        }
    }

    private JobCallback newShipmentJobCallback()
    {
        return new JobCallback() {

            final JobCallback thisCallback = this;

            @Override
            public void onJobStateChange(final Job job) {
                OrderDetailsActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (job.getFinished() == true)
                        {
                            for (int i = 0; i < mShipmentJobs.size(); i++)
                            {
                                if (job.getJobID().getTimeStamp() == mShipmentJobs.get(i)
                                        .getJobID().getTimeStamp())
                                {
                                    mShipmentJobs.remove(i);
                                    mJobControlInterface.deregisterJobCallback(job.getJobID(),
                                            thisCallback);
                                    break;
                                }
                            }
                            /* If a job was finished reload order details. */
                            updateUIWithShipmentJobs(true);
                        }
                        else
                        if (job.getPending() == false)
                        {
                            for (int i = 0; i < mShipmentJobs.size(); i++)
                            {
                                if (job.getJobID().getTimeStamp() == mShipmentJobs.get(i)
                                        .getJobID().getTimeStamp())
                                {
                                    mShipmentJobs.set(i, job);
                                    break;
                                }
                            }
                            /*
                             * If a job fails there is no need to reload order
                             * details from the cache as nothing changed in the
                             * cache.
                             */
                            updateUIWithShipmentJobs(false);
                        }
                    }

                });
            }
        };
    }

    private void registerShipmentJobCallbacks()
    {
        boolean needRefresh = false;

        Iterator<Job> i = mShipmentJobs.iterator();
        while (i.hasNext()) {
            Job job = i.next();
            if (mJobControlInterface.registerJobCallback(job.getJobID(), newShipmentJobCallback()) == false)
            {
                needRefresh = true;
                i.remove();
            }
        }

        if (needRefresh)
        {
            /*
             * If we are here it means some of the jobs are no longer present in
             * the cache which most likely means they succeeded. Let's reload
             * order dets from the cache.
             */
            updateUIWithShipmentJobs(true);
        }
    }

    private void unregisterShipmentJobCallbacks()
    {
        for (Job job : mShipmentJobs)
        {
            mJobControlInterface.deregisterJobCallback(job.getJobID(), null);
        }
    }

    private void registerOrderCreationCallback()
    {
        if (orderCreationJob != null) {
            orderCreationJobCallback = new JobCallback() {
                @Override
                public void onJobStateChange(final Job job) {
                    if (job.getFinished()) {
                        OrderDetailsActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                orderCreationJob = null;
                                mJobControlInterface.deregisterJobCallback(job.getJobID(),
                                        orderCreationJobCallback);

                                mOrderIncrementId = job.getResultData();

                                refreshData();
                                mLayoutCreationPending.setVisibility(View.GONE);
                            }
                        });
                    }
                    else
                    if (job.getPending() == false)
                    {
                        OrderDetailsActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                mLayoutCreationPending.setVisibility(View.VISIBLE);
                                mTextCreationPending.setText("Order creation failed...");
                            }
                        });
                    }
                }
            };

            mLayoutCreationPending.setVisibility(View.VISIBLE);
            mTextCreationPending.setText("Order creation pending...");

            if (!mJobControlInterface.registerJobCallback(orderCreationJob.getJobID(),
                    orderCreationJobCallback)) {
                mLayoutCreationPending.setVisibility(View.GONE);
                orderCreationJobCallback = null;
                orderCreationJob = null;
            }
        }
    }

    public void deregisterOrderCreationJobCallback()
    {
        if (orderCreationJob != null && orderCreationJobCallback != null) {
            mJobControlInterface.deregisterJobCallback(orderCreationJob.getJobID(),
                    orderCreationJobCallback);
        }
    }

    /* Shows a dialog for adding new option. */
    public void showFailureDialog() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Load failed");
        alert.setMessage("Unable to load order details.");

        alert.setPositiveButton(getString(R.string.try_again),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent myIntent = new Intent(OrderDetailsActivity.this
                                .getApplicationContext(), OrderDetailsActivity.class);
                        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        OrderDetailsActivity.this.startActivity(myIntent);
                        OrderDetailsActivity.this.finish();
                    }
                });

        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                OrderDetailsActivity.this.finish();
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }

    public void onOrderDetailsLoadStart() {
        mSpinningWheel.setVisibility(View.VISIBLE);
        mOrderDetailsLayout.setVisibility(View.GONE);

        if (mIsResumed == true)
        {
            unregisterShipmentJobCallbacks();
            deregisterOrderCreationJobCallback();
        }
        mShipmentJobs = null;
    }

    public void onOrderDetailsFailure() {
        showFailureDialog();
    }

    public void onOrderDetailsSuccess() {
        fillTextViewsWithData(true);
        mSpinningWheel.setVisibility(View.GONE);
        mOrderDetailsLayout.setVisibility(View.VISIBLE);

        mShipmentJobs = JobCacheManager.restoreShipmentJobs(getProductSKU(), mSettings.getUrl());
        orderCreationJob = JobCacheManager.restoreSellMultipleProductsJob(getProductSKU(),
                mSettings.getUrl(), mOrderIncrementId);

        if (mIsResumed == true)
        {
            registerShipmentJobCallbacks();

            /*
             * We don't want to reload order details from the cache in that
             * case. We just loaded them.
             */
            updateUIWithShipmentJobs(false);

            registerOrderCreationCallback();
        }
    }

    private void rawDumpMapIntoLayout(Map<String, Object> map, int nestingLevel)
    {
        Resources r = getResources();
        float indentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                INDENTATION_LEVEL_DP * nestingLevel, r.getDisplayMetrics());
        float arrayIndentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                INDENTATION_LEVEL_DP * (nestingLevel + 1), r.getDisplayMetrics());

        for (String key : map.keySet())
        {
            if (map.get(key) instanceof String)
            {
                LinearLayout subitem = (LinearLayout) mInflater.inflate(
                        R.layout.order_details_sub_item, null);

                View indentation = subitem.findViewById(R.id.indentation);

                indentation.setLayoutParams(new LinearLayout.LayoutParams(
                        (int) indentationWidthPix, 0));

                TextView text1 = (TextView) subitem.findViewById(R.id.text1);
                TextView text2 = (TextView) subitem.findViewById(R.id.text2);

                text1.setText(key + ": ");
                text2.setText((String) map.get(key));
                mRawDumpLayout.addView(subitem);
            }
        }

        for (String key : map.keySet())
        {
            if (map.get(key) instanceof Map)
            {
                LinearLayout subitem = (LinearLayout) mInflater.inflate(
                        R.layout.order_details_sub_item, null);

                View indentation = subitem.findViewById(R.id.indentation);

                indentation.setLayoutParams(new LinearLayout.LayoutParams(
                        (int) indentationWidthPix, 0));

                TextView text1 = (TextView) subitem.findViewById(R.id.text1);
                text1.setText(key);
                mRawDumpLayout.addView(subitem);

                rawDumpMapIntoLayout((Map<String, Object>) map.get(key), nestingLevel + 1);
            }
        }

        for (String key : map.keySet())
        {
            if (map.get(key) instanceof Object[] || map.get(key) instanceof List)
            {
                LinearLayout subitem = (LinearLayout) mInflater.inflate(
                        R.layout.order_details_sub_item, null);

                View indentation = subitem.findViewById(R.id.indentation);

                indentation.setLayoutParams(new LinearLayout.LayoutParams(
                        (int) indentationWidthPix, 0));

                TextView text1 = (TextView) subitem.findViewById(R.id.text1);
                text1.setText(key);
                mRawDumpLayout.addView(subitem);

                Object[] arrayObjectForKey = JobCacheManager.getObjectArrayFromDeserializedItem(map
                        .get(key));
                for (int i = 0; i < arrayObjectForKey.length; i++)
                {
                    LinearLayout arraySubitem = (LinearLayout) mInflater.inflate(
                            R.layout.order_details_sub_item, null);
                    View arraySubitemIndentation = arraySubitem.findViewById(R.id.indentation);

                    arraySubitemIndentation.setLayoutParams(new LinearLayout.LayoutParams(
                            (int) arrayIndentationWidthPix, 0));

                    TextView arrayItemText1 = (TextView) arraySubitem.findViewById(R.id.text1);
                    arrayItemText1.setText(key + "[" + i + "]");
                    mRawDumpLayout.addView(arraySubitem);

                    rawDumpMapIntoLayout((Map<String, Object>) (arrayObjectForKey[i]),
                            nestingLevel + 2);
                }
            }
        }
    }

    private String keyToLabel(String key)
    {
        String[] wordsArray = key.split("_");

        for (int i = 0; i < wordsArray.length; i++)
        {
            if (wordsArray[i].equals("base") && i == 0)
            {
                continue;
            }

            String firstLetter = "" + wordsArray[i].charAt(0);
            wordsArray[i] = firstLetter.toUpperCase() + wordsArray[i].substring(1);
        }

        StringBuilder label = new StringBuilder();

        for (int i = 0; i < wordsArray.length; i++)
        {
            if (wordsArray[i].equals("base") && i == 0)
            {
                continue;
            }

            label.append(wordsArray[i]);

            if (i != wordsArray.length)
            {
                label.append(" ");
            }
        }

        return label.toString();
    }

    private void rawDumpMapIntoLayout2(final Map<String, Object> map, int nestingLevel,
            ArrayList<String> keys_to_show)
    {
        Resources r = getResources();
        float indentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                INDENTATION_LEVEL_DP * nestingLevel, r.getDisplayMetrics());
        float arrayIndentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                INDENTATION_LEVEL_DP * (nestingLevel + 1), r.getDisplayMetrics());

        for (String key : keys_to_show)
        {
            if (map.keySet().contains(key) && map.get(key) instanceof String)
            {
                LinearLayout subitem = (LinearLayout) mInflater.inflate(
                        R.layout.order_details_sub_item, null);

                View indentation = subitem.findViewById(R.id.indentation);

                indentation.setLayoutParams(new LinearLayout.LayoutParams(
                        (int) indentationWidthPix, 0));

                TextView text1 = (TextView) subitem.findViewById(R.id.text1);
                TextView text2 = (TextView) subitem.findViewById(R.id.text2);

                text1.setText(keyToLabel(key) + ": ");

                if (key.equals("created_at"))
                {
                    text2.setText(removeSeconds((String) map.get(key)));
                }
                else
                {
                    text2.setText((String) map.get(key));
                }

                mMoreDetailsLayout.addView(subitem);
            }
        }

        for (String key : map.keySet())
        {
            if (keys_to_show.contains(key) && map.get(key) instanceof Map)
            {
                LinearLayout subitem = (LinearLayout) mInflater.inflate(
                        R.layout.order_details_sub_item, null);

                View indentation = subitem.findViewById(R.id.indentation);

                indentation.setLayoutParams(new LinearLayout.LayoutParams(
                        (int) indentationWidthPix, 0));

                TextView text1 = (TextView) subitem.findViewById(R.id.text1);
                text1.setText(keyToLabel(key));
                mMoreDetailsLayout.addView(subitem);

                rawDumpMapIntoLayout((Map<String, Object>) map.get(key), nestingLevel + 1);
            }
        }

        for (String key : map.keySet())
        {
            if (keys_to_show.contains(key)
                    && (map.get(key) instanceof Object[] || map.get(key) instanceof List))
            {
                LinearLayout subitem = (LinearLayout) mInflater.inflate(
                        R.layout.order_details_sub_item, null);

                View indentation = subitem.findViewById(R.id.indentation);

                indentation.setLayoutParams(new LinearLayout.LayoutParams(
                        (int) indentationWidthPix, 0));

                TextView text1 = (TextView) subitem.findViewById(R.id.text1);
                text1.setText(keyToLabel(key));
                mMoreDetailsLayout.addView(subitem);

                Object[] arrayObjectForKey = JobCacheManager.getObjectArrayFromDeserializedItem(map
                        .get(key));
                for (int i = 0; i < arrayObjectForKey.length; i++)
                {
                    LinearLayout arraySubitem = (LinearLayout) mInflater.inflate(
                            R.layout.order_details_sub_item, null);
                    View arraySubitemIndentation = arraySubitem.findViewById(R.id.indentation);

                    arraySubitemIndentation.setLayoutParams(new LinearLayout.LayoutParams(
                            (int) arrayIndentationWidthPix, 0));

                    TextView arrayItemText1 = (TextView) arraySubitem.findViewById(R.id.text1);
                    arrayItemText1.setText(keyToLabel(key + "[" + i + "]"));
                    mMoreDetailsLayout.addView(arraySubitem);

                    rawDumpMapIntoLayout2(
                            (Map<String, Object>) arrayObjectForKey[i], nestingLevel + 2,
                            keys_to_show);
                }
            }
        }
    }

    private void rawDumpMapIntoLayout3(String key, Object[] array, int nestingLevel,
            ArrayList<String> keys_to_show)
    {
        Resources r = getResources();
        float indentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                INDENTATION_LEVEL_DP * nestingLevel, r.getDisplayMetrics());

        for (int i = 0; i < array.length; i++)
        {
            LinearLayout arraySubitem = (LinearLayout) mInflater.inflate(
                    R.layout.order_details_sub_item, null);
            View arraySubitemIndentation = arraySubitem.findViewById(R.id.indentation);

            arraySubitemIndentation.setLayoutParams(new LinearLayout.LayoutParams(
                    (int) indentationWidthPix, 0));

            TextView arrayItemText1 = (TextView) arraySubitem.findViewById(R.id.text1);
            arrayItemText1.setText(keyToLabel(key + "[" + i + "]"));
            mMoreDetailsLayout.addView(arraySubitem);

            rawDumpMapIntoLayout2((Map<String, Object>) array[i], nestingLevel + 1, keys_to_show);
        }
    }

    private void createShippingAddressSection()
    {
        Map<String, Object> data = (Map<String, Object>) mLoadOrderDetailsDataTask.getData().get(
                "shipping_address");

        Resources r = getResources();
        float indentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                INDENTATION_LEVEL_DP, r.getDisplayMetrics());

        LinearLayout header = (LinearLayout) mInflater.inflate(R.layout.order_details_sub_item,
                null);
        TextView headerText = (TextView) header.findViewById(R.id.text1);
        headerText.setText("Delivery Address");
        headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        mMoreDetailsLayout.addView(header);

        if (data != null)
        {
            String address = "https://maps.google.com/maps?q=" + (String) data.get("country_id")
                    + ", " + (String) data.get("city") + ", " + (String) data.get("street");
            address = address.replace(' ', '+');

            LinkTextView addressText = (LinkTextView) mInflater.inflate(
                    R.layout.order_details_link_textview, null);
            addressText.setTextAndURL(
                    (String) data.get("firstname") + " " + (String) data.get("lastname") + ", "
                            + (String) data.get("street") + ", " + (String) data.get("city") + ", "
                            + (String) data.get("postcode") + ", "
                            + (String) data.get("country_id"), address);
            mMoreDetailsLayout.addView(addressText);

            LinearLayout empty_space = (LinearLayout) mInflater.inflate(
                    R.layout.order_details_sub_item, null);
            empty_space.findViewById(R.id.indentation).setLayoutParams(
                    new LinearLayout.LayoutParams((int) indentationWidthPix, 0));
            ((TextView) empty_space.findViewById(R.id.text1)).setVisibility(View.GONE);
            ((TextView) empty_space.findViewById(R.id.text2)).setText("");
            mMoreDetailsLayout.addView(empty_space);

            LinkTextView telephone = (LinkTextView) mInflater.inflate(
                    R.layout.order_details_link_textview, null);
            telephone.setTextAndURL((String) data.get("telephone"),
                    "tel://" + (String) data.get("telephone"));

            mMoreDetailsLayout.addView(telephone);
        }
    }

    private void createCreditMemosSection()
    {
        final String[] KEYS_TO_SHOW = {
                "credit_memos", "increment_id", "created_at", "base_adjustment_negative",
                "base_shipping_amount",
                "base_hidden_tax_amount", "base_subtotal_incl_tax", "base_discount_amount",
                "base_shipping_tax_amount", "base_shipping_incl_tax",
                "base_subtotal", "base_adjustment_positive", "base_grand_total"
        };

        ArrayList<String> keysToShowArrayList = new ArrayList<String>();
        Collections.addAll(keysToShowArrayList, KEYS_TO_SHOW);

        LinearLayout header = (LinearLayout) mInflater.inflate(R.layout.order_details_sub_item,
                null);
        TextView headerText = (TextView) header.findViewById(R.id.text1);
        headerText.setText("Credit Memos");
        headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

        mMoreDetailsLayout.addView(header);

        if (mLoadOrderDetailsDataTask.getData().get("credit_memos") != null)
        {
            rawDumpMapIntoLayout3("credit_memos",
                    JobCacheManager.getObjectArrayFromDeserializedItem(mLoadOrderDetailsDataTask
                            .getData().get("credit_memos")), 1, keysToShowArrayList);
        }
    }

    private String getIsCustomerNotifiedText(String valueFromServer)
    {
        if (valueFromServer.equals("0"))
            return "No";
        else if (valueFromServer.equals("1"))
            return "Yes";
        else
            return "N/A";
    }

    private void createStatusHistorySection()
    {
        Rect bounds = new Rect();
        int notifiedColumnWidthPix;

        LinearLayout header = (LinearLayout) mInflater.inflate(R.layout.order_details_sub_item,
                null);
        TextView headerText = (TextView) header.findViewById(R.id.text1);
        headerText.setText("Status History");
        headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

        mMoreDetailsLayout.addView(header);

        if (mLoadOrderDetailsDataTask.getData().get("status_history") != null)
        {
            LinearLayout statusesLayout = new LinearLayout(this);
            statusesLayout.setBackgroundColor(0x44444444);
            statusesLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            statusesLayout.setOrientation(LinearLayout.VERTICAL);

            LinearLayout statusLayoutHeader = new LinearLayout(this);
            statusLayoutHeader.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView entityTextHeader = (TextView) mInflater.inflate(
                    R.layout.order_details_textview_header, null);
            entityTextHeader.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 2));
            entityTextHeader.setText("Entity");
            entityTextHeader.setBackgroundColor(0x66666666);
            entityTextHeader.setGravity(Gravity.CENTER_HORIZONTAL);

            TextView statusTextHeader = (TextView) mInflater.inflate(
                    R.layout.order_details_textview_header, null);
            statusTextHeader.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 2));
            statusTextHeader.setText("Status");
            statusTextHeader.setBackgroundColor(0x66666666);
            statusTextHeader.setGravity(Gravity.CENTER_HORIZONTAL);

            TextView createdTextHeader = (TextView) mInflater.inflate(
                    R.layout.order_details_textview_header, null);
            createdTextHeader.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 2));
            createdTextHeader.setText("Created");
            createdTextHeader.setBackgroundColor(0x66666666);
            createdTextHeader.setGravity(Gravity.CENTER_HORIZONTAL);

            TextView notifiedTextHeader = (TextView) mInflater.inflate(
                    R.layout.order_details_textview_header, null);
            notifiedTextHeader.setText("Notified");
            notifiedTextHeader.setBackgroundColor(0x66666666);
            notifiedTextHeader.getPaint().getTextBounds("Notified-", 0, "Notified-".length(),
                    bounds);
            notifiedColumnWidthPix = bounds.width();
            notifiedTextHeader.setLayoutParams(new LinearLayout.LayoutParams(
                    notifiedColumnWidthPix, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
            notifiedTextHeader.setGravity(Gravity.CENTER_HORIZONTAL);

            statusLayoutHeader.addView(entityTextHeader);
            statusLayoutHeader.addView(statusTextHeader);
            statusLayoutHeader.addView(createdTextHeader);
            statusLayoutHeader.addView(notifiedTextHeader);

            statusesLayout.addView(statusLayoutHeader);

            Object[] statuses = JobCacheManager
                    .getObjectArrayFromDeserializedItem(mLoadOrderDetailsDataTask.getData().get(
                            "status_history"));

            for (int i = 0; i < statuses.length; i++)
            {
                final Map<String, Object> status = (Map<String, Object>) statuses[i];

                LinearLayout statusLayout = new LinearLayout(this);
                statusLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                TextView entityText = (TextView) mInflater.inflate(R.layout.order_details_textview,
                        null);
                entityText.setLayoutParams(new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 2));
                entityText.setGravity(Gravity.CENTER_HORIZONTAL);

                TextView statusText = (TextView) mInflater.inflate(R.layout.order_details_textview,
                        null);
                statusText.setLayoutParams(new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 2));
                statusText.setGravity(Gravity.CENTER_HORIZONTAL);

                TextView createdText = (TextView) mInflater.inflate(
                        R.layout.order_details_textview, null);
                createdText.setLayoutParams(new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 2));
                createdText.setGravity(Gravity.CENTER_HORIZONTAL);

                TextView notifiedText = (TextView) mInflater.inflate(
                        R.layout.order_details_textview, null);
                notifiedText.setLayoutParams(new LinearLayout.LayoutParams(notifiedColumnWidthPix,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 0));
                notifiedText.setGravity(Gravity.CENTER_HORIZONTAL);

                entityText.setText((String) status.get("entity_name"));
                statusText.setText((String) status.get("status"));
                createdText.setText(removeSeconds((String) status.get("created_at")));
                notifiedText.setText(getIsCustomerNotifiedText((String) status
                        .get("is_customer_notified")));

                statusLayout.addView(entityText);
                statusLayout.addView(statusText);
                statusLayout.addView(createdText);
                statusLayout.addView(notifiedText);

                statusesLayout.addView(statusLayout);
            }

            mMoreDetailsLayout.addView(statusesLayout);
        }
    }

    public static String formatPrice(String number)
    {
        return "$" + String.format(Locale.US, "%.2f", new Double(number));
    }

    public static String formatQuantity(String number)
    {
        if (new Double(number) == Math.round(new Double(number)))
        {
            return "" + Math.round(new Double(number));
        }
        else
        {
            return String.format(Locale.US, "%.2f", new Double(number));
        }
    }

    private void createProductsListSection()
    {
        Resources r = getResources();
        int priceColumnWidthPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70,
                r.getDisplayMetrics());
        int qtyColumnWidthPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40,
                r.getDisplayMetrics());

        LinearLayout header = (LinearLayout) mInflater.inflate(R.layout.order_details_sub_item,
                null);
        TextView headerText = (TextView) header.findViewById(R.id.text1);
        headerText.setText("Products List");
        headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

        mMoreDetailsLayout.addView(header);

        if (mLoadOrderDetailsDataTask.getData().get("items") != null)
        {
            LinearLayout productsLayout = new LinearLayout(this);
            productsLayout.setBackgroundColor(0x44444444);
            productsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            productsLayout.setOrientation(LinearLayout.VERTICAL);

            LinearLayout productLayoutHeader = new LinearLayout(this);
            productLayoutHeader.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView productNameTextHeader = (TextView) mInflater.inflate(
                    R.layout.order_details_textview_header, null);
            productNameTextHeader.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            productNameTextHeader.setText("Product name");
            productNameTextHeader.setBackgroundColor(0x66666666);

            TextView priceTextHeader = (TextView) mInflater.inflate(
                    R.layout.order_details_textview_header, null);
            priceTextHeader.setLayoutParams(new LinearLayout.LayoutParams(priceColumnWidthPx,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0));
            priceTextHeader.setText("Price");
            priceTextHeader.setBackgroundColor(0x66666666);
            priceTextHeader.setGravity(Gravity.RIGHT);

            TextView qtyTextHeader = (TextView) mInflater.inflate(
                    R.layout.order_details_textview_header, null);
            qtyTextHeader.setLayoutParams(new LinearLayout.LayoutParams(qtyColumnWidthPx,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0));
            qtyTextHeader.setText("Qty");
            qtyTextHeader.setBackgroundColor(0x66666666);
            qtyTextHeader.setGravity(Gravity.RIGHT);

            productLayoutHeader.addView(productNameTextHeader);
            productLayoutHeader.addView(priceTextHeader);
            productLayoutHeader.addView(qtyTextHeader);

            productsLayout.addView(productLayoutHeader);

            Object[] products = JobCacheManager
                    .getObjectArrayFromDeserializedItem(mLoadOrderDetailsDataTask.getData().get(
                            "items"));

            for (int i = 0; i < products.length; i++)
            {
                final Map<String, Object> product = (Map<String, Object>) products[i];

                LinearLayout productLayout = new LinearLayout(this);
                productLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                LinkTextView productNameText = (LinkTextView) mInflater.inflate(
                        R.layout.order_details_link_textview, null);
                productNameText
                        .setLayoutParams(new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                TextView priceText = (TextView) mInflater.inflate(R.layout.order_details_textview,
                        null);
                priceText.setLayoutParams(new LinearLayout.LayoutParams(priceColumnWidthPx,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 0));
                priceText.setGravity(Gravity.RIGHT);

                TextView qtyText = (TextView) mInflater.inflate(R.layout.order_details_textview,
                        null);
                qtyText.setLayoutParams(new LinearLayout.LayoutParams(qtyColumnWidthPx,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 0));
                qtyText.setGravity(Gravity.RIGHT);

                productNameText.setTextAndOnClickListener((String) product.get("name"),
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent newIntent = new Intent(OrderDetailsActivity.this,
                                        ProductDetailsActivity.class);

                                newIntent.putExtra(getString(R.string.ekey_product_sku),
                                        (String) (product.get("sku")));
                                newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                OrderDetailsActivity.this.startActivity(newIntent);
                            }
                        });

                priceText.setText(formatPrice((String) product.get("price_incl_tax")));
                qtyText.setText(formatQuantity((String) product.get("qty_ordered")));

                productLayout.addView(productNameText);
                productLayout.addView(priceText);
                productLayout.addView(qtyText);

                productsLayout.addView(productLayout);
            }

            // TOTAL
            LinearLayout totalLayout = new LinearLayout(this);
            totalLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView totalText = (TextView) mInflater
                    .inflate(R.layout.order_details_textview, null);
            totalText.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            TextView totalValueText = (TextView) mInflater.inflate(R.layout.order_details_textview,
                    null);
            totalValueText.setLayoutParams(new LinearLayout.LayoutParams(priceColumnWidthPx,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0));
            totalValueText.setGravity(Gravity.RIGHT);

            TextView totalEmptyText = (TextView) mInflater.inflate(R.layout.order_details_textview,
                    null);
            totalEmptyText.setLayoutParams(new LinearLayout.LayoutParams(qtyColumnWidthPx,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0));
            totalEmptyText.setGravity(Gravity.RIGHT);

            totalText.setText("Total");
            totalValueText
                    .setText(formatPrice((String) ((Map<String, Object>) mLoadOrderDetailsDataTask
                            .getData().get("payment")).get("base_amount_ordered")));

            totalLayout.addView(totalText);
            totalLayout.addView(totalValueText);
            totalLayout.addView(totalEmptyText);

            productsLayout.addView(totalLayout);

            // AMOUNT PAID
            LinearLayout amountPaidLayout = new LinearLayout(this);
            amountPaidLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView amountPaidText = (TextView) mInflater.inflate(R.layout.order_details_textview,
                    null);
            amountPaidText.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            TextView amountPaidValueText = (TextView) mInflater.inflate(
                    R.layout.order_details_textview, null);
            amountPaidValueText.setLayoutParams(new LinearLayout.LayoutParams(priceColumnWidthPx,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0));
            amountPaidValueText.setGravity(Gravity.RIGHT);

            TextView amountPaidEmptyText = (TextView) mInflater.inflate(
                    R.layout.order_details_textview, null);
            amountPaidEmptyText.setLayoutParams(new LinearLayout.LayoutParams(qtyColumnWidthPx,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0));
            amountPaidEmptyText.setGravity(Gravity.RIGHT);

            amountPaidText.setText("Paid ("
                    + (String) ((Map<String, Object>) mLoadOrderDetailsDataTask.getData().get(
                            "payment")).get("method") + ")");

            String paidValue = (String) ((Map<String, Object>) mLoadOrderDetailsDataTask.getData()
                    .get("payment")).get("amount_paid");
            if (paidValue == null)
            {
                paidValue = "0";
            }
            amountPaidValueText.setText(formatPrice(paidValue));

            amountPaidLayout.addView(amountPaidText);
            amountPaidLayout.addView(amountPaidValueText);
            amountPaidLayout.addView(amountPaidEmptyText);

            productsLayout.addView(amountPaidLayout);

            mMoreDetailsLayout.addView(productsLayout);
        }
    }

    private void createShipmentsSection(boolean canCreateNewShipments)
    {
        Resources r = getResources();
        int qtyColumnWidthPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40,
                r.getDisplayMetrics());

        LinearLayout header = (LinearLayout) mInflater.inflate(R.layout.order_details_sub_item,
                null);
        TextView headerText = (TextView) header.findViewById(R.id.text1);
        headerText.setText("Shipments");
        headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

        mMoreDetailsLayout.addView(header);

        if (mLoadOrderDetailsDataTask.getData().get("shipments") != null)
        {
            Object[] shipments = JobCacheManager
                    .getObjectArrayFromDeserializedItem(mLoadOrderDetailsDataTask.getData().get(
                            "shipments"));

            for (Object shipmentObject : shipments)
            {
                Map<String, Object> shipment = (Map<String, Object>) shipmentObject;
                Object[] tracks = JobCacheManager.getObjectArrayFromDeserializedItem(shipment
                        .get("tracks"));

                for (Object trackObject : tracks)
                {
                    Map<String, Object> track = (Map<String, Object>) trackObject;

                    View separator = new View(this);
                    separator
                            .setLayoutParams(new LinearLayout.LayoutParams(0, (int) TypedValue
                                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
                                            r.getDisplayMetrics())));
                    mMoreDetailsLayout.addView(separator);

                    TextView trackDateTrackingText = (TextView) mInflater.inflate(
                            R.layout.order_details_textview, null);
                    trackDateTrackingText.setText(removeSeconds((String) track.get("created_at"))
                            + ", #" + (String) track.get("track_number") + " via "
                            + (String) track.get("title"));
                    mMoreDetailsLayout.addView(trackDateTrackingText);
                }

                LinearLayout productsLayout = new LinearLayout(this);
                productsLayout.setBackgroundColor(0x44444444);
                productsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                productsLayout.setOrientation(LinearLayout.VERTICAL);

                LinearLayout productLayoutHeader = new LinearLayout(this);
                productLayoutHeader.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                TextView productNameTextHeader = (TextView) mInflater.inflate(
                        R.layout.order_details_textview_header, null);
                productNameTextHeader
                        .setLayoutParams(new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                productNameTextHeader.setText("Product name");
                productNameTextHeader.setBackgroundColor(0x66666666);

                TextView qtyTextHeader = (TextView) mInflater.inflate(
                        R.layout.order_details_textview_header, null);
                qtyTextHeader.setLayoutParams(new LinearLayout.LayoutParams(qtyColumnWidthPx,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 0));
                qtyTextHeader.setText("Qty");
                qtyTextHeader.setBackgroundColor(0x66666666);
                qtyTextHeader.setGravity(Gravity.RIGHT);

                productLayoutHeader.addView(productNameTextHeader);
                productLayoutHeader.addView(qtyTextHeader);

                productsLayout.addView(productLayoutHeader);

                Object[] products = JobCacheManager.getObjectArrayFromDeserializedItem(shipment
                        .get("items"));

                for (int i = 0; i < products.length; i++)
                {
                    final Map<String, Object> product = (Map<String, Object>) products[i];

                    LinearLayout productLayout = new LinearLayout(this);
                    productLayout.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));

                    LinkTextView productNameText = (LinkTextView) mInflater.inflate(
                            R.layout.order_details_link_textview, null);
                    productNameText.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                    TextView qtyText = (TextView) mInflater.inflate(
                            R.layout.order_details_textview, null);
                    qtyText.setLayoutParams(new LinearLayout.LayoutParams(qtyColumnWidthPx,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 0));
                    qtyText.setGravity(Gravity.RIGHT);

                    productNameText.setTextAndOnClickListener((String) product.get("name"),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent newIntent = new Intent(OrderDetailsActivity.this,
                                            ProductDetailsActivity.class);

                                    newIntent.putExtra(getString(R.string.ekey_product_sku),
                                            (String) (product.get("sku")));
                                    newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                    OrderDetailsActivity.this.startActivity(newIntent);
                                }
                            });

                    qtyText.setText(formatQuantity((String) product.get("qty")));

                    productLayout.addView(productNameText);
                    productLayout.addView(qtyText);

                    if (new Double((String) product.get("qty")) > 0)
                        productsLayout.addView(productLayout);
                }

                mMoreDetailsLayout.addView(productsLayout);
            }

        }

        mMoreDetailsLayout.addView(mShipmentButton);

        if (canCreateNewShipments)
        {
            mShipmentButton.setEnabled(true);
            mShipmentButton.setText("Add new shipment");
        }
        else
        {
            mShipmentButton.setEnabled(false);
            mShipmentButton.setText("Order shipped");
        }
    }

    private void createCommentsSection()
    {
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        ArrayList<Object> commentsList = new ArrayList<Object>();

        LinearLayout header = (LinearLayout) mInflater.inflate(R.layout.order_details_sub_item,
                null);
        TextView headerText = (TextView) header.findViewById(R.id.text1);
        headerText.setText("Comments");
        headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

        mMoreDetailsLayout.addView(header);

        if (mLoadOrderDetailsDataTask.getData().get("credit_memos") != null)
        {
            Object[] creditMemos = JobCacheManager
                    .getObjectArrayFromDeserializedItem(mLoadOrderDetailsDataTask.getData().get(
                            "credit_memos"));

            for (Object cm : creditMemos)
            {
                Map<String, Object> creditMemo = (Map<String, Object>) cm;

                if (creditMemo.get("comments") != null)
                {
                    for (Object comm : JobCacheManager
                            .getObjectArrayFromDeserializedItem(creditMemo.get("comments")))
                    {
                        Map<String, Object> commMap = (Map<String, Object>) comm;
                        commMap.put("description", "Credit memo #" + creditMemo.get("increment_id"));
                        commentsList.add(comm);
                    }
                }
            }
        }

        if (mLoadOrderDetailsDataTask.getData().get("invoices") != null)
        {
            Object[] invoices = JobCacheManager
                    .getObjectArrayFromDeserializedItem(mLoadOrderDetailsDataTask.getData().get(
                            "invoices"));

            for (Object inv : invoices)
            {
                Map<String, Object> invoice = (Map<String, Object>) inv;

                if (invoice.get("comments") != null)
                {
                    for (Object comm : JobCacheManager.getObjectArrayFromDeserializedItem(invoice
                            .get("comments")))
                    {
                        Map<String, Object> commMap = (Map<String, Object>) comm;
                        commMap.put("description", "Invoice #" + invoice.get("increment_id"));
                        commentsList.add(comm);
                    }
                }
            }
        }

        if (mLoadOrderDetailsDataTask.getData().get("shipments") != null)
        {
            Object[] shipments = JobCacheManager
                    .getObjectArrayFromDeserializedItem(mLoadOrderDetailsDataTask.getData().get(
                            "shipments"));

            for (Object shi : shipments)
            {
                Map<String, Object> shipment = (Map<String, Object>) shi;

                if (shipment.get("comments") != null)
                {
                    for (Object comm : JobCacheManager.getObjectArrayFromDeserializedItem(shipment
                            .get("comments")))
                    {
                        Map<String, Object> commMap = (Map<String, Object>) comm;
                        commMap.put("description", "Shipment #" + shipment.get("increment_id"));
                        commentsList.add(comm);
                    }
                }
            }
        }

        if (mLoadOrderDetailsDataTask.getData().get("status_history") != null)
        {
            Object[] statuses = JobCacheManager
                    .getObjectArrayFromDeserializedItem(mLoadOrderDetailsDataTask.getData().get(
                            "status_history"));

            for (Object st : statuses)
            {
                Map<String, Object> status = (Map<String, Object>) st;

                if (status.get("comment") != null)
                {
                    status.put("description", "Status: " + status.get("status"));
                    commentsList.add(st);
                }
            }
        }

        Collections.sort(commentsList, new Comparator<Object>() {

            @Override
            public int compare(Object lhs, Object rhs) {

                Date leftDate = null;
                Date rightDate = null;

                try {
                    leftDate = dateFormatter.parse((String) ((Map<String, Object>) lhs)
                            .get("created_at"));
                    rightDate = dateFormatter.parse((String) ((Map<String, Object>) rhs)
                            .get("created_at"));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (leftDate == null || rightDate == null)
                    return 0;

                if (leftDate.after(rightDate))
                {
                    return 1;
                }
                else
                if (rightDate.after(leftDate))
                {
                    return -1;
                }

                return 0;
            }
        });

        Rect bounds = new Rect();
        int descriptionColumnWidthPix;
        int createdAtColumnWidthPix;
        int notifiedColumnWidthPix;

        LinearLayout commentsLayout = new LinearLayout(this);
        commentsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        commentsLayout.setOrientation(LinearLayout.VERTICAL);
        commentsLayout.setBackgroundColor(0x44444444);

        LinearLayout commentLayoutHeader = new LinearLayout(this);
        commentLayoutHeader.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView commentTextHeader = (TextView) mInflater.inflate(
                R.layout.order_details_textview_header, null);
        commentTextHeader.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        commentTextHeader.setText("Comment");
        commentTextHeader.setBackgroundColor(0x66666666);

        TextView desciptionTextHeader = (TextView) mInflater.inflate(
                R.layout.order_details_textview_header, null);
        desciptionTextHeader.setGravity(Gravity.CENTER_HORIZONTAL);
        desciptionTextHeader.setText("Description");
        desciptionTextHeader.setBackgroundColor(0x66666666);
        desciptionTextHeader.getPaint().getTextBounds("#0000000000", 0, "#0000000000".length(),
                bounds);
        descriptionColumnWidthPix = bounds.width();
        desciptionTextHeader.setLayoutParams(new LinearLayout.LayoutParams(
                descriptionColumnWidthPix, ViewGroup.LayoutParams.WRAP_CONTENT, 0));

        TextView createdAtTextHeader = (TextView) mInflater.inflate(
                R.layout.order_details_textview_header, null);
        createdAtTextHeader.setGravity(Gravity.CENTER_HORIZONTAL);
        createdAtTextHeader.setText("Created");
        createdAtTextHeader.setBackgroundColor(0x66666666);
        createdAtTextHeader.getPaint().getTextBounds("2012-01-01-", 0, "2012-01-01-".length(),
                bounds);
        createdAtColumnWidthPix = bounds.width();
        createdAtTextHeader.setLayoutParams(new LinearLayout.LayoutParams(createdAtColumnWidthPix,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0));

        TextView notifiedTextHeader = (TextView) mInflater.inflate(
                R.layout.order_details_textview_header, null);
        notifiedTextHeader.setGravity(Gravity.CENTER_HORIZONTAL);
        notifiedTextHeader.setText("Notified");
        notifiedTextHeader.setBackgroundColor(0x66666666);
        notifiedTextHeader.getPaint().getTextBounds("Notified-", 0, "Notified-".length(), bounds);
        notifiedColumnWidthPix = bounds.width();
        notifiedTextHeader.setLayoutParams(new LinearLayout.LayoutParams(notifiedColumnWidthPix,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0));

        commentLayoutHeader.addView(commentTextHeader);
        commentLayoutHeader.addView(desciptionTextHeader);
        commentLayoutHeader.addView(createdAtTextHeader);
        commentLayoutHeader.addView(notifiedTextHeader);

        commentsLayout.addView(commentLayoutHeader);

        for (Object comment : commentsList)
        {
            final Map<String, Object> product = (Map<String, Object>) comment;

            LinearLayout commentLayout = new LinearLayout(this);
            commentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView commentText = (TextView) mInflater.inflate(R.layout.order_details_textview,
                    null);
            commentText.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            TextView desciptionText = (TextView) mInflater.inflate(R.layout.order_details_textview,
                    null);
            desciptionText.setLayoutParams(new LinearLayout.LayoutParams(descriptionColumnWidthPix,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0));
            desciptionText.setGravity(Gravity.CENTER_HORIZONTAL);

            TextView createdAtText = (TextView) mInflater.inflate(R.layout.order_details_textview,
                    null);
            createdAtText.setLayoutParams(new LinearLayout.LayoutParams(createdAtColumnWidthPix,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0));
            createdAtText.setGravity(Gravity.CENTER_HORIZONTAL);

            TextView notifiedText = (TextView) mInflater.inflate(R.layout.order_details_textview,
                    null);
            notifiedText.setLayoutParams(new LinearLayout.LayoutParams(notifiedColumnWidthPix,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0));
            notifiedText.setGravity(Gravity.CENTER_HORIZONTAL);

            commentText.setText((String) product.get("comment"));
            desciptionText.setText((String) product.get("description"));
            createdAtText.setText(removeSeconds((String) product.get("created_at")));
            notifiedText.setText(getIsCustomerNotifiedText((String) product
                    .get("is_customer_notified")));

            commentLayout.addView(commentText);
            commentLayout.addView(desciptionText);
            commentLayout.addView(createdAtText);
            commentLayout.addView(notifiedText);

            commentsLayout.addView(commentLayout);
        }

        mMoreDetailsLayout.addView(commentsLayout);
    }

    public static String removeSeconds(String dateTime)
    {
        int lastColon = dateTime.lastIndexOf(':');

        if (lastColon != -1)
        {
            return dateTime.substring(0, dateTime.lastIndexOf(':'));
        }
        else
        {
            return dateTime;
        }
    }

    private void fillTextViewsWithData(boolean showShipmentButton)
    {
        if (mLoadOrderDetailsDataTask == null || mLoadOrderDetailsDataTask.getData() == null)
            return;

        String orderLink = mSettings.getUrl() + "/index.php/admin/sales_order/view/order_id/"
                + (String) mLoadOrderDetailsDataTask.getData().get("order_id");

        mOrderNumText.setTextAndURL(
                "#" + (String) mLoadOrderDetailsDataTask.getData().get("increment_id"), orderLink);
        mOrderDateText.setText(removeSeconds((String) mLoadOrderDetailsDataTask.getData().get(
                "created_at")));

        mStatusText.setText(Html.fromHtml("<font color=\"#ffffff\">Status:</font> "
                + (String) mLoadOrderDetailsDataTask.getData().get("status")));

        String customerLink = mSettings.getUrl() + "/index.php/admin/customer/edit/id/"
                + (String) mLoadOrderDetailsDataTask.getData().get("customer_id");

        String customerFirstName = (String) mLoadOrderDetailsDataTask.getData().get(
                "customer_firstname");
        String customerEmail = (String) mLoadOrderDetailsDataTask.getData().get("customer_email");

        if (customerFirstName != null)
        {
            mCustomerNameText.setTextAndURL(
                    (String) mLoadOrderDetailsDataTask.getData().get("customer_firstname"),
                    customerLink);
            mCustomerNameText.setVisibility(View.VISIBLE);
        }
        else
        {
            mCustomerNameText.setVisibility(View.GONE);
        }

        if (customerEmail != null)
        {
            mCustomerEmailText.setTextAndURL(
                    (String) mLoadOrderDetailsDataTask.getData().get("customer_email"), "mailto:"
                            + (String) mLoadOrderDetailsDataTask.getData().get("customer_email"));
            mCustomerEmailText.setVisibility(View.VISIBLE);
        }
        else
        {
            mCustomerEmailText.setVisibility(View.GONE);
        }

        mMoreDetailsLayout.removeAllViews();

        // //////////////

        createProductsListSection();
        // createPaymentSection();
        createShippingAddressSection();
        createShipmentsSection(showShipmentButton);
        createCreditMemosSection();
        createStatusHistorySection();
        createCommentsSection();

        // //////////////

        // rawDumpMapIntoLayout(mLoadOrderDetailsDataTask.getData(), 0);
    }

    public void refreshData()
    {
        /*
         * If the spinning wheel is gone we can be sure no other load task is
         * pending so we can start another one.
         */
        if (mSpinningWheel.getVisibility() == View.GONE)
        {
            mLoadOrderDetailsDataTask = new LoadOrderDetailsData(this, mOrderIncrementId, true);
            mLoadOrderDetailsDataTask.execute();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {

            refreshData();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
