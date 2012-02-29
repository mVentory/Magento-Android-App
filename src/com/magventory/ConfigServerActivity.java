package com.magventory;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.magventory.settings.Settings;
import com.magventory.R;

public class ConfigServerActivity extends Activity{
	Settings settings;
	public  ConfigServerActivity() {}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server_config);
		Settings settings = new Settings(getApplicationContext());
		Button save = (Button) findViewById(R.id.savebutton);
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

				String user = ((EditText)findViewById(R.id.user_input)).getText().toString();
				String pass = ((EditText)findViewById(R.id.pass_input)).getText().toString();
				String url = ((EditText)findViewById(R.id.url_input)).getText().toString();
				
				settings.setUser(user);
				settings.setPass(pass);
				settings.setUrl(url);
				Toast.makeText(getApplicationContext(), "Settings Saved", Toast.LENGTH_SHORT).show();
				/*Intent intent = new Intent();
				setResult(RESULT_OK, intent);
				finish();*/
			}
		}
	};

}
