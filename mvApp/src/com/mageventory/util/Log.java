
package com.mageventory.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;

import android.os.Environment;
import android.text.format.DateFormat;

import com.mageventory.MyApplication;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobService.OnJobServiceStateChangedListener;

public class Log {

    public static File logFile;
    public static Object loggingSynchronisationObject = new Object();

    public static interface OnErrorReportingFileStateChangedListener
    {
        void onErrorReportingFileStateChanged(boolean fileExists);
    }

    private static OnErrorReportingFileStateChangedListener sOnJobServiceStateChangedListener;

    public static void registerOnErrorReportingFileStateChangedListener(
            OnErrorReportingFileStateChangedListener listener)
    {
        sOnJobServiceStateChangedListener = listener;

        synchronized (loggingSynchronisationObject)
        {
            File errorReportingFile = JobCacheManager.getErrorReportingFile();

            listener.onErrorReportingFileStateChanged(errorReportingFile.exists());
        }
    }

    public static void deregisterOnErrorReportingFileStateChangedListener()
    {
        sOnJobServiceStateChangedListener = null;
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

        OnErrorReportingFileStateChangedListener listener = sOnJobServiceStateChangedListener;
        File errorReportingFile = JobCacheManager.getErrorReportingFile();

        if (listener != null)
        {
            listener.onErrorReportingFileStateChanged(errorReportingFile.exists());
        }

        try {
            BufferedWriter bos = new BufferedWriter(new FileWriter(errorReportingFile, true));
            bos.write(logFile.getName() + "\n");
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeErrorReports()
    {
        synchronized (Log.loggingSynchronisationObject)
        {
            JobCacheManager.getErrorReportingFile().delete();

            OnErrorReportingFileStateChangedListener listener = sOnJobServiceStateChangedListener;

            if (listener != null)
            {
                listener.onErrorReportingFileStateChanged(false);
            }
        }
    }

    public static void logUncaughtException(Throwable exception) {
        synchronized (loggingSynchronisationObject)
        {
            ensureLogFileIsPresent();

            try {
                final Writer result = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(result);
                exception.printStackTrace(printWriter);
                String stacktrace = result.toString();
                printWriter.close();

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
        logCaughtException(exception, true);
    }

    public static void logCaughtException(Throwable exception, boolean doReport) {
        synchronized (loggingSynchronisationObject)
        {
            exception.printStackTrace();

            ensureLogFileIsPresent();

            try {
                final Writer result = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(result);
                exception.printStackTrace(printWriter);
                String stacktrace = result.toString();
                printWriter.close();

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
