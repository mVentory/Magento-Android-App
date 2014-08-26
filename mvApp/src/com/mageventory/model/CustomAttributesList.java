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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.AbsProductActivity;
import com.mageventory.dialogs.CustomAttributeValueSelectionDialog;
import com.mageventory.dialogs.CustomAttributeValueSelectionDialog.OnCheckedListener;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.CustomAttribute.CustomAttributeOption;
import com.mageventory.model.util.ProductUtils;
import com.mageventory.resprocessor.ProductAttributeAddOptionProcessor;
import com.mageventory.settings.Settings;
import com.mageventory.tasks.CreateOptionTask;
import com.mageventory.util.InputCacheUtils;
import com.reactor.gesture_input.GestureInputActivity;

public class CustomAttributesList implements Serializable, MageventoryConstants {
    private static final long serialVersionUID = 3L;

    private List<CustomAttribute> mCustomAttributeList;
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
    private transient EditText mName;
    private transient OnNewOptionTaskEventListener mNewOptionListener;
    private transient Settings mSettings;
    private transient boolean mProductEdit;
    private transient Runnable mOnEditDoneRunnable;
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

    public CustomAttributesList(AbsProductActivity activity, ViewGroup parentViewGroup,
            EditText nameView, OnNewOptionTaskEventListener listener,
            OnAttributeValueChangedListener onAttributeValueChangedByUserInputListener,
            AttributeViewAdditionalInitializer customAttributeViewAdditionalInitializer,
            boolean productEdit) {
        mParentViewGroup = parentViewGroup;
        mActivity = activity;
        mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mName = nameView;
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
        mName = null;
        mNewOptionListener = null;
        mSettings = null;

        if (mCustomAttributeList != null)
        {
            for (CustomAttribute elem : mCustomAttributeList) {
                /* Don't want to serialize this */
                elem.setCorrespondingView(null);
                elem.setNewOptionSpinningWheel(null);
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
            ViewGroup parentViewGroup, EditText nameView,
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
            c.mName = nameView;
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
        customAttr.setIsRequired(((String) map.get(MAGEKEY_ATTRIBUTE_REQUIRED)).equals("1") ? true
                : false);
        customAttr.setMainLabel((String) map.get(MAGEKEY_ATTRIBUTE_LABEL));
        customAttr.setCode((String) map.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE));
        customAttr.setAttributeID((String) map.get(MAGEKEY_ATTRIBUTE_ID));
        customAttr.setConfigurable(JobCacheManager.safeParseInt(map
                .get(MAGEKEY_ATTRIBUTE_CONFIGURABLE)) == 1);
        customAttr.setUseForSearch(JobCacheManager.safeParseInt(map
                .get(MAGEKEY_ATTRIBUTE_USE_FOR_SEARCH)) == 1);
        customAttr.setCopyFromSearch(JobCacheManager.safeParseInt(map
                .get(MAGEKEY_ATTRIBUTE_COPY_FROM_SEARCH)) == 1);
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
            if (options != null && !options.isEmpty()) {
                customAttr.setOptionSelected(0, true, false);
            }
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
     * Copy all serializable data (which in this case is everything except View
     * classes from one CustomAttribute to another)
     */
    private void copySerializableData(CustomAttribute from, CustomAttribute to) {
        to.setType(from.getType());
        to.setIsRequired(from.getIsRequired());
        to.setConfigurable(from.isConfigurable());
        to.setUseForSearch(from.isUseForSearch());
        to.setCopyFromSearch(from.isCopyFromSearch());
        to.setMainLabel(from.getMainLabel());
        to.setCode(from.getCode());
        to.setOptions(from.getOptions());
        to.setAttributeID(from.getAttributeID());
    }

    /*
     * Update a single custom attribute's options with new data from the cache.
     * Also make this option selected + update the for user to see the changes.
     */
    public void updateCustomAttributeOptions(CustomAttribute attr,
            List<Map<String, Object>> customAttrsList,
            String newOptionToSet) {

        if (customAttrsList == null)
            return;
        String oldValue = attr.getSelectedValue();
        for (Map<String, Object> elem : customAttrsList) {
            if (TextUtils.equals((String) elem.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE),
                    attr.getCode())) {

                CustomAttribute updatedAttrib = createCustomAttribute(elem, mCustomAttributeList);
                copySerializableData(updatedAttrib, attr);

                int i = 0;
                for (CustomAttributeOption option : attr.getOptions()) {

                    if (ProductAttributeAddOptionProcessor.optionStringsEqual(option.getLabel(),
                            newOptionToSet)) {
                        attr.setOptionSelected(i, true, true);

                        break;
                    }
                    i++;
                }

                break;
            }
        }
        if (mOnAttributeValueChangedByUserInputListener != null) {
            mOnAttributeValueChangedByUserInputListener.attributeValueChanged(oldValue,
                    attr.getSelectedValue(), attr);
        }
    }

    /*
     * Convert server response to a format more friendly to a programmer. All
     * other operations on custom attributes will be performed on that format
     * and not directly on the list of maps from the server.
     */
    public void loadFromAttributeList(List<Map<String, Object>> attrList, int setID) {
        mSetID = setID;

        List<CustomAttribute> customAttributeListCopy = null;

        if (mCustomAttributeList != null) {
            customAttributeListCopy = mCustomAttributeList;
        }

        mCustomAttributeList = new ArrayList<CustomAttribute>();
        mSpecialCustomAttributes = new HashMap<String, CustomAttribute>();

        Map<String, Object> thumbnail = null;

        // flag to check whether at least one attribute has useForSearch
        // parameter specified
        boolean hasUseForSearch = false;
        // flag to check whether at least one attribute has copyFromSearch
        // parameter specified
        boolean hasCopyFromSearch = false;
        // reference to name attribute
        CustomAttribute nameAttribute = null;

        for (Map<String, Object> elem : attrList) {
            String attributeCode = (String) elem.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE);
            // if this is special attribute then add it to
            // mSpecialCustomAttributes list and continue the loop
            if (TextUtils.equals(attributeCode, Product.MAGEKEY_PRODUCT_BARCODE)
                    || Product.SPECIAL_ATTRIBUTES.contains(attributeCode)) {
                CustomAttribute customAttribute = createCustomAttribute(elem, null);
                hasUseForSearch |= customAttribute.isUseForSearch();
                hasCopyFromSearch |= customAttribute.isCopyFromSearch();
                // if this is a name attribute then store it for future
                // reference
                if (TextUtils.equals(customAttribute.getCode(), MAGEKEY_PRODUCT_NAME)) {
                    nameAttribute = customAttribute;
                }
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
                hasUseForSearch |= customAttribute.isUseForSearch();
                hasCopyFromSearch |= customAttribute.isCopyFromSearch();
                mCustomAttributeList.add(customAttribute);
            }
        }

        if (thumbnail != null) {
            mCustomAttributeList.add(createCustomAttribute(thumbnail, customAttributeListCopy));
        }
        // check whether nameAttribute parameters should be specified to
        // default values
        processNameAttributeDefaults(hasUseForSearch, hasCopyFromSearch, nameAttribute);

        populateViewGroup();
    }

    /**
     * Process name attribute. CHeck whether the useForSearch and copyFromSearch
     * properties should be specified to default true value. Occurs if there are
     * no any attributes marked for search and copy from search
     * 
     * @param hasUseForSearch is there any attributes marked to be used for
     *            search
     * @param hasCopyFromSearch is there any attributes marked to be copied from
     *            search
     * @param nameAttribute the name custom attribute
     */
    public static void processNameAttributeDefaults(boolean hasUseForSearch, boolean hasCopyFromSearch,
            CustomAttribute nameAttribute) {
        // check whether the name attribute requires adjusting of useForSearch
        // or copyFromSearch settings
        if (nameAttribute != null) {
            // if there are no attributes marked to be used for search then use
            // name attribute for such purpose by default
            if (!hasUseForSearch) {
                nameAttribute.setUseForSearch(true);
            }
            // if there are no attributes marked to be copied for search then
            // use name attribute for such purpose by default
            if (!hasCopyFromSearch) {
                nameAttribute.setCopyFromSearch(true);
            }
        }
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
                value.equalsIgnoreCase("n/a")) {
            return false;
        }

        return true;
    }

    private boolean isSignificantCharacter(char character)
    {
        String insignificantCharacters = ",./\\|- ";

        if (insignificantCharacters.indexOf(character) == -1)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /*
     * Remove the attribute code along with any () pair and trailing
     * insignificant characters.
     */
    private String removeCodeFromCompoundName(String compoundName, String code) {
        int indexBegin = compoundName.indexOf(code);
        int indexEnd;
        int leftParenthesis = -1;
        int rightParenthesis = -1;

        if (indexBegin != -1) {
            indexEnd = indexBegin + code.length();

            // Find left parenthesis
            for (int i = indexBegin - 1; i >= 0; i--)
            {
                char c = compoundName.charAt(i);

                if (c == '(')
                {
                    leftParenthesis = i;
                    break;
                }
                else if (c == ')' || isSignificantCharacter(c))
                {
                    break;
                }
            }

            // Find right parenthesis
            for (int i = indexEnd; i < compoundName.length(); i++)
            {
                char c = compoundName.charAt(i);

                if (c == ')')
                {
                    rightParenthesis = i;
                    break;
                }
                else if (c == '(' || isSignificantCharacter(c))
                {
                    break;
                }
            }

            /*
             * If we localized both parentheses then we'll remove them and
             * whatever is between them.
             */
            if (leftParenthesis != -1 && rightParenthesis != -1)
            {
                indexBegin = leftParenthesis;
                indexEnd = rightParenthesis + 1;
            }

            /* Remove insignificant trailing characters. */
            for (int i = indexEnd; i < compoundName.length(); i++)
            {
                if (!isSignificantCharacter(compoundName.charAt(i)))
                {
                    indexEnd = i + 1;
                }
                else
                {
                    break;
                }
            }

            compoundName = compoundName.replace(compoundName.substring(indexBegin, indexEnd), "");
        }

        return compoundName;
    }

    /*
     * Get compound name based on the formatting string received from the
     * server. If the constructed compound name turns out to be empty - return
     * "n/a" instead.
     */
    public String getCompoundName() {
        String out = null;
        if (mCompoundNameFormatting != null) {
            out = mCompoundNameFormatting;

            for (CustomAttribute ca : mCustomAttributeList) {
                String selectedValue = ca.getUserReadableSelectedValue();

                /*
                 * Check if a given attribute is needed in compound name. It may
                 * not be needed because it is empty, contains "Other", "none",
                 * etc. Remove not needed ones.
                 */
                if (!isNeededInCompoundName(selectedValue)) {
                    out = removeCodeFromCompoundName(out, ca.getCode());
                }
            }

            for (CustomAttribute ca : mCustomAttributeList) {
                String selectedValue = ca.getUserReadableSelectedValue();

                /*
                 * Check if a given attribute is needed in compound name. It may
                 * not be needed because it is empty, contains "Other", "none",
                 * etc. Replace needed attribute codes with their values.
                 */
                if (isNeededInCompoundName(selectedValue)) {
                    out = out.replace(ca.getCode(), selectedValue);
                }
            }

            /*
             * Remove all insignificant characters from the beginning and from
             * the end of the resulting string.
             */
            int left = 0, right = out.length() - 1;

            for (int i = 0; i < out.length(); i++)
            {
                if (!isSignificantCharacter(out.charAt(i)))
                {
                    left = i + 1;
                }
                else
                {
                    break;
                }
            }

            for (int i = out.length() - 1; i >= 0; i--)
            {
                if (!isSignificantCharacter(out.charAt(i)))
                {
                    right = i - 1;
                }
                else
                {
                    break;
                }
            }

            out = out.substring(left, right + 1);
        }

        if (out != null && out.length() > 0) {
            return ProductUtils.removeDuplicateWordsFromName(out);
        } else {
            return "n-a";
        }

    }

    /*
     * Get the formatting string we received from the server and replace
     * attribute codes with labels.
     */
    public String getUserReadableFormattingString() {
        String out = null;
        if (mCompoundNameFormatting != null) {
            out = mCompoundNameFormatting;

            for (CustomAttribute ca : mCustomAttributeList) {
                out = out.replace(ca.getCode(), ca.getMainLabel());
            }
        }

        return out;
    }

    /*
     * After we constructed a list of attributes in memory we have to construct
     * a list of views corresponding to these attriubtes. This function
     * constructs those views.
     */
    private void populateViewGroup() {
        mParentViewGroup.removeAllViews();

        for (CustomAttribute elem : mCustomAttributeList) {
            View v = newAtrEditView(elem);
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

    public void setNameHint() {
        if (mName != null)
            mName.setHint(getCompoundName());
    }

    /*
     * A listener which contains functions which are called when a new option
     * starts being created or when it gets created.
     */
    public static interface OnNewOptionTaskEventListener {
        void OnAttributeCreationStarted();

        void OnAttributeCreationFinished(String attributeName, String newOptionName, boolean success);
    }

    /* Shows a dialog for adding new option. */
    public void showAddNewOptionDialog(final CustomAttribute customAttribute) {
        final View textEntryView = mActivity.getLayoutInflater().inflate(
                R.layout.add_new_option_dialog, null);

        /*
         * User is not able to create a new option if one is currently being
         * created. I'm just checking if the spinning wheel is shown so that I
         * don't have to add another variable for tracking that.
         */
        if (customAttribute.getNewOptionSpinningWheel().getVisibility() == View.VISIBLE) {
            return;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);

        alert.setTitle("New option");
        alert.setMessage("Enter a name for a new option for \"" + customAttribute.getMainLabel()
                + "\" attribute.");
        alert.setView(textEntryView);

        final EditText editText = (EditText) textEntryView.findViewById(R.id.newOptionEditText);
        editText.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (hasFocus)
                {
                    InputMethodManager imm = (InputMethodManager) mActivity
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            }
        });

        alert.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                customAttribute.addNewOption(mActivity, editText.getText().toString());

                CreateOptionTask createOptionTask = new CreateOptionTask(mActivity,
                        customAttribute,
                        CustomAttributesList.this, editText.getText().toString(), "" + mSetID,
                        mNewOptionListener);
                createOptionTask.execute();

                InputMethodManager inputManager = (InputMethodManager) mActivity
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            }
        });

        final AlertDialog srDialog = alert.create();

        srDialog.setOnShowListener(new OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                final Button button = ((AlertDialog) dialog).getButton(Dialog.BUTTON_POSITIVE);
                button.setEnabled(false);

                editText.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (editText.getText().toString().trim().length() == 0)
                        {
                            button.setEnabled(false);
                        }
                        else
                        {
                            button.setEnabled(true);
                        }
                    }
                });

            }
        });

        srDialog.show();
    }

    private void showDatepickerDialog(final CustomAttribute customAttribute) {
        final OnDateSetListener onDateSetL = new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                monthOfYear += 1; // because it's from 0 to 11 for compatibility
                                  // reasons
                final String date = "" + monthOfYear + "/" + dayOfMonth + "/" + year;

                customAttribute.setSelectedValue(date, true);
                setNameHint();
            }
        };

        final Calendar c = Calendar.getInstance();

        // parse date if such is present
        try {
            final SimpleDateFormat f = new SimpleDateFormat("M/d/y");
            final Date d = f.parse(((EditText) customAttribute.getCorrespondingView()).getText()
                    .toString());
            c.setTime(d);
        } catch (Throwable ignored) {
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        final Dialog d = new DatePickerDialog(mActivity, onDateSetL, year, month, day);
        d.show();
    }

    private void showSingleSelectDialog(final CustomAttribute customAttribute) {
        List<String> optionLabels = customAttribute.getOptionsLabels();

        final String[] items = new String[optionLabels.size()];
        for (int i = 0; i < optionLabels.size(); i++) {
            items[i] = optionLabels.get(i);
        }

        // say which items should be checked on start
        // the default value should be -1 to support empty options attributes
        int selectedItemIdx = -1;
        for (int i = 0; i < customAttribute.getOptions().size(); i++) {
            if (customAttribute.getOptions().get(i).getSelected())
            {
                selectedItemIdx = i;
            }
        }

        CustomAttributeValueSelectionDialog dialog = new CustomAttributeValueSelectionDialog(
                mActivity, !customAttribute.isOfType(CustomAttribute.TYPE_BOOLEAN));
        dialog.initSingleSelectDialog(items, selectedItemIdx);

        dialog.setOnCheckedListener(new OnCheckedListener() {

            @Override
            public void onChecked(int position, boolean checked) {

                if (checked)
                {
                    if (position == -1) {
                        showAddNewOptionDialog(customAttribute);
                    } else {
                        String oldValue = customAttribute.getSelectedValue();
                        customAttribute.setOptionSelected(position, checked, false);
                        if (mOnAttributeValueChangedByUserInputListener != null) {
                            mOnAttributeValueChangedByUserInputListener.attributeValueChanged(
                                    oldValue, customAttribute.getSelectedValue(), customAttribute);
                        }
                        if (mOnEditDoneRunnable != null) {
                            mOnEditDoneRunnable.run();
                        }
                    }
                }
            }
        });

        dialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                ((EditText) customAttribute.getCorrespondingView()).setText(customAttribute
                        .getUserReadableSelectedValue());
                setNameHint();
            }
        });

        dialog.show();
    }

    private void showMultiselectDialog(final CustomAttribute customAttribute) {
        List<String> optionLabels = customAttribute.getOptionsLabels();

        final String[] items = new String[optionLabels.size()];
        for (int i = 0; i < optionLabels.size(); i++) {
            items[i] = optionLabels.get(i);
        }

        // say which items should be checked on start
        final boolean[] checkedItems = new boolean[customAttribute.getOptions().size()];
        for (int i = 0; i < customAttribute.getOptions().size(); i++) {
            checkedItems[i] = customAttribute.getOptions().get(i).getSelected();
        }

        CustomAttributeValueSelectionDialog dialog = new CustomAttributeValueSelectionDialog(
                mActivity, !customAttribute.isOfType(CustomAttribute.TYPE_BOOLEAN));
        dialog.initMultiSelectDialog(items, checkedItems);

        dialog.setOnCheckedListener(new OnCheckedListener() {

            @Override
            public void onChecked(int position, boolean checked) {
                if (position == -1) {
                    if (checked) {
                        showAddNewOptionDialog(customAttribute);
                    }
                } else {
                    String oldValue = customAttribute.getSelectedValue();
                    customAttribute.setOptionSelected(position, checked, false);
                    if (mOnAttributeValueChangedByUserInputListener != null) {
                        mOnAttributeValueChangedByUserInputListener.attributeValueChanged(oldValue,
                                customAttribute.getSelectedValue(), customAttribute);
                    }
                }
            }
        });

        dialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                ((EditText) customAttribute.getCorrespondingView()).setText(customAttribute
                        .getUserReadableSelectedValue());
                setNameHint();
            }
        });

        dialog.setRunOnOkButtonPressed(mOnEditDoneRunnable);

        dialog.show();
    }

    /**
     * Set the runnable which will be run after the editing done (either done
     * button pressed in editbox, ok button pressed in multiselect or item
     * selected in single select)
     * 
     * @param onEditDoneRunnable
     */
    public void setOnEditDoneRunnable(Runnable onEditDoneRunnable) {
        mOnEditDoneRunnable = onEditDoneRunnable;
    }

    /*
     * Create a view corresponding to the custom attribute in order to show it
     * to the user.
     */
    private View newAtrEditView(final CustomAttribute customAttribute) {

        final View v = mInflater.inflate(R.layout.product_attribute_edit, null);
        if (customAttribute.isOfType(CustomAttribute.TYPE_BOOLEAN)) {
            v.findViewById(R.id.stub_checkbox).setVisibility(View.VISIBLE);
        } else if (customAttribute.isOfType(CustomAttribute.TYPE_SELECT)
                || customAttribute.isOfType(CustomAttribute.TYPE_MULTISELECT)
                || customAttribute.isOfType(CustomAttribute.TYPE_DROPDOWN)) {
            v.findViewById(R.id.stub_dropdown).setVisibility(View.VISIBLE);
        } else {
            v.findViewById(R.id.stub_simple).setVisibility(View.VISIBLE);
        }
        final CheckBox checkbox;
        final EditText edit;
        if (customAttribute.isOfType(CustomAttribute.TYPE_BOOLEAN)) {
            checkbox = (CheckBox) v.findViewById(R.id.edit);
            edit = null;
            customAttribute.setCorrespondingView(checkbox);
            checkbox.setChecked(customAttribute.isBooleanTrueValue());
        } else {
            edit = (EditText) v.findViewById(R.id.edit);
            checkbox = null;
            customAttribute.setCorrespondingView(edit);
            customAttribute.setNewOptionSpinningWheel(v
                    .findViewById(R.id.new_option_spinning_wheel));
            edit.setText(customAttribute.getUserReadableSelectedValue());
        }

        // save the reference to the hint view
        customAttribute.setHintView((TextView) v.findViewById(R.id.hint));

        if (customAttribute.isOfType(CustomAttribute.TYPE_BOOLEAN)){
            checkbox.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    String id = checkbox.isChecked() ? CustomAttribute.TYPE_BOOLEAN_TRUE_VALUE
                            : CustomAttribute.TYPE_BOOLEAN_FALSE_VALUE;
                    int position = customAttribute.getValuePosition(id);
                    String oldValue = customAttribute.getSelectedValue();
                    customAttribute.setOptionSelected(position, true, false);
                    if (mOnAttributeValueChangedByUserInputListener != null) {
                        mOnAttributeValueChangedByUserInputListener.attributeValueChanged(oldValue,
                                customAttribute.getSelectedValue(), customAttribute);
                    }
                    if (mOnEditDoneRunnable != null) {
                        mOnEditDoneRunnable.run();
                    }
                }
            });
        } else if (customAttribute.isOfType(CustomAttribute.TYPE_SELECT)
                || customAttribute.isOfType(CustomAttribute.TYPE_DROPDOWN)) {

            edit.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    showAddNewOptionDialog(customAttribute);

                    return true;
                }
            });

            edit.setInputType(0);
            edit.setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        showSingleSelectDialog(customAttribute);
                        InputMethodManager imm = (InputMethodManager) mActivity
                                .getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        customAttribute.unmarkAttributeContainer();
                    }
                }
            });

            edit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSingleSelectDialog(customAttribute);
                }
            });
        } else if (customAttribute.isOfType(CustomAttribute.TYPE_PRICE)) {
            edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        } else if (customAttribute.isOfType(CustomAttribute.TYPE_MULTISELECT)) {

            edit.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    showAddNewOptionDialog(customAttribute);

                    return true;
                }
            });

            edit.setInputType(0);
            edit.setSingleLine(false);

            edit.setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        showMultiselectDialog(customAttribute);
                        InputMethodManager imm = (InputMethodManager) mActivity
                                .getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        customAttribute.unmarkAttributeContainer();
                    }
                }
            });

            edit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMultiselectDialog(customAttribute);
                }
            });

        } else if (customAttribute.isOfType(CustomAttribute.TYPE_DATE)) {
            edit.setInputType(0);
            edit.setOnFocusChangeListener(new OnFocusChangeListener() {

                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        showDatepickerDialog(customAttribute);
                        InputMethodManager imm = (InputMethodManager) mActivity
                                .getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            });

            edit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDatepickerDialog(customAttribute);
                }
            });
        }

        if (customAttribute.isOfType(CustomAttribute.TYPE_PRICE)
                || customAttribute.isOfType(CustomAttribute.TYPE_TEXT)
                || customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA)) {

            OnEditorActionListener nextButtonBehaviour = new OnEditorActionListener() {

                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {

                        InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                        if (mOnEditDoneRunnable != null) {
                            mOnEditDoneRunnable.run();
                        }

                        return true;
                    }

                    return false;
                }
            };

            edit.setOnEditorActionListener(nextButtonBehaviour);

            edit.setSelectAllOnFocus(true);

            edit.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void afterTextChanged(Editable s) {
                    String oldValue = customAttribute.getSelectedValue();
                    customAttribute.setSelectedValue(edit.getText().toString(), false);
                    setNameHint();
                    if (edit.isFocused() && mOnAttributeValueChangedByUserInputListener != null) {
                        mOnAttributeValueChangedByUserInputListener.attributeValueChanged(oldValue,
                                customAttribute.getSelectedValue(), customAttribute);
                    }
                }
            });

            if (customAttribute.isOfType(CustomAttribute.TYPE_TEXT)) {
                edit.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            } else if (customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA)) {
                edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            }

            edit.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {

                    edit.requestFocus();

                    Intent scanInt = new Intent(
                            mActivity, GestureInputActivity.class);
                    scanInt.putExtra("PARAM_INPUT_TYPE", edit.getInputType());
                    scanInt.putExtra("PARAM_INITIAL_TEXT", edit.getText().toString());

                    try {
                        mActivity.startActivityForResult(scanInt, LAUNCH_GESTURE_INPUT);
                    } catch (ActivityNotFoundException activityNotFound) {
                    }

                    return true;
                }
            });
        }

        /* Set the auto completion adapter for text and textarea fields. */
        if (customAttribute.isOfType(CustomAttribute.TYPE_TEXT)
                || customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA)) {

            InputCacheUtils.initAutoCompleteTextViewWithAdapterFromInputCache(
                    customAttribute.getCode(), mActivity.inputCache, (AutoCompleteTextView) edit,
                    mActivity);
        }

        TextView label = (TextView) v.findViewById(R.id.label);
        label.setText(customAttribute.getMainLabel() + (customAttribute.getIsRequired() ? "*" : ""));
        return v;
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
