/* Copyright (c) 2014 mVentory Ltd. (http:/import android.view.View;

import com.mageventory.R;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.loading.GenericMultilineVIewLoadingControl.ProgressData;
s — If you compile, transform, or build upon the material,
 * you may not distribute the modified material. 
 * Attribution — You must give appropriate credit, provide a link to the license,
 * and indicate if changes were made. You may do so in any reasonable manner, 
 * but not in any way that suggests the licensor endorses you or your use. 
 */

package com.mageventory.util.loading;

import android.view.View;

import com.mventory.R;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.loading.GenericMultilineViewLoadingControl.ProgressData;

/**
 * Generic multiline view loading control. Control progress overlay visibility
 * and loading messages list
 */
public class GenericMultilineViewLoadingControl extends MultilineViewLoadingControl<ProgressData> {

    /**
     * Possible values for the {@link GenericMultilineViewLoadingControl}
     */
    public enum ProgressData {
        ATTRIBUTE_SETS(R.string.loading_attr_sets), 
        ATTRIBUTES_LIST(R.string.loading_attrs_list), 
        RECENT_WEB_ADDRESSES_LIST(R.string.loading_recent_web_addresses_list), 
        ;
        private String mDescription;

        ProgressData(int resourceId) {
            this(CommonUtils.getStringResource(resourceId));
        }

        ProgressData(String description) {
            mDescription = description;
        }

        @Override
        public String toString() {
            return mDescription;
        }
    }

    /**
     * Constructor
     * 
     * @param view
     */
    public GenericMultilineViewLoadingControl(View view) {
        super(view);
    }

}
