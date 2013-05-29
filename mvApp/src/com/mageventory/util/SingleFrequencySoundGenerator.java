package com.mageventory.util;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SingleFrequencySoundGenerator {
	private final int mSampleRate = 8000;
	
	private int mDurationMillis;
	private int mNumSamples;
	private double mFreqOfTone;

	private byte [] mGeneratedSnd;
	
	private AudioTrack mAudioTrack;

	public SingleFrequencySoundGenerator(double freqHz, int lengthMillis)
	{
		mDurationMillis = lengthMillis;
		mFreqOfTone = freqHz;
		
		mNumSamples = mDurationMillis * mSampleRate / 1000;
	}
	
    private void genTone()
    {
    	mGeneratedSnd = new byte[mNumSamples * 2]; 

        for (int i = 0; i < mNumSamples; ++i)
        {
            final short val = (short) (Math.sin(2 * Math.PI * i / (mSampleRate/mFreqOfTone)) * 32767);
            // in 16 bit wav PCM, first byte is the low order byte
            mGeneratedSnd[i*2] = (byte) (val & 0x00ff);
            mGeneratedSnd[i*2+1] = (byte) ((val & 0xff00) >>> 8);
        }
    }

    public void playSound(){
    	
        final Thread thread = new Thread(new Runnable() {
            public void run() {
            	genTone();
            	
            	mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
            			mSampleRate, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, mNumSamples * 2,
                        AudioTrack.MODE_STATIC);
            	mAudioTrack.write(mGeneratedSnd, 0, mGeneratedSnd.length);
            	mAudioTrack.play();
            }
        });
        thread.start();
    }
    
    public void stopSound()
    {
    	mAudioTrack.stop();
    	mAudioTrack.release();
    }
}
