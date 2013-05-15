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
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
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
		if (mHorizontalScrolling == true) {
			paramsCenter.leftMargin = (int) mCurrentImageX;
		} else {
			paramsCenter.leftMargin = 0;
		}

		paramsCenter.topMargin = (int) mCurrentImageY;
		mCenterImage.setLayoutParams(paramsCenter);

		FrameLayout.LayoutParams paramsLeft = (FrameLayout.LayoutParams) mLeftImage.getLayoutParams();
		paramsLeft.width = mTopLevelLayoutWidth;
		paramsLeft.height = mTopLevelLayoutHeight;
		paramsLeft.leftMargin = (int) (mCurrentImageX - mTopLevelLayoutWidth);
		paramsLeft.topMargin = 0;
		mLeftImage.setLayoutParams(paramsLeft);

		FrameLayout.LayoutParams paramsRight = (FrameLayout.LayoutParams) mRightImage.getLayoutParams();
		paramsRight.width = mTopLevelLayoutWidth;
		paramsRight.height = mTopLevelLayoutHeight;
		paramsRight.leftMargin = (int) (mCurrentImageX + mTopLevelLayoutWidth);
		paramsRight.topMargin = 0;
		mRightImage.setLayoutParams(paramsRight);

		mCenterImage.bringToFront();
	}

	private void recreateContentView() {
		setContentView(R.layout.external_images_edit);
		mTopLevelLayout = (FrameLayout) findViewById(R.id.topLevelLayout);

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
				} else {
					mCurrentImageX += distanceY;
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
						
						/* Imitate left fling */
						this.onFling(null, null, -mTopLevelLayoutDiagonal*(FLING_DETECTION_THRESHOLD+1), 0);
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

				if (mHorizontalScrolling
						&& ((Math.abs(velocityX) / mTopLevelLayoutDiagonal) > FLING_DETECTION_THRESHOLD)) {
					if (velocityX > 0 && mImagesLoader.canSwitchLeft()) {
						Animation centerAnimation = new TranslateAnimation(0, mTopLevelLayoutWidth - mCurrentImageX, 0,
								0);
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
								repositionImages();
								mAnimationRunning = false;
							}
						});

						mCenterImage.startAnimation(centerAnimation);
						mAnimationRunning = true;

						Animation leftAnimation = new TranslateAnimation(0, mTopLevelLayoutWidth - mCurrentImageX, 0, 0);
						leftAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						leftAnimation.setFillEnabled(true);
						mLeftImage.startAnimation(leftAnimation);

						Animation rightAnimation = new TranslateAnimation(0, mTopLevelLayoutWidth - mCurrentImageX, 0,
								0);
						rightAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						rightAnimation.setFillEnabled(true);
						mRightImage.startAnimation(rightAnimation);
						
					} else if (velocityX < 0 && mImagesLoader.canSwitchRight()) {
						Animation centerAnimation = new TranslateAnimation(0, -mTopLevelLayoutWidth - mCurrentImageX,
								0, 0);
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
								mImagesLoader.queueImage(mCurrentImageIndex, mSettings.getCurrentSKU());
								mCurrentImageIndex--;
								
								FrameLayout tmpVar = mLeftImage;
								mLeftImage = mCenterImage;
								mCenterImage = mRightImage;
								mRightImage = tmpVar;
								
								setCurrentImageIndex(mCurrentImageIndex+1);
								
								mCurrentImageX = 0;
								repositionImages();
								mAnimationRunning = false;
								
								
							}
						});
						mCenterImage.startAnimation(centerAnimation);
						mAnimationRunning = true;

						Animation leftAnimation = new TranslateAnimation(0, -mTopLevelLayoutWidth - mCurrentImageX, 0,
								0);
						leftAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						leftAnimation.setFillEnabled(true);
						mLeftImage.startAnimation(leftAnimation);

						Animation rightAnimation = new TranslateAnimation(0, -mTopLevelLayoutWidth - mCurrentImageX, 0,
								0);
						rightAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						rightAnimation.setFillEnabled(true);
						mRightImage.startAnimation(rightAnimation);
					} else {
						return false;
					}
				} else if (mHorizontalScrolling == false
						&& ((Math.abs(velocityY) / mTopLevelLayoutDiagonal) > FLING_DETECTION_THRESHOLD)) {
					if (velocityY > 0 && mImagesLoader.canSwitchRight()) {
						Animation centerAnimation = new TranslateAnimation(0, 0, 0, mTopLevelLayoutHeight
								- mCurrentImageY);
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

						Animation leftAnimation = new TranslateAnimation(0, -mTopLevelLayoutWidth - mCurrentImageX, 0,
								0);
						leftAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						leftAnimation.setFillEnabled(true);
						mLeftImage.startAnimation(leftAnimation);

						Animation rightAnimation = new TranslateAnimation(0, -mTopLevelLayoutWidth - mCurrentImageX, 0,
								0);
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

		mTopLevelLayout.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				if (mAnimationRunning)
					return false;

				boolean consumed = mGestureDetector.onTouchEvent(event);

				if (event.getAction() == MotionEvent.ACTION_UP) {
					mScrollingInProgress = false;
				}

				if (!consumed && event.getAction() == MotionEvent.ACTION_UP) {

					if (mHorizontalScrolling && mCurrentImageX != 0) {
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
								mCurrentImageX = 0;
								repositionImages();
								mAnimationRunning = false;
							}
						});
						mCenterImage.startAnimation(centerAnimation);
						mAnimationRunning = true;

					} else if (mHorizontalScrolling == false && mCurrentImageY != 0) {
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
					}

					if (mAnimationRunning) {
						Animation leftAnimation = new TranslateAnimation(0, -mCurrentImageX, 0, 0);
						leftAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						leftAnimation.setFillEnabled(true);
						mLeftImage.startAnimation(leftAnimation);

						Animation rightAnimation = new TranslateAnimation(0, -mCurrentImageX, 0, 0);
						rightAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						rightAnimation.setFillEnabled(true);
						mRightImage.startAnimation(rightAnimation);
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
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		recreateContentView();
	}
}
