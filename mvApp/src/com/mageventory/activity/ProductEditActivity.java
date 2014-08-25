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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.mageventory.R;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.Product;
import com.mageventory.model.util.ProductUtils;
import com.mageventory.tasks.BookInfoLoader;
import com.mageventory.tasks.LoadProduct;
import com.mageventory.tasks.UpdateProduct;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ScanUtils;

public class ProductEditActivity extends AbsProductActivity {

    private static final int ADD_XXX_SKUS_QUESTION = 1;

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

    // views
    public EditText quantityV;
    private TextView attrFormatterStringV;

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
    public UpdateProduct updateProductTask;

    private OnLongClickListener scanSKUOnClickL = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            ScanUtils.startScanActivityForResult(ProductEditActivity.this, SCAN_QR_CODE,
                    R.string.scan_barcode_or_qr_label);
            return true;
        }
    };

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

                descriptionV.setText(p.getDescription());
                nameV.setText(p.getName());
                setPriceTextValue(ProductUtils.getProductPricesString(p));
                specialPriceData.fromDate = p.getSpecialFromDate();
                specialPriceData.toDate = p.getSpecialToDate();
                weightV.setText(p.getWeight().toString());
                skuV.setText(p.getSku());

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

                final int atrSetId = p.getAttributeSetId();
                selectAttributeSet(atrSetId, false, false);
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

        if (customAttributesList.getList() != null) {
            for (CustomAttribute elem : customAttributesList.getList()) {
                elem.setSelectedValue((String) product.getData().get(elem.getCode()), true);
            }
            customAttributesList.setNameHint();
        }

        String formatterString = customAttributesList.getUserReadableFormattingString();

        if (formatterString != null) {
            attrFormatterStringV.setVisibility(View.VISIBLE);
            attrFormatterStringV.setText(formatterString);
        } else {
            attrFormatterStringV.setVisibility(View.GONE);
        }
        determineWhetherNameIsGeneratedAndSetProductName(product);
    }

    public String getBarcode(final Product product) {
        final List<Map<String, Object>> attrs = getAttributeList();
        if (attrs == null) {
            return "";
        }
        return product.getBarcode(attrs);
    }

    private OnLongClickListener scanBarcodeOnClickL = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            ScanUtils.startScanActivityForResult(ProductEditActivity.this, SCAN_BARCODE,
                    R.string.scan_barcode_or_qr_label);
            return true;
        }
    };

    private Button updateBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.product_edit);

        attributeLoadCount = new AtomicInteger(0);

        nameV = (AutoCompleteTextView) findViewById(R.id.product_name_input);
        nameV.setHorizontallyScrolling(false);
        nameV.setMaxLines(Integer.MAX_VALUE);

        absOnCreate();

        // map views
        descriptionV = (AutoCompleteTextView) findViewById(R.id.description_input);

        quantityV = (EditText) findViewById(R.id.quantity_input);
        weightV = (EditText) findViewById(R.id.weight_input);
        attrFormatterStringV = (TextView) findViewById(R.id.attr_formatter_string);

        // extras
        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            throw new IllegalStateException();
        }
        productSKU = extras.getString(getString(R.string.ekey_product_sku));
        mAdditionalSkusMode = extras.getBoolean(getString(R.string.ekey_additional_skus_mode));
        mRescanAllMode = extras.getBoolean(getString(R.string.ekey_rescan_all_mode));

        onProductLoadStart();

        // listeners

        OnClickListener updateClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {

                ProductEditActivity.this.hideKeyboard();
                updateProduct();
            }
        };

        OnEditorActionListener updateEditorActionListener = new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    InputMethodManager m = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    m.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    showUpdateConfirmationDialog();
                    return true;
                }

                return false;
            }
        };

        updateBtn = (Button) findViewById(R.id.update_btn);
        updateBtn.setOnClickListener(updateClickListener);

        nameV.setOnEditorActionListener(updateEditorActionListener);

        priceV.setOnEditorActionListener(updateEditorActionListener);
        skuV.setOnEditorActionListener(updateEditorActionListener);
        quantityV.setOnEditorActionListener(updateEditorActionListener);
        descriptionV.setOnEditorActionListener(updateEditorActionListener);
        barcodeInput.setOnEditorActionListener(updateEditorActionListener);
        weightV.setOnEditorActionListener(updateEditorActionListener);

        barcodeInput.setOnLongClickListener(scanBarcodeOnClickL);

        skuV.setOnLongClickListener(scanSKUOnClickL);

        customAttributesList.setOnEditDoneRunnable(new Runnable() {

            @Override
            public void run() {
                showUpdateConfirmationDialog();
            }
        });
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
        if (newAttributeOptionPendingCount == 0) {
            if (verifyForm()) {
                showProgressDialog(getString(R.string.updating_product_sku, skuV.getText()
                        .toString()));
                updateProductTask = new UpdateProduct(this);
                updateProductTask.execute();
            }
        } else {
            GuiUtils.alert("Wait for options creation...");
        }
    }

    private boolean verifyForm() {
        if (TextUtils.isEmpty(skuV.getText())) {
            if (TextUtils.isEmpty(barcodeInput.getText())) {
            	GuiUtils.activateField(skuV, false, true, false);
                showSkuFieldIsBlankDialog();
                return false;
            } else {
                skuV.setText(generateSku());
            }
        }

        if (!TextUtils.isEmpty(priceV.getText())) {
            if (!ProductUtils.isValidPricesString(priceV.getText().toString())) {
                GuiUtils.alert(R.string.invalid_price_information);
                GuiUtils.activateField(priceV, true, true, true);
                return false;
            }
        }

        if (!GuiUtils.validateBasicTextData(R.string.fieldCannotBeBlank, new int[] {
            R.string.price
        }, new TextView[] {
            priceV
        }, false)) {
            return false;
        }

        if (customAttributesList.getList() != null) {
            for (CustomAttribute elem : customAttributesList.getList()) {
                if (elem.getIsRequired() && TextUtils.isEmpty(elem.getSelectedValue())) {
                    GuiUtils.alert(R.string.pleaseSpecifyFirst, elem.getMainLabel());
                    GuiUtils.activateField(elem.getCorrespondingView(), true, true, false);
                    return false;
                }
            }
        }
        return true;
    }

    public void showSkuFieldIsBlankDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setMessage(R.string.sku_field_blank_dialog_message);

        alert.setPositiveButton(R.string.sku_field_blank_rescan,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        scanSKUOnClickL.onLongClick(skuV);
                    }
                });

        alert.setNegativeButton(R.string.cancel, null);
        alert.show();
    }

    @Override
    protected void onPriceEditDone(Double price, Double specialPrice, Date fromDate, Date toDate) {
        super.onPriceEditDone(price, specialPrice, fromDate, toDate);
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
                skuV.setText("");
            }
        });

        AlertDialog srDialog = alert.create();

        srDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                skuV.setText("");
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
                String contents = ScanUtils.getSanitizedScanResult(data);

                String[] urlData = contents.split("/");
                String sku;
                if (urlData.length > 0) {
                    if (ScanActivity.isLabelInTheRightFormat(contents))
                    {
                        sku = urlData[urlData.length - 1];
                    }
                    else
                    {
                        sku = contents;
                    }

                    if (!mAdditionalSKUs.contains(sku))
                    {
                        mAdditionalSKUs.add(sku);
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
        if (!BookInfoLoader.isIsbnCode(barcodeInput.getText().toString())) {
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
    protected void onDescriptionUpdatedViaScan() {
        super.onDescriptionUpdatedViaScan();
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
}
