package com.zetaprints.magventory;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.zetaprints.magventory.settings.Settings;

public class ConfigServerActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server_config);
		Button save = (Button) findViewById(R.id.savebutton);
		save.setOnClickListener(buttonlistener);
		restoreFields();
	}

	private void restoreFields() {
		Settings settings = new Settings(getApplicationContext());
		
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

				Settings settings = new Settings(getApplicationContext());
				
				settings.setUser(user);
				settings.setPass(pass);
				settings.setUrl(url);
				
				Intent intent = new Intent();
				setResult(RESULT_OK, intent);
				finish();
			}
		}
	};

}
