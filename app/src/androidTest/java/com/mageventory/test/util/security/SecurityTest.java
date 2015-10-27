package com.mageventory.test.util.security;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.test.InstrumentationTestCase;
import android.util.Log;

import com.mageventory.util.security.Security;

public class SecurityTest extends InstrumentationTestCase {
    private static final String TAG = SecurityTest.class.getSimpleName();

    static final String DATA = "|1.001|1|21.11.2014|0|Joe Bloggs|some@email.com||example.com|test.example.com|";
    static final String SIGNATURE = "lUjEjgRnu/Fb5yFNRXcIjjcdpwfKyJkXAXP9OWBIpjRN3HqRz+T4s9NSSYFhAcT0Q7SeiQZiOJn5zZllKSn0+Q==";
    static final String PUBLIC_KEY = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKZ8O3crJdibpagcIotKaWUTM5cNhXks"
            + "VE2sImMnLY8E4Qxs0jc90tIP8Hd25HXfc195gtoeb3OAQFvvX4ATTDcCAwEAAQ==";

    public void testEncryptDecrypt() throws UnsupportedEncodingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException {
        byte[] plainText = "ST".getBytes("UTF8");
        Log.i(TAG, "testEncryptDecrypt: plain: " + byteToHex(plainText));
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096);
        KeyPair key = keyGen.generateKeyPair();
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key.getPublic());
        byte[] encodedText = cipher.doFinal(plainText);
        Log.i(TAG, "testEncryptDecrypt: encoded: " + byteToHex(encodedText));
        cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key.getPrivate());
        byte[] decodedText = cipher.doFinal(encodedText);
        Log.i(TAG, "testEncryptDecrypt: decoded: " + byteToHex(decodedText));
    }

    public void testVerifySignature() {
        assertTrue(Security.verifySignature(PUBLIC_KEY, DATA, SIGNATURE));
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}
