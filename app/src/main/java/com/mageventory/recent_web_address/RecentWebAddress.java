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

package com.mageventory.recent_web_address;

import java.util.Date;

/**
 * A POJO object which represents recent web address information stored in the
 * database
 * 
 * @author Eugene Popovich
 */
public class RecentWebAddress {
    /**
     * An unique ID in the database
     */
    private long mId;
    /**
     * The recent web address domain. Unique per profile url
     */
    private String mDomain;
    /**
     * Homw many time the domain was used for images upload
     */
    private int mAccessCount;
    /**
     * When was the last images upload action associated with the domain
     */
    private Date mLastUsed;
    /**
     * The related profile url
     */
    private String mProfileUrl;

    public RecentWebAddress() {
    }

    public RecentWebAddress(long id) {
        this.mId = id;
    }

    public String getDomain() {
        return mDomain;
    }

    public void setDomain(String domain) {
        this.mDomain = domain;
    }

    public int getAccessCount() {
        return mAccessCount;
    }

    public void setAccessCount(int count) {
        this.mAccessCount = count;
    }

    public void incrementAccessCount() {
        mAccessCount++;
    }

    public long getId() {
        return mId;
    }

    public Date getLastUsed() {
        return mLastUsed;
    }

    public void setLastUsed(Date lastUsed) {
        this.mLastUsed = lastUsed;
    }

    public String getProfileUrl() {
        return mProfileUrl;
    }

    public void setProfileUrl(String mProfileUrl) {
        this.mProfileUrl = mProfileUrl;
    }

}
