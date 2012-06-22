package com.mageventory.job;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.os.Environment;
import android.text.TextUtils;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.Base64Coder_magento;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.model.Product;

/* Contains methods for performing operations on the cache. */
public class JobCacheManager {

	public static Object sSynchronizationObject = new Object();

	/* Returns true on success. */
	private static boolean serialize(Object o, File file) {
		FileOutputStream fos;
		ObjectOutputStream oos;
		try {
			fos = new FileOutputStream(file);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(o);
			oos.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	/* Returns something else than null on success */
	private static Object deserialize(File file) {
		Object out;
		FileInputStream fis;
		ObjectInputStream ois;

		try {
			fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);
			out = ois.readObject();
			ois.close();
		} catch (Exception e) {
			return null;
		}
		return out;
	}

	/* Return a unique hash for a given SKU. */
	private static String encodeSKU(String SKU) {
		return Base64Coder_magento.encodeString(SKU).replace("+", "_").replace("/", "-").replace("=", "");
	}
	
	/* Get a directory name for a given job type. */
	private static String getCachedJobSubdirName(int resourceType) {
		switch (resourceType) {
		case MageventoryConstants.RES_UPLOAD_IMAGE:
			return "UPLOAD_IMAGE";
		case MageventoryConstants.RES_CATALOG_PRODUCT_SELL:
			return "SELL";

		default:
			return null;
		}
	}

	/* Get a filename for a given job type (job is extracted from jobID). */
	private static String getCachedResourceFileName(JobID jobID) {
		switch (jobID.getJobType()) {
		case MageventoryConstants.RES_UPLOAD_IMAGE:
		case MageventoryConstants.RES_CATALOG_PRODUCT_SELL:
			return jobID.getTimeStamp() + ".obj";

		case MageventoryConstants.RES_CATALOG_PRODUCT_CREATE:
			return "new_prod.obj";
		case MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE:
			return "edit_prod.obj";

		default:
			return null;
		}
	}

	/* Return a directory where a given job resides. */
	private static File getDirectoryAssociatedWithJob(JobID jobID, boolean createDirectories) {
		File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		dir = new File(dir, encodeSKU(jobID.getSKU()));

		String subdir = getCachedJobSubdirName(jobID.getJobType());

		if (subdir != null) {
			dir = new File(dir, subdir);
		}

		if (createDirectories == true) {
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					return null;
				}
			}
		}

		return dir;
	}

	/*
	 * Return a file associated with a given job. It can be used for example to
	 * serialize a job in the right place.
	 */
	private static File getFileAssociatedWithJob(JobID jobID, boolean createDirectories) {
		File fileToSave = new File(getDirectoryAssociatedWithJob(jobID, createDirectories),
				getCachedResourceFileName(jobID));
		return fileToSave;
	}

	/* Return a file path associated with a given job. */
	public static String getFilePathAssociatedWithJob(JobID jobID) {
		synchronized (sSynchronizationObject) {
			return getFileAssociatedWithJob(jobID, false).getAbsolutePath();
		}
	}

	/* Save job in the cache.
	 * 
	 * There is a problem with storing a job in the cache. If multiple pieces of code were trying to do that without
	 * any coordination the state of such job could cause problems difficult to debug. This is why all code in the
	 * application should be following rules:
	 * 
	 * 1. The service and the queue have the absolute priority of restoring and storing jobs from/in the cache. No
	 * other code should interfere with them. When a job starts it is deserialized by the queue and passed to the service.
	 * The service is then allowed to store the job any number of times it wants (for example it's doing that every
	 * time upload progress changes). When the job is finished the job is either deleted or modified and stored
	 * back in the cache. When these things are happening no other code should store the job that is being processed.
	 * There is a way of checking whether a job is being processed: The JobQueue provides getCurrentJob() function
	 * which returns the job that the queue deserialized and passed to the service for processing. In order to store a
	 * job in the queue the interested code should first lock the sQueueSynchronizationObject which prevents the
	 * current job changes and then check the current job. If the current job is equal to the job which the calling code
	 * wants to store then it should either do it later or not at all.
	 * 
	 * 2. All pieces of code that want to restore, then modify, then store a job in the cache should lock the
	 * JobCacheManager.sSynchronizationObject before doing that to prevent a conflict with other pieces of code.
	 * */
	public static boolean store(Job job) {
		synchronized (sSynchronizationObject) {
			File fileToSave = getFileAssociatedWithJob(job.getJobID(), true);

			if (fileToSave != null && serialize(job, fileToSave) == true)
				return true;
			else
				return false;
		}
	}

	/* Load job from the cache. */
	public static Job restore(JobID jobID) {
		synchronized (sSynchronizationObject) {
			File fileToRead = getFileAssociatedWithJob(jobID, false);

			if (fileToRead == null)
				return null;
			else
				return (Job) deserialize(fileToRead);
		}
	}

	/* Remove job from cache. */
	public static void removeFromCache(JobID jobID) {
		synchronized (sSynchronizationObject) {
			File fileToRemove = getFileAssociatedWithJob(jobID, false);

			if (fileToRemove != null) {
				fileToRemove.delete();
			}
		}
	}

	public static File getImageUploadDirectory(String SKU) {
		synchronized (sSynchronizationObject) {
			return getDirectoryAssociatedWithJob(new JobID(-1, MageventoryConstants.RES_UPLOAD_IMAGE, SKU), true);
		}
	}

	public static File getSellDirectory(String SKU) {
		synchronized (sSynchronizationObject) {
			return getDirectoryAssociatedWithJob(new JobID(-1, MageventoryConstants.RES_CATALOG_PRODUCT_SELL, SKU),
					true);
		}
	}

	/* Load all upload jobs for a given SKU. */
	public static List<Job> restoreImageUploadJobs(String SKU) {
		synchronized (sSynchronizationObject) {
			File uploadDir = getImageUploadDirectory(SKU);
			List<Job> out = new ArrayList<Job>();

			if (uploadDir == null)
				return out;

			File[] jobFileList = uploadDir.listFiles();

			if (jobFileList != null) {
				for (int i = 0; i < jobFileList.length; i++) {
					Job job = (Job) deserialize(jobFileList[i]);
					if (job != null)
						out.add(job);
				}
			}

			return out;
		}
	}

	/* Load all sell jobs for a given SKU. */
	public static List<Job> restoreSellJobs(String SKU) {
		synchronized (sSynchronizationObject) {
			File sellDir = getSellDirectory(SKU);
			List<Job> out = new ArrayList<Job>();

			if (sellDir == null)
				return out;

			File[] jobFileList = sellDir.listFiles();

			if (jobFileList != null) {
				for (int i = 0; i < jobFileList.length; i++) {
					Job job = (Job) deserialize(jobFileList[i]);
					if (job != null)
						out.add(job);
				}
			}

			return out;
		}
	}
	
	/* Load edit job for a given SKU. */
	public static Job restoreEditJob(String SKU) {
		synchronized (sSynchronizationObject) {
			File file = getFileAssociatedWithJob(new JobID(-1, MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE, SKU),
					false);
			Job job = null;

			if (file.exists()) {
				job = (Job) deserialize(file);
			}

			return job;
		}
	}

	/* Load product creation job for a given SKU. */
	public static Job restoreProductCreationJob(String SKU) {
		synchronized (sSynchronizationObject) {
			File file = getFileAssociatedWithJob(new JobID(-1, MageventoryConstants.RES_CATALOG_PRODUCT_CREATE, SKU),
					false);
			Job job = null;

			if (file.exists()) {
				job = (Job) deserialize(file);
			}

			return job;
		}
	}

	/* ======================================================================== */
	/* Image download */
	/* ======================================================================== */

	public static File getImageFullPreviewDirectory(String SKU, boolean createIfNotExists) {
		synchronized (sSynchronizationObject) {
			File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
			dir = new File(dir, encodeSKU(SKU));
			dir = new File(dir, "DOWNLOAD_IMAGE_PREVIEW");

			if (createIfNotExists && !dir.exists()) {
				dir.mkdirs();
			}

			return dir;
		}
	}

	public static void clearImageFullPreviewDirectory(String SKU) {
		synchronized (sSynchronizationObject) {
			File dir = getImageFullPreviewDirectory(SKU, false);

			if (dir.exists()) {
				for (File child : dir.listFiles()) {
					child.delete();
				}

				dir.delete();
			}
		}
	}

	public static File getImageDownloadDirectory(String SKU, boolean createIfNotExists) {
		synchronized (sSynchronizationObject) {
			File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
			dir = new File(dir, encodeSKU(SKU));
			dir = new File(dir, "DOWNLOAD_IMAGE");

			if (createIfNotExists && !dir.exists()) {
				dir.mkdirs();
			}

			return dir;
		}
	}

	public static void clearImageDownloadDirectory(String SKU) {
		synchronized (sSynchronizationObject) {
			File dir = getImageDownloadDirectory(SKU, false);

			if (dir.exists()) {
				for (File child : dir.listFiles()) {
					child.delete();
				}

				dir.delete();
			}
		}
	}

	/* ======================================================================== */
	/* Product details data */
	/* ======================================================================== */

	private static File getProductDetailsFile(String SKU, boolean createDirectories) {
		File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		file = new File(file, encodeSKU(SKU));

		if (createDirectories == true) {
			if (!file.exists()) {
				file.mkdirs();
			}
		}

		return new File(file, "prod_dets.obj");
	}

	/* Store product detail in the cache but merge it with currently pending product edit job if any. */
	public static void storeProductDetailsWithMerge(Product product) {
		synchronized (JobQueue.sQueueSynchronizationObject) {
		synchronized (sSynchronizationObject) {
			if (product == null || product.getSku() == null) {
				return;
			}
			
			/* A reference pointing to a product which is going to be serialized at the end. */
			Product mergedProduct = product;
			
			Job existingEditJob = JobCacheManager.restoreEditJob(product.getSku());
			
			/* Check if an edit job exists in the pending table. */
			if (existingEditJob != null && existingEditJob.getPending() == true)
			{
				/* Product edit job exists we will do either one way or two way merge. */
				
				Product prodBeforeChanges = product.getCopy();
				
				Job currentJob = JobQueue.getCurrentJob();

				/* If this is true after next checks it means we will do the merge both ways (from product details
				 * to edit job and the other way around as well). If this will be false it means we just merge product
				 * edit job to product details. */
				boolean twoWayMerge = true;
				
				/* If the currently pending job is an edit job and if it has the same SKU that the product passed to this
				 * 	function we don't merge the product to the product edit job but we still do it the other way around. */
				if (currentJob!=null && currentJob.getJobType() == MageventoryConstants.RES_CATALOG_PRODUCT_UPDATE &&
						currentJob.getSKU().equals(product.getSku()))
				{
					twoWayMerge = false;
				}
				
				List<String> updatedKeys = (List<String>) existingEditJob.getExtraInfo(MageventoryConstants.EKEY_UPDATED_KEYS_LIST);
				
				for (String key : existingEditJob.getExtras().keySet())
				{
					/* This is a special key that is not sent to the server. We shouldn't merge the value of this key. */
					if (key.equals(MageventoryConstants.EKEY_UPDATED_KEYS_LIST))
						continue;
					
					if (updatedKeys.contains(key))
					{
						/* In case of keys that were updated by the user we copy the values from the edit job file to product details file. */
						
						/* We send a different key to the server than the one we get from the server in case of categories. */
						if (key.equals(MageventoryConstants.MAGEKEY_PRODUCT_CATEGORIES))
						{
							mergedProduct.getData().put(MageventoryConstants.MAGEKEY_PRODUCT_CATEGORY_IDS, existingEditJob.getExtraInfo(key));
						}
						else
						{
							mergedProduct.getData().put(key, existingEditJob.getExtraInfo(key));
						}
					}
					else
					{
						/* In case of keys that were not updated by the user we merge the values from the product details file to
						 * edit job file but only in case the product edit request is not being processed while we're doing that. */
						
						if (twoWayMerge)
						{
							/* We send a different key to the server than the one we get from the server in case of categories. */
							if (key.equals(MageventoryConstants.MAGEKEY_PRODUCT_CATEGORIES))
							{
								existingEditJob.putExtraInfo(key, mergedProduct.getData().get(MageventoryConstants.MAGEKEY_PRODUCT_CATEGORY_IDS));	
							}
							else
							{
								existingEditJob.putExtraInfo(key, mergedProduct.getData().get(key));	
							}
						}
					}
				}

				/* Store the merged edit job if we were in two way merge mode. */
				if (twoWayMerge)
				{
					store(existingEditJob);	
				}
				
				/* Reinitialize all fields in Product class with the new data from the map. */
				mergedProduct = new Product(mergedProduct.getData());
				
				/* Remember the copy of original product to easily roll back later on. */
				mergedProduct.setUnmergedProduct(prodBeforeChanges);
			}
			else
			{
				/* Edit job doesn't exist in the pending table. We don't do any merge. */
			}
			
			serialize(mergedProduct, getProductDetailsFile(product.getSku(), true));
		}
		}
	}
	
	public static void storeProductDetails(Product product) {
		synchronized (sSynchronizationObject) {
			if (product == null || product.getSku() == null) {
				return;
			}
			
			serialize(product, getProductDetailsFile(product.getSku(), true));
		}
	}
	
	/* Product details data can be merged with product edit job in two cases:
	 *  - when product details is being saved to the cache
	 *  - when edit job starts or finishes
	 *  
	 *  This function is handling the second case.
	 */
	public static void remergeProductDetailsWithEditJob(String sku)
	{
	synchronized (sSynchronizationObject) {

		Product product = restoreProductDetails(sku);
		
		if (product != null)
		{
			/* We want to merge with the unmerged version of the product details.
			 * If getUnmergedProduct() returns null it means no merge took place on this
			 * instance of product details. */
			if (product.getUnmergedProduct() != null)
			{
				JobCacheManager.storeProductDetailsWithMerge(product.getUnmergedProduct());
			}
			else
			{
				JobCacheManager.storeProductDetailsWithMerge(product);
			}
		}
	}
	}

	public static Product restoreProductDetails(String SKU) {
		synchronized (sSynchronizationObject) {
			return (Product) deserialize(getProductDetailsFile(SKU, false));
		}
	}

	public static void removeProductDetails(String SKU) {
		synchronized (sSynchronizationObject) {
			File f = getProductDetailsFile(SKU, false);

			if (f.exists()) {
				f.delete();
			}
		}
	}

	public static boolean productDetailsExist(String SKU) {
		return getProductDetailsFile(SKU, false).exists();
	}

	/* ======================================================================== */
	/* Product list data */
	/* ======================================================================== */

	private static File getProductListDir(boolean createIfNotExists) {
		File dir = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);

		dir = new File(dir, "product_lists");

		if (createIfNotExists && !dir.exists()) {
			dir.mkdirs();
		}

		return dir;
	}

	private static File getProductListFile(boolean createDirectories, String[] params) {
		File file = getProductListDir(createDirectories);

		StringBuilder fileName = new StringBuilder();

		fileName.append("product_list_");

		if (params.length >= 1 && params[0] != null) {
			fileName.append(params[0]);
		}

		fileName.append("_");

		if (params.length >= 2 && params[1] != null
				&& (Integer.parseInt(params[1]) != MageventoryConstants.INVALID_CATEGORY_ID)) {
			fileName.append(params[1]);
		}

		fileName.append(".obj");

		return new File(file, fileName.toString());
	}

	/* Params are in a form: {nameFilter, categoryId}. */
	public static void storeProductList(List<Map<String, Object>> productList, String[] params) {
		synchronized (sSynchronizationObject) {
			if (productList == null) {
				return;
			}
			serialize(productList, getProductListFile(true, params));
		}
	}

	public static List<Map<String, Object>> restoreProductList(String[] params) {
		synchronized (sSynchronizationObject) {
			return (List<Map<String, Object>>) deserialize(getProductListFile(false, params));
		}
	}

	public static void removeAllProductLists() {
		synchronized (sSynchronizationObject) {
			File dir = getProductListDir(false);

			if (dir.exists()) {
				for (File child : dir.listFiles()) {
					child.delete();
				}

				dir.delete();
			}
		}
	}

	public static boolean productListExist(String[] params) {
		return getProductListFile(false, params).exists();
	}

	/* ======================================================================== */
	/* Categories data */
	/* ======================================================================== */

	private static File getCategoriesFile(boolean createDirectories) {
		File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);

		if (createDirectories == true) {
			if (!file.exists()) {
				file.mkdirs();
			}
		}

		return new File(file, "categories_list.obj");
	}

	public static void storeCategories(Map<String, Object> attributes) {
		synchronized (sSynchronizationObject) {
			if (attributes == null) {
				return;
			}
			serialize(attributes, getCategoriesFile(true));
		}
	}

	public static Map<String, Object> restoreCategories() {
		synchronized (sSynchronizationObject) {
			return (Map<String, Object>) deserialize(getCategoriesFile(false));
		}
	}

	public static void removeCategories() {
		synchronized (sSynchronizationObject) {
			File f = getCategoriesFile(false);

			if (f.exists()) {
				f.delete();
			}
		}
	}

	public static boolean categoriesExist() {
		return getCategoriesFile(false).exists();
	}

	/* ======================================================================== */
	/* Attributes data */
	/* ======================================================================== */

	private static File getAttributesFile(boolean createDirectories) {
		File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);

		if (createDirectories == true) {
			if (!file.exists()) {
				file.mkdirs();
			}
		}

		return new File(file, "attributes_list.obj");
	}

	public static void storeAttributes(List<Map<String, Object>> attributes) {
		synchronized (sSynchronizationObject) {
			if (attributes == null) {
				return;
			}
			serialize(attributes, getAttributesFile(true));
		}
	}

	/*
	 * Helper function providing means of updating an attribute map inside of a
	 * list of attribute maps.
	 */
	private static void updateAttributeInTheAttributeList(List<Map<String, Object>> attribList,
			Map<String, Object> attribute) {
		if (attribList != null) {
			int i = 0;
			for (Map<String, Object> elem : attribList) {
				String codeFromCache = (String) elem
						.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_CODE_ATTRIBUTE_LIST_REQUEST);
				String codeToUpdate = (String) attribute
						.get(MageventoryConstants.MAGEKEY_ATTRIBUTE_CODE_ATTRIBUTE_LIST_REQUEST);

				if (TextUtils.equals(codeFromCache, codeToUpdate)) {
					attribList.set(i, attribute);
					break;
				}
				i++;
			}
		}
	}

	/*
	 * When adding an option to a custom attribute through an application we are
	 * getting back the whole attribute which we might want to save in the cache
	 * without downloading the entire list. This function provides the code that
	 * updates just one attribute in the cache.
	 */
	public static void updateSingleAttributeInTheCache(Map<String, Object> attribute, String setID) {
		synchronized (sSynchronizationObject) {
			List<Map<String, Object>> attrsSetList = restoreAttributes();

			if (attrsSetList != null) {
				for (Map<String, Object> elem : attrsSetList) {
					String elemSetID = (String) elem.get("set_id");

					if (TextUtils.equals(elemSetID, setID)) {
						updateAttributeInTheAttributeList((List<Map<String, Object>>) elem.get("attributes"), attribute);
					}
				}

				storeAttributes(attrsSetList);
			}
		}
	}

	public static List<Map<String, Object>> restoreAttributes() {
		synchronized (sSynchronizationObject) {
			return (List<Map<String, Object>>) deserialize(getAttributesFile(false));
		}
	}

	public static void removeAttributes() {
		synchronized (sSynchronizationObject) {
			File f = getAttributesFile(false);

			if (f.exists()) {
				f.delete();
			}
		}
	}

	public static boolean attributesExist() {
		return getAttributesFile(false).exists();
	}

	/* ======================================================================== */
	/* Last used custom attributes data */
	/* ======================================================================== */
	private static File getLastUsedCustomAttribsFile(boolean createDirectories) {
		File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);

		if (createDirectories == true) {
			if (!file.exists()) {
				file.mkdirs();
			}
		}
		
		return new File(file, "last_used_attributes_list.obj");
	}

	public static void storeLastUsedCustomAttribs(CustomAttributesList list) {
		synchronized (sSynchronizationObject) {
			if (list == null) {
				return;
			}
			serialize(list, getLastUsedCustomAttribsFile(true));
		}
	}

	public static CustomAttributesList restoreLastUsedCustomAttribs() {
		synchronized (sSynchronizationObject) {
			return (CustomAttributesList) deserialize(getLastUsedCustomAttribsFile(false));
		}
	}

	/* ======================================================================== */
	/* Input cache */
	/* ======================================================================== */
	
	private static File getInputCacheFile(boolean createDirectories) {
		File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);

		if (createDirectories == true) {
			if (!file.exists()) {
				file.mkdirs();
			}
		}
		
		return new File(file, "input_cache.obj");
	}

	public static void storeInputCache(Map<String, List<String>> inputCache) {
		synchronized (sSynchronizationObject) {
			if (inputCache == null) {
				return;
			}
			serialize(inputCache, getInputCacheFile(true));
		}
	}

	public static Map<String, List<String>> loadInputCache() {
		synchronized (sSynchronizationObject) {
			return (Map<String, List<String>>) deserialize(getInputCacheFile(false));
		}
	}
	
	/* ======================================================================== */
	/* Deleting whole cache */
	/* ======================================================================== */

	/* Delete all files recursively from a given directory. */
	private static void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				deleteRecursive(child);

		fileOrDirectory.delete();
	}

	public static void deleteEntireCache() {
		synchronized (sSynchronizationObject) {
			File file = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
			deleteRecursive(file);
		}
	}
}
