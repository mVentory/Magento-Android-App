package com.mageventory.tasks;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.ProductListActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.settings.SettingsSnapshot;

public class LoadProductListData extends AsyncTask<Object, Integer, Boolean> implements MageventoryConstants {

	private boolean forceReload;
	private SettingsSnapshot mSettingsSnapshot;
	ProductListActivity mHost;

	public LoadProductListData(boolean forceReload, ProductListActivity host) {
		super();
		this.forceReload = forceReload;
		mHost = host;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		mSettingsSnapshot = new SettingsSnapshot(mHost);
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
			if (args == null || args.length < 1) {
				throw new IllegalArgumentException();
			}

			final int resType = (Integer) args[0];
			final String[] params = args.length >= 2 ? (String[]) args[1] : null;

			// the catalog product list processor doesn't need name
			// filter if it's going to retrieve products by category
			if (params != null && params.length >= 2 && TextUtils.isDigitsOnly(params[1])
					&& Integer.parseInt(params[1]) != INVALID_CATEGORY_ID) {
				params[0] = null;
			}

			if (!forceReload && JobCacheManager.productListExist(params, mSettingsSnapshot.getUrl())) {
				// there is cached data available, retrieve and display it
				mHost.restoreAndDisplayProductList(resType, params);
			} else {
				// load new data
				final int reqId = ResourceServiceHelper.getInstance().loadResource(mHost, resType, params, mSettingsSnapshot);
				mHost.operationRequestId.set(reqId);
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