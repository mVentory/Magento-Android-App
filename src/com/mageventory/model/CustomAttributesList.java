package com.mageventory.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.text.AttributedCharacterIterator.Attribute;
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

import javax.crypto.Mac;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.AbsProductActivity;
import com.mageventory.activity.ProductCreateActivity;
import com.mageventory.activity.ProductDetailsActivity;
import com.mageventory.dialogs.CustomAttributeValueSelectionDialog;
import com.mageventory.dialogs.CustomAttributeValueSelectionDialog.OnCheckedListener;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.jobprocessor.CreateProductProcessor;
import com.mageventory.model.CustomAttribute.CustomAttributeOption;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceConstants;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.settings.Settings;
import com.mageventory.tasks.CreateOptionTask;
import com.mageventory.util.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class CustomAttributesList implements Serializable, MageventoryConstants {
	private static final long serialVersionUID = -6409197154564216767L;

	private List<CustomAttribute> mCustomAttributeList;
	private String mCompoundNameFormatting;
	private int mSetID;

	/* Things we don't serialize */
	private ViewGroup mParentViewGroup;
	private LayoutInflater mInflater;
	private AbsProductActivity mActivity;
	private EditText mName;
	private OnNewOptionTaskEventListener mNewOptionListener;
	private Settings mSettings;
	private boolean mProductEdit;

	public List<CustomAttribute> getList() {
		return mCustomAttributeList;
	}

	public CustomAttributesList(AbsProductActivity activity, ViewGroup parentViewGroup, EditText nameView,
			OnNewOptionTaskEventListener listener, boolean productEdit) {
		mParentViewGroup = parentViewGroup;
		mActivity = activity;
		mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mName = nameView;
		mNewOptionListener = listener;
		setNameHint();
		mSettings = new Settings(activity);
		mProductEdit = productEdit;
	}

	/*
	 * Save the list of custom attributes in the cache for the user to be able
	 * to retrieve it later. We are setting some UI stuff to null here and they
	 * will be reset when the list is reloaded from the cache.
	 */
	public void saveInCache() {
		String url = mSettings.getUrl();
		
		/* Don't want to serialize this */
		mParentViewGroup = null;
		mInflater = null;
		mActivity = null;
		mName = null;
		mNewOptionListener = null;
		mSettings = null;

		if (mCustomAttributeList != null)
		{
			for (CustomAttribute elem : mCustomAttributeList) {
				/* Don't want to serialize this */
				elem.setCorrespondingView(null);
				elem.setNewOptionSpinningWheel(null);
			}
		}
		JobCacheManager.storeLastUsedCustomAttribs(this, url);
	}

	/*
	 * This is used when user wants to load last used custom attribute list (we
	 * have it stored in the cache so we have to load it).
	 */
	public static CustomAttributesList loadFromCache(AbsProductActivity activity, ViewGroup parentViewGroup, EditText nameView,
			OnNewOptionTaskEventListener listener, String url) {
		CustomAttributesList c = JobCacheManager.restoreLastUsedCustomAttribs(url);

		if (c != null) {
			c.mParentViewGroup = parentViewGroup;
			c.mActivity = activity;
			c.mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			c.mName = nameView;
			c.mNewOptionListener = listener;
			c.populateViewGroup();
			c.mSettings = new Settings(activity);
		}

		return c;
	}

	/* In case the user refreshes a list of custom attributes a new list of attributes is created. The problem is
	 * we want to preserve the input from the previous list. This function is called for each corresponding pair
	 * of attributes from old and new list and tries to copy the user input from old attribute to the new one. */
	private void restoreAttributeValue(CustomAttribute newAttribute, CustomAttribute oldAttribute)
	{
		/* If the new attribute we're creating is of any of these types this means it has options and
		 * we're going to try to reset them as they were in the old attribute. */
		if (newAttribute.isOfType(CustomAttribute.TYPE_BOOLEAN)
				|| newAttribute.isOfType(CustomAttribute.TYPE_SELECT)
				|| newAttribute.isOfType(CustomAttribute.TYPE_DROPDOWN)
				|| newAttribute.isOfType(CustomAttribute.TYPE_MULTISELECT)) {
			
			/* Iterate over the options from the old attribute and try to find the selected ones. */
			for (CustomAttributeOption option : oldAttribute.getOptions()) {
				if (option.getSelected() == true) {
					/* We found a selected option from the old attribute. We need to find a corresponding
					 * option from the new attribute now that corresponds to the selected one. */
					for (CustomAttributeOption optionNew : newAttribute.getOptions()) {
						/* If the ids of both options are equal this means it's the same attribute option
						 * and we can set the option selected in the new attribute which means user input
						 * will be preserved. */
						if (optionNew.getID().equals(option.getID())) {
							optionNew.setSelected(true);
						}
					}
				}
			}
		} else {
			/* This is an attribute without the options. It is just enough to copy the selected
			 * value string in this case. */
			newAttribute.setSelectedValue(oldAttribute.getSelectedValue(), false);
		}
	}
	
	/*
	 * Convert a custom attribute data from the server to a more friendly piece
	 * of data (an instance of CustomAttribute class)
	 */
	private CustomAttribute createCustomAttribute(Map<String, Object> map, List<CustomAttribute> listCopy) {
		CustomAttribute customAttr = new CustomAttribute();

		customAttr.setType((String) map.get(MAGEKEY_ATTRIBUTE_TYPE));
		customAttr.setIsRequired(((String) map.get(MAGEKEY_ATTRIBUTE_REQUIRED)).equals("1") ? true : false);
		customAttr.setMainLabel((String) (((Map<String, Object>) (((Object[]) map.get("frontend_label"))[0]))
				.get("label")));
		customAttr.setCode((String) map.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE));
		customAttr.setOptionsFromServerResponse((Object[]) map.get(MAGEKEY_ATTRIBUTE_OPTIONS));
		customAttr.setAttributeID((String) map.get(MAGEKEY_ATTRIBUTE_ID));

		if (customAttr.isOfType(CustomAttribute.TYPE_BOOLEAN) || customAttr.isOfType(CustomAttribute.TYPE_SELECT)
				|| customAttr.isOfType(CustomAttribute.TYPE_DROPDOWN)) {
			customAttr.setOptionSelected(0, true, false);
		}

		/* If we're just refreshing attributes - try to keep user entered data. */
		if (listCopy != null) {
			/* Find the attribute in the previous list that corresponds to the attribute we are creating now. */
			for (CustomAttribute elem : listCopy) {
				/* If they have the same id and the same code they are the same attribute. */
				if (elem.getAttributeID().equals(customAttr.getAttributeID())
						&& elem.getType().equals(customAttr.getType()) && elem.getCode().equals(customAttr.getCode())) {
					
					restoreAttributeValue(customAttr, elem);
					break;
				}
			}
		} else {
			String defaultValue = (String) map.get(MAGEKEY_ATTRIBUTE_DEFAULT_VALUE);
			
			/* See
			 * http://code.google.com/p/mageventory/issues/detail?id=277
			 * for explanation of this if statement. */
			if (defaultValue != null && defaultValue.length()>0 && defaultValue.charAt(0)!='~')
			{
				customAttr.setSelectedValue(defaultValue, false);	
			}
		}

		return customAttr;
	}

	/*
	 * Copy all serializable data (which in this case is everything except View
	 * classes from one CustomAttribute to another)
	 */
	private void copySerializableData(CustomAttribute from, CustomAttribute to) {
		to.setType(from.getType());
		to.setIsRequired(from.getIsRequired());
		to.setMainLabel(from.getMainLabel());
		to.setCode(from.getCode());
		to.setOptions(from.getOptions());
		to.setAttributeID(from.getAttributeID());
	}

	/*
	 * Update a single custom attribute's options with new data from the cache.
	 * Also make this option selected + update the for user to see the changes.
	 */
	public void updateCustomAttributeOptions(CustomAttribute attr, List<Map<String, Object>> customAttrsList,
			String newOptionToSet) {

		if (customAttrsList == null)
			return;

		for (Map<String, Object> elem : customAttrsList) {
			if (TextUtils.equals((String) elem.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE), attr.getCode())) {

				CustomAttribute updatedAttrib = createCustomAttribute(elem, mCustomAttributeList);
				copySerializableData(updatedAttrib, attr);

				int i = 0;
				for (CustomAttributeOption option : attr.getOptions()) {
					if (TextUtils.equals(option.getLabel(), newOptionToSet)) {
						attr.setOptionSelected(i, true, true);

						break;
					}
					i++;
				}

				break;
			}
		}
	}

	/*
	 * Convert server response to a format more friendly to a programmer. All
	 * other operations on custom attributes will be performed on that format
	 * and not directly on the list of maps from the server.
	 */
	public void loadFromAttributeList(List<Map<String, Object>> attrList, int setID) {
		mSetID = setID;

		List<CustomAttribute> customAttributeListCopy = null;

		if (mCustomAttributeList != null) {
			customAttributeListCopy = mCustomAttributeList;
		}

		mCustomAttributeList = new ArrayList<CustomAttribute>();

		Map<String, Object> thumbnail = null;

		for (Map<String, Object> elem : attrList) {
			if (TextUtils.equals((String) elem.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE), "product_barcode_")) {
				continue;
			}

			if (((String) elem.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE)).contains("_thumb")) {
				thumbnail = elem;
				continue;
			}

			Boolean isFormatting = (Boolean) elem.get(MAGEKEY_ATTRIBUTE_IS_FORMATTING_ATTRIBUTE);

			if (isFormatting != null && isFormatting.booleanValue() == true) {
				mCompoundNameFormatting = (String) elem.get(MAGEKEY_ATTRIBUTE_DEFAULT_VALUE);
			} else {
				mCustomAttributeList.add(createCustomAttribute(elem, customAttributeListCopy));
			}
		}

		if (thumbnail != null) {
			mCustomAttributeList.add(createCustomAttribute(thumbnail, customAttributeListCopy));
		}

		populateViewGroup();
	}

	/*
	 * Some codes are not needed in compound name in case the values selected by
	 * the user are not very meaningful like "none", "Other", etc.
	 */
	private boolean isNeededInCompoundName(String value) {
		if (value == null || value.equals("") || value.equalsIgnoreCase("other") || value.equalsIgnoreCase("none")) {
			return false;
		}

		return true;
	}

	/*
	 * Server can return formatting string for example in the following format:
	 * code1, code2, (code3). We may need to remove some of these codes while
	 * still keeping the convention used to build the formatting string. This
	 * function removes a given code but also removes the space that precedes it
	 * and a comma that follows it. It also removes parentheses if they are
	 * present for a given code.
	 */
	private String removeCodeFromCompoundName(String compoundName, String code) {
		int indexBegin = compoundName.indexOf(code);
		int indexEnd;

		if (indexBegin != -1) {
			indexEnd = indexBegin + code.length();

			if (indexBegin > 0) {
				if (compoundName.charAt(indexBegin - 1) == ' ' || compoundName.charAt(indexBegin - 1) == '(') {
					indexBegin--;
				}
			}

			if (indexEnd < compoundName.length()) {
				if (compoundName.charAt(indexEnd) == ',' || compoundName.charAt(indexEnd) == ')') {
					indexEnd++;
				}
			}
			compoundName = compoundName.replace(compoundName.substring(indexBegin, indexEnd), "");
		}

		return compoundName;
	}

	/*
	 * Get compound name based on the formatting string received from the
	 * server. If the constructed compound name turns out to be empty - return
	 * "n/a" instead.
	 */
	public String getCompoundName() {
		String out = null;
		if (mCompoundNameFormatting != null) {
			out = mCompoundNameFormatting;

			for (CustomAttribute ca : mCustomAttributeList) {
				String selectedValue = ca.getUserReadableSelectedValue();

				/*
				 * Check if a given attribute is needed in coumpound name. It
				 * may not be needed because it is empty, contains "Other",
				 * "none", etc.
				 */
				if (isNeededInCompoundName(selectedValue)) {
					out = out.replace(ca.getCode(), ca.getUserReadableSelectedValue());
				} else {
					out = removeCodeFromCompoundName(out, ca.getCode());
				}
			}

			/*
			 * Make sure compound name doesn't contain whitespace characters at
			 * the beginning and the end. If there is a comma at the end -
			 * remove it.
			 */
			out = out.trim();
			if (out.length() > 0 && out.charAt(out.length() - 1) == ',') {
				out = out.substring(0, out.length() - 1);
			}
			out = out.trim();
		}

		if (out != null && out.length() > 0) {
			return out;
		} else {
			return "n-a";
		}

	}

	/*
	 * Get the formatting string we received from the server and replace
	 * attribute codes with labels.
	 */
	public String getUserReadableFormattingString() {
		String out = null;
		if (mCompoundNameFormatting != null) {
			out = mCompoundNameFormatting;

			for (CustomAttribute ca : mCustomAttributeList) {
				out = out.replace(ca.getCode(), ca.getMainLabel());
			}
		}

		return out;
	}

	/*
	 * After we constructed a list of attributes in memory we have to construct
	 * a list of views corresponding to these attriubtes. This function
	 * constructs those views.
	 */
	private void populateViewGroup() {
		mParentViewGroup.removeAllViews();

		for (CustomAttribute elem : mCustomAttributeList) {
			View v = newAtrEditView(elem);

			if (v != null) {
				mParentViewGroup.addView(v);
			}
		}
		setNameHint();
	}

	private void setNameHint() {
		if (mName != null)
			mName.setHint(getCompoundName());
	}

	/*
	 * A listener which contains functions which are called when a new option
	 * starts being created or when it gets created.
	 */
	public static interface OnNewOptionTaskEventListener {
		void OnAttributeCreationStarted();

		void OnAttributeCreationFinished(String attributeName, String newOptionName, boolean success);
	}

	/* Shows a dialog for adding new option. */
	public void showAddNewOptionDialog(final CustomAttribute customAttribute) {
		final View textEntryView = mActivity.getLayoutInflater().inflate(R.layout.add_new_option_dialog, null);

		/*
		 * User is not able to create a new option if one is currently being
		 * created. I'm just checking if the spinning wheel is shown so that I
		 * don't have to add another variable for tracking that.
		 */
		if (customAttribute.getNewOptionSpinningWheel().getVisibility() == View.VISIBLE) {
			return;
		}

		AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);

		alert.setTitle("New option");
		alert.setMessage("Enter a name for a new option for \"" + customAttribute.getMainLabel() + "\" attribute.");
		alert.setView(textEntryView);

		final EditText editText = (EditText) textEntryView.findViewById(R.id.newOptionEditText);

		alert.setPositiveButton("Create", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

				customAttribute.addNewOption(mActivity, editText.getText().toString());

				CreateOptionTask createOptionTask = new CreateOptionTask(mActivity, customAttribute,
						CustomAttributesList.this, editText.getText().toString(), "" + mSetID, mNewOptionListener);
				createOptionTask.execute();
			}
		});

		AlertDialog srDialog = alert.create();
		alert.show();
	}

	private void showDatepickerDialog(final CustomAttribute customAttribute) {
		final OnDateSetListener onDateSetL = new OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				monthOfYear += 1; // because it's from 0 to 11 for compatibility
									// reasons
				final String date = "" + monthOfYear + "/" + dayOfMonth + "/" + year;

				customAttribute.setSelectedValue(date, true);
				setNameHint();
			}
		};

		final Calendar c = Calendar.getInstance();

		// parse date if such is present
		try {
			final SimpleDateFormat f = new SimpleDateFormat("M/d/y");
			final Date d = f.parse(((EditText) customAttribute.getCorrespondingView()).getText().toString());
			c.setTime(d);
		} catch (Throwable ignored) {
		}

		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);

		final Dialog d = new DatePickerDialog(mActivity, onDateSetL, year, month, day);
		d.show();
	}

	private void showSingleSelectDialog(final CustomAttribute customAttribute) {
		List<String> optionLabels = customAttribute.getOptionsLabels();

		final String[] items = new String[optionLabels.size()];
		for (int i = 0; i < optionLabels.size(); i++) {
			items[i] = optionLabels.get(i);
		}

		// say which items should be checked on start
		int selectedItemIdx = 0;
		for (int i = 0; i < customAttribute.getOptions().size(); i++) {
			if (customAttribute.getOptions().get(i).getSelected())
			{
				selectedItemIdx = i;
			}
		}
	
		CustomAttributeValueSelectionDialog dialog = new CustomAttributeValueSelectionDialog(mActivity);
		dialog.initSingleSelectDialog(items, selectedItemIdx);
		
		dialog.setOnCheckedListener(new OnCheckedListener() {
			
			@Override
			public void onChecked(int position, boolean checked) {
				
				if (checked == true)
				{
					customAttribute.setOptionSelected(position, checked, false);
				}
			}
		});
		
		dialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				((EditText) customAttribute.getCorrespondingView()).setText(customAttribute
						.getUserReadableSelectedValue());
				setNameHint();
			}
		});
		
		dialog.show();
	}
	
	private void showMultiselectDialog(final CustomAttribute customAttribute) {
		List<String> optionLabels = customAttribute.getOptionsLabels();

		final String[] items = new String[optionLabels.size()];
		for (int i = 0; i < optionLabels.size(); i++) {
			items[i] = optionLabels.get(i);
		}

		// say which items should be checked on start
		final boolean[] checkedItems = new boolean[customAttribute.getOptions().size()];
		for (int i = 0; i < customAttribute.getOptions().size(); i++) {
			checkedItems[i] = customAttribute.getOptions().get(i).getSelected();
		}
	
		CustomAttributeValueSelectionDialog dialog = new CustomAttributeValueSelectionDialog(mActivity);
		dialog.initMultiSelectDialog(items, checkedItems);
		
		dialog.setOnCheckedListener(new OnCheckedListener() {
			
			@Override
			public void onChecked(int position, boolean checked) {
				customAttribute.setOptionSelected(position, checked, false);
			}
		});
		
		dialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				((EditText) customAttribute.getCorrespondingView()).setText(customAttribute
						.getUserReadableSelectedValue());
				setNameHint();
			}
		});
		
		dialog.show();
	}

	/*
	 * Create a view corresponding to the custom attribute in order to show it
	 * to the user.
	 */
	private View newAtrEditView(final CustomAttribute customAttribute) {

		final View v = mInflater.inflate(R.layout.product_attribute_edit, null);
		final EditText edit = (EditText) v.findViewById(R.id.edit);
		customAttribute.setCorrespondingView(edit);
		customAttribute.setNewOptionSpinningWheel(v.findViewById(R.id.new_option_spinning_wheel));
		edit.setText(customAttribute.getUserReadableSelectedValue());
		
		if (customAttribute.isOfType(CustomAttribute.TYPE_BOOLEAN)
				|| customAttribute.isOfType(CustomAttribute.TYPE_SELECT)
				|| customAttribute.isOfType(CustomAttribute.TYPE_DROPDOWN)) {
			if (customAttribute.getOptions() == null || customAttribute.getOptions().isEmpty()) {
				return null;
			}
			
			edit.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					showAddNewOptionDialog(customAttribute);

					return true;
				}
			});
			
			edit.setInputType(0);
			edit.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						showSingleSelectDialog(customAttribute);
						InputMethodManager imm = (InputMethodManager) mActivity
								.getSystemService(Activity.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
					}
				}
			});

			edit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showSingleSelectDialog(customAttribute);
				}
			});
		} else if (customAttribute.isOfType(CustomAttribute.TYPE_PRICE)) {
			edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		} else if (customAttribute.isOfType(CustomAttribute.TYPE_MULTISELECT)) {

			if (customAttribute.getOptions() == null || customAttribute.getOptions().isEmpty()) {
				return null;
			}

			edit.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					showAddNewOptionDialog(customAttribute);

					return true;
				}
			});

			edit.setInputType(0);
			edit.setSingleLine(false);
			
			edit.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						showMultiselectDialog(customAttribute);
						InputMethodManager imm = (InputMethodManager) mActivity
								.getSystemService(Activity.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
					}
				}
			});

			edit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showMultiselectDialog(customAttribute);
				}
			});

		} else if (customAttribute.isOfType(CustomAttribute.TYPE_DATE)) {
			edit.setInputType(0);
			edit.setOnFocusChangeListener(new OnFocusChangeListener() {

				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						showDatepickerDialog(customAttribute);
						InputMethodManager imm = (InputMethodManager) mActivity
								.getSystemService(Activity.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
					}
				}
			});

			edit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showDatepickerDialog(customAttribute);
				}
			});
		}

		if (customAttribute.isOfType(CustomAttribute.TYPE_PRICE) || customAttribute.isOfType(CustomAttribute.TYPE_TEXT)
				|| customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA)) {

			OnEditorActionListener nextButtonBehaviour = new OnEditorActionListener() {
				
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_NEXT) {
						
						InputMethodManager imm = (InputMethodManager)mActivity.getSystemService(
							Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
							
			            return true;
			        }
					
					return false;
				}
			};
			
			edit.setOnEditorActionListener(nextButtonBehaviour);
			
			edit.setSelectAllOnFocus(true);
			
			edit.addTextChangedListener(new TextWatcher() {
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					// TODO Auto-generated method stub

				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					// TODO Auto-generated method stub

				}

				@Override
				public void afterTextChanged(Editable s) {
					customAttribute.setSelectedValue(edit.getText().toString(), false);
					setNameHint();
				}
			});

			if (customAttribute.isOfType(CustomAttribute.TYPE_TEXT)) {
				edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
			} else if (customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA)) {
				edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
						| InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
			}
		}

		/* Set the auto completion adapter for text and textarea fields. */
		if (customAttribute.isOfType(CustomAttribute.TYPE_TEXT)
				|| customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA)) {
			if (mActivity.inputCache.get(customAttribute.getCode()) != null)
			{
				AutoCompleteTextView autoEdit = (AutoCompleteTextView)edit;
			
				ArrayAdapter<String> nameAdapter = new ArrayAdapter<String>(mActivity,
					android.R.layout.simple_dropdown_item_1line, mActivity.inputCache.get(customAttribute.getCode()));
				autoEdit.setAdapter(nameAdapter);
			}			
		}

		TextView label = (TextView) v.findViewById(R.id.label);
		label.setText(customAttribute.getMainLabel() + (customAttribute.getIsRequired() ? "*" : ""));
		return v;
	}
}
