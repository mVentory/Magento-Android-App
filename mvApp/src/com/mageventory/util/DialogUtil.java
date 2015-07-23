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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface.OnCancelListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.mageventory.MageventoryConstants;
import com.mageventory.model.Category;

public class DialogUtil implements MageventoryConstants {

    // TODO y: I should move all the progress dialog logic here... I think there
    // is a task for this
    private static final String PREFERENCES_NAME = "dialog_util";
    protected static final String PKEY_SELECTION = "selection_from_top";

    public static interface OnCategorySelectListener {
        public boolean onCategorySelect(Category category);
    }

    /**
     * Create the dialog with the ListView
     * 
     * @param host
     * @param dialogTitle
     * @param adapter the list adapter
     * @param onItemClickL
     * @param onCancelListener
     * @return
     */
    public static Dialog createListDialog(final Activity host, final String dialogTitle,
            ListAdapter adapter,
            final OnItemClickListener onItemClickL,
            OnCancelListener onCancelListener) {
        final Dialog dialog = new Dialog(host);
        dialog.setTitle(dialogTitle);

        final ListView list = new ListView(host);

        dialog.setContentView(list);
        dialog.setOnCancelListener(onCancelListener);
        list.setAdapter(adapter);

        list.setOnItemClickListener(onItemClickL);

        return dialog;
    }

}
