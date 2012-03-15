package com.mageventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.mageventory.R;
import com.mageventory.adapters.SimpleStandardAdapter;
import com.mageventory.adapters.tree.InMemoryTreeStateManager;
import com.mageventory.adapters.tree.TreeBuilder;
import com.mageventory.adapters.tree.TreeViewList;
import com.mageventory.client.MagentoClient;
import com.mageventory.model.Category;



public class CategoryListActivity extends BaseActivity {
	MyApplication app;
	MagentoClient magentoClient;
	InMemoryTreeStateManager<Category> manager;
	TreeBuilder<Category> treeBuilder;
	ProgressDialog pDialog;
	private final Set<Category> selected = new HashSet<Category>();
	TreeViewList treeView;
	SimpleStandardAdapter simpleAdapter;
    /*private enum TreeType implements Serializable {
        SIMPLE,
        FANCY
    }*/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.categories_list);

		app = (MyApplication) getApplication();
		ArrayList<Category> categories = app.getCategories();
		this.setTitle("Mventory: Category List");
		
		//TreeType newTreeType = null;
		//TreeViewList treeView = (TreeViewList) findViewById(R.id.mainTreeView);
		treeView= new TreeViewList(getApplicationContext());
		LinearLayout ly= (LinearLayout) findViewById(R.id.linearlayout);
		ly.addView(treeView);
		manager = new InMemoryTreeStateManager<Category>();
        treeBuilder = new TreeBuilder<Category>(manager);
        
        simpleAdapter= new SimpleStandardAdapter(this, selected, manager, 4);
        registerForContextMenu(treeView);
        treeView.setOnItemLongClickListener(myOnItemClickListener);
        CategoryRetrieve cr= new CategoryRetrieve();
        cr.execute(new Integer[]{1});


	}

	private OnItemLongClickListener myOnItemClickListener= new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long categoryId) {

			Intent myIntent = new Intent(getApplicationContext(), ProductListActivity.class);
			myIntent.putExtra("id", categoryId+"");
			myIntent.putExtra("name", "");
			startActivity(myIntent);
			return true;
		}
	};
	
	private static void getCategoryTree(HashMap map,TreeBuilder<Category> tb,Category parent) {
		
		Object[] children = (Object[]) map.get("children");
		if (children.length == 0)
			return;
		try {

			for (Object m : children) {
				HashMap cHashmap = (HashMap) m;
				Category child=new Category(cHashmap.get("name").toString(), cHashmap.get("category_id").toString());
				Log.d("app","cat code "+child.getName()+ " "+child.getId());
				tb.addRelation(parent, child);
				//tb.sequentiallyAddNextNode(child, 0);
				getCategoryTree(cHashmap, tb, child);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.i("APP_INFO", "dead");
		}
	}
		
		private class CategoryRetrieve extends AsyncTask<Integer, Integer, Integer> {
			@Override
			protected void onPreExecute() {
				pDialog = new ProgressDialog(CategoryListActivity.this);
				pDialog.setMessage("Loading Categories");
				pDialog.setIndeterminate(true);
				pDialog.setCancelable(true);
				pDialog.show();
			}

			@Override
			protected Integer doInBackground(Integer... ints) {
				try {
					magentoClient = app.getClient();
					Object categories;
					categories = magentoClient.execute("catalog_category.tree");
					HashMap map = (HashMap) categories;
					Category root=new Category(map.get("name").toString(),map.get("category_id").toString());
					Log.d("app","cat code "+root.getName()+ " "+root.getId());
					treeBuilder.sequentiallyAddNextNode(root,0);
					getCategoryTree(map,treeBuilder, root);
					return 1;
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}

			}

			@Override
			protected void onPostExecute(Integer result) {
				treeView.setAdapter(simpleAdapter);
				pDialog.dismiss();
				if (result == 1) {
					
				} else {
					Toast.makeText(getApplicationContext(), "Request Error", Toast.LENGTH_SHORT).show();
				}
			}

		}
}