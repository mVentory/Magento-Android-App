package com.mageventory.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mageventory.MageventoryConstants;

import android.text.TextUtils;
import android.util.Log;

public class Product implements MageventoryConstants, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String maincategory;

	private String maincategory_name;

	private int status;

	private String id;

	private String sku;

	private String type;

	private String name;

	private Double price;

	private Double cost;

	private String shortDescription;

	private String description;

	private Boolean enabled;

	private Double weight;

	private List<String> categories = new ArrayList<String>();
	
	private String quantity;
	
	private int manageStock;
	
	private int isInStock;

	private int attributeSetId = INVALID_ATTRIBUTE_SET_ID;
	private Map<String, Object> data;


	private int attrSetID;
	private Map<String,Object> attrValuesList;
	private Map<String,Object> attrNamesList;

	

	public Map<String, Object> getData() {
	    return data;
	}
	

	
	private String url;
	
	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMaincategory_name() {
		return maincategory_name;
	}

	public void setMaincategory_name(String maincategory_name) {
		this.maincategory_name = maincategory_name;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPrice() {
		
		String strPrice = String.valueOf(price);
		
		String [] strPriceParts = strPrice.split("\\.");
		
		// if > 1 --> then there is fraction
		if(strPriceParts.length > 1)
		{
			// check if fraction is zero 
			if(Integer.valueOf(strPriceParts[1]) == 0)
				// Fraction is Zero return part before fraction only
				return strPriceParts[0];	
		}
		
		// Fraction is not zero then return it as it is
		return strPrice;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public Double getCost() {
		return cost;
	}

	public void setCost(Double cost) {
		this.cost = cost;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public void addCategory(String category) {
		this.categories.add(category);
	}
	
	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

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

	public Product(Map<String, Object> map, boolean full) {
	    data = map;
	    
		this.name = "" + map.get("name");
		this.id = "" + map.get("product_id");
		if (full) {
			this.sku = "" + map.get("sku");
			this.weight = safeParseDouble(map,  "weight");
			this.status = safeParseInt(map, "status");
			this.price = safeParseDouble(map, "price");
			this.description = "" + map.get("description");
			this.status = safeParseInt(map, "status");
			this.quantity = "" + map.get(MAGEKEY_PRODUCT_QUANTITY);
			this.manageStock = safeParseInt(map, MAGEKEY_PRODUCT_MANAGE_INVENTORY);
			this.isInStock = safeParseInt(map, MAGEKEY_PRODUCT_IS_IN_STOCK);			
			
			// Check the Manage Stock if Quantity is Zero
			// If QTY = -1 Manage Stock is Not Enabled then QTY is NULL
			if(quantity.contains("-1000000"))
				quantity = "";
					
			Object[] o = (Object[]) map.get("categories");
			if (o != null && o.length > 0) {
				Log.d("APP", o[0].toString());
				this.maincategory = o[0].toString();
			} else {
				this.maincategory = "";
			}

                        // y: huss, we have to coordinate... we both added attribute set id fields...			
			attributeSetId = safeParseInt(map, "set");
			attributeSetId = attributeSetId > 0 ? attributeSetId : INVALID_ATTRIBUTE_SET_ID;
			
			// Get Attribute Set ID 
			this.attrSetID = safeParseInt(map, "set");
			
			Set<String> keysSet = map.keySet();
			Object [] keys = keysSet.toArray();
			if(keys.length > 0)
			{
				this.attrValuesList =  new HashMap<String, Object>();
				this.attrNamesList = new HashMap<String, Object>();
			}
			
			for(int i=0;i<keys.length;i++)
			{				
				if(keys[i].toString().endsWith("_"))
				{
					this.attrValuesList.put(keys[i].toString(), map.get(keys[i].toString()));
				}
			}			
		}
	}
	
	public int getAttributeSetId() {
	    return attributeSetId;
	}

	public Product()
	{
		this.id = String.valueOf(INVALID_PRODUCT_ID);
	}
	
	
	public String getMaincategory() {
		return maincategory;
	}

	public void setMaincategory(String maincategory) {
		this.maincategory = maincategory;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}
	
	public String getQuantity() {
		
		String [] quantityParts = this.quantity.split("\\.");
		if(quantityParts.length > 1)
		{
			if(Integer.valueOf(quantityParts[1]) == 0)
				return quantityParts[0];
		}
		
		return this.quantity;
	}
	
	public void setInventory(int inventory)
	{
		manageStock = inventory;
	}
	
	public int getInventory()
	{
		return manageStock;
	}
	
	public void setIsInStock(int isInStock)
	{
		this.isInStock = isInStock;
	}
	
	public int getIsInStock()
	{
		return isInStock;
	}

	/**
	 * @return the attrSetID
	 */
	public int getAttrSetID() {
		return attrSetID;
	}

	/**
	 * @param attrSetID the attrSetID to set
	 */
	public void setAttrSetID(int attrSetID) {
		this.attrSetID = attrSetID;
	}

	
	/**
	 * @return the attrValuesList
	 */
	public Map<String, Object> getAttrValuesList() {
		return attrValuesList;
	}

	/**
	 * @param attrValuesList the attrValuesList to set
	 */
	public void setAttrValuesList(Map<String, Object> attrValuesList) {
		this.attrValuesList = attrValuesList;
	}

	/**
	 * @return the attrNamesList
	 */
	public Map<String, Object> getAttrNamesList() {
		return attrNamesList;
	}

	/**
	 * @param attrNamesList the attrNamesList to set
	 */
	public void setAttrNamesList(Map<String, Object> attrNamesList) {
		this.attrNamesList = attrNamesList;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	
	
}
