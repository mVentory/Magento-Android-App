package com.mageventory.model.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
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
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.dialogs.CustomAttributeValueSelectionDialog;
import com.mageventory.dialogs.CustomAttributeValueSelectionDialog.OnCheckedListener;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttribute.InputMethod;
import com.mageventory.model.CustomAttributesList.OnAttributeValueChangedListener;
import com.mageventory.model.CustomAttributesList.OnNewOptionTaskEventListener;
import com.mageventory.tasks.CreateOptionTask;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.InputCacheUtils;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.SimpleViewLoadingControl;
import com.mventory.R;

/**
 * The class contains custom attribute view creating functionality <br/>
 * TODO still needs a refactoring
 * 
 * @author Eugene Popovich
 */
public abstract class AbstractCustomAttributeViewUtils implements MageventoryConstants {

    /**
     * Action to run when edit is done
     */
    private transient OnEditDoneAction mOnEditDoneAction;
    /**
     * Reference to the {@link OnAttributeValueChangedListener} which should be
     * called when user manually changes attribute value
     */
    private transient OnAttributeValueChangedListener mOnAttributeValueChangedByUserInputListener;

    /**
     * A reference to an in-ram copy of the input cache loaded from sdcard.
     */
    private Map<String, List<String>> mInputCache;

    /**
     * Listener for the new option task events
     */
    private transient OnNewOptionTaskEventListener mNewOptionListener;

    /**
     * Related activity
     */
    Activity mActivity;

    /**
     * Custom attributes list
     */
    private List<CustomAttribute> mCustomAttributesList;

    /**
     * The attribute set id (used for the add new option functionality)
     */
    private String mSetId;

    /**
     * Flag indicating whether the add new option functionality is available
     */
    private boolean mAddNewOptionAvailable;
    /**
     * Flag indicating whether the select empty option functionality is
     * available
     */
    private boolean mSelectEmptyOptionAllowed;

    /**
     * @param addNewOptionAvailable whether the add new option functionality is
     *            available
     * @param selectEmptyOptonAllowed whether the select empty option
     *            functionality is available
     * @param newOptionListener the new option listener for the
     *            {@link CreateOptionTask}
     * @param customAttributesList the list of custom attributes (used for the
     *            create option functionality)
     * @param setId attribute set id (used for the add new option functionality)
     * @param activity the related activity
     */
    public AbstractCustomAttributeViewUtils(boolean addNewOptionAvailable,
            boolean selectEmptyOptonAllowed,
            OnNewOptionTaskEventListener newOptionListener,
            List<CustomAttribute> customAttributesList, String setId, Activity activity) {
        this(null, addNewOptionAvailable, selectEmptyOptonAllowed, null, null, newOptionListener,
                customAttributesList,
                setId, activity);
    }

    /**
     * @param inputCache in-ram copy of the input cache loaded from sdcard
     * @param addNewOptionAvailable whether the add new option functionality is
     *            available
     * @param selectEmptyOptonAllowed whether the select empty option
     *            functionality is available
     * @param onEditDoneAction action to run when edit is done
     * @param onAttributeValueChangedByUserInputListener should be called when
     *            user manually changes attribute value
     * @param newOptionListener the new option listener for the
     *            {@link CreateOptionTask}
     * @param customAttributesList the list of custom attributes (used for the
     *            create option functionality)
     * @param activity the related activity
     */
    public AbstractCustomAttributeViewUtils(Map<String, List<String>> inputCache,
            boolean addNewOptionAvailable, boolean selectEmptyOptonAllowed,
            OnEditDoneAction onEditDoneAction,
            OnAttributeValueChangedListener onAttributeValueChangedByUserInputListener,
            OnNewOptionTaskEventListener newOptionListener,
            List<CustomAttribute> customAttributesList, String setId, Activity activity) {
        super();
        mOnEditDoneAction = onEditDoneAction;
        mOnAttributeValueChangedByUserInputListener = onAttributeValueChangedByUserInputListener;
        mInputCache = inputCache;
        mNewOptionListener = newOptionListener;
        mActivity = activity;
        mCustomAttributesList = customAttributesList;
        mAddNewOptionAvailable = addNewOptionAvailable;
        mSelectEmptyOptionAllowed = selectEmptyOptonAllowed;
        mSetId = setId;
    }

    /**
     * Create a view corresponding to the custom attribute in order to show it
     * to the user.
     * 
     * @param v
     * @param customAttribute
     */
    public void initAtrEditView(final View v, final CustomAttribute customAttribute) {

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
            customAttribute.setAttributeLoadingControl(new SimpleViewLoadingControl(v
                    .findViewById(R.id.new_option_spinning_wheel)));
            edit.setText(customAttribute.getUserReadableSelectedValue());
        }
        // save the reference to the hint view
        customAttribute.setHintView((TextView) v.findViewById(R.id.hint));
        if (customAttribute.isReadOnly()) {
            customAttribute.getCorrespondingView().setEnabled(false);
            customAttribute.getCorrespondingView().setFocusable(false);
        } else if (customAttribute.isOfType(CustomAttribute.TYPE_BOOLEAN)) {
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
                    if (mOnEditDoneAction != null) {
                        mOnEditDoneAction.onEditDone(customAttribute.getCode());
                    }
                }
            });
        } else if (customAttribute.isOfType(CustomAttribute.TYPE_SELECT)
                || customAttribute.isOfType(CustomAttribute.TYPE_DROPDOWN)) {

            if (mAddNewOptionAvailable && customAttribute.isAddNewOptionsAllowed()) {
                edit.setOnLongClickListener(new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        showAddNewOptionDialog(customAttribute);

                        return true;
                    }
                });
            }

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
        } else if (customAttribute.isOfType(CustomAttribute.TYPE_PRICE)
                || customAttribute.isOfType(CustomAttribute.TYPE_WEIGHT)) {
            edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        } else if (customAttribute.isOfType(CustomAttribute.TYPE_MULTISELECT)) {

            if (mAddNewOptionAvailable && customAttribute.isAddNewOptionsAllowed()) {
                edit.setOnLongClickListener(new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        showAddNewOptionDialog(customAttribute);

                        return true;
                    }
                });
            }

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
                || customAttribute.isOfType(CustomAttribute.TYPE_WEIGHT)
                || customAttribute.isOfType(CustomAttribute.TYPE_TEXT)
                || customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA)) {

            edit.setOnEditorActionListener(getAttributeOnEditorActionListener(customAttribute
                    .getCode()));

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
                    if (edit.isFocused() && mOnAttributeValueChangedByUserInputListener != null) {
                        mOnAttributeValueChangedByUserInputListener.attributeValueChanged(oldValue,
                                customAttribute.getSelectedValue(), customAttribute);
                    }
                }
            });

            InputMethod inputMethod = getDefaultKeyboardInputMethod(customAttribute);
            setKeyboardInputMethod(customAttribute, edit, inputMethod);
        }

        /* Set the auto completion adapter for text and textarea fields. */
        if (customAttribute.isOfType(CustomAttribute.TYPE_TEXT)
                || customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA)) {

            InputCacheUtils.initAutoCompleteTextViewWithAdapterFromInputCache(
                    customAttribute.getCode(), mInputCache, (AutoCompleteTextView) edit,
                    mActivity);
        }

        setCustomAttributeLabel(customAttribute.getMainLabel(), customAttribute.getIsRequired(),
                (TextView) v.findViewById(R.id.label));
    }

    /**
     * Set the custom attribute label depend on isRequired settings if the
     * labelView is not null
     * 
     * @param label the custom attribute main label
     * @param isRequired whether the custom attribute is required (red asterisk will be appended if true)
     * @param labelView the label view
     */
    public void setCustomAttributeLabel(String label, boolean isRequired, TextView labelView) {
        if (labelView != null) {
            labelView.setText(Html.fromHtml(label
                    + (isRequired ? " <font color=\"red\">*</font>" : "")));
        }
    }

    /**
     * Get the default keyboard input method for the custom attribute. The logic
     * is based on alternate input methods processing in case default input
     * method is not of keyboard type
     * 
     * @param customAttribute the custom attribute to get the default keyboard input method for
     * @return
     */
    public static InputMethod getDefaultKeyboardInputMethod(final CustomAttribute customAttribute) {
        InputMethod inputMethod = customAttribute.getInputMethod();
        if (inputMethod != InputMethod.NORMAL_KEYBOARD
                && inputMethod != InputMethod.NUMERIC_KEYBOARD) {
            if (customAttribute.hasAlternateInputMethod(InputMethod.NORMAL_KEYBOARD)) {
                inputMethod = InputMethod.NORMAL_KEYBOARD;
            } else if (customAttribute.hasAlternateInputMethod(InputMethod.NUMERIC_KEYBOARD)) {
                inputMethod = InputMethod.NUMERIC_KEYBOARD;
            }
        }
        return inputMethod;
    }

    /**
     * Set the active keyboard input type for the customAttribute corresponding
     * {@link EditText} view
     * 
     * @param customAttribute the custom attribute to process
     * @param edit the related to attribute {@link EditText} view
     * @param inputMethod the desired input method
     */
    public static void setKeyboardInputMethod(final CustomAttribute customAttribute,
            final EditText edit,
            InputMethod inputMethod) {
        if (inputMethod == null || inputMethod == InputMethod.NORMAL_KEYBOARD) {
            // if input method is absent or is normal keyboard
            if (customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA)) {
                // if this is multiline text attribute
                edit.setInputType(
                		InputType.TYPE_CLASS_TEXT 
                		| InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            } else {
                edit.setInputType(
                		InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            }
        } else if (inputMethod == InputMethod.NUMERIC_KEYBOARD) {
            // if input method is numeric keyboard
            edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }
    }

    /**
     * Get on editor action listener for the attribute which handles go and next
     * button click
     * 
     * @param attributeCode the attribute code to get editor action listener for
     * @return instance of {@link OnEditorActionListener} related to the
     *         attribute with code
     */
    public OnEditorActionListener getAttributeOnEditorActionListener(final String attributeCode) {
        OnEditorActionListener nextButtonBehaviour = new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_GO) {
                    // if next or go button is clicked
                	
                    // hide keyboard
                    InputMethodManager imm = (InputMethodManager) mActivity
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    
                    v.clearFocus();

                    // fire edit done action event to listener if exists
                    if (mOnEditDoneAction != null) {
                        mOnEditDoneAction.onEditDone(attributeCode);
                    }

                    return true;
                }

                return false;
            }
        };
        return nextButtonBehaviour;
    }

    /**
     * Shows a dialog for adding new option.
     * 
     * @param customAttribute
     */
    public void showAddNewOptionDialog(final CustomAttribute customAttribute) {
        final View textEntryView = mActivity.getLayoutInflater().inflate(
                R.layout.add_new_option_dialog, null);

        /*
         * User is not able to create a new option if one is currently being
         * created. I'm just checking if the attribute loading control is
         * loading so that I don't have to add another variable for tracking
         * that.
         */
        if (customAttribute.getAttributeLoadingControl().isLoading()) {
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

                if (hasFocus) {
                    InputMethodManager imm = (InputMethodManager) mActivity
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            }
        });

        alert.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String oldValue = customAttribute.getSelectedValue();

                customAttribute.addNewOption(mActivity, editText.getText().toString());

                if (mOnAttributeValueChangedByUserInputListener != null) {
                    mOnAttributeValueChangedByUserInputListener.attributeValueChanged(oldValue,
                            customAttribute.getSelectedValue(), customAttribute);
                }

                CreateOptionTask createOptionTask = new CreateOptionTask(mActivity,
                        customAttribute, mCustomAttributesList, editText.getText().toString(),
                        mSetId, mNewOptionListener, mOnAttributeValueChangedByUserInputListener);
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
                        if (editText.getText().toString().trim().length() == 0) {
                            button.setEnabled(false);
                        } else {
                            button.setEnabled(true);
                        }
                    }
                });

            }
        });

        srDialog.show();
    }

    /**
     * Show date picker dialog
     * 
     * @param customAttribute
     */
    private void showDatepickerDialog(final CustomAttribute customAttribute) {
        final OnDateSetListener onDateSetL = new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                monthOfYear += 1; // because it's from 0 to 11 for compatibility
                                  // reasons
                final String date = "" + monthOfYear + "/" + dayOfMonth + "/" + year;

                String oldValue = customAttribute.getSelectedValue();

                customAttribute.setSelectedValue(date, true);

                if (mOnAttributeValueChangedByUserInputListener != null) {
                    mOnAttributeValueChangedByUserInputListener.attributeValueChanged(oldValue,
                            customAttribute.getSelectedValue(), customAttribute);
                }
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

    /**
     * Show single selection dialog
     * 
     * @param customAttribute
     */
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
            if (customAttribute.getOptions().get(i).getSelected()) {
                selectedItemIdx = i;
            }
        }
        if (mSelectEmptyOptionAllowed && customAttribute.isEmptyValueSelectionAllowed()) {
        	// increase selected item id in case dialog has empty value for the selection
            selectedItemIdx++;
        }

        CustomAttributeValueSelectionDialog dialog = new CustomAttributeValueSelectionDialog(
                mActivity, mAddNewOptionAvailable && customAttribute.isAddNewOptionsAllowed()
                        && !customAttribute.isOfType(CustomAttribute.TYPE_BOOLEAN),
                mSelectEmptyOptionAllowed && customAttribute.isEmptyValueSelectionAllowed());
        dialog.initSingleSelectDialog(items, selectedItemIdx);

        dialog.setOnCheckedListener(new OnCheckedListener() {

            @Override
            public void onChecked(int position, boolean checked) {

                if (checked) {
                    if (position == -1) {
                        showAddNewOptionDialog(customAttribute);
                    } else {
                        String oldValue = customAttribute.getSelectedValue();
                        if (mSelectEmptyOptionAllowed && customAttribute.isEmptyValueSelectionAllowed()) {
                            // decrease position in case empty option selection
                            // is allowed
                            position--;
                        }
                        if (position == -1) {
                            // select empty value in case decreased position is
                            // negative
                            customAttribute.setSelectedValue(null, false);
                        } else {
                            customAttribute.setOptionSelected(position, checked, false);
                        }
                        if (mOnAttributeValueChangedByUserInputListener != null) {
                            mOnAttributeValueChangedByUserInputListener.attributeValueChanged(
                                    oldValue, customAttribute.getSelectedValue(), customAttribute);
                        }
                        if (mOnEditDoneAction != null) {
                            mOnEditDoneAction.onEditDone(customAttribute.getCode());
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
            }
        });

        dialog.show();
    }

    /**
     * Show multiselection dialog
     * 
     * @param customAttribute
     */
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
                mActivity, mAddNewOptionAvailable && customAttribute.isAddNewOptionsAllowed()
                        && !customAttribute.isOfType(CustomAttribute.TYPE_BOOLEAN), false);
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
            }
        });

        dialog.setRunOnOkButtonPressed(new Runnable() {
            
            @Override
            public void run() {
                if (mOnEditDoneAction != null) {
                    mOnEditDoneAction.onEditDone(customAttribute.getCode());
                }
            }
        });

        dialog.show();
    }

    /**
     * Common implementation of the {@link OnNewOptionTaskEventListener}
     */
    public static class CommonOnNewOptionTaskEventListener implements OnNewOptionTaskEventListener {

        /**
         * Loading control for the create new option operation
         */
        LoadingControl mLoadingControl;
        /**
         * Related activity
         */
        BaseFragmentActivity mActivity;

        /**
         * @param loadingControl loading control for the create new option operation
         * @param activity related activity
         */
        public CommonOnNewOptionTaskEventListener(LoadingControl loadingControl,
                BaseFragmentActivity activity) {
            mLoadingControl = loadingControl;
            mActivity = activity;
        }

        @Override
        public void OnAttributeCreationStarted() {
            mLoadingControl.startLoading();
        }

        @Override
        public void OnAttributeCreationFinished(String attributeName, String newOptionName,
                boolean success) {
            mLoadingControl.stopLoading();

            if (!success && mActivity.isActivityAlive()) {
                // if option creation failed and activity was not closed
                showNewOptionErrorDialog(attributeName, newOptionName, mActivity);
            }
        }

        /**
         * Show a dialog informing the user that option creation failed
         * 
         * @param attributeName the attribute name
         * @param optionName the option name
         * @param activity an activity where the dialog should be shown
         */
        public static void showNewOptionErrorDialog(String attributeName, String optionName,
                Activity activity) {
            AlertDialog.Builder alert = new AlertDialog.Builder(activity);

            alert.setTitle(R.string.error);

            alert.setMessage(CommonUtils.getStringResource(R.string.cannont_add_option, optionName,
                    attributeName));

            alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            alert.show();
        }
    }

    /**
     * The interface for the edit done action listener
     */
    public static interface OnEditDoneAction {
        public void onEditDone(String attributeCode);
    }
}
