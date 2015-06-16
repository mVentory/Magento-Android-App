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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.AbsProductActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.CustomAttribute.ContentType;
import com.mageventory.model.CustomAttribute.CustomAttributeOption;
import com.mageventory.model.CustomAttribute.InputMethod;
import com.mageventory.model.util.AbstractCustomAttributeViewUtils;
import com.mageventory.model.util.ProductUtils;
import com.mageventory.settings.Settings;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.run.CallableWithParameterAndResult;
import com.mventory.R;

public class CustomAttributesList implements Serializable, MageventoryConstants {
    private static final long serialVersionUID = 4L;

    private List<CustomAttribute> mCustomAttributeList;
    /**
     * The attribute code / attribute map for easier search of the custom
     * attribute by its code. Used for compound name formatting.
     */
    private Map<String, CustomAttribute> mCustomAttributeMap;
    /**
     * Storage for special attributes defined in the
     * {@link Product#SPECIAL_ATTRIBUTES}
     */
    private Map<String, CustomAttribute> mSpecialCustomAttributes;
    private String mCompoundNameFormatting;
    private int mSetID;

    /* Things we don't serialize */
    private transient ViewGroup mParentViewGroup;
    private transient LayoutInflater mInflater;
    private transient AbsProductActivity mActivity;
    private transient OnNewOptionTaskEventListener mNewOptionListener;
    private transient Settings mSettings;
    private transient boolean mProductEdit;
    /**
     * Reference to the {@link OnAttributeValueChangedListener} which should be
     * called when user manually changes attribute value
     */
    private transient OnAttributeValueChangedListener mOnAttributeValueChangedByUserInputListener;
    /**
     * Reference to additional custom attribute view initializer which may be
     * used in different places where {@link CustomAttributesList} is
     * constructed
     */
    private transient AttributeViewAdditionalInitializer mAttributeViewAdditionalInitializer;

    public List<CustomAttribute> getList() {
        return mCustomAttributeList;
    }

    /**
     * Get the special custom attributes which are absent in the custom
     * attributes list. The list of special attribute coes may be found here
     * {@link Product#SPECIAL_ATTRIBUTES}
     * 
     * @return
     */
    public Map<String, CustomAttribute> getSpecialCustomAttributes() {
        return mSpecialCustomAttributes;
    }

    /**
     * Get the special custom attribute with the attribute code if exists
     * 
     * @param attributeCode the special custom attribute code
     * @return {@link CustomAttribute} with the specified attribute code if
     *         found, null otherwise
     */
    public CustomAttribute getSpecialCustomAttribute(String attributeCode) {
        return mSpecialCustomAttributes == null ? null : mSpecialCustomAttributes
                .get(attributeCode);
    }

    public CustomAttributesList(AbsProductActivity activity, ViewGroup parentViewGroup,
            OnNewOptionTaskEventListener listener,
            OnAttributeValueChangedListener onAttributeValueChangedByUserInputListener,
            AttributeViewAdditionalInitializer customAttributeViewAdditionalInitializer,
            boolean productEdit) {
        mParentViewGroup = parentViewGroup;
        mActivity = activity;
        mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mNewOptionListener = listener;
        mOnAttributeValueChangedByUserInputListener = onAttributeValueChangedByUserInputListener;
        mAttributeViewAdditionalInitializer = customAttributeViewAdditionalInitializer;
        setNameHint();
        mSettings = new Settings(activity);
        mProductEdit = productEdit;
    }

    /*
     * Save the list of custom attributes in the cache for the user to be able
     * to retrieve it later. We are setting some UI stuff to null here and they
     * will be reset when the list is reloaded from the cache.
     */
    public void saveInCache() {
        String url = mSettings.getUrl();

        /* Don't want to serialize this */
        mParentViewGroup = null;
        mInflater = null;
        mActivity = null;
        mNewOptionListener = null;
        mSettings = null;

        if (mCustomAttributeList != null)
        {
            for (CustomAttribute elem : mCustomAttributeList) {
                /* Don't want to serialize this */
                elem.setCorrespondingView(null);
                elem.setAttributeLoadingControl(null);
                elem.setHintView(null);
            }
        }
        JobCacheManager.storeLastUsedCustomAttribs(this, url);
    }

    /*
     * This is used when user wants to load last used custom attribute list (we
     * have it stored in the cache so we have to load it).
     */
    public static CustomAttributesList loadFromCache(AbsProductActivity activity,
            ViewGroup parentViewGroup,
            OnNewOptionTaskEventListener listener, String url, CustomAttributesList currentList) {
        CustomAttributesList c = JobCacheManager.restoreLastUsedCustomAttribs(url);

        if (c != null) {

            if (currentList != null && currentList.getList() != null)
            {
                for (CustomAttribute attribCurrent : currentList.getList())
                {
                    boolean exists = false;
                    for (CustomAttribute attribFromCache : c.getList())
                    {
                        if (attribCurrent.getCode().equals(attribFromCache.getCode()))
                        {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists)
                    {
                        c.getList().add(attribCurrent);
                    }
                }
            }

            c.mParentViewGroup = parentViewGroup;
            c.mActivity = activity;
            c.mInflater = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            c.mNewOptionListener = listener;
            c.populateViewGroup();
            c.mSettings = new Settings(activity);
        }

        return c;
    }

    /*
     * In case the user refreshes a list of custom attributes a new list of
     * attributes is created. The problem is we want to preserve the input from
     * the previous list. This function is called for each corresponding pair of
     * attributes from old and new list and tries to copy the user input from
     * old attribute to the new one.
     */
    private static void restoreAttributeValue(CustomAttribute newAttribute,
            CustomAttribute oldAttribute)
    {
        /*
         * If the new attribute we're creating is of any of these types this
         * means it has options and we're going to try to reset them as they
         * were in the old attribute.
         */
        if (newAttribute.isOfType(CustomAttribute.TYPE_BOOLEAN)
                || newAttribute.isOfType(CustomAttribute.TYPE_SELECT)
                || newAttribute.isOfType(CustomAttribute.TYPE_DROPDOWN)
                || newAttribute.isOfType(CustomAttribute.TYPE_MULTISELECT)) {

            /*
             * Iterate over the options from the old attribute and try to find
             * the selected ones.
             */
            for (CustomAttributeOption option : oldAttribute.getOptions()) {
                if (option.getSelected() == true) {
                    /*
                     * We found a selected option from the old attribute. We
                     * need to find a corresponding option from the new
                     * attribute now that corresponds to the selected one.
                     */
                    for (CustomAttributeOption optionNew : newAttribute.getOptions()) {
                        /*
                         * If the ids of both options are equal this means it's
                         * the same attribute option and we can set the option
                         * selected in the new attribute which means user input
                         * will be preserved.
                         */
                        if (optionNew.getID().equals(option.getID())) {

                            /*
                             * Clear all the options in case of a dropdown-like
                             * field before selecting a single option.
                             */
                            if (newAttribute.isOfType(CustomAttribute.TYPE_BOOLEAN)
                                    || newAttribute.isOfType(CustomAttribute.TYPE_SELECT)
                                    || newAttribute.isOfType(CustomAttribute.TYPE_DROPDOWN))
                            {
                                for (CustomAttributeOption optionNew2 : newAttribute.getOptions())
                                {
                                    optionNew2.setSelected(false);
                                }
                            }

                            optionNew.setSelected(true);
                        }
                    }
                }
            }
        } else {
            /*
             * This is an attribute without the options. It is just enough to
             * copy the selected value string in this case.
             */
            newAttribute.setSelectedValue(oldAttribute.getSelectedValue(), false);
        }
    }

    /**
     * Convert a custom attribute data from the server to a more friendly piece
     * of data (an instance of CustomAttribute class)
     * 
     * @param map representing attribute information
     * @param listCopy
     * @return
     */
    public static CustomAttribute createCustomAttribute(Map<String, Object> map,
            List<CustomAttribute> listCopy) {
        CustomAttribute customAttr = new CustomAttribute();

        customAttr.setType((String) map.get(MAGEKEY_ATTRIBUTE_TYPE));
        customAttr.setMainLabel((String) map.get(MAGEKEY_ATTRIBUTE_LABEL));
        customAttr.setCode((String) map.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE));
        customAttr.setIsRequired(((String) map.get(MAGEKEY_ATTRIBUTE_REQUIRED)).equals("1") ? true
                : false);
        if (customAttr.isOfCode(MAGEKEY_PRODUCT_SKU)) {
            // the SKU attribute is always required but in case it is empty it
            // will be/ automatically generated. So no need to notify user about
            // the attribute is required by the red asterisk
            customAttr.setIsRequired(false);
        }
        customAttr.setAttributeID((String) map.get(MAGEKEY_ATTRIBUTE_ID));
        customAttr.setHint((String) map.get(MAGEKEY_ATTRIBUTE_HINT));
        customAttr.setConfigurable(JobCacheManager.safeParseInt(map
                .get(MAGEKEY_ATTRIBUTE_CONFIGURABLE)) == 1);
        Object useForSearchObj = map.get(MAGEKEY_ATTRIBUTE_USE_FOR_SEARCH);
        if (customAttr.isOfCode(MAGEKEY_PRODUCT_NAME) && useForSearchObj == null) {
            // default value for the name attribute is true
            useForSearchObj = 1;
        }
        customAttr.setUseForSearch(JobCacheManager.safeParseInt(useForSearchObj) == 1);
        customAttr
                .setReadOnly(JobCacheManager.safeParseInt(map.get(MAGEKEY_ATTRIBUTE_READ_ONLY)) == 1);
        customAttr.setAddNewOptionsAllowed(JobCacheManager.safeParseInt(
                map.get(MAGEKEY_ATTRIBUTE_ADD_NEW_VALUES), 1) == 1);
        customAttr.setHtmlAllowedOnFront(JobCacheManager.safeParseInt(
                map.get(MAGEKEY_ATTRIBUTE_IS_HTML_ALLOWED_ON_FRONT)) == 1);
        customAttr.setContentType(ContentType.getContentTypeForCode(JobCacheManager.safeParseInt(
                map.get(MAGEKEY_ATTRIBUTE_CONTENT_TYPE), ContentType.TEXT.getCode())));
        customAttr.setInputMethod(InputMethod.getInputMethodForCode(JobCacheManager.safeParseInt(
                map.get(MAGEKEY_ATTRIBUTE_INPUT_METHOD), -1)));
        if (customAttr.getInputMethod() == null) {
            // if server didn't return input method information set the default
            // values
            if (customAttr.isOfCode(MAGEKEY_PRODUCT_SKU)
                    || customAttr.isOfCode(MAGEKEY_PRODUCT_BARCODE)) {
                // if attribute is SKU or Barcode then use default scanner input
                // method
                customAttr.setInputMethod(InputMethod.SCANNER);
            } else if (customAttr.isOfCode(MAGEKEY_PRODUCT_WEIGHT)) {
                // if attribute is weight then use default numeric keyboard
                // input method
                customAttr.setInputMethod(InputMethod.NUMERIC_KEYBOARD);
            } else {
                customAttr.setInputMethod(InputMethod.NORMAL_KEYBOARD);
            }
        }
        String alternateInputMethod = (String) map.get(MAGEKEY_ATTRIBUTE_ALTERNATE_INPUT_METHOD);
        if (!TextUtils.isEmpty(alternateInputMethod)) {
            // alternateInputMethods is a comma separated string with the input
            // method codes
            for (String inputMethodString : alternateInputMethod.split(",")) {
                InputMethod inputMethod = InputMethod.getInputMethodForCode(JobCacheManager
                        .safeParseInt(inputMethodString));
                if (inputMethod != null) {
                    customAttr.addAlternateInputMethod(inputMethod);
                }
            }
        }
        if (customAttr.getAlternateInputMethods() == null
                || customAttr.getAlternateInputMethods().isEmpty()) {
            // if server didn't return alternative input method information set
            // the default values
            if (customAttr.isOfCode(MAGEKEY_PRODUCT_SKU)
                    || customAttr.isOfCode(MAGEKEY_PRODUCT_BARCODE)) {
                // if attribute is SKU or Barcode then use normal keyboard as
                // alternative input method only
                customAttr.addAlternateInputMethod(InputMethod.NORMAL_KEYBOARD);
            } else if (customAttr.isOfCode(MAGEKEY_PRODUCT_WEIGHT)) {
                // if attribute is weight then use numeric keyboard as
                // alternative input method
                customAttr.addAlternateInputMethod(InputMethod.NUMERIC_KEYBOARD);
            } else {
                customAttr.addAlternateInputMethod(InputMethod.NORMAL_KEYBOARD);
                customAttr.addAlternateInputMethod(InputMethod.SCANNER);
                customAttr.addAlternateInputMethod(InputMethod.COPY_FROM_INTERNET_SEARCH);
                customAttr.addAlternateInputMethod(InputMethod.COPY_FROM_ANOTHER_PRODUCT);
            }
        }
        customAttr.setOptionsFromServerResponse(JobCacheManager
                .getObjectArrayFromDeserializedItem(map.get(MAGEKEY_ATTRIBUTE_OPTIONS)));
        List<CustomAttributeOption> options = customAttr.getOptions();
        // TYPE_BOOLEAN option may come from the server without an options.
        // Fill the attribute with the default options in such case
        if (customAttr.isOfType(CustomAttribute.TYPE_BOOLEAN) && options.isEmpty()) {
            options.add(new CustomAttributeOption(CustomAttribute.TYPE_BOOLEAN_FALSE_VALUE,
                    CustomAttribute.TYPE_BOOLEAN_FALSE_VALUE));
            options.add(new CustomAttributeOption(CustomAttribute.TYPE_BOOLEAN_TRUE_VALUE,
                    CustomAttribute.TYPE_BOOLEAN_TRUE_VALUE));
        }

        if (customAttr.isOfType(CustomAttribute.TYPE_BOOLEAN)
                || customAttr.isOfType(CustomAttribute.TYPE_SELECT)
                || customAttr.isOfType(CustomAttribute.TYPE_DROPDOWN)) {
            customAttr.setSelectedValue(null, true, false);
        }

        /* If we're just refreshing attributes - try to keep user entered data. */
        if (listCopy != null) {
            /*
             * Find the attribute in the previous list that corresponds to the
             * attribute we are creating now.
             */
            for (CustomAttribute elem : listCopy) {
                /*
                 * If they have the same id and the same code they are the same
                 * attribute.
                 */
                if (elem.getAttributeID().equals(customAttr.getAttributeID())
                        && elem.getType().equals(customAttr.getType())
                        && elem.getCode().equals(customAttr.getCode())) {

                    restoreAttributeValue(customAttr, elem);
                    break;
                }
            }
        } else {
            String defaultValue = (String) map.get(MAGEKEY_ATTRIBUTE_DEFAULT_VALUE);

            /*
             * See http://code.google.com/p/mageventory/issues/detail?id=277 for
             * explanation of this if statement.
             */
            if (defaultValue != null && defaultValue.length() > 0 && defaultValue.charAt(0) != '~')
            {
                customAttr.setSelectedValue(defaultValue, false);
            }
        }

        return customAttr;
    }

    /*
     * Convert server response to a format more friendly to a programmer. All
     * other operations on custom attributes will be performed on that format
     * and not directly on the list of maps from the server.
     */
    public void loadFromAttributeList(List<Map<String, Object>> attrList, int setID) {
        mSetID = setID;

        List<CustomAttribute> customAttributeListCopy = null;
        // previous special attributes holder to copy attribute values from
        List<CustomAttribute> specialCustomAttributesListCopy = mSpecialCustomAttributes == null ? null
                : new ArrayList<CustomAttribute>(mSpecialCustomAttributes.values());
        if (mCustomAttributeList != null) {
            customAttributeListCopy = mCustomAttributeList;
        }

        mCustomAttributeList = new ArrayList<CustomAttribute>();
        mCustomAttributeMap = new LinkedHashMap<String, CustomAttribute>();
        mSpecialCustomAttributes = new LinkedHashMap<String, CustomAttribute>();
        mCompoundNameFormatting = null;

        Map<String, Object> thumbnail = null;

        for (Map<String, Object> elem : attrList) {
            String attributeCode = (String) elem.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE);
            // if this is special attribute then add it to
            // mSpecialCustomAttributes list and continue the loop
            if (TextUtils.equals(attributeCode, Product.MAGEKEY_PRODUCT_BARCODE)
                    || Product.SPECIAL_ATTRIBUTES.contains(attributeCode)) {
                CustomAttribute customAttribute = createCustomAttribute(elem,
                        specialCustomAttributesListCopy);
                mSpecialCustomAttributes.put(attributeCode, customAttribute);
                continue;
            }

            if (((String) elem.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE)).contains("_thumb")) {
                thumbnail = elem;
                continue;
            }

            Boolean isFormatting = (Boolean) elem.get(MAGEKEY_ATTRIBUTE_IS_FORMATTING_ATTRIBUTE);

            if (isFormatting != null && isFormatting.booleanValue() == true) {
                mCompoundNameFormatting = (String) elem.get(MAGEKEY_ATTRIBUTE_DEFAULT_VALUE);
            } else {
                CustomAttribute customAttribute = createCustomAttribute(elem,
                        customAttributeListCopy);
                mCustomAttributeList.add(customAttribute);
                mCustomAttributeMap.put(attributeCode, customAttribute);
            }
        }

        if (thumbnail != null) {
            mCustomAttributeList.add(createCustomAttribute(thumbnail, customAttributeListCopy));
        }

        populateViewGroup();
    }

    /*
     * Some codes are not needed in compound name in case the values selected by
     * the user are not very meaningful like "none", "Other", etc.
     */
    private boolean isNeededInCompoundName(String value) {
        if (value == null ||
                value.equals("") ||
                value.equalsIgnoreCase("other") ||
                value.equalsIgnoreCase("none") ||
                value.equalsIgnoreCase(CustomAttribute.NOT_AVAILABLE_VALUE)) {
            return false;
        }

        return true;
    }

    /**
     * Get compound name based on the formatting string received from the
     * server. If the constructed compound name turns out to be empty - return
     * CustomAttribute.NOT_AVAILABLE_VALUE instead.
     * 
     * @return
     */
    public String getCompoundName() {
        String out = null;
        if (mCompoundNameFormatting != null) {
            
            CallableWithParameterAndResult<String, String> getLabelCallable = new CallableWithParameterAndResult<String, String>() {

                @Override
                public String call(String code) {
                    CustomAttribute attribute = mCustomAttributeMap.get(code);
                    if (attribute != null) {
                        // if attribute with such code exists
                        String selectedValue = attribute.getUserReadableSelectedValue();

                        /*
                         * Check if a given attribute is needed in compound
                         * name. It may not be needed because it is empty,
                         * contains "Other", "none", etc. Remove not needed
                         * ones.
                         */
                        if (!isNeededInCompoundName(selectedValue)) {
                            code = "";
                        } else {
                            code = selectedValue;
                        }
                    }
                    return code;
                }
            };
            out = CommonUtils.replaceCodesWithValues(mCompoundNameFormatting, getLabelCallable);
            out = filterCompoundName(out);
        }
        if (out != null && out.length() > 0) {
            return ProductUtils.removeDuplicateWordsFromName(out);
        } else {
            return "";
        }

    }

    /**
     * Filter compound name from the sequential insignificant characters, empty
     * braces, leading and trailing insignificant characters
     * 
     * @param str string to filter
     * @return filtered string value
     */
    public static String filterCompoundName(String str) {
        String insignificantCharacters = "[\\s,./\\|-]";
        str = str
                // remove all braces with no values inside
                .replaceAll("\\(" + insignificantCharacters + "*\\)", "")
                // replace all sequential spaces and insignificant
                // characters with the single space
                .replaceAll("\\s" + insignificantCharacters + "+", " ")
                // trim leading and trailing spaces
                .trim()
                // remove leading insignificant characters
                .replaceAll("^" + insignificantCharacters + "+", "")
                // remove trailing insignificant characters
                .replaceAll(insignificantCharacters + "+$", "");
        return str;
    }

    /*
     * Get the formatting string we received from the server and replace
     * attribute codes with labels.
     */
    public String getUserReadableFormattingString() {
        String out = null;
        if (mCompoundNameFormatting != null) {
            CallableWithParameterAndResult<String, String> getLabelCallable = new CallableWithParameterAndResult<String, String>() {

                @Override
                public String call(String code) {
                    CustomAttribute attribute = mCustomAttributeMap.get(code);
                    if (attribute != null) {
                        code = attribute.getMainLabel();
                    }
                    return code;
                }
            };
            out = CommonUtils.replaceCodesWithValues(mCompoundNameFormatting, getLabelCallable);
        }

        return out;
    }

    /**
     * Get the set id
     * 
     * @return
     */
    public int getSetId() {
        return mSetID;
    }

    /*
     * After we constructed a list of attributes in memory we have to construct
     * a list of views corresponding to these attriubtes. This function
     * constructs those views.
     */
    private void populateViewGroup() {
        mParentViewGroup.removeAllViews();

        CustomAttributeViewUtils customAttributeViewUtils = getCustomAttributViewUtils();
        for (CustomAttribute elem : mCustomAttributeList) {
            View v = newAtrEditView(elem, customAttributeViewUtils);
            if (mAttributeViewAdditionalInitializer != null)
            {
                mAttributeViewAdditionalInitializer.processCustomAttribute(elem);
            }
            if (v != null) {
                mParentViewGroup.addView(v);
            }
        }
        setNameHint();
    }

    /**
     * Get the instance of {@link CustomAttributeViewUtils}
     * 
     * @return
     */
    public CustomAttributeViewUtils getCustomAttributViewUtils() {
        return new CustomAttributeViewUtils();
    }

    /**
     * Set the default value hint for the name attribute generated using
     * formatting attribute
     * 
     * @return the generated hint value
     */
    public String setNameHint() {
        String nameHint = null;
        CustomAttribute attribute = getSpecialCustomAttribute(MAGEKEY_PRODUCT_NAME);
        if (attribute != null && attribute.getCorrespondingEditTextView() != null) {
            nameHint = getCompoundName();
            attribute.getCorrespondingEditTextView().setHint(nameHint);
        }
        return nameHint;
    }

    /*
     * A listener which contains functions which are called when a new option
     * starts being created or when it gets created.
     */
    public static interface OnNewOptionTaskEventListener {
        void OnAttributeCreationStarted();

        void OnAttributeCreationFinished(String attributeName, String newOptionName, boolean success);
    }

    /**
     * Create a view corresponding to the custom attribute in order to show it
     * to the user.
     * 
     * @param customAttribute
     * @param customAttributeViewUtils
     * @return
     */
    public View newAtrEditView(final CustomAttribute customAttribute,
            CustomAttributeViewUtils customAttributeViewUtils) {

        final View v = mInflater.inflate(R.layout.product_attribute_edit, null);
        customAttributeViewUtils.initAtrEditView(v, customAttribute);
        return v;
    }

    /**
     * Implementation of {@link AbstractCustomAttributeViewUtils}
     */
    public class CustomAttributeViewUtils extends AbstractCustomAttributeViewUtils {

        CustomAttributeViewUtils() {
            super(mActivity.inputCache, true, true, mActivity,
                    mOnAttributeValueChangedByUserInputListener, mNewOptionListener,
                    mCustomAttributeList, Integer.toString(mSetID), mActivity);
        }

    }
    /**
     * An interface for the attribute value changed event listener
     * implementation
     */
    public static interface OnAttributeValueChangedListener {
        public void attributeValueChanged(String oldValue, String newValue,
                CustomAttribute attribute);
    }

    /**
     * An interface for the custom additional attribute view initialization
     */
    public static interface AttributeViewAdditionalInitializer {
        void processCustomAttribute(CustomAttribute attribute);
    }
}
