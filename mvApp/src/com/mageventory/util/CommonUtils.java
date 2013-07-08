package com.mageventory.util;

import java.util.Locale;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.mageventory.BuildConfig;
import com.mageventory.MyApplication;
import com.mageventory.R;

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

	/**
	 * Check whether the device is connected to any network
	 * 
	 * @return true if device is connected to any network, otherwise return
	 *         false
	 */
	public static boolean isOnline()
	{
	    return isOnline(MyApplication.getContext());
	}
    /**
     * Check whether the device is connected to any network
     * 
     * @param context
     * @return true if device is connected to any network, otherwise return
     *         false
     */
    public static boolean isOnline(
            Context context)
    {
        boolean result = false;
        try
        {
            ConnectivityManager cm =
                    (ConnectivityManager) context
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnectedOrConnecting())
            {
                result = true;
            }
        } catch (Exception ex)
        {
            GuiUtils.noAlertError(TAG, "Error", ex);
        }
        return result;
    }
    /**
     * Checks whether network connection is available. Otherwise shows warning
     * message
     * 
     * @return
     */
    public static boolean checkOnline()
    {
        return checkOnline(false);
    }

    /**
     * Checks whether network connection is available. Otherwise shows warning
     * message
     * 
     * @param silent whether or not to do not show message in case check failure
     * @return
     */
    public static boolean checkOnline(boolean silent)
    {
        boolean result = isOnline(MyApplication.getContext());
        if (!result && !silent)
        {
            GuiUtils.alert(R.string.no_internet_access);
        }
        return result;
    }

    public static boolean checkLoggedInAndOnline(boolean b) {
        // TODO Auto-generated method stub
        return true;
    }
}
