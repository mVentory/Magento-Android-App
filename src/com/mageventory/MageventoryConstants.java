package com.mageventory;

import com.mageventory.res.ResourceConstants;


public interface MageventoryConstants extends ResourceConstants {

	public static final boolean DEBUG = true;

	// Magento path constants
	public static final String XMLRPC_PATH = "index.php/api/xmlrpc/";
	
	// common extra keys
	public static final String EKEY_OP_REQUEST_ID = "op_request_id";
	public static final String EKEY_PRODUCT_ID = "product_id";
	
	// Magento RPC web service constants
	public static final String MAGEKEY_CATEGORY_ID = "category_id";
	public static final String MAGEKEY_CATEGORY_NAME = "name";
	public static final String MAGEKEY_CATEGORY_CHILDREN = "children";
	
	public static final String MAGEKEY_PRODUCT_ID = "product_id";
	public static final String MAGEKEY_PRODUCT_NAME = "name";
	public static final String MAGEKEY_PRODUCT_SKU = "sku";
	public static final String MAGEKEY_PRODUCT_PRICE = "price";
	public static final String MAGEKEY_PRODUCT_WEBSITE = "website";
	public static final String MAGEKEY_PRODUCT_DESCRIPTION = "description";
	public static final String MAGEKEY_PRODUCT_SHORT_DESCRIPTION = "short_description";
	public static final String MAGEKEY_PRODUCT_STATUS = "status";
	public static final String MAGEKEY_PRODUCT_WEIGHT = "weight";
	public static final String MAGEKEY_PRODUCT_CATEGORIES = "categories";

	public static final String MAGEKEY_PRODUCT_QUANTITY = "qty";
	public static final String MAGEKEY_PRODUCT_IS_IN_STOCK = "is_in_stock";
	public static final String MAGEKEY_PRODUCT_MANAGE_INVENTORY = "manage_stock";	
		
	public static final int INVALID_PRODUCT_ID = -1;
	public static final int INVALID_CATEGORY_ID = -1;
	
	public static final String NEW_QUANTITY = "newQTY";
	
	/* CONSTANTS FOR CART - ORDER */
	/* CUSTOMER INFORMATION */
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
	
	
	
	// resource
	public static final int RES_CATALOG_PRODUCT_LIST = 1;
	public static final int RES_PRODUCT_DETAILS = 4;
	public static final int RES_CATALOG_CATEGORY_TREE = 5;
	public static final int RES_CATALOG_PRODUCT_CREATE = 6;
	public static final int RES_CATALOG_PRODUCT_UPDATE = 7;
	public static final int RES_CART_ORDER_CREATE = 8;
	
	// example for the resource loading framework
	public static final int RES_EXAMPLE_FEED = 2;
	public static final int RES_EXAMPLE_IMAGE = 3;
	
	// startActivityForResult request codes
	public static final int REQ_EDIT_PRODUCT = 1;
	
	public static final int RESULT_CHANGE = 1;
	public static final int RESULT_NO_CHANGE = 2;

}
