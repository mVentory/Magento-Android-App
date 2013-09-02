
package com.mageventory.job;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Parcelable job implementation. Can be used in intents
 * 
 * @author Eugene Popovich
 */
public class ParcelableJobDetails implements Parcelable {
    private JobID mJobId;
    private boolean mPending;
    private boolean mFinished;

    public ParcelableJobDetails(Job job) {
        this.mJobId = job.getJobID();
        this.mPending = job.getPending();
        this.mFinished = job.getFinished();
    }

    public JobID getJobId() {
        return mJobId;
    }

    public boolean ismPending() {
        return mPending;
    }

    public boolean ismFinished() {
        return mFinished;
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
        out.writeParcelable(mJobId, flags);
        out.writeByte((byte) (mPending ? 1 : 0));
        out.writeByte((byte) (mFinished ? 1 : 0));
    }

    public static final Parcelable.Creator<ParcelableJobDetails> CREATOR = new Parcelable.Creator<ParcelableJobDetails>() {
        @Override
        public ParcelableJobDetails createFromParcel(Parcel in) {
            return new ParcelableJobDetails(in);
        }

        @Override
        public ParcelableJobDetails[] newArray(int size) {
            return new ParcelableJobDetails[size];
        }
    };

    private ParcelableJobDetails(Parcel in) {
        mJobId = in.readParcelable(ParcelableJobDetails.class.getClassLoader());
        mPending = in.readByte() == 1;
        mFinished = in.readByte() == 1;
    }
}
