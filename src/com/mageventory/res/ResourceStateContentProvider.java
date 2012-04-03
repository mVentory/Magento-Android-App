package com.mageventory.res;

import java.util.Arrays;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mageventory.MageventoryConstants;
import com.mageventory.res.ResourceState.ResourceStateSchema;

public class ResourceStateContentProvider extends ContentProvider implements MageventoryConstants {

    private static final String NULL_COL_HACK = ResourceStateSchema._ID;
    private static final int RESOURCE_STATE = 1;
    private static final int RESOURCE_STATE_ID = 2;
    private static final String TABLE = ResourceStateSchema.TABLE_NAME;
    private static final String TAG = "ResourceStateContentProvider";
    private static final String AUTHORITY = ResourceState.AUTHORITY;

    private ResourceStateDbHelper dbHelper;
    private final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    public ResourceStateContentProvider() {
        super();
        uriMatcher.addURI(AUTHORITY, ResourceStateSchema.PATH, RESOURCE_STATE);
        uriMatcher.addURI(AUTHORITY, ResourceStateSchema.PATH + "/#", RESOURCE_STATE_ID);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        long id;
        try {
            id = ContentUris.parseId(uri);
        } catch (NumberFormatException e) {
            id = -1;
        }
        if (id != -1) {
            selection = ResourceStateSchema._ID + "=" + id + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ')');
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.delete(TABLE, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
        case RESOURCE_STATE:
            return ResourceStateSchema.CONTENT_TYPE;
        case RESOURCE_STATE_ID:
            return ResourceStateSchema.CONTENT_ITEM_TYPE;
        default:
            throw new IllegalArgumentException("unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final long id = db.insert(TABLE, NULL_COL_HACK, values);
        if (id > 0) {
            Uri itemUri = ContentUris.withAppendedId(uri, id);
            getContext().getContentResolver().notifyChange(uri, null);
            return itemUri;
        }
        throw new SQLiteException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new ResourceStateDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteQueryBuilder q = new SQLiteQueryBuilder();

        // select from table
        q.setTables(TABLE);

        // where id = x
        long id;
        try {
            id = ContentUris.parseId(uri);
        } catch (NumberFormatException e) {
            id = -1;
        }
        if (id != -1) {
            selection = ResourceStateSchema._ID + "=" + id + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ')');
        }
        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = ResourceStateSchema._ID + " ASC";
        }

        // a little hack to limit the number of selected rows
        String limit = null;
        final int indexOfLIMIT = selection.indexOf("LIMIT");
        if (indexOfLIMIT != -1) {
            limit = selection.substring(indexOfLIMIT + 5);
            selection = selection.substring(0, indexOfLIMIT);
        }

        if (DEBUG) {
            String s = q.buildQuery(projection, selection, selectionArgs, null, null, sortOrder, limit);
            Log.d(TAG, "" + s + "," + Arrays.toString(selectionArgs));
        }

        // execute sql
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        final Cursor c = q.query(db, projection, selection, selectionArgs, null, null, sortOrder, limit);
        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        long id;
        try {
            id = ContentUris.parseId(uri);
        } catch (NumberFormatException e) {
            id = -1;
        }
        if (id != -1) {
            selection = ResourceStateSchema._ID + "=" + id + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ')');
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.update(TABLE, values, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

}
