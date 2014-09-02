
package com.mageventory.fragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
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
import com.mageventory.R;
import com.mageventory.activity.ConfigServerActivity;
import com.mageventory.activity.ProductCreateActivity;
import com.mageventory.activity.ProductEditActivity;
import com.mageventory.activity.ScanActivity;
import com.mageventory.fragment.base.BaseDialogFragment;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCacheManager.ProductDetailsExistResult;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributeSimple;
import com.mageventory.model.Product;
import com.mageventory.model.util.AbstractCustomAttributeViewUtils;
import com.mageventory.model.util.ProductUtils;
import com.mageventory.model.util.ProductUtils.PriceInputFieldHandler;
import com.mageventory.model.util.ProductUtils.PricesInformation;
import com.mageventory.res.LoadOperation;
import com.mageventory.resprocessor.ProductDetailsProcessor.ProductDetailsLoadException;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.AbstractSimpleLoadTask;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
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
     * The view to show the SKU belongs to information after the SKU existing
     * check
     */
    TextView mSkuBelongsToView;
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
    List<CustomAttribute> mSourceProductCustomAttributes;
    /**
     * The target product if found during SKU check operation
     */
    Product mTargetProduct;

    /**
     * The loading control for the SKU check operation
     */
    SkuCheckLoadingControl mSkuCheckLoadingControl;

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

        mSkuCheckLoadingControl = new SkuCheckLoadingControl(
                view.findViewById(R.id.skuCheckLoading));

        mSkuBelongsToView = (TextView) view.findViewById(R.id.skuBelongsTo);
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
        mSourcePriceView.setText(ProductUtils.getProductPricesString(mSourceProduct));
        mSourceQtyView.setText(ProductUtils.getQuantityString(mSourceProduct));

        // new product data
        CustomAttributeViewUtils customAttributeViewUtils = new CustomAttributeViewUtils();
        customAttributeViewUtils.initAtrEditView(newProducDetailsView, mNewProductCustomAttribute);
        mNewProductPriceHandler.setDataFromProduct(mSourceProduct);
        mNewProductPriceHandler.setPriceTextValue(null);
        mNewProductPriceHandler.getPriceView().setHint(mSourcePriceView.getText());
        ProductUtils.setQuantityTextValueAndAdjustViewType(1, mNewProductQtyView, mSourceProduct);
        mNewProductQtyView.setHint(mNewProductQtyView.getText());
        mNewProductQtyView.setText(null);

        // initialize buttons' handlers
        mViewScannedProductBtn.setOnClickListener(this);
        mCancelBtn.setOnClickListener(this);
        mSaveBtn.setOnClickListener(this);
        mSaveNoCheckBtn.setOnClickListener(this);

        // adjust views visibility
        mSkuCheckLoadingControl.setViewVisibile(false);
        mSkuBelongsToView.setVisibility(View.GONE);

        mViewScannedProductBtn.setVisibility(View.GONE);
        mSaveBtn.setVisibility(View.GONE);
        mSaveNoCheckBtn.setVisibility(View.VISIBLE);

        if (!TextUtils.isEmpty(mSku)) {
            // if SKU passed to the fragment is not empty run the SKU check
            new LoadProductTaskAsync().execute();
        } else {
            // SKU is not passed so nothing to check. Hide mSaveNoCheckBtn and
            // show mSaveBtn
            mSaveNoCheckBtn.setVisibility(View.GONE);
            mSaveBtn.setVisibility(View.VISIBLE);
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
        if (!GuiUtils.validateBasicTextData(R.string.pleaseSpecifyFirst, new String[] {
            mNewProductCustomAttribute.getSelectedValue()
        }, new String[] {
            mNewProductCustomAttribute.getMainLabel()
        }, null, false)) {
            // data validation failed
            return false;
        }
        // list of predefined custom attribute values which should be passed to
        // Create/Edit activities
        ArrayList<CustomAttributeSimple> predefinedCustomAttributeValues = new ArrayList<CustomAttributeSimple>();
        // add the selected custom attribute value
        predefinedCustomAttributeValues.add(CustomAttributeSimple.from(mNewProductCustomAttribute));

        // add the predefined price information
        String priceString = mNewProductPriceHandler.getPriceView().getText().toString();
        if (TextUtils.isEmpty(priceString)) {
            priceString = mNewProductPriceHandler.getPriceView().getHint().toString();
        }
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
        if (TextUtils.isEmpty(quantityText)) {
            // if quantity absent use the information stored in hint
            quantityText = mNewProductQtyView.getHint().toString();
        }
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
        // check whether the SKU is in the right format
        boolean barcodeScanned = !ScanActivity.isSKUInTheRightFormat(mSku);
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
            List<CustomAttribute> customAttributes) {
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
        return result;
    }

    /**
     * Implementation of {@link AbstractCustomAttributeViewUtils}
     */
    class CustomAttributeViewUtils extends AbstractCustomAttributeViewUtils {

        public CustomAttributeViewUtils() {
            super(false, getActivity());
        }

        @Override
        protected void setNameHint() {
        }
    }

    /**
     * Custom URL span converter to handle link clicks in the mSkuBelongsToView
     */
    class ProductDetailsURLSpanConverter implements
            RichTextUtils.SpanConverter<URLSpan, ProductDetailsClickableSpan> {
        @Override
        public ProductDetailsClickableSpan convert(URLSpan span) {
            return (new ProductDetailsClickableSpan(span.getURL()));
        }
    }

    /**
     * URLSpan implementation to handle link clicks in the mSkuBelongsToView
     */
    class ProductDetailsClickableSpan extends URLSpan {
        /**
         * Prefix for the price URL
         */
        String PRICE_PREFIX = "price/";
        /**
         * Prefix for the special price URL
         */
        String SPECIAL_PRICE_PREFIX = "specialPrice/";
        /**
         * Prefix for the quantity URL
         */
        String QTY_PREFIX = "qty/";
    
        public ProductDetailsClickableSpan(String url) {
            super(url);
        }
    
        @Override
        public void onClick(View widget) {
            // user clicked on the link
            String url = getURL();
            if (url.startsWith(PRICE_PREFIX)) {
                // if price link clicked
                String value = url.substring(PRICE_PREFIX.length());
                mNewProductPriceHandler.setRegularPrice(value);
            } else if (url.startsWith(SPECIAL_PRICE_PREFIX)) {
                // if special price link clicked
                String value = url.substring(SPECIAL_PRICE_PREFIX.length());
                mNewProductPriceHandler.setSpecialPrice(value);
                mNewProductPriceHandler.setSpecialPriceDataFromProduct(mTargetProduct);
            } else if (url.startsWith(QTY_PREFIX)) {
                // if quantity link clicked
                String value = url.substring(QTY_PREFIX.length());
                mQtyFromTargetProductCopied = true;
                ProductUtils.setQuantityTextValueAndAdjustViewType(
                        CommonUtils.parseNumber(value, 0d), mNewProductQtyView,
                        mTargetProduct.getIsQtyDecimal() == 1);
            }
        }
    }

    /**
     * The asynchronous task to check SKU and load product details
     */
    public class LoadProductTaskAsync extends AbstractSimpleLoadTask {

        boolean mNotExists = false;

        public LoadProductTaskAsync() {
            super(new SettingsSnapshot(getActivity()), mSkuCheckLoadingControl);
            CommonUtils.debug(TAG, "LoadProductTaskAsync.constructor");
        }

        @Override
        protected void onSuccessPostExecute() {
            // product details loaded, so adjust visibility of various field and
            // show loaded product details
            mViewScannedProductBtn.setVisibility(View.VISIBLE);
            mSaveNoCheckBtn.setVisibility(View.GONE);
            mSaveBtn.setVisibility(View.VISIBLE);
            mSkuBelongsToView.setMovementMethod(LinkMovementMethod.getInstance());
            mSkuBelongsToView.setText(Html.fromHtml(getString(
                    R.string.sku_belongs_to,
                    mTargetProduct.getSku(),
                    mTargetProduct.getName(),
                    ProductUtils.getQuantityString(mTargetProduct),
                    mTargetProduct.getPrice(),
                    mTargetProduct.getSpecialPrice() == null ? "" : CommonUtils
                            .formatNumber(mTargetProduct.getSpecialPrice()))));
            mSkuBelongsToView.setVisibility(View.VISIBLE);
            mSkuBelongsToView.setText(RichTextUtils.replaceAll(
                    (Spanned) mSkuBelongsToView.getText(), URLSpan.class, new ProductDetailsURLSpanConverter()));

        }

        @Override
        protected void onFailedPostExecute() {
            super.onFailedPostExecute();
            // product details load failure
            if (mNotExists) {
                // product doesn't exist on the server
                mSaveNoCheckBtn.setVisibility(View.GONE);
                mSaveBtn.setVisibility(View.VISIBLE);
            } else {
                // some error occurred during loading operation
                mSkuBelongsToView.setVisibility(View.VISIBLE);
                mSkuBelongsToView.setText(getString(R.string.cannot_check_sku, mSku));
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                CommonUtils.debug(TAG, "LoadProductTaskAsync.doInBackground executing");
                boolean loadResult = true;
                // check product already present in local cache
                ProductDetailsExistResult existResult = JobCacheManager.productDetailsExist(mSku,
                        settingsSnapshot.getUrl(), true);
                if (!existResult.isExisting()) {
                    // product doesn't exist in local cache, load from server
                    loadResult = loadGeneral();
                } else {
                    mSku = existResult.getSku();
                }
                if (loadResult) {
                    mTargetProduct = JobCacheManager.restoreProductDetails(mSku,
                            settingsSnapshot.getUrl());
                }
                CommonUtils.debug(TAG, "LoadAttributeSetTaskAsync.doInBackground completed");
                return !isCancelled() && mTargetProduct != null;
            } catch (Exception ex) {
                CommonUtils.error(ConfigServerActivity.TAG, ex);
            }
            return false;
        }

        @Override
        protected int requestLoadResource() {
            final String[] params = new String[2];
            params[0] = GET_PRODUCT_BY_SKU;
            params[1] = mSku;
            return resHelper.loadResource(getActivity(), RES_PRODUCT_DETAILS, params,
                    settingsSnapshot);
        }

        @Override
        public void onLoadOperationCompleted(LoadOperation op) {
            super.onLoadOperationCompleted(op);
            if (op.getOperationRequestId() == requestId) {
                // check whether any exception occurred during loading
                ProductDetailsLoadException exception = (ProductDetailsLoadException) op
                        .getException();
                if (exception != null
                        && exception.getFaultCode() == ProductDetailsLoadException.ERROR_CODE_PRODUCT_DOESNT_EXIST) {
                    // product doesn't exist on the server, set the mNotExists
                    // flag
                    mNotExists = true;
                }
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
