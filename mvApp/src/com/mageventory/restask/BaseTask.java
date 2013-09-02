
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
