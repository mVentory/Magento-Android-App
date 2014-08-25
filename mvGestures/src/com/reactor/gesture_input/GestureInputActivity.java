/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/
package com.reactor.gesture_input;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

public class GestureInputActivity extends Activity implements OnInitListener, OnGesturePerformedListener {

    public static final String BACKSPACE_GESTURE_NAME = "back";
    private static final String SPACE_GESTURE_NAME = "space";
    private static final String DOT_GESTURE_NAME = "dot";
    private static final String HYPHEN_GESTURE_NAME = "hyphen";
	private static final String OUTPUT_TEXT_KEY = "OUTPUT_TEXT_KEY";
	public static final String PARAM_INPUT_TYPE = "PARAM_INPUT_TYPE";
	public static final String PARAM_INITIAL_TEXT = "PARAM_INITIAL_TEXT";
    private static final int GESTURE_BUILDER_REQUEST_CODE = 0;
	
	private TextToSpeech mTalker;
	private GestureLibrary mGestureLib;
	
	private EditText mEditText;
	private boolean mSpeechInitialized;
	
    private GesturesLoadTask mTask;
    private GesturesAdapter mAdapter;

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
                startActivityForResult(intent, GESTURE_BUILDER_REQUEST_CODE);
			}
		});
		
		mTalker = new TextToSpeech(this, this);
		
        reloadGesturesInformation();
	}

    /**
     * Reload gesture lib and hint overlay view
     */
    public void reloadGesturesInformation() {
        mGestureLib = GestureLibraries.fromPrivateFile(this, GestureBuilderActivity.sStoreFile);
        mGestureLib.load();

		mAdapter = new GesturesAdapter(this);
		GridView gesturesGrid = (GridView) findViewById(R.id.gestures_grid);
		gesturesGrid.setAdapter(mAdapter);
        loadGesturesHintView();
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
	}
	
	@Override
	protected void onDestroy() {
		mTalker.shutdown();
        if (mTask != null && mTask.getStatus() != GesturesLoadTask.Status.FINISHED) {
            mTask.cancel(true);
            mTask = null;
        }
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
            if (bestName.equalsIgnoreCase(SPACE_GESTURE_NAME))
            {
                mEditText.setText(mEditText.getText().toString() + " ");
            } else
            {
                mEditText.setText(mEditText.getText().toString() + bestName);
            }
			mEditText.setSelection(mEditText.getText().length());
		}
		
		if (mSpeechInitialized)
		{
            if (bestName.equals(" "))
            {
                bestName = SPACE_GESTURE_NAME;
            }
            if (bestName.equals("-"))
            {
                bestName = HYPHEN_GESTURE_NAME;
            }
            if (bestName.equals("."))
            {
                bestName = DOT_GESTURE_NAME;
            }
			mTalker.speak(bestName, TextToSpeech.QUEUE_ADD, null);
		}
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GESTURE_BUILDER_REQUEST_CODE)
        {
            reloadGesturesInformation();
        }
    
    }

    private void loadGesturesHintView() {
        if (mTask != null && mTask.getStatus() != GesturesLoadTask.Status.FINISHED) {
            mTask.cancel(true);
        }
        mTask = (GesturesLoadTask) new GesturesLoadTask().execute();
    }

    private class GesturesLoadTask extends GestureUtils.AbstractGesturesLoadTask {

        @Override
        void initParams() {
            final Resources resources = getResources();
            initParams(
                    (int) resources.getDimension(R.dimen.gesture_hint_thumbnail_size),
                    (int) resources.getDimension(R.dimen.gesture_hint_thumbnail_inset),
                    resources.getColor(R.color.gesture_hint_gesture_color),
                    resources.getColor(R.color.gesture_hint_gesture_start_color)
                    );
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mAdapter.setNotifyOnChange(false);
            mAdapter.clear();
        }

        @Override
        void addBitmap(Long id, Bitmap bitmap) {
            mAdapter.addBitmap(id, bitmap);
        }

        @Override
        protected void onProgressUpdate(GestureUtils.NamedGesture... values) {
            super.onProgressUpdate(values);

            final GesturesAdapter adapter = mAdapter;
            adapter.setNotifyOnChange(false);

            for (GestureUtils.NamedGesture gesture : values) {
                adapter.add(gesture);
            }

            adapter.sort(GestureUtils.defaultNamedGestureSorter);
            adapter.notifyDataSetChanged();
        }

        @Override
        GestureLibrary getGestureLibrary() {
            return mGestureLib;
        }
    }

    private class GesturesAdapter extends GestureUtils.AbstractGesturesAdapter {

		public GesturesAdapter(Context context) {
            super(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
                convertView = mInflater.inflate(R.layout.gestures_grid_item, parent, false);
			}

			final GestureUtils.NamedGesture gesture = getItem(position);
            final TextView label = (TextView) convertView.findViewById(R.id.text1);

			label.setTag(gesture);
			label.setText(gesture.name);
			label.setCompoundDrawablesWithIntrinsicBounds(mThumbnails.get(gesture.gesture.getID()), null, null, null);

			return convertView;
		}
	}
}
