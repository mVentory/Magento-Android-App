package com.mageventory.activity;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.R.id;
import com.mageventory.R.layout;
import com.mageventory.R.string;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.OrderStatus;
import com.mageventory.resprocessor.OrdersListByStatusProcessor;
import com.mageventory.settings.Settings;
import com.mageventory.tasks.CreateNewOrderForMultipleProds;
import com.mageventory.tasks.CreateNewProduct;
import com.mageventory.tasks.LoadOrderListData;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderListActivity extends BaseActivity implements OnItemClickListener, MageventoryConstants {
	
	private static class OrderListAdapter extends BaseAdapter {

		private Object [] mData;
		private final LayoutInflater mInflater;
		
		public OrderListAdapter(Object [] data, Context context)
		{
			mData = data;
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		@Override
		public int getCount() {
			return mData.length;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			View out;
			
			if (convertView == null) {
				out = mInflater.inflate(R.layout.order_list_item, null); 
			}
			else
			{
				out = convertView;
			}
			
			TextView orderNumber = (TextView)out.findViewById(R.id.order_number);
			TextView orderDate = (TextView)out.findViewById(R.id.order_date);
			LinearLayout productList = (LinearLayout)out.findViewById(R.id.productList);
			
			orderNumber.setText("" + ((Map<String, Object>)mData[position]).get("increment_id"));
			orderDate.setText(OrderDetailsActivity.removeSeconds("" + ((Map<String, Object>)mData[position]).get("created_at")));
			
			Object [] items = (Object [])((Map<String, Object>)mData[position]).get("items");
			
			productList.removeAllViews();
			
			for(int i=0; i<items.length; i++)
			{
				String productName = (String)((Map<String, Object>)items[i]).get("name");
				TextView productTextView = (TextView) mInflater.inflate(R.layout.order_list_product, null);
				productTextView.setText(productName);
				
				productList.addView(productTextView);
			}
			
			return out;
		}
	}
	
	public Settings mSettings;
	
	private ListView mListView;
	private LinearLayout mCartListLayout;
	private ScrollView mCartListScrollView;
	
	private LinearLayout mSpinningWheelLayout;
	private LinearLayout mOrderListLayout;
	private LoadOrderListData mLoadOrderListDataTask;
	private BaseAdapter mOrderListAdapter;
	private Spinner mStatusSpinner;
	private String mDefaultStatusCode;

	private ArrayList<OrderStatus> mStatusList;
	
	private LinearLayout mShippingCartFooter;
	private TextView mShippingCartFooterText;
	private Button mSellNowButton;
	
	protected boolean isActivityAlive;
	private ProgressDialog progressDialog;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSettings = new Settings(this);
		
		setContentView(R.layout.order_list_activity);
		
		this.setTitle("mVentory: Order list");
		
		mCartListLayout = (LinearLayout) findViewById(R.id.cart_list);
		
		mListView = (ListView) findViewById(R.id.order_listview);
		mListView.setOnItemClickListener(this);
		mSpinningWheelLayout = (LinearLayout) findViewById(R.id.spinning_wheel);
		mOrderListLayout = (LinearLayout) findViewById(R.id.orderlist_layout);
		mStatusSpinner = ((Spinner) findViewById(R.id.status_spinner)); 
		mShippingCartFooter = (LinearLayout) findViewById(R.id.shipping_cart_footer);
		mCartListScrollView = (ScrollView) findViewById(R.id.cart_list_scrollview);
		mShippingCartFooterText = (TextView) findViewById(R.id.shipping_cart_footer_text);
		mSellNowButton = (Button) findViewById(R.id.sell_now_button);
		
		mSellNowButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				sellNow();
			}
		});
		
		reloadList(false, getDefaultStatusCode());
		
		isActivityAlive = true;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		isActivityAlive = false;
	}

	private void sellNow()
	{
		ArrayList<Object> productsToSell = new ArrayList<Object>();
		Object [] items = (Object [])mLoadOrderListDataTask.getData().get(LoadOrderListData.CART_ITEMS_KEY);
		
		String sku = null;
		int productID = 0;
		
		showProgressDialog("Creating an order...");
		
		for(int i=0; i<mCartListLayout.getChildCount(); i++)
		{
			LinearLayout layout = (LinearLayout) mCartListLayout.getChildAt(i);
			CheckBox checkBox = (CheckBox)layout.findViewById(R.id.product_checkbox);
			EditText priceEdit = (EditText)layout.findViewById(R.id.price_edit);
			EditText qtyEdit = (EditText)layout.findViewById(R.id.qty_edit);

			if (checkBox.isChecked())
			{
				Map<String, Object> productToSell = new HashMap<String, Object>();
				
				if (sku==null)
				{
					sku = (String)((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_SKU);
					productID = new Integer((String)((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_ID));
				}
				
				productToSell.put(MAGEKEY_PRODUCT_TRANSACTION_ID, (String)((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_TRANSACTION_ID));
				productToSell.put(MAGEKEY_PRODUCT_PRICE, Double.parseDouble(priceEdit.getText().toString()));
				productToSell.put(MAGEKEY_PRODUCT_QUANTITY, Double.parseDouble(qtyEdit.getText().toString()));
				
				productsToSell.add(productToSell);
			}
		}
		
		CreateNewOrderForMultipleProds createTask = new CreateNewOrderForMultipleProds(this, productsToSell.toArray(new Object[0]), sku, productID);
		createTask.execute();
	}
	
	public void showProgressDialog(final String message) {
		if (isActivityAlive == false) {
			return;
		}
		if (progressDialog != null) {
			return;
		}
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(message);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.show();
	}

	public void dismissProgressDialog() {
		if (progressDialog == null) {
			return;
		}
		progressDialog.dismiss();
		progressDialog = null;
	}
	
	/* Shows a dialog for adding new option. */
	public void showFailureDialog() {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Load failed");
		alert.setMessage("Unable to load order list.");

		alert.setPositiveButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				Intent myIntent = new Intent(OrderListActivity.this.getApplicationContext(), OrderListActivity.class);
				OrderListActivity.this.startActivity(myIntent);
				OrderListActivity.this.finish();
			}
		});

		alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				OrderListActivity.this.finish();
			}
		});
	
		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	public void onOrderListLoadStart() {
		mSpinningWheelLayout.setVisibility(View.VISIBLE);
		mOrderListLayout.setVisibility(View.GONE);
	}

	public void onOrderListLoadFailure() {
		showFailureDialog();
	}

	private String [] getArrayOfStatusLabels()
	{
		String [] statusLabels = new String[mStatusList.size()];
		
		for (int i=0; i<mStatusList.size(); i++)
		{
			statusLabels[i] = mStatusList.get(i).mStatusLabel;
		}
		
		return statusLabels;
	}
	
	private String getDefaultStatusCode()
	{
		if (mDefaultStatusCode == null)
		{
			Object [] cartItems = JobCacheManager.restoreCartItems(mSettings.getUrl());
			
			if (cartItems != null && cartItems.length > 0)
			{
				mDefaultStatusCode = OrdersListByStatusProcessor.SHOPPING_CART_STATUS_CODE;
			}
			else
			{
				mDefaultStatusCode = OrdersListByStatusProcessor.LATEST_STATUS_CODE;
			}
		}
		
		return mDefaultStatusCode;
	}
	
	private int getDefaultSelectionIndex()
	{
		for (int i=0; i<mStatusList.size(); i++)
		{
			if (mStatusList.get(i).mStatusCode.equals(getDefaultStatusCode()))
				return i;
		}
		
		return 0;
	}
	
	public void refreshShippingCartFooterText()
	{
		double total = 0;
		int count = 0;
		
		for(int i=0; i<mCartListLayout.getChildCount(); i++)
		{
			LinearLayout layout = (LinearLayout) mCartListLayout.getChildAt(i);
			CheckBox checkBox = (CheckBox)layout.findViewById(R.id.product_checkbox);
			EditText totalEdit = (EditText)layout.findViewById(R.id.total_edit);

			if (checkBox.isChecked())
			{
				count ++;
				try
				{
					total += Double.parseDouble(totalEdit.getText().toString() );
				}
				catch(NumberFormatException e)
				{
				}
			}
		}
		
		mShippingCartFooterText.setText("Total $" + total + " for " + count + " products.");
	}
	
	public void onOrderListLoadSuccess() {
		
		/* We want to set the adapter to the statuses spinner just once. */
		if (mStatusList == null)
		{
			mStatusList = (ArrayList<OrderStatus>)(mLoadOrderListDataTask.getData().get("statuses"));
			
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
					this, R.layout.default_spinner_dropdown, getArrayOfStatusLabels());
			
			mStatusSpinner.setOnItemSelectedListener(null);
			mStatusSpinner.setAdapter(arrayAdapter);
			
			mStatusSpinner.setSelection(getDefaultSelectionIndex());
			
			mStatusSpinner.setOnItemSelectedListener(
					new OnItemSelectedListener() {
						boolean mFirstSelect = true;
				
						@Override
						public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
							if (mFirstSelect == true)
							{
								/* First time this function is called it is done by Android and the user didn't select anything so ignore the
								 * first call. */
								mFirstSelect = false;
							}
							else
							{
								reloadList(false, mStatusList.get(position).mStatusCode);	
							}
						}
				
						@Override
						public void onNothingSelected(AdapterView<?> parent) {
						}
					}
				);
		}
		
		if (mLoadOrderListDataTask.getStatusParam().equals(OrdersListByStatusProcessor.SHOPPING_CART_STATUS_CODE))
		{
			
			Object [] items = (Object [])mLoadOrderListDataTask.getData().get(LoadOrderListData.CART_ITEMS_KEY);
			
			mCartListLayout.removeAllViews();
			
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			double total = 0;
			
			for(int i=0; i<items.length; i++)
			{
				LinearLayout layout = (LinearLayout)inflater.inflate(R.layout.cart_item, null); 
				
				TextView productName = (TextView)layout.findViewById(R.id.product_name);
				
				TextView priceText = (TextView)layout.findViewById(R.id.price_text);
				EditText priceEdit = (EditText)layout.findViewById(R.id.price_edit);
				
				TextView qtyText = (TextView)layout.findViewById(R.id.qty_text);
				EditText qtyEdit = (EditText)layout.findViewById(R.id.qty_edit);
				
				TextView totalText = (TextView)layout.findViewById(R.id.total_text);
				EditText totalEdit = (EditText)layout.findViewById(R.id.total_edit);
				
				CheckBox checkBox = (CheckBox)layout.findViewById(R.id.product_checkbox);
				checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						refreshShippingCartFooterText();
					}
				});
				
				productName.setText("" + ((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_NAME2));
				
				priceText.setText("" + ((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_PRICE));
				priceEdit.setText("" + ((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_PRICE));
				
				qtyText.setText("" + ((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_QUANTITY));
				qtyEdit.setText("" + ((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_QUANTITY));
				
				totalText.setText("" + ((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_TOTAL));
				totalEdit.setText("" + ((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_TOTAL));
			
				mCartListLayout.addView(layout);
			}
			
			refreshShippingCartFooterText();
			
			mShippingCartFooter.setVisibility(View.VISIBLE);
			
			mCartListScrollView.setVisibility(View.VISIBLE);
			mListView.setVisibility(View.GONE);
		}
		else
		{
			mOrderListAdapter = new OrderListAdapter((Object [])mLoadOrderListDataTask.getData().get("orders"), this);
			mListView.setAdapter(mOrderListAdapter);
			mShippingCartFooter.setVisibility(View.GONE);
			
			mCartListScrollView.setVisibility(View.GONE);
			mListView.setVisibility(View.VISIBLE);
		}
		
		mSpinningWheelLayout.setVisibility(View.GONE);
		mOrderListLayout.setVisibility(View.VISIBLE);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		if (mLoadOrderListDataTask == null || mLoadOrderListDataTask.getData()==null)
			return;
		
		Intent myIntent = new Intent(this, OrderDetailsActivity.class);
		myIntent.putExtra(getString(R.string.ekey_order_increment_id), (String)((Map<String, Object>)((Object [])mLoadOrderListDataTask.getData().get("orders"))[position]).get("increment_id"));
		myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		//myIntent.putExtra(getString(R.string.ekey_order_increment_id), "100000364");
		startActivity(myIntent);
	}

	private void reloadList(boolean refresh, String status)
	{
		/* If the spinning wheel is gone we can be sure no other load task is pending so we can start another one. */
		if (mSpinningWheelLayout.getVisibility() == View.GONE)
		{
			mLoadOrderListDataTask = new LoadOrderListData(this, status, refresh, mStatusList == null);
			mLoadOrderListDataTask.execute();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			
			reloadList(true, mStatusList.get(mStatusSpinner.getSelectedItemPosition()).mStatusCode);
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
