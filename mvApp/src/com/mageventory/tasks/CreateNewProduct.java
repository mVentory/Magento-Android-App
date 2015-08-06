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

package com.mageventory.tasks;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.ProductCreateActivity;
import com.mageventory.activity.ProductDetailsActivity;
import com.mageventory.activity.base.BaseActivityCommon;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.jobprocessor.util.UploadImageJobUtils;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.Product;
import com.mageventory.model.util.CategoryUtils;
import com.mageventory.model.util.ProductUtils;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.run.CallableWithParameterAndResult;
import com.mventory.R;

public class CreateNewProduct extends AsyncTask<Void, Void, Integer> implements
        MageventoryConstants {

    public static final String TAG = CreateNewProduct.class.getSimpleName();

    private ProductCreateActivity mHostActivity;
    private JobControlInterface mJobControlInterface;

    private static int FAILURE = 0;
    private static int E_BAD_FIELDS = 1;
    private static int E_SKU_ALREADY_EXISTS = 3;
    private static int SUCCESS = 4;
    /**
     * Update is pending result code
     */
    private static int UPDATE_PENDING = 5;

    private String mNewSKU;
    private boolean mQuickSellMode;
    private SettingsSnapshot mSettingsSnapshot;

    public CreateNewProduct(ProductCreateActivity hostActivity, boolean quickSellMode) {
        mHostActivity = hostActivity;
        mJobControlInterface = new JobControlInterface(mHostActivity);
        mQuickSellMode = quickSellMode;
    }

    private static class IncompleteDataException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public IncompleteDataException() {
            super();
        }

        public IncompleteDataException(String detailMessage) {
            super(detailMessage);
        }

    }

    private Map<String, Object> extractData(Bundle bundle, boolean exceptionOnFail)
            throws IncompleteDataException {
        // @formatter:off
        final String[] stringKeys = {
                MAGEKEY_PRODUCT_NAME, MAGEKEY_PRODUCT_PRICE, MAGEKEY_PRODUCT_WEBSITE,
                MAGEKEY_PRODUCT_DESCRIPTION, MAGEKEY_PRODUCT_SHORT_DESCRIPTION,
                MAGEKEY_PRODUCT_STATUS,
                MAGEKEY_PRODUCT_WEIGHT,
                MAGEKEY_PRODUCT_SPECIAL_PRICE,
                MAGEKEY_PRODUCT_SPECIAL_FROM_DATE,
                MAGEKEY_PRODUCT_SPECIAL_TO_DATE,
        };
        // @formatter:on
        final Map<String, Object> productData = new HashMap<String, Object>();
        for (final String stringKey : stringKeys) {
            productData.put(stringKey, extractString(bundle, stringKey, 
            		// do not throw exception if attribute is not available
                    exceptionOnFail && mHostActivity.isSpecialAttributeAvailable(stringKey)));
        }
        final Object cat = bundle.get(MAGEKEY_PRODUCT_CATEGORIES);
        if (cat != null && (cat instanceof Object[] == true || cat instanceof List)) {
            productData.put(MAGEKEY_PRODUCT_CATEGORIES, cat);
        }

        return productData;
    }

    private String extractString(final Bundle bundle, final String key,
            final boolean exceptionOnFail)
            throws IncompleteDataException {
        final String s = bundle.getString(key);
        if (s == null && exceptionOnFail) {
            throw new IncompleteDataException("bad data for key '" + key + "'");
        }
        return s == null ? "" : s;
    }

    private Map<String, Object> extractUpdate(Bundle bundle) throws IncompleteDataException {
        final String[] stringKeys = {
                MAGEKEY_PRODUCT_QUANTITY, MAGEKEY_PRODUCT_MANAGE_INVENTORY,
                MAGEKEY_PRODUCT_IS_IN_STOCK, MAGEKEY_PRODUCT_USE_CONFIG_MANAGE_STOCK,
                MAGEKEY_PRODUCT_IS_QTY_DECIMAL
        };
        // @formatter:on
        final Map<String, Object> productData = new HashMap<String, Object>();
        for (final String stringKey : stringKeys) {
            productData.put(stringKey, extractString(bundle, stringKey, true));
        }
        return productData;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        mSettingsSnapshot = new SettingsSnapshot(mHostActivity);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        try {
            if (mHostActivity == null || isCancelled()) {
                return FAILURE;
            }
    
            if (mHostActivity.verifyForm(!mQuickSellMode, true) == false) {
                return E_BAD_FIELDS;
            }
    
            /*
             * The request is missing a structure related to attributes when
             * compared to the response. We're building this structure (list of
             * maps) here to simulate the response.
             */
    
            final Bundle data = new Bundle();
            final Map<String, String> extracted = mHostActivity.extractCommonData();
    
            // user fields
            for (Map.Entry<String, String> e : extracted.entrySet()) {
                data.putString(e.getKey(), e.getValue());
            }
    
            // default values
            data.putString(MAGEKEY_PRODUCT_WEBSITE, TODO_HARDCODED_PRODUCT_WEBSITE);
    
            data.putString(MAGEKEY_PRODUCT_SKU,
                    mHostActivity.getSpecialAttributeValue(MAGEKEY_PRODUCT_SKU));
            data.putSerializable(MAGEKEY_PRODUCT_CATEGORIES,
                    CategoryUtils.getAsObjectArray(mHostActivity.selectedCategoryIds));
            // generated
            String quantity = mHostActivity.quantityV.getText().toString();
    
            /* 1 - status enabled, 2 - status disabled */
            int status = 1;
            String inventoryControl;
            String isQtyDecimal;
            String isInStock;
    
            if (!TextUtils.isEmpty(quantity))
            {
                inventoryControl = "1";
    
                /* See https://code.google.com/p/mageventory/issues/detail?id=148 */
                if (quantity.contains("."))
                {
                    isQtyDecimal = "1";
                }
                else
                {
                    isQtyDecimal = "0";
                }
    
                if (Double.parseDouble(quantity) > 0)
                {
                    isInStock = "1";
                }
                else
                {
                    isInStock = "0";
                }
            }
            else
            {
                isQtyDecimal = "0";
                inventoryControl = "0";
                quantity = "0";
                isInStock = "0";
            }
    
            data.putString(MAGEKEY_PRODUCT_QUANTITY, quantity);
            data.putString(MAGEKEY_PRODUCT_STATUS, "" + status);
            data.putString(MAGEKEY_PRODUCT_MANAGE_INVENTORY, inventoryControl);
            data.putString(MAGEKEY_PRODUCT_IS_IN_STOCK, isInStock);
            data.putString(MAGEKEY_PRODUCT_USE_CONFIG_MANAGE_STOCK, "0");
            data.putString(MAGEKEY_PRODUCT_IS_QTY_DECIMAL, isQtyDecimal);
    
            // attributes
            // bundle attributes
            final HashMap<String, Object> atrs = new HashMap<String, Object>();
    
            if (mHostActivity.customAttributesList.getList() != null) {
                for (CustomAttribute elem : mHostActivity.customAttributesList.getList()) {
                    atrs.put(elem.getCode(), elem.getSelectedValue());
                }
            }
    
            atrs.put(Product.MAGEKEY_PRODUCT_BARCODE,
                    mHostActivity.getSpecialAttributeValue(MAGEKEY_PRODUCT_BARCODE));
    
            data.putInt(EKEY_PRODUCT_ATTRIBUTE_SET_ID, mHostActivity.atrSetId);
            data.putSerializable(EKEY_PRODUCT_ATTRIBUTE_VALUES, atrs);
    
            /* Convert this data to a format understandable by the job service. */
            Map<String, Object> productRequestData = extractData(data, true);
            productRequestData.put(MAGEKEY_PRODUCT_TAX_CLASS_ID, "0");
    
            // extract attribute data
            final int attrSet = data.getInt(EKEY_PRODUCT_ATTRIBUTE_SET_ID, INVALID_ATTRIBUTE_SET_ID);
            @SuppressWarnings("unchecked")
            final Map<String, String> atrs2 = (Map<String, String>) data
                    .getSerializable(EKEY_PRODUCT_ATTRIBUTE_VALUES);
    
            if (atrs2 != null && atrs2.isEmpty() == false) {
                productRequestData.putAll(atrs2);
            }
    
            productRequestData.putAll(extractUpdate(data));
    
            mNewSKU = data.getString(MAGEKEY_PRODUCT_SKU);
    
            if (TextUtils.isEmpty(mNewSKU)) {
                // Empty Generate SKU
                mNewSKU = ProductUtils.generateSku();
            }
    
            productRequestData.put(MAGEKEY_PRODUCT_SKU, mNewSKU);
            productRequestData.put(EKEY_PRODUCT_ATTRIBUTE_SET_ID, new Integer(attrSet));
            // pass attribute set to the request data such as create job may be
            // switched to edit job lately in this method
            productRequestData.put(MAGEKEY_PRODUCT_ATTRIBUTE_SET_ID, Integer.toString(attrSet));
    
            /* Simulate a response from the server so that we can store it in cache. */
            Map<String, Object> productResponseData = new HashMap<String, Object>(productRequestData);
    
            /*
             * Filling the things that were missing in the request to simulate a
             * response.
             */
    
            productResponseData.put(MAGEKEY_PRODUCT_IMAGES, new Object[0]);
            productResponseData.put(MAGEKEY_PRODUCT_ID, INVALID_PRODUCT_ID);
    
            Product p = new Product(productResponseData);
    
            // whether the update job should be used instead of create flag
            boolean isUpdate = false;
            if (JobCacheManager.productDetailsExist(p.getSku(), mSettingsSnapshot.getUrl())) {
    
                /*
                 * It doesn't matter if the product already exists if we are in
                 * quicksell mode. Quick selling is still a valid thing to do in
                 * that case.
                 */
                if (mQuickSellMode)
                {
                    JobCacheManager.removeProductDetails(p.getSku(), mSettingsSnapshot.getUrl());
                }
                else
                {
                    if (TextUtils.isEmpty(mHostActivity.skuToLinkWith)) {
                        return E_SKU_ALREADY_EXISTS;
                    } else {
                        // set the update job should be used flag
                        isUpdate = true;
                        // init the updated attributes list so this information will
                        // not be cleared during adding of edit job and merging job
                        // with product details
                        UpdateProduct.initUpdatedAttributes(
                                productRequestData,
                                JobCacheManager.restoreProductDetails(p.getSku(),
                                        mSettingsSnapshot.getUrl()),
                                mHostActivity.customAttributesList.getList());
                    }
                }
            }
    
            JobID jobID = new JobID(INVALID_PRODUCT_ID, isUpdate ? RES_CATALOG_PRODUCT_UPDATE
                    : RES_CATALOG_PRODUCT_CREATE, mNewSKU, null);
            Job job = new Job(jobID, mSettingsSnapshot);
            job.setExtras(productRequestData);
    
            /*
             * Inform lower layer about which product creation mode was selected by
             * the user (quick sell mode or normal mode)
             */
            job.putExtraInfo(EKEY_QUICKSELLMODE, new Boolean(mQuickSellMode));
            job.putExtraInfo(EKEY_DUPLICATIONMODE, new Boolean(
                    mHostActivity.productToDuplicatePassed != null
                            && !mHostActivity.duplicateRemovedProductMode));
            job.putExtraInfo(EKEY_PRODUCT_SKU_TO_DUPLICATE, mHostActivity.productSKUtoDuplicate);
            job.putExtraInfo(EKEY_DUPLICATION_PHOTO_COPY_MODE, mHostActivity.copyPhotoMode);
            job.putExtraInfo(EKEY_DECREASE_ORIGINAL_QTY, mHostActivity.decreaseOriginalQTY);
    
            if (!TextUtils.isEmpty(mHostActivity.skuToLinkWith)) {
                // if product should be linked with another product
                job.putExtraInfo(MAGEKEY_API_LINK_WITH_PRODUCT, mHostActivity.skuToLinkWith);
                // set the flag that if product found during create operation it
                // should be updated
                job.putExtraInfo(MAGEKEY_API_UPDATE_IF_EXISTS, new Boolean(true));
            }
    
            // flag indicating whether the job was added successfully
            boolean addJobResult = true;
            if (isUpdate) {
                // product details exists so edit job is used instead of create
                addJobResult = mJobControlInterface.addEditJob(job);
            } else {
                mJobControlInterface.addJob(job);
    
                p.getData().put(MAGEKEY_PRODUCT_LAST_USED_QUERY, mHostActivity.lastUsedQuery);
    
                JobCacheManager.storeProductDetailsWithMergeSynchronous(p, mSettingsSnapshot.getUrl());
            }
            
            // handle images to upload if exist
            for (String path : mHostActivity.addedImages) {
                // iterate through paths of the images which should be uploaded
                // for the product
            	
            	// create image upload job for already known SKU
                Job uploadImageJob = UploadImageJobUtils.createImageUploadJob(mNewSKU, path, false,
                        new CallableWithParameterAndResult<File, String>() {

                            @Override
                            public String call(File p) {
                                return p.getName();
                            }
                        }, mSettingsSnapshot);
                // schedule job execution
                mJobControlInterface.addJob(uploadImageJob);
            }
            /* Store additional values in the input cache. */
            mHostActivity.updateInputCacheWithCurrentValues();
    
            /*
             * Save the state of product create activity in permanent storage for
             * the user to be able to restore it next time when creating a product.
             */
            SharedPreferences preferences;
            preferences = mHostActivity.getSharedPreferences(
                    ProductCreateActivity.PRODUCT_CREATE_SHARED_PREFERENCES, Context.MODE_PRIVATE);
    
            SharedPreferences.Editor editor = preferences.edit();
    
            editor.putString(ProductCreateActivity.PRODUCT_CREATE_SHORT_DESCRIPTION,
                    mHostActivity.getSpecialAttributeValue(MAGEKEY_PRODUCT_SHORT_DESCRIPTION));
            editor.putString(ProductCreateActivity.PRODUCT_CREATE_DESCRIPTION,
                    mHostActivity.getSpecialAttributeValue(MAGEKEY_PRODUCT_DESCRIPTION));
            editor.putString(ProductCreateActivity.PRODUCT_CREATE_WEIGHT,
                    mHostActivity.getSpecialAttributeValue(MAGEKEY_PRODUCT_WEIGHT));
            editor.putInt(ProductCreateActivity.PRODUCT_CREATE_ATTRIBUTE_SET, mHostActivity.atrSetId);
    
            editor.putInt(ProductCreateActivity.PRODUCT_CREATE_CATEGORY, INVALID_CATEGORY_ID);
    
            editor.commit();
    
            if (mHostActivity.customAttributesList != null)
                mHostActivity.customAttributesList.saveInCache();
    
            return addJobResult ? SUCCESS : UPDATE_PENDING;
        } catch (Exception ex) {
            CommonUtils.error(TAG, ex);
        }
        return FAILURE;
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        if (mHostActivity == null) {
            return;
        }

        mHostActivity.dismissProgressDialog();

        if (result == SUCCESS) {

            /* NewNewReloadCycle starts here. */
            BaseActivityCommon.sNewNewReloadCycle = true;

            // successful creation, launch product details activity
            final String ekeyProductSKU = mHostActivity.getString(R.string.ekey_product_sku);
            final String ekeyNewProduct = mHostActivity.getString(R.string.ekey_new_product);
            final String ekeyGalleryTimestamp = mHostActivity
                    .getString(R.string.ekey_gallery_timestamp);

            final Intent intent = new Intent(mHostActivity, ProductDetailsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(ekeyProductSKU, mNewSKU);
            intent.putExtra(ekeyNewProduct, true);

            if (mHostActivity.mGalleryTimestamp != 0)
            {
                intent.putExtra(ekeyGalleryTimestamp, mHostActivity.mGalleryTimestamp);
            }

            mHostActivity.startActivity(intent);

        } else if (result == FAILURE) {
            GuiUtils.alert("Creation failed...");
        } else if (result == E_BAD_FIELDS) {
            GuiUtils.alert("Please fill out all fields...");
        } else if (result == UPDATE_PENDING) {
            GuiUtils.alert(R.string.update_being_processed_please_wait);
        }

        if (result != E_SKU_ALREADY_EXISTS)
        {
            mHostActivity.finish();
        }
        else
        {
            mHostActivity.createNewProductCalled = false;
            mHostActivity.showKnownSkuDialog(mNewSKU);
        }
    }
}
