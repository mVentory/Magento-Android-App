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

package com.mageventory.tasks;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.View;
import android.widget.EditText;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.AbsProductActivity;
import com.mageventory.bitmapfun.util.BitmapfunUtils;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttribute.ContentType;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.TrackerUtils;
import com.mageventory.util.WebUtils;

/**
 * Getting Book Details
 * 
 * @author hussein
 */
public class BookInfoLoader extends SimpleAsyncTask implements MageventoryConstants {

    static final String TAG = BookInfoLoader.class.getSimpleName();
    
    /**
     * Enumeration contains possible book code types handled by the
     * {@link BookInfoLoader}. The different google books API calls will be used
     * depend on book code type
     */
    public enum BookCodeType {
    	/**
    	 * The ISBN10 or ISBN13 book code type
    	 */
        ISBN,
        /**
         * The book id code type
         */
        BOOK_ID
    }
    
    /**
     * Enumeration contains possible code types used in the {@link
     * BookInfoLoader.#isValidCode(String, CodeType)} method with extra data
     * such as pattern and short code length
     */
    enum ValidationCodeType {
        ISBN(ISBN_PATTERN, 10), ISSN(ISSN_PATTERN, 8);

        /**
         * The code pattern
         */
        private String mPattern;
        /**
         * The length of the short code version
         */
        private int mShortCodeLength;

        ValidationCodeType(String pattern, int shortCodeLength) {
            mPattern = pattern;
            mShortCodeLength = shortCodeLength;
        }

        /**
         * Get the code type pattern
         * 
         * @return
         */
        public String getPattern() {
            return mPattern;
        }

        /**
         * Get the code type short code length
         * 
         * @return
         */
        public int getShortCodeLength() {
            return mShortCodeLength;
        }
    }
    /**
     * General regular expression pattern for ISBN-13 and ISBN-10 codes
     */
    private static final String ISBN_PATTERN = "^((?:(?:978|979)?\\d{10})|(?:\\d{9}x))$";
    /**
     * General regular expression pattern for ISSN-13 and ISSN-8 codes
     */
    private static final String ISSN_PATTERN = "^((?:977\\d{10})|(?:\\d{7}[\\dx]))$";

    /**
     * Name of the ISBN 10 custom attribute code 
     * 
     * TODO remove with the references
     * when the servers will have ContentType update installed
     */
    public static final String ISBN_10_ATTRIBUTE = "bk_isbn_10_";
    /**
     * Name of the ISBN 13 custom attribute code 
     * 
     * TODO remove with the references
     * when the servers will have ContentType update installed
     */
    public static final String ISBN_13_ATTRIBUTE = "bk_isbn_13_";
    /**
     * Name of the ISSN 13 or ISSN 8 custom attribute code
     * 
     * TODO remove with the references
     * when the servers will have ContentType update installed
     */
    public static final String ISSN_ATTRIBUTE = "bk_issn_";
    /**
     * Prefix for the all custom book attribute code names
     */
    public static final String BOOK_ATTRIBUTE_CODE_PREFIX = "bk_";

    private static final String ITEMS_KEY = "items";
    private static final String INDUSTRY_IDENTIFIERS_KEY = "industryIdentifiers";
    private static final String TYPE_KEY = "type";
    /**
     * The key for the book id parameter in the google books API book details
     * response
     */
    private static final String ID_KEY = "id";
    private static final String IDENTIFIER_KEY = "identifier";
    private static final String TITLE_KEY = "title";
    private static final String DESCRIPTION_KEY = "description";
    private static final String VALUES_SEPARATOR = ", ";
    private static final String[] MANDATORY_KEYS = new String[] {
            TITLE_KEY, DESCRIPTION_KEY
    };

    private Map<String, String> mBookInfoMap = new TreeMap<String, String>();
    private CustomAttributesList mAttribList;
    private AbsProductActivity mHostActivity;
    private String mCode;
    /**
     * The mCode field type: either ISBN or book id. Depend on the code type
     * different google books API calls will be used to load the book details
     * for the specified code
     */
    private BookCodeType mBookCodeType;
    private String mApiKey;
    /**
     * Reference to the custom attribute related to the book info loading
     * operation. May be null in case book info loading operation is performed
     * for the barcode input
     */
    private CustomAttribute mCustomAttribute;

    /**
     * @param hostActivity the calling activity
     * @param attribList attribute list the information should be loaded to
     * @param code the code to check
     * @param bookCodeType the code type: either ISBN or Book Id
     * @param apiKey the google books API key
     * @param customAttribute reference to the related custom attribute or null
     *            if operation is not related to custom attributes
     * @param loadingControl corresponding loading control
     */
    public BookInfoLoader(AbsProductActivity hostActivity, CustomAttributesList attribList,
            String code, BookCodeType bookCodeType, String apiKey, CustomAttribute customAttribute,
            LoadingControl loadingControl) {
        super(loadingControl);
        mHostActivity = hostActivity;
        mAttribList = attribList;
        mCode = code;
        mBookCodeType = bookCodeType;
        mApiKey = apiKey;
        mCustomAttribute = customAttribute;
    }

    @Override
    protected Boolean doInBackground(Void... args) {
        HttpURLConnection urlConnection = null;

        try {
            if (isCancelled()) {
                return false;
            }
            // perform the API request
            String content = performGoogleBooksApiRequest();
            if (isCancelled()) {
                return false;
            }
            JSONObject jsonObject = new JSONObject(content);
            if (mBookCodeType == BookCodeType.ISBN) {
                // if the book details was loaded from the query request, which
                // doesn't send HTML, then get the book id and load details with
                // proper HTML via another API request

                JSONObject itemInformation = getItemInformationJson(jsonObject);
                if (itemInformation != null) {
                    // if item information is present in the output
                    String bookId = itemInformation.getString(ID_KEY);
                    if (!TextUtils.isEmpty(bookId)) {
                        // if book id is present in the returned book details
                        //
                        // set the new code
                        mCode = bookId;
                        // set the new book code type
                        mBookCodeType = BookCodeType.BOOK_ID;
                        // perform the API request again
                        content = performGoogleBooksApiRequest();
                        if (isCancelled()) {
                            return false;
                        }
                        jsonObject = new JSONObject(content);
                    }
                }

            }
            loadBookInfo(jsonObject);
            return !isCancelled();
        } catch (Exception ex) {
            GuiUtils.error(TAG, R.string.errorGeneral, ex);

        }

        return false;

    }

    /**
     * Perform the Google Books API request and get the String response output
     * 
     * @return String which contains the API response text output
     * @throws MalformedURLException
     * @throws IOException
     */
    public String performGoogleBooksApiRequest() throws MalformedURLException, IOException {
        HttpURLConnection urlConnection = null;
        try {
            long start = System.currentTimeMillis();
            final URL url = new URL(CommonUtils.getStringResource(
                    mBookCodeType == BookCodeType.ISBN ?
                        // if ISBN code passed
                        R.string.google_api_query_isbn_url
                        // if books id code passed
                        : 
                        // if the book ID URL/ should be used
                        R.string.google_api_book_id_url
                        , mCode, mApiKey));
            urlConnection = (HttpURLConnection) url.openConnection();
            final InputStream in = new BufferedInputStream(urlConnection.getInputStream(),
                    BitmapfunUtils.IO_BUFFER_SIZE);

            TrackerUtils.trackDataLoadTiming(System.currentTimeMillis() - start,
                    "performGoogleBooksApiRequest",
                    TAG);
            return WebUtils.convertStreamToString(in);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    @Override
    protected void onSuccessPostExecute() {
        if (isCancelled()) {
            return;
        }
        if (mBookInfoMap.isEmpty()) {
            GuiUtils.alert(R.string.no_book_info_found);
            return;
        }

        // Show Book Details

        showBookInfo();
    }

    /**
     * Read all book information from the json object
     * 
     * @param json
     * @throws JSONException
     */
    private void loadBookInfo(JSONObject json) throws JSONException {

        JSONObject itemInformation = getItemInformationJson(json);
        if (itemInformation != null) {
            searchRecursively(itemInformation);
        }
    }

    /**
     * Get the JSON leave containing the book information
     * 
     * @param json
     * @return
     * @throws JSONException
     */
    public JSONObject getItemInformationJson(JSONObject json) throws JSONException {
        JSONObject itemInformation = null;
        if (mBookCodeType == BookCodeType.BOOK_ID) {
            // if book code type is book ID then the book id API request was
            // used and the JSON root contains the book information
            itemInformation = json;
        } else {
            // if ISBN code type then get the first element of the JSON "items"
            // element (query results)
            JSONArray itemsJson = json.optJSONArray(ITEMS_KEY);
            if (itemsJson != null && itemsJson.length() > 0) {
                itemInformation = itemsJson.getJSONObject(0);
            }
        }
        return itemInformation;
    }

    void searchRecursively(JSONObject json) throws JSONException
    {
        @SuppressWarnings("rawtypes")
        Iterator keys=json.keys();
        while(keys.hasNext())
        {
            if (isCancelled()) {
                break;
            }
         // loop to get the dynamic key
            String currentDynamicKey = (String)keys.next();
            Object value=json.get(currentDynamicKey);
            checkDataTypeAndSearch(value, currentDynamicKey);
        }
    }

    void checkDataTypeAndSearch(Object value, final String key) throws JSONException {

        if (isCancelled()) {
            return;
        }
        if (value == null) {
            // do nothing
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++)
            {
                Object object = array.get(i);
                checkDataTypeAndSearch(object, key);
            }
        } else if (value instanceof JSONObject) {
            JSONObject json = (JSONObject) value;
            if (key.equals(INDUSTRY_IDENTIFIERS_KEY)) {
                String newKey = json.getString(TYPE_KEY);
                Object newValue = json.get(IDENTIFIER_KEY);
                checkDataTypeAndSearch(newValue, newKey);
            } else {
                searchRecursively(json);
            }
        } else {
            final String str = value.toString();

            boolean added = false;
            for (Iterator<CustomAttribute> it = mAttribList.getList().iterator(); it.hasNext();) {
                CustomAttribute attrib = it.next();

                String code = attrib.getCode();
                // Code
                String codeString = code.replace(BOOK_ATTRIBUTE_CODE_PREFIX, "").trim(); // Get
                                                                    // Parameter
                                                                    // to
                                                                    // be
                                                                    // found
                                                                    // in
                                                                    // string
                // remove ending underscore if exists
                if (codeString.endsWith("_")) {
                    codeString = codeString.substring(0, codeString.length() - 1);
                }

                if (codeString.equalsIgnoreCase(key)) {
                    addBookInfoValue(code, str);
                    added = true;
                    break;
                }
            }
            if (!added) {
                for (String mandatoryKey : MANDATORY_KEYS) {
                    if (mandatoryKey.equalsIgnoreCase(key)) {
                        addBookInfoValue(mandatoryKey, str);
                        break;
                    }
                }
            }
        }
    }

    void addBookInfoValue(String key, final String value) {
        String currentValue = mBookInfoMap.get(key);
        if (currentValue == null) {
            currentValue = value;
        } else {
            currentValue += VALUES_SEPARATOR + value;
        }
        mBookInfoMap.put(key, currentValue);
    }

    // Show Book Information in attributes
    // Loop Over attributes get the code
    // find the code index in bookInfo string and get the value
    private void showBookInfo() {

        for (Iterator<CustomAttribute> it = mAttribList.getList().iterator(); it.hasNext();) {
            CustomAttribute attrib = it.next();
            if (attrib.isReadOnly()) {
                // skip read only attributes
                continue;
            }
            //
            // Get Code
            String code = attrib.getCode();

            String attrValue = mBookInfoMap.get(code);
            attrib.setSelectedValue(attrValue, true);

            // hide the isbn attribute hint view if it is unrelated to
            // mCustomAttribute
            // TODO remove ISBN code check when the content type functionality
            // will be enabled everywhere
            if (attrib.hasContentType(ContentType.ISBN10)
                    || attrib.hasContentType(ContentType.ISBN13)
                    || TextUtils.equals(code, ISBN_10_ATTRIBUTE)
                    || TextUtils.equals(code, ISBN_13_ATTRIBUTE)) {
                if (mCustomAttribute == null || !TextUtils.equals(mCustomAttribute.getCode(), code)) {
                    attrib.getHintView().setVisibility(View.GONE);
                }
            }

            // Special Cases [Description and Title]
            if (code.toLowerCase().contains(TITLE_KEY))
                mHostActivity.setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_NAME, attrValue,
                        true, true);
            if (code.toLowerCase().contains(DESCRIPTION_KEY)) {
                mHostActivity.setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_SHORT_DESCRIPTION,
                        attrValue, true, true);
                mHostActivity.setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_DESCRIPTION,
                        attrValue, true, true);
            }

            // if attribute value contains links
            if (attrValue != null && attrValue.matches("(?i).*" + ImageUtils.PROTO_PREFIX + ".*"))
                Linkify.addLinks((EditText) attrib.getCorrespondingView(), Linkify.ALL);
        }
        for (String mandatoryKey : MANDATORY_KEYS) {
            String attrValue = mBookInfoMap.get(mandatoryKey);
            // Special Cases [Description and Title]
            if (mandatoryKey.equalsIgnoreCase(TITLE_KEY)) {
                mHostActivity.setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_NAME, attrValue,
                        true, true);
            } else if (mandatoryKey.equalsIgnoreCase(DESCRIPTION_KEY)) {
                mHostActivity.setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_SHORT_DESCRIPTION,
                        attrValue, true, true);
                mHostActivity.setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_DESCRIPTION,
                        attrValue, true, true);
            }
        }
    }

    /**
     * Whether the book information was found
     * 
     * @return
     */
    public boolean isBookInfoFound() {
        return !mBookInfoMap.isEmpty();
    }

    /**
     * Get the custom attribute related to the task
     * 
     * @return
     */
    public CustomAttribute getCustomAttribute() {
        return mCustomAttribute;
    }
    
    /**
     * Sanitize ISBN or ISSN code from punctuation, spaces, hyphens, "ISBN" or
     * "ISSN" text.
     * 
     * @param code
     * @return
     */
    public static String sanitizeIsbnOrIssn(String code) {
        return code.replaceAll("(?i)[,\\.\\s-]|(?:ISBN)|(?:ISSN)", "");
    }

    /**
     * Check thether the code is ISBN-10 code accordingly to standard
     * requirements
     * 
     * @param code
     * @return
     */
    public static boolean isIsbn10Code(String code) {
        if (TextUtils.isEmpty(code)) {
            return false;
        }
        code = sanitizeIsbnOrIssn(code).toLowerCase();
        return code.length() == ValidationCodeType.ISBN.getShortCodeLength() && isIsbnCode(code);
    }

    /**
     * Check thether the code is ISBN-13 code accordingly to standard
     * requirements
     * 
     * @param code
     * @return
     */
    public static boolean isIsbn13Code(String code) {
        if (TextUtils.isEmpty(code)) {
            return false;
        }
        code = sanitizeIsbnOrIssn(code).toLowerCase();
        return code.length() == 13 && isIsbnCode(code);
    }

    /**
     * Check thether the code is ISBN-10 or ISBN-13 code accordingly to standard
     * requirements
     * 
     * @param code
     * @return
     */
    public static boolean isIsbnCode(String code) {
        return isValidCode(code, ValidationCodeType.ISBN);
    }

    /**
     * Check thether the code is ISSN-8 or ISSN-13 code accordingly to standard
     * requirements
     * 
     * @param code
     * @return
     */
    public static boolean isIssnCode(String code) {
        return isValidCode(code, ValidationCodeType.ISSN);
    }

    /**
     * Check thether the code is ISBN (ISBN-13, ISBN-10) or ISSN (ISSN-13,
     * ISSN-8) code accordingly to standard requirements
     * 
     * @param code the code to check
     * @param codeType code type either ISSN or ISBN
     * @return
     */
    public static boolean isValidCode(String code, ValidationCodeType codeType) {
        boolean valid = !TextUtils.isEmpty(code);
        if (valid) {
            code = sanitizeIsbnOrIssn(code).toLowerCase();
            valid = code.matches(codeType.getPattern());
            if (valid) {
                int length = code.length();
                if (length == codeType.getShortCodeLength()) {
                    // ISBN 10 or ISSN 8 validation
                    // The final character of a ten digit International Standard
                    // Book Number or International Standard Serial Number is a
                    // check digit computed so that multiplying
                    // each digit by its position in the number (counting from
                    // the right) and taking the sum of these products modulo 11
                    // is 0. The digit the farthest to the right (which is
                    // multiplied by 1) is the check digit, chosen to make the
                    // sum correct. It may need to have the value 10, which is
                    // represented as the letter X.
                    boolean endsWithX = code.endsWith("x");
                    long parsedCode = Long
                            .valueOf(endsWithX ? code.substring(0, length - 1) : code);
                    int sum = endsWithX ? 10 : 0;
                    int i = endsWithX ? 2 : 1;
                    while (parsedCode != 0) {
                        int mod = (int) (parsedCode % 10);
                        parsedCode = parsedCode / 10;
                        sum += mod * i++;
                    }
                    valid = sum % 11 == 0;
                } else {
                    long parsedCode = Long.valueOf(code);
                    // ISBN 13 or ISSN 13 validation
                    // (10 - (x1+3*x2+x3+3*x4+x5+3*x6+x7+3*x8+x9+3*x10+x11+3*x12) mod 10) mod 10 = x13
                    int sum = 0;
                    int i = 1;
                    int checkDigit = 0;
                    while (parsedCode != 0) {
                        int mod = (int) (parsedCode % 10);
                        parsedCode = parsedCode / 10;
                        if (i == 1) {
                            checkDigit = mod;
                        } else {
                            if ((length - i + 1) % 2 == 0) {
                                sum += mod * 3;
                            } else {
                                sum += mod;
                            }
                        }
                        i++;
                    }
                    valid = (10 - sum % 10) % 10 == checkDigit;
                }
            }
        }
        return valid;
    }
}
