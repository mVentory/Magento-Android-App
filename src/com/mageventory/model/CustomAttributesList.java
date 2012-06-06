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

import com.mageventory.MageventoryConstants;
import com.mageventory.ProductCreateActivity;
import com.mageventory.ProductDetailsActivity;
import com.mageventory.R;
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
import com.mageventory.speech.SpeechRecognition;
import com.mageventory.speech.SpeechRecognition.OnRecognitionFinishedListener;
import com.mageventory.util.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class CustomAttributesList implements Serializable {
	private static final long serialVersionUID = -6409197154564216767L;
	
	/* This is used for a small hack in case of spinner view when we want to handle long click but
		don't want to show the list of elements. */
	private static final String PREVENT_ON_CLICK_TAG = "Don't performOnClick on focus change.";
	
	private List<CustomAttribute> mCustomAttributeList;
	private String mCompoundNameFormatting;
	private int mSetID;
	
	/* Things we don't serialize */
	private ViewGroup mParentViewGroup;
	private LayoutInflater mInflater;
	private Activity mActivity;
	private EditText mName;
	private OnNewOptionTaskEventListener mNewOptionListener;
	
	public List<CustomAttribute> getList()
	{
		return mCustomAttributeList;
	}
	
	public CustomAttributesList(Activity activity, ViewGroup parentViewGroup, EditText nameView, OnNewOptionTaskEventListener listener)
	{
		mParentViewGroup = parentViewGroup;
		mActivity = activity;
		mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mName = nameView;
		mNewOptionListener = listener;
	}
	
	public void saveInCache()
	{
		/* Don't want to serialize this */
		mParentViewGroup = null;
		mInflater = null;
		mActivity = null;
		mName = null;
		mNewOptionListener = null;
		
		for(CustomAttribute elem : mCustomAttributeList)
		{
			/* Don't want to serialize this */
			elem.setCorrespondingView(null);
		}
		JobCacheManager.storeLastUsedCustomAttribs(this);
	}
	
	public static CustomAttributesList loadFromCache(Activity activity, ViewGroup parentViewGroup, EditText nameView, OnNewOptionTaskEventListener listener)
	{
		CustomAttributesList c = JobCacheManager.restoreLastUsedCustomAttribs();
		
		if (c != null)
		{
			c.mParentViewGroup = parentViewGroup;
			c.mActivity = activity;
			c.mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			c.mName = nameView;
			c.mNewOptionListener = listener;
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
	
	public void loadFromAttributeList(List<Map<String, Object>> attrList, int setID)
	{
		mSetID = setID;
		
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
	
	/* An asynctask which can be used to create an attribute option on the server in asynchronous way. */
    private static class CreateOptionTask extends AsyncTask<Void, Void, Integer> implements ResourceConstants, OperationObserver, MageventoryConstants {
    	
    	private CountDownLatch doneSignal;
    	private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
        private int requestId = INVALID_REQUEST_ID;
        private Activity host;
        private boolean success;
        private CustomAttribute attribute;
        private String newOptionName;
        private String setID;
    	private OnNewOptionTaskEventListener newOptionListener;
        
        public CreateOptionTask(Activity host, CustomAttribute attribute, String newOptionName, String setID,
        		OnNewOptionTaskEventListener listener)
        {
        	this.host = host;
        	this.attribute = attribute;
        	this.newOptionName = newOptionName;
        	this.setID = setID;
        	this.newOptionListener = listener;
        }
    
		@Override
        protected Integer doInBackground(Void... params) {
          
			host.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	if (newOptionListener != null)
        			{
        				newOptionListener.OnAttributeCreationStarted();
        			}
                }
            });
				
			doneSignal = new CountDownLatch(1);
	   	    resHelper.registerLoadOperationObserver(this);
	   	    requestId = resHelper.loadResource(host, RES_PRODUCT_ATTRIBUTE_ADD_NEW_OPTION, new String [] {attribute.getCode(), newOptionName, setID});
	   	    while (true) {
	   	    	if (isCancelled()) {
	   	    		return 0;
	   	    	}
	   	    	try {
	   	    		if (doneSignal.await(1, TimeUnit.SECONDS)) {
	   	    			break;
	   	    		}
	   	    	} catch (InterruptedException e) {
	   	    		return 0;
	   	    	}
	   	    }
            resHelper.unregisterLoadOperationObserver(this);

	        if (host == null || isCancelled()) {
	            return 0;
	        }

	        if (success) {
	            host.runOnUiThread(new Runnable() {
	                @Override
	                public void run() {
	                	if (newOptionListener != null)
	        			{
	        				newOptionListener.OnAttributeCreationFinished(attribute.getMainLabel(), newOptionName, true);
	        			}
	                }
	            });
	        } else {
	            host.runOnUiThread(new Runnable() {
	                @Override
	                public void run() {
	                	if (newOptionListener != null)
	        			{
	        				newOptionListener.OnAttributeCreationFinished(attribute.getMainLabel(), newOptionName, false);
	        			}
	                }
	            });
	        }
	        
	        if (host == null || isCancelled()) {
	            return 0;
	        }
	        return 1;
        }

    	@Override
    	public void onLoadOperationCompleted(LoadOperation op) {
    		if (op.getOperationRequestId() == requestId) {
    			success = op.getException() == null;
    			if (success)
    			{
    				success = op.getExtras() != null;
    			}
    			doneSignal.countDown();
    		}
    	}
    
        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
        }
    }
	
	/* A listener which contains functions which are called when a new option starts being created or when
		it gets created. */
    public static interface OnNewOptionTaskEventListener
    {
    	void OnAttributeCreationStarted();
    	void OnAttributeCreationFinished(String attributeName, String newOptionName, boolean success);
    }
    
	/* Shows a dialog for adding new option. */
	public void showAddNewOptionDialog(final CustomAttribute customAttribute)
	{
		final View textEntryView = mActivity.getLayoutInflater().inflate(R.layout.add_new_option_dialog, null);
		
        AlertDialog.Builder alert = new AlertDialog.Builder(mActivity); 

        alert.setTitle("New option"); 
        alert.setMessage("Enter a name for a new option for \"" + customAttribute.getMainLabel() + "\" attribute."); 
        alert.setView(textEntryView);
        
        final EditText editText = (EditText)textEntryView.findViewById(R.id.newOptionEditText);
        
        alert.setPositiveButton("Create", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				CreateOptionTask createOptionTask = new CreateOptionTask(mActivity, customAttribute, editText.getText().toString(), "" + mSetID, mNewOptionListener);
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

        final Dialog d = new DatePickerDialog(mActivity, onDateSetL, year, month, day);
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
        final Dialog dialog = new AlertDialog.Builder(mActivity).setTitle("Options").setCancelable(false)
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
        	final ArrayAdapter<String> adapter = new ArrayAdapter<String>(mActivity,
        		android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, customAttribute.getOptionsLabels());
        	spinner.setAdapter(adapter);
                
        	spinner.setFocusableInTouchMode(true);
        	
        	spinner.setOnLongClickListener(new OnLongClickListener() {
				
				@Override
				public boolean onLongClick(View v) {

					boolean hasFocus = v.hasFocus();
					
					if (!hasFocus)
					{
						v.setTag(PREVENT_ON_CLICK_TAG);
					}
					
					showAddNewOptionDialog(customAttribute);
					
					return true;
				}
			});
        	
        	spinner.setOnFocusChangeListener(new OnFocusChangeListener() {
					
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus)
					{
						if (!TextUtils.equals((String)v.getTag(), PREVENT_ON_CLICK_TAG))
						{
							spinner.performClick();
						}
						else
						{
							v.setTag(null);
						}
						InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
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
					if (hasFocus)
					{
						showMultiselectDialog( customAttribute);
						InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
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
						InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
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
        	
        	/*edit.setOnLongClickListener(new OnLongClickListener() {
    			
    			@Override
    			public boolean onLongClick(View v) {
    	                        
    	            SpeechRecognition sr = new SpeechRecognition(mActivity, new OnRecognitionFinishedListener() {
    					
    					@Override
    					public void onRecognitionFinished(String output) {
    						edit.setText(output);
    					}
    				}, edit.getText().toString());
    	            
    	            return false;
    			}
    		});*/
        }

        edit.setHint(customAttribute.getMainLabel());

        TextView label = (TextView) v.findViewById(R.id.label);
        label.setText(customAttribute.getMainLabel() + (customAttribute.getIsRequired() ? "*" : ""));
        return v;
    }
}
