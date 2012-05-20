package com.mageventory.jobprocessor;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.mageventory.MageventoryConstants;
import com.mageventory.job.Job;

public class JobProcessorManager {
	public static interface IProcessor {
		public void process(Context context, Job job);
	}

	private static Map<Integer, IProcessor> sResourceProcessors = new HashMap<Integer, IProcessor>();

	public static void bindResourceProcessor(final int resourceType, final IProcessor processor) {
		if (sResourceProcessors.containsKey(resourceType)) {
			throw new IllegalArgumentException("there is already a processor added for this resource type: "
					+ resourceType);
		}
		sResourceProcessors.put(resourceType, processor);
	}
	
	/* This function is not pretty but it is for a special case when we need to get updated about some additional
	 * data from the image processor while the image is being uploaded. */
	public UploadImageProcessor getImageProcessorInstance()
	{
		UploadImageProcessor processor = (UploadImageProcessor)sResourceProcessors.get(MageventoryConstants.RES_UPLOAD_IMAGE);
		
		if (processor == null) {
			throw new IllegalArgumentException("no processor for resource type: " + MageventoryConstants.RES_UPLOAD_IMAGE);
		}
		
		return processor;
	}

	public void process(Context context, Job job) {
		final IProcessor processor = sResourceProcessors.get(job.getJobType());
		if (processor == null) {
			throw new IllegalArgumentException("no processor for resource type: " + job.getJobType());
		}
		processor.process(context, job);
	}
}
