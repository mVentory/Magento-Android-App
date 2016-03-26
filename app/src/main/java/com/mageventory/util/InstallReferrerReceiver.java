package com.mageventory.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.mageventory.activity.ConfigServerActivity;
import com.mventory.R;

/**
 * A simple Broadcast Receiver to receive an INSTALL_REFERRER intent and call
 * ConfigServerActivity with the URI data
 */
public class InstallReferrerReceiver extends BroadcastReceiver {

    private static final String TAG = InstallReferrerReceiver.class.getSimpleName();
    private static final String EXTRA_REFERRER = "referrer";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Pass the intent parameter to the ConfigServerActivity .
        CommonUtils.debug(TAG, "onReceive: started");
        if (intent.hasExtra(EXTRA_REFERRER)) {
            String referer = intent.getStringExtra(EXTRA_REFERRER);
            CommonUtils.debug(TAG, "onReceive: referrer \"%1$s\" is present. Starting activity", referer);
            referer = Uri.decode(referer);
            Intent intent2 = new Intent(context, ConfigServerActivity.class);
            String mventorySchema = CommonUtils
                    .getStringResource(R.string.settings_content_uri_path_schema);
            if (ImageUtils.isUrl(referer)) {
                referer = mventorySchema + referer.substring(0, referer.indexOf(":"));
            } else if(referer.matches("(?i).*" + mventorySchema + "s?:\\/\\/" + ".*")){
                // do nothing
            } else {
                referer = mventorySchema + "://" + referer;
            }
            intent2.setData(Uri.parse(referer));
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent2);
        } else {
            CommonUtils.debug(TAG, "onReceive: referrer is missing");
        }
    }
}