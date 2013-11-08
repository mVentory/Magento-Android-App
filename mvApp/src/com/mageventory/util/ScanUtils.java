package com.mageventory.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.text.TextUtils;

/**
 * Utility class for scan operations
 */
public class ScanUtils {
    public static final String SCAN_ACTIVITY = "com.google.zxing.client.android.SCAN";
    public static final String SCAN_ACTIVITY_RESULT = "SCAN_RESULT";

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

    public static Intent getScanActivityIntent() {
        return new Intent(SCAN_ACTIVITY);
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
}
