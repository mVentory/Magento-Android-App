package com.mageventory.job;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import android.text.TextUtils;

public class ExternalImagesJobQueue {

	public static Object sQueueSynchronizationObject = new Object();
	
	/* Specifies how many times a job can fail before it is discarded. */
	public static final int sFailureCounterLimit = 5;

	/* DB helper creates tables we use if they are not already created and helps interface with the underlying database. */
	private ExternalImagesJobQueueDBHelper mDbHelper;
	
	/* Reference to the underlying database. */
	private SQLiteDatabase mDB;

	private static String TAG = "EXTERNAL_IMAGES_JOB_QUEUE";

	/* Add a job to the queue. */
	public boolean add(ExternalImagesJob job) {
		synchronized (sQueueSynchronizationObject) {
			Log.d(TAG, "Adding a job to the queue: " + job.toString());
			dbOpen();
			ContentValues cv = new ContentValues();
			boolean res;
			
			cv.put(ExternalImagesJobQueueDBHelper.JOB_TIMESTAMP, job.mTimestamp);
			cv.put(ExternalImagesJobQueueDBHelper.JOB_PRODUCT_CODE, job.mProductCode);
			
			if (job.mSKU != null)
			{
				cv.put(ExternalImagesJobQueueDBHelper.JOB_PRODUCT_SKU, job.mSKU);
			}
			
			cv.put(ExternalImagesJobQueueDBHelper.JOB_ATTEMPTS_COUNT, job.mAttemptsCount);
			cv.put(ExternalImagesJobQueueDBHelper.JOB_PROFILE_ID, job.mProfileID);
			res = insert(cv);

				if (res != true) {
					
				} else {
				}

			dbClose();
			return res;
		}
	}

	public ExternalImagesJob selectJob() {
		synchronized (sQueueSynchronizationObject) {
			Log.d(TAG, "Selecting next job");

			dbOpen();
			Cursor c;
				
			c = query(new String[] { ExternalImagesJobQueueDBHelper.JOB_TIMESTAMP, ExternalImagesJobQueueDBHelper.JOB_PRODUCT_CODE,
				ExternalImagesJobQueueDBHelper.JOB_PRODUCT_SKU, ExternalImagesJobQueueDBHelper.JOB_PROFILE_ID,
				ExternalImagesJobQueueDBHelper.JOB_ATTEMPTS_COUNT}, null, null, ExternalImagesJobQueueDBHelper.JOB_TIMESTAMP + " ASC", "0, 1");
				
			if (c.moveToFirst() == true) {
				ExternalImagesJob job = new ExternalImagesJob(
						c.getLong(c.getColumnIndex(ExternalImagesJobQueueDBHelper.JOB_TIMESTAMP)),
						c.getString(c.getColumnIndex(ExternalImagesJobQueueDBHelper.JOB_PRODUCT_CODE)),
						c.getString(c.getColumnIndex(ExternalImagesJobQueueDBHelper.JOB_PRODUCT_SKU)),
						c.getLong(c.getColumnIndex(ExternalImagesJobQueueDBHelper.JOB_PROFILE_ID)),
						c.getInt(c.getColumnIndex(ExternalImagesJobQueueDBHelper.JOB_ATTEMPTS_COUNT)));

				Log.d(TAG, "Selected a job: " + job.toString());
				
				c.close();
				dbClose();
				return job;
			}

			Log.d(TAG, "Didn't find any jobs in the queue, returning null");

			c.close();
			dbClose();
			return null;
		}
	}
	
	private boolean isEmpty()
	{
		Cursor cur = mDB.rawQuery("SELECT COUNT(*) FROM " + ExternalImagesJobQueueDBHelper.TABLE_NAME, null);
		if (cur != null) {
		    cur.moveToFirst();                       // Always one row returned.
		    if (cur.getInt (0) == 0) {               // Zero count means empty table.
		    	return true;
		    }
		    else
		    {
		    	return false;
		    }
		}
		
		return false;
	}

	public boolean isTableEmpty()
	{
		dbOpen();
		boolean out = isEmpty();
		dbClose();
		
		return out;
	}
	
	
	public boolean deleteJobFromQueue(ExternalImagesJob job) {
		synchronized (sQueueSynchronizationObject) {
			Log.d(TAG, "Trying to delete a job from queue " + job.toString());
			
			dbOpen();
			boolean del_res;

			/* Delete the specified job from the queue */
			del_res = (delete(ExternalImagesJobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[] { "" + job.mTimestamp }) > 0);

			if (del_res) {
				Log.d(TAG, "Job removed: " + job.toString());
			}

			dbClose();

			//TODO: delete images
			
			return del_res;
		}
	}
	
	
	public void handleProcessedJob(ExternalImagesJob job, boolean success) {
		
		if (success) {
			deleteJobFromQueue(job);
		}
		else
		{ 
			increaseFailureCounter(job);
		}
	}

	private boolean increaseFailureCounter(ExternalImagesJob job) {
		synchronized (sQueueSynchronizationObject) {
			
			Log.d(TAG, "Increasing failure counter: " + job.toString());

			dbOpen();

			boolean res = false;

			int currentFailureCounter = 0;

			Cursor c = query(new String[] { ExternalImagesJobQueueDBHelper.JOB_ATTEMPTS_COUNT }, ExternalImagesJobQueueDBHelper.JOB_TIMESTAMP + "=?",
					new String[] { "" + job.mTimestamp }, null, null);
			
			if (c.moveToFirst() == true) {
				currentFailureCounter = c.getInt(c.getColumnIndex(ExternalImagesJobQueueDBHelper.JOB_ATTEMPTS_COUNT));
				ContentValues cv = new ContentValues();
				cv.put(ExternalImagesJobQueueDBHelper.JOB_ATTEMPTS_COUNT, currentFailureCounter + 1);

				Log.d(TAG,
						"Increasing failure counter, old=" + currentFailureCounter + " new="
								+ (currentFailureCounter + 1) + " " + job.toString());

				res = update(cv, ExternalImagesJobQueueDBHelper.JOB_TIMESTAMP + "=?", new String[] { "" + job.mTimestamp });

				
			}

			c.close();
			dbClose();

			if (currentFailureCounter > sFailureCounterLimit) {
				Log.d(TAG,
						"Failure counter reached the limit, deleting job from queue" + job.toString());
				
				deleteJobFromQueue(job);
			}

			return res;
		}
	}

	/* Wipe all data from both tables. Use only if there are no jobs currently being executed. */
	public void wipeTable()
	{
		synchronized (sQueueSynchronizationObject) {
			dbOpen();
			delete(null, null);
			dbClose();
		}
	}

	public ExternalImagesJobQueue(Context context) {
		mDbHelper = new ExternalImagesJobQueueDBHelper(context);
	}
	
	private boolean insert(ContentValues values) {
		final long id = mDB.insert(ExternalImagesJobQueueDBHelper.TABLE_NAME, null, values);
		if (id == -1) {
			return false;
		}
		return true;
	}
	
	private int delete(String selection, String[] selectionArgs) {
		return mDB.delete(ExternalImagesJobQueueDBHelper.TABLE_NAME, selection, selectionArgs);
	}

	private Cursor query(String[] columns, String selection, String[] selectionArgs, String sortOrder, String limit) {
		return mDB.query(ExternalImagesJobQueueDBHelper.TABLE_NAME, columns, selection, selectionArgs, null, null, sortOrder, limit);
	}

	private Cursor query(String[] columns, String selection, String[] selectionArgs, String groupBy, String having,
			String sortOrder, String limit) {
		return mDB.query(ExternalImagesJobQueueDBHelper.TABLE_NAME, columns, selection, selectionArgs, groupBy, having, sortOrder, limit);
	}

	private boolean update(ContentValues values, String selection, String[] selectionArgs) {
		int count = mDB.update(ExternalImagesJobQueueDBHelper.TABLE_NAME, values, selection, selectionArgs);

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
