package io.jclaw.agent

import io.jclaw.agent.session.SessionManager
import io.jclaw.core.agent.*
import io.jclaw.core.hook.HookName
import io.jclaw.core.model.AgentIdentity
import io.jclaw.core.model.Session
import io.jclaw.core.model.UserMessage
import io.jclaw.core.skill.SkillDefinition
import io.jclaw.core.tool.ToolProfile
import io.jclaw.tools.ToolRegistry
import io.jclaw.tools.bridge.embabel.AgentOrchestrationPort
import org.springframework.ai.chat.client.ChatClient
import spock.lang.Specification

class AgentRuntimeSpec extends Specification {

    SessionManager sessionManager = new SessionManager()
    ChatClient.Builder chatClientBuilder = Mock()
    ToolRegistry toolRegistry = new ToolRegistry()
    List<SkillDefinition> skills = []

    def setup() {
        sessionManager.getOrCreate("test-session", "default")
    }

    def "backward-compatible 4-arg constructor works"() {
        when:
        def runtime = new AgentRuntime(sessionManager, chatClientBuilder, toolRegistry, skills)

        then:
        runtime != null
    }

    def "full constructor accepts all SPI collaborators"() {
        given:
        def hooks = Mock(AgentHookDispatcher)
        def compactor = Mock(ContextCompactor)
        def memory = Mock(MemoryProvider)
        def approval = Mock(ToolApprovalHandler)
        def orchestration = Mock(AgentOrchestrationPort)

        when:
        def runtime = new AgentRuntime(
                sessionManager, chatClientBuilder, toolRegistry, skills,
                null, ToolLoopConfig.DEFAULT, compactor, hooks, memory, approval, orchestration
        )

        then:
        runtime != null
    }

    def "full constructor accepts all null SPIs"() {
        when:
        def runtime = new AgentRuntime(
                sessionManager, chatClientBuilder, toolRegistry, skills,
                null, null, null, null, null, null, null
        )

        then:
        runtime != null
    }

    def "cancel removes from active tasks"() {
        given:
        def runtime = new AgentRuntime(sessionManager, chatClientBuilder, toolRegistry, skills)

        when:
        runtime.cancel("test-session")

        then:
        !runtime.isRunning("test-session")
    }

    def "isRunning returns false for unknown session"() {
        given:
        def runtime = new AgentRuntime(sessionManager, chatClientBuilder, toolRegistry, skills)

        expect:
        !runtime.isRunning("unknown-session")
    }
}
