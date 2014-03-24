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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.MainActivity.ImageData;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.bitmapfun.util.ImageCache;
import com.mageventory.bitmapfun.util.ImageCacheUtils.AbstractClearDiskCachesTask;
import com.mageventory.bitmapfun.util.ImageFetcher;
import com.mageventory.bitmapfun.util.ImageFileSystemFetcher;
import com.mageventory.bitmapfun.util.ImageWorker;
import com.mageventory.bitmapfun.util.ImageWorker.ImageWorkerAdapter;
import com.mageventory.fragment.base.BaseFragmentWithImageWorker;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCallback;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.job.ParcelableJobDetails;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.util.EventBusUtils.GeneralBroadcastEventHandler;
import com.mageventory.util.FileUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageFlowUtils;
import com.mageventory.util.ImageFlowUtils.FlowObjectToStringWrapper;
import com.mageventory.util.ImageFlowUtils.ViewHolder;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.TrackerUtils;
import com.mageventory.widget.YesNoDialogFragment;
import com.mageventory.widget.YesNoDialogFragment.YesNoButtonPressedHandler;

/**
 * Activity which represents images library
 * 
 * @author Eugene Popovich
 */
public class LibraryActivity extends BaseFragmentActivity implements MageventoryConstants,
        GeneralBroadcastEventHandler {
    private static final String TAG = LibraryActivity.class.getSimpleName();
    public static final String IMAGE_URLS = "IMAGE_URLS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            replaceContentFragment();
        }
        EventBusUtils.registerOnGeneralEventBroadcastReceiver(TAG, this, this);
    }

    private void replaceContentFragment() {
        LibraryUiFragment fragment = findOrCreateContentFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment, fragment.getClass().getSimpleName())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            if (isWebLibrary()) {
                getContentFragment().refresh(true);
                return true;
            } else {
                if (LocalLibraryUiFragment.DeleteFilesTask.isActive()) {
                    GuiUtils.alert(R.string.errorCantRefreshFileRemovalInProgress);
                    return false;
                } else {
                    LibraryUiFragment libraryUiFragment = getContentFragment();
                    libraryUiFragment.refresh(true);
                    libraryUiFragment.updateLoadingStatus();
                    return true;
                }
            }
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    boolean isWebLibrary() {
        return getContentFragment() instanceof WebLibraryUiFragment;
    }

    boolean requireWebLibrary() {
        Bundle extras = getIntent().getExtras();
        return extras.containsKey(IMAGE_URLS);
    }

    LibraryUiFragment findOrCreateContentFragment() {
        boolean requireWebLibrary = requireWebLibrary();
        String fragmentTag = requireWebLibrary ? WebLibraryUiFragment.class.getSimpleName()
                : LocalLibraryUiFragment.class.getSimpleName();
        LibraryUiFragment result = (LibraryUiFragment) getSupportFragmentManager()
                .findFragmentByTag(fragmentTag);
        if (result == null) {
            result = requireWebLibrary ? new WebLibraryUiFragment() : new LocalLibraryUiFragment();
        }
        return result;
    }

    LibraryUiFragment getContentFragment() {
        return (LibraryUiFragment) getSupportFragmentManager().findFragmentById(
                R.id.content_frame);
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        LibraryUiFragment currentFragment = getContentFragment();
        boolean isWeb = currentFragment instanceof WebLibraryUiFragment;
        if (isWeb != requireWebLibrary()) {
            replaceContentFragment();
            GuiUtils.post(new Runnable() {
                @Override
                public void run() {
                    LibraryUiFragment currentFragment = getContentFragment();
                    currentFragment.reinitFromIntent(intent);
                }
            });
        } else {
            currentFragment.reinitFromIntent(intent);
        }
    }

    @Override
    public void onBackPressed() {
        LibraryUiFragment fragment = getContentFragment();
        boolean proceed = true;
        proceed &= !fragment.isBackKeyOverrode();
        if (proceed) {
            super.onBackPressed();
        }
    }

    @Override
    public void onGeneralBroadcastEvent(EventType eventType, Intent extra) {
        getContentFragment().onGeneralBroadcastEvent(eventType, extra);
    }

    public static class WebLibraryUiFragment extends LibraryUiFragment {
        int mMinImageSize;
        static String sCachePath = ImageCache.WEB_THUMBS_EXT_CACHE_DIR;

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mFilterText.setVisibility(View.GONE);
        }

        @Override
        void refresh(View v, boolean reloadData) {
            if (!isActivityAlive()) {
                return;
            }
            if (reloadData) {
                if (ClearWebCachesTask.isActive()) {
                    GuiUtils.alert(R.string.library_wait_cache_clear);
                } else {
                    new ClearWebCachesTask(sCachePath, ImageCache.LARGE_IMAGES_CACHE_DIR,
                            ImageFetcher.HTTP_CACHE_DIR).executeOnExecutor(Executors
                            .newSingleThreadExecutor());
                    updateClearCacheStatus();
                }
            }
            loadList();
        }

        @Override
        LibraryAdapterExt createLibraryAdapterExt() {
            final WebLibraryAdapterExt result = new WebLibraryAdapterExt();
            result.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    if (result.isEmpty()) {
                        if (noAcceptableImagesFoundView.getVisibility() == View.GONE) {
                            noAcceptableImagesFoundView
                                    .setText(R.string.no_images_of_acceptable_size_found);
                            noAcceptableImagesFoundView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (noAcceptableImagesFoundView.getVisibility() == View.VISIBLE) {
                            noAcceptableImagesFoundView.setVisibility(View.GONE);
                        }
                    }
                }
            });
            return result;
        }

        void loadList() {

            List<ImageData> data = new ArrayList<ImageData>();
            String[] urls = ClearWebCachesTask.isActive() ? new String[0] : getActivity()
                    .getIntent().getStringArrayExtra(IMAGE_URLS);
            for (String url : urls) {
                ImageDataExt id = new ImageDataExt(null, 10, 10, url);
                data.add(id);
            }
            LibraryImageWorkerAdapter adapter = new LibraryImageWorkerAdapter(data);
            loadList(adapter);
        }

        @Override
        protected void initImageWorker() {
            super.initImageWorker();
            mMinImageSize = getResources().getDimensionPixelSize(R.dimen.web_min_item_size);
            mImageWorker = new CustomImageFetcher(getActivity(), this, mImageThumbSize);
            mImageWorker.setLoadingImage(R.drawable.empty_photo);
            mImageWorker.setImageCache(ImageCache.findOrCreateCache(getActivity(), sCachePath, 50,
                    true, true));
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            if (v.getId() == R.id.image) {
                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.web, menu);
                super.onCreateContextMenu(menu, v, menuInfo);
            } else {
                super.onCreateContextMenu(menu, v, menuInfo);
            }
        }

        @Override
        protected void extraViewIntentInit(Intent intent) {
            super.extraViewIntentInit(intent);
            intent.putExtra(PhotoViewActivity.EXTRA_URL, ((ImageDataExt) mCurrentData).url);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            int menuItemIndex = item.getItemId();
            switch (menuItemIndex) {
                case R.id.menu_view:
                case R.id.menu_upload:
                    if (mCurrentData.getFile() == null) {
                        GuiUtils.alert(R.string.image_not_loaded);
                        return false;
                    } else {
                        return super.onContextItemSelected(item);
                    }
                default:
                    return super.onContextItemSelected(item);
            }
        }

        @Override
        boolean isClearingCache() {
            return ClearWebCachesTask.isActive();
        }

        @Override
        public boolean isCurrentCachePath(String path) {
            return sCachePath.equals(path);
        }

        @Override
        void onNewImageTaskAdded(String filePath) {
            super.onNewImageTaskAdded(filePath);
            if (isActivityAlive() && isVisible()) {
                if (mLibraryAdapter.getSuperCount() == 1) {
                    ImageData id = mLibraryAdapter.getSuperItem(0);
                    if (id.getFile() != null && filePath.equals(id.getFile().getAbsolutePath())) {
                        getActivity().finish();
                    }
                }
            }
        }

        @Override
        public void onGeneralBroadcastEvent(EventType eventType, Intent extra) {
            switch (eventType) {
                case JOB_ADDED_FOR_SOURCE_FILE:
                    String filePath = extra.getStringExtra(EventBusUtils.PATH);
                    onNewImageTaskAdded(filePath);
                    break;
                default:
                    super.onGeneralBroadcastEvent(eventType, extra);
            }
        }

        public static class ClearWebCachesTask extends ClearCachesTask {
            static AtomicInteger activeCounter = new AtomicInteger(0);

            public static boolean isActive() {
                return activeCounter.get() > 0;
            }

            @Override
            protected AtomicInteger getActiveCounter() {
                return activeCounter;
            }

            public ClearWebCachesTask(String... cachesToClear) {
                super(cachesToClear);
            }

        }

        static class ImageDataExt extends ImageData {
            String url;

            public ImageDataExt(File file, int width, int height, String url) {
                super(file, width, height);
                this.url = url;
            }

            @Override
            public String toString() {
                return file == null ? url : super.toString();
            }
        }

        private class WebLibraryAdapterExt extends LibraryAdapterExt {
            @Override
            public void loadImage(ImageData photo, ImageView imageView) {
                ImageDataExt ext = (ImageDataExt) photo;
                FlowObjectToStringWrapper<ImageData> fo = new FlowObjectToStringWrapper<ImageData>(
                        ext, ext.url);
                mImageWorker.loadImage(fo, imageView);
            }

            @Override
            public void additionalSingleImageViewInit(View view, final ImageData value) {
                super.additionalSingleImageViewInit(view, value);
                ViewHolderExt viewHolder = (ViewHolderExt) view.getTag();
                if (value.getFile() == null) {
                    viewHolder.mSizeInfo.setVisibility(View.VISIBLE);
                    ImageDataExt ide = (ImageDataExt) value;
                    viewHolder.mSizeInfo.setText(ide.url);
                }
            }
        }

        private class CustomImageFetcher extends ImageFetcher {

            public CustomImageFetcher(Context context, LoadingControl loadingControl, int size) {
                super(context, loadingControl, size);
            }

            @SuppressWarnings("unchecked")
            @Override
            protected Bitmap processBitmap(Object data, ProcessingState state) {
                Bitmap result = super.processBitmap(data, state);
                FlowObjectToStringWrapper<ImageData> fo = (FlowObjectToStringWrapper<ImageData>) data;
                final ImageData imageData = fo.getObject();
                File f = sLastFile.get();
                try {
                    if (f != null) {
                        ImageData imageData2 = ImageData.getImageDataForFile(f, false);
                        imageData.setFile(imageData2.getFile());
                        imageData.setWidth(imageData2.getWidth());
                        imageData.setHeight(imageData2.getHeight());

                    }
                    GuiUtils.post(new Runnable() {

                        @Override
                        public void run() {
                            if (imageData.getFile() == null || imageData.getWidth() < mMinImageSize
                                    || imageData.getHeight() < mMinImageSize) {
                                CommonUtils
                                        .debug(TAG,
                                                "CustomImageFetcher.processBitmap: image %1$s is smaller than %2$d minimum size. Removing",
                                                ((ImageDataExt) imageData).url, mMinImageSize);
                                LibraryImageWorkerAdapter adapter = (LibraryImageWorkerAdapter) mImageWorker
                                        .getAdapter();
                                adapter.removeItem(imageData);
                            }
                            mLibraryAdapter.notifyDataSetChangedFourceRebuild();
                        }
                    });
                } catch (Exception ex) {
                    GuiUtils.noAlertError(TAG, ex);
                }
                return result;
            }

        }
    }

    public static class LocalLibraryUiFragment extends LibraryUiFragment {
        static String sCachePath = ImageCache.LOCAL_THUMBS_EXT_CACHE_DIR;

        @Override
        protected void initImageWorker() {
            super.initImageWorker();
            mImageWorker = new ImageFileSystemFetcher(getActivity(), null, mImageThumbSize);
            mImageWorker.setLoadingImage(R.drawable.empty_photo);
            mImageWorker.setImageCache(ImageCache.findOrCreateCache(getActivity(), sCachePath,
                    1500, true, false));
        }

        @Override
        public void refresh() {
            boolean reloadData = (LoadFilesListTask.sCachedImageDataList != null && LoadFilesListTask.sCachedImageDataList
                    .isEmpty())
                    || LoadFilesListTask.sLastLoadedTime.get() < MainActivity.ImagesObserver.sLastUpdatedTime
                            .get();
            refresh(reloadData);
        }

        @Override
        void refresh(View v, boolean reloadData) {
            if (!isActivityAlive()) {
                return;
            }
            if (reloadData) {
                clearLoadedImageDataCacheAndLoadList();
            } else {
                loadList();
            }
        }

        void clearLoadedImageDataCacheAndLoadList() {
            if (!LoadFilesListTask.isActive()) {
                if (DeleteFilesTask.isActive()) {
                    GuiUtils.alert(R.string.library_wait_files_removal);
                } else if (ClearLocalCachesTask.isActive()) {
                    GuiUtils.alert(R.string.library_wait_cache_clear);
                } else {
                    LoadFilesListTask.sCachedImageDataList = null;
                    new ClearLocalCachesTask(sCachePath, ImageCache.LARGE_IMAGES_CACHE_DIR)
                            .executeOnExecutor(Executors.newSingleThreadExecutor());
                    updateClearCacheStatus();
                    loadList();
                }
            }
        }

        @Override
        LibraryAdapterExt createLibraryAdapterExt() {
            final LibraryAdapterExt result = new LocalLibraryAdapterExt();
            result.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    if (result.isEmpty() && !LoadFilesListTask.isActive()) {
                        LibraryImageWorkerAdapter adapter = (LibraryImageWorkerAdapter) mImageWorker.getAdapter();
                        String message = adapter.mFilteredIndexes == null ? CommonUtils
                                .getStringResource(R.string.no_local_images_found,
                                        mSettings.getGalleryPhotosDirectory()) : CommonUtils
                                .getStringResource(R.string.no_local_images_found_for_filter);
                        noAcceptableImagesFoundView.setText(message);
                        if (noAcceptableImagesFoundView.getVisibility() == View.GONE) {
                            noAcceptableImagesFoundView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (noAcceptableImagesFoundView.getVisibility() == View.VISIBLE) {
                            noAcceptableImagesFoundView.setVisibility(View.GONE);
                        }
                    }
                }
            });
            return result;
        }

        void loadList() {

            if (LoadFilesListTask.sCachedImageDataList == null && !LoadFilesListTask.isActive()) {
                new LoadFilesListTask(mSettings).execute();
            }
            List<ImageData> data = LoadFilesListTask.sCachedImageDataList;
            if (ClearLocalCachesTask.isActive()) {
                data = null;
            }
            LibraryImageWorkerAdapter adapter = new LibraryImageWorkerAdapter(data);
            loadList(adapter);
            filterList();
        }

        @Override
        void updateRemovalStatus() {
            super.updateRemovalStatus();
            if (isResumed()) {
                mRemovalStatusLine.setVisibility(DeleteFilesTask.isActive() ? View.VISIBLE
                        : View.GONE);
            }
        }

        @Override
        void updateLoadingStatus() {
            super.updateLoadingStatus();
            if (isResumed()) {
                mLoadLibraryStatusLine.setVisibility(LoadFilesListTask.isActive() ? View.VISIBLE
                        : View.GONE);
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            if (v.getId() == R.id.image) {
                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.library, menu);
                super.onCreateContextMenu(menu, v, menuInfo);
            } else {
                super.onCreateContextMenu(menu, v, menuInfo);
            }
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            int menuItemIndex = item.getItemId();
            switch (menuItemIndex) {
                case R.id.menu_delete_all: {
                    YesNoDialogFragment dialogFragment = YesNoDialogFragment.newInstance(
                            CommonUtils.getStringResource(R.string.image_delete_all_confirmation,
                                    mLibraryAdapter.getCount()), new YesNoButtonPressedHandler() {
                                @Override
                                public void yesButtonPressed(DialogInterface dialog) {
                                    TrackerUtils.trackUiEvent("deleteImagesConfirmation",
                                            "Confirmed");
                                    new DeleteAllDisplayedImagesTask().execute();
                                }

                                @Override
                                public void noButtonPressed(DialogInterface dialog) {
                                    TrackerUtils.trackUiEvent("deleteImagesConfirmation",
                                            "Discarded");
                                }
                            });
                    dialogFragment.show(getActivity().getSupportFragmentManager(), dialogFragment
                            .getClass().getSimpleName());
                }
                    break;
                default:
                    return super.onContextItemSelected(item);
            }
            return true;
        }

        @Override
        public boolean isBackKeyOverrode() {
            String filter = mFilterText.getText().toString();
            if (TextUtils.isEmpty(filter)) {
                return super.isBackKeyOverrode();
            } else {
                mFilterText.setText(null);
                filterList();
                return true;
            }
        }

        @Override
        boolean isClearingCache() {
            return ClearLocalCachesTask.isActive();
        }

        @Override
        public boolean isCurrentCachePath(String path) {
            return sCachePath.equals(path);
        }

        @Override
        public void onGeneralBroadcastEvent(EventType eventType, Intent extra) {
            switch (eventType) {
                case LIBRARY_FILES_DELETED:
                    updateRemovalStatus();
                    break;
                case LIBRARY_DATA_LOADED:
                    if (isResumed()) {
                        loadList();
                    }
                    updateLoadingStatus();
                    break;
                default:
                    super.onGeneralBroadcastEvent(eventType, extra);
                    break;
            }
        }

        private class LocalLibraryAdapterExt extends LibraryAdapterExt {
            @Override
            public void loadImage(ImageData photo, ImageView imageView) {
                FlowObjectToStringWrapper<ImageData> fo = new FlowObjectToStringWrapper<ImageData>(
                        photo, photo.getFile().getAbsolutePath());
                mImageWorker.loadImage(fo, imageView);
            }
        }

        private class DeleteAllDisplayedImagesTask extends SimpleAsyncTask {
            List<File> mFilesToDelete = new ArrayList<File>();
            List<Integer> mItems;
            LibraryImageWorkerAdapter mAdapter;

            public DeleteAllDisplayedImagesTask() {
                super(LocalLibraryUiFragment.this);
                mAdapter = (LibraryImageWorkerAdapter) mImageWorker.getAdapter();
                mItems = mAdapter.mFilteredIndexes;
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {

                    synchronized (mAdapter) {
                        int size = mItems == null ? mAdapter.mData.size() : mItems.size();
                        for (int i = size - 1; i >= 0; i--) {
                            ImageData id = mAdapter.mData.get(mItems == null ? i : mItems.get(i));
                            mAdapter.removeItem(id);
                            mFilesToDelete.add(id.getFile());
                        }
                    }
                    return true;
                } catch (Exception ex) {
                    GuiUtils.error(TAG, R.string.errorCantRemoveFiles, ex);
                }
                return false;
            }

            @Override
            protected void onSuccessPostExecute() {
                mLibraryAdapter.notifyDataSetChangedFourceRebuild();
                new DeleteFilesTask(mFilesToDelete).executeOnExecutor(Executors
                        .newSingleThreadExecutor());
                updateRemovalStatus();
                if (mItems == null) {
                    getActivity().finish();
                } else {
                    mFilterText.setText(null);
                    filterList();
                }
            }
        }

        public static class DeleteFilesTask extends SimpleAsyncTask {
            List<File> mFilesToDelete;
            static AtomicInteger activeCounter = new AtomicInteger(0);

            public static boolean isActive() {
                return activeCounter.get() > 0;
            }

            public DeleteFilesTask(List<File> filesToDelete) {
                super(null);
                this.mFilesToDelete = filesToDelete;
                activeCounter.incrementAndGet();
            }

            @Override
            protected void onSuccessPostExecute() {
                GuiUtils.alert(R.string.files_successfully_removed);
                activeCounter.decrementAndGet();
                EventBusUtils.sendGeneralEventBroadcast(EventType.LIBRARY_FILES_DELETED);
            }

            @Override
            protected void onFailedPostExecute() {
                super.onFailedPostExecute();
                activeCounter.decrementAndGet();
                EventBusUtils.sendGeneralEventBroadcast(EventType.LIBRARY_FILES_DELETED);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    for (File f : mFilesToDelete) {
                        if (f.exists() && !f.delete()) {
                            GuiUtils.alert(R.string.errorCantRemoveFiles);
                            return false;
                        }
                    }
                    return true;
                } catch (Exception ex) {
                    GuiUtils.error(TAG, R.string.errorCantRemoveFiles, ex);
                }
                return false;
            }
        }

        public static class ClearLocalCachesTask extends ClearCachesTask {
            static AtomicInteger activeCounter = new AtomicInteger(0);

            public static boolean isActive() {
                return activeCounter.get() > 0;
            }

            @Override
            protected AtomicInteger getActiveCounter() {
                return activeCounter;
            }

            public ClearLocalCachesTask(String... cachesToClear) {
                super(cachesToClear);
            }

        }

        public static class LoadFilesListTask extends SimpleAsyncTask {

            static AtomicLong sLastLoadedTime = new AtomicLong(0);

            static List<ImageData> sCachedImageDataList = null;
            List<ImageData> data;
            static AtomicInteger activeCounter = new AtomicInteger(0);
            Settings mSettings;

            public static boolean isActive() {
                return activeCounter.get() > 0;
            }

            public LoadFilesListTask(Settings settings) {
                super(null);
                activeCounter.incrementAndGet();
                sCachedImageDataList = null;
                this.mSettings = settings;
            }

            @Override
            protected void onSuccessPostExecute() {
                sCachedImageDataList = data;
                activeCounter.decrementAndGet();
                EventBusUtils.sendGeneralEventBroadcast(EventType.LIBRARY_DATA_LOADED);
            }

            @Override
            protected void onFailedPostExecute() {
                super.onFailedPostExecute();
                sCachedImageDataList = new ArrayList<ImageData>();
                activeCounter.decrementAndGet();
                EventBusUtils.sendGeneralEventBroadcast(EventType.LIBRARY_DATA_LOADED);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    CommonUtils.debug(TAG, "LoadFilesListTask.doInBackground: started");
                    String imagesDirPath = mSettings.getGalleryPhotosDirectory();
                    data = new ArrayList<ImageData>();
                    if (TextUtils.isEmpty(imagesDirPath)) {
                        return false;
                    }

                    File f = new File(imagesDirPath);

                    List<File> files = new ArrayList<File>();

                    processDirectory(f, files);

                    Collections.sort(files, new Comparator<File>() {
                        @Override
                        public int compare(File lhs, File rhs) {
                            return lhs.getAbsolutePath().toLowerCase()
                                    .compareTo(rhs.getAbsolutePath().toLowerCase());
                        }
                    });
                    for (File file : files) {
                        data.add(ImageData.getImageDataForFile(file, false));
                    }
                    sLastLoadedTime.set(SystemClock.elapsedRealtime());
                    return true;
                } catch (Exception ex) {
                    GuiUtils.error(TAG, R.string.errorCantReadFilesList, ex);
                }
                return false;
            }

            private void processDirectory(File dir, List<File> allFiles) {
                if (dir.getName().equals(".") || dir.getName().equals("..")) {
                    return;
                }
                File[] files = dir.listFiles();
                if (files == null || files.length == 0) {
                    return;
                }
                for (File f : files) {
                    if (f.isDirectory()) {
                        processDirectory(f, allFiles);
                    } else {
                        if (f.getName().toLowerCase().contains(".jpg")) {
                            allFiles.add(f);
                        } else {
                            String type = FileUtils.getMimeType(f);
                            if (type != null && type.toLowerCase().startsWith("image/")) {
                                allFiles.add(f);
                            }
                        }
                    }
                }
            }
        }
    }

    public abstract static class LibraryUiFragment extends BaseFragmentWithImageWorker implements
            LoadingControl, GeneralBroadcastEventHandler {

        static final int MAX_FILTER_ADAPTER_CAPACITY = 5;
        static List<String> sLastFilterItemsCache = new ArrayList<String>();
        ArrayAdapter<String> mFilterAdapter;

        Settings mSettings;
        JobControlInterface mJobControlInterface;
        int mImageThumbSize;
        int mImageThumbSpacing;
        int mImageThumbBorder;

        AtomicInteger mLoaders = new AtomicInteger(0);
        View mLoadingView;
        View mRemovalStatusLine;
        View mClearCacheStatusLine;
        View mLoadLibraryStatusLine;
        View mUploadStatusLine;
        TextView noAcceptableImagesFoundView;
        AutoCompleteTextView mFilterText;
        TextView mUploadStatusText;
        ListView mLibraryList;
        LibraryAdapterExt mLibraryAdapter;

        ViewTreeObserver.OnGlobalLayoutListener mLibraryGlobalOnLayoutListener;

        ImageData mCurrentData;
        String mProductSku;

        List<Job> mUploadImageJobs = new ArrayList<Job>();
        UploadImageJobCallback mUploadImageJobCallback = new UploadImageJobCallback();
        FilterItemsTask mFilterItemsTask;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mSettings = new Settings(getActivity().getApplicationContext());
            mJobControlInterface = new JobControlInterface(getActivity());
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            View view = inflater.inflate(R.layout.library, container, false);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mLibraryList = (ListView) view.findViewById(R.id.list_photos);
            mLoadingView = view.findViewById(R.id.loading);
            noAcceptableImagesFoundView = (TextView) view.findViewById(
                    R.id.no_acceptable_images_found);
            mUploadStatusLine = view.findViewById(R.id.uploadStatusLine);
            mUploadStatusText = (TextView) view.findViewById(R.id.uploadStatusText);
            mRemovalStatusLine = view.findViewById(R.id.removalStatusLine);
            mClearCacheStatusLine = view.findViewById(R.id.clearCacheStatusLine);
            mLoadLibraryStatusLine = view.findViewById(R.id.loadLibraryStatusLine);
            mFilterText = (AutoCompleteTextView) view.findViewById(R.id.filter_query);
            initFilterText();
            reinitFromIntent(getActivity().getIntent());

            if (mLibraryGlobalOnLayoutListener != null) {
                GuiUtils.removeGlobalOnLayoutListener(mLibraryList, mLibraryGlobalOnLayoutListener);
            }
            mLibraryGlobalOnLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                int lastHeight = 0;

                @Override
                public void onGlobalLayout() {
                    if (mLibraryAdapter != null
                            && (mLibraryAdapter.imageFlowUtils.getTotalWidth() != mLibraryList
                                    .getWidth() || mLibraryList.getHeight() != lastHeight)) {
                        CommonUtils.debug(TAG, "Reinit grid groups");
                        mLibraryAdapter.imageFlowUtils.onGroupsStructureModified();
                        mLibraryAdapter.imageFlowUtils.buildGroups(mLibraryList.getWidth(),
                                mImageThumbSize, mLibraryList.getHeight() - 2
                                        * (mImageThumbBorder + mImageThumbSpacing),
                                mImageThumbBorder + mImageThumbSpacing);
                        mLibraryAdapter.notifyDataSetChanged();
                        lastHeight = mLibraryList.getHeight();
                    }
                }
            };
            mLibraryList.getViewTreeObserver().addOnGlobalLayoutListener(
                    mLibraryGlobalOnLayoutListener);

            refresh();
        }

        private void initFilterText() {
            mFilterAdapter = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_dropdown_item_1line, sLastFilterItemsCache);
            mFilterText.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mFilterText.showDropDown();
                }
            });
            mFilterText.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                private Timer timer = new Timer();

                @Override
                public void afterTextChanged(final Editable s) {
                    timer.cancel();
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            GuiUtils.post(new Runnable() {

                                @Override
                                public void run() {
                                    String filter = mFilterText.getText().toString();
                                    if (!TextUtils.isEmpty(filter)) {
                                        boolean modified = false;
                                        int count = sLastFilterItemsCache.size();
                                        int foundIndex = count;
                                        String lcFilter = filter.toLowerCase();
                                        for (int i = 0; i < count; i++) {
                                            String item = sLastFilterItemsCache.get(i)
                                                    .toLowerCase();
                                            if (item.startsWith(lcFilter)) {
                                                foundIndex = -1;
                                                break;
                                            } else if (lcFilter.startsWith(item)) {
                                                foundIndex = i;
                                                break;
                                            }
                                        }
                                        if (foundIndex != -1 && foundIndex < count) {
                                            sLastFilterItemsCache.remove(foundIndex);
                                            sLastFilterItemsCache.add(filter);
                                            modified = true;
                                        } else if (foundIndex == count) {
                                            if (count >= MAX_FILTER_ADAPTER_CAPACITY) {
                                                sLastFilterItemsCache.remove(0);
                                            }
                                            sLastFilterItemsCache.add(filter);
                                            modified = true;
                                        }
                                        if (modified) {
                                            mFilterAdapter = new ArrayAdapter<String>(
                                                    getActivity(),
                                                    android.R.layout.simple_dropdown_item_1line,
                                                    sLastFilterItemsCache);
                                            mFilterText.setAdapter(mFilterAdapter);
                                        }

                                    }
                                    filterList();
                                }
                            });
                        }

                    }, 2000);
                }
            });
            mFilterText.setOnEditorActionListener(new OnEditorActionListener() {

                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (event != null) {
                        switch (event.getKeyCode()) {
                            case KeyEvent.KEYCODE_ENTER:
                                CommonUtils.debug(TAG, "Key code enter");
                                if (KeyEvent.ACTION_DOWN == event.getAction()) {
                                    CommonUtils.debug(TAG, "Applying filter");
                                    GuiUtils.hideKeyboard(getView());
                                    filterList();
                                    return true;
                                }
                                break;
                        }
                    }
                    return false;
                }
            });

            mFilterText.setAdapter(mFilterAdapter);
        }

        public void reinitFromIntent(Intent intent) {
            mProductSku = intent.getExtras().getString(getString(R.string.ekey_product_sku));
            mJobControlInterface.unregisterJobCallbacks(mUploadImageJobs);
            mUploadImageJobs.clear();
            mUploadImageJobs.addAll(mJobControlInterface.getAllImageUploadJobs(mProductSku,
                    mSettings.getUrl()));
            updateUploadStatus();
        }

        @Override
        protected void initImageWorker() {
            mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.library_item_size);
            mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.library_item_spacing);
            mImageThumbBorder = getResources().getDimensionPixelSize(R.dimen.library_item_border);
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            GuiUtils.removeGlobalOnLayoutListener(mLibraryList, mLibraryGlobalOnLayoutListener);
        }

        public void refresh() {
            refresh(false);
        }

        public void refresh(boolean reloadData) {
            refresh(getView(), reloadData);
        }

        abstract void refresh(View v, boolean reloadData);

        void filterList() {
            if (mFilterItemsTask != null) {
                mFilterItemsTask.cancel(true);
            }
            mFilterItemsTask = new FilterItemsTask(mFilterText.getText().toString(),
                    (LibraryImageWorkerAdapter) mImageWorker.getAdapter());
            mFilterItemsTask.execute();
        }

        abstract LibraryAdapterExt createLibraryAdapterExt();

        void loadList(LibraryImageWorkerAdapter adapter) {
            mImageWorker.setAdapter(adapter);
            if (mLibraryAdapter == null) {
                mLibraryAdapter = createLibraryAdapterExt();
                mLibraryList.setAdapter(mLibraryAdapter);
            } else {
                mLibraryAdapter.notifyDataSetChangedFourceRebuild();
            }
            filterList();
        }

        protected void extraViewIntentInit(Intent intent) {

        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            int menuItemIndex = item.getItemId();
            switch (menuItemIndex) {
                case R.id.menu_view:
                    Intent intent = new Intent(getActivity(), PhotoViewActivity.class);
                    intent.putExtra(PhotoViewActivity.EXTRA_PATH, mCurrentData.getFile()
                            .getAbsolutePath());
                    intent.putExtra(getString(R.string.ekey_product_sku), mProductSku);
                    extraViewIntentInit(intent);
                    startActivity(intent);
                    break;
                case R.id.menu_upload:
                    new AddNewImageTask(mCurrentData.getFile().getAbsolutePath(), false).execute();
                    break;
                case R.id.menu_delete: {
                    YesNoDialogFragment dialogFragment = YesNoDialogFragment.newInstance(
                            R.string.image_delete_confirmation, new YesNoButtonPressedHandler() {
                                @Override
                                public void yesButtonPressed(DialogInterface dialog) {
                                    TrackerUtils.trackUiEvent("deleteImageConfirmation",
                                            "Confirmed");
                                    if (mCurrentData.getFile().delete()
                                            || !mCurrentData.getFile().exists()) {
                                        removeCurrentItem();
                                    } else {
                                        GuiUtils.alert(R.string.errorCantRemoveFile);
                                    }
                                }

                                @Override
                                public void noButtonPressed(DialogInterface dialog) {
                                    TrackerUtils.trackUiEvent("deleteImageConfirmation",
                                            "Discarded");
                                }
                            });
                    dialogFragment.show(getActivity().getSupportFragmentManager(), dialogFragment
                            .getClass().getSimpleName());
                }
                    break;
                case R.id.menu_upload_and_delete:
                    removeCurrentItem();
                    new AddNewImageTask(mCurrentData.getFile().getAbsolutePath(), true).execute();
                    break;
                default:
                    return super.onContextItemSelected(item);
            }
            return true;
        }

        private void removeCurrentItem() {
            LibraryImageWorkerAdapter adapter = (LibraryImageWorkerAdapter) mImageWorker
                    .getAdapter();
            adapter.removeItem(mCurrentData);
            mLibraryAdapter.notifyDataSetChangedFourceRebuild();
        }

        @Override
        public void onResume() {
            super.onResume();
            mJobControlInterface.registerJobCallbacksAndRemoveAbsentJobs(mUploadImageJobs,
                    mUploadImageJobCallback);
            updateLoadingStatus();
            updateUploadStatus();
            updateRemovalStatus();
            updateClearCacheStatus();
        }

        @Override
        public void onPause() {
            super.onPause();
            mJobControlInterface.unregisterJobCallbacks(mUploadImageJobs);
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

        private void updateUploadStatus() {
            if (isResumed()) {
                mUploadStatusLine.setVisibility(mUploadImageJobs.isEmpty() ? View.GONE
                        : View.VISIBLE);
                mUploadStatusText.setText(CommonUtils.getStringResource(
                        R.string.upload_queue_status,
                        CommonUtils.formatNumber(mUploadImageJobs.size())));
            }
        }

        void updateRemovalStatus() {
        }

        void updateClearCacheStatus() {
            if (isResumed()) {
                mClearCacheStatusLine.setVisibility(isClearingCache() ? View.VISIBLE : View.GONE);
            }
        }

        boolean isClearingCache() {
            return false;
        }

        void updateLoadingStatus() {
        }

        /**
         * @return true if we have custom behavior of back button pressed
         */
        public boolean isBackKeyOverrode() {
            return false;
        }

        public boolean isCurrentCachePath(String path) {
            return false;
        }

        @Override
        public void onGeneralBroadcastEvent(EventType eventType, Intent extra) {
            switch (eventType) {
                case LIBRARY_CACHE_CLEARED:
                    updateClearCacheStatus();
                    String path = extra.getStringExtra(EventBusUtils.PATH);
                    if (path != null && mImageWorker != null && isCurrentCachePath(path)) {
                        clearImageWorkerCaches(false);
                    }
                    refresh(false);
                    break;
                case LIBRARY_CACHE_CLEAR_FAILED:
                    updateClearCacheStatus();
                    break;
                case JOB_ADDED:
                    CommonUtils.debug(TAG, "onGeneralBroadcastEvent: received job added event");
                    ParcelableJobDetails job = extra.getParcelableExtra(EventBusUtils.JOB);
                    if (job != null) {
                        if (job.getJobId().getJobType() == RES_UPLOAD_IMAGE
                                && mProductSku.equals(job.getJobId().getSKU())) {
                            CommonUtils.debug(TAG,
                                    "onGeneralBroadcastEvent: upload image job added event");
                            boolean found = false;
                            for (int i = 0, size = mUploadImageJobs.size(); i < size; i++) {
                                Job j = mUploadImageJobs.get(i);
                                if (j.getJobID().getTimeStamp() == job.getJobId().getTimeStamp()) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                CommonUtils
                                        .debug(TAG,
                                                "onGeneralBroadcastEvent: upload image job not found. Updating list and status.");
                                if (isResumed()) {
                                    mJobControlInterface.unregisterJobCallbacks(mUploadImageJobs);
                                }
                                mUploadImageJobs.clear();
                                mUploadImageJobs.addAll(mJobControlInterface.getAllImageUploadJobs(
                                        mProductSku, mSettings.getUrl()));
                                if (isResumed()) {
                                    mJobControlInterface.registerJobCallbacksAndRemoveAbsentJobs(
                                            mUploadImageJobs, mUploadImageJobCallback);
                                }
                                updateUploadStatus();
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        public abstract static class ClearCachesTask extends AbstractClearDiskCachesTask {
            public ClearCachesTask(String... cachesToClear) {
                super(EventType.LIBRARY_CACHE_CLEARED, EventType.LIBRARY_CACHE_CLEAR_FAILED,
                        cachesToClear);
            }
        }

        public class LibraryImageWorkerAdapter extends ImageWorkerAdapter {
            private List<ImageData> mData;
            private List<Integer> mFilteredIndexes;
            private String mFilter;

            LibraryImageWorkerAdapter(List<ImageData> data) {
                if (data == null) {
                    data = new ArrayList<ImageData>();
                }
                this.mData = data;
                mFilteredIndexes = null;
            }

            @Override
            public Object getItem(int num) {
                return mData.get(getCorrectIndex(num));
            }

            private int getCorrectIndex(int num) {
                if (mFilteredIndexes != null) {
                    num = mFilteredIndexes.get(num);
                }
                return num;
            }

            @Override
            public int getSize() {
                return mFilteredIndexes == null ? mData.size() : mFilteredIndexes.size();
            }

            public boolean removeItem(ImageData imageData) {
                int ix = mData.indexOf(imageData);
                boolean result = false;
                if (ix != -1) {
                    mData.remove(ix);
                    if (mFilteredIndexes != null) {
                        int i = mFilteredIndexes.size() - 1;
                        while (i >= 0) {
                            int ix2 = mFilteredIndexes.get(i);
                            if (ix2 > ix) {
                                mFilteredIndexes.set(i, ix2 - 1);
                            } else if (ix2 == ix) {
                                result = true;
                                mFilteredIndexes.remove(i);
                            } else {
                                break;
                            }
                            i--;
                        }
                    }
                }
                return result;
            }

            public void setFilterInformation(String filter, List<Integer> fileredIndexes) {
                this.mFilter = filter;
                this.mFilteredIndexes = fileredIndexes;
            }

        }

        private class FilterItemsTask extends SimpleAsyncTask {
            LibraryImageWorkerAdapter mAdapter;
            String mFilter;
            List<Integer> mFilteredIndexes;
            boolean mSameFilter = false;

            public FilterItemsTask(String filter, LibraryImageWorkerAdapter adapter) {
                super(LibraryUiFragment.this);
                this.mFilter = filter;
                this.mAdapter = adapter;
                CommonUtils.debug(TAG, "FilterItemsTask: constructor");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    if (isCancelled()) {
                        return false;
                    }
                    CommonUtils.debug(TAG, "FilterItemsTask.doInBackground: started");
                    if (mFilter != null) {
                        mFilter = mFilter.trim().toLowerCase();
                    }
                    if (mFilter.equals(mAdapter.mFilter)) {
                        CommonUtils.debug(TAG,
                                "FilterItemsTask.doInBackground: same filter, skipping");
                        mFilteredIndexes = mAdapter.mFilteredIndexes;
                        mSameFilter = true;
                    } else {
                        if (TextUtils.isEmpty(mFilter)) {
                            CommonUtils.debug(TAG, "FilterItemsTask.doInBackground: empty filter");
                            mFilteredIndexes = null;
                        } else {
                            mFilteredIndexes = new ArrayList<Integer>();
                            String[] words = mFilter.split("\\s+");
                            for (int i = 0, size = mAdapter.mData.size(); i < size; i++) {
                                ImageData id = mAdapter.mData.get(i);
                                String fileName = id.getFile().getAbsolutePath().toLowerCase();
                                boolean matched = true;
                                for (String word : words) {
                                    if (fileName.indexOf(word) == -1) {
                                        matched = false;
                                        break;
                                    }
                                }
                                if (matched) {
                                    mFilteredIndexes.add(i);
                                }
                                if (isCancelled()) {
                                    return false;
                                }
                            }
                        }
                    }
                    return !isCancelled();
                } catch (Exception ex) {
                    GuiUtils.noAlertError(TAG, ex);
                }
                return false;
            }

            @Override
            protected void onSuccessPostExecute() {
                if (!mSameFilter && !isCancelled()) {
                    if (isActivityAlive()) {
                        mAdapter.setFilterInformation(mFilter, mFilteredIndexes);
                        mLibraryAdapter.notifyDataSetChangedFourceRebuild();
                    }
                }
            }
        }

        public class ViewHolderExt extends ViewHolder {
            TextView mSizeInfo;
        }

        /**
         * Extended adapter which uses photo groups as items instead of
         * ImageDatas
         */
        public abstract class LibraryAdapterExt extends LibraryAdapter {
            ImageFlowUtils<ImageData> imageFlowUtils;

            public LibraryAdapterExt() {
                super(getActivity(), mImageWorker);
                init();
            }

            void init() {
                imageFlowUtils = new ImageFlowUtils<ImageData>() {

                    @Override
                    public int getHeight(ImageData object) {
                        return object.getHeight();
                    }

                    @Override
                    public int getWidth(ImageData object) {
                        return object.getWidth();
                    }

                    @Override
                    public int getSuperCount() {
                        return LibraryAdapterExt.this.getSuperCount();
                    }

                    @Override
                    public ImageData getSuperItem(int position) {
                        return LibraryAdapterExt.this.getSuperItem(position);
                    }

                    @Override
                    public void additionalSingleImageViewInit(View view, final ImageData value) {
                        super.additionalSingleImageViewInit(view, value);
                        LibraryAdapterExt.this.additionalSingleImageViewInit(view, value);
                    }

                    @Override
                    public void loadImage(final ImageData photo, final ImageView imageView) {
                        LibraryAdapterExt.this.loadImage(photo, imageView);
                    }

                    @Override
                    public ImageFlowUtils.ViewHolder creatViewHolder(View view) {
                        ViewHolderExt result = new ViewHolderExt();
                        result.mSizeInfo = (TextView) view.findViewById(R.id.sizeInfo);
                        return result;
                    }
                };
            }

            public abstract void loadImage(final ImageData photo, final ImageView imageView);

            public void additionalSingleImageViewInit(View view, final ImageData value) {
                ViewHolderExt viewHolder = (ViewHolderExt) view.getTag();
                ImageView imageView = viewHolder.getImageView();
                imageView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        TrackerUtils.trackLongClickEvent("image", LibraryUiFragment.this);
                        mCurrentData = value;
                        registerForContextMenu(v);
                        v.showContextMenu();
                        unregisterForContextMenu(v);
                    }
                });
                if (value.getFile() != null) {
                    viewHolder.mSizeInfo.setVisibility(View.VISIBLE);
                    viewHolder.mSizeInfo.setText(CommonUtils.getStringResource(
                            R.string.library_overlay_size_format, value.getWidth(),
                            value.getHeight(), FileUtils.formatFileSize(value.getFile().length())));
                } else {
                    viewHolder.mSizeInfo.setVisibility(View.GONE);
                }
            }

            public int getSuperCount() {
                return super.getCount();
            }

            ImageData getSuperItem(int position) {
                return (ImageData) super.getItem(position);
            }

            @Override
            public int getCount() {
                return imageFlowUtils == null ? 0 : imageFlowUtils.getGroupsCount();
            }

            @Override
            public Object getItem(int num) {
                return imageFlowUtils.getGroupItem(num);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return imageFlowUtils.getView(position, convertView, parent,
                        R.layout.library_item_image_line, R.layout.library_item_image, R.id.image,
                        mContext);
            }

            public void notifyDataSetChangedFourceRebuild() {
                imageFlowUtils.onGroupsStructureModified();
                notifyDataSetChanged();
            }

            @Override
            public void notifyDataSetChanged() {
                imageFlowUtils.rebuildGroups();
                super.notifyDataSetChanged();
            }

            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public boolean isEnabled(int position) {
                return false;
            }

            @Override
            public long getItemId(int position) {
                return getItem(position).hashCode();
            }
        }

        private static class LibraryAdapter extends BaseAdapter {

            protected final Context mContext;
            private ImageWorker mImageWorker;

            public LibraryAdapter(Context context, ImageWorker imageWorker) {
                super();
                mContext = context;
                this.mImageWorker = imageWorker;
            }

            @Override
            public int getCount() {
                return mImageWorker.getAdapter().getSize();
            }

            @Override
            public Object getItem(int position) {
                return mImageWorker.getAdapter().getItem(position);
            }

            @Override
            public long getItemId(int position) {
                return ((ImageData) getItem(position)).file.getAbsolutePath().hashCode();
            }

            @Override
            public View getView(int arg0, View arg1, ViewGroup arg2) {
                return null;
            }
        }

        private class UploadImageJobCallback implements JobCallback {
            @Override
            public void onJobStateChange(Job job) {
                if (job.getFinished() || job.getException() != null) {
                    for (int i = 0, size = mUploadImageJobs.size(); i < size; i++) {
                        Job j = mUploadImageJobs.get(i);
                        if (j.getJobID().getTimeStamp() == job.getJobID().getTimeStamp()) {
                            mUploadImageJobs.remove(i);
                            mJobControlInterface.deregisterJobCallback(job.getJobID(),
                                    UploadImageJobCallback.this);
                            if (job.getFinished()) {
                                GuiUtils.alert(R.string.upload_job_done_successfully);
                            } else {
                                GuiUtils.alert(R.string.upload_job_failed);
                            }
                            GuiUtils.post(new Runnable() {

                                @Override
                                public void run() {
                                    updateUploadStatus();
                                }
                            });
                            break;
                        }
                    }
                }
            }

        }

        void onNewImageTaskAdded(String filePath) {

        }

        private class AddNewImageTask extends AbstractAddNewImageTask {

            public AddNewImageTask(String filePath, boolean moveOriginal) {
                super(filePath, mProductSku, moveOriginal,
                        LibraryUiFragment.this.mJobControlInterface, new SettingsSnapshot(
                                getActivity()), LibraryUiFragment.this);
            }

            @Override
            protected void doExtraWithJobInBackground() {
                mUploadImageJobs.add(mUploadImageJob);
                if (isResumed()) {
                    mJobControlInterface.registerJobCallback(mUploadImageJob.getJobID(),
                            mUploadImageJobCallback);
                }
            }

            @Override
            protected void onSuccessPostExecute() {
                GuiUtils.alert(R.string.upload_job_added_to_queue);
                updateUploadStatus();
                onNewImageTaskAdded(getFilePath());
            }
        }

        public static abstract class AbstractAddNewImageTask extends SimpleAsyncTask {

            protected Job mUploadImageJob;
            private SettingsSnapshot mSettingsSnapshot;
            private String mProductSku;
            private String mFilePath;
            boolean mMoveOriginal;
            protected JobControlInterface mJobControlInterface;

            public AbstractAddNewImageTask(String filePath, String productSku,
                    boolean moveOriginal, JobControlInterface jobControlInterface,
                    SettingsSnapshot settings, LoadingControl loadingControl) {
                super(loadingControl);
                this.mFilePath = filePath;
                this.mProductSku = productSku;
                this.mMoveOriginal = moveOriginal;
                this.mSettingsSnapshot = settings;
                this.mJobControlInterface = jobControlInterface;
            }

            @Override
            protected Boolean doInBackground(Void... args) {
                try {
                    JobID jobID = new JobID(INVALID_PRODUCT_ID, RES_UPLOAD_IMAGE, "" + mProductSku,
                            null);
                    Job uploadImageJob = new Job(jobID, mSettingsSnapshot);

                    File source = new File(mFilePath);
                    File imagesDir = JobCacheManager.getImageUploadDirectory(mProductSku,
                            mSettingsSnapshot.getUrl());
                    File target = new File(imagesDir, source.getName());
                    if (mMoveOriginal) {
                        source.renameTo(target);
                    } else {
                        FileUtils.copy(source, target);
                    }

                    uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_NAME, target.getName()
                            .substring(0, target.getName().toLowerCase().lastIndexOf(".jpg")));

                    uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_CONTENT,
                            target.getAbsolutePath());
                    String mimeType = FileUtils.getMimeType(target);
                    if (mimeType == null && mFilePath.toLowerCase().contains("jpg")) {
                        mimeType = "image/jpeg";
                    }
                    uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_MIME, mimeType);

                    mUploadImageJob = uploadImageJob;
                    mJobControlInterface.addJob(uploadImageJob);

                    doExtraWithJobInBackground();
                    Intent intent = EventBusUtils.getGeneralEventIntent(EventType.JOB_ADDED);
                    intent.putExtra(EventBusUtils.JOB, new ParcelableJobDetails(mUploadImageJob));
                    EventBusUtils.sendGeneralEventBroadcast(intent);
                    return true;
                } catch (Exception ex) {
                    GuiUtils.noAlertError(TAG, ex);
                }
                return false;

            }

            protected void doExtraWithJobInBackground() {
            }

            public String getFilePath() {
                return mFilePath;
            }
        }
    }

}
