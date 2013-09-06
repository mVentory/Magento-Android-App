
package com.mageventory.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.mageventory.MageventoryConstants;
import com.mageventory.job.JobCacheManager;
import com.mageventory.resprocessor.ProductAttributeFullInfoProcessor;

public class CustomAttribute implements Serializable {
    private static final long serialVersionUID = -6103686023229057345L;

    /*
     * Represents a single option. Used in case of attributes that have options.
     * In case of attributes that don't have options we just use a simple String
     * to store the value.
     */
    public static class CustomAttributeOption implements Serializable {
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
    }

    /* Each attribute is of one of those types. */
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_SELECT = "select";
    public static final String TYPE_MULTISELECT = "multiselect";
    public static final String TYPE_DROPDOWN = "dropdown";
    public static final String TYPE_PRICE = "price";
    public static final String TYPE_DATE = "date";
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_TEXTAREA = "textarea";

    /* Data from the server associated with this attribute. */

    /*
     * A list of options, used only in case of attributes that have them. In
     * other cases we just use mSelectedValue field to store the value of an
     * attribute.
     */
    private List<CustomAttributeOption> mOptions;
    private String mSelectedValue = "";
    /* This always stores one of the TYPE_* constants value. */
    private String mType;
    private boolean mIsRequired;
    private String mMainLabel;
    private String mCode;
    private String mAttributeID;

    /*
     * Each attribute has a corresponding view which is either an EditBox or a
     * Spinner depending on type of the attribute.
     */
    private transient View mCorrespondingView;

    /*
     * Reference to a spinning wheel shown when an option is being created for a
     * custom attribute
     */
    private transient View mNewOptionSpinningWheel;

    public void setAttributeID(String attribID) {
        mAttributeID = attribID;
    }

    public String getAttributeID() {
        return mAttributeID;
    }

    public void setCorrespondingView(View view) {
        mCorrespondingView = view;
    }

    public View getCorrespondingView() {
        return mCorrespondingView;
    }

    public void setNewOptionSpinningWheel(View spinningWheel) {
        mNewOptionSpinningWheel = spinningWheel;
    }

    public View getNewOptionSpinningWheel() {
        return mNewOptionSpinningWheel;
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

    public void setOptions(List<CustomAttributeOption> options) {
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

    public List<CustomAttributeOption> getOptions() {
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
            if (isOfType(CustomAttribute.TYPE_MULTISELECT)
                    || isOfType(CustomAttribute.TYPE_BOOLEAN)
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
        } else {
            return mSelectedValue;
        }

        return null;
    }

    /*
     * Takes comma separated Strings which are either option ids or some text
     * user entered in editbox (depending on type). This is the format returned
     * by the server as value of an attribute. This function is just parsing it
     * and convering it to a more friendly format. In case of attributes which
     * have options we store their value in mOptions array. In all other cases
     * we store the value as a simple string in mSelectedValue field.
     */
    public void setSelectedValue(String selectedValue, boolean updateView) {
        if (selectedValue == null)
            selectedValue = "";

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
            if (!defaultOptionSelected)
            {
                mOptions.get(0).setSelected(true);
            }
            if (updateView) {
                ((EditText) mCorrespondingView).setText(getUserReadableSelectedValue());
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
}
