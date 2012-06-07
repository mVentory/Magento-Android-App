package com.mageventory.jobprocessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import com.mageventory.util.Log;

import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.client.MagentoClient2;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceCache;
import com.mageventory.res.ResourceStateDao;
import com.mageventory.resprocessor.AbsProductProcessor;
import com.mageventory.resprocessor.ResourceExpirationRegistry;
import com.mageventory.resprocessor.AbsProductProcessor.IncompleteDataException;
import com.mageventory.jobprocessor.JobProcessorManager.IProcessor;

public class CreateProductProcessor extends AbsProductProcessor implements
		IProcessor {

	// @formatter:off
	private static final char CHARS[] = {
			// 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
			// 'N', 'O', 'P',
			// 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
			'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	// '+', '/',
	};
	private static Random random;

	private static final String TAG = "CreateProductProcessor";

	// @formatter:on

	public static String generateSku(final Map<String, Object> data, boolean alt) {
		String name = data.get(MAGEKEY_PRODUCT_NAME).toString();
		if (name == null) {
			Log.v(TAG, "product name is null");
			name = "";
		}
		final StringBuilder sku = new StringBuilder();
		while (name.length() < 3) {
			name += randomChar();
		}
		System.out.println("name=" + name);
		System.out.println("base64name="
				+ new String(Base64.encode(name.getBytes(), Base64.NO_PADDING
						| Base64.NO_WRAP)));
		name = name.substring(name.length() - 3);
		name = new String(Base64.encode(name.getBytes(), Base64.NO_PADDING
				| Base64.NO_WRAP));
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

	@Override
	public void process(Context context, Job job) {
		Map<String, Object> requestData = job.getExtras();

		// extract attribute data
		final int attrSet = ((Integer) requestData
				.get(EKEY_PRODUCT_ATTRIBUTE_SET_ID)).intValue();

		/*
		 * Don't need this in extras as we are passing it in a separate argument
		 * later.
		 */
		requestData.remove(EKEY_PRODUCT_ATTRIBUTE_SET_ID);

		if (attrSet == INVALID_ATTRIBUTE_SET_ID) {
			Log.w(TAG, "INVALID ATTRIBUTE SET ID");
			return;
		}

		final MagentoClient2 client = ((MyApplication) context
				.getApplicationContext()).getClient2();

		String sku = (String) requestData.get(MAGEKEY_PRODUCT_SKU);
		Map<String, Object> productMap = client.catalogProductCreate("simple",
				attrSet, sku, requestData);

		int pid = -1;

		Product product = null;

		if (productMap != null) {
			product = new Product(productMap, true);
			pid = Integer.parseInt(product.getId());
		}

		if (pid == -1) {
			// issue #49 (
			// http://code.google.com/p/mageventory/issues/detail?id=49 )
			// says we should regenerate SKU and retry if it fails the first
			// time
			sku = generateSku(requestData, true);
			productMap = client.catalogProductCreate("simple", attrSet, sku,
					requestData);
			if (productMap != null) {
				product = new Product(productMap, true);
				pid = Integer.parseInt(product.getId());
			}
		}

		if (pid == -1) {
			throw new RuntimeException(client.getLastErrorMessage());
		} else {
			JobCacheManager.storeProductDetails(product);
		}
	}

}
