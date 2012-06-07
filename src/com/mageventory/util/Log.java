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

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;

public class Log {

	private static String filePath;

	static {
		File dir = new File(Environment.getExternalStorageDirectory(),
				MyApplication.APP_DIR_NAME);
		dir = new File(dir, "log");

		if (!dir.exists()) {
			dir.mkdirs();
		}

		filePath = new File(dir, "" + System.currentTimeMillis() + ".log")
				.getAbsolutePath();
	}

	private static String getTimeStamp() {
		long milis = System.currentTimeMillis();
		String timestamp = DateFormat.format("yyyy-MM-dd hh:mm:ss.",
				new Date(milis)).toString()
				+ milis % 1000;
		return timestamp;
	}

	public static void logUncaughtException(Throwable exception) {
		if (filePath == null)
			return;
		try {
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			exception.printStackTrace(printWriter);
			String stacktrace = result.toString();
			printWriter.close();

			BufferedWriter bos = new BufferedWriter(new FileWriter(filePath,
					true));
			bos.write("\n====>> UNCAUGHT EXCEPTION\n");
			bos.write("====>> " + getTimeStamp() + "\n");
			bos.write(stacktrace);
			bos.write("=========================\n\n");
			bos.flush();
			bos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void logCaughtException(Throwable exception) {
		exception.printStackTrace();

		if (filePath == null)
			return;
		try {
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			exception.printStackTrace(printWriter);
			String stacktrace = result.toString();
			printWriter.close();

			BufferedWriter bos = new BufferedWriter(new FileWriter(filePath,
					true));
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
		if (filePath == null)
			return;
		try {
			BufferedWriter bos = new BufferedWriter(new FileWriter(filePath,
					true));
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
