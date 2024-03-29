
package com.mageventory.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * taken from http://stackoverflow.com/a/4688335/527759
 */
public class AspectRatioImageView extends ImageView
{
    Float aspectRatio;

    public AspectRatioImageView(Context context)
    {
        super(context);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs,
            int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        if (aspectRatio != null && aspectRatio > 0)
        {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = (int) (width / aspectRatio);
            setMeasuredDimension(width, height);
        } else
        {
            Drawable drawable = getDrawable();
            if (drawable != null)
            {
                int width = MeasureSpec.getSize(widthMeasureSpec);
                int diw = drawable.getIntrinsicWidth();
                if (diw > 0)
                {
                    int height = width * drawable.getIntrinsicHeight() / diw;
                    setMeasuredDimension(width, height);
                }
                else
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
            else
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public Float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(Float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }
}
