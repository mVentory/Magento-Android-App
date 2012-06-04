package ca.ilanguage.labs.pocketsphinx.service;

import android.content.Intent;
import android.speech.RecognitionService;
/**
 * TODO Implementation and registration of this service in the Manifest will allow it to pop up in the Device settings as a
 * speech recognizer service. The user then can choose which service will be the default. The service should be callable in two ways
 * 
 * 
 * 1. As the default speech recognition service device wide (configured by user in settings)
  		Example registration in the manifest: 
	 		<service android:name="ca.ilanguage.labs.pocketsphinx.service.RecognizerServicePocketSphinx"
	                android:label="@string/service_name">
	
	            <intent-filter>
	                <!-- Here we identify that we are a RecognitionService by specifying that
	                     we satisfy RecognitionService's interface intent.
	                     The constant value is defined at RecognitionService.SERVICE_INTERFACE. -->
	                <action android:name="android.speech.RecognitionService" />
	                <category android:name="android.intent.category.DEFAULT" />
	            </intent-filter>
	
	            <!-- This points to a metadata xml file that contains information about this
	                 RecognitionService - specifically, the name of the settings activity to
	                 expose in system settings.
	                 The constant value is defined at RecognitionService.SERVICE_META_DATA. -->
	            <meta-data android:name="android.speech" android:resource="@xml/recognizer" />
	
	        </service>
 * 2. As a speech recognition service called by an open Intent (hardcoded by developers in their code, they can also check to see if the package manager
 * 		has this package installed, if not, prompt the user to install it. 
 * 		Example client code:
		 	PackageManager pm = getPackageManager();
		        List<ResolveInfo> activities = pm.queryIntentActivities(
		                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		        if (activities.size() == 0) {
		        Intent goToMarket = new Intent(Intent.ACTION_VIEW)
			            .setData(Uri.parse("market://details?id=ca.ilanguage.labs.pocketsphinx"));
			        startActivity(goToMarket);
		        }
  
 * 
 * @author cesine
 *
 */
public class RecognizerServicePocketSphinx extends RecognitionService {

	@Override
	protected void onCancel(Callback listener) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onStartListening(Intent recognizerIntent, Callback listener) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onStopListening(Callback listener) {
		// TODO Auto-generated method stub

	}

}
