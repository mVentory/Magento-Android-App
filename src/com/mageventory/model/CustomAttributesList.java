package com.mageventory.model;

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

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.model.CustomAttribute.CustomAttributeOption;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class CustomAttributesList {
	private List<CustomAttribute> mCustomAttributeList;
	private ViewGroup mParentViewGroup;
	private LayoutInflater mInflater;
	private Context mContext;
	
	public List<CustomAttribute> getList()
	{
		return mCustomAttributeList;
	}
	
	public CustomAttributesList(Context context, ViewGroup parentViewGroup)
	{
		mParentViewGroup = parentViewGroup;
		mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	private CustomAttribute createCustomAttribute(Map<String, Object> map)
	{
		CustomAttribute customAttr = new CustomAttribute();
		
		customAttr.setType((String)map.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_TYPE));
		customAttr.setIsRequired(((String)map.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_REQUIRED)).equals("1")?true:false);
		customAttr.setMainLabel( (String)(((Map<String,Object>)(((Object[])map.get("frontend_label"))[0])).get("label")) );
		customAttr.setCode((String)map.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_CODE_ATTRIBUTE_LIST_REQUEST));
		customAttr.setOptionsFromServerResponse((Object [])map.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS));	
		
		if (customAttr.isOfType(CustomAttribute.TYPE_BOOLEAN) ||
			customAttr.isOfType(CustomAttribute.TYPE_SELECT) ||
			customAttr.isOfType(CustomAttribute.TYPE_DROPDOWN))
		{
			customAttr.setOptionSelected(0, true);
		}
		
		return customAttr;
	}
	
	public void loadFromAttributeList(List<Map<String, Object>> attrList)
	{
		mCustomAttributeList = new ArrayList<CustomAttribute>();
		
        Map<String,Object> thumbnail = null;
		
		for( Map<String, Object> elem : attrList )
		{
			if(TextUtils.equals((String)elem.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_CODE_ATTRIBUTE_LIST_REQUEST), "product_barcode_"))
			{
				continue;
			}
			
			if(((String)elem.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_CODE_ATTRIBUTE_LIST_REQUEST)).contains("_thumb"))
			{
				thumbnail = elem;
				continue;
			}
			mCustomAttributeList.add(createCustomAttribute(elem));
		}
		
		if (thumbnail != null)
		{
			mCustomAttributeList.add(createCustomAttribute(thumbnail));
		}
		
		populateViewGroup();
	}
	
	/*public void loadFromCache()
	{
		
	}
	
	public void saveToCache()
	{
		
	}*/
	
	private void populateViewGroup()
	{
		mParentViewGroup.removeAllViews();
		
		for(CustomAttribute elem : mCustomAttributeList)
		{
			View v = newAtrEditView(elem);
			
			if (v != null)
			{
				mParentViewGroup.addView(v);
			}
		}
	}
	
	private void showDatepickerDialog(final CustomAttribute customAttribute) {
        final OnDateSetListener onDateSetL = new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                monthOfYear += 1; // because it's from 0 to 11 for compatibility reasons
                final String date = "" + monthOfYear + "/" + dayOfMonth + "/" + year;

                customAttribute.setSelectedValue(date, true);
            }
        };

        final Calendar c = Calendar.getInstance();

        // parse date if such is present
        try {
            final SimpleDateFormat f = new SimpleDateFormat("M/d/y");
            final Date d = f.parse(((EditText)customAttribute.getCorrespondingView()).getText().toString());
            c.setTime(d);
        } catch (Throwable ignored) {
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        final Dialog d = new DatePickerDialog(mContext, onDateSetL, year, month, day);
        d.show();
    }
	
    private void showMultiselectDialog(final CustomAttribute customAttribute)
    {
    	List<String> optionLabels = customAttribute.getOptionsLabels();
    	
        final CharSequence[] items = new CharSequence[optionLabels.size()];
        for (int i = 0; i < optionLabels.size(); i++) {
            items[i] = optionLabels.get(i);
        }
        
        // say which items should be checked on start
        final boolean[] checkedItems = new boolean[customAttribute.getOptions().size()];
        for (int i = 0; i < customAttribute.getOptions().size(); i++)
        {
        	checkedItems[i] = customAttribute.getOptions().get(i).getSelected();
        }
    
        // create the dialog
        final Dialog dialog = new AlertDialog.Builder(mContext).setTitle("Options").setCancelable(false)
                .setMultiChoiceItems(items, checkedItems, new OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    	customAttribute.setOptionSelected(which, isChecked);
                    }
                }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    	((EditText)customAttribute.getCorrespondingView()).setText(customAttribute.getUserReadableSelectedValue());
                    }
                }).create();
        dialog.show();
    }
	
    private View newAtrEditView(final CustomAttribute customAttribute) {
        		
        // y: actually the "dropdown" type is just a "select" type, but it's added here for clarity
        if (customAttribute.isOfType(CustomAttribute.TYPE_BOOLEAN) ||
        	customAttribute.isOfType(CustomAttribute.TYPE_SELECT) ||
        	customAttribute.isOfType(CustomAttribute.TYPE_DROPDOWN))
        {
        	if (customAttribute.getOptions() == null || customAttribute.getOptions().isEmpty())
        	{
        		return null;
        	}

            // handle boolean and select fields
        	final View v = mInflater.inflate(R.layout.product_attribute_spinner, null);
        	final Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
        	customAttribute.setCorrespondingView(spinner);
        	final ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
        		android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, customAttribute.getOptionsLabels());
        	spinner.setAdapter(adapter);
                
        	spinner.setFocusableInTouchMode(true);
                
        	spinner.setOnFocusChangeListener(new OnFocusChangeListener() {
					
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus)
					{
						spinner.performClick();
						InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
					}
				}
			});
        	
        	spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					customAttribute.setOptionSelected(position, true);
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					// TODO Auto-generated method stub
					
				}
			});

        	final TextView label = (TextView) v.findViewById(R.id.label);
        	label.setText(customAttribute.getMainLabel() + (customAttribute.getIsRequired() ? "*" : ""));
        	return v;
        }

        // handle text fields, multiselect (special case text field), date (another special case), null, etc...

        final View v = mInflater.inflate(R.layout.product_attribute_edit, null);
        final EditText edit = (EditText) v.findViewById(R.id.edit);
    	customAttribute.setCorrespondingView(edit);

        if (customAttribute.isOfType(CustomAttribute.TYPE_PRICE)) {
            edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        } else if (customAttribute.isOfType(CustomAttribute.TYPE_MULTISELECT)) {
        	
        	if (customAttribute.getOptions() == null || customAttribute.getOptions().isEmpty())
        	{
        		return null;
        	}
                
        	edit.setInputType(0);
        	edit.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus)
					{
						showMultiselectDialog( customAttribute);
						InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
					}
				}
			});
                
        	edit.setOnClickListener(new OnClickListener() {
        		@Override
        		public void onClick(View v) {
        			showMultiselectDialog( customAttribute);
        		}
        	});
        } else if (customAttribute.isOfType(CustomAttribute.TYPE_DATE)) {
            edit.setInputType(0);
            edit.setOnFocusChangeListener(new OnFocusChangeListener() {
				
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus)
					{
	                    showDatepickerDialog(customAttribute);
						InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
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
        
        if (customAttribute.isOfType(CustomAttribute.TYPE_PRICE) ||
        	customAttribute.isOfType(CustomAttribute.TYPE_TEXT))
        {
        	edit.addTextChangedListener(new TextWatcher() {
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count,
						int after) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void afterTextChanged(Editable s) {
					customAttribute.setSelectedValue(edit.getText().toString(), false);
				}
			});
        }

        edit.setHint(customAttribute.getMainLabel());

        TextView label = (TextView) v.findViewById(R.id.label);
        label.setText(customAttribute.getMainLabel() + (customAttribute.getIsRequired() ? "*" : ""));
        return v;
    }
}
