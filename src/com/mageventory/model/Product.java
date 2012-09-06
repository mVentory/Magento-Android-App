package com.mageventory.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.util.LangUtils;

import com.mageventory.MageventoryConstants;

import android.text.TextUtils;
import com.mageventory.util.Log;

public class Product implements MageventoryConstants, Serializable {

	/**
	 * This Class contains Attributes Information
	 * 
	 * @author hussein
	 * 
	 */
	public class CustomAttributeInfo implements Serializable {
		private static final long serialVersionUID = 1L;

		private String label;
		private String type;
		private String key;
		private String value;
		private String valueLabel;
		Object[] Options;

		/**
		 * @return the label
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * @param label
		 *            the label to set
		 */
		public void setLabel(String label) {
			this.label = label;
		}

		/**
		 * @return the type
		 */
		public String getType() {
			return type;
		}

		/**
		 * @param type
		 *            the type to set
		 */
		public void setType(String type) {
			this.type = type;
		}

		/**
		 * @return the key
		 */
		public String getKey() {
			return key;
		}

		/**
		 * @param key
		 *            the key to set
		 */
		public void setKey(String key) {
			this.key = key;
		}

		/**
		 * @return the value
		 */
		public String getValue() {
			return value;
		}

		/**
		 * @param value
		 *            the value to set
		 */
		public void setValue(String value) {
			this.value = value;
		}

		/**
		 * @return the valueLabel
		 */
		public String getValueLabel() {
			return valueLabel;
		}

		/**
		 * @param valueLabel
		 *            the valueLabel to set
		 */
		public void setValueLabel(String valueLabel) {

			this.valueLabel = valueLabel;
			// Special Types Handling
			// 1- if Type is Date then remove "00:00:00"
			this.valueLabel = this.valueLabel.replace("00:00:00", "");

			// 2- if type is float remove ".0000"
			this.valueLabel = this.valueLabel.replace(".0000", "");

		}

		/**
		 * @return the options
		 */
		public Object[] getOptions() {
			return Options;
		}

		/**
		 * @param options_objects
		 *            the options to set
		 */
		public void setOptions(Object[] options_objects) {
			Options = options_objects;
		}
	};

	/**
	 * This Class contains Image Information
	 * 
	 * @author hussein
	 * 
	 */
	public class imageInfo implements Serializable {
		private static final long serialVersionUID = 1L;

		private String imgURL;
		private String imgName;
		private int imgPosition;
		private boolean isMain;

		public void setMain(boolean isMain) {
			this.isMain = isMain;
		}

		public boolean getMain() {
			return isMain;
		}

		/**
		 * @return the imgURL
		 */
		public String getImgURL() {
			return imgURL;
		}

		/**
		 * @param imgURL
		 *            the imgURL to set
		 */
		public void setImgURL(String imgURL) {
			this.imgURL = imgURL;
		}

		/**
		 * @return the imgName
		 */
		public String getImgName() {
			return imgName;
		}

		/**
		 * @param imgName
		 *            the imgName to set
		 */
		public void setImgName(String imgName) {
			this.imgName = imgName;
		}

		/**
		 * @return the imgPosition
		 */
		public int getImgPosition() {
			return imgPosition;
		}

		/**
		 * @param imgPosition
		 *            the imgPosition to set
		 */
		public void setImgPosition(int imgPosition) {
			this.imgPosition = imgPosition;
		}
	}

	private static final long serialVersionUID = 1L;

	private Double weight; // WEIGHT
	private String id; // PRODUCT ID
	private int visibility; // VISIBILITY
	private Object tirePrice; // TIRE PRICE
	private String urlPath; // URL PATH
	private int attributeSetId = INVALID_ATTRIBUTE_SET_ID; // ATTRIBUTE SET ID
	private String type; // PRODUCT TYPE
	private ArrayList<CustomAttributeInfo> attrList = new ArrayList<Product.CustomAttributeInfo>(); // LIST
																									// OF
																									// CUSTOM
																									// ATTRIBUTES
	private int requiredOptions; // REQUIRED OPTIONS
	private String shortDescription; // PRODUCT SHORT DESCRIPTION
	private String optionsContainer; // OPTIONS CONTAINER
	private int taxClassId; // TAX CLASS ID
	private String description; // PRODUCT DESCRIPTION
	private String name; // PRODUCT NAME
	private String urlKey; // URL KEY
	private int googleCheckoutEnabled; // ENABLE GOOGLE CHECKOUT
	private int status; // STATUS
	private String quantity; // QUANTITY
	private int hasOptions; // HAS OPTIONS
	private ArrayList<Object> webSites = new ArrayList<Object>(); // WEBSITES
	private String sku; // PRODUCT SKU
	private String typeId; // TYPE ID
	private ArrayList<String> categoriesIds = new ArrayList<String>(); // CATEGORIES
																		// IDs
																		// LIST
	private Double price; // PRODUCT PRICE
	private ArrayList<imageInfo> images = new ArrayList<imageInfo>(); // IMAGES
	private int isInStock; // IS IN STOCK

	private String maincategory; // Main Category ID
	private Double cost; // PRODUCT COST
	private Boolean enabled; // ENABLED
	private int manageStock = 1; // MANAGE STOCK

	private Map<String, Object> data; // DATA -- CONTAINS ALL PRODUCT INFO
	private String url; // PRODUCT SHOP URL

	/*
	 * When an object of this Product class is stored in cache then its data is
	 * merged with product edit job data (if there are any pending edit jobs).
	 * The merge is done because we want to show the product to the user (for
	 * example in ProductDetailsActivity) the way it is going to look like after
	 * the edit succeeds. The problem is that edit job can fail and the we need
	 * to revert the merge. In order to do that easily we just store a copy of
	 * unmerged product here.
	 */
	private Product unmergedProduct;
	
	/************************************** SETTERS **********************************************/
	
	public void setUnmergedProduct(Product unmergedProd)
	{
		unmergedProduct = unmergedProd;
	}

	/************************************** GETTERS **********************************************/
	
	public Product getUnmergedProduct()
	{
		return unmergedProduct;
	}

	public Map<String, Object> getData() {
		return data;
	}

	/**
	 * @return the visibility
	 */
	public int getVisibility() {
		return visibility;
	}

	/**
	 * @return the tirePrice
	 */
	public Object getTirePrice() {
		return tirePrice;
	}

	/**
	 * @return the urlPath
	 */
	public String getUrlPath() {
		return urlPath;
	}

	/**
	 * @return the attrList
	 */
	public ArrayList<CustomAttributeInfo> getAttrList() {
		return attrList;
	}

	/**
	 * @return the requiredOptions
	 */
	public int getRequiredOptions() {
		return requiredOptions;
	}

	/**
	 * @return the optionsContainer
	 */
	public String getOptionsContainer() {
		return optionsContainer;
	}

	/**
	 * @return the taxClassId
	 */
	public int getTaxClassId() {
		return taxClassId;
	}

	/**
	 * @return the urlKey
	 */
	public String getUrlKey() {
		return urlKey;
	}

	/**
	 * @return the googleCheckoutEnabled
	 */
	public int getGoogleCheckoutEnabled() {
		return googleCheckoutEnabled;
	}

	/**
	 * @return the hasOptions
	 */
	public int getHasOptions() {
		return hasOptions;
	}

	/**
	 * @return the webSites
	 */
	public ArrayList<Object> getWebSites() {
		return webSites;
	}

	/**
	 * @return the typeId
	 */
	public String getTypeId() {
		return typeId;
	}

	/**
	 * @return the images
	 */
	public ArrayList<imageInfo> getImages() {
		return images;
	}

	/**
	 * @return the manageStock
	 */
	public int getManageStock() {
		return manageStock;
	}

	public String getId() {
		return id;
	}

	public String getSku() {
		return sku;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getPrice() {

		String strPrice = String.valueOf(price);

		String[] strPriceParts = strPrice.split("\\.");

		// if > 1 --> then there is fraction
		if (strPriceParts.length > 1) {
			// check if fraction is zero
			if (Integer.valueOf(strPriceParts[1]) == 0)
				// Fraction is Zero return part before fraction only
				return strPriceParts[0];
		}

		// Fraction is not zero then return it as it is
		return strPrice;
	}

	public Double getCost() {
		return cost;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public String getDescription() {
		return description;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public Double getWeight() {
		return weight;
	}

	public int getAttributeSetId() {
		return attributeSetId;
	}

	public String getMaincategory() {
		return maincategory;
	}

	public int getStatus() {
		return status;
	}

	public String getQuantity() {

		String[] quantityParts = this.quantity.split("\\.");
		if (quantityParts.length > 1) {
			if (Integer.valueOf(quantityParts[1]) == 0)
				return quantityParts[0];
		}

		return this.quantity;
	}

	public int getInventory() {
		return manageStock;
	}

	public int getIsInStock() {
		return isInStock;
	}

	/**
	 * @return the attrList
	 */
	public ArrayList<CustomAttributeInfo> getAttrValuesList() {
		return attrList;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**********************************************************************************************************/

	/************************************** PARSING FUNCTIONS **********************************************/
	private static int safeParseInt(Map<String, Object> map, String key) {
		final Object o = map.get(key);
		if (o != null) {
			if (o instanceof String) {
				final String s = (String) o;
				if (TextUtils.isDigitsOnly(s)) {
					return Integer.parseInt(s);
				}
			} else if (o instanceof Integer) {
				return ((Integer) o).intValue();
			}
		}
		return 0;
	}

	private static double safeParseDouble(Map<String, Object> map, String key) {
		final Object o = map.get(key);
		if (o != null) {
			if (o instanceof String) {
				final String s = (String) o;
				try {
					return Double.parseDouble(s);
				} catch (NumberFormatException e) {
				}
			} else if (o instanceof Double) {
				return ((Double) o).doubleValue();
			}
		}
		return 0D;
	}

	/**********************************************************************************************************/

	/************************************** CONSTRUCTORS ***********************************************/
	
	public Product getCopy() { 
		return new Product(data);
	}
	 
	@SuppressWarnings("unchecked")
	public Product(Map<String, Object> map) {
		data = map;

		this.name = "" + map.get(MAGEKEY_PRODUCT_NAME); // GET NAME [USEFUL]
		this.id = "" + map.get(MAGEKEY_PRODUCT_ID); // GET PRODUCT ID [USEFUL]
		this.sku = "" + map.get(MAGEKEY_PRODUCT_SKU); // GET SKU [USEFUL]
		this.weight = safeParseDouble(map, MAGEKEY_PRODUCT_WEIGHT); // GET
																	// WEIGHT
																	// [USEFUL]
		this.status = safeParseInt(map, MAGEKEY_PRODUCT_STATUS); // GET
																	// STATUS
																	// [USEFUL]
		this.price = safeParseDouble(map, MAGEKEY_PRODUCT_PRICE); // GET
																	// PRICE
																	// [USEFUL]
		this.description = "" + map.get(MAGEKEY_PRODUCT_DESCRIPTION); // GET
																		// DESCRIPTION
																		// [USEFUL]

		this.quantity = "" + map.get(MAGEKEY_PRODUCT_QUANTITY); // GET
																// QUANTITY
																// [USEFUL]
		// If QTY = -1 Manage Stock is Not Enabled then QTY is NULL
		if (quantity.contains("-1000000"))
			quantity = "";

		this.visibility = safeParseInt(map, MAGEKEY_PRODUCT_VISIBILITY); // GET
																			// VISIBILITY
																			// [NOT
																			// USEFUL]
		this.tirePrice = (Object) map.get(MAGEKEY_PRODUCT_TIER_PRICE); // GET
																		// TIRE
																		// PRICE
																		// [NOT
																		// USEFUL]
		this.urlPath = "" + map.get(MAGEKEY_PRODUCT_URL_PATH); // GET URL
																// PATH
																// [USEFUL]

		this.attributeSetId = safeParseInt(map, MAGEKEY_PRODUCT_ATTRIBUTE_SET_ID); // GET
																					// ATTRIBUTE
																					// SET
																					// ID
																					// [USEFUL]
		// Check Attribute Set ID
		attributeSetId = attributeSetId > 0 ? attributeSetId : INVALID_ATTRIBUTE_SET_ID;

		this.type = "" + map.get(MAGEKEY_PRODUCT_TYPE); // GET TYPE [NOT
														// USEFUL]
		this.requiredOptions = safeParseInt(map, MAGEKEY_PRODUCT_REQUIRED_OPTIONS); // GET
																					// REQUIRED
																					// OPTIONS
																					// [NOT
																					// USEFUL]
		this.shortDescription = "" + map.get(MAGEKEY_PRODUCT_SHORT_DESCRIPTION); // GET
																					// SHORT
																					// DESCRIPTION
																					// [NOT
																					// USEFUL]
		this.optionsContainer = "" + map.get(MAGEKEY_PRODUCT_OPTIONS_CONTAINER); // GET
																					// OPTIONS
																					// CONTAINER
																					// [NOT
																					// USEFUL]
		this.taxClassId = safeParseInt(map, MAGEKEY_PRODUCT_TAX_CLASS_ID); // GET
																			// TAX
																			// CLASS
																			// ID
																			// [NOT
																			// USEFUL]
		this.urlKey = "" + map.get(MAGEKEY_PRODUCT_URL_KEY); // GET URL KEY
																// [NOT
																// USEFUL]
		this.googleCheckoutEnabled = safeParseInt(map, MAGEKEY_PRODUCT_ENABLE_GOOGLE_CHECKOUT); // GET
																								// GOOGLE
																								// CHECKOUT
																								// [NOT
																								// USEFUL]
		this.hasOptions = safeParseInt(map, MAGEKEY_PRODUCT_HAS_OPTIONS); // GET
																			// HAS
																			// OPTIONS
																			// [NOT
																			// USEFUL]
		this.typeId = "" + map.get(MAGEKEY_PRODUCT_TYPE_ID); // GET TYPE ID
																// [NOT
																// USEFUL]
		this.isInStock = safeParseInt(map, MAGEKEY_PRODUCT_IS_IN_STOCK); // GET
																			// IS
																			// IN
																			// STOCK
																			// [NOT
																			// USEFUL]
		
		this.manageStock = safeParseInt(map, MAGEKEY_PRODUCT_MANAGE_INVENTORY);

		// Get Categories IDs & Categories
		Object[] categories_Ids = (Object[]) map.get(MAGEKEY_PRODUCT_CATEGORY_IDS);

		if (categories_Ids != null) {
			for (int i = 0; i < categories_Ids.length; i++) {
				this.categoriesIds.add(categories_Ids[i].toString());
			}
		}

		// Set Main Category
		if (this.categoriesIds.size() > 0) {
			this.maincategory = this.categoriesIds.get(0);
		}

		// GET IMAGES
		Object[] local_images = (Object[]) map.get(MAGEKEY_PRODUCT_IMAGES);

		for (int i = 0; i < local_images.length; i++) {
			imageInfo info = new imageInfo();
			Map<String, Object> local_image_info = (Map<String, Object>) local_images[i];
			info.setImgName(local_image_info.get("file").toString());
			info.setImgURL(local_image_info.get("url").toString());
			info.setImgPosition(safeParseInt(local_image_info, "position"));

			Object[] types = (Object[]) local_image_info.get("types");
			info.setMain(false);

			for (int j = 0; j < types.length; j++) {
				if (((String) types[j]).equals("image"))
					;
				{
					info.setMain(true);
				}
			}

			images.add(info);
		}

		// GET ATTRIBUTES
		// get list of custom attributes
		Object[] keys = map.keySet().toArray();
		Object[] local_attrInfo = (Object[]) map.get("set_attributes");
		for (int i = 0; i < keys.length; i++) {
			// Check If Custom Attribute
			if (keys[i].toString().endsWith("_")) {
				CustomAttributeInfo customInfo = new CustomAttributeInfo();
				customInfo.setKey(keys[i].toString());

				// Search For this Custom Attribute in Attribute List
				for (int j = 0; j < local_attrInfo.length; j++) {
					Map<String, Object> local_attr = (Map<String, Object>) local_attrInfo[j];
					if (local_attr.containsValue(keys[i])) {
						Object[] labels = (Object[]) local_attr.get("frontend_label");
						for (int ls = 0; ls < labels.length; ls++) {
							Map<String, Object> local_label = (Map<String, Object>) labels[ls];
							customInfo.setLabel(local_label.get("label").toString());
						}

						customInfo.setType(local_attr.get("frontend_input").toString());
						customInfo.setValue(map.get(keys[i]).toString());

						Object[] options_objects = (Object[]) local_attr.get(MAGEKEY_ATTRIBUTE_OPTIONS);

						if (options_objects.length > 0) {
							String[] list = map.get(keys[i]).toString().split(",");
							StringBuilder sb = new StringBuilder();

							for (int l = 0; l < list.length; l++) {
								for (int k = 0; k < options_objects.length; k++) {
									Map<String, Object> options = (Map<String, Object>) options_objects[k];
									if (TextUtils.equals(options.get("value").toString(), list[l])) {
										if (sb.length() > 0) {
											sb.append(", ");
										}

										sb.append(options.get("label").toString());
										break;
									}
								}
							}

							customInfo.setValueLabel(sb.toString());
						} else {
							// If there is no Options -- then value
							// label = value
							customInfo.setValueLabel(customInfo.getValue());
						}

						customInfo.setOptions(options_objects);
						break;
					}
				}

				this.attrList.add(customInfo);
			}
		}

	}

	public Product() {
		this.id = String.valueOf(INVALID_PRODUCT_ID);
	}

	/**********************************************************************************************************/

}
