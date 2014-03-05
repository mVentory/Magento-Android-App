
package com.mageventory.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;

import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.job.JobCacheManager;
import com.mageventory.util.EventBusUtils.BroadcastReceiverRegisterHandler;

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
     * Get and register the broadcast receiver for the general event
     * 
     * @param TAG
     * @param handler
     * @param activity
     * @return
     */
    public static BroadcastReceiver getAndRegisterOnErrorReportingFileStateChangedBroadcastReceiver(
            final String TAG, final OnErrorReportingFileStateChangedListener handler,
            final Activity activity) {
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
        LocalBroadcastManager.getInstance(activity).registerReceiver(br,
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
     * @param activity
     * @return
     */
    public static <T extends Activity & BroadcastReceiverRegisterHandler> void registerOnErrorReportingFileStateChangedBroadcastReceiver(
            final String TAG, final OnErrorReportingFileStateChangedListener handler,
            final T activity) {
        activity.addRegisteredLocalReceiver(getAndRegisterOnErrorReportingFileStateChangedBroadcastReceiver(
                TAG,
                handler, activity));
    }

    /**
     * Send the disk cache cleared broadcast
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
            logFile = new File(JobCacheManager.getLogDir(), "" + System.currentTimeMillis()
                    + ".log");
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

    private static void log(String tag, String string) {
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

    public static void d(String tag, String string) {
        android.util.Log.d(tag, string);
        log(tag, string);
    }

    public static void v(String tag, String string) {
        android.util.Log.d(tag, string);
        log(tag, string);
    }

    public static void w(String tag, String string) {
        android.util.Log.w(tag, string);
        log(tag, string);
    }

    public static void e(String tag, String string) {
        android.util.Log.e(tag, string);
        log(tag, string);
    }

}
