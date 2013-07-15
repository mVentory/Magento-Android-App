package com.mageventory.job;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
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
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;

import com.mageventory.MageventoryConstants;
import com.mageventory.client.ImageStreaming.StreamUploadCallback;
import com.mageventory.jobprocessor.JobProcessorManager;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceConstants;
import com.mageventory.res.ResourceProcessorManager;
import com.mageventory.resprocessor.ProductDetailsProcessor.ProductDetailsLoadException;
import com.mageventory.settings.Settings;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.ExternalImageUploader;
import com.mageventory.util.Log;

/* A that in the future will be used to process all requests to the server (At the moment we have two services and
 * we are in the process of moving functionality from the other one into this one) */
public class JobService extends Service implements ResourceConstants {

	private static String TAG = "JOB_SERVICE";

	/* Used to keep wifi and processor working */
	private WifiLock wifiLock;
	private WakeLock wakeLock;

	/*
	 * An object used for access synchronisation when adding or deleting a
	 * callback from the list.
	 */
	public static Object sCallbackListSynchronizationObject = new Object();

	/*
	 * An executor which creates a single background thread where a job can be
	 * performed. In this case "a job" means a request to the server which is
	 * being put in the queue so that it survives application crash, phone
	 * reboot etc. Not all requests to the server are treated as "jobs".
	 */
	private static ExecutorService sJobExecutor = Executors.newFixedThreadPool(1);

	/*
	 * A second executor which is used to process requests which are not being
	 * put in the queue which means they will not get resumed when the process
	 * is killed and then rerun. This executor also contains a queue which we
	 * actually use (as opposed to sJobExecutor for which we created our own
	 * queue in a form of a database)
	 */
	private static ExecutorService sOperationExecutor = Executors.newFixedThreadPool(1);

	/*
	 * Stores a number of requests queued in the sOperationExecutor. We need
	 * this because we don't want to shut down the service if it's in the middle
	 * of processing something.
	 */
	private static int sSynchronousRequestsCount = 0;

	/*
	 * Specifies whether there is a job pending at the moment. This is needed so
	 * that we don't launch another job if one is already being executed. We
	 * also use it when checking if the service can be shut down or not.
	 */
	private static boolean sIsJobPending = false;

	/*
	 * If there are jobs in the queue but there is no internet connection then
	 * we're rechecking the connection in regular intervals of time. We don't
	 * want to recheck the connection if there are no jobs in the queue which is
	 * why we need this variable.
	 */
	private static boolean sJobsPresentInTheQueue = false;

	/*
	 * A processor manager which contains a process() method which takes a job
	 * as a parameter. It forwards the job to the proper class from the
	 * "jobprocessor" package for processing.
	 */
	private JobProcessorManager mJobProcessorManager = new JobProcessorManager();

	/*
	 * A processor manager which contains a process() method which takes care of
	 * all requests which are not being kept track of in the job queue. It
	 * forwards the job to the proper class from the "resprocessor" package for
	 * processing.
	 */
	private ResourceProcessorManager mResourceProcessorManager = new ResourceProcessorManager();

	/* Obvious. */
	private JobQueue mJobQueue;
	private ExternalImagesJobQueue mExternalImagesJobQueue;

	/*
	 * Used to recheck internet connection and job queue if for any reason the
	 * service is not doing anything and there are jobs in the queue and there
	 * is internet connection.
	 */
	private Handler mHandler = null;

	/*
	 * A map of callbacks. It can store a list of callbacks for every job
	 * currently present in the queue so that the interested code can get
	 * notified when a job finishes or whether there is an error in execution.
	 * The key in this map is a String version of JobID, values are lists of
	 * references to callback objects
	 */
	private static Map<String, List<JobCallback>> mCallbacks = new HashMap<String, List<JobCallback>>();
	
	private static boolean mServiceRunning;
	
	/* A reference to a listener which is going to be informed whenever service is stopped or started. There can be at most
	 * one such listener registered (we don't need more at this stage) */
	public static interface OnJobServiceStateChangedListener
	{
		void onJobServiceStateChanged(boolean running);
	}
	
	private static OnJobServiceStateChangedListener mOnJobServiceStateChangedListener;

	public static void registerOnJobServiceStateChangedListener(OnJobServiceStateChangedListener listener)
	{
		mOnJobServiceStateChangedListener = listener;
		listener.onJobServiceStateChanged(mServiceRunning);
	}
	
	public static void deregisterOnJobServiceStateChangedListener()
	{
		mOnJobServiceStateChangedListener = null;
	}
	
	/* Add a callback to the list for a particular job. */
	public static void addCallback(JobID jobID, JobCallback jobCallback) {
		synchronized (sCallbackListSynchronizationObject) {
			Log.d(TAG, "Adding a callback" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
					+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());

			List<JobCallback> list = mCallbacks.get(jobID.toString());

			if (list == null) {
				mCallbacks.put(jobID.toString(), new ArrayList<JobCallback>());
				list = mCallbacks.get(jobID.toString());
			}

			list.add(jobCallback);
		}
	}

	/*
	 * Function which can be used to keep wifi and processor alive even when the
	 * phone wants to go to sleep.
	 */
	public void enableWakeLock(boolean enable) {
		if (enable == true) {
			wifiLock.acquire();
			wakeLock.acquire();
		} else {
			wifiLock.release();
			wakeLock.release();
		}
	}

	
	/*
	 * This causes the service to start if it's not already started. It then
	 * checks if there is something to do. If there is nothing to do the service
	 * shuts down itself.
	 */
	public static void wakeUp(Context context) {

		/* We can create a new Settings object here each time. It doesn't take a lot of memory. Not a huge overhead. */
		Settings settings = new Settings(context);
		
		/* If the service is running the regardless of the service checkbox in ui we can try to run it again
		 * because when the service is run an onStartCommand() method is called that does all necessary checks
		 * to find out whether we can stop the service or not. If the service is not running then we run it
		 * only if the service checkbox allows that. */
		if (mServiceRunning == true || settings.getServiceCheckBox() == true)
		{
			context.startService(new Intent(context, JobService.class));	
		}
	}

	/* Remove a callback from the list for a particular job. */
	public static void removeCallback(JobID jobID, JobCallback jobCallback) {
		synchronized (sCallbackListSynchronizationObject) {
			Log.d(TAG, "Removing a callback" + " timestamp=" + jobID.getTimeStamp() + " jobtype=" + jobID.getJobType()
					+ " prodID=" + jobID.getProductID() + " SKU=" + jobID.getSKU());

			List<JobCallback> list = mCallbacks.get(jobID.toString());

			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					if (jobCallback == null || list.get(i) == jobCallback) {
						list.remove(i);
						break;
					}
				}
			}
		}
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "Starting the service.");

		mJobQueue = new JobQueue(this);
		mExternalImagesJobQueue = new ExternalImagesJobQueue(this);
		mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {

			@Override
			public boolean handleMessage(Message msg) {
				if (sJobsPresentInTheQueue) {
					wakeUp(JobService.this);
					/* Make the service recheck the job queue every 10 seconds. */
					mHandler.postDelayed(null, 10000);
				}
				return true;
			}
		});

		/* Wake ourselves up every 10 seconds */
		mHandler.postDelayed(null, 10000);

		WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "MyWifiLock");

		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Lock");

		enableWakeLock(true);
		
		OnJobServiceStateChangedListener stateChangedListenerCopy = mOnJobServiceStateChangedListener;
		if (stateChangedListenerCopy != null)
		{
			stateChangedListenerCopy.onJobServiceStateChanged(true);
		}
		
		mServiceRunning = true;
	}

	@Override
	public IBinder onBind(final Intent intent) {
		/* We're not binding to this service */
		return null;
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {

		/* We can create a new Settings object here each time. It doesn't take a lot of memory. Not a huge overhead. */
		Settings settings = new Settings(this);
		boolean checkBoxState = settings.getServiceCheckBox();
		
		/*
		 * If we were started because someone wants us to process some non-job
		 * request.
		 */
		if (intent != null && intent.getIntExtra(EKEY_OP_REQUEST_ID, INVALID_REQUEST_ID) != INVALID_REQUEST_ID) {
			final Messenger messenger = (Messenger) intent.getParcelableExtra(EKEY_MESSENGER);

			final int operationRequestId = intent.getIntExtra(EKEY_OP_REQUEST_ID, INVALID_REQUEST_ID);

			/*
			 * Using -1 as default value to be able to tell later if we got
			 * passed the resource type or not.
			 */
			final int resourceType = intent.getIntExtra(EKEY_RESOURCE_TYPE, -1);
            final String[] resourceParams = JobCacheManager
                    .getStringArrayFromDeserializedItem(intent.getExtras().get(EKEY_PARAMS));

			Bundle extraBundle = intent.getExtras().getBundle(EKEY_REQUEST_EXTRAS);
			
			if (extraBundle == null)
			{
				extraBundle = new Bundle();
			}
			
			extraBundle.putSerializable(EKEY_SETTINGS_SNAPSHOT, intent.getExtras().getSerializable(EKEY_SETTINGS_SNAPSHOT));
			
			if (resourceType != -1) {
				/* Process the non-job request */
				obtainResource(extraBundle, new LoadOperation(operationRequestId,
						resourceType, resourceParams), messenger);
			}
		}

		/* If there already is a job pending then just let it finish. */
		if (sIsJobPending == false) {
			
			if (checkBoxState == false && sSynchronousRequestsCount == 0)
			{
				/* If there are no jobs pending and no synchronous requests pending and the service checkbox is unchecked
				 * then we stop the service. */
				Log.d(TAG, "Stopping the service (due to service checkbox unchecked)");
				this.stopSelf();
				return super.onStartCommand(intent, flags, startId);
			}
			else
			if (checkBoxState == false)
			{
				Log.d(TAG, "No job is pending but won't start a new one because serivce checkbox is unchecked.");
				return super.onStartCommand(intent, flags, startId);
			}
			
			boolean networkStateOK = true;
			boolean avoidImageUploadJobs = false;

			WifiManager wifimanager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			NetworkInfo mobileInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			
			if (wifimanager.isWifiEnabled()) {
				if (!wifiInfo.isConnected()) {
					if (mobileInfo.isConnected()) {
						avoidImageUploadJobs = true;
						//Log.d(TAG, "WIFI is enabled but not connected and mobile data is connected, " +
						//		"will process all jobs except the image upload ones.");
					}
					else
					{
						//Log.d(TAG, "WIFI is enabled but not connected and mobile data is disabled, no job will be executed");
						networkStateOK = false;	
					}
					
				} else {
					//Log.d(TAG, "WIFI is enabled and connected");
				}
			} else /* Wifi is not enabled */
			{
				if (!mobileInfo.isConnected()) {
				//	Log.d(TAG, "WIFI is disabled and mobile data is not connected, no job will be executed");
					networkStateOK = false;
				} else {
				//	Log.d(TAG, "WIFI is disabled but mobile data is connected");
				}
			}

			/* If there are no jobs in the queue and no synchronous requests then we can stop the service. */
			if (!mJobQueue.isPendingTableEmpty() || !mExternalImagesJobQueue.isTableEmpty())
			{
				sJobsPresentInTheQueue = true;
				
				if (networkStateOK)
				{
					Job job = mJobQueue.selectJob(avoidImageUploadJobs);
				
					if (job != null) {
						executeJob(job);
					}
					else if (!avoidImageUploadJobs)
					{
						ExternalImagesJob externalImagesJob = mExternalImagesJobQueue.selectJob();
						
						if (externalImagesJob != null)
						{
							executeExternalImagesJob(externalImagesJob);
						}
					}
				}
			}
			else
			{
				sJobsPresentInTheQueue = false;
				if (sSynchronousRequestsCount == 0) {
					Log.d(TAG, "Stopping the service");
					/*
					 * We have no jobs in the queue and we are not taking care
					 * of any synchronous requests. Stop the service.
					 */
					this.stopSelf();
				}
			}
		
		} else {
			Log.d(TAG, "A job is already pending, won't select a new one.");
		}

		return super.onStartCommand(intent, flags, startId);
	}

	/*
	 * Notify the listeners that something happen with a particular job. All the
	 * information about what happened (finished, error) can be found in the job
	 * object which is passed to the listeners.
	 */
	private void notifyListeners(Job job) {
		synchronized (sCallbackListSynchronizationObject) {
			List<JobCallback> list = mCallbacks.get(job.getJobID().toString());
			if (list != null) {
				Log.d(TAG, "Notifying listeners (count=" + list.size() + ") " + " timestamp="
						+ job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType() + " prodID="
						+ job.getJobID().getProductID() + " SKU=" + job.getJobID().getSKU());

				for (int i = 0; i < list.size(); i++) {
					list.get(i).onJobStateChange(job);
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		enableWakeLock(false);
		
		OnJobServiceStateChangedListener stateChangedListenerCopy = mOnJobServiceStateChangedListener;
		if (stateChangedListenerCopy != null)
		{
			stateChangedListenerCopy.onJobServiceStateChanged(false);
		}
		
		mServiceRunning = false;
	}

	public boolean moveImageToGalleryDir(File imageFile)
	{
		String imagesDirPath = new Settings(this).getGalleryPhotosDirectory();
		File renamedFile = new File(imagesDirPath, imageFile.getName());
		boolean success = imageFile.renameTo(renamedFile);
		
		return success;
	}
	
	private void executeExternalImagesJob(final ExternalImagesJob job)
	{
		sIsJobPending = true;
		Log.d(TAG, "Executing a job: " + job.toString());
		sJobExecutor.submit(new Runnable() {
			@Override
			public void run() {
				
				final File destinationDir = new File(JobCacheManager.getProdImagesQueuedDirName());
				final String productCode = job.mProductCode;
				
				try {
					Log.d(TAG, "JOB STARTED: " + job.toString());
					
					File [] filesToProcess = destinationDir.listFiles(new FilenameFilter() {
						
						@Override
						public boolean accept(File dir, String filename) {
							
							if (filename.toLowerCase().contains(".jpg") && filename.startsWith(productCode + "__"))
							{
								if (!filename.endsWith("_x"))
								return true;
							}
							
							return false;
						}
					});

					Arrays.sort(filesToProcess);
					
					if (filesToProcess.length == 0)
					{
						File [] filesToRemove = destinationDir.listFiles(new FilenameFilter() {
							
							@Override
							public boolean accept(File dir, String filename) {
								
								if (filename.toLowerCase().contains(".jpg") && filename.startsWith(productCode + "__"))
								{
									return true;
								}
								
								return false;
							}
						});
						
						for(int i=0; i<filesToRemove.length; i++)
						{
							filesToRemove[i].delete();
						}
						
						Log.d(TAG, "JOB SUCCESSFUL: " + job.toString());
						
						mExternalImagesJobQueue.handleProcessedJob(job, true);
					}
					else
					{
						File fileToProcess = filesToProcess[0];
						
						ExternalImageUploader uploader = new ExternalImageUploader(JobService.this);
						
						String uploaderSKU = uploader.uploadFile(fileToProcess.getAbsolutePath(), job.mProfileID, job.mProductCode, job.mSKU);
						
						ExternalImagesJobQueue.updateExternalImagesCount();
						
						if (job.mSKU == null && uploaderSKU != null)
						{
							mExternalImagesJobQueue.setSKU(job, uploaderSKU);
						}
						
						Log.d(TAG, "JOB PARTLY FINISHED: " + job.toString());	
					}

				} catch (Exception e) {
                    boolean jobRemoved = false;
                    if (e instanceof ProductDetailsLoadException)
                    {
                        ProductDetailsLoadException productDetailsLoadException = (ProductDetailsLoadException) e;
                        if (productDetailsLoadException.getFaultCode() == ProductDetailsLoadException.ERROR_CODE_PRODUCT_DOESNT_EXIST)
                        {
                            CommonUtils.debug(TAG,
                                    "Receiving product not found exception. Removing job");
                            mExternalImagesJobQueue.deleteJobFromQueue(job);
                            jobRemoved = true;
                        }
                    }
                    if (jobRemoved
                            || mExternalImagesJobQueue.reachedFailureLimit(job.mAttemptsCount + 1))
					{
						File [] filesToRestore = destinationDir.listFiles(new FilenameFilter() {
							
							@Override
							public boolean accept(File dir, String filename) {
								
								if (filename.toLowerCase().contains(".jpg") && filename.startsWith(productCode + "__"))
								{
									return true;
								}
								
								return false;
							}
						});
						
						for(int i=0; i<filesToRestore.length; i++)
						{
							moveImageToGalleryDir(filesToRestore[i]);
						}
					}
					if(!jobRemoved)
					{
					    mExternalImagesJobQueue.handleProcessedJob(job, false);
					}
					
					Log.d(TAG, "JOB FAILED: " + job.toString());
				}

				/* Make the service try next job right away. */
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
	
	/* Obvious. */
	private void executeJob(final Job job) {
		sIsJobPending = true;
		Log.d(TAG, "Executing a job" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype="
				+ job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID() + " SKU="
				+ job.getJobID().getSKU());
		sJobExecutor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					/*
					 * This is a special case for image upload. There should be
					 * no more special cases like this. We need to register a
					 * special callback here to get informed about how much data
					 * was already sent to the server so that the UI can show
					 * this data in a form of a progress bar to the user.
					 */
					if (job.getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE) {
						mJobProcessorManager.getImageProcessorInstance().setCallback(new StreamUploadCallback() {

							@Override
							public void onUploadProgress(int progress, int max) {
								Log.d(TAG, "Upload Progress " + progress + "/" + max);
								job.setProgressPercentage(progress * 100 / max);
								JobCacheManager.store(job);
								notifyListeners(job);
							}
						});
					}

					Log.d(TAG, "JOB STARTED" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype="
							+ job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID() + " SKU="
							+ job.getJobID().getSKU());
					mJobProcessorManager.process(JobService.this, job);
					Log.d(TAG, "JOB FINISHED" + " timestamp=" + job.getJobID().getTimeStamp() + " jobtype="
							+ job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID() + " SKU="
							+ job.getJobID().getSKU());

				} catch (RuntimeException e) {
					if (job.getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE) {
						job.setProgressPercentage(0);
					}

					job.setException(e);
					Log.logCaughtException(e);

					/*
					 * The job failed. We're making the queue handle this fact.
					 * It will increase the failure counter and move the job to
					 * a different table if necessary.
					 */
					Job j = mJobQueue.handleProcessedJob(job);

					Log.d(TAG, "JOB FAILED, no job is pending anymore" + " timestamp=" + job.getJobID().getTimeStamp()
							+ " jobtype=" + job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID()
							+ " SKU=" + job.getJobID().getSKU());

					/* Notify listeners about job failure. */
					if (j != null)
					{
						notifyListeners(j);	
					}

					if (job.getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE)
						mJobProcessorManager.getImageProcessorInstance().setCallback(null);

					/* Make the service try next job right away. */
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

				Log.d(TAG, "JOB SUCCESSFUL, no job is pending anymore" + " timestamp=" + job.getJobID().getTimeStamp()
						+ " jobtype=" + job.getJobID().getJobType() + " prodID=" + job.getJobID().getProductID()
						+ " SKU=" + job.getJobID().getSKU());

				/* Notify listeners about job success. */
				notifyListeners(job);

				if (job.getJobType() == MageventoryConstants.RES_UPLOAD_IMAGE)
					mJobProcessorManager.getImageProcessorInstance().setCallback(null);

				/* Make the service try next job right away. */
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

	/*
	 * Puts all non-job requests in the executor for processing. It can be
	 * called multiple times with different request data and the requests will
	 * be queued by the executor but the queue will be lost in case application
	 * process is killed.
	 */
	private void obtainResource(final Bundle requestExtras, final LoadOperation op, final Messenger messenger) {
		sSynchronousRequestsCount++;
		sOperationExecutor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					final Bundle data = mResourceProcessorManager.process(getBaseContext(), op.getResourceType(),
							op.getResourceParams(), requestExtras);
					op.setExtras(data);
				} catch (RuntimeException e) {
					
					if (e instanceof ProductDetailsLoadException)
					{
						ProductDetailsLoadException pdle = (ProductDetailsLoadException)e;
						
						if (pdle.getFaultCode() == ProductDetailsLoadException.ERROR_CODE_PRODUCT_DOESNT_EXIST &&
							pdle.mDontReportProductNotExistsException == true)
						{
							Log.logCaughtException(e, false);
						}
						else
						{
							Log.logCaughtException(e, true);
						}
					}
					else
					{
						Log.logCaughtException(e);
					}
					
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
					Log.logCaughtException(e);
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
