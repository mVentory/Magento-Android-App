
package com.mageventory.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mageventory.MageventoryConstants;
import com.mageventory.job.Job;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.model.Product;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.SimpleAsyncTask;
import com.mventory.R;

/**
 * The abstract implementation of the asynchronous task to add product edit job
 * for the update categories action
 * 
 * @author Eugene Popovich
 */
public abstract class AbstractUpdateProductCategoriesTask extends SimpleAsyncTask implements
        MageventoryConstants {
    /**
     * Tag used for logging
     */
    private static final String TAG = AbstractUpdateProductCategoriesTask.class.getSimpleName();
    /**
     * The settings snapshot
     */
    private SettingsSnapshot mSettingsSnapshot;
    /**
     * The product to update categories for
     */
    private Product mProduct;
    /**
     * The category IDs to associate with the product
     */
    private Collection<Integer> mCategoryIds;
    /**
     * The job controller interface
     */
    protected JobControlInterface mJobControlInterface;

    /**
     * The flag holding the result of the add job action to determine update
     * pending state in the onFailedPostExecute method
     */
    boolean mAddJobResult = true;

    /**
     * @param categoryIds the new category IDs to associate with the product
     * @param product The product for which the categories should be updated
     * @param jobControlInterface The job controller interface
     * @param settings The settings snapshot
     * @param loadingControl the loading control for the progress indication
     */
    public AbstractUpdateProductCategoriesTask(Collection<Integer> categoryIds, Product product,
            JobControlInterface jobControlInterface,
            SettingsSnapshot settings, LoadingControl loadingControl) {
        super(loadingControl);
        mCategoryIds = categoryIds;
        mProduct = product;
        mSettingsSnapshot = settings;
        mJobControlInterface = jobControlInterface;
    }


    @Override
    protected Boolean doInBackground(Void... args) {
        try {
            // prepare product request data
            final Map<String, Object> productRequestData = new HashMap<String, Object>();
            productRequestData.put(MAGEKEY_PRODUCT_SKU, mProduct.getSku());

            if (mCategoryIds != null) {
                List<Object> categoryIdsList = new ArrayList<Object>();
                for (Integer categoryId : mCategoryIds) {
                    if (categoryId.intValue() != INVALID_CATEGORY_ID) {
                        categoryIdsList.add(categoryId.toString());
                    }
                }
                Object[] categoryIds = new Object[categoryIdsList.size()];
                categoryIds = categoryIdsList.toArray(categoryIds);
                productRequestData.put(MAGEKEY_PRODUCT_CATEGORIES, categoryIdsList);
            }
            UpdateProduct.initUpdatedAttributes(productRequestData, mProduct, null, null);
            
            // create product edit job
            JobID jobID = new JobID(INVALID_PRODUCT_ID, RES_CATALOG_PRODUCT_UPDATE,
                    mProduct.getSku(), null);
            Job job = new Job(jobID, mSettingsSnapshot);
            job.setExtras(productRequestData);

            mAddJobResult = mJobControlInterface.addEditJob(job);

            // do some extra background job if necessary
            doExtraJobInBackground();
            return mAddJobResult && !isCancelled();
        } catch (Exception ex) {
            CommonUtils.error(TAG, ex);
        }
        return false;

    }

    @Override
    protected void onFailedPostExecute() {
        super.onFailedPostExecute();
        if (mAddJobResult) {
            // the code didn't reach the addEditJob place or some other error
            // occurred
            GuiUtils.alert(R.string.errorGeneral);
        } else {
            // there are pending update product jobs, show relevant error
            // message
            GuiUtils.alert(R.string.update_being_processed_please_wait);
        }
    }

    /**
     * The method which is called from the doInBackground method which children
     * classes may override to execute some extra job in the background
     */
    protected void doExtraJobInBackground() {
    }

    /**
     * Get the product
     * 
     * @return
     */
    public Product getProduct() {
        return mProduct;
    }
}