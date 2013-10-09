package com.mageventory.util;

import android.content.Intent;

/**
 * Utility class for scan operations
 */
public class ScanUtils {
    public static final String SCAN_ACTIVITY = "com.google.zxing.client.android.SCAN";
    public static final String SCAN_ACTIVITY_RESULT = "SCAN_RESULT";

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
}
