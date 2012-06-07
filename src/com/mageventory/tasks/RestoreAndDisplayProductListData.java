package com.mageventory.tasks;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.os.AsyncTask;

import com.mageventory.MageventoryConstants;
import com.mageventory.ProductListActivity2;
import com.mageventory.ProductListActivity2.SortOrder;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.util.Log;

public class RestoreAndDisplayProductListData extends AsyncTask<Object, Integer, Boolean> implements MageventoryConstants {

	private List<Map<String, Object>> data;
	private WeakReference<ProductListActivity2> host;
	private boolean isRunning = true;

	public RestoreAndDisplayProductListData() {
		super();
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
			if (args == null || args.length < 2) {
				throw new IllegalArgumentException();
			}

			// initialize
			host = new WeakReference<ProductListActivity2>((ProductListActivity2) args[0]);
			final int resType = (Integer) args[1];
			final String[] params = args.length >= 3 ? (String[]) args[2] : null;
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
			
			final SortOrder order = host.get().determineSortOrder(nameFilter, categoryFilter);

			// retrieve data
			data = ResourceServiceHelper.getInstance().restoreResource(host.get(), resType, params);

			// prepare adapter
			if (data != null) {
				for (Iterator<Map<String, Object>> it = data.iterator(); it.hasNext();)
				{
					if (isCancelled())
					{
						return Boolean.FALSE;
					} 

					Map<String, Object> prod = it.next();
					 
					// ensure the required fields are present in the product
					// map
					for (final String field : ProductListActivity2.REQUIRED_PRODUCT_KEYS) {
						if (prod.containsKey(field) == false) {
							it.remove();
							break;
						}
					}
				}

				// y TODO: well... this is a bit hacky
				host.get().filterProductsByName(data, host.get().getNameFilter());
				host.get().sortProducts(data, order);
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
				host.get().displayData(data);
			} else {
				host.get().showDialog(ProductListActivity2.LOAD_FAILURE_DIALOG);
			}
		} catch (Throwable ignored) {
		}
	}

	public void setHost(ProductListActivity2 host) {
		this.host = new WeakReference<ProductListActivity2>(host);
	}

	private void setThreadName() {
		final String threadName = Thread.currentThread().getName();
		Thread.currentThread().setName("RestoreAndDisplayDataTask[" + threadName + "]");
	}

}
