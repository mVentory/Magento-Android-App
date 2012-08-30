package com.mageventory.activity;

import java.util.Map;

import com.mageventory.R;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.settings.Settings;
import com.mageventory.tasks.LoadOrderDetailsData;
import com.mageventory.tasks.LoadOrderListData;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

public class OrderDetailsActivity extends BaseActivity {

	private ScrollView mOrderDetailsLayout;
	private LinearLayout mSpinningWheel;
	
	private LoadOrderDetailsData mLoadOrderDetailsDataTask;
	private TextView mOrderNumText;
	private TextView mDateTimeText;
	private TextView mStatusText;
	private TextView mCustomerIDText;
	private LinearLayout mShippingAddressLayout;
	private LinearLayout mBillingAddressLayout;
	private LinearLayout mItemsOrderedLayout;
	private LinearLayout mPaymentDetailsLayout;
	private LayoutInflater mInflater;
	private String mOrderIncrementId;
	private Settings mSettings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.order_details_activity);
		
		this.setTitle("Mventory: Order details");
		
		mOrderDetailsLayout = (ScrollView) findViewById(R.id.order_details_layout);
		mSpinningWheel = (LinearLayout) findViewById(R.id.spinning_wheel);
		
		mOrderNumText = (TextView) findViewById(R.id.order_num_text);
		mDateTimeText = (TextView) findViewById(R.id.datetime_text);
		mStatusText = (TextView) findViewById(R.id.status_text);
		mCustomerIDText = (TextView) findViewById(R.id.customer_id_text);
		
		mShippingAddressLayout = (LinearLayout) findViewById(R.id.shipping_address_layout);
		mBillingAddressLayout = (LinearLayout) findViewById(R.id.billing_address_layout);
		mItemsOrderedLayout = (LinearLayout) findViewById(R.id.items_ordered_layout);
		mPaymentDetailsLayout = (LinearLayout) findViewById(R.id.payment_details_layout);
		
		mInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mOrderIncrementId = extras.getString(getString(R.string.ekey_order_increment_id));
		}
		
		mLoadOrderDetailsDataTask = new LoadOrderDetailsData(this, mOrderIncrementId, false);
		mLoadOrderDetailsDataTask.execute();
		
		mSettings = new Settings(this);
	}

	/* Shows a dialog for adding new option. */
	public void showFailureDialog() {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Load failed");
		alert.setMessage("Unable to load order details.");

		alert.setPositiveButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				Intent myIntent = new Intent(OrderDetailsActivity.this.getApplicationContext(), OrderDetailsActivity.class);
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
	}

	public void onOrderDetailsFailure() {
		showFailureDialog();
	}

	public void onOrderDetailsSuccess() {
		fillTextViewsWithData();
		mSpinningWheel.setVisibility(View.GONE);
		mOrderDetailsLayout.setVisibility(View.VISIBLE);
	}
	
	private void fillTextViewsWithData()
	{
		if (mLoadOrderDetailsDataTask == null || mLoadOrderDetailsDataTask.getData() == null)
			return;
		
		String orderLink = mSettings.getUrl() + "/index.php/admin/sales_order/view/order_id/" + (String)mLoadOrderDetailsDataTask.getData().get("order_id");
		String customerLink = mSettings.getUrl() + "/index.php/admin/customer/edit/id/" + (String)mLoadOrderDetailsDataTask.getData().get("customer_id");
		
		mOrderNumText.setText(Html.fromHtml("<a href=\"" + orderLink + "\">" + (String)mLoadOrderDetailsDataTask.getData().get("increment_id") + "</a>"));
		mOrderNumText.setMovementMethod(LinkMovementMethod.getInstance());
		
		mDateTimeText.setText((String)mLoadOrderDetailsDataTask.getData().get("created_at"));
		
		mStatusText.setText((String)mLoadOrderDetailsDataTask.getData().get("status"));

		mCustomerIDText.setText(Html.fromHtml("<a href=\"" + customerLink + "\">" + (String)mLoadOrderDetailsDataTask.getData().get("customer_id") + "</a>"));
		mCustomerIDText.setMovementMethod(LinkMovementMethod.getInstance());
		
		mShippingAddressLayout.removeAllViews();
		for (String key : ((Map<String, Object>)mLoadOrderDetailsDataTask.getData().get("shipping_address")).keySet())
		{
			LinearLayout subitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
			
			TextView text1 = (TextView)subitem.findViewById(R.id.text1);
			TextView text2 = (TextView)subitem.findViewById(R.id.text2);
			
			text1.setText(key + ": ");
			text2.setText((String)((Map<String, Object>)mLoadOrderDetailsDataTask.getData().get("shipping_address")).get(key));
			mShippingAddressLayout.addView(subitem);
		}
		
		mBillingAddressLayout.removeAllViews();
		for (String key : ((Map<String, Object>)mLoadOrderDetailsDataTask.getData().get("billing_address")).keySet())
		{
			LinearLayout subitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
			
			TextView text1 = (TextView)subitem.findViewById(R.id.text1);
			TextView text2 = (TextView)subitem.findViewById(R.id.text2);
			
			text1.setText(key + ": ");
			text2.setText((String)((Map<String, Object>)mLoadOrderDetailsDataTask.getData().get("billing_address")).get(key));
			mBillingAddressLayout.addView(subitem);
		}
		
		mItemsOrderedLayout.removeAllViews();
		for (Object item : (Object [])mLoadOrderDetailsDataTask.getData().get("items"))
		{
			Map<String, Object> itemMap = (Map<String, Object>) item;
			
			TextView product = (TextView)mInflater.inflate(R.layout.order_details_product, null);
			product.setText((String)itemMap.get("name") + " (qty: " + (String)itemMap.get("qty_ordered") + ")");
			
			mItemsOrderedLayout.addView(product);
		}
		
		mPaymentDetailsLayout.removeAllViews();
		for (String key : ((Map<String, Object>)mLoadOrderDetailsDataTask.getData().get("payment")).keySet())
		{
			if (! (((Map<String, Object>)mLoadOrderDetailsDataTask.getData().get("payment")).get(key) instanceof String))
			{
				continue;
			}
			
			LinearLayout subitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
			
			TextView text1 = (TextView)subitem.findViewById(R.id.text1);
			TextView text2 = (TextView)subitem.findViewById(R.id.text2);
			
			text1.setText(key + ": ");
			text2.setText((String)((Map<String, Object>)mLoadOrderDetailsDataTask.getData().get("payment")).get(key));
			mPaymentDetailsLayout.addView(subitem);
		}
		
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			
			/* If the spinning wheel is gone we can be sure no other load task is pending so we can start another one. */
			if (mSpinningWheel.getVisibility() == View.GONE)
			{
				mLoadOrderDetailsDataTask = new LoadOrderDetailsData(this, mOrderIncrementId, true);
				mLoadOrderDetailsDataTask.execute();
			}
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
