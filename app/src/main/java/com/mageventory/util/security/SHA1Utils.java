package com.mageventory.util.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilities to work with the SHA1 hash<br/>
 * Source http://stackoverflow.com/a/5980789/527759
 */
public class SHA1Utils {
    /**
     * Convert byte data to hex
     * 
     * @param data
     * @return
     */
    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte)
                        : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * Generate SHA-1 hash for the specified text
     * 
     * @param text the string to generate SHA-1 hash for
     * @return the text SHA-1 hash
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String sha1(String text) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] bytes = text.getBytes("iso-8859-1");
        md.update(bytes, 0, bytes.length);
        byte[] sha1hash = md.digest();
        return convertToHex(sha1hash);
    }
}
