/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/

package com.mageventory.util;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;

public class SingleFrequencySoundGenerator {
    private final int mSampleRate = 8000;

    private int mDurationMillis;
    private int mNumSamples;
    private double mFreqOfTone;

    private byte[] mGeneratedSnd;

    private AudioTrack mAudioTrack;
    private boolean mSquareWave;
    private boolean mReleased;
    private boolean mIsInitialized;

    public SingleFrequencySoundGenerator(double freqHz, int lengthMillis) {
        this(freqHz, lengthMillis, false);
    }

    public SingleFrequencySoundGenerator(double freqHz, int lengthMillis, boolean squaredWave) {
        mDurationMillis = lengthMillis;
        mFreqOfTone = freqHz;

        mNumSamples = mDurationMillis * mSampleRate / 1000;
        mSquareWave = squaredWave;
    }

    private void genTone() {
        mGeneratedSnd = new byte[mNumSamples * 2];

        for (int i = 0; i < mNumSamples; ++i) {
            final short val = (short) (Math.sin(2 * Math.PI * i / (mSampleRate / mFreqOfTone)) * 32767);
            // in 16 bit wav PCM, first byte is the low order byte
            mGeneratedSnd[i * 2] = (byte) (val & 0x00ff);
            mGeneratedSnd[i * 2 + 1] = (byte) ((val & 0xff00) >>> 8);
        }
    }

    private void genToneSquare() {
        mGeneratedSnd = new byte[mNumSamples * 2];

        for (int i = 0; i < mNumSamples; ++i) {
            short val = (short) (Math.sin(2 * Math.PI * i / (mSampleRate / mFreqOfTone)) * 32767);

            if (val < 0)
                val = -32767;
            else
                val = 32767;

            // in 16 bit wav PCM, first byte is the low order byte
            mGeneratedSnd[i * 2] = (byte) (val & 0x00ff);
            mGeneratedSnd[i * 2 + 1] = (byte) ((val & 0xff00) >>> 8);
        }
    }

    public void stopSound()
    {
        synchronized (SingleFrequencySoundGenerator.this)
        {
            if (!mReleased)
            {
                if (mIsInitialized)
                {
                    mAudioTrack.stop();
                    mAudioTrack.release();
                }
                mReleased = true;
            }
        }
    }

    public void playSound() {

        if (mAudioTrack != null)
        {
            return;
        }

        final Thread thread = new Thread(new Runnable() {
            public void run() {

                if (mAudioTrack != null)
                {
                    return;
                }

                if (mSquareWave)
                    genToneSquare();
                else
                    genTone();

                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, mNumSamples * 2, AudioTrack.MODE_STATIC);

                mAudioTrack.write(mGeneratedSnd, 0, mGeneratedSnd.length);

                synchronized (SingleFrequencySoundGenerator.this)
                {
                    mIsInitialized = true;

                    if (mReleased)
                    {
                        mAudioTrack.stop();
                        mAudioTrack.release();
                        return;
                    }
                }

                mAudioTrack.play();

                mAudioTrack.setNotificationMarkerPosition(mNumSamples - 100);
                mAudioTrack
                        .setPlaybackPositionUpdateListener(new OnPlaybackPositionUpdateListener() {
                            @Override
                            public void onPeriodicNotification(AudioTrack track) {
                                // nothing to do
                            }

                            @Override
                            public void onMarkerReached(AudioTrack track) {

                                stopSound();
                            }
                        });
            }
        });
        thread.start();
    }
}
