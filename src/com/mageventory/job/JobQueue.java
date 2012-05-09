package com.mageventory.job;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/* TODO: The values in order column currently grow towards infinity, we have to be making them smaller
 * periodically. */
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
		dbOpen();
		String jobFilePath = JobCacheManager.getFilePathAssociatedWithJob(job.getJobID());
		
		/* Make sure we don't have this job in the queue already. */
		Cursor c = query( new String[] {JobQueueDBHelper.JOB_FILE_PATH}, JobQueueDBHelper.JOB_FILE_PATH + "=?",
			new String[] {jobFilePath}, null);
		
		if (c.getCount() > 0)
		{
			c.close();
			dbClose();
			return false;
		}
		c.close();
		
		/* Store job in cache */
		if (JobCacheManager.store(job) == true)
		{
			/* Add a job to the database */
			int job_order_value = 1;
			c = query( new String[] {JobQueueDBHelper.JOB_ORDER}, null, null, JobQueueDBHelper.JOB_ORDER + " DESC");
			if (c.moveToFirst() == true)
			{
				job_order_value = c.getInt(0) + 1;
			}
			c.close();
			
			ContentValues cv = new ContentValues();
			boolean res;
			cv.put(JobQueueDBHelper.JOB_ORDER, job_order_value);
			cv.put(JobQueueDBHelper.JOB_FILE_PATH, jobFilePath);
			res = insert(cv);
			
			if (res != true)
			{
				JobCacheManager.removeFromCache(job.getJobID());
			}
			dbClose();
			return res;
		}
		
		dbClose();
		return false;
	}
	}
    
    public Job getFront()
    {
    synchronized(sQueueSynchronizationObject)
    {
		dbOpen();
		
		while(true)
		{
			Cursor c = query( new String[] {JobQueueDBHelper.JOB_FILE_PATH}, null, null, JobQueueDBHelper.JOB_ORDER + " ASC");
			if (c.moveToFirst() == true)
			{
				String jobFilePath = c.getString(0);
				c.close();
				Job out = Job.deserialize(new File(jobFilePath));
				if (out == null)
				{
					boolean del_res;
					del_res = deleteFront();
					
					if (del_res == false)
					{
						dbClose();
						return null;
					}
					continue;
				}	
				dbClose();
				return out;
			}
			c.close();
			dbClose();
			return null;
		}
    }
    }
    
    public boolean deleteFront()
    {
    synchronized(sQueueSynchronizationObject)
    {
		dbOpen();
    	int job_order_value = -1;
    	Cursor c = query( new String[] {JobQueueDBHelper.JOB_ORDER}, null, null, JobQueueDBHelper.JOB_ORDER + " ASC");
    	if (c.moveToFirst() == true)
    	{
    		job_order_value = c.getInt(0);
    	}
    	c.close();
    	
    	if (job_order_value != -1)
    	{
    		if (delete(JobQueueDBHelper.JOB_ORDER + "=?", new String[]{"" + job_order_value}) > 0)
    		{
    			dbClose();
    			return true;
    		}
    	}
    	dbClose();
   		return false;
    }
    }
    
    /* Moves a job from the front to the back of the queue. This can be used for
     * example in case when a job failed and we want to retry it later. */
    public boolean moveFromFrontToBack()
    {
		dbOpen();
    	int first_order, last_order;
    	
    	Cursor c = query( new String[] {JobQueueDBHelper.JOB_ORDER}, null, null, JobQueueDBHelper.JOB_ORDER + " ASC");
    	if (c.moveToFirst() == true)
    	{
    		first_order = c.getInt(0);
    	}
    	else
    	{
    		c.close();
    		dbClose();
    		return false;
    	}
    	c.close();
    	
    	c = query( new String[] {JobQueueDBHelper.JOB_ORDER}, null, null, JobQueueDBHelper.JOB_ORDER + " DESC");
    	if (c.moveToFirst() == true)
    	{
    		last_order = c.getInt(0);
    	}
    	else
    	{
    		c.close();
    		dbClose();
    		return false;
    	}
    	c.close();    	
    	
    	ContentValues cv = new ContentValues();
   		cv.put(JobQueueDBHelper.JOB_ORDER, last_order + 1);
   		
		boolean res = update(cv, JobQueueDBHelper.JOB_ORDER + "=?", new String[] {""+first_order});
		dbClose();
		return res;
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
        return mDB.query(JobQueueDBHelper.TABLE_NAME, columns, selection, selectionArgs, null, null, sortOrder);
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
