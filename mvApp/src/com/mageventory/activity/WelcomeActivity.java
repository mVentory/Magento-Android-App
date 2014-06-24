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

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.fragment.base.BaseFragment;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.util.EventBusUtils.GeneralBroadcastEventHandler;

/**
 * Welcom screen activity
 * 
 * @author Eugene Popovich
 */
public class WelcomeActivity extends BaseFragmentActivity implements MageventoryConstants,
        GeneralBroadcastEventHandler {
    static final String TAG = WelcomeActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new WelcomeUiFragment()).commit();
        }
        EventBusUtils.registerOnGeneralEventBroadcastReceiver(TAG, this, this);
    }
    
    @Override
    public void onGeneralBroadcastEvent(EventType eventType, Intent extra) {
        switch (eventType) {
            case PROFILE_CONFIGURED:
                if (isActivityAlive()) {
                    finish();
                }
                break;
            default:
                break;
        }
    }

    public static class WelcomeUiFragment extends BaseFragment {

        WebView mWebView;
        Button mAddProfileButton;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            View view = inflater.inflate(R.layout.welcome, container, false);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mWebView = (WebView) view.findViewById(R.id.webView);
            initWebView();
            mAddProfileButton = (Button) view.findViewById(R.id.addProfileButton);
            mAddProfileButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    addProfile();
                }

            });
            refreshWebView();
        }

        private void addProfile() {
            Intent intent = new Intent(getActivity(), ConfigServerActivity.class);
            intent.putExtra(ConfigServerActivity.ADD_PROFILE_EXTRA, true);
            startActivity(intent);
            getActivity().finish();
        }

        private void initWebView() {
            WebSettings webSettings = mWebView.getSettings();
            // webSettings.setBuiltInZoomControls(true);
            webSettings.setJavaScriptEnabled(true);

        }


        public void refreshWebView() {
            mWebView.loadUrl("file:///android_asset/html/welcome.html");
        }

    }

}
