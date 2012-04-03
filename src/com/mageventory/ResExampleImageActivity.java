package com.mageventory;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.widget.ImageView;

import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;

public class ResExampleImageActivity extends Activity implements MageventoryConstants, OperationObserver {

	private class ImageLoadTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected Boolean doInBackground(String... args) {
			// init params
			final String url = args[0];
			final String[] resParams = new String[] { url };

			// check if resource is available locally and doesn't need
			// processing
			final boolean isAvailable = resHelper.isResourceAvailable(ResExampleImageActivity.this, RES_EXAMPLE_IMAGE,
					resParams);
			if (isAvailable == false) {
				// the resource is not avilable locally, start new service load
				// operation
				requestId = resHelper.loadResource(ResExampleImageActivity.this, RES_EXAMPLE_IMAGE, resParams);
			} else {
				// resource is available, we need to get it from the resource
				// store and display it
				final byte[] bitmapBytes = resHelper.restoreResource(ResExampleImageActivity.this, RES_EXAMPLE_IMAGE,
						resParams);
				final Bitmap bm = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);

				// display
				displayBitmap(bm);
			}
			return Boolean.TRUE;
		}
	}

	private ImageView img;
	private ResourceServiceHelper resHelper;
	private int requestId;
	private String imageUrl;
	private boolean displayed = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init views
		img = new ImageView(this);
		setContentView(img);

		// init fields
		resHelper = ResourceServiceHelper.getInstance();

		// init intent/instance
		imageUrl = getIntent().getExtras().getString("image_url");
		if (savedInstanceState != null) {
			requestId = savedInstanceState.getInt(EKEY_OP_REQUEST_ID, INVALID_REQUEST_ID);
		}
		
		final Object instance = getLastNonConfigurationInstance();
		if (instance != null) {
			displayBitmap((Bitmap) instance);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		resHelper.registerLoadOperationObserver(this);

		// no need to check data if bitmap is already displayed
		if (displayed) {
			return;
		}

		if (resHelper.isPending(requestId)) {
			// wait for notification
		} else {
			startImageLoadTask();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		resHelper.unregisterLoadOperationObserver(this);
	}

	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		if (requestId == op.getOperationRequestId()) {
			// if this is the same operation that this activity requested
			resHelper.stopService(this, false);
			if (op.getException() == null) {
				// if no errors occurred during the operation
				startImageLoadTask();
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		final BitmapDrawable d = (BitmapDrawable) img.getDrawable();
		if (d != null) {
			return d.getBitmap();
		}
		return null;
	}

	private void displayBitmap(final Bitmap bm) {
		final Runnable set = new Runnable() {
			@Override
			public void run() {
				img.setImageBitmap(bm);
			}
		};
		if (isMainThread()) {
			set.run();
		} else {
			runOnUiThread(set);
		}
		displayed = true;
	}

	private boolean isMainThread() {
		return Looper.myLooper() == Looper.getMainLooper();
	}

	private void startImageLoadTask() {
		new ImageLoadTask().execute(imageUrl);
	}

}
