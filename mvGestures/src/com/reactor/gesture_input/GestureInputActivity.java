package com.reactor.gesture_input;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class GestureInputActivity extends Activity implements OnInitListener, OnGesturePerformedListener {

	private static final String BACKSPACE_GESTURE_NAME = "back";
	private static final String OUTPUT_TEXT_KEY = "OUTPUT_TEXT_KEY";
	private static final String PARAM_INPUT_TYPE = "PARAM_INPUT_TYPE";
	private static final String PARAM_INITIAL_TEXT = "PARAM_INITIAL_TEXT";
	
	private TextToSpeech mTalker;
	private GestureLibrary mGestureLib;
	
	private EditText mEditText;
	private boolean mSpeechInitialized;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		GestureOverlayView gestureOverlayView = new GestureOverlayView(this);
		View inflate = getLayoutInflater().inflate(R.layout.activity_main, null);
		gestureOverlayView.addView(inflate);
		gestureOverlayView.setFadeOffset(0);
		gestureOverlayView.setFadeEnabled(false);
		gestureOverlayView.setOrientation(GestureOverlayView.ORIENTATION_VERTICAL);
		gestureOverlayView.setGestureStrokeAngleThreshold(90);
		
		gestureOverlayView.addOnGesturePerformedListener(this);

		gestureOverlayView.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					InputMethodManager imm = (InputMethodManager)getSystemService(
						Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
				}
				
				return false;
			}
		});
		
		setContentView(gestureOverlayView);
		Button closeButton = (Button)findViewById(R.id.close);
		Button configButton = (Button)findViewById(R.id.config);
		
		mEditText = (EditText)findViewById(R.id.editText);
		Bundle extras = getIntent().getExtras();
		
		if (extras != null)
		{
			mEditText.setInputType(extras.getInt(PARAM_INPUT_TYPE));
			String initialText = extras.getString(PARAM_INITIAL_TEXT);
			
			if (initialText != null)
			{
				mEditText.setText(initialText);
				mEditText.setSelection(mEditText.getText().length());
			}
		}
		
		closeButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				InputMethodManager imm = (InputMethodManager)getSystemService(
						Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
				
				Intent in = new Intent( );
				in.putExtra(OUTPUT_TEXT_KEY, mEditText.getText().toString());
				setResult(RESULT_OK, in);
				finish();
			}
		});
		
		configButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(GestureInputActivity.this, GestureBuilderActivity.class);
				startActivity(intent);
			}
		});
		
		mTalker = new TextToSpeech(this, this);
	}

	@Override
	public void onBackPressed() {
		Intent in = new Intent( );
		in.putExtra(OUTPUT_TEXT_KEY, mEditText.getText().toString());
		setResult(RESULT_OK, in);
		finish();
		
		super.onBackPressed();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mGestureLib = GestureLibraries.fromPrivateFile(this, GestureBuilderActivity.sStoreFile);
		mGestureLib.load();
	}
	
	@Override
	protected void onDestroy() {
		mTalker.shutdown();
		super.onDestroy();
	}

	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS)
		{
			mSpeechInitialized = true;
		}
	}
	
	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		
		ArrayList<Prediction> predictions = mGestureLib.recognize(gesture);
		
		double maxScore = -1;
		String bestName = "";
		
		for (Prediction prediction : predictions) {
			if (maxScore < prediction.score)
			{
				maxScore = prediction.score;
				bestName = prediction.name;
			}
		}
		
		if (bestName.equalsIgnoreCase(BACKSPACE_GESTURE_NAME))
		{
			mEditText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
		}
		else
		{
			mEditText.setText(mEditText.getText().toString() + bestName);
			mEditText.setSelection(mEditText.getText().length());
		}
		
		if (mSpeechInitialized)
		{
			mTalker.speak(bestName, TextToSpeech.QUEUE_ADD, null);
		}
	}

}
