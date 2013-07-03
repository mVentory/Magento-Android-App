package com.mageventory.util;

import java.util.Locale;

import android.util.Log;

import com.mageventory.BuildConfig;
import com.mageventory.MyApplication;

/**
 * Contains various common utils methods
 * 
 * @author Eugene Popovich
 */
public class CommonUtils {
	public static final String TAG = CommonUtils.class.getSimpleName();

	/**
	 * Get string resource by id
	 * 
	 * @param resourceId
	 * @return
	 */
	public static String getStringResource(int resourceId) {
		return MyApplication.getContext().getString(resourceId);
	}

	/**
	 * Get string resource by id with parameters
	 * 
	 * @param resourceId
	 * @param args
	 * @return
	 */
	public static String getStringResource(int resourceId, Object... args) {
		return MyApplication.getContext().getString(resourceId, args);
	}

	/**
	 * Write message to the debug log
	 * 
	 * @param TAG
	 * @param message
	 * @param params
	 */
	public static void debug(String TAG, String message, Object... params) {
		try {
			if (BuildConfig.DEBUG) {
				if (params == null || params.length == 0) {
					Log.d(TAG, message);
				} else {
					Log.d(TAG, format(message, params));
				}
			}
		} catch (Exception ex) {
			GuiUtils.noAlertError(TAG, ex);
		}
	}
	
    /**
     * Write message to the verbose log
     * 
     * @param TAG
     * @param message
     * @param params
     */
    public static void verbose(String TAG, String message, Object... params)
    {
        try
        {
            if (BuildConfig.DEBUG)
            {
                if (params == null || params.length == 0)
                {
                    Log.v(TAG, message);
                } else
                {
                    Log.v(TAG, format(message, params));
                }
            }
        } catch (Exception ex)
        {
            GuiUtils.noAlertError(TAG, ex);
        }
    }

    /**
     * Format string with params
     * 
     * @param message
     * @param params
     * @return
     */
	public static String format(String message, Object... params) {
		try {
			return String.format(Locale.ENGLISH, message, params);
		} catch (Exception ex) {
			GuiUtils.noAlertError(TAG, ex);
		}
		return null;
	}

    public static void checkOnline() {
        // TODO Auto-generated method stub
    }

    public static boolean checkLoggedInAndOnline(boolean b) {
        // TODO Auto-generated method stub
        return true;
    }
}
