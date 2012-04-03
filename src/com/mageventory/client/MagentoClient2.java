package com.mageventory.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.util.UrlBuilder;
import com.mageventory.xmlrpc.XMLRPCClient;
import com.mageventory.xmlrpc.XMLRPCFault;

/**
 * Magento remote API client. This class is not thread-safe. It's purpose is to
 * replace the old one by providing stronger encapsulation and loose coupling
 * between the callers and the service.
 * 
 * @author Yordan Mildainov <jordanMiladinov@gmail.com>
 * 
 */
public class MagentoClient2 implements MageventoryConstants {

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

	public MagentoClient2(String url, final String user, final String key) throws MalformedURLException {
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

	@SuppressWarnings("unchecked")
	public Map<String, Object> catalogProductInfo(final int productId) {
		final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
			@Override
			public Map<String, Object> run() throws RetryAfterLoginException {
				try {
					Object result = client.call("call", sessionId, "catalog_product.info", new Object[] { productId });
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

	public List<Map<String, Object>> catalogProductList() {
		return catalogProductList("");
	}

	private List<Map<String, Object>> catalogProductList(final Map<String, Object> filter) {
		final MagentoClientTask<List<Map<String, Object>>> task = new MagentoClientTask<List<Map<String, Object>>>() {
			@Override
			@SuppressWarnings("unchecked")
			public List<Map<String, Object>> run() throws RetryAfterLoginException {
				try {
					final Object[] products;
					if (filter != null) {
						products = (Object[]) client.call("call", sessionId, "catalog_product.list",
								new Object[] { filter });
					} else {
						products = (Object[]) client.call("call", sessionId, "catalog_product.list");
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
	 * 
	 * @param categoryId
	 * @param name
	 * @return
	 */
	public List<Map<String, Object>> catalogCategoryAssignedProducts(final Integer categoryId) {
		if (categoryId == null || categoryId == INVALID_CATEGORY_ID) {
			return null;
		}

		Map<String, Object> filter = new HashMap<String, Object>();
		addCategoryIdFilter(filter, categoryId);

		if (filter.isEmpty()) {
			filter = null;
		}

		final List<Map<String, Object>> categoryProducts = catalogCategoryAssignedProducts(filter);
		return categoryProducts;
	}

	/**
	 * Retrieve product list. Apply name filter.
	 * 
	 * @param @Nullable likeName
	 * 
	 * @return
	 */
	public List<Map<String, Object>> catalogProductList(final String name) {
		Map<String, Object> filter = new HashMap<String, Object>();
		addNameFilter(filter, name);
		// addSkuFilter(filter, sku);

		// if filter is empty, it's better to nullify it, as this way it'll be
		// ignored by the catalogProductList(filter) method
		if (filter.isEmpty()) {
			filter = null;
		}
		return catalogProductList(filter);
	}

	/**
	 * Return the id of the newly created product or -1 in case of error.
	 * 
	 * @param productData
	 * @return
	 */
	public int catalogProductCreate(final String productType, final int attrSetId, final String sku,
			final Map<String, Object> productData) {
		final MagentoClientTask<Integer> task = new MagentoClientTask<Integer>() {
			@Override
			public Integer run() throws RetryAfterLoginException {
				try {
					final String insertedPid = ""
							+ client.call("call", sessionId, "catalog_product.create", new Object[] { productType,
									String.valueOf(attrSetId), sku, productData });
					if (TextUtils.isDigitsOnly(insertedPid)) {
						return Integer.parseInt(insertedPid);
					}
				} catch (XMLRPCFault e) {
					throw new RetryAfterLoginException(e);
				} catch (Throwable e) {
					lastErrorMessage = e.getMessage();
				}
				return -1;
			}
		};
		return retryTaskAfterLogin(task);
	}

	private List<Map<String, Object>> catalogCategoryAssignedProducts(final Map<String, Object> filter) {
		final MagentoClientTask<List<Map<String, Object>>> task = new MagentoClientTask<List<Map<String, Object>>>() {
			@Override
			@SuppressWarnings("unchecked")
			public List<Map<String, Object>> run() throws RetryAfterLoginException {
				try {
					final Object[] products = (Object[]) client.call("call", sessionId,
							"catalog_category.assignedProducts", new Object[] { filter });
					final List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(products.length);
					for (Object productObj : products) {
						try {
							final Map<String, Object> product = (Map<String, Object>) productObj;
							final int productId = Integer.parseInt(product.get(MAGEKEY_PRODUCT_ID).toString());
							// TODO y: making a new query for each product is a
							// HUGE overhead... Optimize!
							final Map<String, Object> detailedProduct = catalogProductInfo(productId);
							result.add((Map<String, Object>) detailedProduct);
						} catch (Throwable e) {
						}
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

	public String getLastErrorMessage() {
		return lastErrorMessage;
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
			lastErrorMessage = e.getMessage();
			return false;
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

}
