package com.mageventory.res;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.provider.BaseColumns;

public class ResourceState {

	public static final String AUTHORITY = "com.mageventory.res.ResourceState";

	public static class ResourceStateSchema implements BaseColumns {

		// meta
		public static final String CONTENT_TYPE = "vnd.mageventory.cursor.dir/vnd.mageventory.resourcestate";
		public static final String CONTENT_ITEM_TYPE = "vnd.mageventory.cursor.item/vnd.mageventory.resourcestate";

		public static final String PATH = "resourcestate";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + '/' + PATH);
		public static final String CONTENT_ITEM_URI = "content://" + AUTHORITY + '/' + PATH + "/%d";

		public static final String TABLE_NAME = "resourcestate";

		// columns
		// id
		// public static final String FILEPATH = "filepath";
		public static final String STATE = "state";
		public static final String TIMESTAMP = "timestamp";
		public static final String TRANSACTING = "transacting";
		public static final String RESOURCE_URI = "resource_uri";
		public static final String OLD = "old";

		// column types
		public static final String ID_T = "INTEGER PRIMARY KEY";
		// public static final String FILEPATH_T = "TEXT";
		public static final String STATE_T = "INTEGER";
		public static final String TIMESTAMP_T = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP";
		public static final String TRANSACTING_T = "INTEGER";
		public static final String RESOURCE_URI_T = "TEXT UNIQUE ON CONFLICT REPLACE";
		public static final String OLD_T = "INTEGER";

		// values
		public static final int STATE_NONE = 1;
		public static final int STATE_DELETING = 2;
		public static final int STATE_UPDATING = 3;
		public static final int STATE_BUILDING = 4;
		public static final int STATE_AVAILABLE = 5;
		// keep these always in order
		public static final int[] STATES = { STATE_NONE, STATE_DELETING, STATE_UPDATING, STATE_BUILDING, STATE_AVAILABLE };

		public static final Map<String, String> COLUMNS;
		static {
			final Map<String, String> tmp = new HashMap<String, String>();
			// tmp.put(_ID, ID_T);
			// tmp.put(FILEPATH, FILEPATH_T);
			tmp.put(STATE, STATE_T);
			tmp.put(TIMESTAMP, TIMESTAMP_T);
			tmp.put(TRANSACTING, TRANSACTING_T);
			tmp.put(RESOURCE_URI, RESOURCE_URI_T);
			tmp.put(OLD, OLD_T);
			COLUMNS = Collections.unmodifiableMap(tmp);
		}

	}

}
