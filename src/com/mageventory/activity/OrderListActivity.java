package com.mageventory.activity;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.R.id;
import com.mageventory.R.layout;
import com.mageventory.R.string;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.components.LinkTextView;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.OrderStatus;
import com.mageventory.resprocessor.CartItemsProcessor;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
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
import android.widget.TextView.OnEditorActionListener;

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
	
	/* We need to keep user entered data between refreshes */
	private Object [] mShoppingCartItemsBeforeRefresh;
	
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
				
				showConfirmationDialog(count, OrderDetailsActivity.formatPrice("" + total));
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
	
	public void showConfirmationDialog(int prodCount, String price) {
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
		alert.setTitle("Confirmation");
		alert.setMessage("Sell " + prodCount + " products for " + price + "?");
			
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				sellNow();
			}
		});
		
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
			
		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	public void showInvalidValuesDialog() {
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
		alert.setTitle("Error");
		alert.setMessage("Missing or invalid values. Check the form.");
			
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	private boolean validateForm()
	{
		boolean res = true;
		
		for(int i=0; i<mCartListLayout.getChildCount(); i++)
		{
			LinearLayout layout = (LinearLayout) mCartListLayout.getChildAt(i);
			CheckBox checkBox = (CheckBox)layout.findViewById(R.id.product_checkbox);
			EditText priceEdit = (EditText)layout.findViewById(R.id.price_edit);
			EditText qtyEdit = (EditText)layout.findViewById(R.id.qty_edit);
			
			if (checkBox.isChecked())
			{
				try
				{
					Double.parseDouble(priceEdit.getText().toString());
					Double.parseDouble(qtyEdit.getText().toString());
				}
				catch(NumberFormatException e)
				{
					res = false;
				}
			}
		}
		return res;
	}
	
	private void sellNow()
	{
		if (validateForm() == false)
		{
			showInvalidValuesDialog();
		}

		ArrayList<Object> productsToSellJobExtras = new ArrayList<Object>();
		ArrayList<Object> productsToSellAllData = new ArrayList<Object>();
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
				
				productsToSellJobExtras.add(productToSell);
				productsToSellAllData.add(items[i]);
			}
		}
		
		CreateNewOrderForMultipleProds createTask
			= new CreateNewOrderForMultipleProds(this, productsToSellJobExtras.toArray(new Object[0]), productsToSellAllData.toArray(new Object[0]), sku, productID);
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
		
		if (count == 0)
		{
			mSellNowButton.setEnabled(false);
		}
		else
		{
			mSellNowButton.setEnabled(true);
		}
		
		mShippingCartFooterText.setText("Total " + OrderDetailsActivity.formatPrice("" + total) + " for " + count + " products.");
	}
	
	private void updateTotal(EditText priceEdit, EditText qtyEdit, EditText totalEdit)
	{
		double price = 0;
		double qty = 0;
		
		try
		{
			price = new Double(priceEdit.getText().toString());
			qty = new Double(qtyEdit.getText().toString());
		}
		catch(NumberFormatException e)
		{}
		
		totalEdit.setText(OrderDetailsActivity.formatPrice("" + price * qty).replace("$", ""));
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
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			Object [] items = (Object [])mLoadOrderListDataTask.getData().get(LoadOrderListData.CART_ITEMS_KEY);
			
			ArrayList<Boolean> lastCheckboxesState = null;
			ArrayList<String> lastPriceEditState = null;
			ArrayList<String> lastQtyEditState = null;
			
			if (mShoppingCartItemsBeforeRefresh != null)
			{
				lastCheckboxesState = new ArrayList<Boolean>();
				lastPriceEditState = new ArrayList<String>();
				lastQtyEditState = new ArrayList<String>();
				
				for(int i=0; i<items.length; i++)
				{
					LinearLayout layout = (LinearLayout)mCartListLayout.getChildAt(i); 
					CheckBox checkBox = (CheckBox)layout.findViewById(R.id.product_checkbox);
					EditText priceEdit = (EditText)layout.findViewById(R.id.price_edit);
					EditText qtyEdit = (EditText)layout.findViewById(R.id.qty_edit);
					
					lastCheckboxesState.add(checkBox.isChecked());
					lastPriceEditState.add(priceEdit.getText().toString());
					lastQtyEditState.add(qtyEdit.getText().toString());
				}
			}
			
			mCartListLayout.removeAllViews();

			for(int i=0; i<items.length; i++)
			{
				LinearLayout layout = (LinearLayout)inflater.inflate(R.layout.cart_item, null); 
				LinkTextView productName = (LinkTextView)layout.findViewById(R.id.product_name);
				final EditText priceEdit = (EditText)layout.findViewById(R.id.price_edit);
				final EditText qtyEdit = (EditText)layout.findViewById(R.id.qty_edit);
				final EditText totalEdit = (EditText)layout.findViewById(R.id.total_edit);
				
				CheckBox checkBox = (CheckBox)layout.findViewById(R.id.product_checkbox);
				
				final int index = i;
				TextWatcher totalUpdater = new TextWatcher() {
					
					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						updateTotal(priceEdit, qtyEdit, totalEdit);
						refreshShippingCartFooterText();
					}
					
					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
						
					}
					
					@Override
					public void afterTextChanged(Editable s) {
						
					}
				};
				
				priceEdit.addTextChangedListener(totalUpdater);
				qtyEdit.addTextChangedListener(totalUpdater);
				
				checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						refreshShippingCartFooterText();
						
						LinearLayout layout = (LinearLayout) mCartListLayout.getChildAt(index);
						LinearLayout editInputs = (LinearLayout)layout.findViewById(R.id.edit_inputs);
						
						if (isChecked)
						{
							editInputs.setVisibility(View.VISIBLE);
						}
						else
						{
							editInputs.setVisibility(View.GONE);
						}
					}
				});
				
				String total = OrderDetailsActivity.formatPrice((String)((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_TOTAL));
				String price = OrderDetailsActivity.formatPrice((String)((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_PRICE));
				String quantity = OrderDetailsActivity.formatQuantity((String)((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_QUANTITY));
				final String sku = (String)((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_SKU);
						
				productName.setTextAndOnClickListener(
					"" + ((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_NAME2) + ", "
					+ quantity + "/" + total,
					new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent newIntent = new Intent(OrderListActivity.this, ProductDetailsActivity.class);
							
							newIntent.putExtra(getString(R.string.ekey_product_sku), sku);
							newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							
							OrderListActivity.this.startActivity(newIntent);
						}
					});
				
				priceEdit.setText(price.replace("$", ""));
				qtyEdit.setText(quantity);
				totalEdit.setText(total.replace("$", ""));
			
				mCartListLayout.addView(layout);
				
				if (mShoppingCartItemsBeforeRefresh != null)
				{
					for(int j=0; j<mShoppingCartItemsBeforeRefresh.length; j++)
					{
						String oldTransID = (String)((Map<String, Object>)mShoppingCartItemsBeforeRefresh[j]).get(MAGEKEY_PRODUCT_TRANSACTION_ID);
						String newTransID = (String)((Map<String, Object>)items[i]).get(MAGEKEY_PRODUCT_TRANSACTION_ID);
						
						if ( oldTransID.equals(newTransID) )
						{
							if (lastCheckboxesState.get(j) == true)
							{
								checkBox.setChecked(true);
							}
							
							priceEdit.setText(lastPriceEditState.get(j));
							qtyEdit.setText(lastQtyEditState.get(j));
						}
					}
				}
			}
			
			if (items.length == 0)
			{
				mSellNowButton.setEnabled(false);
			}
			else
			{
				mSellNowButton.setEnabled(true);
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
			
			if (mLoadOrderListDataTask!=null && mLoadOrderListDataTask.getStatusParam().equals(OrdersListByStatusProcessor.SHOPPING_CART_STATUS_CODE) &&
				status.equals(OrdersListByStatusProcessor.SHOPPING_CART_STATUS_CODE))
			{
				mShoppingCartItemsBeforeRefresh = (Object [])mLoadOrderListDataTask.getData().get(LoadOrderListData.CART_ITEMS_KEY); 
			}
			else
			{
				mShoppingCartItemsBeforeRefresh = null;
			}
			
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
