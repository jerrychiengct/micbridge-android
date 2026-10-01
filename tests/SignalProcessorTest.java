import com.jerry.micbridge.SignalProcessor;

public class SignalProcessorTest {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) {
        SignalProcessor dsp = new SignalProcessor();
        short[] input = {0, 1200, -1200, 32767, -32768};
        short[] output = new short[input.length * 2];
        float peak = dsp.process(input, input.length, output, 3f, false);
        check(peak == 1f, "Negative full scale is measured correctly");
        for (int i = 0; i < input.length; i++) {
            check(output[2*i] == output[2*i+1], "Stereo channels must match");
            check(Math.abs((int)output[2*i]) <= 29490, "Limiter ceiling exceeded");
        }
        dsp.process(input,input.length,output,1f,true);
        for(short s : output) check(s == 0,"Muted block must be silent");
        SignalProcessor unity = new SignalProcessor();
        short[] quiet = {0, 1000, -1000}; short[] q = new short[6];
        unity.process(quiet,3,q,1f,false);
        check(q[2]==1000 && q[4]==-1000,"Unity gain must preserve quiet audio");
        unity.process(quiet,3,q,0.5f,false);
        check(q[2]==500 && q[4]==-500,"Gain adjustment must scale correctly");
        boolean rejected = false;
        try { unity.process(quiet,4,q,1f,false); } catch(IllegalArgumentException e) { rejected=true; }
        check(rejected,"Invalid block length must be rejected");
        System.out.println("PASS: limiter, mute, stereo mapping, gain and bounds");
    }
}
