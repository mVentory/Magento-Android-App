package com.mageventory.settings;

import com.mageventory.client.Base64Coder_magento;
import com.mageventory.job.JobCacheManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Settings {

	private static final String USER_KEY = "user";
	private static final String PASS_KEY = "pass";
	private static final String URL_KEY = "url";
	private static final String CUSTOMER_VALID_KEY = "customer_valid";
	private static final String PROFILE_DATA_VALID = "profile_data_valid";
	private static final String GOOGLE_BOOK_API_KEY = "api_key";
	private static final String GALLERY_PHOTOS_DIRECTORY_KEY = "gallery_photos_directory";
	private static final String MAX_IMAGE_WIDTH_KEY = "image_width";
	private static final String MAX_IMAGE_HEIGHT_KEY = "image_height";
	
	private static final String LIST_OF_STORES_KEY = "list_of_stores";
	private static final String CURRENT_STORE_KEY = "current_store_key";

	private static String listOfStoresFileName = "list_of_stores.dat";
	private static final String NEW_MODE_STRING = "New profile";
	private static final String NO_STORE_IS_CURRENT = "no store is current";
	
	private SharedPreferences settings;

	private Context context;
	
	public void switchToStoreURL(String url)
	{
		SharedPreferences storesPreferences = context.getSharedPreferences(listOfStoresFileName, Context.MODE_PRIVATE);
		
		Editor e = storesPreferences.edit();
		e.putString(CURRENT_STORE_KEY, url);
		e.commit();
		
		if (url != null)
		{
			settings = context.getSharedPreferences(JobCacheManager.encodeURL(url), Context.MODE_PRIVATE);
		}
	}

	public String [] getListOfStores(boolean newMode)
	{
		SharedPreferences storesPreferences = context.getSharedPreferences(listOfStoresFileName, Context.MODE_PRIVATE);
		
		String storesString = storesPreferences.getString(LIST_OF_STORES_KEY, null);
		
		if (storesString == null)
		{
			if (newMode)
			{
				return new String [] {NEW_MODE_STRING};
			}
			else
			{
				return new String [0];
			}
		}
		
		if (newMode)
		{
			return (storesString + "\n" + NEW_MODE_STRING).split("\n");	
		}
		else
		{
			return storesString.split("\n");
		}
	}
	
	public int getStoresCount()
	{
		return getListOfStores(false).length;
	}
	
	public void addStore(String url)
	{
		if (storeExists(url))
		{
			return;
		}
		
		SharedPreferences storesPreferences = context.getSharedPreferences(listOfStoresFileName, Context.MODE_PRIVATE);
		
		String storesString = storesPreferences.getString(LIST_OF_STORES_KEY, null);
		
		if (storesString == null)
		{
			storesString = url;
		}
		else
		{
			storesString = storesString + "\n" + url;	
		}
		
		Editor e = storesPreferences.edit();
		e.putString(LIST_OF_STORES_KEY, storesString);
		e.commit();
	}
	
	public void removeStore(String url)
	{
		SharedPreferences storesPreferences = context.getSharedPreferences(listOfStoresFileName, Context.MODE_PRIVATE);
		
		String storesString = null;
		String [] storesList = getListOfStores(false);
		
		for(int i=0; i<storesList.length; i++)
		{
			if (!storesList[i].equals(url))
			{
				if (storesString == null)
				{
					storesString = storesList[i];
				}
				else
				{
					storesString = storesString + "\n" + storesList[i]; 
				}
			}
			else
			{
				SharedPreferences settingsToRemove = context.getSharedPreferences(JobCacheManager.encodeURL(url), Context.MODE_PRIVATE);
				Editor edit = settingsToRemove.edit();
				edit.clear();
				edit.commit();
			}
		}
		
		Editor e = storesPreferences.edit();
		e.putString(LIST_OF_STORES_KEY, storesString);
		e.commit();
	}
	
	public boolean storeExists(String url)
	{
		String [] storesList = getListOfStores(false);
		
		for(int i=0; i<storesList.length; i++)
		{
			if (storesList[i].equals(url))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public String getCurrentStoreUrl()
	{
		SharedPreferences storesPreferences = context.getSharedPreferences(listOfStoresFileName, Context.MODE_PRIVATE);
		
		return storesPreferences.getString(CURRENT_STORE_KEY, NO_STORE_IS_CURRENT);
	}
	
	public int getCurrentStoreIndex()
	{
		String [] stores = getListOfStores(false);
		String currentStore = getCurrentStoreUrl();
		
		for(int i=0; i<stores.length; i++)
		{
			if (stores[i].equals(currentStore))
				return i;
		}
		
		return -1;
	}
	
	/**
	 * @param act
	 *            The context from which to pick SharedPreferences
	 */
	public Settings(Context act) {
		context = act;
		
		settings = act.getSharedPreferences(
				JobCacheManager.encodeURL(getCurrentStoreUrl()), Context.MODE_PRIVATE);
	}

	public void setUser(String user) {
		Editor editor = settings.edit();
		editor.putString(USER_KEY, user);
		editor.commit();
	}

	public String getUser() {
		return settings.getString(USER_KEY, "");
	}

	public String getPass() {
		return settings.getString(PASS_KEY, "");
	}

	public void setPass(String pass) {
		Editor editor = settings.edit();
		editor.putString(PASS_KEY, pass);
		editor.commit();
	}

	public String getUrl() {
		return settings.getString(URL_KEY, "");
	}

	public void setUrl(String url) {
		Editor editor = settings.edit();
		editor.putString(URL_KEY, url);
		editor.commit();
	}

	public String getAPIkey() {
		return settings.getString(GOOGLE_BOOK_API_KEY, "");
	}

	public void setAPIkey(String url) {
		Editor editor = settings.edit();
		editor.putString(GOOGLE_BOOK_API_KEY, url);
		editor.commit();
	}
	
	public String getMaxImageWidth() {
		return settings.getString(MAX_IMAGE_WIDTH_KEY, "");
	}

	public void setMaxImageWidth(String width) {
		Editor editor = settings.edit();
		editor.putString(MAX_IMAGE_WIDTH_KEY, width);
		editor.commit();
	}
	
	public String getMaxImageHeight() {
		return settings.getString(MAX_IMAGE_HEIGHT_KEY, "");
	}

	public void setMaxImageHeight(String height) {
		Editor editor = settings.edit();
		editor.putString(MAX_IMAGE_HEIGHT_KEY, height);
		editor.commit();
	}
	
	public String getGalleryPhotosDirectory() {
		return settings.getString(GALLERY_PHOTOS_DIRECTORY_KEY, "");
	}

	public void setGalleryPhotosDirectory(String path) {
		Editor editor = settings.edit();
		editor.putString(GALLERY_PHOTOS_DIRECTORY_KEY, path);
		editor.commit();
	}
	
	public void setProfileDataValid(boolean valid) {
		Editor editor = settings.edit();
		editor.putBoolean(PROFILE_DATA_VALID, valid);
		editor.commit();
	}

	public boolean getProfileDataValid() {
		return settings.getBoolean(PROFILE_DATA_VALID, false);
	}
	
	/* Setter and Getter for CustomerValid */
	public void setCustomerValid(boolean valid) {
		Editor editor = settings.edit();
		editor.putBoolean(CUSTOMER_VALID_KEY, valid);
		editor.commit();
	}

	public boolean getCustomerValid() {
		return settings.getBoolean(CUSTOMER_VALID_KEY, false);
	}

	public boolean hasSettings() {
		return (!settings.getString(USER_KEY, "").equals(""));
	}

}
