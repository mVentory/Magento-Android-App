package com.mageventory;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.LightingColorFilter;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import com.mageventory.client.MagentoClient;
import com.mageventory.components.ImagePreviewLayout;
import com.mageventory.components.ProductDetailsScrollView;
import com.mageventory.interfaces.IOnClickManageHandler;
import com.mageventory.interfaces.IScrollListener;
import com.mageventory.model.Category;
import com.mageventory.model.Product;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;

public class ProductDetailsActivity extends BaseActivity implements MageventoryConstants, OperationObserver {
	
	private static final int PHOTO_EDIT_ACTIVITY_REQUEST_CODE = 0;			// request code used to start the PhotoEditActivity
	private static final int CAMERA_ACTIVITY_REQUEST_CODE = 1;				// request code used to start the Camera activity
	private static final String CURRENT_IMAGE_PATH_ATTR = "current_path";	/* attribute used to save the current image path if a low memory event occures on a device and 
																			   while in the camera mode, the current activity may be closed by the OS */
	ArrayList<Category> categories;
	ProgressDialog progressDialog;
	MyApplication app;
	
	Activity activity = null;

	String path;					// path of the images directory for the current product
	File imagesDir;
	String currentImgPath;			// this will actually be: path + "/imageName"
	LinearLayout imagesLayout;		// the layout which will contain ImagePreviewLayout objects
	boolean resultReceived = false;
	ProductDetailsScrollView scrollView;
	Button addImageFirstBtn;
	Button addImageSecondBtn;
	ProgressBar imagesLoadingProgressBar;
	ScrollView scroller;

	ClickManageImageListener onClickManageImageListener;
	ScrollListener scrollListener;
	
	// detail views
	private EditText categoriesView;
	private EditText descriptionInputView;
	private EditText nameInputView;
	private EditText priceInputView;
	private EditText statusView;
	private EditText weightInputView;
	
	// product data
	private int product_id;
	private Product instance;
	
	// resources
	private int requestId = INVALID_REQUEST_ID;
	private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
	private boolean detailsDisplayed = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.product_details);
		
		// map views
		categoriesView = (EditText) findViewById(R.id.product_categories);
		descriptionInputView = (EditText) findViewById(R.id.product_description_input);
		nameInputView = (EditText) findViewById(R.id.product_name_input);
		priceInputView = (EditText) findViewById(R.id.product_price_input);
		statusView = (EditText) findViewById(R.id.product_status);
		weightInputView = (EditText) findViewById(R.id.product_weight_input);
		
		// retrieve product id
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			product_id = extras.getInt(getString(R.string.ekey_product_id), INVALID_PRODUCT_ID);
		} else {
			product_id = INVALID_PRODUCT_ID;
		}
		
		// retrieve last instance
		instance = (Product) getLastNonConfigurationInstance();

		app = (MyApplication) getApplication();
		activity = this;

		this.setTitle("Mventory: Product Details");
	
		imagesLoadingProgressBar = (ProgressBar) findViewById(R.id.imagesLoadingProgressBar);
		scroller = (ScrollView) findViewById(R.id.scrollView1);
		
		((Button)findViewById(R.id.button2)).getBackground().setColorFilter(new LightingColorFilter(0x444444, 0x737575));
		((Button)findViewById(R.id.button3)).getBackground().setColorFilter(new LightingColorFilter(0x444444, 0x737575));
		
		scrollListener = new ScrollListener(this);
		
		scrollView = (ProductDetailsScrollView) findViewById(R.id.scrollView1);
		scrollView.setScrollToBottomListener(scrollListener);
		
		addImageFirstBtn = (Button) findViewById(R.id.addImageFirstBtn);
		addImageSecondBtn = (Button) findViewById(R.id.addImageSecondBtn);
		
		// the absolute path of the images directory for the current product (here will be stored the images received from server)
		path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MageventoryImages/" + product_id;

		imagesDir = new File( path );
		onClickManageImageListener = new ClickManageImageListener(this);

		if(!imagesDir.exists()){
			imagesDir.mkdirs();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		resHelper.registerLoadOperationObserver(this);
		if (detailsDisplayed == false) {
			loadDetails();
		}

		// this happens when OS is killing this activity (e.g. if user goes to
		// Camera activity) and we don't need to load all product details, only
		// the images
		if (!resultReceived) {
			imagesLayout = (LinearLayout) findViewById(R.id.imagesLinearLayout);
			
			// TODO y: check if we should start a new task here

			/* Start loading Details */
//			ProductInfoRetrieve pir = new ProductInfoRetrieve();
//			pir.execute(new String[] { product_id });
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		resHelper.unregisterLoadOperationObserver(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			loadDetails();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onLoadOperationCompleted(LoadOperation op) {
		if (requestId != op.getOperationRequestId()) {
			return;
		}
		if (op.getException() != null) {
			Toast.makeText(this, "" + op.getException().getMessage(), Toast.LENGTH_LONG).show();
			return;
		}
		loadDetails();
	}
	
	private void mapData(final Product p) {
		if (p == null) {
			return;
		}
		final Runnable map = new Runnable() {
			public void run() {
				categoriesView.setText(p.getMaincategory_name());
				descriptionInputView.setText(p.getDescription());
				nameInputView.setText(p.getName());
				priceInputView.setText(p.getPrice().toString());
				weightInputView.setText(p.getWeight().toString());
				statusView.setText(p.getStatus() == 1 ? "Enabled" : "Disabled");
				
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
		showProgressDialog("Loading Product");
		detailsDisplayed = false;
		new ProductInfoDisplay().execute(product_id);
	}
	 
	private void showProgressDialog(final String message) {
		if (progressDialog != null) {
			return;
		}
		progressDialog = new ProgressDialog(ProductDetailsActivity.this);
		progressDialog.setMessage(message);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(true);
		progressDialog.show();
	}
	
	private void dismissProgressDialog() {
		if (progressDialog == null) {
			return;
		}
		progressDialog.dismiss();
		progressDialog = null;
	}
	
//	@Override
//	protected void onResume() {
//		super.onResume();
//		
//		// this happens when OS is killing this activity (e.g. if user goes to Camera activity) and we don't need to load all product details, only the images
//		if(!resultReceived){
//			imagesLayout = (LinearLayout) findViewById(R.id.imagesLinearLayout);
//			
//			/* Start loading Details */
//			ProductInfoRetrieve pir = new ProductInfoRetrieve();
//			pir.execute(new String[] { product_id });
//		}
//	}

//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//		case R.id.menu_refresh:
//			ProductInfoRetrieve pir = new ProductInfoRetrieve();
//			pir.execute(new String[] { product_id });
//			return true;
//		}
//		return super.onOptionsItemSelected(item);
//	}

	/**
	 * Handle click events from "add image" buttons 
	 *
	 * @param v the clicked "add image" button
	 */
	public void onClick(View v) {
		String imageName = String.valueOf(System.currentTimeMillis());
		Uri outputFileUri = Uri.fromFile(new File(imagesDir, imageName));
		// save the current image path so we can use it when we want to start the PhotoEditActivity
		currentImgPath = outputFileUri.getEncodedPath();

		Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE );
		// the outputFileUri contains the location where the taken image will be saved
		intent.putExtra( MediaStore.EXTRA_OUTPUT, outputFileUri );

		// starting the camera activity to take a picture
		startActivityForResult( intent, CAMERA_ACTIVITY_REQUEST_CODE);											
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
		resultReceived = true;
		
		if(resultCode != RESULT_OK){
			return;
		}

		System.out.println("activity result recieved!!!!!!!!!!!");
		switch (requestCode) {
		case CAMERA_ACTIVITY_REQUEST_CODE:
			// when a result ok is received from Camera activity, start the PhotoEditActivity 
			startPhotoEditActivity(currentImgPath, true);
			break;
		case PHOTO_EDIT_ACTIVITY_REQUEST_CODE:
			// after the edit is complete and user pressed on the save button, create a new ImagePreviewLayout and start uploading the image to server
			String imagePath = data.getStringExtra(PhotoEditActivity.IMAGE_PATH_ATTR);
			ImagePreviewLayout prevLayout = getImagePreviewLayout(null);
			prevLayout.setImagePath(imagePath);

			scrollToBottom();
			
			// this is happening when the OS kills this activity because of low memory and all images must be loaded from the server (other details remain saved in their fields)
			if(imagesLayout == null){
				imagesLayout = (LinearLayout) findViewById(R.id.imagesLinearLayout);
				
				new LoadImagesAsyncTask(this).execute(String.valueOf(product_id), prevLayout);
			}
			else{
				int childsCount = imagesLayout.getChildCount();
				prevLayout.sendImageToServer(childsCount);			// start upload
				
				// if OS is not killing this activity everything should be ok and the new image layout can be added here
				imagesLayout.addView(prevLayout);
				
				// change main image checkbox visibility for the first element
				setMainImageCheckVisibility();
			}
			break;
		default:
			break;
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
	 * @param imageName is the image URL which will be shown in the <code>ImageView</code> contained in <code>ImagePreviewLayout</code>. Can be null but then, you must call the <code>sendImageToServer</code> method
	 * @return the newly created layout
	 * 
	 * @see ImagePreviewLayout
	 */
	private ImagePreviewLayout getImagePreviewLayout(String imageName){
		ImagePreviewLayout imagePreview = (ImagePreviewLayout) getLayoutInflater().inflate(R.layout.image_preview, imagesLayout, false);
		imagePreview.setManageClickListener(onClickManageImageListener);

		if(imageName != null){
			imagePreview.setImageName(imageName);
		}

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
		
		// removes the layout from it's previeous position and add it to the first position
		imagesLayout.removeView(layout);
		imagesLayout.addView(layout, 0);

		// update the index on server for the first layout and then for the rest of them who needs it
		layout.updateImageIndex(String.valueOf(product_id), 0);
		
		for (int i = 1; i <= layoutIndex; i++) {
			ImagePreviewLayout layoutToUpdate = (ImagePreviewLayout) imagesLayout.getChildAt(i);
			layoutToUpdate.updateImageIndex(String.valueOf(product_id), i);
		}
	}
	
	private void setMainImageCheckVisibility(){
		if(imagesLayout == null){
			return;
		}
		
		int childCount = imagesLayout.getChildCount();
		
		if(childCount > 0){
			((ImagePreviewLayout)imagesLayout.getChildAt(0)).showCheckBox(childCount > 1);
		}	
	}
	
	private class ProductInfoDisplay extends AsyncTask<Object, Void, Boolean> {

		private Product p;
		
		@Override
		protected Boolean doInBackground(Object... args) {
			final String[] params = new String[] { String.valueOf(args[0]) };
			if (resHelper.isResourceAvailable(ProductDetailsActivity.this, RES_PRODUCT_DETAILS, params)) {
				p = resHelper.restoreResource(ProductDetailsActivity.this, RES_PRODUCT_DETAILS, params);
				return Boolean.TRUE;
			} else {
				requestId = resHelper.loadResource(ProductDetailsActivity.this, RES_PRODUCT_DETAILS, params);
				return Boolean.FALSE;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			mapData(p);
			// start the load images process
			new LoadImagesAsyncTask(ProductDetailsActivity.this).execute(String.valueOf(product_id));
		}
		
	}

//	private class ProductInfoRetrieve extends AsyncTask<String, Integer, Product> {
//		
//		@Override
//		protected void onPreExecute() {
//			showProgressDialog("Loading Product");
//		}
//
//		@Override
//		protected Product doInBackground(String... st) {
//
//			try {
//				magentoClient = app.getClient();
//				Object o = magentoClient.execute("catalog_product.info",
//						new Object[] { st[0] });
//				HashMap map = (HashMap) o;
//				Product product = new Product(map, true);
//				if (!product.getMaincategory().equalsIgnoreCase("")) {
//					Object c = magentoClient.execute("catalog_category.info",
//							new Object[] { product.getMaincategory() });
//					HashMap cat = (HashMap) c;
//					product.setMaincategory_name((String) cat.get("name"));
//				}
//
//				return product;
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//
//			return null;
//
//		}
//
//		@Override
//		protected void onPostExecute(Product result) {
//			if (result == null) {
//				Toast.makeText(getApplicationContext(), "Request Error",
//						Toast.LENGTH_SHORT).show();
//				return;
//			}
//
//			((EditText) findViewById(R.id.product_name_input)).setText(result
//					.getName());
//			((EditText) findViewById(R.id.product_price_input)).setText(result
//					.getPrice().toString());
//			((EditText) findViewById(R.id.product_description_input))
//			.setText(result.getDescription());
//			((EditText) findViewById(R.id.product_weight_input)).setText(result
//					.getWeight().toString());
//			((EditText) findViewById(R.id.product_categories)).setText(result
//					.getMaincategory_name());
//
//			if (result.getStatus() == 1) {
//				((EditText) findViewById(R.id.product_status))
//				.setText("Enabled");
//			} else {
//				((EditText) findViewById(R.id.product_status))
//				.setText("Disabled");
//			}
//
//			dismissProgressDialog();
//
//			// start the load images process
//			new LoadImagesAsyncTask(ProductDetailsActivity.this).execute(String.valueOf(product_id));
//
//			// end execute
//		}
//	}

	private static class LoadImagesAsyncTask extends AsyncTask<Object, Void, Object[]>{

		WeakReference<ProductDetailsActivity> activityReference;
		final ProductDetailsActivity activityInstance;

		public LoadImagesAsyncTask(ProductDetailsActivity instance){
			activityReference = new WeakReference<ProductDetailsActivity>(instance);
			activityInstance = activityReference.get();
		}

		@Override
		protected void onPreExecute() {
			activityInstance.imagesLayout.setVisibility(View.GONE);
			activityInstance.imagesLoadingProgressBar.setVisibility(View.VISIBLE);
			
			//disable the first add image button
			activityInstance.addImageFirstBtn.setEnabled(false);
			activityInstance.addImageFirstBtn.setFocusable(false);
		}

		@Override
		protected Object[] doInBackground(Object... params) {

			// get the images for the current product from server
			MagentoClient magentoClient = activityInstance.app.getClient();
			try{
				Object[] imagesArray = (Object[]) magentoClient.execute("catalog_product_attribute_media.list",
						new Object[] { params[0] });

				// use an array for the result (for the case when we need to add one extra ImagePreviewLayout on low memory which will be result[1])
				Object[] result = new Object[params.length];		
				ImagePreviewLayout[] imageNames = new ImagePreviewLayout[imagesArray.length];

				// build the images layout with the images received from server in background
				for (int i = 0; i < imagesArray.length; i++) {
					@SuppressWarnings("unchecked")
					HashMap<String, Object> imgMap = (HashMap<String, Object>) imagesArray[i];
					imageNames[i] = activityInstance.getImagePreviewLayout((String) imgMap.get("file"));
				}

				result[0] = imageNames;								// this is an array with ImagePreviewLayout built to be added in images layout

				if(params.length > 1){
					result[1] = params[1];							// this is the ImagePreviewLayout to be added at the end after loading other images
				}

				return result;

			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(Object[] result) {
			if(result != null){
				activityInstance.imagesLayout.removeAllViews();										// when we make a load from server we need to have the list of images empty

				ImagePreviewLayout[] imagesFromServer = (ImagePreviewLayout[]) result[0];			// response from server

				// build the images layout with the images received from server
				for (int i = 0; i < imagesFromServer.length; i++) {
					activityInstance.imagesLayout.addView(imagesFromServer[i]);
				}

				if(result.length > 1){
					ImagePreviewLayout prevLayout = (ImagePreviewLayout) result[1];					// the layout to add when and if necessary
					prevLayout.sendImageToServer(activityInstance.imagesLayout.getChildCount());

					activityInstance.imagesLayout.addView(prevLayout);

					// when getting back from Photo edit, and ProductDetailsActivity was destroyed because of low memory, after the images are loaded from server, scroll to bottom to see the image upload to server
					activityInstance.scrollToBottom();
				}

				ImagePreviewLayout firstImageLayout = (ImagePreviewLayout) activityInstance.imagesLayout.getChildAt(0);
				if (firstImageLayout != null) {
					firstImageLayout.setMainImageCheck(true);
				}

				activityInstance.setMainImageCheckVisibility();
			}
			
			activityInstance.imagesLayout.setVisibility(View.VISIBLE);
			activityInstance.imagesLoadingProgressBar.setVisibility(View.GONE);
			
			// enable the first add image button
			activityInstance.addImageFirstBtn.setEnabled(true);
			activityInstance.addImageFirstBtn.setFocusable(true);
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
			removeSecondAddImageBtnIfNeeded();
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
			removeSecondAddImageBtnIfNeeded();
			
			activityInstance.setMainImageCheckVisibility();
		}

		private void removeSecondAddImageBtnIfNeeded(){
			try{
				if(activityInstance.addImageFirstBtn.getLocalVisibleRect(new Rect())){		// if the first add image button is not visible
					activityInstance.addImageSecondBtn.setVisibility(View.GONE);			// set the visibility to the second add image button
					activityInstance.scrollView.requestLayout();
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
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
					new DeleteImageAsyncTask(activityInstance).execute(String.valueOf(activityInstance.product_id), layoutToRemove);
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
			try {
				if(!activityInstance.addImageFirstBtn.getLocalVisibleRect(new Rect())){		// if the first add image button is not visible
					activityInstance.addImageSecondBtn.setVisibility(View.VISIBLE);			// set the visibility to the second add image button
					activityInstance.scrollView.requestLayout();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		@Override
		public void scrollMoved() {
			try {
				activityInstance.addImageSecondBtn.setVisibility(View.GONE);			// when scrolled away from bottom, set visibility to the second button to false
				activityInstance.scrollView.requestLayout();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}
}