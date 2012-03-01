package com.mageventory.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.util.Log;

public class Product {

	private String maincategory;
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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public Product(HashMap map,boolean full) {
		this.name = map.get("name").toString();
		this.id = map.get("product_id").toString();
		if(full){
		this.sku = map.get("sku").toString();
		this.weight=Double.parseDouble( map.get("weight").toString());
		this.status=Integer.parseInt(map.get("status").toString());
		this.price=Double.parseDouble( map.get("price").toString());
		this.description=map.get("description").toString();
		Object[] o=(Object[]) map.get("categories");
		Log.d("APP",o.length+" size");
		if(o.length>0){
			Log.d("APP",o[0].toString());
			this.maincategory=o[0].toString();
		}
		else this.maincategory="";
		}
		
		// {enable_googlecheckout=1, weight=1.0000, product_id=4,
		// visibility=4, status=1, tier_price=[Ljava.lang.Object;@44caa618,
		// url_path=new.html, set=4, has_options=0,
		// websites=[Ljava.lang.Object;@44c51608, type=simple, sku=211211,
		// type_id=simple, category_ids=[Ljava.lang.Object;@44c59528,
		// price=1111.0000, updated_at=2012-02-22 05:14:33,
		// required_options=0, short_description=1,
		// options_container=container2, description=1, name=new,
		// created_at=2012-02-22 05:14:33,
		// categories=[Ljava.lang.Object;@44cbbd90, url_key=new}
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

}
