
package com.mageventory.job;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

/* Represents an id of a job from the queue. Every field from this class can also be found in the
 * database table representing the queue of jobs. */
public class JobID implements Serializable, Parcelable {

    private static final long serialVersionUID = -4150807232569251575L;

    /*
     * Timestamp is here to uniquely identify a job. Each job has a different
     * timestamp.
     */
    private long mTimeStamp;
    private int mProductID;
    private int mJobType;
    private String mSKU;
    private String mUrl;

    public JobID(int productID, int jobType, String SKU, String url) {
        mTimeStamp = System.currentTimeMillis();
        mProductID = productID;
        mJobType = jobType;
        mSKU = SKU;
        mUrl = url;
    }

    public JobID(long timeStamp, int productID, int jobType, String SKU, String url) {
        mTimeStamp = timeStamp;
        mProductID = productID;
        mJobType = jobType;
        mSKU = SKU;
        mUrl = url;
    }

    public String getUrl()
    {
        return mUrl;
    }

    public void setUrl(String url)
    {
        mUrl = url;
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

    public String getSKU() {
        return mSKU;
    }

    public void setSKU(String sku) {
        mSKU = sku;
    }

    public void setProductID(int pid) {
        mProductID = pid;
    }

    public void setTimeStamp(long timeStamp) {
        mTimeStamp = timeStamp;
    }

    public String toString() {
        return "" + mTimeStamp;
    }

    /*****************************
     * PARCELABLE IMPLEMENTATION *
     *****************************/
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mTimeStamp);
        out.writeInt(mProductID);
        out.writeInt(mJobType);
        out.writeString(mSKU);
        out.writeString(mUrl);
    }

    public static final Parcelable.Creator<JobID> CREATOR = new Parcelable.Creator<JobID>() {
        @Override
        public JobID createFromParcel(Parcel in) {
            return new JobID(in);
        }

        @Override
        public JobID[] newArray(int size) {
            return new JobID[size];
        }
    };

    private JobID(Parcel in) {
        mTimeStamp = in.readLong();
        mProductID = in.readInt();
        mJobType = in.readInt();
        mSKU = in.readString();
        mUrl = in.readString();
    }
}
