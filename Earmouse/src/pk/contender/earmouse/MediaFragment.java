package pk.contender.earmouse;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.primitives.Ints;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.min;


/**
 * Handles the MediaPlayer and generates WAV files needed for exercises.
 *
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class MediaFragment extends Fragment {

    /** The name of the most recently generated WAV file */
    private static final String PREPARED_WAV_FILENAME = "prepared_exercise.wav";
    /* SharedPreferences constants */
    private static final String PREFERENCES_CURRENTEXERCISEOBJECT = "PREFERENCES_CURRENTEXERCISEOBJECT";

    private Context mCtx;
    private AssetManager mAssetMan;
    private MediaPlayer mPlayer = null;

    private ImageButton playButton;
    /** the bit rate of the samples we use to generate our exercises */
    private static final int SAMPLES_BITRATE = 16;
    /** The sampling rate of the samples we use to generate our exercises */
    private static final int SAMPLES_RATE = 44100;
    /** The amount of samples available */
    @SuppressWarnings("unused")
    private static final int SAMPLE_COUNT = 41;

    /** Default values for fast playback mode */
    private static final int SAMPLE_LENGTH = 20000;
    private static final int SAMPLE_OVERLAP = 2000;

    /** The length of one sound in samples to make the sequence faster (0 to play the whole unit) */
    private int sample_length = 0;
    /** The length of the overlap to smooth the transition (0 for no smoothing) */
    private int sample_overlap = 0;

    /** Current {@link pk.contender.earmouse.Exercise}, used for state management.*/
    private Exercise currentExercise = null;
    private boolean playWhenReady;

    /**
     * Different states this object can be in:
     * - Idle, mplayer is ready, nothing is playing, no exercise is ready to play
     *      - Should be showing play button
     *      - Should accept a new exercise to prepare
     *      - Should not respond to clickPlay() from parent
     * - Ready, mplayer is ready, nothing is playing, an exercise is prepared
     *      - Should be showing play button
     *      - Should accept a new exercise to prepare
     *      - Should respond to clickPlay()
     * - Playing, mplayer is playing
     *      - Should be showing pause button
     *      - Should accept a new exercise to prepare, but then stop playing.
     *      - Should respond to clickPlay() to pause playback
     * - Paused, mplayer is paused
     *      - Should be showing Play button
     *      - Should accept a new exercise to prepare
     *      - Should respond to clickPlay() to resume playback
     * - Preparing, asyncworker is preparing an exercise
     *      - Should be showing Play button
     *      - Should refuse to start preparing another exercise
     *      - Should not respond to clickPlay()
     *      - Should immediately play when ready when playWhenReady is set.
     *  - Stopped, mplayer has finished playing an exercise
     *      - Should be showing Play button
     *      - Should accept a new exercise to prepare
     *      - If this exercise was a practice exercise, should immediately prepare the original exercise
     *      - Should respond to clickPlay(), in this case that meaks seek(0)
     */
    final private Object stateLock = new Object();
    private boolean playingPracticeExercise;

    private enum MediaPlayerState { IDLE, READY, PLAYING, PAUSED, PREPARING, STOPPED }
    private MediaPlayerState mpState;


    /**
     * Set up MediaPlayer, Assets and a few listeners
     * @param savedInstanceState the saved instance state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCtx = getActivity();
        if(mCtx == null)
            Log.d("DEBUG", "Context is null in MediaFragment onCreate()");

        synchronized (stateLock) {
            mPlayer = new MediaPlayer();
            mpState = MediaPlayerState.IDLE;
        }

        mAssetMan = mCtx.getAssets();

        //Setup MediaPlayer listeners
        mPlayer.setOnErrorListener(new OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.d("DEBUG", "MediaPlayer in ERROR state(" + what + ", " + extra);
                Toast.makeText(mCtx, "Error playing sound, try restarting the app if problem persists.", Toast.LENGTH_SHORT).show();
                return false;
            }

        });
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                synchronized (stateLock) {
                    switch (mpState) {
                        case PREPARING:
                            if(playWhenReady) {
                                mpState = MediaPlayerState.PLAYING;
                                if (!playingPracticeExercise) setButtonImagePause();
                                mPlayer.start();
                            } else {
                                mpState = MediaPlayerState.READY;
                                setButtonImagePlay();
                            }
                            break;
                        default:
                            Log.d("DEBUG", "onPrepared(): Unexpected state " + mpState);
                    }
                }
            }
        });
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                synchronized (stateLock) {
                    switch (mpState) {
                        case PLAYING:
                            mpState = MediaPlayerState.STOPPED;
                            setButtonImagePlay();
                            if(playingPracticeExercise) {
                                playingPracticeExercise = false;
                                // TODO: prepare original exercise now.
                                prepareExercise(currentExercise, false);
                            }
                            break;
                        default:
                            Log.d("DEBUG", "onCompletion(): unexpected state: " + mpState);
                    }
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media, container, false);
        playButton = (ImageButton) view.findViewById(R.id.play_button);

        // Restore state
/*
        SharedPreferences settings = mCtx.getSharedPreferences(Main.PREFS_NAME, Activity.MODE_PRIVATE);
        Gson gson = new Gson();
        currentExercise = gson.fromJson(settings.getString(PREFERENCES_CURRENTEXERCISEOBJECT, null), Exercise.class);
        if(currentExercise != null) {
            synchronized (mpLock) {
                //mPlayer.reset();
                //mpState = MPlayerState.IDLE;
                prepareExercise(currentExercise, false);
            }
        } */
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = mCtx.getSharedPreferences(Main.PREFS_NAME, Activity.MODE_PRIVATE);
        if(prefs.getBoolean(Main.PREFS_FASTPLAY, false)) {
            sample_length = SAMPLE_LENGTH;
            sample_overlap = SAMPLE_OVERLAP;
        } else {
            sample_length = sample_overlap = 0;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
/*
        SharedPreferences settings = mCtx.getSharedPreferences(Main.PREFS_NAME, Activity.MODE_PRIVATE);
        Gson gson = new Gson();
        settings.edit().putString(PREFERENCES_CURRENTEXERCISEOBJECT, gson.toJson(currentExercise)).apply();
        super.onSaveInstanceState(outState); */
    }

    /**
     * This would go well together with storing the current position in the media and restore that.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // No need for synchronized or state management, this Fragment is gone.
        if(mPlayer != null) {
            mPlayer.release();
        }
    }

    /**
     * In order to conform with FN-A1 to 4, we must pause playback when the user leaves the activity
     * @see <a href=http://developer.android.com/distribute/essentials/quality/core.html>Android Core app quality</a>
     */
    @Override
    public void onPause() {
        super.onPause();
        synchronized (stateLock) {
            if(mpState == MediaPlayerState.PLAYING) {
                mPlayer.pause();
                setButtonImagePlay();
                mpState = MediaPlayerState.PAUSED;
            }
        }
    }

    /**
     * Called when the Play button is clicked, the action taken depends on the current state of instance
     */
    public void clickPlay() {
        synchronized (stateLock) {
            switch(mpState) {
                case READY:
                    mPlayer.start();
                    mpState = MediaPlayerState.PLAYING;
                    setButtonImagePause();
                    break;
                case PLAYING:
                    // We dissociate the play button from the practice mode
                    if (!playingPracticeExercise) {
                        mPlayer.pause();
                        mpState = MediaPlayerState.PAUSED;
                        setButtonImagePlay();
                    } else {
                        Log.d("DEBUG", "clickPlay(): Ignoring playButton click in mpstate: " + mpState);
                    }
                    break;
                case PAUSED:
                    mPlayer.start();
                    mpState = MediaPlayerState.PLAYING;
                    setButtonImagePause();
                    break;
                case STOPPED:
                    mPlayer.seekTo(0);
                    mPlayer.start();
                    mpState = MediaPlayerState.PLAYING;
                    setButtonImagePause();
                    break;
                default:
                    Log.d("DEBUG", "clickPlay(): Ignoring playButton click in mpstate: " + mpState);
            }
        }
    }

    /**
     * Set the Play button to display a Play icon
     */
    private void setButtonImagePlay() {
        Resources res = getResources();
        playButton.setImageDrawable(res.getDrawable(android.R.drawable.ic_media_play));
    }

    /**
     * Set the Play button to display a Pause icon
     */
    private void setButtonImagePause() {
        Resources res = getResources();
        playButton.setImageDrawable(res.getDrawable(android.R.drawable.ic_media_pause));
    }

    /**
     * Prepares an exercise to be played.
     * @param exercise
     * @param playNow
     */
    public void prepareExercise(Exercise exercise, boolean playNow) {

        if (!playingPracticeExercise) {
            currentExercise = exercise;
        }
        synchronized (stateLock) {
            switch(mpState) {
                case PREPARING:
                    Log.d("DEBUG", "prepareExercise(): refusing to prepare new exercise in state " + mpState);
                    break;
                case PLAYING:
                    mPlayer.stop();
                    mpState = MediaPlayerState.STOPPED;
                    setButtonImagePlay();
                default:
                    playWhenReady = playNow;
                    mpState = MediaPlayerState.PREPARING;
                    new PrepareExerciseWorker().execute(exercise);
                    break;
            }
        }
    }

    public void playPractice(Exercise exercise) {
        playingPracticeExercise = true;
        prepareExercise(exercise, true);
    }

    /**
     * Prepares a WAVE file for playback of a given {@link pk.contender.earmouse.Exercise}
     * <p>
     * Using the samples from {@link android.content.res.AssetManager}, generates a WAV file by mixing and concatenating samples
     * and loads this into the MediaPlayer for playback.
     *
     * @author Paul Klinkenberg <pklinken.development@gmail.com>
     */
    private class PrepareExerciseWorker extends AsyncTask<Exercise, Void, Void> {

        @Override
        protected Void doInBackground(Exercise... params) {

            // for debug worker thread
            if(android.os.Debug.isDebuggerConnected())
                android.os.Debug.waitForDebugger();


            Exercise exercise = params[0];
            // List of all the samples to be concatenated
            List<byte []> exerciseUnitBufferList = new ArrayList<>();
            int outputSamplerate = SAMPLES_RATE , outputBitrate = SAMPLES_BITRATE;

            for (int i=0;i < exercise.exerciseUnits.size();i++)
                try {
                    exerciseUnitBufferList.add(prepareExerciseUnit(exercise.exerciseUnits.get(i)));
                } catch (IOException e) {
                    e.printStackTrace();
                    cancel(true);
                }
            // At this point we have a list of all the exerciseUnits that are to be
            // concatenated.

	    // start with the length of the WAV header
            int totalSize = 44;
            int unitLen = 0;
            int bufLen = 0;
            for (byte [] buf : exerciseUnitBufferList) {
                bufLen = buf.length;
                unitLen = sample_length > 0 ? min(bufLen, sample_length*4) : bufLen;
                totalSize += unitLen;
            }
            // now add the rest of the last buffer
            totalSize += bufLen - unitLen;
            // totalSize is now the total size of our output data + the size of a WAV header (44 bytes)

            byte [] header = createWavHeader(totalSize, outputSamplerate, outputBitrate);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // Write the header to our outputStream
            try {
                outputStream.write(header);
            } catch (IOException e) {
                e.printStackTrace();
                cancel(true);
            }
            // And all the exerciseUnitBuffers
            int exerciseLen = exerciseUnitBufferList.size();
            int idx = 0;
            // buffer has format Lin16 with 2 channels = 4 Bytes per Sample
            byte [] overlap = new byte[sample_overlap * 4];
            float delta = 1.0f / (sample_overlap);
            for(byte [] exerciseUnitBuffer : exerciseUnitBufferList) {
                int len = exerciseUnitBuffer.length;
                idx++;
                try {
                    // fading can be disabled by setting SAMPLE_OVERLAP to 0
                    // don't fade for the first unit at all
                    if (sample_overlap > 0 && sample_length > 0 && idx > 1) {
                        // fade SAMPLE_OVERLAP from the previous sound out linearly
                        for (int i = 0; i < sample_overlap * 4; i += 4) {
                            // handle both channels
                            for (int j = 0; j < 4; j += 2) {
                                short s1 = (short) ( (exerciseUnitBuffer[i+j] & 0xff) + ( (exerciseUnitBuffer[i+j + 1] & 0xff) << 8));
                                short s2 = (short) ( (overlap[i+j] & 0xff) + ( (overlap[i+j + 1] & 0xff) << 8));
                                int s = (int) (s1 + (1.0f - delta * i/4) * s2);
                                //clip into range
                                if (s > Short.MAX_VALUE) {
                                    s = Short.MAX_VALUE;
                                } else if (s < Short.MIN_VALUE) {
                                    s = Short.MIN_VALUE;
                                }
                                exerciseUnitBuffer[i+j] = (byte) (s & 0xff);
                                exerciseUnitBuffer[i+j + 1] = (byte) ((s >> 8) & 0xff);
                            }
                        }
                    }
                    if (sample_length == 0 || idx == exerciseLen) {
                        outputStream.write(exerciseUnitBuffer);
                    } else {
                        unitLen = min(len, sample_length * 4);
                        byte [] part = Arrays.copyOfRange(exerciseUnitBuffer, 0, unitLen);
                        outputStream.write(part);
                        // save overlap part after the one we have just written
                        overlap = Arrays.copyOfRange(exerciseUnitBuffer, unitLen, min (unitLen + sample_overlap * 4, len) );
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    cancel(true);
                }
            }

            // Write the entire buffer to our temporary file.
            try {
                FileOutputStream fos = mCtx.openFileOutput(PREPARED_WAV_FILENAME, Context.MODE_PRIVATE);
                fos.write(outputStream.toByteArray());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
                cancel(true);
            }

            // Load this file into the MediaPlayer, mPlayer.prepare() is a blocking function but this
            // is an AsyncTask
            try {
                synchronized (stateLock) {
                    if(mpState != MediaPlayerState.PREPARING) {
                        Log.d("DEBUG", "PrepareExerciseWorker(): unexpected state: " + mpState);
                    } else {
                        mPlayer.reset();
                        mPlayer.setDataSource(mCtx.getFilesDir().getPath() + "/" + PREPARED_WAV_FILENAME);
                        mPlayer.prepareAsync();
                    }
                }
            } catch (IllegalArgumentException | SecurityException
                    | IllegalStateException | IOException e) {
                e.printStackTrace();
                cancel(true);
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            Toast toast = Toast.makeText(mCtx, mCtx.getResources().getText(R.string.media_error_preparing), Toast.LENGTH_LONG);
            toast.show();
        }

        @Override
        protected void onPostExecute(Void result) {
        }
}

    /**
     * Mixes the samples associated with the given List of Integers
     * <p>
     * Loads all the samples associated with the given List<Integer> and mixes them into a
     * single buffer that is the size of the largest sample in the set minus its WAV header.
     * Also performs some anti-clipping protection.
     * @param exerciseUnit The list of samples to mix
     * @return A buffer containing a mix of all the samples in exerciseUnit, without a WAV header
     * @throws IOException
     */
    private byte [] prepareExerciseUnit(List<Integer> exerciseUnit) throws IOException {

        long outputSize = 0;
        /** The amount of samples that are to be mixed */
        int sampleCount = exerciseUnit.size();

        List<AssetFileDescriptor> AssFdList = new ArrayList<>();

        for (int sample : exerciseUnit) {
            AssetFileDescriptor assFd = mAssetMan.openFd("sample" + (sample + 1) + ".wav");
            AssFdList.add(assFd);
            if(assFd.getLength() > outputSize) {
                outputSize = assFd.getLength();
            }
        }
        // We now have a List of open AssetFileDescriptors to the required samples and outputSize is the size of our output file
        // So we can allocate the space minus the size of the WAV header
        outputSize -= 44;
        byte [] output = new byte [(int) outputSize];

        // Now we create a list of Little Endian converting streams to read our data from (WAVs are little-endian, Java is big-endian..)
        List<LittleEndianDataInputStream> samplesFdList = new ArrayList<>();
        for (AssetFileDescriptor assFd : AssFdList) {
            samplesFdList.add(new LittleEndianDataInputStream(assFd.createInputStream()));
        }

        // Skip the WAV headers on the input streams, we wont be needing those.
        for (LittleEndianDataInputStream sampleFd : samplesFdList) {
            //noinspection ResultOfMethodCallIgnored
            sampleFd.skip(44);
        }

        // Read the data from all our open streams
        byte[][] buf = new byte [sampleCount][];

        for (int i = 0; i < sampleCount ; i++) {
            buf[i] = new byte[(int) outputSize];

            //noinspection ResultOfMethodCallIgnored
            samplesFdList.get(i).read(buf[i]);

        }

        // Now we go through these, sample by sample, and mix and clip them, if necessary.

        int sum;
        int clippedSamples = 0;
        for (int index = 44; index < outputSize; index += 2) {
            sum = 0;

            for (byte [] item : buf) {
                if(index + 1 >= item.length) // Array out of bounds, add nothing to sum
                    continue;

                // Little endian conversion
                short tmp = (short)Ints.fromBytes((byte) 0, (byte) 0, item[index + 1], item[index]);
                // reduce the amplitude a bit based on the amount of samples we are mixing to avoid
                // excessive clipping later
                tmp *= (1.0f - sampleCount * 0.1f);
                sum += tmp;

            }
            //clip into range
            if(sum > Short.MAX_VALUE) {
                sum = Short.MAX_VALUE;
                clippedSamples++;
            } else if(sum < Short.MIN_VALUE) {
                sum = Short.MIN_VALUE;
                clippedSamples++;
            }

            // Write into output buffer converting endian again
            output[index] = (byte) (sum & 0xff);
            output[index + 1] = (byte) ((sum >> 8) & 0xff);
        }

       // Log.d("DEBUG", "Clipped sample percentage: " + (float)((float)clippedSamples / (float)((float)(outputSize - 44f) / 2f) * 100f));

        // Close out input file descriptors
        for (LittleEndianDataInputStream sampleFd : samplesFdList) {
            sampleFd.close();
        }
        for (AssetFileDescriptor assFd : AssFdList) {
            assFd.close();
        }

        return output;
    }

    /**
     * Returns a WAV header for the given parameters
     *
     * @param bufSize The size of the output WAV (header + data) in bytes
     * @param samplerate The sample rate of the WAV file
     * @param bitrate The bitrate of the WAV file
     * @return A 44-byte WAV header
     */
    private byte [] createWavHeader(int bufSize, int samplerate, int bitrate) {

        byte [] buf = new byte[44];
        int chunkSize = bufSize - 8;
        int byterate = samplerate * 2 * (bitrate / 2);

        buf[0] = 'R';  // RIFF/WAVE header
        buf[1] = 'I';
        buf[2] = 'F';
        buf[3] = 'F';
        buf[4] = (byte) (chunkSize & 0xff);
        buf[5] = (byte) ((chunkSize >> 8) & 0xff);
        buf[6] = (byte) ((chunkSize >> 16) & 0xff);
        buf[7] = (byte) ((chunkSize >> 24) & 0xff);
        buf[8] = 'W';
        buf[9] = 'A';
        buf[10] = 'V';
        buf[11] = 'E';
        buf[12] = 'f';  // 'fmt ' chunk
        buf[13] = 'm';
        buf[14] = 't';
        buf[15] = ' ';
        buf[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        buf[17] = 0;
        buf[18] = 0;
        buf[19] = 0;
        buf[20] = 1;  // format = 1 (PCM)
        buf[21] = 0;
        buf[22] = (byte) 2;
        buf[23] = 0;
        buf[24] = (byte) (samplerate & 0xff);
        buf[25] = (byte) ((samplerate >> 8) & 0xff);
        buf[26] = (byte) ((samplerate >> 16) & 0xff);
        buf[27] = (byte) ((samplerate >> 24) & 0xff);
        buf[28] = (byte) (byterate & 0xff);
        buf[29] = (byte) ((byterate >> 8) & 0xff);
        buf[30] = (byte) ((byterate >> 16) & 0xff);
        buf[31] = (byte) ((byterate >> 24) & 0xff);
        buf[32] = (byte) (2 * (bitrate / 2));  // block align
        buf[33] = 0;
        buf[34] = (byte) bitrate;  // bits per sample
        buf[35] = 0;
        buf[36] = 'd';
        buf[37] = 'a';
        buf[38] = 't';
        buf[39] = 'a';
        buf[40] = (byte) ((bufSize - 44) & 0xff);
        buf[41] = (byte) (((bufSize - 44) >> 8) & 0xff);
        buf[42] = (byte) (((bufSize - 44) >> 16) & 0xff);
        buf[43] = (byte) (((bufSize - 44) >> 24) & 0xff);

        return buf;
    }
}
