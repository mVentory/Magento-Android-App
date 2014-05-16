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

package com.mageventory.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.job.JobCacheManager;
import com.mageventory.util.CommonUtils;

public class Product implements MageventoryConstants, Serializable {

    /**
     * This Class contains Attributes Information
     * 
     * @author hussein
     */
    public class CustomAttributeInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String label;
        private String type;
        private String key;
        private String value;
        private String valueLabel;
        private boolean configurable;
        Object[] Options;

        /**
         * @return the label
         */
        public String getLabel() {
            return label;
        }

        /**
         * @param label the label to set
         */
        public void setLabel(String label) {
            this.label = label;
        }

        /**
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * @return the key
         */
        public String getKey() {
            return key;
        }

        /**
         * @param key the key to set
         */
        public void setKey(String key) {
            this.key = key;
        }

        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }

        /**
         * @param value the value to set
         */
        public void setValue(String value) {
            this.value = value;
        }

        /**
         * @return the valueLabel
         */
        public String getValueLabel() {
            return valueLabel;
        }

        /**
         * @param valueLabel the valueLabel to set
         */
        public void setValueLabel(String valueLabel) {

            this.valueLabel = valueLabel;
            // Special Types Handling
            // 1- if Type is Date then remove "00:00:00"
            this.valueLabel = this.valueLabel.replace("00:00:00", "");

            // 2- if type is float remove ".0000"
            this.valueLabel = this.valueLabel.replace(".0000", "");

        }

        /**
         * @return the options
         */
        public Object[] getOptions() {
            return Options;
        }

        /**
         * @param options_objects the options to set
         */
        public void setOptions(Object[] options_objects) {
            Options = options_objects;
        }

        public boolean isConfigurable() {
            return configurable;
        }

        public void setConfigurable(boolean configurable) {
            this.configurable = configurable;
        }
    };

    public class SiblingInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id; // PRODUCT ID
        private String sku; // PRODUCT SKU
        private String name; // PRODUCT NAME
        private Double price; // PRODUCT PRICE
        private String quantity; // QUANTITY
        Map<String, String> configurableAttribues = new HashMap<String, String>();

        public void addConfigurableAttributeValue(String key, String value)
        {
            configurableAttribues.put(key, value);
        }

        public String getConfigurableAttributeValue(String key)
        {
            return configurableAttribues.get(key);
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public String getQuantity() {
            return quantity;
        }

        public void setQuantity(String quantity) {
            this.quantity = quantity;
        }
    }

    /**
     * This Class contains Image Information
     * 
     * @author hussein
     */
    public class imageInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String imgURL;
        private String imgName;
        private int imgPosition;
        private boolean isMain;

        public void setMain(boolean isMain) {
            this.isMain = isMain;
        }

        public boolean getMain() {
            return isMain;
        }

        /**
         * @return the imgURL
         */
        public String getImgURL() {
            return imgURL;
        }

        /**
         * @param imgURL the imgURL to set
         */
        public void setImgURL(String imgURL) {
            this.imgURL = imgURL;
        }

        /**
         * @return the imgName
         */
        public String getImgName() {
            return imgName;
        }

        /**
         * @param imgName the imgName to set
         */
        public void setImgName(String imgName) {
            this.imgName = imgName;
        }

        /**
         * @return the imgPosition
         */
        public int getImgPosition() {
            return imgPosition;
        }

        /**
         * @param imgPosition the imgPosition to set
         */
        public void setImgPosition(int imgPosition) {
            this.imgPosition = imgPosition;
        }
    }

    private static final long serialVersionUID = 1L;

    private boolean tmRelist;
    private boolean tmAllowBuyNow;
    private boolean tmAddTMFees;
    private int tmShippingTypeID;

    private String[] tmAccountIds;
    private String[] tmAccountLabels;

    private int tmPreselectedCategoryId = INVALID_CATEGORY_ID;
    private String tmPreselectedCategoryPath;
    private int tmDefaultPreselectedCategoryID;

    private int[] tmShippingTypeIds;
    private String[] tmShippingTypeLabels;

    private Integer tmListingID;
    private Double weight; // WEIGHT
    private String id; // PRODUCT ID
    private int visibility; // VISIBILITY
    private Object tirePrice; // TIRE PRICE
    private String urlPath; // URL PATH
    private int attributeSetId = INVALID_ATTRIBUTE_SET_ID; // ATTRIBUTE SET ID
    private String type; // PRODUCT TYPE
    private ArrayList<CustomAttributeInfo> attrList = new ArrayList<Product.CustomAttributeInfo>(); // LIST
    private List<SiblingInfo> siblingsList = new ArrayList<SiblingInfo>(); // LIST
                                                                           // OF
                                                                           // CUSTOM
                                                                           // ATTRIBUTES
    private int requiredOptions; // REQUIRED OPTIONS
    private String shortDescription; // PRODUCT SHORT DESCRIPTION
    private String optionsContainer; // OPTIONS CONTAINER
    private int taxClassId; // TAX CLASS ID
    private String description; // PRODUCT DESCRIPTION
    private String name; // PRODUCT NAME
    private String urlKey; // URL KEY
    private int googleCheckoutEnabled; // ENABLE GOOGLE CHECKOUT
    private int status; // STATUS
    private String quantity; // QUANTITY
    private int hasOptions; // HAS OPTIONS
    private ArrayList<Object> webSites = new ArrayList<Object>(); // WEBSITES
    private String sku; // PRODUCT SKU
    private String typeId; // TYPE ID
    private ArrayList<String> categoriesIds = new ArrayList<String>(); // CATEGORIES
                                                                       // IDs
                                                                       // LIST
    private Double price; // PRODUCT PRICE
    private Double specialPrice; // PRODUCT SPECIAL PRICE
    private Date specialFromDate;
    private Date specialToDate;
    private ArrayList<imageInfo> images = new ArrayList<imageInfo>(); // IMAGES
    private int isInStock; // IS IN STOCK

    private String maincategory; // Main Category ID
    private Double cost; // PRODUCT COST
    private Boolean enabled; // ENABLED
    private int manageStock; // MANAGE STOCK
    private int isQtyDecimal;

    private Map<String, Object> data; // DATA -- CONTAINS ALL PRODUCT INFO
    private String url; // PRODUCT SHOP URL

    /*
     * When an object of this Product class is stored in cache then its data is
     * merged with product edit job data (if there are any pending edit jobs).
     * The merge is done because we want to show the product to the user (for
     * example in ProductDetailsActivity) the way it is going to look like after
     * the edit succeeds. The problem is that edit job can fail and the we need
     * to revert the merge. In order to do that easily we just store a copy of
     * unmerged product here.
     */
    private Product unmergedProduct;

    /************************************** SETTERS **********************************************/

    public void setUnmergedProduct(Product unmergedProd)
    {
        unmergedProduct = unmergedProd;
    }

    /************************************** GETTERS **********************************************/

    public Product getUnmergedProduct()
    {
        return unmergedProduct;
    }

    public Map<String, Object> getData() {
        return data;
    }

    /**
     * @return the visibility
     */
    public int getVisibility() {
        return visibility;
    }

    /**
     * @return the tirePrice
     */
    public Object getTirePrice() {
        return tirePrice;
    }

    /**
     * @return the urlPath
     */
    public String getUrlPath() {
        return urlPath;
    }

    /**
     * @return the attrList
     */
    public ArrayList<CustomAttributeInfo> getAttrList() {
        return attrList;
    }

    public List<SiblingInfo> getSiblingsList()
    {
        return siblingsList;
    }

    /**
     * @return the requiredOptions
     */
    public int getRequiredOptions() {
        return requiredOptions;
    }

    /**
     * @return the optionsContainer
     */
    public String getOptionsContainer() {
        return optionsContainer;
    }

    /**
     * @return the taxClassId
     */
    public int getTaxClassId() {
        return taxClassId;
    }

    /**
     * @return the urlKey
     */
    public String getUrlKey() {
        return urlKey;
    }

    /**
     * @return the googleCheckoutEnabled
     */
    public int getGoogleCheckoutEnabled() {
        return googleCheckoutEnabled;
    }

    /**
     * @return the hasOptions
     */
    public int getHasOptions() {
        return hasOptions;
    }

    /**
     * @return the webSites
     */
    public ArrayList<Object> getWebSites() {
        return webSites;
    }

    /**
     * @return the typeId
     */
    public String getTypeId() {
        return typeId;
    }

    /**
     * @return the images
     */
    public ArrayList<imageInfo> getImages() {
        return images;
    }

    /**
     * @return the manageStock
     */
    public int getManageStock() {
        return manageStock;
    }

    public int getIsQtyDecimal() {
        return isQtyDecimal;
    }

    public String getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {

        String strPrice = String.valueOf(price);

        String[] strPriceParts = strPrice.split("\\.");

        // if > 1 --> then there is fraction
        if (strPriceParts.length > 1) {
            // check if fraction is zero
            if (Integer.valueOf(strPriceParts[1]) == 0)
                // Fraction is Zero return part before fraction only
                return strPriceParts[0];
        }

        // Fraction is not zero then return it as it is
        return strPrice;
    }

    public Double getSpecialPrice() {
        return specialPrice;
    }

    public Date getSpecialFromDate() {
        return specialFromDate;
    }

    public Date getSpecialToDate() {
        return specialToDate;
    }

    public Double getCost() {
        return cost;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Double getWeight() {
        return weight;
    }

    public Integer getTMListingID() {
        return tmListingID;
    }

    public boolean getTMRelistFlag()
    {
        return tmRelist;
    }

    public boolean getAddTMFeesFlag()
    {
        return tmAddTMFees;
    }

    public boolean getTMAllowBuyNowFlag()
    {
        return tmAllowBuyNow;
    }

    public int getShippingTypeID()
    {
        return tmShippingTypeID;
    }

    public int getTmPreselectedCategoryId() {
        return tmPreselectedCategoryId;
    }

    public String getTmPreselectedCategoryPath() {
        return tmPreselectedCategoryPath;
    }

    public int getTMDefaultPreselectedCategoryID()
    {
        return tmDefaultPreselectedCategoryID;
    }

    public String[] getTMAccountIDs()
    {
        return tmAccountIds;
    }

    public String[] getTMAccountLabels()
    {
        return tmAccountLabels;
    }

    public int[] getTMShippingTypeIDs()
    {
        return tmShippingTypeIds;
    }

    public String[] getTMShippingTypeLabels()
    {
        return tmShippingTypeLabels;
    }

    public int getAttributeSetId() {
        return attributeSetId;
    }

    public String getMaincategory() {
        return maincategory;
    }

    public int getStatus() {
        return status;
    }

    public String getQuantity() {

        return quantity;
    }

    public void setQuantity(String q) {

        data.put(MAGEKEY_PRODUCT_QUANTITY, q);
        quantity = q;
    }

    public int getIsInStock() {
        return isInStock;
    }

    /**
     * @return the attrList
     */
    public ArrayList<CustomAttributeInfo> getAttrValuesList() {
        return attrList;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**********************************************************************************************************/

    /************************************** PARSING FUNCTIONS **********************************************/
    public static int safeParseInt(Map<String, Object> map, String key) {
        final Object o = map.get(key);
        if (o != null) {
            if (o instanceof String) {
                final String s = (String) o;
                try
                {
                    return Integer.parseInt(s);
                } catch (NumberFormatException nfe)
                {
                    return 0;
                }
            } else if (o instanceof Number) {
                return ((Number) o).intValue();
            } else if (o instanceof Boolean) {
                return ((Boolean) o) ? 1 : 0;
            }
        }
        return 0;
    }

    private static double safeParseDouble(Map<String, Object> map, String key) {
        return safeParseDouble(map, key, 0d);
    }

    public static Double safeParseDouble(Map<String, Object> map, String key,
            Double defaultValue) {
        final Object o = map.get(key);
        if (o != null) {
            if (o instanceof String) {
                final String s = (String) o;
                try {
                    return Double.parseDouble(s);
                } catch (NumberFormatException e) {
                }
            } else if (o instanceof Double) {
                return ((Double) o).doubleValue();
            }
        }
        return defaultValue;
    }

    public static Date safeParseDate(Map<String, Object> map, String key,
            Date defaultValue) {
        final Object o = map.get(key);
        if (o != null) {
            if (o instanceof String) {
                final String s = (String) o;
                return TextUtils.isEmpty(s) ? null : CommonUtils.parseDateTime(s);
            } else if (o instanceof Date) {
                return (Date) o;
            }
        }
        return defaultValue;
    }

    /**********************************************************************************************************/

    /************************************** CONSTRUCTORS ***********************************************/

    public Product getCopy() {
        return new Product(data);
    }

    /*
     * Returns the new child in case it doesn't already exist or the existing
     * one otherwise.
     */
    private Map<String, Object> addElementToArrayOfTmCategoryMaps(Map<String, Object> parentMap,
            Map<String, Object> newChild)
    {
        /* Check the child exists in the array */
        boolean exists = false;

        Object[] children = JobCacheManager.getObjectArrayFromDeserializedItem(parentMap
                .get(MAGEKEY_CATEGORY_CHILDREN));

        for (int i = 0; i < children.length; i++)
        {
            String childCatName = (String) (((Map<String, Object>) children[i])
                    .get(MAGEKEY_CATEGORY_NAME));
            String newChildCatName = (String) (newChild.get(MAGEKEY_CATEGORY_NAME));

            if (childCatName.equals(newChildCatName))
            {
                exists = true;
                newChild = (Map<String, Object>) children[i];
                break;
            }
        }

        if (!exists)
        {
            Object[] newChildrenArray = new Object[children.length + 1];
            System.arraycopy(children, 0, newChildrenArray, 0, children.length);
            newChildrenArray[children.length] = newChild;

            parentMap.put(MAGEKEY_CATEGORY_CHILDREN, newChildrenArray);
        }

        return newChild;
    }

    @SuppressWarnings("unchecked")
    public Product(Map<String, Object> map) {
        data = map;

        this.name = "" + map.get(MAGEKEY_PRODUCT_NAME); // GET NAME [USEFUL]
        this.id = "" + map.get(MAGEKEY_PRODUCT_ID); // GET PRODUCT ID [USEFUL]
        this.sku = "" + map.get(MAGEKEY_PRODUCT_SKU); // GET SKU [USEFUL]

        if (map.get(MAGEKEY_TM_CATEGORY_MATCH_ID) != null)
        {
            tmDefaultPreselectedCategoryID = safeParseInt(map, MAGEKEY_TM_CATEGORY_MATCH_ID);
        }
        else
        {
            tmDefaultPreselectedCategoryID = INVALID_CATEGORY_ID;
        }

        final Map<String, Object> tm_options = (Map<String, Object>) map
                .get(MAGEKEY_PRODUCT_TM_OPTIONS);

        if (tm_options != null)
        {
            if (tm_options.get(MAGEKEY_PRODUCT_LISTING_ID) != null)
            {
                this.tmListingID = safeParseInt(tm_options, MAGEKEY_PRODUCT_LISTING_ID);
            }

            this.tmAddTMFees = (safeParseInt(tm_options, MAGEKEY_PRODUCT_ADD_TM_FEES) == 0 ? false
                    : true);
            this.tmRelist = (safeParseInt(tm_options, MAGEKEY_PRODUCT_RELIST) == 0 ? false : true);
            this.tmAllowBuyNow = (safeParseInt(tm_options, MAGEKEY_PRODUCT_ALLOW_BUY_NOW) == 0 ? false
                    : true);
            this.tmShippingTypeID = safeParseInt(tm_options, MAGEKEY_PRODUCT_SHIPPING_TYPE_ID);

            Map<String, Object> matchedCategories = (Map<String, Object>) tm_options
                    .get(MAGEKEY_PRODUCT_MATCHED_CATEGORY);
            if(matchedCategories != null)
            {
                tmPreselectedCategoryId = safeParseInt(matchedCategories,
                        MAGEKEY_PRODUCT_MATCHED_CATEGORY_ID);
                if(tmPreselectedCategoryId == 0)
                {
                    tmPreselectedCategoryId = INVALID_CATEGORY_ID;
                }
                tmPreselectedCategoryPath = (String) matchedCategories
                        .get(MAGEKEY_PRODUCT_MATCHED_CATEGORY_NAME);
            }

            if (tm_options.get(MAGEKEY_PRODUCT_SHIPPING_TYPES_LIST) != null)
            {
                Object[] shippingTypes = JobCacheManager
                        .getObjectArrayFromDeserializedItem(tm_options
                                .get(MAGEKEY_PRODUCT_SHIPPING_TYPES_LIST));

                tmShippingTypeIds = new int[shippingTypes.length];
                tmShippingTypeLabels = new String[shippingTypes.length];

                for (int i = 0; i < shippingTypes.length; i++)
                {
                    Map<String, Object> shippingType = ((Map<String, Object>) shippingTypes[i]);

                    tmShippingTypeIds[i] = JobCacheManager.getIntValue(shippingType.get("value"));
                    tmShippingTypeLabels[i] = (String) shippingType.get("label");
                }
            }

            if (tm_options.get(MAGEKEY_PRODUCT_TM_ACCOUNTS) != null)
            {
                Object tmAccounts = tm_options.get(MAGEKEY_PRODUCT_TM_ACCOUNTS);

                Set<String> keys;

                if (tmAccounts instanceof Map)
                {
                    keys = ((Map<String, Object>) tmAccounts).keySet();
                }
                else
                {
                    keys = new HashSet<String>();
                }

                ArrayList<String> keysSorted = new ArrayList<String>(keys);
                keysSorted.remove("");

                Collections.sort(keysSorted);

                tmAccountIds = new String[keysSorted.size()];
                tmAccountLabels = new String[keysSorted.size()];

                int i = 0;
                for (String key : keysSorted)
                {
                    tmAccountIds[i] = key;
                    tmAccountLabels[i] = (String) ((Map<String, Object>) tm_options
                            .get(MAGEKEY_PRODUCT_TM_ACCOUNTS)).get(key);
                    i++;
                }
            }
        }

        this.weight = safeParseDouble(map, MAGEKEY_PRODUCT_WEIGHT); // GET
                                                                    // WEIGHT
                                                                    // [USEFUL]
        this.status = safeParseInt(map, MAGEKEY_PRODUCT_STATUS); // GET
                                                                 // STATUS
                                                                 // [USEFUL]
        this.price = safeParseDouble(map, MAGEKEY_PRODUCT_PRICE); // GET
                                                                  // PRICE
                                                                  // [USEFUL]

        this.specialPrice = safeParseDouble(map, MAGEKEY_PRODUCT_SPECIAL_PRICE, null);
        this.specialFromDate = safeParseDate(map, MAGEKEY_PRODUCT_SPECIAL_FROM_DATE, null);
        this.specialToDate = safeParseDate(map, MAGEKEY_PRODUCT_SPECIAL_TO_DATE, null);

        if (map.get(MAGEKEY_PRODUCT_DESCRIPTION) == null)
        {
            this.description = "";
        }
        else
        {
            this.description = "" + map.get(MAGEKEY_PRODUCT_DESCRIPTION); // GET
                                                                          // DESCRIPTION
                                                                          // [USEFUL]
        }

        this.quantity = "" + map.get(MAGEKEY_PRODUCT_QUANTITY); // GET
                                                                // QUANTITY
                                                                // [USEFUL]
        // If QTY = -1 Manage Stock is Not Enabled then QTY is NULL
        if (quantity.contains("-1000000"))
            quantity = "";

        this.visibility = safeParseInt(map, MAGEKEY_PRODUCT_VISIBILITY); // GET
                                                                         // VISIBILITY
                                                                         // [NOT
                                                                         // USEFUL]
        this.tirePrice = (Object) map.get(MAGEKEY_PRODUCT_TIER_PRICE); // GET
                                                                       // TIRE
                                                                       // PRICE
                                                                       // [NOT
                                                                       // USEFUL]
        this.urlPath = "" + map.get(MAGEKEY_PRODUCT_URL_PATH); // GET URL
                                                               // PATH
                                                               // [USEFUL]

        this.attributeSetId = safeParseInt(map, MAGEKEY_PRODUCT_ATTRIBUTE_SET_ID); // GET
                                                                                   // ATTRIBUTE
                                                                                   // SET
                                                                                   // ID
                                                                                   // [USEFUL]
        // Check Attribute Set ID
        attributeSetId = attributeSetId > 0 ? attributeSetId : INVALID_ATTRIBUTE_SET_ID;

        this.type = "" + map.get(MAGEKEY_PRODUCT_TYPE); // GET TYPE [NOT
                                                        // USEFUL]
        this.requiredOptions = safeParseInt(map, MAGEKEY_PRODUCT_REQUIRED_OPTIONS); // GET
                                                                                    // REQUIRED
                                                                                    // OPTIONS
                                                                                    // [NOT
                                                                                    // USEFUL]
        this.shortDescription = "" + map.get(MAGEKEY_PRODUCT_SHORT_DESCRIPTION); // GET
                                                                                 // SHORT
                                                                                 // DESCRIPTION
                                                                                 // [NOT
                                                                                 // USEFUL]
        this.optionsContainer = "" + map.get(MAGEKEY_PRODUCT_OPTIONS_CONTAINER); // GET
                                                                                 // OPTIONS
                                                                                 // CONTAINER
                                                                                 // [NOT
                                                                                 // USEFUL]
        this.taxClassId = safeParseInt(map, MAGEKEY_PRODUCT_TAX_CLASS_ID); // GET
                                                                           // TAX
                                                                           // CLASS
                                                                           // ID
                                                                           // [NOT
                                                                           // USEFUL]
        this.urlKey = "" + map.get(MAGEKEY_PRODUCT_URL_KEY); // GET URL KEY
                                                             // [NOT
                                                             // USEFUL]
        this.googleCheckoutEnabled = safeParseInt(map, MAGEKEY_PRODUCT_ENABLE_GOOGLE_CHECKOUT); // GET
                                                                                                // GOOGLE
                                                                                                // CHECKOUT
                                                                                                // [NOT
                                                                                                // USEFUL]
        this.hasOptions = safeParseInt(map, MAGEKEY_PRODUCT_HAS_OPTIONS); // GET
                                                                          // HAS
                                                                          // OPTIONS
                                                                          // [NOT
                                                                          // USEFUL]
        this.typeId = "" + map.get(MAGEKEY_PRODUCT_TYPE_ID); // GET TYPE ID
                                                             // [NOT
                                                             // USEFUL]
        this.isInStock = safeParseInt(map, MAGEKEY_PRODUCT_IS_IN_STOCK); // GET
                                                                         // IS
                                                                         // IN
                                                                         // STOCK
                                                                         // [NOT
                                                                         // USEFUL]

        /*
         * If the api didn't return "manage_stock" flag then assume the stock is
         * to be managed by default.
         */
        if (map.get(MAGEKEY_PRODUCT_MANAGE_INVENTORY) != null)
        {
            this.manageStock = safeParseInt(map, MAGEKEY_PRODUCT_MANAGE_INVENTORY);
        }
        else
        {
            this.manageStock = 1;
        }

        /*
         * If the api didn't return "is_qty_decimal" flag then assume the
         * decimal qty is not allowed.
         */
        if (map.get(MAGEKEY_PRODUCT_IS_QTY_DECIMAL) != null)
        {
            this.isQtyDecimal = safeParseInt(map, MAGEKEY_PRODUCT_IS_QTY_DECIMAL);
        }
        else
        {
            this.isQtyDecimal = 0;
        }

        // Get Categories IDs & Categories
        Object[] categories_Ids = JobCacheManager.getObjectArrayFromDeserializedItem(map
                .get(MAGEKEY_PRODUCT_CATEGORY_IDS));

        if (categories_Ids != null) {
            for (int i = 0; i < categories_Ids.length; i++) {
                this.categoriesIds.add(categories_Ids[i].toString());
            }
        }

        // Set Main Category
        if (this.categoriesIds.size() > 0) {
            this.maincategory = this.categoriesIds.get(0);
        }

        // GET IMAGES
        Object[] local_images = JobCacheManager.getObjectArrayFromDeserializedItem(map
                .get(MAGEKEY_PRODUCT_IMAGES));

        if (local_images != null) {
            for (int i = 0; i < local_images.length; i++) {
                imageInfo info = new imageInfo();
                Map<String, Object> local_image_info = (Map<String, Object>) local_images[i];
                info.setImgName(local_image_info.get("file").toString());
                info.setImgURL(local_image_info.get("url").toString());
                info.setImgPosition(safeParseInt(local_image_info, "position"));

                Object[] types = JobCacheManager
                        .getObjectArrayFromDeserializedItem(local_image_info.get("types"));
                info.setMain(false);

                for (int j = 0; j < types.length; j++) {
                    if (((String) types[j]).equals("image"))
                        ;
                    {
                        info.setMain(true);
                    }
                }

                images.add(info);
            }
        }

        Object[] local_attrInfo = JobCacheManager.getObjectArrayFromDeserializedItem(map
                .get(MageventoryConstants.MAGEKEY_PRODUCT_ATTRIBUTES));

        List<String> configurableAttributes = new ArrayList<String>();
        // Search For this Custom Attribute in Attribute List
        for (int j = 0; j < local_attrInfo.length; j++) {
            Map<String, Object> local_attr = (Map<String, Object>) local_attrInfo[j];
            String attribCode = (String) (local_attr.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE));

            if (attribCode.endsWith("_") && (String) map.get(attribCode) != null) {
                String attribValue = (String) map.get(attribCode);

                CustomAttributeInfo customInfo = new CustomAttributeInfo();
                customInfo.setKey((String) (local_attr.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE)));
                String label = (String) local_attr.get(MAGEKEY_ATTRIBUTE_LABEL);
                if (label == null) {
                    label = "";
                }
                customInfo.setLabel(label);

                customInfo.setType(local_attr.get("frontend_input").toString());
                customInfo.setValue(attribValue);
                customInfo
                        .setConfigurable(safeParseInt(local_attr, MAGEKEY_ATTRIBUTE_CONFIGURABLE) == 1);
                if (customInfo.isConfigurable())
                {
                    configurableAttributes.add(customInfo.getKey());
                }
                Object[] options_objects = JobCacheManager
                        .getObjectArrayFromDeserializedItem(local_attr
                                .get(MAGEKEY_ATTRIBUTE_OPTIONS));

                customInfo.setValueLabel(getValueLabel(attribValue, options_objects));

                customInfo.setOptions(options_objects);

                this.attrList.add(customInfo);
            }
        }
        Object[] siblings = JobCacheManager.getObjectArrayFromDeserializedItem(map.get("siblings"));
        if (siblings != null)
        {
            for (int i = 0, size = siblings.length; i < size; i++)
            {
                Map<String, Object> siblingInfoMap = (Map<String, Object>) siblings[i];
                SiblingInfo siblingInfo = new SiblingInfo();
                siblingInfo.setName("" + siblingInfoMap.get(MAGEKEY_PRODUCT_NAME));
                siblingInfo.setId("" + siblingInfoMap.get(MAGEKEY_PRODUCT_ID));
                siblingInfo.setSku("" + siblingInfoMap.get(MAGEKEY_PRODUCT_SKU));
                siblingInfo.setPrice(safeParseDouble(siblingInfoMap, MAGEKEY_PRODUCT_PRICE));
                siblingInfo.setQuantity("" + siblingInfoMap.get(MAGEKEY_PRODUCT_QUANTITY));
                for (String key : configurableAttributes)
                {
                    siblingInfo
                            .addConfigurableAttributeValue(key, (String) siblingInfoMap.get(key));
                }
                siblingsList.add(siblingInfo);
            }
        }
    }

    /**
     * Get the value label for the attribute value
     * 
     * @param attribValue
     * @param options_objects
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String getValueLabel(String attribValue,
            Object[] options_objects) {
        String result;
        if (options_objects.length > 0 && attribValue.length() > 0) {
            String[] list = attribValue.split(",");
            StringBuilder sb = new StringBuilder();

            for (int l = 0; l < list.length; l++) {
                for (int k = 0; k < options_objects.length; k++) {
                    Map<String, Object> options = (Map<String, Object>) options_objects[k];
                    if (TextUtils.equals(options.get("value").toString(), list[l])) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }

                        sb.append(options.get("label").toString());
                        break;
                    }
                }
            }

            result = sb.toString();
        } else {
            // If there is no Options -- then value
            // label = value
            result = attribValue;
        }
        return result;
    }

    public Product() {
        this.id = String.valueOf(INVALID_PRODUCT_ID);
    }

    /**********************************************************************************************************/

}
