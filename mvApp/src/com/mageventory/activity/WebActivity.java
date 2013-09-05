
package com.mageventory.activity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.fragment.base.BaseFragment;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.SimpleAsyncTask;

public class WebActivity extends BaseFragmentActivity implements MageventoryConstants {
    private static final String TAG = WebActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new WebUiFragment()).commit();
        }
    }

    WebUiFragment getContentFragment() {
        return (WebUiFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
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

    public static class WebUiFragment extends BaseFragment {
        WebView mWebView;
        Button mParseButton;
        Button mCancelButton;
        View mParsingImageUrlsStatusLine;
        String mProductSku;
        String mProductName;
        String mLastLoadedPage;
        String mLastLoadedUrl;
        ProgressBar mPageLoadingProgress;
        ParseUrlsTask mParseUrlsTask;

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
            mParseButton = (Button) view.findViewById(R.id.parseButton);
            mParseButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    parseUrls();
                }
            });
            mCancelButton = (Button) view.findViewById(R.id.cancelButton);
            mCancelButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    getActivity().finish();
                }
            });
            mParsingImageUrlsStatusLine = view.findViewById(R.id.parsingImageUrlsStatusLine);
            reinitFromIntent(getActivity().getIntent());
        }

        class MyJavaScriptInterface {
            public void processHTML(String html) {
                mLastLoadedPage = html;
                rememberLastLoadedUrl();
            }
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
                    // check secret prefix
                    if (cmsg.message().startsWith("MAGIC")) {
                        String msg = cmsg.message().substring(5); // strip off
                        /* process HTML */
                        mLastLoadedPage = msg;
                        rememberLastLoadedUrl();
                        return true;
                    }

                    return false;
                }

            });
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
        }

        public void refreshWebView() {
            try {
                mWebView.loadUrl("https://www.google.com/search?q="
                        + URLEncoder.encode(mProductName, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                GuiUtils.error(TAG, e);
            }
        }

        private void updateParsingStatus() {
            if (isResumed()) {
                mParsingImageUrlsStatusLine.setVisibility(mParseUrlsTask != null ? View.VISIBLE
                        : View.GONE);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            updateParsingStatus();
        }

        private void parseUrls() {
            if (mLastLoadedPage == null) {
                GuiUtils.alert(R.string.page_not_yet_loaded);
            } else {
                if (mParseUrlsTask == null) {
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
                mWebView.goBack();
                return true;
            } else {
                return false;
            }
        }

        private class ParseUrlsTask extends SimpleAsyncTask {
            String mContent;
            String mUrl;
            String[] mUrls;

            public ParseUrlsTask(String content, String url) {
                super(null);
                this.mContent = content;
                this.mUrl = url;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                updateParsingStatus();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    mUrls = ImageUtils.extractImageUrls(mContent);
                    int p = mUrl.indexOf("/", 9);
                    CommonUtils.debug(TAG, "ParseUrlsTask.doInBackground: url %1$s", mUrl);
                    String domain = p == -1 ? mUrl : mUrl.substring(0, p);
                    CommonUtils.debug(TAG, "ParseUrlsTask.doInBackground: domain %1$s", domain);
                    p = mUrl.indexOf("?");
                    String domainWithPath = p == -1 ? mUrl : mUrl.substring(0, p);
                    p = domainWithPath.lastIndexOf("/");
                    domainWithPath = p == -1 ? domainWithPath : domainWithPath.substring(0, p);
                    CommonUtils.debug(TAG, "ParseUrlsTask.doInBackground: domainWithPath %1$s",
                            domainWithPath);
                    for (int i = 0; i < mUrls.length; i++) {
                        String url = mUrls[i];
                        String urlLc = url.toLowerCase();
                        if (!(urlLc.startsWith("http://") || urlLc.startsWith("https://"))) {
                            if (url.startsWith("/")) {
                                url = domain + url;
                            } else {
                                url = domainWithPath + "/" + url;
                            }
                            mUrls[i] = url;
                        }
                    }
                    return !isCancelled();
                } catch (Exception ex) {
                    GuiUtils.error(TAG, ex);
                }
                return false;
            }

            @Override
            protected void onSuccessPostExecute() {
                try {
                    if (isActivityAlive()) {
                        if (mUrls == null || mUrls.length == 0) {
                            GuiUtils.alert(R.string.no_urls_found);
                        } else {
                            Intent intent = new Intent(getActivity(), LibraryActivity.class);
                            intent.putExtra(getString(R.string.ekey_product_sku), mProductSku);
                            intent.putExtra(LibraryActivity.IMAGE_URLS, mUrls);
                            startActivity(intent);
                        }
                    }
                } finally {
                    taskFinished();
                }
            }

            @Override
            protected void onFailedPostExecute() {
                super.onFailedPostExecute();
                taskFinished();
            }

            private void taskFinished() {
                mParseUrlsTask = null;
                updateParsingStatus();
            }
        }
    }
}
