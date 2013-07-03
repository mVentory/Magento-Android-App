package com.mageventory.test;

import android.graphics.Rect;
import android.test.InstrumentationTestCase;

import com.mageventory.util.ImagesLoader;

public class ImagesLoaderTest extends InstrumentationTestCase
{
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
			rect = ImagesLoader.translateRect(rect, width, height, 0);
			assertEquals(rect.top, top);
			assertEquals(rect.left, left);
			assertEquals(rect.bottom, bottom);
			assertEquals(rect.right, right);
		}

		{
			Rect rect = new Rect(left, top, right, bottom);
			rect = ImagesLoader.translateRect(rect, width, height, 180);
			assertEquals(rect.top, height - bottom);
			assertEquals(rect.left, width - right);
			assertEquals(rect.bottom, height - top);
			assertEquals(rect.right, width - left);
		}
		{
			Rect rect = new Rect(left, top, right, bottom);
			rect = ImagesLoader.translateRect(rect, width, height, 90);
			assertEquals(rect.top, width - right);
			assertEquals(rect.left, top);
			assertEquals(rect.bottom, width - left);
			assertEquals(rect.right, bottom);
		}
		{
			Rect rect = new Rect(left, top, right, bottom);
			rect = ImagesLoader.translateRect(rect, width, height, 270);
			assertEquals(rect.top, left);
			assertEquals(rect.left, height - bottom);
			assertEquals(rect.bottom, right);
			assertEquals(rect.right, height - top);
		}

	}
}
