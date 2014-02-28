
package com.mageventory.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.ExceptionParser;
import com.google.analytics.tracking.android.ExceptionReporter;
import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.MapBuilder;
import com.mageventory.MyApplication;

public class TrackerUtils {
    private static final String TAG = TrackerUtils.class.getSimpleName();

    /**
     * Used for tests
     */
    public static boolean SKIP_UNCAUGHT_SETUP = false;
    /**
     * The exception parser used to track exceptions
     */
    static ExceptionParser sParser = new ExceptionParser() {
        @Override
        public String getDescription(String threadName, Throwable t) {
            return getStackTrace(t);
        }

    };

    private static String getStackTrace(Throwable throwable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        throwable.printStackTrace(printWriter);

        return result.toString();
    }

    /**
     * Setup uncaugt exception handler
     */
    public static void setupTrackerUncaughtExceptionHandler() {
        EasyTracker.getInstance(MyApplication.getContext());
        if (SKIP_UNCAUGHT_SETUP) {
            return;
        }
        ExceptionReporter myHandler = new ExceptionReporter(EasyTracker.getInstance(MyApplication
                .getContext()), // Currently
                GAServiceManager.getInstance(), // GoogleAnalytics
                Thread.getDefaultUncaughtExceptionHandler(), // Current default
                                                             // uncaught
                                                             // exception
                                                             // handler.
                MyApplication.getContext());
        myHandler.setExceptionParser(sParser);

        Thread.setDefaultUncaughtExceptionHandler(myHandler);
    }

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

    public static void trackView(Object view) {
        // TODO Auto-generated method stub

    }

    public static void trackButtonClickEvent(String string, Object view) {
        // TODO Auto-generated method stub

    }

    public static void trackLongClickEvent(String string, Object view) {
        // TODO Auto-generated method stub

    }

    public static void trackUiEvent(String string, String string2) {
        // TODO Auto-generated method stub

    }

    /**
     * Track throwable
     * 
     * @param t
     */
    public static void trackThrowable(Throwable t) {
        EasyTracker easyTracker = EasyTracker.getInstance(MyApplication.getContext());
        easyTracker.send(MapBuilder.createException(
                sParser.getDescription(Thread.currentThread().getName(), t), false).build());
    }
}
