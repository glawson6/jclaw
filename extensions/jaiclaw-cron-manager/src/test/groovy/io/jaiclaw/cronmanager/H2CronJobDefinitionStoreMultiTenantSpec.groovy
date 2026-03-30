package io.jaiclaw.cronmanager

import io.jaiclaw.core.model.CronJob
import io.jaiclaw.core.tenant.DefaultTenantContext
import io.jaiclaw.core.tenant.TenantContextHolder
import io.jaiclaw.core.tenant.TenantGuard
import io.jaiclaw.core.tenant.TenantMode
import io.jaiclaw.core.tenant.TenantProperties
import io.jaiclaw.core.tool.ToolProfile
import io.jaiclaw.cronmanager.model.CronJobDefinition
import io.jaiclaw.cronmanager.persistence.h2.H2CronJobDefinitionStore
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import spock.lang.Shared
import spock.lang.Specification

class H2CronJobDefinitionStoreMultiTenantSpec extends Specification {

    def multiGuard = new TenantGuard(new TenantProperties(TenantMode.MULTI, "default"))
    @Shared def db = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("schema.sql")
            .build()
    def jdbc = new JdbcTemplate(db)
    def store = new H2CronJobDefinitionStore(jdbc, multiGuard)

    def setup() {
        jdbc.execute("DELETE FROM cron_job_definitions")
    }

    def setTenant(String id) {
        TenantContextHolder.set(new DefaultTenantContext(id, id))
    }

    def cleanup() {
        TenantContextHolder.clear()
    }

    def cleanupSpec() {
        db?.shutdown()
    }

    private CronJobDefinition makeDef(String id, String tenantId) {
        def cronJob = CronJob.builder()
                .id(id)
                .name("job-$id")
                .agentId("agent-1")
                .schedule("0 * * * *")
                .timezone("UTC")
                .prompt("test prompt")
                .enabled(true)
                .tenantId(tenantId)
                .build()
        return new CronJobDefinition(cronJob, null, null, null, ToolProfile.MINIMAL, [])
    }

    def "save as tenant A, findById as tenant B returns empty"() {
        given:
        setTenant("tenant-a")
        store.save(makeDef("job-1", "tenant-a"))

        when:
        setTenant("tenant-b")
        def result = store.findById("job-1")

        then:
        result.isEmpty()

        when:
        setTenant("tenant-a")
        def resultA = store.findById("job-1")

        then:
        resultA.isPresent()
        resultA.get().id() == "job-1"
    }

    def "findAll returns only current tenant's jobs"() {
        given:
        setTenant("tenant-a")
        store.save(makeDef("all-a1", "tenant-a"))
        store.save(makeDef("all-a2", "tenant-a"))

        setTenant("tenant-b")
        store.save(makeDef("all-b1", "tenant-b"))

        when:
        setTenant("tenant-a")
        def jobsA = store.findAll()

        then:
        jobsA.size() == 2

        when:
        setTenant("tenant-b")
        def jobsB = store.findAll()

        then:
        jobsB.size() == 1
    }

    def "findEnabled returns only current tenant's enabled jobs"() {
        given:
        setTenant("tenant-a")
        store.save(makeDef("en-a1", "tenant-a"))

        def disabledJob = CronJob.builder()
                .id("en-a2").name("disabled").agentId("agent-1")
                .schedule("0 * * * *").timezone("UTC").prompt("test")
                .enabled(false).tenantId("tenant-a").build()
        store.save(new CronJobDefinition(disabledJob, null, null, null, ToolProfile.MINIMAL, []))

        setTenant("tenant-b")
        store.save(makeDef("en-b1", "tenant-b"))

        when:
        setTenant("tenant-a")
        def enabled = store.findEnabled()

        then:
        enabled.size() == 1
        enabled[0].id() == "en-a1"
    }

    def "deleteById as wrong tenant returns false"() {
        given:
        setTenant("tenant-a")
        store.save(makeDef("del-1", "tenant-a"))

        when:
        setTenant("tenant-b")
        def result = store.deleteById("del-1")

        then:
        !result

        when:
        setTenant("tenant-a")
        def stillExists = store.findById("del-1")

        then:
        stillExists.isPresent()
    }
}
