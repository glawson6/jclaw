package io.jclaw.cli.architect;

/**
 * Determines how the generated CLI project is structured.
 */
public enum ProjectMode {
    /** Sub-module inside the JClaw repository, inherits jclaw-parent. */
    JCLAW_SUBMODULE,
    /** Sub-module inside another Maven project, imports jclaw-bom. */
    EXTERNAL_SUBMODULE,
    /** Standalone Spring Boot project with spring-boot-starter-parent. */
    STANDALONE,
    /** Single-file JBang script with //DEPS directives. */
    JBANG
}
