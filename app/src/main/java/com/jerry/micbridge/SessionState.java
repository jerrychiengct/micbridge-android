package com.jerry.micbridge;

/** Main-thread session lifecycle, independent of Activity visibility and Android APIs. */
public final class SessionState {
    public enum Phase { IDLE, STARTING, MUTED, LIVE, CALIBRATING, STOPPING }
    private Phase phase=Phase.IDLE;
    private int inputId=-1,outputId=-1;
    private String reason="Ready when you are";
    public boolean begin(int input,int output){if(phase!=Phase.IDLE||input<0||output<0)return false;inputId=input;outputId=output;reason="Checking audio route";phase=Phase.STARTING;return true;}
    public boolean ready(){if(phase!=Phase.STARTING)return false;phase=Phase.MUTED;return true;}
    public boolean mute(boolean muted){if(phase!=Phase.MUTED&&phase!=Phase.LIVE)return false;phase=muted?Phase.MUTED:Phase.LIVE;return true;}
    public boolean calibrate(){if(phase!=Phase.MUTED&&phase!=Phase.LIVE)return false;phase=Phase.CALIBRATING;return true;}
    public boolean calibrated(){if(phase!=Phase.CALIBRATING)return false;phase=Phase.MUTED;return true;}
    public boolean stop(String why){if(phase==Phase.IDLE||phase==Phase.STOPPING)return false;reason=why;phase=Phase.STOPPING;return true;}
    public void stopped(String why){reason=why;phase=Phase.IDLE;}
    public Snapshot snapshot(){return new Snapshot(phase,inputId,outputId,reason);}
    public static final class Snapshot {
        public final Phase phase;
        public final int inputId,outputId;
        public final String reason;
        public final boolean active,ready,muted,calibrating,stopping;
        private Snapshot(Phase p,int input,int output,String why){phase=p;inputId=input;outputId=output;reason=why;active=p!=Phase.IDLE;ready=p==Phase.MUTED||p==Phase.LIVE||p==Phase.CALIBRATING;muted=p!=Phase.LIVE;calibrating=p==Phase.CALIBRATING;stopping=p==Phase.STOPPING;}
    }
}
