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

package com.mageventory.util;

import java.util.concurrent.atomic.AtomicInteger;

import android.view.View;

/**
 * Simple loading control which shows/hides view on start/stop loading
 * 
 * @author Eugene Popovich
 */
public class SimpleViewLoadingControl implements LoadingControl {

    static final String TAG = SimpleViewLoadingControl.class.getSimpleName();

    private AtomicInteger mLoaders = new AtomicInteger(0);
    private View mView;

    public SimpleViewLoadingControl(View view) {
        this.mView = view;
    }

    @Override
    public void startLoading() {
        if (mLoaders.getAndIncrement() == 0) {
            setViewVisibile(true);
        }
    }

    @Override
    public void stopLoading() {
        if (mLoaders.decrementAndGet() == 0) {
            setViewVisibile(false);
        }
    }

    /**
     * Adjust the visibility of the loading view
     * 
     * @param visible
     */
    public void setViewVisibile(boolean visible) {
        try {
            mView.setVisibility(visible ? View.VISIBLE : View.GONE);
        } catch (Exception ex) {
            GuiUtils.noAlertError(TAG, ex);
        }
    }

    @Override
    public boolean isLoading() {
        return mLoaders.get() > 0;
    }

    public int getLoadersCount() {
        return mLoaders.get();
    }
}
