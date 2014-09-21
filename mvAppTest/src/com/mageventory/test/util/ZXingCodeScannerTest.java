package com.mageventory.test.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.test.InstrumentationTestCase;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.mageventory.activity.MainActivity.ImageData;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.ZXingCodeScanner;
import com.mageventory.util.ZXingCodeScanner.DetectDecodeResult;

public class ZXingCodeScannerTest extends InstrumentationTestCase {
	public static final String TAG = ZXingCodeScannerTest.class.getSimpleName();

	int mScreenLargerDimension;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager windowManager = (WindowManager) getInstrumentation().getContext().getSystemService(
				Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getMetrics(metrics);
		mScreenLargerDimension = metrics.widthPixels;
		if (mScreenLargerDimension < metrics.heightPixels) {
			mScreenLargerDimension = metrics.heightPixels;
		}
	}

	public void testDetectDecode() throws IOException {
		File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "IMAGES/mventory/testcodes");
		CommonUtils.debug(TAG, "%1$s", dir.getAbsolutePath());

		assertTrue(dir.exists());
		assertTrue(dir.isDirectory());
		ZXingCodeScanner scanner = new ZXingCodeScanner();
		for (File f : dir.listFiles()) {
			scanner.detectDecodeMultiStep(f.getAbsolutePath(), mScreenLargerDimension);
		}
	}

	public void testDecode() throws IOException {
		CommonUtils.debug(TAG, "Running testDecode.");
		File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "IMAGES/mventory/testcodes");
		assertTrue(dir.exists());
		assertTrue(dir.isDirectory());
		ZXingCodeScanner scanner = new ZXingCodeScanner(false);
		int countDecoded = 0;
		int countNotDecoded = 0;
		long totalTimeDecoded = 0;
		long totalTimeNotDecoded = 0;
		for (File f : dir.listFiles()) {
			long start = System.currentTimeMillis();
			DetectDecodeResult ddr = scanner.decode(f.getAbsolutePath());
			long time = System.currentTimeMillis() - start;
			if (ddr.isDecoded()) {
				countDecoded++;
				totalTimeDecoded += time;
			} else {
				countNotDecoded++;
				totalTimeNotDecoded += time;
				String[] splitFilePath = f.getAbsolutePath().split("/");
				String fileName = splitFilePath[splitFilePath.length - 1];
				CommonUtils.debug(TAG, "image %1$s failed. Taken %2$d ms.", fileName, time);
				// Save undecoded images for review
				ImageData id = ImageData.getImageDataForFile(f, false);
				Bitmap originalBitmap = ImageUtils.decodeSampledBitmapFromFile(f.getAbsolutePath(), 1000, 1000,
		                id.getOrientation(), null);
				splitFilePath = fileName.split("\\.");
				fileName = splitFilePath[0];
				writeImage(originalBitmap, fileName);
			}
		}
		long avgDecoded = countDecoded==0?0:totalTimeDecoded / countDecoded;
		long avgNotDecoded = countNotDecoded==0?0:totalTimeNotDecoded / countNotDecoded;
		
		CommonUtils.debug(TAG, "Decoded: %1$d. Averag time: %2$d.", countDecoded, avgDecoded);
		CommonUtils.debug(TAG, "Not Decoded: %1$d. Averag time: %2$d.", countNotDecoded, avgNotDecoded);
	}

	public void testDecodeTryHard() throws IOException {
		CommonUtils.debug(TAG, "Running testDecodeTryHard.");
		File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "IMAGES/mventory/testcodes");
		assertTrue(dir.exists());
		assertTrue(dir.isDirectory());
		ZXingCodeScanner scanner = new ZXingCodeScanner(true);
		int countDecoded = 0;
		int countNotDecoded = 0;
		long totalTimeDecoded = 0;
		long totalTimeNotDecoded = 0;
		for (File f : dir.listFiles()) {
			long start = System.currentTimeMillis();
			DetectDecodeResult ddr = scanner.decode(f.getAbsolutePath());
			long time = System.currentTimeMillis() - start;
			if (ddr.isDecoded()) {
				countDecoded++;
				totalTimeDecoded += time;
			} else {
				countNotDecoded++;
				totalTimeNotDecoded += time;
				String[] splitFilePath = f.getAbsolutePath().split("/");
				String fileName = splitFilePath[splitFilePath.length - 1];
				CommonUtils.debug(TAG, "image %1$s failed. Taken %2$d ms.", fileName, time);
				// Save undecoded images for review
				ImageData id = ImageData.getImageDataForFile(f, false);
				Bitmap originalBitmap = ImageUtils.decodeSampledBitmapFromFile(f.getAbsolutePath(), 1000, 1000,
		                id.getOrientation(), null);
				splitFilePath = fileName.split("\\.");
				fileName = splitFilePath[0];
				writeImage(originalBitmap, fileName);
			}
		}
		long avgDecoded = countDecoded==0?0:totalTimeDecoded / countDecoded;
		long avgNotDecoded = countNotDecoded==0?0:totalTimeNotDecoded / countNotDecoded;
		
		CommonUtils.debug(TAG, "Decoded: %1$d. Averag time: %2$d.", countDecoded, avgDecoded);
		CommonUtils.debug(TAG, "Not Decoded: %1$d. Averag time: %2$d.", countNotDecoded, avgNotDecoded);
	}

	
	/**
	 * Helper function to write images that fail to decode to SD card for review
	 * @param imageBitmap
	 * @param str
	 */
	public static void writeImage(Bitmap imageBitmap, String str) {
		FileOutputStream out = null;
		String width = Integer.toString(imageBitmap.getWidth());
		String height = Integer.toString(imageBitmap.getHeight());
		try {
			out = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/IMAGES/mventory/"
					+ str + " " + width + " x " + height + ".jpg");
		
			imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
