package com.mageventory.activity;

import java.util.List;
import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;

import com.mageventory.R;
import com.mageventory.settings.Settings;
import com.mageventory.tasks.LoadProduct;
import com.mageventory.tasks.UpdateProduct;
import com.mageventory.util.Util;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.mageventory.R.id;
import com.mageventory.R.layout;
import com.mageventory.R.string;
import com.mageventory.activity.AbsProductActivity.ProductInfoLoader;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.Category;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.Product;
import com.mageventory.util.DefaultOptionsMenuHelper;
import android.widget.AutoCompleteTextView;

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
	public void onCategoryLoadSuccess() {
		super.onCategoryLoadSuccess();

		checkAllLoadedAndLoadProduct();
	}

	@Override
	public void onAttributeSetLoadSuccess() {
		super.onAttributeSetLoadSuccess();

		checkAllLoadedAndLoadProduct();
	}

	// views
	public EditText quantityV;
	public EditText weightV;
	private TextView attrFormatterStringV;

	// state
	private LoadProduct loadProductTask;
	public String productSKU;
	private int categoryId;
	private ProgressDialog progressDialog;
	private boolean customAttributesProductDataLoaded;

	private OnLongClickListener scanSKUOnClickL = new OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			Intent scanInt = new Intent("com.google.zxing.client.android.SCAN");
			startActivityForResult(scanInt, SCAN_QR_CODE);
			return true;
		}
	};
	
	public void dismissProgressDialog() {
		if (progressDialog == null) {
			return;
		}
		progressDialog.dismiss();
		progressDialog = null;
	}

	public Product getProduct() {
		if (loadProductTask == null) {
			return null;
		}
		return loadProductTask.getData();
	}

	private void loadProduct(final String productSKU, final boolean forceRefresh) {
		if (loadProductTask != null) {
			if (loadProductTask.getState() == TSTATE_RUNNING) {
				return;
			} else if (forceRefresh == false && loadProductTask.getState() == TSTATE_TERMINATED
					&& loadProductTask.getData() != null) {
				return;
			}
		}
		if (loadProductTask != null) {
			loadProductTask.cancel(true);
		}
		loadProductTask = new LoadProduct();
		loadProductTask.setHost(this);
		loadProductTask.execute(productSKU, forceRefresh);
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
				categoryV.setHint("");

				final Map<String, Object> rootCategory = getCategories();
				if (rootCategory != null && !rootCategory.isEmpty()) {
					for (Category cat : Util.getCategorylist(rootCategory, null)) {
						if (cat.getId() == categoryId) {
							category = cat;
							setCategoryText(category);
						}
					}
				}

				descriptionV.setText(p.getDescription());
				nameV.setText(p.getName());
				priceV.setText(p.getPrice());
				weightV.setText(p.getWeight().toString());
				statusV.setChecked(p.getStatus() == 1 ? true : false);
				skuV.setText(p.getSku());
				
				if (p.getManageStock() == 0)
				{
					quantityV.setText("");
				}
				else
				{
					double quantityValue = Double.parseDouble(p.getQuantity().toString());
					
					if ( Math.round(quantityValue) == quantityValue )
					{
						quantityV.setText("" + Math.round(quantityValue));	
					}
					else
					{
						quantityV.setText("" + quantityValue);	
					}
				}

				final int atrSetId = p.getAttributeSetId();
				selectAttributeSet(atrSetId, false, false, false);
			}
		};
		if (Looper.myLooper() == Looper.getMainLooper()) {
			map.run();
		} else {
			runOnUiThread(map);
		}
	}

	@Override
	public void onAttributeListLoadSuccess() {
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
			final String code = (String) atr.get(MAGEKEY_ATTRIBUTE_ATTRIBUTE_CODE);
			if (TextUtils.isEmpty(code)) {
				continue;
			}
			if (TextUtils.equals("product_barcode_", code)) {
				if (product.getData().containsKey(code)) {
					barcodeInput.setText(product.getData().get(code).toString());
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
					elem.setSelectedValue((String) product.getData().get(elem.getCode()), true);
				}
			}
		}

		String formatterString = customAttributesList.getUserReadableFormattingString();

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

		if (ENABLE_CATEGORIES)
		{
			categoryAttributeLoadCount = new AtomicInteger(0);
		}
		else
		{
			categoryAttributeLoadCount = new AtomicInteger(1);
		}
		
		nameV = (AutoCompleteTextView) findViewById(R.id.product_name_input);

		super.onCreate(savedInstanceState);

		// map views
		descriptionV = (AutoCompleteTextView) findViewById(R.id.description_input);

		quantityV = (EditText) findViewById(R.id.quantity_input);
		weightV = (EditText) findViewById(R.id.weight_input);
		attrFormatterStringV = (TextView) findViewById(R.id.attr_formatter_string);

		// extras
		final Bundle extras = getIntent().getExtras();
		if (extras == null) {
			throw new IllegalStateException();
		}
		productSKU = extras.getString(getString(R.string.ekey_product_sku));

		onProductLoadStart();

		// listeners

		// attributeSetV.setClickable(false); // attribute set cannot be changed
		attributeSetV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(getApplicationContext(), "Attribute set cannot be changed...", Toast.LENGTH_SHORT)
						.show();
			}
		});

		OnClickListener updateClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				ProductEditActivity.this.hideKeyboard();
				
				if (ENABLE_CATEGORIES && category == null)
				{
					showSelectProdCatDialog();
				}
				else
				{
					updateProduct();
				}
			}
		};
		
		OnEditorActionListener updateEditorActionListener = new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					InputMethodManager m = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					m.hideSoftInputFromWindow(v.getWindowToken(), 0);
					showUpdateConfirmationDialog();
		            return true;
		        }
				
				return false;
			}
		};		
		
		findViewById(R.id.update_btn).setOnClickListener(updateClickListener);
		
		nameV.setOnEditorActionListener(updateEditorActionListener);
		
		priceV.setOnEditorActionListener(updateEditorActionListener);
		skuV.setOnEditorActionListener(updateEditorActionListener);
		quantityV.setOnEditorActionListener(updateEditorActionListener);
		descriptionV.setOnEditorActionListener(updateEditorActionListener);
		barcodeInput.setOnEditorActionListener(updateEditorActionListener);
		weightV.setOnEditorActionListener(updateEditorActionListener);

		barcodeInput.setOnLongClickListener(scanBarcodeOnClickL);

		skuV.setOnLongClickListener(scanSKUOnClickL);
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
		if (isActivityAlive == false) {
			return;
		}
		if (progressDialog != null) {
			return;
		}
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(message);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(true);
		
		progressDialog.setButton(ProgressDialog.BUTTON1, "Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				progressDialog.cancel();
			}
		});
		
		progressDialog.show();
		
		progressDialog.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				ProductEditActivity.this.finish();
			}
		});
	}

	private void updateProduct() {
		if (newAttributeOptionPendingCount == 0) {
			showProgressDialog("Updating product...");
			UpdateProduct updateProductTask = new UpdateProduct(this);
			updateProductTask.execute();
		} else {
			Toast.makeText(getApplicationContext(), "Wait for options creation...", Toast.LENGTH_SHORT).show();
		}
	}

	public void showUpdateConfirmationDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Confirmation");
		alert.setMessage("Do you want do update the product now?");

		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				updateProduct();
			}
		});
		
		alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	public void showInvalidLabelDialog(final String settingsDomainName, final String skuDomainName) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Warning");
		alert.setMessage("Wrong label. Expected domain name: '" + settingsDomainName + "' found: '" + skuDomainName +"'" );

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ScanActivity.rememberDomainNamePair(settingsDomainName, skuDomainName);
			}
		});
		
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				skuV.setText("");
			}
		});

		AlertDialog srDialog = alert.create();
		
		srDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				skuV.setText("");
			}
		});
		
		srDialog.show();
	}
	
	/**
	 * Handles the Scan Process Result --> Get Barcode result and set it in GUI
	 **/
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == SCAN_QR_CODE)
		{
			if (resultCode == RESULT_OK)
			{
				skuScanCommon(data);
				
			} else if (resultCode == RESULT_CANCELED) {
				// Do Nothing
			}
		}
		
		if (requestCode == SCAN_BARCODE) {
			if (resultCode == RESULT_OK) {
				mGalleryTimestamp = JobCacheManager.getGalleryTimestampNow();
				
				String contents = data.getStringExtra("SCAN_RESULT");
				// Set Barcode in Product Barcode TextBox
				barcodeInput.setText(contents);
				
				weightV.requestFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
				imm.showSoftInput(weightV, 0);

			} else if (resultCode == RESULT_CANCELED) {
				// Do Nothing
			}
		}
	}
}
