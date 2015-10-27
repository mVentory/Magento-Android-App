package com.mageventory.test.util;

import android.test.InstrumentationTestCase;

import com.mageventory.util.CommonUtils;
import com.mageventory.util.run.CallableWithParameterAndResult;

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

    public void testReplaceCodesWithValues() {
        String formattedString = "isbn issn hello-world issn_meta (issn_meta2) isbn_meta";
        CallableWithParameterAndResult<String, String> getValueForCode = new CallableWithParameterAndResult<String, String>() {

            @Override
            public String call(String code) {
                if (code.equals("isbn")) {
                    return "ISBN";
                } else if (code.equals("issn")) {
                    return "ISSN";
                } else if (code.equals("isbn_meta")) {
                    return "5";
                } else if (code.equals("issn_meta")) {
                    return "";
                } else if (code.equals("issn_meta2")) {
                    return "";
                }
                return code;
            }
        };
        assertEquals("ISBN ISSN hello-world  () 5",
                CommonUtils.replaceCodesWithValues(formattedString, getValueForCode));
    }
}
