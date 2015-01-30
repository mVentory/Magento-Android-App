/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mageventory.bitmapfun.util;

import android.content.Context;
import android.graphics.Bitmap;

import com.mventory.BuildConfig;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.LoadingControl;

/**
 * A simple subclass of {@link ImageWorker} that resizes images from resources
 * given a target width and height. Useful for when the input images might be
 * too large to simply load directly into memory.
 */
public class ImageResizer extends ImageWorker {
    private static final String TAG = "ImageWorker";
    protected int imageWidth;
    protected int imageHeight;

    /**
     * Initialize providing a single target image size (used for both width and
     * height);
     * 
     * @param context
     * @param loadingControl
     * @param imageWidth
     * @param imageHeight
     */
    public ImageResizer(Context context, LoadingControl loadingControl,
            int imageWidth, int imageHeight)
    {
        super(context, loadingControl);
        setImageSize(imageWidth, imageHeight);
    }

    /**
     * Initialize providing a single target image size (used for both width and
     * height);
     * 
     * @param context
     * @param loadingControl
     * @param imageSize
     */
    public ImageResizer(Context context, LoadingControl loadingControl,
            int imageSize)
    {
        super(context, loadingControl);
        setImageSize(imageSize);
    }

    /**
     * Set the target image width and height.
     * 
     * @param width
     * @param height
     */
    public void setImageSize(int width, int height) {
        imageWidth = width;
        imageHeight = height;
    }

    /**
     * Set the target image size (width and height will be the same).
     * 
     * @param size
     */
    public void setImageSize(int size) {
        setImageSize(size, size);
    }

    /**
     * The main processing method. This happens in a background task. In this
     * case we are just sampling down the bitmap and returning it from a
     * resource.
     * 
     * @param resId
     * @return
     */
    private Bitmap processBitmap(int resId) {
        return processBitmap(resId, imageWidth, imageHeight);
    }

    /**
     * The main processing method. This happens in a background task. In this
     * case we are just sampling down the bitmap and returning it from a
     * resource.
     * 
     * @param resId
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    protected Bitmap processBitmap(int resId, int imageWidth, int imageHeight) {
        if (BuildConfig.DEBUG) {
            CommonUtils.debug(TAG, "processBitmap - " + resId);
        }
        return ImageUtils.decodeSampledBitmapFromResource(
                mContext.getResources(), resId, imageWidth, imageHeight);
    }

    @Override
    protected Bitmap processBitmap(Object data, ProcessingState processingState) {
        return processBitmap(Integer.parseInt(String.valueOf(data)));
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }
}
