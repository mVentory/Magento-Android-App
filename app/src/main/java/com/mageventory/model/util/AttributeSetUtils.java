package com.mageventory.model.util;

import com.mageventory.MageventoryConstants;
import com.mageventory.job.JobCacheManager;

import java.util.List;
import java.util.Map;

/**
 * Various attribue set utilities
 */
public class AttributeSetUtils implements MageventoryConstants {
    /**
     * Get the attribute set specified manual category selection allowed settings
     *
     * @return attribute set specified setting indicating whether the manual category
     * selection operation is allowed or not
     */
    public static boolean isManualCategorySelectionAllowed(Map<String, Object> attributeSetData) {
        return attributeSetData == null ? false : JobCacheManager.safeParseInt(attributeSetData.get(MAGEKEY_ATTRIBUTE_SET_HAS_MATCHING_RULES)) == 0;
    }

    /**
     * Get the attribute set id from the specified attribute set data
     *
     * @param attributeSetData the attribute set data to get the attribute set id from
     * @return attribute set id information for the attribute set data if attribute set data is not null and has the required information, {@link MageventoryConstants#INVALID_ATTRIBUTE_SET_ID} otherwise
     */
    public static int getAttributeSetId(Map<String, Object> attributeSetData) {
        return attributeSetData == null ?
                INVALID_ATTRIBUTE_SET_ID :
                JobCacheManager.safeParseInt(attributeSetData.get(MAGEKEY_ATTRIBUTE_SET_ID),
                        INVALID_ATTRIBUTE_SET_ID);
    }

    /**
     * Find the attribute set information in the list of atttribute sets for the specific attribute set id
     *
     * @param attrSetId     the attribute set id to find
     * @param attributeSets the list of attribtue sets information
     * @return attribute set data for the specified attribute set id if found, null otherwise
     */
    public static Map<String, Object> getAttributeSetForId(int attrSetId, List<Map<String, Object>> attributeSets) {
        Map<String, Object> attributeSetData = null;
        if (attributeSets != null) {
            for (Map<String, Object> attributeSet : attributeSets) {
                if (attrSetId == AttributeSetUtils.getAttributeSetId(attributeSet)) {
                    attributeSetData = attributeSet;
                    break;
                }
            }
        }
        return attributeSetData;
    }
}
