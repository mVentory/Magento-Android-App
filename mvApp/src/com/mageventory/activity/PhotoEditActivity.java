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
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.mageventory.R;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.adapters.CropOptionAdapter;
import com.mageventory.model.CropOption;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.Util;

/**
 * Activity used for photo edit operations: delete, save, rotate left/right,
 * crop
 * 
 * @author Bogdan Petran
 */
public class PhotoEditActivity extends BaseActivity {

    static final String TAG = PhotoEditActivity.class.getSimpleName();

    /**
     * GestureListener used to handle click events on the webview and toggle the
     * buttons visibility
     * 
     * @author Bogdan Petran
     */
    private static class TapDetector extends SimpleOnGestureListener {

        WeakReference<PhotoEditActivity> activityReference;
        final PhotoEditActivity activityInstance;

        public TapDetector(PhotoEditActivity photoEditActivity) {
            activityReference = new WeakReference<PhotoEditActivity>(photoEditActivity);
            activityInstance = activityReference.get();
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            activityInstance.changeButtonsVisibility();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    /**
     * WebViewClient used just for dismissing the loading progress bar visible
     * in the title bar after the load of the web page (image) is finished
     * 
     * @author Bogdan Petran
     */
    private static class MyWebViewClient extends WebViewClient {

        WeakReference<PhotoEditActivity> activityReference;
        final PhotoEditActivity activityInstance;

        public MyWebViewClient(PhotoEditActivity instance) {
            activityReference = new WeakReference<PhotoEditActivity>(instance);
            activityInstance = activityReference.get();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            // hide the loading spinning wheel in title bar after all data is
            // loaded
            activityInstance.setProgressBarIndeterminateVisibility(false);
        }
    }

    public static final String IMAGE_PATH_ATTR = "img_path"; // attribute id
                                                             // used for
                                                             // communicating
                                                             // between the
                                                             // caller
                                                             // activity and
                                                             // this one
    public static final String EDIT_MODE_ATTR = "edit_mode"; // attribute id
                                                             // used for
                                                             // communicating
                                                             // between the
                                                             // caller
                                                             // activity and
                                                             // this one

    private static final int CROP_REQUEST_CODE = 1; // request code used when
                                                    // starting crop activity
    private static final int OTHER_REQUEST_CODE = 0; // request code used when
                                                     // this activity starts

    // next two lines are for disabling the webview Tip: Double tap to zoom in
    // or out
    private static final String PREF_FILE = "WebViewSettings";
    private static final String DOUBLE_TAP_TOAST_COUNT = "double_tap_toast_count";

    WebView webView;
    Bitmap imageBitmap;
    String imagePath;
    LinearLayout buttonsLayout;
    GestureDetector gestureDetector; // used to catch double taps
    View zoom;
    boolean editMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        init(false);
    }

    private void init(boolean restarted) {

        setContentView(R.layout.photo_edit);

        // show the loading spinning wheel in title bar
        setProgressBarIndeterminateVisibility(true);

        buttonsLayout = (LinearLayout) findViewById(R.id.buttonsLinearLayout);

        gestureDetector = new GestureDetector(new TapDetector(this));

        // hack done to remove the double tap tip from WebView
        SharedPreferences prefs = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        if (prefs.getInt(DOUBLE_TAP_TOAST_COUNT, 1) > 0) {
            prefs.edit().putInt(DOUBLE_TAP_TOAST_COUNT, 0).commit();
        }

        // initialize web view
        webView = (WebView) findViewById(R.id.imageHolderWebView);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setWebViewClient(new MyWebViewClient(this));

        WebSettings webSettings = webView.getSettings();
        webSettings.setAllowFileAccess(true);
        webSettings.setJavaScriptEnabled(true);
        // allow native zooming
        webSettings.setBuiltInZoomControls(true);
        // avoid caching
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setAppCacheEnabled(false);
        webSettings.setLoadsImagesAutomatically(true);
        // avoid cropping
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        processData(OTHER_REQUEST_CODE, getIntent().getExtras(), restarted);
    }

    /**
     * Builds up the image from a path (when the activity is started) or from a
     * parcelable (when a crop image comes from crop application)
     * 
     * @param key can be <code>CROP_REQUEST_CODE</code> or other
     * @param extras <code>Bundle</code> containing the path as string or a
     *            parcelable bitmap (when a crop response is handled)
     */
    private void processData(int key, Bundle extras, boolean restarted) {
        if (extras != null) {

            if (key == CROP_REQUEST_CODE) {
                // use the croped image and then save it on sdCard
                imageBitmap = extras.getParcelable("data");
            } else {
                // get image from the path received through intent extras
                imagePath = extras.getString(IMAGE_PATH_ATTR);

                // check if this is edit mode or just preview mode
                editMode = extras.getBoolean(EDIT_MODE_ATTR);

                if (editMode) {
                    // set inSampleSize to 4 to avoid out of memory exceptions
                    BitmapFactory.Options opts = new Options();

                    if (!restarted) {
                        System.out.println("Resizing image to 4 times smaller");
                        opts.inSampleSize = 4;
                    }

                    imageBitmap = BitmapFactory.decodeFile(imagePath, opts);
                }
            }

            if (editMode) {
                // when in edit mode save the resized image on sdcard and make
                // sure the buttons are visible
                Util.saveBitmapOnSDcard(imageBitmap, imagePath);
                buttonsLayout.setVisibility(View.VISIBLE);
            } else {
                changeButtonsVisibility();
            }

            loadImage();
        }
    }

    /**
     * This method will handle the click events occured on the buttons contained
     * by this activity
     * 
     * @param v the <code>View</code> on which the click occured
     */
    public void onClick(View v) {
        if (v.getId() == R.id.cropBtn) {
            doCrop(CROP_REQUEST_CODE);
        } else if (v.getId() == R.id.deleteBtn) {
            // show the delete confirmation when the delete button was pressed
            // on an item and then sends a result back to the caller activity
            Builder alertDialogBuilder = new Builder(this);
            alertDialogBuilder.setTitle("Confirm deletion");
            alertDialogBuilder.setNegativeButton("No", null);
            alertDialogBuilder.setPositiveButton("Yes", new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // sends back a result to the caller activity

                    setResult(true);
                }
            });
            alertDialogBuilder.show();
        } else if (v.getId() == R.id.saveBtn) {
            // sends back a result to the caller activity
            setResult(false);
        } else if (v.getId() == R.id.rotateLeftBtn) {
            rotateImage(true);
        } else if (v.getId() == R.id.rotateRightBtn) {
            rotateImage(false);
        }
    }

    private void rotateImage(boolean left) {
        setProgressBarIndeterminateVisibility(true);

        int width = imageBitmap.getWidth();
        int height = imageBitmap.getHeight();

        // createa matrix for rotation
        Matrix matrix = new Matrix();
        matrix.postRotate(left ? -90 : 90);

        // recreate the new Bitmap
        Bitmap resizedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, width, height, matrix, true);

        // recycle the imageBitmap so the memory is freed
        imageBitmap.recycle();
        imageBitmap = resizedBitmap;

        Util.saveBitmapOnSDcard(imageBitmap, imagePath);

        loadImage();
    }

    /**
     * Wait for a result from the Crop Application
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {
            processData(requestCode, data.getExtras(), false);
        }
    }

    /**
     * Starts a CROP application or if there are more than one installed on the
     * device, will let user to choose which one he want's to use.
     * 
     * @param request_Code will be <code>CROP_REQUEST_CODE</code>
     * @author Lorensius W. L. T <lorenz@londatiga.net>
     * @author Bogdan Petran
     */
    private void doCrop(final int request_Code) {

        // create new intent to start the CROP process
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");

        // find all crop apps available on phone
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);

        int size = list.size();

        if (size == 0) {
            GuiUtils.alert("Can not find image crop app");

            return;
        } else {

            // create URI from the imagePath received through the activity
            // Intent on startup
            final Uri mImageCaptureUri = Uri.fromFile(new File(imagePath));
            intent.setData(mImageCaptureUri);

            // set the aspect ration of the crop rectangle
            intent.putExtra("aspectX", imageBitmap.getWidth());
            intent.putExtra("aspectY", imageBitmap.getHeight());

            intent.putExtra("scale", true);
            intent.putExtra("return-data", true);

            // if there is only one CROP app, start it
            if (size == 1) {
                Intent i = new Intent(intent);
                ResolveInfo res = list.get(0);

                i.setComponent(new ComponentName(res.activityInfo.packageName,
                        res.activityInfo.name));
                startActivityForResult(i, request_Code);
            } else {
                // if there are more than one CROP apps, create a dialog with a
                // list containg all CROP apps available and let user choos
                // which app he wants to use
                final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();

                for (ResolveInfo res : list) {
                    final CropOption co = new CropOption();

                    co.title = getPackageManager().getApplicationLabel(
                            res.activityInfo.applicationInfo);
                    co.icon = getPackageManager().getApplicationIcon(
                            res.activityInfo.applicationInfo);
                    co.appIntent = new Intent(intent);

                    co.appIntent.setComponent(new ComponentName(res.activityInfo.packageName,
                            res.activityInfo.name));

                    cropOptions.add(co);
                }

                CropOptionAdapter adapter = new CropOptionAdapter(getApplicationContext(),
                        cropOptions);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Choose Crop App");
                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        startActivityForResult(cropOptions.get(item).appIntent, request_Code);
                    }
                });

                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {

                        if (mImageCaptureUri != null) {
                            // remove the URI from the content resolver if there
                            // will be no CROP
                            getContentResolver().delete(mImageCaptureUri, null, null);
                        }
                    }
                });

                AlertDialog alert = builder.create();

                alert.show();
            }
        }
    }

    private void saveImgOnSDCard() {
        try {
            FileOutputStream fo = new FileOutputStream(imagePath);
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, fo);

            fo.flush();
            fo.close();
        } catch (Exception e) {
            CommonUtils.error(TAG, e);
        }
    }

    /**
     * Send a result back to the caller activity
     * 
     * @param deleted <code>true</code> if the image is deleted to be deleted
     *            locally and <code>false</code> if it is to be saved by the
     *            caller activity
     */
    private void setResult(boolean deleted) {

        if (deleted) {
            // physically delete the current image from sdCard because we no
            // longer need it
            File f = new File(imagePath);
            if (f.exists()) {
                f.delete();
            }

            setResult(RESULT_CANCELED);
        } else {
            Intent i = new Intent();
            i.putExtra(IMAGE_PATH_ATTR, imagePath);

            setResult(RESULT_OK, i);
        }

        // clear the view so the bitmap can be recycled and free up memory
        webView.clearView();
        webView.clearCache(true);
        imageBitmap.recycle();
        imageBitmap = null;

        finish();
    }

    @Override
    public void onBackPressed() {
        // don't let this activity to finish when back button is pressed but
        // force user to press on Delete or Save buttons in order to exit if in
        // edit mode
        if (editMode) {
            setResult(RESULT_CANCELED);
            return;
        }

        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            // don't do anything when selecting refresh from the menu
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * We have 2 possibilities: load a URL or load an image from sdCard
     */
    private void loadImage() {
        webView.clearCache(true);
        webView.clearView();

        // used to load different css code for both landscape/portrait
        boolean isLandscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        // on portrait make full width, in landscape make full height
        String imageSize = isLandscape ? "height" : "width";

        // css/html code for portrait
        String htmlForPortrait = "<html>" + "<body>" + "<table style=\"height:100%; width:100%;\">"
                + "<tr>"
                + "<td style=\";horizontal-align:middle; vertical-align:middle;\">"
                + "<img src = \"" + imagePath
                + "\" " + imageSize + "=\"100%\"/>" + "</td>" + "</tr>" + "</table>" + "</body>"
                + "</html>";

        // css/html code for landscape
        String htmlForLandscape = "<html>"
                + "<body style=\"text-align: center; horizontal-align:center; vertical-align: center;\">"
                + "<img src = \"" + imagePath + "\" " + imageSize + "=\"100%\"/>" + "</body>"
                + "</html>";

        // final url to load into web view
        String html = isLandscape ? htmlForLandscape : htmlForPortrait;

        // check if needs to be loaded from sdcard or from server
        String root = URLUtil.isValidUrl(imagePath) ? "" : "file:///";

        // load the web view with the image
        webView.loadDataWithBaseURL(root, html, "text/html", "UTF-8", "");
    }

    /**
     * Changes the visibility of the buttons available in this view.
     */
    private void changeButtonsVisibility() {

        if (buttonsLayout.getVisibility() == View.VISIBLE) {
            buttonsLayout.setVisibility(View.GONE);
            return;
        }

        if (!editMode) {
            return;
        }

        buttonsLayout.setVisibility(View.VISIBLE);
    }

    /*
     * Need to override this to load different layouts for portrait/landscape
     * @see
     * android.app.Activity#onConfigurationChanged(android.content.res.Configuration
     * )
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Disable changing layout to landscape version and reloading the webview
//        init(true);
    }

    /*
     * Need to implement this to intercept a click event in WebView - web view
     * do not have any onClickListener so I need to intercept any touch in the
     * screen and dispatch events as necessary
     * @see android.app.Activity#dispatchTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // test if the gesture was consumed by the detector
        if (gestureDetector.onTouchEvent(ev))
            return true;

        return super.dispatchTouchEvent(ev);
    }
}
