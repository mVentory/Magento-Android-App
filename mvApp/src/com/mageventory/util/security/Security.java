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

/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;

/**
 * Security-related methods.
 */
public class Security {
    /**
     * Tag used for logging
     */
    private static final String TAG = Security.class.getSimpleName();

    /**
     * Algorithm used for the public key
     */
    private static final String KEY_FACTORY_ALGORITHM = "RSA";
    /**
     * Algorithm used for the signature verification
     */
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    /**
     * The encryption key for the encoded application public key value
     */
    public static String CRYPT_KEY = "8Mkk*~kkklIjklmmn321";
    /**
     * The public key for the signature verification
     */
    public static final String PUBLIC_KEY = StringXORer.decode(
            getBase64EncodedPublicKeyCrypted(MyApplication.getContext()), CRYPT_KEY);

    /**
     * Get the encrypted application public key <br>
     * base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY (that you
     * got from the Google Play developer console). This is not your developer
     * public key, it's the *app-specific* public key. Instead of just storing
     * the entire literal string here embedded in the program, construct the key
     * at runtime from pieces or use bit manipulation (for example, XOR with
     * some other string) to hide the actual key. The key itself is not secret
     * information, but we don't want to make it easy for an attacker to replace
     * the public key with one of their own and then fake messages from the
     * server.
     * 
     * @param context
     * @return
     */
    public static String getBase64EncodedPublicKeyCrypted(Context context) {
        return context.getString(R.string.application_public_key);
    }

    /**
     * Verifies that the data was signed with the given signature, and returns
     * the verification result. The data is in string format and signed with a
     * private key.
     * 
     * @param base64PublicKey the base64-encoded public key to use for
     *            verifying.
     * @param signedData the signed string (signed, not encrypted, plain data)
     * @param signature the signature for the data, signed with the private key
     */
    public static boolean verifySignature(String base64PublicKey, String signedData,
            String signature) {
        if (signedData == null) {
            CommonUtils.error(TAG, "data is null");
            return false;
        }

        boolean verified = false;
        if (!TextUtils.isEmpty(signature)) {
            PublicKey key = Security.generatePublicKey(base64PublicKey);
            verified = Security.verify(key, signedData, signature);
            if (!verified) {
                Log.w(TAG, "signature does not match data.");
                return false;
            }
        }
        return true;
    }

    /**
     * Generates a PublicKey instance from a string containing the
     * Base64-encoded public key.
     * 
     * @param encodedPublicKey Base64-encoded public key
     * @throws IllegalArgumentException if encodedPublicKey is invalid
     */
    public static PublicKey generatePublicKey(String encodedPublicKey) {
        try {
            byte[] decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
            return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            CommonUtils.error(TAG, "Invalid key specification.");
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Verifies that the signature from the server matches the computed
     * signature on the data. Returns true if the data is correctly signed.
     * 
     * @param publicKey public key used for verification
     * @param signedData signed data from server
     * @param signature data signature
     * @return true if the data and signature match
     */
    public static boolean verify(PublicKey publicKey, String signedData, String signature) {
        Signature sig;
        try {
            sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(signedData.getBytes());
            if (!sig.verify(Base64.decode(signature, Base64.DEFAULT))) {
                CommonUtils.error(TAG, "Signature verification failed.");
                return false;
            }
            return true;
        } catch (NoSuchAlgorithmException e) {
            CommonUtils.error(TAG, "NoSuchAlgorithmException.", e);
        } catch (InvalidKeyException e) {
            CommonUtils.error(TAG, "Invalid key specification.", e);
        } catch (SignatureException e) {
            CommonUtils.error(TAG, "Signature exception.", e);
        }
        return false;
    }

    /**
     * Verify the license signature and store URL information
     * 
     * @param url the store URL to verify
     * @param license the license text
     * @param signature the license signature
     * @param silent whether to do not show any alerts if invalid license or URL
     *            is passed
     * @return true if license signature is valid and URL is allowed, false
     *         otherwise
     */
    public static boolean verifyLicenseAndStore(String url, String license, String signature,
            boolean silent) {
        return verifyLicense(license, signature, silent) && checkStoreValid(license, url, silent);
    }

    /**
     * Verify the license signature
     * 
     * @param license the license text
     * @param signature the license signature is passed
     * @param silent whether to do not show any alerts if license signature is
     *            invalid
     * @return true if license signature is valid, false otherwise
     */
    public static boolean verifyLicense(String license, String signature, boolean silent) {
        boolean validLicense = false;
        if (!TextUtils.isEmpty(license) && !TextUtils.isEmpty(signature)) {
            validLicense = verifySignature(PUBLIC_KEY, license, signature);
        }
        if (!validLicense && !silent) {
            GuiUtils.alert(R.string.invalid_license_information);
        }
        return validLicense;
    }

    /**
     * Check whether the store URL is licensed
     * 
     * @param license the license text
     * @param url the store URL to check
     * @param silent whether to do not show any alerts store URL is not allowed
     * @return true if store URL is allowed by license, false otherwise
     */
    public static boolean checkStoreValid(String license, String url, boolean silent) {
        boolean validStore = false;
        try {
            String[] licenseParts = license.split("\\|\\|");
            if (licenseParts.length == 2) {
                String[] licensedUrls = licenseParts[1].split("\\|");
                Uri uri = Uri.parse(url);
                for (String licensedUrl : licensedUrls) {
                    if (uri.getHost().equals(licensedUrl)) {
                        validStore = true;
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            CommonUtils.error(TAG, ex);
        }
        if (!validStore && !silent) {
            GuiUtils.alert(R.string.store_is_not_licensed);
        }
        return validStore;
    }
}
