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

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.mageventory.R;
import com.mageventory.util.GuiUtils;

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
    List<PROGRESS_DATA> mLoaders = new ArrayList<PROGRESS_DATA>();

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
            // list
            if (!mLoaders.contains(data)) {
                mLoaders.add(data);
            }
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
            // remove data from loaders
            mLoaders.remove(data);
            // adjust visibility of the main progress view if last loader was
            // removed
            if (mLoaders.isEmpty()) {
                setViewVisibile(false);
            }
            updateMessage();
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
        mMessageView.setText(TextUtils.join("\n", mLoaders));
    }

}
