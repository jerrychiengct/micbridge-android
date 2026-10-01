package com.jerry.micbridge;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity implements AudioEngine.Listener {
    private static final int BG = Color.rgb(11,20,28), CARD = Color.rgb(22,35,46);
    private static final int WHITE = Color.rgb(236,245,249), MUTED = Color.rgb(160,181,196), TEAL = Color.rgb(85,222,196);
    private AudioManager audio;
    private AudioEngine engine;
    private Spinner input, output;
    private final List<AudioDeviceInfo> inputs = new ArrayList<>(), outputs = new ArrayList<>();
    private Button start, mute, refresh;
    private TextView status, route, level, gainLabel;
    private ProgressBar meter;
    private boolean active, ready, isMuted = true, destroyed;
    private int liveInputId = -1, liveOutputId = -1;
    private AudioFocusRequest focus;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AudioDeviceCallback devices = new AudioDeviceCallback() {
        @Override public void onAudioDevicesAdded(AudioDeviceInfo[] added) { if (!active) refreshDevices(); }
        @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removed) {
            for (AudioDeviceInfo d : removed)
                if (active && (d.getId() == liveInputId || d.getId() == liveOutputId)) stopAudio("Device disconnected");
            if (!active) refreshDevices();
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        engine = new AudioEngine(this);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        buildUi();
        audio.registerAudioDeviceCallback(devices, handler);
        ensurePermissions();
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size, int colour) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(colour);
        view.setPadding(0,dp(5),0,dp(5)); return view;
    }
    private GradientDrawable background(int colour, int radius) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(colour); shape.setCornerRadius(dp(radius)); return shape;
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(18),dp(14),dp(18),dp(14));
        c.setBackground(background(CARD,20));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin = dp(16); parent.addView(c,lp); return c;
    }
    private Button button(String label, int colour) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(16);
        b.setTextColor(colour == TEAL ? BG : WHITE); b.setBackground(background(colour,14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,dp(54)); lp.topMargin = dp(12); b.setLayoutParams(lp); return b;
    }
    private void buildUi() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(22),dp(18),dp(22),dp(22));
        scroll.addView(root); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            root.setPadding(dp(22),dp(18)+insets.getSystemWindowInsetTop(),dp(22),dp(22)+insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.addView(text("LIVE VOICE • TEST BUILD 0.1",12,TEAL));
        TextView title = text("MicBridge",36,WHITE); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(title);
        TextView sub = text("Your microphone. Your speaker.",16,MUTED); sub.setPadding(0,0,0,dp(24)); root.addView(sub);

        LinearLayout deviceCard = card(root);
        deviceCard.addView(text("01  CONNECTIONS",12,TEAL));
        deviceCard.addView(text("USB microphone",16,WHITE)); input = new Spinner(this); deviceCard.addView(input,new LinearLayout.LayoutParams(-1,dp(50)));
        deviceCard.addView(text("Bluetooth speaker",16,WHITE)); output = new Spinner(this); deviceCard.addView(output,new LinearLayout.LayoutParams(-1,dp(50)));
        refresh = button("Refresh devices",Color.rgb(40,58,71)); deviceCard.addView(refresh); refresh.setOnClickListener(v -> ensurePermissions());
        Button settings = button("Open Bluetooth settings",Color.rgb(40,58,71)); deviceCard.addView(settings);
        settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));

        LinearLayout liveCard = card(root);
        liveCard.addView(text("02  LIVE AUDIO",12,TEAL));
        status = text("Connect your USB receiver and pair the speaker.",18,WHITE); liveCard.addView(status);
        route = text("No active audio route",13,MUTED); liveCard.addView(route);
        meter = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); meter.setMax(100); liveCard.addView(meter,new LinearLayout.LayoutParams(-1,dp(18)));
        level = text("Microphone level: —",13,MUTED); liveCard.addView(level);
        gainLabel = text("Microphone gain  1.0×",16,WHITE); liveCard.addView(gainLabel);
        SeekBar gain = new SeekBar(this); gain.setMax(290); gain.setProgress(90); liveCard.addView(gain);
        gain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int value,boolean user) {
                float g = 0.1f+value/100f; engine.setGain(g); gainLabel.setText(String.format(Locale.UK,"Microphone gain  %.1f×",g));
            }
            public void onStartTrackingTouch(SeekBar s) { }
            public void onStopTrackingTouch(SeekBar s) { }
        });
        start = button("Start microphone",TEAL); liveCard.addView(start); start.setOnClickListener(v -> { if (active) stopAudio("Stopped"); else startAudio(); });
        mute = button("Unmute speaker",Color.rgb(40,58,71)); mute.setEnabled(false); liveCard.addView(mute);
        mute.setOnClickListener(v -> {
            if (!ready) return;
            isMuted = !isMuted; engine.setMuted(isMuted);
            mute.setText(isMuted ? "Unmute speaker" : "Mute speaker");
            status.setText(isMuted ? "Muted • microphone is listening" : "Live • voice to speaker");
        });

        LinearLayout help = card(root);
        help.addView(text("FIRST TEST",12,TEAL));
        help.addView(text("1. Plug in the ONSMO USB-C receiver.\n2. Pair the Tribit and enable media audio.\n3. Select both devices, then tap Start.\n4. Turn speaker volume low, then Unmute.",15,WHITE));
        help.addView(text("Bluetooth adds delay. Keep the speaker away from the microphone to reduce feedback. The limiter reduces digital clipping; it cannot prevent acoustic feedback.\n\nAudio stops when you leave this app. No recording, internet access or saved audio. Use your phone’s volume buttons for speaker volume.",13,MUTED));
        root.addView(text("Created for Jerry • USB → Bluetooth prototype",12,MUTED));
    }

    private boolean hasPermissions() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            && (Build.VERSION.SDK_INT < 31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED);
    }
    private void ensurePermissions() {
        List<String> missing = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.BLUETOOTH_CONNECT);
        if (!missing.isEmpty()) requestPermissions(missing.toArray(new String[0]),1); else refreshDevices();
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants) {
        super.onRequestPermissionsResult(code,permissions,grants);
        if (hasPermissions()) refreshDevices();
        else { status.setText("Allow Microphone and Nearby devices permissions. If blocked, enable them in Android app settings."); start.setEnabled(false); }
    }
    private void refreshDevices() {
        if (destroyed || active || !hasPermissions()) return;
        int oldIn = selectedId(input,inputs), oldOut = selectedId(output,outputs);
        inputs.clear(); outputs.clear();
        try {
            for (AudioDeviceInfo d : audio.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
                int t = d.getType();
                if (t == AudioDeviceInfo.TYPE_USB_DEVICE || t == AudioDeviceInfo.TYPE_USB_HEADSET || t == AudioDeviceInfo.TYPE_USB_ACCESSORY) inputs.add(d);
            }
            for (AudioDeviceInfo d : audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                int t = d.getType();
                if (t == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || (Build.VERSION.SDK_INT >= 31 && t == AudioDeviceInfo.TYPE_BLE_SPEAKER)) outputs.add(d);
            }
            populate(input,inputs,oldIn,"No USB microphone detected"); populate(output,outputs,oldOut,"No Bluetooth media speaker detected");
            start.setEnabled(!inputs.isEmpty() && !outputs.isEmpty());
        } catch (SecurityException e) { status.setText("Nearby devices permission is required. Tap Refresh."); start.setEnabled(false); }
    }
    private int selectedId(Spinner s,List<AudioDeviceInfo> list) {
        int i = s.getSelectedItemPosition(); return i >= 0 && i < list.size() ? list.get(i).getId() : -1;
    }
    private void populate(Spinner spinner,List<AudioDeviceInfo> list,int previous,String empty) {
        List<String> names = new ArrayList<>(); int selected = 0;
        for (int i=0;i<list.size();i++) { names.add(AudioEngine.deviceName(list.get(i))); if (list.get(i).getId()==previous) selected=i; }
        if (names.isEmpty()) names.add(empty);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter); spinner.setSelection(selected);
    }
    private void startAudio() {
        if (!hasPermissions()) { ensurePermissions(); return; }
        int in = input.getSelectedItemPosition(), out = output.getSelectedItemPosition();
        if (in < 0 || in >= inputs.size() || out < 0 || out >= outputs.size()) { refreshDevices(); return; }
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener(change -> { if (change != AudioManager.AUDIOFOCUS_GAIN && active) stopAudio("Paused by another audio app or a call"); },handler)
            .setWillPauseWhenDucked(true).build();
        if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { status.setText("Audio is busy. Stop other audio apps and retry."); focus=null; return; }
        active = true; ready = false; isMuted = true;
        liveInputId=inputs.get(in).getId(); liveOutputId=outputs.get(out).getId();
        input.setEnabled(false); output.setEnabled(false); refresh.setEnabled(false);
        start.setText("Stop microphone"); mute.setEnabled(false); mute.setText("Unmute speaker");
        status.setText("Checking audio route • muted"); route.setText("Waiting for USB input and Bluetooth output…");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        engine.start(inputs.get(in),outputs.get(out));
    }
    private void stopAudio(String reason) {
        ready=false; mute.setEnabled(false); start.setEnabled(false); status.setText("Stopping…");
        engine.stop(reason);
    }
    @Override public void onReady(String actualRoute) {
        if (destroyed || !active || !engine.isRunning()) return;
        ready=true; route.setText(actualRoute); status.setText("Ready • muted"); mute.setEnabled(true);
    }
    @Override public void onMeter(float peak) {
        if (destroyed || !active) return;
        float db = peak > 0 ? (float)(20*Math.log10(peak)) : -60;
        meter.setProgress(Math.round(Math.max(0,Math.min(100,(db+60)/60*100))));
        level.setText(String.format(Locale.UK,"Microphone peak: %.0f dBFS%s",Math.max(-60,db),peak>0.98f ? " • input clipping" : ""));
    }
    @Override public void onStopped(String reason) {
        if (focus != null) { audio.abandonAudioFocusRequest(focus); focus=null; }
        active=false; ready=false; isMuted=true;
        if (destroyed) return;
        status.setText(reason); route.setText("No active audio route"); meter.setProgress(0); level.setText("Microphone level: —");
        start.setText("Start microphone"); mute.setText("Unmute speaker"); mute.setEnabled(false);
        input.setEnabled(true); output.setEnabled(true); refresh.setEnabled(true);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); refreshDevices();
    }
    @Override protected void onResume() { super.onResume(); if (audio!=null && !active) refreshDevices(); }
    @Override protected void onStop() { if (active) stopAudio("Stopped when app left the screen"); super.onStop(); }
    @Override protected void onDestroy() {
        destroyed=true; engine.stop("App closed"); audio.unregisterAudioDeviceCallback(devices);
        if (focus!=null) { audio.abandonAudioFocusRequest(focus); focus=null; }
        super.onDestroy();
    }
}
