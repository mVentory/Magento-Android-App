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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.mageventory.R;
import com.mageventory.activity.ScanActivity.CheckSkuResult;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttribute.InputMethod;
import com.mageventory.model.CustomAttributeSimple;
import com.mageventory.model.Product;
import com.mageventory.tasks.BookInfoLoader;
import com.mageventory.tasks.LoadProduct;
import com.mageventory.tasks.UpdateProduct;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ScanUtils;

public class ProductEditActivity extends AbsProductActivity {

    private static final int ADD_XXX_SKUS_QUESTION = 1;
    /**
     * The key for the updated text attributes intent extra
     */
    public static final String EXTRA_UPDATED_TEXT_ATTRIBUTES = "UPDATED_TEXT_ATTRIBUTES";

    AtomicInteger attributeLoadCount = null;
    public ArrayList<String> mAdditionalSKUs = new ArrayList<String>();

    void checkAllLoadedAndLoadProduct() {
        if (attributeLoadCount != null) {
            int count = attributeLoadCount.incrementAndGet();

            if (count == 1) {
                loadProduct(productSKU, false);
                attributeLoadCount = null;
            }
        }
    }

    @Override
    public void onAttributeSetLoadSuccess() {
        super.onAttributeSetLoadSuccess();

        checkAllLoadedAndLoadProduct();
    }

    // state
    private LoadProduct loadProductTask;
    public String productSKU;
    public boolean mAdditionalSkusMode;
    public boolean mRescanAllMode;
    private ProgressDialog progressDialog;
    private boolean mUpdateConfirmationSkipped = false;
    private boolean mUpdateConfirmationShowing = false;
    
    /**
     * The flag to schedule update confirmation dialog showing when activity
     * resumes
     */
    public boolean mShowUpdateConfirmationDialogOnResume;
    /**
     * Updated text attributes information passed to the activity intent
     */
    ArrayList<CustomAttributeSimple> mUpdatedTextAttributes;
    
    public UpdateProduct updateProductTask;

    public void dismissProgressDialog() {
        if (progressDialog == null) {
            return;
        }
        progressDialog.dismiss();
        progressDialog = null;
    }

    public Product getProduct() {
        if (loadProductTask == null) {
            return null;
        }
        return loadProductTask.getData();
    }

    private void loadProduct(final String productSKU, final boolean forceRefresh) {
        if (loadProductTask != null) {
            if (loadProductTask.getState() == TSTATE_RUNNING) {
                return;
            } else if (forceRefresh == false && loadProductTask.getState() == TSTATE_TERMINATED
                    && loadProductTask.getData() != null) {
                return;
            }
        }
        if (loadProductTask != null) {
            loadProductTask.cancel(true);
        }
        loadProductTask = new LoadProduct();
        loadProductTask.setHost(this);
        loadProductTask.execute(productSKU, forceRefresh);
    }

    private void mapData(final Product p) {
        if (p == null) {
            return;
        }
        final Runnable map = new Runnable() {
            public void run() {

                priceHandler.setDataFromProduct(p);

                if (p.getManageStock() == 0)
                {
                    quantityV.setText("");
                }
                else
                {
                    double quantityValue = CommonUtils.parseNumber(p.getQuantity().toString());

                    quantityV.setInputType(InputType.TYPE_CLASS_NUMBER
                            | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    if (p.getIsQtyDecimal() == 1) {
                        quantityV.setText(CommonUtils
                                .formatNumberWithFractionWithRoundUp(quantityValue));
                    } else {
                        quantityV.setText(CommonUtils.formatDecimalOnlyWithRoundUp(quantityValue));
                    }
                }
                if (!selectAttributeSetFromPredefinedAttributeValues()) {
                    // if attribute set was not selected from the predefined
                    // attribute use the product attribute set to select
                    final int atrSetId = p.getAttributeSetId();
                    selectAttributeSet(atrSetId, false, false);
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            map.run();
        } else {
            runOnUiThread(map);
        }
    }

    @Override
    public void onAttributeListLoadSuccess() {
        super.onAttributeListLoadSuccess();

        final Product product = getProduct();
        if (product == null) {
            return;
        }

        setBarcodeInputTextIgnoreChanges(getBarcode(product));
        setSpecialAttributeValueFromProduct(MAGEKEY_PRODUCT_SHORT_DESCRIPTION, product);
        setSpecialAttributeValueFromProduct(MAGEKEY_PRODUCT_DESCRIPTION, product);
        setSpecialAttributeValueFromProduct(MAGEKEY_PRODUCT_NAME, product);
        setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_WEIGHT,
                CommonUtils.formatNumberIfNotNull(product.getWeight()), true);
        setSpecialAttributeValueFromProduct(MAGEKEY_PRODUCT_SKU, product);

        Set<String> attributesSelectedFromName = null;
        // if attribute set was changed then preselect attribute values from the
        // product name
        if (atrSetId != product.getAttributeSetId() && customAttributesList.getList() != null) {
            // do not show update confirmation dialog to user in case attribute
            // set was changed
            mUpdateConfirmationSkipped = true;
            attributesSelectedFromName = selectAttributeValuesFromProductName();
        }

        Set<String> assignedPredefinedAttributes = assignPredefinedAttributeValues();
        if (customAttributesList.getList() != null) {
            for (CustomAttribute elem : customAttributesList.getList()) {
                String value = (String) product.getData().get(elem.getCode());
                // if the attribute value is not empty or was not modified by
                // the selectAttributeValuesFromProductName call and was not
                // assigned from the predefined attributes
                if (!assignedPredefinedAttributes.contains(elem.getCode())
                        && !(TextUtils.isEmpty(value) && (attributesSelectedFromName != null && attributesSelectedFromName
                        .contains(elem.getCode())))) {
                    elem.setSelectedValue(value, true);
                    // clear any attribute container marks if was marked before
                    elem.unmarkAttributeContainer();
                }
                appendTextIfExists(elem);
            }
            customAttributesList.setNameHint();
        }
        if (assignedPredefinedAttributes.contains(MAGEKEY_PRODUCT_NAME)) {
        	// if the name was assigned from predefined attributes determine
            // whether it matches generated value from the custom attributes
            determineWhetherNameIsGeneratedAndSetProductName(getSpecialAttributeValue(MAGEKEY_PRODUCT_NAME));
        } else if (atrSetId == product.getAttributeSetId()) {
            // set the product name value in case attribute set was not changed
            determineWhetherNameIsGeneratedAndSetProductName(product.getName());
        }
        appendTextIfExists(getSpecialAttribute(MAGEKEY_PRODUCT_NAME));
        appendTextIfExists(getSpecialAttribute(MAGEKEY_PRODUCT_SHORT_DESCRIPTION));
        appendTextIfExists(getSpecialAttribute(MAGEKEY_PRODUCT_DESCRIPTION));
        String formatterString = customAttributesList.getUserReadableFormattingString();

        if (formatterString != null) {
            attrFormatterStringV.setVisibility(View.VISIBLE);
            attrFormatterStringV.setText(formatterString);
        } else {
            attrFormatterStringV.setVisibility(View.GONE);
        }
        if (!allowToEdit) {
            // if editing is not allowed update product
            updateProduct();
        }

    }

    /**
     * Append text to the custom attribute value if exists in the
     * mUpdatedTextAttributes
     * 
     * @param elem
     */
    public void appendTextIfExists(CustomAttribute elem) {
        boolean isTextArea = elem != null && elem.isOfType(CustomAttribute.TYPE_TEXTAREA);
        if (elem != null
                && elem.hasDefaultOrAlternateInputMethod(InputMethod.COPY_FROM_INTERNET_SEARCH)
                && (elem.isOfType(CustomAttribute.TYPE_TEXT) || isTextArea)
                && mUpdatedTextAttributes != null) {
            for (CustomAttributeSimple customAttributeSimple : mUpdatedTextAttributes) {
                // If matches were found, mUpdatedTextAttributes contains text
                // which should be appended to the attribute value
                if (elem.isOfCode(customAttributeSimple.getCode())) {
                    List<String> appendedValues = customAttributeSimple.getAppendedValues();
                    if (appendedValues != null && !appendedValues.isEmpty()) {
                        // if there are appended values
                        if (elem.isOfCode(MAGEKEY_PRODUCT_NAME)) {
                            // for the name attribute value should not be
                            // appended but should to replace the current value.
                            // Get the last appended value and use it as the
                            // name attribute value
                            determineWhetherNameIsGeneratedAndSetProductName(appendedValues
                                    .get(appendedValues.size() - 1));
                        } else {
                            // iterate through appended values if present and append
                            // each one to corresponding text view
                            EditText correspondingView = (EditText) elem.getCorrespondingView();
                            for (String value : appendedValues) {
                                appendText(correspondingView, value, isTextArea);
                            }
                        }
                    }
                }
            }
        }
    }

    public String getBarcode(final Product product) {
        final List<Map<String, Object>> attrs = getAttributeList();
        if (attrs == null) {
            return "";
        }
        return product.getBarcode(attrs);
    }

    private Button updateBtn;

    @Override
    public void onEditDone(String attributeCode) {
        super.onEditDone(attributeCode);
        showUpdateConfirmationDialog();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        attributeLoadCount = new AtomicInteger(0);

        absOnCreate();

        // map views

        // extras
        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            throw new IllegalStateException();
        }
        productSKU = extras.getString(getString(R.string.ekey_product_sku));
        mAdditionalSkusMode = extras.getBoolean(getString(R.string.ekey_additional_skus_mode));
        mRescanAllMode = extras.getBoolean(getString(R.string.ekey_rescan_all_mode));
        mUpdatedTextAttributes = extras.getParcelableArrayList(EXTRA_UPDATED_TEXT_ATTRIBUTES);

        onProductLoadStart();

        // listeners

        OnClickListener updateClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {

                ProductEditActivity.this.hideKeyboard();
                updateProduct();
            }
        };

        updateBtn = (Button) findViewById(R.id.saveBtn);
        updateBtn.setText(R.string.update);
        updateBtn.setOnClickListener(updateClickListener);
    }

    public void onProductLoadFailure() {
        dismissProgressDialog();
    }

    private void onProductLoadStart() {
        showProgressDialog(getString(R.string.loading_product_sku, productSKU));
    }

    public void onProductLoadSuccess() {
        dismissProgressDialog();
        mapData(getProduct());

        if (mAdditionalSkusMode)
        {
            if (mRescanAllMode)
            {
                quantityV.setText("0");
            }

            ScanUtils.startScanActivityForResult(ProductEditActivity.this, SCAN_ADDITIONAL_SKUS,
                    R.string.scan_barcode_or_qr_label);
        }
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
        progressDialog.setCancelable(true);

        progressDialog.setButton(ProgressDialog.BUTTON1, "Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        progressDialog.cancel();
                    }
                });

        progressDialog.show();

        progressDialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                ProductEditActivity.this.finish();
            }
        });
    }

    private void updateProduct() {
        // check whether we have active existing check task and do not allow to
        // save if it is still running
        if (checkCodeValidationRunning()) {
            return;
        }
        if (!newOptionPendingLoadingControl.isLoading()) {
            if (verifyForm()) {
                showProgressDialog(getString(R.string.updating_product_sku,
                        getSpecialAttributeValue(MAGEKEY_PRODUCT_SKU)));
                updateProductTask = new UpdateProduct(this);
                updateProductTask.execute();
            }
        } else {
            GuiUtils.alert("Wait for options creation...");
        }
    }

    private boolean verifyForm() {
        return super.verifyForm(true, false);
    }

    public void showSkuFieldIsBlankDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setMessage(R.string.sku_field_blank_dialog_message);

        alert.setPositiveButton(R.string.sku_field_blank_rescan,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        performClickOnSpecialAttribute(MAGEKEY_PRODUCT_SKU);
                    }
                });

        alert.setNegativeButton(R.string.cancel, null);
        alert.show();
    }

    @Override
    protected void onPriceEditDone() {
        super.onPriceEditDone();
        showUpdateConfirmationDialog();
    }

    public void showUpdateConfirmationDialog() {
        // The dialog should not be shown if it was already skipped or already
        // running or activity was destroyed or update task is running
        if (mUpdateConfirmationSkipped || mUpdateConfirmationShowing || !isActivityAlive()
                || updateProductTask != null) {
            return;
        }
        mUpdateConfirmationShowing = true;
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.confirmation);
        alert.setMessage(R.string.update_product_now_question);

        alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updateProduct();
            }
        });

        alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mUpdateConfirmationSkipped = true;
                mUpdateConfirmationShowing = false;
            }
        });

        alert.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                mUpdateConfirmationShowing = false;
            }
        });

        AlertDialog srDialog = alert.create();
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
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_SKU, "", true);
            }
        });

        AlertDialog srDialog = alert.create();

        srDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_SKU, "", true);
            }
        });

        srDialog.show();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case ADD_XXX_SKUS_QUESTION:
                AlertDialog.Builder addXxxSkusQuestionBuilder = new AlertDialog.Builder(
                        ProductEditActivity.this);

                addXxxSkusQuestionBuilder.setTitle("Confirmation");
                addXxxSkusQuestionBuilder.setMessage("Add " + mAdditionalSKUs.size() + " items?");

                double quantityValue = CommonUtils.parseNumber(getProduct().getQuantity()
                        .toString()) + mAdditionalSKUs.size();

                if (getProduct().getIsQtyDecimal() == 1) {
                    quantityV.setText(CommonUtils
                            .formatNumberWithFractionWithRoundUp(quantityValue));
                } else {
                    quantityV.setText(CommonUtils.formatDecimalOnlyWithRoundUp(quantityValue));
                }

                // If Pressed OK Submit the Order With Details to Site
                addXxxSkusQuestionBuilder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                updateProduct();
                            }
                        });

                addXxxSkusQuestionBuilder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ProductEditActivity.this.finish();
                            }
                        });

                addXxxSkusQuestionBuilder.setOnCancelListener(new OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        ProductEditActivity.this.finish();
                    }
                });

                AlertDialog addXxxSkusQuestion = addXxxSkusQuestionBuilder.create();

                addXxxSkusQuestion.setOnDismissListener(new OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        removeDialog(ADD_XXX_SKUS_QUESTION);
                    }
                });

                return addXxxSkusQuestion;
            default:
                return super.onCreateDialog(id);
        }
    }

    /**
     * Handles the Scan Process Result --> Get Barcode result and set it in GUI
     **/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCAN_QR_CODE)
        {
            if (resultCode == RESULT_OK)
            {
                skuScanCommon(data, SCAN_QR_CODE);

            } else if (resultCode == RESULT_CANCELED) {
                // Do Nothing
            }
        }

        if (requestCode == SCAN_BARCODE) {
            if (resultCode == RESULT_OK) {
                barcodeScanCommon(data, requestCode);
            } else if (resultCode == RESULT_CANCELED) {
                // Do Nothing
            }
        }

        if (requestCode == SCAN_ADDITIONAL_SKUS)
        {
            if (resultCode == RESULT_OK)
            {
                CheckSkuResult checkSkuResult = ScanActivity.checkSku(data);

                if (checkSkuResult != null) {
                    if (!mAdditionalSKUs.contains(checkSkuResult.code))
                    {
                        mAdditionalSKUs.add(checkSkuResult.code);
                    }
                }

                ScanUtils.startScanActivityForResult(ProductEditActivity.this,
                        SCAN_ADDITIONAL_SKUS, R.string.scan_barcode_or_qr_label);
            }
            else if (resultCode == RESULT_CANCELED)
            {
                if (mAdditionalSKUs.size() > 0)
                {
                    showDialog(ADD_XXX_SKUS_QUESTION);
                }
                else
                {
                    ProductEditActivity.this.finish();
                }
            }
        }
    }

    @Override
    protected void onGestureInputSuccess() {
        super.onGestureInputSuccess();
        showUpdateConfirmationDialog();
    }

    @Override
    protected void onKnownSkuCheckCompletedNotFound() {
        super.onKnownSkuCheckCompletedNotFound();
        showUpdateConfirmationDialog();
    }

    @Override
    protected void onKnownBarcodeCheckCompletedNotFound() {
        super.onKnownBarcodeCheckCompletedNotFound();
        // to avoid having 2 dialogs on the screen we should not show the update
        // confirmation dialog for the ISBN codes
        if (!BookInfoLoader.isIsbnCode(getSpecialAttributeValue(MAGEKEY_PRODUCT_BARCODE))) {
            showUpdateConfirmationDialog();
        }
    }

    /**
     * The method to call super.loadBookInfo method logic because it is
     * overridden by this class
     * 
     * @param code
     * @param attribute
     */
    public void super_loadBookInfo(String code, CustomAttribute attribute) {
        super.loadBookInfo(code, attribute);
    }

    @Override
    public void loadBookInfo(final String code, final CustomAttribute attribute) {
        // In product edit book information reloading is not automatic. App
        // should to prompt user before the operation
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.confirmation);
        alert.setMessage(R.string.reload_book_info_question);

        alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // call the super logic
                super_loadBookInfo(code, attribute);
            }
        });

        final Runnable negativeRunnable = new Runnable() {

            @Override
            public void run() {
                // hide possibly visible hint view with the invalid barcode
                // message specified in custom attribute
                // OnAttributeValueChangedListener
                if (attribute != null
                        && (mBookInfoLoader == null
                                || mBookInfoLoader.getCustomAttribute() != attribute || mBookInfoLoader
                                    .isFinished())) {
                    attribute.getHintView().setVisibility(View.GONE);
                }
            }
        };

        alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                negativeRunnable.run();
            }
        });

        alert.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                negativeRunnable.run();
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.show();
    }

    @Override
    protected void onAttributeUpdatedViaScan() {
        super.onAttributeUpdatedViaScan();
        showUpdateConfirmationDialog();
    }

    @Override
    protected boolean isCurrentCode(String code, boolean isBarcode) {
        boolean result = false;
        Product p = getProduct();
        if (p != null) {
            if (isBarcode) {
                result = code.equals(getBarcode(p));
            } else {
                result = code.equals(p.getSku());
            }
        }
        return result;
    }

    @Override
    void initWebActivityIntent(Intent intent) {
        super.initWebActivityIntent(intent);
        intent.putExtra(getString(R.string.ekey_product_sku), productSKU);
    }

    @Override
    protected boolean checkAttributeSetListCanBeShown() {
        boolean result = super.checkAttributeSetListCanBeShown();
        // if super conditions are passed check whether the product is loaded
        // and attribute set changing is supported by api version
        if (result) {
            Product product = getProduct();
            if (product == null) {
                result = false;
            } else {
                if (!product.isAttributeSetChangingSupported()) {
                    GuiUtils.alert(R.string.attribute_set_cannot_be_changed);
                    result = false;
                }
            }
        }
        return result;
    }

    @Override
    boolean isWebTextCopiedEventTarget(Intent extra) {
        // for product edit activity SKU extra passed to the WEB_TEXT_COPIED
        // broadcast event should be same as editing product this activity
        // opened for
        boolean result = TextUtils.equals(extra.getStringExtra(EventBusUtils.SKU), productSKU);
        if(result)
        {
            // if activity is resumed show dialog immediately, otherwise
            // schedule it to be shown when activity will be resumed
            if (isActivityResumed()) {
                showUpdateConfirmationDialog();
            } else {
                mShowUpdateConfirmationDialogOnResume = true;
            }
        }
        return result;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // if update confirmation dialog is scheduled to be shown
        if (mShowUpdateConfirmationDialogOnResume) {
            mShowUpdateConfirmationDialogOnResume = false;
            showUpdateConfirmationDialog();
        }
    }

    /**
     * Launch the product edit activity
     * 
     * @param productSku the SKU of the product to open edit activity for
     * @param additionalSKUsMode whether scan additional SKU mode is enabled
     * @param rescanAllMode whether the rescan all mode is enabled
     * @param allowToEdit whether the editing is allowed or save should be
     *            performed right after the details are loaded
     * @param skuToLinkWith the SKU of the product the editing product should be
     *            linked with. Used for configurable attributes
     * @param predefinedCustomAttributeValues the list of predefined custom
     *            attribute values which should be used to overwrite product
     *            attribute values
     * @param activity the activity from where the edit activity should be
     *            launched
     */
    public static void launchProductEdit(String productSku, boolean additionalSKUsMode,
            boolean rescanAllMode, boolean allowToEdit, String skuToLinkWith,
            ArrayList<CustomAttributeSimple> predefinedCustomAttributeValues, Activity activity) {

        final Intent intent = new Intent(activity, ProductEditActivity.class);
        intent.putExtra(CommonUtils.getStringResource(R.string.ekey_product_sku), productSku);
        intent.putExtra(CommonUtils.getStringResource(R.string.ekey_additional_skus_mode),
                additionalSKUsMode);
        intent.putExtra(CommonUtils.getStringResource(R.string.ekey_rescan_all_mode), rescanAllMode);

        intent.putExtra(EXTRA_ALLOW_TO_EDIT, allowToEdit);
        intent.putParcelableArrayListExtra(EXTRA_PREDEFINED_ATTRIBUTES,
                predefinedCustomAttributeValues);
        intent.putExtra(EXTRA_LINK_WITH_SKU, skuToLinkWith);

        activity.startActivity(intent);
    }
}
