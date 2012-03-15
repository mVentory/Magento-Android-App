package com.mageventory;

import java.util.ArrayList;

import android.app.Application;
import android.util.Log;

import com.mageventory.client.MagentoClient;
import com.mageventory.model.Category;
import com.mageventory.model.Product;

public class MyApplication extends Application
{
	static MagentoClient client;
	static private boolean dirty;
	private ArrayList<Product> products;
	private ArrayList<Category> categories;
	
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		Log.d("APP","Appcreated");
		dirty=true;
		client=null;
		products=new ArrayList<Product>();
		categories=new ArrayList<Category>();
	}
	public ArrayList<Product> getProducts() {
		return products;
	}
	public void setProducts(ArrayList<Product> products) {
		this.products.clear();
		this.products.addAll(products);

	}
	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
		
	}

	public ArrayList<Category> getCategories() {
		return categories;
	}
	public void setCategories(ArrayList<Category> categories) {
		this.categories.clear();
		this.categories.addAll(categories);
	}
	protected MagentoClient getClient()
	{
		if(client==null){
			client=new MagentoClient(getApplicationContext());
		}
		return client;
	}
	protected void setClient(String url,String user,String pass)
	{
		client=new MagentoClient(url,user,pass);
	}
	
}
