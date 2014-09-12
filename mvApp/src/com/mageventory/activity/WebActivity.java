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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.co.senab.photoview.PhotoView;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.SubMenu;
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
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.LibraryActivity.LibraryUiFragment.AbstractAddNewImageTask;
import com.mageventory.activity.LibraryActivity.LibraryUiFragment.AbstractUploadImageJobCallback;
import com.mageventory.activity.MainActivity.ImageData;
import com.mageventory.activity.WebActivity.WebUiFragment.State;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.bitmapfun.util.ImageFetcher;
import com.mageventory.fragment.base.BaseFragmentWithImageWorker;
import com.mageventory.job.JobControlInterface;
import com.mageventory.model.CustomAttributeSimple;
import com.mageventory.recent_web_address.RecentWebAddress;
import com.mageventory.recent_web_address.RecentWebAddressProviderAccessor;
import com.mageventory.recent_web_address.RecentWebAddressProviderAccessor.AbstractLoadRecentWebAddressesTask;
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
import com.mageventory.util.ScanUtils;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.SimpleViewLoadingControl;
import com.mageventory.util.loading.GenericMultilineViewLoadingControl;
import com.mageventory.util.loading.GenericMultilineViewLoadingControl.ProgressData;

public class WebActivity extends BaseFragmentActivity implements MageventoryConstants {
    private static final String TAG = WebActivity.class.getSimpleName();
    /**
     * The key for the custom text attributes intent extra
     */
    public static final String EXTRA_CUSTOM_TEXT_ATTRIBUTES = "CUSTOM_TEXT_ATTRIBUTES";
    /**
     * The key for the search query intent extra
     */
    public static final String EXTRA_SEARCH_QUERY = "SEARCH_QUERY";
    /**
     * The key for the search domains intent extra
     */
    public static final String EXTRA_SEARCH_DOMAINS = "SEARCH_DOMAINS";
    /**
     * The key for the source intent extra
     */
    public static final String EXTRA_SOURCE = "SOURCE";

    /**
     * The source initiated WebActivity launch
     */
    public enum Source {
        /**
         * Product details activity
         */
        PROD_DETAILS,
        /**
         * Product create and edit activities
         */
        ABS_PRODUCT
    }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCAN_QR_CODE) {
            getContentFragment().onTextScanned(resultCode, data);
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
         * Whether the current android version higher or equals to Kit-Kat
         */
        final boolean mKitKatOrHigher = CommonUtils.isKitKatOrHigher();
        /**
         * Whether the current android version higher or equals to Jelly Bean
         */
        final boolean mJellyBeanOrHigher = CommonUtils.isJellyBeanOrHigher();
        
        /**
         * The last selected text in the web page
         */
        String mLastSelectedText;
        /**
         * The count down latch to await until mLastSelectedText variable will
         * be specified via the Java-JavaScript bridge
         */
        CountDownLatch mSelectionDoneSignal;

        /**
         * Flag indicating whether the action mode is active. Handled by
         * CustomActionModeCallback
         */
        boolean mInActionMode;
        
        /**
         * Reference to the action mode. Used for ability to close action mode
         * on demand programmatically
         */
        ActionMode mActionMode;

        /**
         * Flag indicating whether the pointer is down at WebView. Handled in
         * the dispatchTouchEvent method
         */
        boolean mTouching;

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
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (!mJellyBeanOrHigher && mProvider == null) {
                try {
                    // http://grepcode.com/file_/repository.grepcode.com/java/ext/com.google.android/android/4.0.3_r1/android/webkit/WebView.java/?v=source
                    mProvider = CustomWebView.this;
                    mProviderClass = WebView.class;
                } catch (Exception ex) {
                    CommonUtils.error(TAG, ex);
                }
            }
        }

        @Override
        public ActionMode startActionMode(ActionMode.Callback callback) {
            mActionMode = super.startActionMode(new CustomActionModeCallback(callback));
            return mActionMode;
        }

        /**
         * Get the last selected text value
         * 
         * @return
         */
        public String getLastSelectedText() {
            return mLastSelectedText;
        }

        /**
         * Set the last selected text value
         * 
         * @param lastSelectedText
         */
        public void setLastSelectedText(String lastSelectedText) {
            mLastSelectedText = lastSelectedText;
        }

        /**
         * Get the selection done count down latch signal
         * 
         * @return
         */
        public CountDownLatch getSelectionDoneSignal() {
            return mSelectionDoneSignal;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            // we need to use dispatch touch even because not all events are
            // passed to onInterceptTouchEvent and onTouchEvent methods
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mTouching = true;
                if (mJellyBeanOrHigher && !isInActionMode()) {
                    // if WebView is not in action mode and is JB or higher
                    // platform. Calling the loadUrl in action mode will close
                    // it in the Android version prior 4.4
                    loadUrl("javascript:globalOnMouseDown();");
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                mTouching = false;
                // we could not use same loadUrl approach as for the ACTION_DOWN
                // event because it closes action mode on Android JB version. So
                // we should use the flag hack and check its value in the
                // javascript code via Java-Javascript bridge
            }
            return super.dispatchTouchEvent(event);
        }

        /**
         * Is the WebView in action mode (text selection mode)
         * 
         * @return
         */
        boolean isInActionMode()
        {
            return mInActionMode;
        }

        /**
         * Is the user still touches the WebView
         * 
         * @return
         */
        boolean isTouching() {
            return mTouching;
        }

        /**
         * Close the action mode initiated by the WebView
         */
        public void exitActionMode() {
            mActionMode.finish();
        }

        /**
         * Get the WebView selected text
         * 
         * @return
         * @throws NoSuchMethodException
         * @throws IllegalAccessException
         * @throws InvocationTargetException
         * @throws TimeoutException
         */
        public String getSelectedText() throws NoSuchMethodException, IllegalAccessException,
                InvocationTargetException, TimeoutException {
            // in JB and higher we can retrieve selection via
            // JavaScript. We should use that way because
            // reflection approach returns wrong result for
            // expanded via the javascript selection
            if (mJellyBeanOrHigher) {
                mSelectionDoneSignal = new CountDownLatch(1);
                mLastSelectedText = null;
                CustomWebView.this
                        .loadUrl("javascript:window.HTMLOUT.sendSelectionUpdatedEvent(getSelectionText());");
                // javascript call is not synchronous so we
                // should wait for its done via the
                // CountDownLatch
                boolean success = false;
                try {
                    success = mSelectionDoneSignal.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                }
                // if mSelectionDoneSignal reached await time with no
                // success text was not copied so throw TimeoutException
                if (!success) {
                    throw new TimeoutException("Get selection timeout");
                }
            } else {
                // for a now we can access selected text only
                // via reflection in Android ICS

                Method m = mProviderClass.getDeclaredMethod(mKitKatOrHigher ? "getSelectedText"
                        : "getSelection");
                m.setAccessible(true);
                mLastSelectedText = (String) m.invoke(mProvider);
            }
            return mLastSelectedText;
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
             * @param callback a wrapping callback. All the action mode
             *            callbacks will be passed to it
             */
            /**
             * @param callback
             */
            public CustomActionModeCallback(ActionMode.Callback callback) {
                mCallback = callback;
            }

            @Override
            public boolean onCreateActionMode(final ActionMode mode, Menu menu) {
                boolean result = mCallback.onCreateActionMode(mode, menu);
                // clear default menu, we don't need it
                menu.clear();
                mode.getMenuInflater().inflate(R.menu.web_select_text, menu);

                final WebUiFragment fragment = ((WebActivity) getContext()).getContentFragment();
                ArrayList<CustomAttributeSimple> textAttributes = fragment.getTextAttributes();

                // hide copy menu if text attributes are not empty
                menu.findItem(R.id.copy).setVisible(textAttributes == null);
                // hide copy to menu if text attributes are empty
                MenuItem mi = menu.findItem(R.id.copyTo);
                mi.setVisible(textAttributes != null);

                SubMenu subMenu = mi.getSubMenu();
                fragment.initCopyToMenu(subMenu);
                return result;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // lock the drawers when action mode is prepared
                setDrawersLocked(true);
                // set the flag such as action mode is preparing to be shown
                mInActionMode = true;
                final WebUiFragment fragment = ((WebActivity) getContext()).getContentFragment();
                fragment.setState(State.SELECTION);
                boolean result = mCallback.onPrepareActionMode(mode, menu);

                return result;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.copy:
                        try {
                            // copy text to clipboard
                            ClipboardManager clipboard = (ClipboardManager) getContext()
                                    .getSystemService(CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText(TAG, getSelectedText());
                            clipboard.setPrimaryClip(clip);
                        } catch (Exception e) {
                            GuiUtils.error(TAG, R.string.copyTextError, e);
                        }
                        mode.finish();
                        return true;
                    case R.id.copyTo:
                        return true;
                    default:
                        return mCallback.onActionItemClicked(mode, item);
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // unlock the drawers when action mode is destroyed
                setDrawersLocked(false);
                // set the flag such as action mode is destroying
                mInActionMode = false;
                final WebUiFragment fragment = ((WebActivity) getContext()).getContentFragment();
                fragment.setState(State.WEB);
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

        static final int ANIMATION_DURATION = 500;

        /**
         * An enum describing possible WebUiFragment states
         */
        enum State {
            WEB, IMAGE, SELECTION
        }

        CustomWebView mWebView;
        Button mCancelButton;
        /**
         * The loading control for the parsing image urls task
         */
        LoadingControl mParsingImageUrlsLoadingControl;
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
        /**
         * The domains the search should be performed for. May be empty.
         */
        List<String> mSearchDomains = new ArrayList<String>();

        /**
         * Text attributes which support web text copy functionality.
         */
        ArrayList<CustomAttributeSimple> mTextAttributes;
        /**
         * Updated text attributes. Used to store attribute values updates
         * before the product edit activity launch
         */
        Set<CustomAttributeSimple> mUpdatedTextAttributes = new HashSet<CustomAttributeSimple>();

        /**
         * The query which should be used in google search
         */
        String mSearchQuery;
        String mLastLoadedPage;
        String mLastLoadedUrl;
        ProgressBar mPageLoadingProgress;
        ParseUrlsTask mParseUrlsTask;
        /**
         * The current fragment state
         */
        State mCurrentState;
        /**
         * Tip with the information about how to open images widget
         */
        TextView mTipText;
        /**
         * Container which contains download image related information widgets
         */
        View mImageInfoContainer;
        /**
         * Container which contains web view and the progress bar for the images
         * loading
         */
        View mWebViewContainer;
        /**
         * Container which contains copy selection related views
         */
        View mCopySelectionToContainer;
        /**
         * The button which shows copy selection to popup menu
         */
        View mCopySelectionToButton;
        /**
         * The button which shows more popup menu
         */
        Button mMoreButton;
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
         * The loading control for the images loading operation (custom page
         * with images)
         */
        LoadingControl mImagesLoadingControl;
        /**
         * Generic loading control which shows progress information in the
         * overlay on top of activity view
         */
        GenericMultilineViewLoadingControl mOverlayLoadingControl;
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
         * Flag indicating custom images loading operation
         */
        boolean mLoadImages = false;
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

        /**
         * The source started the web activity
         */
        private Source mSource;

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
            mOverlayLoadingControl = new GenericMultilineViewLoadingControl(
                    view.findViewById(R.id.progressStatus));
            mPageLoadingProgress = (ProgressBar) view.findViewById(R.id.pageLoadingProgress);
            mWebView = (CustomWebView) view.findViewById(R.id.webView);
            initWebView();
            mCancelButton = (Button) view.findViewById(R.id.cancelButton);
            mCancelButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // if there wre some text copied to attributes then laucnh
                    // product edit activity before closing the web activity.
                    if (mSource == Source.PROD_DETAILS && !mUpdatedTextAttributes.isEmpty()) {
                        final Intent i = new Intent(getActivity(), ProductEditActivity.class);
                        // pass the product sku extra
                        i.putExtra(getString(R.string.ekey_product_sku), mProductSku);
                        // pass the updated text attribute information to the
                        // intent so the ProductEdit activity may handle it
                        i.putParcelableArrayListExtra(
                                ProductEditActivity.EXTRA_UPDATED_TEXT_ATTRIBUTES,
                                new ArrayList<CustomAttributeSimple>(mUpdatedTextAttributes));
                        startActivity(i);

                    }
                    getActivity().finish();
                }
            });
            mParsingImageUrlsLoadingControl = new SimpleViewLoadingControl(
                    view.findViewById(R.id.parsingImageUrlsStatusLine));

            mTipText = (TextView) view.findViewById(R.id.tipText);
            mTipText.setSelected(true);
            mImageInfoContainer = view.findViewById(R.id.imageInfoContainer);
            mCopySelectionToContainer = view.findViewById(R.id.copySelectionToContainer);
            mCopySelectionToButton = (Button) view.findViewById(R.id.copySelectionToButton);
            mCopySelectionToButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(getActivity(), v);
                    Menu menu = popup.getMenu();
                    initCopyToMenu(menu);
                    popup.show();
                }
            });
            mMoreButton = (Button) view.findViewById(R.id.moreButton);
            mMoreButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    new LoadRecentWebAddressesTaskAndShowMoreMenu().execute();
                }
            });

            mImageInfo = (TextView) view.findViewById(R.id.imageInfo);
            mImageInfoTooSmall = view.findViewById(R.id.imageInfoTooSmall);
            mImageContainer = view.findViewById(R.id.imageContainer);
            mWebViewContainer = view.findViewById(R.id.webViewContainer);
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
                        RecentWebAddressProviderAccessor.updateRecentWebAddressCounterAsync(
                                mLastImageUrl, mSettings.getUrl());
                        setState(State.WEB);
                    }
                }
            });
            mImageLoadingControl = new SimpleViewLoadingControl(
                    view.findViewById(R.id.imageLoading));
            mImagesLoadingControl = new SimpleViewLoadingControl(
                    view.findViewById(R.id.imagesLoading));

            mUploadStatusLine = view.findViewById(R.id.uploadStatusLine);
            mUploadStatusText = (TextView) view.findViewById(R.id.uploadStatusText);

            reinitFromIntent(getActivity().getIntent());
        }

        class MyJavaScriptInterface {
            final String TAG = MyJavaScriptInterface.class.getSimpleName();

            @JavascriptInterface
            public void processHTML(String html) {
                mLastLoadedPage = html;
                rememberLastLoadedUrl();
            }

            /**
             * The method to pass selected text value from the JavaScript to
             * Java
             * 
             * @param selection
             */
            @JavascriptInterface
            public void sendSelectionUpdatedEvent(String selection) {
                mWebView.setLastSelectedText(selection);
                // count down the latch so the awaiting for the selection result
                // method may proceed
                mWebView.getSelectionDoneSignal().countDown();
            }

            /**
             * The method to call the logcat debug from the Javascript
             * 
             * @param message
             */
            @JavascriptInterface
            public void debug(String message) {
                CommonUtils.debug(TAG, message);
            }

            /**
             * The method to check whether the WebView is in action mode from
             * the Javascript
             * 
             * @return
             */
            @JavascriptInterface
            public boolean isInActionMode() {
                return mWebView.isInActionMode();
            }

            /**
             * The method to check whether WebView is touched from the
             * Javascript
             * 
             * @return
             */
            @JavascriptInterface
            public boolean isTouching() {
                return mWebView.isTouching();
            }
            
            /**
             * Javascript will use this method to give haptic feedback to the user
             */
            @JavascriptInterface
            public void performHapticFeedback() {
                mWebView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);;
            }

            /**
             * Method to notify about image started loading event from the
             * Javascript
             */
            @JavascriptInterface
            public void startImageLoading() {
                GuiUtils.post(new Runnable() {
                    
                    @Override
                    public void run() {
                        mImagesLoadingControl.startLoading();
                    }
                });
            }
            
            /**
             * Method to notify about image stopped loading event from the
             * Javascript
             */
            @JavascriptInterface
            public void stopImageLoading() {
                GuiUtils.post(new Runnable() {

                    @Override
                    public void run() {
                        mImagesLoadingControl.stopLoading();
                    }
                });
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
            webSettings.setUserAgentString(mSettings.getWebViewUserAgent());

            final boolean hasJavascriptInterfaceBug = !CommonUtils.isHoneyCombOrHigher();
            if (!hasJavascriptInterfaceBug) {
                mWebView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
            }
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    // show the loading url in the mTipText
                    mTipText.setText(url);
                }

                @Override
                public void onPageFinished(WebView view, String address) {
                    CommonUtils.debug(TAG, "WebViewClient.onPageFinished");
                    if (mLoadImages) {
                        // if that is custom images loading page
                        mLoadImages = false;
                        mParseUrlsTask.loadImages();
                    }
                    // have the page spill its guts, with a secret prefix
                    if (hasJavascriptInterfaceBug) {
                        view.loadUrl("javascript:console.log('MAGIC'+document.getElementsByTagName('html')[0].innerHTML);");
                    } else {
                        view.loadUrl("javascript:window.HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                    }
                    view.loadUrl("javascript:"
                            + "document.body.style.paddingLeft='10pt';"
                            + "document.body.style.paddingRight='10pt';");
                    try {
                        view.loadUrl("javascript:" + CommonUtils.loadAssetAsString("web/custom.js"));
                    } catch (Exception ex) {
                        CommonUtils.error(TAG, ex);
                    }
                    // set the standard tip message
                    mTipText.setText(R.string.find_image_tip);
                }
            });
            
            mWebView.setWebChromeClient(new WebChromeClient() {
                Animation mSlideInAnimation = AnimationUtils.loadAnimation(getActivity(),
                        R.anim.slide_in_bottom);
                Animation mSlideOutAnimation = AnimationUtils.loadAnimation(getActivity(),
                        R.anim.slide_out_top);
                // additional animation init
                {
                    mSlideInAnimation.setDuration(ANIMATION_DURATION);
                    mSlideOutAnimation.setDuration(ANIMATION_DURATION);
                    mSlideOutAnimation.setAnimationListener(new AnimationListener() {

                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mPageLoadingProgress.setVisibility(ProgressBar.GONE);
                        }
                    });
                }
                
                @Override
                public void onProgressChanged(WebView view, int progress) {
                    super.onProgressChanged(view, progress);
                    if (progress < 100 && mPageLoadingProgress.getVisibility() == ProgressBar.GONE) {
                        mPageLoadingProgress.setVisibility(ProgressBar.VISIBLE);
                        mPageLoadingProgress.startAnimation(mSlideInAnimation);
                    }
                    mPageLoadingProgress.setProgress(progress);
                    if (progress == 100) {
                        mPageLoadingProgress.startAnimation(mSlideOutAnimation);
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
                                    mWebView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
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
            // what is the launch activity source?
            String source = intent.getStringExtra(EXTRA_SOURCE);
            if (source == null) {
                mSource = Source.ABS_PRODUCT;
            } else {
                mSource = Source.valueOf(source);
            }
            mWebView.clearHistory();
            mLastLoadedPage = null;
            Bundle extras = intent.getExtras();
            mProductSku = extras.getString(getString(R.string.ekey_product_sku));
            mSearchQuery = extras.getString(EXTRA_SEARCH_QUERY);
            setSearchDomains(extras.getStringArrayList(EXTRA_SEARCH_DOMAINS));
            mTextAttributes = extras.getParcelableArrayList(EXTRA_CUSTOM_TEXT_ATTRIBUTES);
            mUpdatedTextAttributes.clear();

            mCopySelectionToButton.setVisibility(mTextAttributes == null
                    || mTextAttributes.isEmpty() ? View.GONE : View.VISIBLE);
            refreshWebView();
            mUploadImageJobCallback.reinit(mProductSku, mSettings);
            // grab images button should be disabled if sku parameter is not passed
            mGrabImageBtn.setEnabled(mProductSku != null);
        }

        /**
         * Run the google search withing WebView for the passed product name and
         * search domain if present
         */
        public void refreshWebView() {
            String query = mSearchQuery;
            // if mSearchDomains is not empty google search should be performed
            // within the domains. Append "site: " parameter to the search
            // query. For more than one domain 'OR' operand is used
            if (!mSearchDomains.isEmpty()) {
                query += " site:" + TextUtils.join(" OR site:", mSearchDomains);
            }
            googleIt(query);
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

        private void parseUrls() {
            if (mLastLoadedPage == null) {
                GuiUtils.alert(R.string.page_not_yet_loaded);
            } else {
                if (mParseUrlsTask == null || mParseUrlsTask.isFinished()) {
                    // if there are no active parse URLs tasks
                    mParseUrlsTask = new ParseUrlsTask(mLastLoadedPage, mLastLoadedUrl);
                    mParseUrlsTask.execute();
                }
            }
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
                // stop all images loading indications
                while (mImagesLoadingControl.isLoading()) {
                    mImagesLoadingControl.stopLoading();
                }
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
            Animation slideOutLeftAnimation = AnimationUtils.makeOutAnimation(getActivity(), false);
            slideOutLeftAnimation.setDuration(ANIMATION_DURATION);
            Animation slideOutRightAnimation = AnimationUtils.makeOutAnimation(getActivity(), true);
            slideOutRightAnimation.setDuration(ANIMATION_DURATION);

            // remember current state such as it is used for various checks
            // later in the showNewStateWidgetsRunnable
            final State previousState = mCurrentState;
            final Runnable showNewStateWidgetsRunnable = new Runnable() {

                @Override
                public void run() {
                    Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(),
                            android.R.anim.fade_in);
                    fadeInAnimation.setDuration(ANIMATION_DURATION);
                    Animation slideInRightAnimation = AnimationUtils.makeInAnimation(getActivity(),
                            false);
                    slideInRightAnimation.setDuration(ANIMATION_DURATION);
                    Animation slideInLeftAnimation = AnimationUtils.makeInAnimation(getActivity(),
                            true);
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
                        case SELECTION:
                            if (previousState != State.WEB && previousState != State.SELECTION) {
                                mWebViewContainer.startAnimation(fadeInAnimation);
                                mWebViewContainer.setVisibility(View.VISIBLE);
                                mMoreButton.startAnimation(slideInRightAnimation);
                                mMoreButton.setVisibility(View.VISIBLE);
                            }
                            View slideInView = state == State.WEB ? mTipText
                                    : mCopySelectionToContainer;
                            slideInView.startAnimation(slideInLeftAnimation);

                            slideInView.setVisibility(View.VISIBLE);
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
                    case SELECTION:
                        final View slidingLeftView = mCurrentState == State.WEB ? mTipText
                                : mCopySelectionToContainer;
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
                                slidingLeftView.setVisibility(View.GONE);
                                // run scheduled operation to show new state
                                // widgets when the hiding widget animation ends
                                showNewStateWidgetsRunnable.run();
                            }
                        });
                        slidingLeftView.startAnimation(slideOutLeftAnimation);
                        if (state != State.SELECTION && state != State.WEB) {
                            mWebViewContainer.startAnimation(fadeOutAnimation);
                            mWebViewContainer.setVisibility(View.GONE);
                            mMoreButton.startAnimation(slideOutRightAnimation);
                            mMoreButton.setVisibility(View.GONE);
                        }

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
                    Animation slideInLeftAnimation = AnimationUtils.makeInAnimation(getActivity(),
                            true);
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
                    if (mProductSku != null) {
                        mUploadImageJobCallback.onGeneralBroadcastEventJobAdded(extra, mProductSku,
                                mSettings, this);
                    }
                    break;
                case WEB_TEXT_COPIED: {
                    CommonUtils.debug(TAG,
                            "onGeneralBroadcastEvent: received web text copied event");
                    // handle event for the same SKU as passed to the launch
                    // activity intent
                    if (TextUtils.equals(extra.getStringExtra(EventBusUtils.SKU),
                                    mProductSku)) {
                        String text = extra.getStringExtra(EventBusUtils.TEXT);
                        if (mTextAttributes != null && !TextUtils.isEmpty(text)) {
                            String attributeCode = extra.getStringExtra(EventBusUtils.ATTRIBUTE_CODE);
                            // search for attribute with same code and updated appended value information there
                            for (CustomAttributeSimple customAttribute : mTextAttributes) {
                                if (TextUtils.equals(attributeCode, customAttribute.getCode())) {
                                    mUpdatedTextAttributes.add(customAttribute);
                                    customAttribute.addAppendedValue(text);
                                    break;
                                }
                            }
                        }
                    }
                }
                    break;
                case WEBVIEW_USERAGENT_CHANGED:
                    CommonUtils.debug(TAG,
                            "onGeneralBroadcastEvent: received WebView User-Agent changed event");
                    // User-Agent string was updated. Update the WebView
                    // settings
                    mWebView.getSettings().setUserAgentString(mSettings.getWebViewUserAgent());
                    break;
                default:
                    break;
            }
        }

        /**
         * Get the text attributes information
         * 
         * @param textAttributes
         */
        public ArrayList<CustomAttributeSimple> getTextAttributes() {
            return mTextAttributes;
        }

        /**
         * Get the product sku information
         * 
         * @param productSku
         */
        public String getProductSku() {
            return mProductSku;
        }

        /**
         * Get settings
         * 
         * @return
         */
        public Settings getSettings() {
            return mSettings;
        }

        /**
         * Set the single domain which should be used for google search
         * 
         * @param domain the domain to set. May be null
         */
        void setSearchDomain(String domain) {
            mSearchDomains.clear();
            if (domain != null) {
                mSearchDomains.add(domain);
            }
        }

        /**
         * Set the search domains which should be used for the google search
         * 'OR' condition
         * 
         * @param searchDomains the domains to set. May be null
         */
        void setSearchDomains(List<String> searchDomains) {
            mSearchDomains.clear();
            if (searchDomains != null) {
                mSearchDomains.addAll(searchDomains);
            }
        }

        /**
         * Init the copy to menu items based on the text attributes information
         * passed to the activity
         * 
         * @param menu
         */
        public void initCopyToMenu(Menu menu) {
            if (mTextAttributes != null) {
                // add menu item for each custom text attribute to the menu
                for (final CustomAttributeSimple attribute : mTextAttributes) {
                    String menuLabel = attribute.getMainLabel();
                    if (TextUtils.isEmpty(menuLabel)) // so that attrs with no label don't appear as empty menu items
                        menuLabel = attribute.getCode();
                    MenuItem mi = menu.add(menuLabel);
                    if (!TextUtils.isEmpty(attribute.getSelectedValue())
                            || attribute.hasAppendedValues()) {
                        // if there are non empty attribute value or appended
                        // values
                        highlightMenuItem(mi);
                    }
                    mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            try {
                                // send the genearl WEB_TEXT_COPIED broadcast
                                // event
                                Intent intent = EventBusUtils
                                        .getGeneralEventIntent(EventType.WEB_TEXT_COPIED);
                                // where the text should be copied
                                intent.putExtra(EventBusUtils.ATTRIBUTE_CODE, attribute.getCode());
                                // the selected text itself
                                intent.putExtra(EventBusUtils.TEXT, mWebView.getSelectedText());
                                intent.putExtra(EventBusUtils.SKU, getProductSku());
                                EventBusUtils.sendGeneralEventBroadcast(intent);
                                RecentWebAddressProviderAccessor
                                        .updateRecentWebAddressCounterAsync(mWebView.getUrl(),
                                                getSettings().getUrl());
                                mWebView.exitActionMode();
                            } catch (Exception e) {
                                GuiUtils.error(TAG, R.string.copyTextError, e);
                            }
                            return true;
                        }
                    });
                }
            }
        }

        /**
         * The method which is executed from the onActivityResult method when
         * the text is scanned
         * 
         * @param requestCode
         * @param resultCode
         * @param data
         */
        public void onTextScanned(int resultCode, Intent data) {
            if (resultCode == RESULT_OK) {
                String contents = ScanUtils.getSanitizedScanResult(data);
                if (contents != null) {
                    if (contents.matches("(?i).*" + ImageUtils.PROTO_PREFIX + ".*")) {
                        mWebView.loadUrl(contents);
                    } else {
                        googleIt(contents);
                    }
                }
            }
        }

        /**
         * Highlight the menu item by making text yellow
         * 
         * @param menuItem to change the text color at
         */
        public void highlightMenuItem(MenuItem menuItem) {
            SpannableString s = new SpannableString(menuItem.getTitle());
            s.setSpan(new ForegroundColorSpan(Color.YELLOW), 0, s.length(), 0);
            menuItem.setTitle(s);
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

        private class ParseUrlsTask extends SimpleAsyncTask {
            String mContent;
            String mUrl;
            String[] mUrls;
            public static final String TAG = "ParseUrlsTask";

            public ParseUrlsTask(String content, String url) {
                super(mParsingImageUrlsLoadingControl);
                this.mContent = content;
                this.mUrl = url;
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    mUrls = ImageUtils.extractImageUrls(mContent, mUrl);
                    return !isCancelled();
                } catch (Exception ex) {
                    GuiUtils.noAlertError(TAG, ex);
                }
                return false;
            }

            @Override
            protected void onSuccessPostExecute() {
                if (isActivityAlive()) {
                    if (mUrls == null || mUrls.length == 0) {
                        GuiUtils.alert(R.string.no_urls_found);
                    } else {
                    	// write gathered URL list to log file
                        com.mageventory.util.Log.d(TAG, TextUtils.join("\n", mUrls)); 
                        try {
                            // load custom html page which will wrap images
                            mWebView.loadUrl("file:///android_asset/web/images.html");
                            // set the custom images loading flag
                            mLoadImages = true;
                        } catch (Exception e) {
                            GuiUtils.error(TAG, R.string.errorGeneral, e);
                        }
                    }
                }
            }

            /**
             * Load images to the current custom images wrapping HTML page via
             * the Javascript
             */
            void loadImages()
            {
                StringBuilder sb = new StringBuilder();
                sb.append("javascript:");
                for (String url : mUrls) {
                    sb.append(CommonUtils.format("addImageUrl(\"%1$s\");", url));
                }
                sb.append("loadImages();");
                mWebView.loadUrl(sb.toString());
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

        /**
         * Asynchronous task to load all {@link RecentWebAddress}es information
         * from the database and show more popup menu.
         */
        class LoadRecentWebAddressesTaskAndShowMoreMenu extends AbstractLoadRecentWebAddressesTask {

            /**
             * The maximum recent web addresses count which can be shown in the
             * popup menu
             */
        	static final int MAXIMUM_RECENT_WEB_ADDRESSES_COUNT = 20;
        	
            public LoadRecentWebAddressesTaskAndShowMoreMenu() {
                super(mOverlayLoadingControl
                        .getLoadingControlWrapper(ProgressData.RECENT_WEB_ADDRESSES_LIST),
                        mSettings.getUrl());
            }

            @Override
            protected void onSuccessPostExecute() {
                PopupMenu popup = new PopupMenu(getActivity(), mMoreButton);
                MenuInflater inflater = popup.getMenuInflater();
                Menu menu = popup.getMenu();
                inflater.inflate(R.menu.web_more, menu);

                // menu item order in the category for the custom menu items
                // sorting
                int order = 1;
                // init dynamic recent web addresses menu items
                for (final RecentWebAddress recentWebAddress : recentWebAddresses) {
                    MenuItem mi = menu.add(Menu.NONE, View.NO_ID, 10 + order++,
                            recentWebAddress.getDomain());
                    if (mSearchDomains.contains(recentWebAddress.getDomain())) {
                        // highlight the search domains which are used for the
                        // current search
                        highlightMenuItem(mi);
                    }
                            
                    mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            // reinit search for the new domain
                            setSearchDomain(recentWebAddress.getDomain());
                            refreshWebView();
                            return true;
                        }
                    });
                    // break if reached the maximum recent web addresses
                    // limit
                    if (order > MAXIMUM_RECENT_WEB_ADDRESSES_COUNT) {
                        break;
                    }
                }
                // set the general on menu item click listener for the static menu items
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int menuItemIndex = item.getItemId();
                        switch (menuItemIndex) {
                            case R.id.menu_view_all_images:
                                parseUrls();
                                break;
                            case R.id.menu_scan:
                                ScanUtils.startScanActivityForResult(getActivity(), SCAN_QR_CODE,
                                        R.string.scan_address);
                                break;
                            case R.id.menu_search_all_of_internet:
                                setSearchDomain(null);
                                refreshWebView();
                                break;
                            default:
                                return false;
                        }
                        return true;
                    }
                });

                popup.show();
            }

        }
    }
}
