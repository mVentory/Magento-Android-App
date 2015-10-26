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

package com.mageventory.fragment.base;

import android.os.Bundle;

/**
 * BaseDialogFragment which self-closes in case of restore situation
 * 
 * @author Eugene Popovich
 */
public class BaseClosableOnRestoreDialogFragment extends BaseDialogFragment {
    boolean isRestore = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isRestore = savedInstanceState != null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isRestore)
        {
            trackLifecycleEvent("dismiss on restore");
            dismiss();
        }
    }
}
