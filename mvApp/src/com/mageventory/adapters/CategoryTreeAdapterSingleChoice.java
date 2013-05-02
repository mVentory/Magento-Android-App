package com.mageventory.adapters;

import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.model.Category;

public class CategoryTreeAdapterSingleChoice extends AbstractTreeViewAdapter<Category> implements MageventoryConstants {

	private final LayoutInflater inflater;
	private Category currentlySelectedCategory;
	private boolean mShowNonLeafsGreyedOut;
	private int mDefaultCategoryID;

	public CategoryTreeAdapterSingleChoice(Activity activity, TreeStateManager<Category> treeStateManager,
			int numberOfLevels, boolean showNonLeafsGreyedOut, int defaultCategoryID) {
		super(activity, treeStateManager, numberOfLevels);
		inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mShowNonLeafsGreyedOut = showNonLeafsGreyedOut;
		mDefaultCategoryID = defaultCategoryID;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getNewChildView(TreeNodeInfo<Category> treeNodeInfo) {
		
		final LinearLayout viewLayout;
		
		viewLayout = (LinearLayout) inflater
			.inflate(R.layout.category_list_item_single_choice, null);
		
		return updateView(viewLayout, treeNodeInfo);
	}

	@Override
	public View updateView(View view, TreeNodeInfo<Category> treeNodeInfo) {
		final TextView descriptionView = (TextView) view.findViewById(R.id.item_description);

		// set category as tag; this is a bit hacky since users have to know
		// about this concept, but it's OK as long as
		// the DialogUtil is the only user of this class
		view.setTag(treeNodeInfo.getId());
		descriptionView.setText(treeNodeInfo.getId().getName());
		return view;
	}

	@Override
	public void handleItemClick(final View view, final Object id) {
		final Category longId = (Category) id;
		final TreeNodeInfo<Category> info = getManager().getNodeInfo(longId);
		if (info.isWithChildren()) {
			super.handleItemClick(view, id);
		} else {
			view.performLongClick();
			currentlySelectedCategory = longId;
		}
	}

	public void setSelectedCategory(Category preselect) {
		currentlySelectedCategory = preselect;
	}

	@Override
	public Drawable getBackgroundDrawable(TreeNodeInfo<Category> treeNodeInfo) {
		final Category cat = treeNodeInfo.getId();
		if (currentlySelectedCategory != null && cat != null
				&& currentlySelectedCategory.getId() != INVALID_CATEGORY_ID
				&& currentlySelectedCategory.getId() == cat.getId()) {
			// final Resources res = getActivity().getResources();
			// return
			// res.getDrawable(android.R.drawable.list_selector_background);
			return new ColorDrawable(Color.parseColor("#aaFF8000"));
		}
		return super.getBackgroundDrawable(treeNodeInfo);
	}
	
	@Override
    public final LinearLayout populateTreeItem(final LinearLayout layout,
            final View childView, final TreeNodeInfo<Category> nodeInfo,
            final boolean newChildView) {
		
		LinearLayout childLayout = (LinearLayout) childView;
		TextView itemDescription = (TextView)childLayout.findViewById(R.id.item_description);
		
		if (mShowNonLeafsGreyedOut && nodeInfo.isWithChildren())
		{
			itemDescription.setTextColor(0xFF555555);
		}
		else
		{
			itemDescription.setTextColor(0xFFCCCCCC);
		}
		
		if (mDefaultCategoryID != INVALID_CATEGORY_ID && nodeInfo.getId().getId() == mDefaultCategoryID)
		{
			itemDescription.setTypeface(null, Typeface.BOLD);	
		}
		
		return super.populateTreeItem(layout, childView, nodeInfo, newChildView);
    }

}
