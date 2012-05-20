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
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceConstants;
import com.mageventory.res.ResourceProcessorManager;
import com.mageventory.util.Log;

import com.mageventory.MageventoryConstants;
import com.mageventory.client.ImageStreaming.StreamUploadCallback;
import com.mageventory.jobprocessor.JobProcessorManager;

public class JobService extends Service implements ResourceConstants {

	private static final String TAG = "JobService";
	
	public static Object sCallbackListSynchronizationObject = new Object();
	
	private static ExecutorService sJobExecutor = Executors.newFixedThreadPool(1);
	private static ExecutorService sOperationExecutor = Executors.newFixedThreadPool(1);

	private static boolean sIsJobPending = false;
	private JobProcessorManager mJobProcessorManager = new JobProcessorManager();
	private ResourceProcessorManager mResourceProcessorManager = new ResourceProcessorManager();
	
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
		
		if (intent != null && intent.getIntExtra(EKEY_OP_REQUEST_ID, INVALID_REQUEST_ID) != INVALID_REQUEST_ID)
		{
			final Messenger messenger = (Messenger) intent.getParcelableExtra(EKEY_MESSENGER);
			
			final int operationRequestId = intent.getIntExtra(EKEY_OP_REQUEST_ID, INVALID_REQUEST_ID);
			final int resourceType = intent.getIntExtra(EKEY_RESOURCE_TYPE, RES_INVALID);
			final String[] resourceParams = (String[]) intent.getExtras().get(EKEY_PARAMS);
			
			if (resourceType != RES_INVALID) {
				obtainResource(intent.getExtras().getBundle(EKEY_REQUEST_EXTRAS), new LoadOperation(operationRequestId,
						resourceType, resourceParams), messenger);
			}
		}
		
		if (sIsJobPending == false)
		{
			Job job = mJobQueue.selectJob();
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
		
		sJobExecutor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					
					/* This is a special case for image upload. There should be no more special cases like this. */
					if (job.getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE)
					{
						mJobProcessorManager.getImageProcessorInstance().setCallback(new StreamUploadCallback() {
							
							@Override
							public void onUploadProgress(int progress, int max) {
								Log.d("Upload Progress", "" + progress + "/"+ max);
								job.setProgressPercentage(progress*100 / max);
								JobCacheManager.store(job);
								notifyListeners(job);
							}
						});
					}
					
					mJobProcessorManager.process(JobService.this, job);
					
					
				} catch (RuntimeException e) {
					job.setException(e);
					Log.logCaughtException(e);
					mJobQueue.handleProcessedJob(job);
					notifyListeners(job);
					sIsJobPending = false;
					
					if (job.getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE)
						mJobProcessorManager.getImageProcessorInstance().setCallback(null);
					
					/* Give some time to the user to actually read the error message before restarting. */
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e2) {
						Log.logCaughtException(e2);
					}
					
					wakeUp(JobService.this);
					return;
				}
				job.setFinished(true);
				mJobQueue.handleProcessedJob(job);
				notifyListeners(job);
				sIsJobPending = false;
				
				if (job.getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE)
					mJobProcessorManager.getImageProcessorInstance().setCallback(null);
				
				wakeUp(JobService.this);
			}
		});
	}
	
	
	private void obtainResource(final Bundle requestExtras, final LoadOperation op, final Messenger messenger) {
		sOperationExecutor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					final Bundle data = mResourceProcessorManager.process(getBaseContext(), op.getResourceType(),
							op.getResourceParams(), requestExtras);
					op.setExtras(data);
				} catch (RuntimeException e) {
					op.setException(e);
					Log.w(TAG, "" + e);
				}

				// reply after processing
				final Message message = Message.obtain();
				message.what = op.getOperationRequestId();
				message.obj = op;
				try {
					messenger.send(message);
				} catch (RemoteException e) {
					Log.w(TAG, "" + e);
				}
			}
		});
	}
}
