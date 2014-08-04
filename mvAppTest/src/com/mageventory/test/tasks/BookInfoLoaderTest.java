package com.mageventory.test.tasks;

import android.test.InstrumentationTestCase;

import com.mageventory.tasks.BookInfoLoader;

public class BookInfoLoaderTest extends InstrumentationTestCase
{
	public void testIsIsbnCode()
	{
		assertTrue(BookInfoLoader.isIsbnCode("9780495112402"));
		assertTrue(BookInfoLoader.isIsbnCode("978-1-84854-567-0"));
		assertFalse(BookInfoLoader.isIsbnCode("9780495112412"));
		assertFalse(BookInfoLoader.isIsbnCode("9770495112402"));
		assertTrue(BookInfoLoader.isIsbnCode("0495112402"));
		assertFalse(BookInfoLoader.isIsbnCode("0495112412"));
		assertFalse(BookInfoLoader.isIsbnCode("04951124021"));
		assertTrue(BookInfoLoader.isIsbnCode("020530902X"));
		assertTrue(BookInfoLoader.isIsbnCode("0-205-30902-X"));
		assertTrue(BookInfoLoader.isIsbnCode("ISBN-0-205-30902-X"));
		assertFalse(BookInfoLoader.isIsbnCode("020530903X"));
	}

	public void testIsIssnCode()
	{
		assertTrue(BookInfoLoader.isIssnCode("0378-5955"));
		assertFalse(BookInfoLoader.isIssnCode("0378-5953"));
		assertFalse(BookInfoLoader.isIssnCode("0378-5955-5"));
		assertTrue(BookInfoLoader.isIssnCode("9-770317-847001"));
		assertFalse(BookInfoLoader.isIssnCode("9780495112402"));
	}
}
