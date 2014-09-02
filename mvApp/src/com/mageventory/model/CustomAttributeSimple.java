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

package com.mageventory.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Simplified custom attribute information which implements Parcelable and may
 * be passed to intent
 */
public class CustomAttributeSimple implements Parcelable {

    /**
     * The custom attribute main label
     */
    private String mMainLabel;
    /**
     * The custom attribute code
     */
    private String mCode;
    /**
     * The custom attribute type
     */
    private String mType;
    /**
     * The custom attribute selected value
     */
    private String mSelectedValue;

    /**
     * The custom attribute appended value from search results
     */
    private String mAppendedValue;

    /**
     * @param code
     * @param mainLabel
     * @param type
     * @param selectedValue
     */
    public CustomAttributeSimple(String code, String mainLabel, String type, String selectedValue) {
        mCode = code;
        mMainLabel = mainLabel;
        mType = type;
        mSelectedValue = selectedValue;
    }

    /**
     * Create {@link CustomAttributeSimple} from the {@link CustomAttribute} by
     * copying necessary fields
     * 
     * @param customAttribute
     * @return
     */
    public static CustomAttributeSimple from(CustomAttribute customAttribute) {
        return new CustomAttributeSimple(customAttribute.getCode(), customAttribute.getMainLabel(),
                customAttribute.getType(), customAttribute.getSelectedValue());
    }

    /**
     * Get the attribute main label
     * 
     * @return
     */
    public String getMainLabel() {
        return mMainLabel;
    }

    /**
     * Set the attribute main label
     * 
     * @param mainLabel
     */
    public void setMainLabel(String mainLabel) {
        mMainLabel = mainLabel;
    }

    /**
     * Get the attribute code
     * 
     * @return
     */
    public String getCode() {
        return mCode;
    }

    /**
     * Set the attribute code
     * 
     * @param code
     */
    public void setCode(String code) {
        mCode = code;
    }

    /**
     * Get the attribute type
     * 
     * @return
     */
    public String getType() {
        return mType;
    }

    /**
     * Set the attribute type
     * 
     * @param type
     */
    public void setType(String type) {
        mType = type;
    }

    /**
     * Is the attribute of the type
     * 
     * @param type
     * @return
     */
    public boolean isOfType(String type) {
        return mType.equals(type);
    }

    /**
     * Get the attribute appended value
     * 
     * @return
     */
    public String getAppendedValue() {
        return mAppendedValue;
    }

    /**
     * Set the attribute appended value
     * 
     * @param appendedValue
     */
    public void setAppendedValue(String appendedValue) {
        mAppendedValue = appendedValue;
    }

    /**
     * Get the attribute selected value
     * 
     * @return
     */
    public String getSelectedValue() {
        return mSelectedValue;
    }

    /**
     * Set the attribute selected value
     * 
     * @param selectedValue
     */
    public void setSelectedValue(String selectedValue) {
        mSelectedValue = selectedValue;
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
        out.writeString(mMainLabel);
        out.writeString(mCode);
        out.writeString(mType);
        out.writeString(mAppendedValue);
        out.writeString(mSelectedValue);
    }

    public static final Parcelable.Creator<CustomAttributeSimple> CREATOR = new Parcelable.Creator<CustomAttributeSimple>() {
        @Override
        public CustomAttributeSimple createFromParcel(Parcel in) {
            return new CustomAttributeSimple(in);
        }

        @Override
        public CustomAttributeSimple[] newArray(int size) {
            return new CustomAttributeSimple[size];
        }
    };

    private CustomAttributeSimple(Parcel in) {
        mMainLabel = in.readString();
        mCode = in.readString();
        mType = in.readString();
        mAppendedValue = in.readString();
        mSelectedValue = in.readString();
    }
}
