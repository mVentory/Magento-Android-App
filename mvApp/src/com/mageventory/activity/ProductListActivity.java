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

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.fragment.AbstractProductListFragment;
import com.mageventory.util.GuiUtils;
import com.mventory.R;

/**
 * Activity which is used to display product list information
 */
public class ProductListActivity extends BaseFragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new ProductListFragment()).commit();
        }
    }

    /**
     * Get the previously added instance of the {@link ProductListFragment}
     * 
     * @return
     */
    ProductListFragment getContentFragment() {
        return (ProductListFragment) getSupportFragmentManager().findFragmentById(
                R.id.content_frame);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            getContentFragment().loadProductList(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Extension of the {@link AbstractProductListFragment} with the
     * functionality related by the {@link ProductListActivity}
     */
    public static class ProductListFragment extends AbstractProductListFragment {

        @Override
        protected void onLoadFailureDialogCancel() {
            // finish activity in case product list load failed and user
            // selected to cancel operation
            getActivity().finish();
        }

        @Override
        protected void onProductPressed(ProductListItemInfo data, boolean longPressed) {
            // TODO y: use action
            // get product id and put it as intent extra
            String SKU;
            try {
                SKU = data.getSku();
            } catch (Throwable e) {
                GuiUtils.alert(getString(R.string.invalid_product_id));
                return;
            }

            final Intent intent;
            if (longPressed) {
                intent = new Intent(getActivity(), ProductEditActivity.class);
            } else {
                intent = new Intent(getActivity(), ScanActivity.class);
                // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }
            intent.putExtra(getString(R.string.ekey_product_sku), SKU);

            startActivityForResult(intent, REQ_EDIT_PRODUCT);
        }
        
    }
}
