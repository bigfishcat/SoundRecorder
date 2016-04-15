package com.tumanov.andrey.soundrecorder;

import android.media.*;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import java.io.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button startRecordingButton, stopRecordingButton, startPlaybackButton;
    private LevelBar levelBar;
    private SeekBar gainCorrection;

    private boolean isRecording = false;

    private static final int SAMPLE_RATE = 44100;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final float WAV_TIME_KOEFFICIENT = 2;

    private byte[] recorderData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        levelBar = (LevelBar) findViewById(R.id.level_bar);

        startRecordingButton = (Button) findViewById(R.id.start);
        stopRecordingButton = (Button) findViewById(R.id.stop);
        startPlaybackButton = (Button) findViewById(R.id.playback);
        gainCorrection = (SeekBar) findViewById(R.id.gain_correction);

        startRecordingButton.setOnClickListener(this);
        stopRecordingButton.setOnClickListener(this);
        startPlaybackButton.setOnClickListener(this);

        stopRecordingButton.setEnabled(false);
        startPlaybackButton.setEnabled(false);
    }

    public void onClick(View v) {
        if (v == startRecordingButton) {
            record();
        } else if (v == stopRecordingButton) {
            stopRecording();
        } else if (v == startPlaybackButton) {
            play();
        }
    }

    private void play() {
        new PlayAudio().execute();
    }

    private void record() {
        new RecordAudio().execute();
    }
    private void stopRecording() {
        isRecording = false;
    }

    private class PlayAudio extends AsyncTask<Void, Integer, Void> {
        Handler handler;
        float gain;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            handler = new Handler();
            startPlaybackButton.setEnabled(false);
            startRecordingButton.setEnabled(false);
            gain = getGain(MainActivity.this.gainCorrection.getProgress());
            android.util.Log.w("AudioTrack", "gain: " + gain);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                byte[] data = applyGain(recorderData, gain);

                long timePlaying = (long)(data.length / (SAMPLE_RATE * WAV_TIME_KOEFFICIENT) * 1000f);
                final AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO, AUDIO_ENCODING, recorderData.length,
                        AudioTrack.MODE_STATIC);
                audioTrack.write(data, 0, data.length);
                audioTrack.play();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                    audioTrack.setStereoVolume(1.f, 1.f);
                else
                    audioTrack.setVolume(1.f);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startPlaybackButton.setEnabled(true);
                        startRecordingButton.setEnabled(true);
                        audioTrack.release();
                    }
                }, timePlaying);

            } catch (Throwable t) {
                android.util.Log.e("AudioTrack", "Playback Failed");
            }
            return null;
        }
    }

    private class RecordAudio extends AsyncTask<Void, Short, byte[]> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            startRecordingButton.setEnabled(false);
            stopRecordingButton.setEnabled(true);
            startPlaybackButton.setEnabled(false);
            levelBar.clear();
        }

        @Override
        protected byte[] doInBackground(Void... params) {
            isRecording = true;
            try {
                ByteArrayOutputStream memoryStream;
                DataOutputStream dos = new DataOutputStream(memoryStream = new ByteArrayOutputStream());

                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AUDIO_ENCODING);
                AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AUDIO_ENCODING, 2 * bufferSize);
                byte[] buffer = new byte[bufferSize];
                audioRecord.startRecording();
                while (isRecording) {
                    int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
                    dos.write(buffer, 0, bufferReadResult);
                    publishProgress(median(buffer, bufferReadResult));
                }
                audioRecord.stop();
                audioRecord.release();
                dos.close();
                byte[] result = memoryStream.toByteArray();
                android.util.Log.e("AudioRecord", "median: " + median(result, result.length));
                return result;
            } catch (IllegalStateException | IOException e) {
                android.util.Log.e("AudioRecord", "Recording Failed", e);
            }
            return null;
        }
        protected void onProgressUpdate(Short... progress) {
            levelBar.addSamples(progress);
        }
        protected void onPostExecute(byte[] result) {
            startRecordingButton.setEnabled(true);
            stopRecordingButton.setEnabled(false);
            startPlaybackButton.setEnabled(true);
            recorderData = result;
        }
    }

    private float getGain(int x) {
        return (float) x / 50.f;
    }

    private byte[] applyGain(@NonNull byte[] data, float gain) throws IOException {

        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));

        ByteArrayOutputStream memoryStream;
        DataOutputStream outputStream = new DataOutputStream(memoryStream = new ByteArrayOutputStream());

        for (int i = 0; i < data.length / 2; i++) {
            int value = inputStream.readUnsignedShort();
            outputStream.writeShort((int)(value * gain));
        }
        return memoryStream.toByteArray();
    }

    private short median(@NonNull byte[] data, int length) throws IOException {
        if (data.length < length || length <= 2)
            return -1;

        short min = Short.MAX_VALUE, max = Short.MIN_VALUE;
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(data));

        int sum = 0;
        for (int i = 0; i < length / 2; i++) {
            short value = dataStream.readShort();
            if (value < min)
                min = value;
            else if (value < max)
                max = value;
            sum += value;
        }

        sum = sum - min - max;
        return (short) (sum / (length - 2));
    }
}
