package com.mageventory.res;

import static com.mageventory.res.ResourceState.ResourceStateSchema.CONTENT_URI;
import static com.mageventory.res.ResourceState.ResourceStateSchema.OLD;
import static com.mageventory.res.ResourceState.ResourceStateSchema.RESOURCE_URI;
import static com.mageventory.res.ResourceState.ResourceStateSchema.STATE;
import static com.mageventory.res.ResourceState.ResourceStateSchema.STATES;
import static com.mageventory.res.ResourceState.ResourceStateSchema.STATE_NONE;
import static com.mageventory.res.ResourceState.ResourceStateSchema.TIMESTAMP;
import static com.mageventory.res.ResourceState.ResourceStateSchema.TRANSACTING;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

public class ResourceStateDao {

    // example: 2012-03-26 14:39:50
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd H:m:s");

    private static final String[] projection;
    private static final int stateIdx = 0;
    private static final int timestampIdx = 1;
    private static final int transactingIdx = 2;
    private static final int oldIdx = 3;
    static {
        projection = new String[] { STATE, TIMESTAMP, TRANSACTING, OLD };
    }

    private final ContentResolver resolver;

    public ResourceStateDao(Context context) {
        super();
        resolver = context.getContentResolver();
    }

    public boolean addResource(final String resourceUri) {
        final ContentValues values = new ContentValues(4);
        values.put(RESOURCE_URI, resourceUri);

        values.put(STATE, STATE_NONE);
        values.put(TRANSACTING, 0);
        values.put(OLD, 0);

        Uri insertedUri = resolver.insert(CONTENT_URI, values);
        return insertedUri != null;
    }

    public boolean deleteResource(final String resourceUri) {
        return resolver.delete(CONTENT_URI, RESOURCE_URI + "=?", new String[] { resourceUri }) != 0;
    }

    public ResourceRepr getResource(final String resourceUri) {
        final Cursor cursor = resolver.query(CONTENT_URI, projection, RESOURCE_URI + "=?", new String[] { resourceUri }, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    final int state = cursor.getInt(stateIdx);
                    long timestamp;
                    try {
                        final String timestampStr = cursor.getString(timestampIdx);
                        timestamp = dateFormat.parse(timestampStr).getTime();
                    } catch (ParseException e) {
                        timestamp = 0;
                    }
                    final boolean transaction = cursor.getInt(transactingIdx) != 0;
                    final boolean old = cursor.getInt(oldIdx) != 0;
                    return new ResourceRepr(state, timestamp, transaction, resourceUri, old);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * Get state of the resource if -1 if resource is unaccessible.
     * 
     * @param resourceUri
     * @return
     */
    public int getState(final String resourceUri) {
        final ResourceRepr res = getResource(resourceUri);
        if (res != null) {
            return res.state;
        } else {
            return -1;
        }
    }

    public void registerContentObserver(final ContentObserver observer) {
        resolver.registerContentObserver(CONTENT_URI, true, observer);
    }

    public boolean setState(final String resourceUri, int state) {
        if (Arrays.binarySearch(STATES, state) < 0) {
            throw new IllegalArgumentException("unrecogizned state");
        }
        final ContentValues values = new ContentValues(1);
        values.put(STATE, state);
        return resolver.update(CONTENT_URI, values, RESOURCE_URI + "=?", new String[] { resourceUri }) != 0;
    }

    public boolean setTransacting(final String resourceUri, boolean transacting) {
        final ContentValues values = new ContentValues(1);
        values.put(TRANSACTING, transacting ? 1 : 0);
        return resolver.update(CONTENT_URI, values, RESOURCE_URI + "=?", new String[] { resourceUri }) != 0;
    }

    /**
     * Build a parameterized URI in this form: baseUri/param1/param2/param3/...
     * 
     * @param baseUri
     * @param params
     * @return parameterized URI
     */
    public static String buildParameterizedUri(final int resourceType, final String[] params) {
        // y: the exclamation mark is very important, it prevents LIKE queries to match other resources with similar id
        final String baseUri = String.format("urn:mageventory:resource%d!", resourceType);
        final StringBuilder uriBuilder = new StringBuilder(baseUri);
        if (params == null || params.length == 0) {
            return uriBuilder.toString();
        }
        for (int i = 0; i < params.length; i++) {
            final String param = params[i];
            if (TextUtils.isEmpty(param)) {
                continue;
            }

            uriBuilder.append('/');
            uriBuilder.append(param);
        }
        return uriBuilder.toString();
    }

    public boolean setOld(String resourceUri, boolean old) {
        final ContentValues values = new ContentValues(1);
        values.put(OLD, old ? 1 : 0);
        if (resourceUri.contains("*")) {
            // wildcards
        	resourceUri = resourceUri.replace("/*", "%"); // that's hacky
            resourceUri = resourceUri.replace('*', '%');
            return resolver.update(CONTENT_URI, values, RESOURCE_URI + " LIKE ?", new String[] { resourceUri }) != 0;
        }
        return resolver.update(CONTENT_URI, values, RESOURCE_URI + "=?", new String[] { resourceUri }) != 0;
    }
    
    public boolean setOld(final int resourceType, final boolean old) {
        return setOld(resourceType, null, old);
    }

    public boolean setOld(final int resourceType, String[] params, final boolean old) {
	    final ContentValues values = new ContentValues(1);
        values.put(OLD, old ? 1 : 0);
        
        String resourceUri = buildParameterizedUri(resourceType, params);
        if (params == null || params.length == 0) {
            resourceUri += '*';
        }
	    
	    return setOld(resourceUri, old);
	}

	public boolean isOld(String resourceUri) {
		final ResourceRepr res = getResource(resourceUri);
		return res != null && res.old;
    }

}
