package com.jerry.micbridge;

/** Bounded, once-per-second tuning. Grow on glitches, probe down after ten stable seconds. */
public final class OutputBufferTuner {
    private int floor;
    private final int step, capacity;
    private long stableSince;
    private boolean probing;
    public OutputBufferTuner(int minimum, int step, int capacity, long now) {
        this.capacity = Math.max(1, capacity); this.step = Math.max(1, step);
        floor = Math.max(1, Math.min(minimum, this.capacity)); stableSince = now;
    }
    public int update(int current, boolean underrun, long now) {
        current = Math.max(1, Math.min(current, capacity));
        if (underrun) {
            int next = (int)Math.min(capacity, (long)current + step);
            if (probing) floor = Math.max(floor, next);
            probing = false; stableSince = now; return next;
        }
        if (now - stableSince >= 10000 && current > floor) {
            stableSince = now; probing = true; return Math.max(floor, current - step);
        }
        return current;
    }
}
