
package com.mageventory.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.components.LinkTextView;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.CarriersList;
import com.mageventory.tasks.LoadOrderAndShipmentJobs;
import com.mageventory.tasks.LoadOrderCarriers;
import com.mageventory.tasks.ShipProduct;
import com.mageventory.util.ScanUtils;

public class OrderShippingActivity extends BaseActivity implements MageventoryConstants {

    private static final String CUSTOM_VALUE_ID = "custom";

    private LayoutInflater mInflater;

    private LoadOrderAndShipmentJobs mLoadOrderAndShipmentJobsTask = null;
    private LoadOrderCarriers mLoadOrderCarriersTask = null;

    private boolean mIsActivityAlive;
    private ProgressDialog mProgressDialog;
    private String mOrderIncrementId;
    private String mSKU;
    private ProgressBar mCarrierProgress;
    private AutoCompleteTextView mCarrierEdit;
    private EditText mTrackingNumberEdit;
    private EditText mCommentEdit;
    private TextView mCarrierText;
    private LinearLayout mShipmentProductsLayout;
    private Button mButton;

    private ArrayList<String> mOrderItemIDList;

    private boolean mOrderIsLoading = false;
    private boolean mOrderCarriersAreLoading = false;

    private boolean mFirstOrderDetailsLoad = true;

    public static class OrderDataAndShipmentJobs
    {
        public Map<String, Object> mOrderData;
        public List<Job> mShipmentJobs;
    }

    public void scanTrackingNumber()
    {
        ScanUtils.startScanActivityForResult(OrderShippingActivity.this, SCAN_BARCODE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_shipping_activity);
        mIsActivityAlive = true;

        mInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mCarrierProgress = (ProgressBar) findViewById(R.id.carrier_progress);

        mCarrierEdit = (AutoCompleteTextView) findViewById(R.id.carrier_edit);
        mTrackingNumberEdit = (EditText) findViewById(R.id.tracking_number_edit);

        mTrackingNumberEdit.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                scanTrackingNumber();

                return false;
            }
        });

        mCommentEdit = (EditText) findViewById(R.id.comment_edit);
        mCarrierText = (TextView) findViewById(R.id.carrier_text);
        mShipmentProductsLayout = (LinearLayout) findViewById(R.id.shipment_products);
        mButton = (Button) findViewById(R.id.shipment_button);

        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                boolean formFilled = true;
                boolean quantityTooLarge = false;
                boolean quantityZero = true;

                if (mCarrierEdit.getText().toString().length() == 0)
                    formFilled = false;

                if (mTrackingNumberEdit.getText().toString().length() == 0)
                    formFilled = false;

                for (int i = 0; i < mOrderItemIDList.size(); i++)
                {
                    LinearLayout productLayout = (LinearLayout) mShipmentProductsLayout
                            .getChildAt(i);
                    TextView quantityOrdered = (TextView) productLayout
                            .findViewById(R.id.shipment_quantity_ordered);
                    EditText quantityEdit = (EditText) productLayout
                            .findViewById(R.id.quantity_to_ship);

                    if (quantityEdit.getText().toString().length() == 0)
                    {
                        formFilled = false;
                        break;
                    }

                    double orderedQtyDouble = new Double(quantityOrdered.getText().toString());
                    double toShipQuantityDouble = new Double(quantityEdit.getText().toString());

                    if (toShipQuantityDouble > orderedQtyDouble)
                    {
                        quantityTooLarge = true;
                    }

                    if (toShipQuantityDouble > 0)
                    {
                        quantityZero = false;
                    }
                }

                if (formFilled == false)
                {
                    showFormValidationFailureDialog();
                }
                else if (quantityTooLarge == true)
                {
                    showQTYTooLargeDialog();
                }
                else if (quantityZero == true)
                {
                    showZeroQuantityDialog();
                }
                else if (mCarrierEdit.isEnabled())
                {
                    createShipment();
                }
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mOrderIncrementId = extras.getString(getString(R.string.ekey_order_increment_id));
            mSKU = extras.getString(getString(R.string.ekey_product_sku));
        }

        mLoadOrderAndShipmentJobsTask = new LoadOrderAndShipmentJobs(mOrderIncrementId, mSKU,
                false, this);
        mLoadOrderAndShipmentJobsTask.execute();

        mLoadOrderCarriersTask = new LoadOrderCarriers(this);
        mLoadOrderCarriersTask.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == SCAN_BARCODE) {
            if (resultCode == RESULT_OK) {
                String contents = ScanUtils.getSanitizedScanResult(intent);

                // Set Barcode in Product Barcode TextBox
                mTrackingNumberEdit.setText(contents);

            } else if (resultCode == RESULT_CANCELED) {
                // Do nothing
            }
        }
    }

    public void showZeroQuantityDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Zero QTY error");
        alert.setMessage("Please input QTY > 0 for at least one product.");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }

    public void showFormValidationFailureDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Missing data");
        alert.setMessage("Only \"Comment\" field is optional. The rest is required.");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }

    public void showQTYTooLargeDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Check QTY");
        alert.setMessage("Cannot ship more than purchased.");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }

    public void showOrderDetailsLoadFailureDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Data load problem");
        alert.setMessage("Unable to load order details.");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                OrderShippingActivity.this.finish();
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsActivityAlive = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void createShipment() {
        showProgressDialog("Creating shipment...");
        ShipProduct shipProductTask = new ShipProduct(this);
        shipProductTask.execute();
    }

    public void showProgressDialog(final String message) {
        if (mIsActivityAlive == false) {
            return;
        }
        if (mProgressDialog != null) {
            return;
        }
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(message);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                OrderShippingActivity.this.finish();
            }
        });
        mProgressDialog.show();
    }

    public void dismissProgressDialog() {
        if (mProgressDialog == null) {
            return;
        }
        mProgressDialog.dismiss();
        mProgressDialog = null;
    }

    public void onOrderLoadFailure() {
        mOrderIsLoading = false;
        dismissProgressDialog();
        showOrderDetailsLoadFailureDialog();
    }

    public void onOrderLoadStart() {
        mOrderIsLoading = true;
        showProgressDialog("Loading order details...");
    }

    public void onOrderLoadSuccess() {
        mOrderIsLoading = false;
        dismissProgressDialog();

        mOrderItemIDList = new ArrayList<String>();

        Map<String, Object> orderDetails = mLoadOrderAndShipmentJobsTask.getData().mOrderData;
        List<Job> shipmentJobs = mLoadOrderAndShipmentJobsTask.getData().mShipmentJobs;

        Object[] products = JobCacheManager.getObjectArrayFromDeserializedItem(orderDetails
                .get("items"));
        Object[] shipments = JobCacheManager.getObjectArrayFromDeserializedItem(orderDetails
                .get("shipments"));

        mShipmentProductsLayout.removeAllViews();

        for (Object productObject : products)
        {
            final Map<String, Object> product = (Map<String, Object>) productObject;
            String itemId = (String) product.get("item_id");

            LinearLayout productLayout = (LinearLayout) mInflater.inflate(
                    R.layout.order_shipping_product, null);

            LinkTextView productNameText = (LinkTextView) productLayout
                    .findViewById(R.id.shipment_product_name);
            TextView quantityOrderedText = (TextView) productLayout
                    .findViewById(R.id.shipment_quantity_ordered);
            EditText quantityToShipEditText = (EditText) productLayout
                    .findViewById(R.id.quantity_to_ship);

            productNameText.setTextAndOnClickListener((String) product.get("name"),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent newIntent = new Intent(OrderShippingActivity.this,
                                    ProductDetailsActivity.class);

                            newIntent.putExtra(getString(R.string.ekey_product_sku),
                                    (String) (product.get("sku")));
                            newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                            OrderShippingActivity.this.startActivity(newIntent);
                        }
                    });

            mOrderItemIDList.add(itemId);

            double qty = new Double((String) product.get("qty_ordered"));

            // quantityOrderedText.setText(OrderDetailsActivity.formatQuantity((String)product.get("qty_ordered")));

            /*
             * Try to find if this product was already shipped before and
             * decrease the qty accordingly.
             */
            for (Object shipmentObject : shipments)
            {
                Map<String, Object> shipment = (Map<String, Object>) shipmentObject;

                Object[] shipmentItems = JobCacheManager
                        .getObjectArrayFromDeserializedItem(shipment.get("items"));
                for (Object itemObject : shipmentItems)
                {
                    Map<String, Object> item = (Map<String, Object>) itemObject;

                    String shipmentItemID = (String) item.get("order_item_id");

                    if (shipmentItemID.equals(itemId))
                    {
                        qty -= new Double((String) item.get("qty"));
                    }
                }
            }

            /*
             * Try to find out if there are any pending jobs that are trying to
             * ship this product and if so then decrease the qty accordingly.
             */
            for (Job shipmentJob : shipmentJobs)
            {
                if (shipmentJob.getPending() == true)
                {
                    Map<String, Object> qtysMap = (Map<String, Object>) ((Map<String, Object>) shipmentJob
                            .getExtras().get(EKEY_SHIPMENT_WITH_TRACKING_PARAMS))
                            .get(EKEY_SHIPMENT_ITEMS_QTY);

                    for (String itemIDfromJob : qtysMap.keySet())
                    {
                        if (itemIDfromJob.equals(itemId))
                        {
                            qty -= new Double((String) qtysMap.get(itemIDfromJob));
                        }
                    }
                }
            }

            quantityToShipEditText.setText(OrderDetailsActivity.formatQuantity("" + qty));
            quantityOrderedText.setText(OrderDetailsActivity.formatQuantity("" + qty));
            mShipmentProductsLayout.addView(productLayout);
        }

        if (mFirstOrderDetailsLoad == true)
        {
            mFirstOrderDetailsLoad = false;
            scanTrackingNumber();
        }
    }

    public void onOrderCarriersLoadStart() {
        mOrderCarriersAreLoading = true;

        mCarrierProgress.setVisibility(View.VISIBLE);
        mCarrierEdit.setEnabled(false);
        mButton.setEnabled(false);
    }

    public void onOrderCarriersLoadSuccess() {

        CarriersList carriersList;

        mOrderCarriersAreLoading = false;

        mCarrierProgress.setVisibility(View.GONE);
        mCarrierEdit.setEnabled(true);

        carriersList = mLoadOrderCarriersTask.getData();

        ArrayAdapter<String> carriersAdapter;

        if (carriersList == null)
        {
            carriersAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_dropdown_item_1line, new String[] {});
        }
        else
        {
            carriersAdapter = new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    carriersList.mCarriersList.toArray(new String[carriersList.mCarriersList.size()]));

            if (carriersList.mLastUsedCarrier != null)
            {
                mCarrierEdit.setText(carriersList.mLastUsedCarrier);
            }
        }

        mCarrierEdit.setAdapter(carriersAdapter);

        mButton.setEnabled(true);
    }

    // Methods used by the shipment creation task to get data selected and
    // entered by the user
    public String getOrderIncrementID()
    {
        return mOrderIncrementId;
    }

    public String getCarrierIDField()
    {
        return CUSTOM_VALUE_ID;
    }

    public String getTitleField()
    {
        return mCarrierEdit.getText().toString();
    }

    public String getTrackingNumberField()
    {
        return mTrackingNumberEdit.getText().toString();
    }

    public Map<String, Object> getShipmentWithTrackingParams()
    {
        Map<String, Object> params = new HashMap<String, Object>();

        String comment = mCommentEdit.getText().toString();
        Map<String, Object> qtyMap = new HashMap<String, Object>();

        for (int i = 0; i < mOrderItemIDList.size(); i++)
        {
            LinearLayout productLayout = (LinearLayout) mShipmentProductsLayout.getChildAt(i);
            EditText quantityEdit = (EditText) productLayout.findViewById(R.id.quantity_to_ship);

            qtyMap.put(mOrderItemIDList.get(i), quantityEdit.getText().toString());
        }

        params.put(EKEY_SHIPMENT_ITEMS_QTY, qtyMap);

        if (comment.length() > 0)
        {
            params.put(EKEY_SHIPMENT_COMMENT, comment);
            params.put(EKEY_SHIPMENT_INCLUDE_COMMENT, true);
        }

        return params;
    }

    public String getProductSKU()
    {
        return mSKU;
    }

    public int getProductID()
    {
        Map<String, Object> orderDetails = mLoadOrderAndShipmentJobsTask.getData().mOrderData;
        Map<String, Object> product = (Map<String, Object>) ((JobCacheManager
                .getObjectArrayFromDeserializedItem(orderDetails.get("items")))[0]);

        return new Integer((String) product.get("product_id"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {

            if (mOrderCarriersAreLoading == false && mOrderIsLoading == false)
            {
                mLoadOrderAndShipmentJobsTask = new LoadOrderAndShipmentJobs(mOrderIncrementId,
                        mSKU, true, this);
                mLoadOrderAndShipmentJobsTask.execute();

                mLoadOrderCarriersTask = new LoadOrderCarriers(this);
                mLoadOrderCarriersTask.execute();
            }

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
