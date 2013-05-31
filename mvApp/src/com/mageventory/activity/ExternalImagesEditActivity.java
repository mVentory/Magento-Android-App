package com.mageventory.activity;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.ProductDuplicationOptions;
import com.mageventory.settings.Settings;
import com.mageventory.tasks.ErrorReportCreation;
import com.mageventory.util.ExternalImageUploader;
import com.mageventory.util.ImageCroppingTool;
import com.mageventory.util.ImagesLoader;
import com.mageventory.util.SingleFrequencySoundGenerator;
import com.mageventory.util.ImagesLoader.CachedImage;
import com.mageventory.util.Log;

public class ExternalImagesEditActivity extends BaseActivity implements MageventoryConstants {

	private static final String CURRENT_IMAGE_PATH_BUNDLE_KEY = "CURRENT_IMAGE_PATH_BUNDLE_KEY";
	private static final int ANIMATION_LENGTH_MILLIS = 200;
	private static final float FLING_DETECTION_THRESHOLD = 0.3f; // screen
																	// diagonals
																	// per
																	// second
	private static final int CONTEXT_MENU_READSKU = 0;
	private static final int CONTEXT_MENU_SAVE = 1;
	private static final int CONTEXT_MENU_SKIP = 2;
	private static final int CONTEXT_MENU_UNDO = 3;
	private static final int CONTEXT_MENU_CANCEL = 4;
	private static final int CONTEXT_MENU_CROP = 5;
	private static final int CONTEXT_MENU_UPLOAD_REVIEWED = 6;
	private static final int CONTEXT_MENU_EXIT = 7;

	private static final int UPLOAD_IMAGES_DIALOG = 0;
	private static final int FAILED_SKU_DECODING_DIALOG = 1;
	private static final int ILLEGAL_CODE_DIALOG = 2;
	private static final int DELETE_SKIPPED_IMAGES_DIALOG = 3;
	private static final int NO_IMAGES_TO_REVIEW_OR_UPLOAD_DIALOG = 4;
	private static final int NO_IMAGES_TO_UPLOAD_DIALOG = 5;
	private static final int JUST_DELETE_SKIPPED_IMAGES_DIALOG = 6;

	private ImagesLoader mImagesLoader;
	private ImageCroppingTool mImageCroppingTool;

	private FrameLayout mLeftImage;
	private FrameLayout mCenterImage;
	private FrameLayout mRightImage;

	private View mContextViewAnchor;
	private FrameLayout mTopLevelLayout;
	private LinearLayout mUploadingProgressBar;

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

	private String mLastReadSKU, mLastReadSKUType, mCurrentSKU;
	private boolean mLastSKUReadFromImage;

	private Settings mSettings;
	
	private boolean mIsActivityAlive;
	
	private ProgressDialog mProgressDialog;
	
	private SingleFrequencySoundGenerator mCurrentBeep;
	
	private FrameLayout mOverlayLayout;
	private TextView mDecodeButton;
	
	private void playSuccessfulBeep()
	{
		if (mCurrentBeep != null)
		{
			mCurrentBeep.stopSound();
		}
		
		mCurrentBeep = new SingleFrequencySoundGenerator(1500, 200);
		mCurrentBeep.playSound();
	}
	
	private void playFailureBeep()
	{
		if (mCurrentBeep != null)
		{
			mCurrentBeep.stopSound();
		}
		
		mCurrentBeep = new SingleFrequencySoundGenerator(700, 1000, true);
		mCurrentBeep.playSound();
	}
	
	public void dismissProgressDialog() {
		if (mProgressDialog == null) {
			return;
		}
		mProgressDialog.dismiss();
		mProgressDialog = null;
	}
	
	public void showProgressDialog(final String message, final ErrorReportCreation errorReportCreation) {
		if (mIsActivityAlive == false) {
			return;
		}
		if (mProgressDialog != null) {
			return;
		}
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setMessage(message);
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setCancelable(true);

		mProgressDialog.setButton(ProgressDialog.BUTTON1, "Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mProgressDialog.cancel();
			}
		});
		
		mProgressDialog.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				errorReportCreation.cancel(false);				
			}
		});
		
		mProgressDialog.show();
	}
	
	private void setCurrentImageIndex(int index) {
		mCurrentImageIndex = index;
		mImagesLoader.setState(index, mLeftImage, mCenterImage, mRightImage);
		mCurrentSKU = mImagesLoader.getCurrentSKU();
		
		toggleDecodeButtonVisibility();
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

	private void toggleDecodeButtonVisibility()
	{
		if (mCurrentImageIndex == mImagesLoader.getAllFilesCount())
		{
			mImageCroppingTool.hideDecodeButton();
		}
		else
		{
			mImageCroppingTool.showDecodeButton();
		}
	}
	
	private void recreateContentView() {
		setContentView(R.layout.external_images_edit);
		mOverlayLayout = (FrameLayout) findViewById(R.id.overlayLayout);
		mDecodeButton = (TextView) findViewById(R.id.decodeButton);
		
		mDecodeButton.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				
				readSKU();
				
				return true;
			}
		});
		
		mImageCroppingTool.setDecodeButtonView(mDecodeButton);
		
		mTopLevelLayout = (FrameLayout) findViewById(R.id.topLevelLayout);
		mUploadingProgressBar = (LinearLayout) findViewById(R.id.uploadingProgressBar);

		mContextViewAnchor = (View)findViewById(R.id.contextAnchorView);
		
		registerForContextMenu(mContextViewAnchor);

		ViewTreeObserver viewTreeObserver = mTopLevelLayout.getViewTreeObserver();

		viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				if (mTopLevelLayoutWidth != mTopLevelLayout.getWidth()
						&& mTopLevelLayoutHeight != mTopLevelLayout.getHeight()) {
					mTopLevelLayoutWidth = mTopLevelLayout.getWidth();
					mTopLevelLayoutHeight = mTopLevelLayout.getHeight();

					mImageCroppingTool.orientationChange(mTopLevelLayout, mTopLevelLayoutWidth, mTopLevelLayoutHeight);

					if (mImageCroppingTool.mCroppingMode == true) {
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

				if (e1 == null || e2 == null)
				{
					return false;
				}
				
				if (mImageCroppingTool.isInsideCroppingRectangle(e2.getX(), e2.getY()) && mImageCroppingTool.isCroppingShown()) {
					return false;
				}

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
					mCurrentImageY = Math.abs(mCurrentImageX);
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

				if (e1 == null || e2 == null)
				{
					return false;
				}
				
				if (mImageCroppingTool.isInsideCroppingRectangle(e2.getX(), e2.getY()) && mImageCroppingTool.isCroppingShown()) {
					return false;
				}
				
				float flingDistance = (float)Math.sqrt((e1.getX()-e2.getX())*(e1.getX()-e2.getX()) + (e1.getY()-e2.getY())*(e1.getY()-e2.getY()));
				
				/* Take distance into consideration when deciding whether the fling was valid or not. */
				if (flingDistance < mTopLevelLayoutDiagonal * 0.1)
				{
					return false;
				}
				
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
						
						if (mImageCroppingTool.mCroppingMode) {
							mImageCroppingTool.disableCropping();
						}
						
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
								
								mImageCroppingTool.showOverlay();
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

						if (mLastReadSKU == null && mCurrentSKU == null) {
							Toast.makeText(
									ExternalImagesEditActivity.this,
									"Cannot save without reading a product code first. Swipe left or right to discard or open the menu.",
									Toast.LENGTH_LONG).show();
							playFailureBeep();
							return false;
						}

						if (mImageCroppingTool.mCroppingMode) {
							
							if (mLastReadSKU == null)
							{
								RectF cropRect = null;
								cropRect = mImageCroppingTool.getCropRectangle();
								mImagesLoader.crop(cropRect);
							}
							
							mImageCroppingTool.disableCropping();
						}
						
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
								
								if (mLastReadSKU == null)
								{
									mImagesLoader.queueImage(mCurrentImageIndex, mLastReadSKU != null ? mLastReadSKU
										: mCurrentSKU, false);
								}
								else
								{
									if (mLastSKUReadFromImage)
									{
										mImagesLoader.queueImage(mCurrentImageIndex, mLastReadSKU != null ? mLastReadSKU
											: mCurrentSKU, true);
									}
									else
									{
										mImagesLoader.queueImage(mCurrentImageIndex, mLastReadSKU != null ? mLastReadSKU
											: mCurrentSKU, false);
									}
								}
								
								mLastReadSKU = null;

								FrameLayout tmpVar = mLeftImage;
								mLeftImage = mCenterImage;
								mCenterImage = mRightImage;
								mRightImage = tmpVar;

								setCurrentImageIndex(mImagesLoader.getRightVisibleIndex(mCurrentImageIndex));

								mCurrentImageX = 0;
								mCurrentImageY = 0;
								repositionImages();
								mAnimationRunning = false;
								
								mImageCroppingTool.showOverlay();
								
								if (mCurrentImageIndex == mImagesLoader.getAllFilesCount())
								{
									handleUploadMenuItem();
								}
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
						if (mImageCroppingTool.mCroppingMode) {
							mImageCroppingTool.disableCropping();
						}
						
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
								
								mImageCroppingTool.showOverlay();
								
								if (mCurrentImageIndex == mImagesLoader.getAllFilesCount())
								{
									handleUploadMenuItem();
								}
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
					} else if (velocityX > 0 && mImagesLoader.canSwitchRight()) {
						if (mImageCroppingTool.mCroppingMode) {
							mImageCroppingTool.disableCropping();
						}
						
						Animation centerAnimation = new TranslateAnimation(0, mTopLevelLayoutWidth - mCurrentImageX,
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
								
								mImageCroppingTool.showOverlay();
								
								if (mCurrentImageIndex == mImagesLoader.getAllFilesCount())
								{
									handleUploadMenuItem();
								}
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
					} else{
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
					if (!mImageCroppingTool.isInsideCroppingRectangle(event.getX(), event.getY())) {
						openContextMenu(mContextViewAnchor);
					}
				} else {
					openContextMenu(mContextViewAnchor);
				}
			}

			public boolean onDoubleTap(MotionEvent event) {
				if (!mImageCroppingTool.isInsideCroppingRectangle(event.getX(), event.getY())) {
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

				if (event.getPointerCount() == 1)
				{
					if (event.getAction() == MotionEvent.ACTION_DOWN)
					{
						mImageCroppingTool.hideOverlay();
					}
					else
					if (event.getAction() == MotionEvent.ACTION_UP)
					{
						mImageCroppingTool.showOverlayDelayed();
					}
				}
				
				//mLongTapDetector.onTouchEvent(event);

				if (mAnimationRunning)
					return false;

				if (mImageCroppingTool.mCroppingMode == false && event.getPointerCount() > 1) {
					mImageCroppingTool.enableCropping();
					
					/* Reset the current gesture detector. Seems like there is no better way than just creating a new one. */
					mGestureDetector = new GestureDetector(mOnGestureListener);
					// cancelScrolling();
					// mScrollingInProgress = false;
				}

				boolean performedRectangleManipulation = false;

				if (mImageCroppingTool.mCroppingMode == true) {

					if (event.getPointerCount() > 1) {
						lastMoveX = -1;
						lastMoveY = -1;

						RectF cropRect = new RectF();

						cropRect.left = event.getX(0) < event.getX(1) ? event.getX(0) : event.getX(1);
						cropRect.right = event.getX(0) > event.getX(1) ? event.getX(0) : event.getX(1) + 1;

						cropRect.top = event.getY(0) < event.getY(1) ? event.getY(0) : event.getY(1);
						cropRect.bottom = event.getY(0) > event.getY(1) ? event.getY(0) : event.getY(1) + 1;

						mImageCroppingTool.repositionCroppingRectangle(cropRect);
						performedRectangleManipulation = true;
					} else if (event.getPointerCount() == 1 && mImageCroppingTool.isCroppingShown()) {
						if (event.getAction() != MotionEvent.ACTION_MOVE) {
							lastMoveX = -1;
							lastMoveY = -1;

							if (mImageCroppingTool.isInsideCroppingRectangle(lastMoveX, lastMoveY)) {
								performedRectangleManipulation = true;
							}
						} else {

							RectF cropRect = mImageCroppingTool.getCropRectangle();

							if (lastMoveX != -1 && lastMoveY != -1) {
								if (mImageCroppingTool.isInsideCroppingRectangle(lastMoveX, lastMoveY)) {

									int offsetX = (int) event.getX() - lastMoveX;
									int offsetY = (int) event.getY() - lastMoveY;

									cropRect.left += offsetX;
									cropRect.right += offsetX;

									cropRect.top += offsetY;
									cropRect.bottom += offsetY;
									performedRectangleManipulation = true;
								}
							}

							mImageCroppingTool.repositionCroppingRectangle(cropRect);

							lastMoveX = (int) event.getX();
							lastMoveY = (int) event.getY();
						}
					}
				}

				if (!performedRectangleManipulation) {
					boolean consumed = mGestureDetector.onTouchEvent(event);

					if (!consumed && event.getAction() == MotionEvent.ACTION_UP) {
						
						boolean flingPerformed = false;
						
						if (isNearScreenBorder(event))
						{
							if (mHorizontalScrolling == true)
							{
								if (mCurrentImageX / mTopLevelLayoutDiagonal < -0.1)
								{
									if (imitateLeftFling())
										flingPerformed = true;
								}
							}
							else
							{
								if (mCurrentImageY / mTopLevelLayoutDiagonal < -0.1)
								{
									if (imitateUpFling())
										flingPerformed = true;
								} else
								if (mCurrentImageY / mTopLevelLayoutDiagonal > 0.1)
								{
									if (imitateDownFling())
										flingPerformed = true;
								}
							}
						}
						
						if (!flingPerformed)
						{
							cancelScrolling();
							if (mImageCroppingTool.mCroppingMode) {
								mImageCroppingTool.showCropping();
							}
						}
					} else {
						if (mImageCroppingTool.mCroppingMode) {
							if (mCurrentImageX != 0 || mCurrentImageY != 0) {
								mImageCroppingTool.hideCropping();
							}
						}
					}
					
					if (event.getAction() == MotionEvent.ACTION_UP) {
						mScrollingInProgress = false;
					}
				} else if (mScrollingInProgress) {
					mScrollingInProgress = false;
					cancelScrolling();
					
					/* Reset the current gesture detector. Seems like there is no better way than just creating a new one. */
					mGestureDetector = new GestureDetector(mOnGestureListener);
					
					if (mImageCroppingTool.mCroppingMode) {
						mImageCroppingTool.showCropping();
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

	private boolean isNearScreenBorder(MotionEvent me)
	{
		int threshold = (int)(mTopLevelLayoutDiagonal * 0.1f);
		
		Rect innerScreenRect = new Rect(0 + threshold, 0 + threshold, mTopLevelLayoutWidth - threshold, mTopLevelLayoutHeight - threshold);
		
		return !innerScreenRect.contains((int)me.getX(), (int)me.getY());
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
		
		mIsActivityAlive = true;
		
		mSettings = new Settings(this);

		mImagesLoader = new ImagesLoader(this);
		mImageCroppingTool = new ImageCroppingTool(mImagesLoader);

		String imagesDirPath = mSettings.getGalleryPhotosDirectory();

		File f = new File(imagesDirPath);
		
		File[] files = f.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				return (pathname.getName().toLowerCase().contains(".jpg") && !pathname.isDirectory());
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

		boolean currentIndexSet = false;
		String currentImagePath = null;
		
		if (savedInstanceState != null)
		{
			currentImagePath = (String)savedInstanceState.get(CURRENT_IMAGE_PATH_BUNDLE_KEY);
		}
		
		for (int i = 0; i < files.length; i++) {
			mImagesLoader.addCachedImage(new CachedImage(files[i]));
			if (currentImagePath != null && currentIndexSet == false && files[i].getAbsolutePath().equals(currentImagePath))
			{
				mCurrentImageIndex = i;
				currentIndexSet = true;
			}
		}

		if (!currentIndexSet)
		{
			mCurrentImageIndex = mImagesLoader.getRightVisibleIndex(-1);
		}
		
		recreateContentView();
		
		if (mCurrentImageIndex == files.length)
		{
			handleUploadMenuItem();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		
		if (mImagesLoader != null)
		{
			outState.putString(CURRENT_IMAGE_PATH_BUNDLE_KEY, mImagesLoader.getCurrentImagePath());
		}
		
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mIsActivityAlive = false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		menu.setHeaderTitle("Actions");
		menu.add(0, CONTEXT_MENU_READSKU, 0, "Read SKU");
		menu.add(0, CONTEXT_MENU_SAVE, 0, "Save (swipe down)");
		menu.add(0, CONTEXT_MENU_SKIP, 0, "Skip (swipe left or right)");
		menu.add(0, CONTEXT_MENU_UNDO, 0, "Undo (swipe up)");
		
		if (mImageCroppingTool.mCroppingMode) {
			menu.add(0, CONTEXT_MENU_CANCEL, 0, "Cancel");
			menu.add(0, CONTEXT_MENU_CROP, 0, "Crop");
		}
		
		menu.add(0, CONTEXT_MENU_UPLOAD_REVIEWED, 0, "Upload reviewed images");
		menu.add(0, CONTEXT_MENU_EXIT, 0, "Exit");
	}
	
	private boolean imitateUpFling()
	{
		MotionEvent me1 = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, -10000, -10000 + mTopLevelLayoutDiagonal, 0);
		MotionEvent me2 = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, -10000, -10000, 0);
		
		/* Imitate up fling */
		return mOnGestureListener.onFling(me1, me2, 0, -mTopLevelLayoutDiagonal * (FLING_DETECTION_THRESHOLD + 1));
	}
	
	private boolean containsReservedChars(String productCode)
	{
		String reservedChars = "|\\?*<\":>+[]/'";
		
		for (int i=0; i<reservedChars.length(); i++)
		{
			if (productCode.indexOf(reservedChars.charAt(i)) != -1)
			{
				return true;
			}
		}

		return false;
	}
	
	private boolean imitateDownFling()
	{
		if (mLastReadSKU != null && containsReservedChars(mLastReadSKU))
		{
			Log.logCaughtException(new Exception("Illegal product code: " + mLastReadSKU));
			
			mLastReadSKU = null;
			
			showDialog(ILLEGAL_CODE_DIALOG);
			
			return false;
		}
		
		MotionEvent me1 = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, -10000, -10000 - mTopLevelLayoutDiagonal, 0);
		MotionEvent me2 = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, -10000, -10000, 0);
		
		/* Imitate down fling */
		return mOnGestureListener.onFling(me1, me2, 0, mTopLevelLayoutDiagonal * (FLING_DETECTION_THRESHOLD + 1));
	}
	
	private boolean imitateLeftFling()
	{
		MotionEvent me1 = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, -10000 + mTopLevelLayoutDiagonal, -10000, 0);
		MotionEvent me2 = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, -10000, -10000, 0);
		
		/* Imitate left fling */
		return mOnGestureListener.onFling(me1, me2, -mTopLevelLayoutDiagonal * (FLING_DETECTION_THRESHOLD + 1), 0);
	}
	
	private void readSKU() {
		RectF cropRect = null;

		if (mImageCroppingTool.mCroppingMode) {
			cropRect = mImageCroppingTool.getCropRectangle();
		}

		String code = mImagesLoader.decodeQRCode(cropRect);

		if (code != null) {

			playSuccessfulBeep();		
			
			String[] urlData = code.split("/");
			String sku = urlData[urlData.length - 1];

			mLastReadSKUType = mImagesLoader.getLastReadCodeType();
			mLastSKUReadFromImage = true;
			mLastReadSKU = sku;

			imitateDownFling();
		} else {
			showDialog(FAILED_SKU_DECODING_DIALOG);
			playFailureBeep();
		}
	}

	private void prepareImagesForUpload(final boolean removeSkipped)
	{
		mTopLevelLayout.setOnTouchListener(null);

		unregisterForContextMenu(mContextViewAnchor);
		mImageCroppingTool.disableCropping();
		mImageCroppingTool.hideOverlay();
		mCenterImage.setVisibility(View.GONE);

		mTopLevelLayout.setBackgroundColor(0xFF000000);
		mUploadingProgressBar.setVisibility(View.VISIBLE);

		AsyncTask<Void, Void, Boolean> mFileCopier = new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {

				if (removeSkipped)
				{
					mImagesLoader.queueSkippedForRemoval();
				}
				
				ArrayList<File> filesToUpload = mImagesLoader.getFilesToUpload();
				
				ExternalImageUploader uploader = new ExternalImageUploader(ExternalImagesEditActivity.this);

				File destinationDir = new File(JobCacheManager.getProdImagesQueuedDirName());

				if (!destinationDir.exists()) {
					if (destinationDir.mkdirs() == false) {
						return false;
					}
				}

				for (int i = 0; i < filesToUpload.size(); i++) {
					File destinationFile = new File(destinationDir, filesToUpload.get(i).getName());
					filesToUpload.get(i).renameTo(destinationFile);
				}

				File[] files = destinationDir.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String filename) {
						return (filename.toLowerCase().contains(".jpg"));
					}
				});

				if (files == null) {
					files = new File[0];
				}

				Arrays.sort(files);

				for (int i = 0; i < files.length; i++) {
					File fileToUpload = files[i];
					
					if (fileToUpload.getName().endsWith("_x"))
					{
						fileToUpload.delete();
						continue;
					}
					
					boolean success = true;

					Rect bitmapRectangle = mImagesLoader.getBitmapRect(fileToUpload);

					if (bitmapRectangle != null) {
						int orientation = ExifInterface.ORIENTATION_NORMAL;
						ExifInterface exif = null;
						
						try {
							exif = new ExifInterface(fileToUpload.getAbsolutePath());
						} catch (IOException e1) {
						}
						
						if (exif!=null)
						{
							orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
						}
						
						String oldName = fileToUpload.getName();

						int trimTo = oldName.toLowerCase().indexOf(".jpg") + 4;

						if (trimTo != -1) {
							String newFileName = oldName.substring(0,
									oldName.toLowerCase().indexOf(".jpg") + 4);
							File newFile = new File(fileToUpload.getParentFile(), newFileName);

							boolean renamed = fileToUpload.renameTo(newFile);

							if (renamed) {
								fileToUpload = newFile;
							}
						} else {
							success = false;
						}

						if (success) {
							try {
								FileInputStream fis = new FileInputStream(fileToUpload);

								int screenSmallerDimension;

								DisplayMetrics metrics = new DisplayMetrics();
								getWindowManager().getDefaultDisplay().getMetrics(metrics);
								screenSmallerDimension = metrics.widthPixels;
								if (screenSmallerDimension > metrics.heightPixels) {
									screenSmallerDimension = metrics.heightPixels;
								}

								BitmapFactory.Options opts = new BitmapFactory.Options();
								opts.inInputShareable = true;
								opts.inPurgeable = true;

								BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(fis, false);
								Bitmap croppedBitmap = decoder.decodeRegion(bitmapRectangle, opts);

								fis.close();

								FileOutputStream fos = new FileOutputStream(fileToUpload);
								croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
								fos.close();
								croppedBitmap = null;
								
								if (orientation != ExifInterface.ORIENTATION_NORMAL)
								{
									try {
										exif = new ExifInterface(fileToUpload.getAbsolutePath());
									} catch (IOException e1) {
									}
									
									if (exif != null)
									{
										exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + orientation);
										exif.saveAttributes();
									}
								}
							} catch (FileNotFoundException e) {
								Log.logCaughtException(e);
								success = false;
							} catch (IOException e) {
								Log.logCaughtException(e);
								success = false;
							}
						}
					}

					if (!success) {
						ExternalImageUploader.moveImageToBadPics(fileToUpload);
					}

					uploader.scheduleImageUpload(fileToUpload.getAbsolutePath());
				}

				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {

				if (mIsActivityAlive) {
					finish();

					Intent i = new Intent(ExternalImagesEditActivity.this,
						ExternalImagesEditActivity.class);
					ExternalImagesEditActivity.this.startActivity(i);
				}
			}
		};

		mFileCopier.execute();

	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case UPLOAD_IMAGES_DIALOG:

			AlertDialog.Builder alert = new AlertDialog.Builder(ExternalImagesEditActivity.this);

			alert.setTitle("Upload now?");
			alert.setMessage("Upload " + mImagesLoader.getFilesToUploadCount() + " images for " + mImagesLoader.getSKUsToUploadCount() + " products?");

			alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					if (mImagesLoader.getSkippedCount() != 0)
					{
						showDialog(DELETE_SKIPPED_IMAGES_DIALOG);
					}
					else
					{
						prepareImagesForUpload(false);
					}
				}
			});

			alert.setNegativeButton("No", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});

			AlertDialog srDialog = alert.create();

			srDialog.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					removeDialog(UPLOAD_IMAGES_DIALOG);
				}
			});
			
			return srDialog;
		
		case DELETE_SKIPPED_IMAGES_DIALOG:

			AlertDialog.Builder deleteSkippedAlert = new AlertDialog.Builder(ExternalImagesEditActivity.this);

			deleteSkippedAlert.setTitle("Delete skipped?");
			deleteSkippedAlert.setMessage("Delete " + mImagesLoader.getSkippedCount() + " skipped images?");

			deleteSkippedAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					prepareImagesForUpload(true);
				}
			});

			deleteSkippedAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					prepareImagesForUpload(false);
				}
			});

			AlertDialog deleteSkippedDialog = deleteSkippedAlert.create();

			deleteSkippedDialog.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					removeDialog(DELETE_SKIPPED_IMAGES_DIALOG);
				}
			});
			
			return deleteSkippedDialog;
			
		case JUST_DELETE_SKIPPED_IMAGES_DIALOG:
			
			AlertDialog.Builder justDeleteSkippedAlert = new AlertDialog.Builder(ExternalImagesEditActivity.this);

			justDeleteSkippedAlert.setTitle("Delete skipped?");
			justDeleteSkippedAlert.setMessage("No images to upload. Delete " + mImagesLoader.getSkippedCount() + " skipped images?");

			justDeleteSkippedAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					prepareImagesForUpload(true);
				}
			});

			justDeleteSkippedAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});

			AlertDialog justDeleteSkippedDialog = justDeleteSkippedAlert.create();

			justDeleteSkippedDialog.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					removeDialog(JUST_DELETE_SKIPPED_IMAGES_DIALOG);
				}
			});
			
			return justDeleteSkippedDialog;
			
		case NO_IMAGES_TO_REVIEW_OR_UPLOAD_DIALOG:
			
			AlertDialog.Builder noImagesAlert = new AlertDialog.Builder(ExternalImagesEditActivity.this);

			noImagesAlert.setTitle("Empty");
			noImagesAlert.setMessage("No images to review or upload.");

			noImagesAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});

			AlertDialog noImagesDialog = noImagesAlert.create();
			
			return noImagesDialog;
			
		case NO_IMAGES_TO_UPLOAD_DIALOG:
			
			AlertDialog.Builder noImagesTpUploadAlert = new AlertDialog.Builder(ExternalImagesEditActivity.this);

			noImagesTpUploadAlert.setTitle("Empty");
			noImagesTpUploadAlert.setMessage("No images to upload.");

			noImagesTpUploadAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});

			AlertDialog noImagesToUploadDialog = noImagesTpUploadAlert.create();
			
			return noImagesToUploadDialog;
			
		case FAILED_SKU_DECODING_DIALOG:
			
			AlertDialog.Builder skuDecodingFailureAlert = new AlertDialog.Builder(ExternalImagesEditActivity.this);

			skuDecodingFailureAlert.setTitle("Re-scan?");
			skuDecodingFailureAlert.setMessage("Cannot read the code. Re-scan?");

			skuDecodingFailureAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent scanInt = new Intent("com.google.zxing.client.android.SCAN");
					startActivityForResult(scanInt, SCAN_QR_CODE);
				}
			});

			skuDecodingFailureAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});

			AlertDialog skuDecodingFailureDialog = skuDecodingFailureAlert.create();

			skuDecodingFailureDialog.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					removeDialog(FAILED_SKU_DECODING_DIALOG);
				}
			});

			return skuDecodingFailureDialog;
			
			
		case ILLEGAL_CODE_DIALOG:
			AlertDialog.Builder illegalCodeAlert = new AlertDialog.Builder(ExternalImagesEditActivity.this);

			illegalCodeAlert.setTitle("Product code error");
			illegalCodeAlert.setMessage("Illegal code. Report an error?");

			illegalCodeAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new ErrorReportCreation(ExternalImagesEditActivity.this, true).execute();
				}
			});

			illegalCodeAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					
				}
			});

			AlertDialog illegalCodeDialog = illegalCodeAlert.create();

			illegalCodeDialog.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					removeDialog(ILLEGAL_CODE_DIALOG);
				}
			});

			return illegalCodeDialog;
			
		default:
			return null;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == SCAN_QR_CODE) {
			if (resultCode == RESULT_OK) {
				String contents = data.getStringExtra("SCAN_RESULT");
				
				String[] urlData = contents.split("/");
				if (urlData.length > 0) {
					playSuccessfulBeep();
					
					String sku = urlData[urlData.length - 1];
					
					mLastReadSKU = sku;
					mLastSKUReadFromImage = false;
					mLastReadSKUType = data.getStringExtra("SCAN_RESULT_FORMAT");
					imitateDownFling();
				}
				else
				{
					playFailureBeep();
				}
			} else if (resultCode == RESULT_CANCELED) {
				playFailureBeep();
			}
		}
	}
	
	private void handleUploadMenuItem()
	{
		if (mImagesLoader.getAllFilesCount() > 0 )
		{
			if (mImagesLoader.getFilesToUploadCount() > 0)
			{
				showDialog(UPLOAD_IMAGES_DIALOG);
			}
			else
			{
				if (mImagesLoader.getSkippedCount() > 0)
				{
					showDialog(JUST_DELETE_SKIPPED_IMAGES_DIALOG);	
				}
				else
				{
					showDialog(NO_IMAGES_TO_UPLOAD_DIALOG);	
				}
			}
		}
		else
		{
			showDialog(NO_IMAGES_TO_REVIEW_OR_UPLOAD_DIALOG);	
		}

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case CONTEXT_MENU_READSKU:
			readSKU();
			break;
		case CONTEXT_MENU_CANCEL:
			if (mImageCroppingTool.mCroppingMode) {
				mImageCroppingTool.disableCropping();
			}
			break;
		case CONTEXT_MENU_SAVE:
			if (mImageCroppingTool.mCroppingMode) {
				RectF cropRect = mImageCroppingTool.getCropRectangle();
				mImagesLoader.crop(cropRect);

				mImageCroppingTool.disableCropping();
			}

			imitateDownFling();
			break;
		case CONTEXT_MENU_CROP:
			if (mImageCroppingTool.mCroppingMode) {
				RectF cropRect = mImageCroppingTool.getCropRectangle();
				mImagesLoader.crop(cropRect);

				mImageCroppingTool.disableCropping();
			}
			break;

		case CONTEXT_MENU_SKIP:
			if (mImageCroppingTool.mCroppingMode) {
				mImageCroppingTool.disableCropping();
			}

			imitateLeftFling();
			break;
			
		case CONTEXT_MENU_UNDO:
			if (mImageCroppingTool.mCroppingMode) {
				mImageCroppingTool.disableCropping();
			}

			imitateUpFling();
			break;
		case CONTEXT_MENU_UPLOAD_REVIEWED:

			handleUploadMenuItem();
			
			break;
		case CONTEXT_MENU_EXIT:
			finish();
			break;
		default:
			break;
		}

		return true;
	}

	@Override
	public void onBackPressed() {
		if (mImageCroppingTool.mCroppingMode) {
			mImageCroppingTool.disableCropping();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		mImageCroppingTool.disableCropping();
		
		if (mUploadingProgressBar.getVisibility() == View.GONE) {
			recreateContentView();
		}
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_MENU) {
			openContextMenu(mContextViewAnchor);
            return true;
	    }
	    return super.onKeyUp(keyCode, event);
	}
}
