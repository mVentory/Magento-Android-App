package com.mageventory.activity;

import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.R.id;
import com.mageventory.R.layout;
import com.mageventory.R.string;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.tasks.LoadOrderListData;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class OrderListActivity extends BaseActivity implements OnItemClickListener {

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
			orderDate.setText("" + ((Map<String, Object>)mData[position]).get("created_at"));
			
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
	
	private ListView mListView;
	private LinearLayout mSpinningWheelLayout;
	private LinearLayout mOrderListLayout;
	private LoadOrderListData mLoadOrderListDataTask;
	private OrderListAdapter mOrderListAdapter;
	private Spinner mStatusSpinner;

	private String [] mStatusList; /* = new String []
		{
			"",
			"pending",
			"pending_payment",
			"processing",
			"holded",
			"complete",
			"closed",
			"canceled",
			"fraud",
			"payment_review"
		};
		*/
	
	private String [] mStatusLabelsList;/* = new String []
		{
			"Latest",
			"Pending",
			"Pending Payment",
			"Processing",
			"On Hold",
			"Complete",
			"Closed",
			"Canceled",
			"Suspected Fraud",
			"Payment Review"
		};*/
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.order_list_activity);
		
		this.setTitle("Mventory: Order list");
		
		mListView = (ListView) findViewById(R.id.order_listview);
		mListView.setOnItemClickListener(this);
		mSpinningWheelLayout = (LinearLayout) findViewById(R.id.spinning_wheel);
		mOrderListLayout = (LinearLayout) findViewById(R.id.orderlist_layout);
		mStatusSpinner = ((Spinner) findViewById(R.id.status_spinner)); 
		
		reloadList(false, "");
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

	public void onOrderListLoadSuccess() {
		
		/* We want to set the adapter to the statuses spinner just once. */
		if (mStatusLabelsList == null)
		{
			mStatusList = ((Map<String, Object>)mLoadOrderListDataTask.getData().get("statuses")).keySet().toArray(new String [0]);
			mStatusLabelsList = ((Map<String, Object>)mLoadOrderListDataTask.getData().get("statuses")).values().toArray(new String [0]);
			
			/* Add special elements to the beginning of both lists. */
		    ArrayList<String> statusListTmp = new ArrayList<String>(Arrays.asList(mStatusList));
			statusListTmp.add(0, "");
			mStatusList = statusListTmp.toArray(new String [0]);
			
			ArrayList<String> statusLabelsListTmp = new ArrayList<String>(Arrays.asList(mStatusLabelsList));
			statusLabelsListTmp.add(0, "Latest");
			mStatusLabelsList = statusLabelsListTmp.toArray(new String [0]);
			
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
					this, R.layout.default_spinner_dropdown, mStatusLabelsList);
			
			mStatusSpinner.setOnItemSelectedListener(null);
			mStatusSpinner.setAdapter(arrayAdapter);
			mStatusSpinner.setOnItemSelectedListener(
					new OnItemSelectedListener() {
						boolean mFirstSelect = true;
				
						@Override
						public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
							if (mFirstSelect == true)
							{
								/* First time this function is called is never because user selected anything so ignore the first call. */
								mFirstSelect = false;
							}
							else
							{
								reloadList(false, mStatusList[position]);	
							}
						}
				
						@Override
						public void onNothingSelected(AdapterView<?> parent) {
						}
					}
				);
		}
		
		mOrderListAdapter = new OrderListAdapter((Object [])mLoadOrderListDataTask.getData().get("orders"), this);
		mListView.setAdapter(mOrderListAdapter);
		
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
			mLoadOrderListDataTask = new LoadOrderListData(this, status, refresh);
			mLoadOrderListDataTask.execute();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			
			reloadList(true, mStatusList[mStatusSpinner.getSelectedItemPosition()]);
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
