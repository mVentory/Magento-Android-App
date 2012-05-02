package com.mageventory.res;

import java.util.Map;

import com.mageventory.res.ResourceState.ResourceStateSchema;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ImagesStateDbHelper extends SQLiteOpenHelper {

	private static final String DB_NAME = "imagesStates.db";
    private static final int DB_VERSION = 1;

    public ImagesStateDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

	
	@Override
	public void onCreate(SQLiteDatabase db) {
		 // prepare sql
        StringBuilder sql = new StringBuilder(1024);        
        sql.append("CREATE TABLE IF NOT EXISTS ");
        sql.append(ImagesState.TABLE_NAME);
        sql.append(" (");
        sql.append(ImagesState._ID);
        sql.append(' ');
        sql.append(ImagesState.INTEGER_T);
        for (Map.Entry<String, String> e : ImagesState.COLUMNS.entrySet()) {
            sql.append(", ");
            sql.append(e.getKey());
            sql.append(' ');
            sql.append(e.getValue());
        }
        sql.append(",");
        sql.append(" PRIMARY KEY (");
        sql.append(ImagesState._ID +","+ ImagesState.PRODUCT_ID +","+ ImagesState.IMAGE_INDEX + ")");
        sql.append(");");

        // create table
        db.execSQL(sql.toString());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS " + ImagesState.TABLE_NAME + ";");
        onCreate(db);

	}
}
