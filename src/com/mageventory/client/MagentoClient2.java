package com.mageventory.client;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.R.integer;
import android.net.ParseException;
import android.text.TextUtils;
import android.widget.Toast;

import com.mageventory.MageventoryConstants;
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
	
	public List<Map<String, Object>> categoryAttributeList() {
		final MagentoClientTask<List<Map<String, Object>>> task = new MagentoClientTask<List<Map<String,Object>>>() {
			
			@Override
			@SuppressWarnings("unchecked")
			public List<Map<String, Object>> run() throws RetryAfterLoginException {
				try {
					final Object resultObj = client.call("call", sessionId, "category_attribute.list");
					final Object[] objs = (Object[]) resultObj;
					if (objs == null) {
						return null;
					}
					final List<Map<String, Object>> attrs = new ArrayList<Map<String,Object>>(objs.length);
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
	
	public List<Map<String, Object>> catalogProductAttributeSetList() {
		final MagentoClientTask<List<Map<String, Object>>> task = new MagentoClientTask<List<Map<String,Object>>>() {
			
			@Override
			@SuppressWarnings("unchecked")
			public List<Map<String, Object>> run() throws RetryAfterLoginException {
				try {
					final Object resultObj = client.call("call", sessionId, "catalog_product_attribute_set.list");
					final Object[] objs = (Object[]) resultObj;
					if (objs == null) {
						return null;
					}
					final List<Map<String, Object>> attrs = new ArrayList<Map<String,Object>>(objs.length);
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
	
	public List<Map<String, Object>> productAttributeOptions(final int attributeId) {
		final MagentoClientTask<List<Map<String, Object>>> task = new MagentoClientTask<List<Map<String,Object>>>() {
			@Override
			@SuppressWarnings("unchecked")
            public List<Map<String, Object>> run() throws RetryAfterLoginException {
				try {
					final Object resultObj = client.call("call", sessionId, "product_attribute.options",
							new Object[] { /* array("attribute_id", */ attributeId /* ) */ });
					final Object[] objs = (Object[]) resultObj;
					final List<Map<String, Object>> opts = new ArrayList<Map<String,Object>>(objs.length);
					for (final Object obj : objs) {
						opts.add((Map<String, Object>) obj);
					}
					return opts;
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
		final MagentoClientTask<List<Map<String, Object>>> task = new MagentoClientTask<List<Map<String,Object>>>() {
			
            @Override
            @SuppressWarnings("unchecked")
			public List<Map<String, Object>> run() throws RetryAfterLoginException {
				try {
					final Object resultObj = client.call("call", sessionId, "product_attribute.list",
					        new Object[] { setId });
					final Object[] objs = (Object[]) resultObj;
					if (objs == null) {
						return null;
					}
					final List<Map<String, Object>> attrs = new ArrayList<Map<String,Object>>(objs.length);
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
					Object resultObj = client.call("call", sessionId, "catalog_product.info", new Object[] { productId });
					final Map<String, Object> result = (Map<String, Object>) resultObj;					

					// Get Stock Information for the product
					Object quantResult = client.call("call", sessionId, "product_stock.list", new Object[] { productId });
					Map<String, Object> subResult = (Map<String, Object>) ((Object []) quantResult)[0];					
					result.putAll(subResult);					
					
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
	
	public boolean catalogProductUpdate(final int productId, final Map<String, Object> productData) {
		final MagentoClientTask<Boolean> task = new MagentoClientTask<Boolean>() {
			@Override
            public Boolean run() throws RetryAfterLoginException {
				try {
					final Boolean success = (Boolean) client.call("call", sessionId, "catalog_product.update",
					        new Object[] { productId, productData });
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
						// Update Inventory Information
						// TODO y: make this a constant
						final String[] invKeys = {
								MAGEKEY_PRODUCT_QUANTITY,
								MAGEKEY_PRODUCT_MANAGE_INVENTORY,
								MAGEKEY_PRODUCT_IS_IN_STOCK,
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
							client.call("call", sessionId, "product_stock.update", new Object[] {sku, invInfo} );
						}
						return Integer.parseInt(insertedPid);						
					}
				} catch (XMLRPCFault e) {
					throw new RetryAfterLoginException(e);
				} catch (Throwable e) {
					lastErrorMessage = e.getMessage();
				}
				return INVALID_PRODUCT_ID;
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

	
	/**
	 *  Create ORDER
	 *  return 0 on Success 
	 *  return -1 on failure
	 */
	@SuppressWarnings("unchecked")
	public Map<String,Object> orderCreate(final Map<String, Object> productData) {
		final MagentoClientTask<Map<String,Object>> task = new MagentoClientTask<Map<String,Object>>() {
			@Override
			public Map<String,Object> run() throws RetryAfterLoginException {
				try {
					
					String sku = productData.get(MAGEKEY_PRODUCT_SKU).toString();
					int price = Integer.valueOf(productData.get(MAGEKEY_PRODUCT_PRICE).toString());
					int quantity = Integer.valueOf(productData.get(MAGEKEY_PRODUCT_QUANTITY).toString());
					int customerID = Integer.valueOf(user);					
					String description = productData.get(MAGEKEY_PRODUCT_DESCRIPTION).toString();
					Object result = client.call("call",sessionId,"cart.createOrderForProduct",new Object[]{sku,price,quantity,customerID,description});
					
					if(result == null)
					{
						return null;
					}
					else
					{
						return ((Map<String,Object>)result);
					}
					
				} 
				catch (XMLRPCFault e) {
					throw new RetryAfterLoginException(e);
				} 
				catch (Throwable e) {
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
	public boolean validateCustomer()
	{
		final MagentoClientTask<Boolean> task = new MagentoClientTask<Boolean>() {
			@Override
			public Boolean run() throws RetryAfterLoginException {
	
				try {
					// Get Customer Info
					Map<String,Object>custID = new HashMap<String, Object>();
					custID.put("customer_id", user);
					
					Object [] customerInfo = (Object []) client.call("call", sessionId, "customer.list", new Object[]{custID});
					
					// if the Array is empty 
					// then Return False
					if(customerInfo.length > 0)
						return true;
					else
						return false;
								
				} catch (XMLRPCFault e) {
					// Check if Fault Code is Customer is not exist
					if(e.getFaultCode() == 102)
					{
						return false;
					}
					else
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
	 * @param productId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> catalogProductInfoBySKU(final String productSKU) {
		final MagentoClientTask<Map<String, Object>> task = new MagentoClientTask<Map<String, Object>>() {
			@Override
			public Map<String, Object> run() throws RetryAfterLoginException {
				try {
					Object resultObj = client.call("call", sessionId, "catalog_product.info", new Object[] { productSKU });
					final Map<String, Object> result = (Map<String, Object>) resultObj;					

					// Get Stock Information for the product
					Object quantResult = client.call("call", sessionId, "product_stock.list", new Object[] { productSKU });
					Map<String, Object> subResult = (Map<String, Object>) ((Object []) quantResult)[0];					
					result.putAll(subResult);					
					
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
	 * 
	 * @param imageInfo
	 * @param sku
	 * @param index
	 * @return
	 */
	 public String uploadImage(final Map<String,Object> imageInfo,final String sku,final int index)
	{
		final MagentoClientTask<String> task = new MagentoClientTask<String>() {
			@Override
			public String run() throws RetryAfterLoginException {
				try 
				{
					String imageFileName = "";
					
					// Prepare Image Info to be saved
					Map<String, Object> data = new HashMap<String, Object>(); 
					data.put("file", imageInfo);
					data.put(MAGEKEY_PRODUCT_IMAGE_POSITION, index);
					data.put("exclude", 0);
					
					if(index == 0)
					{
						// make first image as main image on server
						data.put("types", new Object[]{"image", "small_image", "thumbnail"});
					}
					
					URI uri = URI.create(MagentoClient2.this.serviceUrl);
					
					if(ensureLoggedIn()) {
					    // Add Image 
					    // client.call("call", sessionId, "product_media.create ", new Object[] { sku, data});
                        imageFileName = (String) ImageStreaming.streamUpload(uri.toURL(), "call", sessionId,
                                "catalog_product_attribute_media.create", new Object[] { sku, data });
					}
					return imageFileName;
					
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


	 public boolean deleteProduct(final String sku)
	 {
			final MagentoClientTask<Boolean> task = new MagentoClientTask<Boolean>() {
				@Override
				public Boolean run() throws RetryAfterLoginException {
					try 
					{
						boolean result = false;
						if(ensureLoggedIn())
						{
							Object resultObject = client.call("call",sessionId,"product.delete",new Object[]{sku});	
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
    
}
