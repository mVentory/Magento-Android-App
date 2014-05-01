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

package com.mageventory.activity;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher.OnViewTapListener;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.LibraryActivity.LibraryUiFragment.AbstractAddNewImageTask;
import com.mageventory.activity.MainActivity.ImageData;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.bitmapfun.util.ImageCache;
import com.mageventory.bitmapfun.util.ImageFileSystemFetcher;
import com.mageventory.fragment.base.BaseFragmentWithImageWorker;
import com.mageventory.job.JobControlInterface;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.util.FileUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.SimpleViewLoadingControl;
import com.mageventory.util.SingleFrequencySoundGenerator;
import com.mageventory.util.TrackerUtils;
import com.mageventory.util.ZXingCodeScanner;

/**
 * Simple image view activity
 * 
 * @author Eugene Popovich
 */
public class PhotoViewActivity extends BaseFragmentActivity implements MageventoryConstants {
    private static final String TAG = PhotoViewActivity.class.getSimpleName();
    public static final String EXTRA_PATH = "PATH";
    public static final String EXTRA_URL = "URL";
    public static final String EXTRA_SOURCE = "SOURCE";

    public enum Source {
        MAIN, LIBRARY
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new PhotoViewUiFragment()).commit();
        }
    }

    PhotoViewUiFragment getContentFragment() {
        return (PhotoViewUiFragment) getSupportFragmentManager().findFragmentById(
                R.id.content_frame);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            getContentFragment().reinitFromIntent(intent);
        }
    }

    public static class PhotoViewUiFragment extends BaseFragmentWithImageWorker implements
            LoadingControl {
        private PhotoView mImageView;
        private View mLoadingView;
        private LoadingControl mDecodeStatusLoadingControl;
        private DecodeImageTask mDecodeImageTask;
        private TextView mFileInfo;
        private String mProductSku;
        private String mPath;
        JobControlInterface mJobControlInterface;
        private Settings mSettings;

        private AtomicInteger mLoaders = new AtomicInteger(0);
        boolean mDetailsVisible;
        private SingleFrequencySoundGenerator mCurrentBeep;
        private Source mSource;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mJobControlInterface = new JobControlInterface(getActivity());
            mSettings = new Settings(getActivity());
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            View view = inflater.inflate(R.layout.photo_view, container, false);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mImageView = (PhotoView) view.findViewById(R.id.image);
            mImageView.setOnViewTapListener(new OnViewTapListener() {

                @Override
                public void onViewTap(View v, float x, float y) {
                    v.playSoundEffect(SoundEffectConstants.CLICK);
                    TrackerUtils.trackButtonClickEvent("image", PhotoViewUiFragment.this);
                    adjustDetailsVisibility(!mDetailsVisible);
                    registerForContextMenu(v);
                    v.showContextMenu();
                    unregisterForContextMenu(v);
                }
            });
            mImageView.setMaxScale(7.0f);
            mLoadingView = view.findViewById(R.id.loading);
            mFileInfo = (TextView) view.findViewById(R.id.fileInfo);
            mDecodeStatusLoadingControl = new SimpleViewLoadingControl(
                    view.findViewById(R.id.decodeStatusLine));
            reinitFromIntent(getActivity().getIntent());

        }

        @Override
        protected void initImageWorker() {
            final DisplayMetrics displaymetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            final int height = displaymetrics.heightPixels;
            final int width = displaymetrics.widthPixels;
            final int longest = height > width ? height : width;

            mImageWorker = new ImageFileSystemFetcher(getActivity(), this, longest);
            mImageWorker.setImageCache(ImageCache.findOrCreateCache(getActivity(),
                    ImageCache.LARGE_IMAGES_CACHE_DIR, 50, false, false));
        }

        public void reinitFromIntent(Intent intent) {
            String source = intent.getStringExtra(EXTRA_SOURCE);
            if (source == null) {
                mSource = Source.LIBRARY;
            } else {
                mSource = Source.valueOf(source);
            }
            if (intent.hasExtra(EXTRA_PATH)) {
                mPath = intent.getStringExtra(EXTRA_PATH);
                String url = intent.getStringExtra(EXTRA_URL);
                mProductSku = intent.getStringExtra(getString(R.string.ekey_product_sku));
                mImageWorker.loadImage(mPath, mImageView);
                try {
                    File file = new File(mPath);
                    ImageData id = ImageData.getImageDataForFile(file, false);
                    mFileInfo.setText(CommonUtils.getStringResource(
                            mSource == Source.MAIN ? R.string.photo_view_overlay_size_format_main
                                    : R.string.photo_view_overlay_size_format, id.getWidth(), id
                                    .getHeight(), FileUtils.formatFileSize(file.length()),
                            url == null ? mPath : url));
                } catch (Exception ex) {
                    GuiUtils.noAlertError(TAG, ex);
                }
                GuiUtils.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (!mDetailsVisible) {
                            adjustDetailsVisibility(false);
                        }
                    }
                }, 4000);
            }
        }

        void adjustDetailsVisibility(final boolean visible) {
            mDetailsVisible = visible;
            if (!isResumed()) {
                return;
            }
            Animation animation = AnimationUtils.loadAnimation(getActivity(),
                    visible ? android.R.anim.fade_in : android.R.anim.fade_out);
            long animationDuration = 500;
            animation.setDuration(animationDuration);
            mFileInfo.startAnimation(animation);
            mFileInfo.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDetailsVisible = visible;
                    mFileInfo.setVisibility(mDetailsVisible ? View.VISIBLE : View.GONE);
                }
            }, animationDuration);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            if (v.getId() == R.id.image) {
                MenuInflater inflater = getActivity().getMenuInflater();
                switch (mSource) {
                    case LIBRARY:
                        inflater.inflate(R.menu.photo_view, menu);
                        break;
                    case MAIN:
                        inflater.inflate(R.menu.photo_view_main, menu);
                        break;
                }
                super.onCreateContextMenu(menu, v, menuInfo);
            } else {
                super.onCreateContextMenu(menu, v, menuInfo);
            }
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            int menuItemIndex = item.getItemId();
            switch (menuItemIndex) {
                case R.id.menu_upload:
                    new AddNewImageTask(mPath, false).execute();
                    break;
                case R.id.menu_decode:
                    if (!checkModifierTasksActive()) {
                        return false;
                    }
                    RectF cropRectF = ImageUtils.getCropRectMultipliers(mImageView.getDisplayRect(),
                            mImageView.getWidth(),
                            mImageView.getHeight());
                    mDecodeImageTask = new DecodeImageTask(mPath, cropRectF);
                    mDecodeImageTask.execute();
                    break;
                case R.id.menu_delete: {
                    if (!checkModifierTasksActive()) {
                        return false;
                    }
                    AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

                    alert.setMessage(R.string.main_delete_confirmation);

                    alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            boolean success = false;
                            CommonUtils.debug(TAG, "deleting image file %1$s", mPath);
                            File f = new File(mPath);
                            success = f.delete() || !f.exists();

                            if (success) {
                                GuiUtils.alert(R.string.main_deleting_success);
                                getActivity().finish();
                            } else {
                                GuiUtils.alert(R.string.errorCantRemoveFile);
                            }
                        }
                    });

                    alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                        }
                    });

                    alert.show();
                }
                    break;
                case R.id.menu_exit:
                    getActivity().finish();
                    break;
                default:
                    return super.onContextItemSelected(item);
            }
            return true;
        }

        public boolean checkModifierTasksActive() {
            if (mDecodeImageTask != null) {
                GuiUtils.alert(R.string.main_decoding_please_wait);
                return false;
            }
            return true;
        }
        
        @Override
        public void startLoading() {
            if (mLoaders.getAndIncrement() == 0) {
                mLoadingView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void stopLoading() {
            if (mLoaders.decrementAndGet() == 0) {
                mLoadingView.setVisibility(View.GONE);
            }
        }

        @Override
        public boolean isLoading() {
            return mLoaders.get() > 0;
        }

        private class AddNewImageTask extends AbstractAddNewImageTask {

            public AddNewImageTask(String filePath, boolean moveOriginal) {
                super(filePath, mProductSku, moveOriginal,
                        PhotoViewUiFragment.this.mJobControlInterface, new SettingsSnapshot(
                                getActivity()), PhotoViewUiFragment.this);
            }

            @Override
            protected void onSuccessPostExecute() {
                GuiUtils.alert(R.string.upload_job_added_to_queue);
                Intent intent = EventBusUtils
                        .getGeneralEventIntent(EventType.JOB_ADDED_FOR_SOURCE_FILE);
                intent.putExtra(EventBusUtils.PATH, getFilePath());
                EventBusUtils.sendGeneralEventBroadcast(intent);
                if (isActivityAlive()) {
                    getActivity().finish();
                }
            }
        }

        public class DecodeImageTask extends SimpleAsyncTask {
            int mScreenLargerDimension;
            String mCode;
            String mFilePath;
            RectF mCropRectMultiplliers;
            long mExifDateTime = -1;

            public DecodeImageTask(String filePath, RectF cropRectMultipliers) {
                super(mDecodeStatusLoadingControl);
                this.mFilePath = filePath;
                this.mCropRectMultiplliers = cropRectMultipliers;
                DisplayMetrics metrics = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                mScreenLargerDimension = metrics.widthPixels;
                if (mScreenLargerDimension < metrics.heightPixels) {
                    mScreenLargerDimension = metrics.heightPixels;
                }
            }

            @Override
            protected Boolean doInBackground(Void... arg0) {
                try {
                    if (isCancelled()) {
                        return false;
                    }
                    File f = new File(mFilePath);
                    ImageData id = ImageData.getImageDataForFile(f, false);
                    Rect cropRect = ImageUtils.getRealCropRectForMultipliers(mCropRectMultiplliers,
                            id.getWidth(), id.getHeight());
                    cropRect = ImageUtils.translateRect(cropRect, id.getWidth(),
                            id.getHeight(), id.getOrientation());
                    Bitmap bitmap = ImageUtils.decodeSampledBitmapFromFile(mFilePath,
                            mScreenLargerDimension, mScreenLargerDimension, id.orientation,
                            cropRect);
                    CommonUtils
                            .debug(TAG,
                                    "DecodeImageTask.doInBackground: Bitmap region dimensions: width %1$d; height %2$d",
                            bitmap.getWidth(), bitmap.getHeight());
                    ZXingCodeScanner multiDetector = new ZXingCodeScanner();
                    mCode = multiDetector.decode(bitmap);
                    if (mCode != null) {

                        if (mCode.startsWith(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX)) {
                            mExifDateTime = ImageUtils
                                    .getExifDateTime(mFilePath);

                            if (mExifDateTime != -1) {
                                f.delete();
                            }
                        }
                    }
                    return !isCancelled();
                } catch (Exception e) {
                    GuiUtils.noAlertError(TAG, e);
                }
                return false;
            }

            void nullifyTask() {
                mDecodeImageTask = null;
            }

            void onFinish() {
                nullifyTask();
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                onFinish();
            }

            @Override
            protected void onSuccessPostExecute() {
                onFinish();
                try {
                    if (mCode != null) {
                        if (mCode.startsWith(CameraTimeSyncActivity.TIMESTAMP_CODE_PREFIX)) {
                            mCurrentBeep = MainActivity.checkConditionAndSetCameraTimeDifference(
                                    mCode, mExifDateTime, mSettings, mCurrentBeep, false, true,
                                    new Runnable() {

                                        @Override
                                        public void run() {
                                            if (isActivityAlive()) {
                                                getActivity().finish();
                                            }
                                        }
                                    });
                        } else {
                            Intent intent = EventBusUtils
                                    .getGeneralEventIntent(EventType.DECODE_RESULT);
                            intent.putExtra(EventBusUtils.CODE, mCode);
                            EventBusUtils.sendGeneralEventBroadcast(intent);
                            mCurrentBeep = SingleFrequencySoundGenerator.playSuccessfulBeep(
                                    mSettings, mCurrentBeep);
                            GuiUtils.alert(R.string.main_decoding_Image_success);
                            if (isActivityAlive()) {
                                getActivity().finish();
                            }
                        }
                    } else {
                        GuiUtils.alert(R.string.main_decoding_Image_failed);
                        mCurrentBeep = SingleFrequencySoundGenerator.playFailureBeep(mSettings,
                                mCurrentBeep);
                    }
                } catch (Exception ex) {
                    GuiUtils.error(mCode, R.string.main_decoding_Image_failed, ex);
                }
            }

            @Override
            protected void onFailedPostExecute() {
                super.onFailedPostExecute();
                onFinish();
                GuiUtils.alert(R.string.main_decoding_Image_failed);
                mCurrentBeep = SingleFrequencySoundGenerator.playFailureBeep(mSettings,
                        mCurrentBeep);
            }
        }
    }
}
