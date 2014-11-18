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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import magick.ColorspaceType;
import magick.ImageInfo;
import magick.MagickImage;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;

/**
 * A helper class containing a collection of image utils static methods.
 * 
 * @author Eugene Popovich
 */
public class ImageUtils {
    public static final String TAG = ImageUtils.class.getSimpleName();
    
    /**
     * Path to the image magic library from the mVentory ImageMagic plugin.
     */
    public static final String IMAGE_MAGICK_LIBRARY_PATH = "/data/data/com.mageventory.image_magic_plugin/lib/libandroid-magick.so";

    public static final String TAG_DATETIME_ORIGINAL = "DateTimeOriginal";
    public static final String TAG_DATETIME_DIGITIZED = "DateTimeDigitized";
    public static final String TAG_DATETIME = ExifInterface.TAG_DATETIME;
    /**
     * The size of bitmap pixel in size when the default config
     * {@link Bitmap.Config.ARGB_8888} is used for bitmap decoding
     */
    public static final int PIXEL_SIZE_IN_BYTES = 4;
    /**
     * Maximim recommended dimension for the device for the device. Value based
     * on the device memory class
     */
    public static final int MAXIMUM_RECOMMENDED_DIMENSION = getMaximumAllowedImageDimensionForCurrentDevice();

    /**
     * The object for the read-write synchronization when converting images from
     * CMYK to RGB color space
     */
    public static Object sReadWriteLockObject = new Object();

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
        
        // switched off the option. The bitmap will be stored in app heap
        options.inPurgeable = false; // Tell to gc that whether it needs
        // free memory, the Bitmap can be
        // cleared
        options.inInputShareable = true; // Which kind of reference will
        // be used to recover the Bitmap
        // data after being clear, when
        // it will be used in the future
        options.inTempStorage = new byte[32 * 1024];

        Bitmap result = decodeBitmap(filename, options, cropRect);
        if (result == null) {
            // if bitmap was not decoded properly
            if (convertToRgbIfNecessary(filename)) {
                // if image has been converted to RGB successfully
                return decodeSampledBitmapFromFile(filename, reqWidth, reqHeight, orientation,
                        cropRect);
            }
        }
        TrackerUtils.trackDataProcessingTiming(System.currentTimeMillis() - start,
                "decodeSampledBitmapFromFile", TAG);
        CommonUtils.debug(TAG, "decodeSampledBitmapFromFile: decoded bitmap %1$dx%2$d",
                result.getWidth(), result.getHeight());
        result = getCorrectlyOrientedBitmap(result, orientation);
        return result;
    }

    /**
     * Convert file to RGB color space if necessary
     * @param filename the full path of the file to check and convert
     * @return true if file was converted to RGB color space, false otherwise
     */
    public static boolean convertToRgbIfNecessary(String filename) {
        boolean result = false;
        if (!(new File(IMAGE_MAGICK_LIBRARY_PATH)).exists()) {
            // if ImageMagick plugin is not installed
            return result;
        }
        try {
            ImageInfo info;
            MagickImage imageCMYK;
            synchronized (sReadWriteLockObject) {
                info = new ImageInfo(filename);
                imageCMYK = new MagickImage(info);
            }
            CommonUtils.debug(TAG,
                    "convertToRgbIfNecessary: ColorSpace BEFORE => " + imageCMYK.getColorspace());
            if (imageCMYK.getColorspace() == ColorspaceType.CMYKColorspace) {
                CommonUtils
                        .debug(TAG,
                                "convertToRgbIfNecessary: the image %1$s has CMYK color space. Converting...",
                                filename);
                boolean status = imageCMYK.transformRgbImage(imageCMYK.getColorspace());
                CommonUtils.debug(TAG,
                        "convertToRgbIfNecessary: ColorSpace AFTER => " + imageCMYK.getColorspace()
                                + ", success = " + status);
                boolean rewritten = false;
                File src = new File(filename);
                File backup = new File(filename + "_bak");
                synchronized (sReadWriteLockObject) {
                    try {
                        // backup source file
                        FileUtils.copy(src, backup);
                        // rewrite original file
                        rewritten = imageCMYK.writeImage(info);
                    } finally {
                        if (rewritten) {
                            // operation was successful, delete backup
                            backup.delete();
                        } else {
                            // operation failed, restore source from backup
                            src.delete();
                            backup.renameTo(src);
                        }
                    }
                }
                if (rewritten) {
                    CommonUtils
                            .debug(TAG,
                                    "convertToRgbIfNecessary: the image %1$s has been successfully converted and overwritten.",
                                    filename);
                    result = true;

                } else {
                    CommonUtils.debug(TAG,
                            "convertToRgbIfNecessaryAndDecode: the image %1$s conversion failed",
                            filename);

                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
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

        // if height or width are more than requested value or the square of
        // requested dimenstion is more than square of maximum recommended
        // dimension 
        if (height > reqHeight || width > reqWidth
                || Math.sqrt(reqHeight) * Math.sqrt(reqWidth) > MAXIMUM_RECOMMENDED_DIMENSION) {
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
            // further. The minimum value of the required values square and the maximum recommended dimension
            float totalReqPixelsCap = (float) Math.min(Math.sqrt(reqWidth) * Math.sqrt(reqHeight)
                    * Math.sqrt(2), MAXIMUM_RECOMMENDED_DIMENSION);
            totalReqPixelsCap = totalReqPixelsCap * totalReqPixelsCap;

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
        return rotateBitmap(bitmap,orientation);
    }

    /**
     * Rotates a bitmap by theta degrees. If theta is 0, the original image is
     * returned. If theta is a multiple of 90, no interpolation fill be
     * performed, otherwise bilinear interpolation will be used.
     * 
     * @param bitmap image to rotate
     * @param theta angle in degrees
     * @return rotated {@code Bitmap}
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int theta) {
        theta = theta % 360;
        if (theta != 0) {
            boolean filter = (theta == 90 || theta == 180 || theta == 270) ? false : true;
            Matrix rotM = new Matrix();
            rotM.setRotate(theta);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotM,
                    filter);
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
     * Translates {@code Rect} to the coordinates in non oriented image
     * 
     * @param bitmapRect {@code Rect} in coordinates of correctly oriented image
     * @param width
     * @param height
     * @param orientation
     * @return {@code Rect} in coordinates of original image
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
     * Takes a {@code RectF} in normalised units (between 0 and 1) and converts
     * to coordinate frame of an image with given width and height. 
     * 
     * @param cropRectMultipliers RectF with normalised units
     * @param imageWidth
     * @param imageHeight
     * @return Rect with units of pixels
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

    /**
     * This is experimental method to get the maximum allowed image dimension
     * for the current device. Calculation is based in device memory class
     * information and pixel size of 4 bytes per image
     * 
     * @return
     */
    public static int getMaximumAllowedImageDimensionForCurrentDevice() {
        final int memClass = ((ActivityManager) MyApplication.getContext().getSystemService(
                Context.ACTIVITY_SERVICE)).getMemoryClass();

        long memCacheSize = 1024 * 1024 * memClass;
        // Image should not use more than half of maximum heap size
        int dimension = (int) (Math.sqrt(memCacheSize / PIXEL_SIZE_IN_BYTES) / 2);
        Log.d(TAG,
                CommonUtils
                        .format("getMaximumAllowedImageDimensionForCurrentDevice: memCacheSize %1$d; dimension %2$d",
                                memCacheSize, dimension));
        TrackerUtils.trackBackgroundEvent("getMaximumAllowedImageDimensionForCurrentDevice",
                CommonUtils.format("memCacheSize %1$d; dimension %2$d", memCacheSize, dimension));
        return dimension;
    }

    /**
     * Check whether the path is URL (has starting http:// or https:// protocol
     * prefix)
     * 
     * @param path the path to check
     * @return true if the specified path is of URL type, otherwise returns
     *         false
     */
    public static boolean isUrl(String path) {
        return path.matches("(?i).*" + ImageUtils.PROTO_PREFIX + ".*");
    }

    /**
     * Merge bitmaps into single line sequentially
     * 
     * @param bitmaps
     * @return merged bitmap
     */
    public static Bitmap mergeBitmapsIntoLine(Bitmap... bitmaps) {
        if (bitmaps == null || bitmaps.length == 0) {
            // no bitmaps to merge
            return null;
        }
        int maxHeight = 0;
        int totalWidth = 0;
        // calculate total width and max height
        for (Bitmap b : bitmaps) {
            maxHeight = Math.max(maxHeight, b.getHeight());
            totalWidth += b.getWidth();
        }
        // create result bitmap to fill
        Bitmap result = Bitmap.createBitmap(totalWidth, maxHeight,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        int leftOffset = 0;
        // draw bitmaps to result canvas sequentially
        for (Bitmap b : bitmaps) {
            // calculate top offset, center bitmap vertically
            int topOffset = (maxHeight - b.getHeight()) / 2;
            canvas.drawBitmap(b, leftOffset, topOffset, null);
            // adjust left offset on bitmap width
            leftOffset += b.getWidth();
        }
        return result;
    }

    public final static String PROTO_PREFIX = "https?:\\/\\/";
    final static String RELATIVE_PATH_SYMBOL = "(?:[^'\\\"\\s\\\\#?]|(?:\\\\\\/))";
    final static Pattern IMG_URL_PATTERN = Pattern.compile(
            "("
                    + "(?:"
                    + "(?:"
                    // proto and domain
                    + PROTO_PREFIX + "(?:[a-z0-9\\-]+\\.)+[a-z]{2,6}"
                    + "(?:\\:\\d{1,5})?\\/" // plus option port number
                    + ")"
                    + "|"
                    + "(?<=[\\\"'])"
                    + ")"
                    + "(?!" + RELATIVE_PATH_SYMBOL + "*" + PROTO_PREFIX + RELATIVE_PATH_SYMBOL + "+)"
                    + RELATIVE_PATH_SYMBOL + "+"
                    + "\\.(?:jpe?g|gif|png)"
                    + ")"
            , Pattern.CASE_INSENSITIVE);

    /**
     * Extract all absolute and relative image urls from the passed html string
     * 
     * @param str
     * @param pageUrl
     * @return
     */
    public static String[] extractImageUrls(String str, String pageUrl) {
        if (str == null) {
            return null;
        }
        List<String> urls = new ArrayList<String>();
        String domain = null;
        String domainWithPath = null;

        if (pageUrl != null) {
            int p = pageUrl.indexOf("/", 9);
            CommonUtils.debug(TAG, "extractImageUrls: url %1$s", pageUrl);
            domain = p == -1 ? pageUrl : pageUrl.substring(0, p);
            CommonUtils.debug(TAG, "extractImageUrls: domain %1$s", domain);
            p = pageUrl.indexOf("?");
            domainWithPath = p == -1 ? pageUrl : pageUrl.substring(0, p);
            p = domainWithPath.lastIndexOf("/");
            domainWithPath = p == -1 ? domainWithPath : domainWithPath.substring(0, p);
            CommonUtils.debug(TAG, "extractImageUrls: domainWithPath %1$s", domainWithPath);
        } else {
            CommonUtils.debug(TAG, "extractImageUrls: pageUrl is null");
        }
        Matcher m = IMG_URL_PATTERN.matcher(str);
        while (m.find()) {
            String url = m.group(1);
            CommonUtils.debug(TAG, "extractImageUrls: extracted %1$s", url);
            String urlUnescaped = url.replaceAll("\\\\\\/", "/");
            if (!urlUnescaped.equals(url)) {
                url = urlUnescaped;
                CommonUtils.debug(TAG, "extractImageUrls: unescaped %1$s", url);
            }
            String urlLc = url.toLowerCase();
            if (pageUrl != null
                    && !(urlLc.startsWith(MageventoryConstants.HTTP_PROTO_PREFIX) || urlLc
                            .startsWith(MageventoryConstants.HTTPS_PROTO_PREFIX))) {
                if (url.startsWith("/")) {
                    url = domain + url;
                } else {
                    url = domainWithPath + "/" + url;
                }
            }
            if (urls.indexOf(url) == -1) {
                CommonUtils.debug(TAG, "extractImageUrls: url %1$s is correct. Adding", url);
                urls.add(url);
            }
        }
        String[] urlsArray = new String[urls.size()];
        urls.toArray(urlsArray);
        return urlsArray;
    }

}
