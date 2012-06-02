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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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

	private static String TAG = "JOB_SERVICE";
	
	public static Object sCallbackListSynchronizationObject = new Object();
	
	private static ExecutorService sJobExecutor = Executors.newFixedThreadPool(1);
	private static ExecutorService sOperationExecutor = Executors.newFixedThreadPool(1);

	private static int sSynchronousRequestsCount = 0;
	private static boolean sIsJobPending = false;
	private static boolean sJobsPresentInTheQueue = false;
	private JobProcessorManager mJobProcessorManager = new JobProcessorManager();
	private ResourceProcessorManager mResourceProcessorManager = new ResourceProcessorManager();
	
	private JobQueue mJobQueue;
	
	private Handler mHandler = null;
	
	/* The key in this map is a String version of JobID, values are lists of references to callback objects */
	private static Map<String,List<JobCallback>> mCallbacks = new HashMap<String, List<JobCallback>>();
	
	public static void addCallback(JobID jobID, JobCallback jobCallback)
	{
	synchronized(sCallbackListSynchronizationObject)
	{
		Log.d(TAG, "Adding a callback" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
				+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());
		
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
		Log.d(TAG, "Removing a callback" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
				+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());
		
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
    	Log.d(TAG, "Starting the service.");
		
		mJobQueue = new JobQueue(this);
		mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
			
			@Override
			public boolean handleMessage(Message msg) {
				if (sJobsPresentInTheQueue)
				{
					wakeUp(JobService.this);
					mHandler.postDelayed(null, 10000);
				}
				return true;
			}
		});
		
		/* Wake ourselves up every 10 seconds */
		mHandler.postDelayed(null, 10000);
	}

	@Override
	public IBinder onBind(final Intent intent) {
		/* We're not binding to this service */
		return null;
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		
		Log.d(TAG, "> onStartCommand()");
		
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
			boolean networkStateOK = true;
			
			WifiManager wifimanager = (WifiManager)getSystemService(Context.WIFI_SERVICE);  
			if (wifimanager.isWifiEnabled())
			{
				ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				
				if (!wifi.isConnected()) {
					Log.d(TAG, "WIFI is enabled but not connected, no job will be executed");
					networkStateOK = false;
				}	
				else
				{
					Log.d(TAG, "WIFI is enabled and connected");
				}
			} 
			else
			{  
			    ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			    NetworkInfo mobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			    
			    if (!mobile.isConnected())
			    {
			    	Log.d(TAG, "WIFI is disabled and mobile data is not connected, no job will be executed");
			    	networkStateOK = false;
			    }
			    else
			    {
			    	Log.d(TAG, "WIFI is disabled but mobile data is connected");
			    }
			}
			
			Job job = mJobQueue.selectJob();
			if (job != null)
			{
				sJobsPresentInTheQueue = true;
				if (networkStateOK)
				{
					executeJob(job);
				}
			}
			else
			{
				sJobsPresentInTheQueue = false;
		    	if (sSynchronousRequestsCount == 0)
		    	{
			    	Log.d(TAG, "Stopping the service");
		    		/* We have no jobs in the queue and we don't take care of any synchronous requests. Stop the service. */
		    		this.stopSelf();
		    	}
			}
		}
		else
		{
			Log.d(TAG, "A job is already pending, won't select a new one.");
		}
		
		Log.d(TAG, "< onStartCommand()");
		
		return super.onStartCommand(intent, flags, startId);
	}

	private void notifyListeners(Job job)
	{
	synchronized(sCallbackListSynchronizationObject)
	{
		List<JobCallback> list = mCallbacks.get(job.getJobID().toString());
		if (list != null)
		{
			Log.d(TAG, "Notifying listeners (count="+list.size()+") " + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
					+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());
			
			for (int i=0; i < list.size(); i++)
			{
				list.get(i).onJobStateChange(job);
			}
		}
	}
	}
	
	private void executeJob(final Job job) {
		sIsJobPending = true;
		Log.d(TAG, "Executing a job" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
			+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());
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
								Log.d(TAG, "Upload Progress " + progress + "/"+ max);
								job.setProgressPercentage(progress*100 / max);
								JobCacheManager.store(job);
								notifyListeners(job);
							}
						});
					}
					
					Log.d(TAG, "JOB STARTED" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
							+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());
					mJobProcessorManager.process(JobService.this, job);
					Log.d(TAG, "JOB FINISHED" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
							+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());
					
				} catch (RuntimeException e) {
					if (job.getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE)
					{
						job.setProgressPercentage(0);
					}
					
					job.setException(e);
					Log.logCaughtException(e);
					mJobQueue.handleProcessedJob(job);
					
					Log.d(TAG, "JOB FAILED, no job is pending anymore" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
							+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());
					notifyListeners(job);
					
					if (job.getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE)
						mJobProcessorManager.getImageProcessorInstance().setCallback(null);
					
					new Handler(Looper.getMainLooper()).post(new Runnable() {
						@Override
						public void run() {
							sIsJobPending = false;
							wakeUp(JobService.this);
						}
					});
					return;
				}
				job.setFinished(true);
				mJobQueue.handleProcessedJob(job);
				
				Log.d(TAG, "JOB SUCCESSFUL, no job is pending anymore" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
						+ " prodID=" + job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());
				
				notifyListeners(job);
				
				if (job.getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE)
					mJobProcessorManager.getImageProcessorInstance().setCallback(null);
				
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						sIsJobPending = false;
						wakeUp(JobService.this);
					}
				});
			}
		});
	}
	
	
	private void obtainResource(final Bundle requestExtras, final LoadOperation op, final Messenger messenger) {
		sSynchronousRequestsCount ++;
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
				
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						sSynchronousRequestsCount--;
						wakeUp(JobService.this);
					}
				});
			}
		});
	}
}
