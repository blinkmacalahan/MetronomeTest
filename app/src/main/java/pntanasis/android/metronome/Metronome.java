package pntanasis.android.metronome;

import android.content.Context;
import android.os.Handler;

import integritybytes.metronometest.MainActivity;


public class Metronome {

    private Object mLock = new Object();
    private double mBpm = 120;
    public static int MIN_BPM = 30;
    private static final int SAMPLE_RATE = 44100;

    private boolean play = false;

    private AudioGenerator audioGenerator = new AudioGenerator(SAMPLE_RATE);
    private Handler mHandler;
    private short[] mTockSilenceSoundArray;

    private AudioGenerator.Sample mTock;

    private int mBeatDivisionSampleCount;

    public Metronome(Handler handler) {
        mHandler = handler;
    }

    public void loadSamples(Context context) {
        mTock = AudioGenerator.loadSampleFromWav(context, "snare.wav");
        mHandler.sendEmptyMessage(MainActivity.Messages.SAMPLES_LOADED);
    }

    public void calcSilence() {
        //(beats per second * SAMPLE_RATE) - NumberOfSamples
        mBeatDivisionSampleCount = (int) (((60 / mBpm) * SAMPLE_RATE));

        int silence = Math.max(mBeatDivisionSampleCount - mTock.getSampleCount(), 0);
        mTockSilenceSoundArray = new short[silence];

        for (int i = 0; i < silence; i++)
            mTockSilenceSoundArray[i] = 0;
    }

    private void isInitialized() {
        if (mTock == null || mHandler == null) {
            throw new IllegalStateException("Not initialized correctly");
        }
    }

    public void play() {
        isInitialized();

        play = true;
        audioGenerator.createPlayer();
        calcSilence();
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {

                    short[] sample = (short[]) mTock.getSample();
                    audioGenerator.writeSound(sample, sample.length > mBeatDivisionSampleCount ? mBeatDivisionSampleCount : sample.length);
                    mHandler.sendEmptyMessage(MainActivity.Messages.METRONOME_CLICK);
                    audioGenerator.writeSound(mTockSilenceSoundArray);

                    synchronized (mLock) {
                        if (!play) return;
                    }

                } while (play);
            }
        }).start();
    }

    public void pause() {
        play = false;
        audioGenerator.destroyAudioTrack();
    }

    public void stop() {
        if (!play) return;

        pause();
        synchronized (mLock) {
            mBeatDivisionSampleCount = 0;
        }
    }

    public boolean isPlaying() {
        return play;
    }

    public void setBpm(int bpm) {
        mBpm = bpm;
        calcSilence();
    }
}
