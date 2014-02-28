
package com.mageventory.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* Class for storing information about which files are being downloaded. For each SKU we store a list
 * of file paths for all images for which download process didn't finish yet, but did begin. This class
 * is useful in cases where we don't want the app to start the download of the file it is already downloading
 * in some other thread. */
public class ImageCachingManager {

    /* This map stores SKUs as keys and list of file paths as values. */
    private static Map<String, List<String>> sNumberOfPendingDownloads = new HashMap<String, List<String>>();

    /*
     * All accesses to the map of paths are synchronised with this
     * synchronisation object.
     */
    public static Object sSynchronisationObject = new Object();

    /* Add a download entry for a given SKU. */
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

    /* Remove a download entry for a given SKU. */
    public static void removeDownload(String SKU, String filePath) {
        synchronized (sSynchronisationObject) {
            List<String> list = sNumberOfPendingDownloads.get(SKU);

            if (list == null)
                return;

            while (list.remove(filePath)) {
            }

            if (list.size() == 0) {
                sNumberOfPendingDownloads.remove(SKU);
            }
        }
    }

    /* Get number of downloads for a given SKU. */
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

    /*
     * Get a list of file paths where images are being downloaded to for a given
     * SKU.
     */
    public static List<String> getPendingDownloads(String SKU) {
        synchronized (sSynchronisationObject) {
            return sNumberOfPendingDownloads.get(SKU);
        }
    }

    /* Is download pending for a given SKU and a given image path. */
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
