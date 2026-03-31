package io.jaiclaw.voicecall.store

import io.jaiclaw.voicecall.model.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class JsonlCallStoreSpec extends Specification {

    @TempDir
    Path tempDir

    def "persist and reload from disk"() {
        given:
        def storePath = tempDir.resolve("calls.jsonl")
        def store = new JsonlCallStore(storePath)

        def call = new CallRecord("c1", "twilio", CallDirection.OUTBOUND,
                "+1555", "+1666", CallMode.CONVERSATION)
        call.providerCallId = "CA123"
        call.addTranscriptEntry(TranscriptEntry.Speaker.BOT, "Hello")

        when:
        store.persist(call)
        store.shutdown()
        // Allow async write to complete
        Thread.sleep(200)

        // Reload from disk
        def store2 = new JsonlCallStore(storePath)
        def history = store2.getHistory(10)

        then:
        history.size() == 1
        history[0].callId == "c1"
        history[0].providerCallId == "CA123"
        history[0].transcript.size() == 1

        cleanup:
        store2.shutdown()
    }

    def "loadActiveCalls excludes terminal"() {
        given:
        def storePath = tempDir.resolve("calls.jsonl")
        def store = new JsonlCallStore(storePath)

        def active = new CallRecord("c1", "twilio", CallDirection.OUTBOUND,
                "+1555", "+1666", CallMode.CONVERSATION)
        active.state = CallState.ACTIVE

        def done = new CallRecord("c2", "twilio", CallDirection.OUTBOUND,
                "+1777", "+1888", CallMode.NOTIFY)
        done.state = CallState.COMPLETED

        when:
        store.persist(active)
        store.persist(done)
        def activeCalls = store.loadActiveCalls()

        then:
        activeCalls.size() == 1
        activeCalls.containsKey("c1")

        cleanup:
        store.shutdown()
    }

    def "handles empty or missing file gracefully"() {
        given:
        def storePath = tempDir.resolve("nonexistent.jsonl")

        when:
        def store = new JsonlCallStore(storePath)

        then:
        store.getHistory(10).isEmpty()
        store.loadActiveCalls().isEmpty()

        cleanup:
        store.shutdown()
    }
}
