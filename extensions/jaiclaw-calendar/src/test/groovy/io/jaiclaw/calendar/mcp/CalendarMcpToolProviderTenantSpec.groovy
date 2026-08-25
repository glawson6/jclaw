package io.jaiclaw.calendar.mcp

import io.jaiclaw.calendar.config.CalendarProperties
import io.jaiclaw.calendar.service.CalendarService
import io.jaiclaw.calendar.util.CalendarEventValidator
import io.jaiclaw.core.tenant.DefaultTenantContext
import tools.jackson.databind.ObjectMapper
import spock.lang.Specification

/**
 * SEV-002 regression guard — asserts that {@code CalendarMcpToolProvider}
 * rejects any caller-supplied {@code tenantId} that does not match the
 * caller's {@link io.jaiclaw.core.tenant.TenantContext}.
 */
class CalendarMcpToolProviderTenantSpec extends Specification {

    def calendarService = Mock(CalendarService)
    def validator = Mock(CalendarEventValidator)
    def properties = CalendarProperties.defaults()
    def provider = new CalendarMcpToolProvider(calendarService, properties, validator, new ObjectMapper())

    def "list_calendars with mismatched tenantId is rejected"() {
        given:
        def callerTenant = new DefaultTenantContext("tenant-A", "A")

        when:
        def result = provider.execute("list_calendars", [tenantId: "tenant-B"], callerTenant)

        then:
        result.isError()
        result.content().contains("cross-tenant access denied")
        0 * calendarService._
    }

    def "delete_event with mismatched tenantId is rejected before service call"() {
        given:
        def callerTenant = new DefaultTenantContext("tenant-A", "A")

        when:
        def result = provider.execute("delete_event",
                [eventId: "e1", tenantId: "tenant-B"], callerTenant)

        then:
        result.isError()
        result.content().contains("cross-tenant access denied")
        0 * calendarService.deleteEvent(_, _, _)
    }

    def "update_event with mismatched tenantId is rejected"() {
        given:
        def callerTenant = new DefaultTenantContext("tenant-A", "A")

        when:
        def result = provider.execute("update_event",
                [eventId: "e1", tenantId: "tenant-B", title: "new"], callerTenant)

        then:
        result.isError()
        result.content().contains("cross-tenant access denied")
        0 * calendarService.updateEvent(_, _, _, _)
    }

    def "create_calendar with mismatched tenantId is rejected"() {
        given:
        def callerTenant = new DefaultTenantContext("tenant-A", "A")

        when:
        def result = provider.execute("create_calendar",
                [calendarId: "c1", name: "My Cal", tenantId: "tenant-B"], callerTenant)

        then:
        result.isError()
        result.content().contains("cross-tenant access denied")
        0 * calendarService.createCalendar(_)
    }

    def "matching tenantId is allowed"() {
        given:
        def callerTenant = new DefaultTenantContext("tenant-A", "A")
        calendarService.listCalendars("tenant-A") >> reactor.core.publisher.Flux.empty()

        when:
        def result = provider.execute("list_calendars", [tenantId: "tenant-A"], callerTenant)

        then:
        !result.isError()
        result.content().contains("\"success\"")
    }

    def "omitted tenantId defaults to caller tenant"() {
        given:
        def callerTenant = new DefaultTenantContext("tenant-A", "A")
        calendarService.listCalendars("tenant-A") >> reactor.core.publisher.Flux.empty()

        when:
        def result = provider.execute("list_calendars", [:], callerTenant)

        then:
        !result.isError()
        1 * calendarService.listCalendars("tenant-A") >> reactor.core.publisher.Flux.empty()
    }
}
