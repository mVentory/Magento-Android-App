package com.mageventory.settings;

import java.io.Serializable;

import android.content.Context;
import android.text.TextUtils;

/* Represents state of the settings at the point in time it was created. */
public class SettingsSnapshot implements Serializable
{
	private static final long serialVersionUID = -3727627184033245655L;
	private String url;
	private String user;
	private String password;
	
	public SettingsSnapshot(Context c)
	{
		Settings s = new Settings(c);
		
		url = s.getUrl();
		user = s.getUser();
		password = s.getPass();
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