package com.mageventory.job;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class JobQueueDBHelper extends SQLiteOpenHelper {

	public static final String DB_NAME = "jobqueue.db";
	private static final int DB_VERSION = 1;
	public static final String TABLE_PENDING_NAME = "jobqueue_pending";
	public static final String TABLE_FAILED_NAME = "jobqueue_failed";

	// column names
	public static final String JOB_TIMESTAMP = "job_timestamp";
	public static final String JOB_PRODUCT_ID = "job_product_id";
	public static final String JOB_TYPE = "job_type";
	public static final String JOB_SKU = "job_sku";
	public static final String JOB_ATTEMPTS = "job_attempts";
	public static final String JOB_SERVER_URL = "job_server_url_hash";

	// column types
	private static final String JOB_TIMESTAMP_T = "INT8";
	private static final String JOB_PRODUCT_ID_T = "INTEGER";
	private static final String JOB_TYPE_T = "INTEGER";
	private static final String JOB_SKU_T = "TEXT";
	private static final String JOB_ATTEMPTS_T = "INTEGER";
	private static final String JOB_SERVER_URL_T = "TEXT";
	

	public JobQueueDBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	private void createJobTable(SQLiteDatabase db, String tableName) {
		// prepare sql
		StringBuilder sql = new StringBuilder(1024);
		sql.append("CREATE TABLE IF NOT EXISTS ");
		sql.append(tableName);
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
		sql.append(", ");
		
		sql.append(JOB_SERVER_URL);
		sql.append(' ');
		sql.append(JOB_SERVER_URL_T);

		sql.append(");");

		// create table
		db.execSQL(sql.toString());
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createJobTable(db, TABLE_PENDING_NAME);
		createJobTable(db, TABLE_FAILED_NAME);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENDING_NAME + ";");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAILED_NAME + ";");
		onCreate(db);
	}
}
