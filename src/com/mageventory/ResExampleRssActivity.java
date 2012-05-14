package com.mageventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import com.mageventory.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;

/**
 * Start new service operation to parse an rss feed, read all of its thumbnails
 * and, when ready, display a list with the urls.
 */
public class ResExampleRssActivity extends ListActivity implements OperationObserver, MageventoryConstants {

	private static final String FEED_URL = "http://feeds.bbci.co.uk/news/rss.xml?edition=int";

	private class ListTask extends AsyncTask<Object, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Object... args) {
			final String[] resourceParams = { FEED_URL };
			final ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
			final boolean resAvail = resHelper.isResourceAvailable(ResExampleRssActivity.this, RES_EXAMPLE_FEED,
					resourceParams);
			if (resAvail) {
				// retrieve and display
				Log.d("qwe", "AVAIL AND READED");
				List<String> thumbnails = resHelper.restoreResource(ResExampleRssActivity.this, RES_EXAMPLE_FEED,
						resourceParams);
				listThumbnails(thumbnails);
			} else {
				// start new service operation
				requestId = resHelper.loadResource(ResExampleRssActivity.this, RES_EXAMPLE_FEED, resourceParams);
			}
			return Boolean.TRUE;
		}
	}

	private static final String EKEY_REQ_ID = "req_id";
	private int requestId;
	private boolean listDisplayed;

	private void listThumbnails(final List<String> thumbnails) {
		final List<Map<String, String>> data = new ArrayList<Map<String, String>>(thumbnails.size());
		for (final String thumbnail : thumbnails) {
			final Map<String, String> item = new HashMap<String, String>();
			item.put("url", thumbnail);
			data.add(item);
		}
		final SimpleAdapter adapter = new SimpleAdapter(ResExampleRssActivity.this, data,
				android.R.layout.simple_list_item_1, new String[] { "url" }, new int[] { android.R.id.text1 });
		final Runnable setAdapter = new Runnable() {
			@Override
			public void run() {
				setListAdapter(adapter);
			}
		};
		if (Looper.getMainLooper() == Looper.myLooper()) {
			setAdapter.run();
		} else {
			runOnUiThread(setAdapter);
		}
		listDisplayed = true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.product_list);
		if (savedInstanceState != null) {
			requestId = savedInstanceState.getInt(EKEY_REQ_ID);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(EKEY_REQ_ID, requestId);
	}

	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		if (requestId == op.getOperationRequestId()) {
			// yes, this is the same task we started
			if (op.getException() == null) {
				new ListTask().execute();
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		ResourceServiceHelper.getInstance().unregisterLoadOperationObserver(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		final ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
		resHelper.registerLoadOperationObserver(this);

		// if data is already displayed, there is no need to check its state,
		// reload it, etc.
		if (listDisplayed) {
			return;
		}

		if (resHelper.isPending(requestId)) {
			// wait, when it's ready the onLoadOperationCompleted method will be
			// called
		} else {
			new ListTask().execute();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		@SuppressWarnings("unchecked")
		final String imageUrl = ((Map<String, String>) getListAdapter().getItem(position)).get("url");
		final Intent gallery = new Intent(this, ResExampleImageActivity.class);
		gallery.putExtra("image_url", imageUrl);
		startActivity(gallery);
	}

}
