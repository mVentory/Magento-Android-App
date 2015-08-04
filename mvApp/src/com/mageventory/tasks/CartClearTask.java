package com.mageventory.tasks;

import java.lang.ref.WeakReference;

import com.mageventory.activity.OrderListActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.LoadingControl;
import com.mventory.R;

/**
 * Asynchronous task to perform clear cart operaion. Used in the
 * {@link OrderListActivity}
 * 
 * @author Eugene Popovich
 */
public class CartClearTask extends AbstractSimpleLoadTask {
    /**
     * Tag used for logging
     */
    static String TAG = CartClearTask.class.getSimpleName();
    /**
     * Weak reference to the starting {@link OrderListActivity}
     */
    WeakReference<OrderListActivity> mActivityInstance;
    /**
     * The returned cart items information after the cart was successfully
     * cleared
     */
    Object[] mCartItems;

    /**
     * @param instance the calling {@link OrderListActivity}
     * @param loadingControl the loading control which should be used to
     *            indicate resource loading process
     */
    public CartClearTask(OrderListActivity instance, LoadingControl loadingControl) {
        super(new SettingsSnapshot(instance), loadingControl);
        mActivityInstance = new WeakReference<OrderListActivity>(instance);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            if (mActivityInstance.get() == null)
                return false;

            boolean success = loadGeneral();

            if (success) {
                // if API call was successful
                mCartItems = JobCacheManager.restoreCartItems(settingsSnapshot.getUrl());
            }

            return success && !isCancelled();
        } catch (Exception ex) {
            CommonUtils.error(TAG, ex);
        }
        return false;
    }

    @Override
    protected int requestLoadResource() {
        return resHelper.loadResource(mActivityInstance.get(), RES_CART_CLEAR, settingsSnapshot);
    }

    @Override
    protected void onFailedPostExecute() {
        super.onFailedPostExecute();
        GuiUtils.alert(R.string.errorGeneral);
    }

    @Override
    protected void onSuccessPostExecute() {
        super.onSuccessPostExecute();
        OrderListActivity activity = mActivityInstance.get();
        if (activity == null || !activity.isActivityAlive()) {
            // if activity was closed
            return;
        }
        // display the updated cart content in the activity
        activity.displayShoppingCart(mCartItems);
    }
}
