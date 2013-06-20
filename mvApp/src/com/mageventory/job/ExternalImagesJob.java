package com.mageventory.job;

public class ExternalImagesJob {
	
	public long mTimestamp;
	/* The product code can be either SKU or a barcode. We do need an SKU to upload an image. */
	public String mProductCode;
	public String mSKU;
	public long mProfileID;
	public int mAttemptsCount;
	
	public ExternalImagesJob(long timestamp, String productCode, String sku, long profileID, int attemptsCount)
	{
		mTimestamp = timestamp;
		mProductCode = productCode;
		mSKU = sku;
		mProfileID = profileID;
		mAttemptsCount = attemptsCount;
	}
	
	@Override
	public String toString() {
		return "" + mTimestamp + ", " + mProductCode + ", " + mSKU + ", " + mProfileID + ", " + mAttemptsCount;
	}
}
