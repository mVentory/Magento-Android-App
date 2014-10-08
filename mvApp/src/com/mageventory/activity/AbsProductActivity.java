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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

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
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.activity.ScanActivity.CheckSkuResult;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCacheManager.ProductDetailsExistResult;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttribute.ContainerMarkers;
import com.mageventory.model.CustomAttribute.ContentType;
import com.mageventory.model.CustomAttribute.CustomAttributeOption;
import com.mageventory.model.CustomAttribute.InputMethod;
import com.mageventory.model.CustomAttributeSimple;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.model.CustomAttributesList.AttributeViewAdditionalInitializer;
import com.mageventory.model.CustomAttributesList.CustomAttributeViewUtils;
import com.mageventory.model.CustomAttributesList.OnAttributeValueChangedListener;
import com.mageventory.model.CustomAttributesList.OnNewOptionTaskEventListener;
import com.mageventory.model.Product;
import com.mageventory.model.util.AbstractCustomAttributeViewUtils;
import com.mageventory.model.util.AbstractCustomAttributeViewUtils.CommonOnNewOptionTaskEventListener;
import com.mageventory.model.util.AbstractCustomAttributeViewUtils.OnEditDoneAction;
import com.mageventory.model.util.ProductUtils;
import com.mageventory.model.util.ProductUtils.PriceInputFieldHandler;
import com.mageventory.recent_web_address.RecentWebAddress;
import com.mageventory.recent_web_address.util.AbstractRecentWebAddressesSearchPopupHandler;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.AbstractLoadProductTask;
import com.mageventory.tasks.BookInfoLoader;
import com.mageventory.tasks.LoadAttributeSets;
import com.mageventory.tasks.LoadAttributesList;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.DialogUtil;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.util.EventBusUtils.GeneralBroadcastEventHandler;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.InputCacheUtils;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.ScanUtils;
import com.mageventory.util.ScanUtils.ScanResult;
import com.mageventory.util.SimpleViewLoadingControl;
import com.mageventory.util.concurent.SerialExecutor;
import com.mageventory.util.loading.GenericMultilineViewLoadingControl;
import com.mageventory.util.loading.GenericMultilineViewLoadingControl.ProgressData;
import com.reactor.gesture_input.GestureInputActivity;

@SuppressLint("NewApi")
public abstract class AbsProductActivity extends BaseFragmentActivity implements
        MageventoryConstants, OperationObserver, GeneralBroadcastEventHandler, OnEditDoneAction {

    public static final String TAG = AbsProductActivity.class.getSimpleName();

    /**
     * The request code used to launch scan activity for scanning text for
     * attribute values functionality
     */
    public static final int SCAN_ATTRIBUTE_TEXT = 100;
    /**
     * The request code used to launch scan activity for scanning of the product
     * barcode/SKU for the copy attribute value functionality
     */
    public static final int SCAN_ANOTHER_PRODUCT_CODE = 101;

    /**
     * The key for the predefined attribute values intent extra
     */
    public static final String EXTRA_PREDEFINED_ATTRIBUTES = "PREDEFINED_ATTRIBUTES";
    /**
     * The key for the SKU the product should be linked with intent extra
     */
    public static final String EXTRA_LINK_WITH_SKU = "LINK_WITH_SKU";
    /**
     * The key for the whether the product is allowed to be edited before saving
     * option intent extra
     */
    public static final String EXTRA_ALLOW_TO_EDIT = "ALLOW_TO_EDIT";

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
    /**
     * The view to show name attribute format information
     */
    protected TextView attrFormatterStringV;
    protected LayoutInflater inflater;
    protected LinearLayout container;
    protected View atrListWrapperV;
    public ViewGroup atrListV;
    protected EditText attributeSetV;
    protected TextView atrSetLabelV;
    private int mDefaultAttrSetLabelVColor;
    protected TextView atrListLabelV;
    /**
     * Generic loading control which shows progress information in the overlay
     * on top of activity view
     */
    protected GenericMultilineViewLoadingControl mOverlayLoadingControl;
    /**
     * The loading control for the new option creation operation
     */
    protected LoadingControl newOptionPendingLoadingControl;
    protected LinearLayout layoutSKUcheckPending;
    protected LinearLayout layoutBarcodeCheckPending;
    /**
     * The container for the SKU attribute view
     */
    private ViewGroup mSkuContainer;
    /**
     * The container for the Barcode attribute view
     */
    private ViewGroup mBarcodeContainer;
    /**
     * The container for the weight attribute view 
     */
    private ViewGroup mWeightContainer;
    /**
     * The container for the name attribute view
     */
    private ViewGroup mNameContainer;
    /**
     * The container for the short description attribute view
     */
    private ViewGroup mShortDescriptionContainer;
    /**
     * The container for the description attribute view
     */
    private ViewGroup mDescriptionContainer;
    public EditText priceV;
    /**
     * The handler for the priceV field which contains various useful methods
     * for the price management including opening of the price edit dialog
     */
    public PriceInputFieldHandler priceHandler;
    /**
     * The quantity input field
     */
    public EditText quantityV;
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

    ClipboardManager mClipboard;

    /**
     * Instance of the last created asynchronous task for copying of the
     * attribute value from another product. Field keeps reference so it may
     * be cancelled when new "Copy From" operation is requested
     */
    ProductAttributeValueLoaderTask mProductAttributeValueLoaderTask;
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
     * Optional predefined values for the product custom attributes including
     * system attributes such as name, sku, barcode, weight and so forth. This
     * is passed as {@link AbsProductActivity#EXTRA_PREDEFINED_ATTRIBUTES}
     * intent extra to override custom attribute default values or values loaded
     * from the product
     */
    List<CustomAttributeSimple> predefinedCustomAttributeValues;
    /**
     * Optional SKU of the product the editing/creating product should be linked
     * with. This is passed as {@link #EXTRA_LINK_WITH_SKU} intent extra
     */
    public String skuToLinkWith;
    /**
     * Whether the editing of product details allowed before saving operation or
     * saving should be performed right after the product loaded. This is
     * passed as {@link #EXTRA_ALLOW_TO_EDIT} intent extra.
     */
    public boolean allowToEdit;

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
     * Handler for the recent web addresses search functionality
     */
    RecentWebAddressesSearchPopupHandler mRecentWebAddressesSearchPopupHandler;
    
    /**
     * The flag indicating that barcode text changes should be ignored in the
     * barcodeInput text watcher. Used to do not run unnecessary barcode checks
     * when setting barcode input value programmatically
     */
    private boolean mIgnoreBarcodeTextChanges = false;

    /**
     * Reference to the last used custom attribute for various operations such
     * as "scan free text", "copy attribute value from product". Last used
     * attribute information is used for operations callback where it can't be
     * passed directly to the operation but should be checked later in context
     * of request. Example: running external scan activity -
     * mLastUsedCustomAttribute is checked in the onActivityResult
     */
    private CustomAttribute mLastUsedCustomAttribute;

    /**
     * Map storing validation failed attributes when user pressed save product
     * button. This is needed to preserve context so once user will enter all
     * the attribute values we can scroll the window back to the save button
     */
    private Map<String, CustomAttribute> mValidationFailedAttributes = new LinkedHashMap<String, CustomAttribute>();


    // lifecycle

    protected void absOnCreate() {
        setContentView(R.layout.product_create_edit);

        mSettings = new Settings(this);
        mClipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        // extract and remember activity starting options from the intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            predefinedCustomAttributeValues = extras
                    .getParcelableArrayList(EXTRA_PREDEFINED_ATTRIBUTES);
            skuToLinkWith = extras.getString(EXTRA_LINK_WITH_SKU);
            allowToEdit = extras.getBoolean(EXTRA_ALLOW_TO_EDIT, true);
        } else {
            allowToEdit = true;
        }

        // find views
        container = (LinearLayout) findViewById(R.id.container);
        mSkuContainer = (ViewGroup) findViewById(R.id.skuContainer);
        mBarcodeContainer = (ViewGroup) findViewById(R.id.barcodeContainer);
        mWeightContainer = (ViewGroup) findViewById(R.id.weightContainer);
        mNameContainer = (ViewGroup) findViewById(R.id.nameContainer);
        mShortDescriptionContainer = (ViewGroup) findViewById(R.id.shortDescriptionContainer);
        mDescriptionContainer = (ViewGroup) findViewById(R.id.descriptionContainer);
        priceV = (EditText) findViewById(R.id.price);
        priceHandler = new PriceInputFieldHandler(priceV, AbsProductActivity.this) {
            @Override
            protected void onPriceEditDone(Double price, Double specialPrice, Date fromDate,
                    Date toDate) {
                super.onPriceEditDone(price, specialPrice, fromDate, toDate);
                // special behavior when price is edited via price editing
                // dialog. Used to show update confirmation dialog in the product edit
                AbsProductActivity.this.onPriceEditDone();
            }
        };

        mErrorTextColor = getResources().getColor(R.color.red);
        quantityV = (EditText) findViewById(R.id.quantity);
        mDefaultTextColor = quantityV.getCurrentTextColor();

        attrFormatterStringV = (TextView) findViewById(R.id.attr_formatter_string);
        atrListWrapperV = findViewById(R.id.attr_list_wrapper);
        attributeSetV = (EditText) findViewById(R.id.attr_set);
        atrListV = (ViewGroup) findViewById(R.id.attr_list);
        // attributeSetV = (EditText) findViewById(R.id.attr_set);
        atrListLabelV = (TextView) findViewById(R.id.attr_list_label);
        atrSetLabelV = (TextView) findViewById(R.id.atr_set_label);
        mDefaultAttrSetLabelVColor = atrSetLabelV.getCurrentTextColor();
        mOverlayLoadingControl = new GenericMultilineViewLoadingControl(
                findViewById(R.id.progressStatus));
        mRecentWebAddressesSearchPopupHandler = new RecentWebAddressesSearchPopupHandler();

        newOptionPendingLoadingControl = new SimpleViewLoadingControl(
                findViewById(R.id.layoutNewOptionPending));
        layoutSKUcheckPending = (LinearLayout) findViewById(R.id.layoutSKUcheckPending);
        layoutBarcodeCheckPending = (LinearLayout) findViewById(R.id.layoutBarcodeCheckPending);

        inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        newOptionListener = new CommonOnNewOptionTaskEventListener(newOptionPendingLoadingControl,
                AbsProductActivity.this);



        if (this instanceof ProductEditActivity)
        {
            customAttributesList = new CustomAttributesList(this, atrListV,
                    newOptionListener, mOnAttributeValueChangedByUserInputListener,
                    mAttributeViewAdditionalInitializer, true);
        }
        else
        {
            customAttributesList = new CustomAttributesList(this, atrListV,
                    newOptionListener, mOnAttributeValueChangedByUserInputListener,
                    mAttributeViewAdditionalInitializer, false);
        }

        attributeSetV.setInputType(0);


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
        EventBusUtils.registerOnGeneralEventBroadcastReceiver(TAG, this, this);
    }

    /**
     * This method is called by {@link #priceHandler} when user presses
     * OK button in the price edit dialog.
     */
    protected void onPriceEditDone() {
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
        if (requestCode == SCAN_ATTRIBUTE_TEXT) {
            if (resultCode == RESULT_OK) {
                String contents = ScanUtils.getSanitizedScanResult(intent);
                if (contents != null) {
                    mLastUsedCustomAttribute.setSelectedValue(contents, true);
                    onAttributeUpdatedViaScan();
                }
            }
        } else if (requestCode == SCAN_ANOTHER_PRODUCT_CODE) {
            if (resultCode == RESULT_OK) {
                CheckSkuResult checkSkuResult = ScanActivity.checkSku(intent);
                if (checkSkuResult != null) {
                    if (mProductAttributeValueLoaderTask != null) {
                    	// cancel previously running attribute value loading task
                        mProductAttributeValueLoaderTask.cancel(true);
                    }
                    mProductAttributeValueLoaderTask = new ProductAttributeValueLoaderTask(checkSkuResult.code,
                            mLastUsedCustomAttribute);
                    mProductAttributeValueLoaderTask.execute();
                }
            }
        } else if (requestCode == LAUNCH_GESTURE_INPUT) {
            if (resultCode == RESULT_OK) {

                Bundle extras = (Bundle) intent.getExtras();
                if (extras != null) {
                    String out = extras.getString("OUTPUT_TEXT_KEY");
                    mLastUsedCustomAttribute.getCorrespondingEditTextView().setText(out);
                    onGestureInputSuccess();
                }
            }
        }
    }

    // methods

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
        // get the scan result with metadata
        ScanResult scanResult = ScanUtils.getFullSanitizedScanResult(intent);
        String code = scanResult.getCode();

        if (!TextUtils.isEmpty(code)) {

            // check whether SKU is of the right format. If it is not assume it
            // is a Barcode
            CheckSkuResult checkResult = ScanActivity.checkSku(code);


            if (checkResult.isBarcode && isSpecialAttributeAvailable(MAGEKEY_PRODUCT_BARCODE))
            {
                // if Barcode scanned and Barcode attribute is available
            	
                setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_SKU, ProductUtils.generateSku(),
                        true);
                return barcodeScanCommon(scanResult, requestCode, false, true);
            }
            else
            {
                mGalleryTimestamp = 0;

                setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_SKU, checkResult.code, true);

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
        if (!ScanActivity.isLabelValid(this, code))
        {
            Settings settings = new Settings(this);
            String settingsUrl = settings.getUrl();

            if (!ScanActivity.domainPairRemembered(ScanActivity.getDomainNameFromUrl(settingsUrl),
                    ScanActivity.getDomainNameFromUrl(code)))
            {
                showInvalidLabelDialog(ScanActivity.getDomainNameFromUrl(settingsUrl),
                        ScanActivity.getDomainNameFromUrl(code));
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
        barcodeScanCommon(ScanUtils.getFullSanitizedScanResult(data), requestCode, true, false);
    }

    /**
     * Common behavior for the Create/Edit activities when the barcode scanned
     * 
     * @param scanResult scan information with the scanned code and metadata to
     *            check
     * @param requestCode request code the activity result is received for
     * @param activateWeightField whether to activate weight field after the
     *            barcode check
     * @param activatePriceField whether to activate price field after the
     *            barcode check
     * @return
     */
    protected boolean barcodeScanCommon(final ScanResult scanResult, final int requestCode,
            final boolean activateWeightField, final boolean activatePriceField) {
        mGalleryTimestamp = JobCacheManager.getGalleryTimestampNow();

        final Runnable barcodeScannedRunnable = new Runnable() {

            @Override
            public void run() {
                // initial code value which contains scanned code and metadata
                // in case there are no SECONDARY_BARCODE attributes to store
                // the metadata extension
                String code = scanResult.getCodeWithExtension();
                if (!TextUtils.isEmpty(scanResult.getExtension())) {
                    // if the scan result has metadata extension
                    if (customAttributesList != null && customAttributesList.getList() != null) {
                        // iterate through custom attributes and search for the
                        // custom attributes with the SECONDARY_BARCODE content
                        // type to set the metadata to
                        for (CustomAttribute customAttribute : customAttributesList.getList()) {
                            if (customAttribute.hasContentType(ContentType.SECONDARY_BARCODE)) {
                                // if custom attribute is of SECONDARY_BARCODE
                                // content type
                            	
                                // update custom attribute value from the scan
                                // result metadata extension information
                                customAttribute.setSelectedValue(scanResult.getExtension(), true);
                                // update the code value with the pure code only
                                // without a metadata, such as now metadata
                                // information will not be lost
                                code = scanResult.getCode();
                            }
                        }
                    }
                }
                // Set Barcode in Product Barcode TextBox
                setBarcodeInputTextIgnoreChanges(code);

                onBarcodeChanged(code);

                if (activateWeightField) {
                    GuiUtils.activateField(getSpecialAttributeEditTextView(MAGEKEY_PRODUCT_WEIGHT), true, false, true);
                }
                if (activatePriceField) {
                    GuiUtils.activateField(priceV, true, true, true);
                }
            }
        };
        return showMissingMetadataDialogIfNecessary(scanResult, barcodeScannedRunnable,
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
     * extra data but that metadata was not scanned (missing). For a now such
     * check is performed for ISSN codes only
     * 
     * @param scanResult scan information with the scanned code and metadata to
     *            check
     * @param barcodeScannedRunnable the runnable which should be run if check
     *            is successful or user decided to ignore the invalid check
     *            result
     * @param rescanRunnable the runnable which should be run if user presses
     *            rescan button
     * @param activity the activity which should to hold question dialog
     * @param settings instance of settings
     * @return true if the missing metadata dialog was shown
     */
    public static boolean showMissingMetadataDialogIfNecessary(ScanResult scanResult,
            Runnable barcodeScannedRunnable, Runnable rescanRunnable, Activity activity,
            final Settings settings) {
        boolean result = false;
        if (BookInfoLoader.isIssnCode(scanResult.getCode())
                && TextUtils.isEmpty(scanResult.getExtension())
                && settings.isIssnMissingMetadataRescanRequestEnabled()) {
            // if scanned code is of ISSN format and missing meta data dialog is
            // not completely disabled and metadata is not present in the scan
            // result
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
     * Set the Barcode attribute value and update corresponding view but specify
     * mIgnoreBarcodeTextChanges flag during operation so related TextWatcher
     * will ignore such changes
     * 
     * @param text the text to set
     */
    protected void setBarcodeInputTextIgnoreChanges(String text) {
        mIgnoreBarcodeTextChanges = true;
        try {
            setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_BARCODE, text, true);
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

    /**
     * This method is called when the attribute value was updated via scan free
     * text operation. It may be overridden to run custom actions on such event
     */
    protected void onAttributeUpdatedViaScan() {
    }
    
    protected void onGestureInputSuccess() {
    }
    
    public static String getProductName(AbsProductActivity apa) {
        String name = apa.getSpecialAttributeValue(MAGEKEY_PRODUCT_NAME);

        // check there are any other character than spaces
        if (name != null && name.trim().length() > 0) {
            return name;
        }

        return apa.customAttributesList.getCompoundName();
    }

    /**
     * Check whether the product name equals to the generated from custom
     * attributes using formatting string name and update nameV content depend
     * on the condition.
     * 
     * @param productName the name of the product to check and set to the nameV
     *            field
     */
    public void determineWhetherNameIsGeneratedAndSetProductName(String productName) {
        boolean generatedName = productName == null
                || productName.equals(customAttributesList.getCompoundName());
        // if generated name equals to product name then set null to the nameV.
        // Hint will be used to display product name in such case. Otherwise set
        // the produt name to the corresponding view
        setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_NAME, generatedName ? null : productName,
                true);
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
                                    AbsProductActivity.this, atrListV, newOptionListener,
                                    mOnAttributeValueChangedByUserInputListener,
                                    mAttributeViewAdditionalInitializer, true);
                        }
                        else
                        {
                            customAttributesList = new CustomAttributesList(
                                    AbsProductActivity.this, atrListV, newOptionListener,
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
            customAttributesList = CustomAttributesList.loadFromCache(this, atrListV,
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
            mOverlayLoadingControl.startLoading(ProgressData.ATTRIBUTES_LIST);
        } else {
            mOverlayLoadingControl.stopLoading(ProgressData.ATTRIBUTES_LIST);
        }
    }

    /**
     * Additional initialization for the special attributes which are excluded
     * from the custom attribute list but requires some processing
     */
    protected void initSpecialAttributes() {
        // Containers for the special attributes
        ViewGroup[] containers = new ViewGroup[]{
                mSkuContainer,
                mBarcodeContainer,
                mWeightContainer,
                mNameContainer,
                mShortDescriptionContainer,
                mDescriptionContainer
        };
        // Special attribute codes to process
        String[] attributeCodes = new String[] {
                MAGEKEY_PRODUCT_SKU, 
                MAGEKEY_PRODUCT_BARCODE, 
                MAGEKEY_PRODUCT_WEIGHT,
                MAGEKEY_PRODUCT_NAME, 
                MAGEKEY_PRODUCT_SHORT_DESCRIPTION,
                MAGEKEY_PRODUCT_DESCRIPTION,
        };
        CustomAttributeViewUtils customAttributeViewUtils = customAttributesList
                .getCustomAttributViewUtils();
        for (int i = 0; i < containers.length; i++) {
            // get the container attributeCode pair
            ViewGroup container = containers[i];
            String attributeCode = attributeCodes[i];
            // clear the container if it has previously added views
            container.removeAllViews();
            // check whether special attribute properties loaded and create view
            // for it with additional initialization
            CustomAttribute attribute = getSpecialAttribute(attributeCode);
            if (attribute != null) {
                View v = customAttributesList.newAtrEditView(attribute, customAttributeViewUtils);
                mAttributeViewAdditionalInitializer.processCustomAttribute(attribute);
                container.addView(v);
            }
        }
        // regenerate name hint such as name attribute view was just initialized
        customAttributesList.setNameHint();

        // monitor price and quantity changes
        priceV.setOnEditorActionListener(customAttributeViewUtils
                .getAttributeOnEditorActionListener(MAGEKEY_PRODUCT_PRICE));
        CustomAttribute priceAttribute = getSpecialAttribute(MAGEKEY_PRODUCT_PRICE);
        if (priceAttribute != null) {
            priceAttribute.setCorrespondingView(priceV);
            // unmark price view container if it was previously marked
            priceAttribute.unmarkAttributeContainer();
        }
        
        quantityV.setOnEditorActionListener(customAttributeViewUtils
                .getAttributeOnEditorActionListener(MAGEKEY_PRODUCT_QUANTITY));
        CustomAttribute quantityAttribute = getSpecialAttribute(MAGEKEY_PRODUCT_QUANTITY);
        if (quantityAttribute != null) {
            quantityAttribute.setCorrespondingView(quantityV);
            // unmark quantity view container if it was previously marked
            quantityAttribute.unmarkAttributeContainer();
        }
    }

    /**
     * Set the special attribute value for the attributeCode by copying value
     * from the product
     * 
     * @param attributeCode the special attribute code
     * @param product the product to copy value from
     */
    protected void setSpecialAttributeValueFromProduct(String attributeCode, Product product) {
        setSpecialAttributeValueIfNotNull(attributeCode,
                product.getStringAttributeValue(attributeCode), true);
    }

    /**
     * Call the performClick() for the attributeCode special attribute
     * corresponding view if exists
     * 
     * @param attributeCode the special attribute code
     */
    protected void performClickOnSpecialAttribute(String attributeCode) {
        EditText view = getSpecialAttributeEditTextView(attributeCode);
        if (view != null) {
            // if such view exists
            view.performClick();
        }
    }

    /**
     * Get the {@link EditText} view for the special attribute if exists
     * 
     * @param attributeCode the special attribute code
     * @return {@link EditText} if special attribute exists and loaded,
     *         otherwise returns null
     */
    protected EditText getSpecialAttributeEditTextView(String attributeCode) {
        CustomAttribute attribute = getSpecialAttribute(attributeCode);
        return attribute == null ? null : attribute.getCorrespondingEditTextView();
    }

    /**
     * Get the selected value for the special attribute if exists
     * 
     * @param attributeCode the special attribute code
     * @return special attribute value if attribute exists and loaded, otherwise
     *         returns null
     */
    public String getSpecialAttributeValue(String attributeCode) {
        CustomAttribute attribute = getSpecialAttribute(attributeCode);
        String result = null;
        if (attribute != null) {
            // if special attribute exists
            result = attribute.getSelectedValue();
        }
        return result;
    }

    /**
     * Set the special attribute value if attribute exists
     * 
     * @param attributeCode the special attribute code
     * @param value the new value for the special attribute
     */
    public void setSpecialAttributeValueIfNotNull(String attributeCode, String value) {
        setSpecialAttributeValueIfNotNull(attributeCode, value, true);
    }

    /**
     * Set the special attribute value if attribute exists
     * 
     * @param attributeCode the special attribute code
     * @param value the new value for the special attribute
     * @param updateView whether the corresponding view of the special custom
     *            attribute should be updated with the new value
     */
    protected void setSpecialAttributeValueIfNotNull(String attributeCode, String value,
            boolean updateView) {
        CustomAttribute attribute = getSpecialAttribute(attributeCode);
        if (attribute != null) {
            // if special attribute exists
            attribute.setSelectedValue(value, updateView);
        }
    }

    /**
     * Check whether the special attribute exists and loaded
     * 
     * @param attributeCode the special attribute code
     * @return true if attribute exists, otherwise returns null
     */
    public boolean isSpecialAttributeAvailable(String attributeCode) {
        return getSpecialAttribute(attributeCode) != null;
    }

    /**
     * Get the special custom attribute if exists
     * 
     * @param attributeCode the special attribute code
     * @return {@link CustomAttribute} for the attributeCode if
     *         customAttributesList is loaded and special attribute exists in
     *         the attribute set, otherwise returns null
     */
    protected CustomAttribute getSpecialAttribute(String attributeCode) {
        CustomAttribute attribute = customAttributesList == null ? null : customAttributesList
                .getSpecialCustomAttribute(attributeCode);
        return attribute;
    }
    /*
     * Called when user creates/updates a product. This function stores all new
     * attribute values in the cache.
     */
    public void updateInputCacheWithCurrentValues()
    {
        if (customAttributesList != null) {
            updateInputCacheWithCurrentValues(customAttributesList.getList());
            updateInputCacheWithCurrentValues(customAttributesList.getSpecialCustomAttributes()
                    .values());
        }

        JobCacheManager.storeInputCache(inputCache, mSettings.getUrl());
    }

    /**
     * Update input cache for the customAttributes collection
     * 
     * @param customAttributes collection of custom attributes to process
     */
    protected void updateInputCacheWithCurrentValues(Collection<CustomAttribute> customAttributes) {
        if (customAttributes == null) {
            return;
        }
        for (CustomAttribute customAttribute : customAttributes)
        {
            if ((customAttribute.isOfType(CustomAttribute.TYPE_TEXT) 
            		|| customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA))
                    && customAttribute.getCorrespondingView() != null) {
                // if attribute is of type TEXT or TEXT_AREA and has valid
                // corresponding view specified
                InputCacheUtils.addValueToInputCacheList(customAttribute.getCode(),
                        ((EditText) customAttribute.getCorrespondingView()).getText().toString(),
                        inputCache);
            }
        }
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
        }
    }

    public void onAttributeSetLoadStart() {
        atrSetLabelV.setTextColor(getResources().getColor(R.color.attr_set_label_color_loading));
        mOverlayLoadingControl.startLoading(ProgressData.ATTRIBUTE_SETS);
        attributeSetV.setClickable(false);
        attributeSetV.setHint("Loading product types...");
    }

    public void onAttributeSetLoadFailure() {
        atrSetLabelV.setTextColor(getResources().getColor(R.color.attr_set_label_color_error));
        mOverlayLoadingControl.stopLoading(ProgressData.ATTRIBUTE_SETS);
        attributeSetV.setClickable(true);
        attributeSetV.setHint("Load failed... Check settings and refresh");
    }

    public void onAttributeSetLoadSuccess() {
        atrSetLabelV.setTextColor(mDefaultAttrSetLabelVColor);
        if (mRefreshPressed) {
            loadAttributeList(false);
            mRefreshPressed = false;
        }
        mOverlayLoadingControl.stopLoading(ProgressData.ATTRIBUTE_SETS);
        attributeSetV.setClickable(true);
        attributeSetV.setHint("Click to select an attribute set...");
    }

    public void onAttributeListLoadSuccess() {
        mValidationFailedAttributes.clear();
        atrListLabelV.setTextColor(mDefaultAttrSetLabelVColor);
        List<Map<String, Object>> atrList = getAttributeList();

        if (atrList != null) {
            customAttributesList.loadFromAttributeList(atrList, atrSetId);

            showAttributeListV(false);

            initSpecialAttributes();
        }

        if (atrList == null || atrList.size() == 0) {
            atrListWrapperV.setVisibility(View.GONE);
        }
    }

    public void onAttributeListLoadFailure() {
        atrListLabelV.setTextColor(getResources().getColor(R.color.attr_set_label_color_error));
        mOverlayLoadingControl.stopLoading(ProgressData.ATTRIBUTES_LIST);
    }

    public void onAttributeListLoadStart() {
        // clean the list
        atrListLabelV.setTextColor(mDefaultAttrSetLabelVColor);
        removeAttributeListV();
        showAttributeListV(true);
    }

    /**
     * Iterate through all custom attribute options and check on their
     * occurrence in the product name. Preselect custom attribute values in case
     * matches found
     * 
     * @return set of codes of the custom attributes which was updated during
     *         method invocation
     */
    public Set<String> selectAttributeValuesFromProductName() {
        // the set which stores updated custom attributes codes
        Set<String> attributesSelectedFromName = new HashSet<String>();
        EditText nameV = getSpecialAttributeEditTextView(MAGEKEY_PRODUCT_NAME);
        if (nameV == null) {
            // where are no name special attribute, interrupt method execution
            return attributesSelectedFromName;
        }
        // get the specified product name
        String name = nameV.getText().toString();
        if (TextUtils.isEmpty(name)) {
            name = nameV.getHint().toString();
        }
        name = name.toLowerCase();

        // if name is empty no need to perform any checks, nothing will match
        if (TextUtils.isEmpty(name)) {
            return attributesSelectedFromName;
        }
        // the set to store duplicate custom attribute option labels
        Set<String> duplicateOptions = new HashSet<String>();
        // the map which stores relations between custom attribute option labels
        // and custom attributes with the related custom attribute option
        Map<String, CustomAttributeValueHolder> valueCustomAttributeMap = new HashMap<String, CustomAttributeValueHolder>();
        // the map to store matched items in the product name from the
        // valueCustomAttributeMap
        Map<String, CustomAttributeValueHolder> selectedValueCustomAttributeMap = new HashMap<String, CustomAttributeValueHolder>();
        // iterate through custom attributes list and fill
        // valueCustomAttributeMap and duplicateOptions collections
        for (CustomAttribute elem : customAttributesList.getList()) {
            for (CustomAttributeOption option : elem.getOptions()) {
                // get the normalized option label for easier comparison
                String label = option.getLabel().toLowerCase().trim();
                // if label already occurred before
                if (valueCustomAttributeMap.containsKey(label)) {
                    duplicateOptions.add(label);
                } else {
                    // store the label custom attribute option and custom
                    // attribute relation
                    valueCustomAttributeMap
                            .put(label, new CustomAttributeValueHolder(option, elem));
                }
            }
        }
        // iterate through valueCustomAttributeMap and check for the attribute
        // options labels occurrence in the product name
        for (String option : valueCustomAttributeMap.keySet()) {
            // if option label occurs in the product name
            if (name.contains(option)) {
                // whether the option should be skipped flag, handled further in
                // the code
                boolean skip = false;
                // iterate through selectedValueCustomAttributeMap and check
                // whether the option contains or contained in some selected
                // before value
                for (Iterator<Map.Entry<String, CustomAttributeValueHolder>> iter = selectedValueCustomAttributeMap
                        .entrySet().iterator(); iter.hasNext();) {
                    Map.Entry<String, CustomAttributeValueHolder> entry = iter.next();
                    String selectedOption = entry.getKey();
                    // is the selected before option contains current option
                    if (selectedOption.contains(option)) {
                        // mark option to be skipped such as there is a selected
                        // option with the larger text which fully contains the
                        // option. Example selected before option label 'XXL'
                        // includes current option 'XL' text
                        skip = true;
                        break;
                    } else if (option.contains(selectedOption)) {
                        // if currently processing option contains some selected
                        // before option remove that selected before option.
                        // Example selected before option 'XL' and current
                        // option is 'XXL'. 'XXL' text fully contains 'XL' text
                        // and if 'XXL' occurs in the product name then remove
                        // 'XL' from selected options
                        iter.remove();
                    }
                }
                // if option is not marked to be skipped then include it to the
                // selectedValueCustomAttributeMap
                if (!skip) {
                    selectedValueCustomAttributeMap
                            .put(option, valueCustomAttributeMap.get(option));
                }
            }
        }
        // the map containing custom attribute - selected custom attribute
        // values relations
        Map<CustomAttribute, List<String>> attributeSelectedValues = new HashMap<CustomAttribute, List<String>>();
        // iterate through selectedValueCustomAttributeMap and build custom
        // attribute - selected custom attribute values relation
        for (CustomAttributeValueHolder holder : selectedValueCustomAttributeMap.values()) {
            // get the normalized option label for easier comparison
            String label = holder.option.getLabel().toLowerCase().trim();
            // if processing option has duplicates then skip it
            if (duplicateOptions.contains(label)) {
                continue;
            }
            // get the list of already selected values for the custom attribute
            List<String> selectedValues = attributeSelectedValues.get(holder.customAttribute);
            if (selectedValues == null) {
                selectedValues = new ArrayList<String>();
                attributeSelectedValues.put(holder.customAttribute, selectedValues);
            }
            // append selected value to the list of custom attribute values
            selectedValues.add(holder.option.getID());
        }
        // iterate through attributeSelectedValues and set the custom attribute
        // selected values. Coma separator is used to join multiple values for
        // the same attribute.
        for (Map.Entry<CustomAttribute, List<String>> entry : attributeSelectedValues.entrySet()) {
            // remember original value to restore it if selecting of the new
            // value fails
            String originalValue = entry.getKey().getSelectedValue();
            String value = TextUtils.join(",", entry.getValue());
            // set the custom attribute selected value
            entry.getKey().setSelectedValue(value, true);
            // check the value selected properly
            if (TextUtils.equals(value, entry.getKey().getSelectedValue())) {
                // mark attribute container with the preselected marker so the
                // user may notice which attributes was prefilled from the
                // product name
                entry.getKey().markAttributeContainer(ContainerMarkers.PRESELECTED);
                // add the attribute code to the list of updated attributes so
                // it may be processed further
                attributesSelectedFromName.add(entry.getKey().getCode());
            } else {
                // restore original value
                entry.getKey().setSelectedValue(originalValue, true);
            }
        }
        return attributesSelectedFromName;
    }

    /**
     * Check whether the {@link #predefinedCustomAttributeValues} contains
     * attribute set information and select attribute set if it does
     * 
     * @return true if the attribute set information was found and selected,
     *         otherwise returns false
     */
    protected boolean selectAttributeSetFromPredefinedAttributeValues() {
        boolean result = false;
        if (predefinedCustomAttributeValues != null) {
            // iterate through predefinedCustomAttributeValues and search for
            // attribute code matches with the MAGEKEY_PRODUCT_ATTRIBUTE_SET_ID
            for (CustomAttributeSimple predefinedAttribute : predefinedCustomAttributeValues) {
                if (TextUtils.equals(predefinedAttribute.getCode(), MAGEKEY_PRODUCT_ATTRIBUTE_SET_ID)) {
                    // select attribute set from the predefined attribute
                    // selected value information
                    selectAttributeSet(Integer.parseInt(predefinedAttribute.getSelectedValue()),
                            false, false);
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Assign values to the custom attributes from the
     * {@link #predefinedCustomAttributeValues}. Also handle special custom
     * attributes like name, description, weight and so forth
     * 
     * @return set of attribute codes which were updated during method
     *         invocation
     */
    protected Set<String> assignPredefinedAttributeValues() {
        Set<String> assignedAttributes = new HashSet<String>();
        // check required information is not null conditions
        if (predefinedCustomAttributeValues != null && customAttributesList != null
                && customAttributesList.getList() != null) {
            // variables to remember quantity and isQuantityDecimal attributes
            // value. Used to set the quantity information to quantityV field.
            // Both quantity and isQuantityDecimal values are required to set
            // quantity information properly
            String quantity = null;
            String isQuantityDecimal = null;
            // iterate through predefinedCustomAttributeValues
            for (CustomAttributeSimple predefinedAttribute : predefinedCustomAttributeValues) {
                // flag indicating whether the match within customAttributesList
                // was found
                boolean assigned = false;
                assigned = assignPredefinedAttributeValues(assignedAttributes, predefinedAttribute,
                        customAttributesList.getList());
                // if match was found within customAttributesList continue the
                // loop (move to the next predefined attribute)
                if (assigned) {
                    continue;
                }
                // set the ignore barcode text changes flag
                mIgnoreBarcodeTextChanges = true;
                try {
                    // check special custom attributes
                    assigned = assignPredefinedAttributeValues(assignedAttributes,
                            predefinedAttribute, customAttributesList.getSpecialCustomAttributes()
                                    .values());
                } finally {
                    mIgnoreBarcodeTextChanges = false;
                }
                // if match was found within special attributes continue the
                // loop (move to the next predefined attribute)
                if (assigned) {
                    continue;
                }
                // flag to manage whether the quantityV value should be assigned
                // condition.
                boolean assignQuantity = false;
                
                String value = predefinedAttribute.getSelectedValue();
                String code = predefinedAttribute.getCode();

                // change the default value of assigned field so after all if
                // conditions will end we may know whether the match was found or no
                assigned = true;
                // handle special attributes
                if (TextUtils.equals(code, MAGEKEY_PRODUCT_PRICE)) {
                    priceHandler.setPriceTextValue(value);
                } else if (TextUtils.equals(code, MAGEKEY_PRODUCT_SPECIAL_PRICE)) {
                    priceHandler.setSpecialPrice(value);
                } else if (TextUtils.equals(code, MAGEKEY_PRODUCT_SPECIAL_FROM_DATE)) {
                    priceHandler.setSpecialPriceFromDate(value);
                } else if (TextUtils.equals(code, MAGEKEY_PRODUCT_SPECIAL_TO_DATE)) {
                    priceHandler.setSpecialPriceToDate(value);
                } else if (TextUtils.equals(code, MAGEKEY_PRODUCT_QUANTITY)) {
                    quantity = value;
                    if (isQuantityDecimal != null) {
                        // if isQuantityDecimal was assigned before set the
                        // assignQuantity flag to true
                        assignQuantity = true;
                    }
                    quantityV.setText(value);
                } else if (TextUtils.equals(code, MAGEKEY_PRODUCT_IS_QTY_DECIMAL)) {
                    isQuantityDecimal = value;
                    if (quantity != null) {
                        // if quantity was assigned before set the
                        // assignQuantity flag to true
                        assignQuantity = true;
                    }
                } else {
                    // no special attribute match found. Set the assigned flag
                    // to false
                    assigned = false;
                }
                // if attribute was assigned add the attribute code to the
                // result
                if (assigned) {
                    assignedAttributes.add(code);
                }
                if (assignQuantity) {
                    // if assign quantity operation scheduled
                    Double parsedQuantity = CommonUtils.parseNumber(quantity);
                    // set the quantity information to the quantityV field and
                    // update quantityV input type
                    ProductUtils.setQuantityTextValueAndAdjustViewType(parsedQuantity == null ? 0d
                            : parsedQuantity, quantityV, JobCacheManager
                            .safeParseInt(isQuantityDecimal) == 1);
                }
            }
        }
        return assignedAttributes;
    }

    /**
     * Check whether customAttributes collection contains predefinedAttribute
     * and update its value if it does
     * 
     * @param assignedAttributes set of updated attribute codes. If match was
     *            found the predefined attribute code will be appended to it
     * @param predefinedAttribute the predefined attribute which contains new
     *            value information
     * @param customAttributes collection of custom attributes to search for
     *            predefinedAttribute match
     * @return true if predefinedAttribute was found within customAttributes
     *         collection and the value of the attribute was updated, otherwise
     *         returns false
     */
    public boolean assignPredefinedAttributeValues(Set<String> assignedAttributes,
            CustomAttributeSimple predefinedAttribute, Collection<CustomAttribute> customAttributes) {
        boolean assigned = false;
        // iterate through customAttributes and search for attribute
        // code matches
        for (CustomAttribute customAttribute : customAttributes) {
            if (TextUtils.equals(customAttribute.getCode(), predefinedAttribute.getCode())) {
                // set the custom attribute selected value from the
                // predefined attribute and update view
                customAttribute.setSelectedValue(predefinedAttribute.getSelectedValue(), true);
                // add the updated attribute code to the result
                assignedAttributes.add(customAttribute.getCode());
                // update the assigned flag and interrupt the loop
                assigned = true;
                break;
            }
        }
        return assigned;
    }

    /**
     * Start the scan activity to can the new SKU.
     * 
     * @param v the SKU attribute related view. If null action will not be
     *            performed
     * @return true in case v is not null and scan activity was started
     */
    protected boolean scanSku(View v) {
        if (v == null) {
            // if view is null interrupt the method execution
            return false;
        }
        ScanUtils.startScanActivityForResult(AbsProductActivity.this, SCAN_QR_CODE,
                R.string.scan_barcode_or_qr_label);
        return true;
    }
    /**
     * Start the scan activity to scan the new Barcode.
     * 
     * @param v the SKU attribute related view. If null action will not be
     *            performed
     * @return true in case v is not null and scan activity was started
     */
    protected boolean scanBarcode(View v) {
        if (v == null) {
        	// if view is null interrupt the method execution
            return false;
        }
        ScanUtils.startScanActivityForResult(AbsProductActivity.this, SCAN_BARCODE,
                R.string.scan_barcode_or_qr_label);
        return true;
    }

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
                    setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_SKU, "", true);
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
                    setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_SKU, "", true);
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
                // TODO remember reference to SimpleViewLoadingControl. But
                // better is to rework this part and use reference to the
                // barcode system attribute and use same logic as for the other
                // custom attributes. This requires some time which is not
                // available now 
                loadingControl = new BookInfoLoadingControl(bookLoadingHint,
                        new SimpleViewLoadingControl(bookLoadingView), false);
            } else {
                loadingControl = new BookInfoLoadingControl(attribute.getHintView(),
                        attribute.getAttributeLoadingControl(), true);
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
     * Additional init of web activity intent. Used in
     * {@link ProductEditActivity} to specify product sku information
     * 
     * @param intent
     */
    void initWebActivityIntent(Intent intent) {
    }

    /**
     * Product Create/Edit activities should to implement own logic. Product
     * edit should to check whether the intent extra SKU data is the same and
     * Product Create whether the intent extra SKU data is empty
     * 
     * @param extra
     * @return
     */
    abstract boolean isWebTextCopiedEventTarget(Intent extra);

    @Override
    public void onGeneralBroadcastEvent(EventType eventType, Intent extra) {
        switch (eventType) {
            case WEB_TEXT_COPIED: {
                CommonUtils.debug(TAG, "onGeneralBroadcastEvent: received web text copied event");
                if (isWebTextCopiedEventTarget(extra)) {
                    String attributeCode = extra.getStringExtra(EventBusUtils.ATTRIBUTE_CODE);
                    String text = extra.getStringExtra(EventBusUtils.TEXT);
                    // general case for any text custom attribute including
                    // special custom attributes
                    if (customAttributesList != null) {
                        if (TextUtils.equals(attributeCode, MAGEKEY_PRODUCT_NAME)) {
                            // for the name attribute value should not be
                            // appended but should to replace the current value
                            determineWhetherNameIsGeneratedAndSetProductName(text);
                        } else {
                            if (!appendText(attributeCode, text, customAttributesList
                                    .getSpecialCustomAttributes().values())) {
                                // if there were no any special attributes
                                // updated
                                appendText(attributeCode, text, customAttributesList.getList());
                            }
                        }
                    }
                }
                break;
            }
            case WEB_ADDRESS_COPIED: {
                CommonUtils
                        .debug(TAG, "onGeneralBroadcastEvent: received web address copied event");
                if (isWebTextCopiedEventTarget(extra)) {
                    String text = extra.getStringExtra(EventBusUtils.TEXT);
                    if (customAttributesList != null) {
                        // set the value to the all WEB_ADDRESS attributes
                        setValueToAttributesOfContentType(text, ContentType.WEB_ADDRESS,
                                customAttributesList.getSpecialCustomAttributes().values());
                        setValueToAttributesOfContentType(text, ContentType.WEB_ADDRESS,
                                customAttributesList.getList());
                    }
                }
                break;
            }
            case CUSTOM_ATTRIBUTE_OPTIONS_UPDATED: {
                CommonUtils.debug(TAG,
                        "onGeneralBroadcastEvent: received custom attribute options updated event");
                String attributeSetId = extra.getStringExtra(EventBusUtils.ATTRIBUTE_SET);
                if (customAttributesList != null && customAttributesList.getList() != null
                        && customAttributesList.getSetId() == Integer.parseInt(attributeSetId)) {
                    // if custom attribute options was updated for the attribute
                    // with the same attribute set id as loaded to the activity
                    CustomAttribute updatedAttribute = extra
                            .getParcelableExtra(EventBusUtils.ATTRIBUTE);
                    for (CustomAttribute customAttribute : customAttributesList.getList()) {
                        if (customAttribute.isSameAttribute(updatedAttribute)) {
                            // if found same attribute within loaded custom
                            // attributes list
                            customAttribute.copyOptionsButPreserveValue(updatedAttribute, true);
                        }
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    /**
     * Append the text to the attribute with the code if exists within
     * customAttributes collection
     * 
     * @param attributeCode the code of the attribute the text should be
     *            appended to
     * @param text the text to append
     * @param customAttributes collection of custom attributes to search for the
     *            attribute code match
     * @return true if the attribute with the code was found and value updated,
     *         otherwise returns false
     */
    public boolean appendText(String attributeCode, String text,
            Collection<CustomAttribute> customAttributes) {
        boolean found = false;
        for (CustomAttribute customAttribute : customAttributes) {
            if (customAttribute.isOfCode(attributeCode)) {
                if (customAttribute.isCopyFromSearch() 
                		&& (customAttribute.isOfType(CustomAttribute.TYPE_TEXT)
                		    || customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA))
                	) {
                    // if attribute is marked to be copied from search and has
                    // type TEXT or TEXTAREA
                    appendText(
                            (EditText) customAttribute.getCorrespondingView(),
                            text,
                            customAttribute.isOfType(CustomAttribute.TYPE_TEXTAREA));
                    found = true;
                }
                break;
            }
        }
        return found;
    }

    /**
     * Set the value to the attributes with the content type if exists within
     * customAttributes collection
     * 
     * @param value the value to set
     * @param contentType the content type of the attributes the value should be
     *            selected for
     * @param customAttributes collection of custom attributes to search for the
     *            content type match
     * @return true if the attributes with the content type was found and value
     *         updated, otherwise returns false
     */
    public boolean setValueToAttributesOfContentType(String value, ContentType contentType,
            Collection<CustomAttribute> customAttributes) {
        boolean found = false;
        for (CustomAttribute customAttribute : customAttributes) {
            if (customAttribute.hasContentType(contentType)) {
                // if attribute with the required content type is found
                customAttribute.setSelectedValue(value, true);
                found = true;
            }
        }
        return found;
    }

    /**
     * Append the text to the edit text box
     * 
     * @param editText view the text should be appended to
     * @param text the text to append
     * @param multiline whether the text should be appended to using new line
     *            separator or the space separator
     */
    void appendText(EditText editText, String text, boolean multiline) {
        String currentText = editText.getText().toString();
        // if current value is empty no need to append, just set the new value
        if (TextUtils.isEmpty(currentText)) {
            editText.setText(text);
        } else {
            editText.setText(currentText + (multiline ? "\n\n" : " ") + text);
        }
    }

    @Override
    public void onEditDone(String attributeCode) {
        checkValidationFailedAttribute(attributeCode);
    }
    
    /**
     * Check attribute with the specified code to be present in the
     * mValidationFailedAttributes collection and revalidate its value if it
     * does. If value will be validated successfully the attribute will be
     * removed from the mValidationFailedAttributes collection.
     * 
     * @param attributeCode the code of the attribute to revalidate
     */
    private void checkValidationFailedAttribute(String attributeCode) {
        CustomAttribute attribute = mValidationFailedAttributes.get(attributeCode);
        if (attribute != null) {
            // the attribute which previously failed value validation gets
            // updated.

            // get the current attribute value
            String value = getAttributeValue(attribute);
            // check value validity
            boolean validValue = validateAttributeValue(attribute, value);
            if (validValue) {
                // if attribute value is valid

                // remove attribute from the validation failed attributes
                // collection
                mValidationFailedAttributes.remove(attributeCode);
                // remove any marks from the attribute container
                attribute.unmarkAttributeContainer();
                if (mValidationFailedAttributes.isEmpty()) {
                    // if there are no more invalid attributes

                    // scroll to save button
                    GuiUtils.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            GuiUtils.activateField(findViewById(R.id.saveBtn), true, true, false);
                        }
                    }, 100);
                }
            }
        }
    }

    /**
     * Verify the form data
     * 
     * @param validateAttributeSet whether the attribute set should be validated
     * @param silent if true when no alerts will be shown
     * @return true if the data validated successfully, false otherwise
     */
    public boolean verifyForm(boolean validateAttributeSet, boolean silent) {
        if (validateAttributeSet) {
            // check attribute set
            if (atrSetId == INVALID_ATTRIBUTE_SET_ID) {
                GuiUtils.alert(R.string.fieldCannotBeBlank, getString(R.string.attr_set));
                GuiUtils.activateField(attributeSetV, true, true, false);
                return false;
            }
        }
        // generate SKU if barcode scanned
        if (TextUtils.isEmpty(getSpecialAttributeValue(MAGEKEY_PRODUCT_SKU))) {
            if (!TextUtils.isEmpty(getSpecialAttributeValue(MAGEKEY_PRODUCT_BARCODE))) {
                setSpecialAttributeValueIfNotNull(MAGEKEY_PRODUCT_SKU, ProductUtils.generateSku(),
                        true);
            }
        }
        // unmark previously marked validation failed attributes
        for (CustomAttribute attribute : mValidationFailedAttributes.values()) {
            attribute.unmarkAttributeContainer();
        }
        // clear the previosly found validation failed attributes
        mValidationFailedAttributes.clear();

        if (customAttributesList != null) {
            // validate special required attributes
            validateAttributes(customAttributesList.getSpecialCustomAttributes() == null ? null
                    : customAttributesList.getSpecialCustomAttributes().values());
            // validate required attributes
            validateAttributes(customAttributesList.getList());
        }
        if (mValidationFailedAttributes.isEmpty()) {
            // if there are no invalid attribute values
            return true;
        } else {
            if (!silent) {
                List<String> attributeLabels = new ArrayList<String>();
                // collect attribute labels for all attributes with invalid
                // value
                for (CustomAttribute attribute : mValidationFailedAttributes.values()) {
                    attributeLabels.add(attribute.getMainLabel());
                }
                GuiUtils.showMessageDialog(
                        null,
                        getString(R.string.missing_values_for,
                                TextUtils.join("\n", attributeLabels)), AbsProductActivity.this);
                // activate the first field in the collection
                GuiUtils.activateField(mValidationFailedAttributes.values().iterator().next()
                        .getCorrespondingView(), true, true, true);
            }
            return false;
        }
    }

    /**
     * Validate the attributes values from the customAttributes collection. All
     * the attributes with invalid values will be added to the
     * {@link #mValidationFailedAttributes} collection
     * 
     * @param customAttributes the collection of custom attributes to check
     */
    public void validateAttributes(Collection<CustomAttribute> customAttributes) {
        if (customAttributes == null) {
            // nothing to validate
            return;
        }
        // iterate through customAttributes
        for (CustomAttribute attribute : customAttributes) {
            // get the current attribute value
            String value = getAttributeValue(attribute);
            // check value validity
            boolean validValue = validateAttributeValue(attribute, value);
            if ((attribute.getIsRequired() || attribute.isOfCode(MAGEKEY_PRODUCT_PRICE))
                    && !validValue) {
                // if attribute value is required or attribute is a price
                // attribute and value is not valid
                if (attribute.getCorrespondingView() != null) {
                    // mark the attribute container with invalid value marker
                    attribute.markAttributeContainer(ContainerMarkers.REQUIRED);
                    // remember invalid attribute for future reference
                    mValidationFailedAttributes.put(attribute.getCode(), attribute);
                }
            }
        }
    }

    /**
     * Validate the attribute value. Handle some special cases such as price
     * attribute.
     * 
     * @param attribute the attribute to validate
     * @param value the attribute value
     * @return true if value is valid, false otherwise
     */
    public boolean validateAttributeValue(CustomAttribute attribute, String value) {
        if (value != null) {
            value = value.trim();
        }
        boolean validValue = attribute.isOfCode(MAGEKEY_PRODUCT_PRICE) ?
        		// price attribute has standalone validation
        		priceHandler.checkPriceValid(true, R.string.price, true) 
        		: 
        		// check value is not empty
        		!TextUtils.isEmpty(value);
        return validValue;
    }

    /**
     * Get the attribute selected value. Handle special cases like name
     * attribute. Also additionally check the corresponding edit text view value
     * 
     * @param attribute the attribute to get the value for
     * @return the selected attribute value
     */
    public String getAttributeValue(CustomAttribute attribute) {
        String value = attribute.isOfCode(MAGEKEY_PRODUCT_NAME) ?
                // product name may have not specified selected value but the 
        		// value may be present in the corresponding view hint
        		getProductName(AbsProductActivity.this)
                : 
                attribute.getSelectedValue();
        if (TextUtils.isEmpty(value) && attribute.getCorrespondingView() != null
                && attribute.getCorrespondingView() instanceof EditText) {
            // if selected attribute value is null and there is corresponding
            // EdtText view for the attribute try to retrieve the value from it
            value = attribute.getCorrespondingEditTextView().getText().toString();
        }
        return value;
    }
    /**
     * Implementation of {@link AbstractRecentWebAddressesSearchPopupHandler}
     * with the functionality necessary for {@link AbsProductActivity}
     */
    class RecentWebAddressesSearchPopupHandler extends AbstractRecentWebAddressesSearchPopupHandler {
        
        public RecentWebAddressesSearchPopupHandler() {
            super(null, WebActivity.Source.ABS_PRODUCT, AbsProductActivity.this);
        }
    
        @Override
        protected List<CustomAttribute> getCustomAttributes() {
            return customAttributesList != null ? customAttributesList.getList() : null;
        }
    
        @Override
        protected void initExtraAttributes(List<String> searchCriteriaParts,
                ArrayList<CustomAttributeSimple> textAttributes,
                ArrayList<CustomAttributeSimple> webAddressAttributes) {
            super.initExtraAttributes(searchCriteriaParts, textAttributes, webAddressAttributes);
            // check special attributes such as name and description and so forth
            if (customAttributesList != null) {
                processCustomAttributes(customAttributesList.getSpecialCustomAttributes().values(),
                        searchCriteriaParts, textAttributes, webAddressAttributes);
            }
        }

        @Override
        protected String getValue(CustomAttribute customAttribute) {
            if (customAttribute.isOfCode(MageventoryConstants.MAGEKEY_PRODUCT_NAME)) {
                // product name may have generated name in this case
                // corresponding views text will be empty
                String value = getProductName(AbsProductActivity.this);
                // updated attribute selected value so the super.getValue method
                // logic will work as expected
                customAttribute.setSelectedValue(value, false);
            }
            return super.getValue(customAttribute);
        }

        @Override
        protected void initWebActivityIntent(Intent intent) {
            super.initWebActivityIntent(intent);
            AbsProductActivity.this.initWebActivityIntent(intent);
        }
    }

    /**
     * The structure used to store custom attribute option - custom attribute
     * relation in the selectAttributeValuesFromProductName method. This is used
     * for intermediate calculations there
     */
    static class CustomAttributeValueHolder {
        /**
         * The custom attribute option
         */
        CustomAttributeOption option;
        /**
         * The related to the option custom attribute
         */
        CustomAttribute customAttribute;
    
        /**
         * @param option the custom attribute option
         * @param customAttribute the related to the option custom attribute
         */
        public CustomAttributeValueHolder(CustomAttributeOption option,
                CustomAttribute customAttribute) {
            this.option = option;
            this.customAttribute = customAttribute;
        }
    
    }

    /**
     * The task to load product attribute value asynchronously
     */
    private class ProductAttributeValueLoaderTask extends AbstractLoadProductTask {
        /**
         * The custom attribute for which the value should be loaded
         */
        CustomAttribute mCustomAttribute;

        /**
         * @param sku the SKU of the product to load attribute value from
         * @param customAttribute the custom attribute for which the value
         *            should be loaded
         */
        public ProductAttributeValueLoaderTask(String sku, CustomAttribute customAttribute) {
            super(sku, new SettingsSnapshot(AbsProductActivity.this), customAttribute
                    .getAttributeLoadingControl());
            mCustomAttribute = customAttribute;
        }

        @Override
        protected void onFailedPostExecute() {
            super.onFailedPostExecute();
            if (isCancelled()) {
                return;
            }
            if (isNotExists()) {
                // if product doesn't exist
                GuiUtils.alert(R.string.product_not_found2, getOriginalSku());
            } else {
                GuiUtils.alert(R.string.errorGeneral);
            }
        }

        @Override
        protected void onSuccessPostExecute() {
            super.onSuccessPostExecute();
            if (isCancelled()) {
                return;
            }
            if (isActivityAlive()) {
                // if activity was not destroyed during the task execution
                Product product = getProduct();
                String attributeValue = product.getStringAttributeValue(mCustomAttribute.getCode());
                if (TextUtils.isEmpty(attributeValue)) {
                    // if loaded product doesn't have a value for the required
                    // custom attribute
                    GuiUtils.alert(R.string.product_doesnt_have_attribute_value, mCustomAttribute
                            .getMainLabel().toLowerCase());
                } else {
                    // if loaded product has the value for the required
                    // custom attribute
                    mCustomAttribute.setSelectedValue(attributeValue, true);
                    // fire attribute updated via scan event
                    onAttributeUpdatedViaScan();
                }
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
         * The linked loading control which should be notified during the
         * operation
         */
        LoadingControl mLinkedLoadingControl;
        /**
         * Whether to show no book found text information after the loading
         * operation is stopped and no information was found
         */
        boolean mShowNoBookFoundHint;

        /**
         * @param hintView the text box to display loading related information
         * @param linkedLoadingControl the linked loading control which should
         *            be notified during the operation
         * @param showNoBookFoundHint whether to show no book found text
         *            information after the loading operation is stopped and no
         *            information was found
         */
        public BookInfoLoadingControl(TextView hintView, LoadingControl linkedLoadingControl,
                boolean showNoBookFoundHint) {
            mHintView = hintView;
            mLinkedLoadingControl = linkedLoadingControl;
            mShowNoBookFoundHint = showNoBookFoundHint;
        }

        @Override
        public void startLoading() {
            mIsLoading = true;
            mHintView.setTextColor(mDefaultTextColor);
            mHintView.setText(R.string.requesting_book_details);
            mHintView.setVisibility(View.VISIBLE);
            if (mLinkedLoadingControl != null) {
                // notify linked loading control about loading started
                mLinkedLoadingControl.startLoading();
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
                if (mLinkedLoadingControl != null) {
                    // notify linked loading control about loading stopped
                    mLinkedLoadingControl.stopLoading();
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
                // TODO ISBN10 and ISBN13 attribute code check should be
                // removed after all servers gets updated. Only content type
                // check should be here
                if (attribute.hasContentType(ContentType.ISBN10)
                        || attribute.isOfCode(BookInfoLoader.ISBN_10_ATTRIBUTE)) {
                    // if isbn 10 attribute modified
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
                } else if (attribute.hasContentType(ContentType.ISBN13)
                        || attribute.isOfCode(BookInfoLoader.ISBN_13_ATTRIBUTE)) {
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
        	// TODO ISBN10 and ISBN13 attribute code check should be
            // removed after all servers gets updated. Only content type
            // check should be here
            if (attribute.hasContentType(ContentType.ISBN10)
                    || attribute.hasContentType(ContentType.ISBN13)
                    || attribute.isOfCode(BookInfoLoader.ISBN_10_ATTRIBUTE)
                    || attribute.isOfCode(BookInfoLoader.ISBN_13_ATTRIBUTE)) {
                // if attribute is ISBN 10 or ISBN 13 attribute
                processIsbnAttribute(attribute);
            } else if (attribute.hasContentType(ContentType.ISSN)
                    || attribute.isOfCode(BookInfoLoader.ISSN_ATTRIBUTE)) {
                // if attribute is iSSN attribute
                //
                // TODO ISSN attribute code check should be
                // removed after all servers gets updated. Only content type
                // check should be here
                processIssnAttribute(attribute);
            } else if (attribute.isOfCode(MAGEKEY_PRODUCT_NAME)) {
                processNameAttribute(attribute);
            } else if (attribute.isOfCode(MAGEKEY_PRODUCT_SKU)) {
                processSkuAttribute(attribute);
            } else if (attribute.isOfCode(MAGEKEY_PRODUCT_BARCODE)) {
                processBarcodeAttribute(attribute);
            }
            // extra initialization for the text attributes
            if (attribute.isOfType(CustomAttribute.TYPE_TEXT)
                    || attribute.isOfType(CustomAttribute.TYPE_TEXTAREA)
                    || attribute.isOfType(CustomAttribute.TYPE_WEIGHT)) {
                final CustomAttributeInputHandler inputHandler = new CustomAttributeInputHandler(
                        attribute, AbsProductActivity.this);
                // some attributes has default input type such as Scan or Gesture
                // input. The corresponding views should be set to non focusable
                // for such attributes to do not show keyboard when tapped
                inputHandler.adjustFocusableIfNecessary(false);
                attribute.getCorrespondingView().setOnFocusChangeListener(
                        new FocusChangeListenerWrapper(attribute.getCorrespondingView()
                                .getOnFocusChangeListener()) {
                            @Override
                            public void onFocusChange(View v, boolean hasFocus) {
                                super.onFocusChange(v, hasFocus);
                                if (!hasFocus) {
                                    // restore the onclick listener if focus
                                    // lost. The onClickListener could be
                                    // removed when the keyboard input method
                                    // gets activated to allow user to click
                                    // within edited text to change the cursor
                                    // position
                                    attribute.getCorrespondingView().setOnClickListener(
                                            inputHandler);
                                    // notify handler that field is deactivated
                                    // by setting null input method
                                    inputHandler.handleInputMethod(null);
                                }
                            }
                        });
                attribute.getCorrespondingView().setOnClickListener(inputHandler);
                attribute.getCorrespondingView().setOnLongClickListener(inputHandler);
                attribute.getCorrespondingEditTextView().setHint(attribute.getHint());
            }
        }
        /**
         * Process the name special attribute. Add additional focus listener which will
         * compare specified name with the generated name on focus lost
         * 
         * @param attribute
         */
        public void processNameAttribute(final CustomAttribute attribute) {
            attribute.getCorrespondingEditTextView().setHorizontallyScrolling(false);
            attribute.getCorrespondingEditTextView().setMaxLines(Integer.MAX_VALUE);
            attribute.getCorrespondingView().setOnFocusChangeListener(
                    new FocusChangeListenerWrapper(attribute.getCorrespondingView()
                            .getOnFocusChangeListener()) {

                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            super.onFocusChange(v, hasFocus);
                            EditText editText = (EditText) v;
                            if (hasFocus) {
                                // if focus gained
                                if (TextUtils.isEmpty(editText.getText().toString())) {
                                    // if value of the text field is empty copy
                                    // generated name from the hint
                                    editText.setText(editText.getHint());
                                    editText.selectAll();
                                }
                            } else {
                                if (TextUtils.equals(editText.getText(), editText.getHint())) {
                                    // if specified text is the same as hint
                                    // clear the editText value but leave hint
                                    editText.setText("");
                                }
                            }
                        }
                    });
        }

        /**
         * Process the Barcode attribute. Add additional focus listener which
         * will check for code existing on focus lost. Also add live ISSN and
         * ISBN code recognition
         * 
         * @param attribute
         */
        public void processBarcodeAttribute(final CustomAttribute attribute) {
            attribute.getCorrespondingView().setOnFocusChangeListener(
                    new FocusChangeListenerWrapper(attribute.getCorrespondingView()
                            .getOnFocusChangeListener()) {

                        /**
                         * Field to store initial value when focus is gained
                         */
                        String mInitialValue;

                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            super.onFocusChange(v, hasFocus);
                            TextView barcodeInput = (TextView) v;
                            String code = barcodeInput.getText().toString();
                            if (hasFocus) {
                                mInitialValue = code;
                            } else {
                                // if value was not changed when do not call
                                // checkCodeExists
                                if (!TextUtils.isEmpty(code)
                                        && !TextUtils.equals(code, mInitialValue)) {
                                    checkCodeExists(code, true);
                                }
                            }
                        }
                    });
            // TODO remove this, add same ISBN check handling as for the other
            // ISBN attributes
            // support for live ISBN code recognition and book information
            // loading
            attribute.getCorrespondingEditTextView().addTextChangedListener(new TextWatcher() {

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
                    if (attribute.getCorrespondingEditTextView().isFocused() && !mIgnoreBarcodeTextChanges) {
                        String code = s.toString();
                        if (!TextUtils.isEmpty(code)) {
                            checkBookBarcodeEntered(code);
                        }
                    }
                }
            });
        }
        
        /**
         * Process the SKU attribute. Add additional focus listener which checks
         * the entered value on focus lost
         * 
         * @param attribute
         */
        public void processSkuAttribute(final CustomAttribute attribute) {
            attribute.getCorrespondingView().setOnFocusChangeListener(
                    new FocusChangeListenerWrapper(attribute.getCorrespondingView()
                            .getOnFocusChangeListener()) {

                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            super.onFocusChange(v, hasFocus);
                            if (hasFocus == false) {
                                TextView skuV = (TextView) v;
                                String skuText = skuV.getText().toString();
                                if (!TextUtils.isEmpty(skuText)) {
                                    // check whether manually entered SKU is of
                                    // the proper format. If it is not assume it
                                    // is a Barcode and clear skuV input, fill
                                    // Barcode input and perform code check as
                                    // Barcode. In other cases run SKU already
                                    // exists check
                                    CheckSkuResult checkResult = ScanActivity.checkSku(skuText);
                                    if (checkResult.isBarcode
                                            && isSpecialAttributeAvailable(MAGEKEY_PRODUCT_BARCODE)) {
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
        }

        /**
         * Process the ISSN attribute. Add additional focus listener which will
         * show hint on focus lost if the value will have invalid format
         * 
         * @param attribute
         */
        public void processIssnAttribute(final CustomAttribute attribute) {
            attribute.getCorrespondingView().setOnFocusChangeListener(
                    new FocusChangeListenerWrapper(attribute.getCorrespondingView()
                            .getOnFocusChangeListener()) {

                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            super.onFocusChange(v, hasFocus);
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
            
            attribute.getCorrespondingView().setOnFocusChangeListener(
                    new FocusChangeListenerWrapper(attribute.getCorrespondingView()
                            .getOnFocusChangeListener()) {

                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            super.onFocusChange(v, hasFocus);
                            if (!hasFocus) {
                                String code = ((EditText) v).getText().toString();
                                boolean valid = true;
                                // check the code validness
                                if (!TextUtils.isEmpty(code)) {
                                    // TODO remove attribute code check, leave
                                    // only content type check when all the
                                    // servers gets updated with the new API
                                    if (attribute.isOfCode(BookInfoLoader.ISBN_10_ATTRIBUTE)
                                            || attribute.hasContentType(ContentType.ISBN10)) {
                                        // if attribute is ISBN10 attribute
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
        
        /**
         * Wrapper around already present FocusChangeListener to translate focus
         * change event to it if present
         */
        class FocusChangeListenerWrapper implements OnFocusChangeListener {
            /**
             * The original focus change listener. If not null the event will be
             * passed to it also in the onFocusChange method
             */
            OnFocusChangeListener mOriginalListener;
            
            /**
             * @param originalListener the focus listener to wrap
             */
            FocusChangeListenerWrapper(OnFocusChangeListener originalListener){
                mOriginalListener = originalListener;
            }
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // pass the event to the original focus listener
                if (mOriginalListener != null) {
                    mOriginalListener.onFocusChange(v, hasFocus);
                }
            }
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
     * Common on click and on long click listeners for attribute fields which
     * shows popup menu or handles default input type. Popup menu items depends
     * on the CustomAttribute parameter passed to constructor
     */
    static class CustomAttributeInputHandler implements OnLongClickListener, OnClickListener {

        /**
         * Map which stores relations between menu item ids and related input
         * methods
         */
        static final Map<Integer, InputMethod> MENU_ID_INPU_METHOD_MAP = new HashMap<Integer, InputMethod>();
        /**
         * Initialize MENU_ID_INPU_METHOD_MAP values
         */
        static {
            MENU_ID_INPU_METHOD_MAP.put(R.id.menu_gesture_input, InputMethod.GESTURES);
            MENU_ID_INPU_METHOD_MAP.put(R.id.menu_normal_keyboard, InputMethod.NORMAL_KEYBOARD);
            MENU_ID_INPU_METHOD_MAP.put(R.id.menu_numeric_keyboard, InputMethod.NUMERIC_KEYBOARD);
            MENU_ID_INPU_METHOD_MAP.put(R.id.menu_scan_free_text, InputMethod.SCANNER);
            MENU_ID_INPU_METHOD_MAP.put(R.id.menu_search_the_internet,
                    InputMethod.COPY_FROM_INTERNET_SEARCH);
            MENU_ID_INPU_METHOD_MAP.put(R.id.menu_copy_from_another,
                    InputMethod.COPY_FROM_ANOTHER_PRODUCT);
        }

        /**
         * The related to the click handler custom attribute. Used to read
         * attribute options
         */
        CustomAttribute mCustomAttribute;

        /**
         * Related activity
         */
        AbsProductActivity mActivity;

        /**
         * The flag indicating alternative keyboard input was activated
         */
        boolean mAlternateKeyboardInputActivated;

        /**
         * @param customAttribute the related to the click handler custom
         *            attribute. Used to read attribute options
         * @param activity related activity
         */
        public CustomAttributeInputHandler(CustomAttribute customAttribute,
                AbsProductActivity activity) {
            mCustomAttribute = customAttribute;
            mActivity = activity;
        }
        
        @Override
        public void onClick(View v) {
            // remember last used attribute
            mActivity.mLastUsedCustomAttribute = mCustomAttribute;
            // handle the default input method
            handleInputMethod(mCustomAttribute.getInputMethod());
        }

        @Override
        public boolean onLongClick(final View v) {
            PopupMenu popup = new PopupMenu(mActivity, v);
            MenuInflater inflater = popup.getMenuInflater();
            Menu menu = popup.getMenu();
            inflater.inflate(R.menu.custom_attribute_popup, menu);

            // check whether the paste item should be enabled. It depends on
            // whether the clipboard contains text or not
            MenuItem pasteItem = menu.findItem(R.id.menu_paste);
            boolean pasteEnabled;
            if (!(mActivity.mClipboard.hasPrimaryClip())) {

                pasteEnabled = false;

            } else if (!(mActivity.mClipboard.getPrimaryClipDescription()
                    .hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))) {

                // This disables the paste menu item, since the clipboard
                // has data but it is not plain text
                pasteEnabled = false;
            } else {

                // This enables the paste menu item, since the clipboard
                // contains plain text in case view is instance of EditText.
                pasteEnabled = v instanceof EditText;
            }
            pasteItem.setVisible(pasteEnabled);

            for (Map.Entry<Integer, InputMethod> entry : MENU_ID_INPU_METHOD_MAP.entrySet()) {
                // check whether the menu item should be visible. It
                // depends on whether the view is instance of EditText and the
                // attribute has alternate input method
                MenuItem menuItem = menu.findItem(entry.getKey());
                boolean additionalCondition = true;
                if (entry.getValue() != InputMethod.COPY_FROM_ANOTHER_PRODUCT) {
                    // all non Copy from another product input methods are
                    // available only for EditText fields
                    additionalCondition = v instanceof EditText;
                }
                boolean hasInputMethod = mCustomAttribute.hasAlternateInputMethod(entry.getValue())
                        // if the alternative keyboard input method was activated the
                        // default input method should be added to alternate input
                        // methods menu even if it is not present in the alternative
                        // input methods of the custom attribute
                        || (mAlternateKeyboardInputActivated 
                        		&& mCustomAttribute.getInputMethod() == entry.getValue());
                menuItem.setVisible(hasInputMethod
                        && additionalCondition);
            }

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    // remember last used attribute
                    mActivity.mLastUsedCustomAttribute = mCustomAttribute;
                    int menuItemIndex = item.getItemId();
                    switch (menuItemIndex) {
                        case R.id.menu_paste:
                            // Examines the item on the clipboard. If
                            // getText() does not return null, the clip item
                            // contains the text. Assumes that this
                            // application can only handle one item at a
                            // time.
                            ClipData.Item citem = mActivity.mClipboard.getPrimaryClip()
                                    .getItemAt(0);

                            CharSequence pasteData = citem.coerceToText(mActivity);
                            ((EditText) v).setText(pasteData);
                            break;
                        case R.id.menu_normal_keyboard:
                        case R.id.menu_numeric_keyboard:
                        case R.id.menu_gesture_input:
                        case R.id.menu_scan_free_text:
                        case R.id.menu_copy_from_another:
                        case R.id.menu_search_the_internet:
                            handleInputMethod(MENU_ID_INPU_METHOD_MAP.get(menuItemIndex));
                            break;
                        default:
                            return false;
                    }
                    return true;
                }
            });

            popup.show();
            return true;
        }

        /**
         * Check the inputMethod and perform corresponding actions
         * 
         * @param inputMethod
         */
        public void handleInputMethod(InputMethod inputMethod) {
            if (inputMethod == null) {
                // if field is deactivated, null inputMethod
                // 
                // make field non focusable again if necessary when the field
                // gets deactivated
                adjustFocusableIfNecessary(false);
                // reset alternate keyboard input activated flag
                mAlternateKeyboardInputActivated = false;
                return;
            }
            switch (inputMethod) {
                case NORMAL_KEYBOARD:
                    normalKeyboard();
                    break;
                case NUMERIC_KEYBOARD:
                    numericKeyboard();
                    break;
                case GESTURES:
                    gestureInput();
                    break;
                case SCANNER:
                    scanFreeText();
                    break;
                case COPY_FROM_ANOTHER_PRODUCT:
                    copyFromAnotherProduct();
                    break;
                case COPY_FROM_INTERNET_SEARCH:
                    searchInternet();
                    break;
                default:
            }
        }

        /**
         * Start searching on the Internet
         */
        public void searchInternet() {
            mActivity.mRecentWebAddressesSearchPopupHandler.prepareAndShowSearchInternetMenu(
                    mCustomAttribute.getCorrespondingView(), mActivity.mSettings.getUrl());
        }

        /**
         * Start the scan activity to scan the product code from where the
         * attribute value should be copied
         */
        public void copyFromAnotherProduct() {
            ScanUtils.startScanActivityForResult(mActivity,
                    SCAN_ANOTHER_PRODUCT_CODE, R.string.scan_barcode_or_qr_label);
        }

        /**
         * Scan any text to fill the attribute value
         */
        public void scanFreeText() {
            if (mCustomAttribute.isOfCode(MAGEKEY_PRODUCT_SKU)) {
                mActivity.scanSku(mCustomAttribute.getCorrespondingView());
            } else if (mCustomAttribute.isOfCode(MAGEKEY_PRODUCT_BARCODE)) {
                mActivity.scanBarcode(mCustomAttribute.getCorrespondingView());
            } else {
                ScanUtils.startScanActivityForResult(mActivity, SCAN_ATTRIBUTE_TEXT,
                        R.string.scan_free_text);
            }
        }

        /**
         * Use gestures to input the custom attribute values
         */
        public void gestureInput() {
            EditText edit = mCustomAttribute.getCorrespondingEditTextView();

            Intent gestureInputIntent = new Intent(mActivity,
                    GestureInputActivity.class);
            gestureInputIntent.putExtra(GestureInputActivity.PARAM_INPUT_TYPE, edit.getInputType());
            gestureInputIntent.putExtra(GestureInputActivity.PARAM_INITIAL_TEXT, edit.getText()
                    .toString());

            mActivity.startActivityForResult(gestureInputIntent, LAUNCH_GESTURE_INPUT);
        }

        /**
         * Adjust input type and show the normal keyboard
         */
        public void normalKeyboard() {
            setKeyboardInputMethod(InputMethod.NORMAL_KEYBOARD);
        }

        /**
         * Adjust input type and show the numeric keyboard
         */
        public void numericKeyboard() {
            setKeyboardInputMethod(InputMethod.NUMERIC_KEYBOARD);
        }

        /**
         * Set the keyboard input method and display the keyboard itself
         * 
         * @param inputMethod
         */
        public void setKeyboardInputMethod(InputMethod inputMethod) {
            if (inputMethod != mCustomAttribute.getInputMethod()) {
                // if alternative keyboard input method was activated set the
                // corresponding flag
                mAlternateKeyboardInputActivated = true;
            }
            // adjust custom attribute corresponding view focusable properties
            // if necessary. Field may be not focusable in case default action
            // is not normal or numeric keyboard
            adjustFocusableIfNecessary(true);
            AbstractCustomAttributeViewUtils.setKeyboardInputMethod(mCustomAttribute,
                    mCustomAttribute.getCorrespondingEditTextView(), inputMethod);
            // remove on click listener to allow user to click within
            // edited text to change the cursor position
            mCustomAttribute.getCorrespondingView().setOnClickListener(null);
            GuiUtils.activateField(mCustomAttribute.getCorrespondingView(), true, true, true);
        }

        /**
         * Adjust focusable settings for the corresponding view in case default
         * input method is not numeric or normal keyboard. Such fields should
         * become non focusable to do not display any keyboard when clicked in
         * some cases
         * 
         * @param focusable whether the field should be focusable or no
         * @return
         */
        public boolean adjustFocusableIfNecessary(boolean focusable) {
            boolean result = false;
            if (mCustomAttribute.getInputMethod() != InputMethod.NORMAL_KEYBOARD
                    && mCustomAttribute.getInputMethod() != InputMethod.NUMERIC_KEYBOARD) {
                View view = mCustomAttribute.getCorrespondingView();
                // if default input method for the custom attribute should not
                // show any keyboard
                view.setFocusable(focusable);
                view.setFocusableInTouchMode(focusable);
                result = true;
            }
            return result;
        }
    }
}
