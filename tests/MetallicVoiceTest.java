import com.jerry.micbridge.*;
import java.util.*;

public final class MetallicVoiceTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static EffectSettings metal(float amount,boolean enabled){return new EffectSettings(enabled,false,0,0,0,0,0,0,false,0,0,250,0,0,false,amount);}
    static short[] render(short[] signal,int rate,int block,EffectSettings fx){
        SignalProcessor processor=new SignalProcessor(rate);short[] input=new short[block],output=new short[block*2],result=new short[signal.length];
        for(int at=0;at<signal.length;at+=block){int n=Math.min(block,signal.length-at);System.arraycopy(signal,at,input,0,n);processor.process(input,n,output,1,false,fx);for(int i=0;i<n;i++){check(output[i*2]==output[i*2+1],"Stereo equality");check(Math.abs((int)output[i*2])<=29490,"Limiter bound");result[at+i]=output[i*2];}}return result;
    }
    static double power(short[] samples,int rate,double hz){double real=0,imag=0;for(int i=rate/2;i<samples.length;i++){double angle=2*Math.PI*hz*i/rate;double window=0.5-0.5*Math.cos(2*Math.PI*(i-rate/2)/(samples.length-rate/2-1));real+=window*samples[i]*Math.cos(angle);imag+=window*samples[i]*Math.sin(angle);}return real*real+imag*imag;}
    public static void main(String[] args){
        check(VoicePresets.TITLES.length==17&&VoicePresets.TITLES[16].equals("Synthetic Overlord"),"New preset appended; old indices retained");
        check(VoicePresets.get(6).pitch==0&&VoicePresets.get(7).pitch==-5,"Speech and Vigilante tuning retained");
        check(metal(Float.NaN,true).metal==0&&metal(10,true).metal==1&&metal(-1,true).metal==0,"Metal amount bounded");
        for(int rate:new int[]{8000,16000,32000,44100,48000,96000,192000}){
            short[] sine=new short[rate],impulse=new short[rate];for(int i=0;i<rate;i++)sine[i]=(short)(2500*Math.sin(2*Math.PI*700*i/rate));impulse[0]=12000;
            check(Arrays.equals(sine,render(sine,rate,240,metal(0,true))),"Zero-metal unity at "+rate);
            check(Arrays.equals(sine,render(sine,rate,240,metal(1,false))),"Bypass ignores metal at "+rate);
            short[] wet=render(sine,rate,240,metal(1,true));check(!Arrays.equals(sine,wet),"Metal changes signal");
            check(Arrays.equals(wet,render(sine,rate,97,metal(1,true))),"Metal independent of block boundaries");
            check(power(wet,rate,653)>power(sine,rate,653)*100+1e6,"47 Hz lower ring sideband at "+rate+" wet="+power(wet,rate,653)+" dry="+power(sine,rate,653));
            check(power(wet,rate,789)>power(sine,rate,789)*100+1e6,"89 Hz upper ring sideband at "+rate+" wet="+power(wet,rate,789)+" dry="+power(sine,rate,789));
            short[] response=render(impulse,rate,240,metal(1,true));check(response[0]!=0,"Dry onset has no added block delay");
            for(int i=rate/2;i<rate;i++)check(response[i]==0,"Damped tail settles without oscillation");
            render(sine,rate,240,VoicePresets.get(16));
            SignalProcessor dsp=new SignalProcessor(rate);short[] in=new short[240],out=new short[480];Arrays.fill(in,(short)10000);
            dsp.process(in,240,out,1,false,VoicePresets.get(16));dsp.process(in,240,out,1,true,VoicePresets.get(16));Arrays.fill(in,(short)0);
            for(int k=0;k<Math.max(10,rate/240);k++){dsp.process(in,240,out,1,false,VoicePresets.get(16));for(short value:out)check(value==0,"Mute clears all synthetic layers");}
            dsp.process(in,240,out,1,false,metal(0,true));dsp.process(in,240,out,1,false,metal(1,true));for(short value:out)check(value==0,"No stale metallic tail on re-enable");
        }
        System.out.println("PASS: Synthetic Overlord selection and parameter bounds, metal bypass/unity, dry onset, dual spectral sidebands, block invariance, damped stability, stereo ceiling and mute clearing at 8–192 kHz.");
    }
}
