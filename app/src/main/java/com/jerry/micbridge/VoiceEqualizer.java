package com.jerry.micbridge;

/** RBJ low shelf, peaking mid and high shelf. Double precision filter state. */
final class VoiceEqualizer {
    private final int rate;
    private final Biquad bass=new Biquad(),mid=new Biquad(),treble=new Biquad();
    private float b,m,t; private boolean configured;
    VoiceEqualizer(int rate){this.rate=rate;}
    void configure(float bassDb,float midDb,float trebleDb) {
        // Ease live slider changes over several blocks; no audio-thread allocation.
        b=configured?approach(b,bassDb):bassDb;m=configured?approach(m,midDb):midDb;t=configured?approach(t,trebleDb):trebleDb;
        bass.configure(rate,180,b,0);mid.configure(rate,1500,m,1);treble.configure(rate,Math.min(4000,rate*0.4),t,2);configured=true;
    }
    private float approach(float a,float target){return Math.abs(a-target)<0.01f?target:a+(target-a)*0.25f;}
    float process(float x){return (float)treble.process(mid.process(bass.process(x)));}
    void reset(){bass.reset();mid.reset();treble.reset();configured=false;}
    private static final class Biquad {
        double b0=1,b1,b2,a1,a2,z1,z2;
        void configure(int rate,double hz,double db,int type){
            double A=Math.pow(10,db/40),w=2*Math.PI*hz/rate,c=Math.cos(w),s=Math.sin(w);
            double alpha=type==1?s/(2*0.8):s/2*Math.sqrt(2),root=2*Math.sqrt(A)*alpha;
            double n0,n1,n2,d0,d1,d2;
            if(type==0){n0=A*((A+1)-(A-1)*c+root);n1=2*A*((A-1)-(A+1)*c);n2=A*((A+1)-(A-1)*c-root);d0=(A+1)+(A-1)*c+root;d1=-2*((A-1)+(A+1)*c);d2=(A+1)+(A-1)*c-root;}
            else if(type==2){n0=A*((A+1)+(A-1)*c+root);n1=-2*A*((A-1)+(A+1)*c);n2=A*((A+1)+(A-1)*c-root);d0=(A+1)-(A-1)*c+root;d1=2*((A-1)-(A+1)*c);d2=(A+1)-(A-1)*c-root;}
            else{n0=1+alpha*A;n1=-2*c;n2=1-alpha*A;d0=1+alpha/A;d1=-2*c;d2=1-alpha/A;}
            b0=n0/d0;b1=n1/d0;b2=n2/d0;a1=d1/d0;a2=d2/d0;
        }
        double process(double x){double y=b0*x+z1;z1=b1*x-a1*y+z2;z2=b2*x-a2*y;return y;}
        void reset(){z1=z2=0;}
    }
}
