package com.mageventory.util;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.widget.ListView;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.adapters.SimpleStandardAdapter;
import com.mageventory.adapters.tree.InMemoryTreeStateManager;
import com.mageventory.adapters.tree.TreeBuilder;
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
    
    public static List<Category> getCategorylist(Map<String, Object> map) {
		Object[] children = null;
		try {
			children = (Object[]) map.get("children");
		} catch (Throwable e) {
			// NOP
		}
		if (children != null && children.length == 0) {
			return new ArrayList<Category>();
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
    
    // dialog utilities
    
    public static Dialog createCategoriesDialog(final Context context, final Map<String, Object> categoryTree, final Set<Category> selected) {
    	// prepare dialog
    	final Dialog dialog = new Dialog(context);
    	dialog.setTitle("Categories");
    	dialog.setContentView(R.layout.dialog_category_tree);

    	// prepare tree adapter
    	final InMemoryTreeStateManager<Category> manager = new InMemoryTreeStateManager<Category>();
		final TreeBuilder<Category> treeBuilder = new TreeBuilder<Category>(manager);
		final SimpleStandardAdapter simpleAdapter = new SimpleStandardAdapter(context, selected, manager, 4);
		
		final Category root = new Category(categoryTree);
		buildCategorySubTree(categoryTree, treeBuilder, root);
		
		// set adapter
		final ListView listView = (ListView) dialog.findViewById(android.R.id.list);
		listView.setAdapter(simpleAdapter);
    	
    	return dialog;
    }
    
}
