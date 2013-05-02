package com.mageventory.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mageventory.R;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class CustomAttributeValueSelectionDialog extends Dialog {
	
	private static final String sListAdapterColumnName = "option_name";
	
	private Context mContext;
	private LayoutInflater mInflater;
	private ListView mOptionsListView;
	private EditText mSearchEditBox;
	private Button mOkButton;
	
	private String [] mOptionNames;
	private boolean [] mOptionDisplayed;
	private boolean [] mCheckedOptions;
	private int mSelectedItemIdx;
	private OnItemClickListener mOnItemClickListener;
	private OnCheckedListener mOnCheckedListener;
	private TextView mSelectedValuesText;
	private int mScrollToIndexOnRefresh = -1;
	
	public static interface OnCheckedListener
	{
		void onChecked(int position, boolean checked);
	}
	
	private void updateSelectedValuesText()
	{
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<mOptionNames.length; i++)
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
		InputMethodManager imm = (InputMethodManager)mContext.getSystemService(
			Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mSearchEditBox.getWindowToken(), 0);
	}
	
	public CustomAttributeValueSelectionDialog(Context context)
	{
		super(context, android.R.style.Theme_Black);
		mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		setTitle("Options selection");
		
		LinearLayout mainLayout = (LinearLayout)mInflater.inflate(R.layout.custom_attribute_value_selection, null);
		setContentView(mainLayout);
		
		mSearchEditBox = (EditText)mainLayout.findViewById(R.id.searchEditBox);
		mOptionsListView = (ListView)mainLayout.findViewById(R.id.listView);
		mSelectedValuesText = (TextView)mainLayout.findViewById(R.id.selectedValuesText);
		
		mOkButton = (Button)mainLayout.findViewById(R.id.okButton);
		
		mOkButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				hideKeyboardOnSearchBox();
				CustomAttributeValueSelectionDialog.this.dismiss();
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
			public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
				
				mOptionsListView.post(new Runnable() {
					@Override
					public void run() {

						final CheckedTextView ct = (CheckedTextView)mOptionsListView.getChildAt(position - mOptionsListView.getFirstVisiblePosition());
						
						if (mOnCheckedListener != null)
						{
							mOnCheckedListener.onChecked(getOptionIdxFromListPosition(position), ct.isChecked());
						}
						
						if (mOptionsListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE)
						{
							mCheckedOptions[getOptionIdxFromListPosition(position)] = ct.isChecked();
							updateSelectedValuesText();
							
							hideKeyboardOnSearchBox();
							
							if (ct.isChecked() == true)
							{
								if (mSearchEditBox.getText().toString().length() > 0)
								{
									mScrollToIndexOnRefresh = getOptionIdxFromListPosition(position);
									mSearchEditBox.setText("");
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
	
	public int getOptionIdxFromListPosition(int position)
	{
		int posTmp = -1;
		for (int i=0; i<mOptionDisplayed.length; i++)
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
		for (int i=0; i<=idx; i++)
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
		
		for (int i=0; i<mOptionNames.length; i++)
		{
			mOptionDisplayed[i] = false;
			
			if (mOptionNames[i].toLowerCase().contains(filter.toLowerCase()))
			{
				mOptionDisplayed[i] = true;
			}
			else
			{
				if (mOptionsListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE)
				{
					if (mCheckedOptions[i] == true)
					{
						mOptionDisplayed[i] = true;
					}
				}
				else
				{
					if (mSelectedItemIdx == i)
					{
						mOptionDisplayed[i] = true;
					}
				}
			}
			
			if (mOptionDisplayed[i] == true)
			{
				Map<String, Object> row = new HashMap<String, Object>();
				row.put(sListAdapterColumnName, mOptionNames[i]);
				data.add(row);
			}
		}
		
		ListAdapter adapter;
		
		if (mOptionsListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE)
		{
			adapter = new SimpleAdapter(mContext, data, android.R.layout.simple_list_item_multiple_choice, new String [] {sListAdapterColumnName},
				new int [] {android.R.id.text1});
		}
		else
		{
			adapter = new SimpleAdapter(mContext, data, android.R.layout.simple_list_item_single_choice, new String [] {sListAdapterColumnName},
				new int [] {android.R.id.text1});
		}
		
		mOptionsListView.setAdapter(adapter);
		
		if (mOptionsListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE)
		{
			for(int i=0; i<mCheckedOptions.length; i++)
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
				for (int i=0; i<mCheckedOptions.length; i++)
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
	
	public void initMultiSelectDialog(String [] optionNames, boolean[] checkedOptions)
	{
		mOptionNames = optionNames;
		mCheckedOptions = checkedOptions;
		mOptionDisplayed = new boolean[optionNames.length];
		
		mOptionsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		refreshList("");
		updateSelectedValuesText();
	}
	
	public void initSingleSelectDialog(String [] optionNames, int selectedItemIdx)
	{
		mOptionNames = optionNames;
		mSelectedItemIdx = selectedItemIdx;
		mOptionDisplayed = new boolean[optionNames.length];
		
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
			for (int i=0; i<mCheckedOptions.length; i++)
			{
				if (mCheckedOptions[i] == true)
				{
					//mOptionsListView.smoothScrollToPosition(i);
					mOptionsListView.setSelection(i);
					break;
				}
			}
		}
		else
		{
			//mOptionsListView.smoothScrollToPosition(mSelectedItemIdx);
			mOptionsListView.setSelection(mSelectedItemIdx);
		}
		
		mOptionsListView.post(new Runnable() {
			
			@Override
			public void run() {
				int visibleChildCount = (mOptionsListView.getLastVisiblePosition() - mOptionsListView.getFirstVisiblePosition()) + 1;
				if ((mOptionNames.length - visibleChildCount) >= 5)
				{
					mSearchEditBox.setVisibility(View.VISIBLE);
				}
			}
		});
    }
}