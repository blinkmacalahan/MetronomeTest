/**
 * Lines 25 and 31-38 were originally found here:
 * http://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android
 * which came from here:
 * http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
 */
package pntanasis.android.metronome;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class AudioGenerator {
	
    private int sampleRate;
    private AudioTrack audioTrack;
    
    public AudioGenerator(int sampleRate) {
    	this.sampleRate = sampleRate;
    }
    
    public void createPlayer(){
    	//FIXME sometimes audioTrack isn't initialized
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, sampleRate,
                AudioTrack.MODE_STREAM);
    	audioTrack.play();
    }

    public void writeSound(short[] sound) {
        audioTrack.write(sound, 0, sound.length);
    }

    public void writeSound(short[] sound, final int length) {
        audioTrack.write(sound, 0, length);
    }
    
    public void destroyAudioTrack() {
    	audioTrack.stop();
    	audioTrack.release();
    }

//    public static void load(Context context, final String filename) {
//        BufferedReader reader = null;
//        try {
//            context.getAssets().openFd(filename).
//            InputStreamReader st = new InputStreamReader(context.getAssets().open(filename));
//            reader = new BufferedReader(
//                    new InputStreamReader(context.getAssets().open("filename.txt"), "UTF-8"));
//
//            // do reading, usually loop until end of file reading
//            String mLine;
//            while ((mLine = reader.readLine()) != null) {
//                //process line
//                ...
//            }
//        } catch (IOException e) {
//            //log the exception
//        } finally {
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (IOException e) {
//                    //log the exception
//                }
//            }
//        }
//    }
    /**
     * This makes the assumption that the wav file IS MONO and a .wav's header info is 44 bytes (which isn't 100% true), might need
     * to add logic to search for Subchunk2ID (0x64617461 big-endian form) plus 4 bytes to reach beginning
     * of data. See http://soundfile.sapp.org/doc/WaveFormat/ for detail about .wav header format.
     * @param filename
     * @return
     */
    public static SampleDataShort loadSampleFromWav(Context context, final String filename) {
        final int WAV_FILE_HEADER_BYTE_SIZE = 44;
        short[] audio = null;
        try {
            AssetFileDescriptor fd = context.getAssets().openFd(filename);
            long fileLength = fd.getDeclaredLength();
            InputStream is = fd.createInputStream();

            audio = new short[((int) fileLength - WAV_FILE_HEADER_BYTE_SIZE) / 2];//size & length of the file
            BufferedInputStream bis = new BufferedInputStream(is);
            //BufferedInputStream bis = new BufferedInputStream(is, 8000);
            DataInputStream dis = new DataInputStream(bis);      //  Create a DataInputStream to read the audio data from the saved file
            int writeIndex = 0;
            int i = 0;

            //  Read the file into the "audio" array
            while (dis.available() > 0) {
                if (i < WAV_FILE_HEADER_BYTE_SIZE) { //discard header
                    i++;
                    dis.readByte();
                    continue;
                }
                byte byte1 = dis.readByte();
                byte byte2 = dis.readByte();
                int low = (byte1 & 0xff);
                int high = byte2 & 0xff;
                short data = (short) (high << 8 | low);
                audio[writeIndex] = data;                                    //  This assignment does not reverse the order
                writeIndex++;
            }

            dis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new SampleDataShort(audio);
    }

    public static class SampleDataShort implements Sample {
        short[] sample;

        /**
         * The # of samples is needed in order to properly calculate the # of samples of silence needed in-between each metronome beat.
         */
        int sampleCount;

        public SampleDataShort(short[] samp) {
            sample = samp;

            //http://stackoverflow.com/a/5838180
            //Subchunk2Size (aka sample byte size) = NumberOfSamples * NumberOfChannels * (BitsPerSample / 8)

            //The samples being used (clap.txt and snare.txt) are 1 channel and 16 bits per sample.
            //That simplifies the formula to:
            //Subchunk2Size = NumberOfSamples * 1 * (16/8)  --> Subchunk2Size = NumberOfSamples * 2;

            //Now, solve for NumberOfSamples.
            //NumberOfSamples = Subchunk2Size / 2

            //NOTE: Since this uses short (2 bytes) the formula would really be:
            //sampleCount = (sample.length * 2) / 2; however, this simplifies down to what we have below.
            //The reason why we multiply sample.length by 2 is because we need to find the byte size of the samples.
            //Since our samples are represented as shorts (2 bytes) we multiply sample length by 2 to get bytes.
            sampleCount = sample.length;
        }

        @Override
        public int getSampleCount() {
            return sampleCount;
        }

        @Override
        public Object getSample() {
            return sample;
        }
    }

    public interface Sample {
        int getSampleCount();
        Object getSample();
    }
}
