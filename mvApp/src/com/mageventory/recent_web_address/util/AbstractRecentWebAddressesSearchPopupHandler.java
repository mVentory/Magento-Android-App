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

package com.mageventory.recent_web_address.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.AbsProductActivity;
import com.mageventory.activity.ProductDetailsActivity;
import com.mageventory.activity.ProductEditActivity;
import com.mageventory.activity.ScanActivity;
import com.mageventory.activity.WebActivity;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.fragment.SearchOptionsFragment;
import com.mageventory.fragment.SearchOptionsFragment.OnRecentWebAddressClickedListener;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributeSimple;
import com.mageventory.recent_web_address.RecentWebAddress;
import com.mageventory.recent_web_address.RecentWebAddressProviderAccessor;
import com.mageventory.recent_web_address.RecentWebAddressProviderAccessor.AbstractLoadRecentWebAddressesTask;
import com.mageventory.util.LoadingControl;

/**
 * The abstract handler with common functionality related to recent web
 * addresses such as show recent web addresses popup and launch
 * {@link WebActivity}
 * 
 * @author Eugene Popovich
 */
public abstract class AbstractRecentWebAddressesSearchPopupHandler {

    /**
     * The source for the WebActivity which uses the handler
     */
    WebActivity.Source mSource;
    /**
     * The activity related to the handler
     */
    BaseFragmentActivity mActivity;
    /**
     * Loading control for the recent web addresses loading operation
     */
    LoadingControl mRecentWebAddressesLoadingControl;

    /**
     * The search criteria parts used to combine search query
     */
    List<String> mSearchCriteriaParts;
    
    /**
     * List of custom text attributes
     */
    ArrayList<CustomAttributeSimple> mTextAttributes;

    /**
     * The value for the name attribute
     */
    String mName;

    public AbstractRecentWebAddressesSearchPopupHandler(
            LoadingControl recentWebAddressesLoadingControl, WebActivity.Source source,
            BaseFragmentActivity activity) {
        mRecentWebAddressesLoadingControl = recentWebAddressesLoadingControl;
        mActivity = activity;
        mSource = source;
    }
    
    /**
     * Show the search Internet options dialog for the already loaded recent web
     * addresses information.
     * 
     * @param recentWebAddresses preloaded recent web addresses information
     */
    public void showSearchOptionsDialog(final List<RecentWebAddress> recentWebAddresses, View view) {
        initRequiredData();

        SearchOptionsFragment fragment = new SearchOptionsFragment();
//        build the original query from search criteria parts
        final String originalQuery = TextUtils.join(" ", mSearchCriteriaParts);
        fragment.setData(mName, originalQuery, recentWebAddresses,
                new OnRecentWebAddressClickedListener() {

                    @Override
                    public void onRecentWebAddressClicked(String query, RecentWebAddress address) {
                        startWebActivity(query, originalQuery, address);
                    }
                });
        fragment.show(mActivity.getSupportFragmentManager(), fragment.getClass().getSimpleName());
    }

    /**
     * Initialize the search criteria parts, name and custom text attributes
     * information
     */
    public void initRequiredData() {
        // initialize the search criteria parts list
        mSearchCriteriaParts = new ArrayList<String>();
        // initialize the custom text attributes list
        mTextAttributes = new ArrayList<CustomAttributeSimple>();
        List<CustomAttribute> customAttributes = getCustomAttributes();
        processCustomAttributes(customAttributes, mSearchCriteriaParts, mTextAttributes);

        // initialize extra attributes such as name, description if necessary
        initExtraAttributes(mSearchCriteriaParts, mTextAttributes);
    }

    /**
     * Initialize search criteria parts and text attributes from the custom
     * attributes collection
     * 
     * @param customAttributes collection of custom attributes
     * @param searchCriteriaParts 
     * @param textAttributes
     */
    protected void processCustomAttributes(Collection<CustomAttribute> customAttributes,
            List<String> searchCriteriaParts, ArrayList<CustomAttributeSimple> textAttributes) {
        if (customAttributes != null) {
            for (CustomAttribute customAttribute : customAttributes) {
                processCustomAttribute(searchCriteriaParts, textAttributes, customAttribute);
            }
        }
    }

    /**
     * Check the custom attribute options and update search criteria parts and
     * text attributes if necessary
     * 
     * @param searchCriteriaParts
     * @param textAttributes
     * @param customAttribute
     */
    public void processCustomAttribute(List<String> searchCriteriaParts,
            ArrayList<CustomAttributeSimple> textAttributes, CustomAttribute customAttribute) {
        String value = getValue(customAttribute);
        // check whether attribute is opened for copying data from
        // search and is of type text or textarea
        if (customAttribute.isCopyFromSearch()
                && (customAttribute.isOfType(CustomAttribute.TYPE_TEXT) || customAttribute
                        .isOfType(CustomAttribute.TYPE_TEXTAREA))) {
            CustomAttributeSimple attributeSimple = CustomAttributeSimple.from(customAttribute);
            // pass the value to the WebActivity so it will know
            // whether the attribute already has a value
            attributeSimple.setSelectedValue(value);
            textAttributes.add(attributeSimple);
        }
        // check whether the attribute value should be used as a part of
        // search criteria
        if (customAttribute.isUseForSearch()) {
            if (!TextUtils.isEmpty(value)) {
                if (TextUtils.equals(customAttribute.getCode(),
                        MageventoryConstants.MAGEKEY_PRODUCT_SKU)
                        || TextUtils.equals(customAttribute.getCode(),
                                MageventoryConstants.MAGEKEY_PRODUCT_BARCODE)) {
                    // barcodes and SKUs are parsed in the
                    // special way
                    parseCode(searchCriteriaParts, value);
                } else {
                    searchCriteriaParts.add(value);
                }
            }
        }
    }

    /**
     * Get the custom attribute value. May be overridden for various special cases
     * 
     * @param customAttribute
     * @return
     */
    protected String getValue(CustomAttribute customAttribute) {
        String value = customAttribute.getUserReadableSelectedValue();
        if (TextUtils.equals(MageventoryConstants.MAGEKEY_PRODUCT_NAME, customAttribute.getCode())) {
            // remember product name
            mName = value;
        }
        return value;
    }

    /**
     * Start the web activity for the recent web address
     * 
     * @param query the query which should be used for search
     * @param originalQuery the original query containing all possible search
     *            words
     * @param address to start the activity for. May be null.
     */
    public void startWebActivity(String query, String originalQuery, RecentWebAddress address) {
        if (address == null) {
            startWebActivityForAddresses(query, originalQuery, null);
        } else {
            List<RecentWebAddress> recentWebAddresses = new ArrayList<RecentWebAddress>();
            recentWebAddresses.add(address);
            startWebActivityForAddresses(query, originalQuery, recentWebAddresses);
        }
    }

    /**
     * Start the web activity for the recent web addresses
     * 
     * @param query the query which should be used for search
     * @param originalQuery the original query containing all possible search
     *            words
     * @param addresses to start the activity for. May be null.
     */
    public void startWebActivityForAddresses(String query, String originalQuery,
            List<RecentWebAddress> addresses) {
        Intent intent = new Intent(mActivity, WebActivity.class);

        // pass recent web addresses to the intent extra as a string array list
        // with the recent web addresses domains information
        if (addresses != null) {
            ArrayList<String> searchDomains = new ArrayList<String>();
            for (RecentWebAddress recentWebAddress : addresses) {
                searchDomains.add(recentWebAddress.getDomain());
            }
            intent.putStringArrayListExtra(WebActivity.EXTRA_SEARCH_DOMAINS, searchDomains);
        }

        // put the search queries information
        intent.putExtra(WebActivity.EXTRA_SEARCH_QUERY, query);
        intent.putExtra(WebActivity.EXTRA_SEARCH_ORIGINAL_QUERY, originalQuery);
        // put text attributes information so WebActivity may handle it
        intent.putParcelableArrayListExtra(WebActivity.EXTRA_CUSTOM_TEXT_ATTRIBUTES,
                mTextAttributes);
        // tell WebActivity where the request came from
        intent.putExtra(WebActivity.EXTRA_SOURCE, mSource.toString());
        // additional intent initialization if necessary
        initWebActivityIntent(intent);
        mActivity.startActivity(intent);
    }

    /**
     * Get the custom attributes. Implementation should to return valid custom
     * attributes list related to the place where the handler is used
     * 
     * @return
     */
    protected abstract List<CustomAttribute> getCustomAttributes();

    /**
     * Extra initialization of attributes information. Override this method if
     * some extra attributes should be initialized. This is used in
     * {@link AbsProductActivity}
     * 
     * @param searchCriteriaParts
     * @param textAttributes
     */
    protected void initExtraAttributes(List<String> searchCriteriaParts,
            ArrayList<CustomAttributeSimple> textAttributes) {
    }

    /**
     * Extra initialization of {@link WebActivity} intent. Override this method
     * if some extra intent parameter should be added. Used to pass product sku
     * in the {@link ProductEditActivity} and {@link ProductDetailsActivity}
     * 
     * @param intent
     */
    protected void initWebActivityIntent(Intent intent) {
    }

    /**
     * Load the recent web addresses information and show it in the search
     * internet popup menu
     */
    public void prepareAndShowSearchInternetMenu(View view, String settingsUrl) {
        new LoadRecentWebAddressesTaskAndShowSearchOptionsDialog(view, settingsUrl)
                .executeOnExecutor(RecentWebAddressProviderAccessor.sRecentWebAddressesExecutor);
    }

    /**
     * Parse the SKU or Barcode in the special way to few words and put to the
     * searchCriteriaParts
     * 
     * @param searchCriteriaParts
     * @param text the text to parse
     */
    public static void parseCode(List<String> searchCriteriaParts, String text) {
        if (ScanActivity.isSKUInTheRightFormat(text)) {
            // do nothing with the correctly formatted SKU, it should not be
            // present in search query
            return;
        }
        Pattern pattern = Pattern.compile("[\\W]+");
        Matcher matcher = pattern.matcher(text);
        List<String> words = new ArrayList<String>();
        while (matcher.find()) {
            // add the substring from start to the found matches start
            words.add(text.substring(0, matcher.start()));
        }
        // add the whole text itself
        searchCriteriaParts.add(text);
        // only no more than 2 last substrings are allowed
        for (int i = 1, size = Math.min(2, words.size()); i <= size; i++) {
            searchCriteriaParts.add(words.get(words.size() - i));
        }
    }
    /**
     * Asynchronous task to load all {@link RecentWebAddress}es information from
     * the database and show search options dialog on success.
     */
    class LoadRecentWebAddressesTaskAndShowSearchOptionsDialog extends
            AbstractLoadRecentWebAddressesTask {

        /**
         * The view for which the popup menu should be shown
         */
        View mView;

        /**
         * @param view for which the popup menu should be shown
         * @param settingsUrl active settings profile URL
         */
        public LoadRecentWebAddressesTaskAndShowSearchOptionsDialog(View view, String settingsUrl) {
            super(mRecentWebAddressesLoadingControl, settingsUrl);
            mView = view;
        }

        @Override
        protected void onSuccessPostExecute() {
            showSearchOptionsDialog(recentWebAddresses, mView);
        }

    }
}
