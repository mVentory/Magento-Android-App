package com.mageventory.resprocessor;

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
		ResourceServiceHelper.getInstance().markResourceAsOld(context,
				RES_CATALOG_PRODUCT_LIST);
	}

	public void productUpdated(final Context context, final int productId) {
		ResourceServiceHelper.getInstance().markResourceAsOld(context,
				RES_CATALOG_PRODUCT_LIST);

		// TODO y: there is somekind of a problem when marking as old with
		// params...
		ResourceServiceHelper.getInstance().markResourceAsOld(context,
				RES_PRODUCT_DETAILS);
		// new String[] { "" + productId });
	}

	// y: mark product details as outdated as required in comment #12 for issue
	// #35
	public void productListChanged(final Context context) {
		ResourceServiceHelper.getInstance().markResourceAsOld(context,
				RES_PRODUCT_DETAILS);
	}

	public void configChanged(final Context context) {
	}
}
