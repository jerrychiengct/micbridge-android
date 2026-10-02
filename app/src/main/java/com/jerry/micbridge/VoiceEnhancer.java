package com.jerry.micbridge;

/** Causal voice care: soft noise expansion, bounded level assist and split-band de-essing. */
final class VoiceEnhancer {
    private final float attack,release,levelAttack,levelRelease,splitAlpha,gainAttack,gainRelease;
    private float envelope,level,low,highEnvelope,totalEnvelope,assist=1,gate=1;
    private float noiseFloor=0.003f;
    VoiceEnhancer(int rate){attack=decay(rate,0.003);release=decay(rate,0.080);levelAttack=decay(rate,0.100);levelRelease=decay(rate,0.500);splitAlpha=1-decay(rate,1/(2*Math.PI*Math.min(3000,rate*0.35)));gainAttack=decay(rate,0.150);gainRelease=decay(rate,0.800);}
    private static float decay(int rate,double seconds){return (float)Math.exp(-1/(rate*seconds));}
    void setNoiseFloor(float value){if(Float.isFinite(value))noiseFloor=Math.max(0.0001f,Math.min(0.04f,value));}
    float process(float x,EffectSettings fx){
        float magnitude=Math.abs(x),a=magnitude>envelope?attack:release;envelope=a*envelope+(1-a)*magnitude;
        float threshold=Math.max(0.0003f,Math.min(0.03f,noiseFloor*1.8f));
        float desiredGate=fx.noise>0&&envelope<threshold?1-fx.noise*0.88f*(1-envelope/threshold):1;
        gate+=0.01f*(desiredGate-gate);
        float la=magnitude>level?levelAttack:levelRelease;level=la*level+(1-la)*magnitude;
        // Never amplify a floor-only input. Cap assist at +6 dB and avoid changing manual gain.
        float desiredGain=fx.autoLevel&&level>Math.max(threshold*2,0.006f)?Math.max(0.5f,Math.min(2f,0.10f/level)):1;
        float ga=desiredGain<assist?gainAttack:gainRelease;assist=ga*assist+(1-ga)*desiredGain;
        low+=splitAlpha*(x-low);float high=x-low;
        highEnvelope=release*highEnvelope+(1-release)*Math.abs(high);totalEnvelope=release*totalEnvelope+(1-release)*magnitude;
        float sibilance=Math.max(0,Math.min(1,(highEnvelope/(totalEnvelope+0.000001f)-0.25f)/0.45f));
        float reduction=fx.deEss*(highEnvelope>0.008f?sibilance:0)*0.75f;
        return (x-high*reduction)*gate*assist;
    }
    void reset(){envelope=level=low=highEnvelope=totalEnvelope=0;assist=gate=1;}
}
