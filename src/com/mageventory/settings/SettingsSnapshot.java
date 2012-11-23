package com.mageventory.settings;

import java.io.Serializable;
import java.net.MalformedURLException;

import com.mageventory.client.MagentoClient;

import android.content.Context;
import android.text.TextUtils;

/* Represents state of the settings at the point in time it was created. */
public class SettingsSnapshot implements Serializable
{
	private static final long serialVersionUID = -3727627184033245655L;
	private String url;
	private String user;
	private String password;
	private long profileID;
	
	public SettingsSnapshot(Context c)
	{
		Settings s = new Settings(c);
		
		url = s.getUrl();
		user = s.getUser();
		password = s.getPass();
		profileID = s.getProfileID();
	}
	
	private SettingsSnapshot(String url, String user, String password, long profileID)
	{
		this.url = url;
		this.user = user;
		this.password = password;
		this.profileID = profileID;
	}
	
	public SettingsSnapshot getCopy() {
		return new SettingsSnapshot(url, user, password, profileID);
	}
	
	public void setUrl(String url)
	{
		this.url = url;
	}
	
	public void setUser(String user)
	{
		this.user = user;
	}
	
	public void setPassword(String password)
	{
		this.password = password;
	}
	
	public String getUrl()
	{
		return url;
	}
	
	public String getUser()
	{
		return user;
	}
	
	public String getPassword()
	{
		return password;
	}
	
	public long getProfileID()
	{
		return profileID;
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (!(o instanceof SettingsSnapshot))
			return false;
		
		SettingsSnapshot ss = (SettingsSnapshot)o;
		
		if (!TextUtils.equals(ss.url, this.url))
			return false;
		
		if (!TextUtils.equals(ss.user, this.user))
			return false;
		
		if (!TextUtils.equals(ss.password, this.password))
			return false;
		
		return true; 
	}
	
	@Override
	public int hashCode() {
		int out = 0;
		
		if (!TextUtils.isEmpty(url))
			out += url.hashCode();
		
		if (!TextUtils.isEmpty(user))
			out += user.hashCode();
		
		if (!TextUtils.isEmpty(password))
			out += password.hashCode();
		
		return out;
	}
}