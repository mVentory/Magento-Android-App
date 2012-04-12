package com.mageventory.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	
	private Double quantity;
	
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

	public Double getPrice() {
		return price;
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
		this.name = "" + map.get("name");
		this.id = "" + map.get("product_id");
		if (full) {
			this.sku = "" + map.get("sku");
			this.weight = safeParseDouble(map,  "weight");
			this.status = safeParseInt(map, "status");
			this.price = safeParseDouble(map, "price");
			this.description = "" + map.get("description");
			this.status = safeParseInt(map, "status");
			this.quantity = safeParseDouble(map, MAGEKEY_PRODUCT_QUANTITY);
			
			Object[] o = (Object[]) map.get("categories");
			if (o != null && o.length > 0) {
				Log.d("APP", o[0].toString());
				this.maincategory = o[0].toString();
			} else
				this.maincategory = "";
		}
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

	public void setQuantity(Double quantity) {
		this.quantity = quantity;
	}
	
	public Double getQuantity() {
		return this.quantity;
	}
	
}
