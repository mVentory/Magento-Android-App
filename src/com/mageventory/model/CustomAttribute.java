package com.mageventory.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomAttribute implements Serializable 
{
	public static class CustomAttributeOption
	{
		private String mID;
		private String mLabel;
		
		public CustomAttributeOption(String ID, String label)
		{
			mID = ID;
			mLabel = label;
		}
		
		public void setID(String ID)
		{
			mID = ID;
		}
		
		public String getID()
		{
			return mID;
		}
		
		public void setLabel(String label)
		{
			mLabel = label;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
	}
	
	public static final String TYPE_BOOLEAN = "boolean";
	public static final String TYPE_SELECT = "select";
	public static final String TYPE_MULTISELECT = "multiselect";
	public static final String TYPE_DROPDOWN = "dropdown";
	public static final String TYPE_PRICE = "price";
	public static final String TYPE_DATE = "date";
	
	private List<CustomAttributeOption> mOptions;
	private String mSelectedValue;
	private String mType;
	private boolean mIsRequired;
	private String mMainLabel;
	
	public void setMainLabel(String mainLabel)
	{
		mMainLabel = mainLabel;
	}
	
	public String getMainLabel()
	{
		return mMainLabel;
	}
	
	public void setIsRequired(boolean isRequired)
	{
		mIsRequired = isRequired;
	}
	
	public boolean getIsRequired()
	{
		return mIsRequired;
	}
	
	public String getType()
	{
		return mType;
	}
	
	public boolean isOfType(String type)
	{
		return mType.equals(type);
	}
	
	public void setType(String type)
	{
		mType = type;
	}
	
	public void setOptions(List<CustomAttributeOption> options)
	{
		mOptions = options;
	}
	
	public List<CustomAttributeOption> getOptions()
	{
		return mOptions;
	}
	
	public List<String> getOptionsLabels()
	{
		List<String> out = new ArrayList<String>();
		for (int i=0; i<mOptions.size(); i++)
		{
			out.add(mOptions.get(i).getLabel());
		}
		
		return out;
	}
	
	public void setSelectedValue(String selectedValue)
	{
		mSelectedValue = selectedValue;
	}
	
	public String getSelectedValue()
	{
		return mSelectedValue;
	}
	
}
