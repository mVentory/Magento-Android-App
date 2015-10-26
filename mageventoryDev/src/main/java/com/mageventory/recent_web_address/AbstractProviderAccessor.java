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

package com.mageventory.recent_web_address;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.mageventory.util.CommonUtils;

/**
 * Abstract content provider accessor which contains various common methods
 * 
 * @author Eugene Popovich
 */
public abstract class AbstractProviderAccessor {
    private static final String TAG = AbstractProviderAccessor.class.getSimpleName();
    /**
     * Date format string which is used to serialize/deserialize DATETIME SQLite
     * values
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * Date format which is used to serialized/deserialized DATETIME SQLite
     * values. ThreadLocal is used because DateFormat is not thread safe
     */
    private static ThreadLocal<SimpleDateFormat> sDateFormat = new ThreadLocal<SimpleDateFormat>();

    private final Context mContext;


    /**
     * @param context
     */
    public AbstractProviderAccessor(Context context) {
        mContext = context;
    }

    /**
     * Access the date format used for DATETIME SQLite field values
     * serialization/deserialization.
     * 
     * @return
     */
    protected static SimpleDateFormat getDateFormat() {
        // Such as SimpleDateFormat i s not thread safe we should use one
        // instance per thread (ThreadLocal variable)
        if (sDateFormat.get() == null) {
            sDateFormat.set(new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH));
        }
        return sDateFormat.get();
    }

    /**
     * Parse date/time value from the string ignore any exception using specific
     * date format
     * 
     * @param value
     * @return
     */
    protected static Date safeParseDate(String value) {
        Date result = null;
        if (!TextUtils.isEmpty(value)) {
            try {
                result = getDateFormat().parse(value);
            } catch (ParseException e) {
                CommonUtils.error(TAG, e);
            }
        }
        return result;
    }

    /**
     * Format the date using specific date format
     * 
     * @param date
     * @return
     */
    protected String formatDate(Date date) {
        String result = null;
        if (date != null) {
            return getDateFormat().format(date);
        }
        return result;
    }

    /**
     * Close cursor ignore any exceptions which may occur during operation
     * 
     * @param cursor the cursor to close
     */
    public static void closeCursor(Cursor cursor) {
        try {
            cursor.close();
        } catch (Exception ex) {
            CommonUtils.error(TAG, ex);
        }
    }

    /**
     * Get the context
     * 
     * @return
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Get the content resolver
     * 
     * @return
     */
    public ContentResolver getContentResolver() {
        return getContext().getContentResolver();
    }
}
