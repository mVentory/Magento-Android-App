package com.mageventory.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.mageventory.util.GuiUtils;
import com.mventory.R;

/**
 * Extension of the {@link AbstractProductListFragment} with the functionality
 * related to the {@link ProductLookupFragment} and other placed where the
 * product selection dialog for the result is used
 */
public class ProductListDialogFragment extends AbstractProductListFragment {

    /**
     * The key used for the SKU intent extra. Intent is returned to the
     * {@link Fragment#onActivityResult(int, int, Intent)} method of the target
     * fragment
     */
    public static final String EXTRA_SKU = "SKU";
        
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (getTargetFragment() == null) {
            throw new IllegalStateException("Target fragment can't be empty");
        }
    }

    @Override
    protected void onLoadFailureDialogCancel() {
        // fire cancelled result and close dialog in case product list load
        // failed and user canceled
        // action
        getTargetFragment()
                .onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
        closeDialog();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        // fire cancelled result
        getTargetFragment()
                .onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
    }

    @Override
    protected void onProductPressed(ProductListItemInfo data, boolean longPressed) {
        if (data == null) {
            // if invalid data is selected
            GuiUtils.alert(getString(R.string.invalid_product_id));
        } else
        {
            if (disabledProductIds != null && disabledProductIds.contains(data.getId())) {
                // if disabled product is selected
                GuiUtils.alert(R.string.disabled_product_selected_hint);
            } else {
                // send the data to the onActivityResult method of the target
                // fragment
                Intent intent = new Intent();
                intent.putExtra(EXTRA_SKU, data.getSku());
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK,
                        intent);
                closeDialog();
            }
        }
    }

}