
package com.mageventory.model.util;

import java.util.Date;
import java.util.regex.Pattern;

import android.text.TextUtils;

import com.mageventory.model.Product;
import com.mageventory.util.CommonUtils;

/**
 * Various utils for {@link Product}
 * 
 * @author Eugene Popovich
 */
public class ProductUtils {

    public static final String PRODUCT_PRICES_SEPARATOR = "/";
    private static Pattern pricePattern = Pattern
            .compile("^\\d*(?:\\.\\d*)?(?:\\/\\d*(?:\\.\\d*)?)?$");
    public static Pattern priceCharacterPattern = Pattern.compile("[\\d\\.\\/]*");
    private static long millisInDay = 24 * 60 * 60 * 1000;

    /**
     * Get product prices string which contains both original and special if
     * exists separated by / symbol
     * 
     * @param product
     * @return
     */
    public static String getProductPricesString(Product product) {
        StringBuilder result = new StringBuilder(product.getPrice());
        if (product.getSpecialPrice() != null) {
            result.append(PRODUCT_PRICES_SEPARATOR);
            result.append(CommonUtils.formatNumber(product.getSpecialPrice()));
        }
        return result.toString();
    }

    /**
     * Get product prices string which contains both original and special if
     * exists separated by / symbol
     * 
     * @param price
     * @param specialPrice
     * @return
     */
    public static String getProductPricesString(Double price, Double specialPrice) {
        StringBuilder result = new StringBuilder();
        if (price != null) {
            result.append(CommonUtils.formatNumber(price));
        }
        if (specialPrice != null) {
            result.append(PRODUCT_PRICES_SEPARATOR);
            result.append(CommonUtils.formatNumber(specialPrice));
        }
        return result.toString();
    }

    /**
     * Check whether the prices string has valid format
     * 
     * @param prices
     * @return
     */
    public static boolean isValidPricesString(String prices) {
        return pricePattern.matcher(prices).matches();
    }

    /**
     * Check whether the price has special price separator
     * 
     * @param price
     * @return
     */
    public static boolean hasSpecialPrice(String price) {
        return price != null && price.indexOf(ProductUtils.PRODUCT_PRICES_SEPARATOR) != -1;
    }
    /**
     * Check whether the special price for product is active now. Whether the
     * current date is between special from date and special to date
     * 
     * @param p
     * @return
     */
    public static boolean isSpecialPriceActive(Product p) {
        if (p.getSpecialPrice() == null) {
            return false;
        }
        Date fromDate = p.getSpecialFromDate();
        Date toDate = p.getSpecialToDate();
        long now = System.currentTimeMillis();
        boolean result = true;
        if (fromDate != null && now < fromDate.getTime()) {
            result = false;
        }
        if (result && toDate != null && toDate.getTime() + millisInDay < now) {
            result = false;
        }
        return result;

    }

    /**
     * Get prices information from the formatted string which may contain 2
     * prices at the time separated by the / symbol
     * 
     * @param prices
     * @return null in case prices string has invalid format. Otherwise returns
     *         PricesInformation which contains original and special prices
     *         information. Fields may be null
     */
    public static PricesInformation getPricesInformation(String prices) {
        PricesInformation result = null;
        if (isValidPricesString(prices)) {
            String[] splitPrices = prices.split(PRODUCT_PRICES_SEPARATOR);
            Double[] parcedPrices = new Double[splitPrices.length];
            for (int i = 0, size = splitPrices.length; i < size; i++) {
                String priceString = splitPrices[i];
                if (!TextUtils.isEmpty(priceString)) {
                    parcedPrices[i] = CommonUtils.parseNumber(priceString);
                }
            }
            result = new PricesInformation();
            result.regularPrice = parcedPrices[0];
            if (parcedPrices.length > 1) {
                result.specialPrice = parcedPrices[1];
            }
        }
        return result;
    }

    /**
     * The simple result class for getPricesInformation method
     */
    public static class PricesInformation {
        public Double regularPrice;
        public Double specialPrice;
    }
}
