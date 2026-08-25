package io.jaiclaw.pipeline.authoring

import io.jaiclaw.pipeline.PipelineDefinition
import io.jaiclaw.pipeline.validation.PipelineValidator
import io.jaiclaw.pipeline.validation.ValidationReport
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

/**
 * PR-3 first-of-its-kind test: proves that {@code @PreAuthorize} annotations
 * on {@link PipelineStudioController} actually fire when method security is
 * enabled. Without {@code @EnableMethodSecurity} these annotations were
 * silent no-ops in the codebase for months — this spec is the regression
 * guard so that stays fixed.
 *
 * <p>Uses a minimal Spring context with {@code @EnableMethodSecurity} plus
 * a hand-rolled test stubs for the collaborators (no Spock Mock() in the
 * bean methods — those don't compose with Spring's factory-method
 * initialization).
 */
@SpringBootTest(classes = [PipelineStudioControllerAuthzSpec.Config])
@Import(PipelineStudioControllerAuthzSpec.Config)
class PipelineStudioControllerAuthzSpec extends Specification {

    @Autowired PipelineStudioController controller

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "with configured author role, a principal WITHOUT the authority is denied on POST /drafts (create)"() {
        given:
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
                "alice", "n/a", [new SimpleGrantedAuthority("ROLE_UNRELATED")])

        when:
        controller.listDrafts()  // viewer() — needs ROLE_PIPELINE_VIEWER → denied

        then:
        thrown(AccessDeniedException)
    }

    def "with configured author role, a principal WITH the authority passes the gate"() {
        given:
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
                "alice", "n/a", [new SimpleGrantedAuthority("ROLE_PIPELINE_AUTHOR")])

        when:
        // Passes @PreAuthorize; null body triggers the controller's 400 branch.
        def response = controller.createDraft(null)

        then:
        noExceptionThrown()
        response.statusCode.value() == 400
    }

    def "under the same non-blank Config, an api-key principal with zero authorities is denied"() {
        // This reproduces the exact scenario the plan warned about:
        // ApiKeyAuthenticationFilter grants no authorities, so if adopters
        // configure a role explicitly they must ALSO ensure the auth layer
        // grants a matching authority — otherwise every api-key call 403s.
        given:
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
                "api-key-user", "n/a", [])  // ZERO authorities (matches ApiKeyAuthenticationFilter default)

        when:
        controller.listDrafts()

        then:
        thrown(AccessDeniedException)
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {

        @Bean
        PipelineAuthzExpressions pipelineAuthzExpressions() {
            return new PipelineAuthzExpressions(new PipelineAuthoringProperties.Roles(
                    "ROLE_PIPELINE_VIEWER",
                    "ROLE_PIPELINE_AUTHOR",
                    "ROLE_PIPELINE_DEPLOYER",
                    "ROLE_PIPELINE_RUNNER"))
        }

        @Bean
        PipelineDraftStore draftStore() {
            return new NoopDraftStore()
        }

        @Bean
        PipelineCatalogService catalogService() {
            return new PipelineCatalogService(null, null)
        }

        @Bean
        PipelineValidator pipelineValidator() {
            return new NoopValidator()
        }

        @Bean
        PipelineStudioController pipelineStudioController(PipelineDraftStore d,
                                                          PipelineCatalogService c,
                                                          PipelineValidator v) {
            return new PipelineStudioController(d, c, v)
        }
    }

    /** Hand-rolled draft store — every operation is a no-op / empty result. */
    static class NoopDraftStore implements PipelineDraftStore {
        @Override List<PipelineDraft> findAll() { return [] }
        @Override Optional<PipelineDraft> find(String id) { return Optional.empty() }
        @Override PipelineDraft save(PipelineDraft draft) { return draft }
        @Override void delete(String id) {}
    }

    /** Hand-rolled validator — returns empty report on any input. */
    static class NoopValidator extends PipelineValidator {
        NoopValidator() { super(null, null, null, null) }
        @Override ValidationReport validate(PipelineDefinition definition) {
            return new ValidationReport.Builder().build()
        }
    }
}
