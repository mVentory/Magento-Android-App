package com.magventory.settings;

import com.magventory.MainActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class Settings {
 
	private static final String USER_KEY = "user";
	private static final String PASS_KEY = "pass";
	private static final String URL_KEY = "url";
	
	private final SharedPreferences settings;
    
	/**
	 * @param act The context from which to pick SharedPreferences
	 */
	public Settings (Context act) {
		 settings = act.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);

	}
 
	public void setUser(String user) {
		Editor editor = settings.edit();
		editor.putString(USER_KEY,user);
		editor.commit();
	}
	public String getUser() {
		return settings.getString(USER_KEY,"");
	}

	public String getPass() {
		return settings.getString(PASS_KEY,"");
	}
	
	public void setPass(String pass) {
		Editor editor = settings.edit();
		editor.putString(PASS_KEY,pass);
		editor.commit();
	}
 
	public String getUrl() {
		return settings.getString(URL_KEY,"");
	}
	
	public void setUrl(String url) {
		Editor editor = settings.edit();
		editor.putString(URL_KEY,url);
		editor.commit();
	}
 
	
	
	public boolean hasSettings() {
		return (!settings.getString(USER_KEY, "").equals(""));
	}
	
	}
 
