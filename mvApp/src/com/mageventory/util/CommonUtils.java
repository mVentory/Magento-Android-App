
package com.mageventory.util;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
     * Decimal only format with no fraction digits
     */
    private static NumberFormat decimalFormat;
    static {
        decimalFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);
        decimalFormat.setMinimumFractionDigits(0);
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
    }
    /**
     * Decimal format with minimum 1 fractional digit and maximum 3
     */
    private static NumberFormat fractionalFormatWithRoundUpAndMinimum1FractionalDigit;
    static {
        fractionalFormatWithRoundUpAndMinimum1FractionalDigit = NumberFormat
                .getNumberInstance(Locale.ENGLISH);
        fractionalFormatWithRoundUpAndMinimum1FractionalDigit.setGroupingUsed(false);
        fractionalFormatWithRoundUpAndMinimum1FractionalDigit.setMinimumFractionDigits(1);
        fractionalFormatWithRoundUpAndMinimum1FractionalDigit.setMaximumFractionDigits(3);
        fractionalFormatWithRoundUpAndMinimum1FractionalDigit.setRoundingMode(RoundingMode.HALF_UP);
    }
    /**
     * Default decimal format
     */
    private static NumberFormat fractionalFormat;
    static {
        fractionalFormat = NumberFormat
                .getNumberInstance(Locale.ENGLISH);
        fractionalFormat.setGroupingUsed(false);
    }

    /**
     * The default date time format
     */
    final static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * The default date format
     */
    final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

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
     * Format the number with no fraction digits information
     * 
     * @param number
     * @return
     */
    public static String formatDecimalOnlyWithRoundUp(Number number) {
        return decimalFormat.format(number);
    }

    /**
     * Format the number keeping fractional digits information. Minimum is 1
     * fractional digita and maximum is 3
     * 
     * @param number
     * @return
     */
    public static String formatNumberWithFractionWithRoundUp(Number number) {
        return fractionalFormatWithRoundUpAndMinimum1FractionalDigit.format(number);
    }

    /**
     * Format the number keeping fractional digits information.
     * 
     * @param number
     * @return
     */
    public static String formatNumber(Number number) {
        return fractionalFormat.format(number);
    }

    /**
     * Format the number keeping fractional digits information.
     * 
     * @param number
     * @return
     */
    public static String formatNumberIfNotNull(Number number) {
        return formatNumberIfNotNull(number, null);
    }

    /**
     * Format the number keeping fractional digits information.
     * 
     * @param number
     * @param defaultValue
     * @return
     */
    public static String formatNumberIfNotNull(Number number, String defaultValue) {
        return number == null ? defaultValue : formatNumber(number);
    }

    /**
     * Parse number
     * 
     * @param str
     * @return null in case of ParseException occurs
     */
    public static Double parseNumber(String str) {
        try {
            return fractionalFormat.parse(str).doubleValue();
        } catch (ParseException ex) {
            GuiUtils.noAlertError(TAG, ex);
        }
        return null;
    }

    /**
     * Format the price keeping fractional digits information and appending $ at
     * the beginning.
     * 
     * @param price
     * @return
     */
    public static String formatPrice(Number price) {
        return "$" + fractionalFormat.format(price);
    }

    /**
     * Parse date/time
     * 
     * @param str
     * @return null in case of ParseException occurs
     */
    public static Date parseDateTime(String str) {
        try {
            return dateTimeFormat.parse(str);
        } catch (ParseException ex) {
            GuiUtils.noAlertError(TAG, ex);
        }
        return null;
    }

    /**
     * Format the date time with the default formatter
     * 
     * @param date
     * @return
     */
    public static String formatDateTime(Date date) {
        return dateTimeFormat.format(date);
    }

    /**
     * Format the date time with the default formatter if date is not null
     * 
     * @param date
     * @return formatted string contains date time information. In case date is
     *         null returns null
     */
    public static String formatDateTimeIfNotNull(Date date) {
        return formatDateTimeIfNotNull(date, null);
    }

    /**
     * Format the date time with the default formatter if date is not null
     * 
     * @param date
     * @param defaultValue to return in case date is null
     * @return
     */
    public static String formatDateTimeIfNotNull(Date date, String defaultValue) {
        return date == null ? defaultValue : formatDateTime(date);
    }

    /**
     * Parse date
     * 
     * @param str
     * @return null in case of ParseException occurs
     */
    public static Date parseDate(String str) {
        try {
            return dateFormat.parse(str);
        } catch (ParseException ex) {
            GuiUtils.noAlertError(TAG, ex);
        }
        return null;
    }

    /**
     * Format the date with the default formatter if date is not null
     * 
     * @param date
     * @return
     */
    public static String formatDate(Date date) {
        return dateFormat.format(date);
    }

    /**
     * Format the date with the default formatter if date is not null
     * 
     * @param date
     * @return formatted string contains date information. In case date is null
     *         returns null
     */
    public static String formatDateIfNotNull(Date date) {
        return formatDateIfNotNull(date, null);
    }

    /**
     * Format the date with the default formatter if date is not null
     * 
     * @param date
     * @param defaultValue to return in case date parameter is null
     * @return
     */
    public static String formatDateIfNotNull(Date date, String defaultValue) {
        return date == null ? defaultValue : formatDate(date);
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

    /**
     * Checks whether the running platform version is 4.1 or higher
     * 
     * @return
     */
    public static boolean isJellyBeanOrHigher() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN;
    }
}
