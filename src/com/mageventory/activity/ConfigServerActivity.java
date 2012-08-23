package com.mageventory.activity;

import java.io.File;
import java.net.MalformedURLException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.R;
import com.mageventory.R.id;
import com.mageventory.R.layout;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.CustomAttribute;
import com.mageventory.model.CustomAttributesList;
import com.mageventory.settings.Settings;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.CreateOptionTask;
import com.mageventory.util.DefaultOptionsMenuHelper;

public class ConfigServerActivity extends BaseActivity implements MageventoryConstants {
	private Settings settings;
	
	private boolean newProfileMode = false;
	
	private Button save_button;
	private Button delete_button;
	private Button new_button;
	
	private Button clear_cache;
	private Button clear_all_caches;
	
	private Button camera_sync_button;

	public ConfigServerActivity() {
	}

	Spinner profileSpinner;
	TextView notWorkingTextView;
	
	/* Show a confirmation when clicking on one of the buttons for deleting the cache so that the user knows
	 * that the button was clicked. */
	public void showCacheRemovedDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Success");
		alert.setMessage("Cache deleted successfully.");

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		AlertDialog srDialog = alert.create();
		srDialog.show();
	}
	
	public void showRemoveProfileQuestion() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Question");
		
		if (newProfileMode == true)
		{
			alert.setMessage("Do you really want to remove new profile?");
		}
		else
		{
			alert.setMessage("Do you really want to remove that profile?");	
		}

		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (newProfileMode == true)
				{
					newProfileMode = false;
					profileSpinner.setEnabled(true);

					refreshProfileSpinner(false);
				}
				else
				{
					settings.removeStore(settings.getCurrentStoreUrl());
					String [] list = settings.getListOfStores(false);
					
					if (list.length > 0)
					{
						settings.switchToStoreURL(list[0]);
					}
					else
					{
						settings.switchToStoreURL(null);
					}
				
					refreshProfileSpinner(false);
				}
				
				if (settings.getListOfStores(false).length == 0)
				{
					cleanAllFields();
				}
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
	
	public void showRemoveCacheQuestion() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Question");
		alert.setMessage("Do you really want to remove cache for the current profile?");

		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				JobCacheManager.deleteCache(settings.getUrl());
				showCacheRemovedDialog();
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
	
	public void showRemoveAllCachesQuestion() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Question");
		alert.setMessage("Do you really want to remove all caches?");

		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				JobCacheManager.deleteAllCaches();
				showCacheRemovedDialog();
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
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server_config);
		notWorkingTextView = ((TextView)findViewById(R.id.not_working_text_view));
		settings = new Settings(getApplicationContext());
		save_button = (Button) findViewById(R.id.savebutton);
		delete_button = (Button) findViewById(R.id.deletebutton);
		new_button = (Button) findViewById(R.id.newbutton);
		camera_sync_button = (Button) findViewById(R.id.cameraSync);
		
		clear_cache = (Button) findViewById(R.id.clearCacheButton);
		clear_all_caches = (Button) findViewById(R.id.clearAllCachesButton);
		
		clear_cache.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showRemoveCacheQuestion();
			}
		});
		
		clear_all_caches.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showRemoveAllCachesQuestion();
			}
		});
		
		this.setTitle("Mventory: Configuration");
		
		restoreFields();
		save_button.setOnClickListener(saveButtonlistener);
		delete_button.setOnClickListener(deleteButtonlistener);
		new_button.setOnClickListener(newButtonlistener);
		
		camera_sync_button.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent i = new Intent(ConfigServerActivity.this, CameraTimeSyncActivity.class);
				ConfigServerActivity.this.startActivity(i);
			}
		});
		
		EditText googleAPI_key = (EditText) findViewById(R.id.google_book_api_input);
		googleAPI_key.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				Intent scanInt = new Intent("com.google.zxing.client.android.SCAN");
				scanInt.putExtra("SCAN_MODE", "QR_CODE_MODE");
				startActivityForResult(scanInt, SCAN_QR_CODE);
				return true;
			}
		});
		
		profileSpinner = ((Spinner)findViewById(R.id.urls_spinner)); 

		refreshProfileSpinner(false);
		
		profileSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				
				if (position < settings.getStoresCount() && position >= 0)
				{
					String url = (String)profileSpinner.getAdapter().getItem(position);
					settings.switchToStoreURL(url);
					((MyApplication)ConfigServerActivity.this.getApplication()).registerFileObserver(settings.getGalleryPhotosDirectory());
					restoreFields();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		
		/* Click the "new" button automatically when there are no profiles. */
		if (settings.getStoresCount() == 0)
		{
			newButtonlistener.onClick(null);
		}
		
		((EditText) findViewById(R.id.user_input)).addTextChangedListener(textWatcher);
		((EditText) findViewById(R.id.pass_input)).addTextChangedListener(textWatcher);
		((EditText) findViewById(R.id.url_input)).addTextChangedListener(textWatcher);
		((EditText) findViewById(R.id.google_book_api_input)).addTextChangedListener(textWatcher);
		
		((EditText) findViewById(R.id.gallery_photos_directory_input)).addTextChangedListener(textWatcher);
		((EditText) findViewById(R.id.max_image_height_px)).addTextChangedListener(textWatcher);
		((EditText) findViewById(R.id.max_image_width_px)).addTextChangedListener(textWatcher);
		
		cleanAllFields();
	}
		
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK)) {
	    	if (newProfileMode)
	    	{
	    		/* If we are in new profile mode and user presses back then go to normal mode instead
	    		 * of closing the activity. */
	    		deleteButtonlistener.onClick(null);
	    		return true;
	    	}
	    }
	    return super.onKeyDown(keyCode, event);
	}

	private void cleanAllFields()
	{
		((EditText) findViewById(R.id.user_input)).setText("");
		((EditText) findViewById(R.id.pass_input)).setText("");
		((EditText) findViewById(R.id.url_input)).setText("");
		((EditText) findViewById(R.id.google_book_api_input)).setText("");
		
		if (TextUtils.isEmpty(settings.getGalleryPhotosDirectory()))
		{
			((EditText) findViewById(R.id.gallery_photos_directory_input)).setText(Environment.getExternalStorageDirectory().getAbsolutePath());
		}
		else
		{
			((EditText) findViewById(R.id.gallery_photos_directory_input)).setText(settings.getGalleryPhotosDirectory());
		}
		
		((EditText) findViewById(R.id.max_image_height_px)).setText("");
		((EditText) findViewById(R.id.max_image_width_px)).setText("");
		
		notWorkingTextView.setVisibility(View.GONE);
		save_button.setEnabled(false);
	}
	
	private void refreshProfileSpinner(boolean withNewOption)
	{
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
			this, R.layout.config_spinner_dropdown, settings.getListOfStores(withNewOption));
		
		profileSpinner.setAdapter(arrayAdapter);
		
		if (withNewOption == true)
		{
			profileSpinner.setSelection(arrayAdapter.getCount()-1);	
		}
		else
		{
			profileSpinner.setSelection(settings.getCurrentStoreIndex());
		}
		
		save_button.setEnabled(false);
		
		if (withNewOption)
		{
			new_button.setEnabled(false);
			delete_button.setEnabled(true);
			clear_cache.setEnabled(false);
		}
		else
		{
			new_button.setEnabled(true);
			
			if (settings.getStoresCount() > 0)
			{
				delete_button.setEnabled(true);
				clear_cache.setEnabled(true);
			}
			else
			{
				delete_button.setEnabled(false);
				clear_cache.setEnabled(false);
			}
		}
		
		if (withNewOption == false && settings.getStoresCount() == 0)
		{
			((EditText) findViewById(R.id.user_input)).setEnabled(false);
			((EditText) findViewById(R.id.pass_input)).setEnabled(false);
			((EditText) findViewById(R.id.url_input)).setEnabled(false);
			((EditText) findViewById(R.id.google_book_api_input)).setEnabled(false);
			((EditText) findViewById(R.id.gallery_photos_directory_input)).setEnabled(false);
			((EditText) findViewById(R.id.max_image_width_px)).setEnabled(false);
			((EditText) findViewById(R.id.max_image_height_px)).setEnabled(false);
		}
		else
		{
			((EditText) findViewById(R.id.user_input)).setEnabled(true);
			((EditText) findViewById(R.id.pass_input)).setEnabled(true);
			((EditText) findViewById(R.id.url_input)).setEnabled(true);
			((EditText) findViewById(R.id.google_book_api_input)).setEnabled(true);
			((EditText) findViewById(R.id.gallery_photos_directory_input)).setEnabled(true);
			((EditText) findViewById(R.id.max_image_width_px)).setEnabled(true);
			((EditText) findViewById(R.id.max_image_height_px)).setEnabled(true);
		}
	}
	
	private void restoreFields() {
		String user = settings.getUser();
		String pass = settings.getPass();
		String url = settings.getUrl();
		String key = settings.getAPIkey();
		String galleryPath = settings.getGalleryPhotosDirectory();
		String maxImageWidth = settings.getMaxImageWidth();
		String maxImageHeight = settings.getMaxImageHeight();		

		((EditText) findViewById(R.id.user_input)).setText(user);
		((EditText) findViewById(R.id.pass_input)).setText(pass);
		((EditText) findViewById(R.id.url_input)).setText(url);
		((EditText) findViewById(R.id.google_book_api_input)).setText(key);
		((EditText) findViewById(R.id.gallery_photos_directory_input)).setText(galleryPath);
		((EditText) findViewById(R.id.max_image_width_px)).setText(maxImageWidth);
		((EditText) findViewById(R.id.max_image_height_px)).setText(maxImageHeight);
		
		if (newProfileMode == true || settings.getProfileDataValid() == true || settings.getStoresCount() == 0)
		{
			notWorkingTextView.setVisibility(View.GONE);			
		}
		else
		{
			notWorkingTextView.setVisibility(View.VISIBLE);
		}
		save_button.setEnabled(false);
	}

	private OnClickListener saveButtonlistener = new OnClickListener() {
		public void onClick(View v) {
			String user = ((EditText) findViewById(R.id.user_input)).getText().toString();
			String pass = ((EditText) findViewById(R.id.pass_input)).getText().toString();
			String url = ((EditText) findViewById(R.id.url_input)).getText().toString();
			String apiKey = ((EditText) findViewById(R.id.google_book_api_input)).getText().toString();
			String galleryPath = ((EditText) findViewById(R.id.gallery_photos_directory_input)).getText().toString();
			String maxImageWidth = ((EditText) findViewById(R.id.max_image_width_px)).getText().toString();
			String maxImageHeight = ((EditText) findViewById(R.id.max_image_height_px)).getText().toString();	
			
			if (!galleryPath.startsWith("/"))
			{
				galleryPath = "/" + galleryPath;
			}
			
			if (!new File(galleryPath).exists())
			{
				Toast.makeText(getApplicationContext(),
						"Gallery photos directory does not exist.", Toast.LENGTH_LONG).show();
				return;
			}
			else
			{
				((EditText) findViewById(R.id.gallery_photos_directory_input)).setText(galleryPath);
			}
			
			if (user.length() == 0)
			{
				Toast.makeText(getApplicationContext(),
						"Please provide user name.", Toast.LENGTH_LONG).show();
				return;
			}
			
			if (pass.length() == 0)
			{
				Toast.makeText(getApplicationContext(),
						"Please provide password.", Toast.LENGTH_LONG).show();
				return;
			}
			
			if (url.length() == 0)
			{
				Toast.makeText(getApplicationContext(),
						"Please provide store url.", Toast.LENGTH_LONG).show();
				return;
			}
			
			if (TextUtils.equals(apiKey, ""))
				Toast.makeText(getApplicationContext(),
						"No Google Books API -- Book Search Feature Will be Disabled", Toast.LENGTH_LONG).show();
			
			if (!url.startsWith("http://")) {
				url = "http://" + url;
			}
			
			
			if (newProfileMode == true)
			{
				settings.addStore(url);
				settings.switchToStoreURL(url);
				
				profileSpinner.setEnabled(true);
				
				newProfileMode = false;
			}
			
			settings.setUrl(url);
			settings.setUser(user);
			settings.setPass(pass);
			settings.setAPIkey(apiKey);
			settings.setGalleryPhotosDirectory(galleryPath);
			settings.setMaxImageHeight(maxImageHeight);
			settings.setMaxImageWidth(maxImageWidth);
			
			((MyApplication)ConfigServerActivity.this.getApplication()).registerFileObserver(galleryPath);
		
			TestingConnection tc = new TestingConnection();
			tc.execute(new String[] {});
		}
	};
	
	private OnClickListener deleteButtonlistener = new OnClickListener() {
		public void onClick(View v) {
			showRemoveProfileQuestion();
		}
	};
	
	private OnClickListener newButtonlistener = new OnClickListener() {
		public void onClick(View v) {
			newProfileMode = true;
			
			refreshProfileSpinner(true);
			
			profileSpinner.setEnabled(false);
			
			cleanAllFields();
		}
	};
	
	private TextWatcher textWatcher = new TextWatcher() {
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			save_button.setEnabled(true);				
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			// TODO Auto-generated method stub
			
		}			
		@Override
		public void afterTextChanged(Editable s) {
			// TODO Auto-generated method stub
			
		}
	};

	private class TestingConnection extends AsyncTask<String, Integer, Boolean> {
		ProgressDialog pDialog;
		private MagentoClient client;

		@Override
		protected void onPreExecute() {
			pDialog = new ProgressDialog(ConfigServerActivity.this);
			pDialog.setMessage("Testing Settings");
			pDialog.setIndeterminate(true);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		@Override
		protected Boolean doInBackground(String... st) {

			try {
				client = new MagentoClient(new SettingsSnapshot(ConfigServerActivity.this));
			} catch (MalformedURLException e) {
				settings.setProfileDataValid(false);
				return false;
			}
			client.login();
				
			if (client.isLoggedIn())
			{
				settings.setProfileDataValid(true);
			}
			else
			{
				settings.setProfileDataValid(false);
				return false;
			}				
				
			boolean isCustomervalid = client.validateCustomer();
			settings.setCustomerValid(isCustomervalid);

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			pDialog.dismiss();
			if (result) {
				Toast.makeText(getApplicationContext(), "Settings working.", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(), client.getLastErrorMessage(), Toast.LENGTH_LONG).show();
			}
			
			if (settings.getProfileDataValid() == true)
			{
				notWorkingTextView.setVisibility(View.GONE);
			}
			else
			{
				notWorkingTextView.setVisibility(View.VISIBLE);
			}
			refreshProfileSpinner(false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mageventory.BaseActivity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == SCAN_QR_CODE) {
			if (resultCode == RESULT_OK) {
				String contents = data.getStringExtra("SCAN_RESULT");
				((EditText) findViewById(R.id.google_book_api_input)).setText(contents);
			}
		}
	}
}
