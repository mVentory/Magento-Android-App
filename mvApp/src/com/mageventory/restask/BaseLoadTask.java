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

package com.mageventory.restask;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;

public abstract class BaseLoadTask<T extends Activity, S extends Object> extends BaseTask<T, S> {

    public static interface OnRequestListener {
        public void onRequest(int requestId);
    }

    private List<OnRequestListener> listeners = new LinkedList<OnRequestListener>();

    public void addOnRequestListener(OnRequestListener onRequestL) {
        if (onRequestL == null) {
            return;
        }
        listeners.add(onRequestL);
    }

    protected void notifyListeners(final int requestId) {
        for (OnRequestListener listener : listeners) {
            listener.onRequest(requestId);
        }
    }

}
