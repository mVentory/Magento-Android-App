
package com.mageventory.test.model.util;

import java.util.ArrayList;

import android.os.Parcel;
import android.test.InstrumentationTestCase;

import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttribute.ContentType;
import com.mageventory.model.CustomAttribute.CustomAttributeOption;
import com.mageventory.model.CustomAttribute.InputMethod;

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
		customAttribute.setReadOnly(true);
		customAttribute.setAddNewOptionsAllowed(true);
		customAttribute.setHtmlAllowedOnFront(true);
		customAttribute.setMainLabel("Test");
		customAttribute.setCode("test");
		customAttribute.setAttributeID("ID");
		customAttribute.setHint("hint");

		customAttribute.setContentType(ContentType.TEXT);
		customAttribute.setInputMethod(InputMethod.NUMERIC_KEYBOARD);

		customAttribute.addAlternateInputMethod(InputMethod.SCANNER);
		customAttribute.addAlternateInputMethod(InputMethod.GESTURES);
		customAttribute
				.addAlternateInputMethod(InputMethod.COPY_FROM_ANOTHER_PRODUCT);

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

		CustomAttribute clonedAttribute = customAttribute.clone();
		checkCustomAttributeInformation(clonedAttribute);
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
		assertEquals(attribute.getHint(), "hint");
		assertEquals(attribute.isConfigurable(), true);
		assertEquals(attribute.isUseForSearch(), true);
		assertEquals(attribute.isCopyFromSearch(), false);
		assertEquals(attribute.isReadOnly(), true);
		assertEquals(attribute.isAddNewOptionsAllowed(), true);
		assertEquals(attribute.isHtmlAllowedOnFront(), true);
		assertEquals(attribute.getContentType(), ContentType.TEXT);
		assertEquals(attribute.getInputMethod(), InputMethod.NUMERIC_KEYBOARD);
		ArrayList<InputMethod> alternateInputMethods = attribute
				.getAlternateInputMethods();
		assertNotNull(alternateInputMethods);
		assertTrue(alternateInputMethods.size() == 3);
		assertEquals(alternateInputMethods.get(0), InputMethod.SCANNER);
		assertEquals(alternateInputMethods.get(1), InputMethod.GESTURES);
		assertEquals(alternateInputMethods.get(2), InputMethod.COPY_FROM_ANOTHER_PRODUCT);

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
