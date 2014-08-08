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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;

import uk.co.senab.photoview.PhotoView;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.LibraryActivity.LibraryUiFragment.AbstractAddNewImageTask;
import com.mageventory.activity.LibraryActivity.LibraryUiFragment.AbstractUploadImageJobCallback;
import com.mageventory.activity.MainActivity.ImageData;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.bitmapfun.util.ImageFetcher;
import com.mageventory.fragment.base.BaseFragmentWithImageWorker;
import com.mageventory.job.JobControlInterface;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.util.EventBusUtils.GeneralBroadcastEventHandler;
import com.mageventory.util.FileUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.SimpleViewLoadingControl;

public class WebActivity extends BaseFragmentActivity implements MageventoryConstants {
    private static final String TAG = WebActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new WebUiFragment()).commit();
        }
    }

    WebUiFragment getContentFragment() {
        return (WebUiFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getContentFragment().reinitFromIntent(intent);
    }

    @Override
    public void onBackPressed() {
        WebUiFragment fragment = getContentFragment();
        boolean proceed = true;
        proceed &= !fragment.isBackKeyOverrode();
        if (proceed) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            getContentFragment().reinitFromIntent(getIntent());
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * An extended WebView class with the custom ActionMode.Callback to handle
     * Drawers lock mode and hide some unnecessary menu items in the text
     * selection action mode
     * 
     * @author Eugene Popovich
     */
    public static class CustomWebView extends WebView {
        /**
         * @see {@link WebView#WebView(Context)}
         */
        public CustomWebView(Context context) {
            super(context);
        }

        /**
         * @see {@link WebView#WebView(Context, AttributeSet)}
         */
        public CustomWebView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        /**
         * @see {@link WebView#WebView(Context, AttributeSet, int)}
         */
        public CustomWebView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public ActionMode startActionMode(ActionMode.Callback callback) {
            return super.startActionMode(new CustomActionModeCallback(callback));
        }

        /**
         * A custom ActionMode.Callback wrapper. Used to lock drawers when
         * action mode is activated and to hide some text selection action mode
         * menu items
         * 
         * @author Eugene Popovich
         */
        public class CustomActionModeCallback implements ActionMode.Callback {
            // wrapping callback
            ActionMode.Callback mCallback;
            /**
             * Reference to provider Object. It depends on Android version
             */
            Object mProvider;
            /**
             * Reference to provider class object. mProvider.getClass() approach
             * doesn't work at Android 4.0.3 so instead of provider class
             * WebView.class should be used
             */
            Class<?> mProviderClass;

            /**
             * @param callback a wrapping callback. All the action mode
             *            callbacks will be passed to it
             */
            /**
             * @param callback
             */
            public CustomActionModeCallback(ActionMode.Callback callback) {
                mCallback = callback;
                try {
                    // http://grepcode.com/file_/repository.grepcode.com/java/ext/com.google.android/android/4.1.1_r1/android/webkit/WebView.java/?v=source
                    // http://grepcode.com/file_/repository.grepcode.com/java/ext/com.google.android/android/4.1.1_r1/android/webkit/WebViewClassic.java/?v=source
                    // http://grepcode.com/file_/repository.grepcode.com/java/ext/com.google.android/android/4.0.3_r1/android/webkit/WebView.java/?v=source
                    boolean isJellyBeanOrHigher = CommonUtils.isJellyBeanOrHigher();
                    if (isJellyBeanOrHigher) {
                        Method m = WebView.class.getMethod("getWebViewProvider");
                        mProvider = m.invoke(CustomWebView.this);
                        mProviderClass = mProvider.getClass();
                        // call this method to avoid overwriting
                        // mSelectHandleLeft, mSelectHandleRight field values
                        // after we change them further
                        m = mProviderClass.getDeclaredMethod("ensureSelectionHandles");
                        m.setAccessible(true);
                        m.invoke(mProvider);
                    } else {
                        mProvider = CustomWebView.this;
                        mProviderClass = WebView.class;
                    }
                    // such as webview ignores theme specified value, we need to
                    // overcome that restriction and update private field values
                    // via reflection
                    Field leftSelectionHandler = mProviderClass
                            .getDeclaredField(
                            "mSelectHandleLeft");
                    leftSelectionHandler.setAccessible(true);
                    leftSelectionHandler
                            .set(mProvider,
                                    getResources().getDrawable(R.drawable.text_select_handle_left)
                                            .mutate());
                    Field rightSelectionHandler = mProviderClass
                            .getDeclaredField(
                            "mSelectHandleRight");
                    rightSelectionHandler.setAccessible(true);
                    rightSelectionHandler.set(mProvider,
                            getResources().getDrawable(R.drawable.text_select_handle_right)
                                    .mutate());
                } catch (Exception ex) {
                    CommonUtils.error(TAG, ex);
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                boolean result = mCallback.onCreateActionMode(mode, menu);
                // clear default menu, we don't need it
                menu.clear();
                mode.getMenuInflater().inflate(R.menu.web_select_text, menu);
                return result;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // lock the drawers when action mode is prepared
                setDrawersLocked(true);
                boolean result = mCallback.onPrepareActionMode(mode, menu);

                return result;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.copy:
                        try {
                            // for a now we can access selected text only via
                            // reflection
                            Method m = mProviderClass.getDeclaredMethod("getSelection");
                            m.setAccessible(true);
                            String selection = (String) m.invoke(mProvider);
                            GuiUtils.alert(CommonUtils
                                    .format("You've selected \"%1$s\"", selection));
                        } catch (Exception e) {
                            GuiUtils.error(TAG, R.string.errorGeneral, e);
                        }
                        mode.finish();
                        return true;
                    default:
                        return mCallback.onActionItemClicked(mode, item);
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // unlock the drawers when action mode is destroyed
                setDrawersLocked(false);
                mCallback.onDestroyActionMode(mode);
            }

            /**
             * Lock/unlock drawers
             * 
             * @param locked
             */
            public void setDrawersLocked(boolean locked) {
                Context c = getContext();
                if (c instanceof WebActivity) {
                    ((WebActivity) c).setDrawersLocked(locked);
                }
            }
        }
    }

    public static class WebUiFragment extends BaseFragmentWithImageWorker implements
            GeneralBroadcastEventHandler {

        public static final int SCAN_QR_CODE = 0;

        static final int ANIMATION_DURATION = 500;

        /**
         * An enum describing possible WebUiFragment states
         */
        enum State {
            WEB, IMAGE
        }

        WebView mWebView;
        Button mCancelButton;
        /**
         * The upload status view reference
         */
        View mUploadStatusLine;
        /**
         * The upload status view text field. Used to display how many items are
         * uploading now
         */
        TextView mUploadStatusText;
        String mProductSku;
        String mProductName;
        String mLastLoadedPage;
        String mLastLoadedUrl;
        ProgressBar mPageLoadingProgress;
        /**
         * The current fragment state
         */
        State mCurrentState;
        /**
         * Tip with the information about how to open images widget
         */
        View mTipText;
        /**
         * Container which contains download image related information widgets
         */
        View mImageInfoContainer;
        /**
         * Text view which contains image related information (dimensions, size)
         */
        TextView mImageInfo;
        /**
         * Reference to the image info too small information view
         */
        View mImageInfoTooSmall;
        /**
        * Container which contains ImageView and loading progress bar
        */
        View mImageContainer;
        /**
         * Image view which displays downloaded image
         */
        PhotoView mImage;
        /**
         * Button which adds new image to the product task for the currently
         * displaying downloaded image
         */
        Button mGrabImageBtn;
        /**
         * The loading control for the image loading operation and for the
         * adding new image task
         */
        LoadingControl mImageLoadingControl;
        /**
         * The minimum recommended image size. The smallest image dimension
         * should be more or equals to this parameter
         */
        int mMinImageSize;
        /**
         * Reference to the last downloaded image data.
         */
        ImageData mLastDownloadedImageData;
        /**
         * Reference to the last handled image url
         */
        String mLastImageUrl;
        /**
         * Instance of {@link JobControlInterface}
         */
        JobControlInterface mJobControlInterface;
        /**
         * The callback for the upload image jobs with some extra functionality
         */
        UploadImageJobCallback mUploadImageJobCallback;
        /**
         * Instance of {@link Settings}
         */
        Settings mSettings;
        /**
         * Flag indicating state switching process active. It takes around 1
         * second because of animation
         */
        boolean mStateSwitchingActive = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mSettings = new Settings(getActivity().getApplicationContext());
            mJobControlInterface = new JobControlInterface(getActivity());
            mUploadImageJobCallback = new UploadImageJobCallback(mJobControlInterface);
            // register the general event broadcast listener. Will be
            // automatically unregistered in the onDestroy method
            EventBusUtils.registerOnGeneralEventBroadcastReceiver(TAG, this, this);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            View view = inflater.inflate(R.layout.web, container, false);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mPageLoadingProgress = (ProgressBar) view.findViewById(R.id.pageLoadingProgress);
            mWebView = (WebView) view.findViewById(R.id.webView);
            initWebView();
            mCancelButton = (Button) view.findViewById(R.id.cancelButton);
            mCancelButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    getActivity().finish();
                }
            });

            mTipText = view.findViewById(R.id.tipText);
            mImageInfoContainer = view.findViewById(R.id.imageInfoContainer);
            mImageInfo = (TextView) view.findViewById(R.id.imageInfo);
            mImageInfoTooSmall = view.findViewById(R.id.imageInfoTooSmall);
            mImageContainer = view.findViewById(R.id.imageContainer);
            mImage = (PhotoView) view.findViewById(R.id.image);
            mImage.setMaxScale(7.0f);
            mGrabImageBtn = (Button) view.findViewById(R.id.grabImageButton);
            mGrabImageBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // if image already downloaded then start add new image task
                    if (mLastDownloadedImageData != null) {
                        new AddNewImageTask(mLastDownloadedImageData.getFile().getAbsolutePath())
                                .execute();
                        setState(State.WEB);
                    }
                }
            });
            mImageLoadingControl = new SimpleViewLoadingControl(
                    view.findViewById(R.id.imageLoading));

            mUploadStatusLine = view.findViewById(R.id.uploadStatusLine);
            mUploadStatusText = (TextView) view.findViewById(R.id.uploadStatusText);

            reinitFromIntent(getActivity().getIntent());
        }

        class MyJavaScriptInterface {
            @JavascriptInterface
            public void processHTML(String html) {
                mLastLoadedPage = html;
                rememberLastLoadedUrl();
            }
        }

        @Override
        protected void initImageWorker() {
            mMinImageSize = getResources().getDimensionPixelSize(R.dimen.web_min_item_size);
            mImageWorker = new CustomImageFetcher(getActivity(), mImageLoadingControl,
                    Integer.MAX_VALUE);
        }

        private void initWebView() {
            WebSettings webSettings = mWebView.getSettings();
            // webSettings.setBuiltInZoomControls(true);
            webSettings.setJavaScriptEnabled(true);

            final boolean hasJavascriptInterfaceBug = !CommonUtils.isHoneyCombOrHigher();
            if (!hasJavascriptInterfaceBug) {
                mWebView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
            }
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String address) {
                    // have the page spill its guts, with a secret prefix
                    if (hasJavascriptInterfaceBug) {
                        view.loadUrl("javascript:console.log('MAGIC'+document.getElementsByTagName('html')[0].innerHTML);");
                    } else {
                        view.loadUrl("javascript:window.HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                    }
                }
            });
            mWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int progress) {
                    super.onProgressChanged(view, progress);
                    if (progress < 100 && mPageLoadingProgress.getVisibility() == ProgressBar.GONE) {
                        mPageLoadingProgress.setVisibility(ProgressBar.VISIBLE);
                    }
                    mPageLoadingProgress.setProgress(progress);
                    if (progress == 100) {
                        mPageLoadingProgress.setVisibility(ProgressBar.GONE);
                    }
                }

                @Override
                public boolean onConsoleMessage(ConsoleMessage cmsg) {
                    String message = cmsg.message();
                    // check secret prefix
                    if (message != null && message.startsWith("MAGIC")) {
                        String msg = cmsg.message().substring(5); // strip off
                        /* process HTML */
                        mLastLoadedPage = msg;
                        rememberLastLoadedUrl();
                        return true;
                    }

                    return false;
                }

            });

            mWebView.setOnLongClickListener(new OnLongClickListener() {
                private static final String URL_KEY = "url";
                
                @Override
                public boolean onLongClick(View v) {
                    Handler handler = new Handler(Looper.getMainLooper()) {

                        @Override
                        public void handleMessage(Message msg) {
                            if (msg.getData().containsKey(URL_KEY)) {
                                String val = msg.getData().getString(URL_KEY);
                                // if val is not empty and is url starting with
                                // http or https then load image and update
                                // fragment state
                                if (val != null
                                        && val.matches("(?i)^" + ImageUtils.PROTO_PREFIX + ".*")) {
                                    mLastImageUrl = val;
                                    mImageWorker.loadImage(val, mImage, mImageLoadingControl);
                                    setState(State.IMAGE);
                                }
                            }
                        }
                    };
                    Message msg = new Message();
                    msg.setTarget(handler);
                    ((WebView) v).requestImageRef(msg);
                    // return false so the standard long click handlers such as
                    // text selection will work as expected
                    return false;
                }
            });
            
            mWebView.setLongClickable(true);            
        }

        private void rememberLastLoadedUrl() {
            GuiUtils.post(new Runnable() {

                @Override
                public void run() {
                    mLastLoadedUrl = mWebView.getUrl();
                }
            });
        }

        public void reinitFromIntent(Intent intent) {
            mWebView.clearHistory();
            mLastLoadedPage = null;
            mProductSku = intent.getExtras().getString(getString(R.string.ekey_product_sku));
            mProductName = intent.getExtras().getString(getString(R.string.ekey_product_name));
            refreshWebView();
            mUploadImageJobCallback.reinit(mProductSku, mSettings);
        }

        public void refreshWebView() {
            googleIt(mProductName);
        }

        private void googleIt(String query) {
            try {
                setState(State.WEB);
                mWebView.loadUrl("https://www.google.com/search?q="
                        + URLEncoder.encode(query, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                GuiUtils.noAlertError(TAG, e);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            mUploadImageJobCallback.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
            mUploadImageJobCallback.onPause();
        }

        /**
         * @return true if we have custom behavior of back button pressed
         */
        public boolean isBackKeyOverrode() {
            // if back key is pressed and fragment is in image viewing state
            // then switch state to web
            if (mCurrentState == State.IMAGE) {
                setState(State.WEB);
                return true;
            } else if (mWebView.canGoBack()) {
                mWebView.goBack();
                return true;
            } else {
                return false;
            }
        }

        /**
         * Set the current fragment state: either IMAGE or WEB. Setting state
         * will adjust visibility of different views with various animation
         * 
         * @param state
         */
        void setState(final State state) {
            if (mCurrentState == state) {
                // no need to perform any changes, the required state already
                // selected
                return;
            }
            mStateSwitchingActive = true;
            Animation fadeOutAnimation = AnimationUtils.loadAnimation(getActivity(),
                    android.R.anim.fade_out);
            fadeOutAnimation.setDuration(ANIMATION_DURATION);
            Animation slideOutLeftAnimation = AnimationUtils.loadAnimation(getActivity(),
                    R.anim.slide_out_left);
            slideOutLeftAnimation.setDuration(ANIMATION_DURATION);
            Animation slideOutRightAnimation = AnimationUtils.loadAnimation(getActivity(),
                    android.R.anim.slide_out_right);
            slideOutRightAnimation.setDuration(ANIMATION_DURATION);

            final Runnable showNewStateWidgetsRunnable = new Runnable() {

                @Override
                public void run() {
                    Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(),
                            android.R.anim.fade_in);
                    fadeInAnimation.setDuration(ANIMATION_DURATION);
                    Animation slideInRightAnimation = AnimationUtils.loadAnimation(getActivity(),
                            R.anim.slide_in_right);
                    slideInRightAnimation.setDuration(ANIMATION_DURATION);
                    Animation slideInLeftAnimation = AnimationUtils.loadAnimation(getActivity(),
                            android.R.anim.slide_in_left);
                    slideInLeftAnimation.setDuration(ANIMATION_DURATION);
                    fadeInAnimation.setAnimationListener(new AnimationListener() {

                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mStateSwitchingActive = false;
                        }
                    });
                    switch (state) {
                        case IMAGE:
                            mImageContainer.startAnimation(fadeInAnimation);
                            mGrabImageBtn.startAnimation(slideInRightAnimation);
                            mImageInfoContainer.startAnimation(slideInLeftAnimation);

                            mImageContainer.setVisibility(View.VISIBLE);
                            mGrabImageBtn.setVisibility(View.VISIBLE);
                            mImageInfoContainer.setVisibility(View.VISIBLE);

                            updateImageInfo(false);
                            break;
                        case WEB:
                            mWebView.startAnimation(fadeInAnimation);
                            mTipText.startAnimation(slideInLeftAnimation);

                            mWebView.setVisibility(View.VISIBLE);
                            mTipText.setVisibility(View.VISIBLE);
                            break;
                        default:
                            break;
                    }
                }
            };
            // if state was specified before we need to hide previous state
            // widgets
            if (mCurrentState != null) {

                switch (mCurrentState) {
                    case IMAGE:
                        mLastImageUrl = null;
                        mLastDownloadedImageData = null;
                        slideOutLeftAnimation.setAnimationListener(new AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                // update visibility of mImageInfoContainer when
                                // animation ends to avoid Exit Search button
                                // flickering.
                                mImageInfoContainer.setVisibility(View.GONE);
                                mImageInfoTooSmall.setVisibility(View.GONE);
                                mImageInfo.setText(null);
                                // run scheduled operation to show new state
                                // widgets when the hiding widget animation ends
                                showNewStateWidgetsRunnable.run();
                            }
                        });
                        mImageContainer.startAnimation(fadeOutAnimation);
                        mGrabImageBtn.startAnimation(slideOutRightAnimation);
                        mImageInfoContainer.startAnimation(slideOutLeftAnimation);

                        mImageContainer.setVisibility(View.GONE);
                        mGrabImageBtn.setVisibility(View.GONE);
                        break;
                    case WEB:
                        slideOutLeftAnimation.setAnimationListener(new AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                // update visibility of mTipText when
                                // animation ends to avoid Exit Search button
                                // flickering.
                                mTipText.setVisibility(View.GONE);
                                // run scheduled operation to show new state
                                // widgets when the hiding widget animation ends
                                showNewStateWidgetsRunnable.run();
                            }
                        });
                        mWebView.startAnimation(fadeOutAnimation);
                        mTipText.startAnimation(slideOutLeftAnimation);

                        mWebView.setVisibility(View.GONE);
                        break;
                    default:
                        break;
                }
            }

            // run widget for the new state showing operation explicitly if
            // previos state is null
            if (mCurrentState == null) {
                showNewStateWidgetsRunnable.run();
            }
            mCurrentState = state;
        }

        /**
         * Update the image information and its visibility when the image is
         * downloaded or show image state widgets operation
         * 
         * @param animate
         */
        void updateImageInfo(boolean animate) {
            // if data is not yet downloaded hide the information widget
            if (mLastDownloadedImageData == null) {
                mImageInfoTooSmall.setVisibility(View.GONE);
                mImageInfo.setText(null);
            } else {
                // if at least one dimension of downloaded picture is less than
                // mMinImageSize then show warning widget
                mImageInfoTooSmall
                        .setVisibility(mLastDownloadedImageData.getWidth() < mMinImageSize
                                || mLastDownloadedImageData.getHeight() < mMinImageSize ? View.VISIBLE
                                : View.GONE);
                mImageInfo.setText(CommonUtils.getStringResource(
                        R.string.image_info_size_format_web, mLastDownloadedImageData.getWidth(),
                        mLastDownloadedImageData.getHeight(),
                        FileUtils.formatFileSize(mLastDownloadedImageData.getFile().length())));

                if (animate && !mStateSwitchingActive) {
                    Animation slideInLeftAnimation = AnimationUtils.loadAnimation(getActivity(),
                            android.R.anim.slide_in_left);
                    slideInLeftAnimation.setDuration(ANIMATION_DURATION);
                    mImageInfoContainer.startAnimation(slideInLeftAnimation);
                }
            }
        }

        /**
         * Update information in the the upload status widgets
         * 
         * @param jobsCount
         */
        private void updateUploadStatus(int jobsCount) {
            if (isResumed()) {
                mUploadStatusLine.setVisibility(jobsCount == 0 ? View.GONE : View.VISIBLE);
                mUploadStatusText.setText(CommonUtils.getStringResource(
                        R.string.upload_queue_status, CommonUtils.formatNumber(jobsCount)));
            }
        }

        @Override
        public void onGeneralBroadcastEvent(EventType eventType, Intent extra) {
            switch (eventType) {
                case JOB_ADDED:
                    mUploadImageJobCallback.onGeneralBroadcastEventJobAdded(extra, mProductSku,
                            mSettings, this);
                    break;
                default:
                    break;
            }
        }

        /**
         * Implementation of AbstractUploadImageJobCallback
         */
        private class UploadImageJobCallback extends AbstractUploadImageJobCallback {

            public UploadImageJobCallback(JobControlInterface jobControlInterface) {
                super(jobControlInterface);
            }

            @Override
            void updateUploadStatus() {
                WebUiFragment.this.updateUploadStatus(getJobsCount());
            }
        }

        /**
         * The custom image fetcher which handles finishing of bitmap download
         * process and updates image information when it occurs
         */
        private class CustomImageFetcher extends ImageFetcher {

            public CustomImageFetcher(Context context, LoadingControl loadingControl, int size) {
                super(context, loadingControl, size);
            }

            @Override
            protected Bitmap processBitmap(Object data, ProcessingState state) {
                Bitmap result = super.processBitmap(data, state);
                // such as we can have few downloading tasks at the time we need
                // to check whether we are handling last selected image before
                // updating information widgets and save data to the
                // mLastDownloadedImageData variable
                if (String.valueOf(data).equals(mLastImageUrl)) {
                    mLastDownloadedImageData = null;
                    File f = sLastFile.get();
                    try {
                        if (f != null) {
                            mLastDownloadedImageData = ImageData.getImageDataForFile(f, false);
                            GuiUtils.post(new Runnable() {

                                @Override
                                public void run() {
                                    updateImageInfo(true);
                                }
                            });
                        }
                    } catch (Exception ex) {
                        GuiUtils.error(TAG, ex);
                    }
                }

                return result;
            }

        }

        /**
         * Implementation of {@link AbstractAddNewImageTask}. Add the upload
         * image job for the product
         */
        private class AddNewImageTask extends AbstractAddNewImageTask {

            public AddNewImageTask(String filePath) {
                super(filePath, mProductSku, false, WebUiFragment.this.mJobControlInterface,
                        new SettingsSnapshot(getActivity()), mImageLoadingControl);
            }

            @Override
            protected void onSuccessPostExecute() {
                // GuiUtils.alert(R.string.upload_job_added_to_queue); //
                // Another alert will be issued once image upload is finished
            }
        }
    }
}
