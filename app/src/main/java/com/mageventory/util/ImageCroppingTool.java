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

package com.mageventory.util;

import com.mventory.R;

import android.graphics.RectF;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class ImageCroppingTool {

    public boolean mCroppingMode;

    private FrameLayout mCroppingLayout;

    private View mTopCropView;
    private View mLeftCropView;
    private View mRightCropView;
    private View mBottomCropView;

    private int mTopLevelLayoutWidth, mTopLevelLayoutHeight;
    private FrameLayout mOverlayLayout;

    private ImagesLoader mImagesLoader;

    private Runnable mOverlayRunnable;

    private TextView mDecodeButton;

    public ImageCroppingTool(ImagesLoader imagesLoader)
    {
        mImagesLoader = imagesLoader;

        mOverlayRunnable = new Runnable() {

            @Override
            public void run() {
                showOverlay();
            }
        };
    }

    public void setDecodeButtonView(TextView decodeButton)
    {
        mDecodeButton = decodeButton;
    }

    public void orientationChange(FrameLayout topLevelLayout, int topLevelLayoutWidth,
            int topLevelLayoutHeight)
    {
        mOverlayLayout = (FrameLayout) topLevelLayout.findViewById(R.id.overlayLayout);
        mDecodeButton = (TextView) topLevelLayout.findViewById(R.id.decodeButton);

        mTopLevelLayoutWidth = topLevelLayoutWidth;
        mTopLevelLayoutHeight = topLevelLayoutHeight;

        mCroppingLayout = (FrameLayout) topLevelLayout.findViewById(R.id.croppingLayout);

        mTopCropView = (View) topLevelLayout.findViewById(R.id.topCropView);
        mLeftCropView = (View) topLevelLayout.findViewById(R.id.leftCropView);

        mRightCropView = (View) topLevelLayout.findViewById(R.id.rightCropView);
        mBottomCropView = (View) topLevelLayout.findViewById(R.id.bottomCropView);

    }

    public void showDecodeButton()
    {
        mDecodeButton.setVisibility(View.VISIBLE);
    }

    public void hideDecodeButton()
    {
        mDecodeButton.setVisibility(View.GONE);
    }

    public void bringCroppingLayoutToFront()
    {
        mCroppingLayout.bringToFront();
        mOverlayLayout.bringToFront();
        mDecodeButton.bringToFront();
    }

    public boolean isInsideCroppingRectangle(float x, float y)
    {
        if (mCroppingLayout == null)
            return false;

        if (mCroppingMode) {
            float leftEdge;
            float rightEdge;

            float topEdge;
            float bottomEdge;

            FrameLayout.LayoutParams topParams = (FrameLayout.LayoutParams) mTopCropView
                    .getLayoutParams();
            FrameLayout.LayoutParams bottomParams = (FrameLayout.LayoutParams) mBottomCropView
                    .getLayoutParams();

            topEdge = topParams.height;
            bottomEdge = bottomParams.topMargin;

            leftEdge = topParams.leftMargin;
            rightEdge = topParams.leftMargin + topParams.width;

            if (x < leftEdge || x > rightEdge || y < topEdge || y > bottomEdge) {
                return false;
            }
        }
        else
        {
            return false;
        }

        return true;
    }

    public void showOverlayDelayed()
    {
        mOverlayLayout.removeCallbacks(mOverlayRunnable);
        mOverlayLayout.postDelayed(mOverlayRunnable, 2000);
    }

    public void showOverlay()
    {
        if (mCroppingMode == false)
        {
            mOverlayLayout.setVisibility(View.VISIBLE);
        }
    }

    public void hideOverlay()
    {
        mOverlayLayout.removeCallbacks(mOverlayRunnable);
        mOverlayLayout.setVisibility(View.GONE);
    }

    public void enableCropping()
    {
        mCroppingLayout.setVisibility(View.VISIBLE);
        mCroppingLayout.bringToFront();
        mOverlayLayout.bringToFront();
        mDecodeButton.bringToFront();
        mCroppingMode = true;
        hideOverlay();
    }

    public void disableCropping()
    {
        mCroppingLayout.setVisibility(View.GONE);
        mCroppingMode = false;
        showOverlay();
    }

    public boolean isCroppingShown()
    {
        return mCroppingLayout.getVisibility() == View.VISIBLE;
    }

    public void hideCropping()
    {
        mCroppingLayout.setVisibility(View.GONE);
    }

    public void showCropping()
    {
        mCroppingLayout.setVisibility(View.VISIBLE);
    }

    public RectF getCropRectangle()
    {
        if (mCroppingLayout == null)
            return null;

        FrameLayout.LayoutParams topParams = (FrameLayout.LayoutParams) mTopCropView
                .getLayoutParams();
        FrameLayout.LayoutParams bottomParams = (FrameLayout.LayoutParams) mBottomCropView
                .getLayoutParams();

        return new RectF(topParams.leftMargin, topParams.height, topParams.leftMargin
                + topParams.width, bottomParams.topMargin);
    }

    public void repositionCroppingRectangle(RectF rect) {

        if (mCroppingLayout == null)
            return;

        RectF currentImgR = mImagesLoader.getCurrentImageRectF();

        if (currentImgR != null)
        {
            if (rect.bottom < currentImgR.top + 5)
            {
                rect.bottom = currentImgR.top + 5;
            }

            if (rect.top > currentImgR.bottom - 5)
            {
                rect.top = currentImgR.bottom - 5;
            }

            if (rect.right < currentImgR.left + 5)
            {
                rect.right = currentImgR.left + 5;
            }

            if (rect.left > currentImgR.right - 5)
            {
                rect.left = currentImgR.right - 5;
            }
        }

        FrameLayout.LayoutParams topParams = (FrameLayout.LayoutParams) mTopCropView
                .getLayoutParams();
        FrameLayout.LayoutParams bottomParams = (FrameLayout.LayoutParams) mBottomCropView
                .getLayoutParams();

        FrameLayout.LayoutParams leftParams = (FrameLayout.LayoutParams) mLeftCropView
                .getLayoutParams();
        FrameLayout.LayoutParams rightParams = (FrameLayout.LayoutParams) mRightCropView
                .getLayoutParams();

        leftParams.width = (int) rect.left;

        topParams.leftMargin = (int) rect.left;
        topParams.height = (int) rect.top;
        topParams.width = (int) rect.right - (int) rect.left;

        bottomParams.topMargin = (int) rect.bottom;
        bottomParams.leftMargin = (int) rect.left;
        bottomParams.height = mTopLevelLayoutHeight - (int) rect.bottom;
        bottomParams.width = (int) rect.right - (int) rect.left;

        rightParams.leftMargin = (int) rect.right;
        rightParams.width = mTopLevelLayoutWidth - (int) rect.right;

        mTopCropView.setLayoutParams(topParams);
        mBottomCropView.setLayoutParams(bottomParams);

        mLeftCropView.setLayoutParams(leftParams);
        mRightCropView.setLayoutParams(rightParams);
    }

}
