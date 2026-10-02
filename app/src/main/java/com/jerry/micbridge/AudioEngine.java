package com.jerry.micbridge;

import android.media.*;
import android.os.*;

/** Foreground-only live audio. Selected routes must be verified before voice playback. */
public final class AudioEngine {
    public interface Listener {
        void onReady(String route);
        void onMeter(float peak);
        void onStopped(String reason);
        void onAudioHealth(String details);
        void onCalibration(float noiseRms);
    }
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private final Object resources = new Object();
    private volatile boolean running;
    private volatile boolean muted = true;
    private volatile float gain = 1f;
    private boolean preferFast=true;
    private volatile boolean calibrationRequested;
    public void setPreferFast(boolean fast){if(!running)preferFast=fast;}
    public void calibrate(){if(running){muted=true;calibrationRequested=true;}}
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

    public boolean start(AudioDeviceInfo input, AudioDeviceInfo output) {
        if (thread != null && thread.isAlive()) return false;
        muted = true;
        calibrationRequested=false;
        running = true;
        stopReason = "Stopped";
        thread = new Thread(() -> pump(input, output), "MicBridge-audio");
        thread.start();
        return true;
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

    private void open(AudioDeviceInfo input, AudioDeviceInfo output, int rate,int channels) {
        int inputMask=channels==2?AudioFormat.CHANNEL_IN_STEREO:AudioFormat.CHANNEL_IN_MONO;
        int inMin = AudioRecord.getMinBufferSize(rate, inputMask, AudioFormat.ENCODING_PCM_16BIT);
        int outMin = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        if (inMin <= 0 || outMin <= 0) throw new IllegalStateException("Unsupported sample rate");
        synchronized (resources) {
            try {
                recorder = new AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate)
                        .setChannelMask(inputMask).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                    .setBufferSizeInBytes(Math.max(inMin*(preferFast?1:2), AudioRoutePlan.blockFrames(rate,preferFast)*channels*2*2)).build();
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
                player.setBufferSizeInFrames(AudioRoutePlan.bufferFrames(rate,preferFast));
            } catch (RuntimeException e) { releaseLocked(); throw e; }
        }
    }

    private void pump(AudioDeviceInfo input, AudioDeviceInfo output) {
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            int rate=0,channels=1;RuntimeException last=null;
            int preferredChannels=1;int[] advertised=input.getChannelCounts();boolean mono=false,stereo=false;
            for(int c:advertised){mono|=c==1;stereo|=c==2;}if(stereo&&!mono)preferredChannels=2;
            outer:for(int candidate:AudioRoutePlan.rates(input.getSampleRates(),output.getSampleRates())) {
                for(int attempt=0;attempt<2;attempt++) {
                    if(!running)return;int count=attempt==0?preferredChannels:3-preferredChannels;
                    try{open(input,output,candidate,count);rate=candidate;channels=count;break outer;}
                    catch(IllegalArgumentException|IllegalStateException failure){last=failure;}
                }
            }
            if(rate==0)throw new IllegalStateException("Microphone format unavailable. Use an Android-compatible USB / headset adapter.",last);
            if (!running) return;
            synchronized (resources) {
                if (!running) return;
                recorder.startRecording();
                player.play();
            }
            int block=AudioRoutePlan.blockFrames(rate,preferFast);
            short[] capture=new short[block*channels],monoCapture=new short[block],playback=new short[block*2];
            SignalProcessor dsp = new SignalProcessor(rate);
            boolean verified = false;
            long deadline = SystemClock.elapsedRealtime() + 3000;
            long lastMeter=0,lastHealth=SystemClock.elapsedRealtime();int lastUnderruns=player.getUnderrunCount();
            boolean calibrating=false;long calibrationFrames=0;double calibrationPower=0;
            while (running) {
                int samples = recorder.read(capture, 0, capture.length, AudioRecord.READ_BLOCKING);
                if (!running) break;
                if(samples<=0)throw new IllegalStateException("Microphone interrupted ("+samples+")");
                int count=AudioRoutePlan.downmix(capture,samples,channels,monoCapture);
                AudioDeviceInfo actualIn = recorder.getRoutedDevice();
                AudioDeviceInfo actualOut = player.getRoutedDevice();
                boolean correct = actualIn != null && actualOut != null
                    && actualIn.getId() == input.getId() && actualOut.getId() == output.getId();
                if (verified && !correct) throw new IllegalStateException("Audio route changed. Stopped to prevent fallback playback.");
                if (!verified && correct) {
                    verified = true;
                    String route = deviceName(actualIn) + " → " + deviceName(actualOut) + "\n" + rate + " Hz • " + (channels==2?"stereo mic mixed to mono":"mono mic") + " • stereo output";
                    main.post(() -> { if (running) listener.onReady(route); });
                }
                if (!verified && SystemClock.elapsedRealtime() > deadline)
                    throw new IllegalStateException("Selected audio route unavailable. Check microphone and media output, then retry.");
                if(calibrationRequested&&verified){calibrationRequested=false;calibrating=true;calibrationFrames=0;calibrationPower=0;}
                if(calibrating){
                    for(int n=0;n<count;n++){double value=monoCapture[n]/32768.0;calibrationPower+=value*value;}calibrationFrames+=count;
                    if(calibrationFrames>=rate*3L/2){float noise=(float)Math.sqrt(calibrationPower/calibrationFrames);dsp.setNoiseFloor(noise);calibrating=false;final float measured=noise;main.post(()->{if(running)listener.onCalibration(measured);});}
                }
                float peak=dsp.process(monoCapture,count,playback,gain,muted||!verified||calibrating,effects);
                int offset = 0;
                while (running && offset < count * 2) {
                    int written = player.write(playback, offset, count * 2 - offset, AudioTrack.WRITE_BLOCKING);
                    if (written <= 0) { if (!running) break; throw new IllegalStateException("Speaker interrupted (" + written + ")"); }
                    offset += written;
                }
                long now = SystemClock.elapsedRealtime();
                if(now-lastHealth>=1000){
                    lastHealth=now;int underruns=player.getUnderrunCount();
                    if(preferFast&&underruns>lastUnderruns)player.setBufferSizeInFrames(AudioRoutePlan.growBuffer(player.getBufferSizeInFrames(),block,player.getBufferCapacityInFrames()));
                    lastUnderruns=underruns;
                    String health="Processing blocks: "+(preferFast?5:10)+" ms • app output buffer: "+Math.round(player.getBufferSizeInFrames()*1000f/rate)+" ms\n"+
                        "Output underruns: "+underruns+" • Android fast path: "+(player.getPerformanceMode()==AudioTrack.PERFORMANCE_MODE_LOW_LATENCY?"active":"unavailable")+"\nBluetooth and hardware delay are additional; this is not total measured latency.";
                    main.post(()->{if(running)listener.onAudioHealth(health);});
                }
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
        CharSequence product=device.getProductName();
        String name = product==null?"":product.toString().trim();
        if(device.getType()==AudioDeviceInfo.TYPE_BUILTIN_MIC) return "Phone microphone";
        if(device.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) return "Phone speaker";
        if(device.getType()==AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) return "Phone earpiece";
        return name.isEmpty() ? "Audio device" : name;
    }
}
