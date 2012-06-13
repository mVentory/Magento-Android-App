package com.mageventory.job;

/* Used to notify code which puts the job in the queue about job status changes
 * (success, failure, upload progress) */
public interface JobCallback {
	void onJobStateChange(Job job);
}
