package com.mageventory.jobprocessor;

import java.util.Map;

import android.content.Context;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient2;
import com.mageventory.job.Job;
import com.mageventory.jobprocessor.JobProcessorManager.IProcessor;

public class UpdateProductProcessor implements IProcessor, MageventoryConstants {

	@Override
	public void process(Context context, Job job) {

		Map<String, Object> requestData = job.getExtras();

		/* Don't need this key here. It is just here in case we need to merge product edit job file with product details file.
		 * We don't need to send that to the server. */
		requestData.remove(EKEY_UPDATED_KEYS_LIST);
		
		final MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();
		if (client.catalogProductUpdate(job.getJobID().getProductID(), job.getExtras()) == false) {
			throw new RuntimeException("unsuccessful update");
		}
	}
}
