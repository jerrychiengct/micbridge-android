import com.jerry.micbridge.*;

public final class SessionThemeTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static double channel(int c){double v=c/255.0;return v<=0.04045?v/12.92:Math.pow((v+0.055)/1.055,2.4);}
    static double luminance(int rgb){return .2126*channel((rgb>>16)&255)+.7152*channel((rgb>>8)&255)+.0722*channel(rgb&255);}
    static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    public static void main(String[] args){
        SessionState s=new SessionState();check(!s.snapshot().active&&s.snapshot().muted,"Idle cannot emit live voice");
        check(!s.begin(-1,5),"Missing microphone rejected");check(!s.begin(5,-1),"Missing output rejected");
        check(s.begin(7,9),"Explicit session starts");check(!s.mute(false),"Cannot unmute before route verification");check(!s.begin(8,10),"No second session while starting");
        check(s.ready()&&s.snapshot().muted,"Route verification ends muted");check(s.mute(false)&&!s.snapshot().muted,"Explicit unmute becomes live");
        SessionState.Snapshot beforeLock=s.snapshot();check(beforeLock.active&&beforeLock.inputId==7&&beforeLock.outputId==9,"UI can reconnect using actual session identity");
        check(s.calibrate()&&s.snapshot().muted,"Calibration mutes live output");check(!s.mute(false),"Cannot unmute while calibrating");check(s.calibrated()&&s.snapshot().muted,"Calibration never auto-unmutes");
        check(s.mute(false),"Explicit return to live");check(s.stop("Device disconnected"),"Disconnect stops session");
        check(s.snapshot().muted&&!s.snapshot().ready&&s.snapshot().stopping,"Stopping is muted and disables ready actions");
        check(!s.ready()&&!s.calibrated()&&!s.mute(false),"Late callbacks cannot revive stopping audio");check(!s.begin(7,9),"Must await worker shutdown before restarting");
        s.stopped("Device disconnected");check(!s.snapshot().active&&s.snapshot().reason.equals("Device disconnected"),"Shutdown preserves reason");
        check(s.begin(20,21)&&s.ready()&&s.mute(false),"A fresh explicit session can start after shutdown");check(s.stop("Call interrupted"),"Focus interruption enters stopping");s.stopped("Call interrupted");check(!s.mute(false),"Focus regain cannot automatically restart microphone");
        for(int mode=0;mode<4;mode++)for(boolean night:new boolean[]{false,true}){
            ThemePalette p=ThemePalette.resolve(mode,night);
            check(contrast(p.ink,p.surface)>=4.5,"Body contrast "+mode);check(contrast(p.secondary,p.surface)>=4.5,"Secondary contrast "+mode);check(contrast(p.secondary,p.background)>=4.5,"Page subtitle contrast "+mode);
            check(contrast(p.accent,p.soft)>=4.5,"Selected label contrast "+mode);check(contrast(p.buttonText(p.ink),p.ink)>=4.5,"Primary action contrast "+mode);check(contrast(p.buttonText(p.accent),p.accent)>=4.5,"Accent action contrast "+mode);
            if(mode==0)check(p.dark==night,"Follow system appearance");if(mode==3)check(p.background==0xFF000000,"OLED background");
        }
        System.out.println("PASS: session start/route verification, immutable snapshots, explicit unmute, calibration, disconnect/focus stop, stale callback rejection and restart guards; system/light/dark/OLED text and action contrast.");
    }
}
