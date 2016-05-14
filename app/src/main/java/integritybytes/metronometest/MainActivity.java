package integritybytes.metronometest;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.lang.ref.WeakReference;

import pntanasis.android.metronome.Metronome;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MetroTest";

    private static Metronome sMetronome;
    private static MetronomeHandler sHandler;

    private TextView mBeat, mBpm;
    private ToggleButton mOnOff;
    private SeekBar mBpmSlider;

    private int mBeatCount;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            sHandler = new MetronomeHandler();

            sMetronome = new Metronome(sHandler);
            sMetronome.loadSamples(this);
        }
        sHandler.setActivity(this);

        mBeat = (TextView) findViewById(R.id.beat);
        mOnOff = (ToggleButton) findViewById(R.id.toggle);
        mOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (sMetronome.isPlaying()) {
                    sMetronome.stop();
                } else {
                    sMetronome.play();
                }
            }
        });

        mBpm = (TextView) findViewById(R.id.bpm);
        mBpmSlider = (SeekBar) findViewById(R.id.bpmSlider);

        mBpmSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int bpm = (progress + Metronome.MIN_BPM);
                mBpm.setText("" + bpm);
                sMetronome.setBpm(bpm);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mBpmSlider.setProgress(120 - Metronome.MIN_BPM);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mOnOff.setChecked(false);
    }

    public void onMetronomeClick() {
        mBeatCount++;
        if (mBeatCount >= 4) {
            mBeatCount = 0;
        }
        mBeat.setText("Beat: " + (mBeatCount + 1));
    }

    public void onSamplesLoaded() {
        Log.d(TAG, "Samples loaded");
    }

    private static class MetronomeHandler extends Handler {
        private WeakReference<MainActivity> mActivity;
        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                if (msg.what == Messages.SAMPLES_LOADED) {
                    activity.onSamplesLoaded();
                } else if (msg.what == Messages.METRONOME_CLICK) {
                    activity.onMetronomeClick();
                }
            }
        }

        public void setActivity(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }
    }

    public static class Messages {
        public static int SAMPLES_LOADED = 10;
        public static int METRONOME_CLICK = 11;
    }
}
