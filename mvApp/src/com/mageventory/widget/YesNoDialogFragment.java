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

package com.mageventory.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.mventory.R;
import com.mageventory.fragment.base.BaseClosableOnRestoreDialogFragment;
import com.mageventory.util.CommonUtils;

/**
 * Basic Yes/No dialog fragment
 * 
 * @author Eugene Popovich
 */
public class YesNoDialogFragment extends BaseClosableOnRestoreDialogFragment {
    public static interface YesNoButtonPressedHandler {
        void yesButtonPressed(DialogInterface dialog);

        void noButtonPressed(DialogInterface dialog);
    }

    YesNoButtonPressedHandler mHandler;
    boolean mCancelable;
    String mMessage;

    /**
     * @param message
     * @param handler
     * @return
     */
    public static YesNoDialogFragment newInstance(int message, YesNoButtonPressedHandler handler) {
        return newInstance(CommonUtils.getStringResource(message), true, handler);
    }

    /**
     * @param message
     * @param handler
     * @return
     */
    public static YesNoDialogFragment newInstance(String message, YesNoButtonPressedHandler handler) {
        return newInstance(message, true, handler);
    }

    /**
     * @param message
     * @param cancelable whether dialog can be cancelled by the back button
     * @param handler
     * @return
     */
    public static YesNoDialogFragment newInstance(String message, boolean cancelable,
            YesNoButtonPressedHandler handler) {
        YesNoDialogFragment frag = new YesNoDialogFragment();
        frag.mHandler = handler;
        frag.mMessage = message;
        frag.mCancelable = cancelable;
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setCancelable(mCancelable)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (mHandler != null) {
                            mHandler.yesButtonPressed(dialog);
                        }
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (mHandler != null) {
                            mHandler.noButtonPressed(dialog);
                        }
                    }
                });
        if (mMessage != null) {
            builder.setMessage(mMessage);
        }
        return builder.create();
    }
}
