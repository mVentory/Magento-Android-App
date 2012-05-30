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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
	
	public CustomAttributesList(Context context, ViewGroup parentViewGroup)
	{
		mParentViewGroup = parentViewGroup;
		mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public void loadFromAttributeList(List<Map<String, Object>> attrList)
	{
       /* if(TextUtils.equals(code, "product_barcode_"))
        {
        	return null;
        }*/
		
		mCustomAttributeList = new ArrayList<CustomAttribute>();
		
		for( Map<String, Object> elem : attrList )
		{
			CustomAttribute customAttr = new CustomAttribute();
			
			//customAttr.setOptions(options)
			//customAttr.setSelectedValue(selectedValue)
			//customAttr.setType(type)
			//customAttr.setIsRequired(isRequired)
			//customAttr.setMainLabel(mainLabel)
			
		}
	}
	
	/*public void loadFromCache()
	{
		
	}
	
	public void saveToCache()
	{
		
	}*/
	
	private void populateViewGroup(ViewGroup vg)
	{
		if ( mCustomAttributeList != null )
		{
			
		}
	}
	
	private void showDatepickerDialog(final EditText v) {
        final OnDateSetListener onDateSetL = new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                monthOfYear += 1; // because it's from 0 to 11 for compatibility reasons
                final String date = "" + monthOfYear + "/" + dayOfMonth + "/" + year;
                v.setText(date);
            }
        };

        final Calendar c = Calendar.getInstance();

        // parse date if such is present
        try {
            final SimpleDateFormat f = new SimpleDateFormat("M/d/y");
            final Date d = f.parse(v.getText().toString());
            c.setTime(d);
        } catch (Throwable ignored) {
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        final Dialog d = new DatePickerDialog(mContext, onDateSetL, year, month, day);
        d.show();
    }
	
    private void showMultiselectDialog(CustomAttribute customAttribute)
    {
    	List<String> optionLabels = customAttribute.getOptionsLabels();
    	
        final CharSequence[] items = new CharSequence[optionLabels.size()];
        for (int i = 0; i < optionLabels.size(); i++) {
            items[i] = optionLabels.get(i);
        }
        
        String[] checkedItemsIDs = customAttribute.getSelectedValue().split(",");
        
        // say which items should be checked on start
        final boolean[] checkedItems = new boolean[customAttribute.getOptions().size()];
        for (int i = 0; i < customAttribute.getOptions().size(); i++)
        	for(int j=0; j<checkedItemsIDs.length; j++)
        	{
        		if (customAttribute.getOptions().get(i).getID().equals(checkedItemsIDs[j]))
        		{
        			checkedItems[i] = true;
        		}
        	}
    
        // create the dialog
        final Dialog dialog = new AlertDialog.Builder(mContext).setTitle("Options").setCancelable(false)
                .setMultiChoiceItems(items, checkedItems, new OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    	/* TODO: need to implement this */
                    }
                }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       /* final Set<String> selectedLabels = (Set<String>) v.getTag(R.id.tkey_atr_selected_labels);
                        if (selectedLabels != null && selectedLabels.isEmpty() == false) {
                            String s = Arrays.toString(selectedLabels.toArray());
                            v.setText(s);
                        } else {
                            v.setText("");
                        }*/
                    	/* TODO: need to implement this */
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

        	final TextView label = (TextView) v.findViewById(R.id.label);
        	label.setText(customAttribute.getMainLabel() + (customAttribute.getIsRequired() ? "*" : ""));
        	return v;
        }

        // handle text fields, multiselect (special case text field), date (another special case), null, etc...

        final View v = mInflater.inflate(R.layout.product_attribute_edit, null);
        EditText edit = (EditText) v.findViewById(R.id.edit);

        if (customAttribute.isOfType(CustomAttribute.TYPE_PRICE)) {
            edit.setInputType(EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);
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
						showMultiselectDialog(customAttribute);
						InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
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
					if (hasFocus)
					{
	                    showDatepickerDialog((EditText) v);
						InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
					}
				}
			});        	
        	
            edit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDatepickerDialog((EditText) v);
                }
            });
        }

        edit.setHint(customAttribute.getMainLabel());

        TextView label = (TextView) v.findViewById(R.id.label);
        label.setText(customAttribute.getMainLabel() + (customAttribute.getIsRequired() ? "*" : ""));
        return v;
    }
}
