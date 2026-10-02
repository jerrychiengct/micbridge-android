package com.jerry.micbridge;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity implements AudioEngine.Listener {
    private static final int BG=0xFFF3F6F7, PAPER=Color.WHITE, INK=0xFF142D36, SUB=0xFF58717A, TEAL=0xFF087F75, PALE=0xFFE4F2EF;
    private AudioManager audio; private AudioEngine engine; private SharedPreferences prefs;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Map<Integer,AudioDeviceInfo> inputs=Collections.emptyMap(),outputs=Collections.emptyMap();
    private boolean refreshingDevices;
    private final Runnable deviceRefresh=()->refreshDevices();
    private void scheduleDeviceRefresh(){handler.removeCallbacks(deviceRefresh);handler.postDelayed(deviceRefresh,150);}
    private Spinner input,output;
    private Button start,mute,refresh,calibrate; private final Button[] tabs=new Button[4], presets=new Button[VoicePresets.TITLES.length];
    private TextView status,route,level,connectionCount,presetName,fxSummary,audioHealth,calibrationStatus;
    private MeterView meter;
    private final LinearLayout[] pages=new LinearLayout[4]; private LinearLayout catalogCards;
    private ScrollView scroll; private Switch bypass,clarity,speech,autoLevel,fast;
    private Slider gain,pitch,tone,bass,mid,treble,growl,robot,echo,delay,noise,deEss;
    private boolean active,ready,calibrating,isMuted=true,destroyed,changing;
    private int liveInputId=-1,liveOutputId=-1,presetIndex=6;
    private AudioFocusRequest focus;
    private final String[] presetTitles=VoicePresets.TITLES,presetHints=VoicePresets.HINTS;
    private final AudioDeviceCallback devices=new AudioDeviceCallback() {
        @Override public void onAudioDevicesAdded(AudioDeviceInfo[] added) { if(!active) scheduleDeviceRefresh(); }
        @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removed) {
            for(AudioDeviceInfo d:removed) if(active&&(d.getId()==liveInputId||d.getId()==liveOutputId)) stopAudio("Device disconnected");
            if(!active) scheduleDeviceRefresh();
        }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        audio=(AudioManager)getSystemService(AUDIO_SERVICE); prefs=getSharedPreferences("studio",MODE_PRIVATE);
        engine=new AudioEngine(this); setVolumeControlStream(AudioManager.STREAM_MUSIC);
        buildUi(); restoreSettings(); audio.registerAudioDeviceCallback(devices,handler); ensurePermissions();
    }
    private int dp(int v) { return Math.round(v*getResources().getDisplayMetrics().density); }
    private TextView text(String s,int size,int color) {
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(4),0,dp(4));return t;
    }
    private TextView heading(String s,int size) { TextView t=text(s,size,INK);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t; }
    private GradientDrawable shape(int color,int radius) { GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d; }
    private LinearLayout vertical() { LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l; }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout l=vertical();l.setPadding(dp(18),dp(16),dp(18),dp(16));l.setBackground(shape(PAPER,22));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);parent.addView(l,p);return l;
    }
    private Button button(String label,int color) {
        Button b=new Button(this);b.setText(label);b.setTextSize(14);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextColor(color==TEAL?Color.WHITE:INK);b.setBackground(shape(color,14));b.setMinWidth(0);b.setMinimumWidth(0);
        b.setMinHeight(dp(54));b.setPadding(dp(10),dp(5),dp(10),dp(5));b.setStateListAnimator(null);return b;
    }
    private void enabled(View view,boolean value){view.setEnabled(value);view.setAlpha(value?1f:0.45f);}
    private void fullButton(LinearLayout p,Button b) { b.setMinHeight(dp(50)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(10);p.addView(b,lp); }
    private void buildUi() {
        LinearLayout root=vertical();root.setBackgroundColor(BG);setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            int top,bottom,left,right;
            if(Build.VERSION.SDK_INT>=30) { Insets n=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());top=n.top;bottom=n.bottom;left=n.left;right=n.right; }
            else { top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();left=insets.getSystemWindowInsetLeft();right=insets.getSystemWindowInsetRight(); }
            root.setPadding(left,top,right,bottom);return insets;
        });
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(16),dp(10),dp(16),dp(10));
        ImageView logo=new ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);logo.setContentDescription("MicBridge logo");LinearLayout.LayoutParams logoSize=new LinearLayout.LayoutParams(dp(36),dp(36));logoSize.rightMargin=dp(12);header.addView(logo,logoSize);
        LinearLayout brand=vertical();brand.addView(heading("MicBridge",24));brand.addView(text("VOICE TO SPEAKER",10,SUB));header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView beta=text("BETA",11,TEAL);beta.setTypeface(Typeface.DEFAULT,Typeface.BOLD);beta.setPadding(dp(12),dp(8),dp(12),dp(8));beta.setBackground(shape(PALE,30));header.addView(beta);root.addView(header);
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setPadding(dp(18),dp(8),dp(18),dp(8));
        LinearLayout content=vertical();scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<4;i++) { pages[i]=vertical();content.addView(pages[i]); }
        buildLive(pages[0]);buildStudio(pages[1]);buildSpeakers(pages[2]);buildAbout(pages[3]);
        LinearLayout dock=vertical();dock.setBackgroundColor(PAPER);dock.setPadding(dp(20),dp(8),dp(20),0);
        status=text("Connect a microphone and speaker",13,SUB);status.setMaxLines(3);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);dock.addView(status);
        LinearLayout actions=new LinearLayout(this);start=button("Start session",TEAL);enabled(start,false);mute=button("Unmute",PALE);enabled(mute,false);
        LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,-2,1);a.setMargins(0,dp(4),dp(8),dp(6));actions.addView(start,a);
        LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,-2,1);b.setMargins(dp(8),dp(4),0,dp(6));actions.addView(mute,b);dock.addView(actions);
        start.setOnClickListener(v->{if(active)stopAudio("Session stopped");else startAudio();});
        mute.setOnClickListener(v->toggleMute());
        LinearLayout nav=new LinearLayout(this);
        String[] labels={"Live","Studio","Speakers","About"};
        for(int i=0;i<4;i++) { final int n=i;tabs[i]=button(labels[i],PAPER);tabs[i].setTextSize(12);nav.addView(tabs[i],new LinearLayout.LayoutParams(0,-2,1));tabs[i].setOnClickListener(v->showPage(n)); }
        dock.addView(nav);root.addView(dock);showPage(0);
    }
    private void buildLive(LinearLayout page) {
        LinearLayout hero=card(page);hero.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF142D36,0xFF235154}));
        hero.addView(text("LIVE MONITOR",11,0xFF91DCCE));
        TextView h=text("Your voice,\namplified.",30,Color.WHITE);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);hero.addView(h);
        meter=new MeterView(this);hero.addView(meter,new LinearLayout.LayoutParams(-1,dp(74)));
        level=text("Microphone level  —",13,0xFFB5D7D9);hero.addView(level);
        route=text("Start muted. Unmute when you are ready.",12,0xFFB5D7D9);hero.addView(route);
        LinearLayout connections=card(page);connections.addView(heading("Your connection",20));connectionCount=text("Built-in or connected mic + your audio output",12,SUB);connections.addView(connectionCount);
        connections.addView(text("MICROPHONE INPUT",11,TEAL));input=new Spinner(this);input.setContentDescription("Choose microphone input");input.setMinimumHeight(dp(52));connections.addView(input,new LinearLayout.LayoutParams(-1,-2));
        connections.addView(text("SPEAKER OUTPUT",11,TEAL));output=new Spinner(this);output.setContentDescription("Choose speaker output");output.setMinimumHeight(dp(52));connections.addView(output,new LinearLayout.LayoutParams(-1,-2));
        connections.addView(text("Only connected media routes appear here. Hands-free Bluetooth microphones and phone earpieces are excluded. Same-name ports are numbered; Bluetooth media and LE Audio are different routes. Android must verify your choice before Unmute.",12,SUB));
        refresh=button("Refresh connected devices",PALE);fullButton(connections,refresh);refresh.setOnClickListener(v->ensurePermissions());
        Button pair=button("Pair a Bluetooth speaker",BG);fullButton(connections,pair);pair.setOnClickListener(v->openExternal(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        fast=new Switch(this);fast.setText("Prefer shorter latency buffers");fast.setTextColor(INK);fast.setTextSize(14);fast.setChecked(true);connections.addView(fast);
        fast.setOnCheckedChangeListener((b,on)->engine.setPreferFast(on));
        connections.addView(text("Fast uses 5 ms processing blocks and requests a smaller output buffer. If audio crackles, stop and switch this off for 10 ms blocks. Bluetooth delay remains device-dependent.",12,SUB));
        audioHealth=text("Buffer diagnostics appear during a session.",12,SUB);connections.addView(audioHealth);
        gain=new Slider(connections,"Microphone gain",290,90,p->String.format(Locale.UK,"%.1f×",0.1f+p/100f));
        gain.setOnChange(()->{engine.setGain(0.1f+gain.value()/100f);});
        LinearLayout quality=card(page);quality.addView(heading("Voice care for any mic",19));
        quality.addView(text("Use an Android-compatible USB / wired microphone or Phone microphone. Lecture adds gentle noise reduction, de-essing and bounded level assist. Processing improves clarity but cannot repair a distorted transmitter or restore missing detail.",13,SUB));
        calibrate=button("Calibrate background noise",PALE);fullButton(quality,calibrate);enabled(calibrate,false);
        calibrationStatus=text("Start a session, stay muted, then remain quiet for 1.5 seconds during calibration. Recalibrate after changing mic or room.",12,SUB);quality.addView(calibrationStatus);
        calibrate.setOnClickListener(v->{if(ready&&!calibrating){calibrating=true;isMuted=true;engine.setMuted(true);engine.calibrate();enabled(mute,false);enabled(calibrate,false);calibrationStatus.setText("Calibrating… please stay quiet for 1.5 seconds.");updateLiveState();}});
        LinearLayout guide=card(page);guide.addView(heading("A good first test",18));
        guide.addView(text("Choose the phone microphone or a connected USB / wired mic, then select your speaker. For speeches and lectures, choose Lecture in Voice studio. Begin at low volume and keep the speaker away from the microphone.",14,SUB));
        Button lectureButton=button("Use Lecture preset",PALE);fullButton(guide,lectureButton);lectureButton.setOnClickListener(v->{applyPreset(6);showPage(1);});
        guide.addView(text("Keep this app open. Bluetooth adds delay; voice effects can add more. No audio is saved.",12,SUB));
    }
    private void buildStudio(LinearLayout page) {
        page.addView(heading("Shape your voice",27));page.addView(text("Choose a preset, then make it yours.",14,SUB));
        LinearLayout choices=card(page);presetName=heading("Natural",20);choices.addView(presetName);
        for(int row=0;row<presetTitles.length/2;row++) { LinearLayout line=new LinearLayout(this);
            if(row==4){choices.addView(heading("Studio collection",18));choices.addView(text("Original tunings inspired by professional voice chains. No paid plug-ins required.",12,SUB));}
            for(int col=0;col<2;col++) { final int index=row*2+col;Button p=button(presetTitles[index]+"\n"+presetHints[index],BG);p.setTextSize(12);presets[index]=p;
                p.setMinHeight(dp(68));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(col==0?0:dp(5),dp(8),col==0?dp(5):0,0);line.addView(p,lp);p.setOnClickListener(v->applyPreset(index)); }
            choices.addView(line);
        }
        LinearLayout controls=card(page);controls.addView(heading("Sound modulator",20));
        bypass=new Switch(this);bypass.setText("Voice effects enabled");bypass.setTextColor(INK);bypass.setTextSize(14);bypass.setChecked(true);controls.addView(bypass);
        pitch=new Slider(controls,"Pitch",12,6,p->String.format(Locale.UK,"≈ %+d semitones",p-6));
        tone=new Slider(controls,"Tone",12,6,p->p==6?"Balanced":(p<6?"Warm ":"Bright ")+Math.abs(p-6)+" dB");
        controls.addView(heading("Voice equaliser",18));
        bass=new Slider(controls,"Bass · 180 Hz",24,12,p->String.format(Locale.UK,"%+d dB",p-12));
        mid=new Slider(controls,"Mid · 1.5 kHz",24,12,p->String.format(Locale.UK,"%+d dB",p-12));
        treble=new Slider(controls,"Treble · 4 kHz",24,12,p->String.format(Locale.UK,"%+d dB",p-12));
        controls.addView(heading("Creative effects",18));
        growl=new Slider(controls,"Grit / growl",100,0,p->p+"%");
        robot=new Slider(controls,"Robot texture",100,0,p->p+"%");
        echo=new Slider(controls,"Echo blend",50,0,p->p+"%");
        delay=new Slider(controls,"Echo timing",500,150,p->(p+100)+" ms");
        clarity=new Switch(this);clarity.setText("Reduce low-frequency rumble");clarity.setTextColor(INK);clarity.setTextSize(14);controls.addView(clarity);
        speech=new Switch(this);speech.setText("Level speech dynamics");speech.setTextColor(INK);speech.setTextSize(14);controls.addView(speech);
        controls.addView(heading("Voice care",18));
        autoLevel=new Switch(this);autoLevel.setText("Assist quiet voices · up to +6 dB");autoLevel.setTextColor(INK);autoLevel.setTextSize(14);controls.addView(autoLevel);
        noise=new Slider(controls,"Soft noise reduction",100,0,p->p+"%");
        deEss=new Slider(controls,"Tame sharp S sounds",100,0,p->p+"%");
        Runnable change=()->{if(!changing){presetIndex=-1;updateEffects();}};
        noise.setOnChange(change);deEss.setOnChange(change);autoLevel.setOnCheckedChangeListener((b,on)->change.run());pitch.setOnChange(change);tone.setOnChange(change);bass.setOnChange(change);mid.setOnChange(change);treble.setOnChange(change);growl.setOnChange(change);robot.setOnChange(change);echo.setOnChange(change);delay.setOnChange(change);
        bypass.setOnCheckedChangeListener((b,on)->{if(!changing)updateEffects();});clarity.setOnCheckedChangeListener((b,on)->change.run());speech.setOnCheckedChangeListener((b,on)->change.run());
        fxSummary=text("Natural voice • effects add no intentional delay",12,SUB);controls.addView(fxSummary);
        Button reset=button("Reset to natural voice",PALE);fullButton(controls,reset);reset.setOnClickListener(v->applyPreset(0));
        page.addView(text("Lecture keeps your natural pitch, removes rumble and gently levels louder speech. Vigilante is a Batman-inspired creative effect, not an exact character or actor voice. Pitch is an approximate beta effect. The limiter reduces digital clipping; it cannot prevent feedback.",12,SUB));
    }
    private void buildSpeakers(LinearLayout page) {
        page.addView(heading("Find your speaker",27));page.addView(text("Generic speakers and 12 named brands.",14,SUB));
        LinearLayout intro=card(page);intro.addView(heading("Xiaomi, Tribit and beyond",19));
        intro.addView(text("Pair in Android settings, enable media audio, then choose the connected speaker on Live. MicBridge works by connection type, not by brand.",14,SUB));
        intro.addView(text("This guide lists examples from manufacturer pages. It is not an exhaustive market list or a list of device-tested models. Malaysian stock and availability vary.",12,SUB));
        LinearLayout generic=card(page);generic.addView(heading("Generic Bluetooth speaker",19));generic.addView(text("No brand name needed. Pair in Android Bluetooth settings, enable Media audio, then select the speaker on Live. Labels such as BT Speaker, Wireless Speaker or a model code are normal. Listing it here does not add a driver; Android must expose a working audio output.",13,SUB));
        Button genericHelp=button("Open Bluetooth settings",PALE);fullButton(generic,genericHelp);genericHelp.setOnClickListener(v->openExternal(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(15);search.setTextColor(INK);search.setHintTextColor(SUB);search.setHint("Search brand or model");search.setPadding(dp(16),dp(10),dp(16),dp(10));search.setBackground(shape(PAPER,14));page.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));
        catalogCards=vertical();LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(14);page.addView(catalogCards,lp);renderCatalog("");
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){renderCatalog(s.toString());}public void afterTextChanged(Editable e){}});
        page.addView(text("Other brands are welcome if Android exposes a Bluetooth media route. Wi-Fi-only speakers and proprietary wireless dongles need a separate audio connection. Speaker stereo/party linking is managed by the manufacturer.",12,SUB));
    }
    private void renderCatalog(String query) {
        catalogCards.removeAllViews();int found=0;String q=query.toLowerCase(Locale.ROOT).trim();
        for(String[] item:SpeakerCatalog.BRANDS) {
            if(!(item[0]+" "+item[1]).toLowerCase(Locale.ROOT).contains(q))continue;found++;
            LinearLayout c=card(catalogCards);c.addView(heading(item[0],19));c.addView(text(item[1],13,SUB));c.addView(text("Bluetooth media • hardware testing required",11,TEAL));
            Button help=button("Connection guide",BG);fullButton(c,help);
            help.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(item[0]+" connection")
                .setMessage("1. Put the speaker into Bluetooth pairing mode.\n2. Pair it in Android settings and enable media audio.\n3. Return to MicBridge, refresh and select it on Live.\n\nFor devices showing both normal and LE names, use the normal media-audio entry. Bluetooth Low Energy control alone is not LE Audio playback.\n\nOpening a manufacturer page leaves this app and stops any live session.")
                .setPositiveButton("Manufacturer page",(d,w)->openExternal(new Intent(Intent.ACTION_VIEW,Uri.parse(item[2])))).setNegativeButton("Close",null).show());
        }
        if(found==0)catalogCards.addView(text("No examples found. You can still connect an unlisted Bluetooth media speaker.",14,SUB));
    }
    private void buildAbout(LinearLayout page){
        page.addView(heading("A voice worth sharing",26));page.addView(text("MicBridge 0.4.2 beta · Android 8+",13,SUB));
        LinearLayout creator=card(page);creator.addView(heading("Created by",16));creator.addView(heading(AppInfo.CREATOR,22));creator.addView(text("An independent project to make live speech amplification and creative voice tools more accessible. Built for lectures, presentations and everyday voice experiments.",14,SUB));
        LinearLayout support=card(page);support.addView(heading("Support the project",20));support.addView(text("Visit my GitHub to follow development. You can support the project on Ko-fi, or email me personally. Support is voluntary; Ko-fi opens in your browser.",14,SUB));
        Button kofi=button("Support on Ko-fi",TEAL);fullButton(support,kofi);kofi.setOnClickListener(v->openExternal(new Intent(Intent.ACTION_VIEW,Uri.parse(AppInfo.KOFI))));
        Button github=button("Visit my GitHub",TEAL);fullButton(support,github);github.setOnClickListener(v->openExternal(new Intent(Intent.ACTION_VIEW,Uri.parse(AppInfo.GITHUB))));
        Button project=button("Project repository",PALE);fullButton(support,project);project.setOnClickListener(v->openExternal(new Intent(Intent.ACTION_VIEW,Uri.parse(AppInfo.REPOSITORY))));support.addView(text("The project repository may require access and a GitHub sign-in.",12,SUB));
        support.addView(text(AppInfo.EMAIL,14,TEAL));Button donate=button("Email about a donation",PALE);fullButton(support,donate);donate.setOnClickListener(v->emailCreator("MicBridge — project support"));
        LinearLayout collaborate=card(page);collaborate.addView(heading("Open for collaboration",20));collaborate.addView(text("Android developers, audio engineers, designers and beta testers are welcome. Share your ideas, hardware test results or a proposal by email.",14,SUB));Button contact=button("Discuss a collaboration",TEAL);fullButton(collaborate,contact);contact.setOnClickListener(v->emailCreator("MicBridge — collaboration enquiry"));
        LinearLayout privacy=card(page);privacy.addView(heading("Privacy and practical limits",18));privacy.addView(text("Processing stays on your phone. No recordings, cloud upload, advertising or analytics. The app remains on screen during a session and stops when you leave it. External links and email open another app and stop live audio. No donation is collected inside MicBridge.",13,SUB));privacy.addView(text("USB and wired mic compatibility depends on Android and the adapter. Bluetooth adds buffering. Voice care is best-effort processing, not studio restoration, feedback cancellation or a guarantee of identical sound across microphones.",12,SUB));
    }
    private void openExternal(Intent intent){try{startActivity(intent);}catch(ActivityNotFoundException error){Toast.makeText(this,"No app available to open this link",Toast.LENGTH_LONG).show();}}
    private void emailCreator(String subject){
        try{startActivity(new Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:"+AppInfo.EMAIL+"?subject="+Uri.encode(subject))));}
        catch(ActivityNotFoundException error){android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("MicBridge creator email",AppInfo.EMAIL));Toast.makeText(this,"Email address copied: "+AppInfo.EMAIL,Toast.LENGTH_LONG).show();}
    }
    private void showPage(int index) {for(int i=0;i<4;i++){pages[i].setVisibility(i==index?View.VISIBLE:View.GONE);tabs[i].setBackground(shape(i==index?PALE:PAPER,14));tabs[i].setTextColor(i==index?TEAL:SUB);tabs[i].setSelected(i==index);}scroll.scrollTo(0,0);}
    private final class Slider {
        final SeekBar bar;final TextView label;final String title;final Formatter formatter;Runnable callback;
        Slider(LinearLayout parent,String title,int max,int value,Formatter f) {
            this.title=title;formatter=f;label=text("",14,INK);parent.addView(label);
            bar=new SeekBar(MainActivity.this);bar.setMax(max);bar.setProgress(value);bar.setProgressTintList(ColorStateList.valueOf(TEAL));bar.setThumbTintList(ColorStateList.valueOf(TEAL));bar.setContentDescription(title);parent.addView(bar,new LinearLayout.LayoutParams(-1,dp(42)));update();
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){update();if(callback!=null)callback.run();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        }
        void update(){label.setText(title+"  ·  "+formatter.format(bar.getProgress()));}
        int value(){return bar.getProgress();}void value(int v){bar.setProgress(v);}void setOnChange(Runnable r){callback=r;}
    }
    private interface Formatter {String format(int p);}
    private void applyPreset(int index) {
        EffectSettings fx=VoicePresets.get(index);changing=true;presetIndex=index;bypass.setChecked(true);clarity.setChecked(fx.clarity);speech.setChecked(fx.speech);autoLevel.setChecked(fx.autoLevel);
        pitch.value(Math.round(fx.pitch)+6);tone.value(Math.round(fx.tone)+6);bass.value(Math.round(fx.bass)+12);mid.value(Math.round(fx.mid)+12);treble.value(Math.round(fx.treble)+12);
        growl.value(Math.round(fx.growl*100));robot.value(Math.round(fx.robot*100));echo.value(Math.round(fx.echo*100));delay.value(fx.echoMillis-100);noise.value(Math.round(fx.noise*100));deEss.value(Math.round(fx.deEss*100));changing=false;updateEffects();
    }
    private void updateEffects() {
        engine.setEffects(new EffectSettings(bypass.isChecked(),clarity.isChecked(),pitch.value()-6,tone.value()-6,bass.value()-12,mid.value()-12,treble.value()-12,growl.value()/100f,speech.isChecked(),robot.value()/100f,echo.value()/100f,delay.value()+100,noise.value()/100f,deEss.value()/100f,autoLevel.isChecked()));
        for(Slider control:new Slider[]{pitch,tone,bass,mid,treble,growl,robot,echo,delay,noise,deEss}){enabled(control.bar,bypass.isChecked());control.label.setAlpha(bypass.isChecked()?1f:0.5f);}
        enabled(clarity,bypass.isChecked());enabled(speech,bypass.isChecked());enabled(autoLevel,bypass.isChecked());
        String title=presetIndex>=0?presetTitles[presetIndex]:"Custom voice";presetName.setText(title);
        for(int i=0;i<presetTitles.length;i++){presets[i].setBackground(shape(i==presetIndex?PALE:BG,14));presets[i].setTextColor(i==presetIndex?TEAL:INK);}
        fxSummary.setText(!bypass.isChecked()?"Effects bypassed • natural voice":pitch.value()!=6?"Pitch shifting adds a short processing delay":echo.value()>0?"Echo timing changes repeat spacing, not Bluetooth delay":"Voice controls update during your session");
    }
    private void restoreSettings() {
        changing=true;presetIndex=prefs.getInt("preset",6);if(presetIndex>=presetTitles.length||presetIndex < -1)presetIndex=6;
        EffectSettings defaults=VoicePresets.get(presetIndex);noise.value(prefs.getInt("noise",Math.round(defaults.noise*100)));deEss.value(prefs.getInt("deEss",Math.round(defaults.deEss*100)));autoLevel.setChecked(prefs.getBoolean("autoLevel",defaults.autoLevel));fast.setChecked(prefs.getBoolean("fast",true));engine.setPreferFast(fast.isChecked());
        gain.value(prefs.getInt("gain",90));pitch.value(prefs.getInt("pitch",6));tone.value(prefs.getInt("tone",6));bass.value(prefs.getInt("bass",12));mid.value(prefs.getInt("mid",12));treble.value(prefs.getInt("treble",12));growl.value(prefs.getInt("growl",0));speech.setChecked(prefs.getBoolean("speech",false));robot.value(prefs.getInt("robot",0));echo.value(prefs.getInt("echo",0));delay.value(prefs.getInt("delay",150));bypass.setChecked(prefs.getBoolean("fx",true));clarity.setChecked(prefs.getBoolean("clarity",false));changing=false;updateEffects();engine.setGain(0.1f+gain.value()/100f);if(!prefs.contains("preset"))applyPreset(6);
    }
    private void saveSettings() {
        prefs.edit().putInt("noise",noise.value()).putInt("deEss",deEss.value()).putBoolean("autoLevel",autoLevel.isChecked()).putBoolean("fast",fast.isChecked()).putInt("preset",presetIndex).putInt("gain",gain.value()).putInt("pitch",pitch.value()).putInt("tone",tone.value()).putInt("bass",bass.value()).putInt("mid",mid.value()).putInt("treble",treble.value()).putInt("growl",growl.value()).putBoolean("speech",speech.isChecked()).putInt("robot",robot.value()).putInt("echo",echo.value()).putInt("delay",delay.value()).putBoolean("fx",bypass.isChecked()).putBoolean("clarity",clarity.isChecked()).apply();
    }
    private boolean hasPermissions(){return checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED&&(Build.VERSION.SDK_INT<31||checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED);}
    private void ensurePermissions(){List<String> p=new ArrayList<>();if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.RECORD_AUDIO);if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.BLUETOOTH_CONNECT);if(!p.isEmpty())requestPermissions(p.toArray(new String[0]),1);else refreshDevices();}
    @Override public void onRequestPermissionsResult(int code,String[] p,int[] g){super.onRequestPermissionsResult(code,p,g);if(hasPermissions())refreshDevices();else{status.setText("Allow Microphone and Nearby devices in app permissions, then Refresh.");enabled(start,false);}}
    private int selectedId(Spinner spinner){Object item=spinner.getSelectedItem();return item instanceof DeviceChoices.Row?((DeviceChoices.Row)item).id:-1;}
    private void selectionChanged(){if(!refreshingDevices&&!active)enabled(start,inputs.containsKey(selectedId(input))&&outputs.containsKey(selectedId(output)));}
    private void refreshDevices(){
        if(destroyed||active)return;
        int oldIn=selectedId(input),oldOut=selectedId(output);
        Map<Integer,AudioDeviceInfo> newInputs=new HashMap<>(),newOutputs=new HashMap<>();
        List<DeviceChoices.Row> inRows=new ArrayList<>(),outRows=new ArrayList<>();
        try{
            if(!hasPermissions())throw new SecurityException();
            // One snapshot for both directions; repeated IDs cannot create duplicate rows.
            for(AudioDeviceInfo device:audio.getDevices(AudioManager.GET_DEVICES_ALL)){
                String type=connection(device);if(type==null)continue;
                boolean builtIn=device.getType()==AudioDeviceInfo.TYPE_BUILTIN_MIC||device.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;
                DeviceChoices.Row row=new DeviceChoices.Row(device.getId(),AudioEngine.deviceName(device),type,builtIn);
                if(device.isSource()&&inputType(device)&&!newInputs.containsKey(device.getId())){newInputs.put(device.getId(),device);inRows.add(row);}
                if(device.isSink()&&outputType(device)&&!newOutputs.containsKey(device.getId())){newOutputs.put(device.getId(),device);outRows.add(row);}
            }
        }catch(SecurityException error){newInputs.clear();newOutputs.clear();inRows.clear();outRows.clear();status.setText("Allow Microphone and Nearby devices, then Refresh.");}
        refreshingDevices=true;
        inputs=Collections.unmodifiableMap(newInputs);outputs=Collections.unmodifiableMap(newOutputs);
        populate(input,DeviceChoices.build(inRows,inRows.isEmpty()?"No microphone detected":"Choose microphone"),oldIn);
        populate(output,DeviceChoices.build(outRows,outRows.isEmpty()?"No speaker detected":"Choose speaker"),oldOut);
        refreshingDevices=false;selectionChanged();
        connectionCount.setText(inputs.size()+" microphone route(s) · "+outputs.size()+" speaker route(s)");
        if((oldIn>=0&&!inputs.containsKey(oldIn))||(oldOut>=0&&!outputs.containsKey(oldOut)))status.setText("Selected device disconnected. Choose an available microphone and speaker.");
    }
    private boolean inputType(AudioDeviceInfo d){int t=d.getType();return t==AudioDeviceInfo.TYPE_BUILTIN_MIC||t==AudioDeviceInfo.TYPE_WIRED_HEADSET||t==AudioDeviceInfo.TYPE_USB_DEVICE||t==AudioDeviceInfo.TYPE_USB_HEADSET||t==AudioDeviceInfo.TYPE_USB_ACCESSORY||t==AudioDeviceInfo.TYPE_LINE_ANALOG||t==AudioDeviceInfo.TYPE_LINE_DIGITAL;}
    private boolean outputType(AudioDeviceInfo d){return d.getType()!=AudioDeviceInfo.TYPE_BUILTIN_MIC;}
    private String connection(AudioDeviceInfo d){
        switch(d.getType()){
            case AudioDeviceInfo.TYPE_BUILTIN_MIC:case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:return "Built-in";
            case AudioDeviceInfo.TYPE_USB_DEVICE:case AudioDeviceInfo.TYPE_USB_HEADSET:case AudioDeviceInfo.TYPE_USB_ACCESSORY:return "USB audio";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:return "Wired audio";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:return "Bluetooth media";
            case AudioDeviceInfo.TYPE_BLE_HEADSET:case AudioDeviceInfo.TYPE_BLE_SPEAKER:case AudioDeviceInfo.TYPE_BLE_BROADCAST:return "LE Audio";
            case AudioDeviceInfo.TYPE_HEARING_AID:return "Hearing aid audio";
            case AudioDeviceInfo.TYPE_LINE_ANALOG:case AudioDeviceInfo.TYPE_LINE_DIGITAL:return "Line audio";
            case AudioDeviceInfo.TYPE_HDMI:case AudioDeviceInfo.TYPE_HDMI_ARC:case AudioDeviceInfo.TYPE_HDMI_EARC:return "HDMI audio";
            case AudioDeviceInfo.TYPE_DOCK:return "Dock audio";
            default:return null; // virtual buses, telephony, safe-speaker aliases and unknown routes
        }
    }
    private void populate(Spinner spinner,List<DeviceChoices.Row> rows,int previous){
        ArrayAdapter<DeviceChoices.Row> adapter=new ArrayAdapter<DeviceChoices.Row>(this,android.R.layout.simple_spinner_item,rows){
            private View style(View view){TextView label=(TextView)view;label.setSingleLine(false);label.setMaxLines(4);label.setTextSize(14);label.setMinHeight(dp(52));label.setGravity(Gravity.CENTER_VERTICAL);label.setPadding(dp(10),dp(8),dp(10),dp(8));return view;}
            @Override public View getView(int position,View convert,ViewGroup parent){return style(super.getView(position,convert,parent));}
            @Override public View getDropDownView(int position,View convert,ViewGroup parent){return style(super.getDropDownView(position,convert,parent));}
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(adapter);spinner.setSelection(DeviceChoices.position(rows,previous));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> parent,View view,int position,long id){selectionChanged();}public void onNothingSelected(AdapterView<?> parent){selectionChanged();}});
    }
    private void startAudio(){
        if(!hasPermissions()){ensurePermissions();return;}refreshDevices();
        AudioDeviceInfo selectedInput=inputs.get(selectedId(input)),selectedOutput=outputs.get(selectedId(output));
        if(selectedInput==null||selectedOutput==null){status.setText("Choose an available microphone and speaker first.");return;}
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setOnAudioFocusChangeListener(change->{if(change!=AudioManager.AUDIOFOCUS_GAIN&&active)stopAudio("Paused by another app or a call");},handler).setWillPauseWhenDucked(true).build();
        if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){status.setText("Audio is busy. Stop other audio apps and retry.");focus=null;return;}
        active=true;ready=false;isMuted=true;liveInputId=selectedInput.getId();liveOutputId=selectedOutput.getId();enabled(input,false);enabled(output,false);enabled(refresh,false);enabled(fast,false);start.setText("Stop session");enabled(mute,false);mute.setText("Unmute");status.setText("Checking your audio route • muted");route.setText("Waiting for microphone and speaker…");getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);if(!engine.start(selectedInput,selectedOutput))onStopped("Audio is still stopping. Please retry.");
    }
    private void toggleMute(){
        if(!ready||calibrating)return;
        AudioDeviceInfo selectedOutput=outputs.get(liveOutputId);
        if(isMuted&&selectedOutput!=null&&selectedOutput.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
            new AlertDialog.Builder(this).setTitle("Phone speaker feedback")
                .setMessage("The phone speaker is close to the microphone and may squeal. Start at very low volume. A separate speaker works better for lectures.")
                .setPositiveButton("Unmute",(d,w)->{if(ready&&!calibrating&&engine.isRunning()){isMuted=false;engine.setMuted(false);updateLiveState();}}).setNegativeButton("Stay muted",null).show();
        }else{isMuted=!isMuted;engine.setMuted(isMuted);updateLiveState();}
    }
    private void stopAudio(String reason){ready=false;enabled(mute,false);enabled(calibrate,false);enabled(start,false);status.setText("Stopping session…");engine.stop(reason);}
    private void updateLiveState(){mute.setText(isMuted?"Unmute":"Mute now");mute.setBackground(shape(isMuted?PALE:0xFFFFE8E3,14));status.setText(isMuted?"Ready • muted · microphone listening":"Live • voice to speaker");}
    @Override public void onReady(String actual){if(destroyed||!active||!engine.isRunning())return;ready=true;route.setText(actual);enabled(calibrate,true);enabled(mute,true);updateLiveState();}
    @Override public void onMeter(float peak){if(destroyed||!active)return;meter.push(peak);float db=peak>0?(float)(20*Math.log10(peak)):-60;level.setText(String.format(Locale.UK,"Microphone peak  %.0f dBFS%s",Math.max(-60,db),peak>0.98f?" · input clipping":""));}
    @Override public void onStopped(String reason){
        if(focus!=null){audio.abandonAudioFocusRequest(focus);focus=null;}active=false;ready=false;calibrating=false;isMuted=true;if(destroyed)return;enabled(calibrate,false);enabled(fast,true);audioHealth.setText("Buffer diagnostics appear during a session.");calibrationStatus.setText("Start a muted session to calibrate background noise.");
        status.setText(reason);route.setText("Start muted. Unmute when you are ready.");meter.clear();level.setText("Microphone level  —");start.setText("Start session");mute.setText("Unmute");enabled(mute,false);mute.setBackground(shape(PALE,14));enabled(input,true);enabled(output,true);enabled(refresh,true);getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);refreshDevices();
    }
    @Override public void onAudioHealth(String details){if(!destroyed&&active)audioHealth.setText(details);}
    @Override public void onCalibration(float noiseRms){if(destroyed||!ready)return;calibrating=false;enabled(calibrate,true);enabled(mute,true);float db=20*(float)Math.log10(Math.max(0.000001f,noiseRms));calibrationStatus.setText(String.format(Locale.UK,"Background measured: %.0f dBFS. %s",db,noiseRms>0.025f?"That was loud. Stay quiet and retry; noise reduction cannot remove speech or heavy room noise.":"Voice care adjusted for this session. You remain muted."));}
    @Override protected void onResume(){super.onResume();if(audio!=null&&!active)refreshDevices();}
    @Override protected void onStop(){saveSettings();if(active)stopAudio("Session stopped when app left the screen");super.onStop();}
    @Override protected void onDestroy(){destroyed=true;handler.removeCallbacks(deviceRefresh);engine.stop("App closed");audio.unregisterAudioDeviceCallback(devices);if(focus!=null){audio.abandonAudioFocusRequest(focus);focus=null;}super.onDestroy();}
}
