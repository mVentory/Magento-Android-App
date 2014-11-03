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

package com.mageventory.fragment;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.fragment.base.BaseDialogFragment;
import com.mageventory.model.util.ProductUtils;
import com.mageventory.recent_web_address.RecentWebAddress;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.widget.FlowLayout;

/**
 * Dialog fragment which is shown to manage search query and search domains
 */
public class SearchOptionsFragment extends BaseDialogFragment {
    /**
     * Tag used for logging
     */
    static final String TAG = SearchOptionsFragment.class.getSimpleName();
    /**
     * The last clicked web address domain. Stored as static field and will be
     * lost if application will be killed by OS. May be replaced with settings
     * in future
     */
    static String sLastUsedOption;
    /**
     * The adapter for the list view which stores words used for search
     */
    WordsAdapter mUseForSearchAdapter;
    /**
     * The original query which contains all possible words for the search
     */
    String mOriginalQuery;
    /**
     * The actual search query. The list view which stores words used for search
     * will be filled from this.
     */
    String mQuery;
    /**
     * The product SKU search is opened for
     */
    String mSku;
    /**
     * The possible search domains options
     */
    List<RecentWebAddress> mAddresses;
    /**
     * The listener for the recent web address clicked event
     */
    OnRecentWebAddressClickedListener mListener;

    /**
     * Set the data which should be used by fragment
     * 
     * @param sku the product SKU search is opened for. Could be empty in case
     *            search is opened for the not yet saved product
     * @param query the actual query which is used for search
     * @param originalQuery the original query which contains all possible words
     *            which may be used for search
     * @param addresses the possible search domain options
     * @param listener the listener for the recent web address clicked event
     */
    public void setData(String sku, String query, String originalQuery,
            List<RecentWebAddress> addresses,
            OnRecentWebAddressClickedListener listener) {
        if (addresses == null) {
            // addresses should not be null
            addresses = new ArrayList<RecentWebAddress>();
        }
        // add all of internet option to the start of the list
        RecentWebAddress address = new RecentWebAddress();
        address.setDomain(CommonUtils.getStringResource(R.string.menu_all_of_internet));
        addresses.add(0, address);
        mAddresses = addresses;
        mQuery = query;
        mOriginalQuery = originalQuery;
        mListener = listener;
        mSku = sku;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_options, container);
        init(view, savedInstanceState);
        return view;
    }

    void init(View view, Bundle savedInstanceState) {
        // all possible words which may be used for query
        List<String> possibleWords = CommonUtils.getUniqueWords(mOriginalQuery, true);
        // the words which are currently used for search
        List<String> actualWords = CommonUtils.getUniqueWords(mQuery, true);
        // the list of words which are absennt in the actualWords but
        // present in the possibleWords
        List<String> missingWords = new ArrayList<String>();
        for(String possibleWord : possibleWords)
        {
            String lcPossibleWord = possibleWord.toLowerCase();
            // the flag indicating word found in the actualWords
            boolean found = false;
            for (String word : actualWords) {
                if (TextUtils.equals(word.toLowerCase(), lcPossibleWord)) {
                    // set the flag and interrupt the loop
                    found = true;
                    break;
                }
            }
            if(!found){
                // the word is absent in the actualWords
                missingWords.add(possibleWord);
            }
        }
        WordsAdapter possibleWordsAdapter = new WordsAdapter();
        possibleWordsAdapter.addAll(missingWords);
        mUseForSearchAdapter = new WordsAdapter();
        mUseForSearchAdapter.addAll(actualWords);

        ListView possibleWordsList = (ListView) view.findViewById(R.id.left);
        possibleWordsList.setAdapter(possibleWordsAdapter);
        ListView useForSearchList = (ListView) view.findViewById(R.id.right);
        useForSearchList.setAdapter(mUseForSearchAdapter);

        possibleWordsList.setOnDragListener(new WordDragListener(possibleWordsList, mUseForSearchAdapter, possibleWordsAdapter));
        useForSearchList.setOnDragListener(new WordDragListener(useForSearchList, possibleWordsAdapter, mUseForSearchAdapter));

        ViewGroup webAddressesContainer = (ViewGroup) view.findViewById(R.id.webAddresses);
        LayoutInflater layoutInflater = getLayoutInflater(savedInstanceState);
        if (mAddresses != null) {
            // add search domain options
            for (final RecentWebAddress address : mAddresses) {
                final View v = layoutInflater.inflate(R.layout.searh_keyword_option,
                        webAddressesContainer, false);
                v.getLayoutParams().width = FlowLayout.LayoutParams.WRAP_CONTENT;
                TextView tv = (TextView) v.findViewById(android.R.id.text1);
                String text = address.getDomain();
                if (TextUtils.equals(text, sLastUsedOption)) {
                    // mark last used domain view
                    v.setSelected(true);
                }
                tv.setText(text);
                v.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // remember last used domain
                        sLastUsedOption = address.getDomain();
                        List<String> selectedWords = new ArrayList<String>(mUseForSearchAdapter.getCount());
                        // get all the selected words
                        for (int i = 0, size = mUseForSearchAdapter.getCount(); i < size; i++) {
                            selectedWords.add(mUseForSearchAdapter.getItem(i));
                        }
                        // build new query
                        String query = TextUtils.join(" ", selectedWords);
                        if(mListener != null){
                            // pass the recent web address clicked event to the
                            // listener
                            mListener.onRecentWebAddressClicked(
                                    query,
                                    address.getId() == 0 ? null : address);
                        }
                        // copy search query to clipboard
                        ClipboardManager clipboard = (ClipboardManager) getActivity()
                                .getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(TAG, query);
                        clipboard.setPrimaryClip(clip);

                        SettingsSnapshot settings = new SettingsSnapshot(MyApplication.getContext());

                        ProductUtils.setProductLastUsedQueryAsync(mSku, query, settings.getUrl());

                        // send the general WEB_SEARCH_ACTIVATED broadcast
                        // event
                        Intent intent = EventBusUtils
                                .getGeneralEventIntent(EventType.WEB_SEARCH_ACTIVATED);
                        // the selected query
                        intent.putExtra(EventBusUtils.TEXT, query);
                        intent.putExtra(EventBusUtils.SKU, mSku);
                        EventBusUtils.sendGeneralEventBroadcast(intent);

                        // close dialog
                        dismissAllowingStateLoss();
                    }
                });
                webAddressesContainer.addView(v);
            }
        }

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // remove title from the dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    /**
     * The interface to implement listener for the recent web address clicked
     * event
     */
    public static interface OnRecentWebAddressClickedListener
    {
        /**
         * Called when the recent web address is clicked in the dialog
         * 
         * @param query the user selected query
         * @param address the address user clicked on. If null means user
         *            clicked to "All of Internet" option
         */
        public void onRecentWebAddressClicked(String query, RecentWebAddress address);
    }
    
    /**
     * WordDragListener will handle dragged word views being dropped on the drop
     * area
     */
    private class WordDragListener implements OnDragListener {
        /**
         * The adapter from where the word should be removed on drop event
         */
        WordsAdapter mCopyFromAdapter;
        /**
         * The adapter to where the word should be added on drop event
         */
        WordsAdapter mCopyToAdapter;
        /**
         * The list where the drop action should be handled
         */
        ListView mCopyToList;

        /**
         * @param list the list where the drop action should be handled
         * @param copyFromAdapter the adapter from where the word should be
         *            removed on drop event
         * @param copyToAdapter the list where the drop action should be handled
         */
        WordDragListener(ListView list, WordsAdapter copyFromAdapter, WordsAdapter copyToAdapter) {
            mCopyToList = list;
            mCopyFromAdapter = copyFromAdapter;
            mCopyToAdapter = copyToAdapter;
        }

        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // no action necessary
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    // no action necessary
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    // no action necessary
                    break;
                case DragEvent.ACTION_DROP:
                    // handle the dragged view being dropped over a drop view

                    // get the dropping view
                    TextView view = (TextView) event.getLocalState();
                    // get the dropping word
                    String word = view.getText().toString();
                    if (mCopyFromAdapter.getPosition(word) != -1) {
                        // if the word is present in the copyFromAdapter. This
                        // mean word is not dropped to the copy to list itself

                        // remove text from the source adapter
                        mCopyFromAdapter.remove(word);

                        // get the index where the text should be inserted to
                        // the target adapter. It depends on the event
                        // coordinates
                        int ind = -1;
                        for (int i = 0; i < mCopyToList.getChildCount(); i++) {
                            View child = mCopyToList.getChildAt(i);
                            // get the middle Y coordinate of the list child
                            float middle = child.getY() + child.getHeight() / 2;
                            if (middle > event.getY()) {
                                // if the middle of the child is more than event
                                // Y coordinate interrupt the loop
                                break;
                            }
                            // remember view position, if loop will end it will
                            // be the last view position the word should be
                            // inserted
                            ind = i;
                        }
                        // get the adapter index from the view position
                        ind += mCopyToList.getFirstVisiblePosition() + 1;
                        // insert word to target adapter
                        mCopyToAdapter.insert(word, ind);
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    // no action necessary
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    /**
     * The list adapter to represent words used for search options
     */
    class WordsAdapter extends ArrayAdapter<String> {

        public WordsAdapter() {
            super(getActivity(), R.layout.searh_keyword_option, android.R.id.text1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View result = super.getView(position, convertView, parent);
            result.setOnTouchListener(new WordTouchListener());
            return result;
        }

        private final class WordTouchListener implements OnTouchListener {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    ClipData data = ClipData.newPlainText("", "");
                    View tv = view.findViewById(android.R.id.text1);
                    DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(tv);
                    // start dragging the item touched
                    tv.startDrag(data, shadowBuilder, tv, 0);
                    return true;
                } else {
                    return false;
                }
            }
        }
    }
}
