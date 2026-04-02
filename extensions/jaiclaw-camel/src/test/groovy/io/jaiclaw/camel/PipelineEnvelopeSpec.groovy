package io.jaiclaw.camel

import spock.lang.Specification

class PipelineEnvelopeSpec extends Specification {

    def "isLastStage returns true for final stage"() {
        given:
        def envelope = new PipelineEnvelope("p1", "c1", 2, 3, null, null, [])

        expect:
        envelope.isLastStage()
    }

    def "isLastStage returns false for non-final stage"() {
        given:
        def envelope = new PipelineEnvelope("p1", "c1", 0, 3, null, null, [])

        expect:
        !envelope.isLastStage()
    }

    def "nextStage increments index and appends output"() {
        given:
        def envelope = new PipelineEnvelope("p1", "c1", 0, 3, "reply-ch", "peer1", [])

        when:
        def next = envelope.nextStage("stage-0-output")

        then:
        next.stageIndex() == 1
        next.stageOutputs() == ["stage-0-output"]
        next.pipelineId() == "p1"
        next.correlationId() == "c1"
        next.replyChannelId() == "reply-ch"
        next.replyPeerId() == "peer1"
        next.totalStages() == 3
    }

    def "original envelope is unchanged after nextStage"() {
        given:
        def original = new PipelineEnvelope("p1", "c1", 0, 3, null, null, [])

        when:
        original.nextStage("output")

        then:
        original.stageIndex() == 0
        original.stageOutputs().isEmpty()
    }

    def "multi-stage accumulation through full pipeline"() {
        given:
        def stage0 = new PipelineEnvelope("p1", "c1", 0, 3, null, null, [])

        when:
        def stage1 = stage0.nextStage("result-0")
        def stage2 = stage1.nextStage("result-1")
        def stage3 = stage2.nextStage("result-2")

        then:
        stage3.stageIndex() == 3
        stage3.stageOutputs() == ["result-0", "result-1", "result-2"]
        stage2.isLastStage()
        !stage1.isLastStage()
    }

    def "null stageOutputs normalizes to empty list"() {
        when:
        def envelope = new PipelineEnvelope("p1", "c1", 0, 2, null, null, null)

        then:
        envelope.stageOutputs() == []
    }

    def "stageOutputs are immutable copies"() {
        given:
        def mutableList = new ArrayList(["a", "b"])
        def envelope = new PipelineEnvelope("p1", "c1", 0, 2, null, null, mutableList)

        when:
        mutableList.add("c")

        then:
        envelope.stageOutputs() == ["a", "b"]
    }
}
