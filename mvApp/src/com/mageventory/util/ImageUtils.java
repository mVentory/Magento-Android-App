package com.mageventory.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

/**
 * Contains various image utils methods
 * 
 * @author Eugene Popovich
 */
public class ImageUtils {
	public static final String TAG = ImageUtils.class.getSimpleName();

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
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
            int reqWidth, int reqHeight) {

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
    public static synchronized Bitmap decodeSampledBitmapFromFile(String filename,
            int reqWidth, int reqHeight) {
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
    public static synchronized Bitmap decodeSampledBitmapFromFile(String filename,
            int reqWidth, int reqHeight, int orientation) {
        long start = System.currentTimeMillis();
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = calculateImageSize(filename);

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

        Bitmap result = decodeBitmap(filename, options);
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
        Bitmap bm = null;
        File file = new File(path);
        FileInputStream fs = null;
        try
        {
            fs = new FileInputStream(file);
        } catch (FileNotFoundException e)
        {
            GuiUtils.noAlertError(TAG, e);
        }

        try
        {
            if (fs != null)
                bm = BitmapFactory.decodeFileDescriptor(fs.getFD(), null,
                        bfOptions);
        } catch (IOException e)
        {
            GuiUtils.error(TAG, e);
        } finally
        {
            if (fs != null)
            {
                try
                {
                    fs.close();
                } catch (IOException e)
                {
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
    public static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
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
		ExifInterface exif = new ExifInterface(fileName);
		int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
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

	/**
	 * Get the correctly oriented bitmap if orientation is different fro 0 value
	 * Idea and code from http://stackoverflow.com/a/11081918/527759
	 * 
	 * @param bitmap
	 *            - bitmap to orient properly
	 * @param orientation
	 *            - rotation degrees
	 * @return
	 */
	public static Bitmap getCorrectlyOrientedBitmap(Bitmap bitmap, int orientation) {
		if (orientation != 0) {
			Matrix matrix = new Matrix();
			matrix.postRotate(orientation);
	
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		}
		return bitmap;
	}

}
