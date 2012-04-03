package com.mageventory;

import com.mageventory.res.ResourceConstants;


public interface MageventoryConstants extends ResourceConstants {

	public static final boolean DEBUG = true;

	// Magento path constants
	public static final String XMLRPC_PATH = "index.php/api/xmlrpc/";
	
	// common extra keys
	public static final String EKEY_OP_REQUEST_ID = "op_request_id";
	
	// Magento RPC web service constants
	public static final String MAGEKEY_CATEGORY_ID = "category_id";
	public static final String MAGEKEY_CATEGORY_NAME = "name";
	
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
	
	public static final int INVALID_PRODUCT_ID = -1;
	public static final int INVALID_CATEGORY_ID = -1;
	
	// resource
	public static final int RES_CATALOG_PRODUCT_LIST = 1;
	public static final int RES_PRODUCT_DETAILS = 4;
	public static final int RES_CATALOG_CATEGORY_TREE = 5;
	public static final int RES_CATALOG_PRODUCT_CREATE = 6;
	
	// example for the resource loading framework
	public static final int RES_EXAMPLE_FEED = 2;
	public static final int RES_EXAMPLE_IMAGE = 3;

}
