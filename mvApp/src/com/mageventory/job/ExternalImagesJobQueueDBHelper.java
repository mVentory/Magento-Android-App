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

package com.mageventory.job;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ExternalImagesJobQueueDBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "externalimagesjobqueue.db";
    private static final int DB_VERSION = 1;
    public static final String TABLE_NAME = "jobqueue";

    // column names
    public static final String JOB_TIMESTAMP = "job_timestamp";
    public static final String JOB_PRODUCT_CODE = "product_code";
    public static final String JOB_PRODUCT_SKU = "product_sku";
    public static final String JOB_PROFILE_ID = "profile_id";
    public static final String JOB_ATTEMPTS_COUNT = "attempts_count";

    // column types
    private static final String JOB_TIMESTAMP_T = "INT8";
    private static final String JOB_PRODUCT_CODE_T = "TEXT";
    private static final String JOB_PRODUCT_SKU_T = "TEXT";
    private static final String JOB_PROFILE_ID_T = "INT8";
    private static final String JOB_ATTEMPTS_COUNT_T = "INTEGER";

    public ExternalImagesJobQueueDBHelper(Context context) {
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

        sql.append(JOB_PRODUCT_CODE);
        sql.append(' ');
        sql.append(JOB_PRODUCT_CODE_T);
        sql.append(", ");

        sql.append(JOB_PRODUCT_SKU);
        sql.append(' ');
        sql.append(JOB_PRODUCT_SKU_T);
        sql.append(", ");

        sql.append(JOB_PROFILE_ID);
        sql.append(' ');
        sql.append(JOB_PROFILE_ID_T);
        sql.append(", ");

        sql.append(JOB_ATTEMPTS_COUNT);
        sql.append(' ');
        sql.append(JOB_ATTEMPTS_COUNT_T);

        sql.append(");");

        // create table
        db.execSQL(sql.toString());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createJobTable(db, TABLE_NAME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME + ";");
        onCreate(db);
    }
}
