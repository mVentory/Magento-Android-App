package com.mageventory.model;

public class Category {

	private String name;
	private String id;

	public Category(String name, String id) {
		super();
		this.name = name;
		this.id = id;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return Integer.parseInt(id);
	}

	public void setId(String id) {
		this.id = id;
	}
	@Override
	public String toString() {
		return name;
	}


}
