package com.mageventory.util;

import android.text.TextUtils;

import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;

/**
 * Utility class to help format prices, total amounts etc
 * 
 * @author Eugene Popovich
 */
public class CurrencyUtils {

    /**
     * Get the currency format used by application
     * 
     * @param settings the application settings
     * @return
     */
    public static String getCurrencyFormat(Settings settings) {
        return settings.getCurrencyFormat();
    }

    /**
     * Get the currency format used by application
     * 
     * @param settingsSnapshot the application settings snapshot
     * @return
     */
    public static String getCurrencyFormat(SettingsSnapshot settingsSnapshot) {
        return settingsSnapshot.getCurrencyFormat();
    }

    /**
     * Append currency sign to the price string if not empty using application
     * currency format
     * 
     * @param settings the application settings
     * @param price the price string to append currency sign to
     * @return formatted price string with the currency sign
     */
    public static String appendCurrencySignToPriceIfNotEmpty(String price, Settings settings) {
        return appendCurrencySignToPriceIfNotEmpty(price, getCurrencyFormat(settings));
    }

    /**
     * Append currency sign to the price string if not empty using application
     * currency format
     * 
     * @param settingsSnapshot the application settings snapshot
     * @param price the price string to append currency sign to
     * @return formatted price string with the currency sign
     */
    public static String appendCurrencySignToPriceIfNotEmpty(String price, SettingsSnapshot settings) {
        return appendCurrencySignToPriceIfNotEmpty(price, getCurrencyFormat(settings));
    }

    /**
     * Append currency sign to the price string if not empty using specified
     * currency format
     * 
     * @param price the price string to append currency sign to
     * @param currencyFormat predefined currency format to format price with
     * @return formatted price string with the currency sign
     */
    public static String appendCurrencySignToPriceIfNotEmpty(String price, String currencyFormat) {
        if (TextUtils.isEmpty(price)) {
            return price;
        }
        return currencyFormat.replace("{0}", price);
    }

    /**
     * Format the price keeping fractional digits information and appending $ at
     * the beginning.
     * 
     * @param price the numerical price value to format
     * @param settings the application settings
     * @return
     */
    public static String formatPrice(Number price, Settings settings) {
        return appendCurrencySignToPriceIfNotEmpty(CommonUtils.formatNumberIfNotNull(price), settings);
    }

    /**
     * Format the price keeping fractional digits information and appending
     * currency sign using application currency format
     * 
     * @param price the numerical price value to format
     * @param settingsSnapshot the application settings snapshot
     * @return
     */
    public static String formatPrice(Number price, SettingsSnapshot settings) {
        return appendCurrencySignToPriceIfNotEmpty(CommonUtils.formatNumberIfNotNull(price), settings);
    }

}
