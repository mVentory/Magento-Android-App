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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttribute.ContentType;
import com.mageventory.model.CustomAttributeSimple;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.model.Product;
import com.mageventory.model.util.RecentProductsUtils;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.TrackerUtils;
import com.mventory.R;

/**
 * The activity to handle share with the mVentory application action. For a now
 * it supports text/plain intent-filter and searches for the youtube URLs in the
 * intent text extra
 * 
 * @author Eugene Popovich
 */
public class ShareActivity extends BaseFragmentActivity implements MageventoryConstants {
    /**
     * Pattern used to extract youtube URLs from the shared text
     */
    public final static Pattern YOUTUBE_LINK_PATTERN = Pattern.compile("^.*"
            + ImageUtils.PROTO_PREFIX + "youtu.be\\/(\\w+).*$", Pattern.CASE_INSENSITIVE);

    /**
     * Instance of the last started {@link LoadAndCheckRecentProductTask}
     */
    LoadAndCheckRecentProductTask mLoadAndCheckRecentProductTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings settings = new Settings(getApplicationContext());
        // if there are no assigned settings navigate to the welcome activity
        if (WelcomeActivity.startWelcomeActivityIfNecessary(ShareActivity.this, settings)) {
            return;
        }
        setContentView(R.layout.scan_activity);

        findViewById(R.id.progressStatus).setVisibility(View.VISIBLE);
        TextView progressMessage = (TextView) findViewById(R.id.progressMesage);
        progressMessage.setText(R.string.share_progress_status_message);
        findViewById(R.id.cancelButton).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                cancelActiveTask();
                finish();
            }
        });
        reinitFromIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // the new intent data received. Reinitialize
        reinitFromIntent();
    }

    /**
     * Reinitialize activity from the passed intent
     */
    void reinitFromIntent() {
        // cancel active task first if present
        cancelActiveTask();
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            // if that is sending intent and type is present
            if ("text/plain".equals(type)) {
                // flag to control whether the intent is valid and processed
                // properly
                boolean processed = false;
                String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (!TextUtils.isEmpty(text)) {
                    // if text is present in the intent
                    Matcher m = YOUTUBE_LINK_PATTERN.matcher(text);
                    if (m.find()) {
                        // if youtube link is present in the text
                        String videoId = m.group(1);
                        // start the last product check task
                        mLoadAndCheckRecentProductTask = new LoadAndCheckRecentProductTask(videoId);
                        mLoadAndCheckRecentProductTask.execute();
                        // set the flag that the intent is processed
                        processed = true;
                    }
                }
                if (!processed) {
                    // if intent is invalid and was not processed
                    showMessageWithActivityCloseAction(R.string.share_no_youtube_video_information);
                }
            }
        }
    }

    /**
     * Cancel the active checking product task if exists
     */
    public void cancelActiveTask() {
        if (mLoadAndCheckRecentProductTask != null) {
            // if there is a previously started task
            mLoadAndCheckRecentProductTask.cancel(true);
        }
    }

    /**
     * Show the message dialog with the specified message. Self close activity
     * in case user closed the dialog
     * 
     * @param messageId
     */
    void showMessageWithActivityCloseAction(int messageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(messageId);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // cancel active task if exists
        cancelActiveTask();
    }

    /**
     * Launch the product edit activity updated attributes information
     * 
     * @param productSku the product SKU
     * @param updatedTextAttributes the updated text attributes information
     */
    public void launchProductEditActivity(String productSku,
            CustomAttributeSimple... updatedTextAttributes) {
        final Intent i = new Intent(ShareActivity.this, ProductEditActivity.class);
        // pass the product SKU extra
        i.putExtra(getString(R.string.ekey_product_sku), productSku);
        // pass the updated text attribute information to the
        // intent so the ProductEdit activity may handle it
        i.putParcelableArrayListExtra(ProductEditActivity.EXTRA_PREDEFINED_ATTRIBUTES,
                new ArrayList<CustomAttributeSimple>(Arrays.asList(updatedTextAttributes)));
        startActivity(i);
    }

    /**
     * Asynchronous task to load the recent product details and check whether it
     * has video attribute. Open the product edit activity with the specified
     * video attribute value in case it does
     */
    class LoadAndCheckRecentProductTask extends SimpleAsyncTask {
        /**
         * Tag used for logging
         */
        final String TAG = LoadAndCheckRecentProductTask.class.getSimpleName();
        /**
         * The Youtube video ID
         */
        String mVideoId;
        /**
         * The field to store recent product SKU if found
         */
        String mSku;
        /**
         * The field to store recent product video attribute information if
         * found
         */
        CustomAttribute mVideoAttribute;
        /**
         * The settings snapshot
         */
        private SettingsSnapshot mSettingsSnapshot;

        /**
         * @param videoId the Youtube video ID
         */
        public LoadAndCheckRecentProductTask(String videoId) {
            super(null);
            mVideoId = videoId;
            mSettingsSnapshot = new SettingsSnapshot(ShareActivity.this);
        }

        @Override
        protected void onSuccessPostExecute() {
            if (!isActivityAlive()) {
                return;
            }
            // create the simple attribute holder from the found attribute
            CustomAttributeSimple videoAttribute = CustomAttributeSimple.from(mVideoAttribute);
            // set the value to pass into edit activity
            videoAttribute.setSelectedValue(mVideoId);
            launchProductEditActivity(mSku, videoAttribute);
            finish();
        }

        @Override
        protected void onFailedPostExecute() {
            super.onFailedPostExecute();
            if (!isActivityAlive()) {
                return;
            }
            showMessageWithActivityCloseAction(R.string.share_couldnt_find_suitable_product);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                long start = System.currentTimeMillis();
                RecentProductsUtils.iterateThroughRecentProducts(mSettingsSnapshot,
                        new RecentProductsUtils.IterateThroughRecentProductsParams() {

                            @Override
                            public boolean processRecentProduct(Product product) {
                                mSku = product.getSku();
                                String attributeSet = Integer.toString(product.getAttributeSetId());
                                if (JobCacheManager.attributeListExist(attributeSet,
                                        mSettingsSnapshot.getUrl())) {
                                    // initialize existing product attribute
                                    // information
                                    List<Map<String, Object>> attributeList = JobCacheManager
                                            .restoreAttributeList(attributeSet,
                                                    mSettingsSnapshot.getUrl());
                                    if (attributeList != null) {
                                        for (Map<String, Object> attribute : attributeList) {
                                            if (isCancelled()) {
                                                return false;
                                            }
                                            CustomAttribute customAttribute = CustomAttributesList
                                                    .createCustomAttribute(attribute, null);
                                            if (customAttribute
                                                    .hasContentType(ContentType.YOUTUBE_VIDEO_ID)) {
                                                mVideoAttribute = customAttribute;
                                                break;
                                            }
                                        }
                                    }
                                }
                                return false;
                            }

                            @Override
                            public boolean isCancelled() {
                                return LoadAndCheckRecentProductTask.this.isCancelled();
                            }
                        });
                TrackerUtils.trackDataLoadTiming(System.currentTimeMillis() - start, TAG
                        + ".doInBackground", TAG);
                return !isCancelled() && mSku != null && mVideoAttribute != null;
            } catch (Exception ex) {
                CommonUtils.error(TAG, ex);
            }
            return false;
        }

    }
}
