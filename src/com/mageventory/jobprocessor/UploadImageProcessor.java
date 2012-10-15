package com.mageventory.jobprocessor;

import java.net.MalformedURLException;
import java.util.Map;
import android.content.Context;
import android.util.Log;

import com.mageventory.MageventoryConstants;
import com.mageventory.client.ImageStreaming;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.jobprocessor.JobProcessorManager.IProcessor;
import com.mageventory.model.Product;

public class UploadImageProcessor implements IProcessor, MageventoryConstants {

	ImageStreaming.StreamUploadCallback mCallback;
	private static String TAG = "UploadImageProcessor";

	public void setCallback(ImageStreaming.StreamUploadCallback callback) {
		mCallback = callback;
	}

	@Override
	public void process(Context context, Job job) {
		Map<String, Object> imageData = job.getExtras();

		MagentoClient client;
		try {
			client = new MagentoClient(job.getSettingsSnapshot());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e.getMessage());
		}
		
		Log.d(TAG, "Uploading image: " + imageData.get(MAGEKEY_PRODUCT_IMAGE_CONTENT));
		Map<String, Object> productMap = client.uploadImage(imageData, "" + job.getJobID().getProductID(), mCallback);

		final Product product;
		if (productMap != null) {
			product = new Product(productMap);
		} else {
			throw new RuntimeException(client.getLastErrorMessage());
		}

		// cache
		if (product != null) {
			JobCacheManager.storeProductDetailsWithMerge(product, job.getJobID().getUrl());
		}
	}
}
