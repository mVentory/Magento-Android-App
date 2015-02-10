/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/

package com.mageventory.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.mventory.R;

public class ImagesLoader {
	
    static final String TAG = ImagesLoader.class.getSimpleName();

    public static class CachedImage {
        public int mWidth, mHeight;
        public Bitmap mBitmap;
        public File mFile;
        public int orientation;

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

    public String getCurrentImagePath()
    {
        if (mCurrentImageIndex >= 0 && mCurrentImageIndex < mCachedImages.size())
        {
            return mCachedImages.get(mCurrentImageIndex).mFile.getAbsolutePath();
        }
        else
        {
            return null;
        }
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

        float preScale = ((float) mCachedImages.get(mCurrentImageIndex).mBitmap.getWidth())
                / bitmapWidth;

        float imageMatrixArray[] = new float[9];
        imageView(mCenterImage).getImageMatrix().getValues(imageMatrixArray);

        float transX = imageMatrixArray[Matrix.MTRANS_X];
        float transY = imageMatrixArray[Matrix.MTRANS_Y];
        float scaleX = imageMatrixArray[Matrix.MSCALE_X];
        float scaleY = imageMatrixArray[Matrix.MSCALE_Y];

        return new RectF(transX, transY, transX + bitmapWidth * scaleX * preScale, transY
                + bitmapHeight * scaleY * preScale);
    }

    private String rectToString(Rect rect)
    {
        return "_" + rect.left + "_" + rect.top + "_" + rect.right + "_" + rect.bottom;
    }

    private static String getRectangleString(File file)
    {
        String fileName = file.getName();

        if (!fileName.toLowerCase().contains(".jpg") || fileName.toLowerCase().endsWith(".jpg"))
        {
            return null;
        }

        String rectString = fileName.substring(fileName.toLowerCase().indexOf(".jpg") + 4);

        return rectString;
    }

    public static Rect getBitmapRect(File file)
    {
        String rectString = getRectangleString(file);

        if (rectString == null)
        {
            return null;
        }

        String[] rectArray = rectString.split("_");

        if (rectArray.length != 5)
        {
            return null;
        }

        Rect r;

        try
        {
            r = new Rect(Integer.parseInt(rectArray[1]), Integer.parseInt(rectArray[2]),
                    Integer.parseInt(rectArray[3]), Integer.parseInt(rectArray[4]));
        } catch (NumberFormatException nfe)
        {
            return null;
        }

        return r;
    }

    private boolean isForRemoval(File file)
    {
        String fileName = file.getName();

        if (!fileName.toLowerCase().contains(".jpg") || fileName.toLowerCase().endsWith(".jpg"))
        {
            return false;
        }

        if (fileName.endsWith("_x"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public void crop(RectF cropRect) {
        if (mCachedImages.get(mCurrentImageIndex).mBitmap == null) {
            return;
        }

        int bitmapWidth = mCachedImages.get(mCurrentImageIndex).mWidth;
        int bitmapHeight = mCachedImages.get(mCurrentImageIndex).mHeight;

        float preScale = ((float) mCachedImages.get(mCurrentImageIndex).mBitmap.getWidth())
                / bitmapWidth;

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

        File originalFile = mCachedImages.get(mCurrentImageIndex).mFile;
        String originalFileName = originalFile.getName();

        int orientation = mCachedImages.get(mCurrentImageIndex).orientation;
        bitmapRect = ImageUtils.translateRect(bitmapRect, bitmapWidth, bitmapHeight, orientation);
        String newFileName = originalFileName.substring(0,
                originalFileName.toLowerCase().indexOf(".jpg") + 4);

        Rect previousBitmapRect = getBitmapRect(mCachedImages.get(mCurrentImageIndex).mFile);

        if (previousBitmapRect != null)
        {
            bitmapRect.offset(previousBitmapRect.left, previousBitmapRect.top);
        }

        newFileName = newFileName + rectToString(bitmapRect);

        File newFile = new File(originalFile.getParentFile(), newFileName);
        originalFile.renameTo(newFile);
        mCachedImages.get(mCurrentImageIndex).mFile = newFile;
        mCachedImages.get(mCurrentImageIndex).mBitmap = null;
        updateImageLayout(mCenterImage, mCurrentImageIndex);
        loadImages();
    }

    private String mLastReadCodeType;

    public String getLastReadCodeType()
    {
        return mLastReadCodeType;
    }

    /**
     * This method is only used in deprecated ExternalImagesEditingActivity and
     * should be removed. TODO Remove/refactor
     * 
     * @param cropRect
     * @return
     * @deprecated
     */
    public String decodeQRCode(RectF cropRect) {
        Bitmap bitmapToDecode = null;

        if (cropRect != null) {
            if (mCachedImages.get(mCurrentImageIndex).mBitmap == null) {
                return null;
            }

            int bitmapWidth = mCachedImages.get(mCurrentImageIndex).mWidth;
            int bitmapHeight = mCachedImages.get(mCurrentImageIndex).mHeight;

            float preScale = ((float) mCachedImages.get(mCurrentImageIndex).mBitmap.getWidth())
                    / bitmapWidth;

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

            Rect bitmapRect = new Rect((int) cropRect.left, (int) cropRect.top,
                    (int) cropRect.right,
                    (int) cropRect.bottom);

            bitmapRect.left = bitmapRect.left < 0 ? 0 : bitmapRect.left;
            bitmapRect.top = bitmapRect.top < 0 ? 0 : bitmapRect.top;

            bitmapRect.right = bitmapRect.right > bitmapWidth ? bitmapWidth : bitmapRect.right;
            bitmapRect.bottom = bitmapRect.bottom > bitmapHeight ? bitmapHeight : bitmapRect.bottom;

            int orientation = mCachedImages.get(mCurrentImageIndex).orientation;
            bitmapRect = ImageUtils.translateRect(bitmapRect, bitmapWidth, bitmapHeight, orientation);

            Rect previousBitmapRect = getBitmapRect(mCachedImages.get(mCurrentImageIndex).mFile);

            if (previousBitmapRect != null)
            {
                bitmapRect.offset(previousBitmapRect.left, previousBitmapRect.top);
            }

            try {
                FileInputStream fis = new FileInputStream(
                        mCachedImages.get(mCurrentImageIndex).mFile);

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
                int coef = Integer.highestOneBit((bitmapRect.right - bitmapRect.left)
                        / screenSmallerDimension);

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

            if (code != null)
            {
                mLastReadCodeType = multiDetector.getLastReadCodeType();
            }

            return code;
        } else {
            return null;
        }
    }

    /**
     * This is used for images with portrait orientation to swap width and
     * height dimensions
     * 
     * @param cachedImage
     */
    private static void swapWidthAndHeightAttributes(CachedImage cachedImage) {
        int height = cachedImage.mHeight;
        cachedImage.mHeight = cachedImage.mWidth;
        cachedImage.mWidth = height;
    }

    /**
     * Returns true if orientation is portrait
     * 
     * @param orientation
     * @return
     */
    public static boolean isPortraitOrientation(int orientation) {
        return orientation == 90 || orientation == 270;
    }

    public void loadBitmap(CachedImage cachedImage) {
        int screenSmallerDimension;

        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        screenSmallerDimension = metrics.widthPixels;

        if (screenSmallerDimension > metrics.heightPixels) {
            screenSmallerDimension = metrics.heightPixels;
        }
        loadBitmap(cachedImage, screenSmallerDimension);
    }

    public static void loadBitmap(CachedImage cachedImage, int smallerDimension) {
        Rect cropRect = getBitmapRect(cachedImage.mFile);
        int orientation = 0;
        try {
            orientation = ImageUtils.getOrientationInDegreesForFileName(cachedImage.mFile
                    .getAbsolutePath());
        } catch (IOException e1) {
            CommonUtils.error(TAG, e1);
        }

        if (cropRect == null)
        {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inInputShareable = true;
            opts.inPurgeable = true;

            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(cachedImage.mFile.getAbsolutePath(), opts);

            cachedImage.mWidth = opts.outWidth;
            cachedImage.mHeight = opts.outHeight;

            if (isPortraitOrientation(orientation)) {
                swapWidthAndHeightAttributes(cachedImage);
            }

            int coef = Integer.highestOneBit(opts.outWidth / smallerDimension);

            opts.inJustDecodeBounds = false;
            if (coef > 1) {
                opts.inSampleSize = coef;
            }

            Bitmap bitmap = BitmapFactory.decodeFile(cachedImage.mFile.getAbsolutePath(), opts);
            bitmap = ImageUtils.getCorrectlyOrientedBitmap(bitmap, orientation);

            cachedImage.mBitmap = bitmap;
            cachedImage.orientation = orientation;
        }
        else
        {
            try {
                FileInputStream fis = new FileInputStream(cachedImage.mFile);

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inInputShareable = true;
                opts.inPurgeable = true;
                opts.inJustDecodeBounds = true;
                int coef = Integer.highestOneBit((cropRect.right - cropRect.left)
                        / smallerDimension);

                opts.inJustDecodeBounds = false;
                if (coef > 1) {
                    opts.inSampleSize = coef;
                }

                BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(fis, false);
                Bitmap bitmap = decoder.decodeRegion(cropRect, opts);
                bitmap = ImageUtils.getCorrectlyOrientedBitmap(bitmap, orientation);
                cachedImage.mBitmap = bitmap;

                cachedImage.mWidth = cropRect.width();
                cachedImage.mHeight = cropRect.height();

                cachedImage.orientation = orientation;

                if (isPortraitOrientation(orientation)) {
                    swapWidthAndHeightAttributes(cachedImage);
                }

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

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

        while (idx < mCachedImages.size()
                && mCachedImages.get(idx).mFile.getAbsolutePath().contains("__")) {
            idx++;
        }
        return idx;
    }

    public void setState(int currentImageIndex, FrameLayout leftImage, FrameLayout centerImage,
            FrameLayout rightImage) {
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
                        mCachedIndices.remove((Integer) mIndexToLoad);
                    }
                }

                mLoaderTask = null;
                loadImages();
            }
        };
    }

    public static String removeSKUFromFileName(String fileName) {
        String out;

        if (fileName.contains("__")) {
            out = fileName.substring(fileName.indexOf("__") + 2);
        } else {
            out = fileName;
        }

        return out;
    }

    /**
     * Get the sku prefix from file name if exists
     * 
     * @param fileName
     * @return
     */
    public static String getSkuFromFileName(String fileName) {
        int p = fileName.indexOf("__");
        String result = null;
        if (p != -1) {
            try {
                result = fileName.substring(0, p);
                // some barcodes or SKUs may be URL encoded because of
                // containing characters such as '/', so try to decode them
                result = URLDecoder.decode(result, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                CommonUtils.error(TAG, e);
            }
        }
        return result;
    }

    /**
     * Checks whether fileName contains SKU prefix
     * 
     * @param fileName
     * @return
     */
    public static boolean hasSKUInFileName(String fileName)
    {
        return fileName.contains("__");
    }

    /**
     * Checks whether fileName contains _x suffix
     * 
     * @param fileName
     * @return
     */
    public static boolean isDecodedCode(String fileName) {
        return fileName.endsWith("_x");
    }

    /**
     * Checks whether the file is of special renamed case. Either ends with "_x"
     * or has bitmap rect coordinates
     * 
     * @param file
     * @return
     */
    public static boolean isSpecialRenamedFile(File file)
    {
        boolean result = false;
        if (file.getName().endsWith("_x"))
        {
            result = true;
        } else
        {
            if (getBitmapRect(file) != null)
            {
                result = true;
            }
        }
        return result;
    }

    public void undoImage(int idx) {
        File originalFile = mCachedImages.get(idx).mFile;
        String originalFileName = originalFile.getName();
        String newFileName = removeSKUFromFileName(originalFileName);

        if (newFileName.endsWith("_x"))
        {
            newFileName = newFileName.substring(0, newFileName.length() - 2);
        }

        if (!originalFileName.equals(newFileName)) {
            File newFile = new File(originalFile.getParentFile(), newFileName);
            originalFile.renameTo(newFile);
            mCachedImages.get(idx).mFile = newFile;
        }
    }

    public String getCurrentSKU() {
        for (int idx = mCurrentImageIndex - 1; idx >= 0; idx--) {
            String fileName = mCachedImages.get(idx).mFile.getName();
            String sku;

            if (fileName.contains("__")) {
                return fileName.substring(0, fileName.indexOf("__"));
            }
        }

        return null;
    }

    public void queueImage(int idx, String sku, boolean discardLater) {
        if (sku == null)
            return;

        // try to URL encode passed SKU parameter because it may contain
        // characters which are not allowed to be used in file names ('/' for
        // example)
        try {
            sku = URLEncoder.encode(sku, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            CommonUtils.error(TAG, e);
        }
        File originalFile = mCachedImages.get(idx).mFile;
        String newFileName = sku + "__" + originalFile.getName();

        if (discardLater == true)
        {
            String rectangleString = getRectangleString(originalFile);

            if (rectangleString != null)
            {
                int rectangleStringIndex = newFileName.lastIndexOf(rectangleString);

                if (rectangleStringIndex != -1)
                {
                    newFileName = newFileName.substring(0, rectangleStringIndex);
                    mCachedImages.get(idx).mBitmap = null;
                }
            }

            newFileName += "_x";
        }

        File newFile = new File(originalFile.getParentFile(), newFileName);
        originalFile.renameTo(newFile);

        mCachedImages.get(idx).mFile = newFile;
    }

    public static File queueImage(File originalFile, String sku, boolean reassignSkuIfExists,
            boolean discardLater) {
        if (sku == null)
            return null;
        String originalName = originalFile.getName();
        int p = originalName.indexOf("__");
        if(p != -1)
        {
            if(reassignSkuIfExists)
            {
                originalName = originalName.substring(p+2);
            } else
            {
                return originalFile;
            }
        }
        // try to URL encode passed SKU parameter because it may contain
        // characters which are not allowed to be used in file names ('/' for
        // example)
        try {
            sku = URLEncoder.encode(sku, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            CommonUtils.error(TAG, e);
        }
        String newFileName = sku + "__" + originalName;
        if (discardLater && !newFileName.endsWith("_x")) {
            String rectangleString = getRectangleString(originalFile);

            if (rectangleString != null) {
                int rectangleStringIndex = newFileName.lastIndexOf(rectangleString);

                if (rectangleStringIndex != -1) {
                    newFileName = newFileName.substring(0, rectangleStringIndex);
                }
            }

            newFileName += "_x";
        }

        File newFile = new File(originalFile.getParentFile(), newFileName);
        originalFile.renameTo(newFile);

        return newFile;
    }

    public static File undoImage(File originalFile) {
        String originalFileName = originalFile.getName();
        String newFileName = removeSKUFromFileName(originalFileName);

        if (newFileName.endsWith("_x")) {
            newFileName = newFileName.substring(0, newFileName.length() - 2);
        }
        File newFile = originalFile;
        if (!originalFileName.equals(newFileName)) {
            newFile = new File(originalFile.getParentFile(), newFileName);
            originalFile.renameTo(newFile);
        }
        return newFile;
    }

    public void queueSkippedForRemoval()
    {
        for (int i = 0; i < mCachedImages.size(); i++) {
            String name = mCachedImages.get(i).mFile.getName();

            if (!name.contains("__") && !name.endsWith("_x")) {
                queueImage(i, "x", true);
            }
        }
    }

    private void loadImages() {
        if (mLoaderTask == null) {
            mLoaderTask = getAsyncTask();
            mLoaderTask.execute();
        }
    }

    public ArrayList<File> getSkippedFiles() {
        ArrayList<File> skippedFiles = new ArrayList<File>();

        for (int i = 0; i < mCachedImages.size(); i++) {
            if (!mCachedImages.get(i).mFile.getName().contains("__")) {
                skippedFiles.add(mCachedImages.get(i).mFile);
            }
        }

        return skippedFiles;
    }

    public ArrayList<File> getFilesToUpload() {
        ArrayList<File> filesToUpload = new ArrayList<File>();

        for (int i = 0; i < mCachedImages.size(); i++) {
            if (mCachedImages.get(i).mFile.getName().contains("__")) {
                filesToUpload.add(mCachedImages.get(i).mFile);
            }
        }

        return filesToUpload;
    }

    public int getAllFilesCount()
    {
        return mCachedImages.size();
    }

    public int getFilesToUploadCount()
    {
        int res = 0;

        for (int i = 0; i < mCachedImages.size(); i++) {
            if (mCachedImages.get(i).mFile.getName().contains("__")
                    && !mCachedImages.get(i).mFile.getName().endsWith("_x")) {
                res++;
            }
        }

        return res;
    }

    public int getSKUsToUploadCount()
    {
        int res = 0;

        List<String> skus = new ArrayList<String>();

        for (int i = 0; i < mCachedImages.size(); i++) {
            String name = mCachedImages.get(i).mFile.getName();

            if (name.contains("__") && !name.endsWith("_x")) {

                String sku = name.substring(0, name.indexOf("__"));

                if (!skus.contains(sku))
                {
                    skus.add(sku);
                }
            }
        }

        return skus.size();
    }

    public int getSkippedCount()
    {
        int res = 0;

        for (int i = 0; i < mCachedImages.size(); i++) {
            String name = mCachedImages.get(i).mFile.getName();

            if (!name.contains("__") && !name.endsWith("_x")) {
                res++;
            }
        }

        return res;
    }

    public int getImagesCount() {
        return mCachedImages.size();
    }
}
