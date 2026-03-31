package io.jaiclaw.voicecall.media

import spock.lang.Specification

class TelephonyAudioCodecSpec extends Specification {

    def "PCM to mulaw round-trip preserves signal shape"() {
        given: "a simple sine wave in PCM16"
        def samples = 320  // 40ms at 8kHz
        short[] pcm = new short[samples]
        for (int i = 0; i < samples; i++) {
            pcm[i] = (short) (Math.sin(2 * Math.PI * 440 * i / 8000) * 16000)
        }

        when:
        byte[] mulaw = TelephonyAudioCodec.pcmToMulaw(pcm)
        short[] recovered = TelephonyAudioCodec.mulawToPcm(mulaw)

        then:
        mulaw.length == samples
        recovered.length == samples
        // Mu-law is lossy, but should preserve general shape
        // Check that the sign matches for most samples
        def signMatches = (0..<samples).count { i ->
            (pcm[i] >= 0) == (recovered[i] >= 0) || Math.abs(pcm[i]) < 100
        }
        signMatches > samples * 0.9
    }

    def "silence encodes to consistent mulaw value"() {
        given:
        short[] silence = new short[160]  // 20ms of silence

        when:
        byte[] mulaw = TelephonyAudioCodec.pcmToMulaw(silence)

        then:
        mulaw.length == 160
        // All silence samples should encode to the same mulaw value
        mulaw.every { it == mulaw[0] }
    }

    def "chunkToFrames creates correct frame sizes"() {
        given:
        byte[] audio = new byte[480]  // 3 frames of 160 bytes

        when:
        byte[][] frames = TelephonyAudioCodec.chunkToFrames(audio)

        then:
        frames.length == 3
        frames.every { it.length == 160 }
    }

    def "chunkToFrames handles non-frame-aligned audio"() {
        given:
        byte[] audio = new byte[200]  // 1.25 frames

        when:
        byte[][] frames = TelephonyAudioCodec.chunkToFrames(audio)

        then:
        frames.length == 2
        frames[0].length == 160
        frames[1].length == 40  // remainder
    }

    def "resampleTo8kHz returns identity for 8kHz input"() {
        given:
        short[] samples = [100, 200, 300, 400, 500] as short[]

        when:
        short[] result = TelephonyAudioCodec.resampleTo8kHz(samples, 8000)

        then:
        result == samples
    }

    def "resampleTo8kHz downsamples 16kHz to 8kHz"() {
        given:
        short[] samples = new short[320]  // 20ms at 16kHz
        for (int i = 0; i < 320; i++) {
            samples[i] = (short) (i * 100)
        }

        when:
        short[] result = TelephonyAudioCodec.resampleTo8kHz(samples, 16000)

        then:
        result.length == 160  // 20ms at 8kHz
    }

    def "bytesToPcm16 converts little-endian bytes"() {
        given:
        // 0x0100 = 256, 0xFF7F = 32767 (little-endian)
        byte[] bytes = [0x00, 0x01, (byte)0xFF, 0x7F] as byte[]

        when:
        short[] pcm = TelephonyAudioCodec.bytesToPcm16(bytes)

        then:
        pcm.length == 2
        pcm[0] == 256
        pcm[1] == 32767
    }

    def "samplesPerFrame returns 160"() {
        expect:
        TelephonyAudioCodec.samplesPerFrame() == 160
    }

    def "frameDurationMs returns 20"() {
        expect:
        TelephonyAudioCodec.frameDurationMs() == 20
    }

    def "linearToMulaw handles max positive value"() {
        when:
        byte result = TelephonyAudioCodec.linearToMulaw((short) 32767)

        then:
        result != 0  // Should produce a valid mulaw byte
    }

    def "linearToMulaw handles max negative value"() {
        when:
        byte result = TelephonyAudioCodec.linearToMulaw((short) -32768)

        then:
        result != 0
    }
}
