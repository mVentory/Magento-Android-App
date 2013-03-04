package com.mageventory.activity;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.conn.SingleClientConnManager;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory.Options;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.LightingColorFilter;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.tasks.CreateOptionTask;
import com.mageventory.tasks.LoadImagePreviewFromServer;
import com.mageventory.util.Log;
import com.mageventory.util.SingleFrequencySoundGenerator;
import com.mageventory.util.Util;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.R.id;
import com.mageventory.R.layout;
import com.mageventory.R.string;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.activity.base.BaseActivityCommon;
import com.mageventory.components.ImageCachingManager;
import com.mageventory.components.ImagePreviewLayout;
import com.mageventory.components.LinkTextView;
import com.mageventory.components.ProductDetailsScrollView;
import com.mageventory.interfaces.IOnClickManageHandler;
import com.mageventory.interfaces.IScrollListener;
import com.mageventory.job.Job;
import com.mageventory.job.JobCacheManager;
import com.mageventory.job.JobCallback;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.job.JobService;
import com.mageventory.model.Category;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.util.DefaultOptionsMenuHelper;

public class ProductDetailsActivity extends BaseActivity implements MageventoryConstants, OperationObserver {

	private static final String TAG = "ProductDetailsActivity";

	private static final int PHOTO_EDIT_ACTIVITY_REQUEST_CODE = 0; // request
																	// code used
																	// to start
																	// the
																	// PhotoEditActivity
	private static final int CAMERA_ACTIVITY_REQUEST_CODE = 1; // request code
																// used to start
																// the Camera
																// activity
	private static final int TM_CATEGORY_LIST_ACTIVITY_REQUEST_CODE = 2;
	
	private static final String CURRENT_IMAGE_PATH_ATTR = "current_path";
	/*
	 * attribute used to save the current image path if a low memory event
	 * occures on a device and while in the camera mode, the current activity
	 * may be closed by the OS
	 */
	private static final int SOLD_CONFIRMATION_DIALOGUE = 1;
	private static final int SOLD_ORDER_SUCCESSEDED = 2;
	private static final int SHOW_MENU = 3;
	private static final int SHOW_DELETE_DIALOGUE = 4;
	private static final int SUBMIT_TO_TM_CONFIRMATION_DIALOGUE = 5;
	private static final int ADD_TO_CART_CONFIRMATION_DIALOGUE = 6;

	private static final String[] menuItems = { "Admin", "Delete", "Duplicate", "Edit", "List on TM", "Shop" };
	private static final int MITEM_ADMIN = 0;
	private static final int MITEM_DELETE = 1;
	private static final int MITEM_DUPLICATE = 2;
	private static final int MITEM_EDIT = 3;
	private static final int MITEM_LIST_ON_TM = 4;
	private static final int MITEM_SHOP = 5;
	
	private boolean isActivityAlive;

	private LayoutInflater inflater;

	public Job productCreationJob;
	public JobCallback productCreationJobCallback;
	
	public Job productEditJob;
	public JobCallback productEditJobCallback;
	
	public Job productSubmitToTMJob;
	public JobCallback productSubmitToTMJobCallback;

	// ArrayList<Category> categories;
	ProgressDialog progressDialog;
	MyApplication app;

	// Activity activity = null;

	boolean refreshImages = false;
	String currentImgPath; // this will actually be: path + "/imageName"
	public LinearLayout imagesLayout; // the layout which will contain
								// ImagePreviewLayout objects
	boolean resultReceived = false;
	ProductDetailsScrollView scrollView;
	Button photoShootBtnTop;
	Button photoShootBtnBottom;
	ProgressBar imagesLoadingProgressBar;
	ScrollView scroller;

	ClickManageImageListener onClickManageImageListener;
	ScrollListener scrollListener;

	// detail views
	private TextView nameInputView;
	private TextView priceInputView;
	private TextView quantityInputView;
	private TextView descriptionInputView;
	private CheckBox statusView;
	private TextView weightInputView;
	private Button soldButtonView;
	private Button addToCartButtonView;
	private Button listOnTMButton;
	private Button hideTMSectionButton;
	private Button selectTMCategoryButton;
	private TextView categoryView;
	private TextView skuTextView;
	private LinearLayout layoutCreationRequestPending;
	private LinearLayout layoutSubmitToTMRequestPending;
	private ProgressBar submitToTMRequestPendingProgressBar;
	private LinearLayout layoutSellRequestPending;
	private LinearLayout layoutSellRequestFailed;
	private TextView textViewSellRequestPending;
	private TextView textViewSellRequestFailed;
	private LinearLayout layoutEditRequestPending;
	private TextView creationOperationPendingText;
	private TextView editOperationPendingText;
	private TextView submitToTMOperationPendingText;
	private LinearLayout submitToTMLayout;
	private TextView selectedTMCategoryTextView;
	private LinearLayout layoutAddToCartRequestPending;
	private LinearLayout layoutAddToCartRequestFailed;
	private TextView textViewAddToCartRequestPending;
	private TextView textViewAddToCartRequestFailed;
	private EditText priceEdit;
	private EditText qtyEdit;
	private EditText totalEdit;
	
	/* Whether the user chooses to see the layout or not. */
	private boolean submitToTMLayoutVisible = false;
	
	private JobControlInterface mJobControlInterface;

	// product data
	private String productSKU;
	public Product instance;

	// resources
	private int loadRequestId = INVALID_REQUEST_ID;
	private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
	private boolean detailsDisplayed = false;
	private int deleteProductID = INVALID_REQUEST_ID;
	private int catReqId = INVALID_REQUEST_ID;
	private int selectedTMCategoryID = 0;

	// y XXX: rework the IMAGE LOADING TASK
	private Set<String> loadedImages = new HashSet<String>();

	private List<Job> mSellJobs;
	private List<Job> mAddToCartJobs;
	
	public Settings mSettings;
	
	private SingleFrequencySoundGenerator mDetailsLoadSuccessSound = new SingleFrequencySoundGenerator(1500, 200);
	
	/* Was this activity opened as a result of scanning a product by means of "Scan" option from the menu. */
	private boolean mOpenedAsAResultOfScanning;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isActivityAlive = true;
		
		mSettings = new Settings(this);
		
		setContentView(R.layout.product_details); // y XXX: REUSE THE PRODUCT
													// CREATION / DETAILS
													// VIEW...

		mJobControlInterface = new JobControlInterface(this);

		// map views
		nameInputView = (TextView) findViewById(R.id.product_name_input);
		priceInputView = (TextView) findViewById(R.id.product_price_input);
		quantityInputView = (TextView) findViewById(R.id.quantity_input);
		descriptionInputView = (TextView) findViewById(R.id.product_description_input);
		statusView = (CheckBox) findViewById(R.id.enabledCheckBox);
		weightInputView = (TextView) findViewById(R.id.weigthOutputTextView);
		categoryView = (TextView) findViewById(R.id.product_categories);
		skuTextView = (TextView) findViewById(R.id.details_sku);
		layoutCreationRequestPending = (LinearLayout) findViewById(R.id.layoutRequestPending);
		layoutSubmitToTMRequestPending = (LinearLayout) findViewById(R.id.layoutSubmitToTMRequestPending);
		submitToTMRequestPendingProgressBar = (ProgressBar) findViewById(R.id.submitToTMRequestPendingProgressBar);
		layoutSellRequestPending = (LinearLayout) findViewById(R.id.layoutSellRequestPending);
		layoutSellRequestFailed = (LinearLayout) findViewById(R.id.layoutSellRequestFailed);
		textViewSellRequestPending = (TextView) findViewById(R.id.textViewSellRequestPending);
		textViewSellRequestFailed = (TextView) findViewById(R.id.textViewSellRequestFailed);
		layoutAddToCartRequestPending = (LinearLayout) findViewById(R.id.layoutAddToCartRequestPending);
		layoutAddToCartRequestFailed = (LinearLayout) findViewById(R.id.layoutAddToCartRequestFailed);
		textViewAddToCartRequestPending = (TextView) findViewById(R.id.textViewAddToCartRequestPending);
		textViewAddToCartRequestFailed = (TextView) findViewById(R.id.textViewAddToCartRequestFailed);
		layoutEditRequestPending = (LinearLayout) findViewById(R.id.layoutEditRequestPending);
		creationOperationPendingText = (TextView) findViewById(R.id.creationOperationPendingText);
		editOperationPendingText = (TextView) findViewById(R.id.editOperationPendingText);
		submitToTMOperationPendingText = (TextView) findViewById(R.id.submitToTMOperationPendingText);
		submitToTMLayout = (LinearLayout) findViewById(R.id.submitToTMLayout);
		selectedTMCategoryTextView = (TextView) findViewById(R.id.selectedTMCategoryTextView);
		priceEdit = (EditText) findViewById(R.id.price_edit);
		qtyEdit = (EditText) findViewById(R.id.qty_edit);
		totalEdit = (EditText) findViewById(R.id.total_edit);
		
		photoShootBtnTop = (Button) findViewById(R.id.photoshootTopButton);
		photoShootBtnBottom = (Button) findViewById(R.id.photoShootBtn);
				
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			productSKU = extras.getString(getString(R.string.ekey_product_sku));
			mOpenedAsAResultOfScanning = extras.getBoolean(getString(R.string.ekey_prod_det_launched_from_menu_scan));
			
			boolean newProduct = extras.getBoolean(getString(R.string.ekey_new_product));
			if (newProduct == false)
			{
				photoShootBtnTop.setVisibility(View.GONE);
			}
		}
		
		/* The product sku must be passed to this activity */
		if (productSKU == null)
			finish();

		if (JobCacheManager.saveRangeStart(productSKU, mSettings.getProfileID()) == false)
		{
			showTimestampRecordingError(this);
		}
		
		// retrieve last instance
		instance = (Product) getLastNonConfigurationInstance();

		app = (MyApplication) getApplication();

		this.setTitle("mVentory: Product Details");

		imagesLoadingProgressBar = (ProgressBar) findViewById(R.id.imagesLoadingProgressBar);
		scroller = (ScrollView) findViewById(R.id.scrollView1);

		((Button) findViewById(R.id.soldButton)).getBackground().setColorFilter(
				new LightingColorFilter(0x444444, 0x737575));
		
		((Button) findViewById(R.id.addToCartButton)).getBackground().setColorFilter(
				new LightingColorFilter(0x444444, 0x737575));

		scrollListener = new ScrollListener(this);

		scrollView = (ProductDetailsScrollView) findViewById(R.id.scrollView1);
		scrollView.setScrollToBottomListener(scrollListener);

		onClickManageImageListener = new ClickManageImageListener(this);
		
		hideTMSectionButton = (Button) findViewById(R.id.hideTMSection);
		hideTMSectionButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ProductDetailsActivity.this.hideKeyboard();
				submitToTMLayoutVisible = false;
				submitToTMLayout.setVisibility(View.GONE);
			}
		});		
		
		
		listOnTMButton = (Button) findViewById(R.id.listOnTMButton);
		listOnTMButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (instance == null)
					return;

				ProductDetailsActivity.this.hideKeyboard();
				// Show Confirmation Dialogue
				showDialog(SUBMIT_TO_TM_CONFIRMATION_DIALOGUE);
			}
		});		
		
		// Set the Sold Button Action
		soldButtonView = (Button) findViewById(R.id.soldButton);
		soldButtonView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (instance == null)
					return;

				ProductDetailsActivity.this.hideKeyboard();
				// Show Confirmation Dialogue
				showDialog(SOLD_CONFIRMATION_DIALOGUE);
			}
		});

		addToCartButtonView = (Button) findViewById(R.id.addToCartButton);
		addToCartButtonView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (instance == null)
					return;

				ProductDetailsActivity.this.hideKeyboard();
				showDialog(ADD_TO_CART_CONFIRMATION_DIALOGUE);
			}
		});
		
		selectTMCategoryButton = (Button) findViewById(R.id.selectTMCategoryButton);
		
		selectTMCategoryButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (instance == null || instance.getTMPreselectedCategoriesMap() == null)
					return;
				
				ProductDetailsActivity.this.hideKeyboard();
				
				Intent i = new Intent(ProductDetailsActivity.this, TMCategoryListActivity.class);
				i.putExtra(TMCategoryListActivity.CATEGORIES_MAP_PARAM_KEY, (Serializable)instance.getTMPreselectedCategoriesMap());
				startActivityForResult(i, TM_CATEGORY_LIST_ACTIVITY_REQUEST_CODE);
			}
		});
		
		priceEdit.addTextChangedListener(evaluteTotalTextWatcher);
		qtyEdit.addTextChangedListener(evaluteTotalTextWatcher);

		totalEdit.addTextChangedListener(evaluatePriceTextWatcher);
		
		/*
		 * Check CustomerVliad If not Valid Customer
		 * "Disable SoldPrice,Qty and Sold Price"
		 */
		Settings settings = new Settings(getApplicationContext());
		if (settings.hasSettings()) {
			if (!settings.getCustomerValid()) {
				soldButtonView.setVisibility(View.GONE);
				priceEdit.setVisibility(View.GONE);
				qtyEdit.setVisibility(View.GONE);
				totalEdit.setVisibility(View.GONE);
			}
		}

		Button menuBtn = (Button) findViewById(R.id.menuButton);
		menuBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				ProductDetailsActivity.this.hideKeyboard();
				showDialog(SHOW_MENU);
			}
		});

		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

		mSellJobs = JobCacheManager.restoreSellJobs(productSKU, mSettings.getUrl());
		mAddToCartJobs = JobCacheManager.restoreAddToCartJobs(productSKU, mSettings.getUrl());
		
		productCreationJob = JobCacheManager.restoreProductCreationJob(productSKU, mSettings.getUrl());
		productEditJob = JobCacheManager.restoreEditJob(productSKU, mSettings.getUrl());
		productSubmitToTMJob = JobCacheManager.restoreSubmitToTMJob(productSKU, mSettings.getUrl());
		
		View.OnClickListener photoShootButtonListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (instance != null) {
					ProductDetailsActivity.this.hideKeyboard();
					startCameraActivity();
				}
			}
		};
		
		photoShootBtnTop.setOnClickListener(photoShootButtonListener);
		photoShootBtnBottom.setOnClickListener(photoShootButtonListener);
		
		((CheckBox)findViewById(R.id.relist_checkbox)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				ProductDetailsActivity.this.hideKeyboard();
			}
		});
			
		((CheckBox)findViewById(R.id.allowbuynow_checkbox)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				ProductDetailsActivity.this.hideKeyboard();
			}
		});
			
		((CheckBox)findViewById(R.id.addtmfees_checkbox)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				ProductDetailsActivity.this.hideKeyboard();
			}
		});
	}
	
	public static void showTimestampRecordingError(Context c) {
		
		AlertDialog.Builder alert = new AlertDialog.Builder(c);
			
		alert.setTitle("Error");
		alert.setMessage("Cannot record timestamps. Do not take external photos.");
			
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
			
		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	public void showGalleryFolderNotExistsError() {
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
		alert.setTitle("Error");
		alert.setMessage("The gallery directory specified in the settings (" + mSettings.getGalleryPhotosDirectory() + ") does not exist.");
			
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
			
		AlertDialog srDialog = alert.create();
		srDialog.show();
	}

	@Override
	protected void onDestroy() {
		isActivityAlive = false;
		super.onDestroy();
	}
	
	@Override
	public void onBackPressed() {
		/* Leaving product details breaks the NewNewReloadCycle */
		BaseActivityCommon.mNewNewReloadCycle = false;
		
		super.onBackPressed();
	}
	
	private void updateUIWithAddToCartJobs()
	{
		int pendingCount = 0;
		int failedCount = 0;
		
		for(Job job: mAddToCartJobs)
		{
			if (job.getPending() == true)
			{
				pendingCount ++;
			}
			else
			{
				failedCount ++;
			}
		}
		
		if (pendingCount > 0)
		{
			textViewAddToCartRequestPending.setText("Adding to cart pending ("+ pendingCount +")");
			layoutAddToCartRequestPending.setVisibility(View.VISIBLE);	
		}
		else
		{
			layoutAddToCartRequestPending.setVisibility(View.GONE);
		}
		
		if (failedCount > 0)
		{
			textViewAddToCartRequestFailed.setText("Adding to cart failed ("+ failedCount +")");
			layoutAddToCartRequestFailed.setVisibility(View.VISIBLE);	
		}
		else
		{
			layoutAddToCartRequestFailed.setVisibility(View.GONE);
		}
	}

	
	/* Show spinning icon if there are any sell jobs etc. */
	private void updateUIWithSellJobs(Product prod)
	{
		if (prod != null)
		{
			double oldQuantity = Double.parseDouble(prod.getQuantity());
			
			/* Calculate quantity value after all sell jobs come through. */
			double newQuantity = oldQuantity;
			
			for(Job job: mSellJobs)
			{
				if (job.getPending() == true)
				{
					String quantity = "0";
					
					if (job.getJobID().getJobType() == RES_SELL_MULTIPLE_PRODUCTS)
					{
						Object[] productsToSell = (Object[])job.getExtras().get(EKEY_PRODUCTS_TO_SELL_ARRAY);
						String[] productsSKUs = (String[])job.getExtras().get(EKEY_PRODUCT_SKUS_TO_SELL_ARRAY);
						
						for(int i=0; i<productsSKUs.length; i++)
						{
							if (productsSKUs[i].equals(productSKU))
							{
								Map<String, Object> prodMap = (Map<String, Object>)productsToSell[i];
								
								quantity = "" + (Double)prodMap.get(MAGEKEY_PRODUCT_QUANTITY);
								break;
							}
						}
						
					}
					else
					{
						quantity = (String)job.getExtraInfo(MAGEKEY_PRODUCT_QUANTITY);
					}
					
					newQuantity -= Double.parseDouble(quantity);
				}
			}
			
			if (productCreationJob != null)
			{
				Boolean isQuickSellMode = (Boolean)productCreationJob.getExtraInfo(EKEY_QUICKSELLMODE);

				if (isQuickSellMode == true)
				{
					newQuantity -= Double.parseDouble((String)productCreationJob.getExtraInfo(MAGEKEY_PRODUCT_QUANTITY));
				}
			}
			
			if (instance != null && instance.getManageStock() == 0)
			{
				//infinity character
				quantityInputView.setText("" + '\u221E');
			}
			else
			{
				StringBuilder quantityInputString = new StringBuilder();
				
				if (newQuantity != oldQuantity)
				{
					if ( Math.round(newQuantity) == Math.round(newQuantity * 10000) / 10000.0 )
						quantityInputString.append(Math.round(newQuantity));
					else
						quantityInputString.append(Math.round(newQuantity * 10000) / 10000.0);
					
					quantityInputString.append("/");
				}
				
				if ( Math.round(oldQuantity) == oldQuantity )
					quantityInputString.append(Math.round(oldQuantity));
				else
					quantityInputString.append(oldQuantity);
				
				double userQty = 0;
				
				try
				{
					userQty = Double.parseDouble(qtyEdit.getText().toString());
				}
				catch(NumberFormatException n)
				{
				}
				
				if (userQty > newQuantity)
				{
					if ( Math.round(newQuantity) == newQuantity )
						qtyEdit.setText("" + Math.round(newQuantity));
					else
						qtyEdit.setText("" + Math.round(newQuantity * 10000) / 10000.0);
				}
				
				quantityInputView.setText(quantityInputString.toString());
			}
		}
		
		int pendingCount = 0;
		int failedCount = 0;
		
		for(Job job: mSellJobs)
		{
			if (job.getPending() == true)
			{
				pendingCount ++;
			}
			else
			{
				failedCount ++;
			}
		}
		
		if (pendingCount > 0)
		{
			textViewSellRequestPending.setText("Sell is pending ("+ pendingCount +")");
			layoutSellRequestPending.setVisibility(View.VISIBLE);	
		}
		else
		{
			layoutSellRequestPending.setVisibility(View.GONE);
		}
		
		if (failedCount > 0)
		{
			textViewSellRequestFailed.setText("Sell failed ("+ failedCount +")");
			layoutSellRequestFailed.setVisibility(View.VISIBLE);	
		}
		else
		{
			layoutSellRequestFailed.setVisibility(View.GONE);
		}
	}
	
	/* A callback that is going to be called when a state of the job changes. If the job finishes
	then the callback reloads the product details activity. */
	private JobCallback newAddToCartJobCallback()
	{
		return new JobCallback() {

			final JobCallback thisCallback = this;
			
			@Override
			public void onJobStateChange(final Job job) {
				ProductDetailsActivity.this.runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						if (job.getFinished() == true)
						{
							for(int i=0; i<mAddToCartJobs.size(); i++)
							{
								if (job.getJobID().getTimeStamp() == mAddToCartJobs.get(i).getJobID().getTimeStamp())
								{
									mAddToCartJobs.remove(i);
									mJobControlInterface.deregisterJobCallback(job.getJobID(), thisCallback);
									break;
								}
							}
							updateUIWithAddToCartJobs();
						}
						else
						if (job.getPending() == false)
						{
							for(int i=0; i<mAddToCartJobs.size(); i++)
							{
								if (job.getJobID().getTimeStamp() == mAddToCartJobs.get(i).getJobID().getTimeStamp())
								{
									mAddToCartJobs.set(i, job);
									break;
								}
							}
							
							updateUIWithAddToCartJobs();
						}
					}
					
				});
			}
		};
	}	
	
	
	/* A callback that is going to be called when a state of the job changes. If the job finishes
		then the callback reloads the product details activity. */
	private JobCallback newSellJobCallback()
	{
		return new JobCallback() {

			final JobCallback thisCallback = this;
			
			@Override
			public void onJobStateChange(final Job job) {
				ProductDetailsActivity.this.runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						if (job.getFinished() == true)
						{
							for(int i=0; i<mSellJobs.size(); i++)
							{
								if (job.getJobID().getTimeStamp() == mSellJobs.get(i).getJobID().getTimeStamp())
								{
									mSellJobs.remove(i);
									mJobControlInterface.deregisterJobCallback(job.getJobID(), thisCallback);
									break;
								}
							}
							loadDetails();
						}
						else
						if (job.getPending() == false)
						{
							for(int i=0; i<mSellJobs.size(); i++)
							{
								if (job.getJobID().getTimeStamp() == mSellJobs.get(i).getJobID().getTimeStamp())
								{
									mSellJobs.set(i, job);
									break;
								}
							}
							
							if (instance != null)
								updateUIWithSellJobs(instance);
						}
					}
					
				});
			}
		};
	}
	
	private JobCallback newSubmitToTMCallback()
	{
		return new JobCallback() {
			@Override
			public void onJobStateChange(final Job job) {
				if (job.getFinished()) {
					ProductDetailsActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							productSubmitToTMJob = null;
							mJobControlInterface.deregisterJobCallback(job.getJobID(), productSubmitToTMJobCallback);
							Log.d(TAG, "Hiding a submit to TM request pending indicator for job: " + " timestamp="
									+ job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
									+ " prodID=" + job.getJobID().getProductID() + " SKU="
									+ job.getJobID().getSKU());
							layoutSubmitToTMRequestPending.setVisibility(View.GONE);
							loadDetails(false, false);
						}
					});
				}
				else
				if (job.getPending() == false)
				{
					productSubmitToTMJob = job;
					ProductDetailsActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							showSubmitToTMJobUIInfo(job);
						}
					});
				}
			}
		};
	}
	
	private void registerSellJobCallbacks()
	{
		boolean needRefresh = false;
		
		Iterator<Job> i = mSellJobs.iterator();
		while (i.hasNext()) {
			Job job = i.next();
			if (mJobControlInterface.registerJobCallback(job.getJobID(), newSellJobCallback()) == false)
			{
				needRefresh = true;
				i.remove();
			}
		}
		
		if (needRefresh)
		{
			loadDetails();
		}
	}
	
	private void unregisterSellJobCallbacks()
	{
		for(Job job : mSellJobs)
		{
			mJobControlInterface.deregisterJobCallback(job.getJobID(), null);
		}
	}
	
	private void registerAddToCartJobCallbacks()
	{
		boolean needRefresh = false;
		
		Iterator<Job> i = mAddToCartJobs.iterator();
		while (i.hasNext()) {
			Job job = i.next();
			if (mJobControlInterface.registerJobCallback(job.getJobID(), newAddToCartJobCallback()) == false)
			{
				needRefresh = true;
				i.remove();
			}
		}
		
		if (needRefresh)
		{
			loadDetails();
		}
	}
	
	private void unregisterAddToCartJobCallbacks()
	{
		for(Job job : mAddToCartJobs)
		{
			mJobControlInterface.deregisterJobCallback(job.getJobID(), null);
		}
	}
	
	private void showSubmitToTMJobUIInfo(Job job)
	{
		if (job.getPending() == false)
		{
			if (job.getException() != null && job.getException().getMessage() != null)
			{
				submitToTMOperationPendingText.setText("TM submit failed: " + job.getException().getMessage());	
			}
			else
			{
				submitToTMOperationPendingText.setText("TM submit failed: Unknown error.");
			}
			
			submitToTMRequestPendingProgressBar.setVisibility(View.GONE);
		}
		else
		{
			submitToTMOperationPendingText.setText("Submitting to TM...");
			submitToTMRequestPendingProgressBar.setVisibility(View.VISIBLE);
		}
		
		layoutSubmitToTMRequestPending.setVisibility(View.VISIBLE);
	}
	
	private void showProductCreationJobUIInfo(Job job)
	{
		Boolean isQuickSellMode = (Boolean)job.getExtraInfo(EKEY_QUICKSELLMODE);
		
		if (isQuickSellMode.booleanValue() == true)
		{
			if (job.getPending() == false)
			{
				creationOperationPendingText.setText("Express sale failed...");
			}
			else
			{
				creationOperationPendingText.setText("Express sale pending...");
			}
		}
		else
		{
			if (job.getPending() == false)
			{
				creationOperationPendingText.setText("Creation failed...");
			}
			else
			{
				creationOperationPendingText.setText("Creation pending...");	
			}
		}
		
		layoutCreationRequestPending.setVisibility(View.VISIBLE);
	}
	
	private void showProductEditJobUIInfo(Job job)
	{
		if (job.getPending() == false)
		{
			editOperationPendingText.setText("Edit failed...");
		}
		else
		{
			editOperationPendingText.setText("Edit pending...");	
		}
		
		layoutEditRequestPending.setVisibility(View.VISIBLE);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "> onResume()");
		
		registerSellJobCallbacks();
		registerAddToCartJobCallbacks();

		resHelper.registerLoadOperationObserver(this);

		// this happens when OS is killing this activity (e.g. if user goes to
		// Camera activity) and we don't need to load all product details, only
		// the images
		if (!resultReceived) {
			imagesLayout = (LinearLayout) findViewById(R.id.imagesLinearLayout);
		}

		for (int i = 0; i < imagesLayout.getChildCount(); i++) {
			((ImagePreviewLayout) imagesLayout.getChildAt(i)).registerCallbacks(mJobControlInterface);
		}
		
		if (productSubmitToTMJob != null) {
			productSubmitToTMJobCallback = newSubmitToTMCallback();
			
			showSubmitToTMJobUIInfo(productSubmitToTMJob);

			if (!mJobControlInterface.registerJobCallback(productSubmitToTMJob.getJobID(), productSubmitToTMJobCallback)) {
				layoutSubmitToTMRequestPending.setVisibility(View.GONE);
				productSubmitToTMJobCallback = null;
				productSubmitToTMJob = null;
				loadDetails();
			}
		}		
		
		/* Show a spinning wheel with information that there is product creation pending and also register a callback
		 * on that product creation job. */

		if (productCreationJob != null) {
			productCreationJobCallback = new JobCallback() {
				@Override
				public void onJobStateChange(final Job job) {
					if (job.getFinished()) {
						ProductDetailsActivity.this.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								productCreationJob = null;
								mJobControlInterface.deregisterJobCallback(job.getJobID(), productCreationJobCallback);
								Log.d(TAG, "Hiding a new product request pending indicator for job: " + " timestamp="
										+ job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
										+ " prodID=" + job.getJobID().getProductID() + " SKU="
										+ job.getJobID().getSKU());
								layoutCreationRequestPending.setVisibility(View.GONE);
								loadDetails(false, false);
							}
						});
					}
					else
					if (job.getPending() == false)
					{
						ProductDetailsActivity.this.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								showProductCreationJobUIInfo(job);
							}
						});
					}
				}
			};
			
			showProductCreationJobUIInfo(productCreationJob);

			if (!mJobControlInterface.registerJobCallback(productCreationJob.getJobID(), productCreationJobCallback)) {
				layoutCreationRequestPending.setVisibility(View.GONE);
				productCreationJobCallback = null;
				productCreationJob = null;
				loadDetails();
			}
		}
		
		/* Show a spinning wheel with information that there is edit creation pending and also register a callback
		 * on that product edit job. */

		if (productEditJob != null) {
			productEditJobCallback = new JobCallback() {
				@Override
				public void onJobStateChange(final Job job) {
					/* If the edit job either succeeded or was moved to the failed table then hide the spinning wheel
					 * and refresh product details. */
					if (job.getFinished()) {
						ProductDetailsActivity.this.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								productEditJob = null;
								mJobControlInterface.deregisterJobCallback(job.getJobID(), productEditJobCallback);
								Log.d(TAG, "Hiding an edit product request pending indicator for job: " + " timestamp="
										+ job.getJobID().getTimeStamp() + " jobtype=" + job.getJobID().getJobType()
										+ " prodID=" + job.getJobID().getProductID() + " SKU="
										+ job.getJobID().getSKU());
								layoutEditRequestPending.setVisibility(View.GONE);
								
								productSKU = (String)job.getExtraInfo(MAGEKEY_PRODUCT_SKU);
								
								loadDetails(false, false);
							}
						});
					}
					else
					if (job.getPending() == false)
					{
						ProductDetailsActivity.this.runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								showProductEditJobUIInfo(job);
							}
						});
					}
				}
			};

			showProductEditJobUIInfo(productEditJob);

			if (!mJobControlInterface.registerJobCallback(productEditJob.getJobID(), productEditJobCallback)) {
				layoutEditRequestPending.setVisibility(View.GONE);
				productEditJobCallback = null;
				productEditJob = null;
				loadDetails();
			}

		}
		
		if (detailsDisplayed == false) {
			loadDetails();
		}
		
		Log.d(TAG, "< onResume()");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "> onPause()");
		
		unregisterSellJobCallbacks();
		unregisterAddToCartJobCallbacks();

		resHelper.unregisterLoadOperationObserver(this);

		for (int i = 0; i < imagesLayout.getChildCount(); i++) {
			((ImagePreviewLayout) imagesLayout.getChildAt(i)).deregisterCallbacks(mJobControlInterface);
		}

		if (productCreationJob != null && productCreationJobCallback != null) {
			mJobControlInterface.deregisterJobCallback(productCreationJob.getJobID(), productCreationJobCallback);
		}
		
		if (productEditJob!= null && productEditJobCallback != null) {
			mJobControlInterface.deregisterJobCallback(productEditJob.getJobID(), productEditJobCallback);
		}

		Log.d(TAG, "< onPause()");
	}

	/* Show a dialog saying that there is a product creation job or send to TM job pending and a product can't be listed on TM at the moment */
	public void showProductCreationOrSendToTMJobPending() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
		alert.setTitle("Information");
		alert.setMessage("Cannot proceed with this operation because there is either a \"product creation\" job or \"send to TM\" job pending.");
			
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
			
		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	/* Show a dialog saying that the product is already listed on TM. */
	public void showProductAlreadyListedDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
		alert.setTitle("Information");
		alert.setMessage("Cannot proceed with this operation. The product is already listed on TM.");
			
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
			
		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	/* Show an warning dialog saying that there are sell requests pending only if there are such requests pending.
	 * This is used in case of deleting or editing a product. */
	public void showEditDeleteWarningDialog(final boolean edit) {
		
		if (mSellJobs.size() > 0)
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
			alert.setTitle("Warning");
			
			if (edit)
			{
				alert.setMessage("There are sell jobs in progress. Are you sure you want to edit the product now?");
			}
			else
			{
				alert.setMessage("There are sell jobs in progress. The results of deleting a product now are unpredictable. Do you want to continue?");	
			}
			
			alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					if (edit)
					{
						startEditActivity();
					}
					else
					{
						showDialog(SHOW_DELETE_DIALOGUE);	
					}
				}
			});
			
			alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// Do nothing
				}
			});
			
			AlertDialog srDialog = alert.create();
			srDialog.show();
		}
		else
		{
			if (edit)
			{
				startEditActivity();
			}
			else
			{
				showDialog(SHOW_DELETE_DIALOGUE);	
			}
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {

			loadDetails(true, true);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onLoadOperationCompleted(LoadOperation op) {

		if (op.getOperationRequestId() == deleteProductID) {
			dismissProgressDialog();
			Intent intent = new Intent();
			intent.putExtra("ComingFrom", "Hello");
			setResult(RESULT_CHANGE, intent);
			finish();
			return;
		}

		if (op.getOperationRequestId() != loadRequestId && op.getOperationRequestId() != catReqId) {
			return;
		}
		if (op.getException() != null) {
			dismissProgressDialog();
			Toast.makeText(this, "" + op.getException(), Toast.LENGTH_LONG).show();
			
			return;
		}

		if (catReqId == op.getOperationRequestId()) {
			loadDetails(true, false);
		} else if (loadRequestId == op.getOperationRequestId()) {
			loadDetails();
		} 

	}

	private void mapData(final Product p, final Map<String, Object> categories) {
		if (p == null) {
			return;
		}
		final Runnable map = new Runnable() {
			public void run() {

				categoryView.setText("");
				int categoryId;

				try {
					categoryId = Integer.parseInt(p.getMaincategory());
				} catch (Throwable e) {
					categoryId = INVALID_CATEGORY_ID;
				}

				if (categories != null && !categories.isEmpty() && p.getMaincategory() != null) {
					List<Category> list = Util.getCategorylist(categories, null);

					for (Category cat : list) {
						if (cat.getId() == categoryId) {
							categoryView.setText(cat.getFullName());
						}
					}
				}
				
				if (p.getIsQtyDecimal() == 1)
				{
					qtyEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
				}
				else
				{
					qtyEdit.setInputType(InputType.TYPE_CLASS_NUMBER);					
				}

				descriptionInputView.setText(p.getDescription());
				
				if (descriptionInputView.getText().toString().equalsIgnoreCase("n/a"))
				{
					descriptionInputView.setVisibility(View.GONE);
				}
				else
				{
					descriptionInputView.setVisibility(View.VISIBLE);
				}
				
				nameInputView.setText(p.getName());
				weightInputView.setText(p.getWeight().toString());
				statusView.setChecked(p.getStatus() == 1 ? true : false);
				skuTextView.setText(p.getSku());
				priceInputView.setText(p.getPrice());
				
				if (p.getManageStock() == 0)
				{
					//infinity character
					quantityInputView.setText("" + '\u221E');	
				}
				else
				{
					quantityInputView.setText(p.getQuantity().toString());	
				}

				if (TextUtils.isEmpty(priceEdit.getText())) {
					priceEdit.setText(p.getPrice());
					priceEdit.setSelection(priceEdit.getText().length());
				}

				String total = "";
				if (p.getQuantity().compareToIgnoreCase("") != 0) {
					total = String.valueOf(Float.valueOf(p.getPrice()) * Float.valueOf(p.getQuantity()));
					String[] totalParts = total.split("\\.");
					if (totalParts.length > 1) {
						if ((!totalParts[1].contains("E")) && (Integer.valueOf(totalParts[1]) == 0))
							total = totalParts[0];
					}
				}
				
				if (p.getIsQtyDecimal() == 1)
				{
					((TextView) findViewById(R.id.total_input)).setText("" + (Math.round(Double.parseDouble(total) * 10000) / 10000.0));
				}
				else
				{
					((TextView) findViewById(R.id.total_input)).setText("" + (Math.round(Double.parseDouble(total))));
				}

				// Show Attributes
				((LinearLayout) findViewById(R.id.barcode_layout)).setVisibility(View.GONE);
				
				ViewGroup vg = (ViewGroup) findViewById(R.id.details_attr_list);
				vg.removeAllViewsInLayout();
				View thumbnailView = null;
				for (int i = 0; i < p.getAttrList().size(); i++) {
					if (TextUtils.equals(p.getAttrList().get(i).getLabel(), "Barcode")) {
						TextView barcodeText = (TextView) findViewById(R.id.details_barcode);
						String barcodeString = p.getAttrList().get(i).getValueLabel();
						barcodeText.setText(p.getAttrList().get(i).getValueLabel());
						
						if (barcodeString.length() >= 5)
						{
							((LinearLayout) findViewById(R.id.barcode_layout)).setVisibility(View.VISIBLE);
						}
						
					} else {

						View v = inflater.inflate(R.layout.product_attribute_view, null);

						TextView label = (TextView) v.findViewById(R.id.attrLabel);
						TextView value = (TextView) v.findViewById(R.id.attrValue);
						label.setText(p.getAttrList().get(i).getLabel());
						value.setText(p.getAttrList().get(i).getValueLabel());

						if (p.getAttrList().get(i).getLabel().contains("Link")
								|| p.getAttrList().get(i).getLabel().contains("humbnail")) {
							Linkify.addLinks(value, Linkify.ALL);
						}

						if ((p.getAttrList().get(i).getLabel().contains("Thumb"))
								&& (!p.getAttrList().get(i).getLabel().contains("Small"))) {
							thumbnailView = v;
						} else {

							vg.addView(v);
						}
					}
				}

				LinearLayout auctionsLayout = (LinearLayout) findViewById(R.id.auctions_layout);
				
				if (p.getTMListingID() != null)
				{
					auctionsLayout.setVisibility(View.VISIBLE);
					submitToTMLayout.setVisibility(View.GONE);
					
					LinkTextView auctionsTextView = (LinkTextView) findViewById(R.id.details_auctions);
					auctionsTextView.setTextAndURL("TradeMe", TRADEME_URL + p.getTMListingID());
				}
				else
				{
					if (submitToTMLayoutVisible == true)
					{
						submitToTMLayout.setVisibility(View.VISIBLE);
					}
					else
					{
						submitToTMLayout.setVisibility(View.GONE);
					}
					
					auctionsLayout.setVisibility(View.GONE);
					
					if (productSubmitToTMJob != null)
					{
						((CheckBox)findViewById(R.id.relist_checkbox)).setChecked(
							(Integer)productSubmitToTMJob.getExtraInfo(MAGEKEY_PRODUCT_RELIST) == 0?false:true);
						
						((CheckBox)findViewById(R.id.allowbuynow_checkbox)).setChecked(
							(Integer)productSubmitToTMJob.getExtraInfo(MAGEKEY_PRODUCT_ALLOW_BUY_NOW) == 0?false:true);
						
						((CheckBox)findViewById(R.id.addtmfees_checkbox)).setChecked(
								(Integer)productSubmitToTMJob.getExtraInfo(MAGEKEY_PRODUCT_ADD_TM_FEES) == 0?false:true);
					}
					else
					{
						((CheckBox)findViewById(R.id.relist_checkbox)).setChecked(p.getTMRelistFlag());
						((CheckBox)findViewById(R.id.allowbuynow_checkbox)).setChecked(p.getTMAllowBuyNowFlag());
						((CheckBox)findViewById(R.id.addtmfees_checkbox)).setChecked(p.getAddTMFeesFlag());	
					}
					
					TextView noTMCategoriesTextView = (TextView)findViewById(R.id.tmcategory_nocategories_textview);
					
					if (p.getTMPreselectedCategoryPaths() != null)
					{
						if (productSubmitToTMJob != null)
						{
							selectedTMCategoryID = (Integer)productSubmitToTMJob.getExtraInfo(MAGEKEY_PRODUCT_TM_CATEGORY_ID);
						}
						else if (p.getTMPreselectedCategoryIDs().length == 1)
						{
							selectedTMCategoryID = p.getTMPreselectedCategoryIDs()[0];
						}
						else
						{
							selectedTMCategoryID = -1;
						}
						
						if (selectedTMCategoryID != -1)
						{
							for(int i=0; i<p.getTMPreselectedCategoryIDs().length; i++)
							{
								if (p.getTMPreselectedCategoryIDs()[i] == selectedTMCategoryID)
								{
									selectedTMCategoryTextView.setText(
											p.getTMPreselectedCategoryPaths()[i]);
									break;
								}
							}
							listOnTMButton.setEnabled(true);
						}
						else
						{
							selectedTMCategoryTextView.setText(
								"No category selected");
							listOnTMButton.setEnabled(false);
						}
						
						selectTMCategoryButton.setVisibility(View.VISIBLE);
						selectedTMCategoryTextView.setVisibility(View.VISIBLE);
						noTMCategoriesTextView.setVisibility(View.GONE);
					}
					else
					{
						selectTMCategoryButton.setVisibility(View.GONE);
						selectedTMCategoryTextView.setVisibility(View.GONE);
						noTMCategoriesTextView.setVisibility(View.VISIBLE);
						listOnTMButton.setEnabled(false);
					}
					
					if (p.getTMShippingTypeLabels() != null)
					{
						Spinner tmShippingTypeSpinner = (Spinner)findViewById(R.id.shippingtype_spinner);
						
						ArrayAdapter<String> tmShippingTypeSpinnerAdapter = new ArrayAdapter<String>(
								ProductDetailsActivity.this, R.layout.default_spinner_dropdown, p.getTMShippingTypeLabels());
						
						tmShippingTypeSpinner.setAdapter(tmShippingTypeSpinnerAdapter);
						
						int [] shippingTypeIDs = p.getTMShippingTypeIDs();
						
						int idToSelect;
						
						if (productSubmitToTMJob != null)
						{
							idToSelect = (Integer)productSubmitToTMJob.getExtraInfo(MAGEKEY_PRODUCT_SHIPPING_TYPE_ID);
						}
						else
						{
							idToSelect = p.getShippingTypeID();
						}
						
						for(int i=0; i<shippingTypeIDs.length; i++)
						{
							if (shippingTypeIDs[i] == idToSelect)
							{
								tmShippingTypeSpinner.setSelection(i);
								break;
							}
						}
					}
					
					if (p.getTMAccountLabels() != null)
					{
						Spinner tmAccountsSpinner = (Spinner)findViewById(R.id.tmaccount_spinner);
						
						ArrayAdapter<String> tmShippingTypeSpinnerAdapter = new ArrayAdapter<String>(
								ProductDetailsActivity.this, R.layout.default_spinner_dropdown, p.getTMAccountLabels());
						
						tmAccountsSpinner.setAdapter(tmShippingTypeSpinnerAdapter);
						
						if (productSubmitToTMJob != null)
						{
							String [] accountIDs = p.getTMAccountIDs();
							String idToSelect = (String)productSubmitToTMJob.getExtraInfo(MAGEKEY_PRODUCT_TM_ACCOUNT_ID);
							for(int i=0; i<accountIDs.length; i++)
							{
								if (accountIDs[i].equals(idToSelect))
								{
									tmAccountsSpinner.setSelection(i);
									break;
								}
							}
						}
						else
						{
							tmAccountsSpinner.setSelection(0);
						}
					}
				}
				
				if (thumbnailView != null) {
					new LoadThumbnailImage().execute(vg, thumbnailView);
				}

				instance = p;
				
				if (instance == null || instance.getId().equals("" + INVALID_PRODUCT_ID))
				{
					addToCartButtonView.setEnabled(false);					
				}
				else
				{
					addToCartButtonView.setEnabled(true);
				}
						
				evaluateTotalFunc();
				
				updateUIWithSellJobs(p);
				updateUIWithAddToCartJobs();
				
				detailsDisplayed = true;
				dismissProgressDialog();
			}
		};
		if (Looper.myLooper() == Looper.getMainLooper()) {
			map.run();
		} else {
			runOnUiThread(map);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return instance;
	}

	private void loadDetails() {
		loadDetails(false, false);
	}

	private void loadDetails(boolean forceDetails, boolean forceCategories) {
		LinearLayout auctionsLayout = (LinearLayout) findViewById(R.id.auctions_layout);
		
		auctionsLayout.setVisibility(View.GONE);
		submitToTMLayout.setVisibility(View.GONE);
		
		showProgressDialog("Loading Product");
		detailsDisplayed = false;

		new ProductInfoDisplay(forceDetails, forceCategories).execute(productSKU);
	}

	private void showProgressDialog(final String message) {
		/*
		 * if (progressDialog != null) { return; } progressDialog = new
		 * ProgressDialog(ProductDetailsActivity.this);
		 * progressDialog.setMessage(message);
		 * progressDialog.setIndeterminate(true);
		 * progressDialog.setCancelable(false); progressDialog.show();
		 */

		ProgressBar progress = (ProgressBar) findViewById(R.id.productLoadingProgressBar);
		progress.setVisibility(View.VISIBLE);
	}
	
	private void addToCart()
	{
		new AddToCart().execute();
	}

	/**
	 * Create Order
	 */
	private void createOrder() {
		new CreateOrder().execute();
	}

	private void dismissProgressDialog() {
		/*
		 * if (progressDialog == null) { return; } progressDialog.dismiss();
		 * progressDialog = null;
		 */

		ProgressBar progress = (ProgressBar) findViewById(R.id.productLoadingProgressBar);
		progress.setVisibility(View.GONE);
	}

	private void deleteProduct() {
		showProgressDialog("Deleting Product...");
		new DeleteProduct().execute();
	}
	
	private void startCameraActivity() {
		String imageName = String.valueOf(System.currentTimeMillis()) + ".jpg";
		File imagesDir = JobCacheManager.getImageUploadDirectory(productSKU, mSettings.getUrl());

		Uri outputFileUri = Uri.fromFile(new File(imagesDir, imageName));
		// save the current image path so we can use it when we want to start
		// the PhotoEditActivity
		currentImgPath = outputFileUri.getEncodedPath();

		Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		// the outputFileUri contains the location where the taken image will be
		// saved
		intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

		// starting the camera activity to take a picture
		startActivityForResult(intent, CAMERA_ACTIVITY_REQUEST_CODE);
	}

	/**
	 * After the photo was taken with camera app, go to photo edit. The image
	 * path is added as an extra to the intent, under
	 * <code>PhotoEditActivity.IMAGE_PATH_ATTR</code>. Also, a newly created
	 * <code>ImagePreviewLayout</code> is added to the <code>imagesLayout</code>
	 * 
	 * @author Bogdan Petran
	 * @see android.app.Activity#onActivityResult(int, int,
	 *      android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		resultReceived = true;

		Log.d(TAG, "onActivityResult()");

		if (resultCode != RESULT_OK && requestCode == CAMERA_ACTIVITY_REQUEST_CODE) {
			scrollToBottom();

			for (int i = 0; i < imagesLayout.getChildCount(); i++) {
				((ImagePreviewLayout) imagesLayout.getChildAt(i)).updateImageTextSize();
			}

			System.out.println("Result was not ok");
			return;
		}

		System.out.println("activity result recieved!!!!!!!!!!!");
		switch (requestCode) {
		case CAMERA_ACTIVITY_REQUEST_CODE:
			addNewImage(currentImgPath);
			startCameraActivity();
			break;
		case TM_CATEGORY_LIST_ACTIVITY_REQUEST_CODE:
			
			if (resultCode == MageventoryConstants.RESULT_SUCCESS)
			{
				selectedTMCategoryID = data.getExtras().getInt(getString(R.string.ekey_category_id));
				String categoryName = data.getExtras().getString(getString(R.string.ekey_category_name));
				
				selectedTMCategoryTextView.setText(categoryName);
				listOnTMButton.setEnabled(true);
			}
			
			break;
		default:
			break;
		}
	}
	
	private class AddNewImageTask extends AsyncTask<String, Void, Boolean> {

		private Job mUploadImageJob;
		private SettingsSnapshot mSettingsSnapshot;

		@Override
		protected void onPreExecute() {
			Log.d(TAG, ">AddNewImageTask.onPreExecute()");
			super.onPreExecute();

			mSettingsSnapshot = new SettingsSnapshot(ProductDetailsActivity.this);
			Log.d(TAG, "<AddNewImageTask.onPreExecute()");
		}
		
		@Override
		protected Boolean doInBackground(String... args) {
			JobID jobID = new JobID(INVALID_PRODUCT_ID, RES_UPLOAD_IMAGE, "" + productSKU, null);
			Job uploadImageJob = new Job(jobID, mSettingsSnapshot);

			File file = new File(args[0]);

			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_NAME,
					file.getName().substring(0, file.getName().toLowerCase().lastIndexOf(".jpg") ));

			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_CONTENT, args[0]);
			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_MIME, "image/jpeg");

			mJobControlInterface.addJob(uploadImageJob);

			mUploadImageJob = uploadImageJob;

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			Log.d(TAG, ">AddNewImageTask.onPostExecute()");

			super.onPostExecute(result);
			
			if (isActivityAlive) {
				boolean layoutExists = false;
				for(int i=0; i<imagesLayout.getChildCount(); i++)
				{
					ImagePreviewLayout previewLayout = (ImagePreviewLayout)imagesLayout.getChildAt(i);
					Job j = previewLayout.getUploadJob();
					if (j != null)
					{
						if (j.getJobID().getTimeStamp() == mUploadImageJob.getJobID().getTimeStamp())
						{
							layoutExists = true;
						}
					}
				}
				
				if (!layoutExists)
				{
					final ImagePreviewLayout newImagePreviewLayout = getUploadingImagePreviewLayout(mUploadImageJob,
							Integer.parseInt(instance.getId()), productSKU);
					imagesLayout.addView(newImagePreviewLayout);
					
					newImagePreviewLayout.registerCallbacks(mJobControlInterface);		
				}
			}
	
			Log.d(TAG, "<AddNewImageTask.onPostExecute()");
		}
	}

	/**
	 * Adds a new <code>ImagePreviewLayout</code> to the imagesLayout
	 */
	private void addNewImage(String imagePath) {
		Log.d(TAG, "> addNewImage()");
		AddNewImageTask newImageTask = new AddNewImageTask();
		newImageTask.execute(imagePath);
		Log.d(TAG, "< addNewImage()");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// save the current image path if the user is in camera view and a low
		// memory event occures, killing this activity
		outState.putString(CURRENT_IMAGE_PATH_ATTR, currentImgPath);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		// get back the current image path after the user returns from the
		// camera view (this should only happen if a low memory event occure)
		if (savedInstanceState != null) {
			currentImgPath = savedInstanceState.getString(CURRENT_IMAGE_PATH_ATTR);
		}
	}

	/**
	 * Utility method for constructing a new <code>ImagePreviewLayout</code>
	 * 
	 * @param imageUrl
	 *            is the image URL which will be shown in the
	 *            <code>ImageView</code> contained in
	 *            <code>ImagePreviewLayout</code>. Can be null but then, you
	 *            must call the <code>sendImageToServer</code> method
	 * @return the newly created layout
	 * 
	 * @see ImagePreviewLayout
	 */
	private ImagePreviewLayout getImagePreviewLayout(String imageUrl, String imageName, int productID, String SKU) {
		ImagePreviewLayout imagePreview = (ImagePreviewLayout) getLayoutInflater().inflate(R.layout.image_preview,
				imagesLayout, false);
		imagePreview.setManageClickListener(onClickManageImageListener);

		imagePreview.setProductID(productID);
		imagePreview.setSKU(SKU);

		if (imageName != null) {
			imagePreview.setImageName(imageName);
		}

		if (imageUrl != null) {
			imagePreview.setImageLocalPath(JobCacheManager.getImageDownloadDirectory(productSKU, mSettings.getUrl(), true)
					.getAbsolutePath());
			imagePreview.setImageUrl(imageUrl);
			if (imagePreview.getImageView() != null && imagePreview.getImageView().getDrawable() != null) {
				loadedImages.add(imageUrl);
			}
		}

		return imagePreview;
	}

	private ImagePreviewLayout getDownloadingImagePreviewLayout(String imageUrl, String imageName, int productID,
			String SKU) {
		ImagePreviewLayout imagePreview = (ImagePreviewLayout) getLayoutInflater().inflate(R.layout.image_preview,
				imagesLayout, false);
		imagePreview.setProductID(productID);
		imagePreview.setSKU(SKU);
		imagePreview.setImageUrlNoDownload(imageUrl);
		imagePreview.setManageClickListener(onClickManageImageListener);
		imagePreview.loadFromSDPendingDownload(imageName,
				JobCacheManager.getImageDownloadDirectory(productSKU, mSettings.getUrl(), true).getAbsolutePath());
		return imagePreview;
	}

	private ImagePreviewLayout getUploadingImagePreviewLayout(Job job, int productID, String SKU) {
		ImagePreviewLayout imagePreview = (ImagePreviewLayout) getLayoutInflater().inflate(R.layout.image_preview,
				imagesLayout, false);
		imagePreview.setProductID(productID);
		imagePreview.setSKU(SKU);
		imagePreview.setUploadJob(job, mJobControlInterface, new Runnable() {

			@Override
			public void run() {
				loadDetails(false, false);
			}
		});
		imagePreview.setManageClickListener(onClickManageImageListener);
		return imagePreview;
	}

	public void startPhotoEditActivity(String imagePath, boolean inEditMode) {
		Intent i = new Intent(this, PhotoEditActivity.class);
		i.putExtra(PhotoEditActivity.IMAGE_PATH_ATTR, imagePath);
		i.putExtra(PhotoEditActivity.EDIT_MODE_ATTR, inEditMode);

		startActivityForResult(i, PHOTO_EDIT_ACTIVITY_REQUEST_CODE);
	}

	/**
	 * Perform a full scroll to bottom of screen
	 */
	private void scrollToBottom() {
		scroller.post(new Runnable() {

			@Override
			public void run() {
				scroller.fullScroll(View.FOCUS_DOWN);
			}
		});
	}

	/**
	 * Enable/Disable the Photo shoot and first Add image buttons
	 */
	private void setButtonsEnabled(boolean clickable) {
		photoShootBtnTop.setEnabled(clickable);
		photoShootBtnTop.setFocusable(clickable);
	}

	private class ProductInfoDisplay extends AsyncTask<Object, Void, Boolean> {

		private Product p;
		private Map<String, Object> c;

		private final boolean forceDetails;
		private final boolean forceCategories;
		private SettingsSnapshot mSettingsSnapshot;

		public ProductInfoDisplay(boolean forceDetails, boolean forceCategories) {
			super();
			this.forceDetails = forceDetails;
			this.forceCategories = forceCategories;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			mSettingsSnapshot = new SettingsSnapshot(ProductDetailsActivity.this);
		}
		
		@Override
		protected Boolean doInBackground(Object... args) {
			final String[] params = new String[2];
			params[0] = GET_PRODUCT_BY_SKU; // ZERO --> Use Product ID , ONE -->
											// Use Product SKU
			params[1] = String.valueOf(args[0]);

			if (forceCategories || JobCacheManager.categoriesExist(mSettingsSnapshot.getUrl()) == false) {
				catReqId = resHelper.loadResource(ProductDetailsActivity.this, RES_CATALOG_CATEGORY_TREE, mSettingsSnapshot);
				return Boolean.FALSE;
			} else if (forceDetails || JobCacheManager.productDetailsExist(params[1], mSettingsSnapshot.getUrl()) == false) {
				loadRequestId = resHelper.loadResource(ProductDetailsActivity.this, RES_PRODUCT_DETAILS, params, mSettingsSnapshot);
				return Boolean.FALSE;
			} else {
				p = JobCacheManager.restoreProductDetails(params[1], mSettingsSnapshot.getUrl());
				c = JobCacheManager.restoreCategories(mSettingsSnapshot.getUrl());
				return Boolean.TRUE;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				if (mSettings.getSoundCheckBox() == true && mOpenedAsAResultOfScanning == true)
				{
					mDetailsLoadSuccessSound.playSound();
				}
				mapData(p, c);
				// start the loading of images
				loadImages();
			} else {
				refreshImages = true;
			}
		}
	}

	private void loadImages() {
		synchronized (ImageCachingManager.sSynchronisationObject) {
			for (int i = 0; i < imagesLayout.getChildCount(); i++) {
				((ImagePreviewLayout) imagesLayout.getChildAt(i)).deregisterCallbacks(mJobControlInterface);
			}
			imagesLayout.removeAllViews();

			if ((refreshImages) && (ImageCachingManager.getPendingDownloadCount(productSKU) == 0)) {
				refreshImages = false;
				
				JobCacheManager.clearImageDownloadDirectory(productSKU, mSettings.getUrl());
				JobCacheManager.clearImageFullPreviewDirectory(productSKU, mSettings.getUrl());

				for (int i = 0; i < instance.getImages().size(); i++) {
					ImagePreviewLayout newImagePreviewLayout = getImagePreviewLayout(instance.getImages().get(i)
							.getImgURL(), instance.getImages().get(i).getImgName(), Integer.parseInt(instance.getId()),
							productSKU);
					newImagePreviewLayout.setMainImageCheck(instance.getImages().get(i).getMain());
					imagesLayout.addView(newImagePreviewLayout);
				}
			} else {
				for (int i = 0; i < instance.getImages().size(); i++) {
					ImagePreviewLayout newImagePreviewLayout = getDownloadingImagePreviewLayout(instance.getImages()
							.get(i).getImgURL(), instance.getImages().get(i).getImgName(),
							Integer.parseInt(instance.getId()), productSKU);
					newImagePreviewLayout.setMainImageCheck(instance.getImages().get(i).getMain());
					imagesLayout.addView(newImagePreviewLayout);
				}
			}

			List<Job> list = mJobControlInterface.getAllImageUploadJobs(productSKU, mSettings.getUrl());

			for (int i = 0; i < list.size(); i++) {
				ImagePreviewLayout newImagePreviewLayout = getUploadingImagePreviewLayout(list.get(i),
						Integer.parseInt(instance.getId()), productSKU);
				imagesLayout.addView(newImagePreviewLayout);
			}

			for (int i = 0; i < imagesLayout.getChildCount(); i++) {
				((ImagePreviewLayout) imagesLayout.getChildAt(i)).registerCallbacks(mJobControlInterface);
			}
			imagesLayout.setVisibility(View.VISIBLE);
			imagesLoadingProgressBar.setVisibility(View.GONE);
		}
	}

	private static class DeleteImageAsyncTask extends AsyncTask<Object, Void, ImagePreviewLayout>
		implements OperationObserver{

		final ProductDetailsActivity activityInstance;
		private int requestId = INVALID_REQUEST_ID;
		private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
		private CountDownLatch doneSignal;
		private boolean success;
		private SettingsSnapshot mSettingsSnapshot;
		
		public DeleteImageAsyncTask(ProductDetailsActivity instance) {
			activityInstance = instance;
		}

		@Override
		protected void onPreExecute() {
			mSettingsSnapshot = new SettingsSnapshot(activityInstance);
		}

		@Override
		protected ImagePreviewLayout doInBackground(Object... params) {

			if (activityInstance == null)
				return null;
			
			ImagePreviewLayout layoutToRemove = (ImagePreviewLayout) params[1];
			
			doneSignal = new CountDownLatch(1);
			resHelper.registerLoadOperationObserver(this);
			requestId = resHelper.loadResource(activityInstance, RES_DELETE_IMAGE,
					new String[] { (String)params[0], layoutToRemove.getImageName() }, mSettingsSnapshot);
			while (true) {
				if (isCancelled()) {
					return null;
				}
				try {
					if (doneSignal.await(1, TimeUnit.SECONDS)) {
						break;
					}
				} catch (InterruptedException e) {
					return null;
				}
			}
			resHelper.unregisterLoadOperationObserver(this);

			if (success == true) {
				return layoutToRemove;
			}
			else
			{
				return null;
			}
		}

		@Override
		protected void onPostExecute(ImagePreviewLayout result) {
			if (result == null) {
				Toast.makeText(activityInstance.getApplicationContext(), "Could not delete image.", Toast.LENGTH_SHORT)
						.show();
				return;
			}

			// remove the image preview layout from the images layout (which
			// contains all images for the current product)
			activityInstance.imagesLayout.removeView(result);
		}
		
		@Override
		public void onLoadOperationCompleted(LoadOperation op) {
			if (op.getOperationRequestId() == requestId) {
				success = op.getException() == null;
				doneSignal.countDown();
			}
		}
	}

	/**
	 * Handler for image deletion and image click inside
	 * <code>ProductDetailsActivity</code>. This will be notified from
	 * <code>ImagePreviewLayout</code> when the "Delete" button or the image is
	 * being clicked.
	 * 
	 * @author Bogdan Petran
	 * @see ImagePreviewLayout
	 */
	private static class ClickManageImageListener implements IOnClickManageHandler {

		WeakReference<ProductDetailsActivity> activityReference;
		final ProductDetailsActivity activityInstance;

		public ClickManageImageListener(ProductDetailsActivity instance) {
			activityReference = new WeakReference<ProductDetailsActivity>(instance);
			activityInstance = activityReference.get();
		}

		@Override
		public void onDelete(final ImagePreviewLayout layoutToRemove) {

			if (activityInstance.instance == null || activityInstance.instance.getId().equals("" + INVALID_PRODUCT_ID))
				return;

			// show the delete confirmation when the delete button was pressed
			// on an item
			Builder alertDialogBuilder = new Builder(activityInstance);
			alertDialogBuilder.setTitle("Confirm deletion");
			alertDialogBuilder.setNegativeButton("No", null);
			alertDialogBuilder.setPositiveButton("Yes", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					layoutToRemove.setLoading(true);

					// start a task to delete the image from server
					new DeleteImageAsyncTask(activityInstance).execute(activityInstance.instance.getId(),
							layoutToRemove);
				}
			});

			alertDialogBuilder.show();

		}

		@Override
		public void onClickForEdit(final ImagePreviewLayout layoutToEdit) {

			activityInstance.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					(new LoadImagePreviewFromServer(activityInstance, layoutToEdit.getImageLocalPath(), layoutToEdit
							.getUrl())).execute();
				}
			});
		}

		@Override
		public void onClickForMainImage(ImagePreviewLayout layoutToEdit) {
			
			if (activityInstance.instance == null || activityInstance.instance.getId().equals("" + INVALID_PRODUCT_ID))
				return;

			layoutToEdit.markAsMain(activityInstance.instance.getId(), activityInstance);
		}
	}

	private static class ScrollListener implements IScrollListener {

		WeakReference<ProductDetailsActivity> activityReference;
		final ProductDetailsActivity activityInstance;

		public ScrollListener(ProductDetailsActivity instance) {
			activityReference = new WeakReference<ProductDetailsActivity>(instance);
			activityInstance = activityReference.get();
		}

		@Override
		public void scrolledToBottom() {
		}

		@Override
		public void scrollMoved() {
		}
	}

	/* Submit the product to TM */
	private class SubmitToTMTask extends AsyncTask<Integer, Integer, Job> {

		private SettingsSnapshot mSettingsSnapshot;
		
		int categoryID, addTmFees, allowBuyNow, shippingTypeID, relist;
		String accountID;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			if (instance.getTMPreselectedCategoryIDs() != null)
			{
				categoryID = selectedTMCategoryID;
			}
			else
			{
				categoryID = -1;
			}
			
			Spinner tmShippingTypeSpinner = (Spinner)findViewById(R.id.shippingtype_spinner);
			shippingTypeID = instance.getTMShippingTypeIDs()[tmShippingTypeSpinner.getSelectedItemPosition()];
			
			Spinner tmAccountsSpinner = (Spinner)findViewById(R.id.tmaccount_spinner);
			accountID = instance.getTMAccountIDs()[tmAccountsSpinner.getSelectedItemPosition()];
			
			relist = ((CheckBox)findViewById(R.id.relist_checkbox)).isChecked() ? 1 : 0;
			allowBuyNow = ((CheckBox)findViewById(R.id.allowbuynow_checkbox)).isChecked() ? 1 : 0;
			addTmFees = ((CheckBox)findViewById(R.id.addtmfees_checkbox)).isChecked() ? 1 : 0;
			
			mSettingsSnapshot = new SettingsSnapshot(ProductDetailsActivity.this);
			
			submitToTMLayoutVisible = false;
			submitToTMLayout.setVisibility(View.GONE);
			
			if (productSubmitToTMJob != null && productSubmitToTMJob.getPending() == false)
			{
				mJobControlInterface.deleteFailedJob(productSubmitToTMJob.getJobID());
			}
		}
		
		@Override
		protected Job doInBackground(Integer... ints) {
			JobID jobID = new JobID(MageventoryConstants.INVALID_PRODUCT_ID, MageventoryConstants.RES_CATALOG_PRODUCT_SUBMIT_TO_TM, productSKU, null);
			Job job = new Job(jobID, mSettingsSnapshot);
			
			Map <String, Object> extras = new HashMap<String, Object>();
			
			extras.put(MAGEKEY_PRODUCT_TM_CATEGORY_ID, categoryID);
			
			extras.put(MAGEKEY_PRODUCT_ADD_TM_FEES, addTmFees);
			extras.put(MAGEKEY_PRODUCT_ALLOW_BUY_NOW, allowBuyNow);
			extras.put(MAGEKEY_PRODUCT_SHIPPING_TYPE_ID, shippingTypeID);
			extras.put(MAGEKEY_PRODUCT_RELIST, relist);
			extras.put(MAGEKEY_PRODUCT_TM_ACCOUNT_ID, accountID);
			
			job.setExtras(extras);

			mJobControlInterface.addJob(job);

			return job;
		}
		
		@Override
		protected void onPostExecute(Job result) {

			productSubmitToTMJob = result;
			productSubmitToTMJobCallback = newSubmitToTMCallback();
			showSubmitToTMJobUIInfo(productSubmitToTMJob);
			
			if (!mJobControlInterface.registerJobCallback(productSubmitToTMJob.getJobID(), productSubmitToTMJobCallback)) {
				layoutSubmitToTMRequestPending.setVisibility(View.GONE);
				productSubmitToTMJobCallback = null;
				productSubmitToTMJob = null;
				loadDetails();
			}
			
			super.onPostExecute(result);
		}
	}
	
	/**
	 * Sell product.
	 */
	private class AddToCart extends AsyncTask<Integer, Integer, Job> {

		private SettingsSnapshot mSettingsSnapshot;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			mSettingsSnapshot = new SettingsSnapshot(ProductDetailsActivity.this);
		}
		
		@Override
		protected Job doInBackground(Integer... ints) {

			if (instance == null || instance.getId().equals("" + INVALID_PRODUCT_ID))
			{
				return null;
			}
			
			String customerID = mSettingsSnapshot.getUser();
			String qty = qtyEdit.getText().toString();
			String price = instance.getPrice().toString();
			String soldPrice = priceEdit.getText().toString();
			String total = new Double(Double.parseDouble(qty) * Double.parseDouble(price)).toString();
			String dateTime = (String) android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss", new java.util.Date());
			String name = instance.getName();
			String productID = instance.getId();
			
			// Check If Sold Price is empty then set the sold price with price
			if (soldPrice.compareToIgnoreCase("") == 0) {
				soldPrice = price;
			}

			JobID jobID = new JobID(INVALID_PRODUCT_ID, RES_ADD_PRODUCT_TO_CART, productSKU, null);
			Job addToCartJob = new Job(jobID, mSettingsSnapshot);
			
			addToCartJob.putExtraInfo(MAGEKEY_PRODUCT_TRANSACTION_ID, "" + jobID.getTimeStamp());
			addToCartJob.putExtraInfo(MAGEKEY_PRODUCT_ID, productID);
			addToCartJob.putExtraInfo(MAGEKEY_PRODUCT_SKU, productSKU);
			addToCartJob.putExtraInfo(MAGEKEY_CUSTOMER_INFO_ID, customerID);
			addToCartJob.putExtraInfo(MAGEKEY_PRODUCT_QUANTITY, qty);
			addToCartJob.putExtraInfo(MAGEKEY_PRODUCT_PRICE, soldPrice);
			addToCartJob.putExtraInfo(MAGEKEY_PRODUCT_TOTAL, total);
			addToCartJob.putExtraInfo(MAGEKEY_PRODUCT_DATE_TIME, dateTime);
			addToCartJob.putExtraInfo(MAGEKEY_PRODUCT_NAME2, name);
			
			mJobControlInterface.addJob(addToCartJob);
			
			JobCacheManager.addCartItem(addToCartJob.getExtras(), mSettingsSnapshot.getUrl());

			return addToCartJob;
		}
		
		@Override
		protected void onPostExecute(Job result) {
			mAddToCartJobs.add(result);
			mJobControlInterface.registerJobCallback(result.getJobID(), newAddToCartJobCallback());
			
			if (instance != null)
				updateUIWithAddToCartJobs();
			
			super.onPostExecute(result);
		}
	}
	
	/**
	 * Sell product.
	 */
	private class CreateOrder extends AsyncTask<Integer, Integer, Job> {

		private SettingsSnapshot mSettingsSnapshot;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			mSettingsSnapshot = new SettingsSnapshot(ProductDetailsActivity.this);
		}
		
		@Override
		protected Job doInBackground(Integer... ints) {

			// 2- Set Product Information
			String sku = productSKU;
			String price = instance.getPrice().toString();
			String soldPrice = priceEdit.getText().toString();
			String qty = qtyEdit.getText().toString();

			// Check If Sold Price is empty then set the sold price with price
			if (soldPrice.compareToIgnoreCase("") == 0) {
				soldPrice = price;
			}

			String name = instance.getName();

			JobID jobID = new JobID(INVALID_PRODUCT_ID, RES_CATALOG_PRODUCT_SELL, productSKU, null);
			Job sellJob = new Job(jobID, mSettingsSnapshot);
			sellJob.putExtraInfo(MAGEKEY_PRODUCT_SKU, sku);
			sellJob.putExtraInfo(MAGEKEY_PRODUCT_QUANTITY, qty);
			sellJob.putExtraInfo(MAGEKEY_PRODUCT_PRICE, soldPrice);
			sellJob.putExtraInfo(MAGEKEY_PRODUCT_NAME, name);

			mJobControlInterface.addJob(sellJob);

			return sellJob;
		}
		
		@Override
		protected void onPostExecute(Job result) {
			mSellJobs.add(result);
			mJobControlInterface.registerJobCallback(result.getJobID(), newSellJobCallback());
			
			if (instance != null)
				updateUIWithSellJobs(instance);
			
			super.onPostExecute(result);
		}
	}

	/**
	 * Implement onCreateDialogue Show the Sold Confirmation Dialogue
	 */
	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case SUBMIT_TO_TM_CONFIRMATION_DIALOGUE:
			AlertDialog.Builder submitToTMDialogueBuilder = new AlertDialog.Builder(ProductDetailsActivity.this);

			submitToTMDialogueBuilder.setTitle("Submit to TM?");
			submitToTMDialogueBuilder.setMessage("Are you sure you want to submit this product to TM?");
			submitToTMDialogueBuilder.setCancelable(false);

			submitToTMDialogueBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					new SubmitToTMTask().execute();
				}
			});

			submitToTMDialogueBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});

			AlertDialog submitToTMDialogue = submitToTMDialogueBuilder.create();
			return submitToTMDialogue;
		case SOLD_CONFIRMATION_DIALOGUE:
			AlertDialog.Builder soldDialogueBuilder = new AlertDialog.Builder(ProductDetailsActivity.this);

			soldDialogueBuilder.setTitle("Confirmation");
			soldDialogueBuilder.setMessage("Sell this product now?");
			soldDialogueBuilder.setCancelable(false);

			// If Pressed OK Submit the Order With Details to Site
			soldDialogueBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

					/* Verify then Create */
					if (isVerifiedData())
						createOrder();
				}
			});

			// If Pressed Cancel Just remove the Dialogue
			soldDialogueBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});

			AlertDialog soldDialogue = soldDialogueBuilder.create();
			return soldDialogue;
		case ADD_TO_CART_CONFIRMATION_DIALOGUE:
			AlertDialog.Builder addToCartDialogueBuilder = new AlertDialog.Builder(ProductDetailsActivity.this);

			addToCartDialogueBuilder.setTitle("Confirmation");
			addToCartDialogueBuilder.setMessage("Add this product to cart to check out later?");
			addToCartDialogueBuilder.setCancelable(false);

			addToCartDialogueBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					if (isVerifiedData())
						addToCart();
				}
			});

			addToCartDialogueBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});

			AlertDialog addToCartDialogue = addToCartDialogueBuilder.create();
			return addToCartDialogue;
		case SOLD_ORDER_SUCCESSEDED:
			AlertDialog.Builder successDlgBuilder = new AlertDialog.Builder(ProductDetailsActivity.this);

			successDlgBuilder.setTitle("Information");
			successDlgBuilder.setMessage("Order Created");
			successDlgBuilder.setCancelable(false);

			// If Pressed OK Submit the Order With Details to Site
			successDlgBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// Reset Sold Price TextView & Qty TestView
					qtyEdit.setText("1");
					priceEdit.setText(String.valueOf(instance.getPrice()));

					loadDetails();
				}
			});

			AlertDialog successDlg = successDlgBuilder.create();
			return successDlg;

		case SHOW_MENU:
			AlertDialog.Builder menuBuilder = new Builder(ProductDetailsActivity.this);
			menuBuilder.setItems(menuItems, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case MITEM_ADMIN:
						Settings settings = new Settings(getApplicationContext());
						String url = settings.getUrl() + "/index.php/admin/catalog_product/edit/id/" + instance.getId();
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(url));
						startActivity(intent);
						break;

					case MITEM_LIST_ON_TM:
						if (instance == null)
							return;
						
						if ((productSubmitToTMJob != null && productSubmitToTMJob.getPending() == true) || productCreationJob != null)
						{
							showProductCreationOrSendToTMJobPending();
						}
						else
						if (instance.getTMListingID() != null)
						{
							showProductAlreadyListedDialog();
						}
						else
						{
							submitToTMLayoutVisible = true;
							submitToTMLayout.setVisibility(View.VISIBLE);
						}
						break;

					case MITEM_EDIT:
						showEditDeleteWarningDialog(true);
						break;

					case MITEM_DELETE:
						if (instance == null || instance.getId().equals("" + INVALID_PRODUCT_ID))
							return;
						
						showEditDeleteWarningDialog(false);
						break;

					case MITEM_SHOP:
						Settings settings2 = new Settings(getApplicationContext());
						String url2 = settings2.getUrl() + "/" + instance.getUrlPath();

						Intent intent2 = new Intent(Intent.ACTION_VIEW);
						intent2.setData(Uri.parse(url2));
						startActivity(intent2);
						break;
						
					case MITEM_DUPLICATE:
						
						/* Launching product create activity from "duplicate menu" breaks NewNewReload cycle. */
						BaseActivityCommon.mNewNewReloadCycle = false;
						
						final Intent intent3 = new Intent(getApplicationContext(), ProductCreateActivity.class);
						final String ekeyProductToDuplicate = getString(R.string.ekey_product_to_duplicate);

						intent3.putExtra(ekeyProductToDuplicate, (Serializable)instance);
						startActivity(intent3);

					default:
						break;
					}
				}
			});

			AlertDialog menuDlg = menuBuilder.create();
			return menuDlg;

		case SHOW_DELETE_DIALOGUE:
			AlertDialog.Builder deleteDialogueBuilder = new AlertDialog.Builder(ProductDetailsActivity.this);

			deleteDialogueBuilder.setTitle("Confirmation");
			deleteDialogueBuilder.setMessage("Are You Sure - This will delete product infomration");
			deleteDialogueBuilder.setCancelable(false);

			// If Pressed OK Submit the Order With Details to Site
			deleteDialogueBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					/* Delete Product */
					deleteProduct();
				}
			});

			// If Pressed Cancel Just remove the Dialogue
			deleteDialogueBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});

			AlertDialog deleteDialogue = deleteDialogueBuilder.create();
			return deleteDialogue;

		default:
			return super.onCreateDialog(id);
		}
	}

	private void startEditActivity() {

		final Intent i = new Intent(this, ProductEditActivity.class);
		i.putExtra(getString(R.string.ekey_product_sku), productSKU);

		startActivity(i);
	}

	// Verify Price & Quantity
	private boolean isVerifiedData() {
		// 1- Check that price is numeric
		try {
			Double testPrice = Double.parseDouble(priceEdit.getText().toString());
		} catch (Exception e) {
			// TODO: handle exception
			Toast.makeText(getApplicationContext(), "Invalid Sold Price", Toast.LENGTH_SHORT).show();
			return false;
		}

		// 2- Check that Qty is numeric
		Double testQty = 0.0;
		try {
			testQty = Double.parseDouble(qtyEdit.getText().toString());
		} catch (Exception e) {
			// TODO: handle exception
			Toast.makeText(getApplicationContext(), "Invalid Quantity", Toast.LENGTH_SHORT).show();
			return false;
		}

		// All Tests Passed
		return true;
	}
	
	private TextWatcher evaluteTotalTextWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			totalEdit.removeTextChangedListener(evaluatePriceTextWatcher);
			evaluateTotalFunc();
			totalEdit.addTextChangedListener(evaluatePriceTextWatcher);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void afterTextChanged(Editable s) {}
	};
	
	private TextWatcher evaluatePriceTextWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			priceEdit.removeTextChangedListener(evaluteTotalTextWatcher);
			evaluatePriceFunc();
			priceEdit.addTextChangedListener(evaluteTotalTextWatcher);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void afterTextChanged(Editable s) {}
	};
	
	private void evaluateTotalFunc()
	{
		String SoldPrice = priceEdit.getText().toString();
		String SoldQty = qtyEdit.getText().toString();

		// if Either QTY or Price is empty then total is Empty too
		// and return
		if ((SoldPrice.compareTo("") == 0) || (SoldQty.compareTo("") == 0)) {
			totalEdit.setText(String.valueOf(""));
			return;
		}

		// Else Calculate Total
		float price;
		float qty;
		
		try
		{
			price = Float.parseFloat(SoldPrice);
		}
		catch (NumberFormatException e)
		{
			price = 0;
		}
		
		try
		{
			qty = Float.parseFloat(SoldQty);
		}
		catch (NumberFormatException e)
		{
			qty = 0;
		}
		
		float total = price * qty;

		totalEdit.setText(OrderDetailsActivity.formatPrice("" + total).replace("$", ""));
	}
	
	private void evaluatePriceFunc()
	{
		String totalPrice = totalEdit.getText().toString();
		String quantity = qtyEdit.getText().toString();

		// if Either QTY or Total price is empty then price is Empty too
		if ((totalPrice.compareTo("") == 0) || (quantity.compareTo("") == 0)) {
			priceEdit.setText("");
			return;
		}

		float total;
		float qty;
		
		try
		{
			total = Float.parseFloat(totalPrice);
		}
		catch (NumberFormatException e)
		{
			total = 0;
		}
		
		try
		{
			qty = Float.parseFloat(quantity);
		}
		catch (NumberFormatException e)
		{
			qty = 0;
		}
		
		//Can't divide by 0. Return.
		if (qty == 0)
		{
			priceEdit.setText("");
			return;
		}
		
		float price = total / qty;
		
		priceEdit.setText(OrderDetailsActivity.formatPrice("" + price).replace("$", ""));
	}

	/**
	 * Create Order Invoice
	 * 
	 * @author hussein
	 * 
	 */
	private class DeleteProduct extends AsyncTask<Integer, Integer, String> {

		private SettingsSnapshot mSettingsSnapshot;
		
		@Override
		protected void onPreExecute() {
			mSettingsSnapshot = new SettingsSnapshot(ProductDetailsActivity.this);
			super.onPreExecute();
		}
		
		@Override
		protected String doInBackground(Integer... ints) {

			try {
				deleteProductID = resHelper.loadResource(ProductDetailsActivity.this, RES_PRODUCT_DELETE, new String [] {productSKU},
						mSettingsSnapshot);
				return "";
			} catch (Exception e) {
				Log.w(TAG, "" + e);
				return null;
			}
		}
	}

	/**
	 * Load Thumbnail
	 * 
	 * @author hussein
	 * 
	 */
	private class LoadThumbnailImage extends AsyncTask<Object, Object, String> {

		Bitmap img = null;
		View view;
		ViewGroup vg;

		@Override
		protected String doInBackground(Object... ints) {
			vg = (ViewGroup) ints[0];
			view = (View) ints[1];
			String path = ((TextView) view.findViewById(R.id.attrValue)).getText().toString();
			try {
				img = BitmapFactory.decodeStream((new URL(path)).openStream());
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return "";
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			if (img != null) {
				((ImageView) view.findViewById(R.id.thumbnailViewValue)).setImageBitmap(img);
				((ImageView) view.findViewById(R.id.thumbnailViewValue)).setVisibility(View.VISIBLE);
				vg.addView(view);
			}
		}

	}

}