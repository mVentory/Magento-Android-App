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

import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.mageventory.R;
import com.mageventory.activity.base.BaseActivityCommon;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.Product;
import com.mageventory.model.util.ProductUtils;
import com.mageventory.model.util.ProductUtils.PricesInformation;
import com.mageventory.resprocessor.ProductDetailsProcessor.ProductDetailsLoadException;
import com.mageventory.settings.Settings;
import com.mageventory.tasks.BookInfoLoader;
import com.mageventory.tasks.CreateNewProduct;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ScanUtils;

public class ProductCreateActivity extends AbsProductActivity {

    public static final String PRODUCT_CREATE_ATTRIBUTE_SET = "attribute_set";
    public static final String PRODUCT_CREATE_DESCRIPTION = "description";
    public static final String PRODUCT_CREATE_WEIGHT = "weight";
    public static final String PRODUCT_CREATE_CATEGORY = "category";
    public static final String PRODUCT_CREATE_SHARED_PREFERENCES = "ProductCreateSharedPreferences";

    private SharedPreferences preferences;

    @SuppressWarnings("unused")
    private static final String TAG = "ProductCreateActivity";
    private static final String[] MANDATORY_USER_FIELDS = {};

    // views
    public EditText quantityV;
    public EditText weightV;
    private TextView attrFormatterStringV;

    // dialogs
    private ProgressDialog progressDialog;
    private boolean firstTimeAttributeSetResponse = true;
    private boolean firstTimeAttributeListResponse = true;

    public float decreaseOriginalQTY;
    public String copyPhotoMode;
    private String productSKUPassed;
    public String productSKUtoDuplicate;
    public Product productToDuplicatePassed;
    public boolean allowToEditInDupliationMode;
    private ProductDetailsLoadException skuExistsOnServerUncertaintyPassed;
    private boolean mLoadLastAttributeSetAndCategory;

    private boolean mSKUExistsOnServerUncertaintyDialogActive = false;

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
            alert.setMessage(CommonUtils.getStringResource(R.string.cannot_check_sku_default));
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
        setContentView(R.layout.product_create);

        nameV = (AutoCompleteTextView) findViewById(R.id.name);
        nameV.setHorizontallyScrolling(false);
        nameV.setMaxLines(Integer.MAX_VALUE);

        absOnCreate();

        mLoadLastAttributeSetAndCategory = BaseActivityCommon.sNewNewReloadCycle;

        quantityV = (EditText) findViewById(R.id.quantity);
        descriptionV = (AutoCompleteTextView) findViewById(R.id.description);
        initDescriptionField();
        weightV = (EditText) findViewById(R.id.weight);
        attrFormatterStringV = (TextView) findViewById(R.id.attr_formatter_string);
        barcodeInput.setOnLongClickListener(scanBarcodeOnClickL);
        barcodeInput.setOnTouchListener(null);

        OnEditorActionListener nextButtonBehaviour = new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {

                    InputMethodManager imm = (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    return true;
                }

                return false;
            }
        };

        nameV.setOnEditorActionListener(nextButtonBehaviour);
        skuV.setOnEditorActionListener(nextButtonBehaviour);
        priceV.setOnEditorActionListener(nextButtonBehaviour);
        quantityV.setOnEditorActionListener(nextButtonBehaviour);
        descriptionV.setOnEditorActionListener(nextButtonBehaviour);
        barcodeInput.setOnEditorActionListener(nextButtonBehaviour);
        weightV.setOnEditorActionListener(nextButtonBehaviour);

        preferences = getSharedPreferences(PRODUCT_CREATE_SHARED_PREFERENCES, Context.MODE_PRIVATE);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            productSKUPassed = extras.getString(getString(R.string.ekey_product_sku));
            productSKUtoDuplicate = extras
                    .getString(getString(R.string.ekey_product_sku_to_duplicate));
            skuExistsOnServerUncertaintyPassed = extras
                    .getParcelable(getString(R.string.ekey_sku_exists_on_server_uncertainty));
            productToDuplicatePassed = (Product) extras
                    .getSerializable(getString(R.string.ekey_product_to_duplicate));
            allowToEditInDupliationMode = extras
                    .getBoolean(getString(R.string.ekey_allow_to_edit_in_duplication_mode));
            copyPhotoMode = extras.getString(getString(R.string.ekey_copy_photo_mode));
            decreaseOriginalQTY = extras.getFloat(getString(R.string.ekey_decrease_original_qty));
            mGalleryTimestamp = extras.getLong(getString(R.string.ekey_gallery_timestamp), 0);
            boolean skipTimestampUpdate = extras.getBoolean(
                    getString(R.string.ekey_skip_timestamp_update), false);

            boolean barcodeScanned = extras.getBoolean(getString(R.string.ekey_barcode_scanned),
                    false);

            /*
             * Not sure whether this product is on the server. Show info about
             * this problem.
             */
            if (skuExistsOnServerUncertaintyPassed != null)
            {
                showSKUExistsOnServerUncertaintyDialog();
            }

            if (!TextUtils.isEmpty(productSKUPassed))
            {
                if (barcodeScanned == true)
                {
                    String generatedSKU = generateSku();
                    if (!skipTimestampUpdate) {
                        if (JobCacheManager.saveRangeStart(generatedSKU, mSettings.getProfileID(),
                                mGalleryTimestamp) == false) {
                            ProductDetailsActivity.showTimestampRecordingError(this);
                        }
                    }

                    skuV.setText(generatedSKU);
                    barcodeInput.setText(productSKUPassed);
                }
                else
                {
                    skuV.setText(productSKUPassed);
                }
            }

            if (productToDuplicatePassed != null)
            {
                if (!allowToEditInDupliationMode)
                {
                    nameV.setText(productToDuplicatePassed.getName());
                }
                setPriceTextValue(ProductUtils.getProductPricesString(productToDuplicatePassed));
                specialPriceData.fromDate = productToDuplicatePassed.getSpecialFromDate();
                specialPriceData.toDate = productToDuplicatePassed.getSpecialToDate();

                if (!productToDuplicatePassed.getDescription().equalsIgnoreCase("n/a"))
                {
                    descriptionV.setText(productToDuplicatePassed.getDescription());
                }
                weightV.setText("" + productToDuplicatePassed.getWeight());

                double dupQty = 0;

                if (decreaseOriginalQTY > 0)
                {
                    dupQty = decreaseOriginalQTY;
                }
                else
                {
                    dupQty = 1;
                }
                if (productToDuplicatePassed.getIsQtyDecimal() == 1)
                {
                    quantityV.setInputType(InputType.TYPE_CLASS_NUMBER
                            | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    quantityV.setText(CommonUtils.formatNumberWithFractionWithRoundUp(dupQty));
                }
                else
                {
                    quantityV.setInputType(InputType.TYPE_CLASS_NUMBER);
                    quantityV.setText(CommonUtils.formatDecimalOnlyWithRoundUp(dupQty));
                }

                if (productToDuplicatePassed.getData().containsKey("product_barcode_")) {
                    barcodeInput.setText(productToDuplicatePassed.getData().get("product_barcode_")
                            .toString());
                } else {
                    barcodeInput.setText("");
                }

                scanSKUOnClickL.onLongClick(skuV);
            }
        }

        if (productToDuplicatePassed == null)
        {
            Settings settings = new Settings(this);
        }

        // listeners
        findViewById(R.id.create_btn).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ProductCreateActivity.this.hideKeyboard();

                /*
                 * It is not possible for the user to create a product if some
                 * custom attribute options are being created.
                 */
                if (newAttributeOptionPendingCount == 0) {
                    if (atrSetId == INVALID_ATTRIBUTE_SET_ID) {
                        showSelectAttributeSetDialog();
                    } else {
                        if (verifyForm(false) == false) {
                            Toast.makeText(getApplicationContext(),
                                    "Please fill out all required fields...", Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            createNewProduct(false);
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Wait for options creation...",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        attributeSetV.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                attributeSetLongTap = true;

                String description = preferences.getString(PRODUCT_CREATE_DESCRIPTION, "");
                String weight = preferences.getString(PRODUCT_CREATE_WEIGHT, "");

                descriptionV.setText(description);
                weightV.setText(weight);

                loadLastAttributeSet(true);

                scanSKUOnClickL.onLongClick(skuV);

                return true;
            }
        });

        // ---
        // sell functionality

        skuV.setOnLongClickListener(scanSKUOnClickL);

        // Get the Extra "PASSING SKU -- SHOW IT"
        if (getIntent().hasExtra(PASSING_SKU)) {
            boolean isSKU = getIntent().getBooleanExtra(PASSING_SKU, false);
            if (isSKU) {
                skuV.setText(getIntent().getStringExtra(MAGEKEY_PRODUCT_SKU));
            }
        }

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

    private OnLongClickListener scanSKUOnClickL = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            ScanUtils.startScanActivityForResult(ProductCreateActivity.this, SCAN_QR_CODE,
                    R.string.scan_barcode_or_qr_label);
            return true;
        }
    };

    private OnLongClickListener scanBarcodeOnClickL = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            ScanUtils.startScanActivityForResult(ProductCreateActivity.this, SCAN_BARCODE,
                    R.string.scan_barcode_or_qr_label);
            return true;
        }
    };

    public boolean verifyForm(boolean quickSellMode) {

        if (!TextUtils.isEmpty(priceV.getText())) {
            if (!ProductUtils.isValidPricesString(priceV.getText().toString())) {
                GuiUtils.alert(R.string.invalid_price_information);
                return false;
            }
        }
        // check user fields
        if (checkForFields(extractCommonData(), MANDATORY_USER_FIELDS) == false) {
            return false;
        }

        /* We don't need attribute set in quick sell mode. */
        if (quickSellMode == false)
        {
            // check attribute set
            if (atrSetId == INVALID_ATTRIBUTE_SET_ID) {
                return false;
            }
        }

        if (customAttributesList.getList() != null) {
            for (CustomAttribute elem : customAttributesList.getList()) {
                if (elem.getIsRequired() == true && TextUtils.isEmpty(elem.getSelectedValue())) {
                    return false;
                }
            }
        }

        return true;
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

        String name = getProductName(this, nameV);
        String price = priceV.getText().toString();
        String description = descriptionV.getText().toString();
        String weight = weightV.getText().toString();

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
            data.put(MAGEKEY_PRODUCT_SPECIAL_FROM_DATE,
                    CommonUtils.formatDateTimeIfNotNull(specialPriceData.fromDate, ""));
            data.put(MAGEKEY_PRODUCT_SPECIAL_TO_DATE,
                    CommonUtils.formatDateTimeIfNotNull(specialPriceData.toDate, ""));
        } else {
            data.put(MAGEKEY_PRODUCT_SPECIAL_PRICE, "");
            data.put(MAGEKEY_PRODUCT_SPECIAL_FROM_DATE, "");
            data.put(MAGEKEY_PRODUCT_SPECIAL_TO_DATE, "");
        }
        data.put(MAGEKEY_PRODUCT_DESCRIPTION, description);
        data.put(MAGEKEY_PRODUCT_SHORT_DESCRIPTION, description);
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
            if (newAttributeOptionPendingCount == 0) {
                createNewProduct(true);
            } else {
                Toast.makeText(getApplicationContext(), "Wait for options creation...",
                        Toast.LENGTH_SHORT).show();
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
        if (TextUtils.isEmpty(getProductName(this, nameV))) {
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

        if (firstTimeAttributeSetResponse == true) {

            if (mLoadLastAttributeSetAndCategory == true)
            {
                loadLastAttributeSet(false);
                mLoadLastAttributeSetAndCategory = false;
                onAttributeSetItemClicked();
            }
            else if (productToDuplicatePassed != null)
            {
                selectAttributeSet(productToDuplicatePassed.getAttributeSetId(), false, false);
            }
            else
            {
                // y: hard-coding 4 as required:
                // http://code.google.com/p/mageventory/issues/detail?id=18#c29
                // selectAttributeSet(TODO_HARDCODED_DEFAULT_ATTRIBUTE_SET,
                // false, false, true);
            }

            firstTimeAttributeSetResponse = false;

            if (isActivityAlive && productToDuplicatePassed == null
                    && atrSetId == INVALID_ATTRIBUTE_SET_ID) {
                if (!mSKUExistsOnServerUncertaintyDialogActive) {
                    showAttributeSetList();
                }
            }
        }

    }

    @Override
    public void onAttributeListLoadSuccess() {
        super.onAttributeListLoadSuccess();

        String formatterString = customAttributesList.getUserReadableFormattingString();

        if (formatterString != null) {
            attrFormatterStringV.setVisibility(View.VISIBLE);
            attrFormatterStringV.setText(formatterString);
        } else {
            attrFormatterStringV.setVisibility(View.GONE);
        }

        if (firstTimeAttributeListResponse == true && customAttributesList.getList() != null)
        {
            if (productToDuplicatePassed != null)
            {
                if (customAttributesList != null && customAttributesList.getList() != null)
                {
                    for (CustomAttribute elem : customAttributesList.getList()) {
                        elem.setSelectedValue(
                                (String) productToDuplicatePassed.getData().get(elem.getCode()),
                                true);
                    }

                    customAttributesList.setNameHint();
                }

                /*
                 * If we are in duplication mode then create a new product only
                 * if sku is provided and categories were loaded.
                 */
                if (TextUtils.isEmpty(skuV.getText().toString()) == false &&
                        !allowToEditInDupliationMode)
                {
                    createNewProduct(false);
                }
                determineWhetherNameIsGeneratedAndSetProductName(productToDuplicatePassed);
            }

            firstTimeAttributeListResponse = false;
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

                skuV.setText("");

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

                skuV.setText("");

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

    @Override
    protected void onAttributeSetItemClicked() {
        super.onAttributeSetItemClicked();
        if (TextUtils.isEmpty(skuV.getText())) {
            scanSKUOnClickL.onLongClick(skuV);
        }
    }
    
    /**
     * Get the Scanned Code
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == SCAN_QR_CODE) {
            if (resultCode == RESULT_OK) {

                boolean invalidLabelDialogShown = skuScanCommon(intent);

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
        else if (requestCode == SCAN_BARCODE) {
            if (resultCode == RESULT_OK) {

                mGalleryTimestamp = JobCacheManager.getGalleryTimestampNow();

                String contents = ScanUtils.getSanitizedScanResult(intent);

                // Set Barcode in Product Barcode TextBox
                barcodeInput.setText(contents);

                // Check if Attribute Set is Book
                EditText attrSet = (EditText) findViewById(R.id.attr_set);
                if (TextUtils.equals(attrSet.getText().toString(), "Book")) {
                    Settings settings = new Settings(getApplicationContext());
                    String apiKey = settings.getAPIkey();
                    if (TextUtils.equals(apiKey, "")) {
                        Toast.makeText(getApplicationContext(),
                                "Book Search is Disabled - set Google API KEY to enable it",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        new BookInfoLoader(this, customAttributesList).execute(contents, apiKey);
                    }
                }
                weightV.requestFocus();
                GuiUtils.showKeyboardDelayed(weightV);
            } else if (resultCode == RESULT_CANCELED) {
                // Do Nothing
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

}
