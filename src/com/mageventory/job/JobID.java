package com.mageventory.job;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class JobID implements Serializable {

	private static final long serialVersionUID = -4150807232569251575L;
	private int mResourceType;
	private String[] mParams;
	
	/* Additional data needed when performing request to the server. */
	Map<String, Object> mExtras = new HashMap<String, Object>();
	
	public JobID(int resourceType, String[] params)
	{
		mResourceType = resourceType;
		mParams = params;
	}
	
	public void putExtraInfo(String key, Object value)
	{
		mExtras.put(key, value);
	}
	
	public Object getExtraInfo(String key)
	{
		return mExtras.get(key);
	}
	
	public Map<String, Object> getExtras()
	{
		return mExtras;
	}
	
	public int getResourceType()
	{
		return mResourceType;
	}
	
	public String[] getParams()
	{
		return mParams;
	}
	
	public String toString()
	{
		final String baseID = String.format("resource%d", mResourceType);
        final StringBuilder IDBuilder = new StringBuilder(baseID);
        if (mParams != null && mParams.length != 0)
        {
        	for (int i = 0; i < mParams.length; i++)
        	{
        		IDBuilder.append('.');
        		IDBuilder.append(mParams[i]);
        	}
        }
        return IDBuilder.toString(); 
	}
}
