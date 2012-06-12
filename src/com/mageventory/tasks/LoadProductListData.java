package com.mageventory.tasks;

import java.util.Arrays;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.ProductListActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.util.Log;

public class LoadProductListData extends AsyncTask<Object, Integer, Boolean>
		implements MageventoryConstants {

	private boolean forceReload;

	public LoadProductListData(boolean forceReload) {
		super();
		this.forceReload = forceReload;
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
		setThreadName();
		try {
			if (args == null || args.length < 2) {
				throw new IllegalArgumentException();
			}

			final ProductListActivity host = (ProductListActivity) args[0];
			final int resType = (Integer) args[1];
			final String[] params = args.length >= 3 ? (String[]) args[2]
					: null;

			// the catalog product list processor doesn't need name
			// filter if it's going to retrieve products by category
			if (params != null && params.length >= 2
					&& TextUtils.isDigitsOnly(params[1])
					&& Integer.parseInt(params[1]) != INVALID_CATEGORY_ID) {
				params[0] = null;
			}

			if (!forceReload
					&& JobCacheManager.productListExist(params)) {
				// there is cached data available, retrieve and display it
				host.restoreAndDisplayProductList(resType, params);
			} else {
				// load new data
				final int reqId = ResourceServiceHelper.getInstance()
						.loadResource(host, resType, params);
				host.operationRequestId.set(reqId);
			}
			return Boolean.TRUE;
		} catch (Throwable e) {
			return Boolean.FALSE;
		}
	}

	private void setThreadName() {
		final String threadName = Thread.currentThread().getName();
		Thread.currentThread().setName("LoadDataTask[" + threadName + "]");
	}

}