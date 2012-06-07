package com.mageventory;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.util.ArrayList;

import android.app.Application;
import android.preference.PreferenceManager;

import com.mageventory.client.MagentoClient;
import com.mageventory.client.MagentoClient2;
import com.mageventory.model.Category;
import com.mageventory.model.Product;
import com.mageventory.pref.MageventoryPreferences;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.resprocessor.CatalogCategoryTreeProcessor;
import com.mageventory.resprocessor.CatalogProductListProcessor;
import com.mageventory.resprocessor.CreateCartOrderProcessor;
import com.mageventory.resprocessor.ProductAttributeAddOptionProcessor;
import com.mageventory.resprocessor.ProductAttributeFullInfoProcessor;
import com.mageventory.resprocessor.ProductDeleteProcessor;
import com.mageventory.resprocessor.ProductDetailsProcessor;
import com.mageventory.resprocessor.ResExampleFeedProcessor;
import com.mageventory.resprocessor.ResExampleImageProcessor;
import com.mageventory.resprocessor.ResourceExpirationRegistry;
import com.mageventory.resprocessor.UpdateProductProcessor;
import com.mageventory.util.Log;
import com.mageventory.jobprocessor.CreateProductProcessor;
import com.mageventory.jobprocessor.JobProcessorManager;
import com.mageventory.jobprocessor.UploadImageProcessor;

public class MyApplication extends Application implements MageventoryConstants {
	public static final String APP_DIR_NAME = "mventory";

	static MagentoClient client;
	static private boolean dirty;
	private ArrayList<Product> products;
	private ArrayList<Category> categories;
	private MageventoryPreferences preferences;

	public class ApplicationExceptionHandler implements
			UncaughtExceptionHandler {

		private UncaughtExceptionHandler defaultUEH;

		public ApplicationExceptionHandler() {
			this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
		}

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			Log.logUncaughtException(e);
			defaultUEH.uncaughtException(t, e);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		configure();
		preferences = new MageventoryPreferences(this,
				PreferenceManager.getDefaultSharedPreferences(this));

		Log.d("APP", "Appcreated");
		dirty = true;
		client = null;
		products = new ArrayList<Product>();
		categories = new ArrayList<Category>();

		Thread.setDefaultUncaughtExceptionHandler(new ApplicationExceptionHandler());
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

	// public ArrayList<Category> getCategories() {
	// return categories;
	// }
	//
	// public void setCategories(ArrayList<Category> categories) {
	// this.categories.clear();
	// this.categories.addAll(categories);
	// }

	@Deprecated
	public MagentoClient getClient() {
		if (client == null) {
			client = new MagentoClient(getApplicationContext());
		}
		return client;
	}

	// TODO y: rework
	public void setClient(String url, String user, String pass) {
		client = new MagentoClient(url, user, pass);

		// TODO y: rework client loading (make it automatic using preference
		// change observer)
		try {
			client2 = new MagentoClient2(url, user, pass);
		} catch (MalformedURLException e) {
		}
		ResourceExpirationRegistry.getInstance().configChanged(this);
	}

	private MagentoClient2 client2;

	public MagentoClient2 getClient2() {
		if (client2 == null) {
			com.mageventory.settings.Settings s = new com.mageventory.settings.Settings(
					this);
			try {
				client2 = new MagentoClient2(s.getUrl(), s.getUser(),
						s.getPass());
			} catch (MalformedURLException ignored) {
			}
		}
		return client2;
	}

	public MageventoryPreferences getPreferences() {
		return preferences;
	}

	private void configure() {
		final ResourceServiceHelper resHelper = ResourceServiceHelper
				.getInstance();
		resHelper.bindResourceProcessor(RES_CATALOG_PRODUCT_LIST,
				new CatalogProductListProcessor());
		resHelper.bindResourceProcessor(RES_PRODUCT_DETAILS,
				new ProductDetailsProcessor());
		resHelper.bindResourceProcessor(RES_CATALOG_CATEGORY_TREE,
				new CatalogCategoryTreeProcessor());
		resHelper.bindResourceProcessor(RES_CATALOG_PRODUCT_UPDATE,
				new UpdateProductProcessor());
		resHelper.bindResourceProcessor(RES_CART_ORDER_CREATE,
				new CreateCartOrderProcessor());
		resHelper.bindResourceProcessor(RES_CATALOG_PRODUCT_ATTRIBUTES,
				new ProductAttributeFullInfoProcessor());
		resHelper.bindResourceProcessor(RES_PRODUCT_ATTRIBUTE_ADD_NEW_OPTION,
				new ProductAttributeAddOptionProcessor());

		// resHelper.bindResourceProcessor(RES_UPLOAD_IMAGE, new
		// UploadImageProcessor());
		resHelper.bindResourceProcessor(RES_PRODUCT_DELETE,
				new ProductDeleteProcessor());
		resHelper.bindResourceProcessor(RES_EXAMPLE_FEED,
				new ResExampleFeedProcessor());
		resHelper.bindResourceProcessor(RES_EXAMPLE_IMAGE,
				new ResExampleImageProcessor());

		JobProcessorManager.bindResourceProcessor(RES_CATALOG_PRODUCT_CREATE,
				new CreateProductProcessor());
		JobProcessorManager.bindResourceProcessor(RES_UPLOAD_IMAGE,
				new UploadImageProcessor());
	}

}