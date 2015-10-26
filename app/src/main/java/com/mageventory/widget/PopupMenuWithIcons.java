package com.mageventory.widget;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;
import android.view.View;
import android.widget.PopupMenu;

import com.mageventory.util.CommonUtils;

/**
 * {@link PopupMenu} extensions which enables showing of popup menu icons via
 * the hack. <br>
 * http://stackoverflow.com/a/18431605/527759
 */
public class PopupMenuWithIcons extends PopupMenu {
    /**
     * Tag used for logging
     */
    public static final String TAG = PopupMenuWithIcons.class.getSimpleName();
    
    /**
     * @see {@link PopupMenu#PopupMenu(Context, View)}
     */
    public PopupMenuWithIcons(Context context, View anchor) {
        super(context, anchor);

        try {
            Field[] fields = PopupMenu.class.getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(this);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon",
                            boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            CommonUtils.error(TAG, e);
        }
    }
}
