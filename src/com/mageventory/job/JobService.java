package com.mageventory.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.mageventory.MageventoryConstants;
import com.mageventory.client.ImageStreaming.StreamUploadCallback;

public class JobService extends Service {

	public static Object sCallbackListSynchronizationObject = new Object();
	
	private static ExecutorService sExecutor = Executors.newFixedThreadPool(1);
	private static boolean sIsJobPending = false;
	private JobProcessorManager mProcessor = new JobProcessorManager();
	
	private JobQueue mJobQueue;

	/* The key in this map is a String version of JobID, values are lists of references to callback objects */
	private static Map<String,List<JobCallback>> mCallbacks = new HashMap<String, List<JobCallback>>();
	
	public static void addCallback(JobID jobID, JobCallback jobCallback)
	{
	synchronized(sCallbackListSynchronizationObject)
	{
		List<JobCallback> list = mCallbacks.get(jobID.toString());
		
		if (list == null)
		{
			mCallbacks.put(jobID.toString(), new ArrayList<JobCallback>());
			list = mCallbacks.get(jobID.toString());
		}
		
		list.add(jobCallback);
	}
	}
	
	public static void wakeUp(Context context)
	{
		context.startService(new Intent(context, JobService.class));
	}
	
	public static void removeCallback(JobID jobID, JobCallback jobCallback)
	{
	synchronized(sCallbackListSynchronizationObject)
	{
		List<JobCallback> list = mCallbacks.get(jobID.toString());

		if (list != null)
		{
			for(int i=0; i<list.size(); i++)
			{
				if (jobCallback == null || list.get(i) == jobCallback)
				{
					list.remove(i);
					break;
				}
			}
		}
	}
	}
	
	@Override
	public void onCreate ()
	{
		mJobQueue = new JobQueue(this);
	}

	@Override
	public IBinder onBind(final Intent intent) {
		/* We're not binding to this service */
		return null;
	}


	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		if (sIsJobPending == false)
		{
			Job job = mJobQueue.getFront();
			if (job != null)
				executeJob(job);
		}
		
		return super.onStartCommand(intent, flags, startId);
	}

	private void notifyListeners(Job job)
	{
	synchronized(sCallbackListSynchronizationObject)
	{
		List<JobCallback> list = mCallbacks.get(job.getJobID().toString());
		if (list != null)
		{
			for (int i=0; i < list.size(); i++)
			{
				list.get(i).onJobStateChange(job);
			}
		}
	}
	}
	
	private void executeJob(final Job job) {
		sIsJobPending = true;
		job.setException(null);
		
		sExecutor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					
					/* This is a special case for image upload. There should be no more special cases like this. */
					if (job.getResourceType() == MageventoryConstants.RES_UPLOAD_IMAGE)
					{
						mProcessor.getImageProcessorInstance().setCallback(new StreamUploadCallback() {
							
							@Override
							public void onUploadProgress(int progress, int max) {
								Log.d("Upload Progress", "" + progress + "/"+ max);
								job.setProgressPercentage(progress*100 / max);
								JobCacheManager.store(job);
								notifyListeners(job);
							}
						});
					}
					
					mProcessor.process(JobService.this, job);
					
					
				} catch (RuntimeException e) {
					job.setException(e);
					e.printStackTrace();
					JobCacheManager.store(job);
					mJobQueue.moveFromFrontToBack();
					notifyListeners(job);
					sIsJobPending = false;
					
					if (job.getResourceType() == MageventoryConstants.RES_UPLOAD_IMAGE)
						mProcessor.getImageProcessorInstance().setCallback(null);
					
					/* Give some time to the user to actually read the error message before restarting. */
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
					
					wakeUp(JobService.this);
					return;
				}
				job.setFinished(true);
				JobCacheManager.store(job);
				mJobQueue.deleteFront();
				notifyListeners(job);
				sIsJobPending = false;
				
				if (job.getResourceType() == MageventoryConstants.RES_UPLOAD_IMAGE)
					mProcessor.getImageProcessorInstance().setCallback(null);
				
				wakeUp(JobService.this);
			}
		});
	}
}
