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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
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
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.activity.LibraryActivity.LibraryUiFragment.AbstractAddNewImageTask;
import com.mageventory.activity.LibraryActivity.LibraryUiFragment.AbstractUploadImageJobCallback;
import com.mageventory.activity.WebActivity.WebUiFragment.State;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.bitmapfun.util.ImageFetcher;
import com.mageventory.fragment.SearchOptionsFragment;
import com.mageventory.fragment.SearchOptionsFragment.OnRecentWebAddressClickedListener;
import com.mageventory.fragment.base.BaseFragment;
import com.mageventory.job.JobControlInterface;
import com.mageventory.model.CustomAttribute.ContentType;
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
import com.mageventory.util.concurent.SerialExecutor;
import com.mageventory.util.loading.GenericMultilineViewLoadingControl;

public class WebActivity extends BaseFragmentActivity implements MageventoryConstants {
    private static final String TAG = WebActivity.class.getSimpleName();
    /**
     * The key for the custom text attributes intent extra
     */
    public static final String EXTRA_CUSTOM_TEXT_ATTRIBUTES = "CUSTOM_TEXT_ATTRIBUTES";
    /**
     * The key for the custom attributes of {@link ContentType#WEB_ADDRESS}
     * content type intent extra
     */
    public static final String EXTRA_WEB_ADDRESS_ATTRIBUTES = "WEB_ADDRESS_ATTRIBUTES";
    /**
     * The key for the search original (full) query intent extra
     */
    public static final String EXTRA_SEARCH_ORIGINAL_QUERY = "SEARCH_ORIGINAL_QUERY";
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
             * The WebUiFragment state which was active before Selection
             * ActionMode was activated
             */
            State mPreviousState;

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
                // get the current fragment state
                State currentState = fragment.getState();
                if (currentState != State.SELECTION) {
                    // if current state is not SELECTION state
                    mPreviousState = currentState;
                }
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
                fragment.setState(mPreviousState);
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

    public static class WebUiFragment extends BaseFragment implements
            GeneralBroadcastEventHandler {

        static final int ANIMATION_DURATION = 500;

        /**
         * The URL for the custom save images page
         */
        static final String IMAGES_URL = "file:///android_asset/web/images.html";

        /**
         * The regular expression pattern to detect google books URLs
         */
        public static final String BOOKS_URL_PATTERN = "^" + ImageUtils.PROTO_PREFIX
                + "[^\\.]*\\.?books.google.*";

        /**
         * An enum describing possible WebUiFragment states
         */
        enum State {
            WEB, SELECTION,
            /**
             * The state for the new images displaying functionality
             */
            IMAGES_NEW,
            /**
             * The state for the view all text functionality
             */
            VIEW_ALL_TEXT
        }

        CustomWebView mWebView;
        Button mCancelButton;
        /**
         * The loading control for the parsing image urls task
         */
        LoadingControl mParsingImageUrlsLoadingControl;
        /**
         * The loading control for the parsing text task
         */
        LoadingControl mParsingTextLoadingControl;
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
         * Attributes with the {@link ContentType#WEB_ADDRESS} content type.
         */
        ArrayList<CustomAttributeSimple> mWebAddressAttributes;
        /**
         * Updated text attributes. Used to store attribute values updates
         * before the product edit activity launch
         */
        Set<CustomAttributeSimple> mUpdatedTextAttributes = new HashSet<CustomAttributeSimple>();
        /**
         * Updated web address attributes. Used to store attribute values updates
         * before the product edit activity launch
         */
        Set<CustomAttributeSimple> mUpdatedWebAddressAttributes = new HashSet<CustomAttributeSimple>();

        /**
         * The query which should be used in google search
         */
        String mSearchQuery;
        /**
         * The full query which is used for query management functionality
         */
        String mSearchOriginalQuery;
        /**
         * The URL for the last loaded web page (excluding the local one which
         * shows images)
         */
        String mLastLoadedWebPageUrl;
        /**
         * Flag indicating page loading is active state
         * TODO perhaps should be replaced with the AtomicInteger counter
         */
        AtomicBoolean mPageLoading = new AtomicBoolean(false);
        String mLastLoadedPage;
        String mLastLoadedUrl;
        ProgressBar mPageLoadingProgress;
        ParseUrlsTask mParseUrlsTask;
        /**
         * Reference to the last parse text task to check whether it is running
         * or not
         */
        ParseTextTask mParseTextTask;
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
         * Button for the back navigation from the images state
         */
        Button mBackBtn;
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

        /**
         * Reference to the last array of URLs passed to the loadImages method
         * so it may be accessed when custom images page will be loaded
         */
        String[] mUrls;

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
                    // if there were some text copied to attributes then launch
                    // product edit activity before closing the web activity.
                    launchProductEditActivityIfNecessary(null);
                    getActivity().finish();
                }

            });
            mParsingImageUrlsLoadingControl = new SimpleViewLoadingControl(
                    view.findViewById(R.id.parsingImageUrlsStatusLine));
            mParsingTextLoadingControl = new SimpleViewLoadingControl(
                    view.findViewById(R.id.parsingTextStatusLine));

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
                    showMorePopup();
                }
            });

            mWebViewContainer = view.findViewById(R.id.webViewContainer);
            mBackBtn = (Button) view.findViewById(R.id.backButton);
            mBackBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (mCurrentState == State.IMAGES_NEW) {
                        isBackKeyOverrode();
                    }
                }
            });
            mImagesLoadingControl = new SimpleViewLoadingControl(
                    view.findViewById(R.id.imagesLoading));

            reinitFromIntent(getActivity().getIntent());
        }

        /**
         * Implementation of the bridge between Javascript and Java. Methods
         * present in this implementation may be called directly from the
         * WebView using Javascript
         */
        class CustomJavaScriptInterface {
            final String TAG = CustomJavaScriptInterface.class.getSimpleName();

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
             * The method to pass parsed web page text value from the JavaScript
             * to Java
             * 
             * @param text
             */
            @JavascriptInterface
            public void sendPageSimplifiedHtmlUpdatedEvent(String text) {
                if (mParseTextTask != null) {
                    // if task is not null
                    mParseTextTask.setText(text);
                }
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

            /**
             * The method to initiate save image process from the Javascript
             * side
             * 
             * @param url the url of the image to save
             */
            @JavascriptInterface
            public void saveImage(final String url) {
                GuiUtils.post(new Runnable() {
                    @Override
                    public void run() {
                        // send the general IMAGE_ADDED broadcast event
                        Intent intent = EventBusUtils.getGeneralEventIntent(EventType.IMAGE_ADDED);
                        // the image path
                        intent.putExtra(EventBusUtils.PATH, url);
                        intent.putExtra(EventBusUtils.SKU, mProductSku);
                        EventBusUtils.sendGeneralEventBroadcast(intent);

                        // download images to cache
                        new AsyncImageFetcher(url)
                                .executeOnExecutor(AsyncImageFetcher.IMAGE_FETCHER_EXECUTOR);
                        // add new image upload job for existing product
                        if (!TextUtils.isEmpty(mProductSku)) {
                            new AddNewImageTask(url).execute();
                            RecentWebAddressProviderAccessor.updateRecentWebAddressCounterAsync(
                                    mLastLoadedWebPageUrl, mSettings.getUrl());
                        }
                        if (mUrls.length == 1) {
                            // if there were only one image URL, return to
                            // previous page
                            isBackKeyOverrode();
                        }
                    }
                });
            }

        }

        private void initWebView() {
            WebSettings webSettings = mWebView.getSettings();
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
            webSettings.setJavaScriptEnabled(true);
            webSettings.setSupportZoom(true);
            webSettings.setUserAgentString(mSettings.getWebViewUserAgent());

            final boolean hasJavascriptInterfaceBug = !CommonUtils.isHoneyCombOrHigher();
            if (!hasJavascriptInterfaceBug) {
                mWebView.addJavascriptInterface(new CustomJavaScriptInterface(), "HTMLOUT");
            }
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    CommonUtils.debug(TAG, "WebViewClient.onPageStarted: %1$s", url);
                    // set the page loading flag such as page started to load
                    mPageLoading.set(true);
                    if (!TextUtils.equals(url, IMAGES_URL) && ImageUtils.isUrl(url)) {
                        // if the URL is not the custom images URL and is http
                        // or https URL remember last loaded real web page
                        // URL
                        mLastLoadedWebPageUrl = url;
                    }
                    // show the loading url in the mTipText
                    mTipText.setText(mLastLoadedWebPageUrl);
                }

                @Override
                public void onPageFinished(WebView view, String address) {
                    CommonUtils.debug(TAG, "WebViewClient.onPageFinished: %1$s", address);
                    if (!isAdded()) {
                        // if activity was destroyed interrupt method invocation
                        // to prevent app crashes when activity is closed but
                        // event fired
                        return;
                    }
                    if (TextUtils.equals(address, IMAGES_URL)) {
                        CommonUtils.debug(TAG, "WebViewClient.onPageFinished: loading images");
                        appendImages();
                    }
                    // have the page spill its guts, with a secret prefix
                    if (hasJavascriptInterfaceBug) {
                        view.loadUrl("javascript:console.log('MAGIC'+document.getElementsByTagName('html')[0].innerHTML);");
                    } else {
                        view.loadUrl("javascript:window.HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                    }
                    try {
                        view.loadUrl("javascript:" + CommonUtils.loadAssetAsString("web/custom.js"));
                    } catch (Exception ex) {
                        CommonUtils.error(TAG, ex);
                    }
                    // set the standard tip message
                    mTipText.setText(getString(R.string.find_image_tip, mLastLoadedWebPageUrl));
                    // reset the page loading flag such as page load is finished
                    mPageLoading.set(false);
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
                    if (mPageLoading.get() || mCurrentState == State.IMAGES_NEW) {
                        // if page is still loading or current state is images
                        // selection
                    	//
                        // finish handling of long click event by returning
                        // true. This disable text selection functionality for
                        // the custom images page
                        return true;
                    }
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
                                    loadImages(val);
                                }
                                if (val == null) {
                                    if (mCurrentState != State.SELECTION) {
                                        // if selecting text mode is not active
                                    	
                                        // for a now i don't know how to
                                        // correctly check that the long press
                                        // will not trigger text selection mode
                                        // so put the delayed action which
                                        // checks whether the selection mode was
                                        // activated after 500 milliseconds
                                        // delay.
                                        // TODO find better approach
                                        GuiUtils.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (mCurrentState != State.SELECTION && isAdded()) {
                                                    // if state was not changed
                                                    // within 500 milliseconds
                                                    if (mCurrentState == State.VIEW_ALL_TEXT) {
                                                        // if current state is
                                                        // VIEW_ALL_TEXT
                                                        GuiUtils.showMessageDialog(
                                                                null,
                                                                R.string.no_text_selected_try_again,
                                                                getActivity());
                                                    } else {
                                                    	// ask to parse URLs or text
                                                        AlertDialog.Builder alert = new AlertDialog.Builder(
                                                                getActivity());

                                                        alert.setMessage(R.string.nothing_to_select);

                                                        alert.setNegativeButton(
                                                                R.string.view_all_images,
                                                                new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(
                                                                            DialogInterface dialog,
                                                                            int which) {
                                                                        parseUrls();
                                                                    }
                                                                });
                                                        alert.setNeutralButton(
                                                                R.string.view_all_text,
                                                                new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(
                                                                            DialogInterface dialog,
                                                                            int which) {
                                                                        viewAllText();
                                                                    }
                                                                });
                                                        alert.setPositiveButton(
                                                                R.string.try_again,
                                                                new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(
                                                                            DialogInterface dialog,
                                                                            int which) {
                                                                        // do
                                                                        // nothing
                                                                    }
                                                                });
                                                        alert.show();
                                                    }
                                                }
                                            }
                                        }, 500);
                                    }
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
            mSearchOriginalQuery = extras.getString(EXTRA_SEARCH_ORIGINAL_QUERY);
            mSearchQuery = extras.getString(EXTRA_SEARCH_QUERY);
            setSearchDomains(extras.getStringArrayList(EXTRA_SEARCH_DOMAINS));
            mTextAttributes = extras.getParcelableArrayList(EXTRA_CUSTOM_TEXT_ATTRIBUTES);
            mUpdatedTextAttributes.clear();
            mWebAddressAttributes = extras.getParcelableArrayList(EXTRA_WEB_ADDRESS_ATTRIBUTES);
            mUpdatedWebAddressAttributes.clear();

            mCopySelectionToButton.setVisibility(mTextAttributes == null
                    || mTextAttributes.isEmpty() ? View.GONE : View.VISIBLE);
            refreshWebView();
            mUploadImageJobCallback.reinit(mProductSku, mSettings);
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
            if (mWebView.canGoBack()) {
                // if back key is pressed and fragment is in image viewing state
                // or view all text state then switch state to web
                if (mCurrentState == State.IMAGES_NEW || mCurrentState == State.VIEW_ALL_TEXT) {
                    setState(State.WEB);
                }
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
         * Get the current fragment state
         * @return
         */
        State getState() {
            return mCurrentState;
        }

        /**
         * Set the current fragment state: either IMAGE_NEW or WEB or
         * VIEW_ALL_TEXT or SELECTION. Setting state will adjust visibility of different
         * views with various animation
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
                        case IMAGES_NEW:
                            mBackBtn.startAnimation(slideInRightAnimation);
                            // this is used only to fill the space released by
                            // the tip text container so the exit button will
                            // not jump to left
                            mImageInfoContainer.setVisibility(View.VISIBLE);
                            mBackBtn.setVisibility(View.VISIBLE);
                            break;
                        case WEB:
                        case VIEW_ALL_TEXT:
                        case SELECTION:
                            if (previousState == State.IMAGES_NEW) {
                                mMoreButton.startAnimation(slideInRightAnimation);
                                mCancelButton.startAnimation(slideInRightAnimation);
                                mMoreButton.setVisibility(View.VISIBLE);
                                mCancelButton.setVisibility(View.VISIBLE);
                            } else if (previousState != State.WEB
                                    && previousState != State.SELECTION
                                    && previousState != State.VIEW_ALL_TEXT) {
                                mWebViewContainer.startAnimation(fadeInAnimation);
                                mWebViewContainer.setVisibility(View.VISIBLE);
                                mMoreButton.startAnimation(slideInRightAnimation);
                                mMoreButton.setVisibility(View.VISIBLE);
                            }
                            View slideInView = state == State.WEB || state == State.VIEW_ALL_TEXT ? mTipText
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
                    case IMAGES_NEW:
                        mBackBtn.startAnimation(slideOutRightAnimation);

                        mImageInfoContainer.setVisibility(View.GONE);
                        mBackBtn.setVisibility(View.GONE);
                        showNewStateWidgetsRunnable.run();
                        break;
                    case WEB:
                    case VIEW_ALL_TEXT:
                    case SELECTION:
                        if (!((mCurrentState == State.WEB || mCurrentState == State.VIEW_ALL_TEXT) && (state == State.WEB || state == State.VIEW_ALL_TEXT))) {
                            // if not current and selecting state is either WEB
                            // or VIEW_ALL_TEXT
                            final View slidingLeftView = mCurrentState == State.WEB
                                    || mCurrentState == State.VIEW_ALL_TEXT ? mTipText
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
                                    // animation ends to avoid Exit Search
                                    // button
                                    // flickering.
                                    slidingLeftView.setVisibility(View.GONE);
                                    if (state == State.IMAGES_NEW) {
                                        mCancelButton.setVisibility(View.GONE);
                                    }
                                    // run scheduled operation to show new state
                                    // widgets when the hiding widget animation
                                    // ends
                                    showNewStateWidgetsRunnable.run();
                                }
                            });
                            slidingLeftView.startAnimation(slideOutLeftAnimation);
                        }
                        if (state == State.IMAGES_NEW) {
                            mCancelButton.startAnimation(slideOutRightAnimation);
                            mMoreButton.startAnimation(slideOutRightAnimation);

                            mMoreButton.setVisibility(View.GONE);
                        } else if (state != State.SELECTION && state != State.WEB
                                && state != State.VIEW_ALL_TEXT) {
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
         * Launch the product edit activity if necessary with the updated
         * attributes information and/or to load details for the book with the
         * specified id <br><br>
         * Product edit activity will be launched only in case web activity was
         * opened from the produc details activity and has modified attributes
         * or non empty book id
         * 
         * @param bookId the id of the book which details should be loaded after
         *            the product edit activity launch
         */
        public void launchProductEditActivityIfNecessary(String bookId) {
            if (mSource == Source.PROD_DETAILS
                    && !(mUpdatedTextAttributes.isEmpty() && mUpdatedWebAddressAttributes.isEmpty() && TextUtils
                            .isEmpty(bookId))) {
                // if web activity was opened from the product details and text
                // was copied to at least one attribute or web address saved or
                // book id is not empty
                final Intent i = new Intent(getActivity(), ProductEditActivity.class);
                // pass the product SKU extra
                i.putExtra(getString(R.string.ekey_product_sku), mProductSku);
                // pass the book id extra
                i.putExtra(ProductEditActivity.EXTRA_BOOK_ID, bookId);
                // pass the updated text attribute information to the
                // intent so the ProductEdit activity may handle it
                i.putParcelableArrayListExtra(ProductEditActivity.EXTRA_UPDATED_TEXT_ATTRIBUTES,
                        new ArrayList<CustomAttributeSimple>(mUpdatedTextAttributes));
                // pass the updated web address attribute information to
                // the intent so the ProductEdit activity may handle it
                i.putParcelableArrayListExtra(ProductEditActivity.EXTRA_PREDEFINED_ATTRIBUTES,
                        new ArrayList<CustomAttributeSimple>(mUpdatedWebAddressAttributes));
                startActivity(i);
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
                    break;
                }
                case WEB_ADDRESS_COPIED: {
                    CommonUtils.debug(TAG,
                            "onGeneralBroadcastEvent: received web address copied event");
                    // handle event for the same SKU as passed to the launch
                    // activity intent
                    if (TextUtils.equals(extra.getStringExtra(EventBusUtils.SKU), mProductSku)) {
                        String text = extra.getStringExtra(EventBusUtils.TEXT);
                        if (mWebAddressAttributes != null && !TextUtils.isEmpty(text)) {
                            // assign value to each web address attribute
                            for (CustomAttributeSimple customAttribute : mWebAddressAttributes) {
                                mUpdatedWebAddressAttributes.add(customAttribute);
                                customAttribute.setSelectedValue(text);
                            }
                        }
                    }
                    break;
                }
                case BOOK_DETAILS_REQUEST: {
                    CommonUtils.debug(TAG,
                            "onGeneralBroadcastEvent: received book details request event");
                    // handle event for the same SKU as passed to the launch
                    // activity intent
                    if (TextUtils.equals(extra.getStringExtra(EventBusUtils.SKU), mProductSku)) {
                        String code = extra.getStringExtra(EventBusUtils.CODE);
                        // check whether the product edit activity should be
                        // launched and launch if necessary
                        launchProductEditActivityIfNecessary(code);
                        // close web search activity
                        getActivity().finish();
                    }
                    break;
                }
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
                                // send the general WEB_TEXT_COPIED broadcast
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
                                        .updateRecentWebAddressCounterAsync(mLastLoadedWebPageUrl,
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
         * Highlight the menu item by making text gray
         * 
         * @param menuItem to change the text color at
         */
        public void highlightMenuItem(MenuItem menuItem) {
            SpannableString s = new SpannableString(menuItem.getTitle());
            s.setSpan(new ForegroundColorSpan(Color.GRAY), 0, s.length(), 0);
            menuItem.setTitle(s);
        }

        /**
         * Load images to the current custom images wrapping HTML page via the
         * Javascript
         */
        void appendImages() {
            CommonUtils.debug(TAG, "appendImages: called");
            StringBuilder sb = new StringBuilder();
            sb.append("javascript:");
            for (String url : mUrls) {
                sb.append(CommonUtils.format("addImageUrl(\"%1$s\");", url));
            }
            sb.append(CommonUtils.format("loadImages(\"%1$s\",\"%2$s\",\"%3$s\",\"%4$s\");",
            // formatting string to display image size
                    CommonUtils.getStringResource(R.string.image_info_size_format_web2),
                    // save button text
                    CommonUtils.getStringResource(R.string.grab_image),
                    // saved button text
                    CommonUtils.getStringResource(R.string.image_grabbed),
                    // text to be shown when no images of acceptable size found
                    CommonUtils.getStringResource(R.string.no_images_of_acceptable_size_found2)));
            mWebView.loadUrl(sb.toString());
        }

        /**
         * Load custom page showing images with URLs
         * 
         * @param urls the image URLs to be shown
         */
        void loadImages(String... urls) {
            CommonUtils.debug(TAG, "loadImages: called");
            // remember urls so it may be accessed when the custom page will
            // finish loading
            mUrls = urls;
            // stop any currently loading pages to prevent invalid context in
            // the onPageFinished method
            mWebView.stopLoading();
            // load custom html page which will wrap images
            mWebView.loadUrl(IMAGES_URL);
            // set the corresponding state
            setState(State.IMAGES_NEW);
        }

        /**
         * Check the URL whether it matches {@link #BOOKS_URL_PATTERN} and get
         * the "id" query parameter if it does
         * 
         * @param url the URL to check
         * @return "id" query parameter from the url in case it is valid books
         *         URL pattern
         */
        String checkBookUrlAndGetBookId(String url) {
            String result = null;
            if (url != null && url.matches(BOOKS_URL_PATTERN)) {
                // if URL is not empty and matches books URL pattern
                Uri uri = Uri.parse(url);
                // get the "id" query parameter which is book id
                result = uri.getQueryParameter("id");
            }
            return result;
        }

        /**
         * Parse web page text and show as a simplified text page
         */
        public void viewAllText() {
            if (mParseTextTask == null || mParseTextTask.isFinished()) {
                // if parse text task is null or already
                // finished
                mParseTextTask = new ParseTextTask();
                mParseTextTask.execute();
            }
        }

        /**
         * Show the popup menu for the More button
         */
        void showMorePopup() {
            PopupMenu popup = new PopupMenu(getActivity(), mMoreButton);
            MenuInflater inflater = popup.getMenuInflater();
            Menu menu = popup.getMenu();
            inflater.inflate(R.menu.web_more, menu);

            // allow saving of the web address only in case there are web
            // address attributes available
            menu.findItem(R.id.menu_save_web_address).setVisible(
                    mWebAddressAttributes != null && !mWebAddressAttributes.isEmpty());

            String url = mWebView.getUrl();
            final String bookId = checkBookUrlAndGetBookId(url);
            // make the save book details menu item visible only in case book id
            // is not empty
            menu.findItem(R.id.menu_save_book_details).setVisible(!TextUtils.isEmpty(bookId));

            // set the general on menu item click listener for the static menu
            // items
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    int menuItemIndex = item.getItemId();
                    switch (menuItemIndex) {
                        case R.id.menu_manage_query:
                            new LoadRecentWebAddressesTaskAndShowSearchOptions()
                                    .executeOnExecutor(RecentWebAddressProviderAccessor.sRecentWebAddressesExecutor);
                            break;
                        case R.id.menu_view_all_images:
                            parseUrls();
                            break;
                        case R.id.menu_view_all_text:
                            viewAllText();
                            break;
                        case R.id.menu_scan:
                            ScanUtils.startScanActivityForResult(getActivity(), SCAN_QR_CODE,
                                    R.string.scan_address);
                            break;
                        case R.id.menu_save_web_address:
                            try {
                                // send the general WEB_ADDRESS_COPIED broadcast
                                // event
                                Intent intent = EventBusUtils
                                        .getGeneralEventIntent(EventType.WEB_ADDRESS_COPIED);
                                // the selected text itself
                                intent.putExtra(EventBusUtils.TEXT, mLastLoadedWebPageUrl);
                                intent.putExtra(EventBusUtils.SKU, getProductSku());
                                EventBusUtils.sendGeneralEventBroadcast(intent);
                                RecentWebAddressProviderAccessor
                                        .updateRecentWebAddressCounterAsync(mLastLoadedWebPageUrl,
                                                getSettings().getUrl());
                            } catch (Exception e) {
                                GuiUtils.error(TAG, R.string.errorGeneral, e);
                            }
                            break;
                        case R.id.menu_save_book_details:
                            // send the general BOOK_DETAILS_REQUEST broadcast
                            // event
                            Intent intent = EventBusUtils
                                    .getGeneralEventIntent(EventType.BOOK_DETAILS_REQUEST);
                            // the book id
                            intent.putExtra(EventBusUtils.CODE, bookId);
                            intent.putExtra(EventBusUtils.SKU, getProductSku());
                            EventBusUtils.sendGeneralEventBroadcast(intent);
                            RecentWebAddressProviderAccessor.updateRecentWebAddressCounterAsync(
                                    mLastLoadedWebPageUrl, getSettings().getUrl());
                            break;
                        default:
                            return false;
                    }
                    return true;
                }
            });
        
            popup.show();
        }

        /**
         * Simple task to fetch images to the HTTP cache asynchronously
         * 
         * @author Eugene Popovich
         */
        private static class AsyncImageFetcher extends SimpleAsyncTask {
            /**
             * Tag used for logging
             */
            static final String TAG = AsyncImageFetcher.class.getSimpleName();
            /**
             * Task executor to do not use standard asynchronous task thread pool
             */
            static final SerialExecutor IMAGE_FETCHER_EXECUTOR = new SerialExecutor(
                    Executors.newSingleThreadExecutor());
            
            /**
             * The URL to fetch
             */
            String mUrl;

            /**
             * @param url the URL to fetch
             */
            AsyncImageFetcher(String url) {
                super(null);
                mUrl = url;
            }

            @Override
            protected void onSuccessPostExecute() {
                // do nothing. This is a background task
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    CommonUtils.debug(TAG, "doInBackground: fetching %1$s", mUrl);
                    ImageFetcher.downloadBitmap(MyApplication.getContext(), mUrl, null);
                    return true;
                } catch (Exception ex) {
                    CommonUtils.error(TAG, ex);
                }
                return false;
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
                // do nothing
            }
        }

        private class ParseTextTask extends SimpleAsyncTask {
            /**
             * Tag used for logging
             */
            public static final String TAG = "ParseTextTask";

            /**
             * The parsed page text
             */
            String mPageText;
            /**
             * The count down latch to await until mPageText variable will be
             * specified via the Java-JavaScript bridge
             */
            CountDownLatch mPageTextDoneSignal;
            /**
             * Flag indicating the page text has been successfully initialized
             */
            boolean mSuccess;
            
            public ParseTextTask() {
                super(mParsingTextLoadingControl);
                mPageTextDoneSignal = new CountDownLatch(1);
                mPageText = null;
                // start the get page text javascript operation
                mWebView.loadUrl("javascript:window.HTMLOUT.sendPageSimplifiedHtmlUpdatedEvent(getPageSimplifiedHtml());");
            }

            /**
             * Set the parsed page text
             * 
             * @param text the web page parsed text
             */
            public void setText(String text) {
                mPageText = text;
                // count down the latch so the awaiting for the selection
                // result method may proceed
                mPageTextDoneSignal.countDown();
            }
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    // javascript call is not synchronous so we
                    // should wait for its done via the
                    // CountDownLatch
                    mSuccess = false;
                    try {
                        mSuccess = mPageTextDoneSignal.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                    }
                    return !isCancelled();
                } catch (Exception ex) {
                    GuiUtils.error(TAG, R.string.getTextError, ex);
                }
                return false;
            }


            @Override
            protected void onSuccessPostExecute() {
                if (isActivityAlive()) {
                    if (mSuccess) {
                        // if text successfully parsed
                        mWebView.loadDataWithBaseURL(
                                "",
                                // generate web page
                                CommonUtils
                                        .format("<html><body style=\"padding-left: 10pt; padding-right: 10pt;\">%1$s</body></html>",
                                                mPageText), "text/html", "UTF-8", "");
                        setState(State.VIEW_ALL_TEXT);
                    } else {
                        // if parse text timeout error occurred
                        GuiUtils.alert(R.string.getTextTimeoutError);
                    }
                }
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
                            loadImages(mUrls);
                        } catch (Exception e) {
                            GuiUtils.error(TAG, R.string.errorGeneral, e);
                        }
                    }
                }
            }

        }

        /**
         * Implementation of {@link AbstractAddNewImageTask}. Add the upload
         * image job for the product
         */
        private class AddNewImageTask extends AbstractAddNewImageTask {

            public AddNewImageTask(String filePath) {
                super(filePath, mProductSku, false, WebUiFragment.this.mJobControlInterface,
                        new SettingsSnapshot(getActivity()), null);
            }

            @Override
            protected String getTargetFileName(File source) {
                String fileName = super.getTargetFileName(source);
                String extension = FileUtils.getExtension(fileName);
                // return UUID genrated file name with the same extension as
                // source
                return UUID.randomUUID() + (extension == null ? "" : ("." + extension));
            }

            @Override
            protected void onSuccessPostExecute() {
                // GuiUtils.alert(R.string.upload_job_added_to_queue); //
                // Another alert will be issued once image upload is finished
            }
        }

        /**
         * Asynchronous task to load all {@link RecentWebAddress}es information
         * from the database and show search options dialog.
         */
        class LoadRecentWebAddressesTaskAndShowSearchOptions extends AbstractLoadRecentWebAddressesTask {

            /**
             * The maximum recent web addresses count which can be shown in the
             * popup menu
             */
        	static final int MAXIMUM_RECENT_WEB_ADDRESSES_COUNT = 20;
        	
            public LoadRecentWebAddressesTaskAndShowSearchOptions() {
                super(null, mSettings.getUrl());
            }

            @Override
            protected void onSuccessPostExecute() {
                SearchOptionsFragment fragment = new SearchOptionsFragment();
                fragment.setData(
                        mProductSku,
                        mSearchQuery,
                        mSearchOriginalQuery,
                        // only no more than MAXIMUM_RECENT_WEB_ADDRESSES_COUNT
                        // recent web addresses are allowed
                        recentWebAddresses.subList(
                                0,
                                Math.min(MAXIMUM_RECENT_WEB_ADDRESSES_COUNT,
                                        recentWebAddresses.size())),
                        new OnRecentWebAddressClickedListener() {

                            @Override
                            public void onRecentWebAddressClicked(String query,
                                    RecentWebAddress address) {
                                setSearchDomain(address == null ? null : address.getDomain());
                                mSearchQuery = query;
                                refreshWebView();
                            }
                        });
                fragment.show(getActivity().getSupportFragmentManager(), fragment.getClass()
                        .getSimpleName());
            }

        }
    }
}
