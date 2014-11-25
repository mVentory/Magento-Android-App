
package com.mageventory.test.model;

import android.test.InstrumentationTestCase;

import com.mageventory.model.CustomAttributesList;

public class CustomAttributesListTest extends InstrumentationTestCase {
    public void testFilterCompoundName() {
        assertEquals(
                CustomAttributesList
                        .filterCompoundName("-   ISBN ISSN hello-world  () (  - , .) -.,   5  -,"),
                "ISBN ISSN hello-world 5");
    }
}
