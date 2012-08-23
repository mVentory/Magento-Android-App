package com.mageventory;

import com.mageventory.tasks.LoadOrderListData;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

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
	private LinearLayout mSpinningWheel;
	private LoadOrderListData mLoadOrderListDataTask;
	private OrderListAdapter mOrderListAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.order_list_activity);
		
		this.setTitle("Mventory: Order list (pending orders)");
		
		mListView = (ListView) findViewById(R.id.order_listview);
		mListView.setOnItemClickListener(this);
		mSpinningWheel = (LinearLayout) findViewById(R.id.spinning_wheel);
		
		mLoadOrderListDataTask = new LoadOrderListData(this);
		mLoadOrderListDataTask.execute();
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

	}

	public void onOrderListLoadFailure() {
		showFailureDialog();
	}

	public void onOrderListLoadSuccess() {
		mOrderListAdapter = new OrderListAdapter(mLoadOrderListDataTask.getData(), this);
		mListView.setAdapter(mOrderListAdapter);
		
		mSpinningWheel.setVisibility(View.GONE);
		mListView.setVisibility(View.VISIBLE);
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		//Intent myIntent = new Intent(this, OrderDetailsActivity.class);
		//startActivity(myIntent);
	}

}
