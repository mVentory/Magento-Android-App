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

package com.mageventory.res.util;

import java.util.List;

import com.mageventory.MyApplication;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Product;
import com.mageventory.model.Product.SiblingInfo;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.AbstractLoadProductTask;
import com.mageventory.tasks.AbstractSimpleLoadTask.SynchrnonousResourceLoader;
import com.mageventory.util.CommonUtils;

/**
 * Utilities for the product resource loading operation
 * 
 * @author Eugene Popovich
 */
public class ProductResourceUtils {

    /**
     * The tag used for logging
     */
    static final String TAG = ProductResourceUtils.class.getSimpleName();

    /**
     * Common {@link SynchrnonousResourceLoader} implementation for the product resource
     * loading
     */
    public static class ProductResourceLoader extends SynchrnonousResourceLoader {
        /**
         * The SKU or Barcode of the product to load
         */
        String mSku;
        /**
         * The settings snapshot
         */
        SettingsSnapshot mSettingsSnapshot;

        /**
         * @param sku the SKU or Barcode of the product to load
         * @param settingsSnapshot the settings snapshot
         * @param resHelper resource loading helper
         */
        public ProductResourceLoader(String sku, SettingsSnapshot settingsSnapshot,
                ResourceServiceHelper resHelper) {
            super(resHelper);
            mSku = sku;
            mSettingsSnapshot = settingsSnapshot;
        }

        @Override
        public int requestLoadResource() {
            return AbstractLoadProductTask.requestLoadProduct(mSku, mSettingsSnapshot, resHelper);
        }

        @Override
        public boolean isCancelled() {
            // always return false such as it is not task and can't be cancelled
            return false;
        }
    }

    /**
     * Reload the details for the product siblings asynchronously.
     * 
     * @param reloadProduct whether the product details should be reloaded also
     *            synchronously
     * @param product the product to reload siblings for
     * @param url the profile url
     * @param returns true if reloadProduct was not requested or it was
     *            requested and data loaded successfully, false otherwise
     */
    public static boolean reloadSiblings(boolean reloadProduct, Product product, String url) {
        ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
        SettingsSnapshot settingsSnapshot = new SettingsSnapshot(MyApplication.getContext());
        settingsSnapshot.setUrl(url);
        boolean result = true;
        if (reloadProduct) {
            // if product details reload is required
            CommonUtils.debug(TAG, "reloadSiblings: reloading product");
            SynchrnonousResourceLoader loader = new ProductResourceUtils.ProductResourceLoader(
                    product.getSku(), settingsSnapshot, resHelper);
            if (loader.loadGeneral()) {
                // if product was reloaded from the server successfully
                product = JobCacheManager.restoreProductDetails(product.getSku(), url);
            } else {
                CommonUtils.error(TAG, "reloadSiblings: failed to reload product details");
                // product reload failed
                result = false;
            }
        }
        List<SiblingInfo> siblings = product.getSiblingsList();
        // iterate through product siblings and request loading of product
        // details for each
        for (SiblingInfo sibling : siblings) {
            CommonUtils.debug(TAG, "reloadSiblings: reloading sibling %1$s", sibling.getSku());
            AbstractLoadProductTask.requestLoadProduct(sibling.getSku(), settingsSnapshot,
                    resHelper);
        }
        return result;
    }

}
