package com.mageventory.job;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class JobID implements Serializable {

	private static final long serialVersionUID = -4150807232569251575L;
	
	private long mTimeStamp;
	private int mProductID;
	private int mJobType;
	private String mSKU;
	
	/* Additional data needed when performing request to the server. */
	Map<String, Object> mExtras = new HashMap<String, Object>();
	
	public JobID(int productID, int jobType, String SKU)
	{
		mTimeStamp = System.currentTimeMillis();
		mProductID = productID;
		mJobType = jobType;
		mSKU = SKU;
	}
	
	public JobID(long timeStamp, int productID, int jobType, String SKU)
	{
		mTimeStamp = timeStamp;
		mProductID = productID;
		mJobType = jobType;
		mSKU = SKU;
	}
	
	public int getJobType()
	{
		return mJobType;
	}
	
	public long getTimeStamp()
	{
		return mTimeStamp;
	}
	
	public int getProductID()
	{
		return mProductID;
	}
	
	public void setProductID(int pid)
	{
		mProductID = pid;
	}
	
	public String getSKU()
	{
		return mSKU;
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
	
	public void setExtras(Map<String, Object> extras)
	{
		mExtras = extras;
	}
	
	public String toString()
	{
		return "" + mTimeStamp;
	}
}
