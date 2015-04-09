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

package com.mageventory.job;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.Time;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.cache.ProductAliasCacheManager;
import com.mageventory.client.Base64Coder_magento;
import com.mageventory.model.CarriersList;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.model.OrderList;
import com.mageventory.model.Product;
import com.mageventory.model.ProductDuplicationOptions;
import com.mageventory.model.util.ProductUtils;
import com.mageventory.recent_web_address.RecentWebAddressProviderAccessor;
import com.mageventory.settings.Settings;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.Log;
import com.mageventory.util.TrackerUtils;

/**
 * Contains methods for performing operations on the cache.
 */
public class JobCacheManager {

    static final String TAG = JobCacheManager.class.getSimpleName();

    /**
     * The supported cache version to determine whether the cache should be
     * cleared on first application start after the new version installed
     */
    public static final int CACHE_VERSION = 13;

    static final long TIMESTAMP_DETECT_THRESHOLD = 5 * 60 * 1000; // 5 minutes;
    /**
     * A .nomedia file name which is used by Android system to tell MediaScanner
     * to do not scan the directory for media files
     */
    public static final String NOMEDIA_FILE = ".nomedia";

    public static Object sSynchronizationObject = new Object();
    public static Object sProductDetailsLock = new Object();
    /**
     * The RAM cache tree structure
     */
    public static final Map<String, Object> sRamCache = new HashMap<String, Object>();

    private static final String PRODUCT_LIST_DIR_NAME = "product_lists";
    private static final String ORDER_DETAILS_DIR_NAME = "order_details";
    private static final String PROFILE_EXECUTION_DIR_NAME = "profile_execution";
    private static final String ATTRIBUTE_LIST_DIR_NAME = "attribute_list";
    private static final String ORDER_LIST_DIR_NAME = "order_list";
    private static final String QUEUE_DATABASE_DUMP_DIR_NAME = "database_dump";
    private static final String DOWNLOAD_IMAGE_PREVIEW_DIR_NAME = "DOWNLOAD_IMAGE_PREVIEW";
    private static final String DOWNLOAD_IMAGE_DIR = "DOWNLOAD_IMAGE";
    public static final String PROD_IMAGES_QUEUED_DIR_NAME = "prod-images-queued";

    public static final String LOG_DIR_NAME = "log";
    private static final String ERROR_REPORTING_DIR_NAME = "error_reporting";
    private static final String ERROR_REPORTING_FILE_NAME = "error_reporting_timestamps";

    private static final String GALLERY_BAD_PICS_DIR_NAME = "bad_pics";
    public static final String GALLERY_TIMESTAMPS_DIR_NAME = "GALLERY_TIMESTAMPS";
    private static final String GALLERY_TIMESTAMPS_FILE_NAME = "gallery_timestamps.txt";

    private static final String PRODUCT_DETAILS_FILE_NAME = "prod_dets.json";
    private static final String ATTRIBUTE_SETS_FILE_NAME = "attribute_sets.json";
    private static final String CATEGORIES_LIST_FILE_NAME = "categories_list.json";
    private static final String ORDER_CARRIERS_FILE_NAME = "order_carriers.json";
    private static final String STATISTICS_FILE_NAME = "statistics.json";
    private static final String PROFILES_FILE_NAME = "profiles.json";
    private static final String CART_ITEMS_FILE_NAME = "cart_items.json";
    private static final String INPUT_CACHE_FILE_NAME = "input_cache.json";
    private static final String LAST_USED_ATTRIBUTES_FILE_NAME = "last_used_attributes_list.json";
    private static final String DUPLICATION_OPTIONS_FILE_NAME = "duplication_options.json";
    public static final String QUEUE_PENDING_TABLE_DUMP_FILE_NAME = "pending_table_dump.csv";
    public static final String QUEUE_FAILED_TABLE_DUMP_FILE_NAME = "failed_table_dump.csv";

    public static final String GALLERY_TAG = "GALLERY_EXTERNAL_CAM_JCM";
    public static final String JCM_TAG = "JCM_TAG";

    public static String getProdImagesQueuedDirName()
    {
        File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        dir = new File(dir, PROD_IMAGES_QUEUED_DIR_NAME);
        return dir.getAbsolutePath();
    }

    public static File getLogDir()
    {
        File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        dir = new File(dir, LOG_DIR_NAME);

        if (!dir.exists())
        {
            dir.mkdir();
        }

        return dir;
    }

    public static File getErrorReportingDir()
    {
        File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        dir = new File(dir, ERROR_REPORTING_DIR_NAME);

        if (!dir.exists())
        {
            dir.mkdir();
        }

        return dir;
    }

    public static File getErrorReportingFile()
    {
        File file = new File(getErrorReportingDir(), ERROR_REPORTING_FILE_NAME);

        return file;
    }

    /* External camera gallery cache functions */
    public static File getBadPicsDir()
    {
        File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        dir = new File(dir, GALLERY_BAD_PICS_DIR_NAME);

        if (!dir.exists())
        {
            Log.d(GALLERY_TAG, "BAD_PICS dir does not exist, creating.");
            dir.mkdir();
        }

        return dir;
    }

    /* Get human readable timestamp of the EXIF format. */
    private static long getGalleryTimestampFromExif(String exifDateTime, long offsetSeconds)
    {
        if (TextUtils.isEmpty(exifDateTime))
        {
            return 0;
        }

        String[] dateTimeArray = exifDateTime.split(" ");
        String[] dateArray = dateTimeArray[0].split(":");
        String[] timeArray = dateTimeArray[1].split(":");

        Time time = new Time();
        time.year = Integer.parseInt(dateArray[0]);
        time.month = Integer.parseInt(dateArray[1]) - 1;
        time.monthDay = Integer.parseInt(dateArray[2]);

        time.hour = Integer.parseInt(timeArray[0]);
        time.minute = Integer.parseInt(timeArray[1]);
        time.second = Integer.parseInt(timeArray[2]);

        time.set(time.toMillis(true) + offsetSeconds * 1000);

        return getGalleryTimestampFromTime(time, 0);
    }

    /* Get human readable timestamp of the current time. */
    public static long getGalleryTimestampNow()
    {
        long millis = System.currentTimeMillis();
        Time time = new Time();
        time.set(millis);

        return getGalleryTimestampFromTime(time, millis);
    }

    /**
     * Get the gallery file timestamp from the unix time
     * 
     * @param unixTime
     * @return
     */
    public static long getGalleryTimestamp(long unixTime) {
        Time time = new Time();
        time.set(unixTime);

        return getGalleryTimestampFromTime(time, unixTime);
    }
    
    /**
     * Parse gallery timestamp and get java time in millis from it
     * 
     * @param timestamp
     * @return
     * @throws ParseException
     */
    public static long getTimeFromGalleryTimestamp(long timestamp) throws ParseException {
        String timestampString = Long.toString(timestamp);
        String pattern = "yyyyMMddHHmmss";
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        Date d = formatter.parse(timestampString.substring(0, pattern.length()));
        long time = d.getTime();
        int hundreds = Integer.valueOf(timestampString.substring(pattern.length())) * 10;
        time += hundreds;
        return time;
    }
    /*
     * Get human readable timestamp of given time. Milliseconds is a separate
     * variable because Time class does not allow storing milliseconds.
     */
    private static long getGalleryTimestampFromTime(Time time, long millis)
    {
        int year = time.year;
        int month = time.month + 1;
        int day = time.monthDay;
        int hour = time.hour;
        int minute = time.minute;
        int second = time.second;
        int hundreth = (int) ((millis / 10) % 100);

        String yearString = "" + year;
        String monthString = "" + month;
        String dayString = "" + day;
        String hourString = "" + hour;
        String minuteString = "" + minute;
        String secondString = "" + second;
        String hundrethString = "" + hundreth;

        if (monthString.length() < 2)
            monthString = "0" + monthString;
        if (dayString.length() < 2)
            dayString = "0" + dayString;
        if (hourString.length() < 2)
            hourString = "0" + hourString;
        if (minuteString.length() < 2)
            minuteString = "0" + minuteString;
        if (secondString.length() < 2)
            secondString = "0" + secondString;
        if (hundrethString.length() < 2)
            hundrethString = "0" + hundrethString;

        String timestamp = yearString + monthString + dayString + hourString + minuteString
                + secondString + hundrethString;

        Log.d(GALLERY_TAG, "getGalleryTimestampNow(); returning: " + timestamp);

        return Long.parseLong(timestamp);
    }

    /* Return a file where timestamp ranges are stored. */
    public static File getGalleryTimestampsFile()
    {
        File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        dir = new File(dir, GALLERY_TIMESTAMPS_DIR_NAME);

        if (!dir.exists())
        {
            Log.d(GALLERY_TAG, "Timestamps file does not exist, creating");
            dir.mkdir();
        }

        return new File(dir, GALLERY_TIMESTAMPS_FILE_NAME);
    }

    public static class GalleryTimestampRange
    {
        public long rangeStart;
        public long profileID;
        /**
         * URL encoded SKU or Barcode value
         */
        public String escapedSKU;
        /**
         * URL decoded (unescaped) SKU or Barcode
         */
        public String sku;
    };

    /*
     * If this is not null it means the gallery file was read successfully and
     * is backed up in the memory.
     */
    public static ArrayList<GalleryTimestampRange> sGalleryTimestampRangesArray;

    private static void reloadGalleryTimestampRangesArray()
    {
        Log.d(GALLERY_TAG, "reloadGalleryTimestampRangesArray(); Entered the function.");

        sGalleryTimestampRangesArray = new ArrayList<GalleryTimestampRange>();

        File galleryFile = getGalleryTimestampsFile();

        if (!galleryFile.exists())
        {
            try {
                galleryFile.createNewFile();
            } catch (IOException e) {
                Log.d(GALLERY_TAG, "Unable to create gallery file.");
                CommonUtils.error(TAG, e);
            }
        }

        if (galleryFile.exists())
        {
            Log.d(GALLERY_TAG, "galleryFile exists. Proceeding.");

            try {
                FileReader fileReader = new FileReader(galleryFile);
                LineNumberReader lineNumberReader = new LineNumberReader(fileReader);
                String line, lastLine = null;

                while ((line = lineNumberReader.readLine()) != null)
                {
                    if (line.length() > 0)
                    {
                        Log.d(GALLERY_TAG, "Parsing line: " + line);

                        String[] splittedLine = line.split(" ");

                        if (splittedLine.length != 3)
                            continue;

                        GalleryTimestampRange newRange = new GalleryTimestampRange();
                        newRange.escapedSKU = splittedLine[0];
                        try {
                        	// Get decoded (unescaped) value
                            newRange.sku = URLDecoder.decode(newRange.escapedSKU, "UTF-8");
                        } catch (Exception e) {
                            CommonUtils.error(GALLERY_TAG, CommonUtils.format(
                                    "reloadGalleryTimestampRangesArray(); Cannot decode sku. %1$s",
                                    newRange.escapedSKU));
                            // such as decoding filed used encoded value as
                            // decoded
                            newRange.sku = newRange.escapedSKU;
                        }

                        try
                        {
                            newRange.profileID = Long.parseLong(splittedLine[1]);
                            newRange.rangeStart = Long.parseLong(splittedLine[2]);
                        } catch (NumberFormatException nfe)
                        {
                            CommonUtils.error(TAG, nfe);
                            continue;
                        }

                        sGalleryTimestampRangesArray.add(newRange);

                    }
                }

                fileReader.close();
            } catch (FileNotFoundException e) {
                CommonUtils.error(TAG, e);
            } catch (IOException e) {
                CommonUtils.error(TAG, e);
            }

        }
        else
        {
            sGalleryTimestampRangesArray = null;
            Log.d(GALLERY_TAG, "galleryFile does not exist and we couldn't create it.");
        }
    }

    public static ArrayList<GalleryTimestampRange> getGalleryTimestampRangesArray()
    {
        if (sGalleryTimestampRangesArray == null)
        {
            reloadGalleryTimestampRangesArray();
        }

        if (sGalleryTimestampRangesArray == null)
        {
            Log.d(GALLERY_TAG, "saveRangeStart(); Unable to load gallery timestamp ranges array.");
        }

        return sGalleryTimestampRangesArray;
    }

    /*
     * Save the beginning of a timestamp range in the timestamps file. Return
     * true on success. Pass 0 as galleryTimestamp param to use the current
     * timestamp.
     */
    public static boolean saveRangeStart(String sku, long profileID, long galleryTimestamp)
    {
        synchronized (sSynchronizationObject) {
            Log.d(GALLERY_TAG, "saveRangeStart(); Entered the function.");

            if (sGalleryTimestampRangesArray == null)
            {
                reloadGalleryTimestampRangesArray();
            }

            if (sGalleryTimestampRangesArray == null)
            {
                Log.d(GALLERY_TAG,
                        "saveRangeStart(); Unable to load gallery timestamp ranges array.");
                return false;
            }

            String escapedSKU;
            try {
                escapedSKU = URLEncoder.encode(sku, "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                CommonUtils.error(GALLERY_TAG,
                        CommonUtils.format("saveRangeStart(); Cannot encode sku. %1$s", sku), e1);
                return false;
            }

            // TODO if sGalleryTimestampRangesArray last entry has same sku need
            // to check timestamp difference to decide whether to write to the
            // log or not
            if (!(sGalleryTimestampRangesArray.size() > 0
                    &&
                    sGalleryTimestampRangesArray.get(sGalleryTimestampRangesArray.size() - 1).escapedSKU
                            .equals(escapedSKU) && sGalleryTimestampRangesArray
                        .get(sGalleryTimestampRangesArray.size() - 1).profileID == profileID))
            {
                long timestamp;

                if (galleryTimestamp == 0)
                {
                    timestamp = getGalleryTimestampNow();
                }
                else
                {
                    timestamp = galleryTimestamp;
                }

                File galleryFile = getGalleryTimestampsFile();

                try {
                    FileWriter fileWriter = null;
                    fileWriter = new FileWriter(galleryFile, true);

                    fileWriter.write(escapedSKU + " " + profileID + " " + timestamp + "\n");
                    fileWriter.close();

                    GalleryTimestampRange newRange = new GalleryTimestampRange();
                    newRange.escapedSKU = escapedSKU;
                    newRange.sku = sku;
                    newRange.profileID = profileID;
                    newRange.rangeStart = timestamp;

                    sGalleryTimestampRangesArray.add(newRange);
                } catch (IOException e) {
                    Log.d(GALLERY_TAG, "saveRangeStart(); Writing to file failed.");
                    CommonUtils.error(TAG, e);
                    return false;
                }
            }

            return true;
        }
    }

    /* Get SKU and profile ID separated with a space. */
    public static GalleryTimestampRange getSkuProfileIDForExifTimeStamp(Context c,
            long exifTimestamp) throws ParseException
    {
        if (exifTimestamp == -1) {
            return null;
        }

        synchronized (sSynchronizationObject) {
            Log.d(GALLERY_TAG, "getSkuProfileIDForExifTimeStamp(); Entered the function.");
            Settings settings = new Settings(c);

            if (sGalleryTimestampRangesArray == null)
            {
                reloadGalleryTimestampRangesArray();
            }

            if (sGalleryTimestampRangesArray == null)
            {
                Log.d(GALLERY_TAG,
                        "getSkuProfileIDForExifTimeStamp(); Unable to load gallery timestamp ranges array.");
                return null;
            }

            Time time = new Time();
            long adjustedTime = exifTimestamp + settings.getCameraTimeDifference() * 1000;
            time.set(adjustedTime);

            long timestamp = getGalleryTimestampFromTime(time, 0);


            for (int i = sGalleryTimestampRangesArray.size() - 1; i >= 0; i--)
            {
                GalleryTimestampRange gts = sGalleryTimestampRangesArray.get(i);
                if (gts.rangeStart <= timestamp)
                {
                    long rangeTime = getTimeFromGalleryTimestamp(gts.rangeStart);
                    if(adjustedTime - rangeTime < TIMESTAMP_DETECT_THRESHOLD)
                    {
                        Log.d(GALLERY_TAG,
                                "getSkuProfileIDForExifTimeStamp(); Found match. Returning: " +
                                        gts.escapedSKU + " "
                                        + gts.profileID);
    
                        return gts;
                    } else
                    {
                        Log.d(GALLERY_TAG,
                                CommonUtils
                                        .format("getSkuProfileIDForExifTimeStamp(); Found match but it is not within a threshold of %1$d milliseconds: found %2$d searched for %3$d",
                                                TIMESTAMP_DETECT_THRESHOLD, gts.rangeStart,
                                                timestamp));
                        return null;
                    }
                }
            }

            Log.d(GALLERY_TAG, "getSkuProfileIDForExifTimeStamp(); No match found. Returning null.");

            return null;
        }
    }

    /**
     * Returns true on success.
     * 
     * @param file
     * @return
     * @deprecated
     */
    @SuppressWarnings("unused")
    private static boolean serialize_old(Object o, File file) {
        long start = System.currentTimeMillis();
        Log.d(JCM_TAG, "Serializing file: " + file.getAbsolutePath());

        FileOutputStream fos;
        ObjectOutputStream oos;
        try {
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(o);
            oos.close();
        } catch (IOException e) {
            CommonUtils.error(TAG, e);
            return false;
        }
        TrackerUtils.trackDataLoadTiming(System.currentTimeMillis()
                - start, "serialize_old", TAG);
        return true;
    }

    /**
     * Returns true on success.
     * 
     * @param o
     * @param file
     * @return
     */
    private static boolean serialize(Object o, File file) {
        long start = System.currentTimeMillis();
        Gson gson = new Gson();
        String json = gson.toJson(o);
        FileOutputStream fos;
        OutputStreamWriter osw;
        try {
            fos = new FileOutputStream(file);
            osw = new OutputStreamWriter(fos, "UTF-8");
            osw.write(json);
            osw.close();
        } catch (IOException e) {
            CommonUtils.error(TAG, e);
            return false;
        }
        TrackerUtils.trackDataLoadTiming(System.currentTimeMillis() - start,
                CommonUtils.format("serialize to file %1$s", file.getAbsolutePath()), TAG);
        return true;
    }

    /**
     * Returns something else than null on success
     * 
     * @param file
     * @return
     * @deprecated
     */
    @SuppressWarnings("unused")
    private static Object deserialize_old(File file) {
        long start = System.currentTimeMillis();
        Object out;
        FileInputStream fis;
        ObjectInputStream ois;

        try {
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);
            out = ois.readObject();
            ois.close();
        } catch (Exception e) {
            return null;
        }
        TrackerUtils.trackDataLoadTiming(System.currentTimeMillis()
                - start, "deserialize_old", TAG);
        return out;
    }

    /**
     * Returns something else than null on success
     * 
     * @param cl
     * @param file
     * @return
     */
    private static <T> T deserialize(Class<T> cl, File file) {
        return deserialize(cl, null, file);
    }

    /**
     * Returns something else than null on success
     * 
     * @param type
     * @param file
     * @return
     */
    private static <T> T deserialize(TypeToken<T> type, File file) {
        return deserialize(null, type, file);
    }

    /**
     * Returns something else than null on success
     * 
     * @param cl
     * @param type
     * @param file
     * @return
     */
    private static <T> T deserialize(Class<T> cl, TypeToken<T> type, File file) {
        long start = System.currentTimeMillis();
        T out;
        String json;
        try {
            if (file.exists())
            {
                json = getJsonStringFromFile(file);

                Gson gson = new Gson();
                if (cl != null)
                {
                    out = gson.fromJson(json, cl);
                } else
                {
                    out = gson.fromJson(json, type.getType());
                }
            } else
            {
                CommonUtils.debug(TAG, "deserialize from file %1$s failed, doesn't exist",
                        file.getAbsolutePath());
                return null;
            }
        } catch (Exception e) {
            GuiUtils.noAlertError(TAG, e);
            CommonUtils
                    .error(TAG,
                            CommonUtils.format("deserialize from file %1$s failed",
                                    file.getAbsolutePath()));
            return null;
        }
        TrackerUtils.trackDataLoadTiming(System.currentTimeMillis()
                - start, CommonUtils.format("deserialize from file %1$s", file.getAbsolutePath()),
                TAG);
        return out;
    }

    private static String getJsonStringFromFile(File file) throws FileNotFoundException,
            UnsupportedEncodingException, IOException {
        String json;
        FileInputStream fis;
        InputStreamReader isr;
        fis = new FileInputStream(file);
        isr = new InputStreamReader(fis, "UTF-8");
        StringBuilder sb = new StringBuilder();
        final char[] buffer = new char[1024];
        while (true) {
            int rsz = isr.read(buffer, 0, buffer.length);
            if (rsz < 0)
                break;
            sb.append(buffer, 0, rsz);
        }
        isr.close();
        json = sb.toString();
        return json;
    }

    /* Return a unique hash for a given SKU. */
    public static String encodeSKU(String SKU) {
        return Base64Coder_magento.encodeString(SKU).replace("+", "_").replace("/", "-")
                .replace("=", "");
    }

    public static String encodeURL(String url) {
        return Base64Coder_magento.encodeString(url).replace("+", "_").replace("/", "-")
                .replace("=", "");
    }

    /* Get a directory name for a given job type. */
    private static String getCachedJobSubdirName(int resourceType) {
        switch (resourceType) {
            case MageventoryConstants.RES_UPLOAD_IMAGE:
                return "UPLOAD_IMAGE";
            case MageventoryConstants.RES_CATALOG_PRODUCT_SELL:
                return "SELL";
            case MageventoryConstants.RES_ADD_PRODUCT_TO_CART:
                return "ADD_TO_CART";
            case MageventoryConstants.RES_ORDER_SHIPMENT_CREATE:
                return "SHIPMENT";
            case MageventoryConstants.RES_SELL_MULTIPLE_PRODUCTS:
                return "SELL_MULTIPLE";
            default:
                return null;
        }
    }

    /* Get a filename for a given job type (job is extracted from jobID). */
    private static String getCachedResourceFileName(JobID jobID) {
        switch (jobID.getJobType()) {
            case MageventoryConstants.RES_UPLOAD_IMAGE:
            case MageventoryConstants.RES_CATALOG_PRODUCT_SELL:
            case MageventoryConstants.RES_ADD_PRODUCT_TO_CART:
            case MageventoryConstants.RES_ORDER_SHIPMENT_CREATE:
            case MageventoryConstants.RES_SELL_MULTIPLE_PRODUCTS:
                return jobID.getTimeStamp() + ".json";

            case MageventoryConstants.RES_CATALOG_PRODUCT_CREATE:
                return "new_prod.json";
            case MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE:
                return "edit_prod.json";
            case MageventoryConstants.RES_CATALOG_PRODUCT_SUBMIT_TO_TM:
                return "submit_to_tm.json";

            default:
                return null;
        }
    }

    /* Return a directory where a given job resides. */
    private static File getDirectoryAssociatedWithJob(JobID jobID, boolean createDirectories) {
        File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);

        dir = new File(dir, encodeURL(jobID.getUrl()));
        dir = new File(dir, encodeSKU(jobID.getSKU()));

        String subdir = getCachedJobSubdirName(jobID.getJobType());

        if (subdir != null) {
            dir = new File(dir, subdir);
        }

        if (createDirectories == true) {
            if (!dir.exists()) {
                Log.d(JCM_TAG, "getDirectoryAssociatedWithJob: Directory doesn't exist, creating: "
                        + dir.getAbsolutePath());

                if (!dir.mkdirs()) {
                    Log.d(JCM_TAG, "getDirectoryAssociatedWithJob: Unable to create directory: "
                            + dir.getAbsolutePath());

                    return null;
                }
            }
        }

        return dir;
    }

    /*
     * Return a file associated with a given job. It can be used for example to
     * serialize a job in the right place.
     */
    private static File getFileAssociatedWithJob(JobID jobID, boolean createDirectories) {
        File fileToSave = new File(getDirectoryAssociatedWithJob(jobID, createDirectories),
                getCachedResourceFileName(jobID));
        return fileToSave;
    }

    /* Return a file path associated with a given job. */
    public static String getFilePathAssociatedWithJob(JobID jobID) {
        synchronized (sSynchronizationObject) {
            return getFileAssociatedWithJob(jobID, false).getAbsolutePath();
        }
    }

    public static void moveSKUdir(String url, String SKUfrom, String SKUto)
    {
        synchronized (sProductDetailsLock) {

            File dirFrom = new File(Environment.getExternalStorageDirectory(),
                    MyApplication.APP_DIR_NAME);
            dirFrom = new File(dirFrom, encodeURL(url));
            dirFrom = new File(dirFrom, encodeSKU(SKUfrom));

            File dirTo = new File(Environment.getExternalStorageDirectory(),
                    MyApplication.APP_DIR_NAME);
            dirTo = new File(dirTo, encodeURL(url));
            dirTo = new File(dirTo, encodeSKU(SKUto));

            if (dirTo.exists())
            {
                /*
                 * If the directory associated with the new sku already exists,
                 * this means that product details from product update call was
                 * already saved there and it is the newest product details info
                 * we have. Let's save it and continute.
                 */
                File newProdDetFile = getProductDetailsFile(SKUto, url, false);
                File oldProdDetFile = getProductDetailsFile(SKUfrom, url, false);
                if (newProdDetFile.exists())
                {
                    if (oldProdDetFile.exists())
                    {
                        oldProdDetFile.delete();
                    }

                    newProdDetFile.renameTo(oldProdDetFile);
                }

                deleteRecursive(dirTo);
            }

            dirFrom.renameTo(dirTo);
            ProductAliasCacheManager.getInstance().updateSkuIfExists(SKUfrom, SKUto, url);
        }
    }

    /*
     * Save job in the cache. There is a problem with storing a job in the
     * cache. If multiple pieces of code were trying to do that without any
     * coordination the state of such job could cause problems difficult to
     * debug. This is why all code in the application should be following rules:
     * 1. The service and the queue have the absolute priority of restoring and
     * storing jobs from/in the cache. No other code should interfere with them.
     * When a job starts it is deserialized by the queue and passed to the
     * service. The service is then allowed to store the job any number of times
     * it wants (for example it's doing that every time upload progress
     * changes). When the job is finished the job is either deleted or modified
     * and stored back in the cache. When these things are happening no other
     * code should store the job that is being processed. There is a way of
     * checking whether a job is being processed: The JobQueue provides
     * getCurrentJob() function which returns the job that the queue
     * deserialized and passed to the service for processing. In order to store
     * a job in the queue the interested code should first lock the
     * sQueueSynchronizationObject which prevents the current job changes and
     * then check the current job. If the current job is equal to the job which
     * the calling code wants to store then it should either do it later or not
     * at all. 2. All pieces of code that want to restore, then modify, then
     * store a job in the cache should lock the
     * JobCacheManager.sSynchronizationObject before doing that to prevent a
     * conflict with other pieces of code.
     */
    public static boolean store(Job job) {
        synchronized (sSynchronizationObject) {
            Log.d(JCM_TAG, "Storing job in the queue: jobType: " + job.getJobID().getJobType() +
                    ", prodID: " + job.getJobID().getProductID() +
                    ", SKU: " + job.getJobID().getSKU() +
                    ", timestamp: " + job.getJobID().getTimeStamp() +
                    ", url: " + job.getJobID().getUrl());

            File fileToSave = getFileAssociatedWithJob(job.getJobID(), true);

            if (fileToSave == null)
            {
                Log.d(JCM_TAG, "File associated with job is NULL!");
            }
            else
            {
                Log.d(JCM_TAG, "File associated with job: " + fileToSave.getAbsolutePath());
            }

            if (fileToSave != null && serialize(job, fileToSave) == true)
            {
                Log.d(JCM_TAG, "Storing job file successful.");
                return true;
            }
            else
            {
                Log.d(JCM_TAG, "Storing job file failed.");
                return false;
            }
        }
    }

    /*
     * Store a job in a directory that doesn't necessarily correspond to the sku
     * found in the job id of the job object passed.
     */
    public static boolean store(Job job, String SKU) {
        synchronized (sSynchronizationObject) {
            /* Create fake job id just to get the file associated with job. */
            JobID jobID = new JobID(job.getJobID().getTimeStamp(), job.getJobID().getProductID(),
                    job.getJobType(), SKU, job.getUrl());

            File fileToSave = getFileAssociatedWithJob(jobID, true);

            if (fileToSave != null && serialize(job, fileToSave) == true)
                return true;
            else
                return false;
        }
    }

    /* Load job from the cache. */
    public static Job restore(JobID jobID) {
        synchronized (sSynchronizationObject) {
            File fileToRead = getFileAssociatedWithJob(jobID, false);

            if (fileToRead == null)
                return null;
            else
                return deserialize(Job.class, fileToRead);
        }
    }

    /* Remove job from cache. */
    public static void removeFromCache(JobID jobID) {
        synchronized (sSynchronizationObject) {
            File fileToRemove = getFileAssociatedWithJob(jobID, false);

            if (jobID.getJobType() == MageventoryConstants.RES_SELL_MULTIPLE_PRODUCTS)
            {
                Job job = deserialize(Job.class, fileToRemove);

                if (job != null)
                {
                    removeMutlipleSellJobStubs(job, jobID.getUrl());
                }
            }

            if (fileToRemove != null) {
                fileToRemove.delete();
            }

        }
    }

    public static File getImageUploadDirectory(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            return getDirectoryAssociatedWithJob(new JobID(-1,
                    MageventoryConstants.RES_UPLOAD_IMAGE, SKU, url), true);
        }
    }

    public static File getSellDirectory(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            return getDirectoryAssociatedWithJob(new JobID(-1,
                    MageventoryConstants.RES_CATALOG_PRODUCT_SELL, SKU, url),
                    true);
        }
    }

    public static File getAddToCartDirectory(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            return getDirectoryAssociatedWithJob(new JobID(-1,
                    MageventoryConstants.RES_ADD_PRODUCT_TO_CART, SKU, url),
                    true);
        }
    }

    public static File getMultipleProductSellDirectory(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            File dir = new File(Environment.getExternalStorageDirectory(),
                    MyApplication.APP_DIR_NAME);

            dir = new File(dir, encodeURL(url));
            dir = new File(dir, encodeSKU(SKU));

            String subdir = "SELL_MULTIPLE_POINTERS";

            dir = new File(dir, subdir);

            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    return null;
                }
            }

            return dir;
        }
    }

    public static File getShipmentDirectory(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            return getDirectoryAssociatedWithJob(new JobID(-1,
                    MageventoryConstants.RES_ORDER_SHIPMENT_CREATE, SKU, url),
                    true);
        }
    }

    /* Load all upload jobs for a given SKU. */
    public static List<Job> restoreImageUploadJobs(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            File uploadDir = getImageUploadDirectory(SKU, url);
            List<Job> out = new ArrayList<Job>();

            if (uploadDir == null)
                return out;

            File[] jobFileList = uploadDir.listFiles();

            if (jobFileList != null) {
                for (int i = 0; i < jobFileList.length; i++) {
                    File jobFile = jobFileList[i];
                    if (jobFile.getName().endsWith(".json")) {
                        Job job = deserialize(Job.class, jobFileList[i]);
                        if (job != null)
                            out.add(job);
                    }
                }
            }

            return out;
        }
    }

    /* Load all sell jobs for a given SKU. */
    public static List<Job> restoreSellJobs(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            File sellDir = getSellDirectory(SKU, url);
            List<Job> out = new ArrayList<Job>();

            if (sellDir == null)
                return out;

            File[] jobFileList = sellDir.listFiles();

            if (jobFileList != null) {
                for (int i = 0; i < jobFileList.length; i++) {
                    Job job = deserialize(Job.class, jobFileList[i]);
                    if (job != null)
                        out.add(job);
                }
            }

            out.addAll(restoreMultipleProductSellJobs(SKU, url));

            return out;
        }
    }

    /* Load all "add to cart" jobs for a given SKU. */
    public static List<Job> restoreAddToCartJobs(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            File addToCartDir = getAddToCartDirectory(SKU, url);
            List<Job> out = new ArrayList<Job>();

            if (addToCartDir == null)
                return out;

            File[] jobFileList = addToCartDir.listFiles();

            if (jobFileList != null) {
                for (int i = 0; i < jobFileList.length; i++) {
                    Job job = deserialize(Job.class, jobFileList[i]);
                    if (job != null)
                        out.add(job);
                }
            }

            return out;
        }
    }

    private static File getSellJobStubFile(Job sellJob, String sku, String url)
    {
        File jobStub = getMultipleProductSellDirectory(sku, url);
        jobStub = new File(jobStub, "" + sellJob.getJobID().getTimeStamp() + ".json");

        return jobStub;
    }

    public static void storeMutlipleSellJobStubs(Job sellJob, String url)
    {
        synchronized (sSynchronizationObject) {

            String jobPath = getFilePathAssociatedWithJob(sellJob.getJobID());

            String[] skusArray = JobCacheManager
                    .getStringArrayFromDeserializedItem(sellJob.getExtras().get(
                            MageventoryConstants.EKEY_PRODUCT_SKUS_TO_SELL_ARRAY));

            for (String sku : skusArray)
            {
                File jobStubFile = getSellJobStubFile(sellJob, sku, url);
                serialize(jobPath, jobStubFile);
            }
        }
    }

    public static void removeMutlipleSellJobStubs(Job sellJob, String url)
    {
        synchronized (sSynchronizationObject) {
            String[] skusArray = JobCacheManager
                    .getStringArrayFromDeserializedItem(sellJob.getExtras().get(
                            MageventoryConstants.EKEY_PRODUCT_SKUS_TO_SELL_ARRAY));

            for (String sku : skusArray)
            {
                File jobStubFile = getSellJobStubFile(sellJob, sku, url);
                if (jobStubFile.exists())
                    jobStubFile.delete();
            }
        }
    }

    /* Load all multiple product sell jobs for a given SKU. */
    public static List<Job> restoreMultipleProductSellJobs(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            File sellDir = getMultipleProductSellDirectory(SKU, url);
            List<Job> out = new ArrayList<Job>();

            if (sellDir == null)
                return out;

            File[] jobFileList = sellDir.listFiles();

            if (jobFileList != null) {
                for (int i = 0; i < jobFileList.length; i++) {
                    String jobPath = deserialize(String.class, jobFileList[i]);
                    Job job = deserialize(Job.class, new File(jobPath));
                    if (job != null)
                    {
                        out.add(job);
                    }
                    else
                    {
                        jobFileList[i].delete();
                    }
                }
            }

            return out;
        }
    }

    /* Load all shipment jobs for a given SKU. */
    public static List<Job> restoreShipmentJobs(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            File shipmentDir = getShipmentDirectory(SKU, url);
            List<Job> out = new ArrayList<Job>();

            if (shipmentDir == null)
                return out;

            File[] jobFileList = shipmentDir.listFiles();

            if (jobFileList != null) {
                for (int i = 0; i < jobFileList.length; i++) {
                    Job job = deserialize(Job.class, jobFileList[i]);
                    if (job != null)
                        out.add(job);
                }
            }

            return out;
        }
    }

    /* Load edit job for a given SKU. */
    public static Job restoreEditJob(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            File file = getFileAssociatedWithJob(new JobID(-1,
                    MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE, SKU, url),
                    false);
            Job job = null;

            if (file.exists()) {
                job = deserialize(Job.class, file);
            }

            return job;
        }
    }

    /* Load "sell multiple products" job for a given SKU. */
    public static Job restoreSellMultipleProductsJob(String SKU, String url, String orderIncrementID) {
        synchronized (sSynchronizationObject) {

            JobID jid = new JobID(Long.parseLong(orderIncrementID), -1,
                    MageventoryConstants.RES_SELL_MULTIPLE_PRODUCTS, SKU, url);

            File file = getFileAssociatedWithJob(jid,
                    false);
            Job job = null;

            if (file.exists()) {
                job = deserialize(Job.class, file);
            }

            return job;
        }
    }

    /* Load product creation job for a given SKU. */
    public static Job restoreProductCreationJob(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            File file = getFileAssociatedWithJob(new JobID(-1,
                    MageventoryConstants.RES_CATALOG_PRODUCT_CREATE, SKU, url),
                    false);
            Job job = null;

            if (file.exists()) {
                job = deserialize(Job.class, file);
            }

            return job;
        }
    }

    /* Load "submit to TM" job for a given SKU. */
    public static Job restoreSubmitToTMJob(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            File file = getFileAssociatedWithJob(new JobID(-1,
                    MageventoryConstants.RES_CATALOG_PRODUCT_SUBMIT_TO_TM, SKU, url),
                    false);
            Job job = null;

            if (file.exists()) {
                job = deserialize(Job.class, file);
            }

            return job;
        }
    }

    /* ======================================================================== */
    /* Queue database dump download */
    /* ======================================================================== */

    public static File getQueueDatabaseDumpDirectory(boolean createIfNotExists) {
        synchronized (sSynchronizationObject) {
            File dir = new File(Environment.getExternalStorageDirectory(),
                    MyApplication.APP_DIR_NAME);
            dir = new File(dir, QUEUE_DATABASE_DUMP_DIR_NAME);

            if (createIfNotExists && !dir.exists()) {
                dir.mkdirs();
                checkDirectoryExists(dir);
            }

            return dir;
        }
    }

    /* Pass null as "dir" parameter to use the default directory */
    public static File getQueuePendingTableDumpFile(File dir) {
        synchronized (sSynchronizationObject) {
            File file;

            if (dir == null)
            {
                file = new File(getQueueDatabaseDumpDirectory(true),
                        QUEUE_PENDING_TABLE_DUMP_FILE_NAME);
            }
            else
            {
                file = new File(dir, QUEUE_PENDING_TABLE_DUMP_FILE_NAME);
            }

            return file;
        }
    }

    /* Pass null as "dir" parameter to use the default directory */
    public static File getQueueFailedTableDumpFile(File dir) {
        synchronized (sSynchronizationObject) {
            File file;

            if (dir == null)
            {
                file = new File(getQueueDatabaseDumpDirectory(true),
                        QUEUE_FAILED_TABLE_DUMP_FILE_NAME);
            }
            else
            {
                file = new File(dir, QUEUE_FAILED_TABLE_DUMP_FILE_NAME);
            }

            return file;
        }
    }

    /* ======================================================================== */
    /* Image download */
    /* ======================================================================== */

    public static File getImageFullPreviewDirectory(String SKU, String url,
            boolean createIfNotExists) {
        synchronized (sSynchronizationObject) {
            File dir = new File(Environment.getExternalStorageDirectory(),
                    MyApplication.APP_DIR_NAME);
            dir = new File(dir, encodeURL(url));
            dir = new File(dir, encodeSKU(SKU));
            dir = new File(dir, DOWNLOAD_IMAGE_PREVIEW_DIR_NAME);

            if (createIfNotExists && !dir.exists()) {
                dir.mkdirs();
                checkDirectoryExists(dir);
            }

            return dir;
        }
    }

    public static void clearImageFullPreviewDirectory(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            File dir = getImageFullPreviewDirectory(SKU, url, false);

            if (dir.exists()) {
                for (File child : dir.listFiles()) {
                    child.delete();
                }

                dir.delete();
            }
        }
    }

    public static File getImageDownloadDirectory(String SKU, String url, boolean createIfNotExists) {
        synchronized (sSynchronizationObject) {
            File dir = new File(Environment.getExternalStorageDirectory(),
                    MyApplication.APP_DIR_NAME);
            dir = new File(dir, encodeURL(url));
            createNoMediaFileIfNecessary(dir);
            dir = new File(dir, encodeSKU(SKU));
            dir = new File(dir, DOWNLOAD_IMAGE_DIR);

            if (createIfNotExists && !dir.exists()) {
                dir.mkdirs();
                checkDirectoryExists(dir);
            }

            createNoMediaFileIfNecessary(dir);
            return dir;
        }
    }

    /**
     * Create the .nomedia file in the directory if it doesn't exist yet.
     * 
     * @see {@link #NOMEDIA_FILE}
     * @param dir
     */
    public static void createNoMediaFileIfNecessary(File dir) {
        File nomedia = new File(dir, NOMEDIA_FILE);
        if (!nomedia.exists()) {
            try {
                nomedia.createNewFile();
            } catch (IOException e) {
                CommonUtils.error(TAG, e);
            }
        }
    }

    public static void checkDirectoryExists(File dir) {
        if (!dir.isDirectory()) {
            String message = CommonUtils.format("Failed to create directory %1$s",
                    dir.getAbsolutePath());
            CommonUtils.error(TAG, message);
            TrackerUtils.trackErrorEvent(TAG + ".checkDirectoryExistsFailed", message);
        }
    }

    public static void clearImageDownloadDirectory(String SKU, String url) {
        synchronized (sSynchronizationObject) {
            File dir = getImageDownloadDirectory(SKU, url, false);

            if (dir.exists()) {
                for (File child : dir.listFiles()) {
                    child.delete();
                }
                
                //dir.delete() was here, but mkdirs right after delete fails for unknown reason
            }
        }
    }

    /* ======================================================================== */
    /* Product details data */
    /* ======================================================================== */

    private static Product sRAMCachedProductDetails;
    private static Object sRAMCachedProductDetailsLock = new Object();

    public static void killRAMCachedProductDetails()
    {
        synchronized (sRAMCachedProductDetailsLock) {
            sRAMCachedProductDetails = null;
        }
    }

    private static File getProductDetailsFile(String SKU, String url, boolean createDirectories) {
        File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        file = new File(file, encodeURL(url));
        file = new File(file, encodeSKU(SKU));

        if (createDirectories == true) {
            if (!file.exists()) {
                file.mkdirs();
                checkDirectoryExists(file);
            }
        }

        return new File(file, PRODUCT_DETAILS_FILE_NAME);
    }

    /*
     * Get product details data merged with the currently pending product edit
     * job (if any)
     */
    private static Product getMergedProductDetails(Product product, String url)
    {
        if (product == null || product.getSku() == null) {
            return null;
        }

        /*
         * A reference pointing to a product which is going to be serialized at
         * the end.
         */
        Product mergedProduct = product;

        Job existingEditJob = JobCacheManager.restoreEditJob(product.getSku(), url);

        /* Check if an edit job exists in the pending table. */
        if (existingEditJob != null && existingEditJob.getPending() == true)
        {
            /*
             * Product edit job exists we will do either one way or two way
             * merge.
             */

            Product prodBeforeChanges = product.getCopy();

            Job currentJob = JobQueue.getCurrentJob();

            /*
             * If this is true after next checks it means we will do the merge
             * both ways (from product details to edit job and the other way
             * around as well). If this will be false it means we just merge
             * product edit job to product details.
             */
            boolean twoWayMerge = true;

            /*
             * If the currently pending job is an edit job and if it has the
             * same SKU that the product passed to this function we don't merge
             * the product to the product edit job but we still do it the other
             * way around.
             */
            if (currentJob != null
                    && currentJob.getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE &&
                    currentJob.getSKU().equals(product.getSku()))
            {
                twoWayMerge = false;
            }

            List<String> updatedKeys = (List<String>) existingEditJob
                    .getExtraInfo(MageventoryConstants.EKEY_UPDATED_KEYS_LIST);

            // special keys which should not be merged
            Set<String> unmergeableKeys = new HashSet<String>() {
                private static final long serialVersionUID = 1L;
                {
                    add(MageventoryConstants.MAGEKEY_API_UPDATE_IF_EXISTS);
                    add(MageventoryConstants.MAGEKEY_API_LINK_WITH_PRODUCT);
                    add(MageventoryConstants.EKEY_UPDATED_KEYS_LIST);
                }
            };

            for (String key : existingEditJob.getExtras().keySet())
            {
                /*
                 * This is a special key that is not sent to the server. We
                 * shouldn't merge the value of this key.
                 */
                if (unmergeableKeys.contains(key))
                    continue;

                if (updatedKeys.contains(key))
                {
                    /*
                     * In case of keys that were updated by the user we copy the
                     * values from the edit job file to product details file.
                     */

                    /*
                     * We send a different key to the server than the one we get
                     * from the server in case of categories.
                     */
                    if (key.equals(MageventoryConstants.MAGEKEY_PRODUCT_CATEGORIES))
                    {
                        mergedProduct.getData().put(
                                MageventoryConstants.MAGEKEY_PRODUCT_CATEGORY_IDS,
                                existingEditJob.getExtraInfo(key));
                    }
                    else
                    {
                        mergedProduct.getData().put(key, existingEditJob.getExtraInfo(key));
                    }
                }
                else
                {
                    /*
                     * In case of keys that were not updated by the user we
                     * merge the values from the product details file to edit
                     * job file but only in case the product edit request is not
                     * being processed while we're doing that.
                     */

                    if (twoWayMerge)
                    {
                        /*
                         * We send a different key to the server than the one we
                         * get from the server in case of categories.
                         */
                        if (key.equals(MageventoryConstants.MAGEKEY_PRODUCT_CATEGORIES))
                        {
                            existingEditJob.putExtraInfo(
                                    key,
                                    mergedProduct.getData().get(
                                            MageventoryConstants.MAGEKEY_PRODUCT_CATEGORY_IDS));
                        }
                        else
                        {
                            existingEditJob.putExtraInfo(key, mergedProduct.getData().get(key));
                        }
                    }
                }
            }

            /* Store the merged edit job if we were in two way merge mode. */
            if (twoWayMerge)
            {
                store(existingEditJob);
            }

            /*
             * Reinitialize all fields in Product class with the new data from
             * the map.
             */
            mergedProduct = new Product(mergedProduct.getData());

            /*
             * Remember the copy of original product to easily roll back later
             * on.
             */
            mergedProduct.setUnmergedProduct(prodBeforeChanges);
        }
        else
        {
            /*
             * Edit job doesn't exist in the pending table. We don't do any
             * merge.
             */
        }

        // merge last used query information from the stored product details
        String lastUsedQuery = ProductUtils.getProductLastUsedQuery(mergedProduct.getSku(), url);
        if(!TextUtils.isEmpty(lastUsedQuery)){
            mergedProduct.getData().put(MageventoryConstants.MAGEKEY_PRODUCT_LAST_USED_QUERY,
                    lastUsedQuery);
        }

        return mergedProduct;
    }

    /*
     * Store product detail in the cache but merge it with currently pending
     * product edit job if any. This method waits for the product details data
     * to be serialized in the cache before returning.
     */
    public static void storeProductDetailsWithMergeSynchronous(Product product, String url) {
        synchronized (JobQueue.sQueueSynchronizationObject) {
            synchronized (sProductDetailsLock) {

                Product mergedProduct = getMergedProductDetails(product, url);

                if (mergedProduct == null)
                {
                    return;
                }

                synchronized (sRAMCachedProductDetailsLock)
                {
                    sRAMCachedProductDetails = null;
                }

                serialize(mergedProduct, getProductDetailsFile(product.getSku(), url, true));
                ProductAliasCacheManager.getInstance().addOrUpdate(mergedProduct, url);
            }
        }
    }

    /*
     * Store product detail in the cache but merge it with currently pending
     * product edit job if any. This method doesn't wait for the product details
     * data to be serialized in the cache before returning.
     */
    public static void storeProductDetailsWithMergeAsynchronous(Product product, final String url) {
        Product mergedProduct;

        synchronized (JobQueue.sQueueSynchronizationObject) {
            synchronized (sProductDetailsLock) {

                mergedProduct = getMergedProductDetails(product, url);

                if (mergedProduct == null)
                {
                    return;
                }
            }
        }

        synchronized (sRAMCachedProductDetailsLock)
        {
            sRAMCachedProductDetails = mergedProduct;
        }

        final Product finalMergedProduct = mergedProduct;

        Thread thread = new Thread()
        {
            @Override
            public void run() {
                synchronized (sProductDetailsLock) {
                    serialize(finalMergedProduct,
                            getProductDetailsFile(finalMergedProduct.getSku(), url, true));
                    ProductAliasCacheManager.getInstance().addOrUpdate(finalMergedProduct, url);
                }
            }
        };
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    public static void storeProductDetails(Product product, String url) {
        synchronized (sProductDetailsLock) {
            if (product == null || product.getSku() == null) {
                return;
            }
            serialize(product, getProductDetailsFile(product.getSku(), url, true));
            ProductAliasCacheManager.getInstance().addOrUpdate(product, url);
        }
    }

    /*
     * Product details data can be merged with product edit job in two cases: -
     * when product details is being saved to the cache - when edit job starts
     * or finishes This function is handling the second case.
     */
    public static void remergeProductDetailsWithEditJob(String sku, String url)
    {
        synchronized (sProductDetailsLock) {

            Product product = restoreProductDetails(sku, url);

            if (product != null)
            {
                /*
                 * We want to merge with the unmerged version of the product
                 * details. If getUnmergedProduct() returns null it means no
                 * merge took place on this instance of product details.
                 */
                if (product.getUnmergedProduct() != null)
                {
                    JobCacheManager.storeProductDetailsWithMergeSynchronous(
                            product.getUnmergedProduct(), url);
                }
                else
                {
                    JobCacheManager.storeProductDetailsWithMergeSynchronous(product, url);
                }
            }
        }
    }

    public static Product restoreProductDetails(String SKU, String url) {
        synchronized (sRAMCachedProductDetailsLock)
        {
            if (sRAMCachedProductDetails != null && sRAMCachedProductDetails.getSku() != null
                    && sRAMCachedProductDetails.getSku().equals(SKU))
            {
                return sRAMCachedProductDetails;
            }
        }

        synchronized (sProductDetailsLock)
        {
            return deserialize(Product.class, getProductDetailsFile(SKU, url, false));
        }
    }

    public static void removeProductDetails(String SKU, String url) {
        synchronized (sProductDetailsLock) {
            File f = getProductDetailsFile(SKU, url, false);

            if (f.exists()) {
                f.delete();
            }
            ProductAliasCacheManager.getInstance().deleteProductFromCache(SKU, url);
        }

        synchronized (sRAMCachedProductDetailsLock)
        {
            if (sRAMCachedProductDetails != null && sRAMCachedProductDetails.getSku() != null
                    && sRAMCachedProductDetails.getSku().equals(SKU))
            {
                sRAMCachedProductDetails = null;
            }
        }
    }

    public static boolean productDetailsExist(String SKU, String url) {

        synchronized (sRAMCachedProductDetailsLock)
        {
            if (sRAMCachedProductDetails != null && sRAMCachedProductDetails.getSku() != null
                    && sRAMCachedProductDetails.getSku().equals(SKU))
            {
                return true;
            }
        }

        synchronized (sProductDetailsLock)
        {
            return getProductDetailsFile(SKU, url, false).exists();
        }
    }

    /**
     * Check whether product details exists for the SKU. If not then try to get
     * linked SKU information from product alias cache and check existing for it
     * 
     * @param SKU the product SKU or barcode
     * @param url
     * @param checkAliases whether or not check alias cache
     * @return result containing information whether product is present in cache
     *         and updated SKU to alias value if checkAliases specified and
     *         barcode is passed to the method instead of SKU
     */
    public static ProductDetailsExistResult productDetailsExist(String SKU, String url,
            boolean checkAliases)
    {
        boolean result = false;
        if (productDetailsExist(SKU, url))
        {
            result = true;
        } else if (checkAliases)
        {
            String linkedSku = ProductAliasCacheManager.getInstance().getCachedSkuForBarcode(
                    SKU, url);
            if (!TextUtils.isEmpty(linkedSku) &&
                    JobCacheManager.productDetailsExist(linkedSku, url))
            {
                SKU = linkedSku;
                result = true;
            }
        }
        return new ProductDetailsExistResult(SKU, result);
    }

    /**
     * Resulting object for the productDetailsExist method
     */
    public static class ProductDetailsExistResult
    {
        String sku;
        boolean existing;

        public ProductDetailsExistResult(String sku, boolean existing) {
            super();
            this.sku = sku;
            this.existing = existing;
        }

        public String getSku() {
            return sku;
        }

        public boolean isExisting() {
            return existing;
        }

    }

    /* ======================================================================== */
    /* Order list data */
    /* ======================================================================== */

    private static File getOrderListDir(boolean createIfNotExists, String url) {
        File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        dir = new File(dir, encodeURL(url));
        dir = new File(dir, ORDER_LIST_DIR_NAME);
        if (createIfNotExists && !dir.exists()) {
            dir.mkdirs();
            checkDirectoryExists(dir);
        }
        return dir;
    }

    private static File getOrderListFile(boolean createDirectories, String[] params, String url) {
        File file = getOrderListDir(createDirectories, url);

        StringBuilder fileName = new StringBuilder();

        fileName.append("order_list_");

        if (params.length >= 1 && params[0] != null) {
            fileName.append(params[0]);
        }

        fileName.append(".json");

        return new File(file, fileName.toString());
    }

    public static void saveEmptyOrderList(String[] params, String url)
    {
        synchronized (sSynchronizationObject) {
            Map<String, Object> orderMap = new HashMap<String, Object>();
            orderMap.put("orders", new Object[0]);
            OrderList orderList = new OrderList(null, orderMap);

            storeOrderList(orderList, params, url);
        }
    }

    /* Add an order to order list */
    public static void addToOrderList(Map<String, Object> order, String[] params, String url) {
        synchronized (sSynchronizationObject) {
            OrderList orderList = restoreOrderList(params, url);
            Map<String, Object> orderMap = orderList == null ? null : orderList.getData();

            if (orderList == null || orderMap == null)
            {
                orderMap = new HashMap<String, Object>();
                orderMap.put("orders", new Object[0]);
                orderList = new OrderList(null, orderMap);
            }

            ArrayList<Object> orders = new ArrayList<Object>();

            for (Object orderObj : JobCacheManager.getObjectArrayFromDeserializedItem(orderMap
                    .get("orders")))
            {
                orders.add(orderObj);
            }

            orders.add(order);

            orderMap.put("orders", orders.toArray(new Object[0]));

            storeOrderList(orderList, params, url);
        }
    }

    /* Add an order to order list */
    public static void removeFromOrderList(String orderIncrementID, String[] params, String url) {
        synchronized (sSynchronizationObject) {
            OrderList orderList = restoreOrderList(params, url);
            Map<String, Object> orderMap = orderList == null ? null : orderList.getData();

            if (orderList == null || orderMap == null)
            {
                return;
            }

            ArrayList<Object> orders = new ArrayList<Object>();

            for (Object orderObj : JobCacheManager.getObjectArrayFromDeserializedItem(orderMap
                    .get("orders")))
            {
                String incID = (String) ((Map<String, Object>) orderObj).get("increment_id");

                if (!orderIncrementID.equals(incID))
                {
                    orders.add(orderObj);
                }
            }

            orderMap.put("orders", orders.toArray(new Object[0]));

            storeOrderList(orderList, params, url);
        }
    }

    public static void storeOrderList(OrderList orderList, String[] params, String url) {
        synchronized (sSynchronizationObject) {
            if (orderList == null) {
                return;
            }
            serialize(orderList, getOrderListFile(true, params, url));
        }
    }

    public static OrderList restoreOrderList(String[] params, String url) {
        synchronized (sSynchronizationObject) {
            return deserialize(OrderList.class, getOrderListFile(false, params, url));
        }
    }

    public static void removeOrderList(String url) {
        synchronized (sSynchronizationObject) {
            File dir = getOrderListDir(false, url);

            if (dir.exists()) {
                for (File child : dir.listFiles()) {
                    child.delete();
                }

                dir.delete();
            }
        }
    }

    public static boolean orderListExist(String[] params, String url) {
        return getOrderListFile(false, params, url).exists();
    }

    /* ======================================================================== */
    /* Order carriers data */
    /* ======================================================================== */

    private static File getOrderCarriersFile(boolean createDirectories, String url) {
        File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        file = new File(file, encodeURL(url));

        if (createDirectories == true) {
            if (!file.exists()) {
                file.mkdirs();
                checkDirectoryExists(file);
            }
        }

        return new File(file, ORDER_CARRIERS_FILE_NAME);
    }

    public static void storeOrderCarriers(CarriersList carriers, String url) {
        synchronized (sSynchronizationObject) {
            if (carriers == null) {
                return;
            }
            serialize(carriers, getOrderCarriersFile(true, url));
        }
    }

    public static CarriersList restoreOrderCarriers(String url) {
        synchronized (sSynchronizationObject) {
            return deserialize(CarriersList.class, getOrderCarriersFile(false, url));
        }
    }

    public static void removeOrderCarriers(String url) {
        synchronized (sSynchronizationObject) {
            File f = getOrderCarriersFile(false, url);

            if (f.exists()) {
                f.delete();
            }
        }
    }

    public static boolean orderCarriersExist(String url) {
        return getOrderCarriersFile(false, url).exists();
    }

    /* ======================================================================== */
    /* Order details data */
    /* ======================================================================== */
    private static File getOrderDetailsDir(boolean createIfNotExists, String url) {
        File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        dir = new File(dir, encodeURL(url));
        dir = new File(dir, ORDER_DETAILS_DIR_NAME);
        if (createIfNotExists && !dir.exists()) {
            dir.mkdirs();
            checkDirectoryExists(dir);
        }
        return dir;
    }

    private static File getOrderDetailsFile(boolean createDirectories, String[] params, String url) {
        File file = getOrderDetailsDir(createDirectories, url);

        StringBuilder fileName = new StringBuilder();

        fileName.append("order_details_");

        if (params.length >= 1 && params[0] != null) {
            fileName.append(params[0]);
        }

        fileName.append(".json");

        return new File(file, fileName.toString());
    }

    /* Params are in a form: {orderIncrementId}. */
    public static void storeOrderDetails(Map<String, Object> orderDetails, String[] params,
            String url) {
        synchronized (sSynchronizationObject) {
            if (orderDetails == null) {
                return;
            }
            serialize(orderDetails, getOrderDetailsFile(true, params, url));
        }
    }

    public static Map<String, Object> restoreOrderDetails(String[] params, String url) {
        synchronized (sSynchronizationObject) {
            return deserialize(new TypeToken<Map<String, Object>>() {
            },
                    getOrderDetailsFile(false, params, url));
        }
    }

    public static void removeOrderDetails(String[] params, String url) {
        synchronized (sSynchronizationObject) {
            File file = getOrderDetailsFile(false, params, url);

            if (file.exists())
                file.delete();
        }
    }

    public static void removeOrderDetails(String url) {
        synchronized (sSynchronizationObject) {
            File dir = getOrderDetailsDir(false, url);

            if (dir.exists()) {
                for (File child : dir.listFiles()) {
                    child.delete();
                }

                dir.delete();
            }
        }
    }

    public static boolean orderDetailsExist(String[] params, String url) {
        return getOrderDetailsFile(false, params, url).exists();
    }

    /* ======================================================================== */
    /* Product list data */
    /* ======================================================================== */

    private static File getProductListDir(boolean createIfNotExists, String url) {
        File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        dir = new File(dir, encodeURL(url));
        dir = new File(dir, PRODUCT_LIST_DIR_NAME);

        if (createIfNotExists && !dir.exists()) {
            dir.mkdirs();
            checkDirectoryExists(dir);
        }

        return dir;
    }

    private static File getProductListFile(boolean createDirectories, String[] params, String url) {
        File file = getProductListDir(createDirectories, url);

        StringBuilder fileName = new StringBuilder();

        fileName.append("product_list_");

        if (params.length >= 1 && params[0] != null) {
            fileName.append(Base64Coder_magento.encodeString(params[0]).replace("+", "_")
                    .replace("/", "-").replace("=", ""));
        }

        fileName.append("_");

        if (params.length >= 2 && params[1] != null
                && (Integer.parseInt(params[1]) != MageventoryConstants.INVALID_CATEGORY_ID)) {
            fileName.append(params[1]);
        }

        fileName.append(".json");

        return new File(file, fileName.toString());
    }

    /* Params are in a form: {nameFilter, categoryId}. */
    public static void storeProductList(List<Map<String, Object>> productList, String[] params,
            String url) {
        synchronized (sSynchronizationObject) {
            if (productList == null) {
                return;
            }
            serialize(productList, getProductListFile(true, params, url));
        }
    }

    public static List<Map<String, Object>> restoreProductList(String[] params, String url) {
        synchronized (sSynchronizationObject) {
            return deserialize(new TypeToken<List<Map<String, Object>>>() {
            },
                    getProductListFile(false, params, url));
        }
    }

    public static void removeAllProductLists(String url) {
        synchronized (sSynchronizationObject) {
            File dir = getProductListDir(false, url);

            if (dir.exists()) {
                for (File child : dir.listFiles()) {
                    child.delete();
                }

                dir.delete();
            }
        }
    }

    public static boolean productListExist(String[] params, String url) {
        return getProductListFile(false, params, url).exists();
    }

    /* ======================================================================== */
    /* Categories data */
    /* ======================================================================== */

    private static File getCategoriesFile(boolean createDirectories, String url) {
        File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        file = new File(file, encodeURL(url));

        if (createDirectories == true) {
            if (!file.exists()) {
                file.mkdirs();
                checkDirectoryExists(file);
            }
        }

        return new File(file, CATEGORIES_LIST_FILE_NAME);
    }

    public static void storeCategories(Map<String, Object> attributes, String url) {
        synchronized (sSynchronizationObject) {
            if (attributes == null) {
                return;
            }
            serialize(attributes, getCategoriesFile(true, url));
        }
    }

    public static Map<String, Object> restoreCategories(String url) {
        synchronized (sSynchronizationObject) {
            return deserialize(new TypeToken<Map<String, Object>>() {
            }, getCategoriesFile(false, url));
        }
    }

    public static void removeCategories(String url) {
        synchronized (sSynchronizationObject) {
            File f = getCategoriesFile(false, url);

            if (f.exists()) {
                f.delete();
            }
        }
    }

    public static boolean categoriesExist(String url) {
        return getCategoriesFile(false, url).exists();
    }

    /* ======================================================================== */
    /* Statistics data */
    /* ======================================================================== */

    private static File getStatisticsFile(boolean createDirectories, String url) {
        File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        file = new File(file, encodeURL(url));

        if (createDirectories == true) {
            if (!file.exists()) {
                file.mkdirs();
                checkDirectoryExists(file);
            }
        }

        return new File(file, STATISTICS_FILE_NAME);
    }

    public static void storeStatistics(Map<String, Object> statistics, String url) {
        synchronized (sSynchronizationObject) {
            if (statistics == null) {
                return;
            }
            serialize(statistics, getStatisticsFile(true, url));
        }
    }

    public static Map<String, Object> restoreStatistics(String url) {
        synchronized (sSynchronizationObject) {
            return deserialize(new TypeToken<Map<String, Object>>() {
            }, getStatisticsFile(false, url));
        }
    }

    public static void removeStatistics(String url) {
        synchronized (sSynchronizationObject) {
            File f = getStatisticsFile(false, url);

            if (f.exists()) {
                f.delete();
            }
        }
    }

    public static boolean statisticsExist(String url) {
        return getStatisticsFile(false, url).exists();
    }

    /* ======================================================================== */
    /* Profiles list data */
    /* ======================================================================== */

    private static File getProfilesListFile(boolean createDirectories, String url) {
        File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        file = new File(file, encodeURL(url));

        if (createDirectories == true) {
            if (!file.exists()) {
                file.mkdirs();
                checkDirectoryExists(file);
            }
        }

        return new File(file, PROFILES_FILE_NAME);
    }

    public static void storeProfilesList(Object[] profilesList, String url) {
        synchronized (sSynchronizationObject) {
            if (profilesList == null) {
                return;
            }
            serialize(profilesList, getProfilesListFile(true, url));
        }
    }

    public static Object[] restoreProfilesList(String url) {
        synchronized (sSynchronizationObject) {
            return (Object[]) deserialize(Object[].class, getProfilesListFile(false, url));
        }
    }

    public static void removeProfilesList(String url) {
        synchronized (sSynchronizationObject) {
            File f = getProfilesListFile(false, url);

            if (f.exists()) {
                f.delete();
            }
        }
    }

    public static boolean profilesListExist(String url) {
        return getProfilesListFile(false, url).exists();
    }

    /* ======================================================================== */
    /* Profile execution */
    /* ======================================================================== */
    private static File getProfileExecutionDir(boolean createIfNotExists, String url) {
        File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        dir = new File(dir, encodeURL(url));
        dir = new File(dir, PROFILE_EXECUTION_DIR_NAME);
        if (createIfNotExists && !dir.exists()) {
            dir.mkdirs();
            checkDirectoryExists(dir);
        }
        return dir;
    }

    private static File getProfileExecutionFile(boolean createDirectories, String[] params,
            String url) {
        File file = getProfileExecutionDir(createDirectories, url);

        StringBuilder fileName = new StringBuilder();

        fileName.append("profile_execution_");

        if (params.length >= 1 && params[0] != null) {
            fileName.append(params[0]);
        }

        fileName.append(".json");

        return new File(file, fileName.toString());
    }

    /* Params are in a form: {profileID}. */
    public static void storeProfileExecution(String profileExecutionMessage, String[] params,
            String url) {
        synchronized (sSynchronizationObject) {
            if (profileExecutionMessage == null) {
                return;
            }
            serialize(profileExecutionMessage, getProfileExecutionFile(true, params, url));
        }
    }

    public static String restoreProfileExecution(String[] params, String url) {
        synchronized (sSynchronizationObject) {
            return deserialize(String.class, getProfileExecutionFile(false, params, url));
        }
    }

    public static void removeProfileExecution(String url) {
        synchronized (sSynchronizationObject) {
            File dir = getProfileExecutionDir(false, url);

            if (dir.exists()) {
                for (File child : dir.listFiles()) {
                    child.delete();
                }

                dir.delete();
            }
        }
    }

    public static boolean profileExecutionExists(String[] params, String url) {
        return getProfileExecutionFile(false, params, url).exists();
    }

    /* ======================================================================== */
    /* Custom attribute set data */
    /* ======================================================================== */

    private static File getAttributeSetsFile(boolean createDirectories, String url) {
        File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        file = new File(file, encodeURL(url));

        if (createDirectories == true) {
            if (!file.exists()) {
                file.mkdirs();
                checkDirectoryExists(file);
            }
        }

        return new File(file, ATTRIBUTE_SETS_FILE_NAME);
    }

    public static void storeAttributeSets(List<Map<String, Object>> attributeSets, String url) {
        if (attributeSets == null) {
            return;
        }
        storeData(attributeSets, getAttributeSetsFile(true, url), url, ATTRIBUTE_SETS_FILE_NAME);
    }

    /*
     * Helper function providing means of updating an attribute map inside of a
     * list of attribute maps.
     */
    private static void updateAttributeInTheAttributeList(List<Map<String, Object>> attribList,
            Map<String, Object> attribute) {
        if (attribList != null) {
            int i = 0;
            for (Map<String, Object> elem : attribList) {
                String codeFromCache = (String) elem
                        .get(MageventoryConstants.MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE);
                String codeToUpdate = (String) attribute
                        .get(MageventoryConstants.MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE);

                if (TextUtils.equals(codeFromCache, codeToUpdate)) {
                    attribList.set(i, attribute);
                    break;
                }
                i++;
            }
        }
    }

    /*
     * When adding an option to a custom attribute through an application we are
     * getting back the whole attribute which we might want to save in the cache
     * without downloading the entire list. This function provides the code that
     * updates just one attribute in the cache.
     */
    public static void updateSingleAttributeInTheCache(Map<String, Object> attribute, String setID,
            String url) {
        synchronized (sSynchronizationObject) {

            List<Map<String, Object>> attrsList = restoreAttributeList(setID, url);

            if (attrsList != null) {
                updateAttributeInTheAttributeList(attrsList, attribute);

                storeAttributeList(attrsList, setID, url);
            }
        }
    }

    public static List<Map<String, Object>> restoreAttributeSets(String url) {
        return restoreData(new TypeToken<List<Map<String, Object>>>() {
        }, getAttributeSetsFile(false, url), url, ATTRIBUTE_SETS_FILE_NAME);
    }

    public static void removeAttributeSets(String url) {
        synchronized (sSynchronizationObject) {
            File f = getAttributeSetsFile(false, url);

            if (f.exists()) {
                f.delete();
            }
            putToRamCache(null, url, ATTRIBUTE_SETS_FILE_NAME);
        }
    }

    public static boolean attributeSetsExist(String url) {
        return dataExists(getAttributeSetsFile(false, url), url, ATTRIBUTE_SETS_FILE_NAME);
    }

    /* ======================================================================== */
    /* Custom attribute list data */
    /* ======================================================================== */
    private static File getAttributeListDir(boolean createIfNotExists, String url) {
        File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        dir = new File(dir, encodeURL(url));
        dir = new File(dir, ATTRIBUTE_LIST_DIR_NAME);
        if (createIfNotExists && !dir.exists()) {
            dir.mkdirs();
            checkDirectoryExists(dir);
        }
        return dir;
    }

    private static File getAttributeListFile(boolean createDirectories, String attributeSetID,
            String url) {
        File file = getAttributeListDir(createDirectories, url);

        StringBuilder fileName = new StringBuilder();

        fileName.append("attribute_list_");
        fileName.append(attributeSetID);
        fileName.append(".json");

        return new File(file, fileName.toString());
    }

    public static void storeAttributeList(List<Map<String, Object>> attributeList,
            String attributeSetID, String url) {
        storeData(attributeList, getAttributeListFile(true, attributeSetID, url), url,
                ATTRIBUTE_LIST_DIR_NAME, attributeSetID);
    }

    public static List<Map<String, Object>> restoreAttributeList(String attributeSetID, String url) {
        return restoreData(new TypeToken<List<Map<String, Object>>>() {
        }, getAttributeListFile(false, attributeSetID, url), url, ATTRIBUTE_LIST_DIR_NAME,
                attributeSetID);
    }

    public static void removeAttributeList(String url) {
        synchronized (sSynchronizationObject) {
            File dir = getAttributeListDir(false, url);

            if (dir.exists()) {
                for (File child : dir.listFiles()) {
                    child.delete();
                }

                dir.delete();
            }

            putToRamCache(null, url, ATTRIBUTE_LIST_DIR_NAME);
        }
    }

    public static boolean attributeListExist(String attributeSetID, String url) {
        return dataExists(getAttributeListFile(false, attributeSetID, url), url,
                ATTRIBUTE_LIST_DIR_NAME, attributeSetID);
    }

    /* ======================================================================== */
    /* Cart items data */
    /* ======================================================================== */
    private static File getCartItemsFile(boolean createDirectories, String url) {
        File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        file = new File(file, encodeURL(url));

        if (createDirectories == true) {
            if (!file.exists()) {
                file.mkdirs();
                checkDirectoryExists(file);
            }
        }

        return new File(file, CART_ITEMS_FILE_NAME);
    }

    public static void addCartItem(Map<String, Object> cartItem, String url)
    {
        Object[] cartItems = restoreCartItems(url);

        ArrayList<Object> cartItemsList = new ArrayList<Object>();

        cartItemsList.add(cartItem);

        if (cartItems != null)
        {
            for (int i = 0; i < cartItems.length; i++)
            {
                if (((Map<String, Object>) cartItems[i]).get(
                        MageventoryConstants.MAGEKEY_PRODUCT_TRANSACTION_ID)
                        .equals(cartItem.get(MageventoryConstants.MAGEKEY_PRODUCT_TRANSACTION_ID)))
                {
                    // The item is already in the cache. Just return.
                    return;
                }

                cartItemsList.add(cartItems[i]);
            }
        }

        storeCartItems(cartItemsList.toArray(new Object[0]), url);
    }

    public static void removeCartItem(String transactionID, String url)
    {
        Object[] cartItems = restoreCartItems(url);

        ArrayList<Object> cartItemsList = new ArrayList<Object>();

        if (cartItems != null)
        {
            for (int i = 0; i < cartItems.length; i++)
            {
                if (!((Map<String, Object>) cartItems[i]).get(
                        MageventoryConstants.MAGEKEY_PRODUCT_TRANSACTION_ID).equals(transactionID))
                {
                    cartItemsList.add(cartItems[i]);
                }
            }
        }

        storeCartItems(cartItemsList.toArray(new Object[0]), url);
    }

    public static void storeCartItems(Object[] cartItems, String url) {
        synchronized (sSynchronizationObject) {
            if (cartItems == null) {
                return;
            }
            serialize(cartItems, getCartItemsFile(true, url));
        }
    }

    public static Object[] restoreCartItems(String url) {
        synchronized (sSynchronizationObject) {
            return deserialize(Object[].class, getCartItemsFile(false, url));
        }
    }

    public static void removeCartItems(String url) {
        synchronized (sSynchronizationObject) {
            File f = getCartItemsFile(false, url);

            if (f.exists()) {
                f.delete();
            }
        }
    }

    public static boolean cartItemsExist(String url) {
        return getCartItemsFile(false, url).exists();
    }

    /* ======================================================================== */
    /* Last used custom attributes data */
    /* ======================================================================== */
    private static File getLastUsedCustomAttribsFile(boolean createDirectories, String url) {
        File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        file = new File(file, encodeURL(url));

        if (createDirectories == true) {
            if (!file.exists()) {
                file.mkdirs();
                checkDirectoryExists(file);
            }
        }

        return new File(file, LAST_USED_ATTRIBUTES_FILE_NAME);
    }

    public static void storeLastUsedCustomAttribs(CustomAttributesList list, String url) {
        synchronized (sSynchronizationObject) {
            if (list == null) {
                return;
            }
            serialize(list, getLastUsedCustomAttribsFile(true, url));
        }
    }

    public static CustomAttributesList restoreLastUsedCustomAttribs(String url) {
        synchronized (sSynchronizationObject) {
            return deserialize(CustomAttributesList.class, getLastUsedCustomAttribsFile(false, url));
        }
    }

    /* ======================================================================== */
    /* Duplication options */
    /* ======================================================================== */
    private static File getDuplicationOptionsFile(boolean createDirectories, String url) {
        File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        file = new File(file, encodeURL(url));

        if (createDirectories == true) {
            if (!file.exists()) {
                file.mkdirs();
                checkDirectoryExists(file);
            }
        }

        return new File(file, DUPLICATION_OPTIONS_FILE_NAME);
    }

    public static void storeDuplicationOptions(ProductDuplicationOptions duplicationOptions,
            String url) {
        synchronized (sSynchronizationObject) {
            if (duplicationOptions == null) {
                return;
            }
            serialize(duplicationOptions, getDuplicationOptionsFile(true, url));
        }
    }

    public static ProductDuplicationOptions restoreDuplicationOptions(String url) {
        synchronized (sSynchronizationObject) {
            return deserialize(ProductDuplicationOptions.class,
                    getDuplicationOptionsFile(false, url));
        }
    }

    /* ======================================================================== */
    /* Input cache */
    /* ======================================================================== */

    private static File getInputCacheFile(boolean createDirectories, String url) {
        File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
        file = new File(file, encodeURL(url));

        if (createDirectories == true) {
            if (!file.exists()) {
                file.mkdirs();
                checkDirectoryExists(file);
            }
        }

        return new File(file, INPUT_CACHE_FILE_NAME);
    }

    public static void storeInputCache(Map<String, List<String>> inputCache, String url) {
        synchronized (sSynchronizationObject) {
            if (inputCache == null) {
                return;
            }
            serialize(inputCache, getInputCacheFile(true, url));
        }
    }

    public static Map<String, List<String>> loadInputCache(String url) {
        synchronized (sSynchronizationObject) {
            return deserialize(
                    new TypeToken<Map<String, List<String>>>() {
                    }, getInputCacheFile(false, url));
        }
    }

    /* ======================================================================== */
    /* Deleting cache */
    /* ======================================================================== */
    private static void deleteCacheFiles(File dirOrFile)
    {
        if (dirOrFile.getName().equals(PRODUCT_LIST_DIR_NAME) ||
                dirOrFile.getName().equals(DOWNLOAD_IMAGE_PREVIEW_DIR_NAME) ||
                dirOrFile.getName().equals(DOWNLOAD_IMAGE_DIR) ||
                dirOrFile.getName().equals(ORDER_DETAILS_DIR_NAME) ||
                dirOrFile.getName().equals(ORDER_LIST_DIR_NAME) ||
                dirOrFile.getName().equals(ATTRIBUTE_LIST_DIR_NAME) ||
                dirOrFile.getName().equals(PROFILE_EXECUTION_DIR_NAME))
        {
            deleteRecursive(dirOrFile);
        }
        else if (dirOrFile.getName().equals(PRODUCT_DETAILS_FILE_NAME) ||
                dirOrFile.getName().equals(ATTRIBUTE_SETS_FILE_NAME) ||
                dirOrFile.getName().equals(CATEGORIES_LIST_FILE_NAME) ||
                dirOrFile.getName().equals(INPUT_CACHE_FILE_NAME) ||
                dirOrFile.getName().equals(LAST_USED_ATTRIBUTES_FILE_NAME) ||
                dirOrFile.getName().equals(STATISTICS_FILE_NAME) ||
                dirOrFile.getName().equals(ORDER_CARRIERS_FILE_NAME) ||
                dirOrFile.getName().equals(PROFILES_FILE_NAME) ||
                dirOrFile.getName().equals(CART_ITEMS_FILE_NAME) ||
                dirOrFile.getName().equals(DUPLICATION_OPTIONS_FILE_NAME))
        {
            dirOrFile.delete();
        }
        else if (dirOrFile.isDirectory())
        {
            for (File child : dirOrFile.listFiles())
                deleteCacheFiles(child);
        }
    }

    public static void deleteCache(String url)
    {
        synchronized (sSynchronizationObject) {
            synchronized (sProductDetailsLock) {
                String encodedUrl = encodeURL(url);

                File file = new File(Environment.getExternalStorageDirectory(),
                        MyApplication.APP_DIR_NAME);
                file = new File(file, encodedUrl);

                if (file.exists())
                    deleteCacheFiles(file);

                killRAMCachedProductDetails();
                sRamCache.remove(url);
                ProductAliasCacheManager.getInstance().deleteProductsFromCache(url);
            }
        }
    }

    public static void deleteAllCaches()
    {
        synchronized (sSynchronizationObject) {
            synchronized (sProductDetailsLock) {
                File file = new File(Environment.getExternalStorageDirectory(),
                        MyApplication.APP_DIR_NAME);

                if (file.exists())
                    deleteCacheFiles(file);

                killRAMCachedProductDetails();
                sRamCache.clear();
            }
        }
    }

    /*
     * If this returns false it means there is a job being executed so we cannot
     * wipe the data. Otherwise it's a success.
     */
    public static boolean wipeData(Context context)
    {
        synchronized (JobQueue.sQueueSynchronizationObject)
        {
            if (JobQueue.getCurrentJob() == null)
            {
                new JobQueue(context).wipeTables();

                synchronized (sSynchronizationObject)
                {
                    synchronized (sProductDetailsLock) {
                        File file = new File(Environment.getExternalStorageDirectory(),
                                MyApplication.APP_DIR_NAME);

                        if (file.exists())
                            deleteRecursive(file);
                        sRamCache.clear();
                    }
                }
                killRAMCachedProductDetails();
                ProductAliasCacheManager.getInstance().wipeTable();
                RecentWebAddressProviderAccessor.getInstance().deleteAllRecentWebAddresses();
                Settings settings = new Settings(context);
                settings.clearCameraTimeDifferenceInformation();
                settings.cleartDisplayZXingInstallRequest();
                settings.clearIssnMissingMetadataRescanRequestEnabled();
            }
            else
            {
                return false;
            }

            return true;
        }
    }

    /* Delete all files recursively from a given directory. */
    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    /**
     * Casts object to Object[] or convert List to Object[]
     * 
     * @param object
     * @return
     */
    public static Object[] getObjectArrayFromDeserializedItem(Object object)
    {
        if (object == null)
        {
            return null;
        } else if (object instanceof Object[])
        {
            return (Object[]) object;
        } else
        {
            List<Object> list = (List<Object>) object;
            return list.toArray();
        }
    }

    /**
     * Get the object array data from the API response with support of arrays
     * which are returned as Map with some keys
     * 
     * @param object to check data type and convert to Object[] array
     * @return data converted to Object[] array if required
     */
    @SuppressWarnings("unchecked")
    public static Object[] getObjectArrayWithMapCompatibility(Object object) {
        final Object[] result;
        if (object != null && object instanceof Map) {
            Map<?, Object> itemsMap = (Map<?, Object>) object;
            result = itemsMap.values().toArray();
        } else {
            result = getObjectArrayFromDeserializedItem(object);
        }
        return result;
    }

    /**
     * Casts object to String[] or convert List to String[]
     * 
     * @param object
     * @return
     */
    public static String[] getStringArrayFromDeserializedItem(Object object)
    {
        if (object == null)
        {
            return null;
        } else if (object instanceof String[])
        {
            return (String[]) object;
        } else
        {
            List<String> list = (List<String>) object;
            String[] result = new String[list.size()];
            result = list.toArray(result);
            return result;
        }
    }

    /**
     * Get int value from the number object
     * 
     * @param value
     * @return
     */
    public static int getIntValue(Object value)
    {
        return ((Number) value).intValue();
    }

    /**
     * Check o type and parse int safely. Returns 0 in case parse failed
     * 
     * @param o
     * @return
     */
    public static int safeParseInt(Object o) {
        return safeParseInt(o, 0);
    }

    /**
     * Check o type and parse int safely
     * 
     * @param o
     * @param defaultValue default value which will be returned in case parse
     *            failed
     * @return
     */
    public static int safeParseInt(Object o, int defaultValue) {
        if (o != null) {
            if (o instanceof String) {
                final String s = (String) o;
                Number value = CommonUtils.parseNumber(s);
                return value == null ? defaultValue : value.intValue();
            } else if (o instanceof Number) {
                return ((Number) o).intValue();
            } else if (o instanceof Boolean) {
                return ((Boolean) o) ? 1 : 0;
            }
        }
        return defaultValue;
    }

    /**
     * Get float value from the number object
     * 
     * @param value
     * @return
     */
    public static float getFloatValue(Object value)
    {
        return ((Number) value).floatValue();
    }

    /**
     * Clone map
     * 
     * @param map
     * @return
     */
    public static <P, C> Map<P, C> cloneMap(Map<P, C> map)
    {
        if (map == null)
        {
            return null;
        } else if (map instanceof HashMap)
        {
            return (Map<P, C>) ((HashMap<P, C>) map).clone();
        } else
        {
            return new HashMap<P, C>(map);
        }
    }

    /**
     * Get the ram cache for the specified path
     * 
     * @param forceCreatePathItems whether to create missing path items
     *            automatically
     * @param ramCachePath the hierarchical path in the cache
     * @return
     */
    @SuppressWarnings("unchecked")
    private static <T> T getRamCache(boolean forceCreatePathItems, String... ramCachePath) {
        Object lastObject = sRamCache;
        for (String pathItem : ramCachePath) {
            Map<String, Object> lastMap = ((Map<String, Object>) lastObject);
            lastObject = lastMap.get(pathItem);
            if (lastObject == null) {
                if (forceCreatePathItems) {
                    lastObject = new HashMap<String, Object>();
                    lastMap.put(pathItem, lastObject);
                } else {
                    break;
                }
            }
        }
        return (T) lastObject;
    }

    /**
     * Put the value to the RAM cache under some path
     * 
     * @param value the value to put into cache under path
     * @param ramCachePath the hierarchical path in the cache
     */
    private static void putToRamCache(Object value, String... ramCachePath) {
        // get the parent cache object for the last path item
        Map<String, Object> pathRamCache = getRamCache(value != null,
                Arrays.copyOf(ramCachePath, ramCachePath.length - 1));
        if (pathRamCache != null) {
            pathRamCache.put(ramCachePath[ramCachePath.length - 1], value);
        }
    }

    /**
     * General method to store the data to disk and RAM cache at the time
     * 
     * @param data data to same
     * @param diskCacheFile the path to the disk cache file
     * @param ramCachePath the hierarchical path in the cache
     */
    public static void storeData(Object data, File diskCacheFile, String... ramCachePath) {
        synchronized (sSynchronizationObject) {
            if (data == null) {
                return;
            }
            long start = System.currentTimeMillis();
            serialize(data, diskCacheFile);
            putToRamCache(data, ramCachePath);
            TrackerUtils.trackDataLoadTiming(
                    System.currentTimeMillis() - start,
                    CommonUtils.format("storeData for file: %1$s, ramCachePath %2$s",
                            diskCacheFile.getAbsolutePath(), TextUtils.join("/", ramCachePath)),
                    TAG);
        }
    }

    /**
     * Check whether the cached data exists either in RAM or disk cache
     * 
     * @param diskCacheFile the disk cache file name
     * @param ramCachePath the hierarchical path in the cache
     * @return
     */
    public static boolean dataExists(File diskCacheFile, String... ramCachePath) {
        synchronized (sSynchronizationObject) {
            long start = System.currentTimeMillis();
            boolean result = getRamCache(false, ramCachePath) != null || diskCacheFile.exists();
            TrackerUtils.trackDataLoadTiming(
                    System.currentTimeMillis() - start,
                    CommonUtils.format("dataExists for file: %1$s, ramCachePath %2$s",
                            diskCacheFile.getAbsolutePath(), TextUtils.join("/", ramCachePath)), TAG);
            return result;
        }
    }

    /**
     * Restore the data from the RAM or disk cache
     * 
     * @param type the type token parameter used for gson deserialization
     * @param diskCacheFile the disk cache file name
     * @param ramCachePath the hierarchical path in the cache
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T restoreData(TypeToken<T> type, File diskCacheFile, String... ramCachePath) {
        synchronized (sSynchronizationObject) {
            long start = System.currentTimeMillis();
            Map<String, Object> ramCache = getRamCache(true,
                    Arrays.copyOf(ramCachePath, ramCachePath.length - 1));
            String lastKey = ramCachePath[ramCachePath.length - 1];
            Object result = ramCache.get(lastKey);
            if (result == null) {
                result = deserialize(type, diskCacheFile);
                ramCache.put(lastKey, result);
            } else {
                TrackerUtils
                        .trackDataLoadTiming(System.currentTimeMillis() - start, CommonUtils
                                .format("restoreData for file: %1$s, ramCachePath %2$s",
                                        diskCacheFile.getAbsolutePath(),
                                        TextUtils.join("/", ramCachePath)), TAG);
            }
            return (T) result;
        }
    }
}
