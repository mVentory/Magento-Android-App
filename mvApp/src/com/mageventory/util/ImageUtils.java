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
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;

/**
 * Contains various image utils methods
 * 
 * @author Eugene Popovich
 */
public class ImageUtils {
    public static final String TAG = ImageUtils.class.getSimpleName();

    public static final String TAG_DATETIME_ORIGINAL = "DateTimeOriginal";
    public static final String TAG_DATETIME_DIGITIZED = "DateTimeDigitized";
    public static final String TAG_DATETIME = ExifInterface.TAG_DATETIME;

    /**
     * Decode and sample down a bitmap from resources to the requested width and
     * height.
     * 
     * @param res The resources object containing the image data
     * @param resId The resource id of the image data
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return A bitmap sampled down from the original with the same aspect
     *         ratio and dimensions that are equal to or greater than the
     *         requested width and height
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth,
            int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * Decode and sample down a bitmap from a file to the requested width and
     * height.
     * 
     * @param filename The full path of the file to decode
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return A bitmap sampled down from the original with the same aspect
     *         ratio and dimensions that are equal to or greater than the
     *         requested width and height
     */
    public static synchronized Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth,
            int reqHeight) {
        return decodeSampledBitmapFromFile(filename, reqWidth, reqHeight, 0);
    }

    /**
     * Decode and sample down a bitmap from a file to the requested width and
     * height.
     * 
     * @param filename The full path of the file to decode
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @param orientation the image orientation in degrees. Final bitmap will be
     *            pre rotated at this value
     * @return A bitmap sampled down from the original with the same aspect
     *         ratio and dimensions that are equal to or greater than the
     *         requested width and height
     */
    public static synchronized Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth,
            int reqHeight, int orientation) {
        return decodeSampledBitmapFromFile(filename, reqWidth, reqHeight, orientation, null);
    }

    /**
     * Decode and sample down a bitmap from a file to the requested width and
     * height.
     * 
     * @param filename The full path of the file to decode
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @param orientation the image orientation in degrees. Final bitmap will be
     *            pre rotated at this value
     * @param cropRect contains image region to crop. If null then ignored
     * @return A bitmap sampled down from the original with the same aspect
     *         ratio and dimensions that are equal to or greater than the
     *         requested width and height
     */
    public static synchronized Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth,
            int reqHeight, int orientation, Rect cropRect) {
        long start = System.currentTimeMillis();
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options;
        if (cropRect == null) {
            options = calculateImageSize(filename);
        } else {
            options = new BitmapFactory.Options();
            options.outWidth = cropRect.width();
            options.outHeight = cropRect.height();
        }

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        // #302 added to reduce amount of OutOfMemory errors
        options.inDither = false; // Disable Dithering mode
        options.inPurgeable = true; // Tell to gc that whether it needs
        // free memory, the Bitmap can be
        // cleared
        options.inInputShareable = true; // Which kind of reference will
        // be used to recover the Bitmap
        // data after being clear, when
        // it will be used in the future
        options.inTempStorage = new byte[32 * 1024];

        Bitmap result = decodeBitmap(filename, options, cropRect);
        TrackerUtils.trackDataProcessingTiming(System.currentTimeMillis() - start,
                "decodeSampledBitmapFromFile", TAG);
        result = getCorrectlyOrientedBitmap(result, orientation);
        return result;
    }

    /**
     * Out of Memory hack taken from here
     * http://stackoverflow.com/a/7116158/527759
     * 
     * @param path
     * @param bfOptions
     * @return
     */
    public static Bitmap decodeBitmap(String path, BitmapFactory.Options bfOptions) {
        return decodeBitmap(path, bfOptions, null);
    }

    /**
     * Out of Memory hack taken from here
     * http://stackoverflow.com/a/7116158/527759
     * 
     * @param path
     * @param bfOptions
     * @return
     */
    public static Bitmap decodeBitmap(String path, BitmapFactory.Options bfOptions, Rect cropRect) {
        Bitmap bm = null;
        File file = new File(path);
        FileInputStream fs = null;
        try {
            fs = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            GuiUtils.noAlertError(TAG, e);
        }

        try {
            if (fs != null) {
                if (cropRect == null) {
                    bm = BitmapFactory.decodeFileDescriptor(fs.getFD(), null, bfOptions);
                } else {
                    BitmapRegionDecoder decoder = BitmapRegionDecoder
                            .newInstance(fs.getFD(), false);
                    bm = decoder.decodeRegion(cropRect, bfOptions);
                }
            }
        } catch (IOException e) {
            GuiUtils.noAlertError(TAG, e);
        } finally {
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    GuiUtils.noAlertError(TAG, e);
                }
            }
        }
        return bm;
    }

    /**
     * Calculate the image size for the given file name.
     * 
     * @param filename
     * @return
     */
    public static BitmapFactory.Options calculateImageSize(String filename) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);
        return options;
    }

    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options}
     * object when decoding bitmaps using the decode* methods from
     * {@link BitmapFactory}. This implementation calculates the closest
     * inSampleSize that will result in the final decoded bitmap having a width
     * and height equal to or larger than the requested width and height. This
     * implementation does not ensure a power of 2 is returned for inSampleSize
     * which can be faster when decoding but results in a larger bitmap which
     * isn't as useful for caching purposes.
     * 
     * @param options An options object with out* params already populated (run
     *            through a decode* method with inJustDecodeBounds==true
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
            int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger
            // inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down
            // further.
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
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
        long start = System.currentTimeMillis();
        ExifInterface exif = new ExifInterface(fileName);
        int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        int rotationInDegrees = exifToDegrees(rotation);
        CommonUtils.debug(TAG, "getOrientationInDegreesForFileName: execution time %1$d ms",
                System.currentTimeMillis() - start);
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

    /**
     * Get the correctly oriented bitmap if orientation is different fro 0 value
     * Idea and code from http://stackoverflow.com/a/11081918/527759
     * 
     * @param bitmap - bitmap to orient properly
     * @param orientation - rotation degrees
     * @return
     */
    public static Bitmap getCorrectlyOrientedBitmap(Bitmap bitmap, int orientation) {
        if (orientation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    matrix, true);
        }
        return bitmap;
    }

    /**
     * Returns number of milliseconds since Jan. 1, 1970, midnight. Returns -1
     * if the date time information if not available.
     * 
     * @param attributeName
     * @throws IOException
     */
    public static long getExifDateTime(String fileName) throws IOException {
        long start = System.currentTimeMillis();
        ExifInterface exif = new ExifInterface(fileName);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        long result = getExifDateTime(exif, TAG_DATETIME_ORIGINAL, formatter);
        CommonUtils.debug(TAG, "getExifDateTime: getting %1$s", TAG_DATETIME_ORIGINAL);
        if (result == -1) {
            CommonUtils.debug(TAG, "getExifDateTime: getting %1$s", TAG_DATETIME_DIGITIZED);
            result = getExifDateTime(exif, TAG_DATETIME_DIGITIZED, formatter);
        }
        if (result == -1) {
            CommonUtils.debug(TAG, "getExifDateTime: getting %1$s", TAG_DATETIME);
            result = getExifDateTime(exif, TAG_DATETIME, formatter);
        }
        CommonUtils.debug(TAG, "getExifTime: execution time %1$d ms", System.currentTimeMillis()
                - start);
        return result;
    }

    /**
     * Returns number of milliseconds since Jan. 1, 1970, midnight. Returns -1
     * if the date time information if not available.
     * 
     * @param exif
     * @param attributeName
     * @param formatter
     * @return
     */
    private static long getExifDateTime(ExifInterface exif, String attributeName,
            SimpleDateFormat formatter) {
        String dateTimeString = exif.getAttribute(attributeName);
        if (dateTimeString == null)
            return -1;

        ParsePosition pos = new ParsePosition(0);
        try {
            Date datetime = formatter.parse(dateTimeString, pos);
            if (datetime == null)
                return -1;
            return datetime.getTime();
        } catch (IllegalArgumentException ex) {
            return -1;
        }
    }

    /**
     * Translate rect to the coordinates in non oriented image
     * 
     * @param bitmapRect
     * @param width
     * @param height
     * @param orientation
     * @return
     */
    public static Rect translateRect(Rect bitmapRect, int width, int height, int orientation)
    {
        RectF parent = new RectF(0, 0, width, height);
        Matrix transform = new Matrix();
        transform.setRotate(-orientation);
        RectF cropRect = new RectF(bitmapRect);
        transform.mapRect(cropRect);
        transform.mapRect(parent);
        Rect adjusted = new Rect((int) cropRect.left, (int) cropRect.top, (int) cropRect.right,
                (int) cropRect.bottom);
        adjusted.offset(-(int) parent.left, -(int) parent.top);
        return adjusted;
    }

    /**
     * Get the crop rect multipliers which can be used to determine real crop
     * rect values if image width and height is available
     * 
     * @param displayRect the component display rect
     * @param componentWidth the component width
     * @param componentHeight the component height
     * @return
     */
    public static RectF getCropRectMultipliers(RectF displayRect, int componentWidth,
            int componentHeight) {
        RectF result = new RectF();

        float displayRectWidth = displayRect.right - displayRect.left;
        float displayRectHeight = displayRect.bottom - displayRect.top;

        result.top = displayRect.top > 0 ? 0 : Math.abs(displayRect.top) / displayRectHeight;
        result.left = displayRect.left > 0 ? 0 : Math.abs(displayRect.left) / displayRectWidth;
        result.bottom = 1f - (displayRect.bottom < componentHeight ? 0
                : ((float) (displayRect.bottom - componentHeight)) / displayRectHeight);
        result.right = 1f - (displayRect.right < componentWidth ? 0
                : ((float) (displayRect.right - componentWidth)) / displayRectWidth);
        return result;
    }

    /**
     * Get the new multipliers rect by the child multipliers withing parent
     * 
     * @param cropRectMultipliersParent
     * @param cropRectMultipliersChild
     * @return
     */
    public static RectF getNewCropRectMultipliers(RectF cropRectMultipliersParent,
            RectF cropRectMultipliersChild) {
        CommonUtils
                .debug(TAG,
                        "getNewCropRectMultipliers: started for parent rect %1$f, %2$f, %3$f, %4$f and child rect %5$f, %6$f, %7$f, %8$f",
                        cropRectMultipliersParent.left, cropRectMultipliersParent.top,
                        cropRectMultipliersParent.right, cropRectMultipliersParent.bottom,
                        cropRectMultipliersChild.left, cropRectMultipliersChild.top,
                        cropRectMultipliersChild.right, cropRectMultipliersChild.bottom);
        RectF result = new RectF();
        float cropRectWidthMultiplier = cropRectMultipliersParent.right
                - cropRectMultipliersParent.left;
        float cropRectHeightMultiplier = cropRectMultipliersParent.bottom
                - cropRectMultipliersParent.top;
        result.left = cropRectMultipliersParent.left + cropRectMultipliersChild.left
                * cropRectWidthMultiplier;
        result.top = cropRectMultipliersParent.top + cropRectMultipliersChild.top
                * cropRectHeightMultiplier;
        result.right = cropRectMultipliersParent.left + cropRectMultipliersChild.right
                * cropRectWidthMultiplier;
        result.bottom = cropRectMultipliersParent.top + cropRectMultipliersChild.bottom
                * cropRectHeightMultiplier;
        CommonUtils.debug(TAG, "getNewCropRectMultipliers: resulting rect %1$f, %2$f, %3$f, %4$f",
                result.left, result.top, result.right, result.bottom);
        return result;
    }

    /**
     * Get realcrop rect for the multipliers. Image dimenstions will be
     * multiplied with the cropRectMultipliers values to get the real crop rect
     * in pixels
     * 
     * @param cropRectMultipliers
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    public static Rect getRealCropRectForMultipliers(RectF cropRectMultipliers, int imageWidth,
            int imageHeight) {
        Rect result = new Rect();
        result.top = (int) Math.floor(cropRectMultipliers.top * imageHeight);
        result.bottom = (int) Math.ceil(cropRectMultipliers.bottom * imageHeight);
        result.left = (int) Math.floor(cropRectMultipliers.left * imageWidth);
        result.right = (int) Math.ceil(cropRectMultipliers.right * imageWidth);
        return result;
    }
    
    public final static String PROTO_PREFIX = "https?:\\/\\/";
}
