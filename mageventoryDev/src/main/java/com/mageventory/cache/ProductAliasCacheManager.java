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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.mageventory.MyApplication;
import com.mageventory.model.Product;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.TrackerUtils;

/**
 * The manager classs for product alias cache database containing various
 * methdos to work with the cache
 * 
 * @author Eugene Popovich
 */
public class ProductAliasCacheManager {

    public static Object sCacheSynchronizationObject = new Object();

    private static ProductAliasCacheManager instance = new ProductAliasCacheManager(
            MyApplication.getContext());

    /**
     * @return an instance of ProductAliasCacheManager
     */
    public static ProductAliasCacheManager getInstance()
    {
        return instance;
    }

    /**
     * DB helper creates tables we use if they are not already created and helps
     * interface with the underlying database.
     */
    private ProductAliasCacheDBHelper mDbHelper;

    /** Reference to the underlying database. */
    private SQLiteDatabase mDB;

    private static String TAG = ProductAliasCacheManager.class.getSimpleName();

    /**
     * Add a product to alias cache or update its details
     * 
     * @param product
     * @param url
     * @return
     */
    public boolean addOrUpdate(Product product, String url) {
        long start = System.currentTimeMillis();
        synchronized (sCacheSynchronizationObject) {
            String barCode = product.getBarcode(null);
            CommonUtils
                    .debug(TAG,
                            "addOrUpdate: Adding a product info to cache: sku: %1$s; code: %2$s; id: %3$s; profile url: %4$s",
                            product.getSku(), barCode, product.getId(), url);
            dbWritableOpen();

            ContentValues cv = new ContentValues();
            boolean res = false;

            cv.put(ProductAliasCacheDBHelper.PRODUCT_BARCODE, barCode);

            cv.put(ProductAliasCacheDBHelper.PRODUCT_ID, product.getId());

            String whereClause = ProductAliasCacheDBHelper.PRODUCT_SKU + "=? AND "
                    + ProductAliasCacheDBHelper.PROFILE_URL + "=?";
            String[] whereValues = new String[] {
                    "" + product.getSku(),
                    "" + url
            };
            if (update(cv,
                    whereClause, whereValues)) {
                CommonUtils
                        .debug(TAG,
                                "addOrUpdate: Product with sku %1$s already present in cache for profile url %2$s. Updated",
                                product.getSku(), url);
                res = true;
            } else
            {

                CommonUtils
                        .debug(TAG,
                                "addOrUpdate: Product with sku %1$s is absent in cache for profile url %2$s. Adding...",
                                product.getSku(), url);
                cv.put(ProductAliasCacheDBHelper.PRODUCT_SKU, product.getSku());
                cv.put(ProductAliasCacheDBHelper.PROFILE_URL, url);
                res = insert(cv);
            }

            dbClose();
            TrackerUtils.trackDataLoadTiming(System.currentTimeMillis() - start,
                    "addOrUpdate", TAG);
            return res;
        }
    }

    /**
     * Update sku in the DB
     * 
     * @param skuFrom
     * @param skuTo
     * @param url
     * @return
     */
    public boolean updateSkuIfExists(String skuFrom, String skuTo, String url) {
        synchronized (sCacheSynchronizationObject) {
            CommonUtils.debug(TAG,
                    "updateSkuIfExists: updating sku from %1$s to %2$s for url %3$s",
                    skuFrom, skuTo, url);
            dbWritableOpen();

            ContentValues cv = new ContentValues();
            boolean res = false;

            cv.put(ProductAliasCacheDBHelper.PRODUCT_SKU, skuTo);

            String whereClause = ProductAliasCacheDBHelper.PRODUCT_SKU + "=? AND "
                    + ProductAliasCacheDBHelper.PROFILE_URL + "=?";
            String[] whereValues = new String[] {
                    "" + skuFrom,
                    "" + url
            };
            if (update(cv,
                    whereClause, whereValues)) {
                CommonUtils
                        .debug(TAG,
                                "updateSkuIfExists: Updated sku from %1$s to %2$s for url %3$s",
                                skuFrom, skuTo, url);
                res = true;
            } else
            {

                CommonUtils
                        .debug(TAG,
                                "updateSkuIfExists: sku %1$s for url %2$s was not found",
                                skuFrom, url);
            }

            dbClose();
            return res;
        }
    }

    /**
     * Get the cached sku for barcode if exists
     * 
     * @param barCode
     * @param url
     * @return
     */
    public String getCachedSkuForBarcode(String barCode, String url)
    {
        if (TextUtils.isEmpty(barCode))
        {
            return null;
        }
        String result = null;
        long start = System.currentTimeMillis();
        synchronized (sCacheSynchronizationObject) {
            CommonUtils.debug(TAG,
                    "getCachedSkuForBarcode: started for barCode %1$s and url %2$s", barCode,
                    url);

            dbReadableOpen();
            Cursor c;

            c = query(new String[] {
                    ProductAliasCacheDBHelper.PRODUCT_SKU,
            },
                    ProductAliasCacheDBHelper.PRODUCT_BARCODE + "=? AND "
                            + ProductAliasCacheDBHelper.PROFILE_URL + "=?",
                    new String[] {
                            "" + barCode,
                            "" + url
                    }, null, null);
            try
            {
                if (c.moveToFirst() == true) {
                    result = c.getString(c.getColumnIndex(ProductAliasCacheDBHelper.PRODUCT_SKU));
                    CommonUtils
                            .debug(TAG,
                                    "getCachedSkuForBarcode: found sku %1$s for barCode %2$s and url %3$s",
                                    result,
                                    barCode, url);
                }
            } finally
            {
                closeCursor(c);
            }

            dbClose();
        }
        TrackerUtils.trackDataLoadTiming(System.currentTimeMillis() - start,
                "getCachedSkuForBarcode", TAG);
        return result;
    }

    /**
     * Delete product information from cache for profile url
     * 
     * @param product
     * @param url
     * @return
     */
    public boolean deleteProductFromCache(Product product, String url) {
        return deleteProductFromCache(product.getSku(), url);
    }

    /**
     * Delete product information from cache for profile url and sku
     * 
     * @param sku
     * @param url
     * @return
     */
    public boolean deleteProductFromCache(String sku, String url) {
        synchronized (sCacheSynchronizationObject) {
            CommonUtils.debug(TAG,
                    "deleteProductFromCache: started for product with sku %1$s and url %2$s",
                    sku, url);

            dbWritableOpen();
            boolean del_res;

            /* Delete the specified job from the queue */
            del_res = (delete(ProductAliasCacheDBHelper.PRODUCT_SKU + "=? AND "
                    + ProductAliasCacheDBHelper.PROFILE_URL + "=?",
                    new String[] {
                            "" + sku,
                            "" + url
                    }) > 0);

            if (del_res) {
                CommonUtils
                        .debug(TAG,
                                "deleteProductFromCache: successfully done for product with sku %1$s and url %2$s",
                                sku, url);
            } else
            {
                CommonUtils
                        .debug(TAG,
                                "deleteProductFromCache: not found for product with sku %1$s and url %2$s",
                                sku, url);

            }

            dbClose();

            return del_res;
        }
    }

    /**
     * Delete products information from cache for profile url
     * 
     * @param url
     * @return
     */
    public boolean deleteProductsFromCache(String url) {
        synchronized (sCacheSynchronizationObject) {
            CommonUtils.debug(TAG,
                    "deleteProductsFromCache: started for url %1$s",
                    url);

            dbWritableOpen();
            boolean del_res;

            /* Delete the specified job from the queue */
            del_res = (delete(ProductAliasCacheDBHelper.PROFILE_URL + "=?",
                    new String[] {
                        "" + url
                    }) > 0);

            if (del_res) {
                CommonUtils
                        .debug(TAG,
                                "deleteProductsFromCache: successfully done for url %1$s",
                                url);
            } else
            {
                CommonUtils
                        .debug(TAG,
                                "deleteProductsFromCache: not found for url %1$s",
                                url);

            }

            dbClose();

            return del_res;
        }
    }

    /*
     * Wipe all data from both tables. Use only if there are no jobs currently
     * being executed.
     */
    public void wipeTable()
    {
        synchronized (sCacheSynchronizationObject) {
            dbWritableOpen();
            delete(null, null);
            dbClose();
        }
    }

    public ProductAliasCacheManager(Context context) {
        mDbHelper = new ProductAliasCacheDBHelper(context);
    }

    private boolean insert(ContentValues values) {
        final long id = mDB.insert(ProductAliasCacheDBHelper.TABLE_NAME, null, values);
        if (id == -1) {
            return false;
        }
        return true;
    }

    private int delete(String selection, String[] selectionArgs) {
        return mDB.delete(ProductAliasCacheDBHelper.TABLE_NAME, selection, selectionArgs);
    }

    private Cursor query(String[] columns, String selection, String[] selectionArgs,
            String sortOrder, String limit) {
        return mDB.query(ProductAliasCacheDBHelper.TABLE_NAME, columns, selection, selectionArgs,
                null, null, sortOrder, limit);
    }

    private boolean update(ContentValues values, String selection, String[] selectionArgs) {
        int count = mDB.update(ProductAliasCacheDBHelper.TABLE_NAME, values, selection,
                selectionArgs);

        if (count < 1) {
            return false;
        }
        return true;
    }

    private void dbWritableOpen() {
        mDB = mDbHelper.getWritableDatabase();
    }

    private void dbReadableOpen() {
        mDB = mDbHelper.getReadableDatabase();
    }

    private void dbClose() {
        mDB.close();
    }

    public void closeCursor(Cursor cursor)
    {
        try
        {
            cursor.close();
        } catch (Exception ex)
        {
            GuiUtils.noAlertError(TAG, null, ex);
        }
    }
}
