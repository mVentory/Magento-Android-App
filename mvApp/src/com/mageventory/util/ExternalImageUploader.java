package com.mageventory.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.client.ImageStreaming;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCacheManager.ProductDetailsExistResult;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.resprocessor.ProductDetailsProcessor.ProductDetailsLoadException;
import com.mageventory.settings.Settings;
import com.mageventory.settings.Settings.ProfileIDNotFoundException;
import com.mageventory.settings.SettingsSnapshot;

public class ExternalImageUploader implements MageventoryConstants, OperationObserver {

	private Context mContext;

	private String mURL;
	private String mUser;
	private String mPassword;

	private SettingsSnapshot mSettingsSnapshot;
	private String mImagePath;
	private ResourceServiceHelper mResHelper = ResourceServiceHelper.getInstance();
	private int mLoadReqId = INVALID_REQUEST_ID;
	private CountDownLatch mDoneSignal;
	private boolean mProductLoadSuccess;
    private ProductDetailsLoadException lastProductDetailsLoadException;
	private String mProductDetailsSKU;

	private static final String TAG = "ExternalImageUploader";

	private String applyCropping(File imageFile) throws Exception {
		String newFilePath = null;

		Rect bitmapRectangle = ImagesLoader.getBitmapRect(imageFile);

		if (bitmapRectangle != null) {
			int orientation = ExifInterface.ORIENTATION_NORMAL;
			ExifInterface exif = null;

			exif = new ExifInterface(imageFile.getAbsolutePath());

			if (exif != null) {
				orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			}

			String oldName = imageFile.getName();

			int trimTo = oldName.toLowerCase().indexOf(".jpg") + 4;

			if (trimTo != -1) {
				String newFileName = oldName.substring(0, oldName.toLowerCase().indexOf(".jpg") + 4);
				File newFile = new File(imageFile.getParentFile(), newFileName);

				boolean renamed = imageFile.renameTo(newFile);

				if (renamed) {
					imageFile = newFile;
					newFilePath = newFile.getAbsolutePath();
				} else {
					throw new Exception("Unable to rename the image file.");
				}
			} else {
				throw new Exception("Image file name problem.");
			}

			FileInputStream fis = new FileInputStream(imageFile);

			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inInputShareable = true;
			opts.inPurgeable = true;

			BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(fis, false);
			Bitmap croppedBitmap = decoder.decodeRegion(bitmapRectangle, opts);

			fis.close();

			FileOutputStream fos = new FileOutputStream(imageFile);
			croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			fos.close();
			croppedBitmap = null;

			if (orientation != ExifInterface.ORIENTATION_NORMAL) {
				try {
					exif = new ExifInterface(imageFile.getAbsolutePath());
				} catch (IOException e1) {
				}

				if (exif != null) {
					exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + orientation);
					exif.saveAttributes();
				}
			}

		}

		return newFilePath;
	}

	/* Returns an SKU or throws an exception if something goes wrong. */
	public String uploadFile(String imagePath, long profileID, String productCode, String SKU) throws Exception {
		mImagePath = imagePath;

		File currentFile = new File(mImagePath);

		String fn = applyCropping(currentFile);

		if (fn != null)
		{
			currentFile = new File(fn);
		}

		if (!currentFile.exists()) {
			throw new Exception("The image does not exist: " + mImagePath);
		}

		Settings settings;
		try {
			settings = new Settings(mContext, profileID);
		} catch (ProfileIDNotFoundException e) {
			e.printStackTrace();
			return null;
		}

		mURL = settings.getUrl();
		mUser = settings.getUser();
		mPassword = settings.getPass();

		mSettingsSnapshot = new SettingsSnapshot(mContext);
		mSettingsSnapshot.setUser(mUser);
		mSettingsSnapshot.setPassword(mPassword);
		mSettingsSnapshot.setUrl(mURL);

		if (SKU == null) {

            ProductDetailsExistResult existResult = JobCacheManager.productDetailsExist(
                    productCode, mURL, true);
            if (existResult.isExisting())
            {
                SKU = existResult.getSku();
            } else {
				// download product details
				final String[] params = new String[2];
				params[0] = GET_PRODUCT_BY_SKU_OR_BARCODE; // ZERO --> Use
															// Product ID , ONE
															// -->
				// Use Product SKU
				params[1] = productCode;

				mResHelper.registerLoadOperationObserver(this);
				mLoadReqId = mResHelper.loadResource(mContext, RES_PRODUCT_DETAILS, params, mSettingsSnapshot);

				mDoneSignal = new CountDownLatch(1);
				while (true) {
					if (mDoneSignal.await(1, TimeUnit.SECONDS)) {
						break;
					}
				}

				mResHelper.unregisterLoadOperationObserver(this);

				if (mProductLoadSuccess == false) {
					Log.logCaughtException(new Exception("Unable to download product details."));
				} else {
					if (mProductDetailsSKU != null) {
						SKU = mProductDetailsSKU;
						mProductDetailsSKU = null;
					}
				}
			}
		}

		if (SKU != null) {
			Map<String, Object> imageData = new HashMap<String, Object>();

			imageData.put(MAGEKEY_PRODUCT_IMAGE_NAME,
					currentFile.getName().substring(0, currentFile.getName().toLowerCase().lastIndexOf(".jpg")));

			imageData.put(MAGEKEY_PRODUCT_IMAGE_CONTENT, currentFile.getAbsolutePath());
			imageData.put(MAGEKEY_PRODUCT_IMAGE_MIME, "image/jpeg");

			MagentoClient client;
			client = new MagentoClient(mSettingsSnapshot);

			final File fileToUpload = currentFile;
			Log.d(TAG, "Starting the upload process: " + fileToUpload.getAbsolutePath());

			Map<String, Object> productMap = client.uploadImage(imageData, SKU,
					new ImageStreaming.StreamUploadCallback() {

						@Override
						public void onUploadProgress(int progress, int max) {
							Log.d(TAG, "Upload progress: " + progress + "/" + max);
						}
					});

			final Product product;
			if (productMap != null) {
				product = new Product(productMap);
			} else {
				throw new RuntimeException(client.getLastErrorMessage());
			}

			// cache
			if (product != null) {
				JobCacheManager.storeProductDetailsWithMergeSynchronous(product, mURL);
			}

			return SKU;
		} else {
            throw new ProductDetailsLoadException("Unable to figure out what the SKU is.",
                    lastProductDetailsLoadException == null ?
                            ProductDetailsLoadException.ERROR_CODE_UNDEFINED
                            : lastProductDetailsLoadException.getFaultCode(), false);
		}
	}

	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		if (op.getOperationRequestId() == mLoadReqId) {

			if (op.getException() == null) {

				Bundle extras = op.getExtras();
				if (extras != null && extras.getString(MAGEKEY_PRODUCT_SKU) != null) {
					mProductDetailsSKU = extras.getString(MAGEKEY_PRODUCT_SKU);
				}

				mProductLoadSuccess = true;
			} else {
				mProductLoadSuccess = false;
                if (op.getException() instanceof ProductDetailsLoadException)
                {
                    lastProductDetailsLoadException = (ProductDetailsLoadException) op
                            .getException();
                }
			}
			mDoneSignal.countDown();
		}
	}

	public ExternalImageUploader(Context c) {
		mContext = c;
	}
}
