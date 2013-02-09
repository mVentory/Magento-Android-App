package com.mageventory.model;

import java.io.Serializable;
import java.util.Map;

import com.mageventory.MageventoryConstants;

public class Category implements MageventoryConstants, Serializable {

	/**
     * 
     */
	private static final long serialVersionUID = 1L;

	private String mName;
	private int mId = INVALID_CATEGORY_ID;
	private boolean mHasChildren;
	private Category mParent;

	public Category(Map<String, Object> categoryData, Category parent) {
		super();
		this.mParent = parent;
		mName = categoryData.get(MAGEKEY_CATEGORY_NAME).toString();
		if (mName == null) {
			throw new IllegalArgumentException("bad category name");
		}
		try {
			mId = Integer.parseInt(categoryData.get(MAGEKEY_CATEGORY_ID).toString());
		} catch (Throwable e) {
			throw new IllegalArgumentException("bad category id");
		}
	}

	@Override
	public String toString() {
		return "Category [name=" + mName + ", id=" + mId + "]";
	}

	public String getName() {
		return mName;
	}

	public String getFullName() {
		if (mParent != null)
			return mParent.getFullName() + "/" + mName;
		else
			return mName;
	}

	public int getId() {
		return mId;
	}
	
	public void setHasChildren(boolean hasChildren)
	{
		mHasChildren = hasChildren;
	}
	
	public boolean getHasChildren()
	{
		return mHasChildren;
	}
}
