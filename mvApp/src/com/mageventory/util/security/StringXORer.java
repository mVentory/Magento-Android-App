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
package com.mageventory.util.security;

import android.util.Base64;

/**
 * Basic string encoding/decoding class via XOR
 */
public class StringXORer {
    /**
     * Encode the string with the key using XOR anb Base64 algorithm
     * 
     * @param s string to encode
     * @param key the encoding key
     * @return Base64 encoded XORed with the key string
     */
    public static String encode(String s, String key) {
        return base64Encode(xorWithKey(s.getBytes(), key.getBytes()));
    }

    /**
     * Decode the string with the key using XOR anb Base64 algorithm
     * 
     * @param s string to decode
     * @param key the decoding key
     * @return Base64 decoded XORed with the key string
     */
    public static String decode(String s, String key) {
        return new String(xorWithKey(base64Decode(s), key.getBytes()));
    }

    /**
     * XOR array with the key
     * 
     * @param a the array to XOR
     * @param key the key to XOR with
     * @return
     */
    private static byte[] xorWithKey(byte[] a, byte[] key) {
        byte[] out = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = (byte) (a[i] ^ key[i % key.length]);
        }
        return out;
    }

    /**
     * Decode Base64 encoded string
     * 
     * @param s the string to decode
     * @return decoded string
     */
    private static byte[] base64Decode(String s) {
        return Base64.decode(s, Base64.DEFAULT);
    }

    /**
     * Encode string with the Base64 algorithm
     * 
     * @param s the string to encode
     * @return encoded string
     */
    private static String base64Encode(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.DEFAULT).replaceAll("\\s", "");

    }
}
