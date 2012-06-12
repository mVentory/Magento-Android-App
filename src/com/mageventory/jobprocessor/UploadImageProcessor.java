package com.mageventory.jobprocessor;

import java.util.Map;
import android.content.Context;
import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.ImageStreaming;
import com.mageventory.client.MagentoClient2;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.jobprocessor.JobProcessorManager.IProcessor;
import com.mageventory.model.Product;

public class UploadImageProcessor implements IProcessor, MageventoryConstants {

	ImageStreaming.StreamUploadCallback mCallback;

	public void setCallback(ImageStreaming.StreamUploadCallback callback) {
		mCallback = callback;
	}

	@Override
	public void process(Context context, Job job) {
		Map<String, Object> imageData = job.getExtras();
		boolean is_main = ((Boolean) job
				.getExtraInfo(MAGEKEY_PRODUCT_IMAGE_IS_MAIN)).booleanValue();

		final MagentoClient2 client = ((MyApplication) context
				.getApplicationContext()).getClient2();

		Map<String, Object> productMap = client.uploadImage(imageData, ""
				+ job.getJobID().getProductID(), is_main, mCallback);

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
	}
}
