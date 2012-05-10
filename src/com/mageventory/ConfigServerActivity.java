package com.mageventory;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mageventory.client.MagentoClient;
import com.mageventory.client.MagentoClient2;
import com.mageventory.settings.Settings;

public class ConfigServerActivity extends BaseActivity {
	Settings settings;
	MyApplication app;

	public ConfigServerActivity() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server_config);
		app = (MyApplication) getApplication();
		settings = new Settings(getApplicationContext());
		Button save = (Button) findViewById(R.id.savebutton);
		this.setTitle("Mventory: Configuration");
		restoreFields();
		save.setOnClickListener(buttonlistener);
	}

	private void restoreFields() {
		String user = settings.getUser();
		String pass = settings.getPass();
		String url = settings.getUrl();
		String key = settings.getAPIkey();

		((EditText) findViewById(R.id.user_input)).setText(user);
		((EditText) findViewById(R.id.pass_input)).setText(pass);
		((EditText) findViewById(R.id.url_input)).setText(url);
		((EditText) findViewById(R.id.google_book_api_input)).setText(key);
	}

	private OnClickListener buttonlistener = new OnClickListener() {
		public void onClick(View v) {
			if (v.getId() == R.id.savebutton) {

				String user = ((EditText) findViewById(R.id.user_input)).getText().toString();
				String pass = ((EditText) findViewById(R.id.pass_input)).getText().toString();
				String url = ((EditText) findViewById(R.id.url_input)).getText().toString();
				String apiKey = ((EditText) findViewById(R.id.google_book_api_input)).getText().toString();
				
				if (!url.startsWith("http://")) {
					url = "http://" + url;
				}
				
				if(TextUtils.equals(apiKey, ""))
					Toast.makeText(getApplicationContext(), "No Google Books API -- Book Search Feature Will be Disabled", Toast.LENGTH_LONG).show();
				
				TestingConecction tc=new TestingConecction();
				tc.execute(new String[]{url,user,pass,apiKey});
			}
		}
	};

	private class TestingConecction extends AsyncTask<String, Integer, Boolean> {
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
			
				String url = st[0];
				String user = st[1];
				String pass = st[2];
				String apiKey = st[3];
				app.setClient(url,user,pass);
				client = app.getClient();
				if(client.isValid()){
    				settings.setUrl(url);
    				settings.setUser(user);
    				settings.setPass(pass);
    				settings.setAPIkey(apiKey);
    				/* Check If Customer is Valid*/
    				try
    				{
    					MagentoClient2 client2 = new MagentoClient2(url,user,pass);    					
    					client2.login();
    					boolean isCustomervalid = client2.validateCustomer();   
    					settings.setCustomerValid(isCustomervalid);    					
    				}
    				catch (Exception e) {
						// TODO: handle exception
					}
    				
    				
    				return true;
				}
				return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			pDialog.dismiss();
			if (result) {
				Toast.makeText(getApplicationContext(), "Settings Working and Saved", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(), client.getLastError(), Toast.LENGTH_LONG).show();
			}
		}
	}

}
