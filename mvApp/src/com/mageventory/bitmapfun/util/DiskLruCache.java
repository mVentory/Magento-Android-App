/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mageventory.bitmapfun.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.mventory.BuildConfig;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.TrackerUtils;
import com.mageventory.util.security.SHA1Utils;

/**
 * A simple disk LRU bitmap cache to illustrate how a disk cache would be used
 * for bitmap caching. A much more robust and efficient disk LRU cache solution
 * can be found in the ICS source code
 * (libcore/luni/src/main/java/libcore/io/DiskLruCache.java) and is preferable
 * to this simple implementation.
 */
public class DiskLruCache {
    private static final String TAG = "DiskLruCache";
    public static final String CACHE_FILENAME_PREFIX = "cache_";
    private static final int MAX_REMOVALS = 4;
    private static final int INITIAL_CAPACITY = 32;
    private static final float LOAD_FACTOR = 0.75f;
    private static final int DEFAULT_MAX_CACHE_ITEM_SIZE = 64;
    /**
     * Maximum file name length in the android file system. It is 256 bytes but
     * string character may take 2 bytes.
     */
    private static final int MAX_FILE_NAME_LENGTH = 127;
    /**
     * Maximum path length in the android file system from various sources is
     * from 1024 up to 4096 bytes. To be sure we use minimum possible.
     */
    private static final int MAX_PATH_LENGTH = 512;

    private final File mCacheDir;
    private int cacheSize = 0;
    private int cacheByteSize = 0;
    private int maxCacheItemSize = DEFAULT_MAX_CACHE_ITEM_SIZE; // 64 item
                                                                // default
    private long maxCacheByteSize = 1024 * 1024 * 5; // 5MB default
    private CompressFormat mCompressFormat = CompressFormat.JPEG;
    private int mCompressQuality = 70;

    private final Map<String, String> mLinkedHashMap = Collections
            .synchronizedMap(new LinkedHashMap<String, String>(INITIAL_CAPACITY, LOAD_FACTOR, true));

    /**
     * A filename filter to use to identify the cache filenames which have
     * CACHE_FILENAME_PREFIX prepended.
     */
    private static final FilenameFilter cacheFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return filename.startsWith(CACHE_FILENAME_PREFIX);
        }
    };

    /**
     * Used to fetch an instance of DiskLruCache.
     * 
     * @param context
     * @param cacheDir
     * @param maxByteSize
     * @return
     */
    public static DiskLruCache openCache(Context context, File cacheDir, long maxByteSize) {
        return openCache(context, cacheDir, maxByteSize, DEFAULT_MAX_CACHE_ITEM_SIZE);
    }

    /**
     * Used to fetch an instance of DiskLruCache.
     * 
     * @param context
     * @param cacheDir
     * @param maxByteSize
     * @param maxItemSize
     * @return
     */
    public static DiskLruCache openCache(Context context, File cacheDir, long maxByteSize,
            int maxItemSize) {
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }

        if (cacheDir.isDirectory() && cacheDir.canWrite()
                && BitmapfunUtils.getUsableSpace(cacheDir) > maxByteSize) {
            return new DiskLruCache(cacheDir, maxByteSize, maxItemSize);
        }
        CommonUtils.debug(TAG, "Couldn't open disk cache");
        TrackerUtils
                .trackBackgroundEvent(
                        "unsuccessfullDiskCacheCreationForParameters",
                        CommonUtils
                                .format("path: %1$s;isDirectory: %2$b; canWrite: %3$b; usableSpace: %4$d; maxByteSize: %5$d",
                                        cacheDir.getAbsolutePath(), cacheDir.canWrite(),
                                        cacheDir.isDirectory(),
                                        BitmapfunUtils.getUsableSpace(cacheDir), maxByteSize));
        return null;
    }

    /**
     * Constructor that should not be called directly, instead use
     * {@link DiskLruCache#openCache(Context, File, long)} which runs some extra
     * checks before creating a DiskLruCache instance.
     * 
     * @param cacheDir
     * @param maxByteSize
     */
    private DiskLruCache(File cacheDir, long maxByteSize) {
        this(cacheDir, maxByteSize, DEFAULT_MAX_CACHE_ITEM_SIZE);
    }

    /**
     * Constructor that should not be called directly, instead use
     * {@link DiskLruCache#openCache(Context, File, long)} which runs some extra
     * checks before creating a DiskLruCache instance.
     * 
     * @param cacheDir
     * @param maxByteSize
     */
    private DiskLruCache(File cacheDir, long maxByteSize, int maxItemSize) {
        mCacheDir = cacheDir;
        maxCacheByteSize = maxByteSize;
        maxCacheItemSize = maxItemSize;
    }

    /**
     * Add a bitmap to the disk cache.
     * 
     * @param key A unique identifier for the bitmap.
     * @param data The bitmap to store.
     */
    public void put(String key, Bitmap data) {
        synchronized (mLinkedHashMap) {
            if (mLinkedHashMap.get(key) == null) {
                try {
                    final String file = createFilePath(mCacheDir, key);
                    if (writeBitmapToFile(data, file)) {
                        put(key, file);
                        flushCache();
                    }
                } catch (final FileNotFoundException e) {
                    GuiUtils.noAlertError(TAG, "Error in put", e);
                } catch (final IOException e) {
                    GuiUtils.noAlertError(TAG, "Error in put", e);
                }
            }
        }
    }

    private void put(String key, String file) {
        mLinkedHashMap.put(key, file);
        cacheSize = mLinkedHashMap.size();
        cacheByteSize += new File(file).length();
    }

    /**
     * Flush the cache, removing oldest entries if the total size is over the
     * specified cache size. Note that this isn't keeping track of stale files
     * in the cache directory that aren't in the HashMap. If the images and keys
     * in the disk cache change often then they probably won't ever be removed.
     */
    private void flushCache() {
        Entry<String, String> eldestEntry;
        File eldestFile;
        long eldestFileSize;
        int count = 0;

        while (count < MAX_REMOVALS
                && (cacheSize > maxCacheItemSize || cacheByteSize > maxCacheByteSize)) {
            eldestEntry = mLinkedHashMap.entrySet().iterator().next();
            eldestFile = new File(eldestEntry.getValue());
            eldestFileSize = eldestFile.length();
            mLinkedHashMap.remove(eldestEntry.getKey());
            eldestFile.delete();
            cacheSize = mLinkedHashMap.size();
            cacheByteSize -= eldestFileSize;
            count++;
            if (BuildConfig.DEBUG) {
                CommonUtils.debug(TAG, "flushCache - Removed cache file, " + eldestFile + ", "
                        + eldestFileSize);
            }
        }
    }

    /**
     * Get an image from the disk cache.
     * 
     * @param key The unique identifier for the bitmap
     * @return The bitmap or null if not found
     */
    public Bitmap get(String key) {
        synchronized (mLinkedHashMap) {
            final String file = mLinkedHashMap.get(key);
            if (file != null) {
                if (BuildConfig.DEBUG) {
                    CommonUtils.debug(TAG, "Disk cache hit");
                }
                TrackerUtils.trackBackgroundEvent("diskCacheHit", TAG);
                return getBitmap(file);
            } else {
                final String existingFile = createFilePath(mCacheDir, key);
                if (new File(existingFile).exists()) {
                    put(key, existingFile);
                    if (BuildConfig.DEBUG) {
                        CommonUtils.debug(TAG, "Disk cache hit (existing file)");
                    }
                    TrackerUtils.trackBackgroundEvent("diskCacheHitExistingFile", TAG);
                    return getBitmap(existingFile);
                }
            }
            return null;
        }
    }

    /**
     * @param path
     * @return
     */
    Bitmap getBitmap(String path) {
        Bitmap bm = null;
        BitmapFactory.Options bfOptions = new BitmapFactory.Options();
        bfOptions.inDither = false; // Disable Dithering mode
        bfOptions.inPurgeable = true; // Tell to gc that whether it needs
                                      // free memory, the Bitmap can be
                                      // cleared
        bfOptions.inInputShareable = true; // Which kind of reference will
                                           // be used to recover the Bitmap
                                           // data after being clear, when
                                           // it will be used in the future
        bfOptions.inTempStorage = new byte[32 * 1024];

        bm = ImageUtils.decodeBitmap(path, bfOptions);
        return bm;
    }

    /**
     * Checks if a specific key exist in the cache.
     * 
     * @param key The unique identifier for the bitmap
     * @return true if found, false otherwise
     */
    public boolean containsKey(String key) {
        // See if the key is in our HashMap
        if (mLinkedHashMap.containsKey(key)) {
            return true;
        }

        // Now check if there's an actual file that exists based on the key
        final String existingFile = createFilePath(mCacheDir, key);
        if (new File(existingFile).exists()) {
            // File found, add it to the HashMap for future use
            put(key, existingFile);
            return true;
        }
        return false;
    }

    /**
     * Removes all disk cache entries from this instance cache dir
     */
    public void clearCache() {
        CommonUtils.debug(TAG, "Disk cache clear request");
        DiskLruCache.clearCache(mCacheDir);
        synchronized (mLinkedHashMap) {
            mLinkedHashMap.clear();
        }
    }

    /**
     * Removes all disk cache entries from the application cache directory in
     * the uniqueName sub-directory.
     * 
     * @param context The context to use
     * @param uniqueName A unique cache directory name to append to the app
     *            cache directory
     */
    public static void clearCache(Context context, String uniqueName) {
        File cacheDir = getDiskCacheDir(context, uniqueName);
        clearCache(cacheDir);
    }

    /**
     * Removes all disk cache entries from the application cache directory in
     * all the uniqueNames sub-directories.
     * 
     * @param context The context to use
     * @param uniqueNames An array of unique cache directory names to append to
     *            the app cache directory
     */
    public static void clearCaches(Context context, String... uniqueNames) {
        for (String uniqueName : uniqueNames) {
            clearCache(context, uniqueName);
        }
    }

    /**
     * Removes all disk cache entries from the given directory. This should not
     * be called directly, call {@link DiskLruCache#clearCache(Context, String)}
     * or {@link DiskLruCache#clearCache()} instead.
     * 
     * @param cacheDir The directory to remove the cache files from
     */
    private static void clearCache(File cacheDir) {
        TrackerUtils.trackBackgroundEvent(
                "clearCacheRequest",
                CommonUtils.format("path: %1$s; exists: %2$b; canWrite: %3$b",
                        cacheDir.getAbsolutePath(), cacheDir.exists(), cacheDir.canWrite()));
        CommonUtils.debug(TAG, "Clear cache request: path: %1$s; exists: %2$b; canWrite: %3$b",
                cacheDir.getAbsolutePath(), cacheDir.exists(), cacheDir.canWrite());
        // Issue #264 additional checks
        if (!cacheDir.exists() || !cacheDir.canWrite()) {
            TrackerUtils.trackBackgroundEvent("clearCacheInterrupt", cacheDir.getAbsolutePath());
            CommonUtils.debug(TAG,
                    "Cache dir doesn't exists or can't write: " + cacheDir.getAbsolutePath());
            return;
        }
        final File[] files = cacheDir.listFiles(cacheFileFilter);
        // Issue #264 additional checks
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                files[i].delete();
            }
        }
    }

    /**
     * Get a usable cache directory (external if available, internal otherwise).
     * 
     * @param context The context to use
     * @param uniqueName A unique directory name to append to the cache dir
     * @return The cache dir
     */
    public static File getDiskCacheDir(Context context, String uniqueName) {

        // Check if media is mounted or storage is built-in, if so, try and use
        // external cache dir
        // otherwise use internal cache dir
        File cacheDir = null;
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
                || !BitmapfunUtils.isExternalStorageRemovable()) {
            cacheDir = BitmapfunUtils.getExternalCacheDir(context);
            if (cacheDir == null) {
                cacheDir = context.getCacheDir();
            } else {
                if (!cacheDir.exists()) {
                    cacheDir.mkdir();
                }
                if (!cacheDir.canWrite()) {
                    CommonUtils
                            .debug(TAG,
                                    true,
                                    "getDiskCacheDir: External cache dir %1$s is not writable. Using default one",
                                    cacheDir.getAbsolutePath());
                    TrackerUtils.trackBackgroundEvent("notWritableDiskCacheDirectory",
                            cacheDir.getAbsolutePath());
                    cacheDir = context.getCacheDir();
                }
            }
        } else {
            cacheDir = context.getCacheDir();
            CommonUtils
                    .debug(TAG,
                            true,
                            "getDiskCacheDir: invalid state or storage is removable. State: %1$s; Removable: %2$b",
                            Environment.getExternalStorageState(),
                            BitmapfunUtils.isExternalStorageRemovable());
        }
        String cachePath = cacheDir.getPath();
        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * Creates a constant cache file path given a target cache directory and an
     * image key.
     * 
     * @param cacheDir
     * @param key
     * @return
     */
    public static String createFilePath(File cacheDir, String key) {
        try {
            // Use URLEncoder to ensure we have a valid filename, a tad hacky
            // but it will do for
            // this example
            String dirName = cacheDir.getAbsolutePath() + File.separator;
            String cacheFileName = CACHE_FILENAME_PREFIX
                    + URLEncoder.encode(key.replace("*", ""), "UTF-8");
            // maximum allowed length for the path
            int pathRest = MAX_PATH_LENGTH - dirName.length();
            // maximum allowed file name length
            int maxAllowedFileNameLength = Math.min(pathRest, MAX_FILE_NAME_LENGTH);
            if (cacheFileName.length() > maxAllowedFileNameLength) {
                // if file name length exceeds max length limits
                CommonUtils.debug(TAG,
                                "createFilePath: file name %1$s:%2$d extends system path/name length limits. Trimming...",
                        cacheFileName, cacheFileName.length());
                // generate hash for the file name
                String nameHash = SHA1Utils.sha1(cacheFileName);
                String hashSeparator = "_";
                // the length of the substring which may be kept from the
                // original file name
                int prefixLength = maxAllowedFileNameLength - nameHash.length()
                        - hashSeparator.length();
                if (prefixLength > 0) {
                    // if some substring from the original file name may be kept
                    // in addition to the generated hash
                    cacheFileName = cacheFileName.substring(0, prefixLength)
                            + hashSeparator.length() + nameHash;
                } else {
                    // replace original file name with the hash
                    cacheFileName = nameHash;
                }
                CommonUtils.debug(TAG, "createFilePath: resulting file name %1$s:%2$d",
                        cacheFileName, cacheFileName.length());
            }
            return dirName + cacheFileName;
        } catch (final NoSuchAlgorithmException e) {
            CommonUtils.error(TAG, "createFilePath", e);
        } catch (final UnsupportedEncodingException e) {
            CommonUtils.error(TAG, "createFilePath", e);
        }

        return null;
    }

    /**
     * Create a constant cache file path using the current cache directory and
     * an image key.
     * 
     * @param key
     * @return
     */
    public String createFilePath(String key) {
        return createFilePath(mCacheDir, key);
    }

    public File getCacheDir() {
        return mCacheDir;
    }

    /**
     * Sets the target compression format and quality for images written to the
     * disk cache.
     * 
     * @param compressFormat
     * @param quality
     */
    public void setCompressParams(CompressFormat compressFormat, int quality) {
        mCompressFormat = compressFormat;
        mCompressQuality = quality;
    }

    /**
     * Writes a bitmap to a file. Call
     * {@link DiskLruCache#setCompressParams(CompressFormat, int)} first to set
     * the target bitmap compression and format.
     * 
     * @param bitmap
     * @param file
     * @return
     */
    private boolean writeBitmapToFile(Bitmap bitmap, String file) throws IOException,
            FileNotFoundException {

        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file),
                    BitmapfunUtils.IO_BUFFER_SIZE);
            return bitmap.compress(mCompressFormat, mCompressQuality, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
