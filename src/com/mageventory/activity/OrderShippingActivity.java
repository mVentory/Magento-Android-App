package com.mageventory.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCallback;
import com.mageventory.job.JobControlInterface;
import com.mageventory.settings.Settings;
import com.mageventory.tasks.LoadOrder;
import com.mageventory.tasks.LoadOrderCarriers;
import com.mageventory.tasks.LoadOrderDetailsData;
import com.mageventory.tasks.ShipProduct;
import com.mageventory.tasks.UpdateProduct;

public class OrderShippingActivity extends BaseActivity implements MageventoryConstants {

	private static class Carrier
	{
		String id;
		String label;
	}
	
	private static final String CUSTOM_VALUE_ID = "custom";
	
	private LayoutInflater mInflater;

	private LoadOrder mLoadOrderTask = null;
	private LoadOrderCarriers mLoadOrderCarriersTask = null;
	
	private boolean mIsActivityAlive;
	private ProgressDialog mProgressDialog;
	private String mOrderIncrementId;
	private String mSKU;
	private ProgressBar mCarrierProgress;
	private Spinner mCarrierSpinner;
	private EditText mTitleEdit;
	private EditText mTrackingNumberEdit;
	private EditText mCommentEdit;
	private TextView mCarrierText;
	private LinearLayout mShipmentProductsLayout;
	private Button mButton;
	
	private ArrayList<Carrier> mCarriersList;
	private ArrayList<String> mOrderItemIDList;
	
	private boolean mOrderIsLoading = false;
	private boolean mOrderCarriersAreLoading = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.order_shipping_activity);
		mIsActivityAlive = true;
		
		mInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mCarrierProgress = (ProgressBar) findViewById(R.id.carrier_progress);
		mCarrierSpinner = (Spinner) findViewById(R.id.carrier_spinner);
		mTitleEdit = (EditText) findViewById(R.id.title_edit);
		mTrackingNumberEdit = (EditText) findViewById(R.id.tracking_number_edit);
		mCommentEdit = (EditText) findViewById(R.id.comment_edit);
		mCarrierText = (TextView) findViewById(R.id.carrier_text);
		mShipmentProductsLayout = (LinearLayout) findViewById(R.id.shipment_products);
		mButton = (Button) findViewById(R.id.shipment_button);
		
		mButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				boolean formFilled = true;
				
				if (mTitleEdit.getText().toString().length() == 0)
					formFilled = false;
				
				if (mTrackingNumberEdit.getText().toString().length() == 0)
					formFilled = false;
				
				for (int i=0; i<mOrderItemIDList.size(); i++)
				{
					LinearLayout productLayout = (LinearLayout)mShipmentProductsLayout.getChildAt(i);
					EditText quantityEdit = (EditText)productLayout.findViewById(R.id.quantity_to_ship);
					
					if (quantityEdit.getText().toString().length() == 0)
					{
						formFilled = false;
					}
				}
				
				if (formFilled == false)
				{
					showFormValidationFailureDialog();
				}
				else if (mCarrierSpinner.isEnabled())
				{
					createShipment();	
				}
			}
		});
		
		mCarrierSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (mCarriersList.get(position).id.equals(CUSTOM_VALUE_ID))
				{
					mTitleEdit.setText("");
				}
				else
				{
					mTitleEdit.setText((String) mCarrierSpinner.getAdapter().getItem(position));
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		});

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mOrderIncrementId = extras.getString(getString(R.string.ekey_order_increment_id));
			mSKU = extras.getString(getString(R.string.ekey_product_sku));
		}
		
		mLoadOrderTask = new LoadOrder(mOrderIncrementId, false, this);
		mLoadOrderTask.execute();
		
		mLoadOrderCarriersTask = new LoadOrderCarriers(mOrderIncrementId, false, this);
		mLoadOrderCarriersTask.execute();
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
		
		Map<String, Object> orderDetails = mLoadOrderTask.getData();
		Object [] products = (Object []) orderDetails.get("items");
		
		mShipmentProductsLayout.removeAllViews();
		
		for(Object productObject : products)
		{
			Map<String, Object> product = (Map<String, Object>) productObject;
		
			LinearLayout productLayout = (LinearLayout)mInflater.inflate(R.layout.order_shipping_product, null);
			
			TextView productNameText = (TextView) productLayout.findViewById(R.id.shipment_product_name);
			TextView quantityOrderedText = (TextView) productLayout.findViewById(R.id.shipment_quantity_ordered);
			
			productNameText.setText((String)product.get("name"));
			mOrderItemIDList.add((String)product.get("item_id"));
			quantityOrderedText.setText(OrderDetailsActivity.formatQuantity((String)product.get("qty_ordered")));
			
			mShipmentProductsLayout.addView(productLayout);
		}
	}
	
	public void onOrderCarriersLoadStart() {
		mOrderCarriersAreLoading = true;

		mCarrierProgress.setVisibility(View.VISIBLE);
		mCarrierSpinner.setEnabled(false);
		mCarrierText.setTextColor(Color.WHITE);
		mButton.setEnabled(false);
	}
	
	public void onOrderCarriersLoadSuccess() {
		mOrderCarriersAreLoading = false;
		
		mCarrierProgress.setVisibility(View.GONE);
		mCarrierSpinner.setEnabled(true);
		
		Map<String, Object> carriersMap = mLoadOrderCarriersTask.getData();
		
		mCarriersList = new ArrayList<Carrier>();
		
		for(String key : carriersMap.keySet())
		{
			Carrier c = new Carrier();
			c.id = key;
			c.label = (String)carriersMap.get(key);
			mCarriersList.add(c);
		}
		
		Collections.sort(mCarriersList, new Comparator<Carrier>(){

			@Override
			public int compare(Carrier lhs, Carrier rhs) {
				return lhs.label.compareTo(rhs.label);
			}
			
		});

		String [] adapterArray = new String[mCarriersList.size()];
		
		for(int i=0; i<adapterArray.length; i++)
		{
			adapterArray[i] = mCarriersList.get(i).label;
		}
		
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
				this, R.layout.default_spinner_dropdown, adapterArray);
		
		mCarrierSpinner.setAdapter(arrayAdapter);
		
		mButton.setEnabled(true);
	}
	
	public void onOrderCarriersLoadFailure() {
		mOrderCarriersAreLoading = false;
		
		mCarrierText.setTextColor(Color.RED);
		mCarrierProgress.setVisibility(View.GONE);
	}

	// Methods used by the shipment creation task to get data selected and entered by the user
	public String getOrderIncrementID()
	{
		return mOrderIncrementId;
	}
	
	public String getCarrierIDField()
	{
		return mCarriersList.get(mCarrierSpinner.getSelectedItemPosition()).id;
	}
	
	public String getTitleField()
	{
		return mTitleEdit.getText().toString();
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

		for (int i=0; i<mOrderItemIDList.size(); i++)
		{
			LinearLayout productLayout = (LinearLayout)mShipmentProductsLayout.getChildAt(i);
			EditText quantityEdit = (EditText)productLayout.findViewById(R.id.quantity_to_ship);
			
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
		Map<String, Object> orderDetails = mLoadOrderTask.getData();
		Map<String, Object> product = (Map<String, Object>)(((Object []) orderDetails.get("items"))[0]);
		
		return new Integer((String)product.get("product_id"));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			
			if (mOrderCarriersAreLoading == false && mOrderIsLoading == false)
			{
				mLoadOrderTask = new LoadOrder(mOrderIncrementId, true, this);
				mLoadOrderTask.execute();
				
				mLoadOrderCarriersTask = new LoadOrderCarriers(mOrderIncrementId, true, this);
				mLoadOrderCarriersTask.execute();
			}
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
