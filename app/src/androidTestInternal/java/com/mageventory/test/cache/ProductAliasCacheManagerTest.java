package com.mageventory.test.cache;

import java.util.HashMap;
import java.util.Map;

import android.test.InstrumentationTestCase;

import com.mageventory.MageventoryConstants;
import com.mageventory.cache.ProductAliasCacheManager;
import com.mageventory.model.Product;

/**
 * Tests for {@link ProductAliasCacheManager}
 * 
 * @author Eugene Popovich
 */
public class ProductAliasCacheManagerTest extends InstrumentationTestCase
		implements MageventoryConstants
{

	public Product getTestProduct(String id, String sku, String barCode)
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(Product.MAGEKEY_PRODUCT_ID, id);
		map.put(Product.MAGEKEY_PRODUCT_SKU, sku);
		map.put(Product.MAGEKEY_PRODUCT_IMAGES, new Object[0]);

		Object[] local_attrInfo = new Object[1];
		map.put("set_attributes", local_attrInfo);
		Map<String, Object> local_attr = new HashMap<String, Object>();
		local_attrInfo[0] = local_attr;
		local_attr.put(Product.MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE, "");
		Object[] labels = new Object[1];
		local_attr.put("frontend_label", labels);
		Map<String, Object> local_label = new HashMap<String, Object>();
		labels[0] = local_label;
		local_label.put("store_id", "0");
		local_label.put("label", "Barcode");
		local_attr.put("frontend_input", "");
		local_attr.put(Product.MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE,
				MAGEKEY_PRODUCT_BARCODE);
		map.put(MAGEKEY_PRODUCT_BARCODE, barCode);

		local_attr.put(Product.MAGEKEY_ATTRIBUTE_OPTIONS, new Object[0]);
		Product product = new Product(map);

		assertEquals(product.getSku(), sku);
		assertEquals(product.getId(), id);
		assertEquals(product.getBarcode(null),
				barCode);
		return product;
	}

	public void testProductCreate()
	{
		getTestProduct("123", "456", "123456789");
	}

	public void testAddOrUpdate()
	{
		String id = "123";
		String sku = "546";
		String barCode = "32434123411234";
		String profileUrl = "http://www.google.com'\"&;=";
		String profileUrl2 = "http://www2.google.com";
		ProductAliasCacheManager manager = new ProductAliasCacheManager(
				getInstrumentation().getTargetContext());
		manager.addOrUpdate(getTestProduct(id, sku, barCode), profileUrl);

		assertNull(manager.getCachedSkuForBarcode(barCode, profileUrl2));
		String dbSku = manager.getCachedSkuForBarcode(barCode, profileUrl);
		assertEquals(sku, dbSku);

		barCode = "32434123411234x";
		manager.addOrUpdate(getTestProduct(id, sku, barCode), profileUrl);

		assertNull(manager.getCachedSkuForBarcode(barCode, profileUrl2));
		dbSku = manager.getCachedSkuForBarcode(barCode, profileUrl);
		assertEquals(sku, dbSku);
		
		assertTrue(manager.deleteProductFromCache(
				getTestProduct(id, sku, barCode), profileUrl));
		assertNull(manager.getCachedSkuForBarcode(barCode, profileUrl));

	}

	public void testDeleteProductsFromCache()
	{
		String id = "123";
		String id2 = "123x";
		String sku = "546";
		String sku2 = "546";
		String barCode = "32434123411234";
		String barCode2 = "32434123411234x";
		String profileUrl = "http://www.google.com'\"&;=";
		String profileUrl2 = "http://www.google.com'\"&;=s";
		ProductAliasCacheManager manager = new ProductAliasCacheManager(
				getInstrumentation().getTargetContext());
		manager.addOrUpdate(getTestProduct(id, sku, barCode), profileUrl);
		manager.addOrUpdate(getTestProduct(id2, sku2, barCode2), profileUrl2);

		String dbSku = manager.getCachedSkuForBarcode(barCode, profileUrl);
		assertEquals(sku, dbSku);
		dbSku = manager.getCachedSkuForBarcode(barCode2, profileUrl2);
		assertEquals(sku2, dbSku);

		barCode = "32434123411234x";
		sku = "546x";
		barCode2 = "32434123411234xx";
		sku2 = "546x";
		manager.addOrUpdate(getTestProduct(id, sku, barCode), profileUrl);
		manager.addOrUpdate(getTestProduct(id2, sku2, barCode2), profileUrl2);

		dbSku = manager.getCachedSkuForBarcode(barCode, profileUrl);
		assertEquals(sku, dbSku);
		dbSku = manager.getCachedSkuForBarcode(barCode2, profileUrl2);
		assertEquals(sku2, dbSku);

		assertTrue(manager.deleteProductsFromCache(profileUrl));
		assertNull(manager.getCachedSkuForBarcode(barCode, profileUrl));
		dbSku = manager.getCachedSkuForBarcode(barCode2, profileUrl2);
		assertEquals(sku2, dbSku);
		assertTrue(manager.deleteProductsFromCache(profileUrl2));
		assertNull(manager.getCachedSkuForBarcode(barCode, profileUrl));
		assertNull(manager.getCachedSkuForBarcode(barCode, profileUrl2));
	}

	public void testUpdateSkuIfExists()
	{
		String id = "123";
		String sku = "546";
		String sku2 = "546x";
		String barCode = "32434123411234";
		String profileUrl = "http://www.google.com'\"&;=";
		ProductAliasCacheManager manager = new ProductAliasCacheManager(
				getInstrumentation().getTargetContext());
		assertFalse(manager.updateSkuIfExists(sku, sku2, profileUrl));
		String dbSku;

		dbSku = manager.getCachedSkuForBarcode(barCode, profileUrl);
		assertNull(dbSku);

		manager.addOrUpdate(getTestProduct(id, sku, barCode), profileUrl);
		dbSku = manager.getCachedSkuForBarcode(barCode, profileUrl);
		assertEquals(sku, dbSku);

		assertTrue(manager.updateSkuIfExists(sku, sku2, profileUrl));
		dbSku = manager.getCachedSkuForBarcode(barCode, profileUrl);
		assertEquals(sku2, dbSku);

		assertTrue(manager.deleteProductsFromCache(profileUrl));
	}
}
