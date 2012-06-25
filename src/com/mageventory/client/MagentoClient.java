package com.mageventory.client;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.R.attr;
import android.R.integer;
import android.net.ParseException;
import android.text.TextUtils;
import android.widget.Toast;

import com.mageventory.MageventoryConstants;
import com.mageventory.model.Product;
import com.mageventory.util.UrlBuilder;
import com.mageventory.xmlrpc.XMLRPCClient;
import com.mageventory.xmlrpc.XMLRPCException;
import com.mageventory.xmlrpc.XMLRPCFault;

/**
 * Magento remote API client. This class is not thread-safe. It's purpose is to
 * replace the old one by providing stronger encapsulation and loose coupling
 * between the callers and the service.
 * 
 * @author Yordan Mildainov <jordanMiladinov@gmail.com>
 * 
 */
public class MagentoClient implements MageventoryConstants {

	private static String prepareForLikeQuery(String like) {
		if (like.startsWith("%") == false) {
			like = "%".concat(like);
		}
		if (like.endsWith("%") == false) {
			like = like.concat("%");
		}
		return like;
	}

	private static String repairServiceUrl(String url) {
		if (url.startsWith("http://") == false) {
			url = "http://".concat(url);
		}
		if (url.endsWith(XMLRPC_PATH) == false) {
			url = UrlBuilder.join(url, XMLRPC_PATH);
		}
		return url;
	}

	private final XMLRPCClient client;
	// private final XmlRpcClientConfigImpl config;
	// private final XmlRpcClient client;

	/**
	 * Holds the login key.
	 */
	private final String key;

	/**
	 * Holds the last error message. Error messages are recorded when an
	 * operation fails.
	 */
	private String lastErrorMessage;
	
	/**
	 * Holds the last error code*/
	private int lastErrorCode;

	/**
	 * Holds the Magento XMLRPC service URL.
	 */
	private final String serviceUrl;

	/**
	 * Session id. Retrieved by calling login.
	 */
	private String sessionId = null;

	/**
	 * Holds the login user.
	 */
	private final String user;

	public MagentoClient(String url, final String user, final String key) throws MalformedURLException {
		super();
		url = repairServiceUrl(url);
		new URL(url); // check if URL is OK, throw exception if not
		this.serviceUrl = repairServiceUrl(url);
		this.user = user;
		this.key = key;
		client = new XMLRPCClient(serviceUrl);
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
					final Object resultObj = client.call("call", sessionId, "category_attribute.list");
					final Object[] objs = (Object[]) resultObj;
					if (objs == null) {
						return null;
					}
					final List<Map<String, Object>> attrs = new ArrayList<Map<String, Object>>(objs.length);
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
					final Object[] resultObj = (Object[]) client.call("call", sessionId,
							"catalog_product_attribute_set.fullInfoList");
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

	public Map<String, Object> productAttributeAddOption(final String attributeCode, final String optionLabel) {
		final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {

			@Override
			@SuppressWarnings("unchecked")
			public Map<String, Object> run() throws RetryAfterLoginException {
				try {
					final Map<String, Object> resultObj = (Map<String, Object>) client.call("call", sessionId,
							"catalog_product_attribute.addOptionAndReturnInfo", new Object[] { attributeCode,
									optionLabel });

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
					final Object resultObj = client.call("call", sessionId, "catalog_product_attribute_set.list");
					final Object[] objs = (Object[]) resultObj;
					if (objs == null) {
						return null;
					}
					final List<Map<String, Object>> attrs = new ArrayList<Map<String, Object>>(objs.length);
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

					final Object[] resultObj = (Object[]) client.call("call", sessionId,
							"catalog_product_attribute_set.fullInfoList");

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
					final List<Map<String, Object>> attrs = new ArrayList<Map<String, Object>>(objs.length);
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
					Object resultObj = client.call("call", sessionId, "catalog_product.fullInfo",
							new Object[] { productId });
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
					Object result = client.call("call", sessionId, "catalog_product_attribute_media.list",
							new Object[] { productId });
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
					Object result = client.call("call", sessionId, "catalog_category.tree");
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
					final Map<String, Object> category = (Map<String, Object>) client.call("call", sessionId,
							"catalog_category.info", new Object[] { categoryId });
					return category;
				} catch (XMLRPCFault e) {
					throw new RetryAfterLoginException(e);
				} catch (Throwable e) {
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
	public Boolean catalogProductAttributeMediaUpdate(final String productID, final String imageName,
		final Map<String, Object> imageData) {
		final MagentoClientTask<Boolean> task = new MagentoClientTask<Boolean>() {
			@Override
			public Boolean run() throws RetryAfterLoginException {
				try {
					Boolean result = (Boolean) client.call("call", sessionId, "catalog_product_attribute_media.update",
						new Object[] { productID, imageName, imageData });
					return result;
				} catch (XMLRPCFault e) {
					throw new RetryAfterLoginException(e);
				} catch (Throwable e) {
					lastErrorMessage = e.getMessage();
				}
				return null;
			}
		};
		return retryTaskAfterLogin(task);	
	}
	
	/**
	 * Remove an image from the server. 
	 */
	public Boolean catalogProductAttributeMediaRemove(final String productID, final String imageName) {
		final MagentoClientTask<Boolean> task = new MagentoClientTask<Boolean>() {
			@Override
			public Boolean run() throws RetryAfterLoginException {
				try {
					Boolean result = (Boolean) client.call("call", sessionId, "catalog_product_attribute_media.remove",
						new Object[] { productID, imageName });
					return result;
				} catch (XMLRPCFault e) {
					throw new RetryAfterLoginException(e);
				} catch (Throwable e) {
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
	 * 
	 * @return
	 */
	public List<Map<String, Object>> catalogProductList(final String filter, final int categoryID) {
		final MagentoClientTask<List<Map<String, Object>>> task = new MagentoClientTask<List<Map<String, Object>>>() {
			@Override
			@SuppressWarnings("unchecked")
			public List<Map<String, Object>> run() throws RetryAfterLoginException {
				try {
					final Object[] products;

					if (categoryID != INVALID_CATEGORY_ID) {
						products = (Object[]) ((Map) client.call("call", sessionId, "catalog_product.limitedList",
								new Object[] { null, categoryID })).get("items");
					} else {
						products = (Object[]) ((Map) client.call("call", sessionId, "catalog_product.limitedList",
								new Object[] { filter })).get("items");
					}

					final List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(products.length);
					for (Object product : products) {
						result.add((Map<String, Object>) product);
					}
					return result;
				} catch (XMLRPCFault e) {
					throw new RetryAfterLoginException(e);
				} catch (Throwable e) {
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
					Boolean success = (Boolean) client.call("call", sessionId, "catalog_product.update", new Object[] {
							productId, productData });

					if (success) {
						final String[] invKeys = { MAGEKEY_PRODUCT_QUANTITY, MAGEKEY_PRODUCT_MANAGE_INVENTORY,
								MAGEKEY_PRODUCT_IS_IN_STOCK, };
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
							success = (Boolean) client.call("call", sessionId, "product_stock.update", new Object[] {
									productId, invInfo });
						}
					}

					return success == null || success == false ? Boolean.FALSE : Boolean.TRUE;
				} catch (XMLRPCFault e) {
					throw new RetryAfterLoginException(e);
				} catch (Throwable e) {
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
	public Map<String, Object> catalogProductCreate(final String productType, final int attrSetId, final String sku,
			final Map<String, Object> productData) {
		final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
			@Override
			public Map<String, Object> run() throws RetryAfterLoginException {
				try {
					final String[] invKeys = { MAGEKEY_PRODUCT_QUANTITY, MAGEKEY_PRODUCT_MANAGE_INVENTORY,
							MAGEKEY_PRODUCT_IS_IN_STOCK, };
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

					@SuppressWarnings("unchecked")
					Map<String, Object> productMap = (Map<String, Object>) client.call("call", sessionId,
							"catalog_product.createAndReturnInfo",
							new Object[] { productType, String.valueOf(attrSetId), sku, productData });

					return productMap;
				} catch (XMLRPCFault e) {
					throw new RetryAfterLoginException(e);
				} catch (Throwable e) {
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
			sessionId = (String) client.call("login", user, key);
			return true;
		} catch (Throwable e) {
			sessionId = null;
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
			lastErrorMessage = e.getMessage();
			return null;
		}
		if (login() == false) {
			return null;
		}
		try {
			return task.run();
		} catch (Throwable e) {
			lastErrorMessage = e.getMessage();
			return null;
		}
	}

	@Override
	public String toString() {
		return "MagentoClient2 [serviceUrl=" + serviceUrl + ", user=" + user + "]";
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

	/**
	 * Create ORDER return 0 on Success return -1 on failure
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> orderCreate(final Map<String, Object> productData) {
		final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
			@Override
			public Map<String, Object> run() throws RetryAfterLoginException {
				try {

					String sku = productData.get(MAGEKEY_PRODUCT_SKU).toString();
					float price = Float.valueOf(productData.get(MAGEKEY_PRODUCT_PRICE).toString());
					float quantity = Float.valueOf(productData.get(MAGEKEY_PRODUCT_QUANTITY).toString());
					int customerID = Integer.valueOf(user);
					long transactionID = Long.valueOf(productData.get(MAGEKEY_PRODUCT_TRANSACTION_ID).toString()); 
					String name = productData.get(MAGEKEY_PRODUCT_NAME).toString();
					Object result = client.call("call", sessionId, "cart.createOrderForProduct", new Object[] { sku,
							price, quantity, customerID, (int)transactionID, name });

					if (result == null) {
						return null;
					} else {
						return ((Map<String, Object>) result);
					}

				} catch (XMLRPCFault e) {
					throw new RetryAfterLoginException(e);
				} catch (Throwable e) {
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
					custID.put("customer_id", user);

					Object[] customerInfo = (Object[]) client.call("call", sessionId, "customer.list",
							new Object[] { custID });

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
	public Map<String, Object> catalogProductInfoBySKU(final String productSKU) {
		final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
			@Override
			public Map<String, Object> run() throws RetryAfterLoginException {
				try {
					Object resultObj = client.call("call", sessionId, "catalog_product.fullInfo", new Object[] { null,
							productSKU });
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
	 * 
	 * @param imageInfo
	 * @param sku
	 * @param index
	 * @return
	 */
	public Map<String, Object> uploadImage(final Map<String, Object> imageInfo, final String pid,
			final boolean makeMain, final ImageStreaming.StreamUploadCallback callback) {
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

					if (makeMain == true) {
						data.put("types", new Object[] { "image", "small_image", "thumbnail" });
					}

					URI uri = URI.create(MagentoClient.this.serviceUrl);

					if (ensureLoggedIn()) {
						// Add Image
						// client.call("call", sessionId,
						// "product_media.create ", new Object[] { sku, data});
						productMap = (Map<String, Object>) ImageStreaming.streamUpload(uri.toURL(), "call", sessionId,
								"catalog_product_attribute_media.createAndReturnInfo", new Object[] { pid, data },
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
						Object resultObject = client.call("call", sessionId, "product.delete", new Object[] { sku });
						result = Boolean.getBoolean(resultObject.toString());
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
						imagesList = (Object[]) client.call("call", sessionId, "catalog_product_attribute_media.list",
								new Object[] { productID });
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
