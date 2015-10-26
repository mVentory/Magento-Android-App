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

package com.mageventory.res;

import java.io.Serializable;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class LoadOperation implements Serializable, Parcelable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Exception exception;
    /**
     * Whether the operation was successful
     */
    private boolean mSuccess = true;
    private Bundle extras;
    private int operationRequestId;
    private String[] resourceParams;
    private int resourceType;

    public LoadOperation(int operationRequestId, int resourceType, String[] resourceParams) {
        super();
        this.operationRequestId = operationRequestId;
        this.resourceType = resourceType;
        this.resourceParams = resourceParams;
    }

    public Exception getException() {
        return exception;
    }

    /**
     * Is operation successful
     * 
     * @return
     */
    public boolean isSuccess() {
        return mSuccess;
    }

    public Bundle getExtras() {
        return extras;
    }

    public int getOperationRequestId() {
        return operationRequestId;
    }

    public String[] getResourceParams() {
        return resourceParams;
    }

    public int getResourceType() {
        return resourceType;
    }

    public void setException(final Exception e) {
        exception = e;
        mSuccess = e == null;
    }

    public void setExtras(final Bundle extras) {
        this.extras = extras;
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
        out.writeByte((byte) (mSuccess ? 1 : 0));
        out.writeParcelable(extras, flags);
        out.writeInt(operationRequestId);
        out.writeStringArray(resourceParams);
        out.writeInt(resourceType);
    }

    public static final Parcelable.Creator<LoadOperation> CREATOR = new Parcelable.Creator<LoadOperation>() {
        @Override
        public LoadOperation createFromParcel(Parcel in) {
            return new LoadOperation(in);
        }

        @Override
        public LoadOperation[] newArray(int size) {
            return new LoadOperation[size];
        }
    };

    private LoadOperation(Parcel in) {
        mSuccess = in.readByte() == 1;
        extras = in.readParcelable(LoadOperation.class.getClassLoader());
        operationRequestId = in.readInt();
        resourceParams = in.createStringArray();
        resourceType = in.readInt();
    }
}
