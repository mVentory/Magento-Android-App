package com.mageventory.job;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class JobQueueDBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "jobqueue.db";
    private static final int DB_VERSION = 1;
	public static final String TABLE_NAME = "jobqueue";

	// column names
	public static final String JOB_ORDER = "job_order";
	public static final String JOB_FILE_PATH = "job_file_path";

	// column types
	private static final String JOB_ORDER_T = "INTEGER";
	private static final String JOB_FILE_PATH_T = "TEXT UNIQUE ON CONFLICT REPLACE";
	
    public JobQueueDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // prepare sql
    	StringBuilder sql = new StringBuilder(1024);
    	sql.append("CREATE TABLE IF NOT EXISTS ");
    	sql.append(TABLE_NAME);
    	sql.append(" (");
    	
    	sql.append(JOB_ORDER);
    	sql.append(' ');
    	sql.append(JOB_ORDER_T);
    	sql.append(", ");
    	
    	sql.append(JOB_FILE_PATH);
    	sql.append(' ');
    	sql.append(JOB_FILE_PATH_T);

        sql.append(");");

        // create table
        db.execSQL(sql.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME + ";");
        onCreate(db);
    }
}
