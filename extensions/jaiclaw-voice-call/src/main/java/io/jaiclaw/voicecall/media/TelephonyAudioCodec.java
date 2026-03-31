package io.jaiclaw.voicecall.media;

/**
 * Audio codec utilities for telephony media streams.
 * Handles PCM-to-mulaw (G.711) conversion and frame chunking for 8kHz telephony audio.
 */
public class TelephonyAudioCodec {

    private static final int MULAW_BIAS = 0x84;
    private static final int MULAW_MAX = 0x7FFF;
    private static final int SAMPLE_RATE = 8000;
    private static final int FRAME_DURATION_MS = 20;
    private static final int SAMPLES_PER_FRAME = SAMPLE_RATE * FRAME_DURATION_MS / 1000; // 160

    private TelephonyAudioCodec() {}

    /**
     * Convert 16-bit PCM samples to mu-law encoded bytes.
     *
     * @param pcm16 array of 16-bit signed PCM samples
     * @return mu-law encoded byte array
     */
    public static byte[] pcmToMulaw(short[] pcm16) {
        byte[] mulaw = new byte[pcm16.length];
        for (int i = 0; i < pcm16.length; i++) {
            mulaw[i] = linearToMulaw(pcm16[i]);
        }
        return mulaw;
    }

    /**
     * Convert mu-law encoded bytes to 16-bit PCM samples.
     *
     * @param mulaw mu-law encoded byte array
     * @return array of 16-bit signed PCM samples
     */
    public static short[] mulawToPcm(byte[] mulaw) {
        short[] pcm16 = new short[mulaw.length];
        for (int i = 0; i < mulaw.length; i++) {
            pcm16[i] = mulawToLinear(mulaw[i]);
        }
        return pcm16;
    }

    /**
     * Convert a byte array of raw PCM16 (little-endian) to 16-bit samples.
     */
    public static short[] bytesToPcm16(byte[] bytes) {
        short[] samples = new short[bytes.length / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (short) ((bytes[i * 2] & 0xFF) | (bytes[i * 2 + 1] << 8));
        }
        return samples;
    }

    /**
     * Resample audio from one sample rate to 8kHz.
     * Uses simple linear interpolation.
     *
     * @param samples     input PCM samples
     * @param fromRate    source sample rate
     * @return resampled PCM samples at 8kHz
     */
    public static short[] resampleTo8kHz(short[] samples, int fromRate) {
        if (fromRate == SAMPLE_RATE) return samples;

        double ratio = (double) SAMPLE_RATE / fromRate;
        int outputLength = (int) (samples.length * ratio);
        short[] output = new short[outputLength];

        for (int i = 0; i < outputLength; i++) {
            double srcIndex = i / ratio;
            int srcIdx = (int) srcIndex;
            double fraction = srcIndex - srcIdx;

            if (srcIdx + 1 < samples.length) {
                output[i] = (short) (samples[srcIdx] * (1 - fraction) +
                        samples[srcIdx + 1] * fraction);
            } else if (srcIdx < samples.length) {
                output[i] = samples[srcIdx];
            }
        }
        return output;
    }

    /**
     * Split mu-law audio into 20ms frames (160 samples each at 8kHz).
     *
     * @param mulaw complete mu-law encoded audio
     * @return list of 160-byte frames
     */
    public static byte[][] chunkToFrames(byte[] mulaw) {
        int numFrames = (mulaw.length + SAMPLES_PER_FRAME - 1) / SAMPLES_PER_FRAME;
        byte[][] frames = new byte[numFrames][];

        for (int i = 0; i < numFrames; i++) {
            int offset = i * SAMPLES_PER_FRAME;
            int length = Math.min(SAMPLES_PER_FRAME, mulaw.length - offset);
            frames[i] = new byte[length];
            System.arraycopy(mulaw, offset, frames[i], 0, length);
        }
        return frames;
    }

    /**
     * Get the number of samples per frame (160 at 8kHz, 20ms).
     */
    public static int samplesPerFrame() {
        return SAMPLES_PER_FRAME;
    }

    /**
     * Get the frame duration in milliseconds.
     */
    public static int frameDurationMs() {
        return FRAME_DURATION_MS;
    }

    // --- Internal mu-law conversion ---

    /**
     * Convert a 16-bit linear PCM sample to mu-law.
     */
    static byte linearToMulaw(short sample) {
        int sign = (sample >> 8) & 0x80;
        if (sign != 0) {
            sample = (short) -sample;
        }
        if (sample > MULAW_MAX) {
            sample = MULAW_MAX;
        }

        sample = (short) (sample + MULAW_BIAS);
        int exponent = 7;

        for (int expMask = 0x4000; (sample & expMask) == 0 && exponent > 0; exponent--, expMask >>= 1) {
            // Find the segment
        }

        int mantissa = (sample >> (exponent + 3)) & 0x0F;
        byte mulawByte = (byte) (sign | (exponent << 4) | mantissa);
        return (byte) ~mulawByte;
    }

    /**
     * Convert a mu-law byte to a 16-bit linear PCM sample.
     */
    static short mulawToLinear(byte mulawByte) {
        int mulaw = ~(mulawByte & 0xFF);
        int sign = mulaw & 0x80;
        int exponent = (mulaw >> 4) & 0x07;
        int mantissa = mulaw & 0x0F;

        int sample = ((mantissa << 3) + MULAW_BIAS) << exponent;
        sample -= MULAW_BIAS;

        return (short) (sign != 0 ? -sample : sample);
    }
}
