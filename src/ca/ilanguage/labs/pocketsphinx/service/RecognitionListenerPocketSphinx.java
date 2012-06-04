package ca.ilanguage.labs.pocketsphinx.service;

import android.os.Bundle;
import android.speech.RecognitionListener;
/**
 * 
 * This in theory should be identical to {@link android.speech.RecognitionListener} and therefore unnecessary. 
 * However it would require implementing a lot of functions in any classes that extend it (ie, the SpeechRecognizer for PocketSphinx) 
 * 
 * TODO decide if this should be a reduced version of the RecognitionListener interface (as was done in the original 
 * by  David Huggins-Daines <dhuggins@cs.cmu.edu (dhdfu), or decide to implement a middle ground version of the RecognitionListner, enough so that it is possible to register
 * PocketSphinx with the device as a speech recognition system that is capable of handling the the full functionality of the RecognizerIntent.
 * 
 * 
 * TODO refer to {@link RecognitionListenerReduced} for an implementation of a subset of the
 *  {@link android.speech.RecognitionListener}, which dhdfu did  to avoid dependencies on Froyo and methods we don't need or can't provide. But the the 
 *  original manifest was changed to Froyo, so maybe this design consideration is depreciated. 
 * 
 * The full list of methods are here..
 * 
 * 
 */
public interface RecognitionListenerPocketSphinx  {

	
	public void onBeginningOfSpeech() ;

	
	public void onBufferReceived(byte[] arg0) ;

	
	public void onEndOfSpeech() ;

	
	public void onError(int error) ;

	
	public void onEvent(int eventType, Bundle params) ;

	
	public void onPartialResults(Bundle partialResults) ;

	
	public void onReadyForSpeech(Bundle params) ;

	
	public void onResults(Bundle results) ;

	
	public void onRmsChanged(float rmsdB) ;

}
