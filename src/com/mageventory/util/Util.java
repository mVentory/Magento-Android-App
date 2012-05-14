package com.mageventory.util;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import pl.polidea.treeview.TreeBuilder;
import android.graphics.Bitmap;
import com.mageventory.util.Log;

import com.mageventory.MageventoryConstants;
import com.mageventory.model.Category;

public class Util implements MageventoryConstants {

	private static final String TAG = "Util";

	// log utilities

	private static Formatter formatter;

	public static String currentThreadName() {
		return Thread.currentThread().getName();
	}

	private static Formatter getFormatter() {
		if (formatter == null) {
			formatter = new Formatter();
		}
		return formatter;
	}

	public static void logThread(String tag, String log, Object... args) {
		if (args != null && args.length > 0) {
			log = getFormatter().format(log, args).toString();
		}
		Log.d(tag, "currentThread=" + currentThreadName() + ",log=" + log);
	}

	// category utilities

	public static void buildCategoryTree(Map<String, Object> map, TreeBuilder<Category> tb) {
		Object[] children = null;
		try {
			children = (Object[]) map.get(MAGEKEY_CATEGORY_CHILDREN);
		} catch (Throwable e) {
			// NOP
		}
		if (children == null || children.length == 0) {
			return;
		}
		try {
			for (Object childObj : children) {
				@SuppressWarnings("unchecked")
				final Map<String, Object> childData = (Map<String, Object>) childObj;
				final Category child = new Category(childData);
				tb.sequentiallyAddNextNode(child, 0);
				buildCategorySubTree(childData, tb, child);
			}
		} catch (Throwable e) {
			Log.w(TAG, "" + e);
		}
	}

	// XXX y: using recursion here is bad, make this function iterable instead
	private static void buildCategorySubTree(Map<String, Object> map, TreeBuilder<Category> tb, Category parent) {
		Object[] children = null;
		try {
			children = (Object[]) map.get(MAGEKEY_CATEGORY_CHILDREN);
		} catch (Throwable e) {
			// NOP
		}
		if (children == null || children.length == 0) {
			return;
		}
		try {
			for (Object childObj : children) {
				@SuppressWarnings("unchecked")
				final Map<String, Object> childData = (Map<String, Object>) childObj;
				final Category child = new Category(childData);
				tb.addRelation(parent, child);
				buildCategorySubTree(childData, tb, child);
			}
		} catch (Throwable e) {
			// TODO y: handle?
			Log.w(TAG, "" + e);
		}
	}

	public static List<Category> getCategoryList(Map<String, Object> rootData, boolean useIndent) {
		final List<Map<String, Object>> categoryMapList = getCategoryMapList(rootData, useIndent);
		if (categoryMapList == null) {
			return null;
		}
		final List<Category> categories = new ArrayList<Category>(categoryMapList.size());
		for (final Map<String, Object> categoryMap : categoryMapList) {
			categories.add(new Category(categoryMap));
		}
		return categories;
	}

	public static List<Map<String, Object>> getCategoryMapList(Map<String, Object> rootData, boolean useIndent) {
		final List<Map<String, Object>> topCategories = getChildren(rootData, 0);
		for (final Map<String, Object> topCategory : topCategories) {
			topCategory.put("level", 0);
		}
		final List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>(32);
		// depth first search
		final Stack<Map<String, Object>> stack = new Stack<Map<String, Object>>();

		Collections.reverse(topCategories);
		stack.addAll(topCategories);
		while (stack.empty() == false) {
			final Map<String, Object> categoryData = stack.pop();
			ret.add(categoryData);

			final int childLevel;
			if (useIndent) {
				final int parentLevel = (Integer) categoryData.get("level");
				childLevel = parentLevel + 1;
			} else {
				childLevel = 0;
			}

			final List<Map<String, Object>> children = getChildren(categoryData, childLevel);
			if (children == null || children.isEmpty()) {
				continue;
			}
			Collections.reverse(children);
			for (final Map<String, Object> child : children) {
				child.put("level", childLevel);
				stack.add(child);
			}
		}
		return ret;
	}

	public static List<Map<String, Object>> getChildren(Map<String, Object> parentData, final int indentLevel) {
		Object[] children = null;
		try {
			children = (Object[]) parentData.get(MAGEKEY_CATEGORY_CHILDREN);
		} catch (Throwable e) {
			// NOP, catches NPE and CCE
		}
		if (children == null || children.length == 0) {
			return null;
		}
		final List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>(children.length);
		for (final Object childObj : children) {
			try {
				@SuppressWarnings("unchecked")
				final Map<String, Object> childData = (Map<String, Object>) childObj;

				if (indentLevel > 0) {
					// indent category name
					char[] space = new char[indentLevel]; // throw exception if indentLevel is negative
					Arrays.fill(space, '-');
					childData.put(MAGEKEY_CATEGORY_NAME,
					        String.format(" %s %s", new String(space), childData.get(MAGEKEY_CATEGORY_NAME)));
				}

				// add to list
				ret.add(childData);
			} catch (Throwable e) {
				// NOP
			}
		}
		return ret;
	}

	public static List<Category> getCategorylist(Map<String, Object> rootData) {
		Object[] children = null;
		try {
			children = (Object[]) rootData.get("children");
		} catch (Throwable e) {
			// NOP
		}
		if (children != null && children.length == 0) {
			return null;
		}
		final List<Category> categoryList = new ArrayList<Category>(32);
		try {
			for (Object m : children) {
				@SuppressWarnings("unchecked")
				Map<String, Object> categoryData = (Map<String, Object>) m;
				categoryList.add(new Category(categoryData));
				categoryList.addAll(getCategorylist(categoryData));
			}
		} catch (Exception e) {
			Log.v(TAG, "" + e);
		}
		return categoryList;
	}

    /**
     * Uses an AsyncTask to save a bitmap on sdCard
     * 
     * @param bmp the <code>Bitmap</code> to save
     * @param imagePath	the path of the bitmap
     */
    public static void saveBitmapOnSDcard(Bitmap bmp, String imagePath){

    	try {
			FileOutputStream fo = new FileOutputStream((String) imagePath);
			bmp.compress(Bitmap.CompressFormat.PNG, 100, fo);

			fo.flush();
			fo.close();
		} catch (Exception e) {
			Log.logCaughtException(e);
		}
    }
}
