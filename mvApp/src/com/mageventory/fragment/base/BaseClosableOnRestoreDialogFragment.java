
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
