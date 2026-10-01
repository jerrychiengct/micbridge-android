package com.jerry.micbridge;

import android.media.*;
import android.os.*;

/** Foreground-only live audio. Selected routes must be verified before voice playback. */
public final class AudioEngine {
    public interface Listener {
        void onReady(String route);
        void onMeter(float peak);
        void onStopped(String reason);
    }
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private final Object resources = new Object();
    private volatile boolean running;
    private volatile boolean muted = true;
    private volatile float gain = 1f;
    private volatile EffectSettings effects = EffectSettings.natural();
    private volatile String stopReason = "Stopped";
    private AudioRecord recorder;
    private AudioTrack player;
    private Thread thread;

    public AudioEngine(Listener listener) { this.listener = listener; }
    public boolean isRunning() { return running; }
    public void setMuted(boolean value) { muted = value; }
    public void setGain(float value) { gain = Math.max(0.1f, Math.min(3f, value)); }

    public void setEffects(EffectSettings value) { effects = value; }

    public void start(AudioDeviceInfo input, AudioDeviceInfo output) {
        if (thread != null && thread.isAlive()) return;
        muted = true;
        running = true;
        stopReason = "Stopped";
        thread = new Thread(() -> pump(input, output), "MicBridge-audio");
        thread.start();
    }

    public void stop(String reason) {
        stopReason = reason;
        muted = true;
        running = false;
        // Stop recording to unblock a pending read; pause output to unblock writes.
        synchronized (resources) {
            if (recorder != null) try { recorder.stop(); } catch (IllegalStateException ignored) { }
            if (player != null) try { player.pause(); player.flush(); } catch (IllegalStateException ignored) { }
        }
    }

    private void open(AudioDeviceInfo input, AudioDeviceInfo output, int rate) {
        int inMin = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int outMin = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        if (inMin <= 0 || outMin <= 0) throw new IllegalStateException("Unsupported sample rate");
        synchronized (resources) {
            try {
                recorder = new AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                    .setBufferSizeInBytes(Math.max(inMin * 2, rate / 50 * 2)).build();
                player = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .setBufferSizeInBytes(Math.max(outMin, rate / 25 * 4)).build();
                if (recorder.getState() != AudioRecord.STATE_INITIALIZED || player.getState() != AudioTrack.STATE_INITIALIZED)
                    throw new IllegalStateException("Audio device could not initialise");
                if (!recorder.setPreferredDevice(input) || !player.setPreferredDevice(output))
                    throw new IllegalStateException("Selected device is no longer available");
            } catch (RuntimeException e) { releaseLocked(); throw e; }
        }
    }

    private void pump(AudioDeviceInfo input, AudioDeviceInfo output) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
        try {
            int rate = 48000;
            try { open(input, output, rate); }
            catch (IllegalArgumentException | IllegalStateException first) { rate = 44100; open(input, output, rate); }
            if (!running) return;
            synchronized (resources) {
                if (!running) return;
                recorder.startRecording();
                player.play();
            }
            short[] capture = new short[rate / 100];
            short[] playback = new short[capture.length * 2];
            SignalProcessor dsp = new SignalProcessor(rate);
            boolean verified = false;
            long deadline = SystemClock.elapsedRealtime() + 3000;
            long lastMeter = 0;
            while (running) {
                int count = recorder.read(capture, 0, capture.length, AudioRecord.READ_BLOCKING);
                if (!running) break;
                if (count <= 0) throw new IllegalStateException("Microphone interrupted (" + count + ")");
                AudioDeviceInfo actualIn = recorder.getRoutedDevice();
                AudioDeviceInfo actualOut = player.getRoutedDevice();
                boolean correct = actualIn != null && actualOut != null
                    && actualIn.getId() == input.getId() && actualOut.getId() == output.getId();
                if (verified && !correct) throw new IllegalStateException("Audio route changed. Stopped to prevent fallback playback.");
                if (!verified && correct) {
                    verified = true;
                    String route = deviceName(actualIn) + " → " + deviceName(actualOut) + "\n" + rate + " Hz • stereo output";
                    main.post(() -> { if (running) listener.onReady(route); });
                }
                if (!verified && SystemClock.elapsedRealtime() > deadline)
                    throw new IllegalStateException("Selected audio route unavailable. Check microphone and media output, then retry.");
                float peak = dsp.process(capture, count, playback, gain, muted || !verified, effects);
                int offset = 0;
                while (running && offset < count * 2) {
                    int written = player.write(playback, offset, count * 2 - offset, AudioTrack.WRITE_BLOCKING);
                    if (written <= 0) { if (!running) break; throw new IllegalStateException("Speaker interrupted (" + written + ")"); }
                    offset += written;
                }
                long now = SystemClock.elapsedRealtime();
                if (now - lastMeter >= 100) {
                    lastMeter = now;
                    main.post(() -> { if (running) listener.onMeter(peak); });
                }
            }
        } catch (RuntimeException e) {
            if (running) stopReason = e.getMessage() == null ? "Audio unavailable" : e.getMessage();
        } finally {
            running = false;
            muted = true;
            synchronized (resources) { releaseLocked(); }
            String result = stopReason;
            main.post(() -> listener.onStopped(result));
        }
    }

    private void releaseLocked() {
        if (recorder != null) {
            try { recorder.stop(); } catch (IllegalStateException ignored) { }
            recorder.release(); recorder = null;
        }
        if (player != null) {
            try { player.pause(); player.flush(); } catch (IllegalStateException ignored) { }
            player.release(); player = null;
        }
    }

    public static String deviceName(AudioDeviceInfo device) {
        String name = device.getProductName().toString().trim();
        if(device.getType()==AudioDeviceInfo.TYPE_BUILTIN_MIC) return "Phone microphone";
        return name.isEmpty() ? "Audio device" : name;
    }
}
