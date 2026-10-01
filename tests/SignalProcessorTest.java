import com.jerry.micbridge.*;
import java.io.*;
import java.util.*;

public class SignalProcessorTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static final int RATE=48000,BLOCK=480;
    public static void main(String[] args)throws Exception {
        SignalProcessor dsp=new SignalProcessor();short[] input={0,1200,-1200,32767,-32768};short[] output=new short[10];
        check(dsp.process(input,5,output,3,false)==1f,"Negative full scale meter");
        for(int i=0;i<5;i++){check(output[i*2]==output[i*2+1],"Stereo equality");check(Math.abs((int)output[i*2])<=29490,"Limiter ceiling");}
        dsp.process(input,5,output,1,true);for(short s:output)check(s==0,"Mute silence");
        SignalProcessor unity=new SignalProcessor();short[] quiet={0,1000,-1000};short[] q=new short[6];unity.process(quiet,3,q,1,false);check(q[2]==1000&&q[4]==-1000,"Natural voice preserved");
        unity.process(quiet,3,q,0.5f,false);check(q[2]==500&&q[4]==-500,"Gain scaling");
        boolean invalid=false;try{unity.process(quiet,4,q,1,false);}catch(IllegalArgumentException e){invalid=true;}check(invalid,"Bounds validation");
        EffectSettings wet=new EffectSettings(true,false,0,0,0,0.5f,250);
        short[] impulse=new short[RATE];impulse[0]=10000;
        short[] echoed=render(impulse,wet,RATE);
        check(echoed[0]==10000,"Echo dry signal");check(Math.abs(echoed[12000]-5000)<=1,"Echo arrival at selected delay");check(Math.abs(echoed[24000]-1100)<=1,"Echo feedback decays");
        SignalProcessor tail=new SignalProcessor(RATE);short[] in=new short[BLOCK],out=new short[BLOCK*2];in[0]=10000;tail.process(in,BLOCK,out,1,false,wet);Arrays.fill(in,(short)0);tail.process(in,BLOCK,out,1,true,wet);
        for(int n=0;n<60;n++){tail.process(in,BLOCK,out,1,false,wet);for(short s:out)check(s==0,"Muted echo tail must not return");}
        EffectSettings bypass=new EffectSettings(false,true,6,6,1,0.5f,600);
        check(Arrays.equals(render(quiet,bypass,RATE),quiet),"Bypass ignores all effects");
        Random random=new Random(2026);
        for(int sampleRate:new int[]{44100,48000}) {
            SignalProcessor stress=new SignalProcessor(sampleRate);
            EffectSettings extreme=new EffectSettings(true,true,-6,6,12,12,12,1,true,1,0.5f,600);
            for(int n=0;n<250;n++){for(int i=0;i<in.length;i++)in[i]=(short)random.nextInt();stress.process(in,BLOCK,out,3,n%31==0,extreme);for(int i=0;i<BLOCK;i++){check(out[i*2]==out[i*2+1],"FX stereo equality");check(Math.abs((int)out[i*2])<=29490,"FX overload bounded");}}
        }
        for(int sampleRate:new int[]{44100,48000}) {
            for(int band=0;band<3;band++){
                double hz=band==0?60:band==1?1500:8000;
                short[] sine=sine(sampleRate,hz,1000);
                double reference=rms(render(sine,EffectSettings.natural(),sampleRate));
                float b=band==0?6:0,m=band==1?6:0,t=band==2?6:0;
                double boost=rms(render(sine,new EffectSettings(true,false,0,0,b,m,t,0,false,0,0,250),sampleRate))/reference;
                double cut=rms(render(sine,new EffectSettings(true,false,0,0,-b,-m,-t,0,false,0,0,250),sampleRate))/reference;
                check(boost>1.75&&boost<2.1,"EQ boost at band "+band+": "+boost);
                check(cut>0.47&&cut<0.58,"EQ cut at band "+band+": "+cut);
            }
            short[] quietSpeech=sine(sampleRate,1000,2000),loudSpeech=sine(sampleRate,1000,16000);
            EffectSettings leveling=new EffectSettings(true,false,0,0,0,0,0,0,true,0,0,250);
            double dynamicRatio=rms(render(loudSpeech,leveling,sampleRate))/rms(render(quietSpeech,leveling,sampleRate));
            check(dynamicRatio>2&&dynamicRatio<5,"Speech compression should reduce 8x amplitude ratio: "+dynamicRatio);
            check(Arrays.equals(render(quietSpeech,EffectSettings.natural(),sampleRate),quietSpeech),"Neutral EQ unity");
            short[] gritty=render(quietSpeech,new EffectSettings(true,false,0,0,0,0,0,0.5f,false,0,0,250),sampleRate);
            check(!Arrays.equals(gritty,quietSpeech),"Grit changes voice");
            for(EffectSettings preset:new EffectSettings[]{EffectSettings.lecture(),EffectSettings.vigilante()}) {
                short[] effected=render(loudSpeech,preset,sampleRate);check(rms(effected)>100,"Preset audible");
                for(short v:effected)check(Math.abs((int)v)<=29490,"Preset limiter");
            }
            EffectSettings allBypassed=new EffectSettings(false,true,-6,6,12,-12,12,1,true,1,0.5f,600);
            check(Arrays.equals(render(quietSpeech,allBypassed,sampleRate),quietSpeech),"New effects respect bypass");
            SignalProcessor cleared=new SignalProcessor(sampleRate);short[] block=new short[BLOCK],stereo=new short[BLOCK*2];Arrays.fill(block,(short)10000);
            cleared.process(block,BLOCK,stereo,1,false,EffectSettings.lecture());cleared.process(block,BLOCK,stereo,1,true,EffectSettings.lecture());Arrays.fill(block,(short)0);
            cleared.process(block,BLOCK,stereo,1,false,EffectSettings.lecture());for(short v:stereo)check(v==0,"Mute clears EQ state");
        }
        if(args.length>0) {
            File dir=new File(args[0]);dir.mkdirs();short[] sine=new short[RATE*3];for(int i=0;i<sine.length;i++)sine[i]=(short)(3000*Math.sin(2*Math.PI*220*i/RATE));
            write(dir,"natural",render(sine,EffectSettings.natural(),RATE));
            write(dir,"pitch-up",render(sine,new EffectSettings(true,false,6,0,0,0,250),RATE));
            write(dir,"pitch-down",render(sine,new EffectSettings(true,false,-6,0,0,0,250),RATE));
            write(dir,"robot",render(sine,new EffectSettings(true,false,0,0,1,0,250),RATE));
        }
        System.out.println("PASS: three-band EQ boost/cut, neutral EQ, speech leveling, grit, Lecture/Vigilante, new-effect bypass and mute state; natural, gain, mute, limiter, echo timing/decay/tail clearing, bypass, bounds and overload at 44.1/48 kHz");
    }
    static short[] sine(int rate,double hz,int amplitude){short[] s=new short[rate*2];for(int i=0;i<s.length;i++)s[i]=(short)(amplitude*Math.sin(2*Math.PI*hz*i/rate));return s;}
    static double rms(short[] s){double sum=0;int start=s.length/2;for(int i=start;i<s.length;i++)sum+=(double)s[i]*s[i];return Math.sqrt(sum/(s.length-start));}
    static short[] render(short[] signal,EffectSettings fx,int rate){
        SignalProcessor dsp=new SignalProcessor(rate);short[] result=new short[signal.length],in=new short[BLOCK],out=new short[BLOCK*2];
        for(int offset=0;offset<signal.length;offset+=BLOCK){int count=Math.min(BLOCK,signal.length-offset);System.arraycopy(signal,offset,in,0,count);dsp.process(in,count,out,1,false,fx);for(int i=0;i<count;i++)result[offset+i]=out[i*2];}return result;
    }
    static void write(File dir,String name,short[] samples)throws Exception{try(DataOutputStream s=new DataOutputStream(new FileOutputStream(new File(dir,name+".pcm")))){for(short v:samples){s.writeByte(v&255);s.writeByte((v>>8)&255);}}}
}
