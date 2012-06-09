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
import android.widget.TextView;
import ca.ilanguage.labs.pocketsphinx.service.RecognitionListenerReduced;
import ca.ilanguage.labs.pocketsphinx.service.RecognizerTask;
import ca.ilanguage.labs.pocketsphinx.util.ConvertWordToNumber;

public class SpeechRecognition implements RecognitionListenerReduced {
	private static RecognizerTask rec;

	/**
	 * Thread in which the recognizer task runs.
	 */
	private static Thread rec_thread;

	/*
	 * Launch recognizer task just once for the entire lifetime of the
	 * application.
	 */
	static {
		System.loadLibrary("pocketsphinx_jni");

		rec = new RecognizerTask();
		rec_thread = new Thread(rec);
		rec_thread.start();
	}

	/* Set a listener which gets triggered when something gets recognized. */
	private static void setRecognitionListener(RecognitionListenerReduced rl) {
		rec.setRecognitionListener(rl);
	}

	/*
	 * The listener that code using this class can register to find out when
	 * user pressed "Done" button.
	 */
	public static interface OnRecognitionFinishedListener {
		void onRecognitionFinished(String output);
	}

	public static final String TAG = "SpeechRecognition";

	private Activity mActivity;
	private TextToSpeech mTts;
	private EditText mEditText;
	private TextView mTextView;
	private OnRecognitionFinishedListener mOnRecognitionFinished;
	private String mInitialText;

	private static final int IO_BUFFER_SIZE = 4 * 1024;

	/* Copy input stream to output stream. */
	private static void saveInputStream(InputStream in, OutputStream out)
			throws IOException {
		byte[] b = new byte[IO_BUFFER_SIZE];
		int read;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
		}
	}

	/*
	 * Extract given model file from resources and put on the sdcard in case
	 * it's not already there.
	 */
	private void extractModelFile(int res, File dir, String fileName) {
		InputStream in = mActivity.getResources().openRawResource(res);
		OutputStream out = null;

		if (!dir.exists()) {
			dir.mkdirs();
		}

		File file = new File(dir, fileName);

		if (!file.exists()) {
			try {
				out = new FileOutputStream(file);
				saveInputStream(in, out);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
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

	/* Extract all model files needed by the application to recognize speech. */
	private void extractModelFiles() {
		File dirHmm = new File(Environment.getExternalStorageDirectory(),
				MyApplication.APP_DIR_NAME);
		dirHmm = new File(dirHmm, "PocketSphinxData");
		dirHmm = new File(dirHmm, "hmm");
		dirHmm = new File(dirHmm, "tidigits");

		File dirLm = new File(Environment.getExternalStorageDirectory(),
				MyApplication.APP_DIR_NAME);
		dirLm = new File(dirLm, "PocketSphinxData");
		dirLm = new File(dirLm, "lm");

		extractModelFile(R.raw.tidigitsdic, dirLm, "tidigits.dic");
		extractModelFile(R.raw.tidigits, dirLm, "tidigits.DMP");

		extractModelFile(R.raw.feat, dirHmm, "feat.params");
		extractModelFile(R.raw.mdef, dirHmm, "mdef");
		extractModelFile(R.raw.means, dirHmm, "means");
		extractModelFile(R.raw.sendump, dirHmm, "sendump");
		extractModelFile(R.raw.transition_matrices, dirHmm,
				"transition_matrices");
		extractModelFile(R.raw.variances, dirHmm, "variances");
	}

	/* Show a dialog allowing the user to enter digits using speech */
	public void showDialog() {
		final View textEntryView = mActivity.getLayoutInflater().inflate(
				R.layout.speech_recognition_dialog, null);

		AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);

		alert.setTitle("Listening...");
		alert.setMessage("Use your voice to enter digits into the edit box.");
		// Set an EditText view to get user input
		alert.setView(textEntryView);

		mEditText = (EditText) textEntryView.findViewById(R.id.speechEdit);
		mEditText.setText(mInitialText);

		mTextView = (TextView) textEntryView.findViewById(R.id.speechText);

		alert.setNegativeButton("Done", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finishSpeechRecognition();
				rec.stop();
				mOnRecognitionFinished.onRecognitionFinished(mEditText
						.getText().toString());
			}
		});

		AlertDialog srDialog = alert.create();
		alert.show();
	}

	/* Launch text to speech and speech recognition. */
	public SpeechRecognition(Activity activity,
			OnRecognitionFinishedListener callback, String initialText) {
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

	/*
	 * Start speech recognition and register a listener so we can get informed
	 * about recognition results.
	 */
	private void startSpeechRecognition() {
		showDialog();
		rec.start();
		mTextView.setText("Speak now.");
		setRecognitionListener(this);
	}

	/* Pause recognition task. */
	private void pauseSpeechRecognition() {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mTextView.setText("");
			}
		});
		rec.stop();
	}

	/* Resume speech recognition task. */
	public void resumeSpeechRecognition() {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mTextView.setText("Speak now.");
			}
		});
		rec.start();
	}

	/* Finish everything we need to finish and deregister recognition callback. */
	public void finishSpeechRecognition() {
		mTts.shutdown();
		setRecognitionListener(null);
	}

	/*
	 * This function gets called when the recognizer thinks it recognized
	 * something.
	 */
	@Override
	public void onPartialResults(Bundle b) {
		// final String hyp = b.getString("hyp");
		pauseSpeechRecognition();
	}

	/*
	 * This function gets called when we stop recognizer task. It provides the
	 * final recognition result. We are launching text to speech here to read
	 * the digit back to the user as well as resuming the recognition task.
	 */
	@Override
	public void onResults(Bundle b) {
		final String hyp = b.getString("hyp");

		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {

				long parsed = -1;
				if (!hyp.equals("OH")) {
					try {
						parsed = ConvertWordToNumber.parse(hyp);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					parsed = 0;
				}

				if (parsed != -1) {
					mEditText.setText(mEditText.getText().toString() + parsed);
					mEditText.setSelection(mEditText.length());

					HashMap<String, String> myHashAlarm = new HashMap<String, String>();
					myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
							"end of message ID");

					mTts.speak("" + parsed, TextToSpeech.QUEUE_FLUSH,
							myHashAlarm);
					mTts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
						@Override
						public void onUtteranceCompleted(String utteranceId) {
							resumeSpeechRecognition();
						}
					});
				} else {
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
