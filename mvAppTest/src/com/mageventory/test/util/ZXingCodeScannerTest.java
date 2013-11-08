package com.mageventory.test.util;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.test.InstrumentationTestCase;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.mageventory.util.ZXingCodeScanner;

public class ZXingCodeScannerTest extends InstrumentationTestCase
{
	public static final String TAG = ZXingCodeScannerTest.class.getSimpleName();

	int mScreenLargerDimension;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager windowManager = (WindowManager) getInstrumentation()
				.getContext().getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getMetrics(metrics);
		mScreenLargerDimension = metrics.widthPixels;
		if (mScreenLargerDimension < metrics.heightPixels)
		{
			mScreenLargerDimension = metrics.heightPixels;
		}
	}

	public void testDetectDecode() throws IOException
	{
		File dir = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath(), "IMAGES/mventory/testcodes");
		assertTrue(dir.exists());
		assertTrue(dir.isDirectory());
		ZXingCodeScanner scanner = new ZXingCodeScanner();
		for (File f : dir.listFiles())
		{
			scanner.detectDecodeMultiStep(f.getAbsolutePath(),
					mScreenLargerDimension);
		}
	}
}
