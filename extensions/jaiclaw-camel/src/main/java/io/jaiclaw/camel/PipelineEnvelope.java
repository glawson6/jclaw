package io.jaiclaw.camel;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable envelope for tracking multi-stage pipeline execution through Camel routes.
 *
 * @param pipelineId     unique pipeline identifier
 * @param correlationId  correlation ID for tracing
 * @param stageIndex     current stage index (0-based)
 * @param totalStages    total number of stages
 * @param replyChannelId channel to send the final result to (nullable)
 * @param replyPeerId    peer to reply to (nullable)
 * @param stageOutputs   accumulated outputs from completed stages
 */
public record PipelineEnvelope(
        String pipelineId,
        String correlationId,
        int stageIndex,
        int totalStages,
        String replyChannelId,
        String replyPeerId,
        List<String> stageOutputs
) {
    public PipelineEnvelope {
        if (stageOutputs == null) {
            stageOutputs = List.of();
        } else {
            stageOutputs = List.copyOf(stageOutputs);
        }
    }

    /**
     * Advance to the next stage, appending the current stage's output.
     *
     * @param currentOutput output from the current stage
     * @return a new envelope with incremented stageIndex and appended output
     */
    public PipelineEnvelope nextStage(String currentOutput) {
        List<String> newOutputs = new ArrayList<>(stageOutputs);
        newOutputs.add(currentOutput);
        return new PipelineEnvelope(
                pipelineId, correlationId,
                stageIndex + 1, totalStages,
                replyChannelId, replyPeerId,
                newOutputs
        );
    }

    /**
     * Whether this is the last stage in the pipeline.
     */
    public boolean isLastStage() {
        return stageIndex >= totalStages - 1;
    }
}
