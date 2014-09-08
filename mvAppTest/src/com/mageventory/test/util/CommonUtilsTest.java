package com.mageventory.test.util;

import android.test.InstrumentationTestCase;

import com.mageventory.util.CommonUtils;

public class CommonUtilsTest extends InstrumentationTestCase
{
	public void testRemoveDuplicateWords()
	{
		assertEquals(CommonUtils.removeDuplicateWords("test test test"), "test");
		assertEquals(CommonUtils.removeDuplicateWords("Test teSt tEst test"),
				"Test");
		assertEquals(
				CommonUtils.removeDuplicateWords("Test, teSt   tEst. test x"),
				"Test x");
		assertEquals(
				CommonUtils
						.removeDuplicateWords("Test dup, teSt   tEst. test dup"),
				"Test dup");
		assertEquals(
				CommonUtils
						.removeDuplicateWords("Test dup, dup teSt  dup tEst. test dup dup2.dup"),
				"Test dup dup2");
	}
}
