package com.mageventory.util;

import com.mageventory.R;

import android.graphics.RectF;
import android.view.View;
import android.widget.FrameLayout;

public class ImageCroppingTool {

	public boolean mCroppingMode;

	private FrameLayout mCroppingLayout;

	private View mTopCropView;
	private View mLeftCropView;
	private View mRightCropView;
	private View mBottomCropView;
	
	private int mTopLevelLayoutWidth, mTopLevelLayoutHeight;
	
	private ImagesLoader mImagesLoader;
	
	public ImageCroppingTool(ImagesLoader imagesLoader)
	{
		mImagesLoader = imagesLoader;
	}
	
	public void orientationChange(FrameLayout topLevelLayout, int topLevelLayoutWidth, int topLevelLayoutHeight)
	{
		mTopLevelLayoutWidth = topLevelLayoutWidth;
		mTopLevelLayoutHeight = topLevelLayoutHeight;
		
		mCroppingLayout = (FrameLayout) topLevelLayout.findViewById(R.id.croppingLayout);

		mTopCropView = (View) topLevelLayout.findViewById(R.id.topCropView);
		mLeftCropView = (View) topLevelLayout.findViewById(R.id.leftCropView);

		mRightCropView = (View) topLevelLayout.findViewById(R.id.rightCropView);
		mBottomCropView = (View) topLevelLayout.findViewById(R.id.bottomCropView);
	}
	
	public void bringCroppingLayoutToFront()
	{
		mCroppingLayout.bringToFront();
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

			FrameLayout.LayoutParams topParams = (FrameLayout.LayoutParams) mTopCropView.getLayoutParams();
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
	
	public void enableCropping()
	{
		mCroppingLayout.setVisibility(View.VISIBLE);
		mCroppingLayout.bringToFront();
		mCroppingMode = true;
	}
	
	public void disableCropping()
	{
		mCroppingLayout.setVisibility(View.GONE);
		mCroppingMode = false;
	}
	
	public RectF getCropRectangle()
	{
		if (mCroppingLayout == null)
			return null;
		
		FrameLayout.LayoutParams topParams = (FrameLayout.LayoutParams) mTopCropView.getLayoutParams();
		FrameLayout.LayoutParams bottomParams = (FrameLayout.LayoutParams) mBottomCropView
				.getLayoutParams();

		return new RectF(topParams.leftMargin, topParams.height, topParams.leftMargin + topParams.width, bottomParams.topMargin);
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
		
		FrameLayout.LayoutParams topParams = (FrameLayout.LayoutParams) mTopCropView.getLayoutParams();
		FrameLayout.LayoutParams bottomParams = (FrameLayout.LayoutParams) mBottomCropView.getLayoutParams();

		FrameLayout.LayoutParams leftParams = (FrameLayout.LayoutParams) mLeftCropView.getLayoutParams();
		FrameLayout.LayoutParams rightParams = (FrameLayout.LayoutParams) mRightCropView.getLayoutParams();

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
