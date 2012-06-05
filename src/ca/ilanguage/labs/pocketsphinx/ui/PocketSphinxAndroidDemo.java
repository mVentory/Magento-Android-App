package ca.ilanguage.labs.pocketsphinx.ui;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;

import com.mageventory.MyApplication;
import com.mageventory.R;

import ca.ilanguage.labs.pocketsphinx.service.RecognitionListenerReduced;
import ca.ilanguage.labs.pocketsphinx.service.RecognizerTask;
import ca.ilanguage.labs.pocketsphinx.util.ConvertWordToNumber;
import ca.ilanguage.labs.pocketsphinx.util.SegmentNumber;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.*;
import android.graphics.Color;


public class PocketSphinxAndroidDemo extends Activity implements OnTouchListener, RecognitionListenerReduced {
	static {
		System.loadLibrary("pocketsphinx_jni");
	}

	/**
	 * Recognizer task, which runs in a worker thread.
	 */
	RecognizerTask rec;

	/**
	 * Thread in which the recognizer task runs.
	 */
	Thread rec_thread;
	/**
	 * Time at which current recognition started.
	 */
	Date start_date;
	/**
	 * Number of seconds of speech.
	 */
	float speech_dur;
	/**
	 * Are we listening?
	 */
	boolean listening;
	/**
	 * Progress dialog for final recognition.
	 */
	ProgressDialog rec_dialog;

	final static int ACTIVITY_CREATE = 1;

	EditText digit;
	EditText activeField;
	String activeFieldStr = "";
	
	private ProgressDialog pd;

	
	/**
	 * Respond to touch events on the Speak button.
	 * 
	 * This allows the Speak button to function as a "push and hold" button, by
	 * triggering the start of recognition when it is first pushed, and the end
	 * of recognition when it is released.
	 * 
	 * @param v
	 *            View on which this event is called
	 * @param event
	 *            Event that was triggered.
	 */
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			this.activeField.setText("");
			
			start_date = new Date();
			this.listening = true;
			/*
			 * Starts the RecognizerTask's thread, which will start listening and analyzing the 
			 * audio
			 */
			this.rec.start();
			break;
		case MotionEvent.ACTION_UP:
			Date end_date = new Date();
			long nmsec = end_date.getTime() - start_date.getTime();
			this.speech_dur = (float)nmsec / 1000;
			if (this.listening) {
				Log.d(getClass().getName(), "Showing Dialog");
				this.rec_dialog = ProgressDialog.show(PocketSphinxAndroidDemo.this, "", "Computing Final Pass .. ", true);
				this.rec_dialog.setCancelable(false);
				this.listening = false;
			}
			/*
			 * Tell the RecognizerTask's thread to stop. 
			 * It will finish analyzing the audio TODO what else does it do?
			 * 
			 */
			this.rec.stop();
			break;
		default:
			;
		}
		/* Let the button handle its own state */
		return false;
	}
	
	
	
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
		InputStream in = this.getResources().openRawResource(res);
		OutputStream out = null;
		
		if (!dir.exists())
		{
			dir.mkdirs();
		}
		
		File file = new File(dir, fileName);
		
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

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		
		final Context context = getApplicationContext();
		CharSequence text = "Hello toast!";
		final int duration = Toast.LENGTH_SHORT;

		extractModelFiles();
		
		CharSequence title = "Carnegie Mellon PocketSphinx Demonstration";
		this.setTitle(title);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_sphinx);

		/*
		 * The User interface is controlled basically by the Button01, the Hold and Speak button.
		 * once the user touches the button the onTouch listener is triggered see the function onTouch above
		 */
		Button b = (Button) findViewById(R.id.Button01);
		b.setOnTouchListener(this);

		/*
		 * digit is for just a string of digits
		 */
		this.digit = (EditText) findViewById(R.id.EditText03);
		digit.setInputType(0);

		digit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
		    @Override
		    public void onFocusChange(View v, boolean hasFocus) {
		        if (hasFocus) {
		        	final Toast toast = Toast.makeText(context, "Digits", duration);
		        	toast.show();

		        	PocketSphinxAndroidDemo.this.rec = new RecognizerTask();
		        	PocketSphinxAndroidDemo.this.rec_thread = new Thread(PocketSphinxAndroidDemo.this.rec);
		        	PocketSphinxAndroidDemo.this.listening = false;
		        	PocketSphinxAndroidDemo.this.rec.setRecognitionListener(PocketSphinxAndroidDemo.this);
		        	PocketSphinxAndroidDemo.this.rec_thread.start();
		        	
		        	activeField = digit;
		        	activeFieldStr  = "digit";
		        	}
		    }
		});
	}

	/** Called when partial results are generated. */
	public void onPartialResults(Bundle b) {
		final PocketSphinxAndroidDemo that = this;
		final String hyp = b.getString("hyp");
		
		that.activeField.post(new Runnable() {
			public void run() {
				if(that.activeFieldStr.equals("number")||that.activeFieldStr.equals("digit"))
				{
					that.activeField.setText(convertWordsToNumbers(hyp));
					//that.activeField.setText(hyp);
				}
				else{
				that.activeField.setText(hyp);
				}
			}
		});
	}

	/** Called with full results are generated. */
	public void onResults(Bundle b) {
		final String hyp = b.getString("hyp");
		final PocketSphinxAndroidDemo that = this;
		this.activeField.post(new Runnable() {
			public void run() {
				
				
				if(that.activeFieldStr.equals("number")||that.activeFieldStr.equals("digit"))
				{
					that.activeField.setText(convertWordsToNumbers(hyp));
				}
				else{
				that.activeField.setText(hyp);
				}

				that.rec_dialog.dismiss();
			}
		});
	}

	public void onError(int err) {
		final PocketSphinxAndroidDemo that = this;
		that.activeField.post(new Runnable() {
			public void run() {
				that.rec_dialog.dismiss();
			}
		});
	}
	
	protected static String convertWordsToNumbers(String hyp)
	{
		String updatedHyp = "";
		
		String[] wordNum = SegmentNumber.segmentNum(hyp.toLowerCase()).replace(" | ","%").split("%");
				
		for(String word:wordNum){
		try {
			word = word.replace("| ","");
			System.out.println(word);
			if(word.equals(""))
				continue;
			String dig = ConvertWordToNumber.WithSeparator(ConvertWordToNumber.parse(word));
			//System.out.println(dig);
			updatedHyp+=dig+" ";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		System.out.println(updatedHyp);
		return updatedHyp;
	}
		
}