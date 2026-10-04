import com.jerry.micbridge.*;
import java.util.*;

public final class LatencyQualityTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static short[] render(short[] signal,int rate,int block){
        SignalProcessor dsp=new SignalProcessor(rate);short[] input=new short[block],output=new short[block*2],result=new short[signal.length];
        for(int at=0;at<signal.length;at+=block){int n=Math.min(block,signal.length-at);System.arraycopy(signal,at,input,0,n);dsp.process(input,n,output,2,false);for(int i=0;i<n;i++)result[at+i]=output[i*2];}return result;
    }
    public static void main(String[] args){
        check(AudioRoutePlan.rates(new int[]{44100,48000},new int[]{44100,48000},44100)[0]==44100,"Use a shared native rate");
        check(AudioRoutePlan.rates(new int[]{16000},new int[]{16000,48000},48000)[0]==16000,"Respect fixed-format microphones");
        check(AudioRoutePlan.blockFrames(48000,true,192)==192,"Use native 4 ms burst");
        check(AudioRoutePlan.blockFrames(48000,true,4096)==240,"Reject huge native burst");
        check(AudioRoutePlan.blockFrames(48000,true,0)==240,"Missing burst fallback");
        check(AudioRoutePlan.blockFrames(48000,false,192)==480,"Balanced blocks unchanged");

        LiveCaptureQueue queue=new LiveCaptureQueue(8,1);short[] result=new short[8];
        queue.offer(new short[]{1,2,3,4,5},5);check(queue.read(result,3)==3&&result[2]==3,"FIFO order");
        queue.offer(new short[]{6,7,8,9,10,11,12},7);check(queue.size()==8&&queue.discardedFrames()==1,"Bounded overflow count");
        check(queue.read(result,8)==8&&result[0]==5&&result[7]==12,"Keep newest speech under overload");
        queue.offer(new short[]{1,2,3,4,5,6,7,8,9,10},10);queue.read(result,8);check(result[0]==3&&result[7]==10&&queue.discardedFrames()==3,"Oversized producer burst");
        check(queue.read(result,8)==0,"Empty queue nonblocking");
        Random random=new Random(501);ArrayDeque<Short> reference=new ArrayDeque<>();queue=new LiveCaptureQueue(31,1);long drops=0;
        for(int k=0;k<20000;k++){
            if(random.nextBoolean()){
                int n=random.nextInt(65);short[] input=new short[n];for(int i=0;i<n;i++){input[i]=(short)random.nextInt();reference.add(input[i]);}
                while(reference.size()>31){reference.remove();drops++;}queue.offer(input,n);
            }else{
                int n=random.nextInt(32);short[] output=new short[n];int expected=Math.min(n,reference.size());check(queue.read(output,n)==expected,"Random read count");for(int i=0;i<expected;i++)check(output[i]==reference.remove(),"Random bounded FIFO content");
            }
            check(queue.size()==reference.size()&&queue.discardedFrames()==drops,"Random backlog accounting");
        }
        LiveCaptureQueue smoothed=new LiveCaptureQueue(8,4);short[] high=new short[8];Arrays.fill(high,(short)10000);smoothed.offer(high,8);smoothed.read(result,8);
        short[] low=new short[12];Arrays.fill(low,(short)-10000);smoothed.offer(low,12);smoothed.read(result,8);
        check(result[0]==5000&&result[3]==-10000&&result[7]==-10000,"Recovery eases discontinuity over four frames");

        OutputBufferTuner tuner=new OutputBufferTuner(480,240,3840,0);int current=480;
        check(tuner.update(current,false,5000)==480,"Do not shrink baseline");
        current=tuner.update(current,true,6000);check(current==720,"Grow promptly on underrun");
        check(tuner.update(current,false,15999)==720,"Wait for stable interval");
        current=tuner.update(current,false,16000);check(current==480,"Recover low buffering after stability");
        current=tuner.update(current,true,17000);check(current==720,"Undo a failed downwards probe");
        check(tuner.update(current,false,99000)==720,"Do not repeatedly probe below learned floor");
        for(int i=0;i<100;i++)current=tuner.update(current,true,100000+i*1000);check(current==3840,"Capacity bound");
        OutputBufferTuner small=new OutputBufferTuner(100,240,120,0);check(small.update(100,true,1000)==120,"Capacity smaller than step");

        for(int rate:new int[]{8000,44100,48000,96000,192000}){
            short[] signal=new short[rate];for(int i=0;i<signal.length;i++)signal[i]=(short)(9000*Math.sin(2*Math.PI*170*i/rate));
            for(int i=rate/10;i<signal.length;i+=rate/5)signal[i]=32767;
            short[] baseline=render(signal,rate,1);
            for(int block:new int[]{96,192,240,480,1920})check(Arrays.equals(baseline,render(signal,rate,block)),"Limiter independent of block boundaries: "+rate+" / "+block);
            for(short sample:baseline)check(Math.abs((int)sample)<=29490,"Causal limiter ceiling");
        }
        short[] pulse={1000,1000,32767},out=new short[6];new SignalProcessor().process(pulse,3,out,1,false);
        check(out[0]==1000&&out[2]==1000,"Later peak does not attenuate earlier speech");
        System.out.println("PASS: native-rate/burst planning, 20,000 randomized bounded FIFO operations, stale capture recovery and smoothing, stable buffer tuning and failed-probe hysteresis; limiter block invariance and ceiling at 8–192 kHz.");
    }
}
