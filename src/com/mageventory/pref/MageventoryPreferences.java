package com.mageventory.pref;

import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;

public class MageventoryPreferences implements MageventoryConstants {

	private static final String TAG = "MageventoryPreferences";
	private final String pkeyKey;
	private final String pkeyServiceUrl;
	private final String pkeyUser;
	
	private final String pkeyMaxProductCount;
	private final String pdefMaxProductCount;
	
	private final SharedPreferences preferences;

	public MageventoryPreferences(Context context, SharedPreferences preferences) {
		super();
		this.preferences = preferences;

		// initialize preference keys
		pkeyServiceUrl = context.getString(R.string.pkey_service_url);
		pkeyUser = context.getString(R.string.pkey_user);
		pkeyKey = context.getString(R.string.pkey_key);
		pkeyMaxProductCount = context.getString(R.string.pkey_max_product_count);
		pdefMaxProductCount = context.getString(R.string.pdef_max_product_count);
	}

	public String getKey() {
		return preferences.getString(pkeyKey, "");
	}

	public String getServiceUrl() {
		return preferences.getString(pkeyServiceUrl, "");
	}

	public String getUser() {
		return preferences.getString(pkeyUser, "");
	}
	
	public int getMaxProductCount() {
		return Integer.parseInt(preferences.getString(pkeyMaxProductCount, pdefMaxProductCount));
	}

	/**
	 * Return true if argument is one of following keys:
	 * <ul>
	 * <li>ServiceUrl</li>
	 * <li>User</li>
	 * <li>Key</li>
	 * </ul>
	 * 
	 * @param key
	 *            key to check
	 * @return
	 */
	public boolean isClientKey(final String key) {
		for (String e : new String[] { pkeyServiceUrl, pkeyUser, pkeyKey }) {
			if (e.equals(key)) {
				return true;
			}
		}
		return false;
	}

	public void dumpPreferences() {
		Log.d(TAG, "Preferences ----------------------------");
		final Map<String, ?> data = preferences.getAll();
		for (Map.Entry<String, ?> entry : data.entrySet()) {
			Log.d(TAG, "" + entry.getKey() + ":" + entry.getValue());
		}
		Log.d(TAG, "End ------------------------------------");
	}

}
