package com.mageventory;

 import java.net.MalformedURLException;
import java.util.ArrayList;

import android.app.Application;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mageventory.client.MagentoClient;
import com.mageventory.client.MagentoClient2;
import com.mageventory.model.Category;
import com.mageventory.model.Product;
import com.mageventory.pref.MageventoryPreferences;
import com.mageventory.processor.CatalogCategoryTreeProcessor;
import com.mageventory.processor.CatalogProductListProcessor;
import com.mageventory.processor.ProductAttributeListProcessor;
import com.mageventory.processor.ProductAttributeSetListProcessor;
import com.mageventory.processor.CreateProductProcessor;
import com.mageventory.processor.ProductDeleteProcessor;
import com.mageventory.processor.ProductDetailsProcessor;
import com.mageventory.processor.ResExampleFeedProcessor;
import com.mageventory.processor.ResExampleImageProcessor;
import com.mageventory.processor.ResourceExpirationRegistry;
import com.mageventory.processor.UpdateProductProcessor;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.processor.CreateCartOrderProcessor;
import com.mageventory.processor.UploadImageProcessor;

public class MyApplication extends Application implements MageventoryConstants
{
	static MagentoClient client;
	static private boolean dirty;
	private ArrayList<Product> products;
	private ArrayList<Category> categories;
	private MageventoryPreferences preferences;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		configure();
		preferences = new MageventoryPreferences(this, PreferenceManager.getDefaultSharedPreferences(this));
		
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

//	public ArrayList<Category> getCategories() {
//		return categories;
//	}
//	
//	public void setCategories(ArrayList<Category> categories) {
//		this.categories.clear();
//		this.categories.addAll(categories);
//	}
	
	@Deprecated
	public MagentoClient getClient()
	{
		if(client==null){
			client=new MagentoClient(getApplicationContext());
		}
		return client;
	}
	
	// TODO y: rework
	public void setClient(String url,String user,String pass)
	{
		client=new MagentoClient(url,user,pass);

		// TODO y: rework client loading (make it automatic using preference change observer)
		try {
            client2 = new MagentoClient2(url, user, pass);
        } catch (MalformedURLException e) {
        }
		ResourceExpirationRegistry.getInstance().configChanged(this);
	}

	private MagentoClient2 client2;
	public MagentoClient2 getClient2() {
	    if (client2 == null) {
	        com.mageventory.settings.Settings s = new com.mageventory.settings.Settings(this);
	        try {
                client2 = new MagentoClient2(s.getUrl(), s.getUser(), s.getPass());
            } catch (MalformedURLException ignored) {
            }
	    }
	    return client2;
	}
	
	public MageventoryPreferences getPreferences() {
		return preferences;
	}
	
	private void configure() {
	    final ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
	    resHelper.bindResourceProcessor(RES_CATALOG_PRODUCT_LIST, new CatalogProductListProcessor());
	    resHelper.bindResourceProcessor(RES_PRODUCT_DETAILS, new ProductDetailsProcessor());
	    resHelper.bindResourceProcessor(RES_CATALOG_CATEGORY_TREE, new CatalogCategoryTreeProcessor());
	    resHelper.bindResourceProcessor(RES_CATALOG_PRODUCT_CREATE, new CreateProductProcessor());
	    resHelper.bindResourceProcessor(RES_CATALOG_PRODUCT_UPDATE, new UpdateProductProcessor());
		resHelper.bindResourceProcessor(RES_CART_ORDER_CREATE, new CreateCartOrderProcessor());
		resHelper.bindResourceProcessor(RES_CATALOG_PRODUCT_ATTRIBUTE_SET_LIST, new ProductAttributeSetListProcessor());
		resHelper.bindResourceProcessor(RES_PRODUCT_ATTRIBUTE_LIST, new ProductAttributeListProcessor());
		resHelper.bindResourceProcessor(RES_UPLOAD_IMAGE, new UploadImageProcessor());
		resHelper.bindResourceProcessor(RES_PRODUCT_DELETE, new ProductDeleteProcessor());
		
	    resHelper.bindResourceProcessor(RES_EXAMPLE_FEED, new ResExampleFeedProcessor());
	    resHelper.bindResourceProcessor(RES_EXAMPLE_IMAGE, new ResExampleImageProcessor());
	}

}