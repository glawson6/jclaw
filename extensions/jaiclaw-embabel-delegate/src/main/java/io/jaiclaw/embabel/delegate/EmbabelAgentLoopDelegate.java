package io.jaiclaw.embabel.delegate;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.ProcessOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jaiclaw.agent.delegate.AgentLoopDelegate;
import io.jaiclaw.agent.delegate.AgentLoopDelegateContext;
import io.jaiclaw.agent.delegate.AgentLoopDelegateResult;
import io.jaiclaw.config.TenantAgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Bridges JaiClaw's message pipeline to Embabel's GOAP agent runtime.
 *
 * <p>When a tenant's agent config specifies {@code loop-delegate.delegate-id: embabel},
 * this delegate takes over from the default LLM+tool loop. The {@code workflow} field
 * maps to an Embabel {@link Agent} by name. User input is bound to the blackboard
 * via {@code IoBinding.DEFAULT_BINDING} ("it"), and the GOAP planner chains actions
 * until the goal type is achieved. The goal object is serialized to JSON and returned
 * as the assistant response.
 */
public class EmbabelAgentLoopDelegate implements AgentLoopDelegate {

    private static final Logger log = LoggerFactory.getLogger(EmbabelAgentLoopDelegate.class);

    public static final String DELEGATE_ID = "embabel";

    private final AgentPlatform agentPlatform;
    private final ObjectMapper objectMapper;

    public EmbabelAgentLoopDelegate(AgentPlatform agentPlatform, ObjectMapper objectMapper) {
        this.agentPlatform = agentPlatform;
        this.objectMapper = objectMapper;
    }

    @Override
    public String delegateId() {
        return DELEGATE_ID;
    }

    @Override
    public boolean canHandle(TenantAgentConfig config) {
        return config.loopDelegate() != null
                && config.loopDelegate().enabled()
                && DELEGATE_ID.equals(config.loopDelegate().delegateId());
    }

    @Override
    public AgentLoopDelegateResult execute(String userInput, AgentLoopDelegateContext context) {
        String workflowName = context.tenantConfig().loopDelegate().workflow();
        log.info("Executing Embabel agent '{}' with input length={}", workflowName, userInput.length());

        Agent agent = findAgent(workflowName);

        try {
            AgentProcess process = agentPlatform.runAgentFrom(
                    agent,
                    ProcessOptions.DEFAULT,
                    Map.of("it", userInput));

            AgentProcessStatusCode status = process.getStatus();
            if (status == AgentProcessStatusCode.COMPLETED) {
                Object result = process.getBlackboard().lastResult();
                if (result == null) {
                    log.warn("Embabel agent '{}' completed but blackboard has no last result", workflowName);
                    return AgentLoopDelegateResult.failure(
                            "Agent completed but produced no result");
                }

                String content = serializeResult(result);
                log.info("Embabel agent '{}' completed successfully, result type={}",
                        workflowName, result.getClass().getSimpleName());
                return AgentLoopDelegateResult.success(content);
            } else {
                String failureInfo = process.getFailureInfo() != null
                        ? process.getFailureInfo().toString()
                        : "status=" + status;
                log.error("Embabel agent '{}' did not complete: {}", workflowName, failureInfo);
                return AgentLoopDelegateResult.failure(
                        "Agent execution failed: " + failureInfo);
            }
        } catch (Exception e) {
            log.error("Error executing Embabel agent '{}'", workflowName, e);
            return AgentLoopDelegateResult.failure(
                    "Agent execution error: " + e.getMessage());
        }
    }

    private Agent findAgent(String workflowName) {
        return agentPlatform.agents().stream()
                .filter(a -> a.getName().equals(workflowName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No Embabel agent named '" + workflowName + "' found. Available: "
                                + agentPlatform.agents().stream().map(Agent::getName).toList()));
    }

    private String serializeResult(Object result) {
        if (result instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize result as JSON, falling back to toString()", e);
            return result.toString();
        }
    }
}
