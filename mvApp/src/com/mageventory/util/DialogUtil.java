package com.mageventory.util;

import java.util.List;
import java.util.Map;

import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.adapters.CategoryTreeAdapterSingleChoice;
import com.mageventory.model.Category;

public class DialogUtil implements MageventoryConstants {

	// TODO y: I should move all the progress dialog logic here... I think there
	// is a task for this
	private static final String PREFERENCES_NAME = "dialog_util";
	protected static final String PKEY_SELECTION = "selection_from_top";

	public static interface OnCategorySelectListener {
		public boolean onCategorySelect(Category category);
	}

	public static Dialog createCategoriesDialog(final Activity context, final Map<String, Object> rootCategory,
			final OnCategorySelectListener onCategorySelectL, final Category preselect) {
		if (rootCategory == null) {
			return null;
		}

		try {
			// prepare
			final TreeViewList list = (TreeViewList) ((LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_category_list, null);

			final Dialog dialog = new Dialog(context);
			dialog.setTitle("Categories");
			dialog.setContentView(list);

			final TreeStateManager<Category> treeStateManager = new InMemoryTreeStateManager<Category>();
			final TreeBuilder<Category> treeBuilder = new TreeBuilder<Category>(treeStateManager);

			Util.buildCategoryTree(rootCategory, treeBuilder);
			final CategoryTreeAdapterSingleChoice adapter = new CategoryTreeAdapterSingleChoice(context,
					treeStateManager, 4, false, INVALID_CATEGORY_ID);
			adapter.setSelectedCategory(preselect);

			// attach listeners
			if (onCategorySelectL != null) {
				list.setOnItemLongClickListener(new OnItemLongClickListener() {
					@Override
					public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						final Category cat = (Category) arg1.getTag();
						if (cat == null || onCategorySelectL == null) {
							return false;
						}
						context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
								.putInt(PKEY_SELECTION, arg2).commit();
						return onCategorySelectL.onCategorySelect(cat);
					}
				});
			}

			// set adapter
			list.setAdapter(adapter);

			// scroll to selected category
			// y XXX: using preferences for this isn't the best way to do it...
			if (preselect != null) {
				final int selection = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).getInt(
						PKEY_SELECTION, 0);
				list.setSelection(selection);
			}

			return dialog;
		} catch (OutOfMemoryError e) {
			return null;
		}
	}

	public static Dialog createListDialog(final Activity host, final String dialogTitle,
			final List<Map<String, Object>> data, final int rowId, final String[] keys, final int[] viewIds,
			final OnItemClickListener onItemClickL) {
		final Dialog dialog = new Dialog(host);
		dialog.setTitle(dialogTitle);

		final ListView list = new ListView(host);
		final SimpleAdapter adapter = new SimpleAdapter(host, data, rowId, keys, viewIds);

		dialog.setContentView(list);
		list.setAdapter(adapter);

		list.setOnItemClickListener(onItemClickL);

		return dialog;
	}

}
