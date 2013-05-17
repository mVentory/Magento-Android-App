package com.mageventory.activity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;

import android.content.res.Configuration;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.mageventory.R;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.settings.Settings;
import com.mageventory.util.ImageCroppingTool;
import com.mageventory.util.ImagesLoader;
import com.mageventory.util.ImagesLoader.CachedImage;

public class ExternalImagesEditActivity extends BaseActivity {

	private static final int ANIMATION_LENGTH_MILLIS = 100;
	private static final float FLING_DETECTION_THRESHOLD = 0.3f; // screen
																	// diagonals
																	// per second
	private static final int CONTEXT_MENU_READSKU = 0;
	private static final int CONTEXT_MENU_CANCEL = 1;
	private static final int CONTEXT_MENU_UPLOAD = 2;
	private static final int CONTEXT_MENU_SKIP = 3;
	
	private ImagesLoader mImagesLoader;
	private ImageCroppingTool mImageCroppingTool;

	private FrameLayout mLeftImage;
	private FrameLayout mCenterImage;
	private FrameLayout mRightImage;

	private FrameLayout mTopLevelLayout;

	private int mTopLevelLayoutWidth, mTopLevelLayoutHeight;
	private float mTopLevelLayoutDiagonal;

	private GestureDetector mGestureDetector;
	private OnGestureListener mOnGestureListener;
	
	private GestureDetector mLongTapDetector;
	private float mCurrentImageX = 0;
	private float mCurrentImageY = 0;

	private boolean mAnimationRunning = false;
	private int mCurrentImageIndex = 0;

	private boolean mHorizontalScrolling;
	private boolean mScrollingInProgress;
	private Settings mSettings;
	
	private void setCurrentImageIndex(int index) {
		mCurrentImageIndex = index;
		mImagesLoader.setState(index, mLeftImage, mCenterImage, mRightImage);
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
		mImageCroppingTool.bringCroppingLayoutToFront();
	}

	private void recreateContentView() {
		setContentView(R.layout.external_images_edit);
		mTopLevelLayout = (FrameLayout) findViewById(R.id.topLevelLayout);
		
		registerForContextMenu(mTopLevelLayout);

		ViewTreeObserver viewTreeObserver = mTopLevelLayout.getViewTreeObserver();

		viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				if (mTopLevelLayoutWidth != mTopLevelLayout.getWidth()
						&& mTopLevelLayoutHeight != mTopLevelLayout.getHeight()) {
					mTopLevelLayoutWidth = mTopLevelLayout.getWidth();
					mTopLevelLayoutHeight = mTopLevelLayout.getHeight();

					mImageCroppingTool.orientationChange(mTopLevelLayout, mTopLevelLayoutWidth, mTopLevelLayoutHeight);
					
					if (mImageCroppingTool.mCroppingMode == true)
					{
						mImageCroppingTool.enableCropping();
					}
					
					mTopLevelLayoutDiagonal = (float) Math.sqrt(mTopLevelLayoutWidth * mTopLevelLayoutWidth
							+ mTopLevelLayoutHeight * mTopLevelLayoutHeight);

					mCurrentImageX = 0;
					mCurrentImageY = 0;
					
					repositionImages();
				}
			}
		});

		mOnGestureListener = new OnGestureListener() {

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
						&& ((Math.abs(velocityY) / mTopLevelLayoutDiagonal) > FLING_DETECTION_THRESHOLD)) {
					if (velocityY < 0 && mImagesLoader.canSwitchLeft()) {
						Animation centerAnimation = new TranslateAnimation(0, 0, 0, -mTopLevelLayoutHeight
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
								int leftIdx = mImagesLoader.getLeftVisibleIndex(mCurrentImageIndex);
								
								mImagesLoader.undoImage(leftIdx);
								
								FrameLayout tmpVar = mLeftImage;
								mLeftImage = mRightImage;
								mRightImage = mCenterImage;
								mCenterImage = tmpVar;
								
								setCurrentImageIndex(leftIdx);

								mCurrentImageX = 0;
								mCurrentImageY = 0;
								repositionImages();
								mAnimationRunning = false;
							}
						});

						mCenterImage.startAnimation(centerAnimation);
						mAnimationRunning = true;

						Animation leftAnimation = new TranslateAnimation(0, 0, 0, -mTopLevelLayoutHeight
								- mCurrentImageY);
						leftAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						leftAnimation.setFillEnabled(true);
						mLeftImage.startAnimation(leftAnimation);

						Animation rightAnimation = new TranslateAnimation(0, 0, 0, -mTopLevelLayoutHeight
								- mCurrentImageY);
						rightAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						rightAnimation.setFillEnabled(true);
						mRightImage.startAnimation(rightAnimation);

					} else if (velocityY > 0 && mImagesLoader.canSwitchRight()) {
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
								mImagesLoader.queueImage(mCurrentImageIndex, mSettings.getCurrentSKU());

								FrameLayout tmpVar = mLeftImage;
								mLeftImage = mCenterImage;
								mCenterImage = mRightImage;
								mRightImage = tmpVar;

								setCurrentImageIndex(mImagesLoader.getRightVisibleIndex(mCurrentImageIndex));

								mCurrentImageX = 0;
								mCurrentImageY = 0;
								repositionImages();
								mAnimationRunning = false;
							}
						});
						mCenterImage.startAnimation(centerAnimation);
						mAnimationRunning = true;

						Animation leftAnimation = new TranslateAnimation(0, 0, 0, mTopLevelLayoutHeight
								- mCurrentImageY);
						leftAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						leftAnimation.setFillEnabled(true);
						mLeftImage.startAnimation(leftAnimation);

						Animation rightAnimation = new TranslateAnimation(0, 0, 0, mTopLevelLayoutHeight
								- mCurrentImageY);
						rightAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						rightAnimation.setFillEnabled(true);
						mRightImage.startAnimation(rightAnimation);
					} else {
						return false;
					}
				} else if (mHorizontalScrolling
						&& ((Math.abs(velocityX) / mTopLevelLayoutDiagonal) > FLING_DETECTION_THRESHOLD)) {
					if (velocityX < 0 && mImagesLoader.canSwitchRight()) {
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
								FrameLayout tmpVar = mLeftImage;
								mLeftImage = mCenterImage;
								mCenterImage = mRightImage;
								mRightImage = tmpVar;

								setCurrentImageIndex(mImagesLoader.getRightVisibleIndex(mCurrentImageIndex));

								mCurrentImageX = 0;
								mCurrentImageY = 0;
								repositionImages();
								mAnimationRunning = false;
							}
						});
						mCenterImage.startAnimation(centerAnimation);
						mAnimationRunning = true;

						Animation leftAnimation = new TranslateAnimation(0, 0, 0, mTopLevelLayoutHeight
								- mCurrentImageY);
						leftAnimation.setDuration(ANIMATION_LENGTH_MILLIS);
						leftAnimation.setFillEnabled(true);
						mLeftImage.startAnimation(leftAnimation);

						Animation rightAnimation = new TranslateAnimation(0, 0, 0, mTopLevelLayoutHeight
								- mCurrentImageY);
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
		};
		
		mGestureDetector = new GestureDetector(mOnGestureListener);

		mLongTapDetector = new GestureDetector(new SimpleOnGestureListener() {
			public void onLongPress(MotionEvent event) {

				if (mImageCroppingTool.mCroppingMode) {
					if (!mImageCroppingTool.isInsideCroppingRectangle(event.getX(), event.getY()))
					{
						openContextMenu(mTopLevelLayout);
					}
				}
				else
				{
					openContextMenu(mTopLevelLayout);
				}
			}
			
			public boolean onDoubleTap(MotionEvent event) {
				if (!mImageCroppingTool.isInsideCroppingRectangle(event.getX(), event.getY()))
				{
					readSKU();	
				}
	            return true;
	        }
		});

		mTopLevelLayout.setOnTouchListener(new OnTouchListener() {

			int lastMoveX;
			int lastMoveY;

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				mLongTapDetector.onTouchEvent(event);

				if (mAnimationRunning)
					return false;

				if (mImageCroppingTool.mCroppingMode == false && event.getPointerCount() > 1) {
					mImageCroppingTool.enableCropping();
					cancelScrolling();
					mScrollingInProgress = false;
				}

				if (mImageCroppingTool.mCroppingMode == false) {
					boolean consumed = mGestureDetector.onTouchEvent(event);

					if (event.getAction() == MotionEvent.ACTION_UP) {
						mScrollingInProgress = false;
					}

					if (!consumed && event.getAction() == MotionEvent.ACTION_UP) {
						cancelScrolling();
					}
				} else if (event.getPointerCount() > 1) {
					lastMoveX = -1;
					lastMoveY = -1;

					RectF cropRect = new RectF();
					
					cropRect.left = event.getX(0) < event.getX(1) ? event.getX(0) : event.getX(1); 
					cropRect.right = event.getX(0) > event.getX(1) ? event.getX(0) : event.getX(1) + 1;
					
					cropRect.top = event.getY(0) < event.getY(1) ? event.getY(0) : event.getY(1);
					cropRect.bottom = event.getY(0) > event.getY(1) ? event.getY(0) : event.getY(1) + 1;

					mImageCroppingTool.repositionCroppingRectangle(cropRect);
				} else if (event.getPointerCount() == 1) {
					if (event.getAction() != MotionEvent.ACTION_MOVE) {
						lastMoveX = -1;
						lastMoveY = -1;
					} else {
						
						RectF cropRect = mImageCroppingTool.getCropRectangle();

						if (event.getX() >= cropRect.left && event.getX() <= cropRect.right && event.getY() >= cropRect.top
								&& event.getY() <= cropRect.bottom)
							if (lastMoveX != -1 && lastMoveY != -1) {
								int offsetX = (int) event.getX() - lastMoveX;
								int offsetY = (int) event.getY() - lastMoveY;

								cropRect.left += offsetX;
								cropRect.right += offsetX;

								cropRect.top += offsetY;
								cropRect.bottom += offsetY;
							}

						mImageCroppingTool.repositionCroppingRectangle(cropRect);

						lastMoveX = (int) event.getX();
						lastMoveY = (int) event.getY();
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

	private void cancelScrolling() {
		if (mHorizontalScrolling == false && mCurrentImageY != 0) {
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
		mImageCroppingTool = new ImageCroppingTool(mImagesLoader);

		String imagesDirPath = Environment.getExternalStorageDirectory() + "/prod-images";

		File f = new File(imagesDirPath);
		File[] files = f.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				return (filename.toLowerCase().endsWith(".jpg"));
			}
		});

		if (files == null) {
			files = new File[0];
		}

		Arrays.sort(files);
		
		Arrays.sort(files, new Comparator<File>() {

			@Override
			public int compare(File lhs, File rhs) {
				
				String leftName = mImagesLoader.removeSKUFromFileName(lhs.getName());
				String rightName = mImagesLoader.removeSKUFromFileName(rhs.getName());
				
				return leftName.compareTo(rightName);
			}
			
		});

		for (int i = 0; i < files.length; i++) {
			mImagesLoader.addCachedImage(new CachedImage(files[i]));
		}

		mCurrentImageIndex = mImagesLoader.getRightVisibleIndex(-1);
		
		recreateContentView();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		menu.setHeaderTitle("Actions");
		menu.add(0, CONTEXT_MENU_READSKU, 0, "Read SKU");
		if (mImageCroppingTool.mCroppingMode)
		{
			menu.add(0, CONTEXT_MENU_CANCEL, 0, "Cancel");
		}
		menu.add(0, CONTEXT_MENU_UPLOAD, 0, "Upload");
		menu.add(0, CONTEXT_MENU_SKIP, 0, "Skip");
	}

	private void readSKU()
	{
		RectF cropRect = null;
		
		if (mImageCroppingTool.mCroppingMode)
		{
			cropRect = mImageCroppingTool.getCropRectangle();
		}
		
		String code = mImagesLoader.decodeQRCode(cropRect);
		
		if (code != null)
		{
			String[] urlData = code.split("/");
			String sku = urlData[urlData.length - 1];
			
			mSettings.setCurrentSKU(sku);
			
			mImageCroppingTool.disableCropping();
			
			/* Imitate down fling */
			mOnGestureListener.onFling(null, null, 0, mTopLevelLayoutDiagonal * (FLING_DETECTION_THRESHOLD + 1));
		}
		else
		{
			Toast.makeText(this, "Unable to read SKU.", Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		switch(item.getItemId())
		{
		case CONTEXT_MENU_READSKU:
			readSKU();
			break;
		case CONTEXT_MENU_CANCEL:
			if (mImageCroppingTool.mCroppingMode)
			{
				mImageCroppingTool.disableCropping();
			}
			break;
		case CONTEXT_MENU_UPLOAD:
			if (mImageCroppingTool.mCroppingMode)
			{
				mImageCroppingTool.disableCropping();
			}
			
			/* Imitate down fling */
			mOnGestureListener.onFling(null, null, 0, mTopLevelLayoutDiagonal * (FLING_DETECTION_THRESHOLD + 1));
			break;
		case CONTEXT_MENU_SKIP:
			if (mImageCroppingTool.mCroppingMode)
			{
				mImageCroppingTool.disableCropping();
			}
			
			/* Imitate left fling */
			mOnGestureListener.onFling(null, null, -mTopLevelLayoutDiagonal * (FLING_DETECTION_THRESHOLD + 1), 0);
			break;
		default:
			break;
		}
		
		return true;
	}
	
	@Override
	public void onBackPressed() {
		if (mImageCroppingTool.mCroppingMode)
		{
			mImageCroppingTool.disableCropping();
		}
		else
		{
			super.onBackPressed();	
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		recreateContentView();
	}
}
