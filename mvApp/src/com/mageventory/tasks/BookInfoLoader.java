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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.mageventory.activity.ProductCreateActivity;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.util.CommonUtils;

/**
 * Getting Book Details
 * 
 * @author hussein
 */
public class BookInfoLoader extends AsyncTask<Object, Void, Boolean> {
	
    static final String TAG = BookInfoLoader.class.getSimpleName();

    private String bookInfo = "";
    private Bitmap image;
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
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet getRequest = new HttpGet();
            getRequest.setURI(new URI("https://www.googleapis.com/books/v1/volumes?q=isbn:"
                    + args[0].toString()
                    + "&key=" + args[1].toString()));

            HttpResponse response = client.execute(getRequest);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity()
                    .getContent()));

            loadBookInfo(reader);

            reader.close();

            return true;

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            CommonUtils.error(TAG, e);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            CommonUtils.error(TAG, e);
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            CommonUtils.error(TAG, e);
        }

        return false;

    }

    @Override
    protected void onPostExecute(Boolean result) {

        mHostActivity.dismissProgressDialog();

        if (TextUtils.isEmpty(bookInfo)) {
            Toast.makeText(mHostActivity, "No Book Found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show Book Details

        showBookInfo();
    }

    // read all Book Information from Buffer reader
    // For the List of attributes get attribute code
    // and try to find its information in the response
    // Special Cases "ISBN-10 , ISBN-13 and Authors"
    // STRING FORMAT
    // code::value;code::value;..............
    private void loadBookInfo(BufferedReader reader) {
        String line = "";
        try {
            // Copy AtrList into a temp list
            ArrayList<ViewGroup> atrViews = new ArrayList<ViewGroup>();
            for (int i = 0; i < mHostActivity.atrListV.getChildCount(); i++)
                atrViews.add((ViewGroup) mHostActivity.atrListV.getChildAt(i));

            while ((line = reader.readLine()) != null) {
                // Set Line in Lower Case [Helpful in comparison and so on]
                line = line.toLowerCase();

                int i = 0;
                for (Iterator<CustomAttribute> it = mAttribList.getList().iterator(); it.hasNext();) {
                    CustomAttribute attrib = it.next();

                    String code = attrib.getCode();
                    // Code
                    String codeString = "\"" + code.replace("bk_", "").trim(); // Get
                                                                               // Parameter
                                                                               // to
                                                                               // be
                                                                               // found
                                                                               // in
                                                                               // string
                    int lastUnderScoreIndex = codeString.lastIndexOf("_");
                    codeString = codeString.substring(0, lastUnderScoreIndex).toLowerCase(); // remove
                                                                                             // last
                                                                                             // underscore

                    // If Line contains the Code
                    if (line.contains(codeString)) {
                        // Handling Special Cases "ISBN_10,ISBN_13"
                        if (codeString.contains("isbn")) {
                            line = reader.readLine(); // Get ISBN
                            bookInfo += code + "::" + getInfo(line, "identifier") + ";";
                            break; // Break Loop --> go to read next line
                        }

                        // Handling Special Case "Authors"
                        if (TextUtils.equals(codeString, "\"authors")) {
                            line = reader.readLine();
                            String authors = "";
                            while (!line.contains("]")) {
                                authors += line.replace("\"", "");
                                line = reader.readLine();
                            }
                            bookInfo += code + "::" + authors.trim() + ";";
                            break; // Break Loop --> go to read next line
                        }

                        // Any Other Parameter -- get details
                        bookInfo += code + "::" + getInfo(line, codeString) + ";";
                        break; // Break Loop --> go to read next line
                    }
                    i++;
                }

            }
        } catch (IOException excpetion) {
            return;
        }
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

            // Get Value from BookInfo String
            // 1- get code index in book info string
            int index = bookInfo.indexOf(code);
            if (index == -1) // / Attribute doesn't exist "Escape it"
                continue;

            // 2- get next index of ";"
            int endOfValIndex = bookInfo.indexOf(";", index);

            String attrCodeValue = bookInfo.substring(index, endOfValIndex);
            String attrValue = attrCodeValue.replace(code, "").replace("::", "");
            attrib.setSelectedValue(attrValue, true);
            // Special Cases [Description and Title]
            if (code.toLowerCase().contains("title"))
                mHostActivity.nameV.setText(attrValue);
            if (code.toLowerCase().contains("description"))
                mHostActivity.descriptionV.setText(attrValue);

            if (attrValue.contains("http:") || attrValue.contains("https:"))
                Linkify.addLinks((EditText) attrib.getCorrespondingView(), Linkify.ALL);
        }
    }

    // Get Book Information from line
    private String getInfo(String line, String name) {
        if (line.contains("https"))
            return line.replace(name, "").replace(",", "").replace("\"", "").replace(":", "")
                    .replace("https", "https:").trim();
        else
            return line.replace(name, "").replace(",", "").replace("\"", "").replace(":", "")
                    .replace("http", "http:")
                    .trim();
    }
}
