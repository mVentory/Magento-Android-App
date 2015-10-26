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

package com.mageventory.model.util;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.support.v4.app.FragmentActivity;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.EditText;

import com.mageventory.MageventoryConstants;
import com.mventory.R;
import com.mageventory.fragment.PriceEditFragment;
import com.mageventory.fragment.PriceEditFragment.OnEditDoneListener;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.SimpleAsyncTask;

/**
 * Various utils for {@link Product}
 * 
 * @author Eugene Popovich
 */
public class ProductUtils {
	/**
	 * The tag used for the logging
	 */
    public static final String TAG = ProductUtils.class.getSimpleName();

    public static final String PRODUCT_PRICES_SEPARATOR = "/";
    private static Pattern pricePattern = Pattern
            .compile("^\\d*(?:\\.\\d*)?(?:\\/\\d*(?:\\.\\d*)?)?$");
    private static long millisInDay = 24 * 60 * 60 * 1000;

    /**
     * Generate a SKU for the product
     * 
     * @return
     */
    public static String generateSku() {
        /*
         * Since we can't get microsecond time in java we just use milliseconds
         * time and add microsecond part from System.nanoTime() which doesn't
         * return a normal timestamp but a number of nanoseconds from some
         * arbitrary point in time which we don't know. This should be enough to
         * make every SKU we'll ever generate different.
         */
        return "P" + System.currentTimeMillis() + (System.nanoTime() / 1000) % 1000;
    }

    /**
     * Get the quantity string from the product based on the quantity and
     * isQtyDecimal product attributes values
     * 
     * @param product
     * @return
     */
    public static String getQuantityString(Product product) {
        double quantityValue = CommonUtils.parseNumber(product.getQuantity().toString());

        String result;
        result = getQuantityString(product, quantityValue);
        return result;
    }

    /**
     * Get the quantity string for the quantity based on the isQtyDecimal
     * product attribute value. Used to format quantity for one product using
     * another product information
     * 
     * @param product to get isQtyDecimal information
     * @param quantity the quantity to format
     * @return
     */
    public static String getQuantityString(Product product, double quantity) {
        return getQuantityString(product.getIsQtyDecimal() == 1, quantity);
    }

    /**
     * Get the quantity string for the quantity based on the isQtyDecimal
     * parameter.
     * 
     * @param isQtyDecimal whether the quantity is decimal or not (different
     *            formatters used)
     * @param quantity the quantity to format
     * @return
     */
    public static String getQuantityString(boolean isQtyDecimal, double quantity) {
        String result;
        if (isQtyDecimal) {
            result = CommonUtils.formatNumberWithFractionWithRoundUp(quantity);
        } else {
            result = CommonUtils.formatDecimalOnlyWithRoundUp(quantity);
        }
        return result;
    }

    /**
     * Set the quantityView text with the formatted quantity string data and
     * adjust input type based on the product isQtyDecimal attribute value
     * 
     * @param quantity the quantity to set to the quantityView field
     * @param quantityView the view to update
     * @param product to get isQtyDecimal information
     */
    public static void setQuantityTextValueAndAdjustViewType(double quantity,
            EditText quantityView, Product product) {
        setQuantityTextValueAndAdjustViewType(quantity, quantityView, product.getIsQtyDecimal() == 1);
    }

    /**
     * Set the quantityView text with the formatted quantity string data and
     * adjust input type based on the isQuantityDecimal parameter information
     * 
     * @param quantity the quantity to set to the quantityView field
     * @param quantityView the view to update
     * @param isQuantityDecimal whether the quantity is decimal or not
     *            (different formatters used)
     */
    public static void setQuantityTextValueAndAdjustViewType(double quantity,
            EditText quantityView, boolean isQuantityDecimal) {
        adjustQuantityViewInputType(quantityView, isQuantityDecimal);
        quantityView.setText(getQuantityString(isQuantityDecimal, quantity));
    }

    /**
     * Adjust quantity view input type based on the isQuantityDecimal parameter
     * information
     * 
     * @param quantityView the view to adjust the input type
     * @param isQuantityDecimal whether the quantity is decimal or not
     *            (different formatters used)
     */
    public static void adjustQuantityViewInputType(EditText quantityView, boolean isQuantityDecimal) {
        if (isQuantityDecimal) {
            quantityView.setInputType(InputType.TYPE_CLASS_NUMBER
                    | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        } else {
            quantityView.setInputType(InputType.TYPE_CLASS_NUMBER
                    | InputType.TYPE_NUMBER_FLAG_SIGNED);
        }
    }

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
     * Remove sequential duplicate words from the name. Example
     * "Black dress dress" will return "Black dress"
     * 
     * @param name
     * @return
     */
    public static String removeDuplicateWordsFromName(String name) {
        Pattern p = Pattern.compile("\\b(\\w+)\\b\\s+\\b\\1\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(name);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, m.group(1));
        }
        m.appendTail(sb);
        String result = sb.toString();
        if (!result.equals(name)) {
            result = removeDuplicateWordsFromName(result);
        }
        return result;
    }

    /**
     * Save the product last used query to cache asynchronously
     * 
     * @param sku the product SKU to cache the query for
     * @param query the query to cache
     * @param profileUrl the profile URL
     */
    public static void setProductLastUsedQueryAsync(String sku, String query,
            String profileUrl) {
        CommonUtils.debug(TAG, "setProductLastUsedQueryAsync: processing sku %1$s; query %2$s",
                sku, query);
        if (!TextUtils.isEmpty(sku)) {
            new UpdateProductLastUsedQueryTask(sku, query, profileUrl).execute();
        }
    }

    /**
     * Save the product last used query to cache synchronously
     * 
     * @param sku the product SKU to cache the query for
     * @param query the query to cache
     * @param profileUrl the profile URL
     * @return true if the product was found and information saved, false
     *         otherwise
     */
    public static boolean setProductLastUsedQuery(String sku, String query, String profileUrl){
        boolean result = false;
        CommonUtils.debug(TAG, "setProductLastUsedQuery: processing sku %1$s; query %2$s", sku,
                query);
        if (!TextUtils.isEmpty(sku)) {
            synchronized (JobCacheManager.sProductDetailsLock) {
                Product p = JobCacheManager.restoreProductDetails(sku, profileUrl);
                if (p != null) {
                    // if product exists and is loaded
                    p.getData().put(MageventoryConstants.MAGEKEY_PRODUCT_LAST_USED_QUERY, query);
                    JobCacheManager.storeProductDetails(p, profileUrl);
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * Get the cached last used query for the product
     * 
     * @param sku the product SKU
     * @param profileUrl the profile URL
     * @return last used query for the product if found, null otherwise
     */
    public static String getProductLastUsedQuery(String sku, String profileUrl) {
        String result = null;
        CommonUtils.debug(TAG, "getProductLastUsedQuery: processing sku %1$s", sku);
        if (!TextUtils.isEmpty(sku)) {
            Product p = JobCacheManager.restoreProductDetails(sku, profileUrl);
            if (p != null) {
                // if product exists and is loaded
                result = p
                        .getStringAttributeValue(MageventoryConstants.MAGEKEY_PRODUCT_LAST_USED_QUERY);
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
    
    /**
     * The handler for the price input field. Contains various useful methods so
     * the price editing functionality may be reused in various places
     */
    public static class PriceInputFieldHandler {
        /**
         * The special price additional data: from and to date
         */
        public static class SpecialPricesData {
            public Date fromDate;
            public Date toDate;
        }

        /**
         * Pattern for the price field
         */
        public static Pattern priceCharacterPattern = Pattern.compile("[\\d\\.\\/]*");

        /**
         * Input filter for the price field to allow only characters matching
         * the priceCharacterPattern
         */
        public static InputFilter PRICE_INPUT_FILTER = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                    int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!priceCharacterPattern.matcher("" + source.charAt(i))
                            .matches()) {
                        return "";
                    }
                }
                return null;
            }
        };
        /**
         * The price editing view
         */
        EditText mPriceView;
        /**
         * Special price data related to this instance handler
         */
        SpecialPricesData mSpecialPriceData = new SpecialPricesData();
        /**
         * Activity where the {@link PriceInputFieldHandler} is used in
         */
        FragmentActivity mActivity;

        /**
         * Construct {@link PriceInputFieldHandler}
         * 
         * @param priceView the price editing view
         * @param activity related activity
         */
        public PriceInputFieldHandler(EditText priceView, FragmentActivity activity) {
            mPriceView = priceView;
            mActivity = activity;
            mPriceView.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    // when long clicked on mPriceView open price editing dialog
                    openPriceEditDialog();
                    return true;
                }
            });
            mPriceView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    String priceText = mPriceView.getText().toString();
                    if (ProductUtils.hasSpecialPrice(priceText)) {
                        // if clicked and price view value contains special
                        // price when do not allow inplace edit and open price
                        // edit dialog
                        openPriceEditDialog();
                    }
                }
            });
            mPriceView.setFilters(new InputFilter[] {
                PRICE_INPUT_FILTER
            });
        }

        /**
         * Open price editing dialog
         */
        protected void openPriceEditDialog() {
            PriceEditFragment detailsFragment = new PriceEditFragment();
            PricesInformation pi = ProductUtils.getPricesInformation(mPriceView.getText()
                    .toString());
            // set parameters
            detailsFragment.setData(pi == null ? null : pi.regularPrice, pi == null ? null
                    : pi.specialPrice, mSpecialPriceData.fromDate, mSpecialPriceData.toDate,
                    new OnEditDoneListener() {

                        @Override
                        public void editDone(Double price, Double specialPrice, Date fromDate,
                                Date toDate) {
                            onPriceEditDone(price, specialPrice, fromDate, toDate);
                        }

                    });
            detailsFragment.show(mActivity.getSupportFragmentManager(), PriceEditFragment.TAG);
        }

        /**
         * Called when user presses OK button in the price edit dialog
         * 
         * @param price
         * @param specialPrice
         * @param fromDate
         * @param toDate
         */
        protected void onPriceEditDone(Double price, Double specialPrice, Date fromDate, Date toDate) {
            setPriceTextValue(ProductUtils.getProductPricesString(price, specialPrice));
            mSpecialPriceData.fromDate = fromDate;
            mSpecialPriceData.toDate = toDate;
        }

        /**
         * Set the priceV field text value and adjust its availability for
         * different price formats
         * 
         * @param price
         */
        public void setPriceTextValue(String price) {
            mPriceView.setText(price);
            boolean editable = !ProductUtils.hasSpecialPrice(price);
            mPriceView.setInputType(editable ? InputType.TYPE_CLASS_NUMBER
                    | InputType.TYPE_NUMBER_FLAG_DECIMAL : InputType.TYPE_NULL);
            mPriceView.setFocusable(editable);
            mPriceView.setFocusableInTouchMode(editable);
            mPriceView.setCursorVisible(editable);
        }

        /**
         * Set the data such as prices and special price information from the
         * product
         * 
         * @param product
         */
        public void setDataFromProduct(Product product) {
            setPriceTextValue(getProductPricesString(product));
            setSpecialPriceDataFromProduct(product);
        }

        /**
         * Set the special price information from the product
         * 
         * @param product
         */
        public void setSpecialPriceDataFromProduct(Product product) {
            mSpecialPriceData.fromDate = product.getSpecialFromDate();
            mSpecialPriceData.toDate = product.getSpecialToDate();
        }

        /**
         * Set the special price from date from the string
         * 
         * @param date
         */
        public void setSpecialPriceFromDate(String date) {
            mSpecialPriceData.fromDate = CommonUtils.parseDate(date);
        }

        /**
         * Set the special price to date from the string
         * 
         * @param date
         */
        public void setSpecialPriceToDate(String date) {
            mSpecialPriceData.toDate = CommonUtils.parseDate(date);
        }

        /**
         * Set regular price information from the string
         * 
         * @param priceString
         */
        public void setRegularPrice(String priceString) {
            Double price = CommonUtils.parseNumber(priceString);
            PricesInformation pi = ProductUtils.getPricesInformation(mPriceView.getText()
                    .toString());
            // update price part in the product price string
            setPriceTextValue(getProductPricesString(price, pi == null ? null : pi.specialPrice));

        }

        /**
         * Set special price information from the string
         * 
         * @param priceString
         */
        public void setSpecialPrice(String priceString) {
            Double specialPrice = CommonUtils.parseNumber(priceString);
            setSpecialPrice(specialPrice);

        }
        /**
         * Set special price information from the number
         * 
         * @param specialPrice
         */
        public void setSpecialPrice(Double specialPrice) {
            PricesInformation pi = ProductUtils.getPricesInformation(mPriceView.getText()
                    .toString());
            // update special price part in the product price string
            setPriceTextValue(getProductPricesString(pi == null ? null : pi.regularPrice,
                    specialPrice));
        }

        /**
         * Get the price view related to the handler
         * 
         * @return
         */
        public EditText getPriceView() {
            return mPriceView;
        }

        /**
         * Get the special price data
         * 
         * @return
         */
        public SpecialPricesData getSpecialPriceData() {
            return mSpecialPriceData;
        }

        /**
         * Validate the price input field value
         * 
         * @param checkNotEmpty whether the non empty value is required
         * @param title the reference to the string constant for the price input
         *            field label
         * @param silent whether no alerts should be shown and no field
         *            activation in case validation fails
         * @return true if value is valid, otherwise reutrns false
         */
        public boolean checkPriceValid(boolean checkNotEmpty, int title, boolean silent) {
            if (!TextUtils.isEmpty(mPriceView.getText())) {
                // check value format
                if (!ProductUtils.isValidPricesString(mPriceView.getText().toString())) {
                    if (!silent) {
                        GuiUtils.alert(R.string.invalid_price_information);
                        GuiUtils.activateField(mPriceView, true, true, true);
                    }
                    return false;
                }
            } else {
                if (checkNotEmpty) {
                    // if empty value is not allowed
                    if (!silent) {
                        GuiUtils.alert(R.string.fieldCannotBeBlank,
                                CommonUtils.getStringResource(title));
                        GuiUtils.activateField(mPriceView, true, true, true);
                    }
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Update the product last used query information task
     */
    public static class UpdateProductLastUsedQueryTask extends SimpleAsyncTask {
        static final String TAG = UpdateProductLastUsedQueryTask.class.getSimpleName();
        /**
         * The product SKU
         */
        String mSku;
        /**
         * The query to save
         */
        String mQuery;
        /**
         * The profile URL
         */
        String mProfileUrl;

        /**
         * @param sku the product SKU to update query for
         * @param query the query to save
         * @param profileUrl the profile URL
         */
        public UpdateProductLastUsedQueryTask(String sku, String query, String profileUrl) {
            super(null);
            mSku = sku;
            mQuery = query;
            mProfileUrl = profileUrl;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                return setProductLastUsedQuery(mSku, mQuery, mProfileUrl) && !isCancelled();
            } catch (Exception ex) {
                CommonUtils.error(TAG, ex);
            }
            return false;
        }

        @Override
        protected void onSuccessPostExecute() {
            // do nothing
        }
    }
}
