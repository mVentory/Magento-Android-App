package com.mageventory.util;

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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.adapters.CategoryTreeAdapterSingleChoice;
import com.mageventory.model.Category;

public class DialogUtil implements MageventoryConstants {
	
	// TODO y: I should move all the progress dialog logic here... I think there is a task for this

	public static interface OnCategorySelectListener {
		public boolean onCategorySelect(Category category);
	}

	public static Dialog createCategoriesDialog(final Activity context, final Map<String, Object> rootCategory,
			final OnCategorySelectListener onCategorySelectL, final Category preselect) {
		if (rootCategory == null) {
			return null;
		}

		// prepare
		final TreeViewList view = (TreeViewList) ((LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_category_list, null);

		final Dialog dialog = new Dialog(context);
		dialog.setTitle("Categories");
		dialog.setContentView(view);

		final TreeStateManager<Category> treeStateManager = new InMemoryTreeStateManager<Category>();
		final TreeBuilder<Category> treeBuilder = new TreeBuilder<Category>(treeStateManager);

		Util.buildCategoryTree(rootCategory, treeBuilder);
		final CategoryTreeAdapterSingleChoice adapter = new CategoryTreeAdapterSingleChoice(context, treeStateManager,
				4);
		adapter.setSelectedCategory(preselect);

		// attach listeners
		if (onCategorySelectL != null) {view.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					final Category cat = (Category) arg1.getTag();
					if (cat == null || onCategorySelectL == null) {
						return false;
					}
					// adapter.markAsSelected(arg1);
					return onCategorySelectL.onCategorySelect(cat);
				}
			});
		}

		// set adapter
		view.setAdapter(adapter);

		return dialog;
	}

}
