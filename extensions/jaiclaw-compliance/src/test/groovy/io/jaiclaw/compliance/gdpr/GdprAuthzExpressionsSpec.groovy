package io.jaiclaw.compliance.gdpr

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

class GdprAuthzExpressionsSpec extends Specification {

    // Explicitly-configured role for tests that exercise real gating.
    // (DEFAULT is now blank so backward-compat with api-key deployments
    // is preserved after @EnableMethodSecurity was turned on.)
    GdprAuthzProperties.Roles configuredRoles = new GdprAuthzProperties.Roles("GDPR_OPERATOR")
    GdprAuthzExpressions authz = new GdprAuthzExpressions(configuredRoles)

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "DEFAULT operator role is blank (backward compat with api-key principals)"() {
        expect:
        GdprAuthzProperties.Roles.DEFAULT.operator() == ""
    }

    def "unauthenticated principal is denied on operator() when a role is configured"() {
        expect:
        !authz.operator()
    }

    def "principal with the GDPR_OPERATOR authority passes"() {
        given:
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
                "alice", "n/a",
                [new SimpleGrantedAuthority("GDPR_OPERATOR")])

        expect:
        authz.operator()
    }

    def "principal without the GDPR_OPERATOR authority is denied"() {
        given:
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
                "alice", "n/a",
                [new SimpleGrantedAuthority("ROLE_USER")])

        expect:
        !authz.operator()
    }

    def "blank role config short-circuits to true for authenticated principals"() {
        given:
        GdprAuthzProperties.Roles blank = new GdprAuthzProperties.Roles("")
        GdprAuthzExpressions permissive = new GdprAuthzExpressions(blank)
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
                "alice", "n/a", [new SimpleGrantedAuthority("ROLE_ANYTHING")])

        expect:
        permissive.operator()
    }

    def "blank role short-circuits even for unauthenticated principals (endpoint auth is the chain's job)"() {
        given:
        GdprAuthzProperties.Roles blank = new GdprAuthzProperties.Roles("")
        GdprAuthzExpressions permissive = new GdprAuthzExpressions(blank)
        SecurityContextHolder.clearContext()

        expect:
        permissive.operator()
    }

    def "null roles fall back to blank DEFAULT (any authenticated caller passes)"() {
        given:
        GdprAuthzExpressions withNull = new GdprAuthzExpressions(null)
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
                "alice", "n/a",
                [new SimpleGrantedAuthority("SOMETHING_ELSE")])

        expect:
        withNull.operator()  // blank DEFAULT short-circuits to true
    }
}
