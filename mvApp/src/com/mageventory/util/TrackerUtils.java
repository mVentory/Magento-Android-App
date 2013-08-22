package com.mageventory.util;

import com.mageventory.fragment.base.BaseDialogFragment;

public class TrackerUtils {
    private static final String TAG = TrackerUtils.class.getSimpleName();

    public static void trackBackgroundEvent(String string, String tag) {
        // TODO Auto-generated method stub
    }

    public static void trackDataProcessingTiming(long l, String string, String tag) {
        // TODO Auto-generated method stub

    }

    public static void trackDataLoadTiming(long l, String string, String tag) {
        CommonUtils.debug(TAG, "Data load timing for \"%1$s.%2$s\" is %3$d ms", tag, string, l);
        // Log.d(TAG,
        // CommonUtils.format("Data load timing for \"%1$s.%2$s\" is %3$d ms",
        // tag, string, l));
    }

    public static void trackEvent(String category, String event, String simpleName) {
        // TODO Auto-generated method stub
    }

    public static void trackView(BaseDialogFragment commonDialogFragment) {
        // TODO Auto-generated method stub

    }

}
