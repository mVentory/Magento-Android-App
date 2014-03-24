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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import android.app.Activity;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.ExceptionParser;
import com.google.analytics.tracking.android.ExceptionReporter;
import com.google.analytics.tracking.android.Fields;
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

    /**
     * Track background event
     * 
     * @param action
     * @param label
     */
    public static void trackBackgroundEvent(String action, String label) {
        trackEvent("background_event", action, label);
    }

    /**
     * @param inteval
     * @param action
     * @param holder
     */
    public static void trackDataProcessingTiming(long inteval, String action, String holder) {
        trackTiming("data_processing", inteval, action, holder);
    }

    /**
     * Track timing
     * 
     * @param category
     * @param inteval
     * @param name
     * @param label
     */
    public static void trackTiming(String category, long inteval, String name, String label) {
        getEasyTracker().send(MapBuilder.createTiming(category, inteval, name, label).build());
    }

    /**
     * @param interval
     * @param action
     * @param holder
     */
    public static void trackDataLoadTiming(long interval, String action, String holder) {
        CommonUtils.debug(TAG, "Data load timing for \"%1$s.%2$s\" is %3$d ms", action, holder,
                interval);
        trackTiming("data_load", interval, action, holder);
    }

    /**
     * Track error event
     * 
     * @param action
     * @param label
     */
    public static void trackErrorEvent(String action, String label) {
        trackEvent("error_event", action, label);
    }

    /**
     * Track an event
     * 
     * @param category
     * @param action
     * @param label
     */
    public static void trackEvent(String category, String action, String label) {
        getEasyTracker().send(MapBuilder.createEvent(category, action, label, null).build());
    }

    /**
     * Called when activity is started
     * 
     * @param activity
     */
    public static void activityStart(Activity activity) {
        getEasyTracker().activityStart(activity);
    }

    /**
     * Called when activity is stopped
     * 
     * @param activity
     */
    public static void activityStop(Activity activity) {
        getEasyTracker().activityStop(activity);
    }

    /**
     * Track view
     * 
     * @param view
     */
    public static void trackView(Object view) {
        EasyTracker easyTracker = getEasyTracker();
        String currentScreenName = easyTracker.get(Fields.SCREEN_NAME);
        easyTracker.set(Fields.SCREEN_NAME, view.getClass().getSimpleName());
        getEasyTracker().send(MapBuilder.createAppView().build());
        easyTracker.set(Fields.SCREEN_NAME, currentScreenName);
    }

    /**
     * Track button click event
     * 
     * @param buttonName
     * @param buttonHolder
     */
    public static void trackButtonClickEvent(String buttonName, Object buttonHolder) {
        trackUiEvent(buttonHolder.getClass().getSimpleName() + ".ButtonClick", buttonName);
    }

    public static void trackLongClickEvent(String string, Object view) {
        // TODO Auto-generated method stub

    }

    /**
     * Track ui event
     * 
     * @param action
     * @param label
     */
    public static void trackUiEvent(String action, String label) {
        trackEvent("ui_event", action, label);
    }

    /**
     * Track throwable
     * 
     * @param t
     */
    public static void trackThrowable(Throwable t) {
        getEasyTracker()
                .send(MapBuilder.createException(
                sParser.getDescription(Thread.currentThread().getName(), t), false).build());
    }

    public static EasyTracker getEasyTracker() {
        return EasyTracker.getInstance(MyApplication.getContext());
    }
}
