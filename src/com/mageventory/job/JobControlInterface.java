package com.mageventory.job;

import java.util.List;

import com.mageventory.MageventoryConstants;
import com.mageventory.model.Product;

import android.content.Context;

public class JobControlInterface {
	
	private Context mContext;
	private JobQueue mJobQueue;
	
	public JobControlInterface(Context context)
	{
		mContext = context;
		mJobQueue = new JobQueue(context);
	}
	
	public boolean registerJobCallback(JobID jobID, JobCallback jobCallback)
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
			JobService.addCallback(jobID, jobCallback);
			return true;
		}
		
		return false;
	}
	}
	
	/* Can pass null as jobCallback here to deregister all callbacks for a job. */
	public void deregisterJobCallback(JobID jobID, JobCallback jobCallback)
	{
		JobService.removeCallback(jobID, jobCallback);
	}
	
	public void addJob(Job job)
	{
		/* In case a job needs product id we check whether we have it in the cache. If it's there we
		 * use it. This needs to be synchronised. We have to make sure that once we establish we 
		 * don't have product id in the cache then it doesn't suddenly appear there before
		 * we manage to add a job to the queue because that would mean the job could never get the product id
		 * assigned. */
		if (job.getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE)
		{
		synchronized(JobCacheManager.mSynchronizationObject)
		{
			Product product = JobCacheManager.restoreProductDetails(job.getJobID().getSKU());
			
			if (product != null)
			{
				job.getJobID().setProductID(Integer.parseInt(product.getId()));
			}
			
			mJobQueue.add(job);
		}
		}
		else
		{
			mJobQueue.add(job);	
		}
		
		// Notify the service there is a new job in the queue
		JobService.wakeUp(mContext);
	}

	public List<Job> getAllImageUploadJobs(String SKU)
	{
		return JobCacheManager.restoreImageUploadJobs(SKU);
	}
	
	public Job getProductDetailsJob(String SKU)
	{
		return JobCacheManager.restoreProductCreationJob(SKU);
	}
}
