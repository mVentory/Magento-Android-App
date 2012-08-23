package com.mageventory.activity;

import com.mageventory.R;
import com.mageventory.R.layout;
import com.mageventory.R.string;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.tasks.CreateOptionTask;
import com.mageventory.tasks.LoadOrderDetailsData;
import com.mageventory.tasks.LoadOrderListData;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.Map;


public class OrderDetailsActivity extends BaseActivity {

	private LoadOrderDetailsData mLoadOrderDetailsDataTask;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.order_list_activity);
		
		this.setTitle("Mventory: Order details");
		
			
		mLoadOrderDetailsDataTask = new LoadOrderDetailsData(this, "100000345");
		mLoadOrderDetailsDataTask.execute();
	}

	/* Shows a dialog for adding new option. */
	public void showFailureDialog() {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Load failed");
		alert.setMessage("Unable to load order details.");

		alert.setPositiveButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				Intent myIntent = new Intent(OrderDetailsActivity.this.getApplicationContext(), OrderDetailsActivity.class);
				OrderDetailsActivity.this.startActivity(myIntent);
				OrderDetailsActivity.this.finish();
			}
		});

		alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				OrderDetailsActivity.this.finish();
			}
		});
	
		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	public void onOrderDetailsLoadStart() {
	}

	public void onOrderDetailsFailure() {
		showFailureDialog();
	}

	public void onOrderDetailsSuccess() {
	}

}
