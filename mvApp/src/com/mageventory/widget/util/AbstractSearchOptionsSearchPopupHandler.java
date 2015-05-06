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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.AbsProductActivity;
import com.mageventory.model.CustomAttribute;

/**
 * The abstract handler with common functionality related to the popup handlers
 * which uses search options selection
 * 
 * @author Eugene Popovich
 */
public abstract class AbstractSearchOptionsSearchPopupHandler {

    /**
     * The search criteria parts used to combine search query
     */
    private List<String> mSearchCriteriaParts;
    
    /**
     * The value for the name attribute
     */
    private String mName;

    /**
     * Check parameters and get the last used query.
     * 
     * @param lastUsedQuery the predefined last used query
     * @return lastUsedQuery parameter in case it is not empty, mName value
     *         otherwise
     */
    public String getLastUsedQuery(String lastUsedQuery) {
        return TextUtils.isEmpty(lastUsedQuery) ? mName : lastUsedQuery;
    }

    /**
     * Build the original query from search criteria parts
     * 
     * @return
     */
    public String getOriginalQuery() {
        return TextUtils.join(" ", mSearchCriteriaParts);
    }

    /**
     * Initialize the search criteria parts and name information
     */
    public void initRequiredData() {
        // initialize the search criteria parts list
        mSearchCriteriaParts = new ArrayList<String>();
        Collection<CustomAttribute> customAttributes = getCustomAttributes();
        processCustomAttributes(customAttributes, mSearchCriteriaParts);

        // initialize extra attributes such as name, description if necessary
        initExtraAttributes(mSearchCriteriaParts);
    }

    /**
     * Initialize search criteria parts from the custom attributes collection
     * 
     * @param customAttributes collection of custom attributes
     * @param searchCriteriaParts
     */
    protected void processCustomAttributes(Collection<CustomAttribute> customAttributes,
            List<String> searchCriteriaParts
            ) {
        if (customAttributes != null) {
            for (CustomAttribute customAttribute : customAttributes) {
                processCustomAttribute(searchCriteriaParts, customAttribute);
            }
        }
    }

    /**
     * Check the custom attribute options and update search criteria parts if
     * necessary
     * 
     * @param searchCriteriaParts
     * @param customAttribute
     */
    public void processCustomAttribute(List<String> searchCriteriaParts,
            CustomAttribute customAttribute) {
        String value = getValue(customAttribute);
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
     */
    protected void initExtraAttributes(List<String> searchCriteriaParts) {
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
}
