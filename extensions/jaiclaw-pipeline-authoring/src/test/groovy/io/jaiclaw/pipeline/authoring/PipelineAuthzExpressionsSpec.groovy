package io.jaiclaw.pipeline.authoring

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

class PipelineAuthzExpressionsSpec extends Specification {

    // Explicit non-blank roles for tests that need real gating behaviour.
    // (The DEFAULT is now blank so backward-compat with api-key deployments
    // is preserved after @EnableMethodSecurity was turned on.)
    PipelineAuthoringProperties.Roles configuredRoles = new PipelineAuthoringProperties.Roles(
            "ROLE_PIPELINE_VIEWER",
            "ROLE_PIPELINE_AUTHOR",
            "ROLE_PIPELINE_DEPLOYER",
            "ROLE_PIPELINE_RUNNER")
    PipelineAuthzExpressions authz = new PipelineAuthzExpressions(configuredRoles)

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "unauthenticated principal denies every role check when roles are configured"() {
        expect:
        !authz.viewer()
        !authz.author()
        !authz.deployer()
        !authz.runner()
    }

    def "principal with matching authority passes"() {
        given:
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
                "alice", "n/a",
                [new SimpleGrantedAuthority("ROLE_PIPELINE_DEPLOYER")])

        expect:
        authz.deployer()
        !authz.author()
    }

    def "DEFAULT is now blank (backward compat with api-key principals with zero authorities)"() {
        expect:
        PipelineAuthoringProperties.Roles.DEFAULT.viewer() == ""
        PipelineAuthoringProperties.Roles.DEFAULT.author() == ""
        PipelineAuthoringProperties.Roles.DEFAULT.deployer() == ""
        PipelineAuthoringProperties.Roles.DEFAULT.runner() == ""
    }

    def "blank role config short-circuits to true for authenticated principals"() {
        given:
        PipelineAuthoringProperties.Roles blank = new PipelineAuthoringProperties.Roles(
                "", "", "", "")
        PipelineAuthzExpressions permissive = new PipelineAuthzExpressions(blank)
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
                "alice", "n/a", [new SimpleGrantedAuthority("ROLE_ANYTHING")])

        expect:
        permissive.viewer()
        permissive.author()
        permissive.deployer()
        permissive.runner()
    }

    def "blank role still denies unauthenticated principals — wait, it short-circuits"() {
        // Design intent: blank role = "no method-level check", which
        // means we don't consult authentication at all. Endpoint
        // authentication is the app's Spring Security chain's job.
        given:
        PipelineAuthoringProperties.Roles blank = new PipelineAuthoringProperties.Roles(
                "", "", "", "")
        PipelineAuthzExpressions permissive = new PipelineAuthzExpressions(blank)
        SecurityContextHolder.clearContext()

        expect:
        permissive.viewer()
    }

    def "null roles fall back to blank DEFAULT (any authenticated caller passes)"() {
        given:
        PipelineAuthzExpressions withNull = new PipelineAuthzExpressions(null)
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
                "alice", "n/a",
                [new SimpleGrantedAuthority("ROLE_PIPELINE_AUTHOR")])

        expect:
        withNull.author()
        withNull.deployer()  // Blank DEFAULT.deployer short-circuits to true
    }
}
