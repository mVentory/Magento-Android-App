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

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.AbsProductActivity;
import com.mageventory.activity.ProductDetailsActivity;
import com.mageventory.activity.ProductEditActivity;
import com.mageventory.activity.WebActivity;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.fragment.SearchOptionsFragment;
import com.mageventory.fragment.SearchOptionsFragment.OnRecentWebAddressClickedListener;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttribute.ContentType;
import com.mageventory.model.CustomAttributeSimple;
import com.mageventory.model.util.ProductUtils;
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
     * List of attributes with the {@link ContentType#WEB_ADDRESS} content type
     */
    ArrayList<CustomAttributeSimple> mWebAddressAttributes;

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
     * @param sku the related product SKU
     * @param lastUsedQuery the last used query if exists
     * @param recentWebAddresses preloaded recent web addresses information
     */
    public void showSearchOptionsDialog(String sku, final String lastUsedQuery,
            final List<RecentWebAddress> recentWebAddresses) {
        initRequiredData();

        SearchOptionsFragment fragment = new SearchOptionsFragment();
//        build the original query from search criteria parts
        final String originalQuery = TextUtils.join(" ", mSearchCriteriaParts);
        fragment.setData(sku, TextUtils.isEmpty(lastUsedQuery) ? mName : lastUsedQuery,
                originalQuery,
                recentWebAddresses,
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
        // initialize the web address custom attributes list
        mWebAddressAttributes = new ArrayList<CustomAttributeSimple>();
        Collection<CustomAttribute> customAttributes = getCustomAttributes();
        processCustomAttributes(customAttributes, mSearchCriteriaParts, mTextAttributes, mWebAddressAttributes);

        // initialize extra attributes such as name, description if necessary
        initExtraAttributes(mSearchCriteriaParts, mTextAttributes, mWebAddressAttributes);
    }

    /**
     * Initialize search criteria parts and text attributes from the custom
     * attributes collection
     * 
     * @param customAttributes collection of custom attributes
     * @param searchCriteriaParts 
     * @param textAttributes
     * @param webAddressAttributes
     */
    protected void processCustomAttributes(Collection<CustomAttribute> customAttributes,
            List<String> searchCriteriaParts, 
            ArrayList<CustomAttributeSimple> textAttributes,
            ArrayList<CustomAttributeSimple> webAddressAttributes
            ) {
        if (customAttributes != null) {
            for (CustomAttribute customAttribute : customAttributes) {
                processCustomAttribute(searchCriteriaParts, textAttributes, webAddressAttributes, customAttribute);
            }
        }
    }

    /**
     * Check the custom attribute options and update search criteria parts and
     * text attributes if necessary
     * 
     * @param searchCriteriaParts
     * @param textAttributes
     * @param webAddressAttributes
     * @param customAttribute
     */
    public void processCustomAttribute(List<String> searchCriteriaParts,
            ArrayList<CustomAttributeSimple> textAttributes,
            ArrayList<CustomAttributeSimple> webAddressAttributes,
            CustomAttribute customAttribute) {
        String value = getValue(customAttribute);
        // check whether attribute is opened for copying data from
        // search and is of type text or textarea
        if (CustomAttribute.canAppendTextFromInternetSearch(customAttribute)) {
            // if text can be appended to attribute
            CustomAttributeSimple attributeSimple = CustomAttributeSimple.from(customAttribute);
            // pass the value to the WebActivity so it will know
            // whether the attribute already has a value
            attributeSimple.setSelectedValue(value);
            textAttributes.add(attributeSimple);
        }

        if (customAttribute.hasContentType(ContentType.WEB_ADDRESS)) {
            // if the attribute has WEB_ADDRESS content type
            CustomAttributeSimple attributeSimple = CustomAttributeSimple.from(customAttribute);
            webAddressAttributes.add(attributeSimple);
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
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

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
        intent.putParcelableArrayListExtra(WebActivity.EXTRA_WEB_ADDRESS_ATTRIBUTES,
                mWebAddressAttributes);
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
    protected abstract Collection<CustomAttribute> getCustomAttributes();

    /**
     * Extra initialization of attributes information. Override this method if
     * some extra attributes should be initialized. This is used in
     * {@link AbsProductActivity}
     * 
     * @param searchCriteriaParts
     * @param textAttributes
     * @param webAddressAttributes
     */
    protected void initExtraAttributes(List<String> searchCriteriaParts,
            ArrayList<CustomAttributeSimple> textAttributes,
            ArrayList<CustomAttributeSimple> webAddressAttributes) {
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
     * internet dialog
     * 
     * @param sku the product SKU to show the search dialog for
     * @param lastUsedQuery last used query for the product if exists
     * @param settingsUrl the profile settings URL
     */
    public void prepareAndShowSearchInternetDialog(String sku, String lastUsedQuery,
            String settingsUrl) {
        new LoadRecentWebAddressesTaskAndShowSearchOptionsDialog(sku, lastUsedQuery,
                settingsUrl)
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
         * The product SKU
         */
        String mSku;

        /**
         * The last used query for the product
         */
        String mLastUsedQuery;

        /**
         * @param sku the product SKU
         * @param lastUsedQuery the last used query for the product
         * @param settingsUrl active settings profile URL
         */
        public LoadRecentWebAddressesTaskAndShowSearchOptionsDialog(String sku,
                String lastUsedQuery,
                String settingsUrl) {
            super(mRecentWebAddressesLoadingControl, settingsUrl);
            mSku = sku;
            mLastUsedQuery = lastUsedQuery;
        }

        @Override
        protected void extraLoadingOperationsAfterRecentWebAddressesAreLoaded() {
            super.extraLoadingOperationsAfterRecentWebAddressesAreLoaded();
            if (TextUtils.isEmpty(mLastUsedQuery)) {
                // if last used query information is not passed
            	//
                // load last used query for the product SKU
                mLastUsedQuery = ProductUtils.getProductLastUsedQuery(mSku, settingsUrl);
            }
        }

        @Override
        protected void onSuccessPostExecute() {
            showSearchOptionsDialog(mSku, mLastUsedQuery, recentWebAddresses);
        }

    }
}
