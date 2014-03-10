package com.mageventory.widget;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.CompletionInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;

import com.mageventory.R;
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
    public static final String WORDS_DELIMITERS = "[!?,\\.\\s]";
    public static final Pattern WORDS_DELIMITER_PATTERN = Pattern.compile(WORDS_DELIMITERS);

    AutoCompleteTextView mTextView;

    public AutoCompleteTextViewCompoundArrayAdapter(Context context, List<Object> data,
            AutoCompleteTextView textView) {
        super(context, android.R.layout.simple_dropdown_item_1line, data);
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

    public static String[] splitToWords(String str)
    {
        return str == null ? null : str.split(WORDS_DELIMITERS + "+");
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
                Matcher matcher = WORDS_DELIMITER_PATTERN.matcher(textStart);

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
            String[] parts = splitToWords(pString);
            if (parts.length > 0 && !pString.matches("^.*" + WORDS_DELIMITERS + "$")) {
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