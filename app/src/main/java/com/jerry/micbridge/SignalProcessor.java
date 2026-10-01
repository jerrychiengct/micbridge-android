package com.jerry.micbridge;

import java.util.Arrays;

/** Streaming mono effects followed by peak limiting and stereo duplication. No per-block allocations. */
public final class SignalProcessor {
    private final int rate;
    private final VoiceEqualizer eq;
    private float speechEnvelope;
    private final float[] pitchBuffer, echoBuffer, work=new float[2048];
    private int pitchWrite, echoWrite;
    private float limiter=1f, low, hpIn, hpOut;
    private double pitchPhase, robotPhase;
    private boolean wasMuted=true;
    private EffectSettings previous=EffectSettings.natural();
    public SignalProcessor() { this(48000); }
    public SignalProcessor(int rate) {
        if(rate<8000 || rate>192000) throw new IllegalArgumentException("Unsupported sample rate");
        this.rate=rate; eq=new VoiceEqualizer(rate); pitchBuffer=new float[rate/10]; echoBuffer=new float[rate];
    }
    public float process(short[] input,int count,short[] output,float gain,boolean muted) {
        return process(input,count,output,gain,muted,EffectSettings.natural());
    }
    public float process(short[] input,int count,short[] output,float gain,boolean muted,EffectSettings fx) {
        if(count<0 || count>input.length || count>work.length || output.length<count*2) throw new IllegalArgumentException("Invalid audio block");
        if(fx==null) throw new IllegalArgumentException("Missing effect settings");
        gain=Float.isNaN(gain)?1f:Math.max(0.1f,Math.min(3f,gain));
        float rawPeak=0;
        for(int i=0;i<count;i++) rawPeak=Math.max(rawPeak,Math.abs((int)input[i])/32768f);
        if(muted) {
            if(!wasMuted) reset();
            wasMuted=true; Arrays.fill(output,0,count*2,(short)0); return rawPeak;
        }
        wasMuted=false;
        if(previous.enabled!=fx.enabled || previous.clarity!=fx.clarity || previous.speech!=fx.speech || previous.pitch!=fx.pitch || previous.echoMillis!=fx.echoMillis || (previous.echo>0 && fx.echo==0)) reset();
        previous=fx;
        if(fx.enabled) eq.configure(fx.bass,fx.mid,fx.treble);
        float attack=(float)Math.exp(-1.0/(rate*0.010)), release=(float)Math.exp(-1.0/(rate*0.180));
        double ratio=Math.pow(2,fx.pitch/12.0), window=rate*0.04;
        double robotStep=2*Math.PI*70/rate;
        float alpha=(float)(1-Math.exp(-2*Math.PI*600/rate));
        float hpAlpha=(float)Math.exp(-2*Math.PI*90/rate);
        float bright=(float)Math.pow(10,fx.tone/20), warm=(float)Math.pow(10,-fx.tone/20);
        int echoDelay=Math.round(rate*fx.echoMillis/1000f);
        float effectedPeak=0;
        for(int i=0;i<count;i++) {
            float x=input[i]/32768f;
            if(fx.enabled) {
                if(fx.clarity) { float h=hpAlpha*(hpOut+x-hpIn); hpIn=x; hpOut=h; x=h; }
                low+=alpha*(x-low); if(fx.tone!=0) x=low*warm+(x-low)*bright;
                x=eq.process(x);
                pitchBuffer[pitchWrite]=x;
                if(fx.pitch!=0) {
                    pitchPhase+=(1-ratio)/window; pitchPhase-=Math.floor(pitchPhase);
                    double second=pitchPhase+0.5; if(second>=1) second-=1;
                    float blend=(float)(0.5-0.5*Math.cos(2*Math.PI*pitchPhase));
                    x=readPitch(2+pitchPhase*window)*blend+readPitch(2+second*window)*(1-blend);
                }
                pitchWrite=(pitchWrite+1)%pitchBuffer.length;
                if(fx.growl>0) {
                    float textured=(float)Math.tanh(x*(1+fx.growl*10));
                    x=x*(1-fx.growl)+textured*fx.growl;
                }
                if(fx.speech) {
                    float magnitude=Math.abs(x), coefficient=magnitude>speechEnvelope?attack:release;
                    speechEnvelope=coefficient*speechEnvelope+(1-coefficient)*magnitude;
                    if(speechEnvelope>0.126f) x*=(float)Math.pow(0.126f/speechEnvelope,0.6);
                }
                if(fx.robot>0) x*=1-fx.robot+fx.robot*(float)Math.sin(robotPhase);
                robotPhase+=robotStep; if(robotPhase>=2*Math.PI) robotPhase-=2*Math.PI;
                if(fx.echo>0) {
                    int tap=(echoWrite-echoDelay+echoBuffer.length)%echoBuffer.length;
                    float delayed=echoBuffer[tap]; echoBuffer[echoWrite]=x+delayed*0.22f;
                    x+=delayed*fx.echo; echoWrite=(echoWrite+1)%echoBuffer.length;
                }
            }
            work[i]=x*gain; effectedPeak=Math.max(effectedPeak,Math.abs(work[i]));
        }
        float target=effectedPeak>0.9f?0.9f/effectedPeak:1f;
        limiter=target<limiter?target:Math.min(target,limiter+0.01f);
        for(int i=0;i<count;i++) {
            int value=Math.round(work[i]*limiter*32768);
            short sample=(short)Math.max(-29490,Math.min(29490,value));
            output[i*2]=sample; output[i*2+1]=sample;
        }
        return rawPeak;
    }
    private float readPitch(double delay) {
        double at=pitchWrite-delay; if(at<0) at+=pitchBuffer.length;
        int index=(int)at; double fraction=at-index;
        return (float)(pitchBuffer[index]*(1-fraction)+pitchBuffer[(index+1)%pitchBuffer.length]*fraction);
    }
    private void reset() {
        Arrays.fill(pitchBuffer,0); Arrays.fill(echoBuffer,0); eq.reset(); speechEnvelope=0;
        pitchWrite=echoWrite=0; pitchPhase=robotPhase=0; low=hpIn=hpOut=0; limiter=1;
    }
}
