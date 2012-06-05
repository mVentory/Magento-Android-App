package com.mageventory.speech;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import com.mageventory.MyApplication;
import com.mageventory.ProductCreateActivity;
import com.mageventory.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import ca.ilanguage.labs.pocketsphinx.service.RecognitionListenerReduced;
import ca.ilanguage.labs.pocketsphinx.service.RecognizerTask;
import ca.ilanguage.labs.pocketsphinx.util.ConvertWordToNumber;

public class SpeechRecognition implements RecognitionListenerReduced
{
	private static RecognizerTask rec;

	/**
	 * Thread in which the recognizer task runs.
	 */
	private static Thread rec_thread;
	
	static {
		System.loadLibrary("pocketsphinx_jni");
		
		rec = new RecognizerTask();
    	rec_thread = new Thread(rec);
    	rec_thread.start();
	}
	
	private static void setRecognitionListener(RecognitionListenerReduced rl)
	{
		rec.setRecognitionListener(rl);
	}
	
	public static interface OnRecognitionFinishedListener
	{
		void onRecognitionFinished(String output);
	}
	
	public static final String TAG = "SpeechRecognition";

	private Activity mActivity;
	private TextToSpeech mTts;
	private EditText mEditText;
	private OnRecognitionFinishedListener mOnRecognitionFinished;
	private String mInitialText; 
	
	private static final int IO_BUFFER_SIZE = 4 * 1024;  
	
	private static void saveInputStream(InputStream in, OutputStream out) throws IOException
	{  
		byte[] b = new byte[IO_BUFFER_SIZE];  
		int read;  
		while ((read = in.read(b)) != -1) {  
			out.write(b, 0, read);  
		}  
	}
	
	private void extractModelFile(int res, File dir, String fileName)
	{
		InputStream in = mActivity.getResources().openRawResource(res);
		OutputStream out = null;
		
		if (!dir.exists())
		{
			dir.mkdirs();
		}
		
		File file = new File(dir, fileName);
		
		if (!file.exists())
		{
			try {
				out = new FileOutputStream(file);
				saveInputStream(in, out);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			finally
			{
				try {
					in.close();
					if (out != null)
						out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void extractModelFiles()
	{
		File dirHmm = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		dirHmm = new File(dirHmm, "PocketSphinxData");
		dirHmm = new File(dirHmm, "hmm");
		dirHmm = new File(dirHmm, "tidigits");
		
		File dirLm = new File(Environment.getExternalStorageDirectory(), MyApplication.APP_DIR_NAME);
		dirLm = new File(dirLm, "PocketSphinxData");
		dirLm = new File(dirLm, "lm");
		
		extractModelFile(R.raw.tidigitsdic, dirLm, "tidigits.dic");
		extractModelFile(R.raw.tidigits, dirLm, "tidigits.DMP");
		
		extractModelFile(R.raw.feat, dirHmm, "feat.params");
		extractModelFile(R.raw.mdef, dirHmm, "mdef");
		extractModelFile(R.raw.means, dirHmm, "means");
		extractModelFile(R.raw.sendump, dirHmm, "sendump");
		extractModelFile(R.raw.transition_matrices, dirHmm, "transition_matrices");
		extractModelFile(R.raw.variances, dirHmm, "variances");
	}
	
	public void showDialog()
	{
		final View textEntryView = mActivity.getLayoutInflater().inflate(R.layout.speech_recognition_dialog, null);
		
        AlertDialog.Builder alert = new AlertDialog.Builder(mActivity); 

        alert.setTitle("Listening..."); 
        alert.setMessage("Use your voice to enter digits into the edit box."); 
        // Set an EditText view to get user input  
        alert.setView(textEntryView);
        
        mEditText = (EditText)textEntryView.findViewById(R.id.speechEdit);
        mEditText.setText(mInitialText);
        
        alert.setNegativeButton("Done", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finishSpeechRecognition();
				mOnRecognitionFinished.onRecognitionFinished(mEditText.getText().toString());
			}
		});
        
        AlertDialog srDialog = alert.create();
        alert.show(); 
	}
	
	public SpeechRecognition(Activity activity, OnRecognitionFinishedListener callback, String initialText)
	{
		mInitialText = initialText;
		mOnRecognitionFinished = callback;
		
		mTts = new TextToSpeech(activity, new OnInitListener() {
			@Override
			public void onInit(int status) {
				/* TODO: ignoring the status */
				startSpeechRecognition();
			}
		});
		
		mActivity = activity;
		extractModelFiles();
	}
	
	private void startSpeechRecognition()
	{
		showDialog();
		rec.start();
		setRecognitionListener(this);
	}
	
	private void pauseSpeechRecognition()
	{
		rec.stop();
	}
	
	public void resumeSpeechRecognition()
	{
		rec.start();
	}
	
	public void finishSpeechRecognition()
	{
		mTts.shutdown();
		setRecognitionListener(null);
	}

	@Override
	public void onPartialResults(Bundle b) {
		//final String hyp = b.getString("hyp");
		pauseSpeechRecognition();
	}

	@Override
	public void onResults(Bundle b) {
		final String hyp = b.getString("hyp");
		
		mActivity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				
				long parsed = -1;
				if (!hyp.equals("OH"))
				{
					try {
						parsed = ConvertWordToNumber.parse(hyp);
					} catch (Exception e) {
					// 	TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else
				{
					parsed = 0;
				}
					
				if (parsed != -1)
				{
					mEditText.setText(mEditText.getText().toString() + parsed);
					
					HashMap<String, String> myHashAlarm = new HashMap<String, String>();
					myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
					        "end of message ID");
					
					mTts.speak(hyp, TextToSpeech.QUEUE_FLUSH, myHashAlarm);
					mTts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
						@Override
						public void onUtteranceCompleted(String utteranceId) {
							resumeSpeechRecognition();
						}
					});
				}
				else
				{
					resumeSpeechRecognition();
				}
			}
		});
	}

	@Override
	public void onError(int err) {
		/* TODO: Handle this? */
	}
}
