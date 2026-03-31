package io.jaiclaw.voicecall.telephony.twilio;

/**
 * Generates TwiML (Twilio Markup Language) responses for various call scenarios.
 */
public class TwimlGenerator {

    private TwimlGenerator() {}

    /**
     * Generate TwiML for a notify call: speak a message, then hang up.
     */
    public static String notifySay(String message, String voice) {
        String v = voice != null ? voice : "Polly.Amy";
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Say voice="%s">%s</Say>
                    <Hangup/>
                </Response>""".formatted(escapeXml(v), escapeXml(message));
    }

    /**
     * Generate TwiML to connect a media stream for bidirectional audio.
     *
     * @param streamUrl  WebSocket URL for media streaming
     * @param callId     internal call ID passed as a custom parameter
     * @param authToken  per-call auth token for the stream
     */
    public static String connectStream(String streamUrl, String callId, String authToken) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Connect>
                        <Stream url="%s">
                            <Parameter name="callId" value="%s"/>
                            <Parameter name="authToken" value="%s"/>
                        </Stream>
                    </Connect>
                </Response>""".formatted(escapeXml(streamUrl), escapeXml(callId), escapeXml(authToken));
    }

    /**
     * Generate TwiML for speech gathering.
     *
     * @param actionUrl   URL Twilio should POST the results to
     * @param language    speech recognition language
     * @param timeoutSec  silence timeout before finalizing
     * @param turnToken   per-turn nonce for dedup
     */
    public static String gatherSpeech(String actionUrl, String language, int timeoutSec, String turnToken) {
        String lang = language != null ? language : "en-US";
        String urlWithToken = turnToken != null
                ? actionUrl + (actionUrl.contains("?") ? "&" : "?") + "turnToken=" + turnToken
                : actionUrl;
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Gather input="speech" action="%s" language="%s" timeout="%d" speechTimeout="auto">
                        <Pause length="120"/>
                    </Gather>
                </Response>""".formatted(escapeXml(urlWithToken), escapeXml(lang), timeoutSec);
    }

    /**
     * Generate a pause TwiML (keeps the call alive).
     */
    public static String pause(int seconds) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Pause length="%d"/>
                </Response>""".formatted(seconds);
    }

    /**
     * Generate an empty TwiML response (acknowledge webhook, no action).
     */
    public static String empty() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response/>""";
    }

    /**
     * Generate a hangup TwiML response.
     */
    public static String hangup() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Hangup/>
                </Response>""";
    }

    /**
     * Escape XML special characters.
     */
    static String escapeXml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
