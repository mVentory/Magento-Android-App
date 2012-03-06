package com.mageventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import android.os.Bundle;
import com.mageventory.R;
import com.mageventory.adapters.tree.TreeViewList;

public class CategoryListActivity extends BaseActivity {
	MyApplication app;
    private enum TreeType implements Serializable {
        SIMPLE,
        FANCY
    }
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.categories_list);

		app = (MyApplication) getApplication();
		this.setTitle("Mventory: Category List");
		
		TreeType newTreeType = null;
		TreeViewList treeView = (TreeViewList) findViewById(R.id.mainTreeView);

	}
}