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
 * This class provides access to the speech recognition service to run on an MP3 file which is present on the device. This service allows access to the
 * speech recognizer. Do not instantiate this class directly, instead, call
 * {@link SpeechRecognizerViaFilePocketSphinx#createSpeechRecognizer(Context)}. This class's methods must be
 * invoked only from the main application thread. 
 * 
 * TODO This class expects an audio file 
 *  1. chunks on pauses
 *  2. sends each chunk to be recognized (either on the device or on a server)
 *  3. returns results as an array of array hypotheses 
 * 
 * 
 * TODO For inspiration on how this can be done see a combination of android.speech.SpeechRecognizer, PocketSphinxAndroidDemo, RecognizerTask
 * 
 * TODO Learn how PocketSphinx works and how change PocketSphinxAndroidDemo to work for a file instead of recording
 *        some info might be here: http://sourceforge.net/projects/cmusphinx/forums/forum/5471/topic/4023606
 *        "In recent version pocketsphinx_continuous has -infile argument to pass file to decode."
 * 
 * TODO How to do the chunking, two options:
 * 	1. LIUM tools allows for speech stream segmentation and speaker recognition. 
 *     Documentation is for command line use (or perl) TODO figure out how to use it programatically
 *  2. Port Praat to Android
 *     Praat is written in C++ and is a powerful phonetic analysis tool used by Phoneticians, might need it anyway to use additional prosodic information to improve Sphinx's recognition
 * 
 * Please note that the application DOES NOT NEED
 * {@link android.Manifest.permission#RECORD_AUDIO} permission to use this class.
 */
public class SpeechRecognizerViaFilePocketSphinx {
    
}