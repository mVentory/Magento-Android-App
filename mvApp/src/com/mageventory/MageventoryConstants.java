
package com.mageventory;

import com.mageventory.res.ResourceConstants;

public interface MageventoryConstants extends ResourceConstants {

    // TODO:
    public static final String TODO_HARDCODED_PRODUCT_WEBSITE = "1";
    public static final int TODO_HARDCODED_DEFAULT_ATTRIBUTE_SET = 4;

    public static final boolean DEBUG = true;

    public static final String TM_SANDBOX_URL = "http://www.tmsandbox.co.nz/Browse/Listing.aspx?id=";
    public static final String TRADEME_URL = "http://www.trademe.co.nz/Browse/Listing.aspx?id=";

    // Magento path constants
    public static final String XMLRPC_PATH = "index.php/api/xmlrpc/";

    /*
     * Helper keys that can be used, for example, to convey some info in a Job
     * object from UI to the lower layers but the values of which are never send
     * to the server. They should always be deleted from the request data before
     * sending it.
     */

    /*
     * There are two modes of product creation: quick sell mode and normal mode.
     * This key is set in UI to inform the lower layers about which mode was
     * selected (The value associated with this key will be set to "true" in
     * case of quick sell mode and "false" otherwise).
     */
    public static final String EKEY_QUICKSELLMODE = "ekey_quicksellmode";

    /* A product can be created in duplication mode or in normal mode. */
    public static final String EKEY_DUPLICATIONMODE = "ekey_duplicationmode";
    public static final String EKEY_PRODUCT_SKU_TO_DUPLICATE = "ekey_product_sku_to_duplicate";
    public static final String EKEY_DUPLICATION_PHOTO_COPY_MODE = "ekey_duplication_photo_copy_mode";
    public static final String EKEY_DECREASE_ORIGINAL_QTY = "ekey_decrease_original_qty";

    /*
     * Used in case of product edit jobs to store information about which
     * attributes exactly were changed by the user. The value associated with
     * this key should be a list of Strings corresponding to the keys of modifed
     * attributes.
     */
    public static final String EKEY_UPDATED_KEYS_LIST = "ekey_updated_keys_list";

    /* Used for passing the array of products to sell to the job processor */
    public static final String EKEY_PRODUCTS_TO_SELL_ARRAY = "ekey_products_to_sell_array";
    public static final String EKEY_PRODUCT_SKUS_TO_SELL_ARRAY = "ekey_product_skus_to_sell_array";

    public static final String EKEY_PRODUCT_ATTRIBUTE_SET_ID = "ekey_product_attribute_set_id";
    public static final String EKEY_PRODUCT_ATTRIBUTE_VALUES = "ekey_product_attribute_values";

    // Order shipment constants
    public static final String EKEY_SHIPMENT_ORDER_INCREMENT_ID = "ekey_shipment_order_increment_id";
    public static final String EKEY_SHIPMENT_CARRIER_CODE = "ekey_shipment_carrier_code";
    public static final String EKEY_SHIPMENT_TITLE = "ekey_shipment_title";
    public static final String EKEY_SHIPMENT_TRACKING_NUMBER = "ekey_shipment_tracking_number";
    public static final String EKEY_SHIPMENT_WITH_TRACKING_PARAMS = "ekey_shipment_with_tracking_params";
    public static final String EKEY_SHIPMENT_ITEMS_QTY = "itemsQty";
    public static final String EKEY_SHIPMENT_COMMENT = "comment";
    public static final String EKEY_SHIPMENT_INCLUDE_COMMENT = "includeComment";

    public static final String EKEY_DONT_REPORT_PRODUCT_NOT_EXIST_EXCEPTION = "ekey_dont_report_product_not_exist_exception";

    // Magento RPC web service constants
    public static final String MAGEKEY_CATEGORY_ID = "category_id";
    public static final String MAGEKEY_CATEGORY_NAME = "name";
    public static final String MAGEKEY_CATEGORY_CHILDREN = "children";

    public static final String MAGEKEY_PRODUCT_ID = "product_id";
    public static final String MAGEKEY_PRODUCT_NAME = "name";
    public static final String MAGEKEY_PRODUCT_NAME2 = "product_name";
    public static final String MAGEKEY_PRODUCT_SKU = "sku";
    public static final String MAGEKEY_PRODUCT_ADDITIONAL_SKUS = "additional_sku";
    public static final String MAGEKEY_PRODUCT_PRICE = "price";
    public static final String MAGEKEY_PRODUCT_SPECIAL_PRICE = "special_price";
    public static final String MAGEKEY_PRODUCT_SPECIAL_FROM_DATE = "special_from_date";
    public static final String MAGEKEY_PRODUCT_SPECIAL_TO_DATE = "special_to_date";
    public static final String MAGEKEY_PRODUCT_TOTAL = "total";
    public static final String MAGEKEY_PRODUCT_DATE_TIME = "date_time";
    public static final String MAGEKEY_PRODUCT_TRANSACTION_ID = "transaction_id";
    public static final String MAGEKEY_PRODUCT_WEBSITE = "website";
    public static final String MAGEKEY_PRODUCT_DESCRIPTION = "description";
    public static final String MAGEKEY_PRODUCT_SHORT_DESCRIPTION = "short_description";
    public static final String MAGEKEY_PRODUCT_STATUS = "status";
    public static final String MAGEKEY_PRODUCT_WEIGHT = "weight";
    public static final String MAGEKEY_PRODUCT_CATEGORIES = "categories";
    public static final String MAGEKEY_PRODUCT_VISIBILITY = "visibility";
    public static final String MAGEKEY_PRODUCT_TIER_PRICE = "tire_price";
    public static final String MAGEKEY_PRODUCT_URL_PATH = "url_path";
    public static final String MAGEKEY_PRODUCT_ATTRIBUTE_SET_ID = "set";
    public static final String MAGEKEY_PRODUCT_TYPE = "type";
    public static final String MAGEKEY_PRODUCT_ATTRIBUTES = "set_attributes";
    public static final String MAGEKEY_PRODUCT_REQUIRED_OPTIONS = "required_options";
    public static final String MAGEKEY_PRODUCT_OPTIONS_CONTAINER = "options_container";
    public static final String MAGEKEY_PRODUCT_TAX_CLASS_ID = "tax_class_id";
    public static final String MAGEKEY_PRODUCT_URL_KEY = "url_key";
    public static final String MAGEKEY_PRODUCT_ENABLE_GOOGLE_CHECKOUT = "enable_googlecheckout";
    public static final String MAGEKEY_PRODUCT_HAS_OPTIONS = "has_options";
    public static final String MAGEKEY_PRODUCT_TYPE_ID = "type_id";
    public static final String MAGEKEY_PRODUCT_CATEGORY_IDS = "category_ids";
    public static final String MAGEKEY_PRODUCT_IMAGES = "images";
    public static final String MAGEKEY_PRODUCT_QUANTITY = "qty";
    public static final String MAGEKEY_PRODUCT_IS_IN_STOCK = "is_in_stock";
    public static final String MAGEKEY_PRODUCT_MANAGE_INVENTORY = "manage_stock";
    public static final String MAGEKEY_PRODUCT_IS_QTY_DECIMAL = "is_qty_decimal";
    public static final String MAGEKEY_PRODUCT_STOCK_DATA = "stock_data";
    public static final String MAGEKEY_PRODUCT_USE_CONFIG_MANAGE_STOCK = "use_config_manage_stock";
    public static final String MAGEKEY_PRODUCT_TM_OPTIONS = "tm_options";
    public static final String MAGEKEY_PRODUCT_LISTING_ID = "tm_listing_id";
    public static final String MAGEKEY_PRODUCT_ALLOW_BUY_NOW = "allow_buy_now";
    public static final String MAGEKEY_PRODUCT_ADD_TM_FEES = "add_tm_fees";
    public static final String MAGEKEY_PRODUCT_RELIST = "relist";
    public static final String MAGEKEY_PRODUCT_SHIPPING_TYPE_ID = "shipping_type";
    public static final String MAGEKEY_PRODUCT_PRESELECTED_CATEGORIES = "preselected_categories";
    public static final String MAGEKEY_PRODUCT_TM_ACCOUNTS = "tm_accounts";
    public static final String MAGEKEY_PRODUCT_SHIPPING_TYPES_LIST = "shipping_types_list";
    public static final String MAGEKEY_PRODUCT_TM_CATEGORY_ID = "tm_category_id";
    public static final String MAGEKEY_PRODUCT_TM_ACCOUNT_ID = "account_id";
    public static final String MAGEKEY_PRODUCT_TM_ERROR = "tm_error";
    public static final String MAGEKEY_PRODUCT_STORE_ID = "store_id";
    public static final String MAGEKEY_TM_CATEGORY_MATCH_ID = "tm_match_id";
    public static final String MAGEKEY_TM_CATEGORY_MATCH_NAME = "tm_match_name";

    public static final String MAGEKEY_ATTRIBUTE_SET_NAME = "name";
    public static final String MAGEKEY_ATTRIBUTE_SET_ID = "set_id";
    public static final String MAGEKEY_ATTRIBUTE_ID = "attribute_id";
    public static final String MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE = "attribute_code";
    public static final String MAGEKEY_ATTRIBUTE_TYPE = "frontend_input";
    public static final String MAGEKEY_ATTRIBUTE_REQUIRED = "is_required";
    public static final String MAGEKEY_ATTRIBUTE_OPTIONS = "options";
    public static final String MAGEKEY_ATTRIBUTE_DEFAULT_VALUE = "default_value";
    public static final String MAGEKEY_ATTRIBUTE_IS_FORMATTING_ATTRIBUTE = "is_formatting_attribute";
    public static final String MAGEKEY_ATTRIBUTE_CONFIGURABLE = "is_configurable";

    public static final String MAGEKEY_ATTRIBUTE_OPTIONS_LABEL = "label";
    public static final String MAGEKEY_ATTRIBUTE_OPTIONS_VALUE = "value";

    public static final int INVALID_PRODUCT_ID = -1;
    public static final int INVALID_CATEGORY_ID = -1;
    public static final int INVALID_ATTRIBUTE_SET_ID = -1;

    public static final String NEW_QUANTITY = "newQTY";
    public static final String UPDATE_PRODUCT_QUANTITY = "UpdateQty";

    /* CONSTANTS FOR CART - ORDER */
    /* CUSTOMER INFORMATION */
    public static final String MAGEKEY_CUSTOMER_INFO_ID = "customer_id";
    public static final String MAGEKEY_CUSTOMER_INFO_FIRST_NAME = "firstname";
    public static final String MAGEKEY_CUSTOMER_INFO_LAST_NAME = "lastname";
    public static final String MAGEKEY_CUSTOMER_INFO_EMAIL = "email";
    public static final String MAGEKEY_CUSTOMER_INFO_WEBSITE_ID = "website_id";
    public static final String MAGEKEY_CUSTOMER_INFO_STORE_ID = "store_id";
    public static final String MAGEKEY_CUSTOMER_INFO_MODE = "mode";

    /* CUSTOMER ADDRESS */
    public static final String MAGEKEY_CUSTOMER_ADDRESS_MODE = "Address_Mode";
    public static final String MAGEKEY_CUSTOMER_ADDRESS_COMPANY = "company";
    public static final String MAGEKEY_CUSTOMER_ADDRESS_STREET = "street";
    public static final String MAGEKEY_CUSTOMER_ADDRESS_CITY = "city";
    public static final String MAGEKEY_CUSTOMER_ADDRESS_REGION = "region";
    public static final String MAGEKEY_CUSTOMER_ADDRESS_POSTCODE = "postcode";
    public static final String MAGEKEY_CUSTOMER_ADDRESS_COUNTRY_ID = "country_id";
    public static final String MAGEKEY_CUSTOMER_ADDRESS_TELEPHONE = "telephone";
    public static final String MAGEKEY_CUSTOMER_ADDRESS_FAX = "fax";
    public static final String MAGEKEY_CUSTOMER_ADDRESS_IS_DEFAULT_SHIPPING = "is_default_shipping";
    public static final String MAGEKEY_CUSTOMER_ADDRESS_IS_DEFAULT_BILLING = "is_default_billing";

    /* PRODUCT IMAGE INFORMATION */
    public static final String MAGEKEY_PRODUCT_IMAGE_NAME = "name";
    public static final String MAGEKEY_PRODUCT_IMAGE_CONTENT = "content";
    public static final String MAGEKEY_PRODUCT_IMAGE_MIME = "mime";
    public static final String MAGEKEY_PRODUCT_IMAGE_POSITION = "position";

    // task states
    public static final int TSTATE_NEW = 1;
    public static final int TSTATE_RUNNING = 2;
    public static final int TSTATE_TERMINATED = 3;
    public static final int TSTATE_CANCELED = 4;

    // resource
    public static final int RES_CATALOG_PRODUCT_CREATE = 0;
    public static final int RES_CATALOG_PRODUCT_UPDATE = 1;
    public static final int RES_CATALOG_PRODUCT_SELL = 2;
    public static final int RES_UPLOAD_IMAGE = 3;
    public static final int RES_DELETE_IMAGE = 4;
    public static final int RES_MARK_IMAGE_MAIN = 5;
    public static final int RES_CATALOG_PRODUCT_LIST = 6;
    public static final int RES_PRODUCT_DETAILS = 7;
    public static final int RES_CATALOG_CATEGORY_TREE = 8;
    public static final int RES_CATALOG_PRODUCT_ATTRIBUTES = 9;
    public static final int RES_PRODUCT_DELETE = 10;
    public static final int RES_PRODUCT_ATTRIBUTE_ADD_NEW_OPTION = 11;
    public static final int RES_ORDERS_LIST_BY_STATUS = 12;
    public static final int RES_ORDER_DETAILS = 13;
    public static final int RES_CATALOG_PRODUCT_STATISTICS = 14;
    public static final int RES_CATALOG_PRODUCT_SUBMIT_TO_TM = 15;
    public static final int RES_ORDER_SHIPMENT_CREATE = 16;
    public static final int RES_GET_PROFILES_LIST = 17;
    public static final int RES_EXECUTE_PROFILE = 18;
    public static final int RES_ADD_PRODUCT_TO_CART = 19;
    public static final int RES_CART_ITEMS = 20;
    public static final int RES_SELL_MULTIPLE_PRODUCTS = 21;

    // startActivityForResult request codes
    public static final int REQ_EDIT_PRODUCT = 1;

    public static final int RESULT_CHANGE = 1;
    public static final int RESULT_NO_CHANGE = 2;
    public static final int RESULT_SUCCESS = 3;
    public static final int RESULT_FAILURE = 4;

    public static final int SCAN_QR_CODE = 0;
    public static final int SCAN_BARCODE = 1;
    public static final int LAUNCH_GESTURE_INPUT = 2;
    public static final int SCAN_ADDITIONAL_SKUS = 3;

    public static final String SCAN_DONE = "scanDone";

    public static final String GET_PRODUCT_BY_ID = "0";
    public static final String GET_PRODUCT_BY_SKU = "1";
    public static final String GET_PRODUCT_BY_SKU_OR_BARCODE = "2";

    public static final String PASSING_SKU = "passingSKU";

}
