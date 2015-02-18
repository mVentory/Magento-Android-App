
package com.mageventory.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.support.v4.app.ShareCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.bitmapfun.util.ImageWorker;
import com.mageventory.components.ImagePreviewLayout;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.Product;
import com.mageventory.model.Product.imageInfo;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.LoadImagesForProduct;
import com.mageventory.util.run.CallableWithParameterAndResult;
import com.mventory.R;

/**
 * Utilities to share product details to other applications
 * 
 * @author Eugene Popovich
 */
public class ShareUtils implements MageventoryConstants {
    public static enum ShareType {
        /**
         * Facebook share type
         */
        FACEBOOK(0),
        /**
         * Twitter share type
         */
        TWITTER(1),
        /**
         * Pinterest share type
         */
        PINTEREST(2),
        /*
         * Other share type
         */
        OTHER(3);
        /**
         * The share type id, index of the item in the
         * {@link R.array#share_items} array
         */
        int mId;

        /**
         * @param id The item index in the {@link R.array#share_items} array
         */
        ShareType(int id) {
            mId = id;
        }

        /**
         * Get the share type for the item index of the
         * {@link R.array#share_items} array
         * 
         * @param id The item index in the {@link R.array#share_items} array
         * @return corresponding share type if found, null otherwise
         */
        public static ShareType getForId(int id) {
            ShareType result = null;
            for (ShareType shareType : values()) {
                if (shareType.mId == id) {
                    result = shareType;
                    break;
                }
            }
            return result;
        }
    }

    /**
     * Share the product details with images to the any external application
     * which supports such action
     * 
     * @param product the product to share
     * @param customAttributes the product custom attributes information
     * @param requiredImageWidth the required image width for the share
     *            operation
     * @param shareType the share type
     * @param settings the application settings
     * @param activity the activity which performs share action
     */
    public static void shareProduct(Product product,
            Map<String, CustomAttribute> customAttributes, final int requiredImageWidth,
            ShareType shareType, Settings settings, final BaseFragmentActivity activity) {
        StringBuilder textMessage = generateTextMessage(customAttributes);
        String url = settings.getUrl() + "/" + product.getUrlPath();

        // callable to get the required image url for the specified size from
        // the imageInfo
        CallableWithParameterAndResult<Product.imageInfo, String> getImageUrlCallable = new CallableWithParameterAndResult<Product.imageInfo, String>() {
            // the settings snapshot
            SettingsSnapshot mSettingsSnapshot = new SettingsSnapshot(activity);

            @Override
            public String call(imageInfo ii) {
                // return the URL for the resized image
                return ImagePreviewLayout.getUrlForResizedImage(ii.getImgURL(), mSettingsSnapshot,
                        requiredImageWidth);
            }
        };
        switch (shareType) {
            case FACEBOOK: {
                shareProductAtFacebook(activity, url);
                break;
            }
            case TWITTER: {
                shareProductAtTwitter(textMessage, url, activity);
                break;
            }
            case PINTEREST: {
                shareProductAtPinterest(product, textMessage, url, getImageUrlCallable, activity);
                break;
            }
            case OTHER: {
                shareProductOther(product, settings, activity, textMessage, url, getImageUrlCallable);
                break;
            }
            default:
                break;
        }
    }

    /**
     * Generate text message for the share operations
     * 
     * @param customAttributes the product custom attributes information
     * @return
     */
    private static StringBuilder generateTextMessage(Map<String, CustomAttribute> customAttributes) {
        // Generate the text information
        StringBuilder plainText = new StringBuilder();
        // the special attributes codes
        String[] attributeCodes = new String[] {
                MAGEKEY_PRODUCT_NAME, MAGEKEY_PRODUCT_SHORT_DESCRIPTION,
                MAGEKEY_PRODUCT_DESCRIPTION,
        };
        String extra = null;
        for (int i = 0; i < attributeCodes.length; i++) {
            // get the code text view pair
            String attributeCode = attributeCodes[i];
            // get the corresponding attribute
            CustomAttribute attribute = customAttributes.get(attributeCode);
            // attribute is not available, continue to the next iteration
            if (attribute == null) {
                continue;
            }
            String value = attribute.getSelectedValue();
            if (attribute != null && !TextUtils.isEmpty(value)
                    && !value.equalsIgnoreCase(CustomAttribute.NOT_AVAILABLE_VALUE)) {
                if (i == 0) {
                    // if it is name attribute
                    plainText.append(value);
                } else {
                    // if it is first matched description attribute
                    extra = value;
                    break;
                }
            }
        }
        if (TextUtils.isEmpty(extra)) {
            // if both short and long descriptions are empty
            //
            // find first best not empty text attribute
            for (final CustomAttribute customAttribute : customAttributes.values()) {
                if (customAttribute.isOfType(CustomAttribute.TYPE_TEXT)
                        || customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA)) {
                    extra = customAttribute.getSelectedValue();
                    if (!TextUtils.isEmpty(extra)) {
                        break;
                    }
                }
            }
        }
        // append description if exists
        if (!TextUtils.isEmpty(extra)) {
            if (plainText.length() > 0) {
                plainText.append("\n\n");
            }
            plainText.append(extra);
        }
        return plainText;
    }

    /**
     * Share the product details at facebook <br>
     * Taken from:http://stackoverflow.com/a/21189010/527759
     * 
     * @param activity the activity which performs share action
     * @param url the product URL
     */
    private static void shareProductAtFacebook(final BaseFragmentActivity activity, String url) {
        Intent intent = ShareCompat.IntentBuilder.from(activity).setType("text/plain")
                .setText(url).getIntent();
        // See if official Facebook app is found
        boolean facebookAppFound = CommonUtils.filterByPackageName(activity, intent,
                "com.facebook.katana");
    
        // As fallback, launch sharer.php in a browser
        if (!facebookAppFound) {
            String sharerUrl = "https://www.facebook.com/sharer/sharer.php?u=" + url;
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sharerUrl));
        }
    
        activity.startActivity(intent);
    }

    /**
     * Share the product details at twitter <br>
     * Taken from: http://stackoverflow.com/a/21186753/527759
     * 
     * @param plainText the share text message
     * @param url the product URL
     * @param activity the activity which performs share action
     */
    private static void shareProductAtTwitter(StringBuilder plainText,
            String url, final BaseFragmentActivity activity) {
        String message = plainText.toString();
        if (message.length() >= 117) {
            // if message exceeds maximum allowed length
            message = message.substring(0, 116) + "…";
        }
        // Create intent using ACTION_VIEW and a normal Twitter URL:
        String tweetUrl = String.format("https://twitter.com/intent/tweet?text=%s&url=%s",
                CommonUtils.urlEncode(message), CommonUtils.urlEncode(url));
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl));
    
        // set the intent to launch native twitter application if exists
        CommonUtils.filterByPackageName(activity, intent, "com.twitter");
    
        activity.startActivity(intent);
    }

    /**
     * Share the product details at Pinterest <br>
     * Taken from: http://stackoverflow.com/a/28197134/527759
     * 
     * @param product the product to share
     * @param plainText the share text message
     * @param url the product URL
     * @param getImageUrlCallable the callable to retrieve image URL from the
     *            {@link imageInfo}
     * @param activity the activity which performs share action
     */
    private static void shareProductAtPinterest(Product product,
            StringBuilder plainText, String url, CallableWithParameterAndResult<Product.imageInfo, String> getImageUrlCallable,
            final BaseFragmentActivity activity) {
        String imgUrl = "";
        // get the main image URL
        for (int i = 0, size = product.getImages().size(); i < size; i++) {
    
            imageInfo ii = product.getImages().get(i);
            if (!ii.getMain()) {
                continue;
            }
            imgUrl = getImageUrlCallable.call(ii);
        }
        String pinterestUrl = String
                .format("https://www.pinterest.com/pin/create/button/?url=%s&description=%s&media=%s",
                        CommonUtils.urlEncode(url),
                        CommonUtils.urlEncode(plainText.toString()),
                        CommonUtils.urlEncode(imgUrl));
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(pinterestUrl));
        // set the intent to launch native pinterest application if exists
        CommonUtils.filterByPackageName(activity, intent, "com.pinterest");
    
        activity.startActivity(intent);
    }

    /**
     * Share product in any other native application which supports used share
     * types
     * 
     * @param product the product to share
     * @param settings the application settings
     * @param activity the activity which performs share action
     * @param plainText the share text message
     * @param url the product URL
     * @param getImageUrlCallable the callable to retrieve image URL from the
     *            {@link imageInfo}
     */
    private static void shareProductOther(Product product, Settings settings,
            final BaseFragmentActivity activity, StringBuilder plainText, String url,
            CallableWithParameterAndResult<Product.imageInfo, String> getImageUrlCallable) {
        // add product URL
        if (plainText.length() > 0) {
            plainText.append("\n\n");
        }
        plainText.append(url);

        ShareCompat.IntentBuilder shareIntentBuilder = ShareCompat.IntentBuilder.from(
                activity).setText(plainText.toString());
        final Intent shareIntent = shareIntentBuilder.getIntent();
        LoadImagesForProduct task = new LoadImagesForProduct(
                product,
                true,
                JobCacheManager.getImageDownloadDirectory(product.getSku(),
                        settings.getUrl(), true), getImageUrlCallable, settings, activity) {
            @Override
            protected void onSuccessPostExecute() {
                super.onSuccessPostExecute();
                if (activity.isActivityAlive()) {
                    // if activity is still alive
                    //
                    // need to use custom implementation because of this bug
                    // https://code.google.com/p/android/issues/detail?id=54391
                    ArrayList<Uri> mStreams = new ArrayList<Uri>();
                    for (File f : getDownloadedImages()) {
                        mStreams.add(Uri.fromFile(f));
                    }
                    if (mStreams.size() > 1) {
                        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,
                                mStreams);

                    } else {
                        shareIntent.setAction(Intent.ACTION_SEND);
                        if (!mStreams.isEmpty()) {
                            shareIntent.putExtra(Intent.EXTRA_STREAM, mStreams.get(0));
                        }
                    }
                    if (mStreams.isEmpty()) {
                        shareIntent.setType("text/plain");
                    } else {
                        shareIntent.setType("image/jpeg");
                    }
                    activity.startActivity(Intent.createChooser(shareIntent,
                            CommonUtils.getStringResource(R.string.share_dialog_title)));
                }
            }

            @Override
            public boolean isProcessingCancelled() {
                // cancel downloading also if activity is closed
                return super.isProcessingCancelled() || !activity.isActivityAlive();
            }
        };
        task.executeOnExecutor(ImageWorker.THREAD_POOL_EXECUTOR);
    }

    public static class DefaultShareItemsAdapter extends BaseAdapter {
        /**
         * The items text
         */
        String[] mLabels;
        /**
         * The items icon resources
         */
        int[] mIcons;
        /**
         * Layout inflater
         */
        LayoutInflater mLayoutInflater;

        /**
         * @param labels The items text
         * @param icons The items icons
         * @param layoutInflater Layout inflater
         */
        public DefaultShareItemsAdapter(String[] labels, TypedArray icons,
                LayoutInflater layoutInflater) {
            mLabels = labels;
            mIcons = new int[icons.length()];
            for (int i = 0, size = mIcons.length; i < size; i++) {
                mIcons[i] = icons.getResourceId(i, -1);
            }
            icons.recycle();
            mLayoutInflater = layoutInflater;
        }

        @Override
        public int getCount() {
            return mLabels.length;
        }

        @Override
        public String getItem(int position) {
            return mLabels[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // view holder pattern implementation
            ViewHolder holder;
            if (convertView == null) {
                // if it's not recycled, instantiate and initialize
                convertView = mLayoutInflater.inflate(R.layout.simple_list_item_with_icon, parent,
                        false);
                holder = new ViewHolder();
                holder.icon = (ImageView) convertView.findViewById(android.R.id.icon);
                holder.text = (TextView) convertView.findViewById(android.R.id.text1);
                convertView.setTag(holder);
            } else {
                // Otherwise re-use the converted view
                holder = (ViewHolder) convertView.getTag();
            }
            holder.text.setText(getItem(position));

            int icon = mIcons[position];
            holder.icon.setImageResource(icon);
            holder.icon.setVisibility(icon == -1 ? View.GONE : View.VISIBLE);

            return convertView;
        }

        /**
         * Class to store views for the viewholder pattern implementation
         */
        class ViewHolder {
            ImageView icon;
            TextView text;
        }
    }
}
