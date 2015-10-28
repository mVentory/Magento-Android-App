package com.mageventory.test.activity;

import android.net.Uri;
import android.test.InstrumentationTestCase;

import com.mageventory.activity.WebActivity;

public class WebActivityTest extends InstrumentationTestCase
{
	public void testBooksUrlPattern()
	{
		assertTrue("http://books.google.co.nz/books?id=O4QUAQAAIAAJ&q=how+things+work&dq=how+things+work&hl=en&sa=X&ei=fNRAVIvwCs_l8AWw94JY&ved=0CDoQ6AEwBA"
				.matches(WebActivity.WebUiFragment.BOOKS_URL_PATTERN));
		assertTrue("https://www.books.google.co.nz/books?id=O4QUAQAAIAAJ&q=how+things+work&dq=how+things+work&hl=en&sa=X&ei=fNRAVIvwCs_l8AWw94JY&ved=0CDoQ6AEwBA"
				.matches(WebActivity.WebUiFragment.BOOKS_URL_PATTERN));
		assertFalse("http://a.www.books.google.co.nz/books?id=O4QUAQAAIAAJ&q=how+things+work&dq=how+things+work&hl=en&sa=X&ei=fNRAVIvwCs_l8AWw94JY&ved=0CDoQ6AEwBA"
				.matches(WebActivity.WebUiFragment.BOOKS_URL_PATTERN));

		Uri uri = Uri
				.parse("http://books.google.co.nz/books?id=O4QUAQAAIAAJ&q=how+things+work&dq=how+things+work&hl=en&sa=X&ei=fNRAVIvwCs_l8AWw94JY&ved=0CDoQ6AEwBA");
		assertEquals("O4QUAQAAIAAJ", uri.getQueryParameter("id"));
	}
}
