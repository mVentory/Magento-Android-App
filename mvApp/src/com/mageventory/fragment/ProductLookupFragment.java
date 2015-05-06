
package com.mageventory.fragment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;

import com.mageventory.activity.ScanActivity;
import com.mageventory.activity.ScanActivity.CheckSkuResult;
import com.mageventory.fragment.base.BaseDialogFragment;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ScanUtils;
import com.mageventory.widget.SearchWordsSet;
import com.mventory.R;

/**
 * Dialog fragment used to display product lookup options. The functionality is
 * used in various places such as "Add product for configurable attribute" in
 * the "Product details" and "Copy attribute value from another product" in the
 * Product Edit/Create activities
 * 
 * @author Eugene Popovich
 */
public class ProductLookupFragment extends BaseDialogFragment {

    /**
     * Tag used for logging
     */
    static final String TAG = ProductLookupFragment.class.getSimpleName();
    
    /**
     * Possible lookup options supported by the fragment
     */
    public enum LookupOption {
        /**
         * Search product in the product list option
         */
        SEARCH(R.id.searchButton),
        /**
         * Use new product (empty SKU)
         */
        NEW(R.id.createNewButton),
        /**
         * Scan product SKU via scanner
         */
        SCAN(R.id.scanButton);
        /**
         * The related to the option button view ID
         */
        int mViewId;

        /**
         * @param viewId The related to the option button view ID
         */
        LookupOption(int viewId) {
            mViewId = viewId;
        }

        /**
         * Get the related to the option button view ID
         * 
         * @return
         */
        public int getViewId() {
            return mViewId;
        }
    }

    /**
     * The request code used for launching scanner activity for the result
     */
    private static final int SCAN_CODE = 1000;

    /**
     * The components set used to manage search words options
     */
    SearchWordsSet mSearchWordsSet = new SearchWordsSet();
    /**
     * The listener for the product SKU selected event
     */
    OnProductSkuSelectedListener mListener;

    /**
     * The collection of the allowed {@link LookupOption}'s specified in the
     * {@link #setData(String, String, OnProductSkuSelectedListener, LookupOption...)}
     * method
     */
    Set<LookupOption> mPossibleOptions;

    /**
     * Set the data which should be used by fragment
     * 
     * @param query the actual query which is used for search
     * @param originalQuery the original query which contains all possible words
     *            which may be used for search
     * @param listener the listener for the product SKU selected event
     * @param possibleOptions the collection of the allowed
     *            {@link LookupOption}'s
     */
    public void setData(String query, String originalQuery, OnProductSkuSelectedListener listener,
            LookupOption... possibleOptions) {
        // pass the required data to the search words set
        mSearchWordsSet.setData(query, originalQuery);
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        mListener = listener;
        if (possibleOptions == null || possibleOptions.length == 0) {
            throw new IllegalArgumentException("Possible options can't be empty");
        }
        mPossibleOptions = new HashSet<LookupOption>(Arrays.asList(possibleOptions));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.product_search_options, container);
        init(view, savedInstanceState);
        return view;
    }

    /**
     * Initialize the view
     * 
     * @param view
     * @param savedInstanceState
     */
    void init(View view, Bundle savedInstanceState) {
        mSearchWordsSet.init(view, savedInstanceState, getActivity());
        // the common options on click listener
        OnClickListener commonListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (LookupOption.SEARCH.getViewId() == v.getId()) {
                    // if SEARCH option is selected
                    searchSku();
                } else if (LookupOption.NEW.getViewId() == v.getId()) {
                    // if NEW option is selected
                    mListener.onProductSkuSelected(null);
                    closeDialog();
                } else if (LookupOption.SCAN.getViewId() == v.getId()) {
                    // if SCAN option is selected
                    scanSku();
                }
            }
        };
        // iterate through all lookup options and initialize corresponding views
        for (LookupOption option : LookupOption.values()) {
            // get the related view
            View optionView = view.findViewById(option.getViewId());
            // adjust visibility depend on whether the option is present in the
            // mPossibleOptions set
            optionView.setVisibility(mPossibleOptions.contains(option) ? View.VISIBLE : View.GONE);
            optionView.setOnClickListener(commonListener);
        }

    }

    /**
     * Scan the SKU through the code scanning application
     */
    void scanSku() {
        // initiate scan for the product which should be used
        ScanUtils.startScanActivityForResult(getActivity(), ProductLookupFragment.this, SCAN_CODE,
                R.string.scan_barcode_or_qr_label);
    }

    /**
     * Search the SKU in the product list with the selected words filter
     */
    void searchSku() {
        ProductListFragment fragment = new ProductListFragment();
        // pass the name filter as argument
        Bundle args = new Bundle();
        args.putString(ProductListFragment.EXTRA_NAME_FILTER,
                TextUtils.join(" ", mSearchWordsSet.getSelectedWords()));
        fragment.setArguments(args);

        // set the additional required fragment data
        fragment.setData(new OnProductSkuSelectedListener() {

            @Override
            public void onProductSkuSelected(String sku) {
                // pass the event to the parent listener and close the dialog
                mListener.onProductSkuSelected(sku);
                closeDialog();
            }
        });
        fragment.show(getFragmentManager(), fragment.getClass().getSimpleName());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SCAN_CODE:
                // returned from the scanner
                final CheckSkuResult checkSkuResult;
                if (resultCode == Activity.RESULT_OK) {
                    checkSkuResult = ScanActivity.checkSku(data);
                } else {
                    checkSkuResult = null;
                }
                if (checkSkuResult != null) {
                    // if there is a valid scanned result pass the event to the
                    // listener and close dialog
                    mListener.onProductSkuSelected(checkSkuResult.code);
                    closeDialog();
                }
                break;
            default:
                break;
        }
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // remove title from the dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    /**
     * The interface to implement listener for the SKU selected event
     */
    public static interface OnProductSkuSelectedListener {
        /**
         * Called when the user selected the SKU either via the Scan or the
         * Product List
         * 
         * @param sku the selected SKU
         */
        public void onProductSkuSelected(String sku);
    }

    /**
     * Extension of the {@link AbstractProductListFragment} with the
     * functionality related by the {@link ProductLookupFragment}
     */
    static class ProductListFragment extends AbstractProductListFragment {

        /**
         * The listener for the product SKU selected event
         */
        OnProductSkuSelectedListener mListener;
        
        /**
         * @param listener The listener for the product SKU selected event
         */
        public void setData(OnProductSkuSelectedListener listener){
            if (listener == null) {
                throw new IllegalArgumentException("Listener cannot be null");
            }
            mListener = listener;
        }
            
        @Override
        protected void onLoadFailureDialogCancel() {
            // close dialog in case product list load failed and user canceled
            // action
            closeDialog();
        }

        @Override
        protected void onProductPressed(ProductListItemInfo data, boolean longPressed) {
            if (data == null) {
                // if invalid data is selected
                GuiUtils.alert(getString(R.string.invalid_product_id));
            } else
            {
                // pass the event to the listener and close the dialog
                mListener.onProductSkuSelected(data.getSku());
                closeDialog();
            }
        }

    }
}
