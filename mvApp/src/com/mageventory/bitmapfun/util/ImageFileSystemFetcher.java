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

package com.mageventory.bitmapfun.util;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ExifInterface;

import com.mventory.BuildConfig;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.LoadingControl;

public class ImageFileSystemFetcher extends ImageResizer {
    private static final String TAG = ImageFileSystemFetcher.class.getSimpleName();

    /**
     * Initialize providing a target image width and height for the processing
     * images.
     * 
     * @param context
     * @param loadingControl
     * @param imageWidth
     * @param imageHeight
     */
    public ImageFileSystemFetcher(Context context, LoadingControl loadingControl, int imageWidth,
            int imageHeight) {
        super(context, loadingControl, imageWidth, imageHeight);
    }

    /**
     * Initialize providing a single target image size (used for both width and
     * height);
     * 
     * @param context
     * @param loadingControl
     * @param imageSize
     */
    public ImageFileSystemFetcher(Context context, LoadingControl loadingControl, int imageSize) {
        super(context, loadingControl, imageSize);
    }

    /**
     * The main process method, which will be called by the ImageWorker in the
     * AsyncTaskEx background thread.
     * 
     * @param data The data to load the bitmap, in this case, a regular http URL
     * @return The downloaded and resized bitmap
     */
    protected Bitmap processBitmap(String data) {
        return processBitmap(data, imageWidth, imageHeight);
    }

    /**
     * The main process method, which will be called by the ImageWorker in the
     * AsyncTaskEx background thread.
     * 
     * @param fileName The fileName to load the bitmap
     * @param imageWidth
     * @param imageHeight
     * @return The downloaded and resized bitmap
     */
    public static Bitmap processBitmap(String fileName, int imageWidth, int imageHeight) {
        if (BuildConfig.DEBUG) {
            CommonUtils.debug(TAG, "processBitmap - " + fileName);
        }
        if (fileName == null) {
            return null;
        }

        // Download a bitmap, write it to a file
        final File f = new File(fileName);

        if (f != null && f.exists()) {
            try {
                int rotationInDegrees = getOrientationInDegreesForFileName(fileName);
                // Return a sampled down version
                return ImageUtils.decodeSampledBitmapFromFile(f.toString(), imageWidth,
                        imageHeight, rotationInDegrees);
            } catch (Exception ex) {
                GuiUtils.noAlertError(TAG, ex);
            }
        }

        return null;
    }

    @Override
    protected Bitmap processBitmap(Object data, ProcessingState processingState) {
        return processBitmap(String.valueOf(data));
    }

    /**
     * Get the orientation in degrees from file EXIF information. Idea and code
     * from http://stackoverflow.com/a/11081918/527759
     * 
     * @param fileName
     * @return
     * @throws IOException
     */
    public static int getOrientationInDegreesForFileName(String fileName) throws IOException {
        ExifInterface exif = new ExifInterface(fileName);
        int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        int rotationInDegrees = exifToDegrees(rotation);
        return rotationInDegrees;
    }

    /**
     * Convert exif orientation information to degrees. Idea and code taken from
     * http://stackoverflow.com/a/11081918/527759
     * 
     * @param exifOrientation
     * @return
     */
    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }
}
