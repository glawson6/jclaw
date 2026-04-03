package io.jaiclaw.embabel.delegate;

import com.embabel.agent.core.AgentPlatform;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that registers {@link EmbabelAgentLoopDelegate} when
 * Embabel's {@link AgentPlatform} is available on the classpath and as a bean.
 *
 * <p>Must run after Embabel's {@code AgentPlatformAutoConfiguration} (which creates
 * the {@link AgentPlatform} bean) and before JaiClaw's auto-config collects delegates
 * into the {@code AgentLoopDelegateRegistry}.
 *
 * <p>The delegate bean is automatically discovered by
 * {@code AgentLoopDelegateRegistry} via {@code ObjectProvider<List<AgentLoopDelegate>>}.
 */
@AutoConfiguration
@AutoConfigureAfter(name = "com.embabel.agent.autoconfigure.platform.AgentPlatformAutoConfiguration")
@ConditionalOnClass(name = "com.embabel.agent.core.AgentPlatform")
public class EmbabelDelegateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AgentPlatform.class)
    public EmbabelAgentLoopDelegate embabelAgentLoopDelegate(
            AgentPlatform agentPlatform, ObjectMapper objectMapper) {
        return new EmbabelAgentLoopDelegate(agentPlatform, objectMapper);
    }
}
