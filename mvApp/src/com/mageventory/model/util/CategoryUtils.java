package com.mageventory.model.util;

import java.util.ArrayList;
import java.util.List;

import com.mageventory.MageventoryConstants;
import com.mageventory.model.Category;
import com.mageventory.util.CommonUtils;

/**
 * Various utilities for the {@link Category} class
 * 
 * @author Eugene Popovich
 */
public class CategoryUtils implements MageventoryConstants {
    /**
     * Tag used for logging
     */
    static final String TAG = CategoryUtils.class.getSimpleName();

    /**
     * Convert category IDs from Integer collection to the {@link Object} array
     * 
     * @param categoryIds the category IDs to convert
     * @return {@link Object} array which contains category IDs strings
     */
    public static Object[] getAsObjectArray(Iterable<Integer> categoryIds) {
        List<Object> categoryIdsList = new ArrayList<Object>();
        for (Integer categoryId : categoryIds) {
            if (categoryId.intValue() != INVALID_CATEGORY_ID) {
                categoryIdsList.add(categoryId.toString());
            }
        }
        Object[] result = new Object[categoryIdsList.size()];
        result = categoryIdsList.toArray(result);
        return result;
    }

    /**
     * Convert category IDs from String collection to the Integer
     * {@link ArrayList}
     * 
     * @param categoryIds the category IDs to convert
     * @return Integer {@link ArrayList} which contains category IDs
     */
    public static ArrayList<Integer> getAsIntegerArrayList(Iterable<String> categoryIds) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        // iterate through all product categories and initialize categoryIds
        for (String categoryId : categoryIds) {
            try {
                result.add(Integer.parseInt(categoryId));
            } catch (Throwable e) {
                CommonUtils.error(TAG, e);
            }
        }
        return result;
    }
}
