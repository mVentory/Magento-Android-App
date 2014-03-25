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
import android.webkit.JavascriptInterface;
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
import com.mageventory.util.ScanUtils;
import com.mageventory.util.SimpleAsyncTask;

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

    public static class WebUiFragment extends BaseFragment {

        public static final int SCAN_QR_CODE = 0;

        WebView mWebView;
        Button mParseButton;
        Button mScanButton;
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
            mScanButton = (Button) view.findViewById(R.id.scanButton);
            mScanButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    scanAddress();
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
            @JavascriptInterface
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
            googleIt(mProductName);
        }

        private void googleIt(String query) {
            try {
                mWebView.loadUrl("https://www.google.com/search?q="
                        + URLEncoder.encode(query, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                GuiUtils.noAlertError(TAG, e);
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

        private void scanAddress() {
            ScanUtils
                    .startScanActivityForResult(getActivity(), SCAN_QR_CODE, R.string.scan_address);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == SCAN_QR_CODE) {
                if (resultCode == RESULT_OK) {
                    String contents = ScanUtils.getSanitizedScanResult(data);
                    if (contents != null) {
                        String lcContents = contents.toLowerCase();
                        if (lcContents.startsWith("http://") || lcContents.startsWith("https://")) {
                            mWebView.loadUrl(contents);
                        } else {
                            googleIt(contents);
                        }
                    }
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
                    mUrls = ImageUtils.extractImageUrls(mContent, mUrl);
                    return !isCancelled();
                } catch (Exception ex) {
                    GuiUtils.noAlertError(TAG, ex);
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
