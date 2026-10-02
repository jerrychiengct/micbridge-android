package com.jerry.micbridge;

/** Original tunings based on standard voice-processing practices; no proprietary preset assets. */
public final class VoicePresets {
    public static final String[] TITLES={"Natural","Clear voice","Deep voice","Bright voice","Robot","Room echo","Lecture","Vigilante","Studio Speech","Broadcast","Warm Narrator","Crisp Presenter","Soft Spoken","Small Room","Cyber Pilot","Cinematic"};
    public static final String[] HINTS={"Original voice","Speech clarity","Lower pitch","Higher pitch","Metallic texture","Spacious repeats","Clear, steady speech","Deep, gritty hero","Balanced dialogue","Radio presence","Warm storytelling","Clear consonants","Gentle level assist","Less boom & hiss","Sci-fi character","Deep trailer texture"};
    public static EffectSettings get(int index){
        switch(index){
            case 1:return voice(0,2,0,0,0,0,false,0,0,250,0.25f,0.25f,false);
            case 2:return voice(-4,-2,0,0,0,0,false,0,0,250,0,0,false);
            case 3:return voice(4,2,0,0,0,0,false,0,0,250,0,0,false);
            case 4:return voice(0,0,0,0,0,0,false,0.85f,0,250,0,0,false);
            case 5:return voice(0,0,0,0,0,0,false,0,0.30f,250,0,0,false);
            case 6:return voice(0,0,-2,3,1,0,true,0,0,250,0.45f,0.35f,true);
            case 7:return voice(-5,-1,4,-1,-2,0.35f,false,0,0,250,0.25f,0.20f,false);
            case 8:return voice(0,0,-1,2,1,0,true,0,0,250,0.35f,0.40f,true);
            case 9:return voice(0,-1,2,2,0,0.08f,true,0,0,250,0.40f,0.45f,true);
            case 10:return voice(0,-1,2,1,-1,0,true,0,0,250,0.30f,0.35f,true);
            case 11:return voice(0,1,-3,3,2,0,true,0,0,250,0.40f,0.55f,true);
            case 12:return voice(0,0,-1,2,1,0,true,0,0,250,0.30f,0.25f,true);
            case 13:return voice(0,0,-4,2,-2,0,true,0,0,250,0.55f,0.50f,false);
            case 14:return voice(-2,1,-2,2,1,0.15f,true,0.45f,0.12f,150,0.35f,0.30f,false);
            case 15:return voice(-3,-1,3,1,-1,0.20f,true,0,0.10f,200,0.35f,0.35f,false);
            default:return EffectSettings.natural();
        }
    }
    private static EffectSettings voice(float pitch,float tone,float bass,float mid,float treble,float grit,boolean speech,float robot,float echo,int delay,float noise,float deEss,boolean assist){return new EffectSettings(true,noise>0,pitch,tone,bass,mid,treble,grit,speech,robot,echo,delay,noise,deEss,assist);}
}
