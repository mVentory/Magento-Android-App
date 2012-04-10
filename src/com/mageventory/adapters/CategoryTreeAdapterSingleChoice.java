package com.mageventory.adapters;

import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.model.Category;

public class CategoryTreeAdapterSingleChoice extends AbstractTreeViewAdapter<Category> implements MageventoryConstants {

	private final LayoutInflater inflater;
	private Category currentlySelectedCategory;
	private boolean showRadioButtons = true;
	private boolean enableRadioButtons = true;
	private View selected;

	public void setEnableRadioButtons(boolean enableRadioButtons) {
		this.enableRadioButtons = enableRadioButtons;
	}

	public void setShowRadioButtons(boolean showRadioButtons) {
		this.showRadioButtons = showRadioButtons;
	}

	public CategoryTreeAdapterSingleChoice(Activity activity, TreeStateManager<Category> treeStateManager,
	        int numberOfLevels) {
		super(activity, treeStateManager, numberOfLevels);
		inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getNewChildView(TreeNodeInfo<Category> treeNodeInfo) {
		final LinearLayout viewLayout = (LinearLayout) inflater
		        .inflate(R.layout.category_list_item_single_choice, null);
		if (currentlySelectedCategory != null && currentlySelectedCategory.getId() != INVALID_CATEGORY_ID
				&& treeNodeInfo.getId() != null && treeNodeInfo.getId().getId() != INVALID_CATEGORY_ID
				&& currentlySelectedCategory.getId() == treeNodeInfo.getId().getId()) {
			markAsSelected(viewLayout);
		}
		return updateView(viewLayout, treeNodeInfo);
	}
	
	@Override
	public View updateView(View view, TreeNodeInfo<Category> treeNodeInfo) {
		final LinearLayout viewLayout = (LinearLayout) view;
		final TextView descriptionView = (TextView) viewLayout.findViewById(R.id.demo_list_item_description);
		final RadioButton btn = (RadioButton) viewLayout.findViewById(R.id.radio_btn);

		// set category as tag; this is a bit hacky since users have to know about this concept, but it's OK as long as
		// the DialogUtil is the only user of this class
		viewLayout.setTag(treeNodeInfo.getId());
		btn.setTag(treeNodeInfo.getId());

		descriptionView.setText(treeNodeInfo.getId().getName());
		btn.setEnabled(enableRadioButtons);
		if (showRadioButtons) {
		} else {
			btn.setVisibility(View.GONE);
		}

		return viewLayout;
	}

	@Override
	public void handleItemClick(final View view, final Object id) {
		final Category longId = (Category) id;
		final TreeNodeInfo<Category> info = getManager().getNodeInfo(longId);
		if (info.isWithChildren()) {
			super.handleItemClick(view, id);
		} else {
			view.performLongClick();
		}
	}

	public void setSelectedCategory(Category preselect) {
		currentlySelectedCategory = preselect;
	}
	
	public void markAsSelected(View v) {
		if (v == null) {
			return;
		}
		markAsUnselected(selected);
		View btn = v.findViewById(R.id.radio_btn);
		if (btn == null) {
			return;
		}
		selected = v;
		((RadioButton) btn).setChecked(true);
	}
	
	private void markAsUnselected(View v) {
		if (v == null) {
			return;
		}
		View btn = v.findViewById(R.id.radio_btn);
		if (btn != null) {
			((RadioButton) btn).setChecked(false);
		}
	}

}
