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
	private LayoutInflater mInflater;
	private String mOrderIncrementId;
	private Settings mSettings;
	private LinearLayout mMoreDetailsLayout;
	private LinearLayout mRawDumpLayout;
	private Button mMoreDetailsButtonShow;
	private Button mMoreDetailsButtonHide;
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
		
		mMoreDetailsButtonShow = (Button) findViewById(R.id.more_details_button_show);
		mMoreDetailsButtonHide = (Button) findViewById(R.id.more_details_button_hide);
		
		mRawDumpButtonShow = (Button) findViewById(R.id.raw_dump_button_show);
		mRawDumpButtonHide = (Button) findViewById(R.id.raw_dump_button_hide);
		
		mMoreDetailsButtonShow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mMoreDetailsLayout.setVisibility(View.VISIBLE);
				mMoreDetailsButtonShow.setEnabled(false);
				mMoreDetailsButtonHide.setEnabled(true);
			}
		});
		
		mMoreDetailsButtonHide.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mMoreDetailsLayout.setVisibility(View.GONE);
				mMoreDetailsButtonShow.setEnabled(true);
				mMoreDetailsButtonHide.setEnabled(false);
			}
		});
		
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
			
			rawDumpMapIntoLayout2((Map<String, Object>)array[i], nestingLevel + 2, keys_to_show);
		}
	}
	
	private void createAccountInformationSection()
	{
		Map<String, Object> data = mLoadOrderDetailsDataTask.getData();
		
		Resources r = getResources();
		float indentationWidthPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, INDENTATION_LEVEL_DP, r.getDisplayMetrics());
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Account information");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		mMoreDetailsLayout.addView(header);
		
		String customerLink = mSettings.getUrl() + "/index.php/admin/customer/edit/id/" + (String)mLoadOrderDetailsDataTask.getData().get("customer_id");
		
		LinearLayout customer_name = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		customer_name.findViewById(R.id.indentation).setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
		((TextView)customer_name.findViewById(R.id.text1)).setText("Name: ");
		((TextView)customer_name.findViewById(R.id.text2)).setText(Html.fromHtml("<a href=\"" + customerLink + "\">" +(String)data.get("customer_firstname") + " " + (String)data.get("customer_lastname")+ "</a>"));
		((TextView)customer_name.findViewById(R.id.text2)).setMovementMethod(LinkMovementMethod.getInstance());
		
		mMoreDetailsLayout.addView(customer_name);
		
		LinearLayout customer_email = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		customer_email.findViewById(R.id.indentation).setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
		((TextView)customer_email.findViewById(R.id.text1)).setText("Email: ");
		((TextView)customer_email.findViewById(R.id.text2)).setText(  Html.fromHtml("<a href=\"mailto:" + (String)data.get("customer_email") + "\">" +  (String)data.get("customer_email")+ "</a>"));
		((TextView)customer_email.findViewById(R.id.text2)).setMovementMethod(LinkMovementMethod.getInstance());

		mMoreDetailsLayout.addView(customer_email);

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
		((TextView)street.findViewById(R.id.text2)).setMovementMethod(LinkMovementMethod.getInstance());
		mMoreDetailsLayout.addView(street);
		
		LinearLayout city = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		city.findViewById(R.id.indentation).setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
		((TextView)city.findViewById(R.id.text1)).setVisibility(View.GONE);
		((TextView)city.findViewById(R.id.text2)).setText(Html.fromHtml("<a href=\"" + address +"\">" + (String)data.get("city") + ", " + (String)data.get("postcode")+ "</a>"));
		((TextView)city.findViewById(R.id.text2)).setMovementMethod(LinkMovementMethod.getInstance());
		mMoreDetailsLayout.addView(city);
		
		LinearLayout country = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		country.findViewById(R.id.indentation).setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
		((TextView)country.findViewById(R.id.text1)).setVisibility(View.GONE);
		((TextView)country.findViewById(R.id.text2)).setText(Html.fromHtml("<a href=\"" + address +"\">" + (String)data.get("country_id")+ "</a>"));
		((TextView)country.findViewById(R.id.text2)).setMovementMethod(LinkMovementMethod.getInstance());
		mMoreDetailsLayout.addView(country);
		
		LinearLayout telephone = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		telephone.findViewById(R.id.indentation).setLayoutParams(new LinearLayout.LayoutParams((int)indentationWidthPix, 0));
		((TextView)telephone.findViewById(R.id.text1)).setVisibility(View.GONE);
		((TextView)telephone.findViewById(R.id.text2)).setText(Html.fromHtml("<a href=\"" + "tel://" + (String)data.get("telephone") + "\">" +"tel. " + (String)data.get("telephone")+ "</a>"));
		((TextView)telephone.findViewById(R.id.text2)).setMovementMethod(LinkMovementMethod.getInstance());
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
		final String [] KEYS_TO_SHOW = {"created_at", "entity_name", "status", "comment", "is_customer_notified"};
		ArrayList<String> keysToShowArrayList = new ArrayList<String>();
		Collections.addAll(keysToShowArrayList, KEYS_TO_SHOW);
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Status History");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		mMoreDetailsLayout.addView(header);
		
		if (mLoadOrderDetailsDataTask.getData().get("status_history") != null)
		{
			rawDumpMapIntoLayout3("status_history", (Object[])mLoadOrderDetailsDataTask.getData().get("status_history"), 1, keysToShowArrayList);
		}
	}
	
	private void createProductsListSection()
	{
		final String [] KEYS_TO_SHOW = {"base_price", "qty_ordered", "name_KEY_TO_LINKIFY"};
		ArrayList<String> keysToShowArrayList = new ArrayList<String>();
		Collections.addAll(keysToShowArrayList, KEYS_TO_SHOW);
		
		LinearLayout header = (LinearLayout)mInflater.inflate(R.layout.order_details_sub_item, null);
		TextView headerText = (TextView)header.findViewById(R.id.text1);
		headerText.setText("Products List");
		headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		
		mMoreDetailsLayout.addView(header);
		
		if (mLoadOrderDetailsDataTask.getData().get("items") != null)
		{
			rawDumpMapIntoLayout3("items", (Object[])mLoadOrderDetailsDataTask.getData().get("items"), 1, keysToShowArrayList);
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
		
		final String [] KEYS_TO_SHOW = {"description", "created_at", "is_customer_notified", "comment"};
		ArrayList<String> keysToShowArrayList = new ArrayList<String>();
		Collections.addAll(keysToShowArrayList, KEYS_TO_SHOW);
		
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
		
		rawDumpMapIntoLayout3("comments", commentsList.toArray(), 1, keysToShowArrayList);
	}
	
	private void fillTextViewsWithData()
	{
		if (mLoadOrderDetailsDataTask == null || mLoadOrderDetailsDataTask.getData() == null)
			return;
		
		String orderLink = mSettings.getUrl() + "/index.php/admin/sales_order/view/order_id/" + (String)mLoadOrderDetailsDataTask.getData().get("order_id");
		
		mOrderNumText.setText(Html.fromHtml("<a href=\"" + orderLink + "\">" + (String)mLoadOrderDetailsDataTask.getData().get("increment_id") + "</a>"));
		mOrderNumText.setMovementMethod(LinkMovementMethod.getInstance());
		
		mDateTimeText.setText((String)mLoadOrderDetailsDataTask.getData().get("created_at"));
		
		mStatusText.setText((String)mLoadOrderDetailsDataTask.getData().get("status"));

		mMoreDetailsLayout.removeAllViews();
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
