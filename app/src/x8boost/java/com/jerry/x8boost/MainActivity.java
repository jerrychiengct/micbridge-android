package com.jerry.x8boost;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS="x8boost";
    private static final String PUBG_GLOBAL="com.tencent.ig";
    private static final String PUBG_KR="com.pubg.krmobile";
    private final int bg=Color.rgb(9,12,16), card=Color.rgb(19,24,31), card2=Color.rgb(26,33,41);
    private final int text=Color.rgb(240,245,248), muted=Color.rgb(157,171,181), accent=Color.rgb(118,230,176), warning=Color.rgb(255,188,92);
    private SharedPreferences prefs;
    private TextView statsText,accessText,sessionText;
    private boolean waitingForReturn=false;
    private long launchedAt=0L;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        setContentView(buildUi()); refreshStats(); refreshAccess(); refreshSession();
    }
    @Override protected void onResume(){
        super.onResume(); refreshStats(); refreshAccess();
        if(waitingForReturn && System.currentTimeMillis()-launchedAt>3000L){
            waitingForReturn=false;
            if(prefs.getBoolean("session_active",false)){ restoreSettings(false); toast("Gaming session ended — phone settings restored."); }
        }
        refreshSession();
    }
    private View buildUi(){
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(bg);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(18),dp(18),dp(30));
        scroll.addView(root,new ScrollView.LayoutParams(-1,-2));
        TextView eyebrow=tv("HONOR X8b • PUBG PROFILE",12,accent,true); eyebrow.setLetterSpacing(0.08f); root.addView(eyebrow);
        TextView title=tv("X8 Boost",34,text,true); title.setPadding(0,dp(4),0,0); root.addView(title);
        TextView subtitle=tv("Safe, no-root gaming prep for frame stability. No PUBG files are modified.",15,muted,false); subtitle.setPadding(0,dp(6),0,dp(18)); root.addView(subtitle);
        sessionText=tv("",14,text,true); root.addView(card(sessionText,card2),lpCard());
        TextView h1=tv("LIVE DEVICE STATUS",12,muted,true); h1.setLetterSpacing(0.06f); h1.setPadding(0,dp(18),0,dp(8)); root.addView(h1);
        statsText=tv("Loading…",15,text,false); statsText.setLineSpacing(0f,1.25f); root.addView(card(statsText,card),lpCard());
        TextView h2=tv("PERMISSION CHECK",12,muted,true); h2.setLetterSpacing(0.06f); h2.setPadding(0,dp(18),0,dp(8)); root.addView(h2);
        accessText=tv("",14,text,false); accessText.setLineSpacing(0f,1.2f); root.addView(card(accessText,card),lpCard());
        Button optimise=button("OPTIMISE & LAUNCH PUBG",accent,Color.rgb(10,24,18)); optimise.setOnClickListener(v->optimiseAndLaunch());
        LinearLayout.LayoutParams big=new LinearLayout.LayoutParams(-1,dp(58)); big.topMargin=dp(18); root.addView(optimise,big);
        Button restore=button("Restore phone settings",card2,text); restore.setOnClickListener(v->restoreSettings(true)); root.addView(restore,lpButton());
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Button displayAccess=button("Display access",card2,text); displayAccess.setOnClickListener(v->openWriteSettings());
        Button dndAccess=button("DND access",card2,text); dndAccess.setOnClickListener(v->openDndAccess());
        LinearLayout.LayoutParams half1=new LinearLayout.LayoutParams(0,dp(50),1f); half1.rightMargin=dp(6);
        LinearLayout.LayoutParams half2=new LinearLayout.LayoutParams(0,dp(50),1f); half2.leftMargin=dp(6);
        row.addView(displayAccess,half1); row.addView(dndAccess,half2);
        LinearLayout.LayoutParams rowLp=new LinearLayout.LayoutParams(-1,-2); rowLp.topMargin=dp(10); root.addView(row,rowLp);
        Button appSettings=button("Open PUBG app settings",card2,text); appSettings.setOnClickListener(v->openPubgSettings()); root.addView(appSettings,lpButton());
        Button refresh=button("Refresh status",card2,text); refresh.setOnClickListener(v->{refreshStats();refreshAccess();refreshSession();}); root.addView(refresh,lpButton());
        TextView note=tv("Test build actions:\n\n• Brightness ~72% with Display access.\n• Requests the highest supported refresh rate where HONOR firmware permits it.\n• Priority DND with DND access.\n• Saves original values and restores them when you return from PUBG.\n\nNo CPU/GPU overclock, thermal bypass, PUBG file edits, device spoofing or anti-cheat bypass.",13,muted,false);
        note.setLineSpacing(0f,1.15f); note.setPadding(0,dp(18),0,0); root.addView(note); return scroll;
    }
    private void optimiseAndLaunch(){
        saveOriginalSettingsIfNeeded();
        boolean wrote=false;
        if(Settings.System.canWrite(this)){
            try{
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,184);
                float hz=getMaxRefreshRate();
                if(hz>=60f){ Settings.System.putFloat(getContentResolver(),"peak_refresh_rate",hz); Settings.System.putFloat(getContentResolver(),"min_refresh_rate",hz); }
                wrote=true;
            }catch(Exception ignored){}
        }
        boolean dnd=false; NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(nm!=null && nm.isNotificationPolicyAccessGranted()){ try{nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY); dnd=true;}catch(Exception ignored){} }
        prefs.edit().putBoolean("session_active",true).putBoolean("display_applied",wrote).putBoolean("dnd_applied",dnd).apply(); refreshSession();
        Intent launch=findPubgLaunchIntent();
        if(launch==null){ toast("PUBG Mobile was not found. Opening the Play Store page."); openPubgStore(); return; }
        waitingForReturn=true; launchedAt=System.currentTimeMillis(); launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(launch);
    }
    private void saveOriginalSettingsIfNeeded(){
        if(prefs.getBoolean("session_active",false)) return;
        SharedPreferences.Editor e=prefs.edit();
        try{e.putInt("old_brightness",Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS));}catch(Exception ignored){}
        try{e.putInt("old_brightness_mode",Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE));}catch(Exception ignored){}
        try{e.putFloat("old_peak",Settings.System.getFloat(getContentResolver(),"peak_refresh_rate"));}catch(Exception ignored){}
        try{e.putFloat("old_min",Settings.System.getFloat(getContentResolver(),"min_refresh_rate"));}catch(Exception ignored){}
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(nm!=null && nm.isNotificationPolicyAccessGranted()){ try{e.putInt("old_dnd",nm.getCurrentInterruptionFilter());}catch(Exception ignored){} }
        e.apply();
    }
    private void restoreSettings(boolean showToast){
        if(Settings.System.canWrite(this)){
            try{
                if(prefs.contains("old_brightness_mode")) Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,prefs.getInt("old_brightness_mode",Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC));
                if(prefs.contains("old_brightness")) Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,prefs.getInt("old_brightness",128));
                if(prefs.contains("old_peak")) Settings.System.putFloat(getContentResolver(),"peak_refresh_rate",prefs.getFloat("old_peak",60f));
                if(prefs.contains("old_min")) Settings.System.putFloat(getContentResolver(),"min_refresh_rate",prefs.getFloat("old_min",60f));
            }catch(Exception ignored){}
        }
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(nm!=null && nm.isNotificationPolicyAccessGranted() && prefs.contains("old_dnd")){ try{nm.setInterruptionFilter(prefs.getInt("old_dnd",NotificationManager.INTERRUPTION_FILTER_ALL));}catch(Exception ignored){} }
        prefs.edit().putBoolean("session_active",false).apply(); waitingForReturn=false; refreshStats();refreshAccess();refreshSession(); if(showToast)toast("Original phone settings restored where Android allowed it.");
    }
    private Intent findPubgLaunchIntent(){ PackageManager pm=getPackageManager(); Intent i=pm.getLaunchIntentForPackage(PUBG_GLOBAL); if(i==null)i=pm.getLaunchIntentForPackage(PUBG_KR); return i; }
    private String findInstalledPubgPackage(){ PackageManager pm=getPackageManager(); try{pm.getPackageInfo(PUBG_GLOBAL,0);return PUBG_GLOBAL;}catch(Exception ignored){} try{pm.getPackageInfo(PUBG_KR,0);return PUBG_KR;}catch(Exception ignored){} return null; }
    private void openPubgSettings(){ String pkg=findInstalledPubgPackage(); if(pkg==null){toast("PUBG Mobile is not installed.");return;} startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+pkg))); }
    private void openPubgStore(){ try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+PUBG_GLOBAL)));}catch(Exception e){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id="+PUBG_GLOBAL)));} }
    private void openWriteSettings(){ try{startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));} }
    private void openDndAccess(){ try{startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));} }
    private void refreshStats(){
        if(statsText==null)return;
        String model=Build.MANUFACTURER+" "+Build.MODEL; int batteryPct=-1; float tempC=-1f;
        Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(battery!=null){int level=battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=battery.getIntExtra(BatteryManager.EXTRA_SCALE,100);if(level>=0&&scale>0)batteryPct=Math.round(level*100f/scale);int t=battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,-1);if(t>=0)tempC=t/10f;}
        ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE); ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo(); if(am!=null)am.getMemoryInfo(mi);
        long availMb=mi.availMem/(1024L*1024L), totalMb=mi.totalMem/(1024L*1024L);
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE); int thermal=(Build.VERSION.SDK_INT>=29&&pm!=null)?pm.getCurrentThermalStatus():-1;
        float maxHz=getMaxRefreshRate(); String pubg=findInstalledPubgPackage()==null?"Not detected":"Installed";
        StringBuilder s=new StringBuilder(); s.append(model).append("\nAndroid ").append(Build.VERSION.RELEASE).append(" • API ").append(Build.VERSION.SDK_INT).append("\nBattery: ").append(batteryPct>=0?batteryPct+"%":"Unknown");
        if(tempC>=0)s.append(" • ").append(String.format(Locale.US,"%.1f°C",tempC));
        s.append("\nThermal status: ").append(thermalName(thermal)).append("\nAvailable RAM: ").append(availMb).append(" MB / ").append(totalMb).append(" MB\nHighest display mode: ").append(String.format(Locale.US,"%.0f Hz",maxHz)).append("\nPUBG Mobile: ").append(pubg);
        statsText.setText(s.toString());
    }
    private void refreshAccess(){
        if(accessText==null)return; boolean display=Settings.System.canWrite(this); NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE); boolean dnd=nm!=null&&nm.isNotificationPolicyAccessGranted();
        accessText.setText((display?"✓":"○")+" Display / brightness write access\n"+(dnd?"✓":"○")+" Do Not Disturb access\n\n"+(display&&dnd?"Ready for the full Competitive profile.":"PUBG still launches without these permissions; grant only the tweaks you want."));
    }
    private void refreshSession(){ if(sessionText==null)return; boolean active=prefs.getBoolean("session_active",false); sessionText.setText(active?"● COMPETITIVE SESSION ACTIVE\nOriginal display/DND values are saved.":"● READY\nCompetitive profile is currently inactive."); sessionText.setTextColor(active?warning:accent); }
    private float getMaxRefreshRate(){ try{Display d=getWindowManager().getDefaultDisplay();float max=d.getRefreshRate();for(Display.Mode m:d.getSupportedModes())if(m.getRefreshRate()>max)max=m.getRefreshRate();return max;}catch(Exception e){return 60f;} }
    private String thermalName(int status){ if(Build.VERSION.SDK_INT<29||status<0)return"Unavailable"; switch(status){case PowerManager.THERMAL_STATUS_NONE:return"Normal";case PowerManager.THERMAL_STATUS_LIGHT:return"Light";case PowerManager.THERMAL_STATUS_MODERATE:return"Moderate";case PowerManager.THERMAL_STATUS_SEVERE:return"Severe";case PowerManager.THERMAL_STATUS_CRITICAL:return"Critical";case PowerManager.THERMAL_STATUS_EMERGENCY:return"Emergency";case PowerManager.THERMAL_STATUS_SHUTDOWN:return"Shutdown";default:return"Unknown";} }
    private TextView tv(String value,int sp,int colour,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextColor(colour);v.setTextSize(sp);v.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));return v;}
    private LinearLayout card(View child,int colour){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(15),dp(16),dp(15));GradientDrawable g=new GradientDrawable();g.setColor(colour);g.setCornerRadius(dp(18));g.setStroke(dp(1),Color.rgb(38,47,57));box.setBackground(g);box.addView(child,new LinearLayout.LayoutParams(-1,-2));return box;}
    private Button button(String value,int colour,int textColour){Button b=new Button(this);b.setText(value);b.setTextSize(14);b.setTextColor(textColour);b.setAllCaps(false);b.setTypeface(Typeface.create("sans",Typeface.BOLD));b.setGravity(Gravity.CENTER);b.setPadding(dp(12),0,dp(12),0);GradientDrawable g=new GradientDrawable();g.setColor(colour);g.setCornerRadius(dp(16));g.setStroke(dp(1),colour==accent?accent:Color.rgb(46,57,68));b.setBackground(g);return b;}
    private LinearLayout.LayoutParams lpCard(){return new LinearLayout.LayoutParams(-1,-2);}
    private LinearLayout.LayoutParams lpButton(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.topMargin=dp(10);return p;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
}
