/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/

package com.mageventory.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.content.Context;

import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobQueue;

public class ErrorReporterUtils {

    public static String sArchiveName = "report.zip";
    private static final String TAG = "ErrorEmailReporter";
    private static final String sDatabaseDirName = "database";

    public static void makeDBDump(Context c)
    {
        File dumpDir = new File(JobCacheManager.getErrorReportingDir(), sDatabaseDirName);

        if (!dumpDir.exists())
        {
            dumpDir.mkdirs();
        }

        JobQueue jq = new JobQueue(c);
        jq.dumpQueueDatabase(dumpDir);
    }

    public static File getZippedErrorReportFile()
    {
        return new File(JobCacheManager.getErrorReportingDir(), sArchiveName);
    }

    private static void addFileToZip(ZipOutputStream zipos, String entryName, File fileToZip)
            throws IOException
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

    public static void zipEverythingUp(boolean includeCurrentLogFileOnly) throws IOException
    {
        Log.d(TAG, ">> zipEverythingUp()");

        String[] databaseFiles = {
                sDatabaseDirName + "/" + JobCacheManager.QUEUE_PENDING_TABLE_DUMP_FILE_NAME,
                sDatabaseDirName + "/" + JobCacheManager.QUEUE_FAILED_TABLE_DUMP_FILE_NAME
        };

        ZipOutputStream zipos = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(getZippedErrorReportFile())));
        /* Compress database files */
        for (String entry : databaseFiles) {
            Log.d(TAG,
                    "Compressing: "
                            + new File(JobCacheManager.getErrorReportingDir(), entry)
                                    .getAbsolutePath());
            addFileToZip(zipos, entry, new File(JobCacheManager.getErrorReportingDir(), entry));
        }

        synchronized (JobCacheManager.sSynchronizationObject)
        {
            File timestampsFile = JobCacheManager.getGalleryTimestampsFile();
            if(timestampsFile.exists())
            {
                Log.d(TAG, "Compressing: " + timestampsFile.getAbsolutePath());
                addFileToZip(zipos, JobCacheManager.GALLERY_TIMESTAMPS_DIR_NAME + "/" + timestampsFile.getName(),
                        timestampsFile);
            }
        }
        synchronized (Log.loggingSynchronisationObject)
        {
            File errorRerportingFile = JobCacheManager.getErrorReportingFile();

            HashSet<File> logFiles = new HashSet<File>();

            if (includeCurrentLogFileOnly)
            {
                logFiles.add(Log.logFile);
            }
            else
            {
                if (errorRerportingFile.exists() == true)
                {
                    FileReader fileReader = new FileReader(errorRerportingFile);
                    LineNumberReader lineNumberReader = new LineNumberReader(fileReader);
                    String line;

                    while ((line = lineNumberReader.readLine()) != null)
                    {
                        if (line.length() > 0)
                        {
                            logFiles.add(new File(JobCacheManager.getLogDir(), line));
                        }
                    }
                }
            }

            /* Compress log files */
            for (File logFile : logFiles)
            {
                if (logFile.exists())
                {
                    Log.d(TAG, "Compressing: " + logFile.getAbsolutePath());
                    addFileToZip(zipos, JobCacheManager.LOG_DIR_NAME + "/" + logFile.getName(),
                            logFile);
                }
                else
                {
                    Log.d(TAG,
                            "Was trying to compress log file but it doesn't exist, something's wrong: "
                                    + logFile.getAbsolutePath());
                }
            }
        }

        zipos.close();

        Log.d(TAG, "<< zipEverythingUp()");
    }
}
