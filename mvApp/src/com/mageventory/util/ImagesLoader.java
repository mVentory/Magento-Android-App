package com.mageventory.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.mageventory.MyApplication;
import com.mageventory.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class ImagesLoader {
	public static class CachedImage {
		public int mWidth, mHeight;
		public Bitmap mBitmap;
		public File mFile;

		public CachedImage(File file) {
			mFile = file;
		}

		@Override
		public boolean equals(Object o) {
			return mFile.getAbsolutePath().equals(((CachedImage) o).mFile.getAbsolutePath());
		}

		@Override
		public int hashCode() {
			return mFile.getAbsolutePath().hashCode();
		}
	}

	private AsyncTask<Void, Void, Void> mLoaderTask;
	private ArrayList<CachedImage> mCachedImages;
	private int mCurrentImageIndex = -1;

	private FrameLayout mLeftImage;
	private FrameLayout mCenterImage;
	private FrameLayout mRightImage;

	private Activity mActivity;

	private ArrayList<Integer> mCachedIndices;

	public ImagesLoader(Activity activity) {
		mCachedImages = new ArrayList<CachedImage>();
		mCachedIndices = new ArrayList<Integer>();
		mActivity = activity;
	}

	public boolean canSwitchLeft() {
		
		int left = getLeftVisibleIndex(mCurrentImageIndex);
		
		if (left >= 0) {
			return true;
		} else {
			return false;
		}
	}

	public boolean canSwitchRight() {
		
		int right = getRightVisibleIndex(mCurrentImageIndex);
		
		if (right <= mCachedImages.size()) {
			return true;
		} else {
			return false;
		}
	}

	public RectF getCurrentImageRectF() {
		if (mCachedImages.get(mCurrentImageIndex).mBitmap == null) {
			return null;
		}

		int bitmapWidth = mCachedImages.get(mCurrentImageIndex).mWidth;
		int bitmapHeight = mCachedImages.get(mCurrentImageIndex).mHeight;

		float imageMatrixArray[] = new float[9];
		imageView(mCenterImage).getImageMatrix().getValues(imageMatrixArray);

		float transX = imageMatrixArray[Matrix.MTRANS_X];
		float transY = imageMatrixArray[Matrix.MTRANS_Y];
		float scaleX = imageMatrixArray[Matrix.MSCALE_X];
		float scaleY = imageMatrixArray[Matrix.MSCALE_Y];

		return new RectF(transX, transY, transX + bitmapWidth * scaleX, transY + bitmapHeight * scaleY);
	}

	public String decodeQRCode(RectF cropRect) {
		Bitmap bitmapToDecode = null;

		if (cropRect != null) {
			if (mCachedImages.get(mCurrentImageIndex).mBitmap == null) {
				return null;
			}

			int bitmapWidth = mCachedImages.get(mCurrentImageIndex).mWidth;
			int bitmapHeight = mCachedImages.get(mCurrentImageIndex).mHeight;

			float preScale = ((float) mCachedImages.get(mCurrentImageIndex).mBitmap.getWidth()) / bitmapWidth;

			float imageMatrixArray[] = new float[9];
			imageView(mCenterImage).getImageMatrix().getValues(imageMatrixArray);

			float transX = imageMatrixArray[Matrix.MTRANS_X];
			float transY = imageMatrixArray[Matrix.MTRANS_Y];
			float scaleX = imageMatrixArray[Matrix.MSCALE_X] * preScale;
			float scaleY = imageMatrixArray[Matrix.MSCALE_Y] * preScale;

			cropRect.offset(-transX, -transY);

			cropRect.left /= scaleX;
			cropRect.right /= scaleX;
			cropRect.top /= scaleY;
			cropRect.bottom /= scaleY;

			Rect bitmapRect = new Rect((int) cropRect.left, (int) cropRect.top, (int) cropRect.right,
					(int) cropRect.bottom);

			bitmapRect.left = bitmapRect.left < 0 ? 0 : bitmapRect.left;
			bitmapRect.top = bitmapRect.top < 0 ? 0 : bitmapRect.top;

			bitmapRect.right = bitmapRect.right > bitmapWidth ? bitmapWidth : bitmapRect.right;
			bitmapRect.bottom = bitmapRect.bottom > bitmapHeight ? bitmapHeight : bitmapRect.bottom;

			try {
				FileInputStream fis = new FileInputStream(mCachedImages.get(mCurrentImageIndex).mFile);

				int screenSmallerDimension;

				DisplayMetrics metrics = new DisplayMetrics();
				mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
				screenSmallerDimension = metrics.widthPixels;
				if (screenSmallerDimension > metrics.heightPixels) {
					screenSmallerDimension = metrics.heightPixels;
				}

				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inInputShareable = true;
				opts.inPurgeable = true;
				opts.inJustDecodeBounds = true;
				int coef = Integer.highestOneBit((bitmapRect.right - bitmapRect.left) / screenSmallerDimension);

				opts.inJustDecodeBounds = false;
				if (coef > 1) {
					opts.inSampleSize = coef;
				}

				BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(fis, false);
				bitmapToDecode = decoder.decodeRegion(bitmapRect, opts);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			bitmapToDecode = mCachedImages.get(mCurrentImageIndex).mBitmap;
		}

		if (bitmapToDecode != null) {
			ZXingCodeScanner multiDetector = new ZXingCodeScanner();
			String code = multiDetector.decode(bitmapToDecode);
			return code;
		} else {
			return null;
		}
	}

	private void loadBitmap(CachedImage cachedImage) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inInputShareable = true;
		opts.inPurgeable = true;

		int screenSmallerDimension;

		DisplayMetrics metrics = new DisplayMetrics();
		mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		screenSmallerDimension = metrics.widthPixels;

		if (screenSmallerDimension > metrics.heightPixels) {
			screenSmallerDimension = metrics.heightPixels;
		}

		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(cachedImage.mFile.getAbsolutePath(), opts);

		cachedImage.mWidth = opts.outWidth;
		cachedImage.mHeight = opts.outHeight;

		int coef = Integer.highestOneBit(opts.outWidth / screenSmallerDimension);

		opts.inJustDecodeBounds = false;
		if (coef > 1) {
			opts.inSampleSize = coef;
		}

		cachedImage.mBitmap = BitmapFactory.decodeFile(cachedImage.mFile.getAbsolutePath(), opts);
	}

	private ImageView imageView(FrameLayout layout) {
		return (ImageView) layout.findViewById(R.id.image);
	}

	private ProgressBar progressBar(FrameLayout layout) {
		return (ProgressBar) layout.findViewById(R.id.progressBar);
	}

	/*
	 * Functions for moving across indices of images that the user should see.
	 * (They don't want to see images with SKU in file name in case of moving
	 * forwards)
	 */
	public int getLeftVisibleIndex(int idx) {
		return idx - 1;
	}

	public int getRightVisibleIndex(int idx) {
		idx = idx + 1;

		while (idx < mCachedImages.size() && mCachedImages.get(idx).mFile.getAbsolutePath().contains("__")) {
			idx++;
		}
		return idx;
	}

	public void setState(int currentImageIndex, FrameLayout leftImage, FrameLayout centerImage, FrameLayout rightImage) {
		int leftIndex, rightIndex;

		leftIndex = getLeftVisibleIndex(currentImageIndex);
		rightIndex = getRightVisibleIndex(currentImageIndex);

		if (mCurrentImageIndex == -1) {
			updateImageLayout(leftImage, leftIndex);
			updateImageLayout(centerImage, currentImageIndex);
			updateImageLayout(rightImage, rightIndex);
		} else {
			if (currentImageIndex < mCurrentImageIndex) {
				updateImageLayout(leftImage, leftIndex);
			} else if (currentImageIndex > mCurrentImageIndex) {
				updateImageLayout(leftImage, leftIndex);
				updateImageLayout(rightImage, rightIndex);
			} else if (currentImageIndex == mCurrentImageIndex) {
				updateImageLayout(leftImage, leftIndex);
				updateImageLayout(centerImage, currentImageIndex);
				updateImageLayout(rightImage, rightIndex);
			}
		}

		mCurrentImageIndex = currentImageIndex;
		mLeftImage = leftImage;
		mCenterImage = centerImage;
		mRightImage = rightImage;

		// Clear unnecessary items in the cache:

		Iterator<Integer> i = mCachedIndices.iterator();
		while (i.hasNext()) {
			Integer s = i.next();

			if (s != currentImageIndex && s != leftIndex && s != rightIndex) {
				mCachedImages.get(s).mBitmap = null;
				i.remove();
			}
		}
		
		loadImages();
	}

	private void updateImageLayout(FrameLayout layout, int associatedIndex) {
		ImageView imageView = imageView(layout);
		ProgressBar progressBar = progressBar(layout);

		if (associatedIndex < 0 || associatedIndex >= mCachedImages.size()) {
			imageView.setImageBitmap(null);
			imageView.setVisibility(View.GONE);
			progressBar.setVisibility(View.GONE);
		} else if (mCachedImages.get(associatedIndex).mBitmap == null) {
			imageView.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
		} else {
			imageView.setImageBitmap(mCachedImages.get(associatedIndex).mBitmap);
			progressBar.setVisibility(View.GONE);
			imageView.setVisibility(View.VISIBLE);
		}
	}

	public void addCachedImage(CachedImage cachedImage) {
		mCachedImages.add(cachedImage);
	}

	private AsyncTask<Void, Void, Void> getAsyncTask() {
		return new AsyncTask<Void, Void, Void>() {
			private CachedImage mCachedImageToLoad;
			private int mIndexToLoad;

			private int calculateIndexToLoad() {
				int leftIndex, rightIndex;
				leftIndex = getLeftVisibleIndex(mCurrentImageIndex);
				rightIndex = getRightVisibleIndex(mCurrentImageIndex);
				
				if (mCurrentImageIndex >= 0 && mCurrentImageIndex < mCachedImages.size()
						&& (mCachedImages.get(mCurrentImageIndex).mBitmap == null)) {
					return mCurrentImageIndex;
				}

				if (rightIndex < mCachedImages.size()
						&& mCachedImages.get(rightIndex).mBitmap == null) {
					return rightIndex;
				}

				if (leftIndex >= 0 && mCachedImages.get(leftIndex).mBitmap == null) {
					return leftIndex;
				}

				return -1;
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();

				mIndexToLoad = calculateIndexToLoad();

				if (mIndexToLoad == -1) {
					this.cancel(false);
					mLoaderTask = null;
				} else {
					mCachedImageToLoad = mCachedImages.get(mIndexToLoad);
				}
			}

			@Override
			protected Void doInBackground(Void... params) {
				loadBitmap(mCachedImageToLoad);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);

				int leftIndex, rightIndex;
				leftIndex = getLeftVisibleIndex(mCurrentImageIndex);
				rightIndex = getRightVisibleIndex(mCurrentImageIndex);
				
				if (mIndexToLoad != -1) {
					mCachedIndices.add(mIndexToLoad);

					if (leftIndex == mIndexToLoad) {
						updateImageLayout(mLeftImage, mIndexToLoad);
					} else if (mCurrentImageIndex == mIndexToLoad) {
						updateImageLayout(mCenterImage, mIndexToLoad);
					} else if (rightIndex == mIndexToLoad) {
						updateImageLayout(mRightImage, mIndexToLoad);
					} else {
						mCachedImages.get(mIndexToLoad).mBitmap = null;
						mCachedIndices.remove((Integer)mIndexToLoad);
					}
				}

				mLoaderTask = null;
				loadImages();
			}
		};
	}

	public String removeSKUFromFileName(String fileName) {
		String out;

		if (fileName.contains("__")) {
			out = fileName.substring(fileName.indexOf("__") + 2);
		} else {
			out = fileName;
		}

		return out;
	}

	public void undoImage(int idx) {
		File originalFile = mCachedImages.get(idx).mFile;
		String originalFileName = originalFile.getName();
		String newFileName = removeSKUFromFileName(originalFileName);

		if (!originalFileName.equals(newFileName)) {
			File newFile = new File(originalFile.getParentFile(), newFileName);
			originalFile.renameTo(newFile);
			mCachedImages.get(idx).mFile = newFile;
		}
	}

	public void queueImage(int idx, String sku) {
		File originalFile = mCachedImages.get(idx).mFile;
		File newFile = new File(originalFile.getParentFile(), sku + "__" + originalFile.getName());
		originalFile.renameTo(newFile);
		
		mCachedImages.get(idx).mFile = newFile;
	}

	private void loadImages() {
		if (mLoaderTask == null) {
			mLoaderTask = getAsyncTask();
			mLoaderTask.execute();
		}
	}
}