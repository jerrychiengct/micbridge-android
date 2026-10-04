package com.jerry.micbridge;

/** Worker-owned bounded mono FIFO. Under overload keep recent speech, never an unbounded delay. */
public final class LiveCaptureQueue {
    private final short[] samples;
    private final int fadeFrames;
    private int head, size, fadeLeft;
    private long discarded;
    private short last;
    private boolean gap;

    public LiveCaptureQueue(int capacity, int fadeFrames) {
        if (capacity < 1 || fadeFrames < 1) throw new IllegalArgumentException("Invalid capture budget");
        samples = new short[capacity]; this.fadeFrames = fadeFrames;
    }
    public int size() { return size; }
    public long discardedFrames() { return discarded; }
    public void offer(short[] input, int count) {
        if (count < 0 || count > input.length) throw new IllegalArgumentException("Invalid capture block");
        int skip = Math.max(0, count - samples.length);
        int drop = Math.max(0, size + count - skip - samples.length);
        if (skip + drop > 0) {
            head = (head + drop) % samples.length; size -= drop;
            discarded += skip + drop; gap = true;
        }
        for (int i = skip; i < count; i++) { samples[(head + size) % samples.length] = input[i]; size++; }
    }
    public int read(short[] output, int maximum) {
        if (maximum < 0 || maximum > output.length) throw new IllegalArgumentException("Invalid output block");
        int count = Math.min(size, maximum);
        if (gap && count > 0) { fadeLeft = fadeFrames; gap = false; }
        for (int i = 0; i < count; i++) {
            short value = samples[head]; head = (head + 1) % samples.length; size--;
            // Ease only a recovery discontinuity; normal capture is bit-exact.
            if (fadeLeft > 0) { value = (short)Math.round(last + (value - last) / (float)fadeLeft); fadeLeft--; }
            output[i] = value; last = value;
        }
        return count;
    }
}
