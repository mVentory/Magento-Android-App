package com.mageventory.job;

import java.io.File;

import com.mageventory.MageventoryConstants;
import com.mageventory.model.Product;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class JobQueue {

	public static Object sQueueSynchronizationObject = new Object();
	
    private JobQueueDBHelper mDbHelper;
	private SQLiteDatabase mDB;
	
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
		if (job.getJobID().getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE)
		{
			
		}
		
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
				JobCacheManager.removeFromCache(job.getJobID());
			}
			dbClose();
			return res;
		}
		
		return false;
	}
	}

	public Job selectJob()
    {
    synchronized(sQueueSynchronizationObject)
    {
		dbOpen();
		
		while(true)
		{
			Cursor c = 
				query( new String[] {JobQueueDBHelper.JOB_TIMESTAMP, JobQueueDBHelper.JOB_PRODUCT_ID, JobQueueDBHelper.JOB_TYPE, JobQueueDBHelper.JOB_SKU},
				JobQueueDBHelper.JOB_PRODUCT_ID + "!=-1 OR " + JobQueueDBHelper.JOB_TYPE + "=0", null, JobQueueDBHelper.JOB_ATTEMPTS + " ASC, " + JobQueueDBHelper.JOB_TIMESTAMP + " DESC");
			if (c.moveToFirst() == true)
			{
				JobID jobID = new JobID(
					c.getLong(c.getColumnIndex(JobQueueDBHelper.JOB_TIMESTAMP)),
					c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_PRODUCT_ID)),
					c.getInt(c.getColumnIndex(JobQueueDBHelper.JOB_TYPE)),
					c.getString(c.getColumnIndex(JobQueueDBHelper.JOB_SKU))
				);
				c.close();
				Job out = JobCacheManager.restore(jobID);
				
				if (out == null)
				{
					dbClose();
					deleteJobFromQueue(jobID);
					dbOpen();
					continue;
				}	
				dbClose();
				
				out.setException(null);
				out.setFinished(false);
				
				out.getJobID().setProductID(jobID.getProductID());
				
				return out;
			}
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
		boolean res = false;
		
		dbOpen();
		ContentValues cv = new ContentValues();
   		cv.put(JobQueueDBHelper.JOB_PRODUCT_ID, prodID);
   		
		res = update(cv, JobQueueDBHelper.JOB_SKU + "=?", new String[] {SKU});
		
		dbClose();
		
		return res;
	}
	}
	
    public void handleProcessedJob(Job job)
    {
    	if (job.getFinished() == true)
    	{
    		deleteJobFromQueue(job.getJobID());
    		
    		if (job.getJobID().getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_CREATE)
    		{
    			Product product = JobCacheManager.restoreProductDetails(job.getJobID().getSKU());
    			
    			if (product != null)
    			{
    				updateProductID(Integer.parseInt(product.getId()), job.getJobID().getSKU());
    			}
    		}
    	}
    	else
    	if (job.getException() != null)
    	{
    		increaseFailureCounter(job.getJobID());
    	}
    }
    
    private boolean increaseFailureCounter(JobID jobID)
    {
    synchronized(sQueueSynchronizationObject)
    {
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
	   		
			res = update(cv, JobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[] {""+jobID.getTimeStamp()});
		}

		c.close();
		dbClose();
		
		return res;
    }
    }
    
    private boolean deleteJobFromQueue(JobID jobID)
    {
    synchronized(sQueueSynchronizationObject)
    {
		dbOpen();
   		if (delete(JobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[]{"" + jobID.getTimeStamp()}) > 0)
   		{
   			JobCacheManager.removeFromCache(jobID);
   			dbClose();
   			return true;
   		}
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
    }

    private Cursor query(String[] columns, String selection, String[] selectionArgs, String sortOrder)
    {
        return mDB.query(JobQueueDBHelper.TABLE_NAME, columns, selection, selectionArgs, null, null, sortOrder, "0, 1");
    }
  
    public boolean update(ContentValues values, String selection, String[] selectionArgs)
    {
        int count = mDB.update(JobQueueDBHelper.TABLE_NAME, values, selection, selectionArgs);
        
        if (count < 1)
        {
        	return false;
        }
        return true;
    }
}
