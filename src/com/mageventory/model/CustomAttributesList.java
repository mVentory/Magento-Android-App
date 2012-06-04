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

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.job.JobCacheManager;
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

public class CustomAttributesList implements Serializable {
	private static final long serialVersionUID = -6409197154564216767L;
	
	private List<CustomAttribute> mCustomAttributeList;
	private ViewGroup mParentViewGroup;
	private LayoutInflater mInflater;
	private Context mContext;
	private String mCompoundNameFormatting;
	private EditText mName;
	
	public List<CustomAttribute> getList()
	{
		return mCustomAttributeList;
	}
	
	public CustomAttributesList(Context context, ViewGroup parentViewGroup, EditText nameView)
	{
		mParentViewGroup = parentViewGroup;
		mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mName = nameView;
	}
	
	public void saveInCache()
	{
		/* Don't want to serialize this */
		mParentViewGroup = null;
		mInflater = null;
		mContext = null;
		mName = null;
		
		for(CustomAttribute elem : mCustomAttributeList)
		{
			/* Don't want to serialize this */
			elem.setCorrespondingView(null);
		}
		JobCacheManager.storeLastUsedCustomAttribs(this);
	}
	
	public static CustomAttributesList loadFromCache(Context context, ViewGroup parentViewGroup, EditText nameView)
	{
		CustomAttributesList c = JobCacheManager.restoreLastUsedCustomAttribs();
		
		if (c != null)
		{
			c.mParentViewGroup = parentViewGroup;
			c.mContext = context;
			c.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			c.mName = nameView;
			c.populateViewGroup();
		}
		
		return c;
	}
	
	private CustomAttribute createCustomAttribute(Map<String, Object> map, List<CustomAttribute> listCopy)
	{
		CustomAttribute customAttr = new CustomAttribute();
		
		customAttr.setType((String)map.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_TYPE));
		customAttr.setIsRequired(((String)map.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_REQUIRED)).equals("1")?true:false);
		customAttr.setMainLabel( (String)(((Map<String,Object>)(((Object[])map.get("frontend_label"))[0])).get("label")) );
		customAttr.setCode((String)map.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_CODE_ATTRIBUTE_LIST_REQUEST));
		customAttr.setOptionsFromServerResponse((Object [])map.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS));	
		customAttr.setAttributeID((String)map.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_ID));

		if (customAttr.isOfType(CustomAttribute.TYPE_BOOLEAN) ||
			customAttr.isOfType(CustomAttribute.TYPE_SELECT) ||
			customAttr.isOfType(CustomAttribute.TYPE_DROPDOWN))
		{
			customAttr.setOptionSelected(0, true);
		}
		
		/* If we're just refreshing attributes - try to keep user entered data. */
		if (listCopy != null)
		{
			for(CustomAttribute elem : listCopy)
			{
				if (elem.getAttributeID().equals(customAttr.getAttributeID()) &&
					elem.getType().equals(customAttr.getType()) &&
					elem.getCode().equals(customAttr.getCode()))
				{
					//we have a match
					if (customAttr.isOfType(CustomAttribute.TYPE_BOOLEAN) ||
						customAttr.isOfType(CustomAttribute.TYPE_SELECT) ||
						customAttr.isOfType(CustomAttribute.TYPE_DROPDOWN) ||
						customAttr.isOfType(CustomAttribute.TYPE_MULTISELECT))
					{
						// restore options
						for(CustomAttributeOption option : elem.getOptions())
						{
							if (option.getSelected() == true)
							{
								for(CustomAttributeOption optionNew : customAttr.getOptions())
								{
									if (optionNew.getID().equals(option.getID()))
									{
										optionNew.setSelected(true);
									}
								}
							}
						}
					}
					else
					{
						customAttr.setSelectedValue(elem.getSelectedValue(), false);
					}
					
					break;
				}
			}
		}
		else
		{
			customAttr.setSelectedValue((String)map.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_DEFAULT_VALUE), false);
		}
		
		return customAttr;
	}
	
	public void loadFromAttributeList(List<Map<String, Object>> attrList)
	{
		List<CustomAttribute> customAttributeListCopy = null;

		if (mCustomAttributeList != null)
		{
			customAttributeListCopy = mCustomAttributeList;
		}
			
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
			
			Boolean isFormatting = (Boolean)elem.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_IS_FORMATTING_ATTRIBUTE);
			
			if (isFormatting!=null && isFormatting.booleanValue()==true)
			{
				mCompoundNameFormatting = (String)elem.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_DEFAULT_VALUE);
			}
			else
			{
				mCustomAttributeList.add(createCustomAttribute(elem, customAttributeListCopy));	
			}
		}
		
		if (thumbnail != null)
		{
			mCustomAttributeList.add(createCustomAttribute(thumbnail, customAttributeListCopy));
		}
		
		populateViewGroup();
	}
	
	private boolean isNeededInCompoundName(String value)
	{
		if (value == null ||
			value.equals("") ||
			value.equalsIgnoreCase("other") ||
			value.equalsIgnoreCase("none"))
		{
			return false;
		}
		
		return true;
	}
	
	private String removeCodeFromCompoundName(String compoundName, String code)
	{
		int indexBegin = compoundName.indexOf(code);
		int indexEnd;
		
		if (indexBegin != -1)
		{
			indexEnd = indexBegin + code.length();
			
			if (indexBegin > 0)
			{
				if (compoundName.charAt(indexBegin-1) == ' ' || compoundName.charAt(indexBegin-1) == '(')
				{
					indexBegin--;
				}
			}
			
			if (indexEnd < compoundName.length())
			{
				if (compoundName.charAt(indexEnd) == ',' || compoundName.charAt(indexEnd) == ')')
				{
					indexEnd++;
				}
			}
			compoundName = compoundName.replace(compoundName.substring(indexBegin, indexEnd), "");
		}
		
		return compoundName;
	}
	
	public String getCompoundName()
	{
		String out = null;
		if (mCompoundNameFormatting!=null)
		{
			out = mCompoundNameFormatting;
			
			for(CustomAttribute ca : mCustomAttributeList)
			{
				String selectedValue = ca.getUserReadableSelectedValue();
				
				if (isNeededInCompoundName(selectedValue))
				{
					out = out.replace(ca.getCode(), ca.getUserReadableSelectedValue());
				}
				else
				{
					out = removeCodeFromCompoundName(out, ca.getCode());
				}
			}
			out = out.trim();
			if (out.length()>0 && out.charAt(out.length()-1) == ',')
			{
				out = out.substring(0, out.length()-1);
			}
			out = out.trim();
		}
		
		if (out != null && out.length() > 0)
    	{
    		return out;
    	}
    	else
    	{
    		return "n/a";
    	}
	
	}
	
	public String getUserReadableFormattingString()
	{
		String out = null;
		if (mCompoundNameFormatting!=null)
		{
			out = mCompoundNameFormatting;
			
			for(CustomAttribute ca : mCustomAttributeList)
			{
				out = out.replace(ca.getCode(), ca.getMainLabel());
			}
		}
		
		return out;		
	}
	
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
		setNameHint();
	}
	
	private void setNameHint()
	{
		if (mName != null)
			mName.setHint(getCompoundName());
	}
	
	private void showDatepickerDialog(final CustomAttribute customAttribute) {
        final OnDateSetListener onDateSetL = new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                monthOfYear += 1; // because it's from 0 to 11 for compatibility reasons
                final String date = "" + monthOfYear + "/" + dayOfMonth + "/" + year;

                customAttribute.setSelectedValue(date, true);
                setNameHint();
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
                    	setNameHint();
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
					setNameHint();
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					// TODO Auto-generated method stub
					
				}
			});

			for(int i=0; i<customAttribute.getOptions().size(); i++)
			{
				if (customAttribute.getOptions().get(i).getSelected() == true)
				{
					spinner.setSelection(i);
				}
			}
        	
        	final TextView label = (TextView) v.findViewById(R.id.label);
        	label.setText(customAttribute.getMainLabel() + (customAttribute.getIsRequired() ? "*" : ""));
        	return v;
        }

        // handle text fields, multiselect (special case text field), date (another special case), null, etc...

        final View v = mInflater.inflate(R.layout.product_attribute_edit, null);
        final EditText edit = (EditText) v.findViewById(R.id.edit);
    	customAttribute.setCorrespondingView(edit);
        edit.setText(customAttribute.getUserReadableSelectedValue());

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
					setNameHint();
				}
			});
        }

        edit.setHint(customAttribute.getMainLabel());

        TextView label = (TextView) v.findViewById(R.id.label);
        label.setText(customAttribute.getMainLabel() + (customAttribute.getIsRequired() ? "*" : ""));
        return v;
    }
}
