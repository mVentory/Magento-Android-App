package com.mageventory.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mageventory.MageventoryConstants;
import com.mageventory.model.Product;
import com.mageventory.util.Log;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class JobQueue {

	public static Object sQueueSynchronizationObject = new Object();
	
	/* Specifies how many times a job can fail before it is moved to the failed table. */
	public static final int sFailureCounterLimit = 5;

	/* DB helper creates tables we use if they are not already created and helps interface with the underlaying database. */
	private JobQueueDBHelper mDbHelper;
	
	/* Reference to the underlaying database. */
	private SQLiteDatabase mDB;

	private static String TAG = "JOB_QUEUE";

	/* Add a job to the queue. */
	public boolean add(Job job) {
		synchronized (sQueueSynchronizationObject) {
			Log.d(TAG, "Adding a job to the queue" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype="
					+ job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID() + " SKU="
					+ job.getJobID().getSKU());
			if (JobCacheManager.store(job) == true) {
				dbOpen();
				ContentValues cv = new ContentValues();
				boolean res;
				cv.put(JobQueueDBHelper.JOB_TIMESTAMP, job.getJobID().getTimeStamp());
				cv.put(JobQueueDBHelper.JOB_PRODUCT_ID, job.getJobID().getProductID());
				cv.put(JobQueueDBHelper.JOB_TYPE, job.getJobID().getJobType());
				cv.put(JobQueueDBHelper.JOB_SKU, job.getJobID().getSKU());
				cv.put(JobQueueDBHelper.JOB_ATTEMPTS, 0);
				res = insert(cv, true);

				if (res != true) {
					Log.d(TAG, "Unable to add a job to the database" + " timestamp=" + job.getJobID().getTimeStamp()
							+ " jobtype=" + job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID()
							+ " SKU=" + job.getJobID().getSKU());
					JobCacheManager.removeFromCache(job.getJobID());
				} else {
					changeSummary(job.getJobID(), true, true);
				}

				dbClose();
				return res;
			} else {
				Log.d(TAG, "Unable to store job in cache" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype="
						+ job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID() + " SKU="
						+ job.getJobID().getSKU());
			}

			return false;
		}
	}

	/* Select next job from the queue to be executed. In case the job cannot be deserialized from the cache
	 * we delete it from the queue. */
	public Job selectJob() {
		synchronized (sQueueSynchronizationObject) {
			Log.d(TAG, "Selecting next job");

			dbOpen();

			while (true) {
				Cursor c = query(new String[] { JobQueueDBHelper.JOB_TIMESTAMP, JobQueueDBHelper.JOB_PRODUCT_ID,
						JobQueueDBHelper.JOB_TYPE, JobQueueDBHelper.JOB_SKU }, JobQueueDBHelper.JOB_PRODUCT_ID
						+ "!=-1 OR " + JobQueueDBHelper.JOB_TYPE + "="
						+ MageventoryConstants.RES_CATALOG_PRODUCT_CREATE, null, JobQueueDBHelper.JOB_ATTEMPTS
						+ " ASC, " + JobQueueDBHelper.JOB_TIMESTAMP + " ASC", "0, 1", true);
				if (c.moveToFirst() == true) {
					JobID jobID = new JobID(c.getLong(c.getColumnIndex(JobQueueDBHelper.JOB_TIMESTAMP)), c.getInt(c
							.getColumnIndex(JobQueueDBHelper.JOB_PRODUCT_ID)), c.getInt(c
							.getColumnIndex(JobQueueDBHelper.JOB_TYPE)), c.getString(c
							.getColumnIndex(JobQueueDBHelper.JOB_SKU)));
					c.close();

					Log.d(TAG,
							"Selected a job" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
									+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());

					Job out = JobCacheManager.restore(jobID);

					if (out == null) {
						Log.d(TAG, "Unable to restore job from cache, will delete it and try the next one"
								+ " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType() + " prodID="
								+ jobID.getProductID() + " SKU=" + jobID.getSKU());

						dbClose();
						deleteJobFromQueue(jobID, true, true, false);
						dbOpen();
						continue;
					}
					dbClose();

					out.setException(null);
					out.setFinished(false);

					Log.d(TAG, "Job selected" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
							+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());

					out.getJobID().setProductID(jobID.getProductID());

					return out;
				}

				Log.d(TAG, "Didn't find any jobs in the queue, returning null");

				c.close();
				dbClose();
				return null;
			}
		}
	}

	/* Update product id of all jobs with a given SKU */
	private boolean updateProductID(int prodID, String SKU) {
		synchronized (sQueueSynchronizationObject) {
			Log.d(TAG, "Updating product id in the database for a given SKU," + " prodID=" + prodID + " SKU=" + SKU);

			boolean res = false;

			dbOpen();
			ContentValues cv = new ContentValues();
			cv.put(JobQueueDBHelper.JOB_PRODUCT_ID, prodID);

			res = update(cv, JobQueueDBHelper.JOB_SKU + "=?", new String[] { SKU }, true);

			if (res == false) {
				Log.d(TAG, "Updating product id unsuccessful," + " prodID=" + prodID + " SKU=" + SKU);
			} else {
				Log.d(TAG, "Updating product id successful," + " prodID=" + prodID + " SKU=" + SKU);
			}

			dbClose();

			return res;
		}
	}

	/* Called by the serivce when it wants to tell the queue it is done with the job. The queue
	 * handles this by checking whether the job succeeded or failed. If the job succeeded then if
	 * it was a product creation job then all dependent jobs are assigned product id. The job is deleted
	 * from the queue in this case. If on the other hand the job fails then it's failure counter is increased. */
	public void handleProcessedJob(Job job) {
		Log.d(TAG, "Handling a processed job" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype="
				+ job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID() + " SKU="
				+ job.getJobID().getSKU());

		/* If the job finished with success. */
		if (job.getFinished() == true) {
			Log.d(TAG, "Handling a processed job (job finished)" + " timestamp=" + job.getJobID().getTimeStamp()
					+ " jobtype=" + job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID() + " SKU="
					+ job.getJobID().getSKU());

			deleteJobFromQueue(job.getJobID(), true, false, false);

			/* If it was a product creation job. */
			if (job.getJobID().getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_CREATE) {
				Log.d(TAG, "Handling a processed job (this is a product job, will update the jobid in database)"
						+ " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
						+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());

				Product product = JobCacheManager.restoreProductDetails(job.getJobID().getSKU());

				if (product != null) {
					/* Update product id of dependent jobs. */
					updateProductID(Integer.parseInt(product.getId()), job.getJobID().getSKU());
				} else {
					Log.d(TAG,
							"Handling a processed job (new product job), unable to restore product details from cache "
									+ " timestamp=" + job.getJobID().getTimeStamp() + " jobtype="
									+ job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID()
									+ " SKU=" + job.getJobID().getSKU());
				}
			}
		/* If the job is not finished and and exception was thrown while processing it. */
		} else if (job.getException() != null) { 
			Log.d(TAG, "Handling a processed job (job failed)" + " timestamp=" + job.getJobID().getTimeStamp()
					+ " jobtype=" + job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID() + " SKU="
					+ job.getJobID().getSKU());

			increaseFailureCounter(job.getJobID());
		}
	}

	/* Increase a failure counter for a given job. If failure counter limit gets reached the jobs will be 
	 * moved to the failed table. */
	private boolean increaseFailureCounter(JobID jobID) {
		synchronized (sQueueSynchronizationObject) {
			Log.d(TAG,
					"Increasing failure counter" + " timestamp=" + jobID.getTimeStamp() + " jobtype="
							+ jobID.getJobType() + " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());

			dbOpen();

			boolean res = false;

			int currentFailureCounter = 0;

			Cursor c = query(new String[] { JobQueueDBHelper.JOB_ATTEMPTS }, JobQueueDBHelper.JOB_TIMESTAMP + "=?",
					new String[] { "" + jobID.getTimeStamp() }, null, null, true);
			if (c.moveToFirst() == true) {
				currentFailureCounter = c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_ATTEMPTS));
				ContentValues cv = new ContentValues();
				cv.put(JobQueueDBHelper.JOB_ATTEMPTS, currentFailureCounter + 1);

				Log.d(TAG,
						"Increasing failure counter, old=" + currentFailureCounter + " new="
								+ (currentFailureCounter + 1) + " timestamp=" + jobID.getTimeStamp() + " jobtype="
								+ jobID.getJobType() + " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());

				res = update(cv, JobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[] { "" + jobID.getTimeStamp() },
						true);

				if (res == false) {
					Log.d(TAG,
							"Unable to increase failure counter, old=" + currentFailureCounter + " new="
									+ (currentFailureCounter + 1) + " timestamp=" + jobID.getTimeStamp() + " jobtype="
									+ jobID.getJobType() + " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());
				} else {
					Log.d(TAG,
							"Increasing failure counter successful" + " timestamp=" + jobID.getTimeStamp()
									+ " jobtype=" + jobID.getJobType() + " prodID=" + jobID.getProductID() + " SKU="
									+ jobID.getSKU());
				}
			} else {
				Log.d(TAG,
						"Increasing failure counter problem (cannot find job in the queue)" + " timestamp="
								+ jobID.getTimeStamp() + " jobtype=" + jobID.getJobType() + " prodID="
								+ jobID.getProductID() + " SKU=" + jobID.getSKU());
			}

			c.close();
			dbClose();

			if (currentFailureCounter > sFailureCounterLimit) {
				Log.d(TAG,
						"Failure counter reached the limit, deleting job from queue" + " timestamp="
								+ jobID.getTimeStamp() + " jobtype=" + jobID.getJobType() + " prodID="
								+ jobID.getProductID() + " SKU=" + jobID.getSKU());

				deleteJobFromQueue(jobID, true, true, true);
			}

			return res;
		}
	}

	/* Move a job from failed table to pending table and reset the failure counter. */
	private boolean retryJob(JobID jobID) {
		synchronized (sQueueSynchronizationObject) {
			Log.d(TAG,
					"Trying to retry a job " + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
							+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());
			dbOpen();

			if (delete(JobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[] { "" + jobID.getTimeStamp() }, false) > 0) {
				changeSummary(jobID, false, false);

				ContentValues cv = new ContentValues();
				boolean res;
				cv.put(JobQueueDBHelper.JOB_TIMESTAMP, jobID.getTimeStamp());
				cv.put(JobQueueDBHelper.JOB_PRODUCT_ID, jobID.getProductID());
				cv.put(JobQueueDBHelper.JOB_TYPE, jobID.getJobType());
				cv.put(JobQueueDBHelper.JOB_SKU, jobID.getSKU());
				cv.put(JobQueueDBHelper.JOB_ATTEMPTS, 0);
				res = insert(cv, true);
				if (res != true) {
					Log.d(TAG,
							"Unable to add a job to the pending table" + " timestamp=" + jobID.getTimeStamp()
									+ " jobtype=" + jobID.getJobType() + " prodID=" + jobID.getProductID() + " SKU="
									+ jobID.getSKU());
					return false;
				} else {
					changeSummary(jobID, true, true);
				}
			} else {
				Log.d(TAG,
						"Unable to find job in the failed queue to delete it" + " timestamp=" + jobID.getTimeStamp()
								+ " jobtype=" + jobID.getJobType() + " prodID=" + jobID.getProductID() + " SKU="
								+ jobID.getSKU());
			}

			dbClose();

			return false;
		}
	}

	/* Delete a job from the queue. There is a number of parameters here. The job can be deleted from the pending table
	 * or from the failed table. The function can delete dependent jobs in case of product creation job or not. The
	 * job can be moved to failed table after deletion or not. */
	private boolean deleteJobFromQueue(JobID jobID, boolean fromPendingTable, boolean deleteDependendIfNewProduct,
			boolean moveToFailedTable) {
		synchronized (sQueueSynchronizationObject) {
			Log.d(TAG,
					"Trying to delete a job from queue" + " timestamp=" + jobID.getTimeStamp() + " jobtype="
							+ jobID.getJobType() + " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU()
							+ "fromPendingTable=" + fromPendingTable + " moveToFailedTable=" + moveToFailedTable);
			dbOpen();
			boolean global_res = true;
			boolean del_res;

			del_res = (delete(JobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[] { "" + jobID.getTimeStamp() },
					fromPendingTable) > 0);

			if (del_res) {
				changeSummary(jobID, fromPendingTable, false);

				if (!moveToFailedTable) {
					JobCacheManager.removeFromCache(jobID);

					Log.d(TAG,
							"Job deleted successfully from queue" + " timestamp=" + jobID.getTimeStamp() + " jobtype="
									+ jobID.getJobType() + " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU()
									+ "fromPendingTable=" + fromPendingTable + " moveToFailedTable="
									+ moveToFailedTable);
				} else {
					ContentValues cv = new ContentValues();
					boolean res;
					cv.put(JobQueueDBHelper.JOB_TIMESTAMP, jobID.getTimeStamp());
					cv.put(JobQueueDBHelper.JOB_PRODUCT_ID, jobID.getProductID());
					cv.put(JobQueueDBHelper.JOB_TYPE, jobID.getJobType());
					cv.put(JobQueueDBHelper.JOB_SKU, jobID.getSKU());
					cv.put(JobQueueDBHelper.JOB_ATTEMPTS, 0);
					res = insert(cv, false);
					if (res != true) {
						Log.d(TAG, "Unable to add a job to the failed table" + " timestamp=" + jobID.getTimeStamp()
								+ " jobtype=" + jobID.getJobType() + " prodID=" + jobID.getProductID() + " SKU="
								+ jobID.getSKU() + "fromPendingTable=" + fromPendingTable + " moveToFailedTable="
								+ moveToFailedTable);
					} else {
						changeSummary(jobID, false, true);
					}
				}

				if (jobID.getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_CREATE
						&& deleteDependendIfNewProduct == true) {
					Cursor c = query(new String[] { JobQueueDBHelper.JOB_TIMESTAMP, JobQueueDBHelper.JOB_PRODUCT_ID,
							JobQueueDBHelper.JOB_TYPE, JobQueueDBHelper.JOB_SKU }, JobQueueDBHelper.JOB_SKU + "=" + "'"
							+ jobID.getSKU() + "'", null, null, null, fromPendingTable);

					for (; c.moveToNext();) {
						JobID jobIDdependent = new JobID(c.getLong(c.getColumnIndex(JobQueueDBHelper.JOB_TIMESTAMP)),
								c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_PRODUCT_ID)), c.getInt(c
										.getColumnIndex(JobQueueDBHelper.JOB_TYPE)), c.getString(c
										.getColumnIndex(JobQueueDBHelper.JOB_SKU)));

						boolean del_res_dependent;

						del_res_dependent = (delete(JobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[] { ""
								+ jobIDdependent.getTimeStamp() }, fromPendingTable) > 0);

						if (del_res_dependent) {
							changeSummary(jobIDdependent, fromPendingTable, false);

							if (!moveToFailedTable) {
								JobCacheManager.removeFromCache(jobIDdependent);
							} else {
								ContentValues cv = new ContentValues();
								boolean res;
								cv.put(JobQueueDBHelper.JOB_TIMESTAMP, jobIDdependent.getTimeStamp());
								cv.put(JobQueueDBHelper.JOB_PRODUCT_ID, jobIDdependent.getProductID());
								cv.put(JobQueueDBHelper.JOB_TYPE, jobIDdependent.getJobType());
								cv.put(JobQueueDBHelper.JOB_SKU, jobIDdependent.getSKU());
								cv.put(JobQueueDBHelper.JOB_ATTEMPTS, 0);
								res = insert(cv, false);
								if (res != true) {
									Log.d(TAG,
											"Unable to add a job to the failed table" + " timestamp="
													+ jobIDdependent.getTimeStamp() + " jobtype="
													+ jobIDdependent.getJobType() + " prodID="
													+ jobIDdependent.getProductID() + " SKU=" + jobIDdependent.getSKU()
													+ "fromPendingTable=" + fromPendingTable + " moveToFailedTable="
													+ moveToFailedTable);
								} else {
									changeSummary(jobIDdependent, false, true);
								}
							}
						} else {

						}
					}
				}
			} else {
				global_res = false;
			}

			return global_res;
		}
	}

	public JobQueue(Context context) {
		mDbHelper = new JobQueueDBHelper(context);

		refreshJobsSummary();
	}
	
	/*==============================================================*/
	/* JobDetail related stuff. The code here can be used to get a more detailed (the job summary) information
	 * about queue state. Most of the time each JobDetail represents a single job but in case of image upload jobs
	 * we bundle them in one job detail for each SKU. */
	/*==============================================================*/
	public static class JobDetail {
		public String productName;
		/* Used only in case of sell jobs. Specifies how many items the user has choosen to sell. */
		public int soldItemsCount;
		public String SKU;
		public int jobType;
		/* TODO: We probably don't need this variable. We can get images count by checking the number of elements
		 * in the jobIdList. */
		public int imagesCount;
		public long timestamp;

		/* JobIDs of all jobs associated with this JobDetail. This can be used to remove all of them
		 * from the queue. */
		public List<JobID> jobIDList = new ArrayList<JobID>();
	}

	/* Remove all jobs associated witt a JobDetail from the queue (either from pending or failed table) */
	public void deleteJobEntries(JobDetail jobDetail, boolean fromPendingTable) {
		synchronized (sQueueSynchronizationObject) {
			for (int i = 0; i < jobDetail.jobIDList.size(); i++) {
				deleteJobFromQueue(jobDetail.jobIDList.get(i), fromPendingTable, true, false);
			}
		}
	}

	/* Move all jobs associated with a JobDetail from the failed table to the pending table. */
	public void retryJobDetail(JobDetail jobDetail) {
		synchronized (sQueueSynchronizationObject) {
			for (int i = 0; i < jobDetail.jobIDList.size(); i++) {
				retryJob(jobDetail.jobIDList.get(i));
			}
		}
	}

	/* Get a list of JobDetails for a given table (it's almost like a list of jobs
	 * but we had to create a new class for this because there was a requirement that
	 * image upload jobs have to be bundled as one) */
	public List<JobDetail> getJobDetailList(boolean pendingTable) {
		synchronized (sQueueSynchronizationObject) {
			List<JobDetail> list = new ArrayList<JobDetail>();
			dbOpen();

			Map<String, Object> imageSKUMap = new HashMap<String, Object>();

			Cursor c = query(new String[] { JobQueueDBHelper.JOB_SKU, JobQueueDBHelper.JOB_TYPE,
					JobQueueDBHelper.JOB_TIMESTAMP, JobQueueDBHelper.JOB_PRODUCT_ID }, null, null,
					JobQueueDBHelper.JOB_ATTEMPTS + " ASC, " + JobQueueDBHelper.JOB_TIMESTAMP + " ASC", null,
					pendingTable);

			for (; c.moveToNext() != false;) {
				String SKU = c.getString(c.getColumnIndex(JobQueueDBHelper.JOB_SKU));
				int type = c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_TYPE));
				int pid = c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_PRODUCT_ID));
				long timestamp = c.getLong(c.getColumnIndex(JobQueueDBHelper.JOB_TIMESTAMP));

				if (type != MageventoryConstants.RES_UPLOAD_IMAGE) {
					JobDetail detail = new JobDetail();
					detail.SKU = SKU;
					detail.jobType = type;
					detail.timestamp = timestamp;
					detail.jobIDList.add(new JobID(timestamp, pid, type, SKU));

					Job job = JobCacheManager.restoreProductCreationJob(SKU);

					if (job != null) {
						detail.productName = (String) job.getExtraInfo(MageventoryConstants.MAGEKEY_PRODUCT_NAME);
					} else {
						detail.productName = "Unable to deserialize job file.";
					}
					
					/* attach qty information to the sell job detail object */
					if (type == MageventoryConstants.RES_CATALOG_PRODUCT_SELL)
					{
						Job sellJob = JobCacheManager.restore(new JobID(timestamp, pid, type, SKU));
						
						if (sellJob != null)
						{
							detail.soldItemsCount = Integer.parseInt((String)sellJob.getExtraInfo(MageventoryConstants.MAGEKEY_PRODUCT_QUANTITY));
						}
						else
						{
							detail.soldItemsCount = -1;
						}
					}

					list.add(detail);
				} else {
					if (imageSKUMap.containsKey(SKU)) {
						JobDetail detail = (JobDetail) imageSKUMap.get(SKU);
						detail.imagesCount++;
						detail.jobIDList.add(new JobID(timestamp, pid, type, SKU));
					} else {
						JobDetail detail = new JobDetail();
						detail.SKU = SKU;
						detail.jobType = type;
						detail.imagesCount = 1;
						detail.productName = "tmp name";
						detail.timestamp = timestamp;
						detail.jobIDList.add(new JobID(timestamp, pid, type, SKU));

						List<Job> jobs = JobCacheManager.restoreImageUploadJobs(SKU);

						if (jobs != null && jobs.size() > 0) {
							detail.productName = (String) jobs.get(0).getExtraInfo(
									MageventoryConstants.MAGEKEY_PRODUCT_NAME);
						} else {
							detail.productName = "Unable to deserialize job file.";
						}

						list.add(detail);

						imageSKUMap.put(SKU, detail);
					}
				}
			}

			c.close();
			dbClose();

			return list;
		}
	}

	/*==============================================================*/
	/* Jobs summary. The stuff here can be used to retrieve and get notified asynchronously about the summary of
	 * what is the status of the job queue (how many jobs of each type are either failed or pending).
	 * Currently we show this info in the main activity. It gets refreshed automatically thanks to the callbacks
	 * mechanisms this code here provides. */
	/*==============================================================*/
	
	/* A listener that can be registered and notified about jobs summary changes. There can be just one listener registered for
	 * the entire app. */
	private static JobSummaryChangedListener mJobSummaryChangedListener;
	
	/* An object which hold all the data about the jobs summary. */
	private static JobsSummary mJobsSummary;
	
	/* Stores number of jobs for each job type. */
	public static class JobsCount {
		public int newProd;
		public int photo;
		public int edit;
		public int sell;
	}

	/* Stores number of jobs for each job type and for each database table (failed, pending) */
	public static class JobsSummary {
		public JobsCount failed;
		public JobsCount pending;
	}

	/* Interface for the listener listening on summary changed event. */
	public static interface JobSummaryChangedListener {
		void OnJobSummaryChanged(JobsSummary jobsSummary);
	}

	/* Initializing job summary before anything else from this class gets called. */
	static {
		mJobsSummary = new JobsSummary();
		mJobsSummary.pending = new JobsCount();
		mJobsSummary.failed = new JobsCount();
	}
	
	/* Count the jobs of a given type in a given table. */
	private int getJobCount(int jobType, boolean pendingJob) {
		synchronized (sQueueSynchronizationObject) {
			int count = 0;
			dbOpen();
			Cursor c = query(new String[] { JobQueueDBHelper.JOB_TIMESTAMP }, JobQueueDBHelper.JOB_TYPE + "=?",
					new String[] { "" + jobType }, null, null, pendingJob);

			count = c.getCount();

			c.close();

			dbClose();

			return count;
		}
	}

	/* Update the summary. Every time something happens in the queue that changes the jobs summary this
	 * function will be called. */
	private void changeSummary(JobID jobId, boolean pendingJobChanged, boolean added) {
		int change = (added ? 1 : -1);

		switch (jobId.getJobType()) {
		case MageventoryConstants.RES_CATALOG_PRODUCT_CREATE:
			if (pendingJobChanged) {
				mJobsSummary.pending.newProd += change;
			} else {
				mJobsSummary.failed.newProd += change;
			}
			break;
		case MageventoryConstants.RES_UPLOAD_IMAGE:
			if (pendingJobChanged) {
				mJobsSummary.pending.photo += change;
			} else {
				mJobsSummary.failed.photo += change;
			}
			break;
		case MageventoryConstants.RES_CATALOG_PRODUCT_SELL:
			if (pendingJobChanged) {
				mJobsSummary.pending.sell += change;
			} else {
				mJobsSummary.failed.sell += change;
			}
			break;	
		default:
			break;
		}

		/* Notify the listener about the change. */
		JobSummaryChangedListener listener = mJobSummaryChangedListener;
		if (listener != null) {
			listener.OnJobSummaryChanged(mJobsSummary);
		}
	}
	
	public static void setOnJobSummaryChangedListener(JobSummaryChangedListener listener) {
		mJobSummaryChangedListener = listener;

		if (listener != null) {
			listener.OnJobSummaryChanged(mJobsSummary);
		}
	}
	
	/* In cache the job summary is totally out of sync with the queue (like for example when the appliation starts)
	 * this function needs to be called. Each next change to the queue will just use changeSummary() function. */
	private void refreshJobsSummary() {
		mJobsSummary.pending.newProd = getJobCount(MageventoryConstants.RES_CATALOG_PRODUCT_CREATE, true);
		mJobsSummary.pending.photo = getJobCount(MageventoryConstants.RES_UPLOAD_IMAGE, true);
		mJobsSummary.pending.sell = getJobCount(MageventoryConstants.RES_CATALOG_PRODUCT_SELL, true);

		mJobsSummary.failed.newProd = getJobCount(MageventoryConstants.RES_CATALOG_PRODUCT_CREATE, false);
		mJobsSummary.failed.photo = getJobCount(MageventoryConstants.RES_UPLOAD_IMAGE, false);
		mJobsSummary.failed.sell = getJobCount(MageventoryConstants.RES_CATALOG_PRODUCT_SELL, false);

		/* Notify the listener about the change. */
		JobSummaryChangedListener listener = mJobSummaryChangedListener;
		if (listener != null) {
			listener.OnJobSummaryChanged(mJobsSummary);
		}
	}
	
	/*==============================================================*/
	/* DB accessors */
	/*==============================================================*/
	private int delete(String selection, String[] selectionArgs, boolean pendingTable) {
		String table = pendingTable ? JobQueueDBHelper.TABLE_PENDING_NAME : JobQueueDBHelper.TABLE_FAILED_NAME;
		return mDB.delete(table, selection, selectionArgs);
	}

	private boolean insert(ContentValues values, boolean pendingTable) {
		String table = pendingTable ? JobQueueDBHelper.TABLE_PENDING_NAME : JobQueueDBHelper.TABLE_FAILED_NAME;
		final long id = mDB.insert(table, null, values);
		if (id == -1) {
			return false;
		}
		return true;
	}

	private Cursor query(String[] columns, String selection, String[] selectionArgs, String sortOrder, String limit,
			boolean pendingTable) {
		String table = pendingTable ? JobQueueDBHelper.TABLE_PENDING_NAME : JobQueueDBHelper.TABLE_FAILED_NAME;
		return mDB.query(table, columns, selection, selectionArgs, null, null, sortOrder, limit);
	}

	private Cursor query(String[] columns, String selection, String[] selectionArgs, String groupBy, String having,
			String sortOrder, String limit, boolean pendingTable) {
		String table = pendingTable ? JobQueueDBHelper.TABLE_PENDING_NAME : JobQueueDBHelper.TABLE_FAILED_NAME;
		return mDB.query(table, columns, selection, selectionArgs, groupBy, having, sortOrder, limit);
	}

	private boolean update(ContentValues values, String selection, String[] selectionArgs, boolean pendingTable) {
		String table = pendingTable ? JobQueueDBHelper.TABLE_PENDING_NAME : JobQueueDBHelper.TABLE_FAILED_NAME;
		int count = mDB.update(table, values, selection, selectionArgs);

		if (count < 1) {
			return false;
		}
		return true;
	}
	
	private void dbOpen() {
		mDB = mDbHelper.getWritableDatabase();
	}

	private void dbClose() {
		mDB.close();
	}
}
