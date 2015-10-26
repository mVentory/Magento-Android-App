
package com.mageventory.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.mageventory.settings.Settings;
import com.mageventory.util.CommonUtils;
import com.mventory.R;

/**
 * The extension of the {@link FrameLayout} which can show touch indicator
 * 
 * @author Eugene Popovich
 */
public class TouchIndicatorView extends FrameLayout {
    /**
     * The last touch coordinates to draw the touch indicator
     */
    Point mLastTouchCoordinates;
    /**
     * The paint used to draw touch indicator
     */
    Paint mPaint;
    /**
     * The touch indicator circle radius
     */
    float mCircleRadius;
    /**
     * The flag indicating whether the touch indicator is enabled
     */
    boolean mEnabled;

    public TouchIndicatorView(Context context) {
        super(context);
        reinit();
    }

    public TouchIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        reinit();
    }

    public TouchIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        reinit();
    }

    public TouchIndicatorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        reinit();
    }

    /**
     * Reinitialize view, get the actual values from the application settings
     */
    public void reinit() {
        Settings settings = new Settings(getContext());
        reinit(settings.isTouchIndicatorEnabled(), settings.getTouchIndicatorLineWidth(),
                settings.getTouchIndicatorRadius());
    }

    /**
     * Reinitialize view using specified parameters
     * 
     * @param enabled whether the touch indicator is enabled
     * @param lineWidth the touch indicator line width
     * @param radius the touch indicator radius
     */
    public void reinit(boolean enabled, float lineWidth, float radius) {
        setWillNotDraw(false);
        mEnabled = enabled;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(CommonUtils.getColorResource(R.color.blue));
        mPaint.setStrokeWidth(lineWidth);

        mPaint.setStyle(Paint.Style.STROKE);
        mCircleRadius = CommonUtils.dipToPixels(radius);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        processEvent(event);
        return super.dispatchTouchEvent(event);
    }

    /**
     * Process the touch event
     * 
     * @param event
     * @return
     */
    private boolean processEvent(MotionEvent event) {
        if (mEnabled) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    // remember last touch coordinates
                    mLastTouchCoordinates = new Point((int) event.getX(), (int) event.getY());
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // forget last touch coordinates
                    mLastTouchCoordinates = null;
                    invalidate();
                    return true;
            }
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mEnabled && mLastTouchCoordinates != null) {
            // if touch indicator is enabled and last touch coordinates data is
            // available
            canvas.drawCircle(mLastTouchCoordinates.x, mLastTouchCoordinates.y, mCircleRadius,
                    mPaint);
        }
    }
}
