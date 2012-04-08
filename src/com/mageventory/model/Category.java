package com.mageventory.model;

import java.io.Serializable;
import java.util.Map;

import com.mageventory.MageventoryConstants;

public class Category implements MageventoryConstants, Serializable {

	/**
     * 
     */
	private static final long serialVersionUID = 1L;

	private String name;
	private int id;

	public Category(Map<String, Object> categoryData) {
		super();
		name = categoryData.get(MAGEKEY_CATEGORY_NAME).toString();
		if (name == null) {
			throw new IllegalArgumentException("bad category name");
		}
		try {
			id = Integer.parseInt(categoryData.get(MAGEKEY_CATEGORY_ID).toString());
		} catch (Throwable e) {
			throw new IllegalArgumentException("bad category id");
		}
	}

	@Override
	public String toString() {
		return "Category [name=" + name + ", id=" + id + "]";
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

}
