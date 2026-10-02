import com.jerry.micbridge.*;
import java.util.*;

public final class VoiceCareTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static EffectSettings care(float noise,float deEss,boolean assist){return new EffectSettings(true,false,0,0,0,0,0,0,false,0,0,250,noise,deEss,assist);}
    static short[] sine(int rate,double hz,int amplitude){short[] s=new short[rate*4];for(int i=0;i<s.length;i++)s[i]=(short)(amplitude*Math.sin(2*Math.PI*hz*i/rate));return s;}
    static double rms(short[] s){double sum=0;for(int i=s.length/2;i<s.length;i++)sum+=(double)s[i]*s[i];return Math.sqrt(sum/(s.length-s.length/2));}
    static short[] render(short[] signal,int rate,EffectSettings settings,float floor,boolean fast){SignalProcessor dsp=new SignalProcessor(rate);dsp.setNoiseFloor(floor);int block=AudioRoutePlan.blockFrames(rate,fast);short[] in=new short[block],out=new short[block*2],result=new short[signal.length];for(int at=0;at<signal.length;at+=block){int n=Math.min(block,signal.length-at);System.arraycopy(signal,at,in,0,n);dsp.process(in,n,out,1,false,settings);for(int i=0;i<n;i++)result[at+i]=out[i*2];}return result;}
    public static void main(String[] args){
        check(AudioRoutePlan.rates(new int[]{16000},new int[]{16000,48000})[0]==16000,"Prefer shared mic/output rate");
        check(AudioRoutePlan.rates(new int[]{44100},new int[]{48000})[0]==44100,"Offer mic native rate before resampling fallbacks");
        check(AudioRoutePlan.rates(new int[0],new int[]{48000})[0]==48000,"Unspecified input supported");
        int[] rates=AudioRoutePlan.rates(new int[]{4000,384000,192000},new int[0]);Set<Integer> seen=new HashSet<>();for(int r:rates){check(r>=8000&&r<=192000,"Supported DSP rate range");check(seen.add(r),"No repeated format attempts");}
        short[] mono=new short[3];check(AudioRoutePlan.downmix(new short[]{32767,32767,-32768,-32768,32767,-32768},6,2,mono)==3,"Stereo frames");check(mono[0]==32767&&mono[1]==-32768&&mono[2]==0,"Downmix without integer overflow");
        boolean invalid=false;try{AudioRoutePlan.downmix(new short[3],3,2,mono);}catch(IllegalArgumentException e){invalid=true;}check(invalid,"Incomplete stereo frame rejected");
        check(AudioRoutePlan.growBuffer(900,480,1000)==1000,"Underrun recovery capped to capacity");
        check(VoicePresets.TITLES.length==16&&VoicePresets.HINTS.length==16,"Preset metadata");
        for(int rate:new int[]{8000,16000,32000,44100,48000,96000}){
            for(boolean fast:new boolean[]{true,false}){
                check(AudioRoutePlan.blockFrames(rate,fast)>0,"Usable processing block");
                short[] background=sine(rate,500,40),speech=sine(rate,1000,2000),quiet=sine(rate,1000,800);
                double noiseRatio=rms(render(background,rate,care(1,0,false),0.003f,fast))/rms(background);check(noiseRatio<0.45,"Soft noise reduction "+rate+": "+noiseRatio);
                double speechRatio=rms(render(speech,rate,care(1,0,false),0.003f,fast))/rms(speech);check(speechRatio>0.98,"Speech passes expander "+rate+": "+speechRatio);
                double assist=rms(render(quiet,rate,care(0,0,true),0.003f,fast))/rms(quiet);check(assist>1.7&&assist<2.05,"Bounded quiet voice assist "+rate+": "+assist);
                double floorAssist=rms(render(background,rate,care(0,0,true),0.003f,fast))/rms(background);check(floorAssist<1.02,"No noise-only boost");
                check(Arrays.equals(render(speech,rate,EffectSettings.natural(),0.003f,fast),speech),"Neutral path unity at negotiated rate");
                double calibrated=rms(render(sine(rate,500,300),rate,care(1,0,false),0.010f,fast));double uncalibrated=rms(render(sine(rate,500,300),rate,care(1,0,false),0.003f,fast));check(calibrated<uncalibrated*0.7,"Calibrated floor changes noise suppression");
            }
            for(int preset=0;preset<16;preset++){
                short[] processed=render(sine(rate,1000,12000),rate,VoicePresets.get(preset),0.003f,true);check(rms(processed)>80,"Preset audible "+preset+" / "+rate);for(short sample:processed)check(Math.abs((int)sample)<=29490,"Preset bounded");
            }
        }
        for(int rate:new int[]{44100,48000}){
            short[] high=sine(rate,8000,6000),low=sine(rate,1000,6000);double highRatio=rms(render(high,rate,care(0,1,false),0.003f,true))/rms(high);double lowRatio=rms(render(low,rate,care(0,1,false),0.003f,true))/rms(low);check(highRatio<0.55&&lowRatio>0.85,"De-esser selective high-frequency control "+highRatio+" / "+lowRatio);
            EffectSettings bypass=new EffectSettings(false,true,6,6,12,12,12,1,true,1,0.5f,250,1,1,true);check(Arrays.equals(render(low,rate,bypass,0.003f,true),low),"Bypass ignores voice care");
            SignalProcessor dsp=new SignalProcessor(rate);short[] in=new short[240],out=new short[480];Arrays.fill(in,(short)8000);dsp.process(in,in.length,out,1,false,VoicePresets.get(6));dsp.process(in,in.length,out,1,true,VoicePresets.get(6));Arrays.fill(in,(short)0);for(int k=0;k<30;k++){dsp.process(in,in.length,out,1,false,VoicePresets.get(6));for(short x:out)check(x==0,"Mute clears voice-care state");}
        }
        System.out.println("PASS: format negotiation, stereo downmix, bounded buffer recovery; 5/10 ms blocks, noise expansion, calibrated floor, no floor-only boost, level assist, all 16 presets at six sample rates; selective de-essing, bypass and mute.");
    }
}
