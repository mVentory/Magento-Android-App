package com.mageventory.job;

import java.io.File;

public class JobCleanupManager {
	public static final int MINIMUM_JOB_LIFESPAN_PERION_DAYS = 7;
	
	/* TODO: stub */
	/* See: http://code.google.com/p/mageventory/issues/detail?id=92#c10 */
	private static String getCurrentSubdirName()
	{
		return "1";
	}
	
	public static File appendCleanupSpecificSubdir(File file)
	{
		return new File(file, getCurrentSubdirName());
	}

}
