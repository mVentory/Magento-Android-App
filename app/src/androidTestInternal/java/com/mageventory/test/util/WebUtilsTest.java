package com.mageventory.test.util;

import android.test.InstrumentationTestCase;

import com.mageventory.util.WebUtils;

public class WebUtilsTest extends InstrumentationTestCase
{
	public void testGetTopLevelDomainFromHost()
	{
		assertEquals("test.com",
				WebUtils.getTopLevelDomainFromHost("subdomain.test.com"));
		assertEquals("test.com",
				WebUtils.getTopLevelDomainFromHost("www.test.com"));
		assertEquals("test.com", WebUtils.getTopLevelDomainFromHost("test.com"));
		assertEquals("test.com",
				WebUtils.getTopLevelDomainFromHost("www.subdomain.test.com"));
		assertEquals("test.co.uk",
				WebUtils.getTopLevelDomainFromHost("www.test.co.uk"));
		assertEquals(
				"test.co.uk",
				WebUtils.getTopLevelDomainFromHost("www.some.sub.domain.test.co.uk"));
		assertEquals(
				"com.co.uk",
				WebUtils.getTopLevelDomainFromHost("www.some.sub.domain.com.co.uk"));
	}
}
