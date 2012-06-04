package ca.ilanguage.labs.pocketsphinx.service;
/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Based on android.speech.SpeechRecognizer.java commit 1c3cca0abed55516d2c6
 * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/speech/SpeechRecognizer.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.Context;

/**
 * This class provides access to the speech recognition service. This service allows access to the
 * speech recognizer. Do not instantiate this class directly, instead, call
 * {@link SpeechRecognizerViaRecordPocketSphinx#createSpeechRecognizer(Context)}. This class's methods must be
 * invoked only from the main application thread. Please note that the application must have
 * {@link android.Manifest.permission#RECORD_AUDIO} permission to use this class.
 * 
 * TODO this class differs from the default system android.speech.SpeechRecognizer in that:
 *   1. It runs with no network connection (using the PocketSphinx running on the device)
 *   2. It allows long audio recording (it does not automatically stop listening when it detects a silence)
 *        It stops recording based on the user preferences 
 *        		a. on back button push 
 *        		b. on screen touch
 *        		c. on screen swipe top to bottom
 *        		d. on voice command (more difficult to implement, but prefered for eyesfree use, and for recording while user is doing something else with the screen)
 * 
 * 
 * TODO For inspiration on how this can be done see a combination of android.speech.SpeechRecognizer, PocketSphinxAndroidDemo, RecognizerTask
 * 
 * 
 */
public class SpeechRecognizerViaRecordPocketSphinx {

}