package io.jaiclaw.gateway.admin

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

class AdminAuthzExpressionsSpec extends Specification {

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "DEFAULT admin role is blank (backward compat with api-key principals)"() {
        expect:
        AdminAuthzProperties.Roles.DEFAULT.admin() == ""
    }

    def "blank config short-circuits to true regardless of authorities"() {
        given:
        def authz = new AdminAuthzExpressions(AdminAuthzProperties.Roles.DEFAULT)
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
                "api-key-user", "n/a", [])

        expect:
        authz.admin()
    }

    def "configured role passes when principal has matching authority"() {
        given:
        def authz = new AdminAuthzExpressions(new AdminAuthzProperties.Roles("JAICLAW_ADMIN"))
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
                "alice", "n/a", [new SimpleGrantedAuthority("JAICLAW_ADMIN")])

        expect:
        authz.admin()
    }

    def "configured role denies principals without the authority"() {
        given:
        def authz = new AdminAuthzExpressions(new AdminAuthzProperties.Roles("JAICLAW_ADMIN"))
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
                "alice", "n/a", [new SimpleGrantedAuthority("ROLE_USER")])

        expect:
        !authz.admin()
    }

    def "configured role denies unauthenticated callers"() {
        given:
        def authz = new AdminAuthzExpressions(new AdminAuthzProperties.Roles("JAICLAW_ADMIN"))
        SecurityContextHolder.clearContext()

        expect:
        !authz.admin()
    }

    def "null roles fall back to blank DEFAULT"() {
        given:
        def authz = new AdminAuthzExpressions(null)
        SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(
                "api-key-user", "n/a", [])

        expect:
        authz.admin()
    }
}
