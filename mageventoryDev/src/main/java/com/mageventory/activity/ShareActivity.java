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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.fragment.ProductListDialogFragment;
import com.mageventory.fragment.base.BaseFragment;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobControlInterface;
import com.mageventory.jobprocessor.util.UploadImageJobUtils;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttribute.ContentType;
import com.mageventory.model.CustomAttributeSimple;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.AbstractAddNewImagesTask;
import com.mageventory.tasks.AbstractLoadProductTask;
import com.mageventory.util.FileUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mventory.R;

/**
 * The activity to handle share with the mVentory application action. For a now
 * it supports text/plain intent-filter and searches for the youtube URLs in the
 * intent text extra. Also supports images share action.
 * 
 * @author Eugene Popovich
 */
public class ShareActivity extends BaseFragmentActivity implements MageventoryConstants {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Settings settings = new Settings(getApplicationContext());
        // if there are no assigned settings navigate to the welcome
        // activity
        if (WelcomeActivity.startWelcomeActivityIfNecessary(ShareActivity.this, settings)) {
            return;
        }
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new UiFragment()).commit();
        }
    }

    /**
     * Get the reference to the current content fragment
     * 
     * @return
     */
    UiFragment getContentFragment() {
        return (UiFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // update activity intent for future references
        setIntent(intent);
        // pass event to the content fragment
        getContentFragment().reinitFromIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            // do nothing
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * The content fragment for the share activity
     */
    public static class UiFragment extends BaseFragment {
        /**
         * Pattern used to extract youtube URLs from the shared text
         */
        public final static Pattern YOUTUBE_LINK_PATTERN = Pattern.compile("^.*"
                + ImageUtils.PROTO_PREFIX + "youtu.be\\/(\\w+).*$", Pattern.CASE_INSENSITIVE);
        /**
         * The request code used for the result returned from the
         * {@link ProductListDialogFragment}
         */
        private static final int SELECT_PRODUCT = 1001;

        /**
         * The possible supported share action types
         */
        enum ShareType {
            /**
             * Youtube video URL share action type
             */
            VIDEO,
            /**
             * Single image share action type
             */
            IMAGE,
            /**
             * Multiple images share action type
             */
            IMAGE_MULTIPLE
        }
        /**
         * Instance of the last started {@link LoadProductAndCheckForVideoAttributesTask}
         */
        LoadProductAndCheckForVideoAttributesTask mLoadProductAndCheckForVideoAttributesTask;
        /**
         * Instance of the last started {@link AddNewImagesTask}
         */
        AddNewImagesTask mAddNewImagesTask;
        /**
         * The currently handling share action type
         */
        ShareType mShareType;
        /**
         * The video ID data received from the share action intent. Used only
         * for {@link ShareType#VIDEO}
         */
        String mVideoId;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            View view = inflater.inflate(R.layout.scan_activity, container, false);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            view.findViewById(R.id.progressStatus).setVisibility(View.VISIBLE);
            TextView progressMessage = (TextView) view.findViewById(R.id.progressMesage);
            progressMessage.setText(R.string.share_progress_status_message);
            view.findViewById(R.id.cancelButton).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    cancelActiveTask();
                    getActivity().finish();
                }
            });
            reinitFromIntent(getActivity().getIntent());
        }

        /**
         * Reinitialize activity from the passed intent
         */
        void reinitFromIntent(Intent intent) {
            // cancel active task first if present
            cancelActiveTask();
            // Get intent, action and MIME type
            String action = intent.getAction();
            String type = intent.getType();
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                // if that is sending intent and type is present
                if ("text/plain".equals(type)) {
                    // if a text shared with the app
                    mShareType = ShareType.VIDEO;
                    // flag to control whether the intent is valid and processed
                    // properly
                    boolean processed = false;
                    String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                    if (!TextUtils.isEmpty(text)) {
                        // if text is present in the intent
                        Matcher m = YOUTUBE_LINK_PATTERN.matcher(text);
                        if (m.find()) {
                            // if youtube link is present in the text
                            mVideoId = m.group(1);
                            // let the user to choose a product to pass the
                            // video data to
                            launchProductSelection();
                            // set the flag that the intent is processed
                            processed = true;
                        }
                    }
                    if (!processed) {
                        // if intent is invalid and was not processed
                        showMessageWithActivityCloseAction(R.string.share_no_youtube_video_information);
                    }
                } else if (type.startsWith("image/")) {
                    // if an image is shared with the app
                    mShareType = ShareType.IMAGE;
                    if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                        // if content stream data is present in the intent

                        // let the user to choose a product to pass the
                        // image data to
                        launchProductSelection();
                    } else {
                        // if intent contains invalid data
                        showMessageWithActivityCloseAction(R.string.share_no_image_information);
                    }
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
                // if multiple data is sent to the app and the data type is not
                // empty
                if (type.startsWith("image/")) {
                    // if multiple images are shared with the app
                    mShareType = ShareType.IMAGE_MULTIPLE;
                    if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                        // if content stream data is present in the intent

                        // let the user to choose a product to pass the
                        // images data to
                        launchProductSelection();
                    } else {
                        // if intent contains invalid data
                        showMessageWithActivityCloseAction(R.string.share_no_image_information);
                    }
                } else {
                    // if type is not supported
                    showMessageWithActivityCloseAction(R.string.share_no_image_information);
                }
            }
        }

        /**
         * Launch the product selection dialog so user can select the proper
         * product to share data with
         */
        void launchProductSelection() {
            ProductListDialogFragment fragment = new ProductListDialogFragment();
            Bundle args = new Bundle();

            // pass the new dialog title as argument
            int dialogTitle = 0;
            switch (mShareType) {
                case IMAGE:
                    dialogTitle = R.string.share_add_image_to_dialog_title;
                    break;
                case IMAGE_MULTIPLE:
                    dialogTitle = R.string.share_add_images_to_dialog_title;
                    break;
                case VIDEO:
                    dialogTitle = R.string.share_add_video_to_dialog_title;
                    break;
                default:
                    break;

            }
            args.putString(ProductListDialogFragment.EXTRA_DIALOG_TITLE, getString(dialogTitle));
            fragment.setArguments(args);

            // set the target fragment data so onActivityResult can be handled
            // properly
            fragment.setTargetFragment(this, SELECT_PRODUCT);
            fragment.show(getFragmentManager(), fragment.getClass().getSimpleName());
        }

        /**
         * Cancel the active checking product task if exists
         */
        public void cancelActiveTask() {
            if (mLoadProductAndCheckForVideoAttributesTask != null) {
                // if there is a previously started video handling task
                mLoadProductAndCheckForVideoAttributesTask.cancel(true);
            }
            if (mAddNewImagesTask != null) {
                // if there is a previously started images handling task
                mAddNewImagesTask.cancel(true);
            }
        }

        /**
         * Show the message dialog with the specified message. Self close
         * activity in case user closed the dialog
         * 
         * @param messageId
         */
        void showMessageWithActivityCloseAction(int messageId) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(messageId);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().finish();
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    getActivity().finish();
                }
            });
            builder.show();
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            cancelActiveTask();
        }

        /**
         * Launch the product edit activity with the updated attributes
         * information
         * 
         * @param productSku the product SKU
         * @param updatedTextAttributes the updated text attributes information
         */
        public void launchProductEditActivity(String productSku,
                CustomAttributeSimple... updatedTextAttributes) {
            final Intent i = new Intent(getActivity(), ProductEditActivity.class);
            // pass the product SKU extra
            i.putExtra(getString(R.string.ekey_product_sku), productSku);
            // pass the updated text attribute information to the
            // intent so the ProductEdit activity may handle it
            i.putParcelableArrayListExtra(ProductEditActivity.EXTRA_PREDEFINED_ATTRIBUTES,
                    new ArrayList<CustomAttributeSimple>(Arrays.asList(updatedTextAttributes)));
            startActivity(i);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            switch (requestCode) {
                case SELECT_PRODUCT:
                    // handling product selection dialog activity result
                    if (resultCode == Activity.RESULT_OK) {
                        String sku = data.getStringExtra(ProductListDialogFragment.EXTRA_SKU);
                        onProductSelected(sku);
                    } else {
                        // finish activity in case user didn't select the
                        // product
                        getActivity().finish();
                    }
                default:
                    break;
            }
        }

        /**
         * Product SKU selected event handler
         * 
         * @param sku the selected product SKU
         */
        void onProductSelected(String sku) {
            switch (mShareType) {
                case IMAGE:
                    Uri contentUri = getActivity().getIntent().getParcelableExtra(
                            Intent.EXTRA_STREAM);
                    mAddNewImagesTask = new AddNewImagesTask(contentUri.toString(), sku);
                    mAddNewImagesTask.execute();
                    break;
                case IMAGE_MULTIPLE:
                    // get all shared images content URI paths
                    ArrayList<Parcelable> list =
                            getActivity().getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    String[] filePaths = new String[list.size()];
                    for (int i = 0; i < filePaths.length; i++) {
                        filePaths[i] = list.get(i).toString();
                    }

                    mAddNewImagesTask = new AddNewImagesTask(filePaths, sku);
                    mAddNewImagesTask.execute();
                    break;
                case VIDEO:
                    mLoadProductAndCheckForVideoAttributesTask = new LoadProductAndCheckForVideoAttributesTask(
                            sku, mVideoId);
                    mLoadProductAndCheckForVideoAttributesTask.execute();
                    break;
                default:
                    break;

            }
        }

        /**
         * Implementation of {@link AbstractAddNewImagesTask}. Add the upload
         * images job for the product
         */
        private class AddNewImagesTask extends AbstractAddNewImagesTask {

            /**
             * Add new image upload job for the specified file path and product
             * SKU
             * 
             * @param filePath the path to the image file which should be
             *            uploaded
             * @param sku the product SKU image should be added to
             */
            public AddNewImagesTask(String filePath, String sku) {
                this(new String[] {
                    filePath
                }, sku);
            }

            /**
             * Add new image upload jobs for the specified file paths and
             * product SKU
             * 
             * @param filePaths the paths to the images file which should be
             *            uploaded
             * @param sku the product SKU image should be added to
             */
            public AddNewImagesTask(String[] filePaths, String sku) {
                super(filePaths, sku, false, new JobControlInterface(getActivity()),
                        new SettingsSnapshot(getActivity()), null);
            }

            @Override
            protected String getTargetFileName(File source) {
                String fileName = super.getTargetFileName(source);
                String extension = FileUtils.getExtension(fileName);
                return UploadImageJobUtils.getGeneratedUploadImageFileName(extension);
            }

            @Override
            protected void onSuccessPostExecute() {
                if (isCancelled()) {
                    return;
                }
                if (isActivityAlive()) {
                    // if activity is still alive launch the details for the
                    // product images was added to
                    ProductDetailsActivity.launchProductDetails(getProductSku(), getActivity());
                    getActivity().finish();
                }
            }
        }

        /**
         * Asynchronous task to load the product details and check whether it
         * has video attributes. Open the product edit activity with the
         * specified video attributes value in case it does
         */
        class LoadProductAndCheckForVideoAttributesTask extends AbstractLoadProductTask {
            /**
             * Tag used for logging
             */
            final String TAG = LoadProductAndCheckForVideoAttributesTask.class.getSimpleName();
            /**
             * Flag indicating whether the attribute set should be loaded in the
             * loadGeneral method
             */
            boolean mLoadAttributesList = false;
            /**
             * The Youtube video ID
             */
            String mVideoId;
            /**
             * The field to store product video attributes information if found
             */
            List<CustomAttribute> mVideoAttributes = new ArrayList<CustomAttribute>();

            /**
             * @param sku the product SKU the video information should be added
             *            to
             * @param videoId the Youtube video ID
             */
            public LoadProductAndCheckForVideoAttributesTask(String sku, String videoId) {
                super(sku, new SettingsSnapshot(getActivity()), null);
                mVideoId = videoId;
            }
            @Override
            public void extraLoadAfterProductIsLoaded() {
                super.extraLoadAfterProductIsLoaded();

                boolean loadResult = true;
                // to get the product attribute set name we need to load
                // attribute sets information
                if (!JobCacheManager.attributeListExist(
                                Integer.toString(getProduct().getAttributeSetId()),
                                settingsSnapshot.getUrl())) {
                    // attribute set or attribute list is not loaded, request
                    // load
                    // it from the server.
                    mLoadAttributesList = true;
                    loadResult = loadGeneral();
                }
                if (isCancelled()) {
                    return;
                }
                if (loadResult) {
                    // if attribute list data exists

                    // initialize existing product attribute information
                    List<Map<String, Object>> attributeList = JobCacheManager.restoreAttributeList(
                            Integer.toString(getProduct().getAttributeSetId()),
                            settingsSnapshot.getUrl());
                    if (attributeList != null) {
                        // iterate through custom attributes list and find all
                        // the attributes with the video content type
                        for (Map<String, Object> attribute : attributeList) {
                            if (isCancelled()) {
                                return;
                            }
                            CustomAttribute customAttribute = CustomAttributesList
                                    .createCustomAttribute(attribute, null);
                            if (customAttribute.hasContentType(ContentType.YOUTUBE_VIDEO_ID)) {
                                // if attribute has video content type store it
                                // in the list of video attributes
                                mVideoAttributes.add(customAttribute);
                            }
                        }
                    }
                }
            }

            @Override
            protected int requestLoadResource() {
                if (mLoadAttributesList) {
                    // the attribute list load is requested
                    return resHelper.loadResource(MyApplication.getContext(),
                            RES_CATALOG_PRODUCT_ATTRIBUTES, settingsSnapshot);
                } else {
                    return super.requestLoadResource();
                }
            }

            @Override
            protected void onFailedPostExecute() {
                super.onFailedPostExecute();
                if (isCancelled()) {
                    return;
                }
                if (isNotExists()) {
                    // if product doesn't exist
                    GuiUtils.alert(R.string.product_not_found2, getOriginalSku());
                } else {
                    GuiUtils.alert(R.string.errorGeneral);
                }
                getActivity().finish();
            }

            @Override
            protected void onSuccessPostExecute() {
                if (!isActivityAlive()) {
                    return;
                }
                if (mVideoAttributes.isEmpty()) {
                    // if no video attributes were found
                    showMessageWithActivityCloseAction(R.string.share_couldnt_find_video_attributes);
                } else {
                    CustomAttributeSimple[] simpleVideoAttributes = new CustomAttributeSimple[mVideoAttributes
                            .size()];
                    for (int i = 0; i < simpleVideoAttributes.length; i++) {
                        // create the simple attribute holder from the found
                        // attribute
                        CustomAttributeSimple videoAttribute = CustomAttributeSimple
                                .from(mVideoAttributes.get(i));
                        // set the value to pass into edit activity
                        videoAttribute.setSelectedValue(mVideoId);
                        simpleVideoAttributes[i] = videoAttribute;
                    }
                    launchProductEditActivity(getSku(), simpleVideoAttributes);
                    getActivity().finish();
                }
            }

        }
    }
}
