package com.mageventory.job;

import java.io.File;

import com.mageventory.MageventoryConstants;
import com.mageventory.model.Product;
import com.mageventory.util.Log;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class JobQueue {

	public static Object sQueueSynchronizationObject = new Object();
	public static int sFailureCounterLimit = 5;
	
    private JobQueueDBHelper mDbHelper;
	private SQLiteDatabase mDB;
	private Context mContext;
	
	private static String TAG = "JOB_QUEUE";
	
    private void dbOpen()
    {
    	mDB = mDbHelper.getWritableDatabase();
    }
    
    private void dbClose()
    {
    	mDB.close();
    }
    
	public boolean add(Job job)
	{
	synchronized(sQueueSynchronizationObject)
	{
		Log.d(TAG, "Adding a job to the queue" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
				+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());
		if (JobCacheManager.store(job) == true)
		{
			dbOpen();
			ContentValues cv = new ContentValues();
			boolean res;
			cv.put(JobQueueDBHelper.JOB_TIMESTAMP, job.getJobID().getTimeStamp());
			cv.put(JobQueueDBHelper.JOB_PRODUCT_ID, job.getJobID().getProductID());
			cv.put(JobQueueDBHelper.JOB_TYPE, job.getJobID().getJobType());
			cv.put(JobQueueDBHelper.JOB_SKU, job.getJobID().getSKU());
			cv.put(JobQueueDBHelper.JOB_ATTEMPTS, 0);
			res = insert(cv);
			
			if (res != true)
			{
				Log.d(TAG, "Unable to add a job to the database" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
						+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());
				JobCacheManager.removeFromCache(job.getJobID());
			}
			dbClose();
			return res;
		}
		else
		{
			Log.d(TAG, "Unable to store job in cache" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
					+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());	
		}
		
		return false;
	}
	}

	public Job selectJob()
    {
		Log.d(TAG, "Selecting next job");
		
		WifiManager wifimanager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);  
		if (wifimanager.isWifiEnabled())
		{
			ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			
			if (!wifi.isConnected()) {
				Log.d(TAG, "WIFI is enabled but not connected, returning null");
				return null;
			}	
			else
			{
				Log.d(TAG, "WIFI is enabled and connected");
			}
		} 
		else
		{  
		    ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		    NetworkInfo mobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		    
		    if (!mobile.isConnected())
		    {
		    	Log.d(TAG, "WIFI is disabled and mobile data is not connected, returning null");
		    	return null;
		    }
		    else
		    {
		    	Log.d(TAG, "WIFI is disabled but mobile data is connected");
		    }
		}  

	synchronized(sQueueSynchronizationObject)
    {
		dbOpen();
		
		while(true)
		{
			Cursor c = 
				query( new String[] {JobQueueDBHelper.JOB_TIMESTAMP, JobQueueDBHelper.JOB_PRODUCT_ID, JobQueueDBHelper.JOB_TYPE, JobQueueDBHelper.JOB_SKU},
				JobQueueDBHelper.JOB_PRODUCT_ID + "!=-1 OR " + JobQueueDBHelper.JOB_TYPE + "=0", null, JobQueueDBHelper.JOB_ATTEMPTS + " ASC, " + JobQueueDBHelper.JOB_TIMESTAMP + " ASC");
			if (c.moveToFirst() == true)
			{
				JobID jobID = new JobID(
					c.getLong(c.getColumnIndex(JobQueueDBHelper.JOB_TIMESTAMP)),
					c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_PRODUCT_ID)),
					c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_TYPE)),
					c.getString(c.getColumnIndex(JobQueueDBHelper.JOB_SKU))
				);
				c.close();
				
				Log.d(TAG, "Selected a job" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
						+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());	
				
				Job out = JobCacheManager.restore(jobID);
				
				if (out == null)
				{
					Log.d(TAG, "Unable to restore job from cache, will delete it and try the next one" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
							+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());
					
					dbClose();
					deleteJobFromQueue(jobID);
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
	
	private boolean updateProductID(int prodID, String SKU)
	{
	synchronized(sQueueSynchronizationObject)
	{
		Log.d(TAG, "Updating product id in the database for a given SKU," + " prodID=" + prodID + " SKU=" + SKU);
		
		boolean res = false;
		
		dbOpen();
		ContentValues cv = new ContentValues();
   		cv.put(JobQueueDBHelper.JOB_PRODUCT_ID, prodID);
   		
		res = update(cv, JobQueueDBHelper.JOB_SKU + "=?", new String[] {SKU});
		
		if (res==false)
		{
			Log.d(TAG, "Updating product id unsuccessful," + " prodID=" + prodID + " SKU=" + SKU);
		}
		else
		{
			Log.d(TAG, "Updating product id successful," + " prodID=" + prodID + " SKU=" + SKU);
		}
		
		dbClose();
		
		return res;
	}
	}
	
    public void handleProcessedJob(Job job)
    {
    	Log.d(TAG, "Handling a processed job" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
				+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());
    	
    	if (job.getFinished() == true)
    	{
    		Log.d(TAG, "Handling a processed job (job finished)" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
    				+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());
    		
    		deleteJobFromQueue(job.getJobID());
    		
    		if (job.getJobID().getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_CREATE)
    		{
    			Log.d(TAG, "Handling a processed job (this is a product job, will update the jobid in database)" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
        				+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());
    			
    			Product product = JobCacheManager.restoreProductDetails(job.getJobID().getSKU());
    			
    			if (product != null)
    			{
    				updateProductID(Integer.parseInt(product.getId()), job.getJobID().getSKU());
    			}
    			else
    			{
    				Log.d(TAG, "Handling a processed job (new product job), unable to restore product details from cache " + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
    	    				+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());
    			}
    		}
    	}
    	else
    	if (job.getException() != null)
    	{
        	Log.d(TAG, "Handling a processed job (job failed)" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
    				+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());
    		
    		increaseFailureCounter(job.getJobID());
    	}
    }
    
    private boolean increaseFailureCounter(JobID jobID)
    {
    synchronized(sQueueSynchronizationObject)
    {
    	Log.d(TAG, "Increasing failure counter" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
				+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());
    	
    	dbOpen();
    	
    	boolean res = false;
    	
    	int currentFailureCounter = 0;
    	
    	Cursor c = 
			query( new String[] {JobQueueDBHelper.JOB_ATTEMPTS},
				JobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[] {""+jobID.getTimeStamp()}, null);
		if (c.moveToFirst() == true)
		{
			currentFailureCounter = c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_ATTEMPTS));
			ContentValues cv = new ContentValues();
	   		cv.put(JobQueueDBHelper.JOB_ATTEMPTS, currentFailureCounter + 1);
	   		
	   		Log.d(TAG, "Increasing failure counter, old=" + currentFailureCounter + " new="+(currentFailureCounter+1) + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
					+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());
	   		
			res = update(cv, JobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[] {""+jobID.getTimeStamp()});
			
			if (res == false)
			{
				Log.d(TAG, "Unable to increase failure counter, old=" + currentFailureCounter + " new="+(currentFailureCounter+1) + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
						+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());	
			}
			else
			{
				Log.d(TAG, "Increasing failure counter successful" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
						+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());
			}
		}
		else
		{
			Log.d(TAG, "Increasing failure counter problem (cannot find job in the queue)" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
					+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());
		}

		c.close();
		dbClose();
		
		if (currentFailureCounter > sFailureCounterLimit)
		{
			Log.d(TAG, "Failure counter reached the limit, deleting job from queue" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
					+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());
			
			deleteJobFromQueue(jobID);
		}
		
		return res;
    }
    }
    
    private boolean deleteJobFromQueue(JobID jobID)
    {
    synchronized(sQueueSynchronizationObject)
    {
    	Log.d(TAG, "Trying to delete a job from queue" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
				+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());
		dbOpen();
		
   		if (delete(JobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[]{"" + jobID.getTimeStamp()}) > 0)
   		{
   			JobCacheManager.removeFromCache(jobID);
   			dbClose();
   			
   			Log.d(TAG, "Job deleted successfully from queue" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
   					+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());
   			return true;
   		}
   		
   		Log.d(TAG, "Unable to find job in the queue to delete it" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
				+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());
   		
    	dbClose();
   		return false;
    }
    }
    
    private int delete(String selection, String[] selectionArgs)
    {
    	return mDB.delete(JobQueueDBHelper.TABLE_NAME, selection, selectionArgs);
    }

    private boolean insert(ContentValues values)
    {
        final long id = mDB.insert(JobQueueDBHelper.TABLE_NAME, null, values);
        if (id == -1) {
            return false;
        }
        return true;
    }

    public JobQueue(Context context)
    {
    	mDbHelper = new JobQueueDBHelper(context);
    	mContext = context;
    }

    private Cursor query(String[] columns, String selection, String[] selectionArgs, String sortOrder)
    {
        return mDB.query(JobQueueDBHelper.TABLE_NAME, columns, selection, selectionArgs, null, null, sortOrder, "0, 1");
    }
  
    private boolean update(ContentValues values, String selection, String[] selectionArgs)
    {
        int count = mDB.update(JobQueueDBHelper.TABLE_NAME, values, selection, selectionArgs);
        
        if (count < 1)
        {
        	return false;
        }
        return true;
    }
}
