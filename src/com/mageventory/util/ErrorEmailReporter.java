package com.mageventory.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.mageventory.MyApplication;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobQueue;

/* A class for sending an email to the developer. The email will contain various data helpful
 * in debugging a problem including logs and database dump. */
public class ErrorEmailReporter {
	
	public static Boolean sSendingEmailRunning = new Boolean(false);
	public static String sArchiveName = "archive.zip";
	public static final String TAG = "ErrorEmailReporter";
	
	public static void makeDBDump()
	{
		long timestamp = System.currentTimeMillis();
		
		/* We have a separate directory for each error report. All we use it for is to store database dump and an
		 * attachment containing logs and the dump zipped up.*/
		
		File dumpDir = new File (JobCacheManager.getErrorReportingDir(), "" + timestamp);
		
		if (!dumpDir.exists())
		{
			dumpDir.mkdirs();
		}
		
		JobQueue jq = new JobQueue(MyApplication.getMyApplication());
		jq.dumpQueueDatabase(dumpDir);
	}
	
	private static void addFileToZip(ZipOutputStream zipos, String entryName, File fileToZip) throws IOException
	{
        BufferedInputStream fileStream = new BufferedInputStream(
                new FileInputStream(fileToZip));

        ZipEntry newEntry = new ZipEntry(entryName);

        zipos.putNextEntry(newEntry);

        // lets put data from file to current archive entry
        int c;
        while ((c = fileStream.read()) != -1)
            zipos.write(c);
        fileStream.close();
	}
	
	public static void zipEverythingUp(long timestamp) throws IOException
	{
    	Log.d(TAG, ">> zipEverythingUp()");

		String[] databaseFiles = { "" + timestamp + "/" + JobCacheManager.QUEUE_PENDING_TABLE_DUMP_FILE_NAME,
        					"" + timestamp + "/" + JobCacheManager.QUEUE_FAILED_TABLE_DUMP_FILE_NAME};
        
        ZipOutputStream zipos = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(new File(new File(JobCacheManager.getErrorReportingDir(), "" + timestamp), sArchiveName))));
        /* Compress database files */
        for (String entry : databaseFiles) {
        	Log.d(TAG, "Compressing: " + new File(JobCacheManager.getErrorReportingDir(), entry).getAbsolutePath());
            addFileToZip(zipos, entry, new File(JobCacheManager.getErrorReportingDir(), entry));
        }
        
        /* Compress log files */
        if (JobCacheManager.getLogDir().exists())
        {
        	File [] logFiles = JobCacheManager.getLogDir().listFiles();
        	
        	for (int i=0; i<logFiles.length; i++)
        	{
            	Log.d(TAG, "Compressing: " + logFiles[i].getAbsolutePath());
                addFileToZip(zipos, JobCacheManager.LOG_DIR_NAME + "/" + logFiles[i].getName(), logFiles[i]);
        	}
        }

        zipos.close();
        
        Log.d(TAG, "<< zipEverythingUp()");
	}
	
	public static void sendEmails()
	{
		synchronized(sSendingEmailRunning)
		{
			if (sSendingEmailRunning == false)
			{
				
			}
		}
	}
}
