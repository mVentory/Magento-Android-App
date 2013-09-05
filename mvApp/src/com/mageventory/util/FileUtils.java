
package com.mageventory.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.mageventory.R;

/**
 * Various utilities for the files
 * 
 * @author Eugene Popovich
 */
public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();
    private static final long KB = 1024l;
    private static final long MB = KB * KB;

    /**
     ** Get the mime type for the file
     * 
     * @param file
     * @return
     */
    public static String getMimeType(File file) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).getPath());
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension.toLowerCase());
        }
        CommonUtils.debug(TAG, "File: %1$s; extension %2$s; MimeType: %3$s",
                file.getAbsolutePath(), extension, type);
        return type;
    }

    /**
     * Copy file from src to dst location
     * 
     * @param src
     * @param dst
     * @throws IOException
     */
    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    /**
     * Format the file size to the human readable string
     * 
     * @param size
     * @return
     */
    public static String formatFileSize(long size) {
        int stringResourceId;
        float floatSize = size;
        if (floatSize < MB) {
            floatSize = floatSize / KB;
            stringResourceId = R.string.size_KB;
        } else {
            floatSize = floatSize / MB;
            stringResourceId = R.string.size_MB;
        }
        return CommonUtils.getStringResource(stringResourceId,
                CommonUtils.formatNumberWithFractionWithRoundUp(floatSize));
    }
}
