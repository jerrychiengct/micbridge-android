package com.jerry.micbridge;

/** Immutable settings snapshot shared between UI and audio threads. */
public final class EffectSettings {
    public final boolean enabled, clarity, speech, autoLevel;
    public final float pitch, tone, bass, mid, treble, growl, robot, echo, noise, deEss;
    public final int echoMillis;
    public EffectSettings(boolean enabled, boolean clarity, float pitch, float tone, float robot, float echo, int echoMillis) {
        this(enabled,clarity,pitch,tone,0,0,0,0,false,robot,echo,echoMillis);
    }
    public EffectSettings(boolean enabled, boolean clarity, float pitch, float tone, float bass, float mid, float treble, float growl, boolean speech, float robot, float echo, int echoMillis) {
        this(enabled,clarity,pitch,tone,bass,mid,treble,growl,speech,robot,echo,echoMillis,0,0,false);
    }
    public EffectSettings(boolean enabled, boolean clarity, float pitch, float tone, float bass, float mid, float treble, float growl, boolean speech, float robot, float echo, int echoMillis, float noise, float deEss, boolean autoLevel) {
        this.noise=bound(noise,0,1);this.deEss=bound(deEss,0,1);this.autoLevel=autoLevel;
        this.enabled=enabled; this.clarity=clarity; this.speech=speech;
        this.pitch=bound(pitch,-6,6); this.tone=bound(tone,-6,6);
        this.bass=bound(bass,-12,12); this.mid=bound(mid,-12,12); this.treble=bound(treble,-12,12);
        this.growl=bound(growl,0,1); this.robot=bound(robot,0,1); this.echo=bound(echo,0,0.5f);
        this.echoMillis=Math.max(100,Math.min(600,echoMillis));
    }
    private static float bound(float x,float low,float high) { return Float.isNaN(x) ? 0 : Math.max(low,Math.min(high,x)); }
    public static EffectSettings natural() { return new EffectSettings(true,false,0,0,0,0,250); }
    public static EffectSettings lecture() { return new EffectSettings(true,true,0,0,-2,3,1,0,true,0,0,250); }
    public static EffectSettings vigilante() { return new EffectSettings(true,true,-5,-1,4,-1,-2,0.35f,false,0,0,250); }
}
