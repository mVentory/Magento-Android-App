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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.text.util.Linkify;
import android.widget.EditText;

import com.mageventory.R;
import com.mageventory.activity.AbsProductActivity;
import com.mageventory.bitmapfun.util.BitmapfunUtils;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.TrackerUtils;
import com.mageventory.util.WebUtils;

/**
 * Getting Book Details
 * 
 * @author hussein
 */
public class BookInfoLoader extends SimpleAsyncTask {

    static final String TAG = BookInfoLoader.class.getSimpleName();
    
    /**
     * General regular expression pattern for ISBN-13 and ISBN-10 codes
     */
    private static final String ISBN_PATTERN = "^((?:(?:978|979)?\\d{10})|(?:\\d{9}x))$";

    private static final String ITEMS_KEY = "items";
    private static final String INDUSTRY_IDENTIFIERS_KEY = "industryIdentifiers";
    private static final String TYPE_KEY = "type";
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
    private String mApiKey;

    /**
     * @param hostActivity the calling activity
     * @param attribList attribute list the information should be loaded to
     * @param code the code to check
     * @param apiKey the google books API key
     */
    public BookInfoLoader(AbsProductActivity hostActivity, CustomAttributesList attribList,String code, String apiKey) {
        super(hostActivity.getBookLoadingControl());
        mHostActivity = hostActivity;
        mAttribList = attribList;
        mCode = code;
        mApiKey = apiKey;
    }

    @Override
    protected Boolean doInBackground(Void... args) {
        HttpURLConnection urlConnection = null;

        try {
            long start = System.currentTimeMillis();
            final URL url = new URL(CommonUtils.getStringResource(R.string.google_api_query_url,
                    mCode, mApiKey));
            urlConnection = (HttpURLConnection) url.openConnection();
            final InputStream in = new BufferedInputStream(urlConnection.getInputStream(),
                    BitmapfunUtils.IO_BUFFER_SIZE);

            TrackerUtils.trackDataLoadTiming(System.currentTimeMillis() - start, "downloadConfig",
                    TAG);
            String content = WebUtils.convertStreamToString(in);

            JSONObject jsonObject = new JSONObject(content);
            loadBookInfo(jsonObject);
            return true;
        } catch (Exception ex) {
            GuiUtils.error(TAG, R.string.errorGeneral, ex);

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return false;

    }

    @Override
    protected void onSuccessPostExecute() {
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

        JSONArray itemsJson = json.optJSONArray(ITEMS_KEY);
        if (itemsJson != null && itemsJson.length() > 0) {
            JSONObject itemInformation = itemsJson.getJSONObject(0);
            searchRecursively(itemInformation);
        }
    }

    void searchRecursively(JSONObject json) throws JSONException
    {
        @SuppressWarnings("rawtypes")
        Iterator keys=json.keys();
        while(keys.hasNext())
        {
         // loop to get the dynamic key
            String currentDynamicKey = (String)keys.next();
            Object value=json.get(currentDynamicKey);
            checkDataTypeAndSearch(value, currentDynamicKey);
        }
    }

    void checkDataTypeAndSearch(Object value, final String key) throws JSONException {

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
                String codeString = code.replace("bk_", "").trim(); // Get
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

            //
            // Get Code
            String code = attrib.getCode();

            String attrValue = mBookInfoMap.get(code);
            attrib.setSelectedValue(attrValue, true);

            // Special Cases [Description and Title]
            if (code.toLowerCase().contains(TITLE_KEY))
                mHostActivity.nameV.setText(attrValue);
            if (code.toLowerCase().contains(DESCRIPTION_KEY))
                mHostActivity.descriptionV.setText(attrValue);

            // if attribute value contains links
            if (attrValue != null && attrValue.matches("(?i).*" + ImageUtils.PROTO_PREFIX + ".*"))
                Linkify.addLinks((EditText) attrib.getCorrespondingView(), Linkify.ALL);
        }
        for (String mandatoryKey : MANDATORY_KEYS) {
            String attrValue = mBookInfoMap.get(mandatoryKey);
            // Special Cases [Description and Title]
            if (mandatoryKey.equalsIgnoreCase(TITLE_KEY)) {
                mHostActivity.nameV.setText(attrValue);
            } else if (mandatoryKey.equalsIgnoreCase(DESCRIPTION_KEY)) {
                mHostActivity.descriptionV.setText(attrValue);
            }
        }
    }

    /**
     * Sanitize ISBN code from punctuation, spaces, hyphens, "ISBN" text.
     * 
     * @param code
     * @return
     */
    public static String sanitizeIsbn(String code) {
        return code.replaceAll("(?i)[,\\.\\s-]|(?:ISBN)", "");
    }

    /**
     * Check thether the code is ISBN-10 or ISBN-13 code accordingly to standard
     * requirements
     * 
     * @param code
     * @return
     */
    public static boolean isIsbnCode(String code) {
        boolean valid = !TextUtils.isEmpty(code);
        if (valid) {
            code = sanitizeIsbn(code).toLowerCase();
            valid = code.matches(ISBN_PATTERN);
            if (valid) {
                int length = code.length();
                if (length == 10) {
                    // ISBN 10 validation
                    // The final character of a ten digit International Standard
                    // Book Number is a check digit computed so that multiplying
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
                    // ISBN 13 validation
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
