package com.mageventory;

import java.util.HashMap;

import junit.framework.Test;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.model.Product;
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

		((EditText) findViewById(R.id.user_input)).setText(user);
		((EditText) findViewById(R.id.pass_input)).setText(pass);
		((EditText) findViewById(R.id.url_input)).setText(url);

	}

	private OnClickListener buttonlistener = new OnClickListener() {
		public void onClick(View v) {
			if (v.getId() == R.id.savebutton) {

				String user = ((EditText) findViewById(R.id.user_input)).getText().toString();
				String pass = ((EditText) findViewById(R.id.pass_input)).getText().toString();
				String url = ((EditText) findViewById(R.id.url_input)).getText().toString();
				if (!url.startsWith("http://")) {
					url = "http://" + url;
				}
				TestingConecction tc=new TestingConecction();
				tc.execute(new String[]{url,user,pass});
			}
		}
	};

	private class TestingConecction extends AsyncTask<String, Integer, Boolean> {
		ProgressDialog pDialog;

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
				app.setClient(url,user,pass);
				if(app.getClient().isValid()){
				settings.setUrl(url);
				settings.setUser(user);
				settings.setPass(pass);
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
				Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
			}

		}
	}

}
