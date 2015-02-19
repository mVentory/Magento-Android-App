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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.job.JobCacheManager;
import com.mageventory.resprocessor.ProductAttributeFullInfoProcessor;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.LoadingControl;
import com.mventory.R;

public class CustomAttribute implements Serializable, Parcelable {
    private static final long serialVersionUID = 6L;

    /**
     * The possible default value for the description attribute which means
     * description is not available
     */
    public static final String NOT_AVAILABLE_VALUE = "n/a";

    /*
     * Represents a single option. Used in case of attributes that have options.
     * In case of attributes that don't have options we just use a simple String
     * to store the value.
     */
    public static class CustomAttributeOption implements Serializable, Parcelable {
        private static final long serialVersionUID = -3872566328848103531L;
        private String mID;
        private String mLabel;
        private boolean mSelected;

        public CustomAttributeOption(String ID, String label) {
            mID = ID;
            mLabel = label;
        }

        public void setSelected(boolean selected) {
            mSelected = selected;
        }

        public boolean getSelected() {
            return mSelected;
        }

        public void setID(String ID) {
            mID = ID;
        }

        public String getID() {
            return mID;
        }

        public void setLabel(String label) {
            mLabel = label;
        }

        public String getLabel() {
            return mLabel;
        }

        @Override
        public CustomAttributeOption clone() {
            CustomAttributeOption result = new CustomAttributeOption(mID, mLabel);
            result.mSelected = mSelected;
            return result;
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
            out.writeString(mID);
            out.writeString(mLabel);
            out.writeByte((byte) (mSelected ? 1 : 0));
        }

        public static final Parcelable.Creator<CustomAttributeOption> CREATOR = new Parcelable.Creator<CustomAttributeOption>() {
            @Override
            public CustomAttributeOption createFromParcel(Parcel in) {
                return new CustomAttributeOption(in);
            }

            @Override
            public CustomAttributeOption[] newArray(int size) {
                return new CustomAttributeOption[size];
            }
        };

        private CustomAttributeOption(Parcel in) {
            mID = in.readString();
            mLabel = in.readString();
            mSelected = in.readByte() == 1;
        }
    }

    /* Each attribute is of one of those types. */
    public static final String TYPE_BOOLEAN = "boolean";
    /**
     * An id of the false attribute option for the TYPE_BOOLEAN attributes
     */
    public static final String TYPE_BOOLEAN_FALSE_VALUE = "0";
    /**
     * An id of the true attribute option for the TYPE_BOOLEAN attributes
     */
    public static final String TYPE_BOOLEAN_TRUE_VALUE = "1";
    public static final String TYPE_SELECT = "select";
    public static final String TYPE_MULTISELECT = "multiselect";
    public static final String TYPE_DROPDOWN = "dropdown";
    public static final String TYPE_PRICE = "price";
    public static final String TYPE_DATE = "date";
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_TEXTAREA = "textarea";
    /**
     * The value for the weight attribute type
     */
    public static final String TYPE_WEIGHT = "weight";

    /**
     * Possible values for the attribute input_method and alt_input_method
     * options
     */
    public enum InputMethod implements Parcelable {
    	/**
    	 * The standard keyboard input type. All characters are allowed.
    	 */
        NORMAL_KEYBOARD(0),
        /**
         * The numeric keyboard input type.
         */
        NUMERIC_KEYBOARD(1),
        /**
         * The scanner input type to scan the attribute value via the scanner
         * application.
         */
        SCANNER(2),
        /**
         * The gestures input type to modify attribute value using gestures
         */
        GESTURES(3),
        /**
         * The copy from Internet search input type to open web search activity
         * to copy the attribute value from the search results
         */
        COPY_FROM_INTERNET_SEARCH(4),
        /**
         * The copy from another product input type. This opens scanner to
         * scan another product SKU/Barcode and copy the same attribute value
         * from the scanned product.
         */
        COPY_FROM_ANOTHER_PRODUCT(5),
        ;
        /**
         * The input type code as server sends it
         */
        int mCode;

        /**
         * @param code the input type code as server sends it
         */
        InputMethod(int code) {
            mCode = code;
        }

        /**
         * Get the input type code
         * 
         * @return
         */
        public int getCode() {
            return mCode;
        }

        /**
         * Get the {@link InputMethod} for the specified code if exists
         * 
         * @param code the input method code to search
         * @return {@link InputMethod} with the same code as specified in the
         *         parameter if found. Otherwise returns null
         */
        public static InputMethod getInputMethodForCode(int code) {
            InputMethod result = null;
            // Iterate through all possible InputMethod values and search for
            // code match
            for (InputMethod inputMethod : values()) {
                if (inputMethod.mCode == code) {
                    // found match, remember result and interrupt the loop
                    result = inputMethod;
                    break;
                }
            }
            return result;
        }

        /*****************************
         * PARCELABLE IMPLEMENTATION *
         *****************************/

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeInt(ordinal());
        }

        public static final Creator<InputMethod> CREATOR = new Creator<InputMethod>() {
            @Override
            public InputMethod createFromParcel(final Parcel source) {
                return InputMethod.values()[source.readInt()];
            }

            @Override
            public InputMethod[] newArray(final int size) {
                return new InputMethod[size];
            }
        };
    }
    
    /**
     * Possible values for the attribute content_type options
     */
    public enum ContentType implements Parcelable {
        /**
         * The text content type (default). There are no any special processing
         * for such attributes.
         */
        TEXT(0),
        /**
         * The Youtube video id content type. Not yet implemented. 
         * TODO
         */
        YOUTUBE_VIDEO_ID(1),
        /**
         * The Web address content type. Not yet implemented 
         * TODO
         */
        WEB_ADDRESS(2),
        /**
         * The ISBN 10 content type. The attributes of such content type will
         * support live ISBN 10 code recognition and book details loading.
         */
        ISBN10(3),
        /**
         * The ISBN 13 content type. The attributes of such content type will
         * support live ISBN 13 code recognition and book details loading.
         */
        ISBN13(4),
        /**
         * The secondary barcode content type. If attribute of such content type
         * is present in the attribute set and user scanned SKU/Barcode with
         * some metadata (ISSN for example) then the metadata will be stored to
         * this attribute with the SECONDARY_BARCODE content type.
         */
        SECONDARY_BARCODE(5),
        /**
         * The ISSN 13 and ISSN 8 content type. The attributes of such content
         * type will support live ISSN code validation.
         */
        ISSN(6),
        ;
        /**
         * The content type code as server sends it
         */
        int mCode;
        
        /**
         * @param code the content type code as server sends it
         */
        ContentType(int code) {
            mCode = code;
        }

        /**
         * Get the content type code
         * 
         * @return
         */
        public int getCode() {
            return mCode;
        }
        
        /**
         * Get the {@link ContentType} for the specified code if exists
         * 
         * @param code the content type code to search
         * @return {@link ContentType} with the same code as specified in the
         *         parameter if found. Otherwise returns null
         */
        public static ContentType getContentTypeForCode(int code) {
            ContentType result = null;
            // Iterate through all possible ContentType values and search for
            // code match
            for (ContentType contentType : values()) {
                if (contentType.mCode == code) {
                    // found match, remember result and interrupt the loop
                    result = contentType;
                    break;
                }
            }
            return result;
        }
        
        /*****************************
         * PARCELABLE IMPLEMENTATION *
         *****************************/
        
        @Override
        public int describeContents() {
            return 0;
        }
        
        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeInt(ordinal());
        }
        
        public static final Creator<ContentType> CREATOR = new Creator<ContentType>() {
            @Override
            public ContentType createFromParcel(final Parcel source) {
                return ContentType.values()[source.readInt()];
            }
            
            @Override
            public ContentType[] newArray(final int size) {
                return new ContentType[size];
            }
        };
    }

    /* Data from the server associated with this attribute. */

    /*
     * A list of options, used only in case of attributes that have them. In
     * other cases we just use mSelectedValue field to store the value of an
     * attribute.
     */
    private ArrayList<CustomAttributeOption> mOptions;
    private String mSelectedValue = "";
    /* This always stores one of the TYPE_* constants value. */
    private String mType;
    private boolean mIsRequired;
    private String mMainLabel;
    private String mCode;
    private String mAttributeID;
    /**
     * The attribute hint
     */
    private String mHint;
    /**
     * Whether the attribute is configurable
     */
    private boolean mConfigurable;

    /**
     * Flag indicating whether attribute value should be used for the web search
     * query
     */
    private boolean mUseForSearch;

    /**
     * Flag indicating whether the attribute is read only and can't be edited
     */
    private boolean mReadOnly;
    
    /**
     * Flag indicating whether the adding of new options is allowed for the
     * attribute. Used for attribute types with predefined options to select
     */
    private boolean mAddNewOptionsAllowed;
    /**
     * Flag indicating whether the attribute value should be processed as HTML
     * text when viewing product details
     */
    private boolean mHtmlAllowedOnFront;

    /**
     * The attribute content type
     */
    private ContentType mContentType;

    /**
     * The attribute default input method
     */
    private InputMethod mInputMethod;

    /**
     * The attribute alternative input methods
     */
    private ArrayList<InputMethod> mAlternateInputMethods;

    /*
     * Each attribute has a corresponding view which is either an EditBox or a
     * Spinner depending on type of the attribute.
     */
    private transient View mCorrespondingView;

    /**
     * The loading control related to the custom attribute for various
     * operations such as adding new attribute value, loading book information,
     * copying attribute value from the another product
     */
    private transient LoadingControl mAttributeLoadingControl;

    /**
     * Reference to the hint view which appears below attribute edit box
     */
    private transient TextView mHintView;

    public CustomAttribute() {
    }

    public void setAttributeID(String attribID) {
        mAttributeID = attribID;
    }

    public String getAttributeID() {
        return mAttributeID;
    }

    /**
     * Set the attribute hint
     * 
     * @param hint
     */
    public void setHint(String hint) {
        mHint = hint;
    }

    /**
     * Get the attribute hint
     * 
     * @return
     */
    public String getHint() {
        return mHint;
    }

    public void setCorrespondingView(View view) {
        mCorrespondingView = view;
    }

    public View getCorrespondingView() {
        return mCorrespondingView;
    }

    /**
     * Get the corresponding attribute view as {@link EditText} to avoid manual
     * type cast in many places
     * 
     * @return
     */
    public EditText getCorrespondingEditTextView() {
        return (EditText) mCorrespondingView;
    }

    /**
     * Set the attribute related loading control
     * 
     * @param attributeLoadingControl
     */
    public void setAttributeLoadingControl(LoadingControl attributeLoadingControl) {
        mAttributeLoadingControl = attributeLoadingControl;
    }

    /**
     * Get the loading control related to the attribute
     * 
     * @return
     */
    public LoadingControl getAttributeLoadingControl() {
        return mAttributeLoadingControl;
    }

    /**
     * Get the corresponding hint view related to the attribute
     * 
     * @return
     */
    public TextView getHintView() {
        return mHintView;
    }

    /**
     * Set the reference to the corresponding hint view so it may be accessed
     * later
     * 
     * @param hintView
     */
    public void setHintView(TextView hintView) {
        mHintView = hintView;
    }

    /**
     * Check whether the attribute has the same code
     * 
     * @param code the code to compare attribute code with
     * @return true if attribute has the same code (including null comparison),
     *         false otherwise
     */
    public boolean isOfCode(String code) {
        return TextUtils.equals(mCode, code);
    }

    public void setCode(String code) {
        mCode = code;
    }

    public String getCode() {
        return mCode;
    }

    public void setMainLabel(String mainLabel) {
        mMainLabel = mainLabel;
    }

    public String getMainLabel() {
        return mMainLabel;
    }

    public void setIsRequired(boolean isRequired) {
        mIsRequired = isRequired;
    }

    public boolean getIsRequired() {
        return mIsRequired;
    }

    public String getType() {
        return mType;
    }

    public boolean isOfType(String type) {
        return mType.equals(type);
    }

    public void setType(String type) {
        mType = type;
        Log.d("type", type);
    }

    /**
     * Is the attribute configurable
     * 
     * @return
     */
    public boolean isConfigurable() {
        return mConfigurable;
    }

    /**
     * Set whether the attribute is configurable
     * 
     * @param configurable
     */
    public void setConfigurable(boolean configurable) {
        this.mConfigurable = configurable;
    }

    /**
     * Is attribute should be used for the web search query
     * 
     * @return
     */
    public boolean isUseForSearch() {
        return mUseForSearch;
    }

    /**
     * Set whether the attribute should be used for the web search query
     * 
     * @param useForSearch
     */
    public void setUseForSearch(boolean useForSearch)
    {
        mUseForSearch = useForSearch;
    }

    /**
     * Is the attribute read only
     * 
     * @return
     */
    public boolean isReadOnly() {
        return mReadOnly;
    }

    /**
     * Set whether attribute is read only
     * 
     * @param readOnly
     */
    public void setReadOnly(boolean readOnly) {
        mReadOnly = readOnly;
    }

    /**
     * Is it allowed to add new attribute options
     * 
     * @return
     */
    public boolean isAddNewOptionsAllowed() {
        return mAddNewOptionsAllowed;
    }

    /**
     * Set whether it is allowed to add new options
     * 
     * @param addNewOptionsAllowed
     */
    public void setAddNewOptionsAllowed(boolean addNewOptionsAllowed) {
        mAddNewOptionsAllowed = addNewOptionsAllowed;
    }

    /**
     * Whether the empty value selection allowed for such attribute. For a now
     * it is hardcoded to condition whether the attribute is of type
     * {@link #TYPE_DROPDOWN} or {@link #TYPE_SELECT}
     * 
     * @return true if empty value selection is allowed, false otherwise
     */
    public boolean isEmptyValueSelectionAllowed() {
        return isOfType(TYPE_DROPDOWN) || isOfType(TYPE_SELECT);
    }

    /**
     * Is the attribute value should be processed as HTML text when viewing
     * product details
     * 
     * @return
     */
    public boolean isHtmlAllowedOnFront() {
        return mHtmlAllowedOnFront;
    }

    /**
     * Set whether the attribute value should be processed as HTML text when
     * viewing product details
     * 
     * @param htmlAllowedOnFront
     */
    public void setHtmlAllowedOnFront(boolean htmlAllowedOnFront) {
        mHtmlAllowedOnFront = htmlAllowedOnFront;
    }

    /**
     * Get the attribute content type
     * 
     * @return
     */
    public ContentType getContentType() {
        return mContentType;
    }

    /**
     * Set the attribute content type
     * 
     * @param contentType
     */
    public void setContentType(ContentType contentType) {
        mContentType = contentType;
    }

    /**
     * Check whether the attribute has the same content type
     * 
     * @param contentType the content type to compare attribute content type
     *            with
     * @return true if attribute has the same content type, false otherwise
     */
    public boolean hasContentType(ContentType contentType) {
        return mContentType == contentType;
    }

    /**
     * Get the default input method for the attribute
     * 
     * @return
     */
    public InputMethod getInputMethod() {
        return mInputMethod;
    }

    /**
     * Set the default input method for the attribute
     * 
     * @param inputMethod
     */
    public void setInputMethod(InputMethod inputMethod) {
        mInputMethod = inputMethod;
    }

    /**
     * Get the attribute alternative input methods
     * 
     * @return
     */
    public ArrayList<InputMethod> getAlternateInputMethods() {
        return mAlternateInputMethods;
    }

    /**
     * Set the attribute alternative input methods
     * 
     * @param alternateInputMethods
     */
    public void setAlternateInputMethods(ArrayList<InputMethod> alternateInputMethods) {
        mAlternateInputMethods = alternateInputMethods;
    }

    /**
     * Add an input method to the collection of the attribute alternative input
     * methods
     * 
     * @param inputMethod the input method to add
     */
    public void addAlternateInputMethod(InputMethod inputMethod) {
        if (inputMethod == null) {
            // only non null input methods are allowed
            throw new IllegalArgumentException("IputMethod cannot be null");
        }
        if (mAlternateInputMethods == null) {
            // if alternative input methods collection is not yet initialized
            mAlternateInputMethods = new ArrayList<InputMethod>();
        }
        mAlternateInputMethods.add(inputMethod);
    }

    /**
     * Check whether the attribute has input method in its alternative input
     * method options
     * 
     * @param inputMethod the input method to check
     * @return true if the alternative input methods collection of the attribute
     *         is initialized and contains the specified inputMethod, otherwise
     *         false
     */
    public boolean hasAlternateInputMethod(InputMethod inputMethod) {
        return mAlternateInputMethods != null && mAlternateInputMethods.contains(inputMethod);
    }

    /**
     * Check whether the attribute has input method as its default input method
     * or in its alternative input method options
     * 
     * @param inputMethod the input method to check
     * @return true if the default input method equals to the inputMethod or the
     *         alternative input methods collection of the attribute is
     *         initialized and contains the specified inputMethod, otherwise
     *         false
     */
    public boolean hasDefaultOrAlternateInputMethod(InputMethod inputMethod) {
        return mInputMethod == inputMethod || hasAlternateInputMethod(inputMethod);
    }

    public void setOptions(ArrayList<CustomAttributeOption> options) {
        mOptions = options;
    }

    /* Convert the server response to a more friendly data type. */
    public void setOptionsFromServerResponse(Object[] options) {
        mOptions = new ArrayList<CustomAttributeOption>();

        for (Object map : options) {
            Map<String, Object> optionsMap = (Map<String, Object>) map;
            CustomAttributeOption op;

            if (!mType.equals(TYPE_BOOLEAN)) {
                if (((String) optionsMap.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS_VALUE))
                        .length() == 0) {
                    continue;
                }

                op = new CustomAttributeOption(
                        (String) optionsMap.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS_VALUE),
                        (String) optionsMap
                                .get(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS_LABEL));
            } else {
                op = new CustomAttributeOption(
                        ""
                                + JobCacheManager.getIntValue(optionsMap
                                        .get(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS_VALUE)),
                        (String) optionsMap
                                .get(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS_LABEL));

            }

            mOptions.add(op);
        }
    }

    /*
     * Get a list of options in a form the server would return them. This can be
     * used to help simulate server response in offline mode.
     */
    public Object[] getOptionsAsArrayOfMaps() {
        List<Object> options = new ArrayList<Object>();

        for (CustomAttributeOption elem : mOptions) {
            Map<String, Object> mapElem = new HashMap<String, Object>();

            mapElem.put(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS_VALUE, elem.getID());
            mapElem.put(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS_LABEL, elem.getLabel());

            options.add(mapElem);
        }

        Object[] out = new Object[options.size()];
        return options.toArray(out);
    }

    public ArrayList<CustomAttributeOption> getOptions() {
        return mOptions;
    }

    /* Get the options list in a form of list of strings. */
    public List<String> getOptionsLabels() {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < mOptions.size(); i++) {
            out.add(mOptions.get(i).getLabel());
        }

        return out;
    }

    /*
     * Set an option with a given id either selected or deselected. Update the
     * corresponding view optionally.
     */
    public void setOptionSelected(int idx, boolean selected, boolean updateView) {
        if (isOfType(CustomAttribute.TYPE_BOOLEAN) || isOfType(CustomAttribute.TYPE_SELECT)
                || isOfType(CustomAttribute.TYPE_DROPDOWN)) {
            for (CustomAttributeOption elem : mOptions) {
                elem.setSelected(false);
            }
        }

        mOptions.get(idx).setSelected(selected);

        if (updateView) {
            // mCorrespondingView is a CheckBox for the TYPE_BOOLEAN
            if (isOfType(CustomAttribute.TYPE_BOOLEAN)) {
                ((CheckBox) mCorrespondingView).setChecked(isBooleanTrueValue());
            } else if (isOfType(CustomAttribute.TYPE_MULTISELECT)
                    || isOfType(CustomAttribute.TYPE_SELECT)
                    || isOfType(CustomAttribute.TYPE_DROPDOWN)) {
                ((EditText) mCorrespondingView).setText(getUserReadableSelectedValue());
            }
        }
    }

    /* Add new option to the list of options without consulting the server. */
    public void addNewOption(Activity activity, String label) {
        CustomAttributeOption newOption = new CustomAttributeOption("" + (-1), label);
        newOption.setSelected(true);

        if (isOfType(CustomAttribute.TYPE_BOOLEAN)
                || isOfType(CustomAttribute.TYPE_SELECT)
                || isOfType(CustomAttribute.TYPE_DROPDOWN))
        {
            for (CustomAttributeOption ca : mOptions)
            {
                ca.setSelected(false);
            }
        }

        mOptions.add(newOption);

        Collections.sort(mOptions, new Comparator<Object>() {

            @Override
            public int compare(Object lhs, Object rhs) {
                String left = ((CustomAttributeOption) lhs).getLabel();
                String right = ((CustomAttributeOption) rhs).getLabel();

                return ProductAttributeFullInfoProcessor.compareOptions(left, right);
            }
        });

        if (isOfType(CustomAttribute.TYPE_MULTISELECT)
                || isOfType(CustomAttribute.TYPE_BOOLEAN)
                || isOfType(CustomAttribute.TYPE_SELECT)
                || isOfType(CustomAttribute.TYPE_DROPDOWN))
        {
            ((EditText) mCorrespondingView).setText(getUserReadableSelectedValue());
        }
    }

    /*
     * Remove a given option from the list of options for this attribute. In
     * case of single select attributes we set selection to the first element.
     */
    public void removeOption(Activity activity, String label) {
        int indexToRemove = -1;

        for (int i = 0; i < mOptions.size(); i++) {
            if (TextUtils.equals(mOptions.get(i).getLabel(), label)) {
                indexToRemove = i;
                break;
            }
        }

        mOptions.remove(indexToRemove);

        if (isOfType(CustomAttribute.TYPE_MULTISELECT)) {
            ((EditText) mCorrespondingView).setText(getUserReadableSelectedValue());
        } else if (isOfType(CustomAttribute.TYPE_BOOLEAN) || isOfType(CustomAttribute.TYPE_SELECT)
                || isOfType(CustomAttribute.TYPE_DROPDOWN)) {
            ((EditText) mCorrespondingView).setText(getUserReadableSelectedValue());
            mOptions.get(0).setSelected(true);
        }
    }

    /*
     * Get the current value of the attribute in a form that we can send to the
     * server.
     */
    public String getSelectedValue() {
        if (isOfType(CustomAttribute.TYPE_MULTISELECT)) {
            /*
             * List<String> out = new ArrayList<String>();
             * for(CustomAttributeOption option : mOptions) { if
             * (option.getSelected() == true) { out.add(option.getID()); } }
             * return out.toArray(new String[out.size()]);
             */
            StringBuilder out = new StringBuilder();
            for (CustomAttributeOption option : mOptions) {
                if (option.getSelected() == true) {
                    if (out.length() > 0) {
                        out.append(",");
                    }
                    out.append(option.getID());
                }
            }

            return out.toString();
        } else if (isOfType(CustomAttribute.TYPE_BOOLEAN) || isOfType(CustomAttribute.TYPE_SELECT)
                || isOfType(CustomAttribute.TYPE_DROPDOWN)) {
            for (CustomAttributeOption option : mOptions) {
                if (option.getSelected() == true) {
                    return option.getID();
                }
            }
            return "";
        } else {
            return mSelectedValue;
        }
    }

    /**
     * Takes comma separated Strings which are either option ids or some text
     * user entered in editbox (depending on type). This is the format returned
     * by the server as value of an attribute. This function is just parsing it
     * and convering it to a more friendly format. In case of attributes which
     * have options we store their value in mOptions array. In all other cases
     * we store the value as a simple string in mSelectedValue field.
     * 
     * @param selectedValue the value to select
     * @param updateView whether to update custom attribute view with the
     *            selected value
     */
    public void setSelectedValue(String selectedValue, boolean updateView) {
        setSelectedValue(selectedValue, true, updateView);
    }

    /**
     * Takes comma separated Strings which are either option ids or some text
     * user entered in editbox (depending on type). This is the format returned
     * by the server as value of an attribute. This function is just parsing it
     * and convering it to a more friendly format. In case of attributes which
     * have options we store their value in mOptions array. In all other cases
     * we store the value as a simple string in mSelectedValue field.
     * 
     * @param selectedValue the value to select
     * @param selectDefault whether to select default value for the TYPE_SELECT
     *            attributes if the selectedValue is missing
     * @param updateView whether to update custom attribute view with the
     *            selected value
     */
    public void setSelectedValue(String selectedValue, boolean selectDefault, boolean updateView) {
        if (selectedValue == null)
            selectedValue = "";

        // do not updated view if corresponding view was not specified
        updateView &= mCorrespondingView != null;

        if (isOfType(CustomAttribute.TYPE_MULTISELECT)) {
            String[] selected = selectedValue.split(",");

            for (CustomAttributeOption option : mOptions) {
                option.setSelected(false);
                for (int i = 0; i < selected.length; i++) {
                    if (option.getID().equals(selected[i])) {
                        option.setSelected(true);
                        break;
                    }
                }
            }
            if (updateView) {
                ((EditText) mCorrespondingView).setText(getUserReadableSelectedValue());
            }
        } else if (isOfType(CustomAttribute.TYPE_BOOLEAN) || isOfType(CustomAttribute.TYPE_SELECT)
                || isOfType(CustomAttribute.TYPE_DROPDOWN)) {

            boolean defaultOptionSelected = false;
            for (CustomAttributeOption option : mOptions) {
                option.setSelected(false);
                if (option.getID().equals(selectedValue)) {
                    option.setSelected(true);
                    defaultOptionSelected = true;
                }
            }
            // mOptions may be empty
            if (!defaultOptionSelected && !mOptions.isEmpty() && selectDefault
                    && !isEmptyValueSelectionAllowed())
            {
                mOptions.get(0).setSelected(true);
            }
            if (updateView) {
            	// mCorrespondingView is a CheckBox for the TYPE_BOOLEAN
                if (isOfType(CustomAttribute.TYPE_BOOLEAN)) {
                    ((CheckBox) mCorrespondingView).setChecked(isBooleanTrueValue());
                } else {
                    ((EditText) mCorrespondingView).setText(getUserReadableSelectedValue());
                }
            }
        } else {
            mSelectedValue = selectedValue;
            if (updateView) {
                ((EditText) mCorrespondingView).setText(getUserReadableSelectedValue());
            }
        }
    }

    /* Get whatever we can put in editbox for user to see */
    public String getUserReadableSelectedValue() {
        if (isOfType(CustomAttribute.TYPE_MULTISELECT)) {
            StringBuilder out = new StringBuilder();
            for (CustomAttributeOption option : mOptions) {
                if (option.getSelected() == true) {
                    if (out.length() > 0) {
                        out.append(", ");
                    }

                    out.append(option.getLabel());
                }
            }

            return out.toString();
        } else if (isOfType(CustomAttribute.TYPE_BOOLEAN) || isOfType(CustomAttribute.TYPE_SELECT)
                || isOfType(CustomAttribute.TYPE_DROPDOWN)) {
            for (CustomAttributeOption option : mOptions) {
                if (option.getSelected() == true) {
                    return option.getLabel();
                }
            }
            return "";
        } else {
            return mSelectedValue;
        }
    }

    /**
     * Convert value from the attribute options codes to attribute options
     * labels form
     * 
     * @param attribValue the attribute value. Comma separated attribute option codes
     * @return comma and space separated attribute option labels
     */
    public String getUserReadableSelectedValue(String attribValue) {
        StringBuilder out = new StringBuilder();
        String[] list = attribValue.split(",");
        for (int l = 0; l < list.length; l++) {
            for (CustomAttributeOption option : mOptions) {
                if (TextUtils.equals(option.getID(), list[l])) {
                    if (out.length() > 0) {
                        out.append(", ");
                    }
                    out.append(option.getLabel());
                    break;
                }
            }
            if (!isOfType(CustomAttribute.TYPE_MULTISELECT)) {
                break;
            }
        }
        return out.toString();
    }

    /**
     * Filter attribute value (some special cases. Code migrated from the
     * Product.CustomAttributeInfo.#setValueLabel method. Currently doesn't
     * exist)
     * 
     * @param value
     * @return
     */
    public static String filterValue(String value) {
        if (value != null) {
            // Special Types Handling
            // 1- if Type is Date then remove "00:00:00"
            value = value.replace("00:00:00", "");

            // 2- if type is float remove ".0000"
            value = value.replace(".0000", "");
        }
        return value;
    }

    /**
     * Get the position of the value in the options list by the option id
     * 
     * @param value
     * @return
     */
    public int getValuePosition(String value) {
        int position = -1;
        for (int i = 0; i < mOptions.size(); i++) {
            CustomAttributeOption option = mOptions.get(i);
            if (TextUtils.equals(value, option.getID())) {
                position = i;
                break;
            }
        }
        return position;
    }

    /**
     * Check whether the selected value is a boolean true. It is used only for
     * TYPE_BOOLEAN attribute types
     * 
     * @return
     */
    public boolean isBooleanTrueValue() {
        return isBooleanTrueValue(getSelectedValue());
    }

    /**
     * Check whether the selected value is a boolean true. It is used only for
     * TYPE_BOOLEAN attribute types
     * 
     * @param value the value to check
     * @return
     */
    public static boolean isBooleanTrueValue(String value) {
        return TextUtils.equals(value, CustomAttribute.TYPE_BOOLEAN_TRUE_VALUE);
    }

    /**
     * Get the custom attribute container which is the corresponding view parent
     * 
     * @return
     */
    public View getContainerView() {
        return (View) getCorrespondingView().getParent();
    }

    /**
     * Mark the attribute container with the colored background
     * 
     * @param marker the container marker to perform mark operation
     */
    public void markAttributeContainer(ContainerMarker marker) {
        marker.mark(getContainerView());
    }

    /**
     * Clear the attribute container background
     */
    @SuppressWarnings("deprecation")
    public void unmarkAttributeContainer() {
        if (CommonUtils.isJellyBeanOrHigher()) {
            // setBackground method is available only since Android JB
            getContainerView().setBackground(null);
        } else {
            getContainerView().setBackgroundDrawable(null);
        }
    }

    @Override
    public CustomAttribute clone() {
        CustomAttribute result = new CustomAttribute();
        result.mSelectedValue = mSelectedValue;
        result.mType = mType;
        result.mIsRequired = mIsRequired;
        result.mMainLabel = mMainLabel;
        result.mCode = mCode;
        result.mAttributeID = mAttributeID;
        result.mHint = mHint;
        result.mConfigurable = mConfigurable;
        result.mUseForSearch = mUseForSearch;
        result.mReadOnly = mReadOnly;
        result.mAddNewOptionsAllowed = mAddNewOptionsAllowed;
        result.mHtmlAllowedOnFront = mHtmlAllowedOnFront;
        result.mContentType = mContentType;
        result.mInputMethod = mInputMethod;
        result.mAlternateInputMethods = mAlternateInputMethods == null ? null
                : new ArrayList<InputMethod>(mAlternateInputMethods);
        // copy options
        result.mOptions = cloneOptions();
        return result;
    }

    /**
     * Clone attribute options list with the each option cloned inside
     * 
     * @return cloned attribute options ready for modification
     */
    public ArrayList<CustomAttributeOption> cloneOptions() {
        if (mOptions != null) {
            ArrayList<CustomAttributeOption> copiedOptions = new ArrayList<CustomAttributeOption>(
                    mOptions.size());
            // iterate through source options and clone each option
            for (CustomAttributeOption option : mOptions) {
                copiedOptions.add(option.clone());
            }
            return copiedOptions;
        }
        return null;
    }

    /**
     * Check whether this attribute is the same as another attribute. The
     * attribute id, type and code will be checked for coincidence
     * 
     * @param attribute the attribute to check for coincidence
     * @return true if the attribute parameter has same id, type and code
     */
    public boolean isSameAttribute(CustomAttribute attribute) {
        return TextUtils.equals(attribute.getAttributeID(), getAttributeID())
                && TextUtils.equals(attribute.getType(), getType())
                && TextUtils.equals(attribute.getCode(), getCode());
    }

    /**
     * Copy options to this attribute from another attribute but preserve the
     * current selected value
     * 
     * @param attribute the attribute to copy options from
     * @param updateView whether the view should be updated after the options
     *            copying is done
     */
    public void copyOptionsButPreserveValue(CustomAttribute attribute, boolean updateView) {
        // remember the current attribute value
        String value = getSelectedValue();
        // copy options from the new product custom attribute which
        // includes newly created options to keep data in the source
        // attribute up to date
        setOptions(attribute.cloneOptions());
        // restore the attribute value which was broken after the
        // options copy operation
        setSelectedValue(value, updateView);
    }
    
    /**
     * Check whether the custom attribute complies all the required conditions
     * so the text copied from the Internet search can be appended to
     * 
     * @param customAttribute
     * @return true if attribute is not null and is not read only and copy from
     *         Internet search input method is allowed for it and has type TEXT
     *         or TEXTAREA and is not of content type WEB_ADDRESS, false
     *         otherwise
     */
    public static boolean canAppendTextFromInternetSearch(CustomAttribute customAttribute) {
        // if attribute is not null and is not read only and copy from Internet
        // search input method is allowed for it and has type TEXT or TEXTAREA
        // and is not of content type WEB_ADDRESS
        return customAttribute != null
                && !customAttribute.isReadOnly()
                && customAttribute
                        .hasDefaultOrAlternateInputMethod(InputMethod.COPY_FROM_INTERNET_SEARCH)
                && !customAttribute.hasContentType(ContentType.WEB_ADDRESS)
                && (customAttribute.isOfType(CustomAttribute.TYPE_TEXT) || customAttribute
                        .isOfType(CustomAttribute.TYPE_TEXTAREA));
    }

    /**
     * The predefined container markers which may be used in the
     * markAttributeContainer method
     */
    public enum ContainerMarkers implements ContainerMarker {
    	/**
    	 * The container marker for the preselected attributes.
    	 */
        PRESELECTED(new ContainerMarker() {
    
            @Override
            public void mark(View v) {
                v.setBackgroundColor(CommonUtils
                        .getColorResource(R.color.custom_attribute_marked_background));
            }
    
        }), 
        /**
         * The container marker for the required attributes.
         */
        REQUIRED(new ContainerMarker() {
    
            @Override
            public void mark(View v) {
                v.setBackgroundColor(CommonUtils
                        .getColorResource(R.color.custom_attribute_required_marked_background));
            }
    
        }), ;
        
        /**
         * The {@link ContainerMarker} to wrap around
         */
        private ContainerMarker mContainerMarker;
    
        /**
         * @param containerMarker the {@link ContainerMarker} to wrap around
         */
        ContainerMarkers(ContainerMarker containerMarker) {
            mContainerMarker = containerMarker;
        }
    
        @Override
        public void mark(View v) {
            // delegate call to the wrapping ContainerMarker
            mContainerMarker.mark(v);
        }
    
    }

    /**
     * Simple interface for the different custom attribute container markers
     * should implement so they may be used in the markAttributeContainer method
     */
    public static interface ContainerMarker {
        public void mark(View v);
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
        out.writeString(mSelectedValue);
        out.writeString(mType);
        out.writeByte((byte) (mIsRequired ? 1 : 0));
        out.writeString(mMainLabel);
        out.writeString(mCode);
        out.writeString(mAttributeID);
        out.writeString(mHint);
        out.writeByte((byte) (mConfigurable ? 1 : 0));
        out.writeByte((byte) (mUseForSearch ? 1 : 0));
        out.writeByte((byte) (mReadOnly ? 1 : 0));
        out.writeByte((byte) (mAddNewOptionsAllowed ? 1 : 0));
        out.writeByte((byte) (mHtmlAllowedOnFront ? 1 : 0));
        out.writeParcelable(mContentType, flags);
        out.writeParcelable(mInputMethod, flags);
        out.writeTypedList(mAlternateInputMethods);
        out.writeTypedList(mOptions);
    }

    public static final Parcelable.Creator<CustomAttribute> CREATOR = new Parcelable.Creator<CustomAttribute>() {
        @Override
        public CustomAttribute createFromParcel(Parcel in) {
            return new CustomAttribute(in);
        }

        @Override
        public CustomAttribute[] newArray(int size) {
            return new CustomAttribute[size];
        }
    };

    private CustomAttribute(Parcel in) {
        mSelectedValue = in.readString();
        mType = in.readString();
        mIsRequired = in.readByte() == 1;
        mMainLabel = in.readString();
        mCode = in.readString();
        mAttributeID = in.readString();
        mHint = in.readString();
        mConfigurable = in.readByte() == 1;
        mUseForSearch = in.readByte() == 1;
        mReadOnly = in.readByte() == 1;
        mAddNewOptionsAllowed = in.readByte() == 1;
        mHtmlAllowedOnFront = in.readByte() == 1;
        mContentType = in.readParcelable(CustomAttribute.class.getClassLoader());
        mInputMethod = in.readParcelable(CustomAttribute.class.getClassLoader());
        mAlternateInputMethods = in.createTypedArrayList(InputMethod.CREATOR);
        mOptions = in.createTypedArrayList(CustomAttributeOption.CREATOR);
    }
}
