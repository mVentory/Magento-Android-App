package com.mageventory.job;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/* Represents an id of a job from the queue. Every field from this class can also be found in the
 * database table representing the queue of jobs. */
public class JobID implements Serializable {

	private static final long serialVersionUID = -4150807232569251575L;

	/* Timestamp is here to uniquely identify a job. Each job has a different timestamp. */
	private long mTimeStamp;
	private int mProductID;
	private int mJobType;
	private String mSKU;

	public JobID(int productID, int jobType, String SKU) {
		mTimeStamp = System.currentTimeMillis();
		mProductID = productID;
		mJobType = jobType;
		mSKU = SKU;
	}

	public JobID(long timeStamp, int productID, int jobType, String SKU) {
		mTimeStamp = timeStamp;
		mProductID = productID;
		mJobType = jobType;
		mSKU = SKU;
	}

	public int getJobType() {
		return mJobType;
	}

	public long getTimeStamp() {
		return mTimeStamp;
	}

	public int getProductID() {
		return mProductID;
	}

	public void setProductID(int pid) {
		mProductID = pid;
	}

	public String getSKU() {
		return mSKU;
	}

	public String toString() {
		return "" + mTimeStamp;
	}
}
