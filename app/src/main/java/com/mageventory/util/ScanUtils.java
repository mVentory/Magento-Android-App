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
package com.mageventory.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;

import com.mageventory.settings.Settings;
import com.mventory.R;

/**
 * Utility class for scan operations
 * 
 * @version 17.06.2014<br>
 *          - ScanUtils: added startScanActivityForResult method variations with
 *          the checkShowDownloadDialogSettings parameter<br>
 *          - ScanUtils: added setDisplayZXingInstallRequest call if ignore
 *          button is pressed in the showDownloadDialog method
 */
public class ScanUtils {
    private static final String TAG = ScanUtils.class.getSimpleName();

    public static final String SCAN_ACTIVITY = "com.google.zxing.client.android.SCAN";
    private static final String BS_PACKAGE = "com.google.zxing.client.android";
    public static final String SCAN_ACTIVITY_RESULT = "SCAN_RESULT";
    /**
     * Call {@link android.content.Intent#getStringExtra(String)} with
     * {@link #RESULT_UPC_EAN_EXTENSION} to return the content of any UPC
     * extension barcode that was also found. Only applicable to
     * {@link com.google.zxing.BarcodeFormat#UPC_A} and
     * {@link com.google.zxing.BarcodeFormat#EAN_13} formats.
     */
    public static final String RESULT_UPC_EAN_EXTENSION = "SCAN_RESULT_UPC_EAN_EXTENSION";

    /**
     * Separator between UPC/EAN code and the its extension
     */
    public static final String UPC_EAN_EXTENSION_SEPARATOR = "-";

    /**
     * Prompt to show on-screen when scanning by intent. Specified as a
     * {@link String}.
     */
    public static final String PROMPT_MESSAGE = "PROMPT_MESSAGE";

    public enum ScanState {
        NOT_SCANNED,
        SCANNED_DETECTED_NOT_DECODED("DND"),
        SCANNED_NOT_DETECTED("ND"),
        SCANNED_DECODED("D")

        ;
        String mSuffix;

        ScanState() {
            this(null);
        }

        ScanState(String suffix) {
            this.mSuffix = suffix;
        }

        String getSuffix() {
            return mSuffix;
        }

        public static ScanState getScanStateBySuffix(String suffix) {
            ScanState result = NOT_SCANNED;
            for (ScanState ss : values()) {
                if (TextUtils.equals(suffix, ss.getSuffix())) {
                    result = ss;
                    break;
                }
            }
            return result;
        }
    }

    public final static Pattern SCAN_STATE_PATTERN = Pattern.compile(
            "^(.*)(\\[(" + ScanState.SCANNED_DETECTED_NOT_DECODED.getSuffix() + "|"
                    + ScanState.SCANNED_NOT_DETECTED.getSuffix() + "|"
                    + ScanState.SCANNED_DECODED.getSuffix() + ")\\])((\\.[^\\.]*))$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Start scan activity for result and check whether it exists before
     * starting
     * 
     * @param activity the activity from where the scan action is initiated
     * @param requestCode the request code for the scan activity, so it can be
     *            checked in the
     *            {@link Fragment#onActivityResult(int, int, Intent)} or
     *            {@link Activity}.onActivityResult method
     * @param scanMessage the scan message which should be used in the scan activity
     * @return true in case package found and activity started
     */
    public static boolean startScanActivityForResult(Activity activity, int requestCode,
            Integer scanMessage) {
        return startScanActivityForResult(activity, requestCode, scanMessage, false);
    }
    
    /**
     * Start scan activity for result and check whether it exists before
     * starting
     * 
     * @param activity the activity from where the scan action is initiated
     * @param fragment the fragment for which the scan activity should be
     *            started. To receive
     *            {@link Fragment#onActivityResult(int, int, Intent)} event
     *            inside the Fragment the
     *            {@link Fragment#startActivityForResult(Intent, int)} should be
     *            called instead of
     *            {@link Activity#startActivityForResult(Intent, int)}
     * @param requestCode the request code for the scan activity, so it can be
     *            checked in the
     *            {@link Fragment#onActivityResult(int, int, Intent)} or
     *            {@link Activity}.onActivityResult method
     * @param scanMessage the scan message which should be used in the scan activity
     * @return true in case package found and activity started
     */
    public static boolean startScanActivityForResult(Activity activity, Fragment fragment, int requestCode,
            Integer scanMessage) {
        return startScanActivityForResult(activity, fragment, requestCode, scanMessage, false);
    }

    /**
     * Start scan activity for result and check whether it exists before
     * starting
     * 
     * @param activity the activity from where the scan action is initiated
     * @param requestCode the request code for the scan activity, so it can be
     *            checked in the {@link Activity}.onActivityResult method
     * @param scanMessage the scan message which should be used in the scan
     *            activity
     * @param checkShowDownloadDialogSettings whether to check ignore ZXing
     *            installation button was pressed before
     * @return true in case package found and activity started
     */
    public static boolean startScanActivityForResult(Activity activity, int requestCode,
            Integer scanMessage, boolean checkShowDownloadDialogSettings) {
        Intent intent = getScanActivityIntent();
        return startScanActivityForResult(activity, null, intent, requestCode, scanMessage,
                new GoToHomeRunnable(activity), null, checkShowDownloadDialogSettings);
    }
    
    /**
     * Start scan activity for result and check whether it exists before
     * starting
     * 
     * @param activity the activity from where the scan action is initiated
     * @param fragment the fragment for which the scan activity should be
     *            started. To receive
     *            {@link Fragment#onActivityResult(int, int, Intent)} event
     *            inside the Fragment the
     *            {@link Fragment#startActivityForResult(Intent, int)} should be
     *            called instead of
     *            {@link Activity#startActivityForResult(Intent, int)}
     * @param requestCode the request code for the scan activity, so it can be
     *            checked in the
     *            {@link Fragment#onActivityResult(int, int, Intent)} or
     *            {@link Activity}.onActivityResult method
     * @param scanMessage the scan message which should be used in the scan activity
     * @param checkShowDownloadDialogSettings whether to check ignore ZXing
     *            installation button was pressed before
     * @return true in case package found and activity started
     */
    public static boolean startScanActivityForResult(Activity activity, Fragment fragment,
            int requestCode, Integer scanMessage, boolean checkShowDownloadDialogSettings) {
        Intent intent = getScanActivityIntent();
        return startScanActivityForResult(activity, fragment, intent, requestCode,
                scanMessage, new GoToHomeRunnable(activity), null, checkShowDownloadDialogSettings);
    }

    /**
     * Start scan activity for result and check whether it exists before
     * starting
     * 
     * @param activity the activity from where the scan action is initiated
     * @param intent the scan activity intent
     * @param requestCode the request code for the scan activity, so it can be
     *            checked in the {@link Activity}.onActivityResult method
     * @param scanMessage the scan message which should be used in the scan
     *            activity
     * @return true in case package found and activity started
     */
    public static boolean startScanActivityForResult(Activity activity, Intent intent,
            int requestCode, Integer scanMessage) {
        return startScanActivityForResult(activity, intent, requestCode, scanMessage, true);
    }

    /**
     * Start scan activity for result and check whether it exists before
     * starting
     * 
     * @param activity the activity from where the scan action is initiated
     * @param intent the scan activity intent
     * @param requestCode the request code for the scan activity, so it can be
     *            checked in the {@link Activity}.onActivityResult method
     * @param scanMessage the scan message which should be used in the scan
     *            activity
     * @param goToHomeIfInstallZXingPressed whether to navigate user to home
     *            activity if he selects install ZXing option in the missing
     *            ZXing dialog
     * @return true in case package found and activity started
     */
    public static boolean startScanActivityForResult(Activity activity, Intent intent,
            int requestCode, Integer scanMessage, boolean goToHomeIfInstallZXingPressed) {
        return startScanActivityForResult(activity, null, intent, requestCode, scanMessage,
                goToHomeIfInstallZXingPressed ? new GoToHomeRunnable(activity) : null, null, false);
    }

    /**
     * Start scan activity for result and check whether it exists before
     * starting
     * 
     * @param activity the activity from where the scan action is initiated
     * @param requestCode the request code for the scan activity, so it can be
     *            checked in the {@link Activity}.onActivityResult method
     * @param scanMessage the scan message which should be used in the scan
     *            activity
     * @param runOnInstallRequested the action to run in case user requests
     *            install ZXing app in the missing scanner dialog
     * @param runOnInstallDismissed the action to run in case user decline
     *            install ZXing app in the missing scanner dialog
     * @return true in case package found and activity started
     */
    public static boolean startScanActivityForResult(Activity activity, int requestCode,
            Integer scanMessage,
            final Runnable runOnInstallRequested, final Runnable runOnInstallDismissed) {
        return startScanActivityForResult(activity, null, getScanActivityIntent(), requestCode,
                scanMessage, runOnInstallRequested, runOnInstallDismissed, false);
    }

    /**
     * Start scan activity for result and check whether it exists before
     * starting
     * 
     * @param activity the activity from where the scan action is initiated
     * @param fragment the fragment for which the scan activity should be
     *            started. To receive
     *            {@link Fragment#onActivityResult(int, int, Intent)} event
     *            inside the Fragment the
     *            {@link Fragment#startActivityForResult(Intent, int)} should be
     *            called instead of
     *            {@link Activity#startActivityForResult(Intent, int)}
     * @param intent the scan activity intent
     * @param requestCode the request code for the scan activity, so it can be
     *            checked in the
     *            {@link Fragment#onActivityResult(int, int, Intent)} or
     *            {@link Activity}.onActivityResult method
     * @param scanMessage the scan message which should be used in the scan
     *            activity
     * @param runOnInstallRequested
     * @param runOnInstallDismissed
     * @param checkShowDownloadDialogSettings whether to check ignore ZXing
     *            installation button was pressed before
     * @return true in case package found and activity started
     */
    public static boolean startScanActivityForResult(Activity activity, Fragment fragment,
            Intent intent, int requestCode, final Integer scanMessage,
            final Runnable runOnInstallRequested, final Runnable runOnInstallDismissed,
            boolean checkShowDownloadDialogSettings) {

        String targetAppPackage = findTargetAppPackage(activity, intent);
        if (targetAppPackage == null) {
            boolean showDownloadDialog = !checkShowDownloadDialogSettings;
            if (!showDownloadDialog) {
                Settings settings = new Settings(activity);
                showDownloadDialog = settings.getDisplayZXingInstallRequest();
            }
            if (showDownloadDialog) {
                showDownloadDialog(activity, runOnInstallRequested, runOnInstallDismissed);
            }
            return false;
        } else {
            if (scanMessage != null) {
                intent.putExtra(PROMPT_MESSAGE, CommonUtils.getStringResource(scanMessage));
                GuiUtils.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        GuiUtils.alert(scanMessage);
                    }
                }, 1500);
            }
            // lock current activity orientation so after the returning from
            // scan it will be restored in case user didn't rotate the device
            GuiUtils.lockOrientation(activity);
            if (fragment != null) {
                /*
                 * if fragment is specified use Fragment.startActivityForResult
                 * method to receive result in the Fragment.onActivityResult
                 * method properly. Otherwise result event will not be fired to
                 * the Fragment
                 */
                fragment.startActivityForResult(intent, requestCode);
            } else {
                activity.startActivityForResult(intent, requestCode);
            }
            return true;
        }
    }

    /**
     * Check whether Android OS has ZXing app installed
     * 
     * @param activity
     * @return
     */
    public static boolean hasZxingInstalled(Activity activity) {
        String targetAppPackage = findTargetAppPackage(activity, getScanActivityIntent());
        return targetAppPackage != null;
    }

    /**
     * Get the scan activity standard intent
     * 
     * @return
     */
    public static Intent getScanActivityIntent() {
        return new Intent(SCAN_ACTIVITY);
    }

    private static String findTargetAppPackage(Activity activity, Intent intent) {
        PackageManager pm = activity.getPackageManager();
        List<ResolveInfo> availableApps = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (availableApps != null) {
            for (ResolveInfo availableApp : availableApps) {
                String packageName = availableApp.activityInfo.packageName;
                if (packageName.equals(BS_PACKAGE)) {
                    return packageName;
                }
            }
        }
        return null;
    }

    private static AlertDialog showDownloadDialog(final Activity activity, final Runnable runOnOk,
            final Runnable runOnDismiss) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(activity);
        downloadDialog.setTitle(R.string.scan_install_question_title);
        downloadDialog.setMessage(R.string.scan_install_question_text);
        downloadDialog.setPositiveButton(R.string.scan_install_yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String packageName = BS_PACKAGE;
                        Uri uri = Uri.parse("market://details?id=" + packageName);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            activity.startActivity(intent);
                            if (runOnOk != null) {
                                runOnOk.run();
                            }
                        } catch (ActivityNotFoundException anfe) {
                            // Hmm, market is not installed
                            GuiUtils.alert("Google Play is not installed; cannot install");
                            Log.w(TAG, "Google Play is not installed; cannot install "
                                    + packageName);
                            if (runOnDismiss != null) {
                                runOnDismiss.run();
                            }
                        }
                    }
                });
        downloadDialog.setNegativeButton(R.string.scan_install_no,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                        GuiUtils.alert(R.string.scan_install_no_message);
                        Settings settings = new Settings(activity);
                        settings.setDisplayZXingInstallRequest(false);
                        if (runOnDismiss != null) {
                            runOnDismiss.run();
                        }
            }
        });
        downloadDialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                GuiUtils.alert(R.string.scan_install_no_message);
                if (runOnDismiss != null) {
                    runOnDismiss.run();
                }

            }
        });
        return downloadDialog.show();
    }

    /**
     * Get the scan result from intent and sanitize it from incorrect characters
     * 
     * @param intent
     * @return
     */
    public static String getSanitizedScanResult(Intent intent) {
        return getFullSanitizedScanResult(intent).getCodeWithExtension();
    }

    /**
     * Get the full scan result (including metadata) from intent and sanitize
     * scan result parts
     * 
     * @param intent the intent to get the scan results from
     * @return {@link ScanResult} which contains scanned text/code and metadata
     *         if present
     */
    public static ScanResult getFullSanitizedScanResult(Intent intent) {
        String content = intent.getStringExtra(SCAN_ACTIVITY_RESULT);
        // append UPC EAN extension to the scanned code with the "-" separator
        String extension = intent.getStringExtra(RESULT_UPC_EAN_EXTENSION);
        if (!TextUtils.isEmpty(extension)) {
            extension = sanitizeScanResult(extension);
        }
        if (content != null) {
            content = sanitizeScanResult(content);
        }
        return new ScanResult(content, extension);
    }

    /**
     * Object to represent scan results with the metadata used in
     * getFullSanitizedScanResult method
     */
    public static class ScanResult implements Parcelable {
        /**
         * Scanned code/text
         */
        private String mCode;
        
        /**
         * Scan result metadata extension
         */
        private String mExtension;

        /**
         * @param code scanned text
         * @param extension scanned metadata
         */
        public ScanResult(String code, String extension) {
            mCode = code;
            mExtension = extension;
        }

        /**
         * Get the scanned code/text
         * 
         * @return
         */
        public String getCode() {
            return mCode;
        }

        /**
         * Get the scanned metadata extension
         * 
         * @return
         */
        public String getExtension() {
            return mExtension;
        }
        
        /**
         * Get scanned code with metadata extension string representation. If
         * exists metadata will be appended to code separated by
         * {@link ScanUtils#UPC_EAN_EXTENSION_SEPARATOR}
         * 
         * @return
         */
        public String getCodeWithExtension() {
            StringBuilder result = new StringBuilder();
            if (!TextUtils.isEmpty(mCode)) {
                result.append(mCode);
            }
            // append UPC EAN extension to the scanned code with the "-"
            // separator
            if (!TextUtils.isEmpty(mExtension)) {
                // if metadata extension is present
                result.append(UPC_EAN_EXTENSION_SEPARATOR + mExtension);
            }
            return result.toString();
        }

        /*****************************
         * PARCELABLE IMPLEMENTATION *
         *****************************/

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(mCode);
            out.writeString(mExtension);
        }

        public static final Parcelable.Creator<ScanResult> CREATOR = new Parcelable.Creator<ScanResult>() {
            @Override
            public ScanResult createFromParcel(Parcel in) {
                return new ScanResult(in);
            }

            @Override
            public ScanResult[] newArray(int size) {
                return new ScanResult[size];
            }
        };

        private ScanResult(Parcel in) {
            mCode = in.readString();
            mExtension = in.readString();
        }
    }

    /**
     * Remove all incorrect characters from the string. The range of valid
     * characters are 0x20-0xd7ff and 0xe000-0xfffd
     * 
     * @param content
     * @return
     */
    public static String sanitizeScanResult(String content) {
        return content.replaceAll("[[^\\u0020-\\ud7ff]&&[^\\ue000-\\ufffd]]", "");
    }

    /**
     * Get the scan state by file name
     * 
     * @param fileName
     * @return
     */
    public static ScanState getScanStateForFileName(String fileName) {
        Matcher m = SCAN_STATE_PATTERN.matcher(fileName);
        String suffix = null;
        if (m.find()) {
            suffix = m.group(3);
        }
        return ScanState.getScanStateBySuffix(suffix);
    }

    /**
     * Modify the fileName to set the scan state information
     * 
     * @param fileName
     * @param scanState
     * @return
     */
    public static String setScanStateForFileName(String fileName, ScanState scanState) {
        Matcher m = SCAN_STATE_PATTERN.matcher(fileName);
        if(m.find())
        {
            fileName = m.group(1)
                    + (scanState == ScanState.NOT_SCANNED ? ""
                            : ("[" + scanState.getSuffix() + "]")) + m.group(4);
        } else
        {
            if(scanState != ScanState.NOT_SCANNED)
            {
                int p = fileName.lastIndexOf('.');
                fileName = fileName.substring(0,p) + "["+scanState.getSuffix()+"]"+fileName.substring(p);
            }
        }
        return fileName;
    }

    public static class FinishActivityRunnable implements Runnable {
        Activity mActivity;

        public FinishActivityRunnable(Activity activity) {
            mActivity = activity;
        }

        @Override
        public void run() {
            mActivity.finish();
        }
    }

    public static class GoToHomeRunnable implements Runnable {
        Activity mActivity;

        public GoToHomeRunnable(Activity activity) {
            mActivity = activity;
        }

        @Override
        public void run() {
            DefaultOptionsMenuHelper.onMenuHomePressed(mActivity);
        }
    }
    
}
