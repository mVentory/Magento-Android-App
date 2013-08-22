package com.mageventory.test.model.util;

import android.test.InstrumentationTestCase;

import com.mageventory.model.util.ProductUtils;
import com.mageventory.model.util.ProductUtils.PricesInformation;

public class ProductUtilsTest extends InstrumentationTestCase
{
	public void testIsValidPricesString()
	{
		assertTrue(ProductUtils.isValidPricesString("123.4"));
		assertTrue(ProductUtils.isValidPricesString("123"));
		assertTrue(ProductUtils.isValidPricesString("123.4/"));
		assertTrue(ProductUtils.isValidPricesString("123.4/2"));
		assertTrue(ProductUtils.isValidPricesString("123.4/2.2"));
		assertTrue(ProductUtils.isValidPricesString("123/2.2"));
		assertTrue(ProductUtils.isValidPricesString("123/2"));
		assertFalse(ProductUtils.isValidPricesString("123.1.1"));
		assertFalse(ProductUtils.isValidPricesString("123 "));
		assertFalse(ProductUtils.isValidPricesString("123/2.2.2"));
		assertFalse(ProductUtils.isValidPricesString("123/2/2"));
	}

	public void testGetPricesInformation()
	{
		PricesInformation pi;
		pi = ProductUtils.getPricesInformation("123.4");
		assertEquals(pi.regularPrice, 123.4d);
		assertNull(pi.specialPrice);

		pi = ProductUtils.getPricesInformation("123.4/");
		assertEquals(pi.regularPrice, 123.4d);
		assertNull(pi.specialPrice);

		pi = ProductUtils.getPricesInformation("123.4/2");
		assertEquals(pi.regularPrice, 123.4d);
		assertEquals(pi.specialPrice, 2d);

		pi = ProductUtils.getPricesInformation("123.4/a");
		assertNull(pi);

		pi = ProductUtils.getPricesInformation("123.4/2.2");
		assertEquals(pi.regularPrice, 123.4d);
		assertEquals(pi.specialPrice, 2.2d);

	}
}
