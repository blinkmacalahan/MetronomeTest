package pntanasis.android.metronome;

import android.content.Context;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

import integritybytes.metronometest.MainActivity;


public class Metronome implements AudioTrack.OnPlaybackPositionUpdateListener {
    private static final String TAG = "Metronome";

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

    private int mMarkerReachedCount;
    private int mInitialMarkerPosition;

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

        mInitialMarkerPosition = mTock.getSampleCount() / 8;
        audioGenerator.getAudioTrack().setNotificationMarkerPosition(mInitialMarkerPosition);
        audioGenerator.getAudioTrack().setPlaybackPositionUpdateListener(this);
    }

    private void isInitialized() {
        if (mTock == null || mHandler == null) {
            throw new IllegalStateException("Not initialized correctly");
        }
    }

    public void play() {
        isInitialized();
        mMarkerReachedCount = 0;
        play = true;
        audioGenerator.createPlayer();
        calcSilence();
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {

                    short[] sample = (short[]) mTock.getSample();
                    Log.d(TAG, "run: hp = " + audioGenerator.getAudioTrack().getPlaybackHeadPosition());
                    audioGenerator.writeSound(sample, sample.length > mBeatDivisionSampleCount ? mBeatDivisionSampleCount : sample.length);
                    //mHandler.sendEmptyMessage(MainActivity.Messages.METRONOME_CLICK);
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
    }

    @Override
    public void onMarkerReached(AudioTrack track) {
        final int headPosition = track.getPlaybackHeadPosition();
//        //This calculates the header position we want/ed to be at
        final int plannedHeadPosition = mInitialMarkerPosition + (mMarkerReachedCount * mBeatDivisionSampleCount);
        int nextMarkerPosition = (plannedHeadPosition + mBeatDivisionSampleCount) - headPosition;
        Log.d(TAG, "onMarkerReached: planned = " + plannedHeadPosition + ", hp = " + headPosition + ", next = " + nextMarkerPosition);
        track.setNotificationMarkerPosition(headPosition + nextMarkerPosition);

        mHandler.sendEmptyMessage(MainActivity.Messages.METRONOME_CLICK);
        mMarkerReachedCount++;
    }

    @Override
    public void onPeriodicNotification(AudioTrack track) {

    }
}
