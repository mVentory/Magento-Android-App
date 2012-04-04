package com.mageventory.processor;

import android.content.Context;

import com.mageventory.MageventoryConstants;
import com.mageventory.res.ResourceServiceHelper;

public class ResourceExpirationRegistry implements MageventoryConstants {

	private static ResourceExpirationRegistry instance;

	public static ResourceExpirationRegistry getInstance() {
		if (instance == null) {
			instance = new ResourceExpirationRegistry();
		}
		return instance;
	}

	public void categoriesChanged(final Context context) {

	}

	/**
	 * Mark product list as outdated.
	 * 
	 * @param context
	 */
	public void productCreated(final Context context) {
		ResourceServiceHelper.getInstance().markResourceAsOld(context, RES_CATALOG_PRODUCT_LIST);
	}

	/**
	 * Call this method each time there is a change in the product data that invalidates what's currently cached. For
	 * example such a change could be a product creation, product deletion, editing a product, etc.
	 * 
	 * @param context
	 */
	public void productsChanged(final Context context) {
		ResourceServiceHelper.getInstance().markResourceAsOld(context, RES_CATALOG_PRODUCT_LIST);
		ResourceServiceHelper.getInstance().markResourceAsOld(context, RES_PRODUCT_DETAILS);
	}

	public void everythingChanged(final Context context) {
		// @formatter:off
    	for (final int resType : new int[] {
    			RES_CATALOG_CATEGORY_TREE,
    			RES_CATALOG_PRODUCT_LIST,
    			RES_PRODUCT_DETAILS }) {
    		ResourceServiceHelper.getInstance().markResourceAsOld(context,
    				resType);
    	}
    	// @formatter:on
	}

}
