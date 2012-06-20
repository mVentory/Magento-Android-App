package com.mageventory;

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
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.graphics.Color;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Category;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.model.CustomAttributesList.OnNewOptionTaskEventListener;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;
import com.mageventory.tasks.LoadAttributes;
import com.mageventory.tasks.LoadCategories;
import com.mageventory.util.DefaultOptionsMenuHelper;
import com.mageventory.util.DialogUtil;
import com.mageventory.util.DialogUtil.OnCategorySelectListener;
import com.mageventory.util.Util;

public abstract class AbsProductActivity extends Activity implements MageventoryConstants {

	public static class CategoriesData {
		public Map<String, Object> categories; // root category
	}

	// icicle keys
	// private String IKEY_CATEGORY_REQID = "category request id";
	// private String IKEY_ATTRIBUTE_SET_REQID = "attribute set request id";

	// views
	protected LayoutInflater inflater;
	protected View atrListWrapperV;
	public ViewGroup atrListV;
	protected EditText attributeSetV;
	protected EditText categoryV;
	protected TextView atrSetLabelV;
	protected TextView categoryLabelV;
	protected TextView atrListLabelV;
	protected ProgressBar atrSetProgressV;
	protected ProgressBar categoryProgressV;
	protected ProgressBar atrListProgressV;
	protected LinearLayout layoutNewOptionPending;
	public AutoCompleteTextView nameV;
	public AutoCompleteTextView descriptionV;
	protected int newAttributeOptionPendingCount;
	private OnNewOptionTaskEventListener newOptionListener;

	boolean attributeSetLongTap;

	// data
	// protected int categoryId;

	public CustomAttributesList customAttributesList;
	public int atrSetId = INVALID_ATTRIBUTE_SET_ID;
	public Category category;

	// private int attributeSetRequestId = INVALID_REQUEST_ID;
	// private int categoryRequestId = INVALID_REQUEST_ID;

	// state
	protected boolean isActive = false;
	private LoadCategories categoriesTask;
	private LoadAttributes atrsTask;
	private Dialog dialog;

	/* A reference to an in-ram copy of the input cache loaded from sdcard. */
	public Map<String, List<String>> inputCache;

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

		// find views
		atrListWrapperV = findViewById(R.id.attr_list_wrapper);
		attributeSetV = (EditText) findViewById(R.id.attr_set);
		atrListV = (ViewGroup) findViewById(R.id.attr_list);
		// attributeSetV = (EditText) findViewById(R.id.attr_set);
		categoryV = (EditText) findViewById(R.id.category);
		atrListLabelV = (TextView) findViewById(R.id.attr_list_label);
		atrSetLabelV = (TextView) findViewById(R.id.atr_set_label);
		categoryLabelV = (TextView) findViewById(R.id.category_label);
		atrSetProgressV = (ProgressBar) findViewById(R.id.atr_set_progress);
		categoryProgressV = (ProgressBar) findViewById(R.id.category_progress);
		atrListProgressV = (ProgressBar) findViewById(R.id.attr_list_progress);

		layoutNewOptionPending = (LinearLayout) findViewById(R.id.layoutNewOptionPending);

		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

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

				if (success == false && isActive == true) {
					showNewOptionErrorDialog(attributeName, newOptionName);
				}
			}
		};

		customAttributesList = new CustomAttributesList(this, atrListV, nameV, newOptionListener);

		// state
		isActive = true;

		attributeSetV.setInputType(0);

		// attach listeners
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

		// load data
		loadCategoriesAndAttributesSet(false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			loadCategoriesAndAttributesSet(true);
			return true;
		}
		return DefaultOptionsMenuHelper.onOptionsItemSelected(this, item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		isActive = true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		isActive = false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		isActive = true;
	}

	// methods

	public static String getProductName(AbsProductActivity apa, EditText nameEditText) {
		String name = nameEditText.getText().toString();

		// check there are any other character than spaces
		if (name.trim().length() > 0) {
			return name;
		}

		return apa.customAttributesList.getCompoundName();
	}

	private void showAttributeSetList() {
		if (isActive == false) {
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

		final Dialog attrSetListDialog = DialogUtil.createListDialog(this, "Attribute sets", atrSets,
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
						customAttributesList = new CustomAttributesList(AbsProductActivity.this, atrListV, nameV,
								newOptionListener);
						selectAttributeSet(atrSetId, false, false, true);
					}
				});
		(dialog = attrSetListDialog).show();
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
						if (rootCategory == null || rootCategory.isEmpty()) {
							return;
						}
						
						for (Category cat : Util.getCategorylist(rootCategory, null)) {
							if (cat.getName().equals(atrSetName)) {
								category = cat;
								categoryV.setText(cat.getFullName());
								break;
							}
						}
					}

				} catch (Throwable ignored) {
				}
				break;
			}
		}
		if (loadLastUsed) {
			customAttributesList = CustomAttributesList.loadFromCache(this, atrListV, nameV, newOptionListener);
			atrListLabelV.setTextColor(Color.WHITE);
			showAttributeListV(false);
		} else {
			loadAttributeList(forceRefresh);
		}
	}

	private void showCategoryList() {
		if (isActive == false) {
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
						} else {
							category = c;
							categoryV.setText(c.getFullName());
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

		if (atrsTask == null) {
			return null;
		}

		if (atrsTask.getData() == null) {
			return null;
		}

		List<Map<String, Object>> list = (List<Map<String, Object>>) atrsTask.getData();

		for (Map<String, Object> listElem : list) {
			String setId = (String) listElem.get("set_id");

			if (TextUtils.equals(setId, "" + atrSetId)) {

				// tmp test code

				/*
				 * ArrayList<String> ar = new ArrayList<String>();
				 * 
				 * for(int i=0; i<((List<Map<String, Object>>)
				 * listElem.get("attributes")).size(); i++) { String label =
				 * (String)(((Map<String,Object>)(((Object[])((List<Map<String,
				 * Object>>)
				 * listElem.get("attributes")).get(i).get("frontend_label"
				 * ))[0])).get("label"));
				 * 
				 * ar.add(label); }
				 */

				return (List<Map<String, Object>>) listElem.get("attributes");
			}
		}

		return null;
	}

	private List<Map<String, Object>> getAttributeSets() {

		if (atrsTask == null) {
			return null;
		}

		if (atrsTask.getData() == null) {
			return null;
		}

		List<Map<String, Object>> list = atrsTask.getData();

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
		// categories
		if (categoriesTask != null && categoriesTask.getState() == TSTATE_RUNNING) {
			// there is currently running task
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

		// attr sets
		if (atrsTask == null || atrsTask.getState() == TSTATE_CANCELED) {
			//
		} else {
			atrsTask.setHost(null);
			atrsTask.cancel(true);
		}
		atrsTask = new LoadAttributes();
		atrsTask.setHost(this);
		atrsTask.execute(refresh);
	}

	protected void loadAttributeList(final boolean refresh) {
		if (atrsTask == null || atrsTask.getState() == TSTATE_CANCELED) {
			//
		} else {
			atrsTask.setHost(null);
			atrsTask.cancel(true);
		}
		atrsTask = new LoadAttributes();
		atrsTask.setHost(this);
		atrsTask.execute(refresh);
	}

	protected void removeAttributeListV() {
		atrListWrapperV.setVisibility(View.GONE);
		atrListV.removeAllViews();
	}

	private void showAttributeListV(boolean showProgressBar) {
		atrListWrapperV.setVisibility(View.VISIBLE);
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
		
		if (customAttributesList !=null)
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
		
		JobCacheManager.storeInputCache(inputCache);
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
		attributeSetV.setHint("Loading attribute sets...");
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

}
