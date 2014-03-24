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

package com.mageventory.job;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.mageventory.settings.SettingsSnapshot;

/* Represents a job that can be put in the queue. */
public class Job implements Serializable {
    private static final long serialVersionUID = -5632314897743194416L;

    /*
     * Job id contains most important information about this job as well as
     * timestamp which is different for each job.
     */
    private JobID mJobID;

    /*
     * Fields describing a job state. The job can actually be only in 3
     * important states: - Job can be "not pending" which means it has been
     * moved to the "failed" table because its failure counter reached the limit
     * (it failed too many times and will not be retried unless user decides to
     * do so). If mPending field of this class is set to "false" it means the
     * job is in that state. - Job can be failed but not moved to the "failed"
     * table yet (failure limit was not reached) in which case the mPending
     * field will be set to "true", mFinished will be set to "false" and
     * mException will be something different than null. - Job can be finished
     * with success in which case mPending will be set to "true", mFinished will
     * be set to "true" and mException will be set to null. - In all other cases
     * the job has either not yet been selected from the queue for processing OR
     * is currently being processed but both of these states are not of any
     * interest for the application at the moment. If a job is in any of these
     * states the mPending will be set to "true" the mFinished will be set to
     * "false" and mException will be null.
     */
    private boolean mPending;
    private boolean mFinished;
    private transient Exception mException;

    /*
     * This is only used in case of image upload jobs. It can assume values in
     * 0-100 range.
     */
    private int mProgressPercentage;

    /* Additional data needed when performing request to the server. */
    private Map<String, Object> mExtras = new HashMap<String, Object>();

    /*
     * Sometimes we need to return some info about the server response to the
     * upper layers but we don't want to do that through the cache. This
     * variable can be used for that purpose. The interested code will register
     * a callback on the job and when the callback gets triggered then that code
     * can read this variable. Currently we don't need more than a string.
     */
    private String mResultData;

    private SettingsSnapshot mSettingsSnapshot;

    public void setResultData(String resultData)
    {
        mResultData = resultData;
    }

    public String getResultData()
    {
        return mResultData;
    }

    public int getJobType() {
        return mJobID.getJobType();
    }

    public String getSKU() {
        return mJobID.getSKU();
    }

    public String getUrl() {
        return mJobID.getUrl();
    }

    public void setProgressPercentage(int progressPercentage) {
        mProgressPercentage = progressPercentage;
    }

    public int getProgressPercentage() {
        return mProgressPercentage;
    }

    public void putExtraInfo(String key, Object value) {
        mExtras.put(key, value);
    }

    public Object getExtraInfo(String key) {
        return mExtras.get(key);
    }

    public Map<String, Object> getExtras() {
        return mExtras;
    }

    public void setExtras(Map<String, Object> extras) {
        mExtras = extras;
    }

    public void setPending(boolean pending)
    {
        mPending = pending;
    }

    public boolean getPending()
    {
        return mPending;
    }

    public boolean getFinished() {
        return mFinished;
    }

    public void setFinished(boolean finished) {
        mFinished = finished;
    }

    public Exception getException() {
        return mException;
    }

    public void setException(Exception exception) {
        mException = exception;
    }

    public Job(JobID jobID, SettingsSnapshot settingsSnapshot) {
        mJobID = jobID;
        mFinished = false;
        mException = null;
        mPending = true;
        mProgressPercentage = 0;

        mSettingsSnapshot = settingsSnapshot;
        jobID.setUrl(mSettingsSnapshot.getUrl());

        if (settingsSnapshot == null)
            throw new RuntimeException("programming error: settings snapshot is null");
    }

    public SettingsSnapshot getSettingsSnapshot()
    {
        return mSettingsSnapshot;
    }

    public JobID getJobID() {
        return mJobID;
    }

}
