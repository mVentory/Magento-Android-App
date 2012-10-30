package com.mageventory.activity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
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
	private LayoutInflater mInflater;
	private String mOrderIncrementId;
	private Settings mSettings;
	private LinearLayout mRawDumpLayout;
	private Button mRawDumpButtonShow;
	private Button mRawDumpButtonHide;
	
	private static final int INDENTATION_LEVEL_DP = 20;
	
	class Comment
	{
		public Date createdAt;
		public String comment;
		public String customerNotified;
	}
	
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
		
		mRawDumpLayout = (LinearLayout) findViewById(R.id.raw_dump_layout);
		
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
				mRawDumpLayout.setVisibility(View.VISIBLE);
				mRawDumpButtonShow.setEnabled(false);
				mRawDumpButtonHide.setEnabled(true);
			}
		});
		
		mRawDumpButtonHide.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mRawDumpLayout.setVisibility(View.GONE);
				mRawDumpButtonShow.setEnabled(true);
				mRawDumpButtonHide.setEnabled(false);
			}
		});
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
	
	private void rawDumpMapIntoLayout(Map<String, Object> map, int nestingLevel)
	{
		Resources r = getResources();
		float indentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, INDENTATION_LEVEL_DP * nestingLevel, r.getDisplayMetrics());
		float arrayIndentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, INDENTATION_LEVEL_DP * (nestingLevel+1), r.getDisplayMetrics());
		
		for (String key : map.keySet())
		{
			if (map.get(key) instanceof String)
			{
				LinearLayout subitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
				
				View indentation = subitem.findViewById(R.id.indentation);
				
				indentation.setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
				
				TextView text1 = (TextView)subitem.findViewById(R.id.text1);
				TextView text2 = (TextView)subitem.findViewById(R.id.text2);
				
				text1.setText(key + ": ");
				text2.setText((String)map.get(key));
				mRawDumpLayout.addView(subitem);
			}
		}

		for (String key : map.keySet())
		{
			if (map.get(key) instanceof Map)
			{
				LinearLayout subitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
				
				View indentation = subitem.findViewById(R.id.indentation);
				
				indentation.setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
				
				TextView text1 = (TextView)subitem.findViewById(R.id.text1);
				text1.setText(key);
				mRawDumpLayout.addView(subitem);
				
				rawDumpMapIntoLayout((Map<String, Object>)map.get(key), nestingLevel + 1);
			}
		}
				
		for (String key : map.keySet())
		{
			if (map.get(key) instanceof Object[])
			{
				LinearLayout subitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
				
				View indentation = subitem.findViewById(R.id.indentation);
				
				indentation.setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
				
				TextView text1 = (TextView)subitem.findViewById(R.id.text1);
				text1.setText(key);
				mRawDumpLayout.addView(subitem);
				
				for(int i=0; i<((Object [])map.get(key)).length; i++)
				{
					LinearLayout arraySubitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
					View arraySubitemIndentation = arraySubitem.findViewById(R.id.indentation);
					
					arraySubitemIndentation.setLayoutParams(new LinearLayout.LayoutParams((int)arrayIndentationWidthPix, 0));
					
					TextView arrayItemText1 = (TextView)arraySubitem.findViewById(R.id.text1);
					arrayItemText1.setText(key + "[" + i + "]");
					mRawDumpLayout.addView(arraySubitem);
					
					rawDumpMapIntoLayout((Map<String, Object>)(((Object[])map.get(key))[i]), nestingLevel + 2);
				}
			}
		}
	}
	
	
	
	private void rawDumpMapIntoLayout2(Map<String, Object> map, int nestingLevel, ArrayList<String> keys_to_show)
	{
		Resources r = getResources();
		float indentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, INDENTATION_LEVEL_DP * nestingLevel, r.getDisplayMetrics());
		float arrayIndentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, INDENTATION_LEVEL_DP * (nestingLevel+1), r.getDisplayMetrics());
		
		for (String key : map.keySet())
		{
			if (keys_to_show.contains(key) && map.get(key) instanceof String)
			{
				LinearLayout subitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
				
				View indentation = subitem.findViewById(R.id.indentation);
				
				indentation.setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
				
				TextView text1 = (TextView)subitem.findViewById(R.id.text1);
				TextView text2 = (TextView)subitem.findViewById(R.id.text2);
				
				text1.setText(key + ": ");
				text2.setText((String)map.get(key));
				mRawDumpLayout.addView(subitem);
			}
		}

		for (String key : map.keySet())
		{
			if (keys_to_show.contains(key) && map.get(key) instanceof Map)
			{
				LinearLayout subitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
				
				View indentation = subitem.findViewById(R.id.indentation);
				
				indentation.setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
				
				TextView text1 = (TextView)subitem.findViewById(R.id.text1);
				text1.setText(key);
				mRawDumpLayout.addView(subitem);
				
				rawDumpMapIntoLayout((Map<String, Object>)map.get(key), nestingLevel + 1);
			}
		}
				
		for (String key : map.keySet())
		{
			if (keys_to_show.contains(key) && map.get(key) instanceof Object[])
			{
				LinearLayout subitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
				
				View indentation = subitem.findViewById(R.id.indentation);
				
				indentation.setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
				
				TextView text1 = (TextView)subitem.findViewById(R.id.text1);
				text1.setText(key);
				mRawDumpLayout.addView(subitem);
				
				for(int i=0; i<((Object [])map.get(key)).length; i++)
				{
					LinearLayout arraySubitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
					View arraySubitemIndentation = arraySubitem.findViewById(R.id.indentation);
					
					arraySubitemIndentation.setLayoutParams(new LinearLayout.LayoutParams((int)arrayIndentationWidthPix, 0));
					
					TextView arrayItemText1 = (TextView)arraySubitem.findViewById(R.id.text1);
					arrayItemText1.setText(key + "[" + i + "]");
					mRawDumpLayout.addView(arraySubitem);
					
					rawDumpMapIntoLayout2((Map<String, Object>)(((Object[])map.get(key))[i]), nestingLevel + 2, keys_to_show);
				}
			}
		}
	}
	
	private void rawDumpMapIntoLayout3(String key, Object [] array, int nestingLevel, ArrayList<String> keys_to_show)
	{
		Resources r = getResources();
		float indentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, INDENTATION_LEVEL_DP * nestingLevel, r.getDisplayMetrics());
		
		for(int i=0; i<array.length; i++)
		{
			LinearLayout arraySubitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
			View arraySubitemIndentation = arraySubitem.findViewById(R.id.indentation);
					
			arraySubitemIndentation.setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
					
			TextView arrayItemText1 = (TextView)arraySubitem.findViewById(R.id.text1);
			arrayItemText1.setText(key + "[" + i + "]");
			mRawDumpLayout.addView(arraySubitem);
			
			rawDumpMapIntoLayout2((Map<String, Object>)array[i], nestingLevel + 2, keys_to_show);
		}
	}
	
	private void createAccountInformationSection()
	{
		final String [] KEYS_TO_SHOW = {"customer_lastname", "customer_firstname", "customer_email"};
		ArrayList<String> keysToShowArrayList = new ArrayList<String>();
		Collections.addAll(keysToShowArrayList, KEYS_TO_SHOW);
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Account information");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		mRawDumpLayout.addView(header);
		rawDumpMapIntoLayout2(mLoadOrderDetailsDataTask.getData(), 1, keysToShowArrayList);
	}
	
	private void createShippingAddressSection()
	{
		final String [] KEYS_TO_SHOW = {"email", "street", "country_id", "lastname", "firstname", "postcode", "telephone", "city"};
		ArrayList<String> keysToShowArrayList = new ArrayList<String>();
		Collections.addAll(keysToShowArrayList, KEYS_TO_SHOW);
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Delivery Address");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		mRawDumpLayout.addView(header);
		
		if (mLoadOrderDetailsDataTask.getData().get("shipping_address") != null)
		{
			rawDumpMapIntoLayout2((Map<String, Object>)mLoadOrderDetailsDataTask.getData().get("shipping_address"), 1, keysToShowArrayList);	
		}
	}
	
	private void createPaymentSection()
	{
		final String [] KEYS_TO_SHOW = {"base_amount_ordered", "amount_paid", "method"};
		ArrayList<String> keysToShowArrayList = new ArrayList<String>();
		Collections.addAll(keysToShowArrayList, KEYS_TO_SHOW);
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Payment");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		mRawDumpLayout.addView(header);
		
		if (mLoadOrderDetailsDataTask.getData().get("payment") != null)
		{
			rawDumpMapIntoLayout2((Map<String, Object>)mLoadOrderDetailsDataTask.getData().get("payment"), 1, keysToShowArrayList);	
		}
	}
	
	private void createCreditMemosSection()
	{
		final String [] KEYS_TO_SHOW = {"credit_memos", "increment_id", "created_at", "base_adjustment_negative", "base_shipping_amount",
			"base_hidden_tax_amount", "base_subtotal_incl_tax", "base_discount_amount", "base_shipping_tax_amount", "base_shipping_incl_tax",
			"base_subtotal", "base_adjustment_positive", "base_grand_total"};
		ArrayList<String> keysToShowArrayList = new ArrayList<String>();
		Collections.addAll(keysToShowArrayList, KEYS_TO_SHOW);
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Credit Memos");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		mRawDumpLayout.addView(header);
		
		if (mLoadOrderDetailsDataTask.getData().get("credit_memos") != null)
		{
			rawDumpMapIntoLayout3("credit_memos", (Object[])mLoadOrderDetailsDataTask.getData().get("credit_memos"), 1, keysToShowArrayList);
		}
	}
	
	private void createStatusHistorySection()
	{
		final String [] KEYS_TO_SHOW = {"created_at", "entity_name", "status"};
		ArrayList<String> keysToShowArrayList = new ArrayList<String>();
		Collections.addAll(keysToShowArrayList, KEYS_TO_SHOW);
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Status History");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		mRawDumpLayout.addView(header);
		
		if (mLoadOrderDetailsDataTask.getData().get("status_history") != null)
		{
			rawDumpMapIntoLayout3("status_history", (Object[])mLoadOrderDetailsDataTask.getData().get("status_history"), 1, keysToShowArrayList);
		}
	}
	
	private void createProductsListSection()
	{
		final String [] KEYS_TO_SHOW = {"base_price", "product_id", "qty_ordered", "name"};
		ArrayList<String> keysToShowArrayList = new ArrayList<String>();
		Collections.addAll(keysToShowArrayList, KEYS_TO_SHOW);
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Products List");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		mRawDumpLayout.addView(header);
		
		if (mLoadOrderDetailsDataTask.getData().get("items") != null)
		{
			rawDumpMapIntoLayout3("items", (Object[])mLoadOrderDetailsDataTask.getData().get("items"), 1, keysToShowArrayList);
		}
	}
	
	private void createShipmentsSection()
	{
		final String [] KEYS_TO_SHOW = {"total_qty", "qty_ordered", "increment_id", "shipment_id", "items", "product_id", "name", "qty",
			"tracks", "carrier_code", "title", "track_number"};
		ArrayList<String> keysToShowArrayList = new ArrayList<String>();
		Collections.addAll(keysToShowArrayList, KEYS_TO_SHOW);
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Shipments");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		mRawDumpLayout.addView(header);
		
		if (mLoadOrderDetailsDataTask.getData().get("shipments") != null)
		{
			rawDumpMapIntoLayout3("shipments", (Object[])mLoadOrderDetailsDataTask.getData().get("shipments"), 1, keysToShowArrayList);
		}
	}
	
	private Comment parseComment(Map<String, Object> commentMap, boolean statusHistoryComment) throws ParseException
	{
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		if (statusHistoryComment == false)
		{
			Comment comment = new Comment();
			
			comment.createdAt = dateFormatter.parse((String)commentMap.get("created_at")); 
			comment.comment = (String) commentMap.get("comment");
			comment.customerNotified = (String) commentMap.get("is_customer_notified");
			
			return comment;
		}
		
		return null;
	}

	private void createCommentsSection()
	{
		final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		final String [] KEYS_TO_SHOW = {"created_at", "is_customer_notified", "comment"};
		ArrayList<String> keysToShowArrayList = new ArrayList<String>();
		Collections.addAll(keysToShowArrayList, KEYS_TO_SHOW);
		
		ArrayList<Object> commentsList = new ArrayList<Object>();
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Comments");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		mRawDumpLayout.addView(header);
		
		if (mLoadOrderDetailsDataTask.getData().get("credit_memos") != null)
		{
			Object[] creditMemos = (Object[])mLoadOrderDetailsDataTask.getData().get("credit_memos");
			
			for (Object cm : creditMemos)
			{
				Map<String, Object> creditMemo = (Map<String, Object>)cm;
				
				if (creditMemo.get("comments") != null)
				{
					for (Object comm : (Object[])creditMemo.get("comments"))
					{
						commentsList.add(comm);
					}
				}
			}
		}
		
		if (mLoadOrderDetailsDataTask.getData().get("invoices") != null)
		{
			Object[] invoices = (Object[])mLoadOrderDetailsDataTask.getData().get("invoices");
			
			for (Object inv : invoices)
			{
				Map<String, Object> invoice = (Map<String, Object>)inv;
				
				if (invoice.get("comments") != null)
				{
					for (Object comm : (Object[])invoice.get("comments"))
					{
						commentsList.add(comm);
					}
				}
			}
		}
		
		if (mLoadOrderDetailsDataTask.getData().get("shipments") != null)
		{
			Object[] shipments = (Object[])mLoadOrderDetailsDataTask.getData().get("shipments");
			
			for (Object shi : shipments)
			{
				Map<String, Object> shipment = (Map<String, Object>)shi;
				
				if (shipment.get("comments") != null)
				{
					for (Object comm : (Object[])shipment.get("comments"))
					{
						commentsList.add(comm);
					}
				}
			}
		}
		
		if (mLoadOrderDetailsDataTask.getData().get("status_history") != null)
		{
			Object[] statuses = (Object[])mLoadOrderDetailsDataTask.getData().get("status_history");
			
			for (Object st : statuses)
			{
				Map<String, Object> status = (Map<String, Object>)st;
				
				if (status.get("comment") != null)
				{
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
					leftDate = dateFormatter.parse((String)((Map<String, Object>)lhs).get("created_at"));
					rightDate = dateFormatter.parse((String)((Map<String, Object>)rhs).get("created_at"));
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
		
		rawDumpMapIntoLayout3("comments", commentsList.toArray(), 1, keysToShowArrayList);
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
		
		mRawDumpLayout.removeAllViews();
		
		////////////////
		
		createAccountInformationSection();
		createShippingAddressSection();
		createPaymentSection();
		createCreditMemosSection();
		createStatusHistorySection();
		createProductsListSection();
		createShipmentsSection();
		createCommentsSection();
		
		////////////////
		
		//rawDumpMapIntoLayout(mLoadOrderDetailsDataTask.getData(), 0);
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
