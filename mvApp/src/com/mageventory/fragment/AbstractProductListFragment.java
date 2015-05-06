
package com.mageventory.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.bitmapfun.util.ImageCache;
import com.mageventory.bitmapfun.util.ImageFetcher;
import com.mageventory.components.ImagePreviewLayout;
import com.mageventory.fragment.base.BaseDialogFragmentWithImageWorker;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.model.Product;
import com.mageventory.model.Product.SiblingInfo;
import com.mageventory.model.Product.imageInfo;
import com.mageventory.model.util.ProductUtils;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.AbstractLoadProductListTask;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.util.EventBusUtils.GeneralBroadcastEventHandler;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.SimpleViewLoadingControl;
import com.mventory.R;

/**
 * The abstract dialog fragment with the common product list loading and
 * displaying functionality. Can be embedded into activity as regular fragment.
 */
public abstract class AbstractProductListFragment extends BaseDialogFragmentWithImageWorker
        implements GeneralBroadcastEventHandler,
        MageventoryConstants {

    /**
     * Tag used for logging
     */
    private static final String TAG = AbstractProductListFragment.class.getSimpleName();
    /**
     * The key for the custom text attributes intent extra
     */
    public static final String EXTRA_NAME_FILTER = "NAME_FILTER";

    private String nameFilter = "";
    private EditText nameFilterEdit;
    /**
     * The last started load products task
     */
    private LoadProductsTask mLoadProductsTask;
    private int selectedItemPos = ListView.INVALID_POSITION;
    /**
     * Whether the data should be reloaded when activity is resumed
     */
    private boolean mRefreshOnResume = false;

    /**
     * The list view to diplay list of products
     */
    ListView mListView;
    /**
     * The loading control to indicate product list loading process
     */
    LoadingControl mProductsLoadingControl;
    /**
     * The view to indicate empty loaded products list
     */
    View mNoProductsIndicator;

    /**
     * The dimension of the thumbnail images in the list
     */
    int mThumbnailSize;

    /**
     * Display the product list data
     * 
     * @param data the loaded product list data
     */
    public void displayData(List<ProductListItemInfo> data) {
        // if call is successful but there are no products to list
        final ListAdapter adapter;
        if (data.size() == 0) {
            adapter = null;
            // show empty products list inidicator
            mNoProductsIndicator.setVisibility(View.VISIBLE);
        } else {
            // hide empty products list inidicator
            mNoProductsIndicator.setVisibility(View.GONE);
            adapter = new ProductListItemInfoAdapter(data);
        }
        setListAdapter(adapter);
        if (selectedItemPos != ListView.INVALID_POSITION) {
            getListView().setSelectionFromTop(selectedItemPos, 0);
        }
    }

    private void emptyList() {
        emptyList(false);
    }

    private void emptyList(final boolean displayPlaceholder) {
        setListAdapter(null);
        mNoProductsIndicator.setVisibility(displayPlaceholder ? View.VISIBLE : View.GONE);
    }

    public String getNameFilter() {
        return nameFilter;
    }

    private void hideSoftKeyboard() {
        nameFilterEdit.clearFocus();
        InputMethodManager m = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        m.hideSoftInputFromWindow(nameFilterEdit.getWindowToken(), 0);
    }

    private void loadProductList() {
        loadProductList(false);
    }

    public void loadProductList(final boolean forceReload) {
        loadProductList(getNameFilter(), forceReload);
    }

    private void loadProductList(final String nameFilter, final boolean forceReload) {
        CommonUtils.debug(TAG, false, "loadProductList(" + nameFilter + ", " + forceReload + ");");

        if (mLoadProductsTask != null && !mLoadProductsTask.isFinished()) {
            // if there is an active load products list task
            if (mLoadProductsTask.isForceReload() == forceReload
                    && TextUtils.equals(mLoadProductsTask.getNameFilter(), nameFilter)) {
                // same loading operation is already active.
                CommonUtils.debug(TAG, "Same loading operation is already active, skipping...");
                return;
            }
            // cancel current loading task
            mLoadProductsTask.cancel(true);
        }
        hideSoftKeyboard();
        emptyList();
        mLoadProductsTask = new LoadProductsTask(nameFilter, forceReload, new SettingsSnapshot(
                getActivity().getApplicationContext()), mProductsLoadingControl);
        mLoadProductsTask.execute();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        initImageWorker();
        EventBusUtils.registerOnGeneralEventBroadcastReceiver(TAG, this, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.product_list, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // initialize swipe to refresh layout
        final SwipeRefreshLayout refreshController = (SwipeRefreshLayout) view.findViewById(R.id.refreshController);
        refreshController.setColorSchemeResources(R.color.blue);
        refreshController.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadProductList(true);
            }
        });

        mProductsLoadingControl = new SimpleViewLoadingControl(
                view.findViewById(R.id.productsLoadingIndicator)) {
            @Override
            public void setViewVisibile(boolean visible) {
                super.setViewVisibile(visible);
                if (!visible) {
                    // hide refresh controller refreshing indicator if visible
                    refreshController.setRefreshing(false);
                }
            };
        };
        mNoProductsIndicator = view.findViewById(R.id.noProductsIndicator);
        mListView = (ListView) view.findViewById(android.R.id.list);
        // initialize
        if (savedInstanceState != null) {
            setNameFilter(savedInstanceState.getString(EXTRA_NAME_FILTER));
            selectedItemPos = savedInstanceState.getInt(getString(R.string.ekey_selected_item_pos),
                    ListView.INVALID_POSITION);
        } else if (getArguments() != null) {
            setNameFilter(getArguments().getString(EXTRA_NAME_FILTER));
        }


        // set on list item click listener
        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ProductListItemInfo data = (ProductListItemInfo) parent.getAdapter()
                        .getItem(position);
                onProductPressed(data, false);
            }
        });
        // set on list item long click listener
        getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View arg1, int position, long arg3) {
                ProductListItemInfo data = (ProductListItemInfo) parent.getAdapter()
                        .getItem(position);
                onProductPressed(data, true);
                return true;
            }
        });

        // initialize filtering
        nameFilterEdit = (EditText) view.findViewById(R.id.filter_query);
        nameFilterEdit.setText(getNameFilter());

        nameFilterEdit.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    String nameFilter = "" + nameFilterEdit.getText();
                    setNameFilter(nameFilter);
                    loadProductList();

                    return true;
                }

                return false;
            }
        });

        loadProductList();
    }

    /**
     * Initialize image worker
     */
    protected void initImageWorker() {
        mThumbnailSize = getResources().getDimensionPixelSize(
                R.dimen.product_list_thumbnail_size);
        imageWorker = new ImageFetcher(getActivity(), null, mThumbnailSize);
        imageWorker.setLoadingImage(R.drawable.empty_photo);
        imageWorker
                .setImageCache(ImageCache.findOrCreateCache(getActivity(), TAG, 0, false, false));
    }

    protected void showLoadFailureDialog() {
        // build dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final AlertDialog dialog = builder.create();

        // set title
        dialog.setTitle(getString(R.string.data_load_failure));

        // set message
        final StringBuilder message = new StringBuilder(64);
        message.append(getString(R.string.check_your_internet_retry));
        dialog.setMessage(message.toString());

        // set buttons
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.try_again),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadProductList();
                    }
                });
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onLoadFailureDialogCancel();
                    }
                });

        dialog.show();
    }

    protected abstract void onLoadFailureDialogCancel();
    
    protected abstract void onProductPressed(final ProductListItemInfo data,
            final boolean longPressed);

    @Override
    public void onResume() {
        super.onResume();
        // if there is a scheduled refresh operation
        if (mRefreshOnResume) {
            mRefreshOnResume = false;
            loadProductList(true);
            return;
        }
    }

    // following 2 methods enable the default options menu

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_NAME_FILTER, getNameFilter());
        outState.putInt(getString(R.string.ekey_selected_item_pos), getListView()
                .getFirstVisiblePosition());
    }

    private void setNameFilter(String s) {
        nameFilter = s;
    }

    /**
     * Set the list adapter to the activity ListView
     * 
     * @param adapter the adapter to set
     */
    protected void setListAdapter(ListAdapter adapter) {
        mListView.setAdapter(adapter);
    }

    /**
     * Get the activity ListView
     * 
     * @return
     */
    protected ListView getListView() {
        return mListView;
    }

    @Override
    public void onGeneralBroadcastEvent(EventType eventType, Intent extra) {
        switch (eventType) {
            case PRODUCT_DELETED: {
                CommonUtils.debug(TAG, "onGeneralBroadcastEvent: received product deleted event");
                // if activity is resumed refresh immediately. Otherwise
                // schedule refresh operation
                if (isResumed()) {
                    loadProductList(true);
                } else {
                    mRefreshOnResume = true;
                }
            }
                break;
            default:
                break;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog result = super.onCreateDialog(savedInstanceState);
        result.setTitle(R.string.activity_product_list_name);
        result.setCanceledOnTouchOutside(true);
        return result;
    }

    /**
     * Product information wrapper to use in the products list
     */
    protected static class ProductListItemInfo extends SiblingInfo {
        private static final long serialVersionUID = 1L;

        /**
         * The list of the configurable attributes associated with the product
         * attribute set
         */
        List<CustomAttribute> configurableAttributes;
        /**
         * The product quantity
         */
        Double parsedQuantity;
        /**
         * Whether the product quantity is in decimal format
         */
        boolean isQuantityDecimal;
        /**
         * Whether the product is sibling of another product
         */
        boolean isSibling;
        /**
         * The list of associated siblings products
         */
        List<ProductListItemInfo> siblings = new ArrayList<ProductListItemInfo>();
        /**
         * The main image information
         */
        imageInfo mainImage;

        /**
         * @param isSibling Whether the product is sibling of another product
         * @param data the raw API data with the product information
         * @param configurableAttributes The list of the configurable attributes
         *            associated with the product attribute set
         */
        @SuppressWarnings("unchecked")
        public ProductListItemInfo(boolean isSibling, Map<String, Object> data,
                List<CustomAttribute> configurableAttributes) {
            super(data);
            this.isSibling = isSibling;
            this.configurableAttributes = configurableAttributes;
            // initialize stock information
            Map<String, Object> stockData = (Map<String, Object>) data.get(MAGEKEY_PRODUCT_STOCK);
            if (stockData != null) {
                // if stock data is available
                parsedQuantity = Product.safeParseDouble(stockData, MAGEKEY_PRODUCT_QUANTITY, null);
                isQuantityDecimal = Product.safeParseInt(stockData, MAGEKEY_PRODUCT_IS_QTY_DECIMAL) == 1;
            }
            // initialize product siblings
            Object[] siblingsData = JobCacheManager.getObjectArrayWithMapCompatibility(data
                    .get(MAGEKEY_PRODUCT_SIBLINGS));
            if (siblingsData != null) {
                // if siblings data is available
                for (int i = 0, size = siblingsData.length; i < size; i++) {
                    // iterate through siblings data and initialize siblings
                    // list
                    Map<String, Object> siblingInfoMap = (Map<String, Object>) siblingsData[i];
                    ProductListItemInfo siblingInfo = new ProductListItemInfo(true, siblingInfoMap,
                            configurableAttributes);
                    siblings.add(siblingInfo);
                }
            }
            // initialize main image information
            Object[] local_images = JobCacheManager.getObjectArrayFromDeserializedItem(data
                    .get(MAGEKEY_PRODUCT_IMAGES));

            if (local_images != null) {
                // if images information is present
                for (int i = 0; i < local_images.length; i++) {
                    // iterate through images information and find the main
                    // image
                    imageInfo info = new imageInfo((Map<String, Object>) local_images[i]);
                    if (info.getMain()) {
                        mainImage = info;
                        break;
                    }
                }
            }
        }
    }

    /**
     * The product information adapter to be used in the products list
     */
    class ProductListItemInfoAdapter extends BaseAdapter {
        /**
         * The list of products to display
         */
        List<ProductListItemInfo> mProducts;
        /**
         * The layout inflater to use in the getView method
         */
        LayoutInflater mInflater;
        /**
         * The settings snapshot
         */
        SettingsSnapshot mSettings;

        /**
         * @param products The list of products to display
         */
        ProductListItemInfoAdapter(List<ProductListItemInfo> products) {
            mProducts = products;
            mInflater = LayoutInflater.from(getActivity());
            mSettings = new SettingsSnapshot(getActivity().getApplicationContext());
        }

        @Override
        public int getCount() {
            return mProducts.size();
        }

        @Override
        public ProductListItemInfo getItem(int position) {
            return mProducts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // View holder pattern implementtation
            ViewHolder vh;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.product_list_item, parent, false);
                vh = new ViewHolder();
                vh.siblingsIndicator = convertView.findViewById(R.id.childIndicator);
                vh.imageView = (ImageView) convertView.findViewById(R.id.image);
                vh.productName = (TextView) convertView.findViewById(R.id.productName);
                vh.productDescription = (TextView) convertView
                        .findViewById(R.id.productDescription);
                vh.configurableAttributeValue = (TextView) convertView
                        .findViewById(R.id.configurableAttributeValue);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            ProductListItemInfo item = getItem(position);
            // siblings indicator shouls be visible only for items marked with
            // the isSibling flag
            vh.siblingsIndicator.setVisibility(item.isSibling ? View.VISIBLE : View.GONE);
            vh.productName.setText(item.getName());
            // fill product description
            if (item.getPrice() == null) {
                vh.productDescription.setText(item.getSku());
            } else {
                vh.productDescription.setText(Html.fromHtml(CommonUtils.getStringResource(
                        item.getSpecialPrice() == null ?
                        // use different string resoruces depend on whether the
                        // speical
                        // price is available
                        R.string.product_list_item_description
                                : R.string.product_list_item_description_with_special_price,
                        CommonUtils.formatPrice(item.getPrice()),
                        item.parsedQuantity == null ? "" : ProductUtils.getQuantityString(
                                item.isQuantityDecimal, item.parsedQuantity), item.getSku(),
                        CommonUtils.formatPrice(item.getSpecialPrice()))));
            }
            // get the configurable attributes value
            StringBuilder configurableAttributeValues = new StringBuilder();
            for (CustomAttribute attribute : item.configurableAttributes) {
                String value = item.getAttributeValue(attribute.getCode());
                if (!TextUtils.isEmpty(value)) {
                    value = attribute.getUserReadableSelectedValue(value);
                }
                if (!TextUtils.isEmpty(value)) {
                    if (configurableAttributeValues.length() > 0) {
                        // append separator if there are more than on
                        // configurable attribute values
                        configurableAttributeValues.append(", ");
                    }
                    configurableAttributeValues.append(value);
                }
            }
            vh.configurableAttributeValue.setText(configurableAttributeValues.toString());
            vh.configurableAttributeValue
                    .setVisibility(configurableAttributeValues.length() > 0 ? View.VISIBLE
                            : View.GONE);
            if (item.mainImage == null) {
                // if main image information is absent
                imageWorker.loadImage(null, vh.imageView);
            } else {
                // load main image thumbnail
                imageWorker.loadImage(
                        ImagePreviewLayout.getUrlForResizedImage(item.mainImage.getImgURL(),
                                mSettings, mThumbnailSize), vh.imageView);
            }
            return convertView;
        }

        /**
         * Class for the view holder pattern implementation
         */
        class ViewHolder {
            /**
             * The view which indicates siblings products
             */
            View siblingsIndicator;
            /**
             * The image view to display product main image thumbnail
             */
            ImageView imageView;
            /**
             * The view to display product name
             */
            TextView productName;
            /**
             * The view to display product description
             */
            TextView productDescription;
            /**
             * The view to display configurable attribute value
             */
            TextView configurableAttributeValue;
        }
    }

    /**
     * Asynchronous task to load and display product list
     */
    class LoadProductsTask extends AbstractLoadProductListTask {

        /**
         * Flag indicating whether the attribute set should be loaded in the
         * loadGeneral method
         */
        boolean mLoadAttributeSet = false;
        /**
         * Flag indicating whether the attribute list successfully loaded
         */
        boolean mAttributeListLoadResult = false;

        /**
         * The loaded products information
         */
        List<ProductListItemInfo> mProducts = new ArrayList<ProductListItemInfo>();

        /**
         * Flag indicating whether the attribute set request is performed for
         * the first time for the task
         */
        boolean mFirstTimeAttributeSetRequest = true;

        /**
         * @param nameFilter the name filter to be used for the product list API
         *            call
         * @param forceReload whether the cached data should be forced to reload
         * @param settings the settings snapshot
         * @param loadingControl the loading control to be used to indicate task
         *            activity
         */
        public LoadProductsTask(String nameFilter, boolean forceReload, SettingsSnapshot settings,
                LoadingControl loadingControl) {
            super(nameFilter, forceReload, settings, loadingControl);
        }

        @Override
        protected void onSuccessPostExecute() {
            if (!isActivityAlive()) {
                // if activity was destroyed interrupt method
                // invocation to avoid various errors
                return;
            }
            displayData(mProducts);

        }

        @Override
        protected void onFailedPostExecute() {
            super.onFailedPostExecute();
            if (!isActivityAlive()) {
                // if activity was destroyed interrupt method
                // invocation to avoid various errors
                return;
            }
            if (!isCancelled()) {
                showLoadFailureDialog();
            }
        }

        @Override
        public boolean extraLoadAfterProductListIsLoaded() {
            super.extraLoadAfterProductListIsLoaded();
            // process the loaded product list raw API data and fill the
            // mProducts list
            List<Map<String, Object>> data = getData();
            // configurable attributes information cache
            Map<Integer, List<CustomAttribute>> configurableAttributesCache = new HashMap<Integer, List<CustomAttribute>>();
            // set to store already processed product ids to avoid duplicate
            // appearance
            Set<String> processedProducts = new HashSet<String>();
            for (Map<String, Object> item : data) {
                int attributeSetId = Product.safeParseInt(item, MAGEKEY_PRODUCT_ATTRIBUTE_SET_ID);
                // get the attribute list information
                List<Map<String, Object>> attributesData = getAttributeList(attributeSetId);
                // check whether the configurable attributes information is
                // present in the cache
                List<CustomAttribute> configurableAttributes = configurableAttributesCache
                        .get(attributeSetId);
                if (configurableAttributes == null) {
                    if (attributesData == null) {
                        // if attributes data doesn't exist in the cache
                        if (mAttributeListLoadResult) {
                            // if data successfully reloaded but attribute set
                            // data doesn't exist on the server anymore
                            CommonUtils.warn(TAG,
                                    "Missing attribute set data %1$d in profile %2$s",
                                    attributeSetId, settingsSnapshot.getUrl());
                            configurableAttributes = Collections.emptyList();
                        } else {
                            // attribute list load request failed, return false
                            // so user may try again
                            return false;
                        }
                    } else {
                        // if configurable attributes information is missing in
                        // the cache
                        configurableAttributes = initConfigurableAttributes(attributesData);
                        // put initialized data to cache for future re-use
                        configurableAttributesCache.put(attributeSetId, configurableAttributes);
                    }
                }
                ProductListItemInfo productInfo = new ProductListItemInfo(false, item,
                        configurableAttributes);
                if (!processedProducts.contains(productInfo.getId())) {
                    // if product information is missing in the mProducts list
                    mProducts.add(productInfo);
                    // add product id to the processed products list for future
                    // checks
                    processedProducts.add(productInfo.getId());
                }
                // process product siblings and add them as a separate items to
                // the mProducts list
                for (ProductListItemInfo sibling : productInfo.siblings) {
                    if (!processedProducts.contains(sibling.getId())) {
                        // if sibling is absent in the mProducts list
                        mProducts.add(sibling);
                        // add sibling product id to the processed products list
                        // for future checks
                        processedProducts.add(sibling.getId());
                    }
                }

            }
            return true;
        }

        /**
         * Initialize configurable attributes information from the attributes
         * list raw API output
         * 
         * @param attributesData the raw API output containing attributes list
         *            information
         * @return list of configurable attributes
         */
        public List<CustomAttribute> initConfigurableAttributes(
                List<Map<String, Object>> attributesData) {
            List<CustomAttribute> configurableAttributes;
            configurableAttributes = new ArrayList<CustomAttribute>();
            for (Map<String, Object> attributeData : attributesData) {
                // iterate through raw API attributes list information
                CustomAttribute attribute = CustomAttributesList.createCustomAttribute(
                        attributeData, null);
                if (attribute.isConfigurable()) {
                    // if attribute is configurable
                    configurableAttributes.add(attribute);
                }
            }
            return configurableAttributes;
        }

        /**
         * Get the raw attributes list information for the specified attribute
         * set id
         * 
         * @param attributeSetId the attribute set id to retrieve attribute list
         *            information for
         * @return raw attributes list information
         */
        List<Map<String, Object>> getAttributeList(int attributeSetId) {
            boolean attributeListExists = true;
            // check whether the attribute list is present in the cache
            if (!JobCacheManager.attributeListExist(Integer.toString(attributeSetId),
                    settingsSnapshot.getUrl())) {
                // attribute list is not loaded, request load it from the
                // server.
                mLoadAttributeSet = true;
                attributeListExists = mFirstTimeAttributeSetRequest ? loadGeneral() : false;
                if (mFirstTimeAttributeSetRequest) {
                    mAttributeListLoadResult = attributeListExists;
                    mFirstTimeAttributeSetRequest = false;
                }
            }
            if (isCancelled()) {
                // if task was cancelled return
                return null;
            }
            List<Map<String, Object>> attributeList = null;
            if (attributeListExists) {
                // load attribute list information from cache
                attributeList = JobCacheManager.restoreAttributeList(
                        Integer.toString(attributeSetId), settingsSnapshot.getUrl());
            }
            return attributeList;
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
}
