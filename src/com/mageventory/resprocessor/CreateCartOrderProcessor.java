package com.mageventory.resprocessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import com.mageventory.util.Log;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient2;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceProcessorManager.IProcessor;

/**
 * Class Implements the Order Invoice
 * 
 * @author hussein
 * 
 */

public class CreateCartOrderProcessor implements IProcessor, MageventoryConstants {

	private static class IncompleteDataException extends RuntimeException {

		/**
         * 
         */
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused")
		public IncompleteDataException() {
			super();
		}

		public IncompleteDataException(String detailMessage) {
			super(detailMessage);
		}

	}

	private static final String TAG = "CreateCartOrderProcessor";

	// @formatter:off
	private static final char CHARS[] = {
			// 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
			// 'N', 'O', 'P',
			// 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
			'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	// '+', '/',
	};
	private static Random random;

	// @formatter:on

	public static String generateSku(final Map<String, Object> data, boolean alt) {

		String name = "";
		if (!data.containsKey(MAGEKEY_PRODUCT_NAME)) {
			name = "";
		} else {
			name = data.get(MAGEKEY_PRODUCT_NAME).toString();
			if (name == null) {
				Log.v(TAG, "product name is null");
				name = "";
			}
		}

		final StringBuilder sku = new StringBuilder();
		while (name.length() < 3) {
			name += randomChar();
		}
		System.out.println("name=" + name);
		System.out.println("base64name="
				+ new String(Base64.encode(name.getBytes(), Base64.NO_PADDING | Base64.NO_WRAP)));
		name = name.substring(name.length() - 3);
		name = new String(Base64.encode(name.getBytes(), Base64.NO_PADDING | Base64.NO_WRAP));
		name = name.toLowerCase();
		sku.append(System.currentTimeMillis());

		if (alt) {
			// insert 3 random characters to make it unique
			for (int i = 0; i < 3; i++) {
				sku.insert(getRandom().nextInt(sku.length()), randomChar());
			}
		}

		sku.append(name);
		return sku.toString();
	}

	private static Random getRandom() {
		if (random == null) {
			random = new Random();
		}
		return random;
	}

	private static char randomChar() {
		return CHARS[getRandom().nextInt(CHARS.length)];
	}

	/**
	 * Extract Product Details
	 * 
	 * @param bundle
	 * @return
	 * @throws IncompleteDataException
	 */
	public static Map<String, Object> extractProductDetails(Bundle bundle) throws IncompleteDataException {
		// Implements Order Invoice Information
		final String[] stringKeys = { MAGEKEY_PRODUCT_SKU, MAGEKEY_PRODUCT_QUANTITY, MAGEKEY_PRODUCT_PRICE,
				MAGEKEY_PRODUCT_NAME };

		final Map<String, Object> productData = new HashMap<String, Object>();
		for (final String stringKey : stringKeys) {
			productData.put(stringKey, extractString(bundle, stringKey));
		}
		return productData;
	}

	public static String extractString(final Bundle bundle, final String key) throws IncompleteDataException {
		final String s = bundle.getString(key);
		if (s == null) {
			throw new IncompleteDataException("bad data for key '" + key + "'");
		}
		return s;
	}

	/**
	 * Process extras has all information of order
	 */
	@Override
	public Bundle process(Context context, String[] params, Bundle extras) {

		MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();

		Map<String, Object> productData = extractProductDetails(extras);

		final Map<String, Object> productMap = client.orderCreate(productData);

		final Product product;
		if (productMap != null) {
			product = new Product(productMap, true);
		} else {
			throw new RuntimeException(client.getLastErrorMessage());
		}

		// cache
		if (product != null) {
			JobCacheManager.storeProductDetails(product);
		}

		return null;
	}
}
