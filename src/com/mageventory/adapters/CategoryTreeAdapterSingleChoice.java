package com.mageventory.adapters;

import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.mageventory.R;
import com.mageventory.model.Category;

public class CategoryTreeAdapterSingleChoice extends AbstractTreeViewAdapter<Category> {

	private final LayoutInflater inflater;
	private RadioButton currentlyChecked;
	private boolean showRadioButtons = true;
	private boolean enableRadioButtons = true;

	public void setEnableRadioButtons(boolean enableRadioButtons) {
		this.enableRadioButtons = enableRadioButtons;
	}

	public void setShowRadioButtons(boolean showRadioButtons) {
		this.showRadioButtons = showRadioButtons;
	}

	private OnCheckedChangeListener myOnCheckedChangeL = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (currentlyChecked != null) {
				currentlyChecked.setChecked(false);
			}
			currentlyChecked = (RadioButton) buttonView;
		}
	};

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
		return updateView(viewLayout, treeNodeInfo);
	}

	@Override
	public View updateView(View view, TreeNodeInfo<Category> treeNodeInfo) {
		final LinearLayout viewLayout = (LinearLayout) view;
		final TextView descriptionView = (TextView) viewLayout.findViewById(R.id.demo_list_item_description);
		final RadioButton btn = (RadioButton) viewLayout.findViewById(R.id.radio_btn);

		viewLayout.setTag(treeNodeInfo.getId());
		descriptionView.setText(treeNodeInfo.getId().getName());
		btn.setEnabled(enableRadioButtons);
		if (showRadioButtons) {
			btn.setOnCheckedChangeListener(myOnCheckedChangeL);
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
			// final RadioButton rb = (RadioButton) view.findViewById(R.id.radio_btn);
			// rb.performClick();
		}
	}

	public static void setRadioButtonChecked(View parent, boolean checked) {
		View radioButton = parent.findViewById(R.id.radio_btn);
		if (radioButton != null && radioButton instanceof RadioButton) {
			((RadioButton) radioButton).setChecked(checked);
		}
	}

}
