package io.jclaw.autoconfigure;

import io.jclaw.agent.AgentRuntime;
import io.jclaw.agent.session.SessionManager;
import io.jclaw.channel.ChannelAdapter;
import io.jclaw.channel.ChannelRegistry;
import io.jclaw.config.JClawProperties;
import io.jclaw.core.agent.*;
import io.jclaw.core.skill.SkillDefinition;
import io.jclaw.memory.InMemorySearchManager;
import io.jclaw.memory.MemorySearchManager;
import io.jclaw.memory.VectorStoreSearchManager;
import io.jclaw.plugin.PluginRegistry;
import io.jclaw.skills.SkillLoader;
import io.jclaw.tools.ToolRegistry;
import io.jclaw.tools.bridge.embabel.AgentOrchestrationPort;
import io.jclaw.tools.builtin.BuiltinTools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Core Spring Boot auto-configuration that wires all JClaw modules together.
 * Add {@code jclaw-spring-boot-starter} as a dependency to activate.
 *
 * <p>Gateway beans are defined in {@link JClawGatewayAutoConfiguration} and
 * channel adapters in {@link JClawChannelAutoConfiguration}, each with proper
 * {@code @AutoConfigureAfter} ordering so that {@code @ConditionalOnBean}
 * checks resolve reliably.
 */
@AutoConfiguration
@AutoConfigureAfter(name = "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration")
@EnableConfigurationProperties(JClawProperties.class)
public class JClawAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ToolRegistry toolRegistry() {
        var registry = new ToolRegistry();
        BuiltinTools.registerAll(registry);
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionManager sessionManager() {
        return new SessionManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public SkillLoader skillLoader() {
        return new SkillLoader();
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginRegistry pluginRegistry() {
        return new PluginRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(MemorySearchManager.class)
    @ConditionalOnClass(name = "org.springframework.ai.vectorstore.VectorStore")
    @ConditionalOnBean(type = "org.springframework.ai.vectorstore.VectorStore")
    public VectorStoreSearchManager vectorStoreSearchManager(
            org.springframework.ai.vectorstore.VectorStore vectorStore) {
        return new VectorStoreSearchManager(vectorStore);
    }

    @Bean
    @ConditionalOnMissingBean(MemorySearchManager.class)
    public InMemorySearchManager inMemorySearchManager() {
        return new InMemorySearchManager();
    }

    // --- SPI adapter beans ---

    @Bean
    @ConditionalOnMissingBean(AgentHookDispatcher.class)
    @ConditionalOnClass(name = "io.jclaw.plugin.HookRunnerAdapter")
    public AgentHookDispatcher agentHookDispatcher(PluginRegistry pluginRegistry) {
        var hookRunner = new io.jclaw.plugin.HookRunner(pluginRegistry);
        return new io.jclaw.plugin.HookRunnerAdapter(hookRunner);
    }

    @Bean
    @ConditionalOnMissingBean(ContextCompactor.class)
    @ConditionalOnClass(name = "io.jclaw.compaction.CompactionServiceAdapter")
    public ContextCompactor contextCompactor() {
        var config = io.jclaw.core.model.CompactionConfig.DEFAULT;
        var service = new io.jclaw.compaction.CompactionService(config);
        return new io.jclaw.compaction.CompactionServiceAdapter(service);
    }

    @Bean
    @ConditionalOnMissingBean(MemoryProvider.class)
    @ConditionalOnClass(name = "io.jclaw.memory.WorkspaceMemoryProvider")
    public MemoryProvider memoryProvider() {
        return new io.jclaw.memory.WorkspaceMemoryProvider();
    }

    // --- AgentRuntime with full SPI wiring ---

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ChatClient.Builder.class)
    public AgentRuntime agentRuntime(
            SessionManager sessionManager,
            ChatClient.Builder chatClientBuilder,
            ToolRegistry toolRegistry,
            SkillLoader skillLoader,
            JClawProperties properties,
            ObjectProvider<ChatModel> chatModelProvider,
            ObjectProvider<ContextCompactor> compactorProvider,
            ObjectProvider<AgentHookDispatcher> hooksProvider,
            ObjectProvider<MemoryProvider> memoryProviderProvider,
            ObjectProvider<ToolApprovalHandler> approvalHandlerProvider,
            ObjectProvider<AgentOrchestrationPort> orchestrationPortProvider) {

        List<SkillDefinition> skills = skillLoader.loadBundled();

        // Resolve tool loop config from properties
        var agentConfig = properties.agent().agents().getOrDefault(
                properties.agent().defaultAgent(),
                io.jclaw.config.AgentProperties.AgentConfig.DEFAULT);
        ToolLoopConfig toolLoopConfig = agentConfig.toolLoop().toConfig();

        return new AgentRuntime(
                sessionManager,
                chatClientBuilder,
                toolRegistry,
                skills,
                chatModelProvider.getIfAvailable(),
                toolLoopConfig,
                compactorProvider.getIfAvailable(),
                hooksProvider.getIfAvailable(),
                memoryProviderProvider.getIfAvailable(),
                approvalHandlerProvider.getIfAvailable(),
                orchestrationPortProvider.getIfAvailable()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ChannelRegistry channelRegistry(List<ChannelAdapter> adapters) {
        var registry = new ChannelRegistry();
        adapters.forEach(registry::register);
        return registry;
    }

    /**
     * Orchestration port auto-configuration.
     */
    @Bean
    @ConditionalOnMissingBean(type = "io.jclaw.tools.bridge.embabel.AgentOrchestrationPort")
    public io.jclaw.tools.bridge.embabel.NoOpOrchestrationPort noOpOrchestrationPort() {
        return new io.jclaw.tools.bridge.embabel.NoOpOrchestrationPort();
    }
}
