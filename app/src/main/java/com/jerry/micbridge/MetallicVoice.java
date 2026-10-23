package com.jerry.micbridge;

import java.util.Arrays;

/** Original causal metallic layer: parallel damped combs and dual ring sidebands.
 * A dry component preserves consonants. No FFT, look-ahead or per-block allocations. */
final class MetallicVoice {
    private final float[][] combs;
    private final int[] positions=new int[3];
    private final float[] damping=new float[3];
    private final float dampAlpha, blendAlpha;
    private final double stepA,stepB;
    private double phaseA,phaseB;
    private float body,blend;
    MetallicVoice(int rate){
        combs=new float[][]{new float[Math.max(1,Math.round(rate*0.0067f))],new float[Math.max(1,Math.round(rate*0.0109f))],new float[Math.max(1,Math.round(rate*0.0163f))]};
        dampAlpha=(float)(1-Math.exp(-2*Math.PI*Math.min(3400,rate*0.35)/rate));
        blendAlpha=(float)(1-Math.exp(-1.0/(rate*0.005)));
        stepA=2*Math.PI*47/rate;stepB=2*Math.PI*89/rate;
    }
    float process(float x,float amount){
        blend+=blendAlpha*(amount-blend);
        float resonant=0;
        for(int c=0;c<3;c++){
            float delayed=combs[c][positions[c]];
            damping[c]+=dampAlpha*(delayed-damping[c]);
            // Alternating signs and non-integer-related delays avoid a single flute-like note.
            float feedback=c==1?-0.34f:0.39f;
            combs[c][positions[c]]=x+damping[c]*feedback;
            positions[c]=(positions[c]+1)%combs[c].length;
            resonant+=delayed;
        }
        body+=dampAlpha*(x-body);
        float carrier=(float)(0.62*Math.sin(phaseA)+0.38*Math.sin(phaseB));
        phaseA+=stepA;if(phaseA>=2*Math.PI)phaseA-=2*Math.PI;
        phaseB+=stepB;if(phaseB>=2*Math.PI)phaseB-=2*Math.PI;
        float wet=0.64f*x+0.12f*resonant+0.30f*body*carrier;
        return x+blend*(wet-x);
    }
    void reset(){for(float[] comb:combs)Arrays.fill(comb,0);Arrays.fill(positions,0);Arrays.fill(damping,0);phaseA=phaseB=0;body=blend=0;}
}
