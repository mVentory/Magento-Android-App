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
	private int id = INVALID_CATEGORY_ID;
	private Category parent;

	public Category(Map<String, Object> categoryData, Category parent) {
		super();
		this.parent = parent;
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
	
	public String getFullName()
	{
		if (parent != null)
			return parent.getFullName() + "/" + name;
		else
			return name;
	}

	public int getId() {
		return id;
	}
	
	private String getIdListWithSlashes()
	{
		StringBuilder s = new StringBuilder();
		
		if (parent != null)
		{
			s.append(parent.getIdListWithSlashes());
			s.append("/");
		}

		s.append(id);
		return s.toString();
	}
	
	public String [] getIdList()
	{
		return getIdListWithSlashes().split("/");
	}

}
