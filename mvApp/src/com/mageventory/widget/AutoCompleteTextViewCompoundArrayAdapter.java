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
package com.mageventory.widget;

import java.util.List;
import java.util.regex.Matcher;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.CompletionInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;

import com.mventory.R;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;

/**
 * Compound adapter for the AutoCompleteTextView which shows word from
 * dictionary in a top line in the horizontal list and phrases in the rest
 * vertical items
 * 
 * @author Eugene Popovich
 */
public class AutoCompleteTextViewCompoundArrayAdapter extends CustomArrayAdapter<Object> {
    public static final int ADAPTER_ITEM = 0;
    public static final int SIMPLE_ITEM = 1;

    AutoCompleteTextView mTextView;

    public AutoCompleteTextViewCompoundArrayAdapter(Context context, List<Object> data,
            AutoCompleteTextView textView) {
        super(context, android.R.layout.simple_spinner_dropdown_item, data);
        mTextView = textView;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == ADAPTER_ITEM) {
            HLViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.horizontal_list_dropdown_item, parent,
                        false);
                holder = new HLViewHolder();
                holder.listView = (HorizontalListView) convertView
                        .findViewById(R.id.horizontal_list);
                convertView.setTag(holder);
            } else {
                holder = (HLViewHolder) convertView.getTag();
            }
            final AutoCompleteTextViewCompoundArrayAdapter.HorizontalArrayAdapter adapter = (AutoCompleteTextViewCompoundArrayAdapter.HorizontalArrayAdapter) getItem(position);
            holder.listView.setAdapter(adapter);
            holder.listView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                    adapter.setSelectedPosition(pos);
                    mTextView.onCommitCompletion(new CompletionInfo(id, position, null));
                }
            });
            return convertView;
        } else {
            return super.getView(position, convertView, parent);
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position) instanceof AutoCompleteTextViewCompoundArrayAdapter.HorizontalArrayAdapter ? ADAPTER_ITEM : SIMPLE_ITEM;
    }

    class HLViewHolder {
        HorizontalListView listView;
    }

    /**
     * The adapter for the horizontal items which represent single words
     * 
     * @author Eugene Popovich
     */
    public static class HorizontalArrayAdapter extends CustomArrayAdapter<String> {
        public static final String TAG = HorizontalArrayAdapter.class.getSimpleName();

        int mSelectedPosition = -1;
        AutoCompleteTextView mTextView;

        public HorizontalArrayAdapter(Context context, List<String> values,
                AutoCompleteTextView textView) {
            super(context, R.layout.simple_dropdown_item_1column, android.R.id.text1, values);
            mTextView = textView;
            setFilter(new ArrayFilter() {
                @Override
                protected String wrapValueWithPrefix(String value, String prefix) {
                    return prefix + value.substring(prefix.length());
                }
            });
        }

        public int getSelectionEnd() {
            return mTextView.getSelectionEnd();
        }

        public String processToString(String str) {
            int selectionEnd = getSelectionEnd();
            String text = mTextView.getText().toString();
            String textStart = text;
            String textEnd = null;
            if (selectionEnd >= 0) {
                textStart = text.substring(0, selectionEnd);
                textEnd = text.substring(selectionEnd);
            }
            int lastIndex = findLastDelimiterOccurrence(textStart);
            StringBuilder result = new StringBuilder();
            result.append(textStart);
            result.append(str.substring(Math.min(str.length(), textStart.length() - lastIndex - 1)));
            lastIndex = lastIndex + 1 + str.length();
            if (!TextUtils.isEmpty(textEnd)) {
                result.append(textEnd);
            }
            final int caretPosition = lastIndex;
            GuiUtils.post(new Runnable() {

                @Override
                public void run() {
                    try {
                        mTextView.setSelection(caretPosition);
                    } catch (Exception ex) {
                        CommonUtils.error(TAG, ex);
                    }
                }
            });
            return result.toString();
        }

        public int findLastDelimiterOccurrence(String textStart) {
            int lastIndex = -1;
            if (!TextUtils.isEmpty(textStart)) {
                Matcher matcher = CommonUtils.WORDS_DELIMITER_PATTERN.matcher(textStart);

                // Search for the given pattern
                while (matcher.find()) {
                    lastIndex = matcher.start();
                }
            }
            return lastIndex;
        }

        @Override
        protected CharSequence processPrefix(CharSequence prefix) {

            String pString = getSelectionEnd() == -1 ? prefix.toString() : prefix.toString()
                    .substring(0, Math.min(getSelectionEnd(), prefix.length()));
            String[] parts = CommonUtils.splitToWords(pString);
            if (parts.length > 0 && !pString.matches("^.*" + CommonUtils.WORDS_DELIMITERS + "$")) {
                prefix = parts[parts.length - 1];
            }
            return prefix;
        }

        public void setSelectedPosition(int position) {
            mSelectedPosition = position;
        }

        @Override
        public void notifyDataSetChanged() {
            mSelectedPosition = -1;
            super.notifyDataSetChanged();
        }

        @Override
        public String toString() {
            String result;
            if (mSelectedPosition == -1) {
                result = getCount() > 0 ? getItem(0).toString() : super.toString();
            } else {
                result = getItem(mSelectedPosition).toString();
                result = processToString(result);
            }

            return result;
        }
    }
}