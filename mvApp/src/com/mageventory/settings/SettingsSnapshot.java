/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/

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
    private long profileID;
    /**
     * Profile license
     */
    private String mLicense;
    /**
     * Profile license signature
     */
    private String mSignature;

    public SettingsSnapshot(Context c)
    {
        Settings s = new Settings(c);

        url = s.getUrl();
        user = s.getUser();
        password = s.getPass();
        profileID = s.getProfileID();
        mLicense = s.getLicense();
        mSignature = s.getSignature();
    }

    /**
     * @param url the profile URL
     * @param user the profile user name
     * @param password the user password
     * @param profileID id of the profile
     * @param license the profile license information
     * @param signature the profile license signature
     */
    public SettingsSnapshot(String url, String user, String password, long profileID,
            String license, String signature)
    {
        this.url = url;
        this.user = user;
        this.password = password;
        this.profileID = profileID;
        mLicense = license;
        mSignature = signature;
    }

    public SettingsSnapshot getCopy() {
        return new SettingsSnapshot(url, user, password, profileID, mLicense, mSignature);
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

    public String getLicense() {
        return mLicense;
    }

    public void setLicense(String license) {
        mLicense = license;
    }

    public String getSignature() {
        return mSignature;
    }

    public void setSignature(String signature) {
        mSignature = signature;
    }

    @Override
    public boolean equals(Object o) {

        if (!(o instanceof SettingsSnapshot))
            return false;

        SettingsSnapshot ss = (SettingsSnapshot) o;

        if (!TextUtils.equals(ss.url, this.url))
            return false;

        if (!TextUtils.equals(ss.user, this.user))
            return false;

        if (!TextUtils.equals(ss.password, this.password))
            return false;
        
        if (!TextUtils.equals(ss.mLicense, this.mLicense))
            return false;
        
        if (!TextUtils.equals(ss.mSignature, this.mSignature))
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

        if (!TextUtils.isEmpty(mLicense))
            out += mLicense.hashCode();

        if (!TextUtils.isEmpty(mSignature))
            out += mSignature.hashCode();

        return out;
    }
}
