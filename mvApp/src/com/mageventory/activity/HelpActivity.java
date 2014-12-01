package com.mageventory.activity;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.mageventory.R;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.fragment.base.BaseFragment;

/**
 * Simple activity with the web view to display help pages
 * 
 * @author Eugene Popovich
 */
public class HelpActivity extends BaseFragmentActivity {
    /**
     * Tag used for logging
     */
    private static final String TAG = HelpActivity.class.getSimpleName();

    /**
     * The key for the URL intent extra
     */
    public static final String EXTRA_URL = "URL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new HelpUiFragment()).commit();
        }
    }

    /**
     * Get the current content fragment
     * 
     * @return
     */
    HelpUiFragment getContentFragment() {
        return (HelpUiFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // pass intent to the content fragment
        getContentFragment().reinitFromIntent(intent);
    }

    @Override
    public void onBackPressed() {
        HelpUiFragment fragment = getContentFragment();
        boolean proceed = true;
        proceed &= !fragment.isBackKeyOverrode();
        if (proceed) {
            // if content fragment doesn't override back key
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            // if refresh is pressed
            getContentFragment().reinitFromIntent(getIntent());
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Launch the Help activity for the URL
     * 
     * @param url the URL to the help page
     * @param activity activity from where the help is launched
     */
    public static void launchHelp(String url, Activity activity) {
        Intent intent = new Intent(activity, HelpActivity.class);
        // tell the task manager reorder help activity to front of the task if
        // it was opened before
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(EXTRA_URL, url);
        activity.startActivity(intent);
    }

    public static class HelpUiFragment extends BaseFragment {

        /**
         * Duration of the animation in ms
         */
        static final int ANIMATION_DURATION = 500;

        /**
         * The web view to display help pages
         */
        WebView mWebView;
        /**
         * Help pages loading progress indicator
         */
        ProgressBar mPageLoadingProgress;

        /**
         * The boolean flag whether the history should be cleared on page
         * loaded. Used to workaround issue when history is not cleared in some
         * cases
         */
        AtomicBoolean mClearHistory = new AtomicBoolean(false);

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            View view = inflater.inflate(R.layout.help, container, false);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mWebView = (WebView) view.findViewById(R.id.webView);
            mPageLoadingProgress = (ProgressBar) view.findViewById(R.id.pageLoadingProgress);
            initWebView();
            reinitFromIntent(getActivity().getIntent());
        }

        /**
         * Initialize the web view
         */
        private void initWebView() {
            WebSettings webSettings = mWebView.getSettings();
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
            webSettings.setJavaScriptEnabled(true);
            webSettings.setSupportZoom(true);

            mWebView.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageFinished(WebView view, String url) {
                    if (mClearHistory.get()) {
                        // if clear history is scheduled
                        mClearHistory.set(false);
                        mWebView.clearHistory();
                    }
                    super.onPageFinished(view, url);
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
            });
        }

        public void reinitFromIntent(Intent intent) {
            String url = intent.getStringExtra(EXTRA_URL);
            mWebView.loadUrl(url);
            mWebView.clearHistory();
            // schedule another history cleaning such as first call doesn't work
            // as expected
            mClearHistory.set(true);
        }

        /**
         * @return true if we have custom behavior of back button pressed
         */
        public boolean isBackKeyOverrode() {
            if (mWebView.canGoBack()) {
                // if the web view can be navigated back
                mWebView.goBack();
                return true;
            } else {
                return false;
            }
        }
    }
}
