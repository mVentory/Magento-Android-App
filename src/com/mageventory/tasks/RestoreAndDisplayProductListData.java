package com.mageventory.tasks;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.os.AsyncTask;

import com.mageventory.MageventoryConstants;
import com.mageventory.ProductListActivity;
import com.mageventory.ProductListActivity.SortOrder;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.Log;

public class RestoreAndDisplayProductListData extends AsyncTask<Object, Integer, Boolean> implements
		MageventoryConstants {

	private List<Map<String, Object>> data;
	private ProductListActivity host;
	private boolean isRunning = true;
	private SettingsSnapshot mSettingsSnapshot;

	public RestoreAndDisplayProductListData(ProductListActivity host) {
		super();
		this.host = host;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		mSettingsSnapshot = new SettingsSnapshot(host);
	}
	
	/**
	 * Expected arguments:
	 * <ul>
	 * <li>Activity host</li>
	 * <li>int resourceType</li>
	 * <li>String[] resourceParams</li>
	 * </ul>
	 */
	@Override
	protected Boolean doInBackground(Object... args) {
		try {
			setThreadName();

			// initialize
			final String[] params = args.length >= 1 ? (String[]) args[0] : null;
			String nameFilter = null;
			int categoryFilter = INVALID_CATEGORY_ID;
			if (params != null) {
				if (params.length >= 1) {
					nameFilter = (String) params[0];
				}
				if (params.length >= 2) {
					categoryFilter = Integer.parseInt(params[1]);
				}
			}

			final SortOrder order = host.determineSortOrder(nameFilter, categoryFilter);

			// retrieve data
			data = JobCacheManager.restoreProductList(params, mSettingsSnapshot.getUrl());

			// prepare adapter
			if (data != null) {
				for (Iterator<Map<String, Object>> it = data.iterator(); it.hasNext();) {
					if (isCancelled()) {
						return Boolean.FALSE;
					}

					Map<String, Object> prod = it.next();

					// ensure the required fields are present in the product
					// map
					for (final String field : ProductListActivity.REQUIRED_PRODUCT_KEYS) {
						if (prod.containsKey(field) == false) {
							it.remove();
							break;
						}
					}
				}

				// y TODO: well... this is a bit hacky
				host.filterProductsByName(data, host.getNameFilter());
				host.sortProducts(data, order);
				return Boolean.TRUE;
			}
		} catch (Throwable e) {
			Log.logCaughtException(e);
		}
		return Boolean.FALSE;
	}

	public List<Map<String, Object>> getData() {
		return data;
	}

	public boolean isRunning() {
		return isRunning;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		isRunning = false;

		super.onPostExecute(result);
		try {
			if (result) {
				host.displayData(data);
			} else {
				host.showDialog(ProductListActivity.LOAD_FAILURE_DIALOG);
			}
		} catch (Throwable ignored) {
		}
	}

	private void setThreadName() {
		final String threadName = Thread.currentThread().getName();
		Thread.currentThread().setName("RestoreAndDisplayDataTask[" + threadName + "]");
	}

}
