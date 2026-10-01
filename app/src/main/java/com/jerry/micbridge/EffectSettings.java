package com.jerry.micbridge;

/** Immutable settings snapshot shared between UI and audio threads. */
public final class EffectSettings {
    public final boolean enabled, clarity;
    public final float pitch, tone, robot, echo;
    public final int echoMillis;
    public EffectSettings(boolean enabled, boolean clarity, float pitch, float tone, float robot, float echo, int echoMillis) {
        this.enabled=enabled; this.clarity=clarity;
        this.pitch=bound(pitch,-6,6); this.tone=bound(tone,-6,6);
        this.robot=bound(robot,0,1); this.echo=bound(echo,0,0.5f);
        this.echoMillis=Math.max(100,Math.min(600,echoMillis));
    }
    private static float bound(float x,float low,float high) { return Float.isNaN(x) ? low : Math.max(low,Math.min(high,x)); }
    public static EffectSettings natural() { return new EffectSettings(true,false,0,0,0,0,250); }
}
