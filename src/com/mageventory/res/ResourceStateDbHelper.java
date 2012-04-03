package com.mageventory.res;

import java.util.Map;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mageventory.res.ResourceState.ResourceStateSchema;

public class ResourceStateDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "resourcestate.db";
    private static final int DB_VERSION = 1;

    public ResourceStateDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // prepare sql
        StringBuilder sql = new StringBuilder(1024);
        sql.append("CREATE TABLE IF NOT EXISTS ");
        sql.append(ResourceStateSchema.TABLE_NAME);
        sql.append(" (");
        sql.append(ResourceStateSchema._ID);
        sql.append(' ');
        sql.append(ResourceStateSchema.ID_T);
        for (Map.Entry<String, String> e : ResourceStateSchema.COLUMNS.entrySet()) {
            sql.append(", ");
            sql.append(e.getKey());
            sql.append(' ');
            sql.append(e.getValue());
        }
        sql.append(");");

        // create table
        db.execSQL(sql.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ResourceStateSchema.TABLE_NAME + ";");
        onCreate(db);
    }

}
