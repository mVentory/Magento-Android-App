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

package com.mageventory.util.loading;

import java.util.HashMap;
import java.util.Map;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.mventory.R;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.LoadingControl;

/**
 * Multiline view loading controller which handles overlay progress view and can
 * handle multiple loading operations at the time by displaying loading message
 * for each operation in new line
 * 
 * @author Eugene Popovich
 * @param <PROGRESS_DATA>
 */
public class MultilineViewLoadingControl<PROGRESS_DATA> {
    public static final String TAG = MultilineViewLoadingControl.class.getSimpleName();

    /**
     * The overlay progress view
     */
    View mView;
    /**
     * The view whichi contains progress messages
     */
    TextView mMessageView;
    /**
     * The separate loaders holder for each operation
     */
    Map<PROGRESS_DATA, Integer> mLoaders = new HashMap<PROGRESS_DATA, Integer>();

    /**
     * @param view the overlay progress view
     */
    public MultilineViewLoadingControl(View view) {
        mView = view;
        mMessageView = (TextView) view.findViewById(R.id.progressMesage);
    }

    /**
     * Start loading of the operation described by data param. Handle loaders
     * for that operation
     * 
     * @param data
     */
    public void startLoading(PROGRESS_DATA data) {
        // mLoaders editing should be synchronous. Request lock
        synchronized (mLoaders) {

            // if where are no active operation prior the startLoading call
            // adjust the visibility of the main view
            if (mLoaders.isEmpty()) {
                setViewVisibile(true);
            }

            // if there are no registered loader for the data yet add it to the
            // map
            Integer loader = mLoaders.get(data);
            if (loader == null) {
                // if loaders was not yet added set counter to 1
                loader = 1;
            } else {
            	//else increment counter
                loader++;
            }
            // updated loader value in the map
            mLoaders.put(data, loader);
            updateMessage();
        }
    }

    /**
     * Stop loading of the operation described by data param. Handle loaders for
     * that operation
     * 
     * @param data
     */
    public void stopLoading(PROGRESS_DATA data) {
        synchronized (mLoaders) {
            Integer loader = mLoaders.get(data);
            if (loader == null || loader.intValue() == 1) {
                // loader reached 1 counter so remove data from loaders
                mLoaders.remove(data);
            } else {
            	//else decrement counter
                loader--;
                // updated loader value in the map
                mLoaders.put(data, loader);
            }
            // adjust visibility of the main progress view if last loader was
            // removed
            if (mLoaders.isEmpty()) {
                setViewVisibile(false);
            }
            updateMessage();
        }
    }

    /**
     * Is the operation still loading
     * 
     * @param data
     * @return
     */
    public boolean isLoading(PROGRESS_DATA data) {
        synchronized (mLoaders) {
            return mLoaders.containsKey(data);
        }
    }

    /**
     * Adjust visibility of the main progress view
     * 
     * @param visible whether the view should be visible or not
     */
    protected void setViewVisibile(boolean visible) {
        try {
            mView.setVisibility(visible ? View.VISIBLE : View.GONE);
        } catch (Exception ex) {
            GuiUtils.noAlertError(TAG, ex);
        }
    }

    /**
     * Update the message with the all loaders information separated by the new
     * line separator
     */
    protected void updateMessage() {
        mMessageView.setText(TextUtils.join("\n", mLoaders.keySet()));
    }

    /**
     * Get the {@link LoadingControl} instance for the operation
     * 
     * @param data
     * @return
     */
    public LoadingControl getLoadingControlWrapper(PROGRESS_DATA data) {
        return new LoadingControlWrapper(data);
    }

    /**
     * Simple {@link LoadingControl} wrapper for the
     * {@link MultilineViewLoadingControl} so it can be used in places where
     * only {@link LoadingControl} type is allowed to be used as loading control
     */
    class LoadingControlWrapper implements LoadingControl {
        /**
         * The data which loading should be controled
         */
        PROGRESS_DATA mData;

        LoadingControlWrapper(PROGRESS_DATA data) {
            mData = data;
        }

        @Override
        public void stopLoading() {
            MultilineViewLoadingControl.this.stopLoading(mData);
        }

        @Override
        public void startLoading() {
            MultilineViewLoadingControl.this.startLoading(mData);
        }

        @Override
        public boolean isLoading() {
            return MultilineViewLoadingControl.this.isLoading(mData);
        }
    }
}
