package com.mageventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;

import com.mageventory.tasks.LoadProduct;
import com.mageventory.tasks.UpdateProduct;
import com.mageventory.util.Log;
import com.mageventory.util.Util;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.model.Category;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;
import com.mageventory.settings.Settings;
import com.mageventory.util.DefaultOptionsMenuHelper;

public class ProductEditActivity extends AbsProductActivity {

	AtomicInteger categoryAttributeLoadCount = null;

	void checkAllLoadedAndLoadProduct() {
		if (categoryAttributeLoadCount != null) {
			int count = categoryAttributeLoadCount.incrementAndGet();

			if (count == 2) {
				loadProduct(productSKU, false);
				categoryAttributeLoadCount = null;
			}
		}
	}

	@Override
	protected void onCategoryLoadSuccess() {
		super.onCategoryLoadSuccess();

		checkAllLoadedAndLoadProduct();
	}

	@Override
	protected void onAttributeSetLoadSuccess() {
		super.onAttributeSetLoadSuccess();

		checkAllLoadedAndLoadProduct();
	}

	// views
	public EditText descriptionV;
	public EditText priceV;
	public EditText quantityV;
	public EditText skuV;
	public EditText weightV;
	public CheckBox statusV;
	public EditText barcodeInput;
	private TextView attrFormatterStringV;

	// state
	private LoadProduct loadProductTask;
	private UpdateProduct updateProductTask;
	public int productId;
	public String productSKU;
	private int categoryId;
	private ProgressDialog progressDialog;
	private boolean customAttributesProductDataLoaded;

	public void dismissProgressDialog() {
		if (progressDialog == null) {
			return;
		}
		progressDialog.dismiss();
		progressDialog = null;
	}

	private Product getProduct() {
		if (loadProductTask == null) {
			return null;
		}
		return loadProductTask.getData();
	}

	private void loadProduct(final String productId, final boolean forceRefresh) {
		if (loadProductTask != null) {
			if (loadProductTask.getState() == TSTATE_RUNNING) {
				return;
			} else if (forceRefresh == false
					&& loadProductTask.getState() == TSTATE_TERMINATED
					&& loadProductTask.getData() != null) {
				return;
			}
		}
		if (loadProductTask != null) {
			loadProductTask.cancel(true);
		}
		loadProductTask = new LoadProduct();
		loadProductTask.setHost(this);
		loadProductTask.execute(productId, forceRefresh);
	}

	private void mapData(final Product p) {
		if (p == null) {
			return;
		}
		final Runnable map = new Runnable() {
			public void run() {
				try {
					categoryId = Integer.parseInt(p.getMaincategory());
				} catch (Throwable e) {
					categoryId = INVALID_CATEGORY_ID;
				}
				categoryV.setText("");

				final Map<String, Object> rootCategory = getCategories();
				if (rootCategory != null && !rootCategory.isEmpty()) {
					for (Category cat : Util
							.getCategorylist(rootCategory, null)) {
						if (cat.getId() == categoryId) {
							category = cat;
							categoryV.setText(cat.getFullName());
						}
					}
				}

				descriptionV.setText(p.getDescription());
				nameV.setText(p.getName());
				priceV.setText(p.getPrice());
				weightV.setText(p.getWeight().toString());
				statusV.setChecked(p.getStatus() == 1 ? true : false);
				skuV.setText(p.getSku());
				quantityV.setText(p.getQuantity().toString());

				String total = "";
				if (p.getQuantity().compareToIgnoreCase("") != 0) {
					total = String.valueOf(Double.valueOf(p.getPrice())
							* Double.valueOf(p.getQuantity()));
					String[] totalParts = total.split("\\.");
					if (totalParts.length > 1) {
						if ((!totalParts[1].contains("E"))
								&& (Integer.valueOf(totalParts[1]) == 0))
							total = totalParts[0];
					}
				}

				final int atrSetId = p.getAttributeSetId();
				selectAttributeSet(atrSetId, false, false);
			}
		};
		if (Looper.myLooper() == Looper.getMainLooper()) {
			map.run();
		} else {
			runOnUiThread(map);
		}
	}

	@Override
	protected void onAttributeListLoadSuccess() {
		super.onAttributeListLoadSuccess();

		final Product product = getProduct();
		if (product == null) {
			return;
		}

		final List<Map<String, Object>> atrs = getAttributeList();
		if (atrs == null) {
			return;
		}

		for (Map<String, Object> atr : atrs) {
			final String code = (String) atr
					.get(MAGEKEY_ATTRIBUTE_CODE_ATTRIBUTE_LIST_REQUEST);
			if (TextUtils.isEmpty(code)) {
				continue;
			}
			if (TextUtils.equals("product_barcode_", code)) {
				if (product.getData().containsKey(code)) {
					barcodeInput
							.setText(product.getData().get(code).toString());
				} else {
					barcodeInput.setText("");
				}
			}
		}

		if (customAttributesProductDataLoaded == false) {
			customAttributesProductDataLoaded = true;
			/* Load data from product into custom attribute fields just once. */
			if (customAttributesList.getList() != null) {
				for (CustomAttribute elem : customAttributesList.getList()) {
					elem.setSelectedValue(
							(String) product.getData().get(elem.getCode()),
							true);
				}
			}
		}

		String formatterString = customAttributesList
				.getUserReadableFormattingString();

		if (formatterString != null) {
			attrFormatterStringV.setVisibility(View.VISIBLE);
			attrFormatterStringV.setText(formatterString);
		} else {
			attrFormatterStringV.setVisibility(View.GONE);
		}
	}

	private OnLongClickListener scanBarcodeOnClickL = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			Intent scanInt = new Intent("com.google.zxing.client.android.SCAN");
			startActivityForResult(scanInt, SCAN_BARCODE);
			return true;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		setContentView(R.layout.product_edit);

		categoryAttributeLoadCount = new AtomicInteger(0);
		nameV = (EditText) findViewById(R.id.product_name_input);

		super.onCreate(savedInstanceState);

		// map views
		descriptionV = (EditText) findViewById(R.id.description_input);

		priceV = (EditText) findViewById(R.id.product_price_input);
		quantityV = (EditText) findViewById(R.id.quantity_input);
		skuV = (EditText) findViewById(R.id.product_sku_input);
		weightV = (EditText) findViewById(R.id.weight_input);
		statusV = (CheckBox) findViewById(R.id.status);
		attrFormatterStringV = (TextView) findViewById(R.id.attr_formatter_string);

		// extras
		final Bundle extras = getIntent().getExtras();
		if (extras == null) {
			throw new IllegalStateException();
		}
		productId = extras.getInt(getString(R.string.ekey_product_id),
				INVALID_PRODUCT_ID);
		productSKU = extras.getString(getString(R.string.ekey_product_sku));

		onProductLoadStart();

		// listeners

		// attributeSetV.setClickable(false); // attribute set cannot be changed
		attributeSetV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(getApplicationContext(),
						"Attribute set cannot be changed...",
						Toast.LENGTH_SHORT).show();
			}
		});

		findViewById(R.id.update_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (newAttributeOptionPendingCount == 0) {
					updateProduct(false);
				} else {
					Toast.makeText(getApplicationContext(),
							"Wait for options creation...", Toast.LENGTH_SHORT)
							.show();
				}
			}
		});

		barcodeInput = (EditText) findViewById(R.id.barcode_input);
		barcodeInput.setOnLongClickListener(scanBarcodeOnClickL);

	}

	public void onProductLoadFailure() {
		dismissProgressDialog();
	}

	private void onProductLoadStart() {
		showProgressDialog("Loading product...");
	}

	public void onProductLoadSuccess() {
		dismissProgressDialog();
		mapData(getProduct());
	}

	public void showProgressDialog(final String message) {
		if (isActive == false) {
			return;
		}
		if (progressDialog != null) {
			return;
		}
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(message);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.show();
	}

	private void updateProduct(boolean force) {
		if (force == false && updateProductTask != null
				&& updateProductTask.getState() == TSTATE_RUNNING) {
			return;
		}
		if (updateProductTask != null) {
			updateProductTask.cancel(true);
		}
		updateProductTask = new UpdateProduct();
		updateProductTask.setHost(this);
		updateProductTask.execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return DefaultOptionsMenuHelper.onCreateOptionsMenu(this, menu);
	}

	/**
	 * Handles the Scan Process Result --> Get Barcode result and set it in GUI
	 **/
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SCAN_BARCODE) {
			if (resultCode == RESULT_OK) {
				String contents = data.getStringExtra("SCAN_RESULT");
				// Set Barcode in Product Barcode TextBox
				barcodeInput.setText(contents);

			} else if (resultCode == RESULT_CANCELED) {
				// Do Nothing
			}
		}
	}
}
