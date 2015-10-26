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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;

import com.mageventory.MyApplication;
import com.mageventory.job.JobCacheManager;
import com.mageventory.settings.Settings;
import com.mageventory.util.EventBusUtils.BroadcastReceiverRegisterHandler;
import com.mventory.R;

public class Log {

    public static String ERROR_REPORTING_FILE_STATE_CHANGED_EVENT_ACTION = MyApplication
            .getContext().getPackageName() + ".ERROR_REPORTING_FILE_STATE_CHANGED_EVENT";
    public static String ERROR_REPORTING_FILE_STATE = MyApplication.getContext().getPackageName()
            + ".ERROR_REPORTING_FILE_STATE";
	
    static final String TAG = Log.class.getSimpleName();

    public static File logFile;
    public static Object loggingSynchronisationObject = new Object();

    public static interface OnErrorReportingFileStateChangedListener
    {
        void onErrorReportingFileStateChanged(boolean fileExists);
    }

    /**
     * Get and register the broadcast receiver for the error reporting file
     * state changed event
     * 
     * @param TAG
     * @param handler
     * @return
     */
    public static BroadcastReceiver getAndRegisterOnErrorReportingFileStateChangedBroadcastReceiver(
            final String TAG, final OnErrorReportingFileStateChangedListener handler) {
        BroadcastReceiver br = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    boolean exists = intent.getBooleanExtra(ERROR_REPORTING_FILE_STATE, false);
                    CommonUtils.debug(TAG,
                                    "Received on error reporting file state changed broadcast message. Exists: %1$b",
                                    exists);
                    handler.onErrorReportingFileStateChanged(exists);
                } catch (Exception ex) {
                    GuiUtils.noAlertError(TAG, ex);
                }
            }
        };
        LocalBroadcastManager.getInstance(MyApplication.getContext()).registerReceiver(br,
                new IntentFilter(ERROR_REPORTING_FILE_STATE_CHANGED_EVENT_ACTION));
        synchronized (loggingSynchronisationObject) {
            File errorReportingFile = JobCacheManager.getErrorReportingFile();

            handler.onErrorReportingFileStateChanged(errorReportingFile.exists());
        }
        return br;
    }

    /**
     * Register the broadcast receiver for the error reporting file state
     * changed event
     * 
     * @param TAG
     * @param handler
     * @param broadcastReceiverRegisterHandler
     * @return
     */
    public static void registerOnErrorReportingFileStateChangedBroadcastReceiver(
            final String TAG, final OnErrorReportingFileStateChangedListener handler,
            final BroadcastReceiverRegisterHandler broadcastReceiverRegisterHandler) {
        broadcastReceiverRegisterHandler
                .addRegisteredLocalReceiver(getAndRegisterOnErrorReportingFileStateChangedBroadcastReceiver(
                        TAG, handler));
    }

    /**
     * Send error reporting file state changed broadcast
     */
    public static void sendErrorReportingFileStateChangedBroadcast(boolean exists) {
        Intent intent = new Intent(ERROR_REPORTING_FILE_STATE_CHANGED_EVENT_ACTION);
        intent.putExtra(ERROR_REPORTING_FILE_STATE, exists);
        LocalBroadcastManager.getInstance(MyApplication.getContext()).sendBroadcast(intent);
    }

    /*
     * Do everything we can to make sure the log file is created and ready to be
     * written to.
     */
    public static void ensureLogFileIsPresent()
    {
        if (logFile == null || logFile.exists() == false)
        {
            // the date format used to generate log file name
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            // the log new log file name
            String logFileName = df.format(new Date(System.currentTimeMillis())) + ".log";
            logFile = new File(JobCacheManager.getLogDir(), logFileName);
            writeLogHeader();
        }
    }

    /**
     * Write the header information to the log file
     */
    private static void writeLogHeader() {
        BufferedWriter bos = null;
        try {
            Context context = MyApplication.getContext();
            Settings settings = new Settings(context);
            bos = new BufferedWriter(new FileWriter(logFile, true));
            bos.write(CommonUtils.format(
                    "Store URL: %1$s\n"
                        + "User ID: %2$s\n"
                        + "App version: %3$s\n"
                        + "Android version: %4$s\n"
                        + "Phone info: %5$s\n",
                    settings.getUrl(),
                    settings.getUser(),
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName,
                    Build.VERSION.RELEASE, getDeviceName()));
        } catch (Exception e) {
            android.util.Log.e(TAG, null, e);
            TrackerUtils.trackThrowable(e);
        } finally {
            if (bos != null) {
                try {
                    bos.flush();
                    bos.close();
                } catch (Exception e) {
                    android.util.Log.e(TAG, null, e);
                    TrackerUtils.trackThrowable(e);
                }
            }
        }
    }

    private static String getTimeStamp() {
        long milis = System.currentTimeMillis();
        String timestamp = DateFormat.format("yyyy-MM-dd hh:mm:ss.", new Date(milis)).toString()
                + milis % 1000;
        return timestamp;
    }

    /* Save the name of the current log file in the error reporting file. */
    private static void saveErrorReportingEntry()
    {
        ensureLogFileIsPresent();

        File errorReportingFile = JobCacheManager.getErrorReportingFile();


        try {
            BufferedWriter bos = new BufferedWriter(new FileWriter(errorReportingFile, true));
            bos.write(logFile.getName() + "\n");
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendErrorReportingFileStateChangedBroadcast(errorReportingFile.exists());
    }

    public static void removeErrorReports()
    {
        synchronized (Log.loggingSynchronisationObject)
        {
            JobCacheManager.getErrorReportingFile().delete();

            sendErrorReportingFileStateChangedBroadcast(false);
        }
    }

    public static void logUncaughtException(Throwable exception) {
        synchronized (loggingSynchronisationObject)
        {
            ensureLogFileIsPresent();

            try {
                String stacktrace = getStackTrace(exception);

                BufferedWriter bos = new BufferedWriter(new FileWriter(logFile, true));
                bos.write("\n====>> UNCAUGHT EXCEPTION\n");
                bos.write("====>> " + getTimeStamp() + "\n");
                bos.write(stacktrace);
                bos.write("=========================\n\n");
                bos.flush();
                bos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            saveErrorReportingEntry();
        }
    }

    public static void logCaughtException(Throwable exception) {
        String stacktrace = null;
        boolean doReport = true;
        try {
            stacktrace = getStackTrace(exception);
            String[] safeErrors = MyApplication.getContext().getResources()
                    .getStringArray(R.array.safe_errors);
            for (String safeError : safeErrors) {
                if (stacktrace.contains(safeError)) {
                    CommonUtils.debug(TAG,
                            "logCaughtException: found safe error with matching %1$s", safeError);
                    doReport = false;
                    break;
                }
            }
        } catch (Exception ex) {
            android.util.Log.e(TAG, null, ex);
        }
        logCaughtException(exception, stacktrace, doReport);
    }

    public static void logCaughtException(Throwable exception, boolean doReport) {
        logCaughtException(exception, null, doReport);
    }

    public static void logCaughtException(Throwable exception, String stacktrace, boolean doReport) {
        synchronized (loggingSynchronisationObject)
        {
            exception.printStackTrace();

            ensureLogFileIsPresent();

            try {
                if (stacktrace == null) {
                    stacktrace = getStackTrace(exception);
                }

                BufferedWriter bos = new BufferedWriter(new FileWriter(logFile, true));
                bos.write("\n====>> CAUGHT EXCEPTION\n");
                bos.write("====>> " + getTimeStamp() + "\n");
                bos.write(stacktrace);
                bos.write("=========================\n\n");
                bos.flush();
                bos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (doReport)
            {
                saveErrorReportingEntry();
            }
        }
    }

    public static String getStackTrace(Throwable exception) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        exception.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();
        return stacktrace;
    }

    public static void log(String tag, String string) {
        synchronized (loggingSynchronisationObject)
        {
            ensureLogFileIsPresent();

            try {
                BufferedWriter bos = new BufferedWriter(new FileWriter(logFile, true));
                bos.write("====>> " + getTimeStamp() + "\n");
                bos.write(tag + "\n" + string + "\n");
                bos.flush();
                bos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param tag
     * @param string
     * @deprecated use
     *             {@link CommonUtils#debug(String, boolean, String, Object...)}
     *             instead of
     */
    public static void d(String tag, String string) {
        android.util.Log.d(tag, string);
        log(tag, string);
    }

    /**
     * @param tag
     * @param string
     * @deprecated use {@link CommonUtils#verbose(String, String, Object...)}
     *             instead of
     */
    public static void v(String tag, String string) {
        android.util.Log.d(tag, string);
        log(tag, string);
    }

    /**
     * @param tag
     * @param string
     * @deprecated use {@link CommonUtils#warn(String, String, Object...)}
     *             instead of
     */
    public static void w(String tag, String string) {
        android.util.Log.w(tag, string);
        log(tag, string);
    }

    /**
     * @param tag
     * @param string
     * @deprecated use {@link CommonUtils#error(String, String)} instead of
     */
    public static void e(String tag, String string) {
        android.util.Log.e(tag, string);
        log(tag, string);
    }

    /**
     * The method which should be called externally when the active profile is
     * changed to nullify the logFile reference so the new file with the new
     * header will be created on the next write to log file attempt
     */
    public static void profileChanged() {
        logFile = null;
    }

    /**
     * Get the device name. <br>
     * Taken from http://stackoverflow.com/a/12707479/527759
     * 
     * @return
     */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    /**
     * Capitalize the first character in the string if necessary <br>
     * Taken from http://stackoverflow.com/a/12707479/527759
     * 
     * @param s
     * @return
     */
    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}
