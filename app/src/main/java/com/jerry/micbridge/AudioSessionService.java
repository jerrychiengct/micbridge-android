package com.jerry.micbridge;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.os.*;

/** Owns live audio across screen lock, Activity recreation and background transitions. */
public final class AudioSessionService extends Service implements AudioEngine.Listener {
    public interface Listener extends AudioEngine.Listener { void onSessionChanged(SessionState.Snapshot snapshot); }
    static final String START="micbridge.START",TOGGLE="micbridge.TOGGLE",STOP="micbridge.STOP";
    private static final String CHANNEL="live_voice";
    private static final int NOTIFICATION=41;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final SessionState state=new SessionState();
    private final LocalBinder binder=new LocalBinder();
    private AudioManager audio;private AudioEngine engine;private AudioFocusRequest focus;
    private PowerManager.WakeLock cpu;
    private Listener listener;
    private String route="",health="";private float noise=-1;
    private boolean foreground,destroyed;
    private long generation;
    private final Runnable renewCpu=new Runnable(){public void run(){if(state.snapshot().active&&!destroyed){cpu.acquire(10*60*1000L);main.postDelayed(this,5*60*1000L);}}};
    private final AudioDeviceCallback devices=new AudioDeviceCallback(){@Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removed){SessionState.Snapshot s=state.snapshot();for(AudioDeviceInfo d:removed)if(s.active&&(d.getId()==s.inputId||d.getId()==s.outputId)){stopSession("Selected device disconnected");break;}}};
    private final BroadcastReceiver noisy=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){stopSession("Audio output disconnected");}};
    public final class LocalBinder extends Binder { AudioSessionService service(){return AudioSessionService.this;} }
    @Override public void onCreate(){
        super.onCreate();audio=(AudioManager)getSystemService(AUDIO_SERVICE);engine=new AudioEngine(this,audio);
        cpu=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,getPackageName()+":live_voice");cpu.setReferenceCounted(false);
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Live microphone session",NotificationManager.IMPORTANCE_LOW);channel.setDescription("Mute or stop your live microphone, including when the screen is locked.");channel.setSound(null,null);getSystemService(NotificationManager.class).createNotificationChannel(channel);
        audio.registerAudioDeviceCallback(devices,main);
        IntentFilter filter=new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(noisy,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(noisy,filter);
    }
    @Override public IBinder onBind(Intent intent){return binder;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        String action=intent.getAction();
        if(START.equals(action))begin(intent.getIntExtra("input",-1),intent.getIntExtra("output",-1));
        else if(STOP.equals(action)){if(state.snapshot().active)stopSession("Session stopped");else stopSelf();}
        else if(TOGGLE.equals(action)){SessionState.Snapshot s=state.snapshot();if(s.active){AudioDeviceInfo out=find(s.outputId,false);if(out==null)stopSession("Selected speaker unavailable");else if(!s.muted||out.getType()!=AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)setMuted(!s.muted);}else stopSelf();}
        else if(!state.snapshot().active)stopSelf();
        return START_NOT_STICKY;
    }
    private AudioDeviceInfo find(int id,boolean source){for(AudioDeviceInfo d:audio.getDevices(AudioManager.GET_DEVICES_ALL))if(d.getId()==id&&(source?d.isSource():d.isSink()))return d;return null;}
    private void begin(int inputId,int outputId){
        if(!state.begin(inputId,outputId)){if(!state.snapshot().active){state.stopped("Choose an available microphone and speaker");publish();stopSelf();}return;}
        route=health="";noise=-1;final long token=++generation;
        try{
            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)throw new IllegalStateException("Microphone permission is required");
            int types=ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK;if(Build.VERSION.SDK_INT>=30)types|=ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION,notification(),types);else startForeground(NOTIFICATION,notification());foreground=true;
            AudioDeviceInfo in=find(inputId,true),out=find(outputId,false);
            if(in==null||out==null)throw new IllegalStateException("Selected device is no longer available");
            focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setWillPauseWhenDucked(true).setOnAudioFocusChangeListener(change->{if(token==generation&&change!=AudioManager.AUDIOFOCUS_GAIN)stopSession("Session stopped for another app or call");},main).build();
            if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)throw new IllegalStateException("Audio is busy. Stop other audio and retry");
            cpu.acquire(10*60*1000L);main.postDelayed(renewCpu,5*60*1000L);
            if(!engine.start(in,out))throw new IllegalStateException("Audio is still stopping. Please retry");
            publish();
        }catch(RuntimeException error){finish(error.getMessage()==null?"Could not start live audio":error.getMessage());}
    }
    void attach(Listener value){listener=value;replay();}
    void detach(Listener value){if(listener==value)listener=null;}
    void replay(){if(listener!=null){listener.onSessionChanged(state.snapshot());if(state.snapshot().ready){listener.onReady(route);if(noise>=0&&state.snapshot().muted&&!state.snapshot().calibrating)listener.onCalibration(noise);}if(!health.isEmpty())listener.onAudioHealth(health);}}
    SessionState.Snapshot snapshot(){return state.snapshot();}
    void setPreferFast(boolean value){engine.setPreferFast(value);}
    void setGain(float value){engine.setGain(value);}
    void setEffects(EffectSettings value){engine.setEffects(value);}
    void setMuted(boolean value){if(state.mute(value)){engine.setMuted(value);publish();}}
    void calibrate(){if(state.calibrate()){engine.calibrate();publish();}}
    void stopSession(String reason){if(state.stop(reason)){engine.stop(reason);publish();}}
    private void publish(){if(destroyed)return;if(foreground)getSystemService(NotificationManager.class).notify(NOTIFICATION,notification());if(listener!=null)listener.onSessionChanged(state.snapshot());}
    private PendingIntent action(String action,int request){return PendingIntent.getService(this,request,new Intent(this,AudioSessionService.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private Notification notification(){
        SessionState.Snapshot s=state.snapshot();PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String text=s.stopping?"Stopping microphone…":s.calibrating?"Calibrating · microphone muted":!s.ready?"Checking route · muted":s.muted?"Microphone listening · output muted":"Voice is live · microphone active";
        Notification.Builder builder=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_notification_mic).setContentTitle("MicBridge · live session").setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_SERVICE).setVisibility(Notification.VISIBILITY_PUBLIC);
        if(Build.VERSION.SDK_INT>=31)builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        if(s.ready&&!s.calibrating){AudioDeviceInfo out=null;try{out=find(s.outputId,false);}catch(SecurityException ignored){}boolean requireUi=s.muted&&out!=null&&out.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;builder.addAction(new Notification.Action.Builder(null,requireUi?"Open to unmute":s.muted?"Unmute":"Mute",requireUi?open:action(TOGGLE,1)).build());}
        builder.addAction(new Notification.Action.Builder(null,"Stop",action(STOP,2)).build());return builder.build();
    }
    private void releaseSession(){main.removeCallbacks(renewCpu);if(cpu!=null&&cpu.isHeld())cpu.release();if(focus!=null){audio.abandonAudioFocusRequest(focus);focus=null;}if(foreground){stopForeground(STOP_FOREGROUND_REMOVE);foreground=false;}}
    private void finish(String reason){generation++;releaseSession();state.stopped(reason);publish();stopSelf();}
    @Override public void onReady(String value){if(!state.ready())return;route=value;publish();if(listener!=null)listener.onReady(value);}
    @Override public void onMeter(float peak){if(listener!=null&&state.snapshot().ready)listener.onMeter(peak);}
    @Override public void onStopped(String reason){if(!destroyed)finish(reason);}
    @Override public void onAudioHealth(String value){health=value;if(listener!=null)listener.onAudioHealth(value);}
    @Override public void onCalibration(float value){if(!state.calibrated())return;noise=value;publish();if(listener!=null)listener.onCalibration(value);}
    @Override public void onTaskRemoved(Intent rootIntent){stopSession("Session stopped when app was dismissed");if(!state.snapshot().active)stopSelf();}
    @Override public void onDestroy(){destroyed=true;listener=null;engine.stop("Audio service closed");releaseSession();audio.unregisterAudioDeviceCallback(devices);unregisterReceiver(noisy);super.onDestroy();}
}
