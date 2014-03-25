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
import android.text.TextUtils;
import android.util.Log;

import com.mageventory.R;

/**
 * Utility class for scan operations
 */
public class ScanUtils {
    private static final String TAG = ScanUtils.class.getSimpleName();

    public static final String SCAN_ACTIVITY = "com.google.zxing.client.android.SCAN";
    private static final String BS_PACKAGE = "com.google.zxing.client.android";
    public static final String SCAN_ACTIVITY_RESULT = "SCAN_RESULT";
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
     * @param activity
     * @param requestCode
     * @param scanMessage
     * @return true in case package found and activity started
     */
    public static boolean startScanActivityForResult(Activity activity, int requestCode,
            Integer scanMessage) {
        Intent intent = getScanActivityIntent();
        return startScanActivityForResult(activity, intent, requestCode, scanMessage);
    }

    /**
     * Start scan activity for result and check whether it exists before
     * starting
     * 
     * @param activity
     * @param intent
     * @param requestCode
     * @param scanMessage
     * @return true in case package found and activity started
     */
    public static boolean startScanActivityForResult(Activity activity, Intent intent,
            int requestCode, Integer scanMessage) {
        return startScanActivityForResult(activity, intent, requestCode, scanMessage, null, null);
    }

    /**
     * Start scan activity for result and check whether it exists before
     * starting
     * 
     * @param activity
     * @param requestCode
     * @param scanMessage
     * @param runOnInstallRequested
     * @param runOnInstallDismissed
     * @return true in case package found and activity started
     */
    public static boolean startScanActivityForResult(Activity activity, int requestCode,
            Integer scanMessage,
            final Runnable runOnInstallRequested, final Runnable runOnInstallDismissed) {
        return startScanActivityForResult(activity, getScanActivityIntent(), requestCode,
                scanMessage, runOnInstallRequested, runOnInstallDismissed);
    }

    /**
     * Start scan activity for result and check whether it exists before
     * starting
     * 
     * @param activity
     * @param intent
     * @param requestCode
     * @param scanMessage
     * @param runOnInstallRequested
     * @param runOnInstallDismissed
     * @return true in case package found and activity started
     */
    public static boolean startScanActivityForResult(Activity activity, Intent intent,
            int requestCode, final Integer scanMessage, final Runnable runOnInstallRequested,
            final Runnable runOnInstallDismissed) {

        String targetAppPackage = findTargetAppPackage(activity, intent);
        if (targetAppPackage == null) {
            showDownloadDialog(activity, runOnInstallRequested, runOnInstallDismissed);
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
            activity.startActivityForResult(intent, requestCode);
            return true;
        }
    }

    /**
     * Check whether app has ZXing installed
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
                        if (runOnDismiss != null) {
                            runOnDismiss.run();
                        }
            }
        });
        downloadDialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
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
        String content = intent.getStringExtra(SCAN_ACTIVITY_RESULT);
        if (content != null) {
            content = sanitizeScanResult(content);
        }
        return content;
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

}
