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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.mageventory.MyApplication;
import com.mageventory.fragment.base.BaseDialogFragment;
import com.mageventory.model.util.ProductUtils;
import com.mageventory.recent_web_address.RecentWebAddress;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.widget.FlowLayout;
import com.mageventory.widget.SearchWordsSet;
import com.mventory.R;

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
     * The components set used to manage search words options
     */
    SearchWordsSet mSearchWordsSet = new SearchWordsSet();
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
        mSearchWordsSet.setData(query, originalQuery);
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
        // initialize search words set
        mSearchWordsSet.init(view, savedInstanceState, getActivity());

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
                        List<String> selectedWords = mSearchWordsSet.getSelectedWords();
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
}
