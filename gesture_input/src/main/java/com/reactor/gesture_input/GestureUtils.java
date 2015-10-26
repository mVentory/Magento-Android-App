/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/

package com.reactor.gesture_input;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.gesture.Gesture;
import android.gesture.GestureLibrary;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

/**
 * Basic utils for gestures
 * 
 * @author Eugene Popovich
 */
public class GestureUtils {
    private static final boolean BITMAP_RENDERING_ANTIALIAS = true;
    private static final boolean BITMAP_RENDERING_DITHER = true;
    private static final int BITMAP_RENDERING_WIDTH = 2;

    static class NamedGesture {
        String name;
        Gesture gesture;
    }

    public static abstract class AbstractGesturesAdapter extends ArrayAdapter<NamedGesture> {
        protected final LayoutInflater mInflater;
        protected final Map<Long, Drawable> mThumbnails = Collections
                .synchronizedMap(new HashMap<Long, Drawable>());

        public AbstractGesturesAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        void addBitmap(Long id, Bitmap bitmap) {
            mThumbnails.put(id, new BitmapDrawable(getContext().getResources(), bitmap));
        }
    }

    public static abstract class AbstractGesturesLoadTask extends
            AsyncTask<Void, NamedGesture, Integer> {
        public static final int STATUS_SUCCESS = 0;
        public static final int STATUS_CANCELLED = 1;
        public static final int STATUS_NOT_LOADED = 3;

        protected int mThumbnailSize;
        protected int mThumbnailInset;
        protected int mPathColor;
        protected int mStartPointColor;

        public AbstractGesturesLoadTask() {
            super();
            initParams();
        }

        /**
         * don't forget to call initParams(int thumbnailSize, int
         * thumbnailInset, int pathColor, int startPointColor) from there
         */
        abstract void initParams();

        public void initParams(int thumbnailSize, int thumbnailInset, int pathColor,
                int startPointColor)
        {
            this.mThumbnailSize = thumbnailSize;
            this.mThumbnailInset = thumbnailInset;
            this.mPathColor = pathColor;
            this.mStartPointColor = startPointColor;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (isCancelled())
                return STATUS_CANCELLED;

            final GestureLibrary store = getGestureLibrary();

            if (store.load()) {
                for (String name : store.getGestureEntries()) {
                    if (isCancelled())
                        break;

                    for (Gesture gesture : store.getGestures(name)) {
                        final Bitmap bitmap = toBitmap(gesture, mThumbnailSize,
                                mThumbnailSize,
                                mThumbnailInset,
                                mPathColor,
                                mStartPointColor);
                        final NamedGesture namedGesture = new NamedGesture();
                        namedGesture.gesture = gesture;
                        namedGesture.name = name;

                        addBitmap(namedGesture.gesture.getID(), bitmap);
                        publishProgress(namedGesture);
                    }
                }

                return STATUS_SUCCESS;
            }

            return STATUS_NOT_LOADED;
        }

        abstract GestureLibrary getGestureLibrary();

        abstract void addBitmap(Long id, Bitmap bitmap);
    }

    /**
     * Creates a bitmap of the gesture with a transparent background.
     * 
     * @param width
     * @param height
     * @param inset
     * @param color
     * @param startPointColor the color of the starting point
     * @return the bitmap
     */
    public static Bitmap toBitmap(Gesture gesture, int width, int height, int inset, int color,
            int startPointColor) {
        final Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);

        final Paint paint = new Paint();
        paint.setAntiAlias(BITMAP_RENDERING_ANTIALIAS);
        paint.setDither(BITMAP_RENDERING_DITHER);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(BITMAP_RENDERING_WIDTH);

        final Path path = gesture.toPath();
        final RectF bounds = new RectF();
        path.computeBounds(bounds, true);

        final float sx = (width - 2 * inset) / bounds.width();
        final float sy = (height - 2 * inset) / bounds.height();
        final float scale = sx > sy ? sy : sx;
        paint.setStrokeWidth(2.0f / scale);

        path.offset(-bounds.left + (width - bounds.width() * scale) / 2.0f,
                -bounds.top + (height - bounds.height() * scale) / 2.0f);

        canvas.translate(inset, inset);
        canvas.scale(scale, scale);

        canvas.drawPath(path, paint);
        PathMeasure pm = new PathMeasure(path, false);
        float aCoordinates[] = {
                0f, 0f
        };

        // get point from the start
        pm.getPosTan(0.0f, aCoordinates, null);
        paint.setColor(startPointColor);
        paint.setStyle(Paint.Style.FILL);
        // draw start point
        canvas.drawCircle(aCoordinates[0], aCoordinates[1], 3.0f / scale, paint);
        return bitmap;
    }

    public static final Comparator<NamedGesture> defaultNamedGestureSorter = new Comparator<NamedGesture>() {
        final String[] preferredOrder = new String[] {
                "/", "-", ".", " ", GestureInputActivity.BACKSPACE_GESTURE_NAME
        };
        int firstLetter = 'a';
        int lastLetter = 'z';
        int lastLetterIndexOffset = lastLetter - firstLetter + 1;
        int firstDigit = '0';
        int lastDigit = '9';
        int lastDigitIndexOffset = lastLetterIndexOffset + lastDigit - firstDigit + 1;

        @Override
        public int compare(NamedGesture object1, NamedGesture object2) {
            int rIndex1 = getReservedIndex(object1.name);
            int rIndex2 = getReservedIndex(object2.name);
            if (rIndex1 == Integer.MAX_VALUE && rIndex2 == Integer.MAX_VALUE)
            {
                return object1.name.compareToIgnoreCase(object2.name);
            } else
            {
                return rIndex1 - rIndex2;
            }
        }

        /**
         * Get index if name is from list of predefined objects order
         * 
         * @param name
         * @return
         */
        public int getReservedIndex(String name)
        {
            int result = Integer.MAX_VALUE;
            name = name.toLowerCase();
            if (name.length() == 1)
            {
                char ch = name.charAt(0);
                if (ch >= firstLetter && ch <= lastLetter)
                {
                    result = ch - firstLetter;
                }
                if (ch >= firstDigit && ch <= lastDigit)
                {
                    result = ch - firstDigit + lastLetterIndexOffset;
                }
            }
            if (result == Integer.MAX_VALUE)
            {
                for (int i = 0, size = preferredOrder.length; i < size; i++)
                {
                    if(name.equals(preferredOrder[i]))
                    {
                        result = i + lastDigitIndexOffset;
                    }
                }
            }
            return result;
        }
    };
}
