package com.mageventory.test.util;

import java.util.regex.Matcher;

import android.test.InstrumentationTestCase;

import com.mageventory.util.ScanUtils;
import com.mageventory.util.ScanUtils.ScanState;

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

	public void testScanStatePattern()
	{
		Matcher m;
		m = ScanUtils.SCAN_STATE_PATTERN.matcher("test file name.jpg");
		assertFalse(m.find());
		m = ScanUtils.SCAN_STATE_PATTERN.matcher("test file name[DND].jpg");
		assertTrue(m.find());
		for (int i = 0; i < m.groupCount(); i++)
		{
			System.out.println("group: " + i + ":" + m.group(i));
		}
		assertTrue(m.groupCount() == 5);
		m = ScanUtils.SCAN_STATE_PATTERN.matcher("test file name[ND].jpg");
		assertTrue(m.find());
		for (int i = 0; i < m.groupCount(); i++)
		{
			System.out.println("group: " + i + ":" + m.group(i));
		}
		assertTrue(m.groupCount() == 5);
		m = ScanUtils.SCAN_STATE_PATTERN.matcher("test file name[D].jpg");
		assertTrue(m.find());
		for (int i = 0; i < m.groupCount(); i++)
		{
			System.out.println("group: " + i + ":" + m.group(i));
		}
		assertTrue(m.groupCount() == 5);
		m = ScanUtils.SCAN_STATE_PATTERN.matcher("test file name[ABC].jpg");
		assertFalse(m.find());
	}

	public void testGetScanStateForFileName()
	{
		assertSame(ScanState.NOT_SCANNED,
				ScanUtils.getScanStateForFileName("test file name[ABC].jpg"));
		assertSame(ScanState.NOT_SCANNED,
				ScanUtils.getScanStateForFileName("test file name.jpg"));
		assertSame(
				ScanState.SCANNED_NOT_DETECTED,
				ScanUtils
						.getScanStateForFileName("ScanUtils.getScanStateForFileName(test file name[ND].jpg"));
		assertSame(ScanState.SCANNED_NOT_DETECTED,
				ScanUtils.getScanStateForFileName("test file name[ND].jpg_x"));
		assertSame(ScanState.SCANNED_DETECTED_NOT_DECODED,
				ScanUtils.getScanStateForFileName("test file name[DND].jpg_x"));
		assertSame(ScanState.SCANNED_DECODED,
				ScanUtils.getScanStateForFileName("test file name[D].jpg_x"));
	}

	public void testSetScanStateForFileName()
	{
		assertEquals("test file name.jpg", ScanUtils.setScanStateForFileName(
				"test file name.jpg", ScanState.NOT_SCANNED));
		assertEquals("test file name.jpg", ScanUtils.setScanStateForFileName(
				"test file name[D].jpg",
						ScanState.NOT_SCANNED));
		assertEquals("test file name[ABC].jpg",
				ScanUtils.setScanStateForFileName("test file name[ABC].jpg",
						ScanState.NOT_SCANNED));
		assertEquals("test file name[D].jpg",
				ScanUtils.setScanStateForFileName("test file name.jpg",
						ScanState.SCANNED_DECODED));
		assertEquals("test file name[DND].jpg",
				ScanUtils.setScanStateForFileName("test file name.jpg",
				ScanState.SCANNED_DETECTED_NOT_DECODED));
		assertEquals("test file name[ND].jpg",
				ScanUtils.setScanStateForFileName("test file name.jpg",
						ScanState.SCANNED_NOT_DETECTED));
		assertEquals("test file name[D].jpg",
				ScanUtils.setScanStateForFileName("test file name[D].jpg",
						ScanState.SCANNED_DECODED));
		assertEquals("test file name[DND].jpg",
				ScanUtils.setScanStateForFileName("test file name[D].jpg",
						ScanState.SCANNED_DETECTED_NOT_DECODED));
		assertEquals("test file name[ND].jpg",
				ScanUtils.setScanStateForFileName("test file name[D].jpg",
						ScanState.SCANNED_NOT_DETECTED));
		assertEquals("test file name[ABC][D].jpg",
				ScanUtils.setScanStateForFileName("test file name[ABC].jpg",
						ScanState.SCANNED_DECODED));
		assertEquals("test file name[ABC].jpg",
				ScanUtils.setScanStateForFileName("test file name[ABC][D].jpg",
						ScanState.NOT_SCANNED));
		assertEquals("test file name[ABC].jpg",
				ScanUtils.setScanStateForFileName("test file name[ABC].jpg",
						ScanState.NOT_SCANNED));
	}
}
