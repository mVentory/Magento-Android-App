package com.mageventory.resprocessor;

import java.net.MalformedURLException;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.mageventory.MageventoryConstants;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.settings.SettingsSnapshot;

public class ProductDetailsProcessor implements IProcessor, MageventoryConstants {

	/* An exception class used to pass error code to UI in case something goes wrong. */
	public static class ProductDetailsLoadException extends RuntimeException
            implements Parcelable
	{
		private static final long serialVersionUID = -6182408798009199205L;

		public static final int ERROR_CODE_PRODUCT_DOESNT_EXIST = 101;
        public static final int ERROR_CODE_ACCESS_DENIED = 2;
        public static final int ERROR_CODE_UNDEFINED = 0;
		
		private int faultCode;
		public boolean mDontReportProductNotExistsException;
		
		public void setFaultCode(int fc)
		{
			faultCode = fc;
		}
		
		public int getFaultCode()
		{
			return faultCode;
		}
		
		public ProductDetailsLoadException(String detailMessage, int faultCode, boolean dontReportProductNotExistsException)
		{
			super(detailMessage);
			this.faultCode = faultCode;
			mDontReportProductNotExistsException = dontReportProductNotExistsException;
		}

        /*****************************
         * PARCELABLE IMPLEMENTATION *
         *****************************/
        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(getMessage());
            out.writeInt(faultCode);
            out.writeByte((byte) (mDontReportProductNotExistsException ? 1 : 0));
        }

        public static final Parcelable.Creator<ProductDetailsLoadException> CREATOR = new Parcelable.Creator<ProductDetailsLoadException>() {
            @Override
            public ProductDetailsLoadException createFromParcel(Parcel in) {
                return new ProductDetailsLoadException(in);
            }

            @Override
            public ProductDetailsLoadException[] newArray(int size) {
                return new ProductDetailsLoadException[size];
            }
        };

        private ProductDetailsLoadException(Parcel in) {
            super(in.readString());
            faultCode = in.readInt();
            mDontReportProductNotExistsException = in.readByte() == 1;
        }
	}
	
	@Override
	public Bundle process(Context context, String[] params, Bundle extras) {
		SettingsSnapshot ss = (SettingsSnapshot)extras.get(EKEY_SETTINGS_SNAPSHOT);
		boolean dontReportProductNotExistsException = extras.getBoolean(EKEY_DONT_REPORT_PRODUCT_NOT_EXIST_EXCEPTION, false);
		
		MagentoClient client;
		try {
			client = new MagentoClient(ss);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e.getMessage());
		}
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
				JobCacheManager.storeProductDetailsWithMergeAsynchronous(product, ss.getUrl());
			}
		} else if (useIDorSKU.compareToIgnoreCase(GET_PRODUCT_BY_SKU) == 0 ||
				useIDorSKU.compareToIgnoreCase(GET_PRODUCT_BY_SKU_OR_BARCODE) == 0) {
			String productSKU = params[1];

			// retrieve product
			final Map<String, Object> productMap = client.catalogProductInfoBySKU(productSKU,
				useIDorSKU.compareToIgnoreCase(GET_PRODUCT_BY_SKU_OR_BARCODE) == 0);

			final Product product;
			if (productMap != null) {
				product = new Product(productMap);
			} else {
				throw new ProductDetailsLoadException(client.getLastErrorMessage(), client.getLastErrorCode(), dontReportProductNotExistsException);
			}

			// cache
			if (product != null) {
				JobCacheManager.storeProductDetailsWithMergeAsynchronous(product, ss.getUrl());

				Bundle retBundle = new Bundle();
				retBundle.putString(MAGEKEY_PRODUCT_SKU, product.getSku());
				
				return retBundle;
			}
		}

		return null;
	}

}
