package com.mageventory.activity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Formatter;
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
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

public class OrderDetailsActivity extends BaseActivity {

	public class LinkMovementMethodWithSelect extends LinkMovementMethod
	{
		@Override
	    public boolean canSelectArbitrarily() {
	        return true;
	    }
	}
	
	public LinkMovementMethodWithSelect mLinkMovementMethodWithSelect = new LinkMovementMethodWithSelect();
	
	private ScrollView mOrderDetailsLayout;
	private LinearLayout mSpinningWheel;
	
	private LoadOrderDetailsData mLoadOrderDetailsDataTask;
	private TextView mOrderNumText;
	private TextView mDateTimeText;
	private TextView mStatusText;
	private TextView mCustomerNameText;
	private TextView mCustomerEmailText;
	private LayoutInflater mInflater;
	private String mOrderIncrementId;
	private Settings mSettings;
	private LinearLayout mMoreDetailsLayout;
	private LinearLayout mRawDumpLayout;
	private Button mRawDumpButtonShow;
	private Button mRawDumpButtonHide;
	
	private static final int INDENTATION_LEVEL_DP = 20;

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
		mCustomerNameText = (TextView) findViewById(R.id.customer_name_text);
		mCustomerEmailText = (TextView) findViewById(R.id.customer_email_text);
		
		mRawDumpLayout = (LinearLayout) findViewById(R.id.raw_dump_layout);
		mMoreDetailsLayout = (LinearLayout) findViewById(R.id.more_details_layout);
		
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
	
	private String keyToLabel(String key)
	{
		String [] wordsArray = key.split("_");
		
		for (int i=0; i<wordsArray.length; i++)
		{
			if (wordsArray[i].equals("base") && i==0)
			{
				continue;
			}
			
			String firstLetter = "" + wordsArray[i].charAt(0);
			wordsArray[i] = firstLetter.toUpperCase() + wordsArray[i].substring(1);
		}
		
		StringBuilder label = new StringBuilder();
		
		for (int i=0; i<wordsArray.length; i++)
		{
			if (wordsArray[i].equals("base") && i==0)
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
	
	private void rawDumpMapIntoLayout2(final Map<String, Object> map, int nestingLevel, ArrayList<String> keys_to_show)
	{
		Resources r = getResources();
		float indentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, INDENTATION_LEVEL_DP * nestingLevel, r.getDisplayMetrics());
		float arrayIndentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, INDENTATION_LEVEL_DP * (nestingLevel+1), r.getDisplayMetrics());
		
		for (String key : keys_to_show)
		{
			if (map.keySet().contains(key) && map.get(key) instanceof String)
			{
				LinearLayout subitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
				
				View indentation = subitem.findViewById(R.id.indentation);
				
				indentation.setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
				
				TextView text1 = (TextView)subitem.findViewById(R.id.text1);
				TextView text2 = (TextView)subitem.findViewById(R.id.text2);
				
				text1.setText(keyToLabel(key) + ": ");
				text2.setText((String)map.get(key));
				mMoreDetailsLayout.addView(subitem);
			}
			else
			if (key.equals("name_KEY_TO_LINKIFY") && map.keySet().contains("name"))
			{
				LinearLayout subitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
				
				View indentation = subitem.findViewById(R.id.indentation);
				
				indentation.setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
				
				TextView text1 = (TextView)subitem.findViewById(R.id.text1);
				TextView text2 = (TextView)subitem.findViewById(R.id.text2);
				
				text1.setText(keyToLabel("name") + ": ");
				text2.setText(Html.fromHtml("<font color=\"#5c5cff\"><u>" + (String)map.get("name") + "</u></font>")  );

				text2.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent newIntent = new Intent(OrderDetailsActivity.this, ProductDetailsActivity.class);

						newIntent.putExtra(getString(R.string.ekey_product_sku), (String)(map.get("sku")));
						newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

						OrderDetailsActivity.this.startActivity(newIntent);
					}
				});
				
				mMoreDetailsLayout.addView(subitem);
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
				text1.setText(keyToLabel(key));
				mMoreDetailsLayout.addView(subitem);
				
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
				text1.setText(keyToLabel(key));
				mMoreDetailsLayout.addView(subitem);
				
				for(int i=0; i<((Object [])map.get(key)).length; i++)
				{
					LinearLayout arraySubitem = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
					View arraySubitemIndentation = arraySubitem.findViewById(R.id.indentation);
					
					arraySubitemIndentation.setLayoutParams(new LinearLayout.LayoutParams((int)arrayIndentationWidthPix, 0));
					
					TextView arrayItemText1 = (TextView)arraySubitem.findViewById(R.id.text1);
					arrayItemText1.setText(keyToLabel(key + "[" + i + "]"));
					mMoreDetailsLayout.addView(arraySubitem);
					
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
			arrayItemText1.setText(keyToLabel(key + "[" + i + "]"));
			mMoreDetailsLayout.addView(arraySubitem);
			
			rawDumpMapIntoLayout2((Map<String, Object>)array[i], nestingLevel + 1, keys_to_show);
		}
	}

	private void createShippingAddressSection()
	{
		Map<String, Object> data = (Map<String, Object>)mLoadOrderDetailsDataTask.getData().get("shipping_address");
		
		Resources r = getResources();
		float indentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, INDENTATION_LEVEL_DP, r.getDisplayMetrics());
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Delivery Address");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		mMoreDetailsLayout.addView(header);
		
		LinearLayout name = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		name.findViewById(R.id.indentation).setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
		((TextView)name.findViewById(R.id.text1)).setVisibility(View.GONE);
		((TextView)name.findViewById(R.id.text2)).setText((String)data.get("firstname") + " " + (String)data.get("lastname"));
		mMoreDetailsLayout.addView(name);
		
		String address = "https://maps.google.com/maps?q=" + (String)data.get("country_id") + ", " + (String)data.get("city") + ", " + (String)data.get("street");
		address = address.replace(' ', '+');
		
		LinearLayout street = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		street.findViewById(R.id.indentation).setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
		((TextView)street.findViewById(R.id.text1)).setVisibility(View.GONE);
		((TextView)street.findViewById(R.id.text2)).setText(Html.fromHtml("<a href=\"" + address +"\">" +(String)data.get("street")+ "</a>"));
		((TextView)street.findViewById(R.id.text2)).setMovementMethod(mLinkMovementMethodWithSelect);
		mMoreDetailsLayout.addView(street);
		
		LinearLayout city = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		city.findViewById(R.id.indentation).setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
		((TextView)city.findViewById(R.id.text1)).setVisibility(View.GONE);
		((TextView)city.findViewById(R.id.text2)).setText(Html.fromHtml("<a href=\"" + address +"\">" + (String)data.get("city") + ", " + (String)data.get("postcode")+ "</a>"));
		((TextView)city.findViewById(R.id.text2)).setMovementMethod(mLinkMovementMethodWithSelect);
		mMoreDetailsLayout.addView(city);
		
		LinearLayout country = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		country.findViewById(R.id.indentation).setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
		((TextView)country.findViewById(R.id.text1)).setVisibility(View.GONE);
		((TextView)country.findViewById(R.id.text2)).setText(Html.fromHtml("<a href=\"" + address +"\">" + (String)data.get("country_id")+ "</a>"));
		((TextView)country.findViewById(R.id.text2)).setMovementMethod(mLinkMovementMethodWithSelect);
		mMoreDetailsLayout.addView(country);
		
		LinearLayout empty_space = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		empty_space.findViewById(R.id.indentation).setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
		((TextView)empty_space.findViewById(R.id.text1)).setVisibility(View.GONE);
		((TextView)empty_space.findViewById(R.id.text2)).setText("");
		mMoreDetailsLayout.addView(empty_space);
		
		LinearLayout telephone = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		telephone.findViewById(R.id.indentation).setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
		((TextView)telephone.findViewById(R.id.text1)).setVisibility(View.GONE);
		((TextView)telephone.findViewById(R.id.text2)).setText(Html.fromHtml("<a href=\"" + "tel://" + (String)data.get("telephone") + "\">" +"tel. " + (String)data.get("telephone")+ "</a>"));
		((TextView)telephone.findViewById(R.id.text2)).setMovementMethod(mLinkMovementMethodWithSelect);
		mMoreDetailsLayout.addView(telephone);
	}
	
	private void createPaymentSection()
	{
		Map<String, Object> data = (Map<String, Object>)mLoadOrderDetailsDataTask.getData().get("payment");
		
		Resources r = getResources();
		float indentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, INDENTATION_LEVEL_DP, r.getDisplayMetrics());
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Payment");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		mMoreDetailsLayout.addView(header);
		
		LinearLayout amount_ordered = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		amount_ordered.findViewById(R.id.indentation).setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
		((TextView)amount_ordered.findViewById(R.id.text1)).setText("Amount ordered: ");
		((TextView)amount_ordered.findViewById(R.id.text2)).setText((String)data.get("base_amount_ordered"));
		mMoreDetailsLayout.addView(amount_ordered);
		
		LinearLayout amount_paid = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		amount_paid.findViewById(R.id.indentation).setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
		((TextView)amount_paid.findViewById(R.id.text1)).setText("Amount paid: ");
		((TextView)amount_paid.findViewById(R.id.text2)).setText((String)data.get("amount_paid"));
		mMoreDetailsLayout.addView(amount_paid);
		
		LinearLayout method = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		method.findViewById(R.id.indentation).setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
		((TextView)method.findViewById(R.id.text1)).setText("Method: ");
		((TextView)method.findViewById(R.id.text2)).setText((String)data.get("method"));
		mMoreDetailsLayout.addView(method);
		
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
		
		mMoreDetailsLayout.addView(header);
		
		if (mLoadOrderDetailsDataTask.getData().get("credit_memos") != null)
		{
			rawDumpMapIntoLayout3("credit_memos", (Object[])mLoadOrderDetailsDataTask.getData().get("credit_memos"), 1, keysToShowArrayList);
		}
	}
	
	private void createStatusHistorySection()
	{
		Rect bounds = new Rect();
		int notifiedColumnWidthPix;
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Status History");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		mMoreDetailsLayout.addView(header);
		
		if (mLoadOrderDetailsDataTask.getData().get("status_history") != null)
		{
			LinearLayout statusesLayout = new LinearLayout(this);
			statusesLayout.setBackgroundColor(0x44444444);
			statusesLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			statusesLayout.setOrientation(LinearLayout.VERTICAL);
			
			LinearLayout statusLayoutHeader = new LinearLayout(this);
			statusLayoutHeader.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			
			TextView entityTextHeader = (TextView)mInflater.inflate(R.layout.order_details_textview_header, null);
			entityTextHeader.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
			entityTextHeader.setText("Entity");
			entityTextHeader.setBackgroundColor(0x66666666);
			entityTextHeader.setGravity(Gravity.CENTER_HORIZONTAL);
			
			TextView statusTextHeader = (TextView)mInflater.inflate(R.layout.order_details_textview_header, null);
			statusTextHeader.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
			statusTextHeader.setText("Status");
			statusTextHeader.setBackgroundColor(0x66666666);
			statusTextHeader.setGravity(Gravity.CENTER_HORIZONTAL);
			
			TextView createdTextHeader = (TextView)mInflater.inflate(R.layout.order_details_textview_header, null);
			createdTextHeader.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
			createdTextHeader.setText("Created");
			createdTextHeader.setBackgroundColor(0x66666666);
			createdTextHeader.setGravity(Gravity.CENTER_HORIZONTAL);
			
			TextView notifiedTextHeader = (TextView)mInflater.inflate(R.layout.order_details_textview_header, null);
			notifiedTextHeader.setText("Notified");
			notifiedTextHeader.setBackgroundColor(0x66666666);
			notifiedTextHeader.getPaint().getTextBounds("Notified-", 0, "Notified-".length(), bounds);
			notifiedColumnWidthPix = bounds.width();
			notifiedTextHeader.setLayoutParams(new LinearLayout.LayoutParams(notifiedColumnWidthPix, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
			notifiedTextHeader.setGravity(Gravity.CENTER_HORIZONTAL);
			
			statusLayoutHeader.addView(entityTextHeader);
			statusLayoutHeader.addView(statusTextHeader);
			statusLayoutHeader.addView(createdTextHeader);
			statusLayoutHeader.addView(notifiedTextHeader);
			
			statusesLayout.addView(statusLayoutHeader);
			
			Object[] statuses = (Object[])mLoadOrderDetailsDataTask.getData().get("status_history");
			
			for(int i=0; i<statuses.length; i++)
			{
				final Map<String, Object> status = (Map<String, Object>) statuses[i];
				
				LinearLayout statusLayout = new LinearLayout(this);
				statusLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				
				TextView entityText = (TextView)mInflater.inflate(R.layout.order_details_textview, null);
				entityText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
				entityText.setGravity(Gravity.CENTER_HORIZONTAL);
				
				TextView statusText = (TextView)mInflater.inflate(R.layout.order_details_textview, null);
				statusText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
				statusText.setGravity(Gravity.CENTER_HORIZONTAL);

				TextView createdText = (TextView)mInflater.inflate(R.layout.order_details_textview, null);
				createdText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
				createdText.setGravity(Gravity.CENTER_HORIZONTAL);
				
				TextView notifiedText = (TextView)mInflater.inflate(R.layout.order_details_textview, null);
				notifiedText.setLayoutParams(new LinearLayout.LayoutParams(notifiedColumnWidthPix, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
				notifiedText.setGravity(Gravity.CENTER_HORIZONTAL);
				
				entityText.setText((String)status.get("entity_name"));
				statusText.setText((String)status.get("status"));
				createdText.setText((String)status.get("created_at"));
				notifiedText.setText((String)status.get("is_customer_notified"));
				
				statusLayout.addView(entityText);
				statusLayout.addView(statusText);
				statusLayout.addView(createdText);
				statusLayout.addView(notifiedText);
				
				statusesLayout.addView(statusLayout);
			}
	
			mMoreDetailsLayout.addView(statusesLayout);
		}
	}
	
	private String roundNumber(String number)
	{
		return String.format("%.2f", new Double(number));
	}
	
	private void createProductsListSection()
	{
		Resources r = getResources();
		int columnWidthPx = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, r.getDisplayMetrics());
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Products List");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		mMoreDetailsLayout.addView(header);
		
		if (mLoadOrderDetailsDataTask.getData().get("items") != null)
		{
			LinearLayout productsLayout = new LinearLayout(this);
			productsLayout.setBackgroundColor(0x44444444);
			productsLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			productsLayout.setOrientation(LinearLayout.VERTICAL);
			
			LinearLayout productLayoutHeader = new LinearLayout(this);
			productLayoutHeader.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			
			TextView productNameTextHeader = (TextView)mInflater.inflate(R.layout.order_details_textview_header, null);
			productNameTextHeader.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
			productNameTextHeader.setText("Product name");
			productNameTextHeader.setBackgroundColor(0x66666666);
			
			TextView priceTextHeader = (TextView)mInflater.inflate(R.layout.order_details_textview_header, null);
			priceTextHeader.setLayoutParams(new LinearLayout.LayoutParams(columnWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
			priceTextHeader.setText("Price");
			priceTextHeader.setBackgroundColor(0x66666666);
			priceTextHeader.setGravity(Gravity.RIGHT);
			
			TextView qtyTextHeader = (TextView)mInflater.inflate(R.layout.order_details_textview_header, null);
			qtyTextHeader.setLayoutParams(new LinearLayout.LayoutParams(columnWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
			qtyTextHeader.setText("Qty");
			qtyTextHeader.setBackgroundColor(0x66666666);
			qtyTextHeader.setGravity(Gravity.RIGHT);
			
			productLayoutHeader.addView(productNameTextHeader);
			productLayoutHeader.addView(priceTextHeader);
			productLayoutHeader.addView(qtyTextHeader);
			
			productsLayout.addView(productLayoutHeader);
			
			Object[] products = (Object[])mLoadOrderDetailsDataTask.getData().get("items");
			
			for(int i=0; i<products.length; i++)
			{
				final Map<String, Object> product = (Map<String, Object>) products[i];
				
				LinearLayout productLayout = new LinearLayout(this);
				productLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				
				TextView productNameText = (TextView)mInflater.inflate(R.layout.order_details_textview, null);
				productNameText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
				
				TextView priceText = (TextView)mInflater.inflate(R.layout.order_details_textview, null);
				priceText.setLayoutParams(new LinearLayout.LayoutParams(columnWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
				priceText.setGravity(Gravity.RIGHT);
				
				TextView qtyText = (TextView)mInflater.inflate(R.layout.order_details_textview, null);
				qtyText.setLayoutParams(new LinearLayout.LayoutParams(columnWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
				qtyText.setGravity(Gravity.RIGHT);
				
				productNameText.setText(Html.fromHtml("<font color=\"#5c5cff\"><u>" + (String)product.get("name") + "</u></font>")  );
				productNameText.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent newIntent = new Intent(OrderDetailsActivity.this, ProductDetailsActivity.class);

						newIntent.putExtra(getString(R.string.ekey_product_sku), (String)(product.get("sku")));
						newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

						OrderDetailsActivity.this.startActivity(newIntent);
					}
				});
				
				priceText.setText("$" + roundNumber((String)product.get("base_price")));
				qtyText.setText(roundNumber((String)product.get("qty_ordered")));
				
				productLayout.addView(productNameText);
				productLayout.addView(priceText);
				productLayout.addView(qtyText);
				
				productsLayout.addView(productLayout);
			}
	
			mMoreDetailsLayout.addView(productsLayout);
		}
	}
	
	private void createShipmentsSection()
	{
		final String [] KEYS_TO_SHOW = {"total_qty", "qty_ordered", "increment_id", "shipment_id", "items", "name_KEY_TO_LINKIFY", "qty",
			"tracks", "carrier_code", "title", "track_number", "created_at"};
		ArrayList<String> keysToShowArrayList = new ArrayList<String>();
		Collections.addAll(keysToShowArrayList, KEYS_TO_SHOW);
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Shipments");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		mMoreDetailsLayout.addView(header);
		
		if (mLoadOrderDetailsDataTask.getData().get("shipments") != null)
		{
			rawDumpMapIntoLayout3("shipments", (Object[])mLoadOrderDetailsDataTask.getData().get("shipments"), 1, keysToShowArrayList);
		}
	}

	private void createCommentsSection()
	{
		final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		ArrayList<Object> commentsList = new ArrayList<Object>();
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Comments");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		mMoreDetailsLayout.addView(header);
		
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
						Map<String, Object> commMap = (Map<String, Object>)comm;
						commMap.put("description", "Credit memo #" + creditMemo.get("increment_id"));
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
						Map<String, Object> commMap = (Map<String, Object>)comm;
						commMap.put("description", "Invoice #" + invoice.get("increment_id"));
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
						Map<String, Object> commMap = (Map<String, Object>)comm;
						commMap.put("description", "Shipment #" + shipment.get("increment_id"));
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
		
		Rect bounds = new Rect();
		int descriptionColumnWidthPix;
		int createdAtColumnWidthPix;
		int notifiedColumnWidthPix;
		
		LinearLayout commentsLayout = new LinearLayout(this);
		commentsLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		commentsLayout.setOrientation(LinearLayout.VERTICAL);
		commentsLayout.setBackgroundColor(0x44444444);
		
		LinearLayout commentLayoutHeader = new LinearLayout(this);
		commentLayoutHeader.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		TextView commentTextHeader = (TextView)mInflater.inflate(R.layout.order_details_textview_header, null);
		commentTextHeader.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		commentTextHeader.setText("Comment");
		commentTextHeader.setBackgroundColor(0x66666666);
		
		TextView desciptionTextHeader = (TextView)mInflater.inflate(R.layout.order_details_textview_header, null);
		desciptionTextHeader.setGravity(Gravity.CENTER_HORIZONTAL);
		desciptionTextHeader.setText("Description");
		desciptionTextHeader.setBackgroundColor(0x66666666);
		desciptionTextHeader.getPaint().getTextBounds("#0000000000", 0, "#0000000000".length(), bounds);
		descriptionColumnWidthPix = bounds.width();
		desciptionTextHeader.setLayoutParams(new LinearLayout.LayoutParams(descriptionColumnWidthPix, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
		
		TextView createdAtTextHeader = (TextView)mInflater.inflate(R.layout.order_details_textview_header, null);
		createdAtTextHeader.setGravity(Gravity.CENTER_HORIZONTAL);
		createdAtTextHeader.setText("Created");
		createdAtTextHeader.setBackgroundColor(0x66666666);
		createdAtTextHeader.getPaint().getTextBounds("2012-01-01-", 0, "2012-01-01-".length(), bounds);
		createdAtColumnWidthPix = bounds.width();
		createdAtTextHeader.setLayoutParams(new LinearLayout.LayoutParams(createdAtColumnWidthPix, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
		
		TextView notifiedTextHeader = (TextView)mInflater.inflate(R.layout.order_details_textview_header, null);
		notifiedTextHeader.setGravity(Gravity.CENTER_HORIZONTAL);
		notifiedTextHeader.setText("Notified");
		notifiedTextHeader.setBackgroundColor(0x66666666);
		notifiedTextHeader.getPaint().getTextBounds("Notified-", 0, "Notified-".length(), bounds);
		notifiedColumnWidthPix = bounds.width();
		notifiedTextHeader.setLayoutParams(new LinearLayout.LayoutParams(notifiedColumnWidthPix, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
		
		commentLayoutHeader.addView(commentTextHeader);
		commentLayoutHeader.addView(desciptionTextHeader);
		commentLayoutHeader.addView(createdAtTextHeader);
		commentLayoutHeader.addView(notifiedTextHeader);
		
		commentsLayout.addView(commentLayoutHeader);
		
		for(Object comment : commentsList)
		{
			final Map<String, Object> product = (Map<String, Object>) comment;
			
			LinearLayout commentLayout = new LinearLayout(this);
			commentLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			
			TextView commentText = (TextView)mInflater.inflate(R.layout.order_details_textview, null);
			commentText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
			
			TextView desciptionText = (TextView)mInflater.inflate(R.layout.order_details_textview, null);
			desciptionText.setLayoutParams(new LinearLayout.LayoutParams(descriptionColumnWidthPix, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
			desciptionText.setGravity(Gravity.CENTER_HORIZONTAL);

			TextView createdAtText = (TextView)mInflater.inflate(R.layout.order_details_textview, null);
			createdAtText.setLayoutParams(new LinearLayout.LayoutParams(createdAtColumnWidthPix, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
			createdAtText.setGravity(Gravity.CENTER_HORIZONTAL);
			
			TextView notifiedText = (TextView)mInflater.inflate(R.layout.order_details_textview, null);
			notifiedText.setLayoutParams(new LinearLayout.LayoutParams(notifiedColumnWidthPix, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
			notifiedText.setGravity(Gravity.CENTER_HORIZONTAL);
			
			commentText.setText((String)product.get("comment"));
			desciptionText.setText((String)product.get("description"));
			createdAtText.setText((String)product.get("created_at"));
			notifiedText.setText((String)product.get("is_customer_notified"));
			
			commentLayout.addView(commentText);
			commentLayout.addView(desciptionText);
			commentLayout.addView(createdAtText);
			commentLayout.addView(notifiedText);
			
			commentsLayout.addView(commentLayout);
		}
		
		mMoreDetailsLayout.addView(commentsLayout);
	}
	
	private void fillTextViewsWithData()
	{
		if (mLoadOrderDetailsDataTask == null || mLoadOrderDetailsDataTask.getData() == null)
			return;
		
		String orderLink = mSettings.getUrl() + "/index.php/admin/sales_order/view/order_id/" + (String)mLoadOrderDetailsDataTask.getData().get("order_id");
		
		mOrderNumText.setText(Html.fromHtml("<a href=\"" + orderLink + "\">" + (String)mLoadOrderDetailsDataTask.getData().get("increment_id") + "</a>"));
		mOrderNumText.setMovementMethod(mLinkMovementMethodWithSelect);
		
		mDateTimeText.setText((String)mLoadOrderDetailsDataTask.getData().get("created_at"));
		
		mStatusText.setText((String)mLoadOrderDetailsDataTask.getData().get("status"));
		
		String customerLink = mSettings.getUrl() + "/index.php/admin/customer/edit/id/" + (String)mLoadOrderDetailsDataTask.getData().get("customer_id");
		
		mCustomerNameText.setText(Html.fromHtml("<a href=\"" + customerLink + "\">" +(String)mLoadOrderDetailsDataTask.getData().get("customer_firstname") + " " + (String)mLoadOrderDetailsDataTask.getData().get("customer_lastname")+ "</a>"));
		mCustomerNameText.setMovementMethod(mLinkMovementMethodWithSelect);
		
		mCustomerEmailText.setText(  Html.fromHtml("<a href=\"mailto:" + (String)mLoadOrderDetailsDataTask.getData().get("customer_email") + "\">" +  (String)mLoadOrderDetailsDataTask.getData().get("customer_email")+ "</a>"));
		mCustomerEmailText.setMovementMethod(mLinkMovementMethodWithSelect);

		mMoreDetailsLayout.removeAllViews();
		mRawDumpLayout.removeAllViews();
		
		////////////////
		
		createProductsListSection();
		createPaymentSection();
		createShippingAddressSection();
		createShipmentsSection();
		createCreditMemosSection();
		createStatusHistorySection();
		createCommentsSection();
		
		////////////////
		
		rawDumpMapIntoLayout(mLoadOrderDetailsDataTask.getData(), 0);
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
