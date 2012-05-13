package com.mageventory;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.client.MagentoClient;
import com.mageventory.components.ImageCachingManager;
import com.mageventory.components.ImagePreviewLayout;
import com.mageventory.components.ProductDetailsScrollView;
import com.mageventory.interfaces.IOnClickManageHandler;
import com.mageventory.interfaces.IScrollListener;
import com.mageventory.job.Job;
import com.mageventory.job.JobCallback;
import com.mageventory.job.JobControlInterface;
import com.mageventory.job.JobID;
import com.mageventory.job.JobService;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.settings.Settings;
import com.mageventory.util.DefaultOptionsMenuHelper;

public class ProductDetailsActivity extends BaseActivity implements MageventoryConstants, OperationObserver {
	
	private static final String TAG = "ProductDetailsActivity";
	
	private static final int PHOTO_EDIT_ACTIVITY_REQUEST_CODE = 0;			// request code used to start the PhotoEditActivity
	private static final int CAMERA_ACTIVITY_REQUEST_CODE = 1;				// request code used to start the Camera activity
	private static final String CURRENT_IMAGE_PATH_ATTR = "current_path";	/* attribute used to save the current image path if a low memory event occures on a device and 
																			   while in the camera mode, the current activity may be closed by the OS */
	private static final int SOLD_CONFIRMATION_DIALOGUE = 1;
	private static final int SOLD_ORDER_SUCCESSEDED = 2;
	private static final int SHOW_MENU =3;

	private static final String [] menuItems = {"Admin","Add Image","Edit","Delete","Shop"};
	private static final int MITEM_ADMIN = 0;
	private static final int MITEM_ADD_IMAGE = 1;
	private static final int MITEM_EDIT = 2;
	private static final int MITEM_DELETE = 3;
	private static final int MITEM_SHOP = 4;

	private static final int SHOW_DELETE_DIALOGUE = 4;

	private LayoutInflater inflater;
	
		
	// ArrayList<Category> categories;
	ProgressDialog progressDialog;
	MyApplication app;
	
	// Activity activity = null;

	String path;					// path of the images directory for the current product
	File imagesDir;
	boolean refreshImages = false;
	String currentImgPath;			// this will actually be: path + "/imageName"
	LinearLayout imagesLayout;		// the layout which will contain ImagePreviewLayout objects
	boolean resultReceived = false;
	ProductDetailsScrollView scrollView;
	Button photoShootBtn;
	ProgressBar imagesLoadingProgressBar;
	ScrollView scroller;

	ClickManageImageListener onClickManageImageListener;
	ScrollListener scrollListener;
	
	// edit
	private Button updateBtn;
	
	// detail views
	private TextView nameInputView;
	private TextView priceInputView;
	private TextView quantityInputView;
	private TextView descriptionInputView;
	private CheckBox statusView;
	private TextView weightInputView;
	private Button soldButtonView;
	private TextView categoryView;
	private EditText[] detailViews;
	private TextView skuTextView;
	private JobControlInterface jobControlInterface;
	
	private View.OnClickListener onUpdateBtnClickL = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			startUpdateOperation();
		}
	};
	
	// product data
	private int productId;
	private Product instance;
	
	// resources
	private int loadRequestId = INVALID_REQUEST_ID;
	private int updateRequestId = INVALID_REQUEST_ID;
	private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
	private boolean detailsDisplayed = false;
	private int orderCreateID = INVALID_REQUEST_ID;
	private int deleteProductID = INVALID_REQUEST_ID;
	// private int uploadImageRequestId = INVALID_REQUEST_ID;
	private int loadAttributeListRequestId;
	
	// y XXX: rework the IMAGE LOADING TASK
	private Set<String> loadedImages = new HashSet<String>();
	
	JobControlInterface mJobControlInterface;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.product_details); // y XXX: REUSE THE PRODUCT CREATION / DETAILS VIEW...
		
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
		detailViews = new EditText[] {
			/*descriptionInputView,*/
			/*statusView,
			weightInputView,*/
			/*categoryView,*/
		};
		updateBtn = (Button) findViewById(R.id.update_btn);
		
		// read arguments
		boolean allowEditting = false;
		boolean forceReload = false;

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			allowEditting = extras.getBoolean(getString(R.string.ekey_allow_editting), false);
			productId = extras.getInt(getString(R.string.ekey_product_id), INVALID_PRODUCT_ID);
			forceReload = extras.getBoolean("FORCE_RELOAD",false);
		} else {
			productId = INVALID_PRODUCT_ID;
		}
			
		// Check if reload 
		if(forceReload)
		{
			// If Service 
			if(resHelper.isResourceAvailable(getApplicationContext(), RES_PRODUCT_DETAILS)) 
				resHelper.markResourceAsOld(getApplicationContext(), RES_PRODUCT_DETAILS);
		}
		
		// attach listeners
		updateBtn.setOnClickListener(onUpdateBtnClickL);

		// (dis)allow editting
		setEditEnabled(allowEditting);
		
		// retrieve last instance
		instance = (Product) getLastNonConfigurationInstance();

		app = (MyApplication) getApplication();

		this.setTitle("Mventory: Product Details");
	
		imagesLoadingProgressBar = (ProgressBar) findViewById(R.id.imagesLoadingProgressBar);
		scroller = (ScrollView) findViewById(R.id.scrollView1);
		
		((Button)findViewById(R.id.soldButton)).getBackground().setColorFilter(new LightingColorFilter(0x444444, 0x737575));
		
		scrollListener = new ScrollListener(this);
		
		scrollView = (ProductDetailsScrollView) findViewById(R.id.scrollView1);
		scrollView.setScrollToBottomListener(scrollListener);
		
		photoShootBtn = (Button) findViewById(R.id.photoShootBtn);
		
		// the absolute path of the images directory for the current product (here will be stored the images received from server)
		path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MageventoryImages/" + productId;

		imagesDir = new File( path );
		onClickManageImageListener = new ClickManageImageListener(this);

		if(!imagesDir.exists()){
			imagesDir.mkdirs();
		}
		
		// Set the Sold Button Action
		soldButtonView = (Button) findViewById(R.id.soldButton);
		soldButtonView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Show Confirmation Dialogue
				showDialog(SOLD_CONFIRMATION_DIALOGUE);
			}
		});			
		
		
		/*Set Events for change in values */
		EditText soldPrice = (EditText) findViewById(R.id.button);
		soldPrice.addTextChangedListener(evaluteTotal());
		soldPrice.setOnKeyListener(new OnKeyListener() {
			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
			
				if(event.getAction() == KeyEvent.KEYCODE_ENTER)
					((EditText) findViewById(R.id.qtyText)).requestFocus();
				
				return false;
			}
		});
		
		EditText soldQty = (EditText) findViewById(R.id.qtyText);
		soldQty.addTextChangedListener(evaluteTotal());
		
		/*EditText totalSold = (EditText) findViewById(R.id.totalText);
		totalSold.addTextChangedListener(evalutePrice());*/
		
		/* Check CustomerVliad
		 * If not Valid Customer "Disable SoldPrice,Qty and Sold Price"*/			
		Settings settings = new Settings(getApplicationContext());				 
		if(settings.hasSettings())
		{
			if(!settings.getCustomerValid())
			{
				soldButtonView.setVisibility(View.GONE);
				((EditText)findViewById(R.id.button)).setVisibility(View.GONE);
				((EditText)findViewById(R.id.qtyText)).setVisibility(View.GONE);
				((EditText)findViewById(R.id.totalText)).setVisibility(View.GONE);
			}
		}
					
		Button menuBtn = (Button) findViewById(R.id.menuButton);
		menuBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showDialog(SHOW_MENU);				
			}
		});
		
		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		
		jobControlInterface = new JobControlInterface(this);
		
						
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		resHelper.registerLoadOperationObserver(this);
		if (detailsDisplayed == false) {
			loadDetails();
		} else {
		    //loadImages(productId);
		}

		// this happens when OS is killing this activity (e.g. if user goes to
		// Camera activity) and we don't need to load all product details, only
		// the images
		if (!resultReceived) {
			imagesLayout = (LinearLayout) findViewById(R.id.imagesLinearLayout);
		}
		
		for (int i = 0; i < imagesLayout.getChildCount(); i++) {
			((ImagePreviewLayout)imagesLayout.getChildAt(i)).registerCallbacks(mJobControlInterface);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		resHelper.unregisterLoadOperationObserver(this);
		
		for (int i = 0; i < imagesLayout.getChildCount(); i++) {
			((ImagePreviewLayout)imagesLayout.getChildAt(i)).deregisterCallbacks(mJobControlInterface);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			loadDetails(true);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		
		if(op.getOperationRequestId() == deleteProductID)
		{
			dismissProgressDialog();
			Intent intent=new Intent();
		    intent.putExtra("ComingFrom", "Hello");
		    setResult(RESULT_CHANGE, intent);
		    finish();
			return;
		}
		
		if(op.getOperationRequestId() == orderCreateID)
		{
			dismissProgressDialog();
			Toast.makeText(getApplicationContext(), "Order Created Successfully", Toast.LENGTH_SHORT).show();
			((EditText)findViewById(R.id.qtyText)).setText("1");
			((EditText)findViewById(R.id.button)).setText(String.valueOf(instance.getPrice()));
										
			// Mark Resource as Old
			resHelper.markResourceAsOld(getApplicationContext(), RES_PRODUCT_DETAILS);
			loadDetails();
		}
		
		if (op.getOperationRequestId() != loadRequestId && op.getOperationRequestId() != updateRequestId) {
			return;
		}
		if (op.getException() != null) {
			dismissProgressDialog();
			Toast.makeText(this, "" + op.getException(), Toast.LENGTH_LONG).show();
			return;
		}
		if (loadRequestId == op.getOperationRequestId()) {
			loadDetails();
		} else if (updateRequestId == op.getOperationRequestId()) {
			dismissProgressDialog();
			Toast.makeText(this, "Product successfully updated", Toast.LENGTH_LONG).show();
			
			setResult(RESULT_CHANGE);
		}
		
	}
	
	private void mapData(final Product p) {
		if (p == null) {
			return;
		}
		final Runnable map = new Runnable() {
			public void run() {
				categoryView.setText(p.getMaincategory_name());
				descriptionInputView.setText(p.getDescription());
				nameInputView.setText(p.getName());								
				weightInputView.setText(p.getWeight().toString());
				statusView.setChecked(p.getStatus() == 1 ? true : false);
				skuTextView.setText(p.getSku());
				priceInputView.setText(p.getPrice());
				quantityInputView.setText(p.getQuantity().toString());
				
				if(TextUtils.isEmpty(((EditText)findViewById(R.id.button)).getText()))
				{
					((EditText)findViewById(R.id.button)).setText(p.getPrice());
					((EditText)findViewById(R.id.button)).setSelection(((EditText)findViewById(R.id.button)).getText().length());
				}
				
				String total = "";
				if(p.getQuantity().compareToIgnoreCase("") != 0)
				{				
					total = String.valueOf(Double.valueOf(p.getPrice()) * Double.valueOf(p.getQuantity()));
					String [] totalParts = total.split("\\.");
					if(totalParts.length > 1)
					{
						if((!totalParts[1].contains("E"))&&(Integer.valueOf(totalParts[1]) == 0))
							total = totalParts[0];
					}
				}
				
				((TextView)findViewById(R.id.total_input)).setText(total);
				
				
				// Show Attributes
				
				ViewGroup vg = (ViewGroup) findViewById(R.id.details_attr_list);
				vg.removeAllViewsInLayout();
				
				for(int i=0;i<p.getAttrList().size();i++)
				{
					if(TextUtils.equals(p.getAttrList().get(i).getLabel(),"Product Barcode"))
					{
						TextView barcodeText = (TextView) findViewById(R.id.details_barcode);
						barcodeText.setText(p.getAttrList().get(i).getValueLabel());
					}
					else
					{
						View v = inflater.inflate(R.layout.product_attribute_view, null);
						TextView label = (TextView) v.findViewById(R.id.attrLabel);
						TextView value = (TextView) v.findViewById(R.id.attrValue);						
						label.setText(p.getAttrList().get(i).getLabel());
						value.setText(p.getAttrList().get(i).getValueLabel());
						
						if(p.getAttrList().get(i).getLabel().contains("Link"))
						{
							Linkify.addLinks(value, Linkify.ALL);
						}
						
						vg.addView(v);
					}
				}
				
				instance = p;
				
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
		loadDetails(false);
	}
	
	private void loadDetails(boolean force) {
		showProgressDialog("Loading Product");
		detailsDisplayed = false;	
		
		new ProductInfoDisplay(force).execute(productId);
	}
	 
	private void showProgressDialog(final String message) {
/*		if (progressDialog != null) {
			return;
		}
		progressDialog = new ProgressDialog(ProductDetailsActivity.this);
		progressDialog.setMessage(message);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.show();*/
		
		ProgressBar progress = (ProgressBar) findViewById(R.id.productLoadingProgressBar);
		progress.setVisibility(View.VISIBLE);
	}
	
	/**
	 * 	Create Order
	 */
	private void createOrder() {
		showProgressDialog("Submitting Order");
		new CreateOrder().execute();
	}
	
	private void dismissProgressDialog() {
	/*	if (progressDialog == null) {
			return;
		}
		progressDialog.dismiss();
		progressDialog = null;*/
		
		ProgressBar progress = (ProgressBar) findViewById(R.id.productLoadingProgressBar);
		progress.setVisibility(View.GONE);
	}
	
	
	private void deleteProduct()
	{
		showProgressDialog("Deleting Product...");
		new DeleteProduct().execute();
	}
	
	/**
	 * Handle click events from "add image" buttons 
	 *
	 * @param v the clicked "add image" button
	 */
	public void onClick(View v) {
		startCameraActivity();
	}

	private void startCameraActivity() {
		String imageName = String.valueOf(System.currentTimeMillis());
		Uri outputFileUri = Uri.fromFile(new File(imagesDir, imageName));
		// save the current image path so we can use it when we want to start the PhotoEditActivity
		currentImgPath = outputFileUri.getEncodedPath();
		
		Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		// the outputFileUri contains the location where the taken image will be saved
		intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
		
		// starting the camera activity to take a picture
		startActivityForResult(intent, CAMERA_ACTIVITY_REQUEST_CODE);
	}

	/**
	 * After the photo was taken with camera app, go to photo edit.
	 * The image path is added as an extra to the intent, under <code>PhotoEditActivity.IMAGE_PATH_ATTR</code>.
	 * Also, a newly created <code>ImagePreviewLayout</code> is added to the <code>imagesLayout</code>
	 * 
	 * @author Bogdan Petran
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (DefaultOptionsMenuHelper.onActivityResult(this, requestCode, resultCode, data) == false)
		{
			resultReceived = true;
			
			if(resultCode != RESULT_OK){
				scrollToBottom();
					
				for (int i = 0; i < imagesLayout.getChildCount(); i++) {
					((ImagePreviewLayout)imagesLayout.getChildAt(i)).updateImageTextSize();
				}
			
				System.out.println("Result was not ok");
				return;
			}
	
			System.out.println("activity result recieved!!!!!!!!!!!");
			switch (requestCode) {
			case CAMERA_ACTIVITY_REQUEST_CODE:
				addNewImage(false, false, currentImgPath);
				startCameraActivity();
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Adds a new <code>ImagePreviewLayout</code> to the imagesLayout
	 */
	private void addNewImage(boolean resizeImg, boolean scrollToBottom, String imagePath){
		
//		if(resizeImg){
//			Options opts = new Options();
//			opts.inSampleSize = 4;
//
//			Bitmap currentImg = BitmapFactory.decodeFile(imagePath, opts);
//			// y FIXME: that's called in the UI thread! why...
//			Util.saveBitmapOnSDcard(currentImg, imagePath);
//		}

		// after the edit is complete and user pressed on the save button, create a new ImagePreviewLayout and start uploading the image to server
		
		if(scrollToBottom)
			scrollToBottom();

		// this is happening when the OS kills this activity because of low memory and all images must be loaded from the server (other details remain saved in their fields)
		if(imagesLayout == null){
			imagesLayout = (LinearLayout) findViewById(R.id.imagesLinearLayout);
			loadImages(productId);
			// new LoadImagesAsyncTask(this).execute(String.valueOf(productId), prevLayout);
		}
		else{
		   //ImagePreviewLayout prevLayout = getImagePreviewLayout(null, null);
		    //prevLayout.setImagePath(imagePath);
		        
			//int childsCount = imagesLayout.getChildCount();
			//requestIdsToViews.put(prevLayout.sendImageToServer(childsCount), prevLayout);
			
			
			JobID jobID = new JobID(RES_UPLOAD_IMAGE, new String[] {imagePath.replace('/', '-')});
			Job uploadImageJob = new Job(jobID);
			
			File file = new File(imagePath);
			
			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_NAME, file.getName());
			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_CONTENT, imagePath);
			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_MIME, "image/jpeg");
			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_SKU, "" + productId);
			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_ID, "" + productId);
			
			uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_IS_MAIN, new Boolean(true));

			
			/* Try to find a proof it's not main image */
			
			List<Job> jobList = mJobControlInterface.getAllImageUploadJobs(productId);
			if (jobList.size() > 0)
			{
				uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_IS_MAIN, new Boolean(false));	
			}

			if (imagesLayout.getChildCount() > 0)
			{
				uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_IS_MAIN, new Boolean(false));
			}
			
			if (instance.getImages().size() > 0)
			{
				uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_IS_MAIN, new Boolean(false));	
			}
			
			//uploadImageJob.putExtraInfo(MAGEKEY_PRODUCT_IMAGE_POSITION, new Integer(mPosition));
			
			/* Shouldn't take too much time. Can probably leave it in UI thread as this happens in the middle of
			 * activity switch. */
			mJobControlInterface.addJob(uploadImageJob);
			
			ImagePreviewLayout newImagePreviewLayout = getUploadingImagePreviewLayout(uploadImageJob, productId);
			newImagePreviewLayout.productDetailsCacheParams = instance.cacheParams;
			imagesLayout.addView(newImagePreviewLayout);
			setMainImageCheckVisibility();				

			// if OS is not killing this activity everything should be ok and the new image layout can be added here
			//imagesLayout.addView(prevLayout);

			// change main image checkbox visibility for the first element if needed
			//setMainImageCheckVisibility();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// save the current image path if the user is in camera view and a low memory event occures, killing this activity
		outState.putString(CURRENT_IMAGE_PATH_ATTR, currentImgPath);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		// get back the current image path after the user returns from the camera view (this should only happen if a low memory event occure)
		if(savedInstanceState != null){
			currentImgPath = savedInstanceState.getString(CURRENT_IMAGE_PATH_ATTR);
		}
	}

	/**
	 * Utility method for constructing a new <code>ImagePreviewLayout</code>
	 * 
	 * @param imageUrl is the image URL which will be shown in the <code>ImageView</code> contained in <code>ImagePreviewLayout</code>. Can be null but then, you must call the <code>sendImageToServer</code> method
	 * @return the newly created layout
	 * 
	 * @see ImagePreviewLayout
	 */
	private ImagePreviewLayout getImagePreviewLayout(String imageUrl, String imageName, int productID) {
		ImagePreviewLayout imagePreview = (ImagePreviewLayout) getLayoutInflater().inflate(R.layout.image_preview, imagesLayout, false);
		imagePreview.setManageClickListener(onClickManageImageListener);

		imagePreview.setProductID(productID);
		
		if (imageName != null) {
		    imagePreview.setImageName(imageName);
		}
		
		if(imageUrl != null){			
			imagePreview.setImageLocalPath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MageventoryImages/" + productId);
			imagePreview.setImageUrl(imageUrl);
			if (imagePreview.getImageView() != null && imagePreview.getImageView().getDrawable() != null) {
			    loadedImages.add(imageUrl);
			}
		}
		
		return imagePreview;
	}
	
	
	/**
	 * Utility method for constructing a new <code>ImagePreviewLayout</code>
	 * 
	 * @param imageUrl is the image URL which will be shown in the <code>ImageView</code> contained in <code>ImagePreviewLayout</code>. Can be null but then, you must call the <code>sendImageToServer</code> method
	 * @return the newly created layout
	 * 
	 * @see ImagePreviewLayout
	 */
	private ImagePreviewLayout getImagePreviewLayout(String imageName) {
		ImagePreviewLayout imagePreview = (ImagePreviewLayout) getLayoutInflater().inflate(R.layout.image_preview, imagesLayout, false);
		imagePreview.setManageClickListener(onClickManageImageListener);
		imagePreview.loadFromSD(imageName,Environment.getExternalStorageDirectory().getAbsolutePath() + "/MageventoryImages/" + productId);
		return imagePreview;
	}

	private ImagePreviewLayout getDownloadingImagePreviewLayout(String imageUrl, String imageName, int productID) {
		ImagePreviewLayout imagePreview = (ImagePreviewLayout) getLayoutInflater().inflate(R.layout.image_preview, imagesLayout, false);
		imagePreview.setProductID(productID);
		imagePreview.setImageUrlNoDownload(imageUrl);
		imagePreview.setManageClickListener(onClickManageImageListener);
		imagePreview.loadFromSDPendingDownload(imageName,Environment.getExternalStorageDirectory().getAbsolutePath() + "/MageventoryImages/" + productId);
		return imagePreview;
	}
	
	private ImagePreviewLayout getUploadingImagePreviewLayout(Job job, int productID) {
		ImagePreviewLayout imagePreview = (ImagePreviewLayout) getLayoutInflater().inflate(R.layout.image_preview, imagesLayout, false);
		imagePreview.setProductID(productID);
		imagePreview.setUploadJob(job, mJobControlInterface);
		imagePreview.setManageClickListener(onClickManageImageListener);
		return imagePreview;
	}
	
	private void startPhotoEditActivity(String imagePath, boolean inEditMode){
		Intent i = new Intent(this, PhotoEditActivity.class);
		i.putExtra(PhotoEditActivity.IMAGE_PATH_ATTR, imagePath);
		i.putExtra(PhotoEditActivity.EDIT_MODE_ATTR, inEditMode);
		
		startActivityForResult(i, PHOTO_EDIT_ACTIVITY_REQUEST_CODE);
	}
	
	/**
	 * Perform a full scroll to bottom of screen
	 */
	private void scrollToBottom(){
		scroller.post(new Runnable() {
			
			@Override
			public void run() {
				scroller.fullScroll(View.FOCUS_DOWN);
			}
		});
	}
	
	/**
	 * Rearange the images layouts and sets the main image as first image
	 * 
	 * @param layout the <code>ImagePreviewLayout</code> to add as top of the list
	 */
	private void setMainImageLayout(ImagePreviewLayout layout){
		
		int layoutIndex = imagesLayout.indexOfChild(layout);
		
		// if the layout checked is already the first layout, ignore
		if(layoutIndex == 0){
			return;
		}
		
		// uncheck the previous first image
		ImagePreviewLayout oldFirstImageLayout = (ImagePreviewLayout) imagesLayout.getChildAt(0);
		oldFirstImageLayout.setMainImageCheck(false);
		
		// removes the layout from it's previous position and add it to the first position
		imagesLayout.removeView(layout);
		imagesLayout.addView(layout, 0);

		// update the index on server for the first layout and then for the rest of them who needs it
		layout.updateImageIndex(String.valueOf(productId), 0);
		
		for (int i = 1; i <= layoutIndex; i++) {
			ImagePreviewLayout layoutToUpdate = (ImagePreviewLayout) imagesLayout.getChildAt(i);
			layoutToUpdate.updateImageIndex(String.valueOf(productId), i);
		}
	}
	
	/**
	 * Show the checkbox on the first/main image only if the images count is > 1
	 */
	private void setMainImageCheckVisibility(){
		if(imagesLayout == null){
			return;
		}
		
		int childCount = imagesLayout.getChildCount();
		
		// this check is done to be sure we can call getChildAt(0)
		if(childCount > 0){
			((ImagePreviewLayout)imagesLayout.getChildAt(0)).setMainImageCheck(true);
		}
	}
	
	/**
	 * Enable/Disable the Photo shoot and first Add image buttons 
	 */
	private void setButtonsEnabled(boolean clickable){
		photoShootBtn.setEnabled(clickable);
		photoShootBtn.setFocusable(clickable);
	}
	
	private class ProductInfoDisplay extends AsyncTask<Object, Void, Boolean> {

		private Product p;
		private final boolean force;
		
		public ProductInfoDisplay(boolean force) {
	        super();
	        this.force = force;
        }

		@Override
		protected Boolean doInBackground(Object... args) {			
			final String[] params = new String[2];
			params[0] = GET_PRODUCT_BY_ID; // ZERO --> Use Product ID , ONE --> Use Product SKU 
			params[1] = String.valueOf(args[0]) ;
			if (force || resHelper.isResourceAvailable(ProductDetailsActivity.this, RES_PRODUCT_DETAILS, params) == false) {
				loadRequestId = resHelper.loadResource(ProductDetailsActivity.this, RES_PRODUCT_DETAILS, params);
				return Boolean.FALSE;
			} else {
				p = resHelper.restoreResource(ProductDetailsActivity.this, RES_PRODUCT_DETAILS, params);
				p.cacheParams = params;
				return Boolean.TRUE;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
			    mapData(p);
			    // start the loading of images			    
			    loadImages(productId);
			}
			else
			{
				refreshImages = true;
			}
		}
	}
	
	private void loadImages(final int productId) {
	synchronized(ImageCachingManager.sSynchronisationObject)
	{
		for (int i = 0; i < imagesLayout.getChildCount(); i++) {
			((ImagePreviewLayout)imagesLayout.getChildAt(i)).deregisterCallbacks(mJobControlInterface);
		}
		imagesLayout.removeAllViews();
		
		if((refreshImages) && (ImageCachingManager.getPendingDownloadCount(productId)==0))
		{
			refreshImages = false;
			ImageCachingManager.removeCachedData(productId);
			for(int i=0;i<instance.getImages().size();i++)
		    {
				ImagePreviewLayout newImagePreviewLayout = getImagePreviewLayout(instance.getImages().get(i).getImgURL(), instance.getImages().get(i).getImgName(), productId);
				newImagePreviewLayout.productDetailsCacheParams = instance.cacheParams;
				imagesLayout.addView(newImagePreviewLayout);
			}
		}
		else
		{
			for(int i=0;i<instance.getImages().size();i++)
			{
				ImagePreviewLayout newImagePreviewLayout = getDownloadingImagePreviewLayout(instance.getImages().get(i).getImgURL(), instance.getImages().get(i).getImgName(), productId);
				newImagePreviewLayout.productDetailsCacheParams = instance.cacheParams;
				imagesLayout.addView(newImagePreviewLayout);
			}
		}
		
		List<Job> list = mJobControlInterface.getAllImageUploadJobs(productId);
		
		for(int i=0;i<list.size();i++)
		{
			ImagePreviewLayout newImagePreviewLayout = getUploadingImagePreviewLayout(list.get(i), productId);
			newImagePreviewLayout.productDetailsCacheParams = instance.cacheParams;
			imagesLayout.addView(newImagePreviewLayout);
		}
		
		for (int i = 0; i < imagesLayout.getChildCount(); i++) {
			((ImagePreviewLayout)imagesLayout.getChildAt(i)).registerCallbacks(mJobControlInterface);
		}
		
		setMainImageCheckVisibility();				
		imagesLayout.setVisibility(View.VISIBLE);
		imagesLoadingProgressBar.setVisibility(View.GONE);
	}
	}
	
	private static class DeleteImageAsyncTask extends AsyncTask<Object, Void, ImagePreviewLayout>{

		// use WeekReference to prevent memory leaks
		WeakReference<ProductDetailsActivity> activityReference;
		final ProductDetailsActivity activityInstance;

		public DeleteImageAsyncTask(ProductDetailsActivity instance){
			activityReference = new WeakReference<ProductDetailsActivity>(instance);
			activityInstance = activityReference.get();
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected ImagePreviewLayout doInBackground(Object... params) {

			ImagePreviewLayout layoutToRemove = (ImagePreviewLayout) params[1];

			MagentoClient magentoClient = activityInstance.app.getClient();
			try{
				boolean deleteSuccesfull = (Boolean) magentoClient.execute("catalog_product_attribute_media.remove",
						new Object[] {params[0], layoutToRemove.getImageName()});									 // params[0] is the product id

				if(deleteSuccesfull){
					return layoutToRemove;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(ImagePreviewLayout result) {
			if (result == null) {
				Toast.makeText(activityInstance.getApplicationContext(), "Could not delete image.",
						Toast.LENGTH_SHORT).show();
				return;
			}

			// remove the image preview layout from the images layout (which contains all images for the current product)
			activityInstance.imagesLayout.removeView(result);
			activityInstance.setMainImageCheckVisibility();
		}
	}
	
	/**
	 * Handler for image deletion and image click inside <code>ProductDetailsActivity</code>. 
	 * This will be notified from <code>ImagePreviewLayout</code> when the "Delete" button or the image is being clicked.
	 *
	 * @author Bogdan Petran
	 * @see ImagePreviewLayout
	 */
	private static class ClickManageImageListener implements IOnClickManageHandler{

		WeakReference<ProductDetailsActivity> activityReference; 
		final ProductDetailsActivity activityInstance; 

		public ClickManageImageListener(ProductDetailsActivity instance){ 
			activityReference = new WeakReference<ProductDetailsActivity>(instance); 
			activityInstance = activityReference.get(); 
		}

		@Override
		public void onDelete(final ImagePreviewLayout layoutToRemove) {

			// show the delete confirmation when the delete button was pressed on an item
			Builder alertDialogBuilder = new Builder(activityInstance);
			alertDialogBuilder.setTitle("Confirm deletion");
			alertDialogBuilder.setNegativeButton("No", null);
			alertDialogBuilder.setPositiveButton("Yes", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					layoutToRemove.setLoading(true);

					// start a task to delete the image from server
					new DeleteImageAsyncTask(activityInstance).execute(String.valueOf(activityInstance.productId), layoutToRemove);
				}
			});

			alertDialogBuilder.show();

		}

		@Override
		public void onClickForEdit(ImagePreviewLayout layoutToEdit) {
			// open PhotoEditActivity in preview mode with zoom
			activityInstance.startPhotoEditActivity(layoutToEdit.getUrl(), false);
		}

		@Override
		public void onClickForMainImage(ImagePreviewLayout layoutToEdit) {
			//TODO: update the main image on server and layout
			activityInstance.setMainImageLayout(layoutToEdit);
			
		}
	}

	private static class ScrollListener implements IScrollListener{

		WeakReference<ProductDetailsActivity> activityReference;
		final ProductDetailsActivity activityInstance;

		public ScrollListener(ProductDetailsActivity instance){
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
	
	/**
	 * Should be called only once and that would be best to be done in onCreate().
	 * @param enabled
	 */
	private void setEditEnabled(boolean enabled) {
		if (enabled) {
			updateBtn.setVisibility(View.VISIBLE);
			return;
		}
		for (final EditText detail : detailViews) {
			detail.setInputType(EditorInfo.TYPE_NULL);
			detail.setKeyListener(null);
		}
	}
	
	// TODO y: move this in a background task
	private void startUpdateOperation() {
		showProgressDialog("Updating product...");

		final String name = nameInputView.getText().toString();
		final String price = priceInputView.getText().toString();
		final String description = descriptionInputView.getText().toString();
		/*final String status = statusView.getText().toString();*/ // make this a spinner as it is in the product creation screen
		final String weight = weightInputView.getText().toString();
		final String categoryId = categoryView.getText().toString();
		final String quantity = quantityInputView.getText().toString();
		
		final Bundle data = new Bundle(8);
		data.putString(MAGEKEY_PRODUCT_NAME, name);
		data.putString(MAGEKEY_PRODUCT_PRICE, price);
		data.putString(MAGEKEY_PRODUCT_DESCRIPTION, description);
		data.putString(MAGEKEY_PRODUCT_SHORT_DESCRIPTION, description);
		/*data.putString(MAGEKEY_PRODUCT_STATUS, "" + status);
		data.putString(MAGEKEY_PRODUCT_WEIGHT, weight);*/
		data.putSerializable(MAGEKEY_PRODUCT_CATEGORIES, new Object[] { String.valueOf(categoryId) });
		data.putString(MAGEKEY_PRODUCT_QUANTITY, quantity);
		
		updateRequestId = ResourceServiceHelper.getInstance().loadResource(this, RES_CATALOG_PRODUCT_UPDATE,
		        new String[] { String.valueOf(productId) }, data);
	}
		
	/**
	 * Create Order Invoice
	 * @author hussein
	 *
	 */
	private class CreateOrder extends AsyncTask<Integer, Integer, String> {

		@Override
		protected String doInBackground(Integer... ints) {
			
			// 2- Set Product Information
			final String sku = instance.getSku();
			final String price = instance.getPrice().toString();
			String soldPrice = ((EditText)findViewById(R.id.button)).getText().toString();
			final String qty = ((EditText)findViewById(R.id.qtyText)).getText().toString();
			
			// Check If Sold Price is empty then set the sold price with price
			if(soldPrice.compareToIgnoreCase("") == 0)
			{
				soldPrice = price;
			}
			
			final String name = instance.getName();
			
			try {
				final Bundle bundle = new Bundle();
				/* PRODUCT INFORMAITON */
				bundle.putString(MAGEKEY_PRODUCT_SKU, sku);
				bundle.putString(MAGEKEY_PRODUCT_QUANTITY, qty);
				bundle.putString(MAGEKEY_PRODUCT_PRICE, soldPrice);
				bundle.putString(MAGEKEY_PRODUCT_NAME, name);
				
				orderCreateID = resHelper.loadResource(ProductDetailsActivity.this,RES_CART_ORDER_CREATE, null, bundle);
				return null;
			} catch (Exception e) {
				Log.w(TAG, "" + e);
				return null;
			}			
		}		
	}	
	
	/**
	 *   Implement onCreateDialogue 
	 *   Show the Sold Confirmation Dialogue 
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		
		switch (id) {
		case SOLD_CONFIRMATION_DIALOGUE:
			AlertDialog.Builder soldDialogueBuilder = new AlertDialog.Builder(ProductDetailsActivity.this);
			
			soldDialogueBuilder.setTitle("Confirmation");
			soldDialogueBuilder.setMessage("Sell Product ? ");
			soldDialogueBuilder.setCancelable(false);
			
			// If Pressed OK Submit the Order With Details to Site
			soldDialogueBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					/* Verify then Create */
					if(isVerifiedData())
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

			
		case SOLD_ORDER_SUCCESSEDED:
			AlertDialog.Builder successDlgBuilder = new AlertDialog.Builder(ProductDetailsActivity.this);
			
			resHelper.markResourceAsOld(ProductDetailsActivity.this, RES_CART_ORDER_CREATE,null);
			
			successDlgBuilder.setTitle("Information");
			successDlgBuilder.setMessage("Order Created");
			successDlgBuilder.setCancelable(false);
			
			// If Pressed OK Submit the Order With Details to Site
			successDlgBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// Reset Sold Price TextView & Qty TestView
					((EditText)findViewById(R.id.qtyText)).setText("1");
					((EditText)findViewById(R.id.button)).setText(String.valueOf(instance.getPrice()));
												
					// Mark Resource as Old
					resHelper.markResourceAsOld(getApplicationContext(), RES_PRODUCT_DETAILS);
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
						Settings settings=new Settings(getApplicationContext());
						String url = settings.getUrl() + "/index.php/admin/catalog_product/edit/id/"+ instance.getId();
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(url));
						startActivity(intent);						
						break;
						
					case MITEM_ADD_IMAGE:
						// Scroll Down to Image Layout
						int LLdown = ((LinearLayout) findViewById(R.id.detailsMainLL)).getBottom();
						scroller.scrollTo(0,LLdown);
						break;
											
					case MITEM_EDIT:
					    startEditActivity();
						break;
						
					case MITEM_DELETE:
						showDialog(SHOW_DELETE_DIALOGUE);
						break;
						
					case MITEM_SHOP:
						Settings settings2=new Settings(getApplicationContext());
						String url2 = settings2.getUrl() + "/" +instance.getUrlPath();
						
						Intent intent2 = new Intent(Intent.ACTION_VIEW);
						intent2.setData(Uri.parse(url2));
						startActivity(intent2);						
						break;
						

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
	    i.putExtra(getString(R.string.ekey_product_id), productId);
	    startActivity(i);
	}

	// Verify Price & Quantity
	private boolean isVerifiedData()
	{
		// 1- Check that price is numeric
		try
		{
			Double testPrice = Double.parseDouble(((EditText)findViewById(R.id.button)).getText().toString());
		}catch (Exception e) {
			// TODO: handle exception
			Toast.makeText(getApplicationContext(), "Invalid Sold Price", Toast.LENGTH_SHORT).show();
			return false;
		}
		
		// 2- Check that Qty is numeric
		Double testQty = 0.0;
		try
		{
			testQty = Double.parseDouble(((EditText)findViewById(R.id.qtyText)).getText().toString());
		}catch (Exception e) {
			// TODO: handle exception
			Toast.makeText(getApplicationContext(), "Invalid Quantity", Toast.LENGTH_SHORT).show();
			return false;
		}
							
		// All Tests Passed
		return true;
	}
	
		
	TextWatcher evaluteTotal() 
	{
		TextWatcher textWatcher = new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
				String SoldPrice = ((EditText)findViewById(R.id.button)).getText().toString();
				String SoldQty = ((EditText)findViewById(R.id.qtyText)).getText().toString();

				// if Either QTY or Price is empty then total is Empty too
				// and return
				if((SoldPrice.compareTo("") == 0) ||(SoldQty.compareTo("") == 0) )
				{
					((EditText)findViewById(R.id.totalText)).setText(String.valueOf(""));
					return;
				}
				
				// Else Calculate Total
				float price = Float.parseFloat(SoldPrice);
				float qty = Float.parseFloat(SoldQty);				
				float total = price * qty;
				
				String totalStr = String.valueOf(total);
				String [] totalStrParts = totalStr.split("\\.");
				if(totalStrParts.length > 1)
				{
					if( (!totalStrParts[1].contains("E")) && (Integer.valueOf(totalStrParts[1]) == 0))
						totalStr = totalStrParts[0];
				}
								
				((EditText)findViewById(R.id.totalText)).setText(totalStr);			
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
		};
		
		return textWatcher;
	}
	
	TextWatcher evalutePrice() 
	{
		TextWatcher textWatcher = new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
				
				// Get The Focus
				
				String totalText = ((EditText)findViewById(R.id.totalText)).getText().toString();
				String SoldQty = ((EditText)findViewById(R.id.qtyText)).getText().toString();

				// if Either QTY or Price is empty then total is Empty too
				// and return
				if((totalText.compareTo("") == 0) ||(SoldQty.compareTo("") == 0) )
				{
					((EditText)findViewById(R.id.totalText)).setText(String.valueOf(""));
					return;
				}
				
				// Else Calculate Total
				double total = Double.parseDouble(totalText);
				double qty = Double.parseDouble(SoldQty);				
				double price = total / qty;
				
				String priceStr = String.valueOf(price);
				String [] priceStrParts = priceStr.split("\\.");
				if(priceStrParts.length > 1)
				{
					if( (!priceStrParts[1].contains("E")) && (Integer.valueOf(priceStrParts[1]) == 0))
						priceStr = priceStrParts[0];
				}
								
				((EditText)findViewById(R.id.totalText)).setText(priceStr);			
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
		};
		
		return textWatcher;
	}
	

	@SuppressWarnings("unchecked")
	private void buildAtrList(List<Map<String, Object>> atrListData)
	{		
		/*if(atrListData.size() > 0)
		{						
			ViewGroup vg = (ViewGroup) findViewById(R.id.details_attr_list);
			vg.removeAllViewsInLayout();
			Map<String,Object> attrList = instance.getAttrValuesList();
			
			for(int i =0;i<atrListData.size();i++)
			{
				Map<String,Object>attribute = atrListData.get(i);
				final String code = attribute.get(MAGEKEY_ATTRIBUTE_CODE).toString();
				final String name = attribute.get(MAGEKEY_ATTRIBUTE_INAME).toString();
				final String type = attribute.get(MAGEKEY_ATTRIBUTE_TYPE).toString();
								
							}
		}*/
	}
	
	/**
	 * Create Order Invoice
	 * @author hussein
	 *
	 */
	private class DeleteProduct extends AsyncTask<Integer, Integer, String> {

		@Override
		protected String doInBackground(Integer... ints) {
			
			try {
				final Bundle bundle = new Bundle();
				/* PRODUCT INFORMAITON */
				bundle.putString(MAGEKEY_PRODUCT_SKU, instance.getSku());
			
				deleteProductID = resHelper.loadResource(ProductDetailsActivity.this,RES_PRODUCT_DELETE, null, bundle);
				return "";
			} catch (Exception e) {
				Log.w(TAG, "" + e);
				return null;
			}			
		}		
	}	

	
}