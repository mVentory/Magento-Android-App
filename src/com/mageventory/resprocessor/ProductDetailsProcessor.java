package com.mageventory.resprocessor;

import java.util.Map;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient2;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.xmlrpc.XMLRPCException;
import com.mageventory.xmlrpc.XMLRPCFault;

public class ProductDetailsProcessor implements IProcessor, MageventoryConstants {

	/* An exception class used to pass error code to UI in case something goes wrong. */
	public static class ProductDetailsLoadException extends RuntimeException
	{
		private static final long serialVersionUID = -6182408798009199205L;

		public static final int ERROR_CODE_PRODUCT_DOESNT_EXIST = 101;
		
		private int faultCode;
		
		public void setFaultCode(int fc)
		{
			faultCode = fc;
		}
		
		public int getFaultCode()
		{
			return faultCode;
		}
		
		public ProductDetailsLoadException(String detailMessage, int faultCode)
		{
			super(detailMessage);
			this.faultCode = faultCode;
		}
	}
	
	@Override
	public Bundle process(Context context, String[] params, Bundle extras) {
		MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();

		String useIDorSKU = params[0];

		if (useIDorSKU.compareToIgnoreCase(GET_PRODUCT_BY_ID) == 0) {
			int productId = Integer.parseInt(params[1]);

			// retrieve product
			final Map<String, Object> productMap = client.catalogProductInfo(productId);

			final Product product;
			if (productMap != null) {
				product = new Product(productMap);
			} else {
				throw new RuntimeException(client.getLastErrorMessage());
			}

			// cache
			if (product != null) {
				JobCacheManager.storeProductDetailsWithMerge(product);
			}
		} else if (useIDorSKU.compareToIgnoreCase(GET_PRODUCT_BY_SKU) == 0) {
			String productSKU = params[1];

			// retrieve product
			final Map<String, Object> productMap = client.catalogProductInfoBySKU(productSKU);

			final Product product;
			if (productMap != null) {
				product = new Product(productMap);
			} else {
				throw new ProductDetailsLoadException(client.getLastErrorMessage(), client.getLastErrorCode());
			}

			// cache
			if (product != null) {
				JobCacheManager.storeProductDetailsWithMerge(product);
			}
		}

		return null;
	}

}
