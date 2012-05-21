package com.mageventory.resprocessor;

import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.res.ResourceProcessorManager.IProcessor;

public abstract class AbsProductProcessor implements MageventoryConstants {

	 public static class IncompleteDataException extends RuntimeException {

		/**
         * 
         */
		private static final long serialVersionUID = 1L;

		public IncompleteDataException() {
			super();
		}

		public IncompleteDataException(String detailMessage) {
			super(detailMessage);
		}

	}
	 
	 /**
	  * Extract Update Information 
	  * Quantity and IS_IN_MARKET
	  */
	protected Map<String, Object> extractUpdate(Bundle bundle) throws IncompleteDataException {
		final String[] stringKeys = {
				MAGEKEY_PRODUCT_QUANTITY,
				MAGEKEY_PRODUCT_MANAGE_INVENTORY,
				MAGEKEY_PRODUCT_IS_IN_STOCK
		};
		// @formatter:on
		final Map<String, Object> productData = new HashMap<String, Object>();
		for (final String stringKey : stringKeys) {
			productData.put(stringKey, extractString(bundle, stringKey, true));
		}       
		return productData;
	}

	protected Map<String, Object> extractData(Bundle bundle, boolean exceptionOnFail) throws IncompleteDataException {
		// TODO y: which fields are mandatory?
		// @formatter:off
        final String[] stringKeys = {
                MAGEKEY_PRODUCT_NAME,
                MAGEKEY_PRODUCT_PRICE,
                MAGEKEY_PRODUCT_WEBSITE,
                MAGEKEY_PRODUCT_DESCRIPTION,
                MAGEKEY_PRODUCT_SHORT_DESCRIPTION,
                MAGEKEY_PRODUCT_STATUS,
                MAGEKEY_PRODUCT_WEIGHT,
        };
        // @formatter:on
		final Map<String, Object> productData = new HashMap<String, Object>();
		for (final String stringKey : stringKeys) {
			productData.put(stringKey, extractString(bundle, stringKey, exceptionOnFail));
		}
		final Object cat = bundle.get(MAGEKEY_PRODUCT_CATEGORIES);
		if (cat == null || cat instanceof Object[] == false) {
			throw new IncompleteDataException("bad category");
		}
		productData.put(MAGEKEY_PRODUCT_CATEGORIES, cat);
		return productData;
	}

	protected static String extractString(final Bundle bundle, final String key, final boolean exceptionOnFail) throws IncompleteDataException {
		final String s = bundle.getString(key);
		if (s == null && exceptionOnFail) {
			throw new IncompleteDataException("bad data for key '" + key + "'");
		}
		return s == null ? "" : s;
	}

}
