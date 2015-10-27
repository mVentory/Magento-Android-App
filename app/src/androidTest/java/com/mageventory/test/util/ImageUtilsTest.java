package com.mageventory.test.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.test.InstrumentationTestCase;

import com.mventory.test.R;
import com.mageventory.util.ImageUtils;

public class ImageUtilsTest extends InstrumentationTestCase
{
	public void testExtractImageUrls()
	{
		{
			String html = readRawTextFile(getInstrumentation().getContext(),
					R.raw.google_image_search_web_content);
			String[] urls = ImageUtils.extractImageUrls(html, null);
			assertNotNull(urls);
			assertTrue(urls.length > 0);
		}
		{
			String html = readRawTextFile(getInstrumentation().getContext(),
					R.raw.ricochet_co_nz_web_content);
			String[] urls = ImageUtils.extractImageUrls(html, null);
			assertNotNull(urls);
			assertTrue(urls.length > 0);
		}
		{
			String html = readRawTextFile(getInstrumentation().getContext(),
					R.raw.chocolat_nz_com_web_content);
			String[] urls = ImageUtils.extractImageUrls(html, null);
			assertNotNull(urls);
			assertTrue(urls.length > 0);
		}
		{
			String html = readRawTextFile(getInstrumentation().getContext(),
					R.raw.nyne_co_nz_web_content);
			String[] urls = ImageUtils.extractImageUrls(html, null);
			assertNotNull(urls);
			assertTrue(urls.length > 0);
		}
		{
			String html = readRawTextFile(getInstrumentation().getContext(),
					R.raw.ariannelingerie_com_web_content);
			String[] urls = ImageUtils
					.extractImageUrls(html,
							"http://www.ariannelingerie.com/shop/index.php/marilyn-chemise.html");
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

	public void testTranslateRect()
	{
		int width = 800;
		int height = 600;
		int top = 10;
		int left = 15;
		int bottom = 420;
		int right = 550;

		{
			Rect rect = new Rect(left, top, right, bottom);
			rect = ImageUtils.translateRect(rect, width, height, 0);
			assertEquals(rect.top, top);
			assertEquals(rect.left, left);
			assertEquals(rect.bottom, bottom);
			assertEquals(rect.right, right);
		}

		{
			Rect rect = new Rect(left, top, right, bottom);
			rect = ImageUtils.translateRect(rect, width, height, 180);
			assertEquals(rect.top, height - bottom);
			assertEquals(rect.left, width - right);
			assertEquals(rect.bottom, height - top);
			assertEquals(rect.right, width - left);
		}
		{
			Rect rect = new Rect(left, top, right, bottom);
			rect = ImageUtils.translateRect(rect, width, height, 90);
			assertEquals(rect.top, width - right);
			assertEquals(rect.left, top);
			assertEquals(rect.bottom, width - left);
			assertEquals(rect.right, bottom);
		}
		{
			Rect rect = new Rect(left, top, right, bottom);
			rect = ImageUtils.translateRect(rect, width, height, 270);
			assertEquals(rect.top, left);
			assertEquals(rect.left, height - bottom);
			assertEquals(rect.bottom, right);
			assertEquals(rect.right, height - top);
		}

	}

	public void testGetCropRectMultipliers()
	{
		{
			RectF displayRect = new RectF(40, 0, 760, 480);
			RectF res = ImageUtils.getCropRectMultipliers(displayRect, 800, 480);
			assertEquals(res.top, 0f);
			assertEquals(res.left, 0f);
			assertEquals(res.bottom, 1f);
			assertEquals(res.right, 1f);
		}
		{
			RectF displayRect = new RectF(0, 40, 800, 440);
			RectF res = ImageUtils.getCropRectMultipliers(displayRect, 800, 480);
			assertEquals(res.top, 0f);
			assertEquals(res.left, 0f);
			assertEquals(res.bottom, 1f);
			assertEquals(res.right, 1f);
		}
		{
			RectF displayRect = new RectF(-200, -100, 1800, 900);
			int precision = 100000;
			RectF res = ImageUtils.getCropRectMultipliers(displayRect, 800, 480);
			assertEquals(Math.round(res.top * precision),
					Math.round(100f / 1000 * precision));
			assertEquals(Math.round(res.left * precision),
					Math.round(200f / 2000 * precision));
			assertEquals(Math.round(res.bottom * precision),
					Math.round((1f - 0.42) * precision));
			assertEquals(Math.round(res.right * precision),
					Math.round((1f - 0.5) * precision));
		}
	}
}
