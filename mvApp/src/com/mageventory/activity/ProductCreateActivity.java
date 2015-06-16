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

package com.mageventory.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;

import com.mageventory.activity.base.BaseActivityCommon;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributeSimple;
import com.mageventory.model.Product;
import com.mageventory.model.util.ProductUtils;
import com.mageventory.model.util.ProductUtils.PricesInformation;
import com.mageventory.resprocessor.ProductDetailsProcessor.ProductDetailsLoadException;
import com.mageventory.settings.Settings;
import com.mageventory.tasks.BookInfoLoader;
import com.mageventory.tasks.CreateNewProduct;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ScanUtils.ScanResult;
import com.mventory.R;

public class ProductCreateActivity extends AbsProductActivity {

	/**
	 * The key for the scan result intent extra
	 */
    public static final String SCAN_RESULT = "SCAN_RESULT";
    public static final String PRODUCT_CREATE_ATTRIBUTE_SET = "attribute_set";
    /**
     * The key for the shared preferences to save last used short description
     * for the new product
     */
    public static final String PRODUCT_CREATE_SHORT_DESCRIPTION = "shortDescription";
    public static final String PRODUCT_CREATE_DESCRIPTION = "description";
    public static final String PRODUCT_CREATE_WEIGHT = "weight";
    public static final String PRODUCT_CREATE_CATEGORY = "category";
    public static final String PRODUCT_CREATE_SHARED_PREFERENCES = "ProductCreateSharedPreferences";

    private SharedPreferences preferences;

    @SuppressWarnings("unused")
    private static final String TAG = "ProductCreateActivity";
    private static final String[] MANDATORY_USER_FIELDS = {};

    // views
    private Button mCreateButton;

    // dialogs
    private ProgressDialog progressDialog;
    private boolean firstTimeAttributeSetResponse = true;
    private boolean firstTimeAttributeListResponse = true;

    public float decreaseOriginalQTY;
    public String copyPhotoMode;
    /**
     * Scan result intent extra if exists. May be generated from the
     * productSKUPassed if absent. It is used to handle passed scanned code data
     * in the same way as used when user scans code by clicking on the SKU or
     * Barcode input field (
     * {@link #codeScanCommon(String, ScanResult, int, boolean, boolean)} method
     * usage)
     */
    private ScanResult mScanResultPassed;
    private String productSKUPassed;
    public String productSKUtoDuplicate;
    public Product productToDuplicatePassed;
    public boolean allowToEditInDupliationMode;
    public boolean duplicateRemovedProductMode;
    private ProductDetailsLoadException skuExistsOnServerUncertaintyPassed;
    private boolean mLoadLastAttributeSetAndCategory;
    /**
     * The last used search query for the product. Temp storage because product
     * details are not yet saved
     */
    public String lastUsedQuery;

    /**
     * The paths to the added images for the product. Temporary storage because
     * product details not yet saved. The stored paths will be processed in the
     * {@link CreateNewProduct} task and the image upload job will be added for
     * each
     */
    public List<String> addedImages = new ArrayList<String>();

    private boolean mSKUExistsOnServerUncertaintyDialogActive = false;
    
    /**
     * Contains Barcode was scanned intent extra value
     */
    private boolean mBarcodeScanned;
    /**
     * Contains skip timestamp update intent extra value
     */
    private boolean mSkipTimestampUpdate;

    /*
     * Show dialog that informs the user that we are uncertain whether the
     * product with a scanned SKU is present on the server or not (This will be
     * only used in case when we get to "product create" activity from "scan"
     * activity)
     */
    public void showSKUExistsOnServerUncertaintyDialog()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.info);
        boolean isOnline = CommonUtils.isOnline();
        if (!isOnline)
        {
            alert.setMessage(CommonUtils
                    .getStringResource(R.string.cant_connect_to_server_to_check_code_offline_work));
        } else
        {
            alert.setMessage(CommonUtils.getStringResource(R.string.cannot_check_sku_default,
                    productSKUPassed));
        }

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                skuExistsOnServerUncertaintyDialogDestroyed();
            }
        });
        alert.setOnCancelListener(new OnCancelListener() {
            
            @Override
            public void onCancel(DialogInterface dialog) {
                skuExistsOnServerUncertaintyDialogDestroyed();
            }

        });
        AlertDialog srDialog = alert.create();
        mSKUExistsOnServerUncertaintyDialogActive = true;
        srDialog.show();
    }

    private void skuExistsOnServerUncertaintyDialogDestroyed() {
        mSKUExistsOnServerUncertaintyDialogActive = false;
        if (!firstTimeAttributeSetResponse) {
            showAttributeSetList();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        absOnCreate();

        mLoadLastAttributeSetAndCategory = BaseActivityCommon.sNewNewReloadCycle;

        preferences = getSharedPreferences(PRODUCT_CREATE_SHARED_PREFERENCES, Context.MODE_PRIVATE);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            productSKUPassed = extras.getString(getString(R.string.ekey_product_sku));
            mScanResultPassed = extras.getParcelable(SCAN_RESULT);
            productSKUtoDuplicate = extras
                    .getString(getString(R.string.ekey_product_sku_to_duplicate));
            skuExistsOnServerUncertaintyPassed = extras
                    .getParcelable(getString(R.string.ekey_sku_exists_on_server_uncertainty));
            productToDuplicatePassed = (Product) extras
                    .getSerializable(getString(R.string.ekey_product_to_duplicate));
            allowToEditInDupliationMode = extras
                    .getBoolean(getString(R.string.ekey_allow_to_edit_in_duplication_mode));
            duplicateRemovedProductMode = extras
                    .getBoolean(getString(R.string.ekey_duplicate_removed_product_mode));
            copyPhotoMode = extras.getString(getString(R.string.ekey_copy_photo_mode));
            decreaseOriginalQTY = extras.getFloat(getString(R.string.ekey_decrease_original_qty));
            mGalleryTimestamp = extras.getLong(getString(R.string.ekey_gallery_timestamp), 0);
            mSkipTimestampUpdate = extras.getBoolean(
                    getString(R.string.ekey_skip_timestamp_update), false);

            mBarcodeScanned = extras.getBoolean(getString(R.string.ekey_barcode_scanned),
                    false);

            /*
             * Not sure whether this product is on the server. Show info about
             * this problem.
             */
            if (skuExistsOnServerUncertaintyPassed != null)
            {
                showSKUExistsOnServerUncertaintyDialog();
            }

        }
        if (productToDuplicatePassed != null) {
            priceHandler.setDataFromProduct(productToDuplicatePassed);

            double dupQty = 0;

            if (decreaseOriginalQTY > 0) {
                dupQty = decreaseOriginalQTY;
            } else {
                dupQty = 1;
            }
            ProductUtils.setQuantityTextValueAndAdjustViewType(dupQty, quantityV,
                    productToDuplicatePassed);

        } else {
            ProductUtils.adjustQuantityViewInputType(quantityV, true);
            quantityV.setText(ProductUtils.getQuantityString(false, 1));
        }
        if (productToDuplicatePassed == null)
        {
            Settings settings = new Settings(this);
        }

        // listeners
        mCreateButton = (Button) findViewById(R.id.saveBtn);
        mCreateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ProductCreateActivity.this.hideKeyboard();

                // check whether we have active existing check task and do not
                // allow to save if it is still running
                if (checkCodeValidationRunning()) {
                    return;
                }
                /*
                 * It is not possible for the user to create a product if some
                 * custom attribute options are being created.
                 */
                if (!newOptionPendingLoadingControl.isLoading()) {
                    if (atrSetId == INVALID_ATTRIBUTE_SET_ID) {
                        showSelectAttributeSetDialog();
                    } else {
                        if (verifyForm(true, false)) {
                            createNewProduct(false);
                        }
                    }
                } else {
                    GuiUtils.alert("Wait for options creation...");
                }
            }
        });

        attributeSetV.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                attributeSetLongTap = true;

                String shortDescription = preferences.getString(PRODUCT_CREATE_SHORT_DESCRIPTION,
                        "");
                String description = preferences.getString(PRODUCT_CREATE_DESCRIPTION, "");
                String weight = preferences.getString(PRODUCT_CREATE_WEIGHT, "");

                setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_SHORT_DESCRIPTION,
                        shortDescription, true, true);
                setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_DESCRIPTION, description, true,
                        true);
                setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_WEIGHT, weight, true, true);

                loadLastAttributeSet(true);

                return true;
            }
        });
    }

    public void showSelectAttributeSetDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.missing_data);
        alert.setMessage(R.string.please_specify_product_type);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showAttributeSetList();
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }
    
    private void loadLastAttributeSet(boolean loadLastUsedCustomAttribs)
    {
        int lastAttributeSet = preferences
                .getInt(PRODUCT_CREATE_ATTRIBUTE_SET, INVALID_CATEGORY_ID);

        selectAttributeSet(lastAttributeSet, false, loadLastUsedCustomAttribs);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /*
     * Make sure this function is called only once before the CreateNewProduct
     * task is launched.
     */
    public boolean createNewProductCalled = false;

    private void createNewProduct(boolean quickSellMode) {
        synchronized (this)
        {
            if (createNewProductCalled == false)
            {
                createNewProductCalled = true;
                showProgressDialog("Creating product...");

                CreateNewProduct createTask = new CreateNewProduct(this, quickSellMode);
                createTask.execute();
            }
        }
    }

    public Map<String, String> extractCommonData() {
        final Map<String, String> data = new HashMap<String, String>();

        String name = getProductName(this);
        String price = priceV.getText().toString();
        String description = getSpecialAttributeValue(MAGEKEY_PRODUCT_DESCRIPTION);
        String weight = getSpecialAttributeValue(MAGEKEY_PRODUCT_WEIGHT);

        if (TextUtils.isEmpty(price)) {
            price = "0";
        }

        if (TextUtils.isEmpty(description)) {
            description = "";
        }

        if (TextUtils.isEmpty(weight)) {
            weight = "0";
        }

        data.put(MAGEKEY_PRODUCT_NAME, name);
        PricesInformation pricesInformation = ProductUtils.getPricesInformation(price);
        if (pricesInformation != null) {
            data.put(MAGEKEY_PRODUCT_PRICE,
                    CommonUtils.formatNumberIfNotNull(pricesInformation.regularPrice));
        } else {
            data.put(MAGEKEY_PRODUCT_PRICE, CommonUtils.formatNumberIfNotNull(0));
        }
        if (pricesInformation != null && pricesInformation.specialPrice != null) {
            data.put(MAGEKEY_PRODUCT_SPECIAL_PRICE,
                    CommonUtils.formatNumber(pricesInformation.specialPrice));
            data.put(MAGEKEY_PRODUCT_SPECIAL_FROM_DATE, CommonUtils.formatDateTimeIfNotNull(
                    priceHandler.getSpecialPriceData().fromDate, ""));
            data.put(MAGEKEY_PRODUCT_SPECIAL_TO_DATE, CommonUtils.formatDateTimeIfNotNull(
                    priceHandler.getSpecialPriceData().toDate, ""));
        } else {
            data.put(MAGEKEY_PRODUCT_SPECIAL_PRICE, "");
            data.put(MAGEKEY_PRODUCT_SPECIAL_FROM_DATE, "");
            data.put(MAGEKEY_PRODUCT_SPECIAL_TO_DATE, "");
        }
        data.put(MAGEKEY_PRODUCT_DESCRIPTION, description);
        data.put(MAGEKEY_PRODUCT_SHORT_DESCRIPTION,
                getSpecialAttributeValue(MAGEKEY_PRODUCT_SHORT_DESCRIPTION));
        data.put(MAGEKEY_PRODUCT_WEIGHT, weight);

        return data;
    }

    private static boolean checkForFields(final Map<String, ?> fields, final String[] fieldKeys) {
        for (final String fieldKey : fieldKeys) {
            final Object obj = fields.get(fieldKey);
            if (obj == null) {
                return false;
            }
            if (obj instanceof String) {
                if (TextUtils.isEmpty((String) obj)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void showProgressDialog(final String message) {
        if (isActivityAlive == false) {
            return;
        }
        if (progressDialog != null) {
            return;
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public void dismissProgressDialog() {
        if (progressDialog == null) {
            return;
        }
        progressDialog.dismiss();
        progressDialog = null;
    }

    // sell functionality

    /**
     * Create Order
     */
    private void createOrder() {
        // Check that all necessary information exists
        if (validateProductInfo()) {
            if (!newOptionPendingLoadingControl.isLoading()) {
                createNewProduct(true);
            } else {
                GuiUtils.alert("Wait for options creation...");
            }
        }
    }

    // Validate Necessary Product Information
    // to create an order [Name, Price, Quantity]
    private boolean validateProductInfo() {
        String message = "You Must Enter:";
        boolean result = true;

        if (TextUtils.isEmpty(priceV.getText())
                || !ProductUtils.isValidPricesString(priceV.getText().toString())) {
            result = false;
            message += " Price,";
        }

        if (TextUtils.isEmpty(quantityV.getText())) {
            result = false;
            message += " Quantity,";
        }

        // Check if name is empty
        if (TextUtils.isEmpty(getProductName(this))) {
            result = false;
            message += " Name";
        }

        if (!result) {
            AlertDialog.Builder builder = new Builder(ProductCreateActivity.this);

            if (message.endsWith(","))
                message = message.substring(0, message.length() - 1);

            builder.setMessage(message);
            builder.setTitle("Missing Information");
            builder.setPositiveButton("OK", null);

            builder.create().show();
        }

        // All Required Data Exists
        return result;
    }

    @Override
    public void onAttributeSetLoadSuccess() {
        super.onAttributeSetLoadSuccess();

        List<Map<String, Object>> attributeSets = getAttributeSets();
        // variable to store information about product create is disabled for
        // all attribute sets
        boolean productCreateDisabled = true;
        for (Map<String, Object> attributeSet : attributeSets) {
            // use boolean AND operation to calculate global
            // productCreateDisabled value 
            productCreateDisabled &= JobCacheManager.safeParseInt(attributeSet
                    .get(MAGEKEY_ATTRIBUTE_SET_PRODUCT_CREATE_DIABLED)) == 1;
            if (!productCreateDisabled) {
                // if found an attribute set where the product create
                // functionality is not disabled
                break;
            }
        }
        if (productCreateDisabled) {
            // if product creation is disabled in all attribute sets
            AlertDialog.Builder builder = new AlertDialog.Builder(ProductCreateActivity.this);
            builder.setTitle(R.string.warning);
            builder.setMessage(R.string.product_creation_disabled);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ProductCreateActivity.this.finish();
                }
            });
            builder.setOnCancelListener(new OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    ProductCreateActivity.this.finish();
                }
            });
            builder.show();
        } else if (firstTimeAttributeSetResponse == true) {

            if (!selectAttributeSetFromPredefinedAttributeValues())
            {
                // if attribute set was not selected from the predefined
                // attribute values
                if (mLoadLastAttributeSetAndCategory == true) {
                    loadLastAttributeSet(false);
                    mLoadLastAttributeSetAndCategory = false;
                    onAttributeSetItemClicked();
                } else if (productToDuplicatePassed != null) {
                    selectAttributeSet(productToDuplicatePassed.getAttributeSetId(), false, false);
                } else {
                    // y: hard-coding 4 as required:
                    // http://code.google.com/p/mageventory/issues/detail?id=18#c29
                    // selectAttributeSet(TODO_HARDCODED_DEFAULT_ATTRIBUTE_SET,
                    // false, false, true);
                }
            }

            firstTimeAttributeSetResponse = false;

            if (isActivityAlive && productToDuplicatePassed == null
                    && atrSetId == INVALID_ATTRIBUTE_SET_ID) {
                if (!mSKUExistsOnServerUncertaintyDialogActive) {
                    showAttributeSetListOrSelectDefault();
                }
            }
        }

    }

    @Override
    public void onAttributeListLoadSuccess() {
        super.onAttributeListLoadSuccess();

        /**
         * Flag indicating whether barcode check is scheduled. Used if
         * productSKUPassed or mScanResultPassed is not null and is of barcode
         * type. Usually it happens if user scans barcode for not yet saved
         * product and selects "Enter as new" option in the product not found
         * dialog. Code can't be checked immediately that is why we should
         * schedule it after user will select attribute set
         */
        boolean scheduleBarcodeCheck = false;
        /**
         * Flag indicating whether code check for the SKU field is scheduled.
         */
        boolean scheduleSkuCheck = false;

        String formatterString = customAttributesList.getUserReadableFormattingString();

        if (formatterString != null) {
            attrFormatterStringV.setVisibility(View.VISIBLE);
            attrFormatterStringV.setText(formatterString);
        } else {
            attrFormatterStringV.setVisibility(View.GONE);
        }

        // if product to duplicated is not passed or user changed attribute set
        // and name field has value which is not autogenerated then fill
        // attribute values from the product name
        if ((productToDuplicatePassed == null || atrSetId != productToDuplicatePassed
                .getAttributeSetId())
                && customAttributesList.getList() != null
                && !TextUtils.isEmpty(getSpecialAttributeValue(MAGEKEY_PRODUCT_NAME))) {
            selectAttributeValuesFromProductName();
        }

        if (firstTimeAttributeListResponse == true && customAttributesList.getList() != null)
        {
            if (!TextUtils.isEmpty(productSKUPassed)) {
                if (mScanResultPassed == null) {
                	// if scan result information is missing but product SKU is passed
                    mScanResultPassed = new ScanResult(productSKUPassed, null);
                }
                // if product SKU was passed to the intent extras
                if (mBarcodeScanned == true
                        && isSpecialAttributeAvailableAndEditable(MAGEKEY_PRODUCT_BARCODE)) {
                    String generatedSKU = ProductUtils.generateSku();
                    if (!mSkipTimestampUpdate) {
                        JobCacheManager.saveRangeStartAsync(generatedSKU, mSettings.getProfileID(),
                                mGalleryTimestamp, this);
                    }
                    setSpecialAttributeValueIfNotNullIgnoreChanges(MAGEKEY_PRODUCT_SKU,
                            generatedSKU, true);
                    // schedule book Barcode check
                    scheduleBarcodeCheck = isSpecialAttributeAvailableAndEditable(MAGEKEY_PRODUCT_BARCODE);
                } else {
                    // schedule book SKU check
                    scheduleSkuCheck = isSpecialAttributeAvailableAndEditable(MAGEKEY_PRODUCT_SKU);
                }
            }
            if (productToDuplicatePassed != null) {
                if (!allowToEditInDupliationMode) {
                    setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_NAME,
                            productToDuplicatePassed.getName(), true, true);
                }

                if (!CustomAttribute.NOT_AVAILABLE_VALUE.equalsIgnoreCase(productToDuplicatePassed.getShortDescription())) {
                    setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_SHORT_DESCRIPTION,
                            productToDuplicatePassed.getShortDescription(), true, true);
                }
                if (!CustomAttribute.NOT_AVAILABLE_VALUE.equalsIgnoreCase(productToDuplicatePassed.getDescription())) {
                    setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_DESCRIPTION,
                            productToDuplicatePassed.getDescription(), true, true);
                }
                setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_WEIGHT, ""
                        + productToDuplicatePassed.getWeight(), true, true);

                setSpecialAttributeValueIfNotNullIgnoreChanges(MAGEKEY_PRODUCT_BARCODE,
                        productToDuplicatePassed.getStringAttributeValue(
                                Product.MAGEKEY_PRODUCT_BARCODE, ""), true);
            }
            // assign the attribute values from predefined attributes and
            // remember updated attribute codes
            Set<String> assignedPredefinedAttributes = assignPredefinedAttributeValues();
            if (productToDuplicatePassed != null)
            {
                if (customAttributesList != null && customAttributesList.getList() != null)
                {
                    for (CustomAttribute elem : customAttributesList.getList()) {
                        if (elem.isReadOnly()) {
                            // skip read only attributes
                            continue;
                        }
                        // do not copy attribute value if it is book attribute
                        // and product is creating in allow to edit in
                        // duplication mode and it is not the duplicate removed
                        // product case and product should not be linked with
                        // another product
                        if (elem.getCode().startsWith(BookInfoLoader.BOOK_ATTRIBUTE_CODE_PREFIX)) {
                            if (allowToEditInDupliationMode && !duplicateRemovedProductMode
                                    && TextUtils.isEmpty(skuToLinkWith)) {
                                continue;
                            }
                        }
                        // if the attribute was already assigned from the
                        // predefined attribute when continue loop to the next
                        // iteration
                        if (assignedPredefinedAttributes.contains(elem.getCode())) {
                            continue;
                        }
                        elem.setSelectedValue(
                                (String) productToDuplicatePassed.getData().get(elem.getCode()),
                                true);
                    }

                    customAttributesList.setNameHint();
                }
                if(isSpecialAttributeAvailableAndEditable(MAGEKEY_PRODUCT_NAME)){
                    determineWhetherNameIsGeneratedAndSetProductName(assignedPredefinedAttributes
                            .contains(MAGEKEY_PRODUCT_NAME) ?
                                    // if the name was assigned from predefined attribute use the
                                    // name special attribute value as the product name
                                    getSpecialAttributeValue(MAGEKEY_PRODUCT_NAME)
                                    : // else
                                    // use product to duplicate name in other cases
                                    productToDuplicatePassed.getName());
                }
                /*
                 * If we are in duplication mode then create a new product only
                 * if sku is provided and categories were loaded.
                 */
                if (!TextUtils.isEmpty(getSpecialAttributeValue(MAGEKEY_PRODUCT_SKU))
                        && !allowToEditInDupliationMode)
                {
                    createNewProduct(false);
                }
            }
            if (assignedPredefinedAttributes.contains(MAGEKEY_PRODUCT_NAME)) {
                // if the name was assigned from predefined attributes determine
                // whether it matches generated value from the custom attributes
                determineWhetherNameIsGeneratedAndSetProductName(getSpecialAttributeValue(MAGEKEY_PRODUCT_NAME));
            }
            if (!allowToEdit) {
                // if editing is not allowed simulate create button click
                mCreateButton.performClick();
            }
            firstTimeAttributeListResponse = false;
            
            // check whether book code check is scheduled and run if it is 
            if (scheduleBarcodeCheck) {
                codeScanCommon(MAGEKEY_PRODUCT_BARCODE, mScanResultPassed, SCAN_BARCODE, false,
                        true);
            }
            // check whether SKU check is scheduled and run if it is
            if (scheduleSkuCheck) {
                codeScanCommon(MAGEKEY_PRODUCT_SKU, mScanResultPassed, SCAN_QR_CODE, false, true);
            }
            if (TextUtils.isEmpty(getSpecialAttributeValue(MAGEKEY_PRODUCT_SKU))
                    && !scheduleBarcodeCheck && !scheduleSkuCheck) {
                // Request input of SKU if it is empty and there are no
                // scheduled barcode or SKU checks
                performClickOnSpecialAttribute(MAGEKEY_PRODUCT_SKU);
            }
        }
        CustomAttribute skuAttribute = getSpecialAttribute(MAGEKEY_PRODUCT_SKU);
        if (skuAttribute != null && skuAttribute.isReadOnly()) {
            // if SKU attribute is available and read-only
        	//
        	// disable create button
            mCreateButton.setEnabled(false);
            GuiUtils.showMessageDialog(R.string.warning, R.string.product_creation_disabled,
                    ProductCreateActivity.this);
        } else {
            // if SKU attribute is not available or is not read-only
            mCreateButton.setEnabled(true);
        }
    }

    public void showDuplicationCancelledDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Warning");
        alert.setMessage("Duplication cancelled.");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ProductCreateActivity.this.finish();
            }
        });

        AlertDialog srDialog = alert.create();

        srDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                ProductCreateActivity.this.finish();
            }
        });

        srDialog.show();
    }

    public void showInvalidLabelDialog(final String settingsDomainName, final String skuDomainName) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Warning");
        alert.setMessage("Wrong label. Expected domain name: '" + settingsDomainName + "' found: '"
                + skuDomainName + "'");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ScanActivity.rememberDomainNamePair(settingsDomainName, skuDomainName);

                /*
                 * If scan was successful then if attribute list and categories
                 * were loaded then create a new product.
                 */
                if (productToDuplicatePassed != null && !allowToEditInDupliationMode)
                {
                    if (firstTimeAttributeListResponse == false)
                    {
                        createNewProduct(false);
                    }
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_SKU, "", true);

                /*
                 * If we are in duplication mode then close the activity in this
                 * case (show dialog first)
                 */
                if (productToDuplicatePassed != null && !allowToEditInDupliationMode)
                {
                    showDuplicationCancelledDialog();
                }
            }
        });

        AlertDialog srDialog = alert.create();

        srDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {

                setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_SKU, "", true);

                /*
                 * If we are in duplication mode then close the activity in this
                 * case (show dialog first)
                 */
                if (productToDuplicatePassed != null && !allowToEditInDupliationMode)
                {
                    showDuplicationCancelledDialog();
                }
            }
        });

        srDialog.show();
    }

    /**
     * Get the Scanned Code
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == SCAN_QR_CODE) {
            if (resultCode == RESULT_OK) {

                boolean invalidLabelDialogShown = skuScanCommon(intent, SCAN_QR_CODE);

                /*
                 * If scan was successful then if attribute list and categories
                 * were loaded then create a new product.
                 */
                if (invalidLabelDialogShown == false && productToDuplicatePassed != null
                        && !allowToEditInDupliationMode)
                {
                    if (firstTimeAttributeListResponse == false)
                    {
                        createNewProduct(false);
                    }
                }

            } else if (resultCode == RESULT_CANCELED) {
                /*
                 * If we are in duplication mode then close the activity in this
                 * case (show dialog first)
                 */
                if (productToDuplicatePassed != null && !allowToEditInDupliationMode)
                {
                    showDuplicationCancelledDialog();
                }
            }
            if (productToDuplicatePassed != null && allowToEditInDupliationMode
                    && decreaseOriginalQTY == 0)
            {
                quantityV.requestFocus();
                GuiUtils.showKeyboardDelayed(quantityV);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    boolean isEventTarget(Intent extra, boolean silent) {
        // for product create activity SKU extra passed to the WEB_TEXT_COPIED
        // broadcast event should be empty
        return TextUtils.isEmpty(extra.getStringExtra(EventBusUtils.SKU));
    }

    @Override
    public String getSku() {
        return null;
    }
    
    @Override
    public Product getProduct() {
        return null;
    }

    @Override
    public void searchInternet() {
        mRecentWebAddressesSearchPopupHandler.prepareAndShowSearchInternetDialog(getSku(),
                lastUsedQuery, mSettings.getUrl());
    }

    @Override
    public void onGeneralBroadcastEvent(EventType eventType, Intent extra) {
        super.onGeneralBroadcastEvent(eventType, extra);
        switch (eventType) {
            case WEB_SEARCH_ACTIVATED: {
                CommonUtils.debug(TAG,
                        "onGeneralBroadcastEvent: received web search activated event");
                if (isEventTarget(extra, true)) {
                    // remember query to the lastUsedQuery field
                    lastUsedQuery = extra.getStringExtra(EventBusUtils.TEXT);
                }
                break;
            }
            case IMAGE_ADDED: {
                CommonUtils.debug(TAG, "onGeneralBroadcastEvent: received image added event");
                if (isEventTarget(extra, true)) {
                    addedImages.add(extra.getStringExtra(EventBusUtils.PATH));
                }
                break;
            }
            default:
                break;
        }
    }

    /**
     * Launch the product create activity
     * 
     * @param sku predefined SKU for new product
     * @param barcodeScanned whether the predefined sku parameter is barcode
     * @param skipTimestampUpdate whether the access timestamp update should be
     *            skipped for the predefined scanned barcode
     * @param galleryTimestamp predefined gallery timestamp which should be used
     *            in the case barcode parameter is passed and
     *            skipTimestampUpdate is false
     * @param skuExistsOnServerUncertainty the exception occurred during product
     *            loading operation. Used to display warning dialog after the
     *            activity launch
     * @param productToDuplicateOrRestoreRemoved the product which should be
     *            duplicated or the product which was removed from server but
     *            stored locally and should be restored using local copy
     * @param allowToEditInDuplicationMode whether the editing is allowed when
     *            duplicating product or save should be performed right after
     *            the details are loaded
     * @param duplicateRemovedProductMode whether it is duplicate removed
     *            product mode. The mode when the product is removed on server
     *            and should be restored from the local cached copy
     * @param copyPhotoMode the mode for copying of photos. Used when
     *            duplicating a product
     * @param decreaseOriginalQty the decreased original quantity for the
     *            duplicate product. Used only when
     *            productToDuplicateOrRestoreRemoved is not null
     * @param allowToEdit whether the editing is allowed or save should be
     *            performed right after the details are loaded
     * @param skuToLinkWith the SKU of the product the creating product should
     *            be linked with. Used for configurable attributes
     * @param predefinedCustomAttributeValues the list of predefined custom
     *            attribute values which should be used to overwrite default
     *            attribute values or values copied from
     *            productToDuplicateOrRestoreRemoved
     * @param activity the activity from where the create activity should be
     *            launched
     */
    public static void launchProductCreate(String sku, boolean barcodeScanned,
            boolean skipTimestampUpdate, long galleryTimestamp,
            ProductDetailsLoadException skuExistsOnServerUncertainty, Product productToDuplicateOrRestoreRemoved,
            boolean allowToEditInDuplicationMode, boolean duplicateRemovedProductMode,
            String copyPhotoMode, Float decreaseOriginalQty, boolean allowToEdit,
            String skuToLinkWith, ArrayList<CustomAttributeSimple> predefinedCustomAttributeValues,
            Activity activity) {
        // TODO remove string resources as an intent extra keys and replace them
        // with the java constants
        final String ekeyProductSKU = CommonUtils.getStringResource(R.string.ekey_product_sku);
        final String ekeySkuExistsOnServerUncertainty = CommonUtils
                .getStringResource(R.string.ekey_sku_exists_on_server_uncertainty);
        final String brScanned = CommonUtils.getStringResource(R.string.ekey_barcode_scanned);
        final String ekeySkipTimestampUpdate = CommonUtils
                .getStringResource(R.string.ekey_skip_timestamp_update);

        final Intent intent = new Intent(activity, ProductCreateActivity.class);

        intent.putExtra(ekeyProductSKU, sku);
        intent.putExtra(ekeySkuExistsOnServerUncertainty, (Parcelable) skuExistsOnServerUncertainty);
        intent.putExtra(brScanned, barcodeScanned);
        intent.putExtra(ekeySkipTimestampUpdate, skipTimestampUpdate);
        intent.putExtra(EXTRA_ALLOW_TO_EDIT, allowToEdit);
        intent.putParcelableArrayListExtra(EXTRA_PREDEFINED_ATTRIBUTES,
                predefinedCustomAttributeValues);
        intent.putExtra(EXTRA_LINK_WITH_SKU, skuToLinkWith);

        if (productToDuplicateOrRestoreRemoved != null) {
            /*
             * Launching product create activity from "duplicate menu" breaks
             * NewNewReload cycle.
             */
            BaseActivityCommon.sNewNewReloadCycle = false;
            final String ekeyProductToDuplicate = CommonUtils
                    .getStringResource(R.string.ekey_product_to_duplicate);
            final String ekeyProductSKUToDuplicate = CommonUtils
                    .getStringResource(R.string.ekey_product_sku_to_duplicate);
            final String ekeyAllowToEditInDuplicationMode = CommonUtils
                    .getStringResource(R.string.ekey_allow_to_edit_in_duplication_mode);
            final String ekeyDuplicateRemovedProductMode = CommonUtils
                    .getStringResource(R.string.ekey_duplicate_removed_product_mode);
            final String ekeyCopyPhotoMode = CommonUtils
                    .getStringResource(R.string.ekey_copy_photo_mode);
            final String ekeyDecreaseOriginalQTY = CommonUtils
                    .getStringResource(R.string.ekey_decrease_original_qty);

            intent.putExtra(ekeyProductToDuplicate, (Serializable) productToDuplicateOrRestoreRemoved);
            intent.putExtra(ekeyProductSKUToDuplicate, productToDuplicateOrRestoreRemoved.getSku());
            intent.putExtra(ekeyAllowToEditInDuplicationMode, allowToEditInDuplicationMode);
            intent.putExtra(ekeyDuplicateRemovedProductMode, duplicateRemovedProductMode);
            intent.putExtra(ekeyCopyPhotoMode, copyPhotoMode);
            intent.putExtra(ekeyDecreaseOriginalQTY, decreaseOriginalQty);
        }

        if (galleryTimestamp != 0) {
            intent.putExtra(CommonUtils.getStringResource(R.string.ekey_gallery_timestamp),
                    galleryTimestamp);
        }

        activity.startActivity(intent);
    }
}
