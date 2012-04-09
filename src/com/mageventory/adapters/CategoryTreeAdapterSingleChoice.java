package com.mageventory.adapters;

import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.mageventory.R;
import com.mageventory.model.Category;

public class CategoryTreeAdapterSingleChoice extends
		AbstractTreeViewAdapter<Category> {

	final LayoutInflater inflater;

	public CategoryTreeAdapterSingleChoice(Activity activity,
			TreeStateManager<Category> treeStateManager, int numberOfLevels) {
		super(activity, treeStateManager, numberOfLevels);
		inflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getNewChildView(TreeNodeInfo<Category> treeNodeInfo) {
		final LinearLayout viewLayout = (LinearLayout) inflater.inflate(
				R.layout.category_list_item_multiple_choice, null);
		return updateView(viewLayout, treeNodeInfo);
	}

	@Override
	public View updateView(View view, TreeNodeInfo<Category> treeNodeInfo) {
		
		return view;
	}

}
