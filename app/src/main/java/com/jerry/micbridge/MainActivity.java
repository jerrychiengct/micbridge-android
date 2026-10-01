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
    private final List<AudioDeviceInfo> inputs=new ArrayList<>(),outputs=new ArrayList<>();
    private Spinner input,output;
    private Button start,mute,refresh; private final Button[] tabs=new Button[3], presets=new Button[6];
    private TextView status,route,level,connectionCount,presetName,fxSummary;
    private MeterView meter;
    private final LinearLayout[] pages=new LinearLayout[3]; private LinearLayout catalogCards;
    private ScrollView scroll; private Switch bypass,clarity;
    private Slider gain,pitch,tone,robot,echo,delay;
    private boolean active,ready,isMuted=true,destroyed,changing;
    private int liveInputId=-1,liveOutputId=-1,presetIndex=0;
    private AudioFocusRequest focus;
    private final String[] presetTitles={"Natural","Clear voice","Deep voice","Bright voice","Robot","Room echo"};
    private final String[] presetHints={"Original voice","Speech clarity","Lower pitch","Higher pitch","Metallic texture","Spacious repeats"};
    private final AudioDeviceCallback devices=new AudioDeviceCallback() {
        @Override public void onAudioDevicesAdded(AudioDeviceInfo[] added) { if(!active) refreshDevices(); }
        @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removed) {
            for(AudioDeviceInfo d:removed) if(active&&(d.getId()==liveInputId||d.getId()==liveOutputId)) stopAudio("Device disconnected");
            if(!active) refreshDevices();
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
        b.setPadding(dp(10),dp(5),dp(10),dp(5));b.setStateListAnimator(null);return b;
    }
    private void enabled(View view,boolean value){view.setEnabled(value);view.setAlpha(value?1f:0.45f);}
    private void fullButton(LinearLayout p,Button b) { LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(50));lp.topMargin=dp(10);p.addView(b,lp); }
    private void buildUi() {
        LinearLayout root=vertical();root.setBackgroundColor(BG);setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            int top,bottom;
            if(Build.VERSION.SDK_INT>=30) { Insets n=insets.getInsets(WindowInsets.Type.systemBars());top=n.top;bottom=n.bottom; }
            else { top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom(); }
            root.setPadding(0,top,0,bottom);return insets;
        });
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(22),dp(10),dp(22),dp(10));
        LinearLayout brand=vertical();brand.addView(heading("MicBridge",27));brand.addView(text("VOICE TO SPEAKER",10,SUB));header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView beta=text("BETA 0.2",11,TEAL);beta.setTypeface(Typeface.DEFAULT,Typeface.BOLD);beta.setPadding(dp(12),dp(8),dp(12),dp(8));beta.setBackground(shape(PALE,30));header.addView(beta);root.addView(header);
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setPadding(dp(18),dp(8),dp(18),dp(8));
        LinearLayout content=vertical();scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<3;i++) { pages[i]=vertical();content.addView(pages[i]); }
        buildLive(pages[0]);buildStudio(pages[1]);buildSpeakers(pages[2]);
        LinearLayout dock=vertical();dock.setBackgroundColor(PAPER);dock.setPadding(dp(20),dp(8),dp(20),0);
        status=text("Connect a microphone and speaker",13,SUB);status.setMaxLines(3);dock.addView(status);
        LinearLayout actions=new LinearLayout(this);start=button("Start session",TEAL);mute=button("Unmute",PALE);enabled(mute,false);
        LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(54),1);a.setMargins(0,dp(4),dp(8),dp(6));actions.addView(start,a);
        LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,dp(54),1);b.setMargins(dp(8),dp(4),0,dp(6));actions.addView(mute,b);dock.addView(actions);
        start.setOnClickListener(v->{if(active)stopAudio("Session stopped");else startAudio();});
        mute.setOnClickListener(v->{if(ready){isMuted=!isMuted;engine.setMuted(isMuted);updateLiveState();}});
        LinearLayout nav=new LinearLayout(this);
        String[] labels={"Live","Voice studio","Speakers"};
        for(int i=0;i<3;i++) { final int n=i;tabs[i]=button(labels[i],PAPER);nav.addView(tabs[i],new LinearLayout.LayoutParams(0,dp(54),1));tabs[i].setOnClickListener(v->showPage(n)); }
        dock.addView(nav);root.addView(dock);showPage(0);
    }
    private void buildLive(LinearLayout page) {
        LinearLayout hero=card(page);hero.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF142D36,0xFF235154}));
        hero.addView(text("LIVE MONITOR",11,0xFF91DCCE));
        TextView h=text("Your voice,\namplified.",30,Color.WHITE);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);hero.addView(h);
        meter=new MeterView(this);hero.addView(meter,new LinearLayout.LayoutParams(-1,dp(74)));
        level=text("Microphone level  —",13,0xFFB5D7D9);hero.addView(level);
        route=text("Start muted. Unmute when you are ready.",12,0xFFB5D7D9);hero.addView(route);
        LinearLayout connections=card(page);connections.addView(heading("Your connection",20));connectionCount=text("USB-C microphone + Bluetooth speaker",12,SUB);connections.addView(connectionCount);
        connections.addView(text("MICROPHONE INPUT",11,TEAL));input=new Spinner(this);connections.addView(input,new LinearLayout.LayoutParams(-1,dp(52)));
        connections.addView(text("SPEAKER OUTPUT",11,TEAL));output=new Spinner(this);connections.addView(output,new LinearLayout.LayoutParams(-1,dp(52)));
        refresh=button("Refresh connected devices",PALE);fullButton(connections,refresh);refresh.setOnClickListener(v->ensurePermissions());
        Button pair=button("Pair a Bluetooth speaker",BG);fullButton(connections,pair);pair.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        gain=new Slider(connections,"Microphone gain",290,90,p->String.format(Locale.UK,"%.1f×",0.1f+p/100f));
        gain.setOnChange(()->{engine.setGain(0.1f+gain.value()/100f);});
        LinearLayout guide=card(page);guide.addView(heading("A good first test",18));
        guide.addView(text("Connect the ONSMO USB-C receiver, pair your speaker, then start a session. Begin at low speaker volume and keep the speaker away from the microphone.",14,SUB));
        guide.addView(text("Keep this app open. Bluetooth adds delay; voice effects can add more. No audio is saved.",12,SUB));
    }
    private void buildStudio(LinearLayout page) {
        page.addView(heading("Shape your voice",27));page.addView(text("Choose a preset, then make it yours.",14,SUB));
        LinearLayout choices=card(page);presetName=heading("Natural",20);choices.addView(presetName);
        for(int row=0;row<3;row++) { LinearLayout line=new LinearLayout(this);
            for(int col=0;col<2;col++) { final int index=row*2+col;Button p=button(presetTitles[index]+"\n"+presetHints[index],BG);p.setTextSize(12);presets[index]=p;
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(68),1);lp.setMargins(col==0?0:dp(5),dp(8),col==0?dp(5):0,0);line.addView(p,lp);p.setOnClickListener(v->applyPreset(index)); }
            choices.addView(line);
        }
        LinearLayout controls=card(page);controls.addView(heading("Sound modulator",20));
        bypass=new Switch(this);bypass.setText("Voice effects enabled");bypass.setTextColor(INK);bypass.setTextSize(14);bypass.setChecked(true);controls.addView(bypass);
        pitch=new Slider(controls,"Pitch",12,6,p->String.format(Locale.UK,"≈ %+d semitones",p-6));
        tone=new Slider(controls,"Tone",12,6,p->p==6?"Balanced":(p<6?"Warm ":"Bright ")+Math.abs(p-6)+" dB");
        robot=new Slider(controls,"Robot texture",100,0,p->p+"%");
        echo=new Slider(controls,"Echo blend",50,0,p->p+"%");
        delay=new Slider(controls,"Echo timing",500,150,p->(p+100)+" ms");
        clarity=new Switch(this);clarity.setText("Reduce low-frequency rumble");clarity.setTextColor(INK);clarity.setTextSize(14);controls.addView(clarity);
        Runnable change=()->{if(!changing){presetIndex=-1;updateEffects();}};
        pitch.setOnChange(change);tone.setOnChange(change);robot.setOnChange(change);echo.setOnChange(change);delay.setOnChange(change);
        bypass.setOnCheckedChangeListener((b,on)->{if(!changing)updateEffects();});clarity.setOnCheckedChangeListener((b,on)->change.run());
        fxSummary=text("Natural voice • effects add no intentional delay",12,SUB);controls.addView(fxSummary);
        Button reset=button("Reset to natural voice",PALE);fullButton(controls,reset);reset.setOnClickListener(v->applyPreset(0));
        page.addView(text("Pitch is a creative beta effect, not studio-grade voice conversion. The limiter reduces digital clipping; it cannot prevent feedback.",12,SUB));
    }
    private void buildSpeakers(LinearLayout page) {
        page.addView(heading("Find your speaker",27));page.addView(text("12 brands. One Bluetooth audio connection.",14,SUB));
        LinearLayout intro=card(page);intro.addView(heading("Xiaomi, Tribit and beyond",19));
        intro.addView(text("Pair in Android settings, enable media audio, then choose the connected speaker on Live. MicBridge works by connection type, not by brand.",14,SUB));
        intro.addView(text("This guide lists examples from manufacturer pages. It is not an exhaustive market list or a list of device-tested models. Malaysian stock and availability vary.",12,SUB));
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
                .setPositiveButton("Manufacturer page",(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(item[2])))).setNegativeButton("Close",null).show());
        }
        if(found==0)catalogCards.addView(text("No examples found. You can still connect an unlisted Bluetooth media speaker.",14,SUB));
    }
    private void showPage(int index) {for(int i=0;i<3;i++){pages[i].setVisibility(i==index?View.VISIBLE:View.GONE);tabs[i].setBackground(shape(i==index?PALE:PAPER,14));tabs[i].setTextColor(i==index?TEAL:SUB);}scroll.scrollTo(0,0);}
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
        changing=true;presetIndex=index;bypass.setChecked(true);clarity.setChecked(index==1);pitch.value(index==2?2:index==3?10:6);tone.value(index==1?8:index==2?4:index==3?8:6);robot.value(index==4?85:0);echo.value(index==5?30:0);delay.value(150);changing=false;updateEffects();
    }
    private void updateEffects() {
        engine.setEffects(new EffectSettings(bypass.isChecked(),clarity.isChecked(),pitch.value()-6,tone.value()-6,robot.value()/100f,echo.value()/100f,delay.value()+100));
        for(Slider control:new Slider[]{pitch,tone,robot,echo,delay}){enabled(control.bar,bypass.isChecked());control.label.setAlpha(bypass.isChecked()?1f:0.5f);}
        enabled(clarity,bypass.isChecked());
        String title=presetIndex>=0?presetTitles[presetIndex]:"Custom voice";presetName.setText(title);
        for(int i=0;i<6;i++){presets[i].setBackground(shape(i==presetIndex?PALE:BG,14));presets[i].setTextColor(i==presetIndex?TEAL:INK);}
        fxSummary.setText(!bypass.isChecked()?"Effects bypassed • natural voice":pitch.value()!=6?"Pitch shifting adds a short processing delay":echo.value()>0?"Echo timing changes repeat spacing, not Bluetooth delay":"Voice controls update during your session");
    }
    private void restoreSettings() {
        changing=true;presetIndex=prefs.getInt("preset",0);if(presetIndex>5)presetIndex=0;
        gain.value(prefs.getInt("gain",90));pitch.value(prefs.getInt("pitch",6));tone.value(prefs.getInt("tone",6));robot.value(prefs.getInt("robot",0));echo.value(prefs.getInt("echo",0));delay.value(prefs.getInt("delay",150));bypass.setChecked(prefs.getBoolean("fx",true));clarity.setChecked(prefs.getBoolean("clarity",false));changing=false;updateEffects();engine.setGain(0.1f+gain.value()/100f);
    }
    private void saveSettings() {
        prefs.edit().putInt("preset",presetIndex).putInt("gain",gain.value()).putInt("pitch",pitch.value()).putInt("tone",tone.value()).putInt("robot",robot.value()).putInt("echo",echo.value()).putInt("delay",delay.value()).putBoolean("fx",bypass.isChecked()).putBoolean("clarity",clarity.isChecked()).apply();
    }
    private boolean hasPermissions(){return checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED&&(Build.VERSION.SDK_INT<31||checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED);}
    private void ensurePermissions(){List<String> p=new ArrayList<>();if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.RECORD_AUDIO);if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.BLUETOOTH_CONNECT);if(!p.isEmpty())requestPermissions(p.toArray(new String[0]),1);else refreshDevices();}
    @Override public void onRequestPermissionsResult(int code,String[] p,int[] g){super.onRequestPermissionsResult(code,p,g);if(hasPermissions())refreshDevices();else{status.setText("Allow Microphone and Nearby devices in app permissions, then Refresh.");enabled(start,false);}}
    private void refreshDevices(){
        if(destroyed||active||!hasPermissions())return;
        int oldIn=selectedId(input,inputs),oldOut=selectedId(output,outputs);inputs.clear();outputs.clear();
        try{
            for(AudioDeviceInfo d:audio.getDevices(AudioManager.GET_DEVICES_INPUTS)){int t=d.getType();if(t==AudioDeviceInfo.TYPE_USB_DEVICE||t==AudioDeviceInfo.TYPE_USB_HEADSET||t==AudioDeviceInfo.TYPE_USB_ACCESSORY||t==AudioDeviceInfo.TYPE_WIRED_HEADSET)inputs.add(d);}
            // Built-in microphone is available as an explicit alternative, never a silent fallback.
            for(AudioDeviceInfo d:audio.getDevices(AudioManager.GET_DEVICES_INPUTS))if(d.getType()==AudioDeviceInfo.TYPE_BUILTIN_MIC){inputs.add(d);break;}
            for(AudioDeviceInfo d:audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){int t=d.getType();if(t==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP||t==AudioDeviceInfo.TYPE_WIRED_HEADPHONES||t==AudioDeviceInfo.TYPE_WIRED_HEADSET||t==AudioDeviceInfo.TYPE_LINE_ANALOG||t==AudioDeviceInfo.TYPE_USB_DEVICE||t==AudioDeviceInfo.TYPE_USB_HEADSET||(Build.VERSION.SDK_INT>=31&&(t==AudioDeviceInfo.TYPE_BLE_SPEAKER||t==AudioDeviceInfo.TYPE_BLE_HEADSET)))outputs.add(d);}
            populate(input,inputs,oldIn,"No microphone detected");populate(output,outputs,oldOut,"Pair a speaker to begin");enabled(start,!inputs.isEmpty()&&!outputs.isEmpty());
            connectionCount.setText(inputs.size()+" microphone option(s)  ·  "+outputs.size()+" audio output(s)");
            if(!active&&outputs.isEmpty())status.setText("Pair your speaker, then tap Refresh");
        }catch(SecurityException e){status.setText("Nearby devices permission required. Tap Refresh.");enabled(start,false);}
    }
    private int selectedId(Spinner s,List<AudioDeviceInfo> list){int i=s.getSelectedItemPosition();return i>=0&&i<list.size()?list.get(i).getId():-1;}
    private String connection(AudioDeviceInfo d){int t=d.getType();if(t==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)return "Bluetooth";if(Build.VERSION.SDK_INT>=31&&(t==AudioDeviceInfo.TYPE_BLE_SPEAKER||t==AudioDeviceInfo.TYPE_BLE_HEADSET))return "LE Audio";if(t==AudioDeviceInfo.TYPE_BUILTIN_MIC)return "Built-in";return "Wired / USB";}
    private void populate(Spinner s,List<AudioDeviceInfo> list,int previous,String empty){
        List<String> names=new ArrayList<>();int selected=0;for(int i=0;i<list.size();i++){names.add(AudioEngine.deviceName(list.get(i))+" · "+connection(list.get(i)));if(list.get(i).getId()==previous)selected=i;}if(names.isEmpty())names.add(empty);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(adapter);s.setSelection(selected);
    }
    private void startAudio(){
        if(!hasPermissions()){ensurePermissions();return;}int i=input.getSelectedItemPosition(),o=output.getSelectedItemPosition();if(i<0||i>=inputs.size()||o<0||o>=outputs.size()){refreshDevices();return;}
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setOnAudioFocusChangeListener(change->{if(change!=AudioManager.AUDIOFOCUS_GAIN&&active)stopAudio("Paused by another app or a call");},handler).setWillPauseWhenDucked(true).build();
        if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){status.setText("Audio is busy. Stop other audio apps and retry.");focus=null;return;}
        active=true;ready=false;isMuted=true;liveInputId=inputs.get(i).getId();liveOutputId=outputs.get(o).getId();enabled(input,false);enabled(output,false);enabled(refresh,false);start.setText("Stop session");enabled(mute,false);mute.setText("Unmute");status.setText("Checking your audio route • muted");route.setText("Waiting for microphone and speaker…");getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);engine.start(inputs.get(i),outputs.get(o));
    }
    private void stopAudio(String reason){ready=false;enabled(mute,false);enabled(start,false);status.setText("Stopping session…");engine.stop(reason);}
    private void updateLiveState(){mute.setText(isMuted?"Unmute":"Mute now");mute.setBackground(shape(isMuted?PALE:0xFFFFE8E3,14));status.setText(isMuted?"Ready • muted · microphone listening":"Live • voice to speaker");}
    @Override public void onReady(String actual){if(destroyed||!active||!engine.isRunning())return;ready=true;route.setText(actual);enabled(mute,true);updateLiveState();}
    @Override public void onMeter(float peak){if(destroyed||!active)return;meter.push(peak);float db=peak>0?(float)(20*Math.log10(peak)):-60;level.setText(String.format(Locale.UK,"Microphone peak  %.0f dBFS%s",Math.max(-60,db),peak>0.98f?" · input clipping":""));}
    @Override public void onStopped(String reason){
        if(focus!=null){audio.abandonAudioFocusRequest(focus);focus=null;}active=false;ready=false;isMuted=true;if(destroyed)return;
        status.setText(reason);route.setText("Start muted. Unmute when you are ready.");meter.clear();level.setText("Microphone level  —");start.setText("Start session");mute.setText("Unmute");enabled(mute,false);mute.setBackground(shape(PALE,14));enabled(input,true);enabled(output,true);enabled(refresh,true);getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);refreshDevices();
    }
    @Override protected void onResume(){super.onResume();if(audio!=null&&!active)refreshDevices();}
    @Override protected void onStop(){saveSettings();if(active)stopAudio("Session stopped when app left the screen");super.onStop();}
    @Override protected void onDestroy(){destroyed=true;engine.stop("App closed");audio.unregisterAudioDeviceCallback(devices);if(focus!=null){audio.abandonAudioFocusRequest(focus);focus=null;}super.onDestroy();}
}
