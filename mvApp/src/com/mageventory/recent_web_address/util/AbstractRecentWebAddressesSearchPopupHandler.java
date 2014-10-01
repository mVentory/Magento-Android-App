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

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.PopupMenu;

import com.mageventory.R;
import com.mageventory.activity.AbsProductActivity;
import com.mageventory.activity.ProductDetailsActivity;
import com.mageventory.activity.ProductEditActivity;
import com.mageventory.activity.WebActivity;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributeSimple;
import com.mageventory.recent_web_address.RecentWebAddress;
import com.mageventory.recent_web_address.RecentWebAddressProviderAccessor;
import com.mageventory.recent_web_address.RecentWebAddressProviderAccessor.AbstractLoadRecentWebAddressesTask;
import com.mageventory.util.CommonUtils;
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
    Activity mActivity;
    /**
     * Loading control for the recent web addresses loading operation
     */
    LoadingControl mRecentWebAddressesLoadingControl;

    public AbstractRecentWebAddressesSearchPopupHandler(
            LoadingControl recentWebAddressesLoadingControl, WebActivity.Source source,
            Activity activity) {
        mRecentWebAddressesLoadingControl = recentWebAddressesLoadingControl;
        mActivity = activity;
        mSource = source;
    }
    
    /**
     * Show the search internet popup menu for the already loaded recent web
     * addresses information.
     * 
     * @param recentWebAddresses preloaded recent web addresses information
     */
    public void showSearchInternetPopupMenu(final List<RecentWebAddress> recentWebAddresses,
            View view) {

        PopupMenu popup = new PopupMenu(mActivity, view);
        MenuInflater inflater = popup.getMenuInflater();
        Menu menu = popup.getMenu();
        inflater.inflate(R.menu.search_internet, menu);

        // menu item order in the category for the custom menu items sorting
        int order = 1;
        // init dynamic recent web addresses menu items
        for (final RecentWebAddress recentWebAddress : recentWebAddresses) {
            MenuItem mi = menu.add(Menu.NONE, View.NO_ID, order++, recentWebAddress.getDomain());
            mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    // start the web activity for the selected recent web
                    // address menu option
                    startWebActivity(recentWebAddress);
                    return true;
                }
            });
        }
        // set the general on menu item click listener for the static menu items
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int menuItemIndex = item.getItemId();
                switch (menuItemIndex) {
                    case R.id.menu_search_all_of_internet:
                        startWebActivity(null);
                        break;
                    case R.id.menu_search_all_recent:
                        startWebActivityForAddresses(recentWebAddresses);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });

        popup.show();
    }

    /**
     * Start the web activity for the recent web address
     * 
     * @param address to start the activity for. May be null.
     */
    public void startWebActivity(RecentWebAddress address) {
        if (address == null) {
            startWebActivityForAddresses(null);
        } else {
            List<RecentWebAddress> recentWebAddresses = new ArrayList<RecentWebAddress>();
            recentWebAddresses.add(address);
            startWebActivityForAddresses(recentWebAddresses);
        }
    }

    /**
     * Start the web activity for the recent web addresses
     * 
     * @param addresses to start the activity for. May be null.
     */
    public void startWebActivityForAddresses(List<RecentWebAddress> addresses) {
        Intent intent = new Intent(mActivity, WebActivity.class);
        // initialize the search criteria parts list
        List<String> searchCriteriaParts = new ArrayList<String>();

        // pass recent web addresses to the intent extra as a string array list
        // with the recent web addresses domains information
        if (addresses != null) {
            ArrayList<String> searchDomains = new ArrayList<String>();
            for (RecentWebAddress recentWebAddress : addresses) {
                searchDomains.add(recentWebAddress.getDomain());
            }
            intent.putStringArrayListExtra(WebActivity.EXTRA_SEARCH_DOMAINS, searchDomains);
        }

        // initialize the custom text attributes list
        ArrayList<CustomAttributeSimple> textAttributes = new ArrayList<CustomAttributeSimple>();
        List<CustomAttribute> customAttributes = getCustomAttributes();
        initAttributes(searchCriteriaParts, textAttributes, customAttributes);
        // initialize extra attributes such as name, description if necessary
        initExtraAttributes(searchCriteriaParts, textAttributes);
        // Join the searchCriteriaParts with space delimiter and put it as
        // search criteria to the intent extra
        intent.putExtra(WebActivity.EXTRA_SEARCH_QUERY,
                CommonUtils.removeDuplicateWords(TextUtils.join(" ", searchCriteriaParts)));
        // put text attributes information so WebActivity may handle it
        intent.putParcelableArrayListExtra(WebActivity.EXTRA_CUSTOM_TEXT_ATTRIBUTES, textAttributes);
        // tell WebActivity where the request came from
        intent.putExtra(WebActivity.EXTRA_SOURCE, mSource.toString());
        // additional intent initialization if necessary
        initWebActivityIntent(intent);
        mActivity.startActivity(intent);
    }

    /**
     * Initialized searchCriteriaParts and textAttributes from the
     * customAttributes collection
     * 
     * @param searchCriteriaParts the parts for the search criteria filled from
     *            the attributes marked to be used for search
     * @param textAttributes the collection of text attributes marked to be
     *            copied from search
     * @param customAttributes the collection of custom attributes
     */
    public void initAttributes(List<String> searchCriteriaParts,
            ArrayList<CustomAttributeSimple> textAttributes,
            Collection<CustomAttribute> customAttributes) {
        if (customAttributes != null) {
            for (CustomAttribute customAttribute : customAttributes) {
                // additional attribute initialization if necessary
                extraAttributeInit(customAttribute);
                // check whether attribute is opened for copying data from
                // search and is of type text or textarea
                if (customAttribute.isCopyFromSearch()
                        && (customAttribute.isOfType(CustomAttribute.TYPE_TEXT) || customAttribute
                                .isOfType(CustomAttribute.TYPE_TEXTAREA))) {
                    textAttributes.add(CustomAttributeSimple.from(customAttribute));
                }
                // check whether the attribute value should be used as a part of
                // search criteria
                if (customAttribute.isUseForSearch()) {
                    String value = customAttribute.getUserReadableSelectedValue();
                    if (!TextUtils.isEmpty(value)) {
                        searchCriteriaParts.add(value);
                    }
                }
            }
        }
    }

    /**
     * Extra initialization for the attribute used in the initAttributes method.
     * This method may be overridden to perform additional actions on attributes
     * during search initialization
     * 
     * @param customAttribute the custom attribute for extra initialization.
     */
    protected void extraAttributeInit(CustomAttribute customAttribute) {

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
        new LoadRecentWebAddressesTaskAndShowSearchInternetPopup(view, settingsUrl)
                .executeOnExecutor(RecentWebAddressProviderAccessor.sRecentWebAddressesExecutor);
    }

    /**
     * Asynchronous task to load all {@link RecentWebAddress}es information from
     * the database and show search more popup menu on success.
     */
    class LoadRecentWebAddressesTaskAndShowSearchInternetPopup extends
            AbstractLoadRecentWebAddressesTask {

        /**
         * The view for which the popup menu should be shown
         */
        View mView;

        /**
         * @param view for which the popup menu should be shown
         * @param settingsUrl active settings profile URL
         */
        public LoadRecentWebAddressesTaskAndShowSearchInternetPopup(View view, String settingsUrl) {
            super(mRecentWebAddressesLoadingControl, settingsUrl);
            mView = view;
        }

        @Override
        protected void onSuccessPostExecute() {
            showSearchInternetPopupMenu(recentWebAddresses, mView);
        }

    }
}
