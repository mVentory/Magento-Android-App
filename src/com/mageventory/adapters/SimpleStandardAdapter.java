package com.mageventory.adapters;

import java.util.Arrays;
import java.util.Set;

import com.mageventory.CategoryListActivity;
import com.mageventory.adapters.tree.AbstractTreeViewAdapter;
import com.mageventory.adapters.tree.TreeNodeInfo;
import com.mageventory.adapters.tree.TreeStateManager;
import com.mageventory.model.Category;
import com.mageventory.R;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This is a very simple adapter that provides very basic tree view with a
 * checkboxes and simple item description.
 * 
 */

public class SimpleStandardAdapter extends AbstractTreeViewAdapter<Category> {

	private final Set<Category> selected;

	private final OnCheckedChangeListener onCheckedChange = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(final CompoundButton buttonView,
				final boolean isChecked) {
			final Category id = (Category) buttonView.getTag();
			changeSelected(isChecked, id);
		}

	};

	private void changeSelected(final boolean isChecked, final Category id) {
		if (isChecked) {
			selected.add(id);
		} else {
			selected.remove(id);
		}
		if(selected.size()>0){
			Log.d("APP",((Category)(selected.toArray()[0])).getName());	
		}
		
	}

	public SimpleStandardAdapter(final CategoryListActivity treeViewListDemo,
			final Set<Category> selected,
			final TreeStateManager<Category> treeStateManager,
			final int numberOfLevels) {
		super(treeViewListDemo, treeStateManager, numberOfLevels);
		this.selected = selected;
	}

	private String getDescription(final Category id) {
		final Integer[] hierarchy = getManager().getHierarchyDescription(id);
		//return ""+id.getName() + Arrays.asList(hierarchy);
		return ""+id.getName();
	}
	private String getCategoryId(final Category id) {
		final Integer[] hierarchy = getManager().getHierarchyDescription(id);
		//return ""+id.getName() + Arrays.asList(hierarchy);
		return ""+id.getId();
	}

	@Override
	public View getNewChildView(final TreeNodeInfo<Category> treeNodeInfo) {
		final LinearLayout viewLayout = (LinearLayout) getActivity()
				.getLayoutInflater().inflate(R.layout.demo_list_item, null);
		return updateView(viewLayout, treeNodeInfo);
	}

	@Override
	public LinearLayout updateView(final View view,
			final TreeNodeInfo<Category> treeNodeInfo) {
		final LinearLayout viewLayout = (LinearLayout) view;
		final TextView descriptionView = (TextView) viewLayout
				.findViewById(R.id.demo_list_item_description);

		final TextView levelView = (TextView) viewLayout
				.findViewById(R.id.demo_list_item_level);
		descriptionView.setText(getDescription(treeNodeInfo.getId()));
		//levelView.setText(Integer.toString(treeNodeInfo.getLevel()));
		levelView.setText("");
		final CheckBox box = (CheckBox) viewLayout
				.findViewById(R.id.demo_list_checkbox);
		box.setTag(treeNodeInfo.getId());
		if (treeNodeInfo.isWithChildren()) {
			box.setVisibility(View.GONE);
		} else {
			box.setVisibility(View.VISIBLE);
			box.setChecked(selected.contains(treeNodeInfo.getId()));
		}
		box.setOnCheckedChangeListener(onCheckedChange);
		return viewLayout;
	}

	@Override
	public void handleItemClick(final View view, final Object id) {
		final Category longId = (Category) id;
		final TreeNodeInfo<Category> info = getManager().getNodeInfo(longId);
		if (info.isWithChildren()) {
			super.handleItemClick(view, id);
		} else {
			final ViewGroup vg = (ViewGroup) view;
			final CheckBox cb = (CheckBox) vg.findViewById(R.id.demo_list_checkbox);
			cb.performClick();
		}
	}

	@Override
	public long getItemId(final int position) {
		return (long)getTreeId(position).getId();
	}
}