package com.mageventory.test.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.mageventory.test.R;
import com.mageventory.util.ImageUtils;

public class ImageUtilsTest extends InstrumentationTestCase
{
	public void testExtractImageUrls()
	{
		{
			String html = readRawTextFile(getInstrumentation().getContext(),
					R.raw.google_image_search_web_content);
			String[] urls = ImageUtils.extractImageUrls(html);
			assertNotNull(urls);
			assertTrue(urls.length > 0);
		}
		{
			String html = readRawTextFile(getInstrumentation().getContext(),
					R.raw.ricochet_co_nz_web_content);
			String[] urls = ImageUtils.extractImageUrls(html);
			assertNotNull(urls);
			assertTrue(urls.length > 0);
		}
		{
			String html = readRawTextFile(getInstrumentation().getContext(),
					R.raw.chocolat_nz_com_web_content);
			String[] urls = ImageUtils.extractImageUrls(html);
			assertNotNull(urls);
			assertTrue(urls.length > 0);
		}
		{
			String html = readRawTextFile(getInstrumentation().getContext(),
					R.raw.nyne_co_nz_web_content);
			String[] urls = ImageUtils.extractImageUrls(html);
			assertNotNull(urls);
			assertTrue(urls.length > 0);
		}
	}

	public static String readRawTextFile(Context ctx, int resId)
	{
		InputStream inputStream = ctx.getResources().openRawResource(resId);

		InputStreamReader inputreader = new InputStreamReader(inputStream);
		BufferedReader buffreader = new BufferedReader(inputreader);
		String line;
		StringBuilder text = new StringBuilder();

		try
		{
			while ((line = buffreader.readLine()) != null)
			{
				text.append(line);
				text.append('\n');
			}
		} catch (IOException e)
		{
			return null;
		}
		return text.toString();
	}
}
