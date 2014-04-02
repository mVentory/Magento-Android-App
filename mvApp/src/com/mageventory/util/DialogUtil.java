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

import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Dialog;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

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

    public static Dialog createListDialog(final Activity host, final String dialogTitle,
            final List<Map<String, Object>> data, final int rowId, final String[] keys,
            final int[] viewIds,
            final OnItemClickListener onItemClickL) {
        final Dialog dialog = new Dialog(host);
        dialog.setTitle(dialogTitle);

        final ListView list = new ListView(host);
        final SimpleAdapter adapter = new SimpleAdapter(host, data, rowId, keys, viewIds);

        dialog.setContentView(list);
        list.setAdapter(adapter);

        list.setOnItemClickListener(onItemClickL);

        return dialog;
    }

}
