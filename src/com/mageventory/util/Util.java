package com.mageventory.util;

import java.util.Formatter;

import android.util.Log;

public class Util {

    private static Formatter formatter;

    public static String currentThreadName() {
        return Thread.currentThread().getName();
    }

    private static Formatter getFormatter() {
        if (formatter == null) {
            formatter = new Formatter();
        }
        return formatter;
    }

    public static void logThread(String tag, String log, Object... args) {
        if (args != null && args.length > 0) {
            log = getFormatter().format(log, args).toString();
        }
        Log.d(tag, "currentThread=" + currentThreadName() + ",log=" + log);
    }

}
