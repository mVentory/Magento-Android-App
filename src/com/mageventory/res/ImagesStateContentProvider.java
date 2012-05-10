package com.mageventory.res;

import java.util.Arrays;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mageventory.MageventoryConstants;


public class ImagesStateContentProvider{
	
	private static final String NULL_COL_HACK = ImagesState._ID;
    private static final String TABLE = ImagesState.TABLE_NAME;
    private static final String TAG = "ImageStateContentProvider";    
    private ImagesStateDbHelper dbHelper;
    private Context appContext;
    private SQLiteDatabase db;
    public static Object mSynchronisationObject = new Object(); 
    
    public ImagesStateContentProvider(Context context) {
    	 dbHelper = new ImagesStateDbHelper(context);
    	 appContext = context;
    }
    
    private void openDB()
    {
    	db = dbHelper.getWritableDatabase();
    }
    
    public void closeDB()
    {
    	db.close();
    }

    public int delete(String selection, String[] selectionArgs) {
    synchronized(mSynchronisationObject)
    {
    	openDB();
    	int count = db.delete(TABLE, selection, selectionArgs);
        closeDB();
    
        return count;
    }
    }

    public void insert(ContentValues values) {
    synchronized(mSynchronisationObject)
    {
    	openDB();
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final long id = db.insert(TABLE, NULL_COL_HACK, values);
        closeDB();
        if (id >= 0) {
           return;
        }
        
        throw new SQLiteException("Failed to insert row");
    }
    }

    public void clearDatabase()
    {
    	appContext.deleteDatabase("imagesStates.db");
    }
    
    //The database needs to be closed from outside of this class (this is bad design but will be changed in the future)
    //Query should be synchronized from the calling code. This is bad design but look at the previous comment...
    public Cursor query(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteQueryBuilder q = new SQLiteQueryBuilder();
        
        // select from table
        q.setTables(TABLE);

        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = ImagesState._ID + " ASC";
        }

        // a little hack to limit the number of selected rows
        String limit = null;
        final int indexOfLIMIT = selection.indexOf("LIMIT");
        if (indexOfLIMIT != -1) {
            limit = selection.substring(indexOfLIMIT + 5);
            selection = selection.substring(0, indexOfLIMIT);
        }

        // execute sql
        openDB(); //The database needs to be closed from outside of this class (this is bad design but will be changed in the future)
        final Cursor c = q.query(db, projection, selection, selectionArgs, null, null, sortOrder, limit);
        return c;
    }

    public int update(ContentValues values, String selection, String[] selectionArgs) {
    synchronized(mSynchronisationObject)
    {
   		openDB();
        int count = db.update(TABLE, values, selection, selectionArgs);
        closeDB();
        return count;
    }
    }
}
