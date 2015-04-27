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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import com.mageventory.MyApplication;
import com.mageventory.util.run.CallableWithParameterAndResult;
import com.mventory.BuildConfig;
import com.mventory.R;

/**
 * Contains various common utils methods
 * 
 * @author Eugene Popovich
 */
public class CommonUtils {
    public static final String TAG = CommonUtils.class.getSimpleName();
    /**
     * The pattern to check whether the word contains at least one alphanumeric
     * character
     */
    public static final String VALID_WORD_PATTERN = ".*\\w+.*";
    /**
     * Word delimiters characters used for regular expressings
     */
    public static final String WORDS_DELIMITERS = "[!?,\\.\\s]";
    /**
     * Word delimiters pattern
     */
    public static final Pattern WORDS_DELIMITER_PATTERN = Pattern.compile(WORDS_DELIMITERS);
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
     * Decimal format with 1 fractional digit 
     */
    private static NumberFormat fractionalFormatWithRoundUpAnd1FractionalDigit;
    static {
        fractionalFormatWithRoundUpAnd1FractionalDigit = NumberFormat
                .getNumberInstance(Locale.ENGLISH);
        fractionalFormatWithRoundUpAnd1FractionalDigit.setGroupingUsed(false);
        fractionalFormatWithRoundUpAnd1FractionalDigit.setMinimumFractionDigits(1);
        fractionalFormatWithRoundUpAnd1FractionalDigit.setMaximumFractionDigits(1);
        fractionalFormatWithRoundUpAnd1FractionalDigit.setRoundingMode(RoundingMode.HALF_UP);
    }
    /**
     * Default decimal format
     */
    private static NumberFormat fractionalFormat;
    static {
        fractionalFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);
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
     * Get color resource by id
     * 
     * @param resourceId
     * @return
     */
    public static int getColorResource(int resourceId) {
        return MyApplication.getContext().getResources().getColor(resourceId);
    }

    /**
     * Write message to the debug log
     * 
     * @param TAG
     * @param message
     * @param params
     */
    public static void debug(String TAG, String message, Object... params) {
        debug(TAG, false, message, params);
    }

    /**
     * Write message to the debug log
     * 
     * @param TAG
     * @param writeToLog
     * @param message
     * @param params
     */
    public static void debug(String TAG, boolean writeToLog, String message, Object... params) {
        try {
            if (writeToLog || BuildConfig.DEBUG) {
                // if information should be written to the log file or it is a
                // debug app version
                if (params != null && params.length > 0) {
                    message = format(message, params);
                }
                if (BuildConfig.DEBUG) {
                    // if app is debugging
                    Log.d(TAG, message);
                }
                if (writeToLog) {
                    com.mageventory.util.Log.log(TAG + ".DEBUG", message);
                }
            }
        } catch (Exception ex) {
            GuiUtils.noAlertError(TAG, ex);
        }
    }

    /**
     * Write message to the warning log
     * 
     * @param TAG
     * @param message
     * @param params
     */
    public static void warn(String TAG, String message, Object... params) {
        try {
            if (params != null && params.length > 0) {
                message = format(message, params);
            }
            Log.w(TAG, message);
            com.mageventory.util.Log.log(TAG + ".WARN", message);
            TrackerUtils.trackWarnEvent(TAG, message);
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
    public static void verbose(String TAG, String message, Object... params) {
        try {
            if (BuildConfig.DEBUG) {
                if (params == null || params.length == 0) {
                    Log.v(TAG, message);
                } else {
                    Log.v(TAG, format(message, params));
                }
            }
        } catch (Exception ex) {
            GuiUtils.noAlertError(TAG, ex);
        }
    }

    /**
     * Write message to the error log
     * 
     * @param TAG
     * @param message
     */
    public static void error(String TAG, String message) {
        error(TAG, message, null);
    }

    /**
     * Write exception to the error log
     * 
     * @param TAG
     * @param tr
     */
    public static void error(String TAG, Throwable tr) {
        error(TAG, null, tr);
    }

    /**
     * Write message to the error log
     * 
     * @param TAG
     * @param message
     * @param tr
     */
    public static void error(String TAG, String message, Throwable tr) {
        Log.e(TAG, message, tr);
        if (!TextUtils.isEmpty(message)) {
            com.mageventory.util.Log.log(TAG + ".ERROR", message);
            TrackerUtils.trackErrorEvent(TAG, message);
        }
        if (tr != null) {
            com.mageventory.util.Log.logCaughtException(tr);
            TrackerUtils.trackThrowable(tr);
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
     * Format the number keeping 1 fractional digit.
     * 
     * @param number
     * @return
     */
    public static String formatNumberWithFractionWithRoundUp1(Number number) {
        return fractionalFormatWithRoundUpAnd1FractionalDigit.format(number);
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
     * @param str the string to parse
     * @return null in case of ParseException occurs or null parameter passed
     */
    public static Double parseNumber(String str) {
        return parseNumber(str, null);
    }

    /**
     * Parse number
     * 
     * @param str the string to parse
     * @param defaultValue the defaultValue to return if parse problem occurs
     * @return defaultValue in case of ParseException occurs or null parameter
     *         passed
     */
    public static Double parseNumber(String str, Double defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return fractionalFormat.parse(str).doubleValue();
        } catch (ParseException ex) {
            GuiUtils.noAlertError(TAG, ex);
        }
        return defaultValue;
    }

    /**
     * Get the currency sign used by application
     * 
     * @return
     */
    public static String getCurrencySign() {
        return "$";
    }

    /**
     * Append currency sign to the price string if not empty
     * 
     * @param price
     * @return concatenated price string with the currency sign
     */
    public static String appendCurrencySignToPriceIfNotEmpty(String price) {
        if (TextUtils.isEmpty(price)) {
            return price;
        }
        return getCurrencySign() + price;
    }
    
    /**
     * Format the price keeping fractional digits information and appending $ at
     * the beginning.
     * 
     * @param price
     * @return
     */
    public static String formatPrice(Number price) {
        return appendCurrencySignToPriceIfNotEmpty(formatNumberIfNotNull(price));
    }

    /**
     * Parse date/time
     * 
     * @param str
     * @return null in case of ParseException occurs  or null parameter passed
     */
    public static Date parseDateTime(String str) {
        if (str == null) {
            return null;
        }
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
     * @return null in case of ParseException occurs or null parameter passed
     */
    public static Date parseDate(String str) {
        if (str == null) {
            return null;
        }
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
    public static boolean isOnline() {
        return isOnline(MyApplication.getContext());
    }

    /**
     * Check whether the device is connected to any network
     * 
     * @param context
     * @return true if device is connected to any network, otherwise return
     *         false
     */
    public static boolean isOnline(Context context) {
        boolean result = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                result = true;
            }
        } catch (Exception ex) {
            GuiUtils.noAlertError(TAG, "Error", ex);
        }
        return result;
    }

    /**
     * Check whether the device has enabled connectivity services
     * 
     * @return true if device has enabled any network (independently on
     *         connection state), otherwise returns false
     */
    public static boolean isInternetEnabled() {
        boolean result = false;
        try {
            Context context = MyApplication.getContext();
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            result = wifiManager != null && wifiManager.isWifiEnabled();
            if (!result) {
                TelephonyManager telephonyManager = (TelephonyManager) context
                        .getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager != null
                        && telephonyManager.getDataState() != TelephonyManager.DATA_DISCONNECTED) {
                    result = true;
                }
            }
        } catch (Exception ex) {
            GuiUtils.noAlertError(TAG, ex);
        }
        return result;
    }

    /**
     * Checks whether network connection is available. Otherwise shows warning
     * message
     * 
     * @return
     */
    public static boolean checkOnline() {
        return checkOnline(false);
    }

    /**
     * Checks whether network connection is available. Otherwise shows warning
     * message
     * 
     * @param silent whether or not to do not show message in case check failure
     * @return
     */
    public static boolean checkOnline(boolean silent) {
        boolean result = isOnline(MyApplication.getContext());
        if (!result && !silent) {
            GuiUtils.alert(R.string.no_internet_access);
        }
        return result;
    }

    public static boolean checkLoggedInAndOnline(boolean b) {
        // TODO Auto-generated method stub
        return true;
    }

    /**
     * Checks whether the running platform version is 4.4 or higher
     * 
     * @return
     */
    public static boolean isKitKatOrHigher() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT;
    }

    /**
     * Checks whether the running platform version is 4.1 or higher
     * 
     * @return
     */
    public static boolean isJellyBeanOrHigher() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN;
    }

    /**
     * Checks whether the running platform version is 3.0 or higher
     * 
     * @return
     */
    public static boolean isHoneyCombOrHigher() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
    }

    /**
     * Returns possible external sd card path. Solution taken from here
     * http://stackoverflow.com/a/13648873/527759
     * 
     * @return
     */
    public static Set<String> getExternalMounts() {
        final Set<String> out = new HashSet<String>();
        try {
            String reg = ".*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
            StringBuilder sb = new StringBuilder();
            try {
                final Process process = new ProcessBuilder().command("mount")
                        .redirectErrorStream(true).start();
                process.waitFor();
                final InputStream is = process.getInputStream();
                final byte[] buffer = new byte[1024];
                while (is.read(buffer) != -1) {
                    sb.append(new String(buffer));
                }
                is.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
            // parse output
            final String[] lines = sb.toString().split("\n");
            for (String line : lines) {
                if (!line.toLowerCase(Locale.ENGLISH).contains("asec")) {
                    if (line.matches(reg)) {
                        String[] parts = line.split(" ");
                        for (String part : parts) {
                            if (part.startsWith("/"))
                                if (!part.toLowerCase(Locale.ENGLISH).contains("vold")) {
                                    CommonUtils.debug(TAG, "Found path: " + part);
                                    out.add(part);
                                }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            error(TAG, null, ex);
        }
        return out;
    }

    /**
     * Convert device independent pixels value to the regular pixels value. The
     * result value is based on device density
     * 
     * @param dipValue
     * @return
     */
    public static float dipToPixels(float dipValue) {
        DisplayMetrics metrics = MyApplication.getContext().getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    /**
     * Load the assets resource as String data
     * 
     * @param path the path within assets folder
     * @return
     * @throws IOException
     */
    public static String loadAssetAsString(String path) throws IOException {
        return WebUtils.convertStreamToString(MyApplication.getContext().getAssets().open(path));
    }

    /**
     * Get the list of unique (ignore case) words which are present in the
     * passed string parameter
     * 
     * @param str
     * @param filterInvalidWords whether the invalid words which doesn't match
     *            {@link #VALID_WORD_PATTERN} should be filtered
     * @return the list of words
     */
    public static List<String> getUniqueWords(String str, boolean filterInvalidWords) {
        // unique words list
        List<String> wordsList = new ArrayList<String>();
        if (!TextUtils.isEmpty(str)) {
            Set<String> processedWords = new HashSet<String>();
            // get all the words from the string 
            String[] words = CommonUtils.splitToWords(str);
            for (String word : words) {
                if (filterInvalidWords && !word.matches(VALID_WORD_PATTERN)) {
                    // skip invalid word
                    continue;
                }
                // get the lower case version of word
                String lcWord = word.toLowerCase();
                if (processedWords.contains(lcWord)) {
                    // the same word was already found before, continue the loop
                    // to next iteration
                    continue;
                }
                // add word to resulting list
                wordsList.add(word);
                // mark word as processed for future checks
                processedWords.add(lcWord);
            }

        }
        return wordsList;
    }
    /**
     * Split the string to words using {@link #WORDS_DELIMITERS}
     * 
     * @param str the string to split to words
     * @return array of words present in the str
     */
    public static String[] splitToWords(String str) {
        return str == null ? null : str.split(WORDS_DELIMITERS + "+");
    }

    /**
     * Remove duplicate words occurrences in the string
     * 
     * @param str the string to filter from duplicating words
     * @return the string wihout words duplicates
     */
    public static String removeDuplicateWords(String str) {
        if (str == null) {
            return str;
        }
        // lowercase string for easier matches search
        String lowerCaseString = str.toLowerCase();
        // words in lowercase present in the string
        String[] words = splitToWords(lowerCaseString);
        // collection of the words which occur more than once in the string
        Set<String> duplicateWords = new HashSet<String>();
        // already processed words
        Set<String> processedWords = new HashSet<String>();
        // iterate through words and search all words which occur more than once
        for (String word : words) {
            if (!duplicateWords.contains(word)) {
                if (processedWords.contains(word)) {
                    // if word was processed before it is duplicate
                    duplicateWords.add(word);
                } else {
                    // mark word as procesed
                    processedWords.add(word);
                }
            }
        }
        // iterate through duplicate words and remove each occurrence except
        // first one
        for (String word : duplicateWords) {
            // it is not the best approach to remove duplicate words. Perhaps
            // there is some regular expression for that.
            // TODO search such regular expression

            // search teh first occurrence of the word in the lower case string
            int p = lowerCaseString.indexOf(word);
            // replace all second occurrences of the word in the lower case
            // string
            lowerCaseString = replaceSecondOccurrences(lowerCaseString, word, p);
            // replace all second occurrences of the word in the original string
            str = replaceSecondOccurrences(str, word, p);
        }

        return str;
    }

    /**
     * Replace second occurrence of the word in the string
     * 
     * @param str the string to remove duplicate words
     * @param word the word to removed duplicates of
     * @param p the position of the first occurrence of the word in the string
     * @return
     */
    private static String replaceSecondOccurrences(String str, String word, int p) {
        return str.substring(0, p + word.length()) // the substring of the
                                                   // original string since
                                                   // start to the end of the
                                                   // first word occurrence
                + str.substring(p + word.length()) // rest of the string
                .replaceAll(
                        "(?i)" // ignore case
                        + WORDS_DELIMITERS + "+" // at least one word
                                                 // delimiter at the
                                                 // start
                        + word // the word itself
                        + "(?=$|" + WORDS_DELIMITERS + ")" // the end of
                                                           // string or
                                                           // some word
                                                           // delimiter
                                                           // in the end
                                                           // of the
                                                           // word
                        ,"");
    }

    /**
     * Check whether the collection is null or empty
     * 
     * @param c the collection to check
     * @return true if either collection is null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(final Collection<?> c) {
        return c == null || c.isEmpty();
    }

    /**
     * Replace all found words(codes) with the corresponding values for them
     * retrieved from the getValueForCode callable
     * 
     * @param str the initial string
     * @param getValueForCode the transformer from the code words into values
     * @return string where all the words replaced with the values given by the
     *         getValueForCode callable
     */
    public static String replaceCodesWithValues(String str,
            CallableWithParameterAndResult<String, String> getValueForCode) {
        Pattern p = Pattern.compile(
                "(?<=^|\\W)" // preceding string start or any non word character
                + "(\\w+)"   // the word itself
                + "(?=$|\\W)"// trailing string end or any non word character
                , Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            CommonUtils.verbose(TAG, "replaceCodesWithValues: word %1$s", m.group(1));
            // replace found word with the value for the word
            m.appendReplacement(sb, getValueForCode.call(m.group(1)));
        }
        m.appendTail(sb);

        return sb.toString();
    }

    /**
     * Encode string so it will be possible to use it in URLs
     * 
     * @param s the string to encode
     * @return the URL encoded string with the UTF-8 encoding
     */
    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.wtf(TAG, "UTF-8 should always be supported", e);
            throw new RuntimeException("URLEncoder.encode() failed for " + s);
        }
    }

    /**
     * Filter the intent to use specific application with the defined package
     * prefix if exists
     * 
     * @param context the context
     * @param intent the intent to set package for
     * @param prefix the application package prefix which should be used to
     *            launch the intent
     * @return true if application with the defined package prefix is found and
     *         specified to the intent, false otherwise
     */
    public static boolean filterByPackageName(Context context, Intent intent, String prefix) {
        List<ResolveInfo> matches = context.getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo info : matches) {
            if (info.activityInfo.packageName.toLowerCase().startsWith(prefix)) {
                intent.setPackage(info.activityInfo.packageName);
                return true;
            }
        }
        return false;
    }
}
