package com.mageventory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;

import com.mageventory.adapters.SimpleStandardAdapter;
import com.mageventory.adapters.tree.InMemoryTreeStateManager;
import com.mageventory.adapters.tree.TreeBuilder;
import com.mageventory.model.Category;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.util.DefaultOptionsMenuHelper;

public class CategoryListActivity extends ListActivity implements MageventoryConstants, OperationObserver {

	private class LoadTask extends AsyncTask<Object, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Object... args) {
			if (resHelper.isResourceAvailable(CategoryListActivity.this, RES_CATALOG_CATEGORY_TREE)) {
				Map<String, Object> tree = resHelper.restoreResource(CategoryListActivity.this,
						RES_CATALOG_CATEGORY_TREE);
				if (tree == null) {
					return Boolean.FALSE;
				}
				Category root = new Category("" + tree.get(MAGEKEY_CATEGORY_NAME), "" + tree.get(MAGEKEY_CATEGORY_ID));
				treeBuilder.sequentiallyAddNextNode(root, 0);
				getCategoryTree(tree, treeBuilder, root);
				return Boolean.TRUE;
			} else {
				requestId = resHelper.loadResource(CategoryListActivity.this, RES_CATALOG_CATEGORY_TREE);
				return Boolean.FALSE;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				displayTree();
			}
		}
	}

	private static final String TAG = "CategoryListActivity";

	private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
	private int requestId;
	private SimpleStandardAdapter simpleAdapter;
	private boolean dataDisplayed;
	private InMemoryTreeStateManager<Category> manager;
	private TreeBuilder<Category> treeBuilder;
	private final Set<Category> selected = new HashSet<Category>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.categories_list);

		// title
		this.setTitle("Mventory: Category List");

		manager = new InMemoryTreeStateManager<Category>();
		treeBuilder = new TreeBuilder<Category>(manager);
		simpleAdapter = new SimpleStandardAdapter(this, selected, manager, 4);

		// attach listeners
		getListView().setOnItemLongClickListener(myOnItemClickListener);
	}

	private OnItemLongClickListener myOnItemClickListener = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long categoryId) {

			Intent myIntent = new Intent(getApplicationContext(), ProductListActivity2.class);
			myIntent.putExtra(getString(R.string.ekey_category_id), (int) categoryId);
			startActivity(myIntent);
			return true;
		}
	};

	private static void getCategoryTree(Map<String, Object> map, TreeBuilder<Category> tb, Category parent) {
		final Object[] children = (Object[]) map.get("children");
		if (children == null || children.length == 0)
			return;
		try {
			for (Object m : children) {
				@SuppressWarnings("unchecked")
				Map<String, Object> cHashmap = (Map<String, Object>) m;
				Category child = new Category(cHashmap.get("name").toString(), cHashmap.get("category_id").toString());
				tb.addRelation(parent, child);
				// tb.sequentiallyAddNextNode(child, 0);
				getCategoryTree(cHashmap, tb, child);
			}
		} catch (Exception e) {
			Log.w(TAG, "" + e);
			// TODO y: handle
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		resHelper.registerLoadOperationObserver(this);
		if (!dataDisplayed) {
			loadData();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		resHelper.unregisterLoadOperationObserver(this);
	}

	private void displayTree() {
		dataDisplayed = true;
		setListAdapter(simpleAdapter);
	}

	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		if (requestId != op.getOperationRequestId()) {
			return;
		}
		if (op.getException() != null) {
			Toast.makeText(this, "" + op.getException().getMessage(), Toast.LENGTH_SHORT).show();
			return;
		}
		loadData();
	}

	private void loadData() {
		dataDisplayed = false;
		new LoadTask().execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return DefaultOptionsMenuHelper.onCreateOptionsMenu(this, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return DefaultOptionsMenuHelper.onOptionsItemSelected(this, item);
	}

}