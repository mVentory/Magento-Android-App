package com.mageventory.test.util.security;

import android.test.InstrumentationTestCase;
import android.util.Log;

import com.mageventory.R;
import com.mageventory.util.security.Security;
import com.mageventory.util.security.StringXORer;

public class TestStringXORer extends InstrumentationTestCase {
    private static final String TAG = TestStringXORer.class.getSimpleName();

    public void testEncodeDecode() {
        String appKey = getInstrumentation().getTargetContext().getString(
                R.string.application_public_key);
        String cryptKey = Security.CRYPT_KEY;
        Log.i(TAG, "App key: " + appKey);

        String encoded = StringXORer.encode(appKey, cryptKey);
        Log.i(TAG, "Encoded: " + encoded);
        assertNotNull(encoded);
        assertTrue(!appKey.equals(encoded));

        String decoded = StringXORer.decode(encoded, cryptKey);
        Log.i(TAG, "Decoded: " + decoded);
        assertNotNull(decoded);
        assertEquals(appKey, decoded);
    }

    public void testDecode() {
        String appKey = getInstrumentation().getTargetContext().getString(
                R.string.application_public_key);
        String cryptKey = Security.CRYPT_KEY;
        Log.i(TAG, "App key: " + appKey);

        String decoded = StringXORer.decode(appKey, cryptKey);
        Log.i(TAG, "Decoded: " + decoded);
        assertNotNull(decoded);
        assertEquals(SecurityTest.PUBLIC_KEY, decoded);
    }
}
