package com.mageventory.activity;

import java.io.File;
import java.util.Arrays;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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

public class ExternalImagesEditActivity extends Activity {

	private static final int ANIMATION_LENGTH_MILLIS = 100;
	private static final float FLING_DETECTION_THRESHOLD = 1f; // screen
																// diagonals per
																// second

	private ImageView mCurrentImage;
	private ImageView mLeftImage;
	private ImageView mRightImage;

	private FrameLayout mTopLevelLayout;

	private int mTopLevelLayoutWidth, mTopLevelLayoutHeight;
	private float mTopLevelLayoutDiagonal;

	private GestureDetector mGestureDetector;
	private float mCurrentImageX = 0;
	private float mCurrentImageY = 0;

	private boolean mAnimationRunning = false;
	private File[] mFiles;
	private int mFileIndex = 0;

	private boolean mHorizontalScrolling;
	private boolean mScrollingInProgress;

	private Bitmap loadBitmap(String path) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inInputShareable = true;
		opts.inPurgeable = true;

		int screenSmallerDimension;
		
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
		screenSmallerDimension = metrics.widthPixels;
		
		if (screenSmallerDimension > metrics.heightPixels)
		{
			screenSmallerDimension = metrics.heightPixels;
		}
		
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, opts);

		int coef = Integer.highestOneBit(opts.outWidth / screenSmallerDimension);
		
		opts.inJustDecodeBounds = false;
		if (coef > 1) {
			opts.inSampleSize = coef;
		}
		
		Bitmap bitmap = BitmapFactory.decodeFile(path, opts);

		return bitmap;
	}

	private void repositionImages() {
		FrameLayout.LayoutParams paramsCenter = (FrameLayout.LayoutParams) mCurrentImage.getLayoutParams();
		paramsCenter.width = mTopLevelLayoutWidth;
		paramsCenter.height = mTopLevelLayoutHeight;
		if (mHorizontalScrolling == true) {
			paramsCenter.leftMargin = (int) mCurrentImageX;
		} else {
			paramsCenter.leftMargin = 0;
		}

		paramsCenter.topMargin = (int) mCurrentImageY;
		mCurrentImage.setLayoutParams(paramsCenter);

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

		mTopLevelLayout.removeAllViews();
		mTopLevelLayout.addView(mLeftImage);
		mTopLevelLayout.addView(mRightImage);
		mTopLevelLayout.addView(mCurrentImage);
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
					mLeftImage.getImageMatrix().getValues(leftImageMatrixArray);

					float rightImageMatrixArray[] = new float[9];
					mRightImage.getImageMatrix().getValues(rightImageMatrixArray);

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

				Drawable d = mCurrentImage.getDrawable();

				if (d != null) {
					Bitmap bitmap = ((BitmapDrawable) d).getBitmap();

					ZXingCodeScanner multiDetector = new ZXingCodeScanner();
					String code = multiDetector.decode(bitmap);

					if (code == null) {
						Toast.makeText(ExternalImagesEditActivity.this, "Unable to decode QR code", Toast.LENGTH_SHORT)
								.show();
					} else {
						Toast.makeText(ExternalImagesEditActivity.this, "Decoded QR code: " + code, Toast.LENGTH_SHORT)
								.show();
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
					if (velocityX > 0 && mFileIndex > 0) {
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
								mCurrentImageX = 0;
								repositionImages();
								mAnimationRunning = false;
							}
						});

						mCurrentImage.startAnimation(centerAnimation);
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

						ImageView tmpVar = mLeftImage;
						mLeftImage = mRightImage;
						mRightImage = mCurrentImage;
						mCurrentImage = tmpVar;

						mFileIndex--;
						if (mFileIndex - 1 >= 0) {
							mLeftImage.setImageBitmap(null);
							Bitmap bitmapLeft = loadBitmap(mFiles[mFileIndex - 1].getPath());
							mLeftImage.setImageBitmap(bitmapLeft);
						} else {
							mLeftImage.setImageBitmap(null);
						}
					} else if (velocityX < 0 && mFileIndex < mFiles.length - 1) {
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
								mCurrentImageX = 0;
								repositionImages();
								mAnimationRunning = false;
							}
						});
						mCurrentImage.startAnimation(centerAnimation);
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

						ImageView tmpVar = mLeftImage;
						mLeftImage = mCurrentImage;
						mCurrentImage = mRightImage;
						mRightImage = tmpVar;

						mFileIndex++;
						if (mFileIndex + 1 < mFiles.length) {
							mRightImage.setImageBitmap(null);
							Bitmap bitmapRight = loadBitmap(mFiles[mFileIndex + 1].getPath());
							mRightImage.setImageBitmap(bitmapRight);
						} else {
							mRightImage.setImageBitmap(null);
						}
					} else {
						return false;
					}
				} else if (mHorizontalScrolling == false
						&& ((Math.abs(velocityY) / mTopLevelLayoutDiagonal) > FLING_DETECTION_THRESHOLD)) {
					if (velocityY > 0 && mFileIndex < mFiles.length - 1) {
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
								mCurrentImageX = 0;
								mCurrentImageY = 0;
								repositionImages();
								mAnimationRunning = false;
							}
						});
						mCurrentImage.startAnimation(centerAnimation);
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

						ImageView tmpVar = mLeftImage;
						mLeftImage = mCurrentImage;
						mCurrentImage = mRightImage;
						mRightImage = tmpVar;

						mFileIndex++;
						if (mFileIndex + 1 < mFiles.length) {
							mRightImage.setImageBitmap(null);
							Bitmap bitmapRight = loadBitmap(mFiles[mFileIndex + 1].getPath());
							mRightImage.setImageBitmap(bitmapRight);
						} else {
							mRightImage.setImageBitmap(null);
						}
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
						mCurrentImage.startAnimation(centerAnimation);
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
						mCurrentImage.startAnimation(centerAnimation);
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

		mLeftImage = (ImageView) findViewById(R.id.leftImage);
		mCurrentImage = (ImageView) findViewById(R.id.currentImage);
		mRightImage = (ImageView) findViewById(R.id.rightImage);

		mLeftImage.setImageBitmap(null);

		if (mFiles.length > 0) {
			mCurrentImage.setImageBitmap(null);
			Bitmap bitmap = loadBitmap(mFiles[0].getPath());
			mCurrentImage.setImageBitmap(bitmap);
		} else {
			mCurrentImage.setImageBitmap(null);
		}

		if (mFiles.length > 1) {
			mRightImage.setImageBitmap(null);
			Bitmap bitmapRight = loadBitmap(mFiles[1].getPath());
			mRightImage.setImageBitmap(bitmapRight);
		} else {
			mRightImage.setImageBitmap(null);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		String imagesDirPath = Environment.getExternalStorageDirectory() + "/prod-images";

		File f = new File(imagesDirPath);
		mFiles = f.listFiles();

		if (mFiles == null) {
			mFiles = new File[0];
		}

		Arrays.sort(mFiles);

		recreateContentView();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		recreateContentView();
	}
}
