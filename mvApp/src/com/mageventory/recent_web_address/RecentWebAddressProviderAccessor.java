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

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.mageventory.MyApplication;
import com.mageventory.recent_web_address.RecentWebAddressProvider.RecentWebAddresses;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.SimpleAsyncTask;

/**
 * Provider accessor for the {@link RecentWebAddress}es
 * 
 * @author Eugene Popovich
 */
public class RecentWebAddressProviderAccessor extends AbstractProviderAccessor {
    private static final String TAG = RecentWebAddressProviderAccessor.class.getSimpleName();

    /**
     * Singleton pattern implementation. Reference to
     * RecentWebAddressProviderAccessor instance
     */
    private static RecentWebAddressProviderAccessor instance = new RecentWebAddressProviderAccessor(
            MyApplication.getContext());

    /**
     * Get the instantiated instance of {@link RecentWebAddressProviderAccessor}
     * 
     * @return an instance of ProductAliasCacheManager
     */
    public static RecentWebAddressProviderAccessor getInstance() {
        return instance;
    }

    // this is private. Use getInstance() method to instantiate
    // RecentWebAddressProviderAccessor
    private RecentWebAddressProviderAccessor(Context context) {
        super(context);
    }

    /**
     * Get the content uri for the recent web address object
     * 
     * @param recentWebAddress
     * @return
     */
    public static Uri getRecentWebAddressContentUri(RecentWebAddress recentWebAddress) {
        return getRecentWebAddressContentUriForId(recentWebAddress.getId());
    }

    /**
     * Get the content uri for the recent web address by id
     * 
     * @param id the recent web address id
     * @return
     */
    public static Uri getRecentWebAddressContentUriForId(long id) {
        Uri contentUri = ContentUris.withAppendedId(
                RecentWebAddressProvider.RecentWebAddresses.CONTENT_ID_URI_BASE, id);
        return contentUri;
    }

    /**
     * Extract the recent web address object from the cusrsor
     * 
     * @param cursor contains database information which should be extracted and
     *            transformed to {@link RecentWebAddress} object
     * @return instance of {@link RecentWebAddress} object with fields values
     *         initiated from the cursor
     */
    public static RecentWebAddress extractRecentWebAddress(Cursor cursor) {
        return extractRecentWebAddress(cursor, new RecentWebAddressColumnsIndexes(cursor));
    }

    /**
     * Extract the recent web address object from the cusrsor using
     * columnIndexes information
     * 
     * @param cursor contains database information which should be extracted and
     *            transformed to {@link RecentWebAddress} object
     * @param columnIndexes information about column indexes in the cursor
     * @return instance of {@link RecentWebAddress} object with fields values
     *         initiated from the cursor
     */
    public static RecentWebAddress extractRecentWebAddress(Cursor cursor,
            RecentWebAddressColumnsIndexes columnIndexes) {
        long id = cursor.getInt(columnIndexes.idColumnIndex);
        RecentWebAddress recentWebAddress = new RecentWebAddress(id);
        recentWebAddress.setDomain(cursor.getString(columnIndexes.domainColumnIndex));
        recentWebAddress.setAccessCount(cursor.getInt(columnIndexes.accessCountColumnIndex));
        recentWebAddress.setLastUsed(safeParseDate(cursor
                .getString(columnIndexes.lastUsedColumnIndex)));
        recentWebAddress.setProfileUrl(cursor.getString(columnIndexes.profileUrlIndex));
        return recentWebAddress;
    }

    /**
     * Get the recent web address by its id
     * 
     * @param id an id of recent web address
     * @return
     */
    public RecentWebAddress getRecentWebAddress(long id) {
        Uri contentUri = getRecentWebAddressContentUriForId(id);
        return getRecentWebAddress(contentUri);
    }

    /**
     * Get the recent web address by content uri
     * 
     * @param uri the recent web address content uri
     * @return
     */
    public RecentWebAddress getRecentWebAddress(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return extractRecentWebAddress(cursor);
            } else {
                return null;
            }
        } finally {
            closeCursor(cursor);
        }
    }

    /**
     * Get recent web address for domain and profile
     * 
     * @param domain the recent web address domain
     * @param profileUrl the profile url for which the recent web address
     *            information should be retrieved
     * @return
     */
    public RecentWebAddress getRecentWebAddress(String domain, String profileUrl) {
        Cursor cursor = getContentResolver().query(
                RecentWebAddressProvider.RecentWebAddresses.CONTENT_URI, null,
                RecentWebAddresses.COLUMN_NAME_DOMAIN + "=?"
                +" AND "+RecentWebAddresses.COLUMN_NAME_PROFILE_URL+"=?"
                , new String[] {
                    domain, profileUrl
                }, null);
        List<RecentWebAddress> recentWebAddresses = getRecentWebAddresses(cursor);
        return recentWebAddresses.isEmpty() ? null : recentWebAddresses.get(0);
    }

    /**
     * Get all recent web addresses for the profile
     * 
     * @param sortOrder the sorting order for the recent web addresses
     * @param profileUrl the profile url for which the recent web addresses
     *            should be retrieved
     * @return
     */
    public List<RecentWebAddress> getAllRecentWebAddresses(String sortOrder, String profileUrl) {
        Cursor cursor = getAllRecentWebAddressesCursor(sortOrder, profileUrl);
        return getRecentWebAddresses(cursor);
    }

    /**
     * Extract the recent web addresses objects from the cursor
     * 
     * @param cursor contains database information which should be extracted and
     *            transformed to {@link RecentWebAddress} objects
     * @return list of {@link RecentWebAddress} objects
     */
    public List<RecentWebAddress> getRecentWebAddresses(Cursor cursor) {
        try {
            List<RecentWebAddress> recentWebAddresss = new ArrayList<RecentWebAddress>(
                    cursor.getCount());
            RecentWebAddressColumnsIndexes columnIndexes = null;
            while (cursor.moveToNext()) {
                if (columnIndexes == null)
                {
                    columnIndexes = new RecentWebAddressColumnsIndexes(cursor);
                }
                    
                RecentWebAddress recentWebAddress = extractRecentWebAddress(cursor, columnIndexes);
                if (recentWebAddress != null) {
                    recentWebAddresss.add(recentWebAddress);
                }
            }
            return recentWebAddresss;
        } finally {
            closeCursor(cursor);
        }
    }

    /**
     * Get the cursor for the recent web addresses content for the specified
     * sorting order and profile url
     * 
     * @param sortOrder the sorting order for the recent web addresses
     * @param profileUrl the profile url for which the recent web addresses
     *            cursor should be retrieved
     * @return
     */
    public Cursor getAllRecentWebAddressesCursor(String sortOrder, String profileUrl) {
        Cursor cursor = getContentResolver().query(
                RecentWebAddressProvider.RecentWebAddresses.CONTENT_URI, null,
                RecentWebAddresses.COLUMN_NAME_PROFILE_URL + "=?", new String[] {
                    profileUrl
                },
                sortOrder);
        return cursor;
    }

    /**
     * Save the recent web address to the database. It may be either insert or
     * update operation depend on whether the recent web address has assigned id
     * information
     * 
     * @param recentWebAddress
     * @return
     */
    public boolean save(RecentWebAddress recentWebAddress) {
        ContentResolver cp = getContentResolver();
        ContentValues values = new ContentValues();
        // update variable columns data
        values.put(RecentWebAddressProvider.RecentWebAddresses.COLUMN_NAME_ACCESS_COUNT,
                recentWebAddress.getAccessCount());
        values.put(RecentWebAddressProvider.RecentWebAddresses.COLUMN_NAME_LAST_USED,
                formatDate(recentWebAddress.getLastUsed()));
        boolean result;
        // if object should be inserted
        if (recentWebAddress.getId() == 0) {
            // set the constant columns data
            values.put(RecentWebAddressProvider.RecentWebAddresses.COLUMN_NAME_DOMAIN,
                    recentWebAddress.getDomain());
            values.put(RecentWebAddressProvider.RecentWebAddresses.COLUMN_NAME_PROFILE_URL,
                    recentWebAddress.getProfileUrl());

            Uri contentUri = cp.insert(RecentWebAddressProvider.RecentWebAddresses.CONTENT_URI,
                    values);
            result = contentUri != null;
        } else {
            int rowsUpdated = cp.update(getRecentWebAddressContentUri(recentWebAddress), values,
                    null, null);
            result = rowsUpdated == 1;
        }
        return result;
    }

    /**
     * Delete the recent web address
     * 
     * @param recentWebAddress recent web address to delete
     */
    public void deleteRecentWebAddress(RecentWebAddress recentWebAddress) {
        deleteRecentWebAddress(recentWebAddress.getId());
    }

    /**
     * Delete the recent web address by id
     * 
     * @param id an id of th recent web address which should be removed
     */
    public void deleteRecentWebAddress(long id) {
        Uri contentUri = getRecentWebAddressContentUriForId(id);
        getContentResolver().delete(contentUri, null, null);
    }

    /**
     * Delete all recent web addresses stored in the database
     */
    public void deleteAllRecentWebAddresses() {
        getContentResolver().delete(RecentWebAddressProvider.RecentWebAddresses.CONTENT_URI, null,
                null);
    }

    /**
     * Update the recent web address counter and last updated information for
     * the url asynchronously. If address is absent it will be created with the
     * counter value 1
     * 
     * @param url the url for the recent web address which should be updated
     * @param profileUrl the profile url for which the recent web addresses
     *            information should be updated
     */
    public static void updateRecentWebAddressCounterAsync(String url, String profileUrl) {
        new UpdateRecentWebAddressCounterTask(url, profileUrl).execute();
    }

    /**
     * The class containing
     */
    public static class RecentWebAddressColumnsIndexes {

        public final int idColumnIndex;
        public final int domainColumnIndex;
        public final int accessCountColumnIndex;
        public final int lastUsedColumnIndex;
        public final int profileUrlIndex;

        public RecentWebAddressColumnsIndexes(Cursor cursor) {
            idColumnIndex = cursor
                    .getColumnIndexOrThrow(RecentWebAddressProvider.RecentWebAddresses._ID);
            domainColumnIndex = cursor
                    .getColumnIndexOrThrow(RecentWebAddressProvider.RecentWebAddresses.COLUMN_NAME_DOMAIN);
            accessCountColumnIndex = cursor
                    .getColumnIndexOrThrow(RecentWebAddressProvider.RecentWebAddresses.COLUMN_NAME_ACCESS_COUNT);
            lastUsedColumnIndex = cursor
                    .getColumnIndexOrThrow(RecentWebAddressProvider.RecentWebAddresses.COLUMN_NAME_LAST_USED);
            profileUrlIndex = cursor
                    .getColumnIndexOrThrow(RecentWebAddressProvider.RecentWebAddresses.COLUMN_NAME_PROFILE_URL);
        }
    }

    /**
     * Update the recent web address counter and last updated information for
     * the url task
     */
    public static class UpdateRecentWebAddressCounterTask extends SimpleAsyncTask {
        String mUrl;
        String mProfileUrl;

        public UpdateRecentWebAddressCounterTask(String url, String profileUrl) {
            super(null);
            mUrl = url;
            mProfileUrl = profileUrl;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (mUrl != null) {
                    // extract the domain from the url
                    URL url = new URL(mUrl);
                    String domain = url.getHost();

                    // obtain the recent web address information for the domain
                    // and profile url if exists
                    RecentWebAddress recentWebAddress = RecentWebAddressProviderAccessor
                            .getInstance().getRecentWebAddress(domain, mProfileUrl);
                    // if information doesn' exist yet create it
                    if (recentWebAddress == null) {
                        recentWebAddress = new RecentWebAddress();
                        recentWebAddress.setDomain(domain);
                        recentWebAddress.setProfileUrl(mProfileUrl);
                    }
                    // increment access counter
                    recentWebAddress.incrementAccessCount();
                    // update last used value
                    recentWebAddress.setLastUsed(new Date(System.currentTimeMillis()));

                    RecentWebAddressProviderAccessor.getInstance().save(recentWebAddress);
                }
                return !isCancelled();
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
