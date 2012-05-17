package com.mageventory.job;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class JobQueueDBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "jobqueue.db";
    private static final int DB_VERSION = 1;
	public static final String TABLE_NAME = "jobqueue";

	// column names
	public static final String JOB_TIMESTAMP = "job_timestamp";
	public static final String JOB_PRODUCT_ID = "job_product_id";
	public static final String JOB_TYPE = "job_type";
	public static final String JOB_SKU = "job_sku";
	public static final String JOB_ATTEMPTS = "job_attempts";

	// column types
	private static final String JOB_TIMESTAMP_T = "INT8";
	private static final String JOB_PRODUCT_ID_T = "INTEGER";
	private static final String JOB_TYPE_T = "INTEGER";
	private static final String JOB_SKU_T = "TEXT";
	private static final String JOB_ATTEMPTS_T = "INTEGER";

	
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
    	
    	sql.append(JOB_TIMESTAMP);
    	sql.append(' ');
    	sql.append(JOB_TIMESTAMP_T);
    	sql.append(", ");
    	
    	sql.append(JOB_PRODUCT_ID);
    	sql.append(' ');
    	sql.append(JOB_PRODUCT_ID_T);
    	sql.append(", ");
    	
    	sql.append(JOB_TYPE);
    	sql.append(' ');
    	sql.append(JOB_TYPE_T);
    	sql.append(", ");
    	
    	sql.append(JOB_SKU);
    	sql.append(' ');
    	sql.append(JOB_SKU_T);
    	sql.append(", ");

    	sql.append(JOB_ATTEMPTS);
    	sql.append(' ');
    	sql.append(JOB_ATTEMPTS_T);
    	
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
