package com.mageventory.job;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

public class Job implements Serializable
{
	private static final long serialVersionUID = -5632314897743194416L;

	private JobID mJobID;
	
	/* In case this job is finished this field is going to contain a reference
	 * to an object containing server resopnse. The class of this object depends
	 * on the resource type (we store info about resource type in JobID). */
	private Object mServerResponse;
	
	/* Job state.
	 * If a job is finished it means there were no exceptions thrown
	 * and the job was completed successfully. If the job is not finished we can
	 * check if there are any problems by checking mException. If there are no
	 * exceptions and the job is not finished this means it's either in a queue
	 * waiting to be started or is already being processed. In both cases we just
	 * have to wait. */
	private boolean mFinished;
	private Exception mException;
	
	/* This is only used in case of image upload jobs. It can assume values in
	 * 0-100 range. */
	private int mProgressPercentage;
	
	public void setProgressPercentage(int progressPercentage)
	{
		mProgressPercentage = progressPercentage;
	}
	
	public int getProgressPercentage()
	{
		return mProgressPercentage;
	}
	
	public void setServerResponse(Object response)
	{
		mServerResponse = response;
	}
	
	public Object getServerResponse()
	{
		return mServerResponse;
	}
	
	public int getResourceType()
	{
		return mJobID.getResourceType();
	}
	
	public void putExtraInfo(String key, Object value)
	{
		mJobID.putExtraInfo(key, value);
	}
	
	public Object getExtraInfo(String key)
	{
		return mJobID.getExtraInfo(key);
	}
	
	public Map<String, Object> getExtras()
	{
		return mJobID.getExtras();
	}
	
	public String[] getParams()
	{
		return mJobID.getParams();
	}
	
	public boolean getFinished()
	{
		return mFinished;
	}
	
	public void setFinished(boolean finished)
	{
		mFinished = finished;
	}
	
	public Exception getException()
	{
		return mException;
	}
	
	public void setException(Exception exception)
	{
		mException = exception;
	}
	
	public Job(JobID jobID)
	{
		mJobID = jobID;
		mFinished = false;
		mException = null;
		mProgressPercentage = 0;
	}

	public JobID getJobID()
	{
		return mJobID;
	}
	
	/* Returns true on success. */
	public boolean serialize(File file)
	{
		FileOutputStream fos;
		ObjectOutputStream oos;
		try {
			fos = new FileOutputStream(file);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(this);
			oos.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	/* Returns something else than null on success */
	public static Job deserialize(File file)
	{
		Job out;
		FileInputStream fis;
		ObjectInputStream ois;
		
		try {
			fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);
			out = (Job) ois.readObject();
			ois.close();
		} catch (Exception e) {
			return null;
		}
		return out;
	}
}
