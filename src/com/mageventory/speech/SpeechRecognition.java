package com.mageventory.speech;

import java.util.Date;
import java.util.HashMap;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;
import android.widget.Toast;
import ca.ilanguage.labs.pocketsphinx.service.RecognitionListenerReduced;
import ca.ilanguage.labs.pocketsphinx.service.RecognizerTask;
import ca.ilanguage.labs.pocketsphinx.ui.PocketSphinxAndroidDemo;

public class SpeechRecognition implements RecognitionListenerReduced
{
	public static final String TAG = "SpeechRecognition";
	
	private RecognizerTask rec;

	/**
	 * Thread in which the recognizer task runs.
	 */
	private Thread rec_thread;

	/**
	 * Progress dialog for final recognition.
	 */
	private ProgressDialog rec_dialog;
	
	private Context mContext;
	private TextToSpeech mTts;
	
	public SpeechRecognition(Context context, TextToSpeech tts)
	{
		mContext = context;
		mTts = tts;
		
		rec = new RecognizerTask();
    	rec_thread = new Thread(this.rec);
    	rec.setRecognitionListener(this);
    	rec_thread.start();
    	
	}
	
	public void startSpeechRecognition()
	{
		rec.start();
	}
	
	public void stopSpeechRecognition()
	{
		rec.stop();
	}
	
	public void finishSpeechRecognition()
	{
		rec.shutdown();
	}

	@Override
	public void onPartialResults(Bundle b) {
		final String hyp = b.getString("hyp");
		stopSpeechRecognition();
	}

	@Override
	public void onResults(Bundle b) {
		final String hyp = b.getString("hyp");
		
		HashMap<String, String> myHashAlarm = new HashMap();
		myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
		        "end of message ID");
		
		mTts.speak(hyp, TextToSpeech.QUEUE_FLUSH, myHashAlarm);
		mTts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
			@Override
			public void onUtteranceCompleted(String utteranceId) {
				Log.d(TAG, "onUtteranceCompleted");
				startSpeechRecognition();
			}
		});
	}

	@Override
	public void onError(int err) {
		// TODO Auto-generated method stub
		
	}
}
