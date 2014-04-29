package com.mageventory.test.tasks;

import android.test.InstrumentationTestCase;

import com.mageventory.tasks.BookInfoLoader;

public class BookInfoLoaderTest extends InstrumentationTestCase
{
	public void testIsIsbnCode()
	{
		assertTrue(BookInfoLoader.isIsbnCode("9780495112402"));
		assertTrue(BookInfoLoader.isIsbnCode("9790495112402"));
		assertFalse(BookInfoLoader.isIsbnCode("9770495112402"));
		assertTrue(BookInfoLoader.isIsbnCode("0495112402"));
		assertFalse(BookInfoLoader.isIsbnCode("04951124021"));
	}
}
