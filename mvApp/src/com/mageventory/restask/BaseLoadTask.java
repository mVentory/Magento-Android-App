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
