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

package com.mageventory.widget.util;

import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.fragment.ProductLookupFragment;
import com.mageventory.fragment.ProductLookupFragment.LookupOption;
import com.mageventory.fragment.ProductLookupFragment.OnProductSkuSelectedListener;

/**
 * The abstract handler with common functionality related to the product lookup
 * such as showing product lookup popup and running scan or product selection
 * actions
 * 
 * @author Eugene Popovich
 */
public abstract class AbstractProductLookupPopupHandler extends
        AbstractSearchOptionsSearchPopupHandler {

    /**
     * The activity related to the handler
     */
    BaseFragmentActivity mActivity;

    /**
     * @param activity The activity related to the handler
     */
    public AbstractProductLookupPopupHandler(
            BaseFragmentActivity activity) {
        mActivity = activity;
    }
    
    /**
     * Show the product lookup dialog with the specified parameters
     * 
     * @param listener the product SKU selected listener
     * @param possibleOptions the possible options in the product lookup window.
     *            See {@link LookupOption} for possible values.
     */
    public void showProductLookupDialog(OnProductSkuSelectedListener listener,
            LookupOption... possibleOptions) {
        initRequiredData();

        ProductLookupFragment fragment = new ProductLookupFragment();
        fragment.setData(getLastUsedQuery(null), getOriginalQuery(), listener, possibleOptions);
        fragment.show(mActivity.getSupportFragmentManager(), fragment.getClass().getSimpleName());
    }
}
