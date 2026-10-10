package com.cloudwebrtc.webrtc.audio;

import android.util.Log;

import org.webrtc.AudioTrack;
import org.webrtc.AudioTrackSink;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * While a teammate is on air, replaces the microphone samples with the
 * teammate's samples. Otherwise leaves the microphone alone.
 */
public class TeammateAudioMixer
        implements AudioProcessingAdapter.ExternalAudioFrameProcessing, AudioTrackSink {
    private static final String TAG = "TeammateAudioMixer";
    // About 2 seconds of 48 kHz mono 16-bit audio
    private static final int RING_SAMPLES = 96000;

    private final short[] ring = new short[RING_SAMPLES];
    private int readPos = 0;
    private int writePos = 0;
    private int available = 0;

    private volatile boolean active = false;
    private volatile int micSampleRate = 48000;
    private volatile int micChannels = 1;
    private AudioTrack currentTrack;
    private int processCount = 0;
    private int dataCount = 0;

    public synchronized void setSourceTrack(AudioTrack track) {
        Log.i(TAG, "setSourceTrack: " + (track != null ? track.id() : "null"));
        if (currentTrack != null) {
            currentTrack.removeSink(this);
            currentTrack = null;
        }
        readPos = 0;
        writePos = 0;
        available = 0;
        if (track != null) {
            currentTrack = track;
            track.addSink(this);
            active = true;
        } else {
            active = false;
        }
    }

    // ---- microphone side ----
    @Override
    public void initialize(int sampleRateHz, int numChannels) {
        micSampleRate = sampleRateHz;
        micChannels = numChannels;
    }

    @Override
    public void reset(int newRate) {
        micSampleRate = newRate;
    }

    @Override
    public void process(int numBands, int numFrames, ByteBuffer buffer) {
        if (!active) return;
        processCount++;
        if (processCount % 200 == 1) {
            Log.i(TAG, "process active: numFrames=" + numFrames + " bytes=" + buffer.remaining() + " ringAvailable=" + available);
        }
        ByteBuffer b = buffer.duplicate().order(ByteOrder.nativeOrder());
        int samples = b.remaining() / 2;
        synchronized (this) {
            for (int i = 0; i < samples; i++) {
                short s = 0;
                if (available > 0) {
                    s = ring[readPos];
                    readPos = (readPos + 1) % RING_SAMPLES;
                    available--;
                }
                b.putShort(i * 2, s);
            }
        }
    }

    // ---- teammate side ----
    @Override
    public void onData(ByteBuffer audioData, int bitsPerSample, int sampleRate,
                       int numberOfChannels, int numberOfFrames, long absoluteCaptureTimestampMs) {
        dataCount++;
        if (dataCount % 200 == 1) {
            Log.i(TAG, "onData: active=" + active + " bits=" + bitsPerSample + " rate=" + sampleRate + " ch=" + numberOfChannels + " frames=" + numberOfFrames);
        }
        if (!active || bitsPerSample != 16) return;
        ByteBuffer b = audioData.duplicate().order(ByteOrder.nativeOrder());
        int total = numberOfFrames * numberOfChannels;
        synchronized (this) {
            // Simple version: only mono-to-mono at the same sample rate is handled.
            // Anything else is logged so we know what to build next.
            if (sampleRate != micSampleRate || numberOfChannels != micChannels) {
                Log.w(TAG, "format mismatch: teammate " + sampleRate + "Hz/" + numberOfChannels
                        + "ch vs mic " + micSampleRate + "Hz/" + micChannels + "ch");
            }
            for (int i = 0; i < total && b.remaining() >= 2; i++) {
                short s = b.getShort();
                if (available < RING_SAMPLES) {
                    ring[writePos] = s;
                    writePos = (writePos + 1) % RING_SAMPLES;
                    available++;
                }
            }
        }
    }
}