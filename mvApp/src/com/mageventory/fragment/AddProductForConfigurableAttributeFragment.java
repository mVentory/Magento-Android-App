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

package com.mageventory.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mventory.R;
import com.mageventory.activity.ProductCreateActivity;
import com.mageventory.activity.ProductEditActivity;
import com.mageventory.activity.ScanActivity;
import com.mageventory.activity.ScanActivity.CheckSkuResult;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.fragment.base.BaseDialogFragment;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributeSimple;
import com.mageventory.model.Product;
import com.mageventory.model.util.AbstractCustomAttributeViewUtils;
import com.mageventory.model.util.AbstractCustomAttributeViewUtils.CommonOnNewOptionTaskEventListener;
import com.mageventory.model.util.ProductUtils;
import com.mageventory.model.util.ProductUtils.PriceInputFieldHandler;
import com.mageventory.model.util.ProductUtils.PricesInformation;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.AbstractLoadProductTask;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.NumberUtils;
import com.mageventory.util.SimpleViewLoadingControl;
import com.mageventory.widget.util.RichTextUtils;

/**
 * The fragment which represents dialog for adding new product for configurable
 * attribute
 * 
 * @author Eugene Popovich
 */
public class AddProductForConfigurableAttributeFragment extends BaseDialogFragment implements
        OnClickListener {

    /**
     * Tag used for logging
     */
    public static final String TAG = AddProductForConfigurableAttributeFragment.class
            .getSimpleName();

    /**
     * View to show the custom attribute main label
     */
    TextView mAttributeLabelView;
    /**
     * View to show value for the custom attribute of the source product
     */
    TextView mSourceAttributeValueView;
    /**
     * View to show the price information of the source product
     */
    TextView mSourcePriceView;
    /**
     * View to show the quantity information of the source product
     */
    TextView mSourceQtyView;
    /**
     * The price input field handler for the new product price field
     */
    PriceInputFieldHandler mNewProductPriceHandler;
    /**
     * The view to edit new product quantity information
     */
    EditText mNewProductQtyView;
    /**
     * The vie to show the product will be created information message
     */
    TextView mProductWillBeCreatedMessageView;
    /**
     * The view to show the SKU belongs to information after the SKU existing
     * check
     */
    TextView mSkuBelongsToView;
    /**
     * The view to show the SKU belongs to extra information after the SKU
     * existing check
     */
    TextView mSkuBelongsToExtraView;
    /**
     * The checkbox view which shows an option whether the editing before save
     * operation is allowed
     */
    CheckBox mEditBeforeSavingCb;

    /**
     * Button that shows found scanned product details when clicked
     */
    Button mViewScannedProductBtn;
    /**
     * Button which navigates user to the save step when clicked. Appears after
     * the SKU check is done successfully
     */
    Button mSaveBtn;
    /**
     * Button which navigates user to the save step when clicked. Appears if SKU
     * check is pending or failed
     */
    Button mSaveNoCheckBtn;
    /**
     * Button which closes dialog when clicked
     */
    Button mCancelBtn;

    /**
     * The source product configurable custom attribute information
     */
    CustomAttribute mSourceCustomAttribute;
    /**
     * The new product custom attribute information
     */
    CustomAttribute mNewProductCustomAttribute;

    /**
     * Reference to the source product
     */
    Product mSourceProduct;
    /**
     * The source product custom attribute information
     */
    Collection<CustomAttribute> mSourceProductCustomAttributes;
    /**
     * The target product if found during SKU check operation
     */
    Product mTargetProduct;

    /**
     * The loading control for the SKU check operation
     */
    SkuCheckLoadingControl mSkuCheckLoadingControl;
    /**
     * The loading control for the new option creation operation
     */
    LoadingControl mNewOptionCreationLoadingControl;

    /**
     * The SKU of the new product for configurable attribute which should be
     * created/edited.
     */
    String mSku;

    /**
     * The flag indicating whether the quantity was copied from the target
     * product. Used to handle passing of isQtyDecimal parameter
     */
    boolean mQtyFromTargetProductCopied = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_product_for_configurable_attribute, container);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view, savedInstanceState);
    }

    /**
     * Initialize the fragment view
     * 
     * @param view
     * @param savedInstanceState
     */
    void init(View view, Bundle savedInstanceState) {
        /*
         * initialize references to views
         */
        mAttributeLabelView = (TextView) view.findViewById(R.id.attrLabel);

        View sourceProductDetailsView = view.findViewById(R.id.producDetails);
        mSourceAttributeValueView = (TextView) sourceProductDetailsView
                .findViewById(R.id.attrValue);
        mSourcePriceView = (TextView) sourceProductDetailsView.findViewById(R.id.price);
        mSourceQtyView = (TextView) sourceProductDetailsView.findViewById(R.id.qty);

        View newProducDetailsView = view.findViewById(R.id.newProducDetails);
        mNewProductPriceHandler = new PriceInputFieldHandler(
                (EditText) newProducDetailsView.findViewById(R.id.price), getActivity());
        mNewProductQtyView = (EditText) newProducDetailsView.findViewById(R.id.qty);

        mNewOptionCreationLoadingControl = new SimpleViewLoadingControl(
                view.findViewById(R.id.newOptionCreationLoading));

        mSkuCheckLoadingControl = new SkuCheckLoadingControl(
                view.findViewById(R.id.skuCheckLoading));

        mProductWillBeCreatedMessageView = (TextView) view.findViewById(R.id.productWillBeCreated);
        mSkuBelongsToView = (TextView) view.findViewById(R.id.skuBelongsTo);
        mSkuBelongsToExtraView = (TextView) view.findViewById(R.id.skuBelongsToExtra);
        mEditBeforeSavingCb = (CheckBox) view.findViewById(R.id.editBeforeSaving);

        mViewScannedProductBtn = (Button) view.findViewById(R.id.viewScannedProductBtn);
        mCancelBtn = (Button) view.findViewById(R.id.cancelBtn);
        mSaveBtn = (Button) view.findViewById(R.id.saveBtn);
        mSaveNoCheckBtn = (Button) view.findViewById(R.id.saveNoCheckBtn);

        /*
         * additional views initialization: filling with values, etc
         */

        mAttributeLabelView.setText(mSourceCustomAttribute.getMainLabel());
        // source product data
        mSourceAttributeValueView.setText(mSourceCustomAttribute.getUserReadableSelectedValue());
        mSourcePriceView.setText(CommonUtils.appendCurrencySignToPriceIfNotEmpty(ProductUtils
                .getProductPricesString(mSourceProduct)));
        mSourceQtyView.setText(ProductUtils.getQuantityString(mSourceProduct));

        // new product data
        CustomAttributeViewUtils customAttributeViewUtils = new CustomAttributeViewUtils();
        customAttributeViewUtils.initAtrEditView(newProducDetailsView, mNewProductCustomAttribute);
        mNewProductPriceHandler.setDataFromProduct(mSourceProduct);
        ProductUtils.setQuantityTextValueAndAdjustViewType(1, mNewProductQtyView, mSourceProduct);
        // monitor for fields text changed to invalidate mSkuBelongsToExtraView
        // link colors
        TextWatcher invalidateSkuBelongsToExtraTextWatcher = new InvalidateSkuBelongsToExtraTextWatcher();
        mNewProductQtyView.addTextChangedListener(invalidateSkuBelongsToExtraTextWatcher);
        mNewProductPriceHandler.getPriceView().addTextChangedListener(
                invalidateSkuBelongsToExtraTextWatcher);
        ((EditText) mNewProductCustomAttribute.getCorrespondingView())
                .addTextChangedListener(invalidateSkuBelongsToExtraTextWatcher);

        // initialize buttons' handlers
        mViewScannedProductBtn.setOnClickListener(this);
        mCancelBtn.setOnClickListener(this);
        mSaveBtn.setOnClickListener(this);
        mSaveNoCheckBtn.setOnClickListener(this);

        // adjust views visibility
        mSkuCheckLoadingControl.setViewVisibile(false);
        mSkuBelongsToView.setVisibility(View.GONE);
        mSkuBelongsToExtraView.setVisibility(View.GONE);
        // hide this option for now
        mEditBeforeSavingCb.setVisibility(View.GONE);

        mViewScannedProductBtn.setVisibility(View.GONE);
        mSaveBtn.setVisibility(View.GONE);
        mSaveNoCheckBtn.setVisibility(View.VISIBLE);

        if (!TextUtils.isEmpty(mSku)) {
            mProductWillBeCreatedMessageView.setVisibility(View.GONE);
            // if SKU passed to the fragment is not empty run the SKU check
            new LoadProductTaskAsync().execute();
        } else {
            // SKU is not passed so nothing to check. Hide mSaveNoCheckBtn and
            // show mSaveBtn
            mSaveNoCheckBtn.setVisibility(View.GONE);
            mSaveBtn.setVisibility(View.VISIBLE);
            mProductWillBeCreatedMessageView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == mViewScannedProductBtn.getId()) {
            // view scanned product button clicked
            ScanActivity.startForSku(mTargetProduct.getSku(), getActivity());
        } else if (v.getId() == mCancelBtn.getId()) {
            // cancel button clicked
            dismissAllowingStateLoss();
        } else if (v.getId() == mSaveBtn.getId() || v.getId() == mSaveNoCheckBtn.getId()) {
            // save or saveNoCheck button clicked
            if (!doSave()) {
                return;
            }
        }
        dismissAllowingStateLoss();
    }

    /**
     * Perform save operation
     * 
     * @return
     */
    boolean doSave() {
        // validate whether the required data is specified
        if (!GuiUtils.validateBasicTextData(R.string.fieldCannotBeBlank, new String[] {
            mNewProductCustomAttribute.getSelectedValue()
        }, new String[] {
            mNewProductCustomAttribute.getMainLabel()
        }, null, false)) {
            // data validation failed
            return false;
        }
        // validate price
        if (!mNewProductPriceHandler.checkPriceValid(true, R.string.price, false)) {
            return false;
        }
        if (!GuiUtils.validateBasicTextData(R.string.fieldCannotBeBlank, new int[] {
            R.string.quantity
        }, new TextView[] {
            mNewProductQtyView
        }, false)) {
            return false;
        }
        // list of predefined custom attribute values which should be passed to
        // Create/Edit activities
        ArrayList<CustomAttributeSimple> predefinedCustomAttributeValues = new ArrayList<CustomAttributeSimple>();
        // add the selected custom attribute value
        predefinedCustomAttributeValues.add(CustomAttributeSimple.from(mNewProductCustomAttribute));

        // add the predefined price information
        String priceString = mNewProductPriceHandler.getPriceView().getText().toString();
        PricesInformation pricesInformation = ProductUtils.getPricesInformation(priceString);
        predefinedCustomAttributeValues.add(new CustomAttributeSimple(
                MageventoryConstants.MAGEKEY_PRODUCT_PRICE, null, null, CommonUtils
                        .formatNumberIfNotNull(pricesInformation.regularPrice)));
        predefinedCustomAttributeValues.add(new CustomAttributeSimple(
                MageventoryConstants.MAGEKEY_PRODUCT_SPECIAL_PRICE, null, null, CommonUtils
                        .formatNumberIfNotNull(pricesInformation.specialPrice)));
        predefinedCustomAttributeValues
                .add(new CustomAttributeSimple(
                        MageventoryConstants.MAGEKEY_PRODUCT_SPECIAL_FROM_DATE, null, null,
                        CommonUtils.formatDateIfNotNull(mNewProductPriceHandler
                                .getSpecialPriceData().fromDate)));
        predefinedCustomAttributeValues
                .add(new CustomAttributeSimple(
                        MageventoryConstants.MAGEKEY_PRODUCT_SPECIAL_TO_DATE, null, null,
                        CommonUtils.formatDateIfNotNull(mNewProductPriceHandler
                                .getSpecialPriceData().toDate)));
        // add the quantity and quantity type information
        String quantityText = mNewProductQtyView.getText().toString();
        predefinedCustomAttributeValues.add(new CustomAttributeSimple(
                MageventoryConstants.MAGEKEY_PRODUCT_QUANTITY, null, null, quantityText));
        predefinedCustomAttributeValues.add(new CustomAttributeSimple(
                MageventoryConstants.MAGEKEY_PRODUCT_IS_QTY_DECIMAL, null, null, Integer
                        .toString(mQtyFromTargetProductCopied ? mTargetProduct.getIsQtyDecimal()
                                : mSourceProduct.getIsQtyDecimal())));

        // add the attribute set information (use source product attribute set)
        predefinedCustomAttributeValues.add(new CustomAttributeSimple(
                MageventoryConstants.MAGEKEY_PRODUCT_ATTRIBUTE_SET_ID, null, null, Integer
                        .toString(mSourceProduct.getAttributeSetId())));

        // add the SKU or barcode information
        if (TextUtils.isEmpty(mSku)) {
            mSku = ProductUtils.generateSku();
        }
        CheckSkuResult checkSkuResult = ScanActivity.checkSku(mSku);
        // check whether the SKU is in the right format
        boolean barcodeScanned = checkSkuResult.isBarcode;
        if (barcodeScanned) {
            // SKU is of barcode format. Put barcode information to predefined
            // attributes
            predefinedCustomAttributeValues.add(new CustomAttributeSimple(
                    MageventoryConstants.MAGEKEY_PRODUCT_BARCODE, null, null, mSku));
        } else {
            // SKU has valid format. Put SKU information to predefined
            // attributes
            predefinedCustomAttributeValues.add(new CustomAttributeSimple(
                    MageventoryConstants.MAGEKEY_PRODUCT_SKU, null, null, mSku));
        }

        if (mTargetProduct != null) {
            // if target product information is empty (empty SKU or product not
            // found or SKU check exception)
            ProductEditActivity.launchProductEdit(mTargetProduct.getSku(), false, false,
                    mEditBeforeSavingCb.isChecked(), mSourceProduct.getSku(),
                    predefinedCustomAttributeValues, getActivity());
        } else {
            // List of attribute codes which should not be copied to the new
            // product
            Set<String> attributeCodesToSkip = new HashSet<String>() {
                private static final long serialVersionUID = 1L;
                {
                    add(MageventoryConstants.MAGEKEY_PRODUCT_SKU);
                    add(MageventoryConstants.MAGEKEY_PRODUCT_BARCODE);
                }
            };
            // add dynamic custom attributes information
            ArrayList<CustomAttributeSimple> predefinedCustomAttributeValues2 = new ArrayList<CustomAttributeSimple>();
            for (CustomAttribute customAttribute : mSourceProductCustomAttributes) {
                // if attribute code should be skipped continue the loop to next
                // iteration
                if (attributeCodesToSkip.contains(customAttribute.getCode())) {
                    continue;
                }
                // flag check whether the predefinedCustomAttributeValues
                // already contains dynamic custom attribute information
                boolean found = false;
                // iterate through predefinedCustomAttributeValues and search
                // for match
                for (CustomAttributeSimple customAttributeSimple : predefinedCustomAttributeValues) {
                    if (TextUtils
                            .equals(customAttributeSimple.getCode(), customAttribute.getCode())) {
                        // if match found set the found flag and interrupt the
                        // loop
                        found = true;
                        break;
                    }
                }
                // if match found within predefinedCustomAttributeValues
                // continue the loop to next iteration
                if (found) {
                    continue;
                }
                // add the custom attribute information to predefined values
                predefinedCustomAttributeValues2.add(CustomAttributeSimple.from(customAttribute));
            }
            // join static and dynamic custom attributes information
            predefinedCustomAttributeValues.addAll(predefinedCustomAttributeValues2);

            ProductCreateActivity.launchProductCreate(null, false, false, 0, null, null, true,
                    false, null, null, mEditBeforeSavingCb.isChecked(), mSourceProduct.getSku(),
                    predefinedCustomAttributeValues, getActivity());
        }
        return true;
    }

    /**
     * Set the data
     * 
     * @param sku The SKU of the new product for configurable attribute which
     *            should be created/edited.
     * @param product the source product
     * @param customAttribute The source product configurable custom attribute
     *            information
     * @param customAttributes the product custom attributes information
     */
    public void setData(String sku, Product product, CustomAttribute customAttribute,
            Collection<CustomAttribute> customAttributes) {
        mSku = sku;
        mSourceProduct = product;
        mSourceCustomAttribute = customAttribute;
        mSourceProductCustomAttributes = customAttributes;
        // clone the source custom attribute and clear the value
        mNewProductCustomAttribute = mSourceCustomAttribute.clone();
        mNewProductCustomAttribute.setSelectedValue(null, false, false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog result = super.onCreateDialog(savedInstanceState);
        result.setTitle(CommonUtils.getStringResource(
                R.string.add_new_product_for_configurable_attribute_title, mSourceCustomAttribute
                        .getMainLabel().toLowerCase()));
        // disable dialog from being automatically closed in case user tapped
        // outside the dialog
        result.setCanceledOnTouchOutside(false);
        return result;
    }

    /**
     * Copy the data from the existing downloaded product for passed SKU
     * 
     * @param urlType
     */
    void setDataFromTargetProduct(ProductDataType urlType) {
        switch (urlType) {
            case ATTRIBUTE:
                mNewProductCustomAttribute.setSelectedValue(mTargetProduct.getStringAttributeValue(
                        mNewProductCustomAttribute.getCode(), ""), true);
                break;
            case PRICE:
                // copy price from target prodcut
                mNewProductPriceHandler.setRegularPrice(mTargetProduct.getPrice());
                break;
            case SPECIAL_PRICE:
                // copy special price from target product
                mNewProductPriceHandler.setSpecialPrice(mTargetProduct.getSpecialPrice());
                mNewProductPriceHandler.setSpecialPriceDataFromProduct(mTargetProduct);
                break;
            case QUANTITY:
                // set the flag that quantity is copied from target product so
                // the information about isQtyDecimal will be taken from it on
                // save operation
                mQtyFromTargetProductCopied = true;
                // copy quantity from target product
                ProductUtils.setQuantityTextValueAndAdjustViewType(
                        CommonUtils.parseNumber(mTargetProduct.getQuantity(), 0d),
                        mNewProductQtyView, mTargetProduct.getIsQtyDecimal() == 1);
                break;
            default:
                break;
        }
    }

    /**
     * TextWatcher to monitor text updates and invalidate mSkuBelongsToExtraView
     * field to refresh link colors
     */
    class InvalidateSkuBelongsToExtraTextWatcher implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            mSkuBelongsToExtraView.invalidate();
        }

    }

    /**
     * The {@link CommonOnNewOptionTaskEventListener} with the extended
     * functionality when attribute creation is finished
     */
    class ExtendedCommonOnNewOptionTaskEventListener extends CommonOnNewOptionTaskEventListener {

        public ExtendedCommonOnNewOptionTaskEventListener() {
            super(mNewOptionCreationLoadingControl, (BaseFragmentActivity) getActivity());
        }
        
    }
    /**
     * Implementation of {@link AbstractCustomAttributeViewUtils}
     */
    class CustomAttributeViewUtils extends AbstractCustomAttributeViewUtils {

        public CustomAttributeViewUtils() {
            super(true, new ExtendedCommonOnNewOptionTaskEventListener(), 
                    Arrays.asList(mNewProductCustomAttribute), 
                    Integer.toString(mSourceProduct.getAttributeSetId()), getActivity());
        }
    }

    /**
     * Custom URL span converter to handle link clicks in the
     * mSkuBelongsToExtraView
     */
    class ProductDetailsURLSpanConverter implements
            RichTextUtils.SpanConverter<URLSpan, ProductDetailsClickableSpan> {
        @Override
        public ProductDetailsClickableSpan convert(URLSpan span) {
            return (new ProductDetailsClickableSpan(span.getURL()));
        }
    }

    /**
     * Enumeration contains data types for the product details copy URLs
     */
    enum ProductDataType {
        /**
         * Attribute URL
         */
        ATTRIBUTE("attr/"),
        /**
         * Price URL
         */
        PRICE("price/"),
        /**
         * Special price URL
         */
        SPECIAL_PRICE("specialPrice/"),
        /**
         * Quantity URL
         */
        QUANTITY("qty/");

        /**
         * The prefix for the URL
         */
        String mUrlPrefix;

        /**
         * @param urlPrefix for the URL
         */
        ProductDataType(String urlPrefix) {
            mUrlPrefix = urlPrefix;
        }

        /**
         * Get the URL prefix
         * 
         * @return
         */
        String getUrlPrefix() {
            return mUrlPrefix;
        }

        /**
         * Get the value from the URL
         * 
         * @param url
         * @return
         */
        String getValue(String url) {
            return url.substring(mUrlPrefix.length());
        }

        /**
         * Get the corresponding type for the URL
         * 
         * @param url
         * @return
         */
        public static ProductDataType getFromUrl(String url) {
            for (ProductDataType type : values()) {
                if (url.startsWith(type.getUrlPrefix())) {
                    // if url starts with the type prefix
                    return type;
                }
            }
            // no matching type is found
            return null;
        }
    }

    /**
     * URLSpan implementation to handle link clicks in the
     * mSkuBelongsToExtraView
     */
    class ProductDetailsClickableSpan extends URLSpan {
        /**
         * Default link color, initialized in the updateDrawState method on
         * first call
         */
        int mDefaultLinkColor = -1;

        /**
         * Related data type for the URLSpan url
         */
        ProductDataType mUrlType;

        public ProductDetailsClickableSpan(String url) {
            super(url);
            mUrlType = ProductDataType.getFromUrl(url);
        }

        @Override
        public void onClick(View widget) {
            // if there are no matching URL types use super logic
            if (mUrlType == null) {
                super.onClick(widget);
                return;
            }
            // user clicked on the link
            String url = getURL();
            setDataFromTargetProduct(mUrlType);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            // if there are no matching URL types interrupt invocation
            if (mUrlType == null) {
                return;
            }
            // initialize default link color if necessary
            if (mDefaultLinkColor == -1) {
                // if default link color is not yet initialized
                mDefaultLinkColor = ds.getColor();
            }
            String url = getURL();
            // target product attribute value for the type taken from link
            String value = mUrlType.getValue(url);
            Double linkValue = mUrlType == ProductDataType.ATTRIBUTE ? 
            		null // attribute value is not a number so can't be parsed
            		: CommonUtils.parseNumber(value);
            // flag whether the link should be marked red
            boolean markRed = false;
            switch (mUrlType) {
                case ATTRIBUTE: {
                    markRed = !TextUtils.equals(value,
                            mNewProductCustomAttribute.getSelectedValue());
                    break;
                }
                case PRICE: {
                    // compare the link price with the price specified in the
                    // price editing field
                    PricesInformation pricesInformation = ProductUtils
                            .getPricesInformation(mNewProductPriceHandler.getPriceView().getText()
                                    .toString());
                    markRed = !NumberUtils.equals(linkValue, pricesInformation.regularPrice);
                    break;
                }
                case SPECIAL_PRICE: {
                    // compare the link special price with the special price
                    // specified in the price editing field
                    PricesInformation pricesInformation = ProductUtils
                            .getPricesInformation(mNewProductPriceHandler.getPriceView().getText()
                                    .toString());
                    markRed = !NumberUtils.equals(linkValue, pricesInformation.specialPrice);
                    break;
                }
                case QUANTITY:
                    // compare the link quantity with the quantity
                    // specified in the quantity editing field
                    Double currentValue = CommonUtils.parseNumber(mNewProductQtyView.getText()
                            .toString());
                    markRed = !NumberUtils.equals(linkValue, currentValue);
                    break;
                default:
                    break;
            }
            if (markRed) {
                // if link marked to be red
                ds.setColor(getResources().getColor(R.color.red));
            } else {
                // reset link color to default value
                ds.setColor(mDefaultLinkColor);
            }
        }
    }

    /**
     * The asynchronous task to check SKU and load product details
     */
    public class LoadProductTaskAsync extends AbstractLoadProductTask {

        /**
         * Flag indicating whether the attribute set should be loaded in the
         * loadGeneral method
         */
        boolean mLoadAttributeSet = false;
        /**
         * The name of the target product attribute set
         */
        String mAttributeSetName;

        public LoadProductTaskAsync() {
            super(mSku, new SettingsSnapshot(getActivity()), mSkuCheckLoadingControl);
        }

        @Override
        protected void onSuccessPostExecute() {
            if (!isAdded()) {
                // if fragment was removed from the activity interrupt method
                // invocation to avoid various errors
                return;
            }
            mTargetProduct = getProduct();
            mSku = getSku();
            // product details loaded, so adjust visibility of various field and
            // show loaded product details
            mViewScannedProductBtn.setVisibility(View.VISIBLE);
            mSaveNoCheckBtn.setVisibility(View.GONE);
            mSaveBtn.setVisibility(View.VISIBLE);
            mSkuBelongsToExtraView.setMovementMethod(LinkMovementMethod.getInstance());
            mSkuBelongsToView.setText(Html.fromHtml(getString(R.string.sku_belongs_to,
                    getOriginalSku(), mTargetProduct.getName())));
            String attributeValue = mTargetProduct.getStringAttributeValue(
                    mNewProductCustomAttribute.getCode(), "");
            mSkuBelongsToExtraView
                    .setText(Html.fromHtml(
                    		getString(mTargetProduct.getSpecialPrice() == null ? 
                    				R.string.sku_belongs_to_without_special_price_extra
                                    : 
                                    R.string.sku_belongs_to_extra, 
                            mAttributeSetName,
                            attributeValue,
                            mNewProductCustomAttribute.getUserReadableSelectedValue(attributeValue),
                            ProductUtils.getQuantityString(mTargetProduct), 
                            mTargetProduct.getPrice(),
                            CommonUtils.appendCurrencySignToPriceIfNotEmpty(mTargetProduct.getPrice()),
                            CommonUtils.formatNumberIfNotNull(mTargetProduct.getSpecialPrice()),
                            CommonUtils.formatPrice(mTargetProduct.getSpecialPrice()))));
            mSkuBelongsToView.setVisibility(View.VISIBLE);
            mSkuBelongsToExtraView.setVisibility(View.VISIBLE);
            mSkuBelongsToExtraView.setText(RichTextUtils.replaceAll(
                    (Spanned) mSkuBelongsToExtraView.getText(), URLSpan.class,
                    new ProductDetailsURLSpanConverter()));
            
            if (!TextUtils.isEmpty(attributeValue)) {
                // if loaded product has proper attribute value then select it
                setDataFromTargetProduct(ProductDataType.ATTRIBUTE);
            }
            // copy values from the target product in case they were not modified
            Double quantity = CommonUtils.parseNumber(mNewProductQtyView.getText().toString());
            if (NumberUtils.equals(quantity, 1d)) {
                // if quantity was not modified (equals to 1 default quantity)
            	
                // set the quantity from the target product
                setDataFromTargetProduct(ProductDataType.QUANTITY);
            }
            PricesInformation pi = ProductUtils.getPricesInformation(mNewProductPriceHandler
                    .getPriceView().getText().toString());
            Double sourcePrice = CommonUtils.parseNumber(mSourceProduct.getPrice());
            if (NumberUtils.equals(pi.regularPrice, sourcePrice)) {
                // if price was not modified (equals to source product
                // price)

                // set the price from the target product
                setDataFromTargetProduct(ProductDataType.PRICE);
            }
            if (NumberUtils.equals(pi.specialPrice, mSourceProduct.getSpecialPrice())) {
                // if special price was not modified (equals to source product
                // special price)

                // set the special price from the target product
                setDataFromTargetProduct(ProductDataType.SPECIAL_PRICE);
            }

        }

        @Override
        protected void onFailedPostExecute() {
            super.onFailedPostExecute();
            if (!isAdded()) {
            	// if fragment was removed from the activity interrupt method
                // invocation to avoid various errors
                return;
            }
            // product details load failure
            if (isNotExists()) {
                // product doesn't exist on the server
                mSaveNoCheckBtn.setVisibility(View.GONE);
                mSaveBtn.setVisibility(View.VISIBLE);
                mProductWillBeCreatedMessageView.setVisibility(View.VISIBLE);
            } else {
                // some error occurred during loading operation
                mSkuBelongsToView.setVisibility(View.VISIBLE);
                mSkuBelongsToView.setText(getString(R.string.cannot_check_sku, getOriginalSku()));
            }
        }

        @Override
        public void extraLoadAfterProductIsLoaded() {
            super.extraLoadAfterProductIsLoaded();
            boolean loadResult = true;
            // to get the product attribute set name we need to load
            // attribute sets information
            if (!JobCacheManager.attributeSetsExist(settingsSnapshot.getUrl())) {
                // attribute set is not loaded, request load it from the
                // server.
                mLoadAttributeSet = true;
                loadResult = loadGeneral();
            }
            if (isCancelled()) {
                return;
            }
            if (loadResult) {
                List<Map<String, Object>> attributeSets = JobCacheManager
                        .restoreAttributeSets(settingsSnapshot.getUrl());
                // iterate through attribute sets and search for matched
                // attribute set id
                for (Map<String, Object> attributeSet : attributeSets) {
                    int attrSetId = JobCacheManager.safeParseInt(
                            attributeSet.get(MAGEKEY_ATTRIBUTE_SET_ID), INVALID_ATTRIBUTE_SET_ID);
                    if (attrSetId == getProduct().getAttributeSetId()) {
                        // init attribute set name and interrupt the
                        // loop
                        mAttributeSetName = (String) attributeSet.get(MAGEKEY_ATTRIBUTE_SET_NAME);
                        break;
                    }
                }
            }
        }

        @Override
        protected int requestLoadResource() {
            if (mLoadAttributeSet) {
                // the attribute set load is requested
                return resHelper.loadResource(MyApplication.getContext(),
                        RES_CATALOG_PRODUCT_ATTRIBUTES, settingsSnapshot);
            } else {
                return super.requestLoadResource();
            }
        }
    }

    /**
     * The loading control for the SKU check operation
     */
    class SkuCheckLoadingControl extends SimpleViewLoadingControl {
        TextView mMessageView;

        public SkuCheckLoadingControl(View view) {
            super(view);
            mMessageView = (TextView) view.findViewById(R.id.message);
        }

        @Override
        public void setViewVisibile(boolean visible) {
            super.setViewVisibile(visible);
            if (visible) {
                mMessageView.setText(getString(R.string.checking_for_existig_sku, mSku));
            }
        }

    }
}
