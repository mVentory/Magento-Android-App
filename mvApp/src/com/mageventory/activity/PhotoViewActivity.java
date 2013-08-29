
package com.mageventory.activity;

import java.util.concurrent.atomic.AtomicInteger;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.mageventory.R;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.bitmapfun.util.ImageCache;
import com.mageventory.bitmapfun.util.ImageFileSystemFetcher;
import com.mageventory.fragment.base.BaseFragmentWithImageWorker;
import com.mageventory.util.LoadingControl;

/**
 * Simple image view activity
 * 
 * @author Eugene Popovich
 */
public class PhotoViewActivity extends BaseFragmentActivity {
    private static final String TAG = PhotoViewActivity.class.getSimpleName();
    public static final String EXTRA_PATH = "PATH";

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
        private ImageView mImageView;
        private View mLoadingView;
        private AtomicInteger mLoaders = new AtomicInteger(0);

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
            mImageView = (ImageView) view.findViewById(R.id.image);
            mLoadingView = view.findViewById(R.id.loading);
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
                String path = intent.getStringExtra(EXTRA_PATH);
                mImageWorker.loadImage(path, mImageView);
            }
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
    }
}
