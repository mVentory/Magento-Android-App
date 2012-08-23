package com.mageventory.tasks;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.AbsProductActivity;
import com.mageventory.activity.AbsProductActivity.CategoriesData;
import com.mageventory.job.JobCacheManager;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.SettingsSnapshot;

public class LoadCategories extends BaseTask<AbsProductActivity, CategoriesData> implements MageventoryConstants,
		OperationObserver {

	private CategoriesData myData = new CategoriesData();
	private boolean forceLoad = false;
	private CountDownLatch doneSignal;
	private int catReqId = INVALID_REQUEST_ID;
	private boolean catSuccess = false;
	private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
	private int state = TSTATE_NEW;
	int nlatches = 0;
	private SettingsSnapshot mSettingsSnapshot;

	public LoadCategories(AbsProductActivity hostActivity) {
		super(hostActivity);
	}

	public int getState() {
		return state;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		state = TSTATE_RUNNING;
		setData(myData);
		getHost().onCategoryLoadStart();
		
		mSettingsSnapshot = new SettingsSnapshot(getHost());
	}

	@Override
	protected Integer doInBackground(Object... args) {
		if (args != null && args.length > 0 && args[0] instanceof Boolean) {
			forceLoad = (Boolean) args[0];
		}

		final AbsProductActivity host = getHost();

		// start remote loading

		if (isCancelled()) {
			return 0;
		}

		if (forceLoad || JobCacheManager.categoriesExist(mSettingsSnapshot.getUrl()) == false) {
			resHelper.registerLoadOperationObserver(this);
			catReqId = resHelper.loadResource(host, RES_CATALOG_CATEGORY_TREE, mSettingsSnapshot);
			nlatches += 1;
		} else {
			catSuccess = true;
		}

		if (nlatches > 0) {
			doneSignal = new CountDownLatch(nlatches);
			while (true) {
				if (isCancelled()) {
					return 0;
				}
				try {
					if (doneSignal.await(2, TimeUnit.SECONDS)) {
						break;
					}
				} catch (InterruptedException e) {
					return 0;
				}
			}
		}
		resHelper.unregisterLoadOperationObserver(this);

		// retrieve local data

		if (isCancelled()) {
			return 0;
		}

		if (catSuccess) {
			myData.categories = JobCacheManager.restoreCategories(mSettingsSnapshot.getUrl());
			host.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (myData.categories != null) {
						host.onCategoryLoadSuccess();
					} else {
						host.onCategoryLoadFailure();
					}
				}
			});
		}
		return 0;
	}

	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		final AbsProductActivity host = getHost();
		if (op.getOperationRequestId() == catReqId) {
			// categories
			if (op.getException() == null) {
				catSuccess = true;
			} else {
				if (host != null) {
					host.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							host.onCategoryLoadFailure();
						}
					});
				}
			}
		} else {
			return;
		}

		doneSignal.countDown();
	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		state = TSTATE_TERMINATED;
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
		state = TSTATE_CANCELED;
	}

}
