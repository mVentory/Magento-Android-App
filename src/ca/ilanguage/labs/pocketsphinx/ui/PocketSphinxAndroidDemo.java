package ca.ilanguage.labs.pocketsphinx.ui;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import com.mageventory.R;

import ca.ilanguage.labs.pocketsphinx.preference.PocketSphinxSettings;
import ca.ilanguage.labs.pocketsphinx.preference.PreferenceConstants;
import ca.ilanguage.labs.pocketsphinx.preference.SpeechRecognitionSettings;
import ca.ilanguage.labs.pocketsphinx.service.RecognitionListenerReduced;
import ca.ilanguage.labs.pocketsphinx.service.RecognizerTask;
import ca.ilanguage.labs.pocketsphinx.util.ConvertWordToNumber;
import ca.ilanguage.labs.pocketsphinx.util.SegmentNumber;
import ca.ilanguage.labs.pocketsphinx.util.Utility;

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

/**
 * This activity presents the user with an activity that has three sections, 
 * Words
 * Numbers
 * Digits
 * 
 * The RecognizerTask will choose which sort of Language Model to use based on which area
 * has focus. (Additional language models can be downloaded from a remote url which is provided in the DownloadData.java activity)
 * 
 * The activity saved the audio which is recorded into the app's external directory
 * 
 * The activity is also able to display text that it "recognizes" as teh user is speaking,
 * this is called the First Pass. Then after the users releases the Hold and Speak button the app
 * goes through and tries to improve the quality of the recognized text now that it has all the context. 
 * 
 * TODO currently this demo can only demo the PocketSphinx speech recognizer, it doesn't make it available for other
 * developers to call, or for the user to use generally.
 * 
 * TODO implement service.SpeechRecognizerViaFilePocketSphinx so that developers can pass a file to the speech recognizer and get bac
 * 		an array of array of hypotheses for utterances in the audio
 * 
 * TODO implement service.SpeechRecognizerViaRecorderSphinx so that users can do speech recognition offline, without a network connection
 * 		(the default speech recognizer provided by com.google.android.voicesearch has to be online and only accepts short utterances. it cannot be used eyes-free). 
 * 
 * 
 * TODO once the two speech recognizers are implemented, edit the ui.TestPocketSPhinxAndAndroidASR so that if PocketSphinx is enabled in the settings, 
 * 		it will run the service.SpeechRecognizerViaRecorderSphinx 
 * 
 *  
 *  
 *  History of this Demo:
 *  	Created by David Huggins-Daines <dhuggins@cs.cmu.edu> sourceforge:dhdfu and other contributors at the cmusphinx project
 *  	Turned into a very user friendly Demo app and apk with very little dependencies by Aasish Pappu sourceforge: aasishp , github aasish
 * 		Infrastructure laid out for eyes-free offline speech recognition by github: cesine
 * 		Eyes-free offline speech recognition implemented by: maybe someone who knows pocketsphinx while i learn how to use it.. ;)
 * 
 * @author aasish
 *
 */
public class PocketSphinxAndroidDemo extends Activity implements OnTouchListener, RecognitionListenerReduced {
	static {
		System.loadLibrary("pocketsphinx_jni");
	}

	/**
	 * Recognizer task, which runs in a worker thread.
	 */
	RecognizerTask rec;
	/**
	 * Download data
	 * 
	 */
	DownloadData downloader;
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
	/**
	 * Performance counter view.
	 */
	TextView performance_text;
	
	TextView which_pass;
	/**
	 * Editable text view.
	 */
	
	final static int ACTIVITY_CREATE = 1;

	EditText words;
	EditText number;
	EditText digit;
	EditText activeField;
	String activeFieldStr = "";
	
	private ProgressDialog pd;
	private final static String PS_DATA_PATH = Environment.getExternalStorageDirectory() + PreferenceConstants.PREFERENCE_BASE_PATH;

	
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
			
			this.which_pass.setTextColor(Color.RED);
			this.which_pass.setText("First Pass");
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

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		
		final Context context = getApplicationContext();
		CharSequence text = "Hello toast!";
		final int duration = Toast.LENGTH_SHORT;

		
		
		CharSequence title = "Carnegie Mellon PocketSphinx Demonstration";
		this.setTitle(title);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_sphinx);

		/*
		 * The current configuration of the app is saved in a text file called currentconf in the 
		 * apps external storage. 
		 * TODO perhaps put the configuration information into a preferences activity to be more
		 * android like in the goal of data persistance
		 */
		if(!Utility.pathExists(PS_DATA_PATH+"currentconf")){
			downloadData();
		}
//		else{
//	    	  String[] defaultConfig = getConfiguration();
//		  		this.rec = new RecognizerTask(defaultConfig[0],defaultConfig[1],defaultConfig[2]);
//		  		this.rec_thread = new Thread(this.rec);
//		  		this.listening = false;
//		  		this.rec.setRecognitionListener(this);
//		  		this.rec_thread.start();
//		}
		/*
		 * The User interface is controlled basically by the Button01, the Hold and Speak button.
		 * once the user touches the button the onTouch listener is triggered see the function onTouch above
		 */
		Button b = (Button) findViewById(R.id.Button01);
		b.setOnTouchListener(this);
		/*
		 * performance text is a line of text which says how long the recording was among other things
		 * 
		 */
		this.performance_text = (TextView) findViewById(R.id.PerformanceText);
		/*
		 * which pass is a line of red text which says if its the first pass or final pass
		 * 
		 */
		this.which_pass = (TextView)findViewById(R.id.WhichPass);

		/*
		 * TODO Words is for free form sentences, or for individual words?
		 */
		this.words = (EditText) findViewById(R.id.EditText01);
		//words.setInputType(0);
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(words.getWindowToken(), 0);
		words.setOnFocusChangeListener(new View.OnFocusChangeListener() {
		    @Override
		    public void onFocusChange(View v, boolean hasFocus) {
		        if (hasFocus) {
		         //change thte config
		         final Toast toast = Toast.makeText(context, "Words", duration);
		         toast.show();
		         //setConfiguration(1);
		         activeField = words;
		         activeFieldStr = "words";
		        }
		    }
		});

		/*
		 * TODO number seems to crash
		 * number is for "one hundred and two"
		 */
		this.number = (EditText) findViewById(R.id.EditText02);
		number.setInputType(0);
		imm.hideSoftInputFromWindow(number.getWindowToken(), 0);
		number.setOnFocusChangeListener(new View.OnFocusChangeListener() {
		    @Override
		    public void onFocusChange(View v, boolean hasFocus) {
		        if (hasFocus) {
		         //change thte config
		        	final Toast toast = Toast.makeText(context, "Numbers", duration);
		        	toast.show();
		        	//setConfiguration(2);
		        	activeField = number;
		        	activeFieldStr = "number";
		        }
		    }
		});
		
		/*
		 * digit is for just a string of digits
		 */
		this.digit = (EditText) findViewById(R.id.EditText03);
		digit.setInputType(0);
		imm.hideSoftInputFromWindow(digit.getWindowToken(), 0);
		digit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
		    @Override
		    public void onFocusChange(View v, boolean hasFocus) {
		        if (hasFocus) {
		        	final Toast toast = Toast.makeText(context, "Digits", duration);
		        	toast.show();
		        	setConfiguration(3);
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
				that.which_pass.setTextColor(Color.RED);
				that.which_pass.setText("First Pass");
				if(that.activeFieldStr.equals("number")||that.activeFieldStr.equals("digit"))
				{
					that.activeField.setText(convertWordsToNumbers(hyp));
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
		this.words.post(new Runnable() {
			public void run() {
				that.which_pass.setTextColor(Color.GREEN);
				that.which_pass.setText("Final Pass");
				
				if(that.activeFieldStr.equals("number")||that.activeFieldStr.equals("digit"))
				{
					that.activeField.setText(convertWordsToNumbers(hyp));
				}
				else{
				that.activeField.setText(hyp);
				}
				Date end_date = new Date();
				long nmsec = end_date.getTime() - that.start_date.getTime();
				float rec_dur = (float)nmsec / 1000;
				that.performance_text.setText(String.format("%.2f seconds %.2f xRT",
															that.speech_dur,
															rec_dur / that.speech_dur));
				Log.d(getClass().getName(), "Hiding Dialog");
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
	
	public boolean onCreateOptionsMenu(Menu menu){

		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_sphinx, menu);
		

		return true;

		}
	
	public boolean onOptionsItemSelected (MenuItem item){

		switch (item.getItemId()){

		
		case R.id.download_data:
			downloadData();
			//showFillerActivity();
		
		return true;
	
		case R.id.settings :
			startActivity(new Intent(this,SpeechRecognitionSettings.class));
		return true;
		case R.id.testBothASR :
		return true;
		case R.id.exit : 
			exitApplication();
		return true;
		
		case R.id.about :
			showAboutActivity();
		return true;
		
		
	  }
	  return false;

    }
	
	public void downloadData()
	{
			Intent i = new Intent(this,DownloadData.class);
			startActivityForResult(i, 0);
	}
	
	public void exitApplication(){
	
	      Log.i("PocketSphinxAndroidDemo","terminated!!");
	       super.onDestroy();
	       this.finish();
	}
	
	/**
	 * Takes in a type of recognition (words, number, digits) and uses this to create a new
	 * RecognizerTask
	 * 
	 * This prepares the Hold and Speak button, says that its not currently listening,
	 * sets the RecognizerTask's recognitionlistener to this activity's context and starts the
	 * recognizertasks thread
	 * 
	 * @param type
	 */
	public void setConfiguration(int type){
		//hardcoded
		//may be open a new activity
		//PocketSphinxSettings
			String[] defaultConfig = new String[3];
			switch(type){
			
			//default hub4 words
			case 1:
				defaultConfig[0] = "hub4wsj_sc_8k";
				defaultConfig[1] = "hub4.5000.DMP";
				defaultConfig[2] = "hub4.5000.dic";
				break;
			//numbers, TODO seems to crash	
			case 2:
				defaultConfig[0] = "hub4wsj_sc_8k";
				defaultConfig[1] = "number.DMP";
				defaultConfig[2] = "number.dic";
				break;
			//digits, seems to work well	
			case 3:
				defaultConfig[0] = "tidigits";
				defaultConfig[1] = "tidigits.DMP";
				defaultConfig[2] = "tidigits.dic";
				break;
	
			}
	  		this.rec = new RecognizerTask(defaultConfig[0],defaultConfig[1],defaultConfig[2]);
	  		this.rec_thread = new Thread(this.rec);
	  		this.listening = false;
	  		this.rec.setRecognitionListener(this);
	  		this.rec_thread.start();
		
	}
	public String[] getConfiguration(){
		//read from config file
		String[] config = new String[3];
		String configFile = PS_DATA_PATH+"currentconf";
		boolean exists = (new File(configFile)).exists();
		if(exists){
			
			//open and write the config to the file
			
			ArrayList<String> lines;
			try {
				lines = Utility.readLines(configFile);
				if(lines.size()>0){
					Log.d("PocketSphinx.Settings","reading configuration");
					config = lines.get(0).split("\t");
					return config;
				}
				else
					Log.d("PocketSphinx.Settings","couldn't read the config file");
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return config;	
		}
		
	
	public void showAboutActivity(){
		
		Intent i = new Intent(this, AboutPocketSphinx.class);
		startActivity(i);
	}
	public void showConfigureActivity(){
		Intent i = new Intent(this,PocketSphinxSettings.class);
		startActivityForResult(i, 1);
	}
	public void showFillerActivity(){
		Intent i = new Intent(this, FillerClass.class);
		startActivity(i);
	}
	
		
	
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
	  super.onActivityResult(requestCode, resultCode, intent);
	  Log.d("PocketSphinx.Main", "result code: " + resultCode + " from " + requestCode);
	  if(resultCode == RESULT_OK){
	    switch(requestCode) {
	      case 0:
	      try{

//	    	if(rec_thread!=null && rec_thread.isAlive()){
//	    		rec_thread.stop();
//	    		rec_thread.destroy();
//	    	}
	    	 Log.d("PocketSphinx.Main", "result code: " + resultCode + " from " + requestCode);  
	  		String[] defaultConfig = getConfiguration();
	  		//Log.d("PocketSphinx.Main","configuration is " + defaultConfig[0]);
	  		//RecognizerTask(String hmm, String lm, String dict)
	  		this.rec = new RecognizerTask(defaultConfig[0],defaultConfig[1],defaultConfig[2]);
	  		
	  		this.rec_thread = new Thread(this.rec);
	  		this.listening = false;
	  		this.rec.setRecognitionListener(this);
	  		this.rec_thread.start();
	    	  
	      }catch(NullPointerException e){
	    	  e.printStackTrace();
	      }break;
	      
	      case 1:
//	    	  if(rec_thread!=null && rec_thread.isAlive()){
//		    		rec_thread.stop();
//		    		rec_thread.destroy();
//		    	}
	    	  String[] defaultConfig = getConfiguration();
		  		this.rec = new RecognizerTask(defaultConfig[0],defaultConfig[1],defaultConfig[2]);
		  		this.rec_thread = new Thread(this.rec);
		  		this.listening = false;
		  		this.rec.setRecognitionListener(this);
		  		this.rec_thread.start();
	    break;  	      	      
	    }
	    
	  }
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