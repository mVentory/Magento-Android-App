package com.mageventory.res;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.mageventory.res.ResourceState.ResourceStateSchema;

/**
 * This activity is implemented for debug reasons only and that's why it's not really fast. Stuff happens in the UI
 * thread.
 * 
 * @author Yordan Miladinov
 * 
 */
public class ResourceStateActivity extends ListActivity {

	private static class CreateCursorTask extends AsyncTask<Void, Void, Boolean> {

		private AtomicReference<Cursor> mCursor = new AtomicReference<Cursor>();
		private AtomicReference<ResourceStateActivity> mHost = new AtomicReference<ResourceStateActivity>();

		public CreateCursorTask(ResourceStateActivity host) {
			super();
			this.mHost.set(host);
			if (host != null) {
				host.dataState = DataState.LOADING;
			}
		}

		@Override
		protected Boolean doInBackground(Void... args) {
			final Context context = getHost();
			if (context != null) {
				final ContentResolver content = context.getContentResolver();
				final Set<String> columns = ResourceStateSchema.COLUMNS.keySet();
				final String[] projection = new String[columns.size() + 1];

				// add the _id column since it's missing from the COLUMNS map
				int i = 0;
				projection[i++] = ResourceStateSchema._ID;
				for (final String column : columns) {
					projection[i++] = column;
				}

				Cursor cursor = content.query(ResourceStateSchema.CONTENT_URI, projection, "1", null, null);
				setCursor(cursor);
				return Boolean.TRUE;
			}
			return Boolean.FALSE;
		}

		private Cursor getCursor() {
			return mCursor.get();
		}

		private ResourceStateActivity getHost() {
			return mHost.get();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result == null || result == Boolean.FALSE) {
				return;
			}
			final ResourceStateActivity host = getHost();
			if (host == null) {
				return;
			}
			host.dataState = DataState.LOADED_NOT_DISPLAYED;
			final Cursor cursor = getCursor();
			if (cursor != null) {
				host.setupCursorAdapter(cursor);
			}
		}

		private void setCursor(Cursor cursor) {
			this.mCursor.set(cursor);
		}

		private void setHost(ResourceStateActivity host) {
			this.mHost.set(host);
		}

	}

	private static enum DataState {
		DISPLAYED, LOADING, LOADED_NOT_DISPLAYED, NONE
	}

	private static class StateCursorAdapter extends CursorAdapter {

		private final LayoutInflater inflater;

		public StateCursorAdapter(Context context, Cursor c, boolean autoRequery) {
			super(context, c, autoRequery);
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final String uri = cursor.getString(cursor.getColumnIndex(ResourceStateSchema.RESOURCE_URI));
			final String text1 = String.format("uri=%s", uri);
			final String text2 = String.format(
					"_id=%d,state=%s,old=%b,avail=%b",
					cursor.getInt(cursor.getColumnIndex(ResourceStateSchema._ID)),
					cursor.getString(cursor.getColumnIndex(ResourceStateSchema.STATE)),
					cursor.getInt(cursor.getColumnIndex(ResourceStateSchema.OLD)) != 0,
					ResourceServiceHelper.getInstance().isResourceAvailable(context, uri));

			final TextView textView1 = (TextView) view.findViewById(android.R.id.text1);
			textView1.setText(text1);
			
			final TextView textView2 = (TextView) view.findViewById(android.R.id.text2);
			textView2.setText(text2);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return inflater.inflate(android.R.layout.simple_list_item_2, null);
		}

	}

	private DataState dataState = DataState.NONE;
	private CreateCursorTask task;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Object[] instance = (Object[]) getLastNonConfigurationInstance();
		if (instance != null) {
			task = (CreateCursorTask) instance[0];
			dataState = (DataState) instance[1];

			if (task != null) {
				task.setHost(this);
			}
		}

		// check if there is data loaded
		final Cursor loaded = task != null ? task.getCursor() : null;
		if (dataState == DataState.LOADED_NOT_DISPLAYED && loaded != null) {
			setupCursorAdapter(loaded);
			return;
		} else {
			dataState = DataState.NONE;
		}

		if (dataState == DataState.NONE) {
			task = new CreateCursorTask(this);
			task.execute();
		} else if (dataState == DataState.LOADING) {
			// wait for the task to set adapter when it's done loading
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new Object[] { task, dataState };
	}

	private void setupCursorAdapter(final Cursor cursor) {
		final CursorAdapter currentAdapter = (CursorAdapter) getListAdapter();
		if (currentAdapter == null) {
			final CursorAdapter adapter = new StateCursorAdapter(this, cursor, true);
			setListAdapter(adapter);
		} else {
			currentAdapter.changeCursor(cursor);
			currentAdapter.notifyDataSetChanged();
		}
		dataState = DataState.DISPLAYED;
	}

}
