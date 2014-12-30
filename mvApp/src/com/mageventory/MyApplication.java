/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/
package com.mageventory;

import java.lang.Thread.UncaughtExceptionHandler;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.mageventory.jobprocessor.AddProductToCartProcessor;
import com.mageventory.jobprocessor.CreateProductProcessor;
import com.mageventory.jobprocessor.CreateShipmentProcessor;
import com.mageventory.jobprocessor.JobProcessorManager;
import com.mageventory.jobprocessor.SellMultipleProductsProcessor;
import com.mageventory.jobprocessor.SellProductProcessor;
import com.mageventory.jobprocessor.SubmitToTMProductProcessor;
import com.mageventory.jobprocessor.UpdateProductProcessor;
import com.mageventory.jobprocessor.UploadImageProcessor;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.resprocessor.CartItemsProcessor;
import com.mageventory.resprocessor.CatalogCategoryTreeProcessor;
import com.mageventory.resprocessor.CatalogProductListProcessor;
import com.mageventory.resprocessor.ImageDeleteProcessor;
import com.mageventory.resprocessor.ImageMarkMainProcessor;
import com.mageventory.resprocessor.OrderDetailsProcessor;
import com.mageventory.resprocessor.OrdersListByStatusProcessor;
import com.mageventory.resprocessor.ProductAttributeAddOptionProcessor;
import com.mageventory.resprocessor.ProductAttributeFullInfoProcessor;
import com.mageventory.resprocessor.ProductDeleteProcessor;
import com.mageventory.resprocessor.ProductDetailsProcessor;
import com.mageventory.resprocessor.ProfileExecutionProcessor;
import com.mageventory.resprocessor.ProfilesListProcessor;
import com.mageventory.resprocessor.StatisticsProcessor;
import com.mageventory.util.ExternalImageUploader_deprecated;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.Log;
import com.mageventory.util.TrackerUtils;

public class MyApplication extends Application implements MageventoryConstants {
    public static final String APP_DIR_NAME = "mventory";

    public ExternalImageUploader_deprecated mExternalImageUploader;
    private static MyApplication instance;

    public MyApplication() {
        instance = this;
    }

    public static Context getContext() {
        return instance;
    }

    public class ApplicationExceptionHandler implements UncaughtExceptionHandler {

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

    public static ExternalImageUploader_deprecated getExternalImageUploader(Activity activity)
    {
        MyApplication ma = (MyApplication) activity.getApplication();
        return ma.mExternalImageUploader;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CalligraphyConfig.initDefault("fonts/Roboto-Regular.ttf", R.attr.fontPath);
        mExternalImageUploader = new ExternalImageUploader_deprecated(this);
        GuiUtils.setup();
        configure();

        Thread.setDefaultUncaughtExceptionHandler(new ApplicationExceptionHandler());
        TrackerUtils.setupTrackerUncaughtExceptionHandler();

        com.reactor.gesture_input.MyApplication.doOnCreate(this);
    }

    private void configure() {
        final ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
        resHelper
                .bindResourceProcessor(RES_CATALOG_PRODUCT_LIST, new CatalogProductListProcessor());
        resHelper.bindResourceProcessor(RES_PRODUCT_DETAILS, new ProductDetailsProcessor());
        resHelper.bindResourceProcessor(RES_CATALOG_CATEGORY_TREE,
                new CatalogCategoryTreeProcessor());
        resHelper.bindResourceProcessor(RES_CATALOG_PRODUCT_ATTRIBUTES,
                new ProductAttributeFullInfoProcessor());
        resHelper.bindResourceProcessor(RES_PRODUCT_ATTRIBUTE_ADD_NEW_OPTION,
                new ProductAttributeAddOptionProcessor());
        resHelper.bindResourceProcessor(RES_PRODUCT_DELETE, new ProductDeleteProcessor());
        resHelper.bindResourceProcessor(RES_DELETE_IMAGE, new ImageDeleteProcessor());
        resHelper.bindResourceProcessor(RES_MARK_IMAGE_MAIN, new ImageMarkMainProcessor());
        resHelper.bindResourceProcessor(RES_ORDERS_LIST_BY_STATUS,
                new OrdersListByStatusProcessor());
        resHelper.bindResourceProcessor(RES_ORDER_DETAILS, new OrderDetailsProcessor());
        resHelper.bindResourceProcessor(RES_CATALOG_PRODUCT_STATISTICS, new StatisticsProcessor());
        resHelper.bindResourceProcessor(RES_GET_PROFILES_LIST, new ProfilesListProcessor());
        resHelper.bindResourceProcessor(RES_EXECUTE_PROFILE, new ProfileExecutionProcessor());
        resHelper.bindResourceProcessor(RES_CART_ITEMS, new CartItemsProcessor());

        JobProcessorManager.bindResourceProcessor(RES_ORDER_SHIPMENT_CREATE,
                new CreateShipmentProcessor());
        JobProcessorManager.bindResourceProcessor(RES_CATALOG_PRODUCT_UPDATE,
                new UpdateProductProcessor());
        JobProcessorManager.bindResourceProcessor(RES_CATALOG_PRODUCT_CREATE,
                new CreateProductProcessor());
        JobProcessorManager.bindResourceProcessor(RES_UPLOAD_IMAGE, new UploadImageProcessor());
        JobProcessorManager.bindResourceProcessor(RES_CATALOG_PRODUCT_SELL,
                new SellProductProcessor());
        JobProcessorManager.bindResourceProcessor(RES_CATALOG_PRODUCT_SUBMIT_TO_TM,
                new SubmitToTMProductProcessor());
        JobProcessorManager.bindResourceProcessor(RES_ADD_PRODUCT_TO_CART,
                new AddProductToCartProcessor());
        JobProcessorManager.bindResourceProcessor(RES_SELL_MULTIPLE_PRODUCTS,
                new SellMultipleProductsProcessor());
    }

}
