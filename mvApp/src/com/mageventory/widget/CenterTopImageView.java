
package com.mageventory.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CenterTopImageView extends ImageView {

    public CenterTopImageView(Context context) {
        super(context);
        setup();
    }

    public CenterTopImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public CenterTopImageView(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        setup();
    }

    private void setup() {
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected boolean setFrame(int frameLeft, int frameTop, int frameRight, int frameBottom) {
        float frameWidth = frameRight - frameLeft;
        float frameHeight = frameBottom - frameTop;

        Drawable drawable = getDrawable();
        float originalImageWidth = drawable == null ? 1 : (float) drawable.getIntrinsicWidth();
        float originalImageHeight = drawable == null ? 1 : (float) drawable.getIntrinsicHeight();

        float usedScaleFactor = 1;

        if ((frameWidth > originalImageWidth) || (frameHeight > originalImageHeight)) {
            // If frame is bigger than image
            // => Crop it, keep aspect ratio and position it at the bottom and
            // center horizontally

            float fitHorizontallyScaleFactor = frameWidth / originalImageWidth;
            float fitVerticallyScaleFactor = frameHeight / originalImageHeight;

            usedScaleFactor = Math.min(fitHorizontallyScaleFactor, fitVerticallyScaleFactor);
        }

        float newImageWidth = originalImageWidth * usedScaleFactor;
        float newImageHeight = originalImageHeight * usedScaleFactor;

        Matrix matrix = getImageMatrix();
        matrix.setScale(usedScaleFactor, usedScaleFactor, 0, 0); // Replaces the
                                                                 // old matrix
                                                                 // completly
        matrix.postTranslate((frameWidth - newImageWidth) / 2, 0);
        setImageMatrix(matrix);
        return super.setFrame(frameLeft, frameTop, frameRight, frameBottom);
    }

}
