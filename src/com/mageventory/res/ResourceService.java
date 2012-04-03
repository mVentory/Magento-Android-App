package com.mageventory.res;

import static com.mageventory.util.Util.logThread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class ResourceService extends Service implements ResourceConstants {

	private static final String TAG = "ResourceService";
	private static ExecutorService executor = Executors.newFixedThreadPool(2);

	private ResourceServiceBinder binder;
	private ResourceProcessorManager processor = new ResourceProcessorManager();

	@Override
	public IBinder onBind(final Intent intent) {
		logThread(TAG, "onBind()");
		return getBinder();
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		final Messenger messenger = (Messenger) intent.getParcelableExtra(EKEY_MESSENGER);

		final int operationRequestId = intent.getIntExtra(EKEY_OP_REQUEST_ID, INVALID_REQUEST_ID);
		final int resourceType = intent.getIntExtra(EKEY_RESOURCE_TYPE, RES_INVALID);
		final String[] resourceParams = (String[]) intent.getExtras().get(EKEY_PARAMS);

		if (resourceType != RES_INVALID) {
			submitOperation(intent.getExtras().getBundle(EKEY_REQUEST_EXTRAS), new LoadOperation(operationRequestId,
					resourceType, resourceParams), messenger);
		}

		return super.onStartCommand(intent, flags, startId);
	}

	private ResourceServiceBinder getBinder() {
		if (binder == null) {
			synchronized (this) {
				if (binder == null) {
					binder = new ResourceServiceBinder();
				}
			}
		}
		return binder;
	}

	private void submitOperation(final Bundle requestExtras, final LoadOperation op, final Messenger messenger) {
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					final Bundle data = processor.process(getBaseContext(), op.getResourceType(),
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
