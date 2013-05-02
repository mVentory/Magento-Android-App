package com.mageventory.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.R.id;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Category;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.model.CustomAttributesList.OnNewOptionTaskEventListener;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.LoadAttributeSets;
import com.mageventory.tasks.LoadAttributesList;
import com.mageventory.tasks.LoadCategories;
import com.mageventory.util.DefaultOptionsMenuHelper;
import com.mageventory.util.DialogUtil;
import com.mageventory.util.DialogUtil.OnCategorySelectListener;
import com.mageventory.util.Util;

public abstract class AbsProductActivity extends BaseActivity implements MageventoryConstants, OperationObserver {

	public static final boolean ENABLE_CATEGORIES = false;
	
	public static class CategoriesData {
		public Map<String, Object> categories; // root category
	}

	// icicle keys
	// private String IKEY_CATEGORY_REQID = "category request id";
	// private String IKEY_ATTRIBUTE_SET_REQID = "attribute set request id";

	// views
	protected LayoutInflater inflater;
	protected LinearLayout container;
	protected View atrListWrapperV;
	public ViewGroup atrListV;
	protected EditText attributeSetV;
	protected EditText categoryV;
	protected LinearLayout categoryLabelLayoutV;
	protected TextView atrSetLabelV;
	protected TextView categoryLabelV;
	protected TextView atrListLabelV;
	protected ProgressBar atrSetProgressV;
	protected ProgressBar categoryProgressV;
	protected ProgressBar atrListProgressV;
	protected LinearLayout layoutNewOptionPending;
	protected LinearLayout layoutSKUcheckPending;
	public AutoCompleteTextView nameV;
	public EditText skuV;
	public AutoCompleteTextView descriptionV;
	public EditText barcodeInput;
	protected int newAttributeOptionPendingCount;
	private OnNewOptionTaskEventListener newOptionListener;
	public CheckBox statusV;

	boolean attributeSetLongTap;
	
	protected Settings mSettings;

	// data
	// protected int categoryId;

	public CustomAttributesList customAttributesList;
	public int atrSetId = INVALID_ATTRIBUTE_SET_ID;
	public Category category;

	// private int attributeSetRequestId = INVALID_REQUEST_ID;
	// private int categoryRequestId = INVALID_REQUEST_ID;

	// state
	private LoadCategories categoriesTask;
	private LoadAttributeSets atrSetsTask;
	private LoadAttributesList atrsListTask;
	private Dialog dialog;

	/* A reference to an in-ram copy of the input cache loaded from sdcard. */
	public Map<String, List<String>> inputCache;

	protected boolean isActivityAlive;
	private int loadRequestID;
	private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();

	protected ProductInfoLoader backgroundProductInfoLoader;
	
	public long mGalleryTimestamp;	
	
	// lifecycle

	/* Show a dialog informing the user that option creation failed */
	public void showNewOptionErrorDialog(String attributeName, String optionName) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Error");

		alert.setMessage("Cannot add \"" + optionName + "\" to \"" + attributeName + "\".");

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		AlertDialog srDialog = alert.create();
		alert.show();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSettings = new Settings(this);
		
		// find views
		container = (LinearLayout) findViewById(R.id.container);
		skuV = (EditText) findViewById(R.id.sku);
		barcodeInput = (EditText) findViewById(R.id.barcode_input);
		statusV = (CheckBox) findViewById(R.id.status);
		atrListWrapperV = findViewById(R.id.attr_list_wrapper);
		attributeSetV = (EditText) findViewById(R.id.attr_set);
		atrListV = (ViewGroup) findViewById(R.id.attr_list);
		// attributeSetV = (EditText) findViewById(R.id.attr_set);
		categoryV = (EditText) findViewById(R.id.category);
		categoryLabelLayoutV = (LinearLayout) findViewById(R.id.category_label_layout);
		atrListLabelV = (TextView) findViewById(R.id.attr_list_label);
		atrSetLabelV = (TextView) findViewById(R.id.atr_set_label);
		categoryLabelV = (TextView) findViewById(R.id.category_label);
		atrSetProgressV = (ProgressBar) findViewById(R.id.atr_set_progress);
		categoryProgressV = (ProgressBar) findViewById(R.id.category_progress);
		atrListProgressV = (ProgressBar) findViewById(R.id.attr_list_progress);

		layoutNewOptionPending = (LinearLayout) findViewById(R.id.layoutNewOptionPending);
		layoutSKUcheckPending = (LinearLayout) findViewById(R.id.layoutSKUcheckPending);
		
		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

		if (!ENABLE_CATEGORIES)
		{
			categoryV.setVisibility(View.GONE);
			categoryLabelLayoutV.setVisibility(View.GONE);
		}
		
		newOptionListener = new OnNewOptionTaskEventListener() {

			@Override
			public void OnAttributeCreationStarted() {
				newAttributeOptionPendingCount++;
				layoutNewOptionPending.setVisibility(View.VISIBLE);
			}

			@Override
			public void OnAttributeCreationFinished(String attributeName, String newOptionName, boolean success) {
				newAttributeOptionPendingCount--;
				if (newAttributeOptionPendingCount == 0) {
					layoutNewOptionPending.setVisibility(View.GONE);
				}

				if (success == false && isActivityAlive == true) {
					showNewOptionErrorDialog(attributeName, newOptionName);
				}
			}
		};

		if (this instanceof ProductEditActivity)
		{
			customAttributesList = new CustomAttributesList(this, atrListV, nameV, newOptionListener, true);	
		}
		else
		{
			customAttributesList = new CustomAttributesList(this, atrListV, nameV, newOptionListener, false);
		}
		
		attributeSetV.setInputType(0);

		// attach listeners
		skuV.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus == false)
				{
					if (skuV.getText().toString().length() > 0)
					{
						checkSKUExists(skuV.getText().toString());
					}
				}
			}
		});
		
		attachListenerToEditText(attributeSetV, new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!attributeSetLongTap) {
					showAttributeSetList();
				} else {
					attributeSetLongTap = false;
				}
			}
		});
		attachListenerToEditText(categoryV, new OnClickListener() {
			@Override
			public void onClick(View v) {
				showCategoryList();
			}
		});
		
		statusV.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				AbsProductActivity.this.hideKeyboard();
			}
		});

		// load data
		loadCategoriesAndAttributesSet(false);
		resHelper.registerLoadOperationObserver(this);
		isActivityAlive = true;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		resHelper.unregisterLoadOperationObserver(this);
		isActivityAlive = false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			loadCategoriesAndAttributesSet(true);
			loadAttributeList(false);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
	}

	// methods
	
	public String generateSku() {
		/* Since we can't get microsecond time in java we just use milliseconds time and add microsecond part from System.nanoTime() which
		 * doesn't return a normal timestamp but a number of nanoseconds from some arbitrary point in time which we don't know. This
		 * should be enough to make every SKU we'll ever generate different. */
		return "P" + System.currentTimeMillis() + (System.nanoTime()/1000)%1000;
	}
	
	private void checkSKUExists(String sku)
	{
		if (backgroundProductInfoLoader != null)
		{
			backgroundProductInfoLoader.cancel(false);
		}
		
		backgroundProductInfoLoader = new ProductInfoLoader(sku);
		backgroundProductInfoLoader.execute();
	}
	
	public abstract void showInvalidLabelDialog(final String settingsDomainName, final String skuDomainName);

	/* Return true if invalid label dialog was displayed and false otherwise */
	protected boolean skuScanCommon(Intent intent)
	{
		String contents = intent.getStringExtra("SCAN_RESULT");
		
		String[] urlData = contents.split("/");
		
		if (urlData.length > 0) {
			
			boolean barcodeScanned = false;
			String sku = null;
			if (ScanActivity.isLabelInTheRightFormat(contents))
			{
				sku = urlData[urlData.length - 1];
			}
			else
			{
				sku = contents;
				
				if (!ScanActivity.isSKUInTheRightFormat(sku))
					barcodeScanned = true;
			}
			
			if (barcodeScanned)
			{
				skuV.setText(generateSku());
				barcodeInput.setText(sku);
				container.requestFocus();
				
				mGalleryTimestamp = JobCacheManager.getGalleryTimestampNow();
			}
			else
			{
				mGalleryTimestamp = 0;
				
				skuV.setText(sku);
				
				if (JobCacheManager.saveRangeStart(sku, mSettings.getProfileID(), 0) == false)
				{
					ProductDetailsActivity.showTimestampRecordingError(this);
				}
				
				checkSKUExists(sku);
				skuV.requestFocus();
			}
		}
		
		boolean invalidLabelDialogShown = false;
		
		/* Check if the label is valid in relation to the url set in the settings and show appropriate
		information if it's not. */
		if (!ScanActivity.isLabelValid(this, contents))
		{
			Settings settings = new Settings(this);
			String settingsUrl = settings.getUrl();
			
			if (!ScanActivity.domainPairRemembered(ScanActivity.getDomainNameFromUrl(settingsUrl), ScanActivity.getDomainNameFromUrl(contents)))
			{
				showInvalidLabelDialog(ScanActivity.getDomainNameFromUrl(settingsUrl), ScanActivity.getDomainNameFromUrl(contents));
				invalidLabelDialogShown = true;
			}
		}
		
		return invalidLabelDialogShown;
	}
	
	public static String getProductName(AbsProductActivity apa, EditText nameEditText) {
		String name = nameEditText.getText().toString();

		// check there are any other character than spaces
		if (name.trim().length() > 0) {
			return name;
		}

		return apa.customAttributesList.getCompoundName();
	}

	private void showAttributeSetList() {
		if (isActivityAlive == false) {
			return;
		}
		List<Map<String, Object>> atrSets = getAttributeSets();
		if (atrSets == null || atrSets.isEmpty()) {
			return;
		}

		// reorganize Attribute Set List
		Map<String, Object> defaultAttrSet = null;

		int i = 1;
		for (i = 1; i < atrSets.size(); i++) {
			defaultAttrSet = atrSets.get(i);
			if (TextUtils.equals(defaultAttrSet.get(MAGEKEY_ATTRIBUTE_SET_NAME).toString(), "Default")) {
				atrSets.remove(i);
				atrSets.add(0, defaultAttrSet);
				break;
			}
		}

		final Dialog attrSetListDialog = DialogUtil.createListDialog(this, "Product types", atrSets,
				android.R.layout.simple_list_item_1, new String[] { MAGEKEY_ATTRIBUTE_SET_NAME },
				new int[] { android.R.id.text1 }, new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						final Object item = arg0.getAdapter().getItem(arg2);
						@SuppressWarnings("unchecked")
						final Map<String, Object> itemData = (Map<String, Object>) item;

						int atrSetId;
						try {
							atrSetId = Integer.parseInt(itemData.get(MAGEKEY_ATTRIBUTE_SET_ID).toString());
						} catch (Throwable e) {
							atrSetId = INVALID_ATTRIBUTE_SET_ID;
						}

						dialog.dismiss();
						
						if (AbsProductActivity.this instanceof ProductEditActivity)
						{
							customAttributesList = new CustomAttributesList(AbsProductActivity.this, atrListV, nameV,
								newOptionListener, true);
						}
						else
						{
							customAttributesList = new CustomAttributesList(AbsProductActivity.this, atrListV, nameV,
								newOptionListener, false);
						}
						
						
						selectAttributeSet(atrSetId, false, false, true);
					}
				});
		(dialog = attrSetListDialog).show();
	}
	
	public void setCategoryText(Category cat)
	{
		categoryV.setText(cat.getFullName());
	}

	protected void selectAttributeSet(final int setId, final boolean forceRefresh, boolean loadLastUsed, boolean setMatchingCategory) {
		if (setId == INVALID_ATTRIBUTE_SET_ID) {
			return;
		}

		atrSetId = setId;

		final List<Map<String, Object>> sets = getAttributeSets();
		if (sets == null) {
			return;
		}

		for (Map<String, Object> set : sets) {
			final int tmpSetId;
			try {
				tmpSetId = Integer.parseInt(set.get(MAGEKEY_ATTRIBUTE_SET_ID).toString());
			} catch (Throwable e) {
				continue;
			}
			if (tmpSetId == setId) {
				try {
					final String atrSetName = set.get(MAGEKEY_ATTRIBUTE_SET_NAME).toString();
					attributeSetV.setText(atrSetName);

					/* Set the corresponding category here as well. */
					if (setMatchingCategory == true)
					{
						final Map<String, Object> rootCategory = getCategories();
						if (rootCategory != null && !rootCategory.isEmpty()) {
							for (Category cat : Util.getCategorylist(rootCategory, null)) {
								if (cat.getName().equals(atrSetName)) {
									category = cat;
									
									setCategoryText(category);
									
									break;
								}
							}
						}							
					}

				} catch (Throwable ignored) {
				}
				break;
			}
		}
		if (loadLastUsed) {
			customAttributesList = CustomAttributesList.loadFromCache(this, atrListV, nameV, newOptionListener, mSettings.getUrl());
			atrListLabelV.setTextColor(Color.WHITE);
			showAttributeListV(false);
		} else {
			loadAttributeList(forceRefresh);
		}
	}

	private void showCategoryList() {
		if (isActivityAlive == false) {
			return;
		}

		final Map<String, Object> rootCategory = getCategories();
		if (rootCategory == null || rootCategory.isEmpty()) {
			return;
		}

		// XXX y: HUGE OVERHEAD... transforming category data in the main thread
		final Dialog categoryListDialog = DialogUtil.createCategoriesDialog(this, rootCategory,
				new OnCategorySelectListener() {
					@Override
					public boolean onCategorySelect(Category c) {
						if (c == null) {
							category = null;
							categoryV.setText("");
							categoryV.setHint("");
						} else {
							category = c;
							setCategoryText(category);
						}
						dialog.dismiss();
						return true;
					}
				}, category);
		if (categoryListDialog != null) {
			(dialog = categoryListDialog).show();
		}
	}

	// resources

	protected List<Map<String, Object>> getAttributeList() {

		if (atrSetId == INVALID_ATTRIBUTE_SET_ID)
			return null;

		if (atrsListTask == null) {
			return null;
		}

		if (atrsListTask.getData() == null) {
			return null;
		}

		List<Map<String, Object>> list = (List<Map<String, Object>>) atrsListTask.getData();

		return list;
	}

	private List<Map<String, Object>> getAttributeSets() {

		if (atrSetsTask == null) {
			return null;
		}

		if (atrSetsTask.getData() == null) {
			return null;
		}

		List<Map<String, Object>> list = atrSetsTask.getData();

		return list;
	}

	protected Map<String, Object> getCategories() {
		if (categoriesTask == null) {
			return null;
		}
		if (categoriesTask.getData() == null) {
			return null;
		}
		return categoriesTask.getData().categories;
	}

	protected void loadCategoriesAndAttributesSet(final boolean refresh) {
		
		if (ENABLE_CATEGORIES)
		{
			// categories
			if (categoriesTask != null && categoriesTask.getState() == TSTATE_RUNNING) {
			// 	there is currently running task
				if (refresh == false) {
					return;
				}
			}
			if (categoriesTask != null) {
				categoriesTask.cancel(true);
				categoriesTask.setHost(null);
				categoriesTask = null;
			}
			categoriesTask = new LoadCategories(this);
			categoriesTask.execute(refresh);
		}

		// attr sets
		if (atrSetsTask == null || atrSetsTask.getState() == TSTATE_CANCELED) {
			//
		} else {
			atrSetsTask.setHost(null);
			atrSetsTask.cancel(true);
		}
		atrSetsTask = new LoadAttributeSets();
		atrSetsTask.setHost(this);
		atrSetsTask.execute(refresh);
	}

	protected void loadAttributeList(final boolean refresh) {
		if (atrsListTask == null || atrsListTask.getState() == TSTATE_CANCELED) {
			//
		} else {
			atrsListTask.setHost(null);
			atrsListTask.cancel(true);
		}
		atrsListTask = new LoadAttributesList("" + atrSetId);
		atrsListTask.setHost(this);
		atrsListTask.execute(refresh);
	}

	protected void removeAttributeListV() {
		atrListWrapperV.setVisibility(View.GONE);
		atrListV.removeAllViews();
	}

	private void showAttributeListV(boolean showProgressBar) {
		if (showProgressBar == false && (customAttributesList.getList() == null || customAttributesList.getList().size()==0))
		{
			atrListWrapperV.setVisibility(View.GONE);	
		}
		else
		{
			atrListWrapperV.setVisibility(View.VISIBLE);
		}
		atrListProgressV.setVisibility(showProgressBar ? View.VISIBLE : View.GONE);
	}

	private static final int MAX_INPUT_CACHE_LIST_SIZE = 100;
	
	/* Helper function. Allows to add a new value to the input cache list associated with a given attribute key. */
	private void addValueToInputCacheList(String attributeKey, String value)
	{
		/* Don't store empty values in the cache. */
		if (TextUtils.isEmpty(value))
			return;
		
		List<String> list = inputCache.get(attributeKey);
		
		if (list == null)
		{
			list = new ArrayList<String>();
			inputCache.put(attributeKey, list);
		}
		
		/* Remove the value if it's already on the list. Then re-add it on the first position. */
		list.remove(value);
		list.add(0, value);
		
		/* If after addition of an element list size exceeds 100 then remove the last element. */
		if (list.size()>MAX_INPUT_CACHE_LIST_SIZE)
			list.remove(100);
	}
	
	/* Called when user creates/updates a product. This function stores all new attribute values in the cache. */
	public void updateInputCacheWithCurrentValues()
	{
		String newNameValue = nameV.getText().toString();
		String newDescriptionValue = descriptionV.getText().toString();
		
		addValueToInputCacheList(MAGEKEY_PRODUCT_NAME, newNameValue);
		addValueToInputCacheList(MAGEKEY_PRODUCT_DESCRIPTION, newDescriptionValue);
		
		if (customAttributesList !=null && customAttributesList.getList() != null)
		{
			for(CustomAttribute customAttribute : customAttributesList.getList())
			{
				if (customAttribute.isOfType(CustomAttribute.TYPE_TEXT)
					|| customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA))
				{
					addValueToInputCacheList(customAttribute.getCode(), ((EditText)customAttribute.getCorrespondingView()).getText().toString());
				}
			}
		}
		
		JobCacheManager.storeInputCache(inputCache, mSettings.getUrl());
	}
	
	// task listeners
	
	/* Called when input cache finishes loading. */
	public void onInputCacheLoaded(Map<String, List<String>> ic) {
		if (inputCache == null)
		{
			if (ic != null)
			{
				inputCache = ic;
			}
			else
			{
				inputCache = new HashMap<String, List<String>>();
			}
			
			/* Associate auto completion adapter with the "name" edit text */
			if (inputCache.get(MAGEKEY_PRODUCT_NAME) != null)
			{
				ArrayAdapter<String> nameAdapter = new ArrayAdapter<String>(this,
						android.R.layout.simple_dropdown_item_1line, inputCache.get(MAGEKEY_PRODUCT_NAME));
				nameV.setAdapter(nameAdapter);
			}
			
			/* Associate auto completion adapter with the "description" edit text */
			if (inputCache.get(MAGEKEY_PRODUCT_DESCRIPTION) != null)
			{
				ArrayAdapter<String> descriptionAdapter = new ArrayAdapter<String>(this,
						android.R.layout.simple_dropdown_item_1line, inputCache.get(MAGEKEY_PRODUCT_DESCRIPTION));
				descriptionV.setAdapter(descriptionAdapter);
			}

		}
	}

	public void onAttributeSetLoadStart() {
		atrSetLabelV.setTextColor(Color.GRAY);
		atrSetProgressV.setVisibility(View.VISIBLE);
		attributeSetV.setClickable(false);
		attributeSetV.setHint("Loading product types...");
	}

	public void onAttributeSetLoadFailure() {
		atrSetLabelV.setTextColor(Color.RED);
		atrSetProgressV.setVisibility(View.INVISIBLE);
		attributeSetV.setClickable(true);
		attributeSetV.setHint("Load failed... Check settings and refresh");
	}

	public void onAttributeSetLoadSuccess() {
		atrSetLabelV.setTextColor(Color.WHITE);
		atrSetProgressV.setVisibility(View.INVISIBLE);
		attributeSetV.setClickable(true);
		attributeSetV.setHint("Click to select an attribute set...");
	}

	public void onCategoryLoadStart() {
		categoryLabelV.setTextColor(Color.GRAY);
		categoryProgressV.setVisibility(View.VISIBLE);
		categoryV.setClickable(false);
		categoryV.setHint("Loading categories...");
	}

	public void onCategoryLoadFailure() {
		categoryLabelV.setTextColor(Color.RED);
		categoryProgressV.setVisibility(View.INVISIBLE);
		categoryV.setClickable(true);
		categoryV.setHint("Load failed... Check settings and refresh");
	}

	public void onCategoryLoadSuccess() {
		categoryLabelV.setTextColor(Color.WHITE);
		categoryProgressV.setVisibility(View.INVISIBLE);
		categoryV.setClickable(true);
		categoryV.setHint("Click to select a category...");
	}

	public void onAttributeListLoadSuccess() {
		atrListLabelV.setTextColor(Color.WHITE);
		List<Map<String, Object>> atrList = getAttributeList();

		if (atrList != null) {
			customAttributesList.loadFromAttributeList(atrList, atrSetId);

			showAttributeListV(false);
		}

		if (atrList == null || atrList.size() == 0) {
			atrListWrapperV.setVisibility(View.GONE);
		}
	}

	public void onAttributeListLoadFailure() {
		atrListLabelV.setTextColor(Color.RED);
		atrListProgressV.setVisibility(View.GONE);
	}

	public void onAttributeListLoadStart() {
		// clean the list
		atrListLabelV.setTextColor(Color.WHITE);
		removeAttributeListV();
		showAttributeListV(true);
	}

	private OnLongClickListener scanBarcodeOnClickL = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			Intent scanInt = new Intent("com.google.zxing.client.android.SCAN");
			startActivityForResult(scanInt, SCAN_BARCODE);
			return true;
		}
	};

	// helper methods

	private static void attachListenerToEditText(final EditText view, final OnClickListener onClickL) {
		view.setOnClickListener(onClickL);
	}

	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		if (isActivityAlive) {
			if (op.getOperationRequestId() == loadRequestID)
			{
				if (op.getException() == null) {
					showKnownSkuDialog(op.getResourceParams()[1]);	
				}
				else
				{
					/* Product sku was not found on the server but we still need to hide the progress indicator. */
					layoutSKUcheckPending.setVisibility(View.GONE);
				}
			}
		}
	}
	
	public void showKnownSkuDialog(final String sku) {
		layoutSKUcheckPending.setVisibility(View.GONE);
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Question");
		alert.setMessage("Known SKU. Show product details?");

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
				launchProductDetails(sku);
			}
		});
		
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				skuV.setText("");
			}
		});

		AlertDialog srDialog = alert.create();
		srDialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				skuV.setText("");
			}
		});
		srDialog.show();
	}
	
	public void showSelectProdCatDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Missing data");
		alert.setMessage("Select a product category.");

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showCategoryList();
			}
		});

		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	private void launchProductDetails(String sku)
	{
		final String ekeyProductSKU = getString(R.string.ekey_product_sku);
		final Intent intent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(ekeyProductSKU, sku);

		startActivity(intent);
	}
	
	protected class ProductInfoLoader extends AsyncTask<String, Void, Boolean> {

		private String sku;
		
		private SettingsSnapshot mSettingsSnapshot;
		
		public ProductInfoLoader(String sku)
		{
			this.sku = sku;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mSettingsSnapshot = new SettingsSnapshot(AbsProductActivity.this);
			layoutSKUcheckPending.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected Boolean doInBackground(String... args) {
			final String[] params = new String[2];
			params[0] = GET_PRODUCT_BY_SKU; // ZERO --> Use Product ID , ONE -->
											// Use Product SKU
			params[1] = this.sku;

			if (JobCacheManager.productDetailsExist(params[1], mSettingsSnapshot.getUrl())) {
				return Boolean.TRUE;
			} else {
				loadRequestID = resHelper.loadResource(AbsProductActivity.this, RES_PRODUCT_DETAILS, params, mSettingsSnapshot);
				return Boolean.FALSE;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result.booleanValue() == true) {
				if (isActivityAlive) {
					showKnownSkuDialog(this.sku);
				}
			}
		}

	}
}
