package com.mageventory.activity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeBuilder;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.R.id;
import com.mageventory.R.layout;
import com.mageventory.R.string;
import com.mageventory.activity.base.BaseListActivity;
import com.mageventory.adapters.SimpleStandardAdapter;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Category;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.DefaultOptionsMenuHelper;
import com.mageventory.util.Util;

public class TMCategoryListActivity extends BaseListActivity implements MageventoryConstants {

	public static final String CATEGORIES_MAP_PARAM_KEY = "categories_map_param_key";

	@SuppressWarnings("unused")
	private static final String TAG = "TMCategoryListActivity";

	private SimpleStandardAdapter simpleAdapter;
	private boolean dataDisplayed;
	private final Set<Category> selected = new HashSet<Category>();
	private Map<String, Object> mTreeData;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.category_list);

		mTreeData = (Map<String, Object>)getIntent().getExtras().getSerializable(CATEGORIES_MAP_PARAM_KEY);
		
		// title
		this.setTitle("Mventory: Categories");

		// attach listeners
		getListView().setOnItemLongClickListener(myOnItemClickListener);
	}

	private OnItemLongClickListener myOnItemClickListener = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long categoryId) {
			String categoryName = "";
			
			final Map<String, Object> rootCategory = mTreeData;
			if (rootCategory != null && !rootCategory.isEmpty()) {
				for (Category cat : Util.getCategorylist(rootCategory, null)) {
					if (cat.getId() == categoryId) {
						categoryName = cat.getFullName();
					}
				}
			}
			
			/* All non-leaf nodes have categoryId == -1. We don't want the user to be able to select those. */
			if (categoryId != -1)
			{
				Intent intent = new Intent();
				intent.putExtra(getString(R.string.ekey_category_id), (int) categoryId);
				intent.putExtra(getString(R.string.ekey_category_name), categoryName);
				
				setResult(MageventoryConstants.RESULT_SUCCESS, intent);
				finish();
			}
			
			return true;
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		if (!dataDisplayed) {
			loadData();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	private void displayTree() {
		if (simpleAdapter != null) {
			if (simpleAdapter == getListAdapter()) {
				simpleAdapter.notifyDataSetChanged();
			} else {
				setListAdapter(simpleAdapter);
			}
		}
		dataDisplayed = true;
	}

	private void loadData() {
		dataDisplayed = false;
		
		InMemoryTreeStateManager<Category> manager = new InMemoryTreeStateManager<Category>();
		TreeBuilder<Category> treeBuilder = new TreeBuilder<Category>(manager);
		Util.buildCategoryTree(mTreeData, treeBuilder);
		simpleAdapter = new SimpleStandardAdapter(TMCategoryListActivity.this, selected, manager, 12);
		displayTree();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			loadData();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}