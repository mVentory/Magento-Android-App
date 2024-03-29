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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.mventory.BuildConfig;
import com.mventory.R;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.ImageUtils;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.TrackerUtils;

/**
 * A simple subclass of {@link ImageResizer} that fetches and resizes images
 * fetched from a URL.
 */
public class ImageFetcher extends ImageResizer {
    private static final String TAG = "ImageFetcher";
    /**
     * The connection timeout in millis for the photo download operation
     */
    private static final int CONNECTION_TIMEOUT_MILLIS = 10000; // 10 seconds
    private static final int HTTP_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final String HTTP_CACHE_DIR = "http";
    public static ThreadLocal<File> sLastFile = new ThreadLocal<File>();

    /**
     * Initialize providing a target image width and height for the processing
     * images.
     * 
     * @param context
     * @param loadingControl
     * @param imageWidth
     * @param imageHeight
     */
    public ImageFetcher(Context context, LoadingControl loadingControl, int imageWidth,
            int imageHeight) {
        super(context, loadingControl, imageWidth, imageHeight);
        init(context);
    }

    /**
     * Initialize providing a single target image size (used for both width and
     * height);
     * 
     * @param context
     * @param loadingControl
     * @param imageSize
     */
    public ImageFetcher(Context context, LoadingControl loadingControl, int imageSize) {
        super(context, loadingControl, imageSize);
        init(context);
    }

    private void init(Context context) {
        checkConnection(context);
    }

    /**
     * Simple network connection check.
     * 
     * @param context
     */
    private void checkConnection(Context context) {
        CommonUtils.checkOnline();
    }

    /**
     * The main process method, which will be called by the ImageWorker in the
     * AsyncTaskEx background thread.
     * 
     * @param data The data to load the bitmap, in this case, a regular http URL
     * @param processingState may be used to determine whether the processing is
     *            cancelled during long operations
     * @return The downloaded and resized bitmap
     */
    private Bitmap processBitmap(String data, ProcessingState processingState) {
        return processBitmap(data, imageWidth, imageHeight, processingState);
    }

    /**
     * The main process method, which will be called by the ImageWorker in the
     * AsyncTaskEx background thread.
     * 
     * @param data The data to load the bitmap, in this case, a regular http URL
     * @param imageWidth
     * @param imageHeight
     * @param processingState may be used to determine whether the processing is
     *            cancelled during long operations
     * @return The downloaded and resized bitmap
     */
    protected Bitmap processBitmap(String data, int imageWidth, int imageHeight,
            ProcessingState processingState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "processBitmap - " + data);
        }

        // Download a bitmap, write it to a file
        final File f = downloadBitmap(mContext, data, processingState);
        sLastFile.set(f);
        if (f != null) {
            try {
                // Return a sampled down version
                return ImageUtils
                        .decodeSampledBitmapFromFile(f.toString(), imageWidth, imageHeight);
            } catch (Exception ex) {
                GuiUtils.noAlertError(TAG, ex);
            }
        }

        return null;
    }

    @Override
    protected Bitmap processBitmap(Object data, ProcessingState processingState) {
        return processBitmap(String.valueOf(data), processingState);
    }

    /**
     * Download a bitmap from a URL, write it to a disk and return the File
     * pointer. This implementation uses a simple disk cache.
     * 
     * @param context The context to use
     * @param urlString The URL to fetch
     * @param processingState may be used to determine whether the processing is
     *            cancelled during long operations
     * @return A File pointing to the fetched bitmap
     */
    public static File downloadBitmap(Context context, String urlString,
            ProcessingState processingState) {
        final File cacheDir = DiskLruCache.getDiskCacheDir(context, HTTP_CACHE_DIR);

        if (urlString == null) {
            return null;
        }
        DiskLruCache cache = DiskLruCache.openCache(context, cacheDir, HTTP_CACHE_SIZE);
        // #273 additional checks
        if (cache == null) {
            CommonUtils.debug(TAG, "Failed to open http cache %1$s", cacheDir.getAbsolutePath());
            TrackerUtils.trackBackgroundEvent("httpCacheOpenFail", cacheDir.getAbsolutePath());
            // cache open may fail if there are not enough free space.
            // application will try to clear that cache dir and open cache again
            DiskLruCache.clearCache(context, HTTP_CACHE_DIR);

            // cache clear attempt finished. Let's try again to open cache
            cache = DiskLruCache.openCache(context, cacheDir, HTTP_CACHE_SIZE);
            if (cache == null) {
                CommonUtils.debug(TAG, "Failed to open http cache second time %1$s",
                        cacheDir.getAbsolutePath());
                // still unsuccessful. We can't download that bitmap. Let's warn
                // user about this.
                GuiUtils.alert(R.string.errorCouldNotStoreDownloadablePhotoNotEnoughSpace);
                TrackerUtils.trackBackgroundEvent("httpCacheSecondOpenFail",
                        cacheDir.getAbsolutePath());
                return null;
            }
        }
        if (processingState != null && processingState.isProcessingCancelled()) {
            return null;
        }
        final File cacheFile = new File(cache.createFilePath(urlString));

        if (cache.containsKey(urlString)) {
            TrackerUtils.trackBackgroundEvent("httpCachHit", TAG);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "downloadBitmap - found in http cache - " + urlString);
            }
            return cacheFile;
        }
        if (!CommonUtils.checkLoggedInAndOnline(true)) {
            return null;
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "downloadBitmap - downloading - " + urlString);
        }

        File tempFile = null;
        boolean success = false;
        try {
            tempFile = File.createTempFile(
                    DiskLruCache.CACHE_FILENAME_PREFIX + "udl", null,
                    cache.getCacheDir());
            if (downloadBitmap(urlString, tempFile, processingState)) {
                if (!cacheFile.exists()) {
                    CommonUtils
                            .debug(TAG,
                                    "downloadBitmap: cache file %1$s doesn't exist, renaming downloaded data",
                                    cacheFile.getAbsolutePath());
                    if (!tempFile.renameTo(cacheFile)) {
                        return null;
                    }
                } else {
                    CommonUtils.debug(TAG,
                            "downloadBitmap: cache file %1$s exists, removing downloaded data",
                            cacheFile.getAbsolutePath());
                    tempFile.delete();
                }
                success = true;
                return cacheFile;
            } else {
                tempFile.delete();
                success = true;
                return null;
            }

        } catch (final IOException e) {
            GuiUtils.noAlertError(TAG, "Error in downloadBitmap", e);
        } finally {
            if (!success && tempFile != null) {
                try {
                    tempFile.delete();
                } catch (Exception ex) {
                    CommonUtils.error(TAG, ex);
                }
            }
        }

        return null;
    }

    public static boolean downloadBitmap(String urlString, File file,
            ProcessingState processingState) {
        BitmapfunUtils.disableConnectionReuseIfNecessary();
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        try {
            long start = System.currentTimeMillis();
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
            final InputStream in = new BufferedInputStream(urlConnection.getInputStream(),
                    BitmapfunUtils.IO_BUFFER_SIZE);
            out = new BufferedOutputStream(new FileOutputStream(file),
                    BitmapfunUtils.IO_BUFFER_SIZE);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                if (processingState != null && processingState.isProcessingCancelled()) {
                    CommonUtils.debug(TAG,
                            "downloadBitmap: processing is cancelled. Removing temp file %1$s",
                            file.getAbsolutePath());
                    out.close();
                    out = null;
                    file.delete();
                    return false;
                }
            }
            TrackerUtils.trackDataLoadTiming(System.currentTimeMillis() - start, "downloadBitmap",
                    TAG);
            return true;
        } catch (final IOException e) {
            GuiUtils.noAlertError(TAG, "Error in downloadBitmap", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException e) {
                    GuiUtils.noAlertError(TAG, "Error in downloadBitmap", e);
                }
            }
        }
        return false;
    }
}
