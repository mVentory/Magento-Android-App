package com.mageventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.mageventory.client.MagentoClient;

// XXX y: the screen orientation is locked to
// portrait because of the async task that takes place here
// TODO y: rework the data loading
public class ProductListActivity2 extends ListActivity {

	private static class LoadTask extends AsyncTask<Object, Integer, Boolean> {

		/**
		 * Host activity. Should be passed as argument to
		 * {@link LoadTask#execute(Object...)};
		 */
		private ListActivity host;
		private MagentoClient client;
		/**
		 * The product list that is retrieved by querying Magento.
		 */
		private Object[] productList;
		/**
		 * List adapter. Data is built after successful retrieving of the
		 * product list.
		 */
		private SimpleAdapter adapter;

		/**
		 * Expected arguments:<br>
		 * <ol>
		 * <li>ListActivity</li>
		 * </ol>
		 */
		@Override
		protected Boolean doInBackground(Object... arg0) {
			if (arg0[0] == null) {
				throw new NullPointerException();
			}
			try {
				host = (ListActivity) arg0[0];
				client = ((MyApplication) host.getApplication()).getClient();
			} catch (ClassCastException e) {
				throw new IllegalArgumentException(e);
			}
			productList = (Object[]) client.execute("catalog_product.list");
			if (productList != null) {
				// TODO y: move hard-coded value to preferences
				final int SIZE = Math.min(productList.length, 50);
				final List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(SIZE);
				for (int i = 0; i < SIZE; i++) {
					@SuppressWarnings("unchecked")
					final Map<String, Object> product = (Map<String, Object>) productList[i];
					// assure the required fields are present in the product map
					boolean reqFieldsPresent = true;
					for (final String field : REQUIRED_PRODUCT_KEYS) {
						if (product.containsKey(field) == false) {
							reqFieldsPresent = false;
							break;
						}
					}
					if (reqFieldsPresent) {
						// convert the category_ids field to human readable
						// string
						final Object[] categoryIds = (Object[]) product.get("category_ids");
						product.put("category_ids", Arrays.toString(categoryIds));
						data.add(product);
					} else {
						// TODO y: log the problem
					}
				}
				// reverse list, because by default (?) Magento selects products
				// by id ASC, but we want the newest first
				Collections.reverse(data);
				adapter = new SimpleAdapter(host, data, R.layout.product, REQUIRED_PRODUCT_KEYS, KEY_TO_VIEW_MAP);
				return true;
			}
			return false;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			super.onPostExecute(success);
			if (success) {
				host.setListAdapter(adapter);
			}
		}

	}

	private static final String[] REQUIRED_PRODUCT_KEYS = { "category_ids", "name", "product_id", "sku", "type", };
	// TODO y: check if the R.id fields change at runtime
	private static final int[] KEY_TO_VIEW_MAP = { R.id.category_ids, R.id.name, R.id.product_id, R.id.sku, R.id.type };

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		// setTitle("Newest products");
		new LoadTask().execute(this);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (toggleAdditionalInfo(v)) {
			return;
		}
		super.onListItemClick(l, v, position, id);
	}

	public boolean toggleAdditionalInfo(View view) {
		if (view == null) {
			throw new NullPointerException();
		}
		final View additionalInfo = view.findViewById(R.id.additional_info);
		if (additionalInfo == null) {
			return false;
		}
		final int visibility = additionalInfo.getVisibility();
		switch (visibility) {
		case View.VISIBLE:
			additionalInfo.setVisibility(View.GONE);
			break;
		case View.GONE:
		case View.INVISIBLE:
			additionalInfo.setVisibility(View.VISIBLE);
			break;
		default:
			// throw new IllegalArgumentException("view.getVisibility() --> " +
			// visibility);
			return false;
		}
		return true;
	}

}