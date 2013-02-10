package com.mageventory.activity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.mageventory.MageventoryConstants;
import com.mageventory.R;
import com.mageventory.R.id;
import com.mageventory.R.layout;
import com.mageventory.activity.base.BaseActivity;
import com.mageventory.settings.Settings;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CameraTimeSyncActivity extends BaseActivity implements MageventoryConstants {

	
	private Runnable mTimeUpdater = new Runnable() {
		
		@Override
		public void run() {
			if (mActivityAlive)
			{
				long milis = System.currentTimeMillis();
				Time time = new Time();
				time.set(milis);
				
				int hour = time.hour;
				int minute = time.minute;
				int second = time.second;
				
				String hourString = "" + hour;
				String minuteString = "" + minute;
				String secondString = "" + second;

				if (hourString.length()<2)
					hourString = "0" + hourString;
				if (minuteString.length()<2)
					minuteString = "0" + minuteString;
				if (secondString.length()<2)
					secondString = "0" + secondString;
				
				mTimeView.setText(hourString + ":" + minuteString + ":" + secondString);
				
				Handler h = new Handler (Looper.getMainLooper());
				h.postDelayed(mTimeUpdater, 500);
			}
		}
	};
	
	private boolean mActivityAlive;
	private TextView mTimeView;
	private TextView mTimeDifferenceView;
	private EditText mPhoneTime;
	private EditText mCameraTime;
	private Settings mSettings;
	
	private void setTimeDifferenceText()
	{
		mTimeDifferenceView.setText("Time difference (phone - camera): " + mSettings.getCameraTimeDifference() + "s");
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_sync);
	
		mSettings = new Settings(this);
		
		mTimeView = (TextView) findViewById(R.id.time_view);
		mTimeDifferenceView =  (TextView) findViewById(R.id.time_diff);

		setTimeDifferenceText();
		
		mPhoneTime = (EditText) findViewById(R.id.phone_time_input);
		mCameraTime = (EditText) findViewById(R.id.camera_time_input);
		
		mActivityAlive = true;
		
		runOnUiThread(mTimeUpdater);
		
		Button syncButton = (Button) findViewById(R.id.sync);
		
		syncButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String phoneTime = mPhoneTime.getText().toString();
				String cameraTime = mCameraTime.getText().toString();
				
				/* If there are no colons then it's not going to be parsed correctly anyway. Try adding them. */
				if (phoneTime.contains(":") == false && phoneTime.length()==6)
				{
					phoneTime = phoneTime.charAt(0) + phoneTime.charAt(1) + ":" +
								phoneTime.charAt(2) + phoneTime.charAt(3) + ":" +
								phoneTime.charAt(4) + phoneTime.charAt(5);
				}
				
				if (cameraTime.contains(":") == false && cameraTime.length()==6)
				{
					cameraTime = cameraTime.charAt(0) + cameraTime.charAt(1) + ":" +
								cameraTime.charAt(2) + cameraTime.charAt(3) + ":" +
								cameraTime.charAt(4) + cameraTime.charAt(5);
				}
				
				SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
				
				try {
					Date phoneDate = timeFormat.parse(phoneTime);
					Date cameraDate = timeFormat.parse(cameraTime);
					
					int timeDifference = (int)( (phoneDate.getTime() - cameraDate.getTime()) /1000 );
					
					mSettings.setCameraTimeDifference(timeDifference);
					setTimeDifferenceText();

					Toast.makeText(CameraTimeSyncActivity.this, "Time difference set with success.", Toast.LENGTH_LONG).show();
					CameraTimeSyncActivity.this.hideKeyboard();
				} catch (ParseException e) {
					Toast.makeText(CameraTimeSyncActivity.this, "The format of the time provided is incorrect.", Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mActivityAlive = false;
	}

}

