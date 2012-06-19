package com.mageventory.jobprocessor;

import android.content.Context;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient2;
import com.mageventory.job.Job;
import com.mageventory.jobprocessor.JobProcessorManager.IProcessor;

public class UpdateProductProcessor implements IProcessor, MageventoryConstants {

	@Override
	public void process(Context context, Job job) {

		final MagentoClient2 client = ((MyApplication) context.getApplicationContext()).getClient2();
		if (client.catalogProductUpdate(job.getJobID().getProductID(), job.getExtras()) == false) {
			throw new RuntimeException("unsuccessful update");
		}
	}
}
