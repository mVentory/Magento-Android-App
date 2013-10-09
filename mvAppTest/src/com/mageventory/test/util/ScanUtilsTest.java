package com.mageventory.test.util;

import android.test.InstrumentationTestCase;

import com.mageventory.util.ScanUtils;

public class ScanUtilsTest extends InstrumentationTestCase
{
	public void testSanitizeScanResult()
	{
		assertEquals(ScanUtils.sanitizeScanResult("te" + (char) 0x1d + "st"),
				"test");
		assertEquals(ScanUtils.sanitizeScanResult("te" + (char) 0x20 + "st"),
				"te" + (char) 0x20 + "st");
		assertEquals(
				ScanUtils
						.sanitizeScanResult("http://www.google.com?test=abc&amp;"),
				"http://www.google.com?test=abc&amp;");
		assertEquals(ScanUtils.sanitizeScanResult("AA1100125" + (char) 0x1d),
				"AA1100125");
		assertEquals(ScanUtils.sanitizeScanResult("AA1100125" + (char) 0xd800),
				"AA1100125");
		assertEquals(ScanUtils.sanitizeScanResult("AA1100125" + (char) 0xe000),
				"AA1100125" + (char) 0xe000);
		assertEquals(
				ScanUtils.sanitizeScanResult((char) 0xffff + "AA1100125"
						+ (char) 0x1d + (char) 0xd800 + (char) 0xe000
						+ (char) 0xfffd), "AA1100125" + (char) 0xe000
						+ (char) 0xfffd);
	}
}
