
package com.mageventory.activity;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher.OnViewTapListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
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
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.FileUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.TrackerUtils;

/**
 * Simple image view activity
 * 
 * @author Eugene Popovich
 */
public class PhotoViewActivity extends BaseFragmentActivity implements MageventoryConstants {
    private static final String TAG = PhotoViewActivity.class.getSimpleName();
    public static final String EXTRA_PATH = "PATH";
    public static final String EXTRA_URL = "URL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new PhotoViewUiFragment()).commit();
        }
    }

    PhotoViewUiFragment getContentFragment() {
        return (PhotoViewUiFragment) getSupportFragmentManager().findFragmentById(
                android.R.id.content);
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
        private TextView mFileInfo;
        private String mProductSku;
        private String mPath;
        JobControlInterface mJobControlInterface;

        private AtomicInteger mLoaders = new AtomicInteger(0);
        boolean mDetailsVisible;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mJobControlInterface = new JobControlInterface(getActivity());
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
                public void onViewTap(View view, float x, float y) {
                    TrackerUtils.trackButtonClickEvent("image", PhotoViewUiFragment.this);
                    adjustDetailsVisibility(!mDetailsVisible);
                }
            });
            mImageView.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    registerForContextMenu(v);
                    v.showContextMenu();
                    unregisterForContextMenu(v);
                    return true;
                }
            });
            mLoadingView = view.findViewById(R.id.loading);
            mFileInfo = (TextView) view.findViewById(R.id.fileInfo);
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
            if (intent.hasExtra(EXTRA_PATH)) {
                mPath = intent.getStringExtra(EXTRA_PATH);
                String url = intent.getStringExtra(EXTRA_URL);
                mProductSku = intent.getStringExtra(getString(R.string.ekey_product_sku));
                mImageWorker.loadImage(mPath, mImageView);
                try {
                    File file = new File(mPath);
                    ImageData id = ImageData.getImageDataForFile(file, false);
                    mFileInfo.setText(CommonUtils.getStringResource(
                            R.string.photo_view_overlay_size_format, id.getWidth(), id.getHeight(),
                            FileUtils.formatFileSize(file.length()), url == null ? mPath : url));
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
                inflater.inflate(R.menu.photo_view, menu);
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
                default:
                    return super.onContextItemSelected(item);
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
            }
        }
    }
}
