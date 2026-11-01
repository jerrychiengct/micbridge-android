package com.jerry.micbridge;

import android.content.*;
import android.os.IBinder;
import android.media.AudioDeviceInfo;

/** Activity connection only; closing it never closes an explicitly started audio session. */
final class AudioSessionClient {
    private final Context context;private final AudioSessionService.Listener listener;
    private AudioSessionService service;private boolean bound,visible;
    private EffectSettings effects=EffectSettings.natural();private float gain=1;private boolean fast=true;
    private final ServiceConnection connection=new ServiceConnection(){
        public void onServiceConnected(ComponentName name,IBinder binder){service=((AudioSessionService.LocalBinder)binder).service();if(!service.snapshot().active){service.setEffects(effects);service.setGain(gain);service.setPreferFast(fast);}if(visible)service.attach(listener);}
        public void onServiceDisconnected(ComponentName name){service=null;if(visible)listener.onStopped("Audio service disconnected. Start a new session.");}
        @Override public void onBindingDied(ComponentName name){service=null;if(bound){context.unbindService(this);bound=false;}if(visible)listener.onStopped("Audio service stopped. Reopen the app to retry.");}
    };
    AudioSessionClient(Context context,AudioSessionService.Listener listener){this.context=context;this.listener=listener;}
    void connect(){visible=true;if(service!=null){service.attach(listener);return;}if(!bound)bound=context.bindService(new Intent(context,AudioSessionService.class),connection,Context.BIND_AUTO_CREATE);}
    void hide(){visible=false;if(service!=null)service.detach(listener);}
    void close(){hide();if(bound){context.unbindService(connection);bound=false;}service=null;}
    boolean isConnected(){return service!=null;}
    boolean isRunning(){return service!=null&&service.snapshot().active;}
    SessionState.Snapshot snapshot(){return service==null?new SessionState().snapshot():service.snapshot();}
    void replay(){if(service!=null&&visible)service.replay();}
    boolean start(AudioDeviceInfo in,AudioDeviceInfo out){if(service==null||service.snapshot().active)return false;try{context.startForegroundService(new Intent(context,AudioSessionService.class).setAction(AudioSessionService.START).putExtra("input",in.getId()).putExtra("output",out.getId()));return true;}catch(RuntimeException error){listener.onStopped("Open MicBridge and allow microphone access before starting");return false;}}
    void stop(String reason){if(service!=null)service.stopSession(reason);}
    void setMuted(boolean value){if(service!=null)service.setMuted(value);}
    void calibrate(){if(service!=null)service.calibrate();}
    void setPreferFast(boolean value){fast=value;if(service!=null)service.setPreferFast(value);}
    void setGain(float value){gain=value;if(service!=null)service.setGain(value);}
    void setEffects(EffectSettings value){effects=value;if(service!=null)service.setEffects(value);}
}
