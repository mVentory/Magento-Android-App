
package com.mageventory.test.model.util;

import java.util.ArrayList;

import android.os.Parcel;
import android.test.InstrumentationTestCase;

import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttribute.CustomAttributeOption;

public class CustomAttributeTest extends InstrumentationTestCase {
	public void testCustomAttributeParcelable()
	{
		CustomAttribute customAttribute = new CustomAttribute();

		ArrayList<CustomAttributeOption> options = new ArrayList<CustomAttribute.CustomAttributeOption>();
		{
			CustomAttributeOption option = new CustomAttributeOption("1",
					"label1");
			option.setSelected(true);
			options.add(option);
		}
		{
			CustomAttributeOption option = new CustomAttributeOption("2",
					"label2");
			option.setSelected(false);
			options.add(option);
		}
		{
			CustomAttributeOption option = new CustomAttributeOption("3",
					"label3");
			option.setSelected(true);
			options.add(option);
		}
		customAttribute.setOptions(options);
		customAttribute.setType(CustomAttribute.TYPE_MULTISELECT);
		customAttribute.setIsRequired(true);
		customAttribute.setMainLabel("Test");
		customAttribute.setCode("test");
		customAttribute.setAttributeID("ID");

		customAttribute.setConfigurable(true);
		customAttribute.setUseForSearch(true);
		customAttribute.setCopyFromSearch(false);

		checkCustomAttributeInformation(customAttribute);

		customAttribute.setSelectedValue("1,3", false);
		checkCustomAttributeInformation(customAttribute);

		Parcel parcel = Parcel.obtain();
		customAttribute.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		CustomAttribute createFromParcel = CustomAttribute.CREATOR
				.createFromParcel(parcel);

		checkCustomAttributeInformation(createFromParcel);
	}

	public static void checkCustomAttributeInformation(CustomAttribute attribute)
	{
		assertNotNull(attribute);
		assertEquals(attribute.getSelectedValue(), "1,3");
		assertEquals(attribute.getType(), CustomAttribute.TYPE_MULTISELECT);
		assertEquals(attribute.getIsRequired(), true);
		assertEquals(attribute.getMainLabel(), "Test");
		assertEquals(attribute.getCode(), "test");
		assertEquals(attribute.getAttributeID(), "ID");
		assertEquals(attribute.isConfigurable(), true);
		assertEquals(attribute.isUseForSearch(), true);
		assertEquals(attribute.isCopyFromSearch(), false);

		ArrayList<CustomAttributeOption> options = attribute.getOptions();
		assertNotNull(options);

		assertTrue(options.size() == 3);

		int ind = 0;
		checkCustomAttributeOption(options.get(ind++), "1", "label1", true);
		checkCustomAttributeOption(options.get(ind++), "2", "label2", false);
		checkCustomAttributeOption(options.get(ind++), "3", "label3", true);
    }

	public static void checkCustomAttributeOption(CustomAttributeOption option, String id, String label, boolean selected){
    	assertNotNull(option);
    	assertEquals(option.getID(), id);
    	assertEquals(option.getLabel(), label);
		assertEquals(option.getSelected(), selected);
    }
}
