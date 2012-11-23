package com.mageventory.tasks;

import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.os.AsyncTask;

import com.mageventory.MageventoryConstants;
import com.mageventory.activity.ProductListActivity;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCacheManager.GalleryTimestampRange;
import com.mageventory.model.Product;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.settings.Settings.ProfileIDNotFoundException;
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

			if (params.length == 2 && params[1].equals("-1000"))
			{
				data = new ArrayList<Map<String, Object>>();
				ArrayList<String> skusList = new ArrayList<String>();
				
				synchronized(JobCacheManager.sSynchronizationObject)
				{
					ArrayList<GalleryTimestampRange> galleryTimestampsRangesArray =
						JobCacheManager.getGalleryTimestampRangesArray();
					
					if (galleryTimestampsRangesArray != null)
					{
						for (int i = galleryTimestampsRangesArray.size() - 1; i>=0; i--)
						{
							String decodedSKU = URLDecoder.decode(galleryTimestampsRangesArray.get(i).escapedSKU, "UTF-8");
							String productName = decodedSKU;
							
							if (galleryTimestampsRangesArray.get(i).profileID == mSettingsSnapshot.getProfileID())
							{
								Map<String, Object> productMap = new HashMap<String, Object>();
								
								Product p = JobCacheManager.restoreProductDetails(decodedSKU, mSettingsSnapshot.getUrl());

								if (p != null)
								{
									productName = p.getName();
								}
								
								productMap.put("name", productName);
								productMap.put("sku", decodedSKU);
								
								if (!skusList.contains(decodedSKU) && (productName.contains(params[0]) || decodedSKU.contains(params[0])))
								{
									data.add(productMap);
									skusList.add(decodedSKU);
								}
								if (data.size()>=50)
									break;
							}
						}
					}
				}
			}
			else
			{
			  // retrieve data
			  data = JobCacheManager.restoreProductList(params, mSettingsSnapshot.getUrl());
			}

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
