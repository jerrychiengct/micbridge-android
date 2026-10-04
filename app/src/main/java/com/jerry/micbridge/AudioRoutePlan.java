package com.jerry.micbridge;

import java.util.ArrayList;
import java.util.List;

/** Format negotiation and buffer arithmetic kept separate from Android device APIs. */
public final class AudioRoutePlan {
    public static int[] rates(int[] input,int[] output){
        return rates(input,output,0);
    }
    public static int[] rates(int[] input,int[] output,int nativeRate){
        List<Integer> result=new ArrayList<>();int[] preferred={48000,44100,32000,24000,16000,96000,22050,8000};
        if(supports(input,nativeRate)&&supports(output,nativeRate))add(result,nativeRate);
        for(int r:preferred)if(supports(input,r)&&supports(output,r))add(result,r);
        for(int r:output)if(supports(input,r))add(result,r);
        for(int r:input)add(result,r);
        // Android may convert to hardware formats even when advertised rates differ.
        for(int r:preferred)add(result,r);
        int[] values=new int[result.size()];for(int i=0;i<values.length;i++)values[i]=result.get(i);return values;
    }
    private static boolean supports(int[] rates,int r){if(rates.length==0)return true;for(int x:rates)if(x==r)return true;return false;}
    private static void add(List<Integer> list,int r){if(r>=8000&&r<=192000&&!list.contains(r))list.add(r);}
    public static int blockFrames(int rate,boolean fast){return Math.max(1,rate*(fast?5:10)/1000);}
    public static int blockFrames(int rate,boolean fast,int nativeBurst){
        if(fast&&nativeBurst>=Math.max(1,rate*2/1000)&&nativeBurst<=rate/100)return nativeBurst;
        return blockFrames(rate,fast);
    }
    public static int bufferFrames(int rate,boolean fast){return Math.max(blockFrames(rate,fast)*2,rate*(fast?10:40)/1000);}
    public static int growBuffer(int current,int block,int capacity){return (int)Math.min((long)capacity,(long)current+block*2L);}
    public static int downmix(short[] captured,int samples,int channels,short[] mono){
        if(channels<1||channels>2||samples<0||samples>captured.length||samples%channels!=0||samples/channels>mono.length)throw new IllegalArgumentException("Invalid capture block");
        int frames=samples/channels;for(int i=0;i<frames;i++)mono[i]=channels==1?captured[i]:(short)(((int)captured[i*2]+captured[i*2+1])/2);return frames;
    }
}
