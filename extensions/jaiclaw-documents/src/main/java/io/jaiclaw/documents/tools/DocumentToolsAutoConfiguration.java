package io.jaiclaw.documents.tools;

import io.jaiclaw.tools.ToolRegistry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureAfter(name = "io.jaiclaw.autoconfigure.JaiClawAgentAutoConfiguration")
@ConditionalOnBean(ToolRegistry.class)
public class DocumentToolsAutoConfiguration {

    @Bean
    public DocumentToolsRegistrar documentToolsRegistrar(
            ToolRegistry toolRegistry,
            @Value("${jaiclaw.tools.documents.workspace-boundary:true}") boolean workspaceBoundary) {
        DocumentTools.registerAll(toolRegistry, workspaceBoundary);
        return new DocumentToolsRegistrar();
    }

    public static class DocumentToolsRegistrar {}
}
