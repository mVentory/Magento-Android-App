package com.mageventory.components;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.provider.ContactsContract.CommonDataKinds.Im;

public class ImageCachingManager {

	/* This map stores SKUs as keys and list of file paths as values. */
	private static Map<String, List<String>> sNumberOfPendingDownloads = new HashMap<String, List<String>>();

	public static Object sSynchronisationObject = new Object();

	public static void addDownload(String SKU, String filePath) {
		synchronized (sSynchronisationObject) {
			List<String> list = sNumberOfPendingDownloads.get(SKU);

			if (list == null) {
				list = new ArrayList<String>();
				sNumberOfPendingDownloads.put(SKU, list);
			}

			list.add(filePath);
		}
	}

	public static void removeDownload(String SKU, String filePath) {
		synchronized (sSynchronisationObject) {
			List<String> list = sNumberOfPendingDownloads.get(SKU);

			if (list == null)
				return;

			list.remove(filePath);

			if (list.size() == 0) {
				sNumberOfPendingDownloads.remove(SKU);
			}
		}
	}

	public static int getPendingDownloadCount(String SKU) {
		synchronized (sSynchronisationObject) {
			List<String> downloadsList = sNumberOfPendingDownloads.get(SKU);

			if (downloadsList != null) {
				return downloadsList.size();
			} else {
				return 0;
			}
		}
	}

	public static List<String> getPendingDownloads(String SKU) {
		synchronized (sSynchronisationObject) {
			return sNumberOfPendingDownloads.get(SKU);
		}
	}

	public static boolean isDownloadPending(String SKU, String path) {
		synchronized (sSynchronisationObject) {
			List<String> downloadsList = sNumberOfPendingDownloads.get(SKU);

			if (downloadsList != null) {
				return downloadsList.contains(path);
			} else {
				return false;
			}
		}
	}
}
