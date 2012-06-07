package com.mageventory.job;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Job implements Serializable {
	private static final long serialVersionUID = -5632314897743194416L;

	private JobID mJobID;

	/*
	 * Job state. If a job is finished it means there were no exceptions thrown
	 * and the job was completed successfully. If the job is not finished we can
	 * check if there are any problems by checking mException. If there are no
	 * exceptions and the job is not finished this means it's either in a queue
	 * waiting to be started or is already being processed. In both cases we
	 * just have to wait.
	 */
	private boolean mFinished;
	private Exception mException;

	/*
	 * This is only used in case of image upload jobs. It can assume values in
	 * 0-100 range.
	 */
	private int mProgressPercentage;

	/* Additional data needed when performing request to the server. */
	private Map<String, Object> mExtras = new HashMap<String, Object>();

	public int getJobType() {
		return mJobID.getJobType();
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

	public Job(JobID jobID) {
		mJobID = jobID;
		mFinished = false;
		mException = null;
		mProgressPercentage = 0;
	}

	public JobID getJobID() {
		return mJobID;
	}

}
