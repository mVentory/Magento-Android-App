package com.mageventory.restask;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.os.AsyncTask;

public abstract class BaseTask<T extends Activity, S extends Object> extends AsyncTask<Object, Integer, Integer> {

	// ---
	// fields

	private S data;
	private WeakReference<T> hostActivity;

	// ---
	// constructors

	public BaseTask() {
		this(null);
	}

	public BaseTask(T hostActivity) {
		if (hostActivity != null) {
			this.hostActivity = new WeakReference<T>(hostActivity);
		}
	}

	// ---
	// methods

	public S getData() {
		return data;
	}

	public T getHost() {
		if (hostActivity == null) {
			return null;
		}
		return hostActivity.get();
	}

	protected void setData(S data) {
		this.data = data;
	}

	public void setHost(T host) {
		hostActivity = new WeakReference<T>(host);
	}

}
