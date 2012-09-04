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

	private String [] mStatusList = new String []
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
	
	private String [] mStatusLabelsList = new String []
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
		};
	
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
		
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
				this, R.layout.default_spinner_dropdown, mStatusLabelsList);
			
		mStatusSpinner.setAdapter(arrayAdapter);
		
		mStatusSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				reloadList(false, mStatusList[position]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
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
		mOrderListAdapter = new OrderListAdapter(mLoadOrderListDataTask.getData(), this);
		mListView.setAdapter(mOrderListAdapter);
		
		mSpinningWheelLayout.setVisibility(View.GONE);
		mOrderListLayout.setVisibility(View.VISIBLE);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		if (mLoadOrderListDataTask == null || mLoadOrderListDataTask.getData()==null)
			return;
		
		Intent myIntent = new Intent(this, OrderDetailsActivity.class);
		myIntent.putExtra(getString(R.string.ekey_order_increment_id), (String)((Map<String, Object>)mLoadOrderListDataTask.getData()[position]).get("increment_id"));
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
