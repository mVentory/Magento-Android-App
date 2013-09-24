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
}
