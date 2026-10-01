package com.jerry.micbridge;

/** Block peak limiter; microphone mono is duplicated into stereo output. */
public final class SignalProcessor {
    private float limiter = 1f;
    public float process(short[] input, int count, short[] output, float gain, boolean muted) {
        if (count < 0 || count > input.length || output.length < count * 2)
            throw new IllegalArgumentException("Invalid audio block");
        float peak = 0f;
        for (int i = 0; i < count; i++) peak = Math.max(peak, Math.abs((int) input[i]) / 32768f);
        float target = peak * gain > 0.9f ? 0.9f / (peak * gain) : 1f;
        // Fast attack, slow release. Hard ceiling also handles rounding.
        limiter = target < limiter ? target : Math.min(target, limiter + 0.01f);
        for (int i = 0; i < count; i++) {
            int value = muted ? 0 : Math.round(input[i] * gain * limiter);
            short limited = (short) Math.max(-29490, Math.min(29490, value));
            output[i * 2] = limited;
            output[i * 2 + 1] = limited;
        }
        return peak;
    }
}
