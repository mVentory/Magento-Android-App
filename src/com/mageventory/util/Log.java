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

public class Log {

	private static File logFile;

	/* Do everything we can to make sure the log file is created and ready to be written to. */
	public static void ensureLogFileIsPresent()
	{
		if (logFile == null || logFile.exists() == false)
		{
			logFile = new File(JobCacheManager.getLogDir(), "" + System.currentTimeMillis() + ".log");			
		}
	}

	private static String getTimeStamp() {
		long milis = System.currentTimeMillis();
		String timestamp = DateFormat.format("yyyy-MM-dd hh:mm:ss.", new Date(milis)).toString() + milis % 1000;
		return timestamp;
	}

	public static void logUncaughtException(Throwable exception) {
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

		/* If something goes wrong when reporting an error then so be it. Can't do much in that case. Let's just catch all exceptions
		 * and do nothing. */
		try {
			ErrorEmailReporter.makeDBDump();
		}catch(Throwable e) {

		}
	}

	public static void logCaughtException(Throwable exception) {
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
	}

	private static void log(String tag, String string) {
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
