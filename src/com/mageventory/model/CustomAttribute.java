package com.mageventory.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.mageventory.MageventoryConstants;

public class CustomAttribute implements Serializable {
	private static final long serialVersionUID = -6103686023229057345L;

	public static class CustomAttributeOption implements Serializable {
		private static final long serialVersionUID = -3872566328848103531L;
		private String mID;
		private String mLabel;
		private boolean mSelected;

		public CustomAttributeOption(String ID, String label) {
			mID = ID;
			mLabel = label;
		}

		public void setSelected(boolean selected) {
			mSelected = selected;
		}

		public boolean getSelected() {
			return mSelected;
		}

		public void setID(String ID) {
			mID = ID;
		}

		public String getID() {
			return mID;
		}

		public void setLabel(String label) {
			mLabel = label;
		}

		public String getLabel() {
			return mLabel;
		}
	}

	public static final String TYPE_BOOLEAN = "boolean";
	public static final String TYPE_SELECT = "select";
	public static final String TYPE_MULTISELECT = "multiselect";
	public static final String TYPE_DROPDOWN = "dropdown";
	public static final String TYPE_PRICE = "price";
	public static final String TYPE_DATE = "date";
	public static final String TYPE_TEXT = "text";

	private List<CustomAttributeOption> mOptions;
	private String mSelectedValue = "";
	private String mType;
	private boolean mIsRequired;
	private String mMainLabel;
	private String mCode;
	private String mAttributeID;
	private View mCorrespondingView;

	/*
	 * Reference to a spinning wheel shown when an option is being created for a
	 * custom attribute
	 */
	private View mNewOptionSpinningWheel;

	public void setAttributeID(String attribID) {
		mAttributeID = attribID;
	}

	public String getAttributeID() {
		return mAttributeID;
	}

	public void setCorrespondingView(View view) {
		mCorrespondingView = view;
	}

	public View getCorrespondingView() {
		return mCorrespondingView;
	}

	public void setNewOptionSpinningWheel(View spinningWheel) {
		mNewOptionSpinningWheel = spinningWheel;
	}

	public View getNewOptionSpinningWheel() {
		return mNewOptionSpinningWheel;
	}

	public void setCode(String code) {
		mCode = code;
	}

	public String getCode() {
		return mCode;
	}

	public void setMainLabel(String mainLabel) {
		mMainLabel = mainLabel;
	}

	public String getMainLabel() {
		return mMainLabel;
	}

	public void setIsRequired(boolean isRequired) {
		mIsRequired = isRequired;
	}

	public boolean getIsRequired() {
		return mIsRequired;
	}

	public String getType() {
		return mType;
	}

	public boolean isOfType(String type) {
		return mType.equals(type);
	}

	public void setType(String type) {
		mType = type;
	}

	public void setOptions(List<CustomAttributeOption> options) {
		mOptions = options;
	}

	public void setOptionsFromServerResponse(Object[] options) {
		mOptions = new ArrayList<CustomAttributeOption>();

		for (Object map : options) {
			Map<String, Object> optionsMap = (Map<String, Object>) map;
			CustomAttributeOption op;

			if (!mType.equals(TYPE_BOOLEAN)) {
				if (((String) optionsMap
						.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS_VALUE))
						.length() == 0) {
					continue;
				}

				op = new CustomAttributeOption(
						(String) optionsMap
								.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS_VALUE),
						(String) optionsMap
								.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS_LABEL));
			} else {
				op = new CustomAttributeOption(
						((Integer) optionsMap
								.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS_VALUE))
								.toString(),
						(String) optionsMap
								.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS_LABEL));

			}

			mOptions.add(op);
		}
	}

	public Object[] getOptionsAsArrayOfMaps() {
		List<Object> options = new ArrayList<Object>();

		for (CustomAttributeOption elem : mOptions) {
			Map<String, Object> mapElem = new HashMap<String, Object>();

			mapElem.put(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS_VALUE,
					elem.getID());
			mapElem.put(MageventoryConstants.MAGEKEY_ATTRIBUTE_OPTIONS_LABEL,
					elem.getLabel());

			options.add(mapElem);
		}

		Object[] out = new Object[options.size()];
		return options.toArray(out);
	}

	public List<CustomAttributeOption> getOptions() {
		return mOptions;
	}

	public List<String> getOptionsLabels() {
		List<String> out = new ArrayList<String>();
		for (int i = 0; i < mOptions.size(); i++) {
			out.add(mOptions.get(i).getLabel());
		}

		return out;
	}

	public void setOptionSelected(int idx, boolean selected, boolean updateView) {
		if (isOfType(CustomAttribute.TYPE_BOOLEAN)
				|| isOfType(CustomAttribute.TYPE_SELECT)
				|| isOfType(CustomAttribute.TYPE_DROPDOWN)) {
			for (CustomAttributeOption elem : mOptions) {
				elem.setSelected(false);
			}
		}

		mOptions.get(idx).setSelected(selected);

		if (updateView) {
			if (isOfType(CustomAttribute.TYPE_MULTISELECT)) {
				((EditText) mCorrespondingView)
						.setText(getUserReadableSelectedValue());
			} else if (isOfType(CustomAttribute.TYPE_BOOLEAN)
					|| isOfType(CustomAttribute.TYPE_SELECT)
					|| isOfType(CustomAttribute.TYPE_DROPDOWN)) {
				((Spinner) mCorrespondingView).setSelection(idx);
			}
		}
	}

	public String getSelectedValue() {
		if (isOfType(CustomAttribute.TYPE_MULTISELECT)) {
			/*
			 * List<String> out = new ArrayList<String>();
			 * for(CustomAttributeOption option : mOptions) { if
			 * (option.getSelected() == true) { out.add(option.getID()); } }
			 * return out.toArray(new String[out.size()]);
			 */
			StringBuilder out = new StringBuilder();
			for (CustomAttributeOption option : mOptions) {
				if (option.getSelected() == true) {
					if (out.length() > 0) {
						out.append(",");
					}
					out.append(option.getID());
				}
			}

			return out.toString();
		} else if (isOfType(CustomAttribute.TYPE_BOOLEAN)
				|| isOfType(CustomAttribute.TYPE_SELECT)
				|| isOfType(CustomAttribute.TYPE_DROPDOWN)) {
			for (CustomAttributeOption option : mOptions) {
				if (option.getSelected() == true) {
					return option.getID();
				}
			}
		} else {
			return mSelectedValue;
		}

		return null;
	}

	/*
	 * takes comma separated Strings which are either option ids or some text
	 * user entered in editbox (depending on type)
	 */
	public void setSelectedValue(String selectedValue, boolean updateView) {
		if (selectedValue == null)
			selectedValue = "";

		if (isOfType(CustomAttribute.TYPE_MULTISELECT)) {
			String[] selected = selectedValue.split(",");

			for (CustomAttributeOption option : mOptions) {
				for (int i = 0; i < selected.length; i++) {
					if (option.getID().equals(selected[i])) {
						option.setSelected(true);
						break;
					}
				}
			}
			if (updateView) {
				((EditText) mCorrespondingView)
						.setText(getUserReadableSelectedValue());
			}
		} else if (isOfType(CustomAttribute.TYPE_BOOLEAN)
				|| isOfType(CustomAttribute.TYPE_SELECT)
				|| isOfType(CustomAttribute.TYPE_DROPDOWN)) {
			int i = 0;
			for (CustomAttributeOption option : mOptions) {
				if (option.getID().equals(selectedValue)) {
					option.setSelected(true);
					if (updateView) {
						((Spinner) mCorrespondingView).setSelection(i);
					}
					break;
				}
				i++;
			}
		} else {
			mSelectedValue = selectedValue;
			if (updateView) {
				((EditText) mCorrespondingView)
						.setText(getUserReadableSelectedValue());
			}
		}
	}

	/* Get whatever we can put in editbox for user to see */
	public String getUserReadableSelectedValue() {
		if (isOfType(CustomAttribute.TYPE_MULTISELECT)) {
			StringBuilder out = new StringBuilder();
			for (CustomAttributeOption option : mOptions) {
				if (option.getSelected() == true) {
					if (out.length() > 0) {
						out.append(", ");
					}

					out.append(option.getLabel());
				}
			}

			return out.toString();
		} else if (isOfType(CustomAttribute.TYPE_BOOLEAN)
				|| isOfType(CustomAttribute.TYPE_SELECT)
				|| isOfType(CustomAttribute.TYPE_DROPDOWN)) {
			for (CustomAttributeOption option : mOptions) {
				if (option.getSelected() == true) {
					return option.getLabel();
				}
			}
			return "";
		} else {
			return mSelectedValue;
		}
	}
}
