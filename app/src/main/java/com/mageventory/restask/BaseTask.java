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

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.os.AsyncTask;

public abstract class BaseTask<T extends Activity, S extends Object> extends
        AsyncTask<Object, Integer, Integer> {

    // ---
    // fields

    private S data;
    private T hostActivity;

    // ---
    // constructors

    public BaseTask() {
        this(null);
    }

    public BaseTask(T hostActivity) {
        this.hostActivity = hostActivity;
    }

    // ---
    // methods

    public S getData() {
        return data;
    }

    public T getHost() {
        return hostActivity;
    }

    protected void setData(S data) {
        this.data = data;
    }

    public void setHost(T host) {
        hostActivity = host;
    }

}
