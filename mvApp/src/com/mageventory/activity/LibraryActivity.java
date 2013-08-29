
package com.mageventory.activity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.MainActivity.ImageData;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.bitmapfun.util.ImageCache;
import com.mageventory.bitmapfun.util.ImageFileSystemFetcher;
import com.mageventory.bitmapfun.util.ImageWorker;
import com.mageventory.bitmapfun.util.ImageWorker.ImageWorkerAdapter;
import com.mageventory.fragment.base.BaseFragmentWithImageWorker;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCallback;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.FileUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageFlowUtils;
import com.mageventory.util.ImageFlowUtils.FlowObjectToStringWrapper;
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
public class LibraryActivity extends BaseFragmentActivity implements MageventoryConstants {
    private static final String TAG = LibraryActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new LibraryUiFragment()).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            LibraryUiFragment.sCachedImageDataList = null;
            LibraryUiFragment libraryUiFragment = getContentFragment();
            libraryUiFragment.refresh();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    LibraryUiFragment getContentFragment() {
        return (LibraryUiFragment) getSupportFragmentManager().findFragmentById(
                android.R.id.content);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getContentFragment().reinitFromIntent(intent);
    }

    public static class LibraryUiFragment extends BaseFragmentWithImageWorker implements
            LoadingControl {
        static List<ImageData> sCachedImageDataList = null;

        private Settings mSettings;
        private JobControlInterface mJobControlInterface;
        private int mImageThumbSize;
        private int mImageThumbSpacing;
        private int mImageThumbBorder;

        private AtomicInteger mLoaders = new AtomicInteger(0);
        private View mLoadingView;
        private View mStatusLine;
        private TextView mStatusText;
        private LoadLibraryTask mLoadLibraryTask;
        private ListView mLibraryList;
        private LibraryAdapterExt mLibraryAdapter;

        ViewTreeObserver.OnGlobalLayoutListener mLibraryGlobalOnLayoutListener;

        private ImageData mCurrentData;
        private String mProductSku;

        private List<Job> mUploadImageJobs = new ArrayList<Job>();
        private UploadImageJobCallback mUploadImageJobCallback = new UploadImageJobCallback();

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
            mStatusLine = view.findViewById(R.id.statusLine);
            mStatusText = (TextView) view.findViewById(R.id.statusText);
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
            mImageWorker = new ImageFileSystemFetcher(getActivity(), null, mImageThumbSize);
            mImageWorker.setLoadingImage(R.drawable.empty_photo);
            mImageWorker.setImageCache(ImageCache.findOrCreateCache(getActivity(),
                    ImageCache.LOCAL_THUMBS_EXT_CACHE_DIR, 1500, true, false));
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            GuiUtils.removeGlobalOnLayoutListener(mLibraryList, mLibraryGlobalOnLayoutListener);
            if (mLoadLibraryTask != null) {
                mLoadLibraryTask.cancel(true);
                mLoadLibraryTask = null;
            }
        }

        public void refresh() {
            refresh(getView());
        }

        void refresh(View v) {
            if (!isActivityAlive()) {
                return;
            }
            if (mLoadLibraryTask != null) {
                mLoadLibraryTask.cancel(true);
            }
            mLoadLibraryTask = new LoadLibraryTask();
            mLoadLibraryTask.execute();
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
                case R.id.menu_view:
                    Intent intent = new Intent(getActivity(), PhotoViewActivity.class);
                    intent.putExtra(PhotoViewActivity.EXTRA_PATH, mCurrentData.getFile()
                            .getAbsolutePath());
                    startActivity(intent);
                    break;
                case R.id.menu_upload:
                    new AddNewImageTask(mCurrentData.getFile().getAbsolutePath(), false).execute();
                    break;
                case R.id.menu_delete:
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
                    break;
                case R.id.menu_upload_and_delete:
                    removeCurrentItem();
                    new AddNewImageTask(mCurrentData.getFile().getAbsolutePath(), true).execute();
                    break;
            }
            return true;
        }

        private void removeCurrentItem() {
            LibraryImageWorkerAdapter adapter = (LibraryImageWorkerAdapter) mImageWorker
                    .getAdapter();
            int ix = adapter.data.indexOf(mCurrentData);
            if (ix != -1) {
                adapter.data.remove(ix);
            }
            mLibraryAdapter.imageFlowUtils.onGroupsStructureModified();
            mLibraryAdapter.notifyDataSetChanged();
        }
        @Override
        public void onResume() {
            super.onResume();
            mJobControlInterface.registerJobCallbacksAndRemoveAbsentJobs(mUploadImageJobs,
                    mUploadImageJobCallback);
            updateUploadStatus();
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
                mStatusLine.setVisibility(mUploadImageJobs.isEmpty() ? View.GONE : View.VISIBLE);
                mStatusText.setText(CommonUtils.getStringResource(R.string.upload_queue_status,
                        CommonUtils.formatNumber(mUploadImageJobs.size())));
            }
        }

        class LoadLibraryTask extends SimpleAsyncTask {
            LibraryImageWorkerAdapter adapter;
        
            public LoadLibraryTask() {
                super(LibraryUiFragment.this);
            }
        
            @Override
            protected void onFailedPostExecute() {
                super.onFailedPostExecute();
                mLoadLibraryTask = null;
            }
        
            @Override
            protected void onSuccessPostExecute() {
                try {
                    if (!isCancelled()) {
                        sCachedImageDataList = adapter.data;
                        mImageWorker.setAdapter(adapter);
                        if (mLibraryAdapter == null) {
                            mLibraryAdapter = new LibraryAdapterExt();
                            mLibraryList.setAdapter(mLibraryAdapter);
                        } else {
                            mLibraryAdapter.imageFlowUtils.onGroupsStructureModified();
                            mLibraryAdapter.notifyDataSetChanged();
                        }
                    }
                } finally {
                    mLoadLibraryTask = null;
                }
            }
        
            @Override
            protected Boolean doInBackground(Void... arg0) {
                try {
                    if (isCancelled()) {
                        return false;
                    }
                    adapter = new LibraryImageWorkerAdapter();
                    return !isCancelled();
                } catch (Exception e) {
                    GuiUtils.error(TAG, e);
                }
                return false;
            }
        
        }

        public class LibraryImageWorkerAdapter extends ImageWorkerAdapter {
            public List<ImageData> data;
        
            LibraryImageWorkerAdapter() throws IOException {
                data = sCachedImageDataList;
                if (data != null) {
                    return;
                }
                String imagesDirPath = mSettings.getGalleryPhotosDirectory();
                data = new ArrayList<ImageData>();
                if (TextUtils.isEmpty(imagesDirPath)) {
                    return;
                }
        
                File f = new File(imagesDirPath);
        
                List<File> files = new ArrayList<File>();
        
                processDirectory(f, files);
                if (files == null || files.isEmpty()) {
                    return;
                }
        
                Collections.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File lhs, File rhs) {
                        return (int) (rhs.length() - lhs.length());
                    }
                });
                for (File file : files) {
                    data.add(ImageData.getImageDataForFile(file, false));
                }
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
        
            @Override
            public Object getItem(int num) {
                return data.get(num);
            }
        
            @Override
            public int getSize() {
                return data.size();
            }
        }

        /**
         * Extended adapter which uses photo groups as items instead of
         * ImageDatas
         */
        public class LibraryAdapterExt extends LibraryAdapter {
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
                        ImageView imageView = (ImageView) view.findViewById(R.id.image);
                        imageView.setOnLongClickListener(new OnLongClickListener() {
        
                            @Override
                            public boolean onLongClick(View v) {
                                TrackerUtils.trackLongClickEvent("image", LibraryUiFragment.this);
                                mCurrentData = value;
                                registerForContextMenu(v);
                                v.showContextMenu();
                                unregisterForContextMenu(v);
                                return true;
                            }
                        });
                    }
        
                    @Override
                    public void loadImage(final ImageData photo, final ImageView imageView) {
                        FlowObjectToStringWrapper<ImageData> fo = new FlowObjectToStringWrapper<ImageData>(
                                photo, photo.getFile().getAbsolutePath());
                        mImageWorker.loadImage(fo, imageView);
                    }
                };
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
            LayoutInflater inflater;
        
            public LibraryAdapter(Context context, ImageWorker imageWorker) {
                super();
                mContext = context;
                this.mImageWorker = imageWorker;
                this.inflater = LayoutInflater.from(context);
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

        private class AddNewImageTask extends SimpleAsyncTask {

            private Job mUploadImageJob;
            private SettingsSnapshot mSettingsSnapshot;
            private String mFilePath;
            boolean mMoveOriginal;

            public AddNewImageTask(String filePath, boolean moveOriginal) {
                super(LibraryUiFragment.this);
                this.mFilePath = filePath;
                this.mMoveOriginal = moveOriginal;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                mSettingsSnapshot = new SettingsSnapshot(getActivity());
            }

            @Override
            protected Boolean doInBackground(Void... args) {
                try {
                    JobID jobID = new JobID(INVALID_PRODUCT_ID, RES_UPLOAD_IMAGE, "" + mProductSku,
                            null);
                    Job uploadImageJob = new Job(jobID, mSettingsSnapshot);

                    File source = new File(mFilePath);
                    File imagesDir = JobCacheManager.getImageUploadDirectory(mProductSku,
                            mSettings.getUrl());
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

                    mJobControlInterface.addJob(uploadImageJob);

                    mUploadImageJob = uploadImageJob;
                    mUploadImageJobs.add(mUploadImageJob);
                    if (isResumed()) {
                        mJobControlInterface.registerJobCallback(mUploadImageJob.getJobID(),
                                mUploadImageJobCallback);
                    }
                    return true;
                } catch (Exception ex) {
                    GuiUtils.error(TAG, ex);
                }
                return false;

            }

            @Override
            protected void onSuccessPostExecute() {
                GuiUtils.alert(R.string.upload_job_added_to_queue);
                updateUploadStatus();
            }
        }
    }

}
