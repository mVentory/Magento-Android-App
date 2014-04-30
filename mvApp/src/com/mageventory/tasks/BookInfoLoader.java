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

import android.os.AsyncTask;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.widget.EditText;
import android.widget.Toast;

import com.mageventory.R;
import com.mageventory.activity.ProductCreateActivity;
import com.mageventory.bitmapfun.util.BitmapfunUtils;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.TrackerUtils;
import com.mageventory.util.WebUtils;

/**
 * Getting Book Details
 * 
 * @author hussein
 */
public class BookInfoLoader extends AsyncTask<Object, Void, Boolean> {

    static final String TAG = BookInfoLoader.class.getSimpleName();
    
    private static final String ISBN_PATTERN = "^(?:978|979)?\\d{10}$";

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
    private ProductCreateActivity mHostActivity;

    public BookInfoLoader(ProductCreateActivity hostActivity, CustomAttributesList attribList) {
        mHostActivity = hostActivity;
        mAttribList = attribList;
    }

    /*
     * (non-Javadoc)
     * @see android.os.AsyncTask#onPreExecute()
     */
    @Override
    protected void onPreExecute() {
        mHostActivity.showProgressDialog("Loading Book Information ..........");
    }

    @Override
    protected Boolean doInBackground(Object... args) {
        HttpURLConnection urlConnection = null;

        try {
            long start = System.currentTimeMillis();
            final URL url = new URL(CommonUtils.getStringResource(R.string.google_api_query_url,
                    args));
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
            CommonUtils.error(TAG, ex);

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return false;

    }

    @Override
    protected void onPostExecute(Boolean result) {

        mHostActivity.dismissProgressDialog();

        if (mBookInfoMap.isEmpty()) {
            Toast.makeText(mHostActivity, "No Book Found", Toast.LENGTH_SHORT).show();
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
                int lastUnderScoreIndex = codeString.lastIndexOf("_");
                codeString = codeString.substring(0, lastUnderScoreIndex); // remove
                                                                           // last
                                                                           // underscore

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
            if (attrValue == null) {
                continue;
            }
            attrib.setSelectedValue(attrValue, true);

            // Special Cases [Description and Title]
            if (code.toLowerCase().contains(TITLE_KEY))
                mHostActivity.nameV.setText(attrValue);
            if (code.toLowerCase().contains(DESCRIPTION_KEY))
                mHostActivity.descriptionV.setText(attrValue);

            if (attrValue.contains("http:") || attrValue.contains("https:"))
                Linkify.addLinks((EditText) attrib.getCorrespondingView(), Linkify.ALL);
        }
        for (String mandatoryKey : MANDATORY_KEYS) {
            String attrValue = mBookInfoMap.get(mandatoryKey);
            if (!TextUtils.isEmpty(attrValue)) {
                // Special Cases [Description and Title]
                if (mandatoryKey.equalsIgnoreCase(TITLE_KEY)) {
                    mHostActivity.nameV.setText(attrValue);
                } else if (mandatoryKey.equalsIgnoreCase(DESCRIPTION_KEY)) {
                    mHostActivity.descriptionV.setText(attrValue);
                }
            }
        }
    }

    /**
     * Check thether the code is ISBN code
     * 
     * @param code
     * @return
     */
    public static boolean isIsbnCode(String code)
    {
        return !TextUtils.isEmpty(code) && code.matches(ISBN_PATTERN);
    }
}
