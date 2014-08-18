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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.activity.AbsProductActivity.ProductLoadingControl.ProgressData;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.fragment.PriceEditFragment;
import com.mageventory.fragment.PriceEditFragment.OnEditDoneListener;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCacheManager.ProductDetailsExistResult;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.model.CustomAttributesList.AttributeViewAdditionalInitializer;
import com.mageventory.model.CustomAttributesList.OnAttributeValueChangedListener;
import com.mageventory.model.CustomAttributesList.OnNewOptionTaskEventListener;
import com.mageventory.model.Product;
import com.mageventory.model.util.ProductUtils;
import com.mageventory.model.util.ProductUtils.PricesInformation;
import com.mageventory.recent_web_address.RecentWebAddress;
import com.mageventory.recent_web_address.RecentWebAddressProvider.RecentWebAddresses;
import com.mageventory.recent_web_address.RecentWebAddressProviderAccessor;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.resprocessor.ProductDetailsProcessor.ProductDetailsLoadException;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.BookInfoLoader;
import com.mageventory.tasks.LoadAttributeSets;
import com.mageventory.tasks.LoadAttributesList;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.DialogUtil;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.InputCacheUtils;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.ScanUtils;
import com.mageventory.util.SimpleAsyncTask;
import com.mageventory.util.SimpleViewLoadingControl;
import com.mageventory.util.concurent.SerialExecutor;

@SuppressLint("NewApi")
public abstract class AbsProductActivity extends BaseFragmentActivity implements
        MageventoryConstants, OperationObserver {

    public static final String TAG = AbsProductActivity.class.getSimpleName();

    public static final int SCAN_ADDITIONAL_DESCRIPTION = 100;
    public static final int SCAN_ANOTHER_PRODUCT_CODE = 101;

    /**
     * Standalone task executor for the book info loading task to allow to run
     * existing barcode check and book info loading process simultaneously
     */
    public static SerialExecutor sBookInfoLoaderExecutor = new SerialExecutor(
            Executors.newSingleThreadExecutor());
    // icicle keys
    // private String IKEY_CATEGORY_REQID = "category request id";
    // private String IKEY_ATTRIBUTE_SET_REQID = "attribute set request id";

    // views
    protected LayoutInflater inflater;
    protected LinearLayout container;
    protected View atrListWrapperV;
    public ViewGroup atrListV;
    protected EditText attributeSetV;
    protected TextView atrSetLabelV;
    private int mDefaultAttrSetLabelVColor;
    protected TextView atrListLabelV;
    protected ProductLoadingControl mProductLoadingControl;
    protected LinearLayout layoutNewOptionPending;
    protected LinearLayout layoutSKUcheckPending;
    protected LinearLayout layoutBarcodeCheckPending;
    public AutoCompleteTextView nameV;
    public EditText skuV;
    public EditText priceV;
    public EditText weightV;
    public AutoCompleteTextView descriptionV;
    public EditText barcodeInput;
    protected int newAttributeOptionPendingCount;
    private OnNewOptionTaskEventListener newOptionListener;

    boolean attributeSetLongTap;

    protected Settings mSettings;

    // data
    // protected int categoryId;

    public CustomAttributesList customAttributesList;
    public int atrSetId = INVALID_ATTRIBUTE_SET_ID;

    // private int attributeSetRequestId = INVALID_REQUEST_ID;
    // private int categoryRequestId = INVALID_REQUEST_ID;

    // state
    private LoadAttributeSets atrSetsTask;
    private LoadAttributesList atrsListTask;
    private Dialog dialog;

    /* A reference to an in-ram copy of the input cache loaded from sdcard. */
    public Map<String, List<String>> inputCache;

    protected boolean isActivityAlive;
    protected boolean mRefreshPressed;
    private int mSkuLoadRequestId;
    private int mBarcodeLoadRequestId;
    private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();

    protected ProductInfoLoader backgroundProductInfoLoader;

    public long mGalleryTimestamp;

    public SpecialPricesData specialPriceData = new SpecialPricesData();

    ClipboardManager mClipboard;

    LoadingControl mDescriptionLoadingControl;
    ProductDescriptionLoaderTask mProductDescriptionLoaderTask;
    /**
     * Reference to the BookInfoLoader task so it may be cancelled if ISBN was
     * changed during loading
     */
    BookInfoLoader mBookInfoLoader;

    /**
     * Default text view color. Usually black
     */
    private int mDefaultTextColor;
    /**
     * Default text view error color. Usually red
     */
    private int mErrorTextColor;

    /**
     * Reference to the implementation of the
     * {@link OnAttributeValueChangedListener} which is used as a parameter
     * passed to the {@link CustomAttributesList} constructor
     */
    private OnAttributeValueChangedListener mOnAttributeValueChangedByUserInputListener = new CustomOnAttributeValueChangedListener();

    /**
     * Reference to the implementation of the
     * {@link AttributeViewAdditionalInitializer} which is used as a parameter
     * passed to the {@link CustomAttributesList} constructor
     */
    private AttributeViewAdditionalInitializer mAttributeViewAdditionalInitializer = new CustomAttributeViewAdditionalInitializer();

    /**
     * The flag indicating that barcode text changes should be ignored in the
     * barcodeInput text watcher. Used to do not run unnecessary barcode checks
     * when setting barcode input value programmatically
     */
    private boolean mIgnoreBarcodeTextChanges = false;

    // lifecycle

    /* Show a dialog informing the user that option creation failed */
    public void showNewOptionErrorDialog(String attributeName, String optionName) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Error");

        alert.setMessage("Cannot add \"" + optionName + "\" to \"" + attributeName + "\".");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog srDialog = alert.create();
        alert.show();
    }

    protected void absOnCreate() {
        mSettings = new Settings(this);
        mClipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        // find views
        container = (LinearLayout) findViewById(R.id.container);
        skuV = (EditText) findViewById(R.id.sku);
        priceV = (EditText) findViewById(R.id.price);
        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                    Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!ProductUtils.priceCharacterPattern.matcher("" + source.charAt(i))
                            .matches()) {
                        return "";
                    }
                }
                return null;
            }
        };
        priceV.setFilters(new InputFilter[] {
                filter
        });
        priceV.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                openPriceEditDialog();
                return true;
            }
        });
        priceV.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String priceText = priceV.getText().toString();
                if (ProductUtils.hasSpecialPrice(priceText)) {
                    openPriceEditDialog();
                }
            }
        });

        mErrorTextColor = getResources().getColor(R.color.red);
        mDefaultTextColor = skuV.getCurrentTextColor();

        mDescriptionLoadingControl = new SimpleViewLoadingControl(
                findViewById(R.id.description_load_progress));
        barcodeInput = (EditText) findViewById(R.id.barcode_input);
        atrListWrapperV = findViewById(R.id.attr_list_wrapper);
        attributeSetV = (EditText) findViewById(R.id.attr_set);
        atrListV = (ViewGroup) findViewById(R.id.attr_list);
        // attributeSetV = (EditText) findViewById(R.id.attr_set);
        atrListLabelV = (TextView) findViewById(R.id.attr_list_label);
        atrSetLabelV = (TextView) findViewById(R.id.atr_set_label);
        mDefaultAttrSetLabelVColor = atrSetLabelV.getCurrentTextColor();
        mProductLoadingControl = new ProductLoadingControl(findViewById(R.id.progressStatus));

        layoutNewOptionPending = (LinearLayout) findViewById(R.id.layoutNewOptionPending);
        layoutSKUcheckPending = (LinearLayout) findViewById(R.id.layoutSKUcheckPending);
        layoutBarcodeCheckPending = (LinearLayout) findViewById(R.id.layoutBarcodeCheckPending);

        inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        newOptionListener = new OnNewOptionTaskEventListener() {

            @Override
            public void OnAttributeCreationStarted() {
                newAttributeOptionPendingCount++;
                layoutNewOptionPending.setVisibility(View.VISIBLE);
            }

            @Override
            public void OnAttributeCreationFinished(String attributeName, String newOptionName,
                    boolean success) {
                newAttributeOptionPendingCount--;
                if (newAttributeOptionPendingCount == 0) {
                    layoutNewOptionPending.setVisibility(View.GONE);
                }

                if (success == false && isActivityAlive == true) {
                    showNewOptionErrorDialog(attributeName, newOptionName);
                }
            }
        };

        nameV.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (TextUtils.isEmpty(nameV.getText().toString())) {
                        nameV.setText(nameV.getHint());
                        nameV.selectAll();
                    }
                } else {
                    if (TextUtils.equals(nameV.getText(), nameV.getHint())) {
                        nameV.setText("");
                    }
                }
            }
        });

        if (this instanceof ProductEditActivity)
        {
            customAttributesList = new CustomAttributesList(this, atrListV, nameV,
                    newOptionListener, mOnAttributeValueChangedByUserInputListener,
                    mAttributeViewAdditionalInitializer, true);
        }
        else
        {
            customAttributesList = new CustomAttributesList(this, atrListV, nameV,
                    newOptionListener, mOnAttributeValueChangedByUserInputListener,
                    mAttributeViewAdditionalInitializer, false);
        }

        attributeSetV.setInputType(0);

        // attach listeners
        skuV.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus == false)
                {
                    String skuText = skuV.getText().toString();
                    if (!TextUtils.isEmpty(skuText))
                    {
                        // check whether manually entered sku is of the proper
                        // format. If it is not assume it is a barcode and clear
                        // skuV input, fill barcode input and perform code check
                        // as barcode. In other cases run sku already exists check
                        CheckSkuResult checkResult = checkSku(skuText);
                        if (checkResult.isBarcode) {
                            skuV.setText(null);
                            setBarcodeInputTextIgnoreChanges(checkResult.code);
                            onBarcodeChanged(checkResult.code);
                        } else {
                            skuV.setText(checkResult.code);
                            checkCodeExists(checkResult.code, false);
                        }
                    }
                }
            }
        });
        barcodeInput.setOnFocusChangeListener(new OnFocusChangeListener() {
        	
            /**
             * Field to store initial value when focus is gained
             */
            String mInitialValue;
            
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                String code = barcodeInput.getText().toString();
                if (hasFocus) {
                    mInitialValue = code;
                } else {
                    // if value was not changed when do not call checkCodeExists
                    if (!TextUtils.isEmpty(code) && !TextUtils.equals(code, mInitialValue)) {
                        checkCodeExists(code, true);
                    }
                }
            }
        });
        // support for live ISBN code recognition and book information loading
        barcodeInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // check whether the user typed value is ISBN code immediately
                // even if field is still editing
                if (barcodeInput.isFocused() && !mIgnoreBarcodeTextChanges) {
                    String code = s.toString();
                    if (!TextUtils.isEmpty(code)) {
                        checkBookBarcodeEntered(code);
                    }
                }
            }
        });

        attachListenerToEditText(attributeSetV, new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!attributeSetLongTap) {
                    showAttributeSetList();
                } else {
                    attributeSetLongTap = false;
                }
            }
        });

        // load data
        loadAttributesSet(false);
        resHelper.registerLoadOperationObserver(this);
        isActivityAlive = true;
    }

    protected void openPriceEditDialog() {
        PriceEditFragment detailsFragment = new PriceEditFragment();
        PricesInformation pi = ProductUtils.getPricesInformation(priceV.getText().toString());
        detailsFragment.setData(
                pi == null ? null : pi.regularPrice,
                pi == null ? null : pi.specialPrice,
                specialPriceData.fromDate,
                specialPriceData.toDate,
                new OnEditDoneListener() {

                    @Override
                    public void editDone(Double price, Double specialPrice, Date fromDate,
                            Date toDate) {
                        onPriceEditDone(price, specialPrice, fromDate, toDate);
                    }

                }
                );
        detailsFragment.show(getSupportFragmentManager(), PriceEditFragment.class.getSimpleName());
    }

    /**
     * Called when user presses OK button in the price edit dialog
     * 
     * @param price
     * @param specialPrice
     * @param fromDate
     * @param toDate
     */
    protected void onPriceEditDone(Double price, Double specialPrice, Date fromDate,
            Date toDate) {
        setPriceTextValue(ProductUtils.getProductPricesString(price, specialPrice));
        specialPriceData.fromDate = fromDate;
        specialPriceData.toDate = toDate;
    }

    /**
     * Set the priceV field text value and adjust its availability for different
     * pcie formats
     * 
     * @param price
     */
    protected void setPriceTextValue(String price) {
        priceV.setText(price);
        boolean editable = !ProductUtils.hasSpecialPrice(price);
        priceV.setInputType(editable ? InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL : InputType.TYPE_NULL);
        priceV.setFocusable(editable);
        priceV.setFocusableInTouchMode(editable);
        priceV.setCursorVisible(editable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resHelper.unregisterLoadOperationObserver(this);
        isActivityAlive = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            loadAttributesSet(true);
            mRefreshPressed = true;
            return true;
        }
        return super.onOptionsItemSelected(item);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == SCAN_ADDITIONAL_DESCRIPTION) {
            if (resultCode == RESULT_OK) {
                String contents = ScanUtils.getSanitizedScanResult(intent);
                if (contents != null) {
                    descriptionV.setText(contents);
                    onDescriptionUpdatedViaScan();
                }
            }
        } else if (requestCode == SCAN_ANOTHER_PRODUCT_CODE) {
            if (resultCode == RESULT_OK) {
                String contents = ScanUtils.getSanitizedScanResult(intent);
                String[] urlData = contents.split("/");
                if (urlData.length > 0) {
                    String sku;
                    if (ScanActivity.isLabelInTheRightFormat(contents)) {
                        sku = urlData[urlData.length - 1];
                    } else {
                        sku = contents;
                    }
                    if (mProductDescriptionLoaderTask != null) {
                        mProductDescriptionLoaderTask.cancel(true);
                    }
                    mProductDescriptionLoaderTask = new ProductDescriptionLoaderTask(sku);
                    mProductDescriptionLoaderTask.execute();
                }
            }
        } else if (requestCode == LAUNCH_GESTURE_INPUT) {
            if (resultCode == RESULT_OK) {

                View currentFocus = getCurrentFocus();

                if (currentFocus instanceof EditText) {
                    EditText editText = (EditText) currentFocus;

                    Bundle extras = (Bundle) intent.getExtras();
                    if (extras != null) {
                        String out = extras.getString("OUTPUT_TEXT_KEY");
                        editText.setText(out);
                        onGestureInputSuccess();
                    }

                }
            }
        }
    }

    // methods

    public String generateSku() {
        /*
         * Since we can't get microsecond time in java we just use milliseconds
         * time and add microsecond part from System.nanoTime() which doesn't
         * return a normal timestamp but a number of nanoseconds from some
         * arbitrary point in time which we don't know. This should be enough to
         * make every SKU we'll ever generate different.
         */
        return "P" + System.currentTimeMillis() + (System.nanoTime() / 1000) % 1000;
    }

    private void checkCodeExists(String code, boolean isBarcode)
    {
        if (isCurrentCode(code, isBarcode)) {
            return;
        }
        if (backgroundProductInfoLoader != null)
        {
            backgroundProductInfoLoader.cancel(false);
        }

        backgroundProductInfoLoader = new ProductInfoLoader(code, isBarcode);
        backgroundProductInfoLoader.execute();
    }

    public abstract void showInvalidLabelDialog(final String settingsDomainName,
            final String skuDomainName);

    /* Return true if invalid label dialog was displayed and false otherwise */
    protected boolean skuScanCommon(Intent intent, int requestCode)
    {
        String contents = ScanUtils.getSanitizedScanResult(intent);


        if (!TextUtils.isEmpty(contents)) {

            // check whether sku is of the right format. If it is not assume it
            // is a barcode
            CheckSkuResult checkResult = checkSku(contents);

            if (checkResult.isBarcode)
            {
                skuV.setText(generateSku());
                return barcodeScanCommon(checkResult.code, requestCode, false, true);
            }
            else
            {
                mGalleryTimestamp = 0;

                skuV.setText(checkResult.code);

                if (JobCacheManager.saveRangeStart(checkResult.code, mSettings.getProfileID(), 0) == false)
                {
                    ProductDetailsActivity.showTimestampRecordingError(this);
                }

                checkCodeExists(checkResult.code, false);
            }
        }

        boolean invalidLabelDialogShown = false;

        /*
         * Check if the label is valid in relation to the url set in the
         * settings and show appropriate information if it's not.
         */
        if (!ScanActivity.isLabelValid(this, contents))
        {
            Settings settings = new Settings(this);
            String settingsUrl = settings.getUrl();

            if (!ScanActivity.domainPairRemembered(ScanActivity.getDomainNameFromUrl(settingsUrl),
                    ScanActivity.getDomainNameFromUrl(contents)))
            {
                showInvalidLabelDialog(ScanActivity.getDomainNameFromUrl(settingsUrl),
                        ScanActivity.getDomainNameFromUrl(contents));
                invalidLabelDialogShown = true;
            }
        }

        GuiUtils.activateField(priceV, true, true, true);

        return invalidLabelDialogShown;
    }

    /**
     * Common behavior for the Create/Edit activities when the barcode scanned
     * 
     * @param data intent data from the onActivityResult method
     * @param requestCode request code the activity result is received for
     */
    protected void barcodeScanCommon(Intent data, final int requestCode) {
        barcodeScanCommon(ScanUtils.getSanitizedScanResult(data), requestCode, true, false);
    }

    /**
     * Common behavior for the Create/Edit activities when the barcode scanned
     * 
     * @param code scanned code
     * @param requestCode request code the activity result is received for
     * @param activateWeightField whether to activate weight field after the
     *            barcode check
     * @param activatePriceField whether to activate price field after the
     *            barcode check
     * @return
     */
    protected boolean barcodeScanCommon(final String code, final int requestCode,
            final boolean activateWeightField, final boolean activatePriceField) {
        mGalleryTimestamp = JobCacheManager.getGalleryTimestampNow();

        final Runnable barcodeScannedRunnable = new Runnable() {

            @Override
            public void run() {
                // Set Barcode in Product Barcode TextBox
                setBarcodeInputTextIgnoreChanges(code);

                onBarcodeChanged(code);

                if (activateWeightField) {
                    GuiUtils.activateField(weightV, true, false, true);
                }
                if (activatePriceField) {
                    GuiUtils.activateField(priceV, true, true, true);
                }
            }
        };
        return showMissingMetadataDialogIfNecessary(code, barcodeScannedRunnable,
                new Runnable() {

                    @Override
                    public void run() {
                        ScanUtils.startScanActivityForResult(AbsProductActivity.this, SCAN_BARCODE,
                                R.string.scan_barcode_or_qr_label);
                    }
                }, AbsProductActivity.this, mSettings);
    }

    /**
     * Show missing metadata dialog in case the scanned code should to have an
     * extra data but that metadata was not scanned (missing). For a now
     * such check is performed for ISSN codes only
     * 
     * @param code the barcode to check
     * @param barcodeScannedRunnable the runnable which should be run if check
     *            is successful or user decided to ignore the invalid check
     *            result
     * @param rescanRunnable the runnable which should be run if user presses
     *            rescan button
     * @param activity the activity which should to hold question dialog
     * @param settings instance of settings
     * @return true if the missing metadata dialog was shown
     */
    public static boolean showMissingMetadataDialogIfNecessary(String code,
            Runnable barcodeScannedRunnable, Runnable rescanRunnable, Activity activity,
            final Settings settings) {
        boolean result = false;
        // if scanned code is of ISSN format and missing meta data dialog is not
        // complitely disabled
        if (BookInfoLoader.isIssnCode(code) && settings.isIssnMissingMetadataRescanRequestEnabled()) {
            showMissingMetadataDialog(barcodeScannedRunnable, rescanRunnable, activity, settings);
            result = true;
        } else {
            // code is valid or dialog disabled, so run scheduled barcode
            // handling operation
            barcodeScannedRunnable.run();
        }
        return result;
    }

    /**
     * Show the missing metadata dialog
     * 
     * @param barcodeScannedRunnable the runnable which should be run if user
     *            decided to ignore the invalid check result
     * @param rescanRunnable the runnable which should be run if user presses
     *            rescan button
     * @param activity the activity which should to hold question dialog
     * @param settings instance of settings
     */
    public static void showMissingMetadataDialog(final Runnable barcodeScannedRunnable, final Runnable rescanRunnable,
            Activity activity, final Settings settings) {
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        alert.setMessage(R.string.scan_issn_metadata_missed_question);

        alert.setNegativeButton(R.string.scan_issn_metadata_missed_ignore,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        barcodeScannedRunnable.run();
                    }
                });

        alert.setNeutralButton(R.string.scan_issn_metadata_missed_rescan,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        rescanRunnable.run();
                    }
                });

        alert.setPositiveButton(R.string.scan_issn_metadata_missed_ignore_forever,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        barcodeScannedRunnable.run();
                        // remember the user choice and do not show dialog again
                        settings.setIssnMissingMetadataRescanRequestEnabled(false);
                    }
                });

        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                barcodeScannedRunnable.run();
            }
        });
        alert.show();
    }

    /**
     * Set the barcodeInput text but specify mIgnoreBarcodeTextChanges flag
     * during operation so related TextWatcher will ignore such changes
     * 
     * @param text the text to set
     */
    protected void setBarcodeInputTextIgnoreChanges(String text) {
        mIgnoreBarcodeTextChanges = true;
        try {
            barcodeInput.setText(text);
        } finally {
            mIgnoreBarcodeTextChanges = false;
        }
    }
    /**
     * The method which should be called when the barcode is changed in various
     * circumstances: either scan, or entering by hand
     * 
     * @param code
     */
    protected void onBarcodeChanged(String code) {
        // run book barcode check simultaneously
        checkBookBarcodeEntered(code);
        checkCodeExists(code, true);
    }

    protected void onKnownSkuCheckCompletedNotFound() {
    }

    protected void onKnownBarcodeCheckCompletedNotFound() {
    }

    protected void onDescriptionUpdatedViaScan() {
    }
    
    protected void onGestureInputSuccess() {
    }
    
    public static String getProductName(AbsProductActivity apa, EditText nameEditText) {
        String name = nameEditText.getText().toString();

        // check there are any other character than spaces
        if (name.trim().length() > 0) {
            return name;
        }

        return apa.customAttributesList.getCompoundName();
    }

    /**
     * @param product
     */
    public void determineWhetherNameIsGeneratedAndSetProductName(Product product) {
        boolean generatedName = product.getName() == null
                || product.getName().equals(customAttributesList.getCompoundName());
        nameV.setText(generatedName ? null : product.getName());
    }

    protected void showAttributeSetListOrSelectDefault() {
        List<Map<String, Object>> attrSets = getAttributeSets();
        if (attrSets != null && attrSets.size() == 1) {
            Map<String, Object> attrSet = attrSets.get(0);
            int atrSetId;
            try {
                atrSetId = JobCacheManager.safeParseInt(attrSet.get(MAGEKEY_ATTRIBUTE_SET_ID),
                        INVALID_ATTRIBUTE_SET_ID);
            } catch (Throwable e) {
                atrSetId = INVALID_ATTRIBUTE_SET_ID;
            }
            selectAttributeSet(atrSetId, false, false);
            onAttributeSetItemClicked();
        } else {
            showAttributeSetList();
        }
    }

    /**
     * Check whether the attribute set list is already loaded and can be shown.
     * Also check whether the activity is still alive
     * 
     * @return true if attribute set list is loaded and can be shown, otherwise
     *         returns false
     */
    protected boolean checkAttributeSetListCanBeShown() {
        if (isActivityAlive == false) {
            return false;
        }
        List<Map<String, Object>> atrSets = getAttributeSets();
        if (atrSets == null || atrSets.isEmpty()) {
            return false;
        }
        return true;
    }

    protected void showAttributeSetList() {
        if (!checkAttributeSetListCanBeShown()) {
            return;
        }

        List<Map<String, Object>> atrSets = getAttributeSets();

        final Dialog attrSetListDialog = DialogUtil.createListDialog(this, "Product types",
                atrSets,
                android.R.layout.simple_list_item_1, new String[] {
                    MAGEKEY_ATTRIBUTE_SET_NAME
                },
                new int[] {
                    android.R.id.text1
                }, new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        final Object item = arg0.getAdapter().getItem(arg2);
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> itemData = (Map<String, Object>) item;

                        int atrSetId;
                        try {
                            atrSetId = JobCacheManager.safeParseInt(itemData.get(
                                    MAGEKEY_ATTRIBUTE_SET_ID), INVALID_ATTRIBUTE_SET_ID);
                        } catch (Throwable e) {
                            atrSetId = INVALID_ATTRIBUTE_SET_ID;
                        }

                        dialog.dismiss();

                        if (AbsProductActivity.this instanceof ProductEditActivity)
                        {
                            customAttributesList = new CustomAttributesList(
                                    AbsProductActivity.this, atrListV, nameV, newOptionListener,
                                    mOnAttributeValueChangedByUserInputListener,
                                    mAttributeViewAdditionalInitializer, true);
                        }
                        else
                        {
                            customAttributesList = new CustomAttributesList(
                                    AbsProductActivity.this, atrListV, nameV, newOptionListener,
                                    mOnAttributeValueChangedByUserInputListener,
                                    mAttributeViewAdditionalInitializer, false);
                        }
                        selectAttributeSet(atrSetId, false, false);
                        onAttributeSetItemClicked();
                    }
                });
        (dialog = attrSetListDialog).show();
    }

    protected void onAttributeSetItemClicked() {

    }

    protected void selectAttributeSet(final int setId, final boolean forceRefresh,
            boolean loadLastUsed) {
        if (setId == INVALID_ATTRIBUTE_SET_ID) {
            return;
        }

        atrSetId = setId;

        final List<Map<String, Object>> sets = getAttributeSets();
        if (sets == null) {
            return;
        }

        for (Map<String, Object> set : sets) {
            final int tmpSetId;
            try {
                int failTmpSetId = Integer.MIN_VALUE;
                tmpSetId = JobCacheManager.safeParseInt(set.get(MAGEKEY_ATTRIBUTE_SET_ID),
                        failTmpSetId);
                if (tmpSetId == failTmpSetId)
                {
                    continue;
                }
            } catch (Throwable e) {
                continue;
            }
            if (tmpSetId == setId) {
                try {
                    final String atrSetName = set.get(MAGEKEY_ATTRIBUTE_SET_NAME).toString();
                    attributeSetV.setText(atrSetName);
                } catch (Throwable ignored) {
                }
                break;
            }
        }
        if (loadLastUsed) {
            customAttributesList = CustomAttributesList.loadFromCache(this, atrListV, nameV,
                    newOptionListener, mSettings.getUrl(), customAttributesList);
            atrListLabelV.setTextColor(mDefaultAttrSetLabelVColor);
            showAttributeListV(false);
        } else {
            loadAttributeList(forceRefresh);
        }
    }

    // resources

    protected List<Map<String, Object>> getAttributeList() {

        if (atrSetId == INVALID_ATTRIBUTE_SET_ID)
            return null;

        if (atrsListTask == null) {
            return null;
        }

        if (atrsListTask.getData() == null) {
            return null;
        }

        List<Map<String, Object>> list = (List<Map<String, Object>>) atrsListTask.getData();

        return list;
    }

    private List<Map<String, Object>> getAttributeSets() {

        if (atrSetsTask == null) {
            return null;
        }

        if (atrSetsTask.getData() == null) {
            return null;
        }

        List<Map<String, Object>> list = atrSetsTask.getData();

        return list;
    }

    protected void loadAttributesSet(final boolean refresh) {

        // attr sets
        if (atrSetsTask == null || atrSetsTask.getState() == TSTATE_CANCELED) {
            //
        } else {
            atrSetsTask.setHost(null);
            atrSetsTask.cancel(true);
        }
        atrSetsTask = new LoadAttributeSets();
        atrSetsTask.setHost(this);
        atrSetsTask.execute(refresh);
    }

    protected void loadAttributeList(final boolean refresh) {
        if (atrSetId == INVALID_ATTRIBUTE_SET_ID)
            return;

        if (atrsListTask == null || atrsListTask.getState() == TSTATE_CANCELED) {
            //
        } else {
            atrsListTask.setHost(null);
            atrsListTask.cancel(true);
        }
        atrsListTask = new LoadAttributesList("" + atrSetId);
        atrsListTask.setHost(this);
        atrsListTask.execute(refresh);
    }

    protected void removeAttributeListV() {
        atrListWrapperV.setVisibility(View.GONE);
        atrListV.removeAllViews();
    }

    private void showAttributeListV(boolean showProgressBar) {
        if (showProgressBar == false
                && (customAttributesList.getList() == null || customAttributesList.getList().size() == 0))
        {
            atrListWrapperV.setVisibility(View.GONE);
        }
        else
        {
            atrListWrapperV.setVisibility(View.VISIBLE);
        }
        if (showProgressBar) {
            mProductLoadingControl.startLoading(ProgressData.ATTRIBUTES_LIST);
        } else {
            mProductLoadingControl.stopLoading(ProgressData.ATTRIBUTES_LIST);
        }
    }

    /*
     * Called when user creates/updates a product. This function stores all new
     * attribute values in the cache.
     */
    public void updateInputCacheWithCurrentValues()
    {
        String newNameValue = nameV.getText().toString();
        String newDescriptionValue = descriptionV.getText().toString();

        InputCacheUtils.addValueToInputCacheList(MAGEKEY_PRODUCT_NAME, newNameValue, inputCache);
        InputCacheUtils.addValueToInputCacheList(MAGEKEY_PRODUCT_DESCRIPTION, newDescriptionValue,
                inputCache);

        if (customAttributesList != null && customAttributesList.getList() != null)
        {
            for (CustomAttribute customAttribute : customAttributesList.getList())
            {
                if (customAttribute.isOfType(CustomAttribute.TYPE_TEXT)
                        || customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA))
                {
                    InputCacheUtils.addValueToInputCacheList(customAttribute.getCode(),
                            ((EditText) customAttribute.getCorrespondingView()).getText()
                                    .toString(), inputCache);
                }
            }
        }

        JobCacheManager.storeInputCache(inputCache, mSettings.getUrl());
    }

    // task listeners

    /* Called when input cache finishes loading. */
    public void onInputCacheLoaded(Map<String, List<String>> ic) {
        if (inputCache == null)
        {
            if (ic != null)
            {
                inputCache = ic;
            }
            else
            {
                inputCache = new HashMap<String, List<String>>();
            }

            /* Associate auto completion adapter with the "name" edit text */
            InputCacheUtils.initAutoCompleteTextViewWithAdapterFromInputCache(MAGEKEY_PRODUCT_NAME,
                    inputCache, nameV, AbsProductActivity.this);

            /*
             * Associate auto completion adapter with the "description" edit
             * text
             */
            InputCacheUtils.initAutoCompleteTextViewWithAdapterFromInputCache(
                    MAGEKEY_PRODUCT_DESCRIPTION, inputCache, descriptionV, AbsProductActivity.this);
        }
    }

    public void onAttributeSetLoadStart() {
        atrSetLabelV.setTextColor(getResources().getColor(R.color.attr_set_label_color_loading));
        mProductLoadingControl.startLoading(ProgressData.ATTRIBUTE_SETS);
        attributeSetV.setClickable(false);
        attributeSetV.setHint("Loading product types...");
    }

    public void onAttributeSetLoadFailure() {
        atrSetLabelV.setTextColor(getResources().getColor(R.color.attr_set_label_color_error));
        mProductLoadingControl.stopLoading(ProgressData.ATTRIBUTE_SETS);
        attributeSetV.setClickable(true);
        attributeSetV.setHint("Load failed... Check settings and refresh");
    }

    public void onAttributeSetLoadSuccess() {
        atrSetLabelV.setTextColor(mDefaultAttrSetLabelVColor);
        if (mRefreshPressed) {
            loadAttributeList(false);
            mRefreshPressed = false;
        }
        mProductLoadingControl.stopLoading(ProgressData.ATTRIBUTE_SETS);
        attributeSetV.setClickable(true);
        attributeSetV.setHint("Click to select an attribute set...");
    }

    public void onAttributeListLoadSuccess() {
        atrListLabelV.setTextColor(mDefaultAttrSetLabelVColor);
        List<Map<String, Object>> atrList = getAttributeList();

        if (atrList != null) {
            customAttributesList.loadFromAttributeList(atrList, atrSetId);

            showAttributeListV(false);
        }

        if (atrList == null || atrList.size() == 0) {
            atrListWrapperV.setVisibility(View.GONE);
        }
    }

    public void onAttributeListLoadFailure() {
        atrListLabelV.setTextColor(getResources().getColor(R.color.attr_set_label_color_error));
        mProductLoadingControl.stopLoading(ProgressData.ATTRIBUTES_LIST);
    }

    public void onAttributeListLoadStart() {
        // clean the list
        atrListLabelV.setTextColor(mDefaultAttrSetLabelVColor);
        removeAttributeListV();
        showAttributeListV(true);
    }

    private OnLongClickListener scanBarcodeOnClickL = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            ScanUtils.startScanActivityForResult(AbsProductActivity.this, SCAN_BARCODE,
                    R.string.scan_barcode_or_qr_label);
            return true;
        }
    };

    // helper methods

    private static void attachListenerToEditText(final EditText view, final OnClickListener onClickL) {
        view.setOnClickListener(onClickL);
    }

    @Override
    public void onLoadOperationCompleted(LoadOperation op) {
        if (isActivityAlive) {
            if (op.getOperationRequestId() == mSkuLoadRequestId)
            {
                if (op.getException() == null) {
                    showKnownSkuDialog(op.getResourceParams()[1]);
                }
                else
                {
                    /*
                     * Product sku was not found on the server but we still need
                     * to hide the progress indicator.
                     */
                    layoutSKUcheckPending.setVisibility(View.GONE);

                    onKnownSkuCheckCompletedNotFound();
                }
            }
            if (op.getOperationRequestId() == mBarcodeLoadRequestId) {
                if (op.getException() == null) {
                    showKnownBarcodeDialog(op.getResourceParams()[1]);
                } else {
                    /*
                     * Product sku was not found on the server but we still need
                     * to hide the progress indicator.
                     */
                    layoutBarcodeCheckPending.setVisibility(View.GONE);

                    onKnownBarcodeCheckCompletedNotFound();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        confirmExit();
    }

    public void onConfirmedExit() {
        finish();
    }

    public void confirmExit() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setMessage(R.string.create_edit_confirm_exit_without_saving);

        alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                onConfirmedExit();
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void showKnownSkuDialog(final String sku) {
        showKnownSkuOrBarcodeDialog(sku, false);
    }

    public void showKnownBarcodeDialog(final String code) {
        showKnownSkuOrBarcodeDialog(code, true);
    }
    
    public void showKnownSkuOrBarcodeDialog(final String code, final boolean isBarcode) {
        int message;
        if (isBarcode) {
            layoutBarcodeCheckPending.setVisibility(View.GONE);
            message = R.string.known_barcode_question;
        } else {
            layoutSKUcheckPending.setVisibility(View.GONE);
            message = R.string.known_sku_question;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.question);
        alert.setMessage(getString(message, code));

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                if (isBarcode) {
                    ScanActivity.startForSku(code, AbsProductActivity.this);
                } else {
                    launchProductDetails(code);
                }
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isBarcode) {
                    setBarcodeInputTextIgnoreChanges("");
                } else {
                    skuV.setText("");
                }
            }
        });

        AlertDialog srDialog = alert.create();
        srDialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (isBarcode) {
                    setBarcodeInputTextIgnoreChanges("");
                } else {
                    skuV.setText("");
                }
            }
        });
        srDialog.show();
    }

    protected boolean isCurrentCode(String code, boolean isBarcode) {
        return false;
    }

    private void launchProductDetails(String sku)
    {
        final String ekeyProductSKU = getString(R.string.ekey_product_sku);
        final Intent intent = new Intent(getApplicationContext(), ProductDetailsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(ekeyProductSKU, sku);

        startActivity(intent);
    }

    protected void initDescriptionField() {
        descriptionV.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                registerForContextMenu(v);
                v.showContextMenu();
                unregisterForContextMenu(v);
                return true;
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v.getId() == descriptionV.getId()) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.additional_description_paste, menu);
            MenuItem pasteItem = menu.findItem(R.id.menu_paste);
            boolean pasteEnabled;
            if (!(mClipboard.hasPrimaryClip())) {

                pasteEnabled = false;

            } else if (!(mClipboard.getPrimaryClipDescription()
                    .hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))) {

                // This disables the paste menu item, since the clipboard has
                // data but it is not plain text
                pasteEnabled = false;
            } else {

                // This enables the paste menu item, since the clipboard
                // contains plain text.
                pasteEnabled = true;
            }
            pasteItem.setEnabled(pasteEnabled);
            super.onCreateContextMenu(menu, v, menuInfo);
        } else {
            super.onCreateContextMenu(menu, v, menuInfo);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int menuItemIndex = item.getItemId();
        switch (menuItemIndex) {
            case R.id.menu_paste:
                // Examines the item on the clipboard. If getText() does not
                // return null, the clip item contains the
                // text. Assumes that this application can only handle one item
                // at a time.
                ClipData.Item citem = mClipboard.getPrimaryClip().getItemAt(0);

                CharSequence pasteData = citem.coerceToText(this);
                descriptionV.setText(pasteData);
                break;
            case R.id.menu_scan_free_text:
                ScanUtils.startScanActivityForResult(this, SCAN_ADDITIONAL_DESCRIPTION,
                        R.string.scan_free_text);
                break;
            case R.id.menu_copy_from_another:
                ScanUtils.startScanActivityForResult(this, SCAN_ANOTHER_PRODUCT_CODE,
                        R.string.scan_barcode_or_qr_label);
                break;
            case R.id.menu_search_the_internet:
                prepareAndShowRecentWebAddressesDialog();
                break;
            default:
                return super.onContextItemSelected(item);
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Check whether the entered barcode is of ISBN format and start loading of
     * book information if it is. Note Google Books API key is required for this
     * operation
     * 
     * @param code
     */
    public void checkBookBarcodeEntered(String code) {
        if (BookInfoLoader.isIsbnCode(code)) {
            loadBookInfo(code, null);
        } else {
            // if code format is invalid we need to stop previously run book
            // info loading task in case it was run for the barcode input
            stopBookInfoLoadingIfSource(null);
        }
    }

    /**
     * Start loading of book information for the ISBN code. The code check
     * should be performed before calling this method. Note Google Books API key
     * is required for this operation.
     * 
     * @param code the code to load book information for
     * @param attribute the related attribute to the code value. It may be
     *            either bk_isbn_10_ or bk_isbn_13_. In case of null value it
     *            assumed to be a barcodeInput field. It is used to construct
     *            proper {@link LoadingControl} so user may see in place loading
     *            indicator
     */
    public void loadBookInfo(String code, CustomAttribute attribute) {
        Settings settings = new Settings(getApplicationContext());
        String apiKey = settings.getAPIkey();
        if (TextUtils.equals(apiKey, "")) {
            GuiUtils.alert(R.string.alert_book_search_disabled);
        } else {
            // immediately cancel previously running book info loader task
            if (mBookInfoLoader != null) {
                mBookInfoLoader.cancel(true);
                mBookInfoLoader.stopLoading();
            }
            LoadingControl loadingControl;
            // if attribute is null when the book info loading operation is
            // performed for the barcodeInput field. Else the attribute
            // corresponding views are used for the LoadingControl
            if (attribute == null) {
                View bookLoadingView = findViewById(R.id.bookLoadingView);
                TextView bookLoadingHint = (TextView) bookLoadingView
                        .findViewById(R.id.bookLoadingHint);
                loadingControl = new BookInfoLoadingControl(bookLoadingHint, bookLoadingView, false);
            } else {
                loadingControl = new BookInfoLoadingControl(attribute.getHintView(),
                        attribute.getNewOptionSpinningWheel(), true);
            }
            mBookInfoLoader = new BookInfoLoader(this, customAttributesList,
                    BookInfoLoader.sanitizeIsbnOrIssn(code), apiKey, null, loadingControl);
            mBookInfoLoader.executeOnExecutor(sBookInfoLoaderExecutor);
        }
    }
    
    /**
     * Checks whether the current book info loading operation should be stopped
     * immediately. It occurs in case previous book info loading operation was
     * run exactly for the same attribute or barcode input field. It is used if
     * user typed valid ISBN code and book info loading operations started but
     * then user edited code and it became invalid. In such case book info
     * loading operation should be stopped immediately
     * 
     * @param attribute the related attribute to check. It may be either
     *            bk_isbn_10_ or bk_isbn_13_. In case of null value it assumed
     *            to be a barcodeInput field.
     */
    void stopBookInfoLoadingIfSource(CustomAttribute attribute) {
        if (mBookInfoLoader != null && attribute == mBookInfoLoader.getCustomAttribute()) {
            mBookInfoLoader.cancel(true);
            mBookInfoLoader.stopLoading();
        }
    }

    /**
     * Check whether the code validation task is running and warn if it is
     * 
     * @return true if code validation is running, false if not
     */
    public boolean checkCodeValidationRunning() {
        // for a now check is disabled
        return false;
        // currently such check is disabled. We allow to save even if code check
        // is still running. Uncomment code below in case such check is
        // required.
//        if (backgroundProductInfoLoader != null) {
//            GuiUtils.alert(backgroundProductInfoLoader.isCheckBarcode() ? R.string.wait_until_existing_barcode_check_complete
//                    : R.string.wait_until_existing_sku_check_complete);
//            return true;
//        }
//        return false;
    }

    /**
     * Check the code format whether it is proper SKU or it is a barcode
     * 
     * @param code
     * @return CheckSkuResult which contains filtered code and code type
     *         (barcode or SKU)
     */
    static CheckSkuResult checkSku(String code) {
        boolean isBarcode = false;
        if (ScanActivity.isLabelInTheRightFormat(code)) {
            String[] urlData = code.split("/");
            code = urlData[urlData.length - 1];
        } else {
            if (!ScanActivity.isSKUInTheRightFormat(code))
                isBarcode = true;
        }
        return new CheckSkuResult(isBarcode, code);
    }

    /**
     * Load the recent web addresses information and show it in the dialog as
     * list
     */
    void prepareAndShowRecentWebAddressesDialog() {
        new LoadRecentWebAddressesTask().execute();
    }

    /**
     * Show the recent web addresses dialog for the already loaded information.
     * Data will be shown as a list with possibility to select any item
     * 
     * @param recentWebAddresses preloaded recent web addresses information
     */
    void showRecentWebAddressesDialog(List<RecentWebAddress> recentWebAddresses) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.recent_web_addresses);

        final RecentWebAddressesAdapter adapter = new RecentWebAddressesAdapter(recentWebAddresses);
        alert.setAdapter(adapter, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                startWebActivity(adapter.getItem(which));
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alert.show();
    }

    /**
     * Start the web activity for the recent web address
     * 
     * @param address to start the activity for
     */
    private void startWebActivity(RecentWebAddress address) {
        Intent intent = new Intent(this, WebActivity.class);
        // for the product name we use the value specified in the nameV field if
        // present. Otherwise use the nameV hint information
        String name = nameV.getText().toString();
        if (TextUtils.isEmpty(name)) {
            name = nameV.getHint().toString();
        }
        intent.putExtra(getString(R.string.ekey_product_name), name);
        intent.putExtra(getString(R.string.ekey_domain), address.getDomain());
        initWebActivityIntent(intent);
        startActivity(intent);
    }

    /**
     * Additional init of web activity intent. Used in
     * {@link ProductEditActivity} to specify product sku information
     * 
     * @param intent
     */
    void initWebActivityIntent(Intent intent) {
    }

    /**
     * A result wrapper for the checkSku method. Contains filtered code and its
     * type (barcode or SKU)
     */
    static class CheckSkuResult {
        boolean isBarcode;
        String code;

        public CheckSkuResult(boolean isBarcode, String code) {
            super();
            this.isBarcode = isBarcode;
            this.code = code;
        }
    }

    private class ProductDescriptionLoaderTask extends SimpleAsyncTask implements OperationObserver {

        private boolean success;
        private boolean doesntExist;
        private String mSku;
        private CountDownLatch doneSignal;
        private int requestId = MageventoryConstants.INVALID_REQUEST_ID;

        private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
        Product mProduct;
        SettingsSnapshot mSettingsSnapshot;

        public ProductDescriptionLoaderTask(String sku) {
            super(mDescriptionLoadingControl);
            mSku = sku;
            mSettingsSnapshot = new SettingsSnapshot(AbsProductActivity.this);
        }

        /**
         * Background processing.
         */
        @Override
        protected Boolean doInBackground(Void... ps) {

            try {
                ProductDetailsExistResult existResult;
                if (!isCancelled()) {
                    existResult = JobCacheManager.productDetailsExist(mSku,
                            mSettingsSnapshot.getUrl(),
                            true);
                } else {
                    CommonUtils
                            .debug(TAG, "ProductDescriptionLoaderTask.doInBackground: cancelled");
                    return false;
                }
                if (existResult.isExisting()) {
                    mProduct = JobCacheManager.restoreProductDetails(existResult.getSku(),
                            mSettingsSnapshot.getUrl());
                } else {
                    doneSignal = new CountDownLatch(1);
                    resHelper.registerLoadOperationObserver(this);
                    try {
                        final String[] params = new String[2];
                        params[0] = MageventoryConstants.GET_PRODUCT_BY_SKU_OR_BARCODE;
                        params[1] = mSku;

                        Bundle b = new Bundle();
                        b.putBoolean(
                                MageventoryConstants.EKEY_DONT_REPORT_PRODUCT_NOT_EXIST_EXCEPTION,
                                true);
                        requestId = resHelper.loadResource(MyApplication.getContext(),
                                MageventoryConstants.RES_PRODUCT_DETAILS, params, b,
                                mSettingsSnapshot);
                        while (true) {
                            if (isCancelled()) {
                                return false;
                            }
                            try {
                                if (doneSignal.await(1, TimeUnit.SECONDS)) {
                                    break;
                                }
                            } catch (InterruptedException e) {
                                CommonUtils
                                        .debug(TAG,
                                                "ProductDescriptionLoaderTask.doInBackground: cancelled (interrupted)");
                                return false;
                            }
                        }
                    } finally {
                        resHelper.unregisterLoadOperationObserver(this);
                    }
                    if (success) {
                        CommonUtils
                                .debug(TAG,
                                        "ProductDescriptionLoaderTask.doInBackground: success loading for sku: %1$s",
                                        mSku);
                        mProduct = JobCacheManager.restoreProductDetails(mSku,
                                mSettingsSnapshot.getUrl());
                    } else {
                        CommonUtils.debug(TAG,
                                "CacheLoaderTask.doInBackground: failed loading for sku: %1$s",
                                String.valueOf(mSku));
                    }
                }

                return !isCancelled();
            } catch (Exception ex) {
                GuiUtils.error(TAG, R.string.errorGeneral, ex);
            }
            return false;
        }

        @Override
        protected void onSuccessPostExecute() {
            if (isCancelled()) {
                return;
            }
            if (isActivityAlive()) {
                if (mProduct == null) {
                    if (doesntExist) {
                        GuiUtils.alert(R.string.product_not_found2, mSku);
                    } else {
                        GuiUtils.alert(R.string.errorGeneral);
                    }
                } else {
                    if (TextUtils.isEmpty(mProduct.getDescription())) {
                        GuiUtils.alert(R.string.product_doesnt_have_description);
                    } else {
                        descriptionV.setText(mProduct.getDescription());
                        onDescriptionUpdatedViaScan();
                    }
                }
            }
        }

        @Override
        public void onLoadOperationCompleted(LoadOperation op) {
            if (op.getOperationRequestId() == requestId) {
                success = op.getException() == null;
                ProductDetailsLoadException exception = (ProductDetailsLoadException) op
                        .getException();
                if (exception != null
                        && exception.getFaultCode() == ProductDetailsLoadException.ERROR_CODE_PRODUCT_DOESNT_EXIST) {
                    doesntExist = true;
                }
                if (success) {
                    Bundle extras = op.getExtras();
                    if (extras != null && extras.getString(MAGEKEY_PRODUCT_SKU) != null) {
                        mSku = extras.getString(MAGEKEY_PRODUCT_SKU);
                    } else {
                        CommonUtils.error(TAG, CommonUtils.format(
                                "API response didn't return SKU information for the sku: %1$s",
                                mSku));
                    }
                }
                doneSignal.countDown();
            }
        }
    }

    protected class ProductInfoLoader extends AsyncTask<String, Void, Boolean> {

        private String sku;

        private SettingsSnapshot mSettingsSnapshot;
        
        private boolean mCheckBarcode;

        public ProductInfoLoader(String sku, boolean checkBarcode)
        {
            this.sku = sku;
            this.mCheckBarcode = checkBarcode;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSettingsSnapshot = new SettingsSnapshot(AbsProductActivity.this);
            if (mCheckBarcode) {
                layoutBarcodeCheckPending.setVisibility(View.VISIBLE);
            } else {
                layoutSKUcheckPending.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Boolean doInBackground(String... args) {
            ProductDetailsExistResult existResult = JobCacheManager.productDetailsExist(sku,
                    mSettingsSnapshot.getUrl(), mCheckBarcode);
            if (existResult.isExisting()) {
                return Boolean.TRUE;
            } else {
                final String[] params = new String[2];
                params[0] = mCheckBarcode?GET_PRODUCT_BY_SKU_OR_BARCODE:GET_PRODUCT_BY_SKU; // ZERO --> Use Product ID , ONE -->
                // Use Product SKU
                params[1] = this.sku;
                int requestId = resHelper.loadResource(AbsProductActivity.this,
                        RES_PRODUCT_DETAILS, params, mSettingsSnapshot);
                if (mCheckBarcode) {
                    mBarcodeLoadRequestId = requestId;
                } else {
                    mSkuLoadRequestId = requestId;
                }
                return Boolean.FALSE;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // nullify backgroundProductInfoLoader activity field so it will be
            // possible to check whether the task completed or no
            backgroundProductInfoLoader = null;
            if (result.booleanValue() == true) {
                if (isActivityAlive) {
                    showKnownSkuOrBarcodeDialog(this.sku, mCheckBarcode);
                }
            }
        }

        /**
         * Whether the task instance had been run for the barcode or SKU check
         * 
         * @return true if task had been run for the barcode check, false if for
         *         the SKU check
         */
        public boolean isCheckBarcode() {
            return mCheckBarcode;
        }
    }

    public static class SpecialPricesData
    {
        public Date fromDate;
        public Date toDate;
    }

    /**
     * Control progress overlay visibility and loading messages list
     */
    static class ProductLoadingControl {

        enum ProgressData {
            ATTRIBUTE_SETS(R.string.loading_attr_sets), 
            ATTRIBUTES_LIST(R.string.loading_attrs_list),
            RECENT_WEB_ADDRESSES_LIST(R.string.loading_recent_web_addresses_list),
            ;
            private String mDescription;

            ProgressData(int resourceId) {
                this(CommonUtils.getStringResource(resourceId));
            }

            ProgressData(String description) {
                mDescription = description;
            }

            @Override
            public String toString() {
                return mDescription;
            }
        }

        public ProductLoadingControl(View view) {
            mView = view;
            mMessageView = (TextView) view.findViewById(R.id.progressMesage);
        }

        List<ProgressData> mLoaders = new ArrayList<ProgressData>();
        View mView;
        TextView mMessageView;

        public void startLoading(ProgressData data) {
            synchronized (mLoaders) {

                if (mLoaders.isEmpty()) {
                    setViewVisibile(true);
                }
                if (!mLoaders.contains(data)) {
                    mLoaders.add(data);
                }
                updateMessage();
            }
        }

        public void stopLoading(ProgressData data) {
            synchronized (mLoaders) {
                mLoaders.remove(data);
                if (mLoaders.isEmpty()) {
                    setViewVisibile(false);
                }
                updateMessage();
            }
        }

        protected void setViewVisibile(boolean visible) {
            try {
                mView.setVisibility(visible ? View.VISIBLE : View.GONE);
            } catch (Exception ex) {
                GuiUtils.noAlertError(TAG, ex);
            }
        }

        protected void updateMessage() {
            mMessageView.setText(TextUtils.join("\n", mLoaders));
        }
    }

    /**
     * Generic loading control for the book information loading operation.
     * Supports loading indication for the custom attributes and for the barcode
     * input field
     */
    class BookInfoLoadingControl implements LoadingControl {

        /**
         * Field to control whether the stopLoading method was already called
         */
        boolean mIsLoading = false;
        /**
         * The text box to display loading related information
         */
        TextView mHintView;
        /**
         * The loading view which visibility should be controlled during the
         * operation
         */
        View mLoadingView;
        /**
         * Whether to show no book found text information after the loading
         * operation is stopped and no information was found
         */
        boolean mShowNoBookFoundHint;

        /**
         * @param hintView
         * @param loadingView
         * @param showNoBookFoundHint
         */
        public BookInfoLoadingControl(TextView hintView, View loadingView,
                boolean showNoBookFoundHint) {
            mHintView = hintView;
            mLoadingView = loadingView;
            mShowNoBookFoundHint = showNoBookFoundHint;
        }

        @Override
        public void startLoading() {
            mIsLoading = true;
            mHintView.setTextColor(mDefaultTextColor);
            mHintView.setText(R.string.requesting_book_details);
            mHintView.setVisibility(View.VISIBLE);
            if (mLoadingView != null) {
                mLoadingView.setVisibility(View.VISIBLE);
            }

        }

        @Override
        public void stopLoading() {
            // do not process stopLoading event twice
            if (!mIsLoading) {
                return;
            }
            mIsLoading = false;
            // if the currently active book info loading controlled by this
            // loading control
            if (mBookInfoLoader.getLoadingControl() == this) {
                if (mLoadingView != null) {
                    mLoadingView.setVisibility(View.GONE);
                }
                if (!mShowNoBookFoundHint || mBookInfoLoader.isBookInfoFound()
                        || mBookInfoLoader.isCancelled()) {
                    mHintView.setVisibility(View.GONE);
                } else {
                    mHintView.setVisibility(View.VISIBLE);
                    mHintView.setTextColor(mErrorTextColor);
                    mHintView.setText(R.string.no_book_info_found2);
                }
            }
        }

        @Override
        public boolean isLoading() {
            return mIsLoading;
        }
    }

    /**
     * Custom attribute value changed listener for the product create/edit
     * activities. Used for operations such as live ISBN attribute on edit event
     * recognition and live book information loading
     */
    class CustomOnAttributeValueChangedListener implements OnAttributeValueChangedListener {

        @Override
        public void attributeValueChanged(String oldValue, String newValue,
                CustomAttribute attribute) {
            // if value changed
            if (!TextUtils.equals(oldValue, newValue)) {
                // if isbn 10 attribute modified
                if (TextUtils.equals(attribute.getCode(), BookInfoLoader.ISBN_10_ATTRIBUTE)) {
                    if (BookInfoLoader.isIsbn10Code(newValue)) {
                        // load the book information based on isbn 10 attribute
                        // value
                        loadBookInfo(newValue, attribute);
                    } else {
                        // if code format is invalid we need to stop previously
                        // run book info loading task in case it was run for the
                        // isbn 10 custom attribute
                        stopBookInfoLoadingIfSource(attribute);
                    }
                } else if (TextUtils.equals(attribute.getCode(), BookInfoLoader.ISBN_13_ATTRIBUTE)) {
                    // if isbn 13 attribute modified
                    if (BookInfoLoader.isIsbn13Code(newValue)) {
                        // load the book information based on isbn 13 attribute
                        // value
                        loadBookInfo(newValue, attribute);
                    } else {
                        // if code format is invalid we need to stop previously
                        // run book info loading task in case it was run for the
                        // isbn 13 custom attribute
                        stopBookInfoLoadingIfSource(attribute);
                    }
                }
            }
        }
    }

    /**
     * Default additional initializer for the custom attribute views. Used to
     * attach additional focus changed listener to the ISBN custom attributes
     * edit text boxes
     */
    class CustomAttributeViewAdditionalInitializer implements AttributeViewAdditionalInitializer {

        @Override
        public void processCustomAttribute(final CustomAttribute attribute) {
            // if attribute is isbn 10 or isbn 13 attribute
            if (TextUtils.equals(attribute.getCode(), BookInfoLoader.ISBN_10_ATTRIBUTE)
                    || TextUtils.equals(attribute.getCode(), BookInfoLoader.ISBN_13_ATTRIBUTE)) {
                processIsbnAttribute(attribute);
            } else if (TextUtils.equals(attribute.getCode(), BookInfoLoader.ISSN_ATTRIBUTE)) {
                // if attribute is issn attribute
                processIssnAttribute(attribute);
            }
        }

        /**
         * Process the ISSN attribute. Add additional focus listener which will
         * show hint on focus lost if the value will have invalid format
         * 
         * @param attribute
         */
        public void processIssnAttribute(final CustomAttribute attribute) {
            // keep reference to the original focus change listener so the
            // event may be passed to it in the custom focus listener
            final OnFocusChangeListener originalListener = attribute.getCorrespondingView()
                    .getOnFocusChangeListener();
            attribute.getCorrespondingView().setOnFocusChangeListener(
                    new OnFocusChangeListener() {

                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            // pass the event to the original focus listener
                            if (originalListener != null) {
                                originalListener.onFocusChange(v, hasFocus);
                            }
                            if (!hasFocus) {
                                String code = ((EditText) v).getText().toString();
                                boolean valid = true;
                                // check the code validness
                                if (!TextUtils.isEmpty(code)) {
                                    valid = BookInfoLoader.isIssnCode(code);

                                }
                                // if code is not valid ISSN show
                                // corresponding message in the hint view
                                TextView hintView = attribute.getHintView();
                                if (!valid) {
                                    hintView.setText(R.string.invalid_issn);
                                    hintView.setTextColor(mErrorTextColor);
                                    hintView.setVisibility(View.VISIBLE);
                                } else {
                                    hintView.setVisibility(View.GONE);
                                }
                            }
                        }
                    });
        }
        /**
         * Process the ISBN attribute. Add additional focus listener which will
         * show hint on focus lost if the value will have invalid format
         * 
         * @param attribute
         */
        public void processIsbnAttribute(final CustomAttribute attribute) {
            // keep reference to the original focus change listener so the
            // event may be passed to it in the custom focus listener
            final OnFocusChangeListener originalListener = attribute.getCorrespondingView()
                    .getOnFocusChangeListener();
            attribute.getCorrespondingView().setOnFocusChangeListener(
                    new OnFocusChangeListener() {

                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            // pass the event to the original focus listener
                            if (originalListener != null) {
                                originalListener.onFocusChange(v, hasFocus);
                            }
                            if (!hasFocus) {
                                String code = ((EditText) v).getText().toString();
                                boolean valid = true;
                                // check the code validness
                                if (!TextUtils.isEmpty(code)) {
                                    if (TextUtils.equals(attribute.getCode(),
                                            BookInfoLoader.ISBN_10_ATTRIBUTE)) {
                                        valid = BookInfoLoader.isIsbn10Code(code);
                                    } else {
                                        valid = BookInfoLoader.isIsbn13Code(code);
                                    }

                                }
                                // if code is not valid ISBN show
                                // corresponding message in the hint view
                                if (!valid) {
                                    TextView hintView = attribute.getHintView();
                                    hintView.setText(R.string.invalid_isbn);
                                    hintView.setTextColor(mErrorTextColor);
                                    hintView.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    });
        }

    }

    /**
     * The list adapter to reprecent {@link RecentWebAddress}es information
     */
    class RecentWebAddressesAdapter extends BaseAdapter {

        /**
         * Adapter data
         */
        List<RecentWebAddress> mRecentWebAddresses;
        LayoutInflater mInflater;

        RecentWebAddressesAdapter(List<RecentWebAddress> recentWebAddresses) {
            mRecentWebAddresses = recentWebAddresses;
            mInflater = LayoutInflater.from(AbsProductActivity.this);
        }

        @Override
        public int getCount() {
            return mRecentWebAddresses.size();
        }

        @Override
        public RecentWebAddress getItem(int position) {
            return mRecentWebAddresses.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            TextView text;

            if (convertView == null) {
                view = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            } else {
                view = convertView;
            }

            text = (TextView) view;
            RecentWebAddress item = getItem(position);
            // set the view text in the format
            // "domain (access count) - date in (dd/MM/yyyy hh:mm:ss)"
            text.setText(CommonUtils.format("%1$s (%2$d) - %3$td/%3$tm/%3$ty %3$tH:%3$tM:%3$tS",
                    item.getDomain(), item.getAccessCount(), item.getLastUsed()));

            return view;
        }
    }

    /**
     * Asynchronous task to load all {@link RecentWebAddress}es information from
     * the database.
     */
    class LoadRecentWebAddressesTask extends SimpleAsyncTask {

        /**
         * Reference to the loaded recent web addresses information
         */
        List<RecentWebAddress> mRecentWebAddresses;

        public LoadRecentWebAddressesTask() {
            super(null);
        }


        @Override
        public void startLoading() {
            super.startLoading();
            mProductLoadingControl.startLoading(ProgressData.RECENT_WEB_ADDRESSES_LIST);
        }

        @Override
        public void stopLoading() {
            super.stopLoading();
            mProductLoadingControl.stopLoading(ProgressData.RECENT_WEB_ADDRESSES_LIST);
        }

        @Override
        protected void onSuccessPostExecute() {
            if (mRecentWebAddresses.isEmpty()) {
                GuiUtils.alert(R.string.no_recent_web_addresses);
            } else {
                showRecentWebAddressesDialog(mRecentWebAddresses);
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // load recent web addresses information sorted by access date
                // descending
                mRecentWebAddresses = RecentWebAddressProviderAccessor.getInstance()
                        .getAllRecentWebAddresses(RecentWebAddresses.LAST_USED_DESC_SORT_ORDER,
                                mSettings.getUrl());
                return !isCancelled();
            } catch (Exception ex) {
                GuiUtils.error(TAG, R.string.errorGeneral, ex);
            }
            return false;
        }

    }
}
