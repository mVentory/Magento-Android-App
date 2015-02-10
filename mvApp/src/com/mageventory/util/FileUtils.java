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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.webkit.MimeTypeMap;

import com.mventory.R;

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
        String fileName = file.getName();
        String extension = getExtension(fileName);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension.toLowerCase());
        }
        CommonUtils.debug(TAG, "File: %1$s; extension %2$s; MimeType: %3$s",
                file.getAbsolutePath(), extension, type);
        return type;
    }

    /**
     * Get the extension for the file name
     * 
     * @param fileName
     * @return
     */
    public static String getExtension(String fileName) {
        int p = fileName.lastIndexOf('.');
        String extension = p == -1 ? null : fileName.substring(p + 1);
        return extension;
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
                CommonUtils.formatNumberWithFractionWithRoundUp1(floatSize));
    }
}
