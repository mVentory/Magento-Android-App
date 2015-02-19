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

package com.mageventory.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.mageventory.util.CommonUtils;
import com.mventory.R;

public class CustomAttributeValueSelectionDialog extends Dialog {

    private static final String sListAdapterColumnName = "option_name";

    private Context mContext;
    private LayoutInflater mInflater;
    private ListView mOptionsListView;
    private EditText mSearchEditBox;
    private Button mOkButton;

    private String[] mOptionNames;
    private boolean[] mOptionDisplayed;
    private boolean[] mCheckedOptions;
    private int mSelectedItemIdx;
    private OnItemClickListener mOnItemClickListener;
    private OnCheckedListener mOnCheckedListener;
    private TextView mSelectedValuesText;
    private int mScrollToIndexOnRefresh = -1;
    private boolean mHasAddNewOption = true;
    /**
     * Flag indicating whether the selection dialog should have empty option
     */
    private boolean mHasEmptyOption;
    private Runnable mRunOnOkButtonPressed;

    public static interface OnCheckedListener
    {
        void onChecked(int position, boolean checked);
    }

    private void updateSelectedValuesText()
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < mOptionNames.length; i++)
        {
            if (mCheckedOptions[i] == true)
            {
                if (sb.length() > 0)
                    sb.append(", ");

                sb.append(mOptionNames[i]);
            }
        }

        mSelectedValuesText.setText(sb.toString());
    }

    private void hideKeyboardOnSearchBox()
    {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchEditBox.getWindowToken(), 0);
    }

    /**
     * @param context
     * @param hasAddNewOption whether the selection dialog should to have add
     *            new option item
     * @param hasEmptyOption whether the single selection dialog should to have
     *            empty item
     */
    public CustomAttributeValueSelectionDialog(Context context, boolean hasAddNewOption,
            boolean hasEmptyOption)
    {
        super(context);
        mHasAddNewOption = hasAddNewOption;
        mHasEmptyOption = hasEmptyOption;
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setTitle("Options selection");

        LinearLayout mainLayout = (LinearLayout) mInflater.inflate(
                R.layout.custom_attribute_value_selection, null);
        setContentView(mainLayout);

        mSearchEditBox = (EditText) mainLayout.findViewById(R.id.searchEditBox);
        mOptionsListView = (ListView) mainLayout.findViewById(R.id.listView);
        mSelectedValuesText = (TextView) mainLayout.findViewById(R.id.selectedValuesText);

        mOkButton = (Button) mainLayout.findViewById(R.id.okButton);

        mOkButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                hideKeyboardOnSearchBox();
                CustomAttributeValueSelectionDialog.this.dismiss();
                if (mRunOnOkButtonPressed != null) {
                    mRunOnOkButtonPressed.run();
                }
            }

        });

        mSearchEditBox.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String filter = s.toString();
                refreshList(filter);
            }
        });

        mOnItemClickListener = new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view, final int position,
                    long id) {

                mOptionsListView.post(new Runnable() {
                    @Override
                    public void run() {

                        final CheckedTextView ct = (CheckedTextView) mOptionsListView
                                .getChildAt(position - mOptionsListView.getFirstVisiblePosition());

                        if (mOnCheckedListener != null)
                        {
                            mOnCheckedListener.onChecked(getOptionIdxFromListPosition(position),
                                    ct.isChecked());
                        }

                        if (mOptionsListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE)
                        {
                            int optionPosition = getOptionIdxFromListPosition(position);
                            if (optionPosition == -1) {
                                hideKeyboardOnSearchBox();
                                CustomAttributeValueSelectionDialog.this.dismiss();
                            } else {
                                mCheckedOptions[optionPosition] = ct.isChecked();
                                updateSelectedValuesText();

                                hideKeyboardOnSearchBox();

                                if (ct.isChecked() == true) {
                                    if (mSearchEditBox.getText().toString().length() > 0) {
                                        mScrollToIndexOnRefresh = getOptionIdxFromListPosition(position);
                                        mSearchEditBox.setText("");
                                    }
                                }
                            }
                        }
                        else
                        {
                            if (ct.isChecked())
                            {
                                mSelectedItemIdx = getOptionIdxFromListPosition(position);
                            }

                            hideKeyboardOnSearchBox();
                            CustomAttributeValueSelectionDialog.this.dismiss();
                        }
                    }
                });
            }
        };
    }

    /**
     * Set the runnable which will be run when ok button is clicked
     * 
     * @param runnable
     */
    public void setRunOnOkButtonPressed(Runnable runnable) {
        mRunOnOkButtonPressed = runnable;
    }

    public int getOptionIdxFromListPosition(int position)
    {
        int posTmp = -1;
        for (int i = 0; i < mOptionDisplayed.length; i++)
        {
            if (mOptionDisplayed[i])
            {
                posTmp++;
            }

            if (posTmp == position)
            {
                return i;
            }
        }

        return -1;
    }

    public int getListPositionFromOptionIdx(int idx)
    {
        int posTmp = -1;
        // support for empty options list or list with no selected item
        if (idx < 0) {
            return posTmp;
        }
        for (int i = 0; i <= idx; i++)
        {
            if (mOptionDisplayed[i])
            {
                posTmp++;
            }
        }

        if (mOptionDisplayed[idx])
        {
            return posTmp;
        }
        else
        {
            return -1;
        }
    }

    public void refreshList(String filter)
    {
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

        int offset = 0;
        if (mHasEmptyOption) {
            // if list should have empty option
            Map<String, Object> row = new HashMap<String, Object>();
            row.put(sListAdapterColumnName, "");
            data.add(row);
            mOptionDisplayed[offset] = true;
            // increment offset, required to handle displayed options for the
            // regular custom attribute option names
            offset++;
        }
        for (int i = 0; i < mOptionNames.length; i++) {
            // mOptionsDisplayed index can be different then option name index
            // in case list has empty option
            int displayIndex = i + offset;
            mOptionDisplayed[displayIndex] = false;

            if (mOptionNames[i].toLowerCase().contains(filter.toLowerCase()))
            {
                mOptionDisplayed[displayIndex] = true;
            }
            else
            {
                if (mOptionsListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE)
                {
                    if (mCheckedOptions[i] == true)
                    {
                        mOptionDisplayed[displayIndex] = true;
                    }
                }
                else
                {
                    if (mSelectedItemIdx == i)
                    {
                        mOptionDisplayed[displayIndex] = true;
                    }
                }
            }

            if (mOptionDisplayed[displayIndex] == true)
            {
                Map<String, Object> row = new HashMap<String, Object>();
                row.put(sListAdapterColumnName, mOptionNames[i]);
                data.add(row);
            }
        }

        if(mHasAddNewOption)
        {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put(sListAdapterColumnName, CommonUtils.getStringResource(R.string.add_new_value_item));
            data.add(row);
        }
        ListAdapter adapter;

        if (mOptionsListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE)
        {
            adapter = new SimpleAdapter(mContext, data,
                    android.R.layout.simple_list_item_multiple_choice, new String[] {
                        sListAdapterColumnName
                    },
                    new int[] {
                        android.R.id.text1
                    });
        }
        else
        {
            adapter = new SimpleAdapter(mContext, data,
                    android.R.layout.simple_list_item_single_choice, new String[] {
                        sListAdapterColumnName
                    },
                    new int[] {
                        android.R.id.text1
                    });
        }

        mOptionsListView.setAdapter(adapter);

        if (mOptionsListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE)
        {
            for (int i = 0; i < mCheckedOptions.length; i++)
            {
                int listPos = getListPositionFromOptionIdx(i);

                if (listPos != -1)
                {
                    mOptionsListView.setItemChecked(listPos, mCheckedOptions[i]);
                }
            }
        }
        else
        {
            int listPos = getListPositionFromOptionIdx(mSelectedItemIdx);

            if (listPos != -1)
                mOptionsListView.setItemChecked(listPos, true);
        }

        mOptionsListView.setOnItemClickListener(mOnItemClickListener);

        if (mScrollToIndexOnRefresh != -1)
        {
            mOptionsListView.setSelection(getListPositionFromOptionIdx(mScrollToIndexOnRefresh));
            mScrollToIndexOnRefresh = -1;
        }
        else
        {
            if (mOptionsListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE)
            {
                for (int i = 0; i < mCheckedOptions.length; i++)
                {
                    if (mCheckedOptions[i] == false && mOptionDisplayed[i] == true)
                    {
                        mOptionsListView.setSelection(getListPositionFromOptionIdx(i));
                        break;
                    }
                }
            }
        }
    }

    public void initMultiSelectDialog(String[] optionNames, boolean[] checkedOptions)
    {
        mOptionNames = optionNames;
        mCheckedOptions = checkedOptions;
        mOptionDisplayed = new boolean[optionNames.length];

        mOptionsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        refreshList("");
        updateSelectedValuesText();
    }

    public void initSingleSelectDialog(String[] optionNames, int selectedItemIdx)
    {
        mOptionNames = optionNames;
        mSelectedItemIdx = selectedItemIdx;
        mOptionDisplayed = new boolean[optionNames.length 
                                       // dialog with empty option 
                                       // will have another length of 
                                       // the mOptionDisplayed array   
                                       + (mHasEmptyOption ? 1 : 0)];

        mOptionsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        refreshList("");
        mOkButton.setVisibility(View.GONE);
        mSelectedValuesText.setVisibility(View.GONE);
    }

    public void setOnCheckedListener(OnCheckedListener onCheckedListener)
    {
        mOnCheckedListener = onCheckedListener;
    }

    public void show()
    {
        super.show();

        if (mOptionsListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE)
        {
            for (int i = 0; i < mCheckedOptions.length; i++)
            {
                if (mCheckedOptions[i] == true)
                {
                    // mOptionsListView.smoothScrollToPosition(i);
                    mOptionsListView.setSelection(i);
                    break;
                }
            }
        }
        else
        {
            // mOptionsListView.smoothScrollToPosition(mSelectedItemIdx);
            mOptionsListView.setSelection(mSelectedItemIdx);
        }

        mOptionsListView.post(new Runnable() {

            @Override
            public void run() {
                int visibleChildCount = (mOptionsListView.getLastVisiblePosition() - mOptionsListView
                        .getFirstVisiblePosition()) + 1;
                if ((mOptionNames.length - visibleChildCount) >= 5)
                {
                    mSearchEditBox.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
