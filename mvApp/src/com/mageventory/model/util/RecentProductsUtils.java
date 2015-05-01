
package com.mageventory.model.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCacheManager.GalleryTimestampRange;
import com.mageventory.job.JobCacheManager.ProductDetailsExistResult;
import com.mageventory.model.Product;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;

/**
 * Various utilities to work with the recent products
 * 
 * @author Eugene Popovich
 */
public class RecentProductsUtils {
    /**
     * The tag used for the logging
     */
    public static final String TAG = RecentProductsUtils.class.getSimpleName();

    /**
     * Iterate through recent products related to the currently selected profile
     * and process them
     * 
     * @param settingsSnapshot the settings snapshot
     * @param params the parameters implementation used by the method
     * @return true in case data was properly processed, false otherwise
     */
    public static boolean iterateThroughRecentProducts(SettingsSnapshot settingsSnapshot,
            IterateThroughRecentProductsParams params) {
        synchronized (JobCacheManager.sSynchronizationObject) {
            ArrayList<GalleryTimestampRange> galleryTimestampsRangesArray = JobCacheManager
                    .getGalleryTimestampRangesArray();

            if (galleryTimestampsRangesArray != null) {
                Set<String> addedProducts = new HashSet<String>();
                // collection of already checked SKUs to do not search for
                // same data twice
                Set<String> processedSkus = new HashSet<String>();
                // variable to count processed unique SKUs
                int processedCount = 0;
                // maximum allowed unique SKUs check to avoid
                // performance problem. The performance problem may
                // occur if there are few hundreds gallery timestamp
                // records and user cleared the cache
                final int maxSearchDeep = 10;
                for (int i = galleryTimestampsRangesArray.size() - 1; i >= 0; i--) {
                    if (params.isCancelled()) {
                        return false;
                    }
                    GalleryTimestampRange gts = galleryTimestampsRangesArray.get(i);
                    if (gts.profileID != settingsSnapshot.getProfileID()) {
                        // skip the record not related to the currently
                        // selected profile
                        continue;
                    }
                    String sku = gts.sku;
                    if (processedSkus.contains(sku)) {
                        // skip SKUs which was checked earlier
                        continue;
                    }
                    // remember that SKU is processed
                    processedSkus.add(sku);
                    ProductDetailsExistResult existResult = JobCacheManager.productDetailsExist(
                            sku, settingsSnapshot.getUrl(), true);
                    if (existResult.isExisting()) {
                        if (addedProducts.contains(existResult.getSku())) {
                            CommonUtils
                                    .debug(TAG,
                                            "iterateThroughRecentProducts: recent product with SKU %1$s already added. Skipping.",
                                            existResult.getSku());
                        } else {
                            Product p = JobCacheManager.restoreProductDetails(existResult.getSku(),
                                    settingsSnapshot.getUrl());
                            if (p != null) {
                                addedProducts.add(existResult.getSku());
                                if (!params.processRecentProduct(p)) {
                                    break;
                                }
                            }
                        }
                    } else {
                        CommonUtils
                                .debug(TAG,
                                        "iterateThroughRecentProducts: recent product with SKU %1$s doesn't have cached details",
                                        sku);
                    }
                    if (processedCount++ >= maxSearchDeep) {
                        CommonUtils.debug(TAG,
                                "iterateThroughRecentProducts: reached max search deep. Breaking.");
                        break;
                    }
                }
            }
        }
        return !params.isCancelled();
    }

    /**
     * Interface used as a parameter in the
     * {@link RecentProductsUtils#iterateThroughRecentProducts(SettingsSnapshot, IterateThroughRecentProductsParams)}
     * method
     */
    public static interface IterateThroughRecentProductsParams {
        /**
         * Whether the executiong should be cancelled
         * 
         * @return
         */
        boolean isCancelled();

        /**
         * Process the found recent product
         * 
         * @param product
         * @return true if product is processed and execution should be
         *         continued, false in case no more recent products are required
         */
        boolean processRecentProduct(Product product);

    }
}
