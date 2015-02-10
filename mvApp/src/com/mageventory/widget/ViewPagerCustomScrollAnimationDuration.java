
package com.mageventory.widget;

import java.lang.reflect.Field;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import com.mventory.R;
import com.mageventory.util.CommonUtils;

/**
 * Solution taken from here http://stackoverflow.com/a/14179739/527759
 */
public class ViewPagerCustomScrollAnimationDuration extends ViewPager {
    private static final String TAG = ViewPagerCustomScrollAnimationDuration.class.getSimpleName();

    public ViewPagerCustomScrollAnimationDuration(Context context) {
        super(context);
        postInitViewPager(-1);
    }

    public ViewPagerCustomScrollAnimationDuration(Context context, AttributeSet attrs) {
        super(context, attrs);
        // support for setting animation via style attributes
        TypedArray viewAttrs = context.obtainStyledAttributes(attrs,
                R.styleable.ViewPagerCustomScrollAnimationDuration);

        int duration = viewAttrs.getInt(
                R.styleable.ViewPagerCustomScrollAnimationDuration_animationDuration, -1);

        viewAttrs.recycle();

        postInitViewPager(duration);
    }

    private ScrollerCustomDuration mScroller = null;

    /**
     * Override the Scroller instance with our own class so we can change the
     * duration
     */
    private void postInitViewPager(int animationDuration) {
        try {
            Field scroller = ViewPager.class.getDeclaredField("mScroller");
            scroller.setAccessible(true);
            Field interpolator = ViewPager.class.getDeclaredField("sInterpolator");
            interpolator.setAccessible(true);

            mScroller = new ScrollerCustomDuration(getContext(),
                    (Interpolator) interpolator.get(null));
            if (animationDuration > 0) {
                mScroller.setScrollDuration(animationDuration);
            }
            scroller.set(this, mScroller);
        } catch (Exception e) {
            CommonUtils.error(TAG, e);
        }
    }

    /**
     * Set the scroll animation duration in milliseconds
     */
    public void setScrollDuration(int scrollDuration) {
        mScroller.setScrollDuration(scrollDuration);
    }

    public static class ScrollerCustomDuration extends Scroller {

        private int mScrollDuration = -1;

        public ScrollerCustomDuration(Context context) {
            super(context);
        }

        public ScrollerCustomDuration(Context context, Interpolator interpolator) {
            super(context, interpolator);
        }

        @SuppressLint("NewApi")
        public ScrollerCustomDuration(Context context, Interpolator interpolator, boolean flywheel) {
            super(context, interpolator, flywheel);
        }

        /**
         * Set the scroll animation duration in milliseconds
         */
        public void setScrollDuration(int scrollDuration) {
            mScrollDuration = scrollDuration;
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, mScrollDuration > 0 ? mScrollDuration
                    : duration);
        }

    }

}
