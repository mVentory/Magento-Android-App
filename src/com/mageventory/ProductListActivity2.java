package com.mageventory;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.util.DefaultOptionsMenuHelper;

public class ProductListActivity2 extends ListActivity implements MageventoryConstants, OperationObserver {

	private static class EmptyListAdapter extends BaseAdapter {

		private static final Map<String, String> data = new HashMap<String, String>();
		static {
			for (final String key : REQUIRED_PRODUCT_KEYS) {
				data.put(key, "");
			}
		}

		private final boolean displayPlaceholder;
		private final LayoutInflater inflater;

		public EmptyListAdapter(final Context context, final boolean displayPlaceholder) {
			this.displayPlaceholder = displayPlaceholder;
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return displayPlaceholder ? 1 : 0;
		}

		@Override
		public Object getItem(int position) {
			if (position == 0) {
				return data;
			}
			throw new IndexOutOfBoundsException();
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView != null) {
				return convertView;
			}
			return inflater.inflate(R.layout.product_list_placeholder, null);
		}

	};

	private static class LoadDataTask extends AsyncTask<Object, Integer, Boolean> {

		private boolean forceReload;

		public LoadDataTask(boolean forceReload) {
			super();
			this.forceReload = forceReload;
		}

		/**
		 * Expected arguments:
		 * <ul>
		 * <li>Activity host</li>
		 * <li>int resourceType</li>
		 * <li>String[] resourceParams</li>
		 * </ul>
		 */
		@Override
		protected Boolean doInBackground(Object... args) {
			Log.d(TAG, "LoadDataTask::doInBackground(" + Arrays.toString(args) + ");");
			setThreadName();
			try {
				if (args == null || args.length < 2) {
					throw new IllegalArgumentException();
				}

				final ProductListActivity2 host = (ProductListActivity2) args[0];
				final int resType = (Integer) args[1];
				final String[] params = args.length >= 3 ? (String[]) args[2] : null;

				// the catalog product list processor doesn't need name
				// filter if it's going to retrieve products by category
				if (params != null && params.length >= 2 && TextUtils.isDigitsOnly(params[1])
				        && Integer.parseInt(params[1]) != INVALID_CATEGORY_ID) {
					params[0] = null;
				}

				if (!forceReload && ResourceServiceHelper.getInstance().isResourceAvailable(host, resType, params)) {
					// there is cached data available, retrieve and display it
					host.restoreAndDisplayProductList(resType, params);
				} else {
					// load new data
					final int reqId = ResourceServiceHelper.getInstance().loadResource(host, resType, params);
					host.operationRequestId.set(reqId);
				}
				return Boolean.TRUE;
			} catch (Throwable e) {
				return Boolean.FALSE;
			}
		}

		private void setThreadName() {
			final String threadName = Thread.currentThread().getName();
			Thread.currentThread().setName("LoadDataTask[" + threadName + "]");
		}

	}

	private static class RestoreAndDisplayDataTask extends AsyncTask<Object, Integer, Boolean> {

		private List<Map<String, Object>> data;
		private WeakReference<ProductListActivity2> host;
		private boolean isRunning = true;

		public RestoreAndDisplayDataTask() {
			super();
		}

		/**
		 * Expected arguments:
		 * <ul>
		 * <li>Activity host</li>
		 * <li>int resourceType</li>
		 * <li>String[] resourceParams</li>
		 * </ul>
		 */
		@Override
		protected Boolean doInBackground(Object... args) {
			Log.d(TAG, "RestoreAndDisplayDataTask::doInBackground(" + Arrays.toString(args) + ");");
			try {
				setThreadName();
				if (args == null || args.length < 2) {
					throw new IllegalArgumentException();
				}

				// initialize
				host = new WeakReference<ProductListActivity2>((ProductListActivity2) args[0]);
				final int resType = (Integer) args[1];
				final String[] params = args.length >= 3 ? (String[]) args[2] : null;
				String nameFilter = null;
				int categoryFilter = INVALID_CATEGORY_ID;
				if (params != null) {
					if (params.length >= 1) {
						nameFilter = (String) params[0];
					}
					if (params.length >= 2) {
						categoryFilter = Integer.parseInt(params[1]);
					}
				}
				final SortOrder order = determineSortOrder(nameFilter, categoryFilter);

				// retrieve data
				data = ResourceServiceHelper.getInstance().restoreResource(host.get(), resType, params);

				// prepare adapter
				if (data != null) {
					for (final Map<String, Object> product : data) {
						if (isCancelled()) {
							return Boolean.FALSE;
						}

						// ensure the required fields are present in the product
						// map
						boolean reqFieldsPresent = true;
						for (final String field : REQUIRED_PRODUCT_KEYS) {
							if (product.containsKey(field) == false) {
								reqFieldsPresent = false;
								break;
							}
						}

						if (reqFieldsPresent == false) {
							// if not all of the required fields are there,
							// remove this product from the list
							data.remove(product);
							Log.v(TAG, "product is missing required fields: " + product);
						}
					}

					// y TODO: well... this is a bit hacky
					filterProductsByName(data, host.get().getNameFilter());
					sortProducts(data, order);
					return Boolean.TRUE;
				}
			} catch (Throwable e) {
			}
			return Boolean.FALSE;
		}

		public List<Map<String, Object>> getData() {
			return data;
		}

		public boolean isRunning() {
			return isRunning;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			isRunning = false;

			super.onPostExecute(result);
			try {
				if (result) {
					host.get().displayData(data);
				} else {
					host.get().showDialog(LOAD_FAILURE_DIALOG);
				}
			} catch (Throwable ignored) {
			}
		}

		public void setHost(ProductListActivity2 host) {
			this.host = new WeakReference<ProductListActivity2>(host);
		}

		private void setThreadName() {
			final String threadName = Thread.currentThread().getName();
			Thread.currentThread().setName("RestoreAndDisplayDataTask[" + threadName + "]");
		}

	}

	private static enum SortOrder {
		ALPHABETICALLY, BY_DATE,
	}

	private static final int[] KEY_TO_VIEW_MAP = { android.R.id.text1};
	private static final int LOAD_FAILURE_DIALOG = 1;
	private static final String[] REQUIRED_PRODUCT_KEYS = { "name" };
	private static final String TAG = "ProductListActivity2";

	private static SortOrder determineSortOrder(String nameFilter, Integer categoryFilter) {
		SortOrder order;
		if (categoryFilter != null && categoryFilter != INVALID_CATEGORY_ID) {
			order = SortOrder.ALPHABETICALLY;
		} else {
			order = TextUtils.isEmpty(nameFilter) ? SortOrder.BY_DATE : SortOrder.ALPHABETICALLY;
		}
		return order;
	}

	// TODO y: filtering by name is happening here for now, in the java
	// code, and not server-side; read issue #44 for more information
	private static void filterProductsByName(final List<Map<String, Object>> products, String nameFilter) {
		if (products == null) {
			throw new NullPointerException();
		}
		if (TextUtils.isEmpty(nameFilter)) {
			return;
		}

		nameFilter = nameFilter.toLowerCase();
		for (Iterator<Map<String, Object>> i = products.iterator(); i.hasNext();) {
			final Map<String, Object> product = i.next();

			// check if map contains name key
			if (product.containsKey(MAGEKEY_PRODUCT_NAME) == false) {
				i.remove();
				continue;
			}

			// check if map contains actual name value
			String productName = product.get(MAGEKEY_PRODUCT_NAME).toString();
			if (productName == null) {
				i.remove();
				continue;
			}

			// check if name value contains the name filter
			productName = productName.toString().toLowerCase();
			if (productName.contains(nameFilter) == false) {
				i.remove();
			}
		}
	}

	private static void sortProducts(List<Map<String, Object>> products, SortOrder order) {
		if (order == SortOrder.BY_DATE) {
			// assume list is untouched; Magento retrieves products by ID ASC,
			// but we want the list to be with the latest entries first
			Collections.reverse(products);
			return;
		}
		if (order == SortOrder.ALPHABETICALLY) {
			Collections.sort(products, new Comparator<Map<String, Object>>() {
				@Override
				public int compare(Map<String, Object> arg0, Map<String, Object> arg1) {
					final String lhsName = arg0.get(MAGEKEY_PRODUCT_NAME).toString();
					final String rhsName = arg1.get(MAGEKEY_PRODUCT_NAME).toString();
					if (lhsName != null && rhsName != null) {
						return String.CASE_INSENSITIVE_ORDER.compare(lhsName, rhsName);
					}
					// fallback
					Log.v(TAG, "missing key: " + MAGEKEY_PRODUCT_NAME + "; map0=" + arg0 + ",map1" + arg1);
					return arg0.hashCode() - arg1.hashCode();
				}
			});
		}
	}

	private String EKEY_ERROR_MESSAGE;

	private int categoryId = INVALID_CATEGORY_ID;
	private boolean isDataDisplayed = false;
	private String nameFilter = "";
	private EditText nameFilterEdit;
	private AtomicInteger operationRequestId = new AtomicInteger(INVALID_REQUEST_ID);
	private RestoreAndDisplayDataTask restoreAndDisplayTask;
	private int selectedItemPos = ListView.INVALID_POSITION;

	private void displayData(final List<Map<String, Object>> data) {
		final ProductListActivity2 host = this;
		final Runnable display = new Runnable() {
			public void run() {
				// if call is successful but there are no products to list
				final ListAdapter adapter;
				if (data.size() == 0) {
					adapter = new EmptyListAdapter(host, true);
				} else {
					adapter = new SimpleAdapter(host, data, android.R.layout.simple_list_item_1, REQUIRED_PRODUCT_KEYS,
					        KEY_TO_VIEW_MAP);
					host.isDataDisplayed = true;
				}
				setListAdapter(adapter);
				if (selectedItemPos != ListView.INVALID_POSITION) {
					getListView().setSelectionFromTop(selectedItemPos, 0);
				}
			}
		};
		if (Looper.myLooper() == Looper.getMainLooper()) {
			display.run();
		} else {
			runOnUiThread(display);
		}
	}

	private void emptyList() {
		emptyList(false);
	}

	private void emptyList(final boolean displayPlaceholder) {
		final Runnable empty = new Runnable() {
			@Override
			public void run() {
				setListAdapter(new EmptyListAdapter(ProductListActivity2.this, displayPlaceholder));
				isDataDisplayed = false;
			}
		};
		if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
			empty.run();
		} else {
			runOnUiThread(empty);
		}
	}

	private int getCategoryId() {
		return categoryId;
	}

	private String getNameFilter() {
		return nameFilter;
	}

	private void hideSoftKeyboard() {
		nameFilterEdit.clearFocus();
		InputMethodManager m = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		m.hideSoftInputFromWindow(nameFilterEdit.getWindowToken(), 0);
	}

	private void loadProductList() {
		loadProductList(false);
	}

	private void loadProductList(final boolean forceReload) {
		loadProductList(getNameFilter(), getCategoryId(), forceReload);
	}

	private void loadProductList(final String nameFilter, final Integer categoryId, final boolean forceReload) {
		Log.v(TAG, "loadProductList(" + nameFilter + ", " + categoryId + ", " + forceReload + ");");

		restoreAndDisplayTask = null;
		hideSoftKeyboard();
		emptyList();
		String[] params;
		if (categoryId != null && categoryId != INVALID_CATEGORY_ID) {
			params = new String[] { nameFilter, String.valueOf(categoryId) };
		} else {
			params = new String[] { nameFilter };
		}
		new LoadDataTask(forceReload).execute(this, RES_CATALOG_PRODUCT_LIST, params);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.product_list);

		String title;

		// initialize
		title = "Mventory: Product List";
		if (icicle != null) {
			setNameFilter(icicle.getString(getString(R.string.ekey_name_filter)));
			setCategoryId(icicle.getInt(getString(R.string.ekey_category_id)));
			operationRequestId.set(icicle.getInt(getString(R.string.ekey_operation_request_id)));
		}
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			setCategoryId(extras.getInt(getString(R.string.ekey_category_id), INVALID_CATEGORY_ID));
			final String categoryName = extras.getString(getString(R.string.ekey_category_name));
			if (TextUtils.isEmpty(categoryName) == false) {
				title = String.format("Mventory: %s", categoryName);
			}
		}
		
		// set title
		this.setTitle(title);
		
		// constants
		EKEY_ERROR_MESSAGE = getString(R.string.ekey_error_message);

		// add header
		final LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View header = inflater.inflate(R.layout.product_list_header, null);
		getListView().addHeaderView(header);

		// set on list item long click listener
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				launchDetails(arg0, arg2, true);
				return true;
			}
		});

		// initialize filtering
		nameFilterEdit = (EditText) header.findViewById(R.id.filter_query);
		nameFilterEdit.setText(getNameFilter());
		header.findViewById(R.id.filter_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String nameFilter = "" + nameFilterEdit.getText();
				setNameFilter(nameFilter);
				loadProductList();
			}
		});

		// try to restore data loading task after orientation switch
		restoreAndDisplayTask = (RestoreAndDisplayDataTask) getLastNonConfigurationInstance();
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle bundle) {
		if (id == LOAD_FAILURE_DIALOG) {
			// build dialog
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final AlertDialog dialog = builder.create();

			// set title
			dialog.setTitle(getString(R.string.data_load_failure));

			// set message
			final StringBuilder message = new StringBuilder(64);
			message.append(getString(R.string.check_your_internet_retry));
			if (bundle != null && bundle.containsKey(EKEY_ERROR_MESSAGE)) {
				message.append("\n\n");
				message.append(getString(R.string.error));
				message.append(": ");
				message.append(bundle.get(EKEY_ERROR_MESSAGE));
			}
			dialog.setMessage(message.toString());

			// set buttons
			dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.try_again),
			        new DialogInterface.OnClickListener() {
				        @Override
				        public void onClick(DialogInterface dialog, int which) {
					        loadProductList();
				        }
			        });
			dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
			        new DialogInterface.OnClickListener() {
				        @Override
				        public void onClick(DialogInterface dialog, int which) {
					        ProductListActivity2.this.finish();
				        }
			        });

			return dialog;
		}
		return super.onCreateDialog(id);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return DefaultOptionsMenuHelper.onCreateOptionsMenu(this, menu);
	}
	
	private void launchDetails(AdapterView<? extends Adapter> list, int position, final boolean edit) {
		// TODO y: use action
		// final Intent intent = new
		// Intent(getString(R.string.action_product_details));
		final Intent intent = new Intent(this, ProductDetailsActivity.class);
		// get product id and put it as intent extra
		int productId;
		try {
			@SuppressWarnings("unchecked")
			final Map<String, Object> data = (Map<String, Object>) list.getAdapter().getItem(position);
			productId = Integer.parseInt(data.get(MAGEKEY_PRODUCT_ID).toString());
		} catch (Throwable e) {
			Toast.makeText(this, getString(R.string.invalid_product_id), Toast.LENGTH_SHORT).show();
			return;
		}
		intent.putExtra(getString(R.string.ekey_allow_editting), edit);
		intent.putExtra(getString(R.string.ekey_product_id), productId);
		startActivity(intent);		
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		launchDetails(l, position, false);
	}

	@Override
	public void onLoadOperationCompleted(final LoadOperation op) {
		if (operationRequestId.get() == op.getOperationRequestId()) {
			ResourceServiceHelper.getInstance().stopService(this, false);
			restoreAndDisplayProductList(op.getResourceType(), op.getResourceParams());
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			loadProductList(true);
			return true;
		}
		return DefaultOptionsMenuHelper.onOptionsItemSelected(this, item);
	}

	@Override
	protected void onPause() {
		super.onPause();
		ResourceServiceHelper.getInstance().unregisterLoadOperationObserver(this);
		if (restoreAndDisplayTask != null) {
			restoreAndDisplayTask.setHost(null);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		selectedItemPos = state.getInt(getString(R.string.ekey_selected_item_pos), ListView.INVALID_POSITION);
	}

	@Override
	protected void onResume() {
		super.onResume();
		ResourceServiceHelper.getInstance().registerLoadOperationObserver(this);

		// do nothing more, if data is already displayed
		if (isDataDisplayed) {
			Log.d(TAG, "onResume(): Data is already displayed.");
			return;
		}

		// if there is currently ongoing restore task
		if (restoreAndDisplayTask != null) {
			restoreAndDisplayTask.setHost(this);
			if (restoreAndDisplayTask.isRunning()) {
				// wait
				Log.d(TAG, "restoreAndDisplayTask is currently running");
				return;
			} else {
				final List<Map<String, Object>> data = restoreAndDisplayTask.getData();
				if (data != null) {
					Log.d(TAG, "onResume(): dispaly data retrieved by calling restoreAndDisplayTask::getData()");
					displayData(data);
					return;
				}
			}
		}

		// is we get here, then there is no restore task currently executing no data displayed,
		// thus we should make a data load request
		if (ResourceServiceHelper.getInstance().isPending(operationRequestId.get())) {
			// good, we just need to wait for notification
		} else {
			// check if the resource is present, if yes -- load it, if not --
			// start new operation
			loadProductList();
		}
	}

	// following 2 methods enable the default options menu

	@Override
	public Object onRetainNonConfigurationInstance() {
		return restoreAndDisplayTask;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(getString(R.string.ekey_name_filter), getNameFilter());
		outState.putInt(getString(R.string.ekey_category_id), getCategoryId());
		outState.putInt(getString(R.string.ekey_operation_request_id), operationRequestId.get());
		outState.putInt(getString(R.string.ekey_selected_item_pos), getListView().getFirstVisiblePosition());
	}

	private synchronized void restoreAndDisplayProductList(int resType, String[] params) {
		if (restoreAndDisplayTask != null && restoreAndDisplayTask.isRunning()) {
			return;
		}
		restoreAndDisplayTask = new RestoreAndDisplayDataTask();
		restoreAndDisplayTask.execute(this, resType, params);
	}

	private void setCategoryId(Integer categoryId) {
		if (categoryId != null) {
			this.categoryId = categoryId;
		}
	}

	private void setNameFilter(String s) {
		nameFilter = s;
	}

}