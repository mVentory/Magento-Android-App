/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/

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
    private int mProgressPercentage;

    public ParcelableJobDetails(Job job) {
        this.mJobId = job.getJobID();
        this.mPending = job.getPending();
        this.mFinished = job.getFinished();
        this.mProgressPercentage = job.getProgressPercentage();
    }

    public JobID getJobId() {
        return mJobId;
    }

    public boolean isPending() {
        return mPending;
    }

    public boolean isFinished() {
        return mFinished;
    }

    public int getProgressPercentage() {
        return mProgressPercentage;
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
        out.writeInt(mProgressPercentage);
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
        mProgressPercentage = in.readInt();
    }
}
