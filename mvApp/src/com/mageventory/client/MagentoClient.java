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

package com.mageventory.client;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.job.JobCacheManager;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;
import com.mageventory.xmlrpc.XMLRPCClient;
import com.mageventory.xmlrpc.XMLRPCFault;

/**
 * Magento remote API client. This class is not thread-safe. It's purpose is to
 * replace the old one by providing stronger encapsulation and loose coupling
 * between the callers and the service.
 * 
 * @author Yordan Mildainov <jordanMiladinov@gmail.com>
 */
public class MagentoClient implements MageventoryConstants {

    /**
     * Constant used to execute various API calls
     */
    private static final String API_CALL = "call";

    /**
     * Name of the login API call
     */
    private static final String API_LOGIN = "login";

    /**
     * Name of the profile list API call
     */
    private static final String API_PROFILE_LIST = "mventory_dataflow_profile.list";
    /**
     * Name of the profile execute API call
     */
    private static final String API_PROFILE_EXECUTE = "mventory_dataflow_profile.execute";

    /**
     * Name of the category tree API call
     */
    private static final String API_CATEGORY_TREE = "mventory_category.tree";

    /**
     * Name of the product information API call
     */
    private static final String API_PRODUCT_INFO = "mventory_product.info";
    /**
     * Name of the product list API call
     */
    private static final String API_PRODUCT_LIST = "mventory_product.list";
    /**
     * Name of the product create API call
     */
    private static final String API_PRODUCT_CREATE = "mventory_product.create";
    /**
     * Name of the product duplicate API call
     */
    private static final String API_PRODUCT_DUPLICATE = "mventory_product.duplicate";
    /**
     * Name of the product statistics API call
     */
    private static final String API_PRODUCT_STATISTICS = "mventory_product.statistics";
    /**
     * Name of the product updated API call
     */
    private static final String API_PRODUCT_UPDATE = "mventory_product.update";
    /**
     * Name of the product delete API call
     */
    private static final String API_PRODUCT_DELETE = "mventory_product.delete";

    /**
     * Name of the product add attribute option API call
     */
    private static final String API_PRODUCT_ATTRIBUTE_ADD_OPTION = "mventory_product_attribute.addOption";

    /**
     * Name of the product attribute set list API call
     */
    private static final String API_PRODUCT_ATTRIBUTE_SET_LIST = "mventory_product_attribute_set.list";

    /**
     * Name of the product attribute media create API call
     */
    private static final String API_PRODUCT_ATTRIBUTE_MEDIA_CREATE = "mventory_product_attribute_media.create";
    /**
     * Name of the product attribute media update API call
     */
    private static final String API_PRODUCT_ATTRIBUTE_MEDIA_UPDATE = "mventory_product_attribute_media.update";
    /**
     * Name of the product attribute media remove API call
     */
    private static final String API_PRODUCT_ATTRIBUTE_MEDIA_REMOVE = "mventory_product_attribute_media.remove";

    /**
     * Name of the cart create order for product API call
     */
    private static final String API_CART_CREATE_ORDER_FOR_PRODUCT = "mventory_cart.createOrderForProduct";
    /**
     * Name of the cart create order for multiple products API call
     */
    private static final String API_CART_CREATE_ORDER_FOR_MULTIPLE_PRODUCTS = "mventory_cart.createOrderForMultipleProducts";
    /**
     * Name of the cart add to cart API call
     */
    private static final String API_CART_ADD_TO_CART = "mventory_cart.addItem";
    /**
     * Name of the cart information API call
     */
    private static final String API_CART_GET_CART = "mventory_cart.info";
    /**
     * Name of the clear cart API call
     */
    private static final String API_CART_CLEAR_CART = "mventory_cart.clear";

    /**
     * Name of the sales order list API call
     */
    private static final String API_ORDER_LIST = "mventory_order.list";
    /**
     * Name of the sales order information API call
     */
    private static final String API_ORDER_INFO = "mventory_order.info";

    /**
     * Name of the sales order create shipment API call
     */
    private static final String API_ORDER_SHIPMENT_CREATE = "mventory_order_shipment.create";

    /**
     * Name of the product submit to TradeMe API call
     */
    private static final String API_CATALOG_PRODUCT_SUBMIT_TO_TM = "trademe_product.submit";

    /*
     * Currently unused API calls
     */
    private static final String API_CATALOG_PRODUCT_ATTRIBUTE_SET_LIST = "catalog_product_attribute_set.list";
    private static final String API_CUSTOMER_LIST = "customer.list";
    private static final String API_SALES_ORDER_SHIPMENT_GET_CARRIERS = "sales_order_shipment.getCarriers";
    private static final String API_CATALOG_CATEGORY_INFO = "catalog_category.info";
    private static final String API_CATALOG_PRODUCT_ATTRIBUTE_MEDIA_LIST = "catalog_product_attribute_media.list";
    private static final String API_CATEGORY_ATTRIBUTE_LIST = "category_attribute.list";
    /*
     * End of unused API calls
     */



    public static final String TAG = MagentoClient.class.getSimpleName();

    private static String prepareForLikeQuery(String like) {
        if (like.startsWith("%") == false) {
            like = "%".concat(like);
        }
        if (like.endsWith("%") == false) {
            like = like.concat("%");
        }
        return like;
    }

    private final XMLRPCClient client;
    // private final XmlRpcClientConfigImpl config;
    // private final XmlRpcClient client;

    /**
     * Holds the last error message. Error messages are recorded when an
     * operation fails.
     */
    private String lastErrorMessage;

    /**
     * Holds the last error code
     */
    private int lastErrorCode;

    private SettingsSnapshot settingsSnapshot;

    /**
     * Session id. Retrieved by calling login.
     */
    private String sessionId = null;

    /*
     * For each different SettingsSnapshot we store session id returned by the
     * server so that we don't have to relogin each time.
     */
    private static Map<SettingsSnapshot, String> sessionIdMap = new HashMap<SettingsSnapshot, String>();
    /* Synchronises access to the hashmap */
    private static Object sessionIdMapSynchronisationObject = new Object();

    private static void storeSessionId(SettingsSnapshot settingsSnapshot, String sessionID)
    {
        synchronized (sessionIdMapSynchronisationObject)
        {
            sessionIdMap.put(settingsSnapshot, sessionID);
        }
    }

    private static String restoreSessionId(SettingsSnapshot settingsSnapshot)
    {
        synchronized (sessionIdMapSynchronisationObject)
        {
            return sessionIdMap.get(settingsSnapshot);
        }
    }

    public MagentoClient(SettingsSnapshot settings) throws MalformedURLException {
        super();

        settingsSnapshot = settings.getCopy();
        settingsSnapshot.setUrl(settingsSnapshot.getUrl() + XMLRPC_PATH);
        new URL(settingsSnapshot.getUrl()); // check if URL is OK, throw
                                            // exception if not
        client = new XMLRPCClient(settingsSnapshot.getUrl());
    }

    /**
     * Return the Java analog of the standard PHP associative array. That
     * "array" can be used to build up XMLRPC query filters easy.
     * 
     * @param key
     * @param value
     * @return
     */
    private Map<String, Object> array(String key, Object obj) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(key, obj);
        return map;
    }

    private boolean ensureLoggedIn() {

        if (TextUtils.isEmpty(sessionId)) {
            sessionId = restoreSessionId(settingsSnapshot);
        }

        if (TextUtils.isEmpty(sessionId)) {
            return login();
        }
        return true;
    }

    public List<Map<String, Object>> categoryAttributeList() {
        final MagentoClientTask<List<Map<String, Object>>> task = new MagentoClientTask<List<Map<String, Object>>>() {

            @Override
            @SuppressWarnings("unchecked")
            public List<Map<String, Object>> run() throws RetryAfterLoginException {
                try {
                    final Object resultObj = client.call(API_CALL, sessionId,
                            API_CATEGORY_ATTRIBUTE_LIST);
                    final Object[] objs = (Object[]) resultObj;
                    if (objs == null) {
                        return null;
                    }
                    final List<Map<String, Object>> attrs = new ArrayList<Map<String, Object>>(
                            objs.length);
                    for (final Object obj : objs) {
                        attrs.add((Map<String, Object>) obj);
                    }
                    return attrs;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    public Object[] productAttributeFullInfo() {
        final MagentoClientTask<Object[]> task = new MagentoClientTask<Object[]>() {

            @Override
            @SuppressWarnings("unchecked")
            public Object[] run() throws RetryAfterLoginException {
                try {
                    final Object[] resultObj = (Object[]) client.call(API_CALL, sessionId,
                            API_PRODUCT_ATTRIBUTE_SET_LIST);
                    // final Object resultObj = client.call("call", sessionId,
                    // "catalog_product_attribute.fullInfoList", new
                    // Object[]{18});

                    return resultObj;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    public Map<String, Object> productAttributeAddOption(final String attributeCode,
            final String optionLabel) {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {

            @Override
            @SuppressWarnings("unchecked")
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {
                    final Map<String, Object> resultObj = (Map<String, Object>) client.call(
                            API_CALL,
                            sessionId,
                            API_PRODUCT_ATTRIBUTE_ADD_OPTION, 
                            new Object[] {
                                    attributeCode,
                                    optionLabel
                            });

                    return resultObj;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    public List<Map<String, Object>> catalogProductAttributeSetList() {
        final MagentoClientTask<List<Map<String, Object>>> task = new MagentoClientTask<List<Map<String, Object>>>() {

            @Override
            @SuppressWarnings("unchecked")
            public List<Map<String, Object>> run() throws RetryAfterLoginException {
                try {
                    final Object resultObj = client.call(API_CALL, sessionId,
                            API_CATALOG_PRODUCT_ATTRIBUTE_SET_LIST);
                    final Object[] objs = (Object[]) resultObj;
                    if (objs == null) {
                        return null;
                    }
                    final List<Map<String, Object>> attrs = new ArrayList<Map<String, Object>>(
                            objs.length);
                    for (final Object obj : objs) {
                        attrs.add((Map<String, Object>) obj);
                    }
                    return attrs;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    public List<Map<String, Object>> productAttributeList(final int setId) {
        final MagentoClientTask<List<Map<String, Object>>> task = new MagentoClientTask<List<Map<String, Object>>>() {

            @Override
            @SuppressWarnings("unchecked")
            public List<Map<String, Object>> run() throws RetryAfterLoginException {
                try {
                    // final Object resultObj = client.call("call", sessionId,
                    // "product_attribute.list",
                    // new Object[] { setId });

                    final Object[] resultObj = (Object[]) client.call(API_CALL, sessionId,
                            API_PRODUCT_ATTRIBUTE_SET_LIST);

                    Object[] objs = null;

                    for (Object attrSet : resultObj) {
                        Map<String, Object> attrSetMap = (Map<String, Object>) attrSet;

                        if (TextUtils.equals((String) attrSetMap.get("set_id"), "" + setId)) {
                            objs = (Object[]) attrSetMap.get("attributes");
                            break;
                        }
                    }

                    if (objs == null) {
                        return null;
                    }
                    final List<Map<String, Object>> attrs = new ArrayList<Map<String, Object>>(
                            objs.length);
                    for (final Object obj : objs) {
                        attrs.add((Map<String, Object>) obj);
                    }
                    return attrs;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> catalogProductInfo(final int productId) {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {
                    Object resultObj = client.call(API_CALL, sessionId,
                            API_PRODUCT_INFO,
                            new Object[] {
                                productId
                            });
                    final Map<String, Object> result = (Map<String, Object>) resultObj;

                    return result;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> catalogProductStatistics() {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {
                    Object resultObj = client.call(API_CALL, sessionId,
                            API_PRODUCT_STATISTICS,
                            new Object[] {});
                    final Map<String, Object> result = (Map<String, Object>) resultObj;

                    return result;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> orderListByStatus(final String status) {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {
                    Object resultObj = client.call(API_CALL, sessionId,
                            API_ORDER_LIST,
                            new Object[] {
                                status
                            });

                    final Map<String, Object> result = (Map<String, Object>) resultObj;

                    return result;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    @SuppressWarnings("unchecked")
    public Object[] cartItems() {
        final MagentoClientTask<Object[]> task = new MagentoClientTask<Object[]>() {
            @Override
            public Object[] run() throws RetryAfterLoginException {
                try {
                    Object resultObj = client.call(API_CALL, sessionId, API_CART_GET_CART,
                            new Object[] {});

                    final Object[] result = (Object[]) resultObj;

                    return result;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    /**
     * Perform the clear cart API call
     * 
     * @return object array representing cart content in case of operation
     *         success, null otherwise
     */
    public Object[] cartClear() {
        final MagentoClientTask<Object[]> task = new MagentoClientTask<Object[]>() {
            @Override
            public Object[] run() throws RetryAfterLoginException {
                try {
                    Object resultObj = client.call(API_CALL, sessionId, API_CART_CLEAR_CART,
                            new Object[] {});

                    final Object[] result = (Object[]) resultObj;

                    return result;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> orderGetCarriers(final String orderIncrementID) {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {
                    Object resultObj = client.call(API_CALL, sessionId,
                            API_SALES_ORDER_SHIPMENT_GET_CARRIERS,
                            new Object[] {
                                orderIncrementID
                            });

                    final Map<String, Object> result = (Map<String, Object>) resultObj;

                    return result;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> orderDetails(final String orderIncrementId) {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {
                    Object resultObj = client.call(API_CALL, sessionId, API_ORDER_INFO,
                            new Object[] {
                                orderIncrementId
                            });
                    final Map<String, Object> result = (Map<String, Object>) resultObj;

                    return result;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object>[] catalogProductAttributeMediaList(final int productId) {
        final MagentoClientTask<Map<String, Object>[]> task = new MagentoClientTask<Map<String, Object>[]>() {
            @Override
            public Map<String, Object>[] run() throws RetryAfterLoginException {
                try {
                    Object result = client.call(API_CALL, sessionId,
                            API_CATALOG_PRODUCT_ATTRIBUTE_MEDIA_LIST,
                            new Object[] {
                                productId
                            });
                    return (Map<String, Object>[]) result;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> catalogCategoryTree() {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {
                    Object result = client.call(API_CALL, sessionId, API_CATEGORY_TREE);
                    return (Map<String, Object>) result;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    public Map<String, Object> catalogCategoryInfo(final int categoryId) {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> category = (Map<String, Object>) client.call(
                            API_CALL,
                            sessionId,
                            API_CATALOG_CATEGORY_INFO, 
                            new Object[] {
                                categoryId
                            });
                    return category;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    GuiUtils.noAlertError(TAG, e);
                    lastErrorMessage = e.getMessage();
                }
                return null;
            }
        };
        return retryTaskAfterLogin(task);
    }

    /**
     * Can be used to mark image as main on the server.
     */
    public Boolean catalogProductAttributeMediaUpdate(final String productID,
            final String imageName,
            final Map<String, Object> imageData) {
        final MagentoClientTask<Boolean> task = new MagentoClientTask<Boolean>() {
            @Override
            public Boolean run() throws RetryAfterLoginException {
                try {
                    Boolean result = (Boolean) client.call(API_CALL, sessionId,
                            API_PRODUCT_ATTRIBUTE_MEDIA_UPDATE,
                            new Object[] {
                                    productID, imageName, imageData
                            });
                    return result;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    GuiUtils.noAlertError(TAG, e);
                    lastErrorMessage = e.getMessage();
                }
                return null;
            }
        };
        return retryTaskAfterLogin(task);
    }

    /**
     * Remove an image from the server and get updated product details.
     * 
     * @param productID the product id
     * @param imageName the image name
     * @return updated product details
     */
    public Map<String, Object> catalogProductAttributeMediaRemoveAndReturnInfo(
            final String productID, final String imageName) {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
            @SuppressWarnings("unchecked")
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {
                    Object result = client.call(API_CALL, sessionId,
                            API_PRODUCT_ATTRIBUTE_MEDIA_REMOVE,
                            new Object[] {
                                    productID, imageName
                            });
                    return (Map<String, Object>) result;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    GuiUtils.noAlertError(TAG, e);
                    lastErrorMessage = e.getMessage();
                }
                return null;
            }
        };
        return retryTaskAfterLogin(task);
    }

    /**
     * Retrieve product list. Apply name filter.
     * 
     * @param @Nullable likeName
     * @return
     */
    public List<Map<String, Object>> catalogProductList(final String filter, final int categoryID) {
        final MagentoClientTask<List<Map<String, Object>>> task = new MagentoClientTask<List<Map<String, Object>>>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<Map<String, Object>> run() throws RetryAfterLoginException {
                try {
                    Object items;
                    if (categoryID != INVALID_CATEGORY_ID) {
                        items = ((Map) client.call(API_CALL, sessionId,
                                API_PRODUCT_LIST,
                                new Object[] {
                                        filter, categoryID
                                })).get("items");
                    } else {
                        items = ((Map) client.call(API_CALL, sessionId,
                                API_PRODUCT_LIST,
                                new Object[] {
                                    filter
                                })).get("items");
                    }
                    final Object[] products = JobCacheManager
                            .getObjectArrayWithMapCompatibility(items);
                    final List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(
                            products.length);
                    for (Object product : products) {
                        result.add((Map<String, Object>) product);
                    }
                    return result;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    GuiUtils.noAlertError(TAG, e);
                    lastErrorMessage = e.getMessage();
                }
                return null;
            }
        };
        return retryTaskAfterLogin(task);
    }

    public boolean catalogProductUpdate(final int productId, final Map<String, Object> productData) {
        final MagentoClientTask<Boolean> task = new MagentoClientTask<Boolean>() {
            @Override
            public Boolean run() throws RetryAfterLoginException {
                try {
                    final Map<String, Object> invInfo = new HashMap<String, Object>();
                    // iterate through stock data attributes
                    for (final String key : MAGEKEY_PRODUCT_STOCK_DATA_ATTRIBUTES) {
                        if (productData.containsKey(key)) {
                            invInfo.put(key, productData.remove(key));
                        }
                    }
                    if (!invInfo.isEmpty()) {
                        // if there were any stock data updates
                        productData.put(MAGEKEY_PRODUCT_STOCK_DATA, invInfo);
                    }

                    Boolean success = (Boolean) client.call(API_CALL, sessionId,
                            API_PRODUCT_UPDATE, new Object[] {
                                    productId, productData
                            });

                    return success == null || success == false ? Boolean.FALSE : Boolean.TRUE;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    GuiUtils.noAlertError(TAG, e);
                    lastErrorMessage = e.getMessage();
                }

                return Boolean.FALSE;
            }
        };
        return retryTaskAfterLogin(task);
    }

    /**
     * Return the id of the newly created product or -1 in case of error.
     * 
     * @param productData
     * @return
     */
    public Map<String, Object> catalogProductCreate(final String productType, final int attrSetId,
            final String sku,
            final Map<String, Object> productData, final boolean duplicationMode,
            final String skuToDuplicate,
            final String photoCopyMode, final float decreaseOriginalQuantity) {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {
                    final String[] invKeys = {
                            MAGEKEY_PRODUCT_QUANTITY, MAGEKEY_PRODUCT_MANAGE_INVENTORY,
                            MAGEKEY_PRODUCT_IS_IN_STOCK, MAGEKEY_PRODUCT_USE_CONFIG_MANAGE_STOCK,
                            MAGEKEY_PRODUCT_IS_QTY_DECIMAL
                    };
                    final Map<String, Object> invInfo = new HashMap<String, Object>();
                    boolean containsInvInfo = true;
                    for (final String key : invKeys) {
                        if (productData.containsKey(key)) {
                            invInfo.put(key, productData.remove(key));
                        } else {
                            containsInvInfo = false;
                            break;
                        }
                    }
                    if (containsInvInfo) {
                        productData.put(MAGEKEY_PRODUCT_STOCK_DATA, invInfo);
                    }

                    Map<String, Object> productMap = null;

                    if (duplicationMode)
                    {
                        productMap = (Map<String, Object>) client.call(API_CALL, sessionId,
                                API_PRODUCT_DUPLICATE,
                                new Object[] {
                                        skuToDuplicate, sku, productData, photoCopyMode,
                                        decreaseOriginalQuantity
                                });
                    }
                    else
                    {
                        productMap = (Map<String, Object>) client.call(API_CALL, sessionId,
                                API_PRODUCT_CREATE,
                                new Object[] {
                                        productType, String.valueOf(attrSetId), sku, productData
                                });
                    }

                    return productMap;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    GuiUtils.noAlertError(TAG, e);
                    lastErrorMessage = e.getMessage();
                    return null;
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    public Map<String, Object> catalogProductSubmitToTM(final int prodID,
            final Map<String, Object> tmData) {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {

                    @SuppressWarnings("unchecked")
                    Map<String, Object> productMap = (Map<String, Object>) client.call(API_CALL,
                            sessionId,
                            API_CATALOG_PRODUCT_SUBMIT_TO_TM,
                            new Object[] {
                                    prodID, tmData
                            });

                    return productMap;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    GuiUtils.noAlertError(TAG, e);
                    lastErrorMessage = e.getMessage();
                    return null;
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public int getLastErrorCode() {
        return lastErrorCode;
    }

    /**
     * Return session string if login operation is successful.
     * 
     * @param url
     * @param user
     * @param key
     * @return
     * @throws RemoteCallException
     */
    public boolean login() {
        try {
            sessionId = (String) client.call(API_LOGIN, settingsSnapshot.getUser(),
                    settingsSnapshot.getPassword());
            storeSessionId(settingsSnapshot, sessionId);
            return true;
        } catch (Throwable e) {
            CommonUtils.error(TAG, null, e);
            sessionId = null;
            storeSessionId(settingsSnapshot, sessionId);
            lastErrorMessage = e.getMessage();
            return false;
        }
    }

    /* Check if we managed to log in. */
    public boolean isLoggedIn()
    {
        if (TextUtils.isEmpty(sessionId) == true)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Execute the task. If it fails with {@link RetryAfterLoginException}, try
     * to login and retry executing it again.
     * 
     * @param task
     * @return result of the task or null, if login is unsuccessful
     */
    private <V> V retryTaskAfterLogin(final MagentoClientTask<V> task) {
        if (ensureLoggedIn() == false) {
            return null;
        }
        try {
            return task.run();
        } catch (RetryAfterLoginException e) {
            // drop
        } catch (Throwable e) {
            CommonUtils.error(TAG, null, e);
            lastErrorMessage = e.getMessage();
            return null;
        }
        if (login() == false) {
            return null;
        }
        try {
            return task.run();
        } catch (Throwable e) {
            CommonUtils.error(TAG, null, e);
            lastErrorMessage = e.getMessage();
            return null;
        }
    }

    @Override
    public String toString() {
        return "MagentoClient2 [serviceUrl=" + settingsSnapshot.getUrl() + ", user="
                + settingsSnapshot.getUser() + "]";
    }

    // filter helpers

    private void addNameFilter(final Map<String, Object> filter, String name) {
        if (TextUtils.isEmpty(name) == false) {
            name = prepareForLikeQuery(name);
            filter.put("name", array("like", name));
        }
    }

    @SuppressWarnings("unused")
    private void addSkuFilter(final Map<String, Object> filter, String sku) {
        if (TextUtils.isEmpty(sku) == false) {
            sku = prepareForLikeQuery(sku);
            filter.put("sku", array("like", sku));
        }
    }

    private void addCategoryIdFilter(final Map<String, Object> filter, Integer categoryId) {
        if (categoryId != null) {
            filter.put("category_ids", categoryId);
        }
    }

    @SuppressWarnings("unchecked")
    public Boolean addToCart(final Map<String, Object> productData) {
        final MagentoClientTask<Boolean> task = new MagentoClientTask<Boolean>() {
            @Override
            public Boolean run() throws RetryAfterLoginException {
                try {
                    client.call(API_CALL, sessionId, API_CART_ADD_TO_CART,
                            new Object[] {
                                productData
                            });

                    return true;
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    GuiUtils.noAlertError(TAG, e);
                    lastErrorMessage = e.getMessage();
                }
                return null;
            }
        };
        return retryTaskAfterLogin(task);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> orderCreate(final Map<String, Object> productData) {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {

                    String sku = productData.get(MAGEKEY_PRODUCT_SKU).toString();
                    float price = Float.valueOf(productData.get(MAGEKEY_PRODUCT_PRICE).toString());
                    String qtyString = productData.get(MAGEKEY_PRODUCT_QUANTITY)
                            .toString();
                    int customerID = Integer.valueOf(settingsSnapshot.getUser());
                    long transactionID = Long.valueOf(productData.get(
                            MAGEKEY_PRODUCT_TRANSACTION_ID).toString());
                    String name = productData.get(MAGEKEY_PRODUCT_NAME).toString();
                    Object result = client.call(API_CALL, sessionId,
                            API_CART_CREATE_ORDER_FOR_PRODUCT,
                            new Object[] {
                                    sku,
                                    price, qtyString, customerID,
                                    (int) (transactionID % (Integer.MAX_VALUE)), name
                            });

                    if (result == null) {
                        return null;
                    } else {
                        return ((Map<String, Object>) result);
                    }

                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    GuiUtils.noAlertError(TAG, e);
                    lastErrorMessage = e.getMessage();
                }
                return null;
            }
        };
        return retryTaskAfterLogin(task);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> orderForMultipleProductsCreate(final Object[] productsArray) {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {
                    Object result = client.call(API_CALL, sessionId,
                            API_CART_CREATE_ORDER_FOR_MULTIPLE_PRODUCTS, new Object[] {
                                productsArray
                            });

                    if (result == null) {
                        return null;
                    } else {
                        return ((Map<String, Object>) result);
                    }

                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    GuiUtils.noAlertError(TAG, e);
                    lastErrorMessage = e.getMessage();
                }
                return null;
            }
        };
        return retryTaskAfterLogin(task);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> shipmentCreate(final String orderIncrementId,
            final String carrierCode, final String title,
            final String trackingNumber, final Map<String, Object> params) {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {
                    Object result = client.call(API_CALL, sessionId,
                            API_ORDER_SHIPMENT_CREATE, new Object[] {
                                    orderIncrementId,
                                    carrierCode, title, trackingNumber, params
                            });

                    if (result == null) {
                        return null;
                    } else {
                        return ((Map<String, Object>) result);
                    }

                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    GuiUtils.noAlertError(TAG, e);
                    lastErrorMessage = e.getMessage();
                }
                return null;
            }
        };
        return retryTaskAfterLogin(task);
    }

    /**
	 * 
	 */
    public boolean validateCustomer() {
        final MagentoClientTask<Boolean> task = new MagentoClientTask<Boolean>() {
            @Override
            public Boolean run() throws RetryAfterLoginException {

                try {
                    // Get Customer Info
                    Map<String, Object> custID = new HashMap<String, Object>();
                    custID.put("customer_id", settingsSnapshot.getUser());

                    Object[] customerInfo = (Object[]) client.call(API_CALL, sessionId,
                            API_CUSTOMER_LIST,
                            new Object[] {
                                custID
                            });

                    // if the Array is empty
                    // then Return False
                    if (customerInfo.length > 0)
                        return true;
                    else
                        return false;

                } catch (XMLRPCFault e) {
                    // Check if Fault Code is Customer is not exist
                    if (e.getFaultCode() == 102) {
                        return false;
                    } else
                        throw new RetryAfterLoginException(e);

                } catch (Throwable e) {
                    GuiUtils.noAlertError(TAG, e);
                    lastErrorMessage = e.getMessage();
                }
                return false;
            }
        };

        return retryTaskAfterLogin(task);
    }

    /**
     * Get Product Information using SKU
     * 
     * @param productId
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> catalogProductInfoBySKU(final String productSKU,
            final boolean searchByBarcode) {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {
                    /*
                     * Object resultObj = client.call("call", sessionId,
                     * "catalog_product.fullInfo", new Object[] { null,
                     * productSKU, searchByBarcode });
                     */

                    Object resultObj;

                    if (searchByBarcode)
                    {
                        resultObj = client.call(API_CALL, sessionId, API_PRODUCT_INFO,
                                new Object[] {
                                    productSKU
                                });
                    }
                    else
                    {
                        resultObj = client.call(API_CALL, sessionId, API_PRODUCT_INFO,
                                new Object[] {
                                        productSKU, "sku"
                                });
                    }

                    final Map<String, Object> result = (Map<String, Object>) resultObj;

                    return result;
                } catch (XMLRPCFault e) {
                    lastErrorCode = e.getFaultCode();
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorCode = 0;
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    /**
     * @param imageInfo
     * @param sku
     * @param index
     * @return
     */
    public Map<String, Object> uploadImage(final Map<String, Object> imageInfo, final String pid,
            final ImageStreaming.StreamUploadCallback callback) {
        final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {

            @SuppressWarnings("unchecked")
            @Override
            public Map<String, Object> run() throws RetryAfterLoginException {
                try {
                    Map<String, Object> productMap = null;

                    // Prepare Image Info to be saved
                    Map<String, Object> data = new HashMap<String, Object>();
                    data.put("file", imageInfo);
                    data.put("exclude", 0);

                    URI uri = URI.create(settingsSnapshot.getUrl());

                    if (ensureLoggedIn()) {
                        // Add Image
                        // client.call("call", sessionId,
                        // "product_media.create ", new Object[] { sku, data});
                        productMap = (Map<String, Object>) ImageStreaming.streamUpload(uri.toURL(),
                                API_CALL, sessionId,
                                API_PRODUCT_ATTRIBUTE_MEDIA_CREATE,
                                new Object[] {
                                        pid, data
                                },
                                callback, client);
                    }
                    return productMap; /*
                                        * Should return a product here (not a
                                        * string)
                                        */
                } catch (XMLRPCFault e) {
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    public boolean deleteProduct(final String sku) {
        final MagentoClientTask<Boolean> task = new MagentoClientTask<Boolean>() {
            @Override
            public Boolean run() throws RetryAfterLoginException {
                try {
                    boolean result = false;
                    if (ensureLoggedIn()) {
                        Object resultObject = client.call(API_CALL, sessionId, API_PRODUCT_DELETE,
                                new Object[] {
                                    sku
                                });
                        result = resultObject == null ? false : Boolean.getBoolean(resultObject
                                .toString());
                    }
                    return result;

                } catch (XMLRPCFault e) {
                    lastErrorCode = e.getFaultCode();
                    throw new RetryAfterLoginException(e);
                } catch (Throwable e) {
                    lastErrorCode = 0;
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        Boolean result = retryTaskAfterLogin(task);
        return result == null ? false : result;

    }

    public Object[] getProfilesList() {
        final MagentoClientTask<Object[]> task = new MagentoClientTask<Object[]>() {
            @Override
            public Object[] run() throws RetryAfterLoginException {
                try {
                    Object[] result = null;
                    if (ensureLoggedIn()) {
                        Object resultObject = client.call(API_CALL, sessionId,
                                API_PROFILE_LIST,
                                new Object[] {});
                        result = (Object[]) resultObject;
                    }
                    return result;

                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    public String executeProfile(final String profileID) {
        final MagentoClientTask<String> task = new MagentoClientTask<String>() {
            @Override
            public String run() throws RetryAfterLoginException {
                try {
                    String result = null;
                    if (ensureLoggedIn()) {
                        Object resultObject = client.call(API_CALL, sessionId,
                                API_PROFILE_EXECUTE,
                                new Object[] {
                                    profileID
                                });
                        result = (String) resultObject;
                    }
                    return result;

                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);
    }

    /**
     * Get Images List
     * 
     * @param productID
     * @return
     */
    public Object[] getImagesList(final String productID) {

        final MagentoClientTask<Object[]> task = new MagentoClientTask<Object[]>() {
            @Override
            public Object[] run() throws RetryAfterLoginException {
                try {
                    Object[] imagesList = null;
                    if (ensureLoggedIn()) {
                        imagesList = (Object[]) client.call(API_CALL, sessionId,
                                API_CATALOG_PRODUCT_ATTRIBUTE_MEDIA_LIST,
                                new Object[] {
                                    productID
                                });
                    }
                    return imagesList;

                } catch (Throwable e) {
                    lastErrorMessage = e.getMessage();
                    throw new RuntimeException(e);
                }
            }
        };
        return retryTaskAfterLogin(task);

    }

}
