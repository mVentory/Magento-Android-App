package com.mageventory.activity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector.OnGestureListener;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.mageventory.R;
import com.mageventory.ZXingCodeScanner;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.components.ImagesLoader;
import com.mageventory.components.ImagesLoader.CachedImage;
import com.mageventory.settings.Settings;

public class ExternalImagesEditActivity extends BaseActivity {
	
	private static final int ANIMATION_LENGTH_MILLIS = 100;
	private static final float FLING_DETECTION_THRESHOLD = 0.4f; // screen
																// diagonals per
	private ImagesLoader mImagesLoader;																// second

	private FrameLayout mLeftImage;
	private FrameLayout mCenterImage;
	private FrameLayout mRightImage;

	private FrameLayout mTopLevelLayout;
	
	private FrameLayout mCroppingLayout;
	
	private View mTopCropView;
	private View mLeftCropView;
	private View mRightCropView;
	private View mBottomCropView;

	private int mTopLevelLayoutWidth, mTopLevelLayoutHeight;
	private float mTopLevelLayoutDiagonal;

	private GestureDetector mGestureDetector;
	private float mCurrentImageX = 0;
	private float mCurrentImageY = 0;

	private boolean mAnimationRunning = false;
	private int mCurrentImageIndex = 0;

	private boolean mHorizontalScrolling;
	private boolean mScrollingInProgress;
	private Settings mSettings;
	private boolean mCroppingMode;

	private void setCurrentImageIndex(int index)
	{
		mCurrentImageIndex = index;
		mImagesLoader.setState(index, mLeftImage, mCenterImage, mRightImage);
	}
	
	private ImageView imageView(FrameLayout layout)
	{
		return (ImageView)layout.findViewById(R.id.image);
	}

	private void repositionImages() {
		FrameLayout.LayoutParams paramsCenter = (FrameLayout.LayoutParams) mCenterImage.getLayoutParams();
		paramsCenter.width = mTopLevelLayoutWidth;
		paramsCenter.height = mTopLevelLayoutHeight;
		if (mHorizontalScrolling == false) {
			paramsCenter.topMargin = (int) mCurrentImageY;
		} else {
			paramsCenter.topMargin = 0;
		}

		paramsCenter.leftMargin = (int) mCurrentImageX;
		mCenterImage.setLayoutParams(paramsCenter);

		FrameLayout.LayoutParams paramsLeft = (FrameLayout.LayoutParams) mLeftImage.getLayoutParams();
		paramsLeft.width = mTopLevelLayoutWidth;
		paramsLeft.height = mTopLevelLayoutHeight;
		paramsLeft.leftMargin = 0;
		paramsLeft.topMargin = (int) (mCurrentImageY + mTopLevelLayoutHeight);
		mLeftImage.setLayoutParams(paramsLeft);

		FrameLayout.LayoutParams paramsRight = (FrameLayout.LayoutParams) mRightImage.getLayoutParams();
		paramsRight.width = mTopLevelLayoutWidth;
		paramsRight.height = mTopLevelLayoutHeight;
		paramsRight.leftMargin = 0;
		paramsRight.topMargin = (int) (mCurrentImageY - mTopLevelLayoutHeight);
		mRightImage.setLayoutParams(paramsRight);

		mCenterImage.bringToFront();
	}

	private void recreateContentView() {
		setContentView(R.layout.external_images_edit);
		mTopLevelLayout = (FrameLayout) findViewById(R.id.topLevelLayout);
		mCroppingLayout = (FrameLayout) findViewById(R.id.croppingLayout);

		mTopCropView = (View) findViewById(R.id.topCropView);
		mLeftCropView = (View) findViewById(R.id.leftCropView);
		
		mRightCropView = (View) findViewById(R.id.rightCropView);
		mBottomCropView = (View) findViewById(R.id.bottomCropView);
		
		//registerForContextMenu(mTopLevelLayout);
		
		ViewTreeObserver viewTreeObserver = mTopLevelLayout.getViewTreeObserver();

		viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				if (mTopLevelLayoutWidth != mTopLevelLayout.getWidth()
						&& mTopLevelLayoutHeight != mTopLevelLayout.getHeight()) {
					mTopLevelLayoutWidth = mTopLevelLayout.getWidth();
					mTopLevelLayoutHeight = mTopLevelLayout.getHeight();

					mTopLevelLayoutDiagonal = (float) Math.sqrt(mTopLevelLayoutWidth * mTopLevelLayoutWidth
							+ mTopLevelLayoutHeight * mTopLevelLayoutHeight);

					mCurrentImageX = 0;
					mCurrentImageY = 0;

					float leftImageMatrixArray[] = new float[9];
					imageView(mLeftImage).getImageMatrix().getValues(leftImageMatrixArray);

					float rightImageMatrixArray[] = new float[9];
					imageView(mRightImage).getImageMatrix().getValues(rightImageMatrixArray);

					repositionImages();
				}
			}
		});

		mGestureDetector = new GestureDetector(new OnGestureListener() {

			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				return false;
			}

			@Override
			public void onShowPress(MotionEvent e) {

			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

				if (mScrollingInProgress == false) {
					mScrollingInProgress = true;

					if (Math.abs(distanceX) > Math.abs(distanceY)) {
						mHorizontalScrolling = true;
					} else {
						mHorizontalScrolling = false;
					}
				}

				if (mHorizontalScrolling) {
					mCurrentImageX -= distanceX;
					mCurrentImageY += distanceX;
				} else {
					mCurrentImageY -= distanceY;
				}

				if (mCurrentImageX < -mTopLevelLayoutWidth / 2.0) {
					mCurrentImageX = -mTopLevelLayoutWidth / 2.0f;
				}

				if (mCurrentImageX > mTopLevelLayoutWidth / 2.0) {
					mCurrentImageX = mTopLevelLayoutWidth / 2.0f;
				}

				if (mCurrentImageY < -mTopLevelLayoutHeight / 2.0) {
					mCurrentImageY = -mTopLevelLayoutHeight / 2.0f;
				}

				if (mCurrentImageY > mTopLevelLayoutHeight / 2.0) {
					mCurrentImageY = mTopLevelLayoutHeight / 2.0f;
				}

				repositionImages();
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e) {
				if (true)
				return;
				Drawable d = imageView(mCenterImage).getDrawable();

				if (d != null) {
					Bitmap bitmap = ((BitmapDrawable) d).getBitmap();

					ZXingCodeScanner multiDetector = new ZXingCodeScanner();
					String code = multiDetector.decode(bitmap);

					if (code == null) {
						//do nothing
					} else {
						String[] urlData = code.split("/");
						String sku = urlData[urlData.length - 1];

						mSettings.setCurrentSKU(sku);
						
						/* Imitate down fling */
						this.onFling(null, null, 0, mTopLevelLayoutDiagonal*(FLING_DETECTION_THRESHOLD+1));
					}
				}
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

				if (mScrollingInProgress == false) {
					mScrollingInProgress = true;

					if (Math.abs(velocityX) > Math.abs(velocityY)) {
						mHorizontalScrolling = true;
					} else {
						mHorizontalScrolling = false;
					}
				}
				
				if (mHorizontalScrolling == false
						&& ((Math.abs(velocityY) / mTopLevelLayoutDiagonal) > FLING_DETECTION_THRESHOLD))
				{
					if (velocityY < 0 && mImagesLoader.canSwitchLeft()) {
						Animation centerAnimation = new TranslateAnimation(0, 0, 0, -mTopLevelLayoutHeight - mCurrentImageY);
						centerAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						centerAnimation.setFillEnabled(true);

						centerAnimation.setAnimationListener(new AnimationListener() {

							@Override
							public void onAnimationStart(Animation animation) {
							}

							@Override
							public void onAnimationRepeat(Animation animation) {
							}

							@Override
							public void onAnimationEnd(Animation animation) {
								FrameLayout tmpVar = mLeftImage;
								mLeftImage = mRightImage;
								mRightImage = mCenterImage;
								mCenterImage = tmpVar;
								
								setCurrentImageIndex(mCurrentImageIndex-1);
								
								mCurrentImageX = 0;
								mCurrentImageY = 0;
								repositionImages();
								mAnimationRunning = false;
							}
						});

						mCenterImage.startAnimation(centerAnimation);
						mAnimationRunning = true;

						Animation leftAnimation = new TranslateAnimation(0, 0, 0, -mTopLevelLayoutHeight - mCurrentImageY);
						leftAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						leftAnimation.setFillEnabled(true);
						mLeftImage.startAnimation(leftAnimation);

						Animation rightAnimation = new TranslateAnimation(0, 0, 0, -mTopLevelLayoutHeight - mCurrentImageY);
						rightAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						rightAnimation.setFillEnabled(true);
						mRightImage.startAnimation(rightAnimation);
						
					} else if (velocityY > 0 && mImagesLoader.canSwitchRight()) {
						Animation centerAnimation = new TranslateAnimation(0, 0, 0, mTopLevelLayoutHeight - mCurrentImageY);
						centerAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						centerAnimation.setFillEnabled(true);

						centerAnimation.setAnimationListener(new AnimationListener() {

							@Override
							public void onAnimationStart(Animation animation) {
							}

							@Override
							public void onAnimationRepeat(Animation animation) {
							}

							@Override
							public void onAnimationEnd(Animation animation) {
								//mImagesLoader.queueImage(mCurrentImageIndex, mSettings.getCurrentSKU());
								//mCurrentImageIndex--;
								
								FrameLayout tmpVar = mLeftImage;
								mLeftImage = mCenterImage;
								mCenterImage = mRightImage;
								mRightImage = tmpVar;
								
								setCurrentImageIndex(mCurrentImageIndex+1);
								
								mCurrentImageX = 0;
								mCurrentImageY = 0;
								repositionImages();
								mAnimationRunning = false;
							}
						});
						mCenterImage.startAnimation(centerAnimation);
						mAnimationRunning = true;

						Animation leftAnimation = new TranslateAnimation(0, 0, 0, mTopLevelLayoutHeight - mCurrentImageY);
						leftAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						leftAnimation.setFillEnabled(true);
						mLeftImage.startAnimation(leftAnimation);

						Animation rightAnimation = new TranslateAnimation(0, 0, 0, mTopLevelLayoutHeight - mCurrentImageY);
						rightAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						rightAnimation.setFillEnabled(true);
						mRightImage.startAnimation(rightAnimation);
					} else {
						return false;
					}
				} else if (mHorizontalScrolling
						&& ((Math.abs(velocityX) / mTopLevelLayoutDiagonal) > FLING_DETECTION_THRESHOLD)) {
					if (velocityX < 0 && mImagesLoader.canSwitchRight()) {
						Animation centerAnimation = new TranslateAnimation(0, -mTopLevelLayoutWidth - mCurrentImageX, 0, 0);
						centerAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						centerAnimation.setFillEnabled(true);

						centerAnimation.setAnimationListener(new AnimationListener() {

							@Override
							public void onAnimationStart(Animation animation) {
							}

							@Override
							public void onAnimationRepeat(Animation animation) {
							}

							@Override
							public void onAnimationEnd(Animation animation) {
								FrameLayout tmpVar = mLeftImage;
								mLeftImage = mCenterImage;
								mCenterImage = mRightImage;
								mRightImage = tmpVar;

								setCurrentImageIndex(mCurrentImageIndex+1);
								
								mCurrentImageX = 0;
								mCurrentImageY = 0;
								repositionImages();
								mAnimationRunning = false;
							}
						});
						mCenterImage.startAnimation(centerAnimation);
						mAnimationRunning = true;

						Animation leftAnimation = new TranslateAnimation(0, 0, 0, mTopLevelLayoutHeight - mCurrentImageY);
						leftAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						leftAnimation.setFillEnabled(true);
						mLeftImage.startAnimation(leftAnimation);

						Animation rightAnimation = new TranslateAnimation(0, 0, 0, mTopLevelLayoutHeight - mCurrentImageY);
						rightAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						rightAnimation.setFillEnabled(true);
						mRightImage.startAnimation(rightAnimation);
					} else {
						return false;
					}
				} else {
					return false;
				}

				return true;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				return false;
			}
		});
		
		/*mTopLevelLayout.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				openContextMenu(mTopLevelLayout);
				return false;
			}
		});*/
		
		mTopLevelLayout.setOnTouchListener(new OnTouchListener() {

			int lastMoveX;
			int lastMoveY;
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				if (mAnimationRunning)
					return false;

				if (mCroppingMode == false && event.getPointerCount() > 1)
				{
					mCroppingLayout.setVisibility(View.VISIBLE);
					mCroppingLayout.bringToFront();
					mCroppingMode = true;
					cancelScrolling();
					mScrollingInProgress = false;
				}

				if (mCroppingMode == false)
				{
					boolean consumed = mGestureDetector.onTouchEvent(event);

					if (event.getAction() == MotionEvent.ACTION_UP) {
						mScrollingInProgress = false;
					}
					
					if (!consumed && event.getAction() == MotionEvent.ACTION_UP) {
						cancelScrolling();
					}
				}
				else if (event.getPointerCount() > 1)
				{
					lastMoveX = -1;
					lastMoveY = -1;
					
					float leftEdge;
					float rightEdge;
					
					float topEdge;
					float bottomEdge;

					leftEdge = event.getX(0)<event.getX(1)?event.getX(0):event.getX(1);
					rightEdge = event.getX(0)>event.getX(1)?event.getX(0):event.getX(1) + 1;
					
					topEdge = event.getY(0)<event.getY(1)?event.getY(0):event.getY(1);
					bottomEdge = event.getY(0)>event.getY(1)?event.getY(0):event.getY(1) + 1;
					
					repositionCroppingRectangle(leftEdge, rightEdge, topEdge, bottomEdge);
				}
				else if (event.getPointerCount() == 1)
				{
					if (event.getAction()!=event.ACTION_MOVE)
					{
						lastMoveX = -1;
						lastMoveY = -1;
					}
					else
					{
						float leftEdge;
						float rightEdge;
					
						float topEdge;
						float bottomEdge;
					
						FrameLayout.LayoutParams topParams = (FrameLayout.LayoutParams)mTopCropView.getLayoutParams();
						FrameLayout.LayoutParams bottomParams = (FrameLayout.LayoutParams)mBottomCropView.getLayoutParams();
					
						topEdge = topParams.height;
						bottomEdge = bottomParams.topMargin;
					
						leftEdge = topParams.leftMargin;
						rightEdge = topParams.leftMargin + topParams.width;
						
						if (lastMoveX != -1 && lastMoveY != -1)
						{
							int offsetX = (int)event.getX() - lastMoveX;
							int offsetY = (int)event.getY() - lastMoveY;
							
							leftEdge += offsetX;
							rightEdge += offsetX;
						
							topEdge += offsetY;
							bottomEdge += offsetY;
						}
						
						repositionCroppingRectangle(leftEdge, rightEdge, topEdge, bottomEdge);
						
						lastMoveX = (int)event.getX();
						lastMoveY = (int)event.getY();
					}
				}

				return true;
			}
		});

		mLeftImage = (FrameLayout) findViewById(R.id.leftLayout);
		mCenterImage = (FrameLayout) findViewById(R.id.centerLayout);
		mRightImage = (FrameLayout) findViewById(R.id.rightLayout);

		setCurrentImageIndex(mCurrentImageIndex);
	}
	
	private void repositionCroppingRectangle(float leftEdge, float rightEdge, float topEdge, float bottomEdge)
	{
		FrameLayout.LayoutParams topParams = (FrameLayout.LayoutParams)mTopCropView.getLayoutParams();
		FrameLayout.LayoutParams bottomParams = (FrameLayout.LayoutParams)mBottomCropView.getLayoutParams();
		
		FrameLayout.LayoutParams leftParams = (FrameLayout.LayoutParams)mLeftCropView.getLayoutParams();
		FrameLayout.LayoutParams rightParams = (FrameLayout.LayoutParams)mRightCropView.getLayoutParams();

		leftParams.width = (int)leftEdge;
		
		topParams.leftMargin = (int)leftEdge;
		topParams.height = (int)topEdge;
		topParams.width = (int)rightEdge - (int)leftEdge;
		
		bottomParams.topMargin = (int)bottomEdge;
		bottomParams.leftMargin = (int)leftEdge;
		bottomParams.height = mTopLevelLayoutHeight - (int)bottomEdge;
		bottomParams.width = (int)rightEdge - (int)leftEdge;
		
		rightParams.leftMargin = (int)rightEdge;
		rightParams.width = mTopLevelLayoutWidth - (int)rightEdge;
		
		mTopCropView.setLayoutParams(topParams);
		mBottomCropView.setLayoutParams(bottomParams);
		
		mLeftCropView.setLayoutParams(leftParams);
		mRightCropView.setLayoutParams(rightParams);
	}
	
	private void cancelScrolling()
	{
		if (mHorizontalScrolling==false && mCurrentImageY != 0) {
			Animation centerAnimation = new TranslateAnimation(0, 0, 0, -mCurrentImageY);
			centerAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
			centerAnimation.setFillEnabled(true);

			centerAnimation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					mCurrentImageY = 0;
					mCurrentImageX = 0;
					repositionImages();
					mAnimationRunning = false;
				}
			});
			mCenterImage.startAnimation(centerAnimation);
			mAnimationRunning = true;

		} else if (mHorizontalScrolling == true && mCurrentImageX != 0) {
			Animation centerAnimation = new TranslateAnimation(0, -mCurrentImageX, 0, 0);
			centerAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
			centerAnimation.setFillEnabled(true);

			centerAnimation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					mCurrentImageY = 0;
					mCurrentImageX = 0;
					repositionImages();
					mAnimationRunning = false;
				}
			});
			mCenterImage.startAnimation(centerAnimation);
			mAnimationRunning = true;
		}

		if (mAnimationRunning) {
			Animation leftAnimation = new TranslateAnimation(0, 0, 0, -mCurrentImageY);
			leftAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
			leftAnimation.setFillEnabled(true);
			mLeftImage.startAnimation(leftAnimation);

			Animation rightAnimation = new TranslateAnimation(0, 0, 0, -mCurrentImageY);
			rightAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
			rightAnimation.setFillEnabled(true);
			mRightImage.startAnimation(rightAnimation);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mSettings = new Settings(this);
		
		mImagesLoader = new ImagesLoader(this);
		
		String imagesDirPath = Environment.getExternalStorageDirectory() + "/prod-images";

		File f = new File(imagesDirPath);
		File[] files = f.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String filename) {
				return (!filename.contains("__") && filename.toLowerCase().endsWith(".jpg"));
			}
		});

		if (files == null) {
			files = new File[0];
		}

		Arrays.sort(files);

		for(int i=0; i<files.length; i++)
		{
			mImagesLoader.addCachedImage(new CachedImage(files[i]));
		}
		
		recreateContentView();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		 menu.setHeaderTitle("Context Menu");  
		 menu.add(0, v.getId(), 0, "Action 1");  
		 menu.add(0, v.getId(), 0, "Action 2");  
		
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		recreateContentView();
	}
}
