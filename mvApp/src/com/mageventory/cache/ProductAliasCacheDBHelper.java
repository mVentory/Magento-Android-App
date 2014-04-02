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

package com.mageventory.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * DB helper for product alias cache
 */
public class ProductAliasCacheDBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "product_alias_cache.db";
    private static final int DB_VERSION = 2;
    public static final String TABLE_NAME = "ProductAliasCache";

    // column names
    public static final String PRODUCT_SKU = "product_sku";
    public static final String PRODUCT_BARCODE = "product_barcode";
    public static final String PRODUCT_ID = "product_id";
    public static final String PROFILE_URL = "url";

    // column types
    private static final String PRODUCT_SKU_T = "TEXT";
    private static final String PRODUCT_BARCODE_T = "TEXT";
    private static final String PRODUCT_ID_T = "TEXT";
    private static final String PROFILE_URL_T = "TEXT";

    public ProductAliasCacheDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    private void createProductAliasCacheTable(SQLiteDatabase db, String tableName) {
        // prepare sql
        StringBuilder sql = new StringBuilder(1024);
        sql.append("CREATE TABLE IF NOT EXISTS ");
        sql.append(tableName);
        sql.append(" (");

        sql.append(PRODUCT_SKU);
        sql.append(' ');
        sql.append(PRODUCT_SKU_T);
        sql.append(", ");

        sql.append(PRODUCT_BARCODE);
        sql.append(' ');
        sql.append(PRODUCT_BARCODE_T);
        sql.append(", ");

        sql.append(PRODUCT_ID);
        sql.append(' ');
        sql.append(PRODUCT_ID_T);
        sql.append(", ");

        sql.append(PROFILE_URL);
        sql.append(' ');
        sql.append(PROFILE_URL_T);

        sql.append(");");

        // create table
        db.execSQL(sql.toString());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createProductAliasCacheTable(db, TABLE_NAME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME + ";");
        onCreate(db);
    }
}
