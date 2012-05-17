package com.mageventory.job;

import java.util.List;

import android.content.Context;

public class JobControlInterface {
	
	private Context mContext;
	private JobQueue mJobQueue;
	
	public JobControlInterface(Context context)
	{
		mContext = context;
		mJobQueue = new JobQueue(context);
	}
	
	public void registerJobCallback(JobID jobID, JobCallback jobCallback)
	{
	/* This synchronisation is necessary here as we want to make sure we receive the
	 * callback when the job gets complete. If we didn't synchronise here the job might
	 * get finished after we check the cache but before we add the callback in which
	 * case UI would think the job is not complete and it would never receive a callback
	 * with information that it completed. */
	synchronized(JobService.sCallbackListSynchronizationObject)
	{
		Job job = JobCacheManager.restore(jobID);
		
		if (job != null)
		{
			jobCallback.onJobStateChange(job);	
		}
		
		JobService.addCallback(jobID, jobCallback);
	}
	}
	
	/* Can pass null as jobCallback here to deregister all callbacks for a job. */
	public void deregisterJobCallback(JobID jobID, JobCallback jobCallback)
	{
		JobService.removeCallback(jobID, jobCallback);
	}
	
	public void addJob(Job job)
	{
		mJobQueue.add(job);
		
		// Notify the service there is a new job in the queue
		JobService.wakeUp(mContext);
	}
	
	public void removeFromCache(JobID jobID)
	{
		JobCacheManager.removeFromCache(jobID);
	}
	
	public List<Job> getAllImageUploadJobs(String SKU)
	{
		return JobCacheManager.restoreImageUploadJobs(SKU);
	}

}
