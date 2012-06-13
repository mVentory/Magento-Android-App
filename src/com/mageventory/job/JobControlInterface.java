package com.mageventory.job;

import java.util.List;

import com.mageventory.MageventoryConstants;
import com.mageventory.job.JobQueue.JobDetail;
import com.mageventory.model.Product;

import android.content.Context;

/* Additional abstraction layer between UI and any code used for manipulating jobs (job queue, job cache manager etc.)*/
public class JobControlInterface {

	private Context mContext;
	private JobQueue mJobQueue;

	public JobControlInterface(Context context) {
		mContext = context;
		mJobQueue = new JobQueue(context);
	}

	/* Register a callback on a job to get informed about its status changes. If the job exists in the cache when
	 * this function is called the callback function is going to be called also right after the callback gets registered. */
	public boolean registerJobCallback(JobID jobID, JobCallback jobCallback) {
		/*
		 * This synchronisation is necessary here as we want to make sure we
		 * receive the callback when the job gets complete. If we didn't
		 * synchronise here the job might get finished after we check the cache
		 * but before we add the callback in which case UI would think the job
		 * is not complete and it would never receive a callback with
		 * information that it completed.
		 */
		synchronized (JobService.sCallbackListSynchronizationObject) {
			Job job = JobCacheManager.restore(jobID);

			if (job != null) {
				jobCallback.onJobStateChange(job);
				JobService.addCallback(jobID, jobCallback);
				return true;
			}

			return false;
		}
	}

	/* Can pass null as jobCallback here to deregister all callbacks for a job. */
	public void deregisterJobCallback(JobID jobID, JobCallback jobCallback) {
		JobService.removeCallback(jobID, jobCallback);
	}

	/* Add a job to the queue. If this is not a product creation job and it doesn't contain a product id
	 * then we try to restore the product details from the cache, retrieve the product id and fill the missing
	 * id in the new job. */
	public void addJob(Job job) {
		/*
		 * In case a job needs product id we check whether we have it in the
		 * cache. If it's there we use it. This needs to be synchronised. We
		 * have to make sure that once we establish we don't have product id in
		 * the cache then it doesn't suddenly appear there before we manage to
		 * add a job to the queue because that would mean the job could never
		 * get the product id assigned.
		 */
		if (job.getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE
				|| job.getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_SELL) {
			synchronized (JobQueue.sQueueSynchronizationObject) {
				Product product = JobCacheManager.restoreProductDetails(job.getJobID().getSKU());

				if (product != null) {
					job.getJobID().setProductID(Integer.parseInt(product.getId()));
				}

				mJobQueue.add(job);
			}
		} else {
			mJobQueue.add(job);
		}

		// Notify the service there is a new job in the queue
		JobService.wakeUp(mContext);
	}

	/* Get a list of all image upload jobs for a given SKU from the cache. */
	public List<Job> getAllImageUploadJobs(String SKU) {
		return JobCacheManager.restoreImageUploadJobs(SKU);
	}

	/* Get a list of all sell jobs for a given SKU from the cache. */
	public List<Job> getAllSellJobs(String SKU) {
		return JobCacheManager.restoreSellJobs(SKU);
	}

	/* ===================================================== */
	/* Operations related to JobDetail class. */
	/* ===================================================== */

	/*
	 * Return information about jobs from a given table (pending/failed) in a
	 * form of a list. The image upload jobs for a given product are getting
	 * returned as one list entry. Each list entry then provides additional list
	 * of image upload jobs that can be used to get more info about each
	 * particular job.
	 */
	public List<JobDetail> getJobDetailList(boolean pendingTable) {
		return mJobQueue.getJobDetailList(pendingTable);
	}

	/*
	 * Delete a job detail. When deleting image upload detail containing
	 * multiple image upload jobs all of them are deleted.
	 */
	public void deleteJobEntries(JobDetail jobDetail, boolean fromPendingTable) {
		mJobQueue.deleteJobEntries(jobDetail, fromPendingTable);
	}

	/*
	 * Move all of the jobs represented by a given JobDetail from the failed
	 * table to the pending table and reset their failure counter to 0.
	 */
	public void retryJobDetail(JobDetail jobDetail) {
		mJobQueue.retryJobDetail(jobDetail);
	}
}
