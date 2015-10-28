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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.mageventory.MyApplication;
import com.mageventory.util.CommonUtils;
import com.mventory.BuildConfig;

/**
 * Content provider for the {@link RecentWebAddress}
 * 
 * @author Eugene Popovich
 */
public class RecentWebAddressProvider extends ContentProvider {
    public static final String TAG = RecentWebAddressProvider.class.getSimpleName();

    /**
     * Content provider authority specified in the AndroidManifest.xml provider
     * description
     */
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".RecentWebAddressManager";

    /**
     * The database that the provider uses as its underlying data store
     */
    private static final String DATABASE_NAME = "recent_web_addresses.db";

    /**
     * The database version
     */
    private static final int DATABASE_VERSION = 1;

    /**
     * The incoming URI matches the RecentWebAddresses URI pattern
     */
    private static final int RECENT_WEB_ADDRESSES = 1;

    /**
     * The incoming URI matches the RecentWebAddress ID URI pattern
     */
    private static final int RECENT_WEB_ADDRESS_ID = 2;

    /**
     * A UriMatcher instance
     */
    private static final UriMatcher sUriMatcher;

    /**
     * 
     Handle to a new DatabaseHelper.
     */
    private DatabaseHelper mOpenHelper;

    /**
     * A block that instantiates and sets static objects
     */
    static {

        /*
         * Creates and initializes the URI matcher
         */
        // Create a new instance
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // Add a pattern that routes URIs terminated with "recent_web_addresses" to a
        // RECENT_WEB_ADDRESSES operation
        sUriMatcher.addURI(AUTHORITY, RecentWebAddresses.TABLE_NAME, RECENT_WEB_ADDRESSES);

        // Add a pattern that routes URIs terminated with "recent_web_addresses" plus an
        // integer to a recent web address ID operation
        sUriMatcher.addURI(AUTHORITY, RecentWebAddresses.TABLE_NAME + "/#", RECENT_WEB_ADDRESS_ID);
    }

    /**
     * Initializes the provider by creating a new DatabaseHelper. onCreate() is
     * called automatically when Android creates the provider in response to a
     * resolver request from a client.
     */
    @Override
    public boolean onCreate() {

        // Creates a new helper object. Note that the database itself isn't
        // opened until something tries to access it, and it's only created if it doesn't already exist.
        mOpenHelper = new DatabaseHelper(getContext());

        // Assumes that any failures will be reported by a thrown exception.
        return true;
    }

    /**
     * This method is called when a client calls
     * {@link android.content.ContentResolver#query(Uri, String[], String, String[], String)}.
     * Queries the database and returns a cursor containing the results.
     *
     * @return A cursor containing the results of the query. The cursor exists but is empty if
     * the query returns no results or an exception occurs.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {

        // Constructs a new query builder and sets its table name
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(RecentWebAddresses.TABLE_NAME);

        /**
         * Choose the projection and adjust the "where" clause based on URI pattern-matching.
         */
        switch (sUriMatcher.match(uri)) {
            // If the incoming URI is for recent web addresss, chooses the
            // RecentWebAddresses projection
            case RECENT_WEB_ADDRESSES:
                break;

            /*
             * If the incoming URI is for a single recent web address identified
             * by its ID, chooses the recent web address ID projection, and
             * appends "_ID = <recent web address ID>" to the where clause, so
             * that it selects that single recent web address
             */
            case RECENT_WEB_ADDRESS_ID:
                qb.appendWhere(RecentWebAddresses._ID + // the name of the ID
                                                        // column
                        "=" +
                        // the position of the recent web address ID itself in
                        // the incoming URI
                        uri.getPathSegments().get(
                                RecentWebAddresses.RECENT_WEB_ADDRESS_ID_PATH_POSITION));
                break;

            default:
                // If the URI doesn't match any of the known patterns, throw an exception.
                throw new IllegalArgumentException("Unknown URI " + uri);
        }


        String orderBy;
        // If no sort order is specified, uses the default
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = RecentWebAddresses.DEFAULT_SORT_ORDER;
        } else {
            // otherwise, uses the incoming sort order
            orderBy = sortOrder;
        }

        // Opens the database object in "read" mode, since no writes need to be done.
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        /*
         * Performs the query. If no problems occur trying to read the database, then a Cursor
         * object is returned; otherwise, the cursor variable contains null. If no records were
         * selected, then the Cursor object is empty, and Cursor.getCount() returns 0.
         */
        Cursor c = qb.query(
            db,            // The database to query
            projection,    // The columns to return from the query
            selection,     // The columns for the where clause
            selectionArgs, // The values for the where clause
            null,          // don't group the rows
            null,          // don't filter by row groups
            orderBy        // The sort order
        );

        // Tells the Cursor what URI to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }
    
    /**
     * This is called when a client calls {@link android.content.ContentResolver#getType(Uri)}.
     * Returns the MIME data type of the URI given as a parameter.
     *
     * @param uri The URI whose MIME type is desired.
     * @return The MIME type of the URI.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    @Override
    public String getType(Uri uri) {

        /**
         * Chooses the MIME type based on the incoming URI pattern
         */
        switch (sUriMatcher.match(uri)) {

            // If the pattern is for recent web addresses, returns the general content type.
            case RECENT_WEB_ADDRESSES:
                return RecentWebAddresses.CONTENT_TYPE;

            // If the pattern is for recent web address IDs, returns the recent web address ID content type.
            case RECENT_WEB_ADDRESS_ID:
                return RecentWebAddresses.CONTENT_ITEM_TYPE;

            // If the URI pattern doesn't match any permitted patterns, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
     }
    
    /**
     * This is called when a client calls
     * {@link android.content.ContentResolver#insert(Uri, ContentValues)}.
     * Inserts a new row into the database. This method sets up default values for any
     * columns that are not included in the incoming map.
     * If rows were inserted, then listeners are notified of the change.
     * @return The row ID of the inserted row.
     * @throws SQLException if the insertion fails.
     */
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {

        // Validates the incoming URI. Only the full provider URI is allowed for inserts.
        if (sUriMatcher.match(uri) != RECENT_WEB_ADDRESSES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // A map to hold the new record's values.
        ContentValues values;

        // If the incoming values map is not null, uses it for the new values.
        if (initialValues != null) {
            values = new ContentValues(initialValues);

        } else {
            // Otherwise, create a new value map
            values = new ContentValues();
        }

        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        // Performs the insert and returns the ID of the new recent web address.
        long rowId = db.insert(
            RecentWebAddresses.TABLE_NAME, // The table to insert
                                           // into.
            null,                          // A hack, SQLite sets this column value to null
                                           // if values is empty.
            values                         // A map of column names, and the values to insert
                                           // into the columns.
        );

        // If the insert succeeded, the row ID exists.
        if (rowId > 0) {
            // Creates a URI with the recent web address ID pattern and the new row ID appended to it.
            Uri recentWebAddressUri = ContentUris.withAppendedId(RecentWebAddresses.CONTENT_ID_URI_BASE, rowId);

            // Notifies observers registered against this provider that the data changed.
            getContext().getContentResolver().notifyChange(recentWebAddressUri, null);
            return recentWebAddressUri;
        }

        // If the insert didn't succeed, then the rowID is <= 0. Throws an exception.
        throw new SQLException("Failed to insert row into " + uri);
    }

    /**
     * This is called when a client calls
     * {@link android.content.ContentResolver#delete(Uri, String, String[])}.
     * Deletes records from the database. If the incoming URI matches the recent web address ID URI pattern,
     * this method deletes the one record specified by the ID in the URI. Otherwise, it deletes a
     * a set of records. The record or records must also match the input selection criteria
     * specified by where and whereArgs.
     *
     * If rows were deleted, then listeners are notified of the change.
     * @return If a "where" clause is used, the number of rows affected is returned, otherwise
     * 0 is returned. To delete all rows and get a row count, use "1" as the where clause.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalWhere;

        int count;

        // Does the delete based on the incoming URI pattern.
        switch (sUriMatcher.match(uri)) {

            // If the incoming pattern matches the general pattern for recent web addresss, does a delete
            // based on the incoming "where" columns and arguments.
            case RECENT_WEB_ADDRESSES:
                count = db.delete(
                    RecentWebAddresses.TABLE_NAME, // The database table name
                    where,                         // The incoming where clause column names
                    whereArgs                      // The incoming where clause values
                );
                break;

                // If the incoming URI matches a single recent web address ID, does the delete based on the
                // incoming data, but modifies the where clause to restrict it to the
                // particular recent web address ID.
            case RECENT_WEB_ADDRESS_ID:
                /*
                 * Starts a final WHERE clause by restricting it to the
                 * desired recent web address ID.
                 */
                finalWhere =
                        RecentWebAddresses._ID + // The ID column name
                        " = " +                  // test for equality
                        uri.getPathSegments().   // the incoming recent web address ID
                                get(RecentWebAddresses.RECENT_WEB_ADDRESS_ID_PATH_POSITION)
                ;

                // If there were additional selection criteria, append them to the final
                // WHERE clause
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                // Performs the delete.
                count = db.delete(
                    RecentWebAddresses.TABLE_NAME, // The database table name.
                    finalWhere,                    // The final WHERE clause
                    whereArgs                      // The incoming where clause values.
                );
                break;

            // If the incoming pattern is invalid, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /* Gets a handle to the content resolver object for the current context, and notifies it
         * that the incoming URI changed. The object passes this along to the resolver framework,
         * and observers that have registered themselves for the provider are notified.
         */
        getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of rows deleted.
        return count;
    }

    /**
     * This is called when a client calls
     * {@link android.content.ContentResolver#update(Uri,ContentValues,String,String[])}
     * Updates records in the database. The column names specified by the keys in the values map
     * are updated with new data specified by the values in the map. If the incoming URI matches the
     * recent web address ID URI pattern, then the method updates the one record specified by the ID in the URI;
     * otherwise, it updates a set of records. The record or records must match the input
     * selection criteria specified by where and whereArgs.
     * If rows were updated, then listeners are notified of the change.
     *
     * @param uri The URI pattern to match and update.
     * @param values A map of column names (keys) and new values (values).
     * @param where An SQL "WHERE" clause that selects records based on their column values. If this
     * is null, then all records that match the URI pattern are selected.
     * @param whereArgs An array of selection criteria. If the "where" param contains value
     * placeholders ("?"), then each placeholder is replaced by the corresponding element in the
     * array.
     * @return The number of rows updated.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {

        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;

        // Does the update based on the incoming URI pattern
        switch (sUriMatcher.match(uri)) {

            // If the incoming URI matches the general recent web addresses pattern, does the update based on
            // the incoming data.
            case RECENT_WEB_ADDRESSES:

                // Does the update and returns the number of rows updated.
                count = db.update(
                    RecentWebAddresses.TABLE_NAME, // The database table name.
                    values,                        // A map of column names and new values to use.
                    where,                         // The where clause column names.
                    whereArgs                      // The where clause column values to select on.
                );
                break;

            // If the incoming URI matches a single recent web address ID, does the update based on the incoming
            // data, but modifies the where clause to restrict it to the particular recent web address ID.
            case RECENT_WEB_ADDRESS_ID:
                // From the incoming URI, get the recent web address ID
                String recentWebAddressId = uri.getPathSegments().get(RecentWebAddresses.RECENT_WEB_ADDRESS_ID_PATH_POSITION);

                /*
                 * Starts creating the final WHERE clause by restricting it to the incoming
                 * recent web address ID.
                 */
                finalWhere =
                        RecentWebAddresses._ID + // The ID column name
                        " = " +                  // test for equality
                        recentWebAddressId       // the incoming recent web address ID
                ;

                // If there were additional selection criteria, append them to the final WHERE
                // clause
                if (where !=null) {
                    finalWhere = finalWhere + " AND " + where;
                }


                // Does the update and returns the number of rows updated.
                count = db.update(
                    RecentWebAddresses.TABLE_NAME, // The database table name.
                    values,                        // A map of column names and new values to use.
                    finalWhere,                    // The final WHERE clause to use placeholders for whereArgs
                    whereArgs                      // The where clause column values to select on, or
                                                   // null if the values are in the where argument.
                );
                break;
            // If the incoming pattern is invalid, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /*
         * Gets a handle to the content resolver object for the current context,
         * and notifies it that the incoming URI changed. The object passes this
         * along to the resolver framework, and observers that have registered
         * themselves for the provider are notified.
         */
        getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of rows updated.
        return count;
    }
    
    /**
     * This class helps open, create, and upgrade the database file
     */
    static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {

            // calls the super constructor, requesting the default cursor
            // factory.
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         * Creates the underlying database
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            // Create table
            db.execSQL("CREATE TABLE " + RecentWebAddresses.TABLE_NAME + " ("
                    + RecentWebAddresses._ID + " " + RecentWebAddresses.COLUMN_TYPE_ID +","
                    + RecentWebAddresses.COLUMN_NAME_DOMAIN + " " + RecentWebAddresses.COLUMN_TYPE_DOMAIN +","
                    + RecentWebAddresses.COLUMN_NAME_ACCESS_COUNT + " " + RecentWebAddresses.COLUMN_TYPE_ACCESS_COUNT +","
                    + RecentWebAddresses.COLUMN_NAME_LAST_USED + " " + RecentWebAddresses.COLUMN_TYPE_LAST_USED +","
                    + RecentWebAddresses.COLUMN_NAME_PROFILE_URL + " " + RecentWebAddresses.COLUMN_TYPE_PROFILE_URL
                    + ");");
            // Create indexes
            db.execSQL("CREATE INDEX PROFILE_URL ON " + RecentWebAddresses.TABLE_NAME
                    + "(" + RecentWebAddresses.COLUMN_NAME_PROFILE_URL + ");");
            db.execSQL("CREATE INDEX DOMAIN ON " + RecentWebAddresses.TABLE_NAME
                    + "(" + RecentWebAddresses.COLUMN_NAME_DOMAIN + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            // Logs that the database is being upgraded
            CommonUtils.verbose(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion
                    + ", which will destroy all old data");

            // Kills the indexes if exists
            db.execSQL("DROP INDEX IF EXISTS PROFILE_URL");
            db.execSQL("DROP INDEX IF EXISTS DOMAIN");
            // Kills the table and existing data
            db.execSQL("DROP TABLE IF EXISTS " + RecentWebAddresses.TABLE_NAME);

            // Recreates the database with a new version
            onCreate(db);
        }
    }

    /**
     * RecentWebAddresses table contract
     */
    public static final class RecentWebAddresses implements BaseColumns {

        // This class cannot be instantiated
        private RecentWebAddresses() {
        }

        /**
         * The table name offered by this provider
         */
        public static final String TABLE_NAME = "recent_web_addresses";

        /*
         * URI definitions
         */

        /**
         * The scheme part for this provider's URI
         */
        private static final String SCHEME = "content://";

        /**
         * Path parts for the URIs
         */

        /**
         * Path part for the RecentWebAddresses URI
         */
        private static final String PATH_RECENT_WEB_ADDRESSES = "/" + TABLE_NAME;

        /**
         * Path part for the RecentWebAddresses ID URI
         */
        private static final String PATH_RECENT_WEB_ADDRESS_ID = PATH_RECENT_WEB_ADDRESSES + "/";

        /**
         * 0-relative position of a recent web address ID segment in the path part of a recent web address
         * ID URI
         */
        public static final int RECENT_WEB_ADDRESS_ID_PATH_POSITION = 1;

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_RECENT_WEB_ADDRESSES);

        /**
         * The content URI base for a single recent web address. Callers must append a
         * numeric recent web address id to this Uri to retrieve a recent web address
         */
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME + AUTHORITY
                + PATH_RECENT_WEB_ADDRESS_ID);

        /**
         * The content URI match pattern for a single recent web address, specified by its
         * ID. Use this to match incoming URIs or to construct an Intent.
         */
        public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME + AUTHORITY
                + PATH_RECENT_WEB_ADDRESS_ID + "/#");

        /*
         * MIME type definitions
         */

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of recent web addresss.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mventory.recent_web_address";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * recent web address.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mventory.recent_web_address";

        /*
         * Column definitions
         */

        /**
         * Database column type for the {@link #_ID} column
         */
        public static final String COLUMN_TYPE_ID = "INTEGER PRIMARY KEY AUTOINCREMENT";
        
        /**
         * Column name for the domain name of the recent web address
         * <P>
         * Type: {@link #COLUMN_TYPE_DOMAIN}
         * </P>
         */
        public static final String COLUMN_NAME_DOMAIN = "domain";
        
        /**
         * Database column type for the {@link #COLUMN_NAME_DOMAIN} column
         */
        public static final String COLUMN_TYPE_DOMAIN="VARCHAR(255) not null";
        
        /**
         * Column name for the access count of the recent web address
         * <P>
         * Type: {@link #COLUMN_TYPE_ACCESS_COUNT}
         * </P>
         */
        public static final String COLUMN_NAME_ACCESS_COUNT = "accessCount";
        
        /**
         * Database column type for the {@link #COLUMN_NAME_ACCESS_COUNT} column
         */
        public static final String COLUMN_TYPE_ACCESS_COUNT="INTEGER not null";

        /**
         * Column name for the access count of the recent web address
         * <P>
         * Type: {@link #COLUMN_TYPE_LAST_USED}
         * </P>
         */
        public static final String COLUMN_NAME_LAST_USED = "lastUsed";
        
        /**
         * Database column type for the {@link #COLUMN_NAME_LAST_USED} column
         */
        public static final String COLUMN_TYPE_LAST_USED="DATETIME not null";
        
        /**
         * Column name for the profile url the recent web address related to
         * <P>
         * Type: {@link #COLUMN_TYPE_PROFILE_URL}
         * </P>
         */
        public static final String COLUMN_NAME_PROFILE_URL = "profileUrl";

        /**
         * Database column type for the {@link #COLUMN_NAME_PROFILE_URL} column
         */
        public static final String COLUMN_TYPE_PROFILE_URL = "TEXT not null";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = _ID + " ASC";

        /**
         * The descending sort order by the lastUsed column for this table
         */
        public static final String LAST_USED_DESC_SORT_ORDER = COLUMN_NAME_LAST_USED + " DESC";
    }
}
