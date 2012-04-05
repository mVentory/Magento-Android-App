package com.mageventory.components;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.client.MagentoClient;
import com.mageventory.interfaces.IOnClickManageHandler;

/**
 * LinearLayout containing three elements: one <code>ImageView</code>, one delete <code>Button</code> and one <code>CheckBox</code>
 * and an indeterminate progress bar for the loading, the deletion or the update of the image on server 
 *
 * @author Bogdan Petran
 */
public class ImagePreviewLayout extends FrameLayout {

	/**
	 * This task sends the image from given image path (params[0]) to server and then sets that image to <code>ImageView</code>
	 */
	private class SaveImageOnServerTask extends AsyncTask<String, Void, String>{

		@Override
		protected void onPreExecute() {
			// a view holder image is set until the upload is complete
			setLoading(true);
		}

		@Override
		protected String doInBackground(String... params) {
			try {

				imagePath = params[0];

				HashMap<String, Object> image_data = new HashMap<String, Object>();

				File imgFile = null;
				// read bytes from image file
				imgFile = new File(getImagePath());
				RandomAccessFile f = new RandomAccessFile(imgFile, "r");
				byte[] buff = new byte[(int)f.length()];
				f.read(buff);

				// build the request data
				HashMap<String, Object> file_data = new HashMap<String, Object>();

				file_data.put("name", imgFile.getName());
				file_data.put("content", Base64.encode(buff, Base64.DEFAULT));
				file_data.put("mime", "image/jpeg");

				image_data.put("file", file_data);
				image_data.put("position", index);
				image_data.put("exclude", 0);
				
				if(index == 0){
					// make first image as main image on server
					image_data.put("types", new Object[]{"image", "small_image", "thumbnail"});
				}

				MyApplication app = (MyApplication)((Activity) getContext()).getApplication();
				MagentoClient magentoClient = app.getClient();

				String imageNameFromServer;
				
				// make request
				synchronized (lockMutex) {
					imageNameFromServer = (String) magentoClient.execute("catalog_product_attribute_media.create", 
							new Object[] {imgFile.getParentFile().getName(), image_data});
				}
				
				// after the response arrived, the image file is deleted because we don't need it to ocupy space on sdcard
				imgFile.delete();

				return imageNameFromServer;
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(String result) {

			if (result == null && errorCounter == 0) {
				// retry the image upload when some kind of error occured
				Toast.makeText(getContext(), "Error uploading image. Retrying upload!",
						Toast.LENGTH_SHORT).show();

				errorCounter++;

				new SaveImageOnServerTask().execute(getImagePath());
				return;
			}
			else if(errorCounter > 0){
				// this is the second time when an error occures and we need to delete this layout
				Toast.makeText(getContext(), "Upload error. Image will be deleted!",
						Toast.LENGTH_SHORT).show();

				onClickManageHandler.onDelete(ImagePreviewLayout.this);
				return;
			}

			if(index == 0){
				setMainImageCheck(true);
			}
			
			setImageName(result);
			setLoading(false);
		}
	}
	
	/**
	 * This task updates the image position on server
	 */
	private class UpdateImageOnServerTask extends AsyncTask<String, Void, Boolean>{

		@Override
		protected void onPreExecute() {
			// a view holder image is set until the upload is complete
			setLoading(true);
		}

		@Override
		protected Boolean doInBackground(String... params) {

			String productId = params[0];

			MyApplication app = (MyApplication)((Activity) getContext()).getApplication();
			MagentoClient magentoClient = app.getClient();

			// build the request data
			HashMap<String, Object> image_data = new HashMap<String, Object>();

			image_data.put("position", index);
			
			if(index == 0){
				// make first image as main image on server
				image_data.put("types", new Object[]{"image", "small_image", "thumbnail"});
			}

			boolean responseFromServer;
			
			// make a synchronized request
			synchronized (lockMutex) {
				responseFromServer = (Boolean) magentoClient.execute("catalog_product_attribute_media.update", 
						new Object[] {productId, imageName, image_data});
			}

			return responseFromServer;
		}

		@Override
		protected void onPostExecute(Boolean result) {

			//TODO: result can be true or false but don't know what to do in either of this cases because an image can recieve a true result while another false
			
			setLoading(false);
		}
	}

	private class EventListener implements OnClickListener, OnCheckedChangeListener{

		WeakReference<ImagePreviewLayout> viewReference; 
		final ImagePreviewLayout viewInstance; 
		
		public EventListener(ImagePreviewLayout instance){ 
			viewReference = new WeakReference<ImagePreviewLayout>(instance); 
			viewInstance = viewReference.get(); 
		} 
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.imageViewHolder:
				// if this will be necessary, it will start the edit image view when clicking the image
				onClickManageHandler.onClickForEdit(viewInstance);
				break;
			case R.id.deleteBtn:
				// notify to delete current layout and image from server
				onClickManageHandler.onDelete(viewInstance);
				break;
			default:
				break;
			}
		}

		@Override
		public void onCheckedChanged(final CompoundButton buttonView,
				final boolean isChecked) {
			
			System.out.println("is checked: " + isChecked + " ask for aproval: " + askForMainImageApproval );
			// don't let this button to be clicked when it's checked but let it when it's unchecked
			buttonView.setFocusable(!isChecked);
			buttonView.setClickable(!isChecked);
			
			if(isChecked && askForMainImageApproval){
				askForMainImageApproval = true;

				Builder dialogBuilder = new Builder(getContext());
				dialogBuilder.setMessage("Make it the main product image?");
				dialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						buttonView.setChecked(false);
					}
				});
				
				dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						// notify to set the selected image as main/first image
						onClickManageHandler.onClickForMainImage(viewInstance);
					}
				});
				
				dialogBuilder.show();
			}
		}
	}
	
	private EventListener eventListener;

	private String IMAGES_URL = "http://mventory.simple-helix.net/media/catalog/product";

	private String url;									// this will be IMAGES_URL + "imageName"
	private String imageName;
	private ImageView imgView;
	private Button deleteBtn;
	private ProgressBar loadingProgressBar;
	private CheckBox mainImageCheckBox;
	private IOnClickManageHandler onClickManageHandler;	// handler for parent layout notification (when a delete or edit click occures inside this layout)
	private int index;									// the index of this layout within its parrent
	private int errorCounter = 0;						// counter used when an upload error occures (if this is > 0, this layout and the image coresponding to this layout, will be deleted)
	private boolean askForMainImageApproval = true;
	private String imagePath; 							// image path is saved for the case when some error occures and we need to try again the image upload
	
	private LinearLayout elementsLayout;
	private TextView imageSizeTxtView;
	
	private static Object lockMutex = new Object();		// object used to synchronize requests

	public ImagePreviewLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		// only after the inflate is finished we can get references to the ImageView and the delete Button
		imgView = (ImageView) findViewById(R.id.imageViewHolder);
		deleteBtn = (Button) findViewById(R.id.deleteBtn);
		loadingProgressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);
		mainImageCheckBox = (CheckBox) findViewById(R.id.mainImageCheckBox);
		elementsLayout = (LinearLayout) findViewById(R.id.elementsLinearLayout);
		imageSizeTxtView = (TextView) findViewById(R.id.imageSizeTxtView);
		
		init();
	}

	/**
	 * Constructs the containing objects
	 */
	private void init() {

		eventListener = new EventListener(this);

		imgView.setOnClickListener(eventListener);
		deleteBtn.setOnClickListener(eventListener);
		mainImageCheckBox.setOnCheckedChangeListener(eventListener);

		// this is necessary only in the case when setUrl is called from outside and the imgView is null then
		if(url != null){
			setImageFromUrl();
		}
	}

	/**
	 * 
	 * @return the url as <code>String</code>
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * 
	 * @param url of the image to be shown in <code>ImageView</code>
	 */
	public void setUrl(String url) {
		this.url = url;

		if(imgView != null){
			setImageFromUrl();
		}
	}

	/**
	 * 
	 * @param listener <code>OnClickManageHandler</code> used to handle click events outside this layout
	 */
	public void setManageClickListener(IOnClickManageHandler listener){
		onClickManageHandler = listener;
	}

	private void setImageFromUrl(){
		try {
			// set the image from url
			imgView.setImageDrawable(Drawable.createFromStream(new URL(url).openStream(), "src"));
			
			imageSizeTxtView.setText(imgView.getDrawable().getIntrinsicWidth() + " x " + imgView.getDrawable().getIntrinsicHeight() + "px");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @return the image name provided by server
	 */
	public String getImageName() {
		return imageName;
	}

	/**
	 * 
	 * @param imageName which is the name of the image saved on server
	 */
	public void setImageName(String imageName) {
		this.imageName = imageName;
		setUrl(IMAGES_URL + imageName);
	}

	/**
	 * 
	 * @return the index of this view inside its parent
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * 
	 * @param index of this view inside its parent
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * You must call setImagePath before calling this method!
	 * 
	 * @param index is the index of this view inside its parent
	 * 
	 * @see ImagePreviewLayout#setImagePath(String)
	 */
	public void sendImageToServer(int index){
		this.index = index;
		errorCounter = 0;
		new SaveImageOnServerTask().execute(imagePath);
	}
	
	public void updateImageIndex(String productId, int index){
		this.index = index;
		
		new UpdateImageOnServerTask().execute(productId);
	}

	public void setLoading(boolean isLoading){
		if(isLoading){
			// show only the progress bar when loading
			setVisibilityToChilds(GONE);
			loadingProgressBar.setVisibility(VISIBLE);
			return;
		}

		// remove the progress bar and show the image view and the delete button
		setVisibilityToChilds(VISIBLE);
		loadingProgressBar.setVisibility(GONE);
	}

	/**
	 * modifies the visibility to the image view and the delete button inside
	 */
	private void setVisibilityToChilds(int visibility){
		elementsLayout.setVisibility(visibility);
	}

	public String getImagePath() {
		return imagePath;
	}

	/**
	 * Sets the image path/url used to load or send image to server
	 * 
	 * @param imagePath
	 */
	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}
	
	/**
	 * Changes the check state for the checkbox contained by this view. Used for the first image so that when it is loaded from server, make it main.
	 * 
	 * @param checked
	 */
	public void setMainImageCheck(boolean checked){
		askForMainImageApproval = false;
		mainImageCheckBox.setChecked(checked);
	}
	
	public void showCheckBox(boolean show){
		mainImageCheckBox.setVisibility(show ? VISIBLE : GONE);
	}
}
